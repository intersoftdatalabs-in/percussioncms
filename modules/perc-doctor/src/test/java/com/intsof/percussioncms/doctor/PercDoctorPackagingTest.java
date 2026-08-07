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
package com.intsof.percussioncms.doctor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Structural packaging contracts for operator {@code bin/perc-doctor} launchers and the dist
 * assembly (issue #2220 / parent #2213 slice 5).
 *
 * <p>Does not require a built jar; asserts source scripts and Maven assembly wiring stay portable
 * (no hardcoded user homes) and resolve install root relative to the script location.
 */
class PercDoctorPackagingTest {

  private static final Path UNIX_SCRIPT = Path.of("src", "main", "scripts", "perc-doctor");
  private static final Path WINDOWS_SCRIPT = Path.of("src", "main", "scripts", "perc-doctor.bat");
  private static final Path ASSEMBLY = Path.of("src", "main", "assembly", "dist-bin.xml");
  private static final Path POM = Path.of("pom.xml");

  /**
   * Hardcoded user-home shapes that must never appear in operator launchers or docs. Avoid matching
   * instructional prose; focus on absolute personal profile paths and this developer's home.
   */
  private static final Pattern HARDCODED_USER_HOME =
      Pattern.compile("(?i)(C:\\\\Users\\\\[A-Za-z]|/home/[a-zA-Z0-9._-]+|/Users/[A-Za-z]|\\\\Users\\\\Nate)");

  @Test
  @DisplayName("Unix and Windows bin wrappers exist")
  void wrappersExist() {
    assertTrue(Files.isRegularFile(UNIX_SCRIPT), () -> "missing " + UNIX_SCRIPT.toAbsolutePath());
    assertTrue(
        Files.isRegularFile(WINDOWS_SCRIPT), () -> "missing " + WINDOWS_SCRIPT.toAbsolutePath());
  }

  @Test
  @DisplayName("wrappers resolve install root from script directory, not hardcoded homes")
  void wrappersResolveInstallRootPortably() throws Exception {
    String unix = Files.readString(UNIX_SCRIPT, StandardCharsets.UTF_8);
    String win = Files.readString(WINDOWS_SCRIPT, StandardCharsets.UTF_8);

    assertTrue(unix.startsWith("#!/"), "Unix wrapper must be a shell script with shebang");
    assertTrue(
        unix.contains("SCRIPT_DIR") && unix.contains("INSTALL_ROOT"),
        "Unix wrapper must derive SCRIPT_DIR / INSTALL_ROOT");
    assertTrue(
        unix.contains("perc-doctor.jar"),
        "Unix wrapper must invoke stable bin/perc-doctor.jar name");
    assertTrue(
        unix.contains("--install-root"),
        "Unix wrapper must pass --install-root when operator omits it");
    assertFalse(
        HARDCODED_USER_HOME.matcher(unix).find(),
        "Unix wrapper must not hardcode a user home path");

    assertTrue(win.toLowerCase().contains("@echo off"), "Windows wrapper must be a .bat");
    assertTrue(
        win.contains("SCRIPT_DIR") && win.contains("INSTALL_ROOT"),
        "Windows wrapper must derive SCRIPT_DIR / INSTALL_ROOT");
    assertTrue(
        win.contains("perc-doctor.jar"),
        "Windows wrapper must invoke stable bin/perc-doctor.jar name");
    assertTrue(
        win.contains("--install-root"),
        "Windows wrapper must pass --install-root when operator omits it");
    assertFalse(
        HARDCODED_USER_HOME.matcher(win).find(),
        "Windows wrapper must not hardcode a user home path");
  }

  @Test
  @DisplayName("dist assembly packs bin wrappers + versionless perc-doctor.jar")
  void distAssemblyLayout() throws Exception {
    assertTrue(Files.isRegularFile(ASSEMBLY), () -> "missing " + ASSEMBLY.toAbsolutePath());
    String xml = Files.readString(ASSEMBLY, StandardCharsets.UTF_8);
    assertTrue(xml.contains("<id>dist</id>"), "assembly id must be dist");
    assertTrue(xml.contains("perc-doctor.jar"), "assembly must ship perc-doctor.jar");
    assertTrue(xml.contains("destName>perc-doctor.jar"), "stable dest name for operators");
    assertTrue(
        xml.contains("perc-doctor.bat") && xml.contains("<include>perc-doctor</include>"),
        "assembly must include both platform wrappers");
    assertTrue(xml.contains("<outputDirectory>bin</outputDirectory>"), "layout under bin/");
  }

  @Test
  @DisplayName("module pom attaches dist assembly and Main-Class on jar")
  void pomWiresAssemblyAndMainClass() throws Exception {
    assertTrue(Files.isRegularFile(POM), () -> "missing " + POM.toAbsolutePath());
    String pom = Files.readString(POM, StandardCharsets.UTF_8);
    assertTrue(
        pom.contains("com.intsof.percussioncms.doctor.DoctorCli"),
        "jar Main-Class must be DoctorCli");
    assertTrue(
        pom.contains("maven-assembly-plugin") && pom.contains("dist-bin.xml"),
        "pom must run assembly descriptor dist-bin.xml");
    assertTrue(
        pom.contains("<classifier>dist</classifier>")
            || pom.contains("descriptor>src/main/assembly/dist-bin.xml"),
        "dist packaging must be wired for consumers (perc-distribution-tree)");
  }

  @Test
  @DisplayName("operator docs emphasize dry-run-first and portable install-root examples")
  void installGuideDryRunFirst() throws Exception {
    Path guide = Path.of("docs", "operator-install-guide.md");
    Path readme = Path.of("README.md");
    assertTrue(Files.isRegularFile(guide), () -> "missing " + guide.toAbsolutePath());
    assertTrue(Files.isRegularFile(readme), () -> "missing " + readme.toAbsolutePath());

    String guideText = Files.readString(guide, StandardCharsets.UTF_8);
    String readmeText = Files.readString(readme, StandardCharsets.UTF_8);

    for (String text : List.of(guideText, readmeText)) {
      assertFalse(
          HARDCODED_USER_HOME.matcher(text).find(),
          "docs must not hardcode a developer user home path");
    }

    assertTrue(guideText.contains("--dry-run"), "install guide must show --dry-run");
    assertTrue(
        guideText.contains("clean-heap-dumps")
            && guideText.contains("clean-install-backups")
            && guideText.contains("clean-logs"),
        "install guide must cover all shipped commands");
    assertTrue(
        guideText.contains("bin/perc-doctor") || guideText.contains("bin\\perc-doctor"),
        "install guide must document install-tree bin wrappers");
    assertTrue(
        guideText.contains("/opt/") || guideText.contains("C:\\Percussion"),
        "install guide should use generic install-root examples (not a user home)");
  }
}
