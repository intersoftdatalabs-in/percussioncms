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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Operator troubleshooting diagnostics readback for the template assembler catalog (issue #6081 /
 * parent #2626).
 *
 * <p>Template normalization keeps one assembler model per template (Velocity, HTML-first,
 * Markdown, legacy XSL), selected through the short {@code templates[].assembler} id against the
 * registered product catalog ({@link PSPageXmlTemplateDefEmitter#REGISTERED_ASSEMBLER_PATHS}).
 * Behind migrated definitions there is no silent fallback (issue #5864): a blank or unregistered
 * assembler fails fast instead of defaulting, so a template that will not load needs a readable
 * explanation — not a stack trace.
 *
 * <p>This class is that explanation. Given modern Component Package Manifests it reads back the
 * registered catalog and diagnoses every {@code templates[]} entry: which registered extension
 * path the assembler resolves to, or exactly why it is blocked (blank value, unregistered short
 * id, unregistered full extension path) plus the fix action. {@link Report#toMarkdown()} renders
 * the panel for operator tooling and issue evidence; {@link Report#toText()} renders a plain-text
 * variant for CLI use.
 *
 * <p>Companion to {@link PSPageXmlMigrationDryRun}, which covers the legacy
 * {@code *.templateDef} upgrade direction: the dry-run reports what blocks converting legacy XML,
 * while this report diagnoses the modern manifests operators already ship. Reads nothing from
 * disk and writes nothing; callers supply parsed manifests.
 *
 * <p>After migration, {@link Report#collectStaleCatalogEntries()} finds registered catalog rows
 * that no diagnosed template resolves to anymore, and {@link
 * Report#withoutStaleCatalogEntries()} returns the collection pass (pruned catalog, same
 * per-template verdicts) so operators can retire dead assembler registrations (issue #6267 /
 * parent #2626).
 *
 * <p>Every render/install path consumes only the first {@code templates[]} entry. {@link
 * Report#getRenderPaths()} names that single render entry per manifest — canonical assembler
 * id plus resolved path — and lists the sibling variants the render path ignores, so a
 * multi-template manifest gets a readable explanation instead of a silent drop (issue #7049 /
 * parent #2626).
 */
public final class PSAssemblerCatalogDiagnostics {

  /** Per-template assembler verdict. */
  public enum Status {
    /** Assembler resolves to a registered product extension path. */
    OK,
    /** Assembler cannot load on the modern path; see {@link Entry#getReason()}. */
    BLOCKED
  }

  /** One registered catalog row: short manifest id → product extension path. */
  public static final class CatalogEntry {
    private final String id;
    private final String extensionPath;

    private CatalogEntry(String id, String extensionPath) {
      this.id = id;
      this.extensionPath = extensionPath;
    }

    /** Short manifest assembler id (never null). */
    public String getId() {
      return id;
    }

    /** Registered product extension path (never null). */
    public String getExtensionPath() {
      return extensionPath;
    }
  }

  /** Assembler diagnosis for one {@code templates[]} entry of one manifest. */
  public static final class Entry {
    private final String manifestId;
    private final String templateName;
    private final String declaredAssembler;
    private final Status status;
    private final String resolvedPath;
    private final String reason;
    private final String fixHint;
    private final String note;

    private Entry(
        String manifestId,
        String templateName,
        String declaredAssembler,
        Status status,
        String resolvedPath,
        String reason,
        String fixHint,
        String note) {
      this.manifestId = manifestId;
      this.templateName = templateName;
      this.declaredAssembler = declaredAssembler;
      this.status = status;
      this.resolvedPath = resolvedPath;
      this.reason = reason;
      this.fixHint = fixHint;
      this.note = note;
    }

    /** Owning manifest id (never null). */
    public String getManifestId() {
      return manifestId;
    }

    /** Template name (never null; falls back to the manifest id when unnamed). */
    public String getTemplateName() {
      return templateName;
    }

    /** Declared {@code templates[].assembler} value; null when absent. */
    public String getDeclaredAssembler() {
      return declaredAssembler;
    }

    /** Diagnosis verdict (never null). */
    public Status getStatus() {
      return status;
    }

    /** Registered extension path; null when {@link #getStatus()} is {@link Status#BLOCKED}. */
    public String getResolvedPath() {
      return resolvedPath;
    }

    /** Why the assembler is blocked; null when {@link #getStatus()} is {@link Status#OK}. */
    public String getReason() {
      return reason;
    }

    /** Operator fix action; null when {@link #getStatus()} is {@link Status#OK}. */
    public String getFixHint() {
      return fixHint;
    }

    /** Extra troubleshooting context (e.g. alias mapping); null when none. */
    public String getNote() {
      return note;
    }

    /** True when the assembler resolves on the modern load path. */
    public boolean isOk() {
      return status == Status.OK;
    }
  }

  /**
   * The single template entry a manifest's render pipeline consumes (issue #7049 / parent #2626).
   *
   * <p>Every render/install path ({@link PSPageXmlTemplateDefEmitter#emit}, {@link
   * PSPageXmlManifestReadback}, {@link PSPageXmlNativeInstall}) consumes only the first
   * {@code templates[]} entry. This value names that entry — the first non-null template,
   * mirroring {@code templates[0]} while skipping nulls like {@link #diagnoseAll} — with its
   * canonical assembler id ({@link PSPageXmlTemplateDefEmitter#canonicalAssemblerId}) and
   * resolved extension path, plus the sibling variant names the single-template path ignores so
   * multi-template manifests get a readable explanation instead of a silent drop.
   */
  public static final class RenderPath {
    private final String manifestId;
    private final String templateName;
    private final String declaredAssembler;
    private final String canonicalAssemblerId;
    private final String resolvedPath;
    private final List<String> ignoredVariantNames;

    private RenderPath(
        String manifestId,
        String templateName,
        String declaredAssembler,
        String canonicalAssemblerId,
        String resolvedPath,
        List<String> ignoredVariantNames) {
      this.manifestId = manifestId;
      this.templateName = templateName;
      this.declaredAssembler = declaredAssembler;
      this.canonicalAssemblerId = canonicalAssemblerId;
      this.resolvedPath = resolvedPath;
      this.ignoredVariantNames =
          Collections.unmodifiableList(new ArrayList<>(ignoredVariantNames));
    }

    /** Owning manifest id (never null). */
    public String getManifestId() {
      return manifestId;
    }

    /** Render entry template name; null when the manifest declares no template. */
    public String getTemplateName() {
      return templateName;
    }

    /** Declared {@code templates[].assembler} of the render entry; null when none. */
    public String getDeclaredAssembler() {
      return declaredAssembler;
    }

    /**
     * Canonical registered catalog id for the render entry (never blank when non-null); null
     * when the render entry is missing or declares a blank/unregistered assembler.
     */
    public String getCanonicalAssemblerId() {
      return canonicalAssemblerId;
    }

    /** Registered extension path the render entry resolves to; null when unresolvable. */
    public String getResolvedPath() {
      return resolvedPath;
    }

    /** Sibling variant names the single-template render path ignores (never null). */
    public List<String> getIgnoredVariantNames() {
      return ignoredVariantNames;
    }

    /** True when the manifest declares a render entry (first non-null template). */
    public boolean hasRenderTemplate() {
      return templateName != null;
    }

    /** True when the render entry resolves to a registered extension path. */
    public boolean isResolvable() {
      return hasRenderTemplate() && resolvedPath != null;
    }

    /** Number of sibling variants the single-template render path ignores. */
    public int getIgnoredVariantCount() {
      return ignoredVariantNames.size();
    }
  }

  /** Assembler-catalog diagnostics report for one or more manifests. */
  public static final class Report {
    private final List<CatalogEntry> catalog;
    private final List<Entry> entries;
    private final List<RenderPath> renderPaths;

    private Report(List<CatalogEntry> catalog, List<Entry> entries, List<RenderPath> renderPaths) {
      this.catalog = Collections.unmodifiableList(new ArrayList<>(catalog));
      this.entries = Collections.unmodifiableList(new ArrayList<>(entries));
      this.renderPaths = Collections.unmodifiableList(new ArrayList<>(renderPaths));
    }

    /** Registered catalog rows in registration order (never null, never empty). */
    public List<CatalogEntry> getCatalog() {
      return catalog;
    }

    /** Per-template diagnoses in manifest iteration order (never null). */
    public List<Entry> getEntries() {
      return entries;
    }

    /**
     * Single-template render paths, one per diagnosed manifest in iteration order (never null).
     * Each names the template entry the render pipeline consumes plus the sibling variants it
     * ignores.
     */
    public List<RenderPath> getRenderPaths() {
      return renderPaths;
    }

    /** Number of {@link Status#OK} entries. */
    public long getOkCount() {
      return entries.stream().filter(Entry::isOk).count();
    }

    /** Number of {@link Status#BLOCKED} entries. */
    public long getBlockedCount() {
      return entries.stream().filter(e -> !e.isOk()).count();
    }

    /** Number of diagnosed templates. */
    public int getTotalCount() {
      return entries.size();
    }

    /** Number of registered catalog rows no diagnosed template resolves to. */
    public long getStaleCount() {
      return collectStaleCatalogEntries().size();
    }

    /**
     * Garbage-collection scan (issue #6267 / parent #2626): registered catalog rows that no
     * diagnosed template resolves to anymore, in registration order.
     *
     * <p>A row counts as referenced when at least one {@link Status#OK} entry resolves to its
     * extension path, so case variants and the {@code xslAssembler → legacyAssembler} alias keep
     * their row. Blocked declarations reference nothing: an unregistered id or full path never
     * resolves to a catalog row. Reads nothing from disk and writes nothing.
     *
     * @return stale catalog rows in registration order (never null, possibly empty)
     */
    public List<CatalogEntry> collectStaleCatalogEntries() {
      Set<String> referenced = new HashSet<>();
      for (Entry entry : entries) {
        if (entry.isOk() && entry.getResolvedPath() != null) {
          referenced.add(entry.getResolvedPath());
        }
      }
      List<CatalogEntry> stale = new ArrayList<>();
      for (CatalogEntry row : catalog) {
        if (!referenced.contains(row.getExtensionPath())) {
          stale.add(row);
        }
      }
      return Collections.unmodifiableList(stale);
    }

    /**
     * Garbage-collection pass (issue #6267 / parent #2626): a new report with the stale rows
     * from {@link #collectStaleCatalogEntries()} removed from the catalog and the same
     * per-template verdicts, so operators can preview exactly which registrations would be
     * retired before touching product configuration. Reads nothing from disk and writes nothing.
     *
     * @return report with the pruned catalog and unchanged entries (never null)
     */
    public Report withoutStaleCatalogEntries() {
      Set<String> stalePaths = new HashSet<>();
      for (CatalogEntry stale : collectStaleCatalogEntries()) {
        stalePaths.add(stale.getExtensionPath());
      }
      List<CatalogEntry> kept = new ArrayList<>();
      for (CatalogEntry row : catalog) {
        if (!stalePaths.contains(row.getExtensionPath())) {
          kept.add(row);
        }
      }
      return new Report(kept, entries, renderPaths);
    }

    /** True when every diagnosed template resolves (vacuously true when empty). */
    public boolean isClean() {
      return getBlockedCount() == 0;
    }

    /**
     * Render the diagnostics panel as Markdown: registered catalog table, per-template verdict
     * table, and summary counts.
     *
     * @return Markdown report text with {@code \n} line separators (never null)
     */
    public String toMarkdown() {
      StringBuilder sb = new StringBuilder();
      sb.append("# Assembler catalog diagnostics\n\n");
      sb.append("Registered assemblers:\n\n");
      sb.append("| Manifest id | Extension path |\n");
      sb.append("|---|---|\n");
      for (CatalogEntry c : catalog) {
        sb.append("| ")
            .append(escapeCell(c.getId()))
            .append(" | ")
            .append(escapeCell(c.getExtensionPath()))
            .append(" |\n");
      }
      sb.append("\nTemplates: OK=")
          .append(getOkCount())
          .append(" BLOCKED=")
          .append(getBlockedCount())
          .append(" Total=")
          .append(getTotalCount())
          .append(" Stale=")
          .append(getStaleCount())
          .append("\n");
      List<CatalogEntry> stale = collectStaleCatalogEntries();
      sb.append("Stale catalog entries (unreferenced by any diagnosed template): ");
      if (stale.isEmpty()) {
        sb.append("—\n\n");
      } else {
        sb.append(
                stale.stream()
                    .map(c -> c.getId() + " -> " + c.getExtensionPath())
                    .collect(Collectors.joining(", ")))
            .append("\n\n");
      }
      sb.append("| Manifest | Template | Declared | Status | Resolves to | Detail |\n");
      sb.append("|---|---|---|---|---|---|\n");
      for (Entry e : entries) {
        sb.append("| ")
            .append(escapeCell(e.getManifestId()))
            .append(" | ")
            .append(escapeCell(e.getTemplateName()))
            .append(" | ")
            .append(escapeCell(e.getDeclaredAssembler() != null ? e.getDeclaredAssembler() : "—"))
            .append(" | ")
            .append(e.getStatus())
            .append(" | ")
            .append(escapeCell(e.getResolvedPath() != null ? e.getResolvedPath() : "—"))
            .append(" | ")
            .append(escapeCell(detailOf(e)))
            .append(" |\n");
      }
      sb.append("\nRender paths (single template consumed per manifest):\n\n");
      sb.append("| Manifest | Render template | Canonical assembler | Resolves to | Ignored variants |\n");
      sb.append("|---|---|---|---|---|\n");
      for (RenderPath r : renderPaths) {
        sb.append("| ")
            .append(escapeCell(r.getManifestId()))
            .append(" | ")
            .append(escapeCell(r.getTemplateName() != null ? r.getTemplateName() : "—"))
            .append(" | ")
            .append(
                escapeCell(
                    r.getCanonicalAssemblerId() != null ? r.getCanonicalAssemblerId() : "—"))
            .append(" | ")
            .append(escapeCell(r.getResolvedPath() != null ? r.getResolvedPath() : "—"))
            .append(" | ")
            .append(
                escapeCell(
                    r.getIgnoredVariantNames().isEmpty()
                        ? "—"
                        : String.join(", ", r.getIgnoredVariantNames())))
            .append(" |\n");
      }
      return sb.toString();
    }

    /**
     * Render the diagnostics panel as plain text (one line per template) for CLI use.
     *
     * @return plain-text report with {@code \n} line separators (never null)
     */
    public String toText() {
      StringBuilder sb = new StringBuilder();
      sb.append("Assembler catalog diagnostics: ok=")
          .append(getOkCount())
          .append(" blocked=")
          .append(getBlockedCount())
          .append(" total=")
          .append(getTotalCount())
          .append(" stale=")
          .append(getStaleCount())
          .append(" catalog=[")
          .append(String.join(",", catalogIds()))
          .append("]\n");
      for (Entry e : entries) {
        sb.append(e.getStatus())
            .append(" ")
            .append(e.getManifestId())
            .append(" / ")
            .append(e.getTemplateName());
        if (e.getResolvedPath() != null) {
          sb.append(" -> ").append(e.getResolvedPath());
        }
        if (e.getReason() != null) {
          sb.append(" : ").append(singleLine(e.getReason()));
        }
        sb.append("\n");
      }
      for (RenderPath r : renderPaths) {
        sb.append("RENDER ")
            .append(r.getManifestId())
            .append(" / ")
            .append(r.getTemplateName() != null ? r.getTemplateName() : "(no template)");
        if (r.getResolvedPath() != null) {
          sb.append(" -> ").append(r.getResolvedPath());
        }
        if (!r.getIgnoredVariantNames().isEmpty()) {
          sb.append(" (ignores: ")
              .append(String.join(", ", r.getIgnoredVariantNames()))
              .append(")");
        }
        sb.append("\n");
      }
      return sb.toString();
    }

    private List<String> catalogIds() {
      List<String> ids = new ArrayList<>(catalog.size());
      for (CatalogEntry c : catalog) {
        ids.add(c.getId());
      }
      return ids;
    }

    private static String detailOf(Entry e) {
      if (e.isOk()) {
        return e.getNote() != null ? e.getNote() : "—";
      }
      String detail = e.getReason() != null ? e.getReason() : "";
      if (e.getFixHint() != null && !e.getFixHint().isBlank()) {
        detail += (detail.isEmpty() ? "" : " ") + "Fix: " + e.getFixHint();
      }
      return detail.isEmpty() ? "—" : detail;
    }

    private static String escapeCell(String value) {
      return value.replace("|", "\\|").replace("\r", " ").replace("\n", " ");
    }

    private static String singleLine(String value) {
      return value.replace("\r", " ").replace("\n", " ").trim();
    }
  }

  private PSAssemblerCatalogDiagnostics() {
    // utility
  }

  /**
   * Read back the registered product assembler catalog (issue #5818 / parent #2626): short
   * manifest ids → product extension paths, in registration order.
   *
   * @return catalog rows (never null, never empty)
   */
  public static List<CatalogEntry> readCatalog() {
    List<CatalogEntry> catalog = new ArrayList<>();
    for (Map.Entry<String, String> registered :
        PSPageXmlTemplateDefEmitter.REGISTERED_ASSEMBLER_PATHS.entrySet()) {
      catalog.add(new CatalogEntry(registered.getKey(), registered.getValue()));
    }
    return Collections.unmodifiableList(catalog);
  }

  /**
   * Diagnose every {@code templates[]} entry of one modern manifest against the registered
   * catalog. Reads nothing from disk and writes nothing.
   *
   * @param manifest non-null modern page package manifest
   * @return diagnostics report (never null)
   */
  public static Report diagnose(PSComponentPackageManifest manifest) {
    Objects.requireNonNull(manifest, "manifest");
    return diagnoseAll(List.of(manifest));
  }

  /**
   * Diagnose every {@code templates[]} entry of each given modern manifest, in iteration order.
   * Null entries are skipped. Reads nothing from disk and writes nothing.
   *
   * @param manifests non-null manifests (null entries are skipped)
   * @return diagnostics report (never null)
   */
  public static Report diagnoseAll(Collection<PSComponentPackageManifest> manifests) {
    Objects.requireNonNull(manifests, "manifests");
    List<Entry> entries = new ArrayList<>();
    List<RenderPath> renderPaths = new ArrayList<>();
    for (PSComponentPackageManifest manifest : manifests) {
      if (manifest == null) {
        continue;
      }
      String manifestId = manifestIdOf(manifest);
      List<PSComponentPackageManifest.TemplateRef> templates = manifest.getTemplates();
      if (templates == null) {
        renderPaths.add(resolveRenderPath(manifestId, null));
        continue;
      }
      for (PSComponentPackageManifest.TemplateRef template : templates) {
        if (template == null) {
          continue;
        }
        entries.add(diagnoseOne(manifestId, template));
      }
      renderPaths.add(resolveRenderPath(manifestId, templates));
    }
    return new Report(readCatalog(), entries, renderPaths);
  }

  private static String manifestIdOf(PSComponentPackageManifest manifest) {
    String id = manifest.getId();
    return id != null && !id.isBlank() ? id : "(unnamed manifest)";
  }

  private static Entry diagnoseOne(
      String manifestId, PSComponentPackageManifest.TemplateRef template) {
    String name = template.getName();
    if (name == null || name.isBlank()) {
      name = manifestId;
    }
    String declared = template.getAssembler();
    if (declared == null || declared.isBlank()) {
      return blocked(
          manifestId,
          name,
          declared,
          "blank assembler: modern manifests must name a registered catalog id "
              + "(no pageAssembler default behind migrated definitions)",
          "Author a registered templates[].assembler id (e.g. \"htmlAssembler\").");
    }
    if (PSPageXmlTemplateDefEmitter.isRegisteredAssemblerId(declared)) {
      String resolved = PSPageXmlTemplateDefEmitter.resolveRegisteredAssemblerPath(declared);
      return new Entry(
          manifestId, name, declared, Status.OK, resolved, null, null, aliasNoteOf(declared));
    }
    if (declared.contains("/")) {
      return blocked(
          manifestId,
          name,
          declared,
          "unregistered extension path '"
              + declared.trim()
              + "': full paths pass through only on the legacy-upgrade emit path, "
              + "modern-only load resolves strictly through the registered catalog",
          "Author the short registered catalog id instead (e.g. \"htmlAssembler\").");
    }
    return blocked(
        manifestId,
        name,
        declared,
        "unregistered assembler '"
            + declared.trim()
            + "': modern page definitions must name a registered catalog id "
            + PSPageXmlTemplateDefEmitter.REGISTERED_ASSEMBLER_PATHS.keySet(),
        "Replace with a registered catalog id; custom ids load only on the "
            + "legacy-upgrade emit path.");
  }

  private static Entry blocked(
      String manifestId, String templateName, String declared, String reason, String fixHint) {
    return new Entry(
        manifestId, templateName, declared, Status.BLOCKED, null, reason, fixHint, null);
  }

  /**
   * Resolve the single-template render path for one manifest (issue #7049 / parent #2626): the
   * first non-null {@code templates[]} entry is the render entry every render/install path
   * consumes; later non-null entries are ignored sibling variants.
   *
   * @param manifestId owning manifest id (never null)
   * @param templates manifest templates; null or empty means no render entry
   * @return render path (never null)
   */
  private static RenderPath resolveRenderPath(
      String manifestId, List<PSComponentPackageManifest.TemplateRef> templates) {
    PSComponentPackageManifest.TemplateRef render = null;
    List<String> ignored = new ArrayList<>();
    if (templates != null) {
      for (PSComponentPackageManifest.TemplateRef template : templates) {
        if (template == null) {
          continue;
        }
        if (render == null) {
          render = template;
        } else {
          ignored.add(variantNameOf(template));
        }
      }
    }
    if (render == null) {
      return new RenderPath(manifestId, null, null, null, null, List.of());
    }
    String name = render.getName();
    if (name == null || name.isBlank()) {
      name = manifestId;
    }
    String declared = render.getAssembler();
    String canonical = PSPageXmlTemplateDefEmitter.canonicalAssemblerId(declared);
    String resolved =
        canonical != null
            ? PSPageXmlTemplateDefEmitter.REGISTERED_ASSEMBLER_PATHS.get(canonical)
            : null;
    return new RenderPath(manifestId, name, declared, canonical, resolved, ignored);
  }

  /** Display name for an ignored sibling variant (blank names fall back to a placeholder). */
  private static String variantNameOf(PSComponentPackageManifest.TemplateRef template) {
    String name = template.getName();
    return name != null && !name.isBlank() ? name : "(unnamed variant)";
  }

  /**
   * Troubleshooting note for alias spellings: the legacy {@code xslAssembler} alias resolves to
   * the {@code legacyAssembler} path. Null for direct registered ids.
   */
  private static String aliasNoteOf(String declared) {
    String leaf = declared.trim();
    int slash = leaf.lastIndexOf('/');
    if (slash >= 0) {
      leaf = leaf.substring(slash + 1);
    }
    if (!leaf.isBlank() && leaf.toLowerCase(Locale.ROOT).equals("xslassembler")) {
      return "xslAssembler is an alias for legacyAssembler";
    }
    return null;
  }
}
