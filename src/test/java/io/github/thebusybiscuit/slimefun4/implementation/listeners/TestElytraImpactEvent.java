package io.github.thebusybiscuit.slimefun4.implementation.listeners;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

import io.github.thebusybiscuit.slimefun4.api.events.ElytraImpactEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.armor.ElytraCap;
import io.github.thebusybiscuit.slimefun4.implementation.tasks.armor.SlimefunArmorTask;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Regression coverage for the elytra impact protection API expansion:
 * {@link ElytraImpactEvent}, exercised through the real {@link ElytraImpactListener}
 * crash-protection path.
 *
 * @author Zurker
 */
class TestElytraImpactEvent {

    private static ServerMock server;
    private static Slimefun plugin;

    private static ElytraCap cap;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);

        // Unit test startups never start the integrations, the ProtectionManager is created there
        Slimefun.getIntegrations().start();
        server.getScheduler().performOneTick();

        // Unit test startups do not register the listeners, register it manually
        new ElytraImpactListener(plugin);

        // Non-configurable items stay DISABLED unless Items.yml says otherwise, enable it first
        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "elytra_impact_test");
        SlimefunItemStack stack = new SlimefunItemStack("TEST_ELYTRA_CAP", Material.LEATHER_HELMET, "&7Test Elytra Cap");
        Slimefun.getItemCfg().setValue("TEST_ELYTRA_CAP.enabled", true);
        cap = new ElytraCap(itemGroup, stack, RecipeType.NULL, new ItemStack[9]);
        cap.register(plugin);
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
     * Creates a gliding player wearing the test cap, with the armor cache refreshed
     * through the real {@link SlimefunArmorTask}.
     */
    private Player setupProtectedPlayer() throws InterruptedException {
        Player player = server.addPlayer();
        TestUtilities.awaitProfile(player);
        player.getInventory().setHelmet(cap.getItem());

        // Refresh the profile's armor cache so the helmet and its protection are visible
        new SlimefunArmorTask().run();

        player.setGliding(true);
        return player;
    }

    @Test
    @DisplayName("ElytraImpactEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Player player = server.addPlayer();
        EntityDamageEvent damageEvent = new EntityDamageEvent(player, DamageCause.FALL, 4.0);

        ElytraImpactEvent event = new ElytraImpactEvent(player, cap, damageEvent);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(cap, event.getHelmet());
        Assertions.assertEquals(damageEvent, event.getDamageEvent());
        Assertions.assertEquals(DamageCause.FALL, event.getDamageCause());
        Assertions.assertEquals(4.0, event.getDamage());
        Assertions.assertEquals(1, event.getHelmetDamage(), "The protection must default to one durability hit");
        Assertions.assertFalse(event.isCancelled());

        // The durability cost can be adjusted, zero makes the protection free
        event.setHelmetDamage(0);
        Assertions.assertEquals(0, event.getHelmetDamage());
        event.setHelmetDamage(5);
        Assertions.assertEquals(5, event.getHelmetDamage());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setHelmetDamage(-1));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ElytraImpactEvent(player, null, damageEvent));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ElytraImpactEvent(player, cap, null));
    }

    @Test
    @DisplayName("A gliding crash fires the event and cancels the damage")
    void testImpactFiresAndCancelsDamage() throws InterruptedException {
        Player player = setupProtectedPlayer();

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onImpact(ElytraImpactEvent event) {
                // Other Players from earlier tests are still online - only look at our own
                if (!event.getPlayer().equals(player)) {
                    return;
                }

                seen[0] = true;
                Assertions.assertEquals(cap, event.getHelmet());
                Assertions.assertEquals(DamageCause.FALL, event.getDamageCause());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            EntityDamageEvent damageEvent = new EntityDamageEvent(player, DamageCause.FALL, 4.0);
            server.getPluginManager().callEvent(damageEvent);

            Assertions.assertTrue(seen[0], "ElytraImpactEvent was not fired");
            Assertions.assertTrue(damageEvent.isCancelled(), "The protection must have cancelled the damage");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling ElytraImpactEvent keeps the vanilla damage")
    void testEventCancellationKeepsDamage() throws InterruptedException {
        Player player = setupProtectedPlayer();

        Listener cancelling = new Listener() {
            @EventHandler
            public void onImpact(ElytraImpactEvent event) {
                if (event.getPlayer().equals(player)) {
                    event.setCancelled(true);
                }
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            EntityDamageEvent damageEvent = new EntityDamageEvent(player, DamageCause.FALL, 4.0);
            server.getPluginManager().callEvent(damageEvent);

            Assertions.assertFalse(damageEvent.isCancelled(), "A cancelled ElytraImpactEvent must keep the vanilla damage");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("A crash without listeners is still protected, preserving the old behavior")
    void testImpactWithoutListenersStillProtects() throws InterruptedException {
        Player player = setupProtectedPlayer();

        EntityDamageEvent damageEvent = new EntityDamageEvent(player, DamageCause.FLY_INTO_WALL, 4.0);
        server.getPluginManager().callEvent(damageEvent);

        Assertions.assertTrue(damageEvent.isCancelled(), "The protection must have cancelled the damage");
    }

    /**
     * Returns the durability damage of the helmet the given {@link Player} is wearing.
     */
    private int wornHelmetDamage(Player player) {
        return ((Damageable) player.getInventory().getHelmet().getItemMeta()).getDamage();
    }

    @Test
    @DisplayName("A protected impact costs one helmet durability by default")
    void testDefaultImpactCostsOneDurability() throws InterruptedException {
        Player player = setupProtectedPlayer();

        EntityDamageEvent damageEvent = new EntityDamageEvent(player, DamageCause.FALL, 4.0);
        server.getPluginManager().callEvent(damageEvent);

        Assertions.assertTrue(damageEvent.isCancelled(), "The protection must have cancelled the damage");
        Assertions.assertEquals(1, wornHelmetDamage(player), "The helmet must have taken one durability hit");
    }

    @Test
    @DisplayName("Scaling the helmet damage charges the adjusted durability cost")
    void testSetHelmetDamageScalesWear() throws InterruptedException {
        Player player = setupProtectedPlayer();

        Listener scaling = new Listener() {
            @EventHandler
            public void onImpact(ElytraImpactEvent event) {
                if (event.getPlayer().equals(player)) {
                    Assertions.assertEquals(1, event.getHelmetDamage(), "The durability cost must default to one");
                    event.setHelmetDamage(3);
                }
            }
        };
        server.getPluginManager().registerEvents(scaling, plugin);

        try {
            EntityDamageEvent damageEvent = new EntityDamageEvent(player, DamageCause.FALL, 4.0);
            server.getPluginManager().callEvent(damageEvent);

            Assertions.assertTrue(damageEvent.isCancelled(), "The protection must have cancelled the damage");
            Assertions.assertEquals(3, wornHelmetDamage(player), "The helmet must have taken the scaled durability hits");
        } finally {
            HandlerList.unregisterAll(scaling);
        }
    }

    @Test
    @DisplayName("Zeroing the helmet damage protects for free")
    void testSetHelmetDamageZeroMakesProtectionFree() throws InterruptedException {
        Player player = setupProtectedPlayer();

        Listener freeing = new Listener() {
            @EventHandler
            public void onImpact(ElytraImpactEvent event) {
                if (event.getPlayer().equals(player)) {
                    event.setHelmetDamage(0);
                }
            }
        };
        server.getPluginManager().registerEvents(freeing, plugin);

        try {
            EntityDamageEvent damageEvent = new EntityDamageEvent(player, DamageCause.FALL, 4.0);
            server.getPluginManager().callEvent(damageEvent);

            Assertions.assertTrue(damageEvent.isCancelled(), "The protection must have cancelled the damage");
            Assertions.assertEquals(0, wornHelmetDamage(player), "The helmet must have stayed untouched");
        } finally {
            HandlerList.unregisterAll(freeing);
        }
    }

    @Test
    @DisplayName("A non-gliding player fires no event and takes the damage")
    void testNoEventWithoutGliding() throws InterruptedException {
        Player player = server.addPlayer();
        TestUtilities.awaitProfile(player);
        player.getInventory().setHelmet(cap.getItem());
        new SlimefunArmorTask().run();

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onImpact(ElytraImpactEvent event) {
                if (event.getPlayer().equals(player)) {
                    seen[0] = true;
                }
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            EntityDamageEvent damageEvent = new EntityDamageEvent(player, DamageCause.FALL, 4.0);
            server.getPluginManager().callEvent(damageEvent);

            Assertions.assertFalse(seen[0], "No event must be fired for a non-gliding player");
            Assertions.assertFalse(damageEvent.isCancelled(), "A non-gliding player must take the damage");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("A vanilla helmet fires no event and takes the damage")
    void testNoEventWithVanillaHelmet() throws InterruptedException {
        Player player = server.addPlayer();
        TestUtilities.awaitProfile(player);
        player.getInventory().setHelmet(new ItemStack(Material.IRON_HELMET));
        new SlimefunArmorTask().run();
        player.setGliding(true);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onImpact(ElytraImpactEvent event) {
                if (event.getPlayer().equals(player)) {
                    seen[0] = true;
                }
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            EntityDamageEvent damageEvent = new EntityDamageEvent(player, DamageCause.FALL, 4.0);
            server.getPluginManager().callEvent(damageEvent);

            Assertions.assertFalse(seen[0], "No event must be fired for a vanilla helmet");
            Assertions.assertFalse(damageEvent.isCancelled(), "A vanilla helmet grants no protection");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }
}
