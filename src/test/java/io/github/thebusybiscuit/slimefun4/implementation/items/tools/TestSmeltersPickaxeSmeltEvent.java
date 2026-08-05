package io.github.thebusybiscuit.slimefun4.implementation.items.tools;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.FurnaceRecipe;
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

import io.github.thebusybiscuit.slimefun4.api.events.SmeltersPickaxeSmeltEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Regression coverage for the smelters pickaxe API expansion: {@link SmeltersPickaxeSmeltEvent},
 * exercised by driving the real {@link SmeltersPickaxe}
 * {@link io.github.thebusybiscuit.slimefun4.core.handlers.ToolUseHandler} with a constructed
 * {@link BlockBreakEvent}.
 * <p>
 * {@code BlockMock.getDrops(ItemStack)} is unimplemented, so the mined block is a Mockito hybrid
 * (real {@link World}/{@link Location}, stubbed type and drops). The durability-damage tail may
 * be rejected by MockBukkit; a RuntimeException from it is ignored - the smelt happened beforehand.
 *
 * @author Zurker
 */
class TestSmeltersPickaxeSmeltEvent {

    private static final int FORTUNE = 3;

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static SmeltersPickaxe pickaxe;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "smelters_pickaxe_test");
        SlimefunItemStack stack = new SlimefunItemStack("_TEST_SMELTERS_PICKAXE", Material.DIAMOND_PICKAXE, "&6Test Smelters Pickaxe");
        Slimefun.getItemCfg().setValue("_TEST_SMELTERS_PICKAXE.enabled", true);
        pickaxe = new SmeltersPickaxe(itemGroup, stack, RecipeType.NULL, new ItemStack[9]);
        pickaxe.register(plugin);

        // The recipe service snapshot is only refreshed on plugin start, do it manually
        server.addRecipe(new FurnaceRecipe(new NamespacedKey(plugin, "smelters_pickaxe_test_smelt"), new ItemStack(Material.IRON_INGOT), Material.RAW_IRON, 0.7F, 200));
        Slimefun.getMinecraftRecipeService().refresh();
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
     * Mines a stubbed iron ore whose only drop is the given {@link ItemStack} via the real
     * handler, returning the (possibly mutated) drop list the handler produced.
     */
    private List<ItemStack> mine(Player player, int x, int z, ItemStack drop) {
        Location loc = new Location(world, x, 4, z);

        Block block = Mockito.mock(Block.class);
        Mockito.when(block.getType()).thenReturn(Material.IRON_ORE);
        Mockito.when(block.getLocation()).thenReturn(loc);
        Mockito.when(block.getWorld()).thenReturn(world);
        Mockito.when(block.getDrops(Mockito.any())).thenReturn(List.of(drop));

        List<ItemStack> drops = new ArrayList<>();
        BlockBreakEvent breakEvent = new BlockBreakEvent(block, player);

        try {
            pickaxe.getItemHandler().onToolUse(breakEvent, pickaxe.getItem(), FORTUNE, drops);
        } catch (RuntimeException ignored) {
            // durability-damage tail not fully supported by MockBukkit - see class javadoc
        }

        return drops;
    }

    @Test
    @DisplayName("SmeltersPickaxeSmeltEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Player player = server.addPlayer();
        Block block = world.getBlockAt(0, 4, 0);
        ItemStack drop = new ItemStack(Material.RAW_IRON);
        ItemStack output = new ItemStack(Material.IRON_INGOT);

        SmeltersPickaxeSmeltEvent event = new SmeltersPickaxeSmeltEvent(player, pickaxe, block, drop, output);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(pickaxe, event.getPickaxe());
        Assertions.assertEquals(block, event.getBlock());
        Assertions.assertEquals(drop, event.getDrop());
        Assertions.assertEquals(output, event.getOutput());
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new SmeltersPickaxeSmeltEvent(player, null, block, drop, output));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new SmeltersPickaxeSmeltEvent(player, pickaxe, null, drop, output));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new SmeltersPickaxeSmeltEvent(player, pickaxe, block, null, output));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new SmeltersPickaxeSmeltEvent(player, pickaxe, block, drop, null));
    }

    @Test
    @DisplayName("Mining an ore fires the event and smelts the raw drop")
    void testMineFiresAndSmelts() {
        Player player = server.addPlayer();
        ItemStack drop = new ItemStack(Material.RAW_IRON);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onSmelt(SmeltersPickaxeSmeltEvent event) {
                seen[0] = true;
                Assertions.assertEquals(pickaxe, event.getPickaxe());
                Assertions.assertEquals(Material.IRON_ORE, event.getBlock().getType());
                Assertions.assertSame(drop, event.getDrop());
                Assertions.assertEquals(Material.RAW_IRON, event.getDrop().getType(), "The drop must still be raw at event time");
                Assertions.assertEquals(Material.IRON_INGOT, event.getOutput().getType());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            List<ItemStack> drops = mine(player, 10, 10, drop);

            Assertions.assertTrue(seen[0], "SmeltersPickaxeSmeltEvent was not fired");
            Assertions.assertEquals(Material.IRON_INGOT, drop.getType(), "The drop must have been smelted");
            Assertions.assertEquals(FORTUNE, drop.getAmount(), "The fortune-based amount adjustment must apply");
            Assertions.assertTrue(drops.contains(drop), "The smelted drop must be added to the drops list");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling SmeltersPickaxeSmeltEvent keeps the drop raw but still applies the amount")
    void testEventCancellationKeepsRawDrop() {
        Player player = server.addPlayer();
        ItemStack drop = new ItemStack(Material.RAW_IRON);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onSmelt(SmeltersPickaxeSmeltEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            mine(player, 20, 20, drop);

            Assertions.assertEquals(Material.RAW_IRON, drop.getType(), "A cancelled smelt must keep the raw drop");
            Assertions.assertEquals(FORTUNE, drop.getAmount(), "The fortune-based amount adjustment must still apply");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Mining without listeners still smelts, preserving the old behavior")
    void testMineWithoutListenersStillSmelts() {
        Player player = server.addPlayer();
        ItemStack drop = new ItemStack(Material.RAW_IRON);

        mine(player, 30, 30, drop);

        Assertions.assertEquals(Material.IRON_INGOT, drop.getType(), "The drop must have been smelted");
        Assertions.assertEquals(FORTUNE, drop.getAmount());
    }
}
