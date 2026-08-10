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
import java.util.List;
import java.util.Map;
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

/**
 * Pre-install entry point invoked by the distribution assembly. Extracts the assembled
 * distribution, resolves database configuration, selects a runtime Java home, and executes the ANT
 * installer against the target directory.
 */
public class Main {

  private static final Logger log = LogManager.getLogger(Main.class);

  /** Default distribution directory name within the staged installation. */
  public static String DISTRIBUTION_DIR = "distribution";

  /** System property name carrying the explicit preinstall Java home override. */
  public static final String PERC_JAVA_HOME = "perc.java.home";

  /** Standard JVM system property name for the Java home directory. */
  public static final String JAVA_HOME = "java.home";

  /** System property name carrying the running product version. */
  public static final String PERCUSSION_VERSION = "percversion";

  /** Prefix for the temporary extraction directory created during install. */
  public static final String INSTALL_TEMPDIR = "percInstallTmp_";

  /** Artifact name of the bundled ANT helper JAR loaded by the installer. */
  public static final String PERC_ANT_JAR = "perc-ant";

  /** System property name that flags a development environment to the installer. */
  public static final String DEVELOPMENT = "DEVELOPMENT";

  /** Name of the ANT build file inside the installer directory. */
  public static final String ANT_INSTALL = "install.xml";

  /** Standard JVM system property name for the temp directory. */
  public static final String JAVA_TEMP = "java.io.tmpdir";

  /** Name of the file under the install root that stores the resolved product version. */
  public static final String VERSION_PROPERTIES = "Version.properties";

  private static final String INSTALLATION_PROPS_PATH = "/jetty/base/etc/installation.properties";
  private static final String SERVER_PROPS_PATH = "/rxconfig/Server/server.properties";
  private static final String JETTY_JDBC_PATH = "/jetty/base/lib/jdbc/";
  private static final String OLD_JDBC_LIST_PATH = "/rxconfig/Installer/oldJdbcJarsList.txt";

  /** Temporary extraction directory created during install; may be {@code null} until set. */
  public static File tmpFolder;

  /** Resolved value of the {@link #DEVELOPMENT} system property (defaults to {@code false}). */
  public static String developmentFlag = "false";

  /**
   * Resolved product version string; populated from the {@link #PERCUSSION_VERSION} system
   * property.
   */
  public static String percVersion;

  /** Current line number being processed on stdout; tracked for diagnostics. */
  public static AtomicInteger currentLineNo = new AtomicInteger(0);

  /** Current line number being processed on stderr; tracked for diagnostics. */
  public static AtomicInteger currentErrLineNo = new AtomicInteger(0);

  /**
   * Debug flag (string {@code "true"}/{@code "false"}) sourced from the {@code DEBUG} system
   * property.
   */
  public static volatile String debug = "false";

  /** Process exit code of the last ANT invocation; {@code 0} when none yet completed. */
  public static Integer processCode = 0;

  /** Aggregated error flag set when any phase of the install reported an error. */
  public static Boolean error = false;

  /** Major version discovered from the existing {@code Version.properties} (or 0 when unknown). */
  public static int majorVersion = 0;

  /** Minor version discovered from the existing {@code Version.properties} (or 0 when unknown). */
  public static int minorVersion = 0;

  /** Explicit no-op constructor to satisfy {@code -Xdoclint}. */
  public Main() {}

