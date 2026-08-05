package io.github.thebusybiscuit.slimefun4.implementation.items.food;

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

import io.github.thebusybiscuit.slimefun4.api.events.MagicSugarSpeedEvent;
import io.github.thebusybiscuit.slimefun4.api.events.PlayerRightClickEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Regression coverage for the magic sugar API expansion: {@link MagicSugarSpeedEvent},
 * exercised by driving the real {@link MagicSugar}
 * {@link io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler} with a constructed
 * {@link PlayerRightClickEvent}.
 *
 * @author Zurker
 */
class TestMagicSugarSpeedEvent {

    private static ServerMock server;
    private static Slimefun plugin;

    private static MagicSugar magicSugar;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "magic_sugar_test");
        SlimefunItemStack stack = new SlimefunItemStack("_TEST_MAGIC_SUGAR", Material.SUGAR, "&6Test Magic Sugar");
        Slimefun.getItemCfg().setValue("_TEST_MAGIC_SUGAR.enabled", true);
        magicSugar = new MagicSugar(itemGroup, stack, RecipeType.NULL, new ItemStack[9]);
        magicSugar.register(plugin);
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
     * Puts three sugar in the player's main hand and uses it via the real handler.
     */
    private void use(Player player) {
        ItemStack sugar = magicSugar.getItem().clone();
        sugar.setAmount(3);
        player.getInventory().setItemInMainHand(sugar);

        // MockBukkit clones the ItemStack in setItemInMainHand, so hand the event the reference
        // actually stored in the inventory for consumeItem(e.getItem()) to be visible.
        ItemStack handItem = player.getInventory().getItemInMainHand();
        PlayerInteractEvent interactEvent = new PlayerInteractEvent(player, Action.RIGHT_CLICK_AIR, handItem, null, null);
        PlayerRightClickEvent event = new PlayerRightClickEvent(interactEvent);
        magicSugar.getItemHandler().onRightClick(event);
    }

    @Test
    @DisplayName("MagicSugarSpeedEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Player player = server.addPlayer();
        PotionEffect effect = new PotionEffect(PotionEffectType.SPEED, 600, 3);

        MagicSugarSpeedEvent event = new MagicSugarSpeedEvent(player, magicSugar, effect);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(magicSugar, event.getMagicSugar());
        Assertions.assertEquals(effect, event.getEffect());
        Assertions.assertFalse(event.isCancelled());

        PotionEffect swapped = new PotionEffect(PotionEffectType.SPEED, 100, 1);
        event.setEffect(swapped);
        Assertions.assertEquals(swapped, event.getEffect());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new MagicSugarSpeedEvent(player, null, effect));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new MagicSugarSpeedEvent(player, magicSugar, null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setEffect(null));
    }

    @Test
    @DisplayName("Using magic sugar fires the event, consumes the sugar and applies speed")
    void testUseFiresAndAppliesSpeed() {
        Player player = server.addPlayer();

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onSpeed(MagicSugarSpeedEvent event) {
                seen[0] = true;
                Assertions.assertEquals(magicSugar, event.getMagicSugar());
                Assertions.assertEquals(PotionEffectType.SPEED, event.getEffect().getType());
                Assertions.assertEquals(600, event.getEffect().getDuration());
                Assertions.assertEquals(3, event.getEffect().getAmplifier());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            use(player);

            Assertions.assertTrue(seen[0], "MagicSugarSpeedEvent was not fired");
            Assertions.assertTrue(player.getInventory().getItemInMainHand().getAmount() < 3, "The sugar must have been consumed");

            PotionEffect applied = player.getPotionEffect(PotionEffectType.SPEED);
            Assertions.assertNotNull(applied, "The speed effect must have been applied");
            Assertions.assertEquals(600, applied.getDuration());
            Assertions.assertEquals(3, applied.getAmplifier());
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling MagicSugarSpeedEvent keeps the sugar and skips the effect")
    void testEventCancellationSkipsUse() {
        Player player = server.addPlayer();

        Listener cancelling = new Listener() {
            @EventHandler
            public void onSpeed(MagicSugarSpeedEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            use(player);

            Assertions.assertEquals(3, player.getInventory().getItemInMainHand().getAmount(), "A cancelled use must keep the sugar");
            Assertions.assertNull(player.getPotionEffect(PotionEffectType.SPEED), "A cancelled use must not apply speed");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Swapping the effect via setEffect replaces the applied potion effect")
    void testEffectSwap() {
        Player player = server.addPlayer();

        Listener swapping = new Listener() {
            @EventHandler
            public void onSpeed(MagicSugarSpeedEvent event) {
                event.setEffect(new PotionEffect(PotionEffectType.SPEED, 100, 1));
            }
        };
        server.getPluginManager().registerEvents(swapping, plugin);

        try {
            use(player);

            PotionEffect applied = player.getPotionEffect(PotionEffectType.SPEED);
            Assertions.assertNotNull(applied, "The swapped effect must have been applied");
            Assertions.assertEquals(100, applied.getDuration());
            Assertions.assertEquals(1, applied.getAmplifier());
        } finally {
            HandlerList.unregisterAll(swapping);
        }
    }

    @Test
    @DisplayName("Using magic sugar without listeners still applies speed, preserving the old behavior")
    void testUseWithoutListenersStillAppliesSpeed() {
        Player player = server.addPlayer();

        use(player);

        Assertions.assertTrue(player.getInventory().getItemInMainHand().getAmount() < 3, "The sugar must have been consumed");
        Assertions.assertNotNull(player.getPotionEffect(PotionEffectType.SPEED), "The speed effect must have been applied");
    }
}
