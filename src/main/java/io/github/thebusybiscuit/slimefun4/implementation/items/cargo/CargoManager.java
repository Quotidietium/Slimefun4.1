package io.github.thebusybiscuit.slimefun4.implementation.items.cargo;

import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.api.events.CargoNetVisualizerToggleEvent;
import io.github.thebusybiscuit.slimefun4.api.events.PlayerRightClickEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.HologramOwner;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockUseHandler;
import io.github.thebusybiscuit.slimefun4.core.networks.cargo.CargoNet;
import io.github.thebusybiscuit.slimefun4.implementation.handlers.SimpleBlockBreakHandler;

import me.mrCookieSlime.CSCoreLibPlugin.Configuration.Config;
import me.mrCookieSlime.Slimefun.Objects.handlers.BlockTicker;
import me.mrCookieSlime.Slimefun.api.BlockStorage;

public class CargoManager extends SlimefunItem implements HologramOwner {

    @ParametersAreNonnullByDefault
    public CargoManager(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);

        addItemHandler(onBreak());
    }

    @Nonnull
    private BlockBreakHandler onBreak() {
        return new SimpleBlockBreakHandler() {

            @Override
            public void onBlockBreak(@Nonnull Block b) {
                removeHologram(b);
            }
        };
    }

    @Override
    public void preRegister() {
        addItemHandler(new BlockTicker() {

            @Override
            public void tick(Block b, SlimefunItem item, Config data) {
                CargoNet.getNetworkFromLocationOrCreate(b.getLocation()).tick(b);
            }

            @Override
            public boolean isSynchronized() {
                return false;
            }

        }, new BlockUseHandler() {

            @Override
            public void onRightClick(PlayerRightClickEvent e) {
                Optional<Block> block = e.getClickedBlock();

                if (block.isPresent()) {
                    Player p = e.getPlayer();
                    Block b = block.get();

                    applyVisualizerToggle(p, b);
                }
            }
        });
    }

    /**
     * Toggles the cargo network visualizer for the given {@link CargoManager} block: fires
     * a vetoable {@link CargoNetVisualizerToggleEvent} first (only if anyone is listening),
     * then stores the new state. A vetoed toggle leaves the stored state untouched.
     * Without listeners this is a plain {@link BlockStorage} write, exactly as before.
     *
     * @param p
     *            The {@link Player} who right-clicked the {@link CargoManager}
     * @param b
     *            The {@link Block} of the {@link CargoManager}
     */
    @ParametersAreNonnullByDefault
    public void applyVisualizerToggle(Player p, Block b) {
        boolean previouslyEnabled = BlockStorage.getLocationInfo(b.getLocation(), "visualizer") == null;
        boolean enabled = !previouslyEnabled;

        if (CargoNetVisualizerToggleEvent.getHandlerList().getRegisteredListeners().length > 0) {
            CargoNetVisualizerToggleEvent event = new CargoNetVisualizerToggleEvent(p, this, b, previouslyEnabled, enabled);
            Bukkit.getPluginManager().callEvent(event);

            if (event.isCancelled()) {
                // An addon vetoed the toggle; the stored state stays as it is.
                return;
            }

            enabled = event.isEnabled();
        }

        BlockStorage.addBlockInfo(b, "visualizer", enabled ? null : "disabled");
        p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cCargo Net Visualizer: " + (enabled ? "&2\u2714" : "&4\u2718")));
    }

}
