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

package com.percussion.jetty.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * GH-1983: structural contract for the shipped {@code defaults/bin/rxjetty.sh} start-helper
 * template used by {@code install-jetty-service.sh} (systemd and {@code --initd}).
 *
 * <p>The template is sed-substituted ({@code ${rxjetty_service}}) into {@code
 * /etc/init.d/<ServiceName>} and must fork + write {@code JETTY_PID} for {@code Type=forking}
 * units. Tests are read-only on source (and assembled distribution when present).
 */
class RxJettyStartHelperTemplateTest {

  private static final Path MODULE_TEMPLATE =
      Path.of("src", "main", "jetty", "defaults", "bin", "rxjetty.sh");
  private static final Path REPO_TEMPLATE =
      Path.of("modules", "perc-jetty").resolve(MODULE_TEMPLATE);

  private static final Path MODULE_DIST =
      Path.of("target", "distribution", "defaults", "bin", "rxjetty.sh");
  private static final Path REPO_DIST = Path.of("modules", "perc-jetty").resolve(MODULE_DIST);

  private static Path templatePath;
  private static String template;

  @BeforeAll
  static void load() throws Exception {
    templatePath = resolveExisting(MODULE_TEMPLATE, REPO_TEMPLATE);
    if (templatePath == null) {
      fail(
          "missing defaults/bin/rxjetty.sh under module or repo-relative paths from CWD "
              + Path.of("").toAbsolutePath().normalize());
    }
    template = Files.readString(templatePath, StandardCharsets.UTF_8);
  }

  private static Path resolveExisting(Path moduleRelative, Path repoRelative) {
    Path cwd = Path.of("").toAbsolutePath().normalize();
    Path[] candidates =
        new Path[] {
          cwd.resolve(moduleRelative),
          cwd.resolve(repoRelative),
          cwd.getParent() != null ? cwd.getParent().resolve(repoRelative) : null,
        };
    for (Path c : candidates) {
      if (c != null && Files.isRegularFile(c)) {
        return c.normalize();
      }
    }
    return null;
  }

  @Test
  void template_existsWithServiceSedPlaceholder() {
    assertTrue(Files.isRegularFile(templatePath), () -> "missing " + templatePath);
    assertTrue(
        template.contains("${rxjetty_service}"),
        "must keep ${rxjetty_service} for install-jetty-service.sh sed substitution");
    assertTrue(
        template.contains("Provides:") || template.contains("### BEGIN INIT INFO"),
        "LSB init headers for SysV / systemd helper");
  }

  @Test
  void template_forksAndWritesPid_forTypeForking() {
    assertTrue(template.contains("JETTY_PID"), "JETTY_PID contract for PIDFile=");
    assertTrue(
        template.contains("start-stop-daemon") || template.contains("nohup"),
        "must background via start-stop-daemon and/or nohup for Type=forking");
    assertTrue(template.contains("start-stop-daemon"), "start-stop-daemon path documented");
    assertTrue(template.contains("nohup"), "nohup fallback path");
    assertTrue(
        template.contains("echo $! >")
            || template.contains("echo $! > ")
            || template.contains("echo \\$! >")
            || template.contains("-m -a")
            || template.contains("-m "),
        "PID must be written on start (start-stop-daemon -m and/or echo $!)");
  }

  @Test
  void template_exposesStartStopRestartForUnitExec() {
    // percussion-cms.service.in: ExecStart=... start / ExecStop=... stop
    assertTrue(template.contains("start)"), "start action");
    assertTrue(template.contains("stop)"), "stop action");
    assertTrue(template.contains("restart)"), "restart action (no ExecReload)");
  }

  @Test
  void template_assembledIntoDistributionDefaultsBin_whenPresent() {
    Path dist = resolveExisting(MODULE_DIST, REPO_DIST);
    // process-resources populates target/distribution before surefire on clean install;
    // skip soft if a focused test-only invocation did not run resource copy.
    if (dist == null) {
      Path expected = Path.of("").toAbsolutePath().normalize().resolve(MODULE_DIST);
      assertTrue(
          !Files.isDirectory(expected.getParent().getParent().getParent()),
          () ->
              "assembly-directory exists but defaults/bin/rxjetty.sh missing: "
                  + expected
                  + " (antrun copies src/main/jetty/**)");
      return;
    }
    assertTrue(Files.isRegularFile(dist), () -> "assembled template missing: " + dist);
  }
}
