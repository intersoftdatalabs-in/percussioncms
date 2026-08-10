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
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Read-only scan of known CMS install/startup logs for ERROR/FATAL/SEVERE and Rhythmyx context
 * death markers ({@code check-logs}).
 *
 * <p>Never deletes or writes. Paths stay under the install root via {@link InstallRootGuard}.
 *
 * <p>Global {@code --dry-run} is accepted for CLI parity and echoed on the report; it has no effect
 * because this command is always non-mutating.
 *
 * <p>Issue: #2556
 */
public final class CheckLogsCommand {

  /** CLI command token. */
  public static final String COMMAND_NAME = "check-logs";

  /** Alias. */
  public static final String COMMAND_ALIAS = "check-startup-logs";

  public static final String PHASE_ALL = "all";
  public static final String PHASE_STARTUP = "startup";
  public static final String PHASE_INSTALL = "install";

  /** Default tail size when reading large log files. */
  public static final int DEFAULT_TAIL_LINES = 4000;

  /** Max absolute bytes to pull when tailing (safety cap). */
  static final long MAX_TAIL_BYTES = 8L * 1024 * 1024;

  /**
   * Startup phase log relative paths (forward-slash segments). First existing path wins per row
   * group when multiple alternates are listed in one {@link LogTarget}.
   */
  static final LogTarget[] STARTUP_LOGS = {
    new LogTarget(
        "log.server",
        "CMS server.log (Jetty Log4j2)",
        true,
        new String[] {"jetty/base/logs/server.log"}),
  };

  /** Install / package / schema logs. */
  static final LogTarget[] INSTALL_LOGS = {
    new LogTarget(
        "log.install-packages",
        "Package install log",
        false,
        new String[] {"rxconfig/Installer/InstallPackages.log", "logs/InstallPackages.log"}),
    new LogTarget(
        "log.install",
        "Installer session log",
        false,
        new String[] {"rxconfig/Installer/install.log"}),
    new LogTarget(
        "log.tablefactory",
        "TableFactory schema/data log",
        false,
        new String[] {"rxconfig/Installer/tablefactory.log", "tablefactory.log"}),
  };

  /** One logical log with alternate relative paths (first existing is scanned). */
  static final class LogTarget {
    final String id;
    final String description;

    /** When phase requires this target and file is missing → FAIL if required flag set. */
    final boolean startupRole;

    final String[] relativePaths;

    LogTarget(String id, String description, boolean startupRole, String[] relativePaths) {
      this.id = id;
      this.description = description;
      this.startupRole = startupRole;
      this.relativePaths = relativePaths;
    }
  }

  /** Command options. */
  public static final class Options {
    private final String phase;
    private final int tailLines;
    private final boolean requireStartup;
    private final boolean requireInstall;

    /**
     * @param phase {@link #PHASE_ALL}, {@link #PHASE_STARTUP}, or {@link #PHASE_INSTALL}
     * @param tailLines max lines to read from the end of each log (minimum 1)
     * @param requireStartup FAIL when no startup server.log present
     * @param requireInstall FAIL when no install-phase log files present
     */
    public Options(String phase, int tailLines, boolean requireStartup, boolean requireInstall) {
      this.phase = phase == null || phase.isEmpty() ? PHASE_ALL : phase.toLowerCase(Locale.ROOT);
      this.tailLines = Math.max(1, tailLines);
      this.requireStartup = requireStartup;
      this.requireInstall = requireInstall;
    }

    public static Options defaults() {
      return new Options(PHASE_ALL, DEFAULT_TAIL_LINES, false, false);
    }

    public String getPhase() {
      return phase;
    }

    public int getTailLines() {
      return tailLines;
    }

    public boolean isRequireStartup() {
      return requireStartup;
    }

    public boolean isRequireInstall() {
      return requireInstall;
    }
  }

  private CheckLogsCommand() {}

  public static boolean isCheckLogsCommand(String command) {
    return COMMAND_NAME.equals(command) || COMMAND_ALIAS.equals(command);
  }

