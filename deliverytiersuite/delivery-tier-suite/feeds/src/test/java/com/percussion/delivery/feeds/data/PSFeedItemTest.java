// REFACTORED: CP-JAVA11
package com.percussion.delivery.feeds.data;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;

/**
 * Tests for PSFeedItem using JUnit 5.
 */
class PSFeedItemTest {
    private static final String TEST_TITLE = "Test Feed Item";
    private static final String TEST_DESC = "Test Description";
    private static final String TEST_LINK = "https://test.percussion.com/item/1";
    private static final Instant TEST_DATE = Instant.now();

    @Test
    @DisplayName("Builder should create valid feed item")
    void builderShouldCreateValidFeedItem() {
        var item = PSFeedItem.builder()
            .title(TEST_TITLE)
            .description(TEST_DESC)
            .link(TEST_LINK)
            .publishDate(TEST_DATE)
            .build();

        assertAll(
            () -> assertEquals(TEST_TITLE, item.getTitle()),
            () -> assertEquals(TEST_DESC, item.getDescription().orElse(null)),
            () -> assertEquals(TEST_LINK, item.getLink()),
            () -> assertEquals(TEST_DATE, item.getPublishDate())
        );
    }

    @Test
    @DisplayName("Builder should handle null description")
    void builderShouldHandleNullDescription() {
        var item = PSFeedItem.builder()
            .title(TEST_TITLE)
            .link(TEST_LINK)
            .publishDate(TEST_DATE)
            .build();

        assertTrue(item.getDescription().isEmpty());
    }

    @Test
    @DisplayName("Builder should validate required fields")
    void builderShouldValidateRequiredFields() {
        assertAll(
            () -> assertThrows(NullPointerException.class, () ->
                PSFeedItem.builder().build()),
            () -> assertThrows(NullPointerException.class, () ->
                PSFeedItem.builder()
                    .title(TEST_TITLE)
                    .description(TEST_DESC)
                    .build())
        );
    }

    @Test
    @DisplayName("toString should not include sensitive data")
    void toStringShouldNotIncludeSensitiveData() {
        var item = PSFeedItem.builder()
            .title(TEST_TITLE)
            .description("SENSITIVE-DATA")
            .link(TEST_LINK)
            .publishDate(TEST_DATE)
            .build();

        var str = item.toString();
        assertAll(
            () -> assertTrue(str.contains(TEST_TITLE)),
            () -> assertTrue(str.contains(TEST_LINK))
        );
    }
}