  /**
   * Pre-install entry point; orchestrates extraction, Java home selection, DB config resolution,
   * and the ANT installer invocation.
   *
   * @param args CLI arguments parsed by {@link DbInstallConfigResolver#parseArgs(String[])}.
   */
  public static void main(String[] args) {
    try {

      DbInstallConfigResolver.ParsedArgs parsedArgs = DbInstallConfigResolver.parseArgs(args);
      Map<String, String> options = parsedArgs.options();
      // Check for silent/non-interactive mode (--silent or --no-tty)
      boolean silent = isSilentMode(options);
      // Issue #1513 Phase 1: interactive path prompt + summary/confirm when a TTY is present.
      // Silent / no-console keeps the historical parameter-driven contract (usage if no path).
      boolean interactive =
          InteractiveInstallWizard.isInteractive(silent, System.console() != null);
      InteractiveInstallWizard.Phase1Result phase1 =
          InteractiveInstallWizard.runPhase1(
              parsedArgs, interactive, SystemConsoleInstallPrompt.INSTANCE);
      if (!phase1.proceed()) {
        if (phase1.message() != null && !phase1.message().isBlank()) {
          System.out.println(phase1.message());
        }
        System.exit(phase1.exitCode());
        return;
      }

      Path installPath = phase1.installPath();
      options = phase1.options();
      DbInstallConfigResolver.ResolvedDbConfig resolvedDbConfig = phase1.dbConfig();
      // Issue #1340 / #1513 Phase 2: Java home already selected and persisted by the wizard.
      JavaInstallSelection.SelectionOutcome javaOutcome = phase1.javaOutcome();
      if (javaOutcome != null) {
        System.out.println("Java home selection: " + javaOutcome.summary());
      }

      debug = System.getProperty("DEBUG");
      if (debug == null || debug.equalsIgnoreCase("")) {
        debug = "false";
      }

      String javaHome =
          javaOutcome != null && javaOutcome.javaHome() != null
              ? javaOutcome.javaHome().toString()
              : System.getProperty(PERC_JAVA_HOME);
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
        majorVersion = parseVersionPart(major, "majorVersion");
        minorVersion = parseVersionPart(minor, "minorVersion");
      }

      // Issue #1157: optional early cleanup of obsolete install-root directories on upgrade only
      try {
        runObsoleteInstallDirCleanup(installPath, options, silent);
      } catch (Exception cleanupEx) {
        System.out.println(
            "Obsolete directory cleanup reported an error (upgrade will continue): "
                + cleanupEx.getMessage());
        log.warn("Obsolete directory cleanup error", cleanupEx);
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

      // Note: operator-supplied embedded H2 DB passwords are intentionally NOT
      // persisted under var/config/generated/passwords. That file is reserved
      // for credentials the system auto-generates (silent-install random
      // cmdb password via PSGenerateRepositoryPassword, plus Admin / Editor /
      // Contributor demo defaults managed by PSUserService). Operator-chosen
      // secrets live only in rxrepository.properties and the encrypted
      // perc-ds.properties written by PSConfigureDatasource.

      Integer antExit =
          execJar(installAntJarPath, execPath, installPath, resolvedDbConfig, options);
      deleteOldJDBCJars(installPath);

      int exitCode = resolveInstallExitCode(antExit, error, processCode);
      if (exitCode != 0) {
        System.out.println(
            "Installation failed (exit code "
                + exitCode
                + "). See installer output and logs under the install directory.");
        System.exit(exitCode);
      }

      // Issue #2337 / parent #934 AC-5: durable post-install verification of selected backend.
      // Runs for silent and interactive paths after a successful ANT install; never prints secrets.
      emitPostInstallVerificationReport(installPath, resolvedDbConfig);

      // Persist non-secret defaults for the next install (no passwords).
      try {
        String versionToSave = percVersion != null ? percVersion : "";
        String javaHomeToSave =
            javaOutcome != null && javaOutcome.javaHome() != null
                ? javaOutcome.javaHome().toString()
                : javaHome;
        new InstallerUserSettings(InstallerUserSettings.PREFIX_CMS)
            .save(
                installPath,
                versionToSave,
                options,
                javaHomeToSave,
                resolvedDbConfig != null ? resolvedDbConfig.systemProperties() : null);
      } catch (Exception saveEx) {
        System.out.println(
            "Warning: could not save installer user settings: " + saveEx.getMessage());
      }

    } catch (Exception e) {
      System.out.println(
          "An error occurred while executing the installation, installation has likely failed. "
              + e.getMessage());
      System.exit(1);
      return;
    }
    System.out.println("Done extracting");
  }

  /**
   * Emits the post-install DB verification report to the console and installer log (issue #2337).
   * Failures are non-fatal so a reporting glitch cannot reverse a successful install.
   *
   * @param installPath install root used for embedded path display
   * @param resolvedDbConfig resolved backend config from Phase 1
   */
  static void emitPostInstallVerificationReport(
      Path installPath, DbInstallConfigResolver.ResolvedDbConfig resolvedDbConfig) {
    try {
      String report = PostInstallVerificationReport.build(installPath, resolvedDbConfig);
      System.out.println(report);
      log.info(report);
    } catch (Exception reportEx) {
      System.out.println(
          "Warning: could not emit post-install DB verification report: " + reportEx.getMessage());
      log.warn("Post-install DB verification report failed", reportEx);
    }
  }

