/*
 * Copyright 1999-2026 Percussion Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.preinstall;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Packaging defaults for #548: new-install embedded repository is H2, not live Derby (QC-013 /
 * T026). Also asserts the H2 password is non-empty in the install flow (interactive + silent),
 * eliminating the "Wrong user name or password" failure mode (issue #548 / #1500 matrix smoke).
 */
@Tag("UnitTest")
class DefaultEmbeddedH2PackagingTest {

  @Test
  void shippedRxrepositoryDefaultsToH2() throws Exception {
    Path props =
        Path.of("src/main/resources/distribution/rxconfig/Installer/rxrepository.properties");
    String text = Files.readString(props, StandardCharsets.UTF_8);
    assertTrue(text.contains("DB_BACKEND=H2"), text);
    assertTrue(text.contains("DB_DRIVER_NAME=h2"), text);
    assertTrue(text.contains("org.h2.Driver"), text);
    assertFalse(
        text.contains("DB_DRIVER_NAME=derby"),
        "new-install ship props must not default to derby driver");
  }

  @Test
  void installRepositoryDefaultsToH2AndSkipsNetworkServer() throws Exception {
    Path xml = Path.of("src/main/resources/distribution/rxconfig/Installer/installRepository.xml");
    String text = Files.readString(xml, StandardCharsets.UTF_8);
    assertTrue(text.contains("value=\"h2\""), "perc.db.type default must be h2");
    assertFalse(
        text.contains("NetworkServerControl"),
        "distribution installRepository must not start Derby NetworkServer");
    assertFalse(text.contains("port=\"1527\""), "must not wait on Derby DRDA port 1527");
  }

  @Test
  void installRepositoryH2BranchWritesNonEmptyPassword() throws Exception {
    // Silent / non-interactive installs: a random password is generated and
    // persisted to var/config/generated/passwords before rxrepository.properties
    // is rewritten. The XML must reference both PSGenerateRepositoryPassword and
    // the cmdb.password ANT property.
    Path xml = Path.of("src/main/resources/distribution/rxconfig/Installer/installRepository.xml");
    String text = Files.readString(xml, StandardCharsets.UTF_8);
    assertTrue(
        text.contains("PSGenerateRepositoryPassword"),
        "H2 fresh-install must call PSGenerateRepositoryPassword");
    assertTrue(
        text.contains("cmdb.password"),
        "H2 fresh-install must propagate cmdb.password into rxrepository.properties");
    assertTrue(text.contains("PWD_ENCRYPTED"), "PWD_ENCRYPTED must be set explicitly to N for H2");

    // PSMakeLasagna encrypts the password with cwd-derived key material, which can
    // disagree with runtime PathUtils.getRxDir() and produce "Wrong user name or
    // password" on first boot. The H2 branch must guard PSMakeLasagna behind a
    // not-equals-h2 condition so the encryption step is skipped for embedded H2.
    int h2BlockStart = text.indexOf("arg2=\"h2\"");
    int h2BlockEnd = text.indexOf("</if>", h2BlockStart);
    assertTrue(h2BlockStart > -1 && h2BlockEnd > h2BlockStart, "H2 block must be present");
    String h2Block = text.substring(h2BlockStart, h2BlockEnd);
    assertFalse(
        h2Block.contains("PSMakeLasagna"),
        "PSMakeLasagna must NOT appear inside the H2 branch (would re-encrypt the random"
            + " password)");

    // PSMakeLasagna must still exist for non-embedded backends.
    assertTrue(
        text.contains("PSMakeLasagna"),
        "Other backends must still call PSMakeLasagna to encrypt the password");
  }
}
