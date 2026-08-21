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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Compiles legacy Page / assembly {@code *.templateDef} XML into a modern {@link
 * PSComponentPackageManifest} plus template source artifacts (Phase 3 / ADR-004 / issue #2770).
 *
 * <p><strong>Scope:</strong> product page layout templates (e.g. {@code perc.baseTemplates}, {@code
 * perc.responsiveTemplates}) and Baseline system assembly templates ({@code perc.page}, dispatchers,
 * resource, widget). Region holes become slots; assembler extension short-names map to the Component
 * Package Manifest assembler field; Velocity body is the canonical template source. Dual-ship also
 * preserves mime type, publish-when, and location suffix when present.
 *
 * <p>Product page layout packages author modern {@code pages/&lt;id&gt;/} sources; install dual-ship
 * emits {@code *.templateDef} at package-build time ({@link PSPageXmlDualShip}, issue #2786). This
 * compiler remains the upgrade path for legacy {@code *.templateDef} input.
 */
public final class PSPageXmlCompiler {

  private static final String DEFAULT_VERSION = "0.0.0";

  private PSPageXmlCompiler() {
    // utility
  }

  /**
   * Compile a {@code *.templateDef} file into a modern component package result.
   *
   * @param templateDefPath path to assembly-template XML
   * @param packageContext optional package metadata; may be null
   * @return compile result with validated manifest and text artifacts
   * @throws PSPageXmlException on parse/compile/validation failure
   * @throws IOException on I/O failure
   */
  public static PSPageXmlCompileResult compile(
      Path templateDefPath, PSPageXmlPackageContext packageContext)
      throws PSPageXmlException, IOException {
    Objects.requireNonNull(templateDefPath, "templateDefPath");
    PSPageXmlModel model = PSPageXmlParser.parse(templateDefPath);
    return compile(model, packageContext);
  }

  /**
   * Compile a previously parsed page template model.
   *
   * @param model non-null parsed template
   * @param packageContext optional package metadata; may be null
   * @return compile result
   * @throws PSPageXmlException on compile/validation failure
   */
  public static PSPageXmlCompileResult compile(
      PSPageXmlModel model, PSPageXmlPackageContext packageContext) throws PSPageXmlException {
    Objects.requireNonNull(model, "model");

    String stem = model.pageStem();
    if (stem == null || stem.isBlank()) {
      throw new PSPageXmlException(
          "Page template model is missing name / source file; cannot derive component id");
    }

    PSComponentPackageManifest manifest = new PSComponentPackageManifest();
    manifest.setSchemaVersion(PSComponentPackageManifest.SUPPORTED_SCHEMA_VERSION);
    manifest.setId(stem);
    String displayName =
        model.getLabel() != null && !model.getLabel().isBlank() ? model.getLabel() : stem;
    manifest.setName(displayName);
    applyPackageIdentity(manifest, packageContext, model);

    PSComponentPackageManifest.Catalog catalog = new PSComponentPackageManifest.Catalog();
    catalog.setKind("page");
    catalog.setTitle(displayName);
    catalog.setDescription(model.getDescription());
    catalog.setCategory("page");
    catalog.setPaletteVisible(Boolean.TRUE);
    if (packageContext != null
        && packageContext.getPackageId() != null
        && packageContext.getPackageId().toLowerCase(Locale.ROOT).contains("responsive")) {
      catalog.setResponsive(Boolean.TRUE);
    }
    manifest.setCatalog(catalog);

    String assembler = mapAssembler(model.getAssembler());
    String templateType = mapTemplateType(model.getOutputFormat(), assembler);
    String sourceRef = "templates/" + stem + assemblerExtension(assembler);

    PSComponentPackageManifest.TemplateRef template = new PSComponentPackageManifest.TemplateRef();
    template.setName(stem);
    template.setType(templateType);
    template.setAssembler(assembler);
    template.setSourceRef(sourceRef);
    // Dual-ship install fields — only persist when non-default so page-layout packages stay lean
    // (emitter defaults: text/html, Default, empty location prefix/suffix).
    if (model.getMimeType() != null
        && !model.getMimeType().isBlank()
        && !"text/html".equalsIgnoreCase(model.getMimeType().trim())) {
      template.setMimeType(model.getMimeType().trim());
    }
    if (model.getPublishWhen() != null
        && !model.getPublishWhen().isBlank()
        && !"Default".equalsIgnoreCase(model.getPublishWhen().trim())) {
      template.setPublishWhen(model.getPublishWhen().trim());
    }
    if (model.getLocationSuffix() != null && !model.getLocationSuffix().isBlank()) {
      template.setLocationSuffix(model.getLocationSuffix());
    }
    if (model.getLocationPrefix() != null && !model.getLocationPrefix().isBlank()) {
      template.setLocationPrefix(model.getLocationPrefix());
    }
    if (model.getTemplateType() != null
        && !model.getTemplateType().isBlank()
        && !"Shared".equalsIgnoreCase(model.getTemplateType().trim())) {
      template.setLegacyTemplateType(model.getTemplateType().trim());
    }
    template.setBindings(buildBindings(model));
    List<PSComponentPackageManifest.TemplateRef> templates = new ArrayList<>();
    templates.add(template);
    manifest.setTemplates(templates);

    List<PSComponentPackageManifest.SlotRef> slots = new ArrayList<>();
    for (PSPageXmlModel.RegionHole hole : model.getRegionHoles()) {
      if (hole == null || hole.getRegionId() == null || hole.getRegionId().isBlank()) {
        continue;
      }
      PSComponentPackageManifest.SlotRef slot = new PSComponentPackageManifest.SlotRef();
      slot.setName(hole.getRegionId());
      slot.setAllowedContentTypes(new ArrayList<>());
      slot.setLayout(
          hole.getLayoutHints() != null
              ? new LinkedHashMap<>(hole.getLayoutHints())
              : new LinkedHashMap<>());
      slot.setStyles(
          hole.getStyleHints() != null
              ? new LinkedHashMap<>(hole.getStyleHints())
              : new LinkedHashMap<>());
      slots.add(slot);
    }
    manifest.setSlots(slots);

    // Page layout templates do not declare CT in templateDef; leave contentTypes empty.
    manifest.setContentTypes(new ArrayList<>());
    manifest.setResources(new ArrayList<>());
    manifest.setUserPreferences(new ArrayList<>());
    manifest.setCssPreferences(new ArrayList<>());

    try {
      PSComponentPackageManifestValidator.validate(manifest);
    } catch (PSComponentPackageManifestException e) {
      throw new PSPageXmlException("Compiled manifest failed validation: " + e.getMessage(), e);
    }

    Map<String, String> artifacts = new LinkedHashMap<>();
    String templateSource = model.getTemplateBody() != null ? model.getTemplateBody() : "";
    artifacts.put(sourceRef, templateSource);

    return new PSPageXmlCompileResult(model, manifest, artifacts);
  }

  /**
   * Write a compile result under {@code outputDir}: {@code component-package.json} plus text
   * artifacts using package-relative paths. Parent directories are created as needed.
   *
   * @param result non-null compile result
   * @param outputDir non-null destination package root
   * @throws IOException on I/O failure
   */
  public static void writeArtifacts(PSPageXmlCompileResult result, Path outputDir)
      throws IOException {
    Objects.requireNonNull(result, "result");
    Objects.requireNonNull(outputDir, "outputDir");
    Files.createDirectories(outputDir);
    Path manifestPath = outputDir.resolve(PSComponentPackageManifest.DEFAULT_MANIFEST_FILE_NAME);
    PSComponentPackageManifestIo.write(result.getManifest(), manifestPath);
    for (Map.Entry<String, String> entry : result.getTextArtifacts().entrySet()) {
      Path artifact = resolvePackageRelative(outputDir, entry.getKey());
      Path parent = artifact.getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }
      Files.writeString(artifact, entry.getValue(), StandardCharsets.UTF_8);
    }
  }

  private static void applyPackageIdentity(
      PSComponentPackageManifest manifest,
      PSPageXmlPackageContext ctx,
      PSPageXmlModel model) {
    if (ctx != null) {
      if (ctx.getVersion() != null && !ctx.getVersion().isBlank()) {
        manifest.setVersion(ctx.getVersion());
      }
      if (model.getDescription() != null && !model.getDescription().isBlank()) {
        manifest.setDescription(model.getDescription());
      } else if (ctx.getDescription() != null && !ctx.getDescription().isBlank()) {
        manifest.setDescription(ctx.getDescription());
      }
      if (ctx.getPublisherName() != null || ctx.getPublisherUrl() != null) {
        PSComponentPackageManifest.Publisher pub = new PSComponentPackageManifest.Publisher();
        pub.setName(ctx.getPublisherName());
        pub.setUrl(ctx.getPublisherUrl());
        manifest.setPublisher(pub);
      }
      if (ctx.getCmsMin() != null || ctx.getCmsMax() != null) {
        PSComponentPackageManifest.CmsVersionRange range =
            new PSComponentPackageManifest.CmsVersionRange();
        range.setMin(ctx.getCmsMin());
        range.setMax(ctx.getCmsMax());
        manifest.setCmsVersion(range);
      }
      if (ctx.getDependencies() != null && !ctx.getDependencies().isEmpty()) {
        List<PSComponentPackageManifest.Dependency> deps = new ArrayList<>();
        for (PSPageXmlPackageContext.Dependency d : ctx.getDependencies()) {
          if (d == null || d.getName() == null || d.getName().isBlank()) {
            continue;
          }
          PSComponentPackageManifest.Dependency dep = new PSComponentPackageManifest.Dependency();
          dep.setName(d.getName());
          dep.setVersion(d.getVersion());
          dep.setImplied(d.isImplied());
          deps.add(dep);
        }
        manifest.setDependencies(deps);
      }
    } else if (model.getDescription() != null) {
      manifest.setDescription(model.getDescription());
    }

    if (manifest.getVersion() == null || manifest.getVersion().isBlank()) {
      manifest.setVersion(DEFAULT_VERSION);
    }
  }

  static List<PSComponentPackageManifest.Binding> buildBindings(PSPageXmlModel model) {
    List<PSComponentPackageManifest.Binding> bindings = new ArrayList<>();
    if (model.getBindings() == null) {
      return bindings;
    }
    for (PSPageXmlModel.Binding b : model.getBindings()) {
      if (b == null || b.getVariable() == null || b.getVariable().isBlank()) {
        continue;
      }
      PSComponentPackageManifest.Binding out = new PSComponentPackageManifest.Binding();
      out.setVariable(b.getVariable());
      out.setExpression(b.getExpression() != null ? b.getExpression() : "");
      bindings.add(out);
    }
    return bindings;
  }

  /**
   * Map assembly-template {@code <assembler>} (often a Java extension path) to a short Component
   * Package Manifest assembler id.
   */
  static String mapAssembler(String assembler) {
    if (assembler == null || assembler.isBlank()) {
      return "pageAssembler";
    }
    String a = assembler.trim();
    String lower = a.toLowerCase(Locale.ROOT);
    // Extension path: Java/global/percussion/assembly/pageAssembler
    int slash = lower.lastIndexOf('/');
    String leaf = slash >= 0 ? a.substring(slash + 1) : a;
    String leafLower = leaf.toLowerCase(Locale.ROOT);
    if (leafLower.endsWith("assembler")) {
      // normalize casing for known product assemblers
      if (leafLower.equals("pageassembler")) {
        return "pageAssembler";
      }
      if (leafLower.equals("pagevariantassembler")) {
        return "pageVariantAssembler";
      }
      if (leafLower.equals("velocityassembler")) {
        return "velocityAssembler";
      }
      if (leafLower.equals("htmlassembler")) {
        return "htmlAssembler";
      }
      if (leafLower.equals("markdownassembler")) {
        return "markdownAssembler";
      }
      if (leafLower.equals("legacyassembler") || leafLower.equals("xslassembler")) {
        return "legacyAssembler";
      }
      if (leafLower.equals("dispatchassembler")) {
        return "dispatchAssembler";
      }
      if (leafLower.equals("resourceassembler")) {
        return "resourceAssembler";
      }
      if (leafLower.equals("pagedatabaseassembler")) {
        return "pageDatabaseAssembler";
      }
      if (leafLower.equals("binaryassembler")) {
        return "binaryAssembler";
      }
      return leaf;
    }
    return leaf + "Assembler";
  }

  /**
   * Map {@code <output-format>} to Component Package Manifest template type (page / snippet /
   * global / binary / resource).
   */
  static String mapTemplateType(String outputFormat, String assembler) {
    if (outputFormat != null && !outputFormat.isBlank()) {
      String o = outputFormat.trim().toLowerCase(Locale.ROOT);
      return switch (o) {
        case "page" -> "page";
        case "snippet" -> "snippet";
        case "global" -> "global";
        case "binary" -> "binary";
        case "database", "db" -> "binary";
        case "resource" -> "resource";
        default -> o;
      };
    }
    if (assembler != null && assembler.toLowerCase(Locale.ROOT).contains("page")) {
      return "page";
    }
    return "page";
  }

  static String assemblerExtension(String assembler) {
    if (assembler == null) {
      return ".vm";
    }
    String a = assembler.toLowerCase(Locale.ROOT);
    if (a.contains("html") && !a.contains("page")) {
      return ".html";
    }
    if (a.contains("markdown") || a.endsWith("mdassembler")) {
      return ".md";
    }
    // pageAssembler is Velocity-based (PSPageAssembler extends PSVelocityAssembler)
    return ".vm";
  }

  /**
   * Resolve a package-relative path (URL-style {@code /}) under {@code base} using portable {@link
   * Path#resolve(String)} per segment — never string-concatenate OS separators.
   */
  static Path resolvePackageRelative(Path base, String packageRelative) {
    Path p = base;
    for (String segment : packageRelative.split("/")) {
      if (segment.isEmpty() || ".".equals(segment)) {
        continue;
      }
      if ("..".equals(segment)) {
        throw new IllegalArgumentException(
            "Package-relative path must not contain '..': " + packageRelative);
      }
      p = p.resolve(segment);
    }
    return p;
  }
}
