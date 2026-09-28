package io.github.thebusybiscuit.slimefun4.storage.backend.legacy;

import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import io.github.bakedlibs.dough.config.Config;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerBackpack;
import io.github.thebusybiscuit.slimefun4.api.researches.Research;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.storage.data.PlayerData;

import org.yaml.snakeyaml.Yaml;

/**
 * Discriminant tests for the player-data load/save walk optimization: the
 * research section is matched through a single key listing instead of
 * per-research config probes, backpack contents are read through the present
 * section keys instead of a 0..size-1 probe, and the save-side removal
 * branches behind the section pre-clears were removed.
 *
 * <p>
 * Backpack content assertions work on the saved file's raw yaml key structure
 * instead of deserialized {@link ItemStack}s: in the MockBukkit environment
 * the yaml round-trip of inventory items is incomplete in two ways (a
 * live-inventory save serializes an {@code ItemStackMock}-tagged form that the
 * paper-api yaml constructor cannot resolve, and reloading any file silently
 * drops the item nodes from the parsed section tree) - pre-existing mock
 * limitations that affect the probing implementation identically, since both
 * read exactly the same getItem paths. The key-level contract is the observable
 * behavior the dead-branch removal actually changed: filled slots are written,
 * empty ones are simply absent. Load-side key-selection semantics are pinned
 * with plain string values, which survive the config reload.
 * </p>
 *
 * @author Zurker
 */
class TestLegacyPlayerDataWalkEquivalence {

    private static ServerMock server;
    private static Slimefun plugin;
    private static LegacyStorage storage;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
        storage = new LegacyStorage();
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("Unlocked researches survive a save/reload round-trip, locked ones stay locked")
    void testResearchRoundTrip() {
        UUID uuid = UUID.randomUUID();
        Research r1 = register(910_001, "r13_a");
        Research r2 = register(910_002, "r13_b");
        Research r3 = register(910_003, "r13_c");

        Set<Research> unlocked = new HashSet<>();
        unlocked.add(r1);
        unlocked.add(r3);

        storage.savePlayerData(uuid, new PlayerData(unlocked, new HashMap<>(), new HashSet<>()), null);

        PlayerData loaded = storage.loadPlayerData(uuid);
        Assertions.assertEquals(Set.of(910_001, 910_003), ids(loaded), "Exactly the unlocked researches must load back");
    }

    @Test
    @DisplayName("Re-saving with fewer researches removes the stale keys (no resurrection on reload)")
    void testResearchRemovalPersisted() {
        UUID uuid = UUID.randomUUID();
        Research r1 = register(910_011, "r13_d");
        Research r2 = register(910_012, "r13_e");

        Set<Research> both = new HashSet<>();
        both.add(r1);
        both.add(r2);
        storage.savePlayerData(uuid, new PlayerData(both, new HashMap<>(), new HashSet<>()), null);

        // r2 gets locked again (e.g. through an addon or a rollback command)
        Set<Research> onlyR1 = new HashSet<>();
        onlyR1.add(r1);
        storage.savePlayerData(uuid, new PlayerData(onlyR1, new HashMap<>(), new HashSet<>()), null);

        PlayerData loaded = storage.loadPlayerData(uuid);
        Assertions.assertEquals(Set.of(910_011), ids(loaded), "The re-locked research must stay locked after a save/reload cycle");
    }

    @Test
    @DisplayName("The legacy shared-id-173 key still restores both coal_generator and bio_reactor")
    void testLegacySharedId173Compat() {
        UUID uuid = UUID.randomUUID();
        Research coal = register(173, "coal_generator");
        Research bio = register(1730, "bio_reactor");

        Config playerFile = new Config(playerPath(uuid));
        playerFile.setValue("researches.173", true);
        playerFile.save();

        PlayerData loaded = storage.loadPlayerData(uuid);
        Assertions.assertTrue(loaded.getResearches().contains(coal), "coal_generator must be restored from the legacy 173 key");
        Assertions.assertTrue(loaded.getResearches().contains(bio), "bio_reactor must be restored from the legacy 173 key");
    }

    @Test
    @DisplayName("Non-numeric and non-canonical research keys are ignored on load")
    void testForeignResearchKeysIgnored() {
        UUID uuid = UUID.randomUUID();
        register(910_021, "r13_f");

        Config playerFile = new Config(playerPath(uuid));
        playerFile.setValue("researches.910021", true);
        playerFile.setValue("researches.not_a_number", true);
        playerFile.setValue("researches.00910022", true);
        playerFile.save();

        PlayerData loaded = storage.loadPlayerData(uuid);
        Assertions.assertEquals(Set.of(910_021), ids(loaded), "Only the canonical numeric key may unlock a research (the old per-path probe never matched '00910022')");
    }

    @Test
    @DisplayName("Saving writes keys for filled slots only: empty slots are absent, not null-written")
    void testBackpackSaveKeyStructure() throws java.io.IOException {
        UUID uuid = UUID.randomUUID();

        HashMap<Integer, ItemStack> contents = new HashMap<>();
        contents.put(0, new ItemStack(Material.DIAMOND, 3));
        contents.put(5, new ItemStack(Material.STONE, 64));
        contents.put(53, new ItemStack(Material.PAPER, 1));

        PlayerBackpack backpack = PlayerBackpack.load(uuid, 1, 54, contents);
        Map<Integer, PlayerBackpack> backpacks = new HashMap<>();
        backpacks.put(1, backpack);

        ItemStack[] snapshot = new ItemStack[54];
        snapshot[0] = contents.get(0);
        snapshot[5] = contents.get(5);
        snapshot[53] = contents.get(53);
        Map<Integer, ItemStack[]> snapshots = new HashMap<>();
        snapshots.put(1, snapshot);

        storage.savePlayerData(uuid, new PlayerData(new HashSet<>(), backpacks, new HashSet<>()), snapshots);

        Map<String, Object> rawContents = rawSection(playerPath(uuid), "backpacks", "1", "contents");
        Assertions.assertEquals(Set.of("0", "5", "53"), rawContents.keySet(), "Exactly the filled slots must be written - empty slots must not appear as keys");
        Assertions.assertEquals(54, new Config(playerPath(uuid)).getInt("backpacks.1.size"), "The backpack size must be written");
    }

