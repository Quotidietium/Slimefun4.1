package io.github.thebusybiscuit.slimefun4.implementation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.core.attributes.MachineTier;
import io.github.thebusybiscuit.slimefun4.core.attributes.MachineType;
import io.github.thebusybiscuit.slimefun4.core.attributes.Radioactivity;
import io.github.thebusybiscuit.slimefun4.implementation.items.magical.staves.StormStaff;
import io.github.thebusybiscuit.slimefun4.utils.ChatUtils;
import io.github.thebusybiscuit.slimefun4.utils.HeadTexture;
import io.github.thebusybiscuit.slimefun4.utils.LoreBuilder;
import io.github.thebusybiscuit.slimefun4.utils.compatibility.VersionedEnchantment;
import io.github.thebusybiscuit.slimefun4.utils.itemstack.ColoredFireworkStar;

/**
 * This class holds a static references to every {@link SlimefunItemStack}
 * found in Slimefun.
 */
@SuppressWarnings("java:S1192") // Suppress "duplicate string literal" warnings
public final class SlimefunItems {

    private SlimefunItems() {}

    /* Items */
    public static final SlimefunItemStack PORTABLE_CRAFTER = new SlimefunItemStack("PORTABLE_CRAFTER", HeadTexture.PORTABLE_CRAFTER, "&6便携式合成台", "&a&oA portable Crafting Table", "", LoreBuilder.RIGHT_CLICK_TO_OPEN);
    public static final SlimefunItemStack PORTABLE_DUSTBIN = new SlimefunItemStack("PORTABLE_DUSTBIN", HeadTexture.TRASH_CAN, "&6便携式垃圾桶", "&fYour portable Item-Destroyer", "", LoreBuilder.RIGHT_CLICK_TO_OPEN);
    public static final SlimefunItemStack ENDER_BACKPACK = new SlimefunItemStack("ENDER_BACKPACK", HeadTexture.ENDER_BACKPACK, "&6末影背包", "&a&oA portable Ender Chest", "", LoreBuilder.RIGHT_CLICK_TO_OPEN);
    public static final SlimefunItemStack MAGIC_EYE_OF_ENDER = new SlimefunItemStack("MAGIC_EYE_OF_ENDER", Material.ENDER_EYE, "&6&l末影之眼", "&4&lRequires full Ender Armor", "", "&7&eRight Click&7 to shoot an Ender Pearl");
    public static final SlimefunItemStack BROKEN_SPAWNER = new SlimefunItemStack("BROKEN_SPAWNER", Material.SPAWNER, "&c损坏的刷怪笼", "&7Type: &b<Type>", "", "&cFractured, must be repaired in an Ancient Altar");
    public static final SlimefunItemStack REPAIRED_SPAWNER = new SlimefunItemStack("REINFORCED_SPAWNER", Material.SPAWNER, "&b强化刷怪笼", "&7Type: &b<Type>");
    public static final SlimefunItemStack INFERNAL_BONEMEAL = new SlimefunItemStack("INFERNAL_BONEMEAL", Material.BONE_MEAL, "&4地狱骨粉", "", "&cSpeeds up the Growth of", "&cNether Warts as well");
    public static final SlimefunItemStack TAPE_MEASURE = new SlimefunItemStack("TAPE_MEASURE", "180d5c43a6cf5bb7769fd0c8240e1e70d2ae38ef9d78a1db401aca6a2cb36f65", "&6卷尺", "", "&eCrouch & Right Click &7to set an anchor", "&eRight Click &7to measure");

    /* Gadgets */
    public static final SlimefunItemStack GOLD_PAN = new SlimefunItemStack("GOLD_PAN", Material.BOWL, "&6淘金盘", "", "&eRight Click&7 to collect resources", "&7from Gravel");
    public static final SlimefunItemStack NETHER_GOLD_PAN = new SlimefunItemStack("NETHER_GOLD_PAN", Material.BOWL, "&4下界淘金盘", "", "&eRight Click&7 to collect resources", "&7from Soul Sand");
    public static final SlimefunItemStack PARACHUTE = new SlimefunItemStack("PARACHUTE", Material.LEATHER_CHESTPLATE, Color.WHITE, "&f&l降落伞", "", LoreBuilder.CROUCH_TO_USE);
    public static final SlimefunItemStack GRAPPLING_HOOK = new SlimefunItemStack("GRAPPLING_HOOK", Material.LEAD, "&6抓钩", "", LoreBuilder.RIGHT_CLICK_TO_USE);
    public static final SlimefunItemStack SOLAR_HELMET = new SlimefunItemStack("SOLAR_HELMET", Material.IRON_HELMET, "&b太阳能头盔", "", "&a&oCharges held Items and Armor");
    public static final SlimefunItemStack CLOTH = new SlimefunItemStack("CLOTH", Material.PAPER, "&b布料");
    public static final SlimefunItemStack REINFORCED_CLOTH = new SlimefunItemStack("REINFORCED_CLOTH", Material.PAPER, "&b强化布料", "", "&fThis cloth has been reinforced", "&fwith &bLead &fto protect against", "&fradioactive substances");
    public static final SlimefunItemStack TIN_CAN = new SlimefunItemStack("CAN", HeadTexture.TIN_CAN, "&f锡罐");
    public static final SlimefunItemStack NIGHT_VISION_GOGGLES = new SlimefunItemStack("NIGHT_VISION_GOGGLES", Material.LEATHER_HELMET, Color.BLACK, "&a夜视仪", "", "&9+ Night Vision");
    public static final SlimefunItemStack ELYTRA_CAP = new SlimefunItemStack("ELYTRA_CAP", Material.LEATHER_HELMET, Color.PURPLE, "&5鞘翅帽", "", "&7This helmet will protect you from", "&7crashing while flying with an elytra.");
    public static final SlimefunItemStack FARMER_SHOES = new SlimefunItemStack("FARMER_SHOES", Material.LEATHER_BOOTS, Color.YELLOW, "&e农夫鞋", "", "&6&oPrevents you from trampling your Crops");
    public static final SlimefunItemStack INFUSED_MAGNET = new SlimefunItemStack("INFUSED_MAGNET", HeadTexture.MAGNET, "&a注入磁铁", "", "&fMagical infused Magnets", "&fattract nearby Items", "&fas long as it is somewhere in", "&fyour Inventory", "", "&7Hold &eShift&7 to pick up nearby Items");
    public static final SlimefunItemStack RAG = new SlimefunItemStack("RAG", Material.PAPER, "&c破布", "", "&aLevel I - Medical Supply", "", "&fRestores 2 Hearts", "&fExtinguishes Fire", "", LoreBuilder.RIGHT_CLICK_TO_USE);
    public static final SlimefunItemStack BANDAGE = new SlimefunItemStack("BANDAGE", Material.PAPER, "&c绷带", "", "&aLevel II - Medical Supply", "", "&fRestores 4 Hearts", "&fExtinguishes Fire", "", LoreBuilder.RIGHT_CLICK_TO_USE);
    public static final SlimefunItemStack SPLINT = new SlimefunItemStack("SPLINT", Material.STICK, "&c夹板", "", "&aLevel I - Medical Supply", "", "&fRestores 2 Hearts", "", LoreBuilder.RIGHT_CLICK_TO_USE);
    public static final SlimefunItemStack VITAMINS = new SlimefunItemStack("VITAMINS", Material.NETHER_WART, "&c维生素", "", "&aLevel III - Medical Supply", "", "&fRestores 4 Hearts", "&fExtinguishes Fire", "&fCures Poison/Wither/Radiation", "", LoreBuilder.RIGHT_CLICK_TO_USE);
    public static final SlimefunItemStack MEDICINE = new SlimefunItemStack("MEDICINE", Material.POTION, Color.RED, "&c药品", "", "&aLevel III - Medical Supply", "", "&fRestores 4 Hearts", "&fExtinguishes Fire", "&fCures Poison/Wither/Radiation");
    public static final SlimefunItemStack MAGICAL_ZOMBIE_PILLS = new SlimefunItemStack("MAGICAL_ZOMBIE_PILLS", Material.NETHER_WART, "&6魔法僵尸药丸", "", "&eRight Click &7a Zombified Villager", "&eor &7a Zombified Piglin to", "&7instantly cure it from its curse");

    public static final SlimefunItemStack FLASK_OF_KNOWLEDGE = new SlimefunItemStack("FLASK_OF_KNOWLEDGE", Material.GLASS_BOTTLE, "&c知识之瓶", "", "&fAllows you to store some of", "&fyour Experience in a Bottle", "&7Cost: &a1 Level");
    public static final SlimefunItemStack FILLED_FLASK_OF_KNOWLEDGE = new SlimefunItemStack("FILLED_FLASK_OF_KNOWLEDGE", Material.EXPERIENCE_BOTTLE, "&a装满的知识之瓶");

    /* Backpacks */
    private static final String BACKPACK_ID = "&7ID: <ID>";
    public static final SlimefunItemStack BACKPACK_SMALL = new SlimefunItemStack("SMALL_BACKPACK", HeadTexture.BACKPACK, "&e小型背包", "", "&7Size: &e9", BACKPACK_ID, "", LoreBuilder.RIGHT_CLICK_TO_OPEN);
    public static final SlimefunItemStack BACKPACK_MEDIUM = new SlimefunItemStack("MEDIUM_BACKPACK", HeadTexture.BACKPACK, "&e背包", "", "&7Size: &e18", BACKPACK_ID, "", LoreBuilder.RIGHT_CLICK_TO_OPEN);
    public static final SlimefunItemStack BACKPACK_LARGE = new SlimefunItemStack("LARGE_BACKPACK", HeadTexture.BACKPACK, "&e大型背包", "", "&7Size: &e27", BACKPACK_ID, "", LoreBuilder.RIGHT_CLICK_TO_OPEN);
    public static final SlimefunItemStack WOVEN_BACKPACK = new SlimefunItemStack("WOVEN_BACKPACK", HeadTexture.BACKPACK, "&e编织背包", "", "&7Size: &e36", BACKPACK_ID, "", LoreBuilder.RIGHT_CLICK_TO_OPEN);
    public static final SlimefunItemStack GILDED_BACKPACK = new SlimefunItemStack("GILDED_BACKPACK", HeadTexture.BACKPACK, "&e镀金背包", "", "&7Size: &e45", BACKPACK_ID, "", LoreBuilder.RIGHT_CLICK_TO_OPEN);
    public static final SlimefunItemStack RADIANT_BACKPACK = new SlimefunItemStack("RADIANT_BACKPACK", HeadTexture.BACKPACK, "&e闪耀背包", "", "&7Size: &e54 (Double chest)", BACKPACK_ID, "", LoreBuilder.RIGHT_CLICK_TO_OPEN);
    public static final SlimefunItemStack BOUND_BACKPACK = new SlimefunItemStack("BOUND_BACKPACK", HeadTexture.ENDER_BACKPACK, "&c灵魂绑定背包", "", "&7Size: &e36", BACKPACK_ID, "", LoreBuilder.RIGHT_CLICK_TO_OPEN);
    public static final SlimefunItemStack COOLER = new SlimefunItemStack("COOLER", HeadTexture.COOLER, "&b冷藏箱", "&fAllows you to store Juices/Smoothies", "&fand automatically consumes them when you are hungry", "&fand you have this in your Inventory", "", "&7Size: &e27", BACKPACK_ID, "", LoreBuilder.RIGHT_CLICK_TO_OPEN);
    public static final SlimefunItemStack RESTORED_BACKPACK = new SlimefunItemStack("RESTORED_BACKPACK", HeadTexture.RESTORED_BACKPACK, "&e恢复的背包", "", "&7Retrieve your lost items", BACKPACK_ID, "", LoreBuilder.RIGHT_CLICK_TO_OPEN);

    /* Jetpacks */
    public static final SlimefunItemStack DURALUMIN_JETPACK = new SlimefunItemStack("DURALUMIN_JETPACK", Material.LEATHER_CHESTPLATE, Color.SILVER, "&9电力喷气背包 &7- &eI", "", LoreBuilder.material("Duralumin"), LoreBuilder.powerCharged(0, 20), "&8\u21E8 &7Thrust: &c0.35", "", LoreBuilder.CROUCH_TO_USE);
    public static final SlimefunItemStack SOLDER_JETPACK = new SlimefunItemStack("SOLDER_JETPACK", Material.LEATHER_CHESTPLATE, Color.SILVER, "&9电力喷气背包 &7- &eII", "", LoreBuilder.material("Solder"), LoreBuilder.powerCharged(0, 30), "&8\u21E8 &7Thrust: &c0.4", "", LoreBuilder.CROUCH_TO_USE);
    public static final SlimefunItemStack BILLON_JETPACK = new SlimefunItemStack("BILLON_JETPACK", Material.LEATHER_CHESTPLATE, Color.SILVER, "&9电力喷气背包 &7- &eIII", "", LoreBuilder.material("Billon"), LoreBuilder.powerCharged(0, 45), "&8\u21E8 &7Thrust: &c0.45", "", LoreBuilder.CROUCH_TO_USE);
    public static final SlimefunItemStack STEEL_JETPACK = new SlimefunItemStack("STEEL_JETPACK", Material.LEATHER_CHESTPLATE, Color.SILVER, "&9电力喷气背包 &7- &eIV", "", LoreBuilder.material("Steel"), LoreBuilder.powerCharged(0, 60), "&8\u21E8 &7Thrust: &c0.5", "", LoreBuilder.CROUCH_TO_USE);
    public static final SlimefunItemStack DAMASCUS_STEEL_JETPACK = new SlimefunItemStack("DAMASCUS_STEEL_JETPACK", Material.LEATHER_CHESTPLATE, Color.SILVER, "&9电力喷气背包 &7- &eV", "", LoreBuilder.material("Damascus Steel"), LoreBuilder.powerCharged(0, 75), "&8\u21E8 &7Thrust: &c0.55", "", LoreBuilder.CROUCH_TO_USE);
    public static final SlimefunItemStack REINFORCED_ALLOY_JETPACK = new SlimefunItemStack("REINFORCED_ALLOY_JETPACK", Material.LEATHER_CHESTPLATE, Color.SILVER, "&9电力喷气背包 &7- &eVI", "", LoreBuilder.material("Reinforced Alloy"), LoreBuilder.powerCharged(0, 100), "&8\u21E8 &7Thrust: &c0.6", "", LoreBuilder.CROUCH_TO_USE);
    public static final SlimefunItemStack CARBONADO_JETPACK = new SlimefunItemStack("CARBONADO_JETPACK", Material.LEATHER_CHESTPLATE, Color.BLACK, "&9电力喷气背包 &7- &eVII", "", LoreBuilder.material("Carbonado"), LoreBuilder.powerCharged(0, 150), "&8\u21E8 &7Thrust: &c0.7", "", LoreBuilder.CROUCH_TO_USE);
    public static final SlimefunItemStack ARMORED_JETPACK = new SlimefunItemStack("ARMORED_JETPACK", Material.IRON_CHESTPLATE, "&9装甲喷气背包", LoreBuilder.material("Steel"), "", LoreBuilder.powerCharged(0, 50), "&8\u21E8 &7Thrust: &c0.5", "", LoreBuilder.CROUCH_TO_USE);

    /* Jetboots */
    public static final SlimefunItemStack DURALUMIN_JETBOOTS = new SlimefunItemStack("DURALUMIN_JETBOOTS", Material.LEATHER_BOOTS, Color.SILVER, "&9喷气靴 &7- &eI", "", LoreBuilder.material("Duralumin"), LoreBuilder.powerCharged(0, 20), LoreBuilder.speed(0.35F), "&8\u21E8 &7Accuracy: &c50%", "", LoreBuilder.CROUCH_TO_USE);
    public static final SlimefunItemStack SOLDER_JETBOOTS = new SlimefunItemStack("SOLDER_JETBOOTS", Material.LEATHER_BOOTS, Color.SILVER, "&9喷气靴 &7- &eII", "", LoreBuilder.material("Solder"), LoreBuilder.powerCharged(0, 30), LoreBuilder.speed(0.4F), "&8\u21E8 &7Accuracy: &660%", "", LoreBuilder.CROUCH_TO_USE);
    public static final SlimefunItemStack BILLON_JETBOOTS = new SlimefunItemStack("BILLON_JETBOOTS", Material.LEATHER_BOOTS, Color.SILVER, "&9喷气靴 &7- &eIII", "", LoreBuilder.material("Billon"), LoreBuilder.powerCharged(0, 40), LoreBuilder.speed(0.45F), "&8\u21E8 &7Accuracy: &665%", "", LoreBuilder.CROUCH_TO_USE);
    public static final SlimefunItemStack STEEL_JETBOOTS = new SlimefunItemStack("STEEL_JETBOOTS", Material.LEATHER_BOOTS, Color.SILVER, "&9喷气靴 &7- &eIV", "", LoreBuilder.material("Steel"), LoreBuilder.powerCharged(0, 50), LoreBuilder.speed(0.5F), "&8\u21E8 &7Accuracy: &e70%", "", LoreBuilder.CROUCH_TO_USE);
    public static final SlimefunItemStack DAMASCUS_STEEL_JETBOOTS = new SlimefunItemStack("DAMASCUS_STEEL_JETBOOTS", Material.LEATHER_BOOTS, Color.SILVER, "&9喷气靴 &7- &eV", "", LoreBuilder.material("Damascus Steel"), LoreBuilder.powerCharged(0, 75), LoreBuilder.speed(0.55F), "&8\u21E8 &7Accuracy: &a75%", "", LoreBuilder.CROUCH_TO_USE);
    public static final SlimefunItemStack REINFORCED_ALLOY_JETBOOTS = new SlimefunItemStack("REINFORCED_ALLOY_JETBOOTS", Material.LEATHER_BOOTS, Color.SILVER, "&9喷气靴 &7- &eVI", "", LoreBuilder.material("Reinforced Alloy"), LoreBuilder.powerCharged(0, 100), LoreBuilder.speed(0.6F), "&8\u21E8 &7Accuracy: &c80%", "", LoreBuilder.CROUCH_TO_USE);
    public static final SlimefunItemStack CARBONADO_JETBOOTS = new SlimefunItemStack("CARBONADO_JETBOOTS", Material.LEATHER_BOOTS, Color.BLACK, "&9喷气靴 &7- &eVII", "", LoreBuilder.material("Carbonado"), LoreBuilder.powerCharged(0, 125), LoreBuilder.speed(0.7F), "&8\u21E8 &7Accuracy: &c99.9%", "", LoreBuilder.CROUCH_TO_USE);
    public static final SlimefunItemStack ARMORED_JETBOOTS = new SlimefunItemStack("ARMORED_JETBOOTS", Material.IRON_BOOTS, "&9装甲喷气靴", "", LoreBuilder.material("Steel"), LoreBuilder.powerCharged(0, 50), LoreBuilder.speed(0.45F), "&8\u21E8 &7Accuracy: &e70%", "", LoreBuilder.CROUCH_TO_USE);

    /* Multi Tools */
    private static final String MULTI_TOOL_LORE = "&eCrouch & Right Click&7 to switch modes";
    public static final SlimefunItemStack DURALUMIN_MULTI_TOOL = new SlimefunItemStack("DURALUMIN_MULTI_TOOL", Material.SHEARS, "&9多功能工具 &7- &eI", "", LoreBuilder.material("Duralumin"), LoreBuilder.powerCharged(0, 20), "", LoreBuilder.RIGHT_CLICK_TO_USE, MULTI_TOOL_LORE);
    public static final SlimefunItemStack SOLDER_MULTI_TOOL = new SlimefunItemStack("SOLDER_MULTI_TOOL", Material.SHEARS, "&9多功能工具 &7- &eII", "", LoreBuilder.material("Solder"), LoreBuilder.powerCharged(0, 30), "", LoreBuilder.RIGHT_CLICK_TO_USE, MULTI_TOOL_LORE);
    public static final SlimefunItemStack BILLON_MULTI_TOOL = new SlimefunItemStack("BILLON_MULTI_TOOL", Material.SHEARS, "&9多功能工具 &7- &eIII", "", LoreBuilder.material("Billon"), LoreBuilder.powerCharged(0, 40), "", LoreBuilder.RIGHT_CLICK_TO_USE, MULTI_TOOL_LORE);
    public static final SlimefunItemStack STEEL_MULTI_TOOL = new SlimefunItemStack("STEEL_MULTI_TOOL", Material.SHEARS, "&9多功能工具 &7- &eIV", "", LoreBuilder.material("Steel"), LoreBuilder.powerCharged(0, 50), "", LoreBuilder.RIGHT_CLICK_TO_USE, MULTI_TOOL_LORE);
    public static final SlimefunItemStack DAMASCUS_STEEL_MULTI_TOOL = new SlimefunItemStack("DAMASCUS_STEEL_MULTI_TOOL", Material.SHEARS, "&9多功能工具 &7- &eV", "", LoreBuilder.material("Damascus Steel"), LoreBuilder.powerCharged(0, 60), "", LoreBuilder.RIGHT_CLICK_TO_USE, MULTI_TOOL_LORE);
    public static final SlimefunItemStack REINFORCED_ALLOY_MULTI_TOOL = new SlimefunItemStack("REINFORCED_ALLOY_MULTI_TOOL", Material.SHEARS, "&9多功能工具 &7- &eVI", "", LoreBuilder.material("Reinforced Alloy"), LoreBuilder.powerCharged(0, 75), "", LoreBuilder.RIGHT_CLICK_TO_USE, MULTI_TOOL_LORE);
    public static final SlimefunItemStack CARBONADO_MULTI_TOOL = new SlimefunItemStack("CARBONADO_MULTI_TOOL", Material.SHEARS, "&9多功能工具 &7- &eVII", "", LoreBuilder.material("Carbonado"), LoreBuilder.powerCharged(0, 100), "", LoreBuilder.RIGHT_CLICK_TO_USE, MULTI_TOOL_LORE);

