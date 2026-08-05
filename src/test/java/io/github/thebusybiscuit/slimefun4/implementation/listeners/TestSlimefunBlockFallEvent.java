package io.github.thebusybiscuit.slimefun4.implementation.listeners;

import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
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

import io.github.thebusybiscuit.slimefun4.api.events.SlimefunBlockFallEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;
import io.github.thebusybiscuit.slimefun4.test.mocks.MockSlimefunItem;

import me.mrCookieSlime.Slimefun.api.BlockStorage;

/**
 * Regression coverage for the gravity/fall protection API expansion:
 * {@link SlimefunBlockFallEvent}, exercised through the real {@link BlockPhysicsListener}
 * fall-protection path. The falling entity is a Mockito {@link FallingBlock}.
 *
 * @author Zurker
 */
class TestSlimefunBlockFallEvent {

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
        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "fall_test");
        Slimefun.getItemCfg().setValue("TEST_FALL_BLOCK.enabled", true);
        sfItem = new MockSlimefunItem(itemGroup, new ItemStack(Material.SAND), "TEST_FALL_BLOCK");
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
        b.setType(Material.SAND);
        BlockStorage.addBlockInfo(b, "id", sfItem.getId(), true);
        return b;
    }

    /**
     * Creates a Mockito falling block whose block data reports the given material.
     */
    private FallingBlock mockFallingBlock(Material material) {
        BlockData blockData = Mockito.mock(BlockData.class);
        Mockito.when(blockData.getMaterial()).thenReturn(material);

        FallingBlock falling = Mockito.mock(FallingBlock.class);
        Mockito.when(falling.getType()).thenReturn(EntityType.FALLING_BLOCK);
        Mockito.when(falling.getDropItem()).thenReturn(true);
        Mockito.when(falling.getBlockData()).thenReturn(blockData);
        Mockito.when(falling.getWorld()).thenReturn(world);
        return falling;
    }

    /**
     * Lets the falling block try to take the given block through the real event pipeline.
     */
    private EntityChangeBlockEvent fall(FallingBlock falling, Block block) {
        EntityChangeBlockEvent event = new EntityChangeBlockEvent(falling, block, Mockito.mock(BlockData.class));
        server.getPluginManager().callEvent(event);
        return event;
    }

    @Test
    @DisplayName("SlimefunBlockFallEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Block b = world.getBlockAt(1, 1, 1);

        SlimefunBlockFallEvent event = new SlimefunBlockFallEvent(sfItem, b);

        Assertions.assertEquals(sfItem, event.getSlimefunItem());
        Assertions.assertEquals(b, event.getBlock());
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new SlimefunBlockFallEvent(null, b));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new SlimefunBlockFallEvent(sfItem, null));
    }

    @Test
    @DisplayName("A gravity Slimefun block about to fall fires the event and is kept in place")
    void testFallFiresAndCancels() {
        Block b = placeSlimefunBlock(10, 10);
        FallingBlock falling = mockFallingBlock(Material.SAND);
        Mockito.when(falling.getLocation()).thenReturn(b.getLocation());

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onFall(SlimefunBlockFallEvent event) {
                seen[0] = true;
                Assertions.assertEquals(sfItem, event.getSlimefunItem());
                Assertions.assertEquals(b, event.getBlock());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            EntityChangeBlockEvent event = fall(falling, b);

            Assertions.assertTrue(seen[0], "SlimefunBlockFallEvent was not fired");
            Assertions.assertTrue(event.isCancelled(), "The fall must have been cancelled");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling SlimefunBlockFallEvent lets the block fall")
    void testEventCancellationAllowsFall() {
        Block b = placeSlimefunBlock(20, 20);
        FallingBlock falling = mockFallingBlock(Material.SAND);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onFall(SlimefunBlockFallEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            EntityChangeBlockEvent event = fall(falling, b);

            Assertions.assertFalse(event.isCancelled(), "A vetoed protection must let the block fall");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Protection without listeners still cancels, preserving the old behavior")
    void testProtectionWithoutListenersStillCancels() {
        Block b = placeSlimefunBlock(30, 30);
        FallingBlock falling = mockFallingBlock(Material.SAND);
        Mockito.when(falling.getLocation()).thenReturn(b.getLocation());

        EntityChangeBlockEvent event = fall(falling, b);

        Assertions.assertTrue(event.isCancelled(), "The fall must have been cancelled");
    }

    @Test
    @DisplayName("A non-falling-block entity fires no event")
    void testNonFallingEntityFiresNothing() {
        Block b = placeSlimefunBlock(40, 40);
        // A FallingBlock reporting a different type is treated as a non-falling entity
        FallingBlock notFalling = Mockito.mock(FallingBlock.class);
        Mockito.when(notFalling.getType()).thenReturn(EntityType.COW);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onFall(SlimefunBlockFallEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            EntityChangeBlockEvent event = new EntityChangeBlockEvent(notFalling, b, Mockito.mock(BlockData.class));
            server.getPluginManager().callEvent(event);

            Assertions.assertFalse(seen[0], "No event must be fired for a non-falling entity");
            Assertions.assertFalse(event.isCancelled(), "A non-falling entity must be left alone");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("A vanilla block fires no event")
    void testVanillaBlockFiresNothing() {
        Block b = world.getBlockAt(50, 1, 50);
        b.setType(Material.SAND);
        FallingBlock falling = mockFallingBlock(Material.SAND);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onFall(SlimefunBlockFallEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            EntityChangeBlockEvent event = fall(falling, b);

            Assertions.assertFalse(seen[0], "No event must be fired for a vanilla block");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }
}
