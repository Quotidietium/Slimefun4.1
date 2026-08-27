package io.github.thebusybiscuit.slimefun4.implementation.items.elevator;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import io.github.bakedlibs.dough.common.ChatColors;
import io.github.bakedlibs.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun4.api.events.ElevatorFloorRenameEvent;
import io.github.thebusybiscuit.slimefun4.api.events.ElevatorTeleportEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockPlaceHandler;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockUseHandler;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.SimpleSlimefunItem;
import io.github.thebusybiscuit.slimefun4.utils.ChatUtils;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import io.papermc.lib.PaperLib;

import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu;
import me.mrCookieSlime.Slimefun.api.BlockStorage;

/**
 * The {@link ElevatorPlate} is a quick way of teleportation.
 * You can place multiple {@link ElevatorPlate ElevatorPlates} along the y axis
 * to teleport between them.
 *
 * @author TheBusyBiscuit
 * @author Walshy
 */
public class ElevatorPlate extends SimpleSlimefunItem<BlockUseHandler> {

    /**
     * This is our key for storing the floor name.
     */
    private static final String DATA_KEY = "floor";

    /**
     * This is the size of our {@link Inventory}.
     */
    private static final int GUI_SIZE = 27;

    /**
     * This is our {@link Set} of currently teleporting {@link Player Players}.
     * It is used to prevent them from triggering the {@link ElevatorPlate} they land on.
     */
    private final Set<UUID> users = new HashSet<>();

    @ParametersAreNonnullByDefault
    public ElevatorPlate(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe, ItemStack recipeOutput) {
        super(itemGroup, item, recipeType, recipe, recipeOutput);

        addItemHandler(onPlace());
    }

    private @Nonnull BlockPlaceHandler onPlace() {
        return new BlockPlaceHandler(false) {

            @Override
            public void onPlayerPlace(BlockPlaceEvent e) {
                Block b = e.getBlock();
                BlockStorage.addBlockInfo(b, DATA_KEY, ChatColor.WHITE + "楼层 0");
                BlockStorage.addBlockInfo(b, "owner", e.getPlayer().getUniqueId().toString());
            }
        };
    }

    @Override
    public @Nonnull BlockUseHandler getItemHandler() {
        return e -> {
            Block b = e.getClickedBlock().get();
            String owner = BlockStorage.getLocationInfo(b.getLocation(), "owner");

            if (owner != null && owner.equals(e.getPlayer().getUniqueId().toString())) {
                openEditor(e.getPlayer(), b);
            }
        };
    }

    public @Nonnull List<ElevatorFloor> getFloors(@Nonnull Block b) {
        LinkedList<ElevatorFloor> floors = new LinkedList<>();
        int index = 0;

        for (int y = b.getWorld().getMinHeight(); y < b.getWorld().getMaxHeight(); y++) {
            if (y == b.getY()) {
                String raw = BlockStorage.getLocationInfo(b.getLocation(), DATA_KEY);
                String name = ChatColors.color(raw != null ? raw : "楼层 " + index);
                floors.addFirst(new ElevatorFloor(name, index, b));
                index++;
                continue;
            }

            Block block = b.getWorld().getBlockAt(b.getX(), y, b.getZ());

            if (block.getType() == getItem().getType() && BlockStorage.check(block, getId())) {
                String raw = BlockStorage.getLocationInfo(block.getLocation(), DATA_KEY);
                String name = ChatColors.color(raw != null ? raw : "楼层 " + index);
                floors.addFirst(new ElevatorFloor(name, index, block));
                index++;
            }
        }

        return floors;
    }

    @ParametersAreNonnullByDefault
    public void openInterface(Player p, Block b) {
        if (users.remove(p.getUniqueId())) {
            return;
        }

        List<ElevatorFloor> floors = getFloors(b);

        if (floors.size() < 2) {
            Slimefun.getLocalization().sendMessage(p, "machines.ELEVATOR.no-destinations", true);
        } else {
            openFloorSelector(b, floors, p, 1);
        }
    }

