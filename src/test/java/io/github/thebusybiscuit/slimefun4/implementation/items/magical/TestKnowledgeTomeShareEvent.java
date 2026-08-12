package io.github.thebusybiscuit.slimefun4.implementation.items.magical;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
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

import io.github.thebusybiscuit.slimefun4.api.events.KnowledgeTomeShareEvent;
import io.github.thebusybiscuit.slimefun4.api.events.PlayerRightClickEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.api.researches.Research;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

/**
 * Regression coverage for the knowledge tome API expansion: {@link KnowledgeTomeShareEvent},
 * exercised by driving the real {@link KnowledgeTome}
 * {@link io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler} with a constructed
 * {@link PlayerRightClickEvent}.
 * <p>
 * The research copy itself is handed to the asynchronous profile chain, so the synchronous
 * observable is the tome consumption: a fired share consumes the tome, a cancelled share keeps
 * it. The binding branch (unbound tome) is covered as a negative path that must not fire.
 *
 * @author Zurker
 */
class TestKnowledgeTomeShareEvent {

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

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "knowledge_tome_test");
        SlimefunItemStack stack = new SlimefunItemStack("_TEST_KNOWLEDGE_TOME", Material.BOOK, "&5Test Knowledge Tome", "&7Owner: ", "");
        Slimefun.getItemCfg().setValue("_TEST_KNOWLEDGE_TOME.enabled", true);
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
     * Creates a tome bound to the given owner (uuid written into the second lore line).
     */
    private ItemStack boundTome(UUID owner) {
        ItemStack bound = tome.getItem().clone();
        ItemMeta meta = bound.getItemMeta();
        List<String> lore = meta.getLore();
        lore.set(1, ChatColor.BLACK + owner.toString());
        meta.setLore(lore);
        bound.setItemMeta(meta);
        return bound;
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
    @DisplayName("KnowledgeTomeShareEvent exposes its fields and validates constructor arguments")
    void testEventFieldsAndValidation() {
        Player player = server.addPlayer();
        ItemStack tomeItem = tome.getItem().clone();
        UUID owner = UUID.randomUUID();

        KnowledgeTomeShareEvent event = new KnowledgeTomeShareEvent(player, tome, tomeItem, owner);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(tome, event.getTome());
        Assertions.assertEquals(tomeItem, event.getTomeItem());
        Assertions.assertEquals(owner, event.getOwner());
        Assertions.assertFalse(event.isCancelled());

        UUID redirect = UUID.randomUUID();
        event.setOwner(redirect);
        Assertions.assertEquals(redirect, event.getOwner());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new KnowledgeTomeShareEvent(player, null, tomeItem, owner));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new KnowledgeTomeShareEvent(player, tome, null, owner));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new KnowledgeTomeShareEvent(player, tome, tomeItem, null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setOwner(null));
    }

    @Test
    @DisplayName("Using a bound tome fires the event with the bound owner")
    void testShareFiresEvent() {
        Player owner = server.addPlayer();
        Player receiver = server.addPlayer();
        ItemStack bound = boundTome(owner.getUniqueId());

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onShare(KnowledgeTomeShareEvent event) {
                seen[0] = true;
                Assertions.assertEquals(receiver, event.getPlayer());
                Assertions.assertEquals(tome, event.getTome());
                Assertions.assertEquals(bound, event.getTomeItem());
                Assertions.assertEquals(owner.getUniqueId(), event.getOwner(), "The bound owner must be exposed");
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            use(receiver, bound);

            Assertions.assertTrue(seen[0], "KnowledgeTomeShareEvent was not fired");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    /**
     * Loads (or returns the cached) {@link PlayerProfile} without asserting on the cache
     * state - the tome's share chain may already have requested the receiver's profile.
     */
    private PlayerProfile profileOf(OfflinePlayer player) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<PlayerProfile> ref = new AtomicReference<>();
        PlayerProfile.get(player, profile -> {
            ref.set(profile);
            latch.countDown();
        });
        Assertions.assertTrue(latch.await(5, TimeUnit.SECONDS), "The profile must have been provided");
        return ref.get();
    }

    @Test
    @DisplayName("Redirecting the share source via setOwner copies the redirected owner's researches")
    void testSetOwnerRedirectsSource() throws InterruptedException {
        Slimefun.getRegistry().setResearchingEnabled(true);

        Player boundOwner = server.addPlayer();
        Player template = server.addPlayer();
        Player receiver = server.addPlayer();

        Research boundResearch = new Research(new NamespacedKey(plugin, "knowledge_tome_redirect_bound"), 9821, "Bound Research", 0);
        boundResearch.register();
        Research templateResearch = new Research(new NamespacedKey(plugin, "knowledge_tome_redirect_template"), 9822, "Template Research", 0);
        templateResearch.register();

        TestUtilities.awaitProfile(boundOwner).setResearched(boundResearch, true);
        TestUtilities.awaitProfile(template).setResearched(templateResearch, true);

        ItemStack bound = boundTome(boundOwner.getUniqueId());

        Listener redirecting = new Listener() {
            @EventHandler
            public void onShare(KnowledgeTomeShareEvent event) {
                Assertions.assertEquals(boundOwner.getUniqueId(), event.getOwner(), "The source must default to the bound owner");
                event.setOwner(template.getUniqueId());
            }
        };
        server.getPluginManager().registerEvents(redirecting, plugin);

        try {
            use(receiver, bound);

            /*
             * The copy chain hops asynchronous profile loads and a scheduled sync task
             * (PlayerResearchTask schedules unlockResearch), so poll the receiver's profile
             * while draining the scheduler instead of awaiting a single signal.
             */
            PlayerProfile receiverProfile = profileOf(receiver);
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);

            while (System.nanoTime() < deadline && !receiverProfile.hasUnlocked(templateResearch)) {
                server.getScheduler().performOneTick();
                Thread.sleep(10);
            }

            Assertions.assertTrue(receiverProfile.hasUnlocked(templateResearch), "The redirected source's research must have been copied");
            Assertions.assertFalse(receiverProfile.hasUnlocked(boundResearch), "The bound owner's research must not have been copied");
        } finally {
            HandlerList.unregisterAll(redirecting);
        }
    }

    @Test
    @DisplayName("Cancelling KnowledgeTomeShareEvent keeps the tome")
    void testCancelKeepsTome() {
        Player owner = server.addPlayer();
        Player receiver = server.addPlayer();
        ItemStack bound = boundTome(owner.getUniqueId());

        Listener cancelling = new Listener() {
            @EventHandler
            public void onShare(KnowledgeTomeShareEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            use(receiver, bound);

            Assertions.assertEquals(1, bound.getAmount(), "A cancelled share must not consume the tome");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Sharing without listeners still consumes the tome, preserving the old behavior")
    void testShareWithoutListenersStillConsumes() {
        Player owner = server.addPlayer();
        Player receiver = server.addPlayer();
        ItemStack bound = boundTome(owner.getUniqueId());

        use(receiver, bound);

        Assertions.assertEquals(0, bound.getAmount(), "The tome must have been consumed");
    }

    @Test
    @DisplayName("Using a tome bound to yourself fires no event")
    void testSelfUseFiresNothing() {
        Player owner = server.addPlayer();
        ItemStack bound = boundTome(owner.getUniqueId());

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onShare(KnowledgeTomeShareEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            use(owner, bound);

            Assertions.assertFalse(seen[0], "No event must be fired when using your own tome");
            Assertions.assertEquals(1, bound.getAmount(), "A self-used tome must not be consumed");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }

    @Test
    @DisplayName("Using an unbound tome binds it to the player and fires no share event")
    void testBindBranchFiresNoShareEvent() {
        Player player = server.addPlayer();
        ItemStack unbound = tome.getItem().clone();

        boolean[] seen = { false };
        Listener watcher = new Listener() {
            @EventHandler
            public void onShare(KnowledgeTomeShareEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(watcher, plugin);

        try {
            use(player, unbound);

            Assertions.assertFalse(seen[0], "Binding a tome must not fire the share event");
            String boundOwner = ChatColor.stripColor(unbound.getItemMeta().getLore().get(1));
            Assertions.assertEquals(player.getUniqueId().toString(), boundOwner, "The tome must have been bound to the player");
        } finally {
            HandlerList.unregisterAll(watcher);
        }
    }
}
