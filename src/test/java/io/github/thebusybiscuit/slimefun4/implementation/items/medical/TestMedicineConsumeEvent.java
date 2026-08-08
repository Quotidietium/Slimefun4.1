package io.github.thebusybiscuit.slimefun4.implementation.items.medical;

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

import io.github.thebusybiscuit.slimefun4.api.events.MedicineConsumeEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;
import io.github.thebusybiscuit.slimefun4.utils.RadiationUtils;

/**
 * Regression coverage for the medicine API expansion: {@link MedicineConsumeEvent},
 * exercised by driving the real {@link Medicine}
 * {@link io.github.thebusybiscuit.slimefun4.core.handlers.ItemConsumptionHandler} with a
 * constructed {@link PlayerItemConsumeEvent}.
 *
 * @author Zurker
 */
class TestMedicineConsumeEvent {

    private static ServerMock server;
    private static Slimefun plugin;

    private static Medicine medicine;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "medicine_test");
        SlimefunItemStack stack = new SlimefunItemStack("_TEST_MEDICINE", Material.POTION, "&cTest Medicine");
        Slimefun.getItemCfg().setValue("_TEST_MEDICINE.enabled", true);
        medicine = new Medicine(itemGroup, stack, RecipeType.NULL, new ItemStack[9]);
        medicine.register(plugin);
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
     * Afflicts the player (injury, poison, fire, radiation) and consumes the medicine via the
     * real consumption handler.
     */
    private void consumeAfflicted(Player player) {
        player.setHealth(10.0);
        player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 200, 0));
        player.setFireTicks(40);
        RadiationUtils.addExposure(player, 30);

        ItemStack item = medicine.getItem().clone();
        player.getInventory().setItemInMainHand(item);
        medicine.getItemHandler().onConsume(new PlayerItemConsumeEvent(player, item, EquipmentSlot.HAND), player, item);
    }

    @Test
    @DisplayName("MedicineConsumeEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Player player = server.addPlayer();

        MedicineConsumeEvent event = new MedicineConsumeEvent(player, medicine, 8);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(medicine, event.getMedicine());
        Assertions.assertEquals(8, event.getHealAmount());
        Assertions.assertFalse(event.isCancelled());

        event.setHealAmount(20);
        Assertions.assertEquals(20, event.getHealAmount());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new MedicineConsumeEvent(player, null, 8));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new MedicineConsumeEvent(player, medicine, -1));
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setHealAmount(-1));
    }

    @Test
    @DisplayName("Consuming medicine fires the event and cures the afflictions")
    void testConsumeFiresAndCures() {
        Player player = server.addPlayer();

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onConsume(MedicineConsumeEvent event) {
                seen[0] = true;
                Assertions.assertEquals(medicine, event.getMedicine());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            consumeAfflicted(player);

            Assertions.assertTrue(seen[0], "MedicineConsumeEvent was not fired");
            Assertions.assertFalse(player.hasPotionEffect(PotionEffectType.POISON), "The poison must have been cured");
            Assertions.assertEquals(0, RadiationUtils.getExposure(player), "The radiation exposure must have been cleared");
            Assertions.assertEquals(0, player.getFireTicks(), "The fire must have been extinguished");
            Assertions.assertEquals(18.0, player.getHealth(), "The player must have been healed by 8");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling MedicineConsumeEvent keeps all afflictions")
    void testEventCancellationSkipsCure() {
        Player player = server.addPlayer();

        Listener cancelling = new Listener() {
            @EventHandler
            public void onConsume(MedicineConsumeEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            consumeAfflicted(player);

            Assertions.assertTrue(player.hasPotionEffect(PotionEffectType.POISON), "A cancelled cure must keep the poison");
            Assertions.assertEquals(30, RadiationUtils.getExposure(player), "A cancelled cure must keep the radiation exposure");
            Assertions.assertEquals(40, player.getFireTicks(), "A cancelled cure must keep the fire");
            Assertions.assertEquals(10.0, player.getHealth(), "A cancelled cure must keep the health");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Consuming medicine without listeners still cures, preserving the old behavior")
    void testConsumeWithoutListenersStillCures() {
        Player player = server.addPlayer();

        consumeAfflicted(player);

        Assertions.assertFalse(player.hasPotionEffect(PotionEffectType.POISON), "The poison must have been cured");
        Assertions.assertEquals(0, RadiationUtils.getExposure(player), "The radiation exposure must have been cleared");
        Assertions.assertEquals(0, player.getFireTicks(), "The fire must have been extinguished");
        Assertions.assertEquals(18.0, player.getHealth(), "The player must have been healed by 8");
    }
}
