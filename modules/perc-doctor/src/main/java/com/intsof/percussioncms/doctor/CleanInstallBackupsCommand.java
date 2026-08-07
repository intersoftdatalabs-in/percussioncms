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
package com.intsof.percussioncms.doctor;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Remove (or inventory) allowlisted installer / upgrade backup artifacts under a CMS install root.
 *
 * <p>Allowlist (see {@link InstallRootGuard#isInstallBackupFileName(String)}):
 *
 * <ul>
 *   <li>{@code AppServer_backup_&lt;timestamp&gt;.zip}
 *   <li>any {@code .bak} file under the install tree
 *   <li>any {@code .backup} file under the install tree (includes known {@code
 *       *.properties.backup} such as {@code Navigation.properties.backup})
 * </ul>
 *
 * <p>Safety: only allowlisted names; every candidate is re-checked against the install root before
 * delete. Dry-run never deletes. No arbitrary user-supplied globs.
 */
public final class CleanInstallBackupsCommand {

  /** CLI command token. */
  public static final String COMMAND_NAME = "clean-install-backups";

  private CleanInstallBackupsCommand() {}

  /**
   * Inventory allowlisted install-backup files under {@code installRoot} and optionally delete
   * them.
   *
   * @param installRoot resolved install root (must exist)
   * @param dryRun when true, only inventory (no deletes)
   * @return report of candidates and actions
   * @throws IOException on walk failures that cannot be recovered
   * @throws IllegalArgumentException if install root is invalid
   */
  public static CleanReport execute(Path installRoot, boolean dryRun) throws IOException {
    Path root = InstallRootGuard.requireInstallRoot(installRoot);
    CleanReport report = new CleanReport(COMMAND_NAME, root, dryRun);

    List<Path> candidates = new ArrayList<>();
    walkInstallBackups(root, candidates, report);
    candidates.sort(Comparator.comparing(p -> p.toString()));

    for (Path candidate : candidates) {
      processCandidate(report, root, candidate, dryRun);
    }
    return report;
  }

  /**
   * Inventory allowlisted install backups under {@code root} without recording walk failures (test
   * helper).
   */
  static List<Path> findInstallBackups(Path root) throws IOException {
    List<Path> found = new ArrayList<>();
    walkInstallBackups(root, found, null);
    return found;
  }

  /**
   * Walk install tree for allowlisted install backups. When {@code report} is non-null, I/O
   * failures during the walk are appended as {@link CleanReport.EntryStatus#FAILED} so operators
   * see skipped paths.
   */
  static void walkInstallBackups(Path root, List<Path> found, CleanReport report)
      throws IOException {
    Objects.requireNonNull(root, "root");
    Objects.requireNonNull(found, "found");
    Files.walkFileTree(
        root,
        new SimpleFileVisitor<Path>() {
          @Override
          public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
            // Skip trees that escape the install root (e.g. unexpected symlink targets).
            if (!InstallRootGuard.isUnderInstallRoot(root, dir)) {
              return FileVisitResult.SKIP_SUBTREE;
            }
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            if (!attrs.isRegularFile()) {
              return FileVisitResult.CONTINUE;
            }
            if (!InstallRootGuard.isUnderInstallRoot(root, file)) {
              return FileVisitResult.CONTINUE;
            }
            Path fileName = file.getFileName();
            if (fileName == null
                || !InstallRootGuard.isInstallBackupFileName(fileName.toString())) {
              return FileVisitResult.CONTINUE;
            }
            found.add(file);
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult visitFileFailed(Path file, IOException exc) {
            recordVisitFailure(report, file, exc);
            return FileVisitResult.CONTINUE;
          }
        });
  }

  /** Record a walk I/O failure on the report when one is provided (package-visible for tests). */
  static void recordVisitFailure(CleanReport report, Path file, IOException exc) {
    if (report == null || file == null) {
      return;
    }
    String detail = exc != null ? exc.getMessage() : "unknown I/O error";
    if (detail == null || detail.isBlank()) {
      detail = exc != null ? exc.getClass().getSimpleName() : "unknown I/O error";
    }
    report.add(
        new CleanReport.Entry(
            file, 0L, CleanReport.EntryStatus.FAILED, "walk: " + detail));
  }

  private static void processCandidate(
      CleanReport report, Path root, Path candidate, boolean dryRun) {
    Objects.requireNonNull(candidate, "candidate");
    try {
      InstallRootGuard.requireUnderInstallRoot(root, candidate);
    } catch (IllegalArgumentException outside) {
      report.add(
          new CleanReport.Entry(
              candidate, 0L, CleanReport.EntryStatus.FAILED, outside.getMessage()));
      return;
    }

    Path name = candidate.getFileName();
    if (name == null || !InstallRootGuard.isInstallBackupFileName(name.toString())) {
      report.add(
          new CleanReport.Entry(
              candidate,
              0L,
              CleanReport.EntryStatus.SKIPPED,
              "not an allowlisted install-backup file"));
      return;
    }

    long size = 0L;
    try {
      if (Files.isRegularFile(candidate)) {
        size = Files.size(candidate);
      }
    } catch (IOException e) {
      report.add(
          new CleanReport.Entry(
              candidate, 0L, CleanReport.EntryStatus.FAILED, "size: " + e.getMessage()));
      return;
    }

    if (dryRun) {
      report.add(
          new CleanReport.Entry(candidate, size, CleanReport.EntryStatus.WOULD_DELETE, null));
      return;
    }

    try {
      // Re-check containment immediately before delete (TOCTOU-resistant enough for ops tool).
      InstallRootGuard.requireUnderInstallRoot(root, candidate);
      boolean deleted = Files.deleteIfExists(candidate);
      if (deleted) {
        report.add(new CleanReport.Entry(candidate, size, CleanReport.EntryStatus.DELETED, null));
      } else {
        report.add(
            new CleanReport.Entry(
                candidate, size, CleanReport.EntryStatus.SKIPPED, "already absent"));
      }
    } catch (IOException e) {
      report.add(
          new CleanReport.Entry(
              candidate, size, CleanReport.EntryStatus.FAILED, "delete: " + e.getMessage()));
    } catch (IllegalArgumentException outside) {
      report.add(
          new CleanReport.Entry(
              candidate, size, CleanReport.EntryStatus.FAILED, outside.getMessage()));
    }
  }
}
