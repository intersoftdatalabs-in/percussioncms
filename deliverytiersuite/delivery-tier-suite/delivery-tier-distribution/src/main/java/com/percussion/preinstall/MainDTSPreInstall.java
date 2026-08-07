/*
 * Copyright 1999-2026 Percussion Software, Inc.
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

package com.percussion.preinstall;

import com.percussion.security.validation.PathValidation;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Entry point for the DTS preinstall flow invoked by the installer JAR.
 *
 * <p>Pulls the install JAR's bundled {@code distribution/} payload to a temp directory, locates the
 * {@code perc-ant-*.jar} launcher inside the staged {@code rxconfig/Installer} tree, and execs the
 * underlying Ant {@code installDts.xml} target with the resolved Java home and database
 * configuration.
 *
 * <p>This class owns the unzipped layout, the final {@code java -jar} invocation, and the silent
 * (non-interactive) bypass path. The interactive path is delegated to {@link
 * InteractiveDtsInstallWizard} / {@code JavaInstallSelection}, which performs JVM selection and
 * returns the chosen Java home; this class consumes that outcome rather than selecting the JVM
 * itself.
 */
public class MainDTSPreInstall {

  private MainDTSPreInstall() {
    // Static-only utility.
  }

  private static final String DISTRIBUTION_DIR = "distribution";
  private static final String PERC_JAVA_HOME = "perc.java.home";
  private static final String JAVA_HOME = "java.home";
  private static final String PERCUSSION_VERSION = "perc.version";
  private static final String INSTALL_TEMPDIR = "percDTSInstallTmp_";
  private static final String PERC_ANT_JAR = "perc-ant";
  private static final String ANT_INSTALL = "installDts.xml";

  /** Default embedded engine after Apache Derby retirement (GitHub #548). */
  public static final String DB_TYPE_DEFAULT = "h2";

  /** Default SSL enabled value for new DTS installs. */
  public static final String DB_SSL_ENABLED_DEFAULT = "true";

  /** Default SSL verify value for new DTS installs. */
  public static final String DB_SSL_VERIFY_DEFAULT = "true";

  /** Default SSL allow-self-signed value for new DTS installs. */
  public static final String DB_SSL_ALLOW_SELF_SIGNED_DEFAULT = "false";

  /** Oracle JDBC driver class (parity with CMS {@code DbInstallConfigResolver}). */
  public static final String ORACLE_DRIVER_CLASS = "oracle.jdbc.OracleDriver";

  /** CLI key for silent/non-interactive mode (--silent or --no-tty). */
  public static final String SILENT_KEY = "silent";

  /**
   * Find a jar by path pattern to avoid hard coding / forcing version.
   *
   * @param execPath Folder containing the jar
   * @param fileNameWithPattern A File name with a glob pattern like perc-ant-*.jar
   * @return Path to the ant jar
   * @throws IOException
   */
  private static Path getVersionLessJarFilePath(Path execPath, String fileNameWithPattern)
      throws IOException {
    try (var ds = Files.newDirectoryStream(execPath.toAbsolutePath(), fileNameWithPattern)) {
      var paths = new ArrayList<Path>();
      for (var path : ds) {
        paths.add(path);
      }
      if (paths.isEmpty()) {
        throw new IOException(fileNameWithPattern + " not found.");
      } else if (paths.size() == 1) {
        return paths.get(0);
      } else {
        System.out.println(
            "Warning: Multiple "
                + fileNameWithPattern
                + " jars found, selecting the first one: "
                + paths.get(0).toAbsolutePath());
        return paths.get(0);
      }
    }
  }

  private static File tmpFolder;

