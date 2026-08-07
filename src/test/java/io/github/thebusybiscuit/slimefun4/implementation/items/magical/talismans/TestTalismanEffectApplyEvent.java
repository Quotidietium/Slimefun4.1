package io.github.thebusybiscuit.slimefun4.implementation.items.magical.talismans;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

import io.github.thebusybiscuit.slimefun4.api.events.TalismanEffectApplyEvent;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Regression coverage for the talisman API expansion: {@link TalismanEffectApplyEvent},
 * exercised by driving the real {@link Talisman#trigger(org.bukkit.event.Event, io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem)}
 * with a constructed {@link BlockBreakEvent} while the player carries the talisman.
 * <p>
 * The test talisman is silent and non-consumable with a 100% chance, so activation is
 * deterministic: the potion effects landing on the player and the cancellation of the
 * triggering {@link BlockBreakEvent} are asserted end-to-end.
 *
 * @author Zurker
 */
class TestTalismanEffectApplyEvent {

    private static final PotionEffect SPEED = new PotionEffect(PotionEffectType.SPEED, 200, 1);

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static Talisman talisman;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        // ProtectionManager is created when the integrations start
        Slimefun.getIntegrations().start();
        server.getScheduler().performOneTick();

        SlimefunItemStack stack = new SlimefunItemStack("_TEST_TALISMAN", Material.EMERALD, "&aTest Talisman", "&7A test talisman");
        Slimefun.getItemCfg().setValue("_TEST_TALISMAN.enabled", true);
        talisman = new Talisman(stack, new ItemStack[9], false, true, null, 100, SPEED);
        talisman.register(plugin);
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
     * Gives the player the talisman and triggers it via a block break.
     *
     * @return the {@link BlockBreakEvent} that was driven, for cancellation assertions
     */
    private BlockBreakEvent activate(Player player, int x, int z) {
        player.getInventory().setItem(0, talisman.getItem());

        Block b = world.getBlockAt(x, 1, z);
        b.setType(Material.STONE);

        BlockBreakEvent event = new BlockBreakEvent(b, player);
        Talisman.trigger(event, talisman);
        return event;
    }

    @Test
    @DisplayName("TalismanEffectApplyEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Player player = server.addPlayer();
        ItemStack talismanItem = talisman.getItem();

        TalismanEffectApplyEvent event = new TalismanEffectApplyEvent(player, talisman, talismanItem, List.of(SPEED));

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(talisman, event.getTalisman());
        Assertions.assertEquals(talismanItem, event.getTalismanItem());
        Assertions.assertEquals(List.of(SPEED), event.getEffects());
        Assertions.assertFalse(event.isCancelled());

        List<PotionEffect> swapped = List.of(new PotionEffect(PotionEffectType.SLOWNESS, 100, 0));
        event.setEffects(swapped);
        Assertions.assertEquals(swapped, event.getEffects());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new TalismanEffectApplyEvent(player, null, talismanItem, List.of(SPEED)));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new TalismanEffectApplyEvent(player, talisman, null, List.of(SPEED)));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new TalismanEffectApplyEvent(player, talisman, talismanItem, null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setEffects(null));
    }

    @Test
    @DisplayName("Triggering a talisman fires the event and applies its effects")
    void testTriggerFiresEventAndAppliesEffects() {
        Player player = server.addPlayer();

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onApply(TalismanEffectApplyEvent event) {
                seen[0] = true;
                Assertions.assertEquals(talisman, event.getTalisman());
                Assertions.assertEquals(1, event.getEffects().size());
                Assertions.assertEquals(PotionEffectType.SPEED, event.getEffects().get(0).getType());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            BlockBreakEvent event = activate(player, 10, 10);

            Assertions.assertTrue(seen[0], "TalismanEffectApplyEvent was not fired");
            Assertions.assertTrue(player.hasPotionEffect(PotionEffectType.SPEED), "The talisman effect must have been applied");
            Assertions.assertTrue(event.isCancelled(), "The triggering event must still be cancelled by the talisman");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling TalismanEffectApplyEvent skips only the potion application")
    void testCancelSkipsOnlyEffects() {
        Player player = server.addPlayer();

        Listener cancelling = new Listener() {
            @EventHandler
            public void onApply(TalismanEffectApplyEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            BlockBreakEvent event = activate(player, 20, 20);

            Assertions.assertFalse(player.hasPotionEffect(PotionEffectType.SPEED), "A cancelled application must not apply any effect");
            Assertions.assertTrue(event.isCancelled(), "The triggering event must still be cancelled by the talisman");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Replacing the effects via setEffects applies the new effects")
    void testSetEffectsReplacesEffects() {
        Player player = server.addPlayer();

        Listener replacing = new Listener() {
            @EventHandler
            public void onApply(TalismanEffectApplyEvent event) {
                event.setEffects(List.of(new PotionEffect(PotionEffectType.SLOWNESS, 100, 0)));
            }
        };
        server.getPluginManager().registerEvents(replacing, plugin);

        try {
            activate(player, 30, 30);

            Assertions.assertTrue(player.hasPotionEffect(PotionEffectType.SLOWNESS), "The replaced effect must have been applied");
            Assertions.assertFalse(player.hasPotionEffect(PotionEffectType.SPEED), "The original effect must not have been applied");
        } finally {
            HandlerList.unregisterAll(replacing);
        }
    }

    @Test
    @DisplayName("Mutating the live effects list via getEffects applies the mutation")
    void testLiveEffectsListMutation() {
        Player player = server.addPlayer();

        Listener mutating = new Listener() {
            @EventHandler
            public void onApply(TalismanEffectApplyEvent event) {
                event.getEffects().clear();
                event.getEffects().add(new PotionEffect(PotionEffectType.JUMP_BOOST, 100, 2));
            }
        };
        server.getPluginManager().registerEvents(mutating, plugin);

        try {
            activate(player, 40, 40);

            Assertions.assertTrue(player.hasPotionEffect(PotionEffectType.JUMP_BOOST), "The added effect must have been applied");
            Assertions.assertFalse(player.hasPotionEffect(PotionEffectType.SPEED), "The cleared effect must not have been applied");
        } finally {
            HandlerList.unregisterAll(mutating);
        }
    }

    @Test
    @DisplayName("Triggering without listeners still applies the default effects, preserving the old behavior")
    void testTriggerWithoutListenersAppliesDefaultEffects() {
        Player player = server.addPlayer();

        BlockBreakEvent event = activate(player, 50, 50);

        Assertions.assertTrue(player.hasPotionEffect(PotionEffectType.SPEED), "The default talisman effect must have been applied");
        Assertions.assertTrue(event.isCancelled(), "The triggering event must still be cancelled by the talisman");
    }
}
