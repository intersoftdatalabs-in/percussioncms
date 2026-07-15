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
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.pagemanagement.service.impl;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Regression for GH-784 / v8.1.7 PR #1169: Categories tab fancytree rebuild/expand guards.
 *
 * <p>These are monorepo path tests (not pure unit tests): they validate that the three shipped
 * PercCategoryView.js copies retain the structural guards. When the monorepo root cannot be
 * resolved (module-only CI checkout), tests are skipped rather than failing spuriously.
 */
class CategoriesFancytreePortTest {

  private static final List<String> VIEW_PATHS =
      List.of(
          "WebUI/war/views/PercCategoryView.js",
          "WebUI/src/main/webapp/cm/views/PercCategoryView.js",
          "WebUI/src/main/webapp/cm/app/js/legacy/views/PercCategoryView.js");

  private static Path repoRoot;

  @BeforeAll
  static void resolveRootOrSkip() {
    repoRoot = findRepoRoot();
    assumeTrue(repoRoot != null, "monorepo root with WebUI/ not found; skip path-based checks");
  }

  @Test
  void destroyFailureIsLoggedNotSwallowedSilently() {
    for (String rel : VIEW_PATHS) {
      String js = read(rel);
      assertTrue(
          js.contains("fancytree destroy failed") || js.contains("destroy failed"),
          rel + " should log destroy failures");
      assertTrue(js.contains("hadTree"), rel + " should track whether a tree existed");
      assertTrue(
          js.contains("removeData(\"ui-fancytree\")") || js.contains("removeData('ui-fancytree')"),
          rel + " should clean widget data after failed destroy");
    }
  }

  @Test
  void autoCollapseDefaultDerivedFromTreeOptions() {
    for (String rel : VIEW_PATHS) {
      String js = read(rel);
      assertTrue(
          js.contains("tree && tree.options ? tree.options.autoCollapse : false")
              || js.contains("tree && tree.options")
                  && js.contains("tree.options.autoCollapse : false"),
          rel + " should derive autoCollapse from tree.options (no dead true default)");
      assertTrue(
          !Pattern.compile("var autoCollapse\\s*=\\s*true\\s*;").matcher(js).find(),
          rel + " must not use misleading autoCollapse = true default");
    }
  }

  @Test
  void generateUidPrefersCryptoGetRandomValuesInAllCopies() {
    for (String rel : VIEW_PATHS) {
      String js = read(rel);
      assertTrue(js.contains("crypto.getRandomValues"), rel + " needs crypto.getRandomValues");
      assertTrue(js.contains("Uint8Array"), rel + " needs Uint8Array buffer");
    }
  }

  @Test
  void newNodeSetsExplicitKeyAndExpandsParentsSafely() {
    for (String rel : VIEW_PATHS) {
      String js = read(rel);
      assertTrue(
          js.contains("key: uid") || js.contains("key : uid"),
          rel + " new nodes need explicit key");
      assertTrue(
          js.contains("noAnimation") || js.contains("setExpanded"),
          rel + " parent expand should prefer noAnimation/setExpanded");
    }
  }

  private static String read(String rel) {
    Path p = repoRoot.resolve(rel);
    if (!Files.isRegularFile(p)) {
      fail("missing " + p);
    }
    try {
      return Files.readString(p, StandardCharsets.UTF_8);
    } catch (Exception e) {
      fail("read failed " + p + ": " + e.getMessage());
      return "";
    }
  }

  /** Walk parents for a directory containing WebUI/ and a root pom.xml. */
  private static Path findRepoRoot() {
    Path dir = Path.of("").toAbsolutePath().normalize();
    for (int i = 0; i < 8 && dir != null; i++) {
      if (Files.isDirectory(dir.resolve("WebUI")) && Files.isRegularFile(dir.resolve("pom.xml"))) {
        return dir;
      }
      dir = dir.getParent();
    }
    return null;
  }
}