  /**
   * Process entry point. Honors {@code --silent}/{@code --no-tty}, drives the interactive wizard
   * when a console is attached, then stages the install payload and execs the embedded Ant install.
   * Non-zero Ant exit codes and unexpected failures surface as {@link AntJobFailedException}.
   *
   * @param args CLI arguments; the first non-flag token is treated as the install path
   */
  public static void main(String[] args) {
    int exitCode = 0;
    try {
      ParsedArgs parsedArgs = parseArgs(args);
      boolean silent = isSilentMode(parsedArgs.options());
      // Issue #1513 Phase 4: interactive path / Java / server type / DB / confirm when TTY.
      boolean interactive =
          InteractiveDtsInstallWizard.isInteractive(silent, System.console() != null);
      InteractiveDtsInstallWizard.WizardResult wizard =
          InteractiveDtsInstallWizard.run(
              parsedArgs,
              interactive,
              SystemConsoleInstallPrompt.INSTANCE,
              parseUnattendedJavaHome(System.getProperty(PERC_JAVA_HOME)),
              System.getProperty("install.prod.dts"));
      if (!wizard.proceed()) {
        if (wizard.message() != null && !wizard.message().isBlank()) {
          System.out.println(wizard.message());
        }
        System.exit(wizard.exitCode());
        return;
      }

      var installPath = wizard.installPath();
      ResolvedDbConfig resolvedDbConfig = wizard.dbConfig();
      var isProduction = wizard.isProduction();
      Map<String, String> options =
          wizard.options() != null ? new HashMap<>(wizard.options()) : new HashMap<>();
      if (wizard.javaOutcome() != null) {
        System.out.println("DTS Java home selection: " + wizard.javaOutcome().summary());
      }

      var javaHome =
          wizard.javaOutcome() != null && wizard.javaOutcome().javaHome() != null
              ? wizard.javaOutcome().javaHome().toString()
              : System.getProperty(PERC_JAVA_HOME);
      if (javaHome == null || javaHome.trim().isEmpty()) {
        javaHome = System.getProperty(JAVA_HOME);
      }

      var javabin =
          System.getProperty("file.separator").equals("/")
              ? javaHome + "/bin/java"
              : javaHome + "/bin/java.exe";

      var percVersion = System.getProperty(PERCUSSION_VERSION);
      if (percVersion == null) {
        percVersion = "";
      }

      System.out.println("perc.java.home=" + javaHome);
      System.out.println("java.executable=" + javabin);
      System.out.println("perc.version=" + percVersion);
      System.out.println("Installation folder =" + installPath);
      System.out.println("install.prod.dts=" + isProduction);

      Path installSrc;
      var currentJar =
          Paths.get(
              MainDTSPreInstall.class.getProtectionDomain().getCodeSource().getLocation().toURI());
      if (!Files.isDirectory(currentJar)) {
        installSrc = Files.createTempDirectory(INSTALL_TEMPDIR);
        System.out.println("install.tempdir=" + installSrc);
        // Add option to not delete for debugging
        Runtime.getRuntime()
            .addShutdownHook(
                new Thread(
                    () -> {
                      try {
                        Files.walk(installSrc)
                            .sorted(Comparator.reverseOrder())
                            .map(Path::toFile)
                            .forEach(File::delete);
                      } catch (IOException ex) {
                        System.out.println(
                            "An error occurred processing installation files. " + ex.getMessage());
                      }
                    }));

        extractArchive(currentJar, installSrc, DISTRIBUTION_DIR);
      } else {
        System.out.println("Running from extracted jar");
        installSrc = currentJar.resolve(DISTRIBUTION_DIR);
      }

      var execPath = installSrc.resolve(Paths.get("rxconfig", "Installer"));
      var installAntJarPath =
          execPath.resolve(getVersionLessJarFilePath(execPath, PERC_ANT_JAR + "-*.jar"));

      exitCode = execJar(installAntJarPath, execPath, installPath, isProduction, resolvedDbConfig);

      if (exitCode == 0) {
        try {
          String javaHomeToSave =
              wizard.javaOutcome() != null && wizard.javaOutcome().javaHome() != null
                  ? wizard.javaOutcome().javaHome().toString()
                  : javaHome;
          new InstallerUserSettings(InstallerUserSettings.dtsPrefix(isProduction))
              .save(
                  installPath,
                  percVersion,
                  options,
                  javaHomeToSave,
                  resolvedDbConfig != null ? resolvedDbConfig.systemProperties() : null);
        } catch (Exception saveEx) {
          System.out.println(
              "Warning: could not save installer user settings: " + saveEx.getMessage());
        }
      }

    } catch (Exception e) {
      System.out.println(
          "An unexpected error occurred processing installation files. " + e.getMessage());
      throw new AntJobFailedException(String.format("Installation failed. %s", e.getMessage()));
    }
    System.out.println(String.format("Done extracting exit code %d", exitCode));
    if (exitCode != 0) {
      throw new AntJobFailedException(
          String.format("Installation failed. Exit code: %d ", exitCode));
    }
  }

