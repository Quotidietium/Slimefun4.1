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

import io.github.thebusybiscuit.slimefun4.api.events.AndroidRefuelEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

import me.mrCookieSlime.Slimefun.api.BlockStorage;

/**
 * Regression coverage for the android API expansion: {@link AndroidRefuelEvent}, exercised
 * by driving the real {@link ProgrammableAndroid#refuel(me.mrCookieSlime.Slimefun.api.inventory.BlockMenu, Block)}
 * against a {@link BlockStorage}-backed android block facing an {@code ANDROID_INTERFACE_FUEL}
 * dispenser stocked with fuel.
 * <p>
 * The transfer moves fuel from the interface into the android's fuel slot (merging into a
 * partial stack when the types match), so tests assert both ends: a cancelled event leaves
 * both sides untouched.
 *
 * @author Zurker
 */
class TestAndroidRefuelEvent {

    private static final int FUEL_SLOT = 43;

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

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "android_refuel_test");
        SlimefunItemStack stack = new SlimefunItemStack("_TEST_REFUEL_ANDROID", Material.PLAYER_HEAD, "&7Test Refuel Android");
        Slimefun.getItemCfg().setValue("_TEST_REFUEL_ANDROID.enabled", true);
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
     * Places an owned android block with the given item in its fuel slot
     * (may be {@code null} for an empty fuel slot).
     */
    private Block placeAndroid(int x, int z, ItemStack fuel) {
        Block b = world.getBlockAt(x, 1, z);
        b.setType(Material.PLAYER_HEAD);
        BlockStorage.addBlockInfo(b, "id", "_TEST_REFUEL_ANDROID");
        BlockStorage.addBlockInfo(b, "owner", owner.getUniqueId().toString());

        if (fuel != null) {
            BlockStorage.getInventory(b).replaceExistingItem(FUEL_SLOT, fuel);
        }

        return b;
    }

    /**
     * Places a fuel interface dispenser with the given item in its first slot
     * (may be {@code null} for an empty dispenser).
     */
    private Block placeInterface(int x, int z, ItemStack fuel) {
        Block b = world.getBlockAt(x, 1, z);
        b.setType(Material.DISPENSER);
        BlockStorage.addBlockInfo(b, "id", "ANDROID_INTERFACE_FUEL");

        if (fuel != null) {
            ((Dispenser) b.getState()).getInventory().setItem(0, fuel);
        }

        return b;
    }

    private void refuel(Block androidBlock, Block interfaceBlock) {
        android.refuel(BlockStorage.getInventory(androidBlock), interfaceBlock);
    }

    private ItemStack fuelSlot(Block androidBlock) {
        return BlockStorage.getInventory(androidBlock).getItemInSlot(FUEL_SLOT);
    }

    private ItemStack interfaceSlot(Block interfaceBlock) {
        return ((Dispenser) interfaceBlock.getState()).getInventory().getItem(0);
    }

    @Test
    @DisplayName("AndroidRefuelEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Block b = world.getBlockAt(1, 1, 1);
        Block faced = world.getBlockAt(2, 1, 1);

        AndroidRefuelEvent event = new AndroidRefuelEvent(android, b, faced);

        Assertions.assertEquals(android, event.getAndroid());
        Assertions.assertEquals(b, event.getBlock());
        Assertions.assertEquals(faced, event.getInterfaceBlock());
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new AndroidRefuelEvent(null, b, faced));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new AndroidRefuelEvent(android, null, faced));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new AndroidRefuelEvent(android, b, null));
    }

    @Test
    @DisplayName("Refuelling from a fuel interface fires the event and transfers the fuel")
    void testRefuelFiresEventAndTransfers() {
        Block b = placeAndroid(10, 10, null);
        Block faced = placeInterface(11, 10, new ItemStack(Material.COAL));

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onRefuel(AndroidRefuelEvent event) {
                seen[0] = true;
                Assertions.assertEquals(android, event.getAndroid());
                Assertions.assertEquals(b, event.getBlock());
                Assertions.assertEquals(faced, event.getInterfaceBlock());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            refuel(b, faced);

            Assertions.assertTrue(seen[0], "AndroidRefuelEvent was not fired");
            ItemStack slot = fuelSlot(b);
            Assertions.assertNotNull(slot, "The fuel must have landed in the android's fuel slot");
            Assertions.assertEquals(Material.COAL, slot.getType());
            Assertions.assertNull(interfaceSlot(faced), "The interface slot must have been emptied");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling AndroidRefuelEvent keeps the fuel in the interface")
    void testCancelKeepsFuelInInterface() {
        Block b = placeAndroid(20, 20, null);
        Block faced = placeInterface(21, 20, new ItemStack(Material.COAL));

        Listener cancelling = new Listener() {
            @EventHandler
            public void onRefuel(AndroidRefuelEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            refuel(b, faced);

            Assertions.assertNull(fuelSlot(b), "A cancelled refuel must not fill the android's fuel slot");
            ItemStack slot = interfaceSlot(faced);
            Assertions.assertNotNull(slot, "A cancelled refuel must keep the fuel in the interface");
            Assertions.assertEquals(Material.COAL, slot.getType());
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Refuelling without listeners still transfers, preserving the old behavior")
    void testRefuelWithoutListenersStillTransfers() {
        Block b = placeAndroid(30, 30, null);
        Block faced = placeInterface(31, 30, new ItemStack(Material.COAL));

        refuel(b, faced);

        ItemStack slot = fuelSlot(b);
        Assertions.assertNotNull(slot, "The fuel must have landed in the android's fuel slot");
        Assertions.assertEquals(Material.COAL, slot.getType());
        Assertions.assertNull(interfaceSlot(faced), "The interface slot must have been emptied");
    }

    @Test
    @DisplayName("Refuelling onto a partial fuel stack merges up to a full stack")
    void testRefuelMergesIntoPartialStack() {
        Block b = placeAndroid(40, 40, new ItemStack(Material.COAL, 32));
        Block faced = placeInterface(41, 40, new ItemStack(Material.COAL, 64));

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onRefuel(AndroidRefuelEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            refuel(b, faced);

            Assertions.assertTrue(seen[0], "AndroidRefuelEvent was not fired");
            ItemStack slot = fuelSlot(b);
            Assertions.assertNotNull(slot);
            Assertions.assertEquals(64, slot.getAmount(), "The fuel slot must have been topped up to a full stack");
            ItemStack rest = interfaceSlot(faced);
            Assertions.assertNotNull(rest, "The merged surplus must have stayed in the interface");
            Assertions.assertEquals(32, rest.getAmount(), "Only the missing 32 coal must have been pulled");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Facing a block that is not a fuel interface fires no event")
    void testNonInterfaceFiresNothing() {
        Block b = placeAndroid(50, 50, null);
        Block faced = world.getBlockAt(51, 1, 50);
        faced.setType(Material.DISPENSER);
        ((Dispenser) faced.getState()).getInventory().setItem(0, new ItemStack(Material.COAL));

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onRefuel(AndroidRefuelEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            refuel(b, faced);

            Assertions.assertFalse(seen[0], "No event must be fired when the faced block is not a fuel interface");
            Assertions.assertNull(fuelSlot(b), "The android's fuel slot must have stayed empty");
            ItemStack slot = interfaceSlot(faced);
            Assertions.assertNotNull(slot, "The fuel must have stayed in the dispenser");
            Assertions.assertEquals(Material.COAL, slot.getType());
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }
}
