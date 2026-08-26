package io.github.thebusybiscuit.slimefun4.implementation.items.blocks;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

import io.github.bakedlibs.dough.common.ChatColors;
import io.github.bakedlibs.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun4.api.events.HologramProjectorOffsetChangeEvent;
import io.github.thebusybiscuit.slimefun4.api.events.HologramProjectorTextChangeEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.HologramOwner;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockPlaceHandler;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockUseHandler;
import io.github.thebusybiscuit.slimefun4.core.services.holograms.HologramsService;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.handlers.SimpleBlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.utils.ArmorStandUtils;
import io.github.thebusybiscuit.slimefun4.utils.ChatUtils;
import io.github.thebusybiscuit.slimefun4.utils.NumberUtils;

import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu;
import me.mrCookieSlime.Slimefun.api.BlockStorage;

/**
 * The {@link HologramProjector} is a very simple block which allows the {@link Player}
 * to create a floating text that is completely configurable.
 *
 * @author TheBusyBiscuit
 * @author Kry-Vosa
 * @author SoSeDiK
 *
 * @see HologramOwner
 * @see HologramsService
 *
 */
public class HologramProjector extends SlimefunItem implements HologramOwner {

    private static final String OFFSET_PARAMETER = "offset";

    @ParametersAreNonnullByDefault
    public HologramProjector(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe, ItemStack recipeOutput) {
        super(itemGroup, item, recipeType, recipe, recipeOutput);

        addItemHandler(onPlace(), onRightClick(), onBreak());
    }

    private @Nonnull BlockPlaceHandler onPlace() {
        return new BlockPlaceHandler(false) {

            @Override
            public void onPlayerPlace(BlockPlaceEvent e) {
                Block b = e.getBlockPlaced();
                BlockStorage.addBlockInfo(b, "text", "通过投影仪编辑此文本");
                BlockStorage.addBlockInfo(b, OFFSET_PARAMETER, "0.5");
                BlockStorage.addBlockInfo(b, "owner", e.getPlayer().getUniqueId().toString());

                getArmorStand(b, true);
            }

        };
    }

    private @Nonnull BlockBreakHandler onBreak() {
        return new SimpleBlockBreakHandler() {

            @Override
            public void onBlockBreak(@Nonnull Block b) {
                killArmorStand(b);
            }
        };
    }

    public @Nonnull BlockUseHandler onRightClick() {
        return e -> {
            e.cancel();

            Player p = e.getPlayer();
            Block b = e.getClickedBlock().get();

            String owner = BlockStorage.getLocationInfo(b.getLocation(), "owner");

            if (owner != null && owner.equals(p.getUniqueId().toString())) {
                openEditor(p, b);
            }
        };
    }

    /**
     * Applies new text to the hologram of the given projector and reopens the editor.
     * Extracted from the chat-input callback so the text change can be driven directly:
     * chat input cannot be simulated under MockBukkit. Without any
     * {@link HologramProjectorTextChangeEvent} listeners the behavior is identical to
     * the original inline body.
     *
     * @param p
     *            The {@link Player} who submitted the text
     * @param projector
     *            The {@link HologramProjector} {@link Block}
     * @param message
     *            The raw text the {@link Player} submitted
     */
    @ParametersAreNonnullByDefault
    void updateText(Player p, Block projector, String message) {
        /*
         * The editor was owner-gated when it opened, but the text arrives via chat input
         * which may be submitted seconds later - the projector may have been broken and
         * re-placed by someone else in the meantime. Do not carry the edit over to the
         * new owner's hologram (mirrors ElevatorPlate#renameFloor).
         */
        if (!isOwner(p, projector)) {
            return;
        }

        if (HologramProjectorTextChangeEvent.getHandlerList().getRegisteredListeners().length > 0) {
            String previousText = BlockStorage.getLocationInfo(projector.getLocation(), "text");
            HologramProjectorTextChangeEvent event = new HologramProjectorTextChangeEvent(p, this, projector, previousText, message);
            Bukkit.getPluginManager().callEvent(event);

            if (event.isCancelled()) {
                // An addon vetoed the change; the hologram keeps its old text.
                return;
            }

            message = event.getNewText();
        }

        ArmorStand hologram = getArmorStand(projector, true);
        hologram.setCustomName(ChatColors.color(message));
        BlockStorage.addBlockInfo(projector, "text", hologram.getCustomName());
        openEditor(p, projector);
    }

