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
}
