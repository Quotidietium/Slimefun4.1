package io.github.thebusybiscuit.slimefun4.implementation.tasks.player;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

import io.github.thebusybiscuit.slimefun4.api.events.BeeWingsSlowFallEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.magical.BeeWings;
import io.github.thebusybiscuit.slimefun4.implementation.listeners.BeeWingsListener;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Regression coverage for the bee wings API expansion: {@link BeeWingsSlowFallEvent},
 * exercised through the real {@link BeeWingsListener} and the scheduled
 * {@link BeeWingsTask} descent watch.
 *
 * @author Zurker
 */
class TestBeeWingsSlowFallEvent {

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static BeeWings wings;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        // Unit test startups never start the integrations, the ProtectionManager is created there
        Slimefun.getIntegrations().start();
        server.getScheduler().performOneTick();

        // Non-configurable items stay DISABLED unless Items.yml says otherwise, enable it first
        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "bee_wings_test");
        Slimefun.getItemCfg().setValue("TEST_BEE_WINGS.enabled", true);
        wings = new BeeWings(itemGroup, new SlimefunItemStack("TEST_BEE_WINGS", Material.ELYTRA, "&6Test Bee Wings"), RecipeType.NULL, new ItemStack[9]);
        wings.register(plugin);

        // Unit test startups do not register the listeners, register it manually
        new BeeWingsListener(plugin, wings);
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
     * Creates a gliding player wearing the wings above a solid floor, starts the glide
     * (scheduling the descent watch) and then descends close to the ground: y=4 is
     * exactly the task's slow-down altitude over a floor at y=0.
     */
    private Player glideDownToGround(int x, int z) {
        world.getBlockAt(x, 0, z).setType(Material.STONE);

        Player player = server.addPlayer();
        player.getInventory().setChestplate(wings.getItem().clone());
        player.setGliding(true);
        player.teleport(new Location(world, x, 5, z));

        server.getPluginManager().callEvent(new EntityToggleGlideEvent(player, true));

        player.teleport(new Location(world, x, 4, z));
        return player;
    }

    @Test
    @DisplayName("BeeWingsSlowFallEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Player player = server.addPlayer();
        org.bukkit.potion.PotionEffect effect = new org.bukkit.potion.PotionEffect(PotionEffectType.SLOW_FALLING, 60, 0);

        BeeWingsSlowFallEvent event = new BeeWingsSlowFallEvent(player, effect);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(effect, event.getEffect());
        Assertions.assertEquals(PotionEffectType.SLOW_FALLING, event.getEffect().getType());
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new BeeWingsSlowFallEvent(player, null));
    }

    @Test
    @DisplayName("Approaching the ground fires the event and applies slow falling")
    void testSlowFallFiresAndApplies() {
        Player player = glideDownToGround(10, 10);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onSlowFall(BeeWingsSlowFallEvent event) {
                if (!event.getPlayer().equals(player)) {
                    return;
                }

                seen[0] = true;
                Assertions.assertEquals(PotionEffectType.SLOW_FALLING, event.getEffect().getType());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            server.getScheduler().performTicks(5);

            Assertions.assertTrue(seen[0], "BeeWingsSlowFallEvent was not fired");
            Assertions.assertTrue(player.hasPotionEffect(PotionEffectType.SLOW_FALLING), "Slow falling must have been applied");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling BeeWingsSlowFallEvent skips the effect")
    void testEventCancellationSkipsEffect() {
        Player player = glideDownToGround(20, 20);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onSlowFall(BeeWingsSlowFallEvent event) {
                if (event.getPlayer().equals(player)) {
                    event.setCancelled(true);
                }
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            server.getScheduler().performTicks(5);

            Assertions.assertFalse(player.hasPotionEffect(PotionEffectType.SLOW_FALLING), "A cancelled event must skip the slow falling effect");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Slow falling without listeners is still applied, preserving the old behavior")
    void testSlowFallWithoutListenersStillApplies() {
        Player player = glideDownToGround(30, 30);

        server.getScheduler().performTicks(5);

        Assertions.assertTrue(player.hasPotionEffect(PotionEffectType.SLOW_FALLING), "Slow falling must have been applied");
    }

    @Test
    @DisplayName("A gliding player without the wings fires no event")
    void testNoWingsFireNothing() {
        world.getBlockAt(40, 0, 40).setType(Material.STONE);

        Player player = server.addPlayer();
        player.getInventory().setChestplate(new ItemStack(Material.ELYTRA));
        player.setGliding(true);
        player.teleport(new Location(world, 40, 5, 40));

        server.getPluginManager().callEvent(new EntityToggleGlideEvent(player, true));

        player.teleport(new Location(world, 40, 4, 40));

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onSlowFall(BeeWingsSlowFallEvent event) {
                if (event.getPlayer().equals(player)) {
                    seen[0] = true;
                }
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            server.getScheduler().performTicks(5);

            Assertions.assertFalse(seen[0], "No event must be fired without the wings");
            Assertions.assertFalse(player.hasPotionEffect(PotionEffectType.SLOW_FALLING), "Plain elytra grants no slow falling");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }
}
