package io.github.thebusybiscuit.slimefun4.implementation.items.multiblocks.miner;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

import io.github.thebusybiscuit.slimefun4.api.events.IndustrialMinerMineEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Regression coverage for the industrial miner mining API expansion:
 * {@link IndustrialMinerMineEvent}, exercised by driving the real
 * {@link MiningTask#mineColumn(Block)} against a one-column mining task.
 * <p>
 * MockBukkit cannot render {@code Effect.STEP_SOUND} for a {@link Material}, so the
 * post-push effect tail of a successful mine throws a RuntimeException; the tests catch
 * it and assert the pre-tail state instead (event fired, outcome pushed to the chest,
 * fuel consumed). The ore removal itself sits behind that unrenderable effect and is
 * deliberately not asserted. The skip paths (cancelled, non-ore, out of fuel) never
 * reach the effect and are asserted fully.
 *
 * @author Zurker
 */
class TestIndustrialMinerMineEvent {

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static IndustrialMiner miner;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        // The mining logic queries the protection manager for every scanned block.
        Slimefun.getIntegrations().start();
        server.getScheduler().performOneTick();

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "industrial_miner_mine_test");
        SlimefunItemStack stack = new SlimefunItemStack("_TEST_SILK_MINER", Material.BLAST_FURNACE, "&7Test Silk-Touch Miner");
        Slimefun.getItemCfg().setValue("_TEST_SILK_MINER.enabled", true);
        miner = new IndustrialMiner(itemGroup, stack, Material.IRON_BLOCK, true, 3);
        miner.register(plugin);
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
     * Builds a running one-column mining task: a blast furnace base with a fueled chest
     * above, scanning only the given column block.
     */
    private MiningTask startMiner(Player owner, int bx, int bz, Block column, ItemStack fuel) {
        Block base = world.getBlockAt(bx, 60, bz);
        base.setType(Material.BLAST_FURNACE);
        Block chestBlock = base.getRelative(BlockFace.UP);
        chestBlock.setType(Material.CHEST);

        if (fuel != null) {
            blockInventory(chestBlock).setItem(0, fuel);
        }

        Block[] pistons = { chestBlock.getRelative(BlockFace.NORTH), chestBlock.getRelative(BlockFace.SOUTH) };
        MiningTask task = new MiningTask(miner, owner.getUniqueId(), chestBlock, pistons, column, column);
        task.start(base);
        return task;
    }

    /**
     * Runs the mining logic for the task's single column, catching the unrenderable
     * effect tail of a successful mine.
     */
    private void mineColumn(MiningTask task, Block base) {
        try {
            task.mineColumn(base);
        } catch (RuntimeException x) {
            // MockBukkit cannot render STEP_SOUND for a Material; the mine itself already happened.
        }
    }

    private Inventory blockInventory(Block chestBlock) {
        return ((Chest) chestBlock.getState()).getBlockInventory();
    }

    @Test
    @DisplayName("IndustrialMinerMineEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Player player = server.addPlayer();
        Block ore = world.getBlockAt(1, 62, 1);
        ItemStack outcome = new ItemStack(Material.COAL_ORE);

        IndustrialMinerMineEvent event = new IndustrialMinerMineEvent(miner, player, ore, outcome);

        Assertions.assertEquals(miner, event.getMiner());
        Assertions.assertEquals(player, event.getOwner());
        Assertions.assertEquals(ore, event.getBlock());
        Assertions.assertEquals(outcome, event.getOutcome());
        Assertions.assertFalse(event.isCancelled());

        ItemStack replacement = new ItemStack(Material.DIAMOND, 2);
        event.setOutcome(replacement);
        Assertions.assertEquals(replacement, event.getOutcome());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new IndustrialMinerMineEvent(null, player, ore, outcome));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new IndustrialMinerMineEvent(miner, null, ore, outcome));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new IndustrialMinerMineEvent(miner, player, null, outcome));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new IndustrialMinerMineEvent(miner, player, ore, null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setOutcome(null));
    }

    @Test
    @DisplayName("Mining an ore fires the event and pushes the outcome to the chest")
    void testMineFiresEventAndPushesOutcome() {
        Player player = server.addPlayer();
        Block ore = world.getBlockAt(105, 62, 100);
        ore.setType(Material.COAL_ORE);
        MiningTask task = startMiner(player, 100, 100, ore, new ItemStack(Material.COAL));
        Block chestBlock = world.getBlockAt(100, 61, 100);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onMine(IndustrialMinerMineEvent event) {
                seen[0] = true;
                Assertions.assertEquals(miner, event.getMiner());
                Assertions.assertEquals(player.getUniqueId(), event.getOwner().getUniqueId());
                Assertions.assertEquals(ore, event.getBlock());
                Assertions.assertEquals(Material.COAL_ORE, event.getOutcome().getType());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            mineColumn(task, world.getBlockAt(100, 60, 100));

            Assertions.assertTrue(seen[0], "IndustrialMinerMineEvent was not fired");
            Assertions.assertTrue(blockInventory(chestBlock).contains(Material.COAL_ORE), "The outcome must have been pushed to the chest");
            ItemStack fuelSlot = blockInventory(chestBlock).getItem(0);
            Assertions.assertTrue(fuelSlot == null || fuelSlot.getAmount() == 0, "The fuel must have been consumed");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Replacing the outcome via setOutcome pushes the replacement to the chest")
    void testSetOutcomeRedirectsYield() {
        Player player = server.addPlayer();
        Block ore = world.getBlockAt(150, 62, 150);
        ore.setType(Material.COAL_ORE);
        MiningTask task = startMiner(player, 150, 150, ore, new ItemStack(Material.COAL));
        Block chestBlock = world.getBlockAt(150, 61, 150);

        Listener redirecting = new Listener() {
            @EventHandler
            public void onMine(IndustrialMinerMineEvent event) {
                Assertions.assertEquals(Material.COAL_ORE, event.getOutcome().getType(), "The outcome must default to the rolled yield");
                event.setOutcome(new ItemStack(Material.DIAMOND, 2));
            }
        };
        server.getPluginManager().registerEvents(redirecting, plugin);

        try {
            mineColumn(task, world.getBlockAt(150, 60, 150));

            Assertions.assertTrue(blockInventory(chestBlock).contains(Material.DIAMOND), "The replacement must have been pushed to the chest");
            Assertions.assertFalse(blockInventory(chestBlock).contains(Material.COAL_ORE), "The default yield must not have been pushed");
            ItemStack fuelSlot = blockInventory(chestBlock).getItem(0);
            Assertions.assertTrue(fuelSlot == null || fuelSlot.getAmount() == 0, "The fuel must still have been consumed");
        } finally {
            HandlerList.unregisterAll(redirecting);
        }
    }

    @Test
    @DisplayName("Cancelling IndustrialMinerMineEvent leaves the ore in the ground")
    void testCancelKeepsOre() {
        Player player = server.addPlayer();
        Block ore = world.getBlockAt(205, 62, 200);
        ore.setType(Material.COAL_ORE);
        MiningTask task = startMiner(player, 200, 200, ore, new ItemStack(Material.COAL));
        Block chestBlock = world.getBlockAt(200, 61, 200);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onMine(IndustrialMinerMineEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            task.mineColumn(world.getBlockAt(200, 60, 200));

            Assertions.assertEquals(Material.COAL_ORE, ore.getType(), "A vetoed ore must stay in the ground");
            Assertions.assertFalse(blockInventory(chestBlock).contains(Material.COAL_ORE), "A vetoed ore must not reach the chest");
            Assertions.assertTrue(blockInventory(chestBlock).contains(Material.COAL), "A vetoed ore must not consume fuel");
            Assertions.assertFalse(miner.activeMiners.containsKey(world.getBlockAt(200, 60, 200).getLocation()), "The finished miner must have been unregistered");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Mining without listeners still pushes the outcome, preserving the old behavior")
    void testMineWithoutListenersMines() {
        Player player = server.addPlayer();
        Block ore = world.getBlockAt(305, 62, 300);
        ore.setType(Material.COAL_ORE);
        MiningTask task = startMiner(player, 300, 300, ore, new ItemStack(Material.COAL));
        Block chestBlock = world.getBlockAt(300, 61, 300);

        mineColumn(task, world.getBlockAt(300, 60, 300));

        Assertions.assertTrue(blockInventory(chestBlock).contains(Material.COAL_ORE), "The outcome must have been pushed to the chest");
    }

    @Test
    @DisplayName("A column without ores fires no event")
    void testNonOreColumnFiresNothing() {
        Player player = server.addPlayer();
        Block stone = world.getBlockAt(405, 62, 400);
        stone.setType(Material.STONE);
        MiningTask task = startMiner(player, 400, 400, stone, new ItemStack(Material.COAL));
        Block chestBlock = world.getBlockAt(400, 61, 400);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onMine(IndustrialMinerMineEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            task.mineColumn(world.getBlockAt(400, 60, 400));

            Assertions.assertFalse(seen[0], "No event must be fired without an ore");
            Assertions.assertEquals(Material.STONE, stone.getType(), "The stone must be untouched");
            Assertions.assertTrue(blockInventory(chestBlock).contains(Material.COAL), "No fuel must have been consumed");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("The event fires before the push, so an out-of-fuel miner fires but cannot mine")
    void testOutOfFuelFiresButCannotMine() {
        Player player = server.addPlayer();
        Block ore = world.getBlockAt(505, 62, 500);
        ore.setType(Material.COAL_ORE);
        MiningTask task = startMiner(player, 500, 500, ore, null);
        Block chestBlock = world.getBlockAt(500, 61, 500);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onMine(IndustrialMinerMineEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            task.mineColumn(world.getBlockAt(500, 60, 500));

            Assertions.assertTrue(seen[0], "The event must fire before the push is even attempted");
            Assertions.assertEquals(Material.COAL_ORE, ore.getType(), "An out-of-fuel miner must not mine the ore");
            Assertions.assertFalse(blockInventory(chestBlock).contains(Material.COAL_ORE), "No outcome must have been pushed");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }
}