  /**
   * Extract the {@code distribution/} (or any prefix-matching) entries from {@code archiveFile}
   * into {@code destPath}, validating each entry path against ZipSlip before writing (CWE-22).
   * Shell-script entries ({@code .sh}) have the executable bit set on POSIX-y hosts so the
   * installer can invoke them later.
   *
   * @param archiveFile the install JAR or ZIP to read; must be a readable archive
   * @param destPath the directory to receive the extracted files; created if missing
   * @param folderPrefix the entry-name prefix to extract (e.g. {@code "distribution"})
   * @throws IOException when the archive cannot be read or a file write fails
   */
  public static void extractArchive(Path archiveFile, Path destPath, String folderPrefix)
      throws IOException {
    Files.createDirectories(destPath); // create dest path folder(s)

    try (var archive = new ZipFile(archiveFile.toFile())) {
      // Sort entries by name to always create folders first
      var entries =
          archive.stream()
              .sorted(Comparator.comparing(ZipEntry::getName))
              .collect(Collectors.toList());

      // Copy each entry in the dest path
      for (var entry : entries) {
        var entryName = entry.getName();
        if (!entryName.startsWith(folderPrefix)) {
          continue;
        }

        var name = entryName.substring(folderPrefix.length() + 1);
        if (name.isEmpty()) {
          continue;
        }

        try {
          // CWE-22: Validate zip entry path to prevent ZipSlip attacks
          File baseDir = destPath.toFile();
          File safeFile = PathValidation.constructSafePath(baseDir, name);
          Path entryDest = safeFile.toPath();

          if (entry.isDirectory()) {
            Files.createDirectory(entryDest);
            continue;
          }
          System.out.println("Creating file " + entryDest);
          Files.copy(archive.getInputStream(entry), entryDest);
          // Preserve executable permissions for shell scripts (v8.1.7 #515 / GH-510)
          if (entryName.endsWith(".sh")) {
            File sh = entryDest.toFile();
            if (!sh.setExecutable(true, false)) {
              System.out.println("Warning: could not set executable bit on " + entryDest);
            }
          }
        } catch (PathValidation.SecurityException | SecurityException se) {
          // ZipSlip / path traversal rejected by PathValidation (CWE-22).
          // PathValidation throws its nested SecurityException (RuntimeException),
          // not java.lang.SecurityException — catch both so either form is skipped.
          System.out.println("Security: Rejected malicious zip entry: " + entryName);
        }
      }
    }
  }

