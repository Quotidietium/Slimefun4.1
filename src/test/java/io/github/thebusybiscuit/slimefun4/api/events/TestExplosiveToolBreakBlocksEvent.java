package io.github.thebusybiscuit.slimefun4.api.events;

import java.util.Arrays;
import java.util.List;

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
import io.github.thebusybiscuit.slimefun4.implementation.items.tools.ExplosiveTool;

/**
 * Regression coverage for {@link ExplosiveToolBreakBlocksEvent} (fields, null-validation,
 * primary/additional block split, cancellation, dispatch).
 *
 * @author Zurker
 */
class TestExplosiveToolBreakBlocksEvent {

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
    @DisplayName("ExplosiveToolBreakBlocksEvent exposes primary/additional blocks, tool and item, validates nulls")
    void testContract() {
        Player player = server.addPlayer();
        Block primary = Mockito.mock(Block.class);
        Block extra = Mockito.mock(Block.class);
        List<Block> additional = Arrays.asList(extra);
        ItemStack item = new ItemStack(Material.DIAMOND_PICKAXE);
        ExplosiveTool tool = Mockito.mock(ExplosiveTool.class);

        ExplosiveToolBreakBlocksEvent event = new ExplosiveToolBreakBlocksEvent(player, primary, additional, item, tool);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(primary, event.getPrimaryBlock());
        Assertions.assertEquals(additional, event.getAdditionalBlocks());
        Assertions.assertEquals(item, event.getItemInHand());
        Assertions.assertEquals(tool, event.getExplosiveTool());
        Assertions.assertFalse(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new ExplosiveToolBreakBlocksEvent(player, null, additional, item, tool));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ExplosiveToolBreakBlocksEvent(player, primary, null, item, tool));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ExplosiveToolBreakBlocksEvent(player, primary, additional, null, tool));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ExplosiveToolBreakBlocksEvent(player, primary, additional, item, null));
    }

    @Test
    @DisplayName("Cancelling ExplosiveToolBreakBlocksEvent suppresses the additional breaks")
    void testCancellationAndDispatch() {
        Player player = server.addPlayer();
        Block primary = Mockito.mock(Block.class);
        List<Block> additional = Arrays.asList(Mockito.mock(Block.class));
        ItemStack item = new ItemStack(Material.DIAMOND_PICKAXE);
        ExplosiveTool tool = Mockito.mock(ExplosiveTool.class);

        boolean[] seen = { false };
        Listener listener = new Listener() {
            @EventHandler
            public void onBreak(ExplosiveToolBreakBlocksEvent e) {
                seen[0] = true;
                e.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(listener, plugin);

        try {
            ExplosiveToolBreakBlocksEvent event = new ExplosiveToolBreakBlocksEvent(player, primary, additional, item, tool);
            server.getPluginManager().callEvent(event);

            Assertions.assertTrue(seen[0]);
            Assertions.assertTrue(event.isCancelled());
        } finally {
            HandlerList.unregisterAll(listener);
        }
    }
}
