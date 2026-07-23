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

package com.percussion.delivery.distribution;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * US2 / T020-T021: structural tests asserting that DTS rootFiles scripts
 * share the same Java home resolution contract as CMS Jetty:
 * {@code TomcatStartup.*}, {@code TomcatShutdown.*}, {@code DTSProductionService.*},
 * {@code DTSStagingService.*}, and the local {@code resolve-java-home.*} helper
 * copies.
 */
class DtsJavaHomeScriptTest {

  private static final Path RESOLVE_SH =
      Path.of("src", "main", "rootFiles", "resolve-java-home.sh");
  private static final Path RESOLVE_BAT =
      Path.of("src", "main", "rootFiles", "resolve-java-home.bat");
  private static final Path TOMCAT_START_SH =
      Path.of("src", "main", "rootFiles", "TomcatStartup.sh");
  private static final Path TOMCAT_START_BAT =
      Path.of("src", "main", "rootFiles", "TomcatStartup.bat");
  private static final Path TOMCAT_STOP_SH =
      Path.of("src", "main", "rootFiles", "TomcatShutdown.sh");
  private static final Path TOMCAT_STOP_BAT =
      Path.of("src", "main", "rootFiles", "TomcatShutdown.bat");
  private static final Path PROD_SH =
      Path.of("src", "main", "rootFiles", "DTSProductionService.sh");
  private static final Path STAGING_SH =
      Path.of("src", "main", "rootFiles", "DTSStagingService.sh");
  private static final Path PROD_BAT =
      Path.of("src", "main", "rootFiles", "DTSProductionService.bat");
  private static final Path STAGING_BAT =
      Path.of("src", "main", "rootFiles", "DTSStagingService.bat");

  @Test
  void dtsResolverShMirrorsJettyContract() throws Exception {
    assertTrue(Files.isRegularFile(RESOLVE_SH), () -> "missing " + RESOLVE_SH.toAbsolutePath());
    String s = Files.readString(RESOLVE_SH, StandardCharsets.UTF_8);
    assertTrue(s.startsWith("#!/bin/bash"), "shebang required");
    assertTrue(s.contains("PRODUCT_CONFIG"));
    assertTrue(s.contains("PROCESS_ENV"));
    assertTrue(s.contains("INSTALL_DIR_JRE"));
    assertTrue(s.contains("INSTALL_DIR_JRE64"));
    assertTrue(s.contains("REQUIRED_MAJOR=21"), "minimum major is 21");
    assertTrue(
        s.contains("-ge \"$REQUIRED_MAJOR\"") || s.contains("-ge $REQUIRED_MAJOR"),
        "sh accepts major >= 21 (21+), not equality-only");
    assertTrue(s.contains("Required Java major version"));
    assertTrue(s.contains("or later"), "failure message states 21 or later");
  }

  @Test
  void dtsResolverBatMirrorsJettyContract() throws Exception {
    assertTrue(Files.isRegularFile(RESOLVE_BAT), () -> "missing " + RESOLVE_BAT.toAbsolutePath());
    String s = Files.readString(RESOLVE_BAT, StandardCharsets.UTF_8);
    assertTrue(s.startsWith("@echo off"), "echo off header");
    assertTrue(s.contains("PRODUCT_CONFIG"));
    assertTrue(s.contains("PROCESS_ENV"));
    assertTrue(s.contains("INSTALL_DIR_JRE"));
    assertTrue(s.contains("REQUIRED_MAJOR=21"), "minimum major is 21");
    assertTrue(s.contains("GEQ %REQUIRED_MAJOR%"),
        "bat accepts major >= 21 (21+), not equality-only");
    assertTrue(s.contains("or later"), "failure message states 21 or later");
    assertTrue(s.contains("exit /b 1"));
  }

  /**
   * Regression for kilo-code-bot PR review thread 3631027615:
   * the DTS copy of resolve-java-home.bat must also be ASCII-safe so it
   * renders correctly under Windows cmd.exe's default OEM code page.
   */
  @Test
  void dtsResolverBatIsAsciiSafe() throws Exception {
    String s = Files.readString(RESOLVE_BAT, StandardCharsets.UTF_8);
    for (int line = 1, i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (c == '\n') {
        line++;
        continue;
      }
      if (c > 0x7E || (c < 0x20 && c != '\t' && c != '\r' && c != '\n')) {
        assertTrue(
            false,
            "DTS resolve-java-home.bat contains non-ASCII char U+"
                + String.format("%04X", (int) c)
                + " on line "
                + line);
      }
    }
  }

  @Test
  void tomcatStartupUsesResolverOnBothPlatforms() throws Exception {
    String sh = Files.readString(TOMCAT_START_SH, StandardCharsets.UTF_8);
    String bat = Files.readString(TOMCAT_START_BAT, StandardCharsets.UTF_8);
    assertTrue(sh.contains("resolve-java-home.sh"),
        "TomcatStartup.sh sources resolve-java-home.sh");
    assertTrue(bat.contains("resolve-java-home.bat"),
        "TomcatStartup.bat calls resolve-java-home.bat");
    // Must not *only* fall back to a hard-coded ../JRE.
    assertFalse(sh.contains("cd JRE") || sh.contains("cd ../JRE"),
        "TomcatStartup.sh must not cd-only into JRE");
    assertFalse(bat.contains("SET JAVA_HOME=%SCRIPT_DIR%\\JRE"),
        "TomcatStartup.bat must not hard-code only JRE");
  }

