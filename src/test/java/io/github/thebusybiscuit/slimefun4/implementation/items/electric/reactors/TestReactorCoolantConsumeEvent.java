package io.github.thebusybiscuit.slimefun4.implementation.items.electric.reactors;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
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

import io.github.thebusybiscuit.slimefun4.api.events.ReactorCoolantConsumeEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun4.implementation.operations.FuelOperation;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

import me.mrCookieSlime.CSCoreLibPlugin.Configuration.Config;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;

/**
 * Regression coverage for the reactor coolant API expansion:
 * {@link ReactorCoolantConsumeEvent}, exercised by driving a {@link NuclearReactor}
 * through {@link Reactor#getGeneratedOutput(Location, Config)} with a fuel operation
 * whose progress lands exactly on a cooling checkpoint ({@code progress % 50 == 0})
 * and a coolant cell in its coolant slot.
 * <p>
 * The success path ends in a hologram update which is only partially supported by
 * MockBukkit, so the drive helper swallows a RuntimeException from that tail - the
 * event was fired and the coolant cell was consumed beforehand. A vetoed or
 * coolant-less reactor returns zero from that tick without reaching the hologram,
 * so those paths assert the return value as well.
 *
 * @author Zurker
 */
class TestReactorCoolantConsumeEvent {

    private static final int COOLANT_SLOT = 25;
    private static final int COOLANT_DURATION = 50;

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static NuclearReactor reactor;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        // Unit test startups never start the integrations, the ProtectionManager is created there
        Slimefun.getIntegrations().start();
        server.getScheduler().performOneTick();

