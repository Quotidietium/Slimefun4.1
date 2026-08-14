package io.github.thebusybiscuit.slimefun4.api.events;

import org.bukkit.Material;
import org.bukkit.entity.Player;
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

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

import io.github.thebusybiscuit.slimefun4.core.guide.SlimefunGuideMode;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;

/**
 * Regression coverage for {@link SlimefunGuideOpenEvent} (fields, null-validation, layout override,
 * cancellation, dispatch).
 *
 * @author Zurker
 */
class TestSlimefunGuideOpenEvent {

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

    @Test
    @DisplayName("SlimefunGuideOpenEvent exposes player/guide/layout and validates nulls")
    void testContract() {
        Player player = server.addPlayer();
        ItemStack guide = new ItemStack(Material.ENCHANTED_BOOK);
        SlimefunGuideOpenEvent event = new SlimefunGuideOpenEvent(player, guide, SlimefunGuideMode.SURVIVAL_MODE);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(guide, event.getGuide());
        Assertions.assertEquals(SlimefunGuideMode.SURVIVAL_MODE, event.getGuideLayout());
        Assertions.assertFalse(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new SlimefunGuideOpenEvent(null, guide, SlimefunGuideMode.SURVIVAL_MODE));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new SlimefunGuideOpenEvent(player, null, SlimefunGuideMode.SURVIVAL_MODE));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new SlimefunGuideOpenEvent(player, guide, null));
    }

    @Test
    @DisplayName("The guide layout can be overridden but not to null")
    void testLayoutOverride() {
        Player player = server.addPlayer();
        ItemStack guide = new ItemStack(Material.ENCHANTED_BOOK);
        SlimefunGuideOpenEvent event = new SlimefunGuideOpenEvent(player, guide, SlimefunGuideMode.SURVIVAL_MODE);

        event.setGuideLayout(SlimefunGuideMode.CHEAT_MODE);
        Assertions.assertEquals(SlimefunGuideMode.CHEAT_MODE, event.getGuideLayout());

        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setGuideLayout(null));
    }

    @Test
    @DisplayName("Cancelling and dispatching SlimefunGuideOpenEvent")
    void testCancellationAndDispatch() {
        Player player = server.addPlayer();
        ItemStack guide = new ItemStack(Material.ENCHANTED_BOOK);

        boolean[] seen = { false };
        Listener listener = new Listener() {
            @EventHandler
            public void onOpen(SlimefunGuideOpenEvent e) {
                seen[0] = true;
                e.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(listener, plugin);

        try {
            SlimefunGuideOpenEvent event = new SlimefunGuideOpenEvent(player, guide, SlimefunGuideMode.SURVIVAL_MODE);
            server.getPluginManager().callEvent(event);

            Assertions.assertTrue(seen[0]);
            Assertions.assertTrue(event.isCancelled());
        } finally {
            HandlerList.unregisterAll(listener);
        }
    }
}
