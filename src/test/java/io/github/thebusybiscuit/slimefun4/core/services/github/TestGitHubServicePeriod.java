package io.github.thebusybiscuit.slimefun4.core.services.github;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Regression coverage for the connector-refresh scheduling period: the old code passed
 * {@code TimeUnit.HOURS.toMillis(1)} into a ticks parameter, stretching the refresh
 * cycle to 50 hours instead of one.
 */
class TestGitHubServicePeriod {

    @Test
    @DisplayName("The connector refresh period is one hour in ticks")
    void testRefreshPeriod() {
        Assertions.assertEquals(72_000L, GitHubService.getRefreshPeriodTicks(), "One hour must be 72000 ticks, not a milliseconds value");
    }
}
