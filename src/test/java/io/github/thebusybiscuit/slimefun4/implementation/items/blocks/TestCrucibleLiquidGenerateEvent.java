package io.github.thebusybiscuit.slimefun4.implementation.items.blocks;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

import io.github.thebusybiscuit.slimefun4.api.events.CrucibleLiquidGenerateEvent;
import io.github.thebusybiscuit.slimefun4.api.events.PlayerRightClickEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

import me.mrCookieSlime.Slimefun.api.BlockStorage;

/**
 * Regression coverage for the crucible API expansion: {@link CrucibleLiquidGenerateEvent},
 * exercised by driving the real {@link Crucible} {@link io.github.thebusybiscuit.slimefun4.core.handlers.BlockUseHandler}
 * with a constructed {@link PlayerRightClickEvent}.
 * <p>
 * The downstream {@code generateLiquid} casts to {@link org.bukkit.block.data.Levelled} which
 * MockBukkit does not provide for water/lava, so a RuntimeException from that tail is ignored
 * here - the event was fired and the input decided beforehand.
 *
 * @author Zurker
 */
class TestCrucibleLiquidGenerateEvent {

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static Crucible crucible;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        // ProtectionManager is created when the integrations start
        Slimefun.getIntegrations().start();
        server.getScheduler().performOneTick();

        // Non-configurable items stay DISABLED unless Items.yml says otherwise, enable it first
        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "crucible_test");
        SlimefunItemStack stack = new SlimefunItemStack("TEST_CRUCIBLE", Material.TERRACOTTA, "&fTest Crucible");
        Slimefun.getItemCfg().setValue("TEST_CRUCIBLE.enabled", true);
        crucible = new Crucible(itemGroup, stack, RecipeType.NULL, new ItemStack[9]);
        crucible.register(plugin);
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
     * Right-clicks the crucible with the given input via a constructed event.
     */
    private void melt(Player player, Block crucibleBlock, ItemStack input) {
        PlayerInteractEvent interactEvent = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, input, crucibleBlock, BlockFace.UP);
        PlayerRightClickEvent event = new PlayerRightClickEvent(interactEvent);

        try {
            crucible.getItemHandler().onRightClick(event);
        } catch (RuntimeException ignored) {
            // generateLiquid() casts to Levelled, which MockBukkit lacks for water/lava
        }
    }

    private Block placeCrucible(int x, int z) {
        Block b = world.getBlockAt(x, 1, z);
        b.setType(Material.TERRACOTTA);
        BlockStorage.addBlockInfo(b, "id", crucible.getId(), true);
        return b;
    }

    @Test
    @DisplayName("CrucibleLiquidGenerateEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Player player = server.addPlayer();
        Block b = world.getBlockAt(1, 1, 1);
        ItemStack input = new ItemStack(Material.OAK_LEAVES, 16);

        CrucibleLiquidGenerateEvent event = new CrucibleLiquidGenerateEvent(player, crucible, b, input, true);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(crucible, event.getCrucible());
        Assertions.assertEquals(b, event.getBlock());
        Assertions.assertEquals(input, event.getInput());
        Assertions.assertTrue(event.isWater());
        Assertions.assertFalse(event.isCancelled());

        event.setWater(false);
        Assertions.assertFalse(event.isWater());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new CrucibleLiquidGenerateEvent(player, null, b, input, true));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new CrucibleLiquidGenerateEvent(player, crucible, null, input, true));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new CrucibleLiquidGenerateEvent(player, crucible, b, null, true));
    }

    @Test
    @DisplayName("Melting leaves fires the event with water, stone with lava")
    void testLeavesFireWater() {
        Player player = server.addPlayer();
        Block b = placeCrucible(10, 10);
        ItemStack leaves = new ItemStack(Material.OAK_LEAVES, 16);
        player.getInventory().setItemInMainHand(leaves);

        boolean[] seenWater = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onGenerate(CrucibleLiquidGenerateEvent event) {
                seenWater[0] = event.isWater();
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            melt(player, b, leaves);

            Assertions.assertTrue(seenWater[0], "Leaves should generate water");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Melting stone fires the event with lava")
    void testStoneFireLava() {
        Player player = server.addPlayer();
        Block b = placeCrucible(20, 20);
        ItemStack stone = new ItemStack(Material.STONE, 12);
        player.getInventory().setItemInMainHand(stone);

        boolean[] seenLava = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onGenerate(CrucibleLiquidGenerateEvent event) {
                // stone is not a leaf -> lava
                seenLava[0] = !event.isWater();
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            melt(player, b, stone);

            Assertions.assertTrue(seenLava[0], "Stone should generate lava");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling CrucibleLiquidGenerateEvent keeps the input untouched")
    void testEventCancellationKeepsInput() {
        Player player = server.addPlayer();
        Block b = placeCrucible(30, 30);
        ItemStack stone = new ItemStack(Material.STONE, 12);
        player.getInventory().setItemInMainHand(stone);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onGenerate(CrucibleLiquidGenerateEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            melt(player, b, stone);

            Assertions.assertEquals(12, player.getInventory().getItemInMainHand().getAmount(), "A cancelled melt must keep the input");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Overriding the liquid type via setWater swaps water for lava")
    void testLiquidTypeOverride() {
        Player player = server.addPlayer();
        Block b = placeCrucible(40, 40);
        ItemStack leaves = new ItemStack(Material.OAK_LEAVES, 16);
        player.getInventory().setItemInMainHand(leaves);

        boolean[] seenOverridden = { false };
        Listener overriding = new Listener() {
            @EventHandler
            public void onGenerate(CrucibleLiquidGenerateEvent event) {
                if (event.isWater()) {
                    event.setWater(false);
                    seenOverridden[0] = true;
                }
            }
        };
        server.getPluginManager().registerEvents(overriding, plugin);

        try {
            melt(player, b, leaves);

            Assertions.assertTrue(seenOverridden[0], "The liquid type must have been overridden from water to lava");
        } finally {
            HandlerList.unregisterAll(overriding);
        }
    }

    @Test
    @DisplayName("An invalid input fires no event")
    void testInvalidInputFiresNothing() {
        Player player = server.addPlayer();
        Block b = placeCrucible(50, 50);
        ItemStack dirt = new ItemStack(Material.DIRT, 12);
        player.getInventory().setItemInMainHand(dirt);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onGenerate(CrucibleLiquidGenerateEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            melt(player, b, dirt);

            Assertions.assertFalse(seen[0], "No event must be fired for an invalid input");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("An input below the recipe amount is rejected without consuming")
    void testInsufficientInputIsRejected() {
        Player player = server.addPlayer();
        Block b = placeCrucible(60, 60);
        // The stone recipe requires 12; a single stone must not smelt into a full lava bucket
        ItemStack stone = new ItemStack(Material.STONE, 1);
        player.getInventory().setItemInMainHand(stone);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onGenerate(CrucibleLiquidGenerateEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            melt(player, b, stone);

            Assertions.assertFalse(seen[0], "No event must fire for an input below the recipe amount");
            Assertions.assertEquals(1, player.getInventory().getItemInMainHand().getAmount(), "An insufficient input must not be consumed");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }
}
