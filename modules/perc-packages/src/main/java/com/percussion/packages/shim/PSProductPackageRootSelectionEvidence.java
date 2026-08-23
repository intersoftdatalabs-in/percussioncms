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

import com.percussion.packages.widgetxml.PSWidgetDefinitionXmlInventory;
import com.percussion.packages.widgetxml.PSWidgetXmlDualShip;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * CI-assertable <strong>M2 product / H2 zero-legacy-selection evidence</strong> (issues #3583 /
 * #3738 / parent #2630).
 *
 * <p>Walks product package roots (or an H2-style {@code Packages/Modern} install tree) and records
 * {@link PSLegacyDefinitionXmlShim} selection. Non-waived widget packages must report {@code
 * wouldUseLegacyShim == false} / {@link PSDefinitionSourceKind#MODERN_COMPONENT_PACKAGE}. Unexpected
 * {@code LEGACY_*} on a non-waived product widget fails the harness. {@code perc.Test} may still
 * select {@link PSDefinitionSourceKind#LEGACY_WIDGET_XML} while it remains on the Widget G4 waive
 * list; after that waiver is dropped (#3736) it must be modern-first like other product packages.
 * The shim itself is <strong>kept</strong> (#2852).
 *
 * <p>This is <em>not</em> M2 PASS overall: customer-only XML and the open upgrade window (M3)
 * still require the dual-run fallback. Do not treat a green scan as removal-ready.
 *
 * <p>Uses {@link Path} / {@link Files} only (Windows / Linux / macOS).
 */
public final class PSProductPackageRootSelectionEvidence {

  /**
   * Known non-waived product widget stems (batches A+B+C). Used as a floor so a missing modern
   * root cannot silently drop a product widget from the scan.
   */
  public static final List<String> KNOWN_PRODUCT_WIDGET_STEMS;

  static {
    List<String> stems = new ArrayList<>();
    stems.addAll(PSWidgetXmlDualShip.BATCH_A_WIDGET_STEMS);
    stems.addAll(PSWidgetXmlDualShip.BATCH_B_WIDGET_STEMS);
    stems.addAll(PSWidgetXmlDualShip.BATCH_C_WIDGET_STEMS);
    KNOWN_PRODUCT_WIDGET_STEMS = List.copyOf(stems);
  }

  /** perc.Test package directory name under {@code Packages/}. */
  public static final String PERC_TEST_PACKAGE_DIR = "perc.Test";

  /** perc.Test widget definition id / modern stem. */
  public static final String PERC_TEST_WIDGET_STEM = "PSWidget_TestProperties";

  private PSProductPackageRootSelectionEvidence() {
    // utility
  }

  /**
   * One widget-bearing package root and its dual-run selection.
   *
   * @param packageDirName immediate child name under the Packages (or Modern) root
   * @param packageRoot absolute normalized package directory
   * @param kind selected kind, or {@code null} when neither modern nor legacy material is found
   * @param wouldUseLegacyShim {@code true} when selection is a legacy XML kind
   * @param waived whether {@code packageDirName} is on the explicit waiver list
   */
  public record RootFinding(
      String packageDirName,
      Path packageRoot,
      PSDefinitionSourceKind kind,
      boolean wouldUseLegacyShim,
      boolean waived) {

    /**
     * @return true when a non-waived root selected a legacy XML kind
     */
    public boolean isUnexpectedLegacy() {
      return !waived && wouldUseLegacyShim;
    }
  }

  /**
   * One definition-id selection against modern roots + optional install Widgets XML.
   *
   * @param definitionId widget definition id
   * @param kind selected kind, or {@code null} when neither source exists
   * @param waived whether this id is from a waived package ({@code perc.Test} until #3736)
   */
  public record DefinitionFinding(
      String definitionId, PSDefinitionSourceKind kind, boolean waived) {

    /**
     * @return true when a non-waived id selected a legacy XML kind
     */
    public boolean isUnexpectedLegacy() {
      return !waived && kind != null && kind != PSDefinitionSourceKind.MODERN_COMPONENT_PACKAGE;
    }
  }

  /**
   * Combined package-root + definition-id evidence report.
   *
   * @param roots widget-bearing package roots (sorted)
   * @param definitions per-id selections (sorted); may be empty when only roots were scanned
   */
  public record Report(List<RootFinding> roots, List<DefinitionFinding> definitions) {

    /**
     * @return non-waived roots that selected legacy XML
     */
    public List<RootFinding> unexpectedLegacyRoots() {
      return roots.stream().filter(RootFinding::isUnexpectedLegacy).collect(Collectors.toList());
    }

    /**
     * @return non-waived definition ids that selected legacy XML
     */
    public List<DefinitionFinding> unexpectedLegacyDefinitions() {
      return definitions.stream()
          .filter(DefinitionFinding::isUnexpectedLegacy)
          .collect(Collectors.toList());
    }

    /**
     * @return true when no unexpected (non-waived) legacy selection is present
     */
    public boolean isClean() {
      return unexpectedLegacyRoots().isEmpty() && unexpectedLegacyDefinitions().isEmpty();
    }

    /**
     * @return count of roots whose selection is modern
     */
    public long modernRootCount() {
      return roots.stream()
          .filter(r -> r.kind() == PSDefinitionSourceKind.MODERN_COMPONENT_PACKAGE)
          .count();
    }

    /**
     * @return count of waived roots that still select legacy XML (expected: {@code perc.Test} until #3736; {@code 0} after)
     */
    public long waivedLegacyRootCount() {
      return roots.stream().filter(r -> r.waived() && r.wouldUseLegacyShim()).count();
    }
  }

  /**
   * Scan immediate package directories under {@code packagesRoot} that carry widget definition
   * material (modern {@code widgets/} or install/staging Widget XML).
   *
   * @param packagesRoot product {@code Packages/} tree or H2 {@code Packages/Modern}
   * @return report of root findings (definitions empty)
   * @throws IOException on I/O failure
   * @throws IllegalArgumentException if {@code packagesRoot} is null or not a directory
   */
  public static Report scanWidgetPackageRoots(Path packagesRoot) throws IOException {
    Objects.requireNonNull(packagesRoot, "packagesRoot");
    if (!Files.isDirectory(packagesRoot)) {
      throw new IllegalArgumentException("Packages root is not a directory: " + packagesRoot);
    }

    List<RootFinding> roots = new ArrayList<>();
    try (DirectoryStream<Path> packages = Files.newDirectoryStream(packagesRoot)) {
      for (Path packageDir : packages) {
        if (!Files.isDirectory(packageDir)) {
          continue;
        }
        Path namePath = packageDir.getFileName();
        if (namePath == null) {
          continue;
        }
        if (!hasWidgetDefinitionMaterial(packageDir)) {
          continue;
        }
        roots.add(classifyPackageRoot(packageDir, namePath.toString()));
      }
    }
    sortRoots(roots);
    return new Report(List.copyOf(roots), List.of());
  }

  /**
   * Scan already-discovered modern install roots (H2 {@code Packages/Modern} children).
   *
   * @param modernRoots package-root directories from {@link
   *     PSModernPackageRootDefaults#discoverPackageRoots(Path)}
   * @return report of root findings
   * @throws IOException on I/O failure
   */
  public static Report scanDiscoveredModernRoots(Collection<Path> modernRoots) throws IOException {
    Objects.requireNonNull(modernRoots, "modernRoots");
    List<RootFinding> roots = new ArrayList<>();
    for (Path root : modernRoots) {
      if (root == null || !Files.isDirectory(root)) {
        continue;
      }
      Path namePath = root.getFileName();
      String name = namePath != null ? namePath.toString() : root.toString();
      roots.add(classifyPackageRoot(root, name));
    }
    sortRoots(roots);
    return new Report(List.copyOf(roots), List.of());
  }

  /**
   * Dual-run {@link PSLegacyDefinitionXmlShim#selectDefinition} for each id against modern roots
   * and optional install Widgets XML (H2/runtime shape).
   *
   * @param modernPackageRoots modern package roots (may be empty)
   * @param legacyWidgetsDir install {@code rxconfig/Widgets} (may be {@code null})
   * @param definitionIds widget ids to classify
   * @param waivedDefinitionIds ids allowed to select {@code LEGACY_*} (typically {@code
   *     PSWidget_TestProperties})
   * @return report of definition findings (roots empty)
   */
  public static Report scanDefinitionSelection(
      List<Path> modernPackageRoots,
      Path legacyWidgetsDir,
      Collection<String> definitionIds,
      Set<String> waivedDefinitionIds) {
    Objects.requireNonNull(definitionIds, "definitionIds");
    Set<String> waived =
        waivedDefinitionIds != null ? waivedDefinitionIds : Set.of();
    List<Path> roots =
        modernPackageRoots != null ? modernPackageRoots : List.of();
    List<DefinitionFinding> findings = new ArrayList<>();
    for (String id : definitionIds) {
      if (id == null || id.isBlank()) {
        continue;
      }
      boolean isWaived = waived.contains(id);
      try {
        PSDefinitionSourceSelection sel =
            PSLegacyDefinitionXmlShim.selectDefinition(
                id, roots, legacyWidgetsDir, null, null);
        findings.add(new DefinitionFinding(id, sel.getKind(), isWaived));
      } catch (PSDefinitionSourceNotFoundException e) {
        findings.add(new DefinitionFinding(id, null, isWaived));
      }
    }
    findings.sort(
        Comparator.comparing(f -> f.definitionId().toLowerCase(Locale.ROOT)));
    return new Report(List.of(), List.copyOf(findings));
  }

  /**
   * Lists modern widget stems under each package root ({@code widgets/&lt;stem&gt;/}).
   *
   * @param packageRoots package directories
   * @return stable unique stem names
   * @throws IOException if a widgets directory cannot be listed
   */
  public static List<String> listModernWidgetDefinitionIds(Collection<Path> packageRoots)
      throws IOException {
    Objects.requireNonNull(packageRoots, "packageRoots");
    Set<String> ids = new LinkedHashSet<>();
    for (Path root : packageRoots) {
      if (root == null || !Files.isDirectory(root)) {
        continue;
      }
      for (Path widgetDir : PSWidgetXmlDualShip.listModernWidgetDirs(root)) {
        Path name = widgetDir.getFileName();
        if (name != null && !name.toString().isBlank()) {
          ids.add(name.toString());
        }
      }
    }
    List<String> sorted = new ArrayList<>(ids);
    sorted.sort(String.CASE_INSENSITIVE_ORDER);
    return List.copyOf(sorted);
  }

  /**
   * Lists modern widget stems from every widget-bearing package under {@code packagesRoot}.
   *
   * @param packagesRoot product {@code Packages/} or {@code Packages/Modern}
   * @return stable unique stem names
   * @throws IOException on I/O failure
   */
  public static List<String> listModernWidgetDefinitionIds(Path packagesRoot) throws IOException {
    Objects.requireNonNull(packagesRoot, "packagesRoot");
    if (!Files.isDirectory(packagesRoot)) {
      return List.of();
    }
    List<Path> dirs = new ArrayList<>();
    try (DirectoryStream<Path> packages = Files.newDirectoryStream(packagesRoot)) {
      for (Path packageDir : packages) {
        if (Files.isDirectory(packageDir)) {
          dirs.add(packageDir);
        }
      }
    }
    return listModernWidgetDefinitionIds(dirs);
  }

  /**
   * Fail-fast: non-waived widget package roots must not select {@code LEGACY_*}.
   *
   * @param packagesRoot product Packages tree or H2 Modern dir
   * @throws IOException on I/O failure
   * @throws IllegalStateException when unexpected legacy selection is present
   */
  public static void assertNoUnexpectedLegacyOnWidgetPackageRoots(Path packagesRoot)
      throws IOException {
    Report report = scanWidgetPackageRoots(packagesRoot);
    throwIfUnclean(report, "widget package roots under " + packagesRoot);
  }

  /**
   * Fail-fast: discovered H2/product modern roots must all be modern-first.
   *
   * @param modernRoots discovered package roots
   * @throws IOException on I/O failure
   * @throws IllegalStateException when unexpected legacy selection is present
   */
  public static void assertNoUnexpectedLegacyOnDiscoveredModernRoots(Collection<Path> modernRoots)
      throws IOException {
    Report report = scanDiscoveredModernRoots(modernRoots);
    throwIfUnclean(report, "discovered modern package roots");
  }

  /**
   * Fail-fast: non-waived definition ids must not select {@code LEGACY_*}.
   *
   * @param modernPackageRoots modern roots
   * @param legacyWidgetsDir optional install Widgets dir
   * @param definitionIds ids to classify
   * @param waivedDefinitionIds waived ids
   * @throws IllegalStateException when unexpected legacy selection is present
   */
  public static void assertNoUnexpectedLegacyDefinitions(
      List<Path> modernPackageRoots,
      Path legacyWidgetsDir,
      Collection<String> definitionIds,
      Set<String> waivedDefinitionIds) {
    Report report =
        scanDefinitionSelection(
            modernPackageRoots, legacyWidgetsDir, definitionIds, waivedDefinitionIds);
    throwIfUnclean(report, "definition-id dual-run selection");
  }

  /**
   * Whether {@code packageDirName} is on the explicit M1/M2 Widget waiver list ({@code perc.Test}
   * only until #3736; empty after).
   *
   * @param packageDirName package folder name
   * @return true if waived
   */
  public static boolean isWaivedPackage(String packageDirName) {
    return PSWidgetDefinitionXmlInventory.isWaivedPackage(packageDirName);
  }

  /**
   * Widget G4/M2 waive list is allowed to be <em>empty</em> (after #3736 perc.Test modern) or
   * exactly {@code perc.Test} (current {@code main} until that slice merges). Any other package on
   * the list is a regression (#3738).
   *
   * @throws IllegalStateException when the waive list is neither empty nor {@code perc.Test} only
   */
  public static void assertWidgetWaiverPolicy() {
    assertWidgetWaiverPolicy(PSWidgetDefinitionXmlInventory.WAIVED_PACKAGE_DIRS);
  }

  /**
   * Same as {@link #assertWidgetWaiverPolicy()} against an explicit set (tests / probes).
   *
   * @param waived candidate waive list (may be {@code null}, treated as empty)
   * @throws IllegalStateException when the waive list is neither empty nor {@code perc.Test} only
   */
  public static void assertWidgetWaiverPolicy(Set<String> waived) {
    Set<String> dirs = waived == null ? Set.of() : waived;
    if (dirs.isEmpty()) {
      return;
    }
    if (dirs.size() == 1 && dirs.contains(PERC_TEST_PACKAGE_DIR)) {
      return;
    }
    throw new IllegalStateException(
        "M2 Widget waive list (#3738): must be empty (perc.Test modern / #3736) or exactly {"
            + PERC_TEST_PACKAGE_DIR
            + "}; found: "
            + dirs);
  }

  /**
   * @return {@code true} when the Widget G4 waive list is empty (perc.Test no longer waived)
   */
  public static boolean isPercTestWidgetWaiverDropped() {
    return PSWidgetDefinitionXmlInventory.WAIVED_PACKAGE_DIRS.isEmpty();
  }

  /**
   * Definition ids allowed to select {@code LEGACY_*} as product residuals. Empty after #3736.
   *
   * @return unmodifiable set, never {@code null}
   */
  public static Set<String> currentWaivedDefinitionIds() {
    if (isWaivedPackage(PERC_TEST_PACKAGE_DIR)) {
      return Set.of(PERC_TEST_WIDGET_STEM);
    }
    return Set.of();
  }

  /**
   * Product/H2 {@code perc.Test} selection must match the current Widget waive list (#3738).
   *
   * <ul>
   *   <li>Waived: residual may select {@link PSDefinitionSourceKind#LEGACY_WIDGET_XML}
   *   <li>Waiver dropped: {@code perc.Test} must be modern-first if present; no waived-legacy roots
   * </ul>
   *
   * @param report package-root scan
   * @param requirePercTestRoot when {@code true}, fail if {@code perc.Test} is missing from the
   *     scan (product authored tree). When {@code false} (H2 modern-only materialize), require the
   *     root only after the waiver is dropped.
   * @throws IllegalStateException on mismatch
   */
  public static void assertPercTestSelectionMatchesWaiver(
      Report report, boolean requirePercTestRoot) {
    Objects.requireNonNull(report, "report");
    assertWidgetWaiverPolicy();
    boolean dropped = isPercTestWidgetWaiverDropped();
    if (dropped && report.waivedLegacyRootCount() != 0) {
      throw new IllegalStateException(
          "M2 (#3738): perc.Test waiver dropped but waived LEGACY_* roots remain: "
              + report.waivedLegacyRootCount());
    }
    boolean found = false;
    for (RootFinding f : report.roots()) {
      if (!PERC_TEST_PACKAGE_DIR.equals(f.packageDirName())) {
        continue;
      }
      found = true;
      if (dropped) {
        if (f.waived()
            || f.wouldUseLegacyShim()
            || f.kind() != PSDefinitionSourceKind.MODERN_COMPONENT_PACKAGE) {
          throw new IllegalStateException(
              "M2 (#3738): perc.Test must select MODERN_COMPONENT_PACKAGE after waiver drop; kind="
                  + f.kind()
                  + " waived="
                  + f.waived()
                  + " wouldUseLegacyShim="
                  + f.wouldUseLegacyShim());
        }
      } else if (!f.waived()) {
        throw new IllegalStateException(
            "M2 (#3738): perc.Test is still on the Widget waive list but the scan did not mark it waived");
      }
    }
    boolean mustFind = requirePercTestRoot || dropped;
    if (mustFind && !found) {
      throw new IllegalStateException(
          "M2 (#3738): perc.Test package root missing from scan (waiverDropped="
              + dropped
              + ", requirePercTestRoot="
              + requirePercTestRoot
              + ")");
    }
  }

  /**
   * CLI: scan a Packages tree and exit non-zero on unexpected non-waived {@code LEGACY_*}.
   *
   * <p>Usage: {@code PSProductPackageRootSelectionEvidence <packagesRoot>}
   *
   * @param args first arg = packages root
   * @throws IOException on I/O failure
   */
  public static void main(String[] args) throws IOException {
    if (args == null || args.length < 1 || args[0] == null || args[0].isBlank()) {
      System.err.println("Usage: PSProductPackageRootSelectionEvidence <packagesRoot>");
      System.err.println(
          "  packagesRoot e.g. modules/perc-packages/src/main/resources/Packages");
      System.err.println(
          "  or H2 install Packages/Modern after classpath materialize");
      System.exit(2);
    }
    Path root = Path.of(args[0]).toAbsolutePath().normalize();
    Report report = scanWidgetPackageRoots(root);
    List<String> modernIds = listModernWidgetDefinitionIds(root);
    System.out.println(
        "M2 product/H2 widget-root selection under "
            + root
            + ": roots="
            + report.roots().size()
            + " modern="
            + report.modernRootCount()
            + " unexpectedLegacy="
            + report.unexpectedLegacyRoots().size()
            + " waivedLegacy="
            + report.waivedLegacyRootCount()
            + " modernWidgetIds="
            + modernIds.size()
            + " waivedPackages="
            + PSWidgetDefinitionXmlInventory.WAIVED_PACKAGE_DIRS
            + " percTestWaiverDropped="
            + isPercTestWidgetWaiverDropped());
    for (RootFinding f : report.roots()) {
      String tag = f.isUnexpectedLegacy() ? "FAIL" : (f.waived() ? "WAIVED" : "OK");
      System.out.println(
          tag
              + " "
              + f.packageDirName()
              + " kind="
              + (f.kind() != null ? f.kind().name() : "none")
              + " wouldUseLegacyShim="
              + f.wouldUseLegacyShim());
    }
    try {
      // CLI may scan product Packages/ or H2 Modern; require perc.Test only when present
      // or after the waiver is dropped (H2 modern materialize includes widgets/).
      assertPercTestSelectionMatchesWaiver(report, false);
    } catch (IllegalStateException e) {
      System.err.println(e.getMessage());
      System.exit(1);
    }
    if (!report.isClean()) {
      System.err.println(
          "M2 FAIL: unexpected non-waived LEGACY_* on product/H2 widget roots (#3583 / #3738)");
      System.exit(1);
    }
    System.out.println(
        "M2 product/H2 roots PASS: zero non-waived LEGACY_* (shim kept; M3 still FAIL)");
  }

  private static RootFinding classifyPackageRoot(Path packageDir, String packageDirName)
      throws IOException {
    Path abs = packageDir.toAbsolutePath().normalize();
    boolean waived = isWaivedPackage(packageDirName);
    try {
      PSDefinitionSourceSelection sel = PSLegacyDefinitionXmlShim.selectForPackageRoot(abs);
      boolean legacy = sel.isLegacyXml();
      return new RootFinding(packageDirName, abs, sel.getKind(), legacy, waived);
    } catch (PSDefinitionSourceNotFoundException e) {
      return new RootFinding(packageDirName, abs, null, false, waived);
    }
  }

  /**
   * Widget definition material: modern {@code widgets/&lt;stem&gt;/component-package.json} or
   * committed/install Widget XML. Non-widget packages (templates, workflow, …) are skipped.
   */
  static boolean hasWidgetDefinitionMaterial(Path packageDir) throws IOException {
    if (packageDir == null || !Files.isDirectory(packageDir)) {
      return false;
    }
    if (PSWidgetXmlDualShip.hasModernWidgetSources(packageDir)) {
      return true;
    }
    Path staging = PSWidgetDefinitionXmlInventory.resolveWidgetsDir(packageDir);
    Path install = packageDir.resolve("rxconfig").resolve("Widgets");
    return hasXmlFiles(staging) || hasXmlFiles(install);
  }

  private static boolean hasXmlFiles(Path dir) throws IOException {
    if (dir == null || !Files.isDirectory(dir)) {
      return false;
    }
    try (DirectoryStream<Path> xmls = Files.newDirectoryStream(dir, "*.xml")) {
      for (Path xml : xmls) {
        if (Files.isRegularFile(xml)) {
          return true;
        }
      }
    }
    return false;
  }

  private static void sortRoots(List<RootFinding> roots) {
    roots.sort(
        Comparator.comparing((RootFinding f) -> f.packageDirName().toLowerCase(Locale.ROOT)));
  }

  private static void throwIfUnclean(Report report, String where) {
    if (report.isClean()) {
      return;
    }
    StringBuilder detail = new StringBuilder();
    for (RootFinding f : report.unexpectedLegacyRoots()) {
      detail
          .append(System.lineSeparator())
          .append("  root ")
          .append(f.packageDirName())
          .append(" kind=")
          .append(f.kind())
          .append(" wouldUseLegacyShim=true path=")
          .append(f.packageRoot());
    }
    for (DefinitionFinding f : report.unexpectedLegacyDefinitions()) {
      detail
          .append(System.lineSeparator())
          .append("  id ")
          .append(f.definitionId())
          .append(" kind=")
          .append(f.kind());
    }
    throw new IllegalStateException(
        "M2 H2/product zero-legacy-selection gate (#3583 / #3738): unexpected non-waived LEGACY_* on "
            + where
            + " (only waived package dirs: "
            + PSWidgetDefinitionXmlInventory.WAIVED_PACKAGE_DIRS
            + "; shim kept, #2852 blocked):"
            + detail);
  }

  /**
   * Unmodifiable view of known product widget stems (A+B+C).
   *
   * @return never empty
   */
  public static Set<String> knownProductWidgetStemSet() {
    return Collections.unmodifiableSet(new LinkedHashSet<>(KNOWN_PRODUCT_WIDGET_STEMS));
  }
}
