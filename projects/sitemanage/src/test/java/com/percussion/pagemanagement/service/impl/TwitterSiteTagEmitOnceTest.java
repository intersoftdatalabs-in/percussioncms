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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * Regression for GH-803 / v8.1.7 PR #838: twitter:site must be appended to AdditionalHeadContent
 * exactly once (not zero, not duplicated).
 */
class TwitterSiteTagEmitOnceTest {

  private static final Path WIDGET =
      Path.of(
          "modules/perc-packages/src/main/resources/Packages/perc.twitterSummaryCards"
              + "/sys__UserDependency--rxconfig/Widgets/percTwitterSummaryCards.xml");

  @Test
  void twitterSiteAppendedExactlyOnce() throws Exception {
    Path root = resolveRepoRoot();
    Path widget = root.resolve(WIDGET);
    if (!Files.isRegularFile(widget)) {
      fail("expected " + widget.toAbsolutePath());
    }
    String xml = Files.readString(widget, StandardCharsets.UTF_8);
    Pattern append =
        Pattern.compile("setAdditionalHeadContent\\([^)]*meta_sitename", Pattern.MULTILINE);
    Matcher m = append.matcher(xml);
    int count = 0;
    while (m.find()) {
      count++;
    }
    assertEquals(
        1, count, "twitter:site (meta_sitename) must be setAdditionalHeadContent exactly once");
    assertTrue(
        xml.contains("if(!empty($use_twitter_site))")
            || xml.contains("if (!empty($use_twitter_site))"),
        "append must be guarded by use_twitter_site");
  }

  private static Path resolveRepoRoot() {
    Path cwd = Path.of("").toAbsolutePath().normalize();
    Path candidate = cwd.resolve("../..").normalize();
    if (Files.isDirectory(candidate.resolve("modules/perc-packages"))) {
      return candidate;
    }
    if (Files.isDirectory(cwd.resolve("modules/perc-packages"))) {
      return cwd;
    }
    fail("could not resolve monorepo root");
    return cwd;
  }
}
