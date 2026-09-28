package benchmark.scenarios;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import benchmark.Bench;
import benchmark.BenchContext;
import benchmark.Results;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.guide.SlimefunGuideImplementation;
import io.github.thebusybiscuit.slimefun4.core.guide.SlimefunGuideMode;
import io.github.thebusybiscuit.slimefun4.core.services.LocalizationService;
import io.github.thebusybiscuit.slimefun4.core.services.localization.Language;
import io.github.thebusybiscuit.slimefun4.core.services.localization.SlimefunLocalization;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.storage.data.PlayerData;

/**
 * Measures the survival guide rendering path: a full category page open
 * ({@code openItemGroup} - the dominant per-click main-thread action, 36
 * displayed items built through the localization layer) and the micro
 * anchors of its localization reads: {@code getLocalizedItem} (name lookup
 * + clone + lore translation), {@code translateLore} (the phrase-replacement
 * scan) and {@code getMessage} (the per-button config walk).
 *
 * <p>
 * The lore lines are LoreBuilder-shaped English text so that every line hits
 * several of the bundled lore phrases - on a translated deployment (this
 * fork defaults to zh-CN) each displayed item pays the full phrase scan per
 * open.
 * </p>
 */
public final class GuideRenderBench {

    private static final int ITEM_COUNT = 36;
    private static final int WARMUP = 3;
    private static final int ROUNDS = 9;
    private static final int OPENS = 400;
    private static final int LOCALIZED_CALLS = 20_000;
    private static final int LORE_CALLS = 50_000;
    private static final int MESSAGE_CALLS = 100_000;

    /**
     * LoreBuilder-shaped lines: several known English labels per line so the
     * phrase replacement does real work on every line.
     */
    private static final String[] LORE_LINES = { "&8\u21E8 &7Speed: &b1", "&8\u21E8 &e256 J Buffer", "&8\u21E8 &e16 J/s", "&8\u21E8 &7Material: &bMulti-Block", "&8\u21E8 &7Range: &b3 blocks", "&7Basic Machine" };

    public void run(BenchContext ctx, Results results) throws Exception {
        SlimefunLocalization localization = Slimefun.getLocalization();
        Player player = ctx.server().addPlayer("bench_guide_user");

        injectLanguages(localization);
        results.note("guide-render default language: " + (localization.getDefaultLanguage() != null ? localization.getDefaultLanguage().getId() : "none"));

        // Without this the bench world counts as disabled and the guide page
        // would render empty (isDisabledIn skips every display item).
        Slimefun.getWorldSettingsService().setEnabled(player.getWorld(), true);

        ItemGroup group = new ItemGroup(new NamespacedKey(ctx.plugin(), "guide_bench_group"), new ItemStack(Material.CHEST));
        List<SlimefunItem> items = new ArrayList<>(ITEM_COUNT);

        for (int i = 0; i < ITEM_COUNT; i++) {
            SlimefunItemStack stack = new SlimefunItemStack("BENCH_GITEM_" + i, Material.PAPER, "Bench Guide Item " + i, LORE_LINES);
            SlimefunItem item = new SlimefunItem(group, stack, RecipeType.NULL, new ItemStack[9]);
            item.register(Slimefun.instance());
            // Items registered after startup never went through the load pass,
            // so the group (and thus the guide page) would stay empty.
            item.load();
            items.add(item);
        }

        PlayerData data = new PlayerData(new HashSet<>(), new HashMap<>(), new HashSet<>());
        PlayerProfile profile = newProfile(player, data);

        SlimefunGuideImplementation guide = Slimefun.getRegistry().getSlimefunGuide(SlimefunGuideMode.SURVIVAL_MODE);

        categoryOpen(results, guide, group, profile);
        Bench.gcSettle();
        localizedItem(results, localization, player, items);
        Bench.gcSettle();
        itemNameLookup(results, localization, player, items);
        Bench.gcSettle();
        itemClone(results, items);
        Bench.gcSettle();
        loreTranslate(results, localization, player);
        Bench.gcSettle();
        messageLookup(results, localization, player);
    }

