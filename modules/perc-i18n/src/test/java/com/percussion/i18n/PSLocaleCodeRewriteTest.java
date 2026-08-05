/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
package com.percussion.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Pure rewrite-map tests for GH-1547 locale migration. Cross-platform: no path I/O or OS-specific
 * asserts.
 */
@Tag("UnitTest")
class PSLocaleCodeRewriteTest {

  @Test
  void normalize_collapsesCaseAndUnderscore() {
    assertEquals("en-us", PSLocaleCodeRewrite.normalize("en_US"));
    assertEquals("en-us", PSLocaleCodeRewrite.normalize("EN-US"));
    assertEquals("es-es", PSLocaleCodeRewrite.normalize("es_ES"));
    assertEquals("ja-jp", PSLocaleCodeRewrite.normalize("ja-JP"));
    assertEquals("hi", PSLocaleCodeRewrite.normalize("HI"));
    assertNull(PSLocaleCodeRewrite.normalize(null));
    assertEquals("", PSLocaleCodeRewrite.normalize("  "));
  }

  @Test
  void rewriteSysLang_promotesHiOnly() {
    assertEquals("hi-in", PSLocaleCodeRewrite.rewriteSysLang("hi"));
    assertEquals("hi-in", PSLocaleCodeRewrite.rewriteSysLang("HI"));
    assertEquals("hi-in", PSLocaleCodeRewrite.rewriteSysLang("Hi"));
    // es is already a valid base locale for login — leave alone after normalize
    assertEquals("es", PSLocaleCodeRewrite.rewriteSysLang("es"));
    assertEquals("es", PSLocaleCodeRewrite.rewriteSysLang("ES"));
    assertEquals("en-us", PSLocaleCodeRewrite.rewriteSysLang("en_US"));
    assertEquals("es-es", PSLocaleCodeRewrite.rewriteSysLang("es_ES"));
    assertEquals("ja-jp", PSLocaleCodeRewrite.rewriteSysLang("ja-JP"));
  }

  @Test
  void rewriteSysLang_idempotentOnCanonical() {
    assertEquals("hi-in", PSLocaleCodeRewrite.rewriteSysLang("hi-in"));
    assertEquals("es", PSLocaleCodeRewrite.rewriteSysLang("es"));
    assertEquals("en-us", PSLocaleCodeRewrite.rewriteSysLang("en-us"));
    assertFalse(PSLocaleCodeRewrite.sysLangNeedsRewrite("hi-in"));
    assertFalse(PSLocaleCodeRewrite.sysLangNeedsRewrite("es"));
    assertFalse(PSLocaleCodeRewrite.sysLangNeedsRewrite("en-us"));
    assertTrue(PSLocaleCodeRewrite.sysLangNeedsRewrite("hi"));
    assertTrue(PSLocaleCodeRewrite.sysLangNeedsRewrite("en_US"));
  }

  @Test
  void rewriteContentLocale_promotesEsToEsEs() {
    assertEquals("es-es", PSLocaleCodeRewrite.rewriteContentLocale("es"));
    assertEquals("es-es", PSLocaleCodeRewrite.rewriteContentLocale("ES"));
    assertEquals("es-es", PSLocaleCodeRewrite.rewriteContentLocale("es_ES"));
    assertEquals("es-es", PSLocaleCodeRewrite.rewriteContentLocale("es-es"));
    // content path does not promote hi → hi-in (sys_lang only)
    assertEquals("hi", PSLocaleCodeRewrite.rewriteContentLocale("hi"));
    assertEquals("en-us", PSLocaleCodeRewrite.rewriteContentLocale("en_US"));
  }

  @Test
  void rewriteContentLocale_idempotentOnCanonical() {
    assertFalse(PSLocaleCodeRewrite.contentLocaleNeedsRewrite("es-es"));
    assertFalse(PSLocaleCodeRewrite.contentLocaleNeedsRewrite("en-us"));
    assertTrue(PSLocaleCodeRewrite.contentLocaleNeedsRewrite("es"));
    assertTrue(PSLocaleCodeRewrite.contentLocaleNeedsRewrite("en_US"));
    // second pass must be a no-op
    String once = PSLocaleCodeRewrite.rewriteContentLocale("es");
    assertEquals(once, PSLocaleCodeRewrite.rewriteContentLocale(once));
  }

  @Test
  void nullAndBlankInputsAreSafe() {
    assertNull(PSLocaleCodeRewrite.rewriteSysLang(null));
    assertNull(PSLocaleCodeRewrite.rewriteContentLocale(null));
    assertFalse(PSLocaleCodeRewrite.sysLangNeedsRewrite(null));
    assertFalse(PSLocaleCodeRewrite.contentLocaleNeedsRewrite(null));
    assertEquals("", PSLocaleCodeRewrite.rewriteSysLang(""));
    assertEquals("", PSLocaleCodeRewrite.rewriteContentLocale(""));
  }
}
