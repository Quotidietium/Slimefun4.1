package io.github.thebusybiscuit.slimefun4.implementation.items;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

import io.github.thebusybiscuit.slimefun4.api.events.SlimefunItemWearEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;
import io.github.thebusybiscuit.slimefun4.test.mocks.MockDamageable;

/**
 * Regression coverage for the durability API expansion: {@link SlimefunItemWearEvent},
 * exercised by driving the real {@link io.github.thebusybiscuit.slimefun4.core.attributes.DamageableItem#damageItem}
 * wear path on a registered {@link MockDamageable}.
 * <p>
 * The wear is fully observable through the item meta's damage value and the stack amount,
 * both functional under MockBukkit. A vetoed wear keeps the durability; a wear at the last
 * point breaks the item unless vetoed.
 *
 * @author Zurker
 */
class TestSlimefunItemWearEvent {

    private static ServerMock server;
    private static Slimefun plugin;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    @BeforeEach
    public void beforeEach() {
        server.getPluginManager().clearEvents();
    }

    private MockDamageable newDamageable(String id) {
        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "wear_test_" + id);
        SlimefunItemStack stack = new SlimefunItemStack("WEAR_PICKAXE_" + id, Material.DIAMOND_PICKAXE, "&fWear Test Pickaxe");
        Slimefun.getItemCfg().setValue("WEAR_PICKAXE_" + id + ".enabled", true);
        MockDamageable item = new MockDamageable(itemGroup, stack, RecipeType.NULL, new ItemStack[9], true);
        item.register(plugin);
        return item;
    }

    private int damageOf(ItemStack stack) {
        return ((Damageable) stack.getItemMeta()).getDamage();
    }

    private ItemStack stackWithDamage(MockDamageable item, int damage) {
        ItemStack stack = item.getItem().clone();
        ItemMeta meta = stack.getItemMeta();
        ((Damageable) meta).setDamage(damage);
        stack.setItemMeta(meta);
        return stack;
    }

    @Test
    @DisplayName("SlimefunItemWearEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Player player = server.addPlayer();
        MockDamageable item = newDamageable("FIELDS");
        ItemStack stack = stackWithDamage(item, 5);

        SlimefunItemWearEvent event = new SlimefunItemWearEvent(player, item, stack, false);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(item, event.getSlimefunItem());
        Assertions.assertEquals(stack, event.getItem());
        Assertions.assertFalse(event.willBreak());
        Assertions.assertFalse(event.isCancelled());

        // willBreak is decided by the constructor argument, not the stack state:
        Assertions.assertTrue(new SlimefunItemWearEvent(player, item, stack, true).willBreak());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        // A non-Slimefun stack still resolves the event with a null SlimefunItem
        ItemStack vanilla = new ItemStack(Material.IRON_PICKAXE);
        SlimefunItemWearEvent vanillaEvent = new SlimefunItemWearEvent(player, null, vanilla, false);
        Assertions.assertNull(vanillaEvent.getSlimefunItem());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new SlimefunItemWearEvent(player, item, null, false));
    }

    @Test
    @DisplayName("Damaging an item fires the event and applies one point of wear")
    void testWearFiresEventAndApplies() {
        Player player = server.addPlayer();
        MockDamageable item = newDamageable("WEAR");
        ItemStack stack = stackWithDamage(item, 0);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onWear(SlimefunItemWearEvent event) {
                seen[0] = true;
                Assertions.assertEquals(player, event.getPlayer());
                Assertions.assertEquals(item, event.getSlimefunItem());
                Assertions.assertFalse(event.willBreak());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            item.damageItem(player, stack);

            Assertions.assertTrue(seen[0], "SlimefunItemWearEvent was not fired");
            Assertions.assertEquals(1, damageOf(stack), "One point of wear must have been applied");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling SlimefunItemWearEvent keeps the durability untouched")
    void testCancelKeepsDurability() {
        Player player = server.addPlayer();
        MockDamageable item = newDamageable("CANCEL");
        ItemStack stack = stackWithDamage(item, 5);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onWear(SlimefunItemWearEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            item.damageItem(player, stack);

            Assertions.assertEquals(5, damageOf(stack), "A vetoed wear must keep the durability");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Damaging without listeners still applies the wear, preserving the old behavior")
    void testWearWithoutListenersApplies() {
        Player player = server.addPlayer();
        MockDamageable item = newDamageable("NOLISTENER");
        ItemStack stack = stackWithDamage(item, 0);

        item.damageItem(player, stack);

        Assertions.assertEquals(1, damageOf(stack), "The wear must have been applied");
    }

    @Test
    @DisplayName("Wear at the last durability point breaks the item")
    void testWearBreaksItemAtMaxDurability() {
        Player player = server.addPlayer();
        MockDamageable item = newDamageable("BREAK");
        ItemStack stack = stackWithDamage(item, Material.DIAMOND_PICKAXE.getMaxDurability());

        boolean[] seen = { false };
        boolean[] willBreak = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onWear(SlimefunItemWearEvent event) {
                seen[0] = true;
                willBreak[0] = event.willBreak();
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            item.damageItem(player, stack);

            Assertions.assertTrue(seen[0], "SlimefunItemWearEvent was not fired");
            Assertions.assertTrue(willBreak[0], "The wear at max durability must be reported as breaking");
            Assertions.assertEquals(0, stack.getAmount(), "The item must have broken");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling the break-preventing wear keeps the item alive")
    void testCancelPreventsBreak() {
        Player player = server.addPlayer();
        MockDamageable item = newDamageable("CANCELBREAK");
        ItemStack stack = stackWithDamage(item, Material.DIAMOND_PICKAXE.getMaxDurability());

        Listener cancelling = new Listener() {
            @EventHandler
            public void onWear(SlimefunItemWearEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            item.damageItem(player, stack);

            Assertions.assertEquals(1, stack.getAmount(), "A vetoed break must keep the item");
            Assertions.assertEquals(Material.DIAMOND_PICKAXE.getMaxDurability(), damageOf(stack), "The durability must stay at max");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("A non-damageable item fires no event")
    void testNonDamageableFiresNothing() {
        Player player = server.addPlayer();
        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "wear_test_safe");
        SlimefunItemStack stack = new SlimefunItemStack("SAFE_PICKAXE", Material.DIAMOND_PICKAXE, "&fSafe Pickaxe");
        Slimefun.getItemCfg().setValue("SAFE_PICKAXE.enabled", true);
        MockDamageable safe = new MockDamageable(itemGroup, stack, RecipeType.NULL, new ItemStack[9], false);
        safe.register(plugin);

        ItemStack held = safe.getItem().clone();
        int before = damageOf(held);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onWear(SlimefunItemWearEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            safe.damageItem(player, held);

            Assertions.assertFalse(seen[0], "No event must be fired for a non-damageable item");
            Assertions.assertEquals(before, damageOf(held), "A non-damageable item must not wear");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }
}
