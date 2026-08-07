package io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines.enchanting;

import java.util.concurrent.atomic.AtomicReference;

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

import io.github.thebusybiscuit.slimefun4.api.events.AsyncAutoDisenchanterProcessEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

import me.mrCookieSlime.Slimefun.Objects.handlers.BlockTicker;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;

/**
 * Regression coverage for the auto disenchanter API expansion:
 * {@link AsyncAutoDisenchanterProcessEvent}, exercised by driving the real
 * {@link AutoDisenchanter} {@link BlockTicker} against a {@link BlockStorage}-backed
 * disenchanter whose input slots hold an enchanted item and a plain book.
 * <p>
 * The event is asynchronous (machine tickers run on an asynchronous thread), so the
 * ticker is driven on a worker thread; listeners only capture and the assertions
 * happen back on the main thread after the join. A disenchant is fully observable:
 * both inputs are consumed and a {@code CraftingOperation} appears on the processor.
 * A vetoed disenchant keeps the inputs and starts no operation. Without a plain
 * book the process event is never reached.
 *
 * @author Zurker
 */
class TestAsyncAutoDisenchanterProcessEvent {

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static AutoDisenchanter disenchanter;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        // Unit test startups never start the integrations, the ProtectionManager is created there
        Slimefun.getIntegrations().start();
        server.getScheduler().performOneTick();

