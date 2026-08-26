package me.mrCookieSlime.Slimefun.api.inventory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import io.github.bakedlibs.dough.config.Config;
import io.github.bakedlibs.dough.inventory.InvUtils;
import io.github.bakedlibs.dough.items.CustomItemStack;
import io.github.bakedlibs.dough.items.ItemUtils;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.utils.itemstack.ItemStackWrapper;

import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu;

// This class will be deprecated, relocated and rewritten in a future version.
public class DirtyChestMenu extends ChestMenu {

    protected final BlockMenuPreset preset;

    /**
     * Thread-safe: menus are marked dirty on the main Thread while the
     * auto-save reads and clears the counter from its asynchronous Thread.
     */
    protected final AtomicInteger changes = new AtomicInteger(1);

    public DirtyChestMenu(@Nonnull BlockMenuPreset preset) {
        super(preset.getTitle());

        this.preset = preset;
    }

    /**
     * This method checks whether this {@link DirtyChestMenu} is currently viewed by a {@link Player}.
     *
     * @return Whether anyone is currently viewing this {@link Inventory}
     */
    public boolean hasViewer() {
        Inventory inv = toInventory();
        return inv != null && !inv.getViewers().isEmpty();
    }

    public void markDirty() {
        changes.incrementAndGet();
    }

    public boolean isDirty() {
        return changes.get() > 0;
    }

    public int getUnsavedChanges() {
        return changes.get();
    }

    @Nonnull
    public BlockMenuPreset getPreset() {
        return preset;
    }

    public boolean canOpen(Block b, Player p) {
        return preset.canOpen(b, p);
    }

    @Override
    public void open(Player... players) {
        super.open(players);

        // The Inventory will likely be modified soon
        markDirty();
    }

    public void close() {
        for (HumanEntity human : new ArrayList<>(toInventory().getViewers())) {
            human.closeInventory();
        }
    }

    public boolean fits(@Nonnull ItemStack item, int... slots) {
        for (int slot : slots) {
            // A small optimization for empty slots
            if (getItemInSlot(slot) == null) {
                return true;
            }
        }

        return InvUtils.fits(toInventory(), ItemStackWrapper.wrap(item), slots);
    }

    /**
     * Adds given {@link ItemStack} to any of the given inventory slots.
     * Items will be added to the inventory slots based on their order in the function argument.
     * Items will be added either to any empty inventory slots or any partially filled slots, in which case
     * as many items as can fit will be added to that specific spot.
     *
     * @param item
     *            {@link ItemStack} to be added to the inventory
     * @param slots
     *            Numbers of slots to add the {@link ItemStack} to
     * @return {@link ItemStack} with any items that did not fit into the inventory
     *         or null when everything had fit
     */
    // Synchronized: see consumeItem(int, int, boolean) for the lost-update rationale
    @Nullable
    public synchronized ItemStack pushItem(ItemStack item, int... slots) {
        if (item == null || item.getType() == Material.AIR) {
            throw new IllegalArgumentException("Cannot push null or AIR");
        }

        ItemStackWrapper wrapper = null;
        int amount = item.getAmount();

        for (int slot : slots) {
            if (amount <= 0) {
                break;
            }

            ItemStack stack = getItemInSlot(slot);

            if (stack == null) {
                replaceExistingItem(slot, item);
                return null;
            } else {
                int maxStackSize = Math.min(stack.getMaxStackSize(), toInventory().getMaxStackSize());
                if (stack.getAmount() < maxStackSize) {
                    if (wrapper == null) {
                        wrapper = ItemStackWrapper.wrap(item);
                    }

                    if (ItemUtils.canStack(wrapper, stack)) {
                        // Only subtract what was actually merged - otherwise the input
                        // stack could be mutated to a negative amount
                        int added = Math.min(amount, maxStackSize - stack.getAmount());
                        stack.setAmount(stack.getAmount() + added);
                        amount -= added;
                        item.setAmount(amount);

                        // The live stack was mutated in place (no replaceExistingItem),
                        // so the change counter must be bumped here - a menu whose only
                        // change was a merge would otherwise never be re-saved and the
                        // merged items would be lost on restart (BlockMenu#save skips
                        // clean menus).
                        markDirty();
                    }
                }
            }
        }

        if (amount > 0) {
            return CustomItemStack.create(item, amount);
        } else {
            return null;
        }
    }

