package io.github.thebusybiscuit.slimefun4.implementation.items.weapons;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
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

import io.github.thebusybiscuit.slimefun4.api.events.SwordOfBeheadingDropEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Regression coverage for the sword of beheading API expansion:
 * {@link SwordOfBeheadingDropEvent}, exercised by driving the real {@link SwordOfBeheading}
 * {@link io.github.thebusybiscuit.slimefun4.core.handlers.EntityKillHandler} with a constructed
 * {@link EntityDeathEvent}.
 * <p>
 * Two swords are registered to make the beheading roll deterministic: one with a 100% chance
 * and one with a 0% chance.
 *
 * @author Zurker
 */
class TestSwordOfBeheadingDropEvent {

    private static ServerMock server;
    private static Slimefun plugin;

    private static SwordOfBeheading alwaysSword;
    private static SwordOfBeheading neverSword;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "beheading_sword_test");

        SlimefunItemStack alwaysStack = new SlimefunItemStack("_TEST_BEHEADING_SWORD_ALWAYS", Material.DIAMOND_SWORD, "&cTest Sword of Beheading");
        Slimefun.getItemCfg().setValue("_TEST_BEHEADING_SWORD_ALWAYS.enabled", true);
        Slimefun.getItemCfg().setValue("_TEST_BEHEADING_SWORD_ALWAYS.chance.ZOMBIE", 100);
        alwaysSword = new SwordOfBeheading(itemGroup, alwaysStack, RecipeType.NULL, new ItemStack[9]);
        alwaysSword.register(plugin);

        SlimefunItemStack neverStack = new SlimefunItemStack("_TEST_BEHEADING_SWORD_NEVER", Material.DIAMOND_SWORD, "&cTest Sword of Beheading");
        Slimefun.getItemCfg().setValue("_TEST_BEHEADING_SWORD_NEVER.enabled", true);
        Slimefun.getItemCfg().setValue("_TEST_BEHEADING_SWORD_NEVER.chance.ZOMBIE", 0);
        neverSword = new SwordOfBeheading(itemGroup, neverStack, RecipeType.NULL, new ItemStack[9]);
        neverSword.register(plugin);
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
     * Kills a Mockito zombie with the given sword via the real handler, returning the drops.
     */
    private List<ItemStack> killZombie(Player player, SwordOfBeheading sword, Zombie zombie) {
        List<ItemStack> drops = new ArrayList<>();
        EntityDeathEvent deathEvent = new EntityDeathEvent(zombie, Mockito.mock(org.bukkit.damage.DamageSource.class), drops);
        sword.getItemHandler().onKill(deathEvent, zombie, player, sword.getItem());
        return drops;
    }

    private Zombie mockZombie() {
        Zombie zombie = Mockito.mock(Zombie.class);
        Mockito.when(zombie.getType()).thenReturn(EntityType.ZOMBIE);
        return zombie;
    }

    @Test
    @DisplayName("SwordOfBeheadingDropEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Player player = server.addPlayer();
        Zombie zombie = mockZombie();
        ItemStack head = new ItemStack(Material.ZOMBIE_HEAD);

        SwordOfBeheadingDropEvent event = new SwordOfBeheadingDropEvent(player, alwaysSword, zombie, head);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(alwaysSword, event.getSword());
        Assertions.assertEquals(zombie, event.getEntity());
        Assertions.assertEquals(head, event.getHead());
        Assertions.assertFalse(event.isCancelled());

        ItemStack swapped = new ItemStack(Material.CREEPER_HEAD);
        event.setHead(swapped);
        Assertions.assertEquals(swapped, event.getHead());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new SwordOfBeheadingDropEvent(player, null, zombie, head));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new SwordOfBeheadingDropEvent(player, alwaysSword, null, head));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new SwordOfBeheadingDropEvent(player, alwaysSword, zombie, null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setHead(null));
    }

    @Test
    @DisplayName("A successful beheading roll fires the event and drops the head")
    void testKillFiresAndDrops() {
        Player player = server.addPlayer();
        Zombie zombie = mockZombie();

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onDrop(SwordOfBeheadingDropEvent event) {
                seen[0] = true;
                Assertions.assertEquals(alwaysSword, event.getSword());
                Assertions.assertEquals(zombie, event.getEntity());
                Assertions.assertEquals(Material.ZOMBIE_HEAD, event.getHead().getType());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            List<ItemStack> drops = killZombie(player, alwaysSword, zombie);

            Assertions.assertTrue(seen[0], "SwordOfBeheadingDropEvent was not fired");
            Assertions.assertEquals(1, drops.size(), "The head must have been dropped");
            Assertions.assertEquals(Material.ZOMBIE_HEAD, drops.get(0).getType());
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling SwordOfBeheadingDropEvent skips the head drop")
    void testEventCancellationSkipsDrop() {
        Player player = server.addPlayer();

        Listener cancelling = new Listener() {
            @EventHandler
            public void onDrop(SwordOfBeheadingDropEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            List<ItemStack> drops = killZombie(player, alwaysSword, mockZombie());

            Assertions.assertTrue(drops.isEmpty(), "A cancelled beheading must not drop the head");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Swapping the head via setHead replaces the dropped head")
    void testHeadSwap() {
        Player player = server.addPlayer();

        Listener swapping = new Listener() {
            @EventHandler
            public void onDrop(SwordOfBeheadingDropEvent event) {
                event.setHead(new ItemStack(Material.DRAGON_HEAD));
            }
        };
        server.getPluginManager().registerEvents(swapping, plugin);

        try {
            List<ItemStack> drops = killZombie(player, alwaysSword, mockZombie());

            Assertions.assertEquals(1, drops.size());
            Assertions.assertEquals(Material.DRAGON_HEAD, drops.get(0).getType(), "The swapped head must be dropped");
        } finally {
            HandlerList.unregisterAll(swapping);
        }
    }

    @Test
    @DisplayName("A failed beheading roll neither fires the event nor drops a head")
    void testFailedRollDoesNothing() {
        Player player = server.addPlayer();

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onDrop(SwordOfBeheadingDropEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            List<ItemStack> drops = killZombie(player, neverSword, mockZombie());

            Assertions.assertFalse(seen[0], "The event must not fire when the roll fails");
            Assertions.assertTrue(drops.isEmpty(), "No head must drop when the roll fails");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Killing without listeners still drops the head, preserving the old behavior")
    void testKillWithoutListenersStillDrops() {
        Player player = server.addPlayer();

        List<ItemStack> drops = killZombie(player, alwaysSword, mockZombie());

        Assertions.assertEquals(1, drops.size(), "The head must have been dropped");
        Assertions.assertEquals(Material.ZOMBIE_HEAD, drops.get(0).getType());
    }
}