        // Non-configurable items stay DISABLED unless Items.yml says otherwise, enable it first
        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "auto_disenchanter_test");
        SlimefunItemStack stack = new SlimefunItemStack("_TEST_AUTO_DISENCHANTER", Material.DISPENSER, "&fTest Auto Disenchanter");
        Slimefun.getItemCfg().setValue("_TEST_AUTO_DISENCHANTER.enabled", true);
        disenchanter = new AutoDisenchanter(itemGroup, stack, RecipeType.NULL, new ItemStack[9]);
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

    private static ItemStack enchantedItem() {
        ItemStack item = new ItemStack(Material.DIAMOND_CHESTPLATE);
        item.addUnsafeEnchantment(Enchantment.UNBREAKING, 1);
        return item;
    }

    /**
     * Places the disenchanter as a real block backed by {@link BlockStorage} and
     * returns its menu.
     */
    private BlockMenu placeDisenchanter(int x, int z) {
        Block b = world.getBlockAt(x, 60, z);
        b.setType(Material.DISPENSER);
        BlockStorage.addBlockInfo(b, "id", disenchanter.getId(), true);
        return BlockStorage.getInventory(b);
    }

    /**
     * Runs one tick of the disenchanter's real {@link BlockTicker} on a worker
     * thread: the {@link AsyncAutoDisenchanterProcessEvent} is asynchronous and
     * MockBukkit refuses to fire it on the main thread.
     */
    private void tick(Block b) throws InterruptedException {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread worker = new Thread(() -> {
            try {
                disenchanter.callItemHandler(BlockTicker.class, ticker -> ticker.tick(b, disenchanter, BlockStorage.getLocationInfo(b.getLocation())));
            } catch (Throwable x) {
                failure.set(x);
            }
        });
        worker.start();
        worker.join(5000);

        if (failure.get() != null) {
            Assertions.fail("The disenchanter tick failed on the worker thread", failure.get());
        }
    }

    private boolean hasOperation(Block b) {
        return disenchanter.getMachineProcessor().getOperation(b.getLocation()) != null;
    }

    /**
     * {@link BlockMenu#consumeItem(int, int)} leaves a zero-amount stack behind
     * instead of clearing the slot, so emptiness means {@code null} or amount zero.
     */
    private void assertInputsConsumed(BlockMenu menu) {
        for (int slot : disenchanter.getInputSlots()) {
            ItemStack item = menu.getItemInSlot(slot);
            Assertions.assertTrue(item == null || item.getAmount() == 0, "The inputs must have been consumed, slot " + slot + " held: " + item);
        }
    }

    private void assertInputsKept(BlockMenu menu) {
        for (int slot : disenchanter.getInputSlots()) {
            ItemStack item = menu.getItemInSlot(slot);
            Assertions.assertNotNull(item, "A vetoed or skipped disenchant must keep the inputs, slot " + slot + " was empty");
            Assertions.assertEquals(1, item.getAmount(), "A vetoed or skipped disenchant must keep the inputs untouched");
        }
    }

    @Test
    @DisplayName("AsyncAutoDisenchanterProcessEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        BlockMenu menu = placeDisenchanter(1, 1);
        ItemStack item = enchantedItem();
        ItemStack book = new ItemStack(Material.BOOK);

        AsyncAutoDisenchanterProcessEvent event = new AsyncAutoDisenchanterProcessEvent(item, book, menu);

        Assertions.assertEquals(item, event.getItem());
        Assertions.assertEquals(book, event.getBook());
        Assertions.assertEquals(menu, event.getMenu());
        Assertions.assertFalse(event.isCancelled());
        Assertions.assertTrue(event.isAsynchronous(), "Machine tickers run asynchronously, the event must be asynchronous");

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new AsyncAutoDisenchanterProcessEvent(null, book, menu));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new AsyncAutoDisenchanterProcessEvent(item, null, menu));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new AsyncAutoDisenchanterProcessEvent(item, book, null));
    }

    @Test
    @DisplayName("A disenchanting tick fires the event, consumes the inputs and starts the operation")
    void testDisenchantFiresEventAndStartsOperation() throws InterruptedException {
        Block b = world.getBlockAt(10, 60, 10);
        BlockMenu menu = placeDisenchanter(10, 10);
        int[] inputSlots = disenchanter.getInputSlots();
        menu.replaceExistingItem(inputSlots[0], enchantedItem());
        menu.replaceExistingItem(inputSlots[1], new ItemStack(Material.BOOK));

        AtomicReference<AsyncAutoDisenchanterProcessEvent> seen = new AtomicReference<>();
        Listener watcher = new Listener() {
            @EventHandler
            public void onProcess(AsyncAutoDisenchanterProcessEvent event) {
                seen.set(event);
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            tick(b);

            AsyncAutoDisenchanterProcessEvent event = seen.get();
            Assertions.assertNotNull(event, "AsyncAutoDisenchanterProcessEvent was not fired");
            Assertions.assertEquals(Material.DIAMOND_CHESTPLATE, event.getItem().getType(), "The event must carry the item being disenchanted");
            Assertions.assertEquals(1, event.getItem().getEnchantmentLevel(Enchantment.UNBREAKING));
            Assertions.assertEquals(Material.BOOK, event.getBook().getType(), "The event must carry the plain book");
            Assertions.assertEquals(menu, event.getMenu());

            assertInputsConsumed(menu);
            Assertions.assertTrue(hasOperation(b), "The disenchanting operation must have been started");

            ItemStack[] results = disenchanter.getMachineProcessor().getOperation(b.getLocation()).getResults();
            Assertions.assertTrue(results[0].getEnchantments().isEmpty(), "The first result must be the disenchanted item");
            Assertions.assertEquals(Material.ENCHANTED_BOOK, results[1].getType(), "The second result must be the enchanted book");
            Assertions.assertEquals(1, ((EnchantmentStorageMeta) results[1].getItemMeta()).getStoredEnchantLevel(Enchantment.UNBREAKING), "The book must hold the transferred enchantment");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling AsyncAutoDisenchanterProcessEvent keeps the inputs and starts no operation")
    void testCancelKeepsInputsAndIdles() throws InterruptedException {
        Block b = world.getBlockAt(20, 60, 20);
        BlockMenu menu = placeDisenchanter(20, 20);
        int[] inputSlots = disenchanter.getInputSlots();
        menu.replaceExistingItem(inputSlots[0], enchantedItem());
        menu.replaceExistingItem(inputSlots[1], new ItemStack(Material.BOOK));

        AtomicReference<AsyncAutoDisenchanterProcessEvent> seen = new AtomicReference<>();
        Listener cancelling = new Listener() {
            @EventHandler
            public void onProcess(AsyncAutoDisenchanterProcessEvent event) {
                seen.set(event);
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            tick(b);

            Assertions.assertNotNull(seen.get(), "AsyncAutoDisenchanterProcessEvent was not fired");
            assertInputsKept(menu);
            Assertions.assertFalse(hasOperation(b), "A vetoed disenchant must not start an operation");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Disenchanting without listeners still consumes the inputs, preserving the old behavior")
    void testDisenchantWithoutListenersDisenchants() throws InterruptedException {
        Block b = world.getBlockAt(30, 60, 30);
        BlockMenu menu = placeDisenchanter(30, 30);
        int[] inputSlots = disenchanter.getInputSlots();
        menu.replaceExistingItem(inputSlots[0], enchantedItem());
        menu.replaceExistingItem(inputSlots[1], new ItemStack(Material.BOOK));

        tick(b);

        assertInputsConsumed(menu);
        Assertions.assertTrue(hasOperation(b), "The disenchanting operation must have been started");
    }

    @Test
    @DisplayName("A missing book never reaches the process event")
    void testMissingBookFiresNothing() throws InterruptedException {
        Block b = world.getBlockAt(40, 60, 40);
        BlockMenu menu = placeDisenchanter(40, 40);
        int[] inputSlots = disenchanter.getInputSlots();
        menu.replaceExistingItem(inputSlots[0], enchantedItem());

        AtomicReference<AsyncAutoDisenchanterProcessEvent> seen = new AtomicReference<>();
        Listener watcher = new Listener() {
            @EventHandler
            public void onProcess(AsyncAutoDisenchanterProcessEvent event) {
                seen.set(event);
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            tick(b);

            Assertions.assertNull(seen.get(), "No process event must be fired without a plain book");
            ItemStack kept = menu.getItemInSlot(inputSlots[0]);
            Assertions.assertNotNull(kept, "The item must stay in its slot");
            Assertions.assertEquals(1, kept.getAmount());
            Assertions.assertFalse(hasOperation(b), "No operation must have been started");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("An unenchanted item fires the event but nothing is consumed")
    void testUnenchantedItemFiresButDoesNothing() throws InterruptedException {
        Block b = world.getBlockAt(50, 60, 50);
        BlockMenu menu = placeDisenchanter(50, 50);
        int[] inputSlots = disenchanter.getInputSlots();
        menu.replaceExistingItem(inputSlots[0], new ItemStack(Material.DIAMOND_CHESTPLATE));
        menu.replaceExistingItem(inputSlots[1], new ItemStack(Material.BOOK));

        AtomicReference<AsyncAutoDisenchanterProcessEvent> seen = new AtomicReference<>();
        Listener watcher = new Listener() {
            @EventHandler
            public void onProcess(AsyncAutoDisenchanterProcessEvent event) {
                seen.set(event);
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            tick(b);

            Assertions.assertNotNull(seen.get(), "The process event fires at entry, before the enchantment scan");
            assertInputsKept(menu);
            Assertions.assertFalse(hasOperation(b), "No operation must have been started without enchantments");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("A jammed output fires the event but keeps the inputs")
    void testJammedOutputFiresButKeepsInputs() throws InterruptedException {
        Block b = world.getBlockAt(60, 60, 60);
        BlockMenu menu = placeDisenchanter(60, 60);
        int[] inputSlots = disenchanter.getInputSlots();
        menu.replaceExistingItem(inputSlots[0], enchantedItem());
        menu.replaceExistingItem(inputSlots[1], new ItemStack(Material.BOOK));

        for (int slot : disenchanter.getOutputSlots()) {
            menu.replaceExistingItem(slot, new ItemStack(Material.STONE, 64));
        }

        AtomicReference<AsyncAutoDisenchanterProcessEvent> seen = new AtomicReference<>();
        Listener watcher = new Listener() {
            @EventHandler
            public void onProcess(AsyncAutoDisenchanterProcessEvent event) {
                seen.set(event);
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            tick(b);

            Assertions.assertNotNull(seen.get(), "The process event fires before the output check");
            assertInputsKept(menu);
            Assertions.assertFalse(hasOperation(b), "No operation must have been started with a jammed output");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }
}
