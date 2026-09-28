package io.github.thebusybiscuit.slimefun4.implementation.listeners;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.WorldMock;
import io.github.thebusybiscuit.slimefun4.api.events.MultiBlockInteractEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.core.multiblocks.MultiBlock;
import io.github.thebusybiscuit.slimefun4.core.multiblocks.MultiBlockMachine;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;

/**
 * Discriminant tests for the multiblock trigger-material bucket pre-filter:
 * the bucket path must pick exactly the multiblock the old full-registry scan
 * picked, for every trigger face, tag equivalence, wildcard cell and
 * registry-ordering scenario.
 *
 * @author Zurker
 */
class TestMultiblockBucketPrefilter {

    private static ServerMock server;
    private static Slimefun plugin;
    private static WorldMock world;
    private static MultiBlockListener listener;
    private static ItemGroup group;

    private static MultiBlockInteractEvent lastEvent;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);

        /*
         * Full-match clicks run the item's interaction handler whose canUse()
         * consults the ProtectionManager - normally created on the first server
         * tick which a unit test never pumps. Inject it the same way
         * IntegrationsManager#onServerStart would (environment setup).
         */
        try {
            java.lang.reflect.Field field = Slimefun.getIntegrations().getClass().getDeclaredField("protectionManager");
            field.setAccessible(true);
            field.set(Slimefun.getIntegrations(), new io.github.bakedlibs.dough.protection.ProtectionManager(plugin));
        } catch (ReflectiveOperationException x) {
            throw new IllegalStateException("Cannot inject the ProtectionManager", x);
        }

        listener = new MultiBlockListener(plugin);
        server.getPluginManager().registerEvents(new Listener() {

            @EventHandler
            public void onMultiblock(MultiBlockInteractEvent e) {
                lastEvent = e;
            }
        }, plugin);
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    @BeforeEach
    public void reset() {
        lastEvent = null;
    }

    private static Player player() {
        Player player = server.addPlayer("mb_test_" + System.nanoTime());
        world = (WorldMock) player.getWorld();
        return player;
    }

    private static MultiBlockMachine registerMachine(String id, Material[] structure, BlockFace trigger) {
        ItemStack[] recipe = new ItemStack[9];

        for (int i = 0; i < 9; i++) {
            recipe[i] = structure[i] == null ? null : new ItemStack(structure[i]);
        }

        SlimefunItemStack stack = new SlimefunItemStack(id, Material.PAPER, "&b" + id, "&7test");
        MultiBlockMachine machine = new MultiBlockMachine(group(), stack, recipe, trigger) {

            @Override
            @ParametersAreNonnullByDefault
            public void onInteract(Player p, Block b) {
                // no behaviour
            }
        };

        machine.register(Slimefun.instance());
        return machine;
    }

    private static ItemGroup group() {
        if (group == null) {
            group = new ItemGroup(new NamespacedKey(plugin, "mb_test_group"), new ItemStack(Material.CHEST));
        }

        return group;
    }

    /**
     * Builds the structure in the world around the center at (x, y, z) with
     * the side columns running north/south, then clicks the block that the
     * trigger face dictates.
     */
    private static void build(Material[] structure, int x, int y, int z) {
        // Middle column (indices 1, 4, 7)
        set(x, y + 1, z, structure[1]);
        set(x, y, z, structure[4]);
        set(x, y - 1, z, structure[7]);

        // Side columns (0,3,6 north and 2,5,8 south)
        set(x, y + 1, z - 1, structure[0]);
        set(x, y, z - 1, structure[3]);
        set(x, y - 1, z - 1, structure[6]);
        set(x, y + 1, z + 1, structure[2]);
        set(x, y, z + 1, structure[5]);
        set(x, y - 1, z + 1, structure[8]);
    }

    private static void set(int x, int y, int z, Material material) {
        world.getBlockAt(x, y, z).setType(material != null ? material : Material.AIR);
    }

    private static void click(Player player, Block block) {
        lastEvent = null;
        PlayerInteractEvent event = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, null, block, BlockFace.EAST, EquipmentSlot.HAND);
        listener.onRightClick(event);
    }

    /**
     * The pre-optimization oracle: walk the registry in order and collect every
     * multiblock whose structure matches, exactly like the old listener body.
     */
    @Nonnull
    private static MultiBlock fullScanBest(Block clicked) {
        MultiBlock best = null;

        for (MultiBlock mb : Slimefun.getRegistry().getMultiBlocks()) {
            Block center = clicked.getRelative(mb.getTriggerBlock());

            if (compareMaterials(center, mb.getStructure(), mb.isSymmetric())) {
                best = mb;
            }
        }

        return best;
    }

    private static boolean compareMaterials(Block b, Material[] blocks, boolean onlyTwoWay) {
        if (!compareMaterialsVertical(b, blocks[1], blocks[4], blocks[7])) {
            return false;
        }

        BlockFace[] directions = onlyTwoWay ? new BlockFace[] { BlockFace.NORTH, BlockFace.EAST } : new BlockFace[] { BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST };

        for (BlockFace direction : directions) {
            if (compareMaterialsVertical(b.getRelative(direction), blocks[0], blocks[3], blocks[6]) && compareMaterialsVertical(b.getRelative(direction.getOppositeFace()), blocks[2], blocks[5], blocks[8])) {
                return true;
            }
        }

        return false;
    }

    private static boolean compareMaterialsVertical(Block b, Material top, Material center, Material bottom) {
        return (center == null || equalsMock(b.getType(), center)) && (top == null || equalsMock(b.getRelative(BlockFace.UP).getType(), top)) && (bottom == null || equalsMock(b.getRelative(BlockFace.DOWN).getType(), bottom));
    }

    private static boolean equalsMock(Material a, Material b) {
        if (a == b) {
            return true;
        }

        for (Tag<Material> tag : MultiBlock.getSupportedTags()) {
            if (tag.isTagged(a) && tag.isTagged(b)) {
                return true;
            }
        }

        return false;
    }

    @Test
    @DisplayName("SELF, UP and DOWN triggers all resolve through the bucket for a fully built structure")
    void testAllTriggerFaces() {
        Material[] structure = { null, Material.IRON_BLOCK, null, null, Material.CAULDRON, null, null, Material.IRON_BLOCK, null };

        // SELF: click the center cell
        registerMachine("MB_TEST_SELF", structure, BlockFace.SELF);
        Player player = player();
        build(structure, 1000, 100, 1000);
        Block clicked = world.getBlockAt(1000, 100, 1000);
        Assertions.assertEquals(fullScanBest(clicked), triggerAndCapture(player, clicked), "SELF trigger must match");

        // UP: the clicked block is the bottom cell (index 7)
        registerMachine("MB_TEST_UP", structure, BlockFace.UP);
        Player playerUp = player();
        build(structure, 1100, 100, 1100);
        Block clickedUp = world.getBlockAt(1100, 99, 1100);
        Assertions.assertEquals(fullScanBest(clickedUp), triggerAndCapture(playerUp, clickedUp), "UP trigger must match when clicking the bottom cell");

        // DOWN: the clicked block is the top cell (index 1)
        registerMachine("MB_TEST_DOWN", structure, BlockFace.DOWN);
        Player playerDown = player();
        build(structure, 1200, 100, 1200);
        Block clickedDown = world.getBlockAt(1200, 101, 1200);
        Assertions.assertEquals(fullScanBest(clickedDown), triggerAndCapture(playerDown, clickedDown), "DOWN trigger must match when clicking the top cell");
    }

    @Test
    @DisplayName("Tag equivalence: an OAK_LOG structure triggers on SPRUCE_LOG clicks")
    void testTagEquivalence() {
        Material[] structure = { null, null, null, null, Material.OAK_LOG, null, null, null, null };
        registerMachine("MB_TEST_TAGGED", structure, BlockFace.SELF);

        Player player = player();
        build(structure, 1300, 100, 1300);
        world.getBlockAt(1300, 100, 1300).setType(Material.SPRUCE_LOG);
        Block clicked = world.getBlockAt(1300, 100, 1300);

        Assertions.assertEquals(fullScanBest(clicked), triggerAndCapture(player, clicked), "A spruce click must trigger the oak-structure multiblock");
    }

    @Test
    @DisplayName("A wildcard trigger cell (null) matches on any material via the unbinned path")
    void testWildcardTriggerCell() {
        Material[] structure = { null, Material.GOLD_BLOCK, null, null, null, null, null, Material.GOLD_BLOCK, null };
        registerMachine("MB_TEST_WILDCARD", structure, BlockFace.SELF);

        Player player = player();
        build(structure, 1400, 100, 1400);
        world.getBlockAt(1400, 100, 1400).setType(Material.DIRT);
        Block clicked = world.getBlockAt(1400, 100, 1400);

        Assertions.assertEquals(fullScanBest(clicked), triggerAndCapture(player, clicked), "A wildcard center must match a DIRT click");
    }

    @Test
    @DisplayName("Multiple matches: the last multiblock in registry order wins (binned and unbinned mixed)")
    void testRegistryOrderSelection() {
        Material[] structure = { null, null, null, null, Material.LAPIS_BLOCK, null, null, null, null };

        MultiBlockMachine first = registerMachine("MB_TEST_ORDER_1", structure, BlockFace.SELF);
        Material[] wildcard = { null, Material.DIAMOND_BLOCK, null, null, null, null, null, Material.DIAMOND_BLOCK, null };
        MultiBlockMachine wildcardMachine = registerMachine("MB_TEST_ORDER_WILDCARD", wildcard, BlockFace.SELF);
        MultiBlockMachine last = registerMachine("MB_TEST_ORDER_2", structure, BlockFace.SELF);

        Player player = player();
        build(structure, 1500, 100, 1500);
        world.getBlockAt(1500, 101, 1500).setType(Material.DIAMOND_BLOCK);
        world.getBlockAt(1500, 99, 1500).setType(Material.DIAMOND_BLOCK);
        Block clicked = world.getBlockAt(1500, 100, 1500);

        // Registry order: first < wildcardMachine < last -> the last binned machine must win
        Assertions.assertSame(last.getMultiBlock(), triggerAndCapture(player, clicked), "The last registry-order match must win even with an unbinned wildcard in between");
        Assertions.assertNotSame(wildcardMachine.getMultiBlock(), lastEvent.getMultiBlock());

        // An unbinned machine at the END of the registry must beat earlier binned ones
        Material[] wildcard2 = { null, Material.EMERALD_BLOCK, null, null, null, null, null, Material.EMERALD_BLOCK, null };
        MultiBlockMachine trailingWildcard = registerMachine("MB_TEST_ORDER_TRAILING", wildcard2, BlockFace.SELF);

        Player player2 = player();
        build(structure, 1600, 100, 1600);
        world.getBlockAt(1600, 101, 1600).setType(Material.EMERALD_BLOCK);
        world.getBlockAt(1600, 99, 1600).setType(Material.EMERALD_BLOCK);
        Block clicked2 = world.getBlockAt(1600, 100, 1600);

        Assertions.assertSame(trailingWildcard.getMultiBlock(), triggerAndCapture(player2, clicked2), "A trailing unbinned wildcard must win over earlier binned matches");
        Assertions.assertEquals(fullScanBest(clicked2), lastEvent.getMultiBlock(), "The bucket path must agree with the full-scan oracle");
    }

    @Test
    @DisplayName("Direct registry additions (bypassing the hook) self-heal via the staleness check")
    void testStaleBucketSelfHealing() {
        Material[] structure = { null, null, null, null, Material.OBSIDIAN, null, null, null, null };

        // Bypass MultiBlockMachine#postRegister: build the machine but do NOT
        // register() it - its multiblock goes straight into the registry list
        SlimefunItemStack realStack = new SlimefunItemStack("MB_TEST_STALE_MACHINE", Material.PAPER, "&bStale Machine", "&7test");
        MultiBlockMachine machine = new MultiBlockMachine(group(), realStack, new ItemStack[] { null, null, null, null, new ItemStack(Material.OBSIDIAN), null, null, null, null }, BlockFace.SELF) {

            @Override
            @ParametersAreNonnullByDefault
            public void onInteract(Player p, Block b) {
                // no behaviour
            }
        };

        // Do NOT call register() - insert the multiblock straight into the registry list
        Slimefun.getRegistry().getMultiBlocks().add(machine.getMultiBlock());
        Assertions.assertTrue(Slimefun.getRegistry().isMultiblockBucketStale(), "Precondition: direct add must mark the buckets stale");

        Player player = player();
        build(structure, 1700, 100, 1700);
        Block clicked = world.getBlockAt(1700, 100, 1700);

        Assertions.assertEquals(fullScanBest(clicked), triggerAndCapture(player, clicked), "The self-healing rebuild must make the directly added multiblock match");
        Assertions.assertFalse(Slimefun.getRegistry().isMultiblockBucketStale(), "The interaction must have rebuilt the buckets");
    }

    @Test
    @DisplayName("A click on a material no multiblock uses fires no event at all")
    void testNoCandidates() {
        Player player = player();
        Block clicked = world.getBlockAt(1800, 100, 1800);
        clicked.setType(Material.BEDROCK);

        Assertions.assertNull(triggerAndCapture(player, clicked), "No multiblock event may fire for a material no multiblock uses");
    }

    @Test
    @DisplayName("The bucket path agrees with the full-scan oracle across many click materials")
    void testOracleEquivalence() {
        List<Material> clickMaterials = new ArrayList<>();
        clickMaterials.add(Material.CAULDRON);
        clickMaterials.add(Material.SPRUCE_LOG);
        clickMaterials.add(Material.DIRT);
        clickMaterials.add(Material.IRON_BLOCK);
        clickMaterials.add(Material.OBSIDIAN);
        clickMaterials.add(Material.BEDROCK);

        for (Material material : clickMaterials) {
            Player player = player();
            Block clicked = world.getBlockAt(1900, 100, 1800 + material.ordinal() % 50);
            clicked.setType(material);

            MultiBlock expected = fullScanBest(clicked);
            MultiBlock actual = triggerAndCapture(player, clicked);

            Assertions.assertEquals(expected == null ? null : expected.getSlimefunItem().getId(), actual == null ? null : actual.getSlimefunItem().getId(), "Bucket path must agree with the oracle for material " + material);
        }
    }

    private MultiBlock triggerAndCapture(Player player, Block clicked) {
        click(player, clicked);
        return lastEvent != null ? lastEvent.getMultiBlock() : null;
    }
}
