package io.github.thebusybiscuit.slimefun4.core.services;

import java.lang.reflect.Field;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;

/**
 * Regression coverage for {@link AutoSavingService#start(Slimefun, int)} interval
 * validation: a missing or corrupted config value (0) or a negative delay would
 * schedule the repeating save tasks with a nonsensical period - every tick (disk
 * and log storm) or never repeating (auto-save silently disabled). The interval
 * must be clamped to the documented default of 10 minutes.
 *
 * @author Zurker
 */
class TestAutoSavingService {

    private static ServerMock server;
    private static Slimefun plugin;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    private static int getInterval(AutoSavingService service) throws ReflectiveOperationException {
        Field field = AutoSavingService.class.getDeclaredField("interval");
        field.setAccessible(true);
        return field.getInt(service);
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, -1, -500 })
    @DisplayName("An invalid auto-save interval is clamped to the 10 minute default")
    void testInvalidIntervalClampedToDefault(int invalid) throws ReflectiveOperationException {
        AutoSavingService service = new AutoSavingService();
        service.start(plugin, invalid);

        Assertions.assertEquals(10, getInterval(service), "An invalid interval must fall back to the default instead of scheduling a broken period");
    }

    @Test
    @DisplayName("A valid auto-save interval is kept as configured")
    void testValidIntervalKept() throws ReflectiveOperationException {
        AutoSavingService service = new AutoSavingService();
        service.start(plugin, 30);

        Assertions.assertEquals(30, getInterval(service));
    }
}
