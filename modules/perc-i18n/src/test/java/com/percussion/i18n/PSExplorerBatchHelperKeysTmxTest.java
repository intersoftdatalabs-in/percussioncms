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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
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
 * Explorer publish/takedown/stage/unstage batch-helper i18n registration for issue #7056,
 * backfilled for issue #7082.
 *
 * <p>The batch-helper failure fragments ({@code BATCH_MISSING_CONTENT_ID} et al in {@code
 * WebUI/src/main/ts/contentExplorer/messages.ts}) are lowercase sentence fragments keyed through
 * {@code perc.ui.explorer@...}. They are case-sensitively distinct from the capitalized confirm
 * keys ({@code Publish failed}, {@code Takedown failed}, {@code Not staged}) — lookups are
 * case-sensitive, so both forms coexist. This test pins both the registration (en-us seg matches
 * the TypeScript key suffix) and the #7082 backfill: every key ships non-blank {@code <seg>} text
 * for the full non-en ship matrix, matching the coverage of neighboring translated keys.
 */
public class PSExplorerBatchHelperKeysTmxTest {

  /** Expected {@code perc.ui.explorer@...} tuid suffix (English) for every batch-helper key. */
  private static final List<String> EXPECTED_ENGLISH =
      List.of(
          "missing content id",
          "not published",
          "publish failed",
          "not taken down",
          "takedown failed",
          "not staged",
          "not removed");

  /** Non-en locales every backfilled batch-helper {@code <tu>} must cover (issue #7082). */
  private static final List<String> REQUIRED_NON_EN_LOCALES =
      List.of(
          "ar", "bn", "de", "es", "fr", "he", "hi", "it", "ja-jp", "lou",
          "nb", "nl", "pl", "pt", "ru", "sv", "te", "tr", "uk", "zh-cn", "zh-tw");

  @Test
  public void batchHelperKeys_areRegisteredWithEnglish() throws Exception {
    assertKeysRegistered(loadSegsByTuid(), EXPECTED_ENGLISH);
  }

  @Test
  public void batchHelperKeys_haveNonBlankTranslationsForShipMatrix() throws Exception {
    Map<String, Map<String, String>> segsByTuid = loadSegsByTuid();
    List<String> offenders = new ArrayList<>();
    for (String en : EXPECTED_ENGLISH) {
      String tuid = "perc.ui.explorer@" + en;
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
        "CmsUi.tmx Explorer batch-helper keys lack ship-matrix translations (#7082): "
            + offenders);
  }

  private static Map<String, Map<String, String>> loadSegsByTuid() throws Exception {
    Path cmsUi = resolveCmsUiTmx();
    assertNotNull(cmsUi, "CmsUi.tmx not found");

    Map<String, Map<String, String>> segsByTuid = new LinkedHashMap<>();
    Document doc = parse(cmsUi);
    NodeList tus = doc.getElementsByTagName("tu");
    for (int i = 0; i < tus.getLength(); i++) {
      Element tu = (Element) tus.item(i);
      String tuid = tu.getAttribute("tuid");
      if (!tuid.startsWith("perc.ui.explorer@")) {
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
      String tuid = "perc.ui.explorer@" + en;
      Map<String, String> segs = segsByTuid.get(tuid);
      if (segs == null || !segs.containsKey("en-us")) {
        missing.add(tuid);
      } else if (!en.equals(segs.get("en-us"))) {
        mismatched.add(tuid + " -> " + segs.get("en-us"));
      }
    }
    assertTrue(missing.isEmpty(), "CmsUi.tmx missing Explorer batch-helper keys: " + missing);
    assertTrue(
        mismatched.isEmpty(), "CmsUi.tmx en-us seg differs from TS fallback: " + mismatched);
  }

  /**
   * Resolve {@code CmsUi.tmx} portably: prefer the test classpath resource (Maven surefire), then
   * fall back to common source-tree locations relative to the process working directory.
   */
  private static Path resolveCmsUiTmx() throws URISyntaxException {
    URL url = PSExplorerBatchHelperKeysTmxTest.class.getResource("/i18n/CmsUi.tmx");
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
