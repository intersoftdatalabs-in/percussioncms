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
 * Product package inventory for committed Widget definition XML under the install ship path
 * {@code sys__UserDependency--rxconfig/Widgets/*.xml} (Phase 5 gate G4 / issue #3026, parent
 * #2630, ADR-004).
 *
 * <p><strong>Pass condition (M1 Widget portion / G4):</strong> zero <em>non-waived</em> product
 * Widget definition XML files under a {@code Packages/} tree. The only explicit waiver is {@code
 * perc.Test} (test-harness package residual).
 *
 * <p>Uses {@link Path#resolve(String)} / {@link Files} only — no hardcoded filesystem separators.
 *
 * @see PSWidgetXmlInstallEmitter
 * @see PSWidgetXmlPackageCompiler#resolveWidgetsDir(Path)
 */
public final class PSWidgetDefinitionXmlInventory {

  /**
   * Package directory names under {@code Packages/} allowed to still commit Widget definition XML.
   * Explicit and minimal — do not expand without an ADR / residual issue.
   */
  public static final Set<String> WAIVED_PACKAGE_DIRS =
      Collections.unmodifiableSet(new LinkedHashSet<>(List.of("perc.Test")));

  private PSWidgetDefinitionXmlInventory() {
    // utility
  }

  /**
   * One committed Widget definition XML file under a package's install Widgets path.
   *
   * @param packageDirName immediate child name under the Packages root (e.g. {@code perc.Test})
   * @param xmlPath absolute or normalized path to the {@code .xml} file
   * @param waived whether {@code packageDirName} is in {@link #WAIVED_PACKAGE_DIRS}
   */
  public record Finding(String packageDirName, Path xmlPath, boolean waived) {}

  /**
   * Inventory scan result for a Packages root.
   *
   * @param all all Widget definition XML findings (waived + non-waived), sorted
   * @param nonWaived findings whose package is not waived
   * @param waived findings whose package is waived
   */
  public record Report(List<Finding> all, List<Finding> nonWaived, List<Finding> waived) {

    /**
     * @return true when no non-waived Widget definition XML is present
     */
    public boolean isClean() {
      return nonWaived.isEmpty();
    }
  }

  /**
   * Scan every immediate package directory under {@code packagesRoot} for committed Widget
   * definition XML under the install Widgets path.
   *
   * @param packagesRoot non-null directory of package roots (e.g. {@code
   *     modules/perc-packages/src/main/resources/Packages})
   * @return report of findings; never null
   * @throws IOException on I/O failure
   * @throws IllegalArgumentException if packagesRoot is null or not a directory
   */
  public static Report scan(Path packagesRoot) throws IOException {
    Objects.requireNonNull(packagesRoot, "packagesRoot");
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
        Path widgetsDir = resolveWidgetsDir(packageDir);
        if (!Files.isDirectory(widgetsDir)) {
          continue;
        }
        try (DirectoryStream<Path> xmls = Files.newDirectoryStream(widgetsDir, "*.xml")) {
          for (Path xml : xmls) {
            if (Files.isRegularFile(xml)) {
              all.add(new Finding(packageDirName, xml.toAbsolutePath().normalize(), waived));
            }
          }
        }
      }
    }

    all.sort(
        Comparator.comparing((Finding f) -> f.packageDirName().toLowerCase(Locale.ROOT))
            .thenComparing(f -> f.xmlPath().getFileName().toString().toLowerCase(Locale.ROOT)));

    List<Finding> nonWaived = all.stream().filter(f -> !f.waived()).collect(Collectors.toList());
    List<Finding> waivedOnly = all.stream().filter(Finding::waived).collect(Collectors.toList());
    return new Report(
        Collections.unmodifiableList(all),
        Collections.unmodifiableList(nonWaived),
        Collections.unmodifiableList(waivedOnly));
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
   * Fail-fast assertion used by Surefire / CI: non-waived Widget definition XML must not reappear.
   *
   * @param packagesRoot Packages tree root
   * @throws IOException on I/O failure
   * @throws IllegalStateException when non-waived Widget def XML is present (message lists paths)
   */
  public static void assertNoNonWaivedWidgetDefinitionXml(Path packagesRoot) throws IOException {
    Report report = scan(packagesRoot);
    if (report.isClean()) {
      return;
    }
    String detail =
        report.nonWaived().stream()
            .map(f -> f.packageDirName() + " -> " + f.xmlPath())
            .collect(Collectors.joining(System.lineSeparator() + "  "));
    throw new IllegalStateException(
        "G4 inventory gate (#3026): non-waived product Widget definition XML under "
            + packagesRoot
            + " (only waived package dirs: "
            + WAIVED_PACKAGE_DIRS
            + "):"
            + System.lineSeparator()
            + "  "
            + detail);
  }

  /**
   * Resolve install Widgets directory under a package using portable path segments (same layout as
   * {@link PSWidgetXmlPackageCompiler#resolveWidgetsDir(Path)}).
   *
   * @param packageDir package root
   * @return Widgets directory path (may not exist)
   */
  public static Path resolveWidgetsDir(Path packageDir) {
    Objects.requireNonNull(packageDir, "packageDir");
    Path p = packageDir;
    for (String segment : PSWidgetXmlPackageCompiler.WIDGETS_RELATIVE.split("/")) {
      p = p.resolve(segment);
    }
    return p;
  }

  /**
   * CLI entry for local / optional CI use (non-zero exit when non-waived XML is found).
   *
   * <p>Usage: {@code PSWidgetDefinitionXmlInventory <packagesRoot>}
   *
   * @param args first arg = packages root path
   * @throws IOException on I/O failure
   */
  public static void main(String[] args) throws IOException {
    if (args == null || args.length < 1 || args[0] == null || args[0].isBlank()) {
      System.err.println(
          "Usage: PSWidgetDefinitionXmlInventory <packagesRoot>");
      System.err.println(
          "  packagesRoot e.g. modules/perc-packages/src/main/resources/Packages");
      System.exit(2);
      return;
    }
    Path root = Path.of(args[0]).toAbsolutePath().normalize();
    Report report = scan(root);
    System.out.println(
        "Widget def XML inventory under "
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
          (f.waived() ? "WAIVED " : "FAIL   ") + f.packageDirName() + " " + f.xmlPath());
    }
    if (!report.isClean()) {
      System.err.println("G4 FAIL: non-waived Widget definition XML present (#3026)");
      System.exit(1);
    }
    System.out.println("G4 PASS: zero non-waived product Widget definition XML");
  }
}