    @ParametersAreNonnullByDefault
    private void openFloorSelector(Block b, List<ElevatorFloor> floors, Player p, int page) {
        ChestMenu menu = new ChestMenu(Slimefun.getLocalization().getMessage(p, "machines.ELEVATOR.pick-a-floor"));
        menu.setEmptySlotsClickable(false);

        int index = GUI_SIZE * (page - 1);

        for (int i = 0; i < Math.min(GUI_SIZE, floors.size() - index); i++) {
            ElevatorFloor floor = floors.get(index + i);

            // @formatter:off
            if (floor.getAltitude() == b.getY()) {
                menu.addItem(i, CustomItemStack.create(Material.COMPASS,
                    ChatColor.GRAY.toString() + floor.getNumber() + ". " + ChatColor.BLACK + floor.getName(),
                    Slimefun.getLocalization().getMessage(p, "machines.ELEVATOR.current-floor") + ' ' + ChatColor.WHITE + floor.getName()), ChestMenuUtils.getEmptyClickHandler());
            } else {
                menu.addItem(i, CustomItemStack.create(Material.PAPER,
                    ChatColor.GRAY.toString() + floor.getNumber() + ". " + ChatColor.BLACK + floor.getName(),
                    Slimefun.getLocalization().getMessage(p, "machines.ELEVATOR.click-to-teleport") + ' ' + ChatColor.WHITE + floor.getName()), (player, slot, itemStack, clickAction) -> {
                    teleport(player, floor);
                    return false;
                });
            }
            // @formatter:on
        }

        int pages = getPageCount(floors.size());

        // 0 index so size is the first slot of the last row.
        for (int i = GUI_SIZE; i < GUI_SIZE + 9; i++) {
            if (i == GUI_SIZE + 2 && pages > 1 && page != 1) {
                menu.addItem(i, ChestMenuUtils.getPreviousButton(p, page, pages), (player, i1, itemStack, clickAction) -> {
                    openFloorSelector(b, floors, p, page - 1);
                    return false;
                });
            } else if (i == GUI_SIZE + 6 && pages > 1 && page != pages) {
                menu.addItem(i, ChestMenuUtils.getNextButton(p, page, pages), (player, i1, itemStack, clickAction) -> {
                    openFloorSelector(b, floors, p, page + 1);
                    return false;
                });
            } else {
                menu.addItem(i, ChestMenuUtils.getBackground(), (player, i1, itemStack, clickAction) -> false);
            }
        }

        menu.open(p);
    }

    /**
     * Computes how many pages the floor selector needs. The old formula
     * {@code 1 + floors / GUI_SIZE} produced an extra, completely empty page
     * whenever the floor count was an exact multiple of the page size.
     *
     * @param floorCount
     *            The number of floors to display
     *
     * @return The number of pages, at least one
     */
    static int getPageCount(int floorCount) {
        return Math.max(1, (floorCount - 1) / GUI_SIZE + 1);
    }

    @ParametersAreNonnullByDefault
    private void teleport(Player player, ElevatorFloor floor) {
        /*
         * Fire an ElevatorTeleportEvent before the teleport sequence starts.
         * Cancellation leaves the floor selector open and never claims the
         * Player in #users, so they may simply pick another floor.
         */
        ElevatorTeleportEvent event = new ElevatorTeleportEvent(player, floor);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            return;
        }

        // An addon may have redirected the travel to another floor
        ElevatorFloor targetFloor = event.getFloor();

