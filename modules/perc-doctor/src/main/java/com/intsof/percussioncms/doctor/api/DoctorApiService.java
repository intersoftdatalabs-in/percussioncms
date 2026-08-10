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
package com.intsof.percussioncms.doctor.api;

import com.intsof.percussioncms.doctor.CleanHeapDumpsCommand;
import com.intsof.percussioncms.doctor.CleanInstallBackupsCommand;
import com.intsof.percussioncms.doctor.CleanLogsCommand;
import com.intsof.percussioncms.doctor.CleanReport;
import com.intsof.percussioncms.doctor.CleanTempCommand;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Locale;
import java.util.Objects;

/**
 * Executes doctor clean commands for the HTTP API. Reuses the same command implementations as the
 * CLI ({@link CleanHeapDumpsCommand}, {@link CleanInstallBackupsCommand}, {@link CleanLogsCommand},
 * {@link CleanTempCommand}).
 *
 * <p>Authorization is enforced by the caller ({@link DoctorRestService}); this class only runs
 * commands.
 */
public final class DoctorApiService {

  private final DoctorInstallRootProvider installRootProvider;

  /**
   * @param installRootProvider default install root when the request omits {@code installRoot}
   */
  public DoctorApiService(DoctorInstallRootProvider installRootProvider) {
    this.installRootProvider = Objects.requireNonNull(installRootProvider, "installRootProvider");
  }

  /**
   * Run a doctor command and return a JSON-friendly report.
   *
   * @param command CLI command token (e.g. {@code clean-heap-dumps})
   * @param request body; null treated as empty request (dry-run default)
   * @return structured report
   * @throws DoctorUnknownCommandException if the command is not supported
   * @throws IllegalArgumentException if options or install root are invalid
   * @throws IOException on unrecoverable walk failures
   */
  public DoctorReportView execute(String command, DoctorRequest request)
      throws IOException, DoctorUnknownCommandException {
    DoctorRequest body = request != null ? request : new DoctorRequest();
    String cmd = command == null ? "" : command.trim();
    if (cmd.isEmpty()) {
      throw new DoctorUnknownCommandException("(empty)");
    }

    Path installRoot = resolveInstallRoot(body);
    boolean dryRun = body.isEffectiveDryRun();

    CleanReport report;
    if (CleanHeapDumpsCommand.COMMAND_NAME.equals(cmd)) {
      report = CleanHeapDumpsCommand.execute(installRoot, dryRun);
    } else if (CleanInstallBackupsCommand.COMMAND_NAME.equals(cmd)) {
      report = CleanInstallBackupsCommand.execute(installRoot, dryRun);
    } else if (CleanLogsCommand.COMMAND_NAME.equals(cmd)) {
      Duration olderThan = null;
      if (body.getOlderThan() != null && !body.getOlderThan().isBlank()) {
        olderThan = CleanLogsCommand.parseOlderThan(body.getOlderThan().trim());
      }
      CleanLogsCommand.Options options =
          new CleanLogsCommand.Options(olderThan, body.isEffectiveKeepCurrent());
      report = CleanLogsCommand.execute(installRoot, dryRun, options);
    } else if (CleanTempCommand.COMMAND_NAME.equals(cmd)) {
      report = CleanTempCommand.execute(installRoot, dryRun);
    } else {
      throw new DoctorUnknownCommandException(cmd);
    }
    return DoctorReportView.from(report);
  }

  /**
   * Resolve the install root for HTTP execution.
   *
   * <p><strong>Security:</strong> the HTTP API never uses a client-supplied path for filesystem
   * I/O. Optional {@link DoctorRequest#getInstallRoot()} is accepted only when it normalizes to the
   * same absolute path as the host default; the return value is always the host-provided {@link
   * Path} so CodeQL / runtime path-injection cannot flow from the JSON body into {@code
   * Files.exists} / walk / delete sinks.
   *
   * @param body request body (non-null)
   * @return trusted install root from {@link DoctorInstallRootProvider}
   * @throws IllegalArgumentException if a non-blank override does not match the host root
   */
  private Path resolveInstallRoot(DoctorRequest body) {
    Path hostRoot =
        Objects.requireNonNull(
            installRootProvider.getDefaultInstallRoot(), "installRootProvider returned null");
    String explicit = body.getInstallRoot();
    if (explicit == null || explicit.isBlank()) {
      return hostRoot;
    }
    Path requested = Path.of(explicit.trim()).toAbsolutePath().normalize();
    Path trusted = hostRoot.toAbsolutePath().normalize();
    if (!installRootPathsEqual(requested, trusted)) {
      throw new IllegalArgumentException(
          "installRoot override is not permitted for the HTTP API; omit installRoot to use the"
              + " server install root");
    }
    // Always return the host Path (trusted), never Path.of(client string).
    return hostRoot;
  }

  /**
   * Equality of install roots after absolute normalize. On Windows, compare case-insensitively
   * because the FS does.
   */
  static boolean installRootPathsEqual(Path a, Path b) {
    if (a.equals(b)) {
      return true;
    }
    // Windows path equality is case-insensitive; Path.equals is not always.
    String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
    if (os.contains("win")) {
      return a.toString().equalsIgnoreCase(b.toString());
    }
    return false;
  }

  /**
   * Normalize a path-style command token to the CLI form if callers use underscores or mixed case.
   * Currently the API expects the same tokens as the CLI (lowercase with hyphens).
   *
   * @param command raw path segment
   * @return trimmed command token
   */
  public static String normalizeCommand(String command) {
    if (command == null) {
      return "";
    }
    return command.trim().toLowerCase(Locale.ROOT);
  }
}
