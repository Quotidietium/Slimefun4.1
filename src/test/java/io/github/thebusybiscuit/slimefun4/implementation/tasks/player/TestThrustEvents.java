package io.github.thebusybiscuit.slimefun4.implementation.tasks.player;

import org.bukkit.Material;
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
import be.seeseemelk.mockbukkit.entity.PlayerMock;

import io.github.thebusybiscuit.slimefun4.api.events.JetBootsThrustEvent;
import io.github.thebusybiscuit.slimefun4.api.events.JetpackThrustEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.electric.gadgets.JetBoots;
import io.github.thebusybiscuit.slimefun4.implementation.items.electric.gadgets.Jetpack;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Regression coverage for the jetpack and jet boots API expansion: {@link JetpackThrustEvent}
 * and {@link JetBootsThrustEvent}, exercised by driving the real {@link JetpackTask} and
 * {@link JetBootsTask} for one thrust.
 * <p>
 * The tasks only run for a sneaking, online, living {@link org.bukkit.entity.Player}, so the
 * mock player is set sneaking before every thrust. MockBukkit tolerates the task's defensive
 * {@code cancelTask} call, which never matches a scheduled task here.
 *
 * @author Zurker
 */
class TestThrustEvents {

    private static final float JETPACK_COST = 0.08F;
    private static final float JETBOOTS_COST = 0.075F;
    private static final float DELTA = 0.0001F;

    private static ServerMock server;
    private static Slimefun plugin;