    static {
        ItemMeta duralumin = DURALUMIN_MULTI_TOOL.getItemMeta();
        duralumin.setUnbreakable(true);
        DURALUMIN_MULTI_TOOL.setItemMeta(duralumin);

        ItemMeta solder = SOLDER_MULTI_TOOL.getItemMeta();
        solder.setUnbreakable(true);
        SOLDER_MULTI_TOOL.setItemMeta(solder);

        ItemMeta billon = BILLON_MULTI_TOOL.getItemMeta();
        billon.setUnbreakable(true);
        BILLON_MULTI_TOOL.setItemMeta(billon);

        ItemMeta steel = STEEL_MULTI_TOOL.getItemMeta();
        steel.setUnbreakable(true);
        STEEL_MULTI_TOOL.setItemMeta(steel);

        ItemMeta damascus = DAMASCUS_STEEL_MULTI_TOOL.getItemMeta();
        damascus.setUnbreakable(true);
        DAMASCUS_STEEL_MULTI_TOOL.setItemMeta(damascus);

        ItemMeta reinforced = REINFORCED_ALLOY_MULTI_TOOL.getItemMeta();
        reinforced.setUnbreakable(true);
        REINFORCED_ALLOY_MULTI_TOOL.setItemMeta(reinforced);

        ItemMeta carbonado = CARBONADO_MULTI_TOOL.getItemMeta();
        carbonado.setUnbreakable(true);
        CARBONADO_MULTI_TOOL.setItemMeta(carbonado);
    }

    /* Food */
    public static final SlimefunItemStack FORTUNE_COOKIE = new SlimefunItemStack("FORTUNE_COOKIE", Material.COOKIE, "&6幸运饼干", "", "&a&oTells you stuff about your Future :o");
    public static final SlimefunItemStack DIET_COOKIE = new SlimefunItemStack("DIET_COOKIE", Material.COOKIE, "&6减肥饼干", "", "&aA very &olightweight &f&acookie.");
    public static final SlimefunItemStack MAGIC_SUGAR = new SlimefunItemStack("MAGIC_SUGAR", Material.SUGAR, "&6魔法糖", "", "&a&oFeel the Power of Hermes!");
    public static final SlimefunItemStack MONSTER_JERKY = new SlimefunItemStack("MONSTER_JERKY", Material.ROTTEN_FLESH, "&6怪物肉干", "", "&a&oNo longer hungry");
    public static final SlimefunItemStack APPLE_JUICE = new SlimefunItemStack("APPLE_JUICE", Color.RED, new PotionEffect(PotionEffectType.SATURATION, 5, 0), "&c苹果汁", "", LoreBuilder.hunger(3));
    public static final SlimefunItemStack MELON_JUICE = new SlimefunItemStack("MELON_JUICE", Color.RED, new PotionEffect(PotionEffectType.SATURATION, 5, 0), "&c西瓜汁", "", LoreBuilder.hunger(3));
    public static final SlimefunItemStack CARROT_JUICE = new SlimefunItemStack("CARROT_JUICE", Color.ORANGE, new PotionEffect(PotionEffectType.SATURATION, 5, 0), "&6胡萝卜汁", "", LoreBuilder.hunger(3));
    public static final SlimefunItemStack PUMPKIN_JUICE = new SlimefunItemStack("PUMPKIN_JUICE", Color.ORANGE, new PotionEffect(PotionEffectType.SATURATION, 5, 0), "&6南瓜汁", "", LoreBuilder.hunger(3));
    public static final SlimefunItemStack SWEET_BERRY_JUICE = new SlimefunItemStack("SWEET_BERRY_JUICE", Color.RED, new PotionEffect(PotionEffectType.SATURATION, 5, 0), "&c甜浆果汁", "", LoreBuilder.hunger(3));
    public static final SlimefunItemStack GLOW_BERRY_JUICE = new SlimefunItemStack("GLOW_BERRY_JUICE", Color.ORANGE, new PotionEffect(PotionEffectType.SATURATION, 5, 0), "&6发光浆果汁", "", LoreBuilder.hunger(3));
    public static final SlimefunItemStack GOLDEN_APPLE_JUICE = new SlimefunItemStack("GOLDEN_APPLE_JUICE", Color.YELLOW, new PotionEffect(PotionEffectType.ABSORPTION, 20 * 20, 0), "&b金苹果汁");

    public static final SlimefunItemStack BEEF_JERKY = new SlimefunItemStack("BEEF_JERKY", Material.COOKED_BEEF, "&6牛肉干", "", "&fExtra saturating!");
    public static final SlimefunItemStack PORK_JERKY = new SlimefunItemStack("PORK_JERKY", Material.COOKED_PORKCHOP, "&6猪肉干", "", "&fExtra saturating!");
    public static final SlimefunItemStack CHICKEN_JERKY = new SlimefunItemStack("CHICKEN_JERKY", Material.COOKED_CHICKEN, "&6鸡肉干", "", "&fExtra saturating!");
    public static final SlimefunItemStack MUTTON_JERKY = new SlimefunItemStack("MUTTON_JERKY", Material.COOKED_MUTTON, "&6羊肉干", "", "&fExtra saturating!");
    public static final SlimefunItemStack RABBIT_JERKY = new SlimefunItemStack("RABBIT_JERKY", Material.COOKED_RABBIT, "&6兔肉干", "", "&fExtra saturating!");
    public static final SlimefunItemStack FISH_JERKY = new SlimefunItemStack("FISH_JERKY", Material.COOKED_COD, "&6鱼肉干", "", "&fExtra saturating!");

    public static final SlimefunItemStack KELP_COOKIE = new SlimefunItemStack("KELP_COOKIE", Material.COOKIE, "&2海带饼干");

    /* Christmas */
    public static final SlimefunItemStack CHRISTMAS_MILK = new SlimefunItemStack("CHRISTMAS_MILK", Color.WHITE, new PotionEffect(PotionEffectType.SATURATION, 4, 0), "&6一杯牛奶", "", LoreBuilder.hunger(2.5));
    public static final SlimefunItemStack CHRISTMAS_CHOCOLATE_MILK = new SlimefunItemStack("CHRISTMAS_CHOCOLATE_MILK", Color.MAROON, new PotionEffect(PotionEffectType.SATURATION, 11, 0), "&6巧克力牛奶", "", LoreBuilder.hunger(6));
    public static final SlimefunItemStack CHRISTMAS_EGG_NOG = new SlimefunItemStack("CHRISTMAS_EGG_NOG", Color.GRAY, new PotionEffect(PotionEffectType.SATURATION, 6, 0), "&a蛋奶酒", "", LoreBuilder.hunger(3.5));
    public static final SlimefunItemStack CHRISTMAS_APPLE_CIDER = new SlimefunItemStack("CHRISTMAS_APPLE_CIDER", Color.RED, new PotionEffect(PotionEffectType.SATURATION, 13, 0), "&c苹果西打", "", LoreBuilder.hunger(7));
    public static final SlimefunItemStack CHRISTMAS_COOKIE = new SlimefunItemStack("CHRISTMAS_COOKIE", Material.COOKIE, ChatUtils.christmas("圣诞节饼干"));
    public static final SlimefunItemStack CHRISTMAS_FRUIT_CAKE = new SlimefunItemStack("CHRISTMAS_FRUIT_CAKE", Material.PUMPKIN_PIE, ChatUtils.christmas("圣诞水果蛋糕"));
    public static final SlimefunItemStack CHRISTMAS_APPLE_PIE = new SlimefunItemStack("CHRISTMAS_APPLE_PIE", Material.PUMPKIN_PIE, "&f苹果派");
    public static final SlimefunItemStack CHRISTMAS_HOT_CHOCOLATE = new SlimefunItemStack("CHRISTMAS_HOT_CHOCOLATE", Color.MAROON, new PotionEffect(PotionEffectType.SATURATION, 13, 0), "&6热巧克力", "", LoreBuilder.hunger(7));
    public static final SlimefunItemStack CHRISTMAS_CAKE = new SlimefunItemStack("CHRISTMAS_CAKE", Material.PUMPKIN_PIE, ChatUtils.christmas("圣诞蛋糕"));
    public static final SlimefunItemStack CHRISTMAS_CARAMEL = new SlimefunItemStack("CHRISTMAS_CARAMEL", Material.BRICK, "&6焦糖");
    public static final SlimefunItemStack CHRISTMAS_CARAMEL_APPLE = new SlimefunItemStack("CHRISTMAS_CARAMEL_APPLE", Material.APPLE, "&6焦糖苹果");
    public static final SlimefunItemStack CHRISTMAS_CHOCOLATE_APPLE = new SlimefunItemStack("CHRISTMAS_CHOCOLATE_APPLE", Material.APPLE, "&6巧克力苹果");
    public static final SlimefunItemStack CHRISTMAS_PRESENT = new SlimefunItemStack("CHRISTMAS_PRESENT", HeadTexture.CHRISTMAS_PRESENT, ChatUtils.christmas("Christmas Present"), "&7来自：&cTheBusyBiscuit", "&7To: &eYou", "", LoreBuilder.RIGHT_CLICK_TO_OPEN);

    /* Easter */
    public static final SlimefunItemStack EASTER_EGG = new SlimefunItemStack("EASTER_EGG", HeadTexture.EASTER_EGG, "&f复活节彩蛋", "&dHappy Easter! Have a surprise.", "", LoreBuilder.RIGHT_CLICK_TO_OPEN);
    public static final SlimefunItemStack EASTER_CARROT_PIE = new SlimefunItemStack("CARROT_PIE", Material.PUMPKIN_PIE, "&6胡萝卜派");
    public static final SlimefunItemStack EASTER_APPLE_PIE = new SlimefunItemStack("EASTER_APPLE_PIE", Material.PUMPKIN_PIE, "&f苹果派");

    /* Weapons */
    public static final SlimefunItemStack GRANDMAS_WALKING_STICK = new SlimefunItemStack("GRANDMAS_WALKING_STICK", Material.STICK, "&7奶奶的手杖");
    public static final SlimefunItemStack GRANDPAS_WALKING_STICK = new SlimefunItemStack("GRANDPAS_WALKING_STICK", Material.STICK, "&7爷爷的手杖");
    public static final SlimefunItemStack SWORD_OF_BEHEADING = new SlimefunItemStack("SWORD_OF_BEHEADING", Material.IRON_SWORD, "&6斩首之剑", "&7Beheading II", "", "&fHas a chance to behead Mobs", "&f(even a higher chance for Wither Skeletons)");
    public static final SlimefunItemStack BLADE_OF_VAMPIRES = new SlimefunItemStack("BLADE_OF_VAMPIRES", Material.GOLDEN_SWORD, "&c吸血鬼之刃", "&7Life Steal I", "", "&fEverytime you attack something", "&fyou have a 45% chance to", "&frecover 2 Hearts of your Health");
    public static final SlimefunItemStack SEISMIC_AXE = new SlimefunItemStack("SEISMIC_AXE", Material.IRON_AXE, "&a地震斧", "", "&7&oA portable Earthquake...", "", LoreBuilder.RIGHT_CLICK_TO_USE);

    static {
        GRANDMAS_WALKING_STICK.addUnsafeEnchantment(Enchantment.KNOCKBACK, 2);
        GRANDPAS_WALKING_STICK.addUnsafeEnchantment(Enchantment.KNOCKBACK, 5);

        BLADE_OF_VAMPIRES.addUnsafeEnchantment(Enchantment.FIRE_ASPECT, 2);
        BLADE_OF_VAMPIRES.addUnsafeEnchantment(VersionedEnchantment.UNBREAKING, 4);
        BLADE_OF_VAMPIRES.addUnsafeEnchantment(VersionedEnchantment.SHARPNESS, 2);
    }

    /* Bows */
    public static final SlimefunItemStack EXPLOSIVE_BOW = new SlimefunItemStack("EXPLOSIVE_BOW", Material.BOW, "&c爆炸之弓", "&fAny Arrows fired using this Bow", "&fwill launch hit enemys into the air");
    public static final SlimefunItemStack ICY_BOW = new SlimefunItemStack("ICY_BOW", Material.BOW, "&b冰霜之弓", "&fAny Arrows fired using this Bow", "&fwill prevent hit enemys from moving", "&ffor 2 seconds");

    /* Tools */
    public static final SlimefunItemStack SMELTERS_PICKAXE = new SlimefunItemStack("SMELTERS_PICKAXE", Material.DIAMOND_PICKAXE, "&6冶炼工之镐", "&c&lAuto-Smelting", "", "&9Works with Fortune");
    public static final SlimefunItemStack LUMBER_AXE = new SlimefunItemStack("LUMBER_AXE", Material.DIAMOND_AXE, "&6伐木斧", "&a&oCuts down the whole Tree...");
    public static final SlimefunItemStack PICKAXE_OF_CONTAINMENT = new SlimefunItemStack("PICKAXE_OF_CONTAINMENT", Material.IRON_PICKAXE, "&c收容之镐", "", "&9Can pickup Spawners");
    public static final SlimefunItemStack EXPLOSIVE_PICKAXE = new SlimefunItemStack("EXPLOSIVE_PICKAXE", Material.DIAMOND_PICKAXE, "&e爆炸镐", "", "&fAllows you to mine a good bit", "&fof Blocks at once...", "", "&9Works with Fortune");
    public static final SlimefunItemStack EXPLOSIVE_SHOVEL = new SlimefunItemStack("EXPLOSIVE_SHOVEL", Material.DIAMOND_SHOVEL, "&e爆炸铲", "", "&fAllows you to mine a good bit", "&fof diggable Blocks at once...");
    public static final SlimefunItemStack PICKAXE_OF_THE_SEEKER = new SlimefunItemStack("PICKAXE_OF_THE_SEEKER", Material.DIAMOND_PICKAXE, "&a寻矿者之镐", "&fWill always point you to the nearest Ore", "&fbut might get damaged when doing it", "", "&7&eRight Click&7 to be pointed to the nearest Ore");
    public static final SlimefunItemStack COBALT_PICKAXE = new SlimefunItemStack("COBALT_PICKAXE", Material.IRON_PICKAXE, "&9钴镐");
    public static final SlimefunItemStack PICKAXE_OF_VEIN_MINING = new SlimefunItemStack("PICKAXE_OF_VEIN_MINING", Material.DIAMOND_PICKAXE, "&e连锁挖矿镐", "", "&fThis Pickaxe will dig out", "&fwhole Veins of Ores...");
    public static final SlimefunItemStack CLIMBING_PICK = new SlimefunItemStack("CLIMBING_PICK", Material.IRON_PICKAXE, "&b攀岩镐", "", "&fAllows you to climb certain surfaces", "&fby right-clicking.", "&fEnchant this pick with Efficiency to", "&fclimb even faster!");

    static {
        COBALT_PICKAXE.addUnsafeEnchantment(VersionedEnchantment.UNBREAKING, 10);
        COBALT_PICKAXE.addUnsafeEnchantment(VersionedEnchantment.EFFICIENCY, 6);
    }

    /* Armor */
    public static final SlimefunItemStack GLOWSTONE_HELMET = new SlimefunItemStack("GLOWSTONE_HELMET", Material.LEATHER_HELMET, Color.YELLOW, "&e&l萤石头盔", "", "&a&oShining like the sun!", "", "&9+ Night Vision");
    public static final SlimefunItemStack GLOWSTONE_CHESTPLATE = new SlimefunItemStack("GLOWSTONE_CHESTPLATE", Material.LEATHER_CHESTPLATE, Color.YELLOW, "&e&l萤石胸甲", "", "&a&oShining like the sun!", "", "&9+ Night Vision");
    public static final SlimefunItemStack GLOWSTONE_LEGGINGS = new SlimefunItemStack("GLOWSTONE_LEGGINGS", Material.LEATHER_LEGGINGS, Color.YELLOW, "&e&l萤石护腿", "", "&a&oShining like the sun!", "", "&9+ Night Vision");
    public static final SlimefunItemStack GLOWSTONE_BOOTS = new SlimefunItemStack("GLOWSTONE_BOOTS", Material.LEATHER_BOOTS, Color.YELLOW, "&e&l萤石靴子", "", "&a&oShining like the sun!", "", "&9+ Night Vision");
    public static final SlimefunItemStack RAINBOW_LEATHER = new SlimefunItemStack("RAINBOW_LEATHER", Material.RABBIT_HIDE, Color.FUCHSIA, "&d彩虹皮革", "", "&fCan be used to craft rainbow armor");
    public static final SlimefunItemStack RAINBOW_HELMET = new SlimefunItemStack("RAINBOW_HELMET", Material.LEATHER_HELMET, Color.FUCHSIA, "&d&l彩虹头盔", "", LoreBuilder.RAINBOW);
    public static final SlimefunItemStack RAINBOW_CHESTPLATE = new SlimefunItemStack("RAINBOW_CHESTPLATE", Material.LEATHER_CHESTPLATE, Color.FUCHSIA, "&d&l彩虹胸甲", "", LoreBuilder.RAINBOW);
    public static final SlimefunItemStack RAINBOW_LEGGINGS = new SlimefunItemStack("RAINBOW_LEGGINGS", Material.LEATHER_LEGGINGS, Color.FUCHSIA, "&d&l彩虹护腿", "", LoreBuilder.RAINBOW);
    public static final SlimefunItemStack RAINBOW_BOOTS = new SlimefunItemStack("RAINBOW_BOOTS", Material.LEATHER_BOOTS, Color.FUCHSIA, "&d&l彩虹靴子", "", LoreBuilder.RAINBOW);
    public static final SlimefunItemStack ENDER_HELMET = new SlimefunItemStack("ENDER_HELMET", Material.LEATHER_HELMET, Color.fromRGB(28, 25, 112), "&5&l末影头盔", "", "&a&oSometimes its here, sometimes there!");
    public static final SlimefunItemStack ENDER_CHESTPLATE = new SlimefunItemStack("ENDER_CHESTPLATE", Material.LEATHER_CHESTPLATE, Color.fromRGB(28, 25, 112), "&5&l末影胸甲", "", "&a&oSometimes its here, sometimes there!");
    public static final SlimefunItemStack ENDER_LEGGINGS = new SlimefunItemStack("ENDER_LEGGINGS", Material.LEATHER_LEGGINGS, Color.fromRGB(28, 25, 112), "&5&l末影护腿", "", "&a&oSometimes its here, sometimes there!");
    public static final SlimefunItemStack ENDER_BOOTS = new SlimefunItemStack("ENDER_BOOTS", Material.LEATHER_BOOTS, Color.fromRGB(28, 25, 112), "&5&l末影靴子", "", "&a&oSometimes its here, sometimes there!", "", "&9+ No Enderpearl Damage");

    public static final SlimefunItemStack SLIME_HELMET = new SlimefunItemStack("SLIME_HELMET", Material.LEATHER_HELMET, Color.LIME, "&a&l史莱姆头盔", "", "&a&oBouncy Feeling");
    public static final SlimefunItemStack SLIME_CHESTPLATE = new SlimefunItemStack("SLIME_CHESTPLATE", Material.LEATHER_CHESTPLATE, Color.LIME, "&a&l史莱姆胸甲", "", "&a&oBouncy Feeling");
    public static final SlimefunItemStack SLIME_LEGGINGS = new SlimefunItemStack("SLIME_LEGGINGS", Material.LEATHER_LEGGINGS, Color.LIME, "&a&l史莱姆护腿", "", "&a&oBouncy Feeling", "", "&9+ Speed");
    public static final SlimefunItemStack SLIME_BOOTS = new SlimefunItemStack("SLIME_BOOTS", Material.LEATHER_BOOTS, Color.LIME, "&a&l史莱姆靴子", "", "&a&oBouncy Feeling", "", "&9+ Jump Boost", "&9+ No Fall Damage");

