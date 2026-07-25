package io.github.bakedlibs.dough.skins;

import java.net.URL;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

/**
 * A version-compatible replacement for dough's {@code CustomGameProfile}.
 * <p>
 * The original dough class extended {@code com.mojang.authlib.GameProfile}, but since
 * Minecraft 1.21.2 {@code GameProfile} is {@code final} and can no longer be subclassed,
 * which caused an {@link IncompatibleClassChangeError} on startup (see reason.md).
 * <p>
 * This replacement holds the same data (uuid, base64 texture, skin url) and builds
 * head/skull textures via the Bukkit {@link PlayerProfile} API instead, so no
 * {@code GameProfile} inheritance is needed. It is only accessed from within the
 * {@code io.github.bakedlibs.dough.skins} package (by {@link PlayerSkin} and {@link PlayerHead}).
 *
 * @see PlayerSkin
 * @see PlayerHead
 */
public class CustomGameProfile {

    /**
     * The player name for this profile.
     * "CS-CoreLib" for historical reasons and backwards compatibility.
     */
    private static final String PLAYER_NAME = "CS-CoreLib";

    private final UUID uuid;
    @Nullable
    private final String texture;
    private final URL skinUrl;

    CustomGameProfile(@Nonnull UUID uuid, @Nullable String texture, @Nonnull URL url) {
        this.uuid = uuid;
        this.texture = texture;
        this.skinUrl = url;
    }

    @Nonnull
    UUID getId() {
        return uuid;
    }

    @Nullable
    public String getBase64Texture() {
        return texture;
    }

    @Nonnull
    URL getSkinUrl() {
        return skinUrl;
    }

    /**
     * Builds a Bukkit {@link PlayerProfile} carrying this skin.
     *
     * @return A {@link PlayerProfile} with the skin texture applied.
     */
    @Nonnull
    PlayerProfile toPlayerProfile() {
        PlayerProfile profile = Bukkit.createPlayerProfile(uuid, PLAYER_NAME);
        PlayerTextures textures = profile.getTextures();
        textures.setSkin(skinUrl);
        profile.setTextures(textures);
        return profile;
    }

    /**
     * Applies this skin to the given {@link SkullMeta} (used for head {@link org.bukkit.inventory.ItemStack ItemStacks}).
     *
     * @param meta
     *            The {@link SkullMeta} to apply the skin to.
     */
    void apply(@Nonnull SkullMeta meta) {
        meta.setOwnerProfile(toPlayerProfile());
    }
}
