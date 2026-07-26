package io.github.thebusybiscuit.slimefun4.implementation.listeners;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang.Validate;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerBackpack;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile;
import io.github.thebusybiscuit.slimefun4.core.services.sounds.SoundEffect;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.backpacks.Cooler;
import io.github.thebusybiscuit.slimefun4.implementation.items.backpacks.SlimefunBackpack;

/**
 * This {@link Listener} is responsible for all events centered around a {@link SlimefunBackpack}.
 * This also includes the {@link Cooler}
 * 
 * @author TheBusyBiscuit
 * @author Walshy
 * @author NihilistBrew
 * @author AtomicScience
 * @author VoidAngel
 * @author John000708
 * 
 * @see SlimefunBackpack
 * @see PlayerBackpack
 *
 */
public class BackpackListener implements Listener {

    private final Map<UUID, ItemStack> backpacks = new ConcurrentHashMap<>();

    public void register(@Nonnull Slimefun plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        Player p = (Player) e.getPlayer();

        if (markBackpackDirty(p)) {
            SoundEffect.BACKPACK_CLOSE_SOUND.playFor(p);
        }
    }

    private boolean markBackpackDirty(@Nonnull Player p) {
        ItemStack backpack = backpacks.remove(p.getUniqueId());

        if (backpack != null) {
            PlayerProfile.getBackpack(backpack, PlayerBackpack::markDirty);
            return true;
        } else {
            return false;
        }
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent e) {
        if (backpacks.containsKey(e.getPlayer().getUniqueId())) {
            ItemStack item = e.getItemDrop().getItemStack();
            SlimefunItem sfItem = SlimefunItem.getByItem(item);

            if (sfItem instanceof SlimefunBackpack) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent e) {
        // getClickedInventory() may be null for certain InventoryActions (e.g. clicking outside
        // the inventory). Guard against NPE before calling .getType().
        if (e.getClickedInventory() == null) {
            return;
        }

        ItemStack item = backpacks.get(e.getWhoClicked().getUniqueId());

        if (item != null) {
            SlimefunItem backpack = SlimefunItem.getByItem(item);

            if (backpack instanceof SlimefunBackpack slimefunBackpack) {
                if (e.getClick() == ClickType.NUMBER_KEY) {
                    // Prevent disallowed items from being moved using number keys.
                    if (e.getClickedInventory().getType() != InventoryType.PLAYER) {
                        ItemStack hotbarItem = e.getWhoClicked().getInventory().getItem(e.getHotbarButton());

                        if (!isAllowed(slimefunBackpack, hotbarItem)) {
                            e.setCancelled(true);
                        }
                    }
                } else if (e.getClick() == ClickType.SWAP_OFFHAND) {
                    if (e.getClickedInventory().getType() != InventoryType.PLAYER) {
                        // Fixes #3265 - Don't move disallowed items using the off hand.
                        ItemStack offHandItem = e.getWhoClicked().getInventory().getItemInOffHand();

                        if (!isAllowed(slimefunBackpack, offHandItem)) {
                            e.setCancelled(true);
                        }
                    } else {
                        // Fixes #3664 - Do not swap the backpack to your off hand.
                        if (e.getCurrentItem() != null && e.getCurrentItem().isSimilar(item)) {
                            e.setCancelled(true);
                        }
                    }
                } else if (e.getClickedInventory().getType() == InventoryType.PLAYER) {
                    // Shift-clicking inside the Player's own inventory moves the item into the backpack.
                    if (e.getClick().isShiftClick() && !isAllowed(slimefunBackpack, e.getCurrentItem())) {
                        e.setCancelled(true);
                    }
                } else if (!isAllowed(slimefunBackpack, e.getCursor()) || !isAllowed(slimefunBackpack, e.getCurrentItem())) {
                    /*
                     * Clicking inside the backpack GUI may place the item held on the cursor
                     * into the backpack (or swap it with the clicked slot's content), so the
                     * cursor item must be checked - previously only the clicked slot's item
                     * was checked, which is null for empty slots and allowed disallowed items
                     * (e.g. shulker boxes or nested backpacks) to be placed into empty slots.
                     * The clicked item is still checked as well: interacting with a disallowed
                     * item that somehow ended up inside the backpack stays blocked.
                     */
                    e.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrag(InventoryDragEvent e) {
        ItemStack item = backpacks.get(e.getWhoClicked().getUniqueId());

        if (item == null) {
            return;
        }

        SlimefunItem backpack = SlimefunItem.getByItem(item);

        if (backpack instanceof SlimefunBackpack slimefunBackpack) {
            int topInventorySize = e.getView().getTopInventory().getSize();

            for (int rawSlot : e.getRawSlots()) {
                if (rawSlot < topInventorySize) {
                    /*
                     * Dragging distributes the dragged item across the affected slots.
                     * InventoryDragEvent is NOT an InventoryClickEvent, so the click-based
                     * checks above never see this - validate the dragged item here.
                     */
                    if (!isAllowed(slimefunBackpack, e.getOldCursor())) {
                        e.setCancelled(true);
                        return;
                    }
                }
            }
        }
    }

    private boolean isAllowed(@Nonnull SlimefunBackpack backpack, @Nullable ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return true;
        }

        return backpack.isItemAllowed(item, SlimefunItem.getByItem(item));
    }

    @ParametersAreNonnullByDefault
    public void openBackpack(Player p, ItemStack item, SlimefunBackpack backpack) {
        if (item.getAmount() == 1) {
            if (backpack.canUse(p, true) && !PlayerProfile.get(p, profile -> openBackpack(p, item, profile, backpack.getSize()))) {
                Slimefun.getLocalization().sendMessage(p, "messages.opening-backpack");
            }
        } else {
            Slimefun.getLocalization().sendMessage(p, "backpack.no-stack", true);
        }
    }

    @ParametersAreNonnullByDefault
    private void openBackpack(Player p, ItemStack item, PlayerProfile profile, int size) {
        ItemMeta meta = item.getItemMeta();

        // The lore (and the "ID: <ID>" line) may be missing if the item was stripped/edited.
        // ItemMeta.getLore() returns null when there is no lore.
        if (meta == null || !meta.hasLore()) {
            return;
        }

        List<String> lore = meta.getLore();

        for (int line = 0; line < lore.size(); line++) {
            if (lore.get(line).equals(ChatColor.GRAY + "ID: <ID>")) {
                setBackpackId(p, item, line, profile.createBackpack(size).getId());
                break;
            }
        }

        /*
         * If the current Player is already viewing a backpack (for whatever reason),
         * terminate that view.
         */
        if (markBackpackDirty(p)) {
            p.closeInventory();
        }

        // Check if someone else is currently viewing this backpack
        if (!backpacks.containsValue(item)) {
            PlayerProfile.getBackpack(item, backpack -> {
                // Only the owner (or an admin with the bypass permission) may open a backpack.
                // Without this check, a forged lore line ("ID: <victim-uuid>#<id>") would allow
                // anyone to open and loot arbitrary players' backpacks (IDOR).
                if (backpack != null && (p.getUniqueId().equals(backpack.getOwnerId()) || p.hasPermission("slimefun.inventory.bypass"))) {
                    SoundEffect.BACKPACK_OPEN_SOUND.playAt(p.getLocation(), SoundCategory.PLAYERS);
                    backpacks.put(p.getUniqueId(), item);
                    backpack.open(p);
                }
            });
        } else {
            Slimefun.getLocalization().sendMessage(p, "backpack.already-open", true);
        }
    }

    /**
     * This method sets the id for a backpack onto the given {@link ItemStack}.
     * 
     * @param backpackOwner
     *            The owner of this backpack
     * @param item
     *            The {@link ItemStack} to modify
     * @param line
     *            The line at which the ID should be replaced
     * @param id
     *            The id of this backpack
     */
    public void setBackpackId(@Nonnull OfflinePlayer backpackOwner, @Nonnull ItemStack item, int line, int id) {
        Validate.notNull(backpackOwner, "Backpacks must have an owner!");
        Validate.notNull(item, "Cannot set the id onto null!");

        ItemMeta im = item.getItemMeta();

        if (!im.hasLore()) {
            throw new IllegalArgumentException("This backpack does not have any lore!");
        }

        List<String> lore = im.getLore();

        if (line >= lore.size() || !lore.get(line).contains("<ID>")) {
            throw new IllegalArgumentException("Specified a line that is out of bounds or invalid!");
        }

        lore.set(line, lore.get(line).replace("<ID>", backpackOwner.getUniqueId() + "#" + id));
        im.setLore(lore);
        item.setItemMeta(im);
    }
}