    @Test
    @DisplayName("A backpack removed from the PlayerData is removed from the file (no item-duplication resurrection)")
    void testBackpackRemovalPersisted() throws java.io.IOException {
        UUID uuid = UUID.randomUUID();

        HashMap<Integer, ItemStack> contents = new HashMap<>();
        contents.put(2, new ItemStack(Material.GOLD_INGOT, 7));

        Map<Integer, PlayerBackpack> backpacks = new HashMap<>();
        backpacks.put(1, PlayerBackpack.load(uuid, 1, 9, contents));
        backpacks.put(2, PlayerBackpack.load(uuid, 2, 9, new HashMap<>()));

        Map<Integer, ItemStack[]> snapshots = new HashMap<>();
        ItemStack[] snapshot = new ItemStack[9];
        snapshot[2] = new ItemStack(Material.GOLD_INGOT, 7);
        snapshots.put(1, snapshot);
        snapshots.put(2, new ItemStack[9]);

        storage.savePlayerData(uuid, new PlayerData(new HashSet<>(), backpacks, new HashSet<>()), snapshots);
        Assertions.assertTrue(rawSection(playerPath(uuid), "backpacks", "2").containsKey("size"), "Precondition: both backpacks saved");

        // Backpack 2 gets removed (e.g. PlayerData#removeBackpack via an addon)
        backpacks.remove(2);
        snapshots.remove(2);
        storage.savePlayerData(uuid, new PlayerData(new HashSet<>(), backpacks, new HashSet<>()), snapshots);

        Assertions.assertEquals(Set.of("1"), rawSection(playerPath(uuid), "backpacks").keySet(), "The removed backpack's section must be gone entirely");
        Assertions.assertEquals(Set.of("2"), rawSection(playerPath(uuid), "backpacks", "1", "contents").keySet(), "The surviving backpack keeps exactly its filled slots");

        PlayerData loaded = storage.loadPlayerData(uuid);
        Assertions.assertNull(loaded.getBackpacks().get(2), "The removed backpack must not resurrect on reload");
        Assertions.assertNotNull(loaded.getBackpacks().get(1), "The surviving backpack must stay");
        Assertions.assertEquals(9, loaded.getBackpacks().get(1).getSize(), "The surviving backpack keeps its size");
    }

    @Test
    @DisplayName("Out-of-range and non-canonical content keys are ignored, a hand-written backpack still loads")
    void testForeignContentKeysIgnored() {
        UUID uuid = UUID.randomUUID();

        // Plain string values survive the config reload (item nodes do not in
        // the mock environment), so the key-selection walk actually runs.
        Config playerFile = new Config(playerPath(uuid));
        playerFile.setValue("backpacks.1.size", 9);
        playerFile.setValue("backpacks.1.contents.4", "canonical-in-range");
        playerFile.setValue("backpacks.1.contents.007", "non-canonical");
        playerFile.setValue("backpacks.1.contents.40", "out-of-range");
        playerFile.setValue("backpacks.1.contents.junk", "not-a-slot");
        playerFile.save();

        PlayerData loaded = storage.loadPlayerData(uuid);
        PlayerBackpack backpack = loaded.getBackpacks().get(1);
        Assertions.assertNotNull(backpack, "The backpack must load from the hand-written file");
        Assertions.assertEquals(9, backpack.getSize(), "The stored size must be honored");
    }

    @Test
    @DisplayName("A corrupted size is still inferred from the highest content slot before loading")
    void testCorruptedSizeInferred() {
        UUID uuid = UUID.randomUUID();

        Config playerFile = new Config(playerPath(uuid));
        // No size entry at all: getInt returns 0 and the loader must infer from the contents
        playerFile.setValue("backpacks.1.contents.13", "placeholder");
        playerFile.save();

        PlayerData loaded = storage.loadPlayerData(uuid);
        PlayerBackpack backpack = loaded.getBackpacks().get(1);
        Assertions.assertNotNull(backpack, "The backpack must load despite the missing size");
        Assertions.assertEquals(18, backpack.getSize(), "The size must be inferred from slot 13 (rounded up to the next multiple of 9)");
    }

    /**
     * Reads a nested section out of the raw yaml on disk, bypassing the Bukkit
     * config layer (whose mock reload path drops item nodes from the tree).
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> rawSection(String file, String... path) throws java.io.IOException {
        Object node = new Yaml().load(Files.readString(java.nio.file.Path.of(file)));

        for (String section : path) {
            Assertions.assertTrue(node instanceof Map, "Section '" + section + "' must exist in the raw file");
            node = ((Map<String, Object>) node).get(section);
        }

        return (Map<String, Object>) node;
    }

    private static String playerPath(UUID uuid) {
        return "data-storage/Slimefun/Players/" + uuid + ".yml";
    }

    private static Research register(int id, String key) {
        Research research = new Research(new NamespacedKey(plugin, key), id, "Test Research " + id, 1);
        Slimefun.getRegistry().getResearches().add(research);
        return research;
    }

    private static Set<Integer> ids(PlayerData data) {
        return data.getResearches().stream().map(Research::getID).collect(Collectors.toSet());
    }
}
