package io.github.thebusybiscuit.slimefun4.implementation.items.food;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.EquipmentSlot;
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

import io.github.thebusybiscuit.slimefun4.api.events.JuiceDrinkEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Regression coverage for the juice API expansion: {@link JuiceDrinkEvent}, exercised by
 * driving the real {@link Juice}
 * {@link io.github.thebusybiscuit.slimefun4.core.handlers.ItemConsumptionHandler} with a
 * constructed {@link PlayerItemConsumeEvent}.
 * <p>
 * MockBukkit clones the ItemStack in setItemInMainHand, so the handler is handed the reference
 * actually stored in the inventory for the glass bottle removal to be visible.
 *
 * @author Zurker
 */
class TestJuiceDrinkEvent {

    private static ServerMock server;
    private static Slimefun plugin;

    private static Juice juice;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "juice_test");
        SlimefunItemStack stack = new SlimefunItemStack("_TEST_JUICE", Color.YELLOW, new PotionEffect(PotionEffectType.SATURATION, 100, 1), "&eTest Juice");
        Slimefun.getItemCfg().setValue("_TEST_JUICE.enabled", true);
        juice = new Juice(itemGroup, stack, RecipeType.NULL, new ItemStack[9]);
        juice.register(plugin);
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
     * Puts a single juice in the player's main hand and drinks it via the real handler.
     */
    private void drink(Player player) {
        player.getInventory().setItemInMainHand(juice.getItem().clone());
        ItemStack handItem = player.getInventory().getItemInMainHand();
        juice.getItemHandler().onConsume(new PlayerItemConsumeEvent(player, handItem, EquipmentSlot.HAND), player, handItem);
    }

    @Test
    @DisplayName("JuiceDrinkEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Player player = server.addPlayer();
        ItemStack item = juice.getItem().clone();
        PotionEffect effect = new PotionEffect(PotionEffectType.SATURATION, 100, 1);

        JuiceDrinkEvent event = new JuiceDrinkEvent(player, juice, item, effect);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(juice, event.getJuice());
        Assertions.assertEquals(item, event.getItem());
        Assertions.assertEquals(effect, event.getEffect());
        Assertions.assertFalse(event.isCancelled());

        PotionEffect swapped = new PotionEffect(PotionEffectType.ABSORPTION, 60, 0);
        event.setEffect(swapped);
        Assertions.assertEquals(swapped, event.getEffect());

        event.setEffect(null);
        Assertions.assertNull(event.getEffect(), "A null effect must be accepted to skip the effect");

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new JuiceDrinkEvent(player, null, item, effect));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new JuiceDrinkEvent(player, juice, null, effect));

        Assertions.assertTrue(event.isRemoveBottle(), "The bottle removal must default to true");
        event.setRemoveBottle(false);
        Assertions.assertFalse(event.isRemoveBottle());
    }

    @Test
    @DisplayName("Keeping the bottle via setRemoveBottle still applies the effect")
    void testKeepBottleStillAppliesEffect() {
        Player player = server.addPlayer();

        Listener keeping = new Listener() {
            @EventHandler
            public void onDrink(JuiceDrinkEvent event) {
                event.setRemoveBottle(false);
            }
        };
        server.getPluginManager().registerEvents(keeping, plugin);

        try {
            drink(player);

            PotionEffect applied = player.getPotionEffect(PotionEffectType.SATURATION);
            Assertions.assertNotNull(applied, "Saturation must still have been applied");
            Assertions.assertEquals(1, player.getInventory().getItemInMainHand().getAmount(), "The bottle must have been kept");
        } finally {
            HandlerList.unregisterAll(keeping);
        }
    }

    @Test
    @DisplayName("Drinking juice fires the event, applies saturation and removes the glass bottle")
    void testDrinkFiresAndAppliesEffect() {
        Player player = server.addPlayer();

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onDrink(JuiceDrinkEvent event) {
                seen[0] = true;
                Assertions.assertEquals(juice, event.getJuice());
                Assertions.assertNotNull(event.getEffect());
                Assertions.assertEquals(PotionEffectType.SATURATION, event.getEffect().getType());
                Assertions.assertEquals(100, event.getEffect().getDuration());
                Assertions.assertEquals(1, event.getEffect().getAmplifier());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            drink(player);

            Assertions.assertTrue(seen[0], "JuiceDrinkEvent was not fired");
            PotionEffect applied = player.getPotionEffect(PotionEffectType.SATURATION);
            Assertions.assertNotNull(applied, "Saturation must have been applied");
            Assertions.assertEquals(100, applied.getDuration());
            Assertions.assertEquals(1, applied.getAmplifier());
            Assertions.assertEquals(0, player.getInventory().getItemInMainHand().getAmount(), "The empty bottle must have been removed");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling JuiceDrinkEvent applies no effect and keeps the bottle")
    void testEventCancellationSkipsDrink() {
        Player player = server.addPlayer();

        Listener cancelling = new Listener() {
            @EventHandler
            public void onDrink(JuiceDrinkEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            drink(player);

            Assertions.assertNull(player.getPotionEffect(PotionEffectType.SATURATION), "A cancelled drink must not apply saturation");
            Assertions.assertEquals(1, player.getInventory().getItemInMainHand().getAmount(), "A cancelled drink must keep the bottle");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Swapping the effect via setEffect applies the replacement instead of saturation")
    void testEffectSwap() {
        Player player = server.addPlayer();

        Listener swapping = new Listener() {
            @EventHandler
            public void onDrink(JuiceDrinkEvent event) {
                event.setEffect(new PotionEffect(PotionEffectType.ABSORPTION, 60, 0));
            }
        };
        server.getPluginManager().registerEvents(swapping, plugin);

        try {
            drink(player);

            Assertions.assertNull(player.getPotionEffect(PotionEffectType.SATURATION), "Saturation must have been replaced");
            PotionEffect applied = player.getPotionEffect(PotionEffectType.ABSORPTION);
            Assertions.assertNotNull(applied, "The swapped effect must have been applied");
            Assertions.assertEquals(60, applied.getDuration());
            Assertions.assertEquals(0, applied.getAmplifier());
        } finally {
            HandlerList.unregisterAll(swapping);
        }
    }

    @Test
    @DisplayName("Nulling the effect via setEffect applies no effect but still removes the bottle")
    void testEffectRemoval() {
        Player player = server.addPlayer();

        Listener removing = new Listener() {
            @EventHandler
            public void onDrink(JuiceDrinkEvent event) {
                event.setEffect(null);
            }
        };
        server.getPluginManager().registerEvents(removing, plugin);

        try {
            drink(player);

            Assertions.assertNull(player.getPotionEffect(PotionEffectType.SATURATION), "No effect must have been applied");
            Assertions.assertEquals(0, player.getInventory().getItemInMainHand().getAmount(), "The empty bottle must still have been removed");
        } finally {
            HandlerList.unregisterAll(removing);
        }
    }

    @Test
    @DisplayName("Drinking juice without listeners still applies saturation, preserving the old behavior")
    void testDrinkWithoutListenersStillAppliesEffect() {
        Player player = server.addPlayer();

        drink(player);

        PotionEffect applied = player.getPotionEffect(PotionEffectType.SATURATION);
        Assertions.assertNotNull(applied, "Saturation must have been applied");
        Assertions.assertEquals(100, applied.getDuration());
        Assertions.assertEquals(1, applied.getAmplifier());
        Assertions.assertEquals(0, player.getInventory().getItemInMainHand().getAmount(), "The empty bottle must have been removed");
    }
}