  /**
   * Spawn a child JVM that runs the embedded {@code installDts.xml} target via {@code perc-ant}.
   * Inherits the parent's I/O so the install output streams straight to the operator console.
   *
   * @param jar absolute path to the {@code perc-ant-*.jar} to invoke
   * @param execPath working directory for the child JVM (typically {@code rxconfig/Installer})
   * @param installDir absolute path to the installation root passed as {@code -Drxdeploydir}
   * @param isProduction {@code "true"} or {@code "false"} mapped to {@code -Dinstall.prod.dts}
   * @param resolvedDbConfig the {@code perc.db.*} system properties to forward to the child JVM
   * @return the child JVM's exit code; {@code 0} indicates success
   * @throws IOException if the child process cannot be started
   * @throws InterruptedException if the calling thread is interrupted while waiting
   */
  public static int execJar(
      Path jar,
      Path execPath,
      Path installDir,
      String isProduction,
      ResolvedDbConfig resolvedDbConfig)
      throws IOException, InterruptedException {

    var dir = installDir.toAbsolutePath().toString();
    var javaHome = System.getProperty(PERC_JAVA_HOME);
    if (javaHome == null || javaHome.trim().isEmpty()) {
      javaHome = System.getProperty(JAVA_HOME);
    }

    var javabin =
        System.getProperty("file.separator").equals("/")
            ? javaHome + "/bin/java"
            : javaHome + "/bin/java.exe";

    System.out.println("isProduction:" + isProduction);
    System.out.println("Install Dir:" + dir);
    System.out.println("Java Executable:" + javabin);

    List<String> command = new ArrayList<>();
    command.add(javabin);
    command.add("-Dinstall.prod.dts=" + isProduction);
    command.add("-Dfile.encoding=UTF8");
    command.add("-Dsun.jnu.encoding=UTF8");
    command.add("-Dinstall.dir=" + dir);
    command.add("-Drxdeploydir=" + dir);
    for (Map.Entry<String, String> entry : resolvedDbConfig.systemProperties().entrySet()) {
      command.add("-D" + entry.getKey() + "=" + entry.getValue());
    }
    command.add("-jar");
    command.add(jar.toAbsolutePath().toString());
    command.add("-f");
    command.add(ANT_INSTALL);

    var builder = new ProcessBuilder(command).directory(execPath.toFile());
    var process = builder.inheritIO().start();
    process.waitFor();
    return process.exitValue();
  }

  static ParsedArgs parseArgs(String[] args) {
    Path installPath = null;
    Map<String, String> options = new HashMap<>();

    int index = 0;
    while (index < args.length) {
      String argument = args[index];
      if (!argument.startsWith("--")) {
        if (installPath == null) {
          installPath = Paths.get(argument);
        }
        index++;
        continue;
      }

      String option = argument.substring(2);
      String key;
      String value;
      int equalsPosition = option.indexOf('=');
      if (equalsPosition > -1) {
        key = option.substring(0, equalsPosition).trim();
        value = option.substring(equalsPosition + 1).trim();
      } else {
        key = option.trim();
        if (index + 1 < args.length && !args[index + 1].startsWith("--")) {
          value = args[index + 1].trim();
          index++;
        } else {
          value = "true";
        }
      }
      if (!key.isEmpty()) {
        options.put(key, value);
      }
      index++;
    }

    return new ParsedArgs(installPath, options);
  }

  /**
   * Treat -Dperc.java.home=... as the unattended input to the Java home selector. Returns {@code
   * null} when unset so the discovery / interactive path takes over.
   */
  static java.nio.file.Path parseUnattendedJavaHome(String raw) {
    if (raw == null) {
      return null;
    }
    String trimmed = raw.trim();
    if (trimmed.isEmpty() || "${perc.java.home}".equals(trimmed)) {
      return null;
    }
    return java.nio.file.Path.of(trimmed);
  }

  /**
   * Check if silent/non-interactive mode is enabled via --silent or --no-tty CLI options.
   *
   * @param options parsed CLI options
   * @return true if silent mode is enabled
   */
  /**
   * Check if silent/non-interactive mode is enabled via --silent or --no-tty CLI options.
   *
   * @param options parsed CLI options
   * @return true if silent mode is enabled
   */
  static boolean isSilentMode(Map<String, String> options) {
    if (options == null) {
      return false;
    }
    String silent = options.get(SILENT_KEY);
    String noTty = options.get("no-tty");
    return "true".equalsIgnoreCase(silent)
        || "yes".equalsIgnoreCase(silent)
        || "1".equals(silent)
        || "true".equalsIgnoreCase(noTty)
        || "yes".equalsIgnoreCase(noTty)
        || "1".equals(noTty);
  }

