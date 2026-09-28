package io.github.thebusybiscuit.slimefun4.core.networks.cargo;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.WorldMock;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import me.mrCookieSlime.Slimefun.api.inventory.DirtyChestMenu;
import me.mrCookieSlime.Slimefun.api.item_transport.ItemTransportFlow;

/**
 * Discriminant tests for the cargo insert-scan optimizations: a Material
 * pre-check skips {@code isItemSimilar()} for provably non-matching slots, the
 * {@link io.github.thebusybiscuit.slimefun4.utils.itemstack.ItemStackWrapper}
 * for the in-transit stack is created lazily, and the destination list is
 * iterated without a defensive copy. All three must be semantically
 * invisible: matching (including meta-sensitive matching) must still work
 * exactly as before, and non-matching slots must remain untouched.
 *
 * @author Zurker
 */
class TestCargoInsertScanSemantics {

    private static ServerMock server;
    private static Slimefun plugin;
    private static WorldMock world;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = server.addSimpleWorld("cargo-insert-scan");
        // The machine-target test stores block data, which needs a BlockStorage
        Slimefun.getRegistry().getWorlds().put(world.getName(), new me.mrCookieSlime.Slimefun.api.BlockStorage(world));
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    private CargoNet allowAllNetwork() {
        CargoNet network = Mockito.mock(CargoNet.class);

        ItemFilter allowAll = Mockito.mock(ItemFilter.class);
        Mockito.when(allowAll.test(Mockito.any())).thenReturn(true);
        Mockito.when(network.getItemFilter(Mockito.any())).thenReturn(allowAll);

        return network;
    }

    /**
     * Places a chest at the given coordinates and returns its inventory.
     */
    private Inventory chestAt(int x, int z) {
        Block block = world.getBlockAt(x, 60, z);
        block.setType(Material.CHEST);
        return ((InventoryHolder) block.getState()).getInventory();
    }

    /**
     * Inserts into the chest at the given coordinates via the real
     * {@link CargoUtils#insert} vanilla path.
     */
    private ItemStack insert(int x, int z, ItemStack stack, boolean smartFill) {
        CargoNet network = allowAllNetwork();
        Block node = world.getBlockAt(x, 59, z);
        Block target = world.getBlockAt(x, 60, z);

        return CargoUtils.insert(network, new HashMap<>(), node, target, smartFill, stack);
    }

    @Test
    @DisplayName("Insert merges only into the matching slot of a mixed partial-stack chest")
    void testMixedPartialStacksMergeIntoMatchingSlot() {
        Inventory chest = chestAt(10, 10);
        chest.setItem(0, new ItemStack(Material.STONE, 3));
        chest.setItem(1, new ItemStack(Material.DIAMOND, 5));

        ItemStack rest = insert(10, 10, new ItemStack(Material.DIAMOND, 5), false);

        Assertions.assertNull(rest, "The chest has room, everything must be inserted");
        Assertions.assertEquals(Material.STONE, chest.getItem(0).getType(), "The mismatching slot must keep its material");
        Assertions.assertEquals(3, chest.getItem(0).getAmount(), "The mismatching slot must be untouched");
        Assertions.assertEquals(10, chest.getItem(1).getAmount(), "The matching partial stack must have absorbed the items");
    }

    @Test
    @DisplayName("A lored stack does not merge into a lore-less stack of the same material")
    void testLazyWrapperKeepsMetaSensitiveMatching() {
        // isItemSimilar(checkLore=true) ignores enchantments but compares lore
        Inventory chest = chestAt(20, 20);
        chest.setItem(0, new ItemStack(Material.DIAMOND, 5));

        ItemStack lored = new ItemStack(Material.DIAMOND, 2);
        lored.editMeta(meta -> meta.setLore(java.util.List.of("sigil")));
        chest.setItem(1, lored.clone());

        ItemStack rest = insert(20, 20, lored.clone(), false);

        Assertions.assertNull(rest, "The identical lored stack must absorb everything");
        Assertions.assertEquals(5, chest.getItem(0).getAmount(), "The lore-less stack must not have absorbed lored items");
        Assertions.assertFalse(chest.getItem(0).hasItemMeta() && chest.getItem(0).getItemMeta().hasLore(), "The first slot must stay lore-less");
        Assertions.assertEquals(4, chest.getItem(1).getAmount(), "The lored stack must have merged");
        Assertions.assertTrue(chest.getItem(1).getItemMeta().hasLore(), "The merged stack must keep its lore");
    }

