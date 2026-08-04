package io.github.thebusybiscuit.slimefun4.implementation.listeners;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
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

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

import io.github.thebusybiscuit.slimefun4.api.events.SlimefunBootsFallEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.armor.LongFallBoots;
import io.github.thebusybiscuit.slimefun4.implementation.items.armor.StomperBoots;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Regression coverage for the boots API expansion: {@link SlimefunBootsFallEvent},
 * exercised through the real {@link SlimefunBootsListener} fall-protection path.
 *
 * @author Zurker
 */
class TestSlimefunBootsFallEvent {

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static StomperBoots stomperBoots;
    private static LongFallBoots longFallBoots;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        // Unit test startups never start the integrations, the ProtectionManager is created there
        Slimefun.getIntegrations().start();
        server.getScheduler().performOneTick();

        // Unit test startups do not register the listeners, register it manually
        new SlimefunBootsListener(plugin);

        // Non-configurable items stay DISABLED unless Items.yml says otherwise, enable them first
        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "boots_fall_test");
        Slimefun.getItemCfg().setValue("TEST_STOMPER_BOOTS.enabled", true);
        Slimefun.getItemCfg().setValue("TEST_LONG_FALL_BOOTS.enabled", true);

        stomperBoots = new StomperBoots(itemGroup, new SlimefunItemStack("TEST_STOMPER_BOOTS", Material.LEATHER_BOOTS, "&7Test Stomper Boots"), RecipeType.NULL, new ItemStack[9]);
        stomperBoots.register(plugin);

        longFallBoots = new LongFallBoots(itemGroup, new SlimefunItemStack("TEST_LONG_FALL_BOOTS", Material.CHAINMAIL_BOOTS, "&7Test Long Fall Boots"), RecipeType.NULL, new ItemStack[9], null);
        longFallBoots.register(plugin);
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
     * Creates a player wearing the given boots, lifted off the ground floor: the stomp
     * effect inspects the block below, and MockBukkit worlds start at y=0.
     */
    private Player setupPlayer(ItemStack boots) {
        Player player = server.addPlayer();
        player.teleport(new Location(world, 5, 5, 5));
        player.getInventory().setBoots(boots);
        return player;
    }

    @Test
    @DisplayName("SlimefunBootsFallEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Player player = server.addPlayer();
        EntityDamageEvent damageEvent = new EntityDamageEvent(player, DamageCause.FALL, 6.0);

        SlimefunBootsFallEvent event = new SlimefunBootsFallEvent(player, stomperBoots, damageEvent);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(stomperBoots, event.getBoots());
        Assertions.assertEquals(damageEvent, event.getDamageEvent());
        Assertions.assertEquals(6.0, event.getFallDamage());
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new SlimefunBootsFallEvent(player, null, damageEvent));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new SlimefunBootsFallEvent(player, stomperBoots, null));
    }

    @Test
    @DisplayName("Stomper boots fire the event and cancel the fall damage")
    void testStomperBootsFireAndProtect() {
        Player player = setupPlayer(stomperBoots.getItem().clone());

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onFall(SlimefunBootsFallEvent event) {
                if (!event.getPlayer().equals(player)) {
                    return;
                }

                seen[0] = true;
                Assertions.assertEquals(stomperBoots, event.getBoots());
                Assertions.assertEquals(DamageCause.FALL, event.getDamageEvent().getCause());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            EntityDamageEvent damageEvent = new EntityDamageEvent(player, DamageCause.FALL, 6.0);
            try {
                server.getPluginManager().callEvent(damageEvent);
            } catch (IllegalArgumentException ex) {
                // MockBukkit rejects the Material data of Effect.STEP_SOUND ("Wrong kind of
                // data for this effect!") which a real server accepts. The event was fired
                // and the damage cancelled before the stomp effect ran, so this is safe.
                Assertions.assertEquals("Wrong kind of data for this effect!", ex.getMessage());
            }

            Assertions.assertTrue(seen[0], "SlimefunBootsFallEvent was not fired");
            Assertions.assertTrue(damageEvent.isCancelled(), "The boots must have cancelled the fall damage");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Long fall boots fire the event and cancel the fall damage")
    void testLongFallBootsFireAndProtect() {
        Player player = setupPlayer(longFallBoots.getItem().clone());

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onFall(SlimefunBootsFallEvent event) {
                if (!event.getPlayer().equals(player)) {
                    return;
                }

                seen[0] = true;
                Assertions.assertEquals(longFallBoots, event.getBoots());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            EntityDamageEvent damageEvent = new EntityDamageEvent(player, DamageCause.FALL, 6.0);
            server.getPluginManager().callEvent(damageEvent);

            Assertions.assertTrue(seen[0], "SlimefunBootsFallEvent was not fired");
            Assertions.assertTrue(damageEvent.isCancelled(), "The boots must have cancelled the fall damage");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling SlimefunBootsFallEvent keeps the vanilla fall damage")
    void testEventCancellationKeepsDamage() {
        Player player = setupPlayer(stomperBoots.getItem().clone());

        Listener cancelling = new Listener() {
            @EventHandler
            public void onFall(SlimefunBootsFallEvent event) {
                if (event.getPlayer().equals(player)) {
                    event.setCancelled(true);
                }
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            EntityDamageEvent damageEvent = new EntityDamageEvent(player, DamageCause.FALL, 6.0);
            server.getPluginManager().callEvent(damageEvent);

            Assertions.assertFalse(damageEvent.isCancelled(), "A cancelled SlimefunBootsFallEvent must keep the vanilla fall damage");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Fall damage without listeners is still cancelled, preserving the old behavior")
    void testFallWithoutListenersStillProtected() {
        Player player = setupPlayer(longFallBoots.getItem().clone());

        EntityDamageEvent damageEvent = new EntityDamageEvent(player, DamageCause.FALL, 6.0);
        server.getPluginManager().callEvent(damageEvent);

        Assertions.assertTrue(damageEvent.isCancelled(), "The boots must have cancelled the fall damage");
    }

    @Test
    @DisplayName("Vanilla boots fire no event and take the damage")
    void testVanillaBootsFireNothing() {
        Player player = setupPlayer(new ItemStack(Material.IRON_BOOTS));

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onFall(SlimefunBootsFallEvent event) {
                if (event.getPlayer().equals(player)) {
                    seen[0] = true;
                }
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            EntityDamageEvent damageEvent = new EntityDamageEvent(player, DamageCause.FALL, 6.0);
            server.getPluginManager().callEvent(damageEvent);

            Assertions.assertFalse(seen[0], "No event must be fired for vanilla boots");
            Assertions.assertFalse(damageEvent.isCancelled(), "Vanilla boots grant no protection");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Non-fall damage fires no event")
    void testNonFallDamageFiresNothing() {
        Player player = setupPlayer(stomperBoots.getItem().clone());

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onFall(SlimefunBootsFallEvent event) {
                if (event.getPlayer().equals(player)) {
                    seen[0] = true;
                }
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            EntityDamageEvent damageEvent = new EntityDamageEvent(player, DamageCause.ENTITY_ATTACK, 6.0);
            server.getPluginManager().callEvent(damageEvent);

            Assertions.assertFalse(seen[0], "No event must be fired for non-fall damage");
            Assertions.assertFalse(damageEvent.isCancelled(), "Non-fall damage must stay untouched");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }
}
