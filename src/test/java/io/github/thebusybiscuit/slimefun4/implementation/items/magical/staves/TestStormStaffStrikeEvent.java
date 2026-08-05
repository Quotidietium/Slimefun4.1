package io.github.thebusybiscuit.slimefun4.implementation.items.magical.staves;

import org.bukkit.GameMode;
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
import org.mockito.Mockito;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.UnimplementedOperationException;

import io.github.thebusybiscuit.slimefun4.api.events.PlayerRightClickEvent;
import io.github.thebusybiscuit.slimefun4.api.events.StormStaffStrikeEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Regression coverage for the storm staff API expansion: {@link StormStaffStrikeEvent},
 * exercised by driving the real {@link StormStaff}
 * {@link io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler} with a constructed
 * {@link PlayerRightClickEvent}.
 * <p>
 * MockBukkit implements neither {@code getTargetBlock} nor {@code strikeLightning}, so the
 * player is a Mockito mock whose target block is a real block of the test world, and reaching
 * the lightning strike is reported through the resulting
 * {@link UnimplementedOperationException}.
 *
 * @author Zurker
 */
class TestStormStaffStrikeEvent {

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static StormStaff stormStaff;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        // ProtectionManager is created when the integrations start
        Slimefun.getIntegrations().start();
        server.getScheduler().performOneTick();

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "storm_staff_test");
        SlimefunItemStack stack = new SlimefunItemStack("_TEST_STORM_STAFF", Material.STICK, "&9Test Storm Staff");
        Slimefun.getItemCfg().setValue("_TEST_STORM_STAFF.enabled", true);
        stormStaff = new StormStaff(itemGroup, stack, RecipeType.NULL, new ItemStack[9]);
        stormStaff.register(plugin);
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
     * Casts the staff with a Mockito player targeting the given block.
     *
     * @return true if the cast reached the lightning strike, false if it returned earlier
     */
    private boolean cast(Block target) {
        Player player = Mockito.mock(Player.class);
        Mockito.when(player.getGameMode()).thenReturn(GameMode.CREATIVE);
        Mockito.when(player.getTargetBlock(null, 30)).thenReturn(target);

        PlayerInteractEvent interactEvent = new PlayerInteractEvent(player, Action.RIGHT_CLICK_AIR, stormStaff.getItem().clone(), null, null);

        try {
            stormStaff.getItemHandler().onRightClick(new PlayerRightClickEvent(interactEvent));
            return false;
        } catch (UnimplementedOperationException expected) {
            // WorldMock.strikeLightning is unimplemented - see class javadoc
            return true;
        }
    }

    @Test
    @DisplayName("StormStaffStrikeEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Player player = server.addPlayer();
        Location loc = new Location(world, 0, 4, 0);

        StormStaffStrikeEvent event = new StormStaffStrikeEvent(player, stormStaff, loc);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(stormStaff, event.getStaff());
        Assertions.assertEquals(loc, event.getLocation());
        Assertions.assertFalse(event.isCancelled());

        Location redirected = new Location(world, 5, 4, 5);
        event.setLocation(redirected);
        Assertions.assertEquals(redirected, event.getLocation());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new StormStaffStrikeEvent(player, null, loc));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new StormStaffStrikeEvent(player, stormStaff, null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setLocation(null));
    }

    @Test
    @DisplayName("Casting the staff fires the event and strikes the targeted location")
    void testCastFiresAndStrikes() {
        Block target = world.getBlockAt(10, 4, 10);
        target.setType(Material.STONE);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onStrike(StormStaffStrikeEvent event) {
                seen[0] = true;
                Assertions.assertEquals(stormStaff, event.getStaff());
                Assertions.assertEquals(target.getLocation(), event.getLocation());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            boolean strikeReached = cast(target);

            Assertions.assertTrue(seen[0], "StormStaffStrikeEvent was not fired");
            Assertions.assertTrue(strikeReached, "The cast must have reached the lightning strike");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling StormStaffStrikeEvent skips the lightning strike")
    void testEventCancellationSkipsStrike() {
        Block target = world.getBlockAt(20, 4, 20);
        target.setType(Material.STONE);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onStrike(StormStaffStrikeEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            boolean strikeReached = cast(target);

            Assertions.assertFalse(strikeReached, "A cancelled cast must not strike lightning");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("A hungry non-creative player neither fires the event nor strikes lightning")
    void testHungryPlayerCannotCast() {
        // A real player is required here: the hungry branch sends a localized message, which
        // reads the player's language from its persistent data container.
        Player player = server.addPlayer();
        player.setGameMode(GameMode.SURVIVAL);
        player.setFoodLevel(0);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onStrike(StormStaffStrikeEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            PlayerInteractEvent interactEvent = new PlayerInteractEvent(player, Action.RIGHT_CLICK_AIR, stormStaff.getItem().clone(), null, null);
            stormStaff.getItemHandler().onRightClick(new PlayerRightClickEvent(interactEvent));

            Assertions.assertFalse(seen[0], "StormStaffStrikeEvent must not fire for a hungry player");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Casting the staff without listeners still strikes lightning, preserving the old behavior")
    void testCastWithoutListenersStillStrikes() {
        Block target = world.getBlockAt(40, 4, 40);
        target.setType(Material.STONE);

        boolean strikeReached = cast(target);

        Assertions.assertTrue(strikeReached, "The cast must have reached the lightning strike");
    }
}
