package io.github.thebusybiscuit.slimefun4.implementation.items.multiblocks.miner;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
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

import io.github.thebusybiscuit.slimefun4.api.events.IndustrialMinerStartEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Regression coverage for the industrial miner API expansion:
 * {@link IndustrialMinerStartEvent}, exercised by driving the real
 * {@link IndustrialMiner#onInteract(Player, Block)} start path.
 * <p>
 * Starting registers a {@link MiningTask} in {@code activeMiners} synchronously (the
 * warm-up animation itself is only queued), so tests assert the outcome end-to-end:
 * a cancelled event leaves the miner idle. The queued scheduler chain never advances
 * because the tests never tick the scheduler.
 *
 * @author Zurker
 */
class TestIndustrialMinerStartEvent {

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static IndustrialMiner miner;
    private static AdvancedIndustrialMiner advancedMiner;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "industrial_miner_test");

        SlimefunItemStack stack = new SlimefunItemStack("_TEST_INDUSTRIAL_MINER", Material.BLAST_FURNACE, "&7Test Industrial Miner");
        Slimefun.getItemCfg().setValue("_TEST_INDUSTRIAL_MINER.enabled", true);
        miner = new IndustrialMiner(itemGroup, stack, Material.IRON_BLOCK, false, 3);
        miner.register(plugin);

        SlimefunItemStack advancedStack = new SlimefunItemStack("_TEST_ADVANCED_INDUSTRIAL_MINER", Material.BLAST_FURNACE, "&7Test Advanced Industrial Miner");
        Slimefun.getItemCfg().setValue("_TEST_ADVANCED_INDUSTRIAL_MINER.enabled", true);
        advancedMiner = new AdvancedIndustrialMiner(itemGroup, advancedStack);
        advancedMiner.register(plugin);
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
     * The base block of a miner multiblock at a high, flat y to stay clear of the terrain.
     */
    private Block minerBase(int x, int z) {
        Block b = world.getBlockAt(x, 60, z);
        b.setType(Material.BLAST_FURNACE);
        return b;
    }

    @Test
    @DisplayName("IndustrialMinerStartEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Player player = server.addPlayer();
        Block b = minerBase(1, 1);

        IndustrialMinerStartEvent event = new IndustrialMinerStartEvent(player, miner, b);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(miner, event.getMiner());
        Assertions.assertEquals(b, event.getBlock());
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new IndustrialMinerStartEvent(player, null, b));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new IndustrialMinerStartEvent(player, miner, null));
    }

    @Test
    @DisplayName("Starting a miner fires the event and registers the mining task")
    void testStartFiresEventAndStarts() {
        Player player = server.addPlayer();
        Block b = minerBase(100, 100);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onStart(IndustrialMinerStartEvent event) {
                seen[0] = true;
                Assertions.assertEquals(player, event.getPlayer());
                Assertions.assertEquals(miner, event.getMiner());
                Assertions.assertEquals(b, event.getBlock());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            miner.onInteract(player, b);

            Assertions.assertTrue(seen[0], "IndustrialMinerStartEvent was not fired");
            Assertions.assertTrue(miner.activeMiners.containsKey(b.getLocation()), "The mining task must have been registered");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling IndustrialMinerStartEvent keeps the miner idle")
    void testCancelPreventsStart() {
        Player player = server.addPlayer();
        Block b = minerBase(200, 200);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onStart(IndustrialMinerStartEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            miner.onInteract(player, b);

            Assertions.assertFalse(miner.activeMiners.containsKey(b.getLocation()), "A vetoed start must not register a mining task");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Starting without listeners still starts, preserving the old behavior")
    void testStartWithoutListenersStarts() {
        Player player = server.addPlayer();
        Block b = minerBase(300, 300);

        miner.onInteract(player, b);

        Assertions.assertTrue(miner.activeMiners.containsKey(b.getLocation()), "The mining task must have been registered");
    }

    @Test
    @DisplayName("Interacting with an already-running miner fires no event")
    void testAlreadyRunningFiresNothing() {
        Player player = server.addPlayer();
        Block b = minerBase(400, 400);

        // Start the miner first, without any watcher.
        miner.onInteract(player, b);
        Assertions.assertTrue(miner.activeMiners.containsKey(b.getLocation()));

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onStart(IndustrialMinerStartEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            miner.onInteract(player, b);

            Assertions.assertFalse(seen[0], "No event must be fired when the miner is already running");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Starting an AdvancedIndustrialMiner fires the event with the advanced machine")
    void testAdvancedMinerFiresEvent() {
        Player player = server.addPlayer();
        Block b = minerBase(500, 500);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onStart(IndustrialMinerStartEvent event) {
                seen[0] = true;
                Assertions.assertEquals(advancedMiner, event.getMiner());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            advancedMiner.onInteract(player, b);

            Assertions.assertTrue(seen[0], "IndustrialMinerStartEvent was not fired for the advanced miner");
            Assertions.assertTrue(advancedMiner.activeMiners.containsKey(b.getLocation()), "The mining task must have been registered");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }
}
