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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Report (and optionally fix) common install permission / mode issues under the CMS install root
 * ({@code fix-permissions}).
 *
 * <p><strong>Scoped, documented targets only</strong> — no arbitrary paths, no shell, no user
 * globs. Mutations are limited to POSIX mode bits on allowlisted paths when the filesystem supports
 * {@link PosixFilePermission}. On Windows (and other non-POSIX stores) the command reports
 * readability / writability and skips mode changes.
 *
 * <p>Allowlisted relative targets:
 *
 * <ul>
 *   <li>Launchers under {@code bin/}: {@code perc-doctor}, {@code perc-doctor.bat}, {@code
 *       perc-doctor.jar} — ensure owner read (+ execute for non-{@code .bat}/non-{@code .jar} Unix
 *       scripts when present)
 *   <li>Known log directories from {@link InstallRootGuard#LOG_DIR_RELATIVE} — ensure owner
 *       read/write/execute on the directory when present (so the service can write logs)
 *   <li>Key config files ({@link CheckConfigCommand#SERVER_PROPERTIES_REL}, {@link
 *       CheckConfigCommand#RXREPOSITORY_PROPERTIES_REL}) — report owner-read; set owner-read on
 *       POSIX when missing
 * </ul>
 *
 * <p>Dry-run never writes. Apply re-checks containment immediately before each mode change.
 */
public final class FixPermissionsCommand {

  /** CLI command token. */
  public static final String COMMAND_NAME = "fix-permissions";

  /**
   * Allowlisted launcher file names under {@code bin/} (not full paths). Bare names only; no path
   * separators.
   */
  public static final String[] BIN_LAUNCHER_NAMES = {
    "perc-doctor", "perc-doctor.bat", "perc-doctor.jar"
  };

  private FixPermissionsCommand() {}

  /**
   * Inventory and optionally fix permissions under {@code installRoot}.
   *
   * @param installRoot CMS install root (must exist and be a directory)
   * @param dryRun when true, report only (no mode writes)
   * @return report of inspected / fixed paths
   * @throws IllegalArgumentException if install root is invalid
   * @throws IOException on unrecoverable I/O (individual entries prefer FAILED rows)
   */
  public static FixPermissionsReport execute(Path installRoot, boolean dryRun) throws IOException {
    Path root = InstallRootGuard.requireInstallRoot(installRoot);
    FixPermissionsReport report = new FixPermissionsReport(COMMAND_NAME, root, dryRun);
    boolean posix = supportsPosix(root);

    if (!posix) {
      report.add(
          new FixPermissionsReport.Entry(
              root,
              FixPermissionsReport.EntryStatus.SKIPPED,
              "POSIX mode bits not supported on this filesystem; reporting access only"
                  + " (Windows ACL repair is out of scope for this command)"));
    }

    fixBinLaunchers(report, root, dryRun, posix);
    fixLogDirs(report, root, dryRun, posix);
    fixKeyConfigFiles(report, root, dryRun, posix);

    return report;
  }

  static void fixBinLaunchers(
      FixPermissionsReport report, Path root, boolean dryRun, boolean posix) {
    Path binDir = InstallRootGuard.resolveRelativeUnderRoot(root, "bin");
    if (binDir == null || !InstallRootGuard.isUnderInstallRoot(root, binDir)) {
      report.add(
          new FixPermissionsReport.Entry(
              root,
              FixPermissionsReport.EntryStatus.FAILED,
              "bin/ path rejected by install-root guard"));
      return;
    }
    if (!Files.isDirectory(binDir)) {
      report.add(
          new FixPermissionsReport.Entry(
              binDir,
              FixPermissionsReport.EntryStatus.SKIPPED,
              "bin/ directory missing; launcher mode checks skipped"));
      return;
    }

    for (String name : BIN_LAUNCHER_NAMES) {
      if (name.indexOf('/') >= 0 || name.indexOf('\\') >= 0) {
        continue;
      }
      Path file = binDir.resolve(name).toAbsolutePath().normalize();
      if (!InstallRootGuard.isUnderInstallRoot(root, file)) {
        report.add(
            new FixPermissionsReport.Entry(
                file,
                FixPermissionsReport.EntryStatus.FAILED,
                "Launcher path escaped install root"));
        continue;
      }
      if (!Files.isRegularFile(file)) {
        report.add(
            new FixPermissionsReport.Entry(
                file,
                FixPermissionsReport.EntryStatus.SKIPPED,
                "Launcher not present: bin/" + name));
        continue;
      }

      // Report basic accessibility always.
      boolean readable = Files.isReadable(file);
      if (!readable) {
        report.add(
            new FixPermissionsReport.Entry(
                file,
                FixPermissionsReport.EntryStatus.FAILED,
                "Launcher not readable by current user: bin/" + name));
        continue;
      }

      boolean needOwnerExec = posix && isUnixScriptLauncher(name);
      if (!needOwnerExec) {
        report.add(
            new FixPermissionsReport.Entry(
                file,
                FixPermissionsReport.EntryStatus.OK,
                "Launcher present and readable: bin/" + name));
        continue;
      }

      try {
        Set<PosixFilePermission> perms = Files.getPosixFilePermissions(file);
        if (perms.contains(PosixFilePermission.OWNER_EXECUTE)) {
          report.add(
              new FixPermissionsReport.Entry(
                  file,
                  FixPermissionsReport.EntryStatus.OK,
                  "Unix launcher already owner-executable: bin/" + name));
          continue;
        }
        if (dryRun) {
          report.add(
              new FixPermissionsReport.Entry(
                  file,
                  FixPermissionsReport.EntryStatus.WOULD_FIX,
                  "Would add owner-execute on bin/" + name));
          continue;
        }
        // Re-check containment immediately before mutation.
        InstallRootGuard.requireUnderInstallRoot(root, file);
        EnumSet<PosixFilePermission> updated = EnumSet.copyOf(perms);
        updated.add(PosixFilePermission.OWNER_READ);
        updated.add(PosixFilePermission.OWNER_EXECUTE);
        Files.setPosixFilePermissions(file, updated);
        report.add(
            new FixPermissionsReport.Entry(
                file,
                FixPermissionsReport.EntryStatus.FIXED,
                "Added owner-execute on bin/" + name));
      } catch (UnsupportedOperationException e) {
        report.add(
            new FixPermissionsReport.Entry(
                file,
                FixPermissionsReport.EntryStatus.SKIPPED,
                "POSIX permissions unsupported for bin/" + name));
      } catch (IOException | SecurityException e) {
        report.add(
            new FixPermissionsReport.Entry(
                file,
                FixPermissionsReport.EntryStatus.FAILED,
                "Failed to inspect/fix launcher mode: " + e.getMessage()));
      }
    }
  }

  static void fixLogDirs(FixPermissionsReport report, Path root, boolean dryRun, boolean posix) {
    for (String relative : InstallRootGuard.LOG_DIR_RELATIVE) {
      Path dir = InstallRootGuard.resolveRelativeUnderRoot(root, relative);
      if (dir == null) {
        report.add(
            new FixPermissionsReport.Entry(
                root,
                FixPermissionsReport.EntryStatus.FAILED,
                "Invalid log dir relative path: " + relative));
        continue;
      }
      if (!InstallRootGuard.isUnderInstallRoot(root, dir)) {
        report.add(
            new FixPermissionsReport.Entry(
                dir,
                FixPermissionsReport.EntryStatus.FAILED,
                "Log dir path escaped install root: " + relative));
        continue;
      }
      if (!Files.isDirectory(dir)) {
        report.add(
            new FixPermissionsReport.Entry(
                dir,
                FixPermissionsReport.EntryStatus.SKIPPED,
                "Log directory missing: " + relative));
        continue;
      }

      boolean writable = Files.isWritable(dir);
      boolean readable = Files.isReadable(dir);
      if (!posix) {
        FixPermissionsReport.EntryStatus st =
            writable && readable
                ? FixPermissionsReport.EntryStatus.OK
                : FixPermissionsReport.EntryStatus.FAILED;
        report.add(
            new FixPermissionsReport.Entry(
                dir,
                st,
                "Log dir access readable="
                    + readable
                    + " writable="
                    + writable
                    + " (no POSIX mode fix on this platform): "
                    + relative));
        continue;
      }

      try {
        Set<PosixFilePermission> perms = Files.getPosixFilePermissions(dir);
        boolean hasOwnerRwx =
            perms.contains(PosixFilePermission.OWNER_READ)
                && perms.contains(PosixFilePermission.OWNER_WRITE)
                && perms.contains(PosixFilePermission.OWNER_EXECUTE);
        if (hasOwnerRwx && writable) {
          report.add(
              new FixPermissionsReport.Entry(
                  dir,
                  FixPermissionsReport.EntryStatus.OK,
                  "Log dir has owner rwx: " + relative));
          continue;
        }
        if (dryRun) {
          report.add(
              new FixPermissionsReport.Entry(
                  dir,
                  FixPermissionsReport.EntryStatus.WOULD_FIX,
                  "Would ensure owner rwx on log dir: " + relative));
          continue;
        }
        InstallRootGuard.requireUnderInstallRoot(root, dir);
        EnumSet<PosixFilePermission> updated = EnumSet.copyOf(perms);
        updated.add(PosixFilePermission.OWNER_READ);
        updated.add(PosixFilePermission.OWNER_WRITE);
        updated.add(PosixFilePermission.OWNER_EXECUTE);
        Files.setPosixFilePermissions(dir, updated);
        report.add(
            new FixPermissionsReport.Entry(
                dir,
                FixPermissionsReport.EntryStatus.FIXED,
                "Ensured owner rwx on log dir: " + relative));
      } catch (UnsupportedOperationException e) {
        report.add(
            new FixPermissionsReport.Entry(
                dir,
                FixPermissionsReport.EntryStatus.SKIPPED,
                "POSIX permissions unsupported for log dir: " + relative));
      } catch (IOException | SecurityException e) {
        report.add(
            new FixPermissionsReport.Entry(
                dir,
                FixPermissionsReport.EntryStatus.FAILED,
                "Failed to inspect/fix log dir mode (" + relative + "): " + e.getMessage()));
      }
    }
  }

  static void fixKeyConfigFiles(
      FixPermissionsReport report, Path root, boolean dryRun, boolean posix) {
    String[] relatives = {
      CheckConfigCommand.SERVER_PROPERTIES_REL, CheckConfigCommand.RXREPOSITORY_PROPERTIES_REL
    };
    for (String relative : relatives) {
      Path file = InstallRootGuard.resolveRelativeUnderRoot(root, relative);
      if (file == null || !InstallRootGuard.isUnderInstallRoot(root, file)) {
        report.add(
            new FixPermissionsReport.Entry(
                root,
                FixPermissionsReport.EntryStatus.FAILED,
                "Config path rejected by guard: " + relative));
        continue;
      }
      if (!Files.isRegularFile(file)) {
        report.add(
            new FixPermissionsReport.Entry(
                file,
                FixPermissionsReport.EntryStatus.SKIPPED,
                "Config file missing: " + relative));
        continue;
      }
      boolean readable = Files.isReadable(file);
      if (!posix) {
        report.add(
            new FixPermissionsReport.Entry(
                file,
                readable
                    ? FixPermissionsReport.EntryStatus.OK
                    : FixPermissionsReport.EntryStatus.FAILED,
                "Config readable=" + readable + " (no POSIX mode fix): " + relative));
        continue;
      }
      try {
        Set<PosixFilePermission> perms = Files.getPosixFilePermissions(file);
        if (perms.contains(PosixFilePermission.OWNER_READ) && readable) {
          report.add(
              new FixPermissionsReport.Entry(
                  file,
                  FixPermissionsReport.EntryStatus.OK,
                  "Config owner-readable: " + relative));
          continue;
        }
        if (dryRun) {
          report.add(
              new FixPermissionsReport.Entry(
                  file,
                  FixPermissionsReport.EntryStatus.WOULD_FIX,
                  "Would add owner-read on config: " + relative));
          continue;
        }
        InstallRootGuard.requireUnderInstallRoot(root, file);
        EnumSet<PosixFilePermission> updated = EnumSet.copyOf(perms);
        updated.add(PosixFilePermission.OWNER_READ);
        Files.setPosixFilePermissions(file, updated);
        report.add(
            new FixPermissionsReport.Entry(
                file,
                FixPermissionsReport.EntryStatus.FIXED,
                "Added owner-read on config: " + relative));
      } catch (UnsupportedOperationException e) {
        report.add(
            new FixPermissionsReport.Entry(
                file,
                FixPermissionsReport.EntryStatus.SKIPPED,
                "POSIX permissions unsupported for config: " + relative));
      } catch (IOException | SecurityException e) {
        report.add(
            new FixPermissionsReport.Entry(
                file,
                FixPermissionsReport.EntryStatus.FAILED,
                "Failed to inspect/fix config mode (" + relative + "): " + e.getMessage()));
      }
    }
  }

  /**
   * Unix shell launcher (no extension / not Windows batch / not jar) needs owner-execute.
   *
   * @param fileName bare name under bin/
   * @return true when owner-execute should be ensured on POSIX
   */
  static boolean isUnixScriptLauncher(String fileName) {
    Objects.requireNonNull(fileName, "fileName");
    String lower = fileName.toLowerCase(Locale.ROOT);
    if (lower.endsWith(".bat") || lower.endsWith(".cmd") || lower.endsWith(".jar")) {
      return false;
    }
    // perc-doctor (no extension) is the Unix wrapper
    return "perc-doctor".equals(lower) || !lower.contains(".");
  }

  /**
   * Whether {@code path}'s file store supports POSIX permissions (probe via store or attribute
   * view).
   */
  static boolean supportsPosix(Path path) {
    try {
      return path.getFileSystem().supportedFileAttributeViews().contains("posix");
    } catch (RuntimeException e) {
      return false;
    }
  }
}
