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
 * Time-boxed <strong>runtime compatibility shim</strong> for customer legacy Widget / Page /
 * Gadget definition XML when no modern Component Package Manifest is present (ADR-004, Phase 3
 * slice #2752, parent #2630 / epic #2626).
 *
 * <h2>Selection policy (hard order)</h2>
 *
 * <ol>
 *   <li><strong>Modern preferred</strong> — if {@value #MODERN_MANIFEST_FILE_NAME} is present for
 *       the package (or a matching definition id under a modern package root), use {@link
 *       PSDefinitionSourceKind#MODERN_COMPONENT_PACKAGE}.
 *   <li><strong>Legacy XML fallback</strong> — when modern is absent, load legacy Widget / Page /
 *       Gadget definition XML if present (same paths product uses today).
 *   <li><strong>Neither</strong> — throw {@link PSDefinitionSourceNotFoundException} with a clear
 *       operator-facing message (do not silently invent a source).
 * </ol>
 *
 * <h2>Time box / deprecation</h2>
 *
 * <p>This shim is <strong>transitional</strong>. Product packages must not depend on legacy
 * definition XML as the authoring source of truth. Operators should convert customer XML via the
 * Widget XML compiler (#2751) and remove XML loads once conversion metrics allow (Phase 5 / #2632).
 * Dual-run policy and exit criteria: {@code
 * docs/ai-generated/tasks/template-assembler-normalization/dual-run-legacy-definition-xml-shim.md}.
 *
 * <h2>Runtime entry points</h2>
 *
 * <ul>
 *   <li>Widgets: {@code projects/sitemanage/.../dao/impl/PSWidgetDao} — production dual-run wire
 *       (#3024). Repository {@code ${rxdeploydir}/rxconfig/Widgets} loads install Widget XML;
 *       optional {@code widgetDao.modernPackageRoots} feeds this API so modern manifests win when
 *       present. Selection kinds are test-visible on the DAO. Do <strong>not</strong> delete this
 *       shim (#2852).
 *   <li>Package source trees: {@code modules/perc-packages/.../Packages/<id>/} with either {@code
 *       component-package.json} (modern) or {@code sys__UserDependency--rxconfig/Widgets/*.xml}
 *       (legacy).
 *   <li>Gadget registry / page meta: see dual-run operator doc for remaining load surfaces.
 * </ul>
 *
 * <p>This class owns <strong>selection logic</strong>. Callers pass paths and act on the returned
 * {@link PSDefinitionSourceSelection}. Content loaders may still read install Widget XML while
 * reporting modern as the preferred dual-run kind when a Component Package Manifest is present.
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
   * Pure selection from presence flags (unit-testable without I/O). Preference order: modern →
   * widget XML → page XML → gadget XML. If nothing is present, throws.
   *
   * @param definitionId optional id for error messages (may be {@code null})
   * @param modernPresent modern {@code component-package.json} (or equivalent) present
   * @param legacyWidgetXmlPresent at least one Widget definition XML present
   * @param legacyPageXmlPresent at least one Page definition XML present
   * @param legacyGadgetXmlPresent at least one Gadget definition XML present
   * @return selected kind (never null)
   * @throws PSDefinitionSourceNotFoundException if no source is present
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
    if (legacyWidgetXmlPresent) {
      return new PSDefinitionSourceSelection(
          PSDefinitionSourceKind.LEGACY_WIDGET_XML, definitionId, null);
    }
    if (legacyPageXmlPresent) {
      return new PSDefinitionSourceSelection(
          PSDefinitionSourceKind.LEGACY_PAGE_XML, definitionId, null);
    }
    if (legacyGadgetXmlPresent) {
      return new PSDefinitionSourceSelection(
          PSDefinitionSourceKind.LEGACY_GADGET_XML, definitionId, null);
    }
    throw notFound(definitionId, null);
  }

  /**
   * Select source for a single product / customer <strong>package root</strong> directory.
   *
   * <p>Modern wins if {@link #MODERN_MANIFEST_FILE_NAME} exists under the root <em>or</em> under a
   * dual-ship {@code widgets/&lt;stem&gt;/} child package (batch A / #2831). Otherwise legacy Widget
   * XML under package staging or install-relative Widgets is preferred over page/gadget markers when
   * both exist in the same tree (widgets are the primary dual-run surface).
   *
   * @param packageRoot package source or install package directory (must not be null)
   * @return selection with primary path set to the manifest or first legacy XML found
   * @throws PSDefinitionSourceNotFoundException if neither modern nor legacy definition material is
   *     found
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

    Optional<Path> widgetXml = findFirstXml(resolvePackageWidgetsDir(root));
    if (widgetXml.isPresent()) {
      return new PSDefinitionSourceSelection(
          PSDefinitionSourceKind.LEGACY_WIDGET_XML, packageName, widgetXml.get());
    }

    Optional<Path> pageXml = findFirstXml(root.resolve("rxconfig").resolve("Pages"));
    if (pageXml.isEmpty()) {
      pageXml = findFirstXml(root.resolve("sys__UserDependency--rxconfig").resolve("Pages"));
    }
    if (pageXml.isPresent()) {
      return new PSDefinitionSourceSelection(
          PSDefinitionSourceKind.LEGACY_PAGE_XML, packageName, pageXml.get());
    }

    Optional<Path> gadgetXml = findFirstXml(root.resolve("rxconfig").resolve("Gadgets"));
    if (gadgetXml.isEmpty()) {
      gadgetXml = findFirstXml(root.resolve("sys__UserDependency--rxconfig").resolve("Gadgets"));
    }
    if (gadgetXml.isPresent()) {
      return new PSDefinitionSourceSelection(
          PSDefinitionSourceKind.LEGACY_GADGET_XML, packageName, gadgetXml.get());
    }

    throw notFound(
        packageName,
        "Package root: "
            + root
            + ". Expected either "
            + MODERN_MANIFEST_FILE_NAME
            + " (modern) or legacy definition XML under rxconfig/Widgets|Pages|Gadgets (or package staging sys__UserDependency--rxconfig/...).");
  }

  /**
   * Resolve a single <strong>definition id</strong> against modern package roots and legacy install
   * directories (the dual-run runtime path).
   *
   * <p>Modern preferred: any {@code modernPackageRoot} that is a directory containing {@link
   * #MODERN_MANIFEST_FILE_NAME} and whose folder name equals {@code definitionId}, <em>or</em>
   * whose manifest sibling folder name matches, wins. Legacy fallback: {@code
   * legacyWidgetsDir/definitionId.xml}, then pages, then gadgets.
   *
   * @param definitionId widget / page / gadget definition id (non-empty)
   * @param modernPackageRoots zero or more package directories that may hold modern manifests
   * @param legacyWidgetsDir install {@code rxconfig/Widgets} (may be null)
   * @param legacyPagesDir optional pages definition directory (may be null)
   * @param legacyGadgetsDir optional gadgets definition directory (may be null)
   * @return selection with primary path
   * @throws PSDefinitionSourceNotFoundException if neither modern nor legacy source exists
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
          && (definitionId.equals(folderName) || folderMatchesDefinition(folderName, definitionId))) {
        return new PSDefinitionSourceSelection(
            PSDefinitionSourceKind.MODERN_COMPONENT_PACKAGE, definitionId, manifest);
      }
      // Dual-ship multi-widget package root: widgets/<definitionId>/component-package.json (#2831)
      Path nested =
          normalized
              .resolve(PSWidgetXmlDualShip.WIDGETS_DIR_NAME)
              .resolve(definitionId)
              .resolve(MODERN_MANIFEST_FILE_NAME);
      if (Files.isRegularFile(nested)) {
        return new PSDefinitionSourceSelection(
            PSDefinitionSourceKind.MODERN_COMPONENT_PACKAGE, definitionId, nested);
      }
    }

    Path widgetXml = resolveLegacyXmlFile(legacyWidgetsDir, definitionId);
    if (widgetXml != null) {
      return new PSDefinitionSourceSelection(
          PSDefinitionSourceKind.LEGACY_WIDGET_XML, definitionId, widgetXml);
    }

    Path pageXml = resolveLegacyXmlFile(legacyPagesDir, definitionId);
    if (pageXml != null) {
      return new PSDefinitionSourceSelection(
          PSDefinitionSourceKind.LEGACY_PAGE_XML, definitionId, pageXml);
    }

    Path gadgetXml = resolveLegacyXmlFile(legacyGadgetsDir, definitionId);
    if (gadgetXml != null) {
      return new PSDefinitionSourceSelection(
          PSDefinitionSourceKind.LEGACY_GADGET_XML, definitionId, gadgetXml);
    }

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
    throw notFound(
        definitionId,
        "No modern component package and no legacy definition XML for id '"
            + definitionId
            + "'. Searched: "
            + String.join("; ", searched)
            + ". Convert legacy XML with the Widget XML compiler or install a modern component package.");
  }

  /**
   * Whether the given package root would use the legacy XML shim path (modern absent, legacy
   * present). Useful for operator metrics / dual-run dashboards.
   *
   * @param packageRoot package source or install package directory
   * @return {@code true} if selection resolves to a legacy XML kind; {@code false} if modern is
   *     chosen or nothing is found
   * @throws IOException if the package root cannot be listed while probing
   */
  public static boolean wouldUseLegacyShim(Path packageRoot) throws IOException {
    try {
      return selectForPackageRoot(packageRoot).isLegacyXml();
    } catch (PSDefinitionSourceNotFoundException e) {
      return false;
    }
  }

  private static boolean folderMatchesDefinition(String folderName, String definitionId) {
    // Product packages often use perc.widget.* folder names while XML file is percSimpleText.
    // Exact folder match is primary; callers that need id-inside-manifest matching will use #2750 IO.
    return folderName != null && folderName.equalsIgnoreCase(definitionId);
  }

  private static Path resolvePackageWidgetsDir(Path packageRoot) {
    Path staging = packageRoot.resolve("sys__UserDependency--rxconfig").resolve("Widgets");
    if (Files.isDirectory(staging)) {
      return staging;
    }
    return packageRoot.resolve("rxconfig").resolve("Widgets");
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

  private static Path resolveLegacyXmlFile(Path dir, String definitionId) {
    if (dir == null || !Files.isDirectory(dir)) {
      return null;
    }
    Path file = dir.resolve(definitionId + ".xml");
    return Files.isRegularFile(file) ? file : null;
  }

  private static Optional<Path> findFirstXml(Path dir) throws IOException {
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

  private static PSDefinitionSourceNotFoundException notFound(String definitionId, String detail) {
    String base =
        "No definition source found"
            + (definitionId != null && !definitionId.isBlank() ? " for '" + definitionId + "'" : "")
            + ": neither modern component package ("
            + MODERN_MANIFEST_FILE_NAME
            + ") nor legacy Widget/Page/Gadget definition XML is present.";
    String message = detail == null || detail.isBlank() ? base : base + " " + detail;
    return new PSDefinitionSourceNotFoundException(definitionId, message);
  }
}
