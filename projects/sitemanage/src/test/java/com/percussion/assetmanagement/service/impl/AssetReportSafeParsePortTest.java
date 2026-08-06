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
package com.percussion.assetmanagement.service.impl;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Regression for v8.1.7 PR #735: safer asset report parsing + email/report help wiring. */
class AssetReportSafeParsePortTest {

  @Test
  void assetServiceUsesSafeParseHelpers() throws Exception {
    Path root = resolveRoot();
    Path svc =
        root.resolve(
            "projects/sitemanage/src/main/java/com/percussion/assetmanagement/service/impl/PSAssetService.java");
    Path wf = root.resolve("system/src/main/java/com/percussion/workflow/PSWorkFlowUtils.java");
    Path tmx = root.resolve("modules/perc-i18n/src/main/resources/i18n/CmsUi.tmx");
    Path rest = root.resolve("rest/src/main/java/com/percussion/rest/assets/AssetsResource.java");
    for (Path p : new Path[] {svc, wf, tmx, rest}) {
      if (!Files.isRegularFile(p)) fail(p.toString());
    }
    String asset = Files.readString(svc, StandardCharsets.UTF_8);
    assertTrue(asset.contains("safeParseInt"));
    assertTrue(asset.contains("safeParseLong"));
    assertTrue(
        asset.contains("safeParseInt((String) a.getFields().get(IPSHtmlParameters.SYS_CONTENTID)"));

    String mail = Files.readString(wf, StandardCharsets.UTF_8);
    assertTrue(mail.contains("SMTP_PORT property is not a number"));
    assertTrue(mail.contains("Failed to send email with attachment"));

    String xml = Files.readString(tmx, StandardCharsets.UTF_8);
    assertTrue(xml.contains("perc.ui.reports.gadget@Report Help"));

    String r = Files.readString(rest, StandardCharsets.UTF_8);
    assertTrue(r.contains("Response.accepted().status(202)"));
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
