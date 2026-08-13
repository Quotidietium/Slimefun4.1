package io.github.thebusybiscuit.slimefun4.implementation.items.armor;

import org.bukkit.Material;
import org.bukkit.entity.Cow;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
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

import io.github.thebusybiscuit.slimefun4.api.events.StomperBootsPushEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Regression coverage for the stomper boots API expansion: {@link StomperBootsPushEvent},
 * exercised by calling the real {@link StomperBoots#stomp(EntityDamageEvent)} with a
 * constructed fall damage event.
 * <p>
 * The stomp ends in a {@code playEffect(STEP_SOUND, Material)} loop that MockBukkit rejects
 * with an {@link IllegalArgumentException} after every entity was already handled, so that
 * tail is ignored here.
 *
 * @author Zurker
 */
class TestStomperBootsPushEvent {

    private static final double FALL_DAMAGE = 10.0;

    private static ServerMock server;
    private static Slimefun plugin;

    private static StomperBoots boots;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);

        // ProtectionManager is created when the integrations start
        Slimefun.getIntegrations().start();
        server.getScheduler().performOneTick();

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "stomper_boots_test");
        SlimefunItemStack stack = new SlimefunItemStack("_TEST_STOMPER_BOOTS", Material.LEATHER_BOOTS, "&7Test Stomper Boots");
        Slimefun.getItemCfg().setValue("_TEST_STOMPER_BOOTS.enabled", true);
        boots = new StomperBoots(itemGroup, stack, RecipeType.NULL, new ItemStack[9]);
        boots.register(plugin);
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
     * Stomps with the player via the real method.
     */
    private void stomp(Player player) {
        EntityDamageEvent fallDamageEvent = new EntityDamageEvent(player, DamageCause.FALL, FALL_DAMAGE);

        try {
            boots.stomp(fallDamageEvent);
        } catch (IllegalArgumentException ex) {
            if (ex.getMessage() == null || !ex.getMessage().contains("Wrong kind of data")) {
                throw ex;
            }
            // MockBukkit rejects playEffect(STEP_SOUND, Material) - see class javadoc
        }
    }

    private Cow spawnCow(Player player) {
        return (Cow) player.getWorld().spawnEntity(player.getLocation().clone().add(1, 0, 0), EntityType.COW);
    }

    @Test
    @DisplayName("StomperBootsPushEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Player player = server.addPlayer();
        Cow cow = (Cow) player.getWorld().spawnEntity(player.getLocation(), EntityType.COW);

        StomperBootsPushEvent event = new StomperBootsPushEvent(player, boots, cow, 5.0);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(boots, event.getBoots());
        Assertions.assertEquals(cow, event.getEntity());
        Assertions.assertEquals(5.0, event.getDamage());
        Assertions.assertNull(event.getPushVelocity(), "The computed shockwave must be represented by a null push velocity");
        Assertions.assertFalse(event.isCancelled());

        event.setDamage(2.5);
        Assertions.assertEquals(2.5, event.getDamage());

        // NaN or infinite damage would corrupt the entity's health state
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setDamage(Double.NaN));
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setDamage(Double.POSITIVE_INFINITY));
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setDamage(Double.NEGATIVE_INFINITY));

        // The push can be overridden and reset to the computed shockwave
        Vector custom = new Vector(0, 2, 0);
        event.setPushVelocity(custom);
        Assertions.assertEquals(custom, event.getPushVelocity());
        event.setPushVelocity(null);
        Assertions.assertNull(event.getPushVelocity(), "Setting the push velocity back to null must restore the computed shockwave");

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new StomperBootsPushEvent(player, null, cow, 5.0));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new StomperBootsPushEvent(player, boots, null, 5.0));
    }

    @Test
    @DisplayName("Stomping fires the event per entity and pushes and damages it")
    void testStompFiresAndPushes() {
        Player player = server.addPlayer();

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onPush(StomperBootsPushEvent event) {
                if (event.getEntity() instanceof Cow) {
                    seen[0] = true;
                    Assertions.assertEquals(boots, event.getBoots());
                    Assertions.assertEquals(FALL_DAMAGE / 2, event.getDamage());
                }
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            Cow cow = spawnCow(player);
            double healthBefore = cow.getHealth();

            stomp(player);

            Assertions.assertTrue(seen[0], "StomperBootsPushEvent was not fired");
            Assertions.assertTrue(cow.getVelocity().length() > 0, "The cow must have been pushed");
            Assertions.assertEquals(healthBefore - FALL_DAMAGE / 2, cow.getHealth(), 0.001, "The cow must have taken half the fall damage");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling StomperBootsPushEvent spares the entity")
    void testEventCancellationSparesEntity() {
        Player player = server.addPlayer();

        Listener cancelling = new Listener() {
            @EventHandler
            public void onPush(StomperBootsPushEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            Cow cow = spawnCow(player);
            double healthBefore = cow.getHealth();

            stomp(player);

            Assertions.assertEquals(0, cow.getVelocity().length(), "A spared cow must not be pushed");
            Assertions.assertEquals(healthBefore, cow.getHealth(), "A spared cow must not be damaged");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Overriding the damage via setDamage changes the dealt damage")
    void testDamageOverride() {
        Player player = server.addPlayer();

        Listener overriding = new Listener() {
            @EventHandler
            public void onPush(StomperBootsPushEvent event) {
                event.setDamage(1.0);
            }
        };
        server.getPluginManager().registerEvents(overriding, plugin);

        try {
            Cow cow = spawnCow(player);
            double healthBefore = cow.getHealth();

            stomp(player);

            Assertions.assertEquals(healthBefore - 1.0, cow.getHealth(), 0.001, "The cow must have taken the overridden damage");
        } finally {
            HandlerList.unregisterAll(overriding);
        }
    }

    @Test
    @DisplayName("Overriding the push velocity via setPushVelocity redirects the push")
    void testPushVelocityOverride() {
        Player player = server.addPlayer();

        Vector custom = new Vector(0, 2, 0);
        Listener overriding = new Listener() {
            @EventHandler
            public void onPush(StomperBootsPushEvent event) {
                Assertions.assertNull(event.getPushVelocity(), "The computed shockwave must be the default");
                event.setPushVelocity(custom);
            }
        };
        server.getPluginManager().registerEvents(overriding, plugin);

        try {
            Cow cow = spawnCow(player);

            stomp(player);

            Assertions.assertEquals(custom, cow.getVelocity(), "The cow must have been pushed with the overridden velocity");
        } finally {
            HandlerList.unregisterAll(overriding);
        }
    }

    @Test
    @DisplayName("Stomping without listeners still pushes and damages, preserving the old behavior")
    void testStompWithoutListenersStillPushes() {
        Player player = server.addPlayer();

        Cow cow = spawnCow(player);
        double healthBefore = cow.getHealth();

        stomp(player);

        // The cow spawned one block away on the x axis, so the computed shockwave is
        // exactly (1.4, 0, 0) - the old hardcoded behavior.
        Assertions.assertEquals(new Vector(1.4, 0, 0), cow.getVelocity(), "The cow must have been pushed with the computed shockwave");
        Assertions.assertEquals(healthBefore - FALL_DAMAGE / 2, cow.getHealth(), 0.001, "The cow must have taken half the fall damage");
    }
}
