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
 * Remove (or inventory) Java heap dump files (recursive {@code *.hprof}) under a CMS install root.
 *
 * <p>Safety: only allowlisted {@code .hprof} names; every candidate is re-checked against the
 * install root before delete. Dry-run never deletes.
 */
public final class CleanHeapDumpsCommand {

  /** CLI command token. */
  public static final String COMMAND_NAME = "clean-heap-dumps";

  private CleanHeapDumpsCommand() {}

  /**
   * Inventory recursive {@code *.hprof} files under {@code installRoot} and optionally delete them.
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

    List<Path> candidates = findHeapDumps(root);
    candidates.sort(Comparator.comparing(p -> p.toString()));

    for (Path candidate : candidates) {
      processCandidate(report, root, candidate, dryRun);
    }
    return report;
  }

  static List<Path> findHeapDumps(Path root) throws IOException {
    List<Path> found = new ArrayList<>();
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
            if (fileName == null || !InstallRootGuard.isHeapDumpFileName(fileName.toString())) {
              return FileVisitResult.CONTINUE;
            }
            found.add(file);
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult visitFileFailed(Path file, IOException exc) {
            // Continue walk; failures on individual entries are recorded at process time if needed
            return FileVisitResult.CONTINUE;
          }
        });
    return found;
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
    if (name == null || !InstallRootGuard.isHeapDumpFileName(name.toString())) {
      report.add(
          new CleanReport.Entry(
              candidate, 0L, CleanReport.EntryStatus.SKIPPED, "not an allowlisted .hprof file"));
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
