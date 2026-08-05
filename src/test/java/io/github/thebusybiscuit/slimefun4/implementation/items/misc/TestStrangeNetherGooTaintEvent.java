package io.github.thebusybiscuit.slimefun4.implementation.items.misc;

import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

import io.github.thebusybiscuit.slimefun4.api.events.StrangeNetherGooTaintEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.ItemHandler;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.EntityInteractHandler;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Regression coverage for the strange nether goo API expansion: {@link StrangeNetherGooTaintEvent},
 * exercised by driving the real {@link StrangeNetherGoo} {@link EntityInteractHandler} with a
 * constructed {@link PlayerInteractEntityEvent}.
 *
 * @author Zurker
 */
class TestStrangeNetherGooTaintEvent {

    private static ServerMock server;
    private static Slimefun plugin;

    private static StrangeNetherGoo goo;
    private static EntityInteractHandler entityInteractHandler;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "nether_goo_test");
        SlimefunItemStack stack = new SlimefunItemStack("_TEST_NETHER_GOO", Material.PURPLE_DYE, "&5Test Strange Nether Goo");
        Slimefun.getItemCfg().setValue("_TEST_NETHER_GOO.enabled", true);
        goo = new StrangeNetherGoo(itemGroup, stack, RecipeType.NULL, new ItemStack[9]);
        goo.register(plugin);

        for (ItemHandler handler : goo.getHandlers()) {
            if (handler instanceof EntityInteractHandler eih) {
                entityInteractHandler = eih;
            }
        }

        Assertions.assertNotNull(entityInteractHandler, "The StrangeNetherGoo must expose an EntityInteractHandler");
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
     * Right-clicks the given sheep with three goo via the real handler, returning the used stack.
     */
    private ItemStack taint(Player player, Sheep sheep, PlayerInteractEntityEvent interactEventOut) {
        ItemStack item = goo.getItem().clone();
        item.setAmount(3);

        entityInteractHandler.onInteract(interactEventOut, item, false);
        return item;
    }

    private Sheep spawnSheep(Player player) {
        return (Sheep) player.getWorld().spawnEntity(player.getLocation().clone().add(1, 0, 0), EntityType.SHEEP);
    }

    @Test
    @DisplayName("StrangeNetherGooTaintEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Player player = server.addPlayer();
        Sheep sheep = spawnSheep(player);

        StrangeNetherGooTaintEvent event = new StrangeNetherGooTaintEvent(player, goo, sheep);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(goo, event.getGoo());
        Assertions.assertEquals(sheep, event.getSheep());
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new StrangeNetherGooTaintEvent(player, null, sheep));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new StrangeNetherGooTaintEvent(player, goo, null));
    }

    @Test
    @DisplayName("Tainting a sheep fires the event and taints the sheep")
    void testTaintFiresAndTaints() {
        Player player = server.addPlayer();
        Sheep sheep = spawnSheep(player);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onTaint(StrangeNetherGooTaintEvent event) {
                seen[0] = true;
                Assertions.assertEquals(goo, event.getGoo());
                Assertions.assertEquals(sheep, event.getSheep());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            PlayerInteractEntityEvent interactEvent = new PlayerInteractEntityEvent(player, sheep);
            ItemStack item = taint(player, sheep, interactEvent);

            Assertions.assertTrue(seen[0], "StrangeNetherGooTaintEvent was not fired");
            Assertions.assertEquals(2, item.getAmount(), "One goo must have been consumed");
            Assertions.assertTrue(sheep.hasPotionEffect(PotionEffectType.POISON), "The sheep must have been poisoned");
            Assertions.assertEquals(DyeColor.PURPLE, sheep.getColor(), "The sheep must have been dyed purple");
            Assertions.assertNotNull(sheep.getCustomName(), "The sheep must have been renamed");
            Assertions.assertTrue(interactEvent.isCancelled(), "The interaction must have been consumed");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling StrangeNetherGooTaintEvent keeps the goo, the sheep and the interaction")
    void testEventCancellationSkipsTaint() {
        Player player = server.addPlayer();
        Sheep sheep = spawnSheep(player);
        DyeColor colorBefore = sheep.getColor();

        Listener cancelling = new Listener() {
            @EventHandler
            public void onTaint(StrangeNetherGooTaintEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            PlayerInteractEntityEvent interactEvent = new PlayerInteractEntityEvent(player, sheep);
            ItemStack item = taint(player, sheep, interactEvent);

            Assertions.assertEquals(3, item.getAmount(), "A cancelled taint must keep the goo");
            Assertions.assertFalse(sheep.hasPotionEffect(PotionEffectType.POISON), "A cancelled taint must not poison the sheep");
            Assertions.assertEquals(colorBefore, sheep.getColor(), "A cancelled taint must keep the sheep color");
            Assertions.assertNull(sheep.getCustomName(), "A cancelled taint must not rename the sheep");
            Assertions.assertFalse(interactEvent.isCancelled(), "A cancelled taint must not consume the interaction");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("A named sheep neither fires the event nor gets tainted")
    void testNamedSheepDoesNothing() {
        Player player = server.addPlayer();
        Sheep sheep = spawnSheep(player);
        sheep.setCustomName("Dolly");

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onTaint(StrangeNetherGooTaintEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            PlayerInteractEntityEvent interactEvent = new PlayerInteractEntityEvent(player, sheep);
            ItemStack item = taint(player, sheep, interactEvent);

            Assertions.assertFalse(seen[0], "The event must not fire for an already named sheep");
            Assertions.assertEquals(3, item.getAmount(), "No goo must be consumed for a named sheep");
            Assertions.assertEquals("Dolly", sheep.getCustomName(), "The name must be untouched");
            Assertions.assertTrue(interactEvent.isCancelled(), "The interaction must still be consumed, as before");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Tainting without listeners still taints, preserving the old behavior")
    void testTaintWithoutListenersStillTaints() {
        Player player = server.addPlayer();
        Sheep sheep = spawnSheep(player);

        PlayerInteractEntityEvent interactEvent = new PlayerInteractEntityEvent(player, sheep);
        ItemStack item = taint(player, sheep, interactEvent);

        Assertions.assertEquals(2, item.getAmount(), "One goo must have been consumed");
        Assertions.assertEquals(DyeColor.PURPLE, sheep.getColor(), "The sheep must have been dyed purple");
        Assertions.assertNotNull(sheep.getCustomName(), "The sheep must have been renamed");
    }
}
