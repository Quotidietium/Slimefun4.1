package io.github.thebusybiscuit.slimefun4.implementation.items;

import javax.annotation.Nonnull;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

import io.github.thebusybiscuit.slimefun4.api.events.LimitedUseItemConsumeEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Regression coverage for the limited-use item API expansion:
 * {@link LimitedUseItemConsumeEvent}, exercised by driving the real
 * {@link LimitedUseItem#damageItem(Player, ItemStack)} charge consumption path.
 * <p>
 * The charge count lives in the item meta's persistent data container, which is
 * functional under MockBukkit, so consumption is asserted end-to-end through the
 * stored "uses_left" value and the broken stack.
 *
 * @author Zurker
 */
class TestLimitedUseItemConsumeEvent {

    private static final int MAX_USES = 3;

    private static ServerMock server;
    private static Slimefun plugin;

    private static TestLimitedUseItem item;

    /**
     * A minimal concrete {@link LimitedUseItem} with three charges.
     */
    private static class TestLimitedUseItem extends LimitedUseItem {

        TestLimitedUseItem(ItemGroup group, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
            super(group, item, recipeType, recipe);
        }

        @Nonnull
        @Override
        public ItemUseHandler getItemHandler() {
            return e -> {
            };
        }
    }

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "limited_use_test");
        SlimefunItemStack stack = new SlimefunItemStack("_TEST_LIMITED_USE_ITEM", Material.BLAZE_ROD, "&eTest Limited Use Item");
        Slimefun.getItemCfg().setValue("_TEST_LIMITED_USE_ITEM.enabled", true);
        item = new TestLimitedUseItem(itemGroup, stack, RecipeType.NULL, new ItemStack[9]);
        item.setMaxUseCount(MAX_USES);
        item.register(plugin);
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    @BeforeEach
    public void beforeEach() {
        server.getPluginManager().clearEvents();
    }

    private NamespacedKey usesLeftKey() {
        return new NamespacedKey(Slimefun.instance(), "uses_left");
    }

    private int usesLeftOf(ItemStack stack) {
        return stack.getItemMeta().getPersistentDataContainer().getOrDefault(usesLeftKey(), PersistentDataType.INTEGER, -1);
    }

    private ItemStack stackWithUses(int usesLeft) {
        ItemStack stack = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = stack.getItemMeta();
        meta.getPersistentDataContainer().set(usesLeftKey(), PersistentDataType.INTEGER, usesLeft);
        stack.setItemMeta(meta);
        return stack;
    }

    @Test
    @DisplayName("LimitedUseItemConsumeEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Player player = server.addPlayer();
        ItemStack stack = stackWithUses(2);

        LimitedUseItemConsumeEvent event = new LimitedUseItemConsumeEvent(player, item, stack, 2);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(item, event.getItem());
        Assertions.assertEquals(stack, event.getItemStack());
        Assertions.assertEquals(2, event.getUsesLeftBefore());
        Assertions.assertEquals(1, event.getUsesLeftAfter(), "The uses left after must default to one less than before");
        Assertions.assertFalse(event.willBreak(), "Two charges left must not be breaking");
        Assertions.assertFalse(event.isCancelled());

        Assertions.assertTrue(new LimitedUseItemConsumeEvent(player, item, stack, 1).willBreak(), "One charge left must be breaking");

        // Zeroing the remaining charges breaks the item
        event.setUsesLeftAfter(0);
        Assertions.assertEquals(0, event.getUsesLeftAfter());
        Assertions.assertTrue(event.willBreak(), "Zero charges left after must be breaking");

        // A use must cost at least one charge: negative, unchanged and increased counts are rejected
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setUsesLeftAfter(-1));
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setUsesLeftAfter(2));
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setUsesLeftAfter(3));

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new LimitedUseItemConsumeEvent(player, null, stack, 2));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new LimitedUseItemConsumeEvent(player, item, null, 2));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new LimitedUseItemConsumeEvent(player, item, stack, 0));
    }

    @Test
    @DisplayName("Using the item fires the event and decrements the charge")
    void testUseFiresEventAndDecrements() {
        Player player = server.addPlayer();
        ItemStack stack = new ItemStack(Material.BLAZE_ROD);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onConsume(LimitedUseItemConsumeEvent event) {
                seen[0] = true;
                Assertions.assertEquals(player, event.getPlayer());
                Assertions.assertEquals(item, event.getItem());
                Assertions.assertEquals(stack, event.getItemStack());
                Assertions.assertEquals(MAX_USES, event.getUsesLeftBefore(), "A fresh item must report the maximum charge count");
                Assertions.assertFalse(event.willBreak());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            item.damageItem(player, stack);

            Assertions.assertTrue(seen[0], "LimitedUseItemConsumeEvent was not fired");
            Assertions.assertEquals(MAX_USES - 1, usesLeftOf(stack), "The charge must have been decremented");
            Assertions.assertEquals(1, stack.getAmount(), "The item must not have broken");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Consuming the last charge fires with willBreak and breaks the item")
    void testLastChargeBreaksItem() {
        Player player = server.addPlayer();
        ItemStack stack = stackWithUses(1);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onConsume(LimitedUseItemConsumeEvent event) {
                seen[0] = true;
                Assertions.assertEquals(1, event.getUsesLeftBefore());
                Assertions.assertTrue(event.willBreak(), "The last charge must be reported as breaking");
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            item.damageItem(player, stack);

            Assertions.assertTrue(seen[0], "LimitedUseItemConsumeEvent was not fired");
            Assertions.assertEquals(0, stack.getAmount(), "The item must have broken");
            Assertions.assertEquals(Material.AIR, stack.getType(), "The broken item must have turned to air");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling LimitedUseItemConsumeEvent keeps the charge")
    void testCancelKeepsCharge() {
        Player player = server.addPlayer();
        ItemStack stack = new ItemStack(Material.BLAZE_ROD);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onConsume(LimitedUseItemConsumeEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            item.damageItem(player, stack);

            Assertions.assertEquals(-1, usesLeftOf(stack), "A vetoed use must not write a charge count");
            Assertions.assertEquals(1, stack.getAmount(), "A vetoed use must not break the item");
            Assertions.assertEquals(Material.BLAZE_ROD, stack.getType());
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Cancelling on the last charge prevents the break")
    void testCancelPreventsBreak() {
        Player player = server.addPlayer();
        ItemStack stack = stackWithUses(1);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onConsume(LimitedUseItemConsumeEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            item.damageItem(player, stack);

            Assertions.assertEquals(1, stack.getAmount(), "A vetoed break must keep the item");
            Assertions.assertEquals(Material.BLAZE_ROD, stack.getType(), "A vetoed break must keep the material");
            Assertions.assertEquals(1, usesLeftOf(stack), "A vetoed break must keep the last charge");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Overriding the uses left after skips charges without breaking the item")
    void testSetUsesLeftAfterSkipsCharges() {
        Player player = server.addPlayer();
        ItemStack stack = new ItemStack(Material.BLAZE_ROD);

        Listener skipping = new Listener() {
            @EventHandler
            public void onConsume(LimitedUseItemConsumeEvent event) {
                Assertions.assertEquals(MAX_USES - 1, event.getUsesLeftAfter(), "The uses left after must default to one less");
                event.setUsesLeftAfter(1);
            }
        };
        server.getPluginManager().registerEvents(skipping, plugin);

        try {
            item.damageItem(player, stack);

            Assertions.assertEquals(1, usesLeftOf(stack), "The charge count must have dropped straight to one");
            Assertions.assertEquals(1, stack.getAmount(), "The item must not have broken yet");
            Assertions.assertEquals(Material.BLAZE_ROD, stack.getType());
        } finally {
            HandlerList.unregisterAll(skipping);
        }
    }

    @Test
    @DisplayName("Zeroing the uses left after breaks the item early")
    void testSetUsesLeftAfterZeroBreaksEarly() {
        Player player = server.addPlayer();
        ItemStack stack = new ItemStack(Material.BLAZE_ROD);

        Listener breaking = new Listener() {
            @EventHandler
            public void onConsume(LimitedUseItemConsumeEvent event) {
                Assertions.assertFalse(event.willBreak(), "A fresh item must not be breaking by default");
                event.setUsesLeftAfter(0);
            }
        };
        server.getPluginManager().registerEvents(breaking, plugin);

        try {
            item.damageItem(player, stack);

            Assertions.assertEquals(0, stack.getAmount(), "The item must have broken early");
            Assertions.assertEquals(Material.AIR, stack.getType(), "The broken item must have turned to air");
        } finally {
            HandlerList.unregisterAll(breaking);
        }
    }

    @Test
    @DisplayName("Using the item without listeners still decrements, preserving the old behavior")
    void testUseWithoutListenersDecrements() {
        Player player = server.addPlayer();
        ItemStack stack = new ItemStack(Material.BLAZE_ROD);

        item.damageItem(player, stack);

        Assertions.assertEquals(MAX_USES - 1, usesLeftOf(stack), "The charge must have been decremented");
    }

    @Test
    @DisplayName("Using a stacked item splits one off and consumes the copy's charge")
    void testStackedUseSplitsAndConsumesCopy() {
        Player player = server.addPlayer();
        ItemStack stack = new ItemStack(Material.BLAZE_ROD, 2);

        int[] fired = { 0 };
        Listener watcher = new Listener() {
            @EventHandler
            public void onConsume(LimitedUseItemConsumeEvent event) {
                fired[0]++;
                Assertions.assertEquals(MAX_USES, event.getUsesLeftBefore());
                Assertions.assertFalse(event.willBreak());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            item.damageItem(player, stack);

            Assertions.assertEquals(1, fired[0], "Exactly one charge consumption must have been fired");
            Assertions.assertEquals(1, stack.getAmount(), "The original stack must have lost one unit");
            Assertions.assertEquals(-1, usesLeftOf(stack), "The original stack's charges must be untouched");

            ItemStack copy = null;
            for (ItemStack inv : player.getInventory()) {
                if (inv != null && inv.getType() == Material.BLAZE_ROD) {
                    copy = inv;
                    break;
                }
            }

            Assertions.assertNotNull(copy, "The separated item must have been given to the player");
            Assertions.assertEquals(MAX_USES - 1, usesLeftOf(copy), "The separated copy must carry the consumed charge");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }
}
