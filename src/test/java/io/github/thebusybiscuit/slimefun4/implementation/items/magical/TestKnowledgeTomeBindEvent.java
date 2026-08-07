package io.github.thebusybiscuit.slimefun4.implementation.items.magical;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

import io.github.thebusybiscuit.slimefun4.api.events.KnowledgeTomeBindEvent;
import io.github.thebusybiscuit.slimefun4.api.events.PlayerRightClickEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Regression coverage for the knowledge tome API expansion: {@link KnowledgeTomeBindEvent},
 * exercised by driving the real {@link KnowledgeTome}
 * {@link io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler} with a constructed
 * {@link PlayerRightClickEvent}.
 * <p>
 * The binding is a synchronous lore rewrite on the held item, so tests assert it end-to-end:
 * a fired bind writes the player uuid into the lore, a cancelled bind leaves the tome unbound.
 *
 * @author Zurker
 */
class TestKnowledgeTomeBindEvent {

    private static ServerMock server;
    private static Slimefun plugin;
    private static World world;

    private static KnowledgeTome tome;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        world = TestUtilities.createWorld(server);

        // ProtectionManager is created when the integrations start
        Slimefun.getIntegrations().start();
        server.getScheduler().performOneTick();

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "knowledge_tome_bind_test");
        SlimefunItemStack stack = new SlimefunItemStack("_TEST_BINDABLE_TOME", Material.BOOK, "&5Test Bindable Tome", "&7Owner: ", "");
        Slimefun.getItemCfg().setValue("_TEST_BINDABLE_TOME.enabled", true);
        tome = new KnowledgeTome(itemGroup, stack, RecipeType.NULL, new ItemStack[9]);
        tome.register(plugin);
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
     * Right-clicks with the given tome via the real item use handler.
     */
    private void use(Player player, ItemStack tomeItem) {
        Block b = world.getBlockAt(0, 1, 0);
        b.setType(Material.STONE);

        PlayerInteractEvent interactEvent = new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, tomeItem, b, BlockFace.UP);
        tome.getItemHandler().onRightClick(new PlayerRightClickEvent(interactEvent));
    }

    @Test
    @DisplayName("KnowledgeTomeBindEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Player player = server.addPlayer();
        ItemStack tomeItem = tome.getItem().clone();

        KnowledgeTomeBindEvent event = new KnowledgeTomeBindEvent(player, tome, tomeItem);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(tome, event.getTome());
        Assertions.assertEquals(tomeItem, event.getTomeItem());
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new KnowledgeTomeBindEvent(player, null, tomeItem));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new KnowledgeTomeBindEvent(player, tome, null));
    }

    @Test
    @DisplayName("Using an unbound tome fires the event and binds the tome")
    void testBindFiresEventAndBinds() {
        Player player = server.addPlayer();
        ItemStack unbound = tome.getItem().clone();

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onBind(KnowledgeTomeBindEvent event) {
                seen[0] = true;
                Assertions.assertEquals(player, event.getPlayer());
                Assertions.assertEquals(tome, event.getTome());
                Assertions.assertEquals(unbound, event.getTomeItem());
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            use(player, unbound);

            Assertions.assertTrue(seen[0], "KnowledgeTomeBindEvent was not fired");
            List<String> lore = unbound.getItemMeta().getLore();
            Assertions.assertEquals(player.getUniqueId().toString(), ChatColor.stripColor(lore.get(1)), "The player uuid must have been written into the lore");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Cancelling KnowledgeTomeBindEvent leaves the tome unbound")
    void testCancelKeepsTomeUnbound() {
        Player player = server.addPlayer();
        ItemStack unbound = tome.getItem().clone();

        Listener cancelling = new Listener() {
            @EventHandler
            public void onBind(KnowledgeTomeBindEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            use(player, unbound);

            List<String> lore = unbound.getItemMeta().getLore();
            Assertions.assertEquals("", lore.get(1), "A cancelled bind must leave the owner line empty");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Binding without listeners still binds, preserving the old behavior")
    void testBindWithoutListenersStillBinds() {
        Player player = server.addPlayer();
        ItemStack unbound = tome.getItem().clone();

        use(player, unbound);

        List<String> lore = unbound.getItemMeta().getLore();
        Assertions.assertEquals(player.getUniqueId().toString(), ChatColor.stripColor(lore.get(1)), "The player uuid must have been written into the lore");
    }

    @Test
    @DisplayName("Using a bound tome fires no bind event")
    void testBoundTomeFiresNoBindEvent() {
        Player owner = server.addPlayer();
        Player receiver = server.addPlayer();
        ItemStack bound = tome.getItem().clone();
        ItemMeta meta = bound.getItemMeta();
        List<String> lore = meta.getLore();
        lore.set(1, ChatColor.BLACK + owner.getUniqueId().toString());
        meta.setLore(lore);
        bound.setItemMeta(meta);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onBind(KnowledgeTomeBindEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            use(receiver, bound);

            Assertions.assertFalse(seen[0], "Sharing a bound tome must not fire the bind event");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("A tome with stripped lore fires no event")
    void testStrippedLoreFiresNothing() {
        Player player = server.addPlayer();
        ItemStack stripped = tome.getItem().clone();
        ItemMeta meta = stripped.getItemMeta();
        meta.setLore(null);
        stripped.setItemMeta(meta);

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onBind(KnowledgeTomeBindEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            use(player, stripped);

            Assertions.assertFalse(seen[0], "No event must be fired for a tome without lore");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }
}
