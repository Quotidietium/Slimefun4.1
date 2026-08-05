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
import io.github.thebusybiscuit.slimefun4.api.events.SplintHealEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;
import io.github.thebusybiscuit.slimefun4.utils.compatibility.VersionedPotionEffectType;

/**
 * Regression coverage for the splint API expansion: {@link SplintHealEvent}, exercised by
 * driving the real {@link Splint}
 * {@link io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler} with a constructed
 * {@link PlayerRightClickEvent}.
 * <p>
 * MockBukkit clones the ItemStack in setItemInMainHand, so the event is handed the reference
 * actually stored in the inventory for consumeItem(e.getItem()) to be visible.
 *
 * @author Zurker
 */
class TestSplintHealEvent {

    private static ServerMock server;
    private static Slimefun plugin;

    private static Splint splint;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "splint_test");
        SlimefunItemStack stack = new SlimefunItemStack("_TEST_SPLINT", Material.STICK, "&fTest Splint");
        Slimefun.getItemCfg().setValue("_TEST_SPLINT.enabled", true);
        splint = new Splint(itemGroup, stack, RecipeType.NULL, new ItemStack[9], new ItemStack(Material.STICK));
        splint.register(plugin);
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
     * Puts three splints in the injured player's main hand and applies one via the real handler.
     */
    private void apply(Player player) {
        ItemStack item = splint.getItem().clone();
        item.setAmount(3);
        player.getInventory().setItemInMainHand(item);
        player.setHealth(10);

        ItemStack handItem = player.getInventory().getItemInMainHand();
        PlayerInteractEvent interactEvent = new PlayerInteractEvent(player, Action.RIGHT_CLICK_AIR, handItem, null, null);
        splint.getItemHandler().onRightClick(new PlayerRightClickEvent(interactEvent));
    }

    @Test
    @DisplayName("SplintHealEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Player player = server.addPlayer();
        PotionEffect effect = new PotionEffect(VersionedPotionEffectType.INSTANT_HEALTH, 1, 0);

        SplintHealEvent event = new SplintHealEvent(player, splint, effect);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(splint, event.getSplint());
        Assertions.assertEquals(effect, event.getEffect());
        Assertions.assertFalse(event.isCancelled());

        PotionEffect swapped = new PotionEffect(PotionEffectType.REGENERATION, 40, 1);
        event.setEffect(swapped);
        Assertions.assertEquals(swapped, event.getEffect());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new SplintHealEvent(player, null, effect));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new SplintHealEvent(player, splint, null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setEffect(null));
    }

    @Test
    @DisplayName("Using a splint fires the event, consumes it and applies instant health")
    void testUseFiresAndAppliesEffect() {
        Player player = server.addPlayer();

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onHeal(SplintHealEvent event) {
                seen[0] = true;
                Assertions.assertEquals(splint, event.getSplint());
                Assertions.assertEquals(VersionedPotionEffectType.INSTANT_HEALTH, event.getEffect().getType());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            apply(player);

            Assertions.assertTrue(seen[0], "SplintHealEvent was not fired");
            Assertions.assertTrue(player.getInventory().getItemInMainHand().getAmount() < 3, "The splint must have been consumed");
            Assertions.assertNotNull(player.getPotionEffect(VersionedPotionEffectType.INSTANT_HEALTH), "Instant health must have been applied");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling SplintHealEvent keeps the splint and applies no effect")
    void testEventCancellationSkipsHeal() {
        Player player = server.addPlayer();

        Listener cancelling = new Listener() {
            @EventHandler
            public void onHeal(SplintHealEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            apply(player);

            Assertions.assertEquals(3, player.getInventory().getItemInMainHand().getAmount(), "A cancelled splint use must keep the splint");
            Assertions.assertNull(player.getPotionEffect(VersionedPotionEffectType.INSTANT_HEALTH), "A cancelled splint use must not apply instant health");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Swapping the effect via setEffect applies the replacement instead of instant health")
    void testEffectSwap() {
        Player player = server.addPlayer();

        Listener swapping = new Listener() {
            @EventHandler
            public void onHeal(SplintHealEvent event) {
                event.setEffect(new PotionEffect(PotionEffectType.REGENERATION, 40, 1));
            }
        };
        server.getPluginManager().registerEvents(swapping, plugin);

        try {
            apply(player);

            Assertions.assertNull(player.getPotionEffect(VersionedPotionEffectType.INSTANT_HEALTH), "Instant health must have been replaced");
            PotionEffect applied = player.getPotionEffect(PotionEffectType.REGENERATION);
            Assertions.assertNotNull(applied, "The swapped effect must have been applied");
            Assertions.assertEquals(40, applied.getDuration());
            Assertions.assertEquals(1, applied.getAmplifier());
        } finally {
            HandlerList.unregisterAll(swapping);
        }
    }

    @Test
    @DisplayName("A healthy and unburnt player neither fires the event nor consumes the splint")
    void testHealthyPlayerSkipsSplint() {
        Player player = server.addPlayer();

        ItemStack item = splint.getItem().clone();
        item.setAmount(3);
        player.getInventory().setItemInMainHand(item);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onHeal(SplintHealEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            ItemStack handItem = player.getInventory().getItemInMainHand();
            PlayerInteractEvent interactEvent = new PlayerInteractEvent(player, Action.RIGHT_CLICK_AIR, handItem, null, null);
            splint.getItemHandler().onRightClick(new PlayerRightClickEvent(interactEvent));

            Assertions.assertFalse(seen[0], "SplintHealEvent must not fire for a healthy player");
            Assertions.assertEquals(3, player.getInventory().getItemInMainHand().getAmount(), "Nothing must be consumed");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Using a splint without listeners still applies instant health, preserving the old behavior")
    void testUseWithoutListenersStillAppliesEffect() {
        Player player = server.addPlayer();

        apply(player);

        Assertions.assertTrue(player.getInventory().getItemInMainHand().getAmount() < 3, "The splint must have been consumed");
        Assertions.assertNotNull(player.getPotionEffect(VersionedPotionEffectType.INSTANT_HEALTH), "Instant health must have been applied");
    }
}
