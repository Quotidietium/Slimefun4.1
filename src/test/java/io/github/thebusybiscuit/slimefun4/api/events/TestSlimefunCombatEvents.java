package io.github.thebusybiscuit.slimefun4.api.events;

import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
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
import be.seeseemelk.mockbukkit.entity.PlayerMock;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.core.attributes.SlimefunWeapon;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.weapons.SeismicAxe;
import io.github.thebusybiscuit.slimefun4.implementation.items.weapons.SlimefunBow;
import io.github.thebusybiscuit.slimefun4.implementation.items.weapons.SwordOfBeheading;
import io.github.thebusybiscuit.slimefun4.implementation.items.weapons.VampireBlade;
import io.github.thebusybiscuit.slimefun4.implementation.listeners.SlimefunItemHitListener;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Regression coverage for the combat API expansion: {@link SlimefunWeapon} marker,
 * {@link SlimefunItemDamageEvent} and {@link SlimefunBowShootEvent}.
 *
 * @author Zurker
 */
class TestSlimefunCombatEvents {

    private static ServerMock server;
    private static Slimefun plugin;
    private static SlimefunItem testItem;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);

        // The combat listener self-registers in its constructor.
        new SlimefunItemHitListener(plugin);

        testItem = TestUtilities.mockSlimefunItem(plugin, "COMBAT_TEST_ITEM", new ItemStack(Material.IRON_SWORD));
        testItem.register(plugin);
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    @BeforeEach
    public void beforeEach() {
        server.getPluginManager().clearEvents();
    }

    @Test
    @DisplayName("Built-in weapon classes implement SlimefunWeapon")
    void testBuiltInWeaponsImplementSlimefunWeapon() {
        Assertions.assertTrue(SlimefunWeapon.class.isAssignableFrom(VampireBlade.class));
        Assertions.assertTrue(SlimefunWeapon.class.isAssignableFrom(SeismicAxe.class));
        Assertions.assertTrue(SlimefunWeapon.class.isAssignableFrom(SwordOfBeheading.class));
        Assertions.assertTrue(SlimefunWeapon.class.isAssignableFrom(SlimefunBow.class));
        // A plain SlimefunItem is not a weapon
        Assertions.assertFalse(SlimefunWeapon.class.isAssignableFrom(SlimefunItem.class));
    }

    @Test
    @DisplayName("SlimefunItemDamageEvent fires when a Slimefun item deals melee damage")
    void testDamageEventFiresFromListener() {
        PlayerMock damager = new PlayerMock(server, "attacker");
        PlayerMock victim = new PlayerMock(server, "victim");
        server.addPlayer(damager);
        server.addPlayer(victim);

        ItemStack weapon = testItem.getItem();
        damager.getInventory().setItemInMainHand(weapon);

        EntityDamageByEntityEvent bukkitEvent = new EntityDamageByEntityEvent(damager, victim, EntityDamageEvent.DamageCause.ENTITY_ATTACK, 5.0);
        server.getPluginManager().callEvent(bukkitEvent);

        server.getPluginManager().assertEventFired(SlimefunItemDamageEvent.class, e -> {
            Assertions.assertEquals(damager, e.getDamager());
            Assertions.assertEquals(victim, e.getVictim());
            Assertions.assertEquals(testItem, e.getSlimefunItem());
            Assertions.assertEquals(weapon, e.getWeapon());
            Assertions.assertEquals(5.0, e.getDamage());
            Assertions.assertFalse(e.isCancelled());
            // The plain mock item is not tagged as a SlimefunWeapon
            Assertions.assertFalse(e.isWeapon());
            return true;
        });
    }

    @Test
    @DisplayName("SlimefunItemDamageEvent delegates damage and cancellation to the underlying event")
    void testDamageEventDelegates() {
        PlayerMock damager = new PlayerMock(server, "delegater");
        PlayerMock victim = new PlayerMock(server, "delegatee");

        EntityDamageByEntityEvent underlying = Mockito.mock(EntityDamageByEntityEvent.class);
        Mockito.when(underlying.getDamage()).thenReturn(7.0);
        Mockito.when(underlying.isCancelled()).thenReturn(false);

        SlimefunItemDamageEvent event = new SlimefunItemDamageEvent(damager, victim, testItem, testItem.getItem(), underlying);

        // Reads delegate
        Assertions.assertEquals(7.0, event.getDamage());
        Assertions.assertFalse(event.isCancelled());

        // Writes delegate through to the underlying Bukkit event
        event.setDamage(42.0);
        Mockito.verify(underlying).setDamage(42.0);

        event.setCancelled(true);
        Mockito.verify(underlying).setCancelled(true);
    }

    @Test
    @DisplayName("SlimefunBowShootEvent exposes bow/arrow and supports cancellation")
    void testBowShootEventUnit() {
        SlimefunBow bow = Mockito.mock(SlimefunBow.class);
        PlayerMock shooter = new PlayerMock(server, "archer");
        ItemStack bowItem = new ItemStack(Material.BOW);
        Arrow arrow = Mockito.mock(Arrow.class);
        EntityShootBowEvent underlying = Mockito.mock(EntityShootBowEvent.class);

        SlimefunBowShootEvent event = new SlimefunBowShootEvent(shooter, bow, bowItem, arrow, underlying);

        Assertions.assertEquals(shooter, event.getPlayer());
        Assertions.assertEquals(bow, event.getBow());
        Assertions.assertEquals(bowItem, event.getBowItem());
        Assertions.assertEquals(arrow, event.getArrow());
        Assertions.assertEquals(underlying, event.getEntityShootBowEvent());
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());
    }
}
