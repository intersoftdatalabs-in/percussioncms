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
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

/**
 * Read-only install health checklist for operators ({@code diagnose} / {@code health}).
 *
 * <p>Never deletes or writes. Checks install-root layout markers, free disk space, key config file
 * presence, the running Java version, and known log directory existence. Every resolved path is
 * constrained under the install root via {@link InstallRootGuard}.
 *
 * <p>Global {@code --dry-run} is accepted for CLI parity and echoed on the report; it has no effect
 * because this command is always non-mutating.
 */
public final class DiagnoseCommand {

  /** Primary CLI command token. */
  public static final String COMMAND_NAME = "diagnose";

  /** Alias accepted by the CLI (same behavior as {@link #COMMAND_NAME}). */
  public static final String COMMAND_ALIAS = "health";

  /**
   * Expected layout directories relative to install root (forward-slash segments). Critical when
   * missing → {@link DiagnoseReport.CheckStatus#FAIL}; optional → WARN.
   */
  static final String[] LAYOUT_DIRS_CRITICAL = {
    "jetty",
    "jetty/base",
    "rxconfig"
  };

  /** Optional but common layout dirs (missing → WARN). */
  static final String[] LAYOUT_DIRS_OPTIONAL = {
    "bin",
    "rxconfig/Server",
    "Deployment"
  };

  /**
   * Key config files relative to install root. Missing → WARN (install may be partial or mid-upgrade).
   */
  static final String[] KEY_CONFIG_FILES = {
    "rxconfig/Server/server.properties",
    "rxconfig/Installer/rxrepository.properties"
  };

  /** Free space below this many bytes is reported as WARN (1 GiB). */
  static final long LOW_DISK_BYTES = 1L * 1024 * 1024 * 1024;

  /** CMS product baseline Java major version (informational WARN when lower). */
  static final int EXPECTED_JAVA_MAJOR = 21;

  private DiagnoseCommand() {}

  /**
   * Whether {@code command} is {@code diagnose} or {@code health} (case-sensitive CLI tokens).
   *
   * @param command raw command token
   * @return true when this command should run
   */
  public static boolean isDiagnoseCommand(String command) {
    return COMMAND_NAME.equals(command) || COMMAND_ALIAS.equals(command);
  }

  /**
   * Run the read-only checklist under {@code installRoot}.
   *
   * @param installRoot CMS install root (must exist and be a directory)
   * @param dryRun echoed global flag only; never enables writes
   * @param commandToken token to record on the report ({@code diagnose} or {@code health})
   * @return checklist report
   * @throws IllegalArgumentException if install root is invalid
   * @throws IOException if free-space probe fails in an unrecoverable way (individual checks still
   *     prefer WARN/FAIL rows over throwing when possible)
   */
  public static DiagnoseReport execute(Path installRoot, boolean dryRun, String commandToken)
      throws IOException {
    Path root = InstallRootGuard.requireInstallRoot(installRoot);
    String token =
        commandToken != null && !commandToken.isEmpty() ? commandToken : COMMAND_NAME;
    DiagnoseReport report = new DiagnoseReport(token, root, dryRun);

    // Always-true once requireInstallRoot succeeds — documents the root for operators.
    report.add(
        new DiagnoseReport.Check(
            "install-root",
            DiagnoseReport.CheckStatus.PASS,
            "Install root exists and is a directory",
            root));

    checkLayoutDirs(report, root);
    checkKeyConfigs(report, root);
    checkLogDirs(report, root);
    checkFreeDisk(report, root);
    checkJavaVersion(report);

    return report;
  }

  /**
   * Convenience overload that records {@link #COMMAND_NAME} on the report.
   *
   * @param installRoot CMS install root
   * @param dryRun echoed global flag
   * @return checklist report
   * @throws IOException on unrecoverable I/O
   */
  public static DiagnoseReport execute(Path installRoot, boolean dryRun) throws IOException {
    return execute(installRoot, dryRun, COMMAND_NAME);
  }

  static void checkLayoutDirs(DiagnoseReport report, Path root) {
    for (String relative : LAYOUT_DIRS_CRITICAL) {
      addDirCheck(report, root, relative, true);
    }
    for (String relative : LAYOUT_DIRS_OPTIONAL) {
      addDirCheck(report, root, relative, false);
    }
  }