  @Test
  void tomcatShutdownUsesResolverOnBothPlatforms() throws Exception {
    String sh = Files.readString(TOMCAT_STOP_SH, StandardCharsets.UTF_8);
    String bat = Files.readString(TOMCAT_STOP_BAT, StandardCharsets.UTF_8);
    assertTrue(sh.contains("resolve-java-home.sh"));
    assertTrue(bat.contains("resolve-java-home.bat"));
    assertFalse(sh.contains("cd JRE") || sh.contains("cd ../JRE"));
    assertFalse(bat.contains("SET JAVA_HOME=%SCRIPT_DIR%\\JRE"),
        "TomcatShutdown.bat must not hard-code only JRE");
  }

  @Test
  void dtsProductionServiceUsesResolver() throws Exception {
    String sh = Files.readString(PROD_SH, StandardCharsets.UTF_8);
    String bat = Files.readString(PROD_BAT, StandardCharsets.UTF_8);
    assertTrue(sh.contains("resolve-java-home.sh"),
        "DTSProductionService.sh sources resolve-java-home.sh");
    assertTrue(bat.contains("resolve-java-home.bat"),
        "DTSProductionService.bat calls resolve-java-home.bat");
    assertTrue(bat.contains("--JavaHome=%JRE_HOME%"),
        "Procrun --JavaHome is wired to resolved Java home");
  }

  @Test
  void dtsProductionServiceShDoesNotWrapResolverInSubshell() throws Exception {
    // Regression for kilo-code-bot PR review thread 3631027580: a subshell
    // `(source ...)` would scope JAVA_HOME / JAVA / RESOLVE_SOURCE inside
    // the subshell and the legacy JRE / JRE64 fallback would silently win.
    String sh = Files.readString(PROD_SH, StandardCharsets.UTF_8);
    assertFalse(
        sh.contains("(source \"$RESOLVER\""),
        "DTSProductionService.sh must not wrap the resolver in a subshell");
  }

  @Test
  void dtsStagingServiceUsesResolver() throws Exception {
    String sh = Files.readString(STAGING_SH, StandardCharsets.UTF_8);
    String bat = Files.readString(STAGING_BAT, StandardCharsets.UTF_8);
    assertTrue(sh.contains("resolve-java-home.sh"),
        "DTSStagingService.sh sources resolve-java-home.sh");
    assertTrue(bat.contains("resolve-java-home.bat"),
        "DTSStagingService.bat calls resolve-java-home.bat");
    assertTrue(bat.contains("--JavaHome=%JRE_HOME%"),
        "Procrun --JavaHome is wired to resolved Java home");
  }

  /**
   * Regression for issue #1475: the ciphers {@code <replace>} in
   * installDts.xml must be gated on an {@code <available>} check for
   * {@code Deployment/Server/conf/server.xml}, otherwise fresh installs
   * (where the file does not yet exist) crash at line 1059 with
   * "Replace: source file ... doesn't exist". Without the guard, the
   * entire install aborts.
   */
  @Test
  void installDtsCiphersReplaceIsGatedOnServerXmlPresence() throws Exception {
    Path script =
        Path.of(
            "src",
            "main",
            "rootFiles",
            "rxconfig",
            "Installer",
            "installDts.xml");
    assertTrue(
        Files.isRegularFile(script), () -> "missing " + script.toAbsolutePath());
    String xml = Files.readString(script, StandardCharsets.UTF_8);

    // Find the "Adding Ciphers to connector..." block and assert the
    // immediately-following <if> contains an <available> guard for
    // server.xml.
    int ciphersIdx = xml.indexOf("Adding Ciphers to connector");
    assertTrue(ciphersIdx >= 0, "'Adding Ciphers to connector...' block missing");
    int ifIdx = xml.indexOf("<if>", ciphersIdx);
    assertTrue(ifIdx > ciphersIdx, "no <if> after ciphers block");
    int thenIdx = xml.indexOf("<then>", ifIdx);
    assertTrue(thenIdx > ifIdx, "no <then> after ciphers <if>");
    String block = xml.substring(ifIdx, thenIdx);
    assertTrue(
        block.contains("<available file=\"${install.dir}${staging.dir}/Deployment/Server/conf/server.xml\""),
        "ciphers <if> block must guard on <available ...server.xml ...>; "
            + "without it, fresh installs (no server.xml yet) crash at line 1059. "
            + "See issue #1475.");
  }

  @Test
  void dtsStagingServiceShDoesNotWrapResolverInSubshell() throws Exception {
    // Regression for kilo-code-bot PR review thread 3631027600.
    String sh = Files.readString(STAGING_SH, StandardCharsets.UTF_8);
    assertFalse(
        sh.contains("(source \"$RESOLVER\""),
        "DTSStagingService.sh must not wrap the resolver in a subshell");
  }
}
