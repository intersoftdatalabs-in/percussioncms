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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.i18n;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Sanity tests that every source-tree TMX file has a well-formed {@code <header>} per the TMX 1.4
 * DTD: the {@code srclang} and {@code adminlang} attributes are present, and at least one {@code
 * <prop type="supportedlanguage">} child declares a non-empty locale. Guarded against the "No
 * supported language is specified in the header" error that fires when a build artifact is emitted
 * without those required attributes.
 */
public class PSTmxResourceBundleScanTest {

  @Test
  public void sourceTmxHeaders_haveRequiredTmx14Attributes() throws Exception {
    Path dir = Paths.get("src", "main", "resources", "i18n").toAbsolutePath();
    if (!Files.isDirectory(dir)) {
      // The test only runs against the source tree checked out alongside the module.
      return;
    }
    try (Stream<Path> stream = Files.list(dir)) {
      var files = stream.filter(p -> p.toString().endsWith(".tmx")).sorted().toList();
      assertTrue(files.size() > 0, "expected TMX files under " + dir);
      for (var f : files) {
        Document doc = parse(f);
        NodeList headers = doc.getElementsByTagName("header");
        assertTrue(headers.getLength() >= 1, "TMX file must declare a <header> element: " + f);
        Element header = (Element) headers.item(0);
        assertTrue(
            header.hasAttribute("srclang"),
            "TMX <header> is missing required srclang attribute: " + f);
        assertTrue(
            header.hasAttribute("adminlang"),
            "TMX <header> is missing required adminlang attribute: " + f);

        NodeList props = header.getElementsByTagName("prop");
        boolean hasSupportedLanguage = false;
        for (int i = 0; i < props.getLength(); i++) {
          Element p = (Element) props.item(i);
          if ("supportedlanguage".equals(p.getAttribute("type"))) {
            String value = p.getTextContent();
            if (value != null && !value.isBlank()) {
              hasSupportedLanguage = true;
              break;
            }
          }
        }
        assertTrue(
            hasSupportedLanguage,
            "TMX <header> must declare at least one <prop type=\"supportedlanguage\">: " + f);
        assertNotNull(doc);
      }
    }
  }

  /**
   * SPA TopNav / UserMenu keys under {@code perc.ui.dashboard.modern@} must ship with at least
   * {@code en-us} and base {@code hi} so Hindi (India) login does not fall back to English chrome
   * (GH-1841). Live username/role values are not TMX keys and stay out of scope.
   */
  @Test
  public void cmsUi_shellChromeKeys_haveEnUsAndHindi() throws Exception {
    Path cmsUi = Paths.get("src", "main", "resources", "i18n", "CmsUi.tmx").toAbsolutePath();
    if (!Files.isRegularFile(cmsUi)) {
      return;
    }

    Set<String> required =
        Set.of(
            "perc.ui.dashboard.modern@Administration",
            "perc.ui.dashboard.modern@Admin tools",
            "perc.ui.dashboard.modern@Explorer",
            "perc.ui.dashboard.modern@Developer",
            "perc.ui.dashboard.modern@Main",
            "perc.ui.dashboard.modern@Dashboard gadgets on Home",
            "perc.ui.dashboard.modern@CMS design tools (content types, templates, ...)",
            "perc.ui.dashboard.modern@Signed in as",
            "perc.ui.dashboard.modern@user");

    Document doc = parse(cmsUi);
    NodeList tus = doc.getElementsByTagName("tu");
    Set<String> found = new LinkedHashSet<>();
    for (int i = 0; i < tus.getLength(); i++) {
      Element tu = (Element) tus.item(i);
      String tuid = tu.getAttribute("tuid");
      if (!required.contains(tuid)) {
        continue;
      }
      found.add(tuid);
      Set<String> langs = new LinkedHashSet<>();
      String enSeg = null;
      String hiSeg = null;
      NodeList tuvs = tu.getElementsByTagName("tuv");
      for (int j = 0; j < tuvs.getLength(); j++) {
        Element tuv = (Element) tuvs.item(j);
        String lang = normalizeLangAttr(readXmlLang(tuv));
        if (lang.isEmpty()) {
          continue;
        }
        langs.add(lang);
        NodeList segs = tuv.getElementsByTagName("seg");
        if (segs.getLength() == 0) {
          continue;
        }
        String seg = segs.item(0).getTextContent();
        if ("en-us".equals(lang)) {
          enSeg = seg;
        } else if ("hi".equals(lang)) {
          hiSeg = seg;
        }
      }
      assertTrue(langs.contains("en-us"), tuid + " missing en-us");
      assertTrue(langs.contains("hi"), tuid + " missing hi (base for hi-in)");
      assertNotNull(enSeg, tuid + " en-us seg null");
      assertFalse(enSeg.isBlank(), tuid + " en-us seg blank");
      assertNotNull(hiSeg, tuid + " hi seg null");
      assertFalse(hiSeg.isBlank(), tuid + " hi seg blank");
      assertFalse(
          enSeg.equals(hiSeg),
          tuid + " hi must differ from en-us for localized shell chrome, got: " + hiSeg);
    }
    assertTrue(
        found.containsAll(required),
        "CmsUi.tmx missing shell chrome TUs: "
            + required.stream().filter(k -> !found.contains(k)).toList());
  }

  private static String readXmlLang(Element tuv) {
    String lang = tuv.getAttributeNS("http://www.w3.org/XML/1998/namespace", "lang");
    if (lang == null || lang.isBlank()) {
      lang = tuv.getAttribute("xml:lang");
    }
    if (lang == null || lang.isBlank()) {
      lang = tuv.getAttribute("lang");
    }
    return lang;
  }

  private static String normalizeLangAttr(String raw) {
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
    return factory.newDocumentBuilder().parse(is);
  }
}