  /**
   * Early upgrade cleanup of obsolete install-root directories (issue #1157). Never fails the
   * overall install solely due to cleanup errors.
   */
  /** Parse a version property; blank/null/invalid → 0 (never throws). */
  static int parseVersionPart(String raw, String label) {
    if (raw == null || raw.isBlank()) {
      return 0;
    }
    try {
      return Integer.parseInt(raw.trim());
    } catch (NumberFormatException ne) {
      log.warn("Invalid {} in Version.properties: {}", label, raw);
      return 0;
    }
  }

  /**
   * Treat the supplied -Dperc.java.home value as the unattended input to the Java home selection.
   * Returns {@code null} when the property is unset, so the discovery / interactive path takes
   * over.
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

  static void runObsoleteInstallDirCleanup(
      Path installPath, java.util.Map<String, String> cliOptions, boolean silent) throws Exception {
    if (!ObsoleteInstallDirCleaner.isUpgradeInstallRoot(installPath)) {
      return;
    }
    boolean cleanFlag = ObsoleteInstallDirCleaner.parseCleanInstallDirFlag(cliOptions);
    boolean interactive = !silent && System.console() != null;
    ObsoleteInstallDirCleaner.CleanupResult result =
        ObsoleteInstallDirCleaner.run(
            installPath,
            majorVersion,
            minorVersion,
            cleanFlag,
            interactive,
            prompt -> {
              if (silent) {
                return "";
              }
              System.out.print(prompt);
              System.out.flush();
              java.io.Console console = System.console();
              if (console == null) {
                return "";
              }
              String line = console.readLine();
              return line == null ? "" : line;
            });
    System.out.print(ObsoleteInstallDirCleaner.formatCleanupReport(result));
    if (!result.proceeded()
        && "default-retain".equals(result.decisionSource())
        && !result.candidates().isEmpty()) {
      System.out.println(
          "Tip: re-run upgrade with --clean-install-dir to remove obsolete directories"
              + " without a prompt.");
    }
  }

  /**
   * Map Ant process outcome to the preinstall process exit code.
   *
   * @param antExit return value from {@link #execJar}, may be null
   * @param errorFlag shared error flag set by {@link #execJar}
   * @param sharedProcessCode shared process code field updated by {@link #execJar}
   * @return 0 on success; non-zero on failure
   */
  static int resolveInstallExitCode(Integer antExit, Boolean errorFlag, Integer sharedProcessCode) {
    if (antExit != null && antExit != 0) {
      return antExit;
    }
    if (Boolean.TRUE.equals(errorFlag)) {
      if (sharedProcessCode != null && sharedProcessCode != 0) {
        return sharedProcessCode;
      }
      return 1;
    }
    if (sharedProcessCode != null && sharedProcessCode != 0) {
      return sharedProcessCode;
    }
    return 0;
  }

