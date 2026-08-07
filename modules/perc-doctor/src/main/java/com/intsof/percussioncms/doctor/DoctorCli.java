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
import java.io.PrintStream;
import java.nio.file.Path;
import java.time.Duration;

/**
 * CLI entry for {@code perc-doctor}.
 *
 * <pre>
 * perc-doctor [--install-root &lt;path&gt;] [--dry-run] [-v|--verbose]
 *   &lt;command&gt; [command-options]
 * </pre>
 *
 * <p>Commands: {@code diagnose}/{@code health}, {@code clean-heap-dumps}, {@code
 * clean-install-backups}, {@code clean-logs}.
 */
public final class DoctorCli {

  static final int EXIT_OK = 0;
  static final int EXIT_USAGE = 2;
  static final int EXIT_ERROR = 1;

  private DoctorCli() {}

  /**
   * Process entrypoint; exits the JVM with the command result code.
   *
   * @param args CLI arguments
   */
  public static void main(String[] args) {
    int code = run(args, System.out, System.err);
    System.exit(code);
  }

  /**
   * Parse args and execute; returns process exit code without calling {@link System#exit}.
   * Exposed for tests.
   *
   * @param args command-line arguments (same as {@code main})
   * @param out standard output stream
   * @param err standard error stream
   * @return process exit code ({@link #EXIT_OK}, {@link #EXIT_ERROR}, or {@link #EXIT_USAGE})
   */
  public static int run(String[] args, PrintStream out, PrintStream err) {
    ParsedArgs parsed;
    try {
      parsed = parseArgs(args);
    } catch (IllegalArgumentException e) {
      err.println("Error: " + e.getMessage());
      printHelp(err);
      return EXIT_USAGE;
    }

    if (parsed.help) {
      printHelp(out);
      return EXIT_OK;
    }

    if (parsed.command == null || parsed.command.isEmpty()) {
      err.println(
          "Error: missing command. Try: "
              + DiagnoseCommand.COMMAND_NAME
              + "/"
              + DiagnoseCommand.COMMAND_ALIAS
              + ", "
              + CleanHeapDumpsCommand.COMMAND_NAME
              + ", "
              + CleanInstallBackupsCommand.COMMAND_NAME
              + ", or "
              + CleanLogsCommand.COMMAND_NAME);
      printHelp(err);
      return EXIT_USAGE;
    }

    Path installRoot =
        parsed.installRoot != null
            ? Path.of(parsed.installRoot)
            : Path.of("").toAbsolutePath().normalize();

    // clean-logs-only options: warn (do not hard-fail) when used with other commands.
    if (parsed.olderThan != null
        && !CleanLogsCommand.COMMAND_NAME.equals(parsed.command)) {
      err.println(
          "Warning: --older-than is only used by "
              + CleanLogsCommand.COMMAND_NAME
              + "; ignoring for "
              + parsed.command);
    }
    if (parsed.keepCurrentExplicit
        && !CleanLogsCommand.COMMAND_NAME.equals(parsed.command)) {
      err.println(
          "Warning: --keep-current / --no-keep-current are only used by "
              + CleanLogsCommand.COMMAND_NAME
              + "; ignoring for "
              + parsed.command);
    }

    try {
      if (DiagnoseCommand.isDiagnoseCommand(parsed.command)) {
        DiagnoseReport report =
            DiagnoseCommand.execute(installRoot, parsed.dryRun, parsed.command);
        printDiagnoseReport(report, parsed.verbose, out);
        return report.isHealthy() ? EXIT_OK : EXIT_ERROR;
      }
      if (CleanHeapDumpsCommand.COMMAND_NAME.equals(parsed.command)) {
        CleanReport report = CleanHeapDumpsCommand.execute(installRoot, parsed.dryRun);
        printReport(report, parsed.verbose, out);
        return report.getFailedCount() > 0 ? EXIT_ERROR : EXIT_OK;
      }
      if (CleanInstallBackupsCommand.COMMAND_NAME.equals(parsed.command)) {
        CleanReport report = CleanInstallBackupsCommand.execute(installRoot, parsed.dryRun);
        printReport(report, parsed.verbose, out);
        return report.getFailedCount() > 0 ? EXIT_ERROR : EXIT_OK;
      }
      if (CleanLogsCommand.COMMAND_NAME.equals(parsed.command)) {
        CleanLogsCommand.Options options =
            new CleanLogsCommand.Options(parsed.olderThan, parsed.keepCurrent);
        CleanReport report = CleanLogsCommand.execute(installRoot, parsed.dryRun, options);
        printReport(report, parsed.verbose, out);
        return report.getFailedCount() > 0 ? EXIT_ERROR : EXIT_OK;
      }
      err.println("Error: unknown command: " + parsed.command);
      err.println(
          "Supported commands: "
              + DiagnoseCommand.COMMAND_NAME
              + "/"
              + DiagnoseCommand.COMMAND_ALIAS
              + ", "
              + CleanHeapDumpsCommand.COMMAND_NAME
              + ", "
              + CleanInstallBackupsCommand.COMMAND_NAME
              + ", "
              + CleanLogsCommand.COMMAND_NAME);
      printHelp(err);
      return EXIT_USAGE;
    } catch (IllegalArgumentException e) {
      err.println("Error: " + e.getMessage());
      return EXIT_ERROR;
    } catch (IOException e) {
      err.println("Error: " + e.getMessage());
      return EXIT_ERROR;
    }
  }

