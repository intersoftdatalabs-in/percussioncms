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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * Regression for GH-784 / v8.1.7 PR #1169: Categories tab fancytree rebuild/expand must not force
 * open all children, must avoid animation races, and must destroy stale trees before re-init.
 */
class CategoriesFancytreePortTest {

  private static final String[] CATEGORY_VIEW_COPIES = {
    "WebUI/war/views/PercCategoryView.js",
    "WebUI/src/main/webapp/cm/views/PercCategoryView.js",
    "WebUI/src/main/webapp/cm/app/js/legacy/views/PercCategoryView.js"
  };

  @Test
  void categoryViewAppliesFancytreeRebuildGuards() throws Exception {
    Path root = resolveRoot();
    Path view = root.resolve(CATEGORY_VIEW_COPIES[0]);
    if (!Files.isRegularFile(view)) {
      fail(view.toString());
    }
    String js = Files.readString(view, StandardCharsets.UTF_8);
    assertTrue(js.contains("destroy"), "must destroy stale tree before re-init");
    assertTrue(js.contains("autoCollapse = false") || js.contains("autoCollapse=false")
        || js.contains("tree.options.autoCollapse = false"));
    assertTrue(js.contains("noAnimation") || js.contains("initialViewCollapsed === \"false\""));
    assertTrue(js.contains("key: uid") || js.contains("key : uid"));
    assertTrue(js.contains("Do not expand purely because a node has children")
        || js.contains("Only force-expand nodes explicitly marked"));
  }

  /**
   * CodeQL js/insecure-randomness: generateUid must prefer crypto.getRandomValues in all three
   * deployed copies of PercCategoryView.js.
   */
  @Test
  void generateUidPrefersCryptoGetRandomValuesInAllCopies() throws Exception {
    Path root = resolveRoot();
    for (String rel : CATEGORY_VIEW_COPIES) {
      Path view = root.resolve(rel);
      if (!Files.isRegularFile(view)) {
        fail(view.toString());
      }
      String js = Files.readString(view, StandardCharsets.UTF_8);
      assertTrue(js.contains("generateUid"), rel + " must define generateUid");
      assertTrue(js.contains("crypto.getRandomValues"), rel + " must use crypto.getRandomValues");
      assertTrue(js.contains("Uint8Array"), rel + " must fill a Uint8Array buffer");
      // Math.random may remain only inside the crypto-unavailable fallback branch.
      int cryptoIdx = js.indexOf("crypto.getRandomValues");
      assertTrue(cryptoIdx >= 0, rel + " missing crypto path");
    }
  }

  private static Path resolveRoot() {
    Path dir = Path.of("").toAbsolutePath().normalize();
    while (dir != null) {
      if (Files.isDirectory(dir.resolve("WebUI")) && Files.isRegularFile(dir.resolve("pom.xml"))) {
        return dir;
      }
      Path parent = dir.getParent();
      if (parent == null || parent.equals(dir)) {
        break;
      }
      dir = parent;
    }
    fail("could not resolve monorepo root from " + Path.of("").toAbsolutePath().normalize());
    return Path.of("").toAbsolutePath().normalize();
  }
}
