package io.github.thebusybiscuit.slimefun4.api.profiles;

import java.util.Optional;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.thebusybiscuit.slimefun4.api.player.PlayerBackpack;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.test.TestUtilities;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

class TestPlayerBackpacks {

    private static ServerMock server;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        MockBukkit.load(Slimefun.class);
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("Test creating a new Player Backpack")
    void testCreateBackpack() throws InterruptedException {
        Player player = server.addPlayer();
        PlayerProfile profile = TestUtilities.awaitProfile(player);

        PlayerBackpack backpack = profile.createBackpack(18);

        Assertions.assertNotNull(backpack);

        Assertions.assertEquals(player.getUniqueId(), backpack.getOwnerId());
        Assertions.assertEquals(18, backpack.getSize());
        Assertions.assertEquals(18, backpack.getInventory().getSize());
    }

    @Test
    @DisplayName("Test creating a new backpack will increment the id")
    void testCreateBackpackIncrementsId() throws InterruptedException {
        Player player = server.addPlayer();
        PlayerProfile profile = TestUtilities.awaitProfile(player);

        PlayerBackpack backpackOne = profile.createBackpack(18);
        PlayerBackpack backpackTwo = profile.createBackpack(18);
        PlayerBackpack backpackThree = profile.createBackpack(18);

        Assertions.assertEquals(0, backpackOne.getId());
        Assertions.assertEquals(1, backpackTwo.getId());
        Assertions.assertEquals(2, backpackThree.getId());
    }

    @Test
    @DisplayName("Test upgrading the backpack size")
    void testChangeSize() throws InterruptedException {
        Player player = server.addPlayer();
        PlayerProfile profile = TestUtilities.awaitProfile(player);
        PlayerBackpack backpack = profile.createBackpack(9);

        Assertions.assertThrows(IllegalArgumentException.class, () -> profile.createBackpack(-9));
        Assertions.assertThrows(IllegalArgumentException.class, () -> profile.createBackpack(12));
        Assertions.assertThrows(IllegalArgumentException.class, () -> profile.createBackpack(3000));

        Assertions.assertThrows(IllegalArgumentException.class, () -> backpack.setSize(-9));
        Assertions.assertThrows(IllegalArgumentException.class, () -> backpack.setSize(12));
        Assertions.assertThrows(IllegalArgumentException.class, () -> backpack.setSize(3000));

        backpack.setSize(27);

        Assertions.assertEquals(27, backpack.getSize());
    }

    @Test
    @DisplayName("Test that growing a backpack preserves its contents")
    void testGrowPreservesContents() throws InterruptedException {
        Player player = server.addPlayer();
        PlayerProfile profile = TestUtilities.awaitProfile(player);
        PlayerBackpack backpack = profile.createBackpack(9);
        ItemStack item = new ItemStack(Material.DIAMOND, 12);
        backpack.getInventory().setItem(4, item);

        backpack.setSize(54);

        Assertions.assertEquals(54, backpack.getSize());
        Assertions.assertEquals(54, backpack.getInventory().getSize());
        Assertions.assertEquals(item, backpack.getInventory().getItem(4));
    }

    @Test
    @DisplayName("Test that shrinking a backpack with items beyond the new size is refused")
    void testShrinkWithItemsIsRefused() throws InterruptedException {
        Player player = server.addPlayer();
        PlayerProfile profile = TestUtilities.awaitProfile(player);
        PlayerBackpack backpack = profile.createBackpack(27);
        ItemStack item = new ItemStack(Material.DIAMOND, 12);
        backpack.getInventory().setItem(20, item);

        Assertions.assertThrows(IllegalStateException.class, () -> backpack.setSize(9));

        // The backpack must remain in its previous, fully working state
        Assertions.assertEquals(27, backpack.getSize());
        Assertions.assertEquals(27, backpack.getInventory().getSize());
        Assertions.assertEquals(item, backpack.getInventory().getItem(20));
    }

    @Test
    @DisplayName("Test that shrinking a backpack with an empty tail works")
    void testShrinkWithEmptyTail() throws InterruptedException {
        Player player = server.addPlayer();
        PlayerProfile profile = TestUtilities.awaitProfile(player);
        PlayerBackpack backpack = profile.createBackpack(27);
        ItemStack item = new ItemStack(Material.DIAMOND, 12);
        backpack.getInventory().setItem(3, item);

        backpack.setSize(9);

        Assertions.assertEquals(9, backpack.getSize());
        Assertions.assertEquals(9, backpack.getInventory().getSize());
        Assertions.assertEquals(item, backpack.getInventory().getItem(3));
    }

    @Test
    @DisplayName("Test that a removed backpack's id is never reassigned")
    void testCreateBackpackAfterRemoval() throws InterruptedException {
        Player player = server.addPlayer();
        PlayerProfile profile = TestUtilities.awaitProfile(player);

        PlayerBackpack backpackOne = profile.createBackpack(9);
        PlayerBackpack backpackTwo = profile.createBackpack(9);
        Assertions.assertEquals(0, backpackOne.getId());
        Assertions.assertEquals(1, backpackTwo.getId());

        profile.getPlayerData().removeBackpack(backpackOne);

        // Using the map size would reassign id 1 and overwrite backpackTwo
        PlayerBackpack backpackThree = profile.createBackpack(9);
        Assertions.assertEquals(2, backpackThree.getId());

        Optional<PlayerBackpack> existing = profile.getBackpack(1);
        Assertions.assertTrue(existing.isPresent());
        Assertions.assertEquals(backpackTwo, existing.get());
    }

    @Test
    @DisplayName("Test getting a backpack by its id")
    void testGetBackpackById() throws InterruptedException {
        Player player = server.addPlayer();
        PlayerProfile profile = TestUtilities.awaitProfile(player);
        PlayerBackpack backpack = profile.createBackpack(9);
        int id = backpack.getId();

        Assertions.assertThrows(IllegalArgumentException.class, () -> profile.getBackpack(-20));

        Optional<PlayerBackpack> optional = profile.getBackpack(id);
        Assertions.assertTrue(optional.isPresent());
        Assertions.assertEquals(backpack, optional.get());

        Assertions.assertFalse(profile.getBackpack(500).isPresent());
    }
}
