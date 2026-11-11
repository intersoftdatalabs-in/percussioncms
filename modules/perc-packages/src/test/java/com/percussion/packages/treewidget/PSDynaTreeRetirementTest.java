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
 * DynaTree retirement gate (issue #5662, parent audit #5627).
 *
 * <p>The vendored DynaTree 1.1.0 copy was deleted when tree widgets consolidated onto the
 * npm-tracked FancyTree build; every remaining functional reference under product
 * {@code Packages} must go with it. Fails on:
 *
 * <ul>
 *   <li>any {@code psxctl:FileLocation} in widget {@code *.xsl} controls pointing at a
 *       {@code dynatree} path (deleted vendored bytes 404 at runtime);
 *   <li>any {@code .dynatree(} plugin invocation or {@code dynatree.js} / {@code dynatree/} path
 *       reference in shipped {@code *.js} (the plugin global no longer exists).
 * </ul>
 *
 * <p>Positive control: the published category widget ({@code category.min.js}) must invoke
 * {@code .fancytree(}, matching its unminified twin {@code category.js}.
 *
 * <p>Out of scope (delivery-side provisioning, tracked separately): the
 * {@code sys__UserDependency--rxconfig/Resources/percCategoryList.xml} DTS resource entries and
 * dead {@code .dynatree-*} CSS selectors in widget stylesheets, which need a live
 * publish-path verification.
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
}
