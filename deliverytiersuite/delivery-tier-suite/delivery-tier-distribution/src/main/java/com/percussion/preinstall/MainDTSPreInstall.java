/*
 * Copyright 1999-2025 Percussion Software, Inc.
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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

// REFACTORED: CP-JAVA11
public class MainDTSPreInstall {
  private static final String DISTRIBUTION_DIR = "distribution";
  private static final String PERC_JAVA_HOME = "perc.java.home";
  private static final String JAVA_HOME = "java.home";
  private static final String PERCUSSION_VERSION = "perc.version";
  private static final String INSTALL_TEMPDIR = "percDTSInstallTmp_";
  private static final String PERC_ANT_JAR = "perc-ant";
  private static final String ANT_INSTALL = "installDts.xml";

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

      if (args.length < 1) {
        System.out.println("Must specify installation or upgrade folder");
        System.exit(0);
      }

      System.out.println("Installation folder =" + args[0]);
      var installPath = Paths.get(args[0]);
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

      exitCode = execJar(installAntJarPath, execPath, installPath, isProduction);

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

        var entryDest = destPath.resolve(name);

        if (entry.isDirectory()) {
          Files.createDirectory(entryDest);
          continue;
        }
        System.out.println("Creating file " + entryDest);
        Files.copy(archive.getInputStream(entry), entryDest);
      }
    }
  }

  public static int execJar(Path jar, Path execPath, Path installDir, String isProduction)
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

    var builder =
        new ProcessBuilder(
                javabin,
                "-Dinstall.prod.dts=" + isProduction,
                "-Dfile.encoding=UTF8",
                "-Dsun.jnu.encoding=UTF8",
                "-Dinstall.dir=" + dir,
                "-Drxdeploydir=" + dir,
                "-jar",
                jar.toAbsolutePath().toString(),
                "-f",
                ANT_INSTALL)
            .directory(execPath.toFile());
    var process = builder.inheritIO().start();
    process.waitFor();
    return process.exitValue();
  }
}
