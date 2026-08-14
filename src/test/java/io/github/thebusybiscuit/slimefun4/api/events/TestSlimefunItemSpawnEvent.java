package io.github.thebusybiscuit.slimefun4.api.events;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
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
import org.mockito.Mockito;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

import io.github.thebusybiscuit.slimefun4.api.items.ItemSpawnReason;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;

/**
 * Regression coverage for {@link SlimefunItemSpawnEvent} (fields, optional player, location/item
 * overrides, constructor + setter validation consistency, cancellation, dispatch).
 *
 * @author Zurker
 */
class TestSlimefunItemSpawnEvent {

    private static ServerMock server;
    private static Slimefun plugin;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    @BeforeEach
    public void beforeEach() {
        server.getPluginManager().clearEvents();
    }

    private Location loc() {
        return new Location(Mockito.mock(World.class), 1, 2, 3);
    }

    @Test
    @DisplayName("SlimefunItemSpawnEvent exposes location/item/reason and an optional player")
    void testContract() {
        Player player = server.addPlayer();
        ItemStack item = new ItemStack(Material.IRON_INGOT);
        Location location = loc();

        SlimefunItemSpawnEvent event = new SlimefunItemSpawnEvent(player, location, item, ItemSpawnReason.CARGO_OVERFLOW);

        Assertions.assertEquals(location, event.getLocation());
        Assertions.assertEquals(item, event.getItemStack());
        Assertions.assertEquals(ItemSpawnReason.CARGO_OVERFLOW, event.getItemSpawnReason());
        Assertions.assertTrue(event.getPlayer().isPresent());
        Assertions.assertEquals(player, event.getPlayer().get());

        // The 3-arg constructor carries no player.
        SlimefunItemSpawnEvent anonymous = new SlimefunItemSpawnEvent(loc(), new ItemStack(Material.STONE), ItemSpawnReason.CARGO_OVERFLOW);
        Assertions.assertTrue(anonymous.getPlayer().isEmpty());
    }

    @Test
    @DisplayName("The constructor enforces the same invariants as the setters (no null/air item, no null location)")
    void testConstructorValidation() {
        ItemStack item = new ItemStack(Material.DIAMOND);
        Location location = loc();

        Assertions.assertThrows(IllegalArgumentException.class, () -> new SlimefunItemSpawnEvent(null, null, item, ItemSpawnReason.CARGO_OVERFLOW), "null location must be rejected");
        Assertions.assertThrows(IllegalArgumentException.class, () -> new SlimefunItemSpawnEvent(null, location, null, ItemSpawnReason.CARGO_OVERFLOW), "null item must be rejected");
        Assertions.assertThrows(IllegalArgumentException.class, () -> new SlimefunItemSpawnEvent(null, location, new ItemStack(Material.AIR), ItemSpawnReason.CARGO_OVERFLOW), "air item must be rejected");
    }

    @Test
    @DisplayName("Setters reject null location, null item and air item")
    void testSetterValidation() {
        SlimefunItemSpawnEvent event = new SlimefunItemSpawnEvent(loc(), new ItemStack(Material.DIAMOND), ItemSpawnReason.CARGO_OVERFLOW);

        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setLocation(null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setItemStack(null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setItemStack(new ItemStack(Material.AIR)));
    }

    @Test
    @DisplayName("Cancelling and dispatching SlimefunItemSpawnEvent with a redirected location")
    void testCancellationAndDispatch() {
        ItemStack item = new ItemStack(Material.GOLD_INGOT);

        Location[] redirected = { loc() };
        redirected[0] = new Location(Mockito.mock(World.class), 9, 9, 9);

        Listener listener = new Listener() {
            @EventHandler
            public void onSpawn(SlimefunItemSpawnEvent e) {
                e.setLocation(redirected[0]);
            }
        };
        server.getPluginManager().registerEvents(listener, plugin);

        try {
            SlimefunItemSpawnEvent event = new SlimefunItemSpawnEvent(loc(), item, ItemSpawnReason.CARGO_OVERFLOW);
            server.getPluginManager().callEvent(event);

            Assertions.assertEquals(redirected[0], event.getLocation(), "An addon location override must be observable");

            event.setCancelled(true);
            Assertions.assertTrue(event.isCancelled());
        } finally {
            HandlerList.unregisterAll(listener);
        }
    }
}
