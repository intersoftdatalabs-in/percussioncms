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

import com.intsof.common.utilities.AppConfigurationFolder;
import com.intsof.common.utilities.UserConfiguration;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

/**
 * Load and save non-secret installer defaults under {@code
 * ~/.intsof/percussion/last-install.properties}.
 *
 * <p>Keys are namespaced by a product/role prefix ({@link #PREFIX_CMS}, {@link #PREFIX_DTS_PROD},
 * {@link #PREFIX_DTS_STAGE}). Saves merge only keys under the active prefix so sibling roles are
 * preserved. Passwords and keystore secrets are never written.
 */
public final class InstallerUserSettings {

  /** Application folder under {@code ~/.intsof}. */
  public static final String APPLICATION_NAME = "percussion";

  /** Shared settings file name. */
  public static final String SETTINGS_FILE = "last-install.properties";

  /** CMS property prefix (e.g. {@code cms.install.directory}). */
  public static final String PREFIX_CMS = "cms.";

  /** DTS production property prefix. */
  public static final String PREFIX_DTS_PROD = "dts.prod.";

  /** DTS staging property prefix. */
  public static final String PREFIX_DTS_STAGE = "dts.stage.";

  /** Property key for the resolved install directory. */
  public static final String KEY_INSTALL_DIRECTORY = "install.directory";

  /** Property key for the resolved product version. */
  public static final String KEY_VERSION = "version";

  /** Property key for the resolved {@code JAVA_HOME}. */
  public static final String KEY_JAVA_HOME = "java.home";

  /** Option keys (without prefix) that may be persisted / restored. Never includes passwords. */
  public static final Set<String> PERSISTABLE_OPTION_KEYS =
      Set.of(
          "db.type",
          "db.host",
          "db.port",
          "db.name",
          "db.schema",
          "db.user",
          "db.ssl.enabled",
          "db.ssl.verify",
          "db.ssl.allowSelfSigned",
          "db.ssl.trustStorePath",
          "db.ssl.keyStorePath",
          DbInstallConfigResolver.DEMO_SITES_KEY);

  private static final Set<String> FORBIDDEN_SUFFIXES =
      Set.of(
          "db.password", "db.ssl.trustStorePassword", "db.ssl.keyStorePassword", "password", "pwd");

  private final Path userHomeOverride;
  private final String prefix;

  /**
   * Settings for the given prefix using the real user home.
   *
   * @param prefix e.g. {@link #PREFIX_CMS}
   */
  public InstallerUserSettings(String prefix) {
    this(null, prefix);
  }

  /**
   * Settings with an injectable user home (unit tests).
   *
   * @param userHomeOverride home directory containing {@code .intsof}, or null for default
   * @param prefix property key prefix including trailing dot
   */
  public InstallerUserSettings(Path userHomeOverride, String prefix) {
    this.userHomeOverride = userHomeOverride;
    this.prefix = Objects.requireNonNull(prefix, "prefix");
    if (prefix.isBlank() || !prefix.endsWith(".")) {
      throw new IllegalArgumentException("prefix must be non-blank and end with '.': " + prefix);
    }
  }

  /**
   * Map DTS production flag to the settings prefix.
   *
   * @param isProduction {@code "true"} for production, otherwise staging
   * @return {@link #PREFIX_DTS_PROD} or {@link #PREFIX_DTS_STAGE}
   */
  public static String dtsPrefix(String isProduction) {
    return "true".equalsIgnoreCase(isProduction) ? PREFIX_DTS_PROD : PREFIX_DTS_STAGE;
  }

  /**
   * Apply saved defaults to a parsed CLI result: fill install path and missing option keys only.
   * Explicit CLI values always win.
   *
   * @param parsedArgs CLI parse result
   * @return new parse result with defaults merged
   */
  public DbInstallConfigResolver.ParsedArgs applyDefaults(
      DbInstallConfigResolver.ParsedArgs parsedArgs) {
    Objects.requireNonNull(parsedArgs, "parsedArgs");
    Map<String, String> options =
        parsedArgs.options() != null
            ? new LinkedHashMap<>(parsedArgs.options())
            : new LinkedHashMap<>();
    Map<String, String> saved = loadOptionDefaults();
    for (Map.Entry<String, String> e : saved.entrySet()) {
      if (!options.containsKey(e.getKey()) && e.getValue() != null && !e.getValue().isBlank()) {
        options.put(e.getKey(), e.getValue());
      }
    }
    Path installPath = parsedArgs.installPath();
    if (installPath == null) {
      Optional<Path> savedPath = loadInstallDirectory();
      if (savedPath.isPresent()) {
        installPath = savedPath.get();
      }
    }
    return new DbInstallConfigResolver.ParsedArgs(
        installPath, Collections.unmodifiableMap(options));
  }

