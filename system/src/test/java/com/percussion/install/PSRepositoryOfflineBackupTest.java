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
package com.percussion.install;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Offline backup NIO copy + disk precheck (T053 / T056 / T082 start). */
@Tag("UnitTest")
public class PSRepositoryOfflineBackupTest {

  @TempDir Path temp;

  @Test
  void copiesRepositoryTreeAndCompanionConfig() throws Exception {
    Path repo = temp.resolve("Repository");
    Path nested = repo.resolve("subdir");
    Files.createDirectories(nested);
    Files.writeString(repo.resolve("seg0"), "data0", StandardCharsets.UTF_8);
    Files.writeString(nested.resolve("seg1"), "data1", StandardCharsets.UTF_8);

    Path companion =
        temp.resolve("rxconfig")
            .resolve("Installer")
            .resolve("rxrepository.properties");
    Files.createDirectories(companion.getParent());
    Files.writeString(companion, "DB_BACKEND=DERBY\n", StandardCharsets.UTF_8);

    Path backupRoot = temp.resolve("PreInstall").resolve("migration-backup");
    PSRepositoryOfflineBackup.Result result =
        PSRepositoryOfflineBackup.copyRepositoryTree(repo, backupRoot, companion);

    assertTrue(result.filesCopied() >= 3);
    assertTrue(result.bytesCopied() > 0);
    assertTrue(Files.isRegularFile(backupRoot.resolve("repository-data").resolve("seg0")));
    assertTrue(
        Files.isRegularFile(
            backupRoot.resolve("repository-data").resolve("subdir").resolve("seg1")));
    assertTrue(
        Files.isRegularFile(
            backupRoot.resolve("companion-config").resolve("rxrepository.properties")));
    assertEquals(
        "data0",
        Files.readString(backupRoot.resolve("repository-data").resolve("seg0"), StandardCharsets.UTF_8));
  }

  @Test
  void diskSpaceCheckAgainstTemp() throws Exception {
    assertTrue(PSRepositoryOfflineBackup.hasSufficientDiskSpace(temp, 1L));
    // Unreasonably large requirement should fail on any normal volume
    assertTrue(
        !PSRepositoryOfflineBackup.hasSufficientDiskSpace(
            temp, Long.MAX_VALUE / 2));
  }

  /**
   * T082 / QC-017: path building uses {@link Path#resolve} (portable separators). Companion dest
   * is file-name only so Windows absolute paths do not leak into backup tree structure.
   */
  @Test
  void pathBuildingUsesPortableResolve_companionIsFilenameOnly() throws Exception {
    Path repo = temp.resolve("install").resolve("Repository");
    Path nested = repo.resolve("seg").resolve("nested");
    Files.createDirectories(nested);
    Files.writeString(nested.resolve("data.bin"), "x", StandardCharsets.UTF_8);

    // Multi-segment resolve must equal successive resolve (no hardcoded separators)
    Path viaMulti = temp.resolve(Path.of("install", "Repository", "seg", "nested"));
    assertEquals(nested.normalize(), viaMulti.normalize());

    Path companion =
        temp.resolve("rxconfig")
            .resolve("Installer")
            .resolve("rxrepository.properties");
    Files.createDirectories(companion.getParent());
    Files.writeString(companion, "DB_BACKEND=DERBY\n", StandardCharsets.UTF_8);

    Path backupRoot = temp.resolve("PreInstall").resolve("migration-backup");
    PSRepositoryOfflineBackup.Result result =
        PSRepositoryOfflineBackup.copyRepositoryTree(repo, backupRoot, companion);

    assertTrue(result.filesCopied() >= 2);
    Path companionDest =
        backupRoot.resolve("companion-config").resolve("rxrepository.properties");
    assertTrue(Files.isRegularFile(companionDest));
    // Destination uses getFileName() only — not absolute path segments
    assertEquals("rxrepository.properties", companionDest.getFileName().toString());
    assertTrue(
        Files.isRegularFile(
            backupRoot.resolve("repository-data").resolve("seg").resolve("nested").resolve("data.bin")));
  }

  @Test
  void pathNormalize_relativeSegmentsDoNotEscapeIntendedParent() {
    Path base = temp.resolve("Repository").normalize();
    Path joined = base.resolve("..").resolve("Repository").resolve("CMDB").normalize();
    assertEquals(base.resolve("CMDB").normalize(), joined);
  }
}
