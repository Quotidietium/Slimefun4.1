package io.github.thebusybiscuit.slimefun4.implementation.items.electric.gadgets;

import org.bukkit.Location;
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
import be.seeseemelk.mockbukkit.entity.PlayerMock;

import io.github.thebusybiscuit.slimefun4.api.events.MultimeterReadEvent;
import io.github.thebusybiscuit.slimefun4.api.events.PlayerRightClickEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetComponent;
import io.github.thebusybiscuit.slimefun4.core.networks.energy.EnergyNetComponentType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

import me.mrCookieSlime.Slimefun.api.BlockStorage;

/**
 * Regression coverage for the multimeter API expansion: {@link MultimeterReadEvent}, exercised
 * by driving the real {@link Multimeter}
 * {@link io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler} with a constructed
 * {@link PlayerRightClickEvent} against a {@link BlockStorage}-backed {@link EnergyNetComponent}
 * block.
 * <p>
 * The readout path only sends chat messages, so tests can assert the outcome end-to-end:
 * a cancelled event leaves the player without a single message, the no-listener path still
 * delivers the classic readout.
 *
 * @author Zurker
 */
class TestMultimeterReadEvent {

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static Multimeter multimeter;
    private static TestEnergyComponent component;
    private static TestEnergyComponent unchargeable;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        // ProtectionManager is created when the integrations start
        Slimefun.getIntegrations().start();
        server.getScheduler().performOneTick();

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "multimeter_test");

        SlimefunItemStack meterStack = new SlimefunItemStack("_TEST_MULTIMETER", Material.CLOCK, "&6Test Multimeter");
        Slimefun.getItemCfg().setValue("_TEST_MULTIMETER.enabled", true);
        multimeter = new Multimeter(itemGroup, meterStack, RecipeType.NULL, new ItemStack[9]);
        multimeter.register(plugin);

        component = registerComponent(itemGroup, "_TEST_ENERGY_COMPONENT", 250);
        unchargeable = registerComponent(itemGroup, "_TEST_UNCHARGEABLE_COMPONENT", 0);
    }

    private static TestEnergyComponent registerComponent(ItemGroup itemGroup, String id, int capacity) {
        SlimefunItemStack stack = new SlimefunItemStack(id, Material.FURNACE, "&7Test Energy Component");
        Slimefun.getItemCfg().setValue(id + ".enabled", true);
        TestEnergyComponent item = new TestEnergyComponent(itemGroup, stack, capacity);
        item.register(plugin);
        return item;
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
     * Places a block wired to the given component via {@link BlockStorage}, optionally with a
     * stored charge.
     */
    private Block placeComponent(int x, int z, TestEnergyComponent item, Integer charge) {
        Block b = world.getBlockAt(x, 1, z);
        b.setType(Material.FURNACE);
        BlockStorage.addBlockInfo(b, "id", item.getId());

        if (charge != null) {
            BlockStorage.addBlockInfo(b, "energy-charge", String.valueOf(charge));
        }

        return b;
    }

    /**
     * Right-clicks the block with the multimeter via the real item use handler.
     */
    private void measure(Player player, Block block) {
        PlayerInteractEvent interactEvent = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, multimeter.getItem().clone(), block, BlockFace.UP);
        PlayerRightClickEvent event = new PlayerRightClickEvent(interactEvent);
        multimeter.getItemHandler().onRightClick(event);
    }

    @Test
    @DisplayName("MultimeterReadEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Player player = server.addPlayer();
        Location location = new Location(world, 1, 1, 1);

        MultimeterReadEvent event = new MultimeterReadEvent(player, multimeter, location, component, 120, 250);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(multimeter, event.getMultimeter());
        Assertions.assertEquals(location, event.getLocation());
        Assertions.assertEquals(component, event.getComponent());
        Assertions.assertEquals(120, event.getStored());
        Assertions.assertEquals(250, event.getCapacity());
        Assertions.assertFalse(event.isCancelled());

        // The displayed readings can be adjusted
        event.setStored(0);
        Assertions.assertEquals(0, event.getStored());
        event.setStored(999);
        Assertions.assertEquals(999, event.getStored());
        event.setCapacity(1);
        Assertions.assertEquals(1, event.getCapacity());
        event.setCapacity(500);
        Assertions.assertEquals(500, event.getCapacity());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setStored(-1));
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setCapacity(0));
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setCapacity(-5));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new MultimeterReadEvent(player, null, location, component, 120, 250));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new MultimeterReadEvent(player, multimeter, null, component, 120, 250));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new MultimeterReadEvent(player, multimeter, location, null, 120, 250));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new MultimeterReadEvent(player, multimeter, location, component, -1, 250));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new MultimeterReadEvent(player, multimeter, location, component, 120, 0));
    }

    @Test
    @DisplayName("Measuring a chargeable component fires the event with the readings")
    void testMeasureFiresEvent() {
        Player player = server.addPlayer();
        Block b = placeComponent(10, 10, component, 120);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onRead(MultimeterReadEvent event) {
                seen[0] = true;
                Assertions.assertEquals(multimeter, event.getMultimeter());
                Assertions.assertEquals(b.getLocation(), event.getLocation());
                Assertions.assertEquals(component, event.getComponent());
                Assertions.assertEquals(120, event.getStored(), "The stored charge must have been read from BlockStorage");
                Assertions.assertEquals(250, event.getCapacity());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            measure(player, b);

            Assertions.assertTrue(seen[0], "MultimeterReadEvent was not fired");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling MultimeterReadEvent sends no readout at all")
    void testCancelSendsNoMessage() {
        PlayerMock player = server.addPlayer();
        Block b = placeComponent(20, 20, component, 120);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onRead(MultimeterReadEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            measure(player, b);

            Assertions.assertNull(player.nextMessage(), "A cancelled readout must not send any message");
            Assertions.assertNull(player.nextComponentMessage(), "A cancelled readout must not dispatch any component");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Right-clicking a non-component block fires no event")
    void testNonComponentBlockFiresNothing() {
        PlayerMock player = server.addPlayer();
        Block stone = world.getBlockAt(30, 1, 30);
        stone.setType(Material.STONE);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onRead(MultimeterReadEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            measure(player, stone);

            Assertions.assertFalse(seen[0], "No event must be fired for a non-component block");
            Assertions.assertNull(player.nextMessage(), "No readout must be sent for a non-component block");
            Assertions.assertNull(player.nextComponentMessage(), "No component must be dispatched for a non-component block");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Right-clicking an unchargeable component fires no event")
    void testUnchargeableComponentFiresNothing() {
        PlayerMock player = server.addPlayer();
        Block b = placeComponent(40, 40, unchargeable, null);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onRead(MultimeterReadEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            measure(player, b);

            Assertions.assertFalse(seen[0], "No event must be fired for an unchargeable component");
            Assertions.assertNull(player.nextMessage(), "No readout must be sent for an unchargeable component");
            Assertions.assertNull(player.nextComponentMessage(), "No component must be dispatched for an unchargeable component");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Measuring without listeners still sends the readout, preserving the old behavior")
    void testMeasureWithoutListenersSendsReadout() {
        PlayerMock player = server.addPlayer();
        Block b = placeComponent(50, 50, component, 120);

        measure(player, b);

        // The display block frames the readout with two empty chat lines; the localized line
        // itself uses the LocalizationService replacer variant, which returns without sending
        // under MinecraftVersion.UNIT_TEST, so only the framing lines land in the chat queue.
        // The readings themselves are asserted end-to-end in testMeasureFiresEvent.
        Assertions.assertEquals("", player.nextMessage(), "The readout starts with an empty line");
        Assertions.assertEquals("", player.nextMessage(), "The readout ends with an empty line");
        Assertions.assertNull(player.nextMessage(), "The display block must not send further chat lines");
    }

    @Test
    @DisplayName("Adjusting the readings propagates through the dispatch and keeps the display")
    void testAdjustedReadingsPropagate() {
        PlayerMock player = server.addPlayer();
        Block b = placeComponent(60, 60, component, 120);

        // Registered first, so it runs before the observer within the same priority
        Listener adjusting = new Listener() {
            @EventHandler
            public void onRead(MultimeterReadEvent event) {
                event.setStored(7);
                event.setCapacity(3);
            }
        };

        boolean[] seenAdjusted = { false };
        Listener observer = new Listener() {
            @EventHandler
            public void onRead(MultimeterReadEvent event) {
                seenAdjusted[0] = true;
                Assertions.assertEquals(7, event.getStored(), "The adjusted stored charge must propagate through the dispatch");
                Assertions.assertEquals(3, event.getCapacity(), "The adjusted capacity must propagate through the dispatch");
            }
        };
        server.getPluginManager().registerEvents(adjusting, plugin);
        server.getPluginManager().registerEvents(observer, plugin);

        try {
            measure(player, b);

            Assertions.assertTrue(seenAdjusted[0], "MultimeterReadEvent was not fired");
            // The localized readout line is gated by MinecraftVersion.UNIT_TEST (see
            // testMeasureWithoutListenersSendsReadout), so only the framing lines are observable:
            // the display must still happen with the adjusted readings.
            Assertions.assertEquals("", player.nextMessage(), "The readout starts with an empty line");
            Assertions.assertEquals("", player.nextMessage(), "The readout ends with an empty line");
            Assertions.assertNull(player.nextMessage(), "The display block must not send further chat lines");
        } finally {
            HandlerList.unregisterAll(adjusting);
            HandlerList.unregisterAll(observer);
        }
    }

    /**
     * Minimal {@link EnergyNetComponent} {@link SlimefunItem} with a configurable capacity.
     */
    private static class TestEnergyComponent extends SlimefunItem implements EnergyNetComponent {

        private final int capacity;

        TestEnergyComponent(ItemGroup itemGroup, SlimefunItemStack item, int capacity) {
            super(itemGroup, item, RecipeType.NULL, new ItemStack[9]);
            this.capacity = capacity;
        }

        @Override
        public int getCapacity() {
            return capacity;
        }

        @Override
        public EnergyNetComponentType getEnergyComponentType() {
            return EnergyNetComponentType.CONSUMER;
        }
    }
}
