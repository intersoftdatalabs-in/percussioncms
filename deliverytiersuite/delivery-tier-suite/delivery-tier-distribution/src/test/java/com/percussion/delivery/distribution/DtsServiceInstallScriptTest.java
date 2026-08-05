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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** GH-962: Production and Staging DTS installers prefer systemd, init.d fallback. */
class DtsServiceInstallScriptTest {

  private static final List<Path> SCRIPTS =
      List.of(
          Path.of("src", "main", "rootFiles", "DTSProductionService.sh"),
          Path.of("src", "main", "rootFiles", "DTSStagingService.sh"));

  private static String production;
  private static String staging;

  @BeforeAll
  static void load() throws Exception {
    production = Files.readString(SCRIPTS.get(0), StandardCharsets.UTF_8);
    staging = Files.readString(SCRIPTS.get(1), StandardCharsets.UTF_8);
  }

  @Test
  void bothScripts_detectSystemdAndInstallUnit() {
    for (String script : List.of(production, staging)) {
      assertTrue(script.contains("is_systemd_available"));
      assertTrue(script.contains("/run/systemd/system"));
      assertTrue(script.contains("installSystemdUnit"));
      assertTrue(script.contains("dts-tomcat.service.in"));
      assertTrue(script.contains("substitute_unit_template"));
      assertTrue(script.contains("validate_service_name"));
      assertTrue(script.contains("systemctl enable"));
      assertTrue(script.contains("SysV boot registration skipped"));
      assertTrue(script.contains("had_systemd"));
      assertTrue(script.contains("had_initd"));
    }
  }

  @Test
  void bothScripts_supportInitdForceAndUninstall() {
    for (String script : List.of(production, staging)) {
      assertTrue(script.contains("--initd"));
      assertTrue(script.contains("--systemd"));
      assertTrue(script.contains("removeSystemdUnit"));
      assertTrue(script.contains("disable --now") || script.contains("systemctl disable"));
    }
  }

  @Test
  void bothScripts_requireRoot_noDryRunFlag() {
    // GH-1977: install is root-only; no product --dry-run (docs cover offline review)
    for (String script : List.of(production, staging)) {
      assertTrue(script.contains("id -u"), "root check via id -u");
      assertTrue(
          script.contains("must be run with sudo or as root")
              || script.contains("must be run as root"),
          "root error message");
      assertFalse(script.contains("--dry-run"), "no --dry-run installer flag");
    }
  }

  @Test
  void bothScripts_defaultsFile_isEnvironmentFileSafe() {
    // Must not embed mkdir/chown into /etc/default (breaks EnvironmentFile)
    for (String script : List.of(production, staging)) {
      assertTrue(script.contains("cat > \"/etc/default/${SERVICE_NAME}\""));
      assertFalse(
          script.contains("CATALINA_PID=")
              && script.contains("mkdir -p ${TOMCAT_RUN}")
              && script.indexOf("mkdir -p ${TOMCAT_RUN}")
                  > script.indexOf("cat > \"/etc/default/${SERVICE_NAME}\"")
              && script.indexOf("mkdir -p ${TOMCAT_RUN}")
                  < script.indexOf("EOF", script.indexOf("cat > \"/etc/default/${SERVICE_NAME}\"")),
          "defaults heredoc must not contain mkdir");
    }
  }

  @Test
  void production_defaultServiceName() {
    assertTrue(production.contains("SERVICE_NAME=PercussionProductionDTS"));
  }

  @Test
  void staging_defaultServiceName() {
    assertTrue(staging.contains("SERVICE_NAME=PercussionStagingDTS"));
  }

  /**
   * GH-1984: systemd install resolves the unit template beside the service script ({@code dirname
   * $0}/dts-tomcat.service.in). Scripts are installed under {@code Deployment/Server/}, so the
   * template must not be assumed only at product surface root.
   */
  @Test
  void bothScripts_resolveUnitTemplateBesideScript() {
    for (String script : List.of(production, staging)) {
      assertTrue(
          script.contains(
                  "unit_template=\"$(dirname \"$(abspath \"$0\")\")/dts-tomcat.service.in\"")
              || script.contains(
                  "unit_template=$(dirname \"$(abspath \"$0\")\")/dts-tomcat.service.in"),
          "installSystemdUnit must resolve dts-tomcat.service.in next to the service script");
      assertTrue(
          script.contains("Missing systemd unit template"),
          "must fail closed when unit template is missing");
    }
  }

  /**
   * GH-1984 / inventory Gap B: {@code installDts.xml} must copy {@code dts-tomcat.service.in} (and
   * {@code README-systemd.md}) into {@code Deployment/Server/} on both fresh and upgrade Linux
   * paths, co-located with {@code DTSProductionService.sh} / {@code DTSStagingService.sh}.
   */
  @Test
  void installDts_colocatesSystemdTemplateWithServiceScripts() throws Exception {
    Path installDts =
        Path.of("src", "main", "rootFiles", "rxconfig", "Installer", "installDts.xml");
    assertTrue(Files.isRegularFile(installDts), () -> "missing " + installDts.toAbsolutePath());
    String xml = Files.readString(installDts, StandardCharsets.UTF_8);

    // At least two Linux co-location copies (fresh + upgrade).
    int serviceInIncludes = countOccurrences(xml, "dts-tomcat.service.in");
    assertTrue(
        serviceInIncludes >= 2,
        "installDts.xml must reference dts-tomcat.service.in at least twice (fresh + upgrade); was "
            + serviceInIncludes);

    int readmeIncludes = countOccurrences(xml, "README-systemd.md");
    assertTrue(
        readmeIncludes >= 2,
        "installDts.xml must reference README-systemd.md at least twice (fresh + upgrade); was "
            + readmeIncludes);

    // Each Deployment/Server service-script copy block must also include the unit template.
    int searchFrom = 0;
    int serverCopyBlocks = 0;
    while (true) {
      int copyIdx =
          xml.indexOf("todir=\"${install.dir}${staging.dir}/Deployment/Server\"", searchFrom);
      if (copyIdx < 0) {
        // also match without quotes variation used on upgrade (same attribute form)
        break;
      }
      int filesetEnd = xml.indexOf("</fileset>", copyIdx);
      assertTrue(filesetEnd > copyIdx, "unclosed fileset after Deployment/Server copy");
      String block = xml.substring(copyIdx, filesetEnd);
      if (block.contains("serviceFileNameLinux") || block.contains("${serviceFileNameLinux}")) {
        serverCopyBlocks++;
        assertTrue(
            block.contains("dts-tomcat.service.in"),
            "Deployment/Server service-script copy must include dts-tomcat.service.in (GH-1984)");
        assertTrue(
            block.contains("README-systemd.md"),
            "Deployment/Server service-script copy must include README-systemd.md (GH-1984)");
        assertTrue(
            block.contains("if=\"${isLinux}\""),
            "systemd template co-location must be gated on isLinux");
      }
      searchFrom = filesetEnd + 1;
    }
    assertTrue(
        serverCopyBlocks >= 2,
        "expected >=2 Deployment/Server service-script copy blocks (fresh+upgrade); was "
            + serverCopyBlocks);
  }

  private static int countOccurrences(String haystack, String needle) {
    int count = 0;
    int from = 0;
    while (true) {
      int idx = haystack.indexOf(needle, from);
      if (idx < 0) {
        return count;
      }
      count++;
      from = idx + needle.length();
    }
  }
}
