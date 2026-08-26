package io.github.thebusybiscuit.slimefun4.implementation.items.blocks;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
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
import be.seeseemelk.mockbukkit.entity.ArmorStandMock;

import io.github.thebusybiscuit.slimefun4.api.events.HologramProjectorOffsetChangeEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

import me.mrCookieSlime.Slimefun.api.BlockStorage;

/**
 * Regression coverage for the hologram projector API expansion:
 * {@link HologramProjectorOffsetChangeEvent}, exercised by driving the real
 * {@link HologramProjector#updateOffset} offset application path.
 * <p>
 * The offset is adjusted through the projector's editor menu whose clicks cannot be
 * simulated under MockBukkit, so the tests drive the extracted application method
 * directly. The outcome is asserted end-to-end through the stored BlockStorage offset
 * and the hologram armor stand, which is pre-registered per projector: MockBukkit does
 * not implement {@code LivingEntityMock#setRemoveWhenFarAway}, so the spawn path of
 * {@code ArmorStandUtils} cannot run.
 *
 * @author Zurker
 */
class TestHologramProjectorOffsetChangeEvent {

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static HologramProjector projector;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "hologram_projector_offset_test");
        SlimefunItemStack stack = new SlimefunItemStack("_TEST_HOLOGRAM_PROJECTOR_OFFSET", Material.QUARTZ_BLOCK, "&7Test Hologram Projector");
        Slimefun.getItemCfg().setValue("_TEST_HOLOGRAM_PROJECTOR_OFFSET.enabled", true);
        projector = new HologramProjector(itemGroup, stack, RecipeType.NULL, new ItemStack[9], null);
        projector.register(plugin);
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
     * Places a projector block with its BlockStorage-backed text and offset, plus a
     * matching hologram armor stand floating at the current offset. The stand is
     * pre-registered because MockBukkit does not implement
     * {@code LivingEntityMock#setRemoveWhenFarAway}, which the spawn path of
     * {@code ArmorStandUtils} calls; finding an existing stand avoids it.
     */
    private Block placeProjector(org.bukkit.entity.Player owner, int x, int z) {
        Block b = world.getBlockAt(x, 60, z);
        b.setType(Material.QUARTZ_BLOCK);
        BlockStorage.addBlockInfo(b, "id", "_TEST_HOLOGRAM_PROJECTOR_OFFSET");
        BlockStorage.addBlockInfo(b, "text", "&7Old Text");
        BlockStorage.addBlockInfo(b, "offset", "0.5");
        BlockStorage.addBlockInfo(b, "owner", owner.getUniqueId().toString(), true);

        ArmorStandMock stand = new ArmorStandMock(server, UUID.randomUUID());
        stand.setLocation(new Location(world, x + 0.5, 60.5, z + 0.5));
        stand.setCustomName("&7Old Text");
        server.registerEntity(stand);

        return b;
    }

    private String storedOffset(Block b) {
        return BlockStorage.getLocationInfo(b.getLocation(), "offset");
    }

    /**
     * All hologram armor stands within two blocks of the projector.
     */
    private List<ArmorStand> hologramsNear(Block b) {
        List<ArmorStand> stands = new ArrayList<>();

        for (ArmorStand stand : b.getWorld().getEntitiesByClass(ArmorStand.class)) {
            if (stand.getLocation().distanceSquared(b.getLocation()) < 4) {
                stands.add(stand);
            }
        }

        return stands;
    }

    @Test
    @DisplayName("HologramProjectorOffsetChangeEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Player player = server.addPlayer();
        Block b = placeProjector(player, 1, 1);

        HologramProjectorOffsetChangeEvent event = new HologramProjectorOffsetChangeEvent(player, projector, b, 0.5, 0.6);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(projector, event.getProjector());
        Assertions.assertEquals(b, event.getBlock());
        Assertions.assertEquals(0.5, event.getPreviousOffset());
        Assertions.assertEquals(0.6, event.getNewOffset());
        Assertions.assertFalse(event.isCancelled());

        event.setNewOffset(1.2);
        Assertions.assertEquals(1.2, event.getNewOffset());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new HologramProjectorOffsetChangeEvent(player, null, b, 0.5, 0.6));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new HologramProjectorOffsetChangeEvent(player, projector, null, 0.5, 0.6));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new HologramProjectorOffsetChangeEvent(player, projector, b, Double.NaN, 0.6));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new HologramProjectorOffsetChangeEvent(player, projector, b, 0.5, Double.POSITIVE_INFINITY));
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setNewOffset(Double.NaN));
    }

    @Test
    @DisplayName("Adjusting the offset fires the event and moves the hologram")
    void testChangeFiresEventAndApplies() {
        Player player = server.addPlayer();
        Block b = placeProjector(player, 100, 100);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onOffsetChange(HologramProjectorOffsetChangeEvent event) {
                seen[0] = true;
                Assertions.assertEquals(player, event.getPlayer());
                Assertions.assertEquals(projector, event.getProjector());
                Assertions.assertEquals(b, event.getBlock());
                Assertions.assertEquals(0.5, event.getPreviousOffset());
                Assertions.assertEquals(0.6, event.getNewOffset());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            projector.updateOffset(player, b, 0.6);

            Assertions.assertTrue(seen[0], "HologramProjectorOffsetChangeEvent was not fired");
            Assertions.assertEquals("0.6", storedOffset(b), "The stored offset must have been updated");

            List<ArmorStand> stands = hologramsNear(b);
            Assertions.assertEquals(1, stands.size(), "Exactly one hologram must exist");
            Assertions.assertEquals(60.6, stands.get(0).getLocation().getY(), 1e-9, "The hologram must float at the new offset");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling HologramProjectorOffsetChangeEvent keeps the old offset")
    void testCancelKeepsOldOffset() {
        Player player = server.addPlayer();
        Block b = placeProjector(player, 200, 200);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onOffsetChange(HologramProjectorOffsetChangeEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            projector.updateOffset(player, b, 0.6);

            Assertions.assertEquals("0.5", storedOffset(b), "A vetoed adjustment must keep the old offset");

            List<ArmorStand> stands = hologramsNear(b);
            Assertions.assertEquals(1, stands.size(), "A vetoed adjustment must not remove the hologram");
            Assertions.assertEquals(60.5, stands.get(0).getLocation().getY(), 1e-9, "A vetoed adjustment must not move the hologram");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Adjusting without listeners still applies the offset, preserving the old behavior")
    void testChangeWithoutListenersApplies() {
        Player player = server.addPlayer();
        Block b = placeProjector(player, 300, 300);

        projector.updateOffset(player, b, 0.6);

        Assertions.assertEquals("0.6", storedOffset(b), "The stored offset must have been updated");
        Assertions.assertEquals(60.6, hologramsNear(b).get(0).getLocation().getY(), 1e-9, "The hologram must float at the new offset");
    }

    @Test
    @DisplayName("Overriding the offset via setNewOffset applies the override")
    void testOverrideAppliesOverriddenOffset() {
        Player player = server.addPlayer();
        Block b = placeProjector(player, 400, 400);

        Listener overriding = new Listener() {
            @EventHandler
            public void onOffsetChange(HologramProjectorOffsetChangeEvent event) {
                event.setNewOffset(1.2);
            }
        };
        server.getPluginManager().registerEvents(overriding, plugin);

        try {
            projector.updateOffset(player, b, 0.6);

            Assertions.assertEquals("1.2", storedOffset(b), "The overridden offset must have been stored");

            List<ArmorStand> stands = hologramsNear(b);
            Assertions.assertEquals(1, stands.size(), "Exactly one hologram must exist");
            Assertions.assertEquals(61.2, stands.get(0).getLocation().getY(), 1e-9, "The hologram must float at the overridden offset");
        } finally {
            HandlerList.unregisterAll(overriding);
        }
    }

    @Test
    @DisplayName("A downward adjustment moves the hologram down")
    void testDecreaseMovesHologramDown() {
        Player player = server.addPlayer();
        Block b = placeProjector(player, 500, 500);

        projector.updateOffset(player, b, 0.4);

        Assertions.assertEquals("0.4", storedOffset(b), "The stored offset must have been updated");
        Assertions.assertEquals(60.4, hologramsNear(b).get(0).getLocation().getY(), 1e-9, "The hologram must have moved down");
    }

    @Test
    @org.junit.jupiter.api.DisplayName("Adjusting after the projector was re-placed by someone else is rejected")
    void testEditOffsetAfterReplacedByOtherOwner() {
        org.bukkit.entity.Player originalOwner = server.addPlayer();
        org.bukkit.entity.Player newOwner = server.addPlayer();
        Block b = placeProjector(newOwner, 600, 600);

        boolean[] seen = { false };
        org.bukkit.event.Listener watcher = new org.bukkit.event.Listener() {
            @org.bukkit.event.EventHandler
            public void onOffset(HologramProjectorOffsetChangeEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            // The original owner's still-open editor clicks after the projector changed hands
            projector.updateOffset(originalOwner, b, 0.9);

            org.junit.jupiter.api.Assertions.assertFalse(seen[0], "HologramProjectorOffsetChangeEvent must not fire for a non-owner adjustment");
            org.junit.jupiter.api.Assertions.assertEquals("0.5", BlockStorage.getLocationInfo(b.getLocation(), "offset"), "A non-owner adjustment must not change the offset");
        } finally {
            org.bukkit.event.HandlerList.unregisterAll(watcher);
        }
    }
}
