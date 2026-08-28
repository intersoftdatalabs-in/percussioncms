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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Native package-install path for modern page component packages (issue #2806 / parent #2630).
 *
 * <p>Converts {@code pages/&lt;id&gt;/component-package.json} + template sources into deployer {@code
 * TemplateDef} archive entries ({@code TemplateDef-N/&lt;stem&gt;.templateDef}) without dual-ship
 * root-level {@code *.templateDef} materialization.
 *
 * <p>Aligns with {@link com.percussion.packages.shim.PSLegacyDefinitionXmlShim}: modern packages are
 * preferred; dual-ship is an explicit opt-in (package-local or system property) via {@link
 * PSPageXmlInstallPolicy} (native is the default as of #3949). Package-build uses this native path
 * only (#3950); dual-ship mode fails closed.
 *
 * <p>Deployer runtime still consumes legacy assembly-template XML inside the {@code .ppkg}
 * (PSTemplateDefDependencyHandler). This class is the package-build / staging bridge that makes that
 * install path work from modern authoring alone. First-assign install keeps unused archive UUIDs
 * ({@code TemplateDef-602} → {@code 0-4-602}); existing customer rows are not remapped (issue
 * #3727).
 */
public final class PSPageXmlNativeInstall {

  private static final Pattern TEMPLATE_DEF_KEY =
      Pattern.compile("^(.+)\\.templateDef$", Pattern.CASE_INSENSITIVE);
  private static final Pattern TEMPLATE_DEF_VALUE =
      Pattern.compile("^TemplateDef-(\\d+)$", Pattern.CASE_INSENSITIVE);

  private PSPageXmlNativeInstall() {
    // utility
  }

  /**
   * Build install artifacts for every modern page under {@code pages/}.
   *
   * @param packageDir product package source or staging copy
   * @return ordered list (by stem); empty when no modern pages
   */
  public static List<PSPageXmlInstallArtifact> listInstallArtifacts(Path packageDir)
      throws PSPageXmlException, IOException {
    Objects.requireNonNull(packageDir, "packageDir");
    if (!PSPageXmlDualShip.hasModernPageSources(packageDir)) {
      return List.of();
    }

    Map<String, String> guidsByStem = PSPageXmlDualShip.loadGuidsFromMapping(packageDir);
    Map<String, String> foldersByStem = loadArchiveFoldersFromMapping(packageDir);
    Path pages = packageDir.resolve(PSPageXmlDualShip.PAGES_DIR_NAME);
    List<Path> pageDirs = PSPageXmlDualShip.listModernPageDirs(pages);

    List<PSPageXmlInstallArtifact> artifacts = new ArrayList<>(pageDirs.size());
    for (Path pageDir : pageDirs) {
      artifacts.add(toInstallArtifact(pageDir, guidsByStem, foldersByStem, packageDir));
    }
    return artifacts;
  }

  /**
   * Stage deployer archive folders under {@code archiveRoot}: {@code
   * TemplateDef-N/&lt;stem&gt;.templateDef}.
   *
   * @param packageDir modern sources + mapping (staging copy is fine)
   * @param archiveRoot package-build temp2 / archive layout root
   * @return number of templateDefs written
   */
  public static int stageArchiveTemplateDefs(Path packageDir, Path archiveRoot)
      throws PSPageXmlException, IOException {
    Objects.requireNonNull(packageDir, "packageDir");
    Objects.requireNonNull(archiveRoot, "archiveRoot");
    List<PSPageXmlInstallArtifact> artifacts = listInstallArtifacts(packageDir);
    int written = 0;
    for (PSPageXmlInstallArtifact a : artifacts) {
      Path outDir = archiveRoot.resolve(a.archiveFolder());
      Files.createDirectories(outDir);
      Path out = outDir.resolve(a.rootFileName());
      Files.writeString(out, a.templateDefXml(), StandardCharsets.UTF_8);
      written++;
    }
    return written;
  }

  /**
   * Convenience: emit root-level dual-ship style files using the native conversion pipeline
   * (semantic parity with {@link PSPageXmlDualShip#materializeInstallTemplateDefs}). Prefer {@link
   * #stageArchiveTemplateDefs} when dual-ship is off.
   */
  public static int materializeRootTemplateDefs(Path packageDir)
      throws PSPageXmlException, IOException {
    Objects.requireNonNull(packageDir, "packageDir");
    List<PSPageXmlInstallArtifact> artifacts = listInstallArtifacts(packageDir);
    int written = 0;
    for (PSPageXmlInstallArtifact a : artifacts) {
      Path out = packageDir.resolve(a.rootFileName());
      Files.writeString(out, a.templateDefXml(), StandardCharsets.UTF_8);
      written++;
    }
    return written;
  }

  /**
   * Convert a single modern page directory to install XML for a known GUID.
   *
   * @param pageDir directory containing {@code component-package.json}
   * @param guid assembly GUID (e.g. {@code 0-4-591}); required non-blank
   * @return install artifact (archive folder inferred as {@code TemplateDef-&lt;uuid&gt;} from guid
   *     when form {@code 0-4-N})
   */
  public static PSPageXmlInstallArtifact fromModernPageDir(Path pageDir, String guid)
      throws PSPageXmlException, IOException {
    Objects.requireNonNull(pageDir, "pageDir");
    if (guid == null || guid.isBlank()) {
      throw new PSPageXmlException("GUID is required for native page install: " + pageDir);
    }
    String archiveFolder = archiveFolderFromGuid(guid.trim());
    Map<String, String> guids = Map.of();
    Map<String, String> folders = Map.of();
    // Load with explicit guid override path
    Path manifestPath = pageDir.resolve(PSComponentPackageManifest.DEFAULT_MANIFEST_FILE_NAME);
    PSComponentPackageManifest manifest = readManifest(manifestPath, pageDir);
    String stem = resolveStem(manifest, pageDir);
    String templateSource = readPrimaryTemplateSource(pageDir, manifest);
    String xml = PSPageXmlTemplateDefEmitter.emit(manifest, templateSource, guid.trim());
    if (archiveFolder == null) {
      archiveFolder = "TemplateDef-" + stem;
    }
    // Prefer caller guid; folder from 0-4-N when possible
    return new PSPageXmlInstallArtifact(stem, guid.trim(), archiveFolder, xml);
  }

  private static PSPageXmlInstallArtifact toInstallArtifact(
      Path pageDir,
      Map<String, String> guidsByStem,
      Map<String, String> foldersByStem,
      Path packageDir)
      throws PSPageXmlException, IOException {
    Path manifestPath = pageDir.resolve(PSComponentPackageManifest.DEFAULT_MANIFEST_FILE_NAME);
    PSComponentPackageManifest manifest = readManifest(manifestPath, pageDir);
    String stem = resolveStem(manifest, pageDir);
    // Mapping loaders key stems lower-case (Windows/macOS case-insensitive product history;
    // dual-ship uses the same policy). Preserve original stem for on-disk templateDef names.
    String stemKey = stem != null ? stem.toLowerCase(Locale.ROOT) : "";
    String guid = guidsByStem.get(stemKey);
    String archiveFolder = foldersByStem.get(stemKey);
    if (guid == null || guid.isBlank() || archiveFolder == null || archiveFolder.isBlank()) {
      Path mapping = PSPageXmlDualShip.findMappingProperties(packageDir);
      String mappingHint =
          mapping != null ? mapping.toString() : "(no *.mapping.properties under package root)";
      throw new PSPageXmlException(
          "Missing stable install mapping for modern page stem '"
              + stem
              + "'. Expected key '"
              + stem
              + ".templateDef=TemplateDef-N' in mapping file: "
              + mappingHint);
    }
    String templateSource = readPrimaryTemplateSource(pageDir, manifest);
    String xml = PSPageXmlTemplateDefEmitter.emit(manifest, templateSource, guid);
    return new PSPageXmlInstallArtifact(stem, guid, archiveFolder, xml);
  }

  /**
   * Mapping value {@code TemplateDef-N} by stem (for archive folder names). Same keys as GUID
   * loader — stem keys are lower-cased for case-insensitive match against manifest ids.
   */
  public static Map<String, String> loadArchiveFoldersFromMapping(Path packageDir)
      throws IOException {
    Map<String, String> folders = new LinkedHashMap<>();
    Path propsFile = PSPageXmlDualShip.findMappingProperties(packageDir);
    if (propsFile == null) {
      return folders;
    }
    Properties props = new Properties();
    try (InputStream in = Files.newInputStream(propsFile)) {
      props.load(in);
    }
    for (String key : props.stringPropertyNames()) {
      String lower = key.toLowerCase(Locale.ROOT);
      if (!lower.endsWith(".templatedef") || lower.endsWith(".templatedef.acldef")) {
        continue;
      }
      Matcher km = TEMPLATE_DEF_KEY.matcher(key);
      if (!km.matches()) {
        continue;
      }
      // Match loadGuidsFromMapping: lowercase stem keys for mixed-case product ids.
      String stem = km.group(1).toLowerCase(Locale.ROOT);
      String value = props.getProperty(key);
      if (value == null) {
        continue;
      }
      String trimmed = value.trim();
      Matcher vm = TEMPLATE_DEF_VALUE.matcher(trimmed);
      if (vm.matches()) {
        folders.put(stem, trimmed);
      }
    }
    return folders;
  }

  static String archiveFolderFromGuid(String guid) {
    // product GUIDs are 0-4-N for TemplateDef-N
    if (guid == null) {
      return null;
    }
    String g = guid.trim();
    if (g.startsWith("0-4-")) {
      return "TemplateDef-" + g.substring("0-4-".length());
    }
    return null;
  }

  private static PSComponentPackageManifest readManifest(Path manifestPath, Path pageDir)
      throws PSPageXmlException, IOException {
    try {
      PSComponentPackageManifest manifest = PSComponentPackageManifestIo.read(manifestPath);
      PSComponentPackageManifestValidator.validate(manifest);
      return manifest;
    } catch (PSComponentPackageManifestException e) {
      throw new PSPageXmlException(
          "Invalid modern page package at " + pageDir + ": " + e.getMessage(), e);
    }
  }

  private static String resolveStem(PSComponentPackageManifest manifest, Path pageDir) {
    if (manifest.getId() != null && !manifest.getId().isBlank()) {
      return manifest.getId();
    }
    return pageDir.getFileName().toString();
  }

  private static String readPrimaryTemplateSource(Path pageDir, PSComponentPackageManifest manifest)
      throws PSPageXmlException, IOException {
    String sourceRef = null;
    if (manifest.getTemplates() != null && !manifest.getTemplates().isEmpty()) {
      sourceRef = manifest.getTemplates().get(0).getSourceRef();
    }
    if (sourceRef == null || sourceRef.isBlank()) {
      throw new PSPageXmlException(
          "Modern page package missing templates[0].sourceRef: " + pageDir);
    }
    Path templatePath = PSPageXmlCompiler.resolvePackageRelative(pageDir, sourceRef);
    if (!Files.isRegularFile(templatePath)) {
      throw new PSPageXmlException("Missing template source " + sourceRef + " under " + pageDir);
    }
    return Files.readString(templatePath, StandardCharsets.UTF_8);
  }
}
