package io.github.thebusybiscuit.slimefun4.implementation.tasks.player;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

import io.github.thebusybiscuit.slimefun4.api.events.JetBootsThrustEvent;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.electric.gadgets.JetBoots;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Behavioral coverage for {@link JetBootsThrustEvent} through the real {@link JetBootsTask}
 * thrust path, mirroring {@link TestJetpackThrustEvent}.
 *
 * @author Zurker
 */
class TestJetBootsThrustEvent {

    private static final float INITIAL_CHARGE = 10F;
    private static final float COST = 0.075F;

    private static ServerMock server;
    private static Slimefun plugin;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    @BeforeEach
    public void beforeEach() {
        server.getPluginManager().clearEvents();
    }

    private JetBoots equipJetBoots(Player player, String id) {
        SlimefunItemStack bootsItem = new SlimefunItemStack(id, Material.LEATHER_BOOTS, "&bTest JetBoots");
        JetBoots boots = new JetBoots(TestUtilities.getItemGroup(plugin, "jetboots_test"), bootsItem, new ItemStack[9], 0.9, 100);
        boots.register(plugin);

        ItemStack bootsStack = bootsItem.item();
        boots.setItemCharge(bootsStack, INITIAL_CHARGE);
        player.getInventory().setBoots(bootsStack);
        return boots;
    }

    @Test
    @DisplayName("JetBootsThrustEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Player player = server.addPlayer();
        JetBoots boots = equipJetBoots(player, "THRUST_BOOTS_FIELDS");

        JetBootsThrustEvent event = new JetBootsThrustEvent(player, boots, COST);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(boots, event.getJetBoots());
        Assertions.assertEquals(COST, event.getCost());
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new JetBootsThrustEvent(player, null, COST));
    }

    @Test
    @DisplayName("JetBoots thrust fires the event, consumes charge and applies velocity")
    void testThrustConsumesChargeAndFiresEvent() {
        Player player = server.addPlayer();
        JetBoots boots = equipJetBoots(player, "THRUST_BOOTS_FIRED");

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onThrust(JetBootsThrustEvent event) {
                if (event.getPlayer().equals(player)) {
                    seen[0] = true;
                    Assertions.assertEquals(boots, event.getJetBoots());
                    Assertions.assertEquals(COST, event.getCost());
                }
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            new JetBootsTask(player, boots).executeTask();

            Assertions.assertTrue(seen[0], "JetBootsThrustEvent was not fired");
            Assertions.assertEquals(INITIAL_CHARGE - COST, boots.getItemCharge(player.getInventory().getBoots()), 0.01);
            Assertions.assertTrue(player.getVelocity().length() > 0, "Thrust should apply velocity");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling JetBootsThrustEvent skips charge consumption and velocity")
    void testThrustCancellation() {
        Player player = server.addPlayer();
        JetBoots boots = equipJetBoots(player, "THRUST_BOOTS_CANCELLED");

        Listener cancelling = new Listener() {
            @EventHandler
            public void onThrust(JetBootsThrustEvent event) {
                if (event.getPlayer().equals(player)) {
                    event.setCancelled(true);
                }
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            new JetBootsTask(player, boots).executeTask();

            Assertions.assertEquals(INITIAL_CHARGE, boots.getItemCharge(player.getInventory().getBoots()), 0.001);
            Assertions.assertEquals(0, player.getVelocity().length(), "Cancelled thrust must not apply velocity");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Thrusting without listeners still applies, preserving the old behavior")
    void testThrustWithoutListenersStillThrusts() {
        Player player = server.addPlayer();
        JetBoots boots = equipJetBoots(player, "THRUST_BOOTS_NO_LISTENER");

        new JetBootsTask(player, boots).executeTask();

        Assertions.assertEquals(INITIAL_CHARGE - COST, boots.getItemCharge(player.getInventory().getBoots()), 0.01);
        Assertions.assertTrue(player.getVelocity().length() > 0, "Thrust should apply velocity");
    }
}