  static void addDirCheck(
      DiagnoseReport report, Path root, String relativeSlashPath, boolean critical) {
    Path resolved = InstallRootGuard.resolveRelativeUnderRoot(root, relativeSlashPath);
    String id = "layout." + relativeSlashPath.replace('/', '.');
    if (resolved == null) {
      report.add(
          new DiagnoseReport.Check(
              id,
              DiagnoseReport.CheckStatus.FAIL,
              "Invalid relative layout path (path guard rejected): " + relativeSlashPath,
              null));
      return;
    }
    // Containment re-check (defense in depth).
    if (!InstallRootGuard.isUnderInstallRoot(root, resolved)) {
      report.add(
          new DiagnoseReport.Check(
              id,
              DiagnoseReport.CheckStatus.FAIL,
              "Resolved path is outside install root: " + resolved,
              resolved));
      return;
    }
    if (Files.isDirectory(resolved)) {
      report.add(
          new DiagnoseReport.Check(
              id, DiagnoseReport.CheckStatus.PASS, "Directory present: " + relativeSlashPath, resolved));
    } else if (Files.exists(resolved)) {
      report.add(
          new DiagnoseReport.Check(
              id,
              DiagnoseReport.CheckStatus.FAIL,
              "Expected a directory but found a non-directory: " + relativeSlashPath,
              resolved));
    } else {
      report.add(
          new DiagnoseReport.Check(
              id,
              critical ? DiagnoseReport.CheckStatus.FAIL : DiagnoseReport.CheckStatus.WARN,
              "Missing directory: " + relativeSlashPath,
              resolved));
    }
  }

  static void checkKeyConfigs(DiagnoseReport report, Path root) {
    for (String relative : KEY_CONFIG_FILES) {
      Path resolved = InstallRootGuard.resolveRelativeUnderRoot(root, relative);
      String id = "config." + relative.replace('/', '.');
      if (resolved == null) {
        report.add(
            new DiagnoseReport.Check(
                id,
                DiagnoseReport.CheckStatus.FAIL,
                "Invalid relative config path (path guard rejected): " + relative,
                null));
        continue;
      }
      if (!InstallRootGuard.isUnderInstallRoot(root, resolved)) {
        report.add(
            new DiagnoseReport.Check(
                id,
                DiagnoseReport.CheckStatus.FAIL,
                "Resolved config path is outside install root: " + resolved,
                resolved));
        continue;
      }
      if (Files.isRegularFile(resolved)) {
        report.add(
            new DiagnoseReport.Check(
                id, DiagnoseReport.CheckStatus.PASS, "Config file present: " + relative, resolved));
      } else {
        report.add(
            new DiagnoseReport.Check(
                id,
                DiagnoseReport.CheckStatus.WARN,
                "Key config file missing: " + relative,
                resolved));
      }
    }
  }

  static void checkLogDirs(DiagnoseReport report, Path root) {
    for (String relative : InstallRootGuard.LOG_DIR_RELATIVE) {
      Path resolved = InstallRootGuard.resolveRelativeUnderRoot(root, relative);
      String id = "logs." + relative.replace('/', '.');
      if (resolved == null) {
        report.add(
            new DiagnoseReport.Check(
                id,
                DiagnoseReport.CheckStatus.FAIL,
                "Invalid relative log path (path guard rejected): " + relative,
                null));
        continue;
      }
      if (!InstallRootGuard.isUnderInstallRoot(root, resolved)) {
        report.add(
            new DiagnoseReport.Check(
                id,
                DiagnoseReport.CheckStatus.FAIL,
                "Resolved log path is outside install root: " + resolved,
                resolved));
        continue;
      }
      if (Files.isDirectory(resolved)) {
        report.add(
            new DiagnoseReport.Check(
                id, DiagnoseReport.CheckStatus.PASS, "Log directory present: " + relative, resolved));
      } else {
        // Missing log dirs are common on fresh or partial trees — warn, do not fail.
        report.add(
            new DiagnoseReport.Check(
                id,
                DiagnoseReport.CheckStatus.WARN,
                "Known log directory missing: " + relative,
                resolved));
      }
    }
  }

