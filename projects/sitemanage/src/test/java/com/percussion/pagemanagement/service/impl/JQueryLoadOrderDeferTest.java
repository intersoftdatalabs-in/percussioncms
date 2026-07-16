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

/**
 * Regression for GH-819 / v8.1.7 PR #824: when the jQuery widget is deferred, dependent scripts
 * (jQuery UI and other catalogued JS) must also defer outside edit mode.
 */
class JQueryLoadOrderDeferTest {

  private static final Path VM =
      Path.of(
          "system/cms/content/applications/sys_resources/ApplicationFiles/vm/sys_assembly.vm");

  @Test
  void assemblyDefersDependentsWhenJqueryDeferred() throws Exception {
    Path root = resolveRepoRoot();
    Path vm = root.resolve(VM);
    if (!Files.isRegularFile(vm)) {
      fail("expected " + vm.toAbsolutePath());
    }
    String text = Files.readString(vm, StandardCharsets.UTF_8);
    assertTrue(text.contains("#set($deferDependents = false)##"));
    assertTrue(text.contains("#set($jqueryIsDeferred = \"no\")##")
        || text.contains("#set($jqueryIsDeferred = \"no\")##".replace("\\\"", "\"")));
    assertTrue(text.contains("$jqueryIsDeferred == \"yes\""));
    assertTrue(text.contains("#if($deferDependents) defer#end"));
    assertTrue(text.contains("#if($jqueryWidgetInstances.size() > 0)##"));
    // force UI deferred when dependents deferred
    assertTrue(text.contains("#if($deferDependents)##"));
    assertTrue(text.contains("#set($isDeferred = \"yes\")##"));
  }

  private static Path resolveRepoRoot() {
    Path cwd = Path.of("").toAbsolutePath().normalize();
    Path candidate = cwd.resolve("../..").normalize();
    if (Files.isDirectory(candidate.resolve("system"))) {
      return candidate;
    }
    if (Files.isDirectory(cwd.resolve("system"))) {
      return cwd;
    }
    fail("could not resolve monorepo root");
    return cwd;
  }
}
