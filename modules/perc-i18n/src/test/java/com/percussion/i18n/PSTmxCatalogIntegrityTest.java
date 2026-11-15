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
 * Catalog-wide TMX integrity for the canonical {@code perc-i18n} bundles
 * ({@code CmsUi.tmx}, {@code SystemResources.tmx}, {@code DeveloperUi.tmx}).
 *
 * <p>Covers the endemic follow-up to issue #6172 (fixed by #6182 for 11 new Telugu TipTap
 * segments, then catalog-wide by issue #6173): translation-pipeline output embedded literal
 * ASCII {@code u200c} (a dropped-backslash ZWNJ escape) in Telugu segments. Nothing at
 * runtime unescapes it, so it renders as visible garbage. The audit for #6173 confirmed
 * 1,120 sites (705 {@code CmsUi} + 279 {@code SystemResources} + 136 {@code DeveloperUi}),
 * every one immediately after a virama (U+0C4D) before a consonant — i.e. a genuine ZWNJ
 * position preventing unwanted conjuncts — so each was replaced with real U+200C.
 *
 * <p>Also pins the #6173 duplicate-{@code tuid} repair: {@code
 * perc.ui.profile.modern@Saving…} was registered twice with identical content (runtime
 * resolution is last-wins, so the duplicate was order-fragile).
 *
 * <p>Sibling dropped-backslash artifacts ({@code u200b}, {@code u003d}, {@code u200e})
 * were intentionally out of scope there — they span many locales and needed per-site
 * adjudication (tracked in the #6173 follow-up residual, issue #6276). They are now
 * repaired by issue #6276 and guarded below:
 *
 * <ul>
 *   <li>{@code u003d} (243 sites): every site sat in an {@code =} position (HTML
 *       attribute assignments such as {@code hrefu003d}, or diagnostic {@code key=value}
 *       renderings such as {@code contentidu003d{0}}), verified against the
 *       {@code en-us} pivot of each affected TU — replaced with {@code =}.</li>
 *   <li>{@code u200b} (62 occurrences in 31 segments, always doubled): every run sat
 *       at a word boundary where a space already precedes it (Latin/Cyrillic/Hebrew)
 *       or inside CJK/katakana runs that take no space — i.e. a zero-width space was
 *       never genuine here — so the runs were deleted.</li>
 *   <li>{@code u200e} (1 site, Hebrew): genuine left-to-right-mark bidi anchor before
 *       {@code .xml} in right-to-left text — replaced with real U+200E.</li>
 * </ul>
 */
public class PSTmxCatalogIntegrityTest {

  /** Canonical TMX bundles covered by the catalog-wide guards. */
  private static final List<String> CATALOG_FILES =
      List.of("CmsUi.tmx", "SystemResources.tmx", "DeveloperUi.tmx");

  /**
   * Regression guard for issue #6173: no segment in any canonical bundle may contain
   * literal ASCII {@code u200c}. Real zero-width non-joiners must be stored as real
   * Unicode (U+200C).
   */
  @Test
  public void catalogSegments_haveNoAsciiU200cArtifacts() throws Exception {
    List<String> offenders = new ArrayList<>();
    for (String name : CATALOG_FILES) {
      Path f = resolveCatalogTmx(name);
      assertNotNull(f, name + " not found");
      Document doc = parse(f);
      NodeList tus = doc.getElementsByTagName("tu");
      for (int i = 0; i < tus.getLength(); i++) {
        Element tu = (Element) tus.item(i);
        String tuid = tu.getAttribute("tuid");
        NodeList tuvs = tu.getElementsByTagName("tuv");
        for (int j = 0; j < tuvs.getLength(); j++) {
          Element tuv = (Element) tuvs.item(j);
          String lang = normalizeLang(readXmlLang(tuv));
          NodeList segs = tuv.getElementsByTagName("seg");
          for (int k = 0; k < segs.getLength(); k++) {
            String seg = segs.item(k).getTextContent();
            if (seg != null && seg.contains("u200c")) {
              offenders.add(name + " :: " + tuid + " [" + lang + "]");
            }
          }
        }
      }
    }
    assertTrue(
        offenders.isEmpty(),
        "TMX catalog segments contain ASCII u200c escape artifacts: " + offenders);
  }

  /**
   * Regression guard for issue #6276: no segment in any canonical bundle may contain
   * literal ASCII {@code u200b}. The 62 audited occurrences were garbage at
   * word-boundary / CJK positions (never a genuine zero-width space) and were deleted.
   * Real zero-width spaces, if ever genuinely needed, must be stored as real
   * Unicode (U+200B).
   */
  @Test
  public void catalogSegments_haveNoAsciiU200bArtifacts() throws Exception {
    List<String> offenders = collectArtifactOffenders("u200b");
    assertTrue(
        offenders.isEmpty(),
        "TMX catalog segments contain ASCII u200b escape artifacts: " + offenders);
  }

  /**
   * Regression guard for issue #6276: no segment in any canonical bundle may contain
   * literal ASCII {@code u003d}. The 243 audited occurrences all sat in {@code =}
   * positions (verified against the {@code en-us} pivot) and were replaced with
   * {@code =}.
   */
  @Test
  public void catalogSegments_haveNoAsciiU003dArtifacts() throws Exception {
    List<String> offenders = collectArtifactOffenders("u003d");
    assertTrue(
        offenders.isEmpty(),
        "TMX catalog segments contain ASCII u003d escape artifacts: " + offenders);
  }

  /**
   * Regression guard for issue #6276: no segment in any canonical bundle may contain
   * literal ASCII {@code u200e}. The single audited occurrence (Hebrew bidi anchor
   * before {@code .xml}) was replaced with real U+200E.
   */
  @Test
  public void catalogSegments_haveNoAsciiU200eArtifacts() throws Exception {
    List<String> offenders = collectArtifactOffenders("u200e");
    assertTrue(
        offenders.isEmpty(),
        "TMX catalog segments contain ASCII u200e escape artifacts: " + offenders);
  }

  /**
   * Scan every {@code <seg>} in every canonical bundle for one exact ASCII escape
   * literal. The match is deliberately an exact literal, never a general
   * {@code u[0-9a-fA-F]{4}} pattern: the naive pattern false-positives on ordinary
   * words (e.g. Spanish "bloqueada", "categorías").
   */
  private static List<String> collectArtifactOffenders(String asciiEscape) throws Exception {
    List<String> offenders = new ArrayList<>();
    for (String name : CATALOG_FILES) {
      Path f = resolveCatalogTmx(name);
      assertNotNull(f, name + " not found");
      Document doc = parse(f);
      NodeList tus = doc.getElementsByTagName("tu");
      for (int i = 0; i < tus.getLength(); i++) {
        Element tu = (Element) tus.item(i);
        String tuid = tu.getAttribute("tuid");
        NodeList tuvs = tu.getElementsByTagName("tuv");
        for (int j = 0; j < tuvs.getLength(); j++) {
          Element tuv = (Element) tuvs.item(j);
          String lang = normalizeLang(readXmlLang(tuv));
          NodeList segs = tuv.getElementsByTagName("seg");
          for (int k = 0; k < segs.getLength(); k++) {
            String seg = segs.item(k).getTextContent();
            if (seg != null && seg.contains(asciiEscape)) {
              offenders.add(name + " :: " + tuid + " [" + lang + "]");
            }
          }
        }
      }
    }
    return offenders;
  }

  /**
   * Regression guard for issue #6173: every {@code <tu tuid="...">} is unique within its
   * file. Duplicate tuids resolve last-wins at runtime, so duplicates are order-fragile
   * and must not be re-introduced.
   */
  @Test
  public void catalogTuids_areUniqueWithinEachFile() throws Exception {
    List<String> duplicates = new ArrayList<>();
    for (String name : CATALOG_FILES) {
      Path f = resolveCatalogTmx(name);
      assertNotNull(f, name + " not found");
      Document doc = parse(f);
      Map<String, Integer> counts = new LinkedHashMap<>();
      NodeList tus = doc.getElementsByTagName("tu");
      for (int i = 0; i < tus.getLength(); i++) {
        String tuid = ((Element) tus.item(i)).getAttribute("tuid");
        counts.merge(tuid, 1, Integer::sum);
      }
      counts.forEach(
          (tuid, count) -> {
            if (count > 1) {
              duplicates.add(name + " :: " + tuid + " x" + count);
            }
          });
    }
    assertTrue(duplicates.isEmpty(), "TMX catalog has duplicate tuids: " + duplicates);
  }

  /**
   * Resolve a canonical bundle portably: prefer the test classpath resource (Maven
   * surefire), then fall back to common source-tree locations relative to the process
   * working directory.
   */
  private static Path resolveCatalogTmx(String name) throws URISyntaxException {
    URL url = PSTmxCatalogIntegrityTest.class.getResource("/i18n/" + name);
    if (url != null && "file".equalsIgnoreCase(url.getProtocol())) {
      Path p = Paths.get(url.toURI());
      if (Files.isRegularFile(p)) {
        return p;
      }
    }
    Path[] candidates =
        new Path[] {
          Paths.get("src", "main", "resources", "i18n", name),
          Paths.get("modules", "perc-i18n", "src", "main", "resources", "i18n", name),
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
