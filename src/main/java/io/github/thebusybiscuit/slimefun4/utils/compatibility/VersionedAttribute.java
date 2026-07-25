package io.github.thebusybiscuit.slimefun4.utils.compatibility;

import java.lang.reflect.Field;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bukkit.attribute.Attribute;

/**
 * Provides version-compatible {@link Attribute} constants.
 * <p>
 * Minecraft 1.21.2 removed the {@code generic.}, {@code player.} and {@code zombie.}
 * prefixes from all attribute resource locations (e.g. {@code minecraft:generic.max_health}
 * became {@code minecraft:max_health}). The Bukkit API followed suit and renamed the
 * {@link Attribute} constants accordingly: {@code GENERIC_MAX_HEALTH} (Minecraft ≤ 1.21.1)
 * was renamed to {@code MAX_HEALTH} (Minecraft ≥ 1.21.2), with the legacy constant being
 * removed.
 * <p>
 * Referencing the field name directly (e.g. {@code Attribute.GENERIC_MAX_HEALTH}) would
 * therefore throw a {@link NoSuchFieldError} at runtime when the plugin is run on the
 * "wrong" server version. To support both ≤ 1.21.1 and ≥ 1.21.2 servers from a single
 * build, the constant is resolved reflectively here, trying the new name first and
 * falling back to the legacy one.
 *
 * @see VersionedEnchantment
 * @see VersionedPotionType
 */
public class VersionedAttribute {

    /**
     * The "Max Health" {@link Attribute}.
     * Replaces the removed {@code Attribute.GENERIC_MAX_HEALTH} constant.
     */
    public static final Attribute MAX_HEALTH;

    static {
        // 1.21.2+ renamed the constant by removing the "GENERIC_" prefix.
        MAX_HEALTH = getKey("MAX_HEALTH", "GENERIC_MAX_HEALTH");
    }

    @Nullable
    private static Attribute getKey(@Nonnull String newName, @Nonnull String legacyName) {
        // Try the new (1.21.2+) constant name first, then fall back to the legacy one.
        Attribute attribute = getField(newName);

        if (attribute == null) {
            attribute = getField(legacyName);
        }

        return attribute;
    }

    @Nullable
    private static Attribute getField(@Nonnull String name) {
        try {
            Field field = Attribute.class.getField(name);
            return (Attribute) field.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return null;
        }
    }
}
