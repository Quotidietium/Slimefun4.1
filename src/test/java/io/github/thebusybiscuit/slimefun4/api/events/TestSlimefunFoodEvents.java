package io.github.thebusybiscuit.slimefun4.api.events;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemConsumeEvent;
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
import be.seeseemelk.mockbukkit.entity.PlayerMock;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.core.attributes.SlimefunFood;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.food.DietCookie;
import io.github.thebusybiscuit.slimefun4.implementation.items.food.FortuneCookie;
import io.github.thebusybiscuit.slimefun4.implementation.items.food.Juice;
import io.github.thebusybiscuit.slimefun4.implementation.items.food.MeatJerky;
import io.github.thebusybiscuit.slimefun4.implementation.items.food.MonsterJerky;
import io.github.thebusybiscuit.slimefun4.implementation.listeners.SlimefunItemConsumeListener;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Regression coverage for the food / diet API expansion: {@link SlimefunFood} marker and
 * {@link SlimefunItemConsumeEvent}.
 *
 * @author Zurker
 */
class TestSlimefunFoodEvents {

    private static ServerMock server;
    private static Slimefun plugin;
    private static SlimefunItem testItem;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);

        // The consume listener self-registers in its constructor.
        new SlimefunItemConsumeListener(plugin);

        testItem = TestUtilities.mockSlimefunItem(plugin, "FOOD_TEST_ITEM", new ItemStack(Material.BREAD));
        testItem.register(plugin);
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
    @DisplayName("Built-in food classes implement SlimefunFood")
    void testBuiltInFoodsImplementSlimefunFood() {
        // Juice is the base of all juice variants, so it covers them via inheritance
        Assertions.assertTrue(SlimefunFood.class.isAssignableFrom(Juice.class));
        Assertions.assertTrue(SlimefunFood.class.isAssignableFrom(DietCookie.class));
        Assertions.assertTrue(SlimefunFood.class.isAssignableFrom(FortuneCookie.class));
        Assertions.assertTrue(SlimefunFood.class.isAssignableFrom(MonsterJerky.class));
        Assertions.assertTrue(SlimefunFood.class.isAssignableFrom(MeatJerky.class));
        // A plain SlimefunItem is not food
        Assertions.assertFalse(SlimefunFood.class.isAssignableFrom(SlimefunItem.class));
    }

    @Test
    @DisplayName("SlimefunItemConsumeEvent fires when a Slimefun item is consumed")
    void testConsumeEventFiresFromListener() {
        PlayerMock player = new PlayerMock(server, "eater");
        server.addPlayer(player);

        ItemStack item = testItem.getItem();
        player.getInventory().setItemInMainHand(item);

        PlayerItemConsumeEvent bukkitEvent = new PlayerItemConsumeEvent(player, item);
        server.getPluginManager().callEvent(bukkitEvent);

        server.getPluginManager().assertEventFired(SlimefunItemConsumeEvent.class, e -> {
            Assertions.assertEquals(player, e.getPlayer());
            Assertions.assertEquals(testItem, e.getSlimefunItem());
            Assertions.assertEquals(item, e.getItem());
            Assertions.assertEquals(bukkitEvent, e.getPlayerItemConsumeEvent());
            Assertions.assertFalse(e.isCancelled());
            // The plain mock item is not tagged as a SlimefunFood
            Assertions.assertFalse(e.isFood());
            return true;
        });
    }

    @Test
    @DisplayName("SlimefunItemConsumeEvent delegates cancellation to the underlying event")
    void testConsumeEventDelegates() {
        PlayerMock player = new PlayerMock(server, "delegater");

        PlayerItemConsumeEvent underlying = Mockito.mock(PlayerItemConsumeEvent.class);
        Mockito.when(underlying.isCancelled()).thenReturn(false);

        SlimefunItemConsumeEvent event = new SlimefunItemConsumeEvent(player, testItem, testItem.getItem(), underlying);

        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Mockito.verify(underlying).setCancelled(true);
    }
}
