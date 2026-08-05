package io.github.thebusybiscuit.slimefun4.implementation.listeners;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

import io.github.thebusybiscuit.slimefun4.api.events.SlimefunBlockBurnEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;
import io.github.thebusybiscuit.slimefun4.test.mocks.MockSlimefunItem;

import me.mrCookieSlime.Slimefun.api.BlockStorage;

/**
 * Regression coverage for the fire/burn protection API expansion:
 * {@link SlimefunBlockBurnEvent}, exercised through the real {@link BlockPhysicsListener}
 * burn-protection path.
 *
 * @author Zurker
 */
class TestSlimefunBlockBurnEvent {

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
        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "burn_test");
        Slimefun.getItemCfg().setValue("TEST_BURN_BLOCK.enabled", true);
        sfItem = new MockSlimefunItem(itemGroup, new ItemStack(Material.OAK_PLANKS), "TEST_BURN_BLOCK");
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
     * Places the test item's block backed by {@link BlockStorage}.
     */
    private Block placeBlock(int x, int z) {
        Block b = world.getBlockAt(x, 1, z);
        b.setType(Material.OAK_PLANKS);
        BlockStorage.addBlockInfo(b, "id", sfItem.getId(), true);
        return b;
    }

    /**
     * Lets fire try to burn the block through the real event pipeline.
     */
    private BlockBurnEvent burn(Block b) {
        BlockBurnEvent event = new BlockBurnEvent(b);
        server.getPluginManager().callEvent(event);
        return event;
    }

    @Test
    @DisplayName("SlimefunBlockBurnEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Block b = world.getBlockAt(1, 1, 1);

        SlimefunBlockBurnEvent event = new SlimefunBlockBurnEvent(sfItem, b);

        Assertions.assertEquals(sfItem, event.getSlimefunItem());
        Assertions.assertEquals(b, event.getBlock());
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new SlimefunBlockBurnEvent(null, b));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new SlimefunBlockBurnEvent(sfItem, null));
    }

    @Test
    @DisplayName("Fire burning a Slimefun block fires the event and is cancelled")
    void testBurnFiresAndCancels() {
        Block b = placeBlock(10, 10);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onBurn(SlimefunBlockBurnEvent event) {
                seen[0] = true;
                Assertions.assertEquals(sfItem, event.getSlimefunItem());
                Assertions.assertEquals(b, event.getBlock());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            BlockBurnEvent event = burn(b);

            Assertions.assertTrue(seen[0], "SlimefunBlockBurnEvent was not fired");
            Assertions.assertTrue(event.isCancelled(), "The burn must have been cancelled");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling SlimefunBlockBurnEvent lets the block burn")
    void testEventCancellationAllowsBurn() {
        Block b = placeBlock(20, 20);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onBurn(SlimefunBlockBurnEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            BlockBurnEvent event = burn(b);

            Assertions.assertFalse(event.isCancelled(), "A vetoed protection must let the block burn");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Burning without listeners still cancels, preserving the old behavior")
    void testBurnWithoutListenersStillCancels() {
        Block b = placeBlock(30, 30);

        BlockBurnEvent event = burn(b);

        Assertions.assertTrue(event.isCancelled(), "The burn must have been cancelled");
    }

    @Test
    @DisplayName("A vanilla block fires no event")
    void testVanillaBlockFiresNothing() {
        Block b = world.getBlockAt(40, 1, 40);
        b.setType(Material.OAK_PLANKS);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onBurn(SlimefunBlockBurnEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            BlockBurnEvent event = burn(b);

            Assertions.assertFalse(seen[0], "No event must be fired for a vanilla block");
            Assertions.assertFalse(event.isCancelled(), "A vanilla block is left to vanilla fire handling");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }
}
