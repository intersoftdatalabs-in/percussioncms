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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Reads normalized assembler-model definitions back through the modern Component Package Manifest
 * path (issue #5862 / parent #2626).
 *
 * <p>The legacy definition-XML readback ({@link PSPageXmlParser}) parses {@code *.templateDef}
 * ({@code <assembly-template>}) into {@link PSPageXmlModel}. The forward compiler ({@link
 * PSPageXmlCompiler}) turns that model into a manifest plus template source artifacts. This class
 * closes the loop: given a manifest (plus its template sources), it rebuilds the same normalized
 * {@link PSPageXmlModel} — same assembler extension path, output format, bindings order, region
 * holes, and install defaults — without touching legacy definition XML. The legacy fallback is
 * untouched; removal stays tracked separately.
 *
 * <p>Normalization contract (mirrors {@link PSPageXmlTemplateDefEmitter} install defaults so that
 * {@code parse(emit(manifest, source, guid))} and {@code readback(manifest, source, guid)} agree
 * field-for-field):
 *
 * <ul>
 *   <li>assembler: short manifest id resolved to the legacy extension path (registered catalog or
 *       {@code Java/global/percussion/assembly/} pass-through); blank resolves to the page-assembler
 *       default.
 *   <li>output format: modern template type mapped back ({@code page} → {@code Page}, …).
 *   <li>mime type / charset / publish-when / template-type: manifest values when present, else the
 *       install defaults ({@code text/html}, {@code UTF-8}, {@code Default}, {@code Shared}).
 *       Binary asset templates omit mime type and charset (see #3674).
 *   <li>active assembly type: {@code Normal} (the only value package build emits).
 *   <li>bindings: ordered, blank variables skipped, blank expressions normalized to null — the same
 *       shape the legacy parser produces from emitted XML.
 *   <li>region holes: re-derived from the template source with the same {@code #region} discovery
 *       the parser applies.
 * </ul>
 */
public final class PSPageXmlManifestReadback {

  private static final String DEFAULT_TEMPLATE_TYPE = "Shared";
  private static final String DEFAULT_ACTIVE_ASSEMBLY = "Normal";
  private static final String DEFAULT_CHARSET = "UTF-8";
  private static final String DEFAULT_MIME = "text/html";
  private static final String DEFAULT_PUBLISH_WHEN = "Default";

  private PSPageXmlManifestReadback() {
    // utility
  }

  /**
   * Read back the normalized assembler-model definition for a modern page package directory
   * (directory containing {@code component-package.json}).
   *
   * @param pageDir non-null page package directory
   * @return normalized model (never null)
   * @throws PSPageXmlException on invalid manifest, missing template source, or empty templates
   * @throws IOException on I/O failure
   */
  public static PSPageXmlModel readback(Path pageDir) throws PSPageXmlException, IOException {
    return readback(pageDir, null);
  }

  /**
   * Read back the normalized assembler-model definition for a modern page package directory,
   * attaching a legacy assembly GUID (e.g. {@code 0-4-591}) when known.
   *
   * @param pageDir non-null page package directory
   * @param guid optional assembly GUID; blank treated as absent
   * @return normalized model (never null)
   * @throws PSPageXmlException on invalid manifest, missing template source, or empty templates
   * @throws IOException on I/O failure
   */
  public static PSPageXmlModel readback(Path pageDir, String guid)
      throws PSPageXmlException, IOException {
    Objects.requireNonNull(pageDir, "pageDir");
    Path manifestPath = pageDir.resolve(PSComponentPackageManifest.DEFAULT_MANIFEST_FILE_NAME);
    PSComponentPackageManifest manifest = readManifest(manifestPath, pageDir);
    String templateSource = readPrimaryTemplateSource(pageDir, manifest);
    return readback(manifest, templateSource, guid);
  }

  /**
   * Read back the normalized assembler-model definition from an in-memory manifest plus its
   * primary template source text.
   *
   * @param manifest non-null validated manifest
   * @param templateSource template body text; may be null (treated as empty)
   * @return normalized model (never null)
   * @throws PSPageXmlException when the manifest id/templates cannot yield a definition
   */
  public static PSPageXmlModel readback(
      PSComponentPackageManifest manifest, String templateSource) throws PSPageXmlException {
    return readback(manifest, templateSource, null);
  }

  /**
   * Read back the normalized assembler-model definition from an in-memory manifest plus its
   * primary template source text, attaching a legacy assembly GUID when known.
   *
   * @param manifest non-null validated manifest
   * @param templateSource template body text; may be null (treated as empty)
   * @param guid optional assembly GUID; blank treated as absent
   * @return normalized model (never null)
   * @throws PSPageXmlException when the manifest id/templates cannot yield a definition
   */
  public static PSPageXmlModel readback(
      PSComponentPackageManifest manifest, String templateSource, String guid)
      throws PSPageXmlException {
    Objects.requireNonNull(manifest, "manifest");
    PSComponentPackageManifest.TemplateRef template = primaryTemplate(manifest);

    String stem = firstNonBlank(template.getName(), manifest.getId());
    if (stem == null) {
      throw new PSPageXmlException(
          "Manifest templates[0].name / id is required for assembler-model readback");
    }
    boolean binary = "binary".equalsIgnoreCase(template.getType());

    PSPageXmlModel model = new PSPageXmlModel();
    model.setSourceFileName(stem + ".templateDef");
    model.setName(stem);
    model.setLabel(
        firstNonBlank(
            manifest.getName(),
            manifest.getCatalog() != null ? manifest.getCatalog().getTitle() : null,
            stem));
    model.setDescription(
        firstNonBlank(
            manifest.getCatalog() != null ? manifest.getCatalog().getDescription() : null,
            manifest.getDescription()));
    model.setGuid(guid != null && !guid.isBlank() ? guid.trim() : null);
    model.setAssembler(PSPageXmlTemplateDefEmitter.toLegacyAssemblerPath(template.getAssembler()));
    model.setOutputFormat(PSPageXmlTemplateDefEmitter.toLegacyOutputFormat(template.getType()));
    model.setTemplateType(firstNonBlank(template.getLegacyTemplateType(), DEFAULT_TEMPLATE_TYPE));
    String mime = firstNonBlank(template.getMimeType(), binary ? null : DEFAULT_MIME);
    model.setMimeType(mime);
    model.setCharset(binary ? null : DEFAULT_CHARSET);
    model.setActiveAssemblyType(DEFAULT_ACTIVE_ASSEMBLY);
    model.setPublishWhen(firstNonBlank(template.getPublishWhen(), DEFAULT_PUBLISH_WHEN));
    model.setLocationPrefix(blankToNull(template.getLocationPrefix()));
    model.setLocationSuffix(blankToNull(template.getLocationSuffix()));
    String body = PSPageXmlParser.normalizeBody(templateSource != null ? templateSource : "");
    model.setTemplateBody(body);
    model.setBindings(buildBindings(template.getBindings()));
    model.setRegionHoles(PSPageXmlParser.extractRegionHoles(body));
    return model;
  }

  /**
   * Read back definitions from an artifact map as produced by {@link
   * PSPageXmlCompileResult#getTextArtifacts()} (package-relative {@code sourceRef} → source text).
   *
   * @param manifest non-null validated manifest
   * @param textArtifacts non-null source map
   * @return normalized model (never null)
   * @throws PSPageXmlException when templates are empty or the primary source is missing
   */
  public static PSPageXmlModel readback(
      PSComponentPackageManifest manifest, Map<String, String> textArtifacts)
      throws PSPageXmlException {
    Objects.requireNonNull(manifest, "manifest");
    Objects.requireNonNull(textArtifacts, "textArtifacts");
    PSComponentPackageManifest.TemplateRef template = primaryTemplate(manifest);
    String sourceRef = template.getSourceRef();
    if (sourceRef == null || sourceRef.isBlank() || !textArtifacts.containsKey(sourceRef)) {
      throw new PSPageXmlException(
          "Missing template source artifact for readback: "
              + (sourceRef == null ? "(no sourceRef)" : sourceRef));
    }
    return readback(manifest, textArtifacts.get(sourceRef), null);
  }

  private static PSComponentPackageManifest.TemplateRef primaryTemplate(
      PSComponentPackageManifest manifest) throws PSPageXmlException {
    if (manifest.getTemplates() == null || manifest.getTemplates().isEmpty()) {
      throw new PSPageXmlException(
          "Manifest templates must not be empty for assembler-model readback: "
              + manifest.getId());
    }
    PSComponentPackageManifest.TemplateRef template = manifest.getTemplates().get(0);
    if (template == null) {
      throw new PSPageXmlException("Manifest templates[0] must not be null");
    }
    return template;
  }

  private static List<PSPageXmlModel.Binding> buildBindings(
      List<PSComponentPackageManifest.Binding> bindings) {
    List<PSPageXmlModel.Binding> out = new ArrayList<>();
    if (bindings == null) {
      return out;
    }
    for (PSComponentPackageManifest.Binding b : bindings) {
      if (b == null || b.getVariable() == null || b.getVariable().isBlank()) {
        continue;
      }
      PSPageXmlModel.Binding binding = new PSPageXmlModel.Binding();
      binding.setVariable(b.getVariable().trim());
      binding.setExpression(blankToNull(b.getExpression()));
      out.add(binding);
    }
    return out;
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

  private static String readPrimaryTemplateSource(
      Path pageDir, PSComponentPackageManifest manifest) throws PSPageXmlException, IOException {
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

  private static String firstNonBlank(String... values) {
    if (values == null) {
      return null;
    }
    for (String v : values) {
      if (v != null && !v.isBlank()) {
        // Trim: the legacy parser trims element text, so parse(emit(x)) is trimmed too.
        return v.trim();
      }
    }
    return null;
  }

  private static String blankToNull(String value) {
    return value != null && !value.isBlank() ? value.trim() : null;
  }
}
