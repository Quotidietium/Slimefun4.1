package io.github.thebusybiscuit.slimefun4.core.services;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

import io.github.thebusybiscuit.slimefun4.api.events.SlimefunBlockDataSaveEvent;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;

/**
 * Regression coverage for the block data persistence API expansion:
 * {@link SlimefunBlockDataSaveEvent}, exercised by calling the private
 * {@code AutoSavingService.saveAllBlocks()} via reflection.
 *
 * @author Zurker
 */
class TestSlimefunBlockDataSaveEvent {

    private static ServerMock server;
    private static Slimefun plugin;
    private static AutoSavingService service;

    @BeforeAll
    public static void load() throws Exception {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);

        Field field = Slimefun.class.getDeclaredField("autoSavingService");
        field.setAccessible(true);
        service = (AutoSavingService) field.get(plugin);
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    @BeforeEach
    public void beforeEach() {
        server.getPluginManager().clearEvents();
    }

    private void runSaveAllBlocks() throws Exception {
        Method method = AutoSavingService.class.getDeclaredMethod("saveAllBlocks");
        method.setAccessible(true);
        method.invoke(service);
    }

    @Test
    @DisplayName("SlimefunBlockDataSaveEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        SlimefunBlockDataSaveEvent event = new SlimefunBlockDataSaveEvent(3);

        Assertions.assertEquals(3, event.getWorldsSaved());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new SlimefunBlockDataSaveEvent(-1));
    }

    @Test
    @DisplayName("Running the block data save fires SlimefunBlockDataSaveEvent")
    void testSaveFiresEvent() throws Exception {
        boolean[] seen = { false };
        int[] saved = { -1 };
        Listener watcher = new Listener() {
            @EventHandler
            public void onBlockSave(SlimefunBlockDataSaveEvent event) {
                seen[0] = true;
                saved[0] = event.getWorldsSaved();
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            runSaveAllBlocks();

            Assertions.assertTrue(seen[0], "SlimefunBlockDataSaveEvent was not fired");
            Assertions.assertTrue(saved[0] >= 0, "The saved world count must be non-negative");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Running without listeners still completes, preserving the old behavior")
    void testSaveWithoutListenersCompletes() throws Exception {
        Assertions.assertDoesNotThrow(() -> runSaveAllBlocks());
    }
}
