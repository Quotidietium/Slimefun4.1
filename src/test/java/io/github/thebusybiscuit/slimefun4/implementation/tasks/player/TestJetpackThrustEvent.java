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

import io.github.thebusybiscuit.slimefun4.api.events.JetpackThrustEvent;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.electric.gadgets.Jetpack;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Behavioral coverage for {@link JetpackThrustEvent} through the real
 * {@link JetpackTask} thrust path.
 *
 * @author Zurker
 */
class TestJetpackThrustEvent {

    private static final float INITIAL_CHARGE = 10F;
    private static final float COST = 0.08F;

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

    private Jetpack equipJetpack(Player player, String id) {
        SlimefunItemStack jetpackItem = new SlimefunItemStack(id, Material.LEATHER_CHESTPLATE, "&bTest Jetpack");
        Jetpack jetpack = new Jetpack(TestUtilities.getItemGroup(plugin, "jetpack_test"), jetpackItem, new ItemStack[9], 0.9, 100);
        jetpack.register(plugin);

        ItemStack chestplate = jetpackItem.item();
        jetpack.setItemCharge(chestplate, INITIAL_CHARGE);
        player.getInventory().setChestplate(chestplate);
        return jetpack;
    }

    @Test
    @DisplayName("Jetpack thrust fires the event, consumes charge and applies velocity")
    void testThrustConsumesChargeAndFiresEvent() {
        Player player = server.addPlayer();
        Jetpack jetpack = equipJetpack(player, "THRUST_JETPACK_FIRED");

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onThrust(JetpackThrustEvent event) {
                if (event.getPlayer().equals(player)) {
                    seen[0] = true;
                    Assertions.assertEquals(jetpack, event.getJetpack());
                    Assertions.assertEquals(COST, event.getCost());
                }
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            new JetpackTask(player, jetpack).executeTask();

            Assertions.assertTrue(seen[0], "JetpackThrustEvent was not fired");
            Assertions.assertEquals(INITIAL_CHARGE - COST, jetpack.getItemCharge(player.getInventory().getChestplate()), 0.001);
            Assertions.assertTrue(player.getVelocity().length() > 0, "Thrust should apply velocity");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling JetpackThrustEvent skips charge consumption and velocity")
    void testThrustCancellation() {
        Player player = server.addPlayer();
        Jetpack jetpack = equipJetpack(player, "THRUST_JETPACK_CANCELLED");

        Listener cancelling = new Listener() {
            @EventHandler
            public void onThrust(JetpackThrustEvent event) {
                if (event.getPlayer().equals(player)) {
                    event.setCancelled(true);
                }
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            new JetpackTask(player, jetpack).executeTask();

            Assertions.assertEquals(INITIAL_CHARGE, jetpack.getItemCharge(player.getInventory().getChestplate()), 0.001);
            Assertions.assertEquals(0, player.getVelocity().length(), "Cancelled thrust must not apply velocity");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }
}
