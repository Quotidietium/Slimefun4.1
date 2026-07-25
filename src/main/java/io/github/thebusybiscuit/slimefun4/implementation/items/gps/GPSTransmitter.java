package io.github.thebusybiscuit.slimefun4.implementation.items.gps;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetComponent;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockPlaceHandler;
import io.github.thebusybiscuit.slimefun4.core.networks.energy.EnergyNetComponentType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.SimpleSlimefunItem;

import me.mrCookieSlime.CSCoreLibPlugin.Configuration.Config;
import me.mrCookieSlime.Slimefun.Objects.handlers.BlockTicker;
import me.mrCookieSlime.Slimefun.api.BlockStorage;

public abstract class GPSTransmitter extends SimpleSlimefunItem<BlockTicker> implements EnergyNetComponent {

    private final int capacity;

    @ParametersAreNonnullByDefault
    protected GPSTransmitter(ItemGroup itemGroup, int tier, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);
        this.capacity = 4 << (2 * tier);

        addItemHandler(onPlace(), onBreak());
    }

    @Override
    public int getCapacity() {
        return capacity;
    }

    @Nonnull
    private BlockPlaceHandler onPlace() {
        return new BlockPlaceHandler(false) {

            @Override
            public void onPlayerPlace(BlockPlaceEvent e) {
                BlockStorage.addBlockInfo(e.getBlock(), "owner", e.getPlayer().getUniqueId().toString());
            }
        };
    }

    @Nonnull
    private BlockBreakHandler onBreak() {
        return new BlockBreakHandler(false, false) {

            @Override
            public void onPlayerBreak(BlockBreakEvent e, ItemStack item, List<ItemStack> drops) {
                Location l = e.getBlock().getLocation();
                UUID owner = getOwner(l);

                if (owner != null) {
                    Slimefun.getGPSNetwork().updateTransmitter(l, owner, false);
                }
            }
        };
    }

    /**
     * Resolves the {@link UUID} of the {@link org.bukkit.entity.Player} who placed this transmitter.
     * Returns {@code null} when the owner data is missing or corrupted, in which case the
     * transmitter is treated as unowned and skipped (no network updates).
     */
    @Nullable
    private UUID getOwner(@Nonnull Location l) {
        String owner = BlockStorage.getLocationInfo(l, "owner");

        if (owner == null) {
            return null;
        }

        try {
            return UUID.fromString(owner);
        } catch (IllegalArgumentException x) {
            return null;
        }
    }

    public abstract int getMultiplier(int y);

    public abstract int getEnergyConsumption();

    @Override
    public BlockTicker getItemHandler() {
        return new BlockTicker() {

            @Override
            public void tick(Block b, SlimefunItem item, Config data) {
                int charge = getCharge(b.getLocation(), data);
                UUID owner = getOwner(b.getLocation());

                if (owner == null) {
                    return;
                }

                if (charge >= getEnergyConsumption()) {
                    Slimefun.getGPSNetwork().updateTransmitter(b.getLocation(), owner, true);
                    removeCharge(b.getLocation(), getEnergyConsumption());
                } else {
                    Slimefun.getGPSNetwork().updateTransmitter(b.getLocation(), owner, false);
                }
            }

            @Override
            public boolean isSynchronized() {
                return false;
            }
        };
    }

    @Override
    public EnergyNetComponentType getEnergyComponentType() {
        return EnergyNetComponentType.CONSUMER;
    }

}
