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
package com.percussion.packages.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Guards the jQuery-UI smoothness theme migration (issue #7381, parent audit
 * #5627).
 *
 * <p>The stock 1.12.1 ThemeRoller bundle checked in under the drifted
 * filename {@code cm/themes/smoothness/jquery-ui-1.8.9.custom.css} (not a
 * custom cut) was replaced by the Dependabot-tracked npm
 * {@code jquery-ui} 1.14 dist theme at the corrected URL
 * {@code cm/themes/smoothness/jquery-ui.css}: the WAR serves it from the
 * generated-webui overlay while the installed-package path
 * ({@code /web_resources/...}) serves the byte-identical payload copy in
 * {@code perc.baseWidgets} with sibling images from
 * {@code perc.Baseline}. This pin fails loudly if the lie filename
 * resurrects anywhere in the payload tree, a widget control descriptor
 * still points at it, the payload copy drifts from the npm banner line,
 * or an {@code images/} reference in the payload CSS has no sibling
 * payload image (image-path parity).
 *
 * <p>Cross-platform: all path construction uses {@link Path#resolve(String)}
 * / {@link Files}; relative keys always use {@code '/'} separators.
 */
class WidgetThemeUrlParityTest {

  private static final String RETIRED_THEME = "jquery-ui-1.8.9.custom.css";
  private static final String THEME_PATH =
      "perc.baseWidgets/sys__UserDependency--web_resources/cm/themes/smoothness/jquery-ui.css";
  private static final Pattern IMAGES_URL = Pattern.compile("url\\(\"?images/([^\")]+)\"?\\)");

  private static Path packagesRoot() {
    List<Path> candidates =
        List.of(
            Path.of(System.getProperty("user.dir"), "src", "main", "resources", "Packages"),
            Path.of(
                System.getProperty("user.dir"),
                "modules",
                "perc-packages",
                "src",
                "main",
                "resources",
                "Packages"));
    for (Path candidate : candidates) {
      if (Files.isDirectory(candidate)) {
        return candidate;
      }
    }
    fail("Packages payload root not found under user.dir=" + System.getProperty("user.dir"));
    throw new IllegalStateException("unreachable");
  }

  @Test
  void retiredLieFilenameGoneFromPayloadTree() throws IOException {
    Path root = packagesRoot();
    List<String> hits = new ArrayList<>();
    try (Stream<Path> walk = Files.walk(root)) {
      walk.filter(Files::isRegularFile)
          .forEach(
              p -> {
                if (p.getFileName().toString().equals(RETIRED_THEME)) {
                  hits.add(root.relativize(p).toString());
                  return;
                }
                if (p.getFileName().toString().endsWith(".log")) {
                  return;
                }
                try {
                  String content = Files.readString(p, StandardCharsets.UTF_8);
                  if (content.contains(RETIRED_THEME)) {
                    hits.add(root.relativize(p).toString());
                  }
                } catch (IOException e) {
                  // Binary payload (png/swf/fla): filename checked above.
                }
              });
    }
    assertTrue(
        hits.isEmpty(),
        "retired lie-filename theme still referenced in payload tree (issue #7381): " + hits);
  }

  @Test
  void payloadThemeCarriesNpmBanner() throws IOException {
    Path root = packagesRoot();
    String content = Files.readString(root.resolve(THEME_PATH), StandardCharsets.UTF_8);
    String[] lines = content.split("\n", -1);
    StringBuilder head = new StringBuilder();
    for (int i = 0; i < Math.min(40, lines.length); i++) {
      head.append(lines[i]).append('\n');
    }
    assertTrue(
        head.toString().contains("jQuery UI - v1.14"),
        "payload theme copy lost the npm 1.14 banner line: " + THEME_PATH);
    assertTrue(
        head.toString().contains("OpenJS Foundation"),
        "payload theme copy lost the upstream attribution: " + THEME_PATH);
  }

  @Test
  void payloadThemeImagesResolveToPayloadSiblings() throws IOException {
    Path root = packagesRoot();
    String css = Files.readString(root.resolve(THEME_PATH), StandardCharsets.UTF_8);
    Matcher m = IMAGES_URL.matcher(css);
    List<String> refs = new ArrayList<>();
    while (m.find()) {
      refs.add(m.group(1));
    }
    assertTrue(!refs.isEmpty(), "payload theme references no images: " + THEME_PATH);
    Path imagesDir =
        root.resolve(
            "perc.Baseline/sys__UserDependency--web_resources/cm/themes/smoothness/images");
    List<String> missing = new ArrayList<>();
    for (String img : refs) {
      if (!Files.isRegularFile(imagesDir.resolve(img))) {
        missing.add(img);
      }
    }
    assertEquals(
        List.of(),
        missing,
        "payload theme images missing from perc.Baseline web_resources (issue #7381)");
  }

  @Test
  void baseWidgetsArchiveMetadataTracksRenamedTheme() throws IOException {
    Path root = packagesRoot();
    String info =
        Files.readString(
            root.resolve("perc.baseWidgets/psx_archiveInfo.xml"), StandardCharsets.UTF_8);
    assertTrue(
        info.contains("web_resources/cm/themes/smoothness/jquery-ui.css"),
        "psx_archiveInfo.xml still references the retired theme filename");
    String manifest =
        Files.readString(
            root.resolve("perc.baseWidgets/psx_archiveManifest.xml"), StandardCharsets.UTF_8);
    assertTrue(
        manifest.contains("jquery-ui.css"),
        "psx_archiveManifest.xml still references the retired theme filename");
    assertTrue(
        !manifest.contains(RETIRED_THEME) && !info.contains(RETIRED_THEME),
        "archive metadata still references the retired theme filename");
  }
}
