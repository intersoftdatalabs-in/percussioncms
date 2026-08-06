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
package com.percussion.analytics.service.impl.google;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * Regression for GH-756 / v8.1.7 PR #760: Google Analytics integration uses GA4 Admin + Data APIs
 * rather than Universal Analytics.
 */
class Ga4MigrationPortTest {

  @Test
  void googleAnalyticsProvidersUseGa4Clients() throws Exception {
    Path root = resolveRoot();
    Path helper =
        root.resolve(
            "projects/sitemanage/src/main/java/com/percussion/analytics/service/impl/google/PSGoogleAnalyticsProviderHelper.java");
    Path handler =
        root.resolve(
            "projects/sitemanage/src/main/java/com/percussion/analytics/service/impl/google/PSGoogleAnalyticsProviderHandler.java");
    Path query =
        root.resolve(
            "projects/sitemanage/src/main/java/com/percussion/analytics/service/impl/google/PSGoogleAnalyticsProviderQueryHandler.java");
    Path creds =
        root.resolve(
            "projects/sitemanage/src/main/java/com/percussion/analytics/service/impl/google/GoogleCreds.java");
    Path pom = root.resolve("projects/sitemanage/pom.xml");
    Path parent = root.resolve("pom.xml");
    Path tmx = root.resolve("modules/perc-i18n/src/main/resources/i18n/CmsUi.tmx");

    for (Path p : new Path[] {helper, handler, query, pom, parent, tmx}) {
      if (!Files.isRegularFile(p)) {
        fail("missing " + p);
      }
    }
    assertFalse(Files.exists(creds), "legacy GoogleCreds should be removed");

    String h = Files.readString(helper, StandardCharsets.UTF_8);
    assertTrue(h.contains("getGa4DataClient"));
    assertTrue(h.contains("getGa4AdminClient"));
    assertTrue(h.contains("BetaAnalyticsDataClient"));
    assertTrue(h.contains("AnalyticsAdminServiceClient"));
    assertFalse(h.contains("com.google.api.services.analytics.Analytics"));
    assertFalse(h.contains("AnalyticsReporting"));

    String handlerSrc = Files.readString(handler, StandardCharsets.UTF_8);
    assertTrue(handlerSrc.contains("AnalyticsAdminServiceClient"));
    assertTrue(handlerSrc.contains("PropertySummary"));

    String q = Files.readString(query, StandardCharsets.UTF_8);
    assertTrue(q.contains("RunReportRequest"));
    assertTrue(q.contains("BetaAnalyticsDataClient"));
    assertFalse(q.contains("analyticsreporting"));

    String sitemanagePom = Files.readString(pom, StandardCharsets.UTF_8);
    assertTrue(sitemanagePom.contains("google-analytics-data"));
    assertTrue(sitemanagePom.contains("google-analytics-admin"));

    String parentPom = Files.readString(parent, StandardCharsets.UTF_8);
    assertTrue(parentPom.contains("libraries-bom"));

    String xml = Files.readString(tmx, StandardCharsets.UTF_8);
    assertTrue(xml.contains("Select a Google property for each site:"));
    assertTrue(xml.contains("Select Google Analytics Property"));
  }

  private static Path resolveRoot() {
    Path cwd = Path.of("").toAbsolutePath().normalize();
    Path up = cwd.resolve("../..").normalize();
    if (Files.isDirectory(up.resolve("projects/sitemanage"))) return up;
    if (Files.isDirectory(cwd.resolve("projects/sitemanage"))) return cwd;
    fail("could not resolve monorepo root");
    return cwd;
  }
}