    public static final SlimefunItemStack CACTUS_HELMET = new SlimefunItemStack("CACTUS_HELMET", Material.LEATHER_HELMET, Color.GREEN, "&2仙人掌头盔");
    public static final SlimefunItemStack CACTUS_CHESTPLATE = new SlimefunItemStack("CACTUS_CHESTPLATE", Material.LEATHER_CHESTPLATE, Color.GREEN, "&2仙人掌胸甲");
    public static final SlimefunItemStack CACTUS_LEGGINGS = new SlimefunItemStack("CACTUS_LEGGINGS", Material.LEATHER_LEGGINGS, Color.GREEN, "&2仙人掌护腿");
    public static final SlimefunItemStack CACTUS_BOOTS = new SlimefunItemStack("CACTUS_BOOTS", Material.LEATHER_BOOTS, Color.GREEN, "&2仙人掌靴子");

    public static final SlimefunItemStack DAMASCUS_STEEL_HELMET = new SlimefunItemStack("DAMASCUS_STEEL_HELMET", Material.IRON_HELMET, "&7大马士革钢头盔");
    public static final SlimefunItemStack DAMASCUS_STEEL_CHESTPLATE = new SlimefunItemStack("DAMASCUS_STEEL_CHESTPLATE", Material.IRON_CHESTPLATE, "&7大马士革钢胸甲");
    public static final SlimefunItemStack DAMASCUS_STEEL_LEGGINGS = new SlimefunItemStack("DAMASCUS_STEEL_LEGGINGS", Material.IRON_LEGGINGS, "&7大马士革钢护腿");
    public static final SlimefunItemStack DAMASCUS_STEEL_BOOTS = new SlimefunItemStack("DAMASCUS_STEEL_BOOTS", Material.IRON_BOOTS, "&7大马士革钢靴子");

    public static final SlimefunItemStack REINFORCED_ALLOY_HELMET = new SlimefunItemStack("REINFORCED_ALLOY_HELMET", Material.IRON_HELMET, "&b强化合金头盔");
    public static final SlimefunItemStack REINFORCED_ALLOY_CHESTPLATE = new SlimefunItemStack("REINFORCED_ALLOY_CHESTPLATE", Material.IRON_CHESTPLATE, "&b强化合金胸甲");
    public static final SlimefunItemStack REINFORCED_ALLOY_LEGGINGS = new SlimefunItemStack("REINFORCED_ALLOY_LEGGINGS", Material.IRON_LEGGINGS, "&b强化合金护腿");
    public static final SlimefunItemStack REINFORCED_ALLOY_BOOTS = new SlimefunItemStack("REINFORCED_ALLOY_BOOTS", Material.IRON_BOOTS, "&b强化合金靴子");

    private static final List<String> hazmatLore = new ArrayList<>();

    static {
        hazmatLore.add("");
        hazmatLore.add(ChatColor.GOLD + "Full set effects:");
        hazmatLore.add(ChatColor.YELLOW + "- Radiation immunity");
        hazmatLore.add(ChatColor.YELLOW + "- Bee Sting protection");
    }

    public static final SlimefunItemStack SCUBA_HELMET = new SlimefunItemStack("SCUBA_HELMET", Material.LEATHER_HELMET, Color.ORANGE, "&c潜水头盔", "", "&7Allows you to breathe underwater");
    public static final SlimefunItemStack HAZMAT_CHESTPLATE = new SlimefunItemStack("HAZMAT_CHESTPLATE", Material.LEATHER_CHESTPLATE, Color.ORANGE, "&c防化服", "", "&7Allows you to walk through fire and lava");
    public static final SlimefunItemStack HAZMAT_LEGGINGS = new SlimefunItemStack("HAZMAT_LEGGINGS", Material.LEATHER_LEGGINGS, Color.ORANGE, "&c防化服护腿", hazmatLore.toArray(new String[0]));
    public static final SlimefunItemStack HAZMAT_BOOTS = new SlimefunItemStack("RUBBER_BOOTS", Material.LEATHER_BOOTS, Color.BLACK, "&c防化靴子", hazmatLore.toArray(new String[0]));

    static {
        ItemMeta helmetMeta = SCUBA_HELMET.getItemMeta();
        List<String> helmetLore = helmetMeta.getLore();
        helmetLore.addAll(hazmatLore);
        helmetMeta.setLore(helmetLore);
        SCUBA_HELMET.setItemMeta(helmetMeta);

        ItemMeta chestplateMeta = HAZMAT_CHESTPLATE.getItemMeta();
        List<String> chestplateLore = chestplateMeta.getLore();
        chestplateLore.addAll(hazmatLore);
        chestplateMeta.setLore(chestplateLore);
        HAZMAT_CHESTPLATE.setItemMeta(chestplateMeta);
    }

    public static final SlimefunItemStack GILDED_IRON_HELMET = new SlimefunItemStack("GILDED_IRON_HELMET", Material.GOLDEN_HELMET, "&6镀金铁头盔");
    public static final SlimefunItemStack GILDED_IRON_CHESTPLATE = new SlimefunItemStack("GILDED_IRON_CHESTPLATE", Material.GOLDEN_CHESTPLATE, "&6镀金铁胸甲");
    public static final SlimefunItemStack GILDED_IRON_LEGGINGS = new SlimefunItemStack("GILDED_IRON_LEGGINGS", Material.GOLDEN_LEGGINGS, "&6镀金铁护腿");
    public static final SlimefunItemStack GILDED_IRON_BOOTS = new SlimefunItemStack("GILDED_IRON_BOOTS", Material.GOLDEN_BOOTS, "&6镀金铁靴子");

    public static final SlimefunItemStack GOLDEN_HELMET_12K = new SlimefunItemStack("GOLD_12K_HELMET", Material.GOLDEN_HELMET, "&6黄金头盔 &7(12K)");
    public static final SlimefunItemStack GOLDEN_CHESTPLATE_12K = new SlimefunItemStack("GOLD_12K_CHESTPLATE", Material.GOLDEN_CHESTPLATE, "&6黄金胸甲 &7(12K)");
    public static final SlimefunItemStack GOLDEN_LEGGINGS_12K = new SlimefunItemStack("GOLD_12K_LEGGINGS", Material.GOLDEN_LEGGINGS, "&6黄金护腿 &7(12K)");
    public static final SlimefunItemStack GOLDEN_BOOTS_12K = new SlimefunItemStack("GOLD_12K_BOOTS", Material.GOLDEN_BOOTS, "&6黄金靴子 &7(12K)");

    public static final SlimefunItemStack SLIME_HELMET_STEEL = new SlimefunItemStack("SLIME_STEEL_HELMET", Material.IRON_HELMET, "&a&l史莱姆钢头盔", "&7&oReinforced", "", "&a&oBouncy Feeling");
    public static final SlimefunItemStack SLIME_CHESTPLATE_STEEL = new SlimefunItemStack("SLIME_STEEL_CHESTPLATE", Material.IRON_CHESTPLATE, "&a&l史莱姆钢胸甲", "&7&oReinforced", "", "&a&oBouncy Feeling");
    public static final SlimefunItemStack SLIME_LEGGINGS_STEEL = new SlimefunItemStack("SLIME_STEEL_LEGGINGS", Material.IRON_LEGGINGS, "&a&l史莱姆钢护腿", "&7&oReinforced", "", "&a&oBouncy Feeling", "", "&9+ Speed");
    public static final SlimefunItemStack SLIME_BOOTS_STEEL = new SlimefunItemStack("SLIME_STEEL_BOOTS", Material.IRON_BOOTS, "&a&l史莱姆钢靴子", "&7&oReinforced", "", "&a&oBouncy Feeling", "", "&9+ Jump Boost", "&9+ No Fall Damage");

    public static final SlimefunItemStack BOOTS_OF_THE_STOMPER = new SlimefunItemStack("BOOTS_OF_THE_STOMPER", Material.LEATHER_BOOTS, Color.AQUA, "&b践踏者之靴", "", "&9All Fall Damage you receive", "&9will be applied to nearby Mobs/Players", "", "&9+ No Fall Damage");

    public static final SlimefunItemStack BEE_HELMET = new SlimefunItemStack("BEE_HELMET", Material.GOLDEN_HELMET, "&e&l蜜蜂头盔", " ", "&fBzzzzzzz");
    public static final SlimefunItemStack BEE_WINGS = new SlimefunItemStack("BEE_WINGS", Material.ELYTRA, "&e&l蜜蜂之翼", " ", "&fBzzzzzzz", " ", "&9Activates Slow falling", "&9when approaching the ground");
    public static final SlimefunItemStack BEE_LEGGINGS = new SlimefunItemStack("BEE_LEGGINGS", Material.GOLDEN_LEGGINGS, "&e&l蜜蜂护腿", " ", "&fBzzzzzzz");
    public static final SlimefunItemStack BEE_BOOTS = new SlimefunItemStack("BEE_BOOTS", Material.GOLDEN_BOOTS, "&e&l蜜蜂靴子", "", "&fBzzzzzzz", "", "&9+ Jump Boost", "&9+ No Fall Damage");

    static {
        Map<Enchantment, Integer> cactusEnchs = new HashMap<>();
        cactusEnchs.put(Enchantment.THORNS, 3);
        cactusEnchs.put(VersionedEnchantment.UNBREAKING, 6);

        CACTUS_HELMET.addUnsafeEnchantments(cactusEnchs);
        CACTUS_CHESTPLATE.addUnsafeEnchantments(cactusEnchs);
        CACTUS_LEGGINGS.addUnsafeEnchantments(cactusEnchs);
        CACTUS_BOOTS.addUnsafeEnchantments(cactusEnchs);

        Map<Enchantment, Integer> damascusEnchs = new HashMap<>();
        damascusEnchs.put(VersionedEnchantment.UNBREAKING, 5);
        damascusEnchs.put(VersionedEnchantment.PROTECTION, 5);

        DAMASCUS_STEEL_HELMET.addUnsafeEnchantments(damascusEnchs);
        DAMASCUS_STEEL_CHESTPLATE.addUnsafeEnchantments(damascusEnchs);
        DAMASCUS_STEEL_LEGGINGS.addUnsafeEnchantments(damascusEnchs);
        DAMASCUS_STEEL_BOOTS.addUnsafeEnchantments(damascusEnchs);

        Map<Enchantment, Integer> reinforcedEnchs = new HashMap<>();
        reinforcedEnchs.put(VersionedEnchantment.UNBREAKING, 9);
        reinforcedEnchs.put(VersionedEnchantment.PROTECTION, 9);

        REINFORCED_ALLOY_HELMET.addUnsafeEnchantments(reinforcedEnchs);
        REINFORCED_ALLOY_CHESTPLATE.addUnsafeEnchantments(reinforcedEnchs);
        REINFORCED_ALLOY_LEGGINGS.addUnsafeEnchantments(reinforcedEnchs);
        REINFORCED_ALLOY_BOOTS.addUnsafeEnchantments(reinforcedEnchs);

        Map<Enchantment, Integer> gildedEnchs = new HashMap<>();
        gildedEnchs.put(VersionedEnchantment.UNBREAKING, 6);
        gildedEnchs.put(VersionedEnchantment.PROTECTION, 8);

        GILDED_IRON_HELMET.addUnsafeEnchantments(gildedEnchs);
        GILDED_IRON_CHESTPLATE.addUnsafeEnchantments(gildedEnchs);
        GILDED_IRON_LEGGINGS.addUnsafeEnchantments(gildedEnchs);
        GILDED_IRON_BOOTS.addUnsafeEnchantments(gildedEnchs);

        GOLDEN_HELMET_12K.addUnsafeEnchantment(VersionedEnchantment.UNBREAKING, 10);
        GOLDEN_CHESTPLATE_12K.addUnsafeEnchantment(VersionedEnchantment.UNBREAKING, 10);
        GOLDEN_LEGGINGS_12K.addUnsafeEnchantment(VersionedEnchantment.UNBREAKING, 10);
        GOLDEN_BOOTS_12K.addUnsafeEnchantment(VersionedEnchantment.UNBREAKING, 10);

        Map<Enchantment, Integer> slimeEnchs = new HashMap<>();
        slimeEnchs.put(VersionedEnchantment.UNBREAKING, 4);
        slimeEnchs.put(VersionedEnchantment.PROTECTION, 2);

        SLIME_HELMET_STEEL.addUnsafeEnchantments(slimeEnchs);
        SLIME_CHESTPLATE_STEEL.addUnsafeEnchantments(slimeEnchs);
        SLIME_LEGGINGS_STEEL.addUnsafeEnchantments(slimeEnchs);
        SLIME_BOOTS_STEEL.addUnsafeEnchantments(slimeEnchs);

        Map<Enchantment, Integer> beeEnchs = new HashMap<>();
        beeEnchs.put(VersionedEnchantment.UNBREAKING, 4);
        beeEnchs.put(VersionedEnchantment.PROTECTION, 2);

        BEE_HELMET.addUnsafeEnchantments(beeEnchs);
        BEE_WINGS.addUnsafeEnchantments(beeEnchs);
        BEE_LEGGINGS.addUnsafeEnchantments(beeEnchs);
        BEE_BOOTS.addUnsafeEnchantments(beeEnchs);
    }

    /* Magical components */
    public static final SlimefunItemStack MAGIC_LUMP_1 = new SlimefunItemStack("MAGIC_LUMP_1", Material.GOLD_NUGGET, "&6魔法 lump &7- &eI", "", "&c&oTier: I");
    public static final SlimefunItemStack MAGIC_LUMP_2 = new SlimefunItemStack("MAGIC_LUMP_2", Material.GOLD_NUGGET, "&6魔法 lump &7- &eII", "", "&c&oTier: II");
    public static final SlimefunItemStack MAGIC_LUMP_3 = new SlimefunItemStack("MAGIC_LUMP_3", Material.GOLD_NUGGET, "&6魔法 lump &7- &eIII", "", "&c&oTier: III");
    public static final SlimefunItemStack ENDER_LUMP_1 = new SlimefunItemStack("ENDER_LUMP_1", Material.GOLD_NUGGET, "&5末影 lump &7- &eI", "", "&c&oTier: I");
    public static final SlimefunItemStack ENDER_LUMP_2 = new SlimefunItemStack("ENDER_LUMP_2", Material.GOLD_NUGGET, "&5末影 lump &7- &eII", "", "&c&oTier: II");
    public static final SlimefunItemStack ENDER_LUMP_3 = new SlimefunItemStack("ENDER_LUMP_3", Material.GOLD_NUGGET, "&5末影 lump &7- &eIII", "", "&c&oTier: III");
    public static final SlimefunItemStack MAGICAL_BOOK_COVER = new SlimefunItemStack("MAGICAL_BOOK_COVER", Material.PAPER, "&6魔法书封皮", "", "&a&oUsed for various Magic Books");
    public static final SlimefunItemStack MAGICAL_GLASS = new SlimefunItemStack("MAGICAL_GLASS", Material.GLASS_PANE, "&6魔法玻璃", "", "&a&oUsed for various Magical Gadgets");
    public static final SlimefunItemStack SYNTHETIC_SHULKER_SHELL = new SlimefunItemStack("SYNTHETIC_SHULKER_SHELL", Material.SHULKER_SHELL, "&d合成潜影壳", "", "&fThis item can be used in a", "&fworkbench like a normal Shulker Shell");

    /* Technical components */
    public static final SlimefunItemStack BASIC_CIRCUIT_BOARD = new SlimefunItemStack("BASIC_CIRCUIT_BOARD", Material.ACTIVATOR_RAIL, "&b基础电路板");
    public static final SlimefunItemStack ADVANCED_CIRCUIT_BOARD = new SlimefunItemStack("ADVANCED_CIRCUIT_BOARD", Material.POWERED_RAIL, "&b高级电路板");
    public static final SlimefunItemStack WHEAT_FLOUR = new SlimefunItemStack("WHEAT_FLOUR", Material.SUGAR, "&f小麦粉");
    public static final SlimefunItemStack STEEL_PLATE = new SlimefunItemStack("STEEL_PLATE", Material.PAPER, "&7&l钢板");
    public static final SlimefunItemStack BATTERY = new SlimefunItemStack("BATTERY", HeadTexture.BATTERY, "&6电池");
    public static final SlimefunItemStack CARBON = new SlimefunItemStack("CARBON", HeadTexture.CARBON, "&e碳");
    public static final SlimefunItemStack COMPRESSED_CARBON = new SlimefunItemStack("COMPRESSED_CARBON", HeadTexture.COMPRESSED_CARBON, "&c压缩碳");
    public static final SlimefunItemStack CARBON_CHUNK = new SlimefunItemStack("CARBON_CHUNK", HeadTexture.COMPRESSED_CARBON, "&4碳块");
    public static final SlimefunItemStack STEEL_THRUSTER = new SlimefunItemStack("STEEL_THRUSTER", Material.BUCKET, "&7&l钢制推进器");
    public static final SlimefunItemStack POWER_CRYSTAL = new SlimefunItemStack("POWER_CRYSTAL", HeadTexture.POWER_CRYSTAL, "&c&l能量水晶");
    public static final SlimefunItemStack CHAIN = new SlimefunItemStack("CHAIN", Material.STRING, "&b锁链");
    public static final SlimefunItemStack HOOK = new SlimefunItemStack("HOOK", Material.FLINT, "&b钩子");
    public static final SlimefunItemStack SIFTED_ORE = new SlimefunItemStack("SIFTED_ORE", Material.GUNPOWDER, "&6筛分矿石");
    public static final SlimefunItemStack STONE_CHUNK = new SlimefunItemStack("STONE_CHUNK", HeadTexture.STONE_CHUNK, "&6石块");
    public static final SlimefunItemStack LAVA_CRYSTAL = new SlimefunItemStack("LAVA_CRYSTAL", HeadTexture.LAVA_CRYSTAL, "&4岩浆水晶");
    public static final SlimefunItemStack SALT = new SlimefunItemStack("SALT", Material.SUGAR, "&f盐");
    public static final SlimefunItemStack CHEESE = new SlimefunItemStack("CHEESE", HeadTexture.CHEESE, "&f奶酪");
    public static final SlimefunItemStack BUTTER = new SlimefunItemStack("BUTTER", HeadTexture.BUTTER, "&f黄油");
    public static final SlimefunItemStack DUCT_TAPE = new SlimefunItemStack("DUCT_TAPE", HeadTexture.DUCT_TAPE, "&8胶带", "", "&fYou can repair Items using this", "&fin an Auto-Anvil");
    public static final SlimefunItemStack HEAVY_CREAM = new SlimefunItemStack("HEAVY_CREAM", Material.SNOWBALL, "&f浓奶油");
    public static final SlimefunItemStack CRUSHED_ORE = new SlimefunItemStack("CRUSHED_ORE", Material.GUNPOWDER, "&6粉碎矿石");
    public static final SlimefunItemStack PULVERIZED_ORE = new SlimefunItemStack("PULVERIZED_ORE", Material.GUNPOWDER, "&6研磨矿石");
    public static final SlimefunItemStack PURE_ORE_CLUSTER = new SlimefunItemStack("PURE_ORE_CLUSTER", Material.GUNPOWDER, "&6纯净矿石簇");
    public static final SlimefunItemStack SMALL_URANIUM = new SlimefunItemStack("SMALL_URANIUM", HeadTexture.URANIUM, "&c小块铀", "", LoreBuilder.radioactive(Radioactivity.MODERATE), LoreBuilder.HAZMAT_SUIT_REQUIRED);
    public static final SlimefunItemStack TINY_URANIUM = new SlimefunItemStack("TINY_URANIUM", HeadTexture.URANIUM, "&c微量铀", "", LoreBuilder.radioactive(Radioactivity.LOW));
    public static final SlimefunItemStack SOLAR_PANEL = new SlimefunItemStack("SOLAR_PANEL", Material.DAYLIGHT_DETECTOR, "&9光伏电池", "", "&7Important component for", "&7crafting a &bSolar Generator");
    public static final SlimefunItemStack PLASTIC_SHEET = new SlimefunItemStack("PLASTIC_SHEET", Material.PAPER, "&f塑料片");

