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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Dry-run migration of legacy definition XML ({@code *.templateDef} / {@code
 * <assembly-template>}) to modern Component Package Manifests (issue #5863 / parent #2626,
 * vertical slice 2).
 *
 * <p>Given legacy {@code *.templateDef} files, this class simulates the full upgrade path —
 * parse ({@link PSPageXmlParser}), compile ({@link PSPageXmlCompiler}), then read back the
 * normalized assembler model ({@link PSPageXmlManifestReadback}) and check it against the legacy
 * install-XML oracle ({@link PSPageXmlTemplateDefEmitter}) — <strong>without writing
 * anything</strong>. {@link PSPageXmlCompiler#writeArtifacts} is never called here; the compile
 * result lives only in memory.
 *
 * <p>The resulting {@link Report} is the per-package migration status panel: for every input
 * file, what would change (would-be manifest id, template type, assembler, slot / binding counts)
 * and what blocks (parse / compile / validation / parity reason). {@link Report#toMarkdown()}
 * renders the panel as Markdown for operator tooling and issue evidence; {@link Report#toText()}
 * renders a plain-text variant for CLI use. {@link Report#writeTo(Path)} persists both
 * renderings under an operator-chosen directory so upgrade dry-runs can be archived and
 * audited without touching the scanned legacy inputs.
 *
 * <p>Fallback-path removal behind migrated definitions (issue #5864 / parent #2626): the
 * manifest readback resolves the assembler strictly against the registered product catalog, so
 * a legacy definition whose assembler has no catalog entry surfaces here as {@link Status#BLOCKED}
 * (unregistered-assembler reason) instead of silently defaulting. The legacy-upgrade compile path
 * itself is untouched — it still parses and compiles customer XML with site-local custom
 * assemblers; only the modern-only readback fails closed.
 */
public final class PSPageXmlMigrationDryRun {

  /** Per-package migration verdict. */
  public enum Status {
    /** Legacy XML parses, compiles, validates, and round-trips through the manifest path. */
    READY,
    /** Migration cannot proceed; see {@link Entry#getBlockReason()}. */
    BLOCKED
  }

  /** Dry-run verdict for one legacy {@code *.templateDef} file. */
  public static final class Entry {
    private final String fileName;
    private final String stem;
    private final Status status;
    private final String blockReason;
    private final String manifestId;
    private final String templateType;
    private final String assembler;
    private final int slotCount;
    private final int bindingCount;
    private final int regionCount;

    private Entry(
        String fileName,
        String stem,
        Status status,
        String blockReason,
        String manifestId,
        String templateType,
        String assembler,
        int slotCount,
        int bindingCount,
        int regionCount) {
      this.fileName = fileName;
      this.stem = stem;
      this.status = status;
      this.blockReason = blockReason;
      this.manifestId = manifestId;
      this.templateType = templateType;
      this.assembler = assembler;
      this.slotCount = slotCount;
      this.bindingCount = bindingCount;
      this.regionCount = regionCount;
    }

    /** Legacy file name (never null). */
    public String getFileName() {
      return fileName;
    }

    /** Component stem derived from the file name (never null). */
    public String getStem() {
      return stem;
    }

    /** Migration verdict (never null). */
    public Status getStatus() {
      return status;
    }

    /** Why migration is blocked; null when {@link #getStatus()} is {@link Status#READY}. */
    public String getBlockReason() {
      return blockReason;
    }

    /** Would-be manifest id; null when blocked before compile. */
    public String getManifestId() {
      return manifestId;
    }

    /** Would-be modern template type; null when blocked before compile. */
    public String getTemplateType() {
      return templateType;
    }

    /** Would-be short manifest assembler id; null when blocked before compile. */
    public String getAssembler() {
      return assembler;
    }

    /** Would-be manifest slot count (0 when blocked before compile). */
    public int getSlotCount() {
      return slotCount;
    }

    /** Would-be manifest binding count (0 when blocked before compile). */
    public int getBindingCount() {
      return bindingCount;
    }

    /** Region holes discovered in the template body (0 when blocked before parse). */
    public int getRegionCount() {
      return regionCount;
    }

    /** True when this package would migrate cleanly. */
    public boolean isReady() {
      return status == Status.READY;
    }
  }

  /** Per-package dry-run report; the migration status panel. */
  public static final class Report {
    /** File name used by {@link #writeTo(Path)} for the Markdown panel. */
    public static final String MARKDOWN_FILE_NAME = "migration-dry-run.md";

    /** File name used by {@link #writeTo(Path)} for the plain-text panel. */
    public static final String TEXT_FILE_NAME = "migration-dry-run.txt";

    private final List<Entry> entries;

    private Report(List<Entry> entries) {
      this.entries = Collections.unmodifiableList(new ArrayList<>(entries));
    }

    /** Per-package entries in file-name order (never null). */
    public List<Entry> getEntries() {
      return entries;
    }

    /** Number of {@link Status#READY} entries. */
    public long getReadyCount() {
      return entries.stream().filter(Entry::isReady).count();
    }

    /** Number of {@link Status#BLOCKED} entries. */
    public long getBlockedCount() {
      return entries.stream().filter(e -> !e.isReady()).count();
    }

    /** Total number of scanned files. */
    public int getTotalCount() {
      return entries.size();
    }

    /** True when every scanned package is {@link Status#READY} (vacuously true when empty). */
    public boolean isClean() {
      return getBlockedCount() == 0;
    }

    /**
     * Render the status panel as Markdown (one row per package).
     *
     * @return Markdown report text with {@code \n} line separators (never null)
     */
    public String toMarkdown() {
      StringBuilder sb = new StringBuilder();
      sb.append("# Legacy definition-XML migration dry-run\n\n");
      sb.append("Ready: ")
          .append(getReadyCount())
          .append("  Blocked: ")
          .append(getBlockedCount())
          .append("  Total: ")
          .append(getTotalCount())
          .append("\n\n");
      sb.append("| Package (file) | Status | Would-be manifest | Assembler | Slots | Bindings | Blocks |\n");
      sb.append("|---|---|---|---|---|---|---|\n");
      for (Entry e : entries) {
        sb.append("| ")
            .append(escapeCell(e.getFileName()))
            .append(" | ")
            .append(e.getStatus())
            .append(" | ")
            .append(escapeCell(e.getManifestId() != null ? e.getManifestId() : "—"))
            .append(" | ")
            .append(escapeCell(e.getAssembler() != null ? e.getAssembler() : "—"))
            .append(" | ")
            .append(e.getSlotCount())
            .append(" | ")
            .append(e.getBindingCount())
            .append(" | ")
            .append(escapeCell(e.getBlockReason() != null ? e.getBlockReason() : "—"))
            .append(" |\n");
      }
      return sb.toString();
    }

    /**
     * Render the status panel as plain text (one line per package) for CLI use.
     *
     * @return plain-text report with {@code \n} line separators (never null)
     */
    public String toText() {
      StringBuilder sb = new StringBuilder();
      sb.append("Legacy definition-XML migration dry-run: ready=")
          .append(getReadyCount())
          .append(" blocked=")
          .append(getBlockedCount())
          .append(" total=")
          .append(getTotalCount())
          .append("\n");
      for (Entry e : entries) {
        sb.append(e.getStatus()).append(" ").append(e.getFileName());
        if (e.getManifestId() != null) {
          sb.append(" -> ").append(e.getManifestId());
        }
        if (e.getBlockReason() != null) {
          sb.append(" : ").append(singleLine(e.getBlockReason()));
        }
        sb.append("\n");
      }
      return sb.toString();
    }

    private static String escapeCell(String value) {
      return value.replace("|", "\\|").replace("\r", " ").replace("\n", " ");
    }

    private static String singleLine(String value) {
      return value.replace("\r", " ").replace("\n", " ").trim();
    }

    /**
     * Persist this report for upgrade-run archival: writes {@link #toMarkdown()} and {@link
     * #toText()} as {@link #MARKDOWN_FILE_NAME} / {@link #TEXT_FILE_NAME} under {@code
     * outputDir} (created with parents when missing), then returns both files in write order.
     * Re-running overwrites the previous pair; the scanned legacy inputs are never touched.
     *
     * @param outputDir non-null directory that receives the report pair
     * @return the two written files, Markdown first (never null)
     * @throws IOException on directory-creation or write failure
     */
    public List<Path> writeTo(Path outputDir) throws IOException {
      Objects.requireNonNull(outputDir, "outputDir");
      Files.createDirectories(outputDir);
      Path markdown = outputDir.resolve(MARKDOWN_FILE_NAME);
      Path text = outputDir.resolve(TEXT_FILE_NAME);
      writeMarkdown(markdown);
      writeText(text);
      return List.of(markdown, text);
    }

    /**
     * Write {@link #toMarkdown()} to {@code file} as UTF-8, creating missing parents.
     *
     * @param file non-null destination file
     * @throws IOException on directory-creation or write failure
     */
    public void writeMarkdown(Path file) throws IOException {
      Objects.requireNonNull(file, "file");
      writePanel(file, toMarkdown());
    }

    /**
     * Write {@link #toText()} to {@code file} as UTF-8, creating missing parents.
     *
     * @param file non-null destination file
     * @throws IOException on directory-creation or write failure
     */
    public void writeText(Path file) throws IOException {
      Objects.requireNonNull(file, "file");
      writePanel(file, toText());
    }

    private static void writePanel(Path file, String panel) throws IOException {
      Path parent = file.toAbsolutePath().getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }
      Files.writeString(file, panel, StandardCharsets.UTF_8);
    }
  }

  private PSPageXmlMigrationDryRun() {
    // utility
  }

  /**
   * Dry-run every {@code *.templateDef} file directly under {@code legacyDir}, in file-name
   * order. Writes nothing.
   *
   * @param legacyDir non-null directory of legacy definition XML
   * @return per-package report (never null; empty when no {@code *.templateDef} files found)
   * @throws IOException on directory-listing failure
   */
  public static Report dryRun(Path legacyDir) throws IOException {
    Objects.requireNonNull(legacyDir, "legacyDir");
    List<Path> files = new ArrayList<>();
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(legacyDir, "*.templateDef")) {
      for (Path p : stream) {
        if (Files.isRegularFile(p)) {
          files.add(p);
        }
      }
    }
    files.sort(Comparator.comparing(p -> p.getFileName().toString()));
    return dryRun(files);
  }

  /**
   * Dry-run the given legacy {@code *.templateDef} files, in iteration order. Writes nothing.
   *
   * @param templateDefPaths non-null legacy definition files (null entries are skipped)
   * @return per-package report (never null)
   */
  public static Report dryRun(Collection<Path> templateDefPaths) {
    Objects.requireNonNull(templateDefPaths, "templateDefPaths");
    List<Entry> entries = new ArrayList<>();
    for (Path p : templateDefPaths) {
      if (p == null) {
        continue;
      }
      entries.add(dryRunOne(p));
    }
    return new Report(entries);
  }

  /**
   * Dry-run a single legacy {@code *.templateDef} file: parse → compile (in memory only) →
   * manifest readback → parity against the legacy install-XML oracle. Writes nothing.
   *
   * @param templateDefPath non-null legacy definition file
   * @return verdict entry (never null; failures surface as {@link Status#BLOCKED}, never thrown)
   */
  public static Entry dryRunOne(Path templateDefPath) {
    Objects.requireNonNull(templateDefPath, "templateDefPath");
    Path fileName = templateDefPath.getFileName();
    String name = fileName != null ? fileName.toString() : templateDefPath.toString();
    String stem = stemOf(name);
    try {
      PSPageXmlModel model = PSPageXmlParser.parse(templateDefPath);
      int regions = model.getRegionHoles() != null ? model.getRegionHoles().size() : 0;
      PSPageXmlCompileResult compiled = PSPageXmlCompiler.compile(model, null);
      PSComponentPackageManifest manifest = compiled.getManifest();
      Map<String, String> artifacts = compiled.getTextArtifacts();
      String source = artifacts.values().iterator().next();
      PSPageXmlModel readback = PSPageXmlManifestReadback.readback(manifest, artifacts);

      String guid = model.getGuid();
      String installXml = PSPageXmlTemplateDefEmitter.emit(manifest, source, guid);
      String guidForOracle = guid != null && !guid.isBlank() ? guid.trim() : null;
      PSPageXmlModel oracle = PSPageXmlParser.parse(installXml);
      // The emitter omits a blank GUID as empty; normalize both sides before comparing.
      String mismatch = firstMismatch(withGuid(oracle, guidForOracle), withGuid(readback, guidForOracle));
      if (mismatch != null) {
        return blocked(
            name, stem, regions, "readback parity mismatch: " + mismatch);
      }

      PSComponentPackageManifest.TemplateRef template = manifest.getTemplates().get(0);
      int slots = manifest.getSlots() != null ? manifest.getSlots().size() : 0;
      int bindings = template.getBindings() != null ? template.getBindings().size() : 0;
      return new Entry(
          name,
          stem,
          Status.READY,
          null,
          manifest.getId(),
          template.getType(),
          template.getAssembler(),
          slots,
          bindings,
          regions);
    } catch (PSPageXmlException | IOException | RuntimeException e) {
      // Best-effort reporting: any single-file failure (parse, compile, validation, parity
      // oracle, or unexpected runtime) is a BLOCKED verdict, never an aborted scan.
      String reason = e.getMessage() != null && !e.getMessage().isBlank()
          ? e.getMessage().trim()
          : e.getClass().getSimpleName();
      return blocked(name, stem, 0, reason);
    }
  }

  private static Entry blocked(
      String fileName, String stem, int regions, String reason) {
    return new Entry(fileName, stem, Status.BLOCKED, reason, null, null, null, 0, 0, regions);
  }

  private static PSPageXmlModel withGuid(PSPageXmlModel model, String guid) {
    model.setGuid(guid);
    return model;
  }

  private static String stemOf(String fileName) {
    String lower = fileName.toLowerCase(Locale.ROOT);
    if (lower.endsWith(".templatedef")) {
      return fileName.substring(0, fileName.length() - ".templateDef".length());
    }
    int dot = fileName.lastIndexOf('.');
    return dot > 0 ? fileName.substring(0, dot) : fileName;
  }

  /** First field-for-field difference between oracle and readback, or null when they agree. */
  static String firstMismatch(PSPageXmlModel expected, PSPageXmlModel actual) {
    String mismatch = firstIdentityMismatch(expected, actual);
    if (mismatch != null) {
      return mismatch;
    }
    mismatch = firstInstallMismatch(expected, actual);
    if (mismatch != null) {
      return mismatch;
    }
    mismatch = firstBindingMismatch(expected, actual);
    if (mismatch != null) {
      return mismatch;
    }
    return firstRegionMismatch(expected, actual);
  }

  private static String check(String field, Object expected, Object actual) {
    return Objects.equals(expected, actual)
        ? null
        : field + " expected=" + expected + " actual=" + actual;
  }

  private static String firstIdentityMismatch(PSPageXmlModel expected, PSPageXmlModel actual) {
    String mismatch = check("name", expected.getName(), actual.getName());
    if (mismatch != null) {
      return mismatch;
    }
    mismatch = check("label", expected.getLabel(), actual.getLabel());
    if (mismatch != null) {
      return mismatch;
    }
    mismatch = check("description", expected.getDescription(), actual.getDescription());
    if (mismatch != null) {
      return mismatch;
    }
    mismatch = check("guid", expected.getGuid(), actual.getGuid());
    if (mismatch != null) {
      return mismatch;
    }
    mismatch = check("assembler", expected.getAssembler(), actual.getAssembler());
    if (mismatch != null) {
      return mismatch;
    }
    mismatch = check("outputFormat", expected.getOutputFormat(), actual.getOutputFormat());
    if (mismatch != null) {
      return mismatch;
    }
    return check("templateType", expected.getTemplateType(), actual.getTemplateType());
  }

  private static String firstInstallMismatch(PSPageXmlModel expected, PSPageXmlModel actual) {
    String mismatch = check("mimeType", expected.getMimeType(), actual.getMimeType());
    if (mismatch != null) {
      return mismatch;
    }
    mismatch = check("charset", expected.getCharset(), actual.getCharset());
    if (mismatch != null) {
      return mismatch;
    }
    mismatch =
        check(
            "activeAssemblyType",
            expected.getActiveAssemblyType(),
            actual.getActiveAssemblyType());
    if (mismatch != null) {
      return mismatch;
    }
    mismatch = check("publishWhen", expected.getPublishWhen(), actual.getPublishWhen());
    if (mismatch != null) {
      return mismatch;
    }
    mismatch =
        check("locationPrefix", expected.getLocationPrefix(), actual.getLocationPrefix());
    if (mismatch != null) {
      return mismatch;
    }
    mismatch =
        check("locationSuffix", expected.getLocationSuffix(), actual.getLocationSuffix());
    if (mismatch != null) {
      return mismatch;
    }
    return check("templateBody", expected.getTemplateBody(), actual.getTemplateBody());
  }

  private static String firstBindingMismatch(PSPageXmlModel expected, PSPageXmlModel actual) {
    List<PSPageXmlModel.Binding> eb =
        expected.getBindings() != null ? expected.getBindings() : List.of();
    List<PSPageXmlModel.Binding> ab =
        actual.getBindings() != null ? actual.getBindings() : List.of();
    if (!eb.equals(ab)) {
      return "bindings size expected=" + eb.size() + " actual=" + ab.size();
    }
    return null;
  }

  private static String firstRegionMismatch(PSPageXmlModel expected, PSPageXmlModel actual) {
    List<PSPageXmlModel.RegionHole> er =
        expected.getRegionHoles() != null ? expected.getRegionHoles() : List.of();
    List<PSPageXmlModel.RegionHole> ar =
        actual.getRegionHoles() != null ? actual.getRegionHoles() : List.of();
    if (er.size() != ar.size()) {
      return "regionHoles size expected=" + er.size() + " actual=" + ar.size();
    }
    for (int i = 0; i < er.size(); i++) {
      if (regionHoleDiffers(er.get(i), ar.get(i))) {
        return "regionHoles[" + i + "] mismatch";
      }
    }
    return null;
  }

  private static boolean regionHoleDiffers(
      PSPageXmlModel.RegionHole expected, PSPageXmlModel.RegionHole actual) {
    return !Objects.equals(expected.getRegionId(), actual.getRegionId())
        || !Objects.equals(expected.getCssClass(), actual.getCssClass())
        || !Objects.equals(expected.getLayoutHints(), actual.getLayoutHints())
        || !Objects.equals(expected.getStyleHints(), actual.getStyleHints());
  }
}
