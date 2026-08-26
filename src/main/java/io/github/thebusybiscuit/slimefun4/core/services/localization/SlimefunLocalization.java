package io.github.thebusybiscuit.slimefun4.core.services.localization;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang.Validate;
import org.bukkit.ChatColor;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import io.github.bakedlibs.dough.common.ChatColors;
import io.github.bakedlibs.dough.config.Config;
import io.github.bakedlibs.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun4.api.MinecraftVersion;
import io.github.thebusybiscuit.slimefun4.api.SlimefunBranch;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.services.LocalizationService;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.utils.ConfigUtils;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;

/**
 * This is an abstract parent class of {@link LocalizationService}.
 * There is not really much more I can say besides that...
 *
 * @author TheBusyBiscuit
 *
 * @see LocalizationService
 *
 */
public abstract class SlimefunLocalization implements Keyed {

    /**
     * The path (inside {@link LanguageFile#MESSAGES}) under which lore-phrase translations are stored.
     * Lore phrases are mapped from an English substring to its localized equivalent and are applied to
     * lore lines at display time. This allows us to translate the static text portion of <em>dynamic</em>
     * lore (e.g. the labels produced by {@code LoreBuilder}) while preserving any numbers, icons,
     * color codes and placeholders (such as {@code <Type>} or {@code <ID>}).
     */
    private static final String LORE_PHRASES_PATH = "lore-phrases";

    private final Config defaultConfig;

    /**
     * A per-{@link Language} cache of colorized lore phrases (English -> localized), sorted longest-first.
     */
    private final Map<Language, List<String[]>> lorePhraseCache = new ConcurrentHashMap<>();

    protected SlimefunLocalization(@Nonnull Slimefun plugin) {
        this.defaultConfig = new Config(plugin, "messages.yml");
    }

    protected @Nonnull Config getConfig() {
        return defaultConfig;
    }

    /**
     * Saves this Localization to its File
     */
    protected void save() {
        ConfigUtils.saveAtomically(defaultConfig);
    }

    /**
     * This returns the chat prefix for our messages.
     * Every message (unless explicitly omitted) will have this
     * prefix prepended.
     *
     * @return The chat prefix
     */
    public @Nonnull String getChatPrefix() {
        return getMessage("prefix");
    }

    /**
     * This method attempts to return the {@link Language} with the given
     * language code.
     *
     * @param id
     *            The language code
     *
     * @return A {@link Language} with the given id or null
     */

    public abstract @Nullable Language getLanguage(@Nonnull String id);

    /**
     * This method returns the currently selected {@link Language} of a {@link Player}.
     *
     * @param p
     *            The {@link Player} to query
     *
     * @return The {@link Language} that was selected by the given {@link Player}
     */

    public abstract @Nullable Language getLanguage(@Nonnull Player p);

    /**
     * This method returns the default {@link Language} of this {@link Server}
     *
     * @return The default {@link Language}
     */

    public abstract @Nullable Language getDefaultLanguage();

    /**
     * This returns whether a {@link Language} with the given id exists within
     * the project resources.
     *
     * @param id
     *            The {@link Language} id
     *
     * @return Whether the project contains a {@link Language} with that id
     */
    protected abstract boolean hasLanguage(@Nonnull String id);

    /**
     * This method returns a full {@link Collection} of every {@link Language} that was
     * found.
     *
     * @return A {@link Collection} that contains every installed {@link Language}
     */

    public abstract @Nonnull Collection<Language> getLanguages();

    /**
     * This method adds a new {@link Language} with the given id and texture.
     *
     * @param id
     *            The {@link Language} id
     * @param texture
     *            The texture of how this {@link Language} should be displayed
     */
    protected abstract void addLanguage(@Nonnull String id, @Nonnull String texture);

    /**
     * This will load every {@link LanguagePreset} into memory.
     * To be precise: It performs {@link #addLanguage(String, String)} for every
     * value of {@link LanguagePreset}.
     */
    protected void loadEmbeddedLanguages() {
        for (LanguagePreset lang : LanguagePreset.values()) {
            if (lang.isReadyForRelease() || Slimefun.getUpdater().getBranch() != SlimefunBranch.STABLE) {
                addLanguage(lang.getLanguageCode(), lang.getTexture());
            }
        }
    }

