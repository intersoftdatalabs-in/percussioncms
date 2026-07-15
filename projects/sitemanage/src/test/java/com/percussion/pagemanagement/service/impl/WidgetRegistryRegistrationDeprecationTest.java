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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * Regression for GH-844 / v8.1.7 PR #850: Registration widget is listed under the Deprecated group
 * and labels carry the (Deprecated) suffix.
 */
class WidgetRegistryRegistrationDeprecationTest {

  @Test
  void registrationIsInDeprecatedGroupOnly() throws Exception {
    String xml;
    try (InputStream in =
        Thread.currentThread()
            .getContextClassLoader()
            .getResourceAsStream(
                "com/percussion/pagemanagement/service/impl/WidgetRegistry.xml")) {
      assertNotNull(in, "WidgetRegistry.xml must be on the classpath");
      xml = new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }

    assertFalse(
        xml.contains("<widget name=\"Registration\" />")
            || xml.contains("<widget name=\"Registration\"/>"),
        "Registration must not remain in the active Percussion group");
    assertTrue(
        xml.contains("Registration (Deprecated)"),
        "Registration must appear with Deprecated label");
    int deprecatedIdx = xml.indexOf("<group name=\"Deprecated\">");
    int regIdx = xml.indexOf("Registration (Deprecated)");
    assertTrue(deprecatedIdx >= 0, "Deprecated group must exist");
    assertTrue(
        regIdx > deprecatedIdx,
        "Registration (Deprecated) entry must appear after Deprecated group opens");
  }

  @Test
  void packageLabelsCarryDeprecatedSuffix() throws Exception {
    Path root = resolveRepoRoot();
    Path itemDef =
        root.resolve(
            "modules/perc-packages/src/main/resources/Packages/perc.widget.registration"
                + "/percRegistrationAsset.itemDef.contentType");
    Path nodeDef =
        root.resolve(
            "modules/perc-packages/src/main/resources/Packages/perc.widget.registration"
                + "/percRegistrationAsset.nodeDef.contentType");
    Path widget =
        root.resolve(
            "modules/perc-packages/src/main/resources/Packages/perc.widget.registration"
                + "/sys__UserDependency--rxconfig/Widgets/percRegistration.xml");
    for (Path p : new Path[] {itemDef, nodeDef, widget}) {
      if (!Files.isRegularFile(p)) {
        fail("expected " + p.toAbsolutePath());
      }
      String text = Files.readString(p, StandardCharsets.UTF_8);
      assertTrue(
          text.contains("Deprecated"),
          p.getFileName() + " must include Deprecated in label/title/description");
    }
  }

  private static Path resolveRepoRoot() {
    Path cwd = Path.of("").toAbsolutePath().normalize();
    Path candidate = cwd.resolve("../..").normalize();
    if (Files.isDirectory(candidate.resolve("modules"))) {
      return candidate;
    }
    if (Files.isDirectory(cwd.resolve("modules"))) {
      return cwd;
    }
    fail("could not resolve monorepo root");
    return cwd;
  }
}
