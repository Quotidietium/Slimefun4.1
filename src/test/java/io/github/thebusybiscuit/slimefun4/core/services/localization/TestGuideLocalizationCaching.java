package io.github.thebusybiscuit.slimefun4.core.services.localization;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
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

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import io.github.bakedlibs.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.core.services.LocalizationService;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Discriminant coverage for the round-11 localization read-path memoization:
 * the read-through string cache ({@code getStringOrNull}), the per-language
 * lore-line memo inside {@code translateLore} and the explicit
 * {@link SlimefunLocalization#invalidateTranslationCaches()} contract.
 *
 * <p>
 * The unit-test environment boots the {@link LocalizationService} without a
 * server language, so this suite installs the bundled zh-CN bundle (plus the
 * "en" fallback) through the production {@code addLanguage} loader first -
 * the same deployment shape the fork ships.
 * </p>
 *
 * @author Zurker
 */
class TestGuideLocalizationCaching {

    private static ServerMock server;
    private static Slimefun plugin;
    private static SlimefunLocalization localization;

    @BeforeAll
    public static void load() throws Exception {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);

        localization = Slimefun.getLocalization();
        injectLanguages();
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("translateLore: memoized lines stay correct, return fresh lists and survive invalidation")
    void testTranslateLoreMemo() {
        Player player = server.addPlayer();

        List<String> lore = Arrays.asList("&8\u21E8 &7Speed: &b1", "&8\u21E8 &e256 J Buffer", "&7untranslatable line");

        List<String> first = localization.translateLore(player, lore);
        Assertions.assertTrue(first.get(0).contains("速度： "), "The known phrase 'Speed: ' must be translated");
        Assertions.assertTrue(first.get(1).contains(" J 缓冲"), "The known phrase ' J Buffer' must be translated");
        Assertions.assertEquals("&7untranslatable line", first.get(2), "Lines without phrases stay unchanged");

        // Second call goes through the line memo and must be identical...
        List<String> second = localization.translateLore(player, lore);
        Assertions.assertEquals(first, second);

        // ...but the returned list is fresh every time: mutating it must not
        // poison the memo.
        second.set(0, "corrupted");
        List<String> third = localization.translateLore(player, lore);
        Assertions.assertEquals(first.get(0), third.get(0));

        // After invalidation the result is recomputed and stays correct.
        localization.invalidateTranslationCaches();
        List<String> fourth = localization.translateLore(player, lore);
        Assertions.assertEquals(first, fourth);
    }

    @Test
    @DisplayName("String lookups are memoized: out-of-band config changes need invalidateTranslationCaches()")
    void testOutBandContract() {
        Player player = server.addPlayer();
        String key = "guide.memo-contract-test";

        localization.getDefaultLanguage().getFile(LanguageFile.MESSAGES).set(key, "A");
        Assertions.assertEquals("A", localization.getMessage(player, key));

        // Swapping the value behind the memo's back is the documented
        // out-of-band contract: the memoized read stays stable...
        localization.getDefaultLanguage().getFile(LanguageFile.MESSAGES).set(key, "B");
        Assertions.assertEquals("A", localization.getMessage(player, key));

        // ...until the caches are explicitly dropped.
        localization.invalidateTranslationCaches();
        Assertions.assertEquals("B", localization.getMessage(player, key));
    }

    @Test
    @DisplayName("Memo keys are scoped per LanguageFile: an item name and a message may share a path")
    void testFileScopedKeys() {
        Player player = server.addPlayer();
        String path = "MEMO_TEST_ITEM.name";

        localization.getDefaultLanguage().getFile(LanguageFile.ITEMS).set(path, "物品名");
        localization.getDefaultLanguage().getFile(LanguageFile.MESSAGES).set(path, "消息名");

        SlimefunItem item = TestUtilities.mockSlimefunItem(plugin, "MEMO_TEST_ITEM", CustomItemStack.create(Material.PAPER, "&f测试"));

        Assertions.assertEquals("物品名", localization.getItemName(player, item));
        Assertions.assertEquals("消息名", localization.getMessage(player, path));

        localization.invalidateTranslationCaches();
    }

    @Test
    @DisplayName("getLocalizedItem still returns a fresh clone per call with translated lore")
    void testLocalizedItemCloneSemantics() {
        Player player = server.addPlayer();

        SlimefunItem item = TestUtilities.mockSlimefunItem(plugin, "MEMO_CLONE_ITEM", CustomItemStack.create(Material.PAPER, "&f克隆测试", "&8\u21E8 &7Speed: &b1"));

        ItemStack first = localization.getLocalizedItem(player, item);
        ItemStack second = localization.getLocalizedItem(player, item);

        Assertions.assertNotSame(first, second, "Every localized display item must be a fresh instance");
        Assertions.assertEquals(first.getType(), second.getType());
        Assertions.assertNotNull(first.getItemMeta().getLore());
        Assertions.assertTrue(first.getItemMeta().getLore().get(0).contains("速度： "), "The lore must be translated in the display clone");
    }

    /**
     * Loads the bundled zh-CN bundle (plus the "en" fallback) through the
     * production {@code addLanguage} loader and installs zh-CN as the default
     * language - reflection because both hooks are protected/private.
     */
    private static void injectLanguages() throws Exception {
        LocalizationService service = (LocalizationService) localization;

        Method addLanguage = SlimefunLocalization.class.getDeclaredMethod("addLanguage", String.class, String.class);
        addLanguage.setAccessible(true);
        addLanguage.invoke(service, "en", "420b152778e7e2dce6f5e58d3e34ae4d1c3e5a6e4b6d3f4a5e6b7c8d9e0f1a2");
        addLanguage.invoke(service, "zh-CN", "7f9bc035cdc80f1ab5e1198f29f3ad3fdd2b42d9a69aeb64de990681800b98dc");

        Field defaultField = LocalizationService.class.getDeclaredField("defaultLanguage");
        defaultField.setAccessible(true);

        Method getLanguage = LocalizationService.class.getDeclaredMethod("getLanguage", String.class);
        getLanguage.setAccessible(true);
        Language zh = (Language) getLanguage.invoke(service, "zh-CN");
        defaultField.set(service, zh);
    }
}
