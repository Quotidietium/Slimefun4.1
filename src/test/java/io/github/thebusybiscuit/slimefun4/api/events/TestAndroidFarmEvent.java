package io.github.thebusybiscuit.slimefun4.api.events;

import org.bukkit.Material;
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
import org.mockito.Mockito;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.androids.AndroidInstance;

/**
 * Regression coverage for {@link AndroidFarmEvent} (fields, advanced flag, nullable drop override,
 * cancellation, dispatch).
 *
 * @author Zurker
 */
class TestAndroidFarmEvent {

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

    @Test
    @DisplayName("AndroidFarmEvent exposes block/android/advanced/drop and allows a nullable drop override")
    void testContract() {
        Block block = Mockito.mock(Block.class);
        AndroidInstance android = Mockito.mock(AndroidInstance.class);
        ItemStack drop = new ItemStack(Material.WHEAT);

        AndroidFarmEvent event = new AndroidFarmEvent(block, android, true, drop);

        Assertions.assertEquals(block, event.getBlock());
        Assertions.assertEquals(android, event.getAndroid());
        Assertions.assertTrue(event.isAdvanced());
        Assertions.assertEquals(drop, event.getDrop());

        // The drop may legitimately be set to null (a non-harvestable block yields nothing).
        event.setDrop(null);
        Assertions.assertNull(event.getDrop());

        ItemStack custom = new ItemStack(Material.CARROT);
        event.setDrop(custom);
        Assertions.assertEquals(custom, event.getDrop());
    }

    @Test
    @DisplayName("AndroidFarmEvent is cancellable and dispatchable (adaptive async reads sync on main thread)")
    void testCancellationAndDispatch() {
        Block block = Mockito.mock(Block.class);
        AndroidInstance android = Mockito.mock(AndroidInstance.class);

        boolean[] seen = { false };
        Listener listener = new Listener() {
            @EventHandler
            public void onFarm(AndroidFarmEvent e) {
                seen[0] = true;
                e.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(listener, plugin);

        try {
            AndroidFarmEvent event = new AndroidFarmEvent(block, android, false, null);
            Assertions.assertFalse(event.isAsynchronous(), "Adaptive async flag reports false on the main thread");

            server.getPluginManager().callEvent(event);

            Assertions.assertTrue(seen[0]);
            Assertions.assertTrue(event.isCancelled(), "Cancelling must skip the harvest");
        } finally {
            HandlerList.unregisterAll(listener);
        }
    }
}
