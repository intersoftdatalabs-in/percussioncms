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
package com.percussion.services.pubserver.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

/**
 * {@code PSX_PUBSERVER.HAS_FULL_PUBLISHED} is CHAR(1). Site rename saves the row, so the flag
 * written must be one character or null (issue #4784).
 */
class PSPubServerHasFullPublishedFlagTest {

  @Test
  void setterStoresSingleCharacterFlag() {
    PSPubServer published = new PSPubServer();
    published.setHasFullPublished(true);
    assertEquals("y", published.storedHasFullPublished());
    assertTrue(published.hasFullPublished());

    PSPubServer unpublished = new PSPubServer();
    unpublished.setHasFullPublished(false);
    assertEquals("n", unpublished.storedHasFullPublished());
    assertFalse(unpublished.hasFullPublished());
    assertEquals(1, unpublished.storedHasFullPublished().length());
  }

  @Test
  void normalizeCoercesLegacyWordsAndKeepsNull() throws Exception {
    assertNull(PSPubServer.toHasFullPublishedColumn(null));
    assertNull(PSPubServer.toHasFullPublishedColumn("  "));

    PSPubServer legacyNo = new PSPubServer();
    store(legacyNo, "no");
    assertFalse(legacyNo.hasFullPublished());
    legacyNo.normalizeHasFullPublishedForColumn();
    assertEquals("n", legacyNo.storedHasFullPublished());

    PSPubServer legacyYes = new PSPubServer();
    store(legacyYes, "yes");
    assertTrue(legacyYes.hasFullPublished());
    legacyYes.normalizeHasFullPublishedForColumn();
    assertEquals("y", legacyYes.storedHasFullPublished());

    PSPubServer unset = new PSPubServer();
    store(unset, null);
    unset.normalizeHasFullPublishedForColumn();
    assertNull(unset.storedHasFullPublished());
    assertFalse(unset.hasFullPublished());
  }

  @Test
  void jdbcConverterWritesOneCharacterForLegacyWords() {
    PSHasFullPublishedColumnConverter converter = new PSHasFullPublishedColumnConverter();
    assertEquals("y", converter.convertToDatabaseColumn("yes"));
    assertEquals("n", converter.convertToDatabaseColumn("no"));
    assertEquals("y", converter.convertToDatabaseColumn("true"));
    assertEquals("n", converter.convertToDatabaseColumn("false"));
    assertNull(converter.convertToDatabaseColumn(null));
    assertEquals("y", converter.convertToEntityAttribute("yes"));
    assertEquals("n", converter.convertToEntityAttribute("no"));
  }

  private static void store(PSPubServer server, String raw) throws Exception {
    Field field = PSPubServer.class.getDeclaredField("hasFullPublished");
    field.setAccessible(true);
    field.set(server, raw);
  }
}
