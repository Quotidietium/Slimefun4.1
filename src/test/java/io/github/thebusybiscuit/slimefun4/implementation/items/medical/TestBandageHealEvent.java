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

import io.github.thebusybiscuit.slimefun4.utils.compatibility.VersionedPotionEffectType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

import io.github.thebusybiscuit.slimefun4.api.events.BandageHealEvent;
import io.github.thebusybiscuit.slimefun4.api.events.PlayerRightClickEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Regression coverage for the bandage API expansion: {@link BandageHealEvent}, exercised by
 * driving the real {@link Bandage} {@link io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler}
 * with a constructed {@link PlayerRightClickEvent}.
 * <p>
 * The heal ends in {@code playEffect(STEP_SOUND, Material)}, which MockBukkit rejects with an
 * {@link IllegalArgumentException} right after the bandage was consumed - reaching that tail
 * proves the heal ran, the potion/fire tails behind it stay unobservable here.
 *
 * @author Zurker
 */
class TestBandageHealEvent {

    private static ServerMock server;
    private static Slimefun plugin;

    private static Bandage bandage;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "bandage_test");
        SlimefunItemStack stack = new SlimefunItemStack("_TEST_BANDAGE", Material.PAPER, "&fTest Bandage");
        Slimefun.getItemCfg().setValue("_TEST_BANDAGE.enabled", true);
        bandage = new Bandage(itemGroup, stack, RecipeType.NULL, new ItemStack[9], new ItemStack(Material.PAPER), 2);
        bandage.register(plugin);
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
     * Injures the player, puts three bandages in their main hand and uses one via the real
     * handler.
     *
     * @return true if the heal tail (the playEffect MockBukkit rejects) was reached
     */
    private boolean useOnInjured(Player player) {
        player.setHealth(10.0);

        ItemStack item = bandage.getItem().clone();
        item.setAmount(3);
        player.getInventory().setItemInMainHand(item);

        // MockBukkit clones the ItemStack in setItemInMainHand, so hand the event the reference
        // actually stored in the inventory for consumeItem(e.getItem()) to be visible.
        ItemStack handItem = player.getInventory().getItemInMainHand();
        PlayerInteractEvent interactEvent = new PlayerInteractEvent(player, Action.RIGHT_CLICK_AIR, handItem, null, null);
        PlayerRightClickEvent event = new PlayerRightClickEvent(interactEvent);

        try {
            bandage.getItemHandler().onRightClick(event);
            return false;
        } catch (IllegalArgumentException ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("Wrong kind of data")) {
                // MockBukkit rejects playEffect(STEP_SOUND, Material) - see class javadoc
                return true;
            }

            throw ex;
        }
    }

    @Test
    @DisplayName("BandageHealEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Player player = server.addPlayer();
        PotionEffect effect = new PotionEffect(VersionedPotionEffectType.INSTANT_HEALTH, 1, 2);

        BandageHealEvent event = new BandageHealEvent(player, bandage, effect);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(bandage, event.getBandage());
        Assertions.assertEquals(effect, event.getEffect());
        Assertions.assertFalse(event.isCancelled());

        // setEffect: override the healing
        PotionEffect custom = new PotionEffect(VersionedPotionEffectType.INSTANT_HEALTH, 1, 5);
        event.setEffect(custom);
        Assertions.assertEquals(custom, event.getEffect());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new BandageHealEvent(player, null, effect));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new BandageHealEvent(player, bandage, null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setEffect(null));
    }

    @Test
    @DisplayName("Using a bandage while injured fires the event, consumes it and heals")
    void testUseFiresAndHeals() {
        Player player = server.addPlayer();

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onHeal(BandageHealEvent event) {
                seen[0] = true;
                Assertions.assertEquals(bandage, event.getBandage());
                Assertions.assertNotNull(event.getEffect(), "The effect must be initialized");
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            boolean healReached = useOnInjured(player);

            Assertions.assertTrue(seen[0], "BandageHealEvent was not fired");
            Assertions.assertTrue(player.getInventory().getItemInMainHand().getAmount() < 3, "The bandage must have been consumed");
            Assertions.assertTrue(healReached, "The heal tail must have been reached");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling BandageHealEvent keeps the bandage and skips the heal")
    void testEventCancellationSkipsHeal() {
        Player player = server.addPlayer();

        Listener cancelling = new Listener() {
            @EventHandler
            public void onHeal(BandageHealEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            boolean healReached = useOnInjured(player);

            Assertions.assertEquals(3, player.getInventory().getItemInMainHand().getAmount(), "A cancelled heal must keep the bandage");
            Assertions.assertEquals(10.0, player.getHealth(), "A cancelled heal must keep the health");
            Assertions.assertFalse(healReached, "A cancelled heal must not reach the heal tail");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("A healthy player neither fires the event nor consumes a bandage")
    void testHealthyPlayerDoesNothing() {
        Player player = server.addPlayer();

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onHeal(BandageHealEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            ItemStack item = bandage.getItem().clone();
            item.setAmount(3);
            player.getInventory().setItemInMainHand(item);

            PlayerInteractEvent interactEvent = new PlayerInteractEvent(player, Action.RIGHT_CLICK_AIR, player.getInventory().getItemInMainHand(), null, null);
            bandage.getItemHandler().onRightClick(new PlayerRightClickEvent(interactEvent));

            Assertions.assertFalse(seen[0], "The event must not fire for a healthy player");
            Assertions.assertEquals(3, player.getInventory().getItemInMainHand().getAmount(), "No bandage must be consumed for a healthy player");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Using a bandage without listeners still heals, preserving the old behavior")
    void testUseWithoutListenersStillHeals() {
        Player player = server.addPlayer();

        boolean healReached = useOnInjured(player);

        Assertions.assertTrue(player.getInventory().getItemInMainHand().getAmount() < 3, "The bandage must have been consumed");
        Assertions.assertTrue(healReached, "The heal tail must have been reached");
    }
}
