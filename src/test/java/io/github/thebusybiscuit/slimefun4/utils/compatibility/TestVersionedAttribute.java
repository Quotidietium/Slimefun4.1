package io.github.thebusybiscuit.slimefun4.utils.compatibility;

import org.bukkit.attribute.Attribute;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TestVersionedAttribute {

    @Test
    @DisplayName("Test that the max health Attribute is resolved on every supported version")
    void testMaxHealthResolved() {
        // VersionedAttribute resolves the "max health" Attribute reflectively so that a
        // single build runs on both Minecraft <= 1.21.1 (GENERIC_MAX_HEALTH) and
        // >= 1.21.2 (MAX_HEALTH). It must never be null on any supported server version,
        // otherwise every caller (VampireBlade, Bandage, Splint, MedicalSupply) would
        // throw a NullPointerException at runtime.
        Attribute maxHealth = VersionedAttribute.MAX_HEALTH;
        Assertions.assertNotNull(maxHealth, "VersionedAttribute.MAX_HEALTH must resolve on every supported Minecraft version");
    }
}
