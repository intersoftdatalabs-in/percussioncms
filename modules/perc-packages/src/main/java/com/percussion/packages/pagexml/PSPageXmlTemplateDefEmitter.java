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
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Emits legacy install-path {@code *.templateDef} ({@code <assembly-template>}) XML from a modern
 * Component Package Manifest + template source (ADR-004 dual-ship / issue #2786).
 *
 * <p>Used so product packages can author modern {@code pages/&lt;id&gt;/component-package.json} while
 * {@code .ppkg} install continues to consume {@code TemplateDef} dependencies until the deployer
 * install path is fully modern.
 */
public final class PSPageXmlTemplateDefEmitter {

  private static final String DEFAULT_ASSEMBLER_PATH =
      "Java/global/percussion/assembly/pageAssembler";
  private static final String DEFAULT_ACTIVE_ASSEMBLY = "Normal";
  private static final String DEFAULT_ASSEMBLY_URL = "../assembler/render";
  private static final String DEFAULT_CHARSET = "UTF-8";
  private static final String DEFAULT_MIME = "text/html";
  private static final String DEFAULT_TEMPLATE_TYPE = "Shared";
  private static final String DEFAULT_PUBLISH_WHEN = "Default";
  private static final String DEFAULT_GLOBAL_USAGE = "None";

  private PSPageXmlTemplateDefEmitter() {
    // utility
  }

  /**
   * Build install-parity {@code *.templateDef} XML for a compiled page component.
   *
   * @param manifest non-null modern page package manifest ({@code catalog.kind=page} expected)
   * @param templateSource Velocity (or other) body text; may be empty but not null preferred
   * @param guid optional legacy assembly GUID (e.g. {@code 0-4-591}); when null/blank omitted as empty
   * @return XML document text (UTF-8 logical, no BOM)
   */
  public static String emit(
      PSComponentPackageManifest manifest, String templateSource, String guid) {
    Objects.requireNonNull(manifest, "manifest");
    if (manifest.getId() == null || manifest.getId().isBlank()) {
      throw new IllegalArgumentException("manifest.id is required");
    }
    if (manifest.getTemplates() == null || manifest.getTemplates().isEmpty()) {
      throw new IllegalArgumentException("manifest.templates must not be empty for page dual-ship");
    }

    PSComponentPackageManifest.TemplateRef t = manifest.getTemplates().get(0);
    String name = firstNonBlank(t.getName(), manifest.getId());
    String label =
        firstNonBlank(
            manifest.getName(),
            manifest.getCatalog() != null ? manifest.getCatalog().getTitle() : null,
            name);
    // Prefer catalog/template description only — package-level description is often a package
    // summary and was not written into individual product templateDefs.
    String description =
        firstNonBlank(
            manifest.getCatalog() != null ? manifest.getCatalog().getDescription() : null, "");
    String assembler = toLegacyAssemblerPath(t.getAssembler());
    String outputFormat = toLegacyOutputFormat(t.getType());
    String body = templateSource != null ? templateSource : "";
    String guidValue = guid != null ? guid.trim() : "";

    StringBuilder xml = new StringBuilder(Math.max(512, body.length() + 512));
    xml.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>  <assembly-template id=\"1\">\n");
    appendBindings(xml, t.getBindings());
    xml.append("    <template-slot-ids/>\n");
    xml.append("    <guid>").append(escapeXml(guidValue)).append("</guid>\n");
    xml.append("    <active-assembly-type>")
        .append(DEFAULT_ACTIVE_ASSEMBLY)
        .append("</active-assembly-type>\n");
    xml.append("    <assembler>").append(escapeXml(assembler)).append("</assembler>\n");
    xml.append("    <assembly-url>").append(DEFAULT_ASSEMBLY_URL).append("</assembly-url>\n");
    xml.append("    <charset>").append(DEFAULT_CHARSET).append("</charset>\n");
    xml.append("    <description>").append(escapeXml(description)).append("</description>\n");
    xml.append("    <global-template/>\n");
    xml.append("    <global-template-usage>")
        .append(DEFAULT_GLOBAL_USAGE)
        .append("</global-template-usage>\n");
    xml.append("    <label>").append(escapeXml(label)).append("</label>\n");
    xml.append("    <location-prefix/>\n");
    xml.append("    <location-suffix/>\n");
    xml.append("    <mime-type>").append(DEFAULT_MIME).append("</mime-type>\n");
    xml.append("    <name>").append(escapeXml(name)).append("</name>\n");
    xml.append("    <output-format>").append(escapeXml(outputFormat)).append("</output-format>\n");
    xml.append("    <publish-when>").append(DEFAULT_PUBLISH_WHEN).append("</publish-when>\n");
    xml.append("    <style-sheet-path/>\n");
    xml.append("    <template>").append(escapeXml(body)).append("</template>\n");
    xml.append("    <template-type>").append(DEFAULT_TEMPLATE_TYPE).append("</template-type>\n");
    xml.append("    <type>TEMPLATE</type>\n");
    xml.append("    <variant>false</variant>\n");
    xml.append("  </assembly-template>\n");
    return xml.toString();
  }

  /**
   * Map short Component Package Manifest assembler id back to the product extension path used in
   * legacy {@code *.templateDef} files.
   */
  public static String toLegacyAssemblerPath(String assembler) {
    if (assembler == null || assembler.isBlank()) {
      return DEFAULT_ASSEMBLER_PATH;
    }
    String a = assembler.trim();
    if (a.contains("/")) {
      return a;
    }
    String leaf = a;
    String lower = leaf.toLowerCase(Locale.ROOT);
    return switch (lower) {
      case "pageassembler" -> "Java/global/percussion/assembly/pageAssembler";
      case "pagevariantassembler" -> "Java/global/percussion/assembly/pageVariantAssembler";
      case "velocityassembler" -> "Java/global/percussion/assembly/velocityAssembler";
      case "htmlassembler" -> "Java/global/percussion/assembly/htmlAssembler";
      case "markdownassembler" -> "Java/global/percussion/assembly/markdownAssembler";
      case "legacyassembler", "xslassembler" -> "Java/global/percussion/assembly/legacyAssembler";
      default -> "Java/global/percussion/assembly/" + leaf;
    };
  }

  /** Map modern template type ({@code page}/{@code global}/…) to legacy {@code output-format}. */
  public static String toLegacyOutputFormat(String templateType) {
    if (templateType == null || templateType.isBlank()) {
      return "Page";
    }
    String t = templateType.trim().toLowerCase(Locale.ROOT);
    return switch (t) {
      case "page" -> "Page";
      case "snippet" -> "Snippet";
      case "global" -> "Global";
      case "binary" -> "Binary";
      case "resource" -> "Resource";
      default -> capitalize(templateType.trim());
    };
  }

  private static void appendBindings(
      StringBuilder xml, List<PSComponentPackageManifest.Binding> bindings) {
    if (bindings == null || bindings.isEmpty()) {
      xml.append("    <bindings/>\n");
      return;
    }
    xml.append("    <bindings>\n");
    for (PSComponentPackageManifest.Binding b : bindings) {
      if (b == null || b.getVariable() == null || b.getVariable().isBlank()) {
        continue;
      }
      xml.append("      <binding variable=\"")
          .append(escapeXmlAttr(b.getVariable()))
          .append("\" expression=\"")
          .append(escapeXmlAttr(b.getExpression() != null ? b.getExpression() : ""))
          .append("\"/>\n");
    }
    xml.append("    </bindings>\n");
  }

  static String escapeXml(String s) {
    if (s == null || s.isEmpty()) {
      return "";
    }
    StringBuilder out = new StringBuilder(s.length() + 16);
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      switch (c) {
        case '&' -> out.append("&amp;");
        case '<' -> out.append("&lt;");
        case '>' -> out.append("&gt;");
        case '"' -> out.append("&quot;");
        // apostrophe uncommon in body; keep literal for Velocity
        default -> out.append(c);
      }
    }
    return out.toString();
  }

  static String escapeXmlAttr(String s) {
    return escapeXml(s).replace("'", "&apos;");
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

  private static String capitalize(String s) {
    if (s.isEmpty()) {
      return s;
    }
    return Character.toUpperCase(s.charAt(0)) + s.substring(1);
  }
}
