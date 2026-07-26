/*
 * Copyright 1999-2026 Percussion Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.
 *
 * See the License for the specific language governing permissions and limitations under the
 * License.
 */
package com.percussion.preinstall;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Console wizard for CMS preinstall (issue #1513).
 *
 * <p><strong>Phase 1:</strong> installation directory (when missing) and summary + confirm. Later
 * phases add Java selection orchestration, multi-step database capture, and optional connection
 * test. Silent / non-TTY installs skip prompts and keep the existing parameter-driven contract.
 *
 * <p>Passwords and other secret values from resolved DB config are never printed in the summary.
 */
public final class InteractiveInstallWizard {

  /** Exit code when the operator declines to proceed or cancels. */
  public static final int EXIT_ABORTED = 0;

  /** Exit code when required input is missing in non-interactive mode. */
  public static final int EXIT_USAGE = 0;

  /** Exit code when database configuration validation fails. */
  public static final int EXIT_DB_CONFIG = 1;

  private InteractiveInstallWizard() {}

  /**
   * Whether {@code --silent} / {@code --no-tty} (or aliases) disable interactive prompts.
   *
   * @param options parsed CLI options; may be null
   * @return true when silent mode is requested
   */
  public static boolean isSilentMode(Map<String, String> options) {
    if (options == null) {
      return false;
    }
    String silent = options.get(DbInstallConfigResolver.SILENT_KEY);
    String noTty = options.get("no-tty");
    return isTruthy(silent) || isTruthy(noTty);
  }

  /**
   * Interactive wizard runs only when not silent and a console (or test harness) is available.
   *
   * @param silent silent CLI flag
   * @param consoleAvailable true when {@link System#console()} is non-null or tests inject a
   *     console
   * @return true when the wizard may prompt
   */
  public static boolean isInteractive(boolean silent, boolean consoleAvailable) {
    return !silent && consoleAvailable;
  }

  /**
   * Phase 1: ensure install path, resolve DB config from existing CLI/env defaults, show summary,
   * and confirm when interactive.
   *
   * @param parsedArgs CLI parse result (path may be null)
   * @param interactive whether to prompt
   * @param prompt operator I/O; required when interactive
   * @return result with proceed flag and exit code when aborted
   */
  public static Phase1Result runPhase1(
      DbInstallConfigResolver.ParsedArgs parsedArgs, boolean interactive, InstallPrompt prompt) {
    Objects.requireNonNull(parsedArgs, "parsedArgs");
    if (interactive && prompt == null) {
      throw new IllegalArgumentException("prompt is required when interactive");
    }

    Path installPath = parsedArgs.installPath();
    Map<String, String> options =
        parsedArgs.options() != null
            ? new LinkedHashMap<>(parsedArgs.options())
            : new LinkedHashMap<>();

    if (installPath == null) {
      if (!interactive) {
        return Phase1Result.abort(EXIT_USAGE, usageMessage());
      }
      installPath = promptForInstallPath(prompt);
      if (installPath == null) {
        return Phase1Result.abort(EXIT_ABORTED, "Installation cancelled: no install directory.");
      }
    } else {
      installPath = installPath.toAbsolutePath().normalize();
    }

    DbInstallConfigResolver.ResolvedDbConfig dbConfig;
    try {
      dbConfig = DbInstallConfigResolver.resolveDbConfig(options);
    } catch (IllegalArgumentException badDb) {
      return Phase1Result.abort(EXIT_DB_CONFIG, "Database configuration error: " + badDb.getMessage());
    }

    if (interactive) {
      String summary = buildSummary(installPath, dbConfig);
      prompt.println(summary);
      boolean defaultYes = isDefaultYesConfirm(dbConfig);
      if (!confirmProceed(prompt, defaultYes)) {
        return Phase1Result.abort(EXIT_ABORTED, "Installation cancelled by operator.");
      }
    }

    return Phase1Result.proceed(installPath, Map.copyOf(options), dbConfig);
  }

  /**
   * Short usage printed when install path is missing and the session is non-interactive.
   *
   * @return multi-line usage text
   */
  public static String usageMessage() {
    return String.join(
        System.lineSeparator(),
        "Must specify installation or upgrade folder",
        "When a console is available, omit the path to enter interactive mode.",
        "Optional database target for new installs: -Ddbprops=<path> or --dbprops=<path>",
        "Optional upgrade cleanup: --clean-install-dir (default false) removes obsolete"
            + " folders such as PreInstall",
        "Silent mode: --silent or --no-tty (disables interactive prompts for automated testing)");
  }

  /**
   * Whether the final confirm defaults to yes (embedded engines only).
   *
   * @param dbConfig resolved database configuration
   * @return true for h2/derby defaults
   */
  static boolean isDefaultYesConfirm(DbInstallConfigResolver.ResolvedDbConfig dbConfig) {
    if (dbConfig == null || dbConfig.systemProperties() == null) {
      return true;
    }
    String type =
        dbConfig.systemProperties().getOrDefault("perc.db.type", DbInstallConfigResolver.DB_TYPE_DEFAULT);
    String normalized = type == null ? "" : type.trim().toLowerCase(Locale.ROOT);
    return "h2".equals(normalized) || "derby".equals(normalized);
  }

  /**
   * Builds a non-secret installation summary for the operator.
   *
   * @param installPath absolute install path
   * @param dbConfig resolved DB config
   * @return multi-line summary text
   */
  static String buildSummary(
      Path installPath, DbInstallConfigResolver.ResolvedDbConfig dbConfig) {
    List<String> lines = new ArrayList<>();
    lines.add("");
    lines.add("========================================");
    lines.add("Percussion CMS installation summary");
    lines.add("========================================");
    lines.add("Install path : " + installPath.toAbsolutePath().normalize());
    lines.add("Mode         : " + (isUpgradeInstall(installPath) ? "Upgrade" : "New install"));
    lines.add("Database     : " + formatDbSummary(dbConfig));
    lines.add("Java home    : " + formatJavaHomeHint());
    lines.add("========================================");
    return String.join(System.lineSeparator(), lines);
  }

  static boolean isUpgradeInstall(Path installPath) {
    if (installPath == null) {
      return false;
    }
    return Files.isRegularFile(installPath.resolve(Main.VERSION_PROPERTIES));
  }

  static String formatDbSummary(DbInstallConfigResolver.ResolvedDbConfig dbConfig) {
    if (dbConfig == null) {
      return "(unknown)";
    }
    Map<String, String> p = dbConfig.systemProperties();
    String type = p.getOrDefault("perc.db.type", DbInstallConfigResolver.DB_TYPE_DEFAULT);
    StringBuilder sb = new StringBuilder(type);
    sb.append(" (source=").append(dbConfig.source()).append(')');
    appendIfPresent(sb, "host", p.get("perc.db.host"));
    appendIfPresent(sb, "port", p.get("perc.db.port"));
    appendIfPresent(sb, "name", firstNonBlank(p.get("perc.db.name"), p.get("perc.db.cms.name")));
    appendIfPresent(sb, "user", p.get("perc.db.user"));
    // Never append password / perc.db.password / truststore passwords
    return sb.toString();
  }

  static String formatJavaHomeHint() {
    String unattended = System.getProperty(Main.PERC_JAVA_HOME);
    if (unattended != null
        && !unattended.isBlank()
        && !"${perc.java.home}".equals(unattended.trim())) {
      return unattended.trim() + " (from -Dperc.java.home)";
    }
    return "will be selected next (discover Java 21+)";
  }

  /**
   * Parse a yes/no answer. Empty input uses {@code defaultYes}.
   *
   * @param answer raw operator input
   * @param defaultYes value when answer is blank
   * @return true for yes, false for no, null if unparseable
   */
  static Boolean parseYesNo(String answer, boolean defaultYes) {
    if (answer == null || answer.isBlank()) {
      return defaultYes;
    }
    String a = answer.trim().toLowerCase(Locale.ROOT);
    if ("y".equals(a) || "yes".equals(a)) {
      return true;
    }
    if ("n".equals(a) || "no".equals(a)) {
      return false;
    }
    return null;
  }

  private static Path promptForInstallPath(InstallPrompt prompt) {
    prompt.println("");
    prompt.println("Percussion CMS interactive installer");
    prompt.println("Enter the installation directory (new install or existing upgrade root).");
    for (int attempt = 0; attempt < 5; attempt++) {
      String line = prompt.readLine("Installation directory: ");
      if (line == null || line.isBlank()) {
        prompt.println("Install directory is required. Enter a path or Ctrl+C to abort.");
        continue;
      }
      String trimmed = line.trim();
      // Strip matching quotes operators may paste from docs
      if ((trimmed.startsWith("\"") && trimmed.endsWith("\""))
          || (trimmed.startsWith("'") && trimmed.endsWith("'"))) {
        if (trimmed.length() >= 2) {
          trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
        }
      }
      if (trimmed.isEmpty()) {
        prompt.println("Install directory is required.");
        continue;
      }
      try {
        return Paths.get(trimmed).toAbsolutePath().normalize();
      } catch (Exception ex) {
        prompt.println("Invalid path: " + ex.getMessage());
      }
    }
    return null;
  }

  private static boolean confirmProceed(InstallPrompt prompt, boolean defaultYes) {
    String hint = defaultYes ? "[Y/n]" : "[y/N]";
    for (int attempt = 0; attempt < 5; attempt++) {
      String line = prompt.readLine("Proceed with installation? " + hint + " ");
      Boolean parsed = parseYesNo(line, defaultYes);
      if (parsed != null) {
        return parsed;
      }
      prompt.println("Please answer y or n.");
    }
    return false;
  }

  private static void appendIfPresent(StringBuilder sb, String label, String value) {
    if (value != null && !value.isBlank()) {
      sb.append(", ").append(label).append('=').append(value.trim());
    }
  }

  private static String firstNonBlank(String a, String b) {
    if (a != null && !a.isBlank()) {
      return a;
    }
    if (b != null && !b.isBlank()) {
      return b;
    }
    return null;
  }

  private static boolean isTruthy(String value) {
    if (value == null) {
      return false;
    }
    return "true".equalsIgnoreCase(value)
        || "yes".equalsIgnoreCase(value)
        || "1".equals(value);
  }

  /**
   * Outcome of Phase 1 wizard (path + DB resolve + optional confirm).
   *
   * @param proceed true when install may continue
   * @param installPath resolved absolute install path when proceed (or null if aborted early)
   * @param options CLI options map (immutable when proceed)
   * @param dbConfig resolved DB config when proceed
   * @param exitCode process exit code when !proceed
   * @param message operator-facing message when !proceed (may be null)
   */
  public record Phase1Result(
      boolean proceed,
      Path installPath,
      Map<String, String> options,
      DbInstallConfigResolver.ResolvedDbConfig dbConfig,
      int exitCode,
      String message) {

    static Phase1Result proceed(
        Path installPath,
        Map<String, String> options,
        DbInstallConfigResolver.ResolvedDbConfig dbConfig) {
      return new Phase1Result(true, installPath, options, dbConfig, 0, null);
    }

    static Phase1Result abort(int exitCode, String message) {
      return new Phase1Result(false, null, Map.of(), null, exitCode, message);
    }
  }
}
