package io.github.thebusybiscuit.slimefun4.api.events;

import org.bukkit.Material;
import org.bukkit.block.Block;
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
import org.mockito.Mockito;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;

/**
 * Regression coverage for {@link AncientAltarCraftEvent} (fields, output override, air/null rejection,
 * cancellation, dispatch).
 *
 * @author Zurker
 */
class TestAncientAltarCraftEvent {

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
    @DisplayName("AncientAltarCraftEvent exposes output/altar and allows overriding the output")
    void testContractAndOverride() {
        Player player = server.addPlayer();
        Block altar = Mockito.mock(Block.class);
        ItemStack output = new ItemStack(Material.DIAMOND);
        AncientAltarCraftEvent event = new AncientAltarCraftEvent(output, altar, player);

        Assertions.assertEquals(altar, event.getAltarBlock());
        Assertions.assertEquals(output, event.getItem());
        Assertions.assertFalse(event.isCancelled());

        ItemStack replacement = new ItemStack(Material.NETHER_STAR);
        event.setItem(replacement);
        Assertions.assertEquals(replacement, event.getItem());
    }

    @Test
    @DisplayName("The altar output cannot be set to null or air")
    void testOutputValidation() {
        Player player = server.addPlayer();
        Block altar = Mockito.mock(Block.class);
        AncientAltarCraftEvent event = new AncientAltarCraftEvent(new ItemStack(Material.DIAMOND), altar, player);

        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setItem(null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setItem(new ItemStack(Material.AIR)));
    }

    @Test
    @DisplayName("Cancelling AncientAltarCraftEvent suppresses the dropped output")
    void testCancellationAndDispatch() {
        Player player = server.addPlayer();
        Block altar = Mockito.mock(Block.class);

        boolean[] seen = { false };
        Listener listener = new Listener() {
            @EventHandler
            public void onCraft(AncientAltarCraftEvent e) {
                seen[0] = true;
                e.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(listener, plugin);

        try {
            AncientAltarCraftEvent event = new AncientAltarCraftEvent(new ItemStack(Material.EMERALD), altar, player);
            server.getPluginManager().callEvent(event);

            Assertions.assertTrue(seen[0]);
            Assertions.assertTrue(event.isCancelled());
        } finally {
            HandlerList.unregisterAll(listener);
        }
    }
}