    private void openEditor(@Nonnull Player p, @Nonnull Block projector) {
        ChestMenu menu = new ChestMenu(Slimefun.getLocalization().getMessage(p, "machines.HOLOGRAM_PROJECTOR.inventory-title"));

        String text = BlockStorage.getLocationInfo(projector.getLocation(), "text");
        menu.addItem(0, CustomItemStack.create(Material.NAME_TAG, "&7文本 &e（点击编辑）", "", "&f" + ChatColors.color(text != null ? text : "")));
        menu.addMenuClickHandler(0, (pl, slot, item, action) -> {
            pl.closeInventory();
            Slimefun.getLocalization().sendMessage(pl, "machines.HOLOGRAM_PROJECTOR.enter-text", true);

            ChatUtils.awaitInput(pl, message -> {
                // Fixes #3445 - Make sure the projector is not broken
                if (!BlockStorage.check(projector, getId())) {
                    // Hologram projector no longer exists.
                    // TODO: Add a chat message informing the player that their message was ignored.
                    return;
                }

                updateText(pl, projector, message);
            });

            return false;
        });

        menu.addItem(1, CustomItemStack.create(Material.CLOCK, "&7偏移： &e" + NumberUtils.roundDecimalNumber(getOffset(projector) + 1.0D), "", "&f左键： &7+0.1", "&f右键： &7-0.1"));
        menu.addMenuClickHandler(1, (pl, slot, item, action) -> {
            double offset = NumberUtils.reparseDouble(getOffset(projector) + (action.isRightClicked() ? -0.1F : 0.1F));
            updateOffset(pl, projector, offset);
            return false;
        });

        menu.open(p);
    }

    /**
     * Applies a new vertical offset to the hologram of the given projector and reopens
     * the editor. Extracted from the editor's click handler so the offset change can be
     * driven directly: menu clicks cannot be simulated under MockBukkit. Without any
     * {@link HologramProjectorOffsetChangeEvent} listeners the behavior is identical to
     * the original inline body.
     *
     * @param p
     *            The {@link Player} who adjusted the offset
     * @param projector
     *            The {@link HologramProjector} {@link Block}
     * @param offset
     *            The new vertical offset for the hologram
     */
    @ParametersAreNonnullByDefault
    void updateOffset(Player p, Block projector, double offset) {
        /*
         * Same drift window as updateText: the editor re-opens after every change, so a
         * menu that was opened by the previous owner would otherwise keep adjusting the
         * projector indefinitely, even after it was re-placed by someone else.
         */
        if (!isOwner(p, projector)) {
            return;
        }

        if (HologramProjectorOffsetChangeEvent.getHandlerList().getRegisteredListeners().length > 0) {
            HologramProjectorOffsetChangeEvent event = new HologramProjectorOffsetChangeEvent(p, this, projector, getOffset(projector), offset);
            Bukkit.getPluginManager().callEvent(event);

            if (event.isCancelled()) {
                // An addon vetoed the adjustment; the hologram keeps its old offset.
                return;
            }

            offset = event.getNewOffset();
        }

        ArmorStand hologram = getArmorStand(projector, true);
        Location l = new Location(projector.getWorld(), projector.getX() + 0.5, projector.getY() + offset, projector.getZ() + 0.5);
        hologram.teleport(l);

        BlockStorage.addBlockInfo(projector, OFFSET_PARAMETER, String.valueOf(offset));
        openEditor(p, projector);
    }

    private static double getOffset(@Nonnull Block projector) {
        String raw = BlockStorage.getLocationInfo(projector.getLocation(), OFFSET_PARAMETER);

        if (raw == null) {
            return 0.5D;
        }

        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException x) {
            return 0.5D;
        }
    }

    /**
     * Verifies that the given {@link Player} still owns this projector. Real projectors
     * always carry an "owner" key (written by the place handler), so a missing or
     * mismatched value fails closed.
     */
    private static boolean isOwner(@Nonnull Player p, @Nonnull Block projector) {
        String owner = BlockStorage.getLocationInfo(projector.getLocation(), "owner");
        return p.getUniqueId().toString().equals(owner);
    }

    private static ArmorStand getArmorStand(@Nonnull Block projector, boolean createIfNoneExists) {
        String nametag = BlockStorage.getLocationInfo(projector.getLocation(), "text");
        double offset = getOffset(projector);
        Location l = new Location(projector.getWorld(), projector.getX() + 0.5, projector.getY() + offset, projector.getZ() + 0.5);

        for (Entity n : l.getChunk().getEntities()) {
            if (n instanceof ArmorStand armorStand && l.distanceSquared(n.getLocation()) < 0.4) {
                String customName = n.getCustomName();

                if (customName != null && customName.equals(nametag)) {
                    return armorStand;
                }
            }
        }

        if (!createIfNoneExists) {
            return null;
        }

        return ArmorStandUtils.spawnArmorStand(l, nametag);
    }

    private static void killArmorStand(@Nonnull Block b) {
        ArmorStand hologram = getArmorStand(b, false);

        if (hologram != null) {
            hologram.remove();
        }
    }
}
