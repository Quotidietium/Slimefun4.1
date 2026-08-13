package io.github.thebusybiscuit.slimefun4.implementation.items.tools;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

import io.github.thebusybiscuit.slimefun4.api.events.GrapplingHookFireEvent;
import io.github.thebusybiscuit.slimefun4.api.events.PlayerRightClickEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Regression coverage for the grappling hook fire API expansion: {@link GrapplingHookFireEvent},
 * exercised by driving the real {@link GrapplingHook} {@link io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler}
 * with a constructed {@link PlayerRightClickEvent} (right-click into the air).
 * <p>
 * The fire path spawns an arrow and a bat and registers the hook; {@code Arrow#setShooter} is
 * not implemented by MockBukkit, so a RuntimeException from that tail is ignored here - the
 * arrow is already spawned (at its direction-derived location) and the lead consumed beforehand.
 *
 * @author Zurker
 */
class TestGrapplingHookFireEvent {

    private static ServerMock server;
    private static Slimefun plugin;

    private static GrapplingHook grapplingHook;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "grappling_hook_test");
        SlimefunItemStack stack = new SlimefunItemStack("_TEST_GRAPPLING_HOOK", Material.LEAD, "&fTest Grappling Hook");
        Slimefun.getItemCfg().setValue("_TEST_GRAPPLING_HOOK.enabled", true);
        grapplingHook = new GrapplingHook(itemGroup, stack, RecipeType.NULL, new ItemStack[9]);
        grapplingHook.register(plugin);
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    @BeforeEach
    public void beforeEach() {
        server.getPluginManager().clearEvents();
    }

    private void fire(Player player) {
        // MockBukkit clones the ItemStack in setItemInMainHand, so the event must be handed the
        // reference actually stored in the inventory for consumeItem(e.getItem()) to be visible.
        ItemStack handItem = player.getInventory().getItemInMainHand();
        PlayerInteractEvent interactEvent = new PlayerInteractEvent(player, Action.RIGHT_CLICK_AIR, handItem, null, null);
        PlayerRightClickEvent event = new PlayerRightClickEvent(interactEvent);

        try {
            grapplingHook.getItemHandler().onRightClick(event);
        } catch (RuntimeException ignored) {
            // Arrow#setShooter tail not fully supported by MockBukkit - see class javadoc
        }
    }

    @Test
    @DisplayName("GrapplingHookFireEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Player player = server.addPlayer();

        GrapplingHookFireEvent event = new GrapplingHookFireEvent(player, grapplingHook);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(grapplingHook, event.getGrapplingHook());
        Assertions.assertFalse(event.isCancelled());

        // The default direction is the player's eye direction scaled by the launch speed
        Vector expected = player.getEyeLocation().getDirection().multiply(2.0);
        Assertions.assertEquals(expected, event.getDirection());

        // The delegating constructor accepts an explicit direction
        Vector custom = new Vector(0, 4, 0);
        GrapplingHookFireEvent explicit = new GrapplingHookFireEvent(player, grapplingHook, custom);
        Assertions.assertEquals(custom, explicit.getDirection());

        event.setDirection(custom);
        Assertions.assertEquals(custom, event.getDirection());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new GrapplingHookFireEvent(player, null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new GrapplingHookFireEvent(player, grapplingHook, null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setDirection(null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setDirection(new Vector(0, Double.NaN, 0)), "Non-finite vector components must be rejected");
    }

    @Test
    @DisplayName("Firing the grappling hook fires the event and consumes the lead")
    void testFireFiresAndConsumes() {
        Player player = server.addPlayer();
        ItemStack lead = grapplingHook.getItem().clone();
        lead.setAmount(3);
        player.getInventory().setItemInMainHand(lead);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onFire(GrapplingHookFireEvent event) {
                seen[0] = true;
                Assertions.assertEquals(grapplingHook, event.getGrapplingHook());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            fire(player);

            Assertions.assertTrue(seen[0], "GrapplingHookFireEvent was not fired");
            Assertions.assertTrue(player.getInventory().getItemInMainHand().getAmount() < 3, "The lead must have been consumed");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Overriding the direction via setDirection changes where the hook arrow spawns")
    void testSetDirectionRedirectsArrow() {
        Player player = server.addPlayer();
        ItemStack lead = grapplingHook.getItem().clone();
        lead.setAmount(3);
        player.getInventory().setItemInMainHand(lead);

        Vector custom = new Vector(0, 4, 0);
        Listener redirecting = new Listener() {
            @EventHandler
            public void onFire(GrapplingHookFireEvent event) {
                Assertions.assertEquals(player.getEyeLocation().getDirection().multiply(2.0), event.getDirection(), "The direction must default to the eye direction");
                event.setDirection(custom);
            }
        };
        server.getPluginManager().registerEvents(redirecting, plugin);

        try {
            Set<Arrow> before = new HashSet<>(player.getWorld().getEntitiesByClass(Arrow.class));
            fire(player);

            Set<Arrow> spawned = new HashSet<>(player.getWorld().getEntitiesByClass(Arrow.class));
            spawned.removeAll(before);

            Assertions.assertEquals(1, spawned.size(), "Exactly one hook arrow must have been spawned");

            // MockBukkit aborts the fire path at Arrow#setShooter, before the velocity is applied -
            // but the spawn location is derived from the direction beforehand and proves the override
            Location expectedSpawn = player.getEyeLocation().add(custom);
            Assertions.assertEquals(expectedSpawn, spawned.iterator().next().getLocation(), "The arrow must spawn offset along the overridden direction");
        } finally {
            HandlerList.unregisterAll(redirecting);
        }
    }

    @Test
    @DisplayName("Cancelling GrapplingHookFireEvent keeps the lead untouched")
    void testEventCancellationKeepsLead() {
        Player player = server.addPlayer();
        ItemStack lead = grapplingHook.getItem().clone();
        lead.setAmount(3);
        player.getInventory().setItemInMainHand(lead);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onFire(GrapplingHookFireEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            fire(player);

            Assertions.assertEquals(3, player.getInventory().getItemInMainHand().getAmount(), "A cancelled fire must keep the lead");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Firing without listeners still consumes, preserving the old behavior")
    void testFireWithoutListenersStillConsumes() {
        Player player = server.addPlayer();
        ItemStack lead = grapplingHook.getItem().clone();
        lead.setAmount(3);
        player.getInventory().setItemInMainHand(lead);

        fire(player);

        Assertions.assertTrue(player.getInventory().getItemInMainHand().getAmount() < 3, "The lead must have been consumed");
    }
}
