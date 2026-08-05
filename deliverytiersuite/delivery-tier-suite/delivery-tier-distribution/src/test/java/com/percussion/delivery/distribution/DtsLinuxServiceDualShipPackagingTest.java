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

package com.percussion.delivery.distribution;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;

/**
 * GH-1978 / GH-962 slice 3: packaging dual-ship proof for DTS Linux service assets.
 *
 * <p>Proves rootFiles ship Production + Staging service installers, shared unit template, and ops
 * README; that the shipping installer jar embeds them; and that {@code installDts.xml}
 * intentionally excludes role scripts from the install-root bulk copy while co-locating the unit
 * template with the role-specific script under {@code Deployment/Server/}.
 *
 * <p><b>Dual-ship policy:</b> keep init.d until live soak + ops review (do not implement #1976
 * here). Live systemctl soak is residual work on #1978.
 */
class DtsLinuxServiceDualShipPackagingTest {

  private static final Path ROOT_FILES = Path.of("src", "main", "rootFiles");

  private static final String[] REQUIRED_ROOT_FILES = {
    "DTSProductionService.sh", "DTSStagingService.sh", "dts-tomcat.service.in", "README-systemd.md",
  };

  @Test
  void rootFiles_shipDualShipLinuxServiceAssets() {
    for (String name : REQUIRED_ROOT_FILES) {
      Path f = ROOT_FILES.resolve(name);
      assertTrue(Files.isRegularFile(f), () -> "missing dual-ship rootFile " + f.toAbsolutePath());
    }
    // Windows installers remain (out of dual-ship soak scope but must not vanish)
    assertTrue(Files.isRegularFile(ROOT_FILES.resolve("DTSProductionService.bat")));
    assertTrue(Files.isRegularFile(ROOT_FILES.resolve("DTSStagingService.bat")));
  }

  @Test
  void scripts_retainInitdFallback_dualShipPolicy() throws IOException {
    for (String name : new String[] {"DTSProductionService.sh", "DTSStagingService.sh"}) {
      String text = Files.readString(ROOT_FILES.resolve(name), StandardCharsets.UTF_8);
      assertTrue(text.contains("--initd"), name + " must keep --initd until soak");
      assertTrue(text.contains("enableSysV") || text.contains("/etc/init.d/"), name + " init.d");
      assertTrue(text.contains("installSystemdUnit"), name + " systemd unit install");
      assertTrue(
          text.contains("dts-tomcat.service.in"),
          name + " must resolve shared unit template next to itself");
    }
  }

  @Test
  void readme_documentsDualShipPolicy() throws IOException {
    String text = Files.readString(ROOT_FILES.resolve("README-systemd.md"), StandardCharsets.UTF_8);
    assertTrue(text.contains("--initd") || text.contains("init.d") || text.contains("SysV"));
    assertTrue(
        text.toLowerCase().contains("dual-ship")
            || text.contains("keep init.d")
            || text.contains("until soak")
            || text.contains("fallback"),
        "README-systemd.md must state dual-ship / keep-init.d policy");
  }

  @Test
  void installDts_excludesRoleScriptsFromRootBulkCopy_andColocatesUnitTemplate() throws Exception {
    Path installDts = ROOT_FILES.resolve(Path.of("rxconfig", "Installer", "installDts.xml"));
    assertTrue(Files.isRegularFile(installDts), () -> "missing " + installDts.toAbsolutePath());
    String xml = Files.readString(installDts, StandardCharsets.UTF_8);

    // Intentional excludes: only one role script is copied into Deployment/Server
    assertTrue(
        xml.contains("<exclude name=\"DTSProductionService.sh\""),
        "installDts must exclude DTSProductionService.sh from install-root * bulk copy");
    assertTrue(
        xml.contains("<exclude name=\"DTSStagingService.sh\""),
        "installDts must exclude DTSStagingService.sh from install-root * bulk copy");
    assertTrue(
        xml.contains("<exclude name=\"DTSProductionService.bat\""),
        "installDts must exclude DTSProductionService.bat from install-root * bulk copy");
    assertTrue(
        xml.contains("<exclude name=\"DTSStagingService.bat\""),
        "installDts must exclude DTSStagingService.bat from install-root * bulk copy");

    // Co-locate unit template with Linux installer under Deployment/Server (GH-1978)
    assertTrue(
        xml.contains("dts-tomcat.service.in"),
        "installDts must reference dts-tomcat.service.in for co-location with service scripts");
    assertTrue(
        xml.contains("<include name=\"dts-tomcat.service.in\""),
        "installDts must explicitly include dts-tomcat.service.in into Deployment/Server on Linux");

    // Role-specific include into Deployment/Server (not both scripts for one role install)
    assertTrue(
        xml.contains("${serviceFileNameLinux}") || xml.contains("serviceFileNameLinux"),
        "installDts must select role-specific Linux service script via serviceFileNameLinux");

    // Must NOT exclude the unit template or README from the root * copy (docs + dual location)
    assertFalse(
        xml.contains("<exclude name=\"dts-tomcat.service.in\""),
        "must not exclude unit template from install-root payload");
    assertFalse(
        xml.contains("<exclude name=\"README-systemd.md\""),
        "must not exclude README-systemd.md from install-root payload");
  }

  @Test
  void shippingJar_embedsDualShipServiceAssetsWhenPresent() {
    Path jar = Path.of("target", "delivery-tier-distribution.jar");
    if (!Files.isRegularFile(jar)) {
      return;
    }

    Set<String> entries = new HashSet<>();
    try (ZipFile zip = new ZipFile(jar.toFile())) {
      zip.stream().map(e -> e.getName().replace('\\', '/')).forEach(entries::add);
    } catch (IOException io) {
      fail("Could not read " + jar + ": " + io.getMessage());
    }

    for (String name : REQUIRED_ROOT_FILES) {
      String path = "distribution/" + name;
      assertTrue(
          entries.contains(path),
          () ->
              "Expected "
                  + path
                  + " in "
                  + jar
                  + " (rootFiles dual-ship; GH-1978). Check antrun populate perc distribution"
                  + " copies src/main/rootFiles.");
    }
  }
}
