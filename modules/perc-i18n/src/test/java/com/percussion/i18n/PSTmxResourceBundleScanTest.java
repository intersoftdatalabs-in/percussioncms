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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.i18n;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URISyntaxException;
import java.net.URL;
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
    Path dir = resolveI18nDir();
    if (dir == null) {
      // Source tree not available (e.g. dependency-only classpath) — nothing to scan.
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
    Path cmsUi = resolveCmsUiTmx();
    assertNotNull(cmsUi, "CmsUi.tmx not found (cwd=" + Paths.get("").toAbsolutePath() + ")");
    assertTrue(Files.isRegularFile(cmsUi), "CmsUi.tmx not a regular file: " + cmsUi);

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

  /**
   * Residual Home dashboard body / modal / description keys (GH-1852) under {@code
   * perc.ui.dashboard.modern@}, {@code welcome@}, and {@code activity@} must ship with at least
   * {@code en-us} and base {@code de} so non-English locales do not fall back to English for gadget
   * body chrome. Gadget <em>title</em> keys owned by open PR #1851 and shell chrome owned by
   * #1863/#1870 are intentionally out of this set.
   *
   * <p>Representative residual set (modal, config, welcome, activity, gadget desc, widget body) —
   * full backfill is larger; this set is a canary for the change class.
   */
  @Test
  public void cmsUi_dashboardModernBodyKeys_haveEnUsAndGerman() throws Exception {
    Path cmsUi = resolveCmsUiTmx();
    assertNotNull(cmsUi, "CmsUi.tmx not found (cwd=" + Paths.get("").toAbsolutePath() + ")");
    assertTrue(Files.isRegularFile(cmsUi), "CmsUi.tmx not a regular file: " + cmsUi);

    // Representative residual set: modal, config, welcome, activity, gadget desc, widget body.
    Set<String> required =
        Set.of(
            "perc.ui.dashboard.modern@Add",
            "perc.ui.dashboard.modern@Added",
            "perc.ui.dashboard.modern@Search gadgets...",
            "perc.ui.dashboard.modern@No gadgets found",
            "perc.ui.dashboard.modern@Other",
            "perc.ui.dashboard.modern@Loading gadgets",
            "perc.ui.dashboard.modern@Apply layout",
            "perc.ui.dashboard.modern@Select at least one gadget.",
            "perc.ui.dashboard.modern@Welcome message and dashboard introduction",
            "perc.ui.dashboard.modern@Content activity metrics by path and duration",
            "perc.ui.dashboard.modern@Loading comments",
            "perc.ui.dashboard.modern@No sites available.",
            "perc.ui.dashboard.modern@Loading effectiveness",
            "perc.ui.dashboard.modern@Google Analytics is not configured",
            "perc.ui.dashboard.modern@Not available in React Home",
            "perc.ui.dashboard.welcome@Good morning",
            "perc.ui.dashboard.welcome@Using Percussion CMS",
            "perc.ui.dashboard.welcome@Site Management",
            "perc.ui.dashboard.activity@Loading activity",
            "perc.ui.dashboard.activity@No activity for path",
            "perc.ui.dashboard.activity@Published",
            "perc.ui.dashboard.activity@Pending");

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
      String deSeg = null;
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
        } else if ("de".equals(lang)) {
          deSeg = seg;
        }
      }
      assertTrue(langs.contains("en-us"), "missing en-us for residual dashboard key: " + tuid);
      assertNotNull(enSeg, "empty en-us seg for: " + tuid);
      assertFalse(enSeg.isBlank(), "blank en-us seg for: " + tuid);
      assertTrue(langs.contains("de"), "missing de for residual dashboard key: " + tuid);
      assertNotNull(deSeg, "empty de seg for: " + tuid);
      assertFalse(deSeg.isBlank(), "blank de seg for: " + tuid);
    }
    Set<String> missing = new LinkedHashSet<>(required);
    missing.removeAll(found);
    assertTrue(missing.isEmpty(), "CmsUi.tmx missing residual dashboard keys: " + missing);
  }

  /**
   * Residual Spanish locale surface (GH-961): Finder root <em>display</em> labels, root icon
   * tooltips, and default Dashboard gadget titles/welcome body must ship with at least {@code
   * en-us} and base {@code es} so Spanish login does not fall back to English for those keys.
   *
   * <p>Finder path constants ({@code Sites}/{@code Assets}/…) remain English repository roots and
   * are intentionally not localized here; display-label keys under {@code perc.ui.finder.root@}
   * prepare wiring without changing path APIs. Live Playwright locale smoke is residual.
   */
  @Test
  public void cmsUi_spanishLocaleResidualKeys_haveEnUsAndSpanish() throws Exception {
    Path cmsUi = resolveCmsUiTmx();
    assertNotNull(cmsUi, "CmsUi.tmx not found (cwd=" + Paths.get("").toAbsolutePath() + ")");
    assertTrue(Files.isRegularFile(cmsUi), "CmsUi.tmx not a regular file: " + cmsUi);

    Set<String> required =
        Set.of(
            // Finder root display labels (paths stay English)
            "perc.ui.finder.root@Sites",
            "perc.ui.finder.root@Assets",
            "perc.ui.finder.root@Design",
            "perc.ui.finder.root@Search",
            "perc.ui.finder.root@Recycling",
            // Finder root icon tooltips
            "perc.ui.images@SiteIconTitle",
            "perc.ui.images@AssetLibraryIconTitle",
            "perc.ui.images@DesignIconTitle",
            "perc.ui.images@SearchIconTitle",
            "perc.ui.images@RecyclingIconTitle",
            // Default dashboard gadgets
            "perc.ui.gadgets.welcome@WELCOME",
            "perc.ui.gadgets.processmonitor@Process Monitor",
            "perc.ui.gadgets.processmonitor@ProcessMonitor",
            "perc.ui.gadgets.workflowStatus@PAGES BY STATUS",
            "perc.ui.gadgets.licenseMonitor@License Monitor",
            // Welcome body + catalog descriptions
            "perc.ui.dashboard.welcome@Good morning",
            "perc.ui.dashboard.welcome@Using Percussion CMS",
            "perc.ui.dashboard.welcome@Site Management",
            "perc.ui.dashboard.modern@Welcome message and dashboard introduction",
            "perc.ui.dashboard.modern@System process and monitoring status",
            "perc.ui.dashboard.modern@Pages grouped by workflow state");

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
      String esSeg = null;
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
        } else if ("es".equals(lang)) {
          esSeg = seg;
        }
      }
      assertTrue(langs.contains("en-us"), "missing en-us for GH-961 residual key: " + tuid);
      assertNotNull(enSeg, "empty en-us seg for: " + tuid);
      assertFalse(enSeg.isBlank(), "blank en-us seg for: " + tuid);
      assertTrue(langs.contains("es"), "missing es for GH-961 residual key: " + tuid);
      assertNotNull(esSeg, "empty es seg for: " + tuid);
      assertFalse(esSeg.isBlank(), "blank es seg for: " + tuid);
      assertFalse(
          enSeg.equals(esSeg),
          tuid + " es must differ from en-us for Spanish residual surface, got: " + esSeg);
    }
    Set<String> missing = new LinkedHashSet<>(required);
    missing.removeAll(found);
    assertTrue(missing.isEmpty(), "CmsUi.tmx missing GH-961 residual keys: " + missing);
  }

  /**
   * Resolve {@code CmsUi.tmx} portably: prefer the test classpath resource (Maven surefire), then
   * fall back to common source-tree locations relative to the process working directory.
   */
  private static Path resolveCmsUiTmx() throws URISyntaxException {
    URL url = PSTmxResourceBundleScanTest.class.getResource("/i18n/CmsUi.tmx");
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

  /** Resolve the source {@code i18n} directory for header scans, or null if unavailable. */
  private static Path resolveI18nDir() throws URISyntaxException {
    Path cmsUi = resolveCmsUiTmx();
    if (cmsUi != null) {
      Path parent = cmsUi.getParent();
      if (parent != null && Files.isDirectory(parent)) {
        return parent;
      }
    }
    Path dir = Paths.get("src", "main", "resources", "i18n").toAbsolutePath().normalize();
    return Files.isDirectory(dir) ? dir : null;
  }

  private static String readXmlLang(Element tuv) {
    // Namespace-aware parsers (see parse()) expose xml:lang via the XML NS URI first.
    String lang = tuv.getAttributeNS("http://www.w3.org/XML/1998/namespace", "lang");
    if (lang == null || lang.isBlank()) {
      lang = tuv.getAttribute("xml:lang");
    }
    if (lang == null || lang.isBlank()) {
      lang = tuv.getAttribute("lang");
    }
    return lang != null ? lang : "";
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
