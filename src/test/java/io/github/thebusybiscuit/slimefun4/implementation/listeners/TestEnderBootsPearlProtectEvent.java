package io.github.thebusybiscuit.slimefun4.implementation.listeners;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.EnderPearl;
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
import org.mockito.Mockito;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

import io.github.thebusybiscuit.slimefun4.api.events.EnderBootsPearlProtectEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.armor.EnderBoots;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Regression coverage for the ender boots API expansion:
 * {@link EnderBootsPearlProtectEvent}, exercised by driving the real
 * {@link SlimefunBootsListener#onEnderPearlDamage(EntityDamageByEntityEvent)} with a
 * constructed damage event sourced from a pearl and a player wearing the boots.
 * <p>
 * MockBukkit has no pearl entity mock, so the {@link EnderPearl} damager is a plain
 * Mockito stub - the listener only ever reads it via {@code getDamager()}.
 * The protection manifests as the damage event being cancelled, so tests assert it
 * end-to-end: a cancelled protection event lets the vanilla damage through.
 *
 * @author Zurker
 */
class TestEnderBootsPearlProtectEvent {

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static EnderBoots enderBoots;
    private static SlimefunBootsListener listener;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "ender_boots_test");
        SlimefunItemStack stack = new SlimefunItemStack("_TEST_ENDER_BOOTS", Material.LEATHER_BOOTS, "&7Test Ender Boots");
        Slimefun.getItemCfg().setValue("_TEST_ENDER_BOOTS.enabled", true);
        enderBoots = new EnderBoots(itemGroup, stack, RecipeType.NULL, new ItemStack[9]);
        enderBoots.register(plugin);

        listener = new SlimefunBootsListener(plugin);
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
     * Hits the player with pearl damage via the real boots listener.
     */
    private EntityDamageByEntityEvent pearlDamage(Player player, EnderPearl pearl) {
        EntityDamageByEntityEvent damageEvent = new EntityDamageByEntityEvent(pearl, player, DamageCause.FALL, 5.0);
        listener.onEnderPearlDamage(damageEvent);
        return damageEvent;
    }

    @Test
    @DisplayName("EnderBootsPearlProtectEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Player player = server.addPlayer();
        EnderPearl pearl = Mockito.mock(EnderPearl.class);
        ItemStack bootsItem = enderBoots.getItem().clone();

        EnderBootsPearlProtectEvent event = new EnderBootsPearlProtectEvent(player, enderBoots, bootsItem, pearl);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(enderBoots, event.getBoots());
        Assertions.assertEquals(bootsItem, event.getBootsItem());
        Assertions.assertEquals(pearl, event.getPearl());
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new EnderBootsPearlProtectEvent(player, null, bootsItem, pearl));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new EnderBootsPearlProtectEvent(player, enderBoots, null, pearl));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new EnderBootsPearlProtectEvent(player, enderBoots, bootsItem, null));
    }

    @Test
    @DisplayName("Taking pearl damage with the boots fires the event and cancels the damage")
    void testPearlDamageFiresEventAndProtects() {
        Player player = server.addPlayer();
        player.getInventory().setBoots(enderBoots.getItem().clone());
        EnderPearl pearl = Mockito.mock(EnderPearl.class);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onProtect(EnderBootsPearlProtectEvent event) {
                seen[0] = true;
                Assertions.assertEquals(player, event.getPlayer());
                Assertions.assertEquals(enderBoots, event.getBoots());
                Assertions.assertEquals(pearl, event.getPearl());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            EntityDamageByEntityEvent damageEvent = pearlDamage(player, pearl);

            Assertions.assertTrue(seen[0], "EnderBootsPearlProtectEvent was not fired");
            Assertions.assertTrue(damageEvent.isCancelled(), "The pearl damage must have been negated");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling EnderBootsPearlProtectEvent lets the vanilla damage through")
    void testCancelLetsDamageThrough() {
        Player player = server.addPlayer();
        player.getInventory().setBoots(enderBoots.getItem().clone());
        EnderPearl pearl = Mockito.mock(EnderPearl.class);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onProtect(EnderBootsPearlProtectEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            EntityDamageByEntityEvent damageEvent = pearlDamage(player, pearl);

            Assertions.assertFalse(damageEvent.isCancelled(), "A vetoed protection must let the damage through");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Protecting without listeners still cancels the damage, preserving the old behavior")
    void testProtectWithoutListenersStillProtects() {
        Player player = server.addPlayer();
        player.getInventory().setBoots(enderBoots.getItem().clone());
        EnderPearl pearl = Mockito.mock(EnderPearl.class);

        EntityDamageByEntityEvent damageEvent = pearlDamage(player, pearl);

        Assertions.assertTrue(damageEvent.isCancelled(), "The pearl damage must have been negated");
    }

    @Test
    @DisplayName("Taking pearl damage without the boots fires no event")
    void testNoBootsFiresNothing() {
        Player player = server.addPlayer();
        EnderPearl pearl = Mockito.mock(EnderPearl.class);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onProtect(EnderBootsPearlProtectEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            EntityDamageByEntityEvent damageEvent = pearlDamage(player, pearl);

            Assertions.assertFalse(seen[0], "No event must be fired without the boots");
            Assertions.assertFalse(damageEvent.isCancelled(), "A bare-foot pearl hit must not be negated");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Damage from a non-pearl source fires no event")
    void testNonPearlSourceFiresNothing() {
        Player player = server.addPlayer();
        player.getInventory().setBoots(enderBoots.getItem().clone());
        Player attacker = server.addPlayer();

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onProtect(EnderBootsPearlProtectEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            EntityDamageByEntityEvent damageEvent = new EntityDamageByEntityEvent(attacker, player, DamageCause.ENTITY_ATTACK, 5.0);
            listener.onEnderPearlDamage(damageEvent);

            Assertions.assertFalse(seen[0], "No event must be fired for a non-pearl damager");
            Assertions.assertFalse(damageEvent.isCancelled(), "A melee hit must not be negated");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }
}