    public static final SlimefunItemStack MAGNET = new SlimefunItemStack("MAGNET", HeadTexture.MAGNET, "&c磁铁");
    public static final SlimefunItemStack NECROTIC_SKULL = new SlimefunItemStack("NECROTIC_SKULL", HeadTexture.NECROTIC_SKULL, "&c坏死头颅");
    public static final SlimefunItemStack ESSENCE_OF_AFTERLIFE = new SlimefunItemStack("ESSENCE_OF_AFTERLIFE", Material.GUNPOWDER, "&4来世精华");
    public static final SlimefunItemStack STRANGE_NETHER_GOO = new SlimefunItemStack("STRANGE_NETHER_GOO", Material.PURPLE_DYE, "&5诡异下界黏液", "", "&fA strange bio matter that", "&fcan be acquired from", "&fbartering with Piglins");
    public static final SlimefunItemStack ELECTRO_MAGNET = new SlimefunItemStack("ELECTRO_MAGNET", HeadTexture.MAGNET, "&c电磁铁");
    public static final SlimefunItemStack HEATING_COIL = new SlimefunItemStack("HEATING_COIL", HeadTexture.HEATING_COIL, "&c加热线圈");
    public static final SlimefunItemStack COOLING_UNIT = new SlimefunItemStack("COOLING_UNIT", HeadTexture.COOLING_UNIT, "&b冷却单元");
    public static final SlimefunItemStack ELECTRIC_MOTOR = new SlimefunItemStack("ELECTRIC_MOTOR", HeadTexture.MOTOR, "&c电动机");
    public static final SlimefunItemStack CARGO_MOTOR = new SlimefunItemStack("CARGO_MOTOR", HeadTexture.CARGO_MOTOR, "&3货物马达", "", "&7Important ingredient for items", "&7related to Cargo Management");
    public static final SlimefunItemStack SCROLL_OF_DIMENSIONAL_TELEPOSITION = new SlimefunItemStack("SCROLL_OF_DIMENSIONAL_TELEPOSITION", Material.PAPER, "&6维度传送卷轴", "", "&cThis Scroll is capable of creating", "&ca temporary black Hole which pulls", "&cnearby Entities into itself and sends", "&cthem into another Dimension where", "&ceverything is turned around", "", "&fIn other words: Makes Entities turn by 180 Degrees");
    public static final SlimefunItemStack TOME_OF_KNOWLEDGE_SHARING = new SlimefunItemStack("TOME_OF_KNOWLEDGE_SHARING", Material.ENCHANTED_BOOK, "&6知识共享之书", "&7Owner: &bNone", "", "&eRight Click&7 to bind this Tome to yourself", "", "", "&eRight Click&7 to obtain all Researches by", "&7the previously assigned Owner");
    public static final SlimefunItemStack HARDENED_GLASS = new SlimefunItemStack("HARDENED_GLASS", Material.LIGHT_GRAY_STAINED_GLASS, "&7硬化玻璃", "", "&fWithstands Explosions");
    public static final SlimefunItemStack WITHER_PROOF_OBSIDIAN = new SlimefunItemStack("WITHER_PROOF_OBSIDIAN", Material.OBSIDIAN, "&5防凋零黑曜石", "", "&fWithstands Explosions", "&fWithstands Wither Bosses");
    public static final SlimefunItemStack WITHER_PROOF_GLASS = new SlimefunItemStack("WITHER_PROOF_GLASS", Material.PURPLE_STAINED_GLASS, "&5防凋零玻璃", "", "&fWithstands Explosions", "&fWithstands Wither Bosses");
    public static final SlimefunItemStack REINFORCED_PLATE = new SlimefunItemStack("REINFORCED_PLATE", Material.PAPER, "&7强化板");
    public static final SlimefunItemStack ANCIENT_PEDESTAL = new SlimefunItemStack("ANCIENT_PEDESTAL", Material.DISPENSER, "&d古代基座", "", "&5Part of the Ancient Altar");
    public static final SlimefunItemStack ANCIENT_ALTAR = new SlimefunItemStack("ANCIENT_ALTAR", Material.ENCHANTING_TABLE, "&d古代祭坛", "", "&5Multi-Block Altar for", "&5magical Crafting Processes");
    public static final SlimefunItemStack COPPER_WIRE = new SlimefunItemStack("COPPER_WIRE", Material.STRING, "&6铜线", "", "&6Crucial component in electric modules");
    public static final SlimefunItemStack CRAFTING_MOTOR = new SlimefunItemStack("CRAFTING_MOTOR", HeadTexture.CRAFTING_MOTOR, "&6合成马达", "", "&7Important component of Auto-Crafters");

    /* Rainbow blocks */
    public static final SlimefunItemStack RAINBOW_WOOL = new SlimefunItemStack("RAINBOW_WOOL", Material.WHITE_WOOL, "&5彩虹羊毛", "", LoreBuilder.RAINBOW);
    public static final SlimefunItemStack RAINBOW_GLASS = new SlimefunItemStack("RAINBOW_GLASS", Material.WHITE_STAINED_GLASS, "&5彩虹玻璃", "", LoreBuilder.RAINBOW);
    public static final SlimefunItemStack RAINBOW_CLAY = new SlimefunItemStack("RAINBOW_CLAY", Material.WHITE_TERRACOTTA, "&5彩虹黏土", "", LoreBuilder.RAINBOW);
    public static final SlimefunItemStack RAINBOW_GLASS_PANE = new SlimefunItemStack("RAINBOW_GLASS_PANE", Material.WHITE_STAINED_GLASS_PANE, "&5彩虹玻璃板", "", LoreBuilder.RAINBOW);
    public static final SlimefunItemStack RAINBOW_CONCRETE = new SlimefunItemStack("RAINBOW_CONCRETE", Material.WHITE_CONCRETE, "&5彩虹混凝土", "", LoreBuilder.RAINBOW);
    public static final SlimefunItemStack RAINBOW_GLAZED_TERRACOTTA = new SlimefunItemStack("RAINBOW_GLAZED_TERRACOTTA", Material.WHITE_GLAZED_TERRACOTTA, "&5彩虹带釉陶瓦", "", LoreBuilder.RAINBOW);
    /* Seasonal */
    private static final String CHRISTMAS = ChatUtils.christmas("[Christmas Edition]");
    public static final SlimefunItemStack RAINBOW_WOOL_XMAS = new SlimefunItemStack("RAINBOW_WOOL_XMAS", Material.WHITE_WOOL, "&5彩虹羊毛 &7(圣诞节)", "", CHRISTMAS);
    public static final SlimefunItemStack RAINBOW_GLASS_XMAS = new SlimefunItemStack("RAINBOW_GLASS_XMAS", Material.WHITE_STAINED_GLASS, "&5彩虹玻璃 &7(圣诞节)", "", CHRISTMAS);
    public static final SlimefunItemStack RAINBOW_CLAY_XMAS = new SlimefunItemStack("RAINBOW_CLAY_XMAS", Material.WHITE_TERRACOTTA, "&5彩虹黏土 &7(圣诞节)", "", CHRISTMAS);
    public static final SlimefunItemStack RAINBOW_GLASS_PANE_XMAS = new SlimefunItemStack("RAINBOW_GLASS_PANE_XMAS", Material.WHITE_STAINED_GLASS_PANE, "&5彩虹玻璃板 &7(圣诞节)", "", CHRISTMAS);
    public static final SlimefunItemStack RAINBOW_CONCRETE_XMAS = new SlimefunItemStack("RAINBOW_CONCRETE_XMAS", Material.WHITE_CONCRETE, "&5彩虹混凝土 &7(圣诞节)", "", CHRISTMAS);
    public static final SlimefunItemStack RAINBOW_GLAZED_TERRACOTTA_XMAS = new SlimefunItemStack("RAINBOW_GLAZED_TERRACOTTA_XMAS", Material.WHITE_GLAZED_TERRACOTTA, "&5彩虹带釉陶瓦 &7(圣诞节)", "", CHRISTMAS);

    private static final String VALENTINES_DAY = "&5[&dValentine's Day Edition&5]";
    public static final SlimefunItemStack RAINBOW_WOOL_VALENTINE = new SlimefunItemStack("RAINBOW_WOOL_VALENTINE", Material.PINK_WOOL, "&5彩虹羊毛 &7(情人节)", "", VALENTINES_DAY);
    public static final SlimefunItemStack RAINBOW_GLASS_VALENTINE = new SlimefunItemStack("RAINBOW_GLASS_VALENTINE", Material.PINK_STAINED_GLASS, "&5彩虹玻璃 &7(情人节)", "", VALENTINES_DAY);
    public static final SlimefunItemStack RAINBOW_CLAY_VALENTINE = new SlimefunItemStack("RAINBOW_CLAY_VALENTINE", Material.PINK_TERRACOTTA, "&5彩虹黏土 &7(情人节)", "", VALENTINES_DAY);
    public static final SlimefunItemStack RAINBOW_GLASS_PANE_VALENTINE = new SlimefunItemStack("RAINBOW_GLASS_PANE_VALENTINE", Material.PINK_STAINED_GLASS_PANE, "&5彩虹玻璃板 &7(情人节)", "", VALENTINES_DAY);
    public static final SlimefunItemStack RAINBOW_CONCRETE_VALENTINE = new SlimefunItemStack("RAINBOW_CONCRETE_VALENTINE", Material.PINK_CONCRETE, "&5彩虹混凝土 &7(情人节)", "", VALENTINES_DAY);
    public static final SlimefunItemStack RAINBOW_GLAZED_TERRACOTTA_VALENTINE = new SlimefunItemStack("RAINBOW_GLAZED_TERRACOTTA_VALENTINE", Material.PINK_GLAZED_TERRACOTTA, "&5彩虹带釉陶瓦 &7(情人节)", "", VALENTINES_DAY);

    private static final String HALLOWEEN = "&c[&6Halloween Edition&c]";
    public static final SlimefunItemStack RAINBOW_WOOL_HALLOWEEN = new SlimefunItemStack("RAINBOW_WOOL_HALLOWEEN", Material.ORANGE_WOOL, "&5彩虹羊毛 &7(万圣节)", "", HALLOWEEN);
    public static final SlimefunItemStack RAINBOW_GLASS_HALLOWEEN = new SlimefunItemStack("RAINBOW_GLASS_HALLOWEEN", Material.ORANGE_STAINED_GLASS, "&5彩虹玻璃 &7(万圣节)", "", HALLOWEEN);
    public static final SlimefunItemStack RAINBOW_CLAY_HALLOWEEN = new SlimefunItemStack("RAINBOW_CLAY_HALLOWEEN", Material.ORANGE_TERRACOTTA, "&5彩虹黏土 &7(万圣节)", "", HALLOWEEN);
    public static final SlimefunItemStack RAINBOW_GLASS_PANE_HALLOWEEN = new SlimefunItemStack("RAINBOW_GLASS_PANE_HALLOWEEN", Material.ORANGE_STAINED_GLASS_PANE, "&5彩虹玻璃板 &7(万圣节)", "", HALLOWEEN);
    public static final SlimefunItemStack RAINBOW_CONCRETE_HALLOWEEN = new SlimefunItemStack("RAINBOW_CONCRETE_HALLOWEEN", Material.ORANGE_CONCRETE, "&5彩虹混凝土 &7(万圣节)", "", HALLOWEEN);
    public static final SlimefunItemStack RAINBOW_GLAZED_TERRACOTTA_HALLOWEEN = new SlimefunItemStack("RAINBOW_GLAZED_TERRACOTTA_HALLOWEEN", Material.ORANGE_GLAZED_TERRACOTTA, "&5彩虹带釉陶瓦 &7(万圣节)", "", HALLOWEEN);

    /* Ingots */
    public static final SlimefunItemStack COPPER_INGOT = new SlimefunItemStack("COPPER_INGOT", Material.BRICK, "&b铜锭");
    public static final SlimefunItemStack TIN_INGOT = new SlimefunItemStack("TIN_INGOT", Material.IRON_INGOT, "&b锡锭");
    public static final SlimefunItemStack SILVER_INGOT = new SlimefunItemStack("SILVER_INGOT", Material.IRON_INGOT, "&b银锭");
    public static final SlimefunItemStack ALUMINUM_INGOT = new SlimefunItemStack("ALUMINUM_INGOT", Material.IRON_INGOT, "&b铝锭");
    public static final SlimefunItemStack LEAD_INGOT = new SlimefunItemStack("LEAD_INGOT", Material.IRON_INGOT, "&b铅锭");
    public static final SlimefunItemStack ZINC_INGOT = new SlimefunItemStack("ZINC_INGOT", Material.IRON_INGOT, "&b锌锭");
    public static final SlimefunItemStack MAGNESIUM_INGOT = new SlimefunItemStack("MAGNESIUM_INGOT", Material.IRON_INGOT, "&b镁锭");

    /* Alloy (Carbon + Iron) */
    public static final SlimefunItemStack STEEL_INGOT = new SlimefunItemStack("STEEL_INGOT", Material.IRON_INGOT, "&b钢锭");
    /* Alloy (Copper + Tin) */
    public static final SlimefunItemStack BRONZE_INGOT = new SlimefunItemStack("BRONZE_INGOT", Material.BRICK, "&b青铜锭");
    /* Alloy (Copper + Aluminum) */
    public static final SlimefunItemStack DURALUMIN_INGOT = new SlimefunItemStack("DURALUMIN_INGOT", Material.IRON_INGOT, "&b硬铝锭");
    /* Alloy (Copper + Silver) */
    public static final SlimefunItemStack BILLON_INGOT = new SlimefunItemStack("BILLON_INGOT", Material.IRON_INGOT, "&b比尔隆合金锭");
    /* Alloy (Copper + Zinc) */
    public static final SlimefunItemStack BRASS_INGOT = new SlimefunItemStack("BRASS_INGOT", Material.GOLD_INGOT, "&b黄铜锭");
    /* Alloy (Aluminum + Brass) */
    public static final SlimefunItemStack ALUMINUM_BRASS_INGOT = new SlimefunItemStack("ALUMINUM_BRASS_INGOT", Material.GOLD_INGOT, "&b铝黄铜锭");
    /* Alloy (Aluminum + Bronze) */
    public static final SlimefunItemStack ALUMINUM_BRONZE_INGOT = new SlimefunItemStack("ALUMINUM_BRONZE_INGOT", Material.GOLD_INGOT, "&b铝青铜锭");
    /* Alloy (Gold + Silver + Copper) */
    public static final SlimefunItemStack CORINTHIAN_BRONZE_INGOT = new SlimefunItemStack("CORINTHIAN_BRONZE_INGOT", Material.GOLD_INGOT, "&b科林斯青铜锭");
    /* Alloy (Lead + Tin) */
    public static final SlimefunItemStack SOLDER_INGOT = new SlimefunItemStack("SOLDER_INGOT", Material.IRON_INGOT, "&b焊料锭");
    /* Alloy (Steel + Iron + Carbon) */
    public static final SlimefunItemStack DAMASCUS_STEEL_INGOT = new SlimefunItemStack("DAMASCUS_STEEL_INGOT", Material.IRON_INGOT, "&b大马士革钢锭");
    /* Alloy (Damascus Steel + Duralumin + Compressed Carbon + Aluminium Bronze) */
    public static final SlimefunItemStack HARDENED_METAL_INGOT = new SlimefunItemStack("HARDENED_METAL_INGOT", Material.IRON_INGOT, "&b&l硬化金属锭");
    /* Alloy (Hardened Metal + Corinthian Bronze + Solder + Billon + Damascus Steel) */
    public static final SlimefunItemStack REINFORCED_ALLOY_INGOT = new SlimefunItemStack("REINFORCED_ALLOY_INGOT", Material.IRON_INGOT, "&b&l强化合金锭");
    /* Alloy (Iron + Silicon) */
    public static final SlimefunItemStack FERROSILICON = new SlimefunItemStack("FERROSILICON", Material.IRON_INGOT, "&b铁硅合金");
    /* Alloy (Iron + Gold) */
    public static final SlimefunItemStack GILDED_IRON = new SlimefunItemStack("GILDED_IRON", Material.GOLD_INGOT, "&6&l镀金铁");
    /* Alloy (Redstone + Ferrosilicon) */
    public static final SlimefunItemStack REDSTONE_ALLOY = new SlimefunItemStack("REDSTONE_ALLOY", Material.BRICK, "&c红石合金锭");
    /* Alloy (Iron + Copper) */
    public static final SlimefunItemStack NICKEL_INGOT = new SlimefunItemStack("NICKEL_INGOT", Material.IRON_INGOT, "&b镍锭");
    /* Alloy (Nickel + Iron + Copper) */
    public static final SlimefunItemStack COBALT_INGOT = new SlimefunItemStack("COBALT_INGOT", Material.IRON_INGOT, "&9钴锭");

    /* Gold */
    public static final SlimefunItemStack GOLD_4K = new SlimefunItemStack("GOLD_4K", Material.GOLD_INGOT, "&f金锭 &7(4K)");
    public static final SlimefunItemStack GOLD_6K = new SlimefunItemStack("GOLD_6K", Material.GOLD_INGOT, "&f金锭 &7(6K)");
    public static final SlimefunItemStack GOLD_8K = new SlimefunItemStack("GOLD_8K", Material.GOLD_INGOT, "&f金锭 &7(8K)");
    public static final SlimefunItemStack GOLD_10K = new SlimefunItemStack("GOLD_10K", Material.GOLD_INGOT, "&f金锭 &7(10K)");
    public static final SlimefunItemStack GOLD_12K = new SlimefunItemStack("GOLD_12K", Material.GOLD_INGOT, "&f金锭 &7(12K)");
    public static final SlimefunItemStack GOLD_14K = new SlimefunItemStack("GOLD_14K", Material.GOLD_INGOT, "&f金锭 &7(14K)");
    public static final SlimefunItemStack GOLD_16K = new SlimefunItemStack("GOLD_16K", Material.GOLD_INGOT, "&f金锭 &7(16K)");
    public static final SlimefunItemStack GOLD_18K = new SlimefunItemStack("GOLD_18K", Material.GOLD_INGOT, "&f金锭 &7(18K)");
    public static final SlimefunItemStack GOLD_20K = new SlimefunItemStack("GOLD_20K", Material.GOLD_INGOT, "&f金锭 &7(20K)");
    public static final SlimefunItemStack GOLD_22K = new SlimefunItemStack("GOLD_22K", Material.GOLD_INGOT, "&f金锭 &7(22K)");
    public static final SlimefunItemStack GOLD_24K = new SlimefunItemStack("GOLD_24K", Material.GOLD_INGOT, "&f金锭 &7(24K)");

    /* Dusts */
    public static final SlimefunItemStack IRON_DUST = new SlimefunItemStack("IRON_DUST", Material.GUNPOWDER, "&6铁粉");
    public static final SlimefunItemStack GOLD_DUST = new SlimefunItemStack("GOLD_DUST", Material.GLOWSTONE_DUST, "&6金粉");
    public static final SlimefunItemStack TIN_DUST = new SlimefunItemStack("TIN_DUST", Material.SUGAR, "&6锡粉");
    public static final SlimefunItemStack COPPER_DUST = new SlimefunItemStack("COPPER_DUST", Material.GLOWSTONE_DUST, "&6铜粉");
    public static final SlimefunItemStack SILVER_DUST = new SlimefunItemStack("SILVER_DUST", Material.SUGAR, "&6银粉");
    public static final SlimefunItemStack ALUMINUM_DUST = new SlimefunItemStack("ALUMINUM_DUST", Material.SUGAR, "&6铝粉");
    public static final SlimefunItemStack LEAD_DUST = new SlimefunItemStack("LEAD_DUST", Material.GUNPOWDER, "&6铅粉");
    public static final SlimefunItemStack ZINC_DUST = new SlimefunItemStack("ZINC_DUST", Material.SUGAR, "&6锌粉");
    public static final SlimefunItemStack MAGNESIUM_DUST = new SlimefunItemStack("MAGNESIUM_DUST", Material.SUGAR, "&6镁粉");

    public static final SlimefunItemStack SULFATE = new SlimefunItemStack("SULFATE", Material.GLOWSTONE_DUST, "&6硫酸盐");
    public static final SlimefunItemStack SILICON = new SlimefunItemStack("SILICON", Material.FIREWORK_STAR, "&6硅");
    public static final SlimefunItemStack GOLD_24K_BLOCK = new SlimefunItemStack("GOLD_24K_BLOCK", Material.GOLD_BLOCK, "&f金块 &7(24K)");

