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
import org.junit.jupiter.api.Test;

/**
 * Guards the Tomcat 11 / EE11 Windows service naming alignment for issue #667.
 *
 * <p>Historically installer scripts, Procrun EXEs, and service installers disagreed (tomcat9 vs
 * tomcat10) while runtime {@code tomcat.version} was already on the 11 line. All three must name
 * {@code tomcat11.exe} / {@code tomcat11w.exe}.
 */
class DtsTomcat11WindowsServiceAlignmentTest {

  private static final Path ROOT_FILES = Path.of("src", "main", "rootFiles");

  @Test
  void productionAndStagingServiceBatsReferenceTomcat11Procrun() throws IOException {
    for (String bat : new String[] {"DTSProductionService.bat", "DTSStagingService.bat"}) {
      String text = read(ROOT_FILES.resolve(bat));
      assertTrue(
          text.contains("tomcat11.exe"),
          bat + " must reference tomcat11.exe for Tomcat 11 Procrun");
      assertFalse(text.contains("tomcat9.exe"), bat + " must not reference retired tomcat9.exe");
      assertFalse(text.contains("tomcat10.exe"), bat + " must not reference tomcat10.exe");
    }
  }

  @Test
  void installDtsXmlInstallsTomcat11ProcrunOnWindows() throws IOException {
    Path install = ROOT_FILES.resolve(Path.of("rxconfig", "Installer", "installDts.xml"));
    String text = read(install);
    assertTrue(text.contains("tomcat11.exe"), "installDts.xml must install tomcat11.exe");
    assertTrue(text.contains("tomcat11w.exe"), "installDts.xml must install tomcat11w.exe");
    assertFalse(text.contains("tomcat10.exe"), "installDts.xml must not install tomcat10.exe");
    assertFalse(text.contains("tomcat9.exe"), "installDts.xml must not install tomcat9.exe");
  }

  @Test
  void rootFilesShipTomcat11ProcrunBinariesOnly() {
    assertTrue(Files.isRegularFile(ROOT_FILES.resolve("tomcat11.exe")));
    assertTrue(Files.isRegularFile(ROOT_FILES.resolve("tomcat11w.exe")));
    assertFalse(Files.exists(ROOT_FILES.resolve("tomcat10.exe")));
    assertFalse(Files.exists(ROOT_FILES.resolve("tomcat10w.exe")));
    assertFalse(Files.exists(ROOT_FILES.resolve("tomcat9.exe")));
    assertFalse(Files.exists(ROOT_FILES.resolve("tomcat9w.exe")));
  }

  @Test
  void activeConfOverlayIsTomcat11Tree() {
    assertTrue(Files.isDirectory(Path.of("src", "main", "tomcat11")));
    assertFalse(Files.exists(Path.of("src", "main", "tomcat10")));
    assertFalse(Files.exists(Path.of("src", "main", "tomcat9")));
  }

  private static String read(Path path) throws IOException {
    return Files.readString(path, StandardCharsets.UTF_8);
  }
}