  /**
   * Resolve DTS database configuration into {@code perc.db.*} system properties for the ANT install
   * JVM.
   *
   * <p>Supported structured {@code db.type} values match CMS installer parity: {@code h2}, {@code
   * derby}, {@code mysql}, {@code sqlserver}, {@code oracle}, {@code postgresql} (aliases {@code
   * mssql}, {@code ora}, {@code postgres}). Unknown types fail fast — they must not silently leave
   * DTS on the embedded H2 default when the operator requested an external RDBMS (issue #2338).
   *
   * @param cliOptions options from {@link #parseArgs(String[])}
   * @return resolved config (never null)
   */
  static ResolvedDbConfig resolveDbConfig(Map<String, String> cliOptions) {
    if (cliOptions == null) {
      cliOptions = Map.of();
    }
    Map<String, String> envFileValues = new HashMap<>();
    String envFilePath =
        firstNonBlank(
            cliOptions.get("db.config.env.file"),
            System.getenv("DB_CONFIG_ENV_FILE"),
            System.getenv("PERC_DB_CONFIG_ENV_FILE"));
    if (envFilePath != null) {
      envFileValues.putAll(loadEnvFile(envFilePath));
    }

    String dbTypeRaw = getConfigValue("db.type", cliOptions, envFileValues, DB_TYPE_DEFAULT);
    String dbTypeNormalized = normalizeStructuredDbType(dbTypeRaw);
    String sslEnabled =
        normalizeBoolean(
            getConfigValue("db.ssl.enabled", cliOptions, envFileValues, DB_SSL_ENABLED_DEFAULT),
            "db.ssl.enabled");
    String sslVerify =
        normalizeBoolean(
            getConfigValue("db.ssl.verify", cliOptions, envFileValues, DB_SSL_VERIFY_DEFAULT),
            "db.ssl.verify");
    String sslAllowSelfSigned =
        normalizeBoolean(
            getConfigValue(
                "db.ssl.allowSelfSigned",
                cliOptions,
                envFileValues,
                DB_SSL_ALLOW_SELF_SIGNED_DEFAULT),
            "db.ssl.allowSelfSigned");

    Map<String, String> systemProperties = new HashMap<>();
    systemProperties.put("perc.db.type", dbTypeNormalized);
    systemProperties.put("perc.db.ssl.enabled", sslEnabled);
    systemProperties.put("perc.db.ssl.verify", sslVerify);
    systemProperties.put("perc.db.ssl.allowSelfSigned", sslAllowSelfSigned);

    setIfPresent(
        systemProperties,
        "perc.db.host",
        getConfigValue("db.host", cliOptions, envFileValues, null));
    setIfPresent(
        systemProperties,
        "perc.db.port",
        getConfigValue("db.port", cliOptions, envFileValues, null));
    setIfPresent(
        systemProperties,
        "perc.db.name",
        getConfigValue("db.name", cliOptions, envFileValues, null));
    setIfPresent(
        systemProperties,
        "perc.db.schema",
        getConfigValue("db.schema", cliOptions, envFileValues, null));
    setIfPresent(
        systemProperties,
        "perc.db.user",
        getConfigValue("db.user", cliOptions, envFileValues, null));
    setIfPresent(
        systemProperties,
        "perc.db.password",
        getConfigValue("db.password", cliOptions, envFileValues, null));
    setIfPresent(
        systemProperties,
        "perc.db.ssl.trustStorePath",
        getConfigValue("db.ssl.trustStorePath", cliOptions, envFileValues, null));
    setIfPresent(
        systemProperties,
        "perc.db.ssl.trustStorePassword",
        getConfigValue("db.ssl.trustStorePassword", cliOptions, envFileValues, null));

    String host = systemProperties.get("perc.db.host");
    String port = systemProperties.get("perc.db.port");
    String name = systemProperties.get("perc.db.name");
    String user = systemProperties.get("perc.db.user");
    String password = systemProperties.get("perc.db.password");
    String schema = systemProperties.get("perc.db.schema");

    // Embedded engines (H2 default, Derby migration window) do not require host/port/name.
    boolean embedded = "h2".equals(dbTypeNormalized) || "derby".equals(dbTypeNormalized);
    if (!embedded) {
      List<String> missing = new ArrayList<>();
      if (isBlank(host)) {
        missing.add("db.host");
      }
      if (isBlank(port)) {
        missing.add("db.port");
      }
      if (isBlank(name)) {
        missing.add("db.name");
      }
      if (isBlank(user)) {
        missing.add("db.user");
      }
      if (isBlank(password)) {
        missing.add("db.password");
      }
      if (!missing.isEmpty()) {
        throw new IllegalArgumentException(
            "Missing required database parameters for db.type="
                + dbTypeNormalized
                + ": "
                + String.join(", ", missing));
      }
    }

    if ("mysql".equals(dbTypeNormalized)) {
      String dtsJdbcUrl =
          "jdbc:mysql://"
              + host
              + ":"
              + port
              + "/"
              + name
              + "?useUnicode=yes&characterEncoding=UTF-8"
              + "&useSSL="
              + sslEnabled
              + "&requireSSL="
              + sslEnabled
              + "&verifyServerCertificate="
              + sslVerify;
      systemProperties.put("perc.db.dts.jdbcUrl", dtsJdbcUrl);
      systemProperties.put("perc.db.dts.jdbcDriver", "com.mysql.cj.jdbc.Driver");
      systemProperties.put(
          "perc.db.dts.hibernateDialect", "org.hibernate.dialect.MySQL5InnoDBDialect");
      // Schema may be empty for MySQL; do not use firstNonBlank("", ...) (empty is blank).
      systemProperties.put("perc.db.dts.schema", schema == null ? "" : schema);
    } else if ("sqlserver".equals(dbTypeNormalized)) {
      String trustServerCertificate =
          ("true".equals(sslAllowSelfSigned) || "false".equals(sslVerify)) ? "true" : "false";
      String dtsJdbcUrl =
          "jdbc:sqlserver://"
              + host
              + ":"
              + port
              + ";databaseName="
              + name
              + ";encrypt="
              + sslEnabled
              + ";trustServerCertificate="
              + trustServerCertificate;
      systemProperties.put("perc.db.dts.jdbcUrl", dtsJdbcUrl);
      systemProperties.put(
          "perc.db.dts.jdbcDriver", "com.microsoft.sqlserver.jdbc.SQLServerDriver");
      systemProperties.put(
          "perc.db.dts.hibernateDialect",
          "com.percussion.delivery.rdbms.PSUnicodeSQLServerDialect");
      systemProperties.put("perc.db.dts.schema", firstNonBlank(schema, "DBO"));
    } else if ("postgresql".equals(dbTypeNormalized)) {
      String dtsJdbcUrl = "jdbc:postgresql://" + host + ":" + port + "/" + name;
      systemProperties.put("perc.db.dts.jdbcUrl", dtsJdbcUrl);
      systemProperties.put("perc.db.dts.jdbcDriver", "org.postgresql.Driver");
      systemProperties.put(
          "perc.db.dts.hibernateDialect", "org.hibernate.dialect.PostgreSQLDialect");
      systemProperties.put("perc.db.dts.schema", firstNonBlank(schema, "public"));
    } else if ("oracle".equals(dbTypeNormalized)) {
      // Easy Connect service form: @//host:port/serviceOrSid — same as CMS DbInstallConfigResolver
      // (multi-tenant PDB service names such as XEPDB1 are not classic SID forms).
      String serviceOrSid = name == null ? "" : name.trim();
      String resolvedSchema =
          isBlank(schema) ? (user == null ? "" : user.trim()) : schema.trim();
      String dtsJdbcUrl =
          "jdbc:oracle:thin:@//" + host + ":" + port + "/" + serviceOrSid;
      systemProperties.put("perc.db.dts.jdbcUrl", dtsJdbcUrl);
      systemProperties.put("perc.db.dts.jdbcDriver", ORACLE_DRIVER_CLASS);
      systemProperties.put(
          "perc.db.dts.hibernateDialect", "org.hibernate.dialect.Oracle12cDialect");
      systemProperties.put("perc.db.dts.schema", resolvedSchema);
    }

    return new ResolvedDbConfig(systemProperties);
  }

