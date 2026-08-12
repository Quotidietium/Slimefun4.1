package io.github.thebusybiscuit.slimefun4.implementation.items.magical;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.entity.ZombieVillager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
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

import io.github.thebusybiscuit.slimefun4.api.events.MagicalZombiePillsCureEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Regression coverage for the magical zombie pills API expansion:
 * {@link MagicalZombiePillsCureEvent}, exercised by driving the real
 * {@link MagicalZombiePills} {@link io.github.thebusybiscuit.slimefun4.core.handlers.EntityInteractHandler}
 * with a constructed {@link PlayerInteractEntityEvent}.
 * <p>
 * MockBukkit has no ZombieVillager mock, so the cured entity is a Mockito mock whose
 * conversion calls are verified.
 *
 * @author Zurker
 */
class TestMagicalZombiePillsCureEvent {

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static MagicalZombiePills pills;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        // ProtectionManager is created when the integrations start
        Slimefun.getIntegrations().start();
        server.getScheduler().performOneTick();

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "zombie_pills_test");
        SlimefunItemStack stack = new SlimefunItemStack("_TEST_ZOMBIE_PILLS", org.bukkit.Material.GHAST_TEAR, "&dTest Magical Zombie Pills");
        Slimefun.getItemCfg().setValue("_TEST_ZOMBIE_PILLS.enabled", true);
        pills = new MagicalZombiePills(itemGroup, stack, RecipeType.NULL, new ItemStack[9], new ItemStack(org.bukkit.Material.GHAST_TEAR));
        pills.register(plugin);
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
     * Right-clicks a Mockito zombie villager with a fresh stack of pills via the real handler.
     *
     * @return The used (possibly consumed) pills stack
     */
    private ItemStack cure(Player player, ZombieVillager zombieVillager) {
        ItemStack item = pills.getItem().clone();
        item.setAmount(3);

        PlayerInteractEntityEvent interactEvent = new PlayerInteractEntityEvent(player, zombieVillager);

        try {
            pills.getItemHandler().onInteract(interactEvent, item, false);
        } catch (RuntimeException ignored) {
            // sound tail not fully supported by MockBukkit - the cure decision happened beforehand
        }

        return item;
    }

    private ZombieVillager mockZombieVillager(int x, int z) {
        ZombieVillager zombieVillager = Mockito.mock(ZombieVillager.class);
        Mockito.when(zombieVillager.getLocation()).thenReturn(new Location(world, x, 4, z));
        return zombieVillager;
    }

    @Test
    @DisplayName("MagicalZombiePillsCureEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Player player = server.addPlayer();
        ZombieVillager zombieVillager = mockZombieVillager(0, 0);
        ItemStack item = new ItemStack(org.bukkit.Material.GHAST_TEAR);

        MagicalZombiePillsCureEvent event = new MagicalZombiePillsCureEvent(player, pills, zombieVillager, item);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(pills, event.getPills());
        Assertions.assertEquals(zombieVillager, event.getEntity());
        Assertions.assertEquals(item, event.getItem());
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new MagicalZombiePillsCureEvent(player, null, zombieVillager, item));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new MagicalZombiePillsCureEvent(player, pills, null, item));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new MagicalZombiePillsCureEvent(player, pills, zombieVillager, null));
    }

    @Test
    @DisplayName("The conversion time defaults to 1, is modifiable and validated")
    void testConversionTimeValidation() {
        Player player = server.addPlayer();
        ZombieVillager zombieVillager = mockZombieVillager(0, 0);
        ItemStack item = new ItemStack(org.bukkit.Material.GHAST_TEAR);

        MagicalZombiePillsCureEvent event = new MagicalZombiePillsCureEvent(player, pills, zombieVillager, item);

        Assertions.assertEquals(1, event.getConversionTime(), "The conversion time must default to the historic instant cure");

        event.setConversionTime(2000);
        Assertions.assertEquals(2000, event.getConversionTime());

        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setConversionTime(0));
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setConversionTime(-1));
    }

    @Test
    @DisplayName("A modified conversion time is applied to the cured zombie villager")
    void testModifiedConversionTimeApplied() {
        Player player = server.addPlayer();
        ZombieVillager zombieVillager = mockZombieVillager(40, 40);

        Listener delaying = new Listener() {
            @EventHandler
            public void onCure(MagicalZombiePillsCureEvent event) {
                event.setConversionTime(2000);
            }
        };
        server.getPluginManager().registerEvents(delaying, plugin);

        try {
            ItemStack item = cure(player, zombieVillager);

            Assertions.assertEquals(2, item.getAmount(), "One pill must have been consumed");
            Mockito.verify(zombieVillager).setConversionTime(2000);
            Mockito.verify(zombieVillager).setConversionPlayer(player);
        } finally {
            HandlerList.unregisterAll(delaying);
        }
    }

    @Test
    @DisplayName("Curing a zombie villager fires the event, consumes a pill and starts the conversion")
    void testCureFiresAndConverts() {
        Player player = server.addPlayer();
        ZombieVillager zombieVillager = mockZombieVillager(10, 10);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onCure(MagicalZombiePillsCureEvent event) {
                seen[0] = true;
                Assertions.assertEquals(pills, event.getPills());
                Assertions.assertEquals(zombieVillager, event.getEntity());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            ItemStack item = cure(player, zombieVillager);

            Assertions.assertTrue(seen[0], "MagicalZombiePillsCureEvent was not fired");
            Assertions.assertEquals(2, item.getAmount(), "One pill must have been consumed");
            Mockito.verify(zombieVillager).setConversionTime(1);
            Mockito.verify(zombieVillager).setConversionPlayer(player);
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling MagicalZombiePillsCureEvent keeps the pill and skips the conversion")
    void testEventCancellationSkipsCure() {
        Player player = server.addPlayer();
        ZombieVillager zombieVillager = mockZombieVillager(20, 20);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onCure(MagicalZombiePillsCureEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            ItemStack item = cure(player, zombieVillager);

            Assertions.assertEquals(3, item.getAmount(), "A cancelled cure must keep the pill");
            Mockito.verify(zombieVillager, Mockito.never()).setConversionTime(Mockito.anyInt());
            Mockito.verify(zombieVillager, Mockito.never()).setConversionPlayer(Mockito.any());
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Curing without listeners still converts, preserving the old behavior")
    void testCureWithoutListenersStillConverts() {
        Player player = server.addPlayer();
        ZombieVillager zombieVillager = mockZombieVillager(30, 30);

        ItemStack item = cure(player, zombieVillager);

        Assertions.assertEquals(2, item.getAmount(), "One pill must have been consumed");
        Mockito.verify(zombieVillager).setConversionTime(1);
        Mockito.verify(zombieVillager).setConversionPlayer(player);
    }
}
