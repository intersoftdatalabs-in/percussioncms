/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@Tag("UnitTest")
class InstallerUserSettingsTest {

  @TempDir Path tempHome;

  @Test
  void saveProdDoesNotWipeStageOrCms() throws Exception {
    Path prodDir = tempHome.resolve("dts-prod");
    Path stageDir = tempHome.resolve("dts-stage");
    Path cmsDir = tempHome.resolve("cms");
    Files.createDirectories(prodDir);
    Files.createDirectories(stageDir);
    Files.createDirectories(cmsDir);

    new InstallerUserSettings(tempHome, InstallerUserSettings.PREFIX_CMS)
        .save(cmsDir, "8.2.0", Map.of("db.type", "h2"), null);
    new InstallerUserSettings(tempHome, InstallerUserSettings.PREFIX_DTS_STAGE)
        .save(stageDir, "8.2.0", Map.of("db.type", "mysql", "db.host", "stage-db"), null);
    new InstallerUserSettings(tempHome, InstallerUserSettings.PREFIX_DTS_PROD)
        .save(prodDir, "8.2.1", Map.of("db.type", "postgres", "db.host", "prod-db"), null);

    InstallerUserSettings stage =
        new InstallerUserSettings(tempHome, InstallerUserSettings.PREFIX_DTS_STAGE);
    assertEquals(Optional.of("8.2.0"), stage.loadVersion());
    assertEquals("mysql", stage.loadOptionDefaults().get("db.type"));

    InstallerUserSettings cms =
        new InstallerUserSettings(tempHome, InstallerUserSettings.PREFIX_CMS);
    assertEquals(Optional.of("8.2.0"), cms.loadVersion());

    Properties all = new Properties();
    try (var in =
        Files.newInputStream(
            tempHome.resolve(".intsof").resolve("percussion").resolve("last-install.properties"))) {
      all.load(in);
    }
    assertEquals("postgres", all.getProperty("dts.prod.db.type"));
    assertEquals("mysql", all.getProperty("dts.stage.db.type"));
    assertEquals("h2", all.getProperty("cms.db.type"));
  }

  @Test
  void applyDefaultsAndPasswordNeverPersisted() throws Exception {
    Path install = tempHome.resolve("dts");
    Files.createDirectories(install);
    InstallerUserSettings settings =
        new InstallerUserSettings(tempHome, InstallerUserSettings.PREFIX_DTS_PROD);
    settings.save(
        install,
        "8.2.0",
        Map.of("db.type", "mysql", "db.host", "h", "db.password", "SECRET"),
        null);

    MainDTSPreInstall.ParsedArgs filled =
        settings.applyDefaults(new MainDTSPreInstall.ParsedArgs(null, Map.of()));
    assertEquals(install.toAbsolutePath().normalize(), filled.installPath());
    assertEquals("mysql", filled.options().get("db.type"));
    assertFalse(filled.options().containsKey("db.password"));

    String raw =
        Files.readString(
            tempHome.resolve(".intsof").resolve("percussion").resolve("last-install.properties"));
    assertFalse(raw.contains("SECRET"));
    assertFalse(raw.toLowerCase().contains("password"));
  }

  @Test
  void loadAnyDtsPrefersProdThenStage() throws Exception {
    Path stageDir = tempHome.resolve("stage-only");
    Files.createDirectories(stageDir);
    new InstallerUserSettings(tempHome, InstallerUserSettings.PREFIX_DTS_STAGE)
        .save(stageDir, "1.0", Map.of(), null);
    assertEquals(
        stageDir.toAbsolutePath().normalize(),
        InstallerUserSettings.loadAnyDtsInstallDirectory(tempHome).orElseThrow());

    Path prodDir = tempHome.resolve("prod");
    Files.createDirectories(prodDir);
    new InstallerUserSettings(tempHome, InstallerUserSettings.PREFIX_DTS_PROD)
        .save(prodDir, "1.0", Map.of(), null);
    assertEquals(
        prodDir.toAbsolutePath().normalize(),
        InstallerUserSettings.loadAnyDtsInstallDirectory(tempHome).orElseThrow());
  }
}
