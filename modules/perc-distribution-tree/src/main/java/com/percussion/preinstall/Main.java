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

import com.percussion.security.error.PSExceptionUtils;
import com.percussion.security.validation.PathValidation;
import com.percussion.security.xml.PSSecureXMLUtils;
import com.percussion.security.xml.PSXmlSecurityOptions;
import com.percussion.utils.io.PathUtils;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tools.ant.taskdefs.Replace;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class Main {

  private static final Logger log = LogManager.getLogger(Main.class);

  public static String DISTRIBUTION_DIR = "distribution";
  public static final String PERC_JAVA_HOME = "perc.java.home";
  public static final String JAVA_HOME = "java.home";
  public static final String PERCUSSION_VERSION = "percversion";
  public static final String INSTALL_TEMPDIR = "percInstallTmp_";
  public static final String PERC_ANT_JAR = "perc-ant";
  public static final String DEVELOPMENT = "DEVELOPMENT";
  public static final String ANT_INSTALL = "install.xml";
  public static final String JAVA_TEMP = "java.io.tmpdir";
  public static final String VERSION_PROPERTIES = "Version.properties";
  private static final String INSTALLATION_PROPS_PATH = "/jetty/base/etc/installation.properties";
  private static final String SERVER_PROPS_PATH = "/rxconfig/Server/server.properties";
  private static final String JETTY_JDBC_PATH = "/jetty/base/lib/jdbc/";
  private static final String OLD_JDBC_LIST_PATH = "/rxconfig/Installer/oldJdbcJarsList.txt";
  private static final String DB_TYPE_DEFAULT = "derby";
  private static final String DB_SSL_ENABLED_DEFAULT = "true";
  private static final String DB_SSL_VERIFY_DEFAULT = "true";
  private static final String DB_SSL_ALLOW_SELF_SIGNED_DEFAULT = "false";
  public static File tmpFolder;
  public static String developmentFlag = "false";
  public static String percVersion;
  public static AtomicInteger currentLineNo = new AtomicInteger(0);
  public static AtomicInteger currentErrLineNo = new AtomicInteger(0);
  public static volatile String debug = "false";
  public static Integer processCode = 0;
  public static Boolean error = false;
  public static int majorVersion = 0;
  public static int minorVersion = 0;

  public static void main(String[] args) {
    try {

      ParsedArgs parsedArgs = parseArgs(args);
      if (parsedArgs.installPath() == null) {
        System.out.println("Must specify installation or upgrade folder");
        System.exit(0);
      }

      Path installPath = parsedArgs.installPath();

      ResolvedDbConfig resolvedDbConfig = resolveDbConfig(parsedArgs.options());

      debug = System.getProperty("DEBUG");
      if (debug == null || debug.equalsIgnoreCase("")) {
        debug = "false";
      }

      String javaHome = System.getProperty(PERC_JAVA_HOME);
      if (javaHome == null || javaHome.trim().equalsIgnoreCase(""))
        javaHome = System.getProperty(JAVA_HOME);

      String javabin = "";

      if (System.getProperty("file.separator").equals("/")) {
        javabin = javaHome + "/bin/java";
      } else {
        javabin = javaHome + "/bin/java.exe";
      }

      percVersion = System.getProperty(PERCUSSION_VERSION);
      if (percVersion == null) percVersion = "";

      developmentFlag = System.getProperty(DEVELOPMENT);
      if (developmentFlag == null || DEVELOPMENT.trim().equalsIgnoreCase(""))
        developmentFlag = "false";

      System.out.println("perc.java.home=" + javaHome);
      System.out.println("java.bin=" + javabin);
      System.out.println("percversion=" + percVersion);
      System.out.println(DEVELOPMENT + "=" + developmentFlag);

      System.out.println("Installation folder is " + installPath.toAbsolutePath().toString());

      Properties existingVersion = loadVersionProperties(installPath);
      if (existingVersion != null) {
        String major = existingVersion.getProperty("majorVersion");
        String minor = existingVersion.getProperty("minorVersion");
        log.info("Major Version Found: {}", major);
        log.info("Minor Version Found: {}", minor);
        try {
          majorVersion = Integer.parseInt(major);
          minorVersion = Integer.parseInt(minor);
        } catch (NumberFormatException ne) {
          log.warn("Invalid Version number in Version File");
        }
      }

      Path installSrc;
      Path currentJar =
          Paths.get(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
      if (!Files.isDirectory(currentJar)) {
        installSrc = Files.createTempDirectory(INSTALL_TEMPDIR);

        // add option to not delete for debugging
        Runtime.getRuntime()
            .addShutdownHook(
                new Thread() {
                  @Override
                  public void run() {
                    // If the debug flag is set don't delete the files.
                    if (debug.equalsIgnoreCase("false")) {
                      try {
                        Files.walk(installSrc)
                            .sorted(Comparator.reverseOrder())
                            .map(Path::toFile)
                            .forEach(File::delete);
                      } catch (IOException ex) {
                        System.out.println(
                            "An error occurred while executing the installation, installation has"
                                + " likely failed. "
                                + ex.getMessage());
                      }
                    }
                  }
                });

        extractArchive(currentJar, installSrc, DISTRIBUTION_DIR);

      } else {
        System.out.println("Running from extracted jar");
        installSrc = currentJar.resolve(DISTRIBUTION_DIR);
      }

      Path execPath = installSrc.resolve(Paths.get("rxconfig", "Installer"));
      Path installAntJarPath =
          execPath.resolve(PathUtils.getVersionLessJarFilePath(execPath, PERC_ANT_JAR + "-*.jar"));
      execJar(installAntJarPath, execPath, installPath, resolvedDbConfig);
      deleteOldJDBCJars(installPath);

    } catch (Exception e) {
      System.out.println(
          "An error occurred while executing the installation, installation has likely failed. "
              + e.getMessage());
    }
    System.out.println("Done extracting");
  }

  private static void deleteOldJDBCJars(Path installPath) {
    String oldJarsFileName = installPath + OLD_JDBC_LIST_PATH;
    log.info("Old JDBC File List File..... " + oldJarsFileName);
    File oldJarNamesFile = new File(oldJarsFileName);
    List<String> listOfStrings = new ArrayList<String>();
    if (oldJarNamesFile.exists()) {
      log.info("Old JDBC File List File Found..... ");
      BufferedReader bf = null;
      try {
        bf = new BufferedReader(new FileReader(oldJarsFileName));
        String line = bf.readLine();
        while (line != null) {
          listOfStrings.add(line);
          line = bf.readLine();
        }
        bf.close();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    log.info("Old JDBC Files Found..... " + listOfStrings.toString());

    for (int i = 0; i < listOfStrings.size(); i++) {
      String fileName = installPath + JETTY_JDBC_PATH + listOfStrings.get(i);
      log.info("Deleting Old JDBC File..... " + fileName);
      File oldFile = new File(fileName);
      if (oldFile.exists()) {
        log.info("JDBC File Exists : " + fileName);
        oldFile.delete();
        log.info("Delete Old JDBC File Succeded");
      }
    }
  }

  public static void extractArchive(Path archiveFile, Path destPath, String folderPrefix)
      throws IOException {

    Files.createDirectories(destPath); // create dest path folder(s)

    try (ZipFile archive = new ZipFile(archiveFile.toFile())) {

      // sort entries by name to always create folders first
      List<? extends ZipEntry> entries =
          archive.stream()
              .sorted(Comparator.comparing(ZipEntry::getName))
              .collect(Collectors.toList());

      // copy each entry in the dest path
      for (ZipEntry entry : entries) {
        currentLineNo.getAndIncrement();
        String entryName = entry.getName();
        if (!entryName.startsWith(folderPrefix)) continue;

        String name = entryName.substring(folderPrefix.length() + 1);
        if (name.length() == 0) continue;

        try {
          // CWE-22: Validate zip entry path to prevent ZipSlip attacks
          File safeFile = PathValidation.constructSafePath(destPath.toFile(), name);
          Path entryDest = safeFile.toPath();
          File newFile = new File(entryDest.toString());
          System.out.println("Unzipping to " + newFile.getAbsolutePath());
          // create directories for sub directories in zip
          new File(newFile.getParent()).mkdirs();

          if (entry.isDirectory()) {
            Files.createDirectory(entryDest);
            continue;
          }
          System.out.println("Creating file " + entryDest);
          Files.copy(archive.getInputStream(entry), entryDest);
          // Preserve executable permissions for shell scripts (v8.1.7 #515 / GH-510)
          if (entryName.endsWith(".sh")) {
            entryDest.toFile().setExecutable(true, false);
          }
        } catch (SecurityException se) {
          // ZipSlip or path traversal attack detected
          log.warn("Rejected malicious zip entry: {} - {}", entryName, se.getMessage());
        }
      }
    } catch (Exception ex) {
      log.error(ex.getMessage());
      log.debug(ex.getMessage(), ex);
      error = true;
    }
  }

  public static Integer execJar(
      Path jar, Path execPath, Path installDir, ResolvedDbConfig resolvedDbConfig)
      throws IOException, InterruptedException {

    try {

      String javaHome = System.getProperty(PERC_JAVA_HOME);
      if (javaHome == null || javaHome.trim().equalsIgnoreCase(""))
        javaHome = System.getProperty(JAVA_HOME);

      String javabin = "";

      if (System.getProperty("file.separator").equals("/")) {
        javabin = javaHome + "/bin/java";
      } else {
        javabin = javaHome + "\\bin\\java.exe";
      }
      String debugMode = System.getProperty("perc.debug", "false");
      String debugFlag = "";
      if (Boolean.parseBoolean(debugMode)) {
        debugFlag = "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=8054";
      }

      List<String> command = new ArrayList<>();
      command.add(javabin);
      if (debugFlag.trim().length() > 0) {
        command.add(debugFlag);
      }
      command.add("-Dfile.encoding=UTF-8");
      command.add("-Dsun.jnu.encoding=UTF-8");
      command.add("-Dinstall.dir=" + installDir.toAbsolutePath());
      for (Map.Entry<String, String> entry : resolvedDbConfig.systemProperties().entrySet()) {
        command.add("-D" + entry.getKey() + "=" + entry.getValue());
      }
      command.add("-jar");
      command.add(jar.toAbsolutePath().toString());
      command.add("-f");
      command.add(ANT_INSTALL);

      ProcessBuilder builder = new ProcessBuilder(command).directory(execPath.toFile());
      // pass in known flags
      builder.environment().put(DEVELOPMENT, developmentFlag);
      builder.environment().put(PERCUSSION_VERSION, percVersion);
      // Pass on the temp dir if set
      builder.environment().put(JAVA_TEMP, System.getProperty("java.io.tmpdir"));
      Process process = builder.inheritIO().start();

      try (InputStream inStream = process.getInputStream()) {
        try (InputStream inErrStream = process.getErrorStream()) {

          InputStreamLineBuffer outBuff = new InputStreamLineBuffer(inStream);
          InputStreamLineBuffer errBuff = new InputStreamLineBuffer(inErrStream);
          Thread streamReader =
              new Thread(
                  new Runnable() {
                    public void run() {
                      // start the input reader buffer threads
                      outBuff.start();
                      errBuff.start();

                      // while an input reader buffer thread is alive
                      // or there are unconsumed data left
                      while (outBuff.isAlive()
                          || outBuff.hasNext()
                          || errBuff.isAlive()
                          || errBuff.hasNext()) {

                        // get the normal output if at least 50 millis have passed
                        if (outBuff.timeElapsed() > 50)
                          while (outBuff.hasNext()) {
                            currentLineNo.getAndIncrement();
                            System.out.println(outBuff.getNext());
                          }
                        // get the error output if at least 50 millis have passed
                        if (errBuff.timeElapsed() > 50)
                          while (errBuff.hasNext()) currentErrLineNo.getAndIncrement();

                        System.err.println(errBuff.getNext());
                        // sleep a bit bofore next run
                        try {
                          Thread.sleep(100);
                        } catch (InterruptedException e) {
                          Thread.currentThread().interrupt();
                        }
                      }
                      System.out.println("Finish reading error and output stream");
                    }
                  });

          streamReader.start();

          process.waitFor();

          // Shutdown threads and streams
          streamReader.interrupt();
          process.getInputStream().close();
          process.getErrorStream().close();

          streamReader.join();
          updateUserSpringConfig(installDir);
          updateCategoryXMLForUpgrade(installDir);
          // Loading JettyServerPort & SSL settings from server.xml for 5.3 and prior release
          // After that the properties are in Installation.properties, thus no need to load from
          // that file.
          if (majorVersion == 5 && minorVersion < 4) {
            log.info("Updating JettyServerPortAndSSLToPreUpgradeSettings from 5.3");
            updateJettyServerPortAndSSLToPreUpgradeSettings(installDir);
          }
          updateSSLProtocol(installDir);
          processCode = process.exitValue();
          if (processCode != 0) {
            error = true;
          }
        }
      }
    } catch (Exception ex) {
      log.error(ex.getMessage());
      log.debug(ex.getMessage(), ex);
      processCode = -2;
      error = true;
    }
    return processCode;
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
    Map<String, String> environmentFileValues = new HashMap<>();
    String envFilePath =
        firstNonBlank(
            cliOptions.get("db.config.env.file"),
            System.getenv("DB_CONFIG_ENV_FILE"),
            System.getenv("PERC_DB_CONFIG_ENV_FILE"));

    if (envFilePath != null) {
      environmentFileValues.putAll(loadEnvFile(envFilePath));
    }

    String dbType = getConfigValue("db.type", cliOptions, environmentFileValues, null);
    if (isBlank(dbType)) {
      dbType = DB_TYPE_DEFAULT;
    }
    dbType = dbType.toLowerCase(Locale.ROOT);

    String sslEnabled =
        normalizeBoolean(
            getConfigValue(
                "db.ssl.enabled", cliOptions, environmentFileValues, DB_SSL_ENABLED_DEFAULT),
            "db.ssl.enabled");
    String sslVerify =
        normalizeBoolean(
            getConfigValue(
                "db.ssl.verify", cliOptions, environmentFileValues, DB_SSL_VERIFY_DEFAULT),
            "db.ssl.verify");
    String sslAllowSelfSigned =
        normalizeBoolean(
            getConfigValue(
                "db.ssl.allowSelfSigned",
                cliOptions,
                environmentFileValues,
                DB_SSL_ALLOW_SELF_SIGNED_DEFAULT),
            "db.ssl.allowSelfSigned");

    String host = getConfigValue("db.host", cliOptions, environmentFileValues, null);
    String port = getConfigValue("db.port", cliOptions, environmentFileValues, null);
    String name = getConfigValue("db.name", cliOptions, environmentFileValues, null);
    String schema = getConfigValue("db.schema", cliOptions, environmentFileValues, null);
    String user = getConfigValue("db.user", cliOptions, environmentFileValues, null);
    String password = getConfigValue("db.password", cliOptions, environmentFileValues, null);

    if (!Objects.equals(dbType, "derby")) {
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
                + dbType
                + ": "
                + String.join(", ", missing));
      }
    }

    Map<String, String> systemProperties = new HashMap<>();
    systemProperties.put("perc.db.type", dbType);
    systemProperties.put("perc.db.ssl.enabled", sslEnabled);
    systemProperties.put("perc.db.ssl.verify", sslVerify);
    systemProperties.put("perc.db.ssl.allowSelfSigned", sslAllowSelfSigned);
    setIfPresent(systemProperties, "perc.db.host", host);
    setIfPresent(systemProperties, "perc.db.port", port);
    setIfPresent(systemProperties, "perc.db.name", name);
    setIfPresent(systemProperties, "perc.db.schema", schema);
    setIfPresent(systemProperties, "perc.db.user", user);
    setIfPresent(systemProperties, "perc.db.password", password);
    setIfPresent(
        systemProperties,
        "perc.db.ssl.trustStorePath",
        getConfigValue("db.ssl.trustStorePath", cliOptions, environmentFileValues, null));
    setIfPresent(
        systemProperties,
        "perc.db.ssl.trustStorePassword",
        getConfigValue("db.ssl.trustStorePassword", cliOptions, environmentFileValues, null));
    setIfPresent(
        systemProperties,
        "perc.db.ssl.keyStorePath",
        getConfigValue("db.ssl.keyStorePath", cliOptions, environmentFileValues, null));
    setIfPresent(
        systemProperties,
        "perc.db.ssl.keyStorePassword",
        getConfigValue("db.ssl.keyStorePassword", cliOptions, environmentFileValues, null));

    if (Objects.equals(dbType, "mysql")) {
      String resolvedSchema = firstNonBlank(schema, "");
      String cmsServer =
          "//"
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
      systemProperties.put("perc.db.cms.backend", "MYSQL");
      systemProperties.put("perc.db.cms.driverName", "mysql");
      systemProperties.put("perc.db.cms.driverClass", "com.mysql.cj.jdbc.Driver");
      systemProperties.put("perc.db.cms.server", cmsServer);
      systemProperties.put("perc.db.cms.name", name);
      systemProperties.put("perc.db.cms.schema", resolvedSchema);
      systemProperties.put(
          "perc.db.dts.jdbcUrl",
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
              + sslVerify);
      systemProperties.put("perc.db.dts.jdbcDriver", "com.mysql.cj.jdbc.Driver");
      systemProperties.put(
          "perc.db.dts.hibernateDialect", "org.hibernate.dialect.MySQL5InnoDBDialect");
      systemProperties.put("perc.db.dts.schema", resolvedSchema);
    } else if (Objects.equals(dbType, "sqlserver")) {
      String resolvedSchema = firstNonBlank(schema, "DBO");
      String trustServerCertificate =
          Objects.equals(sslAllowSelfSigned, "true") || Objects.equals(sslVerify, "false")
              ? "true"
              : "false";
      String cmsServer =
          "//"
              + host
              + ":"
              + port
              + ";databaseName="
              + name
              + ";encrypt="
              + sslEnabled
              + ";trustServerCertificate="
              + trustServerCertificate;
      systemProperties.put("perc.db.cms.backend", "MSSQL");
      systemProperties.put("perc.db.cms.driverName", "sqlserver");
      systemProperties.put(
          "perc.db.cms.driverClass", "com.microsoft.sqlserver.jdbc.SQLServerDriver");
      systemProperties.put("perc.db.cms.server", cmsServer);
      systemProperties.put("perc.db.cms.name", name);
      systemProperties.put("perc.db.cms.schema", resolvedSchema);
      systemProperties.put(
          "perc.db.dts.jdbcUrl",
          "jdbc:sqlserver://"
              + host
              + ":"
              + port
              + ";databaseName="
              + name
              + ";encrypt="
              + sslEnabled
              + ";trustServerCertificate="
              + trustServerCertificate);
      systemProperties.put(
          "perc.db.dts.jdbcDriver", "com.microsoft.sqlserver.jdbc.SQLServerDriver");
      systemProperties.put(
          "perc.db.dts.hibernateDialect",
          "com.percussion.delivery.rdbms.PSUnicodeSQLServerDialect");
      systemProperties.put("perc.db.dts.schema", resolvedSchema);
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
      List<String> lines = Files.readAllLines(path);
      for (String line : lines) {
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
        String key = trimmed.substring(0, separator).trim();
        String value = trimmed.substring(separator + 1).trim();
        values.put(key, value);
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

  private static String firstNonBlank(String... values) {
    for (String value : values) {
      if (!isBlank(value)) {
        return value.trim();
      }
    }
    return null;
  }

  private static boolean isBlank(String value) {
    return value == null || value.trim().isEmpty();
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

  private static void setIfPresent(Map<String, String> target, String key, String value) {
    if (!isBlank(value)) {
      target.put(key, value.trim());
    }
  }

  private record ParsedArgs(Path installPath, Map<String, String> options) {}

  private record ResolvedDbConfig(Map<String, String> systemProperties) {}

  private static Properties loadVersionProperties(Path installDir) {
    File versionFile = new File(installDir + File.separator + VERSION_PROPERTIES);
    Properties rawVersionProperties = new Properties();
    if (versionFile.exists()) {
      try (FileInputStream versionfileStream = new FileInputStream(versionFile)) {
        rawVersionProperties.load(versionfileStream);
        return rawVersionProperties;
      } catch (IOException e) {
        log.info("Loading Version.properties file failed", PSExceptionUtils.getMessageForLog(e));
      }
    }
    return rawVersionProperties;
  }

  private static void updateSSLProtocol(Path installDir) {
    String installationPropertiesFilePath =
        installDir.toAbsolutePath().toString() + INSTALLATION_PROPS_PATH;
    File installationPropertiesFile = new File(installationPropertiesFilePath);
    Properties installationProperties = new Properties();
    String newProtocol = null;
    if (installationPropertiesFile.exists()) {
      try (FileInputStream installationPropsfileStream =
          new FileInputStream(installationPropertiesFile)) {
        installationProperties.load(installationPropsfileStream);
        String protocols = installationProperties.getProperty("perc.ssl.protocols");
        if (protocols != null) {
          String[] protocolArray = protocols.split(",");
          for (String pr : protocolArray) {
            if (!"".equals(pr) && !"TLSv1".equals(pr) && !"TLSv1.1".equals(pr)) {
              if (newProtocol == null) {
                newProtocol = pr;
              } else {
                newProtocol += "," + pr;
              }
            }
          }
          installationProperties.setProperty("perc.ssl.protocols", protocols);
          try (FileOutputStream os = new FileOutputStream(installationPropertiesFile)) {
            installationProperties.store(os, "update ssl Protocol");
          }
        }
      } catch (IOException e) {
        log.info(
            "Loading Installation.properties file failed", PSExceptionUtils.getMessageForLog(e));
      }
    }
  }

  public static void updateUserSpringConfig(Path installDir) {
    String userSprinXMLDir =
        installDir.toAbsolutePath().toString()
            + "/jetty/base/webapps/Rhythmyx/WEB-INF/config/user/spring/";
    String[] ext = new String[] {"xml"};
    File userSprinXMLDirectory = new File(userSprinXMLDir);
    if (userSprinXMLDirectory.exists()) {
      List<File> userSpringXMLFiles =
          (List<File>) FileUtils.listFiles(userSprinXMLDirectory, ext, false);
      for (File xmlFile : userSpringXMLFiles) {
        if (xmlFile.exists()) {

          replaceTokens(
              xmlFile,
              "<!DOCTYPE beans PUBLIC \"-//SPRING//DTD BEAN//EN\" \n"
                  + "   \"http://www.springframework.org/dtd/spring-beans.dtd\">\n"
                  + "<beans>",
              "<beans xmlns=\"http://www.springframework.org/schema/beans\"\n"
                  + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                  + "xmlns:tx=\"http://www.springframework.org/schema/tx\"\n"
                  + "xmlns:context=\"http://www.springframework.org/schema/context\"\n"
                  + "xsi:schemaLocation=\"http://www.springframework.org/schema/beans\n"
                  + "http://www.springframework.org/schema/beans/spring-beans-4.2.xsd\n"
                  + "http://www.springframework.org/schema/tx\n"
                  + "http://www.springframework.org/schema/tx/spring-tx-4.2.xsd\n"
                  + "http://www.springframework.org/schema/context\n"
                  + "http://www.springframework.org/schema/context/spring-context-4.2.xsd\">");
        }
      }
    }
  }

  public static void updateCategoryXMLForUpgrade(Path installDir) {
    String categoryXMLDir =
        installDir.toAbsolutePath().toString() + "/rx_resources/category/category.xml";
    File categoryXML = new File(categoryXMLDir);
    AtomicReference<String> topLevelNodeToReplace = new AtomicReference<>("");
    AtomicReference<String> topLevelNodeEndToReplace = new AtomicReference<>("");
    AtomicReference<Boolean> topLevelNodeStringPresent = new AtomicReference<>(false);
    if (categoryXML.exists()) {

      try (Stream<String> stream = Files.lines(Paths.get(categoryXMLDir))) {
        stream.forEach(
            s -> {
              if (s.contains("<CategoryTree")) {
                topLevelNodeToReplace.set(s);
              }
              if (s.contains("</CategoryTree"))
                ;
              {
                topLevelNodeEndToReplace.set(s);
              }
              if (s.contains("topLevelNodes")) {
                topLevelNodeStringPresent.set(true);
              }
            });
      } catch (Exception e) {
        log.error(PSExceptionUtils.getMessageForLog(e));
        log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      }

      if (!topLevelNodeStringPresent.get()) {
        replaceTokens(
            categoryXML,
            topLevelNodeToReplace.get(),
            topLevelNodeToReplace + "\n" + "<topLevelNodes>");
        replaceTokens(
            categoryXML,
            topLevelNodeEndToReplace.get(),
            "</topLevelNodes>\n" + topLevelNodeEndToReplace);
      }
    }
  }

  public static void replaceTokens(File file, String replaceToken, String replaceValue) {
    Replace r = new Replace();
    r.setFile(file);
    r.setToken(replaceToken);
    r.createReplaceToken();
    r.setValue(replaceValue);
    r.createReplaceValue();
    r.execute();
  }

  public static void updateJettyServerPortAndSSLToPreUpgradeSettings(Path installDir)
      throws ParserConfigurationException, IOException, SAXException {
    String oldServerXMLDir = installDir.toAbsolutePath().toString() + "/JBossServerXML_BAK/";
    File oldServerXMLFile = new File(oldServerXMLDir + "server.xml");
    log.info("In updateJettyServerPortAndSSLToPreUpgradeSettings");
    if (oldServerXMLFile.exists()) {
      DocumentBuilderFactory dbf =
          PSSecureXMLUtils.getSecuredDocumentBuilderFactory(
              new PSXmlSecurityOptions(true, true, true, false, true, false));
      dbf.setValidating(false);
      DocumentBuilder db = dbf.newDocumentBuilder();
      try (FileInputStream fis = new FileInputStream(oldServerXMLFile)) {
        log.info("Updating connectors");
        Document doc = db.parse(fis);
        NodeList nodeList = doc.getElementsByTagName("Connector");
        for (int i = 0; i < nodeList.getLength(); i++) {
          Element e = (Element) nodeList.item(i);
          boolean hasAttribute = e.hasAttribute("scheme");
          if (hasAttribute && e.getAttribute("scheme").equalsIgnoreCase("http")) {
            writeInstallationPropertiesForJetty(
                installDir, "jetty.http.port=", e.getAttribute("port"));
          }
          if (hasAttribute && e.getAttribute("scheme").equalsIgnoreCase("https")) {
            setSSLConnectorProperties(installDir, e);
            updateServerPropsForJettySSL(installDir);
          }
        }
      }
    }
  }

  private static void setSSLConnectorProperties(Path installDir, Element e) throws IOException {
    writeInstallationPropertiesForJetty(installDir, "jetty.ssl.port=", e.getAttribute("port"));
    String keyStorefileAttr = e.getAttribute("keystoreFile");
    String keyStorefileName = "";
    String keystorePassWord = e.getAttribute("keystorePass");
    String keyStoreFilePath = e.getAttribute("jetty.sslContext.keyStorePath");
    String sslProtocols = e.getAttribute("protocols");
    if (keyStoreFilePath == null || keyStoreFilePath.trim().equals("")) {
      String[] splitArr;
      if (keyStorefileAttr != "") {
        splitArr = keyStorefileAttr.split("/");
        keyStorefileName = splitArr[splitArr.length - 1];
        if (System.getProperty("file.separator").equals("/")) {
          keyStoreFilePath = "etc/" + keyStorefileName;
        } else {
          String wPath = installDir.toAbsolutePath().toString().replace("\\", "\\\\");
          keyStoreFilePath = "etc\\\\" + keyStorefileName;
        }
      }
    }
    writeInstallationPropertiesForJetty(
        installDir, "jetty.sslContext.keyStorePath=", keyStoreFilePath);
    writeInstallationPropertiesForJetty(
        installDir, "jetty.sslContext.trustStorePath=", keyStoreFilePath);
    writeInstallationPropertiesForJetty(
        installDir, "jetty.sslContext.keyStorePassword=", keystorePassWord);
    writeInstallationPropertiesForJetty(
        installDir, "jetty.sslContext.keyManagerPassword=", keystorePassWord);
    writeInstallationPropertiesForJetty(
        installDir, "jetty.sslContext.trustStorePassword=", keystorePassWord);
    String newProtocol = null;
    if (sslProtocols != null) {
      String[] protocolArray = sslProtocols.split(",");
      for (String pr : protocolArray) {
        if (!"".equals(pr) && !"TLSv1".equals(pr) && !"TLSv1.1".equals(pr)) {
          if (newProtocol == null) {
            newProtocol = pr;
          } else {
            newProtocol += "," + pr;
          }
        }
      }
    }
    if (newProtocol == null) {
      newProtocol = "";
    }
    writeInstallationPropertiesForJetty(installDir, "perc.ssl.protocols=", newProtocol);
  }

  public static void writeInstallationPropertiesForJetty(
      Path installDir, String replaceToken, String value) throws IOException {
    AtomicReference<String> replaceString = new AtomicReference<>("");
    AtomicReference<String> replaceValue = new AtomicReference<>("");

    String installationPropertiesFileDir =
        installDir.toAbsolutePath().toString() + INSTALLATION_PROPS_PATH;

    try (Stream<String> stream = Files.lines(Paths.get(installationPropertiesFileDir))) {
      stream.forEach(
          s -> {
            if (s.contains(replaceToken)) {
              replaceString.set(s);
              replaceValue.set(replaceToken + value);
            }
          });
    }
    File installationPropertiesFile =
        new File(installDir.toAbsolutePath().toString() + INSTALLATION_PROPS_PATH);
    replaceTokens(installationPropertiesFile, replaceString.get(), replaceValue.get());
  }

  public static void updateServerPropsForJettySSL(Path installDir) throws IOException {
    String serverPropertiesFilePath = installDir.toAbsolutePath().toString() + SERVER_PROPS_PATH;
    File serverPropertiesFile = new File(serverPropertiesFilePath);
    Properties serverProperties = new Properties();
    String newProtocol = null;
    if (serverPropertiesFile.exists()) {
      try (FileInputStream serverfileStream = new FileInputStream(serverPropertiesFile)) {
        serverProperties.load(serverfileStream);
        String requireHTTPS = serverProperties.getProperty("requireHTTPS");
        if (requireHTTPS != null) {
          serverProperties.setProperty("requireHTTPS", "true");
          try (FileOutputStream os = new FileOutputStream(serverPropertiesFile)) {
            serverProperties.store(os, "updated requiredHTTPS Flag");
          }
        }
      } catch (IOException e) {
        log.error(
            "Loading Server.properties file failed. Error: {}",
            PSExceptionUtils.getMessageForLog(e));
      }
    }
  }
}
