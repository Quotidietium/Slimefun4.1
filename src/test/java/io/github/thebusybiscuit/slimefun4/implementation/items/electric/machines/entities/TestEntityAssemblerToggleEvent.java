package io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines.entities;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
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

import io.github.thebusybiscuit.slimefun4.api.events.EntityAssemblerToggleEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;

/**
 * Regression coverage for the entity assembler API expansion:
 * {@link EntityAssemblerToggleEvent}, exercised by driving the package-private
 * {@link AbstractEntityAssembler#toggleEnabled} against a {@link BlockStorage}-backed
 * {@link WitherAssembler}.
 * <p>
 * A toggle is fully observable: the "enabled" block info flips and the toggle
 * button is redrawn (gunpowder when disabled, redstone when enabled). A vetoed
 * toggle keeps both untouched.
 *
 * @author Zurker
 */
class TestEntityAssemblerToggleEvent {

    // AbstractEntityAssembler#KEY_ENABLED, asserted literally because it is private
    private static final String ENABLED_KEY = "enabled";

    private static final int TOGGLE_SLOT = 22;

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static WitherAssembler assembler;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        // Non-configurable items stay DISABLED unless Items.yml says otherwise, enable it first
        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "assembler_toggle_test");
        SlimefunItemStack stack = new SlimefunItemStack("_TEST_WITHER_ASSEMBLER", Material.DISPENSER, "&fTest Wither Assembler");
        Slimefun.getItemCfg().setValue("_TEST_WITHER_ASSEMBLER.enabled", true);
        assembler = new WitherAssembler(itemGroup, stack, RecipeType.NULL, new ItemStack[9]);
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
     * Places the assembler as a real block backed by {@link BlockStorage} with the
     * given enabled state and returns its menu.
     * <p>
     * The "id" info is written last on purpose: writing it with update=true
     * immediately creates the menu and runs newInstance, and the toggle button
     * must already find the enabled state at that point to be drawn correctly.
     */
    private BlockMenu placeAssembler(Block b, boolean enabled) {
        b.setType(Material.DISPENSER);
        BlockStorage.addBlockInfo(b, ENABLED_KEY, String.valueOf(enabled), true);
        BlockStorage.addBlockInfo(b, "offset", "3.0", true);
        BlockStorage.addBlockInfo(b, "id", assembler.getId(), true);
        return BlockStorage.getInventory(b);
    }

    private String storedState(Block b) {
        return BlockStorage.getLocationInfo(b.getLocation(), ENABLED_KEY);
    }

    @Test
    @DisplayName("EntityAssemblerToggleEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Player player = server.addPlayer();
        Block b = world.getBlockAt(1, 60, 1);

        EntityAssemblerToggleEvent event = new EntityAssemblerToggleEvent(player, assembler, b, true);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(assembler, event.getAssembler());
        Assertions.assertEquals(b, event.getBlock());
        Assertions.assertTrue(event.isEnabling());
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new EntityAssemblerToggleEvent(player, null, b, true));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new EntityAssemblerToggleEvent(player, assembler, null, true));
    }

    @Test
    @DisplayName("Enabling fires the event, stores the state and redraws the button")
    void testEnableFiresEventAndFlipsState() {
        Player player = server.addPlayer();
        Block b = world.getBlockAt(10, 60, 10);
        BlockMenu menu = placeAssembler(b, false);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onToggle(EntityAssemblerToggleEvent event) {
                seen[0] = true;
                Assertions.assertEquals(player, event.getPlayer());
                Assertions.assertEquals(assembler, event.getAssembler());
                Assertions.assertEquals(b, event.getBlock());
                Assertions.assertTrue(event.isEnabling());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            assembler.toggleEnabled(player, menu, b, true);

            Assertions.assertTrue(seen[0], "EntityAssemblerToggleEvent was not fired");
            Assertions.assertEquals("true", storedState(b), "The assembler must have been enabled");
            Assertions.assertEquals(Material.REDSTONE, menu.getItemInSlot(TOGGLE_SLOT).getType(), "The button must show the enabled state");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling EntityAssemblerToggleEvent keeps the state and the button")
    void testCancelKeepsStateAndButton() {
        Player player = server.addPlayer();
        Block b = world.getBlockAt(20, 60, 20);
        BlockMenu menu = placeAssembler(b, false);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onToggle(EntityAssemblerToggleEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            assembler.toggleEnabled(player, menu, b, true);

            Assertions.assertEquals("false", storedState(b), "A vetoed toggle must keep the assembler disabled");
            Assertions.assertEquals(Material.GUNPOWDER, menu.getItemInSlot(TOGGLE_SLOT).getType(), "A vetoed toggle must not redraw the button");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Disabling fires the event with isEnabling false and flips the state back")
    void testDisableFiresEventAndFlipsState() {
        Player player = server.addPlayer();
        Block b = world.getBlockAt(30, 60, 30);
        BlockMenu menu = placeAssembler(b, true);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onToggle(EntityAssemblerToggleEvent event) {
                seen[0] = true;
                Assertions.assertFalse(event.isEnabling(), "The event must carry the target state of the clicked button");
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            assembler.toggleEnabled(player, menu, b, false);

            Assertions.assertTrue(seen[0], "EntityAssemblerToggleEvent was not fired");
            Assertions.assertEquals("false", storedState(b), "The assembler must have been disabled");
            Assertions.assertEquals(Material.GUNPOWDER, menu.getItemInSlot(TOGGLE_SLOT).getType(), "The button must show the disabled state");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling a disable keeps the assembler enabled")
    void testCancelDisableKeepsEnabled() {
        Player player = server.addPlayer();
        Block b = world.getBlockAt(40, 60, 40);
        BlockMenu menu = placeAssembler(b, true);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onToggle(EntityAssemblerToggleEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            assembler.toggleEnabled(player, menu, b, false);

            Assertions.assertEquals("true", storedState(b), "A vetoed toggle must keep the assembler enabled");
            Assertions.assertEquals(Material.REDSTONE, menu.getItemInSlot(TOGGLE_SLOT).getType(), "A vetoed toggle must not redraw the button");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Toggling without listeners still flips the state, preserving the old behavior")
    void testToggleWithoutListenersFlips() {
        Player player = server.addPlayer();
        Block b = world.getBlockAt(50, 60, 50);
        BlockMenu menu = placeAssembler(b, false);

        assembler.toggleEnabled(player, menu, b, true);

        Assertions.assertEquals("true", storedState(b), "The assembler must have been enabled");
        Assertions.assertEquals(Material.REDSTONE, menu.getItemInSlot(TOGGLE_SLOT).getType(), "The button must show the enabled state");
    }
}
