package io.github.thebusybiscuit.slimefun4.implementation.listeners.crafting;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Crafter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.inventory.CraftingRecipe;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

import io.github.thebusybiscuit.slimefun4.api.events.SlimefunItemCrafterPreventEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Regression coverage for the vanilla auto-crafter API expansion:
 * {@link SlimefunItemCrafterPreventEvent}, exercised by driving the real
 * {@link CrafterListener#onCrafterCraft(CrafterCraftEvent)} with a constructed craft event.
 * <p>
 * MockBukkit has no {@link Crafter} block state mock, so the {@link Block} / {@link Crafter} /
 * {@link Inventory} chain is stubbed with Mockito while the {@link CrafterCraftEvent} and the
 * listener run for real. The protection manifests as the craft event being cancelled, so tests
 * assert it end-to-end: a cancelled protection event lets the craft through.
 *
 * @author Zurker
 */
class TestSlimefunItemCrafterPreventEvent {

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static SlimefunItem ingredient;
    private static CrafterListener listener;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "crafter_prevent_test");
        SlimefunItemStack stack = new SlimefunItemStack("_TEST_CRAFTER_INGREDIENT", Material.POLISHED_ANDESITE, "&7Test Crafter Ingredient");
        Slimefun.getItemCfg().setValue("_TEST_CRAFTER_INGREDIENT.enabled", true);
        ingredient = new SlimefunItem(itemGroup, stack, RecipeType.NULL, new ItemStack[9]);
        ingredient.register(plugin);

        listener = new CrafterListener(plugin);
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    @BeforeEach
    public void beforeEach() {
        server.getPluginManager().clearEvents();
    }

    /**
     * Stubs a crafter block whose inventory holds the given contents.
     */
    private Block mockCrafterBlock(ItemStack... contents) {
        Block block = Mockito.mock(Block.class);
        Crafter crafter = Mockito.mock(Crafter.class);
        Inventory inventory = Mockito.mock(Inventory.class);
        Mockito.when(block.getState()).thenReturn(crafter);
        Mockito.when(crafter.getInventory()).thenReturn(inventory);
        Mockito.when(inventory.getContents()).thenReturn(contents);
        return block;
    }

    /**
     * Attempts a craft on the given block via the real crafter listener.
     */
    private CrafterCraftEvent craft(Block block) {
        CrafterCraftEvent craftEvent = new CrafterCraftEvent(block, Mockito.mock(CraftingRecipe.class), new ItemStack(Material.STONE));
        listener.onCrafterCraft(craftEvent);
        return craftEvent;
    }

    @Test
    @DisplayName("SlimefunItemCrafterPreventEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Block block = Mockito.mock(Block.class);
        ItemStack stack = ingredient.getItem().clone();

        SlimefunItemCrafterPreventEvent event = new SlimefunItemCrafterPreventEvent(ingredient, stack, block);

        Assertions.assertEquals(ingredient, event.getSlimefunItem());
        Assertions.assertEquals(stack, event.getItemStack());
        Assertions.assertEquals(block, event.getBlock());
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new SlimefunItemCrafterPreventEvent(null, stack, block));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new SlimefunItemCrafterPreventEvent(ingredient, null, block));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new SlimefunItemCrafterPreventEvent(ingredient, stack, null));
    }

    @Test
    @DisplayName("Crafting with a Slimefun ingredient fires the event and cancels the craft")
    void testSlimefunIngredientFiresEventAndPrevents() {
        ItemStack stack = ingredient.getItem().clone();
        Block block = mockCrafterBlock(stack);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onPrevent(SlimefunItemCrafterPreventEvent event) {
                seen[0] = true;
                Assertions.assertEquals(ingredient, event.getSlimefunItem());
                Assertions.assertEquals(stack, event.getItemStack());
                Assertions.assertEquals(block, event.getBlock());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            CrafterCraftEvent craftEvent = craft(block);

            Assertions.assertTrue(seen[0], "SlimefunItemCrafterPreventEvent was not fired");
            Assertions.assertTrue(craftEvent.isCancelled(), "The craft must have been prevented");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling SlimefunItemCrafterPreventEvent lets the craft through")
    void testCancelLetsCraftThrough() {
        Block block = mockCrafterBlock(ingredient.getItem().clone());

        Listener cancelling = new Listener() {
            @EventHandler
            public void onPrevent(SlimefunItemCrafterPreventEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            CrafterCraftEvent craftEvent = craft(block);

            Assertions.assertFalse(craftEvent.isCancelled(), "A vetoed protection must let the craft through");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Preventing without listeners still cancels the craft, preserving the old behavior")
    void testPreventWithoutListenersStillPrevents() {
        Block block = mockCrafterBlock(ingredient.getItem().clone());

        CrafterCraftEvent craftEvent = craft(block);

        Assertions.assertTrue(craftEvent.isCancelled(), "The craft must have been prevented");
    }

    @Test
    @DisplayName("Crafting with vanilla-only ingredients fires no event")
    void testVanillaIngredientsFireNothing() {
        Block block = mockCrafterBlock(new ItemStack(Material.DIAMOND), new ItemStack(Material.STICK));

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onPrevent(SlimefunItemCrafterPreventEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            CrafterCraftEvent craftEvent = craft(block);

            Assertions.assertFalse(seen[0], "No event must be fired for vanilla ingredients");
            Assertions.assertFalse(craftEvent.isCancelled(), "A vanilla craft must not be prevented");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("A craft event from a non-crafter block fires no event")
    void testNonCrafterBlockFiresNothing() {
        Block chest = world.getBlockAt(1, 1, 1);
        chest.setType(Material.CHEST);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onPrevent(SlimefunItemCrafterPreventEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            CrafterCraftEvent craftEvent = craft(chest);

            Assertions.assertFalse(seen[0], "No event must be fired for a non-crafter block");
            Assertions.assertFalse(craftEvent.isCancelled(), "A craft outside a crafter must not be prevented");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }
}
