package io.github.thebusybiscuit.slimefun4.implementation.items.weapons;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
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
import org.mockito.Mockito;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

import io.github.thebusybiscuit.slimefun4.api.events.PlayerRightClickEvent;
import io.github.thebusybiscuit.slimefun4.api.events.SeismicAxeShockwaveEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Regression coverage for the seismic axe API expansion: {@link SeismicAxeShockwaveEvent},
 * exercised by driving the real {@link SeismicAxe}
 * {@link io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler} with a constructed
 * {@link PlayerRightClickEvent}.
 * <p>
 * MockBukkit's {@code LivingEntityMock.getLineOfSight} is unimplemented, so the player is a
 * Mockito mock whose line of sight returns real blocks of the test world. The wave's per-block
 * effect ({@code playEffect(STEP_SOUND, Material)}) is rejected by MockBukkit with an
 * {@link IllegalArgumentException}; reaching that tail proves the wave loop ran, so the helper
 * reports whether the tail was reached instead of asserting world state.
 *
 * @author Zurker
 */
class TestSeismicAxeShockwaveEvent {

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static SeismicAxe seismicAxe;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "seismic_axe_test");
        SlimefunItemStack stack = new SlimefunItemStack("_TEST_SEISMIC_AXE", Material.DIAMOND_AXE, "&bTest Seismic Axe");
        Slimefun.getItemCfg().setValue("_TEST_SEISMIC_AXE.enabled", true);
        seismicAxe = new SeismicAxe(itemGroup, stack, RecipeType.NULL, new ItemStack[9]);
        seismicAxe.register(plugin);
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
     * Slams the axe with a Mockito player whose line of sight is the given row of stone blocks.
     *
     * @return true if the wave loop reached a per-block effect (the playEffect tail), false if
     *         the wave was skipped
     */
    private boolean slam(int x, int z) {
        List<Block> blocks = new ArrayList<>();

        for (int i = 0; i < 6; i++) {
            Block b = world.getBlockAt(x + i, 4, z);
            b.setType(Material.STONE);
            blocks.add(b);
        }

        Player player = Mockito.mock(Player.class);
        Mockito.when(player.getLineOfSight(null, 10)).thenReturn(blocks);

        PlayerInteractEvent interactEvent = new PlayerInteractEvent(player, Action.RIGHT_CLICK_AIR, seismicAxe.getItem().clone(), null, null);

        try {
            seismicAxe.getItemHandler().onRightClick(new PlayerRightClickEvent(interactEvent));
            return false;
        } catch (IllegalArgumentException ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("Wrong kind of data")) {
                // MockBukkit rejects playEffect(STEP_SOUND, Material) - see class javadoc
                return true;
            }

            throw ex;
        }
    }

    @Test
    @DisplayName("SeismicAxeShockwaveEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Player player = server.addPlayer();

        SeismicAxeShockwaveEvent event = new SeismicAxeShockwaveEvent(player, seismicAxe, new ArrayList<>());

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(seismicAxe, event.getSeismicAxe());
        Assertions.assertTrue(event.getBlocks().isEmpty());
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new SeismicAxeShockwaveEvent(player, null, new ArrayList<>()));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new SeismicAxeShockwaveEvent(player, seismicAxe, null));
    }

    @Test
    @DisplayName("Slamming the axe fires the event with the line-of-sight blocks and runs the wave")
    void testSlamFiresEvent() {
        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onShockwave(SeismicAxeShockwaveEvent event) {
                seen[0] = true;
                Assertions.assertEquals(seismicAxe, event.getSeismicAxe());
                Assertions.assertEquals(6, event.getBlocks().size(), "The whole line of sight must be included");
                Assertions.assertEquals(world.getBlockAt(12, 4, 10), event.getBlocks().get(2));
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            boolean waveRan = slam(10, 10);

            Assertions.assertTrue(seen[0], "SeismicAxeShockwaveEvent was not fired");
            Assertions.assertTrue(waveRan, "The wave loop must have reached a per-block effect");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling SeismicAxeShockwaveEvent skips the whole wave")
    void testEventCancellationSkipsWave() {
        Listener cancelling = new Listener() {
            @EventHandler
            public void onShockwave(SeismicAxeShockwaveEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            boolean waveRan = slam(20, 20);

            Assertions.assertFalse(waveRan, "A cancelled slam must not run the wave");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Removing entries from getBlocks trims the wave's path")
    void testBlocksTrimming() {
        Listener trimming = new Listener() {
            @EventHandler
            public void onShockwave(SeismicAxeShockwaveEvent event) {
                event.getBlocks().clear();
            }
        };
        server.getPluginManager().registerEvents(trimming, plugin);

        try {
            boolean waveRan = slam(30, 30);

            Assertions.assertFalse(waveRan, "Clearing the line of sight must skip the wave loop");
        } finally {
            HandlerList.unregisterAll(trimming);
        }
    }

    @Test
    @DisplayName("Slamming the axe without listeners still runs the wave, preserving the old behavior")
    void testSlamWithoutListenersStillRunsWave() {
        boolean waveRan = slam(40, 40);

        Assertions.assertTrue(waveRan, "The wave loop must have reached a per-block effect");
    }
}
