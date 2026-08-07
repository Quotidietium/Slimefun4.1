package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang.Validate;
import org.bukkit.block.Block;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import io.github.thebusybiscuit.slimefun4.implementation.items.androids.AndroidInterface;
import io.github.thebusybiscuit.slimefun4.implementation.items.androids.Instruction;
import io.github.thebusybiscuit.slimefun4.implementation.items.androids.ProgrammableAndroid;

/**
 * This {@link Event} is fired whenever a {@link ProgrammableAndroid} executes the
 * {@link Instruction#INTERFACE_ITEMS} instruction and is about to deposit the contents of
 * its output slots into the {@link AndroidInterface} it is facing: the target block was
 * verified to be an items interface and the android's owner was verified to have access
 * to it.
 * <p>
 * Cancelling this event skips the deposit entirely: every item stays in the android's
 * output slots and the {@link AndroidInterface} inventory remains untouched.
 *
 * @author Zurker
 *
 * @see ProgrammableAndroid
 * @see AndroidInterface
 * @see AndroidFuelConsumeEvent
 */
public class AndroidItemDepositEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final ProgrammableAndroid android;
    private final Block block;
    private final Block interfaceBlock;
    private boolean cancelled;

    @ParametersAreNonnullByDefault
    public AndroidItemDepositEvent(ProgrammableAndroid android, Block block, Block interfaceBlock) {
        Validate.notNull(android, "The ProgrammableAndroid must not be null");
        Validate.notNull(block, "The android Block must not be null");
        Validate.notNull(interfaceBlock, "The interface Block must not be null");

        this.android = android;
        this.block = block;
        this.interfaceBlock = interfaceBlock;
    }

    /**
     * This returns the {@link ProgrammableAndroid} that is depositing items.
     *
     * @return The {@link ProgrammableAndroid}
     */
    @Nonnull
    public ProgrammableAndroid getAndroid() {
        return android;
    }

    /**
     * This returns the {@link Block} of the {@link ProgrammableAndroid}.
     *
     * @return The android {@link Block}
     */
    @Nonnull
    public Block getBlock() {
        return block;
    }

    /**
     * This returns the {@link Block} of the {@link AndroidInterface} that receives the items.
     *
     * @return The interface {@link Block}
     */
    @Nonnull
    public Block getInterfaceBlock() {
        return interfaceBlock;
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
