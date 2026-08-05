package io.github.thebusybiscuit.slimefun4.implementation.listeners;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.TreeType;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.world.StructureGrowEvent;
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

import io.github.thebusybiscuit.slimefun4.api.events.SlimefunStructureGrowEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;
import io.github.thebusybiscuit.slimefun4.test.mocks.MockSlimefunItem;

import me.mrCookieSlime.Slimefun.api.BlockStorage;

/**
 * Regression coverage for the structure-grow protection API expansion:
 * {@link SlimefunStructureGrowEvent}, exercised through the real {@link BlockPhysicsListener}
 * structure-grow protection path.
 *
 * @author Zurker
 */
class TestSlimefunStructureGrowEvent {

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static MockSlimefunItem sfItem;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        // Unit test startups do not register the listeners, register it manually
        new BlockPhysicsListener(plugin);

        // Non-configurable items stay DISABLED unless Items.yml says otherwise, enable it first
        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "structure_grow_test");
        Slimefun.getItemCfg().setValue("TEST_STRUCTURE_GROW_BLOCK.enabled", true);
        sfItem = new MockSlimefunItem(itemGroup, new ItemStack(Material.OAK_LOG), "TEST_STRUCTURE_GROW_BLOCK");
        sfItem.register(plugin);
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
     * Places the test item's block backed by {@link BlockStorage} and returns it.
     */
    private Block placeSlimefunBlock(int x, int z) {
        Block b = world.getBlockAt(x, 1, z);
        b.setType(Material.OAK_LOG);
        BlockStorage.addBlockInfo(b, "id", sfItem.getId(), true);
        return b;
    }

    /**
     * Builds a structure-grow event whose block list contains the given block states.
     */
    private StructureGrowEvent grow(List<BlockState> blocks) {
        return new StructureGrowEvent(new org.bukkit.Location(world, 0, 0, 0), TreeType.TREE, false, null, blocks);
    }

    /**
     * Creates a Mockito block state reporting the given location.
     */
    private BlockState stateAt(Block b) {
        BlockState state = Mockito.mock(BlockState.class);
        Mockito.when(state.getLocation()).thenReturn(b.getLocation());
        return state;
    }

    @Test
    @DisplayName("SlimefunStructureGrowEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Block b = world.getBlockAt(1, 1, 1);

        SlimefunStructureGrowEvent event = new SlimefunStructureGrowEvent(sfItem, b);

        Assertions.assertEquals(sfItem, event.getSlimefunItem());
        Assertions.assertEquals(b, event.getBlock());
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new SlimefunStructureGrowEvent(null, b));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new SlimefunStructureGrowEvent(sfItem, null));
    }

    @Test
    @DisplayName("A tree growing into a Slimefun block fires the event and skips that block")
    void testGrowFiresAndSkips() {
        Block sfBlock = placeSlimefunBlock(10, 10);
        Block vanilla = world.getBlockAt(11, 1, 10);
        vanilla.setType(Material.OAK_LOG);

        List<BlockState> blocks = new ArrayList<>(List.of(stateAt(sfBlock), stateAt(vanilla)));

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onGrow(SlimefunStructureGrowEvent event) {
                seen[0] = true;
                Assertions.assertEquals(sfItem, event.getSlimefunItem());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            StructureGrowEvent event = grow(blocks);
            server.getPluginManager().callEvent(event);

            Assertions.assertTrue(seen[0], "SlimefunStructureGrowEvent was not fired");
            Assertions.assertEquals(1, event.getBlocks().size(), "The Slimefun block must have been skipped");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling SlimefunStructureGrowEvent lets the structure overwrite the block")
    void testEventCancellationKeepsBlock() {
        Block sfBlock = placeSlimefunBlock(20, 20);

        List<BlockState> blocks = new ArrayList<>(List.of(stateAt(sfBlock)));

        Listener cancelling = new Listener() {
            @EventHandler
            public void onGrow(SlimefunStructureGrowEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            StructureGrowEvent event = grow(blocks);
            server.getPluginManager().callEvent(event);

            Assertions.assertEquals(1, event.getBlocks().size(), "A vetoed protection must keep the block in the structure");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Protection without listeners still skips the block, preserving the old behavior")
    void testProtectionWithoutListenersStillSkips() {
        Block sfBlock = placeSlimefunBlock(30, 30);
        Block vanilla = world.getBlockAt(31, 1, 30);
        vanilla.setType(Material.OAK_LOG);

        List<BlockState> blocks = new ArrayList<>(List.of(stateAt(sfBlock), stateAt(vanilla)));

        StructureGrowEvent event = grow(blocks);
        server.getPluginManager().callEvent(event);

        Assertions.assertEquals(1, event.getBlocks().size(), "The Slimefun block must have been skipped");
    }

    @Test
    @DisplayName("A structure growing only over vanilla blocks fires no event")
    void testVanillaBlocksFireNothing() {
        Block vanilla = world.getBlockAt(40, 1, 40);
        vanilla.setType(Material.OAK_LOG);

        List<BlockState> blocks = new ArrayList<>(List.of(stateAt(vanilla)));

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onGrow(SlimefunStructureGrowEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            StructureGrowEvent event = grow(blocks);
            server.getPluginManager().callEvent(event);

            Assertions.assertFalse(seen[0], "No event must be fired for vanilla blocks");
            Assertions.assertEquals(1, event.getBlocks().size(), "The vanilla block must not have been skipped");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }
}
