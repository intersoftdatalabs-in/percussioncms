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
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Interactive multi-step collection of repository database options for new CMS installs (issue
 * #1513 Phase 3). Populates the same structured {@code db.*} keys consumed by {@link
 * DbInstallConfigResolver}.
 *
 * <p>Does not print password values. Upgrade installs skip collection (existing repo config is
 * preserved by the install path).
 */
public final class InteractiveDbConfigCollector {

  private InteractiveDbConfigCollector() {}

  /**
   * Whether CLI already supplies an explicit database target that should skip field prompts.
   *
   * @param options CLI options map
   * @return true when dbprops path is set or structured host credentials are present
   */
  public static boolean hasExplicitDbOverride(Map<String, String> options) {
    if (options == null || options.isEmpty()) {
      return false;
    }
    if (!isBlank(options.get(DbInstallConfigResolver.DBPROPS_KEY))
        || !isBlank(options.get("db.props"))) {
      return true;
    }
    // Structured partial/full override: any of these means operator chose non-wizard input
    return !isBlank(options.get("db.host"))
        || !isBlank(options.get("db.user"))
        || !isBlank(options.get("db.password"))
        || (!isBlank(options.get("db.type"))
            && !"h2".equalsIgnoreCase(options.get("db.type").trim())
            && !"derby".equalsIgnoreCase(options.get("db.type").trim()));
  }

  /**
   * Collect database options interactively, merging into a copy of {@code existingOptions}.
   *
   * @param existingOptions CLI options already parsed (may be empty)
   * @param upgrade true when install root is an upgrade
   * @param prompt operator I/O
   * @return updated options map (never null)
   */
  public static Map<String, String> collect(
      Map<String, String> existingOptions, boolean upgrade, InstallPrompt prompt) {
    Objects.requireNonNull(prompt, "prompt");
    Map<String, String> options =
        existingOptions != null
            ? new LinkedHashMap<>(existingOptions)
            : new LinkedHashMap<>();

    if (upgrade) {
      prompt.println(
          "Upgrade detected: existing repository configuration will be preserved (database"
              + " target prompts skipped).");
      return options;
    }

    if (hasExplicitDbOverride(options)) {
      if (!isBlank(options.get(DbInstallConfigResolver.DBPROPS_KEY))) {
        prompt.println(
            "Using database properties file: " + options.get(DbInstallConfigResolver.DBPROPS_KEY));
      } else if (!isBlank(options.get("db.props"))) {
        prompt.println("Using database properties file: " + options.get("db.props"));
      } else {
        prompt.println(
            "Using database options supplied on the command line (interactive field prompts"
                + " skipped).");
      }
      return options;
    }

    prompt.println("");
    prompt.println("Database configuration (new install)");
    prompt.println("  [1] Embedded H2 (default — demo / small site)");
    prompt.println("  [2] SQL Server (incl. Express — small site / scale-to-corp path)");
    prompt.println("  [3] MySQL / MariaDB");
    prompt.println("  [4] PostgreSQL");
    prompt.println("  [5] Oracle");
    prompt.println("  [6] Load properties file (rxrepository.properties format)");
    String choice = prompt.readLine("Select database backend [1]: ").trim();
    if (choice.isEmpty()) {
      choice = "1";
    }

    switch (choice) {
      case "1" -> {
        options.put("db.type", "h2");
        clearStructuredAndSsl(options);
        prompt.println("Selected embedded H2.");
      }
      case "2" -> {
        options.put("db.type", "sqlserver");
        prompt.println(
            "SQL Server / Express: provision an empty database first. Express is suitable for"
                + " small sites (<~10 users, <~10 GB). Starting on SQL Server can avoid a later"
                + " H2 migration if you grow into a corporate SQL Server estate.");
        collectExternalFields(options, prompt, "1433", "dbo", "percussion");
      }
      case "3" -> {
        options.put("db.type", "mysql");
        collectExternalFields(options, prompt, "3306", "", "percussion");
      }
      case "4" -> {
        options.put("db.type", "postgresql");
        collectExternalFields(options, prompt, "5432", "public", "percussion");
      }
      case "5" -> {
        options.put("db.type", "oracle");
        collectExternalFields(options, prompt, "1521", "", "ORCL");
      }
      case "6" -> {
        String path =
            promptRequired(
                prompt, "Path to repository properties file: ", "Properties file path is required.");
        Path p = Paths.get(path).toAbsolutePath().normalize();
        if (!Files.isRegularFile(p) || !Files.isReadable(p)) {
          throw new IllegalArgumentException(
              "dbprops file not found or not readable: " + p);
        }
        options.put(DbInstallConfigResolver.DBPROPS_KEY, p.toString());
        options.remove("db.type");
        clearStructuredAndSsl(options);
      }
      default ->
          throw new IllegalArgumentException(
              "Invalid database selection '" + choice + "'. Choose 1-6.");
    }

    return options;
  }