        // Non-configurable items stay DISABLED unless Items.yml says otherwise, enable it first
        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "reactor_coolant_test");
        SlimefunItemStack stack = new SlimefunItemStack("TEST_COOLANT_REACTOR", Material.DISPENSER, "&fTest Coolant Reactor");
        Slimefun.getItemCfg().setValue("TEST_COOLANT_REACTOR.enabled", true);
        reactor = new NuclearReactor(itemGroup, stack, RecipeType.NULL, new ItemStack[9]) {
            @Override
            public int getEnergyProduction() {
                return 100;
            }

            @Override
            public int getCapacity() {
                return 512;
            }
        };
        reactor.register(plugin);
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
     * Places the reactor as a real block backed by {@link BlockStorage} and returns its menu.
     */
    private BlockMenu placeReactor(int x, int z) {
        Block b = world.getBlockAt(x, 1, z);
        b.setType(Material.DISPENSER);
        BlockStorage.addBlockInfo(b, "id", reactor.getId(), true);
        return BlockStorage.getInventory(b);
    }

    /**
     * Runs one output tick on a fuel operation pre-charged to {@code progress}, so the
     * in-tick progress increment lands on a cooling checkpoint when {@code progress + 1}
     * is a multiple of the coolant duration. The trailing hologram update is not fully
     * supported by MockBukkit, so the RuntimeException from that tail is ignored - see
     * the class javadoc.
     */
    private int runTick(Block b, int progress) {
        Location l = b.getLocation();
        FuelOperation operation = new FuelOperation(new ItemStack(Material.IRON_INGOT), new ItemStack(Material.NETHERITE_INGOT), COOLANT_DURATION);
        operation.addProgress(progress);

        reactor.getMachineProcessor().startOperation(l, operation);

        Config data = BlockStorage.getLocationInfo(l);

        try {
            return reactor.getGeneratedOutput(l, data);
        } catch (RuntimeException ignored) {
            // The hologram tail is not fully supported by MockBukkit - see class javadoc
            return -1;
        }
    }

    /**
     * {@link BlockMenu#consumeItem(int)} leaves a zero-amount stack behind instead of clearing
     * the slot, so emptiness means {@code null} or amount zero.
     */
    private void assertCoolantConsumed(BlockMenu menu) {
        ItemStack slot = menu.getItemInSlot(COOLANT_SLOT);
        Assertions.assertTrue(slot == null || slot.getAmount() == 0, "The coolant cell must have been consumed, got: " + slot);
    }

    @Test
    @DisplayName("ReactorCoolantConsumeEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Location l = new Location(world, 1, 1, 1);
        ItemStack coolant = new ItemStack(Material.ICE);

        ReactorCoolantConsumeEvent event = new ReactorCoolantConsumeEvent(reactor, l, coolant, COOLANT_SLOT);

        Assertions.assertEquals(reactor, event.getReactor());
        Assertions.assertEquals(l, event.getLocation());
        Assertions.assertEquals(coolant, event.getCoolantItem());
        Assertions.assertEquals(COOLANT_SLOT, event.getSlot());
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new ReactorCoolantConsumeEvent(null, l, coolant, COOLANT_SLOT));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ReactorCoolantConsumeEvent(reactor, null, coolant, COOLANT_SLOT));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ReactorCoolantConsumeEvent(reactor, l, null, COOLANT_SLOT));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ReactorCoolantConsumeEvent(reactor, l, coolant, -1));
    }

    @Test
    @DisplayName("A cooling checkpoint fires the event and consumes the coolant cell")
    void testCheckpointFiresEventAndConsumesCell() {
        Block b = world.getBlockAt(10, 1, 10);
        BlockMenu menu = placeReactor(10, 10);
        menu.replaceExistingItem(COOLANT_SLOT, SlimefunItems.REACTOR_COOLANT_CELL.item());

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onCoolant(ReactorCoolantConsumeEvent event) {
                seen[0] = true;
                Assertions.assertEquals(reactor, event.getReactor());
                Assertions.assertEquals(COOLANT_SLOT, event.getSlot());
                Assertions.assertTrue(SlimefunItems.REACTOR_COOLANT_CELL.item().isSimilar(event.getCoolantItem()), "The coolant item must be the reactor's coolant cell");
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            runTick(b, COOLANT_DURATION - 1);

            Assertions.assertTrue(seen[0], "ReactorCoolantConsumeEvent was not fired");
            assertCoolantConsumed(menu);
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling ReactorCoolantConsumeEvent keeps the cell and yields no energy")
    void testCancelKeepsCellAndYieldsNothing() {
        Block b = world.getBlockAt(20, 1, 20);
        BlockMenu menu = placeReactor(20, 20);
        menu.replaceExistingItem(COOLANT_SLOT, SlimefunItems.REACTOR_COOLANT_CELL.item());

        Listener cancelling = new Listener() {
            @EventHandler
            public void onCoolant(ReactorCoolantConsumeEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            int produced = runTick(b, COOLANT_DURATION - 1);

            Assertions.assertEquals(0, produced, "A vetoed cooling must make the tick yield no energy");
            ItemStack slot = menu.getItemInSlot(COOLANT_SLOT);
            Assertions.assertNotNull(slot, "A vetoed cooling must keep the coolant cell");
            Assertions.assertEquals(1, slot.getAmount(), "A vetoed cooling must keep the coolant cell untouched");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Cooling without listeners still consumes the cell, preserving the old behavior")
    void testCoolingWithoutListenersStillConsumes() {
        Block b = world.getBlockAt(30, 1, 30);
        BlockMenu menu = placeReactor(30, 30);
        menu.replaceExistingItem(COOLANT_SLOT, SlimefunItems.REACTOR_COOLANT_CELL.item());

        runTick(b, COOLANT_DURATION - 1);

        assertCoolantConsumed(menu);
    }

    @Test
    @DisplayName("A tick between checkpoints fires no event and keeps the cell")
    void testBetweenCheckpointsFiresNothing() {
        Block b = world.getBlockAt(40, 1, 40);
        BlockMenu menu = placeReactor(40, 40);
        menu.replaceExistingItem(COOLANT_SLOT, SlimefunItems.REACTOR_COOLANT_CELL.item());

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onCoolant(ReactorCoolantConsumeEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            runTick(b, 10);

            Assertions.assertFalse(seen[0], "No event must be fired between cooling checkpoints");
            ItemStack slot = menu.getItemInSlot(COOLANT_SLOT);
            Assertions.assertNotNull(slot, "The coolant cell must have stayed put");
            Assertions.assertEquals(1, slot.getAmount());
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("A non-coolant item in the coolant slot fires no event and cools nothing")
    void testWrongItemFiresNothingAndYieldsNothing() {
        Block b = world.getBlockAt(50, 1, 50);
        BlockMenu menu = placeReactor(50, 50);
        menu.replaceExistingItem(COOLANT_SLOT, new ItemStack(Material.DIRT));

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onCoolant(ReactorCoolantConsumeEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            int produced = runTick(b, COOLANT_DURATION - 1);

            Assertions.assertEquals(0, produced, "An uncooled reactor tick must yield no energy");
            Assertions.assertFalse(seen[0], "No event must be fired for a non-coolant item");
            ItemStack slot = menu.getItemInSlot(COOLANT_SLOT);
            Assertions.assertNotNull(slot, "The foreign item must have stayed put");
            Assertions.assertEquals(Material.DIRT, slot.getType());
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }
}
