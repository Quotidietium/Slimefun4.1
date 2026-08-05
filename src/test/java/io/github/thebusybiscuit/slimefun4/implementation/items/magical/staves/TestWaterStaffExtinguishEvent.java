package io.github.thebusybiscuit.slimefun4.implementation.items.magical.staves;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

import io.github.thebusybiscuit.slimefun4.api.events.PlayerRightClickEvent;
import io.github.thebusybiscuit.slimefun4.api.events.WaterStaffExtinguishEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Regression coverage for the water staff API expansion: {@link WaterStaffExtinguishEvent},
 * exercised by driving the real {@link WaterStaff} {@link io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler}
 * with a constructed {@link PlayerRightClickEvent}.
 *
 * @author Zurker
 */
class TestWaterStaffExtinguishEvent {

    private static ServerMock server;
    private static Slimefun plugin;

    private static WaterStaff waterStaff;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "water_staff_test");
        SlimefunItemStack stack = new SlimefunItemStack("_TEST_WATER_STAFF", Material.STICK, "&fTest Water Staff");
        Slimefun.getItemCfg().setValue("_TEST_WATER_STAFF.enabled", true);
        waterStaff = new WaterStaff(itemGroup, stack, RecipeType.NULL, new ItemStack[9]);
        waterStaff.register(plugin);
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
        PlayerInteractEvent interactEvent = new PlayerInteractEvent(player, Action.RIGHT_CLICK_AIR, waterStaff.getItem().clone(), null, null);
        PlayerRightClickEvent event = new PlayerRightClickEvent(interactEvent);
        waterStaff.getItemHandler().onRightClick(event);
    }

    @Test
    @DisplayName("WaterStaffExtinguishEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Player player = server.addPlayer();

        WaterStaffExtinguishEvent event = new WaterStaffExtinguishEvent(player, waterStaff);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(waterStaff, event.getWaterStaff());
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new WaterStaffExtinguishEvent(player, null));
    }

    @Test
    @DisplayName("Using the water staff on a burning player fires the event and extinguishes them")
    void testExtinguishFiresAndClearsFire() {
        Player player = server.addPlayer();
        player.setFireTicks(100);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onExtinguish(WaterStaffExtinguishEvent event) {
                seen[0] = true;
                Assertions.assertEquals(waterStaff, event.getWaterStaff());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            useStaff(player);

            Assertions.assertTrue(seen[0], "WaterStaffExtinguishEvent was not fired");
            Assertions.assertEquals(0, player.getFireTicks(), "The player must have been extinguished");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling WaterStaffExtinguishEvent leaves the player burning")
    void testEventCancellationKeepsBurning() {
        Player player = server.addPlayer();
        player.setFireTicks(100);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onExtinguish(WaterStaffExtinguishEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            useStaff(player);

            Assertions.assertEquals(100, player.getFireTicks(), "A cancelled extinguish must keep the player burning");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Extinguishing without listeners still applies, preserving the old behavior")
    void testExtinguishWithoutListenersStillApplies() {
        Player player = server.addPlayer();
        player.setFireTicks(100);

        useStaff(player);

        Assertions.assertEquals(0, player.getFireTicks(), "The player must have been extinguished");
    }
}
