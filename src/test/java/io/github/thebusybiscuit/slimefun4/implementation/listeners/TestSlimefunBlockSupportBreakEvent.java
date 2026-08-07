package io.github.thebusybiscuit.slimefun4.implementation.listeners;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

import io.github.thebusybiscuit.slimefun4.api.events.SlimefunBlockSupportBreakEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

import me.mrCookieSlime.Slimefun.api.BlockStorage;

/**
 * Regression coverage for the sensitive-block API expansion:
 * {@link SlimefunBlockSupportBreakEvent}, exercised by driving the real
 * {@link BlockListener#onBlockBreak(BlockBreakEvent)} against a vanilla support block
 * with a {@link BlockStorage}-backed pressure plate (a sensitive material) on top.
 * <p>
 * The cascade destroys the plate, so tests assert the outcome end-to-end through the
 * plate's type: a cancelled event leaves the plate floating above the broken support.
 * The asynchronous {@code BlockStorage} cleanup is not observable under MockBukkit and
 * is therefore not asserted.
 *
 * @author Zurker
 */
class TestSlimefunBlockSupportBreakEvent {

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static SlimefunItem plate;
    private static BlockListener listener;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        // Unit test startups never start the integrations, the ProtectionManager is created there
        Slimefun.getIntegrations().start();
        server.getScheduler().performOneTick();

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "support_break_test");
        SlimefunItemStack stack = new SlimefunItemStack("_TEST_SENSITIVE_PLATE", Material.STONE_PRESSURE_PLATE, "&7Test Sensitive Plate");
        Slimefun.getItemCfg().setValue("_TEST_SENSITIVE_PLATE.enabled", true);
        plate = new SlimefunItem(itemGroup, stack, RecipeType.NULL, new ItemStack[9]);
        plate.register(plugin);

        listener = new BlockListener(plugin);
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
     * Places a vanilla stone support with the sensitive test plate on top, at a high,
     * flat y to stay clear of the terrain.
     */
    private Block placeSupportWithPlate(int x, int z) {
        Block support = world.getBlockAt(x, 60, z);
        support.setType(Material.STONE);

        Block plateBlock = support.getRelative(0, 1, 0);
        plateBlock.setType(Material.STONE_PRESSURE_PLATE);
        BlockStorage.addBlockInfo(plateBlock, "id", "_TEST_SENSITIVE_PLATE");

        return support;
    }

    private void breakBlock(Player player, Block support) {
        listener.onBlockBreak(new BlockBreakEvent(support, player));
    }

    @Test
    @DisplayName("SlimefunBlockSupportBreakEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Player player = server.addPlayer();
        Block plateBlock = world.getBlockAt(1, 61, 1);
        Block support = world.getBlockAt(1, 60, 1);
        ItemStack hand = player.getInventory().getItemInMainHand();

        SlimefunBlockSupportBreakEvent event = new SlimefunBlockSupportBreakEvent(player, hand, plateBlock, support, plate);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(hand, event.getItem());
        Assertions.assertEquals(plateBlock, event.getBlock());
        Assertions.assertEquals(support, event.getSupportingBlock());
        Assertions.assertEquals(plate, event.getSlimefunItem());
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new SlimefunBlockSupportBreakEvent(player, null, plateBlock, support, plate));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new SlimefunBlockSupportBreakEvent(player, hand, null, support, plate));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new SlimefunBlockSupportBreakEvent(player, hand, plateBlock, null, plate));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new SlimefunBlockSupportBreakEvent(player, hand, plateBlock, support, null));
    }

    @Test
    @DisplayName("Breaking the support fires the event and destroys the sensitive block above")
    void testCascadeFiresEventAndDestroys() {
        Player player = server.addPlayer();
        Block support = placeSupportWithPlate(100, 100);
        Block plateBlock = support.getRelative(0, 1, 0);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onSupportBreak(SlimefunBlockSupportBreakEvent event) {
                seen[0] = true;
                Assertions.assertEquals(player, event.getPlayer());
                Assertions.assertEquals(plateBlock, event.getBlock());
                Assertions.assertEquals(support, event.getSupportingBlock());
                Assertions.assertEquals(plate, event.getSlimefunItem());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            breakBlock(player, support);

            Assertions.assertTrue(seen[0], "SlimefunBlockSupportBreakEvent was not fired");
            Assertions.assertEquals(Material.AIR, plateBlock.getType(), "The sensitive block must have been destroyed");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling SlimefunBlockSupportBreakEvent protects the sensitive block")
    void testCancelProtectsBlock() {
        Player player = server.addPlayer();
        Block support = placeSupportWithPlate(200, 200);
        Block plateBlock = support.getRelative(0, 1, 0);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onSupportBreak(SlimefunBlockSupportBreakEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            breakBlock(player, support);

            Assertions.assertEquals(Material.STONE_PRESSURE_PLATE, plateBlock.getType(), "A vetoed cascade must leave the block floating in place");
            Assertions.assertEquals("_TEST_SENSITIVE_PLATE", BlockStorage.getLocationInfo(plateBlock.getLocation(), "id"), "A vetoed cascade must keep the BlockStorage data");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Breaking the support without listeners still destroys the sensitive block, preserving the old behavior")
    void testCascadeWithoutListenersDestroys() {
        Player player = server.addPlayer();
        Block support = placeSupportWithPlate(300, 300);
        Block plateBlock = support.getRelative(0, 1, 0);

        breakBlock(player, support);

        Assertions.assertEquals(Material.AIR, plateBlock.getType(), "The sensitive block must have been destroyed");
    }

    @Test
    @DisplayName("A non-sensitive Slimefun block above fires no event and stays untouched")
    void testNonSensitiveBlockAboveFiresNoEvent() {
        Player player = server.addPlayer();
        Block support = world.getBlockAt(400, 60, 400);
        support.setType(Material.STONE);

        Block above = support.getRelative(0, 1, 0);
        above.setType(Material.QUARTZ_BLOCK);
        BlockStorage.addBlockInfo(above, "id", "_TEST_SENSITIVE_PLATE");

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onSupportBreak(SlimefunBlockSupportBreakEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            breakBlock(player, support);

            Assertions.assertFalse(seen[0], "No event must be fired for a non-sensitive material");
            Assertions.assertEquals(Material.QUARTZ_BLOCK, above.getType(), "The non-sensitive block must have stayed untouched");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("A vanilla sensitive block above fires no event and is left to vanilla behavior")
    void testVanillaSensitiveBlockAboveFiresNoEvent() {
        Player player = server.addPlayer();
        Block support = world.getBlockAt(500, 60, 500);
        support.setType(Material.STONE);
        support.getRelative(0, 1, 0).setType(Material.STONE_PRESSURE_PLATE);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onSupportBreak(SlimefunBlockSupportBreakEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            breakBlock(player, support);

            Assertions.assertFalse(seen[0], "No event must be fired without a Slimefun item");
            Assertions.assertEquals(Material.STONE_PRESSURE_PLATE, support.getRelative(0, 1, 0).getType(), "The vanilla plate must have been left alone");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }
}
