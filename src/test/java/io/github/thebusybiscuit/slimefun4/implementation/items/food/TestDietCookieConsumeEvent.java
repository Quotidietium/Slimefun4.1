package io.github.thebusybiscuit.slimefun4.implementation.items.food;

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

import io.github.thebusybiscuit.slimefun4.api.events.DietCookieConsumeEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Regression coverage for the diet cookie API expansion: {@link DietCookieConsumeEvent},
 * exercised by driving the real {@link DietCookie}
 * {@link io.github.thebusybiscuit.slimefun4.core.handlers.ItemConsumptionHandler} with a
 * constructed {@link PlayerItemConsumeEvent}.
 *
 * @author Zurker
 */
class TestDietCookieConsumeEvent {

    private static ServerMock server;
    private static Slimefun plugin;

    private static DietCookie cookie;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "diet_cookie_test");
        SlimefunItemStack stack = new SlimefunItemStack("_TEST_DIET_COOKIE", Material.COOKIE, "&eTest Diet Cookie");
        Slimefun.getItemCfg().setValue("_TEST_DIET_COOKIE.enabled", true);
        cookie = new DietCookie(itemGroup, stack, RecipeType.NULL, new ItemStack[9]);
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
    private void consume(Player player) {
        ItemStack item = cookie.getItem().clone();
        player.getInventory().setItemInMainHand(item);
        cookie.getItemHandler().onConsume(new PlayerItemConsumeEvent(player, item, EquipmentSlot.HAND), player, item);
    }

    @Test
    @DisplayName("DietCookieConsumeEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Player player = server.addPlayer();
        PotionEffect effect = PotionEffectType.LEVITATION.createEffect(60, 1);

        DietCookieConsumeEvent event = new DietCookieConsumeEvent(player, cookie, effect);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(cookie, event.getCookie());
        Assertions.assertEquals(effect, event.getEffect());
        Assertions.assertFalse(event.isCancelled());

        PotionEffect swapped = PotionEffectType.SPEED.createEffect(100, 2);
        event.setEffect(swapped);
        Assertions.assertEquals(swapped, event.getEffect());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new DietCookieConsumeEvent(player, null, effect));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new DietCookieConsumeEvent(player, cookie, null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setEffect(null));
    }

    @Test
    @DisplayName("Consuming a diet cookie fires the event and applies levitation")
    void testConsumeFiresAndAppliesLevitation() {
        Player player = server.addPlayer();

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onConsume(DietCookieConsumeEvent event) {
                seen[0] = true;
                Assertions.assertEquals(cookie, event.getCookie());
                Assertions.assertEquals(PotionEffectType.LEVITATION, event.getEffect().getType());
                Assertions.assertEquals(60, event.getEffect().getDuration());
                Assertions.assertEquals(1, event.getEffect().getAmplifier());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            consume(player);

            Assertions.assertTrue(seen[0], "DietCookieConsumeEvent was not fired");
            PotionEffect applied = player.getPotionEffect(PotionEffectType.LEVITATION);
            Assertions.assertNotNull(applied, "Levitation must have been applied");
            Assertions.assertEquals(60, applied.getDuration());
            Assertions.assertEquals(1, applied.getAmplifier());
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling DietCookieConsumeEvent applies no effect")
    void testEventCancellationSkipsEffect() {
        Player player = server.addPlayer();

        Listener cancelling = new Listener() {
            @EventHandler
            public void onConsume(DietCookieConsumeEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            consume(player);

            Assertions.assertNull(player.getPotionEffect(PotionEffectType.LEVITATION), "A cancelled consumption must not apply levitation");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Swapping the effect via setEffect applies the replacement instead of levitation")
    void testEffectSwap() {
        Player player = server.addPlayer();

        Listener swapping = new Listener() {
            @EventHandler
            public void onConsume(DietCookieConsumeEvent event) {
                event.setEffect(PotionEffectType.SPEED.createEffect(100, 2));
            }
        };
        server.getPluginManager().registerEvents(swapping, plugin);

        try {
            consume(player);

            Assertions.assertNull(player.getPotionEffect(PotionEffectType.LEVITATION), "Levitation must have been replaced");
            PotionEffect applied = player.getPotionEffect(PotionEffectType.SPEED);
            Assertions.assertNotNull(applied, "The swapped effect must have been applied");
            Assertions.assertEquals(100, applied.getDuration());
            Assertions.assertEquals(2, applied.getAmplifier());
        } finally {
            HandlerList.unregisterAll(swapping);
        }
    }

    @Test
    @DisplayName("Consuming a diet cookie without listeners still applies levitation, preserving the old behavior")
    void testConsumeWithoutListenersStillAppliesLevitation() {
        Player player = server.addPlayer();

        consume(player);

        PotionEffect applied = player.getPotionEffect(PotionEffectType.LEVITATION);
        Assertions.assertNotNull(applied, "Levitation must have been applied");
        Assertions.assertEquals(60, applied.getDuration());
        Assertions.assertEquals(1, applied.getAmplifier());
    }
}
