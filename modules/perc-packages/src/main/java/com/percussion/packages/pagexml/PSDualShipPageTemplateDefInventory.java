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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Fail-closed inventory for dual-ship page {@code *.templateDef} materialization (issue #3675 /
 * parent #2630, ADR-004).
 *
 * <p>Peer of {@link PSPageDefinitionXmlInventory} / {@code PSWidgetDefinitionXmlInventory}. Pass
 * condition: zero product packages that would emit dual-ship root templateDefs (modern {@code
 * pages/} + committed {@code page.installMode=dual-ship}). Waiver set is empty after perc.Test page
 * dual-ship exit (#3737). Unconfigured packages and packages on {@link PSPageXmlInstallMode#NATIVE}
 * (the policy default as of #3949) are not dual-ship emitters.
 *
 * <p>Committed policy is package-local {@code package-install.properties} only. JVM system
 * properties ({@link PSPageXmlInstallPolicy#SYS_PROP_INSTALL_MODE} / {@link
 * PSPageXmlInstallPolicy#SYS_PROP_DUAL_SHIP}) must not hide an explicit dual-ship opt-in in CI, and
 * must not make unconfigured packages look dual-ship.
 *
 * <p>Uses {@link Path#resolve(String)} / {@link Files} only — no hardcoded filesystem separators.
 *
 * @see PSPageXmlDualShip
 * @see PSPageXmlInstallPolicy
 */
public final class PSDualShipPageTemplateDefInventory {

  /**
   * Marker substring of the package-build log line written by {@code PSPackageBuilder} when dual-ship
   * root templateDefs are materialized.
   */
  public static final String DUAL_SHIP_LOG_MARKER = "dual-ship page templateDefs for ";

  /**
   * Widget leftover binary templateDef packages that remain <em>explicitly dual-ship-retained</em>
   * from sibling #3674.
   *
   * <p>#3674 leftovers on {@code main} were authored root files (not modern {@code pages/}): {@code
   * perc.FileAssetWidget/perc.fileBinary.templateDef}, {@code
   * perc.widgets.image/perc.imageMainBinary.templateDef}, {@code
   * perc.widgets.image/perc.imageThumbBinary.templateDef}. Sibling PR #3680 converts those to native
   * {@code pages/} rather than dual-ship-retain. This set is therefore <strong>empty</strong> — do
   * not add those package dirs here.
   */
  public static final Set<String> RETAINED_WIDGET_BINARY_PACKAGE_DIRS = Collections.emptySet();

  /**
   * Package directory names under {@code Packages/} allowed to still dual-ship page templateDefs.
   * Empty after perc.Test page dual-ship exit (#3737): {@code perc.Test} never authored page {@code
   * pages/} / {@code *.templateDef}. Plus {@link #RETAINED_WIDGET_BINARY_PACKAGE_DIRS} (also empty).
   * Do not expand without an ADR / residual issue.
   */
  public static final Set<String> WAIVED_PACKAGE_DIRS =
      Collections.unmodifiableSet(new LinkedHashSet<>(RETAINED_WIDGET_BINARY_PACKAGE_DIRS));

  /**
   * One package that would (or did) emit dual-ship page templateDefs.
   *
   * @param packageDirName immediate child name under the Packages root
   * @param packageDir absolute normalized package directory, or {@code null} for log-only findings
   * @param committedMode committed install mode that selected dual-ship
   * @param modernPageCount modern {@code pages/} package count ({@code 0} when unknown from a log)
   * @param waived whether {@code packageDirName} is in {@link #WAIVED_PACKAGE_DIRS}
   */
  public record Finding(
      String packageDirName,
      Path packageDir,
      PSPageXmlInstallMode committedMode,
      int modernPageCount,
      boolean waived) {}

  /**
   * Inventory scan result.
   *
   * @param all all dual-ship findings (waived + non-waived), sorted
   * @param nonWaived findings whose package is not waived
   * @param waived findings whose package is waived
   */
  public record Report(List<Finding> all, List<Finding> nonWaived, List<Finding> waived) {

    /**
     * @return true when no non-waived dual-ship page templateDef emitters are present
     */
    public boolean isClean() {
      return nonWaived.isEmpty();
    }
  }

  private PSDualShipPageTemplateDefInventory() {
    // utility
  }

  /**
   * Scan every immediate package directory under {@code packagesRoot} for committed dual-ship page
   * templateDef emitters (modern {@code pages/} + explicit committed {@code dual-ship} install
   * mode).
   *
   * @param packagesRoot non-null directory of package roots
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
        Path absPkg = packageDir.toAbsolutePath().normalize();
        if (!PSPageXmlDualShip.hasModernPageSources(absPkg)) {
          continue;
        }
        PSPageXmlInstallMode mode = resolveCommittedInstallMode(absPkg);
        if (mode != PSPageXmlInstallMode.DUAL_SHIP) {
          continue;
        }
        Path pagesDir = absPkg.resolve(PSPageXmlDualShip.PAGES_DIR_NAME);
        int modernCount = PSPageXmlDualShip.listModernPageDirs(pagesDir).size();
        boolean waived = isWaivedPackage(packageDirName);
        all.add(new Finding(packageDirName, absPkg, mode, modernCount, waived));
      }
    }

    return toReport(all);
  }

  /**
   * Parse package-build log lines for {@link #DUAL_SHIP_LOG_MARKER} and report dual-ship emitters.
   *
   * @param lines log lines (may be null entries); never null iterable
   * @return report of dual-ship packages named in the log
   */
  public static Report scanLogLines(Iterable<String> lines) {
    Objects.requireNonNull(lines, "lines");
    List<Finding> all = new ArrayList<>();
    LinkedHashSet<String> seen = new LinkedHashSet<>();
    for (String line : lines) {
      Optional<String> pkg = parseDualShipPackageFromLogLine(line);
      if (pkg.isEmpty()) {
        continue;
      }
      String packageDirName = pkg.get();
      if (!seen.add(packageDirName)) {
        continue;
      }
      boolean waived = isWaivedPackage(packageDirName);
      all.add(new Finding(packageDirName, null, PSPageXmlInstallMode.DUAL_SHIP, 0, waived));
    }
    return toReport(all);
  }

  /**
   * Scan a UTF-8 log file for dual-ship page templateDef lines.
   *
   * @param logFile existing text file
   * @return report of dual-ship packages named in the log
   * @throws IOException on I/O failure
   */
  public static Report scanLogFile(Path logFile) throws IOException {
    Objects.requireNonNull(logFile, "logFile");
    if (!Files.isRegularFile(logFile)) {
      throw new IllegalArgumentException("Log file is not a regular file: " + logFile);
    }
    return scanLogLines(Files.readAllLines(logFile, StandardCharsets.UTF_8));
  }

  /**
   * Format the historical package-build log line for dual-ship materialization.
   *
   * <p>{@code PSPackageBuilder} no longer emits this line (#3950). Kept so inventory log scans and
   * tests can still detect a regression if dual-ship writes reappear.
   *
   * @param packageName package directory name
   * @param written number of templateDefs written
   * @return log line including the {@link #DUAL_SHIP_LOG_MARKER}
   */
  public static String formatDualShipLogLine(String packageName, int written) {
    Objects.requireNonNull(packageName, "packageName");
    return "  " + DUAL_SHIP_LOG_MARKER + packageName + ": " + written + " written";
  }

  /**
   * Parse a dual-ship package-build log line to the package directory name.
   *
   * @param line one log line; may be null
   * @return package name when the line is a dual-ship templateDef write, else empty
   */
  public static Optional<String> parseDualShipPackageFromLogLine(String line) {
    if (line == null || line.isBlank()) {
      return Optional.empty();
    }
    int idx = line.indexOf(DUAL_SHIP_LOG_MARKER);
    if (idx < 0) {
      return Optional.empty();
    }
    String rest = line.substring(idx + DUAL_SHIP_LOG_MARKER.length());
    int colon = rest.indexOf(':');
    if (colon <= 0) {
      return Optional.empty();
    }
    String pkg = rest.substring(0, colon).trim();
    if (pkg.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(pkg);
  }

  /**
   * Whether the package directory name is on the explicit waiver list.
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
   * Fail-fast for package-build: non-waived packages must not materialize dual-ship page
   * templateDefs.
   *
   * @param packageDirName package folder name
   * @throws IllegalStateException when the package is not waived
   */
  public static void assertDualShipMaterializationAllowed(String packageDirName) {
    if (isWaivedPackage(packageDirName)) {
      return;
    }
    throw new IllegalStateException(
        "Dual-ship page templateDefs CI gate (#3675): package "
            + packageDirName
            + " is not waived (waived package dirs: "
            + WAIVED_PACKAGE_DIRS
            + "). Dual-ship is explicit opt-in only (#3949). Unset "
            + PSPageXmlInstallPolicy.PROP_PAGE_INSTALL_MODE
            + " (native default) or set it to native in "
            + PSPageXmlInstallPolicy.PACKAGE_INSTALL_PROPS
            + ", or add an explicit waiver.");
  }

  /**
   * Fail-fast assertion used by Surefire / CI: non-waived dual-ship page templateDef emitters must
   * not reappear under the Packages tree.
   *
   * @param packagesRoot Packages tree root
   * @throws IOException on I/O failure
   * @throws IllegalStateException when non-waived dual-ship emitters are present
   */
  public static void assertNoNonWaivedDualShipPageTemplateDefs(Path packagesRoot)
      throws IOException {
    Report report = scan(packagesRoot);
    failIfUnclean(report, "Packages tree " + packagesRoot);
  }

  /**
   * Fail-fast assertion for package-build log lines.
   *
   * @param lines log lines
   * @throws IllegalStateException when a non-waived dual-ship log line is present
   */
  public static void assertNoNonWaivedDualShipLogLines(Iterable<String> lines) {
    Report report = scanLogLines(lines);
    failIfUnclean(report, "package-build log");
  }

  /**
   * Committed install mode from package-local properties only (default {@link
   * PSPageXmlInstallPolicy#DEFAULT_MODE} = native). Ignores JVM system properties so CI reflects
   * the source tree. Dual-ship requires explicit {@code page.installMode=dual-ship}.
   *
   * @param packageDir package root
   * @return non-null mode
   * @throws IOException on I/O failure
   */
  public static PSPageXmlInstallMode resolveCommittedInstallMode(Path packageDir)
      throws IOException {
    Objects.requireNonNull(packageDir, "packageDir");
    Properties props = PSPageXmlInstallPolicy.loadPackageInstallProps(packageDir);
    if (props != null) {
      String mode = props.getProperty(PSPageXmlInstallPolicy.PROP_PAGE_INSTALL_MODE);
      if (mode != null && !mode.isBlank()) {
        return PSPageXmlInstallPolicy.parseMode(mode.trim());
      }
    }
    return PSPageXmlInstallPolicy.DEFAULT_MODE;
  }

  /**
   * CLI entry for local / optional CI use (non-zero exit when non-waived dual-ship emitters exist).
   *
   * <p>Usage: {@code PSDualShipPageTemplateDefInventory <packagesRoot> [logFile]}
   *
   * @param args packages root and optional log file
   * @throws IOException on I/O failure
   */
  public static void main(String[] args) throws IOException {
    if (args == null || args.length < 1 || args[0] == null || args[0].isBlank()) {
      System.err.println("Usage: PSDualShipPageTemplateDefInventory <packagesRoot> [logFile]");
      System.err.println(
          "  packagesRoot e.g. modules/perc-packages/src/main/resources/Packages");
      System.exit(2);
      return;
    }
    Path root = Path.of(args[0]).toAbsolutePath().normalize();
    Report report = scan(root);
    printReport("Committed dual-ship page templateDef inventory under " + root, report);
    if (!report.isClean()) {
      System.err.println(
          "FAIL: non-waived dual-ship page templateDefs would be materialized (#3675)");
      System.exit(1);
      return;
    }
    if (args.length > 1 && args[1] != null && !args[1].isBlank()) {
      Path logFile = Path.of(args[1]).toAbsolutePath().normalize();
      Report logReport = scanLogFile(logFile);
      printReport("Dual-ship page templateDef log inventory under " + logFile, logReport);
      if (!logReport.isClean()) {
        System.err.println("FAIL: non-waived dual-ship page templateDefs log lines (#3675)");
        System.exit(1);
        return;
      }
    }
    System.out.println(
        "PASS: zero non-waived dual-ship page templateDefs (#3675; waived="
            + WAIVED_PACKAGE_DIRS
            + ")");
  }

  private static Report toReport(List<Finding> all) {
    all.sort(
        Comparator.comparing((Finding f) -> f.packageDirName().toLowerCase(Locale.ROOT))
            .thenComparing(f -> f.packageDir() == null ? "" : f.packageDir().toString()));
    List<Finding> nonWaived = all.stream().filter(f -> !f.waived()).collect(Collectors.toList());
    List<Finding> waivedOnly = all.stream().filter(Finding::waived).collect(Collectors.toList());
    return new Report(
        Collections.unmodifiableList(all),
        Collections.unmodifiableList(nonWaived),
        Collections.unmodifiableList(waivedOnly));
  }

  private static void failIfUnclean(Report report, String where) {
    if (report.isClean()) {
      return;
    }
    String detail =
        report.nonWaived().stream()
            .map(
                f ->
                    f.packageDirName()
                        + " mode="
                        + f.committedMode()
                        + " modernPages="
                        + f.modernPageCount()
                        + (f.packageDir() != null ? " -> " + f.packageDir() : ""))
            .collect(Collectors.joining(System.lineSeparator() + "  "));
    throw new IllegalStateException(
        "Dual-ship page templateDefs CI gate (#3675): non-waived dual-ship emitters under "
            + where
            + " (only waived package dirs: "
            + WAIVED_PACKAGE_DIRS
            + "):"
            + System.lineSeparator()
            + "  "
            + detail);
  }

  private static void printReport(String title, Report report) {
    System.out.println(
        title
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
              + f.packageDirName()
              + " mode="
              + f.committedMode()
              + " modernPages="
              + f.modernPageCount()
              + (f.packageDir() != null ? " " + f.packageDir() : ""));
    }
  }
}
