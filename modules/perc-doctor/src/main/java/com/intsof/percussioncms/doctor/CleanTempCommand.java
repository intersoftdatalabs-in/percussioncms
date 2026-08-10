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
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Remove (or inventory) files under known CMS install temp / work directories.
 *
 * <p>Target roots (relative to install root; missing dirs skipped):
 *
 * <ul>
 *   <li>{@code temp}
 *   <li>{@code jetty/base/work}
 *   <li>{@code Deployment/Server/temp}
 *   <li>{@code Deployment/Server/work}
 * </ul>
 *
 * <p>Only files under those allowlisted roots are considered. The allowlisted root directories
 * themselves are never deleted. Dry-run never deletes. Paths outside the install root are never
 * deleted. Prefer stopping the CMS / DTS processes before apply so files are not locked.
 */
public final class CleanTempCommand {

  /** CLI command token. */
  public static final String COMMAND_NAME = "clean-temp";

  private CleanTempCommand() {}

  /**
   * Inventory files under known temp/work dirs and optionally delete them.
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

    List<Path> tempRoots = InstallRootGuard.existingTempDirs(root);
    List<Path> candidates = new ArrayList<>();
    for (Path tempDir : tempRoots) {
      walkTempDir(root, tempDir, candidates, report);
    }
    candidates.sort(Comparator.comparing(p -> p.toString()));

    Set<Path> parentDirs = new HashSet<>();
    for (Path candidate : candidates) {
      processCandidate(report, root, tempRoots, candidate, dryRun);
      Path parent = candidate.getParent();
      if (parent != null) {
        parentDirs.add(parent);
      }
    }

    // Best-effort: remove empty nested dirs after apply (never the allowlisted roots).
    if (!dryRun) {
      removeEmptyNestedDirs(root, tempRoots, parentDirs);
    }
    return report;
  }

  /**
   * Inventory temp files under known temp dirs without recording walk failures (test helper).
   * Returns all regular files found under allowlisted roots.
   */
  static List<Path> findTempFiles(Path root) throws IOException {
    Path installRoot = InstallRootGuard.requireInstallRoot(root);
    List<Path> found = new ArrayList<>();
    for (Path tempDir : InstallRootGuard.existingTempDirs(installRoot)) {
      walkTempDir(installRoot, tempDir, found, null);
    }
    return found;
  }

