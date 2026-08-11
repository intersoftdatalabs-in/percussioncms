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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Compiles legacy Widget definition XML into a modern {@link PSComponentPackageManifest} plus
 * template source artifacts (Phase 3 / ADR-004 / issues #2751, #2772, #2789, #2802, #2830).
 *
 * <p><strong>Scope:</strong> baseWidgets-shaped widgets, high-traffic product packages (title,
 * lists, nav chrome, file, image — #2772), residual long-tail product packages (blog, calendar,
 * directory, social, forms, poll, login, rss, iframe — #2789), remaining product packages
 * (auto-lists, blog/list companions, comments/liked/cards, event/slider/cookie/jquery, login
 * variants, Result/Redirect, defaultLanguage — #2802), and the final {@code perc.Test} residual
 * ({@code PSWidget_TestProperties} — #2830): JEXL code + Velocity content + optional asset content
 * type + UserPref/CssPref + {@code <Resource>} CSS/JS refs. Product Widget XML remains dual-run
 * until Phase 5 exit; inventory:
 * {@code docs/ai-generated/tasks/template-assembler-normalization/widget-xml-inventory.md}.
 *
 * <p>Assembler mapping: {@code Content type="velocity"} → {@code velocityAssembler}; {@code html}
 * → {@code htmlAssembler}; {@code markdown} → {@code markdownAssembler}. Code language is always
 * preserved as JEXL bindings (ADR-001).
 */
public final class PSWidgetXmlCompiler {

  /**
   * Binding variable used when the full JEXL {@code <Code>} body is stored as a single script
   * binding (control-flow safe). Simple top-level {@code $var = expr} assignments are also emitted
   * as individual bindings for ergonomics.
   */
  public static final String FULL_CODE_BINDING_VARIABLE = "__widgetCode";

  private static final Pattern SIMPLE_ASSIGNMENT =
      Pattern.compile(
          "(?m)^\\s*\\$([A-Za-z_][\\w.]*)\\s*=\\s*(.+?)\\s*;\\s*$");

  private static final String DEFAULT_VERSION = "0.0.0";

  private PSWidgetXmlCompiler() {
    // utility
  }

  /**
   * Compile a widget XML file into a modern component package result.
   *
   * @param widgetXmlPath path to Widget definition XML
   * @param packageContext optional package metadata (version, publisher, deps); may be null
   * @return compile result with validated manifest and text artifacts
   * @throws PSWidgetXmlException on parse/compile/validation failure
   * @throws IOException on I/O failure
   */
  public static PSWidgetXmlCompileResult compile(Path widgetXmlPath, PSWidgetXmlPackageContext packageContext)
      throws PSWidgetXmlException, IOException {
    Objects.requireNonNull(widgetXmlPath, "widgetXmlPath");
    PSWidgetXmlModel model = PSWidgetXmlParser.parse(widgetXmlPath);
    return compile(model, packageContext);
  }

  /**
   * Compile a previously parsed widget model.
   *
   * @param model non-null parsed widget
   * @param packageContext optional package metadata; may be null
   * @return compile result
   * @throws PSWidgetXmlException on compile/validation failure
   */
  public static PSWidgetXmlCompileResult compile(
      PSWidgetXmlModel model, PSWidgetXmlPackageContext packageContext) throws PSWidgetXmlException {
    Objects.requireNonNull(model, "model");

    String stem = model.widgetStem();
    if (stem == null || stem.isBlank()) {
      throw new PSWidgetXmlException(
          "Widget model is missing source file name; cannot derive widget stem / component id");
    }

    PSComponentPackageManifest manifest = new PSComponentPackageManifest();
    manifest.setSchemaVersion(PSComponentPackageManifest.SUPPORTED_SCHEMA_VERSION);
    manifest.setId(stem);
    manifest.setName(
        model.getTitle() != null && !model.getTitle().isBlank() ? model.getTitle() : stem);
    applyPackageIdentity(manifest, packageContext, model);

    PSComponentPackageManifest.Catalog catalog = new PSComponentPackageManifest.Catalog();
    catalog.setKind("component");
    catalog.setTitle(model.getTitle());
    catalog.setCategory(model.getCategory());
    catalog.setDescription(model.getDescription());
    catalog.setAuthor(model.getAuthor());
    catalog.setPreferredEditorWidth(model.getPreferredEditorWidth());
    catalog.setPreferredEditorHeight(model.getPreferredEditorHeight());
    catalog.setCreateSharedAsset(model.getCreateSharedAsset());
    catalog.setEditableOnTemplate(model.getEditableOnTemplate());
    catalog.setResponsive(model.getResponsive());
    catalog.setPaletteVisible(Boolean.TRUE);

    String packageRelativeThumbnail = toPackageRelativePath(model.getThumbnail());
    if (packageRelativeThumbnail != null) {
      catalog.setThumbnail(packageRelativeThumbnail);
      catalog.setIcon(packageRelativeThumbnail);
    }
    manifest.setCatalog(catalog);

    // Content type (optional for logic/chrome widgets).
    String ctName = model.getContentTypeName();
    if (ctName != null && !ctName.isBlank()) {
      PSComponentPackageManifest.ContentTypeRef ct = new PSComponentPackageManifest.ContentTypeRef();
      ct.setName(ctName);
      ct.setRef("contentTypes/" + ctName);
      List<PSComponentPackageManifest.ContentTypeRef> cts = new ArrayList<>();
      cts.add(ct);
      manifest.setContentTypes(cts);
    }

    // Template + JEXL bindings + Velocity (or other) source.
    PSComponentPackageManifest.TemplateRef template = new PSComponentPackageManifest.TemplateRef();
    String templateName = stem + "Snippet";
    template.setName(templateName);
    template.setType("snippet");
    template.setAssembler(mapAssembler(model.getContentType()));
    String sourceRef = "templates/" + templateName + assemblerExtension(template.getAssembler());
    template.setSourceRef(sourceRef);
    if (ctName != null && !ctName.isBlank()) {
      template.setContentType(ctName);
    }
    template.setBindings(buildBindings(model.getCodeBody()));
    List<PSComponentPackageManifest.TemplateRef> templates = new ArrayList<>();
    templates.add(template);
    manifest.setTemplates(templates);

    // Default slot: asset content type when present; chrome/logic widgets (nav) still get a styles
    // slot when CssPref is declared so ADR-003 layout/styles has a home without inventing a CT.
    Map<String, Object> styles = cssPrefStyles(model);
    Map<String, Object> layout = layoutFromUserPrefs(model);
    if (ctName != null && !ctName.isBlank()) {
      PSComponentPackageManifest.SlotRef slot = new PSComponentPackageManifest.SlotRef();
      slot.setName(stem + "Content");
      List<String> allowed = new ArrayList<>();
      allowed.add(ctName);
      slot.setAllowedContentTypes(allowed);
      slot.setStyles(styles);
      slot.setLayout(layout);
      List<PSComponentPackageManifest.SlotRef> slots = new ArrayList<>();
      slots.add(slot);
      manifest.setSlots(slots);
    } else if (!styles.isEmpty() || !layout.isEmpty()) {
      PSComponentPackageManifest.SlotRef slot = new PSComponentPackageManifest.SlotRef();
      slot.setName(stem + "Chrome");
      slot.setAllowedContentTypes(new ArrayList<>());
      slot.setStyles(styles);
      slot.setLayout(layout);
      List<PSComponentPackageManifest.SlotRef> slots = new ArrayList<>();
      slots.add(slot);
      manifest.setSlots(slots);
    }

    // Resources: thumbnail + <Resource href=…> (CSS/JS) declared on high-traffic widgets.
    List<PSComponentPackageManifest.ResourceRef> resources = new ArrayList<>();
    if (packageRelativeThumbnail != null) {
      PSComponentPackageManifest.ResourceRef res = new PSComponentPackageManifest.ResourceRef();
      // Package-local staging path for the resource (URL-style separators).
      String localPath = "resources/" + fileNameOf(packageRelativeThumbnail);
      res.setPath(localPath);
      res.setTarget(packageRelativeThumbnail);
      res.setType(guessResourceType(packageRelativeThumbnail));
      resources.add(res);
      // Point catalog at the package-local staging path (validator prefers relative refs).
      catalog.setThumbnail(localPath);
      catalog.setIcon(localPath);
    }
    for (PSWidgetXmlModel.Resource declared : model.getResources()) {
      if (declared == null) {
        continue;
      }
      String packageRelative = toPackageRelativePath(declared.getHref());
      if (packageRelative == null) {
        continue;
      }
      PSComponentPackageManifest.ResourceRef res = new PSComponentPackageManifest.ResourceRef();
      final String candidatePath = "resources/" + fileNameOf(packageRelative);
      // Avoid duplicate path keys when thumbnail and Resource share a file name (rare).
      final String localPath =
          resources.stream().anyMatch(r -> candidatePath.equals(r.getPath()))
              ? "resources/" + packageRelative.replace('/', '_')
              : candidatePath;
      res.setPath(localPath);
      res.setTarget(packageRelative);
      String type =
          declared.getType() != null && !declared.getType().isBlank()
              ? declared.getType().trim().toLowerCase(Locale.ROOT)
              : guessResourceType(packageRelative);
      res.setType(type);
      if (declared.getPlacement() != null && !declared.getPlacement().isBlank()) {
        res.setPlacement(declared.getPlacement().trim());
      }
      resources.add(res);
    }
    manifest.setResources(resources);

    // Transitional UserPref / CssPref mirrors.
    List<PSComponentPackageManifest.UserPreference> userPrefs = new ArrayList<>();
    for (PSWidgetXmlModel.UserPref up : model.getUserPrefs()) {
      if (up == null || up.getName() == null || up.getName().isBlank()) {
        continue;
      }
      PSComponentPackageManifest.UserPreference p = new PSComponentPackageManifest.UserPreference();
      p.setName(up.getName());
      p.setDisplayName(up.getDisplayName());
      p.setDatatype(up.getDatatype());
      p.setRequired(up.isRequired());
      p.setDefaultValue(up.getDefaultValue());
      List<PSComponentPackageManifest.EnumValue> enums = new ArrayList<>();
      for (PSWidgetXmlModel.EnumValue ev : up.getEnumValues()) {
        if (ev == null) {
          continue;
        }
        PSComponentPackageManifest.EnumValue e = new PSComponentPackageManifest.EnumValue();
        e.setValue(ev.getValue());
        e.setDisplayValue(ev.getDisplayValue());
        enums.add(e);
      }
      p.setEnumValues(enums);
      userPrefs.add(p);
    }
    manifest.setUserPreferences(userPrefs);

    List<PSComponentPackageManifest.CssPreference> cssPrefs = new ArrayList<>();
    for (PSWidgetXmlModel.CssPref cp : model.getCssPrefs()) {
      if (cp == null || cp.getName() == null || cp.getName().isBlank()) {
        continue;
      }
      PSComponentPackageManifest.CssPreference p = new PSComponentPackageManifest.CssPreference();
      p.setName(cp.getName());
      p.setDisplayName(cp.getDisplayName());
      p.setDatatype(cp.getDatatype());
      p.setDefaultValue(cp.getDefaultValue());
      cssPrefs.add(p);
    }
    manifest.setCssPreferences(cssPrefs);

    try {
      PSComponentPackageManifestValidator.validate(manifest);
    } catch (PSComponentPackageManifestException e) {
      throw new PSWidgetXmlException("Compiled manifest failed validation: " + e.getMessage(), e);
    }

    Map<String, String> artifacts = new LinkedHashMap<>();
    String templateSource = model.getContentBody() != null ? model.getContentBody() : "";
    artifacts.put(sourceRef, templateSource);

    return new PSWidgetXmlCompileResult(model, manifest, artifacts);
  }

  /**
   * Write a compile result under {@code outputDir}: {@code component-package.json} plus text
   * artifacts using package-relative paths. Parent directories are created as needed.
   *
   * @param result non-null compile result
   * @param outputDir non-null destination package root
   * @throws IOException on I/O failure
   */
  public static void writeArtifacts(PSWidgetXmlCompileResult result, Path outputDir)
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
      PSWidgetXmlPackageContext ctx,
      PSWidgetXmlModel model) {
    if (ctx != null) {
      if (ctx.getVersion() != null && !ctx.getVersion().isBlank()) {
        manifest.setVersion(ctx.getVersion());
      }
      if (ctx.getDescription() != null && !ctx.getDescription().isBlank()) {
        // Prefer widget description for component; keep package description only if widget empty.
        if (model.getDescription() == null || model.getDescription().isBlank()) {
          manifest.setDescription(ctx.getDescription());
        } else {
          manifest.setDescription(model.getDescription());
        }
      } else if (model.getDescription() != null) {
        manifest.setDescription(model.getDescription());
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
        for (PSWidgetXmlPackageContext.Dependency d : ctx.getDependencies()) {
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
    if (manifest.getDescription() == null && model.getDescription() != null) {
      manifest.setDescription(model.getDescription());
    }
    // Author from widget becomes publisher when package context has none.
    if (manifest.getPublisher() == null
        && model.getAuthor() != null
        && !model.getAuthor().isBlank()) {
      PSComponentPackageManifest.Publisher pub = new PSComponentPackageManifest.Publisher();
      pub.setName(model.getAuthor());
      manifest.setPublisher(pub);
    }
  }

  static List<PSComponentPackageManifest.Binding> buildBindings(String codeBody) {
    List<PSComponentPackageManifest.Binding> bindings = new ArrayList<>();
    if (codeBody == null || codeBody.isBlank()) {
      return bindings;
    }
    String normalized = PSWidgetXmlParser.normalizeBody(codeBody);
    // Individual simple assignments first (ergonomic; mirrors common widget patterns).
    Matcher m = SIMPLE_ASSIGNMENT.matcher(normalized);
    while (m.find()) {
      PSComponentPackageManifest.Binding b = new PSComponentPackageManifest.Binding();
      b.setVariable(m.group(1));
      b.setExpression(m.group(2).trim());
      bindings.add(b);
    }
    // Always retain full script so control-flow (if/else) is not lost.
    PSComponentPackageManifest.Binding full = new PSComponentPackageManifest.Binding();
    full.setVariable(FULL_CODE_BINDING_VARIABLE);
    full.setExpression(normalized);
    bindings.add(full);
    return bindings;
  }

  /** CssPrefs that look like class names land in slot styles (ADR-003 direction). */
  static Map<String, Object> cssPrefStyles(PSWidgetXmlModel model) {
    Map<String, Object> styles = new LinkedHashMap<>();
    for (PSWidgetXmlModel.CssPref css : model.getCssPrefs()) {
      if (css != null && css.getName() != null && !css.getName().isBlank()) {
        styles.put(css.getName(), css.getDefaultValue() != null ? css.getDefaultValue() : "");
      }
    }
    return styles;
  }

  /**
   * Map layout-ish UserPrefs ({@code layout}, {@code maxlength}) into the slot {@code layout} map
   * (region-slot mapping guidance for list widgets).
   */
  static Map<String, Object> layoutFromUserPrefs(PSWidgetXmlModel model) {
    Map<String, Object> layout = new LinkedHashMap<>();
    for (PSWidgetXmlModel.UserPref up : model.getUserPrefs()) {
      if (up == null || up.getName() == null || up.getName().isBlank()) {
        continue;
      }
      String name = up.getName().trim();
      if ("layout".equalsIgnoreCase(name) || "maxlength".equalsIgnoreCase(name)) {
        layout.put(name, up.getDefaultValue() != null ? up.getDefaultValue() : "");
      }
    }
    return layout;
  }

  static String mapAssembler(String contentType) {
    if (contentType == null || contentType.isBlank()) {
      return "velocityAssembler";
    }
    String t = contentType.trim().toLowerCase(Locale.ROOT);
    return switch (t) {
      case "velocity", "vm" -> "velocityAssembler";
      case "html", "html-first", "htmlfirst" -> "htmlAssembler";
      case "markdown", "md" -> "markdownAssembler";
      default -> t.endsWith("assembler") ? t : t + "Assembler";
    };
  }

  static String assemblerExtension(String assembler) {
    if (assembler == null) {
      return ".vm";
    }
    String a = assembler.toLowerCase(Locale.ROOT);
    if (a.contains("html")) {
      return ".html";
    }
    if (a.contains("markdown") || a.contains("md")) {
      return ".md";
    }
    return ".vm";
  }

  /**
   * Convert a Widget thumbnail path (often absolute install path starting with {@code /}) into a
   * package-relative path for the manifest validator.
   */
  static String toPackageRelativePath(String path) {
    if (path == null || path.isBlank()) {
      return null;
    }
    String p = path.trim().replace('\\', '/');
    while (p.startsWith("/")) {
      p = p.substring(1);
    }
    if (p.isEmpty() || p.contains("..")) {
      return null;
    }
    return p;
  }

  static String fileNameOf(String packageRelativePath) {
    int idx = packageRelativePath.lastIndexOf('/');
    return idx >= 0 ? packageRelativePath.substring(idx + 1) : packageRelativePath;
  }

  static String guessResourceType(String path) {
    String lower = path.toLowerCase(Locale.ROOT);
    if (lower.endsWith(".css")) {
      return "css";
    }
    if (lower.endsWith(".js")) {
      return "js";
    }
    if (lower.endsWith(".png")
        || lower.endsWith(".jpg")
        || lower.endsWith(".jpeg")
        || lower.endsWith(".gif")
        || lower.endsWith(".svg")
        || lower.endsWith(".webp")) {
      return "image";
    }
    return "file";
  }

  /**
   * Resolve a package-relative path under {@code base} using portable {@link Path#resolve(String)}
   * per segment — never string-concatenate OS separators. Accepts URL-style {@code /} and Windows
   * {@code \} in the relative string; strips a leading slash; rejects {@code ..} escapes.
   */
  static Path resolvePackageRelative(Path base, String packageRelative) {
    if (packageRelative == null || packageRelative.isBlank()) {
      throw new IllegalArgumentException("relative path must be non-blank");
    }
    String normalized = packageRelative.replace('\\', '/');
    while (normalized.startsWith("/")) {
      normalized = normalized.substring(1);
    }
    Path p = base;
    for (String segment : normalized.split("/")) {
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
