package io.github.thebusybiscuit.slimefun4.implementation.listeners.entity;

import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.potion.PotionEffect;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.BeeMock;
import be.seeseemelk.mockbukkit.entity.CowMock;

import io.github.thebusybiscuit.slimefun4.api.events.BeeStingProtectionEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.armor.HazmatArmorPiece;
import io.github.thebusybiscuit.slimefun4.implementation.tasks.armor.SlimefunArmorTask;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Regression coverage for the hazmat bee-sting protection API expansion:
 * {@link BeeStingProtectionEvent}, exercised through the real {@link BeeListener}
 * sting-absorption path with a full hazmat set whose armor cache is refreshed by the
 * real {@link SlimefunArmorTask}.
 *
 * @author Zurker
 */
class TestBeeStingProtectionEvent {

    private static ServerMock server;
    private static Slimefun plugin;

    private static HazmatArmorPiece helmet;
    private static HazmatArmorPiece chestplate;
    private static HazmatArmorPiece leggings;
    private static HazmatArmorPiece boots;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);

        // Unit test startups never start the integrations, the ProtectionManager is created there
        Slimefun.getIntegrations().start();
        server.getScheduler().performOneTick();

        // Unit test startups do not register the listeners, register it manually
        new BeeListener(plugin);

        // Non-configurable items stay DISABLED unless Items.yml says otherwise, enable them first
        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "bee_sting_test");
        helmet = register(itemGroup, "TEST_HAZMAT_HELMET", Material.LEATHER_HELMET);
        chestplate = register(itemGroup, "TEST_HAZMAT_CHESTPLATE", Material.LEATHER_CHESTPLATE);
        leggings = register(itemGroup, "TEST_HAZMAT_LEGGINGS", Material.LEATHER_LEGGINGS);
        boots = register(itemGroup, "TEST_HAZMAT_BOOTS", Material.LEATHER_BOOTS);
    }

    private static HazmatArmorPiece register(ItemGroup itemGroup, String id, Material material) {
        Slimefun.getItemCfg().setValue(id + ".enabled", true);
        SlimefunItemStack stack = new SlimefunItemStack(id, material, "&cTest Hazmat Piece");
        HazmatArmorPiece piece = new HazmatArmorPiece(itemGroup, stack, RecipeType.NULL, new ItemStack[9], new PotionEffect[0]);
        piece.register(plugin);
        return piece;
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
     * Creates a player wearing the full test hazmat set, with the armor cache
     * refreshed through the real {@link SlimefunArmorTask} so the bee protection
     * becomes visible to the profile.
     */
    private Player setupProtectedPlayer() throws InterruptedException {
        Player player = server.addPlayer();
        TestUtilities.awaitProfile(player);

        player.getInventory().setHelmet(helmet.getItem());
        player.getInventory().setChestplate(chestplate.getItem());
        player.getInventory().setLeggings(leggings.getItem());
        player.getInventory().setBoots(boots.getItem());

        // Refresh the profile's armor cache so the set and its protection are visible
        new SlimefunArmorTask().run();
        return player;
    }

    /**
     * Lets the given bee sting the player for six hearts of damage through the real
     * event pipeline and returns the damage event for assertions.
     */
    private EntityDamageByEntityEvent sting(org.bukkit.entity.Bee bee, Player player) {
        EntityDamageByEntityEvent damageEvent = new EntityDamageByEntityEvent(bee, player, DamageCause.ENTITY_ATTACK, 6.0);
        server.getPluginManager().callEvent(damageEvent);
        return damageEvent;
    }

    private int getDamage(ItemStack item) {
        return ((Damageable) item.getItemMeta()).getDamage();
    }

    @Test
    @DisplayName("BeeStingProtectionEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Player player = server.addPlayer();
        BeeMock bee = new BeeMock(server, UUID.randomUUID());
        EntityDamageByEntityEvent damageEvent = new EntityDamageByEntityEvent(bee, player, DamageCause.ENTITY_ATTACK, 6.0);

        BeeStingProtectionEvent event = new BeeStingProtectionEvent(player, bee, damageEvent);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(bee, event.getBee());
        Assertions.assertEquals(damageEvent, event.getDamageEvent());
        Assertions.assertEquals(6.0, event.getDamage());
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new BeeStingProtectionEvent(player, null, damageEvent));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new BeeStingProtectionEvent(player, bee, null));
    }

    @Test
    @DisplayName("A bee sting with a full hazmat set fires the event, nullifies the damage and hurts the armor")
    void testStingFiresAndProtects() throws InterruptedException {
        Player player = setupProtectedPlayer();
        BeeMock bee = new BeeMock(server, UUID.randomUUID());

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onProtect(BeeStingProtectionEvent event) {
                seen[0] = true;
                Assertions.assertEquals(player, event.getPlayer());
                Assertions.assertEquals(bee, event.getBee());
                Assertions.assertEquals(6.0, event.getDamage());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            EntityDamageByEntityEvent damageEvent = sting(bee, player);

            Assertions.assertTrue(seen[0], "BeeStingProtectionEvent was not fired");
            Assertions.assertEquals(0.0, damageEvent.getDamage(), "The armor must have absorbed the sting");
            Assertions.assertEquals(1, getDamage(player.getInventory().getHelmet()), "The helmet must have lost one durability point");
            Assertions.assertEquals(1, getDamage(player.getInventory().getChestplate()), "The chestplate must have lost one durability point");
            Assertions.assertEquals(1, getDamage(player.getInventory().getLeggings()), "The leggings must have lost one durability point");
            Assertions.assertEquals(1, getDamage(player.getInventory().getBoots()), "The boots must have lost one durability point");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling BeeStingProtectionEvent lets the sting through and keeps the armor intact")
    void testEventCancellationSkipsProtection() throws InterruptedException {
        Player player = setupProtectedPlayer();
        BeeMock bee = new BeeMock(server, UUID.randomUUID());

        Listener cancelling = new Listener() {
            @EventHandler
            public void onProtect(BeeStingProtectionEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            EntityDamageByEntityEvent damageEvent = sting(bee, player);

            Assertions.assertEquals(6.0, damageEvent.getDamage(), "A cancelled protection must let the sting through");
            Assertions.assertEquals(0, getDamage(player.getInventory().getHelmet()), "A cancelled protection must keep the armor intact");
            Assertions.assertEquals(0, getDamage(player.getInventory().getBoots()), "A cancelled protection must keep the armor intact");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Protection without listeners still absorbs, preserving the old behavior")
    void testStingWithoutListenersStillProtects() throws InterruptedException {
        Player player = setupProtectedPlayer();
        BeeMock bee = new BeeMock(server, UUID.randomUUID());

        EntityDamageByEntityEvent damageEvent = sting(bee, player);

        Assertions.assertEquals(0.0, damageEvent.getDamage(), "The armor must have absorbed the sting");
        Assertions.assertEquals(1, getDamage(player.getInventory().getHelmet()), "The helmet must have lost one durability point");
    }

    @Test
    @DisplayName("An incomplete hazmat set fires no event and does not protect")
    void testIncompleteSetFiresNothing() throws InterruptedException {
        Player player = server.addPlayer();
        TestUtilities.awaitProfile(player);

        player.getInventory().setHelmet(helmet.getItem());
        player.getInventory().setChestplate(chestplate.getItem());
        player.getInventory().setLeggings(leggings.getItem());
        new SlimefunArmorTask().run();

        BeeMock bee = new BeeMock(server, UUID.randomUUID());

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onProtect(BeeStingProtectionEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            EntityDamageByEntityEvent damageEvent = sting(bee, player);

            Assertions.assertFalse(seen[0], "No event must be fired without the full set");
            Assertions.assertEquals(6.0, damageEvent.getDamage(), "An incomplete set must not protect");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Damage not caused by a bee fires no event")
    void testNonBeeDamagerFiresNothing() throws InterruptedException {
        Player player = setupProtectedPlayer();
        Player attacker = server.addPlayer();

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onProtect(BeeStingProtectionEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            EntityDamageByEntityEvent damageEvent = new EntityDamageByEntityEvent(attacker, player, DamageCause.ENTITY_ATTACK, 6.0);
            server.getPluginManager().callEvent(damageEvent);

            Assertions.assertFalse(seen[0], "No event must be fired for a non-bee damager");
            Assertions.assertEquals(6.0, damageEvent.getDamage(), "The armor must not absorb other damage");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("A bee stinging a non-player fires no event")
    void testNonPlayerVictimFiresNothing() {
        BeeMock bee = new BeeMock(server, UUID.randomUUID());
        CowMock cow = new CowMock(server, UUID.randomUUID());

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onProtect(BeeStingProtectionEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            EntityDamageByEntityEvent damageEvent = new EntityDamageByEntityEvent(bee, cow, DamageCause.ENTITY_ATTACK, 6.0);
            server.getPluginManager().callEvent(damageEvent);

            Assertions.assertFalse(seen[0], "No event must be fired for a non-player victim");
            Assertions.assertEquals(6.0, damageEvent.getDamage());
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }
}
