package io.github.thebusybiscuit.slimefun4.implementation.listeners;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

import io.github.thebusybiscuit.slimefun4.api.events.FarmerShoesTramplePreventEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.armor.FarmerShoes;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Regression coverage for the farmer shoes API expansion:
 * {@link FarmerShoesTramplePreventEvent}, exercised by driving the real
 * {@link SlimefunBootsListener#onTrample(PlayerInteractEvent)} with a constructed
 * {@code PHYSICAL} interact on a farmland block and a player wearing the shoes.
 * <p>
 * The protection manifests as the interact being cancelled, so tests assert it
 * end-to-end: a cancelled protection event lets the vanilla trample through.
 *
 * @author Zurker
 */
class TestFarmerShoesTramplePreventEvent {

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static FarmerShoes farmerShoes;
    private static SlimefunBootsListener listener;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "farmer_shoes_test");
        SlimefunItemStack stack = new SlimefunItemStack("_TEST_FARMER_SHOES", Material.LEATHER_BOOTS, "&7Test Farmer Shoes");
        Slimefun.getItemCfg().setValue("_TEST_FARMER_SHOES.enabled", true);
        farmerShoes = new FarmerShoes(itemGroup, stack, RecipeType.NULL, new ItemStack[9]);
        farmerShoes.register(plugin);

        listener = new SlimefunBootsListener(plugin);
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
     * Steps on the given block via the real boots listener.
     */
    private PlayerInteractEvent trample(Player player, Block b) {
        PlayerInteractEvent interactEvent = new PlayerInteractEvent(player, Action.PHYSICAL, null, b, BlockFace.UP);
        listener.onTrample(interactEvent);
        return interactEvent;
    }

    private Block placeFarmland(int x, int z) {
        Block b = world.getBlockAt(x, 1, z);
        b.setType(Material.FARMLAND);
        return b;
    }

    @Test
    @DisplayName("FarmerShoesTramplePreventEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Player player = server.addPlayer();
        Block b = placeFarmland(1, 1);
        ItemStack bootsItem = farmerShoes.getItem().clone();

        FarmerShoesTramplePreventEvent event = new FarmerShoesTramplePreventEvent(player, farmerShoes, bootsItem, b);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(farmerShoes, event.getBoots());
        Assertions.assertEquals(bootsItem, event.getBootsItem());
        Assertions.assertEquals(b, event.getFarmland());
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new FarmerShoesTramplePreventEvent(player, null, bootsItem, b));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new FarmerShoesTramplePreventEvent(player, farmerShoes, null, b));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new FarmerShoesTramplePreventEvent(player, farmerShoes, bootsItem, null));
    }

    @Test
    @DisplayName("Stepping on farmland with the shoes fires the event and cancels the trample")
    void testTrampleFiresEventAndProtects() {
        Player player = server.addPlayer();
        player.getInventory().setBoots(farmerShoes.getItem().clone());
        Block b = placeFarmland(10, 10);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onPrevent(FarmerShoesTramplePreventEvent event) {
                seen[0] = true;
                Assertions.assertEquals(player, event.getPlayer());
                Assertions.assertEquals(farmerShoes, event.getBoots());
                Assertions.assertEquals(b, event.getFarmland());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            PlayerInteractEvent interactEvent = trample(player, b);

            Assertions.assertTrue(seen[0], "FarmerShoesTramplePreventEvent was not fired");
            Assertions.assertTrue(interactEvent.isCancelled(), "The trample must have been prevented");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling FarmerShoesTramplePreventEvent lets the vanilla trample through")
    void testCancelLetsTrampleThrough() {
        Player player = server.addPlayer();
        player.getInventory().setBoots(farmerShoes.getItem().clone());
        Block b = placeFarmland(20, 20);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onPrevent(FarmerShoesTramplePreventEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            PlayerInteractEvent interactEvent = trample(player, b);

            Assertions.assertFalse(interactEvent.isCancelled(), "A vetoed protection must let the trample through");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Protecting without listeners still cancels the trample, preserving the old behavior")
    void testProtectWithoutListenersStillProtects() {
        Player player = server.addPlayer();
        player.getInventory().setBoots(farmerShoes.getItem().clone());
        Block b = placeFarmland(30, 30);

        PlayerInteractEvent interactEvent = trample(player, b);

        Assertions.assertTrue(interactEvent.isCancelled(), "The trample must have been prevented");
    }

    @Test
    @DisplayName("Stepping on farmland without the shoes fires no event")
    void testNoShoesFiresNothing() {
        Player player = server.addPlayer();
        Block b = placeFarmland(40, 40);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onPrevent(FarmerShoesTramplePreventEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            PlayerInteractEvent interactEvent = trample(player, b);

            Assertions.assertFalse(seen[0], "No event must be fired without the shoes");
            Assertions.assertFalse(interactEvent.isCancelled(), "A bare-foot trample must not be prevented");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Stepping on a non-farmland block fires no event")
    void testNonFarmlandFiresNothing() {
        Player player = server.addPlayer();
        player.getInventory().setBoots(farmerShoes.getItem().clone());
        Block b = world.getBlockAt(50, 1, 50);
        b.setType(Material.DIRT);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onPrevent(FarmerShoesTramplePreventEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            PlayerInteractEvent interactEvent = trample(player, b);

            Assertions.assertFalse(seen[0], "No event must be fired for a non-farmland block");
            Assertions.assertFalse(interactEvent.isCancelled(), "A trample on dirt must not be prevented");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }
}
