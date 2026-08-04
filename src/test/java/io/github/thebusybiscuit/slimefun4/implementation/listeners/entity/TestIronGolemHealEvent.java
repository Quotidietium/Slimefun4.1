package io.github.thebusybiscuit.slimefun4.implementation.listeners.entity;

import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.IronGolemMock;

import io.github.thebusybiscuit.slimefun4.api.events.IronGolemHealEvent;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Regression coverage for the iron golem heal protection API expansion:
 * {@link IronGolemHealEvent}, exercised through the real {@link IronGolemListener}
 * heal-blocking path.
 *
 * @author Zurker
 */
class TestIronGolemHealEvent {

    private static ServerMock server;
    private static Slimefun plugin;

    private static SlimefunItem fakeIron;
    private static SlimefunItem otherItem;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);

        // Unit test startups do not register the listeners, register it manually
        new IronGolemListener(plugin);

        // Non-configurable items stay DISABLED unless Items.yml says otherwise, enable them first
        Slimefun.getItemCfg().setValue("TEST_FAKE_IRON.enabled", true);
        fakeIron = TestUtilities.mockSlimefunItem(plugin, "TEST_FAKE_IRON", new ItemStack(Material.IRON_INGOT));
        fakeIron.register(plugin);

        Slimefun.getItemCfg().setValue("TEST_OTHER_ITEM.enabled", true);
        otherItem = TestUtilities.mockSlimefunItem(plugin, "TEST_OTHER_ITEM", new ItemStack(Material.GOLD_INGOT));
        otherItem.register(plugin);
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
     * Clicks the golem with the player's current main hand through the real event
     * pipeline and returns the interact event for assertions.
     */
    private PlayerInteractEntityEvent clickGolem(Player player, IronGolemMock golem, EquipmentSlot hand) {
        PlayerInteractEntityEvent event = new PlayerInteractEntityEvent(player, golem, hand);
        server.getPluginManager().callEvent(event);
        return event;
    }

    @Test
    @DisplayName("IronGolemHealEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Player player = server.addPlayer();
        IronGolemMock golem = new IronGolemMock(server, UUID.randomUUID());
        PlayerInteractEntityEvent interactEvent = new PlayerInteractEntityEvent(player, golem, EquipmentSlot.HAND);

        IronGolemHealEvent event = new IronGolemHealEvent(player, fakeIron, golem, interactEvent);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(fakeIron, event.getSlimefunItem());
        Assertions.assertEquals(golem, event.getIronGolem());
        Assertions.assertEquals(interactEvent, event.getInteractEvent());
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new IronGolemHealEvent(player, null, golem, interactEvent));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new IronGolemHealEvent(player, fakeIron, null, interactEvent));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new IronGolemHealEvent(player, fakeIron, golem, null));
    }

    @Test
    @DisplayName("Healing with a Slimefun iron ingot fires the event and blocks the heal")
    void testHealFiresAndBlocks() {
        Player player = server.addPlayer();
        IronGolemMock golem = new IronGolemMock(server, UUID.randomUUID());
        player.getInventory().setItemInMainHand(fakeIron.getItem().clone());

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onHeal(IronGolemHealEvent event) {
                seen[0] = true;
                Assertions.assertEquals(player, event.getPlayer());
                Assertions.assertEquals(fakeIron, event.getSlimefunItem());
                Assertions.assertEquals(golem, event.getIronGolem());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            PlayerInteractEntityEvent event = clickGolem(player, golem, EquipmentSlot.HAND);

            Assertions.assertTrue(seen[0], "IronGolemHealEvent was not fired");
            Assertions.assertTrue(event.isCancelled(), "The heal must have been blocked");
            Assertions.assertTrue(fakeIron.isItem(player.getInventory().getItemInMainHand()), "The hand must have been refreshed with the item");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling IronGolemHealEvent allows the heal")
    void testEventCancellationAllowsHeal() {
        Player player = server.addPlayer();
        IronGolemMock golem = new IronGolemMock(server, UUID.randomUUID());
        player.getInventory().setItemInMainHand(fakeIron.getItem().clone());

        Listener cancelling = new Listener() {
            @EventHandler
            public void onHeal(IronGolemHealEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            PlayerInteractEntityEvent event = clickGolem(player, golem, EquipmentSlot.HAND);

            Assertions.assertFalse(event.isCancelled(), "A cancelled block must allow the heal");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Healing with a Slimefun iron ingot in the off hand is blocked too")
    void testOffHandHealBlocked() {
        Player player = server.addPlayer();
        IronGolemMock golem = new IronGolemMock(server, UUID.randomUUID());
        player.getInventory().setItemInOffHand(fakeIron.getItem().clone());

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onHeal(IronGolemHealEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            PlayerInteractEntityEvent event = clickGolem(player, golem, EquipmentSlot.OFF_HAND);

            Assertions.assertTrue(seen[0], "IronGolemHealEvent was not fired");
            Assertions.assertTrue(event.isCancelled(), "The off-hand heal must have been blocked");
            Assertions.assertTrue(fakeIron.isItem(player.getInventory().getItemInOffHand()), "The off hand must have been refreshed with the item");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Healing with a vanilla iron ingot fires no event and is allowed")
    void testVanillaIronFiresNothing() {
        Player player = server.addPlayer();
        IronGolemMock golem = new IronGolemMock(server, UUID.randomUUID());
        player.getInventory().setItemInMainHand(new ItemStack(Material.IRON_INGOT));

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onHeal(IronGolemHealEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            PlayerInteractEntityEvent event = clickGolem(player, golem, EquipmentSlot.HAND);

            Assertions.assertFalse(seen[0], "No event must be fired for a vanilla iron ingot");
            Assertions.assertFalse(event.isCancelled(), "A vanilla iron ingot must heal normally");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Clicking with a Slimefun item that is no iron ingot fires no event")
    void testOtherSlimefunItemFiresNothing() {
        Player player = server.addPlayer();
        IronGolemMock golem = new IronGolemMock(server, UUID.randomUUID());
        player.getInventory().setItemInMainHand(otherItem.getItem().clone());

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onHeal(IronGolemHealEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            PlayerInteractEntityEvent event = clickGolem(player, golem, EquipmentSlot.HAND);

            Assertions.assertFalse(seen[0], "No event must be fired for a non-ingot SlimefunItem");
            Assertions.assertFalse(event.isCancelled(), "The interaction must have been left alone");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Blocking without listeners still applies, preserving the old behavior")
    void testBlockWithoutListenersStillApplies() {
        Player player = server.addPlayer();
        IronGolemMock golem = new IronGolemMock(server, UUID.randomUUID());
        player.getInventory().setItemInMainHand(fakeIron.getItem().clone());

        PlayerInteractEntityEvent event = clickGolem(player, golem, EquipmentSlot.HAND);

        Assertions.assertTrue(event.isCancelled(), "The heal must have been blocked");
    }
}
