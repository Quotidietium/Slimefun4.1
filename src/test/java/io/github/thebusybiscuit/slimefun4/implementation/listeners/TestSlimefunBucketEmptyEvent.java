package io.github.thebusybiscuit.slimefun4.implementation.listeners;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

import io.github.thebusybiscuit.slimefun4.api.events.SlimefunBucketEmptyEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;
import io.github.thebusybiscuit.slimefun4.test.mocks.MockSlimefunItem;

import me.mrCookieSlime.Slimefun.api.BlockStorage;

/**
 * Regression coverage for the bucket-empty protection API expansion:
 * {@link SlimefunBucketEmptyEvent}, exercised through the real {@link BlockPhysicsListener}
 * bucket-empty protection path.
 * <p>
 * MockBukkit's {@link PlayerBucketEmptyEvent} resolves the clicked block in its own way, so the
 * Slimefun block is registered at the exact location the handler will check - computed with the
 * same expression the handler uses ({@code getBlockClicked().getRelative(getBlockFace())}).
 *
 * @author Zurker
 */
class TestSlimefunBucketEmptyEvent {

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
        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "bucket_empty_test");
        Slimefun.getItemCfg().setValue("TEST_BUCKET_EMPTY_BLOCK.enabled", true);
        sfItem = new MockSlimefunItem(itemGroup, new ItemStack(Material.PLAYER_HEAD), "TEST_BUCKET_EMPTY_BLOCK");
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
     * Builds a bucket-empty event clicked on a block at the given slot, using the EAST face.
     */
    private PlayerBucketEmptyEvent makeEvent(Player player, int slot) {
        Block clicked = world.getBlockAt(slot, 64, slot);
        return new PlayerBucketEmptyEvent(player, clicked, BlockFace.EAST, Material.WATER_BUCKET, new ItemStack(Material.WATER_BUCKET));
    }

    /**
     * The block the handler will protect - computed the same way the handler does.
     */
    private Block placementTarget(PlayerBucketEmptyEvent event) {
        return event.getBlockClicked().getRelative(event.getBlockFace());
    }

    /**
     * Registers the Slimefun item at the given block and returns it.
     */
    private Block placeSlimefunBlock(Block target) {
        target.setType(Material.PLAYER_HEAD);
        BlockStorage.addBlockInfo(target, "id", sfItem.getId(), true);
        return target;
    }

    @Test
    @DisplayName("SlimefunBucketEmptyEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Player player = server.addPlayer();
        Block b = world.getBlockAt(1, 64, 1);

        SlimefunBucketEmptyEvent event = new SlimefunBucketEmptyEvent(player, sfItem, b);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(sfItem, event.getSlimefunItem());
        Assertions.assertEquals(b, event.getBlock());
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new SlimefunBucketEmptyEvent(player, null, b));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new SlimefunBucketEmptyEvent(player, sfItem, null));
    }

    @Test
    @DisplayName("Emptying a bucket onto a Slimefun block fires the event and is cancelled")
    void testEmptyFiresAndCancels() {
        Player player = server.addPlayer();
        PlayerBucketEmptyEvent event = makeEvent(player, 10);
        Block target = placeSlimefunBlock(placementTarget(event));

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onEmpty(SlimefunBucketEmptyEvent event) {
                seen[0] = true;
                Assertions.assertEquals(sfItem, event.getSlimefunItem());
                Assertions.assertEquals(target, event.getBlock());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            server.getPluginManager().callEvent(event);

            Assertions.assertTrue(seen[0], "SlimefunBucketEmptyEvent was not fired");
            Assertions.assertTrue(event.isCancelled(), "The bucket empty must have been cancelled");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling SlimefunBucketEmptyEvent lets the liquid be placed")
    void testEventCancellationAllowsEmpty() {
        Player player = server.addPlayer();
        PlayerBucketEmptyEvent event = makeEvent(player, 20);
        placeSlimefunBlock(placementTarget(event));

        Listener cancelling = new Listener() {
            @EventHandler
            public void onEmpty(SlimefunBucketEmptyEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            server.getPluginManager().callEvent(event);

            Assertions.assertFalse(event.isCancelled(), "A vetoed protection must let the liquid be placed");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Protection without listeners still cancels, preserving the old behavior")
    void testProtectionWithoutListenersStillCancels() {
        Player player = server.addPlayer();
        PlayerBucketEmptyEvent event = makeEvent(player, 30);
        placeSlimefunBlock(placementTarget(event));

        server.getPluginManager().callEvent(event);

        Assertions.assertTrue(event.isCancelled(), "The bucket empty must have been cancelled");
    }

    @Test
    @DisplayName("Emptying a bucket onto a vanilla block fires no event")
    void testVanillaBlockFiresNothing() {
        Player player = server.addPlayer();
        PlayerBucketEmptyEvent event = makeEvent(player, 40);
        // Leave the placement target as a vanilla block (no Slimefun data)

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onEmpty(SlimefunBucketEmptyEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            server.getPluginManager().callEvent(event);

            Assertions.assertFalse(seen[0], "No event must be fired for a vanilla block");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }
}
