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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.percussion.jetty.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * US1 / T012: structural tests asserting that {@code install-jetty-service.sh} and {@code
 * install-jetty-service.bat} populate the service Java home from the shared resolver / {@code
 * java.properties} instead of relying solely on the legacy operator-provided {@code
 * <installRoot>/JRE} folder.
 */
class InstallJettyServiceJavaHomeTest {

  private static final Path INSTALL_SH =
      Path.of("src", "main", "jetty", "service", "install-jetty-service.sh");
  private static final Path INSTALL_BAT =
      Path.of("src", "main", "jetty", "service", "install-jetty-service.bat");

  @Test
  void installSh_usesResolvedJavaHome() throws Exception {
    assertTrue(Files.isRegularFile(INSTALL_SH), () -> "missing " + INSTALL_SH.toAbsolutePath());
    String s = Files.readString(INSTALL_SH, StandardCharsets.UTF_8);
    assertTrue(
        s.contains("resolve-java-home.sh"),
        "install-jetty-service.sh must call resolve-java-home.sh");
    assertTrue(
        s.contains("RESOLVE_SOURCE"),
        "install-jetty-service.sh must log / use RESOLVE_SOURCE output");
    assertTrue(
        s.contains("JAVA_HOME=") || s.contains("JAVA_HOME:%"),
        "install script writes JAVA_HOME to /etc/default");
  }

  @Test
  void installBat_usesResolvedJavaHome() throws Exception {
    assertTrue(Files.isRegularFile(INSTALL_BAT), () -> "missing " + INSTALL_BAT.toAbsolutePath());
    String s = Files.readString(INSTALL_BAT, StandardCharsets.UTF_8);
    assertTrue(
        s.contains("resolve-java-home.bat"),
        "install-jetty-service.bat must call resolve-java-home.bat");
    assertTrue(
        s.contains("--JavaHome=%JAVA_HOME%"), "Procrun --JavaHome is wired to resolved JAVA_HOME");
    assertTrue(
        s.contains("if errorlevel 1"),
        "install-jetty-service.bat must hard-fail when resolve fails");
  }

  /**
   * Issue #1804: Procrun classpath must not reference JDK {@code lib/tools.jar}. That JAR was
   * removed in JDK 9+; Maven 3.9+ also rejects the legacy {@code com.sun:tools} systemPath
   * dependency. Product runtime is JDK 21.
   */
  @Test
  void installBat_procrunClasspathDoesNotReferenceToolsJar() throws Exception {
    String s = Files.readString(INSTALL_BAT, StandardCharsets.UTF_8);
    assertTrue(s.contains("PR_CLASSPATH="), "Procrun classpath must be set");
    // Ignore REM comments that may mention tools.jar historically; only classpath lines matter.
    boolean classpathHasToolsJar =
        s.lines()
            .map(String::trim)
            .filter(
                line ->
                    line.regionMatches(
                        true, 0, "set PR_CLASSPATH=", 0, "set PR_CLASSPATH=".length()))
            .anyMatch(line -> line.toLowerCase().contains("tools.jar"));
    assertFalse(
        classpathHasToolsJar,
        "install-jetty-service.bat must not put tools.jar on PR_CLASSPATH (JDK 9+)");
    assertTrue(
        s.lines()
            .map(String::trim)
            .anyMatch(
                line -> line.equalsIgnoreCase("set PR_CLASSPATH=\"%JETTY_HOME%\\start.jar\"")),
        "Procrun classpath should be Jetty start.jar only");
  }

  /**
   * Regression for kilo-code-bot PR review thread 3631027608: the resolver must be sourced INTO the
   * install shell, not in a subshell. A subshell isolates the assignments of JAVA_HOME / JAVA /
   * RESOLVE_SOURCE from the resolver and silently discards them, bypassing the documented
   * precedence contract in favor of the legacy JRE/JRE64 fallback.
   */
  @Test
  void installSh_doesNotWrapResolverInSubshell() throws Exception {
    String s = Files.readString(INSTALL_SH, StandardCharsets.UTF_8);
    assertFalse(
        s.contains("(source \"$RESOLVER\""),
        "install-jetty-service.sh must not wrap the resolver in a subshell");
  }

  /**
   * GH-991: service install must hard-fail through resolve-java-home and must not re-introduce a
   * mandatory or soft-fallback {@code <InstallDir>/JRE} requirement after install-time {@code
   * java.properties} selection.
   */
  @Test
  void installSh_hardFailsOnResolveWithoutJreFallback() throws Exception {
    String s = Files.readString(INSTALL_SH, StandardCharsets.UTF_8);
    assertTrue(
        s.contains("source \"$RESOLVER\"") && s.contains("|| exit 1"),
        "install-jetty-service.sh must hard-fail when resolve-java-home fails");
    assertFalse(
        s.contains("falling back to install-dir JRE"),
        "install-jetty-service.sh must not soft-fail into install-dir JRE after resolve failure");
    assertFalse(
        s.contains("Found ${rxDir}/JRE to use as JRE Folder")
            || s.contains("JAVA_HOME=${rxDir}/JRE")
            || s.contains("JAVA_HOME=${rxDir}/JRE64"),
        "install-jetty-service.sh must not assign JAVA_HOME from a hard-coded JRE folder after"
            + " resolve");
  }
}
