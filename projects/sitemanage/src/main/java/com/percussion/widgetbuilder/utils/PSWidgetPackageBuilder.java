// REFACTORED: CP-JAVA11
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
package com.percussion.widgetbuilder.utils;

import com.percussion.security.validation.PathValidation;
import com.percussion.utils.PSTokenReplacingReader;
import com.percussion.widgetbuilder.utils.xform.PSAclFileTransformer;
import com.percussion.widgetbuilder.utils.xform.PSContentTypeFileTransformer;
import com.percussion.widgetbuilder.utils.xform.PSControlManager;
import com.percussion.widgetbuilder.utils.xform.PSResourceFileTransformer;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

/**
 * Builds a widget package from a supplied specification.
 *
 * <p>Sunny Sal says: "Packaging widgets is like packing for a Goa trip—don't forget anything, and
 * zip it up tight!"
 */
public class PSWidgetPackageBuilder {

  private static final String BIN_EXT = ".png";
  private static final String WIDGET_TEMPLATE_NAME = "percWidgetTemplate";

  private List<IPSWidgetFileTransformer> xformList = new ArrayList<>();
  private final File srcFile;
  private final File tmpDir;

  /**
   * Create a reusable instance of the package builder for a given source widget package file.
   *
   * @param srcFile A valid file reference to a source template pkg file, not {@code null}.
   * @param tmpDir A valid file reference to a temp directory to use.
   */
  public PSWidgetPackageBuilder(File srcFile, File tmpDir) {
    Validate.notNull(srcFile, "srcFile must not be null");
    Validate.notNull(tmpDir, "tmpDir must not be null");

    this.srcFile = srcFile;
    this.tmpDir = tmpDir;
    xformList.add(new PSAclFileTransformer());
    xformList.add(new PSContentTypeFileTransformer(new PSControlManager()));
    xformList.add(new PSResourceFileTransformer());
  }

  /**
   * Can override the default file transformers, used by unit tests.
   *
   * @param xformList The list of file transformers to use, not {@code null}.
   */
  public void setFileTransformers(List<IPSWidgetFileTransformer> xformList) {
    Validate.notNull(xformList, "xformList must not be null");
    this.xformList = xformList;
  }

  /**
   * Generate the package file in the specified directory using the supplied spec.
   *
   * @param tgtDir The directory in which the package is to be created, not {@code null}
   * @param packageSpec The package spec to use, not {@code null}.
   * @return A reference to the created package file.
   * @throws PSWidgetPackageBuilderException if there are any errors.
   */
  public File generatePackage(File tgtDir, PSWidgetPackageSpec packageSpec)
      throws PSWidgetPackageBuilderException {
    Validate.notNull(tgtDir, "tgtDir must not be null");
    Validate.notNull(packageSpec, "packageSpec must not be null");

    if (!tgtDir.isDirectory()) {
      throw new IllegalArgumentException("tgtDir must be a valid directory");
    }

    var tmpPkgDir = extractAndResolveFiles(packageSpec);
    return createPackage(tmpPkgDir, packageSpec, tgtDir);
  }

