package io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines.enchanting;

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

import io.github.thebusybiscuit.slimefun4.api.events.BookBindEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

import me.mrCookieSlime.Slimefun.Objects.handlers.BlockTicker;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;

/**
 * Regression coverage for the book binder API expansion: {@link BookBindEvent},
 * exercised by driving the real {@link BookBinder} {@link BlockTicker} against a
 * {@link BlockStorage}-backed binder whose input slots hold two combinable
 * enchanted books.
 * <p>
 * A bind is fully observable: both books are consumed and a
 * {@code CraftingOperation} carrying the combined book appears on the processor.
 * A vetoed bind keeps the books and starts no operation. A combination that
 * would not change anything (result equals an input) idles without an event,
 * as does a jammed output.
 *
 * @author Zurker
 */
class TestBookBindEvent {

    private static final int SLOT_A = 19;
    private static final int SLOT_B = 20;

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static BookBinder binder;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        // Unit test startups never start the integrations, the ProtectionManager is created there
        Slimefun.getIntegrations().start();
        server.getScheduler().performOneTick();

        // Non-configurable items stay DISABLED unless Items.yml says otherwise, enable it first
        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "book_binder_test");
        SlimefunItemStack stack = new SlimefunItemStack("_TEST_BOOK_BINDER", Material.DISPENSER, "&fTest Book Binder");
        Slimefun.getItemCfg().setValue("_TEST_BOOK_BINDER.enabled", true);
        binder = new BookBinder(itemGroup, stack, RecipeType.NULL, new ItemStack[9]);
        binder.setCapacity(512).setEnergyConsumption(10).setProcessingSpeed(1);
        binder.register(plugin);
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
     * Creates an enchanted book holding the given stored enchantment.
     * <p>
     * The meta must come from the ItemFactory: MockBukkit only specializes the
     * {@link EnchantmentStorageMeta} there, {@code new ItemStack(ENCHANTED_BOOK).getItemMeta()}
     * returns a plain ItemMetaMock.
     */
    private static ItemStack enchantedBook(Enchantment enchantment, int level) {
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) org.bukkit.Bukkit.getItemFactory().getItemMeta(Material.ENCHANTED_BOOK);
        meta.addStoredEnchant(enchantment, level, false);
        book.setItemMeta(meta);
        return book;
    }

    private static int storedLevel(ItemStack book, Enchantment enchantment) {
        return ((EnchantmentStorageMeta) book.getItemMeta()).getStoredEnchantLevel(enchantment);
    }

    /**
     * Places the binder as a real block backed by {@link BlockStorage} and returns its menu.
     */
    private BlockMenu placeBinder(int x, int z) {
        Block b = world.getBlockAt(x, 60, z);
        b.setType(Material.DISPENSER);
        BlockStorage.addBlockInfo(b, "id", binder.getId(), true);
        return BlockStorage.getInventory(b);
    }

    /**
     * Runs one tick of the binder's real {@link BlockTicker}.
     */
    private void tick(Block b) {
        binder.callItemHandler(BlockTicker.class, ticker -> ticker.tick(b, binder, BlockStorage.getLocationInfo(b.getLocation())));
    }

    private boolean hasOperation(Block b) {
        return binder.getMachineProcessor().getOperation(b.getLocation()) != null;
    }

    /**
     * {@link BlockMenu#consumeItem(int, int)} leaves a zero-amount stack behind instead of
     * clearing the slot, so emptiness means {@code null} or amount zero.
     */
    private void assertBooksConsumed(BlockMenu menu) {
        for (int slot : binder.getInputSlots()) {
            ItemStack item = menu.getItemInSlot(slot);
            Assertions.assertTrue(item == null || item.getAmount() == 0, "The books must have been consumed, slot " + slot + " held: " + item);
        }
    }

    private void assertBooksKept(BlockMenu menu) {
        for (int slot : binder.getInputSlots()) {
            ItemStack item = menu.getItemInSlot(slot);
            Assertions.assertNotNull(item, "A vetoed bind must keep the books, slot " + slot + " was empty");
            Assertions.assertEquals(1, item.getAmount(), "A vetoed bind must keep the books untouched");
        }
    }

    @Test
    @DisplayName("BookBindEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Block b = world.getBlockAt(1, 60, 1);
        ItemStack target = enchantedBook(Enchantment.UNBREAKING, 2);
        ItemStack source = enchantedBook(Enchantment.UNBREAKING, 1);
        ItemStack result = enchantedBook(Enchantment.UNBREAKING, 2);

        BookBindEvent event = new BookBindEvent(binder, b.getLocation(), target, source, result);

        Assertions.assertEquals(binder, event.getBinder());
        Assertions.assertEquals(b.getLocation(), event.getLocation());
        Assertions.assertEquals(target, event.getTargetBook());
        Assertions.assertEquals(source, event.getSourceBook());
        Assertions.assertEquals(result, event.getResult());
        Assertions.assertFalse(event.isCancelled());

        ItemStack replacement = enchantedBook(Enchantment.MENDING, 1);
        event.setResult(replacement);
        Assertions.assertEquals(replacement, event.getResult());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new BookBindEvent(null, b.getLocation(), target, source, result));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new BookBindEvent(binder, null, target, source, result));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new BookBindEvent(binder, b.getLocation(), null, source, result));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new BookBindEvent(binder, b.getLocation(), target, null, result));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new BookBindEvent(binder, b.getLocation(), target, source, null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setResult(null));
    }

    @Test
    @DisplayName("A binding tick fires the event, consumes both books and starts the operation")
    @SuppressWarnings("deprecation")
    void testBindFiresEventAndStartsOperation() {
        Block b = world.getBlockAt(10, 60, 10);
        BlockMenu menu = placeBinder(10, 10);
        menu.replaceExistingItem(SLOT_A, enchantedBook(Enchantment.UNBREAKING, 1));
        menu.replaceExistingItem(SLOT_B, enchantedBook(Enchantment.UNBREAKING, 1));

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onBind(BookBindEvent event) {
                seen[0] = true;
                Assertions.assertEquals(binder, event.getBinder());
                Assertions.assertEquals(b.getLocation(), event.getLocation());
                Assertions.assertEquals(Material.ENCHANTED_BOOK, event.getTargetBook().getType());
                Assertions.assertEquals(Material.ENCHANTED_BOOK, event.getSourceBook().getType());
                Assertions.assertEquals(2, storedLevel(event.getResult(), Enchantment.UNBREAKING), "Two Unbreaking I books must combine into Unbreaking II");
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            tick(b);

            Assertions.assertTrue(seen[0], "BookBindEvent was not fired");
            assertBooksConsumed(menu);
            Assertions.assertTrue(hasOperation(b), "The binding operation must have been started");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling BookBindEvent keeps both books and starts no operation")
    void testCancelKeepsBooksAndIdles() {
        Block b = world.getBlockAt(20, 60, 20);
        BlockMenu menu = placeBinder(20, 20);
        menu.replaceExistingItem(SLOT_A, enchantedBook(Enchantment.UNBREAKING, 1));
        menu.replaceExistingItem(SLOT_B, enchantedBook(Enchantment.UNBREAKING, 1));

        Listener cancelling = new Listener() {
            @EventHandler
            public void onBind(BookBindEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            tick(b);

            assertBooksKept(menu);
            Assertions.assertFalse(hasOperation(b), "A vetoed bind must not start an operation");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Binding without listeners still consumes the books, preserving the old behavior")
    void testBindWithoutListenersBinds() {
        Block b = world.getBlockAt(30, 60, 30);
        BlockMenu menu = placeBinder(30, 30);
        menu.replaceExistingItem(SLOT_A, enchantedBook(Enchantment.UNBREAKING, 1));
        menu.replaceExistingItem(SLOT_B, enchantedBook(Enchantment.UNBREAKING, 1));

        tick(b);

        assertBooksConsumed(menu);
        Assertions.assertTrue(hasOperation(b), "The binding operation must have been started");
    }

    @Test
    @DisplayName("Replacing the result via setResult bakes the replacement into the operation")
    void testSetResultRedirect() {
        Block b = world.getBlockAt(40, 60, 40);
        BlockMenu menu = placeBinder(40, 40);
        menu.replaceExistingItem(SLOT_A, enchantedBook(Enchantment.UNBREAKING, 1));
        menu.replaceExistingItem(SLOT_B, enchantedBook(Enchantment.UNBREAKING, 1));

        ItemStack replacement = enchantedBook(Enchantment.MENDING, 1);
        Listener redirecting = new Listener() {
            @EventHandler
            public void onBind(BookBindEvent event) {
                event.setResult(replacement.clone());
            }
        };
        server.getPluginManager().registerEvents(redirecting, plugin);

        try {
            tick(b);

            Assertions.assertTrue(hasOperation(b), "The binding operation must have been started");
            ItemStack produced = binder.getMachineProcessor().getOperation(b.getLocation()).getResults()[0];
            Assertions.assertTrue(replacement.isSimilar(produced), "The operation must produce the replacement book, got: " + produced);
        } finally {
            HandlerList.unregisterAll(redirecting);
        }
    }

    @Test
    @DisplayName("A combination that changes nothing fires no event and keeps the books")
    void testNoOpCombinationFiresNothing() {
        Block b = world.getBlockAt(50, 60, 50);
        BlockMenu menu = placeBinder(50, 50);
        menu.replaceExistingItem(SLOT_A, enchantedBook(Enchantment.UNBREAKING, 1));
        menu.replaceExistingItem(SLOT_B, enchantedBook(Enchantment.UNBREAKING, 2));

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onBind(BookBindEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            tick(b);

            Assertions.assertFalse(seen[0], "No event must be fired when the result equals an input");
            assertBooksKept(menu);
            Assertions.assertFalse(hasOperation(b), "No operation must have been started");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("A jammed output fires no event and keeps the books")
    void testJammedOutputFiresNothing() {
        Block b = world.getBlockAt(60, 60, 60);
        BlockMenu menu = placeBinder(60, 60);
        menu.replaceExistingItem(SLOT_A, enchantedBook(Enchantment.UNBREAKING, 1));
        menu.replaceExistingItem(SLOT_B, enchantedBook(Enchantment.UNBREAKING, 1));
        menu.replaceExistingItem(24, new ItemStack(Material.STONE, 64));
        menu.replaceExistingItem(25, new ItemStack(Material.STONE, 64));

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onBind(BookBindEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            tick(b);

            Assertions.assertFalse(seen[0], "No event must be fired when the output cannot hold the book");
            assertBooksKept(menu);
            Assertions.assertFalse(hasOperation(b), "No operation must have been started");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }
}
