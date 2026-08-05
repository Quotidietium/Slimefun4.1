package io.github.thebusybiscuit.slimefun4.implementation.items.food;

import org.bukkit.Material;
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
import be.seeseemelk.mockbukkit.entity.PlayerMock;

import io.github.bakedlibs.dough.common.ChatColors;
import io.github.thebusybiscuit.slimefun4.api.events.FortuneCookieFortuneEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Regression coverage for the fortune cookie API expansion: {@link FortuneCookieFortuneEvent},
 * exercised by driving the real {@link FortuneCookie}
 * {@link io.github.thebusybiscuit.slimefun4.core.handlers.ItemConsumptionHandler} with a
 * constructed {@link PlayerItemConsumeEvent}.
 *
 * @author Zurker
 */
class TestFortuneCookieFortuneEvent {

    private static ServerMock server;
    private static Slimefun plugin;

    private static FortuneCookie cookie;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "fortune_cookie_test");
        SlimefunItemStack stack = new SlimefunItemStack("_TEST_FORTUNE_COOKIE", Material.COOKIE, "&6Test Fortune Cookie");
        Slimefun.getItemCfg().setValue("_TEST_FORTUNE_COOKIE.enabled", true);
        cookie = new FortuneCookie(itemGroup, stack, RecipeType.NULL, new ItemStack[9]);
        cookie.register(plugin);
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
     * Feeds the cookie to the player via the real consumption handler.
     */
    private void consume(PlayerMock player) {
        ItemStack item = cookie.getItem().clone();
        player.getInventory().setItemInMainHand(item);
        cookie.getItemHandler().onConsume(new PlayerItemConsumeEvent(player, item, EquipmentSlot.HAND), player, item);
    }

    @Test
    @DisplayName("FortuneCookieFortuneEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        PlayerMock player = server.addPlayer();

        FortuneCookieFortuneEvent event = new FortuneCookieFortuneEvent(player, cookie, "You will be lucky");

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(cookie, event.getCookie());
        Assertions.assertEquals("You will be lucky", event.getMessage());
        Assertions.assertFalse(event.isCancelled());

        event.setMessage("You will be rich");
        Assertions.assertEquals("You will be rich", event.getMessage());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new FortuneCookieFortuneEvent(player, null, "msg"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new FortuneCookieFortuneEvent(player, cookie, null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setMessage(null));
    }

    @Test
    @DisplayName("Consuming a fortune cookie fires the event and sends the rolled fortune")
    void testConsumeFiresAndSendsFortune() {
        PlayerMock player = server.addPlayer();

        String[] rolled = { null };
        Listener watcher = new Listener() {
            @EventHandler
            public void onFortune(FortuneCookieFortuneEvent event) {
                rolled[0] = event.getMessage();
                Assertions.assertEquals(cookie, event.getCookie());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            consume(player);

            Assertions.assertNotNull(rolled[0], "FortuneCookieFortuneEvent was not fired");
            Assertions.assertEquals(ChatColors.color(rolled[0]), player.nextMessage(), "The rolled fortune must have been sent");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling FortuneCookieFortuneEvent sends no message")
    void testEventCancellationSkipsFortune() {
        PlayerMock player = server.addPlayer();

        Listener cancelling = new Listener() {
            @EventHandler
            public void onFortune(FortuneCookieFortuneEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            consume(player);

            Assertions.assertNull(player.nextMessage(), "A cancelled fortune must not be sent");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Swapping the message via setMessage sends the replacement")
    void testMessageSwap() {
        PlayerMock player = server.addPlayer();

        Listener swapping = new Listener() {
            @EventHandler
            public void onFortune(FortuneCookieFortuneEvent event) {
                event.setMessage("A custom fortune");
            }
        };
        server.getPluginManager().registerEvents(swapping, plugin);

        try {
            consume(player);

            Assertions.assertEquals(ChatColors.color("A custom fortune"), player.nextMessage(), "The swapped fortune must have been sent");
        } finally {
            HandlerList.unregisterAll(swapping);
        }
    }

    @Test
    @DisplayName("Consuming a fortune cookie without listeners still sends a fortune, preserving the old behavior")
    void testConsumeWithoutListenersStillSendsFortune() {
        PlayerMock player = server.addPlayer();

        consume(player);

        Assertions.assertNotNull(player.nextMessage(), "A fortune must have been sent");
    }
}
