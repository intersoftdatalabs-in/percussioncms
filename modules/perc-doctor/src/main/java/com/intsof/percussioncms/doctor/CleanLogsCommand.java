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
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Remove (or inventory) aged log files under known CMS / Jetty / DTS log directories.
 *
 * <p>Target roots (relative to install root; missing dirs skipped):
 *
 * <ul>
 *   <li>{@code jetty/base/logs}
 *   <li>{@code jetty/base/modules/perc-logging/logs}
 *   <li>{@code Deployment/Server/logs}
 * </ul>
 *
 * <p>Only allowlisted log file names ({@link InstallRootGuard#isLogFileName(String)}) under those
 * roots are considered. Safety options:
 *
 * <ul>
 *   <li>{@code --older-than &lt;duration&gt;} — only files with mtime older than the duration
 *   <li>{@code --keep-current} (default true) — never delete identifiable active {@code *.log} /
 *       {@code *.out} files
 * </ul>
 *
 * <p>Dry-run never deletes. Paths outside the install root are never deleted.
 */
public final class CleanLogsCommand {

  /** CLI command token. */
  public static final String COMMAND_NAME = "clean-logs";

  /** Duration token: one or more digits plus unit {@code w|d|h|m|s} (case-insensitive). */
  private static final Pattern DURATION_PATTERN =
      Pattern.compile("^(\\d+)([smhdw])$", Pattern.CASE_INSENSITIVE);

  private CleanLogsCommand() {}

  /**
   * Options for {@link #execute(Path, boolean, Options)}.
   *
   * <p>{@code olderThan} — when non-null, only files with last-modified before {@code now -
   * olderThan}. {@code keepCurrent} — when true, skip identifiable current / active log files.
   */
  public static final class Options {
    private final Duration olderThan;
    private final boolean keepCurrent;

    /**
     * Create clean-logs filter options.
     *
     * @param olderThan optional minimum age; null means no age filter
     * @param keepCurrent retain current active logs (recommended default true)
     */
    public Options(Duration olderThan, boolean keepCurrent) {
      this.olderThan = olderThan;
      this.keepCurrent = keepCurrent;
      if (olderThan != null && (olderThan.isNegative() || olderThan.isZero())) {
        throw new IllegalArgumentException("--older-than must be a positive duration");
      }
    }

    /**
     * Age threshold, or null when unset.
     *
     * @return age threshold, or null when unset
     */
    public Duration getOlderThan() {
      return olderThan;
    }

    /**
     * Whether current active logs are retained.
     *
     * @return whether current active logs are retained
     */
    public boolean isKeepCurrent() {
      return keepCurrent;
    }

    /**
     * Default: no age filter, keep current active logs.
     *
     * @return default options instance
     */
    public static Options defaults() {
      return new Options(null, true);
    }
  }

  /**
   * Inventory log candidates under known log dirs and optionally delete them.
   *
   * @param installRoot resolved install root (must exist)
   * @param dryRun when true, only inventory (no deletes)
   * @param options age / keep-current filters
   * @return report of candidates and actions
   * @throws IOException on walk failures that cannot be recovered
   * @throws IllegalArgumentException if install root or options are invalid
   */
  public static CleanReport execute(Path installRoot, boolean dryRun, Options options)
      throws IOException {
    Objects.requireNonNull(options, "options");
    Path root = InstallRootGuard.requireInstallRoot(installRoot);
    CleanReport report = new CleanReport(COMMAND_NAME, root, dryRun);

    Instant cutoff =
        options.getOlderThan() == null ? null : Instant.now().minus(options.getOlderThan());

    List<Path> candidates = new ArrayList<>();
    for (Path logDir : InstallRootGuard.existingLogDirs(root)) {
      walkLogDir(root, logDir, candidates, report);
    }
    candidates.sort(Comparator.comparing(p -> p.toString()));

    for (Path candidate : candidates) {
      processCandidate(report, root, candidate, dryRun, options.isKeepCurrent(), cutoff);
    }
    return report;
  }

  /**
   * Inventory log candidates under known log dirs without recording walk failures (test helper).
   * Does not apply age / keep-current filters — returns all allowlisted log file names found.
   */
  static List<Path> findLogFiles(Path root) throws IOException {
    Path installRoot = InstallRootGuard.requireInstallRoot(root);
    List<Path> found = new ArrayList<>();
    for (Path logDir : InstallRootGuard.existingLogDirs(installRoot)) {
      walkLogDir(installRoot, logDir, found, null);
    }
    return found;
  }

  /**
   * Walk a single log directory tree. When {@code report} is non-null, I/O failures during the walk
   * are appended as {@link CleanReport.EntryStatus#FAILED}.
   */
  static void walkLogDir(Path installRoot, Path logDir, List<Path> found, CleanReport report)
      throws IOException {
    Objects.requireNonNull(installRoot, "installRoot");
    Objects.requireNonNull(logDir, "logDir");
    Objects.requireNonNull(found, "found");
    if (!InstallRootGuard.isUnderInstallRoot(installRoot, logDir)) {
      return;
    }
    if (!Files.isDirectory(logDir)) {
      return;
    }
    Files.walkFileTree(
        logDir,
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
            Path fileName = file.getFileName();
            if (fileName == null || !InstallRootGuard.isLogFileName(fileName.toString())) {
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
        new CleanReport.Entry(file, 0L, CleanReport.EntryStatus.FAILED, "walk: " + detail));
  }

  private static void processCandidate(
      CleanReport report,
      Path root,
      Path candidate,
      boolean dryRun,
      boolean keepCurrent,
      Instant cutoff) {
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
    if (name == null || !InstallRootGuard.isLogFileName(name.toString())) {
      report.add(
          new CleanReport.Entry(
              candidate, 0L, CleanReport.EntryStatus.SKIPPED, "not an allowlisted log file"));
      return;
    }

    if (keepCurrent && InstallRootGuard.isCurrentLogFileName(name.toString())) {
      long size = sizeOrZero(candidate);
      report.add(
          new CleanReport.Entry(
              candidate, size, CleanReport.EntryStatus.SKIPPED, "keep-current: active log"));
      return;
    }

    if (cutoff != null) {
      Instant mtime;
      try {
        FileTime ft = Files.getLastModifiedTime(candidate);
        mtime = ft.toInstant();
      } catch (IOException e) {
        report.add(
            new CleanReport.Entry(
                candidate, 0L, CleanReport.EntryStatus.FAILED, "mtime: " + e.getMessage()));
        return;
      }
      if (!mtime.isBefore(cutoff)) {
        long size = sizeOrZero(candidate);
        report.add(
            new CleanReport.Entry(
                candidate,
                size,
                CleanReport.EntryStatus.SKIPPED,
                "newer than --older-than cutoff"));
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

  private static long sizeOrZero(Path candidate) {
    try {
      if (Files.isRegularFile(candidate)) {
        return Files.size(candidate);
      }
    } catch (IOException ignored) {
      // best-effort for skip reporting
    }
    return 0L;
  }

  /**
   * Parse a simple duration token for {@code --older-than}.
   *
   * <p>Accepted forms (case-insensitive unit): {@code &lt;digits&gt;&lt;unit&gt;} where unit is
   * {@code s} (seconds), {@code m} (minutes), {@code h} (hours), {@code d} (days), or {@code w}
   * (weeks). Examples: {@code 7d}, {@code 24h}, {@code 30m}, {@code 90s}, {@code 2w}.
   *
   * @param token duration string
   * @return positive {@link Duration}
   * @throws IllegalArgumentException if the token is null, empty, malformed, or non-positive
   */
  public static Duration parseOlderThan(String token) {
    if (token == null || token.isBlank()) {
      throw new IllegalArgumentException(
          "--older-than requires a duration like 7d, 24h, 30m, 90s, or 2w");
    }
    String trimmed = token.trim();
    Matcher m = DURATION_PATTERN.matcher(trimmed);
    if (!m.matches()) {
      throw new IllegalArgumentException(
          "invalid --older-than duration '"
              + token
              + "' (expected <number><s|m|h|d|w>, e.g. 7d)");
    }
    long amount;
    try {
      amount = Long.parseLong(m.group(1));
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("invalid --older-than duration '" + token + "'", e);
    }
    if (amount <= 0L) {
      throw new IllegalArgumentException("--older-than must be a positive duration");
    }
    char unit = m.group(2).toLowerCase(Locale.ROOT).charAt(0);
    switch (unit) {
      case 's':
        return Duration.ofSeconds(amount);
      case 'm':
        return Duration.ofMinutes(amount);
      case 'h':
        return Duration.ofHours(amount);
      case 'd':
        return Duration.ofDays(amount);
      case 'w':
        return Duration.ofDays(Math.multiplyExact(amount, 7L));
      default:
        throw new IllegalArgumentException("invalid --older-than unit in '" + token + "'");
    }
  }
}
