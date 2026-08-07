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

import io.github.bakedlibs.dough.common.ChatColors;
import io.github.thebusybiscuit.slimefun4.api.events.HologramProjectorTextChangeEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

import me.mrCookieSlime.Slimefun.api.BlockStorage;

/**
 * Regression coverage for the hologram projector API expansion:
 * {@link HologramProjectorTextChangeEvent}, exercised by driving the real
 * {@link HologramProjector#updateText} text application path.
 * <p>
 * The text is submitted through chat input which cannot be simulated under MockBukkit,
 * so the tests drive the extracted application method directly. The outcome is asserted
 * end-to-end through the stored BlockStorage text and the hologram armor stand, which is
 * pre-registered per projector: MockBukkit does not implement
 * {@code LivingEntityMock#setRemoveWhenFarAway}, so the spawn path of
 * {@code ArmorStandUtils} cannot run.
 *
 * @author Zurker
 */
class TestHologramProjectorTextChangeEvent {

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static HologramProjector projector;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "hologram_projector_test");
        SlimefunItemStack stack = new SlimefunItemStack("_TEST_HOLOGRAM_PROJECTOR", Material.QUARTZ_BLOCK, "&7Test Hologram Projector");
        Slimefun.getItemCfg().setValue("_TEST_HOLOGRAM_PROJECTOR.enabled", true);
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
     * matching hologram armor stand. The stand is pre-registered because MockBukkit
     * does not implement {@code LivingEntityMock#setRemoveWhenFarAway}, which the
     * spawn path of {@code ArmorStandUtils} calls; finding an existing stand avoids it.
     */
    private Block placeProjector(int x, int z) {
        Block b = world.getBlockAt(x, 60, z);
        b.setType(Material.QUARTZ_BLOCK);
        BlockStorage.addBlockInfo(b, "id", "_TEST_HOLOGRAM_PROJECTOR");
        BlockStorage.addBlockInfo(b, "text", "&7Old Text");
        BlockStorage.addBlockInfo(b, "offset", "0.5");

        ArmorStandMock stand = new ArmorStandMock(server, UUID.randomUUID());
        stand.setLocation(new Location(world, x + 0.5, 60.5, z + 0.5));
        stand.setCustomName("&7Old Text");
        server.registerEntity(stand);

        return b;
    }

    private String storedText(Block b) {
        return BlockStorage.getLocationInfo(b.getLocation(), "text");
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
    @DisplayName("HologramProjectorTextChangeEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Player player = server.addPlayer();
        Block b = placeProjector(1, 1);

        HologramProjectorTextChangeEvent event = new HologramProjectorTextChangeEvent(player, projector, b, "&7Old Text", "&aNew Text");

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(projector, event.getProjector());
        Assertions.assertEquals(b, event.getBlock());
        Assertions.assertEquals("&7Old Text", event.getPreviousText());
        Assertions.assertEquals("&aNew Text", event.getNewText());
        Assertions.assertFalse(event.isCancelled());

        event.setNewText("&cRewritten");
        Assertions.assertEquals("&cRewritten", event.getNewText());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new HologramProjectorTextChangeEvent(player, null, b, "&7Old Text", "&aNew Text"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new HologramProjectorTextChangeEvent(player, projector, null, "&7Old Text", "&aNew Text"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new HologramProjectorTextChangeEvent(player, projector, b, null, "&aNew Text"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new HologramProjectorTextChangeEvent(player, projector, b, "&7Old Text", null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setNewText(null));
    }

    @Test
    @DisplayName("Submitting a new text fires the event and applies it to the hologram")
    void testChangeFiresEventAndApplies() {
        Player player = server.addPlayer();
        Block b = placeProjector(100, 100);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onTextChange(HologramProjectorTextChangeEvent event) {
                seen[0] = true;
                Assertions.assertEquals(player, event.getPlayer());
                Assertions.assertEquals(projector, event.getProjector());
                Assertions.assertEquals(b, event.getBlock());
                Assertions.assertEquals("&7Old Text", event.getPreviousText());
                Assertions.assertEquals("&aNew Text", event.getNewText());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            projector.updateText(player, b, "&aNew Text");

            Assertions.assertTrue(seen[0], "HologramProjectorTextChangeEvent was not fired");
            Assertions.assertEquals(ChatColors.color("&aNew Text"), storedText(b), "The stored text must have been updated");

            List<ArmorStand> stands = hologramsNear(b);
            Assertions.assertEquals(1, stands.size(), "Exactly one hologram must exist");
            Assertions.assertEquals(ChatColors.color("&aNew Text"), stands.get(0).getCustomName(), "The hologram must show the new text");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling HologramProjectorTextChangeEvent keeps the old text")
    void testCancelKeepsOldText() {
        Player player = server.addPlayer();
        Block b = placeProjector(200, 200);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onTextChange(HologramProjectorTextChangeEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            projector.updateText(player, b, "&aNew Text");

            Assertions.assertEquals("&7Old Text", storedText(b), "A vetoed change must keep the old text");

            List<ArmorStand> stands = hologramsNear(b);
            Assertions.assertEquals(1, stands.size(), "A vetoed change must not remove the hologram");
            Assertions.assertEquals("&7Old Text", stands.get(0).getCustomName(), "A vetoed change must not rename the hologram");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Submitting without listeners still applies the text, preserving the old behavior")
    void testChangeWithoutListenersApplies() {
        Player player = server.addPlayer();
        Block b = placeProjector(300, 300);

        projector.updateText(player, b, "&aNew Text");

        Assertions.assertEquals(ChatColors.color("&aNew Text"), storedText(b), "The stored text must have been updated");
    }

    @Test
    @DisplayName("Rewriting the text via setNewText applies the rewritten text")
    void testRewriteAppliesRewrittenText() {
        Player player = server.addPlayer();
        Block b = placeProjector(400, 400);

        Listener rewriting = new Listener() {
            @EventHandler
            public void onTextChange(HologramProjectorTextChangeEvent event) {
                event.setNewText("&cCensored");
            }
        };
        server.getPluginManager().registerEvents(rewriting, plugin);

        try {
            projector.updateText(player, b, "&aNew Text");

            Assertions.assertEquals(ChatColors.color("&cCensored"), storedText(b), "The rewritten text must have been applied");

            List<ArmorStand> stands = hologramsNear(b);
            Assertions.assertEquals(1, stands.size(), "Exactly one hologram must exist");
            Assertions.assertEquals(ChatColors.color("&cCensored"), stands.get(0).getCustomName(), "The hologram must show the rewritten text");
        } finally {
            HandlerList.unregisterAll(rewriting);
        }
    }

    @Test
    @DisplayName("Repeated text changes reuse the same hologram instead of spawning duplicates")
    void testRepeatedChangesReuseHologram() {
        Player player = server.addPlayer();
        Block b = placeProjector(500, 500);

        projector.updateText(player, b, "&aFirst");
        projector.updateText(player, b, "&eSecond");

        Assertions.assertEquals(ChatColors.color("&eSecond"), storedText(b), "The latest text must have been stored");

        List<ArmorStand> stands = hologramsNear(b);
        Assertions.assertEquals(1, stands.size(), "The hologram must have been reused, not duplicated");
        Assertions.assertEquals(ChatColors.color("&eSecond"), stands.get(0).getCustomName(), "The hologram must show the latest text");
    }
}
