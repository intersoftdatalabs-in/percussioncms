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

package com.percussion.preinstall;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * US6 / T045: structural assertion that the {@code install.xml} Ant install script soft-gates the
 * JRE-directory-dependent tasks so the upgrade path does not fail when the operator has stopped
 * using {@code <InstallDir>/JRE} or {@code <InstallDir>/JRE64}.
 *
 * <p>The script already wraps the JRE/lib/ext fileset block in {@code failonerror="false"}; this
 * test makes that contract explicit so a future change does not silently re-introduce a hard
 * failure.
 */
class InstallXmlJreSoftGateTest {

  private static final Path INSTALL_XML =
      Path.of("src", "main", "resources", "distribution", "rxconfig", "Installer", "install.xml");

  @Test
  void installXmlSoftGatesJreBackupAndLibExt() throws Exception {
    assertTrue(Files.isRegularFile(INSTALL_XML), () -> "missing " + INSTALL_XML.toAbsolutePath());
    String xml = Files.readString(INSTALL_XML, StandardCharsets.UTF_8);

    // deleteOldBouncyCastleJars: must be a failonerror=false block that
    // references JRE/lib/ext and JRE64/lib/ext. Operators without those
    // folders do not have the upgrade blow up.
    int deleteTarget = xml.indexOf("name=\"deleteOldBouncyCastleJars\"");
    assertTrue(deleteTarget > 0, "deleteOldBouncyCastleJars target missing");
    int afterBlock = Math.min(xml.length(), deleteTarget + 4000);
    String block = xml.substring(deleteTarget, afterBlock);
    assertTrue(
        block.contains("failonerror=\"false\""),
        "deleteOldBouncyCastleJars must be soft-gated with failonerror=false");
    assertTrue(
        block.contains("${install.dir}/JRE/lib/ext"),
        "deleteOldBouncyCastleJars must scan JRE/lib/ext");
    assertTrue(
        block.contains("${install.dir}/JRE64/lib/ext"),
        "deleteOldBouncyCastleJars must scan JRE64/lib/ext");
  }
}
