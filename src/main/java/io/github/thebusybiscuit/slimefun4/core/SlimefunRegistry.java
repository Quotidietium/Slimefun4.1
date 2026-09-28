package io.github.thebusybiscuit.slimefun4.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang.Validate;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Server;
import org.bukkit.Tag;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Piglin;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import io.github.bakedlibs.dough.collections.KeyMap;
import io.github.bakedlibs.dough.config.Config;
import io.github.thebusybiscuit.slimefun4.api.geo.GEOResource;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.ItemHandler;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile;
import io.github.thebusybiscuit.slimefun4.api.researches.Research;
import io.github.thebusybiscuit.slimefun4.core.guide.SlimefunGuide;
import io.github.thebusybiscuit.slimefun4.core.guide.SlimefunGuideImplementation;
import io.github.thebusybiscuit.slimefun4.core.guide.SlimefunGuideMode;
import io.github.thebusybiscuit.slimefun4.core.multiblocks.MultiBlock;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.guide.CheatSheetSlimefunGuide;
import io.github.thebusybiscuit.slimefun4.implementation.guide.SurvivalSlimefunGuide;

import me.mrCookieSlime.Slimefun.api.BlockInfoConfig;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import me.mrCookieSlime.Slimefun.api.inventory.UniversalBlockMenu;

/**
 * This class houses a lot of instances of {@link Map} and {@link List} that hold
 * various mappings and collections related to {@link SlimefunItem}.
 *
 * @author TheBusyBiscuit
 *
 */
public final class SlimefunRegistry {

    /*
     * ConcurrentHashMap: getById(...) is called from async machine tickers (e.g. auto
     * crafters resolving their machine), while runtime item registration may put here
     * on the main thread - a plain HashMap is not safe under that overlap.
     */
    private final Map<String, SlimefunItem> slimefunIds = new ConcurrentHashMap<>();
    private final List<SlimefunItem> slimefunItems = new ArrayList<>();
    private final List<SlimefunItem> enabledItems = new ArrayList<>();

    /**
     * A set of every {@link Material} used by the template of any registered
     * {@link SlimefunItem}. This mirrors {@link #slimefunIds} and acts as a fast
     * negative lookup in {@link SlimefunItem#getByItem(ItemStack)}: an item whose
     * {@link Material} is not in this set can never resolve to a {@link SlimefunItem},
     * so the (comparatively expensive) PersistentDataContainer read can be skipped.
     */
    private final Set<Material> slimefunItemMaterials = ConcurrentHashMap.newKeySet();

    private final List<ItemGroup> categories = new ArrayList<>();
    private final List<MultiBlock> multiblocks = new LinkedList<>();

    /**
     * Multiblocks bucketed by the {@link Material} a player must click to trigger
     * them (the structure cell at the trigger face, tag-expanded). Lets the
     * interaction listener skip every multiblock whose trigger cell can never
     * match the clicked block. Rebuilt via {@link #rebuildMultiblockBuckets()}.
     */
    private final Map<Material, List<MultiBlock>> multiblockTriggerBuckets = new EnumMap<>(Material.class);

    /**
     * Multiblocks whose trigger structure cell is a null wildcard - they cannot
     * be pre-filtered by material and are always fully compared.
     */
    private final List<MultiBlock> unbinnedMultiblocks = new ArrayList<>();

    /**
     * Registry order of every binned/unbinned multiblock, so the listener can
     * keep the "last matching multiblock in registry order wins" selection
     * while iterating the (order-fragmented) buckets.
     */
    private final Map<MultiBlock, Integer> multiblockOrder = new IdentityHashMap<>();

    /**
     * The registry size the buckets were built from; a mismatch means someone
     * added to {@link #multiblocks} directly and the buckets must be rebuilt.
     */
    private int bucketedMultiblockCount = -1;

    private final List<Research> researches = new LinkedList<>();
    private final List<String> researchRanks = new ArrayList<>();
    private final Set<UUID> researchingPlayers = Collections.synchronizedSet(new HashSet<>());

    // TODO: Move this all into a proper "config cache" class
    private boolean automaticallyLoadItems;
    private boolean enableResearches;
    private boolean freeCreativeResearches;
    private boolean researchFireworks;
    private boolean disableLearningAnimation;
    private boolean logDuplicateBlockEntries;
    private boolean talismanActionBarMessages;

