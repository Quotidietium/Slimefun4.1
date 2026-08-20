package io.github.thebusybiscuit.slimefun4.utils;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import javax.annotation.Nonnull;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.core.attributes.MachineTier;
import io.github.thebusybiscuit.slimefun4.core.attributes.MachineType;
import io.github.thebusybiscuit.slimefun4.core.attributes.Radioactivity;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;

/**
 * This utility class provides a few handy methods and constants to build the lore of any
 * {@link SlimefunItemStack}. It is mostly used directly inside the class {@link SlimefunItems}.
 * 
 * @author TheBusyBiscuit
 * 
 * @see SlimefunItems
 *
 */
public final class LoreBuilder {

    public static final String HAZMAT_SUIT_REQUIRED = "&8\u21E8 &4\u9700\u8981\u9632\u5316\u670D\uFF01";
    public static final String RAINBOW = "&d\u5FAA\u73AF\u663E\u793A\u5F69\u8679\u7684\u6240\u6709\u989C\u8272\uFF01";
    public static final String RIGHT_CLICK_TO_USE = "&e\u53F3\u952E&7\u4F7F\u7528";
    public static final String RIGHT_CLICK_TO_OPEN = "&e\u53F3\u952E&7\u6253\u5F00";
    public static final String CROUCH_TO_USE = "&e\u6F5C\u884C&7\u4F7F\u7528";

    private static final DecimalFormat hungerFormat = new DecimalFormat("#.0", DecimalFormatSymbols.getInstance(Locale.ROOT));

    private LoreBuilder() {}

    public static @Nonnull String radioactive(@Nonnull Radioactivity radioactivity) {
        return radioactivity.getLore();
    }

    public static @Nonnull String machine(@Nonnull MachineTier tier, @Nonnull MachineType type) {
        return tier + "" + type;
    }

    public static @Nonnull String speed(float speed) {
        return "&8\u21E8 &b\u26A1 &7\u901F\u5EA6\uFF1A &b" + speed + 'x';
    }

    public static @Nonnull String powerBuffer(int power) {
        return power(power, " \u7F13\u51B2");
    }

    public static @Nonnull String powerPerSecond(int power) {
        return power(power, "/\u79D2");
    }

    public static @Nonnull String power(int power, @Nonnull String suffix) {
        return "&8\u21E8 &e\u26A1 &7" + power + " J" + suffix;
    }

    public static @Nonnull String powerCharged(int charge, int capacity) {
        return "&8\u21E8 &e\u26A1 &7" + charge + " / " + capacity + " J";
    }

    public static @Nonnull String material(@Nonnull String material) {
        return "&8\u21E8 &7\u6750\u6599\uFF1A &b" + material;
    }

    public static @Nonnull String hunger(double value) {
        return "&7&o\u6062\u590D &b&o" + hungerFormat.format(value) + " &7&o\u9965\u997F\u503C";
    }

    public static @Nonnull String range(int blocks) {
        return "&7\u8303\u56F4\uFF1A &c" + blocks + " \u683C";
    }

    public static @Nonnull String usesLeft(int usesLeft) {
        return "&e" + usesLeft + " \u6B21 &7\u5269\u4F59";
    }

}
