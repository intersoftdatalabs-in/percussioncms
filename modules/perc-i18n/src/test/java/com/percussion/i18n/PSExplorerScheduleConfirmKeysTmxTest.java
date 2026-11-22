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
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Explorer Schedule / Clear-scheduled-dates native-confirm i18n registration for issue #7032.
 *
 * <p>The multi-schedule Playwright spec asserted {@code /every selected page and asset/i} while
 * the product confirm reads "every selected item" — the spec was realigned by #7026, and the
 * remaining gap was catalog-side: the three native-confirm strings ({@code CONFIRM_SCHEDULE},
 * {@code CONFIRM_SCHEDULE_MULTI}, {@code CONFIRM_CLEAR_SCHEDULE_MULTI} in
 * {@code WebUI/src/main/ts/contentExplorer/messages.ts}) had no {@code CmsUi.tmx} entries, so
 * non-English sessions silently rendered English fallback. This test pins that registration:
 * each key has exactly one {@code <tu>} with an {@code en-us} segment matching the TypeScript
 * fallback text (same mechanics as {@code PSTipTapEditorKeysTmxTest}).
 */
public class PSExplorerScheduleConfirmKeysTmxTest {

  /** Expected {@code perc.ui.explorer@...} tuid suffix (English) for every schedule confirm key. */
  private static final List<String> EXPECTED_ENGLISH = List.of(
      "Save schedule publish dates for this item?",
      "Save the same schedule publish dates for every selected item? Folders are skipped.",
      "Clear start and end publish dates on {count} selected items? Folders in the selection are not cleared.");

  @Test
  public void scheduleConfirmKeys_areRegisteredWithEnglish() throws Exception {
    assertKeysRegistered(loadEnSegsByTuid(), EXPECTED_ENGLISH);
  }

  private static Map<String, List<String>> loadEnSegsByTuid() throws Exception {
    Path cmsUi = resolveCmsUiTmx();
    assertNotNull(cmsUi, "CmsUi.tmx not found");

    Map<String, List<String>> enSegsByTuid = new LinkedHashMap<>();
    Document doc = parse(cmsUi);
    NodeList tus = doc.getElementsByTagName("tu");
    for (int i = 0; i < tus.getLength(); i++) {
      Element tu = (Element) tus.item(i);
      String tuid = tu.getAttribute("tuid");
      if (!tuid.startsWith("perc.ui.explorer@")) {
        continue;
      }
      NodeList tuvs = tu.getElementsByTagName("tuv");
      for (int j = 0; j < tuvs.getLength(); j++) {
        Element tuv = (Element) tuvs.item(j);
        if (!"en-us".equals(normalizeLang(readXmlLang(tuv)))) {
          continue;
        }
        NodeList segs = tuv.getElementsByTagName("seg");
        if (segs.getLength() > 0) {
          enSegsByTuid
              .computeIfAbsent(tuid, k -> new ArrayList<>())
              .add(segs.item(0).getTextContent());
        }
      }
    }
    return enSegsByTuid;
  }

  private static void assertKeysRegistered(
      Map<String, List<String>> enSegsByTuid, List<String> expected) {
    Set<String> missing = new LinkedHashSet<>();
    List<String> mismatched = new ArrayList<>();
    List<String> duplicated = new ArrayList<>();
    for (String en : expected) {
      String tuid = "perc.ui.explorer@" + en;
      List<String> segs = enSegsByTuid.get(tuid);
      if (segs == null || segs.isEmpty()) {
        missing.add(tuid);
      } else {
        if (segs.size() > 1) {
          duplicated.add(tuid);
        }
        if (!en.equals(segs.get(0))) {
          mismatched.add(tuid + " -> " + segs.get(0));
        }
      }
    }
    assertTrue(missing.isEmpty(), "CmsUi.tmx missing Explorer schedule confirm keys: " + missing);
    assertTrue(
        duplicated.isEmpty(), "CmsUi.tmx has duplicate Explorer schedule confirm keys: " + duplicated);
    assertTrue(
        mismatched.isEmpty(), "CmsUi.tmx en-us seg differs from TS fallback: " + mismatched);
  }

  /**
   * Resolve {@code CmsUi.tmx} portably: prefer the test classpath resource (Maven surefire), then
   * fall back to common source-tree locations relative to the process working directory.
   */
  private static Path resolveCmsUiTmx() throws URISyntaxException {
    URL url = PSExplorerScheduleConfirmKeysTmxTest.class.getResource("/i18n/CmsUi.tmx");
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