  /**
   * Parse phase token; throws {@link IllegalArgumentException} when unknown.
   *
   * @param raw phase string
   * @return normalized phase
   */
  public static String parsePhase(String raw) {
    if (raw == null || raw.isBlank()) {
      return PHASE_ALL;
    }
    String p = raw.trim().toLowerCase(Locale.ROOT);
    if (PHASE_ALL.equals(p) || PHASE_STARTUP.equals(p) || PHASE_INSTALL.equals(p)) {
      return p;
    }
    throw new IllegalArgumentException(
        "--phase must be all, startup, or install (got: " + raw + ")");
  }

  /**
   * Run log content checks under {@code installRoot}.
   *
   * @param installRoot CMS install root
   * @param dryRun echoed global flag only
   * @param options phase / tail / require flags
   * @return report
   * @throws IllegalArgumentException if install root invalid
   * @throws IOException on unrecoverable I/O (individual files prefer FAIL/SKIP rows)
   */
  public static CheckLogsReport execute(Path installRoot, boolean dryRun, Options options)
      throws IOException {
    Path root = InstallRootGuard.requireInstallRoot(installRoot);
    Options opts = options != null ? options : Options.defaults();
    CheckLogsReport report = new CheckLogsReport(COMMAND_NAME, root, dryRun, opts.getPhase());

    report.add(
        new CheckLogsReport.Check(
            "install-root",
            CheckLogsReport.CheckStatus.PASS,
            "Install root exists and is a directory",
            root,
            null));
    report.add(
        new CheckLogsReport.Check(
            "phase",
            CheckLogsReport.CheckStatus.INFO,
            "Scanning phase="
                + opts.getPhase()
                + " tailLines="
                + opts.getTailLines()
                + " requireStartup="
                + opts.isRequireStartup()
                + " requireInstall="
                + opts.isRequireInstall(),
            null,
            null));

    List<LogTarget> targets = selectTargets(opts.getPhase());
    int startupSeen = 0;
    int installSeen = 0;

    for (LogTarget target : targets) {
      ScanOutcome outcome = scanTarget(root, target, opts.getTailLines());
      report.add(outcome.check);
      if (outcome.filePresent) {
        if (target.startupRole) {
          startupSeen++;
        } else {
          installSeen++;
        }
      }
    }

    if (opts.isRequireStartup()
        && (PHASE_ALL.equals(opts.getPhase()) || PHASE_STARTUP.equals(opts.getPhase()))
        && startupSeen == 0) {
      report.add(
          new CheckLogsReport.Check(
              "require.startup",
              CheckLogsReport.CheckStatus.FAIL,
              "require-startup: no jetty/base/logs/server.log found under install root",
              null,
              "missing_server_log"));
    }
    if (opts.isRequireInstall()
        && (PHASE_ALL.equals(opts.getPhase()) || PHASE_INSTALL.equals(opts.getPhase()))
        && installSeen == 0) {
      report.add(
          new CheckLogsReport.Check(
              "require.install",
              CheckLogsReport.CheckStatus.FAIL,
              "require-install: no install/package/tablefactory logs found under install root",
              null,
              "missing_install_logs"));
    }

    return report;
  }

  public static CheckLogsReport execute(Path installRoot, boolean dryRun) throws IOException {
    return execute(installRoot, dryRun, Options.defaults());
  }

  static List<LogTarget> selectTargets(String phase) {
    List<LogTarget> list = new ArrayList<>();
    if (PHASE_ALL.equals(phase) || PHASE_STARTUP.equals(phase)) {
      for (LogTarget t : STARTUP_LOGS) {
        list.add(t);
      }
    }
    if (PHASE_ALL.equals(phase) || PHASE_INSTALL.equals(phase)) {
      for (LogTarget t : INSTALL_LOGS) {
        list.add(t);
      }
    }
    return list;
  }

  private static final class ScanOutcome {
    final CheckLogsReport.Check check;
    final boolean filePresent;

    ScanOutcome(CheckLogsReport.Check check, boolean filePresent) {
      this.check = check;
      this.filePresent = filePresent;
    }
  }

