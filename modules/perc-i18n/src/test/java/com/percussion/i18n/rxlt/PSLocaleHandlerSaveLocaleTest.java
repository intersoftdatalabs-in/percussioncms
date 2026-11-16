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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.i18n.rxlt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.tablefactory.PSJdbcColumnData;
import com.percussion.tablefactory.PSJdbcRowData;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Behavioral tests for the pure helpers extracted from {@code PSLocaleHandler.saveLocale} (GH-6458
 * complexity debt slice). Cross-platform: no path I/O, no DB, no OS-specific asserts.
 */
@Tag("UnitTest")
class PSLocaleHandlerSaveLocaleTest {

  @Test
  void validate_rejectsNullOrBlankLanguageString() {
    assertThrows(
        IllegalArgumentException.class, () -> PSLocaleHandler.validateSaveLocaleArgs(null, "x"));
    assertThrows(
        IllegalArgumentException.class, () -> PSLocaleHandler.validateSaveLocaleArgs("", "x"));
    assertThrows(
        IllegalArgumentException.class, () -> PSLocaleHandler.validateSaveLocaleArgs("   ", "x"));
  }

  @Test
  void validate_rejectsNullOrBlankDisplayName() {
    assertThrows(
        IllegalArgumentException.class,
        () -> PSLocaleHandler.validateSaveLocaleArgs("en-us", null));
    assertThrows(
        IllegalArgumentException.class, () -> PSLocaleHandler.validateSaveLocaleArgs("en-us", ""));
  }

  @Test
  void isLanguageOnlyCode_flagsRegionSuffixes() {
    assertTrue(PSLocaleHandler.isLanguageOnlyCode("es"));
    assertTrue(PSLocaleHandler.isLanguageOnlyCode("hi"));
    assertFalse(PSLocaleHandler.isLanguageOnlyCode("en-us"));
    assertFalse(PSLocaleHandler.isLanguageOnlyCode("en_US"));
    assertFalse(PSLocaleHandler.isLanguageOnlyCode(null));
  }

  @Test
  void resolveIsBaseValue_preservesStoredFlagOnReplace() {
    assertEquals("1", PSLocaleHandler.resolveIsBaseValue(true, rowOf("ISBASE", "1"), "en-us"));
    assertEquals("0", PSLocaleHandler.resolveIsBaseValue(true, rowOf("ISBASE", "0"), "es"));
  }

  @Test
  void resolveIsBaseValue_fallsBackToNonBaseOnReplace() {
    assertEquals("0", PSLocaleHandler.resolveIsBaseValue(true, rowOf("ISBASE", ""), "en-us"));
    assertEquals("0", PSLocaleHandler.resolveIsBaseValue(true, rowOf("OTHER", "1"), "en-us"));
    assertEquals("0", PSLocaleHandler.resolveIsBaseValue(true, null, "en-us"));
  }

  @Test
  void resolveIsBaseValue_defaultsNewLanguageOnlyCodesToBase() {
    assertEquals("1", PSLocaleHandler.resolveIsBaseValue(false, null, "es"));
    assertEquals("0", PSLocaleHandler.resolveIsBaseValue(false, null, "en-us"));
    assertEquals("0", PSLocaleHandler.resolveIsBaseValue(false, null, "en_US"));
    assertEquals("0", PSLocaleHandler.resolveIsBaseValue(false, null, null));
  }

  @Test
  void resolveContentUsableValue_defaultsNewRowsToUsable() {
    assertEquals("1", PSLocaleHandler.resolveContentUsableValue(false, null));
  }

  @Test
  void resolveContentUsableValue_preservesStoredFlagOnReplace() {
    assertEquals(
        "0", PSLocaleHandler.resolveContentUsableValue(true, rowOf("CONTENTUSABLE", "0")));
    assertEquals(
        "1", PSLocaleHandler.resolveContentUsableValue(true, rowOf("CONTENTUSABLE", "1")));
  }

  @Test
  void resolveContentUsableValue_fallsBackToUsableOnReplace() {
    assertEquals("1", PSLocaleHandler.resolveContentUsableValue(true, rowOf("CONTENTUSABLE", "")));
    assertEquals("1", PSLocaleHandler.resolveContentUsableValue(true, rowOf("OTHER", "0")));
    assertEquals("1", PSLocaleHandler.resolveContentUsableValue(true, null));
  }

  private static PSJdbcRowData rowOf(String columnName, String value) {
    return new PSJdbcRowData(
        List.of(new PSJdbcColumnData(columnName, value)).iterator(),
        PSJdbcRowData.ACTION_REPLACE);
  }
}
