package io.github.thebusybiscuit.slimefun4.core.services;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.thebusybiscuit.slimefun4.api.SlimefunBranch;

class TestUpdaterService {

    @Test
    @DisplayName("Test if the development branch is recognized correctly")
    void testDevelopmentBuilds() {
        UpdaterService service = new UpdaterService("Dev - 131 (git 123456)");
        Assertions.assertEquals(SlimefunBranch.DEVELOPMENT, service.getBranch());
        Assertions.assertTrue(service.getBranch().isOfficial());
    }

    @Test
    @DisplayName("Test if the stable branch is recognized correctly")
    void testStableBuilds() {
        UpdaterService service = new UpdaterService("RC - 6 (git 123456)");
        Assertions.assertEquals(SlimefunBranch.STABLE, service.getBranch());
        Assertions.assertTrue(service.getBranch().isOfficial());
    }

    @Test
    @DisplayName("Test if an unofficial build is recognized correctly")
    void testUnofficialBuilds() {
        UpdaterService service = new UpdaterService("4.20 UNOFFICIAL");
        Assertions.assertEquals(SlimefunBranch.UNOFFICIAL, service.getBranch());
        Assertions.assertFalse(service.getBranch().isOfficial());
    }

    @Test
    @DisplayName("Test if unknown builds are caught")
    void testUnknownBuilds() {
        UpdaterService service = new UpdaterService("I am special");
        Assertions.assertEquals(SlimefunBranch.UNKNOWN, service.getBranch());
        Assertions.assertFalse(service.getBranch().isOfficial());
    }
}
