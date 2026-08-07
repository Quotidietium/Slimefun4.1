package io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

import io.github.thebusybiscuit.slimefun4.api.events.AutoBrewEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

import me.mrCookieSlime.Slimefun.Objects.handlers.BlockTicker;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;

/**
 * Regression coverage for the auto brewer API expansion: {@link AutoBrewEvent},
 * exercised by driving the real {@link AutoBrewer} {@link BlockTicker} against a
 * {@link BlockStorage}-backed brewer whose input slots hold a water potion and a
 * nether wart.
 * <p>
 * A brew is fully observable: both inputs are consumed and a
 * {@code CraftingOperation} carrying the awkward potion appears on the processor.
 * A vetoed brew keeps the inputs and starts no operation. A named ingredient, an
 * unknown recipe or a jammed output idles without an event.
 *
 * @author Zurker
 */
class TestAutoBrewEvent {

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static AutoBrewer brewer;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        // Unit test startups never start the integrations, the ProtectionManager is created there
        Slimefun.getIntegrations().start();
        server.getScheduler().performOneTick();

        // Non-configurable items stay DISABLED unless Items.yml says otherwise, enable it first
        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "auto_brewer_test");
        SlimefunItemStack stack = new SlimefunItemStack("_TEST_AUTO_BREWER", Material.DISPENSER, "&fTest Auto Brewer");
        Slimefun.getItemCfg().setValue("_TEST_AUTO_BREWER.enabled", true);
        brewer = new AutoBrewer(itemGroup, stack, RecipeType.NULL, new ItemStack[9]);
        brewer.setCapacity(512).setEnergyConsumption(10).setProcessingSpeed(1);
        brewer.register(plugin);
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
     * Creates a potion of the given base type. The meta must come from the
     * ItemFactory: MockBukkit only specializes the {@link PotionMeta} there.
     */
    private static ItemStack potionOf(PotionType type) {
        ItemStack potion = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) Bukkit.getItemFactory().getItemMeta(Material.POTION);
        meta.setBasePotionType(type);
        potion.setItemMeta(meta);
        return potion;
    }

    /**
     * Places the brewer as a real block backed by {@link BlockStorage} and returns
     * its menu.
     */
    private BlockMenu placeBrewer(int x, int z) {
        Block b = world.getBlockAt(x, 60, z);
        b.setType(Material.DISPENSER);
        BlockStorage.addBlockInfo(b, "id", brewer.getId(), true);
        return BlockStorage.getInventory(b);
    }

    /**
     * Runs one tick of the brewer's real {@link BlockTicker}.
     */
    private void tick(Block b) {
        brewer.callItemHandler(BlockTicker.class, ticker -> ticker.tick(b, brewer, BlockStorage.getLocationInfo(b.getLocation())));
    }

    private boolean hasOperation(Block b) {
        return brewer.getMachineProcessor().getOperation(b.getLocation()) != null;
    }

    /**
     * {@link BlockMenu#consumeItem(int, int)} leaves a zero-amount stack behind
     * instead of clearing the slot, so emptiness means {@code null} or amount zero.
     */
    private void assertInputsConsumed(BlockMenu menu) {
        for (int slot : brewer.getInputSlots()) {
            ItemStack item = menu.getItemInSlot(slot);
            Assertions.assertTrue(item == null || item.getAmount() == 0, "The inputs must have been consumed, slot " + slot + " held: " + item);
        }
    }

    private void assertInputsKept(BlockMenu menu) {
        for (int slot : brewer.getInputSlots()) {
            ItemStack item = menu.getItemInSlot(slot);
            Assertions.assertNotNull(item, "A vetoed or skipped brew must keep the inputs, slot " + slot + " was empty");
            Assertions.assertEquals(1, item.getAmount(), "A vetoed or skipped brew must keep the inputs untouched");
        }
    }

    @Test
    @DisplayName("AutoBrewEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Block b = world.getBlockAt(1, 60, 1);
        ItemStack potion = potionOf(PotionType.WATER);
        ItemStack wart = new ItemStack(Material.NETHER_WART);
        ItemStack result = potionOf(PotionType.AWKWARD);

        AutoBrewEvent event = new AutoBrewEvent(brewer, b.getLocation(), potion, wart, result);

        Assertions.assertEquals(brewer, event.getBrewer());
        Assertions.assertEquals(b.getLocation(), event.getLocation());
        Assertions.assertEquals(potion, event.getPotion());
        Assertions.assertEquals(wart, event.getIngredient());
        Assertions.assertEquals(result, event.getResult());
        Assertions.assertFalse(event.isCancelled());

        ItemStack replacement = potionOf(PotionType.STRENGTH);
        event.setResult(replacement);
        Assertions.assertEquals(replacement, event.getResult());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new AutoBrewEvent(null, b.getLocation(), potion, wart, result));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new AutoBrewEvent(brewer, null, potion, wart, result));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new AutoBrewEvent(brewer, b.getLocation(), null, wart, result));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new AutoBrewEvent(brewer, b.getLocation(), potion, null, result));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new AutoBrewEvent(brewer, b.getLocation(), potion, wart, null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setResult(null));
    }

    @Test
    @DisplayName("A brewing tick fires the event, consumes the inputs and starts the operation")
    void testBrewFiresEventAndStartsOperation() {
        Block b = world.getBlockAt(10, 60, 10);
        BlockMenu menu = placeBrewer(10, 10);
        int[] inputSlots = brewer.getInputSlots();
        menu.replaceExistingItem(inputSlots[0], potionOf(PotionType.WATER));
        menu.replaceExistingItem(inputSlots[1], new ItemStack(Material.NETHER_WART));

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onBrew(AutoBrewEvent event) {
                seen[0] = true;
                Assertions.assertEquals(brewer, event.getBrewer());
                Assertions.assertEquals(b.getLocation(), event.getLocation());
                Assertions.assertEquals(Material.POTION, event.getPotion().getType());
                Assertions.assertEquals(PotionType.WATER, ((PotionMeta) event.getPotion().getItemMeta()).getBasePotionType(), "The input potion must be the water potion");
                Assertions.assertEquals(Material.NETHER_WART, event.getIngredient().getType());
                Assertions.assertEquals(PotionType.AWKWARD, ((PotionMeta) event.getResult().getItemMeta()).getBasePotionType(), "Water plus nether wart must brew into an awkward potion");
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            tick(b);

            Assertions.assertTrue(seen[0], "AutoBrewEvent was not fired");
            assertInputsConsumed(menu);
            Assertions.assertTrue(hasOperation(b), "The brewing operation must have been started");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling AutoBrewEvent keeps the inputs and starts no operation")
    void testCancelKeepsInputsAndIdles() {
        Block b = world.getBlockAt(20, 60, 20);
        BlockMenu menu = placeBrewer(20, 20);
        int[] inputSlots = brewer.getInputSlots();
        menu.replaceExistingItem(inputSlots[0], potionOf(PotionType.WATER));
        menu.replaceExistingItem(inputSlots[1], new ItemStack(Material.NETHER_WART));

        Listener cancelling = new Listener() {
            @EventHandler
            public void onBrew(AutoBrewEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            tick(b);

            assertInputsKept(menu);
            Assertions.assertFalse(hasOperation(b), "A vetoed brew must not start an operation");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Brewing without listeners still consumes the inputs, preserving the old behavior")
    void testBrewWithoutListenersBrews() {
        Block b = world.getBlockAt(30, 60, 30);
        BlockMenu menu = placeBrewer(30, 30);
        int[] inputSlots = brewer.getInputSlots();
        menu.replaceExistingItem(inputSlots[0], potionOf(PotionType.WATER));
        menu.replaceExistingItem(inputSlots[1], new ItemStack(Material.NETHER_WART));

        tick(b);

        assertInputsConsumed(menu);
        Assertions.assertTrue(hasOperation(b), "The brewing operation must have been started");
    }

    @Test
    @DisplayName("Replacing the result via setResult bakes the replacement into the operation")
    void testSetResultRedirect() {
        Block b = world.getBlockAt(40, 60, 40);
        BlockMenu menu = placeBrewer(40, 40);
        int[] inputSlots = brewer.getInputSlots();
        menu.replaceExistingItem(inputSlots[0], potionOf(PotionType.WATER));
        menu.replaceExistingItem(inputSlots[1], new ItemStack(Material.NETHER_WART));

        ItemStack replacement = potionOf(PotionType.STRENGTH);
        Listener redirecting = new Listener() {
            @EventHandler
            public void onBrew(AutoBrewEvent event) {
                event.setResult(replacement.clone());
            }
        };
        server.getPluginManager().registerEvents(redirecting, plugin);

        try {
            tick(b);

            Assertions.assertTrue(hasOperation(b), "The brewing operation must have been started");
            ItemStack produced = brewer.getMachineProcessor().getOperation(b.getLocation()).getResults()[0];
            Assertions.assertTrue(replacement.isSimilar(produced), "The operation must produce the replacement potion, got: " + produced);
        } finally {
            HandlerList.unregisterAll(redirecting);
        }
    }

    @Test
    @DisplayName("A named ingredient fires no event and keeps the inputs")
    void testNamedIngredientFiresNothing() {
        Block b = world.getBlockAt(50, 60, 50);
        BlockMenu menu = placeBrewer(50, 50);
        int[] inputSlots = brewer.getInputSlots();
        menu.replaceExistingItem(inputSlots[0], potionOf(PotionType.WATER));

        ItemStack namedWart = new ItemStack(Material.NETHER_WART);
        ItemMeta meta = namedWart.getItemMeta();
        meta.setDisplayName("Suspicious Wart");
        namedWart.setItemMeta(meta);
        menu.replaceExistingItem(inputSlots[1], namedWart);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onBrew(AutoBrewEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            tick(b);

            Assertions.assertFalse(seen[0], "No event must be fired for a named ingredient");
            assertInputsKept(menu);
            Assertions.assertFalse(hasOperation(b), "No operation must have been started");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("An unbrewable combination fires no event and keeps the inputs")
    void testUnknownRecipeFiresNothing() {
        Block b = world.getBlockAt(60, 60, 60);
        BlockMenu menu = placeBrewer(60, 60);
        int[] inputSlots = brewer.getInputSlots();
        menu.replaceExistingItem(inputSlots[0], potionOf(PotionType.WATER));
        menu.replaceExistingItem(inputSlots[1], new ItemStack(Material.DIRT));

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onBrew(AutoBrewEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            tick(b);

            Assertions.assertFalse(seen[0], "No event must be fired for an unbrewable combination");
            assertInputsKept(menu);
            Assertions.assertFalse(hasOperation(b), "No operation must have been started");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }
}
