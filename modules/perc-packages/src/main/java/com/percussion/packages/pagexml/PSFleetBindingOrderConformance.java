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
import com.percussion.packages.manifest.PSComponentPackageManifestIo;
import com.percussion.packages.manifest.PSComponentPackageManifestValidator;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Fleet-wide JEXL binding-order conformance sweep over every real package manifest (issue #7387 /
 * parent #2626).
 *
 * <p>The next-a gate ({@code PSPageXmlBindingConformance}) was proven on pilot-only fixtures
 * (#7305) and the pilot product package (#7307). This scanner extends it to every {@code pages/} +
 * {@code widgets/} manifest under the product {@code Packages} tree: it reads and validates each
 * {@code component-package.json}, runs {@code checkManifestBindings} on every template, and
 * reports hard defects (unreadable manifests, validation errors, null entries, blank variables)
 * separately from snippet branch shadowing (duplicate variables in document order).
 *
 * <p>Shadowing is the Velocity snippet idiom of reassigning a JEXL variable across conditional
 * branches (later entries shadow earlier ones at assembly time); the gate surfaces it in document
 * order and never sorts it away. Shadowed templates carry a recorded disposition (expected
 * duplicate set, see {@code PSFleetBindingOrderConformanceTest}); anything else is a hard defect
 * and must be fixed in the manifest in place.
 *
 * <p>Cross-platform: all traversal uses {@link Path} / {@link Files} — no hardcoded filesystem
 * separators. Display keys join name segments with {@code "/"} for reporting only and are never
 * used for I/O.
 */
public final class PSFleetBindingOrderConformance {

  /** Manifest directory kinds covered by the sweep. */
  static final String PAGES_DIR_NAME = "pages";

  static final String WIDGETS_DIR_NAME = "widgets";

  private PSFleetBindingOrderConformance() {
    // utility
  }

  /**
   * Scan every {@code pages/} + {@code widgets/} manifest under a {@code Packages} root.
   *
   * @param packagesRoot non-null directory holding one subdirectory per product package
   * @return sweep report (never null); findings sorted by package, kind, stem for determinism
   * @throws IOException on traversal failure
   */
  public static Report scan(Path packagesRoot) throws IOException {
    Objects.requireNonNull(packagesRoot, "packagesRoot");
    List<TemplateFinding> findings = new ArrayList<>();
    try (Stream<Path> stream = Files.walk(packagesRoot)) {
      List<Path> manifests =
          stream
              .filter(Files::isRegularFile)
              .filter(p -> p.getFileName().toString().equals(
                  PSComponentPackageManifest.DEFAULT_MANIFEST_FILE_NAME))
              .filter(PSFleetBindingOrderConformance::isPagesOrWidgetsManifest)
              .sorted()
              .toList();
      for (Path manifestPath : manifests) {
        findings.addAll(checkManifest(manifestPath));
      }
    }
    findings.sort(
        Comparator.comparing(TemplateFinding::getPackageDirName)
            .thenComparing(TemplateFinding::getKindDirName)
            .thenComparing(TemplateFinding::getStem));
    return new Report(packagesRoot, findings);
  }

  private static boolean isPagesOrWidgetsManifest(Path manifestPath) {
    Path stemDir = manifestPath.getParent();
    if (stemDir == null) {
      return false;
    }
    Path kindDir = stemDir.getParent();
    if (kindDir == null || kindDir.getFileName() == null) {
      return false;
    }
    String kind = kindDir.getFileName().toString();
    return PAGES_DIR_NAME.equals(kind) || WIDGETS_DIR_NAME.equals(kind);
  }

  private static List<TemplateFinding> checkManifest(Path manifestPath) {
    Path stemDir = manifestPath.getParent();
    Path kindDir = stemDir.getParent();
    String packageDirName = kindDir.getParent().getFileName().toString();
    String kindDirName = kindDir.getFileName().toString();
    String stem = stemDir.getFileName().toString();
    String key = displayKey(packageDirName, kindDirName, stem);

    PSComponentPackageManifest manifest;
    try {
      manifest = PSComponentPackageManifestIo.read(manifestPath);
    } catch (Exception e) {
      return List.of(
          TemplateFinding.unreadable(
              packageDirName, kindDirName, stem, key, "unreadable manifest: " + e.getMessage()));
    }
    List<String> validationErrors =
        PSComponentPackageManifestValidator.validateCollecting(manifest);
    List<PSComponentPackageManifest.TemplateRef> templates = manifest.getTemplates();
    if (templates == null || templates.isEmpty()) {
      List<String> errors = new ArrayList<>(validationErrors);
      errors.add("manifest declares no templates");
      return List.of(
          TemplateFinding.unreadable(
              packageDirName, kindDirName, stem, key, String.join("; ", errors)));
    }
    List<TemplateFinding> perTemplate = new ArrayList<>();
    for (PSComponentPackageManifest.TemplateRef template : templates) {
      String templateName = template == null ? null : template.getName();
      List<PSComponentPackageManifest.Binding> bindings =
          template == null ? null : template.getBindings();
      PSPageXmlBindingConformance.Result result =
          PSPageXmlBindingConformance.checkManifestBindings(templateName, bindings);
      perTemplate.add(
          new TemplateFinding(
              packageDirName,
              kindDirName,
              stem,
              key,
              templateName,
              result,
              new ArrayList<>(validationErrors)));
    }
    return perTemplate;
  }

  private static String displayKey(String packageDirName, String kindDirName, String stem) {
    // Reporting only — never used for filesystem I/O (segments are never passed to Path/Files).
    // mkd-code-review[paths.hardcoded_sep]: adjudicated intentional (issue #7387,
    // same as #7273/#5897/#5898/#5910/#5927/#6195/#7272) — "/" joins a display-only
    // report key, not an OS filesystem path.
    return packageDirName + "/" + kindDirName + "/" + stem;
  }

  /** Conformance verdict for one template of one fleet manifest. */
  public static final class TemplateFinding {
    private final String packageDirName;
    private final String kindDirName;
    private final String stem;
    private final String displayKey;
    private final String templateName;
    private final PSPageXmlBindingConformance.Result result;
    private final List<String> manifestErrors;

    TemplateFinding(
        String packageDirName,
        String kindDirName,
        String stem,
        String displayKey,
        String templateName,
        PSPageXmlBindingConformance.Result result,
        List<String> manifestErrors) {
      this.packageDirName = packageDirName;
      this.kindDirName = kindDirName;
      this.stem = stem;
      this.displayKey = displayKey;
      this.templateName = templateName;
      this.result = result;
      this.manifestErrors = Collections.unmodifiableList(new ArrayList<>(manifestErrors));
    }

    static TemplateFinding unreadable(
        String packageDirName, String kindDirName, String stem, String displayKey, String error) {
      return new TemplateFinding(
          packageDirName,
          kindDirName,
          stem,
          displayKey,
          null,
          PSPageXmlBindingConformance.checkManifestBindings("?", null),
          List.of(error));
    }

    /** Product package directory name under {@code Packages} (never null). */
    public String getPackageDirName() {
      return packageDirName;
    }

    /** Manifest kind directory: {@code pages} or {@code widgets} (never null). */
    public String getKindDirName() {
      return kindDirName;
    }

    /** Page/widget stem directory holding the manifest (never null). */
    public String getStem() {
      return stem;
    }

    /** Stable reporting key {@code <package>/<kind>/<stem>} (never null; display only). */
    public String getDisplayKey() {
      return displayKey;
    }

    /** Template name as authored (may be null for unreadable manifests). */
    public String getTemplateName() {
      return templateName;
    }

    /** Binding-order verdict for this template (never null). */
    public PSPageXmlBindingConformance.Result getResult() {
      return result;
    }

    /** Manifest-level errors: unreadable file, validation failures (never null). */
    public List<String> getManifestErrors() {
      return manifestErrors;
    }

    /**
     * True when the manifest itself is unusable (unreadable, invalid, or template-less),
     * regardless of binding content.
     */
    public boolean hasManifestErrors() {
      return !manifestErrors.isEmpty();
    }

    /** One-line operator summary, e.g. {@code perc.widget.title/widgets/percTitle: template
     * 'percTitle': 8 bindings, NOT conformant (duplicates: [dsUrl], blank: 0, null: 0)}. */
    public String summarize() {
      StringBuilder sb = new StringBuilder(displayKey).append(": ");
      if (hasManifestErrors()) {
        sb.append("MANIFEST ERROR: ").append(String.join("; ", manifestErrors));
        return sb.toString();
      }
      sb.append(result.summarize());
      return sb.toString();
    }
  }

  /** Immutable sweep report over one {@code Packages} root. */
  public static final class Report {
    private final Path packagesRoot;
    private final List<TemplateFinding> findings;

    Report(Path packagesRoot, List<TemplateFinding> findings) {
      this.packagesRoot = packagesRoot;
      this.findings = Collections.unmodifiableList(new ArrayList<>(findings));
    }

    /** Packages root scanned (never null). */
    public Path getPackagesRoot() {
      return packagesRoot;
    }

    /** Per-template findings in package/kind/stem order (never null). */
    public List<TemplateFinding> getFindings() {
      return findings;
    }

    /** Number of manifests (templates) scanned. */
    public int getManifestCount() {
      return findings.size();
    }

    /** Total bindings checked across all templates. */
    public int getBindingCount() {
      int total = 0;
      for (TemplateFinding finding : findings) {
        total += finding.getResult().getBindingCount();
      }
      return total;
    }

    /** Findings with manifest-level errors (unreadable or invalid). */
    public List<TemplateFinding> getManifestErrorFindings() {
      List<TemplateFinding> rows = new ArrayList<>();
      for (TemplateFinding finding : findings) {
        if (finding.hasManifestErrors()) {
          rows.add(finding);
        }
      }
      return Collections.unmodifiableList(rows);
    }

    /** Findings with null binding entries or blank variables (hard binding defects). */
    public List<TemplateFinding> getHardBindingDefectFindings() {
      List<TemplateFinding> rows = new ArrayList<>();
      for (TemplateFinding finding : findings) {
        if (!finding.hasManifestErrors()
            && (finding.getResult().getNullEntryCount() > 0
                || finding.getResult().getBlankVariableCount() > 0)) {
          rows.add(finding);
        }
      }
      return Collections.unmodifiableList(rows);
    }

    /** Findings with duplicate variables (snippet branch shadowing — dispositional, not hard). */
    public List<TemplateFinding> getShadowingFindings() {
      List<TemplateFinding> rows = new ArrayList<>();
      for (TemplateFinding finding : findings) {
        if (!finding.hasManifestErrors()
            && !finding.getResult().getDuplicateVariables().isEmpty()) {
          rows.add(finding);
        }
      }
      return Collections.unmodifiableList(rows);
    }

    /** True when any manifest is unreadable/invalid or any template has null/blank defects. */
    public boolean hasHardDefects() {
      return !getManifestErrorFindings().isEmpty() || !getHardBindingDefectFindings().isEmpty();
    }

    /**
     * Multi-line operator summary: totals plus one row per non-clean finding.
     *
     * @return summary text (never null)
     */
    public String summarize() {
      StringBuilder sb = new StringBuilder();
      sb.append("fleet binding-order sweep over ").append(packagesRoot).append(": ");
      sb.append(getManifestCount()).append(" manifests, ");
      sb.append(getBindingCount()).append(" bindings, ");
      sb.append(getShadowingFindings().size()).append(" shadowing, ");
      sb.append(getHardBindingDefectFindings().size()).append(" hard binding defects, ");
      sb.append(getManifestErrorFindings().size()).append(" manifest errors");
      for (TemplateFinding finding : findings) {
        if (finding.hasManifestErrors()
            || !finding.getResult().isConformant()) {
          sb.append(System.lineSeparator()).append("  ").append(finding.summarize());
        }
      }
      return sb.toString();
    }
  }

  /**
   * CLI entry for local / operator use (non-zero exit when hard defects are found).
   *
   * <p>Usage: {@code PSFleetBindingOrderConformance <packagesRoot>}
   *
   * @param args first arg = packages root path
   * @throws IOException on traversal failure
   */
  public static void main(String[] args) throws IOException {
    if (args == null || args.length < 1 || args[0] == null || args[0].isBlank()) {
      System.err.println("Usage: PSFleetBindingOrderConformance <packagesRoot>");
      System.err.println(
          "  packagesRoot e.g. modules/perc-packages/src/main/resources/Packages");
      System.exit(2);
      return;
    }
    Report report = scan(Path.of(args[0]));
    System.out.println(report.summarize());
    if (report.hasHardDefects()) {
      System.exit(1);
    }
  }
}