    /**
     * The unit-test environment boots the {@link LocalizationService} without a
     * server language (translations disabled, no embedded language loaded), so
     * every localization read would take the "no language" constant path.
     * This loads the real bundled zh-CN bundle (plus "en" as fallback) through
     * the production {@code addLanguage} loader and installs zh-CN as the
     * default - the deployment shape this fork ships.
     */
    private void injectLanguages(SlimefunLocalization localization) throws Exception {
        LocalizationService service = (LocalizationService) localization;

        java.lang.reflect.Method addLanguage = SlimefunLocalization.class.getDeclaredMethod("addLanguage", String.class, String.class);
        addLanguage.setAccessible(true);
        addLanguage.invoke(service, "en", "420b152778e7e2dce6f5e58d3e34ae4d1c3e5a6e4b6d3f4a5e6b7c8d9e0f1a2");
        addLanguage.invoke(service, "zh-CN", "7f9bc035cdc80f1ab5e1198f29f3ad3fdd2b42d9a69aeb64de990681800b98dc");

        java.lang.reflect.Field defaultField = LocalizationService.class.getDeclaredField("defaultLanguage");
        defaultField.setAccessible(true);

        java.lang.reflect.Method getLanguage = LocalizationService.class.getDeclaredMethod("getLanguage", String.class);
        getLanguage.setAccessible(true);
        Language zh = (Language) getLanguage.invoke(service, "zh-CN");
        defaultField.set(service, zh);
    }

    /**
     * Constructs a {@link PlayerProfile} through the protected constructor -
     * no async loading, exactly the state we set up.
     */
    private PlayerProfile newProfile(OfflinePlayer player, PlayerData data) throws Exception {
        Constructor<PlayerProfile> constructor = PlayerProfile.class.getDeclaredConstructor(OfflinePlayer.class, PlayerData.class);
        constructor.setAccessible(true);
        return constructor.newInstance(player, data);
    }

    /**
     * The full per-click action: opening a category page with all 36 slots
     * filled (header, back/pagination buttons and one built display item per
     * registered item).
     */
    private void categoryOpen(Results results, SlimefunGuideImplementation guide, ItemGroup group, PlayerProfile profile) {
        for (int w = 0; w < WARMUP; w++) {
            for (int i = 0; i < OPENS; i++) {
                guide.openItemGroup(profile, group, 1);
            }
        }

        long[] samples = new long[ROUNDS];

        for (int r = 0; r < ROUNDS; r++) {
            long start = System.nanoTime();

            for (int i = 0; i < OPENS; i++) {
                guide.openItemGroup(profile, group, 1);
            }

            samples[r] = System.nanoTime() - start;
        }

        Arrays.sort(samples);
        results.emit("guide-render", "category-open", "min_ns_per_open", "ns", Bench.min(samples) / (double) OPENS);
        results.emit("guide-render", "category-open", "median_ns_per_open", "ns", Bench.median(samples) / (double) OPENS);
    }

    /**
     * The per-displayed-item localization build alone: item-name lookup,
     * clone and lore translation.
     */
    private void localizedItem(Results results, SlimefunLocalization localization, Player player, List<SlimefunItem> items) {
        for (int w = 0; w < WARMUP; w++) {
            for (int i = 0; i < LOCALIZED_CALLS; i++) {
                localization.getLocalizedItem(player, items.get(i % ITEM_COUNT));
            }
        }

        long[] samples = new long[ROUNDS];

        for (int r = 0; r < ROUNDS; r++) {
            long start = System.nanoTime();

            for (int i = 0; i < LOCALIZED_CALLS; i++) {
                localization.getLocalizedItem(player, items.get(i % ITEM_COUNT));
            }

            samples[r] = System.nanoTime() - start;
        }

        Arrays.sort(samples);
        results.emit("guide-render", "localized-item", "min_ns_per_call", "ns", Bench.min(samples) / (double) LOCALIZED_CALLS);
        results.emit("guide-render", "localized-item", "median_ns_per_call", "ns", Bench.median(samples) / (double) LOCALIZED_CALLS);
    }

