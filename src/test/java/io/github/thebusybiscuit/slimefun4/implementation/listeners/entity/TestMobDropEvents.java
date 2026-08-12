package io.github.thebusybiscuit.slimefun4.implementation.listeners.entity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
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

import io.github.bakedlibs.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun4.api.events.SlimefunEntityKillEvent;
import io.github.thebusybiscuit.slimefun4.api.events.SlimefunMobDropEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.EntityKillHandler;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.SimpleSlimefunItem;

/**
 * Regression coverage for the entity-kill API expansion: {@link SlimefunMobDropEvent}
 * and {@link SlimefunEntityKillEvent}, exercised through the real {@link MobDropListener}
 * kill path.
 *
 * @author Zurker
 */
class TestMobDropEvents {

    private static ServerMock server;
    private static Slimefun plugin;
    private static MobDropListener listener;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        listener = new MobDropListener(plugin);
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    @BeforeEach
    public void beforeEach() {
        server.getPluginManager().clearEvents();
    }

    private EntityDeathEvent newDeathEvent(Player killer, List<ItemStack> drops) {
        LivingEntity entity = Mockito.mock(LivingEntity.class);
        Mockito.when(entity.getType()).thenReturn(EntityType.ZOMBIE);
        Mockito.when(entity.getKiller()).thenReturn(killer);

        return new EntityDeathEvent(entity, Mockito.mock(org.bukkit.damage.DamageSource.class), drops);
    }

    @Test
    @DisplayName("SlimefunMobDropEvent fires per custom drop and cancellation skips only that drop")
    void testMobDropEventAndCancellation() {
        Player player = server.addPlayer();
        ItemStack customDrop = new ItemStack(Material.NETHER_STAR, 1);

        Set<ItemStack> drops = Slimefun.getRegistry().getMobDrops().computeIfAbsent(EntityType.ZOMBIE, type -> new HashSet<>());
        drops.add(customDrop);

        try {
            // Baseline: without listeners the custom drop is added as before
            EntityDeathEvent deathEvent = newDeathEvent(player, new ArrayList<>());
            listener.onEntityKill(deathEvent);
            Assertions.assertEquals(1, deathEvent.getDrops().size());
            Assertions.assertEquals(customDrop, deathEvent.getDrops().get(0));

            // A cancelling listener vetoes exactly this drop
            Listener cancelling = new Listener() {
                @EventHandler
                public void onDrop(SlimefunMobDropEvent event) {
                    Assertions.assertEquals(player, event.getKiller());
                    Assertions.assertEquals(EntityType.ZOMBIE, event.getEntity().getType());
                    Assertions.assertEquals(customDrop, event.getDrop());
                    event.setCancelled(true);
                }
            };
            server.getPluginManager().registerEvents(cancelling, plugin);

            try {
                EntityDeathEvent cancelledDeath = newDeathEvent(player, new ArrayList<>());
                listener.onEntityKill(cancelledDeath);
                Assertions.assertTrue(cancelledDeath.getDrops().isEmpty());
            } finally {
                HandlerList.unregisterAll(cancelling);
            }
        } finally {
            Slimefun.getRegistry().getMobDrops().remove(EntityType.ZOMBIE);
        }
    }

