package io.github.thebusybiscuit.slimefun4.core.services.localization;

import java.util.Arrays;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.bakedlibs.dough.common.ChatColors;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import io.github.bakedlibs.dough.data.persistent.PersistentDataAPI;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.core.services.LocalizationService;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * This tests the per-item localization added for translating item names (and lore)
 * in the Slimefun guide based on the player's selected language.
 *
 * @see SlimefunLocalization#getItemName(org.bukkit.entity.Player, SlimefunItem)
 */
class TestItemLocalization {

    private static ServerMock server;
    private static Slimefun plugin;
    private static LocalizationService localization;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);

        // Construct a LocalizationService with a known default language ("en").
        // This is what triggers all embedded languages (including zh-CN) to be
        // loaded from the resources folder, which the unit-test startup skips.
        localization = new LocalizationService(plugin, "", "en");
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("Test that a zh-CN player gets the localized item name")
    void testChineseItemName() {
        Player player = new PlayerMock(server, "ChinesePlayer");

        // Select Chinese (China) for this player.
        PersistentDataAPI.setString(player, localization.getKey(), "zh-CN");

        // PORTABLE_CRAFTER has a zh-CN translation in items.yml
        SlimefunItem item = TestUtilities.mockSlimefunItem(plugin, "PORTABLE_CRAFTER", new ItemStack(Material.PAPER));
        Assertions.assertNotNull(item);

        String name = localization.getItemName(player, item);

        // The value in items.yml includes the color-code prefix (e.g. "&6便携式合成台").
        Assertions.assertNotNull(name, "A zh-CN translation for PORTABLE_CRAFTER should exist");
        Assertions.assertTrue(name.contains("便携式合成台"), "Item name should be translated to Chinese, but was: " + name);

        // The display-name helper should return the colorized, localized name.
        String displayName = localization.getDisplayName(player, item);
        Assertions.assertTrue(displayName.contains("便携式合成台"), "Display name should contain the Chinese translation, but was: " + displayName);
    }

    @Test
    @DisplayName("Test that an English player has no items.yml override (falls back to hardcoded name)")
    void testEnglishFallback() {
        Player player = new PlayerMock(server, "EnglishPlayer");
        SlimefunItem item = TestUtilities.mockSlimefunItem(plugin, "PORTABLE_CRAFTER", new ItemStack(Material.PAPER));

        // There is no en/items.yml, so the lookup must return null so the caller
        // falls back to the hardcoded English item name.
        Assertions.assertNull(localization.getItemName(player, item));

        // getDisplayName must therefore return the (English) hardcoded name unchanged.
        Assertions.assertEquals(item.getItemName(), localization.getDisplayName(player, item));
    }

    @Test
    @DisplayName("Test that dynamic lore labels are translated while numbers and icons are preserved")
    void testDynamicLoreTranslation() {
        Player player = new PlayerMock(server, "ChinesePlayer");
        PersistentDataAPI.setString(player, localization.getKey(), "zh-CN");

        // Simulate a LoreBuilder.powerBuffer(4) line (already colorized, as it is on a real ItemStack).
        List<String> lore = Arrays.asList(ChatColors.color("&8⇨ &e⚡ &74 J Buffer"));

        List<String> translated = localization.translateLore(player, lore);
        Assertions.assertEquals(1, translated.size());

        String line = translated.get(0);
        // The label "Buffer" must be translated...
        Assertions.assertTrue(line.contains("缓冲"), "Lore label should be translated, but was: " + line);
        // ...but the dynamic number and the icon must be preserved.
        Assertions.assertTrue(line.contains("4"), "The dynamic number should be preserved, but was: " + line);
        Assertions.assertTrue(line.contains("⚡"), "The icon should be preserved, but was: " + line);
    }

    @Test
    @DisplayName("Test that placeholders like <Type> are preserved when translating lore")
    void testPlaceholderPreserved() {
        Player player = new PlayerMock(server, "ChinesePlayer");
        PersistentDataAPI.setString(player, localization.getKey(), "zh-CN");

        // A Broken Spawner-style line that contains the <Type> placeholder.
        List<String> lore = Arrays.asList(ChatColors.color("&7Type: &b<Type>"));

        List<String> translated = localization.translateLore(player, lore);
        Assertions.assertEquals(1, translated.size());
        Assertions.assertTrue(translated.get(0).contains("<Type>"), "Placeholders must be preserved, but was: " + translated.get(0));
    }

    @Test
    @DisplayName("Test that an English player's lore is left untouched")
    void testEnglishLoreUntouched() {
        Player player = new PlayerMock(server, "EnglishPlayer");
        List<String> lore = Arrays.asList(ChatColors.color("&8⇨ &e⚡ &74 J Buffer"));

        List<String> translated = localization.translateLore(player, lore);
        Assertions.assertEquals(lore, translated);
    }
}
