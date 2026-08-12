package io.github.thebusybiscuit.slimefun4.implementation.operations;

import java.util.OptionalInt;

import javax.annotation.Nonnull;

import io.github.bakedlibs.dough.blocks.BlockPosition;
import io.github.thebusybiscuit.slimefun4.api.geo.GEOResource;
import io.github.thebusybiscuit.slimefun4.api.geo.ResourceManager;
import io.github.thebusybiscuit.slimefun4.core.machines.MachineOperation;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.geo.GEOMiner;

/**
 * This {@link MachineOperation} represents a {@link GEOMiner}
 * mining a {@link GEOResource}.
 *
 * @author iTwins
 *
 * @see GEOMiner
 */
public class GEOMiningOperation extends MiningOperation {

    private final GEOResource resource;
    private final int consumedSupplies;

    public GEOMiningOperation(@Nonnull GEOResource resource, int totalTicks) {
        this(resource, totalTicks, 1);
    }

    /**
     * This constructs a new {@link GEOMiningOperation} that consumed the given amount of
     * the chunk's supplies, so a mid-operation cancellation can return the same amount.
     *
     * @param resource
     *            The {@link GEOResource} being mined
     * @param totalTicks
     *            The total duration of the operation in ticks
     * @param consumedSupplies
     *            How many supply units this operation consumed, must not be negative
     */
    public GEOMiningOperation(@Nonnull GEOResource resource, int totalTicks, int consumedSupplies) {
        super(resource.getItem().clone(), totalTicks);
        org.apache.commons.lang.Validate.isTrue(consumedSupplies >= 0, "The consumed supplies must not be negative");

        this.resource = resource;
        this.consumedSupplies = consumedSupplies;
    }

    /**
     * This returns how many units of the chunk's supplies this operation consumed.
     *
     * @return The consumed supplies
     */
    public int getConsumedSupplies() {
        return consumedSupplies;
    }

    /**
     * This returns the {@link GEOResource} back to the chunk
     * when the {@link GEOMiningOperation} gets cancelled
     */
    @Override
    public void onCancel(@Nonnull BlockPosition position) {
        if (consumedSupplies > 0) {
            ResourceManager resourceManager = Slimefun.getGPSNetwork().getResourceManager();
            OptionalInt supplies = resourceManager.getSupplies(resource, position.getWorld(), position.getChunkX(), position.getChunkZ());
            supplies.ifPresent(s -> resourceManager.setSupplies(resource, position.getWorld(), position.getChunkX(), position.getChunkZ(), s + consumedSupplies));
        }
    }

}
