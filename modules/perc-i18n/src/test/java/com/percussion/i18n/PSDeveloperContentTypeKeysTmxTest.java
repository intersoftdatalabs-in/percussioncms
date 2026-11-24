/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or otherwise agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.i18n;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Developer content-type chrome i18n registration for issues #7083 (CD-10 search indexing),
 * #7084 (CD-11 icon strategy), and #7114 (CD-01 hide-from-menu remainder), backfilled for
 * issue #7138 (parent #1690), plus the remaining icon-chrome strings for issue #7222
 * (parent #1690).
 *
 * <p>The content-type chrome slices landed their {@code DeveloperUi.tmx} translation units as
 * {@code en-us} only (per the #6894/#7005 precedent), so non-English sessions silently rendered
 * the English {@code message()} fallback. The CD-10 search-indexing TUs never merged with the
 * stranded #7083 branch, so this change also registers those three {@code en-us} units
 * (byte-identical to the {@code WebUI/src/main/ts/developer/messages.ts} suffixes) before
 * backfilling. Issue #7222 registers six more icon-chrome {@code en-us} units ({@code Source},
 * {@code Specified file}, {@code From file field}, the value placeholder, the value-required
 * notice, and the icon hint paragraph); {@code Value} and {@code None} reuse their pre-existing
 * catalog TUs. This test pins the catalog side: every key has an {@code en-us} segment matching
 * the TypeScript fallback text, and every key ships non-blank {@code <seg>} text for the full
 * 21-locale non-en ship matrix (floor + above-floor), matching the coverage of neighboring
 * translated keys.
 *
 * <p>{@code lou} (Kouri-Vini) has no Google Translate engine support, so its TUVs are offline
 * glossary drafts from {@code scripts/i18n_lou_draft.py} and are excluded from the
 * cache-agreement check below.
 */
public class PSDeveloperContentTypeKeysTmxTest {

  /** Expected {@code perc.ui.developer@...} tuid suffix (English) for every content-type key. */
  private static final List<String> EXPECTED_ENGLISH =
      List.of(
          "Search indexing",
          "Could not load search indexing. Default is on.",
          "Could not save search indexing.",
          "Icon strategy",
          "Could not load icon strategy.",
          "Could not save icon strategy.",
          "Hide from menu",
          "Source",
          "Value",
          "None",
          "Specified file",
          "From file field",
          "File path, name, or file field name",
          "value is required when source is not none",
          "Lock to edit. none clears the icon. specified is a file path or name. fromFileField is a file field name. Save writes while the lock is held and does not unlock. This chrome does not upload icon binaries.");

  /** Non-en locales every backfilled content-type {@code <tu>} must cover (issues #7138/#7222). */
  private static final List<String> REQUIRED_NON_EN_LOCALES =
      List.of(
          "ar", "bn", "de", "es", "fr", "he", "hi", "it", "ja-jp", "lou",
          "nb", "nl", "pl", "pt", "ru", "sv", "te", "tr", "uk", "zh-cn", "zh-tw");

  /**
   * Engine-backed locales (everything except {@code lou}) whose segments must agree with the
   * checked-in translate cache, so a re-run of the pipeline cannot silently regress a shipped
   * segment the way #7116 did for takedown sense.
   */
  private static final List<String> ENGINE_LOCALES =
      List.of(
          "ar", "bn", "de", "es", "fr", "he", "hi", "it", "ja-jp",
          "nb", "nl", "pl", "pt", "ru", "sv", "te", "tr", "uk",
          "zh-cn", "zh-tw");

  @Test
  public void contentTypeKeys_areRegisteredWithEnglish() throws Exception {
    assertKeysRegistered(loadSegsByTuid(), EXPECTED_ENGLISH);
  }

  @Test
  public void contentTypeKeys_haveNonBlankTranslationsForShipMatrix() throws Exception {
    Map<String, Map<String, String>> segsByTuid = loadSegsByTuid();
    List<String> offenders = new ArrayList<>();
    for (String en : EXPECTED_ENGLISH) {
      String tuid = "perc.ui.developer@" + en;
      Map<String, String> segs = segsByTuid.get(tuid);
      if (segs == null) {
        offenders.add(tuid + " (missing <tu>)");
        continue;
      }
      for (String lang : REQUIRED_NON_EN_LOCALES) {
        String seg = segs.get(lang);
        if (seg == null || seg.isBlank()) {
          offenders.add(tuid + " (missing/blank " + lang + ")");
        }
      }
    }
    assertTrue(
        offenders.isEmpty(),
        "DeveloperUi.tmx content-type keys lack ship-matrix translations (#7138/#7222): "
            + offenders);
  }

  /**
   * Guards issue #7116: {@code scripts/cache/i18n_translate.json} once kept stale segments that
   * a pipeline re-run would write back over corrected TMX. Every cache entry for these 15
   * content-type sources must agree with the shipped {@code DeveloperUi.tmx} {@code <seg>}
   * (cache key is {@code sha256(target + "\\0" + source)}, same as {@code
   * i18n_translate.cache_key}). Missing keys are fine (uncached); disagreements fail.
   */
  @Test
  public void contentTypeCache_agreesWithShippedTmx() throws Exception {
    Map<String, Map<String, String>> segsByTuid = loadSegsByTuid();
    Path cacheFile = resolveTranslateCache();
    assertNotNull(cacheFile, "i18n_translate.json not found");
    Map<String, String> cached = parseFlatJsonStringMap(cacheFile);

    List<String> offenders = new ArrayList<>();
    for (String en : EXPECTED_ENGLISH) {
      String tuid = "perc.ui.developer@" + en;
      Map<String, String> segs = segsByTuid.get(tuid);
      if (segs == null) {
        continue;
      }
      for (String lang : ENGINE_LOCALES) {
        String seg = segs.get(lang);
        if (seg == null) {
          continue;
        }
        String cachedValue = cached.get(translateCacheKey(en, lang));
        if (cachedValue != null && !cachedValue.equals(seg)) {
          offenders.add(
              tuid + " [" + lang + "] cache=" + cachedValue + " tmx=" + seg);
        }
      }
    }
    assertTrue(
        offenders.isEmpty(),
        "i18n_translate.json cache disagrees with shipped DeveloperUi.tmx (#7138/#7222): "
            + offenders);
  }

  /** Mirror of {@code i18n_translate.cache_key}: {@code sha256(target + "\\0" + text)}. */
  private static String translateCacheKey(String text, String target) throws Exception {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    digest.update(target.getBytes(StandardCharsets.UTF_8));
    digest.update((byte) 0);
    digest.update(text.getBytes(StandardCharsets.UTF_8));
    return HexFormat.of().formatHex(digest.digest());
  }

  /**
   * Resolve {@code scripts/cache/i18n_translate.json} portably: prefer the module-relative path
   * (Maven surefire runs with the module dir as CWD), then fall back to repo-root-relative.
   */
  private static Path resolveTranslateCache() {
    Path[] candidates =
        new Path[] {
          Paths.get("scripts", "cache", "i18n_translate.json"),
          Paths.get(
              "modules", "perc-i18n", "scripts", "cache", "i18n_translate.json"),
        };
    for (Path c : candidates) {
      Path abs = c.toAbsolutePath().normalize();
      if (Files.isRegularFile(abs)) {
        return abs;
      }
    }
    return null;
  }

  /**
   * Parse a flat JSON {@code {"key": "value", ...}} object. Only string keys/values are
   * supported; full JSON is not needed for the checked-in string cache.
   */
  private static Map<String, String> parseFlatJsonStringMap(Path json) throws Exception {
    String raw = Files.readString(json, StandardCharsets.UTF_8).trim();
    Map<String, String> map = new LinkedHashMap<>();
    int[] pos = {0};
    skipWs(raw, pos);
    expect(raw, pos, '{');
    skipWs(raw, pos);
    if (peek(raw, pos) == '}') {
      return map;
    }
    while (true) {
      skipWs(raw, pos);
      String key = readJsonString(raw, pos);
      skipWs(raw, pos);
      expect(raw, pos, ':');
      skipWs(raw, pos);
      String value = readJsonString(raw, pos);
      map.put(key, value);
      skipWs(raw, pos);
      char c = peek(raw, pos);
      if (c == ',') {
        pos[0]++;
        continue;
      }
      if (c == '}') {
        break;
      }
      throw new IllegalArgumentException("Unexpected char '" + c + "' in cache JSON");
    }
    return map;
  }

  private static void skipWs(String raw, int[] pos) {
    while (pos[0] < raw.length()
        && (raw.charAt(pos[0]) == ' '
            || raw.charAt(pos[0]) == '\n'
            || raw.charAt(pos[0]) == '\r'
            || raw.charAt(pos[0]) == '\t')) {
      pos[0]++;
    }
  }

  private static char peek(String raw, int[] pos) {
    if (pos[0] >= raw.length()) {
      throw new IllegalArgumentException("Truncated cache JSON");
    }
    return raw.charAt(pos[0]);
  }

  private static void expect(String raw, int[] pos, char want) {
    if (peek(raw, pos) != want) {
      throw new IllegalArgumentException("Expected '" + want + "' in cache JSON");
    }
    pos[0]++;
  }

  private static String readJsonString(String raw, int[] pos) {
    expect(raw, pos, '"');
    StringBuilder sb = new StringBuilder();
    while (true) {
      char c = peek(raw, pos);
      if (c == '"') {
        pos[0]++;
        return sb.toString();
      }
      if (c != '\\') {
        sb.append(c);
        pos[0]++;
        continue;
      }
      pos[0]++;
      char e = peek(raw, pos);
      pos[0]++;
      switch (e) {
        case '"':
          sb.append('"');
          break;
        case '\\':
          sb.append('\\');
          break;
        case '/':
          sb.append('/');
          break;
        case 'b':
          sb.append('\b');
          break;
        case 'f':
          sb.append('\f');
          break;
        case 'n':
          sb.append('\n');
          break;
        case 'r':
          sb.append('\r');
          break;
        case 't':
          sb.append('\t');
          break;
        case 'u':
          if (pos[0] + 4 > raw.length()) {
            throw new IllegalArgumentException("Truncated unicode escape in cache JSON");
          }
          sb.append((char) Integer.parseInt(raw.substring(pos[0], pos[0] + 4), 16));
          pos[0] += 4;
          break;
        default:
          throw new IllegalArgumentException("Bad escape '\\" + e + "' in cache JSON"); // mkd-code-review[paths.hardcoded_sep]: adjudicated intentional (issue #7138, same as #7116) — backslash is a JSON-escape in the test helper message, not an OS path; real paths use Paths.get joins
      }
    }
  }

  private static Map<String, Map<String, String>> loadSegsByTuid() throws Exception {
    Path tmx = resolveDeveloperUiTmx();
    assertNotNull(tmx, "DeveloperUi.tmx not found");

    Map<String, Map<String, String>> segsByTuid = new LinkedHashMap<>();
    Document doc = parse(tmx);
    NodeList tus = doc.getElementsByTagName("tu");
    for (int i = 0; i < tus.getLength(); i++) {
      Element tu = (Element) tus.item(i);
      String tuid = tu.getAttribute("tuid");
      if (!tuid.startsWith("perc.ui.developer@")) {
        continue;
      }
      Map<String, String> segs =
          segsByTuid.computeIfAbsent(tuid, k -> new LinkedHashMap<>());
      NodeList tuvs = tu.getElementsByTagName("tuv");
      for (int j = 0; j < tuvs.getLength(); j++) {
        Element tuv = (Element) tuvs.item(j);
        String lang = normalizeLang(readXmlLang(tuv));
        if (lang.isBlank()) {
          continue;
        }
        NodeList segNodes = tuv.getElementsByTagName("seg");
        if (segNodes.getLength() > 0) {
          segs.putIfAbsent(lang, segNodes.item(0).getTextContent());
        }
      }
    }
    return segsByTuid;
  }

  private static void assertKeysRegistered(
      Map<String, Map<String, String>> segsByTuid, List<String> expected) {
    Set<String> missing = new LinkedHashSet<>();
    List<String> mismatched = new ArrayList<>();
    for (String en : expected) {
      String tuid = "perc.ui.developer@" + en;
      Map<String, String> segs = segsByTuid.get(tuid);
      if (segs == null || !segs.containsKey("en-us")) {
        missing.add(tuid);
      } else if (!en.equals(segs.get("en-us"))) {
        mismatched.add(tuid + " -> " + segs.get("en-us"));
      }
    }
    assertTrue(missing.isEmpty(), "DeveloperUi.tmx missing content-type keys: " + missing);
    assertTrue(
        mismatched.isEmpty(), "DeveloperUi.tmx en-us seg differs from TS fallback: " + mismatched);
  }

  /**
   * Resolve {@code DeveloperUi.tmx} portably: prefer the test classpath resource (Maven
   * surefire), then fall back to common source-tree locations relative to the process working
   * directory.
   */
  private static Path resolveDeveloperUiTmx() throws URISyntaxException {
    URL url = PSDeveloperContentTypeKeysTmxTest.class.getResource("/i18n/DeveloperUi.tmx");
    if (url != null && "file".equalsIgnoreCase(url.getProtocol())) {
      Path p = Paths.get(url.toURI());
      if (Files.isRegularFile(p)) {
        return p;
      }
    }
    Path[] candidates =
        new Path[] {
          Paths.get("src", "main", "resources", "i18n", "DeveloperUi.tmx"),
          Paths.get("modules", "perc-i18n", "src", "main", "resources", "i18n", "DeveloperUi.tmx"),
        };
    for (Path c : candidates) {
      Path abs = c.toAbsolutePath().normalize();
      if (Files.isRegularFile(abs)) {
        return abs;
      }
    }
    return null;
  }

  private static Document parse(Path tmx) throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    try (var reader = Files.newBufferedReader(tmx)) {
      return factory.newDocumentBuilder().parse(new InputSource(reader));
    }
  }

  private static String readXmlLang(Element tuv) {
    String lang = tuv.getAttributeNS("http://www.w3.org/XML/1998/namespace", "lang");
    if (lang == null || lang.isBlank()) {
      lang = tuv.getAttribute("xml:lang");
    }
    if (lang == null || lang.isBlank()) {
      lang = tuv.getAttribute("lang");
    }
    return lang != null ? lang : "";
  }

  private static String normalizeLang(String raw) {
    if (raw == null || raw.isBlank()) {
      return "";
    }
    return raw.trim().toLowerCase(java.util.Locale.ROOT).replace('_', '-');
  }
}
