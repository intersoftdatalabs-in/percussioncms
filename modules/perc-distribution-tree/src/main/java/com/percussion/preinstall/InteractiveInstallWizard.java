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

import com.percussion.preinstall.java.JavaInstallSelection;
import java.io.IOException;
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
 * <p><strong>Phase 1:</strong> installation directory (prompted when not supplied on the CLI;
 * last-install path is the editable default only) and summary + confirm.
 *
 * <p><strong>Phase 2:</strong> Java home selection ({@link JavaInstallSelection}) after path.
 *
 * <p><strong>Phase 3:</strong> interactive multi-step database capture ({@link
 * InteractiveDbConfigCollector}) and optional {@link RepositoryConnectionProbe} for external
 * backends. Silent / non-TTY installs skip prompts and keep the existing parameter-driven contract.
 *
 * <p>Passwords and other secret values from resolved DB config are never printed in the summary.
 */
public final class InteractiveInstallWizard {

  /**
   * Exit code when the operator declines to proceed or cancels. Zero so interactive cancel is not
   * treated as a hard automation failure.
   */
  public static final int EXIT_ABORTED = 0;

  /**
   * Exit code when required input is missing in non-interactive mode (sysexits-style {@code
   * EX_USAGE}). Distinct from {@link #EXIT_ABORTED} so scripts can detect missing path/args.
   */
  public static final int EXIT_USAGE = 64;

  /** Exit code when database configuration validation fails. */
  public static final int EXIT_DB_CONFIG = 1;

  /** Exit code when Java home selection fails (matches historical Main exit code). */
  public static final int EXIT_JAVA = 2;

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
   * Phase 1–2: ensure install path, select/persist Java home, resolve DB config from existing
   * CLI/env defaults, show summary, and confirm when interactive.
   *
   * <p>Install path rules:
   *
   * <ul>
   *   <li>CLI path always wins (no path re-prompt in interactive mode).
   *   <li>Interactive with no CLI path always prompts; last-install path is only the default the
   *       operator may accept or change.
   *   <li>Silent / non-TTY with no CLI path uses the last-install path when present, otherwise
   *       usage abort.
   * </ul>
   *
   * @param parsedArgs CLI parse result (path may be null)
   * @param interactive whether to prompt
   * @param prompt operator I/O; required when interactive
   * @return result with proceed flag and exit code when aborted
   */
  public static Phase1Result runPhase1(
      DbInstallConfigResolver.ParsedArgs parsedArgs, boolean interactive, InstallPrompt prompt) {
    return runPhase1(
        parsedArgs,
        interactive,
        prompt,
        Main.parseUnattendedJavaHome(System.getProperty(Main.PERC_JAVA_HOME)));
  }

  /**
   * Same as {@link #runPhase1(DbInstallConfigResolver.ParsedArgs, boolean, InstallPrompt)} with an
   * explicit unattended Java home (tests inject a fixture home).
   *
   * @param unattendedJavaHome explicit Java home override, or null to discover
   */
  public static Phase1Result runPhase1(
      DbInstallConfigResolver.ParsedArgs parsedArgs,
      boolean interactive,
      InstallPrompt prompt,
      Path unattendedJavaHome) {
    return runPhase1(parsedArgs, interactive, prompt, unattendedJavaHome, null);
  }

