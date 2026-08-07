package io.github.thebusybiscuit.slimefun4.implementation.items.electric.reactors;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Cow;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

import io.github.thebusybiscuit.slimefun4.api.events.NetherStarReactorWitherEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

import me.mrCookieSlime.Slimefun.api.BlockStorage;

/**
 * Regression coverage for the reactor wither API expansion:
 * {@link NetherStarReactorWitherEvent}, exercised by driving the real
 * {@link NetherStarReactor#extraTick(Location)} and running the scheduled sync task it
 * enqueues, with a cow standing inside the reactor's range.
 * <p>
 * The wither effect lands on the entity's real potion effect map, so tests assert it
 * end-to-end: a cancelled event leaves the entity unwithered.
 *
 * @author Zurker
 */
class TestNetherStarReactorWitherEvent {

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static NetherStarReactor reactor;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        // Unit test startups never start the integrations, the ProtectionManager is created there
        Slimefun.getIntegrations().start();
        server.getScheduler().performOneTick();

        // Non-configurable items stay DISABLED unless Items.yml says otherwise, enable it first
        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "nether_star_reactor_test");
        SlimefunItemStack stack = new SlimefunItemStack("TEST_NETHER_STAR_REACTOR", Material.DISPENSER, "&fTest Nether Star Reactor");
        Slimefun.getItemCfg().setValue("TEST_NETHER_STAR_REACTOR.enabled", true);
        reactor = new NetherStarReactor(itemGroup, stack, RecipeType.NULL, new ItemStack[9]) {
            @Override
            public int getEnergyProduction() {
                return 100;
            }

            @Override
            public int getCapacity() {
                return 512;
            }
        };
        reactor.register(plugin);
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
     * Places the reactor as a real block backed by {@link BlockStorage} (without owner data,
     * so the withering is not gated by claim protection).
     */
    private Block placeReactor(int x, int z) {
        Block b = world.getBlockAt(x, 1, z);
        b.setType(Material.DISPENSER);
        BlockStorage.addBlockInfo(b, "id", reactor.getId(), true);
        return b;
    }

    /**
     * Runs one extra tick: the reactor schedules its withering sweep via
     * {@link Slimefun#runSync(Runnable)}, so one scheduler tick executes it.
     */
    private void extraTick(Block b) {
        reactor.extraTick(b.getLocation());
        server.getScheduler().performOneTick();
    }

    private Cow spawnCow(double x, double z) {
        return world.spawn(new Location(world, x, 1, z), Cow.class);
    }

    @Test
    @DisplayName("NetherStarReactorWitherEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Location l = new Location(world, 1, 1, 1);
        Cow cow = spawnCow(2, 1);

        NetherStarReactorWitherEvent event = new NetherStarReactorWitherEvent(reactor, l, cow);

        Assertions.assertEquals(reactor, event.getReactor());
        Assertions.assertEquals(l, event.getLocation());
        Assertions.assertEquals(cow, event.getEntity());
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new NetherStarReactorWitherEvent(null, l, cow));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new NetherStarReactorWitherEvent(reactor, null, cow));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new NetherStarReactorWitherEvent(reactor, l, null));
    }

    @Test
    @DisplayName("An extra tick fires the event and withers the entity in range")
    void testTickFiresEventAndWithers() {
        Block b = placeReactor(10, 10);
        Cow cow = spawnCow(11, 10);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onWither(NetherStarReactorWitherEvent event) {
                seen[0] = true;
                Assertions.assertEquals(reactor, event.getReactor());
                Assertions.assertEquals(cow, event.getEntity());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            extraTick(b);

            Assertions.assertTrue(seen[0], "NetherStarReactorWitherEvent was not fired");
            Assertions.assertTrue(cow.hasPotionEffect(PotionEffectType.WITHER), "The entity in range must have been withered");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling NetherStarReactorWitherEvent leaves the entity unwithered")
    void testCancelLeavesEntityUnwithered() {
        Block b = placeReactor(20, 20);
        Cow cow = spawnCow(21, 20);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onWither(NetherStarReactorWitherEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            extraTick(b);

            Assertions.assertFalse(cow.hasPotionEffect(PotionEffectType.WITHER), "A cancelled withering must not apply the effect");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Withering without listeners still applies, preserving the old behavior")
    void testTickWithoutListenersStillWithers() {
        Block b = placeReactor(30, 30);
        Cow cow = spawnCow(31, 30);

        extraTick(b);

        Assertions.assertTrue(cow.hasPotionEffect(PotionEffectType.WITHER), "The entity in range must have been withered");
    }

    @Test
    @DisplayName("An entity outside the reactor's range fires no event")
    void testEntityOutOfRangeFiresNothing() {
        Block b = placeReactor(40, 40);
        Cow cow = spawnCow(47, 40);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onWither(NetherStarReactorWitherEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            extraTick(b);

            Assertions.assertFalse(seen[0], "No event must be fired for an entity out of range");
            Assertions.assertFalse(cow.hasPotionEffect(PotionEffectType.WITHER), "An entity out of range must not be withered");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Every entity in range fires its own event")
    void testEachEntityInRangeFiresEvent() {
        Block b = placeReactor(50, 50);
        Cow first = spawnCow(51, 50);
        Cow second = spawnCow(50, 51);

        int[] count = { 0 };
        Listener watcher = new Listener() {
            @EventHandler
            public void onWither(NetherStarReactorWitherEvent event) {
                count[0]++;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            extraTick(b);

            Assertions.assertTrue(count[0] >= 2, "Each entity in range must fire its own event, got: " + count[0]);
            Assertions.assertTrue(first.hasPotionEffect(PotionEffectType.WITHER), "The first entity must have been withered");
            Assertions.assertTrue(second.hasPotionEffect(PotionEffectType.WITHER), "The second entity must have been withered");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }
}
