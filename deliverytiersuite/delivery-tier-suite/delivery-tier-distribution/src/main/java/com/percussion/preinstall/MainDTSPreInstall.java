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

public class MainDTSPreInstall {
  private static final String DISTRIBUTION_DIR = "distribution";
  private static final String PERC_JAVA_HOME = "perc.java.home";
  private static final String JAVA_HOME = "java.home";
  private static final String PERCUSSION_VERSION = "perc.version";
  private static final String INSTALL_TEMPDIR = "percDTSInstallTmp_";
  private static final String PERC_ANT_JAR = "perc-ant";
  private static final String ANT_INSTALL = "installDts.xml";
  private static final String DB_TYPE_DEFAULT = "derby";
  private static final String DB_SSL_ENABLED_DEFAULT = "true";
  private static final String DB_SSL_VERIFY_DEFAULT = "true";
  private static final String DB_SSL_ALLOW_SELF_SIGNED_DEFAULT = "false";

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

  public static void main(String[] args) {
    int exitCode = 0;
    try {
      var javaHome = System.getProperty(PERC_JAVA_HOME);
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

      ParsedArgs parsedArgs = parseArgs(args);
      if (parsedArgs.installPath() == null) {
        System.out.println("Must specify installation or upgrade folder");
        System.exit(0);
      }

      System.out.println("Installation folder =" + parsedArgs.installPath());
      var installPath = parsedArgs.installPath();
      ResolvedDbConfig resolvedDbConfig = resolveDbConfig(parsedArgs.options());
      var isProduction = System.getProperty("install.prod.dts");
      System.out.println(
          "====Will remove below code if value of is Production comes fine"
              + " PSDeliveryTierServerTYpePanel"
              + isProduction);

      var staging = installPath.toFile() + File.separator + "Staging";
      var f = new File(staging);
      var prod = installPath.toFile() + File.separator + "Deployment";
      var f2 = new File(prod);

      if (Files.exists(f.toPath()) && !Files.exists(f2.toPath())) {
        isProduction = "false";
      }

      // If isProduction value is not passed in and we are not able to figure out either, then set
      // the value to be true
      // e.g. in case of upgrade installer is passing value $DTS_SERVER_TYPE$, which doesn't match
      // any of the cases and thus fails
      if (isProduction == null
          || isProduction.isEmpty()
          || (!"true".equalsIgnoreCase(isProduction) && !"false".equalsIgnoreCase(isProduction))) {
        isProduction = "true"; // change done for dev environment
      }

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
        } catch (SecurityException se) {
          // ZipSlip attack detected - skip malicious entry
          System.out.println("Security: Rejected malicious zip entry: " + entryName);
        }
      }
    }
  }

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

  private static ParsedArgs parseArgs(String[] args) {
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

  private static ResolvedDbConfig resolveDbConfig(Map<String, String> cliOptions) {
    Map<String, String> envFileValues = new HashMap<>();
    String envFilePath =
        firstNonBlank(
            cliOptions.get("db.config.env.file"),
            System.getenv("DB_CONFIG_ENV_FILE"),
            System.getenv("PERC_DB_CONFIG_ENV_FILE"));
    if (envFilePath != null) {
      envFileValues.putAll(loadEnvFile(envFilePath));
    }

    String dbType = getConfigValue("db.type", cliOptions, envFileValues, DB_TYPE_DEFAULT);
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
    systemProperties.put("perc.db.type", dbType.toLowerCase(Locale.ROOT));
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

    String dbTypeNormalized = dbType.toLowerCase(Locale.ROOT);
    String host = systemProperties.get("perc.db.host");
    String port = systemProperties.get("perc.db.port");
    String name = systemProperties.get("perc.db.name");
    String user = systemProperties.get("perc.db.user");
    String password = systemProperties.get("perc.db.password");
    String schema = systemProperties.get("perc.db.schema");

    if (!"derby".equals(dbTypeNormalized)) {
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
      systemProperties.put("perc.db.dts.schema", firstNonBlank(schema, ""));
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
    }

    return new ResolvedDbConfig(systemProperties);
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

  private record ParsedArgs(Path installPath, Map<String, String> options) {}

  private record ResolvedDbConfig(Map<String, String> systemProperties) {}
}
