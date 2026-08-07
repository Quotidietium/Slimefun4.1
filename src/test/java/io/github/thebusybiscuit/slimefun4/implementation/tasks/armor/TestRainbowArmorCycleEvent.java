package io.github.thebusybiscuit.slimefun4.implementation.tasks.armor;

import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

import io.github.thebusybiscuit.slimefun4.api.events.RainbowArmorCycleEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.armor.RainbowArmorPiece;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Regression coverage for the rainbow armor API expansion:
 * {@link RainbowArmorCycleEvent}, exercised by driving the real
 * {@link RainbowArmorTask#updateRainbowArmor} color application path with a fresh task
 * per test, so the global color index deterministically points at the first color.
 * <p>
 * The armor is pre-dyed: MockBukkit's {@code LeatherArmorMetaMock#getColor()} returns
 * {@code null} until a color was set, unlike CraftBukkit which falls back to the
 * default leather color. Pre-dyeing matches the real-server state the event exposes.
 *
 * @author Zurker
 */
class TestRainbowArmorCycleEvent {

    private static ServerMock server;
    private static Slimefun plugin;

    private static RainbowArmorPiece armorPiece;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        TestUtilities.createWorld(server);

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "rainbow_armor_test");
        SlimefunItemStack stack = new SlimefunItemStack("_TEST_RAINBOW_CHESTPLATE", Material.LEATHER_CHESTPLATE, "&7Test Rainbow Chestplate");
        Slimefun.getItemCfg().setValue("_TEST_RAINBOW_CHESTPLATE.enabled", true);
        armorPiece = new RainbowArmorPiece(itemGroup, stack, RecipeType.NULL, new ItemStack[9], new DyeColor[] { DyeColor.RED, DyeColor.BLUE });
        armorPiece.register(plugin);
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    @BeforeEach
    public void beforeEach() {
        server.getPluginManager().clearEvents();
    }

    private ItemStack dyedChestplate(Color color) {
        ItemStack item = new ItemStack(Material.LEATHER_CHESTPLATE);
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        meta.setColor(color);
        item.setItemMeta(meta);
        return item;
    }

    private Color colorOf(ItemStack item) {
        return ((LeatherArmorMeta) item.getItemMeta()).getColor();
    }

    @Test
    @DisplayName("RainbowArmorCycleEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Player player = server.addPlayer();
        ItemStack item = dyedChestplate(Color.GRAY);

        RainbowArmorCycleEvent event = new RainbowArmorCycleEvent(player, armorPiece, item, Color.GRAY, Color.RED);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(armorPiece, event.getArmorPiece());
        Assertions.assertEquals(item, event.getItemStack());
        Assertions.assertEquals(Color.GRAY, event.getPreviousColor());
        Assertions.assertEquals(Color.RED, event.getNewColor());
        Assertions.assertFalse(event.isCancelled());

        event.setNewColor(Color.LIME);
        Assertions.assertEquals(Color.LIME, event.getNewColor());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new RainbowArmorCycleEvent(player, null, item, Color.GRAY, Color.RED));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new RainbowArmorCycleEvent(player, armorPiece, null, Color.GRAY, Color.RED));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new RainbowArmorCycleEvent(player, armorPiece, item, null, Color.RED));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new RainbowArmorCycleEvent(player, armorPiece, item, Color.GRAY, null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setNewColor(null));
    }

    @Test
    @DisplayName("A color cycle fires the event and recolors the armor")
    void testCycleFiresEventAndRecolors() {
        Player player = server.addPlayer();
        ItemStack item = dyedChestplate(Color.GRAY);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onCycle(RainbowArmorCycleEvent event) {
                seen[0] = true;
                Assertions.assertEquals(player, event.getPlayer());
                Assertions.assertEquals(armorPiece, event.getArmorPiece());
                Assertions.assertSame(item, event.getItemStack(), "The event must carry the live armor stack");
                Assertions.assertEquals(Color.GRAY, event.getPreviousColor());
                Assertions.assertEquals(DyeColor.RED.getColor(), event.getNewColor());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            new RainbowArmorTask().updateRainbowArmor(player, item, armorPiece);

            Assertions.assertTrue(seen[0], "RainbowArmorCycleEvent was not fired");
            Assertions.assertEquals(DyeColor.RED.getColor(), colorOf(item), "The armor must have changed to the next color");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling RainbowArmorCycleEvent keeps the current color")
    void testCancelKeepsColor() {
        Player player = server.addPlayer();
        ItemStack item = dyedChestplate(Color.GRAY);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onCycle(RainbowArmorCycleEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            new RainbowArmorTask().updateRainbowArmor(player, item, armorPiece);

            Assertions.assertEquals(Color.GRAY, colorOf(item), "A vetoed cycle must keep the current color");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("A color cycle without listeners still recolors the armor, preserving the old behavior")
    void testCycleWithoutListenersRecolors() {
        Player player = server.addPlayer();
        ItemStack item = dyedChestplate(Color.GRAY);

        new RainbowArmorTask().updateRainbowArmor(player, item, armorPiece);

        Assertions.assertEquals(DyeColor.RED.getColor(), colorOf(item), "The armor must have changed to the next color");
    }

    @Test
    @DisplayName("Overriding the color via setNewColor applies the override")
    void testOverrideAppliesOverriddenColor() {
        Player player = server.addPlayer();
        ItemStack item = dyedChestplate(Color.GRAY);

        Listener overriding = new Listener() {
            @EventHandler
            public void onCycle(RainbowArmorCycleEvent event) {
                event.setNewColor(Color.LIME);
            }
        };
        server.getPluginManager().registerEvents(overriding, plugin);

        try {
            new RainbowArmorTask().updateRainbowArmor(player, item, armorPiece);

            Assertions.assertEquals(Color.LIME, colorOf(item), "The overridden color must have been applied");
        } finally {
            HandlerList.unregisterAll(overriding);
        }
    }

    @Test
    @DisplayName("A non-leather armor stack fires no event and stays unchanged")
    void testNonLeatherFiresNoEvent() {
        Player player = server.addPlayer();
        ItemStack item = new ItemStack(Material.IRON_CHESTPLATE);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onCycle(RainbowArmorCycleEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            new RainbowArmorTask().updateRainbowArmor(player, item, armorPiece);

            Assertions.assertFalse(seen[0], "No event must be fired for non-leather armor");
            // MockBukkit's hasItemMeta() is true even for fresh stacks, so the early
            // return can only be observed through the missing event.
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("A vetoed armor piece rejoins the color sequence at its next position")
    void testVetoedPieceRejoinsSequence() {
        Player player = server.addPlayer();
        ItemStack item = dyedChestplate(Color.GRAY);
        RainbowArmorTask task = new RainbowArmorTask();

        Listener cancelling = new Listener() {
            @EventHandler
            public void onCycle(RainbowArmorCycleEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            task.updateRainbowArmor(player, item, armorPiece);
            Assertions.assertEquals(Color.GRAY, colorOf(item), "A vetoed cycle must keep the current color");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }

        task.onTick();
        task.updateRainbowArmor(player, item, armorPiece);

        Assertions.assertEquals(DyeColor.BLUE.getColor(), colorOf(item), "The armor must rejoin the sequence at its next position");
    }
}