    @Test
    @DisplayName("A chest full of other materials bounces the item without touching any slot")
    void testAllMismatchingBouncesUntouched() {
        Inventory chest = chestAt(30, 30);

        for (int slot = 0; slot < chest.getSize(); slot++) {
            chest.setItem(slot, new ItemStack(Material.STONE, 1));
        }

        ItemStack rest = insert(30, 30, new ItemStack(Material.DIAMOND, 5), false);

        Assertions.assertNotNull(rest, "A chest without any acceptable slot must bounce the item");
        Assertions.assertEquals(5, rest.getAmount(), "Nothing may be voided");

        for (int slot = 0; slot < chest.getSize(); slot++) {
            Assertions.assertEquals(Material.STONE, chest.getItem(slot).getType(), "Slot " + slot + " must be untouched");
            Assertions.assertEquals(1, chest.getItem(slot).getAmount(), "Slot " + slot + " must keep its amount");
        }
    }

    @Test
    @DisplayName("Smart-fill with full mismatching stacks keeps scanning without merging")
    void testSmartFillFullMismatchingBounces() {
        Inventory chest = chestAt(40, 40);

        for (int slot = 0; slot < chest.getSize(); slot++) {
            chest.setItem(slot, new ItemStack(Material.STONE, 64));
        }

        ItemStack rest = insert(40, 40, new ItemStack(Material.DIAMOND, 5), true);

        Assertions.assertNotNull(rest, "No slot accepts the item, it must stay in transit");
        Assertions.assertEquals(5, rest.getAmount(), "Nothing may be voided or merged elsewhere");

        for (int slot = 0; slot < chest.getSize(); slot++) {
            Assertions.assertEquals(64, chest.getItem(slot).getAmount(), "Slot " + slot + " must stay full");
        }
    }

    @Test
    @DisplayName("A merge that exceeds the stack size returns the leftover")
    void testMergeOverflowReturnsLeftover() {
        Inventory chest = chestAt(50, 50);
        chest.setItem(0, new ItemStack(Material.DIAMOND, 60));

        ItemStack rest = insert(50, 50, new ItemStack(Material.DIAMOND, 10), false);

        Assertions.assertNotNull(rest, "Only 4 of the 10 items fit, the rest stays in transit");
        Assertions.assertEquals(6, rest.getAmount(), "Exactly the overflow may remain");
        Assertions.assertEquals(64, chest.getItem(0).getAmount(), "The slot must be filled to the max stack size");
    }

    @Test
    @DisplayName("A machine target skips mismatching menu slots and fills the empty one")
    void testMachineInsertSkipsMismatchingSlots() {
        Block target = world.getBlockAt(60, 60, 60);
        target.setType(Material.DISPENSER);

        // A machine menu with the usual input slots 19/20, transport-routed
        new BlockMenuPreset("INSERT_SCAN_MACHINE", "scan") {

            @Override
            public void init() {
                setSize(27);
            }

            @Override
            public boolean canOpen(Block b, Player p) {
                return true;
            }

            @Override
            public int[] getSlotsAccessedByItemTransport(ItemTransportFlow flow) {
                return new int[] { 19, 20 };
            }
        };

        BlockStorage.addBlockInfo(target, "id", "INSERT_SCAN_MACHINE");
        BlockMenu menu = BlockStorage.getInventory(target);
        menu.replaceExistingItem(19, new ItemStack(Material.STONE, 3), false);

        CargoNet network = allowAllNetwork();
        Block node = world.getBlockAt(60, 59, 60);

        ItemStack rest = CargoUtils.insert(network, new HashMap<>(), node, target, false, new ItemStack(Material.DIAMOND, 5));

        Assertions.assertNull(rest, "The empty input slot must accept the item");
        Assertions.assertEquals(Material.STONE, menu.getItemInSlot(19).getType(), "The mismatching menu slot must be untouched");
        Assertions.assertEquals(3, menu.getItemInSlot(19).getAmount(), "The mismatching menu slot must keep its amount");
        Assertions.assertEquals(5, menu.getItemInSlot(20).getAmount(), "The empty slot must hold the inserted item");
    }

