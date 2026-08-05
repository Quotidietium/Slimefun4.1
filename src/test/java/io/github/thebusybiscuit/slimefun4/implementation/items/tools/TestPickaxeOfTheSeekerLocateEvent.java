package io.github.thebusybiscuit.slimefun4.implementation.items.tools;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
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

import io.github.thebusybiscuit.slimefun4.api.events.PickaxeOfTheSeekerLocateEvent;
import io.github.thebusybiscuit.slimefun4.api.events.PlayerRightClickEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Regression coverage for the pickaxe of the seeker API expansion:
 * {@link PickaxeOfTheSeekerLocateEvent}, exercised by driving the real
 * {@link PickaxeOfTheSeeker} {@link io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler}
 * with a constructed {@link PlayerRightClickEvent}.
 * <p>
 * In every test the ore sits directly below the player, so the locate path ends in the
 * "look straight down" rotation (pitch 90). The durability damage tail on the cancelled path
 * is not fully supported by MockBukkit, so a RuntimeException from that tail is ignored here -
 * the event decision happened beforehand.
 *
 * @author Zurker
 */
class TestPickaxeOfTheSeekerLocateEvent {

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static PickaxeOfTheSeeker pickaxe;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "seeker_pickaxe_test");
        SlimefunItemStack stack = new SlimefunItemStack("_TEST_SEEKER_PICKAXE", Material.DIAMOND_PICKAXE, "&bTest Pickaxe of the Seeker");
        Slimefun.getItemCfg().setValue("_TEST_SEEKER_PICKAXE.enabled", true);
        pickaxe = new PickaxeOfTheSeeker(itemGroup, stack, RecipeType.NULL, new ItemStack[9]);
        pickaxe.register(plugin);
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
     * Places the player directly above an iron ore and runs the seeker's use handler.
     *
     * @return The ore {@link Block} that was placed
     */
    private Block seek(Player player, int x, int z) {
        Block ore = world.getBlockAt(x, 4, z);
        ore.setType(Material.IRON_ORE);
        player.teleport(new Location(world, x + 0.5, 5, z + 0.5));

        PlayerInteractEvent interactEvent = new PlayerInteractEvent(player, Action.RIGHT_CLICK_AIR, pickaxe.getItem().clone(), null, null);
        PlayerRightClickEvent event = new PlayerRightClickEvent(interactEvent);

        try {
            pickaxe.getItemHandler().onRightClick(event);
        } catch (RuntimeException ignored) {
            // durability damage tail not fully supported by MockBukkit - see class javadoc
        }

        return ore;
    }

    @Test
    @DisplayName("PickaxeOfTheSeekerLocateEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Player player = server.addPlayer();
        Block ore = world.getBlockAt(0, 4, 0);

        PickaxeOfTheSeekerLocateEvent event = new PickaxeOfTheSeekerLocateEvent(player, pickaxe, ore);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(pickaxe, event.getPickaxe());
        Assertions.assertEquals(ore, event.getOre());
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new PickaxeOfTheSeekerLocateEvent(player, null, ore));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new PickaxeOfTheSeekerLocateEvent(player, pickaxe, null));
    }

    @Test
    @DisplayName("Locating an ore fires the event and rotates the player towards it")
    void testSeekFiresAndRotates() {
        Player player = server.addPlayer();

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onLocate(PickaxeOfTheSeekerLocateEvent event) {
                seen[0] = true;
                Assertions.assertEquals(pickaxe, event.getPickaxe());
                Assertions.assertEquals(Material.IRON_ORE, event.getOre().getType());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            seek(player, 10, 10);

            Assertions.assertTrue(seen[0], "PickaxeOfTheSeekerLocateEvent was not fired");
            Assertions.assertEquals(90.0f, player.getLocation().getPitch(), "The player must look straight down at the ore below");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling PickaxeOfTheSeekerLocateEvent skips the rotation")
    void testEventCancellationSkipsRotation() {
        Player player = server.addPlayer();

        Listener cancelling = new Listener() {
            @EventHandler
            public void onLocate(PickaxeOfTheSeekerLocateEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            seek(player, 20, 20);

            Assertions.assertEquals(0.0f, player.getLocation().getPitch(), "A cancelled locate must not rotate the player");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Locating without listeners still rotates, preserving the old behavior")
    void testSeekWithoutListenersStillRotates() {
        Player player = server.addPlayer();

        seek(player, 30, 30);

        Assertions.assertEquals(90.0f, player.getLocation().getPitch(), "The player must look straight down at the ore below");
    }
}
