package io.github.thebusybiscuit.slimefun4.api.events;

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
import be.seeseemelk.mockbukkit.WorldMock;

import io.github.bakedlibs.dough.blocks.BlockPosition;
import io.github.thebusybiscuit.slimefun4.core.attributes.MachineProcessHolder;
import io.github.thebusybiscuit.slimefun4.core.machines.MachineOperation;
import io.github.thebusybiscuit.slimefun4.core.machines.MachineProcessor;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;

/**
 * Regression coverage for the machines API expansion: {@link AsyncMachineOperationStartEvent}.
 *
 * @author Zurker
 */
class TestMachineOperationStartEvent {

    private static ServerMock server;
    private static Slimefun plugin;
    private static WorldMock world;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = server.addSimpleWorld("machines");
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
    @DisplayName("AsyncMachineOperationStartEvent exposes position, processor, operation and cancellation")
    void testEventFieldsAndCancellation() {
        MachineProcessor<MachineOperation> processor = newProcessor();
        MachineOperation operation = Mockito.mock(MachineOperation.class);
        BlockPosition position = new BlockPosition(world, 1, 2, 3);

        AsyncMachineOperationStartEvent event = new AsyncMachineOperationStartEvent(position, processor, operation);

        Assertions.assertEquals(position, event.getPosition());
        Assertions.assertEquals(processor, event.getProcessor());
        Assertions.assertEquals(operation, event.getOperation());
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        event.setCancelled(false);
        Assertions.assertFalse(event.isCancelled());
    }

    @Test
    @DisplayName("startOperation fires AsyncMachineOperationStartEvent and starts the operation when not cancelled")
    void testStartOperationFiresEvent() {
        MachineProcessor<MachineOperation> processor = newProcessor();
        MachineOperation operation = Mockito.mock(MachineOperation.class);
        BlockPosition position = new BlockPosition(world, 4, 5, 6);

        CapturingListener listener = new CapturingListener();
        server.getPluginManager().registerEvents(listener, plugin);

        try {
            boolean started = processor.startOperation(position, operation);

            Assertions.assertTrue(started);
            Assertions.assertTrue(listener.fired);
            Assertions.assertEquals(position, listener.seenPosition);
            Assertions.assertEquals(operation, listener.seenOperation);
            Assertions.assertEquals(operation, processor.getOperation(position));
        } finally {
            HandlerList.unregisterAll(listener);
        }
    }

    @Test
    @DisplayName("Cancelling AsyncMachineOperationStartEvent prevents the operation from starting")
    void testStartOperationRespectsCancellation() {
        MachineProcessor<MachineOperation> processor = newProcessor();
        MachineOperation operation = Mockito.mock(MachineOperation.class);
        BlockPosition position = new BlockPosition(world, 7, 8, 9);

        CancellingListener listener = new CancellingListener();
        server.getPluginManager().registerEvents(listener, plugin);

        try {
            boolean started = processor.startOperation(position, operation);

            Assertions.assertFalse(started);
            Assertions.assertTrue(listener.fired);
            Assertions.assertNull(processor.getOperation(position));
        } finally {
            HandlerList.unregisterAll(listener);
        }
    }

    @Test
    @DisplayName("startOperation behaves identically when no listener is registered")
    void testStartOperationWithoutListeners() {
        MachineProcessor<MachineOperation> processor = newProcessor();
        MachineOperation operation = Mockito.mock(MachineOperation.class);
        BlockPosition position = new BlockPosition(world, 10, 11, 12);

        Assertions.assertTrue(processor.startOperation(position, operation));
        Assertions.assertEquals(operation, processor.getOperation(position));

        // A second start at the same position still fails (occupied semantics unchanged)
        Assertions.assertFalse(processor.startOperation(position, Mockito.mock(MachineOperation.class)));
    }

    @SuppressWarnings("unchecked")
    private static MachineProcessor<MachineOperation> newProcessor() {
        MachineProcessHolder<MachineOperation> owner = Mockito.mock(MachineProcessHolder.class);
        return new MachineProcessor<>(owner);
    }

    private static class CapturingListener implements Listener {
        boolean fired;
        BlockPosition seenPosition;
        MachineOperation seenOperation;

        @EventHandler
        public void onStart(AsyncMachineOperationStartEvent event) {
            fired = true;
            seenPosition = event.getPosition();
            seenOperation = event.getOperation();
        }
    }

    private static class CancellingListener implements Listener {
        boolean fired;

        @EventHandler
        public void onStart(AsyncMachineOperationStartEvent event) {
            fired = true;
            event.setCancelled(true);
        }
    }
}
