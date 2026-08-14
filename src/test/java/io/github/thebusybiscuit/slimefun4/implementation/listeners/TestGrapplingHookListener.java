package io.github.thebusybiscuit.slimefun4.implementation.listeners;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
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

import io.github.thebusybiscuit.slimefun4.api.events.GrapplingHookPullEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.tools.GrapplingHook;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Regression coverage for the grappling hook landing logic in {@link GrapplingHookListener}.
 * <p>
 * Covers two correctness properties:
 * <ul>
 * <li>The landing is processed exactly once even when several Bukkit events fire for the same
 *     arrow (re-entry guard via {@link GrapplingHookEntity#markHandled()}), preventing a
 *     hook-item duplication.</li>
 * <li>A successful pull grants a one-shot fall-damage immunity ({@code onFallDamage}); this was
 *     the original intent of the {@code invulnerability} set but the entry was never added, so
 *     the cancellation was dead code.</li>
 * </ul>
 * <p>
 * The private {@code handleGrapplingHook} is driven via reflection with a Mockito {@link Arrow}
 * (whose {@code getShooter}/{@code getLocation} MockBukkit does not implement), and the player is
 * a real {@code addPlayer()} so teleport/velocity/world queries behave realistically.
 *
 * @author Zurker
 */
class TestGrapplingHookListener {

    private static ServerMock server;
    private static Slimefun plugin;
    private static GrapplingHookListener listener;
    private static GrapplingHook grapplingHookItem;

    private static Field activeHooksField;
    private static Field invulnerabilityField;
    private static Field grapplingHookItemField;
    private static Method handleGrapplingHook;

    @BeforeAll
    public static void load() throws Exception {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);

        // Register a real GrapplingHook item so the listener's isDisabled() guard passes. The
        // listener field is only wired during full item loading, which the unit-test startup does
        // not perform, so we set it explicitly.
        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "grappling_hook_listener_test");
        SlimefunItemStack stack = new SlimefunItemStack("_TEST_GRAPPLING_HOOK_LISTENER", Material.LEAD, "&fTest Grappling Hook");
        Slimefun.getItemCfg().setValue("_TEST_GRAPPLING_HOOK_LISTENER.enabled", true);
        grapplingHookItem = new GrapplingHook(itemGroup, stack, RecipeType.NULL, new ItemStack[9]);
        grapplingHookItem.register(plugin);

        listener = Slimefun.getGrapplingHookListener();

        grapplingHookItemField = GrapplingHookListener.class.getDeclaredField("grapplingHook");
        grapplingHookItemField.setAccessible(true);
        grapplingHookItemField.set(listener, grapplingHookItem);

        activeHooksField = GrapplingHookListener.class.getDeclaredField("activeHooks");
        activeHooksField.setAccessible(true);

        invulnerabilityField = GrapplingHookListener.class.getDeclaredField("invulnerability");
        invulnerabilityField.setAccessible(true);

        handleGrapplingHook = GrapplingHookListener.class.getDeclaredMethod("handleGrapplingHook", Arrow.class);
        handleGrapplingHook.setAccessible(true);
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    @BeforeEach
    public void beforeEach() {
        server.getPluginManager().clearEvents();
    }

    @SuppressWarnings("unchecked")
    private Map<UUID, GrapplingHookEntity> activeHooks() throws IllegalAccessException {
        return (Map<UUID, GrapplingHookEntity>) activeHooksField.get(listener);
    }

    @SuppressWarnings("unchecked")
    private Set<UUID> invulnerability() throws IllegalAccessException {
        return (Set<UUID>) invulnerabilityField.get(listener);
    }

    private Arrow mockArrow(Player player, Location target) {
        Arrow arrow = Mockito.mock(Arrow.class);
        Mockito.when(arrow.isValid()).thenReturn(true);
        Mockito.when(arrow.getShooter()).thenReturn(player);
        Mockito.when(arrow.getLocation()).thenReturn(target);
        return arrow;
    }

    /**
     * Builds an independent pull target one block below the player, in the same world, so the
     * close-range (&lt; 3.0) pull branch runs without teleport or division. A fresh {@link Location}
     * is used (rather than mutating {@code player.getLocation()}) because some MockBukkit
     * {@code getLocation()} implementations return a live, mutable location.
     */
    private Location targetNear(Player player) {
        Location l = player.getLocation();
        return new Location(l.getWorld(), l.getX(), l.getY() - 1, l.getZ());
    }

    private void land(Player player, Arrow arrow) throws Exception {
        handleGrapplingHook.invoke(listener, arrow);
    }

    @Test
    @DisplayName("markHandled() claims the landing exactly once")
    void testMarkHandledReturnsTrueOnlyOnce() {
        Arrow arrow = Mockito.mock(Arrow.class);
        Entity leash = Mockito.mock(Entity.class);
        Player player = server.addPlayer();

        GrapplingHookEntity hook = new GrapplingHookEntity(player, arrow, leash, false, false);

        Assertions.assertTrue(hook.markHandled(), "The first claim must succeed");
        Assertions.assertFalse(hook.markHandled(), "A second claim must be rejected");
        Assertions.assertFalse(hook.markHandled(), "Every further claim must be rejected");
    }

    @Test
    @DisplayName("A successful pull grants a one-shot fall-damage immunity")
    void testSuccessfulPullGrantsFallImmunity() throws Exception {
        Player player = server.addPlayer();
        Location target = targetNear(player);
        Arrow arrow = mockArrow(player, target);

        // dropItem=false / wasConsumed=false -> no item drop, isolating the immunity behaviour.
        GrapplingHookEntity hook = new GrapplingHookEntity(player, arrow, Mockito.mock(Entity.class), false, false);
        activeHooks().put(player.getUniqueId(), hook);

        try {
            land(player, arrow);

            Assertions.assertTrue(invulnerability().contains(player.getUniqueId()), "A successful pull must grant fall-damage immunity");
        } finally {
            activeHooks().clear();
            invulnerability().clear();
        }
    }

    @Test
    @DisplayName("An already-handled hook does not re-grant immunity (re-entry guard wired in)")
    void testLandingSkippedWhenAlreadyHandled() throws Exception {
        Player player = server.addPlayer();
        Location target = targetNear(player);
        Arrow arrow = mockArrow(player, target);

        GrapplingHookEntity hook = new GrapplingHookEntity(player, arrow, Mockito.mock(Entity.class), false, false);
        Assertions.assertTrue(hook.markHandled(), "Pre-condition: simulate a prior event having handled the landing");
        activeHooks().put(player.getUniqueId(), hook);

        try {
            land(player, arrow);

            Assertions.assertFalse(invulnerability().contains(player.getUniqueId()), "A re-entrant landing must not re-process the hook");
        } finally {
            activeHooks().clear();
            invulnerability().clear();
        }
    }

    @Test
    @DisplayName("A cancelled pull drops the hook but grants no fall-damage immunity")
    void testCancelledPullGrantsNoFallImmunity() throws Exception {
        Player player = server.addPlayer();
        Location target = targetNear(player);
        Arrow arrow = mockArrow(player, target);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onPull(GrapplingHookPullEvent e) {
                e.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        GrapplingHookEntity hook = new GrapplingHookEntity(player, arrow, Mockito.mock(Entity.class), false, false);
        activeHooks().put(player.getUniqueId(), hook);

        try {
            land(player, arrow);

            Assertions.assertFalse(invulnerability().contains(player.getUniqueId()), "A cancelled pull must not move the player nor grant immunity");
        } finally {
            HandlerList.unregisterAll(cancelling);
            activeHooks().clear();
            invulnerability().clear();
        }
    }

    @Test
    @DisplayName("Fall damage after a pull is cancelled exactly once")
    void testFallDamageConsumesImmunity() throws Exception {
        Player player = server.addPlayer();
        Location target = targetNear(player);
        Arrow arrow = mockArrow(player, target);

        GrapplingHookEntity hook = new GrapplingHookEntity(player, arrow, Mockito.mock(Entity.class), false, false);
        activeHooks().put(player.getUniqueId(), hook);

        try {
            // Drive the pull first; this grants the one-shot immunity.
            land(player, arrow);
            Assertions.assertTrue(invulnerability().contains(player.getUniqueId()));

            EntityDamageEvent firstFall = new EntityDamageEvent(player, DamageCause.FALL, 6.0);
            listener.onFallDamage(firstFall);
            Assertions.assertTrue(firstFall.isCancelled(), "The fall following a grapple must be cancelled");
            Assertions.assertFalse(invulnerability().contains(player.getUniqueId()), "The immunity must be consumed by the first fall");

            // A subsequent fall is no longer covered.
            EntityDamageEvent secondFall = new EntityDamageEvent(player, DamageCause.FALL, 6.0);
            listener.onFallDamage(secondFall);
            Assertions.assertFalse(secondFall.isCancelled(), "Only one fall may be cancelled per grapple");
        } finally {
            activeHooks().clear();
            invulnerability().clear();
        }
    }

    @Test
    @DisplayName("Non-fall damage is never cancelled by the grappling hook immunity")
    void testFallDamageIgnoresNonFall() throws Exception {
        Player player = server.addPlayer();
        Location target = targetNear(player);
        Arrow arrow = mockArrow(player, target);

        GrapplingHookEntity hook = new GrapplingHookEntity(player, arrow, Mockito.mock(Entity.class), false, false);
        activeHooks().put(player.getUniqueId(), hook);

        try {
            land(player, arrow);

            EntityDamageEvent attack = new EntityDamageEvent(player, DamageCause.ENTITY_ATTACK, 6.0);
            listener.onFallDamage(attack);
            Assertions.assertFalse(attack.isCancelled(), "Only FALL damage is covered");
            Assertions.assertTrue(invulnerability().contains(player.getUniqueId()), "Non-fall damage must not consume the immunity");
        } finally {
            activeHooks().clear();
            invulnerability().clear();
        }
    }
}
