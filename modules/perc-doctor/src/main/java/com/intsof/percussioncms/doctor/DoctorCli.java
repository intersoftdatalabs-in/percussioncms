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

/**
 * CLI entry for {@code perc-doctor}.
 *
 * <pre>
 * perc-doctor [--install-root &lt;path&gt;] [--dry-run] [-v|--verbose] &lt;command&gt;
 * </pre>
 *
 * <p>Commands: {@code clean-heap-dumps}, {@code clean-install-backups}.
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
              + CleanHeapDumpsCommand.COMMAND_NAME
              + " or "
              + CleanInstallBackupsCommand.COMMAND_NAME);
      printHelp(err);
      return EXIT_USAGE;
    }

    Path installRoot =
        parsed.installRoot != null
            ? Path.of(parsed.installRoot)
            : Path.of("").toAbsolutePath().normalize();

    try {
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
      err.println("Error: unknown command: " + parsed.command);
      err.println(
          "Supported commands: "
              + CleanHeapDumpsCommand.COMMAND_NAME
              + ", "
              + CleanInstallBackupsCommand.COMMAND_NAME);
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
  }

  /**
   * Minimal argv parser (no external CLI library). Supports long options and {@code -v}/{@code
   * -h}.
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
    stream.println("usage: perc-doctor [options] <command>");
    stream.println();
    stream.println(
        "CMS Doctor — safe install-tree maintenance"
            + " (clean-heap-dumps, clean-install-backups).");
    stream.println();
    stream.println("Options:");
    stream.println("  --install-root <path>  CMS install root (default: current working directory)");
    stream.println("  --dry-run              Report only; never delete or write");
    stream.println("  -v, --verbose          Print each candidate path and size");
    stream.println("  -h, --help             Show usage");
    stream.println();
    stream.println("Commands:");
    stream.println("  clean-heap-dumps       Remove recursive *.hprof under install root");
    stream.println(
        "  clean-install-backups  Remove allowlisted installer/upgrade backups"
            + " (*.bak, *.backup, AppServer_backup_*.zip)");
    stream.println();
    stream.println("Examples:");
    stream.println("  perc-doctor --install-root /opt/Percussion --dry-run clean-heap-dumps");
    stream.println("  perc-doctor --install-root C:\\Percussion -v clean-heap-dumps");
    stream.println(
        "  perc-doctor --install-root /opt/Percussion --dry-run -v clean-install-backups");
    stream.println("  perc-doctor --install-root C:\\Percussion -v clean-install-backups");
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
