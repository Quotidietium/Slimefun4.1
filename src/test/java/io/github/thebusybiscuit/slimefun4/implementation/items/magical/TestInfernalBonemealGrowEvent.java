package io.github.thebusybiscuit.slimefun4.implementation.items.magical;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
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

import io.github.thebusybiscuit.slimefun4.api.events.InfernalBonemealGrowEvent;
import io.github.thebusybiscuit.slimefun4.api.events.PlayerRightClickEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Regression coverage for the infernal bonemeal API expansion: {@link InfernalBonemealGrowEvent},
 * exercised by driving the real {@link InfernalBonemeal}
 * {@link io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler} with a constructed
 * {@link PlayerRightClickEvent}.
 * <p>
 * MockBukkit's block data registry has no {@link Ageable} for nether wart, so the wart is a
 * Mockito hybrid block with a mocked {@link Ageable} block data. The growth tail plays its
 * effect with {@code BlockData} (the primary data type of {@code STEP_SOUND}), which
 * MockBukkit accepts, so the whole tail - growth, effect and bonemeal consumption - is
 * observable end to end.
 *
 * @author Zurker
 */
class TestInfernalBonemealGrowEvent {

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static InfernalBonemeal bonemeal;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "infernal_bonemeal_test");
        SlimefunItemStack stack = new SlimefunItemStack("_TEST_INFERNAL_BONEMEAL", Material.BONE_MEAL, "&4Test Infernal Bonemeal");
        Slimefun.getItemCfg().setValue("_TEST_INFERNAL_BONEMEAL.enabled", true);
        bonemeal = new InfernalBonemeal(itemGroup, stack, RecipeType.NULL, new ItemStack[9], new ItemStack(Material.BONE_MEAL));
        bonemeal.register(plugin);
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
     * Right-clicks a Mockito nether wart with three bonemeal via the real handler and
     * returns the used {@link ItemStack} for consumption assertions.
     */
    private ItemStack grow(Player player, Block wart) {
        ItemStack item = bonemeal.getItem().clone();
        item.setAmount(3);

        PlayerInteractEvent interactEvent = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, item, wart, BlockFace.UP);
        bonemeal.getItemHandler().onRightClick(new PlayerRightClickEvent(interactEvent));
        return item;
    }

    private Block mockNetherWart(int x, int z, Ageable ageable) {
        Block wart = Mockito.mock(Block.class);
        Mockito.when(wart.getType()).thenReturn(Material.NETHER_WART);
        Mockito.when(wart.getBlockData()).thenReturn(ageable);
        Mockito.when(wart.getLocation()).thenReturn(new Location(world, x, 4, z));
        Mockito.when(wart.getWorld()).thenReturn(world);
        return wart;
    }

    private Ageable mockAgeable(int age, int maxAge) {
        Ageable ageable = Mockito.mock(Ageable.class);
        Mockito.when(ageable.getAge()).thenReturn(age);
        Mockito.when(ageable.getMaximumAge()).thenReturn(maxAge);
        return ageable;
    }

    @Test
    @DisplayName("InfernalBonemealGrowEvent exposes its fields and validates constructor and setter arguments")
    void testEventFieldsAndValidation() {
        Player player = server.addPlayer();
        Block wart = mockNetherWart(0, 0, mockAgeable(1, 3));

        InfernalBonemealGrowEvent event = new InfernalBonemealGrowEvent(player, bonemeal, wart);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(bonemeal, event.getBonemeal());
        Assertions.assertEquals(wart, event.getBlock());
        Assertions.assertEquals(3, event.getMaximumAge(), "The maximum age must come from the block data");
        Assertions.assertEquals(3, event.getTargetAge(), "The growth must default to full maturity");
        Assertions.assertFalse(event.isCancelled());

        // The growth can be throttled to any age within the maximum
        event.setTargetAge(1);
        Assertions.assertEquals(1, event.getTargetAge());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setTargetAge(-1));
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setTargetAge(4), "The target age must not exceed the maximum");
        Assertions.assertThrows(IllegalArgumentException.class, () -> new InfernalBonemealGrowEvent(player, null, wart));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new InfernalBonemealGrowEvent(player, bonemeal, null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new InfernalBonemealGrowEvent(player, bonemeal, wart, -1));
    }

    @Test
    @DisplayName("Growing a nether wart fires the event and sets the wart to full age")
    void testGrowFiresAndGrows() {
        Player player = server.addPlayer();
        Ageable ageable = mockAgeable(1, 3);
        Block wart = mockNetherWart(10, 10, ageable);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onGrow(InfernalBonemealGrowEvent event) {
                seen[0] = true;
                Assertions.assertEquals(bonemeal, event.getBonemeal());
                Assertions.assertEquals(wart, event.getBlock());
                Assertions.assertEquals(3, event.getTargetAge(), "The growth must default to full maturity");
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            ItemStack item = grow(player, wart);

            Assertions.assertTrue(seen[0], "InfernalBonemealGrowEvent was not fired");
            Mockito.verify(ageable).setAge(3);
            Mockito.verify(wart).setBlockData(ageable);
            Assertions.assertEquals(2, item.getAmount(), "One bonemeal must have been consumed");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("setTargetAge throttles the growth to the given age")
    void testSetTargetAgeThrottlesGrowth() {
        Player player = server.addPlayer();
        Ageable ageable = mockAgeable(1, 3);
        Block wart = mockNetherWart(20, 20, ageable);

        Listener throttling = new Listener() {
            @EventHandler
            public void onGrow(InfernalBonemealGrowEvent event) {
                event.setTargetAge(2);
            }
        };
        server.getPluginManager().registerEvents(throttling, plugin);

        try {
            ItemStack item = grow(player, wart);

            Mockito.verify(ageable).setAge(2);
            Mockito.verify(ageable, Mockito.never()).setAge(3);
            Assertions.assertEquals(2, item.getAmount(), "One bonemeal must have been consumed");
        } finally {
            HandlerList.unregisterAll(throttling);
        }
    }

    @Test
    @DisplayName("Cancelling InfernalBonemealGrowEvent keeps the wart age and the bonemeal untouched")
    void testEventCancellationSkipsGrowth() {
        Player player = server.addPlayer();
        Ageable ageable = mockAgeable(1, 3);
        Block wart = mockNetherWart(30, 30, ageable);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onGrow(InfernalBonemealGrowEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            ItemStack item = grow(player, wart);

            Mockito.verify(ageable, Mockito.never()).setAge(Mockito.anyInt());
            Mockito.verify(wart, Mockito.never()).setBlockData(Mockito.any());
            Assertions.assertEquals(3, item.getAmount(), "A cancelled growth must keep the bonemeal");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("A fully grown nether wart neither fires the event nor grows")
    void testFullyGrownWartDoesNothing() {
        Player player = server.addPlayer();
        Ageable ageable = mockAgeable(3, 3);
        Block wart = mockNetherWart(40, 40, ageable);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onGrow(InfernalBonemealGrowEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            ItemStack item = grow(player, wart);

            Assertions.assertFalse(seen[0], "The event must not fire for a fully grown wart");
            Mockito.verify(ageable, Mockito.never()).setAge(Mockito.anyInt());
            Assertions.assertEquals(3, item.getAmount(), "Nothing must have been consumed");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Growing without listeners still grows, preserving the old behavior")
    void testGrowWithoutListenersStillGrows() {
        Player player = server.addPlayer();
        Ageable ageable = mockAgeable(1, 3);
        Block wart = mockNetherWart(50, 50, ageable);

        ItemStack item = grow(player, wart);

        Mockito.verify(ageable).setAge(3);
        Mockito.verify(wart).setBlockData(ageable);
        Assertions.assertEquals(2, item.getAmount(), "One bonemeal must have been consumed");
    }
}