    /* Gems */
    public static final SlimefunItemStack SYNTHETIC_DIAMOND = new SlimefunItemStack("SYNTHETIC_DIAMOND", Material.DIAMOND, "&b合成钻石", "", "&fThis item can be used in a", "&fworkbench and acts like a normal Diamond");
    public static final SlimefunItemStack SYNTHETIC_EMERALD = new SlimefunItemStack("SYNTHETIC_EMERALD", Material.EMERALD, "&b合成绿宝石", "", "&fThis item can be used to", "&ftrade with Villagers");
    public static final SlimefunItemStack SYNTHETIC_SAPPHIRE = new SlimefunItemStack("SYNTHETIC_SAPPHIRE", HeadTexture.SAPPHIRE, "&b合成蓝宝石", "", "&fThis item can be used in a", "&fworkbench and acts like Lapis Lazuli");
    public static final SlimefunItemStack CARBONADO = new SlimefunItemStack("CARBONADO", HeadTexture.CARBONADO, "&b&l黑金刚石", "", "&7&o\"Black Diamond\"");
    public static final SlimefunItemStack RAW_CARBONADO = new SlimefunItemStack("RAW_CARBONADO", HeadTexture.RAW_CARBONADO, "&b原始黑金刚石");

    public static final SlimefunItemStack URANIUM = new SlimefunItemStack("URANIUM", HeadTexture.URANIUM, "&4铀", "", LoreBuilder.radioactive(Radioactivity.HIGH), LoreBuilder.HAZMAT_SUIT_REQUIRED);
    public static final SlimefunItemStack NEPTUNIUM = new SlimefunItemStack("NEPTUNIUM", HeadTexture.NEPTUNIUM, "&a镎", "", LoreBuilder.radioactive(Radioactivity.HIGH), LoreBuilder.HAZMAT_SUIT_REQUIRED);
    public static final SlimefunItemStack PLUTONIUM = new SlimefunItemStack("PLUTONIUM", HeadTexture.PLUTONIUM, "&7钚", "", LoreBuilder.radioactive(Radioactivity.VERY_HIGH), LoreBuilder.HAZMAT_SUIT_REQUIRED);
    public static final SlimefunItemStack BOOSTED_URANIUM = new SlimefunItemStack("BOOSTED_URANIUM", HeadTexture.BOOSTED_URANIUM, "&2增殖铀", "", LoreBuilder.radioactive(Radioactivity.VERY_HIGH), LoreBuilder.HAZMAT_SUIT_REQUIRED);

    /* Talisman */
    public static final SlimefunItemStack COMMON_TALISMAN = new SlimefunItemStack("COMMON_TALISMAN", Material.EMERALD, "&6普通护身符");
    public static final SlimefunItemStack ENDER_TALISMAN = new SlimefunItemStack("ENDER_TALISMAN", Material.EMERALD, "&5末影护身符");

    public static final SlimefunItemStack TALISMAN_ANVIL = new SlimefunItemStack("ANVIL_TALISMAN", Material.EMERALD, "&a铁砧护身符", "", "&fEach Talisman can prevent", "&f1 Tool from breaking, but will then", "&fbe consumed", "", "&4&lWARNING:", "&4This Talisman does not work on", "&4Tools which are too powerful", "&4due to their complexity");
    public static final SlimefunItemStack TALISMAN_MINER = new SlimefunItemStack("MINER_TALISMAN", Material.EMERALD, "&a矿工护身符", "", "&fWhile you have this Talisman", "&fin your Inventory it has", "&fa 20% chance of doubling", "&fall Ores you mine");
    public static final SlimefunItemStack TALISMAN_FARMER = new SlimefunItemStack("FARMER_TALISMAN", Material.EMERALD, "&a农夫护身符", "", "&fWhile you have this Talisman", "&fin your Inventory it has", "&fa 20% chance of doubling", "&fall crops you harvest");
    public static final SlimefunItemStack TALISMAN_HUNTER = new SlimefunItemStack("HUNTER_TALISMAN", Material.EMERALD, "&a猎人护身符", "", "&fWhile you have this Talisman", "&fin your Inventory it has", "&fa 20% chance of doubling", "&fall Drops from Mobs you kill");
    public static final SlimefunItemStack TALISMAN_LAVA = new SlimefunItemStack("LAVA_TALISMAN", Material.EMERALD, "&a熔岩行者护身符", "", "&fWhile you have this Talisman", "&fin your Inventory it will", "&fgive you Fire Resistance", "&fas soon as you touch Lava", "&fbut will then be consumed");
    public static final SlimefunItemStack TALISMAN_WATER = new SlimefunItemStack("WATER_TALISMAN", Material.EMERALD, "&a水下呼吸护身符", "", "&fWhile you have this Talisman", "&fin your Inventory it will", "&fgive you the ability", "&fto breath underwater as", "&fsoon as you start drowning", "&fbut will then be consumed");
    public static final SlimefunItemStack TALISMAN_ANGEL = new SlimefunItemStack("ANGEL_TALISMAN", Material.EMERALD, "&a天使护身符", "", "&fWhile you have this Talisman", "&fin your Inventory it has a", "&f75% chance to prevent you", "&ffrom taking Fall Damage");
    public static final SlimefunItemStack TALISMAN_FIRE = new SlimefunItemStack("FIRE_TALISMAN", Material.EMERALD, "&a消防员护身符", "", "&fWhile you have this Talisman", "&fin your Inventory it will", "&fgive you Fire Resistance", "&fas soon as you start burning", "&fbut will then be consumed");
    public static final SlimefunItemStack TALISMAN_MAGICIAN = new SlimefunItemStack("MAGICIAN_TALISMAN", Material.EMERALD, "&a魔术师护身符", "", "&fWhile you have this Talisman", "&fin your Inventory it gives", "&fyou a 80% Luck Bonus on Enchanting", "&fYou will sometimes get an Extra Enchantment");
    public static final SlimefunItemStack TALISMAN_TRAVELLER = new SlimefunItemStack("TRAVELLER_TALISMAN", Material.EMERALD, "&a旅行者护身符", "", "&fWhile you have this Talisman", "&fin your Inventory it gives", "&fyou a 60% Chance for a decent", "&fSpeed Buff when you start sprinting");
    public static final SlimefunItemStack TALISMAN_WARRIOR = new SlimefunItemStack("WARRIOR_TALISMAN", Material.EMERALD, "&a战士护身符", "", "&fWhile you have this Talisman", "&fin your Inventory it gives", "&fyou Strength III whenever you get hit", "&fbut will then be consumed");
    public static final SlimefunItemStack TALISMAN_KNIGHT = new SlimefunItemStack("KNIGHT_TALISMAN", Material.EMERALD, "&a骑士护身符", "", "&fWhile you have this Talisman", "&fin your Inventory it gives", "&fyou a 30% Chance for 5 Seconds of Regeneration", "&fwhenever You get hit", "&fbut will then be consumed");
    public static final SlimefunItemStack TALISMAN_WHIRLWIND = new SlimefunItemStack("WHIRLWIND_TALISMAN", Material.EMERALD, "&a旋风护身符", "", "&fHaving this Talisman", "&fin your Inventory will reflect", "&f60% of any projectiles fired at you.", "&e&oOnly a thrown Trident can pierce", "&e&othrough this layer of protection");
    public static final SlimefunItemStack TALISMAN_WIZARD = new SlimefunItemStack("WIZARD_TALISMAN", Material.EMERALD, "&a巫师护身符", "", "&fWhile you have this Talisman", "&fin your Inventory it allows you to", "&fobtain Fortune Level 4/5 however", "&fit also has a chance to lower the", "&fLevel of some Enchantments on your Item");
    public static final SlimefunItemStack TALISMAN_CAVEMAN = new SlimefunItemStack("CAVEMAN_TALISMAN", Material.EMERALD, "&a穴居人护身符", "", "&fWhile you have this Talisman", "&fin your inventory it gives", "&fyou a 50% chance for a decent", "&fHaste buff when you mine any ore");
    public static final SlimefunItemStack TALISMAN_WISE = new SlimefunItemStack("WISE_TALISMAN", Material.EMERALD, "&a智者护身符", "", "&fWhile you have this Talisman", "&fin your inventory it gives", "&fyou a 20% chance of doubling", "&fany experience you obtain");

    /* Staves */
    public static final SlimefunItemStack STAFF_ELEMENTAL = new SlimefunItemStack("STAFF_ELEMENTAL", Material.STICK, "&6元素法杖");
    public static final SlimefunItemStack STAFF_WIND = new SlimefunItemStack("STAFF_ELEMENTAL_WIND", Material.STICK, "&6元素法杖 &7- &b&o风", "", "&7Element: &b&oWind", "", "&eRight Click&7 to launch yourself forward");
    public static final SlimefunItemStack STAFF_FIRE = new SlimefunItemStack("STAFF_ELEMENTAL_FIRE", Material.STICK, "&6元素法杖 &7- &c&o火", "", "&7Element: &c&oFire");
    public static final SlimefunItemStack STAFF_WATER = new SlimefunItemStack("STAFF_ELEMENTAL_WATER", Material.STICK, "&6元素法杖 &7- &1&o水", "", "&7Element: &1&oWater", "", "&eRight Click&7 to extinguish yourself");
    public static final SlimefunItemStack STAFF_STORM = new SlimefunItemStack("STAFF_ELEMENTAL_STORM", Material.STICK, "&6元素法杖 &7- &8&o雷暴", "", "&7Element: &8&oStorm", "", "&eRight Click&7 to summon a lightning", LoreBuilder.usesLeft(StormStaff.MAX_USES));

    static {
        STAFF_WIND.addUnsafeEnchantment(VersionedEnchantment.LUCK_OF_THE_SEA, 1);
        STAFF_FIRE.addUnsafeEnchantment(Enchantment.FIRE_ASPECT, 5);
        STAFF_WATER.addUnsafeEnchantment(VersionedEnchantment.AQUA_AFFINITY, 1);
        STAFF_STORM.addUnsafeEnchantment(VersionedEnchantment.UNBREAKING, 1);
    }

    /* Multiblocks */
    public static final SlimefunItemStack ENHANCED_CRAFTING_TABLE = new SlimefunItemStack("ENHANCED_CRAFTING_TABLE", Material.CRAFTING_TABLE, "&e强化合成台", "", "&aA regular Crafting Table cannot", "&ahold this massive Amount of Power...");
    public static final SlimefunItemStack GRIND_STONE = new SlimefunItemStack("GRIND_STONE", Material.DISPENSER, "&b磨石", "", "&aGrinds items down into other items");
    public static final SlimefunItemStack ARMOR_FORGE = new SlimefunItemStack("ARMOR_FORGE", Material.ANVIL, "&6盔甲锻造台", "", "&aGives you the ability to create powerful armor");
    public static final SlimefunItemStack MAKESHIFT_SMELTERY = new SlimefunItemStack("MAKESHIFT_SMELTERY", Material.BLAST_FURNACE, "&e简易冶炼炉", "", "&fImprovised version of the Smeltery", "&fthat only allows you to", "&fsmelt dusts into ingots");
    public static final SlimefunItemStack SMELTERY = new SlimefunItemStack("SMELTERY", Material.FURNACE, "&6冶炼炉", "", "&fA high-temperature furnace", "&fthat allows you to smelt dusts", "&finto ingots and create alloys.");
    public static final SlimefunItemStack ORE_CRUSHER = new SlimefunItemStack("ORE_CRUSHER", Material.DISPENSER, "&b矿石粉碎机", "", "&aCrushes ores to double them");
    public static final SlimefunItemStack COMPRESSOR = new SlimefunItemStack("COMPRESSOR", Material.PISTON, "&b压缩机", "", "&aCompresses Items");
    public static final SlimefunItemStack PRESSURE_CHAMBER = new SlimefunItemStack("PRESSURE_CHAMBER", Material.GLASS, "&b压力舱", "", "&aCompresses Items even further");
    public static final SlimefunItemStack MAGIC_WORKBENCH = new SlimefunItemStack("MAGIC_WORKBENCH", Material.CRAFTING_TABLE, "&6魔法工作台", "", "&dInfuses Items with magical Energy");
    public static final SlimefunItemStack ORE_WASHER = new SlimefunItemStack("ORE_WASHER", Material.CAULDRON, "&6洗矿机", "", "&aWashes Sifted Ore to filter Ores", "&aand gives you small Stone Chunks");
    public static final SlimefunItemStack TABLE_SAW = new SlimefunItemStack("TABLE_SAW", Material.STONECUTTER, "&6台锯", "", "&aAllows you to get 8 planks from 1 Log", "&a(Works with all log types)");
    public static final SlimefunItemStack JUICER = new SlimefunItemStack("JUICER", Material.GLASS_BOTTLE, "&a榨汁机", "", "&aAllows you to create delicious Juice");
    public static final SlimefunItemStack AUTOMATED_PANNING_MACHINE = new SlimefunItemStack("AUTOMATED_PANNING_MACHINE", Material.BOWL, "&e自动淘金机", "", "&fA MultiBlock Version of the Gold Pan", "&fand Nether Gold Pan combined in one machine.");

    public static final SlimefunItemStack INDUSTRIAL_MINER = new SlimefunItemStack("INDUSTRIAL_MINER", Material.GOLDEN_PICKAXE, "&b工业采矿机", "", "&fThis Multiblock will mine any Ores", "&fin a 7x7 area underneath it.", "&fPlace coal or similar in its chest", "&fto fuel this machine.");
    public static final SlimefunItemStack ADVANCED_INDUSTRIAL_MINER = new SlimefunItemStack("ADVANCED_INDUSTRIAL_MINER", Material.DIAMOND_PICKAXE, "&c高级工业采矿机", "", "&fThis Multiblock will mine any Ores", "&fin a 11x11 area underneath it.", "&fPlace a bucket of fuel or lava in", "&fits chest to fuel this machine.", "", "&a+ Silk Touch");

    static {
        ItemMeta meta = INDUSTRIAL_MINER.getItemMeta();
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        INDUSTRIAL_MINER.setItemMeta(meta);

        ItemMeta meta2 = ADVANCED_INDUSTRIAL_MINER.getItemMeta();
        meta2.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        ADVANCED_INDUSTRIAL_MINER.setItemMeta(meta2);
    }

    /* Machines */
    public static final SlimefunItemStack COMPOSTER = new SlimefunItemStack("COMPOSTER", Material.CAULDRON, "&a堆肥桶", "", "&a&oCan convert various Materials over Time...");
    public static final SlimefunItemStack CRUCIBLE = new SlimefunItemStack("CRUCIBLE", Material.CAULDRON, "&c坩埚", "", "&a&oUsed to smelt Items into Liquids");
    public static final SlimefunItemStack OUTPUT_CHEST = new SlimefunItemStack("OUTPUT_CHEST", Material.CHEST, "&4输出箱", "", "&c&oA basic machine will try to put", "&c&oitems in this chest if it's placed", "&c&oadjacent to the dispenser.");
    public static final SlimefunItemStack IGNITION_CHAMBER = new SlimefunItemStack("IGNITION_CHAMBER", Material.DROPPER, "&4自动点火室", "", "&fPrevents the Smeltery from using up fire.", "&fJust fill it up with \"Flint and Steel\"", "&fand place it adjacent to the Smeltery's dispenser");
    public static final SlimefunItemStack HOLOGRAM_PROJECTOR = new SlimefunItemStack("HOLOGRAM_PROJECTOR", Material.QUARTZ_SLAB, "&b全息投影仪", "", "&fProjects an Editable Hologram");
    public static final SlimefunItemStack BLOCK_PLACER = new SlimefunItemStack("BLOCK_PLACER", Material.DISPENSER, "&a方块放置器", "", "&fAll Blocks in this Dispenser", "&fwill automatically get placed");

    /* Enhanced Furnaces */
    public static final SlimefunItemStack ENHANCED_FURNACE = new SlimefunItemStack("ENHANCED_FURNACE", Material.FURNACE, "&7强化熔炉 - &eI", "", "&7Processing Speed: &e1x", "&7Fuel Efficiency: &e1x", "&7Luck Multiplier: &e1x");
    public static final SlimefunItemStack ENHANCED_FURNACE_2 = new SlimefunItemStack("ENHANCED_FURNACE_2", Material.FURNACE, "&7强化熔炉 - &eII", "", "&7Processing Speed: &e2x", "&7Fuel Efficiency: &e1x", "&7Luck Multiplier: &e1x");
    public static final SlimefunItemStack ENHANCED_FURNACE_3 = new SlimefunItemStack("ENHANCED_FURNACE_3", Material.FURNACE, "&7强化熔炉 - &eIII", "", "&7Processing Speed: &e2x", "&7Fuel Efficiency: &e2x", "&7Luck Multiplier: &e1x");
    public static final SlimefunItemStack ENHANCED_FURNACE_4 = new SlimefunItemStack("ENHANCED_FURNACE_4", Material.FURNACE, "&7强化熔炉 - &eIV", "", "&7Processing Speed: &e3x", "&7Fuel Efficiency: &e2x", "&7Luck Multiplier: &e1x");
    public static final SlimefunItemStack ENHANCED_FURNACE_5 = new SlimefunItemStack("ENHANCED_FURNACE_5", Material.FURNACE, "&7强化熔炉 - &eV", "", "&7Processing Speed: &e3x", "&7Fuel Efficiency: &e2x", "&7Luck Multiplier: &e2x");
    public static final SlimefunItemStack ENHANCED_FURNACE_6 = new SlimefunItemStack("ENHANCED_FURNACE_6", Material.FURNACE, "&7强化熔炉 - &eVI", "", "&7Processing Speed: &e3x", "&7Fuel Efficiency: &e3x", "&7Luck Multiplier: &e2x");
    public static final SlimefunItemStack ENHANCED_FURNACE_7 = new SlimefunItemStack("ENHANCED_FURNACE_7", Material.FURNACE, "&7强化熔炉 - &eVII", "", "&7Processing Speed: &e4x", "&7Fuel Efficiency: &e3x", "&7Luck Multiplier: &e2x");
    public static final SlimefunItemStack ENHANCED_FURNACE_8 = new SlimefunItemStack("ENHANCED_FURNACE_8", Material.FURNACE, "&7强化熔炉 - &eVIII", "", "&7Processing Speed: &e4x", "&7Fuel Efficiency: &e4x", "&7Luck Multiplier: &e2x");
    public static final SlimefunItemStack ENHANCED_FURNACE_9 = new SlimefunItemStack("ENHANCED_FURNACE_9", Material.FURNACE, "&7强化熔炉 - &eIX", "", "&7Processing Speed: &e5x", "&7Fuel Efficiency: &e4x", "&7Luck Multiplier: &e2x");
    public static final SlimefunItemStack ENHANCED_FURNACE_10 = new SlimefunItemStack("ENHANCED_FURNACE_10", Material.FURNACE, "&7强化熔炉 - &eX", "", "&7Processing Speed: &e5x", "&7Fuel Efficiency: &e5x", "&7Luck Multiplier: &e2x");
    public static final SlimefunItemStack ENHANCED_FURNACE_11 = new SlimefunItemStack("ENHANCED_FURNACE_11", Material.FURNACE, "&7强化熔炉 - &eXI", "", "&7Processing Speed: &e5x", "&7Fuel Efficiency: &e5x", "&7Luck Multiplier: &e3x");
    public static final SlimefunItemStack REINFORCED_FURNACE = new SlimefunItemStack("REINFORCED_FURNACE", Material.FURNACE, "&7强化（高级）熔炉", "", "&7Processing Speed: &e10x", "&7Fuel Efficiency: &e10x", "&7Luck Multiplier: &e3x");
    public static final SlimefunItemStack CARBONADO_EDGED_FURNACE = new SlimefunItemStack("CARBONADO_EDGED_FURNACE", Material.FURNACE, "&7黑金刚石刃口熔炉", "", "&7Processing Speed: &e20x", "&7Fuel Efficiency: &e10x", "&7Luck Multiplier: &e3x");

    /* Soulbound Items */
    public static final SlimefunItemStack SOULBOUND_SWORD = new SlimefunItemStack("SOULBOUND_SWORD", Material.DIAMOND_SWORD, "&c灵魂绑定剑");
    public static final SlimefunItemStack SOULBOUND_BOW = new SlimefunItemStack("SOULBOUND_BOW", Material.BOW, "&c灵魂绑定弓");
    public static final SlimefunItemStack SOULBOUND_PICKAXE = new SlimefunItemStack("SOULBOUND_PICKAXE", Material.DIAMOND_PICKAXE, "&c灵魂绑定镐");
    public static final SlimefunItemStack SOULBOUND_AXE = new SlimefunItemStack("SOULBOUND_AXE", Material.DIAMOND_AXE, "&c灵魂绑定斧");
    public static final SlimefunItemStack SOULBOUND_SHOVEL = new SlimefunItemStack("SOULBOUND_SHOVEL", Material.DIAMOND_SHOVEL, "&c灵魂绑定铲");
    public static final SlimefunItemStack SOULBOUND_HOE = new SlimefunItemStack("SOULBOUND_HOE", Material.DIAMOND_HOE, "&c灵魂绑定锄");
    public static final SlimefunItemStack SOULBOUND_TRIDENT = new SlimefunItemStack("SOULBOUND_TRIDENT", Material.TRIDENT, "&c灵魂绑定三叉戟");

