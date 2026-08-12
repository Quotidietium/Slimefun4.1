package io.github.thebusybiscuit.slimefun4.implementation.items.magical.runes;

import java.util.List;
import java.util.function.Predicate;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
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
import be.seeseemelk.mockbukkit.entity.PlayerMock;

import io.github.thebusybiscuit.slimefun4.api.events.EnchantmentRuneApplyEvent;
import io.github.thebusybiscuit.slimefun4.api.events.SoulboundRuneApplyEvent;
import io.github.thebusybiscuit.slimefun4.api.events.VillagerRuneResetEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;

/**
 * Regression coverage for the magical rune API expansion: {@link EnchantmentRuneApplyEvent},
 * {@link SoulboundRuneApplyEvent} and {@link VillagerRuneResetEvent}, exercised through the
 * real {@link EnchantmentRune}, {@link SoulboundRune} and {@link VillagerRune} item handlers.
 *
 * @author Zurker
 */
class TestRuneEvents {

    private static ServerMock server;
    private static Slimefun plugin;
    private static World villagerWorld;

    private static EnchantmentRune enchantmentRune;
    private static SoulboundRune soulboundRune;
    private static VillagerRune villagerRune;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        // A plain mock: WorldMock does not implement every spawnParticle variant used by VillagerRune
        villagerWorld = Mockito.mock(World.class);

        ItemGroup itemGroup = TestUtilities.getItemGroup(plugin, "rune_events_test");

        enchantmentRune = new EnchantmentRune(itemGroup, new SlimefunItemStack("TEST_ENCHANTMENT_RUNE", Material.FIRE_CHARGE, "&5Test Enchantment Rune"), RecipeType.NULL, new ItemStack[9]);
        enchantmentRune.register(plugin);

        soulboundRune = new SoulboundRune(itemGroup, new SlimefunItemStack("TEST_SOULBOUND_RUNE", Material.FIRE_CHARGE, "&5Test Soulbound Rune"), RecipeType.NULL, new ItemStack[9]);
        soulboundRune.register(plugin);

        villagerRune = new VillagerRune(itemGroup, new SlimefunItemStack("TEST_VILLAGER_RUNE", Material.FIRE_CHARGE, "&5Test Villager Rune"), RecipeType.NULL, new ItemStack[9], null);
        villagerRune.register(plugin);

        // onUnitTestStart() never starts the integrations, so start them manually and
        // run the scheduled onServerStart task to create the ProtectionManager (queried by VillagerRune)
        Slimefun.getIntegrations().start();
        server.getScheduler().performOneTick();
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
     * Creates a dropped {@link Item} entity mock. The {@link World} is a mock so the
     * nearby-entity scan of the rune rituals can be fed with a compatible target.
     */
    private Item newDroppedItem(Location location, ItemStack stack) {
        Item entity = Mockito.mock(Item.class);
        Mockito.when(entity.getItemStack()).thenReturn(stack);
        Mockito.when(entity.isValid()).thenReturn(true);
        Mockito.when(entity.getLocation()).thenReturn(location);
        return entity;
    }

    private void stubNearbyTarget(World world, Item target) {
        Mockito.doAnswer(invocation -> List.of(target)).when(world).getNearbyEntities(Mockito.any(Location.class), Mockito.anyDouble(), Mockito.anyDouble(), Mockito.anyDouble(), Mockito.<Predicate<Entity>>any());
    }

    // ---------- EnchantmentRuneApplyEvent ----------