  static final class ParsedArgs {
    String installRoot;
    boolean dryRun;
    boolean verbose;
    boolean help;
    String command;
    /** Optional age filter for {@code clean-logs}; null when unset. */
    Duration olderThan;
    /** Default true for {@code clean-logs}. */
    boolean keepCurrent = true;
    /** True when the user passed {@code --keep-current} or {@code --no-keep-current}. */
    boolean keepCurrentExplicit;
  }

  /**
   * Minimal argv parser (no external CLI library). Supports long options and {@code -v}/{@code
   * -h}. Global and command-specific options may appear before or after the command token.
   */
  static ParsedArgs parseArgs(String[] args) {
    ParsedArgs parsed = new ParsedArgs();
    if (args == null) {
      return parsed;
    }
    for (int i = 0; i < args.length; i++) {
      String a = args[i];
      if ("--help".equals(a) || "-h".equals(a)) {
        parsed.help = true;
      } else if ("--dry-run".equals(a)) {
        parsed.dryRun = true;
      } else if ("--verbose".equals(a) || "-v".equals(a)) {
        parsed.verbose = true;
      } else if ("--install-root".equals(a)) {
        if (i + 1 >= args.length) {
          throw new IllegalArgumentException("--install-root requires a path argument");
        }
        parsed.installRoot = args[++i];
      } else if ("--older-than".equals(a)) {
        if (i + 1 >= args.length) {
          throw new IllegalArgumentException(
              "--older-than requires a duration argument (e.g. 7d)");
        }
        parsed.olderThan = CleanLogsCommand.parseOlderThan(args[++i]);
      } else if ("--keep-current".equals(a)) {
        parsed.keepCurrent = true;
        parsed.keepCurrentExplicit = true;
      } else if ("--no-keep-current".equals(a)) {
        parsed.keepCurrent = false;
        parsed.keepCurrentExplicit = true;
      } else if (a.startsWith("-")) {
        throw new IllegalArgumentException("unknown option: " + a);
      } else if (parsed.command == null) {
        parsed.command = a;
      } else {
        throw new IllegalArgumentException("unexpected argument: " + a);
      }
    }
    return parsed;
  }

