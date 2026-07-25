package io.github.bakedlibs.dough.skins;

import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Skull;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;

/**
 * Version-compatible replacement for dough's {@code PlayerHead}.
 * <p>
 * Uses the {@code PlayerProfile} APIs for both head items and head blocks, so no
 * {@code GameProfile}/NMS reflection is required. This avoids the
 * {@code IncompatibleClassChangeError} caused by {@code GameProfile} being {@code final}
 * since Minecraft 1.21.2.
 * <ul>
 *   <li>Head {@link ItemStack}s use {@link SkullMeta#setOwnerProfile(org.bukkit.profile.PlayerProfile)}.</li>
 *   <li>Head {@link Block}s use {@link Skull#setPlayerProfile(PlayerProfile)} (Paper profile).</li>
 * </ul>
 *
 * @see PlayerSkin
 */
public final class PlayerHead {

    /**
     * The player name for head profiles.
     * "CS-CoreLib" for historical reasons and backwards compatibility.
     */
    private static final String PLAYER_NAME = "CS-CoreLib";

    private PlayerHead() {}

    /**
     * Returns a {@link ItemStack} for the specified {@link OfflinePlayer}'s head.
     *
     * @param player
     *            The owner of the head.
     *
     * @return A new head {@link ItemStack} for the specified player.
     */
    @ParametersAreNonnullByDefault
    public static @Nonnull ItemStack getItemStack(OfflinePlayer player) {
        Validate.notNull(player, "The player cannot be null!");

        return getItemStack(meta -> meta.setOwningPlayer(player));
    }

    /**
     * Returns a {@link ItemStack} for the specified {@link PlayerSkin}.
     *
     * @param skin
     *            The skin of the head.
     *
     * @return A new head {@link ItemStack} for the specified skin.
     */
    @ParametersAreNonnullByDefault
    public static @Nonnull ItemStack getItemStack(PlayerSkin skin) {
        Validate.notNull(skin, "The skin cannot be null!");

        return getItemStack(meta -> skin.getProfile().apply(meta));
    }

    private static @Nonnull ItemStack getItemStack(@Nonnull Consumer<SkullMeta> consumer) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();

        if (meta != null) {
            consumer.accept(meta);
            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Applies the given {@link PlayerSkin} to a player-head {@link Block}.
     *
     * @param block
     *            The head {@link Block} to update.
     * @param skin
     *            The {@link PlayerSkin} to apply.
     * @param sendBlockUpdate
     *            Whether to send a block update to the clients.
     */
    @ParametersAreNonnullByDefault
    public static void setSkin(Block block, PlayerSkin skin, boolean sendBlockUpdate) {
        Validate.notNull(block, "The block cannot be null!");
        Validate.notNull(skin, "The skin cannot be null!");

        Material material = block.getType();

        if (material != Material.PLAYER_HEAD && material != Material.PLAYER_WALL_HEAD) {
            throw new IllegalArgumentException("Cannot update a head texture. Expected a Player Head, received: " + material);
        }

        BlockState state = block.getState();

        if (state instanceof Skull) {
            Skull skull = (Skull) state;
            CustomGameProfile profile = skin.getProfile();

            // Skull#setPlayerProfile expects a Paper (com.destroystokyo.paper.profile) PlayerProfile.
            PlayerProfile paperProfile = Bukkit.createProfile(profile.getId(), PLAYER_NAME);
            String texture = profile.getBase64Texture();

            if (texture != null) {
                paperProfile.setProperty(new ProfileProperty("textures", texture));
            }

            skull.setPlayerProfile(paperProfile);
            state.update(true, sendBlockUpdate);
        }
    }

}