    @Test
    @DisplayName("Distribution without a defensive copy still reaches every output")
    void testDistributionIteratesAllOutputs() throws Exception {
        Location inputNode = new Location(world, 80, 60, 79);
        world.getBlockAt(inputNode).setType(Material.STONE);
        Block inputChest = world.getBlockAt(81, 60, 80);
        inputChest.setType(Material.CHEST);
        getInventory(81, 80).setItem(0, new ItemStack(Material.DIAMOND, 5));

        Inventory firstOutput = chestAt(90, 90);
        Inventory secondOutput = chestAt(95, 95);

        // The first output only has mismatching partial stacks: it must bounce
        for (int slot = 0; slot < firstOutput.getSize(); slot++) {
            firstOutput.setItem(slot, new ItemStack(Material.STONE, 1));
        }

        // Nodes sit one block BEHIND their chests: setting the node to STONE must
        // never overwrite the chest block it is attached to.
        Location firstNode = new Location(world, 90, 60, 89);
        Location secondNode = new Location(world, 95, 60, 94);
        world.getBlockAt(firstNode).setType(Material.STONE);
        world.getBlockAt(secondNode).setType(Material.STONE);

        CargoNet network = allowAllNetwork();
        Mockito.when(network.getAttachedBlock(inputNode)).thenReturn(Optional.of(inputChest));
        Mockito.when(network.getAttachedBlock(firstNode)).thenReturn(Optional.of(world.getBlockAt(90, 60, 90)));
        Mockito.when(network.getAttachedBlock(secondNode)).thenReturn(Optional.of(world.getBlockAt(95, 60, 95)));
        Mockito.when(network.getRegulator()).thenReturn(new Location(world, 0, 60, 0));

        Map<Location, Integer> inputs = new HashMap<>();
        inputs.put(inputNode, 0);
        Map<Integer, List<Location>> outputs = new HashMap<>();
        List<Location> outputNodes = new ArrayList<>();
        outputNodes.add(firstNode);
        outputNodes.add(secondNode);
        outputs.put(0, outputNodes);

        Method routeItems = CargoNetworkTask.class.getDeclaredMethod("routeItems", Location.class, Block.class, int.class, Map.class);
        routeItems.setAccessible(true);

        try {
            routeItems.invoke(new CargoNetworkTask(network, inputs, outputs), inputNode, inputChest, 0, outputs);
        } catch (InvocationTargetException x) {
            if (x.getCause() instanceof RuntimeException cause) {
                throw cause;
            }

            throw new IllegalStateException("routeItems failed", x.getCause());
        }

        Assertions.assertEquals(0, countMaterial(getInventory(81, 80), Material.DIAMOND), "The source chest must have been emptied");
        Assertions.assertEquals(0, countMaterial(firstOutput, Material.DIAMOND), "The mismatching output must not have received anything");
        Assertions.assertEquals(5, countMaterial(secondOutput, Material.DIAMOND), "The second output must have received everything");
    }

    private Inventory getInventory(int x, int z) {
        return ((InventoryHolder) world.getBlockAt(x, 60, z).getState()).getInventory();
    }

    private int countMaterial(Inventory inv, Material material) {
        int total = 0;

        for (ItemStack item : inv.getContents()) {
            if (item != null && item.getType() == material) {
                total += item.getAmount();
            }
        }

        return total;
    }
}
