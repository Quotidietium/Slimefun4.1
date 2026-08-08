package io.github.thebusybiscuit.slimefun4.implementation.items.medical;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

import io.github.thebusybiscuit.slimefun4.api.events.PlayerRightClickEvent;
import io.github.thebusybiscuit.slimefun4.api.events.VitaminsCureEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;
import io.github.thebusybiscuit.slimefun4.utils.RadiationUtils;

/**
 * Regression coverage for the vitamins API expansion: {@link VitaminsCureEvent}, exercised by
 * driving the real {@link Vitamins} {@link io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler}
 * with a constructed {@link PlayerRightClickEvent}.
 *
 * @author Zurker
 */
class TestVitaminsCureEvent {

    private static ServerMock server;
    private static Slimefun plugin;

    private static Vitamins vitamins;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "vitamins_test");
        SlimefunItemStack stack = new SlimefunItemStack("_TEST_VITAMINS", Material.NETHER_WART, "&eTest Vitamins");
        Slimefun.getItemCfg().setValue("_TEST_VITAMINS.enabled", true);
        vitamins = new Vitamins(itemGroup, stack, RecipeType.NULL, new ItemStack[9]);
        vitamins.register(plugin);
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
     * Afflicts the player (injury, poison, fire, radiation), puts three vitamins in their main
     * hand and uses one via the real handler.
     */
    private void useOnAfflicted(Player player) {
        player.setHealth(10.0);
        player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 200, 0));
        player.setFireTicks(40);
        RadiationUtils.addExposure(player, 30);

        ItemStack item = vitamins.getItem().clone();
        item.setAmount(3);
        player.getInventory().setItemInMainHand(item);

        // MockBukkit clones the ItemStack in setItemInMainHand, so hand the event the reference
        // actually stored in the inventory for consumeItem(e.getItem()) to be visible.
        ItemStack handItem = player.getInventory().getItemInMainHand();
        PlayerInteractEvent interactEvent = new PlayerInteractEvent(player, Action.RIGHT_CLICK_AIR, handItem, null, null);
        PlayerRightClickEvent event = new PlayerRightClickEvent(interactEvent);
        vitamins.getItemHandler().onRightClick(event);
    }

    @Test
    @DisplayName("VitaminsCureEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Player player = server.addPlayer();

        VitaminsCureEvent event = new VitaminsCureEvent(player, vitamins, 8);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(vitamins, event.getVitamins());
        Assertions.assertEquals(8, event.getHealAmount());
        Assertions.assertFalse(event.isCancelled());

        // setHealAmount: override the healing
        event.setHealAmount(20);
        Assertions.assertEquals(20, event.getHealAmount());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new VitaminsCureEvent(player, null, 8));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new VitaminsCureEvent(player, vitamins, -1));
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setHealAmount(-1));
    }

    @Test
    @DisplayName("Using vitamins fires the event and cures the afflictions")
    void testUseFiresAndCures() {
        Player player = server.addPlayer();

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onCure(VitaminsCureEvent event) {
                seen[0] = true;
                Assertions.assertEquals(vitamins, event.getVitamins());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            useOnAfflicted(player);

            Assertions.assertTrue(seen[0], "VitaminsCureEvent was not fired");
            Assertions.assertTrue(player.getInventory().getItemInMainHand().getAmount() < 3, "The vitamins must have been consumed");
            Assertions.assertFalse(player.hasPotionEffect(PotionEffectType.POISON), "The poison must have been cured");
            Assertions.assertEquals(0, RadiationUtils.getExposure(player), "The radiation exposure must have been cleared");
            Assertions.assertEquals(0, player.getFireTicks(), "The fire must have been extinguished");
            Assertions.assertEquals(18.0, player.getHealth(), "The player must have been healed by 8");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling VitaminsCureEvent keeps the vitamins and all afflictions")
    void testEventCancellationSkipsCure() {
        Player player = server.addPlayer();

        Listener cancelling = new Listener() {
            @EventHandler
            public void onCure(VitaminsCureEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            useOnAfflicted(player);

            Assertions.assertEquals(3, player.getInventory().getItemInMainHand().getAmount(), "A cancelled cure must keep the vitamins");
            Assertions.assertTrue(player.hasPotionEffect(PotionEffectType.POISON), "A cancelled cure must keep the poison");
            Assertions.assertEquals(30, RadiationUtils.getExposure(player), "A cancelled cure must keep the radiation exposure");
            Assertions.assertEquals(40, player.getFireTicks(), "A cancelled cure must keep the fire");
            Assertions.assertEquals(10.0, player.getHealth(), "A cancelled cure must keep the health");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Using vitamins without listeners still cures, preserving the old behavior")
    void testUseWithoutListenersStillCures() {
        Player player = server.addPlayer();

        useOnAfflicted(player);

        Assertions.assertTrue(player.getInventory().getItemInMainHand().getAmount() < 3, "The vitamins must have been consumed");
        Assertions.assertFalse(player.hasPotionEffect(PotionEffectType.POISON), "The poison must have been cured");
        Assertions.assertEquals(0, RadiationUtils.getExposure(player), "The radiation exposure must have been cleared");
        Assertions.assertEquals(18.0, player.getHealth(), "The player must have been healed by 8");
    }
}
