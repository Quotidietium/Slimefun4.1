package io.github.thebusybiscuit.slimefun4.api.recipes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.bukkit.ChatColor;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import io.github.bakedlibs.dough.items.CustomItemStack;
import io.github.bakedlibs.dough.recipes.MinecraftRecipe;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.core.multiblocks.MultiBlockMachine;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun4.implementation.items.altar.AltarRecipe;
import io.github.thebusybiscuit.slimefun4.implementation.items.altar.AncientAltar;

// TODO: Remove this class and rewrite the recipe system
public class RecipeType implements Keyed {

    public static final RecipeType MULTIBLOCK = new RecipeType(new NamespacedKey(Slimefun.instance(), "multiblock"), CustomItemStack.create(Material.BRICKS, "&b多方块结构", "", "&a&o在世界中按图建造"));
    public static final RecipeType ARMOR_FORGE = new RecipeType(new NamespacedKey(Slimefun.instance(), "armor_forge"), SlimefunItems.ARMOR_FORGE, "", "&a&o在盔甲锻造台中制作");
    public static final RecipeType GRIND_STONE = new RecipeType(new NamespacedKey(Slimefun.instance(), "grind_stone"), SlimefunItems.GRIND_STONE, "", "&a&o使用磨石研磨");
    public static final RecipeType SMELTERY = new RecipeType(new NamespacedKey(Slimefun.instance(), "smeltery"), SlimefunItems.SMELTERY, "", "&a&o使用冶炼炉熔炼");
    public static final RecipeType ORE_CRUSHER = new RecipeType(new NamespacedKey(Slimefun.instance(), "ore_crusher"), SlimefunItems.ORE_CRUSHER, "", "&a&o使用矿石粉碎机粉碎");
    public static final RecipeType GOLD_PAN = new RecipeType(new NamespacedKey(Slimefun.instance(), "gold_pan"), SlimefunItems.GOLD_PAN, "", "&a&o对沙砾使用淘金盘获取此物品");
    public static final RecipeType COMPRESSOR = new RecipeType(new NamespacedKey(Slimefun.instance(), "compressor"), SlimefunItems.COMPRESSOR, "", "&a&o使用压缩机压缩");
    public static final RecipeType PRESSURE_CHAMBER = new RecipeType(new NamespacedKey(Slimefun.instance(), "pressure_chamber"), SlimefunItems.PRESSURE_CHAMBER, "", "&a&o使用压力舱加压");
    public static final RecipeType MAGIC_WORKBENCH = new RecipeType(new NamespacedKey(Slimefun.instance(), "magic_workbench"), SlimefunItems.MAGIC_WORKBENCH, "", "&a&o在魔法工作台中制作");
    public static final RecipeType ORE_WASHER = new RecipeType(new NamespacedKey(Slimefun.instance(), "ore_washer"), SlimefunItems.ORE_WASHER, "", "&a&o在洗矿机中淘洗");
    public static final RecipeType ENHANCED_CRAFTING_TABLE = new RecipeType(new NamespacedKey(Slimefun.instance(), "enhanced_crafting_table"), SlimefunItems.ENHANCED_CRAFTING_TABLE, "", "&a&o普通合成台无法", "&a&o容纳如此庞大的能量……");
    public static final RecipeType JUICER = new RecipeType(new NamespacedKey(Slimefun.instance(), "juicer"), SlimefunItems.JUICER, "", "&a&o用于制作果汁");

    public static final RecipeType ANCIENT_ALTAR = new RecipeType(new NamespacedKey(Slimefun.instance(), "ancient_altar"), SlimefunItems.ANCIENT_ALTAR.item(), (recipe, output) -> {
        AltarRecipe altarRecipe = new AltarRecipe(Arrays.asList(recipe), output);
        AncientAltar altar = ((AncientAltar) SlimefunItems.ANCIENT_ALTAR.getItem());
        altar.getRecipes().add(altarRecipe);
    });