    public static final SlimefunItemStack SOULBOUND_HELMET = new SlimefunItemStack("SOULBOUND_HELMET", Material.DIAMOND_HELMET, "&c灵魂绑定头盔");
    public static final SlimefunItemStack SOULBOUND_CHESTPLATE = new SlimefunItemStack("SOULBOUND_CHESTPLATE", Material.DIAMOND_CHESTPLATE, "&c灵魂绑定胸甲");
    public static final SlimefunItemStack SOULBOUND_LEGGINGS = new SlimefunItemStack("SOULBOUND_LEGGINGS", Material.DIAMOND_LEGGINGS, "&c灵魂绑定护腿");
    public static final SlimefunItemStack SOULBOUND_BOOTS = new SlimefunItemStack("SOULBOUND_BOOTS", Material.DIAMOND_BOOTS, "&c灵魂绑定靴子");

    /* Runes */
    public static final SlimefunItemStack BLANK_RUNE = new SlimefunItemStack("BLANK_RUNE", ColoredFireworkStar.create(Color.BLACK, "&8空白符文"));

    public static final SlimefunItemStack AIR_RUNE = new SlimefunItemStack("ANCIENT_RUNE_AIR", ColoredFireworkStar.create(Color.AQUA, "&7古代符文 &8&l[&b&l风&8&l]"));
    public static final SlimefunItemStack WATER_RUNE = new SlimefunItemStack("ANCIENT_RUNE_WATER", ColoredFireworkStar.create(Color.BLUE, "&7古代符文 &8&l[&1&l水&8&l]"));
    public static final SlimefunItemStack FIRE_RUNE = new SlimefunItemStack("ANCIENT_RUNE_FIRE", ColoredFireworkStar.create(Color.RED, "&7古代符文 &8&l[&4&l火&8&l]"));
    public static final SlimefunItemStack EARTH_RUNE = new SlimefunItemStack("ANCIENT_RUNE_EARTH", ColoredFireworkStar.create(Color.fromRGB(112, 47, 7), "&7古代符文 &8&l[&c&l土&8&l]"));
    public static final SlimefunItemStack ENDER_RUNE = new SlimefunItemStack("ANCIENT_RUNE_ENDER", ColoredFireworkStar.create(Color.PURPLE, "&7古代符文 &8&l[&5&l末影&8&l]"));

    public static final SlimefunItemStack RAINBOW_RUNE = new SlimefunItemStack("ANCIENT_RUNE_RAINBOW", ColoredFireworkStar.create(Color.FUCHSIA, "&7古代符文 &8&l[&d&l彩虹&8&l]"));
    public static final SlimefunItemStack LIGHTNING_RUNE = new SlimefunItemStack("ANCIENT_RUNE_LIGHTNING", ColoredFireworkStar.create(Color.fromRGB(255, 255, 95), "&7古代符文 &8&l[&e&l闪电&8&l]"));
    public static final SlimefunItemStack SOULBOUND_RUNE = new SlimefunItemStack("ANCIENT_RUNE_SOULBOUND", ColoredFireworkStar.create(Color.fromRGB(47, 0, 117), "&7古代符文 &8&l[&5&l灵魂绑定&8&l]", "&eDrop this rune onto a dropped item to", "&5bind &ethat item to your soul.", " ", "&eIt is advised that you only use this rune", "&eon &6important &eitems.", " ", "&eItems bound to your soul won't drop on death."));
    public static final SlimefunItemStack ENCHANTMENT_RUNE = new SlimefunItemStack("ANCIENT_RUNE_ENCHANTMENT", ColoredFireworkStar.create(Color.fromRGB(255, 217, 25), "&7古代符文 &8&l[&6&l附魔&8&l]", "&eDrop this rune onto a dropped item to", "&6enchant &ethat item with a random enchantment."));
    public static final SlimefunItemStack VILLAGER_RUNE = new SlimefunItemStack("ANCIENT_RUNE_VILLAGERS", ColoredFireworkStar.create(Color.fromRGB(160, 20, 5), "&7古代符文 &8&l[&4&l村民&8&l]", "&eRight click a villager to clear", "&etheir current job and trades.", "&eThe villager will start looking", "&efor a job again after some", "&etime has passed."));

    /* Electricity */
    public static final SlimefunItemStack SOLAR_GENERATOR = new SlimefunItemStack("SOLAR_GENERATOR", Material.DAYLIGHT_DETECTOR, "&b太阳能发电机", "", LoreBuilder.machine(MachineTier.BASIC, MachineType.GENERATOR), LoreBuilder.powerBuffer(0), LoreBuilder.powerPerSecond(4));
    public static final SlimefunItemStack SOLAR_GENERATOR_2 = new SlimefunItemStack("SOLAR_GENERATOR_2", Material.DAYLIGHT_DETECTOR, "&c高级太阳能发电机", "", LoreBuilder.machine(MachineTier.MEDIUM, MachineType.GENERATOR), LoreBuilder.powerBuffer(0), LoreBuilder.powerPerSecond(16));
    public static final SlimefunItemStack SOLAR_GENERATOR_3 = new SlimefunItemStack("SOLAR_GENERATOR_3", Material.DAYLIGHT_DETECTOR, "&4黑金刚石太阳能发电机", "", LoreBuilder.machine(MachineTier.END_GAME, MachineType.GENERATOR), LoreBuilder.powerBuffer(0), LoreBuilder.powerPerSecond(64));
    public static final SlimefunItemStack SOLAR_GENERATOR_4 = new SlimefunItemStack("SOLAR_GENERATOR_4", Material.DAYLIGHT_DETECTOR, "&e充能太阳能发电机", "", "&9Works at Night", "", LoreBuilder.machine(MachineTier.END_GAME, MachineType.GENERATOR), LoreBuilder.powerBuffer(0), LoreBuilder.powerPerSecond(256) + " (Day)", LoreBuilder.powerPerSecond(128) + " (Night)");

    public static final SlimefunItemStack COAL_GENERATOR = new SlimefunItemStack("COAL_GENERATOR", HeadTexture.GENERATOR, "&c燃煤发电机", "", LoreBuilder.machine(MachineTier.AVERAGE, MachineType.GENERATOR), LoreBuilder.powerBuffer(64), LoreBuilder.powerPerSecond(16));
    public static final SlimefunItemStack COAL_GENERATOR_2 = new SlimefunItemStack("COAL_GENERATOR_2", HeadTexture.GENERATOR, "&c燃煤发电机 &7(&eII&7)", "", LoreBuilder.machine(MachineTier.ADVANCED, MachineType.GENERATOR), LoreBuilder.powerBuffer(256), LoreBuilder.powerPerSecond(30));

    public static final SlimefunItemStack LAVA_GENERATOR = new SlimefunItemStack("LAVA_GENERATOR", HeadTexture.GENERATOR, "&4岩浆发电机", "", LoreBuilder.machine(MachineTier.AVERAGE, MachineType.GENERATOR), LoreBuilder.powerBuffer(512), LoreBuilder.powerPerSecond(20));
    public static final SlimefunItemStack LAVA_GENERATOR_2 = new SlimefunItemStack("LAVA_GENERATOR_2", HeadTexture.GENERATOR, "&4岩浆发电机 &7(&eII&7)", "", LoreBuilder.machine(MachineTier.ADVANCED, MachineType.GENERATOR), LoreBuilder.powerBuffer(1024), LoreBuilder.powerPerSecond(40));

    public static final SlimefunItemStack ELECTRIC_FURNACE = new SlimefunItemStack("ELECTRIC_FURNACE", Material.FURNACE, "&c电炉", "", LoreBuilder.machine(MachineTier.BASIC, MachineType.MACHINE), LoreBuilder.speed(1), LoreBuilder.powerPerSecond(4));
    public static final SlimefunItemStack ELECTRIC_FURNACE_2 = new SlimefunItemStack("ELECTRIC_FURNACE_2", Material.FURNACE, "&c电炉 &7- &eII", "", LoreBuilder.machine(MachineTier.MEDIUM, MachineType.MACHINE), LoreBuilder.speed(2), LoreBuilder.powerPerSecond(6));
    public static final SlimefunItemStack ELECTRIC_FURNACE_3 = new SlimefunItemStack("ELECTRIC_FURNACE_3", Material.FURNACE, "&c电炉 &7- &eIII", "", LoreBuilder.machine(MachineTier.MEDIUM, MachineType.MACHINE), LoreBuilder.speed(4), LoreBuilder.powerPerSecond(10));

    public static final SlimefunItemStack ELECTRIC_ORE_GRINDER = new SlimefunItemStack("ELECTRIC_ORE_GRINDER", Material.FURNACE, "&c电力矿石粉碎机", "", "&fWorks as an Ore Crusher and Grind Stone", "", LoreBuilder.machine(MachineTier.ADVANCED, MachineType.MACHINE), LoreBuilder.speed(1), LoreBuilder.powerPerSecond(12));
    public static final SlimefunItemStack ELECTRIC_ORE_GRINDER_2 = new SlimefunItemStack("ELECTRIC_ORE_GRINDER_2", Material.FURNACE, "&c电力矿石粉碎机 &7(&eII&7)", "", "&fWorks as an Ore Crusher and Grind Stone", "", LoreBuilder.machine(MachineTier.END_GAME, MachineType.MACHINE), LoreBuilder.speed(4), LoreBuilder.powerPerSecond(30));
    public static final SlimefunItemStack ELECTRIC_ORE_GRINDER_3 = new SlimefunItemStack("ELECTRIC_ORE_GRINDER_3", Material.FURNACE, "&c电力矿石粉碎机 &7(&eIII&7)", "", "&fWorks as an Ore Crusher and Grind Stone", "", LoreBuilder.machine(MachineTier.END_GAME, MachineType.MACHINE), LoreBuilder.speed(10), LoreBuilder.powerPerSecond(90));
    public static final SlimefunItemStack ELECTRIC_INGOT_PULVERIZER = new SlimefunItemStack("ELECTRIC_INGOT_PULVERIZER", Material.FURNACE, "&c电力锭粉碎机", "", "&fPulverizes Ingots into Dust", "", LoreBuilder.machine(MachineTier.MEDIUM, MachineType.MACHINE), LoreBuilder.speed(1), LoreBuilder.powerPerSecond(14));
    public static final SlimefunItemStack AUTO_DRIER = new SlimefunItemStack("AUTO_DRIER", Material.SMOKER, "&6自动干燥机", "", LoreBuilder.machine(MachineTier.MEDIUM, MachineType.MACHINE), LoreBuilder.speed(1), LoreBuilder.powerPerSecond(10));
    public static final SlimefunItemStack AUTO_ENCHANTER = new SlimefunItemStack("AUTO_ENCHANTER", Material.ENCHANTING_TABLE, "&5自动附魔台", "", LoreBuilder.machine(MachineTier.MEDIUM, MachineType.MACHINE), LoreBuilder.speed(1), LoreBuilder.powerPerSecond(18));
    public static final SlimefunItemStack AUTO_ENCHANTER_2 = new SlimefunItemStack("AUTO_ENCHANTER_2", Material.ENCHANTING_TABLE, "&5自动附魔台 &7- &eII", "", LoreBuilder.machine(MachineTier.END_GAME, MachineType.MACHINE), LoreBuilder.speed(3), LoreBuilder.powerPerSecond(48));
    public static final SlimefunItemStack AUTO_DISENCHANTER = new SlimefunItemStack("AUTO_DISENCHANTER", Material.ENCHANTING_TABLE, "&5自动祛魔台", "", LoreBuilder.machine(MachineTier.MEDIUM, MachineType.MACHINE), LoreBuilder.speed(1), LoreBuilder.powerPerSecond(18));
    public static final SlimefunItemStack AUTO_DISENCHANTER_2 = new SlimefunItemStack("AUTO_DISENCHANTER_2", Material.ENCHANTING_TABLE, "&5自动祛魔台 &7- &eII", "", LoreBuilder.machine(MachineTier.END_GAME, MachineType.MACHINE), LoreBuilder.speed(3), LoreBuilder.powerPerSecond(48));
    public static final SlimefunItemStack AUTO_ANVIL = new SlimefunItemStack("AUTO_ANVIL", Material.IRON_BLOCK, "&7自动铁砧", "", LoreBuilder.machine(MachineTier.ADVANCED, MachineType.MACHINE), "&8\u21E8 &7Repair Factor: 10%", LoreBuilder.powerPerSecond(24));
    public static final SlimefunItemStack AUTO_ANVIL_2 = new SlimefunItemStack("AUTO_ANVIL_2", Material.IRON_BLOCK, "&7自动铁砧 Mk.II", "", LoreBuilder.machine(MachineTier.END_GAME, MachineType.MACHINE), "&8\u21E8 &7Repair Factor: 25%", LoreBuilder.powerPerSecond(32));
    public static final SlimefunItemStack AUTO_BREWER = new SlimefunItemStack("AUTO_BREWER", Material.SMOKER, "&6自动酿造台", "", LoreBuilder.machine(MachineTier.MEDIUM, MachineType.MACHINE), LoreBuilder.speed(1), LoreBuilder.powerPerSecond(12));

    public static final SlimefunItemStack BOOK_BINDER = new SlimefunItemStack("BOOK_BINDER", Material.BOOKSHELF, "&6书本装订机", "", "&fBinds multiple enchanted books into one.", "", LoreBuilder.machine(MachineTier.MEDIUM, MachineType.MACHINE), LoreBuilder.powerPerSecond(16));

    public static final SlimefunItemStack BIO_REACTOR = new SlimefunItemStack("BIO_REACTOR", Material.LIME_TERRACOTTA, "&2生物反应堆", "", LoreBuilder.machine(MachineTier.AVERAGE, MachineType.GENERATOR), LoreBuilder.powerBuffer(128), LoreBuilder.powerPerSecond(8));
    public static final SlimefunItemStack MULTIMETER = new SlimefunItemStack("MULTIMETER", Material.CLOCK, "&e万用表", "", "&fMeasures the Amount of stored", "&fEnergy in a Block");

    public static final SlimefunItemStack SMALL_CAPACITOR = new SlimefunItemStack("SMALL_CAPACITOR", HeadTexture.CAPACITOR_25, "&a小型能量电容", LoreBuilder.range(6), "", LoreBuilder.machine(MachineTier.BASIC, MachineType.CAPACITOR), "&8\u21E8 &e\u26A1 &7128 J Capacity");
    public static final SlimefunItemStack MEDIUM_CAPACITOR = new SlimefunItemStack("MEDIUM_CAPACITOR", HeadTexture.CAPACITOR_25, "&a中型能量电容", LoreBuilder.range(6), "", LoreBuilder.machine(MachineTier.AVERAGE, MachineType.CAPACITOR), "&8\u21E8 &e\u26A1 &7512 J Capacity");
    public static final SlimefunItemStack BIG_CAPACITOR = new SlimefunItemStack("BIG_CAPACITOR", HeadTexture.CAPACITOR_25, "&a大型能量电容", LoreBuilder.range(6), "", LoreBuilder.machine(MachineTier.MEDIUM, MachineType.CAPACITOR), "&8\u21E8 &e\u26A1 &71024 J Capacity");
    public static final SlimefunItemStack LARGE_CAPACITOR = new SlimefunItemStack("LARGE_CAPACITOR", HeadTexture.CAPACITOR_25, "&a巨型能量电容", LoreBuilder.range(6), "", LoreBuilder.machine(MachineTier.GOOD, MachineType.CAPACITOR), "&8\u21E8 &e\u26A1 &78192 J Capacity");
    public static final SlimefunItemStack CARBONADO_EDGED_CAPACITOR = new SlimefunItemStack("CARBONADO_EDGED_CAPACITOR", HeadTexture.CAPACITOR_25, "&a黑金刚石刃口能量电容", LoreBuilder.range(6), "", LoreBuilder.machine(MachineTier.END_GAME, MachineType.CAPACITOR), "&8\u21E8 &e\u26A1 &765536 J Capacity");
    public static final SlimefunItemStack ENERGIZED_CAPACITOR = new SlimefunItemStack("ENERGIZED_CAPACITOR", HeadTexture.CAPACITOR_25, "&a充能能量电容", LoreBuilder.range(6), "", LoreBuilder.machine(MachineTier.END_GAME, MachineType.CAPACITOR), "&8\u21E8 &e\u26A1 &7524288 J Capacity");

    /* Robots */
    public static final SlimefunItemStack PROGRAMMABLE_ANDROID = new SlimefunItemStack("PROGRAMMABLE_ANDROID", HeadTexture.PROGRAMMABLE_ANDROID, "&c可编程机器人 &7(普通)", "", "&8\u21E8 &7Function: None", "&8\u21E8 &7Fuel Efficiency: 1.0x");
    public static final SlimefunItemStack PROGRAMMABLE_ANDROID_FARMER = new SlimefunItemStack("PROGRAMMABLE_ANDROID_FARMER", HeadTexture.PROGRAMMABLE_ANDROID_FARMER, "&c可编程机器人 &7(农夫)", "", "&8\u21E8 &7Function: Farming", "&8\u21E8 &7Fuel Efficiency: 1.0x");
    public static final SlimefunItemStack PROGRAMMABLE_ANDROID_MINER = new SlimefunItemStack("PROGRAMMABLE_ANDROID_MINER", HeadTexture.PROGRAMMABLE_ANDROID_MINER, "&c可编程机器人 &7(矿工)", "", "&8\u21E8 &7Function: Mining", "&8\u21E8 &7Fuel Efficiency: 1.0x");
    public static final SlimefunItemStack PROGRAMMABLE_ANDROID_WOODCUTTER = new SlimefunItemStack("PROGRAMMABLE_ANDROID_WOODCUTTER", HeadTexture.PROGRAMMABLE_ANDROID_WOODCUTTER, "&c可编程机器人 &7(伐木工)", "", "&8\u21E8 &7Function: Woodcutting", "&8\u21E8 &7Fuel Efficiency: 1.0x");
    public static final SlimefunItemStack PROGRAMMABLE_ANDROID_BUTCHER = new SlimefunItemStack("PROGRAMMABLE_ANDROID_BUTCHER", HeadTexture.PROGRAMMABLE_ANDROID_BUTCHER, "&c可编程机器人 &7(屠夫)", "", "&8\u21E8 &7Function: Slaughtering", "&8\u21E8 &7Damage: 4", "&8\u21E8 &7Fuel Efficiency: 1.0x");
    public static final SlimefunItemStack PROGRAMMABLE_ANDROID_FISHERMAN = new SlimefunItemStack("PROGRAMMABLE_ANDROID_FISHERMAN", HeadTexture.PROGRAMMABLE_ANDROID_FISHERMAN, "&c可编程机器人 &7(渔夫)", "", "&8\u21E8 &7Function: Fishing", "&8\u21E8 &7Success Rate: 10%", "&8\u21E8 &7Fuel Efficiency: 1.0x");

    public static final SlimefunItemStack PROGRAMMABLE_ANDROID_2 = new SlimefunItemStack("PROGRAMMABLE_ANDROID_2", HeadTexture.PROGRAMMABLE_ANDROID, "&c高级可编程机器人 &7(普通)", "", "&8\u21E8 &7Function: None", "&8\u21E8 &7Fuel Efficiency: 1.5x");
    public static final SlimefunItemStack PROGRAMMABLE_ANDROID_2_FISHERMAN = new SlimefunItemStack("PROGRAMMABLE_ANDROID_2_FISHERMAN", HeadTexture.PROGRAMMABLE_ANDROID_FISHERMAN, "&c高级可编程机器人 &7(渔夫)", "", "&8\u21E8 &7Function: Fishing", "&8\u21E8 &7Success Rate: 20%", "&8\u21E8 &7Fuel Efficiency: 1.5x");
    public static final SlimefunItemStack PROGRAMMABLE_ANDROID_2_FARMER = new SlimefunItemStack("PROGRAMMABLE_ANDROID_2_FARMER", HeadTexture.PROGRAMMABLE_ANDROID_FARMER, "&c高级可编程机器人 &7(农夫)", "", "&8\u21E8 &7Function: Farming", "&8\u21E8 &7Fuel Efficiency: 1.5x", "&8\u21E8 &7Can also harvest Plants from ExoticGarden");
    public static final SlimefunItemStack PROGRAMMABLE_ANDROID_2_BUTCHER = new SlimefunItemStack("PROGRAMMABLE_ANDROID_2_BUTCHER", HeadTexture.PROGRAMMABLE_ANDROID_BUTCHER, "&c高级可编程机器人 &7(屠夫)", "", "&8\u21E8 &7Function: Slaughtering", "&8\u21E8 &7Damage: 8", "&8\u21E8 &7Fuel Efficiency: 1.5x");

