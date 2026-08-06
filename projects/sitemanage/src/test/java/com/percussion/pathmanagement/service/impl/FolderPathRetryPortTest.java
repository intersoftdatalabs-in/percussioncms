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
package com.percussion.pathmanagement.service.impl;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Regression for GH-867 / v8.1.7 PRs #1152+#1156. */
class FolderPathRetryPortTest {

  @Test
  void addFolderRetriesLookup() throws Exception {
    Path root = resolveRepoRoot();
    Path svc =
        root.resolve(
            "projects/sitemanage/src/main/java/com/percussion/pathmanagement/service/impl/PSPathItemService.java");
    String text = Files.readString(svc, StandardCharsets.UTF_8);
    assertTrue(text.contains("int retryCount = 5;"));
    assertTrue(text.contains("int retryDelayMs = 200;"));
    assertTrue(text.contains("Folder added but path not found, retrying lookup"));
  }

  @Test
  void openPathRetriesAndDeleteSilentOnMiss() throws Exception {
    Path root = resolveRepoRoot();
    Path pathMgr = root.resolve("WebUI/war/plugins/perc_path_manager.js");
    Path del = root.resolve("WebUI/war/widgets/perc_delete_page_button.js");
    Path utils = root.resolve("WebUI/war/plugins/perc_utils.js");
    for (Path p : new Path[] {pathMgr, del, utils}) {
      if (!Files.isRegularFile(p)) {
        fail(p.toString());
      }
    }
    String pm = Files.readString(pathMgr, StandardCharsets.UTF_8);
    assertTrue(pm.contains("maxRetries = 6"));
    assertTrue(pm.contains("retryDelay = 300"));
    assertTrue(pm.contains("xhr.status === 404"));

    String d = Files.readString(del, StandardCharsets.UTF_8);
    assertTrue(d.toLowerCase().contains("silently disable"));

    String u = Files.readString(utils, StandardCharsets.UTF_8);
    assertTrue(u.contains("isRenamingFolder"));
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