  /**
   * Normalize structured installer {@code db.type} values (CMS parity).
   *
   * @param dbTypeRaw raw type from CLI / env / default
   * @return canonical lower-case type
   * @throws IllegalArgumentException when the type is not a supported DTS backend
   */
  static String normalizeStructuredDbType(String dbTypeRaw) {
    if (isBlank(dbTypeRaw)) {
      return DB_TYPE_DEFAULT;
    }
    String t = dbTypeRaw.trim().toLowerCase(Locale.ROOT);
    return switch (t) {
      case "h2", "derby", "mysql", "sqlserver", "oracle", "postgresql" -> t;
      case "mssql" -> "sqlserver";
      case "ora" -> "oracle";
      case "postgres" -> "postgresql";
      default ->
          throw new IllegalArgumentException(
              "Unknown db.type='"
                  + dbTypeRaw
                  + "'. Allowed values: h2, derby, mysql, sqlserver, oracle, postgresql");
    };
  }

  /**
   * Whether {@code installDts.xml} should rewrite {@code perc-datasources.properties} for this
   * backend (fresh install, non-embedded).
   *
   * @param dbType normalized {@code perc.db.type}
   * @return true when external RDBMS datasource keys must be written
   */
  static boolean shouldWriteDtsDatasourceProperties(String dbType) {
    if (isBlank(dbType)) {
      return false;
    }
    String t = dbType.trim().toLowerCase(Locale.ROOT);
    return !"h2".equals(t) && !"derby".equals(t);
  }

