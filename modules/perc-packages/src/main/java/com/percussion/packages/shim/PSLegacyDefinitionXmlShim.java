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

package com.percussion.packages.shim;

import com.percussion.packages.widgetxml.PSWidgetXmlDualShip;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * <strong>RETIRED</strong> time-boxed runtime compatibility shim for customer legacy Widget / Page /
 * Gadget definition XML (ADR-004, Phase 3 slice #2752, parent #2630 / epic #2626; retired Phase 5 /
 * #2632 / #2852 on owner directives <strong>2026-11-08</strong>: M1 PASS, M3 PASS upgrade-YES, M2
 * live-telemetry remainder QA-tracked in #5808 and no longer a start gate).
 *
 * <h2>Modern-only selection policy (hard order)</h2>
 *
 * <ol>
 *   <li><strong>Modern required</strong> — if {@value #MODERN_MANIFEST_FILE_NAME} is present for
 *       the package (or a matching definition id under a modern package root), use {@link
 *       PSDefinitionSourceKind#MODERN_COMPONENT_PACKAGE}.
 *   <li><strong>Otherwise fail clearly</strong> — when modern is absent, throw {@link
 *       PSDefinitionSourceNotFoundException} with an operator-facing message (do not silently
 *       invent a source, and do <strong>not</strong> load legacy definition XML). When legacy XML
 *       is present but retired, the message names the retired file and points at the Widget XML
 *       compiler upgrade path.
 * </ol>
 *
 * <p>The legacy Widget / Page / Gadget definition-XML fallback branches were removed here. Upgrade
 * inputs are unaffected: the {@code PSWidgetXml*} / page / gadget <strong>compilers</strong> still
 * read legacy XML as upgrade input and emit modern manifests. The Gadget WebUI dual-load ({@code
 * GadgetRegistry} modern catalog vs legacy registry XML) is a separate surface with its own
 * focused residual — it is not rewired by this change.
 *
 * <p>Dual-run policy and exit criteria: {@code
 * docs/ai-generated/tasks/template-assembler-normalization/dual-run-legacy-definition-xml-shim.md}.
 *
 * <h2>Runtime entry points</h2>
 *
 * <ul>
 *   <li>Widgets: {@code projects/sitemanage/.../dao/impl/PSWidgetDao} — modern-only selection
 *       (#2852). Repository {@code ${rxdeploydir}/rxconfig/Widgets} still carries the install
 *       Widget XML wire format; {@code widgetDao.modernPackageRoots} (or product defaults under
 *       {@code Packages/Modern} via {@link PSModernPackageRootDefaults}) feeds this API so a
 *       Component Package Manifest must be present for each id. Selection kinds are test-visible
 *       on the DAO.
 *   <li>Package source trees: {@code modules/perc-packages/.../Packages/<id>/} with {@code
 *       component-package.json} (modern, required).
 * </ul>
 *
 * <p>This class owns <strong>selection logic</strong>. Callers pass paths and act on the returned
 * {@link PSDefinitionSourceSelection}.
 *
 * <p>Filesystem APIs use {@link Path} / {@link Files} (portable Windows / Linux / macOS).
 */
public final class PSLegacyDefinitionXmlShim {

  /** Default modern Component Package Manifest file name (schema v1.0 / #2750). */
  public static final String MODERN_MANIFEST_FILE_NAME = "component-package.json";

  private PSLegacyDefinitionXmlShim() {
    // utility
  }

  /**
   * Pure modern-only selection from presence flags (unit-testable without I/O). Modern wins; any
   * legacy-only combination throws because the legacy fallback is retired (#2852).
   *
   * @param definitionId optional id for error messages (may be {@code null})
   * @param modernPresent modern {@code component-package.json} (or equivalent) present
   * @param legacyWidgetXmlPresent at least one Widget definition XML present (retired; presence
   *     only enriches the error message, never selects)
   * @param legacyPageXmlPresent at least one Page definition XML present (retired; same)
   * @param legacyGadgetXmlPresent at least one Gadget definition XML present (retired; same)
   * @return selected kind (always {@link PSDefinitionSourceKind#MODERN_COMPONENT_PACKAGE})
   * @throws PSDefinitionSourceNotFoundException if modern is absent (including when retired legacy
   *     XML is present)
   */
  public static PSDefinitionSourceSelection selectByPresence(
      String definitionId,
      boolean modernPresent,
      boolean legacyWidgetXmlPresent,
      boolean legacyPageXmlPresent,
      boolean legacyGadgetXmlPresent)
      throws PSDefinitionSourceNotFoundException {
    if (modernPresent) {
      return new PSDefinitionSourceSelection(
          PSDefinitionSourceKind.MODERN_COMPONENT_PACKAGE, definitionId, null);
    }
    throw retiredNotFound(definitionId, legacyProbeHint(
        legacyWidgetXmlPresent, legacyPageXmlPresent, legacyGadgetXmlPresent, null));
  }

  /**
   * Select source for a single product / customer <strong>package root</strong> directory.
   *
   * <p>Modern-only (#2852): returns {@link PSDefinitionSourceKind#MODERN_COMPONENT_PACKAGE} when
   * {@link #MODERN_MANIFEST_FILE_NAME} exists under the root <em>or</em> under a dual-ship {@code
   * widgets/&lt;stem&gt;/} child package (batch A / #2831). Otherwise throws — including when
   * retired legacy definition XML is present (named in the message for operator triage).
   *
   * @param packageRoot package source or install package directory (must not be null)
   * @return selection with primary path set to the manifest
   * @throws PSDefinitionSourceNotFoundException if no modern manifest is found
   * @throws IOException if the package root cannot be listed
   */
  public static PSDefinitionSourceSelection selectForPackageRoot(Path packageRoot)
      throws PSDefinitionSourceNotFoundException, IOException {
    Objects.requireNonNull(packageRoot, "packageRoot");
    Path root = packageRoot.toAbsolutePath().normalize();
    String packageName = root.getFileName() != null ? root.getFileName().toString() : root.toString();

    Path modernManifest = root.resolve(MODERN_MANIFEST_FILE_NAME);
    if (Files.isRegularFile(modernManifest)) {
      return new PSDefinitionSourceSelection(
          PSDefinitionSourceKind.MODERN_COMPONENT_PACKAGE, packageName, modernManifest);
    }

    // Dual-ship multi-widget authoring: widgets/<stem>/component-package.json (#2831)
    Optional<Path> modernWidget = findFirstModernWidgetManifest(root);
    if (modernWidget.isPresent()) {
      return new PSDefinitionSourceSelection(
          PSDefinitionSourceKind.MODERN_COMPONENT_PACKAGE, packageName, modernWidget.get());
    }

    throw retiredNotFound(
        packageName,
        "Package root: "
            + root
            + ". Expected modern "
            + MODERN_MANIFEST_FILE_NAME
            + " (at the root or under widgets/<stem>/)."
            + legacyProbeHintForRoot(root));
  }

  /**
   * Resolve a single <strong>definition id</strong> against modern package roots (the modern-only
   * runtime path, #2852).
   *
   * <p>Modern required: any {@code modernPackageRoot} that is a directory containing {@link
   * #MODERN_MANIFEST_FILE_NAME} and whose folder name equals {@code definitionId}, <em>or</em>
   * whose manifest sibling folder name matches, wins — as does a dual-ship {@code
   * widgets/<definitionId>/component-package.json} nested manifest (#2831). Otherwise throws
   * {@link PSDefinitionSourceNotFoundException} with a clear operator-facing message. The legacy
   * {@code legacyWidgetsDir} / {@code legacyPagesDir} / {@code legacyGadgetsDir} parameters are
   * retained for signature stability but are <strong>never loaded</strong>; when they contain the
   * retired XML for {@code definitionId} the error names it and points at the compiler upgrade
   * path instead of selecting it.
   *
   * @param definitionId widget / page / gadget definition id (non-empty)
   * @param modernPackageRoots zero or more package directories that may hold modern manifests
   * @param legacyWidgetsDir retired install {@code rxconfig/Widgets} probe dir (may be null;
   *     probed for diagnostics only, never loaded)
   * @param legacyPagesDir retired pages definition directory (may be null; diagnostics only)
   * @param legacyGadgetsDir retired gadgets definition directory (may be null; diagnostics only)
   * @return selection with primary path
   * @throws PSDefinitionSourceNotFoundException if no modern source exists
   */
  public static PSDefinitionSourceSelection selectDefinition(
      String definitionId,
      List<Path> modernPackageRoots,
      Path legacyWidgetsDir,
      Path legacyPagesDir,
      Path legacyGadgetsDir)
      throws PSDefinitionSourceNotFoundException {
    if (definitionId == null || definitionId.isBlank()) {
      throw new IllegalArgumentException("definitionId must be non-blank");
    }
    List<Path> roots =
        modernPackageRoots != null ? modernPackageRoots : List.of();

    Optional<Path> modern = findModernManifestForId(definitionId, roots);
    if (modern.isPresent()) {
      return new PSDefinitionSourceSelection(
          PSDefinitionSourceKind.MODERN_COMPONENT_PACKAGE, definitionId, modern.get());
    }

    Path retiredWidgetXml = findLegacyXmlFile(legacyWidgetsDir, definitionId);
    Path retiredPageXml = findLegacyXmlFile(legacyPagesDir, definitionId);
    Path retiredGadgetXml = findLegacyXmlFile(legacyGadgetsDir, definitionId);

    List<String> searched = new ArrayList<>();
    searched.add("modern package roots (" + roots.size() + ") for " + MODERN_MANIFEST_FILE_NAME);
    if (legacyWidgetsDir != null) {
      searched.add(legacyWidgetsDir.toAbsolutePath().normalize() + " / " + definitionId + ".xml");
    }
    if (legacyPagesDir != null) {
      searched.add(legacyPagesDir.toAbsolutePath().normalize() + " / " + definitionId + ".xml");
    }
    if (legacyGadgetsDir != null) {
      searched.add(legacyGadgetsDir.toAbsolutePath().normalize() + " / " + definitionId + ".xml");
    }
    StringBuilder detail = new StringBuilder();
    detail
        .append("No modern component package for id '")
        .append(definitionId)
        .append("'. Searched: ")
        .append(String.join("; ", searched))
        .append(".");
    Path retired =
        retiredWidgetXml != null
            ? retiredWidgetXml
            : (retiredPageXml != null ? retiredPageXml : retiredGadgetXml);
    if (retired != null) {
      detail
          .append(" Retired legacy definition XML is present at ")
          .append(retired)
          .append(" but is no longer loaded (#2852).");
    }
    detail.append(
        " Install a modern component package or convert legacy XML with the Widget XML compiler.");
    throw retiredNotFound(definitionId, detail.toString());
  }

  /**
   * Whether the given package root would use the legacy XML shim path.
   *
   * @deprecated Retired with the dual-run fallback (#2852): the legacy path is never selected, so
   *     this always returns {@code false}. Kept for metric / dashboard callers ({@code
   *     PSProductPackageRootSelectionEvidence}, product tests) until they drop the probe.
   * @param packageRoot package source or install package directory
   * @return {@code false} always
   * @throws IOException if the package root cannot be listed while probing
   */
  @Deprecated(since = "8.2", forRemoval = false)
  public static boolean wouldUseLegacyShim(Path packageRoot) throws IOException {
    return false;
  }

  private static boolean folderMatchesDefinition(String folderName, String definitionId) {
    // Product packages often use perc.widget.* folder names while XML file is percSimpleText.
    // Exact folder match is primary; callers that need id-inside-manifest matching will use #2750 IO.
    return folderName != null && folderName.equalsIgnoreCase(definitionId);
  }

  /**
   * First modern manifest for {@code definitionId} under {@code modernPackageRoots}: either {@code
   * <root>/component-package.json} on a matching root folder, or a dual-ship {@code
   * <root>/widgets/<definitionId>/component-package.json} nested manifest (#2831). Empty when no
   * modern source exists (callers fail clearly; retired legacy is never consulted).
   */
  private static Optional<Path> findModernManifestForId(
      String definitionId, List<Path> roots) {
    for (Path candidate : roots) {
      if (candidate == null) {
        continue;
      }
      Path normalized = candidate.toAbsolutePath().normalize();
      if (!Files.isDirectory(normalized)) {
        continue;
      }
      String folderName =
          normalized.getFileName() != null ? normalized.getFileName().toString() : "";
      Path manifest = normalized.resolve(MODERN_MANIFEST_FILE_NAME);
      if (Files.isRegularFile(manifest)
          && (definitionId.equals(folderName)
              || folderMatchesDefinition(folderName, definitionId))) {
        return Optional.of(manifest);
      }
      // Dual-ship multi-widget package root: widgets/<definitionId>/component-package.json (#2831)
      Path nested =
          normalized
              .resolve(PSWidgetXmlDualShip.WIDGETS_DIR_NAME)
              .resolve(definitionId)
              .resolve(MODERN_MANIFEST_FILE_NAME);
      if (Files.isRegularFile(nested)) {
        return Optional.of(nested);
      }
    }
    return Optional.empty();
  }

  /**
   * First modern dual-ship widget manifest under {@code packageRoot/widgets/&lt;stem&gt;/component-package.json}
   * (stable order by folder name). Empty when no modern widget authoring tree is present.
   */
  static Optional<Path> findFirstModernWidgetManifest(Path packageRoot) throws IOException {
    Path widgets = packageRoot.resolve(PSWidgetXmlDualShip.WIDGETS_DIR_NAME);
    if (!Files.isDirectory(widgets)) {
      return Optional.empty();
    }
    List<Path> manifests = new ArrayList<>();
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(widgets)) {
      for (Path child : stream) {
        if (!Files.isDirectory(child)) {
          continue;
        }
        Path manifest = child.resolve(MODERN_MANIFEST_FILE_NAME);
        if (Files.isRegularFile(manifest)) {
          manifests.add(manifest);
        }
      }
    }
    manifests.sort(
        (a, b) -> {
          String an =
              a.getParent() != null && a.getParent().getFileName() != null
                  ? a.getParent().getFileName().toString()
                  : a.toString();
          String bn =
              b.getParent() != null && b.getParent().getFileName() != null
                  ? b.getParent().getFileName().toString()
                  : b.toString();
          // Locale.ROOT for stable order with PSWidgetXmlDualShip.listModernWidgetDirs
          return an.toLowerCase(Locale.ROOT).compareTo(bn.toLowerCase(Locale.ROOT));
        });
    return manifests.isEmpty() ? Optional.empty() : Optional.of(manifests.get(0));
  }

  /**
   * Diagnostic probe for a retired legacy {@code <definitionId>.xml} file. Never used to load
   * content — only to name the retired file in modern-only failure messages (#2852).
   */
  private static Path findLegacyXmlFile(Path dir, String definitionId) {
    if (dir == null || !Files.isDirectory(dir)) {
      return null;
    }
    Path file = dir.resolve(definitionId + ".xml");
    return Files.isRegularFile(file) ? file : null;
  }

  /**
   * Diagnostic probe for any retired legacy definition XML under {@code dir}. Never used to load
   * content — only to enrich modern-only failure messages (#2852).
   */
  private static Optional<Path> findFirstRetiredXml(Path dir) throws IOException {
    if (dir == null || !Files.isDirectory(dir)) {
      return Optional.empty();
    }
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.xml")) {
      for (Path p : stream) {
        if (Files.isRegularFile(p)) {
          return Optional.of(p);
        }
      }
    }
    return Optional.empty();
  }

  private static Path resolvePackageWidgetsDir(Path packageRoot) {
    Path staging = packageRoot.resolve("sys__UserDependency--rxconfig").resolve("Widgets");
    if (Files.isDirectory(staging)) {
      return staging;
    }
    return packageRoot.resolve("rxconfig").resolve("Widgets");
  }

  /**
   * Diagnostic hint naming retired legacy material under a package root (never loaded). Empty
   * string when no retired XML is present.
   */
  private static String legacyProbeHintForRoot(Path root) {
    try {
      Optional<Path> widgetXml = findFirstRetiredXml(resolvePackageWidgetsDir(root));
      if (widgetXml.isPresent()) {
        return " Retired legacy Widget XML is present at "
            + widgetXml.get()
            + " but is no longer loaded (#2852). Convert it with the Widget XML compiler.";
      }
      Optional<Path> pageXml = findFirstRetiredXml(root.resolve("rxconfig").resolve("Pages"));
      if (pageXml.isEmpty()) {
        pageXml =
            findFirstRetiredXml(root.resolve("sys__UserDependency--rxconfig").resolve("Pages"));
      }
      if (pageXml.isPresent()) {
        return " Retired legacy Page XML is present at "
            + pageXml.get()
            + " but is no longer loaded (#2852).";
      }
      Optional<Path> gadgetXml =
          findFirstRetiredXml(root.resolve("rxconfig").resolve("Gadgets"));
      if (gadgetXml.isEmpty()) {
        gadgetXml =
            findFirstRetiredXml(
                root.resolve("sys__UserDependency--rxconfig").resolve("Gadgets"));
      }
      if (gadgetXml.isPresent()) {
        return " Retired legacy Gadget XML is present at "
            + gadgetXml.get()
            + " but is no longer loaded (#2852).";
      }
    } catch (IOException e) {
      // Diagnostics must not mask the modern-only failure.
    }
    return "";
  }

  private static String legacyProbeHint(
      boolean legacyWidgetXmlPresent,
      boolean legacyPageXmlPresent,
      boolean legacyGadgetXmlPresent,
      Path retiredPath) {
    StringBuilder hint = new StringBuilder();
    if (legacyWidgetXmlPresent || legacyPageXmlPresent || legacyGadgetXmlPresent) {
      hint.append(" Retired legacy definition XML is present (");
      String sep = "";
      if (legacyWidgetXmlPresent) {
        hint.append(sep).append("Widget");
        sep = "/";
      }
      if (legacyPageXmlPresent) {
        hint.append(sep).append("Page");
        sep = "/";
      }
      if (legacyGadgetXmlPresent) {
        hint.append(sep).append("Gadget");
      }
      hint.append(") but is no longer loaded (#2852).");
    }
    if (retiredPath != null) {
      hint.append(" Retired file: ").append(retiredPath).append(".");
    }
    if (hint.length() == 0) {
      return " Install a modern component package or convert legacy XML"
          + " with the Widget XML compiler.";
    }
    hint.append(" Convert it with the Widget XML compiler or install a modern component package.");
    return hint.toString();
  }

  private static PSDefinitionSourceNotFoundException retiredNotFound(
      String definitionId, String detail) {
    String base =
        "No modern definition source found"
            + (definitionId != null && !definitionId.isBlank() ? " for '" + definitionId + "'" : "")
            + ": modern component package ("
            + MODERN_MANIFEST_FILE_NAME
            + ") is required; the legacy Widget/Page/Gadget definition-XML fallback is retired"
            + " (#2852).";
    String message = detail == null || detail.isBlank() ? base : base + " " + detail;
    return new PSDefinitionSourceNotFoundException(definitionId, message);
  }
}