  /**
   * Same as {@link #runPhase1(DbInstallConfigResolver.ParsedArgs, boolean, InstallPrompt, Path)}
   * with injectable user-settings home (tests) or null for {@code user.home}.
   *
   * @param userSettingsHome optional home override for {@link InstallerUserSettings}; null =
   *     default
   */
  public static Phase1Result runPhase1(
      DbInstallConfigResolver.ParsedArgs parsedArgs,
      boolean interactive,
      InstallPrompt prompt,
      Path unattendedJavaHome,
      Path userSettingsHome) {
    Objects.requireNonNull(parsedArgs, "parsedArgs");
    if (interactive && prompt == null) {
      throw new IllegalArgumentException("prompt is required when interactive");
    }

    InstallerUserSettings userSettings =
        new InstallerUserSettings(userSettingsHome, InstallerUserSettings.PREFIX_CMS);
    // Track CLI path before defaults: applyDefaults fills install.directory from
    // ~/.intsof/percussion/last-install.properties for silent reuse, but interactive mode must
    // still prompt so the operator can change the directory (saved value is the default only).
    boolean pathFromCli = parsedArgs.installPath() != null;
    DbInstallConfigResolver.ParsedArgs withDefaults = userSettings.applyDefaults(parsedArgs);

    Path installPath = withDefaults.installPath();
    Map<String, String> options =
        withDefaults.options() != null
            ? new LinkedHashMap<>(withDefaults.options())
            : new LinkedHashMap<>();

    if (pathFromCli) {
      installPath = installPath.toAbsolutePath().normalize();
    } else if (interactive) {
      // Always prompt when the operator did not pass a path on the CLI. Saved last-install path
      // (if any) is offered as the editable default, not a forced destination.
      Path defaultPath =
          installPath != null
              ? installPath.toAbsolutePath().normalize()
              : userSettings.loadInstallDirectory().orElse(null);
      installPath = promptForInstallPath(prompt, defaultPath);
      if (installPath == null) {
        return Phase1Result.abort(EXIT_ABORTED, "Installation cancelled: no install directory.");
      }
    } else if (installPath == null) {
      return Phase1Result.abort(EXIT_USAGE, usageMessage());
    } else {
      // Silent / non-TTY: honor last-install path from applyDefaults
      installPath = installPath.toAbsolutePath().normalize();
    }

    JavaInstallSelection.SelectionOutcome javaOutcome;
    try {
      JavaInstallSelection.InteractivePrompt javaPrompt =
          interactive && prompt != null ? prompt::readLine : null;
      javaOutcome =
          new JavaInstallSelection(installPath, unattendedJavaHome, javaPrompt).selectAndPersist();
      if (interactive && prompt != null) {
        prompt.println("Java home selection: " + javaOutcome.summary());
      }
    } catch (JavaInstallSelection.JavaSelectionException sel) {
      return Phase1Result.abort(EXIT_JAVA, "Java home selection failed: " + sel.getMessage());
    } catch (IOException io) {
      return Phase1Result.abort(
          EXIT_JAVA,
          "Could not write java.properties at "
              + installPath.resolve("java.properties")
              + ": "
              + io.getMessage());
    }

    boolean upgrade = isUpgradeInstall(installPath);
    DbInstallConfigResolver.ResolvedDbConfig dbConfig;
    try {
      if (interactive) {
        dbConfig = collectAndResolveDbInteractive(options, upgrade, prompt);
      } else {
        dbConfig = DbInstallConfigResolver.resolveDbConfig(options);
      }
    } catch (IllegalArgumentException badDb) {
      return Phase1Result.abort(
          EXIT_DB_CONFIG, "Database configuration error: " + badDb.getMessage());
    }

    // Sample sites (Corporate Investments / Enterprise Investments) seed on both new
    // installs and upgrades. The install.demo.sites flag drives an additional PSTableAction
    // pass that runs after the core schema load. Protection against overwriting operator
    // locale tables (RXLOCALE / RXLOCALEFORMAT) is handled at the ANT layer by the strip
    // step in installRepository.xml, not by gating here.
    boolean demoSites = resolveDemoSites(interactive, options, prompt);

    if (interactive) {
      String summary = buildSummary(installPath, dbConfig, javaOutcome, demoSites, upgrade);
      prompt.println(summary);
      boolean defaultYes = isDefaultYesConfirm(dbConfig);
      if (!confirmProceed(prompt, defaultYes)) {
        return Phase1Result.abort(EXIT_ABORTED, "Installation cancelled by operator.");
      }
    }

    return Phase1Result.proceed(installPath, Map.copyOf(options), dbConfig, javaOutcome);
  }

  /**
   * Decide whether the operator opted in to sample-site seeding.
   *
   * <p>Interactive: prompt with default No (sample data is destructive to an existing locale/site
   * matrix; the install dir may be re-used for QA experiments). Pre-populated from any CLI {@code
   * --demo-sites} flag so a script that later upgrades its prompt path picks up the same value.
   *
   * <p>Silent: honor the resolved CLI value from {@link
   * DbInstallConfigResolver#parseDemoSitesFlag(java.util.Map)}.
   *
   * <p>Same flow on new installs and upgrades — the install.demo.sites flag drives an additional
   * PSTableAction pass after the core schema load regardless of install type. RXLOCALE /
   * RXLOCALEFORMAT protection is handled by the strip step in installRepository.xml, not by gating
   * here.
   *
   * @param interactive whether the wizard may prompt
   * @param options mutable options map; mutated to record the resolved value
   * @param prompt operator I/O
   * @return resolved sample-sites decision (never null)
   */
  static boolean resolveDemoSites(
      boolean interactive, Map<String, String> options, InstallPrompt prompt) {
    if (!interactive) {
      boolean resolved = DbInstallConfigResolver.parseDemoSitesFlag(options);
      options.put(DbInstallConfigResolver.DEMO_SITES_KEY, Boolean.toString(resolved));
      return resolved;
    }

    boolean preFromCli = DbInstallConfigResolver.parseDemoSitesFlag(options);
    for (int attempt = 0; attempt < 5; attempt++) {
      String hint = preFromCli ? "[Y/n]" : "[y/N]";
      String line =
          prompt.readLine(
              "Install sample sites (Corporate Investments / Enterprise Investments)? "
                  + hint
                  + " ");
      // Empty input on first attempt honors the CLI pre-population; otherwise default to No.
      Boolean parsed;
      if (line == null || line.isBlank()) {
        parsed = preFromCli;
      } else {
        parsed = parseYesNo(line, preFromCli);
      }
      if (parsed != null) {
        options.put(DbInstallConfigResolver.DEMO_SITES_KEY, Boolean.toString(parsed));
        return parsed;
      }
      prompt.println("Please answer y or n.");
    }
    options.put(DbInstallConfigResolver.DEMO_SITES_KEY, "false");
    return false;
  }