  /**
   * Drop structured connection fields and SSL overrides so a backend switch (or H2 / dbprops path)
   * does not inherit stale keys from a prior attempt or CLI partial options.
   */
  static void clearStructuredAndSsl(Map<String, String> options) {
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

  private static void collectExternalFields(
      Map<String, String> options,
      InstallPrompt prompt,
      String defaultPort,
      String defaultSchema,
      String defaultName) {
    options.put(
        "db.host",
        promptWithDefault(prompt, "Database host", "localhost"));
    options.put(
        "db.port",
        promptWithDefault(prompt, "Database port", defaultPort));
    options.put(
        "db.name",
        promptWithDefault(prompt, "Database name (or Oracle service/SID)", defaultName));
    if (defaultSchema != null && !defaultSchema.isEmpty()) {
      options.put(
          "db.schema",
          promptWithDefault(prompt, "Schema", defaultSchema));
    } else {
      String schema = prompt.readLine("Schema (optional, Enter to skip): ").trim();
      if (!schema.isEmpty()) {
        options.put("db.schema", schema);
      } else {
        options.remove("db.schema");
      }
    }
    options.put(
        "db.user",
        promptRequired(prompt, "Database user: ", "Database user is required."));
    options.put("db.password", readPasswordToString(prompt));

    String sslEnabled =
        promptWithDefault(prompt, "SSL enabled (true/false)", DbInstallConfigResolver.DB_SSL_ENABLED_DEFAULT);
    options.put("db.ssl.enabled", normalizeBool(sslEnabled, DbInstallConfigResolver.DB_SSL_ENABLED_DEFAULT));
    String sslVerify =
        promptWithDefault(prompt, "SSL verify server cert (true/false)", DbInstallConfigResolver.DB_SSL_VERIFY_DEFAULT);
    options.put("db.ssl.verify", normalizeBool(sslVerify, DbInstallConfigResolver.DB_SSL_VERIFY_DEFAULT));
  }

  static String promptWithDefault(InstallPrompt prompt, String label, String defaultValue) {
    String line = prompt.readLine(label + " [" + defaultValue + "]: ");
    if (line == null || line.isBlank()) {
      return defaultValue;
    }
    return line.trim();
  }

  static String promptRequired(InstallPrompt prompt, String label, String errorMessage) {
    for (int i = 0; i < 5; i++) {
      String line = prompt.readLine(label);
      if (line != null && !line.isBlank()) {
        return line.trim();
      }
      prompt.println(errorMessage);
    }
    throw new IllegalArgumentException(errorMessage);
  }

  /**
   * Read a password as {@code char[]}; convert to String for the options map (CLI contract), then
   * zero the buffer. {@code null} from the prompt means no console — fail fast rather than
   * re-prompt forever.
   */
  static String readPasswordToString(InstallPrompt prompt) {
    char[] chars = prompt.readPassword("Database password: ");
    if (chars == null) {
      throw new IllegalArgumentException(
          "No console available to read database password (non-interactive environment).");
    }
    if (chars.length == 0) {
      Arrays.fill(chars, '\0');
      chars = prompt.readPassword("Password was empty. Re-enter database password: ");
      if (chars == null) {
        throw new IllegalArgumentException(
            "No console available to read database password (non-interactive environment).");
      }
    }
    try {
      return new String(chars);
    } finally {
      Arrays.fill(chars, '\0');
    }
  }

  private static String normalizeBool(String raw, String defaultValue) {
    if (raw == null || raw.isBlank()) {
      return defaultValue;
    }
    String v = raw.trim().toLowerCase(Locale.ROOT);
    if ("true".equals(v) || "yes".equals(v) || "y".equals(v) || "1".equals(v)) {
      return "true";
    }
    if ("false".equals(v) || "no".equals(v) || "n".equals(v) || "0".equals(v)) {
      return "false";
    }
    return defaultValue;
  }

  private static boolean isBlank(String s) {
    return s == null || s.isBlank();
  }
}
