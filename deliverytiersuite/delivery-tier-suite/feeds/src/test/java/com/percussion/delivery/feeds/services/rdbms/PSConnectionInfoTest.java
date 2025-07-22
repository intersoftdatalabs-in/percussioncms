// REFACTORED: CP-JAVA11
package com.percussion.delivery.feeds.services.rdbms;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PSConnectionInfo with focus on security handling.
 */
class PSConnectionInfoTest {
    private static final String TEST_URL = "https://test.percussion.com/api";
    private static final String TEST_USER = "testuser";
    private static final String TEST_PASS = "sensitive-data";

    @Test
    @DisplayName("Should handle credentials securely")
    void shouldHandleCredentialsSecurely() {
        var info = new PSConnectionInfo(TEST_URL, TEST_USER, TEST_PASS, true);

        assertAll(
            () -> assertEquals(TEST_URL, info.getUrl().orElse(null)),
            () -> assertEquals(TEST_USER, info.getUsername().orElse(null)),
            () -> assertFalse(info.toString().contains(TEST_PASS)),
            () -> assertTrue(info.isEncrypted())
        );
    }

    @Test
    @DisplayName("Should use safe toString implementation")
    void shouldUseSafeToStringImplementation() {
        var info = new PSConnectionInfo(TEST_URL, TEST_USER, TEST_PASS, true);
        var safeString = info.toSafeString();

        assertAll(
            () -> assertTrue(safeString.contains(TEST_URL)),
            () -> assertTrue(safeString.contains(TEST_USER)),
            () -> assertFalse(safeString.contains(TEST_PASS)),
            () -> assertTrue(safeString.contains("encrypted=true"))
        );
    }

    @Test
    @DisplayName("Should validate required URL")
    void shouldValidateRequiredUrl() {
        assertThrows(NullPointerException.class,
            () -> new PSConnectionInfo(null, TEST_USER, TEST_PASS, true));
    }

    @Test
    @DisplayName("Should handle optional fields")
    void shouldHandleOptionalFields() {
        var info = new PSConnectionInfo(TEST_URL, null, null, false);

        assertAll(
            () -> assertTrue(info.getUsername().isEmpty()),
            () -> assertTrue(info.getPassword().isEmpty()),
            () -> assertFalse(info.isEncrypted())
        );
    }
}
