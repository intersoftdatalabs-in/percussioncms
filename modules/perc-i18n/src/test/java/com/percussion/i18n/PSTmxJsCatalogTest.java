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
package com.percussion.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.percussion.security.validation.XSSValidation;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

/**
 * Regression for GH-1611: {@code tmx.jsp?mode=js&prefix=perc.ui.&sys_lang=hi-in} must build a
 * complete accepted-key map and escape every key/value without throwing (HTTP 500). Mirrors the JSP
 * catalog path via {@link PSTmxJsCatalog} and exercises product TMX from the source tree.
 */
public class PSTmxJsCatalogTest {

  private static final String PERC_UI_PREFIX = "perc.ui.";

  @BeforeEach
  void reset() {
    PSTmxResourceBundle.getInstance().flushCacheForTest();
  }

  @AfterEach
  void teardown() {
    PSTmxResourceBundle.getInstance().flushCacheForTest();
  }

  @Test
  public void accept_nullSafe() {
    assertFalse(PSTmxJsCatalog.accept(null, "perc.ui.x"));
    assertFalse(PSTmxJsCatalog.accept(new String[] {"perc.ui."}, null));
    assertFalse(PSTmxJsCatalog.accept(new String[] {null, "perc.ui."}, null));
    assertTrue(PSTmxJsCatalog.accept(new String[] {"perc.ui."}, "perc.ui.home@Title"));
    assertFalse(PSTmxJsCatalog.accept(new String[] {"perc.ui."}, "psx.ce.label@X"));
    assertTrue(PSTmxJsCatalog.accept(new String[] {"a.", "b."}, "b.z"));
  }

  @Test
  public void collectAccepted_nullGetKeysYieldsEmptyMap() {
    // Fresh cache with no language buckets → getKeys returns null.
    PSTmxResourceBundle bundle = PSTmxResourceBundle.getInstance();
    assertTrue(bundle.getResourceBundlesForTest().isEmpty());
    Iterator<String> keys = bundle.getKeys("hi-in");
    // May be null when no maps exist at all.
    Map<String, String> accepted = PSTmxJsCatalog.collectAccepted(bundle, "hi-in", PERC_UI_PREFIX);
    assertNotNull(accepted);
    assertTrue(accepted.isEmpty());
    // keys was null or empty — either way, no throw and empty catalog.
    if (keys != null) {
      assertFalse(keys.hasNext());
    }
  }

  @Test
  public void collectAccepted_emptyValuesAndSyntheticSparseHiIn() throws Exception {
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
                + "<tuv xml:lang=\"hi\"><seg>प्रवेश</seg></tuv>"
                + "</tu>"
                + "<tu tuid=\"perc.ui.empty@Blank\">"
                + "<tuv xml:lang=\"en-us\"><seg></seg></tuv>"
                + "</tu>"
                + "<tu tuid=\"perc.ui.quote@Q\">"
                + "<tuv xml:lang=\"en-us\"><seg>Say \"hi\"</seg></tuv>"
                + "<tuv xml:lang=\"hi\"><seg>कहो \"नमस्ते\"</seg></tuv>"
                + "</tu>"
                + "<tu tuid=\"other.ns@Skip\">"
                + "<tuv xml:lang=\"en-us\"><seg>nope</seg></tuv>"
                + "</tu>"
                + "</body></tmx>");
    PSTmxResourceBundle.getInstance().addResourcesToCacheForTest(doc);

    Map<String, String> hiIn =
        PSTmxJsCatalog.collectAccepted(PSTmxResourceBundle.getInstance(), "hi-in", PERC_UI_PREFIX);
    assertTrue(hiIn.containsKey("perc.ui.login.modern@Sign in"));
    assertEquals("प्रवेश", hiIn.get("perc.ui.login.modern@Sign in"));
    // Empty <seg> is invalid → getString returns last sub-key ("Blank"), not null.
    // Catalog still includes the key with a non-null value (never aborts emit).
    assertTrue(hiIn.containsKey("perc.ui.empty@Blank"));
    assertNotNull(hiIn.get("perc.ui.empty@Blank"));
    assertEquals("Blank", hiIn.get("perc.ui.empty@Blank"));
    assertEquals("कहो \"नमस्ते\"", hiIn.get("perc.ui.quote@Q"));
    assertFalse(hiIn.containsKey("other.ns@Skip"));

