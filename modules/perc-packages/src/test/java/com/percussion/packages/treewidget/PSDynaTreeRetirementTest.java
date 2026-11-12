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

package com.percussion.packages.treewidget;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * DynaTree retirement gate (issue #5662, parent audit #5627; delivery-side
 * closure issue #6052).
 *
 * <p>The vendored DynaTree 1.1.0 copy was deleted when tree widgets consolidated onto the
 * npm-tracked FancyTree build; every remaining functional reference under product
 * {@code Packages} must go with it. Fails on:
 *
 * <ul>
 *   <li>any {@code psxctl:FileLocation} in widget {@code *.xsl} controls pointing at a
 *       {@code dynatree} path (deleted vendored bytes 404 at runtime);
 *   <li>any {@code .dynatree(} plugin invocation or {@code dynatree.js} / {@code dynatree/} path
 *       reference in shipped {@code *.js} (the plugin global no longer exists);
 *   <li>any {@code dynatree} entry in the published category-list DTS resources
 *       ({@code percCategoryList.xml}) — published pages provision FancyTree from the
 *       {@code web_resources/cm} publish-time copies (issue #6052), so a stale
 *       {@code jquery-dynatree_*} entry 404s on every collapsible category page;
 *   <li>any {@code dynatree} selector or path in shipped {@code *.css} (FancyTree renders
 *       {@code .fancytree-*}, so {@code .dynatree-*} rules never match).
 * </ul>
 *
 * <p>Positive controls: the published category widget ({@code category.min.js}) must invoke
 * {@code .fancytree(}, matching its unminified twin {@code category.js}; the DTS resources
 * must reference the published FancyTree JS + skin CSS that
 * {@code installDistributionFiles.xml} copies into {@code web_resources/cm}.
 *
 * <p>Cross-platform: all path construction uses {@link Path#resolve(String)} / {@link Files}.
 */
class PSDynaTreeRetirementTest {

  private static final Pattern XSL_LOCATION =
      Pattern.compile("<psxctl:FileLocation>(.*?)</psxctl:FileLocation>", Pattern.DOTALL);

  private static final Pattern JS_DYNATREE_USAGE =
      Pattern.compile("\\.dynatree\\s*\\(|dynatree\\.js|dynatree/", Pattern.CASE_INSENSITIVE);

  @Test
  void widgetXsl_declaresNoDynaTreeFileLocations() throws Exception {
    Path packagesRoot = locatePackagesRoot();
    assertNotNull(
        packagesRoot,
        "Packages root must be visible when running module Surefire from perc-packages "
            + "(src/main/resources/Packages)");

    List<String> offenders = new ArrayList<>();
    Files.walkFileTree(
        packagesRoot,
        new SimpleFileVisitor<>() {
          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            if (file.getFileName().toString().endsWith(".xsl")) {
              try {
                String content = Files.readString(file, StandardCharsets.UTF_8);
                var matcher = XSL_LOCATION.matcher(content);
                while (matcher.find()) {
                  if (matcher.group(1).toLowerCase().contains("dynatree")) {
                    offenders.add(
                        packagesRoot.relativize(file) + " -> " + matcher.group(1).strip());
                  }
                }
              } catch (IOException e) {
                throw new IllegalStateException("cannot read " + file, e);
              }
            }
            return FileVisitResult.CONTINUE;
          }
        });

    assertTrue(
        offenders.isEmpty(),
        () -> "retired DynaTree FileLocations must not reappear in widget XSL controls; found: "
            + offenders);
  }

  @Test
  void shippedJs_invokesNoDynaTreePlugin() throws Exception {
    Path packagesRoot = locatePackagesRoot();
    assertNotNull(packagesRoot, "Packages root must be visible from perc-packages Surefire");

    List<String> offenders = new ArrayList<>();
    Files.walkFileTree(
        packagesRoot,
        new SimpleFileVisitor<>() {
          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            if (file.getFileName().toString().endsWith(".js")) {
              try {
                String content = Files.readString(file, StandardCharsets.UTF_8);
                if (JS_DYNATREE_USAGE.matcher(content).find()) {
                  offenders.add(packagesRoot.relativize(file).toString());
                }
              } catch (IOException e) {
                throw new IllegalStateException("cannot read " + file, e);
              }
            }
            return FileVisitResult.CONTINUE;
          }
        });

    assertTrue(
        offenders.isEmpty(),
        () -> "shipped widget JS must not invoke the retired DynaTree plugin; found: "
            + offenders);
  }

  @Test
  void categoryMinJs_matchesFancyTreeTwin() throws Exception {
    Path packagesRoot = locatePackagesRoot();
    assertNotNull(packagesRoot, "Packages root must be visible from perc-packages Surefire");

    Path minified =
        packagesRoot
            .resolve("perc.widget.categoryList")
            .resolve("sys__UserDependency--web_resources")
            .resolve("widgets")
            .resolve("category")
            .resolve("js")
            .resolve("category.min.js");
    assertTrue(Files.isRegularFile(minified), "expected shipped minified widget: " + minified);

    String content = Files.readString(minified, StandardCharsets.UTF_8);
    assertTrue(
        content.contains(".fancytree("),
        "category.min.js must invoke .fancytree( like its twin category.js (issue #5662)");
  }

  private static Path locatePackagesRoot() {
    Path candidate = Path.of("src", "main", "resources", "Packages");
    if (Files.isDirectory(candidate)) {
      return candidate.toAbsolutePath().normalize();
    }
    Path cwd = Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
    Path alt =
        cwd.resolve("src").resolve("main").resolve("resources").resolve("Packages");
    return Files.isDirectory(alt) ? alt : null;
  }

  @Test
  void categoryListResources_pointsAtPublishedFancyTree() throws Exception {
    Path packagesRoot = locatePackagesRoot();
    assertNotNull(packagesRoot, "Packages root must be visible from perc-packages Surefire");

    Path resources =
        packagesRoot
            .resolve("perc.widget.categoryList")
            .resolve("sys__UserDependency--rxconfig")
            .resolve("Resources")
            .resolve("percCategoryList.xml");
    assertTrue(
        Files.isRegularFile(resources), "expected DTS resources: " + resources.toAbsolutePath());
    String xml = Files.readString(resources, StandardCharsets.UTF_8);

    assertTrue(
        !xml.toLowerCase().contains("dynatree"),
        "percCategoryList.xml must not reference retired DynaTree (404s on published pages)");
    assertTrue(
        xml.contains("id=\"jquery-fancytree_js\"")
            && xml.contains("/web_resources/cm/jslib/jquery.fancytree.js"),
        "percCategoryList.xml must provision published FancyTree JS from web_resources/cm");
    assertTrue(
        xml.contains("id=\"jquery-fancytree_css\"")
            && xml.contains("/web_resources/cm/css/fancytree/skin/ui.fancytree.css"),
        "percCategoryList.xml must provision published FancyTree skin CSS from web_resources/cm");
  }

  @Test
  void shippedCss_hasNoDynaTreeSelectors() throws Exception {
    Path packagesRoot = locatePackagesRoot();
    assertNotNull(packagesRoot, "Packages root must be visible from perc-packages Surefire");

    List<String> offenders = new ArrayList<>();
    Files.walkFileTree(
        packagesRoot,
        new SimpleFileVisitor<>() {
          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            if (file.getFileName().toString().endsWith(".css")) {
              try {
                String content = Files.readString(file, StandardCharsets.UTF_8);
                if (content.toLowerCase().contains("dynatree")) {
                  offenders.add(packagesRoot.relativize(file).toString());
                }
              } catch (IOException e) {
                throw new IllegalStateException("cannot read " + file, e);
              }
            }
            return FileVisitResult.CONTINUE;
          }
        });

    // Dead-skin hosts outside Packages (same retirement; FancyTree renders
    // .fancytree-*, so .dynatree-* rules never match).
    for (String rel :
        List.of(
            "system/cms/content/applications/sys_resources/ApplicationFiles/css/checkboxTree/tree.css",
            "system/cms/content/applications/sys_resources/ApplicationFiles/css/cmlite.css",
            "system/cms/content/applications/rx_resources/ApplicationFiles/default_theme/theme.css",
            "projects/sitemanage/src/test/resources/importer/data/rx_resources/default_theme/theme.css")) {
      Path css = resolveRepoRoot().resolve(rel);
      assertTrue(Files.isRegularFile(css), "expected shipped/test CSS at " + css.toAbsolutePath());
      try {
        if (Files.readString(css, StandardCharsets.UTF_8).toLowerCase().contains("dynatree")) {
          offenders.add(rel);
        }
      } catch (IOException e) {
        throw new IllegalStateException("cannot read " + css, e);
      }
    }

    assertTrue(
        offenders.isEmpty(),
        () -> "retired DynaTree selectors must not reappear in shipped CSS; found: " + offenders);
  }

  private static Path resolveRepoRoot() {
    Path cwd = Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
    Path candidate = cwd.resolve("..").resolve("..").normalize();
    if (Files.isDirectory(candidate.resolve("system"))
        && Files.isDirectory(candidate.resolve("WebUI"))) {
      return candidate;
    }
    if (Files.isDirectory(cwd.resolve("system")) && Files.isDirectory(cwd.resolve("WebUI"))) {
      return cwd;
    }
    assertNotNull(null, "could not resolve monorepo root (need system/ + WebUI/)");
    return cwd; // unreachable
  }
}
