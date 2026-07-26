/*
 * Copyright 1999-2026 Percussion Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.
 *
 * See the License for the specific language governing permissions and limitations under the
 * License.
 */
package com.percussion.preinstall;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Guards that fresh-install repository property writes remain behind {@code do.install} so upgrade
 * paths do not rewrite {@code DB_BACKEND} (issue #949 / FR-006).
 */
@Tag("UnitTest")
class RepositoryPropertiesInstallGuardTest {

  @Test
  void repositoryPropertiesTargetIsGatedByDoInstall() throws Exception {
    String xml;
    try (InputStream in =
        getClass().getResourceAsStream("/distribution/rxconfig/Installer/installRepository.xml")) {
      // Resource may live only under main resources — read via filesystem relative to module
      if (in == null) {
        xml =
            java.nio.file.Files.readString(
                java.nio.file.Path.of(
                    "src/main/resources/distribution/rxconfig/Installer/installRepository.xml"),
                StandardCharsets.UTF_8);
      } else {
        xml = new String(in.readAllBytes(), StandardCharsets.UTF_8);
      }
    }

    assertTrue(
        xml.contains("name=\"repository_properties\""), "repository_properties target must exist");
    // Fresh-install apply block must check do.install
    assertTrue(
        xml.contains("<istrue value=\"${do.install}\" />")
            || xml.contains("<istrue value=\"${do.install}\"/>"),
        "repository_properties must gate on do.install");
    // Oracle branch present for new installs
    assertTrue(
        xml.contains("arg2=\"oracle\""), "oracle branch required for new-install db targets");
    // #1500: PostgreSQL silent/matrix installs must rewrite rxrepository (not leave H2 defaults)
    assertTrue(
        xml.contains("arg2=\"postgresql\""),
        "postgresql branch required for new-install db targets");
    // Connection validation wired and excluded for embedded H2/Derby new installs (#548)
    assertTrue(
        xml.contains("PSValidateRepositoryConnection"), "connection validation task must be wired");
    assertTrue(xml.contains("arg2=\"h2\""), "validation must exclude default h2 new installs");
    assertTrue(
        xml.contains("arg2=\"derby\""), "validation must still exclude legacy derby new installs");
  }

  @Test
  void installChainAppliesRepositoryBeforeConfigureJetty() throws Exception {
    String xml =
        java.nio.file.Files.readString(
            java.nio.file.Path.of(
                "src/main/resources/distribution/rxconfig/Installer/install.xml"),
            StandardCharsets.UTF_8);
    int chain = xml.indexOf("<target name=\"install.chain\"");
    assertTrue(chain >= 0, "install.chain target required");
    String chainBody = xml.substring(chain, Math.min(xml.length(), chain + 1200));
    int setupDb = chainBody.indexOf("target=\"setupDB\"");
    int configureJetty = chainBody.indexOf("target=\"configure_jetty\"");
    assertTrue(setupDb >= 0 && configureJetty >= 0, "setupDB and configure_jetty in install.chain");
    assertTrue(
        setupDb < configureJetty,
        "setupDB must precede configure_jetty so external DB props apply before perc-ds");
  }

  @Test
  void upgradeFixtureDocumentsNonDerbyBackend() throws Exception {
    String fixture =
        new String(
            getClass()
                .getResourceAsStream(
                    "/com/percussion/preinstall/upgrade-fixture/rxrepository.properties")
                .readAllBytes(),
            StandardCharsets.UTF_8);
    assertTrue(fixture.contains("DB_BACKEND=MSSQL"));
  }
}
