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

package com.percussion.packages.inventory;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Shared product-package inventory for committed Page/Gadget definition XML under Packages ship
 * paths (Phase 5 gate G4 / issue #3581, parent #2630, ADR-004).
 *
 * <p>Peer of {@code PSWidgetDefinitionXmlInventory}. Scans both the package staging layout ({@code
 * sys__UserDependency--rxconfig/Pages|Gadgets}) and the install layout ({@code
 * rxconfig/Pages|Gadgets}) that {@code PSLegacyDefinitionXmlShim} recognizes. Does <em>not</em>
 * treat modern authoring trees ({@code pages/}, {@code gadgets/}, catalog JSON) as definition XML.
 *
 * <p>Uses {@link Path#resolve(String)} / {@link Files} only — no hardcoded filesystem separators.
 */
public final class PSDefinitionXmlShipPathInventory {

  /**
   * Package directory names under {@code Packages/} allowed to still commit Page/Gadget definition
   * XML. Explicit and minimal — do not expand without an ADR / residual issue.
   */
  public static final Set<String> WAIVED_PACKAGE_DIRS =
      Collections.unmodifiableSet(new LinkedHashSet<>(List.of("perc.Test")));

  /** Definition-XML kind under a Packages tree. */
  public enum Kind {
    PAGE(
        "Page",
        List.of(
            List.of("sys__UserDependency--rxconfig", "Pages"),
            List.of("rxconfig", "Pages"))),
    GADGET(
        "Gadget",
        List.of(
            List.of("sys__UserDependency--rxconfig", "Gadgets"),
            List.of("rxconfig", "Gadgets")));

    private final String label;
    private final List<List<String>> relativeSegmentSets;

    Kind(String label, List<List<String>> relativeSegmentSets) {
      this.label = label;
      this.relativeSegmentSets = relativeSegmentSets;
    }

    /**
     * @return human label used in G4 messages ({@code Page} / {@code Gadget})
     */
    public String label() {
      return label;
    }

    /**
     * @return portable path segment lists for each recognized ship directory
     */
    public List<List<String>> relativeSegmentSets() {
      return relativeSegmentSets;
    }
  }

  /**
   * One committed definition XML file under a package ship path.
   *
   * @param kind Page or Gadget
   * @param packageDirName immediate child name under the Packages root (e.g. {@code perc.Test})
   * @param xmlPath absolute normalized path to the {@code .xml} file
   * @param waived whether {@code packageDirName} is in {@link #WAIVED_PACKAGE_DIRS}
   */
  public record Finding(Kind kind, String packageDirName, Path xmlPath, boolean waived) {}

  /**
   * Inventory scan result for a Packages root and one or more kinds.
   *
   * @param all all definition XML findings (waived + non-waived), sorted
   * @param nonWaived findings whose package is not waived
   * @param waived findings whose package is waived
   */
  public record Report(List<Finding> all, List<Finding> nonWaived, List<Finding> waived) {

    /**
     * @return true when no non-waived definition XML is present
     */
    public boolean isClean() {
      return nonWaived.isEmpty();
    }
  }

  private PSDefinitionXmlShipPathInventory() {
    // utility
  }

  /**
   * Scan every immediate package directory under {@code packagesRoot} for committed definition XML
   * of {@code kind}.
   *
   * @param packagesRoot non-null directory of package roots
   * @param kind Page or Gadget
   * @return report of findings; never null
   * @throws IOException on I/O failure
   * @throws IllegalArgumentException if packagesRoot is null or not a directory
   */
  public static Report scan(Path packagesRoot, Kind kind) throws IOException {
    Objects.requireNonNull(kind, "kind");
    return scan(packagesRoot, List.of(kind));
  }

  /**
   * Scan Pages and Gadgets ship paths in one pass.
   *
   * @param packagesRoot Packages tree root
   * @return combined report
   * @throws IOException on I/O failure
   */
  public static Report scanPagesAndGadgets(Path packagesRoot) throws IOException {
    return scan(packagesRoot, List.of(Kind.PAGE, Kind.GADGET));
  }

  /**
   * Fail-fast assertion used by Surefire / CI: non-waived definition XML of {@code kind} must not
   * reappear.
   *
   * @param packagesRoot Packages tree root
   * @param kind Page or Gadget
   * @throws IOException on I/O failure
   * @throws IllegalStateException when non-waived def XML is present
   */
  public static void assertNoNonWaivedDefinitionXml(Path packagesRoot, Kind kind)
      throws IOException {
    Objects.requireNonNull(kind, "kind");
    assertNoNonWaivedDefinitionXml(packagesRoot, List.of(kind));
  }

  /**
   * Fail-fast assertion for both Page and Gadget ship paths.
   *
   * @param packagesRoot Packages tree root
   * @throws IOException on I/O failure
   * @throws IllegalStateException when non-waived Page or Gadget def XML is present
   */
  public static void assertNoNonWaivedPageOrGadgetDefinitionXml(Path packagesRoot)
      throws IOException {
    assertNoNonWaivedDefinitionXml(packagesRoot, List.of(Kind.PAGE, Kind.GADGET));
  }

  /**
   * Whether the package directory name is on the explicit waiver list ({@code perc.Test} only).
   *
   * @param packageDirName package folder name under Packages
   * @return true if waived
   */
  public static boolean isWaivedPackage(String packageDirName) {
    if (packageDirName == null || packageDirName.isBlank()) {
      return false;
    }
    return WAIVED_PACKAGE_DIRS.contains(packageDirName);
  }

  /**
   * Resolve the primary (package-staging) ship directory for {@code kind} under a package.
   *
   * @param packageDir package root
   * @param kind Page or Gadget
   * @return staging ship directory (may not exist)
   */
  public static Path resolvePrimaryShipDir(Path packageDir, Kind kind) {
    Objects.requireNonNull(packageDir, "packageDir");
    Objects.requireNonNull(kind, "kind");
    return resolveRelative(packageDir, kind.relativeSegmentSets().get(0));
  }

  /**
   * Resolve the install {@code rxconfig/Pages|Gadgets} ship directory for {@code kind}.
   *
   * @param packageDir package root
   * @param kind Page or Gadget
   * @return rxconfig ship directory (may not exist)
   */
  public static Path resolveRxconfigShipDir(Path packageDir, Kind kind) {
    Objects.requireNonNull(packageDir, "packageDir");
    Objects.requireNonNull(kind, "kind");
    return resolveRelative(packageDir, kind.relativeSegmentSets().get(1));
  }

  /**
   * CLI entry for local / optional CI use (non-zero exit when non-waived XML is found).
   *
   * <p>Usage: {@code PSDefinitionXmlShipPathInventory <packagesRoot> [PAGE|GADGET|ALL]}
   *
   * @param args packages root and optional kind
   * @throws IOException on I/O failure
   */
  public static void main(String[] args) throws IOException {
    if (args == null || args.length < 1 || args[0] == null || args[0].isBlank()) {
      System.err.println(
          "Usage: PSDefinitionXmlShipPathInventory <packagesRoot> [PAGE|GADGET|ALL]");
      System.err.println(
          "  packagesRoot e.g. modules/perc-packages/src/main/resources/Packages");
      System.exit(2);
      return;
    }
    Path root = Path.of(args[0]).toAbsolutePath().normalize();
    List<Kind> kinds = parseKinds(args.length > 1 ? args[1] : "ALL");
    Report report = scan(root, kinds);
    String kindLabel =
        kinds.stream().map(Kind::label).collect(Collectors.joining("+"));
    System.out.println(
        kindLabel
            + " def XML inventory under "
            + root
            + ": total="
            + report.all().size()
            + " waived="
            + report.waived().size()
            + " nonWaived="
            + report.nonWaived().size()
            + " waivedPackages="
            + WAIVED_PACKAGE_DIRS);
    for (Finding f : report.all()) {
      System.out.println(
          (f.waived() ? "WAIVED " : "FAIL   ")
              + f.kind().label()
              + " "
              + f.packageDirName()
              + " "
              + f.xmlPath());
    }
    if (!report.isClean()) {
      System.err.println("G4 FAIL: non-waived " + kindLabel + " definition XML present (#3581)");
      System.exit(1);
    }
    System.out.println("G4 PASS: zero non-waived product " + kindLabel + " definition XML");
  }

  static Report scan(Path packagesRoot, List<Kind> kinds) throws IOException {
    Objects.requireNonNull(packagesRoot, "packagesRoot");
    Objects.requireNonNull(kinds, "kinds");
    if (kinds.isEmpty()) {
      throw new IllegalArgumentException("kinds must not be empty");
    }
    if (!Files.isDirectory(packagesRoot)) {
      throw new IllegalArgumentException("Packages root is not a directory: " + packagesRoot);
    }

    List<Finding> all = new ArrayList<>();
    try (DirectoryStream<Path> packages = Files.newDirectoryStream(packagesRoot)) {
      for (Path packageDir : packages) {
        if (!Files.isDirectory(packageDir)) {
          continue;
        }
        Path namePath = packageDir.getFileName();
        if (namePath == null) {
          continue;
        }
        String packageDirName = namePath.toString();
        boolean waived = isWaivedPackage(packageDirName);
        for (Kind kind : kinds) {
          collectKind(all, packageDir, packageDirName, waived, kind);
        }
      }
    }

    all.sort(
        Comparator.comparing((Finding f) -> f.kind().name())
            .thenComparing(f -> f.packageDirName().toLowerCase(Locale.ROOT))
            .thenComparing(f -> f.xmlPath().getFileName().toString().toLowerCase(Locale.ROOT)));

    List<Finding> nonWaived = all.stream().filter(f -> !f.waived()).collect(Collectors.toList());
    List<Finding> waivedOnly = all.stream().filter(Finding::waived).collect(Collectors.toList());
    return new Report(
        Collections.unmodifiableList(all),
        Collections.unmodifiableList(nonWaived),
        Collections.unmodifiableList(waivedOnly));
  }

  static void assertNoNonWaivedDefinitionXml(Path packagesRoot, List<Kind> kinds)
      throws IOException {
    Report report = scan(packagesRoot, kinds);
    if (report.isClean()) {
      return;
    }
    String kindLabel =
        kinds.stream().map(Kind::label).collect(Collectors.joining("+"));
    String detail =
        report.nonWaived().stream()
            .map(f -> f.kind().label() + " " + f.packageDirName() + " -> " + f.xmlPath())
            .collect(Collectors.joining(System.lineSeparator() + "  "));
    throw new IllegalStateException(
        "G4 inventory gate (#3581): non-waived product "
            + kindLabel
            + " definition XML under "
            + packagesRoot
            + " (only waived package dirs: "
            + WAIVED_PACKAGE_DIRS
            + "):"
            + System.lineSeparator()
            + "  "
            + detail);
  }

  private static void collectKind(
      List<Finding> all, Path packageDir, String packageDirName, boolean waived, Kind kind)
      throws IOException {
    for (List<String> segments : kind.relativeSegmentSets()) {
      Path shipDir = resolveRelative(packageDir, segments);
      if (!Files.isDirectory(shipDir)) {
        continue;
      }
      try (DirectoryStream<Path> xmls = Files.newDirectoryStream(shipDir, "*.xml")) {
        for (Path xml : xmls) {
          if (Files.isRegularFile(xml)) {
            Path absXml = shipDir.resolve(xml.getFileName()).toAbsolutePath().normalize();
            all.add(new Finding(kind, packageDirName, absXml, waived));
          }
        }
      }
    }
  }

  private static Path resolveRelative(Path packageDir, List<String> segments) {
    Path p = packageDir;
    for (String segment : segments) {
      p = p.resolve(segment);
    }
    return p;
  }

  private static List<Kind> parseKinds(String raw) {
    if (raw == null || raw.isBlank() || "ALL".equalsIgnoreCase(raw)) {
      return List.of(Kind.PAGE, Kind.GADGET);
    }
    return List.of(Kind.valueOf(raw.trim().toUpperCase(Locale.ROOT)));
  }
}
