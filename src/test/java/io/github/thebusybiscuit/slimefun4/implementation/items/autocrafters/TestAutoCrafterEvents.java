package io.github.thebusybiscuit.slimefun4.implementation.items.autocrafters;

import javax.annotation.Nonnull;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.opentest4j.TestAbortedException;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.inventory.ChestInventoryMock;

import io.github.thebusybiscuit.slimefun4.api.events.AutoCrafterCraftCompleteEvent;
import io.github.thebusybiscuit.slimefun4.api.events.AutoCrafterCraftEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

import me.mrCookieSlime.CSCoreLibPlugin.Configuration.Config;
import me.mrCookieSlime.Slimefun.api.BlockStorage;

/**
 * Regression coverage for the auto crafter API expansion: {@link AutoCrafterCraftEvent}
 * and {@link AutoCrafterCraftCompleteEvent}, exercised through the real
 * {@link AbstractAutoCrafter#tick(Block, Config)} path.
 *
 * @author Zurker
 */
class TestAutoCrafterEvents {

    private static ServerMock server;
    private static Slimefun plugin;
    private static org.bukkit.World world;

    private static AbstractAutoCrafter crafter;
    private static ShapelessRecipe vanillaRecipe;
    private static AbstractRecipe selectedRecipe;

    private static final ItemStack RESULT = new ItemStack(Material.DIAMOND);
    private static final ItemStack INGREDIENT = new ItemStack(Material.GOLD_NUGGET);

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        // Unit test startups never start the integrations, the ProtectionManager is created there
        Slimefun.getIntegrations().start();
        server.getScheduler().performOneTick();

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "auto_crafter_events");
        SlimefunItemStack stack = new SlimefunItemStack("TEST_EVENT_AUTO_CRAFTER", Material.PLAYER_HEAD, "&7Test Auto Crafter");

        crafter = new AbstractAutoCrafter(itemGroup, stack, RecipeType.NULL, new ItemStack[9]) {
            @Override
            public AbstractRecipe getSelectedRecipe(@Nonnull Block b) {
                return selectedRecipe;
            }

            @Override
            protected void updateRecipe(@Nonnull Block b, @Nonnull Player p) {}
        };
        crafter.setCapacity(100);
        crafter.setEnergyConsumption(10);
        crafter.register(plugin);

        vanillaRecipe = new ShapelessRecipe(new NamespacedKey(plugin, "auto_crafter_event_recipe"), RESULT);
        vanillaRecipe.addIngredient(INGREDIENT.getType());
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    @BeforeEach
    public void beforeEach() {
        server.getPluginManager().clearEvents();
        selectedRecipe = AbstractRecipe.of(vanillaRecipe);
    }

    /**
     * Builds a charged auto crafter with a filled chest below it and returns the
     * {@link Config} for the crafter's location, ready for a direct tick.
     */
    @Nonnull
    private Config setupCrafter(int x, int z) {
        Block crafterBlock = world.getBlockAt(x, 1, z);
        crafterBlock.setType(Material.PLAYER_HEAD);
        BlockStorage.addBlockInfo(crafterBlock, "id", crafter.getId(), true);
        BlockStorage.addBlockInfo(crafterBlock.getLocation(), "energy-charge", "50", false);

        Block chest = world.getBlockAt(x, 0, z);
        chest.setType(Material.CHEST);
        ((InventoryHolder) chest.getState()).getInventory().addItem(INGREDIENT.clone());

        return BlockStorage.getLocationInfo(crafterBlock.getLocation());
    }

    @Nonnull
    private Inventory getChestInventory(int x, int z) {
        return ((InventoryHolder) world.getBlockAt(x, 0, z).getState()).getInventory();
    }

    /**
     * Ticks the crafter at the given location. MockBukkit does not implement
     * {@code World#spawnParticle(Particle, Location, int)}, so the success path
     * dies with a {@link TestAbortedException} after the events were fired - that
     * exception is caught and ignored here.
     */
    private void tick(int x, int z, Config data) {
        try {
            crafter.tick(world.getBlockAt(x, 1, z), data);
        } catch (TestAbortedException ignored) {
            // spawnParticle is unimplemented in MockBukkit, the events fired before that
        }
    }

    // ---------- AutoCrafterCraftEvent ----------

    @Test
    @DisplayName("AutoCrafterCraftEvent exposes its fields and validates constructor arguments")
    void testCraftEventFieldsAndValidation() {
        Block block = world.getBlockAt(1, 0, 1);
        Inventory inv = new ChestInventoryMock(null, 9);
        AutoCrafterCraftEvent event = new AutoCrafterCraftEvent(crafter, block, inv, selectedRecipe);

        Assertions.assertEquals(crafter, event.getCrafter());
        Assertions.assertEquals(block, event.getBlock());
        Assertions.assertEquals(inv, event.getInventory());
        Assertions.assertEquals(selectedRecipe, event.getRecipe());
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new AutoCrafterCraftEvent(null, block, inv, selectedRecipe));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new AutoCrafterCraftEvent(crafter, null, inv, selectedRecipe));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new AutoCrafterCraftEvent(crafter, block, null, selectedRecipe));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new AutoCrafterCraftEvent(crafter, block, inv, null));
    }

    @Test
    @DisplayName("AutoCrafterCraftCompleteEvent exposes its fields and validates constructor arguments")
    void testCompleteEventFieldsAndValidation() {
        Block block = world.getBlockAt(1, 0, 1);
        Inventory inv = new ChestInventoryMock(null, 9);
        AutoCrafterCraftCompleteEvent event = new AutoCrafterCraftCompleteEvent(crafter, block, inv, selectedRecipe);

        Assertions.assertEquals(crafter, event.getCrafter());
        Assertions.assertEquals(block, event.getBlock());
        Assertions.assertEquals(inv, event.getInventory());
        Assertions.assertEquals(selectedRecipe, event.getRecipe());
        Assertions.assertEquals(RESULT, event.getResult(), "The result convenience getter must mirror the recipe result");

        Assertions.assertThrows(IllegalArgumentException.class, () -> new AutoCrafterCraftCompleteEvent(null, block, inv, selectedRecipe));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new AutoCrafterCraftCompleteEvent(crafter, null, inv, selectedRecipe));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new AutoCrafterCraftCompleteEvent(crafter, block, null, selectedRecipe));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new AutoCrafterCraftCompleteEvent(crafter, block, inv, null));
    }

    // ---------- tick wiring ----------

    @Test
    @DisplayName("A successful tick fires both events and performs the craft")
    void testTickFiresEventsAndCrafts() {
        int x = 10, z = 10;
        Config data = setupCrafter(x, z);
        Block crafterBlock = world.getBlockAt(x, 1, z);

        boolean[] craftSeen = { false };
        boolean[] completeSeen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onCraft(AutoCrafterCraftEvent event) {
                craftSeen[0] = true;
                Assertions.assertEquals(crafter, event.getCrafter());
                Assertions.assertEquals(crafterBlock, event.getBlock());
                Assertions.assertEquals(selectedRecipe, event.getRecipe());
                Assertions.assertTrue(event.getInventory().contains(INGREDIENT.getType(), 1), "The ingredients must still be present before the craft");
            }

            @EventHandler
            public void onComplete(AutoCrafterCraftCompleteEvent event) {
                completeSeen[0] = true;
                Assertions.assertEquals(crafter, event.getCrafter());
                Assertions.assertEquals(crafterBlock, event.getBlock());
                Assertions.assertEquals(selectedRecipe, event.getRecipe());
                Assertions.assertTrue(event.getInventory().containsAtLeast(RESULT, 1), "The result must already be in the inventory when the complete event fires");
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            tick(x, z, data);

            Assertions.assertTrue(craftSeen[0], "AutoCrafterCraftEvent was not fired");
            Assertions.assertTrue(completeSeen[0], "AutoCrafterCraftCompleteEvent was not fired");

            Inventory inv = getChestInventory(x, z);
            Assertions.assertFalse(inv.contains(INGREDIENT.getType(), 1), "The ingredient must have been consumed");
            Assertions.assertTrue(inv.containsAtLeast(RESULT, 1), "The result must have been produced");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling AutoCrafterCraftEvent skips the craft and fires no complete event")
    void testCraftEventCancellationSkipsCraft() {
        int x = 20, z = 20;
        Config data = setupCrafter(x, z);

        boolean[] completeSeen = { false };
        Listener cancelling = new Listener() {
            @EventHandler
            public void onCraft(AutoCrafterCraftEvent event) {
                event.setCancelled(true);
            }

            @EventHandler
            public void onComplete(AutoCrafterCraftCompleteEvent event) {
                completeSeen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            tick(x, z, data);

            Assertions.assertFalse(completeSeen[0], "A cancelled craft must not fire the complete event");

            Inventory inv = getChestInventory(x, z);
            Assertions.assertTrue(inv.contains(INGREDIENT.getType(), 1), "A cancelled craft must not consume the ingredients");
            Assertions.assertFalse(inv.containsAtLeast(RESULT, 1), "A cancelled craft must not produce the result");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("A tick without listeners still crafts, preserving the old behavior")
    void testTickWithoutListenersStillCrafts() {
        int x = 30, z = 30;
        Config data = setupCrafter(x, z);

        tick(x, z, data);

        Inventory inv = getChestInventory(x, z);
        Assertions.assertFalse(inv.contains(INGREDIENT.getType(), 1), "The ingredient must have been consumed");
        Assertions.assertTrue(inv.containsAtLeast(RESULT, 1), "The result must have been produced");
    }

    @Test
    @DisplayName("A tick without energy fires no events and does not craft")
    void testTickWithoutEnergyFiresNoEvent() {
        int x = 40, z = 40;
        Block crafterBlock = world.getBlockAt(x, 1, z);
        crafterBlock.setType(Material.PLAYER_HEAD);
        BlockStorage.addBlockInfo(crafterBlock, "id", crafter.getId(), true);

        Block chest = world.getBlockAt(x, 0, z);
        chest.setType(Material.CHEST);
        ((InventoryHolder) chest.getState()).getInventory().addItem(INGREDIENT.clone());

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onCraft(AutoCrafterCraftEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            tick(x, z, BlockStorage.getLocationInfo(crafterBlock.getLocation()));

            Assertions.assertFalse(seen[0], "No event must be fired when the crafter has no energy");

            Inventory inv = getChestInventory(x, z);
            Assertions.assertTrue(inv.contains(INGREDIENT.getType(), 1), "The ingredients must be untouched without energy");
            Assertions.assertFalse(inv.containsAtLeast(RESULT, 1));
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("A tick with a disabled recipe fires no events and does not craft")
    void testTickWithDisabledRecipeFiresNoEvent() {
        int x = 50, z = 50;
        Config data = setupCrafter(x, z);
        selectedRecipe.setEnabled(false);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onCraft(AutoCrafterCraftEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            tick(x, z, data);

            Assertions.assertFalse(seen[0], "No event must be fired when the recipe is disabled");

            Inventory inv = getChestInventory(x, z);
            Assertions.assertTrue(inv.contains(INGREDIENT.getType(), 1), "The ingredients must be untouched for a disabled recipe");
            Assertions.assertFalse(inv.containsAtLeast(RESULT, 1));
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }
}
