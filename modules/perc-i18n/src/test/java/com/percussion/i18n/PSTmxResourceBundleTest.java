/*
 * Copyright 1999-2025 Percussion Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package com.percussion.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

/**
 * Locale-tag normalization and cache-population tests for {@link PSTmxResourceBundle}. Drives the
 * package-private {@code addResourcesToCache} entry point directly with synthetic DOM documents so
 * the tests do not depend on filesystem state or a running server.
 */
public class PSTmxResourceBundleTest {

  @BeforeEach
  void reset() {
    PSTmxResourceBundle.getInstance().flushCacheForTest();
  }

  @AfterEach
  void teardown() {
    PSTmxResourceBundle.getInstance().flushCacheForTest();
  }

  /** Direct test of the static {@code normalizeLang} helper. */
  @Test
  public void normalizeLang_collapsesCaseAndUnderscore() {
    assertEquals("en-us", PSTmxResourceBundle.normalizeLang("en_US"));
    assertEquals("en-us", PSTmxResourceBundle.normalizeLang("EN-US"));
    assertEquals("en-us", PSTmxResourceBundle.normalizeLang("en-us"));
    assertEquals("de", PSTmxResourceBundle.normalizeLang("DE"));
    assertEquals("ja-jp", PSTmxResourceBundle.normalizeLang("ja-JP"));
    assertEquals("es-es", PSTmxResourceBundle.normalizeLang("es_ES"));
    assertNull(PSTmxResourceBundle.normalizeLang(null));
    assertEquals("", PSTmxResourceBundle.normalizeLang(""));
  }

  /**
   * A header that declares {@code supportedlanguage} values in mixed case + underscore form must
   * collapse to canonical BCP-47 keys.
   */
  @Test
  public void addResourcesToCache_normalizesHeaderLanguageKeys() throws Exception {
    Document doc =
        parse(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<tmx version=\"1.4\"><header>"
                + "<prop type=\"supportedlanguage\">EN_US</prop>"
                + "<prop type=\"supportedlanguage\">de</prop>"
                + "<prop type=\"supportedlanguage\">es_ES</prop>"
                + "</header><body>"
                + "<tu tuid=\"k@hello\"><tuv xml:lang=\"en-US\"><seg>Hello</seg></tuv></tu>"
                + "<tu tuid=\"k@world\"><tuv xml:lang=\"de\"><seg>Welt</seg></tuv></tu>"
                + "<tu tuid=\"k@hola\"><tuv xml:lang=\"es_ES\"><seg>Hola</seg></tuv></tu>"
                + "</body></tmx>");
    PSTmxResourceBundle.getInstance().addResourcesToCacheForTest(doc);

    Map<String, Map<String, PSTmxUnit>> bundles =
        PSTmxResourceBundle.getInstance().getResourceBundlesForTest();
    assertNotNull(bundles.get("en-us"), "en-us bucket must exist");
    assertNotNull(bundles.get("de"), "de bucket must exist");
    assertNotNull(bundles.get("es-es"), "es-es bucket must exist");
    assertNull(bundles.get("EN_US"), "non-normalized key must not exist");
    assertNull(bundles.get("es_ES"), "underscore key must not exist");

    assertEquals("Hello", bundles.get("en-us").get("k@hello").toString());
    assertEquals("Welt", bundles.get("de").get("k@world").toString());
    assertEquals("Hola", bundles.get("es-es").get("k@hola").toString());
  }

  /** Confirms a malformed / non-canonical header value is tolerated (normalized). */
  @Test
  public void addResourcesToCache_acceptsMalformedHeaderValue() throws Exception {
    Document doc =
        parse(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<tmx version=\"1.4\"><header>"
                + "<prop type=\"supportedlanguage\">EN_US</prop>"
                + "</header><body>"
                + "<tu tuid=\"k@1\"><tuv xml:lang=\"en_US\"><seg>Hi</seg></tuv></tu>"
                + "</body></tmx>");
    PSTmxResourceBundle.getInstance().addResourcesToCacheForTest(doc);
    Map<String, Map<String, PSTmxUnit>> bundles =
        PSTmxResourceBundle.getInstance().getResourceBundlesForTest();
    assertTrue(bundles.containsKey("en-us"), "normalized key en-us must exist");
    assertEquals("Hi", bundles.get("en-us").get("k@1").toString());
  }

