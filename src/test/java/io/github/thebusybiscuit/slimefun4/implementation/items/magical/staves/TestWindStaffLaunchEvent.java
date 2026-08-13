package io.github.thebusybiscuit.slimefun4.implementation.items.magical.staves;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

import io.github.thebusybiscuit.slimefun4.api.events.PlayerRightClickEvent;
import io.github.thebusybiscuit.slimefun4.api.events.WindStaffLaunchEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Regression coverage for the wind staff API expansion: {@link WindStaffLaunchEvent}, exercised
 * by driving the real {@link WindStaff} {@link io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler}
 * with a constructed {@link PlayerRightClickEvent}.
 * <p>
 * The launch path ends in a {@code playEffect(SMOKE)} that MockBukkit rejects, so a RuntimeException
 * from that tail is ignored here - the event was fired and the velocity applied beforehand.
 *
 * @author Zurker
 */
class TestWindStaffLaunchEvent {

    private static ServerMock server;
    private static Slimefun plugin;

    private static WindStaff windStaff;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "wind_staff_test");
        SlimefunItemStack stack = new SlimefunItemStack("_TEST_WIND_STAFF", Material.STICK, "&fTest Wind Staff");
        Slimefun.getItemCfg().setValue("_TEST_WIND_STAFF.enabled", true);
        windStaff = new WindStaff(itemGroup, stack, RecipeType.NULL, new ItemStack[9]);
        windStaff.register(plugin);
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    @BeforeEach
    public void beforeEach() {
        server.getPluginManager().clearEvents();
    }

    private void useStaff(Player player) {
        PlayerInteractEvent interactEvent = new PlayerInteractEvent(player, Action.RIGHT_CLICK_AIR, windStaff.getItem().clone(), null, null);
        PlayerRightClickEvent event = new PlayerRightClickEvent(interactEvent);

        try {
            windStaff.getItemHandler().onRightClick(event);
        } catch (RuntimeException ignored) {
            // playEffect(SMOKE) is not fully supported by MockBukkit - see class javadoc
        }
    }

    @Test
    @DisplayName("WindStaffLaunchEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Player player = server.addPlayer();
        Vector velocity = new Vector(0, 1, 0);

        WindStaffLaunchEvent event = new WindStaffLaunchEvent(player, windStaff, velocity);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(windStaff, event.getWindStaff());
        Assertions.assertEquals(velocity, event.getVelocity());
        Assertions.assertFalse(event.isCancelled());

        Vector swapped = new Vector(1, 0, 0);
        event.setVelocity(swapped);
        Assertions.assertEquals(swapped, event.getVelocity());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new WindStaffLaunchEvent(player, null, velocity));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new WindStaffLaunchEvent(player, windStaff, null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setVelocity(null));
    }

    @Test
    @DisplayName("Using the wind staff fires the event and launches the player")
    void testLaunchFiresAndAppliesVelocity() {
        Player player = server.addPlayer();
        player.setFoodLevel(20);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onLaunch(WindStaffLaunchEvent event) {
                seen[0] = true;
                Assertions.assertEquals(windStaff, event.getWindStaff());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            useStaff(player);

            Assertions.assertTrue(seen[0], "WindStaffLaunchEvent was not fired");
            Assertions.assertTrue(player.getVelocity().length() > 0, "The player must have been launched");
            Assertions.assertEquals(18, player.getFoodLevel(), "A launched cast must still consume hunger");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling WindStaffLaunchEvent prevents the launch")
    void testEventCancellationSkipsLaunch() {
        Player player = server.addPlayer();
        int foodBefore = 20;
        player.setFoodLevel(foodBefore);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onLaunch(WindStaffLaunchEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            useStaff(player);

            Assertions.assertEquals(0, player.getVelocity().length(), "A cancelled launch must not apply velocity");
            Assertions.assertEquals(foodBefore, player.getFoodLevel(), "A cancelled launch must not consume hunger");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Overriding the velocity via setVelocity launches with the custom vector")
    void testVelocityOverride() {
        Player player = server.addPlayer();
        player.setFoodLevel(20);
        Vector custom = new Vector(0, 2, 0);

        boolean[] seenOverridden = { false };
        Listener overriding = new Listener() {
            @EventHandler
            public void onLaunch(WindStaffLaunchEvent event) {
                event.setVelocity(custom);
                seenOverridden[0] = true;
            }
        };
        server.getPluginManager().registerEvents(overriding, plugin);

        try {
            useStaff(player);

            Assertions.assertTrue(seenOverridden[0], "WindStaffLaunchEvent was not fired");
            Assertions.assertEquals(custom, player.getVelocity(), "The custom velocity must have been applied");
        } finally {
            HandlerList.unregisterAll(overriding);
        }
    }

    @Test
    @DisplayName("Using the staff without enough food fires no event")
    void testHungryFiresNothing() {
        Player player = server.addPlayer();
        player.setFoodLevel(1);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onLaunch(WindStaffLaunchEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            useStaff(player);

            Assertions.assertFalse(seen[0], "No event must be fired without enough food");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }
}