  /**
   * Check if silent/non-interactive mode is enabled via --silent or --no-tty CLI options.
   *
   * @param options parsed CLI options
   * @return true if silent mode is enabled
   */
  static boolean isSilentMode(Map<String, String> options) {
    return InteractiveInstallWizard.isSilentMode(options);
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

  /**
   * Extracts a ZIP archive into {@code destPath}, filtering entries whose names start with {@code
   * folderPrefix} (the prefix is stripped before write).
   *
   * @param archiveFile archive to extract; must exist.
   * @param destPath destination directory; created if missing.
   * @param folderPrefix path prefix required of each entry; entries without this prefix are
   *     skipped.
   * @throws IOException if any entry cannot be read or written.
   */
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
            File sh = entryDest.toFile();
            if (!sh.setExecutable(true, false)) {
              log.warn("Could not set executable bit on extracted script: {}", entryDest);
            }
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

  /**
   * Resolve the ANT-visible {@code install.demo.sites} value from Phase-1 installer options (wizard
   * Yes/No or CLI {@code --demo-sites} / {@code --no-demo-sites}), with fallback to the JVM system
   * property of the same name via {@link DbInstallConfigResolver#parseDemoSitesFlag}.
   *
   * <p>Phase-1 stores the operator choice under {@link DbInstallConfigResolver#DEMO_SITES_KEY}
   * ({@code "demo-sites"}). Prior to #2192, {@link #execJar} only consulted {@link
   * System#getProperties()}, so interactive Yes / bare {@code --demo-sites} never reached ANT and
   * sample sites were not seeded (empty {@code RXSITES}).
   *
   * <p>Honored on both new installs and upgrades; locale tables are protected by the ANT {@code
   * stripSampleLocales} step, not by install-type gating.
   *
   * @param phase1Options options map from {@link InteractiveInstallWizard#runPhase1}; may be null
   * @return {@code true} when the ANT child should seed sample sites
   */
  static boolean resolveDemoSitesForAnt(Map<String, String> phase1Options) {
    return DbInstallConfigResolver.parseDemoSitesFlag(phase1Options);
  }

  /**
   * Build the {@code -Dinstall.demo.sites=…} token that must appear on the ANT child JVM command
   * line. Package-visible for unit tests that assert options propagation without spawning a
   * process.
   *
   * @param phase1Options options map from Phase 1; may be null
   * @return a single command-line argument of the form {@code -Dinstall.demo.sites=true|false}
   */
  static String demoSitesAntSystemPropertyArg(Map<String, String> phase1Options) {
    return "-D"
        + DbInstallConfigResolver.DEMO_SITES_SYSTEM_PROPERTY
        + "="
        + resolveDemoSitesForAnt(phase1Options);
  }

  /**
   * Executes the bundled installer JAR with the supplied resolved DB configuration.
   *
   * @param jar path to the bundled installer JAR.
   * @param execPath working directory for the spawned process.
   * @param installDir target install directory passed to the installer.
   * @param resolvedDbConfig DB configuration to surface via system properties on the child JVM.
   * @param phase1Options Phase-1 options (wizard / CLI), including {@code demo-sites}; may be null.
   * @return process exit code returned by the spawned JVM.
   * @throws IOException if the child process cannot be started.
   * @throws InterruptedException if the wait is interrupted.
   */
  public static Integer execJar(
      Path jar,
      Path execPath,
      Path installDir,
      DbInstallConfigResolver.ResolvedDbConfig resolvedDbConfig,
      Map<String, String> phase1Options)
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
      // Propagate demo-sites from Phase-1 options (wizard / --demo-sites) so ANT can chain
      // installSampleSites after core schema load. Also honors -Dinstall.demo.sites on this JVM
      // when the options map does not set the flag. Runs on new installs and upgrades; RXLOCALE
      // protection is stripSampleLocales in installRepository.xml, not install-type gating.
      command.add(demoSitesAntSystemPropertyArg(phase1Options));
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

  /**
   * Updates the user Spring configuration files under the install root.
   *
   * @param installDir install root directory whose {@code
   *     jetty/base/webapps/Rhythmyx/WEB-INF/config/user/spring/} tree will be refreshed.
   */
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

  /**
   * Updates the category XML under the install root during upgrade flows.
   *
   * @param installDir install root directory whose {@code rx_resources/category/category.xml} is
   *     updated.
   */
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
              if (s.contains("</CategoryTree")) {
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

  /**
   * Replaces all occurrences of {@code replaceToken} with {@code replaceValue} in {@code file}.
   *
   * @param file target file whose contents are rewritten in place.
   * @param replaceToken literal token to replace.
   * @param replaceValue replacement value.
   */
  public static void replaceTokens(File file, String replaceToken, String replaceValue) {
    Replace r = new Replace();
    r.setFile(file);
    r.setToken(replaceToken);
    r.createReplaceToken();
    r.setValue(replaceValue);
    r.createReplaceValue();
    r.execute();
  }

  /**
   * Updates Jetty's server port and SSL settings to the pre-upgrade values from the legacy {@code
   * JBossServerXML_BAK/server.xml} snapshot when present.
   *
   * @param installDir install root directory whose Jetty configuration will be updated.
   * @throws ParserConfigurationException if the XML parser cannot be configured.
   * @throws IOException if the legacy settings file cannot be read.
   * @throws SAXException if the legacy XML cannot be parsed.
   */
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

  /**
   * Writes the Jetty installation properties file under the install root, replacing the supplied
   * token with {@code value}.
   *
   * @param installDir install root directory.
   * @param replaceToken literal token to replace in the properties file.
   * @param value replacement value.
   * @throws IOException if the properties file cannot be read or written.
   */
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

  /**
   * Updates SSL-related entries in the Jetty {@code server.properties} under the install root.
   *
   * @param installDir install root directory whose Jetty server properties will be updated.
   * @throws IOException if the properties file cannot be read or written.
   */
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
