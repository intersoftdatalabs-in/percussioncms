// REFACTORED: CP-JAVA11
package com.percussion.delivery.feeds.data;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PSFeedDTO using JUnit 5.
 */
class PSFeedDTOTest {
    private static final String TEST_URL = "https://test.percussion.com/feeds";
    private static final String TEST_HOST = "test.percussion.com";

    @Test
    @DisplayName("Builder should create valid DTO")
    void builderShouldCreateValidDTO() {
        var dto = PSFeedDTO.builder()
            .feedsUrl(TEST_URL)
            .hostName(TEST_HOST)
            .build();

        assertAll(
            () -> assertEquals(TEST_URL, dto.getFeedsUrl()),
            () -> assertEquals(TEST_HOST, dto.getHostName())
        );
    }

    @Test
    @DisplayName("Optional getters should handle null values")
    void optionalGettersShouldHandleNullValues() {
        var dto = new PSFeedDTO(null, null);

        assertAll(
            () -> assertTrue(dto.getFeedsUrlOptional().isEmpty()),
            () -> assertTrue(dto.getHostNameOptional().isEmpty())
        );
    }

    @Test
    @DisplayName("Equals and hashCode should work correctly")
    void equalsAndHashCodeShouldWorkCorrectly() {
        var dto1 = PSFeedDTO.builder()
            .feedsUrl(TEST_URL)
            .hostName(TEST_HOST)
            .build();

        var dto2 = PSFeedDTO.builder()
            .feedsUrl(TEST_URL)
            .hostName(TEST_HOST)
            .build();

        var different = PSFeedDTO.builder()
            .feedsUrl("different")
            .hostName("different")
            .build();

        assertAll(
            () -> assertEquals(dto1, dto2),
            () -> assertEquals(dto1.hashCode(), dto2.hashCode()),
            () -> assertNotEquals(dto1, different),
            () -> assertNotEquals(dto1.hashCode(), different.hashCode())
        );
    }
}
