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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * US1 / T012: structural tests asserting that {@code install-jetty-service.sh}
 * and {@code install-jetty-service.bat} populate the service Java home from the
 * shared resolver / {@code java.properties} instead of relying solely on the
 * legacy operator-provided {@code <installRoot>/JRE} folder.
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
    assertTrue(s.contains("resolve-java-home.sh"),
        "install-jetty-service.sh must call resolve-java-home.sh");
    assertTrue(s.contains("RESOLVE_SOURCE"),
        "install-jetty-service.sh must log / use RESOLVE_SOURCE output");
    assertTrue(s.contains("JAVA_HOME=") || s.contains("JAVA_HOME:%"),
        "install script writes JAVA_HOME to /etc/default");
  }

  @Test
  void installBat_usesResolvedJavaHome() throws Exception {
    assertTrue(Files.isRegularFile(INSTALL_BAT), () -> "missing " + INSTALL_BAT.toAbsolutePath());
    String s = Files.readString(INSTALL_BAT, StandardCharsets.UTF_8);
    assertTrue(s.contains("resolve-java-home.bat"),
        "install-jetty-service.bat must call resolve-java-home.bat");
    assertTrue(s.contains("--JavaHome=%JAVA_HOME%"),
        "Procrun --JavaHome is wired to resolved JAVA_HOME");
  }
}
