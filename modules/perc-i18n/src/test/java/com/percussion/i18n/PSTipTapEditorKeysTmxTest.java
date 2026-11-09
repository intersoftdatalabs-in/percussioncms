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
 * TipTap editor i18n registration for epic #5622 (slice #5732).
 *
 * <p>The React editor host resolves toolbar + dialog strings through {@code window.I18N} (the
 * {@code tmx.jsp?mode=js&prefix=perc.ui.} catalog for the session locale) with an English
 * {@code @}-fallback. Every {@code perc.ui.editor@...} key the TipTap toolbar
 * ({@code TipTapToolbar.tsx}) and Percussion plugin dialogs ({@code TipTapPercussionBar.tsx})
 * render must therefore be registered in {@code CmsUi.tmx}; otherwise non-English sessions silently
 * stay English. This test pins that registration: each key has exactly one {@code <tu>} with an
 * {@code en-us} segment matching the TypeScript fallback text.
 */
public class PSTipTapEditorKeysTmxTest {

  /** Expected {@code perc.ui.editor@...} tuid suffix (English) for every TipTap toolbar/dialog key. */
  private static final List<String> EXPECTED_ENGLISH = List.of(
      "Rich text",
      "Add row",
      "Remove row",
      "Formatting toolbar",
      "Undo",
      "Redo",
      "Bold",
      "Italic",
      "Underline",
      "Text style",
      "Paragraph",
      "Align left",
      "Align center",
      "Align right",
      "Justify",
      "Bullet list",
      "Numbered list",
      "Outdent list item",
      "Indent list item",
      "Link",
      "Remove link",
      "Link address",
      "Apply link",
      "Close link editor",
      "That link address cannot be used.",
      "Inline code",
      "Code block",
      "Insert table",
      "Add column",
      "Remove column",
      "Delete table",
      "Percussion content",
      "Insert inline link",
      "Insert inline image",
      "Insert inline template",
      "Insert HTML fragment",
      "Insert global variable",
      "Insert more link",
      "View HTML source",
      "Close",
      "Apply",
      "Link address or CMS path",
      "Link text",
      "Title",
      "Target",
      "Same window",
      "New window",
      "Parent window",
      "Top window",
      "Image address or CMS path",
      "Alternate text",
      "Decorative image",
      "Width",
      "Height",
      "Alignment",
      "No alignment",
      "Left",
      "Right",
      "Top",
      "Middle",
      "Bottom",
      "Template path",
      "Template text",
      "HTML fragment",
      "Global variable",
      "HTML source",
      "Fill in the required fields.");

  /** Expected {@code perc.ui.editor@...} suffixes for the #5900 content-picker lookup. */
  private static final List<String> PICKER_ENGLISH_5900 =
      List.of(
          "Browse\u2026",
          "Pick CMS content",
          "Folder",
          "List",
          "Search",
          "Search CMS content",
          "Use selected",
          "No content found.",
          "Content lookup failed.",
          "Loading\u2026",
          "Could not read the selected item.");

  @Test
  public void tiptapToolbarAndDialogKeys_areRegisteredWithEnglish() throws Exception {
    assertKeysRegistered(loadEnSegsByTuid(), EXPECTED_ENGLISH);
    // No exact-size pin: later editor slices may register further perc.ui.editor keys.
  }

  @Test
  public void contentPickerKeys_areRegisteredWithEnglish() throws Exception {
    assertKeysRegistered(loadEnSegsByTuid(), PICKER_ENGLISH_5900);
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
      if (!tuid.startsWith("perc.ui.editor@")) {
        continue;
      }
      collectEnSeg(enSegsByTuid, tuid, tu);
    }
    return enSegsByTuid;
  }

  private static void collectEnSeg(
      Map<String, List<String>> enSegsByTuid, String tuid, Element tu) {
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

  private static void assertKeysRegistered(
      Map<String, List<String>> enSegsByTuid, List<String> expected) {
    Set<String> missing = new LinkedHashSet<>();
    List<String> mismatched = new ArrayList<>();
    List<String> duplicated = new ArrayList<>();
    for (String en : expected) {
      String tuid = "perc.ui.editor@" + en;
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
    assertTrue(missing.isEmpty(), "CmsUi.tmx missing TipTap editor keys: " + missing);
    assertTrue(duplicated.isEmpty(), "CmsUi.tmx has duplicate TipTap editor keys: " + duplicated);
    assertTrue(mismatched.isEmpty(), "CmsUi.tmx en-us seg differs from TS fallback: " + mismatched);
  }

  /**
   * Resolve {@code CmsUi.tmx} portably: prefer the test classpath resource (Maven surefire), then
   * fall back to common source-tree locations relative to the process working directory.
   */
  private static Path resolveCmsUiTmx() throws URISyntaxException {
    URL url = PSTipTapEditorKeysTmxTest.class.getResource("/i18n/CmsUi.tmx");
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
