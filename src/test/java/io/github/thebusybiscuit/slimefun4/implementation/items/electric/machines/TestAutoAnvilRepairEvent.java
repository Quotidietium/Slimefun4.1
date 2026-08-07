package io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

import io.github.thebusybiscuit.slimefun4.api.events.AutoAnvilRepairEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

import me.mrCookieSlime.Slimefun.Objects.handlers.BlockTicker;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;

/**
 * Regression coverage for the auto anvil API expansion: {@link AutoAnvilRepairEvent},
 * exercised by driving the real {@link AutoAnvil} {@link BlockTicker} against a
 * {@link BlockStorage}-backed anvil whose input slots hold a damaged pickaxe and a
 * duct tape.
 * <p>
 * A repair is fully observable: both inputs are consumed and a
 * {@code CraftingOperation} carrying the repaired item appears on the processor.
 * A vetoed repair keeps the inputs and starts no operation. An undamaged item or
 * a missing duct tape idles without an event.
 *
 * @author Zurker
 */
class TestAutoAnvilRepairEvent {

    /**
     * The test anvil uses a repair factor of 10, so one repair shaves
     * {@code maxDurability / (100 / 10)} = 1561 / 10 = 156 damage off a diamond pickaxe.
     */
    private static final int REPAIR_FACTOR = 10;
    private static final int REPAIRED_AMOUNT = 156;

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static AutoAnvil anvil;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        // Unit test startups never start the integrations, the ProtectionManager is created there
        Slimefun.getIntegrations().start();
        server.getScheduler().performOneTick();

