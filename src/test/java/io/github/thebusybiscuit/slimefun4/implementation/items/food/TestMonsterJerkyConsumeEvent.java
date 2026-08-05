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

import io.github.thebusybiscuit.slimefun4.api.events.MonsterJerkyConsumeEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Regression coverage for the monster jerky API expansion: {@link MonsterJerkyConsumeEvent},
 * exercised by driving the real {@link MonsterJerky}
 * {@link io.github.thebusybiscuit.slimefun4.core.handlers.ItemConsumptionHandler} with a
 * constructed {@link PlayerItemConsumeEvent}.
 * <p>
 * The jerky applies its effect one tick later via a delayed sync task, so the tests advance
 * the scheduler after consuming.
 *
 * @author Zurker
 */
class TestMonsterJerkyConsumeEvent {

    private static ServerMock server;
    private static Slimefun plugin;

    private static MonsterJerky jerky;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "monster_jerky_test");
        SlimefunItemStack stack = new SlimefunItemStack("_TEST_MONSTER_JERKY", Material.ROTTEN_FLESH, "&aTest Monster Jerky");
        Slimefun.getItemCfg().setValue("_TEST_MONSTER_JERKY.enabled", true);
        jerky = new MonsterJerky(itemGroup, stack, RecipeType.NULL, new ItemStack[9]);
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
     * Feeds the jerky to the player via the real consumption handler and advances the
     * scheduler so the delayed effect task runs.
     */
    private void consume(Player player) {
        ItemStack item = jerky.getItem().clone();
        player.getInventory().setItemInMainHand(item);
        jerky.getItemHandler().onConsume(new PlayerItemConsumeEvent(player, item, EquipmentSlot.HAND), player, item);
        server.getScheduler().performTicks(2);
    }

    @Test
    @DisplayName("MonsterJerkyConsumeEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Player player = server.addPlayer();
        PotionEffect effect = new PotionEffect(PotionEffectType.SATURATION, 5, 0);

        MonsterJerkyConsumeEvent event = new MonsterJerkyConsumeEvent(player, jerky, effect);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(jerky, event.getJerky());
        Assertions.assertEquals(effect, event.getEffect());
        Assertions.assertFalse(event.isCancelled());

        PotionEffect swapped = new PotionEffect(PotionEffectType.ABSORPTION, 100, 1);
        event.setEffect(swapped);
        Assertions.assertEquals(swapped, event.getEffect());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new MonsterJerkyConsumeEvent(player, null, effect));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new MonsterJerkyConsumeEvent(player, jerky, null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setEffect(null));
    }

    @Test
    @DisplayName("Consuming monster jerky fires the event, removes hunger and applies saturation")
    void testConsumeFiresAndAppliesSaturation() {
        Player player = server.addPlayer();
        player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 200, 0));

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onConsume(MonsterJerkyConsumeEvent event) {
                seen[0] = true;
                Assertions.assertEquals(jerky, event.getJerky());
                Assertions.assertEquals(PotionEffectType.SATURATION, event.getEffect().getType());
                Assertions.assertEquals(5, event.getEffect().getDuration());
                Assertions.assertEquals(0, event.getEffect().getAmplifier());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            consume(player);

            Assertions.assertTrue(seen[0], "MonsterJerkyConsumeEvent was not fired");
            Assertions.assertNull(player.getPotionEffect(PotionEffectType.HUNGER), "Hunger must have been removed");
            PotionEffect applied = player.getPotionEffect(PotionEffectType.SATURATION);
            Assertions.assertNotNull(applied, "Saturation must have been applied");
            Assertions.assertEquals(5, applied.getDuration());
            Assertions.assertEquals(0, applied.getAmplifier());
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling MonsterJerkyConsumeEvent keeps hunger and applies no saturation")
    void testEventCancellationSkipsEffect() {
        Player player = server.addPlayer();
        player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 200, 0));

        Listener cancelling = new Listener() {
            @EventHandler
            public void onConsume(MonsterJerkyConsumeEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            consume(player);

            Assertions.assertNotNull(player.getPotionEffect(PotionEffectType.HUNGER), "A cancelled consumption must not remove hunger");
            Assertions.assertNull(player.getPotionEffect(PotionEffectType.SATURATION), "A cancelled consumption must not apply saturation");
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
            public void onConsume(MonsterJerkyConsumeEvent event) {
                event.setEffect(new PotionEffect(PotionEffectType.ABSORPTION, 100, 1));
            }
        };
        server.getPluginManager().registerEvents(swapping, plugin);

        try {
            consume(player);

            Assertions.assertNull(player.getPotionEffect(PotionEffectType.SATURATION), "Saturation must have been replaced");
            PotionEffect applied = player.getPotionEffect(PotionEffectType.ABSORPTION);
            Assertions.assertNotNull(applied, "The swapped effect must have been applied");
            Assertions.assertEquals(100, applied.getDuration());
            Assertions.assertEquals(1, applied.getAmplifier());
        } finally {
            HandlerList.unregisterAll(swapping);
        }
    }

    @Test
    @DisplayName("Consuming monster jerky without listeners still applies saturation, preserving the old behavior")
    void testConsumeWithoutListenersStillAppliesSaturation() {
        Player player = server.addPlayer();

        consume(player);

        PotionEffect applied = player.getPotionEffect(PotionEffectType.SATURATION);
        Assertions.assertNotNull(applied, "Saturation must have been applied");
        Assertions.assertEquals(5, applied.getDuration());
        Assertions.assertEquals(0, applied.getAmplifier());
    }
}
