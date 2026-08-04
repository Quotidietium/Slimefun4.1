package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import io.github.bakedlibs.dough.blocks.BlockPosition;
import io.github.thebusybiscuit.slimefun4.core.machines.MachineOperation;
import io.github.thebusybiscuit.slimefun4.core.machines.MachineProcessor;

/**
 * This {@link Event} is fired whenever a {@link MachineProcessor} is about to start
 * a {@link MachineOperation}.
 * <p>
 * Cancelling this event prevents the {@link MachineOperation} from being registered.
 * The machine will then behave as if the start attempt failed and (depending on the
 * machine) may retry on its next tick.
 *
 * @author Zurker
 *
 * @see AsyncMachineOperationFinishEvent
 */
public class AsyncMachineOperationStartEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final BlockPosition position;
    private final MachineProcessor<?> machineProcessor;
    private final MachineOperation machineOperation;

    private boolean cancelled;

    public <T extends MachineOperation> AsyncMachineOperationStartEvent(BlockPosition pos, MachineProcessor<T> processor, T operation) {
        super(!Bukkit.isPrimaryThread());

        this.position = pos;
        this.machineProcessor = processor;
        this.machineOperation = operation;
    }

    /**
     * This returns the {@link BlockPosition} of the machine.
     *
     * @return The {@link BlockPosition} of the machine
     */
    @Nonnull
    public BlockPosition getPosition() {
        return position;
    }

    /**
     * The {@link MachineProcessor} instance of the machine.
     *
     * @return The {@link MachineProcessor} instance of the machine
     */
    @Nullable
    public MachineProcessor<?> getProcessor() {
        return machineProcessor;
    }

    /**
     * This returns the {@link MachineOperation} that is about to start.
     *
     * @return The {@link MachineOperation} of the process
     */
    @Nullable
    public MachineOperation getOperation() {
        return machineOperation;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Nonnull
    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Nonnull
    @Override
    public HandlerList getHandlers() {
        return getHandlerList();
    }
}