    @Test
    @DisplayName("SlimefunEntityKillEvent fires before the EntityKillHandler and cancellation skips it")
    void testEntityKillEventAndCancellation() {
        Player player = server.addPlayer();

        ItemGroup itemGroup = new ItemGroup(new NamespacedKey(plugin, "test_kill_events"), CustomItemStack.create(Material.DIAMOND_SWORD, "&4Test Weapons"));
        SlimefunItemStack swordStack = new SlimefunItemStack("KILL_EVENT_SWORD", Material.DIAMOND_SWORD, "&cKill Event Sword");

        AtomicBoolean handlerCalled = new AtomicBoolean(false);
        SimpleSlimefunItem<EntityKillHandler> sword = new SimpleSlimefunItem<>(itemGroup, swordStack, RecipeType.NULL, new ItemStack[9]) {
            @Override
            public EntityKillHandler getItemHandler() {
                return (e, entity, killer, item) -> handlerCalled.set(true);
            }
        };
        sword.register(plugin);

        player.getInventory().setItemInMainHand(swordStack.item());

        // Baseline: the handler runs and the kill event fires with the right context
        AtomicBoolean eventSeen = new AtomicBoolean(false);
        Listener watcher = new Listener() {
            @EventHandler
            public void onKill(SlimefunEntityKillEvent event) {
                eventSeen.set(true);
                Assertions.assertEquals(player, event.getKiller());
                Assertions.assertEquals(sword, event.getSlimefunItem());
                Assertions.assertNotNull(event.getEntityDeathEvent());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            listener.onEntityKill(newDeathEvent(player, new ArrayList<>()));
            Assertions.assertTrue(eventSeen.get());
            Assertions.assertTrue(handlerCalled.get());
        } finally {
            HandlerList.unregisterAll(watcher);
        }

        // A cancelling listener prevents the handler from running
        handlerCalled.set(false);
        Listener cancelling = new Listener() {
            @EventHandler
            public void onKill(SlimefunEntityKillEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            listener.onEntityKill(newDeathEvent(player, new ArrayList<>()));
            Assertions.assertFalse(handlerCalled.get());
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Replacing the drop via setDrop adds the replacement to the drops")
    void testSetDropReplacesCustomDrop() {
        Player player = server.addPlayer();
        ItemStack customDrop = new ItemStack(Material.NETHER_STAR, 1);

        Set<ItemStack> drops = Slimefun.getRegistry().getMobDrops().computeIfAbsent(EntityType.ZOMBIE, type -> new HashSet<>());
        drops.add(customDrop);

        ItemStack replacement = new ItemStack(Material.DIAMOND, 3);
        Listener replacing = new Listener() {
            @EventHandler
            public void onDrop(SlimefunMobDropEvent event) {
                Assertions.assertEquals(customDrop, event.getDrop(), "The original custom drop must be exposed");
                event.setDrop(replacement);
            }
        };
        server.getPluginManager().registerEvents(replacing, plugin);

        try {
            EntityDeathEvent deathEvent = newDeathEvent(player, new ArrayList<>());
            listener.onEntityKill(deathEvent);

            Assertions.assertEquals(1, deathEvent.getDrops().size(), "Exactly one drop must have been added");
            Assertions.assertEquals(replacement, deathEvent.getDrops().get(0), "The replacement must have been added");
            Assertions.assertNotEquals(Material.NETHER_STAR, deathEvent.getDrops().get(0).getType(), "The original drop must not have been added");
        } finally {
            HandlerList.unregisterAll(replacing);
            Slimefun.getRegistry().getMobDrops().remove(EntityType.ZOMBIE);
        }
    }

    @Test
    @DisplayName("Both events reject null arguments")
    void testNullValidation() {
        Player player = server.addPlayer();
        LivingEntity entity = Mockito.mock(LivingEntity.class);
        ItemStack drop = new ItemStack(Material.DIAMOND);
        EntityDeathEvent deathEvent = newDeathEvent(player, new ArrayList<>());
        SlimefunItem sfItem = Mockito.mock(SlimefunItem.class);

        SlimefunMobDropEvent mobDropEvent = new SlimefunMobDropEvent(player, entity, drop, deathEvent);
        mobDropEvent.setDrop(new ItemStack(Material.EMERALD));
        Assertions.assertEquals(Material.EMERALD, mobDropEvent.getDrop().getType(), "The replaced drop must be returned");
        Assertions.assertThrows(IllegalArgumentException.class, () -> mobDropEvent.setDrop(null));

        Assertions.assertThrows(IllegalArgumentException.class, () -> new SlimefunMobDropEvent(null, entity, drop, deathEvent));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new SlimefunMobDropEvent(player, null, drop, deathEvent));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new SlimefunMobDropEvent(player, entity, null, deathEvent));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new SlimefunMobDropEvent(player, entity, drop, null));

        Assertions.assertThrows(IllegalArgumentException.class, () -> new SlimefunEntityKillEvent(null, entity, sfItem, drop, deathEvent));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new SlimefunEntityKillEvent(player, entity, null, drop, deathEvent));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new SlimefunEntityKillEvent(player, entity, sfItem, null, deathEvent));
    }
}