    /**
     * Calibration anchor: the per-item name lookup alone (a miss in zh-CN
     * items.yml falling through to the en fallback - the shape every addon
     * item has).
     */
    private void itemNameLookup(Results results, SlimefunLocalization localization, Player player, List<SlimefunItem> items) {
        for (int w = 0; w < WARMUP; w++) {
            for (int i = 0; i < LOCALIZED_CALLS; i++) {
                localization.getItemName(player, items.get(i % ITEM_COUNT));
            }
        }

        long[] samples = new long[ROUNDS];

        for (int r = 0; r < ROUNDS; r++) {
            long start = System.nanoTime();

            for (int i = 0; i < LOCALIZED_CALLS; i++) {
                localization.getItemName(player, items.get(i % ITEM_COUNT));
            }

            samples[r] = System.nanoTime() - start;
        }

        Arrays.sort(samples);
        results.emit("guide-render", "item-name-lookup", "min_ns_per_call", "ns", Bench.min(samples) / (double) LOCALIZED_CALLS);
        results.emit("guide-render", "item-name-lookup", "median_ns_per_call", "ns", Bench.median(samples) / (double) LOCALIZED_CALLS);
    }

    /**
     * Calibration anchor: the bare ItemStack clone + meta round-trip that
     * every localized display item pays (the part memoization cannot remove).
     */
    private void itemClone(Results results, List<SlimefunItem> items) {
        for (int w = 0; w < WARMUP; w++) {
            for (int i = 0; i < LOCALIZED_CALLS; i++) {
                io.github.bakedlibs.dough.items.CustomItemStack.create(items.get(i % ITEM_COUNT).getItem(), meta -> {});
            }
        }

        long[] samples = new long[ROUNDS];

        for (int r = 0; r < ROUNDS; r++) {
            long start = System.nanoTime();

            for (int i = 0; i < LOCALIZED_CALLS; i++) {
                io.github.bakedlibs.dough.items.CustomItemStack.create(items.get(i % ITEM_COUNT).getItem(), meta -> {});
            }

            samples[r] = System.nanoTime() - start;
        }

        Arrays.sort(samples);
        results.emit("guide-render", "item-clone", "min_ns_per_call", "ns", Bench.min(samples) / (double) LOCALIZED_CALLS);
        results.emit("guide-render", "item-clone", "median_ns_per_call", "ns", Bench.median(samples) / (double) LOCALIZED_CALLS);
    }

    /**
     * The lore phrase-replacement scan alone (the inner loop of every
     * displayed item on a translated deployment).
     */
    private void loreTranslate(Results results, SlimefunLocalization localization, Player player) {
        List<String> lore = Arrays.asList(LORE_LINES);

        for (int w = 0; w < WARMUP; w++) {
            for (int i = 0; i < LORE_CALLS; i++) {
                localization.translateLore(player, lore);
            }
        }

        long[] samples = new long[ROUNDS];

        for (int r = 0; r < ROUNDS; r++) {
            long start = System.nanoTime();

            for (int i = 0; i < LORE_CALLS; i++) {
                localization.translateLore(player, lore);
            }

            samples[r] = System.nanoTime() - start;
        }

        Arrays.sort(samples);
        results.emit("guide-render", "lore-translate", "min_ns_per_call", "ns", Bench.min(samples) / (double) LORE_CALLS);
        results.emit("guide-render", "lore-translate", "median_ns_per_call", "ns", Bench.median(samples) / (double) LORE_CALLS);
    }

    /**
     * The per-button message lookup alone (title, back/search/settings
     * buttons, pagination, locked-item hints).
     */
    private void messageLookup(Results results, SlimefunLocalization localization, Player player) {
        for (int w = 0; w < WARMUP; w++) {
            for (int i = 0; i < MESSAGE_CALLS; i++) {
                localization.getMessage(player, "guide.pages.previous");
            }
        }

        long[] samples = new long[ROUNDS];

        for (int r = 0; r < ROUNDS; r++) {
            long start = System.nanoTime();

            for (int i = 0; i < MESSAGE_CALLS; i++) {
                localization.getMessage(player, "guide.pages.previous");
            }

            samples[r] = System.nanoTime() - start;
        }

        Arrays.sort(samples);
        results.emit("guide-render", "message-lookup", "min_ns_per_call", "ns", Bench.min(samples) / (double) MESSAGE_CALLS);
        results.emit("guide-render", "message-lookup", "median_ns_per_call", "ns", Bench.median(samples) / (double) MESSAGE_CALLS);
    }
}
