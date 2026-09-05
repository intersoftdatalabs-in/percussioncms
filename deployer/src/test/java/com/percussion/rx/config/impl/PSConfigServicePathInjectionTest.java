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
package com.percussion.rx.config.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.rx.config.IPSConfigService.ConfigTypes;
import com.percussion.utils.io.PathUtils;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Path-injection barrier for package configuration files (CodeQL {@code java/path-injection}
 * #2039–#2044).
 */
@DisplayName("PSConfigService.getConfigFile path-injection barrier (CWE-22, #2039)")
class PSConfigServicePathInjectionTest {

  @TempDir Path tmp;

  @AfterEach
  void clearThreadRxDir() {
    PathUtils.unsetThreadOnlyRxDir(tmp.toFile());
  }

  @Test
  void getConfigFile_rejectsParentTraversalAndSeparators() {
    PathUtils.setThreadOnlyRxDir(tmp.toFile());
    PSConfigService svc = new PSConfigService();
    assertThrows(
        IllegalArgumentException.class,
        () -> svc.getConfigFile(ConfigTypes.LOCAL_CONFIG, ".." + File.separator + "escape"));
    assertThrows(
        IllegalArgumentException.class,
        () -> svc.getConfigFile(ConfigTypes.LOCAL_CONFIG, "a/b"));
    assertThrows(
        IllegalArgumentException.class, () -> svc.getConfigFile(ConfigTypes.LOCAL_CONFIG, ".."));
  }

  @Test
  void getConfigFile_resolvesUnderRxConfigPackages() throws Exception {
    File rx = tmp.toFile();
    PathUtils.setThreadOnlyRxDir(rx);
    PSConfigService svc = new PSConfigService();
    File resolved = svc.getConfigFile(ConfigTypes.LOCAL_CONFIG, "MyPkg");
    Path expected =
        tmp.resolve("rxconfig")
            .resolve("Packages")
            .resolve("LocalConfigs")
            .resolve("MyPkg_localConfig.xml");
    assertEquals(expected.toFile().getCanonicalFile(), resolved.getCanonicalFile());
    assertTrue(
        resolved.getCanonicalFile().toPath().startsWith(rx.getCanonicalFile().toPath()));
  }

  @Test
  void requireConfigFileUnderRxDir_rejectsEscape() throws Exception {
    PathUtils.setThreadOnlyRxDir(tmp.toFile());
    File outside = tmp.getParent().resolve("escape-config.xml").toFile();
    Files.writeString(outside.toPath(), "<cfg/>");
    assertThrows(
        IllegalArgumentException.class,
        () -> PSConfigService.requireConfigFileUnderRxDir(outside));

    File child = tmp.resolve("ok.xml").toFile();
    Files.writeString(child.toPath(), "<ok/>");
    File safe = PSConfigService.requireConfigFileUnderRxDir(child);
    assertEquals(child.getCanonicalFile(), safe.getCanonicalFile());
  }
}
