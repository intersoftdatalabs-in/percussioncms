// REFACTORED: CP-JAVA11
package com.percussion.delivery.feeds.integration;

import com.percussion.delivery.feeds.PSFeedGenerator;
import com.percussion.delivery.feeds.data.*;
import com.percussion.delivery.feeds.services.IPSFeedDao;
import com.percussion.delivery.feeds.services.rdbms.PSConnectionInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the feeds module.
 * Tests the interaction between feed generation, data access, and connection handling.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {
    "classpath:test-beans.xml",
    "classpath:perc-datasources.xml"
})
@Transactional
class FeedsIntegrationTest {
    @Autowired
    private IPSFeedDao feedDao;

    @Autowired
    private PSFeedGenerator feedGenerator;

    private static final String TEST_SITE = "test-site";
    private static final String TEST_HOST = "test.percussion.com";

    @BeforeEach
    void setUp() {
        // Set up test connection info
        feedDao.saveConnectionInfo(
            "https://test.percussion.com/api",
            "testuser",
            "encrypted-password",
            true
        );
    }

    @Test
    @DisplayName("Should generate feed with data from DAO")
    void shouldGenerateFeedWithDataFromDao() throws Exception {
        // Create and save test descriptor
        var descriptor = PSFeedDescriptor.builder()
            .name("test-feed")
            .site(TEST_SITE)
            .title("Test Feed")
            .description("Integration Test Feed")
            .link("https://test.percussion.com/feed")
            .type("ATOM")
            .build();

        feedDao.saveDescriptors(List.of(descriptor));

        // Create test items
        var items = List.of(
            PSFeedItem.builder()
                .title("Test Item 1")
                .description("Description 1")
                .link("https://test.percussion.com/item/1")
                .publishDate(Instant.now())
                .build()
        );

        // Generate feed
        var feed = feedGenerator.makeFeedContent(descriptor, TEST_HOST, items);

        // Verify feed content
        assertAll(
            () -> assertTrue(feed.contains("<?xml")),
            () -> assertTrue(feed.contains("<feed")),
            () -> assertTrue(feed.contains(descriptor.getTitle().orElse(""))),
            () -> assertTrue(feed.contains(items.get(0).getTitle())),
            () -> assertTrue(feed.contains(TEST_HOST))
        );
    }

    @Test
    @DisplayName("Should handle secure connection info")
    void shouldHandleSecureConnectionInfo() {
        var connectionInfo = feedDao.getConnectionInfo();

        assertAll(
            () -> assertTrue(connectionInfo.isPresent()),
            () -> assertTrue(connectionInfo.get().isEncrypted()),
            () -> assertFalse(connectionInfo.get().toString().contains("encrypted-password"))
        );
    }

    @Test
    @DisplayName("Should find feeds by site")
    void shouldFindFeedsBySite() {
        // Create test descriptors
        var descriptor1 = PSFeedDescriptor.builder()
            .name("feed1")
            .site(TEST_SITE)
            .type("RSS2")
            .build();

        var descriptor2 = PSFeedDescriptor.builder()
            .name("feed2")
            .site(TEST_SITE)
            .type("ATOM")
            .build();

        feedDao.saveDescriptors(List.of(descriptor1, descriptor2));

        // Find feeds
        var feeds = feedDao.findBySite(TEST_SITE);

        assertAll(
            () -> assertEquals(2, feeds.size()),
            () -> assertTrue(feeds.stream()
                .map(d -> d.getName().orElse(""))
                .anyMatch("feed1"::equals)),
            () -> assertTrue(feeds.stream()
                .map(d -> d.getName().orElse(""))
                .anyMatch("feed2"::equals))
        );
    }
}
