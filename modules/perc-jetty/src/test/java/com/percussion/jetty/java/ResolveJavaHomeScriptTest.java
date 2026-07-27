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

package com.percussion.jetty.java;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * Structural / contract-marker tests for the {@code resolve-java-home} sh and bat scripts shipped
 * alongside the Jetty scripts. These enforce the dual-script contract from
 * specs/991-system-java-home/contracts/java-home-resolution.md without trying to execute the
 * scripts (which would require a Java 21+ install matching the project's runtime contract).
 */
class ResolveJavaHomeScriptTest {

  private static final Path SH = Path.of("src", "main", "jetty", "resolve-java-home.sh");
  private static final Path BAT = Path.of("src", "main", "jetty", "resolve-java-home.bat");

  @Test
  void shScriptExistsAndIsSourcedStyle() throws Exception {
    assertTrue(Files.isRegularFile(SH), () -> "missing " + SH.toAbsolutePath());
    String s = Files.readString(SH, StandardCharsets.UTF_8);
    assertTrue(s.startsWith("#!/bin/bash"), "shebang required");
    assertTrue(s.contains("PRODUCT_CONFIG"), "contains PRODUCT_CONFIG source label");
    assertTrue(s.contains("PROCESS_ENV"), "contains PROCESS_ENV source label");
    assertTrue(s.contains("INSTALL_DIR_JRE"), "contains INSTALL_DIR_JRE source label");
    assertTrue(s.contains("INSTALL_DIR_JRE64"), "contains INSTALL_DIR_JRE64 source label");
    assertTrue(s.contains("PATH"), "PATH source label");
    assertTrue(s.contains("REQUIRED_MAJOR=21"), "REQUIRED_MAJOR minimum is 21");
    assertTrue(
        s.contains("-ge \"$REQUIRED_MAJOR\"") || s.contains("-ge $REQUIRED_MAJOR"),
        "sh accepts major >= REQUIRED_MAJOR (21+), not equality-only");
    assertTrue(s.contains("exit 1") || s.contains("return 1"), "failure path exits non-zero");
    assertTrue(
        s.contains("Required Java major version"),
        "failure message includes required major version label");
    assertTrue(s.contains("or later"), "failure message states 21 or later");
  }

  @Test
  void batScriptExistsAndMirrorsContract() throws Exception {
    assertTrue(Files.isRegularFile(BAT), () -> "missing " + BAT.toAbsolutePath());
    String s = Files.readString(BAT, StandardCharsets.UTF_8);
    assertTrue(s.startsWith("@echo off"), "echo off header");
    assertTrue(s.contains("PRODUCT_CONFIG"), "contains PRODUCT_CONFIG source label");
    assertTrue(s.contains("PROCESS_ENV"), "contains PROCESS_ENV source label");
    assertTrue(s.contains("INSTALL_DIR_JRE"), "contains INSTALL_DIR_JRE source label");
    assertTrue(s.contains("REQUIRED_MAJOR=21"), "REQUIRED_MAJOR minimum is 21");
    assertTrue(
        s.contains("GEQ %REQUIRED_MAJOR%"),
        "bat accepts major >= REQUIRED_MAJOR (21+), not equality-only");
    assertTrue(
        s.contains("Required Java major version"),
        "failure message includes required major version label");
    assertTrue(s.contains("or later"), "failure message states 21 or later");
    assertTrue(s.contains("exit /b 1"), "bat exits with /b 1 on failure");
  }

  /**
   * Regression for kilo-code-bot PR review thread 3631027615: non-ASCII em-dashes render as Latin-1
   * mojibake under Windows cmd.exe's default OEM code page. The script must be ASCII-only in
   * comments so it ships portably across code pages.
   */
  @Test
  void batScriptIsAsciiSafe() throws Exception {
    String s = Files.readString(BAT, StandardCharsets.UTF_8);
    for (int line = 1, i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (c == '\n') {
        line++;
        continue;
      }
      if (c > 0x7E || (c < 0x20 && c != '\t' && c != '\r' && c != '\n')) {
        assertTrue(
            false,
            "resolve-java-home.bat contains non-ASCII char U+"
                + String.format("%04X", (int) c)
                + " on line "
                + line
                + " (Windows cmd.exe OEM code page renders as mojibake)");
      }
    }
  }

  /**
   * Regression for SHIFT-command parsing trap: the bat was originally edited with a comment
   * marker that began with the literal token {@code SHIFT } followed by another keyword. cmd.exe
   * parses {@code SHIFT /n} and {@code SHIFT REM ...} as commands (not comments), which prints
   * "Invalid parameter to SHIFT command" on startup before the script reaches the launcher. The
   * bat must not contain a {@code SHIFT} keyword anywhere outside a true SHIFT command (which
   * the bat does not need).
   */
  @Test
  void batScriptDoesNotInvokeShiftCommand() throws Exception {
    String s = Files.readString(BAT, StandardCharsets.UTF_8);
    // Match the SHIFT keyword only when followed by whitespace and another token (i.e. a real
    // SHIFT invocation), or followed by '/' (SHIFT /n switch). REPLACE_ME-style SHIFT foo
    // comments must not appear.
    java.util.regex.Pattern p =
        java.util.regex.Pattern.compile("(?im)^\\s*SHIFT\\s+[^/\\s]|\\bSHIFT\\s+/");
    java.util.regex.Matcher m = p.matcher(s);
    assertFalse(
        m.find(),
        "resolve-java-home.bat must not contain SHIFT command invocations"
            + " (cmd.exe parses 'SHIFT foo' as a command, not a comment). Offending match: "
            + (m.find() ? m.group() : ""));
  }

  @Test
  void shPrefersConfigOverEnvMarker() throws Exception {
    String s = Files.readString(SH, StandardCharsets.UTF_8);
    // The _config_source invocation precedes _env_source in the precedence chain.
    int configIdx = s.indexOf("_config_source");
    int envIdx = s.indexOf("_env_source");
    int legacyIdx = s.indexOf("_legacy_source");
    int pathIdx = s.indexOf("_path_source");
    assertTrue(
        configIdx > 0 && envIdx > 0 && legacyIdx > 0 && pathIdx > 0, "all four sources referenced");
    assertTrue(
        configIdx < envIdx && envIdx < legacyIdx && legacyIdx < pathIdx,
        "precedence order: config > env > legacy > PATH");
  }

  @Test
  void shResolveReadsJavaPropertiesKeys() throws Exception {
    String s = Files.readString(SH, StandardCharsets.UTF_8);
    assertTrue(s.contains("JAVA_HOME"), "reads JAVA_HOME key");
    assertTrue(s.contains("LAUNCHER"), "infers launcher via LAUNCHER variable");
  }

  @Test
  void batPrefersConfigOverEnvMarker() throws Exception {
    String s = Files.readString(BAT, StandardCharsets.UTF_8);
    int cfg = s.indexOf(":try_config");
    int env = s.indexOf(":try_env");
    int legacy = s.indexOf(":try_legacy");
    int path = s.indexOf(":try_path");
    assertTrue(cfg > 0 && env > 0 && legacy > 0 && path > 0, "all four sources referenced in bat");
    assertTrue(
        cfg < env && env < legacy && legacy < path,
        "bat precedence order: config > env > legacy > PATH");
  }

  @Test
  void scriptsDoNotEmbedHardCodedInstallDirJreOnly() throws Exception {
    String sh = Files.readString(SH, StandardCharsets.UTF_8);
    String bat = Files.readString(BAT, StandardCharsets.UTF_8);
    // Guardrail: must not *only* accept <installRoot>/JRE (no other source).
    assertFalse(
        sh.contains("JAVA_HOME=$INSTALL_ROOT/JRE") && !sh.contains("_env_source"),
        "sh must not hard-code only JRE");
    assertFalse(
        bat.contains("set JAVA_HOME=%INSTALL_ROOT%\\JRE") && !bat.contains(":try_env"),
        "bat must not hard-code only JRE");
  }

  // ----- US1 wiring assertions (T011) -----

  private static final Path START_SH = Path.of("src", "main", "jetty", "StartJetty.sh");
  private static final Path START_BAT = Path.of("src", "main", "jetty", "StartJetty.bat");
  private static final Path STOP_BAT = Path.of("src", "main", "jetty", "StopJetty.bat");

  @Test
  void startJettySh_sourcesResolveHelper() throws Exception {
    String s = Files.readString(START_SH, StandardCharsets.UTF_8);
    assertTrue(
        s.contains("source \"${DIR}/resolve-java-home.sh\"")
            || s.contains("source ./resolve-java-home.sh")
            || s.contains("resolve-java-home.sh"),
        "StartJetty.sh must source resolve-java-home.sh");
  }

  @Test
  void startJettyBat_callsResolveHelper() throws Exception {
    String s = Files.readString(START_BAT, StandardCharsets.UTF_8);
    assertTrue(
        s.contains("resolve-java-home.bat"), "StartJetty.bat must call resolve-java-home.bat");
  }

  @Test
  void stopJettyBat_callsResolveHelper() throws Exception {
    String s = Files.readString(STOP_BAT, StandardCharsets.UTF_8);
    assertTrue(
        s.contains("resolve-java-home.bat"), "StopJetty.bat must call resolve-java-home.bat");
  }

  @Test
  void startStopScriptsDoNotHardCodeOnlyJre() throws Exception {
    String startSh = Files.readString(START_SH, StandardCharsets.UTF_8);
    String startBat = Files.readString(START_BAT, StandardCharsets.UTF_8);
    String stopBat = Files.readString(STOP_BAT, StandardCharsets.UTF_8);
    assertFalse(
        startSh.contains("JAVA_HOME=${rxDir}/JRE"),
        "StartJetty.sh must not hard-code only ${rxDir}/JRE");
    assertFalse(
        startBat.contains("SET JAVA_HOME=%rxDir%\\JRE"),
        "StartJetty.bat must not hard-code only %rxDir%\\JRE");
    assertFalse(
        stopBat.contains("SET JAVA_HOME=%rxDir%\\JRE"),
        "StopJetty.bat must not hard-code only %rxDir%\\JRE");
  }

  /**
   * Regression: Properties.store() escapes {@code \} as {@code \\} and {@code :} as {@code \:} in
   * values. The bat script reads values with {@code for /f "tokens=1,2 delims=="} which preserves
   * those escapes verbatim, so a Windows path like {@code C:\Program Files\jdk-21} round-trips
   * as {@code C\:\\Program Files\\jdk-21}. Jetty's start.jar then treats the {@code C:} colon as a
   * {@code --module} arg separator and refuses to launch ("launcher missing: C\:\..."). The bat
   * must unescape {@code \\}, {@code \:} and {@code \=} before validating the path. See issue
   * surfaced during interactive-install smoke test (cmd.exe + Microsoft JDK in {@code Program
   * Files}).
   */
  @Test
  void batScriptUnescapesPropertiesStoreEscapes() throws Exception {
    String s = Files.readString(BAT, StandardCharsets.UTF_8);
    // The unescape must run inside :try_config after for /f parses the line.
    // `call set` with %%VAR:SEARCH=REPLACE%% is the cmd.exe idiom for replacing
    // special characters; Properties.store() escapes backslash, colon, and
    // equals inside property values, so all three need to be reversed before the
    // path is fed to the launcher arg.
    int tryConfigIdx = s.indexOf(":try_config");
    assertTrue(tryConfigIdx > 0, ":try_config block must exist");
    int afterTryConfig = s.indexOf("for /f", tryConfigIdx);
    assertTrue(afterTryConfig > 0, "for /f must appear inside :try_config");

    int backslashIdx = s.indexOf("VAL:", afterTryConfig);
    assertTrue(backslashIdx > 0, "bat must declare VAL inside :try_config");
    // Read a wide window so all three `call set` unescape lines land in the
    // substring check.
    String valBlock = s.substring(backslashIdx, Math.min(s.length(), backslashIdx + 1000));
    assertTrue(
        valBlock.contains("VAL:\\\\=\\"),
        "bat must unescape \\\\ in property values (call set VAL=%%VAL:\\\\=\\\\%%); was:\n"
            + valBlock);
    assertTrue(
        valBlock.contains("VAL:\\^:=:") || valBlock.contains("VAL:\\:=:"),
        "bat must unescape \\: in property values");
    assertTrue(
        valBlock.contains("VAL:\\^=="),
        "bat must unescape \\= in property values");
  }
}
