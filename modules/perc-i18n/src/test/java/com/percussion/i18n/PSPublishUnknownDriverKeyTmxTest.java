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
 * Unknown file driver i18n registration for issue #7106 (residual from #7076).
 *
 * <p>{@code FileDriverFields.tsx} renders {@code message(MSG.
 * PUBLISH_DELIVERY_DELIVERY_UNKNOWN_FILE_DRIVER, [driver])} for unrecognized
 * delivery drivers. The key ({@code perc.ui.publish.modern@Unknown file driver:
 * {0}}) had no {@code CmsUi.tmx} entry, so the double-fault path (unknown
 * driver + missing catalog entry) fell back to the text after {@code @} with
 * no arg substitution and showed a literal {@code {0}}. The WebUI fallback now
 * interpolates positional args, and this test pins the catalog side: exactly
 * one {@code <tu>} with an {@code en-us} segment matching the TypeScript
 * fallback text (same mechanics as {@code PSExplorerScheduleConfirmKeysTmxTest}).
 */
public class PSPublishUnknownDriverKeyTmxTest {

  /** Expected {@code perc.ui.publish.modern@...} tuid suffix (English). */
  private static final String EXPECTED_ENGLISH = "Unknown file driver: {0}";

  @Test
  public void unknownDriverKey_isRegisteredWithEnglish() throws Exception {
    Map<String, List<String>> enSegsByTuid = loadEnSegsByTuid();
    assertNotNull(enSegsByTuid, "CmsUi.tmx not found");
    String tuid = "perc.ui.publish.modern@" + EXPECTED_ENGLISH;
    List<String> segs = enSegsByTuid.get(tuid);
    assertTrue(
        segs != null && !segs.isEmpty(), "CmsUi.tmx missing unknown file driver key: " + tuid);
    assertTrue(
        segs.size() == 1, "CmsUi.tmx has duplicate unknown file driver keys: " + tuid);
    assertTrue(
        EXPECTED_ENGLISH.equals(segs.get(0)),
        "CmsUi.tmx en-us seg differs from TS fallback: " + tuid + " -> " + segs.get(0));
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
      if (!tuid.startsWith("perc.ui.publish.modern@")) {
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

  /**
   * Resolve {@code CmsUi.tmx} portably: prefer the test classpath resource (Maven surefire), then
   * fall back to common source-tree locations relative to the process working directory.
   */
  private static Path resolveCmsUiTmx() throws URISyntaxException {
    URL url = PSPublishUnknownDriverKeyTmxTest.class.getResource("/i18n/CmsUi.tmx");
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
