package io.github.thebusybiscuit.slimefun4.implementation.items.magical;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;

import io.github.thebusybiscuit.slimefun4.api.events.MagicEyeOfEnderLaunchEvent;
import io.github.thebusybiscuit.slimefun4.api.events.PlayerRightClickEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Regression coverage for the magic eye of ender API expansion:
 * {@link MagicEyeOfEnderLaunchEvent}, exercised by driving the real {@link MagicEyeOfEnder}
 * {@link io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler} with a constructed
 * {@link PlayerRightClickEvent}.
 * <p>
 * MockBukkit's {@code launchProjectile} creates the projectile via {@code createEntity} without
 * registering it in the world, so the ender pearl is not observable through
 * {@code world.getEntities()}. The use sound ({@link Sound#ENTITY_ENDERMAN_TELEPORT}) plays right
 * after the launch, so hearing it proves the launch path completed, and not hearing it proves the
 * launch was skipped.
 *
 * @author Zurker
 */
class TestMagicEyeOfEnderLaunchEvent {

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static MagicEyeOfEnder magicEye;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "magic_eye_test");
        SlimefunItemStack stack = new SlimefunItemStack("_TEST_MAGIC_EYE", Material.ENDER_EYE, "&5Test Magic Eye of Ender");
        Slimefun.getItemCfg().setValue("_TEST_MAGIC_EYE.enabled", true);
        magicEye = new MagicEyeOfEnder(itemGroup, stack, RecipeType.NULL, new ItemStack[9]);
        magicEye.register(plugin);
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    @BeforeEach
    public void beforeEach() {
        server.getPluginManager().clearEvents();
    }

    private void equipEnderArmor(Player player) {
        player.getInventory().setHelmet(SlimefunItems.ENDER_HELMET.item());
        player.getInventory().setChestplate(SlimefunItems.ENDER_CHESTPLATE.item());
        player.getInventory().setLeggings(SlimefunItems.ENDER_LEGGINGS.item());
        player.getInventory().setBoots(SlimefunItems.ENDER_BOOTS.item());
    }

    private void use(Player player) {
        PlayerInteractEvent interactEvent = new PlayerInteractEvent(player, Action.RIGHT_CLICK_AIR, magicEye.getItem().clone(), null, null);
        PlayerRightClickEvent event = new PlayerRightClickEvent(interactEvent);
        magicEye.getItemHandler().onRightClick(event);
    }

    @Test
    @DisplayName("MagicEyeOfEnderLaunchEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        PlayerMock player = server.addPlayer();

        MagicEyeOfEnderLaunchEvent event = new MagicEyeOfEnderLaunchEvent(player, magicEye);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(magicEye, event.getMagicEye());
        Assertions.assertNull(event.getVelocity(), "The vanilla default launch must be represented by a null velocity");
        Assertions.assertFalse(event.isCancelled());

        // The launch velocity can be overridden and reset to the vanilla default
        Vector velocity = new Vector(0.5, 1.0, -0.5);
        event.setVelocity(velocity);
        Assertions.assertEquals(velocity, event.getVelocity());
        event.setVelocity(null);
        Assertions.assertNull(event.getVelocity(), "Setting the velocity back to null must restore the vanilla default");

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new MagicEyeOfEnderLaunchEvent(player, null));
    }

    @Test
    @DisplayName("Using the eye with ender armor fires the event and launches an ender pearl")
    void testUseFiresAndLaunches() {
        PlayerMock player = server.addPlayer();
        equipEnderArmor(player);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onLaunch(MagicEyeOfEnderLaunchEvent event) {
                seen[0] = true;
                Assertions.assertEquals(magicEye, event.getMagicEye());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            use(player);

            Assertions.assertTrue(seen[0], "MagicEyeOfEnderLaunchEvent was not fired");
            player.assertSoundHeard(Sound.ENTITY_ENDERMAN_TELEPORT);
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling MagicEyeOfEnderLaunchEvent skips the launch")
    void testEventCancellationSkipsLaunch() {
        PlayerMock player = server.addPlayer();
        equipEnderArmor(player);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onLaunch(MagicEyeOfEnderLaunchEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            use(player);

            Assertions.assertTrue(player.getHeardSounds().isEmpty(), "A cancelled launch must not reach the launch/sound tail");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Using the eye without listeners still launches, preserving the old behavior")
    void testUseWithoutListenersStillLaunches() {
        PlayerMock player = server.addPlayer();
        equipEnderArmor(player);

        use(player);

        player.assertSoundHeard(Sound.ENTITY_ENDERMAN_TELEPORT);
    }

    @Test
    @DisplayName("Overriding the velocity redirects the launch and keeps the launch tail")
    void testSetVelocityRedirectsLaunch() {
        PlayerMock player = server.addPlayer();
        equipEnderArmor(player);

        Vector custom = new Vector(0.5, 1.0, -0.5);

        // Registered first, so it runs before the observer within the same priority
        Listener redirecting = new Listener() {
            @EventHandler
            public void onLaunch(MagicEyeOfEnderLaunchEvent event) {
                event.setVelocity(custom);
            }
        };

        boolean[] seenRedirect = { false };
        Listener observer = new Listener() {
            @EventHandler
            public void onLaunch(MagicEyeOfEnderLaunchEvent event) {
                seenRedirect[0] = true;
                Assertions.assertEquals(custom, event.getVelocity(), "The overridden velocity must propagate through the dispatch");
            }
        };
        server.getPluginManager().registerEvents(redirecting, plugin);
        server.getPluginManager().registerEvents(observer, plugin);

        try {
            use(player);

            Assertions.assertTrue(seenRedirect[0], "MagicEyeOfEnderLaunchEvent was not fired");
            // The projectile itself is not registered in the world (see class javadoc), so the
            // use sound proves the velocity-overloaded launch path completed.
            player.assertSoundHeard(Sound.ENTITY_ENDERMAN_TELEPORT);
        } finally {
            HandlerList.unregisterAll(redirecting);
            HandlerList.unregisterAll(observer);
        }
    }

    @Test
    @DisplayName("Using the eye without ender armor neither fires the event nor launches")
    void testUseWithoutArmorDoesNothing() {
        PlayerMock player = server.addPlayer();

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onLaunch(MagicEyeOfEnderLaunchEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            use(player);

            Assertions.assertFalse(seen[0], "The event must not fire without the ender armor set");
            Assertions.assertTrue(player.getHeardSounds().isEmpty(), "No launch must happen without the armor");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }
}
