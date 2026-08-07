package io.github.thebusybiscuit.slimefun4.implementation.tasks.armor;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

import io.github.thebusybiscuit.slimefun4.api.events.SlimefunArmorChangeEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.armor.SlimefunArmorPiece;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Regression coverage for the armor API expansion: {@link SlimefunArmorChangeEvent},
 * exercised by driving a real {@link SlimefunArmorTask} player tick against players
 * whose armor slots change between ticks.
 * <p>
 * Equipping a {@link SlimefunArmorPiece} fires the event with no previous armor,
 * unequipping fires it with no new armor, and a repeated tick with unchanged armor
 * stays silent because the armor cache was already updated. A swap between two
 * vanilla pieces never fires, and a pure durability change does not count as a
 * change at all (the armor hash ignores damage by design).
 *
 * @author Zurker
 */
class TestSlimefunArmorChangeEvent {

    private static ServerMock server;
    private static Slimefun plugin;
    private static SlimefunArmorTask task;
    private static SlimefunArmorPiece armorPiece;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        task = new SlimefunArmorTask();

        // Non-configurable items stay DISABLED unless Items.yml says otherwise, enable it first
        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "armor_change_test");
        SlimefunItemStack stack = new SlimefunItemStack("_TEST_CHANGE_BOOTS", Material.LEATHER_BOOTS, "&fTest Change Boots");
        Slimefun.getItemCfg().setValue("_TEST_CHANGE_BOOTS.enabled", true);

        // No potion effects: the per-tick effect pipeline is SlimefunArmorEffectEvent's business
        armorPiece = new SlimefunArmorPiece(itemGroup, stack, RecipeType.NULL, new ItemStack[9], null);
        armorPiece.register(plugin);
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
     * Loads the {@link PlayerProfile} for the given {@link Player}, waiting for the
     * asynchronous load to finish.
     */
    private static PlayerProfile profileOf(Player p) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<PlayerProfile> ref = new AtomicReference<>();
        PlayerProfile.get(p, profile -> {
            ref.set(profile);
            latch.countDown();
        });
        Assertions.assertTrue(latch.await(2, TimeUnit.SECONDS), "The PlayerProfile did not load in time");
        return ref.get();
    }

    /**
     * Runs one player tick of a real {@link SlimefunArmorTask}. The task lives in
     * the same package, so the protected tick method is directly reachable here.
     */
    private void tick(Player p, PlayerProfile profile) {
        task.onPlayerTick(p, profile);
    }

    /**
     * Registers a listener capturing the next {@link SlimefunArmorChangeEvent} and
     * returns the capture. Unregister with {@link HandlerList#unregisterAll(Listener)}.
     */
    private Listener watch(AtomicReference<SlimefunArmorChangeEvent> capture) {
        Listener watcher = new Listener() {
            @EventHandler
            public void onArmorChange(SlimefunArmorChangeEvent event) {
                capture.set(event);
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);
        return watcher;
    }

    private static void wear(Player p, ItemStack armor) {
        p.getInventory().setArmorContents(new ItemStack[] { armor, null, null, null });
    }

    @Test
    @DisplayName("SlimefunArmorChangeEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Player p = server.addPlayer();
        ItemStack boots = armorPiece.getItem();

        SlimefunArmorChangeEvent event = new SlimefunArmorChangeEvent(p, 0, null, boots, armorPiece);

        Assertions.assertEquals(p, event.getPlayer());
        Assertions.assertEquals(0, event.getSlot());
        Assertions.assertNull(event.getPreviousArmor());
        Assertions.assertEquals(boots, event.getNewItem());
        Assertions.assertEquals(armorPiece, event.getNewArmor());
        Assertions.assertFalse(event.isAsynchronous(), "Constructed on the main thread, the event must be synchronous");

        Assertions.assertThrows(IllegalArgumentException.class, () -> new SlimefunArmorChangeEvent(null, 0, null, boots, armorPiece));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new SlimefunArmorChangeEvent(p, -1, null, boots, armorPiece));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new SlimefunArmorChangeEvent(p, 4, null, boots, armorPiece));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new SlimefunArmorChangeEvent(p, 0, null, boots, null), "Both sides being non-Slimefun is not a Slimefun armor change");
    }

    @Test
    @DisplayName("Equipping a SlimefunArmorPiece fires the event with no previous armor")
    void testEquipFiresEvent() throws InterruptedException {
        Player p = server.addPlayer();
        PlayerProfile profile = profileOf(p);
        wear(p, armorPiece.getItem());

        AtomicReference<SlimefunArmorChangeEvent> seen = new AtomicReference<>();
        Listener watcher = watch(seen);

        try {
            tick(p, profile);

            SlimefunArmorChangeEvent event = seen.get();
            Assertions.assertNotNull(event, "SlimefunArmorChangeEvent was not fired");
            Assertions.assertEquals(p, event.getPlayer());
            Assertions.assertEquals(0, event.getSlot());
            Assertions.assertNull(event.getPreviousArmor(), "Equipping into an empty slot must have no previous armor");
            Assertions.assertEquals(armorPiece, event.getNewArmor());
            Assertions.assertNotNull(event.getNewItem());
            Assertions.assertSame(armorPiece, SlimefunItem.getByItem(event.getNewItem()), "The new item must resolve to the equipped armor piece");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Unequipping a SlimefunArmorPiece fires the event with no new armor")
    void testUnequipFiresEvent() throws InterruptedException {
        Player p = server.addPlayer();
        PlayerProfile profile = profileOf(p);
        wear(p, armorPiece.getItem());

        // Let the armor cache learn the piece without listeners first
        tick(p, profile);

        AtomicReference<SlimefunArmorChangeEvent> seen = new AtomicReference<>();
        Listener watcher = watch(seen);

        try {
            wear(p, null);
            tick(p, profile);

            SlimefunArmorChangeEvent event = seen.get();
            Assertions.assertNotNull(event, "SlimefunArmorChangeEvent was not fired");
            Assertions.assertEquals(0, event.getSlot());
            Assertions.assertEquals(armorPiece, event.getPreviousArmor(), "The unequipped piece must be reported as the previous armor");
            Assertions.assertNull(event.getNewArmor(), "Unequipping must have no new armor");

            ItemStack newItem = event.getNewItem();
            Assertions.assertTrue(newItem == null || newItem.getType().isAir(), "The emptied slot must read as null or air, got: " + newItem);
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Unchanged armor fires no event on the following tick")
    void testUnchangedArmorStaysSilent() throws InterruptedException {
        Player p = server.addPlayer();
        PlayerProfile profile = profileOf(p);
        wear(p, armorPiece.getItem());

        AtomicReference<SlimefunArmorChangeEvent> seen = new AtomicReference<>();
        Listener watcher = watch(seen);

        try {
            tick(p, profile);
            Assertions.assertNotNull(seen.get(), "The first tick must have detected the equip");

            seen.set(null);
            tick(p, profile);
            Assertions.assertNull(seen.get(), "An unchanged armor slot must not fire again");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("A swap between vanilla armor pieces fires no event")
    void testVanillaSwapStaysSilent() throws InterruptedException {
        Player p = server.addPlayer();
        PlayerProfile profile = profileOf(p);
        wear(p, new ItemStack(Material.LEATHER_BOOTS));

        AtomicReference<SlimefunArmorChangeEvent> seen = new AtomicReference<>();
        Listener watcher = watch(seen);

        try {
            tick(p, profile);
            Assertions.assertNull(seen.get(), "Vanilla armor appearing must not fire the event");

            wear(p, new ItemStack(Material.IRON_BOOTS));
            tick(p, profile);
            Assertions.assertNull(seen.get(), "A vanilla to vanilla swap must not fire the event");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Equipping without listeners still updates the armor cache, preserving the old behavior")
    void testEquipWithoutListenersUpdatesCache() throws InterruptedException {
        Player p = server.addPlayer();
        PlayerProfile profile = profileOf(p);
        wear(p, armorPiece.getItem());

        // No listeners: the tick must update the armor cache silently
        tick(p, profile);

        AtomicReference<SlimefunArmorChangeEvent> seen = new AtomicReference<>();
        Listener watcher = watch(seen);

        try {
            tick(p, profile);
            Assertions.assertNull(seen.get(), "The cache must have been updated by the first tick already");
            Assertions.assertTrue(profile.getArmor()[0].getItem().isPresent(), "The armor cache must know the equipped piece");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("A pure durability change does not count as an armor change")
    void testDurabilityChangeStaysSilent() throws InterruptedException {
        Player p = server.addPlayer();
        PlayerProfile profile = profileOf(p);
        wear(p, armorPiece.getItem());

        // Let the armor cache learn the piece without listeners first
        tick(p, profile);

        AtomicReference<SlimefunArmorChangeEvent> seen = new AtomicReference<>();
        Listener watcher = watch(seen);

        try {
            ItemStack[] armor = p.getInventory().getArmorContents();
            ItemMeta meta = armor[0].getItemMeta();
            ((Damageable) meta).setDamage(10);
            armor[0].setItemMeta(meta);
            p.getInventory().setArmorContents(armor);

            tick(p, profile);

            Assertions.assertNull(seen.get(), "The armor hash ignores durability, so a damaged piece must not fire");
            Assertions.assertEquals(10, ((Damageable) p.getInventory().getArmorContents()[0].getItemMeta()).getDamage(), "The damage must really have reached the slot");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }
}
