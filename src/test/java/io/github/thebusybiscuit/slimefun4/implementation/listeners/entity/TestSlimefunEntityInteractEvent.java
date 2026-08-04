package io.github.thebusybiscuit.slimefun4.implementation.listeners.entity;

import java.util.concurrent.atomic.AtomicBoolean;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
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

import io.github.thebusybiscuit.slimefun4.api.events.SlimefunEntityInteractEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.EntityInteractHandler;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.SimpleSlimefunItem;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Regression coverage for the entity interaction API expansion:
 * {@link SlimefunEntityInteractEvent}, exercised through the real
 * {@link EntityInteractionListener} dispatch path.
 *
 * @author Zurker
 */
class TestSlimefunEntityInteractEvent {

    private static ServerMock server;
    private static Slimefun plugin;

    private static SlimefunItem item;

    private static final AtomicBoolean interactHandlerCalled = new AtomicBoolean(false);
    private static final AtomicBoolean interactHandlerOffHand = new AtomicBoolean(false);

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        TestUtilities.createWorld(server);

        // Unit test startups never start the integrations, the ProtectionManager is created there
        Slimefun.getIntegrations().start();
        server.getScheduler().performOneTick();

        // Unit test startups do not register the listeners, register it manually
        new EntityInteractionListener(plugin);

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "entity_interact_test");
        SlimefunItemStack stack = new SlimefunItemStack("TEST_ENTITY_INTERACT_ITEM", Material.STICK, "&7Test Entity Interact Stick");

        item = new SimpleSlimefunItem<EntityInteractHandler>(itemGroup, stack, RecipeType.NULL, new ItemStack[9]) {
            @Override
            public EntityInteractHandler getItemHandler() {
                return (e, held, offHand) -> {
                    interactHandlerCalled.set(true);
                    interactHandlerOffHand.set(offHand);
                };
            }
        };
        item.register(plugin);
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    @BeforeEach
    public void beforeEach() {
        server.getPluginManager().clearEvents();
        interactHandlerCalled.set(false);
        interactHandlerOffHand.set(false);
    }

    @Test
    @DisplayName("SlimefunEntityInteractEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        PlayerMock player = server.addPlayer();
        PlayerMock target = server.addPlayer();
        ItemStack held = item.getItem().clone();
        PlayerInteractEntityEvent interactEvent = new PlayerInteractEntityEvent(player, target, EquipmentSlot.HAND);

        SlimefunEntityInteractEvent event = new SlimefunEntityInteractEvent(player, item, held, interactEvent);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(item, event.getSlimefunItem());
        Assertions.assertEquals(held, event.getItem());
        Assertions.assertEquals(interactEvent, event.getInteractEvent());
        Assertions.assertEquals(target, event.getRightClicked(), "The convenience getter must mirror the clicked entity");
        Assertions.assertFalse(event.isOffHand(), "The main hand must not report as off hand");
        Assertions.assertFalse(event.isCancelled());

        PlayerInteractEntityEvent offHandEvent = new PlayerInteractEntityEvent(player, target, EquipmentSlot.OFF_HAND);
        Assertions.assertTrue(new SlimefunEntityInteractEvent(player, item, held, offHandEvent).isOffHand(), "The off hand must report as off hand");

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new SlimefunEntityInteractEvent(player, null, held, interactEvent));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new SlimefunEntityInteractEvent(player, item, null, interactEvent));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new SlimefunEntityInteractEvent(player, item, held, null));
    }

    @Test
    @DisplayName("Right-clicking an entity with a Slimefun item fires the event and runs the handler")
    void testInteractFiresAndRunsHandler() {
        PlayerMock player = server.addPlayer();
        PlayerMock target = server.addPlayer();
        ItemStack held = item.getItem().clone();
        player.getInventory().setItemInMainHand(held);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onEntityInteract(SlimefunEntityInteractEvent event) {
                seen[0] = true;
                Assertions.assertEquals(player, event.getPlayer());
                Assertions.assertEquals(item, event.getSlimefunItem());
                Assertions.assertEquals(held, event.getItem());
                Assertions.assertEquals(target, event.getRightClicked());
                Assertions.assertFalse(event.isOffHand());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            PlayerInteractEntityEvent interactEvent = new PlayerInteractEntityEvent(player, target, EquipmentSlot.HAND);
            server.getPluginManager().callEvent(interactEvent);

            Assertions.assertTrue(seen[0], "SlimefunEntityInteractEvent was not fired");
            Assertions.assertTrue(interactHandlerCalled.get(), "The EntityInteractHandler was not called");
            Assertions.assertFalse(interactEvent.isCancelled(), "The underlying interaction must not be cancelled");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("An off-hand interaction reports the off hand to the event and the handler")
    void testOffHandInteract() {
        PlayerMock player = server.addPlayer();
        PlayerMock target = server.addPlayer();
        player.getInventory().setItemInOffHand(item.getItem().clone());

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onEntityInteract(SlimefunEntityInteractEvent event) {
                seen[0] = true;
                Assertions.assertTrue(event.isOffHand(), "The off hand interaction must report as off hand");
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            PlayerInteractEntityEvent interactEvent = new PlayerInteractEntityEvent(player, target, EquipmentSlot.OFF_HAND);
            server.getPluginManager().callEvent(interactEvent);

            Assertions.assertTrue(seen[0], "SlimefunEntityInteractEvent was not fired");
            Assertions.assertTrue(interactHandlerCalled.get(), "The EntityInteractHandler was not called");
            Assertions.assertTrue(interactHandlerOffHand.get(), "The handler must receive the off hand flag");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling SlimefunEntityInteractEvent skips the handler and cancels the interaction")
    void testInteractCancellationSkipsHandler() {
        PlayerMock player = server.addPlayer();
        PlayerMock target = server.addPlayer();
        player.getInventory().setItemInMainHand(item.getItem().clone());

        Listener cancelling = new Listener() {
            @EventHandler
            public void onEntityInteract(SlimefunEntityInteractEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            PlayerInteractEntityEvent interactEvent = new PlayerInteractEntityEvent(player, target, EquipmentSlot.HAND);
            server.getPluginManager().callEvent(interactEvent);

            Assertions.assertFalse(interactHandlerCalled.get(), "A cancelled entity interaction must not call the handler");
            Assertions.assertTrue(interactEvent.isCancelled(), "A cancelled entity interaction must cancel the underlying interaction");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("SlimefunEntityInteractEvent is not fired for vanilla items")
    void testInteractNotFiredForVanillaItem() {
        PlayerMock player = server.addPlayer();
        PlayerMock target = server.addPlayer();
        player.getInventory().setItemInMainHand(new ItemStack(Material.STICK));

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onEntityInteract(SlimefunEntityInteractEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            PlayerInteractEntityEvent interactEvent = new PlayerInteractEntityEvent(player, target, EquipmentSlot.HAND);
            server.getPluginManager().callEvent(interactEvent);

            Assertions.assertFalse(seen[0], "No event must be fired for a vanilla item");
            Assertions.assertFalse(interactEvent.isCancelled(), "A vanilla interaction must stay untouched");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }
}