  /** getString falls back to language-only then to default language. */
  @Test
  public void getString_fallsBackToLanguageOnlyThenDefault() throws Exception {
    Document doc =
        parse(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<tmx version=\"1.4\"><header>"
                + "<prop type=\"supportedlanguage\">en-us</prop>"
                + "<prop type=\"supportedlanguage\">es</prop>"
                + "</header><body>"
                + "<tu tuid=\"perc.ui.common.label@Ok\">"
                + "<tuv xml:lang=\"en-us\"><seg>Ok</seg></tuv>"
                + "<tuv xml:lang=\"es\"><seg>Ok-es</seg></tuv>"
                + "</tu>"
                + "</body></tmx>");
    PSTmxResourceBundle.getInstance().addResourcesToCacheForTest(doc);

    // Exact match
    assertEquals(
        "Ok", PSTmxResourceBundle.getInstance().getString("perc.ui.common.label@Ok", "en-us"));
    // Language-only fallback: en-gb -> en-us
    assertEquals(
        "Ok", PSTmxResourceBundle.getInstance().getString("perc.ui.common.label@Ok", "en-gb"));
    // Default fallback: ja-jp -> en-us
    assertEquals(
        "Ok", PSTmxResourceBundle.getInstance().getString("perc.ui.common.label@Ok", "ja-jp"));
    // Spanish exact match.
    assertEquals(
        "Ok-es", PSTmxResourceBundle.getInstance().getString("perc.ui.common.label@Ok", "es"));
  }

  /**
   * When a regional locale is registered (empty header bucket) but TUs only exist under the
   * language-only tag, getString must still resolve the language-only content (GH-1609 / hi-in vs
   * hi).
   */
  @Test
  public void getString_keyLevelFallbackFromSparseRegionalBucket() throws Exception {
    Document doc =
        parse(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<tmx version=\"1.4\"><header>"
                + "<prop type=\"supportedlanguage\">en-us</prop>"
                + "<prop type=\"supportedlanguage\">hi</prop>"
                + "<prop type=\"supportedlanguage\">hi-in</prop>"
                + "</header><body>"
                + "<tu tuid=\"perc.ui.login.modern@Sign in\">"
                + "<tuv xml:lang=\"en-us\"><seg>Sign in</seg></tuv>"
                + "<tuv xml:lang=\"hi\"><seg>sain-in-hi</seg></tuv>"
                + "</tu>"
                + "</body></tmx>");
    PSTmxResourceBundle.getInstance().addResourcesToCacheForTest(doc);

    Map<String, Map<String, PSTmxUnit>> bundles =
        PSTmxResourceBundle.getInstance().getResourceBundlesForTest();
    assertNotNull(bundles.get("hi-in"), "header must create sparse hi-in bucket");
    assertTrue(
        bundles.get("hi-in").isEmpty()
            || !bundles.get("hi-in").containsKey("perc.ui.login.modern@Sign in"),
        "hi-in must not own the key");

    assertEquals(
        "sain-in-hi",
        PSTmxResourceBundle.getInstance().getString("perc.ui.login.modern@Sign in", "hi-in"));
    assertEquals(
        "sain-in-hi",
        PSTmxResourceBundle.getInstance().getString("perc.ui.login.modern@Sign in", "hi"));
  }

