/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.services.datasource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PSCommunityDerbyDialect} to verify that the custom dialect correctly fixes the
 * FOR UPDATE locking clause duplication in the Hibernate 7.x community DerbyDialect.
 */
@DisplayName("PSCommunityDerbyDialect")
class PSCommunityDerbyDialectTest {

  @Nested
  @DisplayName("Lock string methods")
  class LockStringMethods {

    @Test
    @DisplayName("getForUpdateString should return 'for update with rs'")
    void getForUpdateString_shouldReturnForUpdateWithRs() {
      // Given
      var dialect = new PSCommunityDerbyDialect();

      // When
      var result = dialect.getForUpdateString();

      // Then
      assertNotNull(result, "FOR UPDATE string should not be null");
      assertTrue(result.contains("for update"), "Should contain 'for update'");
      assertTrue(result.contains("with rs"), "Should contain 'with rs'");
      assertFalse(result.contains("with rs with rs"), "Should NOT contain duplicate 'with rs'");
    }

    @Test
    @DisplayName("getWriteLockString should return 'for update with rs'")
    void getWriteLockString_shouldReturnForUpdateWithRs() {
      // Given
      var dialect = new PSCommunityDerbyDialect();

      // When
      var result = dialect.getWriteLockString(0);

      // Then
      assertNotNull(result, "Write lock string should not be null");
      assertEquals(
          " for update with rs", result, "Write lock string should be ' for update with rs'");
    }

    @Test
    @DisplayName("getReadLockString should return 'for read only with rs'")
    void getReadLockString_shouldReturnForReadOnlyWithRs() {
      // Given
      var dialect = new PSCommunityDerbyDialect();

      // When
      var result = dialect.getReadLockString(0);

      // Then
      assertNotNull(result, "Read lock string should not be null");
      assertEquals(
          " for read only with rs", result, "Read lock string should be ' for read only with rs'");
    }
  }

  @Nested
  @DisplayName("Dialect identity")
  class DialectIdentity {

    @Test
    @DisplayName("should extend DerbyDialect")
    void shouldExtendDerbyDialect() {
      // Given/When
      var dialect = new PSCommunityDerbyDialect();

      // Then
      assertTrue(
          dialect instanceof org.hibernate.community.dialect.DerbyDialect,
          "Should be an instance of DerbyDialect");
    }
  }
}
