package io.github.thebusybiscuit.slimefun4.implementation.items.weapons;

import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
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
import be.seeseemelk.mockbukkit.entity.CowMock;

import io.github.thebusybiscuit.slimefun4.api.events.VampireBladeHealEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Regression coverage for the vampire blade API expansion: {@link VampireBladeHealEvent},
 * exercised by driving the real {@link VampireBlade} {@link io.github.thebusybiscuit.slimefun4.core.handlers.WeaponUseHandler}
 * directly. The blade is registered with a 100% heal chance so the lifesteal always triggers.
 *
 * @author Zurker
 */
class TestVampireBladeHealEvent {

    private static ServerMock server;
    private static Slimefun plugin;

    private static VampireBlade blade;
    private static final double HEALING_AMOUNT = 4.0;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "vampire_blade_test");
        SlimefunItemStack stack = new SlimefunItemStack("_TEST_VAMPIRE_BLADE", Material.IRON_SWORD, "&fTest Vampire Blade");
        // Force the lifesteal chance to 100% so the heal always triggers
        Slimefun.getItemCfg().setValue("_TEST_VAMPIRE_BLADE.enabled", true);
        Slimefun.getItemCfg().setValue("_TEST_VAMPIRE_BLADE.chance", 100);
        blade = new VampireBlade(itemGroup, stack, RecipeType.NULL, new ItemStack[9]);
        blade.register(plugin);
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
     * Lets the player hit the cow with the blade via the handler directly.
     */
    private void hit(Player player, LivingEntity victim) {
        EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(player, victim, DamageCause.ENTITY_ATTACK, 5.0);
        blade.getItemHandler().onHit(event, player, blade.getItem().clone());
    }

    @Test
    @DisplayName("VampireBladeHealEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Player player = server.addPlayer();

        VampireBladeHealEvent event = new VampireBladeHealEvent(player, blade, HEALING_AMOUNT);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(blade, event.getVampireBlade());
        Assertions.assertEquals(HEALING_AMOUNT, event.getHealAmount(), 0.001);
        Assertions.assertFalse(event.isCancelled());

        event.setHealAmount(8.0);
        Assertions.assertEquals(8.0, event.getHealAmount(), 0.001);

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new VampireBladeHealEvent(player, null, HEALING_AMOUNT));
    }

    @Test
    @DisplayName("A hit fires the event and heals the attacker by the default amount")
    void testHitFiresAndHeals() {
        Player player = server.addPlayer();
        player.setHealth(10.0);
        CowMock victim = new CowMock(server, UUID.randomUUID());

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onHeal(VampireBladeHealEvent event) {
                seen[0] = true;
                Assertions.assertEquals(blade, event.getVampireBlade());
                Assertions.assertEquals(HEALING_AMOUNT, event.getHealAmount(), 0.001);
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            hit(player, victim);

            Assertions.assertTrue(seen[0], "VampireBladeHealEvent was not fired");
            Assertions.assertEquals(10.0 + HEALING_AMOUNT, player.getHealth(), 0.001, "The attacker must have been healed");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling VampireBladeHealEvent prevents the heal")
    void testEventCancellationSkipsHeal() {
        Player player = server.addPlayer();
        player.setHealth(10.0);
        CowMock victim = new CowMock(server, UUID.randomUUID());

        Listener cancelling = new Listener() {
            @EventHandler
            public void onHeal(VampireBladeHealEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            hit(player, victim);

            Assertions.assertEquals(10.0, player.getHealth(), 0.001, "A cancelled heal must not restore health");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Overriding the heal amount via setHealAmount heals by the custom amount")
    void testHealAmountOverride() {
        Player player = server.addPlayer();
        player.setHealth(10.0);
        CowMock victim = new CowMock(server, UUID.randomUUID());

        Listener overriding = new Listener() {
            @EventHandler
            public void onHeal(VampireBladeHealEvent event) {
                event.setHealAmount(6.0);
            }
        };
        server.getPluginManager().registerEvents(overriding, plugin);

        try {
            hit(player, victim);

            Assertions.assertEquals(16.0, player.getHealth(), 0.001, "The attacker must have been healed by the custom amount");
        } finally {
            HandlerList.unregisterAll(overriding);
        }
    }

    @Test
    @DisplayName("A heal is capped at the player's max health")
    void testHealCappedAtMaxHealth() {
        Player player = server.addPlayer();
        player.setHealth(18.0);
        CowMock victim = new CowMock(server, UUID.randomUUID());

        try {
            hit(player, victim);

            // 18 + 4 = 22, capped at 20 (MockBukkit reports no MAX_HEALTH attribute -> fallback 20)
            Assertions.assertEquals(20.0, player.getHealth(), 0.001, "The heal must have been capped at max health");
        } finally {
            HandlerList.unregisterAll();
        }
    }

    @Test
    @DisplayName("Healing without listeners still applies, preserving the old behavior")
    void testHealWithoutListenersStillApplies() {
        Player player = server.addPlayer();
        player.setHealth(10.0);
        CowMock victim = new CowMock(server, UUID.randomUUID());

        hit(player, victim);

        Assertions.assertEquals(10.0 + HEALING_AMOUNT, player.getHealth(), 0.001, "The attacker must have been healed");
    }
}