  /**
   * Extract the files from the template src zip file, transforming as necessary.
   *
   * @param packageSpec The spec to use for transforms
   * @return a file reference to the directory containing the package files
   * @throws PSWidgetPackageBuilderException if there are any errors.
   */
  private File extractAndResolveFiles(PSWidgetPackageSpec packageSpec)
      throws PSWidgetPackageBuilderException {
    var rootDir = new File(tmpDir, packageSpec.getWidgetName());

    try {
      // Clean up any existing directory
      if (rootDir.exists()) {
        FileUtils.deleteDirectory(rootDir);
      }
      // PathValidation.constructSafePath requires baseDir to exist.
      if (!rootDir.mkdirs() && !rootDir.isDirectory()) {
        throw new IOException("Could not create package extract directory: " + rootDir);
      }

      try (var in = new FileInputStream(srcFile);
          var zin = new ZipInputStream(in)) {

        ZipEntry entry = zin.getNextEntry();
        while (entry != null) {
          if (!entry.isDirectory()) {
            // CWE-22 / java/zipslip #722: validate resolved entry path under rootDir before any
            // FileOutputStream / mkdirs. resolvePath may rewrite template tokens but must still
            // stay relative; PathValidation rejects absolute paths and .. traversal.
            var resolvePath = resolvePath(entry.getName(), packageSpec);
            var file = PathValidation.constructSafePath(rootDir, resolvePath);
            var xform = getFileTransformer(file);
            if (xform != null) {
              file = xform.transformPath(file, packageSpec);
              // Re-check after transform so a transformer cannot re-introduce ZipSlip.
              PathValidation.validatePathWithinDirectory(file, rootDir);
            }
            File parent = file.getParentFile();
            if (parent != null) {
              parent.mkdirs();
            }

            try (var fout = new FileOutputStream(file)) {
              if (isTextFile(file)) {
                try (var reader =
                    new PSTokenReplacingReader(
                        new InputStreamReader(zin), new PSWidgetPackageResolver(packageSpec))) {
                  var transformedReader =
                      (xform != null) ? xform.transformFile(file, reader, packageSpec) : reader;
                  IOUtils.copy(transformedReader, fout, "UTF-8");
                }
              } else {
                IOUtils.copy(zin, fout);
              }
            }
          }
          zin.closeEntry();
          entry = zin.getNextEntry();
        }
      }
      return rootDir;
    } catch (Exception e) {
      throw new PSWidgetPackageBuilderException(
          "Error generating widget package file contents: " + e.getLocalizedMessage(), e);
    }
  }

  /**
   * Get a transformer for the specified file.
   *
   * @param file The file to check.
   * @return The transformer to use, or {@code null} if none is found.
   */
  private IPSWidgetFileTransformer getFileTransformer(File file) {
    return xformList.stream().filter(xform -> xform.handleFile(file)).findFirst().orElse(null);
  }

  /**
   * Determine if the supplied file reference is a transformable text file, or a binary file.
   *
   * @param file The file to check.
   * @return {@code true} if a text file, {@code false} if a binary.
   */
  private boolean isTextFile(File file) {
    return !file.getName().endsWith(BIN_EXT);
  }

  /**
   * Resolve the supplied path with the supplied package spec.
   *
   * @param path The path to resolve.
   * @param packageSpec The package spec.
   * @return The resolved path.
   */
  private String resolvePath(String path, PSWidgetPackageSpec packageSpec) {
    return StringUtils.replace(path, WIDGET_TEMPLATE_NAME, packageSpec.getFullWidgetName());
  }

  /**
   * Create the package file from the files in the supplied directory.
   *
   * @param packageSpec The package spec to use.
   * @param tmpPkgDir The temp directory containing the files to add to the package.
   * @param tgtDir The directory to create the package file in.
   * @return A reference to the created package file.
   * @throws PSWidgetPackageBuilderException if there are any errors.
   */
  private File createPackage(File tmpPkgDir, PSWidgetPackageSpec packageSpec, File tgtDir)
      throws PSWidgetPackageBuilderException {
    var tgtFile = new File(tgtDir, packageSpec.getPackageName() + ".ppkg");
    try (var zout = new ZipOutputStream(new FileOutputStream(tgtFile))) {
      writeFiles(tmpPkgDir, zout, tmpPkgDir);
      return tgtFile;
    } catch (Exception e) {
      throw new PSWidgetPackageBuilderException(
          "Error writing widget package file: " + e.getLocalizedMessage(), e);
    }
  }

  /**
   * Recursively write files to the zip stream, recursing into subdirectories.
   *
   * @param filesDir The directory to check for files.
   * @param zout The zip output stream to write to.
   * @param rootDir Root directory of all files, zip entry path should be relative to this.
   * @throws IOException if there are any IO errors.
   */
  private void writeFiles(File filesDir, ZipOutputStream zout, File rootDir) throws IOException {
    var files = filesDir.listFiles();
    if (files == null) {
      return;
    }
    for (var file : files) {
      if (file.isDirectory()) {
        writeFiles(file, zout, rootDir);
        continue;
      }
      try (var fin = new FileInputStream(file)) {
        // Remove tmpdir and widget name from path
        var path = file.getCanonicalPath();
        path = StringUtils.removeStart(path, rootDir.getCanonicalPath());
        path = StringUtils.removeStart(path, File.separator);
        if ('\\' == File.separatorChar) {
          path = path.replace('\\', '/');
        }
        zout.putNextEntry(new ZipEntry(path));
        IOUtils.copy(fin, zout);
        zout.closeEntry();
      }
    }
  }
}
