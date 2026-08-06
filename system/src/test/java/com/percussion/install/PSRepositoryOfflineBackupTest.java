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
package com.percussion.install;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Offline backup NIO copy + disk precheck (T053 / T056 / T082). */
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
        temp.resolve("rxconfig").resolve("Installer").resolve("rxrepository.properties");
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
        Files.readString(
            backupRoot.resolve("repository-data").resolve("seg0"), StandardCharsets.UTF_8));
  }

  @Test
  void diskSpaceCheckAgainstTemp() throws Exception {
    assertTrue(PSRepositoryOfflineBackup.hasSufficientDiskSpace(temp, 1L));
    assertTrue(!PSRepositoryOfflineBackup.hasSufficientDiskSpace(temp, Long.MAX_VALUE / 2));
  }

  @Test
  void symlinkSourceTreeIsCopied() throws Exception {
    Path real = temp.resolve("realRepo");
    Files.createDirectories(real);
    Files.writeString(real.resolve("seg0"), "via-symlink", StandardCharsets.UTF_8);
    Path link = temp.resolve("linkRepo");
    try {
      Files.createSymbolicLink(link, real);
    } catch (Exception ex) {
      org.junit.jupiter.api.Assumptions.assumeTrue(
          false, "symlinks not supported: " + ex.getMessage());
    }
    Path backupRoot = temp.resolve("backup-symlink");
    PSRepositoryOfflineBackup.Result result =
        PSRepositoryOfflineBackup.copyRepositoryTree(link, backupRoot);
    assertTrue(result.filesCopied() >= 1);
    assertTrue(Files.isRegularFile(backupRoot.resolve("repository-data").resolve("seg0")));
  }

  /**
   * T082 / QC-017: path building uses {@link Path#resolve} (portable separators). Companion dest is
   * file-name only so Windows absolute paths do not leak into backup tree structure.
   */
  @Test
  void pathBuildingUsesPortableResolve_companionIsFilenameOnly() throws Exception {
    Path repo = temp.resolve("install").resolve("Repository");
    Path nested = repo.resolve("seg").resolve("nested");
    Files.createDirectories(nested);
    Files.writeString(nested.resolve("data.bin"), "x", StandardCharsets.UTF_8);

    Path viaMulti = temp.resolve(Path.of("install", "Repository", "seg", "nested"));
    assertEquals(nested.normalize(), viaMulti.normalize());

    Path companion =
        temp.resolve("rxconfig").resolve("Installer").resolve("rxrepository.properties");
    Files.createDirectories(companion.getParent());
    Files.writeString(companion, "DB_BACKEND=DERBY\n", StandardCharsets.UTF_8);

    Path backupRoot = temp.resolve("PreInstall").resolve("migration-backup");
    PSRepositoryOfflineBackup.Result result =
        PSRepositoryOfflineBackup.copyRepositoryTree(repo, backupRoot, companion);

    assertTrue(result.filesCopied() >= 2);
    Path companionDest = backupRoot.resolve("companion-config").resolve("rxrepository.properties");
    assertTrue(Files.isRegularFile(companionDest));
    assertEquals("rxrepository.properties", companionDest.getFileName().toString());
    assertTrue(
        Files.isRegularFile(
            backupRoot
                .resolve("repository-data")
                .resolve("seg")
                .resolve("nested")
                .resolve("data.bin")));
  }

  @Test
  void pathNormalize_relativeSegmentsDoNotEscapeIntendedParent() {
    Path base = temp.resolve("Repository").normalize();
    Path joined = base.resolve("..").resolve("Repository").resolve("CMDB").normalize();
    assertEquals(base.resolve("CMDB").normalize(), joined);
  }

  /** T088 — refuse offline backup when engine lock markers present. */
  @Test
  void refusesWhenLiveLockMarkerPresent() throws Exception {
    Path repo = temp.resolve("liveRepo");
    Files.createDirectories(repo);
    Files.writeString(repo.resolve("seg0"), "data", StandardCharsets.UTF_8);
    Files.writeString(repo.resolve("db.lck"), "lock", StandardCharsets.UTF_8);

    assertFalse(PSRepositoryOfflineBackup.findLiveMarkers(repo).isEmpty());

    Path backupRoot = temp.resolve("backup-live");
    IOException ex =
        assertThrows(
            IOException.class,
            () -> PSRepositoryOfflineBackup.copyRepositoryTree(repo, backupRoot));
    assertTrue(ex.getMessage().toLowerCase().contains("live"), ex.getMessage());

    // Explicit allow when operator forces offline copy despite markers
    PSRepositoryOfflineBackup.Result forced =
        PSRepositoryOfflineBackup.copyRepositoryTree(repo, backupRoot, false);
    assertTrue(forced.filesCopied() >= 1);
    // Lock markers must never be archived — restore would reintroduce startup blockers
    assertFalse(Files.exists(backupRoot.resolve("repository-data").resolve("db.lck")));
    assertTrue(Files.isRegularFile(backupRoot.resolve("repository-data").resolve("seg0")));
  }

  @Test
  void clearStaleLiveMarkersRemovesLocksAndLeavesData() throws Exception {
    Path repo = temp.resolve("staleRepo");
    Files.createDirectories(repo.resolve("nested"));
    Files.writeString(repo.resolve("seg0"), "data", StandardCharsets.UTF_8);
    Files.writeString(repo.resolve("db.lck"), "stale", StandardCharsets.UTF_8);
    Files.writeString(repo.resolve("dbex.lck"), "stale", StandardCharsets.UTF_8);
    Files.writeString(repo.resolve("nested").resolve("other.lck"), "stale", StandardCharsets.UTF_8);

    assertTrue(PSRepositoryOfflineBackup.isLiveMarkerFileName("db.lck"));
    assertTrue(PSRepositoryOfflineBackup.isLiveMarkerFileName("foo.lock.db"));
    assertFalse(PSRepositoryOfflineBackup.isLiveMarkerFileName("seg0"));

    var removed = PSRepositoryOfflineBackup.clearStaleLiveMarkers(repo);
    assertEquals(3, removed.size());
    assertTrue(PSRepositoryOfflineBackup.findLiveMarkers(repo).isEmpty());
    assertTrue(Files.isRegularFile(repo.resolve("seg0")));
    assertFalse(Files.exists(repo.resolve("db.lck")));
    assertFalse(Files.exists(repo.resolve("dbex.lck")));
  }

  @Test
  void clearThenCopyProducesRestorableBackupWithoutLocks() throws Exception {
    Path repo = temp.resolve("offlineRepo");
    Files.createDirectories(repo);
    Files.writeString(repo.resolve("seg0"), "payload", StandardCharsets.UTF_8);
    Files.writeString(repo.resolve("db.lck"), "stale", StandardCharsets.UTF_8);

    PSRepositoryOfflineBackup.clearStaleLiveMarkers(repo);
    Path backupRoot = temp.resolve("backup-clean");
    PSRepositoryOfflineBackup.Result result =
        PSRepositoryOfflineBackup.copyRepositoryTree(repo, backupRoot);
    assertTrue(result.filesCopied() >= 1);
    assertTrue(Files.isRegularFile(backupRoot.resolve("repository-data").resolve("seg0")));
    assertFalse(Files.exists(backupRoot.resolve("repository-data").resolve("db.lck")));
    assertTrue(PSRepositoryOfflineBackup.findLiveMarkers(backupRoot).isEmpty());
  }
}
