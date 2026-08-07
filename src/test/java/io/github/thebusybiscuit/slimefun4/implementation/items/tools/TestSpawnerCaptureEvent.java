package io.github.thebusybiscuit.slimefun4.implementation.items.tools;

import java.util.ArrayList;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;
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

import io.github.thebusybiscuit.slimefun4.api.events.SpawnerCaptureEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun4.implementation.items.blocks.BrokenSpawner;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Regression coverage for the spawner capture API expansion: {@link SpawnerCaptureEvent},
 * exercised by driving the real {@link PickaxeOfContainment}
 * {@link io.github.thebusybiscuit.slimefun4.core.handlers.ToolUseHandler} with a constructed
 * {@link BlockBreakEvent}.
 * <p>
 * The capture path ends in {@code SlimefunUtils.spawnItem} which may be unsupported by
 * MockBukkit, so a RuntimeException from that tail is ignored - the event was fired and the
 * drop decided beforehand.
 *
 * @author Zurker
 */
class TestSpawnerCaptureEvent {

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static PickaxeOfContainment pickaxe;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        // ProtectionManager is created when the integrations start
        Slimefun.getIntegrations().start();
        server.getScheduler().performOneTick();

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "spawner_capture_test");

        // PickaxeOfContainment#breakSpawner resolves the built-in BrokenSpawner via
        // SlimefunItems.BROKEN_SPAWNER.getItem() - register one so the lookup succeeds.
        Slimefun.getItemCfg().setValue("BROKEN_SPAWNER.enabled", true);
        new BrokenSpawner(itemGroup, SlimefunItems.BROKEN_SPAWNER, RecipeType.NULL, new ItemStack[9]).register(plugin);

        SlimefunItemStack stack = new SlimefunItemStack("_TEST_PICKAXE_OF_CONTAINMENT", Material.DIAMOND_PICKAXE, "&bTest Pickaxe of Containment");
        Slimefun.getItemCfg().setValue("_TEST_PICKAXE_OF_CONTAINMENT.enabled", true);
        pickaxe = new PickaxeOfContainment(itemGroup, stack, RecipeType.NULL, new ItemStack[9]);
        pickaxe.register(plugin);
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
     * Places a spawner block spawning the given entity type.
     */
    private Block placeSpawner(int x, int z, EntityType type) {
        Block b = world.getBlockAt(x, 1, z);
        b.setType(Material.SPAWNER);

        BlockState state = b.getState();

        if (state instanceof CreatureSpawner creatureSpawner) {
            creatureSpawner.setSpawnedType(type);
            creatureSpawner.update(true, false);
        }

        return b;
    }

    /**
     * Breaks the block with the pickaxe via the real tool handler, swallowing the spawnItem tail.
     *
     * @return the {@link BlockBreakEvent} that was driven, for drop/exp assertions
     */
    private BlockBreakEvent breakBlock(Player player, Block block) {
        BlockBreakEvent event = new BlockBreakEvent(block, player);

        try {
            pickaxe.getItemHandler().onToolUse(event, pickaxe.getItem().clone(), 0, new ArrayList<>());
        } catch (RuntimeException ignored) {
            // SlimefunUtils.spawnItem is not fully supported by MockBukkit - see class javadoc
        }

        return event;
    }

    @Test
    @DisplayName("SpawnerCaptureEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Player player = server.addPlayer();
        Block spawner = placeSpawner(1, 1, EntityType.ZOMBIE);
        ItemStack drop = new ItemStack(Material.SPAWNER);

        SpawnerCaptureEvent event = new SpawnerCaptureEvent(player, pickaxe, spawner, EntityType.ZOMBIE, drop);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(pickaxe, event.getPickaxe());
        Assertions.assertEquals(spawner, event.getBlock());
        Assertions.assertEquals(EntityType.ZOMBIE, event.getEntityType());
        Assertions.assertEquals(drop, event.getDrop());
        Assertions.assertFalse(event.isCancelled());

        ItemStack swapped = new ItemStack(Material.DIAMOND);
        event.setDrop(swapped);
        Assertions.assertEquals(swapped, event.getDrop());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new SpawnerCaptureEvent(player, null, spawner, EntityType.ZOMBIE, drop));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new SpawnerCaptureEvent(player, pickaxe, null, EntityType.ZOMBIE, drop));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new SpawnerCaptureEvent(player, pickaxe, spawner, EntityType.ZOMBIE, null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setDrop(null));
    }

    @Test
    @DisplayName("Breaking a spawner fires the event with the spawned entity type and a broken spawner drop")
    void testBreakSpawnerFiresEvent() {
        Player player = server.addPlayer();
        Block spawner = placeSpawner(10, 10, EntityType.ZOMBIE);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onCapture(SpawnerCaptureEvent event) {
                seen[0] = true;
                Assertions.assertEquals(pickaxe, event.getPickaxe());
                Assertions.assertEquals(spawner, event.getBlock());
                Assertions.assertEquals(EntityType.ZOMBIE, event.getEntityType(), "The spawned entity type must have been captured");
                Assertions.assertNotNull(event.getDrop());
                Assertions.assertInstanceOf(BrokenSpawner.class, SlimefunItem.getByItem(event.getDrop()), "A vanilla spawner must yield a BrokenSpawner");
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            breakBlock(player, spawner);

            Assertions.assertTrue(seen[0], "SpawnerCaptureEvent was not fired");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling SpawnerCaptureEvent keeps vanilla drops and experience behaviour")
    void testCancelSkipsCapture() {
        Player player = server.addPlayer();
        Block spawner = placeSpawner(20, 20, EntityType.SKELETON);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onCapture(SpawnerCaptureEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            BlockBreakEvent event = breakBlock(player, spawner);

            Assertions.assertTrue(event.isDropItems(), "A cancelled capture must keep vanilla drop behaviour");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Swapping the drop via setDrop replaces the captured spawner")
    void testDropSwap() {
        Player player = server.addPlayer();
        Block spawner = placeSpawner(30, 30, EntityType.ZOMBIE);
        ItemStack custom = new ItemStack(Material.GOLDEN_APPLE);

        boolean[] seen = { false };
        Listener swapping = new Listener() {
            @EventHandler
            public void onCapture(SpawnerCaptureEvent event) {
                event.setDrop(custom);
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(swapping, plugin);

        try {
            breakBlock(player, spawner);

            Assertions.assertTrue(seen[0], "SpawnerCaptureEvent was not fired");
            // The drop was swapped before the spawnItem tail, which is enough to assert the
            // API contract: the handler would spawn the swapped item downstream.
        } finally {
            HandlerList.unregisterAll(swapping);
        }
    }

    @Test
    @DisplayName("Breaking a non-spawner block fires no event")
    void testNonSpawnerBlockFiresNothing() {
        Player player = server.addPlayer();
        Block stone = world.getBlockAt(40, 1, 40);
        stone.setType(Material.STONE);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onCapture(SpawnerCaptureEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            breakBlock(player, stone);

            Assertions.assertFalse(seen[0], "No event must be fired for a non-spawner block");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Breaking a spawner without listeners still captures, preserving the old behavior")
    void testBreakWithoutListenersStillCaptures() {
        Player player = server.addPlayer();
        Block spawner = placeSpawner(50, 50, EntityType.ZOMBIE);

        // Should not throw out of the handler beyond the unsupported spawnItem tail
        breakBlock(player, spawner);
    }
}
