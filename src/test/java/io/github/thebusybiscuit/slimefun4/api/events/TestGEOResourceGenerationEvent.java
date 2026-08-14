package io.github.thebusybiscuit.slimefun4.api.events;

import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

import io.github.thebusybiscuit.slimefun4.api.geo.GEOResource;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;

/**
 * Regression coverage for {@link GEOResourceGenerationEvent} (fields, value override, negative-value
 * rejection, dispatch). Previously untested despite r21 auditing the GEO subsystem.
 *
 * @author Zurker
 */
class TestGEOResourceGenerationEvent {

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
    @DisplayName("GEOResourceGenerationEvent exposes world/biome/coords/resource/value")
    void testContract() {
        World world = Mockito.mock(World.class);
        Mockito.when(world.getEnvironment()).thenReturn(World.Environment.NETHER);
        Biome biome = Biome.NETHER_WASTES;
        GEOResource resource = Mockito.mock(GEOResource.class);

        GEOResourceGenerationEvent event = new GEOResourceGenerationEvent(world, biome, 10, -7, resource, 32);

        Assertions.assertEquals(world, event.getWorld());
        Assertions.assertEquals(biome, event.getBiome());
        Assertions.assertEquals(World.Environment.NETHER, event.getEnvironment());
        Assertions.assertEquals(10, event.getChunkX());
        Assertions.assertEquals(-7, event.getChunkZ());
        Assertions.assertEquals(resource, event.getResource());
        Assertions.assertEquals(32, event.getValue());
    }

    @Test
    @DisplayName("The generated supply can be overridden but not to a negative value")
    void testValueOverride() {
        World world = Mockito.mock(World.class);
        GEOResource resource = Mockito.mock(GEOResource.class);
        GEOResourceGenerationEvent event = new GEOResourceGenerationEvent(world, Biome.PLAINS, 0, 0, resource, 5);

        event.setValue(64);
        Assertions.assertEquals(64, event.getValue());

        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setValue(-1));
        Assertions.assertEquals(64, event.getValue(), "A rejected setter must not mutate the value");
    }

    @Test
    @DisplayName("The constructor enforces the same non-negative and non-null invariants as the setter")
    void testConstructorValidation() {
        World world = Mockito.mock(World.class);
        GEOResource resource = Mockito.mock(GEOResource.class);

        Assertions.assertThrows(IllegalArgumentException.class, () -> new GEOResourceGenerationEvent(world, Biome.PLAINS, 0, 0, resource, -1));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new GEOResourceGenerationEvent(null, Biome.PLAINS, 0, 0, resource, 5));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new GEOResourceGenerationEvent(world, null, 0, 0, resource, 5));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new GEOResourceGenerationEvent(world, Biome.PLAINS, 0, 0, null, 5));
    }

    @Test
    @DisplayName("GEOResourceGenerationEvent is dispatchable and the value survives the round-trip")
    void testDispatch() {
        World world = Mockito.mock(World.class);
        GEOResource resource = Mockito.mock(GEOResource.class);
        GEOResourceGenerationEvent event = new GEOResourceGenerationEvent(world, Biome.PLAINS, 0, 0, resource, 8);

        int[] seen = { 0 };
        Listener listener = new Listener() {
            @EventHandler
            public void onGenerate(GEOResourceGenerationEvent e) {
                seen[0] = e.getValue();
                e.setValue(99);
            }
        };
        server.getPluginManager().registerEvents(listener, plugin);

        try {
            server.getPluginManager().callEvent(event);
            Assertions.assertEquals(8, seen[0]);
            Assertions.assertEquals(99, event.getValue(), "An addon's value override must be observable after dispatch");
        } finally {
            HandlerList.unregisterAll(listener);
        }
    }
}
