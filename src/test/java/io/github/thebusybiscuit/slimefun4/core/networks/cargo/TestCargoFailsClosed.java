package io.github.thebusybiscuit.slimefun4.core.networks.cargo;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.WorldMock;

import io.github.thebusybiscuit.slimefun4.api.network.NetworkComponent;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;
import io.github.thebusybiscuit.slimefun4.utils.itemstack.ItemStackWrapper;

import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import me.mrCookieSlime.Slimefun.api.inventory.DirtyChestMenu;
import me.mrCookieSlime.Slimefun.api.item_transport.ItemTransportFlow;

/**
 * Regression coverage for fail-closed cargo handling of corrupted data and
 * preset-hook vetoes:
 * <ul>
 * <li>A corrupted frequency (non-numeric or overflowing the int range) skips the
 * node (-1) instead of rerouting it into channel 0 or crashing the manager.</li>
 * <li>Removing a node evicts its filterCache/roundRobin entries (unbounded growth).</li>
 * <li>The template-based vanilla withdraw clears the slot instead of leaving a
 * 0-amount ghost stack.</li>
 * <li>A preset whose {@code onItemStackChange} vetoes or shrinks a commit no longer
 * causes duplication (withdraw) or voiding (insert).</li>
 * </ul>
 *
 * @author Zurker
 */
class TestCargoFailsClosed {

    private static ServerMock server;
    private static Slimefun plugin;
    private static WorldMock world;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = (WorldMock) TestUtilities.createWorld(server);
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    @BeforeEach
    public void beforeEach() {
        server.getPluginManager().clearEvents();
    }

    private AbstractItemNetwork allowAllNetwork(Block node) {
        ItemFilter allowAll = Mockito.mock(ItemFilter.class);
        Mockito.when(allowAll.test(Mockito.any())).thenReturn(true);
        Mockito.when(allowAll.isDirty()).thenReturn(false);

        CargoNet network = Mockito.mock(CargoNet.class);
        Mockito.when(network.getItemFilter(Mockito.any())).thenReturn(allowAll);
        Mockito.when(network.getAttachedBlock(Mockito.any())).thenReturn(Optional.of(node));
        return network;
    }

    private static int getFrequency(Location node) {
        try {
            Method method = CargoNet.class.getDeclaredMethod("getFrequency", Location.class);
            method.setAccessible(true);
            return (int) method.invoke(null, node);
        } catch (ReflectiveOperationException x) {
            throw new IllegalStateException("getFrequency is not accessible", x);
        }
    }

    private Block nodeBlock(int x, int z, String frequency) {
        Block b = world.getBlockAt(x, 60, z);
        b.setType(Material.PLAYER_HEAD);
        BlockStorage.addBlockInfo(b, "id", "CARGO_NODE_INPUT");

        if (frequency != null) {
            BlockStorage.addBlockInfo(b, "frequency", frequency);
        }

        return b;
    }

    @Test
    @DisplayName("A corrupted frequency skips the node instead of rerouting to channel 0")
    void testCorruptedFrequencyFailsClosed() {
        Assertions.assertEquals(0, getFrequency(nodeBlock(1, 1, null).getLocation()), "A missing frequency defaults to channel 0");
        Assertions.assertEquals(5, getFrequency(nodeBlock(2, 2, "5").getLocation()), "A valid frequency is parsed");
        Assertions.assertEquals(-1, getFrequency(nodeBlock(3, 3, "abc").getLocation()), "A non-numeric frequency must skip the node");
        Assertions.assertEquals(-1, getFrequency(nodeBlock(4, 4, "99999999999999999999").getLocation()), "An overflowing frequency must skip the node (previously crashed the Cargo Manager)");
        Assertions.assertEquals(-2, getFrequency(nodeBlock(5, 5, "-2").getLocation()), "A negative frequency parses fine and is dropped by the 0-15 range guard");
    }

    @Test
    @DisplayName("Removing a node evicts its filter and round-robin cache entries")
    void testNodeRemovalEvictsCaches() {
        Location regulator = new Location(world, 10, 60, 10);
        CargoNet network = new CargoNet(regulator);

        Location node = new Location(world, 11, 60, 10);
        network.filterCache.put(node, Mockito.mock(ItemFilter.class));
        network.roundRobin.put(node, 3);

        network.onClassificationChange(node, NetworkComponent.TERMINUS, null);

        Assertions.assertFalse(network.filterCache.containsKey(node), "The removed node's filter must be evicted");
        Assertions.assertFalse(network.roundRobin.containsKey(node), "The removed node's round-robin index must be evicted");
    }