    private final Set<String> tickers = new HashSet<>();
    private final Set<SlimefunItem> radioactive = new HashSet<>();
    private final Set<ItemStack> barterDrops = new HashSet<>();

    private NamespacedKey soulboundKey;
    private NamespacedKey itemChargeKey;
    private NamespacedKey guideKey;

    private final KeyMap<GEOResource> geoResources = new KeyMap<>();

    private final Map<UUID, PlayerProfile> profiles = new ConcurrentHashMap<>();
    private final Map<String, BlockStorage> worlds = new ConcurrentHashMap<>();

    /*
     * These two Maps are written from the asynchronous ticker Thread
     * (GEO resources, network data, machine menus) while the auto-save
     * Thread iterates them - they must be concurrent.
     */
    private final Map<String, BlockInfoConfig> chunks = new ConcurrentHashMap<>();
    private final Map<SlimefunGuideMode, SlimefunGuideImplementation> guides = new EnumMap<>(SlimefunGuideMode.class);
    private final Map<EntityType, Set<ItemStack>> mobDrops = new ConcurrentHashMap<>();

    private final Map<String, BlockMenuPreset> blockMenuPresets = new HashMap<>();
    private final Map<String, UniversalBlockMenu> universalInventories = new ConcurrentHashMap<>();
    private final Map<Class<? extends ItemHandler>, Set<ItemHandler>> globalItemHandlers = new ConcurrentHashMap<>();

    public void load(@Nonnull Slimefun plugin, @Nonnull Config cfg) {
        Validate.notNull(plugin, "The Plugin cannot be null!");
        Validate.notNull(cfg, "The Config cannot be null!");

        soulboundKey = new NamespacedKey(plugin, "soulbound");
        itemChargeKey = new NamespacedKey(plugin, "item_charge");
        guideKey = new NamespacedKey(plugin, "slimefun_guide_mode");

        boolean showVanillaRecipes = cfg.getBoolean("guide.show-vanilla-recipes");
        boolean showHiddenItemGroupsInSearch = cfg.getBoolean("guide.show-hidden-item-groups-in-search");
        guides.put(SlimefunGuideMode.SURVIVAL_MODE, new SurvivalSlimefunGuide(showVanillaRecipes, showHiddenItemGroupsInSearch));
        guides.put(SlimefunGuideMode.CHEAT_MODE, new CheatSheetSlimefunGuide());

        researchRanks.addAll(cfg.getStringList("research-ranks"));

        freeCreativeResearches = cfg.getBoolean("researches.free-in-creative-mode");
        researchFireworks = cfg.getBoolean("researches.enable-fireworks");
        disableLearningAnimation = cfg.getBoolean("researches.disable-learning-animation");
        logDuplicateBlockEntries = cfg.getBoolean("options.log-duplicate-block-entries");
        talismanActionBarMessages = cfg.getBoolean("talismans.use-actionbar");
    }

    /**
     * This returns whether auto-loading is enabled.
     * Auto-Loading will automatically call {@link SlimefunItem#load()} when the item is registered.
     * Normally that method is called after the {@link Server} finished starting up.
     * But in the unusual scenario if a {@link SlimefunItem} is registered after that, this is gonna cover that.
     *
     * @return Whether auto-loading is enabled
     */
    public boolean isAutoLoadingEnabled() {
        return automaticallyLoadItems;
    }

    /**
     * This method will make any {@link SlimefunItem} which is registered automatically
     * call {@link SlimefunItem#load()}.
     * Normally this method call is delayed but when the {@link Server} is already running,
     * the method can be called instantaneously.
     *
     * @param mode
     *            Whether auto-loading should be enabled
     */
    public void setAutoLoadingMode(boolean mode) {
        automaticallyLoadItems = mode;
    }

    /**
     * This returns a {@link List} containing every enabled {@link ItemGroup}.
     *
     * @return {@link List} containing every enabled {@link ItemGroup}
     */
    public @Nonnull List<ItemGroup> getAllItemGroups() {
        return categories;
    }

    /**
     * This {@link List} contains every {@link SlimefunItem}, even disabled items.
     *
     * @return A {@link List} containing every {@link SlimefunItem}
     */
    public @Nonnull List<SlimefunItem> getAllSlimefunItems() {
        return slimefunItems;
    }

