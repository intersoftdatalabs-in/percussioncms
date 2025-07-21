// REFACTORED: CP-JAVA11
package com.percussion.delivery.feeds.test;

import com.percussion.delivery.feeds.data.PSFeedItem;
import com.percussion.delivery.feeds.data.PSFeedDescriptor;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Test utilities for feed-related tests.
 * Provides factory methods for creating test data.
 */
public final class FeedTestUtils {
    private FeedTestUtils() {
        // Utility class, no instantiation
    }

    /**
     * Creates a test feed descriptor.
     *
     * @param name Feed name
     * @param site Site name
     * @param type Feed type (ATOM, RSS1, RSS2)
     * @return Test feed descriptor
     */
    public static PSFeedDescriptor createTestDescriptor(String name, String site, String type) {
        return PSFeedDescriptor.builder()
            .name(name)
            .site(site)
            .title("Test Feed " + name)
            .description("Test Description for " + name)
            .link("https://test.percussion.com/feed/" + name)
            .type(type)
            .build();
    }

    /**
     * Creates a list of test feed items.
     *
     * @param count Number of items to create
     * @param baseUrl Base URL for item links
     * @return List of test feed items
     */
    public static List<PSFeedItem> createTestItems(int count, String baseUrl) {
        return IntStream.range(0, count)
            .mapToObj(i -> PSFeedItem.builder()
                .title("Test Item " + i)
                .description("Description for item " + i)
                .link(baseUrl + "/item/" + i)
                .publishDate(Instant.now().minusSeconds(i * 3600)) // 1 hour apart
                .build())
            .collect(Collectors.toList());
    }

    /**
     * Verifies if a string contains valid XML.
     *
     * @param xml XML string to verify
     * @return true if valid XML
     */
    public static boolean isValidXml(String xml) {
        try {
            javax.xml.parsers.DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(new org.xml.sax.InputSource(new java.io.StringReader(xml)));
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