  static ScanOutcome scanTarget(Path root, LogTarget target, int tailLines) {
    Path found = null;
    for (String rel : target.relativePaths) {
      Path resolved = InstallRootGuard.resolveRelativeUnderRoot(root, rel);
      if (resolved != null && Files.isRegularFile(resolved)) {
        found = resolved;
        break;
      }
    }
    if (found == null) {
      String tried = String.join(", ", target.relativePaths);
      return new ScanOutcome(
          new CheckLogsReport.Check(
              target.id + ".present",
              CheckLogsReport.CheckStatus.SKIP,
              target.description + " not present (tried: " + tried + ")",
              null,
              null),
          false);
    }

    try {
      String text = readTail(found, tailLines);
      String match = LogScanRules.findStartupError(text);
      if (match != null) {
        return new ScanOutcome(
            new CheckLogsReport.Check(
                target.id + ".content",
                CheckLogsReport.CheckStatus.FAIL,
                target.description + " contains ERROR/FATAL/SEVERE or context-failure marker",
                found,
                match),
            true);
      }
      return new ScanOutcome(
          new CheckLogsReport.Check(
              target.id + ".content",
              CheckLogsReport.CheckStatus.PASS,
              target.description + " clean (no ERROR/FATAL/SEVERE or context markers in tail)",
              found,
              null),
          true);
    } catch (IOException e) {
      return new ScanOutcome(
          new CheckLogsReport.Check(
              target.id + ".content",
              CheckLogsReport.CheckStatus.FAIL,
              target.description + " unreadable: " + e.getMessage(),
              found,
              "unreadable"),
          true);
    }
  }

  /**
   * Read the last {@code tailLines} lines of {@code file} (UTF-8, falls back to platform default on
   * decode issues by reading bytes as ISO-8859-1).
   */
  static String readTail(Path file, int tailLines) throws IOException {
    Objects.requireNonNull(file, "file");
    long size = Files.size(file);
    if (size == 0) {
      return "";
    }
    if (size <= MAX_TAIL_BYTES && size <= 512 * 1024) {
      // Small files: read whole content.
      return Files.readString(file, StandardCharsets.UTF_8);
    }
    // Tail from the end using RandomAccessFile (cross-platform).
    int linesWanted = Math.max(1, tailLines);
    long maxBytes = Math.min(size, MAX_TAIL_BYTES);
    try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "r")) {
      long start = Math.max(0, size - maxBytes);
      raf.seek(start);
      long toRead = size - start;
      if (toRead > Integer.MAX_VALUE) {
        toRead = Integer.MAX_VALUE;
      }
      byte[] buf = new byte[(int) toRead];
      int read = raf.read(buf);
      if (read <= 0) {
        return "";
      }
      String chunk = decodeLenient(buf, read);
      if (start > 0) {
        // Drop partial first line when we mid-file seeked.
        int nl = chunk.indexOf('\n');
        if (nl >= 0 && nl + 1 < chunk.length()) {
          chunk = chunk.substring(nl + 1);
        }
      }
      return lastNLines(chunk, linesWanted);
    }
  }

  static String decodeLenient(byte[] buf, int len) {
    String utf8 = new String(buf, 0, len, StandardCharsets.UTF_8);
    // If the file is pure ASCII/UTF-8 this is fine; for legacy encodings operators still get
    // searchable ERROR tokens.
    if (utf8.indexOf('\uFFFD') < 0) {
      return utf8;
    }
    return new String(buf, 0, len, Charset.forName("ISO-8859-1"));
  }

  static String lastNLines(String text, int n) {
    if (text == null || text.isEmpty() || n <= 0) {
      return text == null ? "" : text;
    }
    String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
    String[] lines = normalized.split("\n", -1);
    if (lines.length <= n) {
      return normalized;
    }
    StringBuilder sb = new StringBuilder();
    for (int i = lines.length - n; i < lines.length; i++) {
      if (sb.length() > 0) {
        sb.append('\n');
      }
      sb.append(lines[i]);
    }
    return sb.toString();
  }
}
