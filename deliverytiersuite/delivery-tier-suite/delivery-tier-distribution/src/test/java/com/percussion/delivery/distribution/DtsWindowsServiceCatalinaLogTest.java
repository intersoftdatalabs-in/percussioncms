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

package com.percussion.delivery.distribution;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Guards issue #938: Windows DTS Procrun service install must write console output to {@code
 * logs/catalina.log} (parity with Linux {@code CATALINA_OUT}) and wire Log4j JUL correctly.
 *
 * <p>Historically installers used {@code PR_STDOUTPUT=auto} (creates {@code
 * service-stdout.YYYY-MM-DD.log}) and pointed JUL {@code ClassLoaderLogManager} at Log4j2 XML via
 * {@code java.util.logging.config.file}, so {@code catalina.log} never updated under the Windows
 * service.
 */
class DtsWindowsServiceCatalinaLogTest {

  private static final Path ROOT_FILES = Path.of("src", "main", "rootFiles");

  private static final List<String> SERVICE_BATS =
      List.of("DTSProductionService.bat", "DTSStagingService.bat");

  private static String production;
  private static String staging;

  @BeforeAll
  static void load() throws IOException {
    production = read(ROOT_FILES.resolve("DTSProductionService.bat"));
    staging = read(ROOT_FILES.resolve("DTSStagingService.bat"));
  }

  @Test
  void bothBats_redirectStdoutStderrToCatalinaLog() {
    for (String bat : List.of(production, staging)) {
      assertTrue(
          bat.contains("logs\\catalina.log") || bat.contains("logs/catalina.log"),
          "service bat must reference logs\\catalina.log for Procrun StdOutput/StdError");
      assertTrue(
          bat.contains("--StdOutput=") || bat.contains("PR_STDOUTPUT="),
          "must set Procrun StdOutput");
      assertTrue(
          bat.contains("--StdError=") || bat.contains("PR_STDERROR="), "must set Procrun StdError");
      assertTrue(
          bat.contains("PR_STDOUTPUT=%CATALINA_BASE%\\logs\\catalina.log")
              || bat.contains("--StdOutput=\"%CATALINA_BASE%\\logs\\catalina.log\""),
          "StdOutput must be catalina.log under CATALINA_BASE\\logs (not procrun auto)");
      assertFalse(
          bat.matches("(?s).*PR_STDOUTPUT\\s*=\\s*auto.*")
              || bat.contains("--StdOutput=auto")
              || bat.contains("--StdOutput auto"),
          "must not use Procrun StdOutput=auto (that creates dated service-stdout logs)");
      assertFalse(
          bat.matches("(?s).*PR_STDERROR\\s*=\\s*auto.*")
              || bat.contains("--StdError=auto")
              || bat.contains("--StdError auto"),
          "must not use Procrun StdError=auto");
    }
  }

  @Test
  void bothBats_useLog4jJulManagerAndConfigurationFile() {
    for (String bat : List.of(production, staging)) {
      assertTrue(
          bat.contains("java.util.logging.manager=org.apache.logging.log4j.jul.LogManager"),
          "service JVM options must use Log4j JUL bridge (matches setenv.bat)");
      assertTrue(
          bat.contains("log4j.configurationFile=") && bat.contains("log4j2-tomcat.xml"),
          "must set log4j.configurationFile to log4j2-tomcat.xml");
      assertFalse(
          bat.contains("org.apache.juli.ClassLoaderLogManager"),
          "must not use ClassLoaderLogManager against Log4j2 XML");
      assertFalse(
          bat.contains("java.util.logging.config.file=") && bat.contains("log4j2-tomcat.xml"),
          "must not pass log4j2-tomcat.xml as java.util.logging.config.file");
    }
  }

  @Test
  void bothBats_keepLog4jOnProcrunClasspath() {
    for (String bat : List.of(production, staging)) {
      assertTrue(
          bat.contains("log4j2/lib/*") && bat.contains("log4j2/conf"),
          "Procrun classpath must include log4j2/lib and log4j2/conf");
    }
  }

  @Test
  void linuxServiceScripts_setCatalinaOutToCatalinaLog() throws IOException {
    for (String sh : new String[] {"DTSProductionService.sh", "DTSStagingService.sh"}) {
      String text = read(ROOT_FILES.resolve(sh));
      assertTrue(
          text.contains("CATALINA_OUT=${CATALINA_HOME}/logs/catalina.log"),
          sh + " must set CATALINA_OUT to logs/catalina.log (Linux parity baseline)");
    }
  }

  @Test
  void log4j2TomcatConfig_writesCatalinaLogUnderCatalinaBase() throws IOException {
    String xml = read(Path.of("src", "main", "tomcat11", "conf", "log4j2-tomcat.xml"));
    assertTrue(xml.contains("${logdir}/catalina.log") || xml.contains("catalina.log"));
    assertTrue(xml.contains("${sys:catalina.base}/logs") || xml.contains("logdir"));
  }

  private static String read(Path path) throws IOException {
    assertTrue(Files.isRegularFile(path), () -> "missing " + path.toAbsolutePath());
    return Files.readString(path, StandardCharsets.UTF_8);
  }
}
