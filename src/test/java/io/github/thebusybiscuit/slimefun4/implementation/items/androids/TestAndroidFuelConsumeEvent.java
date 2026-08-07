package io.github.thebusybiscuit.slimefun4.implementation.items.androids;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
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

import io.github.thebusybiscuit.slimefun4.api.events.AndroidFuelConsumeEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.MachineFuel;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;

/**
 * Regression coverage for the android API expansion: {@link AndroidFuelConsumeEvent}, exercised
 * by driving the real {@link ProgrammableAndroid#tick(Block, me.mrCookieSlime.CSCoreLibPlugin.Configuration.Config)}
 * against a {@link BlockStorage}/{@link BlockMenu}-backed android block that ran out of fuel
 * with a fuel item in its fuel slot.
 * <p>
 * The fuel refill lands in the block's data and the fuel slot is emptied, so tests assert the
 * outcome end-to-end: a cancelled event leaves both untouched.
 *
 * @author Zurker
 */
class TestAndroidFuelConsumeEvent {

    private static final int FUEL_SLOT = 43;

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static ProgrammableAndroid android;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        // ProtectionManager is created when the integrations start
        Slimefun.getIntegrations().start();
        server.getScheduler().performOneTick();

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "android_fuel_test");
        SlimefunItemStack stack = new SlimefunItemStack("_TEST_ANDROID", Material.PLAYER_HEAD, "&7Test Android");
        Slimefun.getItemCfg().setValue("_TEST_ANDROID.enabled", true);
        android = new ProgrammableAndroid(itemGroup, 1, stack, RecipeType.NULL, new ItemStack[9]);
        android.register(plugin);
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
     * Places an android block that is unpaused and out of fuel, with the given item in its
     * fuel slot (may be {@code null} for an empty slot).
     */
    private Block placeAndroid(int x, int z, ItemStack fuelItem) {
        Block b = world.getBlockAt(x, 1, z);
        b.setType(Material.PLAYER_HEAD);
        BlockStorage.addBlockInfo(b, "id", "_TEST_ANDROID");
        BlockStorage.addBlockInfo(b, "paused", "false");
        BlockStorage.addBlockInfo(b, "fuel", "0");

        if (fuelItem != null) {
            BlockStorage.getInventory(b).replaceExistingItem(FUEL_SLOT, fuelItem);
        }

        return b;
    }

    private void tick(Block b) {
        android.tick(b, BlockStorage.getLocationInfo(b.getLocation()));
    }

    /**
     * {@link BlockMenu#consumeItem(int)} leaves a zero-amount stack behind instead of clearing
     * the slot, so emptiness means {@code null} or amount zero.
     */
    private void assertFuelSlotEmpty(Block b) {
        ItemStack slot = BlockStorage.getInventory(b).getItemInSlot(FUEL_SLOT);
        Assertions.assertTrue(slot == null || slot.getAmount() == 0, "The fuel item must have been consumed, got: " + slot);
    }

    @Test
    @DisplayName("AndroidFuelConsumeEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Block b = world.getBlockAt(1, 1, 1);
        ItemStack coal = new ItemStack(Material.COAL);
        MachineFuel machineFuel = new MachineFuel(8, coal);

        AndroidFuelConsumeEvent event = new AndroidFuelConsumeEvent(android, b, coal, machineFuel, 8);

        Assertions.assertEquals(android, event.getAndroid());
        Assertions.assertEquals(b, event.getBlock());
        Assertions.assertEquals(coal, event.getFuelItem());
        Assertions.assertEquals(machineFuel, event.getMachineFuel());
        Assertions.assertEquals(8, event.getFuelTicks());
        Assertions.assertFalse(event.isCancelled());

        event.setFuelTicks(40);
        Assertions.assertEquals(40, event.getFuelTicks());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new AndroidFuelConsumeEvent(null, b, coal, machineFuel, 8));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new AndroidFuelConsumeEvent(android, null, coal, machineFuel, 8));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new AndroidFuelConsumeEvent(android, b, null, machineFuel, 8));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new AndroidFuelConsumeEvent(android, b, coal, null, 8));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new AndroidFuelConsumeEvent(android, b, coal, machineFuel, 0));
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setFuelTicks(0));
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setFuelTicks(-1));
    }

    @Test
    @DisplayName("An android out of fuel fires the event and consumes the fuel item")
    void testConsumeFiresEvent() {
        Block b = placeAndroid(10, 10, new ItemStack(Material.COAL));

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onFuel(AndroidFuelConsumeEvent event) {
                seen[0] = true;
                Assertions.assertEquals(android, event.getAndroid());
                Assertions.assertEquals(b, event.getBlock());
                Assertions.assertEquals(Material.COAL, event.getFuelItem().getType());
                Assertions.assertNotNull(event.getMachineFuel());
                Assertions.assertEquals(16, event.getFuelTicks(), "A coal must grant its registered burn time");
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            tick(b);

            Assertions.assertTrue(seen[0], "AndroidFuelConsumeEvent was not fired");
            Assertions.assertEquals("16", BlockStorage.getLocationInfo(b.getLocation(), "fuel"), "The fuel level must have been refilled");
            assertFuelSlotEmpty(b);
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Scaling the fuel ticks via setFuelTicks changes the refill amount")
    void testSetFuelTicksScalesRefill() {
        Block b = placeAndroid(20, 20, new ItemStack(Material.COAL));

        Listener scaling = new Listener() {
            @EventHandler
            public void onFuel(AndroidFuelConsumeEvent event) {
                event.setFuelTicks(40);
            }
        };
        server.getPluginManager().registerEvents(scaling, plugin);

        try {
            tick(b);

            Assertions.assertEquals("40", BlockStorage.getLocationInfo(b.getLocation(), "fuel"), "The scaled fuel level must have been written");
            assertFuelSlotEmpty(b);
        } finally {
            HandlerList.unregisterAll(scaling);
        }
    }

    @Test
    @DisplayName("Cancelling AndroidFuelConsumeEvent keeps the fuel item and the empty tank")
    void testCancelSkipsConsumption() {
        Block b = placeAndroid(30, 30, new ItemStack(Material.COAL));

        Listener cancelling = new Listener() {
            @EventHandler
            public void onFuel(AndroidFuelConsumeEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            tick(b);

            ItemStack fuelSlot = BlockStorage.getInventory(b).getItemInSlot(FUEL_SLOT);
            Assertions.assertNotNull(fuelSlot, "A cancelled consumption must keep the fuel item");
            Assertions.assertEquals(Material.COAL, fuelSlot.getType());
            Assertions.assertEquals("0", BlockStorage.getLocationInfo(b.getLocation(), "fuel"), "A cancelled consumption must not refill the tank");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Consuming without listeners still refills, preserving the old behavior")
    void testConsumeWithoutListenersStillRefills() {
        Block b = placeAndroid(40, 40, new ItemStack(Material.COAL));

        tick(b);

        Assertions.assertEquals("16", BlockStorage.getLocationInfo(b.getLocation(), "fuel"), "The fuel level must have been refilled");
        assertFuelSlotEmpty(b);
    }

    @Test
    @DisplayName("A non-fuel item fires no event and stays put")
    void testNonFuelItemFiresNothing() {
        Block b = placeAndroid(50, 50, new ItemStack(Material.DIRT));

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onFuel(AndroidFuelConsumeEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            tick(b);

            Assertions.assertFalse(seen[0], "No event must be fired for a non-fuel item");
            ItemStack fuelSlot = BlockStorage.getInventory(b).getItemInSlot(FUEL_SLOT);
            Assertions.assertNotNull(fuelSlot, "The non-fuel item must have stayed in the slot");
            Assertions.assertEquals(Material.DIRT, fuelSlot.getType());
            Assertions.assertEquals("0", BlockStorage.getLocationInfo(b.getLocation(), "fuel"), "The tank must have stayed empty");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }
}
