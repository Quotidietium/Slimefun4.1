package io.github.thebusybiscuit.slimefun4.implementation.listeners;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import io.github.bakedlibs.dough.protection.Interaction;
import io.github.bakedlibs.dough.protection.ProtectionModule;
import io.github.thebusybiscuit.slimefun4.api.events.PlayerRightClickEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.altar.AncientAltar;
import io.github.thebusybiscuit.slimefun4.implementation.items.altar.AncientPedestal;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;
import me.mrCookieSlime.Slimefun.api.BlockStorage;

/**
 * Regression coverage for the permission-denied paths of {@link AncientAltarListener}:
 * {@code onInteract} marks the clicked altar as "in use" before {@code useAltar} performs
 * its permission checks. Both denial paths (the altar itself, or one of the 8 pedestals)
 * must remove that marker again - otherwise a single denied click used to brick the altar
 * until the server restarted.
 *
 * <p>
 * The denial is proven via the {@link io.github.bakedlibs.dough.protection.ProtectionManager}
 * itself rather than chat output (the unit-test localization renders every message as
 * "Error: No language present"): since the permission check is the very first statement of
 * {@code useAltar}, a denied permission makes the denial branch the only reachable code.
 *
 * <p>
 * The pedestal stays unregistered (consistent with {@code TestPedestalItemPlaceEvent}:
 * its BlockDispenseHandler fails framework validation for a test item). Only its id string
 * is compared, which does not require registry presence.
 *
 * @author Zurker
 */
class TestAncientAltarPermissionCleanup {

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static AncientAltarListener listener;

    /** Locations that the registered protection module denies INTERACT_BLOCK for. */
    private static final Set<Location> deniedLocations = new HashSet<>();

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        // The ProtectionManager is created when the integrations start
        Slimefun.getIntegrations().start();
        server.getScheduler().performOneTick();

        // Register a controllable protection module that denies specific locations
        MockBukkit.createMockPlugin("AltarDenyGuard");
        Slimefun.getProtectionManager().registerModule(server.getPluginManager(), "AltarDenyGuard", new Function<org.bukkit.plugin.Plugin, ProtectionModule>() {

            @Override
            public ProtectionModule apply(org.bukkit.plugin.Plugin modulePlugin) {
                return new ProtectionModule() {

                    @Override
                    public void load() {}

                    @Override
                    public org.bukkit.plugin.Plugin getPlugin() {
                        return modulePlugin;
                    }

                    @Override
                    public boolean hasPermission(org.bukkit.OfflinePlayer player, Location loc, Interaction action) {
                        return action != Interaction.INTERACT_BLOCK || !deniedLocations.contains(loc);
                    }
                };
            }
        });

        // The plugin boot under MockBukkit registers no items - provide our own altar
        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "altar_perm_test");
        SlimefunItemStack altarStack = new SlimefunItemStack("_TEST_ALTAR_PERM_ALTAR", Material.ENCHANTING_TABLE, "&dTest Altar");
        Slimefun.getItemCfg().setValue("_TEST_ALTAR_PERM_ALTAR.enabled", true);
        AncientAltar altar = new AncientAltar(itemGroup, altarStack, RecipeType.NULL, new ItemStack[9]);
        altar.register(plugin);

        SlimefunItemStack pedestalStack = new SlimefunItemStack("_TEST_ALTAR_PERM_PEDESTAL", Material.DISPENSER, "&dTest Pedestal");
        AncientPedestal pedestal = new AncientPedestal(itemGroup, pedestalStack, RecipeType.NULL, new ItemStack[9], new ItemStack(Material.AIR));

        listener = new AncientAltarListener(Slimefun.instance(), altar, pedestal);
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    @BeforeEach
    public void beforeEach() {
        deniedLocations.clear();
        listener.getAltarsInUse().clear();
        listener.getAltars().clear();
    }

    private void clickAltar(PlayerMock player, Block altar) {
        PlayerInteractEvent interactEvent = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, new ItemStack(Material.AIR), altar, BlockFace.UP);
        listener.onInteract(new PlayerRightClickEvent(interactEvent));
    }

    /**
     * Proves the interaction necessarily reached the permission-denied branch: the permission
     * check is the first statement of {@code useAltar}, so with this asserted false, the denial
     * branch is the only code that can run (the follow-up message is also expected to exist).
     */
    private void assertDenialEngaged(PlayerMock player, Block altar) {
        assertFalse(Slimefun.getProtectionManager().hasPermission(player, altar, Interaction.INTERACT_BLOCK), "The deny module did not engage for the altar location");
        assertNotNull(player.nextMessage(), "The denial branch sent no message at all");
    }

    @Test
    @DisplayName("A denied click on the altar itself must not leave it marked as in-use")
    void testDeniedAltarClickDoesNotBrickAltar() {
        PlayerMock player = server.addPlayer();
        Block altar = world.getBlockAt(0, 64, 0);
        altar.setType(Material.ENCHANTING_TABLE);
        BlockStorage.addBlockInfo(altar.getLocation(), "id", "_TEST_ALTAR_PERM_ALTAR", false);

        deniedLocations.add(altar.getLocation());
        clickAltar(player, altar);
        assertDenialEngaged(player, altar);

        assertTrue(listener.getAltarsInUse().isEmpty(), "The altar is still marked as in-use after a denied click");
        assertTrue(listener.getAltars().isEmpty(), "The altar is still registered as active after a denied click");
    }

    @Test
    @DisplayName("A denied pedestal permission must not leave the altar marked as in-use")
    void testDeniedPedestalPermissionDoesNotBrickAltar() {
        PlayerMock player = server.addPlayer();
        Block altar = world.getBlockAt(0, 64, 0);
        altar.setType(Material.ENCHANTING_TABLE);
        BlockStorage.addBlockInfo(altar.getLocation(), "id", "_TEST_ALTAR_PERM_ALTAR", false);

        // Build the full 8-pedestal ring exactly like AncientAltarListener#getPedestals expects
        int[][] offsets = { { 2, -2 }, { 3, 0 }, { 2, 2 }, { 0, 3 }, { -2, 2 }, { -3, 0 }, { -2, -2 }, { 0, -3 } };

        for (int[] offset : offsets) {
            Block pedestal = altar.getRelative(offset[0], 0, offset[1]);
            pedestal.setType(Material.DISPENSER);
            BlockStorage.addBlockInfo(pedestal.getLocation(), "id", "_TEST_ALTAR_PERM_PEDESTAL", false);
        }

        // Deny only one pedestal of the ring; the altar itself stays permitted
        Block deniedPedestal = altar.getRelative(2, 0, -2);
        deniedLocations.add(deniedPedestal.getLocation());
        assertFalse(deniedLocations.contains(altar.getLocation()), "Sanity: the altar location itself is not denied");

        clickAltar(player, altar);

        // The denial happened on the pedestal check inside useAltar (the altar passed its own check)
        assertFalse(Slimefun.getProtectionManager().hasPermission(player, deniedPedestal, Interaction.INTERACT_BLOCK), "The deny module did not engage for the pedestal location");
        assertNotNull(player.nextMessage(), "The denial branch sent no message at all");

        assertTrue(listener.getAltarsInUse().isEmpty(), "The altar is still marked as in-use after a denied pedestal permission");
        assertTrue(listener.getAltars().isEmpty(), "The altar is still registered as active after a denied pedestal permission");
    }
}
