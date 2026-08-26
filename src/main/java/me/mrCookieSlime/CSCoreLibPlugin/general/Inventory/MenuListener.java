package me.mrCookieSlime.CSCoreLibPlugin.general.Inventory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.plugin.Plugin;

import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu.AdvancedMenuClickHandler;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu.MenuClickHandler;
import me.mrCookieSlime.Slimefun.api.inventory.DirtyChestMenu;

/**
 * An old {@link Listener} for CS-CoreLib
 * This is an old remnant of CS-CoreLib, the last bits of the past. They will be removed once everything is
 *             updated.
 */
public class MenuListener implements Listener {

    // ConcurrentHashMap: entries are (de)registered on the main thread, but an async
    // InventoryCloseEvent (e.g. a plugin closing an inventory off-thread) also reaches
    // onClose - a plain HashMap would corrupt under that concurrency.
    static final Map<UUID, ChestMenu> menus = new ConcurrentHashMap<>();

    public MenuListener(Plugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        ChestMenu menu = menus.remove(e.getPlayer().getUniqueId());

        if (menu != null) {
            menu.getMenuCloseHandler().onClose((Player) e.getPlayer());
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        ChestMenu menu = menus.get(e.getWhoClicked().getUniqueId());

        if (menu != null) {
            if (e.getRawSlot() < e.getInventory().getSize()) {
                MenuClickHandler handler = menu.getMenuClickHandler(e.getSlot());

                if (handler == null) {
                    e.setCancelled(!menu.isEmptySlotsClickable() && (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR));
                } else if (handler instanceof AdvancedMenuClickHandler) {
                    e.setCancelled(!((AdvancedMenuClickHandler) handler).onClick(e, (Player) e.getWhoClicked(), e.getSlot(), e.getCursor(), new ClickAction(e.isRightClick(), e.isShiftClick())));
                } else {
                    e.setCancelled(!handler.onClick((Player) e.getWhoClicked(), e.getSlot(), e.getCurrentItem(), new ClickAction(e.isRightClick(), e.isShiftClick())));
                }

                // Any click inside the menu that was not cancelled may modify its contents
                if (!e.isCancelled() && menu instanceof DirtyChestMenu dirtyMenu) {
                    dirtyMenu.markDirty();
                }
            } else {
                e.setCancelled(!menu.getPlayerInventoryClickHandler().onClick((Player) e.getWhoClicked(), e.getSlot(), e.getCurrentItem(), new ClickAction(e.isRightClick(), e.isShiftClick())));

                // Shift-clicking inside the Player inventory moves the item into the menu
                if (!e.isCancelled() && e.isShiftClick() && menu instanceof DirtyChestMenu dirtyMenu) {
                    dirtyMenu.markDirty();
                }
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        ChestMenu menu = menus.get(e.getWhoClicked().getUniqueId());

        if (menu == null) {
            return;
        }

        int topInventorySize = e.getView().getTopInventory().getSize();
        boolean touchesMenu = false;

        for (int rawSlot : e.getRawSlots()) {
            if (rawSlot < topInventorySize) {
                touchesMenu = true;

                /*
                 * Dragging is not an InventoryClickEvent, so the click handlers never
                 * see it. Mirror the click logic: slots with a registered handler are
                 * protected, handler-less slots follow isEmptySlotsClickable().
                 */
                if (menu.getMenuClickHandler(rawSlot) != null || !menu.isEmptySlotsClickable()) {
                    e.setCancelled(true);
                    return;
                }
            }
        }

        if (!touchesMenu && !menu.isPlayerInventoryClickable()) {
            e.setCancelled(true);
            return;
        }

        if (touchesMenu && menu instanceof DirtyChestMenu dirtyMenu) {
            dirtyMenu.markDirty();
        }
    }

}
