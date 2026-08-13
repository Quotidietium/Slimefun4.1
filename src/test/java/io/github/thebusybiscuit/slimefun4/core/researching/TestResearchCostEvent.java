package io.github.thebusybiscuit.slimefun4.core.researching;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

import io.github.bakedlibs.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun4.api.events.ResearchCostEvent;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile;
import io.github.thebusybiscuit.slimefun4.api.researches.Research;
import io.github.thebusybiscuit.slimefun4.core.guide.SlimefunGuideImplementation;
import io.github.thebusybiscuit.slimefun4.core.guide.SlimefunGuideMode;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Regression coverage for the research API expansion: {@link ResearchCostEvent},
 * exercised by driving the real {@link SlimefunGuideImplementation#unlockItem} cost
 * deduction path.
 * <p>
 * The level deduction happens synchronously before the asynchronous unlock task, so the
 * cost outcome is asserted directly on the player's level. Cancelling makes the unlock
 * free; {@code setCost} adjusts the deduction.
 *
 * @author Zurker
 */
class TestResearchCostEvent {

    private static ServerMock server;
    private static Slimefun plugin;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        Slimefun.getRegistry().setResearchingEnabled(true);
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    @BeforeEach
    public void beforeEach() {
        server.getPluginManager().clearEvents();
    }

    private SlimefunGuideImplementation guide() {
        return Slimefun.getRegistry().getSlimefunGuide(SlimefunGuideMode.SURVIVAL_MODE);
    }

    /**
     * Builds a fresh player with a loaded profile and enough levels, plus a freshly
     * registered research bound to a throwaway Slimefun item.
     */
    private Research prepare(Player player, int id, int cost) throws InterruptedException {
        TestUtilities.awaitProfile(player);

        SlimefunItem item = TestUtilities.mockSlimefunItem(plugin, "RESEARCH_COST_ITEM_" + id, CustomItemStack.create(Material.PAPER, "&fCost Test"));
        item.register(plugin);

        Research research = new Research(new NamespacedKey(plugin, "research_cost_" + id), id, "Cost Test", cost);
        research.register();
        research.addItems(item);

        return research;
    }

    @Test
    @DisplayName("ResearchCostEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Player player = server.addPlayer();
        Research research = new Research(new NamespacedKey(plugin, "research_cost_fields"), 1, "Test", 50);
        research.register();

        ResearchCostEvent event = new ResearchCostEvent(player, research, 50);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(research, event.getResearch());
        Assertions.assertEquals(50, event.getCost());
        Assertions.assertFalse(event.isCancelled());

        event.setCost(20);
        Assertions.assertEquals(20, event.getCost());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new ResearchCostEvent(player, null, 50));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ResearchCostEvent(player, research, -1));
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setCost(-5));
    }

    @Test
    @DisplayName("Unlocking deducts the level cost and fires the event")
    void testUnlockDeductsCost() throws InterruptedException {
        Player player = server.addPlayer();
        player.setLevel(100);
        Research research = prepare(player, 10, 50);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onCost(ResearchCostEvent event) {
                seen[0] = true;
                Assertions.assertEquals(player, event.getPlayer());
                Assertions.assertEquals(research, event.getResearch());
                Assertions.assertEquals(50, event.getCost());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            guide().unlockItem(player, SlimefunItem.getById("RESEARCH_COST_ITEM_10"), pl -> {
            });

            Assertions.assertTrue(seen[0], "ResearchCostEvent was not fired");
            Assertions.assertEquals(50, player.getLevel(), "The level cost must have been deducted");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling ResearchCostEvent makes the unlock free")
    void testCancelMakesFree() throws InterruptedException {
        Player player = server.addPlayer();
        player.setLevel(100);
        Research research = prepare(player, 20, 50);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onCost(ResearchCostEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            guide().unlockItem(player, SlimefunItem.getById("RESEARCH_COST_ITEM_20"), pl -> {
            });

            Assertions.assertEquals(100, player.getLevel(), "A vetoed cost must not deduct any levels");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Adjusting the cost via setCost applies the adjusted deduction")
    void testSetCostDiscount() throws InterruptedException {
        Player player = server.addPlayer();
        player.setLevel(100);
        Research research = prepare(player, 30, 50);

        Listener discounting = new Listener() {
            @EventHandler
            public void onCost(ResearchCostEvent event) {
                event.setCost(20);
            }
        };
        server.getPluginManager().registerEvents(discounting, plugin);

        try {
            guide().unlockItem(player, SlimefunItem.getById("RESEARCH_COST_ITEM_30"), pl -> {
            });

            Assertions.assertEquals(80, player.getLevel(), "The adjusted cost must have been deducted");
        } finally {
            HandlerList.unregisterAll(discounting);
        }
    }

    @Test
    @DisplayName("A surcharge above the player's level refuses the unlock without deducting anything")
    void testSurchargeAboveLevelRefusesUnlock() throws InterruptedException {
        Player player = server.addPlayer();
        player.setLevel(100);
        Research research = prepare(player, 40, 50);

        Listener surcharging = new Listener() {
            @EventHandler
            public void onCost(ResearchCostEvent event) {
                // The base-cost gate passed (100 >= 50), but the surcharge exceeds the player's level
                event.setCost(150);
            }
        };
        server.getPluginManager().registerEvents(surcharging, plugin);

        try {
            guide().unlockItem(player, SlimefunItem.getById("RESEARCH_COST_ITEM_40"), pl -> {
            });

            Assertions.assertEquals(100, player.getLevel(), "An unaffordable surcharge must not deduct any levels");
            PlayerProfile profile = TestUtilities.awaitProfile(player);
            Assertions.assertFalse(profile.hasUnlocked(research), "An unaffordable surcharge must refuse the unlock");
        } finally {
            HandlerList.unregisterAll(surcharging);
        }
    }

    @Test
    @DisplayName("Setting the cost to zero is equivalent to cancelling")
    void testSetCostZero() throws InterruptedException {
        Player player = server.addPlayer();
        player.setLevel(100);
        Research research = prepare(player, 40, 50);

        Listener zeroing = new Listener() {
            @EventHandler
            public void onCost(ResearchCostEvent event) {
                event.setCost(0);
            }
        };
        server.getPluginManager().registerEvents(zeroing, plugin);

        try {
            guide().unlockItem(player, SlimefunItem.getById("RESEARCH_COST_ITEM_40"), pl -> {
            });

            Assertions.assertEquals(100, player.getLevel(), "A zero cost must not deduct any levels");
        } finally {
            HandlerList.unregisterAll(zeroing);
        }
    }

    @Test
    @DisplayName("Unlocking without listeners still deducts the full cost, preserving the old behavior")
    void testUnlockWithoutListenersDeducts() throws InterruptedException {
        Player player = server.addPlayer();
        player.setLevel(100);
        Research research = prepare(player, 50, 50);

        guide().unlockItem(player, SlimefunItem.getById("RESEARCH_COST_ITEM_50"), pl -> {
        });

        Assertions.assertEquals(50, player.getLevel(), "The full level cost must have been deducted");
    }
}