  /**
   * getKeys for a regional tag must union keys from the language-only and default buckets so
   * tmx.jsp can emit a complete JS catalog (GH-1609).
   */
  @Test
  public void getKeys_unionsLookupChainForSparseRegional() throws Exception {
    Document doc =
        parse(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<tmx version=\"1.4\"><header>"
                + "<prop type=\"supportedlanguage\">en-us</prop>"
                + "<prop type=\"supportedlanguage\">hi</prop>"
                + "<prop type=\"supportedlanguage\">hi-in</prop>"
                + "</header><body>"
                + "<tu tuid=\"perc.ui.login.modern@Sign in\">"
                + "<tuv xml:lang=\"en-us\"><seg>Sign in</seg></tuv>"
                + "<tuv xml:lang=\"hi\"><seg>sain-in-hi</seg></tuv>"
                + "</tu>"
                + "<tu tuid=\"perc.ui.login.modern@Locale\">"
                + "<tuv xml:lang=\"en-us\"><seg>Locale</seg></tuv>"
                + "<tuv xml:lang=\"hi\"><seg>sthaan</seg></tuv>"
                + "</tu>"
                + "</body></tmx>");
    PSTmxResourceBundle.getInstance().addResourcesToCacheForTest(doc);

    java.util.Set<String> keys = new java.util.HashSet<>();
    java.util.Iterator<String> it = PSTmxResourceBundle.getInstance().getKeys("hi-in");
    assertNotNull(it);
    it.forEachRemaining(keys::add);
    assertTrue(keys.contains("perc.ui.login.modern@Sign in"));
    assertTrue(keys.contains("perc.ui.login.modern@Locale"));
  }

  /** Regional override wins over language-only when both define the key. */
  @Test
  public void getString_prefersExactRegionalOverLanguageOnly() throws Exception {
    Document doc =
        parse(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<tmx version=\"1.4\"><header>"
                + "<prop type=\"supportedlanguage\">en-us</prop>"
                + "<prop type=\"supportedlanguage\">es</prop>"
                + "<prop type=\"supportedlanguage\">es-mx</prop>"
                + "</header><body>"
                + "<tu tuid=\"k@hello\">"
                + "<tuv xml:lang=\"en-us\"><seg>Hello</seg></tuv>"
                + "<tuv xml:lang=\"es\"><seg>Hola</seg></tuv>"
                + "<tuv xml:lang=\"es-mx\"><seg>Hola-MX</seg></tuv>"
                + "</tu>"
                + "</body></tmx>");
    PSTmxResourceBundle.getInstance().addResourcesToCacheForTest(doc);

    assertEquals("Hola-MX", PSTmxResourceBundle.getInstance().getString("k@hello", "es-mx"));
    assertEquals("Hola", PSTmxResourceBundle.getInstance().getString("k@hello", "es"));
    // es-es has no bucket content; falls back to language-only es
    assertEquals("Hola", PSTmxResourceBundle.getInstance().getString("k@hello", "es-es"));
  }

  /** Arabic base locale: exact hit, then missing key falls back to en-us (product default). */
  @Test
  public void getString_arabicBaseAndDefaultFallback() throws Exception {
    Document doc =
        parse(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<tmx version=\"1.4\"><header>"
                + "<prop type=\"supportedlanguage\">ar</prop>"
                + "<prop type=\"supportedlanguage\">en-us</prop>"
                + "</header><body>"
                + "<tu tuid=\"perc.ui.login.modern@Sign in\">"
                + "<tuv xml:lang=\"en-us\"><seg>Sign in</seg></tuv>"
                + "<tuv xml:lang=\"ar\"><seg>تسجيل الدخول</seg></tuv>"
                + "</tu>"
                + "<tu tuid=\"k@only-en\">"
                + "<tuv xml:lang=\"en-us\"><seg>EnglishOnly</seg></tuv>"
                + "</tu>"
                + "</body></tmx>");
    PSTmxResourceBundle.getInstance().addResourcesToCacheForTest(doc);

    assertEquals(
        "تسجيل الدخول",
        PSTmxResourceBundle.getInstance().getString("perc.ui.login.modern@Sign in", "ar"));
    assertEquals("EnglishOnly", PSTmxResourceBundle.getInstance().getString("k@only-en", "ar"));
  }

  private static Document parse(String xml) throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    return factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
  }
}
