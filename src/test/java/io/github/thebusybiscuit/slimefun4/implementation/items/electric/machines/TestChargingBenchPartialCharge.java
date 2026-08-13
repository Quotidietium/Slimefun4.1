package io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.Rechargeable;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

import me.mrCookieSlime.Slimefun.Objects.handlers.BlockTicker;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;

/**
 * Regression coverage for the {@link ChargingBench} energy accounting:
 * {@code addItemCharge()} clamps to the item's maximum charge, so when only part of the
 * intended charge fits, the bench must only consume the energy actually delivered
 * (2 J network energy per 1 J item charge) - previously the full operation cost was
 * deducted and the undelivered energy vanished.
 * <p>
 * The bench runs through its real {@link BlockTicker} against a {@link BlockStorage}-backed
 * block, with a 10 J rechargeable test item in the input slot (consumption 10 J,
 * capacity 64 J).
 *
 * @author Zurker
 */
class TestChargingBenchPartialCharge {

    private static final int INPUT_SLOT = 19;

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static ChargingBench bench;
    private static TestRechargeable rechargeable;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        ItemGroup group = new ItemGroup(new NamespacedKey(Slimefun.instance(), "charging"), new ItemStack(Material.GOLD_INGOT));
        Slimefun.getItemCfg().setValue("TEST_CHARGING_BENCH.enabled", true);
        Slimefun.getItemCfg().setValue("TEST_RECHARGEABLE_ITEM.enabled", true);

        bench = new ChargingBench(group, new SlimefunItemStack("TEST_CHARGING_BENCH", Material.DISPENSER, "Test Charging Bench"), RecipeType.NULL, new ItemStack[9]);
        bench.setCapacity(64).setEnergyConsumption(10).setProcessingSpeed(1);
        bench.register(plugin);

        rechargeable = new TestRechargeable(group, new SlimefunItemStack("TEST_RECHARGEABLE_ITEM", Material.DIAMOND_PICKAXE, "Test Rechargeable"));
        rechargeable.register(plugin);
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    /**
     * Places the bench backed by {@link BlockStorage} with the given stored charge and
     * returns its menu.
     */
    private BlockMenu placeBench(int x, int z, int storedCharge) {
        Block b = world.getBlockAt(x, 60, z);
        b.setType(Material.DISPENSER);
        BlockStorage.addBlockInfo(b, "id", bench.getId(), true);
        BlockStorage.addBlockInfo(b, "energy-charge", String.valueOf(storedCharge), false);
        return BlockStorage.getInventory(b);
    }

    private ItemStack chargedItem(float charge) {
        ItemStack item = rechargeable.getItem().clone();
        rechargeable.setItemCharge(item, charge);
        return item;
    }

    private void tick(BlockMenu menu) {
        bench.callItemHandler(BlockTicker.class, ticker -> ticker.tick(menu.getBlock(), bench, BlockStorage.getLocationInfo(menu.getLocation())));
    }

    private int storedCharge(BlockMenu menu) {
        return Integer.parseInt(BlockStorage.getLocationInfo(menu.getLocation(), "energy-charge"));
    }

    @Test
    @DisplayName("A partial charge only consumes the delivered fraction of the operation cost")
    void testPartialChargeConsumesDeliveredOnly() {
        BlockMenu menu = placeBench(10, 10, 32);
        menu.replaceExistingItem(INPUT_SLOT, chargedItem(9F), false);

        tick(menu);

        ItemStack item = menu.getItemInSlot(INPUT_SLOT);
        Assertions.assertNotNull(item, "The item must stay in the input slot");
        Assertions.assertEquals(10F, rechargeable.getItemCharge(item), "The item must have been topped up to its maximum");
        Assertions.assertEquals(30, storedCharge(menu),
            "Only 1 J of the 5 J charge could be delivered - only 2 J (not 10 J) of network energy may be consumed");
    }

    @Test
    @DisplayName("A full charge consumes the full operation cost, preserving the old behavior")
    void testFullChargeConsumesFullCost() {
        BlockMenu menu = placeBench(20, 20, 32);
        menu.replaceExistingItem(INPUT_SLOT, chargedItem(0F), false);

        tick(menu);

        ItemStack item = menu.getItemInSlot(INPUT_SLOT);
        Assertions.assertNotNull(item);
        Assertions.assertEquals(5F, rechargeable.getItemCharge(item), "Half the consumption is delivered per tick");
        Assertions.assertEquals(22, storedCharge(menu), "The full 10 J operation cost must be consumed");
    }

    @Test
    @DisplayName("A fully charged item is moved to the output without consuming energy")
    void testFullItemMovesToOutput() {
        BlockMenu menu = placeBench(30, 30, 32);
        menu.replaceExistingItem(INPUT_SLOT, chargedItem(10F), false);

        tick(menu);

        Assertions.assertNull(menu.getItemInSlot(INPUT_SLOT), "The finished item must leave the input slot");
        Assertions.assertEquals(32, storedCharge(menu), "No charge was delivered - no energy may be consumed");

        boolean found = false;
        for (int slot : bench.getOutputSlots()) {
            ItemStack out = menu.getItemInSlot(slot);

            if (out != null && rechargeable.isItem(out)) {
                found = true;
                break;
            }
        }

        Assertions.assertTrue(found, "The finished item must have been moved to the output slots");
    }

    /**
     * Minimal {@link Rechargeable} {@link SlimefunItem} with a 10 J maximum charge.
     */
    private static class TestRechargeable extends SlimefunItem implements Rechargeable {

        TestRechargeable(ItemGroup group, SlimefunItemStack item) {
            super(group, item, RecipeType.NULL, new ItemStack[9]);
        }

        @Override
        public float getMaxItemCharge(ItemStack item) {
            return 10F;
        }
    }
}