    public static final SlimefunItemStack PROGRAMMABLE_ANDROID_3 = new SlimefunItemStack("PROGRAMMABLE_ANDROID_3", HeadTexture.PROGRAMMABLE_ANDROID, "&e强化可编程机器人 &7(普通)", "", "&8\u21E8 &7Function: None", "&8\u21E8 &7Fuel Efficiency: 3.0x");
    public static final SlimefunItemStack PROGRAMMABLE_ANDROID_3_FISHERMAN = new SlimefunItemStack("PROGRAMMABLE_ANDROID_3_FISHERMAN", HeadTexture.PROGRAMMABLE_ANDROID_FISHERMAN, "&e强化可编程机器人 &7(渔夫)", "", "&8\u21E8 &7Function: Fishing", "&8\u21E8 &7Success Rate: 30%", "&8\u21E8 &7Fuel Efficiency: 8.0x");
    public static final SlimefunItemStack PROGRAMMABLE_ANDROID_3_BUTCHER = new SlimefunItemStack("PROGRAMMABLE_ANDROID_3_BUTCHER", HeadTexture.PROGRAMMABLE_ANDROID_BUTCHER, "&e强化可编程机器人 &7(屠夫)", "", "&8\u21E8 &7Function: Slaughtering", "&8\u21E8 &7Damage: 20", "&8\u21E8 &7Fuel Efficiency: 8.0x");

    /* GPS */
    public static final SlimefunItemStack GPS_TRANSMITTER = new SlimefunItemStack("GPS_TRANSMITTER", HeadTexture.GPS_TRANSMITTER, "&bGPS 发射器", "", LoreBuilder.powerBuffer(16), LoreBuilder.powerPerSecond(2));
    public static final SlimefunItemStack GPS_TRANSMITTER_2 = new SlimefunItemStack("GPS_TRANSMITTER_2", HeadTexture.GPS_TRANSMITTER, "&c高级 GPS 发射器", "", LoreBuilder.powerBuffer(64), LoreBuilder.powerPerSecond(6));
    public static final SlimefunItemStack GPS_TRANSMITTER_3 = new SlimefunItemStack("GPS_TRANSMITTER_3", HeadTexture.GPS_TRANSMITTER, "&4黑金刚石 GPS 发射器", "", LoreBuilder.powerBuffer(256), LoreBuilder.powerPerSecond(22));
    public static final SlimefunItemStack GPS_TRANSMITTER_4 = new SlimefunItemStack("GPS_TRANSMITTER_4", HeadTexture.GPS_TRANSMITTER, "&e充能 GPS 发射器", "", LoreBuilder.powerBuffer(1024), LoreBuilder.powerPerSecond(92));

    public static final SlimefunItemStack GPS_MARKER_TOOL = new SlimefunItemStack("GPS_MARKER_TOOL", Material.REDSTONE_TORCH, "&bGPS 标记工具", "", "&fAllows you to set a Waypoint at", "&fthe Location you place this");
    public static final SlimefunItemStack GPS_CONTROL_PANEL = new SlimefunItemStack("GPS_CONTROL_PANEL", HeadTexture.GPS_CONTROL_PANEL, "&bGPS 控制面板", "", "&fAllows you to track your Satellites", "&fand manage your Waypoints");
    public static final SlimefunItemStack GPS_EMERGENCY_TRANSMITTER = new SlimefunItemStack("GPS_EMERGENCY_TRANSMITTER", HeadTexture.GPS_TRANSMITTER, "&cGPS 紧急发射器", "", "&fCarrying this in your Inventory", "&fautomatically sets a Waypoint", "&fat your Location when you die.");

    public static final SlimefunItemStack ANDROID_INTERFACE_FUEL = new SlimefunItemStack("ANDROID_INTERFACE_FUEL", Material.DISPENSER, "&7机器人接口 &c(燃料)", "", "&fItems stored in this Interface", "&fwill be inserted into an Android's Fuel Slot", "&fwhen its Script tells them to do so");
    public static final SlimefunItemStack ANDROID_INTERFACE_ITEMS = new SlimefunItemStack("ANDROID_INTERFACE_ITEMS", Material.DISPENSER, "&7机器人接口 &9(物品)", "", "&fItems stored in an Android's Inventory", "&fwill be inserted into this Interface", "&fwhen its Script tells them to do so");

    public static final SlimefunItemStack GPS_GEO_SCANNER = new SlimefunItemStack("GPS_GEO_SCANNER", HeadTexture.GEO_SCANNER, "&bGEO 扫描仪", "", "&fScans a Chunk for natural Resources", "&fsuch as &8Oil");
    public static final SlimefunItemStack PORTABLE_GEO_SCANNER = new SlimefunItemStack("PORTABLE_GEO_SCANNER", Material.CLOCK, "&b便携式 GEO 扫描仪", "", "&fScans a Chunk for natural Resources", "", "&eRight Click&7 to scan");
    public static final SlimefunItemStack GEO_MINER = new SlimefunItemStack("GEO_MINER", HeadTexture.GEO_MINER, "&6GEO 采矿机", "", "&eMines up resources from the chunk", "&eThese Resources cannot be mined with a pickaxe", "", LoreBuilder.machine(MachineTier.ADVANCED, MachineType.MACHINE), LoreBuilder.speed(1), LoreBuilder.powerPerSecond(48), "", "&c&l! &cMake sure to Geo-Scan the Chunk first");
    public static final SlimefunItemStack OIL_PUMP = new SlimefunItemStack("OIL_PUMP", HeadTexture.OIL_PUMP, "&4抽油泵", "", "&7Pumps up Oil and fills it into Buckets", "", "&c&l! &cMake sure to Geo-Scan the Chunk first");
    public static final SlimefunItemStack OIL_BUCKET = new SlimefunItemStack("BUCKET_OF_OIL", HeadTexture.OIL_BUCKET, "&f原油桶");
    public static final SlimefunItemStack FUEL_BUCKET = new SlimefunItemStack("BUCKET_OF_FUEL", HeadTexture.FUEL_BUCKET, "&f燃油桶");

    public static final SlimefunItemStack REFINERY = new SlimefunItemStack("REFINERY", Material.PISTON, "&c精炼厂", "", "&fRefines Oil to create Fuel");
    public static final SlimefunItemStack COMBUSTION_REACTOR = new SlimefunItemStack("COMBUSTION_REACTOR", HeadTexture.GENERATOR, "&c燃烧反应堆", "", LoreBuilder.machine(MachineTier.ADVANCED, MachineType.GENERATOR), LoreBuilder.powerBuffer(256), LoreBuilder.powerPerSecond(24));
    public static final SlimefunItemStack ANDROID_MEMORY_CORE = new SlimefunItemStack("ANDROID_MEMORY_CORE", HeadTexture.ENERGY_REGULATOR, "&b机器人内存核心");

    public static final SlimefunItemStack GPS_TELEPORTER_PYLON = new SlimefunItemStack("GPS_TELEPORTER_PYLON", Material.PURPLE_STAINED_GLASS, "&5GPS 传送塔标", "", "&7Teleporter Component");
    public static final SlimefunItemStack GPS_TELEPORTATION_MATRIX = new SlimefunItemStack("GPS_TELEPORTATION_MATRIX", Material.IRON_BLOCK, "&bGPS 传送矩阵", "", "&fThis is your Teleporter's Main Component", "&fThis Matrix allows Players to choose from all", "&fWaypoints made by the Player who has placed", "&fthis Device.");
    public static final SlimefunItemStack GPS_ACTIVATION_DEVICE_SHARED = new SlimefunItemStack("GPS_ACTIVATION_DEVICE_SHARED", Material.STONE_PRESSURE_PLATE, "&fGPS 激活装置 &3(共享)", "", "&fPlace this onto a Teleportation Matrix", "&fand step onto this Plate to activate", "&fthe Teleportation Process");
    public static final SlimefunItemStack GPS_ACTIVATION_DEVICE_PERSONAL = new SlimefunItemStack("GPS_ACTIVATION_DEVICE_PERSONAL", Material.STONE_PRESSURE_PLATE, "&fGPS 激活装置 &a(私人)", "", "&fPlace this onto a Teleportation Matrix", "&fand step onto this Plate to activate", "&fthe Teleportation Process", "", "&fThis Version only allows the Person who", "&fplaced this Device to use it");
    public static final SlimefunItemStack PORTABLE_TELEPORTER = new SlimefunItemStack("PORTABLE_TELEPORTER", Material.COMPASS, "&b便携传送器", "", "&fThis device allows you to teleport", "&fto your waypoints from anywhere", "", LoreBuilder.powerCharged(0, 50), "", "&eRight Click&7 to use");

    public static final SlimefunItemStack ELEVATOR_PLATE = new SlimefunItemStack("ELEVATOR_PLATE", Material.STONE_PRESSURE_PLATE, "&b电梯板", "", "&fPlace an Elevator Plate on every floor", "&fand you will be able to teleport between them.", "", "&eRight Click this Block &7to name it");
    public static final SlimefunItemStack INFUSED_HOPPER = new SlimefunItemStack("INFUSED_HOPPER", Material.HOPPER, "&5注入漏斗", "", "&fAutomatically picks up nearby Items in a 7x7x7", "&fRadius when placed.");

    public static final SlimefunItemStack HEATED_PRESSURE_CHAMBER = new SlimefunItemStack("HEATED_PRESSURE_CHAMBER", Material.LIGHT_GRAY_STAINED_GLASS, "&c加热压力舱", "", LoreBuilder.machine(MachineTier.END_GAME, MachineType.MACHINE), LoreBuilder.speed(1), LoreBuilder.powerPerSecond(10));
    public static final SlimefunItemStack HEATED_PRESSURE_CHAMBER_2 = new SlimefunItemStack("HEATED_PRESSURE_CHAMBER_2", Material.LIGHT_GRAY_STAINED_GLASS, "&c加热压力舱 &7- &eII", "", LoreBuilder.machine(MachineTier.END_GAME, MachineType.MACHINE), LoreBuilder.speed(5), LoreBuilder.powerPerSecond(44));

    public static final SlimefunItemStack ELECTRIC_SMELTERY = new SlimefunItemStack("ELECTRIC_SMELTERY", Material.FURNACE, "&c电力冶炼炉", "", "&4Alloys-Only, doesn't smelt Dust into Ingots", "", LoreBuilder.machine(MachineTier.END_GAME, MachineType.MACHINE), LoreBuilder.speed(1), LoreBuilder.powerPerSecond(20));
    public static final SlimefunItemStack ELECTRIC_SMELTERY_2 = new SlimefunItemStack("ELECTRIC_SMELTERY_2", Material.FURNACE, "&c电力冶炼炉 &7- &eII", "", "&4Alloys-Only, doesn't smelt Dust into Ingots", "", LoreBuilder.machine(MachineTier.END_GAME, MachineType.MACHINE), LoreBuilder.speed(3), LoreBuilder.powerPerSecond(40));

    public static final SlimefunItemStack ELECTRIC_PRESS = new SlimefunItemStack("ELECTRIC_PRESS", HeadTexture.ELECTRIC_PRESS, "&e电动冲压机", "", LoreBuilder.machine(MachineTier.MEDIUM, MachineType.MACHINE), LoreBuilder.speed(1), LoreBuilder.powerPerSecond(16));
    public static final SlimefunItemStack ELECTRIC_PRESS_2 = new SlimefunItemStack("ELECTRIC_PRESS_2", HeadTexture.ELECTRIC_PRESS, "&e电动冲压机 &7- &eII", "", LoreBuilder.machine(MachineTier.END_GAME, MachineType.MACHINE), LoreBuilder.speed(3), LoreBuilder.powerPerSecond(40));

    public static final SlimefunItemStack ELECTRIFIED_CRUCIBLE = new SlimefunItemStack("ELECTRIFIED_CRUCIBLE", Material.RED_TERRACOTTA, "&c电力坩埚", "", LoreBuilder.machine(MachineTier.END_GAME, MachineType.MACHINE), LoreBuilder.speed(1), LoreBuilder.powerPerSecond(48));
    public static final SlimefunItemStack ELECTRIFIED_CRUCIBLE_2 = new SlimefunItemStack("ELECTRIFIED_CRUCIBLE_2", Material.RED_TERRACOTTA, "&c电力坩埚 &7- &eII", "", LoreBuilder.machine(MachineTier.END_GAME, MachineType.MACHINE), LoreBuilder.speed(2), LoreBuilder.powerPerSecond(80));
    public static final SlimefunItemStack ELECTRIFIED_CRUCIBLE_3 = new SlimefunItemStack("ELECTRIFIED_CRUCIBLE_3", Material.RED_TERRACOTTA, "&c电力坩埚 &7- &eIII", "", LoreBuilder.machine(MachineTier.END_GAME, MachineType.MACHINE), LoreBuilder.speed(4), LoreBuilder.powerPerSecond(120));

    public static final SlimefunItemStack CARBON_PRESS = new SlimefunItemStack("CARBON_PRESS", Material.BLACK_STAINED_GLASS, "&c碳压机", "", LoreBuilder.machine(MachineTier.END_GAME, MachineType.MACHINE), LoreBuilder.speed(1), LoreBuilder.powerPerSecond(20));
    public static final SlimefunItemStack CARBON_PRESS_2 = new SlimefunItemStack("CARBON_PRESS_2", Material.BLACK_STAINED_GLASS, "&c碳压机 &7- &eII", "", LoreBuilder.machine(MachineTier.END_GAME, MachineType.MACHINE), LoreBuilder.speed(3), LoreBuilder.powerPerSecond(50));
    public static final SlimefunItemStack CARBON_PRESS_3 = new SlimefunItemStack("CARBON_PRESS_3", Material.BLACK_STAINED_GLASS, "&c碳压机 &7- &eIII", "", LoreBuilder.machine(MachineTier.END_GAME, MachineType.MACHINE), LoreBuilder.speed(15), LoreBuilder.powerPerSecond(180));

    public static final SlimefunItemStack BLISTERING_INGOT = new SlimefunItemStack("BLISTERING_INGOT", Material.GOLD_INGOT, "&6炽热锭 &7(33%)", "", LoreBuilder.radioactive(Radioactivity.HIGH), LoreBuilder.HAZMAT_SUIT_REQUIRED);
    public static final SlimefunItemStack BLISTERING_INGOT_2 = new SlimefunItemStack("BLISTERING_INGOT_2", Material.GOLD_INGOT, "&6炽热锭 &7(66%)", "", LoreBuilder.radioactive(Radioactivity.VERY_HIGH), LoreBuilder.HAZMAT_SUIT_REQUIRED);
    public static final SlimefunItemStack BLISTERING_INGOT_3 = new SlimefunItemStack("BLISTERING_INGOT_3", Material.GOLD_INGOT, "&6炽热锭", "", LoreBuilder.radioactive(Radioactivity.VERY_HIGH), LoreBuilder.HAZMAT_SUIT_REQUIRED);

    public static final SlimefunItemStack ENERGY_REGULATOR = new SlimefunItemStack("ENERGY_REGULATOR", HeadTexture.ENERGY_REGULATOR, "&6能量调节器", "", "&fCore Component of an Energy Network");
    public static final SlimefunItemStack ENERGY_CONNECTOR = new SlimefunItemStack("ENERGY_CONNECTOR", HeadTexture.ENERGY_CONNECTOR, "&e能量连接器", LoreBuilder.range(6), "", "&fPlace this between machines", "&fand generators to connect them", "&fto your regulator.");
    public static final SlimefunItemStack DEBUG_FISH = new SlimefunItemStack("DEBUG_FISH", Material.SALMON, "&3这条鱼多少钱？", "", "&eRight Click &fany Block to view it's BlockData", "&eLeft Click &fto break a Block", "&eShift + Left Click &fany Block to erase it's BlockData", "&eShift + Right Click &fto place a Placeholder Block");

    public static final SlimefunItemStack NETHER_ICE = new SlimefunItemStack("NETHER_ICE", HeadTexture.NETHER_ICE, "&e下界冰", "", LoreBuilder.radioactive(Radioactivity.MODERATE), LoreBuilder.HAZMAT_SUIT_REQUIRED);
    public static final SlimefunItemStack ENRICHED_NETHER_ICE = new SlimefunItemStack("ENRICHED_NETHER_ICE", HeadTexture.ENRICHED_NETHER_ICE, "&e富集下界冰", "", LoreBuilder.radioactive(Radioactivity.VERY_HIGH), LoreBuilder.HAZMAT_SUIT_REQUIRED);
    public static final SlimefunItemStack NETHER_ICE_COOLANT_CELL = new SlimefunItemStack("NETHER_ICE_COOLANT_CELL", HeadTexture.NETHER_ICE_COOLANT_CELL, "&6下界冰冷却单元");

    // Cargo
    public static final SlimefunItemStack CARGO_MANAGER = new SlimefunItemStack("CARGO_MANAGER", HeadTexture.CARGO_MANAGER, "&6货物管理器", "", "&fCore Component of an Item Transport Network");
    public static final SlimefunItemStack CARGO_CONNECTOR_NODE = new SlimefunItemStack("CARGO_NODE", HeadTexture.CARGO_CONNECTOR_NODE, "&7货物节点 &c(连接器)", "", "&fCargo Connector Pipe");
    public static final SlimefunItemStack CARGO_INPUT_NODE = new SlimefunItemStack("CARGO_NODE_INPUT", HeadTexture.CARGO_INPUT_NODE, "&7货物节点 &c(输入)", "", "&fCargo Input Pipe");
    public static final SlimefunItemStack CARGO_OUTPUT_NODE = new SlimefunItemStack("CARGO_NODE_OUTPUT", HeadTexture.CARGO_OUTPUT_NODE, "&7货物节点 &c(输出)", "", "&fCargo Output Pipe");
    public static final SlimefunItemStack CARGO_OUTPUT_NODE_2 = new SlimefunItemStack("CARGO_NODE_OUTPUT_ADVANCED", HeadTexture.CARGO_OUTPUT_NODE, "&6高级货物节点 &c(输出)", "", "&fCargo Output Pipe");

    // Animal farm
    public static final SlimefunItemStack AUTO_BREEDER = new SlimefunItemStack("AUTO_BREEDER", Material.HAY_BLOCK, "&e自动喂养机", "", "&fRuns on &aOrganic Food", "", LoreBuilder.machine(MachineTier.END_GAME, MachineType.MACHINE), LoreBuilder.powerBuffer(1024), "&8\u21E8 &e\u26A1 &760 J/Animal");
    public static final SlimefunItemStack PRODUCE_COLLECTOR = new SlimefunItemStack("PRODUCE_COLLECTOR", Material.HAY_BLOCK, "&b产品收集器", "", "&fThis machine allows you to", "&fcollect produce from nearby animals.", "", LoreBuilder.machine(MachineTier.ADVANCED, MachineType.MACHINE), LoreBuilder.powerBuffer(512), LoreBuilder.powerPerSecond(32));