    private @Nonnull FileConfiguration getDefaultFile(@Nonnull LanguageFile file) {
        Language language = getLanguage(LanguagePreset.ENGLISH.getLanguageCode());

        if (language == null) {
            throw new IllegalStateException("Fallback language \"en\" is missing!");
        }

        FileConfiguration fallback = language.getFile(file);

        if (fallback != null) {
            return fallback;
        } else {
            throw new IllegalStateException("Fallback file: \"" + file.getFilePath("en") + "\" is missing!");
        }
    }

    @ParametersAreNonnullByDefault
    private @Nullable String getStringOrNull(@Nullable Language language, LanguageFile file, String path) {
        Validate.notNull(file, "You need to provide a LanguageFile!");
        Validate.notNull(path, "The path cannot be null!");

        if (language == null) {
            // Unit-Test scenario (or something went horribly wrong)
            return "Error: No language present";
        }

        FileConfiguration config = language.getFile(file);

        if (config != null) {
            String value = config.getString(path);

            // Return the found value (unless null)
            if (value != null) {
                return value;
            }
        }

        // Fallback to default configuration
        FileConfiguration defaults = getDefaultFile(file);
        String defaultValue = defaults.getString(path);

        // Return the default value or an error message
        return defaultValue != null ? defaultValue : null;
    }

    @ParametersAreNonnullByDefault
    private @Nonnull String getString(@Nullable Language language, LanguageFile file, String path) {
        String string = getStringOrNull(language, file, path);
        return string != null ? string : "! Missing string \"" + path + '"';
    }

    @ParametersAreNonnullByDefault
    private @Nullable List<String> getStringListOrNull(@Nullable Language language, LanguageFile file, String path) {
        Validate.notNull(file, "You need to provide a LanguageFile!");
        Validate.notNull(path, "The path cannot be null!");

        if (language == null) {
            // Unit-Test scenario (or something went horribly wrong)
            return Arrays.asList("Error: No language present");
        }

        FileConfiguration config = language.getFile(file);

        if (config != null) {
            List<String> value = config.getStringList(path);

            // Return the found value (unless empty)
            if (!value.isEmpty()) {
                return value;
            }
        }

        // Fallback to default configuration
        FileConfiguration defaults = getDefaultFile(file);
        List<String> defaultValue = defaults.getStringList(path);

        // Return the default value or an error message
        return !defaultValue.isEmpty() ? defaultValue : null;
    }

    @ParametersAreNonnullByDefault
    private @Nonnull List<String> getStringList(@Nullable Language language, LanguageFile file, String path) {
        List<String> list = getStringListOrNull(language, file, path);
        return list != null ? list : Arrays.asList("! Missing string \"" + path + '"');
    }

    public @Nonnull String getMessage(@Nonnull String key) {
        Validate.notNull(key, "Message key must not be null!");

        Language language = getDefaultLanguage();

        String message = language == null ? null : language.getFile(LanguageFile.MESSAGES).getString(key);

        if (message == null) {
            return getDefaultFile(LanguageFile.MESSAGES).getString(key);
        }

        return message;
    }

    public @Nonnull String getMessage(@Nonnull Player p, @Nonnull String key) {
        Validate.notNull(p, "Player must not be null!");
        Validate.notNull(key, "Message key must not be null!");

        return getString(getLanguage(p), LanguageFile.MESSAGES, key);
    }

    /**
     * Returns the Strings referring to the specified Key
     *
     * @param key
     *            The Key of those Messages
     * @return The List this key is referring to
     */
    public @Nonnull List<String> getDefaultMessages(@Nonnull String key) {
        return defaultConfig.getStringList(key);
    }

    public @Nonnull List<String> getMessages(@Nonnull Player p, @Nonnull String key) {
        Validate.notNull(p, "Player should not be null.");
        Validate.notNull(key, "Message key cannot be null.");

        return getStringList(getLanguage(p), LanguageFile.MESSAGES, key);
    }

    @ParametersAreNonnullByDefault
    public @Nonnull List<String> getMessages(Player p, String key, UnaryOperator<String> function) {
        Validate.notNull(p, "Player cannot be null.");
        Validate.notNull(key, "Message key cannot be null.");
        Validate.notNull(function, "Function cannot be null.");

        List<String> messages = getMessages(p, key);
        messages.replaceAll(function);

        return messages;
    }

    public @Nullable String getResearchName(@Nonnull Player p, @Nonnull NamespacedKey key) {
        Validate.notNull(p, "Player must not be null.");
        Validate.notNull(key, "NamespacedKey cannot be null.");

        return getStringOrNull(getLanguage(p), LanguageFile.RESEARCHES, key.getNamespace() + '.' + key.getKey());
    }

