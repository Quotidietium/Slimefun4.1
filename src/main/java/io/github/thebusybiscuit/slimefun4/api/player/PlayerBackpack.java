package io.github.thebusybiscuit.slimefun4.api.player;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import io.github.bakedlibs.dough.config.Config;
import io.github.thebusybiscuit.slimefun4.api.events.BackpackResizeEvent;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.backpacks.SlimefunBackpack;
import io.github.thebusybiscuit.slimefun4.implementation.listeners.BackpackListener;

/**
 * This class represents the instance of a {@link SlimefunBackpack} that is ready to
 * be opened.
 * 
 * It holds an actual {@link Inventory} and represents the backpack on the
 * level of an individual {@link ItemStack} as opposed to the class {@link SlimefunBackpack}.
 * 
 * @author TheBusyBiscuit
 *
 * @see SlimefunBackpack
 * @see BackpackListener
 */
public class PlayerBackpack {

    private static final String CONFIG_PREFIX = "backpacks.";

    private final UUID ownerId;
    private final int id;

    @Deprecated
    private PlayerProfile profile;
    @Deprecated
    private Config cfg;
    private Inventory inventory;
    private int size;

    private PlayerBackpack(@Nonnull UUID ownerId, int id, int size) {
        this.ownerId = ownerId;
        this.id = id;
        this.size = size;
    }

    /**
     * This constructor loads an existing Backpack
     * 
     * @param profile
     *            The {@link PlayerProfile} of this Backpack
     * @param id
     *            The id of this Backpack
     *
     * @deprecated Use {@link PlayerBackpack#load(UUID, int, int, HashMap)} instead
     */
    @Deprecated
    public PlayerBackpack(@Nonnull PlayerProfile profile, int id) {
        this(profile, id, profile.getConfig().getInt(CONFIG_PREFIX + id + ".size"));

        for (int i = 0; i < size; i++) {
            inventory.setItem(i, cfg.getItem(CONFIG_PREFIX + id + ".contents." + i));
        }
    }

    /**
     * This constructor creates a new Backpack
     * 
     * @param profile
     *            The {@link PlayerProfile} of this Backpack
     * @param id
     *            The id of this Backpack
     * @param size
     *            The size of this Backpack
     *
     * @deprecated Use {@link PlayerBackpack#newBackpack(UUID, int, int)} instead
     */
    @Deprecated
    public PlayerBackpack(@Nonnull PlayerProfile profile, int id, int size) {
        if (size < 9 || size > 54 || size % 9 != 0) {
            throw new IllegalArgumentException("Invalid size! Size must be one of: [9, 18, 27, 36, 45, 54]");
        }

        this.ownerId = profile.getUUID();
        this.profile = profile;
        this.id = id;
        this.cfg = profile.getConfig();
        this.size = size;

        cfg.setValue(CONFIG_PREFIX + id + ".size", size);
        markDirty();

        inventory = Bukkit.createInventory(null, size, "背包 [" + size + " 格]");
    }

    /**
     * This returns the id of this {@link PlayerBackpack}
     * 
     * @return The id of this {@link PlayerBackpack}
     */
    public int getId() {
        return id;
    }

    /**
     * This method returns the {@link PlayerProfile} this {@link PlayerBackpack} belongs to
     * 
     * @return The owning {@link PlayerProfile}
     * 
     * @deprecated Use {@link PlayerBackpack#getOwnerId()} instead
     */
    @Deprecated
    @Nonnull
    public PlayerProfile getOwner() {
        return profile != null ? profile : PlayerProfile.find(Bukkit.getOfflinePlayer(ownerId)).orElse(null);
    }

    public UUID getOwnerId() {
        return this.ownerId;
    }

    /**
     * This returns the size of this {@link PlayerBackpack}.
     * 
     * @return The size of this {@link PlayerBackpack}
     */
    public int getSize() {
        return size;
    }

    /**
     * This method returns the {@link Inventory} of this {@link PlayerBackpack}
     * 
     * @return The {@link Inventory} of this {@link PlayerBackpack}
     */
    @Nonnull
    public Inventory getInventory() {
        return inventory;
    }

    /**
     * This will open the {@link Inventory} of this backpack to every {@link Player}
     * that was passed onto this method.
     * 
     * @param players
     *            The players who this Backpack will be shown to
     */
    public void open(Player... players) {
        Slimefun.runSync(() -> {
            for (Player p : players) {
                p.openInventory(inventory);
            }
        });
    }

    /**
     * This will close the {@link Inventory} of this backpack for every {@link Player}
     * that has opened it.
     * 
     * This process has to run on the main server thread.
     * 
     * @return A {@link CompletableFuture}.
     */
    public CompletableFuture<Void> closeForAll() {
        CompletableFuture<Void> future = new CompletableFuture<>();

        Slimefun.runSync(() -> {
            try {
                Iterator<HumanEntity> iterator = new ArrayList<>(inventory.getViewers()).iterator();

                while (iterator.hasNext()) {
                    iterator.next().closeInventory();
                }

                future.complete(null);
            } catch (Exception | LinkageError x) {
                // Never leave the future orphaned - chained follow-up actions would silently never run
                future.completeExceptionally(x);
            }
        });

        return future;
    }