    @Test
    @DisplayName("EnchantmentRuneApplyEvent exposes its fields and validates constructor and setter arguments")
    void testEnchantmentEventFieldsAndValidation() {
        PlayerMock player = server.addPlayer();
        Item runeEntity = Mockito.mock(Item.class);
        Item targetEntity = Mockito.mock(Item.class);
        ItemStack stack = new ItemStack(Material.DIAMOND_SWORD);

        EnchantmentRuneApplyEvent event = new EnchantmentRuneApplyEvent(player, runeEntity, targetEntity, stack, Enchantment.SHARPNESS, 2);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(runeEntity, event.getRune());
        Assertions.assertEquals(targetEntity, event.getItem());
        Assertions.assertEquals(stack, event.getItemStack());
        Assertions.assertEquals(Enchantment.SHARPNESS, event.getEnchantment());
        Assertions.assertEquals(2, event.getLevel());
        Assertions.assertFalse(event.isCancelled());

        event.setEnchantment(Enchantment.PROTECTION);
        event.setLevel(4);
        Assertions.assertEquals(Enchantment.PROTECTION, event.getEnchantment());
        Assertions.assertEquals(4, event.getLevel());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new EnchantmentRuneApplyEvent(player, null, targetEntity, stack, Enchantment.SHARPNESS, 2));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new EnchantmentRuneApplyEvent(player, runeEntity, null, stack, Enchantment.SHARPNESS, 2));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new EnchantmentRuneApplyEvent(player, runeEntity, targetEntity, null, Enchantment.SHARPNESS, 2));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new EnchantmentRuneApplyEvent(player, runeEntity, targetEntity, stack, null, 2));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new EnchantmentRuneApplyEvent(player, runeEntity, targetEntity, stack, Enchantment.SHARPNESS, 0));
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setEnchantment(null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setLevel(0));
    }

    @Test
    @DisplayName("EnchantmentRune fires EnchantmentRuneApplyEvent and applies the chosen enchantment")
    void testEnchantmentRuneSuccess() {
        PlayerMock player = server.addPlayer();
        World world = Mockito.mock(World.class);
        Location loc = new Location(world, 0.5, 64, 0.5);

        Item runeEntity = newDroppedItem(loc, enchantmentRune.getItem().clone());
        ItemStack targetStack = new ItemStack(Material.DIAMOND_SWORD);
        Item targetEntity = newDroppedItem(loc, targetStack);
        stubNearbyTarget(world, targetEntity);

        boolean[] seen = { false };
        Listener listener = new Listener() {
            @EventHandler
            public void onApply(EnchantmentRuneApplyEvent event) {
                seen[0] = true;
                Assertions.assertEquals(player, event.getPlayer());
                Assertions.assertEquals(runeEntity, event.getRune());
                Assertions.assertEquals(targetEntity, event.getItem());
                Assertions.assertEquals(targetStack, event.getItemStack());
                Assertions.assertNotNull(event.getEnchantment());
                Assertions.assertTrue(event.getLevel() >= 1);
            }
        };
        server.getPluginManager().registerEvents(listener, plugin);

        try {
            boolean consumed = enchantmentRune.getItemHandler().onItemDrop(Mockito.mock(PlayerDropItemEvent.class), player, runeEntity);

            Assertions.assertTrue(consumed);
            Assertions.assertTrue(seen[0], "EnchantmentRuneApplyEvent was not fired");
            Mockito.verify(runeEntity).remove();
            Mockito.verify(targetEntity).remove();
            Assertions.assertFalse(targetStack.getEnchantments().isEmpty(), "The ritual must have enchanted the target stack");
        } finally {
            HandlerList.unregisterAll(listener);
        }
    }

    @Test
    @DisplayName("Cancelling EnchantmentRuneApplyEvent aborts the ritual, both entities remain")
    void testEnchantmentRuneCancellation() {
        PlayerMock player = server.addPlayer();
        World world = Mockito.mock(World.class);
        Location loc = new Location(world, 0.5, 64, 0.5);

        Item runeEntity = newDroppedItem(loc, enchantmentRune.getItem().clone());
        ItemStack targetStack = new ItemStack(Material.DIAMOND_SWORD);
        Item targetEntity = newDroppedItem(loc, targetStack);
        stubNearbyTarget(world, targetEntity);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onApply(EnchantmentRuneApplyEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            boolean consumed = enchantmentRune.getItemHandler().onItemDrop(Mockito.mock(PlayerDropItemEvent.class), player, runeEntity);

            Assertions.assertTrue(consumed);
            Mockito.verify(runeEntity, Mockito.never()).remove();
            Mockito.verify(targetEntity, Mockito.never()).remove();
            Mockito.verify(world, Mockito.never()).strikeLightningEffect(Mockito.any(Location.class));
            Assertions.assertTrue(targetStack.getEnchantments().isEmpty(), "A cancelled ritual must not enchant anything");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("EnchantmentRuneApplyEvent listeners can replace the enchantment and its level")
    void testEnchantmentRuneEnchantmentReplaced() {
        PlayerMock player = server.addPlayer();
        World world = Mockito.mock(World.class);
        Location loc = new Location(world, 0.5, 64, 0.5);

        Item runeEntity = newDroppedItem(loc, enchantmentRune.getItem().clone());
        ItemStack targetStack = new ItemStack(Material.DIAMOND_SWORD);
        Item targetEntity = newDroppedItem(loc, targetStack);
        stubNearbyTarget(world, targetEntity);

        Listener replacing = new Listener() {
            @EventHandler
            public void onApply(EnchantmentRuneApplyEvent event) {
                event.setEnchantment(Enchantment.SHARPNESS);
                event.setLevel(3);
            }
        };
        server.getPluginManager().registerEvents(replacing, plugin);

        try {
            enchantmentRune.getItemHandler().onItemDrop(Mockito.mock(PlayerDropItemEvent.class), player, runeEntity);

            Assertions.assertEquals(3, targetStack.getEnchantmentLevel(Enchantment.SHARPNESS), "The replacement enchantment must have been applied");
            Assertions.assertEquals(1, targetStack.getEnchantments().size(), "Only the replacement enchantment must have been applied");
        } finally {
            HandlerList.unregisterAll(replacing);
        }
    }

    // ---------- SoulboundRuneApplyEvent ----------

    @Test
    @DisplayName("SoulboundRuneApplyEvent exposes its fields and validates constructor arguments")
    void testSoulboundEventFieldsAndValidation() {
        PlayerMock player = server.addPlayer();
        Item runeEntity = Mockito.mock(Item.class);
        Item targetEntity = Mockito.mock(Item.class);
        ItemStack stack = new ItemStack(Material.DIAMOND_SWORD);

        SoulboundRuneApplyEvent event = new SoulboundRuneApplyEvent(player, runeEntity, targetEntity, stack);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(runeEntity, event.getRune());
        Assertions.assertEquals(targetEntity, event.getItem());
        Assertions.assertEquals(stack, event.getItemStack());
        Assertions.assertFalse(event.isCancelled());

        // The ritual can be redirected to a different dropped item
        Item retarget = Mockito.mock(Item.class);
        ItemStack retargetStack = new ItemStack(Material.EMERALD);
        Mockito.when(retarget.getItemStack()).thenReturn(retargetStack);
        event.setTarget(retarget);
        Assertions.assertEquals(retarget, event.getItem());
        Assertions.assertEquals(retargetStack, event.getItemStack(), "The item stack must follow the new target");

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setTarget(null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setTarget(Mockito.mock(Item.class)), "An item without a stack must be rejected");
        Assertions.assertThrows(IllegalArgumentException.class, () -> new SoulboundRuneApplyEvent(player, null, targetEntity, stack));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new SoulboundRuneApplyEvent(player, runeEntity, null, stack));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new SoulboundRuneApplyEvent(player, runeEntity, targetEntity, null));
    }

    @Test
    @DisplayName("SoulboundRune fires SoulboundRuneApplyEvent and makes the target soulbound")
    void testSoulboundRuneSuccess() {
        PlayerMock player = server.addPlayer();
        World world = Mockito.mock(World.class);
        Location loc = new Location(world, 0.5, 64, 0.5);

        Item runeEntity = newDroppedItem(loc, soulboundRune.getItem().clone());
        ItemStack targetStack = new ItemStack(Material.DIAMOND_SWORD);
        Item targetEntity = newDroppedItem(loc, targetStack);
        stubNearbyTarget(world, targetEntity);

        boolean[] seen = { false };
        Listener listener = new Listener() {
            @EventHandler
            public void onApply(SoulboundRuneApplyEvent event) {
                seen[0] = true;
                Assertions.assertEquals(player, event.getPlayer());
                Assertions.assertEquals(runeEntity, event.getRune());
                Assertions.assertEquals(targetEntity, event.getItem());
                Assertions.assertEquals(targetStack, event.getItemStack());
            }
        };
        server.getPluginManager().registerEvents(listener, plugin);

        try {
            boolean consumed = soulboundRune.getItemHandler().onItemDrop(Mockito.mock(PlayerDropItemEvent.class), player, runeEntity);

            Assertions.assertTrue(consumed);
            Assertions.assertTrue(seen[0], "SoulboundRuneApplyEvent was not fired");
            Mockito.verify(runeEntity).remove();
            Mockito.verify(targetEntity).remove();
            Assertions.assertTrue(SlimefunUtils.isSoulbound(targetStack), "The ritual must have made the target soulbound");
        } finally {
            HandlerList.unregisterAll(listener);
        }
    }

    @Test
    @DisplayName("Cancelling SoulboundRuneApplyEvent aborts the ritual, both entities remain")
    void testSoulboundRuneCancellation() {
        PlayerMock player = server.addPlayer();
        World world = Mockito.mock(World.class);
        Location loc = new Location(world, 0.5, 64, 0.5);

        Item runeEntity = newDroppedItem(loc, soulboundRune.getItem().clone());
        ItemStack targetStack = new ItemStack(Material.DIAMOND_SWORD);
        Item targetEntity = newDroppedItem(loc, targetStack);
        stubNearbyTarget(world, targetEntity);

        Listener cancelling = new Listener() {
            @EventHandler
            public void onApply(SoulboundRuneApplyEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            boolean consumed = soulboundRune.getItemHandler().onItemDrop(Mockito.mock(PlayerDropItemEvent.class), player, runeEntity);

            Assertions.assertTrue(consumed);
            Mockito.verify(runeEntity, Mockito.never()).remove();
            Mockito.verify(targetEntity, Mockito.never()).remove();
            Mockito.verify(world, Mockito.never()).strikeLightningEffect(Mockito.any(Location.class));
            Assertions.assertFalse(SlimefunUtils.isSoulbound(targetStack), "A cancelled ritual must not bind any souls");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("Redirecting SoulboundRuneApplyEvent binds the replacement target instead")
    void testSoulboundRuneRetargeted() {
        PlayerMock player = server.addPlayer();
        World world = Mockito.mock(World.class);
        Location loc = new Location(world, 0.5, 64, 0.5);

        Item runeEntity = newDroppedItem(loc, soulboundRune.getItem().clone());

        // The rune finds the diamond sword, but the listener prefers the emerald
        ItemStack swordStack = new ItemStack(Material.DIAMOND_SWORD);
        Item swordEntity = newDroppedItem(loc, swordStack);
        stubNearbyTarget(world, swordEntity);

        ItemStack emeraldStack = new ItemStack(Material.EMERALD);
        Item emeraldEntity = newDroppedItem(loc, emeraldStack);

        Listener redirecting = new Listener() {
            @EventHandler
            public void onApply(SoulboundRuneApplyEvent event) {
                Assertions.assertEquals(swordEntity, event.getItem(), "The ritual must default to the item the rune found");
                event.setTarget(emeraldEntity);
            }
        };
        server.getPluginManager().registerEvents(redirecting, plugin);

        try {
            boolean consumed = soulboundRune.getItemHandler().onItemDrop(Mockito.mock(PlayerDropItemEvent.class), player, runeEntity);

            Assertions.assertTrue(consumed);
            Mockito.verify(runeEntity).remove();
            Mockito.verify(emeraldEntity).remove();
            Mockito.verify(swordEntity, Mockito.never()).remove();
            Assertions.assertTrue(SlimefunUtils.isSoulbound(emeraldStack), "The replacement target must have become soulbound");
            Assertions.assertFalse(SlimefunUtils.isSoulbound(swordStack), "The originally found item must have been left untouched");
        } finally {
            HandlerList.unregisterAll(redirecting);
        }
    }

    // ---------- VillagerRuneResetEvent ----------

    private PlayerInteractEntityEvent newInteractEvent(PlayerMock player, Villager villager) {
        PlayerInteractEntityEvent event = Mockito.mock(PlayerInteractEntityEvent.class);
        Mockito.when(event.getPlayer()).thenReturn(player);
        Mockito.when(event.getRightClicked()).thenReturn(villager);
        Mockito.when(event.isCancelled()).thenReturn(false);
        return event;
    }

    private Villager newVillager(Villager.Profession profession) {
        Villager villager = Mockito.mock(Villager.class);
        Mockito.when(villager.getProfession()).thenReturn(profession);
        Mockito.when(villager.getLocation()).thenReturn(new Location(villagerWorld, 1, 65, 1));
        Mockito.when(villager.getWorld()).thenReturn(villagerWorld);
        return villager;
    }

    @Test
    @DisplayName("VillagerRuneResetEvent exposes its fields and validates constructor arguments")
    void testVillagerEventFieldsAndValidation() {
        PlayerMock player = server.addPlayer();
        Villager villager = Mockito.mock(Villager.class);
        ItemStack stack = new ItemStack(Material.FIRE_CHARGE);

        VillagerRuneResetEvent event = new VillagerRuneResetEvent(player, villager, stack);

        Assertions.assertEquals(player, event.getPlayer());
        Assertions.assertEquals(villager, event.getVillager());
        Assertions.assertEquals(stack, event.getRune());
        Assertions.assertEquals(Villager.Profession.NONE, event.getTargetProfession(), "The reset must default to clearing the profession");
        Assertions.assertFalse(event.isCancelled());

        // The resulting profession can be replaced
        event.setTargetProfession(Villager.Profession.MASON);
        Assertions.assertEquals(Villager.Profession.MASON, event.getTargetProfession());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> event.setTargetProfession(null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new VillagerRuneResetEvent(player, null, stack));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new VillagerRuneResetEvent(player, villager, null));
    }

    @Test
    @DisplayName("VillagerRune fires VillagerRuneResetEvent and resets the villager profession")
    void testVillagerRuneSuccess() {
        PlayerMock player = server.addPlayer();
        player.setGameMode(GameMode.SURVIVAL);
        Villager villager = newVillager(Villager.Profession.FARMER);
        PlayerInteractEntityEvent interactEvent = newInteractEvent(player, villager);
        ItemStack heldRune = villagerRune.getItem().clone();

        boolean[] seen = { false };
        Listener listener = new Listener() {
            @EventHandler
            public void onReset(VillagerRuneResetEvent event) {
                seen[0] = true;
                Assertions.assertEquals(player, event.getPlayer());
                Assertions.assertEquals(villager, event.getVillager());
                Assertions.assertEquals(heldRune, event.getRune());
            }
        };
        server.getPluginManager().registerEvents(listener, plugin);

        try {
            villagerRune.getItemHandler().onInteract(interactEvent, heldRune, false);

            Assertions.assertTrue(seen[0], "VillagerRuneResetEvent was not fired");
            Mockito.verify(villager).setProfession(Villager.Profession.NONE);
            Mockito.verify(villager).setVillagerExperience(0);
            Mockito.verify(villager).setVillagerLevel(1);
            Mockito.verify(interactEvent).setCancelled(true);
        } finally {
            HandlerList.unregisterAll(listener);
        }
    }

    @Test
    @DisplayName("Replacing the target profession rerolls the villager instead of clearing it")
    void testVillagerRuneProfessionRedirected() {
        PlayerMock player = server.addPlayer();
        player.setGameMode(GameMode.SURVIVAL);
        Villager villager = newVillager(Villager.Profession.FARMER);
        PlayerInteractEntityEvent interactEvent = newInteractEvent(player, villager);
        ItemStack heldRune = villagerRune.getItem().clone();

        Listener redirecting = new Listener() {
            @EventHandler
            public void onReset(VillagerRuneResetEvent event) {
                Assertions.assertEquals(Villager.Profession.NONE, event.getTargetProfession(), "The reset must default to clearing the profession");
                event.setTargetProfession(Villager.Profession.MASON);
            }
        };
        server.getPluginManager().registerEvents(redirecting, plugin);

        try {
            villagerRune.getItemHandler().onInteract(interactEvent, heldRune, false);

            Mockito.verify(villager).setProfession(Villager.Profession.MASON);
            Mockito.verify(villager, Mockito.never()).setProfession(Villager.Profession.NONE);
            Mockito.verify(villager).setVillagerExperience(0);
            Mockito.verify(villager).setVillagerLevel(1);
            Mockito.verify(interactEvent).setCancelled(true);
        } finally {
            HandlerList.unregisterAll(redirecting);
        }
    }

    @Test
    @DisplayName("Cancelling VillagerRuneResetEvent keeps the profession and the rune")
    void testVillagerRuneCancellation() {
        PlayerMock player = server.addPlayer();
        player.setGameMode(GameMode.SURVIVAL);
        Villager villager = newVillager(Villager.Profession.LIBRARIAN);
        PlayerInteractEntityEvent interactEvent = newInteractEvent(player, villager);
        ItemStack heldRune = villagerRune.getItem().clone();

        Listener cancelling = new Listener() {
            @EventHandler
            public void onReset(VillagerRuneResetEvent event) {
                event.setCancelled(true);
            }
        };
        server.getPluginManager().registerEvents(cancelling, plugin);

        try {
            villagerRune.getItemHandler().onInteract(interactEvent, heldRune, false);

            Mockito.verify(villager, Mockito.never()).setProfession(Mockito.any());
            Mockito.verify(villager, Mockito.never()).setVillagerExperience(Mockito.anyInt());
            Mockito.verify(villager, Mockito.never()).setVillagerLevel(Mockito.anyInt());
            Mockito.verify(interactEvent, Mockito.never()).setCancelled(true);
            Assertions.assertEquals(1, heldRune.getAmount(), "A cancelled reset must not consume the rune");
        } finally {
            HandlerList.unregisterAll(cancelling);
        }
    }

    @Test
    @DisplayName("VillagerRune does not fire an event for professionless villagers")
    void testVillagerRuneIgnoresProfessionless() {
        PlayerMock player = server.addPlayer();
        Villager villager = newVillager(Villager.Profession.NONE);
        PlayerInteractEntityEvent interactEvent = newInteractEvent(player, villager);
        ItemStack heldRune = villagerRune.getItem().clone();

        boolean[] seen = { false };
        Listener listener = new Listener() {
            @EventHandler
            public void onReset(VillagerRuneResetEvent event) {
                seen[0] = true;
            }
        };
        server.getPluginManager().registerEvents(listener, plugin);

        try {
            villagerRune.getItemHandler().onInteract(interactEvent, heldRune, false);

            Assertions.assertFalse(seen[0], "No event must be fired for a villager without a profession");
            Mockito.verify(villager, Mockito.never()).setProfession(Mockito.any());
        } finally {
            HandlerList.unregisterAll(listener);
        }
    }
}
