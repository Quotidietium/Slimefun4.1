package io.github.thebusybiscuit.slimefun4.implementation.items.androids;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Dispenser;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

import io.github.thebusybiscuit.slimefun4.api.events.AndroidItemDepositEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

import me.mrCookieSlime.Slimefun.api.BlockStorage;

/**
 * Regression coverage for the android API expansion: {@link AndroidItemDepositEvent}, exercised
 * by driving the real {@link ProgrammableAndroid#depositItems(me.mrCookieSlime.Slimefun.api.inventory.BlockMenu, Block)}
 * against a {@link BlockStorage}-backed android block facing an {@code ANDROID_INTERFACE_ITEMS}
 * dispenser with an item in one of its output slots.
 * <p>
 * The transfer empties the android's output slots into the interface inventory, so tests assert
 * the outcome end-to-end: a cancelled event leaves both sides untouched.
 *
 * @author Zurker
 */
class TestAndroidItemDepositEvent {

    private static final int OUTPUT_SLOT = 20;

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static ProgrammableAndroid android;
    private static Player owner;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        // ProtectionManager is created when the integrations start
        Slimefun.getIntegrations().start();
        server.getScheduler().performOneTick();

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "android_deposit_test");
        SlimefunItemStack stack = new SlimefunItemStack("_TEST_DEPOSIT_ANDROID", Material.PLAYER_HEAD, "&7Test Deposit Android");
        Slimefun.getItemCfg().setValue("_TEST_DEPOSIT_ANDROID.enabled", true);
        android = new ProgrammableAndroid(itemGroup, 1, stack, RecipeType.NULL, new ItemStack[9]);
        android.register(plugin);

        owner = server.addPlayer();
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
     * Places an owned android block with the given item in one of its output slots
     * (may be {@code null} for empty output slots).
     */
    private Block placeAndroid(int x, int z, ItemStack output) {
        Block b = world.getBlockAt(x, 1, z);
        b.setType(Material.PLAYER_HEAD);
        BlockStorage.addBlockInfo(b, "id", "_TEST_DEPOSIT_ANDROID");
        BlockStorage.addBlockInfo(b, "owner", owner.getUniqueId().toString());

        if (output != null) {
            BlockStorage.getInventory(b).replaceExistingItem(OUTPUT_SLOT, output);
        }

        return b;
    }

    /**
     * Places an items interface dispenser the android is facing.
     */
    private Block placeInterface(int x, int z) {
        Block b = world.getBlockAt(x, 1, z);
        b.setType(Material.DISPENSER);
        BlockStorage.addBlockInfo(b, "id", "ANDROID_INTERFACE_ITEMS");
        return b;
    }

    private void deposit(Block androidBlock, Block interfaceBlock) {
        android.depositItems(BlockStorage.getInventory(androidBlock), interfaceBlock);
    }

    private boolean dispenserContains(Block interfaceBlock, Material material) {
        return ((Dispenser) interfaceBlock.getState()).getInventory().contains(material);
    }

    @Test
    @DisplayName("AndroidItemDepositEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Block b = world.getBlockAt(1, 1, 1);
        Block faced = world.getBlockAt(2, 1, 1);

        AndroidItemDepositEvent event = new AndroidItemDepositEvent(android, b, faced);

        Assertions.assertEquals(android, event.getAndroid());
        Assertions.assertEquals(b, event.getBlock());
        Assertions.assertEquals(faced, event.getInterfaceBlock());
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new AndroidItemDepositEvent(null, b, faced));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new AndroidItemDepositEvent(android, null, faced));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new AndroidItemDepositEvent(android, b, null));
    }

    @Test
    @DisplayName("Depositing into an items interface fires the event and transfers the item")
    void testDepositFiresEventAndTransfers() {
        Block b = placeAndroid(10, 10, new ItemStack(Material.DIAMOND));
        Block faced = placeInterface(11, 10);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onDeposit(AndroidItemDepositEvent event) {
                seen[0] = true;
                Assertions.assertEquals(android, event.getAndroid());
                Assertions.assertEquals(b, event.getBlock());
                Assertions.assertEquals(faced, event.getInterfaceBlock());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            deposit(b, faced);

            Assertions.assertTrue(seen[0], "AndroidItemDepositEvent was not fired");
            Assertions.assertNull(BlockStorage.getInventory(b).getItemInSlot(OUTPUT_SLOT), "The android's output slot must have been emptied");
            Assertions.assertTrue(dispenserContains(faced, Material.DIAMOND), "The item must have landed in the interface inventory");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling AndroidItemDepositEvent keeps the items in the android")
    void testCancelKeepsItemsInAndroid() {
        Block b = placeAndroid(20, 20, new ItemStack(Material.DIAMOND));
        Block faced = placeInterface(21, 20);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onDeposit(AndroidItemDepositEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            deposit(b, faced);

            ItemStack slot = BlockStorage.getInventory(b).getItemInSlot(OUTPUT_SLOT);
            Assertions.assertNotNull(slot, "A cancelled deposit must keep the item in the android");
            Assertions.assertEquals(Material.DIAMOND, slot.getType());
            Assertions.assertFalse(dispenserContains(faced, Material.DIAMOND), "A cancelled deposit must not touch the interface inventory");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Depositing without listeners still transfers, preserving the old behavior")
    void testDepositWithoutListenersStillTransfers() {
        Block b = placeAndroid(30, 30, new ItemStack(Material.DIAMOND));
        Block faced = placeInterface(31, 30);

        deposit(b, faced);

        Assertions.assertNull(BlockStorage.getInventory(b).getItemInSlot(OUTPUT_SLOT), "The android's output slot must have been emptied");
        Assertions.assertTrue(dispenserContains(faced, Material.DIAMOND), "The item must have landed in the interface inventory");
    }

    @Test
    @DisplayName("Facing a block that is not an items interface fires no event")
    void testNonInterfaceFiresNothing() {
        Block b = placeAndroid(40, 40, new ItemStack(Material.DIAMOND));
        Block faced = world.getBlockAt(41, 1, 40);
        faced.setType(Material.DISPENSER);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onDeposit(AndroidItemDepositEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            deposit(b, faced);

            Assertions.assertFalse(seen[0], "No event must be fired when the faced block is not an items interface");
            ItemStack slot = BlockStorage.getInventory(b).getItemInSlot(OUTPUT_SLOT);
            Assertions.assertNotNull(slot, "The item must have stayed in the android");
            Assertions.assertEquals(Material.DIAMOND, slot.getType());
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("An android without owner data fires no event")
    void testOwnerlessAndroidFiresNothing() {
        Block b = world.getBlockAt(50, 1, 50);
        b.setType(Material.PLAYER_HEAD);
        BlockStorage.addBlockInfo(b, "id", "_TEST_DEPOSIT_ANDROID");
        BlockStorage.getInventory(b).replaceExistingItem(OUTPUT_SLOT, new ItemStack(Material.DIAMOND));
        Block faced = placeInterface(51, 50);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onDeposit(AndroidItemDepositEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            deposit(b, faced);

            Assertions.assertFalse(seen[0], "No event must be fired when the owner check failed");
            ItemStack slot = BlockStorage.getInventory(b).getItemInSlot(OUTPUT_SLOT);
            Assertions.assertNotNull(slot, "The item must have stayed in the android");
            Assertions.assertEquals(Material.DIAMOND, slot.getType());
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }
}
