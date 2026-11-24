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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Bare-{@code Type} noun-form pins for issue #7218.
 *
 * <p>Five pre-existing {@code CmsUi.tmx} units render the bare label {@code Type} with
 * verb-form machine translations (fr {@code Taper}, ar {@code يكتب}), while the new
 * delivery-panel unit {@code perc.ui.publish.modern@Type} (issue #7216) pins the noun forms
 * (fr {@code Type}, ar {@code النوع}, per Codex review). This test pins the five old units
 * to the same noun forms, plus two same-class corrections found during the per-locale review:
 * es {@code Escribe} -&gt; {@code Tipo} on {@code perc.ui.publish.title@Type} (verb sense;
 * the three sibling {@code Type} units and the #7216 unit all carry {@code Tipo}) and the
 * {@code perc.ui.dashboard@Type} uk colon casing ({@code тип:} -&gt; {@code Тип:}, matching
 * the #7216 {@code Тип} and every sibling {@code Тип}).
 *
 * <p>Locales deliberately left unchanged (named-reason defers, no precedent to copy): te
 * {@code టైప్ చేయండి} on the four bare units is byte-identical to the reviewed #7216 unit,
 * and dashboard te {@code రకం:} is already a noun form ({@code రకం} = kind); both need a
 * native-speaker call, not a machine guess.
 */
public class PSTypeNounFormsTmxTest {

  /** The five pre-existing bare-{@code Type} units (tuid -&gt; en-us source). */
  private static final Map<String, String> TYPE_SOURCES =
      Map.of(
          "perc.ui.publish.title@Type", "Type",
          "perc.ui.newSectionDialog.label@Type", "Type",
          "perc.ui.dashboard@Type", "Type:",
          "perc.ui.widget.builder@Type", "Type",
          "perc.ui.gadget.bulkUpload@Type", "Type");

  /** Expected noun-form segments per locale (tuid -&gt; seg). */
  private static final Map<String, Map<String, String>> EXPECTED_BY_LANG =
      Map.of(
          "fr",
              Map.of(
                  "perc.ui.publish.title@Type", "Type",
                  "perc.ui.newSectionDialog.label@Type", "Type",
                  "perc.ui.dashboard@Type", "Type:",
                  "perc.ui.widget.builder@Type", "Type",
                  "perc.ui.gadget.bulkUpload@Type", "Type"),
          "ar",
              Map.of(
                  "perc.ui.publish.title@Type", "النوع",
                  "perc.ui.newSectionDialog.label@Type", "النوع",
                  "perc.ui.dashboard@Type", "النوع:",
                  "perc.ui.widget.builder@Type", "النوع",
                  "perc.ui.gadget.bulkUpload@Type", "النوع"),
          "es",
              Map.of(
                  "perc.ui.publish.title@Type", "Tipo",
                  "perc.ui.newSectionDialog.label@Type", "Tipo",
                  "perc.ui.dashboard@Type", "Tipo:",
                  "perc.ui.widget.builder@Type", "Tipo",
                  "perc.ui.gadget.bulkUpload@Type", "Tipo"),
          "uk",
              Map.of(
                  "perc.ui.publish.title@Type", "Тип",
                  "perc.ui.newSectionDialog.label@Type", "Тип",
                  "perc.ui.dashboard@Type", "Тип:",
                  "perc.ui.widget.builder@Type", "Тип",
                  "perc.ui.gadget.bulkUpload@Type", "Тип"));

  /** Non-en locales every covered {@code <tu>} must keep (same ship matrix as #7115). */
  private static final List<String> REQUIRED_NON_EN_LOCALES =
      List.of(
          "ar", "bn", "de", "es", "fr", "he", "hi", "it", "ja-jp", "lou",
          "nb", "nl", "pl", "pt", "ru", "sv", "te", "tr", "uk", "zh-cn", "zh-tw");

  @Test
  public void typeKeys_carryNounForms() throws Exception {
    Map<String, Map<String, String>> segsByTuid = loadSegsByTuid();
    List<String> offenders = new ArrayList<>();
    for (Map.Entry<String, Map<String, String>> langEntry : EXPECTED_BY_LANG.entrySet()) {
      String lang = langEntry.getKey();
      for (Map.Entry<String, String> pin : langEntry.getValue().entrySet()) {
        Map<String, String> segs = segsByTuid.get(pin.getKey());
        if (segs == null) {
          offenders.add(pin.getKey() + " (missing <tu>)");
          continue;
        }
        String actual = segs.get(lang);
        if (!pin.getValue().equals(actual)) {
          offenders.add(pin.getKey() + " [" + lang + "] expected=" + pin.getValue()
              + " actual=" + actual);
        }
      }
    }
    assertTrue(offenders.isEmpty(),
        "CmsUi.tmx bare-Type units lack noun-form segs (#7218): " + offenders);
  }

  @Test
  public void typeKeys_haveNonBlankTranslationsForShipMatrix() throws Exception {
    Map<String, Map<String, String>> segsByTuid = loadSegsByTuid();
    List<String> offenders = new ArrayList<>();
    for (String tuid : TYPE_SOURCES.keySet()) {
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
    assertTrue(offenders.isEmpty(),
        "CmsUi.tmx bare-Type keys lack ship-matrix translations (#7218): " + offenders);
  }

  /**
   * Guards issue #7218: the checked-in {@code scripts/cache/i18n_translate.json} entries for
   * these bare-{@code Type} sources must agree with the shipped {@code CmsUi.tmx}
   * {@code <seg>} text (cache key is {@code sha256(target + "\\0" + source)}, same as {@code
   * i18n_translate.cache_key}). Missing keys are fine (uncached); disagreements fail so a
   * re-run can never write a stale cached segment back over reviewed TMX.
   */
  @Test
  public void typeKeysCache_agreesWithShippedTmx() throws Exception {
    Map<String, Map<String, String>> segsByTuid = loadSegsByTuid();
    Path cacheFile = resolveTranslateCache();
    assertNotNull(cacheFile, "i18n_translate.json not found");
    Map<String, String> cached = parseFlatJsonStringMap(cacheFile);

    List<String> offenders = new ArrayList<>();
    for (Map.Entry<String, String> e : TYPE_SOURCES.entrySet()) {
      Map<String, String> segs = segsByTuid.get(e.getKey());
      if (segs == null) {
        continue;
      }
      for (String lang : REQUIRED_NON_EN_LOCALES) {
        if ("lou".equals(lang)) {
          continue;
        }
        String seg = segs.get(lang);
        if (seg == null) {
          continue;
        }
        String cachedValue = cached.get(translateCacheKey(e.getValue(), lang));
        if (cachedValue != null && !cachedValue.equals(seg)) {
          offenders.add(
              e.getKey() + " [" + lang + "] cache=" + cachedValue + " tmx=" + seg);
        }
      }
    }
    assertTrue(offenders.isEmpty(),
        "i18n_translate.json cache disagrees with shipped CmsUi.tmx (#7218): " + offenders);
  }

  /** Mirror of {@code i18n_translate.cache_key}: {@code sha256(target + "\\0" + text)}. */
  private static String translateCacheKey(String text, String target) throws Exception {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    digest.update(target.getBytes(StandardCharsets.UTF_8));
    digest.update((byte) 0);
    digest.update(text.getBytes(StandardCharsets.UTF_8));
    return HexFormat.of().formatHex(digest.digest());
  }

  /** Covered tuids -&gt; (normalized lang -&gt; seg text). */
  private static Map<String, Map<String, String>> loadSegsByTuid() throws Exception {
    Path cmsUi = resolveCmsUiTmx();
    assertNotNull(cmsUi, "CmsUi.tmx not found");

    Map<String, Map<String, String>> segsByTuid = new LinkedHashMap<>();
    Document doc = parse(cmsUi);
    NodeList tus = doc.getElementsByTagName("tu");
    for (int i = 0; i < tus.getLength(); i++) {
      Element tu = (Element) tus.item(i);
      String tuid = tu.getAttribute("tuid");
      if (!TYPE_SOURCES.containsKey(tuid)) {
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
          throw new IllegalArgumentException("Bad escape in cache JSON");
      }
    }
  }

  /**
   * Resolve {@code CmsUi.tmx} portably: prefer the test classpath resource (Maven surefire), then
   * fall back to common source-tree locations relative to the process working directory.
   */
  private static Path resolveCmsUiTmx() throws URISyntaxException {
    URL url = PSTypeNounFormsTmxTest.class.getResource("/i18n/CmsUi.tmx");
    if (url != null && "file".equalsIgnoreCase(url.getProtocol())) {
      Path p = Paths.get(url.toURI());
      if (Files.isRegularFile(p)) {
        return p;
      }
    }
    Path[] candidates =
        new Path[] {
          Paths.get("src", "main", "resources", "i18n", "CmsUi.tmx"),
          Paths.get("modules", "perc-i18n", "src", "main", "resources", "i18n", "CmsUi.tmx"),
        };
    for (Path c : candidates) {
      Path abs = c.toAbsolutePath().normalize();
      if (Files.isRegularFile(abs)) {
        return abs;
      }
    }
    return null;
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
    return raw.trim().toLowerCase(Locale.ROOT).replace('_', '-');
  }

  private static Document parse(Path f) throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setIgnoringComments(true);
    factory.setIgnoringElementContentWhitespace(true);
    factory.setValidating(false);
    factory.setNamespaceAware(true);
    var is = new InputSource(f.toUri().toASCIIString());
    is.setEncoding("UTF-8");
    return factory.newDocumentBuilder().parse(is);
  }
}
