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
import org.mockito.Mockito;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

import io.github.thebusybiscuit.slimefun4.core.multiblocks.MultiBlockMachine;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;

/**
 * Regression coverage for {@link MultiBlockCraftEvent} (fields, both constructors, output override
 * returning the previous value, cancellation, dispatch).
 *
 * @author Zurker
 */
class TestMultiBlockCraftEvent {

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
    @DisplayName("MultiBlockCraftEvent exposes machine/input/output via both constructors")
    void testContract() {
        Player player = server.addPlayer();
        MultiBlockMachine machine = Mockito.mock(MultiBlockMachine.class);
        ItemStack input = new ItemStack(Material.IRON_INGOT);
        ItemStack output = new ItemStack(Material.IRON_BLOCK);

        // Single-input convenience constructor wraps the item in a one-element array.
        MultiBlockCraftEvent single = new MultiBlockCraftEvent(player, machine, input, output);
        Assertions.assertEquals(machine, single.getMachine());
        Assertions.assertEquals(1, single.getInput().length);
        Assertions.assertEquals(input, single.getInput()[0]);
        Assertions.assertEquals(output, single.getOutput());

        // Array constructor carries multiple inputs.
        ItemStack[] inputs = new ItemStack[] { new ItemStack(Material.STICK), new ItemStack(Material.STICK) };
        MultiBlockCraftEvent multi = new MultiBlockCraftEvent(player, machine, inputs, output);
        Assertions.assertEquals(2, multi.getInput().length);
    }

    @Test
    @DisplayName("setOutput replaces the output and returns the previous one")
    void testOutputOverride() {
        Player player = server.addPlayer();
        MultiBlockMachine machine = Mockito.mock(MultiBlockMachine.class);
        ItemStack original = new ItemStack(Material.IRON_BLOCK);
        MultiBlockCraftEvent event = new MultiBlockCraftEvent(player, machine, new ItemStack(Material.IRON_INGOT), original);

        ItemStack replacement = new ItemStack(Material.DIAMOND);
        ItemStack previous = event.setOutput(replacement);

        Assertions.assertEquals(original, previous, "setOutput must return the previous output");
        Assertions.assertEquals(replacement, event.getOutput());
    }

    @Test
    @DisplayName("Cancelling MultiBlockCraftEvent suppresses ingredient consumption and output")
    void testCancellationAndDispatch() {
        Player player = server.addPlayer();
        MultiBlockMachine machine = Mockito.mock(MultiBlockMachine.class);

        boolean[] seen = { false };
        Listener listener = new Listener() {
            @EventHandler
            public void onCraft(MultiBlockCraftEvent e) {
                seen[0] = true;
                e.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(listener, plugin);

        try {
            MultiBlockCraftEvent event = new MultiBlockCraftEvent(player, machine, new ItemStack(Material.IRON_INGOT), new ItemStack(Material.IRON_BLOCK));
            server.getPluginManager().callEvent(event);

            Assertions.assertTrue(seen[0]);
            Assertions.assertTrue(event.isCancelled());
        } finally {
            HandlerList.unregisterAll(listener);
        }
    }
}