    public static final RecipeType MOB_DROP = new RecipeType(new NamespacedKey(Slimefun.instance(), "mob_drop"), CustomItemStack.create(Material.IRON_SWORD, "&b怪物掉落"), RecipeType::registerMobDrop, "", "&r击杀指定的怪物以获取此物品");
    public static final RecipeType BARTER_DROP = new RecipeType(new NamespacedKey(Slimefun.instance(), "barter_drop"), CustomItemStack.create(Material.GOLD_INGOT, "&b以物易物"), RecipeType::registerBarterDrop, "&a与猪灵以物易物", "&a有几率获取此物品");
    public static final RecipeType INTERACT = new RecipeType(new NamespacedKey(Slimefun.instance(), "interact"), CustomItemStack.create(Material.PLAYER_HEAD, "&b使用物品", "", "&a&o手持此物品右键"));

    public static final RecipeType HEATED_PRESSURE_CHAMBER = new RecipeType(new NamespacedKey(Slimefun.instance(), "heated_pressure_chamber"), SlimefunItems.HEATED_PRESSURE_CHAMBER);
    public static final RecipeType FOOD_FABRICATOR = new RecipeType(new NamespacedKey(Slimefun.instance(), "food_fabricator"), SlimefunItems.FOOD_FABRICATOR);
    public static final RecipeType FOOD_COMPOSTER = new RecipeType(new NamespacedKey(Slimefun.instance(), "food_composter"), SlimefunItems.FOOD_COMPOSTER);
    public static final RecipeType FREEZER = new RecipeType(new NamespacedKey(Slimefun.instance(), "freezer"), SlimefunItems.FREEZER);
    public static final RecipeType REFINERY = new RecipeType(new NamespacedKey(Slimefun.instance(), "refinery"), SlimefunItems.REFINERY);

    public static final RecipeType GEO_MINER = new RecipeType(new NamespacedKey(Slimefun.instance(), "geo_miner"), SlimefunItems.GEO_MINER);
    public static final RecipeType NUCLEAR_REACTOR = new RecipeType(new NamespacedKey(Slimefun.instance(), "nuclear_reactor"), SlimefunItems.NUCLEAR_REACTOR);

    public static final RecipeType NULL = new RecipeType();

    private final ItemStack item;
    private final NamespacedKey key;
    private final String machine;
    private BiConsumer<ItemStack[], ItemStack> consumer;

    private RecipeType() {
        this.item = null;
        this.machine = "";
        this.key = new NamespacedKey(Slimefun.instance(), "null");
    }

    public RecipeType(ItemStack item, String machine) {
        this.item = item;
        this.machine = machine;

        if (machine.length() > 0) {
            this.key = new NamespacedKey(Slimefun.instance(), machine.toLowerCase(Locale.ROOT));
        } else {
            this.key = new NamespacedKey(Slimefun.instance(), "unknown");
        }
    }

    public RecipeType(NamespacedKey key, SlimefunItemStack slimefunItem, String... lore) {
        this(key, slimefunItem.item(), null, lore);
    }

    public RecipeType(NamespacedKey key, ItemStack item, BiConsumer<ItemStack[], ItemStack> callback, String... lore) {
        this.item = CustomItemStack.create(item, null, lore);
        this.key = key;
        this.consumer = callback;

        Optional<String> itemId = Slimefun.getItemDataService().getItemData(item);
        this.machine = itemId.orElse("");
    }

    public RecipeType(NamespacedKey key, ItemStack item) {
        this(key, item, null);
    }

    public RecipeType(MinecraftRecipe<?> recipe) {
        this.item = new ItemStack(recipe.getMachine());
        this.machine = "";
        this.key = NamespacedKey.minecraft(recipe.getRecipeClass().getSimpleName().toLowerCase(Locale.ROOT).replace("recipe", ""));
    }

    public void register(ItemStack[] recipe, ItemStack result) {
        if (consumer != null) {
            consumer.accept(recipe, result);
        } else {
            SlimefunItem slimefunItem = SlimefunItem.getById(this.machine);

            if (slimefunItem instanceof MultiBlockMachine mbm) {
                mbm.addRecipe(recipe, result);
            }
        }
    }

