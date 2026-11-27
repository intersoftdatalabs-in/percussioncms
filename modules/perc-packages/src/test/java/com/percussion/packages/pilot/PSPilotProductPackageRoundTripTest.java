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

package com.percussion.packages.pilot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.packages.gadgetxml.PSGadgetDefinitionXmlInventory;
import com.percussion.packages.inventory.PSDefinitionXmlShipPathInventory;
import com.percussion.packages.manifest.PSComponentPackageManifest;
import com.percussion.packages.manifest.PSComponentPackageManifestIo;
import com.percussion.packages.manifest.PSComponentPackageManifestValidator;
import com.percussion.packages.pagexml.PSPageDefinitionXmlInventory;
import com.percussion.packages.pagexml.PSPageXmlBindingConformance;
import com.percussion.packages.pagexml.PSPageXmlDualShip;
import com.percussion.packages.pagexml.PSPageXmlInstallArtifact;
import com.percussion.packages.pagexml.PSPageXmlInstallMode;
import com.percussion.packages.pagexml.PSPageXmlInstallPolicy;
import com.percussion.packages.pagexml.PSPageXmlManifestReadback;
import com.percussion.packages.pagexml.PSPageXmlMigrationDryRun;
import com.percussion.packages.pagexml.PSPageXmlModel;
import com.percussion.packages.pagexml.PSPageXmlNativeInstall;
import com.percussion.packages.pagexml.PSPageXmlParser;
import com.percussion.packages.pagexml.PSSlotLayoutStylesConformance;
import com.percussion.packages.widgetxml.PSWidgetDefinitionXmlInventory;
import com.percussion.packages.widgetxml.PSWidgetXmlCompileResult;
import com.percussion.packages.widgetxml.PSWidgetXmlInstallEmitter;
import com.percussion.packages.widgetxml.PSWidgetXmlModel;
import com.percussion.packages.widgetxml.PSWidgetXmlPackageCompiler;
import com.percussion.packages.widgetxml.PSWidgetXmlParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Pilot product-package round-trip proof (issue #7307 / parent #2626, slice next-c).
 *
 * <p>{@code perc.FileAssetWidget} is the single-template pilot: one binary page template
 * ({@code perc.fileBinary}, {@code TemplateDef-607}) plus one widget ({@code percFile}), authored
 * as modern {@code pages/} + {@code widgets/} manifests with {@code page.installMode=native} and
 * zero Page/Widget definition XML. This test exercises the real product package directory (not
 * synthetic fixtures) end to end:
 *
 * <ol>
 *   <li>native install staging ({@code PSPageXmlNativeInstall}),
 *   <li>manifest readback ({@code PSPageXmlManifestReadback}),
 *   <li>migration dry-run READY on the staged install XML ({@code PSPageXmlMigrationDryRun}),
 *   <li>M1 recount — zero definition XML on the pilot path,
 *   <li>widget compile + install-XML materialization, and
 *   <li>next-a / next-b conformance gates on the pilot manifests.
 * </ol>
 *
 * <p>Operators converting the next package copy this sequence; the walkthrough lives under
 * {@code product-docs/8.2/developer/page-package-pilot.md}.
 *
 * <p>Cross-platform: all path construction uses {@link Path#resolve(String)} / {@link Files} —
 * no hardcoded filesystem separators.
 */
class PSPilotProductPackageRoundTripTest {

  /** Pilot package directory name under the product {@code Packages} tree. */
  static final String PILOT_PACKAGE_DIR = "perc.FileAssetWidget";

  /** Pilot page stem and its stable archive GUID (from the package mapping). */
  static final String PILOT_PAGE_STEM = "perc.fileBinary";

  static final String PILOT_PAGE_ARCHIVE_FOLDER = "TemplateDef-607";
  static final String PILOT_PAGE_GUID = "0-4-607";

  /** Pilot widget manifest id. */
  static final String PILOT_WIDGET_ID = "percFile";

  @TempDir Path tempDir;

  private Path stagingRoot;
  private Path pilotPackageDir;

  @BeforeEach
  void stagePilotPackage() throws IOException {
    Path source = locatePilotPackageDir();
    assertNotNull(
        source, "Pilot package must be visible under perc-packages Surefire: " + PILOT_PACKAGE_DIR);
    stagingRoot = tempDir.resolve("staging");
    pilotPackageDir = stagingRoot.resolve(PILOT_PACKAGE_DIR);
    copyTree(source, pilotPackageDir);
  }

  @AfterEach
  void clearSysProps() {
    System.clearProperty(PSPageXmlInstallPolicy.SYS_PROP_INSTALL_MODE);
    System.clearProperty(PSPageXmlInstallPolicy.SYS_PROP_DUAL_SHIP);
  }

  @Test
  void pilotPackage_resolvesToNativeInstall() throws IOException {
    assertEquals(PSPageXmlInstallMode.NATIVE, PSPageXmlInstallPolicy.resolve(pilotPackageDir));
    assertTrue(PSPageXmlInstallPolicy.isNativeInstallEnabled(pilotPackageDir));
    assertFalse(PSPageXmlInstallPolicy.isDualShipEnabled(pilotPackageDir));
  }

  @Test
  void pilotPackage_zeroDefinitionXmlOnPilotPath() throws IOException {
    // The pilot ships no legacy install Widget XML beside its modern widgets/ sources.
    assertFalse(PSWidgetXmlInstallEmitter.hasCommittedWidgetXml(pilotPackageDir));

    // No root *.templateDef files: native install stages archive TemplateDef-N/ folders only.
    try (Stream<Path> entries = Files.list(pilotPackageDir)) {
      List<String> templateDefs =
          entries
              .filter(Files::isRegularFile)
              .map(p -> p.getFileName().toString())
              .filter(name -> name.endsWith(".templateDef"))
              .toList();
      assertTrue(
          templateDefs.isEmpty(),
          "Pilot package must not commit root *.templateDef: " + templateDefs);
    }

    // M1 recount scoped to the pilot path: scan a Packages root holding only the pilot.
    Path packagesRoot = tempDir.resolve("packages-root");
    Files.createDirectories(packagesRoot);
    copyTree(pilotPackageDir, packagesRoot.resolve(PILOT_PACKAGE_DIR));

    assertTrue(
        PSWidgetDefinitionXmlInventory.scan(packagesRoot).isClean(),
        "M1: zero Widget definition XML on the pilot path");
    PSPageDefinitionXmlInventory.assertNoNonWaivedPageDefinitionXml(packagesRoot);
    PSGadgetDefinitionXmlInventory.assertNoNonWaivedGadgetDefinitionXml(packagesRoot);
    PSDefinitionXmlShipPathInventory.assertNoNonWaivedPageOrGadgetDefinitionXml(packagesRoot);
  }

  @Test
  void pilotPage_nativeInstallStagesArchiveTemplateDef() throws Exception {
    List<PSPageXmlInstallArtifact> artifacts =
        PSPageXmlNativeInstall.listInstallArtifacts(pilotPackageDir);
    assertEquals(1, artifacts.size(), "Pilot package carries exactly one page template");
    PSPageXmlInstallArtifact artifact = artifacts.get(0);
    assertEquals(PILOT_PAGE_STEM, artifact.stem());
    assertEquals(PILOT_PAGE_STEM + ".templateDef", artifact.rootFileName());
    assertEquals(PILOT_PAGE_ARCHIVE_FOLDER, artifact.archiveFolder());

    Path archiveRoot = tempDir.resolve("archive");
    Files.createDirectories(archiveRoot);
    assertEquals(1, PSPageXmlNativeInstall.stageArchiveTemplateDefs(pilotPackageDir, archiveRoot));

    // Native mode must not require dual-ship roots next to the modern sources.
    assertFalse(
        Files.isRegularFile(pilotPackageDir.resolve(PILOT_PAGE_STEM + ".templateDef")));

    Path staged = archiveRoot.resolve(PILOT_PAGE_ARCHIVE_FOLDER).resolve(artifact.rootFileName());
    assertTrue(Files.isRegularFile(staged), "archive TemplateDef folder layout");

    // The staged install XML is deployer-consumable: stable GUID, binary assembler, same body.
    PSPageXmlModel install = PSPageXmlParser.parse(staged);
    assertEquals(PILOT_PAGE_GUID, install.getGuid());
    assertEquals(PILOT_PAGE_STEM, install.getName());
    assertTrue(
        install.getAssembler().endsWith("/binaryAssembler"),
        "staged assembler: " + install.getAssembler());

    Path templateSource =
        pilotPackageDir
            .resolve(PSPageXmlDualShip.PAGES_DIR_NAME)
            .resolve(PILOT_PAGE_STEM)
            .resolve("templates")
            .resolve(PILOT_PAGE_STEM + ".vm");
    assertEquals(
        normalizeNewlines(Files.readString(templateSource, StandardCharsets.UTF_8)),
        normalizeNewlines(install.getTemplateBody()));
  }

  @Test
  void pilotPage_manifestReadbackRebuildsDefinition() throws Exception {
    Path pageDir =
        pilotPackageDir.resolve(PSPageXmlDualShip.PAGES_DIR_NAME).resolve(PILOT_PAGE_STEM);

    PSComponentPackageManifest manifest =
        PSComponentPackageManifestIo.read(
            pageDir.resolve(PSComponentPackageManifest.DEFAULT_MANIFEST_FILE_NAME));
    PSComponentPackageManifestValidator.validate(manifest);
    assertEquals(1, manifest.getTemplates().size(), "pilot keeps one template per manifest");
    assertTrue(manifest.getSlots().isEmpty(), "binary pilot page declares no slots");

    PSPageXmlModel readback = PSPageXmlManifestReadback.readback(pageDir, PILOT_PAGE_GUID);
    assertEquals(PILOT_PAGE_GUID, readback.getGuid());
    assertEquals(PILOT_PAGE_STEM, readback.getName());
    assertEquals("Java/global/percussion/assembly/binaryAssembler", readback.getAssembler());
    // Binary-ness travels in the assembler, not the legacy type label: manifests without
    // legacyTemplateType read back to the legacy "Shared" default (same as install emit).
    assertEquals("Shared", readback.getTemplateType());
    // Binary asset templates omit mime type and charset on install emit and readback.
    assertNull(readback.getMimeType());
    assertNull(readback.getCharset());

    // Ordered JEXL bindings survive the manifest path in document order (next-a contract).
    List<String> variables =
        readback.getBindings().stream().map(PSPageXmlModel.Binding::getVariable).toList();
    assertEquals(
        Arrays.asList(
            "$sys.binary",
            "$sys.mimetype",
            "$sys.metadata.linktext",
            "$sys.metadata.dcterms:created",
            "$sys.metadata.dcterms:title",
            "$sys.metadata.dcterms:format"),
        variables);
  }

  @Test
  void pilotPage_bindingOrderConformance() throws Exception {
    PSComponentPackageManifest manifest = readPilotPageManifest();
    List<PSComponentPackageManifest.Binding> bindings =
        manifest.getTemplates().get(0).getBindings();

    PSPageXmlBindingConformance.Result result =
        PSPageXmlBindingConformance.checkManifestBindings(PILOT_PAGE_STEM, bindings);
    assertTrue(result.isConformant(), result::summarize);
    assertEquals(6, result.getBindingCount());
    assertTrue(result.getDuplicateVariables().isEmpty());
    assertEquals(0, result.getNullEntryCount());
    assertEquals(0, result.getBlankVariableCount());
  }

  @Test
  void pilotPage_stagedInstallXmlDryRunsReady() throws Exception {
    Path archiveRoot = tempDir.resolve("archive");
    Files.createDirectories(archiveRoot);
    PSPageXmlNativeInstall.stageArchiveTemplateDefs(pilotPackageDir, archiveRoot);

    // The dry-run consumes legacy-format install XML: feed it the native-staged output to prove
    // the emitted install XML round-trips through the manifest path.
    Path legacyDir = tempDir.resolve("legacy");
    Files.createDirectories(legacyDir);
    Files.copy(
        archiveRoot
            .resolve(PILOT_PAGE_ARCHIVE_FOLDER)
            .resolve(PILOT_PAGE_STEM + ".templateDef"),
        legacyDir.resolve(PILOT_PAGE_STEM + ".templateDef"),
        StandardCopyOption.REPLACE_EXISTING);

    PSPageXmlMigrationDryRun.Report report = PSPageXmlMigrationDryRun.dryRun(legacyDir);
    assertEquals(1, report.getTotalCount());
    assertEquals(1, report.getReadyCount());
    assertEquals(0, report.getBlockedCount());
    assertTrue(report.isClean());

    PSPageXmlMigrationDryRun.Entry entry = report.getEntries().get(0);
    assertEquals(PSPageXmlMigrationDryRun.Status.READY, entry.getStatus());
    assertNull(entry.getBlockReason());
    assertEquals(PILOT_PAGE_STEM, entry.getManifestId());
    assertEquals("binary", entry.getTemplateType());
    assertEquals("binaryAssembler", entry.getAssembler());
    assertEquals(6, entry.getBindingCount());
  }

  @Test
  void pilotWidget_compilesAndMaterializesInstallXml() throws Exception {
    List<PSWidgetXmlCompileResult> compiled =
        PSWidgetXmlPackageCompiler.compilePackage(pilotPackageDir);
    assertEquals(1, compiled.size(), "Pilot package carries exactly one widget");
    assertEquals(PILOT_WIDGET_ID, compiled.get(0).getManifest().getId());
    PSComponentPackageManifestValidator.validate(compiled.get(0).getManifest());

    // Materialize onto the staging copy (never the source tree): the pilot ships zero
    // committed install Widget XML, so the emitter writes exactly one file.
    assertEquals(1, PSWidgetXmlInstallEmitter.materializeInstallWidgetXml(pilotPackageDir));
    assertTrue(PSWidgetXmlInstallEmitter.hasCommittedWidgetXml(pilotPackageDir));

    Path emitted = installWidgetsDir(pilotPackageDir).resolve(PILOT_WIDGET_ID + ".xml");
    assertTrue(Files.isRegularFile(emitted));
    PSWidgetXmlModel model = PSWidgetXmlParser.parse(emitted);
    assertEquals(PILOT_WIDGET_ID, model.widgetStem());
  }

  @Test
  void pilotWidget_slotLayoutStylesConformant() throws Exception {
    PSComponentPackageManifest manifest = readPilotWidgetManifest();
    assertEquals(1, manifest.getSlots().size());
    PSComponentPackageManifest.SlotRef slot = manifest.getSlots().get(0);
    assertEquals(PILOT_WIDGET_ID + "Content", slot.getName());

    // Empty layout map is informational only; the scalar styles value is conformant
    // (next-b contract: layout keys closed, styles values scalar).
    PSSlotLayoutStylesConformance.Result result =
        PSSlotLayoutStylesConformance.check(slot.getName(), slot.getLayout(), slot.getStyles());
    assertTrue(result.isConformant(), result::summarize);
    assertTrue(result.isLayoutEmpty());
    assertEquals(List.of("rootclass"), result.getStylesKeysInOrder());
  }

  @Test
  void pilotWidget_snippetShadowingSurfacedInDocumentOrder() throws Exception {
    // The pilot snippet reassigns JEXL variables across conditional branches ($target,
    // $rootclass, $link): later entries shadow earlier ones. The next-a gate must surface
    // that shadowing in document order — never sort it away.
    PSComponentPackageManifest manifest = readPilotWidgetManifest();
    List<PSComponentPackageManifest.Binding> bindings =
        manifest.getTemplates().get(0).getBindings();

    PSPageXmlBindingConformance.Result result =
        PSPageXmlBindingConformance.checkManifestBindings(PILOT_WIDGET_ID + "Snippet", bindings);
    assertEquals(
        Arrays.asList(
            "props",
            "referalPolicy",
            "rootclass",
            "rootclass",
            "linkNewWindow",
            "target",
            "target",
            "linkContext",
            "assetItems",
            "assetItem",
            "link",
            "title",
            "link",
            "dsUrl",
            "dynamicListData",
            "__widgetCode"),
        result.getVariablesInOrder());
    assertEquals(Arrays.asList("rootclass", "target", "link"), result.getDuplicateVariables());
    assertEquals(0, result.getNullEntryCount());
    assertEquals(0, result.getBlankVariableCount());
  }

  private PSComponentPackageManifest readPilotPageManifest() throws Exception {
    Path pageDir =
        pilotPackageDir.resolve(PSPageXmlDualShip.PAGES_DIR_NAME).resolve(PILOT_PAGE_STEM);
    return PSComponentPackageManifestIo.read(
        pageDir.resolve(PSComponentPackageManifest.DEFAULT_MANIFEST_FILE_NAME));
  }

  private PSComponentPackageManifest readPilotWidgetManifest() throws Exception {
    // Modern widget sources live under widgets/<id>/ (sibling layout, not the install dir).
    Path manifestPath =
        pilotPackageDir
            .resolve("widgets")
            .resolve(PILOT_WIDGET_ID)
            .resolve(PSComponentPackageManifest.DEFAULT_MANIFEST_FILE_NAME);
    return PSComponentPackageManifestIo.read(manifestPath);
  }

  /**
   * Install-path Widgets directory ({@code sys__UserDependency--rxconfig/Widgets}) resolved with
   * portable segments — mirrors {@code PSWidgetXmlPackageCompiler} layout without touching its
   * package-private resolver.
   */
  private static Path installWidgetsDir(Path packageDir) {
    return packageDir.resolve("sys__UserDependency--rxconfig").resolve("Widgets");
  }

  private static Path locatePilotPackageDir() {
    Path candidate = Path.of("src", "main", "resources", "Packages", PILOT_PACKAGE_DIR);
    if (Files.isDirectory(candidate)) {
      return candidate.toAbsolutePath().normalize();
    }
    Path cwd = Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
    Path alt =
        cwd.resolve("src")
            .resolve("main")
            .resolve("resources")
            .resolve("Packages")
            .resolve(PILOT_PACKAGE_DIR);
    return Files.isDirectory(alt) ? alt : null;
  }

  private static void copyTree(Path source, Path target) throws IOException {
    try (Stream<Path> stream = Files.walk(source)) {
      for (Path from : stream.toList()) {
        Path to = target.resolve(source.relativize(from).toString());
        if (Files.isDirectory(from)) {
          Files.createDirectories(to);
        } else {
          Files.createDirectories(to.getParent());
          Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
        }
      }
    }
  }

  private static String normalizeNewlines(String text) {
    return text.replace("\r\n", "\n");
  }
}