    /**
     * This {@link List} contains every <strong>enabled</strong> {@link SlimefunItem}.
     *
     * @return A {@link List} containing every enabled {@link SlimefunItem}
     */
    @Nonnull
    public List<SlimefunItem> getEnabledSlimefunItems() {
        return enabledItems;
    }

    /**
     * This returns a {@link List} containing every enabled {@link Research}.
     *
     * @return A {@link List} containing every enabled {@link Research}
     */
    @Nonnull
    public List<Research> getResearches() {
        return researches;
    }

    /**
     * This method returns a {@link Set} containing the {@link UUID} of every
     * {@link Player} who is currently unlocking a {@link Research}.
     *
     * @return A {@link Set} holding the {@link UUID} from every {@link Player}
     *         who is currently unlocking a {@link Research}
     */
    @Nonnull
    public Set<UUID> getCurrentlyResearchingPlayers() {
        return researchingPlayers;
    }

    @Nonnull
    public List<String> getResearchRanks() {
        return researchRanks;
    }

    public void setResearchingEnabled(boolean enabled) {
        enableResearches = enabled;
    }

    public boolean isResearchingEnabled() {
        return enableResearches;
    }

    public void setFreeCreativeResearchingEnabled(boolean enabled) {
        freeCreativeResearches = enabled;
    }

    public boolean isFreeCreativeResearchingEnabled() {
        return freeCreativeResearches;
    }

    public boolean isResearchFireworkEnabled() {
        return researchFireworks;
    }

    /**
     * Returns whether the research learning animations is disabled
     *
     * @return Whether the research learning animations is disabled
     */
    public boolean isLearningAnimationDisabled() {
        return disableLearningAnimation;
    }

    /**
     * This method returns a {@link List} of every enabled {@link MultiBlock}.
     *
     * @return A {@link List} containing every enabled {@link MultiBlock}
     */
    @Nonnull
    public List<MultiBlock> getMultiBlocks() {
        return multiblocks;
    }

    /**
     * Rebuilds the trigger-material buckets for the multiblock interaction
     * listener from the current {@link #getMultiBlocks()} list.
     *
     * Multiblocks whose trigger structure cell is a null wildcard are
     * collected separately (they always require a full comparison) and the
     * registry order of every multiblock is recorded so the listener's
     * "last match in registry order wins" selection stays intact.
     */
    public void rebuildMultiblockBuckets() {
        multiblockTriggerBuckets.clear();
        unbinnedMultiblocks.clear();
        multiblockOrder.clear();

        int index = 0;

        for (MultiBlock mb : multiblocks) {
            multiblockOrder.put(mb, index++);

            Material clickMaterial = mb.getClickMaterial();

            if (clickMaterial == null) {
                unbinnedMultiblocks.add(mb);
                continue;
            }

            addToBucket(clickMaterial, mb);

            // Tag equivalence: a wooden structure must trigger on every wood variant
            for (Tag<Material> tag : MultiBlock.getSupportedTags()) {
                if (tag.isTagged(clickMaterial)) {
                    for (Material variant : tag.getValues()) {
                        addToBucket(variant, mb);
                    }
                }
            }
        }

        bucketedMultiblockCount = multiblocks.size();
    }

    /**
     * Whether the trigger-material buckets were built from a different
     * registry size than the current one (i.e. someone added multiblocks
     * directly to the list instead of going through a registration hook).
     * The listener checks this before every lookup and rebuilds on demand.
     *
     * @return Whether the buckets are stale
     */
    public boolean isMultiblockBucketStale() {
        return bucketedMultiblockCount != multiblocks.size();
    }

    @ParametersAreNonnullByDefault
    private void addToBucket(Material material, MultiBlock mb) {
        multiblockTriggerBuckets.computeIfAbsent(material, key -> new ArrayList<>()).add(mb);
    }

    /**
     * The multiblocks that can possibly trigger when a player clicks a block
     * of the given {@link Material} (never null, possibly empty).
     *
     * @param material
     *            The clicked block's {@link Material}
     *
     * @return The candidate multiblocks for that material
     */
    @Nonnull
    public List<MultiBlock> getMultiblockCandidates(@Nonnull Material material) {
        List<MultiBlock> candidates = multiblockTriggerBuckets.get(material);
        return candidates != null ? candidates : Collections.emptyList();
    }