    public @Nullable String getItemGroupName(@Nonnull Player p, @Nonnull NamespacedKey key) {
        Validate.notNull(p, "Player must not be null.");
        Validate.notNull(key, "NamespacedKey cannot be null!");

        return getStringOrNull(getLanguage(p), LanguageFile.CATEGORIES, key.getNamespace() + '.' + key.getKey());
    }

    /**
     * This returns the localized name of the given {@link SlimefunItem} for the specified {@link Player}.
     * The lookup is performed in the {@link LanguageFile#ITEMS} file (e.g. {@code items.yml}),
     * keyed by {@code "<itemId>.name"}.
     *
     * If no translation is found (or the current language has no {@code items.yml}), this returns {@code null}
     * and the caller is expected to fall back to the default (hardcoded) item name.
     *
     * @param p
     *            The {@link Player} whose language should be used
     * @param item
     *            The {@link SlimefunItem} whose name to translate
     *
     * @return The localized item name or {@code null} if no translation exists
     */
    public @Nullable String getItemName(@Nonnull Player p, @Nonnull SlimefunItem item) {
        Validate.notNull(p, "Player must not be null!");
        Validate.notNull(item, "SlimefunItem must not be null!");

        return getStringOrNull(getLanguage(p), LanguageFile.ITEMS, item.getId() + ".name");
    }

    /**
     * This translates the given lore lines for the specified {@link Player} using the
     * {@link #LORE_PHRASES_PATH lore-phrase} table of the player's language.
     *
     * <p>
     * <b>This is safe for dynamic lore:</b> it only replaces known English substrings (the translatable
     * labels, e.g. the ones produced by {@code LoreBuilder}) and leaves everything else untouched.
     * Numbers, icons, color codes and placeholders (such as {@code <Type>} or {@code <ID>}) are preserved,
     * so the dynamic content (power values, speed, capacity, runtime-resolved placeholders) remains intact.
     * </p>
     *
     * <p>
     * Lines that contain no known phrase are returned unchanged.
     * </p>
     *
     * @param p
     *            The {@link Player} whose language should be used
     * @param lore
     *            The original (English) lore lines
     *
     * @return The translated lore lines (a new list)
     */
    @ParametersAreNonnullByDefault
    public @Nonnull List<String> translateLore(Player p, List<String> lore) {
        Validate.notNull(p, "Player must not be null!");
        Validate.notNull(lore, "Lore must not be null!");

        return translateLore(getLorePhrases(getLanguage(p)), lore);
    }

    /**
     * This applies the given (colorized) lore phrases to the given lore lines.
     *
     * Phrases are matched longest-first so that a more specific phrase (e.g. " J Buffer") is replaced
     * before a shorter prefix of it could interfere.
     *
     * @param phrases
     *            The colorized phrases, each a {@code String[]} of {@code {english, localized}}
     * @param lore
     *            The original lore lines
     *
     * @return A new list with all matching phrases replaced
     */
    private @Nonnull List<String> translateLore(@Nonnull List<String[]> phrases, @Nonnull List<String> lore) {
        if (phrases.isEmpty()) {
            return new ArrayList<>(lore);
        }

        List<String> result = new ArrayList<>(lore.size());

        for (String line : lore) {
            String translated = line;

            for (String[] phrase : phrases) {
                String english = phrase[0];

                if (translated.contains(english)) {
                    translated = translated.replace(english, phrase[1]);
                }
            }

            result.add(translated);
        }

        return result;
    }

    /**
     * This returns the list of colorized lore phrases (English -> localized) for the given {@link Language},
     * sorted longest-first. The result is cached per language.
     *
     * @param language
     *            The {@link Language} to load the phrases for
     *
     * @return The (possibly empty) list of phrases, each a {@code String[]} of {@code {english, localized}}
     */
    private @Nonnull List<String[]> getLorePhrases(@Nullable Language language) {
        if (language == null) {
            return Collections.emptyList();
        }

        List<String[]> cached = lorePhraseCache.get(language);

        if (cached != null) {
            return cached;
        }

        List<String[]> phrases = new ArrayList<>();
        FileConfiguration config = language.getFile(LanguageFile.MESSAGES);

        if (config != null && config.isConfigurationSection(LORE_PHRASES_PATH)) {
            ConfigurationSection section = config.getConfigurationSection(LORE_PHRASES_PATH);

            for (String key : section.getKeys(false)) {
                String value = section.getString(key);

                // Skip empty keys: String.replace("", value) would insert the value between
                // every character, corrupting the entire lore. Guard against misconfiguration.
                if (value != null && !key.isEmpty()) {
                    phrases.add(new String[] { ChatColors.color(key), ChatColors.color(value) });
                }
            }

            // Apply longer phrases first so that e.g. " J Buffer" is matched before " J".
            phrases.sort((a, b) -> Integer.compare(b[0].length(), a[0].length()));
        }

        lorePhraseCache.put(language, phrases);
        return phrases;
    }

