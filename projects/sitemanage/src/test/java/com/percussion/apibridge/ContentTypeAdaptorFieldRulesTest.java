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

package com.percussion.apibridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.percussion.design.objectstore.PSExtensionCallSet;
import com.percussion.design.objectstore.PSField;
import com.percussion.design.objectstore.PSFieldTranslation;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Behavioral unit tests for ContentTypeAdaptor field-rule mapping helpers (P0.2c). */
@Tag("UnitTest")
public class ContentTypeAdaptorFieldRulesTest {

  @Test
  public void mapOccurrenceCoversAllKnownDimensions() {
    assertEquals(
        "optional", ContentTypeAdaptor.mapOccurrence(PSField.OCCURRENCE_DIMENSION_OPTIONAL));
    assertEquals(
        "required", ContentTypeAdaptor.mapOccurrence(PSField.OCCURRENCE_DIMENSION_REQUIRED));
    assertEquals(
        "oneOrMore", ContentTypeAdaptor.mapOccurrence(PSField.OCCURRENCE_DIMENSION_ONE_OR_MORE));
    assertEquals(
        "zeroOrMore", ContentTypeAdaptor.mapOccurrence(PSField.OCCURRENCE_DIMENSION_ZERO_OR_MORE));
    assertEquals("count", ContentTypeAdaptor.mapOccurrence(PSField.OCCURRENCE_DIMENSION_COUNT));
    assertEquals("unknown", ContentTypeAdaptor.mapOccurrence(-1));
    assertEquals("unknown", ContentTypeAdaptor.mapOccurrence(99));
  }

  @Test
  public void hasTranslationNullSafe() {
    assertFalse(ContentTypeAdaptor.hasTranslation(null));

    PSFieldTranslation empty = mock(PSFieldTranslation.class);
    when(empty.getTranslations()).thenReturn(null);
    assertFalse(ContentTypeAdaptor.hasTranslation(empty));

    PSFieldTranslation emptySet = mock(PSFieldTranslation.class);
    when(emptySet.getTranslations()).thenReturn(new PSExtensionCallSet());
    assertFalse(ContentTypeAdaptor.hasTranslation(emptySet));

    PSExtensionCallSet calls = mock(PSExtensionCallSet.class);
    when(calls.isEmpty()).thenReturn(false);
    PSFieldTranslation present = mock(PSFieldTranslation.class);
    when(present.getTranslations()).thenReturn(calls);
    assertTrue(ContentTypeAdaptor.hasTranslation(present));
  }
}
