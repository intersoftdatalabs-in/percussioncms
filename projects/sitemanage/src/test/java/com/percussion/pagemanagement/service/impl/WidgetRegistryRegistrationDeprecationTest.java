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
 *
 * <p>Package XML files live under {@code modules/perc-packages} (not the sitemanage classpath).
 * {@link #resolveRepoRoot()} therefore walks from the Surefire CWD ({@code projects/sitemanage}) to
 * the monorepo root — the same pattern as other 005 migration package-file tests.
 */
class WidgetRegistryRegistrationDeprecationTest {

  @Test
  void registrationIsInDeprecatedGroupOnly() throws Exception {
    String xml;
    try (InputStream in =
        Thread.currentThread()
            .getContextClassLoader()
            .getResourceAsStream("com/percussion/pagemanagement/service/impl/WidgetRegistry.xml")) {
      assertNotNull(in, "WidgetRegistry.xml must be on the classpath");
      xml = new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }

    assertFalse(
        xml.contains("<widget name=\"Registration\" />")
            || xml.contains("<widget name=\"Registration\"/>"),
        "Registration must not remain in the active Percussion group");
    assertTrue(
        xml.contains("<widget name=\"Registration (Deprecated)\" />")
            || xml.contains("<widget name=\"Registration (Deprecated)\"/>"),
        "Registration must appear as Registration (Deprecated) widget entry");
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

    assertTrue(Files.isRegularFile(itemDef), "expected itemDef at " + itemDef.toAbsolutePath());
    assertTrue(Files.isRegularFile(nodeDef), "expected nodeDef at " + nodeDef.toAbsolutePath());
    assertTrue(Files.isRegularFile(widget), "expected widget xml at " + widget.toAbsolutePath());

    String itemText = Files.readString(itemDef, StandardCharsets.UTF_8);
    String nodeText = Files.readString(nodeDef, StandardCharsets.UTF_8);
    String widgetText = Files.readString(widget, StandardCharsets.UTF_8);

    assertTrue(
        itemText.contains("label=\"Registration Asset (Deprecated)\""),
        "itemDef label must be Registration Asset (Deprecated)");
    assertTrue(
        nodeText.contains("<label>Registration Asset (Deprecated)</label>"),
        "nodeDef label must be Registration Asset (Deprecated)");
    assertTrue(
        widgetText.contains("title=\"Registration (Deprecated)\""),
        "widget title must be Registration (Deprecated)");
    assertTrue(
        widgetText.contains(
                "description=\"Widget to build and render a registration form. (Deprecated)\"")
            || widgetText.contains("registration form. (Deprecated)"),
        "widget description must include form. (Deprecated) suffix, not only title");
  }

  private static Path resolveRepoRoot() {
    Path cwd = Path.of("").toAbsolutePath().normalize();
    Path candidate = cwd.resolve("../..").normalize();
    if (Files.isDirectory(candidate.resolve("modules/perc-packages"))
        && Files.isDirectory(candidate.resolve("projects/sitemanage"))) {
      return candidate;
    }
    if (Files.isDirectory(cwd.resolve("modules/perc-packages"))) {
      return cwd;
    }
    fail(
        "could not resolve monorepo root (need modules/perc-packages/). Tried: "
            + candidate
            + " and "
            + cwd
            + " — Surefire basedir is typically projects/sitemanage");
    return cwd;
  }
}
