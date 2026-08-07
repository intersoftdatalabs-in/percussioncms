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
package com.percussion.distribution.install;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Issue #2220 / parent #2213 slice 5: CMS distribution must ship operator {@code perc-doctor}
 * under {@code bin/} and refresh it on upgrade.
 *
 * <p>Structural packaging assertions (source POM / install.xml / sibling module scripts) — does not
 * require a full distribution assembly when SNAPSHOT deps are missing.
 */
class PercDoctorDistPackagingTest {

  private static final Path POM = Path.of("pom.xml");
  private static final Path INSTALL_XML =
      Path.of("src", "main", "resources", "distribution", "rxconfig", "Installer", "install.xml");
  private static final Path SIBLING_UNIX =
      Path.of("..", "perc-doctor", "src", "main", "scripts", "perc-doctor");
  private static final Path SIBLING_WIN =
      Path.of("..", "perc-doctor", "src", "main", "scripts", "perc-doctor.bat");
  private static final Path SIBLING_ASSEMBLY =
      Path.of("..", "perc-doctor", "src", "main", "assembly", "dist-bin.xml");

  @Test
  @DisplayName("pom declares perc-doctor dist zip and unpacks it into the assembly")
  void pomUnpacksPercDoctorDist() throws Exception {
    assertTrue(Files.isRegularFile(POM), () -> "missing " + POM.toAbsolutePath().normalize());
    String pom = Files.readString(POM, StandardCharsets.UTF_8);

    assertTrue(
        pom.contains("<artifactId>perc-doctor</artifactId>"),
        "pom must depend on perc-doctor (dist packaging)");
    assertTrue(
        pom.contains("<classifier>dist</classifier>") && pom.contains("perc-doctor"),
        "pom must consume perc-doctor classifier dist");
    assertTrue(
        pom.contains("unpack-perc-doctor-dist")
            || (pom.contains("perc-doctor")
                && pom.contains("<type>zip</type>")
                && pom.contains("${assembly-directory}")),
        "pom must unpack perc-doctor dist zip into assembly-directory");
    assertTrue(
        pom.contains("<id>unpack-perc-doctor-dist</id>"),
        "unpack execution id unpack-perc-doctor-dist must exist for lockstep greps");
  }

  @Test
  @DisplayName("install.xml upgrade.overwrite refreshes bin/perc-doctor{.bat,.jar}")
  void installXmlUpgradeOverwritesPercDoctorBin() throws Exception {
    assertTrue(
        Files.isRegularFile(INSTALL_XML), () -> "missing " + INSTALL_XML.toAbsolutePath().normalize());
    String xml = Files.readString(INSTALL_XML, StandardCharsets.UTF_8);

    assertTrue(xml.contains("upgrade.overwrite"), "install.xml must define upgrade.overwrite");
    assertTrue(
        xml.contains("bin/perc-doctor")
            && xml.contains("bin/perc-doctor.bat")
            && xml.contains("bin/perc-doctor.jar"),
        "upgrade.overwrite must include Unix launcher, Windows .bat, and jar (extensionless script)");
    assertFalse(
        xml.contains("<exclude name=\"bin/perc-doctor")
            || xml.contains("<exclude name=\"**/perc-doctor"),
        "must not exclude perc-doctor from install/upgrade packaging");
  }

  @Test
  @DisplayName("sibling perc-doctor module ships scripts + dist assembly (lockstep)")
  void siblingModuleShipsWrappersAndAssembly() {
    assertTrue(
        Files.isRegularFile(SIBLING_UNIX),
        () -> "missing sibling Unix launcher " + SIBLING_UNIX.toAbsolutePath().normalize());
    assertTrue(
        Files.isRegularFile(SIBLING_WIN),
        () -> "missing sibling Windows launcher " + SIBLING_WIN.toAbsolutePath().normalize());
    assertTrue(
        Files.isRegularFile(SIBLING_ASSEMBLY),
        () -> "missing sibling dist assembly " + SIBLING_ASSEMBLY.toAbsolutePath().normalize());
  }
}
