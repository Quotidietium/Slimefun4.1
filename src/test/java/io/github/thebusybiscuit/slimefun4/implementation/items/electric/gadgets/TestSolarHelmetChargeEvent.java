package io.github.thebusybiscuit.slimefun4.implementation.items.electric.gadgets;

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
import org.mockito.Mockito;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

import io.github.thebusybiscuit.slimefun4.api.events.SolarHelmetChargeEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.Rechargeable;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Regression coverage for the solar helmet API expansion: {@link SolarHelmetChargeEvent},
 * exercised by equipping a real {@link Jetpack} and driving {@link SolarHelmet#rechargeItems}.
 *
 * @author Zurker
 */
class TestSolarHelmetChargeEvent {

    private static ServerMock server;
    private static Slimefun plugin;

    private static SolarHelmet helmet;
    private static Jetpack jetpack;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "solar_helmet_test");

        SlimefunItemStack helmetStack = new SlimefunItemStack("_TEST_SOLAR_HELMET", Material.GOLDEN_HELMET, "&6Test Helmet");
        Slimefun.getItemCfg().setValue("_TEST_SOLAR_HELMET.enabled", true);
        helmet = new SolarHelmet(itemGroup, helmetStack, RecipeType.NULL, new ItemStack[9], 1.0);
        helmet.register(plugin);

        SlimefunItemStack jetpackStack = new SlimefunItemStack("_TEST_JETPACK_CHARGE", Material.IRON_CHESTPLATE, "&bTest Jetpack");
        Slimefun.getItemCfg().setValue("_TEST_JETPACK_CHARGE.enabled", true);
        jetpack = new Jetpack(itemGroup, jetpackStack, new ItemStack[9], 0.5, 100F);
        jetpack.register(plugin);
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
     * Equips a freshly cloned, uncharged jetpack to the player's chestplate slot and returns the
     * exact stack stored in the inventory (MockBukkit clones on set, so the stored reference is
     * the one the helmet will actually charge).
     */
    private ItemStack equipJetpack(Player player) {
        player.getInventory().setChestplate(jetpack.getItem().clone());
        return player.getInventory().getChestplate();
    }

    @Test
    @DisplayName("SolarHelmetChargeEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Player player = server.addPlayer();
        ItemStack item = new ItemStack(Material.IRON_CHESTPLATE);
        Rechargeable rechargeable = Mockito.mock(Rechargeable.class);

        SolarHelmetChargeEvent event = new SolarHelmetChargeEvent(player, helmet, item, rechargeable, 1F);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(helmet, event.getHelmet());
        Assertions.assertEquals(item, event.getItem());
        Assertions.assertEquals(rechargeable, event.getRechargeable());
        Assertions.assertEquals(1F, event.getCharge());
        Assertions.assertFalse(event.isCancelled());

        event.setCharge(2.5F);
        Assertions.assertEquals(2.5F, event.getCharge());

        // Zero and negative charge are rejected (Rechargeable#addItemCharge requires > 0)
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setCharge(0));
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setCharge(-1));

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new SolarHelmetChargeEvent(player, null, item, rechargeable, 1F));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new SolarHelmetChargeEvent(player, helmet, null, rechargeable, 1F));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new SolarHelmetChargeEvent(player, helmet, item, null, 1F));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new SolarHelmetChargeEvent(player, helmet, item, rechargeable, 0));
    }

    @Test
    @DisplayName("rechargeItems fires the event per item and adds the configured charge")
    void testRechargeItemsFiresAndCharges() {
        Player player = server.addPlayer();
        ItemStack equipped = equipJetpack(player);
        Assertions.assertEquals(0F, jetpack.getItemCharge(equipped), 0.0001F);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onCharge(SolarHelmetChargeEvent event) {
                seen[0] = true;
                Assertions.assertEquals(helmet, event.getHelmet());
                Assertions.assertEquals(jetpack, event.getRechargeable());
                Assertions.assertEquals(1F, event.getCharge(), 0.0001F);
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            helmet.rechargeItems(player);

            Assertions.assertTrue(seen[0], "SolarHelmetChargeEvent was not fired");
            Assertions.assertEquals(1F, jetpack.getItemCharge(equipped), 0.0001F, "The jetpack must have been charged by the helmet");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling SolarHelmetChargeEvent skips the charge for that item")
    void testCancelEventSkipsCharge() {
        Player player = server.addPlayer();
        ItemStack equipped = equipJetpack(player);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onCharge(SolarHelmetChargeEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            helmet.rechargeItems(player);

            Assertions.assertEquals(0F, jetpack.getItemCharge(equipped), 0.0001F, "A cancelled charge must not add any charge");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Overriding the charge via setCharge changes the added amount")
    void testChargeOverride() {
        Player player = server.addPlayer();
        ItemStack equipped = equipJetpack(player);

        Listener overriding = new Listener() {
            @EventHandler
            public void onCharge(SolarHelmetChargeEvent event) {
                event.setCharge(5F);
            }
        };
        server.getPluginManager().registerEvents(overriding, plugin);

        try {
            helmet.rechargeItems(player);

            Assertions.assertEquals(5F, jetpack.getItemCharge(equipped), 0.0001F, "The jetpack must have been charged by the overridden amount");
        } finally {
            HandlerList.unregisterAll(overriding);
        }
    }

    @Test
    @DisplayName("rechargeItems without listeners still charges, preserving the old behavior")
    void testRechargeWithoutListenersStillCharges() {
        Player player = server.addPlayer();
        ItemStack equipped = equipJetpack(player);

        helmet.rechargeItems(player);

        Assertions.assertEquals(1F, jetpack.getItemCharge(equipped), 0.0001F, "The jetpack must have been charged by the helmet");
    }
}
