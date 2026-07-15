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

  @Test
  void categoryViewAppliesFancytreeRebuildGuards() throws Exception {
    Path root = resolveRoot();
    Path view = root.resolve("WebUI/war/views/PercCategoryView.js");
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

  private static Path resolveRoot() {
    Path cwd = Path.of("").toAbsolutePath().normalize();
    Path up = cwd.resolve("../..").normalize();
    if (Files.isDirectory(up.resolve("WebUI"))) return up;
    if (Files.isDirectory(cwd.resolve("WebUI"))) return cwd;
    fail("could not resolve monorepo root");
    return cwd;
  }
}
