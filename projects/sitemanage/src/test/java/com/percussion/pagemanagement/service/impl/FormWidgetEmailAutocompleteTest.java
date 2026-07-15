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

/** Regression for GH-106 / v8.1.7 PR #658: form widget email-from a11y + package 1.4.8. */
class FormWidgetEmailAutocompleteTest {

  private static final Path PKG =
      Path.of("modules/perc-packages/src/main/resources/Packages/perc.widget.form");

  @Test
  void formWidgetSetsEmailAutocomplete() throws Exception {
    Path root = resolveRepoRoot();
    Path js = root.resolve(PKG.resolve("sys__UserDependency--web_resources/widgets/form/js/form.js"));
    Path min =
        root.resolve(PKG.resolve("sys__UserDependency--web_resources/widgets/form/js/form.min.js"));
    Path archive = root.resolve(PKG.resolve("psx_archiveInfo.xml"));
    for (Path p : new Path[] {js, min, archive}) {
      if (!Files.isRegularFile(p)) {
        fail(p.toString());
      }
    }
    String form = Files.readString(js, StandardCharsets.UTF_8);
    assertTrue(form.contains("getElementById(\"email-from\")") || form.contains("getElementById('email-from')"));
    assertTrue(form.contains("autocomplete") && form.contains("email"));
    // type is set via the DOM property (emailField.type = "email"), not setAttribute
    assertTrue(
        form.contains(".type = \"email\"") || form.contains(".type='email'") || form.contains(".type = 'email'"));
    assertTrue(form.contains("setAttribute") && form.contains("autocomplete"));

    String minJs = Files.readString(min, StandardCharsets.UTF_8);
    assertTrue(minJs.contains("email-from"));
    assertTrue(minJs.contains("autocomplete"));
    assertTrue(minJs.contains("email"));

    String arch = Files.readString(archive, StandardCharsets.UTF_8);
    assertTrue(arch.contains("<Version>1.4.8</Version>"));
  }

  /**
   * Walks up from the process working directory until a monorepo marker is
   * found ({@code modules/perc-packages} next to root {@code pom.xml}), so the
   * test is robust when run from repo root, module basedir, or nested CI paths.
   */
  private static Path resolveRepoRoot() {
    Path dir = Path.of("").toAbsolutePath().normalize();
    while (dir != null) {
      if (Files.isDirectory(dir.resolve("modules/perc-packages"))
          && Files.isRegularFile(dir.resolve("pom.xml"))) {
        return dir;
      }
      Path parent = dir.getParent();
      if (parent == null || parent.equals(dir)) {
        break;
      }
      dir = parent;
    }
    fail("could not resolve monorepo root from " + Path.of("").toAbsolutePath().normalize());
    return Path.of("").toAbsolutePath().normalize();
  }
}
