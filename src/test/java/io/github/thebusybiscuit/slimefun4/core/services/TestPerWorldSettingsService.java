package io.github.thebusybiscuit.slimefun4.core.services;

import java.util.Locale;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import org.bukkit.World;
import io.github.bakedlibs.dough.config.Config;
import org.bukkit.plugin.java.JavaPlugin;

import io.github.thebusybiscuit.slimefun4.api.SlimefunAddon;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Regression coverage for {@link PerWorldSettingsService#isAddonEnabled(World, io.github.thebusybiscuit.slimefun4.api.SlimefunAddon)}
 * and the addon-level disable semantics in {@code loadItemsFromWorldConfig}: the config polarity
 * used to be inverted (".enabled" read as "is disabled") and every addon's blacklist entry was
 * keyed by the Slimefun plugin itself - together this made isAddonEnabled(...) return false for
 * every non-Slimefun addon in every world.
 *
 * <p>
 * Every test uses its own fresh {@link World} (and therefore its own settings file), which the
 * test pre-writes before the first {@link PerWorldSettingsService#load(World)} call resolves it.
 *
 * @author Zurker
 */
class TestPerWorldSettingsService {

    private static ServerMock server;
    private static Slimefun plugin;
    private static ItemGroup group;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        group = TestUtilities.getItemGroup(plugin, "per_world_test");
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    private static SlimefunItem registerItem(String id) {
        SlimefunItemStack stack = new SlimefunItemStack(id, Material.TORCH, "&7Test " + id);
        Slimefun.getItemCfg().setValue(id + ".enabled", true);
        SlimefunItem item = new SlimefunItem(group, stack, RecipeType.NULL, new ItemStack[9]);
        item.register(plugin);
        return item;
    }

    /**
     * Pre-writes the world settings file for the given fresh world and resolves the
     * world's settings exactly once (the service only loads a world once, putIfAbsent).
     */
    private void prepare(World world, String addonKey, String itemId, Boolean addonEnabled, Boolean itemEnabled) {
        Config config = new Config(plugin, "world-settings/" + world.getName() + ".yml");
        config.setValue("enabled", true);

        if (addonEnabled != null) {
            config.setValue(addonKey + ".enabled", addonEnabled);
        }

        if (itemEnabled != null) {
            config.setValue(addonKey + '.' + itemId, itemEnabled);
        }

        config.save();
        Slimefun.getWorldSettingsService().load(world);
    }

    @Test
    @DisplayName("An enabled addon reports as enabled in an enabled world")
    void testEnabledAddonReportsEnabled() {
        SlimefunItem item = registerItem("_TEST_PWS_ITEM_A");
        World world = TestUtilities.createWorld(server);

        prepare(world, "slimefun", item.getId(), true, true);

        PerWorldSettingsService service = Slimefun.getWorldSettingsService();
        Assertions.assertTrue(service.isWorldEnabled(world), "Sanity: the world itself is enabled");
        Assertions.assertTrue(service.isAddonEnabled(world, item.getAddon()), "The (enabled) Slimefun addon must report as enabled");
        Assertions.assertTrue(service.isEnabled(world, item), "A default-enabled item must be enabled in an enabled world");
    }

    @Test
    @DisplayName("An addon disabled via .enabled=false reports disabled and disables all of its items")
    void testAddonDisableSemantics() {
        SlimefunItem item = registerItem("_TEST_PWS_ITEM_B");
        World world = TestUtilities.createWorld(server);

        // The item's own key stays true - disabling the whole addon must still disable it
        prepare(world, "slimefun", item.getId(), false, true);

        PerWorldSettingsService service = Slimefun.getWorldSettingsService();
        Assertions.assertFalse(service.isAddonEnabled(world, item.getAddon()), "An addon disabled via .enabled=false must report as disabled");
        Assertions.assertFalse(service.isEnabled(world, item), "Disabling the addon must disable all of its items in that world");
    }

    @Test
    @DisplayName("An item disabled individually stays disabled while the addon itself reports enabled")
    void testItemDisableSemantics() {
        SlimefunItem item = registerItem("_TEST_PWS_ITEM_C");
        World world = TestUtilities.createWorld(server);

        prepare(world, "slimefun", item.getId(), true, false);

        PerWorldSettingsService service = Slimefun.getWorldSettingsService();
        Assertions.assertTrue(service.isAddonEnabled(world, item.getAddon()), "The addon itself stays enabled");
        Assertions.assertFalse(service.isEnabled(world, item), "The individually disabled item must be disabled in that world");
    }

    @Test
    @DisplayName("Defaults (no keys written) keep the addon and item enabled")
    void testDefaults() {
        SlimefunItem item = registerItem("_TEST_PWS_ITEM_D");
        World world = TestUtilities.createWorld(server);

        prepare(world, "slimefun", item.getId(), null, null);

        PerWorldSettingsService service = Slimefun.getWorldSettingsService();
        Assertions.assertTrue(service.isAddonEnabled(world, item.getAddon()), "A fresh world must report the addon as enabled (default)");
        Assertions.assertTrue(service.isEnabled(world, item), "A fresh world must report the item as enabled (default)");
    }

    @Test
    @DisplayName("Addon enablement is evaluated per world (one disabled world does not leak into another)")
    void testPerWorldIsolation() {
        SlimefunItem item = registerItem("_TEST_PWS_ITEM_E");
        World disabledWorld = TestUtilities.createWorld(server);
        World otherWorld = TestUtilities.createWorld(server);

        prepare(disabledWorld, "slimefun", item.getId(), false, true);
        prepare(otherWorld, "slimefun", item.getId(), null, null);

        PerWorldSettingsService service = Slimefun.getWorldSettingsService();
        Assertions.assertFalse(service.isAddonEnabled(disabledWorld, item.getAddon()), "Sanity: disabled in its world");
        Assertions.assertTrue(service.isAddonEnabled(otherWorld, item.getAddon()), "The other world must not inherit the disable");
    }

    @Test
    @DisplayName("A foreign (non-Slimefun) addon reports as enabled on a fresh world")
    void testForeignAddonReportsEnabled() {
        /*
         * The original bug keyed every addon's blacklist entry under the Slimefun plugin
         * itself, so isAddonEnabled(...) could never find an entry for a foreign addon
         * and returned false for it in every world. This is the discriminating case.
         */
        SlimefunAddon foreignAddon = new SlimefunAddon() {
            @Override
            public JavaPlugin getJavaPlugin() {
                return plugin;
            }

            @Override
            public String getBugTrackerURL() {
                return null;
            }

            @Override
            public String getName() {
                return "ForeignAddon";
            }
        };

        SlimefunItemStack stack = new SlimefunItemStack("_TEST_PWS_FOREIGN", Material.LANTERN, "&7Foreign");
        Slimefun.getItemCfg().setValue("_TEST_PWS_FOREIGN.enabled", true);
        SlimefunItem item = new SlimefunItem(group, stack, RecipeType.NULL, new ItemStack[9]);
        item.register(foreignAddon);

        World world = TestUtilities.createWorld(server);
        prepare(world, "foreignaddon", item.getId(), null, null);

        PerWorldSettingsService service = Slimefun.getWorldSettingsService();
        Assertions.assertTrue(service.isAddonEnabled(world, foreignAddon), "A foreign addon on a fresh world must report as enabled");
        Assertions.assertTrue(service.isEnabled(world, item), "Its default-enabled item must be enabled");
    }
}
