package io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines.entities;

import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
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

import io.github.thebusybiscuit.slimefun4.api.events.ExpCollectorProduceEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

import me.mrCookieSlime.Slimefun.api.BlockStorage;

/**
 * Regression coverage for the ExpCollector produce API expansion:
 * {@link ExpCollectorProduceEvent}, exercised by driving the real {@link ExpCollector}
 * ticker with enough stored XP to produce flasks.
 *
 * @author Zurker
 */
class TestExpCollectorProduceEvent {

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static ExpCollector collector;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        Slimefun.getIntegrations().start();
        server.getScheduler().performOneTick();

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "exp_produce_test");
        SlimefunItemStack stack = new SlimefunItemStack("_TEST_EXP_PRODUCE", Material.PLAYER_HEAD, "&fTest Exp Produce");
        Slimefun.getItemCfg().setValue("_TEST_EXP_PRODUCE.enabled", true);
        collector = new ExpCollector(itemGroup, stack, RecipeType.NULL, new ItemStack[9], 4.0);
        collector.setCapacity(100);
        collector.setEnergyConsumption(5);
        collector.register(plugin);
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    @BeforeEach
    public void beforeEach() {
        server.getPluginManager().clearEvents();
    }

    private Block placeCollector(int x, int z) {
        Block b = world.getBlockAt(x, 1, z);
        b.setType(Material.PLAYER_HEAD);
        BlockStorage.addBlockInfo(b, "id", collector.getId(), true);
        BlockStorage.addBlockInfo(b.getLocation(), "energy-charge", "100", false);
        // Pre-store 10 XP so that the first orb collection triggers flask production
        // (produceFlasks reads the PREVIOUSLY stored value, not the just-collected one)
        BlockStorage.addBlockInfo(b.getLocation(), "stored-exp", "10", false);
        return b;
    }

    private void tick(Block b) {
        collector.callItemHandler(me.mrCookieSlime.Slimefun.Objects.handlers.BlockTicker.class, ticker -> ticker.tick(b, collector, me.mrCookieSlime.Slimefun.api.BlockStorage.getLocationInfo(b.getLocation())));
    }

    private ExperienceOrbMock spawnOrb(Block collectorBlock, int experience) {
        Location loc = collectorBlock.getLocation().clone().add(1.5, 0, 0.5);
        ExperienceOrbMock orb = new ExperienceOrbMock(server, UUID.randomUUID(), experience);
        orb.setLocation(loc);
        server.registerEntity(orb);
        return orb;
    }

    @Test
    @DisplayName("ExpCollectorProduceEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Block b = placeCollector(1, 1);
        ItemStack flask = SlimefunItems.FILLED_FLASK_OF_KNOWLEDGE.item();

        ExpCollectorProduceEvent event = new ExpCollectorProduceEvent(collector, b, 10, flask);

        Assertions.assertEquals(collector, event.getCollector());
        Assertions.assertEquals(b, event.getBlock());
        Assertions.assertEquals(10, event.getExperienceCost());
        Assertions.assertEquals(flask, event.getResult());
        Assertions.assertFalse(event.isCancelled());

        ItemStack replacement = new ItemStack(Material.DIAMOND);
        event.setResult(replacement);
        Assertions.assertEquals(replacement, event.getResult());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new ExpCollectorProduceEvent(null, b, 10, flask));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ExpCollectorProduceEvent(collector, null, 10, flask));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ExpCollectorProduceEvent(collector, b, 0, flask));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ExpCollectorProduceEvent(collector, b, 10, null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setResult(null));
    }

    @Test
    @DisplayName("Producing a flask from stored XP fires ExpCollectorProduceEvent")
    void testProduceFiresEvent() {
        Block b = placeCollector(10, 10);
        ExperienceOrbMock orb = spawnOrb(b, 5);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onProduce(ExpCollectorProduceEvent event) {
                if (event.getBlock().equals(b)) {
                    seen[0] = true;
                    Assertions.assertEquals(collector, event.getCollector());
                    Assertions.assertEquals(10, event.getExperienceCost());
                }
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            // The pre-stored 10 XP triggers flask production during this tick's orb collection
            tick(b);

            Assertions.assertTrue(seen[0], "ExpCollectorProduceEvent was not fired");
        } finally {
            HandlerList.unregisterAll(watcher);
            if (orb.isValid()) orb.remove();
            server.unregisterEntity(orb);
        }
    }

    @Test
    @DisplayName("Cancelling ExpCollectorProduceEvent keeps the XP stored")
    void testCancelKeepsXp() {
        Block b = placeCollector(20, 20);
        ExperienceOrbMock orb = spawnOrb(b, 5);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onProduce(ExpCollectorProduceEvent event) {
                if (event.getBlock().equals(b)) {
                    event.setCancelled(true);
                }
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            tick(b); // collect orb + attempt to produce (cancelled)

            // The 10 pre-stored XP must remain (production was vetoed)
            String stored = BlockStorage.getLocationInfo(b.getLocation(), "stored-exp");
            Assertions.assertNotNull(stored);
            Assertions.assertTrue(Integer.parseInt(stored) >= 10, "A vetoed production must keep at least the pre-stored XP");
        } finally {
            HandlerList.unregisterAll(cancelling);
            if (orb.isValid()) orb.remove();
            server.unregisterEntity(orb);
        }
    }

    @Test
    @DisplayName("Producing without listeners still works, preserving the old behavior")
    void testProduceWithoutListenersWorks() {
        Block b = placeCollector(30, 30);
        ExperienceOrbMock orb = spawnOrb(b, 5);

        try {
            tick(b); // collect orb + produce flask from pre-stored XP

            // The pre-stored 10 XP should be consumed (withdrawn), only the new orb's XP remains
            String stored = BlockStorage.getLocationInfo(b.getLocation(), "stored-exp");
            Assertions.assertNotNull(stored);
            Assertions.assertTrue(Integer.parseInt(stored) < 15, "The pre-stored XP must have been consumed for flask production");
        } finally {
            if (orb.isValid()) orb.remove();
            server.unregisterEntity(orb);
        }
    }
}
