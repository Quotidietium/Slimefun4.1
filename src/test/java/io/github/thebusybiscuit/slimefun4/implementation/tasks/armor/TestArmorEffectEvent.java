package io.github.thebusybiscuit.slimefun4.implementation.tasks.armor;

import java.util.Arrays;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
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

import io.github.thebusybiscuit.slimefun4.api.events.SlimefunArmorEffectEvent;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.armor.SlimefunArmorPiece;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Regression coverage for the armor API expansion: {@link SlimefunArmorEffectEvent},
 * exercised through the production {@link SlimefunArmorTask} pipeline.
 *
 * @author Zurker
 */
class TestArmorEffectEvent {

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

    @BeforeEach
    public void beforeEach() {
        server.getPluginManager().clearEvents();
    }

    private SlimefunArmorPiece registerArmor(String id, PotionEffect[] effects) {
        SlimefunItemStack helmet = new SlimefunItemStack(id, Material.IRON_HELMET, "&bTest Helmet");
        SlimefunArmorPiece armor = new SlimefunArmorPiece(TestUtilities.getItemGroup(plugin, "armor_effect_test"), helmet, RecipeType.NULL, new ItemStack[9], effects);
        armor.register(plugin);
        return armor;
    }

    @Test
    @DisplayName("SlimefunArmorEffectEvent fires through the armor task with the right context")
    void testArmorEffectEventFired() throws InterruptedException {
        Player player = server.addPlayer();
        TestUtilities.awaitProfile(player);

        PotionEffect[] effects = { new PotionEffect(PotionEffectType.SPEED, 50, 3) };
        SlimefunArmorPiece armor = registerArmor("EVENT_HELMET_FIRED", effects);
        player.getInventory().setHelmet(armor.getItem());

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onEffect(SlimefunArmorEffectEvent event) {
                // Other Players from earlier tests are still online - only look at our own
                if (!event.getPlayer().equals(player)) {
                    return;
                }

                seen[0] = true;
                Assertions.assertEquals(player, event.getPlayer());
                Assertions.assertEquals(armor, event.getArmorItem());
                Assertions.assertArrayEquals(effects, event.getEffects());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            new SlimefunArmorTask().run();

            Assertions.assertTrue(seen[0], "SlimefunArmorEffectEvent was not fired");
            Assertions.assertTrue(player.getActivePotionEffects().containsAll(Arrays.asList(effects)));
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling SlimefunArmorEffectEvent prevents the effects from being applied")
    void testArmorEffectEventCancellation() throws InterruptedException {
        Player player = server.addPlayer();
        TestUtilities.awaitProfile(player);

        PotionEffect[] effects = { new PotionEffect(PotionEffectType.JUMP_BOOST, 50, 3) };
        SlimefunArmorPiece armor = registerArmor("EVENT_HELMET_CANCELLED", effects);
        player.getInventory().setHelmet(armor.getItem());

        Listener cancelling = new Listener() {
            @EventHandler
            public void onEffect(SlimefunArmorEffectEvent event) {
                if (event.getPlayer().equals(player)) {
                    event.setCancelled(true);
                }
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            new SlimefunArmorTask().run();

            Assertions.assertFalse(player.getActivePotionEffects().containsAll(Arrays.asList(effects)));
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("No event is fired for armor pieces without potion effects")
    void testNoEventForEffectlessArmor() throws InterruptedException {
        Player player = server.addPlayer();
        TestUtilities.awaitProfile(player);

        SlimefunArmorPiece armor = registerArmor("EVENT_HELMET_EFFECTLESS", null);
        player.getInventory().setHelmet(armor.getItem());

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onEffect(SlimefunArmorEffectEvent event) {
                // Other Players from earlier tests are still online - only look at our own
                if (event.getPlayer().equals(player)) {
                    seen[0] = true;
                }
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            new SlimefunArmorTask().run();
            Assertions.assertFalse(seen[0], "Event must not fire for effect-less armor");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }
}