    /**
     * This returns the display name of the given {@link SlimefunItem} for the specified {@link Player},
     * already colorized and ready to be shown.
     *
     * It uses the localized name from {@link LanguageFile#ITEMS} when available and otherwise falls back
     * to the default (hardcoded) item name.
     *
     * @param p
     *            The {@link Player} whose language should be used
     * @param item
     *            The {@link SlimefunItem} whose name to translate
     *
     * @return The (colorized) display name, localized if a translation exists
     */
    public @Nonnull String getDisplayName(@Nonnull Player p, @Nonnull SlimefunItem item) {
        Validate.notNull(p, "Player must not be null!");
        Validate.notNull(item, "SlimefunItem must not be null!");

        String localized = getItemName(p, item);
        return localized != null ? ChatColors.color(localized) : item.getItemName();
    }

    /**
     * This returns a {@link SlimefunItem}'s display {@link ItemStack} with its name (and lore) translated
     * to the {@link Player}'s selected language.
     *
     * <p>
     * The item <b>name</b> is translated via the per-item {@code items.yml} ({@code "<id>.name"}).
     * </p>
     * <p>
     * The <b>lore</b> is translated via the {@link #LORE_PHRASES_PATH lore-phrase} table, which only replaces
     * known English labels and thus preserves dynamic content (numbers, icons, color codes, placeholders
     * like {@code <Type>}/{@code <ID>}). This makes it safe for dynamic lore such as machine stats.
     * </p>
     * <p>
     * If no name translation exists and the language has no lore phrases, the original (hardcoded, English)
     * {@link ItemStack} is returned unchanged.
     * </p>
     *
     * @param p
     *            The {@link Player} whose language should be used
     * @param item
     *            The {@link SlimefunItem} to localize
     *
     * @return A localized display {@link ItemStack}, or the original if no translation is available
     */
    @ParametersAreNonnullByDefault
    public @Nonnull ItemStack getLocalizedItem(Player p, SlimefunItem item) {
        Validate.notNull(p, "Player must not be null!");
        Validate.notNull(item, "SlimefunItem must not be null!");

        String name = getItemName(p, item);
        List<String[]> lorePhrases = getLorePhrases(getLanguage(p));

        // No name translation and no lore phrases -> nothing to localize.
        if (name == null && lorePhrases.isEmpty()) {
            return item.getItem();
        }

        return CustomItemStack.create(item.getItem(), meta -> {
            if (name != null) {
                meta.setDisplayName(ChatColors.color(name));
            }

            if (!lorePhrases.isEmpty() && meta.hasLore()) {
                meta.setLore(translateLore(lorePhrases, meta.getLore()));
            }
        });
    }

    /**
     * This resolves the {@link SlimefunItem} behind the given {@link ItemStack} and returns a localized
     * display copy (see {@link #getLocalizedItem(Player, SlimefunItem)}). If the {@link ItemStack} is not
     * a Slimefun item, it is returned unchanged.
     *
     * @param p
     *            The {@link Player} whose language should be used
     * @param item
     *            The {@link ItemStack} to localize
     *
     * @return A localized display {@link ItemStack}, or the original if it is not a Slimefun item
     */
    @ParametersAreNonnullByDefault
    public @Nonnull ItemStack getLocalizedItem(Player p, ItemStack item) {
        Validate.notNull(p, "Player must not be null!");
        Validate.notNull(item, "ItemStack must not be null!");

        SlimefunItem sfItem = SlimefunItem.getByItem(item);

        if (sfItem == null) {
            return item;
        }

        return getLocalizedItem(p, sfItem);
    }

    public @Nullable String getResourceString(@Nonnull Player p, @Nonnull String key) {
        Validate.notNull(p, "Player should not be null!");
        Validate.notNull(key, "Message key should not be null!");

        return getStringOrNull(getLanguage(p), LanguageFile.RESOURCES, key);
    }