  /**
   * Last saved install directory for this prefix, if any.
   *
   * @return absolute path when present and non-blank
   */
  public Optional<Path> loadInstallDirectory() {
    Properties props = loadAllProperties();
    String raw = props.getProperty(prefix + KEY_INSTALL_DIRECTORY);
    if (raw == null || raw.isBlank()) {
      return Optional.empty();
    }
    try {
      return Optional.of(Path.of(raw.trim()).toAbsolutePath().normalize());
    } catch (Exception ex) {
      return Optional.empty();
    }
  }

  /**
   * Last saved product version for this prefix, if any.
   *
   * @return version string
   */
  public Optional<String> loadVersion() {
    Properties props = loadAllProperties();
    String raw = props.getProperty(prefix + KEY_VERSION);
    if (raw == null || raw.isBlank()) {
      return Optional.empty();
    }
    return Optional.of(raw.trim());
  }

  /**
   * Load persistable option defaults (keys without prefix) for this role.
   *
   * @return map of installer option keys → values
   */
  public Map<String, String> loadOptionDefaults() {
    Properties props = loadAllProperties();
    Map<String, String> out = new LinkedHashMap<>();
    for (String suffix : PERSISTABLE_OPTION_KEYS) {
      String value = props.getProperty(prefix + suffix);
      if (value != null && !value.isBlank()) {
        out.put(suffix, value.trim());
      }
    }
    return Collections.unmodifiableMap(out);
  }

  /**
   * Merge-write non-secret settings for this prefix after a successful install.
   *
   * @param installDirectory absolute install root
   * @param productVersion version being installed (may be blank)
   * @param options effective CLI/options map (passwords ignored)
   * @param javaHome optional java home path string
   */
  public void save(
      Path installDirectory, String productVersion, Map<String, String> options, String javaHome) {
    save(installDirectory, productVersion, options, javaHome, null);
  }

  /**
   * Merge-write non-secret settings, also folding {@code perc.db.*} system properties from a
   * resolved DB config when option keys are missing.
   *
   * @param installDirectory absolute install root
   * @param productVersion version being installed (may be blank)
   * @param options effective CLI/options map (passwords ignored)
   * @param javaHome optional java home path string
   * @param percDbSystemProperties optional {@code perc.db.*} map from resolved DB config
   */
  public void save(
      Path installDirectory,
      String productVersion,
      Map<String, String> options,
      String javaHome,
      Map<String, String> percDbSystemProperties) {
    Objects.requireNonNull(installDirectory, "installDirectory");
    try {
      Map<String, String> merged = new LinkedHashMap<>();
      if (options != null) {
        merged.putAll(options);
      }
      mergePercDbIntoOptions(merged, percDbSystemProperties);

      Properties all = loadAllProperties();
      all.setProperty(
          prefix + KEY_INSTALL_DIRECTORY, installDirectory.toAbsolutePath().normalize().toString());
      if (productVersion != null && !productVersion.isBlank()) {
        all.setProperty(prefix + KEY_VERSION, productVersion.trim());
      }
      if (javaHome != null && !javaHome.isBlank()) {
        all.setProperty(prefix + KEY_JAVA_HOME, javaHome.trim());
      }
      for (String suffix : PERSISTABLE_OPTION_KEYS) {
        if (isForbiddenSuffix(suffix)) {
          continue;
        }
        String value = merged.get(suffix);
        if (value != null && !value.isBlank()) {
          all.setProperty(prefix + suffix, value.trim());
        }
      }
      // Defensive: strip any forbidden keys that may have been present historically
      stripForbiddenKeys(all);
      storeAllProperties(all);
    } catch (IOException ex) {
      System.out.println(
          "Warning: could not save installer user settings under ~/.intsof/percussion: "
              + ex.getMessage());
    }
  }

