package io.github.thebusybiscuit.slimefun4.api.items;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.DistinctiveItem;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;

/**
 * Discriminant tests for the item identity resolution memo and the cached
 * template comparison meta: resolution results must be identical to the
 * uncached path, late registrations and out-of-band re-tagging must
 * invalidate, and {@code SlimefunUtils#isItemSimilar} must keep its full
 * comparison semantics (including the potion base-type check).
 *
 * @author Zurker
 */
class TestItemIdentityResolutionMemo {

    private static ServerMock server;
    private static Slimefun plugin;
    private static ItemGroup group;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        group = new ItemGroup(new NamespacedKey(plugin, "r14_test_group"), new ItemStack(Material.CHEST));
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("A registered template clone resolves to its item, a vanilla stack resolves to null - repeatedly")
    void testResolutionEquivalence() {
        SlimefunItemStack stack = new SlimefunItemStack("R14_RESOLVE_A", Material.PAPER, "&bResolve A", "&7line");
        SlimefunItem item = new SlimefunItem(group, stack, RecipeType.NULL, new ItemStack[9]);
        item.register(Slimefun.instance());

        ItemStack templateClone = stack.item();
        Assertions.assertSame(item, SlimefunItem.getByItem(templateClone), "First resolution must find the item");
        Assertions.assertSame(item, SlimefunItem.getByItem(templateClone), "Memoized resolution must be identical");

        ItemStack vanilla = new ItemStack(Material.PAPER);
        vanilla.editMeta(meta -> meta.setDisplayName("Plain"));
        Assertions.assertNull(SlimefunItem.getByItem(vanilla), "A vanilla stack must resolve to null");
        Assertions.assertNull(SlimefunItem.getByItem(vanilla), "The memoized null must stay null");

        ItemStack foreign = new ItemStack(Material.STONE);
        Assertions.assertNull(SlimefunItem.getByItem(foreign), "A foreign-material stack must resolve to null");
    }

    @Test
    @DisplayName("A late registration turns a previously-null resolution non-null (register() clears the memo)")
    void testLateRegistrationInvalidation() {
        SlimefunItemStack stack = new SlimefunItemStack("R14_LATE_ITEM", Material.PAPER, "&bLate Item", "&7line");
        ItemStack templateClone = stack.item();

        Assertions.assertNull(SlimefunItem.getByItem(templateClone), "Before registration the PDC id must resolve to null (memoized)");

        SlimefunItem item = new SlimefunItem(group, stack, RecipeType.NULL, new ItemStack[9]);
        item.register(Slimefun.instance());

        Assertions.assertSame(item, SlimefunItem.getByItem(templateClone), "After the late registration the memo must have been cleared and the resolution must succeed");
    }

    @Test
    @DisplayName("Out-of-band re-tagging only takes effect after invalidateItemResolutionCache()")
    void testOutOfBandRetagContract() {
        SlimefunItemStack stack = new SlimefunItemStack("R14_RETAG_ITEM", Material.PAPER, "&bRetag Item", "&7line");
        new SlimefunItem(group, stack, RecipeType.NULL, new ItemStack[9]).register(Slimefun.instance());

        ItemStack vanilla = new ItemStack(Material.PAPER);
        vanilla.editMeta(meta -> meta.setDisplayName("Plain"));
        Assertions.assertNull(SlimefunItem.getByItem(vanilla), "Precondition: the vanilla stack resolves to null");

        // An addon retags the existing stack instance (out-of-band id write)
        Slimefun.getItemDataService().setItemData(vanilla, "R14_RETAG_ITEM");

        Assertions.assertNull(SlimefunItem.getByItem(vanilla), "The memoized null must survive until the documented invalidation");
        SlimefunItem.invalidateItemResolutionCache();
        Assertions.assertNotNull(SlimefunItem.getByItem(vanilla), "After invalidation the retagged stack must resolve");
    }

    @Test
    @DisplayName("isItemSimilar keeps its full comparison semantics through the template meta cache")
    void testIsItemSimilarSemantics() {
        SlimefunItemStack stackA = new SlimefunItemStack("R14_SIM_A", Material.PAPER, "&bSimilar A", "&7lore a", "&8marker");
        SlimefunItem itemA = new SlimefunItem(group, stackA, RecipeType.NULL, new ItemStack[9]);
        itemA.register(Slimefun.instance());
        ItemStack templateA = stackA.item();

        SlimefunItemStack stackB = new SlimefunItemStack("R14_SIM_B", Material.PAPER, "&bSimilar B", "&7lore b");
        new SlimefunItem(group, stackB, RecipeType.NULL, new ItemStack[9]).register(Slimefun.instance());
        ItemStack templateB = stackB.item();

        // PDC-to-PDC: same id matches, different ids do not
        Assertions.assertTrue(SlimefunUtils.isItemSimilar(templateA.clone(), templateA, true), "Same-id stacks must be similar");
        Assertions.assertFalse(SlimefunUtils.isItemSimilar(templateA.clone(), templateB, true), "Different-id stacks must not be similar");

        // Vanilla-vs-template (the template meta cache path)
        ItemStack vanilla = new ItemStack(Material.PAPER);
        vanilla.editMeta(meta -> meta.setDisplayName("Plain Paper"));
        Assertions.assertFalse(SlimefunUtils.isItemSimilar(vanilla, templateA, true), "A differently-named vanilla stack must not match the template");

        // A vanilla stack fully renamed to the template identity matches
        ItemStack lookalike = new ItemStack(Material.PAPER);
        lookalike.editMeta(meta -> {
            meta.setDisplayName("§bSimilar A");
            meta.setLore(java.util.List.of("§7lore a", "§8marker"));
        });
        Assertions.assertTrue(SlimefunUtils.isItemSimilar(lookalike, templateA, true), "A fully renamed lookalike must match the template");

        // Lore mismatch with checkLore
        ItemStack wrongLore = new ItemStack(Material.PAPER);
        wrongLore.editMeta(meta -> {
            meta.setDisplayName("§bSimilar A");
            meta.setLore(java.util.List.of("§7different lore"));
        });
        Assertions.assertFalse(SlimefunUtils.isItemSimilar(wrongLore, templateA, true), "A lore mismatch must fail with checkLore");

        // The cached template meta must be a memo of the same instance
        Assertions.assertSame(itemA.getTemplateItemMeta(), itemA.getTemplateItemMeta(), "The template meta cache must be a memo");
    }