  /**
   * Map resolved {@code perc.db.*} system properties to the {@code perc-datasources.properties}
   * keys written by {@code installDts.xml} on fresh external-DB installs.
   *
   * <p>Keeps the Java resolve path and the ANT propertyfile write contract aligned for unit tests
   * (issue #2338).
   *
   * @param systemProperties from {@link ResolvedDbConfig#systemProperties()}
   * @return ordered map of datasource property keys (never null)
   */
  static Map<String, String> dtsDatasourcePropertyEntries(Map<String, String> systemProperties) {
    Map<String, String> entries = new java.util.LinkedHashMap<>();
    if (systemProperties == null) {
      return entries;
    }
    entries.put("db.username", nullToEmpty(systemProperties.get("perc.db.user")));
    entries.put("db.password", nullToEmpty(systemProperties.get("perc.db.password")));
    entries.put("db.schema", nullToEmpty(systemProperties.get("perc.db.dts.schema")));
    entries.put("jdbcDriver", nullToEmpty(systemProperties.get("perc.db.dts.jdbcDriver")));
    entries.put("jdbcUrl", nullToEmpty(systemProperties.get("perc.db.dts.jdbcUrl")));
    entries.put(
        "hibernate.dialect", nullToEmpty(systemProperties.get("perc.db.dts.hibernateDialect")));
    entries.put("db.ssl.enabled", nullToEmpty(systemProperties.get("perc.db.ssl.enabled")));
    entries.put("db.ssl.verify", nullToEmpty(systemProperties.get("perc.db.ssl.verify")));
    entries.put(
        "db.ssl.allowSelfSigned",
        nullToEmpty(systemProperties.get("perc.db.ssl.allowSelfSigned")));
    return entries;
  }

