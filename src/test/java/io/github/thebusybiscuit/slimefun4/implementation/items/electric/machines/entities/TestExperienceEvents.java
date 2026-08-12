package io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines.entities;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.ExperienceOrbMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;

import io.github.thebusybiscuit.slimefun4.api.events.ExpCollectorCollectEvent;
import io.github.thebusybiscuit.slimefun4.api.events.KnowledgeFlaskFillEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun4.implementation.items.magical.KnowledgeFlask;
import io.github.thebusybiscuit.slimefun4.implementation.listeners.SlimefunItemInteractListener;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

import me.mrCookieSlime.Slimefun.api.BlockStorage;

/**
 * Regression coverage for the experience API expansion: {@link ExpCollectorCollectEvent}
 * and {@link KnowledgeFlaskFillEvent}, exercised through the real {@link ExpCollector} tick
 * and the {@link SlimefunItemInteractListener} dispatch path.
 *
 * @author Zurker
 */
class TestExperienceEvents {

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static ExpCollector collector;
    private static KnowledgeFlask flask;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        // Unit test startups never start the integrations, the ProtectionManager is created there
        Slimefun.getIntegrations().start();
        server.getScheduler().performOneTick();

        // Unit test startups do not register the listeners, register it manually
        new SlimefunItemInteractListener(plugin);

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "experience_events");

        SlimefunItemStack collectorStack = new SlimefunItemStack("TEST_EXP_COLLECTOR", Material.PLAYER_HEAD, "&7Test Exp Collector");
        collector = new ExpCollector(itemGroup, collectorStack, RecipeType.NULL, new ItemStack[9], 4.0);
        collector.setCapacity(100);
        collector.setEnergyConsumption(5);
        collector.register(plugin);

        SlimefunItemStack flaskStack = new SlimefunItemStack("TEST_KNOWLEDGE_FLASK", Material.GLASS_BOTTLE, "&7Test Knowledge Flask");
        flask = new KnowledgeFlask(itemGroup, flaskStack, RecipeType.NULL, new ItemStack[9], null);
        flask.register(plugin);
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    @BeforeEach
    public void beforeEach() {
        server.getPluginManager().clearEvents();
    }

    /**
     * Places a charged Exp Collector on the world grid at the given coordinates.
     */
    private Block setupCollector(int x, int z, String charge) {
        Block b = world.getBlockAt(x, 1, z);
        b.setType(Material.PLAYER_HEAD);
        BlockStorage.addBlockInfo(b, "id", collector.getId(), true);
        BlockStorage.addBlockInfo(b.getLocation(), "energy-charge", charge, false);
        return b;
    }

    /**
     * Registers an {@link ExperienceOrb} with the given experience value next to the
     * given collector {@link Block}, well within its collection range.
     */
    private ExperienceOrbMock spawnOrb(Block collectorBlock, int experience) {
        Location loc = collectorBlock.getLocation().clone().add(1.5, 0, 0.5);
        ExperienceOrbMock orb = new ExperienceOrbMock(server, UUID.randomUUID(), experience);
        orb.setLocation(loc);
        server.registerEntity(orb);
        return orb;
    }

    /**
     * Unregisters an orb. MockBukkit requires the entity to be marked removed first.
     */
    private void cleanupOrb(ExperienceOrbMock orb) {
        if (orb.isValid()) {
            orb.remove();
        }
        server.unregisterEntity(orb);
    }

    private int getStoredExperience(Block b) {
        String value = BlockStorage.getLocationInfo(b.getLocation(), "stored-exp");
        return value == null ? 0 : Integer.parseInt(value);
    }

    // ---------- ExpCollectorCollectEvent ----------

    @Test
    @DisplayName("ExpCollectorCollectEvent exposes its fields and validates constructor arguments")
    void testCollectEventFieldsAndValidation() {
        Block b = world.getBlockAt(1, 1, 1);
        ExperienceOrbMock orb = new ExperienceOrbMock(server, UUID.randomUUID(), 8);

        ExpCollectorCollectEvent event = new ExpCollectorCollectEvent(collector, b, orb);

        Assertions.assertEquals(collector, event.getCollector());
        Assertions.assertEquals(b, event.getBlock());
        Assertions.assertEquals(orb, event.getOrb());
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new ExpCollectorCollectEvent(null, b, orb));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ExpCollectorCollectEvent(collector, null, orb));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ExpCollectorCollectEvent(collector, b, null));
    }

    @Test
    @DisplayName("A collector tick fires ExpCollectorCollectEvent and collects the orb")
    void testTickFiresEventAndCollects() {
        Block b = setupCollector(10, 10, "10");
        ExperienceOrbMock orb = spawnOrb(b, 8);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onCollect(ExpCollectorCollectEvent event) {
                seen[0] = true;
                Assertions.assertEquals(collector, event.getCollector());
                Assertions.assertEquals(b, event.getBlock());
                Assertions.assertEquals(orb, event.getOrb());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            collector.tick(b);

            Assertions.assertTrue(seen[0], "ExpCollectorCollectEvent was not fired");
            Assertions.assertFalse(orb.isValid(), "The orb must have been collected");
            Assertions.assertEquals(8, getStoredExperience(b), "The orb's experience must have been stored");
            Assertions.assertEquals("5", BlockStorage.getLocationInfo(b.getLocation(), "energy-charge"), "The energy must have been consumed");
        } finally {
            HandlerList.unregisterAll(watcher);
            cleanupOrb(orb);
        }
    }

    @Test
    @DisplayName("Cancelling ExpCollectorCollectEvent keeps the orb and stores nothing")
    void testCollectCancellationKeepsOrb() {
        Block b = setupCollector(20, 20, "10");
        ExperienceOrbMock orb = spawnOrb(b, 8);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onCollect(ExpCollectorCollectEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            collector.tick(b);

            Assertions.assertTrue(orb.isValid(), "A cancelled collection must not remove the orb");
            Assertions.assertEquals(0, getStoredExperience(b), "A cancelled collection must not store experience");
            Assertions.assertEquals("10", BlockStorage.getLocationInfo(b.getLocation(), "energy-charge"), "A cancelled collection must not consume energy");
        } finally {
            HandlerList.unregisterAll(cancelling);
            cleanupOrb(orb);
        }
    }

    @Test
    @DisplayName("A collector tick without listeners still collects, preserving the old behavior")
    void testTickWithoutListenersStillCollects() {
        Block b = setupCollector(30, 30, "10");
        ExperienceOrbMock orb = spawnOrb(b, 8);

        try {
            collector.tick(b);

            Assertions.assertFalse(orb.isValid(), "The orb must have been collected");
            Assertions.assertEquals(8, getStoredExperience(b), "The orb's experience must have been stored");
        } finally {
            cleanupOrb(orb);
        }
    }

    @Test
    @DisplayName("A collector tick without energy fires no event and keeps the orb")
    void testTickWithoutEnergyFiresNoEvent() {
        Block b = setupCollector(40, 40, "0");
        ExperienceOrbMock orb = spawnOrb(b, 8);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onCollect(ExpCollectorCollectEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            collector.tick(b);

            Assertions.assertFalse(seen[0], "No event must be fired when the collector has no energy");
            Assertions.assertTrue(orb.isValid(), "The orb must be untouched without energy");
            Assertions.assertEquals(0, getStoredExperience(b));
        } finally {
            HandlerList.unregisterAll(watcher);
            cleanupOrb(orb);
        }
    }

    @Test
    @DisplayName("The collected experience defaults to the orb's value and is modifiable and validated")
    void testCollectExperienceDefaultAndValidation() {
        Block b = world.getBlockAt(2, 1, 2);
        ExperienceOrbMock orb = new ExperienceOrbMock(server, UUID.randomUUID(), 8);

        ExpCollectorCollectEvent event = new ExpCollectorCollectEvent(collector, b, orb);

        Assertions.assertEquals(8, event.getExperience(), "The collected experience must default to the orb's value");

        event.setExperience(16);
        Assertions.assertEquals(16, event.getExperience());

        event.setExperience(0);
        Assertions.assertEquals(0, event.getExperience(), "Zero must be allowed: consume the orb, store nothing");

        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setExperience(-1));
        Assertions.assertEquals(8, orb.getExperience(), "The orb itself must never be modified");
    }

    @Test
    @DisplayName("A modified experience amount is stored instead of the orb's own value")
    void testModifiedExperienceIsStored() {
        Block b = setupCollector(50, 50, "10");
        ExperienceOrbMock orb = spawnOrb(b, 8);

        Listener scaling = new Listener() {
            @EventHandler
            public void onCollect(ExpCollectorCollectEvent event) {
                event.setExperience(event.getExperience() * 2);
            }
        };
        server.getPluginManager().registerEvents(scaling, plugin);

        try {
            collector.tick(b);

            Assertions.assertFalse(orb.isValid(), "The orb must have been collected");
            Assertions.assertEquals(16, getStoredExperience(b), "The scaled experience must have been stored");
            Assertions.assertEquals("5", BlockStorage.getLocationInfo(b.getLocation(), "energy-charge"), "The energy must have been consumed");
        } finally {
            HandlerList.unregisterAll(scaling);
            cleanupOrb(orb);
        }
    }

    @Test
    @DisplayName("A zero experience amount consumes the orb but stores nothing")
    void testZeroExperienceStoresNothing() {
        Block b = setupCollector(60, 60, "10");
        ExperienceOrbMock orb = spawnOrb(b, 8);

        Listener voiding = new Listener() {
            @EventHandler
            public void onCollect(ExpCollectorCollectEvent event) {
                event.setExperience(0);
            }
        };
        server.getPluginManager().registerEvents(voiding, plugin);

        try {
            collector.tick(b);

            Assertions.assertFalse(orb.isValid(), "The orb must have been consumed");
            Assertions.assertEquals(0, getStoredExperience(b), "A zeroed collection must store nothing");
            Assertions.assertEquals("5", BlockStorage.getLocationInfo(b.getLocation(), "energy-charge"), "The energy must have been consumed");
        } finally {
            HandlerList.unregisterAll(voiding);
            cleanupOrb(orb);
        }
    }

    @Test
    @DisplayName("An untouched experience amount reproduces the orb's value, preserving the old behavior")
    void testUntouchedExperienceKeepsOrbValue() {
        Block b = setupCollector(70, 70, "10");
        ExperienceOrbMock orb = spawnOrb(b, 8);

        Listener watcher = new Listener() {
            @EventHandler
            public void onCollect(ExpCollectorCollectEvent event) {
                // Only observe, do not touch the amount
                Assertions.assertEquals(8, event.getExperience());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            collector.tick(b);

            Assertions.assertFalse(orb.isValid(), "The orb must have been collected");
            Assertions.assertEquals(8, getStoredExperience(b), "An untouched amount must reproduce the orb's experience");
        } finally {
            HandlerList.unregisterAll(watcher);
            cleanupOrb(orb);
        }
    }

    // ---------- KnowledgeFlaskFillEvent ----------

    @Test
    @DisplayName("KnowledgeFlaskFillEvent exposes its fields and validates constructor arguments")
    void testFlaskEventFieldsAndValidation() {
        PlayerMock player = server.addPlayer();
        ItemStack item = flask.getItem().clone();
        ItemStack result = new ItemStack(Material.EXPERIENCE_BOTTLE);

        KnowledgeFlaskFillEvent event = new KnowledgeFlaskFillEvent(player, flask, item, 1, result);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(flask, event.getFlask());
        Assertions.assertEquals(item, event.getItem());
        Assertions.assertEquals(1, event.getLevelCost());
        Assertions.assertEquals(result, event.getResult());
        Assertions.assertFalse(event.isCancelled());

        // setLevelCost
        event.setLevelCost(3);
        Assertions.assertEquals(3, event.getLevelCost());

        // setResult
        ItemStack custom = new ItemStack(Material.DIAMOND);
        event.setResult(custom);
        Assertions.assertEquals(custom, event.getResult());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new KnowledgeFlaskFillEvent(player, null, item, 1, result));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new KnowledgeFlaskFillEvent(player, flask, null, 1, result));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new KnowledgeFlaskFillEvent(player, flask, item, -1, result));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new KnowledgeFlaskFillEvent(player, flask, item, 1, null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setLevelCost(-1));
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setResult(null));
    }

    @Test
    @DisplayName("Right-clicking a Knowledge Flask fires the event and fills the flask")
    void testFlaskFillFiresAndFills() {
        PlayerMock player = server.addPlayer();
        player.setLevel(5);
        ItemStack held = flask.getItem().clone();
        player.getInventory().setItemInMainHand(held);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onFill(KnowledgeFlaskFillEvent event) {
                seen[0] = true;
                Assertions.assertEquals(player, event.getPlayer());
                Assertions.assertEquals(flask, event.getFlask());
                Assertions.assertNotNull(event.getItem());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            PlayerInteractEvent interactEvent = new PlayerInteractEvent(player, Action.RIGHT_CLICK_AIR, held, null, BlockFace.UP, EquipmentSlot.HAND);
            server.getPluginManager().callEvent(interactEvent);

            Assertions.assertTrue(seen[0], "KnowledgeFlaskFillEvent was not fired");
            Assertions.assertEquals(4, player.getLevel(), "One level must have been deducted");
            Assertions.assertTrue(player.getInventory().containsAtLeast(SlimefunItems.FILLED_FLASK_OF_KNOWLEDGE.item(), 1), "A filled flask must have been produced");
            Assertions.assertEquals(0, held.getAmount(), "The empty flask must have been consumed");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling KnowledgeFlaskFillEvent keeps the level and the empty flask")
    void testFlaskFillCancellation() {
        PlayerMock player = server.addPlayer();
        player.setLevel(5);
        ItemStack held = flask.getItem().clone();
        player.getInventory().setItemInMainHand(held);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onFill(KnowledgeFlaskFillEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            PlayerInteractEvent interactEvent = new PlayerInteractEvent(player, Action.RIGHT_CLICK_AIR, held, null, BlockFace.UP, EquipmentSlot.HAND);
            server.getPluginManager().callEvent(interactEvent);

            Assertions.assertEquals(5, player.getLevel(), "A cancelled fill must not deduct the level");
            Assertions.assertFalse(player.getInventory().containsAtLeast(SlimefunItems.FILLED_FLASK_OF_KNOWLEDGE.item(), 1), "A cancelled fill must not produce a filled flask");
            Assertions.assertEquals(1, held.getAmount(), "A cancelled fill must not consume the empty flask");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("A player without a level fires no KnowledgeFlaskFillEvent")
    void testFlaskFillWithoutLevelFiresNoEvent() {
        PlayerMock player = server.addPlayer();
        player.setLevel(0);
        ItemStack held = flask.getItem().clone();
        player.getInventory().setItemInMainHand(held);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onFill(KnowledgeFlaskFillEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            PlayerInteractEvent interactEvent = new PlayerInteractEvent(player, Action.RIGHT_CLICK_AIR, held, null, BlockFace.UP, EquipmentSlot.HAND);
            server.getPluginManager().callEvent(interactEvent);

            Assertions.assertFalse(seen[0], "No event must be fired when the player has no level");
            Assertions.assertFalse(player.getInventory().containsAtLeast(SlimefunItems.FILLED_FLASK_OF_KNOWLEDGE.item(), 1));
            Assertions.assertEquals(1, player.getInventory().getItemInMainHand().getAmount(), "The empty flask must stay untouched");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }
}
