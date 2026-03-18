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

package com.percussion.packages;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Builds .ppkg files from the exploded package source directories in system/Packages. Applies the
 * same path transformation as PSPackageBuildToolHelper.moveFilesToOriginalPaths in the perc-ant
 * module: root files stay at root, sys__UserDependency-- paths are mangled to a flat single-level
 * directory, Extension/Stylesheet files get a subdirectory named by the file base name, and all
 * other files in subdirectories get a subdirectory named by the full filename. The
 * .mapping.properties file maps design-object filenames to their target directories.
 */
public class PSPackageBuilder {

  static final String SYS_USER_DEPENDENCY = "sys__UserDependency--";
  static final String EXTENSION_DIR = "Extension";
  static final String STYLE_SHEET_DIR = "Stylesheet";
  static final String MAPPING_EXT = ".mapping.properties";

  public static void main(String[] args) throws IOException {
    if (args.length < 2) {
      System.err.println("Usage: PSPackageBuilder <sourceDir> <outputDir> [<tempDir>]");
      System.exit(1);
    }

    File sourceDir = new File(args[0]);
    File outputDir = new File(args[1]);

    if (!sourceDir.exists() || !sourceDir.isDirectory()) {
      System.err.println("Source directory does not exist: " + sourceDir);
      System.exit(1);
    }

    outputDir.mkdirs();

    File[] packages = sourceDir.listFiles(File::isDirectory);
    if (packages == null) {
      return;
    }

    for (File pkg : packages) {
      String pkgName = pkg.getName();
      if ("Percussion".equals(pkgName) || "packageholder".equals(pkgName)) {
        continue;
      }
      File outputFile = new File(outputDir, pkgName + ".ppkg");
      System.out.println("Building package: " + outputFile.getName());
      buildPackage(pkg, pkgName, outputFile);
    }
  }

  static void buildPackage(File pkgDir, String pkgName, File outputFile) throws IOException {
    Properties props = loadMappingProperties(pkgDir, pkgName);

    // Collect ordered file→entryName pairs
    Map<File, String> fileEntries = new LinkedHashMap<>();
    collectFileEntries(pkgDir, pkgDir, pkgName, props, fileEntries);

    // Collect all unique ancestor directory paths needed by the file entries
    Set<String> dirs = new LinkedHashSet<>();
    for (String entryName : fileEntries.values()) {
      int slash = entryName.indexOf('/');
      while (slash >= 0 && slash < entryName.length() - 1) {
        dirs.add(entryName.substring(0, slash + 1));
        slash = entryName.indexOf('/', slash + 1);
      }
    }

    try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outputFile))) {
      for (String dir : dirs) {
        zos.putNextEntry(new ZipEntry(dir));
        zos.closeEntry();
      }
      for (Map.Entry<File, String> entry : fileEntries.entrySet()) {
        zos.putNextEntry(new ZipEntry(entry.getValue()));
        try (FileInputStream fis = new FileInputStream(entry.getKey())) {
          byte[] buf = new byte[8192];
          int len;
          while ((len = fis.read(buf)) > 0) {
            zos.write(buf, 0, len);
          }
        }
        zos.closeEntry();
      }
    }
  }

  private static void collectFileEntries(
      File rootDir, File currentDir, String pkgName, Properties props, Map<File, String> result) {
    File[] files = currentDir.listFiles();
    if (files == null) {
      return;
    }
    Arrays.sort(files);
    for (File file : files) {
      if (file.isDirectory()) {
        collectFileEntries(rootDir, file, pkgName, props, result);
      } else {
        String relativePath = getRelativePath(rootDir, file).replace(File.separatorChar, '/');
        if (relativePath.equals(pkgName + MAPPING_EXT)) {
          continue; // skip the mapping properties file
        }
        String entryName = computeEntryName(relativePath, props);
        if (entryName != null) {
          result.put(file, entryName);
        }
      }
    }
  }

  /**
   * Computes the zip entry name for a file using the same logic as
   * PSPackageBuildToolHelper.moveFilesToOriginalPaths. Uses '/' as the separator throughout.
   *
   * @param relSlash relative path from the package root, using '/' separators
   * @param props the package's .mapping.properties (may be empty, not null)
   * @return the zip entry name, or null to skip this file
   */
  static String computeEntryName(String relSlash, Properties props) {
    int lastSlash = relSlash.lastIndexOf('/');
    String fileName = lastSlash >= 0 ? relSlash.substring(lastSlash + 1) : relSlash;
    String pathBefore = lastSlash >= 0 ? relSlash.substring(0, lastSlash) : "";

    // 1. Design objects mapped via .mapping.properties (ContentType, AclDef, Schema, etc.)
    if (props != null && props.containsKey(fileName)) {
      String dirName = props.getProperty(fileName);
      return dirName + "/" + fileName;
    }

    // 2. sys__UserDependency-- files: flatten the deep path into a single mangled directory
    if (pathBefore.startsWith(SYS_USER_DEPENDENCY)) {
      // Strip the sys__UserDependency-- prefix from the directory portion
      String innerPath = pathBefore.substring(SYS_USER_DEPENDENCY.length());
      // Apply the same transforms as the ant helper: _ -> __, . -> -, / -> _
      String mangledPath = innerPath.replace("_", "__").replace(".", "-").replace("/", "_");
      // Apply the filename transforms: - -> --, . -> -, _ -> __
      String mangledFile = fileName.replace("-", "--").replace(".", "-").replace("_", "__");
      String flatDir =
          SYS_USER_DEPENDENCY + (mangledPath.isEmpty() ? "" : mangledPath + "_") + mangledFile;
      return flatDir + "/" + fileName;
    }

    // 3. Files in subdirectories: Extension, Stylesheet, SupportFile, ImageFile, etc.
    if (!pathBefore.isEmpty()) {
      String topDir =
          pathBefore.contains("/") ? pathBefore.substring(0, pathBefore.indexOf('/')) : pathBefore;
      String pathSuffix;
      if (topDir.equals(EXTENSION_DIR)
          || topDir.startsWith(EXTENSION_DIR + "-")
          || topDir.equals(STYLE_SHEET_DIR)
          || topDir.startsWith(STYLE_SHEET_DIR + "-")) {
        // Extension/Stylesheet: use filename without extension as the final subdirectory
        int dot = fileName.lastIndexOf('.');
        pathSuffix = dot > 0 ? fileName.substring(0, dot) : fileName;
      } else {
        // SupportFile, ImageFile, LocalContent, etc.: use full filename as subdirectory
        pathSuffix = fileName;
      }
      return pathBefore + "/" + pathSuffix + "/" + fileName;
    }

    // 4. Root-level files (psx_archiveInfo.xml, psx_archiveManifest.xml, etc.)
    return fileName;
  }

  static String getRelativePath(File rootDir, File file) {
    String rootPath = rootDir.getAbsolutePath();
    String filePath = file.getAbsolutePath();
    if (filePath.startsWith(rootPath)) {
      String relative = filePath.substring(rootPath.length());
      if (relative.startsWith(File.separator)) {
        relative = relative.substring(1);
      }
      return relative;
    }
    return file.getName();
  }

  private static Properties loadMappingProperties(File pkgDir, String pkgName) {
    Properties props = new Properties();
    File propsFile = new File(pkgDir, pkgName + MAPPING_EXT);
    if (propsFile.exists()) {
      try (FileInputStream fis = new FileInputStream(propsFile)) {
        props.load(fis);
      } catch (IOException e) {
        System.err.println("Warning: Could not read mapping file: " + e.getMessage());
      }
    }
    return props;
  }
}