        Slimefun.runSync(() -> {
            users.add(player.getUniqueId());

            /*
             * Close the floor selector before teleporting: leaving it open would
             * allow teleporting again from anywhere without standing on a plate.
             */
            player.closeInventory();

            float yaw = player.getEyeLocation().getYaw() + 180;

            if (yaw > 180) {
                yaw = -180 + (yaw - 180);
            }

            Location loc = targetFloor.getLocation();
            Location destination = new Location(player.getWorld(), loc.getX() + 0.5, loc.getY() + 0.4, loc.getZ() + 0.5, yaw, player.getEyeLocation().getPitch());

            PaperLib.teleportAsync(player, destination).thenAccept(teleported -> {
                if (teleported.booleanValue()) {
                    player.sendTitle(ChatColor.WHITE + ChatColors.color(targetFloor.getName()), null, 20, 60, 20);
                } else {
                    /*
                     * The teleport failed, so the Player never lands on a plate
                     * that would consume their entry - remove it here, otherwise
                     * their next legitimate plate usage would be swallowed.
                     * This callback may run on any Thread, hence the runSync.
                     */
                    Slimefun.runSync(() -> users.remove(player.getUniqueId()));
                }
            });
        });
    }

    @ParametersAreNonnullByDefault
    public void openEditor(Player p, Block b) {
        ChestMenu menu = new ChestMenu(Slimefun.getLocalization().getMessage(p, "machines.ELEVATOR.editor-title"));

        String currentName = BlockStorage.getLocationInfo(b.getLocation(), DATA_KEY);
        menu.addItem(4, CustomItemStack.create(Material.NAME_TAG, "&7楼层名称 &e（点击编辑）", "", ChatColor.WHITE + ChatColors.color(currentName != null ? currentName : "楼层 0")));
        menu.addMenuClickHandler(4, (pl, slot, item, action) -> {
            pl.closeInventory();
            pl.sendMessage("");
            Slimefun.getLocalization().sendMessage(p, "machines.ELEVATOR.enter-name");
            pl.sendMessage("");

            ChatUtils.awaitInput(pl, message -> renameFloor(pl, b, message));

            return false;
        });

        menu.open(p);
    }

    /**
     * This renames the {@link ElevatorFloor} of the given {@link ElevatorPlate}
     * after the {@link Player} typed a name in the floor editor.
     * <p>
     * Package-private so that regression tests can drive the rename directly;
     * reaching it through the editor would require simulating the chat input
     * that {@link ChatUtils#awaitInput(Player, java.util.function.Consumer)}
     * listens for, which MockBukkit cannot provide.
     *
     * @param p
     *            The {@link Player} renaming the floor
     * @param b
     *            The {@link ElevatorPlate} {@link Block}
     * @param message
     *            The name the {@link Player} typed
     */
    @ParametersAreNonnullByDefault
    void renameFloor(Player p, Block b, String message) {
        String name = message;

        /*
         * The player typed the name asynchronously while the editor was open - the plate
         * may have been broken in the meantime. Writing the floor data regardless would
         * attach it to whatever block now occupies that spot (ghost BlockStorage data).
         */
        if (!BlockStorage.check(b, getId())) {
            return;
        }

        /*
         * The plate may also have been broken and re-placed by someone else in the
         * meantime: the editor was owner-gated when it opened, but the pending chat
         * input must not carry the rename over to the new owner's plate.
         */
        String owner = BlockStorage.getLocationInfo(b.getLocation(), "owner");

        if (!p.getUniqueId().toString().equals(owner)) {
            return;
        }

        if (ElevatorFloorRenameEvent.getHandlerList().getRegisteredListeners().length > 0) {
            ElevatorFloorRenameEvent event = new ElevatorFloorRenameEvent(p, b, BlockStorage.getLocationInfo(b.getLocation(), DATA_KEY), name);
            Bukkit.getPluginManager().callEvent(event);

            if (event.isCancelled()) {
                // An addon vetoed the rename; the floor keeps its old name and the editor stays closed.
                return;
            }

            name = event.getNewName();
        }

        BlockStorage.addBlockInfo(b, DATA_KEY, name.replace(ChatColor.COLOR_CHAR, '&'));

        String stored = name;
        p.sendMessage("");
        Slimefun.getLocalization().sendMessage(p, "machines.ELEVATOR.named", msg -> msg.replace("%floor%", stored));
        p.sendMessage("");

        openEditor(p, b);
    }

}
