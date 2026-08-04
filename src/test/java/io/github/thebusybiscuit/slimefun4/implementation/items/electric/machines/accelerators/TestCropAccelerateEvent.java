package io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines.accelerators;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
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
import org.mockito.Mockito;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

import io.github.thebusybiscuit.slimefun4.api.events.CropAccelerateEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;

/**
 * Regression coverage for the crop growth accelerator API expansion:
 * {@link CropAccelerateEvent}, exercised through the real {@link CropGrowthAccelerator#tick(Block)}
 * growth path.
 * <p>
 * MockBukkit has no {@link Ageable} block data for crops, so both the accelerator and the
 * crop are Mockito hybrids over a real {@link BlockStorage}-registered location with a real
 * {@link BlockMenu}, following the fluid pump pattern.
 *
 * @author Zurker
 */
class TestCropAccelerateEvent {

    private static final int ENERGY_CONSUMPTION = 32;

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static CropGrowthAccelerator accelerator;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        // Unit test startups never start the integrations, the ProtectionManager is created there
        Slimefun.getIntegrations().start();
        server.getScheduler().performOneTick();

        // A BlockTicker item stays DISABLED while tickers are off and non-configurable items
        // stay DISABLED unless Items.yml says otherwise, so enable both before registering.
        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "crop_accelerator_test");
        SlimefunItemStack stack = new SlimefunItemStack("TEST_CROP_ACCELERATOR", Material.DISPENSER, "&aTest Crop Accelerator");
        Slimefun.getCfg().setValue("URID.enable-tickers", true);
        Slimefun.getItemCfg().setValue("TEST_CROP_ACCELERATOR.enabled", true);
        accelerator = new CropGrowthAccelerator(itemGroup, stack, RecipeType.NULL, new ItemStack[9]) {

            @Override
            public int getEnergyConsumption() {
                return ENERGY_CONSUMPTION;
            }

            @Override
            public int getRadius() {
                return 1;
            }

            @Override
            public int getSpeed() {
                return 1;
            }
        };
        accelerator.register(plugin);
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
     * Registers a charged accelerator with one fertilizer at the given position and
     * returns a mocked accelerator block whose {@code getRelative()} scan finds the
     * given crop at offset (1, 0, 0). The inventory behind it is entirely real.
     */
    private Block setupAccelerator(int x, int z, Block crop) {
        Block real = world.getBlockAt(x, 1, z);
        real.setType(Material.DISPENSER);
        BlockStorage.addBlockInfo(real, "id", accelerator.getId(), true);
        BlockStorage.addBlockInfo(real.getLocation(), "energy-charge", "1024", false);

        BlockMenu menu = BlockStorage.getInventory(real);
        menu.replaceExistingItem(10, SlimefunItems.FERTILIZER.item().clone());

        Location loc = real.getLocation();
        Block machine = Mockito.mock(Block.class);
        Mockito.when(machine.getType()).thenReturn(Material.DISPENSER);
        Mockito.when(machine.getLocation()).thenReturn(loc);
        Mockito.when(machine.getWorld()).thenReturn(world);
        Mockito.when(machine.getRelative(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt())).thenAnswer(inv -> {
            int dx = inv.getArgument(0);
            int dz = inv.getArgument(2);

            if (dx == 1 && dz == 0) {
                return crop;
            }

            return world.getBlockAt(loc.getBlockX() + dx, loc.getBlockY(), loc.getBlockZ() + dz);
        });
        return machine;
    }

    /**
     * Creates a mocked crop block at offset (1, 0, 0) from the accelerator position,
     * backed by a mocked {@link Ageable} with the given age.
     */
    private Block mockCrop(int x, int z, int age, int maximumAge) {
        Ageable ageable = Mockito.mock(Ageable.class);
        Mockito.when(ageable.getAge()).thenReturn(age);
        Mockito.when(ageable.getMaximumAge()).thenReturn(maximumAge);

        Block crop = Mockito.mock(Block.class);
        Mockito.when(crop.getType()).thenReturn(Material.WHEAT);
        Mockito.when(crop.getBlockData()).thenReturn(ageable);
        Mockito.when(crop.getWorld()).thenReturn(world);
        Mockito.when(crop.getLocation()).thenReturn(new Location(world, x + 1, 1, z));
        return crop;
    }

    /**
     * Ticks the accelerator once. The growth path ends in a particle effect that
     * MockBukkit does not fully support, so a RuntimeException from that tail is
     * ignored here - the event was fired and the resources consumed beforehand.
     */
    private void tick(Block machine) {
        try {
            accelerator.tick(machine);
        } catch (RuntimeException ignored) {
            // See the javadoc above
        }
    }

    @Test
    @DisplayName("CropAccelerateEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Block b = world.getBlockAt(1, 1, 1);
        Block crop = world.getBlockAt(2, 1, 1);
        ItemStack fertilizer = SlimefunItems.FERTILIZER.item().clone();

        CropAccelerateEvent event = new CropAccelerateEvent(accelerator, b, crop, fertilizer);

        Assertions.assertEquals(accelerator, event.getAccelerator());
        Assertions.assertEquals(b, event.getBlock());
        Assertions.assertEquals(crop, event.getCrop());
        Assertions.assertEquals(fertilizer, event.getFertilizer());
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new CropAccelerateEvent(null, b, crop, fertilizer));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new CropAccelerateEvent(accelerator, null, crop, fertilizer));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new CropAccelerateEvent(accelerator, b, null, fertilizer));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new CropAccelerateEvent(accelerator, b, crop, null));
    }

    @Test
    @DisplayName("Accelerating a crop fires the event, consumes energy and fertilizer and grows the crop")
    void testAccelerateFiresAndGrows() {
        Block crop = mockCrop(10, 10, 0, 7);
        Ageable ageable = (Ageable) crop.getBlockData();
        Block machine = setupAccelerator(10, 10, crop);
        BlockMenu menu = BlockStorage.getInventory(machine.getLocation());

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onAccelerate(CropAccelerateEvent event) {
                seen[0] = true;
                Assertions.assertEquals(accelerator, event.getAccelerator());
                Assertions.assertEquals(machine, event.getBlock());
                Assertions.assertEquals(crop, event.getCrop());
                Assertions.assertTrue(SlimefunItems.FERTILIZER.item().isSimilar(event.getFertilizer()), "The fertilizer must be Slimefun fertilizer");
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            tick(machine);

            Assertions.assertTrue(seen[0], "CropAccelerateEvent was not fired");
            Assertions.assertEquals(1024 - ENERGY_CONSUMPTION, accelerator.getCharge(machine.getLocation()), "The energy must have been consumed");
            Assertions.assertEquals(0, menu.getItemInSlot(10).getAmount(), "The fertilizer must have been consumed");
            Mockito.verify(ageable).setAge(1);
            Mockito.verify(crop).setBlockData(ageable);
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling CropAccelerateEvent skips the crop and keeps the resources")
    void testEventCancellationSkipsCrop() {
        Block crop = mockCrop(20, 20, 0, 7);
        Block machine = setupAccelerator(20, 20, crop);
        BlockMenu menu = BlockStorage.getInventory(machine.getLocation());

        Listener cancelling = new Listener() {
            @EventHandler
            public void onAccelerate(CropAccelerateEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            tick(machine);

            Assertions.assertEquals(1024, accelerator.getCharge(machine.getLocation()), "A cancelled acceleration must keep the energy");
            Assertions.assertEquals(1, menu.getItemInSlot(10).getAmount(), "A cancelled acceleration must keep the fertilizer");
            Mockito.verify(crop, Mockito.never()).setBlockData(Mockito.any());
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Acceleration without listeners still grows, preserving the old behavior")
    void testAccelerateWithoutListenersStillGrows() {
        Block crop = mockCrop(30, 30, 0, 7);
        Ageable ageable = (Ageable) crop.getBlockData();
        Block machine = setupAccelerator(30, 30, crop);

        tick(machine);

        Assertions.assertEquals(1024 - ENERGY_CONSUMPTION, accelerator.getCharge(machine.getLocation()), "The energy must have been consumed");
        Mockito.verify(crop).setBlockData(ageable);
    }

    @Test
    @DisplayName("An accelerator without fertilizer fires no event")
    void testNoFertilizerFiresNothing() {
        Block crop = mockCrop(40, 40, 0, 7);
        Block machine = setupAccelerator(40, 40, crop);
        BlockMenu menu = BlockStorage.getInventory(machine.getLocation());
        menu.replaceExistingItem(10, null);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onAccelerate(CropAccelerateEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            tick(machine);

            Assertions.assertFalse(seen[0], "No event must be fired without fertilizer");
            Mockito.verify(crop, Mockito.never()).setBlockData(Mockito.any());
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("An accelerator without energy fires no event")
    void testNoEnergyFiresNothing() {
        Block crop = mockCrop(50, 50, 0, 7);
        Block machine = setupAccelerator(50, 50, crop);
        BlockStorage.addBlockInfo(machine.getLocation(), "energy-charge", "0", true);
        BlockMenu menu = BlockStorage.getInventory(machine.getLocation());

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onAccelerate(CropAccelerateEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            tick(machine);

            Assertions.assertFalse(seen[0], "No event must be fired without energy");
            Assertions.assertEquals(1, menu.getItemInSlot(10).getAmount(), "The fertilizer must have been kept");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("A fully grown crop fires no event")
    void testFullyGrownCropFiresNothing() {
        Block crop = mockCrop(60, 60, 7, 7);
        Block machine = setupAccelerator(60, 60, crop);
        BlockMenu menu = BlockStorage.getInventory(machine.getLocation());

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onAccelerate(CropAccelerateEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            tick(machine);

            Assertions.assertFalse(seen[0], "No event must be fired for a fully grown crop");
            Assertions.assertEquals(1, menu.getItemInSlot(10).getAmount(), "The fertilizer must have been kept");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }
}