  /**
   * Interactive DB collection, resolve, optional connection probe with re-edit loop.
   *
   * @param options mutable options map (updated in place)
   * @param upgrade whether install root is an upgrade
   * @param prompt operator I/O
   * @return resolved DB config
   */
  static DbInstallConfigResolver.ResolvedDbConfig collectAndResolveDbInteractive(
      Map<String, String> options, boolean upgrade, InstallPrompt prompt) {
    for (int attempt = 0; attempt < 5; attempt++) {
      Map<String, String> collected =
          InteractiveDbConfigCollector.collect(options, upgrade, prompt);
      options.clear();
      options.putAll(collected);

      DbInstallConfigResolver.ResolvedDbConfig dbConfig =
          DbInstallConfigResolver.resolveDbConfig(options);

      if (upgrade || isDefaultYesConfirm(dbConfig)) {
        return dbConfig;
      }

      Boolean test = parseYesNo(prompt.readLine("Test database connection now? [Y/n] "), true);
      if (test == null || !test) {
        return dbConfig;
      }

      RepositoryConnectionProbe.ProbeResult probe =
          RepositoryConnectionProbe.probe(
              dbConfig.systemProperties(), RepositoryConnectionProbe.DEFAULT_LOGIN_TIMEOUT_SECONDS);
      prompt.println(probe.message());

      if (probe.isSuccess() || probe.status() == RepositoryConnectionProbe.ProbeStatus.SKIPPED) {
        return dbConfig;
      }

      Boolean retry =
          parseYesNo(
              prompt.readLine("Connection test failed. Re-enter database settings? [Y/n] "), true);
      if (retry == null || !retry) {
        throw new IllegalArgumentException(
            "Database connection test failed and operator chose not to re-enter settings.");
      }
      // Force re-prompt of fields on next loop (clear explicit override keys except silent flags)
      options.remove(DbInstallConfigResolver.DBPROPS_KEY);
      options.remove("db.props");
      options.remove("db.type");
      options.remove("db.host");
      options.remove("db.port");
      options.remove("db.name");
      options.remove("db.schema");
      options.remove("db.user");
      options.remove("db.password");
      options.remove(InteractiveDbConfigCollector.EMBEDDED_H2_DB_PASSWORD_KEY);
      options.remove("db.ssl.enabled");
      options.remove("db.ssl.verify");
      options.remove("db.ssl.allowSelfSigned");
      options.remove("db.ssl.trustStorePath");
      options.remove("db.ssl.trustStorePassword");
      options.remove("db.ssl.keyStorePath");
      options.remove("db.ssl.keyStorePassword");
    }
    throw new IllegalArgumentException(
        "Too many failed database configuration attempts. Aborting.");
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
        "Optional sample sites: --demo-sites / --no-demo-sites (default no). Seeds the"
            + " Corporate Investments / Enterprise Investments sample sites on a new"
            + " install. Ignored on upgrades.",
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
        dbConfig
            .systemProperties()
            .getOrDefault("perc.db.type", DbInstallConfigResolver.DB_TYPE_DEFAULT);
    String normalized = type == null ? "" : type.trim().toLowerCase(Locale.ROOT);
    return "h2".equals(normalized) || "derby".equals(normalized);
  }

  /**
   * Builds a non-secret installation summary for the operator.
   *
   * @param installPath absolute install path
   * @param dbConfig resolved DB config
   * @param javaOutcome selected Java home (may be null before Phase 2)
   * @param demoSites whether sample sites will be seeded after the core schema load
   * @param upgrade whether the install root is an existing product install
   * @return multi-line summary text
   */
  static String buildSummary(
      Path installPath,
      DbInstallConfigResolver.ResolvedDbConfig dbConfig,
      JavaInstallSelection.SelectionOutcome javaOutcome,
      boolean demoSites,
      boolean upgrade) {
    List<String> lines = new ArrayList<>();
    lines.add("");
    lines.add("========================================");
    lines.add("Percussion CMS installation summary");
    lines.add("========================================");
    lines.add("Install path : " + installPath.toAbsolutePath().normalize());
    lines.add("Mode         : " + (upgrade ? "Upgrade" : "New install"));
    lines.add("Database     : " + formatDbSummary(dbConfig));
    String secretLocation = formatDbSecretLocation(dbConfig);
    if (secretLocation != null) {
      lines.add("DB password  : stored in " + secretLocation);
    }
    lines.add("Java home    : " + formatJavaHomeLine(javaOutcome));
    lines.add(
        "Sample sites : "
            + (demoSites ? "enabled (Corporate + Enterprise Investments)" : "disabled"));
    lines.add("========================================");
    return String.join(System.lineSeparator(), lines);
  }