    @Test
    @DisplayName("The potion base-type check survives the template meta cache")
    void testPotionCheckSurvives() {
        SlimefunItemStack potionStack = new SlimefunItemStack("R14_POTION_A", Material.POTION, "&bBrew A", "&7potion lore");

        // The comparison runs against the REGISTERED template's meta, so the base
        // type must live on the registered template itself (not just on a clone)
        ItemMeta potionMeta = potionStack.item().getItemMeta();
        ((PotionMeta) potionMeta).setBasePotionType(PotionType.SWIFTNESS);
        potionStack.setItemMeta(potionMeta);

        new SlimefunItem(group, potionStack, RecipeType.NULL, new ItemStack[9]).register(Slimefun.instance());
        ItemStack template = potionStack.item();

        ItemStack sameBase = new ItemStack(Material.POTION);
        sameBase.editMeta(meta -> {
            meta.setDisplayName("§bBrew A");
            meta.setLore(java.util.List.of("§7potion lore"));
            ((PotionMeta) meta).setBasePotionType(PotionType.SWIFTNESS);
        });

        ItemStack otherBase = new ItemStack(Material.POTION);
        otherBase.editMeta(meta -> {
            meta.setDisplayName("§bBrew A");
            meta.setLore(java.util.List.of("§7potion lore"));
            ((PotionMeta) meta).setBasePotionType(PotionType.SLOWNESS);
        });

        Assertions.assertTrue(SlimefunUtils.isItemSimilar(sameBase, template, true), "Same name and base potion type must match");
        Assertions.assertFalse(SlimefunUtils.isItemSimilar(otherBase, template, true), "A different base potion type must not match even with the same name");
    }

    @Test
    @DisplayName("DistinctiveItem comparisons: PDC'd same-id stacks compare registered templates (pre-existing), vanilla lookalikes compare actual metas")
    void testDistinctiveItems() {
        SlimefunItemStack stack = new SlimefunItemStack("R14_DISTINCT_A", Material.PAPER, "&bDistinct A", "&7base");
        DistinctTestItem item = new DistinctTestItem(group, stack, new ItemStack[9]);
        item.register(Slimefun.instance());

        ItemStack base = stack.item();
        ItemStack marked = stack.item();
        marked.editMeta(meta -> meta.setLore(java.util.List.of("§7base", "§5marked")));

        /*
         * Pre-existing upstream semantics (unchanged by the memo round): two PDC'd
         * stacks of the same id run canStack against the REGISTERED item's
         * template meta on both sides, so instance-level differences do not
         * distinguish here.
         */
        Assertions.assertTrue(SlimefunUtils.isItemSimilar(base, base.clone(), true), "Same-id instances must stack (template-based pre-existing behavior)");
        Assertions.assertTrue(SlimefunUtils.isItemSimilar(marked, base, true), "Same-id instances must stack regardless of instance lore (template-based pre-existing behavior)");

        /*
         * The vanilla-lookalike branch (no PDC on the tested item) compares the
         * tested meta against the ACTUAL argument meta - that is where the
         * distinctive marker has its power (e.g. cargo filtering renamed items).
         */
        ItemStack lookalike = new ItemStack(Material.PAPER);
        lookalike.editMeta(meta -> {
            meta.setDisplayName("§bDistinct A");
            meta.setLore(java.util.List.of("§7base", "§5marked"));
        });
        Assertions.assertFalse(SlimefunUtils.isItemSimilar(lookalike, base, true), "A lookalike carrying the distinctive marker must not match the plain template");

        ItemStack plainLookalike = new ItemStack(Material.PAPER);
        plainLookalike.editMeta(meta -> {
            meta.setDisplayName("§bDistinct A");
            meta.setLore(java.util.List.of("§7base"));
        });
        Assertions.assertTrue(SlimefunUtils.isItemSimilar(plainLookalike, base, true), "A lookalike matching the template exactly (incl. distinctive lore) must match");
    }

    private static class DistinctTestItem extends SlimefunItem implements DistinctiveItem {

        @ParametersAreNonnullByDefault
        DistinctTestItem(ItemGroup itemGroup, SlimefunItemStack item, ItemStack[] recipe) {
            super(itemGroup, item, RecipeType.NULL, recipe);
        }

        @Override
        public boolean canStack(@Nonnull ItemMeta itemMetaOne, @Nonnull ItemMeta itemMetaTwo) {
            return itemMetaOne.getLore().equals(itemMetaTwo.getLore());
        }
    }
}