    /**
     * The multiblocks with a null-wildcard trigger cell; compared on every
     * click regardless of material (never null, usually empty).
     *
     * @return The unfilterable multiblocks
     */
    @Nonnull
    public List<MultiBlock> getUnbinnedMultiblocks() {
        return unbinnedMultiblocks;
    }

    /**
     * The registry order of a multiblock as recorded by the last
     * {@link #rebuildMultiblockBuckets()}.
     *
     * @param mb
     *            The multiblock to look up
     *
     * @return Its registry index, or -1 if unknown
     */
    public int getMultiblockOrder(@Nonnull MultiBlock mb) {
        Integer order = multiblockOrder.get(mb);
        return order != null ? order : -1;
    }

    /**
     * This returns the corresponding {@link SlimefunGuideImplementation} for a certain
     * {@link SlimefunGuideMode}.
     * <p>
     * This mainly only exists for internal purposes, if you want to open a certain section
     * using the {@link SlimefunGuide}, then please use the static methods provided in the
     * {@link SlimefunGuide} class.
     *
     * @param mode
     *            The {@link SlimefunGuideMode}
     *
     * @return The corresponding {@link SlimefunGuideImplementation}
     */
    @Nonnull
    public SlimefunGuideImplementation getSlimefunGuide(@Nonnull SlimefunGuideMode mode) {
        Validate.notNull(mode, "The Guide mode cannot be null");

        SlimefunGuideImplementation guide = guides.get(mode);

        if (guide == null) {
            throw new IllegalStateException("Slimefun Guide '" + mode + "' has no registered implementation.");
        }

        return guide;
    }

    /**
     * This returns a {@link Map} connecting the {@link EntityType} with a {@link Set}
     * of {@link ItemStack ItemStacks} which would be dropped when an {@link Entity} of that type was killed.
     *
     * @return The {@link Map} of custom mob drops
     */
    @Nonnull
    public Map<EntityType, Set<ItemStack>> getMobDrops() {
        return mobDrops;
    }

    /**
     * This returns a {@link Set} of {@link ItemStack ItemStacks} which can be obtained by bartering
     * with {@link Piglin Piglins}.
     *
     * @return A {@link Set} of bartering drops
     */
    @Nonnull
    public Set<ItemStack> getBarteringDrops() {
        return barterDrops;
    }

    @Nonnull
    public Set<SlimefunItem> getRadioactiveItems() {
        return radioactive;
    }

    @Nonnull
    public Set<String> getTickerBlocks() {
        return tickers;
    }

    @Nonnull
    public Map<String, SlimefunItem> getSlimefunItemIds() {
        return slimefunIds;
    }

    @Nonnull
    public Set<Material> getSlimefunItemMaterials() {
        return slimefunItemMaterials;
    }

    @Nonnull
    public Map<String, BlockMenuPreset> getMenuPresets() {
        return blockMenuPresets;
    }

    @Nonnull
    public Map<String, UniversalBlockMenu> getUniversalInventories() {
        return universalInventories;
    }

    @Nonnull
    public Map<UUID, PlayerProfile> getPlayerProfiles() {
        return profiles;
    }

    @Nonnull
    public Map<Class<? extends ItemHandler>, Set<ItemHandler>> getGlobalItemHandlers() {
        return globalItemHandlers;
    }

    @Nonnull
    public Set<ItemHandler> getGlobalItemHandlers(@Nonnull Class<? extends ItemHandler> identifier) {
        Validate.notNull(identifier, "The identifier for an ItemHandler cannot be null!");

        return globalItemHandlers.computeIfAbsent(identifier, c -> new HashSet<>());
    }

    @Nonnull
    public Map<String, BlockStorage> getWorlds() {
        return worlds;
    }

    @Nonnull
    public Map<String, BlockInfoConfig> getChunks() {
        return chunks;
    }

    @Nonnull
    public KeyMap<GEOResource> getGEOResources() {
        return geoResources;
    }

    public boolean logDuplicateBlockEntries() {
        return logDuplicateBlockEntries;
    }

    public boolean useActionbarForTalismans() {
        return talismanActionBarMessages;
    }

    @Nonnull
    public NamespacedKey getSoulboundDataKey() {
        return soulboundKey;
    }

    @Nonnull
    public NamespacedKey getItemChargeDataKey() {
        return itemChargeKey;
    }

    @Nonnull
    public NamespacedKey getGuideDataKey() {
        return guideKey;
    }

}
