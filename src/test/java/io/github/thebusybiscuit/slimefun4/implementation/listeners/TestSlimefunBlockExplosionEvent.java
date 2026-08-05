package io.github.thebusybiscuit.slimefun4.implementation.listeners;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import org.bukkit.ExplosionResult;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
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

import io.github.thebusybiscuit.slimefun4.api.events.SlimefunBlockExplosionEvent;
import io.github.thebusybiscuit.slimefun4.api.events.SlimefunBlockExplosionEvent.Cause;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.core.attributes.WitherProof;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;
import io.github.thebusybiscuit.slimefun4.test.mocks.MockSlimefunItem;

import me.mrCookieSlime.Slimefun.api.BlockStorage;

/**
 * Regression coverage for the explosion protection API expansion:
 * {@link SlimefunBlockExplosionEvent}, exercised through the real {@link ExplosionsListener}
 * block-destruction path. {@code BlockStorage.clearBlockInfo} is asynchronous, so block
 * destruction is asserted via the material changing to AIR (a synchronous {@code setType}).
 *
 * @author Zurker
 */
class TestSlimefunBlockExplosionEvent {

    /**
     * A minimal {@link WitherProof} test item, to verify wither-proof blocks are excluded.
     */
    static class WitherProofMockItem extends MockSlimefunItem implements WitherProof {

        WitherProofMockItem(ItemGroup itemGroup, ItemStack item, String id) {
            super(itemGroup, item, id);
        }

        @Override
        public void onAttack(@Nonnull Block block, @Nonnull org.bukkit.entity.Wither wither) {
            // No-op
        }
    }

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static MockSlimefunItem plainItem;
    private static WitherProofMockItem witherProofItem;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        // Unit test startups do not register the listeners, register it manually
        new ExplosionsListener(plugin);

        // Non-configurable items stay DISABLED unless Items.yml says otherwise, enable them first
        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "explosion_test");
        Slimefun.getItemCfg().setValue("TEST_EXPLOSION_BLOCK.enabled", true);
        plainItem = new MockSlimefunItem(itemGroup, new ItemStack(Material.DISPENSER), "TEST_EXPLOSION_BLOCK");
        plainItem.register(plugin);

        Slimefun.getItemCfg().setValue("TEST_EXPLOSION_WITHERPROOF.enabled", true);
        witherProofItem = new WitherProofMockItem(itemGroup, new ItemStack(Material.OBSIDIAN), "TEST_EXPLOSION_WITHERPROOF");
        witherProofItem.register(plugin);
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
     * Places the given item's block backed by {@link BlockStorage}.
     */
    private Block placeBlock(int x, int z, MockSlimefunItem item, Material material) {
        Block b = world.getBlockAt(x, 1, z);
        b.setType(material);
        BlockStorage.addBlockInfo(b, "id", item.getId(), true);
        return b;
    }

    /**
     * Detonates a block explosion over the given blocks through the real event pipeline.
     */
    private void blockExplode(List<Block> blocks) {
        Block source = world.getBlockAt(0, 0, 0);
        BlockExplodeEvent event = new BlockExplodeEvent(source, source.getState(), blocks, 1.0F, ExplosionResult.DESTROY);
        server.getPluginManager().callEvent(event);
    }

    /**
     * Detonates an entity explosion over the given blocks through the real event pipeline.
     */
    private void entityExplode(List<Block> blocks) {
        Entity entity = Mockito.mock(Entity.class);
        EntityExplodeEvent event = new EntityExplodeEvent(entity, new Location(world, 0, 0, 0), blocks, 1.0F, ExplosionResult.DESTROY);
        server.getPluginManager().callEvent(event);
    }

    @Test
    @DisplayName("SlimefunBlockExplosionEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Block b = world.getBlockAt(1, 1, 1);

        SlimefunBlockExplosionEvent event = new SlimefunBlockExplosionEvent(plainItem, b, Cause.BLOCK_EXPLOSION);

        Assertions.assertEquals(plainItem, event.getSlimefunItem());
        Assertions.assertEquals(b, event.getBlock());
        Assertions.assertEquals(Cause.BLOCK_EXPLOSION, event.getCause());
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new SlimefunBlockExplosionEvent(null, b, Cause.BLOCK_EXPLOSION));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new SlimefunBlockExplosionEvent(plainItem, null, Cause.BLOCK_EXPLOSION));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new SlimefunBlockExplosionEvent(plainItem, b, null));
    }

    @Test
    @DisplayName("A block explosion destroys a Slimefun block and fires the event")
    void testBlockExplosionDestroys() {
        Block b = placeBlock(10, 10, plainItem, Material.DISPENSER);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onExplode(SlimefunBlockExplosionEvent event) {
                seen[0] = true;
                Assertions.assertEquals(plainItem, event.getSlimefunItem());
                Assertions.assertEquals(b, event.getBlock());
                Assertions.assertEquals(Cause.BLOCK_EXPLOSION, event.getCause());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            blockExplode(new ArrayList<>(List.of(b)));

            Assertions.assertTrue(seen[0], "SlimefunBlockExplosionEvent was not fired");
            Assertions.assertEquals(Material.AIR, b.getType(), "The block must have been destroyed");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("An entity explosion fires the event with the entity cause")
    void testEntityExplosionCause() {
        Block b = placeBlock(20, 20, plainItem, Material.DISPENSER);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onExplode(SlimefunBlockExplosionEvent event) {
                seen[0] = true;
                Assertions.assertEquals(Cause.ENTITY_EXPLOSION, event.getCause());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            entityExplode(new ArrayList<>(List.of(b)));

            Assertions.assertTrue(seen[0], "SlimefunBlockExplosionEvent was not fired");
            Assertions.assertEquals(Material.AIR, b.getType(), "The block must have been destroyed");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling SlimefunBlockExplosionEvent protects the block")
    void testEventCancellationProtects() {
        Block b = placeBlock(30, 30, plainItem, Material.DISPENSER);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onExplode(SlimefunBlockExplosionEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            blockExplode(new ArrayList<>(List.of(b)));

            Assertions.assertEquals(Material.DISPENSER, b.getType(), "A cancelled explosion must leave the block intact");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("A wither-proof block fires no event and survives the explosion")
    void testWitherProofSurvives() {
        Block b = placeBlock(40, 40, witherProofItem, Material.OBSIDIAN);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onExplode(SlimefunBlockExplosionEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            blockExplode(new ArrayList<>(List.of(b)));

            Assertions.assertFalse(seen[0], "No event must be fired for a wither-proof block");
            Assertions.assertEquals(Material.OBSIDIAN, b.getType(), "A wither-proof block must survive the explosion");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Destruction without listeners still applies, preserving the old behavior")
    void testExplosionWithoutListenersStillDestroys() {
        Block b = placeBlock(50, 50, plainItem, Material.DISPENSER);

        blockExplode(new ArrayList<>(List.of(b)));

        Assertions.assertEquals(Material.AIR, b.getType(), "The block must have been destroyed");
    }

    @Test
    @DisplayName("A vanilla block fires no event")
    void testVanillaBlockFiresNothing() {
        Block b = world.getBlockAt(60, 1, 60);
        b.setType(Material.STONE);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onExplode(SlimefunBlockExplosionEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            blockExplode(new ArrayList<>(List.of(b)));

            Assertions.assertFalse(seen[0], "No event must be fired for a vanilla block");
            Assertions.assertEquals(Material.STONE, b.getType(), "A vanilla block is left to vanilla explosion handling");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }
}