    public @Nonnull ItemStack getRecipeTypeItem(@Nonnull Player p, @Nonnull RecipeType recipeType) {
        Validate.notNull(p, "Player cannot be null!");
        Validate.notNull(recipeType, "Recipe type cannot be null!");

        ItemStack item = recipeType.toItem();

        if (item == null) {
            // Fixes #3088
            return new ItemStack(Material.AIR);
        }

        Language language = getLanguage(p);
        NamespacedKey key = recipeType.getKey();

        return CustomItemStack.create(item, meta -> {
            String displayName = getStringOrNull(language, LanguageFile.RECIPES, key.getNamespace() + "." + key.getKey() + ".name");

            // Set the display name if possible, else keep the default item name.
            if (displayName != null) {
                meta.setDisplayName(ChatColor.AQUA + displayName);
            }

            List<String> lore = getStringListOrNull(language, LanguageFile.RECIPES, key.getNamespace() + "." + key.getKey() + ".lore");

            // Set the lore if possible, else keep the default lore.
            if (lore != null) {
                lore.replaceAll(line -> ChatColor.GRAY + line);
                meta.setLore(lore);
            }

            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        });
    }

    public void sendMessage(@Nonnull CommandSender recipient, @Nonnull String key, boolean addPrefix) {
        Validate.notNull(recipient, "Recipient cannot be null!");
        Validate.notNull(key, "Message key cannot be null!");

        String prefix = addPrefix ? getChatPrefix() : "";

        if (recipient instanceof Player player) {
            recipient.sendMessage(ChatColors.color(prefix + getMessage(player, key)));
        } else {
            recipient.sendMessage(ChatColor.stripColor(ChatColors.color(prefix + getMessage(key))));
        }
    }

    public void sendActionbarMessage(@Nonnull Player player, @Nonnull String key, boolean addPrefix) {
        Validate.notNull(player, "Player cannot be null!");
        Validate.notNull(key, "Message key cannot be null!");

        String prefix = addPrefix ? getChatPrefix() : "";
        String message = ChatColors.color(prefix + getMessage(player, key));

        BaseComponent[] components = TextComponent.fromLegacyText(message);
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, components);
    }

    public void sendMessage(@Nonnull CommandSender recipient, @Nonnull String key) {
        sendMessage(recipient, key, true);
    }

    @ParametersAreNonnullByDefault
    public void sendMessage(CommandSender recipient, String key, UnaryOperator<String> function) {
        sendMessage(recipient, key, true, function);
    }

    @ParametersAreNonnullByDefault
    public void sendMessage(CommandSender recipient, String key, boolean addPrefix, UnaryOperator<String> function) {
        if (Slimefun.getMinecraftVersion() == MinecraftVersion.UNIT_TEST) {
            return;
        }

        String prefix = addPrefix ? getChatPrefix() : "";

        if (recipient instanceof Player player) {
            recipient.sendMessage(ChatColors.color(prefix + function.apply(getMessage(player, key))));
        } else {
            recipient.sendMessage(ChatColor.stripColor(ChatColors.color(prefix + function.apply(getMessage(key)))));
        }
    }

    public void sendMessages(@Nonnull CommandSender recipient, @Nonnull String key) {
        String prefix = getChatPrefix();

        if (recipient instanceof Player player) {
            for (String translation : getMessages(player, key)) {
                String message = ChatColors.color(prefix + translation);
                recipient.sendMessage(message);
            }
        } else {
            for (String translation : getDefaultMessages(key)) {
                String message = ChatColors.color(prefix + translation);
                recipient.sendMessage(ChatColor.stripColor(message));
            }
        }
    }

    @ParametersAreNonnullByDefault
    public void sendMessages(CommandSender recipient, String key, boolean addPrefix, UnaryOperator<String> function) {
        String prefix = addPrefix ? getChatPrefix() : "";

        if (recipient instanceof Player player) {
            for (String translation : getMessages(player, key)) {
                String message = ChatColors.color(prefix + function.apply(translation));
                recipient.sendMessage(message);
            }
        } else {
            for (String translation : getDefaultMessages(key)) {
                String message = ChatColors.color(prefix + function.apply(translation));
                recipient.sendMessage(ChatColor.stripColor(message));
            }
        }
    }

    @ParametersAreNonnullByDefault
    public void sendMessages(CommandSender recipient, String key, UnaryOperator<String> function) {
        sendMessages(recipient, key, true, function);
    }

    protected @Nonnull Set<String> getTotalKeys(@Nonnull Language lang) {
        return getKeys(lang.getFiles());
    }

    protected @Nonnull Set<String> getKeys(@Nonnull FileConfiguration... files) {
        Set<String> keys = new HashSet<>();

        for (FileConfiguration cfg : files) {
            keys.addAll(cfg.getKeys(true));
        }

        return keys;
    }
}
