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

package com.percussion.packages.pagexml;

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
 * Compiles page layout packages from product package source trees (e.g. {@code perc.baseTemplates},
 * {@code perc.responsiveTemplates}).
 *
 * <p><strong>Authoring preference (ADR-004 / #2786):</strong> modern {@code pages/&lt;id&gt;/}
 * component packages. Falls back to root-level {@code *.templateDef} for upgrade-input / dual-run
 * staging. Package build dual-ships modern → install {@code *.templateDef} via {@link
 * PSPageXmlDualShip}.
 */
public final class PSPageXmlPackageCompiler {

  private PSPageXmlPackageCompiler() {
    // utility
  }

  /**
   * Compile every page template in the package: prefer modern {@code pages/*} sources when present;
   * otherwise compile root-level {@code *.templateDef} (upgrade-input / legacy dual-run).
   *
   * @param packageDir non-null package source root (e.g. {@code …/Packages/perc.baseTemplates})
   * @return compile results sorted by template name (stable for golden tests)
   * @throws PSPageXmlException on parse/compile failure
   * @throws IOException on I/O failure
   */
  public static List<PSPageXmlCompileResult> compilePackage(Path packageDir)
      throws PSPageXmlException, IOException {
    Objects.requireNonNull(packageDir, "packageDir");
    if (!Files.isDirectory(packageDir)) {
      throw new PSPageXmlException("Package directory does not exist: " + packageDir);
    }

    if (PSPageXmlDualShip.hasModernPageSources(packageDir)) {
      return PSPageXmlDualShip.compileModernPages(packageDir);
    }

    PSPageXmlPackageContext ctx = PSPageXmlPackageContext.fromPackageDir(packageDir);
    List<Path> templateFiles = listTemplateDefs(packageDir);
    if (templateFiles.isEmpty()) {
      throw new PSPageXmlException(
          "No modern pages/ sources or *.templateDef files found in package: " + packageDir);
    }

    List<PSPageXmlCompileResult> results = new ArrayList<>();
    for (Path templateDef : templateFiles) {
      results.add(PSPageXmlCompiler.compile(templateDef, ctx));
    }
    return results;
  }

  /**
   * Write each page compile result under {@code outputRoot/<templateStem>/}.
   *
   * @param results non-null list of compile results
   * @param outputRoot non-null output root directory
   * @throws IOException on I/O failure
   */
  public static void writeAll(List<PSPageXmlCompileResult> results, Path outputRoot)
      throws IOException {
    Objects.requireNonNull(results, "results");
    Objects.requireNonNull(outputRoot, "outputRoot");
    Files.createDirectories(outputRoot);
    for (PSPageXmlCompileResult result : results) {
      String stem = result.getSource().pageStem();
      if (stem == null || stem.isBlank()) {
        stem = result.getManifest().getId();
      }
      Path pageOut = outputRoot.resolve(stem);
      PSPageXmlCompiler.writeArtifacts(result, pageOut);
    }
  }

  /**
   * List {@code *.templateDef} files at package root, sorted by file name (case-insensitive).
   * Portable: uses {@link DirectoryStream} + {@link Path}, not hardcoded separators.
   */
  public static List<Path> listTemplateDefs(Path packageDir) throws IOException {
    List<Path> files = new ArrayList<>();
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(packageDir, "*.templateDef")) {
      for (Path p : stream) {
        if (Files.isRegularFile(p)) {
          files.add(p);
        }
      }
    }
    files.sort(Comparator.comparing(p -> p.getFileName().toString().toLowerCase(Locale.ROOT)));
    return files;
  }
}