  /**
   * Backwards-compatible overload for tests that pre-date the demo-sites flag.
   *
   * @deprecated pass demoSites/upgrade explicitly; kept for legacy callers
   */
  @Deprecated
  static String buildSummary(Path installPath, DbInstallConfigResolver.ResolvedDbConfig dbConfig) {
    return buildSummary(installPath, dbConfig, null, false, isUpgradeInstall(installPath));
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
    // Never append password / perc.db.password / truststore passwords. For
    // embedded H2, see formatDbSecretLocation below for the operator-visible
    // location: rxrepository.properties (operator-supplied) or
    // var/config/generated/passwords (system-generated random value).
    return sb.toString();
  }

  static String formatDbSecretLocation(DbInstallConfigResolver.ResolvedDbConfig dbConfig) {
    if (dbConfig == null || dbConfig.systemProperties() == null) {
      return null;
    }
    String type = dbConfig.systemProperties().get("perc.db.type");
    if (type == null || !"h2".equalsIgnoreCase(type)) {
      return null;
    }
    // Interactive H2 installs: operator chose the password and it lives only in
    // rxrepository.properties + perc-ds.properties (encrypted by Jetty). Silent
    // installs: a random value was generated and persisted under
    // var/config/generated/passwords (key cmdb).
    String operatorSupplied = dbConfig.systemProperties().get("cmdb.password");
    if (operatorSupplied != null && !operatorSupplied.isEmpty()) {
      return "rxrepository.properties (PWD=...) — operator-chosen";
    }
    return "var/config/generated/passwords (key cmdb)";
  }

  static String formatJavaHomeLine(JavaInstallSelection.SelectionOutcome javaOutcome) {
    if (javaOutcome != null && javaOutcome.javaHome() != null) {
      return javaOutcome.javaHome() + " (" + javaOutcome.source() + ")";
    }
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

  private static Path promptForInstallPath(InstallPrompt prompt, Path defaultPath) {
    prompt.println("");
    prompt.println("Percussion CMS interactive installer");
    prompt.println("Enter the installation directory (new install or existing upgrade root).");
    String defaultHint =
        defaultPath != null ? " [" + defaultPath.toAbsolutePath().normalize() + "]" : "";
    for (int attempt = 0; attempt < 5; attempt++) {
      String line = prompt.readLine("Installation directory" + defaultHint + ": ");
      if (line == null || line.isBlank()) {
        if (defaultPath != null) {
          return defaultPath.toAbsolutePath().normalize();
        }
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
        if (defaultPath != null) {
          return defaultPath.toAbsolutePath().normalize();
        }
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

  /** First non-blank value, trimmed (aligned with {@code RepositoryConnectionProbe} contract). */
  private static String firstNonBlank(String a, String b) {
    if (a != null && !a.isBlank()) {
      return a.trim();
    }
    if (b != null && !b.isBlank()) {
      return b.trim();
    }
    return null;
  }

  private static boolean isTruthy(String value) {
    if (value == null) {
      return false;
    }
    return "true".equalsIgnoreCase(value) || "yes".equalsIgnoreCase(value) || "1".equals(value);
  }

  /**
   * Outcome of Phase 1–2 wizard (path + Java + DB resolve + optional confirm).
   *
   * @param proceed true when install may continue
   * @param installPath resolved absolute install path when proceed (or null if aborted early)
   * @param options CLI options map (immutable when proceed)
   * @param dbConfig resolved DB config when proceed
   * @param javaOutcome selected Java home when proceed
   * @param exitCode process exit code when !proceed
   * @param message operator-facing message when !proceed (may be null)
   */
  public record Phase1Result(
      boolean proceed,
      Path installPath,
      Map<String, String> options,
      DbInstallConfigResolver.ResolvedDbConfig dbConfig,
      JavaInstallSelection.SelectionOutcome javaOutcome,
      int exitCode,
      String message) {

    static Phase1Result proceed(
        Path installPath,
        Map<String, String> options,
        DbInstallConfigResolver.ResolvedDbConfig dbConfig,
        JavaInstallSelection.SelectionOutcome javaOutcome) {
      return new Phase1Result(true, installPath, options, dbConfig, javaOutcome, 0, null);
    }

    static Phase1Result abort(int exitCode, String message) {
      return new Phase1Result(false, null, Map.of(), null, null, exitCode, message);
    }
  }
}
