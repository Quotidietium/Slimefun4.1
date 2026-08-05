package io.github.thebusybiscuit.slimefun4.implementation.tasks.player;

import org.bukkit.Material;
import org.bukkit.entity.Player;
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

import io.github.thebusybiscuit.slimefun4.api.events.ParachuteSlowFallEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.armor.Parachute;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Behavioral coverage for {@link ParachuteSlowFallEvent} through the real {@link ParachuteTask}
 * slow-fall path.
 *
 * @author Zurker
 */
class TestParachuteSlowFallEvent {

    private static ServerMock server;
    private static Slimefun plugin;

    private static Parachute parachute;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);

        // Non-configurable items stay DISABLED unless Items.yml says otherwise, enable it first
        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "parachute_test");
        SlimefunItemStack stack = new SlimefunItemStack("TEST_PARACHUTE", Material.LEATHER_CHESTPLATE, "&fTest Parachute");
        Slimefun.getItemCfg().setValue("TEST_PARACHUTE.enabled", true);
        parachute = new Parachute(itemGroup, stack, RecipeType.NULL, new ItemStack[9]);
        parachute.register(plugin);
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    @BeforeEach
    public void beforeEach() {
        server.getPluginManager().clearEvents();
    }

    @Test
    @DisplayName("ParachuteSlowFallEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Player player = server.addPlayer();

        ParachuteSlowFallEvent event = new ParachuteSlowFallEvent(player, parachute);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(parachute, event.getParachute());
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new ParachuteSlowFallEvent(player, null));
    }

    @Test
    @DisplayName("A parachute tick fires the event and applies slow-fall velocity")
    void testSlowFallFiresAndAppliesVelocity() {
        Player player = server.addPlayer();

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onSlowFall(ParachuteSlowFallEvent event) {
                if (event.getPlayer().equals(player)) {
                    seen[0] = true;
                    Assertions.assertEquals(parachute, event.getParachute());
                }
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            new ParachuteTask(player, parachute).executeTask();

            Assertions.assertTrue(seen[0], "ParachuteSlowFallEvent was not fired");
            Assertions.assertTrue(player.getVelocity().getY() < 0, "Slow-fall should apply a downward velocity");
            Assertions.assertEquals(0F, player.getFallDistance(), 0.001);
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling ParachuteSlowFallEvent skips the velocity for this tick")
    void testEventCancellationSkipsVelocity() {
        Player player = server.addPlayer();

        Listener cancelling = new Listener() {
            @EventHandler
            public void onSlowFall(ParachuteSlowFallEvent event) {
                if (event.getPlayer().equals(player)) {
                    event.setCancelled(true);
                }
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            new ParachuteTask(player, parachute).executeTask();

            Assertions.assertEquals(0, player.getVelocity().length(), "A cancelled slow-fall must not apply velocity");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Slow-fall without listeners still applies, preserving the old behavior")
    void testSlowFallWithoutListenersStillApplies() {
        Player player = server.addPlayer();

        new ParachuteTask(player, parachute).executeTask();

        Assertions.assertTrue(player.getVelocity().getY() < 0, "Slow-fall should apply a downward velocity");
    }
}