  /**
   * Walk a single temp/work directory tree. When {@code report} is non-null, I/O failures during
   * the walk are appended as {@link CleanReport.EntryStatus#FAILED}.
   */
  static void walkTempDir(Path installRoot, Path tempDir, List<Path> found, CleanReport report)
      throws IOException {
    Objects.requireNonNull(installRoot, "installRoot");
    Objects.requireNonNull(tempDir, "tempDir");
    Objects.requireNonNull(found, "found");
    if (!InstallRootGuard.isUnderInstallRoot(installRoot, tempDir)) {
      return;
    }
    if (!Files.isDirectory(tempDir)) {
      return;
    }
    Files.walkFileTree(
        tempDir,
        new SimpleFileVisitor<Path>() {
          @Override
          public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
            if (!InstallRootGuard.isUnderInstallRoot(installRoot, dir)) {
              return FileVisitResult.SKIP_SUBTREE;
            }
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            if (!attrs.isRegularFile()) {
              return FileVisitResult.CONTINUE;
            }
            if (!InstallRootGuard.isUnderInstallRoot(installRoot, file)) {
              return FileVisitResult.CONTINUE;
            }
            // Never treat the allowlisted root itself as a file candidate.
            if (tempDir.equals(file.toAbsolutePath().normalize())) {
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
    report.add(new CleanReport.Entry(file, 0L, CleanReport.EntryStatus.FAILED, "walk: " + detail));
  }

  private static void processCandidate(
      CleanReport report, Path root, List<Path> tempRoots, Path candidate, boolean dryRun) {
    Objects.requireNonNull(candidate, "candidate");
    try {
      InstallRootGuard.requireUnderInstallRoot(root, candidate);
    } catch (IllegalArgumentException outside) {
      report.add(
          new CleanReport.Entry(
              candidate, 0L, CleanReport.EntryStatus.FAILED, outside.getMessage()));
      return;
    }

    if (!isUnderAnyTempRoot(tempRoots, candidate)) {
      report.add(
          new CleanReport.Entry(
              candidate,
              0L,
              CleanReport.EntryStatus.SKIPPED,
              "not under an allowlisted temp/work directory"));
      return;
    }

    // Never delete the allowlisted root directories themselves.
    Path normalizedCandidate = candidate.toAbsolutePath().normalize();
    for (Path tempRoot : tempRoots) {
      if (tempRoot.toAbsolutePath().normalize().equals(normalizedCandidate)) {
        report.add(
            new CleanReport.Entry(
                candidate,
                0L,
                CleanReport.EntryStatus.SKIPPED,
                "allowlisted temp/work root is retained"));
        return;
      }
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
      if (!isUnderAnyTempRoot(tempRoots, candidate)) {
        report.add(
            new CleanReport.Entry(
                candidate,
                size,
                CleanReport.EntryStatus.FAILED,
                "path escaped allowlisted temp/work roots before delete"));
        return;
      }
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

  /**
   * True when {@code candidate} is under (or equal to) one of the allowlisted temp roots, all
   * relative to install-root containment already verified by the caller.
   */
  static boolean isUnderAnyTempRoot(List<Path> tempRoots, Path candidate) {
    if (tempRoots == null || candidate == null) {
      return false;
    }
    Path path = candidate.toAbsolutePath().normalize();
    for (Path tempRoot : tempRoots) {
      if (tempRoot == null) {
        continue;
      }
      Path root = tempRoot.toAbsolutePath().normalize();
      if (InstallRootGuard.pathStartsWithRoot(path, root)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Best-effort empty directory cleanup under allowlisted roots. Never removes the allowlisted
   * roots themselves. Failures are ignored (files are what reclaim space).
   */
  static void removeEmptyNestedDirs(Path installRoot, List<Path> tempRoots, Set<Path> seedParents) {
    if (tempRoots == null || tempRoots.isEmpty()) {
      return;
    }
    Set<Path> rootsNormalized = new HashSet<>();
    for (Path r : tempRoots) {
      rootsNormalized.add(r.toAbsolutePath().normalize());
    }

    // Walk deepest first so children empty before parents.
    List<Path> toTry = new ArrayList<>();
    if (seedParents != null) {
      toTry.addAll(seedParents);
    }
    // Also re-scan each temp root for empty nested dirs that had no files.
    for (Path tempRoot : tempRoots) {
      try {
        collectNestedDirs(installRoot, tempRoot, toTry);
      } catch (IOException ignored) {
        // best-effort
      }
    }

    toTry.sort(
        Comparator.comparingInt((Path p) -> p.toAbsolutePath().normalize().getNameCount())
            .reversed()
            .thenComparing(p -> p.toString()));

    for (Path dir : toTry) {
      Path normalized = dir.toAbsolutePath().normalize();
      if (rootsNormalized.contains(normalized)) {
        continue;
      }
      if (!InstallRootGuard.isUnderInstallRoot(installRoot, normalized)) {
        continue;
      }
      if (!isUnderAnyTempRoot(tempRoots, normalized)) {
        continue;
      }
      try {
        if (Files.isDirectory(normalized)) {
          // delete only if empty
          try (var stream = Files.list(normalized)) {
            if (stream.findAny().isEmpty()) {
              Files.deleteIfExists(normalized);
            }
          }
        }
      } catch (IOException ignored) {
        // best-effort
      }
    }
  }

  private static void collectNestedDirs(Path installRoot, Path tempRoot, List<Path> out)
      throws IOException {
    if (!Files.isDirectory(tempRoot)) {
      return;
    }
    Files.walkFileTree(
        tempRoot,
        new SimpleFileVisitor<Path>() {
          @Override
          public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
            if (!InstallRootGuard.isUnderInstallRoot(installRoot, dir)) {
              return FileVisitResult.SKIP_SUBTREE;
            }
            Path normalized = dir.toAbsolutePath().normalize();
            Path rootNorm = tempRoot.toAbsolutePath().normalize();
            if (!normalized.equals(rootNorm)) {
              out.add(normalized);
            }
            return FileVisitResult.CONTINUE;
          }
        });
  }
}
