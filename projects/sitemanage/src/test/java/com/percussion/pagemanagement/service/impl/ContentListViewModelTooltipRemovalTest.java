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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Regression for GH-880/881 / v8.1.7 PR #883: content list view model must not install a global
 * jQuery UI tooltip (causes persistent dashboard tooltips) and must not require jquery-ui AMD.
 */
class ContentListViewModelTooltipRemovalTest {

  private static final List<String> PATHS =
      List.of(
          "WebUI/src/main/webapp/cm/cui/widgets/contentList/contentList.ViewModel.js",
          "WebUI/src/main/webapp/cm/pages/cui/widgets/contentList/contentList.ViewModel.js",
          "WebUI/war/cui/widgets/contentList/contentList.ViewModel.js");

  @Test
  void noGlobalJqueryUiTooltipInAnyCopy() throws Exception {
    Path root = resolveRepoRoot();
    for (String rel : PATHS) {
      Path p = root.resolve(rel);
      if (!Files.isRegularFile(p)) {
        fail("expected " + p.toAbsolutePath());
      }
      String js = Files.readString(p, StandardCharsets.UTF_8);
      assertFalse(js.contains("jquery-ui"), rel + " must not AMD-require jquery-ui");
      assertFalse(js.contains(".tooltip("), rel + " must not call .tooltip(");
      assertTrue(
          js.contains("define([\"knockout\", \"pubsub\", \"utils\"]"),
          rel + " must keep knockout/pubsub/utils define");
    }
  }

  private static Path resolveRepoRoot() {
    Path cwd = Path.of("").toAbsolutePath().normalize();
    Path candidate = cwd.resolve("../..").normalize();
    if (Files.isDirectory(candidate.resolve("WebUI"))) {
      return candidate;
    }
    if (Files.isDirectory(cwd.resolve("WebUI"))) {
      return cwd;
    }
    fail("could not resolve monorepo root");
    return cwd;
  }
}
