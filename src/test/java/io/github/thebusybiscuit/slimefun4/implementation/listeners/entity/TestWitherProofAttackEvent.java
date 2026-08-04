package io.github.thebusybiscuit.slimefun4.implementation.listeners.entity;

import java.util.UUID;

import javax.annotation.Nonnull;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Wither;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
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
import be.seeseemelk.mockbukkit.entity.CowMock;

import io.github.thebusybiscuit.slimefun4.api.events.WitherProofAttackEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.core.attributes.WitherProof;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;
import io.github.thebusybiscuit.slimefun4.test.mocks.MockSlimefunItem;

import me.mrCookieSlime.Slimefun.api.BlockStorage;

/**
 * Regression coverage for the wither protection API expansion:
 * {@link WitherProofAttackEvent}, exercised through the real {@link WitherListener}
 * block-protection path. MockBukkit has no wither mock, so the attacker is a Mockito
 * {@link Wither}.
 *
 * @author Zurker
 */
class TestWitherProofAttackEvent {

    /**
     * A minimal {@link WitherProof} test item that records incoming attacks.
     */
    static class WitherProofMockItem extends MockSlimefunItem implements WitherProof {

        private Block attackedBlock;
        private Wither attackedBy;

        WitherProofMockItem(ItemGroup itemGroup, ItemStack item, String id) {
            super(itemGroup, item, id);
        }

        @Override
        public void onAttack(@Nonnull Block block, @Nonnull Wither wither) {
            this.attackedBlock = block;
            this.attackedBy = wither;
        }
    }

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static WitherProofMockItem witherProofItem;
    private static MockSlimefunItem plainItem;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        // Unit test startups do not register the listeners, register it manually
        new WitherListener(plugin);

        // Non-configurable items stay DISABLED unless Items.yml says otherwise, enable them first
        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "wither_proof_test");
        Slimefun.getItemCfg().setValue("TEST_WITHER_PROOF_BLOCK.enabled", true);
        witherProofItem = new WitherProofMockItem(itemGroup, new ItemStack(Material.OBSIDIAN), "TEST_WITHER_PROOF_BLOCK");
        witherProofItem.register(plugin);

        Slimefun.getItemCfg().setValue("TEST_PLAIN_BLOCK.enabled", true);
        plainItem = new MockSlimefunItem(itemGroup, new ItemStack(Material.BARREL), "TEST_PLAIN_BLOCK");
        plainItem.register(plugin);
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    @BeforeEach
    public void beforeEach() {
        server.getPluginManager().clearEvents();
        witherProofItem.attackedBlock = null;
        witherProofItem.attackedBy = null;
    }

    /**
     * Creates a Mockito wither, the only thing the listener needs from it is its type.
     */
    private Wither mockWither() {
        Wither wither = Mockito.mock(Wither.class);
        Mockito.when(wither.getType()).thenReturn(org.bukkit.entity.EntityType.WITHER);
        return wither;
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
     * Lets the wither destroy the block through the real event pipeline and returns
     * the event for assertions.
     */
    private EntityChangeBlockEvent destroyBlock(Wither wither, Block b) {
        EntityChangeBlockEvent event = new EntityChangeBlockEvent(wither, b, Bukkit.createBlockData(Material.AIR));
        server.getPluginManager().callEvent(event);
        return event;
    }

    @Test
    @DisplayName("WitherProofAttackEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Wither wither = mockWither();
        Block b = world.getBlockAt(1, 1, 1);
        EntityChangeBlockEvent changeEvent = new EntityChangeBlockEvent(wither, b, Bukkit.createBlockData(Material.AIR));

        WitherProofAttackEvent event = new WitherProofAttackEvent(witherProofItem, b, wither, changeEvent);

        Assertions.assertEquals(witherProofItem, event.getSlimefunItem());
        Assertions.assertEquals(b, event.getBlock());
        Assertions.assertEquals(wither, event.getWither());
        Assertions.assertEquals(changeEvent, event.getChangeBlockEvent());
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new WitherProofAttackEvent(null, b, wither, changeEvent));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new WitherProofAttackEvent(witherProofItem, null, wither, changeEvent));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new WitherProofAttackEvent(witherProofItem, b, null, changeEvent));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new WitherProofAttackEvent(witherProofItem, b, wither, null));
    }

    @Test
    @DisplayName("A wither attacking a wither-proof block fires the event and applies the protection")
    void testAttackFiresAndProtects() {
        Wither wither = mockWither();
        Block b = placeBlock(10, 10, witherProofItem, Material.OBSIDIAN);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onAttack(WitherProofAttackEvent event) {
                seen[0] = true;
                Assertions.assertEquals(witherProofItem, event.getSlimefunItem());
                Assertions.assertEquals(b, event.getBlock());
                Assertions.assertEquals(wither, event.getWither());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            EntityChangeBlockEvent event = destroyBlock(wither, b);

            Assertions.assertTrue(seen[0], "WitherProofAttackEvent was not fired");
            Assertions.assertTrue(event.isCancelled(), "The block change must have been prevented");
            Assertions.assertEquals(b, witherProofItem.attackedBlock, "onAttack must have been called with the block");
            Assertions.assertEquals(wither, witherProofItem.attackedBy, "onAttack must have been called with the wither");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling WitherProofAttackEvent lets the wither destroy the block")
    void testEventCancellationSkipsProtection() {
        Wither wither = mockWither();
        Block b = placeBlock(20, 20, witherProofItem, Material.OBSIDIAN);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onAttack(WitherProofAttackEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            EntityChangeBlockEvent event = destroyBlock(wither, b);

            Assertions.assertFalse(event.isCancelled(), "A cancelled protection must let the destruction through");
            Assertions.assertNull(witherProofItem.attackedBlock, "A cancelled protection must not call onAttack");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Protection without listeners still applies, preserving the old behavior")
    void testProtectionWithoutListenersStillApplies() {
        Wither wither = mockWither();
        Block b = placeBlock(30, 30, witherProofItem, Material.OBSIDIAN);

        EntityChangeBlockEvent event = destroyBlock(wither, b);

        Assertions.assertTrue(event.isCancelled(), "The block change must have been prevented");
        Assertions.assertEquals(b, witherProofItem.attackedBlock, "onAttack must have been called");
    }

    @Test
    @DisplayName("A non-wither entity fires no event")
    void testNonWitherFiresNothing() {
        CowMock cow = new CowMock(server, UUID.randomUUID());
        Block b = placeBlock(40, 40, witherProofItem, Material.OBSIDIAN);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onAttack(WitherProofAttackEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            EntityChangeBlockEvent event = new EntityChangeBlockEvent(cow, b, Bukkit.createBlockData(Material.AIR));
            server.getPluginManager().callEvent(event);

            Assertions.assertFalse(seen[0], "No event must be fired for a non-wither entity");
            Assertions.assertFalse(event.isCancelled(), "The block change must have been left alone");
            Assertions.assertNull(witherProofItem.attackedBlock, "onAttack must not have been called");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("A wither attacking a regular Slimefun block fires no event")
    void testNonWitherProofBlockFiresNothing() {
        Wither wither = mockWither();
        Block b = placeBlock(50, 50, plainItem, Material.BARREL);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onAttack(WitherProofAttackEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            EntityChangeBlockEvent event = destroyBlock(wither, b);

            Assertions.assertFalse(seen[0], "No event must be fired for a non-wither-proof block");
            Assertions.assertFalse(event.isCancelled(), "The block change must have been left alone");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }
}
