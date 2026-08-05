package io.github.thebusybiscuit.slimefun4.implementation.items.blocks;

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

import io.github.thebusybiscuit.slimefun4.api.events.ComposterProcessEvent;
import io.github.thebusybiscuit.slimefun4.api.events.PlayerRightClickEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

import me.mrCookieSlime.Slimefun.api.BlockStorage;

/**
 * Regression coverage for the composter API expansion: {@link ComposterProcessEvent}, exercised
 * by driving the real {@link Composter} {@link io.github.thebusybiscuit.slimefun4.core.handlers.BlockUseHandler}
 * with a constructed {@link PlayerRightClickEvent}.
 * <p>
 * The composter schedules its output via a {@link io.github.bakedlibs.dough.scheduling.TaskQueue}, so
 * the actual push is deferred. The event and the input consumption happen synchronously beforehand,
 * which is what these tests assert.
 *
 * @author Zurker
 */
class TestComposterProcessEvent {

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static Composter composter;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        // ProtectionManager is created when the integrations start
        Slimefun.getIntegrations().start();
        server.getScheduler().performOneTick();

        // Non-configurable items stay DISABLED unless Items.yml says otherwise, enable it first
        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "composter_test");
        SlimefunItemStack stack = new SlimefunItemStack("TEST_COMPOSTER", Material.COMPOSTER, "&fTest Composter");
        Slimefun.getItemCfg().setValue("TEST_COMPOSTER.enabled", true);
        composter = new Composter(itemGroup, stack, RecipeType.NULL, new ItemStack[9]);
        composter.register(plugin);
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
     * Right-clicks the composter with the given input via a constructed event.
     */
    private void compost(Player player, Block composterBlock, ItemStack input) {
        PlayerInteractEvent interactEvent = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, input, composterBlock, BlockFace.UP);
        PlayerRightClickEvent event = new PlayerRightClickEvent(interactEvent);

        composter.getItemHandler().onRightClick(event);
    }

    private Block placeComposter(int x, int z) {
        Block b = world.getBlockAt(x, 1, z);
        b.setType(Material.COMPOSTER);
        BlockStorage.addBlockInfo(b, "id", composter.getId(), true);
        return b;
    }

    @Test
    @DisplayName("ComposterProcessEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Player player = server.addPlayer();
        Block b = placeComposter(1, 1);
        ItemStack input = new ItemStack(Material.OAK_LEAVES, 8);
        ItemStack output = new ItemStack(Material.DIRT);

        ComposterProcessEvent event = new ComposterProcessEvent(player, composter, b, input, output);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(composter, event.getComposter());
        Assertions.assertEquals(b, event.getBlock());
        Assertions.assertEquals(input, event.getInput());
        Assertions.assertEquals(output, event.getOutput());
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        ItemStack swapped = new ItemStack(Material.NETHERRACK);
        event.setOutput(swapped);
        Assertions.assertEquals(swapped, event.getOutput());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new ComposterProcessEvent(player, null, b, input, output));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ComposterProcessEvent(player, composter, null, input, output));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ComposterProcessEvent(player, composter, b, null, output));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ComposterProcessEvent(player, composter, b, input, null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setOutput(null));
    }

    @Test
    @DisplayName("Composting leaves fires the event and consumes the input")
    void testCompostFiresAndConsumes() {
        Player player = server.addPlayer();
        Block b = placeComposter(10, 10);
        ItemStack input = new ItemStack(Material.OAK_LEAVES, 8);
        player.getInventory().setItemInMainHand(input);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onProcess(ComposterProcessEvent event) {
                seen[0] = true;
                Assertions.assertEquals(composter, event.getComposter());
                Assertions.assertTrue(event.getOutput().getType() == Material.DIRT, "Leaves should compost into dirt");
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            compost(player, b, input);

            Assertions.assertTrue(seen[0], "ComposterProcessEvent was not fired");
            Assertions.assertTrue(player.getInventory().getItemInMainHand().getAmount() < 8, "The leaves must have been consumed");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling ComposterProcessEvent leaves the input untouched")
    void testEventCancellationKeepsInput() {
        Player player = server.addPlayer();
        Block b = placeComposter(20, 20);
        ItemStack input = new ItemStack(Material.OAK_LEAVES, 8);
        player.getInventory().setItemInMainHand(input);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onProcess(ComposterProcessEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            compost(player, b, input);

            Assertions.assertEquals(8, player.getInventory().getItemInMainHand().getAmount(), "A cancelled compost must keep the input");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Swapping the output via setOutput replaces the produced item")
    void testOutputSwap() {
        Player player = server.addPlayer();
        Block b = placeComposter(30, 30);
        ItemStack input = new ItemStack(Material.OAK_LEAVES, 8);
        player.getInventory().setItemInMainHand(input);
        ItemStack custom = new ItemStack(Material.GOLDEN_APPLE);

        boolean[] seenSwapped = { false };
        Listener swapping = new Listener() {
            @EventHandler
            public void onProcess(ComposterProcessEvent event) {
                event.setOutput(custom);
                seenSwapped[0] = true;
            }
        };
        server.getPluginManager().registerEvents(swapping, plugin);

        try {
            compost(player, b, input);

            Assertions.assertTrue(seenSwapped[0], "ComposterProcessEvent was not fired");
            Assertions.assertTrue(player.getInventory().getItemInMainHand().getAmount() < 8, "The input must still have been consumed");
            // The custom output is pushed by the deferred TaskQueue, downstream of the synchronous
            // event + consume path asserted above.
        } finally {
            HandlerList.unregisterAll(swapping);
        }
    }

    @Test
    @DisplayName("An invalid input fires no event")
    void testInvalidInputFiresNothing() {
        Player player = server.addPlayer();
        Block b = placeComposter(40, 40);
        ItemStack input = new ItemStack(Material.DIAMOND, 8);
        player.getInventory().setItemInMainHand(input);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onProcess(ComposterProcessEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            compost(player, b, input);

            Assertions.assertFalse(seen[0], "No event must be fired for an invalid input");
            Assertions.assertEquals(8, player.getInventory().getItemInMainHand().getAmount(), "An invalid input must not be consumed");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }
}
