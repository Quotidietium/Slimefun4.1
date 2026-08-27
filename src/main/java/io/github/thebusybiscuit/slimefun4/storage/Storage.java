package io.github.thebusybiscuit.slimefun4.storage;

import java.util.Map;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import org.bukkit.inventory.ItemStack;

import com.google.common.annotations.Beta;

import io.github.thebusybiscuit.slimefun4.storage.data.PlayerData;

/**
 * The {@link Storage} interface is the abstract layer on top of our storage backends.
 * Every backend has to implement this interface and has to implement it in a thread-safe way.
 * There will be no expectation of running functions in here within the main thread.
 *
 * <p>
 * <b>This API is still experimental, it may change without notice.</b>
 */
@Beta
@ThreadSafe
public interface Storage {

    PlayerData loadPlayerData(UUID uuid);

    /**
     * Persists the given {@link PlayerData}.
     *
     * <p>
     * Backpack inventories are main-thread objects and must not be read from another
     * thread while a player could be editing them. {@code backpackSnapshots} carries
     * slot-by-slot copies of every backpack's contents, taken on the main thread by the
     * caller; backends serialize from the snapshot (keyed by backpack id) and only fall
     * back to reading the live inventory for ids that are absent from the map.
     * </p>
     *
     * @param uuid
     *            The {@link UUID} of the profile's owner
     * @param data
     *            The {@link PlayerData} to persist
     * @param backpackSnapshots
     *            Main-thread content snapshots per backpack id, see above
     */
    void savePlayerData(@Nonnull UUID uuid, @Nonnull PlayerData data, @Nullable Map<Integer, ItemStack[]> backpackSnapshots);
}
