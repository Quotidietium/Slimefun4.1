package io.github.thebusybiscuit.slimefun4.core.attributes;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.networks.energy.EnergyNetComponentType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;
import me.mrCookieSlime.CSCoreLibPlugin.Configuration.Config;
import me.mrCookieSlime.Slimefun.api.BlockStorage;

/**
 * Regression tests for the {@code setCharge(Location, Config, int)} overload added by the
 * P2 optimisation. It must behave identically to {@code setCharge(Location, int)} while
 * reusing the caller-held {@link Config} (the EnergyNet settlement path).
 */
class TestEnergyNetComponent {

    private static ServerMock server;
    private static TestEnergyItem item;
    private World world;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        MockBukkit.load(Slimefun.class);

        ItemGroup group = new ItemGroup(new NamespacedKey(Slimefun.instance(), "energy"), new ItemStack(Material.EMERALD));
        item = new TestEnergyItem(group, new SlimefunItemStack("TEST_ENERGY_ITEM", Material.FURNACE, "Test Energy Item"), 128);
        item.register(Slimefun.instance());
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    @BeforeEach
    void setUp() {
        world = TestUtilities.createWorld(server);
    }

    @Test
    @DisplayName("Test setCharge(Location, Config, int) writes the charge value")
    void testSetChargeWithConfigWritesValue() {
        Location l = new Location(world, 0, 0, 0);
        BlockStorage.addBlockInfo(l, "id", item.getId(), false);

        Config data = BlockStorage.getLocationInfo(l);
        item.setCharge(l, data, 64);

        Assertions.assertEquals(64, item.getCharge(l, data));
        Assertions.assertEquals("64", BlockStorage.getLocationInfo(l, "energy-charge"));
    }

    @Test
    @DisplayName("Test setCharge(Location, Config, int) matches setCharge(Location, int)")
    void testSetChargeOverloadMatchesSingleArg() {
        Location l = new Location(world, 1, 0, 0);
        BlockStorage.addBlockInfo(l, "id", item.getId(), false);

        // Single-arg version.
        item.setCharge(l, 100);
        Assertions.assertEquals(100, item.getCharge(l));

        // Two-arg version on a different block must reach the same state.
        Location l2 = new Location(world, 2, 0, 0);
        BlockStorage.addBlockInfo(l2, "id", item.getId(), false);
        Config data2 = BlockStorage.getLocationInfo(l2);
        item.setCharge(l2, data2, 100);

        Assertions.assertEquals(item.getCharge(l), item.getCharge(l2));
    }

    @Test
    @DisplayName("Test setCharge clamps to capacity")
    void testSetChargeClampsToCapacity() {
        Location l = new Location(world, 3, 0, 0);
        BlockStorage.addBlockInfo(l, "id", item.getId(), false);

        Config data = BlockStorage.getLocationInfo(l);
        item.setCharge(l, data, 9999);

        Assertions.assertEquals(128, item.getCharge(l, data));
    }

    @Test
    @DisplayName("Test setCharge with equal value is a no-op (no write)")
    void testSetChargeNoOpWhenEqual() {
        Location l = new Location(world, 4, 0, 0);
        BlockStorage.addBlockInfo(l, "id", item.getId(), false);

        Config data = BlockStorage.getLocationInfo(l);
        item.setCharge(l, data, 50);
        String written = BlockStorage.getLocationInfo(l, "energy-charge");

        // Setting the same value again must not change the stored value.
        item.setCharge(l, data, 50);
        Assertions.assertEquals(written, BlockStorage.getLocationInfo(l, "energy-charge"));
        Assertions.assertEquals(50, item.getCharge(l, data));
    }

    /**
     * Minimal {@link EnergyNetComponent} {@link SlimefunItem} for testing the charge API.
     */
    private static class TestEnergyItem extends SlimefunItem implements EnergyNetComponent {
        private final int capacity;

        TestEnergyItem(ItemGroup group, SlimefunItemStack item, int capacity) {
            super(group, item, RecipeType.NULL, new ItemStack[9]);
            this.capacity = capacity;
        }

        @Override
        public int getCapacity() {
            return capacity;
        }

        @Override
        public EnergyNetComponentType getEnergyComponentType() {
            return EnergyNetComponentType.CONSUMER;
        }
    }
}
