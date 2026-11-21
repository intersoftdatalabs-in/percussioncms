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
 * Classic code-editor legend i18n registration for issue #6903 (parent #5626).
 *
 * <p>The {@code sys_TextArea} codeeditor legend in {@code sys_Templates.xsl} used to render
 * hardcoded English (find/replace shortcut legend, {@code Mode:} badge, {@code Dark mode} /
 * {@code A-} / {@code A+} display prefs, bundle-load notice, CodeMirror screen-reader label).
 * The legend now resolves every string through {@code getLocaleString} ({@code $lang}) with the
 * English text as the {@code @}-fallback key suffix. Each {@code psx.ce.codeeditor@...} key the
 * XSL references must therefore be registered in {@code SystemResources.tmx}; otherwise
 * non-English sessions silently stay on the raw key fallback instead of a catalogued pivot.
 * This test pins that registration: each key has exactly one {@code <tu>} with an {@code en-us}
 * segment matching the XSL fallback text. Per-key translations remain owned by
 * {@code i18n_translate.py}; until a locale ships a TUV the runtime lookup chain falls back to
 * the en-us pivot, so this test pins en-us only by design.
 */
public class PSCodeEditorKeysTmxTest {

  /** Full {@code psx.ce.codeeditor...@English} tuids referenced by the legend branch. */
  private static final List<String> EXPECTED_TUIDS =
      List.of(
          "psx.ce.codeeditor.legend.shortcuts@Find: Ctrl-F (Mac: Cmd-F) | Find next: Ctrl-G (Mac: Cmd-G) | Find previous: Shift-Ctrl-G (Mac: Shift-Cmd-G) | Replace: Shift-Ctrl-F (Mac: Cmd-Alt-F) | Replace all: Shift-Ctrl-R (Mac: Shift-Cmd-Alt-F)",
          "psx.ce.codeeditor.legend.validation@Validation: unbalanced tags are flagged in the gutter before save - red error, yellow warning; hover a marker for details",
          "psx.ce.codeeditor.mode.label@Mode:",
          "psx.ce.codeeditor.theme.dark@Dark mode",
          "psx.ce.codeeditor.theme.toggle.label@Toggle dark mode for the code editor",
          "psx.ce.codeeditor.font.decrease@A-",
          "psx.ce.codeeditor.font.decrease.label@Decrease code editor font size",
          "psx.ce.codeeditor.font.increase@A+",
          "psx.ce.codeeditor.font.increase.label@Increase code editor font size",
          "psx.ce.codeeditor.load.error@Code editor could not be loaded. You can still edit this field as plain text.",
          "psx.ce.codeeditor.screenreader.label@HTML Source Code Editor");

  @Test
  public void codeEditorLegendKeys_areRegisteredWithEnglish() throws Exception {
    Path tmx = resolveSystemResourcesTmx();
    assertNotNull(tmx, "SystemResources.tmx not found");
    Map<String, List<String>> enSegsByTuid = loadEnSegsByTuid(parse(tmx));
    Set<String> missing = new LinkedHashSet<>();
    List<String> mismatched = new ArrayList<>();
    List<String> duplicated = new ArrayList<>();
    for (String tuid : EXPECTED_TUIDS) {
      List<String> segs = enSegsByTuid.get(tuid);
      if (segs == null || segs.isEmpty()) {
        missing.add(tuid);
      } else {
        if (segs.size() > 1) {
          duplicated.add(tuid);
        }
        String english = tuid.substring(tuid.indexOf('@') + 1);
        if (!english.equals(segs.get(0))) {
          mismatched.add(tuid + " -> " + segs.get(0));
        }
      }
    }
    assertTrue(missing.isEmpty(), "SystemResources.tmx missing code-editor legend keys: " + missing);
    assertTrue(duplicated.isEmpty(), "SystemResources.tmx has duplicate code-editor keys: " + duplicated);
    assertTrue(
        mismatched.isEmpty(), "SystemResources.tmx en-us seg differs from XSL fallback: " + mismatched);
  }

  private static Map<String, List<String>> loadEnSegsByTuid(Document doc) {
    Map<String, List<String>> enSegsByTuid = new LinkedHashMap<>();
    NodeList tus = doc.getElementsByTagName("tu");
    for (int i = 0; i < tus.getLength(); i++) {
      Element tu = (Element) tus.item(i);
      String tuid = tu.getAttribute("tuid");
      if (!tuid.startsWith("psx.ce.codeeditor")) {
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
   * Resolve {@code SystemResources.tmx} portably: prefer the test classpath resource (Maven
   * surefire), then fall back to common source-tree locations relative to the process working
   * directory.
   */
  private static Path resolveSystemResourcesTmx() throws URISyntaxException {
    URL url = PSCodeEditorKeysTmxTest.class.getResource("/i18n/SystemResources.tmx");
    if (url != null && "file".equalsIgnoreCase(url.getProtocol())) {
      Path p = Paths.get(url.toURI());
      if (Files.isRegularFile(p)) {
        return p;
      }
    }
    Path[] candidates =
        new Path[] {
          Paths.get("src", "main", "resources", "i18n", "SystemResources.tmx"),
          Paths.get("modules", "perc-i18n", "src", "main", "resources", "i18n", "SystemResources.tmx"),
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
