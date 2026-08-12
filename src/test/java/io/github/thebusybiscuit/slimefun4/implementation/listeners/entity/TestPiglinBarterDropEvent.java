package io.github.thebusybiscuit.slimefun4.implementation.listeners.entity;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nonnull;

import org.bukkit.Material;
import org.bukkit.entity.Piglin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDropItemEvent;
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
import be.seeseemelk.mockbukkit.entity.ItemEntityMock;

import io.github.thebusybiscuit.slimefun4.api.events.PiglinBarterDropEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.PiglinBarterDrop;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Regression coverage for the piglin bartering API expansion:
 * {@link PiglinBarterDropEvent}, exercised through the real {@link PiglinListener}
 * drop replacement path.
 * <p>
 * The barter roll is random (a 99% chance item fails on a roll of 99), so this test
 * registers five independent barter items: every roll is independent, leaving a
 * 10^-10 chance that none of them wins, which is deterministic for practical purposes.
 *
 * @author Zurker
 */
class TestPiglinBarterDropEvent {

    private static ServerMock server;
    private static Slimefun plugin;

    private static final Set<SlimefunItem> barterItems = new LinkedHashSet<>();

    private static class TestBarterItem extends SlimefunItem implements PiglinBarterDrop {

        TestBarterItem(@Nonnull ItemGroup itemGroup, @Nonnull SlimefunItemStack item) {
            super(itemGroup, item, RecipeType.BARTER_DROP, new ItemStack[9]);
        }

        @Override
        public int getBarteringLootChance() {
            return 99;
        }
    }

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        TestUtilities.createWorld(server);

        // Unit test startups never start the integrations, the ProtectionManager is created there
        Slimefun.getIntegrations().start();
        server.getScheduler().performOneTick();

