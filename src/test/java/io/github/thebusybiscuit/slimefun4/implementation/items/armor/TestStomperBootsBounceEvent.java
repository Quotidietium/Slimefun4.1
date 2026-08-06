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

import io.github.thebusybiscuit.slimefun4.api.events.StomperBootsBounceEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Regression coverage for the stomper boots bounce API expansion:
 * {@link StomperBootsBounceEvent}, exercised by calling the real
 * {@link StomperBoots#stomp(EntityDamageEvent)} with a constructed fall damage event.
 * <p>
 * The stomp ends in a {@code playEffect(STEP_SOUND, Material)} loop that MockBukkit rejects with
 * an {@link IllegalArgumentException} after the bounce and shockwave already happened, so that
 * tail is ignored here.
 *
 * @author Zurker
 */
class TestStomperBootsBounceEvent {

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

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "stomper_boots_bounce_test");
        SlimefunItemStack stack = new SlimefunItemStack("_TEST_STOMPER_BOOTS_BOUNCE", Material.LEATHER_BOOTS, "&7Test Stomper Boots");
        Slimefun.getItemCfg().setValue("_TEST_STOMPER_BOOTS_BOUNCE.enabled", true);
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
     * Stomps with the player via the real method, swallowing the playEffect tail exception.
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
    @DisplayName("StomperBootsBounceEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Player player = server.addPlayer();

        StomperBootsBounceEvent event = new StomperBootsBounceEvent(player, boots, new Vector(0, 0.7, 0));

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(boots, event.getBoots());
        Assertions.assertEquals(new Vector(0, 0.7, 0), event.getBounceVelocity());
        Assertions.assertFalse(event.isCancelled());

        event.setBounceVelocity(new Vector(0, 2, 0));
        Assertions.assertEquals(new Vector(0, 2, 0), event.getBounceVelocity());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new StomperBootsBounceEvent(player, null, new Vector(0, 0.7, 0)));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new StomperBootsBounceEvent(player, boots, null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setBounceVelocity(null));
    }

    @Test
    @DisplayName("Stomping fires the bounce event and launches the player")
    void testStompFiresAndLaunches() {
        Player player = server.addPlayer();

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onBounce(StomperBootsBounceEvent event) {
                seen[0] = true;
                Assertions.assertEquals(boots, event.getBoots());
                Assertions.assertEquals(0.7, event.getBounceVelocity().getY(), 0.0001);
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            stomp(player);

            Assertions.assertTrue(seen[0], "StomperBootsBounceEvent was not fired");
            Assertions.assertEquals(0.7, player.getVelocity().getY(), 0.0001, "The player must have been launched back up");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling StomperBootsBounceEvent skips the launch but keeps the shockwave")
    void testCancelBounceSkipsLaunchButKeepsShockwave() {
        Player player = server.addPlayer();
        Cow cow = spawnCow(player);
        double cowHealthBefore = cow.getHealth();

        Listener cancelling = new Listener() {
            @EventHandler
            public void onBounce(StomperBootsBounceEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            stomp(player);

            Assertions.assertEquals(0.0, player.getVelocity().getY(), 0.0001, "A cancelled bounce must not launch the player");
            Assertions.assertTrue(cow.getVelocity().length() > 0, "The shockwave must still push nearby entities");
            Assertions.assertTrue(cow.getHealth() < cowHealthBefore, "The shockwave must still damage nearby entities");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Overriding the bounce velocity changes the launch strength")
    void testBounceVelocityOverride() {
        Player player = server.addPlayer();

        Listener overriding = new Listener() {
            @EventHandler
            public void onBounce(StomperBootsBounceEvent event) {
                event.setBounceVelocity(new Vector(0, 2.0, 0));
            }
        };
        server.getPluginManager().registerEvents(overriding, plugin);

        try {
            stomp(player);

            Assertions.assertEquals(2.0, player.getVelocity().getY(), 0.0001, "The player must have been launched with the overridden velocity");
        } finally {
            HandlerList.unregisterAll(overriding);
        }
    }

    @Test
    @DisplayName("Stomping without listeners still launches, preserving the old behavior")
    void testStompWithoutListenersStillBounces() {
        Player player = server.addPlayer();

        stomp(player);

        Assertions.assertEquals(0.7, player.getVelocity().getY(), 0.0001, "The player must have been launched back up");
    }
}
