package io.github.thebusybiscuit.slimefun4.core.multiblocks;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang.Validate;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.BlockFace;

import io.github.thebusybiscuit.slimefun4.api.events.MultiBlockInteractEvent;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.core.handlers.MultiBlockInteractionHandler;

/**
 * A {@link MultiBlock} represents a structure build in a {@link World}.
 * A {@link MultiBlock} is often linked to a {@link MultiBlockMachine} and is used
 * to recognize that machine in a {@link MultiBlockInteractEvent}.
 * 
 * @author TheBusyBiscuit
 * @author Liruxo
 * 
 * @see MultiBlockMachine
 * @see MultiBlockInteractionHandler
 * @see MultiBlockInteractEvent
 *
 */
public class MultiBlock {

    private static final Set<Tag<Material>> SUPPORTED_TAGS = new HashSet<>();

    static {
        // Allow variations of different types of wood to be used
        SUPPORTED_TAGS.add(Tag.LOGS);
        SUPPORTED_TAGS.add(Tag.WOODEN_TRAPDOORS);
        SUPPORTED_TAGS.add(Tag.WOODEN_SLABS);
        SUPPORTED_TAGS.add(Tag.WOODEN_FENCES);
        SUPPORTED_TAGS.add(Tag.FIRE);
    }

    @Nonnull
    public static Set<Tag<Material>> getSupportedTags() {
        return SUPPORTED_TAGS;
    }

    private final SlimefunItem item;
    private final Material[] blocks;
    private final BlockFace trigger;
    private final boolean isSymmetric;

    public MultiBlock(@Nonnull SlimefunItem item, Material[] build, @Nonnull BlockFace trigger) {
        Validate.notNull(item, "A MultiBlock requires a SlimefunItem!");

        if (build == null || build.length != 9) {
            throw new IllegalArgumentException("MultiBlocks must have a length of 9!");
        }

        if (trigger != BlockFace.SELF && trigger != BlockFace.UP && trigger != BlockFace.DOWN) {
            throw new IllegalArgumentException("Multiblock Blockface must be either UP, DOWN or SELF");
        }

        this.item = item;
        this.blocks = build;
        this.trigger = trigger;
        this.isSymmetric = isSymmetric(build);
    }

    @Nonnull
    public SlimefunItem getSlimefunItem() {
        return item;
    }

    private static boolean isSymmetric(@Nonnull Material[] blocks) {
        return blocks[0] == blocks[2] && blocks[3] == blocks[5] && blocks[6] == blocks[8];
    }

    @Nonnull
    public Material[] getStructure() {
        return blocks;
    }

    @Nonnull
    public BlockFace getTriggerBlock() {
        return trigger;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MultiBlock)) {
            return false;
        }

        MultiBlock mb = (MultiBlock) obj;

        if (trigger == mb.getTriggerBlock() && isSymmetric == mb.isSymmetric) {
            for (int i = 0; i < mb.getStructure().length; i++) {
                if (!compareBlocks(blocks[i], mb.getStructure()[i])) {
                    return false;
                }
            }

            return true;
        }

        return false;
    }

    @Override
    public int hashCode() {
        /*
         * Must stay consistent with equals(...): equals compares the structure via
         * tag equivalence (and the piston special case), the trigger and the
         * symmetry - it does NOT compare the owning item. Hashing raw materials or
         * the array identity would break the equals/hashCode contract for any
         * hash-based container, so every block is normalized to its equivalence
         * class first.
         */
        int[] hashes = new int[blocks.length];

        for (int i = 0; i < blocks.length; i++) {
            hashes[i] = blockHash(blocks[i]);
        }

        return Objects.hash(Arrays.hashCode(hashes), trigger, isSymmetric);
    }

    private static int blockHash(@Nullable Material material) {
        if (material == null) {
            return 0;
        }

        // Pistons and moving pistons compare as equal in equals(...) too
        if (material == Material.PISTON || material == Material.MOVING_PISTON) {
            return Material.PISTON.hashCode();
        }

        for (Tag<Material> tag : SUPPORTED_TAGS) {
            if (tag.isTagged(material)) {
                // Tag constants are singleton instances, so this is stable within a runtime
                return tag.hashCode();
            }
        }

        return material.hashCode();
    }

    private boolean compareBlocks(Material a, @Nullable Material b) {
        if (b != null) {

            for (Tag<Material> tag : SUPPORTED_TAGS) {
                if (tag.isTagged(b)) {
                    return tag.isTagged(a);
                }
            }

            // This ensures that the Industrial Miner is still recognized while operating
            if (a == Material.PISTON) {
                return b == Material.PISTON || b == Material.MOVING_PISTON;
            } else if (b == Material.PISTON) {
                return a == Material.MOVING_PISTON;
            }

            if (b != a) {
                return false;
            }
        }

        return true;
    }

    /**
     * This returns whether this {@link MultiBlock} is a symmetric structure or whether
     * the left and right side differ.
     * 
     * @return Whether this {@link MultiBlock} is a symmetric structure
     */
    public boolean isSymmetric() {
        return isSymmetric;
    }
    @Override
    public String toString() {
        return "MultiBlock (" + item.getId() + ") {" + Arrays.toString(blocks) + "}";
    }
}