    private static Jetpack jetpack;
    private static JetBoots jetBoots;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "thrust_test");

        SlimefunItemStack jetpackStack = new SlimefunItemStack("_TEST_THRUST_JETPACK", Material.IRON_CHESTPLATE, "&bTest Jetpack");
        Slimefun.getItemCfg().setValue("_TEST_THRUST_JETPACK.enabled", true);
        jetpack = new Jetpack(itemGroup, jetpackStack, new ItemStack[9], 0.5, 100F);
        jetpack.register(plugin);

        SlimefunItemStack bootsStack = new SlimefunItemStack("_TEST_THRUST_JETBOOTS", Material.IRON_BOOTS, "&bTest Jet Boots");
        Slimefun.getItemCfg().setValue("_TEST_THRUST_JETBOOTS.enabled", true);
        jetBoots = new JetBoots(itemGroup, bootsStack, new ItemStack[9], 1.2, 100F);
        jetBoots.register(plugin);
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
     * Equips a freshly cloned jetpack with the given charge and returns the exact stack stored
     * in the inventory (MockBukkit clones on set, so the stored reference is the one the task
     * will actually drain).
     */
    private ItemStack equipChargedJetpack(PlayerMock player, float charge) {
        player.getInventory().setChestplate(jetpack.getItem().clone());
        ItemStack equipped = player.getInventory().getChestplate();
        jetpack.addItemCharge(equipped, charge);
        return equipped;
    }

    /**
     * Same as {@link #equipChargedJetpack(PlayerMock, float)} but for the boots slot.
     */
    private ItemStack equipChargedJetBoots(PlayerMock player, float charge) {
        player.getInventory().setBoots(jetBoots.getItem().clone());
        ItemStack equipped = player.getInventory().getBoots();
        jetBoots.addItemCharge(equipped, charge);
        return equipped;
    }

    private void thrustJetpack(PlayerMock player) {
        player.setSneaking(true);
        new JetpackTask(player, jetpack).run();
    }

    private void thrustJetBoots(PlayerMock player) {
        player.setSneaking(true);
        new JetBootsTask(player, jetBoots).run();
    }

    // ---------- JetpackThrustEvent ----------

    @Test
    @DisplayName("JetpackThrustEvent exposes its fields and validates its cost")
    void testJetpackEventFieldsAndValidation() {
        PlayerMock player = server.addPlayer();

        JetpackThrustEvent event = new JetpackThrustEvent(player, jetpack, JETPACK_COST);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(jetpack, event.getJetpack());
        Assertions.assertEquals(JETPACK_COST, event.getCost(), DELTA);
        Assertions.assertFalse(event.isCancelled());

        event.setCost(0.5F);
        Assertions.assertEquals(0.5F, event.getCost(), DELTA);

        // Zero and negative costs are rejected (Rechargeable#removeItemCharge requires > 0)
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setCost(0));
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setCost(-1));

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new JetpackThrustEvent(player, null, JETPACK_COST));
    }

    @Test
    @DisplayName("A jetpack thrust fires the event and consumes the default cost")
    void testJetpackThrustFiresAndConsumesDefaultCost() {
        PlayerMock player = server.addPlayer();
        ItemStack equipped = equipChargedJetpack(player, 10F);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onThrust(JetpackThrustEvent event) {
                seen[0] = true;
                Assertions.assertEquals(player, event.getPlayer());
                Assertions.assertEquals(jetpack, event.getJetpack());
                Assertions.assertEquals(JETPACK_COST, event.getCost(), DELTA, "The cost must default to the jetpack thrust cost");
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            thrustJetpack(player);

            Assertions.assertTrue(seen[0], "JetpackThrustEvent was not fired");
            Assertions.assertEquals(10F - JETPACK_COST, jetpack.getItemCharge(equipped), DELTA, "The default cost must have been consumed");
            Assertions.assertTrue(player.getVelocity().getY() > 0.4, "The thrust must have applied an upward velocity");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Overriding the cost via setCost changes the consumed charge")
    void testJetpackSetCostAdjustsConsumption() {
        PlayerMock player = server.addPlayer();
        ItemStack equipped = equipChargedJetpack(player, 10F);

        Listener adjusting = new Listener() {
            @EventHandler
            public void onThrust(JetpackThrustEvent event) {
                event.setCost(0.5F);
            }
        };
        server.getPluginManager().registerEvents(adjusting, plugin);

        try {
            thrustJetpack(player);

            Assertions.assertEquals(9.5F, jetpack.getItemCharge(equipped), DELTA, "The overridden cost must have been consumed");
            Assertions.assertTrue(player.getVelocity().getY() > 0.4, "The thrust must still have been applied");
        } finally {
            HandlerList.unregisterAll(adjusting);
        }
    }

    @Test
    @DisplayName("Cancelling JetpackThrustEvent consumes no charge and applies no velocity")
    void testJetpackCancelSkipsThrust() {
        PlayerMock player = server.addPlayer();
        ItemStack equipped = equipChargedJetpack(player, 10F);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onThrust(JetpackThrustEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            thrustJetpack(player);

            Assertions.assertEquals(10F, jetpack.getItemCharge(equipped), DELTA, "A cancelled thrust must not consume any charge");
            Assertions.assertEquals(0, player.getVelocity().lengthSquared(), "A cancelled thrust must not apply any velocity");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("A jetpack thrust without listeners still thrusts, preserving the old behavior")
    void testJetpackWithoutListenersStillThrusts() {
        PlayerMock player = server.addPlayer();
        ItemStack equipped = equipChargedJetpack(player, 10F);

        thrustJetpack(player);

        Assertions.assertEquals(10F - JETPACK_COST, jetpack.getItemCharge(equipped), DELTA, "The default cost must have been consumed");
        Assertions.assertTrue(player.getVelocity().getY() > 0.4, "The thrust must have applied an upward velocity");
    }

    // ---------- JetBootsThrustEvent ----------

    @Test
    @DisplayName("JetBootsThrustEvent exposes its fields and validates its cost")
    void testJetBootsEventFieldsAndValidation() {
        PlayerMock player = server.addPlayer();

        JetBootsThrustEvent event = new JetBootsThrustEvent(player, jetBoots, JETBOOTS_COST);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(jetBoots, event.getJetBoots());
        Assertions.assertEquals(JETBOOTS_COST, event.getCost(), DELTA);
        Assertions.assertFalse(event.isCancelled());

        event.setCost(0.3F);
        Assertions.assertEquals(0.3F, event.getCost(), DELTA);

        // Zero and negative costs are rejected (Rechargeable#removeItemCharge requires > 0)
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setCost(0));
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setCost(-1));

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new JetBootsThrustEvent(player, null, JETBOOTS_COST));
    }

    @Test
    @DisplayName("A jet boots thrust fires the event and consumes the default cost")
    void testJetBootsThrustFiresAndConsumesDefaultCost() {
        PlayerMock player = server.addPlayer();
        ItemStack equipped = equipChargedJetBoots(player, 10F);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onThrust(JetBootsThrustEvent event) {
                seen[0] = true;
                Assertions.assertEquals(player, event.getPlayer());
                Assertions.assertEquals(jetBoots, event.getJetBoots());
                Assertions.assertEquals(JETBOOTS_COST, event.getCost(), DELTA, "The cost must default to the jet boots thrust cost");
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            thrustJetBoots(player);

            Assertions.assertTrue(seen[0], "JetBootsThrustEvent was not fired");
            // ChargeUtils rounds stored charge to 2 decimals, so 10 - 0.075 = 9.925 lands as 9.93
            Assertions.assertEquals(9.93F, jetBoots.getItemCharge(equipped), DELTA, "The default cost must have been consumed");
            Assertions.assertTrue(player.getVelocity().getY() > 0.03, "The thrust must have applied an upward velocity");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Overriding the cost via setCost changes the consumed charge")
    void testJetBootsSetCostAdjustsConsumption() {
        PlayerMock player = server.addPlayer();
        ItemStack equipped = equipChargedJetBoots(player, 10F);

        Listener adjusting = new Listener() {
            @EventHandler
            public void onThrust(JetBootsThrustEvent event) {
                event.setCost(0.3F);
            }
        };
        server.getPluginManager().registerEvents(adjusting, plugin);

        try {
            thrustJetBoots(player);

            Assertions.assertEquals(9.7F, jetBoots.getItemCharge(equipped), DELTA, "The overridden cost must have been consumed");
            Assertions.assertTrue(player.getVelocity().getY() > 0.03, "The thrust must still have been applied");
        } finally {
            HandlerList.unregisterAll(adjusting);
        }
    }

    @Test
    @DisplayName("Cancelling JetBootsThrustEvent consumes no charge and applies no velocity")
    void testJetBootsCancelSkipsThrust() {
        PlayerMock player = server.addPlayer();
        ItemStack equipped = equipChargedJetBoots(player, 10F);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onThrust(JetBootsThrustEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            thrustJetBoots(player);

            Assertions.assertEquals(10F, jetBoots.getItemCharge(equipped), DELTA, "A cancelled thrust must not consume any charge");
            Assertions.assertEquals(0, player.getVelocity().lengthSquared(), "A cancelled thrust must not apply any velocity");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("A jet boots thrust without listeners still thrusts, preserving the old behavior")
    void testJetBootsWithoutListenersStillThrusts() {
        PlayerMock player = server.addPlayer();
        ItemStack equipped = equipChargedJetBoots(player, 10F);

        thrustJetBoots(player);

        // ChargeUtils rounds stored charge to 2 decimals, so 10 - 0.075 = 9.925 lands as 9.93
        Assertions.assertEquals(9.93F, jetBoots.getItemCharge(equipped), DELTA, "The default cost must have been consumed");
        Assertions.assertTrue(player.getVelocity().getY() > 0.03, "The thrust must have applied an upward velocity");
    }
}
