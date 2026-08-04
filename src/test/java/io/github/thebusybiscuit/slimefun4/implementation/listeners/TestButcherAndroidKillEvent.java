package io.github.thebusybiscuit.slimefun4.implementation.listeners;

import java.util.ArrayList;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.CowMock;
import be.seeseemelk.mockbukkit.entity.ItemEntityMock;

import io.github.thebusybiscuit.slimefun4.api.events.ButcherAndroidKillEvent;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.androids.AndroidInstance;
import io.github.thebusybiscuit.slimefun4.implementation.items.androids.ProgrammableAndroid;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Regression coverage for the butcher android API expansion:
 * {@link ButcherAndroidKillEvent}, exercised through the real {@link ButcherAndroidListener}
 * drop-harvesting path. The android itself is a Mockito mock so the collected drops can
 * be verified on {@code addItems}.
 *
 * @author Zurker
 */
class TestButcherAndroidKillEvent {

    private static final String METADATA_KEY = "android_killer";

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        // Unit test startups do not register the listeners, register it manually
        new ButcherAndroidListener(plugin);
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
     * Creates a cow marked as killed by a mocked android at the given position, with a
     * dropped item next to it waiting to be harvested.
     */
    private CowMock setupKill(int x, int z, ProgrammableAndroid android, Block androidBlock) {
        Location loc = new Location(world, x, 1, z);

        ItemEntityMock drop = new ItemEntityMock(server, UUID.randomUUID(), new ItemStack(Material.LEATHER));
        drop.setLocation(loc);
        server.registerEntity(drop);

        CowMock cow = new CowMock(server, UUID.randomUUID());
        cow.setLocation(loc);
        server.registerEntity(cow);
        cow.setMetadata(METADATA_KEY, new FixedMetadataValue(plugin, new AndroidInstance(android, androidBlock)));
        return cow;
    }

    /**
     * Lets the cow die through the real event pipeline and returns the death event.
     */
    private EntityDeathEvent kill(CowMock cow) {
        EntityDeathEvent event = new EntityDeathEvent(cow, Mockito.mock(org.bukkit.damage.DamageSource.class), new ArrayList<>());
        server.getPluginManager().callEvent(event);
        return event;
    }

    /**
     * Runs the scheduled harvest task. The experience orb tail may not be fully
     * supported by MockBukkit, so a RuntimeException from it is ignored here - the
     * drops were collected into the android beforehand.
     */
    private void runHarvestTask() {
        try {
            server.getScheduler().performTicks(3);
        } catch (RuntimeException ignored) {
            // See the javadoc above
        }
    }

    @Test
    @DisplayName("ButcherAndroidKillEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        ProgrammableAndroid android = Mockito.mock(ProgrammableAndroid.class);
        Block b = world.getBlockAt(1, 1, 1);
        CowMock cow = new CowMock(server, UUID.randomUUID());
        EntityDeathEvent deathEvent = new EntityDeathEvent(cow, Mockito.mock(org.bukkit.damage.DamageSource.class), new ArrayList<>());

        ButcherAndroidKillEvent event = new ButcherAndroidKillEvent(android, b, cow, deathEvent);

        Assertions.assertEquals(android, event.getAndroid());
        Assertions.assertEquals(b, event.getBlock());
        Assertions.assertEquals(cow, event.getEntity());
        Assertions.assertEquals(deathEvent, event.getDeathEvent());
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new ButcherAndroidKillEvent(null, b, cow, deathEvent));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ButcherAndroidKillEvent(android, null, cow, deathEvent));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ButcherAndroidKillEvent(android, b, null, deathEvent));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ButcherAndroidKillEvent(android, b, cow, null));
    }

    @Test
    @DisplayName("A butcher android kill fires the event and harvests the drops")
    void testKillFiresAndHarvests() {
        ProgrammableAndroid android = Mockito.mock(ProgrammableAndroid.class);
        Block androidBlock = world.getBlockAt(10, 1, 10);
        CowMock cow = setupKill(10, 10, android, androidBlock);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onKill(ButcherAndroidKillEvent event) {
                seen[0] = true;
                Assertions.assertEquals(android, event.getAndroid());
                Assertions.assertEquals(androidBlock, event.getBlock());
                Assertions.assertEquals(cow, event.getEntity());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            kill(cow);

            Assertions.assertTrue(seen[0], "ButcherAndroidKillEvent was not fired");
            Assertions.assertFalse(cow.hasMetadata(METADATA_KEY), "The android metadata must have been cleaned up");

            runHarvestTask();

            ItemStack[] harvested = captureHarvest(android, androidBlock);
            Assertions.assertEquals(1, harvested.length, "Exactly the dropped item must have been harvested");
            Assertions.assertEquals(Material.LEATHER, harvested[0].getType(), "The dropped item must have been harvested");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    /**
     * Captures the varargs array the harvest was collected into. Mockito expands varargs
     * when matching arguments, so an {@code argThat} on the array itself would see the
     * individual elements - an {@link org.mockito.ArgumentCaptor} does not.
     */
    private ItemStack[] captureHarvest(ProgrammableAndroid android, Block androidBlock) {
        org.mockito.ArgumentCaptor<ItemStack[]> captor = org.mockito.ArgumentCaptor.forClass(ItemStack[].class);
        Mockito.verify(android).addItems(Mockito.eq(androidBlock), captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("Cancelling ButcherAndroidKillEvent leaves the drops on the ground")
    void testEventCancellationSkipsHarvest() {
        ProgrammableAndroid android = Mockito.mock(ProgrammableAndroid.class);
        Block androidBlock = world.getBlockAt(20, 1, 20);
        CowMock cow = setupKill(20, 20, android, androidBlock);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onKill(ButcherAndroidKillEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            kill(cow);
            runHarvestTask();

            Mockito.verify(android, Mockito.never()).addItems(Mockito.any(), Mockito.any());
            Assertions.assertFalse(cow.hasMetadata(METADATA_KEY), "The android metadata must have been cleaned up");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Harvesting without listeners still applies, preserving the old behavior")
    void testHarvestWithoutListenersStillApplies() {
        ProgrammableAndroid android = Mockito.mock(ProgrammableAndroid.class);
        Block androidBlock = world.getBlockAt(30, 1, 30);
        CowMock cow = setupKill(30, 30, android, androidBlock);

        kill(cow);
        runHarvestTask();

        ItemStack[] harvested = captureHarvest(android, androidBlock);
        Assertions.assertEquals(1, harvested.length, "Exactly the dropped item must have been harvested");
        Assertions.assertEquals(Material.LEATHER, harvested[0].getType(), "The dropped item must have been harvested");
    }

    @Test
    @DisplayName("An entity not killed by an android fires no event")
    void testNoAndroidMetadataFiresNothing() {
        CowMock cow = new CowMock(server, UUID.randomUUID());
        cow.setLocation(new Location(world, 40, 1, 40));
        server.registerEntity(cow);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onKill(ButcherAndroidKillEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            kill(cow);

            Assertions.assertFalse(seen[0], "No event must be fired without the android metadata");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }
}
