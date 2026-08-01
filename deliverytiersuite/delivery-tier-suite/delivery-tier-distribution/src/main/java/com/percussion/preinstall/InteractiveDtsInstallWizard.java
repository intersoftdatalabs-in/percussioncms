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
 * Console wizard for DTS preinstall (issue #1513 Phase 4). Mirrors the CMS interactive flow:
 * install directory, Java home, production/staging, database multi-step, optional connection probe,
 * summary + confirm. Silent / non-TTY installs skip prompts.
 */
public final class InteractiveDtsInstallWizard {

  /**
   * Exit code when the operator declines to proceed or cancels. Zero so interactive cancel is not
   * treated as a hard automation failure.
   */
  public static final int EXIT_ABORTED = 0;

  /**
   * Exit code when required input is missing in non-interactive mode (sysexits-style {@code
   * EX_USAGE}). Distinct from {@link #EXIT_ABORTED}.
   */
  public static final int EXIT_USAGE = 64;

  /** Exit code when database configuration validation fails. */
  public static final int EXIT_DB_CONFIG = 1;

  /** Exit code when Java home selection fails. */
  public static final int EXIT_JAVA = 2;

  private InteractiveDtsInstallWizard() {}

  /**
   * Whether {@code --silent} / {@code --no-tty} disable interactive prompts.
   *
   * @param options parsed CLI options; may be null
   * @return true when silent mode is requested
   */
  public static boolean isSilentMode(Map<String, String> options) {
    return MainDTSPreInstall.isSilentMode(options);
  }

  /**
   * Interactive wizard runs only when not silent and a console is available.
   *
   * @param silent silent CLI flag
   * @param consoleAvailable true when {@link System#console()} is non-null
   * @return true when the wizard may prompt
   */
  public static boolean isInteractive(boolean silent, boolean consoleAvailable) {
    return !silent && consoleAvailable;
  }

  /**
   * Run the DTS interactive (or non-interactive) pre-confirm phase.
   *
   * @param parsedArgs CLI parse result
   * @param interactive whether to prompt
   * @param prompt operator I/O; required when interactive
   * @param unattendedJavaHome explicit Java home or null
   * @param installProdDtsProperty raw {@code install.prod.dts} system property (may be null)
   * @return result with proceed flag
   */
  public static WizardResult run(
      MainDTSPreInstall.ParsedArgs parsedArgs,
      boolean interactive,
      InstallPrompt prompt,
      Path unattendedJavaHome,
      String installProdDtsProperty) {
    return run(parsedArgs, interactive, prompt, unattendedJavaHome, installProdDtsProperty, null);
  }

  /**
   * Same as {@link #run(MainDTSPreInstall.ParsedArgs, boolean, InstallPrompt, Path, String)} with
   * injectable user-settings home for tests.
   *
   * @param userSettingsHome optional {@code user.home} override; null for default
   */
  public static WizardResult run(
      MainDTSPreInstall.ParsedArgs parsedArgs,
      boolean interactive,
      InstallPrompt prompt,
      Path unattendedJavaHome,
      String installProdDtsProperty,
      Path userSettingsHome) {
    Objects.requireNonNull(parsedArgs, "parsedArgs");
    if (interactive && prompt == null) {
      throw new IllegalArgumentException("prompt is required when interactive");
    }

    // Seed path/options from DTS prod when install.prod.dts=true; else prefer any saved DTS path
    // for the path prompt default. Final option defaults re-applied after server type is known.
    String initialPrefix =
        "true".equalsIgnoreCase(installProdDtsProperty)
            ? InstallerUserSettings.PREFIX_DTS_PROD
            : "false".equalsIgnoreCase(installProdDtsProperty)
                ? InstallerUserSettings.PREFIX_DTS_STAGE
                : InstallerUserSettings.PREFIX_DTS_PROD;
    InstallerUserSettings initialSettings =
        new InstallerUserSettings(userSettingsHome, initialPrefix);
    MainDTSPreInstall.ParsedArgs withDefaults = initialSettings.applyDefaults(parsedArgs);

    Path installPath = withDefaults.installPath();
    Map<String, String> options =
        withDefaults.options() != null
            ? new LinkedHashMap<>(withDefaults.options())
            : new LinkedHashMap<>();

    if (installPath == null) {
      Path pathDefault =
          InstallerUserSettings.loadAnyDtsInstallDirectory(userSettingsHome).orElse(null);
      if (!interactive) {
        if (pathDefault != null) {
          installPath = pathDefault;
        } else {
          return WizardResult.abort(EXIT_USAGE, usageMessage());
        }
      } else {
        installPath = promptForInstallPath(prompt, pathDefault);
        if (installPath == null) {
          return WizardResult.abort(EXIT_ABORTED, "Installation cancelled: no install directory.");
        }
      }
    } else {
      installPath = installPath.toAbsolutePath().normalize();
    }

    JavaInstallSelection.SelectionOutcome javaOutcome;
    try {
      JavaInstallSelection.InteractivePrompt javaPrompt =
          interactive && prompt != null ? prompt::readLine : null;
      javaOutcome =
          new JavaInstallSelection(installPath, unattendedJavaHome, javaPrompt).selectAndPersist();
      if (interactive && prompt != null) {
        prompt.println("DTS Java home selection: " + javaOutcome.summary());
      }
    } catch (JavaInstallSelection.JavaSelectionException sel) {
      return WizardResult.abort(EXIT_JAVA, "DTS Java home selection failed: " + sel.getMessage());
    } catch (IOException io) {
      return WizardResult.abort(
          EXIT_JAVA,
          "Could not write java.properties at "
              + installPath.resolve("java.properties")
              + ": "
              + io.getMessage());
    }

    boolean upgrade = isUpgradeInstall(installPath);
    String isProduction;
    try {
      isProduction =
          resolveServerType(installPath, installProdDtsProperty, upgrade, interactive, prompt);
    } catch (IllegalArgumentException ex) {
      return WizardResult.abort(EXIT_ABORTED, ex.getMessage());
    }

    // Re-merge DB option defaults for the resolved DTS role (prod vs stage)
    new InstallerUserSettings(userSettingsHome, InstallerUserSettings.dtsPrefix(isProduction))
        .mergeMissingOptions(options);

    MainDTSPreInstall.ResolvedDbConfig dbConfig;
    try {
      if (interactive) {
        dbConfig = collectAndResolveDbInteractive(options, upgrade, prompt);
      } else {
        dbConfig = MainDTSPreInstall.resolveDbConfig(options);
      }
    } catch (IllegalArgumentException badDb) {
      return WizardResult.abort(
          EXIT_DB_CONFIG, "Database configuration error: " + badDb.getMessage());
    }

    if (interactive) {
      String summary = buildSummary(installPath, dbConfig, javaOutcome, isProduction);
      prompt.println(summary);
      boolean defaultYes = isDefaultYesConfirm(dbConfig);
      if (!confirmProceed(prompt, defaultYes)) {
        return WizardResult.abort(EXIT_ABORTED, "Installation cancelled by operator.");
      }
    }

    return WizardResult.proceed(
        installPath, Map.copyOf(options), dbConfig, javaOutcome, isProduction);
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
        "Optional: --db.type= --db.host= --db.port= --db.name= --db.user= --db.password=",
        "Silent mode: --silent or --no-tty (disables interactive prompts for automated testing)");
  }

  static boolean isUpgradeInstall(Path installPath) {
    if (installPath == null) {
      return false;
    }
    return Files.isDirectory(installPath.resolve("Deployment"))
        || Files.isDirectory(installPath.resolve("Staging"));
  }

  static boolean isDefaultYesConfirm(MainDTSPreInstall.ResolvedDbConfig dbConfig) {
    if (dbConfig == null || dbConfig.systemProperties() == null) {
      return true;
    }
    String type =
        dbConfig.systemProperties().getOrDefault("perc.db.type", MainDTSPreInstall.DB_TYPE_DEFAULT);
    String normalized = type == null ? "" : type.trim().toLowerCase(Locale.ROOT);
    return "h2".equals(normalized) || "derby".equals(normalized);
  }

  static String buildSummary(
      Path installPath,
      MainDTSPreInstall.ResolvedDbConfig dbConfig,
      JavaInstallSelection.SelectionOutcome javaOutcome,
      String isProduction) {
    List<String> lines = new ArrayList<>();
    lines.add("");
    lines.add("========================================");
    lines.add("Percussion DTS installation summary");
    lines.add("========================================");
    lines.add("Install path : " + installPath.toAbsolutePath().normalize());
    lines.add("Mode         : " + (isUpgradeInstall(installPath) ? "Upgrade" : "New install"));
    lines.add(
        "Server type  : " + ("true".equalsIgnoreCase(isProduction) ? "Production" : "Staging"));
    lines.add("Database     : " + formatDbSummary(dbConfig));
    lines.add(
        "Java home    : "
            + (javaOutcome != null
                ? javaOutcome.javaHome() + " (" + javaOutcome.source() + ")"
                : "unknown"));
    lines.add("========================================");
    return String.join(System.lineSeparator(), lines);
  }

  static String formatDbSummary(MainDTSPreInstall.ResolvedDbConfig dbConfig) {
    if (dbConfig == null) {
      return "(unknown)";
    }
    Map<String, String> p = dbConfig.systemProperties();
    String type = p.getOrDefault("perc.db.type", MainDTSPreInstall.DB_TYPE_DEFAULT);
    StringBuilder sb = new StringBuilder(type);
    appendIfPresent(sb, "host", p.get("perc.db.host"));
    appendIfPresent(sb, "port", p.get("perc.db.port"));
    appendIfPresent(sb, "name", p.get("perc.db.name"));
    appendIfPresent(sb, "user", p.get("perc.db.user"));
    return sb.toString();
  }

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

  static MainDTSPreInstall.ResolvedDbConfig collectAndResolveDbInteractive(
      Map<String, String> options, boolean upgrade, InstallPrompt prompt) {
    for (int attempt = 0; attempt < 5; attempt++) {
      Map<String, String> collected =
          InteractiveDbConfigCollector.collect(options, upgrade, prompt);
      options.clear();
      options.putAll(collected);

      MainDTSPreInstall.ResolvedDbConfig dbConfig = MainDTSPreInstall.resolveDbConfig(options);

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
      options.remove("db.config.env.file");
      options.remove("db.type");
      options.remove("db.host");
      options.remove("db.port");
      options.remove("db.name");
      options.remove("db.schema");
      options.remove("db.user");
      options.remove("db.password");
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
   * Resolve production vs staging for DTS install.
   *
   * @return {@code "true"} for production, {@code "false"} for staging
   */
  static String resolveServerType(
      Path installPath,
      String installProdDtsProperty,
      boolean upgrade,
      boolean interactive,
      InstallPrompt prompt) {
    // Existing tree detection (historical Main behavior)
    Path staging = installPath.resolve("Staging");
    Path prod = installPath.resolve("Deployment");
    if (Files.exists(staging) && !Files.exists(prod)) {
      return "false";
    }

    if (installProdDtsProperty != null
        && !installProdDtsProperty.isBlank()
        && ("true".equalsIgnoreCase(installProdDtsProperty.trim())
            || "false".equalsIgnoreCase(installProdDtsProperty.trim()))) {
      return installProdDtsProperty.trim().toLowerCase(Locale.ROOT);
    }

    if (interactive && prompt != null && !upgrade) {
      prompt.println("");
      prompt.println("DTS server type");
      prompt.println("  [1] Production (default)");
      prompt.println("  [2] Staging");
      String choice = prompt.readLine("Select server type [1]: ").trim();
      if (choice.isEmpty() || "1".equals(choice)) {
        return "true";
      }
      if ("2".equals(choice)) {
        return "false";
      }
      throw new IllegalArgumentException("Invalid server type selection '" + choice + "'.");
    }

    // Historical default for non-interactive when unset
    return "true";
  }

  private static Path promptForInstallPath(InstallPrompt prompt, Path defaultPath) {
    prompt.println("");
    prompt.println("Percussion DTS interactive installer");
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

  /**
   * Outcome of the DTS wizard.
   *
   * @param proceed true when install may continue
   * @param installPath resolved absolute install path when proceed; may be {@code null} when {@code
   *     !proceed}
   * @param options CLI options map (possibly empty)
   * @param dbConfig resolved DB config when proceed; {@code null} when {@code !proceed}
   * @param javaOutcome selected Java home when proceed; {@code null} when {@code !proceed}
   * @param isProduction {@code "true"} or {@code "false"} for {@code install.prod.dts}; {@code
   *     null} when {@code !proceed}
   * @param exitCode process exit code when {@code !proceed}; {@code 0} when {@code proceed}
   * @param message operator-facing message when {@code !proceed}; {@code null} when {@code proceed}
   */
  public record WizardResult(
      boolean proceed,
      Path installPath,
      Map<String, String> options,
      MainDTSPreInstall.ResolvedDbConfig dbConfig,
      JavaInstallSelection.SelectionOutcome javaOutcome,
      String isProduction,
      int exitCode,
      String message) {

    /**
     * Build a "proceed" result carrying the resolved install configuration.
     *
     * @param installPath resolved absolute install path
     * @param options CLI options map (must be non-null)
     * @param dbConfig resolved DB config
     * @param javaOutcome selected Java home outcome
     * @param isProduction {@code "true"} or {@code "false"}
     * @return a successful wizard result
     */
    static WizardResult proceed(
        Path installPath,
        Map<String, String> options,
        MainDTSPreInstall.ResolvedDbConfig dbConfig,
        JavaInstallSelection.SelectionOutcome javaOutcome,
        String isProduction) {
      return new WizardResult(
          true, installPath, options, dbConfig, javaOutcome, isProduction, 0, null);
    }

    /**
     * Build an "abort" result carrying the operator-facing message and exit code.
     *
     * @param exitCode sysexits-style exit code for the wrapper
     * @param message operator-facing explanation; never {@code null}
     * @return an aborted wizard result
     */
    static WizardResult abort(int exitCode, String message) {
      return new WizardResult(false, null, Map.of(), null, null, null, exitCode, message);
    }
  }
}
