package io.github.thebusybiscuit.slimefun4.implementation.listeners;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opentest4j.TestAbortedException;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;

import io.github.bakedlibs.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun4.api.events.AncientAltarRitualAbortEvent;
import io.github.thebusybiscuit.slimefun4.api.events.AncientAltarRitualStartEvent;
import io.github.thebusybiscuit.slimefun4.api.events.PlayerRightClickEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun4.implementation.items.altar.AltarRecipe;
import io.github.thebusybiscuit.slimefun4.implementation.items.altar.AncientAltar;
import io.github.thebusybiscuit.slimefun4.implementation.items.altar.AncientPedestal;
import io.github.thebusybiscuit.slimefun4.implementation.tasks.AncientAltarTask;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Regression coverage for the ancient altar API expansion: {@link AncientAltarRitualStartEvent}
 * and {@link AncientAltarRitualAbortEvent}, exercised through the real {@link AncientAltarListener}
 * activation path and the real {@link AncientAltarTask} abort path.
 *
 * @author Zurker
 */
class TestAncientAltarRitualEvents {

    private static final int[][] PEDESTAL_OFFSETS = { { 2, -2 }, { 3, 0 }, { 2, 2 }, { 0, 3 }, { -2, 2 }, { -3, 0 }, { -2, -2 }, { 0, -3 } };

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static AncientAltar altarItem;
    private static AncientPedestal pedestalItem;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        // Unit test startups do not register the listeners, register the place listener manually
        new BlockListener(plugin);

