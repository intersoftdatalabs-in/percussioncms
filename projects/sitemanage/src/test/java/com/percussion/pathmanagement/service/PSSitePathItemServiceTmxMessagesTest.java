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
package com.percussion.pathmanagement.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * Regression for GH-879 / v8.1.7 PR #913: orphaned site/page path errors load from TMX (EN segs use
 * curly apostrophes to avoid HTML encoding of straight apostrophes).
 */
class PSSitePathItemServiceTmxMessagesTest {

  private static final char CURLY = '\u2019';

  @Test
  void sitePathErrorsComeFromTmxWithCurlyApostrophes() throws Exception {
    Path root = resolveRoot();
    Path svc =
        root.resolve(
            "projects/sitemanage/src/main/java/com/percussion/pathmanagement/service/impl/PSSitePathItemService.java");
    Path tmx = root.resolve("modules/perc-i18n/src/main/resources/i18n/CmsUi.tmx");
    if (!Files.isRegularFile(svc) || !Files.isRegularFile(tmx)) {
      fail("expected service and TMX under " + root);
    }
    String java = Files.readString(svc, StandardCharsets.UTF_8);
    assertTrue(java.contains("PSI18NTranslationKeyValues"));
    assertTrue(java.contains("perc.ui.pathmanagement@Oops.  We can't find the site "));
    assertTrue(java.contains("perc.ui.pathmanagement@.  It may have been deleted."));
    assertTrue(
        java.contains("perc.ui.pathmanagement@Oops. We're sorry. The requested page is no longer"));
    assertTrue(java.contains("available."));
    // no hardcoded English user strings left in findItem orphan path
    assertFalse(java.contains("? \"Oops.  We can"));

    String xml = Files.readString(tmx, StandardCharsets.UTF_8);
    assertTrue(xml.contains("perc.ui.pathmanagement@Oops.  We can't find the site "));
    assertTrue(xml.contains("perc.ui.pathmanagement@.  It may have been deleted."));
    assertTrue(
        xml.contains(
            "perc.ui.pathmanagement@Oops. We're sorry. The requested page is no longer"
                + " available."));
    assertTrue(xml.contains("We can" + CURLY + "t find the site"));
    assertTrue(xml.contains("We" + CURLY + "re sorry"));
  }

  private static Path resolveRoot() {
    Path cwd = Path.of("").toAbsolutePath().normalize();
    Path up = cwd.resolve("../..").normalize();
    if (Files.isDirectory(up.resolve("modules/perc-i18n"))) return up;
    if (Files.isDirectory(cwd.resolve("modules/perc-i18n"))) return cwd;
    fail("could not resolve monorepo root");
    return cwd;
  }
}
