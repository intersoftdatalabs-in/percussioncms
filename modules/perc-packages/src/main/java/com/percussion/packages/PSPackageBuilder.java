/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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

package com.percussion.packages;

import com.percussion.packages.pagexml.PSPageXmlDualShip;
import com.percussion.packages.pagexml.PSPageXmlException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Builds .ppkg package files from source directories. Replaces the Ant-based foreach/antcall build
 * with a single-pass Java builder that supports incremental builds (skips packages whose sources
 * haven't changed).
 *
 * <p>Page layout packages that author modern {@code pages/&lt;id&gt;/component-package.json} (ADR-004
 * / #2786) dual-ship install {@code *.templateDef} into the staging copy before zip so deployer
 * {@code TemplateDef} install parity is preserved.
 *
 * <p>Usage: {@code PSPackageBuilder <packagesDir> <outputDir> <tempDir>}
 */
public final class PSPackageBuilder {

  private static final String MAPPING_EXT = ".mapping.properties";
  private static final String SYS_USER_DEPENDENCY = "sys__UserDependency--";
  private static final String EXTENSION = "Extension";
  private static final String STYLE_SHEET = "Stylesheet";
  private static final String SCHEMA_FILE = "Schema";
  private static final String APPLICATION_FILE = "Application";

  private final Path packagesDir;
  private final Path outputDir;
  private final Path tempDir;
  private int built;
  private int skipped;

  PSPackageBuilder(Path packagesDir, Path outputDir, Path tempDir) {
    this.packagesDir = packagesDir;
    this.outputDir = outputDir;
    this.tempDir = tempDir;
  }

  /**
   * Program entry point invoked by the {@code exec-maven-plugin} during the {@code prepare-package}
   * phase. Expects at least three arguments: the packages source directory, the output directory
   * for the generated {@code .ppkg} files, and a scratch directory used while reorganizing the
   * package contents. Any additional positional arguments beyond the first three are ignored.
   *
   * <p>Usage: {@code PSPackageBuilder <packagesDir> <outputDir> <tempDir>}. Exits with a non-zero
   * status if fewer than three arguments are supplied or the packages directory does not exist.
   *
   * @param args command-line arguments; must contain at least three entries ordered {@code
   *     <packagesDir> <outputDir> <tempDir>}.
   * @throws IOException if an I/O error occurs while preparing, walking or zipping package files.
   */
  public static void main(String[] args) throws IOException {
    if (args.length < 3) {
      System.err.println("Usage: PSPackageBuilder <packagesDir> <outputDir> <tempDir>");
      System.exit(1);
    }

    var packagesDir = Path.of(args[0]);
    var outputDir = Path.of(args[1]);
    var tempDir = Path.of(args[2]);

    if (!Files.isDirectory(packagesDir)) {
      System.err.println("Packages directory does not exist: " + packagesDir);
      System.exit(1);
    }

    var builder = new PSPackageBuilder(packagesDir, outputDir, tempDir);
    builder.buildAll();
  }

  void buildAll() throws IOException {
    Files.createDirectories(outputDir);
    Files.createDirectories(tempDir);

    try (var stream = Files.newDirectoryStream(packagesDir, Files::isDirectory)) {
      for (var packageDir : stream) {
        var dirName = packageDir.getFileName().toString();
        if ("Percussion".equals(dirName) || "packageholder".equals(dirName)) {
          continue;
        }
        buildPackage(packageDir, dirName);
      }
    }

    System.out.println("Package build complete: " + built + " built, " + skipped + " up-to-date");

    // Clean temp directory
    deleteDirectory(tempDir);
  }

  private void buildPackage(Path packageDir, String dirName) throws IOException {
    var ppkgName = dirName + ".ppkg";
    var outputFile = outputDir.resolve(ppkgName);

    // Incremental: skip if output exists and is newer than all source files
    if (Files.exists(outputFile) && isUpToDate(packageDir, outputFile)) {
      skipped++;
      return;
    }

    System.out.println("Building package: " + ppkgName);

    // Per-package temp dirs to avoid conflicts
    var temp1 = tempDir.resolve(dirName + "-copy");
    var temp2 = tempDir.resolve(dirName + "-stage");
    deleteDirectory(temp1);
    deleteDirectory(temp2);
    Files.createDirectories(temp1);
    Files.createDirectories(temp2);

    try {
      // Step 1: Copy source files to temp1
      copyDirectory(packageDir, temp1);

      // Step 1b: Dual-ship modern page packages → install *.templateDef (ADR-004 / #2786)
      dualShipPageTemplateDefs(temp1, dirName);

      // Step 2: Read mapping properties
      var propsFile = packageDir.resolve(dirName + MAPPING_EXT);
      var props = new Properties();
      if (Files.exists(propsFile)) {
        try (var fis = new FileInputStream(propsFile.toFile())) {
          props.load(fis);
        }
      }

      // Step 3: Reorganize files from temp1 into temp2 using mapping
      reorganizeFiles(dirName, temp1, temp1.toFile(), temp2, props);

      // Step 4: Zip temp2 into .ppkg
      zipDirectory(temp2, outputFile);
    } finally {
      deleteDirectory(temp1);
      deleteDirectory(temp2);
    }

    built++;
  }

  /**
   * When a package authors modern {@code pages/} component packages, materialize root-level {@code
   * *.templateDef} for deployer install parity. Fail the package build if dual-ship cannot run.
   */
  private static void dualShipPageTemplateDefs(Path stagingPackageDir, String packageName)
      throws IOException {
    try {
      if (!PSPageXmlDualShip.hasModernPageSources(stagingPackageDir)) {
        return;
      }
      int n = PSPageXmlDualShip.materializeInstallTemplateDefs(stagingPackageDir);
      System.out.println(
          "  dual-ship page templateDefs for " + packageName + ": " + n + " written");
    } catch (PSPageXmlException e) {
      throw new IOException(
          "Page dual-ship failed for package " + packageName + ": " + e.getMessage(), e);
    }
  }

  /** Check if the output .ppkg is newer than all source files. */
  private boolean isUpToDate(Path sourceDir, Path outputFile) throws IOException {
    long outputTime = Files.getLastModifiedTime(outputFile).toMillis();
    var result = new boolean[] {true};

    Files.walkFileTree(
        sourceDir,
        new SimpleFileVisitor<>() {
          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            if (attrs.lastModifiedTime().toMillis() > outputTime) {
              result[0] = false;
              return FileVisitResult.TERMINATE;
            }
            return FileVisitResult.CONTINUE;
          }
        });

    return result[0];
  }

  /**
   * Reorganize files from the copied source structure into the .ppkg directory layout, using the
   * mapping properties file to determine target directories.
   *
   * <p>This mirrors the logic from {@code PSPackageBuildToolHelper.moveFilesToOriginalPaths()}.
   */
  private void reorganizeFiles(
      String packageName, Path rootDir, File currentFile, Path destDir, Properties props)
      throws IOException {

    if (!currentFile.isDirectory()) {
      var fileName = currentFile.getName();

      // Skip the mapping properties file
      if (fileName.equals(packageName + MAPPING_EXT)) {
        return;
      }

      // Calculate relative path from root
      var relativePath =
          rootDir.toAbsolutePath().relativize(currentFile.toPath().toAbsolutePath()).toString();
      var actualFile = fileName;
      var pathBefore =
          relativePath.contains(File.separator)
              ? relativePath.substring(0, relativePath.length() - actualFile.length() - 1)
              : "";

      // Check if file is in the mapping properties
      var propValue = props.getProperty(actualFile);

      Path targetFile;
      if (propValue != null) {
        // Mapped file (ContentType, AclDef, TemplateDef, etc.)
        targetFile = destDir.resolve(propValue).resolve(actualFile);
      } else if (pathBefore.startsWith(SYS_USER_DEPENDENCY)) {
        // sys__UserDependency files: encode path
        var subPath = pathBefore.substring(SYS_USER_DEPENDENCY.length());
        var encodedPath = subPath.replace("_", "__").replace(".", "-").replace(File.separator, "_");
        var encodedSuffix = actualFile.replace("-", "--").replace(".", "-").replace("_", "__");
        var fullPath = SYS_USER_DEPENDENCY + encodedPath + File.separator + encodedSuffix;
        fullPath = fullPath.replace(File.separator, "_");
        targetFile = destDir.resolve(fullPath).resolve(actualFile);
      } else if (!pathBefore.isEmpty()) {
        // Extension, Stylesheet, SupportFile, ImageFile
        String pathSuffix;
        if (pathBefore.startsWith(EXTENSION) || pathBefore.startsWith(STYLE_SHEET)) {
          pathSuffix =
              actualFile.contains(".")
                  ? actualFile.substring(0, actualFile.lastIndexOf('.'))
                  : actualFile;
        } else {
          pathSuffix = actualFile;
        }
        var fullPath = pathBefore + File.separator + pathSuffix;
        targetFile = destDir.resolve(fullPath).resolve(actualFile);
      } else {
        // Root-level file
        targetFile = destDir.resolve(actualFile);
      }

      Files.createDirectories(targetFile.getParent());
      Files.copy(currentFile.toPath(), targetFile);
      return;
    }

    // Directory: check if it's a design object directory
    var dirName = currentFile.getName();
    var children = currentFile.listFiles();
    if (children == null) {
      return;
    }

    var isDesignObj =
        (dirName.indexOf('_') == -1
                || dirName.startsWith(SCHEMA_FILE)
                || dirName.startsWith(APPLICATION_FILE))
            && children.length > 0
            && children[0].isFile();

    if (isDesignObj) {
      // Design object directory: files get mapped via properties, handled
      // when we recurse into files
    }

    for (var child : children) {
      reorganizeFiles(packageName, rootDir, child, destDir, props);
    }
  }

  private static void zipDirectory(Path sourceDir, Path zipFile) throws IOException {
    Files.createDirectories(zipFile.getParent());
    try (OutputStream fos = Files.newOutputStream(zipFile);
        var zos = new ZipOutputStream(fos)) {

      Files.walkFileTree(
          sourceDir,
          new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                throws IOException {
              var entryName = sourceDir.relativize(file).toString().replace('\\', '/');
              zos.putNextEntry(new ZipEntry(entryName));
              Files.copy(file, zos);
              zos.closeEntry();
              return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                throws IOException {
              if (!dir.equals(sourceDir)) {
                var entryName = sourceDir.relativize(dir).toString().replace('\\', '/') + "/";
                zos.putNextEntry(new ZipEntry(entryName));
                zos.closeEntry();
              }
              return FileVisitResult.CONTINUE;
            }
          });
    }
  }

  private static void copyDirectory(Path source, Path target) throws IOException {
    Files.walkFileTree(
        source,
        new SimpleFileVisitor<>() {
          @Override
          public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
              throws IOException {
            Files.createDirectories(target.resolve(source.relativize(dir)));
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
              throws IOException {
            Files.copy(file, target.resolve(source.relativize(file)));
            return FileVisitResult.CONTINUE;
          }
        });
  }

  private static void deleteDirectory(Path dir) throws IOException {
    if (!Files.exists(dir)) {
      return;
    }
    Files.walkFileTree(
        dir,
        new SimpleFileVisitor<>() {
          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
              throws IOException {
            Files.delete(file);
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            Files.delete(dir);
            return FileVisitResult.CONTINUE;
          }
        });
  }
}