    /**
     * This will change the current size of this Backpack to the specified size.
     *
     * @param size
     *            The new size for this Backpack
     *
     * @throws IllegalStateException
     *             When shrinking the Backpack would destroy items
     */
    public void setSize(int size) {
        if (size < 9 || size > 54 || size % 9 != 0) {
            throw new IllegalArgumentException("Invalid size! Size must be one of: [9, 18, 27, 36, 45, 54]");
        }

        if (size < this.inventory.getSize()) {
            // Refuse to shrink when that would destroy items beyond the new bounds
            for (int slot = size; slot < this.inventory.getSize(); slot++) {
                ItemStack item = this.inventory.getItem(slot);

                if (item != null && item.getType() != Material.AIR) {
                    throw new IllegalStateException("Cannot shrink this Backpack: Slots beyond the new size are not empty!");
                }
            }
        }

        if (BackpackResizeEvent.getHandlerList().getRegisteredListeners().length > 0) {
            BackpackResizeEvent event = new BackpackResizeEvent(this, this.size, size);
            Bukkit.getPluginManager().callEvent(event);

            if (event.isCancelled()) {
                // An addon vetoed this resize; the backpack keeps its current size and contents.
                return;
            }
        }

        Inventory inv = Bukkit.createInventory(null, size, "背包 [" + size + " 格]");

        /*
         * Copy at most as many slots as the new inventory offers - previously the
         * loop ran over the OLD inventory size, which threw (and left this Backpack
         * in a half-updated state) whenever the Backpack was shrunk.
         */
        for (int slot = 0; slot < Math.min(this.inventory.getSize(), size); slot++) {
            inv.setItem(slot, this.inventory.getItem(slot));
        }

        /*
         * Anyone currently viewing this backpack is looking at the OLD Inventory.
         * After the swap their edits would go into that discarded instance and be
         * lost on save - move every viewer onto the new Inventory instead.
         */
        List<HumanEntity> viewers = new ArrayList<>(this.inventory.getViewers());

        // Only swap the state over once the new inventory is fully populated
        this.size = size;
        this.inventory = inv;

        Slimefun.runSync(() -> {
            for (HumanEntity viewer : viewers) {
                viewer.closeInventory();
                viewer.openInventory(inv);
            }
        });

        markDirty();
    }

    /**
     * This method will save the contents of this backpack to a {@link File}.
     * 
     * @deprecated Handled by {@link PlayerProfile#save()} now
     */
    @Deprecated
    public void save() {
        for (int i = 0; i < size; i++) {
            cfg.setValue(CONFIG_PREFIX + id + ".contents." + i, inventory.getItem(i));
        }
    }

    /**
     * This method marks the backpack dirty, it will then be queued for an autosave
     * using {@link PlayerBackpack#save()}
     */
    public void markDirty() {
        if (profile != null) {
            profile.markDirty();
        }
    }

    private void setContents(int size, HashMap<Integer, ItemStack> contents) {
        if (this.inventory == null) {
            this.inventory = Bukkit.createInventory(null, size, "背包 [" + size + " 格]");
        }

        for (int i = 0; i < size; i++) {
            this.inventory.setItem(i, contents.get(i));
        }
    }

    @ParametersAreNonnullByDefault
    public static PlayerBackpack load(UUID ownerId, int id, int size, HashMap<Integer, ItemStack> contents) {
        if (size < 9 || size > 54 || size % 9 != 0) {
            /*
             * A corrupted size entry (e.g. a hand-edited or truncated config file)
             * would make Bukkit.createInventory() throw and drop the whole backpack.
             * Clamp to a valid inventory size instead, preserving as much as we can.
             */
            int clamped = Math.min(54, Math.max(9, ((size + 8) / 9) * 9));
            Slimefun.logger().log(Level.WARNING, "Backpack {0} of Player {1} has an invalid size of {2}, clamping it to {3}", new Object[] { id, ownerId, size, clamped });
            size = clamped;
        }

        PlayerBackpack backpack = new PlayerBackpack(ownerId, id, size);

        backpack.setContents(size, contents);

        return backpack;
    }

    @ParametersAreNonnullByDefault
    public static PlayerBackpack newBackpack(UUID ownerId, int id, int size) {
        if (size < 9 || size > 54 || size % 9 != 0) {
            throw new IllegalArgumentException("Invalid size! Size must be one of: [9, 18, 27, 36, 45, 54]");
        }

        PlayerBackpack backpack = new PlayerBackpack(ownerId, id, size);

        backpack.setContents(size, new HashMap<>());

        return backpack;
    }
}
