package io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines.accelerators;

import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.CowMock;

import io.github.thebusybiscuit.slimefun4.api.events.AnimalAccelerateEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;

/**
 * Regression coverage for the animal growth accelerator API expansion:
 * {@link AnimalAccelerateEvent}, exercised through the real
 * {@link AnimalGrowthAccelerator#tick(Block)} growth path with real entity mocks.
 *
 * @author Zurker
 */
class TestAnimalAccelerateEvent {

    private static final int ENERGY_CONSUMPTION = 14;

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static AnimalGrowthAccelerator accelerator;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        // Unit test startups never start the integrations, the ProtectionManager is created there
        Slimefun.getIntegrations().start();
        server.getScheduler().performOneTick();

        // A BlockTicker item stays DISABLED while tickers are off and non-configurable items
        // stay DISABLED unless Items.yml says otherwise, so enable both before registering.
        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "animal_accelerator_test");
        SlimefunItemStack stack = new SlimefunItemStack("TEST_ANIMAL_ACCELERATOR", Material.DISPENSER, "&eTest Animal Accelerator");
        Slimefun.getCfg().setValue("URID.enable-tickers", true);
        Slimefun.getItemCfg().setValue("TEST_ANIMAL_ACCELERATOR.enabled", true);
        accelerator = new AnimalGrowthAccelerator(itemGroup, stack, RecipeType.NULL, new ItemStack[9]);
        accelerator.register(plugin);
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
     * Builds a charged accelerator with organic food in its first input slot at the
     * given position.
     */
    private Block setupAccelerator(int x, int z) {
        Block b = world.getBlockAt(x, 1, z);
        b.setType(Material.DISPENSER);
        BlockStorage.addBlockInfo(b, "id", accelerator.getId(), true);
        BlockStorage.addBlockInfo(b.getLocation(), "energy-charge", "1024", false);

        BlockMenu menu = BlockStorage.getInventory(b);
        menu.replaceExistingItem(10, SlimefunItems.ORGANIC_FOOD.item().clone());
        return b;
    }

    /**
     * Spawns a cow with the given age next to the accelerator and registers it with
     * the server. A negative age makes it a baby.
     */
    private CowMock spawnCow(int x, int z, int age) {
        CowMock cow = new CowMock(server, UUID.randomUUID());
        cow.setAge(age);
        cow.setLocation(new Location(world, x + 1, 1, z));
        server.registerEntity(cow);
        return cow;
    }

    /**
     * Ticks the accelerator once. The growth path ends in a particle effect that
     * MockBukkit does not fully support, so a RuntimeException from that tail is
     * ignored here - the event was fired and the resources consumed beforehand.
     */
    private void tick(Block b) {
        try {
            accelerator.tick(b);
        } catch (RuntimeException ignored) {
            // See the javadoc above
        }
    }

    private void removeCow(CowMock cow) {
        if (cow.isValid()) {
            cow.remove();
        }
        server.unregisterEntity(cow);
    }

    @Test
    @DisplayName("AnimalAccelerateEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Block b = world.getBlockAt(1, 1, 1);
        CowMock cow = new CowMock(server, UUID.randomUUID());
        ItemStack food = SlimefunItems.ORGANIC_FOOD.item().clone();

        AnimalAccelerateEvent event = new AnimalAccelerateEvent(accelerator, b, cow, food);

        Assertions.assertEquals(accelerator, event.getAccelerator());
        Assertions.assertEquals(b, event.getBlock());
        Assertions.assertEquals(cow, event.getAnimal());
        Assertions.assertEquals(food, event.getFood());
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new AnimalAccelerateEvent(null, b, cow, food));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new AnimalAccelerateEvent(accelerator, null, cow, food));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new AnimalAccelerateEvent(accelerator, b, null, food));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new AnimalAccelerateEvent(accelerator, b, cow, null));
    }

    @Test
    @DisplayName("Accelerating a baby animal fires the event, consumes energy and food and grows it")
    void testAccelerateFiresAndGrows() {
        Block b = setupAccelerator(10, 10);
        CowMock cow = spawnCow(10, 10, -10000);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onAccelerate(AnimalAccelerateEvent event) {
                seen[0] = true;
                Assertions.assertEquals(accelerator, event.getAccelerator());
                Assertions.assertEquals(b, event.getBlock());
                Assertions.assertEquals(cow, event.getAnimal());
                Assertions.assertTrue(SlimefunItems.ORGANIC_FOOD.item().isSimilar(event.getFood()), "The food must be organic food");
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            tick(b);

            Assertions.assertTrue(seen[0], "AnimalAccelerateEvent was not fired");
            Assertions.assertEquals(1024 - ENERGY_CONSUMPTION, accelerator.getCharge(b.getLocation()), "The energy must have been consumed");
            Assertions.assertEquals(0, BlockStorage.getInventory(b).getItemInSlot(10).getAmount(), "The organic food must have been consumed");
            Assertions.assertEquals(-8000, cow.getAge(), "The cow must have grown by 2000 ticks");
        } finally {
            HandlerList.unregisterAll(watcher);
            removeCow(cow);
        }
    }

    @Test
    @DisplayName("Cancelling AnimalAccelerateEvent skips the animal and keeps the resources")
    void testEventCancellationSkipsAnimal() {
        Block b = setupAccelerator(20, 20);
        CowMock cow = spawnCow(20, 20, -10000);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onAccelerate(AnimalAccelerateEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            tick(b);

            Assertions.assertEquals(1024, accelerator.getCharge(b.getLocation()), "A cancelled acceleration must keep the energy");
            Assertions.assertEquals(1, BlockStorage.getInventory(b).getItemInSlot(10).getAmount(), "A cancelled acceleration must keep the food");
            Assertions.assertEquals(-10000, cow.getAge(), "A cancelled acceleration must leave the animal's age alone");
        } finally {
            HandlerList.unregisterAll(cancelling);
            removeCow(cow);
        }
    }

    @Test
    @DisplayName("Acceleration without listeners still grows, preserving the old behavior")
    void testAccelerateWithoutListenersStillGrows() {
        Block b = setupAccelerator(30, 30);
        CowMock cow = spawnCow(30, 30, -10000);

        try {
            tick(b);

            Assertions.assertEquals(-8000, cow.getAge(), "The cow must have grown by 2000 ticks");
        } finally {
            removeCow(cow);
        }
    }

    @Test
    @DisplayName("A baby animal about to outgrow its childhood is clamped to adulthood")
    void testAccelerateClampsAgeToAdulthood() {
        Block b = setupAccelerator(35, 35);
        CowMock cow = spawnCow(35, 35, -1000);

        try {
            tick(b);

            Assertions.assertEquals(0, cow.getAge(), "The age must have been clamped to adulthood");
        } finally {
            removeCow(cow);
        }
    }

    @Test
    @DisplayName("An accelerator without food fires no event")
    void testNoFoodFiresNothing() {
        Block b = world.getBlockAt(40, 1, 40);
        b.setType(Material.DISPENSER);
        BlockStorage.addBlockInfo(b, "id", accelerator.getId(), true);
        BlockStorage.addBlockInfo(b.getLocation(), "energy-charge", "1024", false);
        BlockStorage.getInventory(b);
        CowMock cow = spawnCow(40, 40, -10000);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onAccelerate(AnimalAccelerateEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            tick(b);

            Assertions.assertFalse(seen[0], "No event must be fired without food");
            Assertions.assertEquals(-10000, cow.getAge(), "The cow must not have grown without food");
        } finally {
            HandlerList.unregisterAll(watcher);
            removeCow(cow);
        }
    }

    @Test
    @DisplayName("An accelerator without energy fires no event")
    void testNoEnergyFiresNothing() {
        Block b = setupAccelerator(50, 50);
        BlockStorage.addBlockInfo(b.getLocation(), "energy-charge", "0", true);
        CowMock cow = spawnCow(50, 50, -10000);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onAccelerate(AnimalAccelerateEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            tick(b);

            Assertions.assertFalse(seen[0], "No event must be fired without energy");
            Assertions.assertEquals(-10000, cow.getAge(), "The cow must not have grown without energy");
            Assertions.assertEquals(1, BlockStorage.getInventory(b).getItemInSlot(10).getAmount(), "The food must have been kept");
        } finally {
            HandlerList.unregisterAll(watcher);
            removeCow(cow);
        }
    }

    @Test
    @DisplayName("An adult animal fires no event")
    void testAdultAnimalFiresNothing() {
        Block b = setupAccelerator(60, 60);
        CowMock cow = spawnCow(60, 60, 0);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onAccelerate(AnimalAccelerateEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            tick(b);

            Assertions.assertFalse(seen[0], "No event must be fired for an adult animal");
            Assertions.assertEquals(1, BlockStorage.getInventory(b).getItemInSlot(10).getAmount(), "The food must have been kept");
        } finally {
            HandlerList.unregisterAll(watcher);
            removeCow(cow);
        }
    }
}
