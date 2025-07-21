// REFACTORED: CP-JAVA11
package com.percussion.delivery.feeds;

import com.percussion.delivery.feeds.data.IPSFeedDescriptor;
import com.percussion.delivery.feeds.data.PSFeedDescriptor;
import com.percussion.delivery.feeds.data.PSFeedItem;
import com.rometools.rome.io.FeedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.List;

/**
 * Tests for PSFeedGenerator using JUnit 5.
 */
class PSFeedGeneratorTest {
    private PSFeedGenerator generator;
    private IPSFeedDescriptor descriptor;
    private List<PSFeedItem> items;
    private static final String TEST_HOST = "test.percussion.com";

    @BeforeEach
    void setUp() {
        generator = new PSFeedGenerator();
        descriptor = PSFeedDescriptor.builder()
            .name("test-feed")
            .site("test-site")
            .title("Test Feed")
            .description("Test Description")
            .link("https://test.percussion.com/feed")
            .type("ATOM")
            .build();

        items = List.of(
            PSFeedItem.builder()
                .title("Test Item 1")
                .description("Description 1")
                .link("https://test.percussion.com/item/1")
                .publishDate(Instant.now())
                .build(),
            PSFeedItem.builder()
                .title("Test Item 2")
                .description("Description 2")
                .link("https://test.percussion.com/item/2")
                .publishDate(Instant.now())
                .build()
        );
    }

    @Test
    @DisplayName("Should generate valid ATOM feed")
    void shouldGenerateValidAtomFeed() throws FeedException {
        var feed = generator.makeFeedContent(descriptor, TEST_HOST, items);

        assertAll(
            () -> assertNotNull(feed),
            () -> assertTrue(feed.contains("<?xml")),
            () -> assertTrue(feed.contains("<feed")),
            () -> assertTrue(feed.contains(descriptor.getTitle().orElse(""))),
            () -> assertTrue(feed.contains(items.get(0).getTitle()))
        );
    }

    @Test
    @DisplayName("Should handle empty item list")
    void shouldHandleEmptyItemList() throws FeedException {
        var feed = generator.makeFeedContent(descriptor, TEST_HOST, List.of());

        assertAll(
            () -> assertNotNull(feed),
            () -> assertTrue(feed.contains("<?xml")),
            () -> assertTrue(feed.contains(descriptor.getTitle().orElse("")))
        );
    }

    @Test
    @DisplayName("Should throw exception for null inputs")
    void shouldThrowExceptionForNullInputs() {
        assertAll(
            () -> assertThrows(NullPointerException.class,
                () -> generator.makeFeedContent(null, TEST_HOST, items)),
            () -> assertThrows(NullPointerException.class,
                () -> generator.makeFeedContent(descriptor, null, items)),
            () -> assertThrows(NullPointerException.class,
                () -> generator.makeFeedContent(descriptor, TEST_HOST, null))
        );
    }
}
