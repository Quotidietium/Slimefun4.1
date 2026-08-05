package io.github.thebusybiscuit.slimefun4.implementation.items.weapons;

import java.util.ArrayList;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;

import io.github.thebusybiscuit.slimefun4.api.events.IcyBowFreezeEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Regression coverage for the icy bow API expansion: {@link IcyBowFreezeEvent}, exercised by
 * driving the real {@link IcyBow}
 * {@link io.github.thebusybiscuit.slimefun4.core.handlers.BowShootHandler} with a constructed
 * {@link EntityDamageByEntityEvent}.
 * <p>
 * The ice particle tail ({@code playEffect(STEP_SOUND, Material)}) is rejected by MockBukkit
 * with an {@link IllegalArgumentException}, so the helper reports whether that tail was
 * reached. The freeze ticks are applied to a player target before the tail and are directly
 * observable.
 *
 * @author Zurker
 */
class TestIcyBowFreezeEvent {

    private static ServerMock server;
    private static Slimefun plugin;

    private static IcyBow icyBow;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "icy_bow_test");
        SlimefunItemStack stack = new SlimefunItemStack("_TEST_ICY_BOW", Material.BOW, "&bTest Icy Bow");
        Slimefun.getItemCfg().setValue("_TEST_ICY_BOW.enabled", true);
        icyBow = new IcyBow(itemGroup, stack, new ItemStack[9]);
        icyBow.register(plugin);
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
     * Hits the target with an icy arrow via the real handler.
     *
     * @return true if the handler reached the ice particle tail, false if it returned earlier
     */
    private boolean hit(PlayerMock target) {
        Player shooter = server.addPlayer();
        EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(shooter, target, DamageCause.ENTITY_ATTACK, 4.0);

        try {
            icyBow.onShoot().onHit(event, target);
            return false;
        } catch (IllegalArgumentException ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("Wrong kind of data")) {
                // MockBukkit rejects playEffect(STEP_SOUND, Material) - see class javadoc
                return true;
            }

            throw ex;
        }
    }

    @Test
    @DisplayName("IcyBowFreezeEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        PlayerMock target = server.addPlayer();

        IcyBowFreezeEvent event = new IcyBowFreezeEvent(icyBow, target, 60, new ArrayList<>());

        Assertions.assertEquals(icyBow, event.getBow());
        Assertions.assertEquals(target, event.getTarget());
        Assertions.assertEquals(60, event.getFreezeTicks());
        Assertions.assertTrue(event.getEffects().isEmpty());
        Assertions.assertFalse(event.isCancelled());

        event.setFreezeTicks(100);
        Assertions.assertEquals(100, event.getFreezeTicks());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new IcyBowFreezeEvent(null, target, 60, new ArrayList<>()));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new IcyBowFreezeEvent(icyBow, null, 60, new ArrayList<>()));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new IcyBowFreezeEvent(icyBow, target, -1, new ArrayList<>()));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new IcyBowFreezeEvent(icyBow, target, 60, null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setFreezeTicks(-1));
    }

    @Test
    @DisplayName("A hit fires the event and freezes the target player")
    void testHitFiresAndFreezes() {
        PlayerMock target = server.addPlayer();

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onFreeze(IcyBowFreezeEvent event) {
                seen[0] = true;
                Assertions.assertEquals(icyBow, event.getBow());
                Assertions.assertEquals(target, event.getTarget());
                Assertions.assertEquals(60, event.getFreezeTicks());
                Assertions.assertEquals(2, event.getEffects().size(), "Slowness and negative jump boost must be included");
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            boolean tailReached = hit(target);

            Assertions.assertTrue(seen[0], "IcyBowFreezeEvent was not fired");
            Assertions.assertEquals(60, target.getFreezeTicks(), "The target must have been frozen");
            Assertions.assertTrue(tailReached, "The handler must have reached the ice particle tail");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling IcyBowFreezeEvent neither freezes nor slows the target")
    void testEventCancellationSkipsEffects() {
        PlayerMock target = server.addPlayer();

        Listener cancelling = new Listener() {
            @EventHandler
            public void onFreeze(IcyBowFreezeEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            boolean tailReached = hit(target);

            Assertions.assertFalse(tailReached, "A cancelled hit must return before the ice particle tail");
            Assertions.assertEquals(0, target.getFreezeTicks(), "A cancelled hit must not freeze the target");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Adjusting the freeze ticks via setFreezeTicks changes the applied value")
    void testFreezeTicksAdjustment() {
        PlayerMock target = server.addPlayer();

        Listener adjusting = new Listener() {
            @EventHandler
            public void onFreeze(IcyBowFreezeEvent event) {
                event.setFreezeTicks(100);
            }
        };
        server.getPluginManager().registerEvents(adjusting, plugin);

        try {
            hit(target);

            Assertions.assertEquals(100, target.getFreezeTicks(), "The adjusted freeze ticks must have been applied");
        } finally {
            HandlerList.unregisterAll(adjusting);
        }
    }

    @Test
    @DisplayName("A hit without listeners still freezes the target, preserving the old behavior")
    void testHitWithoutListenersStillFreezes() {
        PlayerMock target = server.addPlayer();

        boolean tailReached = hit(target);

        Assertions.assertEquals(60, target.getFreezeTicks(), "The target must have been frozen");
        Assertions.assertTrue(tailReached, "The handler must have reached the ice particle tail");
    }
}