    @Test
    @DisplayName("The template-based vanilla withdraw clears the slot (no 0-amount ghost stack)")
    void testTemplateWithdrawClearsSlot() {
        Block chestBlock = world.getBlockAt(20, 60, 20);
        chestBlock.setType(Material.CHEST);
        Inventory inv = ((InventoryHolder) chestBlock.getState()).getInventory();
        inv.setItem(0, new ItemStack(Material.DIAMOND, 5));

        Block node = world.getBlockAt(19, 60, 20);
        ItemStack withdrawn = CargoUtils.withdrawFromVanillaInventory(allowAllNetwork(node), node, new ItemStack(Material.DIAMOND, 5), inv);

        Assertions.assertNotNull(withdrawn);
        Assertions.assertEquals(5, withdrawn.getAmount());
        Assertions.assertNull(inv.getItem(0), "The slot must be cleared - a 0-amount ghost stack would still be returned by getItem()");
    }

    /**
     * A preset routing every commit through the given onItemStackChange hook.
     * The hook receives (previous, next) and returns what is actually placed.
     */
    private DirtyChestMenu createHookedMenu(Block target, String id, java.util.function.BiFunction<ItemStack, ItemStack, ItemStack> hook) {
        new BlockMenuPreset(id, "hooked") {

            @Override
            public void init() {
                setSize(9);
            }

            @Override
            public boolean canOpen(Block b, Player p) {
                return true;
            }

            @Override
            public int[] getSlotsAccessedByItemTransport(ItemTransportFlow flow) {
                return new int[] { 0 };
            }

            @Override
            public ItemStack onItemStackChange(DirtyChestMenu menu, int slot, ItemStack previous, ItemStack next) {
                return hook.apply(previous, next);
            }
        };

        BlockStorage.addBlockInfo(target, "id", id);
        return BlockStorage.getInventory(target);
    }

    @Test
    @DisplayName("A preset vetoing the withdraw commit keeps the item in place and withdraws nothing")
    void testWithdrawHookVetoDoesNotDuplicate() {
        Block node = world.getBlockAt(30, 60, 30);
        Block target = world.getBlockAt(31, 60, 30);
        target.setType(Material.PLAYER_HEAD);

        // The hook vetoes every change: the previous content stays in the slot
        DirtyChestMenu menu = createHookedMenu(target, "VETO_WITHDRAW_MACHINE", (previous, next) -> previous);
        // event=false: stock the machine without routing through the vetoing hook
        menu.replaceExistingItem(0, new ItemStack(Material.DIAMOND, 5), false);

        ItemStackAndInteger withdrawn = CargoUtils.withdraw(allowAllNetwork(node), new HashMap<>(), node, target);

        Assertions.assertNull(withdrawn, "A vetoed extraction must withdraw nothing (previously the item was duplicated)");
        Assertions.assertEquals(5, menu.getItemInSlot(0).getAmount(), "The item must still be in the machine");
    }

    @Test
    @DisplayName("A preset vetoing the insert commit keeps the whole stack in transit")
    void testInsertHookVetoKeepsStackInTransit() {
        Block node = world.getBlockAt(40, 60, 40);
        Block target = world.getBlockAt(41, 60, 40);
        target.setType(Material.PLAYER_HEAD);

        DirtyChestMenu menu = createHookedMenu(target, "VETO_INSERT_MACHINE", (previous, next) -> previous);
        ItemStack stack = new ItemStack(Material.DIAMOND, 5);

        ItemStack rest = CargoUtils.insert(allowAllNetwork(node), new HashMap<>(), node, target, false, stack, ItemStackWrapper.wrap(stack));

        Assertions.assertNotNull(rest, "A vetoed insert must return the stack to the transit pipeline");
        Assertions.assertEquals(5, rest.getAmount(), "Nothing may be reported as inserted");
        Assertions.assertNull(menu.getItemInSlot(0), "The machine must have received nothing");
    }

    @Test
    @DisplayName("A preset shrinking the insert commit only reports the inserted part")
    void testInsertHookShrinkReturnsLeftover() {
        Block node = world.getBlockAt(50, 60, 50);
        Block target = world.getBlockAt(51, 60, 50);
        target.setType(Material.PLAYER_HEAD);

        DirtyChestMenu menu = createHookedMenu(target, "SHRINK_INSERT_MACHINE", (previous, next) -> {
            ItemStack shrunk = next.clone();
            shrunk.setAmount(Math.min(2, next.getAmount()));
            return shrunk;
        });
        ItemStack stack = new ItemStack(Material.DIAMOND, 5);

        ItemStack rest = CargoUtils.insert(allowAllNetwork(node), new HashMap<>(), node, target, false, stack, ItemStackWrapper.wrap(stack));

        Assertions.assertNotNull(rest, "The uninserted part must stay in transit");
        Assertions.assertEquals(3, rest.getAmount(), "Only the 2 items actually committed may be subtracted");
        Assertions.assertEquals(2, menu.getItemInSlot(0).getAmount(), "The machine holds the shrunken commit");
    }
}
