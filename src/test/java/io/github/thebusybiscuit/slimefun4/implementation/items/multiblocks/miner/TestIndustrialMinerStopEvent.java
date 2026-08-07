package io.github.thebusybiscuit.slimefun4.implementation.items.multiblocks.miner;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

import io.github.thebusybiscuit.slimefun4.api.events.IndustrialMinerStopEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Regression coverage for the industrial miner API expansion:
 * {@link IndustrialMinerStopEvent}, exercised by starting a miner via the real
 * {@link IndustrialMiner#onInteract} and then driving the registered
 * {@link MiningTask#stop(MinerStoppingReason)} error-stop path.
 * <p>
 * The event is informational (not cancellable) and fired right before the task is
 * removed from {@code activeMiners}, so the outcome is asserted end-to-end: the task is
 * gone after the stop, and the reason and ore count are reported.
 *
 * @author Zurker
 */
class TestIndustrialMinerStopEvent {

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static IndustrialMiner miner;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "industrial_miner_stop_test");
        SlimefunItemStack stack = new SlimefunItemStack("_TEST_STOP_MINER", Material.BLAST_FURNACE, "&7Test Stop Miner");
        Slimefun.getItemCfg().setValue("_TEST_STOP_MINER.enabled", true);
        miner = new IndustrialMiner(itemGroup, stack, Material.IRON_BLOCK, false, 3);
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

    private Block minerBase(int x, int z) {
        Block b = world.getBlockAt(x, 60, z);
        b.setType(Material.BLAST_FURNACE);
        return b;
    }

    /**
     * Starts a miner and returns its registered {@link MiningTask}.
     */
    private MiningTask startMiner(Player player, Block b) {
        miner.onInteract(player, b);
        return miner.activeMiners.get(b.getLocation());
    }

    @Test
    @DisplayName("IndustrialMinerStopEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Block b = minerBase(1, 1);
        Block chest = b.getRelative(org.bukkit.block.BlockFace.UP);

        IndustrialMinerStopEvent event = new IndustrialMinerStopEvent(miner, chest, MinerStoppingReason.NO_FUEL, 12);

        Assertions.assertEquals(miner, event.getMiner());
        Assertions.assertEquals(chest, event.getChest());
        Assertions.assertEquals(MinerStoppingReason.NO_FUEL, event.getReason());
        Assertions.assertEquals(12, event.getOresMined());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new IndustrialMinerStopEvent(null, chest, MinerStoppingReason.NO_FUEL, 0));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new IndustrialMinerStopEvent(miner, null, MinerStoppingReason.NO_FUEL, 0));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new IndustrialMinerStopEvent(miner, chest, null, 0));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new IndustrialMinerStopEvent(miner, chest, MinerStoppingReason.NO_FUEL, -1));
    }

    @Test
    @DisplayName("Stopping a miner with a reason fires the event and unregisters the task")
    void testStopFiresEventAndUnregisters() {
        Player player = server.addPlayer();
        Block b = minerBase(10, 10);
        MiningTask task = startMiner(player, b);
        Assertions.assertNotNull(task, "The miner must have started a task");

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onStop(IndustrialMinerStopEvent event) {
                seen[0] = true;
                Assertions.assertEquals(miner, event.getMiner());
                Assertions.assertEquals(MinerStoppingReason.CHEST_FULL, event.getReason());
                Assertions.assertEquals(0, event.getOresMined(), "No ores were mined before the stop");
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            task.stop(MinerStoppingReason.CHEST_FULL);

            Assertions.assertTrue(seen[0], "IndustrialMinerStopEvent was not fired");
            Assertions.assertFalse(miner.activeMiners.containsKey(b.getLocation()), "The task must have been unregistered");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Stopping without listeners still unregisters, preserving the old behavior")
    void testStopWithoutListenersUnregisters() {
        Player player = server.addPlayer();
        Block b = minerBase(20, 20);
        MiningTask task = startMiner(player, b);
        Assertions.assertNotNull(task);

        task.stop(MinerStoppingReason.NO_FUEL);

        Assertions.assertFalse(miner.activeMiners.containsKey(b.getLocation()), "The task must have been unregistered");
    }

    @Test
    @DisplayName("The reason is reported faithfully for each stop condition")
    void testReasonReported() {
        MinerStoppingReason[] reasons = { MinerStoppingReason.NO_FUEL, MinerStoppingReason.NO_PERMISSION, MinerStoppingReason.CHEST_FULL, MinerStoppingReason.STRUCTURE_DESTROYED, MinerStoppingReason.PISTON_WRONG_DIRECTION, MinerStoppingReason.PISTON_NO_SPACE };

        for (int i = 0; i < reasons.length; i++) {
            Player player = server.addPlayer();
            Block b = minerBase(30 + i * 10, 30);
            MiningTask task = startMiner(player, b);
            Assertions.assertNotNull(task, "Task for reason " + reasons[i] + " was not started");

            MinerStoppingReason[] captured = { null };
            Listener watcher = new Listener() {
                @EventHandler
                public void onStop(IndustrialMinerStopEvent event) {
                    captured[0] = event.getReason();
                }
            };
            server.getPluginManager().registerEvents(watcher, plugin);

            try {
                task.stop(reasons[i]);
                Assertions.assertEquals(reasons[i], captured[0], "The reported reason must match " + reasons[i]);
            } finally {
                HandlerList.unregisterAll(watcher);
            }
        }
    }

    @Test
    @DisplayName("A plain stop() (normal completion path) fires no event")
    void testPlainStopFiresNothing() {
        Player player = server.addPlayer();
        Block b = minerBase(400, 400);
        MiningTask task = startMiner(player, b);
        Assertions.assertNotNull(task);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onStop(IndustrialMinerStopEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            task.stop();

            Assertions.assertFalse(seen[0], "A plain stop must not fire the error-stop event");
            Assertions.assertFalse(miner.activeMiners.containsKey(b.getLocation()));
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }
}
