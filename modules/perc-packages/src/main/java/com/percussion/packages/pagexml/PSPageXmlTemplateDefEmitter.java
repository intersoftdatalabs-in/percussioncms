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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

  /** Product extension context for registered assembly plugins. */
  public static final String ASSEMBLER_EXTENSION_PREFIX = "Java/global/percussion/assembly/";

  /**
   * Registered Component Package Manifest assembler ids → product extension paths (issue #5818 /
   * parent #2626). A manifest-authored page template selects its assembler through this catalog:
   * the short {@code templates[].assembler} id resolves to the registered path, so new page
   * templates never need Widget/Page definition XML. Keys are canonical short ids; lookup is
   * case-insensitive (see {@link #isRegisteredAssemblerId}).
   */
  public static final Map<String, String> REGISTERED_ASSEMBLER_PATHS = registeredAssemblerPaths();

  private static Map<String, String> registeredAssemblerPaths() {
    Map<String, String> paths = new LinkedHashMap<>();
    paths.put("pageAssembler", ASSEMBLER_EXTENSION_PREFIX + "pageAssembler");
    paths.put("pageVariantAssembler", ASSEMBLER_EXTENSION_PREFIX + "pageVariantAssembler");
    paths.put("velocityAssembler", ASSEMBLER_EXTENSION_PREFIX + "velocityAssembler");
    paths.put("htmlAssembler", ASSEMBLER_EXTENSION_PREFIX + "htmlAssembler");
    paths.put("markdownAssembler", ASSEMBLER_EXTENSION_PREFIX + "markdownAssembler");
    paths.put("legacyAssembler", ASSEMBLER_EXTENSION_PREFIX + "legacyAssembler");
    paths.put("dispatchAssembler", ASSEMBLER_EXTENSION_PREFIX + "dispatchAssembler");
    paths.put("resourceAssembler", ASSEMBLER_EXTENSION_PREFIX + "resourceAssembler");
    paths.put("pageDatabaseAssembler", ASSEMBLER_EXTENSION_PREFIX + "pageDatabaseAssembler");
    paths.put("binaryAssembler", ASSEMBLER_EXTENSION_PREFIX + "binaryAssembler");
    return Collections.unmodifiableMap(paths);
  }

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
    // Prefer catalog description; fall back to package-level description so dual-ship
    // round-trip preserves descriptions when catalog omits one (e.g. some Baseline pages).
    String description =
        firstNonBlank(
            manifest.getCatalog() != null ? manifest.getCatalog().getDescription() : null,
            manifest.getDescription(),
            "");
    String assembler = toLegacyAssemblerPath(t.getAssembler());
    String outputFormat = toLegacyOutputFormat(t.getType());
    String body = templateSource != null ? templateSource : "";
    String guidValue = guid != null ? guid.trim() : "";
    boolean binary = "binary".equalsIgnoreCase(t.getType());
    // Binary asset templates bind $sys.mimetype; do not inject HTML defaults (#3674).
    String mime = firstNonBlank(t.getMimeType(), binary ? "" : DEFAULT_MIME);
    String charset = binary ? "" : DEFAULT_CHARSET;
    String publishWhen = firstNonBlank(t.getPublishWhen(), DEFAULT_PUBLISH_WHEN);
    String locationPrefix = t.getLocationPrefix() != null ? t.getLocationPrefix() : "";
    String locationSuffix = t.getLocationSuffix() != null ? t.getLocationSuffix() : "";
    String templateType = firstNonBlank(t.getLegacyTemplateType(), DEFAULT_TEMPLATE_TYPE);

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
    appendMaybeEmpty(xml, "charset", charset);
    xml.append("    <description>").append(escapeXml(description)).append("</description>\n");
    xml.append("    <global-template/>\n");
    xml.append("    <global-template-usage>")
        .append(DEFAULT_GLOBAL_USAGE)
        .append("</global-template-usage>\n");
    xml.append("    <label>").append(escapeXml(label)).append("</label>\n");
    if (locationPrefix.isEmpty()) {
      xml.append("    <location-prefix/>\n");
    } else {
      xml.append("    <location-prefix>")
          .append(escapeXml(locationPrefix))
          .append("</location-prefix>\n");
    }
    if (locationSuffix.isEmpty()) {
      xml.append("    <location-suffix/>\n");
    } else {
      xml.append("    <location-suffix>")
          .append(escapeXml(locationSuffix))
          .append("</location-suffix>\n");
    }
    appendMaybeEmpty(xml, "mime-type", mime);
    xml.append("    <name>").append(escapeXml(name)).append("</name>\n");
    xml.append("    <output-format>").append(escapeXml(outputFormat)).append("</output-format>\n");
    xml.append("    <publish-when>").append(escapeXml(publishWhen)).append("</publish-when>\n");
    xml.append("    <style-sheet-path/>\n");
    xml.append("    <template>").append(escapeXml(body)).append("</template>\n");
    xml.append("    <template-type>").append(escapeXml(templateType)).append("</template-type>\n");
    xml.append("    <type>TEMPLATE</type>\n");
    xml.append("    <variant>false</variant>\n");
    xml.append("  </assembly-template>\n");
    return xml.toString();
  }

  /**
   * Whether the value names a registered product assembler — either a short Component Package
   * Manifest id (e.g. {@code htmlAssembler}) or the full extension path (e.g. {@code
   * Java/global/percussion/assembly/htmlAssembler}). Comparison is case-insensitive; the legacy
   * {@code xslAssembler} alias counts as {@code legacyAssembler}. Custom (unregistered) ids and
   * full paths return {@code false}: they still pass through {@link #toLegacyAssemblerPath} for
   * the legacy-upgrade emit path only — modern-only load paths ({@link
   * #resolveRegisteredAssemblerPath}, {@code PSPageXmlManifestReadback}) fail closed on them
   * (issue #5864 / parent #2626).
   */
  public static boolean isRegisteredAssemblerId(String assembler) {
    return lookupRegisteredPath(assembler) != null;
  }

  /**
   * Resolve an assembler value strictly against the registered product catalog (issue #5864 /
   * parent #2626): the modern-only resolution behind migrated definitions.
   *
   * <p>Unlike {@link #toLegacyAssemblerPath}, there is <strong>no fallback</strong> here — blank
   * values do not default to the page assembler, unregistered short ids do not gain the product
   * extension prefix, and full extension paths are accepted only when their leaf names a
   * registered assembler. Callers on the modern manifest load path fail closed so an unmigrated
   * assembler surfaces as a migration blocker instead of a silently defaulted install.
   *
   * @param assembler short manifest id or full extension path (must name a registered assembler)
   * @return registered product extension path (never null)
   * @throws IllegalArgumentException when blank or unregistered
   */
  public static String resolveRegisteredAssemblerPath(String assembler) {
    String registered = lookupRegisteredPath(assembler);
    if (registered == null) {
      throw new IllegalArgumentException(
          "Unregistered assembler '"
              + assembler
              + "': modern page definitions must name a registered catalog id "
              + REGISTERED_ASSEMBLER_PATHS.keySet()
              + " (xslAssembler counts as legacyAssembler).");
    }
    return registered;
  }

  /**
   * Map short Component Package Manifest assembler id back to the product extension path used in
   * legacy {@code *.templateDef} files.
   *
   * <p><strong>Lenient upgrade path (kept):</strong> blank defaults to the page assembler, full
   * paths pass through, and unregistered short ids gain the product extension prefix so
   * customer legacy XML with site-local custom assemblers still compiles and emits. Modern-only
   * load paths behind migrated definitions must use {@link #resolveRegisteredAssemblerPath}
   * instead — the silent fallback is removed there (issue #5864).
   */
  public static String toLegacyAssemblerPath(String assembler) {
    if (assembler == null || assembler.isBlank()) {
      return DEFAULT_ASSEMBLER_PATH;
    }
    String a = assembler.trim();
    if (a.contains("/")) {
      return a;
    }
    String registered = lookupRegisteredPath(a);
    if (registered != null) {
      return registered;
    }
    return ASSEMBLER_EXTENSION_PREFIX + a;
  }

  /**
   * Resolve a short id (or the leaf of a full extension path) against {@link
   * #REGISTERED_ASSEMBLER_PATHS}; {@code null} when unregistered. The {@code xslAssembler} alias
   * resolves to the {@code legacyAssembler} path.
   */
  private static String lookupRegisteredPath(String assembler) {
    if (assembler == null || assembler.isBlank()) {
      return null;
    }
    String a = assembler.trim();
    int slash = a.lastIndexOf('/');
    String leaf = slash >= 0 ? a.substring(slash + 1) : a;
    if (leaf.isBlank()) {
      return null;
    }
    String lower = leaf.toLowerCase(Locale.ROOT);
    if (lower.equals("xslassembler")) {
      return REGISTERED_ASSEMBLER_PATHS.get("legacyAssembler");
    }
    for (Map.Entry<String, String> entry : REGISTERED_ASSEMBLER_PATHS.entrySet()) {
      if (entry.getKey().toLowerCase(Locale.ROOT).equals(lower)) {
        return entry.getValue();
      }
    }
    return null;
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

  private static void appendMaybeEmpty(StringBuilder xml, String tag, String value) {
    if (value == null || value.isEmpty()) {
      xml.append("    <").append(tag).append("/>\n");
      return;
    }
    xml.append("    <")
        .append(tag)
        .append(">")
        .append(escapeXml(value))
        .append("</")
        .append(tag)
        .append(">\n");
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
