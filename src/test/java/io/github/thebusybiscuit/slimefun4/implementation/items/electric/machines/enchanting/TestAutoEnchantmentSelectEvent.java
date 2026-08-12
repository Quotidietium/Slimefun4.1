package io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines.enchanting;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

import io.github.thebusybiscuit.slimefun4.api.events.AsyncAutoEnchantmentSelectEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

import me.mrCookieSlime.Slimefun.Objects.handlers.BlockTicker;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;

/**
 * Regression coverage for the auto enchantment API expansion:
 * {@link AsyncAutoEnchantmentSelectEvent}, exercised by driving the real
 * {@link AutoEnchanter} and {@link AutoDisenchanter} {@link BlockTicker}s against
 * {@link BlockStorage}-backed machines whose input slots hold a two-enchantment setup.
 * <p>
 * The event carries the live enchantment selection: removing an entry excludes that
 * enchantment from the transfer, which is observable on the operation results - and on
 * the processing time, which is computed per selected enchantment. Cancelling vetoes
 * the whole operation and keeps the inputs.
 *
 * @author Zurker
 */
class TestAutoEnchantmentSelectEvent {

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static AutoEnchanter enchanter;
    private static AutoDisenchanter disenchanter;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        // Unit test startups never start the integrations, the ProtectionManager is created there
        Slimefun.getIntegrations().start();
        server.getScheduler().performOneTick();

