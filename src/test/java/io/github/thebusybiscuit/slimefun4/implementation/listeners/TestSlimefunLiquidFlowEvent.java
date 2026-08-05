package io.github.thebusybiscuit.slimefun4.implementation.listeners;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

import io.github.thebusybiscuit.slimefun4.api.events.SlimefunLiquidFlowEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;
import io.github.thebusybiscuit.slimefun4.test.mocks.MockSlimefunItem;

import me.mrCookieSlime.Slimefun.api.BlockStorage;

/**
 * Regression coverage for the liquid flow protection API expansion:
 * {@link SlimefunLiquidFlowEvent}, exercised through the real {@link BlockPhysicsListener}
 * liquid-flow protection path. Uses a head-based Slimefun block, which is fluid-sensitive.
 *
 * @author Zurker
 */
class TestSlimefunLiquidFlowEvent {

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

        // Non-configurable items stay DISABLED unless Items.yml says otherwise, enable it first.
        // Player heads are fluid-sensitive, so a head-based Slimefun block is protected.
        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "liquid_flow_test");
        Slimefun.getItemCfg().setValue("TEST_LIQUID_FLOW_BLOCK.enabled", true);
        sfItem = new MockSlimefunItem(itemGroup, new ItemStack(Material.PLAYER_HEAD), "TEST_LIQUID_FLOW_BLOCK");
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
     * Places the test item's block (a head) backed by {@link BlockStorage} and returns it.
     */
    private Block placeSlimefunBlock(int x, int z) {
        Block b = world.getBlockAt(x, 1, z);
        b.setType(Material.PLAYER_HEAD);
        BlockStorage.addBlockInfo(b, "id", sfItem.getId(), true);
        return b;
    }

    /**
     * Lets liquid flow from the source into the given block through the real event pipeline.
     */
    private BlockFromToEvent flowInto(Block to) {
        Block source = world.getBlockAt(to.getX() + 1, 1, to.getZ());
        source.setType(Material.WATER);
        BlockFromToEvent event = new BlockFromToEvent(source, to);
        server.getPluginManager().callEvent(event);
        return event;
    }

    @Test
    @DisplayName("SlimefunLiquidFlowEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Block b = world.getBlockAt(1, 1, 1);

        SlimefunLiquidFlowEvent event = new SlimefunLiquidFlowEvent(sfItem, b);

        Assertions.assertEquals(sfItem, event.getSlimefunItem());
        Assertions.assertEquals(b, event.getBlock());
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new SlimefunLiquidFlowEvent(null, b));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new SlimefunLiquidFlowEvent(sfItem, null));
    }

    @Test
    @DisplayName("A liquid flowing into a fluid-sensitive Slimefun block fires the event and is cancelled")
    void testFlowFiresAndCancels() {
        Block b = placeSlimefunBlock(10, 10);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onFlow(SlimefunLiquidFlowEvent event) {
                seen[0] = true;
                Assertions.assertEquals(sfItem, event.getSlimefunItem());
                Assertions.assertEquals(b, event.getBlock());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            BlockFromToEvent event = flowInto(b);

            Assertions.assertTrue(seen[0], "SlimefunLiquidFlowEvent was not fired");
            Assertions.assertTrue(event.isCancelled(), "The liquid flow must have been cancelled");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling SlimefunLiquidFlowEvent lets the liquid flow in")
    void testEventCancellationAllowsFlow() {
        Block b = placeSlimefunBlock(20, 20);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onFlow(SlimefunLiquidFlowEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            BlockFromToEvent event = flowInto(b);

            Assertions.assertFalse(event.isCancelled(), "A vetoed protection must let the liquid flow in");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Protection without listeners still cancels, preserving the old behavior")
    void testProtectionWithoutListenersStillCancels() {
        Block b = placeSlimefunBlock(30, 30);

        BlockFromToEvent event = flowInto(b);

        Assertions.assertTrue(event.isCancelled(), "The liquid flow must have been cancelled");
    }

    @Test
    @DisplayName("A liquid flowing into a non-fluid-sensitive Slimefun block fires no event")
    void testNonFluidSensitiveFiresNothing() {
        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "liquid_flow_test_other");
        Slimefun.getItemCfg().setValue("TEST_LIQUID_DISPENSER.enabled", true);
        MockSlimefunItem dispenserItem = new MockSlimefunItem(itemGroup, new ItemStack(Material.DISPENSER), "TEST_LIQUID_DISPENSER");
        dispenserItem.register(plugin);

        Block b = world.getBlockAt(40, 1, 40);
        b.setType(Material.DISPENSER);
        BlockStorage.addBlockInfo(b, "id", dispenserItem.getId(), true);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onFlow(SlimefunLiquidFlowEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            BlockFromToEvent event = flowInto(b);

            Assertions.assertFalse(seen[0], "No event must be fired for a non-fluid-sensitive block");
            Assertions.assertFalse(event.isCancelled(), "A non-fluid-sensitive block must be left alone");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }
}
