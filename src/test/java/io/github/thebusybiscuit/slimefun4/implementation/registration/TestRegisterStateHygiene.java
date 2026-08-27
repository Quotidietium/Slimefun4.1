package io.github.thebusybiscuit.slimefun4.implementation.registration;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import io.github.thebusybiscuit.slimefun4.api.items.ItemState;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;
import me.mrCookieSlime.Slimefun.Objects.handlers.BlockTicker;

/**
 * State-machine hygiene for {@link SlimefunItem#register(SlimefunAddon)}:
 * a failed registration must not leave a half-registered husk in the registry,
 * and an item disabled via URID.enable-tickers=false must go through the same
 * handler-cleanup path as any other disabled item.
 *
 * @author Zurker
 */
class TestRegisterStateHygiene {

    private static ServerMock server;
    private static Slimefun plugin;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("A failed registration leaves no half-registered husk in the registry")
    void testFailedRegistrationRollsBack() {
        SlimefunItem failing = new SlimefunItem(TestUtilities.getItemGroup(plugin, "rollback_test"), new SlimefunItemStack("_FAILING_REGISTRATION", Material.BEDROCK, "&cBoom"), RecipeType.NULL, new ItemStack[9]) {

            @Override
            public void postRegister() {
                // Fires AFTER the registry entries were written - the roll-back under
                // test only matters for failures at this stage or later
                throw new IllegalStateException("Intentional failure for the test");
            }
        };

        // In a unit test the error is re-thrown, in production it is only logged -
        // in BOTH cases the registry must not contain the half-registered item
        Assertions.assertThrows(RuntimeException.class, () -> failing.register(plugin));

        Assertions.assertNull(Slimefun.getRegistry().getSlimefunItemIds().get("_FAILING_REGISTRATION"), "A failed registration must not stay discoverable via getByID");
        Assertions.assertFalse(Slimefun.getRegistry().getAllSlimefunItems().contains(failing), "A failed registration must not stay in the item registry");
    }

    @Test
    @DisplayName("An item disabled via enable-tickers=false gets the same handler cleanup as any disabled item")
    void testTickersDisabledCleansHandlers() {
        Slimefun.getCfg().setValue("URID.enable-tickers", false);

        try {
            SlimefunItem item = new SlimefunItem(TestUtilities.getItemGroup(plugin, "ticker_off_test"), new SlimefunItemStack("_TICKERS_OFF_ITEM", Material.FURNACE, "&cTicker off"), RecipeType.NULL, new ItemStack[9]);

            item.addItemHandler(new BlockTicker() {

                @Override
                public boolean isSynchronized() {
                    return true;
                }

                @Override
                public void tick(org.bukkit.block.Block b, SlimefunItem item, me.mrCookieSlime.CSCoreLibPlugin.Configuration.Config data) {
                    // Never actually ticked - tickers are disabled
                }
            });

            item.register(plugin);

            Assertions.assertEquals(ItemState.DISABLED, item.getState(), "The item must be disabled");
            Assertions.assertFalse(Slimefun.getRegistry().getTickerBlocks().contains("_TICKERS_OFF_ITEM"), "A disabled ticking item must be removed from tickerBlocks");
            Assertions.assertTrue(item.getHandlers().isEmpty(), "A disabled item must not keep live item handlers");
        } finally {
            Slimefun.getCfg().setValue("URID.enable-tickers", true);
        }
    }
}