    public static final SlimefunItemStack ORGANIC_FOOD = new SlimefunItemStack("ORGANIC_FOOD", HeadTexture.FILLED_CAN, "&a有机食物", "&7Content: &9???");
    public static final SlimefunItemStack WHEAT_ORGANIC_FOOD = new SlimefunItemStack("ORGANIC_FOOD_WHEAT", HeadTexture.FILLED_CAN, ORGANIC_FOOD.getDisplayName(), "&7内容物：&9小麦");
    public static final SlimefunItemStack CARROT_ORGANIC_FOOD = new SlimefunItemStack("ORGANIC_FOOD_CARROT", HeadTexture.FILLED_CAN, ORGANIC_FOOD.getDisplayName(), "&7内容物：&9胡萝卜");
    public static final SlimefunItemStack POTATO_ORGANIC_FOOD = new SlimefunItemStack("ORGANIC_FOOD_POTATO", HeadTexture.FILLED_CAN, ORGANIC_FOOD.getDisplayName(), "&7内容物：&9马铃薯");
    public static final SlimefunItemStack SEEDS_ORGANIC_FOOD = new SlimefunItemStack("ORGANIC_FOOD_SEEDS", HeadTexture.FILLED_CAN, ORGANIC_FOOD.getDisplayName(), "&7内容物：&9种子");
    public static final SlimefunItemStack BEETROOT_ORGANIC_FOOD = new SlimefunItemStack("ORGANIC_FOOD_BEETROOT", HeadTexture.FILLED_CAN, ORGANIC_FOOD.getDisplayName(), "&7内容物：&9甜菜根");
    public static final SlimefunItemStack MELON_ORGANIC_FOOD = new SlimefunItemStack("ORGANIC_FOOD_MELON", HeadTexture.FILLED_CAN, ORGANIC_FOOD.getDisplayName(), "&7内容物：&9西瓜");
    public static final SlimefunItemStack APPLE_ORGANIC_FOOD = new SlimefunItemStack("ORGANIC_FOOD_APPLE", HeadTexture.FILLED_CAN, ORGANIC_FOOD.getDisplayName(), "&7内容物：&9苹果");
    public static final SlimefunItemStack SWEET_BERRIES_ORGANIC_FOOD = new SlimefunItemStack("ORGANIC_FOOD_SWEET_BERRIES", HeadTexture.FILLED_CAN, ORGANIC_FOOD.getDisplayName(), "&7内容物：&9甜浆果");
    public static final SlimefunItemStack KELP_ORGANIC_FOOD = new SlimefunItemStack("ORGANIC_FOOD_KELP", HeadTexture.FILLED_CAN, ORGANIC_FOOD.getDisplayName(), "&7内容物：&9干海带");
    public static final SlimefunItemStack COCOA_ORGANIC_FOOD = new SlimefunItemStack("ORGANIC_FOOD_COCOA", HeadTexture.FILLED_CAN, ORGANIC_FOOD.getDisplayName(), "&7内容物：&9可可豆");
    public static final SlimefunItemStack SEAGRASS_ORGANIC_FOOD = new SlimefunItemStack("ORGANIC_FOOD_SEAGRASS", HeadTexture.FILLED_CAN, ORGANIC_FOOD.getDisplayName(), "&7内容物：&9海草");

    public static final SlimefunItemStack FERTILIZER = new SlimefunItemStack("FERTILIZER", HeadTexture.FILLED_CAN, "&a有机肥料", "&7Content: &9???");
    public static final SlimefunItemStack WHEAT_FERTILIZER = new SlimefunItemStack("FERTILIZER_WHEAT", HeadTexture.FILLED_CAN, FERTILIZER.getDisplayName(), "&7内容物：&9小麦");
    public static final SlimefunItemStack CARROT_FERTILIZER = new SlimefunItemStack("FERTILIZER_CARROT", HeadTexture.FILLED_CAN, FERTILIZER.getDisplayName(), "&7内容物：&9胡萝卜");
    public static final SlimefunItemStack POTATO_FERTILIZER = new SlimefunItemStack("FERTILIZER_POTATO", HeadTexture.FILLED_CAN, FERTILIZER.getDisplayName(), "&7内容物：&9马铃薯");
    public static final SlimefunItemStack SEEDS_FERTILIZER = new SlimefunItemStack("FERTILIZER_SEEDS", HeadTexture.FILLED_CAN, FERTILIZER.getDisplayName(), "&7内容物：&9种子");
    public static final SlimefunItemStack BEETROOT_FERTILIZER = new SlimefunItemStack("FERTILIZER_BEETROOT", HeadTexture.FILLED_CAN, FERTILIZER.getDisplayName(), "&7内容物：&9甜菜根");
    public static final SlimefunItemStack MELON_FERTILIZER = new SlimefunItemStack("FERTILIZER_MELON", HeadTexture.FILLED_CAN, FERTILIZER.getDisplayName(), "&7内容物：&9西瓜");
    public static final SlimefunItemStack APPLE_FERTILIZER = new SlimefunItemStack("FERTILIZER_APPLE", HeadTexture.FILLED_CAN, FERTILIZER.getDisplayName(), "&7内容物：&9苹果");
    public static final SlimefunItemStack SWEET_BERRIES_FERTILIZER = new SlimefunItemStack("FERTILIZER_SWEET_BERRIES", HeadTexture.FILLED_CAN, FERTILIZER.getDisplayName(), "&7内容物：&9甜浆果");
    public static final SlimefunItemStack KELP_FERTILIZER = new SlimefunItemStack("FERTILIZER_KELP", HeadTexture.FILLED_CAN, FERTILIZER.getDisplayName(), "&7内容物：&9干海带");
    public static final SlimefunItemStack COCOA_FERTILIZER = new SlimefunItemStack("FERTILIZER_COCOA", HeadTexture.FILLED_CAN, FERTILIZER.getDisplayName(), "&7内容物：&9可可豆");
    public static final SlimefunItemStack SEAGRASS_FERTILIZER = new SlimefunItemStack("FERTILIZER_SEAGRASS", HeadTexture.FILLED_CAN, FERTILIZER.getDisplayName(), "&7内容物：&9海草");

    public static final SlimefunItemStack ANIMAL_GROWTH_ACCELERATOR = new SlimefunItemStack("ANIMAL_GROWTH_ACCELERATOR", Material.HAY_BLOCK, "&b动物生长加速器", "", "&fRuns on &aOrganic Food", "", LoreBuilder.machine(MachineTier.END_GAME, MachineType.MACHINE), LoreBuilder.powerBuffer(1024), LoreBuilder.powerPerSecond(28));
    public static final SlimefunItemStack CROP_GROWTH_ACCELERATOR = new SlimefunItemStack("CROP_GROWTH_ACCELERATOR", Material.LIME_TERRACOTTA, "&a作物生长加速器", "", "&fRuns on &aOrganic Fertilizer", "", LoreBuilder.machine(MachineTier.END_GAME, MachineType.MACHINE), "&8\u21E8 &7Radius: 7x7", "&8\u21E8 &7Speed: &a3/time", LoreBuilder.powerBuffer(1024), LoreBuilder.powerPerSecond(50));
    public static final SlimefunItemStack CROP_GROWTH_ACCELERATOR_2 = new SlimefunItemStack("CROP_GROWTH_ACCELERATOR_2", Material.LIME_TERRACOTTA, "&a作物生长加速器 &7(&eII&7)", "", "&fRuns on &aOrganic Fertilizer", "", LoreBuilder.machine(MachineTier.END_GAME, MachineType.MACHINE), "&8\u21E8 &7Radius: 9x9", "&8\u21E8 &7Speed: &a4/time", LoreBuilder.powerBuffer(1024), LoreBuilder.powerPerSecond(60));
    public static final SlimefunItemStack TREE_GROWTH_ACCELERATOR = new SlimefunItemStack("TREE_GROWTH_ACCELERATOR", Material.BROWN_TERRACOTTA, "&a树木生长加速器", "", "&fRuns on &aOrganic Fertilizer", "", LoreBuilder.machine(MachineTier.END_GAME, MachineType.MACHINE), "&8\u21E8 &7Radius: 9x9", "&8\u21E8 &7Speed: &a4/time", LoreBuilder.powerBuffer(1024), LoreBuilder.powerPerSecond(48));

    public static final SlimefunItemStack FOOD_FABRICATOR = new SlimefunItemStack("FOOD_FABRICATOR", Material.GREEN_STAINED_GLASS, "&c食物制造机", "", "&fProduces &aOrganic Food", "", LoreBuilder.machine(MachineTier.ADVANCED, MachineType.MACHINE), LoreBuilder.speed(1), LoreBuilder.powerBuffer(256), LoreBuilder.powerPerSecond(14));
    public static final SlimefunItemStack FOOD_FABRICATOR_2 = new SlimefunItemStack("FOOD_FABRICATOR_2", Material.GREEN_STAINED_GLASS, "&c食物制造机 &7(&eII&7)", "", "&fProduces &aOrganic Food", "", LoreBuilder.machine(MachineTier.END_GAME, MachineType.MACHINE), LoreBuilder.speed(6), LoreBuilder.powerBuffer(512), LoreBuilder.powerPerSecond(48));

    public static final SlimefunItemStack FOOD_COMPOSTER = new SlimefunItemStack("FOOD_COMPOSTER", Material.GREEN_TERRACOTTA, "&c食物堆肥机", "", "&fProduces &aOrganic Fertilizer", "", LoreBuilder.machine(MachineTier.ADVANCED, MachineType.MACHINE), LoreBuilder.speed(1), LoreBuilder.powerBuffer(256), LoreBuilder.powerPerSecond(16));
    public static final SlimefunItemStack FOOD_COMPOSTER_2 = new SlimefunItemStack("FOOD_COMPOSTER_2", Material.GREEN_TERRACOTTA, "&c食物堆肥机 &7(&eII&7)", "", "&fProduces &aOrganic Fertilizer", "", LoreBuilder.machine(MachineTier.END_GAME, MachineType.MACHINE), LoreBuilder.speed(10), LoreBuilder.powerBuffer(512), LoreBuilder.powerPerSecond(52));

    public static final SlimefunItemStack EXP_COLLECTOR = new SlimefunItemStack("XP_COLLECTOR", HeadTexture.EXP_COLLECTOR, "&a经验收集器", "", "&fCollects nearby Exp and stores it", "", LoreBuilder.machine(MachineTier.END_GAME, MachineType.MACHINE), LoreBuilder.powerBuffer(1024), LoreBuilder.powerPerSecond(20));
    public static final SlimefunItemStack REACTOR_COOLANT_CELL = new SlimefunItemStack("REACTOR_COLLANT_CELL", HeadTexture.COOLANT_CELL, "&b反应堆冷却单元");

    public static final SlimefunItemStack NUCLEAR_REACTOR = new SlimefunItemStack("NUCLEAR_REACTOR", HeadTexture.NUCLEAR_REACTOR, "&2核反应堆", "", "&fRequires Cooling!", "&8\u21E8 &bMust be surrounded by Water", "&8\u21E8 &bMust be supplied with Reactor Coolant Cells", "", LoreBuilder.machine(MachineTier.END_GAME, MachineType.GENERATOR), LoreBuilder.powerBuffer(16384), LoreBuilder.powerPerSecond(500));
    public static final SlimefunItemStack NETHER_STAR_REACTOR = new SlimefunItemStack("NETHERSTAR_REACTOR", HeadTexture.NETHER_STAR_REACTOR, "&f下界之星反应堆", "", "&fRuns on Nether Stars", "&8\u21E8 &bMust be surrounded by Water", "&8\u21E8 &bMust be supplied with Nether Ice Coolant Cells", "", LoreBuilder.machine(MachineTier.END_GAME, MachineType.GENERATOR), LoreBuilder.powerBuffer(32768), LoreBuilder.powerPerSecond(1024), "&8\u21E8 &4Causes nearby Entities to get Withered");
    public static final SlimefunItemStack REACTOR_ACCESS_PORT = new SlimefunItemStack("REACTOR_ACCESS_PORT", Material.CYAN_TERRACOTTA, "&2反应堆接入端口", "", "&fAllows you to interact with a Reactor", "&fvia Cargo Nodes, can also be used", "&fas a Buffer", "", "&8\u21E8 &eMust be placed &a3 Blocks &eabove the Reactor");

    public static final SlimefunItemStack FREEZER = new SlimefunItemStack("FREEZER", Material.LIGHT_BLUE_STAINED_GLASS, "&b冷冻机", "", LoreBuilder.machine(MachineTier.ADVANCED, MachineType.MACHINE), LoreBuilder.speed(1), LoreBuilder.powerBuffer(256), LoreBuilder.powerPerSecond(18));
    public static final SlimefunItemStack FREEZER_2 = new SlimefunItemStack("FREEZER_2", Material.LIGHT_BLUE_STAINED_GLASS, "&b冷冻机 &7(&eII&7)", "", LoreBuilder.machine(MachineTier.END_GAME, MachineType.MACHINE), LoreBuilder.speed(2), LoreBuilder.powerBuffer(256), LoreBuilder.powerPerSecond(30));
    public static final SlimefunItemStack FREEZER_3 = new SlimefunItemStack("FREEZER_3", Material.LIGHT_BLUE_STAINED_GLASS, "&b冷冻机 &7(&eIII&7)", "", LoreBuilder.machine(MachineTier.END_GAME, MachineType.MACHINE), LoreBuilder.speed(3), LoreBuilder.powerBuffer(256), LoreBuilder.powerPerSecond(42));

    public static final SlimefunItemStack ELECTRIC_GOLD_PAN = new SlimefunItemStack("ELECTRIC_GOLD_PAN", Material.BROWN_TERRACOTTA, "&6电动淘金盘", "", LoreBuilder.machine(MachineTier.BASIC, MachineType.MACHINE), LoreBuilder.speed(1), LoreBuilder.powerPerSecond(2));
    public static final SlimefunItemStack ELECTRIC_GOLD_PAN_2 = new SlimefunItemStack("ELECTRIC_GOLD_PAN_2", Material.BROWN_TERRACOTTA, "&6电动淘金盘 &7(&eII&7)", "", LoreBuilder.machine(MachineTier.BASIC, MachineType.MACHINE), LoreBuilder.speed(3), LoreBuilder.powerPerSecond(4));
    public static final SlimefunItemStack ELECTRIC_GOLD_PAN_3 = new SlimefunItemStack("ELECTRIC_GOLD_PAN_3", Material.BROWN_TERRACOTTA, "&6电动淘金盘 &7(&eIII&7)", "", LoreBuilder.machine(MachineTier.END_GAME, MachineType.MACHINE), LoreBuilder.speed(10), LoreBuilder.powerPerSecond(14));

    public static final SlimefunItemStack ELECTRIC_DUST_WASHER = new SlimefunItemStack("ELECTRIC_DUST_WASHER", Material.BLUE_STAINED_GLASS, "&3电动洗尘机", "", LoreBuilder.machine(MachineTier.BASIC, MachineType.MACHINE), LoreBuilder.speed(1), LoreBuilder.powerPerSecond(6));
    public static final SlimefunItemStack ELECTRIC_DUST_WASHER_2 = new SlimefunItemStack("ELECTRIC_DUST_WASHER_2", Material.BLUE_STAINED_GLASS, "&3电动洗尘机 &7(&eII&7)", "", LoreBuilder.machine(MachineTier.BASIC, MachineType.MACHINE), LoreBuilder.speed(2), LoreBuilder.powerPerSecond(10));
    public static final SlimefunItemStack ELECTRIC_DUST_WASHER_3 = new SlimefunItemStack("ELECTRIC_DUST_WASHER_3", Material.BLUE_STAINED_GLASS, "&3电动洗尘机 &7(&eIII&7)", "", LoreBuilder.machine(MachineTier.END_GAME, MachineType.MACHINE), LoreBuilder.speed(10), LoreBuilder.powerPerSecond(30));

    public static final SlimefunItemStack ELECTRIC_INGOT_FACTORY = new SlimefunItemStack("ELECTRIC_INGOT_FACTORY", Material.RED_TERRACOTTA, "&c电力铸锭工厂", "", LoreBuilder.machine(MachineTier.BASIC, MachineType.MACHINE), LoreBuilder.speed(1), LoreBuilder.powerPerSecond(8));
    public static final SlimefunItemStack ELECTRIC_INGOT_FACTORY_2 = new SlimefunItemStack("ELECTRIC_INGOT_FACTORY_2", Material.RED_TERRACOTTA, "&c电力铸锭工厂 &7(&eII&7)", "", LoreBuilder.machine(MachineTier.BASIC, MachineType.MACHINE), LoreBuilder.speed(2), LoreBuilder.powerPerSecond(14));
    public static final SlimefunItemStack ELECTRIC_INGOT_FACTORY_3 = new SlimefunItemStack("ELECTRIC_INGOT_FACTORY_3", Material.RED_TERRACOTTA, "&c电力铸锭工厂 &7(&eIII&7)", "", LoreBuilder.machine(MachineTier.END_GAME, MachineType.MACHINE), LoreBuilder.speed(8), LoreBuilder.powerPerSecond(40));

    public static final SlimefunItemStack FLUID_PUMP = new SlimefunItemStack("FLUID_PUMP", Material.BLUE_TERRACOTTA, "&9流体泵", "", LoreBuilder.machine(MachineTier.ADVANCED, MachineType.MACHINE), "&8\u21E8 &e\u26A1 &732 J/Block");
    public static final SlimefunItemStack CHARGING_BENCH = new SlimefunItemStack("CHARGING_BENCH", Material.CRAFTING_TABLE, "&6充电台", "", "&fCharges Items such as Jetpacks", "", LoreBuilder.machine(MachineTier.BASIC, MachineType.MACHINE), LoreBuilder.powerBuffer(128), "&8\u21E8 &e\u26A1 &7Energy Loss: &c50%");

    public static final SlimefunItemStack VANILLA_AUTO_CRAFTER = new SlimefunItemStack("VANILLA_AUTO_CRAFTER", HeadTexture.VANILLA_AUTO_CRAFTER, "&2自动合成台 &8(原版)", "", "&fPlace this machine on top of a", "&fchest or similar and make it craft", "&fanything that can be crafted using a", "&fnormal &eCrafting Table", "", LoreBuilder.machine(MachineTier.ADVANCED, MachineType.MACHINE), "&8\u21E8 &e\u26A1 &716 J/Item");
    public static final SlimefunItemStack ENHANCED_AUTO_CRAFTER = new SlimefunItemStack("ENHANCED_AUTO_CRAFTER", HeadTexture.ENHANCED_AUTO_CRAFTER, "&2自动合成台 &8(强化)", "", "&fPlace this machine on top of a", "&fchest or similar and make it craft", "&fanything that can be crafted using an", "&eEnhanced Crafting Table", "", LoreBuilder.machine(MachineTier.ADVANCED, MachineType.MACHINE), "&8\u21E8 &e\u26A1 &716 J/Item");
    public static final SlimefunItemStack ARMOR_AUTO_CRAFTER = new SlimefunItemStack("ARMOR_AUTO_CRAFTER", HeadTexture.ARMOR_AUTO_CRAFTER, "&2自动合成台 &8(盔甲锻造)", "", "&fPlace this machine on top of a", "&fchest or similar and make it craft", "&fanything that can be crafted using an", "&eArmor Forge", "", LoreBuilder.machine(MachineTier.ADVANCED, MachineType.MACHINE), "&8\u21E8 &e\u26A1 &732 J/Item");

    public static final SlimefunItemStack IRON_GOLEM_ASSEMBLER = new SlimefunItemStack("IRON_GOLEM_ASSEMBLER", Material.IRON_BLOCK, "&6铁傀儡组装机", "", LoreBuilder.machine(MachineTier.END_GAME, MachineType.MACHINE), "&8\u21E8 &7Cooldown: &b30 Seconds", LoreBuilder.powerBuffer(4096), "&8\u21E8 &e\u26A1 &72048 J/Golem");
    public static final SlimefunItemStack WITHER_ASSEMBLER = new SlimefunItemStack("WITHER_ASSEMBLER", Material.OBSIDIAN, "&5凋灵组装机", "", LoreBuilder.machine(MachineTier.END_GAME, MachineType.MACHINE), "&8\u21E8 &7Cooldown: &b30 Seconds", LoreBuilder.powerBuffer(4096), "&8\u21E8 &e\u26A1 &74096 J/Wither");

    public static final SlimefunItemStack TRASH_CAN = new SlimefunItemStack("TRASH_CAN_BLOCK", HeadTexture.TRASH_CAN, "&3垃圾桶", "", "&fWill destroy all Items put into it");

    public static final SlimefunItemStack ELYTRA_SCALE = new SlimefunItemStack("ELYTRA_SCALE", Material.FEATHER, "&b鞘翅鳞片");
    public static final SlimefunItemStack INFUSED_ELYTRA = new SlimefunItemStack("INFUSED_ELYTRA", Material.ELYTRA, "&5注入鞘翅");
    public static final SlimefunItemStack SOULBOUND_ELYTRA = new SlimefunItemStack("SOULBOUND_ELYTRA", Material.ELYTRA, "&c灵魂绑定鞘翅");

    public static final SlimefunItemStack MAGNESIUM_SALT = new SlimefunItemStack("MAGNESIUM_SALT", Material.SUGAR, "&c镁盐", "", "&7A special type of fuel that can be", "&7used in a Magnesium-powered Generator");
    public static final SlimefunItemStack MAGNESIUM_GENERATOR = new SlimefunItemStack("MAGNESIUM_GENERATOR", HeadTexture.GENERATOR, "&c镁燃料发电机", "", LoreBuilder.machine(MachineTier.MEDIUM, MachineType.GENERATOR), LoreBuilder.powerBuffer(128), LoreBuilder.powerPerSecond(36));

    static {
        INFUSED_ELYTRA.addUnsafeEnchantment(Enchantment.MENDING, 1);
    }
}