        // Non-configurable items stay DISABLED unless Items.yml says otherwise, enable them first
        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "enchant_select_test");

        SlimefunItemStack enchanterStack = new SlimefunItemStack("_TEST_SELECT_ENCHANTER", Material.DISPENSER, "&fTest Select Enchanter");
        Slimefun.getItemCfg().setValue("_TEST_SELECT_ENCHANTER.enabled", true);
        enchanter = new AutoEnchanter(itemGroup, enchanterStack, RecipeType.NULL, new ItemStack[9]);
        enchanter.setCapacity(512).setEnergyConsumption(10).setProcessingSpeed(1);
        enchanter.register(plugin);

        SlimefunItemStack disenchanterStack = new SlimefunItemStack("_TEST_SELECT_DISENCHANTER", Material.DISPENSER, "&fTest Select Disenchanter");
        Slimefun.getItemCfg().setValue("_TEST_SELECT_DISENCHANTER.enabled", true);
        disenchanter = new AutoDisenchanter(itemGroup, disenchanterStack, RecipeType.NULL, new ItemStack[9]);
        disenchanter.setCapacity(512).setEnergyConsumption(10).setProcessingSpeed(1);
        disenchanter.register(plugin);
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    @BeforeEach
    public void beforeEach() {
        server.getPluginManager().clearEvents();
    }

    private static ItemStack enchantedBook() {
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        book.setItemMeta(Bukkit.getItemFactory().getItemMeta(Material.ENCHANTED_BOOK));

        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) book.getItemMeta();
        meta.addStoredEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addStoredEnchant(Enchantment.PROTECTION, 2, true);
        book.setItemMeta(meta);

        return book;
    }

    private static ItemStack enchantedItem() {
        ItemStack item = new ItemStack(Material.DIAMOND_CHESTPLATE);
        item.addUnsafeEnchantment(Enchantment.UNBREAKING, 1);
        item.addUnsafeEnchantment(Enchantment.PROTECTION, 2);
        return item;
    }

    private BlockMenu placeMachine(AbstractEnchantmentMachine machine, int x, int z) {
        Block b = world.getBlockAt(x, 60, z);
        b.setType(Material.DISPENSER);
        BlockStorage.addBlockInfo(b, "id", machine.getId(), true);
        return BlockStorage.getInventory(b);
    }

    /**
     * Runs one tick of the machine's real {@link BlockTicker} on a worker thread:
     * the event is asynchronous and MockBukkit refuses to fire it on the main thread.
     */
    private void tick(AbstractEnchantmentMachine machine, Block b) throws InterruptedException {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread worker = new Thread(() -> {
            try {
                machine.callItemHandler(BlockTicker.class, ticker -> ticker.tick(b, machine, BlockStorage.getLocationInfo(b.getLocation())));
            } catch (Throwable x) {
                failure.set(x);
            }
        });
        worker.start();
        worker.join(5000);

        if (failure.get() != null) {
            Assertions.fail("The machine tick failed on the worker thread", failure.get());
        }
    }

    private void assertInputsKept(AbstractEnchantmentMachine machine, BlockMenu menu) {
        for (int slot : machine.getInputSlots()) {
            ItemStack item = menu.getItemInSlot(slot);
            Assertions.assertNotNull(item, "A vetoed or skipped transfer must keep the inputs, slot " + slot + " was empty");
            Assertions.assertEquals(1, item.getAmount(), "A vetoed or skipped transfer must keep the inputs untouched");
        }
    }

    @Test
    @DisplayName("AsyncAutoEnchantmentSelectEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        BlockMenu menu = placeMachine(enchanter, 1, 1);
        ItemStack item = new ItemStack(Material.DIAMOND_CHESTPLATE);
        ItemStack book = enchantedBook();
        Map<Enchantment, Integer> selection = new HashMap<>();
        selection.put(Enchantment.UNBREAKING, 1);

        AsyncAutoEnchantmentSelectEvent event = new AsyncAutoEnchantmentSelectEvent(item, book, menu, selection);

        Assertions.assertEquals(item, event.getItem());
        Assertions.assertEquals(book, event.getBook());
        Assertions.assertEquals(menu, event.getMenu());
        Assertions.assertEquals(selection, event.getEnchantments());
        Assertions.assertFalse(event.isCancelled());
        Assertions.assertTrue(event.isAsynchronous(), "Machine tickers run asynchronously, the event must be asynchronous");

        // The selection is the live map
        event.getEnchantments().remove(Enchantment.UNBREAKING);
        Assertions.assertTrue(selection.isEmpty());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new AsyncAutoEnchantmentSelectEvent(null, book, menu, selection));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new AsyncAutoEnchantmentSelectEvent(item, null, menu, selection));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new AsyncAutoEnchantmentSelectEvent(item, book, null, selection));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new AsyncAutoEnchantmentSelectEvent(item, book, menu, null));
    }

    @Test
    @DisplayName("AutoEnchanter: removing an enchantment from the selection excludes it and shortens the operation")
    void testEnchanterSelectionFilter() throws InterruptedException {
        Block b = world.getBlockAt(10, 60, 10);
        BlockMenu menu = placeMachine(enchanter, 10, 10);
        int[] inputSlots = enchanter.getInputSlots();
        menu.replaceExistingItem(inputSlots[0], enchantedBook());
        menu.replaceExistingItem(inputSlots[1], new ItemStack(Material.DIAMOND_CHESTPLATE));

        AtomicReference<AsyncAutoEnchantmentSelectEvent> seen = new AtomicReference<>();
        Listener filtering = new Listener() {
            @EventHandler
            public void onSelect(AsyncAutoEnchantmentSelectEvent event) {
                if (event.getBook().getType() == Material.ENCHANTED_BOOK) {
                    seen.set(event);
                    event.getEnchantments().remove(Enchantment.UNBREAKING);
                }
            }
        };
        server.getPluginManager().registerEvents(filtering, plugin);

        try {
            tick(enchanter, b);

            Assertions.assertNotNull(seen.get(), "AsyncAutoEnchantmentSelectEvent was not fired");
            Assertions.assertEquals(2, seen.get().getEnchantments().size() + 1, "The machine must have selected both enchantments initially");

            var operation = enchanter.getMachineProcessor().getOperation(b.getLocation());
            Assertions.assertNotNull(operation, "The enchanting operation must have been started");

            ItemStack enchantedItem = operation.getResults()[0];
            Assertions.assertEquals(0, enchantedItem.getEnchantmentLevel(Enchantment.UNBREAKING), "The filtered enchantment must not be applied");
            Assertions.assertEquals(2, enchantedItem.getEnchantmentLevel(Enchantment.PROTECTION), "The remaining enchantment must be applied");
            Assertions.assertEquals(Material.BOOK, operation.getResults()[1].getType(), "The drained book must be the second output");
            Assertions.assertEquals(150, operation.getTotalTicks(), "The processing time must reflect the single remaining enchantment");
        } finally {
            HandlerList.unregisterAll(filtering);
        }
    }

    @Test
    @DisplayName("AutoEnchanter: cancelling the selection event keeps the inputs and starts no operation")
    void testEnchanterSelectionCancel() throws InterruptedException {
        Block b = world.getBlockAt(20, 60, 20);
        BlockMenu menu = placeMachine(enchanter, 20, 20);
        int[] inputSlots = enchanter.getInputSlots();
        menu.replaceExistingItem(inputSlots[0], enchantedBook());
        menu.replaceExistingItem(inputSlots[1], new ItemStack(Material.DIAMOND_CHESTPLATE));

        Listener cancelling = new Listener() {
            @EventHandler
            public void onSelect(AsyncAutoEnchantmentSelectEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            tick(enchanter, b);

            assertInputsKept(enchanter, menu);
            Assertions.assertNull(enchanter.getMachineProcessor().getOperation(b.getLocation()), "A vetoed enchant must not start an operation");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("AutoDisenchanter: removing an enchantment from the selection keeps it on the item and off the book")
    void testDisenchanterSelectionFilter() throws InterruptedException {
        Block b = world.getBlockAt(30, 60, 30);
        BlockMenu menu = placeMachine(disenchanter, 30, 30);
        int[] inputSlots = disenchanter.getInputSlots();
        menu.replaceExistingItem(inputSlots[0], enchantedItem());
        menu.replaceExistingItem(inputSlots[1], new ItemStack(Material.BOOK));

        Listener filtering = new Listener() {
            @EventHandler
            public void onSelect(AsyncAutoEnchantmentSelectEvent event) {
                if (event.getBook().getType() == Material.BOOK) {
                    event.getEnchantments().remove(Enchantment.PROTECTION);
                }
            }
        };
        server.getPluginManager().registerEvents(filtering, plugin);

        try {
            tick(disenchanter, b);

            var operation = disenchanter.getMachineProcessor().getOperation(b.getLocation());
            Assertions.assertNotNull(operation, "The disenchanting operation must have been started");

            ItemStack strippedItem = operation.getResults()[0];
            Assertions.assertEquals(0, strippedItem.getEnchantmentLevel(Enchantment.UNBREAKING), "The selected enchantment must have been removed from the item");
            Assertions.assertEquals(2, strippedItem.getEnchantmentLevel(Enchantment.PROTECTION), "The filtered enchantment must stay on the item");

            EnchantmentStorageMeta bookMeta = (EnchantmentStorageMeta) operation.getResults()[1].getItemMeta();
            Assertions.assertEquals(1, bookMeta.getStoredEnchantLevel(Enchantment.UNBREAKING), "The book must hold the selected enchantment");
            Assertions.assertFalse(bookMeta.hasStoredEnchant(Enchantment.PROTECTION), "The book must not hold the filtered enchantment");
            Assertions.assertEquals(180, operation.getTotalTicks(), "The processing time must reflect the single selected enchantment");
        } finally {
            HandlerList.unregisterAll(filtering);
        }
    }

    @Test
    @DisplayName("AutoDisenchanter: cancelling the selection event keeps the inputs and starts no operation")
    void testDisenchanterSelectionCancel() throws InterruptedException {
        Block b = world.getBlockAt(40, 60, 40);
        BlockMenu menu = placeMachine(disenchanter, 40, 40);
        int[] inputSlots = disenchanter.getInputSlots();
        menu.replaceExistingItem(inputSlots[0], enchantedItem());
        menu.replaceExistingItem(inputSlots[1], new ItemStack(Material.BOOK));

        Listener cancelling = new Listener() {
            @EventHandler
            public void onSelect(AsyncAutoEnchantmentSelectEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            tick(disenchanter, b);

            assertInputsKept(disenchanter, menu);
            Assertions.assertNull(disenchanter.getMachineProcessor().getOperation(b.getLocation()), "A vetoed disenchant must not start an operation");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("An untouched selection reproduces the full transfer, preserving the old behavior")
    void testUntouchedSelectionTransfersEverything() throws InterruptedException {
        Block b = world.getBlockAt(50, 60, 50);
        BlockMenu menu = placeMachine(disenchanter, 50, 50);
        int[] inputSlots = disenchanter.getInputSlots();
        menu.replaceExistingItem(inputSlots[0], enchantedItem());
        menu.replaceExistingItem(inputSlots[1], new ItemStack(Material.BOOK));

        Listener watcher = new Listener() {
            @EventHandler
            public void onSelect(AsyncAutoEnchantmentSelectEvent event) {
                // Only observe, do not touch the selection
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            tick(disenchanter, b);

            var operation = disenchanter.getMachineProcessor().getOperation(b.getLocation());
            Assertions.assertNotNull(operation, "The disenchanting operation must have been started");
            Assertions.assertTrue(operation.getResults()[0].getEnchantments().isEmpty(), "An untouched selection must strip every enchantment");
            Assertions.assertEquals(360, operation.getTotalTicks(), "An untouched selection must reproduce the per-enchantment processing time");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }
}
