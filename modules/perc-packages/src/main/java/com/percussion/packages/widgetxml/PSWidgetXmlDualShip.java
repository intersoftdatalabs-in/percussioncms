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

import com.percussion.packages.manifest.PSComponentPackageManifest;
import com.percussion.packages.manifest.PSComponentPackageManifestException;
import com.percussion.packages.manifest.PSComponentPackageManifestIo;
import com.percussion.packages.manifest.PSComponentPackageManifestValidator;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Dual-ship bridge for product Widget packages (ADR-004 / issue #2831 batch A, parent #2630).
 *
 * <p><strong>Authoring truth (modern):</strong> {@code widgets/&lt;widgetStem&gt;/component-package.json}
 * plus template sources under the product package tree (e.g. {@code perc.baseWidgets}).
 *
 * <p><strong>Install path (dual-run):</strong> product packages still ship legacy {@code
 * sys__UserDependency--rxconfig/Widgets/*.xml} so deployer / {@code PSWidgetDao} install is
 * unchanged. Modern roots are committed so selection prefers Component Package Manifest when both
 * exist ({@code PSLegacyDefinitionXmlShim}). Do not mass-delete remaining Widget XML until a native
 * install path exists (Phase 5 / #2632).
 *
 * <p>Batch A (#2831): {@code perc.baseWidgets}, {@code perc.defaultLanguage}, {@code
 * perc.eventWidget}, {@code perc.openGraphWidget}, {@code perc.twitterSummaryCards} (8 widgets).
 *
 * @see PSWidgetXmlCompiler
 * @see PSWidgetXmlPackageCompiler
 */
public final class PSWidgetXmlDualShip {

  /** Package-relative directory holding per-widget modern component packages. */
  public static final String WIDGETS_DIR_NAME = "widgets";

  /**
   * Batch A dual-ship exit package directory names under {@code Packages/} (issue #2831). Coherent
   * base/core set: baseWidgets (3) + defaultLanguage (2) + event + openGraph + twitter cards.
   */
  public static final List<String> BATCH_A_PACKAGE_DIRS =
      List.of(
          "perc.baseWidgets",
          "perc.defaultLanguage",
          "perc.eventWidget",
          "perc.openGraphWidget",
          "perc.twitterSummaryCards");

  /** Expected modern widget stems for batch A (stable for tests / residual counting). */
  public static final List<String> BATCH_A_WIDGET_STEMS =
      List.of(
          "percSimpleText",
          "percRichText",
          "percRawHtml",
          "percDefaultLang",
          "percLocalLang",
          "percEvent",
          "percOpenGraph",
          "percTwitterSummaryCards");

  private PSWidgetXmlDualShip() {
    // utility
  }

  /**
   * CLI for one-time migration / dual-ship ops.
   *
   * <p>Usage:
   *
   * <ul>
   *   <li>{@code materialize-modern &lt;packageDir&gt;} — Widget XML → {@code widgets/}
   *   <li>{@code materialize-modern-batch-a &lt;packagesRoot&gt;} — batch A packages only
   * </ul>
   */
  public static void main(String[] args) throws Exception {
    final String usage =
        "Usage: PSWidgetXmlDualShip materialize-modern|materialize-modern-batch-a <path>";
    if (args.length < 2) {
      System.err.println(usage);
      System.exit(1);
    }
    String cmd = args[0].trim().toLowerCase(Locale.ROOT);
    Path path = Path.of(args[1]).toAbsolutePath().normalize();
    int n;
    switch (cmd) {
      case "materialize-modern" -> n = materializeModernWidgetSources(path);
      case "materialize-modern-batch-a" -> n = materializeModernBatchA(path);
      default -> {
        System.err.println("Unknown command: " + args[0]);
        System.err.println(usage);
        System.exit(1);
        return;
      }
    }
    System.out.println(cmd + " wrote " + n + " modern widget package(s) under " + path);
  }

  /**
   * Whether the package root contains modern widget authoring sources under {@value
   * #WIDGETS_DIR_NAME}.
   */
  public static boolean hasModernWidgetSources(Path packageDir) throws IOException {
    Objects.requireNonNull(packageDir, "packageDir");
    return !listModernWidgetDirs(packageDir).isEmpty();
  }

  /**
   * Materialize modern {@code widgets/&lt;stem&gt;/} sources from package Widget XML (dual-ship
   * authoring path). Overwrites existing modern trees for the same stem.
   *
   * @param packageDir product package source (e.g. {@code …/Packages/perc.baseWidgets})
   * @return number of modern widget packages written
   * @throws PSWidgetXmlException on parse/compile failure
   * @throws IOException on I/O failure
   */
  public static int materializeModernWidgetSources(Path packageDir)
      throws PSWidgetXmlException, IOException {
    Objects.requireNonNull(packageDir, "packageDir");
    if (!Files.isDirectory(packageDir)) {
      throw new PSWidgetXmlException("Package directory does not exist: " + packageDir);
    }
    Path widgetsXmlDir = PSWidgetXmlPackageCompiler.resolveWidgetsDir(packageDir);
    if (!Files.isDirectory(widgetsXmlDir)) {
      return 0;
    }
    List<PSWidgetXmlCompileResult> compiled = PSWidgetXmlPackageCompiler.compilePackage(packageDir);
    Path widgets = packageDir.resolve(WIDGETS_DIR_NAME);
    Files.createDirectories(widgets);
    int written = 0;
    for (PSWidgetXmlCompileResult result : compiled) {
      String stem = result.getSource().widgetStem();
      if (stem == null || stem.isBlank()) {
        stem = result.getManifest().getId();
      }
      Path out = widgets.resolve(stem);
      PSWidgetXmlCompiler.writeArtifacts(result, out);
      written++;
    }
    return written;
  }

  /**
   * Materialize modern widget sources for every batch A package under {@code packagesRoot}. Missing
   * package directories are soft-skipped.
   *
   * @param packagesRoot {@code Packages/} directory
   * @return total modern widget packages written across batch A
   */
  public static int materializeModernBatchA(Path packagesRoot)
      throws PSWidgetXmlException, IOException {
    Objects.requireNonNull(packagesRoot, "packagesRoot");
    if (!Files.isDirectory(packagesRoot)) {
      throw new PSWidgetXmlException("Packages root does not exist: " + packagesRoot);
    }
    int total = 0;
    for (String dirName : BATCH_A_PACKAGE_DIRS) {
      Path packageDir = packagesRoot.resolve(dirName);
      if (!Files.isDirectory(packageDir)) {
        continue;
      }
      total += materializeModernWidgetSources(packageDir);
    }
    return total;
  }

  /**
   * Compile (load + validate) all modern widget packages under {@code widgets/}.
   *
   * @param packageDir product package source
   * @return validated results sorted by widget id
   */
  public static List<PSWidgetXmlCompileResult> compileModernWidgets(Path packageDir)
      throws PSWidgetXmlException, IOException {
    Objects.requireNonNull(packageDir, "packageDir");
    List<Path> widgetDirs = listModernWidgetDirs(packageDir);
    if (widgetDirs.isEmpty()) {
      throw new PSWidgetXmlException(
          "No modern widget packages under: " + packageDir.resolve(WIDGETS_DIR_NAME));
    }
    List<PSWidgetXmlCompileResult> results = new ArrayList<>();
    for (Path widgetDir : widgetDirs) {
      results.add(loadModernAsCompileResult(widgetDir));
    }
    results.sort(
        Comparator.comparing(
            r -> r.getManifest().getId() != null ? r.getManifest().getId() : "",
            String.CASE_INSENSITIVE_ORDER));
    return results;
  }

  /**
   * Load a modern widget package directory into a {@link PSWidgetXmlCompileResult} for parity tests
   * and dual-ship selection.
   */
  public static PSWidgetXmlCompileResult loadModernAsCompileResult(Path widgetDir)
      throws PSWidgetXmlException, IOException {
    Objects.requireNonNull(widgetDir, "widgetDir");
    Path manifestPath = widgetDir.resolve(PSComponentPackageManifest.DEFAULT_MANIFEST_FILE_NAME);
    if (!Files.isRegularFile(manifestPath)) {
      throw new PSWidgetXmlException("Missing component-package.json under " + widgetDir);
    }
    PSComponentPackageManifest manifest;
    try {
      manifest = PSComponentPackageManifestIo.read(manifestPath);
      PSComponentPackageManifestValidator.validate(manifest);
    } catch (PSComponentPackageManifestException e) {
      throw new PSWidgetXmlException(
          "Invalid modern widget package at " + widgetDir + ": " + e.getMessage(), e);
    }

    Map<String, String> artifacts = new LinkedHashMap<>();
    if (manifest.getTemplates() != null) {
      for (PSComponentPackageManifest.TemplateRef t : manifest.getTemplates()) {
        if (t == null || t.getSourceRef() == null || t.getSourceRef().isBlank()) {
          continue;
        }
        artifacts.put(t.getSourceRef(), readTemplateSourceByRef(widgetDir, t.getSourceRef()));
      }
    }

    // Synthetic model for dual-ship tests (stem + title from modern id/name).
    PSWidgetXmlModel model = new PSWidgetXmlModel();
    model.setTitle(manifest.getName());
    model.setDescription(manifest.getDescription());
    String stem = manifest.getId();
    if (stem == null || stem.isBlank()) {
      stem = widgetDir.getFileName() != null ? widgetDir.getFileName().toString() : "widget";
    }
    model.setSourceFileName(stem + ".xml");
    if (manifest.getContentTypes() != null && !manifest.getContentTypes().isEmpty()) {
      model.setContentTypeName(manifest.getContentTypes().get(0).getName());
    }
    if (manifest.getCatalog() != null) {
      model.setCategory(manifest.getCatalog().getCategory());
      model.setAuthor(manifest.getCatalog().getAuthor());
    }
    if (manifest.getTemplates() != null && !manifest.getTemplates().isEmpty()) {
      model.setContentType("velocity");
      model.setCodeType("jexl");
      String body = artifacts.values().stream().findFirst().orElse("");
      model.setContentBody(body);
    }

    return new PSWidgetXmlCompileResult(model, manifest, artifacts);
  }

  /**
   * List modern widget package directories under {@code packageDir/widgets} that contain a
   * {@code component-package.json}. Portable {@link Path} resolve only.
   */
  public static List<Path> listModernWidgetDirs(Path packageDir) throws IOException {
    Objects.requireNonNull(packageDir, "packageDir");
    Path widgets = packageDir.resolve(WIDGETS_DIR_NAME);
    if (!Files.isDirectory(widgets)) {
      return List.of();
    }
    List<Path> dirs = new ArrayList<>();
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(widgets)) {
      for (Path child : stream) {
        if (!Files.isDirectory(child)) {
          continue;
        }
        Path manifest = child.resolve(PSComponentPackageManifest.DEFAULT_MANIFEST_FILE_NAME);
        if (Files.isRegularFile(manifest)) {
          dirs.add(child);
        }
      }
    }
    dirs.sort(
        Comparator.comparing(
            p -> p.getFileName().toString().toLowerCase(Locale.ROOT)));
    return dirs;
  }

  /**
   * Collect modern widget package roots for dual-run selection ({@code
   * PSLegacyDefinitionXmlShim#selectDefinition}).
   *
   * @param packageDir product package source that may contain {@code widgets/}
   * @return list of modern widget package directories (may be empty)
   */
  public static List<Path> modernWidgetPackageRoots(Path packageDir) throws IOException {
    return listModernWidgetDirs(packageDir);
  }

  private static String readTemplateSourceByRef(Path widgetDir, String sourceRef)
      throws PSWidgetXmlException, IOException {
    Path resolved = resolvePackageRelative(widgetDir, sourceRef);
    if (!Files.isRegularFile(resolved)) {
      throw new PSWidgetXmlException(
          "Template source missing for modern widget at " + widgetDir + ": " + sourceRef);
    }
    return Files.readString(resolved);
  }

  /**
   * Resolve a package-relative path under {@code root} without accepting absolute or {@code ..}
   * escapes. Delegates to {@link PSWidgetXmlCompiler#resolvePackageRelative(Path, String)} so widget
   * dual-ship and the Widget XML compiler share one implementation.
   */
  static Path resolvePackageRelative(Path root, String relative) {
    Objects.requireNonNull(root, "root");
    return PSWidgetXmlCompiler.resolvePackageRelative(root, relative);
  }
}