    public void consumeItem(int slot) {
        consumeItem(slot, 1);
    }

    public void consumeItem(int slot, int amount) {
        consumeItem(slot, amount, false);
    }

    /*
     * Synchronized (menu monitor): the read-slot / mutate-amount / write-back sequence
     * is a lost-update window when the async machine ticker pushes while the main
     * thread (cargo / player interactions) pushes or consumes the same menu - an
     * 8-thread probe measured silently voided items without this lock. The
     * BlockMenuPreset#onItemStackChange callback runs under the monitor by design;
     * its default implementation is pure, addons must not re-enter OTHER menus from
     * within it.
     */
    public synchronized void consumeItem(int slot, int amount, boolean replaceConsumables) {
        ItemUtils.consumeItem(getItemInSlot(slot), amount, replaceConsumables);
        markDirty();
    }

    @Override
    public void replaceExistingItem(int slot, ItemStack item) {
        replaceExistingItem(slot, item, true);
    }

    public void replaceExistingItem(int slot, ItemStack item, boolean event) {
        if (event) {
            ItemStack previous = getItemInSlot(slot);
            item = preset.onItemStackChange(this, slot, previous, item);
        }

        super.replaceExistingItem(slot, item);
        markDirty();
    }

    /**
     * Writes a {@link Config} to disk via a temporary file and an atomic move
     * (with a plain replace as fallback for filesystems that do not support
     * atomic moves), so a crash mid-write cannot corrupt the previous state.
     *
     * @param config
     *            The {@link Config} to write
     *
     * @return Whether the file was successfully written
     */
    protected static boolean saveAtomically(@Nonnull Config config) {
        File target = config.getFile();
        File tmpFile = new File(target.getParentFile(), target.getName() + ".tmp");

        try {
            /*
             * The parent directory may be missing (external cleanup of data-storage
             * mid-session): Files.write does not create parents, and a missing parent
             * would make every save of this inventory file fail forever. See the same
             * fix in LegacyStorage#saveAtomically.
             */
            Files.createDirectories(target.getParentFile().toPath());

            /*
             * Write via Files.write instead of Config#save: Config#save swallows any
             * IOException, so a half-written tmp (disk full, I/O error) would pass the
             * exists() check below and atomically replace a perfectly good file with a
             * truncated one. On failure the partial tmp is deleted before reporting it.
             */
            Files.write(tmpFile.toPath(), config.getConfiguration().saveToString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (IOException x) {
            try {
                Files.deleteIfExists(tmpFile.toPath());
            } catch (IOException ignored) {
                // The tmp is already gone or undeletable - either way there is nothing to move
            }

            Slimefun.logger().log(Level.SEVERE, x, () -> "Could not write a temporary file for \"" + target.getName() + "\" (disk full?), will retry on the next save cycle");
            return false;
        }

        if (!tmpFile.exists()) {
            // Defensive: the write reported success but there is nothing to move
            Slimefun.logger().log(Level.SEVERE, "Could not write a temporary file for \"{0}\", will retry on the next save cycle", target.getName());
            return false;
        }

        try {
            Files.move(tmpFile.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE);
            return true;
        } catch (IOException x) {
            try {
                // Some filesystems do not support atomic moves - fall back to a plain replace
                Files.move(tmpFile.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                return true;
            } catch (IOException x2) {
                Slimefun.logger().log(Level.SEVERE, x2, () -> "An Error occurred while saving inventory data to \"" + target.getName() + "\", will retry on the next save cycle");
                return false;
            }
        }
    }

}