        // Unit test startups do not register the listeners, register it manually
        new PiglinListener(plugin);

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "piglin_barter_test");

        for (int i = 1; i <= 5; i++) {
            SlimefunItemStack stack = new SlimefunItemStack("TEST_BARTER_DROP_" + i, Material.GOLD_INGOT, "&6Test Barter Drop " + i);
            SlimefunItem item = new TestBarterItem(itemGroup, stack);
            item.register(plugin);
            barterItems.add(item);

            // load() is not invoked in unit tests, so the RecipeType.BARTER_DROP registration
            // callback never runs; this mirrors it exactly (RecipeType#registerBarterDrop).
            Slimefun.getRegistry().getBarteringDrops().add(item.getRecipeOutput());
        }
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
     * Dispatches a vanilla {@link EntityDropItemEvent} (a plain gold ingot) for a
     * mocked {@link Piglin}, running the real barter replacement path.
     */
    private ItemEntityMock dropVanillaBarter() {
        Piglin piglin = Mockito.mock(Piglin.class);
        ItemEntityMock itemDrop = new ItemEntityMock(server, UUID.randomUUID(), new ItemStack(Material.GOLD_INGOT));
        server.getPluginManager().callEvent(new EntityDropItemEvent(piglin, itemDrop));
        return itemDrop;
    }

    private boolean isTestBarterDrop(ItemStack stack) {
        SlimefunItem sfItem = SlimefunItem.getByItem(stack);
        return sfItem != null && barterItems.contains(sfItem);
    }

    @Test
    @DisplayName("PiglinBarterDropEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Piglin piglin = Mockito.mock(Piglin.class);
        ItemEntityMock itemDrop = new ItemEntityMock(server, UUID.randomUUID(), new ItemStack(Material.GOLD_INGOT));
        SlimefunItem barterItem = barterItems.iterator().next();

        PiglinBarterDropEvent event = new PiglinBarterDropEvent(piglin, itemDrop, barterItem, 99);

        Assertions.assertEquals(piglin, event.getEntity());
        Assertions.assertEquals(piglin, event.getPiglin());
        Assertions.assertEquals(itemDrop, event.getItemDrop());
        Assertions.assertEquals(barterItem, event.getSlimefunItem());
        Assertions.assertEquals(99, event.getChance());
        Assertions.assertFalse(event.isCancelled());

        // The drop defaults to the winning item's recipe output
        Assertions.assertTrue(event.getDrop().isSimilar(barterItem.getRecipeOutput()), "The drop must default to the recipe output");

        ItemStack replacement = new ItemStack(Material.DIAMOND, 3);
        event.setDrop(replacement);
        Assertions.assertEquals(replacement, event.getDrop());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new PiglinBarterDropEvent(piglin, null, barterItem, 99));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new PiglinBarterDropEvent(piglin, itemDrop, null, 99));
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setDrop(null));
    }

    @Test
    @DisplayName("A winning barter roll fires the event and replaces the vanilla drop")
    void testBarterFiresAndReplaces() {
        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onBarterDrop(PiglinBarterDropEvent event) {
                seen[0] = true;
                Assertions.assertTrue(barterItems.contains(event.getSlimefunItem()), "The winning item must be one of the test barter items");
                Assertions.assertEquals(99, event.getChance());
                Assertions.assertNotNull(event.getItemDrop());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            ItemEntityMock itemDrop = dropVanillaBarter();

            Assertions.assertTrue(seen[0], "PiglinBarterDropEvent was not fired");
            Assertions.assertTrue(isTestBarterDrop(itemDrop.getItemStack()), "The vanilla drop must have been replaced with a barter drop");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Replacing the drop via setDrop makes the piglin drop the replacement")
    void testSetDropRedirectsBarter() {
        Listener redirecting = new Listener() {
            @EventHandler
            public void onBarterDrop(PiglinBarterDropEvent event) {
                Assertions.assertTrue(event.getDrop().isSimilar(event.getSlimefunItem().getRecipeOutput()), "The drop must default to the recipe output");
                event.setDrop(new ItemStack(Material.DIAMOND, 3));
            }
        };
        server.getPluginManager().registerEvents(redirecting, plugin);

        try {
            ItemEntityMock itemDrop = dropVanillaBarter();

            Assertions.assertEquals(Material.DIAMOND, itemDrop.getItemStack().getType(), "The drop must have been replaced with the custom stack");
            Assertions.assertEquals(3, itemDrop.getItemStack().getAmount(), "The replacement's amount must be kept");
        } finally {
            HandlerList.unregisterAll(redirecting);
        }
    }

    @Test
    @DisplayName("Cancelling PiglinBarterDropEvent keeps the vanilla drop")
    void testBarterCancellationKeepsVanillaDrop() {
        AtomicBoolean seen = new AtomicBoolean(false);
        Listener cancelling = new Listener() {
            @EventHandler
            public void onBarterDrop(PiglinBarterDropEvent event) {
                seen.set(true);
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            ItemEntityMock itemDrop = dropVanillaBarter();

            Assertions.assertTrue(seen.get(), "PiglinBarterDropEvent was not fired");
            Assertions.assertEquals(Material.GOLD_INGOT, itemDrop.getItemStack().getType(), "A cancelled barter must keep the vanilla drop");
            Assertions.assertNull(SlimefunItem.getByItem(itemDrop.getItemStack()), "A cancelled barter must keep the vanilla drop");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("A barter drop without listeners is still replaced, preserving the old behavior")
    void testBarterWithoutListenersStillReplaces() {
        ItemEntityMock itemDrop = dropVanillaBarter();

        Assertions.assertTrue(isTestBarterDrop(itemDrop.getItemStack()), "The vanilla drop must have been replaced with a barter drop");
    }

    @Test
    @DisplayName("A non-piglin drop fires no event and stays untouched")
    void testNonPiglinDropFiresNothing() {
        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onBarterDrop(PiglinBarterDropEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            org.bukkit.entity.Player player = server.addPlayer();
            ItemEntityMock itemDrop = new ItemEntityMock(server, UUID.randomUUID(), new ItemStack(Material.GOLD_INGOT));
            server.getPluginManager().callEvent(new EntityDropItemEvent(player, itemDrop));

            Assertions.assertFalse(seen[0], "No event must be fired for a non-piglin drop");
            Assertions.assertEquals(Material.GOLD_INGOT, itemDrop.getItemStack().getType(), "A non-piglin drop must stay untouched");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }
}