    public @Nullable ItemStack toItem() {
        return this.item;
    }

    public @Nonnull ItemStack getItem(Player p) {
        return Slimefun.getLocalization().getRecipeTypeItem(p, this);
    }

    public SlimefunItem getMachine() {
        return SlimefunItem.getById(machine);
    }

    @Override
    public final @Nonnull NamespacedKey getKey() {
        return key;
    }

    @Override
    public final boolean equals(Object obj) {
        if (obj instanceof RecipeType recipeType) {
            return recipeType.getKey().equals(this.getKey());
        } else {
            return false;
        }
    }

    @Override
    public final int hashCode() {
        return getKey().hashCode();
    }

    @ParametersAreNonnullByDefault
    private static void registerBarterDrop(ItemStack[] recipe, ItemStack output) {
        Slimefun.getRegistry().getBarteringDrops().add(output);
    }

    @ParametersAreNonnullByDefault
    private static void registerMobDrop(ItemStack[] recipe, ItemStack output) {
        String mob = ChatColor.stripColor(recipe[4].getItemMeta().getDisplayName()).toUpperCase(Locale.ROOT).replace(' ', '_');
        EntityType entity = resolveEntityType(mob);

        // computeIfAbsent is atomic on the (now concurrent) mobDrops map. The previous
        // getOrDefault + put sequence could lose a concurrently-registered drop set entirely.
        Slimefun.getRegistry().getMobDrops().computeIfAbsent(entity, e -> new HashSet<>()).add(output);
    }

    /**
     * Localized display-name aliases for mob-drop recipe icons. The icon name is parsed back into an
     * {@link EntityType} by {@link #registerMobDrop(ItemStack[], ItemStack)}; these aliases allow the
     * icon to be displayed in Chinese while remaining resolvable.
     */
    private static final Map<String, EntityType> ENTITY_NAME_ALIASES = Map.of("铁傀儡", EntityType.IRON_GOLEM, "猪灵", EntityType.PIGLIN);

    private static @Nonnull EntityType resolveEntityType(@Nonnull String name) {
        try {
            return EntityType.valueOf(name);
        } catch (IllegalArgumentException x) {
            EntityType alias = ENTITY_NAME_ALIASES.get(name);

            if (alias != null) {
                return alias;
            }

            throw x;
        }
    }

    public static List<ItemStack> getRecipeInputs(MultiBlockMachine machine) {
        if (machine == null) {
            return new ArrayList<>();
        }

        List<ItemStack[]> recipes = machine.getRecipes();
        List<ItemStack> convertible = new ArrayList<>();

        for (int i = 0; i < recipes.size(); i++) {
            if (i % 2 == 0) {
                convertible.add(recipes.get(i)[0]);
            }
        }

        return convertible;
    }

    public static List<ItemStack[]> getRecipeInputList(MultiBlockMachine machine) {
        if (machine == null) {
            return new ArrayList<>();
        }

        List<ItemStack[]> recipes = machine.getRecipes();
        List<ItemStack[]> convertible = new ArrayList<>();

        for (int i = 0; i < recipes.size(); i++) {
            if (i % 2 == 0) {
                convertible.add(recipes.get(i));
            }
        }

        convertible.sort(Comparator.comparing(recipe -> {
            int emptySlots = 9;

            for (ItemStack ingredient : recipe) {
                if (ingredient != null) {
                    emptySlots--;
                }
            }

            return emptySlots;
        }));

        return convertible;
    }

    public static ItemStack getRecipeOutput(MultiBlockMachine machine, ItemStack input) {
        List<ItemStack[]> recipes = machine.getRecipes();
        return recipes.get(((getRecipeInputs(machine).indexOf(input) * 2) + 1))[0].clone();
    }

    public static ItemStack getRecipeOutputList(MultiBlockMachine machine, ItemStack[] input) {
        List<ItemStack[]> recipes = machine.getRecipes();
        return recipes.get((recipes.indexOf(input) + 1))[0];
    }
}
