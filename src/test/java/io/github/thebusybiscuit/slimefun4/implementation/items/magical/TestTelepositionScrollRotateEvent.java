package io.github.thebusybiscuit.slimefun4.implementation.items.magical;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Cow;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

import io.github.thebusybiscuit.slimefun4.api.events.PlayerRightClickEvent;
import io.github.thebusybiscuit.slimefun4.api.events.TelepositionScrollRotateEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Regression coverage for the teleposition scroll API expansion:
 * {@link TelepositionScrollRotateEvent}, exercised by driving the real
 * {@link TelepositionScroll} {@link io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler}
 * with a constructed {@link PlayerRightClickEvent}.
 *
 * @author Zurker
 */
class TestTelepositionScrollRotateEvent {

    private static ServerMock server;
    private static Slimefun plugin;

    private static TelepositionScroll scroll;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "teleposition_scroll_test");
        SlimefunItemStack stack = new SlimefunItemStack("_TEST_TELEPOSITION_SCROLL", Material.PAPER, "&6Test Teleposition Scroll");
        Slimefun.getItemCfg().setValue("_TEST_TELEPOSITION_SCROLL.enabled", true);
        scroll = new TelepositionScroll(itemGroup, stack, RecipeType.NULL, new ItemStack[9]);
        scroll.register(plugin);
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
     * Spawns a cow right next to the player and uses the scroll via the real handler.
     */
    private Cow useScrollNearCow(Player player) {
        Location cowLoc = player.getLocation().clone().add(1, 0, 0);
        Cow cow = (Cow) player.getWorld().spawnEntity(cowLoc, EntityType.COW);

        PlayerInteractEvent interactEvent = new PlayerInteractEvent(player, Action.RIGHT_CLICK_AIR, scroll.getItem().clone(), null, null);
        PlayerRightClickEvent event = new PlayerRightClickEvent(interactEvent);
        scroll.getItemHandler().onRightClick(event);

        return cow;
    }

    private static float expectedYaw(float initialYaw) {
        float yaw = initialYaw + 180F;
        return yaw > 360F ? yaw - 360F : yaw;
    }

    @Test
    @DisplayName("TelepositionScrollRotateEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Player player = server.addPlayer();
        Cow cow = (Cow) player.getWorld().spawnEntity(player.getLocation(), EntityType.COW);

        TelepositionScrollRotateEvent event = new TelepositionScrollRotateEvent(player, scroll, cow, 180F);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(scroll, event.getScroll());
        Assertions.assertEquals(cow, event.getEntity());
        Assertions.assertEquals(180F, event.getNewYaw());
        Assertions.assertFalse(event.isCancelled());

        event.setNewYaw(45F);
        Assertions.assertEquals(45F, event.getNewYaw());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new TelepositionScrollRotateEvent(player, null, cow, 180F));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new TelepositionScrollRotateEvent(player, scroll, null, 180F));
    }

    @Test
    @DisplayName("Using the scroll fires the event per entity and rotates it by 180 degrees")
    void testUseFiresAndRotates() {
        Player player = server.addPlayer();

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onRotate(TelepositionScrollRotateEvent event) {
                if (event.getEntity() instanceof Cow) {
                    seen[0] = true;
                    Assertions.assertEquals(scroll, event.getScroll());
                }
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            float before = expectedYawOfNewCow(player);
            Cow cow = useScrollNearCow(player);
            float expected = expectedYaw(before);

            Assertions.assertTrue(seen[0], "TelepositionScrollRotateEvent was not fired");
            Assertions.assertEquals(expected, cow.getLocation().getYaw(), 0.001, "The cow must have been rotated by 180 degrees");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    /**
     * The scroll rotates every matching nearby entity, so read the yaw of a freshly spawned
     * reference cow that has not been scrolled yet.
     */
    private float expectedYawOfNewCow(Player player) {
        Cow reference = (Cow) player.getWorld().spawnEntity(player.getLocation().clone().add(0, 0, 1), EntityType.COW);
        float yaw = reference.getLocation().getYaw();
        reference.remove();
        return yaw;
    }

    @Test
    @DisplayName("Cancelling TelepositionScrollRotateEvent leaves the entity facing its old direction")
    void testEventCancellationSkipsRotation() {
        Player player = server.addPlayer();

        Listener cancelling = new Listener() {
            @EventHandler
            public void onRotate(TelepositionScrollRotateEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            float before = expectedYawOfNewCow(player);
            Cow cow = useScrollNearCow(player);

            Assertions.assertEquals(before, cow.getLocation().getYaw(), 0.001, "A cancelled rotation must keep the old yaw");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Overriding the yaw via setNewYaw rotates the entity to the custom yaw")
    void testYawOverride() {
        Player player = server.addPlayer();

        Listener overriding = new Listener() {
            @EventHandler
            public void onRotate(TelepositionScrollRotateEvent event) {
                event.setNewYaw(45F);
            }
        };
        server.getPluginManager().registerEvents(overriding, plugin);

        try {
            Cow cow = useScrollNearCow(player);

            Assertions.assertEquals(45F, cow.getLocation().getYaw(), 0.001, "The cow must face the overridden yaw");
        } finally {
            HandlerList.unregisterAll(overriding);
        }
    }

    @Test
    @DisplayName("Using the scroll without listeners still rotates, preserving the old behavior")
    void testUseWithoutListenersStillRotates() {
        Player player = server.addPlayer();

        float before = expectedYawOfNewCow(player);
        Cow cow = useScrollNearCow(player);

        Assertions.assertEquals(expectedYaw(before), cow.getLocation().getYaw(), 0.001, "The cow must have been rotated by 180 degrees");
    }
}