  private static void printHelp(PrintStream stream) {
    stream.println("usage: perc-doctor [options] <command> [command-options]");
    stream.println();
    stream.println(
        "CMS Doctor — install diagnose + safe install-tree maintenance"
            + " (diagnose/health, clean-heap-dumps, clean-install-backups, clean-logs).");
    stream.println();
    stream.println("Options:");
    stream.println("  --install-root <path>  CMS install root (default: current working directory)");
    stream.println("  --dry-run              Report only; never delete or write");
    stream.println("  -v, --verbose          Print each candidate path / check detail");
    stream.println("  -h, --help             Show usage");
    stream.println();
    stream.println("Commands:");
    stream.println(
        "  diagnose, health       Read-only install checklist (layout, disk, config, Java,"
            + " log dirs); never deletes");
    stream.println("  clean-heap-dumps       Remove recursive *.hprof under install root");
    stream.println(
        "  clean-install-backups  Remove allowlisted installer/upgrade backups"
            + " (*.bak, *.backup, AppServer_backup_*.zip)");
    stream.println(
        "  clean-logs             Remove aged logs under known Jetty/CMS/DTS log dirs");
    stream.println();
    stream.println("clean-logs options:");
    stream.println(
        "  --older-than <dur>     Only files older than duration (e.g. 7d, 24h, 30m)");
    stream.println(
        "  --keep-current         Never delete active current *.log/*.out (default)");
    stream.println(
        "  --no-keep-current      Allow deleting active current log basenames");
    stream.println();
    stream.println("Examples:");
    stream.println("  perc-doctor --install-root /opt/Percussion -v diagnose");
    stream.println("  perc-doctor --install-root C:\\Percussion --dry-run health");
    stream.println("  perc-doctor --install-root /opt/Percussion --dry-run clean-heap-dumps");
    stream.println("  perc-doctor --install-root C:\\Percussion -v clean-heap-dumps");
    stream.println(
        "  perc-doctor --install-root /opt/Percussion --dry-run -v clean-install-backups");
    stream.println("  perc-doctor --install-root C:\\Percussion -v clean-install-backups");
    stream.println(
        "  perc-doctor --install-root /opt/Percussion --dry-run -v clean-logs --older-than 7d");
    stream.println(
        "  perc-doctor --install-root C:\\Percussion -v clean-logs --older-than 14d");
  }

  static void printDiagnoseReport(DiagnoseReport report, boolean verbose, PrintStream out) {
    out.println("command=" + report.getCommand());
    out.println("install-root=" + report.getInstallRoot());
    out.println("dry-run=" + report.isDryRun());
    out.println("read-only=true");
    out.println("checks=" + report.getCheckCount());
    out.println("pass=" + report.getPassCount());
    out.println("warn=" + report.getWarnCount());
    out.println("fail=" + report.getFailCount());
    out.println("info=" + report.getInfoCount());
    out.println("healthy=" + report.isHealthy());
    if (verbose) {
      for (DiagnoseReport.Check c : report.getChecks()) {
        String pathPart = c.getPath() == null ? "" : " " + c.getPath();
        out.println("  " + c.getStatus() + " " + c.getId() + " " + c.getMessage() + pathPart);
      }
    } else if (report.getCheckCount() > 0) {
      out.println("(use -v/--verbose for checklist detail)");
    }
  }

  static void printReport(CleanReport report, boolean verbose, PrintStream out) {
    out.println("command=" + report.getCommand());
    out.println("install-root=" + report.getInstallRoot());
    out.println("dry-run=" + report.isDryRun());
    out.println("candidates=" + report.getCandidateCount());
    out.println("bytes=" + report.getTotalBytes());
    if (!report.isDryRun()) {
      out.println("deleted=" + report.getDeletedCount());
    }
    out.println("failed=" + report.getFailedCount());
    if (verbose) {
      for (CleanReport.Entry e : report.getEntries()) {
        String detail = e.getDetail() == null ? "" : " " + e.getDetail();
        out.println(
            "  " + e.getStatus() + " " + e.getSizeBytes() + " " + e.getPath() + detail);
      }
    } else if (report.isDryRun() && report.getCandidateCount() > 0) {
      out.println("(use -v/--verbose for path list)");
    }
  }
}
