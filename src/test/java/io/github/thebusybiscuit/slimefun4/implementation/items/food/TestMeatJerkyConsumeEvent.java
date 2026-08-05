package io.github.thebusybiscuit.slimefun4.implementation.items.food;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

import io.github.thebusybiscuit.slimefun4.api.events.MeatJerkyConsumeEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Regression coverage for the meat jerky API expansion: {@link MeatJerkyConsumeEvent},
 * exercised by driving the real {@link MeatJerky}
 * {@link io.github.thebusybiscuit.slimefun4.core.handlers.ItemConsumptionHandler} with a
 * constructed {@link PlayerItemConsumeEvent}.
 *
 * @author Zurker
 */
class TestMeatJerkyConsumeEvent {

    private static ServerMock server;
    private static Slimefun plugin;

    private static MeatJerky jerky;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "meat_jerky_test");
        SlimefunItemStack stack = new SlimefunItemStack("_TEST_MEAT_JERKY", Material.COOKED_BEEF, "&6Test Meat Jerky");
        Slimefun.getItemCfg().setValue("_TEST_MEAT_JERKY.enabled", true);
        jerky = new MeatJerky(itemGroup, stack, RecipeType.NULL, new ItemStack[9]);
        jerky.register(plugin);
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
     * Feeds the jerky to the player via the real consumption handler.
     */
    private void consume(Player player) {
        ItemStack item = jerky.getItem().clone();
        player.getInventory().setItemInMainHand(item);
        jerky.getItemHandler().onConsume(new PlayerItemConsumeEvent(player, item, EquipmentSlot.HAND), player, item);
    }

    @Test
    @DisplayName("MeatJerkyConsumeEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Player player = server.addPlayer();

        MeatJerkyConsumeEvent event = new MeatJerkyConsumeEvent(player, jerky, 6);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(jerky, event.getJerky());
        Assertions.assertEquals(6, event.getSaturation());
        Assertions.assertFalse(event.isCancelled());

        event.setSaturation(3);
        Assertions.assertEquals(3, event.getSaturation());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new MeatJerkyConsumeEvent(player, null, 6));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new MeatJerkyConsumeEvent(player, jerky, -1));
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setSaturation(-1));
    }

    @Test
    @DisplayName("Consuming meat jerky fires the event and adds its saturation")
    void testConsumeFiresAndAddsSaturation() {
        Player player = server.addPlayer();
        player.setSaturation(5);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onConsume(MeatJerkyConsumeEvent event) {
                seen[0] = true;
                Assertions.assertEquals(jerky, event.getJerky());
                Assertions.assertEquals(6, event.getSaturation(), "The default saturation-level is 6");
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            consume(player);

            Assertions.assertTrue(seen[0], "MeatJerkyConsumeEvent was not fired");
            Assertions.assertEquals(11, player.getSaturation(), 0.0001, "The jerky's saturation must have been added");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling MeatJerkyConsumeEvent grants no saturation")
    void testEventCancellationSkipsSaturation() {
        Player player = server.addPlayer();
        player.setSaturation(5);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onConsume(MeatJerkyConsumeEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            consume(player);

            Assertions.assertEquals(5, player.getSaturation(), 0.0001, "A cancelled consumption must not add saturation");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Adjusting the saturation via setSaturation changes the granted amount")
    void testSaturationAdjustment() {
        Player player = server.addPlayer();
        player.setSaturation(5);

        Listener adjusting = new Listener() {
            @EventHandler
            public void onConsume(MeatJerkyConsumeEvent event) {
                event.setSaturation(3);
            }
        };
        server.getPluginManager().registerEvents(adjusting, plugin);

        try {
            consume(player);

            Assertions.assertEquals(8, player.getSaturation(), 0.0001, "The adjusted saturation must have been added");
        } finally {
            HandlerList.unregisterAll(adjusting);
        }
    }

    @Test
    @DisplayName("Consuming meat jerky without listeners still adds saturation, preserving the old behavior")
    void testConsumeWithoutListenersStillAddsSaturation() {
        Player player = server.addPlayer();
        player.setSaturation(5);

        consume(player);

        Assertions.assertEquals(11, player.getSaturation(), 0.0001, "The jerky's saturation must have been added");
    }
}