        // Non-configurable items stay DISABLED unless Items.yml says otherwise, enable it first
        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "auto_anvil_test");
        SlimefunItemStack stack = new SlimefunItemStack("_TEST_AUTO_ANVIL", Material.DISPENSER, "&fTest Auto Anvil");
        Slimefun.getItemCfg().setValue("_TEST_AUTO_ANVIL.enabled", true);
        anvil = new AutoAnvil(itemGroup, REPAIR_FACTOR, stack, RecipeType.NULL, new ItemStack[9]);
        anvil.setCapacity(512).setEnergyConsumption(10).setProcessingSpeed(1);
        anvil.register(plugin);
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    @BeforeEach
    public void beforeEach() {
        server.getPluginManager().clearEvents();
    }

    private static ItemStack damagedPickaxe(int damage) {
        ItemStack item = new ItemStack(Material.DIAMOND_PICKAXE);
        ItemMeta meta = item.getItemMeta();
        ((Damageable) meta).setDamage(damage);
        item.setItemMeta(meta);
        return item;
    }

    private static int damageOf(ItemStack item) {
        return ((Damageable) item.getItemMeta()).getDamage();
    }

    /**
     * Places the anvil as a real block backed by {@link BlockStorage} and returns
     * its menu.
     */
    private BlockMenu placeAnvil(int x, int z) {
        Block b = world.getBlockAt(x, 60, z);
        b.setType(Material.DISPENSER);
        BlockStorage.addBlockInfo(b, "id", anvil.getId(), true);
        return BlockStorage.getInventory(b);
    }

    /**
     * Runs one tick of the anvil's real {@link BlockTicker}.
     */
    private void tick(Block b) {
        anvil.callItemHandler(BlockTicker.class, ticker -> ticker.tick(b, anvil, BlockStorage.getLocationInfo(b.getLocation())));
    }

    private boolean hasOperation(Block b) {
        return anvil.getMachineProcessor().getOperation(b.getLocation()) != null;
    }

    /**
     * {@link BlockMenu#consumeItem(int, int)} leaves a zero-amount stack behind
     * instead of clearing the slot, so emptiness means {@code null} or amount zero.
     */
    private void assertInputsConsumed(BlockMenu menu) {
        for (int slot : anvil.getInputSlots()) {
            ItemStack item = menu.getItemInSlot(slot);
            Assertions.assertTrue(item == null || item.getAmount() == 0, "The inputs must have been consumed, slot " + slot + " held: " + item);
        }
    }

    private void assertInputsKept(BlockMenu menu) {
        for (int slot : anvil.getInputSlots()) {
            ItemStack item = menu.getItemInSlot(slot);
            Assertions.assertNotNull(item, "A vetoed or skipped repair must keep the inputs, slot " + slot + " was empty");
            Assertions.assertEquals(1, item.getAmount(), "A vetoed or skipped repair must keep the inputs untouched");
        }
    }

    @Test
    @DisplayName("AutoAnvilRepairEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Block b = world.getBlockAt(1, 60, 1);
        ItemStack tape = SlimefunItems.DUCT_TAPE.item();
        ItemStack item = damagedPickaxe(500);
        ItemStack result = damagedPickaxe(344);

        AutoAnvilRepairEvent event = new AutoAnvilRepairEvent(anvil, b.getLocation(), tape, item, result);

        Assertions.assertEquals(anvil, event.getAnvil());
        Assertions.assertEquals(b.getLocation(), event.getLocation());
        Assertions.assertEquals(tape, event.getDuctTape());
        Assertions.assertEquals(item, event.getItem());
        Assertions.assertEquals(result, event.getResult());
        Assertions.assertFalse(event.isCancelled());

        ItemStack replacement = new ItemStack(Material.GOLDEN_PICKAXE);
        event.setResult(replacement);
        Assertions.assertEquals(replacement, event.getResult());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new AutoAnvilRepairEvent(null, b.getLocation(), tape, item, result));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new AutoAnvilRepairEvent(anvil, null, tape, item, result));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new AutoAnvilRepairEvent(anvil, b.getLocation(), null, item, result));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new AutoAnvilRepairEvent(anvil, b.getLocation(), tape, null, result));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new AutoAnvilRepairEvent(anvil, b.getLocation(), tape, item, null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setResult(null));
    }

    @Test
    @DisplayName("A repairing tick fires the event, consumes the inputs and starts the operation")
    void testRepairFiresEventAndStartsOperation() {
        Block b = world.getBlockAt(10, 60, 10);
        BlockMenu menu = placeAnvil(10, 10);
        int[] inputSlots = anvil.getInputSlots();
        menu.replaceExistingItem(inputSlots[0], damagedPickaxe(500));
        menu.replaceExistingItem(inputSlots[1], SlimefunItems.DUCT_TAPE.item());

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onRepair(AutoAnvilRepairEvent event) {
                seen[0] = true;
                Assertions.assertEquals(anvil, event.getAnvil());
                Assertions.assertEquals(b.getLocation(), event.getLocation());
                Assertions.assertTrue(SlimefunItems.DUCT_TAPE.item().isSimilar(event.getDuctTape()), "The event must carry the duct tape");
                Assertions.assertEquals(Material.DIAMOND_PICKAXE, event.getItem().getType());
                Assertions.assertEquals(500, damageOf(event.getItem()), "The event must carry the damaged input item");
                Assertions.assertEquals(500 - REPAIRED_AMOUNT, damageOf(event.getResult()), "The result must have 156 damage repaired off");
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            tick(b);

            Assertions.assertTrue(seen[0], "AutoAnvilRepairEvent was not fired");
            assertInputsConsumed(menu);
            Assertions.assertTrue(hasOperation(b), "The repairing operation must have been started");

            ItemStack produced = anvil.getMachineProcessor().getOperation(b.getLocation()).getResults()[0];
            Assertions.assertEquals(Material.DIAMOND_PICKAXE, produced.getType());
            Assertions.assertEquals(500 - REPAIRED_AMOUNT, damageOf(produced), "The operation must produce the repaired pickaxe");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling AutoAnvilRepairEvent keeps the inputs and starts no operation")
    void testCancelKeepsInputsAndIdles() {
        Block b = world.getBlockAt(20, 60, 20);
        BlockMenu menu = placeAnvil(20, 20);
        int[] inputSlots = anvil.getInputSlots();
        menu.replaceExistingItem(inputSlots[0], damagedPickaxe(500));
        menu.replaceExistingItem(inputSlots[1], SlimefunItems.DUCT_TAPE.item());

        Listener cancelling = new Listener() {
            @EventHandler
            public void onRepair(AutoAnvilRepairEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            tick(b);

            assertInputsKept(menu);
            Assertions.assertEquals(500, damageOf(menu.getItemInSlot(inputSlots[0])), "A vetoed repair must keep the item damaged");
            Assertions.assertFalse(hasOperation(b), "A vetoed repair must not start an operation");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Repairing without listeners still consumes the inputs, preserving the old behavior")
    void testRepairWithoutListenersRepairs() {
        Block b = world.getBlockAt(30, 60, 30);
        BlockMenu menu = placeAnvil(30, 30);
        int[] inputSlots = anvil.getInputSlots();
        menu.replaceExistingItem(inputSlots[0], damagedPickaxe(500));
        menu.replaceExistingItem(inputSlots[1], SlimefunItems.DUCT_TAPE.item());

        tick(b);

        assertInputsConsumed(menu);
        Assertions.assertTrue(hasOperation(b), "The repairing operation must have been started");
    }

    @Test
    @DisplayName("Replacing the result via setResult bakes the replacement into the operation")
    void testSetResultRedirect() {
        Block b = world.getBlockAt(40, 60, 40);
        BlockMenu menu = placeAnvil(40, 40);
        int[] inputSlots = anvil.getInputSlots();
        menu.replaceExistingItem(inputSlots[0], damagedPickaxe(500));
        menu.replaceExistingItem(inputSlots[1], SlimefunItems.DUCT_TAPE.item());

        ItemStack replacement = new ItemStack(Material.GOLDEN_PICKAXE);
        Listener redirecting = new Listener() {
            @EventHandler
            public void onRepair(AutoAnvilRepairEvent event) {
                event.setResult(replacement.clone());
            }
        };
        server.getPluginManager().registerEvents(redirecting, plugin);

        try {
            tick(b);

            Assertions.assertTrue(hasOperation(b), "The repairing operation must have been started");
            ItemStack produced = anvil.getMachineProcessor().getOperation(b.getLocation()).getResults()[0];
            Assertions.assertEquals(Material.GOLDEN_PICKAXE, produced.getType(), "The operation must produce the replacement item, got: " + produced);
            Assertions.assertEquals(0, damageOf(produced), "The replacement must be undamaged");
        } finally {
            HandlerList.unregisterAll(redirecting);
        }
    }

    @Test
    @DisplayName("An undamaged item fires no event and keeps the inputs")
    void testUndamagedItemFiresNothing() {
        Block b = world.getBlockAt(50, 60, 50);
        BlockMenu menu = placeAnvil(50, 50);
        int[] inputSlots = anvil.getInputSlots();
        menu.replaceExistingItem(inputSlots[0], new ItemStack(Material.DIAMOND_PICKAXE));
        menu.replaceExistingItem(inputSlots[1], SlimefunItems.DUCT_TAPE.item());

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onRepair(AutoAnvilRepairEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            tick(b);

            Assertions.assertFalse(seen[0], "No event must be fired for an undamaged item");
            assertInputsKept(menu);
            Assertions.assertFalse(hasOperation(b), "No operation must have been started");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("A missing duct tape fires no event and keeps the item")
    void testMissingDuctTapeFiresNothing() {
        Block b = world.getBlockAt(60, 60, 60);
        BlockMenu menu = placeAnvil(60, 60);
        int[] inputSlots = anvil.getInputSlots();
        menu.replaceExistingItem(inputSlots[0], damagedPickaxe(500));

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onRepair(AutoAnvilRepairEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            tick(b);

            Assertions.assertFalse(seen[0], "No event must be fired without duct tape");
            ItemStack kept = menu.getItemInSlot(inputSlots[0]);
            Assertions.assertNotNull(kept, "The damaged item must stay in its slot");
            Assertions.assertEquals(500, damageOf(kept));
            Assertions.assertFalse(hasOperation(b), "No operation must have been started");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }
}