    Map<String, String> hi =
        PSTmxJsCatalog.collectAccepted(PSTmxResourceBundle.getInstance(), "hi", PERC_UI_PREFIX);
    assertEquals("प्रवेश", hi.get("perc.ui.login.modern@Sign in"));

    // Escape every entry — must not throw; shape is "k": "v" pairs.
    String entries = PSTmxJsCatalog.toJsObjectEntries(hiIn);
    assertTrue(entries.contains("\"perc.ui.login.modern@Sign in\""));
    assertTrue(entries.contains("प्रवेश") || entries.contains("\\u"));
    assertTrue(entries.contains("\\\""), "literal quotes in values must be escaped");
    String json = PSTmxJsCatalog.toJsonDocument(hiIn);
    assertTrue(json.startsWith("{\"tmxmessages\": {"));
    assertTrue(json.endsWith("}}"));
  }

  @Test
  public void escapeJs_nullEmptyAndSpecials() {
    assertEquals("", PSTmxJsCatalog.escapeJs(null));
    assertEquals("", PSTmxJsCatalog.escapeJs(""));
    assertEquals("ok", PSTmxJsCatalog.escapeJs("ok"));
    String quoted = PSTmxJsCatalog.escapeJs("a\"b");
    assertTrue(quoted.contains("\\\"") || quoted.contains("\\u0022"));
    String newline = PSTmxJsCatalog.escapeJs("a\nb");
    assertTrue(newline.contains("\\n") || newline.contains("\\u000a"));
    // Devanagari passes through as Unicode (or escaped) without throw.
    assertNotNull(PSTmxJsCatalog.escapeJs("नमस्ते"));
    assertEquals(XSSValidation.escapeJavaScript("x"), PSTmxJsCatalog.escapeJs("x"));
  }

  @Test
  public void fallbackEscape_handlesControlsAndQuotes() {
    assertEquals("a\\\"b", PSTmxJsCatalog.fallbackEscape("a\"b"));
    assertEquals("a\\nb", PSTmxJsCatalog.fallbackEscape("a\nb"));
    assertEquals("a\\\\b", PSTmxJsCatalog.fallbackEscape("a\\b"));
    // Full C0 range (not only n/r/t) must be escaped as JSON unicode escapes for validity.
    // Expected form is backslash + "u" + four hex digits (split so the source stays legal).
    assertEquals("a\\" + "u0000b", PSTmxJsCatalog.fallbackEscape("a" + (char) 0 + "b"));
    assertEquals("a\\" + "u0008b", PSTmxJsCatalog.fallbackEscape("a" + '\b' + "b"));
    assertEquals("a\\" + "u000cb", PSTmxJsCatalog.fallbackEscape("a" + '\f' + "b"));
    assertEquals("a\\" + "u001fb", PSTmxJsCatalog.fallbackEscape("a" + (char) 0x1f + "b"));
    assertEquals("a\\" + "u2028b", PSTmxJsCatalog.fallbackEscape("a" + '\u2028' + "b"));
  }

  /**
   * Product TMX regression: load every source-tree {@code *.tmx}, then for {@code hi-in} and {@code
   * hi} build the same accepted map as {@code tmx.jsp} ({@code prefix=perc.ui.}), escape every
   * key/value, and assert stable JS/JSON object shape without throw (GH-1611).
   */
  @Test
  public void productTmx_hiInAndHi_percUiCatalogEscapesWithoutThrow() throws Exception {
    Path dir = Paths.get("src", "main", "resources", "i18n").toAbsolutePath().normalize();
    if (!Files.isDirectory(dir)) {
      // Source tree not present (e.g. jar-only test run) — skip without failing CI packaging.
      return;
    }
    PSTmxResourceBundle bundle = PSTmxResourceBundle.getInstance();
    try (Stream<Path> stream = Files.list(dir)) {
      var files = stream.filter(p -> p.toString().endsWith(".tmx")).sorted().toList();
      assertFalse(files.isEmpty(), "expected product TMX under " + dir);
      for (Path f : files) {
        Document doc = parseFile(f);
        bundle.addResourcesToCacheForTest(doc);
      }
    }

    Map<String, Map<String, PSTmxUnit>> buckets = bundle.getResourceBundlesForTest();
    assertTrue(
        buckets.containsKey("hi") || buckets.containsKey("hi-in") || buckets.containsKey("en-us"));

    for (String lang : new String[] {"hi-in", "hi", "en-us"}) {
      Map<String, String> accepted = PSTmxJsCatalog.collectAccepted(bundle, lang, PERC_UI_PREFIX);
      assertNotNull(accepted, "accepted map for " + lang);
      // hi-in / hi resolve via language-only/default chain (sparse regional); en-us has full pack.
      assertFalse(
          accepted.isEmpty(), "expected perc.ui. keys for " + lang + " after loading product TMX");

      for (Map.Entry<String, String> e : accepted.entrySet()) {
        assertNotNull(e.getKey(), "null key in accepted for " + lang);
        assertNotNull(e.getValue(), "null value for key " + e.getKey());
        try {
          String safeKey = PSTmxJsCatalog.escapeJs(e.getKey());
          String safeVal = PSTmxJsCatalog.escapeJs(e.getValue());
          assertNotNull(safeKey);
          assertNotNull(safeVal);
          // Direct XSSValidation path used by older JSP (must also not throw).
          XSSValidation.escapeJavaScript(e.getKey());
          XSSValidation.escapeJavaScript(e.getValue());
        } catch (Throwable t) {
          fail(
              "escape threw for lang="
                  + lang
                  + " key="
                  + e.getKey()
                  + " valueSnippet="
                  + snippet(e.getValue())
                  + ": "
                  + t);
        }
      }

      String jsEntries = PSTmxJsCatalog.toJsObjectEntries(accepted);
      assertNotNull(jsEntries);
      // Every entry uses quoted keys; first key starts with quote.
      if (!accepted.isEmpty()) {
        assertTrue(jsEntries.startsWith("\""), "JS entries must start with quoted key");
        assertTrue(jsEntries.contains("\": \""), "JS entries must use \": \" separators");
      }

      String json = PSTmxJsCatalog.toJsonDocument(accepted);
      assertTrue(json.startsWith("{\"tmxmessages\": {"));
      assertTrue(json.endsWith("}}"));
      // escapeJs must neutralize raw newlines inside values (catalog stays one logical emit).
      assertFalse(json.contains("\r"), "JSON catalog must not embed raw CR for " + lang);
      // Unescaped LF would break JS string literals when this JSON is inlined; escaper uses \n.
      if (json.contains("\n")) {
        // Pretty-print is not used; any newline would be from a failed escape of a value.
        fail("JSON catalog must not embed raw LF for " + lang);
      }
    }
  }

  @Test
  public void toJsObjectEntries_skipsNullKeysAndOrdersStable() {
    Map<String, String> map = new LinkedHashMap<>();
    map.put("perc.ui.a@A", "1");
    map.put(null, "x");
    map.put("perc.ui.b@B", "2");
    String entries = PSTmxJsCatalog.toJsObjectEntries(map);
    assertEquals("\"perc.ui.a@A\": \"1\",\"perc.ui.b@B\": \"2\"", entries);
  }

  private static String snippet(String v) {
    if (v == null) {
      return "null";
    }
    return v.length() <= 80 ? v : v.substring(0, 80) + "...";
  }

  private static Document parse(String xml) throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    factory.setValidating(false);
    return factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
  }

  private static Document parseFile(Path f) throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setIgnoringComments(true);
    factory.setIgnoringElementContentWhitespace(true);
    factory.setValidating(false);
    factory.setNamespaceAware(true);
    return factory.newDocumentBuilder().parse(f.toFile());
  }
}
