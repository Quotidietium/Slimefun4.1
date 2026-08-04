package io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines.entities;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.IronGolem;
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

import io.github.thebusybiscuit.slimefun4.api.events.AsyncEntityAssembleEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

import me.mrCookieSlime.CSCoreLibPlugin.Configuration.Config;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;

/**
 * Regression coverage for the entity assembler API expansion:
 * {@link AsyncEntityAssembleEvent}, exercised through the real
 * {@link AbstractEntityAssembler} ticker with an {@link IronGolemAssembler}.
 *
 * @author Zurker
 */
class TestAsyncEntityAssembleEvent {

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static IronGolemAssembler assembler;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        // Unit test startups never start the integrations, the ProtectionManager is created there
        Slimefun.getIntegrations().start();
        server.getScheduler().performOneTick();

        // A BlockTicker item stays DISABLED while tickers are off and non-configurable items
        // stay DISABLED unless Items.yml says otherwise, so enable both before registering.
        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "entity_assembler_test");
        SlimefunItemStack stack = new SlimefunItemStack("TEST_IRON_GOLEM_ASSEMBLER", Material.DISPENSER, "&fTest Iron Golem Assembler");
        Slimefun.getCfg().setValue("URID.enable-tickers", true);
        Slimefun.getItemCfg().setValue("TEST_IRON_GOLEM_ASSEMBLER.enabled", true);
        assembler = new IronGolemAssembler(itemGroup, stack, RecipeType.NULL, new ItemStack[9]);
        assembler.register(plugin);
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
     * Builds a charged assembler stocked with a pumpkin and four iron blocks. The
     * "offset" info is set explicitly: the ticker parses it and a missing value would
     * abort the assembly after the event was fired.
     */
    private Block setupAssembler(int x, int z) {
        Block b = world.getBlockAt(x, 1, z);
        b.setType(Material.DISPENSER);
        BlockStorage.addBlockInfo(b, "id", assembler.getId(), true);
        BlockStorage.addBlockInfo(b.getLocation(), "energy-charge", "4096", false);
        BlockStorage.addBlockInfo(b.getLocation(), "offset", "3.0", false);

        BlockMenu menu = BlockStorage.getInventory(b);
        menu.replaceExistingItem(19, new ItemStack(Material.CARVED_PUMPKIN));
        menu.replaceExistingItem(25, new ItemStack(Material.IRON_BLOCK, 4));
        return b;
    }

    /**
     * Ticks the assembler once on a worker thread: the event is asynchronous and the
     * plugin manager refuses async events on the main thread - in production this ticker
     * always runs asynchronously, so this mirrors reality. MockBukkit does not fully
     * support the sound and block effect played after the entity was spawned, so a
     * RuntimeException from that tail is ignored (the event was fired and the resources
     * consumed beforehand). Any Error, e.g. a failing assertion inside an event
     * listener, is rethrown as a test failure.
     */
    private void tick(Block b) {
        Throwable[] error = new Throwable[1];
        Thread thread = new Thread(() -> {
            try {
                Config data = BlockStorage.getLocationInfo(b.getLocation());
                assembler.getItemHandler().tick(b, assembler, data);
            } catch (RuntimeException ignored) {
                // See the javadoc above
            } catch (Throwable t) {
                error[0] = t;
            }
        });
        thread.start();

        try {
            thread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Assertions.fail("Interrupted while ticking the assembler");
        }

        if (error[0] != null) {
            Assertions.fail("The assembler tick died unexpectedly", error[0]);
        }
    }

    private long golemCount() {
        return world.getEntities().stream().filter(e -> e instanceof IronGolem).count();
    }

    @Test
    @DisplayName("AsyncEntityAssembleEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Block b = world.getBlockAt(1, 1, 1);

        AsyncEntityAssembleEvent event = new AsyncEntityAssembleEvent(assembler, b);

        Assertions.assertEquals(assembler, event.getAssembler());
        Assertions.assertEquals(b, event.getBlock());
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new AsyncEntityAssembleEvent(null, b));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new AsyncEntityAssembleEvent(assembler, null));
    }

    @Test
    @DisplayName("Assembling fires the event and consumes the resources")
    void testAssembleFiresAndConsumes() {
        Block b = setupAssembler(10, 10);
        BlockMenu menu = BlockStorage.getInventory(b);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onAssemble(AsyncEntityAssembleEvent event) {
                seen[0] = true;
                Assertions.assertEquals(assembler, event.getAssembler());
                Assertions.assertEquals(b, event.getBlock());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            tick(b);

            Assertions.assertTrue(seen[0], "AsyncEntityAssembleEvent was not fired");
            ItemStack pumpkin = menu.getItemInSlot(19);
            ItemStack iron = menu.getItemInSlot(25);
            Assertions.assertTrue(pumpkin == null || pumpkin.getAmount() == 0, "The pumpkin must have been consumed");
            Assertions.assertTrue(iron == null || iron.getAmount() == 0, "The iron blocks must have been consumed");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling AsyncEntityAssembleEvent keeps the resources and spawns nothing")
    void testEventCancellationSkipsAssembly() {
        Block b = setupAssembler(20, 20);
        BlockMenu menu = BlockStorage.getInventory(b);
        long golemsBefore = golemCount();

        Listener cancelling = new Listener() {
            @EventHandler
            public void onAssemble(AsyncEntityAssembleEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            tick(b);

            Assertions.assertEquals(Material.CARVED_PUMPKIN, menu.getItemInSlot(19).getType(), "A cancelled assembly must keep the pumpkin");
            Assertions.assertEquals(4, menu.getItemInSlot(25).getAmount(), "A cancelled assembly must keep the iron blocks");
            Assertions.assertEquals(golemsBefore, golemCount(), "A cancelled assembly must not spawn a golem");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Assembly without listeners still consumes, preserving the old behavior")
    void testAssembleWithoutListenersStillConsumes() {
        Block b = setupAssembler(30, 30);
        BlockMenu menu = BlockStorage.getInventory(b);

        tick(b);

        ItemStack pumpkin = menu.getItemInSlot(19);
        Assertions.assertTrue(pumpkin == null || pumpkin.getAmount() == 0, "The pumpkin must have been consumed");
    }

    @Test
    @DisplayName("An assembler without all resources fires no event")
    void testMissingBodyFiresNothing() {
        Block b = world.getBlockAt(40, 1, 40);
        b.setType(Material.DISPENSER);
        BlockStorage.addBlockInfo(b, "id", assembler.getId(), true);
        BlockStorage.addBlockInfo(b.getLocation(), "energy-charge", "4096", false);
        BlockStorage.addBlockInfo(b.getLocation(), "offset", "3.0", false);

        BlockMenu menu = BlockStorage.getInventory(b);
        menu.replaceExistingItem(19, new ItemStack(Material.CARVED_PUMPKIN));

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onAssemble(AsyncEntityAssembleEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            tick(b);

            Assertions.assertFalse(seen[0], "No event must be fired without all resources");
            Assertions.assertEquals(Material.CARVED_PUMPKIN, menu.getItemInSlot(19).getType(), "The pumpkin must have been kept");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("An assembler without energy fires no event")
    void testNoEnergyFiresNothing() {
        Block b = setupAssembler(50, 50);
        BlockStorage.addBlockInfo(b.getLocation(), "energy-charge", "0", true);
        BlockMenu menu = BlockStorage.getInventory(b);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onAssemble(AsyncEntityAssembleEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            tick(b);

            Assertions.assertFalse(seen[0], "No event must be fired without energy");
            Assertions.assertEquals(Material.CARVED_PUMPKIN, menu.getItemInSlot(19).getType(), "The pumpkin must have been kept");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }
}