  /**
   * Copy non-secret {@code perc.db.*} values into installer option keys when not already present.
   *
   * @param options mutable options map
   * @param percDbSystemProperties resolved system properties; may be null
   */
  static void mergePercDbIntoOptions(
      Map<String, String> options, Map<String, String> percDbSystemProperties) {
    if (options == null || percDbSystemProperties == null) {
      return;
    }
    putIfAbsentFromPerc(options, percDbSystemProperties, "perc.db.type", "db.type");
    putIfAbsentFromPerc(options, percDbSystemProperties, "perc.db.host", "db.host");
    putIfAbsentFromPerc(options, percDbSystemProperties, "perc.db.port", "db.port");
    putIfAbsentFromPerc(options, percDbSystemProperties, "perc.db.name", "db.name");
    putIfAbsentFromPerc(options, percDbSystemProperties, "perc.db.schema", "db.schema");
    putIfAbsentFromPerc(options, percDbSystemProperties, "perc.db.user", "db.user");
    putIfAbsentFromPerc(options, percDbSystemProperties, "perc.db.ssl.enabled", "db.ssl.enabled");
    putIfAbsentFromPerc(options, percDbSystemProperties, "perc.db.ssl.verify", "db.ssl.verify");
    putIfAbsentFromPerc(
        options, percDbSystemProperties, "perc.db.ssl.allowSelfSigned", "db.ssl.allowSelfSigned");
    putIfAbsentFromPerc(
        options, percDbSystemProperties, "perc.db.ssl.trustStorePath", "db.ssl.trustStorePath");
    putIfAbsentFromPerc(
        options, percDbSystemProperties, "perc.db.ssl.keyStorePath", "db.ssl.keyStorePath");
  }

  private static void putIfAbsentFromPerc(
      Map<String, String> options, Map<String, String> percDb, String percKey, String optionKey) {
    if (options.containsKey(optionKey)) {
      return;
    }
    String value = percDb.get(percKey);
    if (value != null && !value.isBlank()) {
      options.put(optionKey, value.trim());
    }
  }

  private static boolean isForbiddenSuffix(String suffix) {
    if (suffix == null) {
      return true;
    }
    String lower = suffix.toLowerCase();
    if (FORBIDDEN_SUFFIXES.contains(lower)) {
      return true;
    }
    return lower.contains("password") || lower.endsWith(".pwd");
  }

  private void stripForbiddenKeys(Properties all) {
    for (String name : all.stringPropertyNames()) {
      if (!name.startsWith(prefix)) {
        continue;
      }
      String suffix = name.substring(prefix.length());
      if (isForbiddenSuffix(suffix)) {
        all.remove(name);
      }
    }
  }

  private Properties loadAllProperties() {
    Properties props = new Properties();
    try {
      UserConfiguration config = openConfig();
      Optional<AppConfigurationFolder> app = config.findApplication(APPLICATION_NAME);
      if (app.isEmpty()) {
        return props;
      }
      Optional<Path> file = app.get().get(SETTINGS_FILE);
      if (file.isEmpty() || !Files.isRegularFile(file.get())) {
        return props;
      }
      try (InputStream in = Files.newInputStream(file.get())) {
        props.load(in);
      }
    } catch (IOException | RuntimeException ex) {
      // Missing or unreadable settings is non-fatal for installs
    }
    return props;
  }

  private void storeAllProperties(Properties props) throws IOException {
    UserConfiguration config = openConfig();
    AppConfigurationFolder app = config.createApplication(APPLICATION_NAME);
    Path file = app.get(SETTINGS_FILE, true);
    try (OutputStream out = Files.newOutputStream(file)) {
      props.store(out, "Percussion installer non-secret defaults (cms / dts.prod / dts.stage)");
    }
  }

  private UserConfiguration openConfig() throws IOException {
    if (userHomeOverride != null) {
      return UserConfiguration.open(userHomeOverride);
    }
    return UserConfiguration.openDefault();
  }
}