  static void checkFreeDisk(DiagnoseReport report, Path root) {
    try {
      FileStore store = Files.getFileStore(root);
      long usable = store.getUsableSpace();
      long total = store.getTotalSpace();
      String human =
          "usable="
              + formatBytes(usable)
              + " total="
              + formatBytes(total)
              + " store="
              + store.name();
      if (usable < LOW_DISK_BYTES) {
        report.add(
            new DiagnoseReport.Check(
                "disk.free",
                DiagnoseReport.CheckStatus.WARN,
                "Low free disk space (" + human + ")",
                root));
      } else {
        report.add(
            new DiagnoseReport.Check(
                "disk.free",
                DiagnoseReport.CheckStatus.PASS,
                "Adequate free disk space (" + human + ")",
                root));
      }
    } catch (IOException e) {
      String detail = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
      report.add(
          new DiagnoseReport.Check(
              "disk.free",
              DiagnoseReport.CheckStatus.WARN,
              "Could not probe free disk space: " + detail,
              root));
    }
  }

  static void checkJavaVersion(DiagnoseReport report) {
    String version = System.getProperty("java.version", "unknown");
    String home = System.getProperty("java.home", "");
    int major = parseJavaMajor(version);
    String detail =
        "java.version="
            + version
            + (home.isEmpty() ? "" : " java.home=" + home)
            + (major > 0 ? " major=" + major : "");
    report.add(
        new DiagnoseReport.Check(
            "java.version", DiagnoseReport.CheckStatus.INFO, detail, null));
    if (major > 0 && major < EXPECTED_JAVA_MAJOR) {
      report.add(
          new DiagnoseReport.Check(
              "java.major",
              DiagnoseReport.CheckStatus.WARN,
              "Running Java major "
                  + major
                  + " is below CMS baseline "
                  + EXPECTED_JAVA_MAJOR
                  + " (doctor process JVM)",
              null));
    } else if (major >= EXPECTED_JAVA_MAJOR) {
      report.add(
          new DiagnoseReport.Check(
              "java.major",
              DiagnoseReport.CheckStatus.PASS,
              "Running Java major " + major + " meets CMS baseline " + EXPECTED_JAVA_MAJOR,
              null));
    } else {
      report.add(
          new DiagnoseReport.Check(
              "java.major",
              DiagnoseReport.CheckStatus.WARN,
              "Could not parse Java major from version string: " + version,
              null));
    }
  }

  /**
   * Parse the major version from a {@code java.version} string ({@code 1.8.0_xxx} → 8, {@code
   * 21.0.2} → 21). Returns 0 when unparseable.
   *
   * @param version {@code java.version} property value
   * @return major version or 0
   */
  static int parseJavaMajor(String version) {
    if (version == null || version.isEmpty()) {
      return 0;
    }
    String v = version.trim().toLowerCase(Locale.ROOT);
    // Strip common prefixes
    if (v.startsWith("java")) {
      int sp = v.indexOf(' ');
      if (sp > 0) {
        v = v.substring(sp + 1).trim();
      }
    }
    // Legacy 1.x
    if (v.startsWith("1.")) {
      int secondDot = v.indexOf('.', 2);
      String mid = secondDot > 2 ? v.substring(2, secondDot) : v.substring(2);
      return parsePositiveIntPrefix(mid);
    }
    // Modern: 9, 11.0.2, 21.0.1+12
    int end = 0;
    while (end < v.length() && Character.isDigit(v.charAt(end))) {
      end++;
    }
    if (end == 0) {
      return 0;
    }
    return parsePositiveIntPrefix(v.substring(0, end));
  }

  private static int parsePositiveIntPrefix(String s) {
    if (s == null || s.isEmpty()) {
      return 0;
    }
    try {
      int n = Integer.parseInt(s);
      return n > 0 ? n : 0;
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  /** Human-readable byte size for operator output (binary units). */
  static String formatBytes(long bytes) {
    if (bytes < 0) {
      return Long.toString(bytes);
    }
    final String[] units = {"B", "KiB", "MiB", "GiB", "TiB"};
    double v = (double) bytes;
    int u = 0;
    while (v >= 1024.0 && u < units.length - 1) {
      v /= 1024.0;
      u++;
    }
    if (u == 0) {
      return bytes + " B";
    }
    return String.format(Locale.ROOT, "%.1f %s", v, units[u]);
  }

  /**
   * Resolve a relative path under root for tests / callers; package-visible path-guard helper.
   *
   * @param root install root
   * @param relativeSlashPath forward-slash relative path
   * @return resolved path or null if rejected by the path guard
   */
  static Path resolveChecked(Path root, String relativeSlashPath) {
    Objects.requireNonNull(root, "root");
    return InstallRootGuard.resolveRelativeUnderRoot(root, relativeSlashPath);
  }
}