  /**
   * Write DTS datasource keys into a properties file using the same entry set as {@code
   * installDts.xml}. Existing keys in the target file are preserved except those overwritten by
   * this mapping.
   *
   * @param targetFile path to {@code perc-datasources.properties} (parent dirs must exist)
   * @param systemProperties resolved {@code perc.db.*} map
   * @throws IOException if the file cannot be read or written
   */
  static void writeDtsDatasourceProperties(Path targetFile, Map<String, String> systemProperties)
      throws IOException {
    java.util.Properties props = new java.util.Properties();
    if (Files.isRegularFile(targetFile)) {
      try (var in = Files.newInputStream(targetFile)) {
        props.load(in);
      }
    }
    for (Map.Entry<String, String> e : dtsDatasourcePropertyEntries(systemProperties).entrySet()) {
      props.setProperty(e.getKey(), e.getValue());
    }
    try (var out = Files.newOutputStream(targetFile)) {
      props.store(out, "DTS datasource configuration (MainDTSPreInstall)");
    }
  }

  private static String nullToEmpty(String value) {
    return value == null ? "" : value;
  }

  private static Map<String, String> loadEnvFile(String envFilePath) {
    Map<String, String> values = new HashMap<>();
    Path path = Paths.get(envFilePath);
    if (!Files.exists(path)) {
      throw new IllegalArgumentException("db.config.env.file not found: " + envFilePath);
    }
    try {
      for (String line : Files.readAllLines(path)) {
        if (line == null) {
          continue;
        }
        String trimmed = line.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
          continue;
        }
        int separator = trimmed.indexOf('=');
        if (separator <= 0) {
          continue;
        }
        values.put(trimmed.substring(0, separator).trim(), trimmed.substring(separator + 1).trim());
      }
      return values;
    } catch (IOException e) {
      throw new IllegalArgumentException("Unable to read db.config.env.file: " + envFilePath, e);
    }
  }

  private static String getConfigValue(
      String logicalKey,
      Map<String, String> cliOptions,
      Map<String, String> envFileValues,
      String defaultValue) {
    String envStyle = logicalToEnvStyle(logicalKey);
    String percEnvStyle = "PERC_" + envStyle;
    return firstNonBlank(
        cliOptions.get(logicalKey),
        cliOptions.get(envStyle),
        cliOptions.get(percEnvStyle),
        envFileValues.get(logicalKey),
        envFileValues.get(envStyle),
        envFileValues.get(percEnvStyle),
        System.getenv(logicalKey),
        System.getenv(envStyle),
        System.getenv(percEnvStyle),
        defaultValue);
  }

  private static String logicalToEnvStyle(String logicalKey) {
    return logicalKey.toUpperCase(Locale.ROOT).replace('.', '_');
  }

  private static String normalizeBoolean(String value, String key) {
    if (isBlank(value)) {
      throw new IllegalArgumentException("Missing required boolean value for " + key);
    }
    if ("true".equalsIgnoreCase(value)) {
      return "true";
    }
    if ("false".equalsIgnoreCase(value)) {
      return "false";
    }
    throw new IllegalArgumentException("Invalid boolean value for " + key + ": " + value);
  }

  private static boolean isBlank(String value) {
    return value == null || value.trim().isEmpty();
  }

  private static String firstNonBlank(String... values) {
    for (String value : values) {
      if (!isBlank(value)) {
        return value.trim();
      }
    }
    return null;
  }

  private static void setIfPresent(Map<String, String> target, String key, String value) {
    if (!isBlank(value)) {
      target.put(key, value.trim());
    }
  }

  /**
   * CLI arguments parsed from the DTS preinstall invocation.
   *
   * @param installPath absolute or relative install path (may be {@code null} for fully interactive
   *     mode)
   * @param options map of {@code --key[=value]} CLI options (e.g. {@code silent}, {@code db.*})
   */
  public record ParsedArgs(Path installPath, Map<String, String> options) {}

  /**
   * Resolved database configuration for the ANT install JVM.
   *
   * @param systemProperties map of {@code perc.db.*} keys for the ANT JVM
   */
  public record ResolvedDbConfig(Map<String, String> systemProperties) {}
}
