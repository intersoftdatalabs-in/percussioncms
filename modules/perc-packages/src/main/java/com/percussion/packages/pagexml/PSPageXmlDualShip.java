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

import com.percussion.packages.manifest.PSComponentPackageManifest;
import com.percussion.packages.manifest.PSComponentPackageManifestException;
import com.percussion.packages.manifest.PSComponentPackageManifestIo;
import com.percussion.packages.manifest.PSComponentPackageManifestValidator;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Dual-ship bridge for product page layout packages (ADR-004 / issues #2786 + #2806).
 *
 * <p><strong>Authoring truth:</strong> {@code pages/&lt;templateId&gt;/component-package.json} plus
 * template sources under the product package tree (e.g. {@code perc.baseTemplates}, {@code
 * perc.responsiveTemplates}).
 *
 * <p><strong>Install path (dual-ship mode):</strong> package build materializes root-level {@code
 * *.templateDef} files (legacy {@code TemplateDef} dependency) so reorganize + {@code .ppkg}
 * install parity is preserved. Prefer {@link PSPageXmlNativeInstall} / {@link
 * PSPageXmlInstallMode#NATIVE} (package-local or system property) to stage archive {@code
 * TemplateDef-N/} folders without dual-ship root files — see {@link PSPageXmlInstallPolicy}.
 *
 * <p>GUIDs for install templateDefs are derived from {@code &lt;package&gt;.mapping.properties}
 * entries ({@code TemplateDef-N} → {@code 0-4-N}).
 */
public final class PSPageXmlDualShip {

  /** Package-relative directory holding per-template modern component packages. */
  public static final String PAGES_DIR_NAME = "pages";

  private static final Pattern TEMPLATE_DEF_KEY =
      Pattern.compile("^(.+)\\.templateDef$", Pattern.CASE_INSENSITIVE);
  private static final Pattern TEMPLATE_DEF_VALUE =
      Pattern.compile("^TemplateDef-(\\d+)$", Pattern.CASE_INSENSITIVE);

  private PSPageXmlDualShip() {
    // utility
  }

  /**
   * CLI for one-time migration / dual-ship ops.
   *
   * <p>Usage:
   *
   * <ul>
   *   <li>{@code materialize-modern <packageDir>} — templateDef → {@code pages/}
   *   <li>{@code materialize-install <packageDir>} — modern {@code pages/} → root {@code
   *       *.templateDef}
   * </ul>
   */
  public static void main(String[] args) throws Exception {
    if (args.length < 2) {
      System.err.println(
          "Usage: PSPageXmlDualShip materialize-modern|materialize-install <packageDir>");
      System.exit(1);
    }
    String cmd = args[0].trim().toLowerCase(Locale.ROOT);
    Path packageDir = Path.of(args[1]).toAbsolutePath().normalize();
    int n =
        switch (cmd) {
          case "materialize-modern" -> materializeModernPageSources(packageDir);
          case "materialize-install" -> materializeInstallTemplateDefs(packageDir);
          default -> throw new IllegalArgumentException("Unknown command: " + args[0]);
        };
    System.out.println(cmd + " wrote " + n + " artifact(s) under " + packageDir);
  }

  /**
   * Whether the package root contains modern page authoring sources under {@value #PAGES_DIR_NAME}.
   */
  public static boolean hasModernPageSources(Path packageDir) throws IOException {
    Objects.requireNonNull(packageDir, "packageDir");
    Path pages = packageDir.resolve(PAGES_DIR_NAME);
    if (!Files.isDirectory(pages)) {
      return false;
    }
    return !listModernPageDirs(pages).isEmpty();
  }

  /**
   * Materialize install-path {@code *.templateDef} files at the package root from modern {@code
   * pages/&lt;id&gt;/} sources. No-op when no modern pages are present.
   *
   * @param packageDir product package source (or staging copy)
   * @return number of templateDefs written
   * @throws PSPageXmlException on compile/validation/emit failure
   * @throws IOException on I/O failure
   */
  public static int materializeInstallTemplateDefs(Path packageDir)
      throws PSPageXmlException, IOException {
    Objects.requireNonNull(packageDir, "packageDir");
    Path pages = packageDir.resolve(PAGES_DIR_NAME);
    if (!Files.isDirectory(pages)) {
      return 0;
    }
    List<Path> pageDirs = listModernPageDirs(pages);
    if (pageDirs.isEmpty()) {
      return 0;
    }

    Map<String, String> guidsByStem = loadGuidsFromMapping(packageDir);
    int written = 0;
    for (Path pageDir : pageDirs) {
      Path manifestPath = pageDir.resolve(PSComponentPackageManifest.DEFAULT_MANIFEST_FILE_NAME);
      PSComponentPackageManifest manifest;
      try {
        manifest = PSComponentPackageManifestIo.read(manifestPath);
        PSComponentPackageManifestValidator.validate(manifest);
      } catch (PSComponentPackageManifestException e) {
        throw new PSPageXmlException(
            "Invalid modern page package at " + pageDir + ": " + e.getMessage(), e);
      }

      String stem = manifest.getId();
      if (stem == null || stem.isBlank()) {
        stem = pageDir.getFileName().toString();
      }
      String templateSource = readTemplateSource(pageDir, manifest);
      String guid = guidsByStem.get(stem);
      if (guid == null || guid.isBlank()) {
        Path mapping = findMappingProperties(packageDir);
        String mappingHint =
            mapping != null ? mapping.toString() : "(no *.mapping.properties under package root)";
        throw new PSPageXmlException(
            "Missing stable install GUID for modern page stem '"
                + stem
                + "'. Expected key '"
                + stem
                + ".templateDef=TemplateDef-N' in mapping file: "
                + mappingHint);
      }
      String xml = PSPageXmlTemplateDefEmitter.emit(manifest, templateSource, guid);
      Path out = packageDir.resolve(stem + ".templateDef");
      Files.writeString(out, xml, StandardCharsets.UTF_8);
      written++;
    }
    return written;
  }

  /**
   * Bootstrap modern {@code pages/&lt;id&gt;/} sources from root-level {@code *.templateDef} files
   * (one-time migration). Overwrites existing modern trees for the same stem.
   *
   * @param packageDir product package source
   * @return number of modern page packages written
   * @throws PSPageXmlException on parse/compile failure
   * @throws IOException on I/O failure
   */
  public static int materializeModernPageSources(Path packageDir)
      throws PSPageXmlException, IOException {
    Objects.requireNonNull(packageDir, "packageDir");
    List<Path> templateDefs = PSPageXmlPackageCompiler.listTemplateDefs(packageDir);
    if (templateDefs.isEmpty()) {
      return 0;
    }
    PSPageXmlPackageContext ctx = PSPageXmlPackageContext.fromPackageDir(packageDir);
    Path pages = packageDir.resolve(PAGES_DIR_NAME);
    Files.createDirectories(pages);
    int written = 0;
    for (Path templateDef : templateDefs) {
      PSPageXmlCompileResult result = PSPageXmlCompiler.compile(templateDef, ctx);
      String stem = result.getManifest().getId();
      Path out = pages.resolve(stem);
      PSPageXmlCompiler.writeArtifacts(result, out);
      written++;
    }
    return written;
  }

  /**
   * Compile all modern page packages under {@code pages/} (authoring path).
   *
   * @param packageDir product package source
   * @return validated compile results sorted by template id
   */
  public static List<PSPageXmlCompileResult> compileModernPages(Path packageDir)
      throws PSPageXmlException, IOException {
    Objects.requireNonNull(packageDir, "packageDir");
    Path pages = packageDir.resolve(PAGES_DIR_NAME);
    if (!Files.isDirectory(pages)) {
      throw new PSPageXmlException("No modern pages directory: " + pages);
    }
    List<Path> pageDirs = listModernPageDirs(pages);
    if (pageDirs.isEmpty()) {
      throw new PSPageXmlException("No modern page packages under: " + pages);
    }

    List<PSPageXmlCompileResult> results = new ArrayList<>();
    for (Path pageDir : pageDirs) {
      results.add(loadModernAsCompileResult(pageDir));
    }
    results.sort(
        Comparator.comparing(
            r -> r.getManifest().getId() != null ? r.getManifest().getId() : "",
            String.CASE_INSENSITIVE_ORDER));
    return results;
  }

  /**
   * Load a modern page package directory into a {@link PSPageXmlCompileResult} (re-parse template
   * body for region holes so slots stay consistent with the Velocity source).
   */
  public static PSPageXmlCompileResult loadModernAsCompileResult(Path pageDir)
      throws PSPageXmlException, IOException {
    Objects.requireNonNull(pageDir, "pageDir");
    Path manifestPath = pageDir.resolve(PSComponentPackageManifest.DEFAULT_MANIFEST_FILE_NAME);
    if (!Files.isRegularFile(manifestPath)) {
      throw new PSPageXmlException("Missing component-package.json under " + pageDir);
    }
    PSComponentPackageManifest manifest;
    try {
      manifest = PSComponentPackageManifestIo.read(manifestPath);
      PSComponentPackageManifestValidator.validate(manifest);
    } catch (PSComponentPackageManifestException e) {
      throw new PSPageXmlException(
          "Invalid modern page package at " + pageDir + ": " + e.getMessage(), e);
    }

    // Primary template (templates[0]) drives the dual-ship model body / install templateDef.
    String templateSource = readTemplateSource(pageDir, manifest);
    // Build a synthetic model so dual-ship emit + tests share one shape.
    PSPageXmlModel model = new PSPageXmlModel();
    model.setName(manifest.getId());
    model.setLabel(manifest.getName());
    model.setDescription(manifest.getDescription());
    model.setSourceFileName(manifest.getId() + ".templateDef");
    if (manifest.getTemplates() != null && !manifest.getTemplates().isEmpty()) {
      PSComponentPackageManifest.TemplateRef t = manifest.getTemplates().get(0);
      model.setAssembler(PSPageXmlTemplateDefEmitter.toLegacyAssemblerPath(t.getAssembler()));
      model.setOutputFormat(PSPageXmlTemplateDefEmitter.toLegacyOutputFormat(t.getType()));
    }
    model.setTemplateBody(templateSource);
    model.setRegionHoles(PSPageXmlParser.extractRegionHoles(templateSource));
    model.setMimeType("text/html");
    model.setCharset("UTF-8");
    model.setTemplateType("Shared");
    model.setActiveAssemblyType("Normal");

    // Each template ref keeps its own source — do not reuse templates[0] for siblings.
    Map<String, String> artifacts = new LinkedHashMap<>();
    if (manifest.getTemplates() != null) {
      for (PSComponentPackageManifest.TemplateRef t : manifest.getTemplates()) {
        if (t != null && t.getSourceRef() != null && !t.getSourceRef().isBlank()) {
          artifacts.put(t.getSourceRef(), readTemplateSourceByRef(pageDir, t.getSourceRef()));
        }
      }
    }
    return new PSPageXmlCompileResult(model, manifest, artifacts);
  }

  /**
   * Resolve legacy assembly GUID from mapping properties ({@code name.templateDef=TemplateDef-N}
   * → {@code 0-4-N}).
   *
   * <p>Locates {@code *.mapping.properties} under the package dir (does not require the directory
   * name to match the package id — package build stages under {@code &lt;pkg&gt;-copy}).
   */
  public static Map<String, String> loadGuidsFromMapping(Path packageDir) throws IOException {
    Map<String, String> guids = new LinkedHashMap<>();
    Path propsFile = findMappingProperties(packageDir);
    if (propsFile == null) {
      return guids;
    }
    Properties props = new Properties();
    try (InputStream in = Files.newInputStream(propsFile)) {
      props.load(in);
    }
    for (String key : props.stringPropertyNames()) {
      // Only pure templateDef keys (not *.templateDef.aclDef)
      String lower = key.toLowerCase(Locale.ROOT);
      if (!lower.endsWith(".templatedef") || lower.endsWith(".templatedef.acldef")) {
        continue;
      }
      Matcher km = TEMPLATE_DEF_KEY.matcher(key);
      if (!km.matches()) {
        continue;
      }
      String stem = km.group(1);
      String value = props.getProperty(key);
      if (value == null) {
        continue;
      }
      Matcher vm = TEMPLATE_DEF_VALUE.matcher(value.trim());
      if (vm.matches()) {
        guids.put(stem, "0-4-" + vm.group(1));
      }
    }
    return guids;
  }

  /** Find the first {@code *.mapping.properties} file at the package root, if any. */
  static Path findMappingProperties(Path packageDir) throws IOException {
    Path name = packageDir.getFileName();
    if (name != null) {
      String base = name.toString();
      // staging dir may be "<pkg>-copy"
      if (base.endsWith("-copy")) {
        base = base.substring(0, base.length() - "-copy".length());
      }
      Path preferred = packageDir.resolve(base + ".mapping.properties");
      if (Files.isRegularFile(preferred)) {
        return preferred;
      }
      Path asIs = packageDir.resolve(name + ".mapping.properties");
      if (Files.isRegularFile(asIs)) {
        return asIs;
      }
    }
    try (DirectoryStream<Path> stream =
        Files.newDirectoryStream(packageDir, "*.mapping.properties")) {
      for (Path p : stream) {
        if (Files.isRegularFile(p)) {
          return p;
        }
      }
    }
    return null;
  }

  static List<Path> listModernPageDirs(Path pagesDir) throws IOException {
    List<Path> dirs = new ArrayList<>();
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(pagesDir, Files::isDirectory)) {
      for (Path p : stream) {
        if (Files.isRegularFile(
            p.resolve(PSComponentPackageManifest.DEFAULT_MANIFEST_FILE_NAME))) {
          dirs.add(p);
        }
      }
    }
    dirs.sort(Comparator.comparing(p -> p.getFileName().toString().toLowerCase(Locale.ROOT)));
    return dirs;
  }

  private static String readTemplateSource(Path pageDir, PSComponentPackageManifest manifest)
      throws PSPageXmlException, IOException {
    String sourceRef = null;
    if (manifest.getTemplates() != null && !manifest.getTemplates().isEmpty()) {
      sourceRef = manifest.getTemplates().get(0).getSourceRef();
    }
    if (sourceRef == null || sourceRef.isBlank()) {
      throw new PSPageXmlException(
          "Modern page package missing templates[0].sourceRef: " + pageDir);
    }
    return readTemplateSourceByRef(pageDir, sourceRef);
  }

  /**
   * Read a package-relative template source path under {@code pageDir}. Used so multi-template
   * modern packages load each {@code sourceRef} independently.
   */
  private static String readTemplateSourceByRef(Path pageDir, String sourceRef)
      throws PSPageXmlException, IOException {
    if (sourceRef == null || sourceRef.isBlank()) {
      throw new PSPageXmlException("Blank template sourceRef under " + pageDir);
    }
    Path templatePath = PSPageXmlCompiler.resolvePackageRelative(pageDir, sourceRef);
    if (!Files.isRegularFile(templatePath)) {
      throw new PSPageXmlException("Missing template source " + sourceRef + " under " + pageDir);
    }
    return Files.readString(templatePath, StandardCharsets.UTF_8);
  }
}
