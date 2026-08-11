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

package com.percussion.pagemanagement.service.impl;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * Regression for GH-811 / v8.1.7 PR #814: Open Graph widget Jexl must guard each {@code og:*} meta
 * tag so assembly does not append duplicates into AdditionalHeadContent.
 */
class PercOpenGraphDedupeGuardTest {

  private static final String[] OG_PROPS = {
    "og:site_name",
    "og:title",
    "og:description",
    "og:url",
    "og:type",
    "og:image",
    "og:image:width",
    "og:image:height",
    "og:locale",
    "og:fb_app_id"
  };

  @Test
  void openGraphWidgetGuardsEachPropertyBeforeAppend() throws Exception {
    // After Widget XML dual-ship stop-ship (#2897 / cluster #2897), product packages author
    // modern widgets/<stem>/component-package.json only — install Widget XML is materialised.
    Path manifest =
        resolveRepoRoot()
            .resolve(
                "modules/perc-packages/src/main/resources/Packages/perc.openGraphWidget"
                    + "/widgets/percOpenGraph/component-package.json");
    if (!Files.isRegularFile(manifest)) {
      fail("expected Open Graph modern package at " + manifest.toAbsolutePath());
    }
    String json = Files.readString(manifest, StandardCharsets.UTF_8);
    for (String prop : OG_PROPS) {
      // JSON embeds Jexl as raw text: contains(\"property=\\\"og:title\\\"\")
      // (backslash-quote escapes inside the JSON string value)
      String guard = "contains(\\\"property=\\\\\\\"" + prop + "\\\\\\\"\\\")";
      assertTrue(json.contains(guard), "missing contains() guard for " + prop);
    }
  }

  private static Path resolveRepoRoot() {
    Path cwd = Path.of("").toAbsolutePath().normalize();
    // Surefire basedir is projects/sitemanage when -pl projects/sitemanage
    Path candidate = cwd.resolve("../..").normalize();
    if (Files.isDirectory(candidate.resolve("modules/perc-packages"))) {
      return candidate;
    }
    if (Files.isDirectory(cwd.resolve("modules/perc-packages"))) {
      return cwd;
    }
    return cwd;
  }
}
