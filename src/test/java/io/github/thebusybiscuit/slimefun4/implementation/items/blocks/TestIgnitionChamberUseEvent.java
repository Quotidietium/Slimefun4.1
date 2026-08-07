package io.github.thebusybiscuit.slimefun4.implementation.items.blocks;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Dropper;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
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

import io.github.thebusybiscuit.slimefun4.api.events.IgnitionChamberUseEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

import me.mrCookieSlime.Slimefun.api.BlockStorage;

/**
 * Regression coverage for the ignition chamber API expansion:
 * {@link IgnitionChamberUseEvent}, exercised through the real
 * {@link IgnitionChamber#useFlintAndSteel(Player, Block)} path against a
 * {@link BlockStorage}-backed dropper chamber next to a dispenser standing in for the
 * smeltery.
 * <p>
 * A use damages the Flint and Steel inside the chamber, so tests assert the outcome
 * end-to-end through the item's durability: a cancelled event returns {@code false}
 * and leaves the durability untouched.
 *
 * @author Zurker
 */
class TestIgnitionChamberUseEvent {

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static IgnitionChamber chamber;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "ignition_chamber_test");
        SlimefunItemStack stack = new SlimefunItemStack("_TEST_IGNITION_CHAMBER", Material.DROPPER, "&7Test Ignition Chamber");
        Slimefun.getItemCfg().setValue("_TEST_IGNITION_CHAMBER.enabled", true);
        chamber = new IgnitionChamber(itemGroup, stack, RecipeType.NULL, new ItemStack[9]);
        chamber.register(plugin);
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
     * Places a dispenser (standing in for the smeltery) with an ignition chamber
     * dropper to its north, at a high, flat y to stay clear of the terrain.
     */
    private Block placeSmelteryWithChamber(int x, int z) {
        Block smeltery = world.getBlockAt(x, 60, z);
        smeltery.setType(Material.DISPENSER);

        Block dropper = smeltery.getRelative(BlockFace.NORTH);
        dropper.setType(Material.DROPPER);
        BlockStorage.addBlockInfo(dropper, "id", "_TEST_IGNITION_CHAMBER");

        return smeltery;
    }

    private Inventory chamberInventory(Block smeltery) {
        return ((Dropper) smeltery.getRelative(BlockFace.NORTH).getState()).getInventory();
    }

    private ItemStack flintAndSteel(int damage) {
        ItemStack item = new ItemStack(Material.FLINT_AND_STEEL);
        ItemMeta meta = item.getItemMeta();
        ((Damageable) meta).setDamage(damage);
        item.setItemMeta(meta);
        return item;
    }

    private int damageOf(Inventory inv) {
        ItemStack item = inv.getItem(0);
        Assertions.assertNotNull(item, "The flint and steel must still be in the chamber");
        return ((Damageable) item.getItemMeta()).getDamage();
    }

    @Test
    @DisplayName("IgnitionChamberUseEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Player player = server.addPlayer();
        Block smeltery = world.getBlockAt(1, 60, 1);
        Block chamberBlock = world.getBlockAt(1, 60, 2);
        ItemStack flint = new ItemStack(Material.FLINT_AND_STEEL);

        IgnitionChamberUseEvent event = new IgnitionChamberUseEvent(player, smeltery, chamberBlock, flint);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(smeltery, event.getSmelteryBlock());
        Assertions.assertEquals(chamberBlock, event.getChamber());
        Assertions.assertEquals(flint, event.getFlintAndSteel());
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new IgnitionChamberUseEvent(player, null, chamberBlock, flint));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new IgnitionChamberUseEvent(player, smeltery, null, flint));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new IgnitionChamberUseEvent(player, smeltery, chamberBlock, null));
    }

    @Test
    @DisplayName("Using the chamber fires the event and damages the flint and steel")
    void testUseFiresEventAndDamages() {
        Player player = server.addPlayer();
        Block smeltery = placeSmelteryWithChamber(100, 100);
        Inventory inv = chamberInventory(smeltery);
        inv.setItem(0, flintAndSteel(0));

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onUse(IgnitionChamberUseEvent event) {
                seen[0] = true;
                Assertions.assertEquals(player, event.getPlayer());
                Assertions.assertEquals(smeltery, event.getSmelteryBlock());
                Assertions.assertEquals(smeltery.getRelative(BlockFace.NORTH), event.getChamber());
                Assertions.assertSame(inv.getItem(0), event.getFlintAndSteel(), "The event must carry the live inventory stack");
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            Assertions.assertTrue(IgnitionChamber.useFlintAndSteel(player, smeltery), "The ignition must have succeeded");
            Assertions.assertTrue(seen[0], "IgnitionChamberUseEvent was not fired");
            Assertions.assertEquals(1, damageOf(inv), "The flint and steel must have lost one point of durability");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling IgnitionChamberUseEvent vetoes the ignition and keeps the durability")
    void testCancelVetoesIgnition() {
        Player player = server.addPlayer();
        Block smeltery = placeSmelteryWithChamber(200, 200);
        Inventory inv = chamberInventory(smeltery);
        inv.setItem(0, flintAndSteel(0));

        Listener cancelling = new Listener() {
            @EventHandler
            public void onUse(IgnitionChamberUseEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            Assertions.assertFalse(IgnitionChamber.useFlintAndSteel(player, smeltery), "A vetoed ignition must report failure");
            Assertions.assertEquals(0, damageOf(inv), "A vetoed ignition must not damage the flint and steel");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Using the chamber without listeners still damages the flint and steel, preserving the old behavior")
    void testUseWithoutListenersDamages() {
        Player player = server.addPlayer();
        Block smeltery = placeSmelteryWithChamber(300, 300);
        Inventory inv = chamberInventory(smeltery);
        inv.setItem(0, flintAndSteel(0));

        Assertions.assertTrue(IgnitionChamber.useFlintAndSteel(player, smeltery), "The ignition must have succeeded");
        Assertions.assertEquals(1, damageOf(inv), "The flint and steel must have lost one point of durability");
    }

    @Test
    @DisplayName("No adjacent chamber means no event and failure")
    void testNoChamberReturnsFalse() {
        Player player = server.addPlayer();
        Block smeltery = world.getBlockAt(400, 60, 400);
        smeltery.setType(Material.DISPENSER);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onUse(IgnitionChamberUseEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            Assertions.assertFalse(IgnitionChamber.useFlintAndSteel(player, smeltery), "No chamber must report failure");
            Assertions.assertFalse(seen[0], "No event must be fired without a chamber");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("A chamber without a flint and steel fires no event and fails")
    void testNoFlintReturnsFalse() {
        Player player = server.addPlayer();
        Block smeltery = placeSmelteryWithChamber(500, 500);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onUse(IgnitionChamberUseEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            Assertions.assertFalse(IgnitionChamber.useFlintAndSteel(player, smeltery), "No flint and steel must report failure");
            Assertions.assertFalse(seen[0], "No event must be fired without a flint and steel");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("A flint and steel on its last durability breaks after the event fired")
    void testBreakingFlintAndSteel() {
        Player player = server.addPlayer();
        Block smeltery = placeSmelteryWithChamber(600, 600);
        Inventory inv = chamberInventory(smeltery);
        inv.setItem(0, flintAndSteel(Material.FLINT_AND_STEEL.getMaxDurability() - 1));

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onUse(IgnitionChamberUseEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            Assertions.assertTrue(IgnitionChamber.useFlintAndSteel(player, smeltery), "The ignition must have succeeded");
            Assertions.assertTrue(seen[0], "IgnitionChamberUseEvent was not fired");

            ItemStack slot = inv.getItem(0);
            Assertions.assertTrue(slot == null || slot.getAmount() == 0, "The flint and steel must have broken, got: " + slot);
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }
}
