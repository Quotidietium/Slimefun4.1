package io.github.thebusybiscuit.slimefun4.utils;

import java.util.Collections;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.bakedlibs.dough.data.persistent.PersistentDataAPI;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;

import be.seeseemelk.mockbukkit.MockBukkit;

class TestChargeUtils {

    @BeforeAll
    public static void load() {
        MockBukkit.mock();
        MockBukkit.load(Slimefun.class);
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("Test setting charge")
    void testSetCharge() {
        ItemStack item = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta meta = item.getItemMeta();

        // Make sure the lore is set
        ChargeUtils.setCharge(meta, 1, 10);
        Assertions.assertTrue(meta.hasLore());
        Assertions.assertEquals(1, meta.getLore().size());

        // Make sure the lore is correct
        ChargeUtils.setCharge(meta, 10.1f, 100.5f);
        Assertions.assertEquals("&8\u21E8 &e\u26A1 &710.1 / 100.5 J".replace('&', ChatColor.COLOR_CHAR), meta.getLore().get(0));

        // Make sure the persistent data was set
        Assertions.assertEquals(10.1, PersistentDataAPI.getFloat(meta, Slimefun.getRegistry().getItemChargeDataKey()), 0.001);

        // Test exceptions
        Assertions.assertThrows(IllegalArgumentException.class, () -> ChargeUtils.setCharge(null, 1, 1));
        Assertions.assertThrows(IllegalArgumentException.class, () -> ChargeUtils.setCharge(meta, -1, 10));
        Assertions.assertThrows(IllegalArgumentException.class, () -> ChargeUtils.setCharge(meta, 100, 10));
        Assertions.assertThrows(IllegalArgumentException.class, () -> ChargeUtils.setCharge(meta, 10, -10));
    }

    @Test
    @DisplayName("Test getting charge")
    void testGetCharge() {
        // Test with persistent data
        ItemStack itemWithData = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta metaWithData = itemWithData.getItemMeta();
        PersistentDataAPI.setFloat(metaWithData, Slimefun.getRegistry().getItemChargeDataKey(), 10.5f);

        Assertions.assertEquals(10.5f, ChargeUtils.getCharge(metaWithData), 0.001);

        // Test with lore
        ItemStack itemWithLore = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta metaWithLore = itemWithLore.getItemMeta();
        metaWithLore.setLore(Collections.singletonList("&8\u21E8 &e\u26A1 &710.5 / 100.5 J".replace('&', ChatColor.COLOR_CHAR)));

        Assertions.assertEquals(10.5, ChargeUtils.getCharge(metaWithLore), 0.001);
        Assertions.assertTrue(PersistentDataAPI.hasFloat(metaWithLore, Slimefun.getRegistry().getItemChargeDataKey()));

        // Test no data and empty lore
        ItemStack itemWithEmptyLore = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta metaWithEmptyLore = itemWithEmptyLore.getItemMeta();
        metaWithEmptyLore.setLore(Collections.emptyList());

        Assertions.assertEquals(0, ChargeUtils.getCharge(metaWithEmptyLore));

        // Test no data and no lore
        ItemStack itemWithNoDataOrLore = new ItemStack(Material.DIAMOND_SWORD);

        Assertions.assertEquals(0, ChargeUtils.getCharge(itemWithNoDataOrLore.getItemMeta()));

        // Test exceptions
        Assertions.assertThrows(IllegalArgumentException.class, () -> ChargeUtils.getCharge(null));
    }

    @Test
    @DisplayName("Crafted or corrupted persistent-data charge values are sanitized to uncharged")
    void testGetChargeSanitizesCraftedPersistentData() {
        // A modified client can place arbitrary floats in the item's persistent data.
        // Negative, NaN and infinite values must read back as 0 instead of flowing into
        // setCharge (which would throw inside async machine ticks and can destroy machines
        // via the ticker error limit).
        ItemStack negative = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta negativeMeta = negative.getItemMeta();
        PersistentDataAPI.setFloat(negativeMeta, Slimefun.getRegistry().getItemChargeDataKey(), -5.0f);
        Assertions.assertEquals(0.0f, ChargeUtils.getCharge(negativeMeta), "A negative crafted charge must read as 0");

        ItemStack nan = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta nanMeta = nan.getItemMeta();
        PersistentDataAPI.setFloat(nanMeta, Slimefun.getRegistry().getItemChargeDataKey(), Float.NaN);
        Assertions.assertEquals(0.0f, ChargeUtils.getCharge(nanMeta), "A NaN crafted charge must read as 0");

        ItemStack infinite = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta infiniteMeta = infinite.getItemMeta();
        PersistentDataAPI.setFloat(infiniteMeta, Slimefun.getRegistry().getItemChargeDataKey(), Float.POSITIVE_INFINITY);
        Assertions.assertEquals(0.0f, ChargeUtils.getCharge(infiniteMeta), "An infinite crafted charge must read as 0");
    }

    @Test
    @DisplayName("Crafted lore with a saturated number neither returns nor persists a non-finite charge")
    void testGetChargeSanitizesCraftedLore() {
        // The lore regex allows arbitrarily long digit strings; Float.parseFloat saturates
        // those to Infinity WITHOUT throwing. Such lore must read as 0 and must not write
        // the non-finite value into the persistent data container.
        ItemStack saturated = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta saturatedMeta = saturated.getItemMeta();
        saturatedMeta.setLore(Collections.singletonList(("&8⇨ &e⚡ &7" + "9".repeat(400) + " / 100.5 J").replace('&', ChatColor.COLOR_CHAR)));

        Assertions.assertEquals(0.0f, ChargeUtils.getCharge(saturatedMeta), "A saturated lore number must read as 0");
        Assertions.assertFalse(PersistentDataAPI.hasFloat(saturatedMeta, Slimefun.getRegistry().getItemChargeDataKey()), "The non-finite lore value must not be persisted");

        // A negative number cannot be produced by the regex ([0-9.]+), but a decimal like
        // "1.2.3" must still fall back to 0 instead of throwing.
        ItemStack malformed = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta malformedMeta = malformed.getItemMeta();
        malformedMeta.setLore(Collections.singletonList("&8⇨ &e⚡ &71.2.3 / 100.5 J".replace('&', ChatColor.COLOR_CHAR)));

        Assertions.assertEquals(0.0f, ChargeUtils.getCharge(malformedMeta), "A malformed lore number must read as 0");
    }
}
