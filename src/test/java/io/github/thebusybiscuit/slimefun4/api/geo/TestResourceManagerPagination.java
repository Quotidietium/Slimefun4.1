package io.github.thebusybiscuit.slimefun4.api.geo;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Boundary coverage for the scan-results pagination in {@link ResourceManager}: the
 * menu layout skips its border columns, so every page holds 28 entries. The page count
 * used to be computed with a divisor of 36, which made entries 28-35 (and every
 * analogous tail range) unreachable from the next/previous buttons - the scan silently
 * hid those resources.
 *
 * @author Zurker
 */
class TestResourceManagerPagination {

    @Test
    @DisplayName("Exactly one full page of resources fits on a single page")
    void testExactlyOnePage() {
        Assertions.assertEquals(1, ResourceManager.getPageCount(0), "No resources still means one (empty) page");
        Assertions.assertEquals(1, ResourceManager.getPageCount(1));
        Assertions.assertEquals(1, ResourceManager.getPageCount(28));
    }

    @Test
    @DisplayName("29 resources need a second page - this is the previously hidden tail")
    void testFirstTailPage() {
        // 29 was the first size where the old divisor of 36 returned one page,
        // hiding the 29th resource from the scan results forever
        Assertions.assertEquals(2, ResourceManager.getPageCount(29));
        Assertions.assertEquals(2, ResourceManager.getPageCount(36));
    }

    @Test
    @DisplayName("Page boundaries stay aligned at every multiple of 28")
    void testPageBoundaries() {
        Assertions.assertEquals(2, ResourceManager.getPageCount(56));
        Assertions.assertEquals(3, ResourceManager.getPageCount(57));
        Assertions.assertEquals(3, ResourceManager.getPageCount(84));
        Assertions.assertEquals(4, ResourceManager.getPageCount(85));
    }

    @Test
    @DisplayName("The scan loop and the page count agree on the page size")
    void testPageSizeConstant() {
        Assertions.assertEquals(28, ResourceManager.RESOURCES_PER_PAGE);
    }
}