        // onUnitTestStart() never starts the integrations, so start them manually and
        // run the scheduled onServerStart task to create the ProtectionManager
        Slimefun.getIntegrations().start();
        server.getScheduler().performOneTick();

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "altar_ritual_test");

        altarItem = new AncientAltar(itemGroup, SlimefunItems.ANCIENT_ALTAR, RecipeType.NULL, new ItemStack[9]);
        altarItem.register(plugin);

        pedestalItem = new AncientPedestal(itemGroup, SlimefunItems.ANCIENT_PEDESTAL, RecipeType.NULL, new ItemStack[9], null);
        pedestalItem.register(plugin);

        // Catalyst sits at index 4 of a crafting-grid style recipe, every pedestal input is a diamond
        List<ItemStack> recipeInput = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            recipeInput.add(new ItemStack(i == 4 ? Material.BLAZE_ROD : Material.DIAMOND));
        }

        altarItem.getRecipes().add(new AltarRecipe(recipeInput, new ItemStack(Material.NETHER_STAR)));
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    @BeforeEach
    public void beforeEach() {
        server.getPluginManager().clearEvents();
    }

    private Block placeSlimefunBlockAt(io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem item, Location loc, Player player) {
        Block block = world.getBlockAt(loc);
        block.setType(item.getItem().getType());
        Block blockAgainst = world.getBlockAt(loc.getBlockX(), loc.getBlockY() + 1, loc.getBlockZ());
        blockAgainst.setType(Material.GRASS_BLOCK);

        BlockPlaceEvent placeEvent = new BlockPlaceEvent(block, block.getState(), blockAgainst, item.getItem(), player, true, EquipmentSlot.HAND);
        server.getPluginManager().callEvent(placeEvent);
        return block;
    }

    /**
     * Builds a complete altar multiblock: the altar itself, eight pedestals around it
     * and a diamond resting on every pedestal.
     * <p>
     * Note that MockBukkit's BlockMock shares its internal {@link Location} instance,
     * so the drop location must be cloned before being shifted - otherwise the pedestal
     * block itself "moves" and the ritual's entity scan misses the placed item.
     */
    private Block buildAltar(Player player, Location altarLoc) {
        Block altar = placeSlimefunBlockAt(altarItem, altarLoc, player);

        for (int i = 0; i < PEDESTAL_OFFSETS.length; i++) {
            Location pedestalLoc = altarLoc.clone().add(PEDESTAL_OFFSETS[i][0], 0, PEDESTAL_OFFSETS[i][1]);
            Block pedestal = placeSlimefunBlockAt(pedestalItem, pedestalLoc, player);

            ItemStack display = CustomItemStack.create(new ItemStack(Material.DIAMOND), AncientPedestal.ITEM_PREFIX + i);
            world.dropItem(pedestal.getLocation().clone().add(0.5, 1.2, 0.5), display);
        }

        return altar;
    }

    private PlayerRightClickEvent newAltarClick(PlayerMock player, Block altar) {
        PlayerInteractEvent interactEvent = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, player.getInventory().getItemInMainHand(), altar, org.bukkit.block.BlockFace.UP, EquipmentSlot.HAND);
        return new PlayerRightClickEvent(interactEvent);
    }

    // ---------- AncientAltarRitualStartEvent ----------

    @Test
    @DisplayName("AncientAltarRitualStartEvent exposes its fields and validates constructor arguments")
    void testStartEventFieldsAndValidation() {
        PlayerMock player = server.addPlayer();
        Block altar = Mockito.mock(Block.class);
        List<Block> pedestals = List.of(Mockito.mock(Block.class));
        ItemStack catalyst = new ItemStack(Material.BLAZE_ROD);
        List<ItemStack> input = List.of(new ItemStack(Material.DIAMOND));
        ItemStack output = new ItemStack(Material.NETHER_STAR);

        AncientAltarRitualStartEvent event = new AncientAltarRitualStartEvent(player, altar, pedestals, catalyst, input, output);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(altar, event.getAltar());
        Assertions.assertEquals(pedestals, event.getPedestals());
        Assertions.assertEquals(catalyst, event.getCatalyst());
        Assertions.assertEquals(input, event.getInput());
        Assertions.assertEquals(output, event.getOutput());
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(UnsupportedOperationException.class, () -> event.getPedestals().clear());
        Assertions.assertThrows(UnsupportedOperationException.class, () -> event.getInput().clear());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new AncientAltarRitualStartEvent(player, null, pedestals, catalyst, input, output));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new AncientAltarRitualStartEvent(player, altar, null, catalyst, input, output));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new AncientAltarRitualStartEvent(player, altar, pedestals, null, input, output));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new AncientAltarRitualStartEvent(player, altar, pedestals, catalyst, null, output));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new AncientAltarRitualStartEvent(player, altar, pedestals, catalyst, input, null));
    }

    @Test
    @DisplayName("Activating an altar with a valid recipe fires AncientAltarRitualStartEvent")
    void testRitualStartFires() {
        PlayerMock player = server.addPlayer();
        player.setGameMode(GameMode.SURVIVAL);
        Block altar = buildAltar(player, new Location(world, 0, 64, 0));
        AncientAltarListener listener = new AncientAltarListener(plugin, altarItem, pedestalItem);
        player.getInventory().setItemInMainHand(new ItemStack(Material.BLAZE_ROD));

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onRitualStart(AncientAltarRitualStartEvent event) {
                seen[0] = true;
                Assertions.assertEquals(player, event.getPlayer());
                Assertions.assertEquals(altar, event.getAltar());
                Assertions.assertEquals(8, event.getPedestals().size());
                Assertions.assertEquals(Material.BLAZE_ROD, event.getCatalyst().getType());
                Assertions.assertEquals(8, event.getInput().size());
                Assertions.assertTrue(event.getInput().stream().allMatch(item -> item.getType() == Material.DIAMOND));
                Assertions.assertEquals(Material.NETHER_STAR, event.getOutput().getType());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            try {
                listener.onInteract(newAltarClick(player, altar));
            } catch (TestAbortedException x) {
                // WorldMock does not implement every spawnParticle overload used by the ritual animation
            }

            Assertions.assertTrue(seen[0], "AncientAltarRitualStartEvent was not fired");
            Assertions.assertEquals(0, player.getInventory().getItemInMainHand().getAmount(), "The catalyst must have been consumed");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling AncientAltarRitualStartEvent keeps the catalyst and releases the altar")
    void testRitualStartCancellation() {
        PlayerMock player = server.addPlayer();
        player.setGameMode(GameMode.SURVIVAL);
        Block altar = buildAltar(player, new Location(world, 100, 64, 100));
        AncientAltarListener listener = new AncientAltarListener(plugin, altarItem, pedestalItem);
        player.getInventory().setItemInMainHand(new ItemStack(Material.BLAZE_ROD));

        Listener cancelling = new Listener() {
            @EventHandler
            public void onRitualStart(AncientAltarRitualStartEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            listener.onInteract(newAltarClick(player, altar));

            Assertions.assertEquals(1, player.getInventory().getItemInMainHand().getAmount(), "A cancelled ritual must not consume the catalyst");

            /*
             * The ritual's pedestal scan shifts the shared BlockMock locations (a MockBukkit
             * artifact - on a real server Block#getLocation() returns a fresh copy), which
             * changes their hash codes and makes the lock removal miss. Only the altar lock,
             * whose location is never shifted, can be asserted to be released here.
             */
            Assertions.assertFalse(listener.getAltarsInUse().contains(altar.getLocation()), "A cancelled ritual must release the altar lock");
            Assertions.assertTrue(listener.getAltars().isEmpty(), "A cancelled ritual must not keep the altar registered");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    // ---------- AncientAltarRitualAbortEvent ----------

    @Test
    @DisplayName("AncientAltarRitualAbortEvent exposes its fields and validates constructor arguments")
    void testAbortEventFieldsAndValidation() {
        PlayerMock player = server.addPlayer();
        Block altar = Mockito.mock(Block.class);
        List<Block> pedestals = List.of(Mockito.mock(Block.class));
        List<ItemStack> returned = List.of(new ItemStack(Material.BLAZE_ROD));

        AncientAltarRitualAbortEvent event = new AncientAltarRitualAbortEvent(player, altar, pedestals, returned);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(altar, event.getAltar());
        Assertions.assertEquals(pedestals, event.getPedestals());
        Assertions.assertEquals(returned, event.getReturnedItems());

        Assertions.assertThrows(UnsupportedOperationException.class, () -> event.getReturnedItems().clear());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new AncientAltarRitualAbortEvent(player, null, pedestals, returned));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new AncientAltarRitualAbortEvent(player, altar, null, returned));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new AncientAltarRitualAbortEvent(player, altar, pedestals, null));
    }

    @Test
    @DisplayName("Moving a locked pedestal item aborts the ritual and fires AncientAltarRitualAbortEvent")
    void testRitualAbortFires() {
        PlayerMock player = server.addPlayer();
        World mockWorld = Mockito.mock(World.class);
        Location altarLoc = new Location(mockWorld, 0, 64, 0);
        Location pedestalLoc = new Location(mockWorld, 2, 64, -2);
        Location itemLoc = pedestalLoc.clone().add(0.5, 1.2, 0.5);

        Item itemEntity = Mockito.mock(Item.class);
        Mockito.when(itemEntity.getItemStack()).thenReturn(CustomItemStack.create(new ItemStack(Material.DIAMOND), AncientPedestal.ITEM_PREFIX + 0));
        Mockito.when(itemEntity.isValid()).thenReturn(true);
        Mockito.when(itemEntity.getLocation()).thenReturn(itemLoc);

        Mockito.doAnswer(invocation -> List.of(itemEntity)).when(mockWorld).getNearbyEntities(Mockito.any(Location.class), Mockito.anyDouble(), Mockito.anyDouble(), Mockito.anyDouble(), Mockito.<Predicate<Entity>>any());

        Block altar = Mockito.mock(Block.class);
        Mockito.when(altar.getLocation()).thenReturn(altarLoc);
        Block pedestal = Mockito.mock(Block.class);
        Mockito.when(pedestal.getLocation()).thenReturn(pedestalLoc);

        AncientAltarListener listener = new AncientAltarListener(plugin, altarItem, pedestalItem);
        List<ItemStack> consumed = new ArrayList<>();
        consumed.add(new ItemStack(Material.BLAZE_ROD));

        AncientAltarTask task = new AncientAltarTask(listener, altar, 0, new ItemStack(Material.NETHER_STAR), List.of(pedestal), consumed, player);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onRitualAbort(AncientAltarRitualAbortEvent event) {
                seen[0] = true;
                Assertions.assertEquals(player, event.getPlayer());
                Assertions.assertEquals(altar, event.getAltar());
                Assertions.assertEquals(1, event.getPedestals().size());
                Assertions.assertEquals(1, event.getReturnedItems().size());
                Assertions.assertEquals(Material.BLAZE_ROD, event.getReturnedItems().get(0).getType());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            // The locked item moved away from its pedestal, the next task run must abort
            Mockito.when(itemEntity.getLocation()).thenReturn(new Location(mockWorld, 50, 64, 50));
            task.run();

            Assertions.assertTrue(seen[0], "AncientAltarRitualAbortEvent was not fired");
            Assertions.assertTrue(consumed.isEmpty(), "The consumed items must have been returned");
            Assertions.assertTrue(listener.getAltarsInUse().isEmpty(), "An aborted ritual must release all altar locks");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }
}
