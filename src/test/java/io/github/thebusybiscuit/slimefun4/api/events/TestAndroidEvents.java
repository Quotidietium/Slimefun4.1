package io.github.thebusybiscuit.slimefun4.api.events;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
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
import org.mockito.Mockito;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.androids.AndroidInstance;
import io.github.thebusybiscuit.slimefun4.implementation.items.androids.ProgrammableAndroid;

/**
 * Regression coverage for the android API expansion: {@link AndroidAttackEvent} and
 * {@link AndroidFishEvent}.
 *
 * @author Zurker
 */
class TestAndroidEvents {

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

    private static AndroidInstance newInstance() {
        return new AndroidInstance(Mockito.mock(ProgrammableAndroid.class), Mockito.mock(Block.class));
    }

    @Test
    @DisplayName("AndroidAttackEvent exposes android, target, adjustable damage and cancellation")
    void testAttackEventFieldsAndDamage() {
        AndroidInstance instance = newInstance();
        LivingEntity target = Mockito.mock(LivingEntity.class);

        AndroidAttackEvent event = new AndroidAttackEvent(instance, target, 8.0);

        Assertions.assertEquals(instance, event.getAndroid());
        Assertions.assertEquals(target, event.getTarget());
        Assertions.assertEquals(8.0, event.getDamage());
        Assertions.assertFalse(event.isCancelled());

        event.setDamage(20.0);
        Assertions.assertEquals(20.0, event.getDamage());

        // Negative damage is rejected
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setDamage(-1));

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());
    }

    @Test
    @DisplayName("AndroidFishEvent exposes android, replaceable drop and cancellation")
    void testFishEventFieldsAndDrop() {
        AndroidInstance instance = newInstance();
        ItemStack drop = new ItemStack(Material.COD, 1);

        AndroidFishEvent event = new AndroidFishEvent(instance, drop);

        Assertions.assertEquals(instance, event.getAndroid());
        Assertions.assertEquals(drop, event.getDrop());
        Assertions.assertFalse(event.isCancelled());

        ItemStack replacement = new ItemStack(Material.DIAMOND, 3);
        event.setDrop(replacement);
        Assertions.assertEquals(replacement, event.getDrop());

        // A null drop is rejected
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setDrop(null));

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());
    }

    @Test
    @DisplayName("AndroidMineEvent exposes android, block and the mutable drops")
    void testMineEventFieldsAndDrops() {
        AndroidInstance instance = newInstance();
        Block block = Mockito.mock(Block.class);
        ItemStack coal = new ItemStack(Material.COAL, 2);

        // The legacy constructor carries no drops
        AndroidMineEvent legacy = new AndroidMineEvent(block, instance);
        Assertions.assertEquals(block, legacy.getBlock());
        Assertions.assertEquals(instance, legacy.getAndroid());
        Assertions.assertTrue(legacy.getDrops().isEmpty());
        Assertions.assertFalse(legacy.isCancelled());

        AndroidMineEvent event = new AndroidMineEvent(block, instance, java.util.List.of(coal));
        Assertions.assertEquals(1, event.getDrops().size());
        Assertions.assertEquals(coal, event.getDrops().get(0));

        // The drops are mutable: entries can be removed and added
        event.getDrops().clear();
        Assertions.assertTrue(event.getDrops().isEmpty());

        ItemStack diamond = new ItemStack(Material.DIAMOND, 1);
        event.getDrops().add(diamond);
        Assertions.assertEquals(1, event.getDrops().size());
        Assertions.assertEquals(diamond, event.getDrops().get(0));

        Assertions.assertThrows(IllegalArgumentException.class, () -> new AndroidMineEvent(null, instance, java.util.List.of(coal)));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new AndroidMineEvent(block, null, java.util.List.of(coal)));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new AndroidMineEvent(block, instance, null));

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());
    }

    @Test
    @DisplayName("AndroidMineEvent is dispatchable and listener mutations to the drops are visible")
    void testMineEventDispatch() {
        AndroidInstance instance = newInstance();
        Block block = Mockito.mock(Block.class);

        Listener mutating = new Listener() {
            @EventHandler
            public void onMine(AndroidMineEvent event) {
                event.getDrops().clear();
                event.getDrops().add(new ItemStack(Material.EMERALD, 5));
            }
        };
        server.getPluginManager().registerEvents(mutating, plugin);

        try {
            AndroidMineEvent event = new AndroidMineEvent(block, instance, java.util.List.of(new ItemStack(Material.COAL)));
            server.getPluginManager().callEvent(event);

            Assertions.assertEquals(1, event.getDrops().size());
            Assertions.assertEquals(Material.EMERALD, event.getDrops().get(0).getType());
            Assertions.assertEquals(5, event.getDrops().get(0).getAmount());
        } finally {
            HandlerList.unregisterAll(mutating);
        }
    }

    @Test
    @DisplayName("Both android events are dispatchable and listener mutations are visible")
    void testEventDispatch() {
        AndroidInstance instance = newInstance();
        LivingEntity target = Mockito.mock(LivingEntity.class);

        AndroidListener listener = new AndroidListener();
        server.getPluginManager().registerEvents(listener, plugin);

        try {
            AndroidAttackEvent attackEvent = new AndroidAttackEvent(instance, target, 4.0);
            server.getPluginManager().callEvent(attackEvent);

            Assertions.assertTrue(listener.attackSeen);
            Assertions.assertTrue(attackEvent.isCancelled());

            AndroidFishEvent fishEvent = new AndroidFishEvent(instance, new ItemStack(Material.COD));
            server.getPluginManager().callEvent(fishEvent);

            Assertions.assertTrue(listener.fishSeen);
            Assertions.assertEquals(Material.TROPICAL_FISH, fishEvent.getDrop().getType());
        } finally {
            HandlerList.unregisterAll(listener);
        }
    }

    private static class AndroidListener implements Listener {
        boolean attackSeen;
        boolean fishSeen;

        @EventHandler
        public void onAttack(AndroidAttackEvent event) {
            attackSeen = true;
            event.setCancelled(true);
        }

        @EventHandler
        public void onFish(AndroidFishEvent event) {
            fishSeen = true;
            event.setDrop(new ItemStack(Material.TROPICAL_FISH));
        }
    }
}
