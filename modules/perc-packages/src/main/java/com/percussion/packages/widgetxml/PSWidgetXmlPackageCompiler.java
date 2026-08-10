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

package com.percussion.packages.widgetxml;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Compiles all Widget definition XML files found in a legacy product package source directory.
 *
 * <p>Looks under {@code sys__UserDependency--rxconfig/Widgets/*.xml} (product packaging layout used
 * by {@code modules/perc-packages}). Suitable for the baseWidgets subset first; residual packages
 * are tracked in the widget XML inventory.
 */
public final class PSWidgetXmlPackageCompiler {

  /** Legacy staging folder name for user-dependency config inside a package source tree. */
  public static final String WIDGETS_RELATIVE =
      "sys__UserDependency--rxconfig/Widgets";

  private PSWidgetXmlPackageCompiler() {
    // utility
  }

  /**
   * Compile every Widget XML under the package's Widgets directory.
   *
   * @param packageDir non-null package source root (e.g. {@code …/Packages/perc.baseWidgets})
   * @return compile results sorted by widget stem (stable for golden tests)
   * @throws PSWidgetXmlException on parse/compile failure
   * @throws IOException on I/O failure
   */
  public static List<PSWidgetXmlCompileResult> compilePackage(Path packageDir)
      throws PSWidgetXmlException, IOException {
    Objects.requireNonNull(packageDir, "packageDir");
    if (!Files.isDirectory(packageDir)) {
      throw new PSWidgetXmlException("Package directory does not exist: " + packageDir);
    }

    PSWidgetXmlPackageContext ctx = PSWidgetXmlPackageContext.fromPackageDir(packageDir);
    Path widgetsDir = resolveWidgetsDir(packageDir);
    if (!Files.isDirectory(widgetsDir)) {
      throw new PSWidgetXmlException(
          "Widgets directory not found under package (expected "
              + WIDGETS_RELATIVE
              + "): "
              + packageDir);
    }

    List<Path> widgetFiles = new ArrayList<>();
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(widgetsDir, "*.xml")) {
      for (Path p : stream) {
        if (Files.isRegularFile(p)) {
          widgetFiles.add(p);
        }
      }
    }
    widgetFiles.sort(
        Comparator.comparing(p -> p.getFileName().toString().toLowerCase(Locale.ROOT)));

    if (widgetFiles.isEmpty()) {
      throw new PSWidgetXmlException("No Widget XML files found in: " + widgetsDir);
    }

    List<PSWidgetXmlCompileResult> results = new ArrayList<>();
    for (Path widgetXml : widgetFiles) {
      results.add(PSWidgetXmlCompiler.compile(widgetXml, ctx));
    }
    return results;
  }

  /**
   * Write each widget compile result under {@code outputRoot/<widgetStem>/}.
   *
   * @param results non-null list of compile results
   * @param outputRoot non-null output root directory
   * @throws IOException on I/O failure
   */
  public static void writeAll(List<PSWidgetXmlCompileResult> results, Path outputRoot)
      throws IOException {
    Objects.requireNonNull(results, "results");
    Objects.requireNonNull(outputRoot, "outputRoot");
    Files.createDirectories(outputRoot);
    for (PSWidgetXmlCompileResult result : results) {
      String stem = result.getSource().widgetStem();
      if (stem == null || stem.isBlank()) {
        stem = result.getManifest().getId();
      }
      Path widgetOut = outputRoot.resolve(stem);
      PSWidgetXmlCompiler.writeArtifacts(result, widgetOut);
    }
  }

  /**
   * Resolve the Widgets directory under a package using portable path segments (not string
   * concatenation with hardcoded separators).
   */
  static Path resolveWidgetsDir(Path packageDir) {
    Path p = packageDir;
    for (String segment : WIDGETS_RELATIVE.split("/")) {
      p = p.resolve(segment);
    }
    return p;
  }
}
