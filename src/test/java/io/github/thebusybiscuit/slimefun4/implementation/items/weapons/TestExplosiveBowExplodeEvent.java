package io.github.thebusybiscuit.slimefun4.implementation.items.weapons;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Cow;
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
import be.seeseemelk.mockbukkit.UnimplementedOperationException;

import io.github.thebusybiscuit.slimefun4.api.events.ExplosiveBowExplodeEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Regression coverage for the explosive bow API expansion: {@link ExplosiveBowExplodeEvent},
 * exercised by driving the real {@link ExplosiveBow}
 * {@link io.github.thebusybiscuit.slimefun4.core.handlers.BowShootHandler} with a constructed
 * {@link EntityDamageByEntityEvent}.
 * <p>
 * MockBukkit's {@code WorldMock.spawnParticle} is unimplemented and throws, so the helper
 * reports whether the explosion tail was reached instead of asserting knockback. The target
 * cow is spawned far from the default spawn point so lingering players from other tests are
 * not picked up as nearby entities.
 *
 * @author Zurker
 */
class TestExplosiveBowExplodeEvent {

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static ExplosiveBow explosiveBow;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "explosive_bow_test");
        SlimefunItemStack stack = new SlimefunItemStack("_TEST_EXPLOSIVE_BOW", Material.BOW, "&cTest Explosive Bow");
        Slimefun.getItemCfg().setValue("_TEST_EXPLOSIVE_BOW.enabled", true);
        explosiveBow = new ExplosiveBow(itemGroup, stack, new ItemStack[9]);
        explosiveBow.register(plugin);
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
     * Hits the target cow with an explosive arrow via the real handler.
     *
     * @return true if the handler reached the particle tail, false if it returned earlier
     */
    private boolean hit(Cow target) {
        Player shooter = server.addPlayer();
        EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(shooter, target, DamageCause.ENTITY_ATTACK, 6.0);

        try {
            explosiveBow.onShoot().onHit(event, target);
            return false;
        } catch (UnimplementedOperationException expected) {
            // WorldMock.spawnParticle is unimplemented - see class javadoc
            return true;
        }
    }

    @Test
    @DisplayName("ExplosiveBowExplodeEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Cow target = world.spawn(world.getSpawnLocation(), Cow.class);

        ExplosiveBowExplodeEvent event = new ExplosiveBowExplodeEvent(explosiveBow, target, new java.util.ArrayList<>());

        Assertions.assertEquals(explosiveBow, event.getBow());
        Assertions.assertEquals(target, event.getTarget());
        Assertions.assertTrue(event.getAffectedEntities().isEmpty());
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new ExplosiveBowExplodeEvent(null, target, new java.util.ArrayList<>()));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ExplosiveBowExplodeEvent(explosiveBow, null, new java.util.ArrayList<>()));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ExplosiveBowExplodeEvent(explosiveBow, target, null));

        target.remove();
    }

    @Test
    @DisplayName("A hit fires the event with the nearby entities and runs the explosion")
    void testHitFiresEvent() {
        Cow target = world.spawn(new org.bukkit.Location(world, 100, 5, 100), Cow.class);
        Cow bystander = world.spawn(new org.bukkit.Location(world, 101, 5, 100), Cow.class);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onExplode(ExplosiveBowExplodeEvent event) {
                seen[0] = true;
                Assertions.assertEquals(explosiveBow, event.getBow());
                Assertions.assertEquals(target, event.getTarget());
                Assertions.assertTrue(event.getAffectedEntities().contains(bystander), "The bystander must be affected");
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            boolean tailReached = hit(target);

            Assertions.assertTrue(seen[0], "ExplosiveBowExplodeEvent was not fired");
            Assertions.assertTrue(tailReached, "The handler must have reached the particle tail");
        } finally {
            HandlerList.unregisterAll(watcher);
            target.remove();
            bystander.remove();
        }
    }

    @Test
    @DisplayName("Cancelling ExplosiveBowExplodeEvent skips the whole explosion")
    void testEventCancellationSkipsExplosion() {
        Cow target = world.spawn(new org.bukkit.Location(world, 110, 5, 110), Cow.class);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onExplode(ExplosiveBowExplodeEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            boolean tailReached = hit(target);

            Assertions.assertFalse(tailReached, "A cancelled explosion must return before the particle tail");
        } finally {
            HandlerList.unregisterAll(cancelling);
            target.remove();
        }
    }

    @Test
    @DisplayName("A hit without listeners still runs the explosion, preserving the old behavior")
    void testHitWithoutListenersStillExplodes() {
        Cow target = world.spawn(new org.bukkit.Location(world, 120, 5, 120), Cow.class);

        try {
            boolean tailReached = hit(target);

            Assertions.assertTrue(tailReached, "The handler must have reached the particle tail");
        } finally {
            target.remove();
        }
    }
}
