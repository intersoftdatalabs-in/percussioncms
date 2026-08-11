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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Install-side Widget definition XML emitter for modern-only product packages (ADR-004 / ship-exit
 * batches #2883 / #2884 / #2885, parent #2630).
 *
 * <p><strong>Authoring truth:</strong> {@code widgets/&lt;stem&gt;/component-package.json} +
 * templates.
 *
 * <p><strong>Install wire format (transitional):</strong> package build materializes {@code
 * sys__UserDependency--rxconfig/Widgets/*.xml} from modern sources so deployer / {@code
 * PSWidgetDao} still receive legacy Widget XML. Product source trees for converted batches no
 * longer commit that XML.
 *
 * <p>Policy: materialize only when modern widget packages are present <em>and</em> the package has
 * no committed install Widget XML (so dual-ship packages that still commit XML keep authored
 * install sources until their ship-exit slice). Keep {@code PSLegacyDefinitionXmlShim} and
 * upgrade-input compilers.
 *
 * @see PSWidgetXmlDualShip
 * @see PSWidgetXmlPackageCompiler
 */
public final class PSWidgetXmlInstallEmitter {

  private PSWidgetXmlInstallEmitter() {
    // utility
  }

  /**
   * Whether the package still commits install Widget definition XML under {@code
   * sys__UserDependency--rxconfig/Widgets/*.xml}.
   */
  public static boolean hasCommittedWidgetXml(Path packageDir) throws IOException {
    Objects.requireNonNull(packageDir, "packageDir");
    Path widgetsDir = PSWidgetXmlPackageCompiler.resolveWidgetsDir(packageDir);
    if (!Files.isDirectory(widgetsDir)) {
      return false;
    }
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(widgetsDir, "*.xml")) {
      for (Path p : stream) {
        if (Files.isRegularFile(p)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Materialize install-path Widget XML from modern {@code widgets/} sources when the package is
   * modern-only (modern present, no committed Widget XML). No-op when dual-ship XML is still
   * committed or when no modern widgets exist.
   *
   * @param packageDir product package source or staging copy
   * @return number of Widget XML files written
   * @throws PSWidgetXmlException on load/emit failure
   * @throws IOException on I/O failure
   */
  public static int materializeInstallWidgetXml(Path packageDir)
      throws PSWidgetXmlException, IOException {
    Objects.requireNonNull(packageDir, "packageDir");
    if (!Files.isDirectory(packageDir)) {
      return 0;
    }
    if (!PSWidgetXmlDualShip.hasModernWidgetSources(packageDir)) {
      return 0;
    }
    // Dual-ship packages still commit install XML — leave them alone until their ship-exit slice.
    if (hasCommittedWidgetXml(packageDir)) {
      return 0;
    }

    List<PSWidgetXmlCompileResult> modern = PSWidgetXmlDualShip.compileModernWidgets(packageDir);
    Path widgetsDir = PSWidgetXmlPackageCompiler.resolveWidgetsDir(packageDir);
    Files.createDirectories(widgetsDir);
    int written = 0;
    for (PSWidgetXmlCompileResult result : modern) {
      PSWidgetXmlModel model = modelFromModern(result);
      String stem = model.widgetStem();
      if (stem == null || stem.isBlank()) {
        stem = result.getManifest().getId();
      }
      if (stem == null || stem.isBlank()) {
        throw new PSWidgetXmlException(
            "Cannot emit install Widget XML without stem/id under " + packageDir);
      }
      Path out = widgetsDir.resolve(stem + ".xml");
      Files.writeString(out, emitWidgetXml(model), StandardCharsets.UTF_8);
      written++;
    }
    return written;
  }

  /**
   * Build a parse-model suitable for install XML emission from a modern compile result (manifest +
   * template artifacts).
   */
  public static PSWidgetXmlModel modelFromModern(PSWidgetXmlCompileResult modern)
      throws PSWidgetXmlException {
    Objects.requireNonNull(modern, "modern");
    PSComponentPackageManifest manifest = modern.getManifest();
    if (manifest == null) {
      throw new PSWidgetXmlException("Modern compile result missing manifest");
    }

    PSWidgetXmlModel model = new PSWidgetXmlModel();
    String stem = manifest.getId();
    if (stem == null || stem.isBlank()) {
      stem = "widget";
    }
    model.setSourceFileName(stem + ".xml");

    PSComponentPackageManifest.Catalog catalog = manifest.getCatalog();
    if (catalog != null) {
      model.setTitle(
          firstNonBlank(catalog.getTitle(), manifest.getName(), stem));
      model.setCategory(catalog.getCategory());
      model.setDescription(
          firstNonBlank(catalog.getDescription(), manifest.getDescription()));
      model.setAuthor(catalog.getAuthor());
      model.setPreferredEditorWidth(catalog.getPreferredEditorWidth());
      model.setPreferredEditorHeight(catalog.getPreferredEditorHeight());
      model.setCreateSharedAsset(catalog.getCreateSharedAsset());
      model.setEditableOnTemplate(catalog.getEditableOnTemplate());
      model.setResponsive(catalog.getResponsive());
    } else {
      model.setTitle(firstNonBlank(manifest.getName(), stem));
      model.setDescription(manifest.getDescription());
    }

    if (manifest.getContentTypes() != null && !manifest.getContentTypes().isEmpty()) {
      PSComponentPackageManifest.ContentTypeRef ct = manifest.getContentTypes().get(0);
      if (ct != null && ct.getName() != null && !ct.getName().isBlank()) {
        model.setContentTypeName(ct.getName().trim());
      }
    }

    // Thumbnail: prefer first image resource target (install path), else package-relative catalog.
    String thumbnail = null;
    if (manifest.getResources() != null) {
      for (PSComponentPackageManifest.ResourceRef res : manifest.getResources()) {
        if (res == null) {
          continue;
        }
        String type = res.getType() != null ? res.getType().trim().toLowerCase(Locale.ROOT) : "";
        if ("image".equals(type) && res.getTarget() != null && !res.getTarget().isBlank()) {
          thumbnail = toInstallHref(res.getTarget());
          break;
        }
      }
    }
    if (thumbnail == null && catalog != null && catalog.getThumbnail() != null) {
      // Catalog may hold package-local resources/… path — leave without inventing install root.
      String t = catalog.getThumbnail().trim();
      if (!t.isEmpty() && !t.startsWith("resources/")) {
        thumbnail = toInstallHref(t);
      }
    }
    model.setThumbnail(thumbnail);

    // CSS / JS Resource elements (skip image thumbnail already mapped to WidgetPrefs).
    if (manifest.getResources() != null) {
      for (PSComponentPackageManifest.ResourceRef res : manifest.getResources()) {
        if (res == null || res.getTarget() == null || res.getTarget().isBlank()) {
          continue;
        }
        String type = res.getType() != null ? res.getType().trim().toLowerCase(Locale.ROOT) : "";
        if ("image".equals(type) || "file".equals(type) || type.isEmpty()) {
          // Thumbnail already handled; skip generic files that are not CSS/JS.
          if (!"css".equals(type) && !"js".equals(type)) {
            continue;
          }
        }
        if (!"css".equals(type) && !"js".equals(type)) {
          continue;
        }
        PSWidgetXmlModel.Resource r = new PSWidgetXmlModel.Resource();
        r.setHref(toInstallHref(res.getTarget()));
        r.setType(type);
        if (res.getPlacement() != null && !res.getPlacement().isBlank()) {
          r.setPlacement(res.getPlacement().trim());
        }
        model.getResources().add(r);
      }
    }

    if (manifest.getUserPreferences() != null) {
      for (PSComponentPackageManifest.UserPreference up : manifest.getUserPreferences()) {
        if (up == null || up.getName() == null || up.getName().isBlank()) {
          continue;
        }
        PSWidgetXmlModel.UserPref pref = new PSWidgetXmlModel.UserPref();
        pref.setName(up.getName());
        pref.setDisplayName(up.getDisplayName());
        pref.setDatatype(up.getDatatype());
        pref.setRequired(up.isRequired());
        pref.setDefaultValue(up.getDefaultValue());
        if (up.getEnumValues() != null) {
          for (PSComponentPackageManifest.EnumValue ev : up.getEnumValues()) {
            if (ev == null) {
              continue;
            }
            PSWidgetXmlModel.EnumValue e = new PSWidgetXmlModel.EnumValue();
            e.setValue(ev.getValue());
            e.setDisplayValue(ev.getDisplayValue());
            pref.getEnumValues().add(e);
          }
        }
        model.getUserPrefs().add(pref);
      }
    }

    if (manifest.getCssPreferences() != null) {
      for (PSComponentPackageManifest.CssPreference cp : manifest.getCssPreferences()) {
        if (cp == null || cp.getName() == null || cp.getName().isBlank()) {
          continue;
        }
        PSWidgetXmlModel.CssPref css = new PSWidgetXmlModel.CssPref();
        css.setName(cp.getName());
        css.setDisplayName(cp.getDisplayName());
        css.setDatatype(cp.getDatatype());
        css.setDefaultValue(cp.getDefaultValue());
        model.getCssPrefs().add(css);
      }
    }

    // Template body + JEXL code from first template (product widgets are single-snippet).
    String contentType = "velocity";
    String contentBody = "";
    String codeBody = null;
    if (manifest.getTemplates() != null && !manifest.getTemplates().isEmpty()) {
      PSComponentPackageManifest.TemplateRef t = manifest.getTemplates().get(0);
      if (t != null) {
        contentType = contentTypeFromAssembler(t.getAssembler());
        if (t.getSourceRef() != null && modern.getTextArtifacts() != null) {
          String body = modern.getTextArtifacts().get(t.getSourceRef());
          if (body != null) {
            contentBody = body;
          }
        }
        if (t.getBindings() != null) {
          for (PSComponentPackageManifest.Binding b : t.getBindings()) {
            if (b == null || b.getVariable() == null) {
              continue;
            }
            if (PSWidgetXmlCompiler.FULL_CODE_BINDING_VARIABLE.equals(b.getVariable())) {
              codeBody = b.getExpression();
              break;
            }
          }
        }
      }
    }
    model.setContentType(contentType);
    model.setContentBody(contentBody);
    if (codeBody != null && !codeBody.isBlank()) {
      model.setCodeType("jexl");
      model.setCodeBody(codeBody);
    }

    return model;
  }

  /** Serialize a widget model to legacy install Widget definition XML. */
  public static String emitWidgetXml(PSWidgetXmlModel model) {
    Objects.requireNonNull(model, "model");
    StringBuilder sb = new StringBuilder(512);
    sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    sb.append("<Widget>\n");
    sb.append("\t<WidgetPrefs");
    appendAttr(sb, "title", model.getTitle());
    appendAttr(sb, "contenttype_name", model.getContentTypeName());
    appendAttr(sb, "category", model.getCategory());
    appendAttr(sb, "description", model.getDescription());
    appendAttr(sb, "author", model.getAuthor());
    appendAttr(sb, "thumbnail", model.getThumbnail());
    if (model.getPreferredEditorWidth() != null) {
      appendAttr(sb, "preferred_editor_width", Integer.toString(model.getPreferredEditorWidth()));
    }
    if (model.getPreferredEditorHeight() != null) {
      appendAttr(sb, "preferred_editor_height", Integer.toString(model.getPreferredEditorHeight()));
    }
    if (model.getCreateSharedAsset() != null) {
      appendAttr(sb, "create_shared_asset", Boolean.toString(model.getCreateSharedAsset()));
    }
    if (model.getEditableOnTemplate() != null) {
      appendAttr(sb, "is_editable_on_template", Boolean.toString(model.getEditableOnTemplate()));
    }
    if (model.getResponsive() != null) {
      appendAttr(sb, "is_responsive", Boolean.toString(model.getResponsive()));
    }
    sb.append(" />\n");

    for (PSWidgetXmlModel.Resource res : model.getResources()) {
      if (res == null || res.getHref() == null || res.getHref().isBlank()) {
        continue;
      }
      sb.append("\t<Resource");
      appendAttr(sb, "href", res.getHref());
      appendAttr(sb, "type", res.getType());
      appendAttr(sb, "placement", res.getPlacement());
      sb.append(" />\n");
    }

    for (PSWidgetXmlModel.UserPref up : model.getUserPrefs()) {
      if (up == null || up.getName() == null || up.getName().isBlank()) {
        continue;
      }
      sb.append("\t<UserPref");
      appendAttr(sb, "name", up.getName());
      appendAttr(sb, "display_name", up.getDisplayName());
      appendAttr(sb, "datatype", up.getDatatype());
      if (up.isRequired()) {
        appendAttr(sb, "required", "true");
      }
      appendAttr(sb, "default_value", up.getDefaultValue());
      if (up.getEnumValues() == null || up.getEnumValues().isEmpty()) {
        sb.append(" />\n");
      } else {
        sb.append(">\n");
        for (PSWidgetXmlModel.EnumValue ev : up.getEnumValues()) {
          if (ev == null) {
            continue;
          }
          sb.append("\t\t<EnumValue");
          appendAttr(sb, "value", ev.getValue());
          appendAttr(sb, "display_value", ev.getDisplayValue());
          sb.append(" />\n");
        }
        sb.append("\t</UserPref>\n");
      }
    }

    for (PSWidgetXmlModel.CssPref css : model.getCssPrefs()) {
      if (css == null || css.getName() == null || css.getName().isBlank()) {
        continue;
      }
      sb.append("\t<CssPref");
      appendAttr(sb, "name", css.getName());
      appendAttr(sb, "display_name", css.getDisplayName());
      appendAttr(sb, "datatype", css.getDatatype());
      appendAttr(sb, "default_value", css.getDefaultValue());
      sb.append(" />\n");
    }

    if (model.getCodeBody() != null && !model.getCodeBody().isBlank()) {
      String codeType =
          model.getCodeType() != null && !model.getCodeType().isBlank()
              ? model.getCodeType()
              : "jexl";
      sb.append("\t<Code type=\"").append(escapeAttr(codeType)).append("\">\n");
      sb.append("        <![CDATA[\n");
      sb.append(normalizeCdata(model.getCodeBody()));
      sb.append("\n    ]]>\n");
      sb.append("\t</Code>\n");
    }

    String contentType =
        model.getContentType() != null && !model.getContentType().isBlank()
            ? model.getContentType()
            : "velocity";
    sb.append("\t<Content type=\"").append(escapeAttr(contentType)).append("\"><![CDATA[");
    sb.append(normalizeCdata(model.getContentBody() != null ? model.getContentBody() : ""));
    sb.append("]]></Content>\n");
    sb.append("</Widget>\n");
    return sb.toString();
  }

  private static String contentTypeFromAssembler(String assembler) {
    if (assembler == null || assembler.isBlank()) {
      return "velocity";
    }
    String a = assembler.toLowerCase(Locale.ROOT);
    if (a.contains("html")) {
      return "html";
    }
    if (a.contains("markdown") || a.contains("md")) {
      return "markdown";
    }
    return "velocity";
  }

  /**
   * Install href form used in product Widget XML ({@code /rx_resources/...} or {@code
   * /web_resources/...}). Package-relative targets without a leading slash get one.
   */
  static String toInstallHref(String packageRelativeOrInstall) {
    if (packageRelativeOrInstall == null || packageRelativeOrInstall.isBlank()) {
      return null;
    }
    String p = packageRelativeOrInstall.trim().replace('\\', '/');
    if (p.startsWith("/")) {
      return p;
    }
    return "/" + p;
  }

  private static void appendAttr(StringBuilder sb, String name, String value) {
    if (value == null || value.isBlank()) {
      return;
    }
    sb.append(' ').append(name).append("=\"").append(escapeAttr(value)).append('"');
  }

  private static String escapeAttr(String value) {
    return value
        .replace("&", "&amp;")
        .replace("\"", "&quot;")
        .replace("<", "&lt;")
        .replace(">", "&gt;");
  }

  private static String normalizeCdata(String body) {
    // Avoid breaking CDATA sections if product content ever contains the terminator.
    return body.replace("]]>", "]]]]><![CDATA[>");
  }

  private static String firstNonBlank(String... values) {
    if (values == null) {
      return null;
    }
    for (String v : values) {
      if (v != null && !v.isBlank()) {
        return v;
      }
    }
    return null;
  }
}
