package io.github.thebusybiscuit.slimefun4.implementation.listeners;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
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

import io.github.thebusybiscuit.slimefun4.api.events.CoolerSaturationEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerBackpack;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.backpacks.Cooler;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Regression coverage for the cooler API expansion: {@link CoolerSaturationEvent}, exercised
 * through the real {@link CoolerListener} feed path: a {@link FoodLevelChangeEvent} triggers a
 * juice lookup in the player's cooler backpack and the consume runs via the sync scheduler.
 * <p>
 * The saturation landing on the player, the potion effects of the juice and the consumption
 * itself are asserted end-to-end; a cancelled event skips only the saturation restore.
 *
 * @author Zurker
 */
class TestCoolerSaturationEvent {

    private static final int COOLER_SIZE = 9;

    private static ServerMock server;
    private static Slimefun plugin;

    private static BackpackListener backpackListener;
    private static Cooler cooler;
    private static SlimefunItemStack coolerStack;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);

        // ProtectionManager is created when the integrations start
        Slimefun.getIntegrations().start();
        server.getScheduler().performOneTick();

        backpackListener = new BackpackListener();
        backpackListener.register(plugin);

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "cooler_saturation_test");
        coolerStack = new SlimefunItemStack("_TEST_COOLER", Material.CHEST, "&bTest Cooler", "", "&7Size: &e" + COOLER_SIZE, "&7ID: <ID>", "", "&7&eRight Click&7 to open");
        Slimefun.getItemCfg().setValue("_TEST_COOLER.enabled", true);
        cooler = new Cooler(COOLER_SIZE, itemGroup, coolerStack, RecipeType.NULL, new ItemStack[9]);
        cooler.register(plugin);

        new CoolerListener(plugin, cooler);
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
     * Creates a profile-owned cooler backpack for the player, puts it (and optionally a juice)
     * into their inventory and returns the backpack.
     */
    private PlayerBackpack giveCooler(Player player, boolean withJuice) throws InterruptedException {
        PlayerProfile profile = TestUtilities.awaitProfile(player);
        PlayerBackpack backpack = profile.createBackpack(COOLER_SIZE);

        ItemStack stack = coolerStack.item();
        backpackListener.setBackpackId(player, stack, 2, backpack.getId());
        player.getInventory().setItem(0, stack);

        if (withJuice) {
            ItemStack juice = new ItemStack(Material.POTION);
            PotionMeta meta = (PotionMeta) juice.getItemMeta();
            meta.setDisplayName("§bTest Juice");
            meta.addCustomEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 0), true);
            juice.setItemMeta(meta);
            backpack.getInventory().setItem(0, juice);
        }

        return backpack;
    }

    /**
     * Drops the player's food level so the real listener consumes a juice; the backpack lookup
     * hands off to the sync scheduler, so flush it after giving the async chain a moment.
     */
    private void feed(Player player) throws InterruptedException {
        server.getPluginManager().callEvent(new FoodLevelChangeEvent(player, 4));
        Thread.sleep(300);
        server.getScheduler().performTicks(5);
    }

    @Test
    @DisplayName("CoolerSaturationEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Player player = server.addPlayer();
        ItemStack coolerItem = coolerStack.item();
        ItemStack juice = new ItemStack(Material.POTION);

        CoolerSaturationEvent event = new CoolerSaturationEvent(player, cooler, coolerItem, juice, 6F);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(cooler, event.getCooler());
        Assertions.assertEquals(coolerItem, event.getCoolerItem());
        Assertions.assertEquals(juice, event.getConsumedItem());
        Assertions.assertEquals(6F, event.getSaturation());
        Assertions.assertFalse(event.isCancelled());

        event.setSaturation(14F);
        Assertions.assertEquals(14F, event.getSaturation());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new CoolerSaturationEvent(player, null, coolerItem, juice, 6F));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new CoolerSaturationEvent(player, cooler, null, juice, 6F));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new CoolerSaturationEvent(player, cooler, coolerItem, null, 6F));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new CoolerSaturationEvent(player, cooler, coolerItem, juice, -1F));
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setSaturation(-1F));
    }

    @Test
    @DisplayName("Feeding fires the event and restores the default saturation")
    void testFeedFiresEventAndRestoresSaturation() throws InterruptedException {
        Player player = server.addPlayer();
        PlayerBackpack backpack = giveCooler(player, true);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onSaturation(CoolerSaturationEvent event) {
                seen[0] = true;
                Assertions.assertEquals(cooler, event.getCooler());
                Assertions.assertEquals(Material.POTION, event.getConsumedItem().getType());
                Assertions.assertEquals(6F, event.getSaturation());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            feed(player);

            Assertions.assertTrue(seen[0], "CoolerSaturationEvent was not fired");
            Assertions.assertEquals(6F, player.getSaturation(), "The default saturation must have been restored");
            Assertions.assertTrue(player.hasPotionEffect(PotionEffectType.REGENERATION), "The juice effect must have been applied");
            Assertions.assertNull(backpack.getInventory().getItem(0), "The juice must have been consumed");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Adjusting the saturation via setSaturation scales the restore")
    void testSetSaturationAdjustsRestore() throws InterruptedException {
        Player player = server.addPlayer();
        giveCooler(player, true);

        Listener adjusting = new Listener() {
            @EventHandler
            public void onSaturation(CoolerSaturationEvent event) {
                event.setSaturation(14F);
            }
        };
        server.getPluginManager().registerEvents(adjusting, plugin);

        try {
            feed(player);

            Assertions.assertEquals(14F, player.getSaturation(), "The adjusted saturation must have been restored");
        } finally {
            HandlerList.unregisterAll(adjusting);
        }
    }

    @Test
    @DisplayName("Cancelling CoolerSaturationEvent skips only the saturation restore")
    void testCancelSkipsOnlySaturation() throws InterruptedException {
        Player player = server.addPlayer();
        PlayerBackpack backpack = giveCooler(player, true);
        player.setSaturation(2F);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onSaturation(CoolerSaturationEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            feed(player);

            Assertions.assertEquals(2F, player.getSaturation(), "A cancelled restore must leave the saturation untouched");
            Assertions.assertTrue(player.hasPotionEffect(PotionEffectType.REGENERATION), "The juice effect must still have been applied");
            Assertions.assertNull(backpack.getInventory().getItem(0), "The juice must still have been consumed");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Feeding without listeners still restores 6 saturation, preserving the old behavior")
    void testFeedWithoutListenersRestoresDefault() throws InterruptedException {
        Player player = server.addPlayer();
        PlayerBackpack backpack = giveCooler(player, true);

        feed(player);

        Assertions.assertEquals(6F, player.getSaturation(), "The default saturation must have been restored");
        Assertions.assertNull(backpack.getInventory().getItem(0), "The juice must have been consumed");
    }

    @Test
    @DisplayName("An empty cooler fires no event and restores nothing")
    void testEmptyCoolerFiresNothing() throws InterruptedException {
        Player player = server.addPlayer();
        giveCooler(player, false);
        player.setSaturation(3F);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onSaturation(CoolerSaturationEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            feed(player);

            Assertions.assertFalse(seen[0], "No event must be fired for an empty cooler");
            Assertions.assertEquals(3F, player.getSaturation(), "An empty cooler must not restore saturation");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }
}
