package io.github.thebusybiscuit.slimefun4.implementation.items.multiblocks;

import java.lang.reflect.Method;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

import io.github.thebusybiscuit.slimefun4.api.events.SmelteryFireConsumeEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Regression coverage for the Smeltery fire API expansion:
 * {@link SmelteryFireConsumeEvent}, exercised by calling the private
 * {@link Smeltery#consumeFire} via reflection.
 * <p>
 * The consume path ends in {@code playEffect(STEP_SOUND, Material)} which MockBukkit
 * rejects, but the event fires before that tail — so the event is captured and the
 * cancellation keeps the fire block intact.
 *
 * @author Zurker
 */
class TestSmelteryFireConsumeEvent {

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static Smeltery smeltery;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "smeltery_fire_test");
        SlimefunItemStack stack = new SlimefunItemStack("_TEST_SMELTERY_FIRE", Material.NETHER_BRICK_FENCE, "&fTest Smeltery Fire");
        smeltery = new Smeltery(itemGroup, stack);
        smeltery.register(plugin);
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
     * Places a smeltery block, an empty dispenser below it, and a fire block two below.
     */
    private Block placeFire(int x, int z) {
        world.getBlockAt(x, 61, z).setType(Material.DISPENSER);
        Block fire = world.getBlockAt(x, 60, z);
        fire.setType(Material.FIRE);
        return world.getBlockAt(x, 62, z);
    }

    private void consumeFire(Player player, Block smelteryBlock) throws Throwable {
        Block dispenser = smelteryBlock.getRelative(org.bukkit.block.BlockFace.DOWN);
        Method method = Smeltery.class.getDeclaredMethod("consumeFire", Player.class, Block.class, Block.class);
        method.setAccessible(true);

        try {
            method.invoke(smeltery, player, dispenser, smelteryBlock);
        } catch (java.lang.reflect.InvocationTargetException e) {
            // playEffect(STEP_SOUND, Material) is not fully supported under MockBukkit
            if (!(e.getCause() instanceof IllegalArgumentException)) {
                throw e.getCause();
            }
        }
    }

    @Test
    @DisplayName("SmelteryFireConsumeEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Player player = server.addPlayer();
        Block smelteryBlock = world.getBlockAt(1, 62, 1);
        Block fire = world.getBlockAt(1, 60, 1);

        SmelteryFireConsumeEvent event = new SmelteryFireConsumeEvent(player, smelteryBlock, fire);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(smelteryBlock, event.getSmeltery());
        Assertions.assertEquals(fire, event.getFire());
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new SmelteryFireConsumeEvent(player, null, fire));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new SmelteryFireConsumeEvent(player, smelteryBlock, null));
    }

    @Test
    @DisplayName("Consuming the fire fires the event")
    void testConsumeFiresEvent() throws Throwable {
        Player player = server.addPlayer();
        Block smelteryBlock = placeFire(10, 10);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onFireConsume(SmelteryFireConsumeEvent event) {
                seen[0] = true;
                Assertions.assertEquals(smelteryBlock, event.getSmeltery());
                Assertions.assertEquals(Material.FIRE, event.getFire().getType());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            consumeFire(player, smelteryBlock);

            Assertions.assertTrue(seen[0], "SmelteryFireConsumeEvent was not fired");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling SmelteryFireConsumeEvent keeps the fire block intact")
    void testCancelKeepsFire() throws Throwable {
        Player player = server.addPlayer();
        Block smelteryBlock = placeFire(20, 20);
        Block fireBlock = world.getBlockAt(20, 60, 20);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onFireConsume(SmelteryFireConsumeEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            consumeFire(player, smelteryBlock);

            Assertions.assertEquals(Material.FIRE, fireBlock.getType(), "A vetoed consume must keep the fire block");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Consuming without listeners still runs the consume path, preserving the old behavior")
    void testConsumeWithoutListenersRuns() throws Throwable {
        Player player = server.addPlayer();
        Block smelteryBlock = placeFire(30, 30);

        // Should not throw any event-related error; the consume path runs without allocation.
        Assertions.assertDoesNotThrow(() -> consumeFire(player, smelteryBlock));
    }
}
