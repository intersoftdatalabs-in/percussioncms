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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.packages.manifest.PSComponentPackageManifest;
import com.percussion.packages.manifest.PSComponentPackageManifestIo;
import com.percussion.packages.manifest.PSComponentPackageManifestValidator;
import com.percussion.packages.shim.PSDefinitionSourceKind;
import com.percussion.packages.shim.PSDefinitionSourceSelection;
import com.percussion.packages.shim.PSLegacyDefinitionXmlShim;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Dual-ship / ship-exit modern widget authoring tests (issues #2831 batch A, #2832 batch B, #2844
 * batch C, #2883/#2884/#2885 batch A+B+C stop-shipping install XML / parent #2630).
 *
 * <p>Batches A/B/C product packages author modern {@code widgets/&lt;stem&gt;/} only (no committed
 * install Widget XML). Selection prefers modern when both exist; install materialization regenerates
 * Widget XML at package-build time for modern-only packages. {@code perc.Test} ship-exit is #3736.
 */
class PSWidgetXmlDualShipTest {

  @TempDir Path tempDir;

  @Test
  void materializeModern_fromWidgetXml_writesComponentPackageAndTemplate() throws Exception {
    Path packageDir = tempDir.resolve("demoPkg");
    Path widgetsXml =
        packageDir
            .resolve("sys__UserDependency--rxconfig")
            .resolve("Widgets");
    Files.createDirectories(widgetsXml);

    // Upgrade-input fixture (product batch A no longer commits Widget XML — #2883).
    try (var in =
        PSWidgetXmlDualShipTest.class.getResourceAsStream("/widgetxml/percSimpleText.xml")) {
      assertNotNull(in, "classpath fixture /widgetxml/percSimpleText.xml");
      Files.write(widgetsXml.resolve("percSimpleText.xml"), in.readAllBytes());
    }

    int written = PSWidgetXmlDualShip.materializeModernWidgetSources(packageDir);
    assertEquals(1, written);
    assertTrue(PSWidgetXmlDualShip.hasModernWidgetSources(packageDir));

    Path modernRoot =
        packageDir.resolve(PSWidgetXmlDualShip.WIDGETS_DIR_NAME).resolve("percSimpleText");
    assertTrue(
        Files.isRegularFile(
            modernRoot.resolve(PSComponentPackageManifest.DEFAULT_MANIFEST_FILE_NAME)));

    PSWidgetXmlCompileResult loaded = PSWidgetXmlDualShip.loadModernAsCompileResult(modernRoot);
    PSComponentPackageManifestValidator.validate(loaded.getManifest());
    assertEquals("percSimpleText", loaded.getManifest().getId());
    assertFalse(loaded.getTextArtifacts().isEmpty());
  }

  @Test
  void productBatchA_modernOnly_noCommittedXml_installMaterializeRoundTrip() throws Exception {
    Path packagesRoot = locatePackagesRoot();
    if (packagesRoot == null) {
      System.err.println("WARN: Packages root not found; skipping product batch A ship-exit test");
      return;
    }

    Set<String> foundStems = new HashSet<>();
    int packagesWithModern = 0;
    int installXmlWritten = 0;

    for (String pkgName : PSWidgetXmlDualShip.BATCH_A_PACKAGE_DIRS) {
      Path product = packagesRoot.resolve(pkgName);
      if (!Files.isDirectory(product)) {
        System.err.println("WARN: missing package " + pkgName + "; soft-skip");
        continue;
      }

      assertTrue(
          PSWidgetXmlDualShip.hasModernWidgetSources(product),
          pkgName + " must author modern widgets/ sources (#2831 batch A)");

      // Ship-exit (#2883): product source no longer commits install Widget XML.
      assertFalse(
          PSWidgetXmlInstallEmitter.hasCommittedWidgetXml(product),
          pkgName + " must not commit sys__UserDependency--rxconfig/Widgets/*.xml (#2883)");
      assertNoAuthoredWidgetXmlArchivePaths(product, pkgName);

      List<PSWidgetXmlCompileResult> fromModern = PSWidgetXmlDualShip.compileModernWidgets(product);
      // compilePackage falls back to modern when XML absent.
      List<PSWidgetXmlCompileResult> fromPackage =
          PSWidgetXmlPackageCompiler.compilePackage(product);
      assertEquals(
          fromModern.size(),
          fromPackage.size(),
          pkgName + " package compile must equal modern widget count");

      Map<String, PSWidgetXmlCompileResult> modernById =
          fromModern.stream()
              .collect(Collectors.toMap(r -> r.getManifest().getId(), r -> r, (a, b) -> a));
      for (String id : modernById.keySet()) {
        foundStems.add(id);
        PSComponentPackageManifestValidator.validate(modernById.get(id).getManifest());
      }

      // Staging materialize: install XML regenerated from modern for deployer wire format.
      Path staging = tempDir.resolve("batch-a-stage-" + pkgName);
      copyTree(product, staging);
      int written = PSWidgetXmlDualShip.materializeInstallWidgetXml(staging);
      assertEquals(
          fromModern.size(),
          written,
          pkgName + " materialize-install must write one XML per modern widget");
      assertTrue(PSWidgetXmlInstallEmitter.hasCommittedWidgetXml(staging));
      assertTrue(
          PSWidgetArchiveManifestInventory.containsWidgetXmlArchivePath(
              Files.readString(
                  staging.resolve(PSWidgetArchiveManifestInventory.ARCHIVE_INFO_FILE_NAME))),
          pkgName + " staging archiveInfo must list Widget XML after install emit (#3582)");
      installXmlWritten += written;

      List<PSWidgetXmlCompileResult> fromMaterialized =
          PSWidgetXmlPackageCompiler.compilePackage(staging);
      assertEquals(fromModern.size(), fromMaterialized.size(), pkgName + " install XML count");

      Map<String, PSWidgetXmlCompileResult> xmlById =
          fromMaterialized.stream()
              .collect(Collectors.toMap(r -> r.getManifest().getId(), r -> r, (a, b) -> a));
      for (Map.Entry<String, PSWidgetXmlCompileResult> e : modernById.entrySet()) {
        String id = e.getKey();
        assertTrue(xmlById.containsKey(id), pkgName + " missing install widget " + id);
        // Core identity + template body parity after reverse emit → recompile.
        assertEquals(
            e.getValue().getManifest().getId(), xmlById.get(id).getManifest().getId());
        assertEquals(
            e.getValue().getManifest().getName(), xmlById.get(id).getManifest().getName());
        String templateKey =
            e.getValue().getManifest().getTemplates().isEmpty()
                ? null
                : e.getValue().getManifest().getTemplates().get(0).getSourceRef();
        if (templateKey != null) {
          assertEquals(
              normalizeNewlines(e.getValue().getTextArtifacts().get(templateKey)),
              normalizeNewlines(xmlById.get(id).getTextArtifacts().get(templateKey)),
              "template parity after install materialize for " + id);
        }
      }

      // Second materialize is a no-op while committed XML is present on staging.
      assertEquals(0, PSWidgetXmlDualShip.materializeInstallWidgetXml(staging));

      // Shim: package root with modern widgets/ prefers modern over legacy XML.
      PSDefinitionSourceSelection sel = PSLegacyDefinitionXmlShim.selectForPackageRoot(product);
      assertEquals(PSDefinitionSourceKind.MODERN_COMPONENT_PACKAGE, sel.getKind(), pkgName);
      assertFalse(
          PSLegacyDefinitionXmlShim.wouldUseLegacyShim(product),
          pkgName + " modern roots should win selection without dual-ship XML");

      packagesWithModern++;
    }

    assertEquals(
        PSWidgetXmlDualShip.BATCH_A_PACKAGE_DIRS.size(),
        packagesWithModern,
        "all batch A packages should be present with modern roots");
    assertEquals(
        new HashSet<>(PSWidgetXmlDualShip.BATCH_A_WIDGET_STEMS),
        foundStems,
        "batch A must cover the 8 named widget stems");
    assertEquals(
        PSWidgetXmlDualShip.BATCH_A_WIDGET_STEMS.size(),
        installXmlWritten,
        "batch A install materialize must cover 8 widgets");
  }

  @Test
  void selectDefinition_prefersNestedWidgetsManifest() throws Exception {
    Path pkg = tempDir.resolve("perc.baseWidgets");
    Path modern =
        pkg.resolve("widgets").resolve("percSimpleText");
    Files.createDirectories(modern);
    Path manifest = modern.resolve(PSLegacyDefinitionXmlShim.MODERN_MANIFEST_FILE_NAME);
    Files.writeString(
        manifest,
        "{\"schemaVersion\":\"1.0\",\"id\":\"percSimpleText\",\"name\":\"Simple Text\",\"version\":\"1.0.0\"}",
        StandardCharsets.UTF_8);

    Path widgetsXml = pkg.resolve("sys__UserDependency--rxconfig").resolve("Widgets");
    Files.createDirectories(widgetsXml);
    Files.writeString(widgetsXml.resolve("percSimpleText.xml"), "<Widget/>", StandardCharsets.UTF_8);

    PSDefinitionSourceSelection s =
        PSLegacyDefinitionXmlShim.selectDefinition(
            "percSimpleText",
            List.of(pkg),
            widgetsXml,
            null,
            null);
    assertEquals(PSDefinitionSourceKind.MODERN_COMPONENT_PACKAGE, s.getKind());
    assertEquals(manifest, s.getPrimaryPath().orElseThrow());
  }

  @Test
  void batchA_packageDirs_disjointFromTestAndSized() {
    assertFalse(PSWidgetXmlDualShip.BATCH_A_PACKAGE_DIRS.contains("perc.Test"));
    assertEquals(5, PSWidgetXmlDualShip.BATCH_A_PACKAGE_DIRS.size());
    assertEquals(8, PSWidgetXmlDualShip.BATCH_A_WIDGET_STEMS.size());
    assertEquals(
        PSWidgetXmlDualShip.BATCH_A_PACKAGE_DIRS,
        PSWidgetXmlPackageCompiler.DUAL_SHIP_BATCH_A_PACKAGE_DIRS);
  }

  @Test
  void productBatchB_modernOnly_noCommittedXml_installMaterializeRoundTrip() throws Exception {
    Path packagesRoot = locatePackagesRoot();
    if (packagesRoot == null) {
      System.err.println("WARN: Packages root not found; skipping product batch B ship-exit test");
      return;
    }

    Set<String> foundStems = new HashSet<>();
    int packagesWithModern = 0;
    int installXmlWritten = 0;

    for (String pkgName : PSWidgetXmlDualShip.BATCH_B_PACKAGE_DIRS) {
      Path product = packagesRoot.resolve(pkgName);
      if (!Files.isDirectory(product)) {
        System.err.println("WARN: missing package " + pkgName + "; soft-skip");
        continue;
      }

      assertTrue(
          PSWidgetXmlDualShip.hasModernWidgetSources(product),
          pkgName + " must author modern widgets/ sources (#2832 batch B)");

      // Ship-exit (#2884): product source no longer commits install Widget XML.
      assertFalse(
          PSWidgetXmlInstallEmitter.hasCommittedWidgetXml(product),
          pkgName + " must not commit sys__UserDependency--rxconfig/Widgets/*.xml (#2884)");
      assertNoAuthoredWidgetXmlArchivePaths(product, pkgName);

      List<PSWidgetXmlCompileResult> fromModern = PSWidgetXmlDualShip.compileModernWidgets(product);
      // compilePackage falls back to modern when XML absent.
      List<PSWidgetXmlCompileResult> fromPackage =
          PSWidgetXmlPackageCompiler.compilePackage(product);
      assertEquals(
          fromModern.size(),
          fromPackage.size(),
          pkgName + " package compile must equal modern widget count");

      Map<String, PSWidgetXmlCompileResult> modernById =
          fromModern.stream()
              .collect(Collectors.toMap(r -> r.getManifest().getId(), r -> r, (a, b) -> a));
      for (String id : modernById.keySet()) {
        foundStems.add(id);
        PSComponentPackageManifestValidator.validate(modernById.get(id).getManifest());
      }

      // Staging materialize: install XML regenerated from modern for deployer wire format.
      Path staging = tempDir.resolve("batch-b-stage-" + pkgName);
      copyTree(product, staging);
      int written = PSWidgetXmlDualShip.materializeInstallWidgetXml(staging);
      assertEquals(
          fromModern.size(),
          written,
          pkgName + " materialize-install must write one XML per modern widget");
      assertTrue(PSWidgetXmlInstallEmitter.hasCommittedWidgetXml(staging));
      assertTrue(
          PSWidgetArchiveManifestInventory.containsWidgetXmlArchivePath(
              Files.readString(
                  staging.resolve(PSWidgetArchiveManifestInventory.ARCHIVE_INFO_FILE_NAME))),
          pkgName + " staging archiveInfo must list Widget XML after install emit (#3582)");
      installXmlWritten += written;

      List<PSWidgetXmlCompileResult> fromMaterialized =
          PSWidgetXmlPackageCompiler.compilePackage(staging);
      assertEquals(fromModern.size(), fromMaterialized.size(), pkgName + " install XML count");

      Map<String, PSWidgetXmlCompileResult> xmlById =
          fromMaterialized.stream()
              .collect(Collectors.toMap(r -> r.getManifest().getId(), r -> r, (a, b) -> a));
      for (Map.Entry<String, PSWidgetXmlCompileResult> e : modernById.entrySet()) {
        String id = e.getKey();
        assertTrue(xmlById.containsKey(id), pkgName + " missing install widget " + id);
        assertEquals(
            e.getValue().getManifest().getId(), xmlById.get(id).getManifest().getId());
        assertEquals(
            e.getValue().getManifest().getName(), xmlById.get(id).getManifest().getName());
        String templateKey =
            e.getValue().getManifest().getTemplates().isEmpty()
                ? null
                : e.getValue().getManifest().getTemplates().get(0).getSourceRef();
        if (templateKey != null) {
          assertEquals(
              normalizeNewlines(e.getValue().getTextArtifacts().get(templateKey)),
              normalizeNewlines(xmlById.get(id).getTextArtifacts().get(templateKey)),
              "template parity after install materialize for " + id);
        }
      }

      // Second materialize is a no-op while committed XML is present on staging.
      assertEquals(0, PSWidgetXmlDualShip.materializeInstallWidgetXml(staging));

      PSDefinitionSourceSelection sel = PSLegacyDefinitionXmlShim.selectForPackageRoot(product);
      assertEquals(PSDefinitionSourceKind.MODERN_COMPONENT_PACKAGE, sel.getKind(), pkgName);
      assertFalse(
          PSLegacyDefinitionXmlShim.wouldUseLegacyShim(product),
          pkgName + " modern roots should win selection without dual-ship XML");

      packagesWithModern++;
    }

    assertEquals(
        PSWidgetXmlDualShip.BATCH_B_PACKAGE_DIRS.size(),
        packagesWithModern,
        "all batch B packages should be present with modern roots");
    assertEquals(
        new HashSet<>(PSWidgetXmlDualShip.BATCH_B_WIDGET_STEMS),
        foundStems,
        "batch B must cover the 20 named widget stems");
    assertEquals(
        PSWidgetXmlDualShip.BATCH_B_WIDGET_STEMS.size(),
        installXmlWritten,
        "batch B install materialize must cover 20 widgets");
  }

  @Test
  void batchB_packageDirs_disjointFromBatchAAndTestAndSized() {
    assertFalse(PSWidgetXmlDualShip.BATCH_B_PACKAGE_DIRS.contains("perc.Test"));
    assertEquals(14, PSWidgetXmlDualShip.BATCH_B_PACKAGE_DIRS.size());
    assertEquals(20, PSWidgetXmlDualShip.BATCH_B_WIDGET_STEMS.size());
    assertEquals(
        PSWidgetXmlDualShip.BATCH_B_PACKAGE_DIRS,
        PSWidgetXmlPackageCompiler.DUAL_SHIP_BATCH_B_PACKAGE_DIRS);
    // Batches must not overlap packages.
    Set<String> batchA = new HashSet<>(PSWidgetXmlDualShip.BATCH_A_PACKAGE_DIRS);
    for (String pkg : PSWidgetXmlDualShip.BATCH_B_PACKAGE_DIRS) {
      assertFalse(batchA.contains(pkg), "batch B must not re-cover batch A package " + pkg);
    }
    // High-traffic + residual long-tail coverage alignment.
    assertTrue(
        PSWidgetXmlDualShip.BATCH_B_PACKAGE_DIRS.containsAll(
            PSWidgetXmlPackageCompiler.HIGH_TRAFFIC_PACKAGE_DIRS));
    assertTrue(
        PSWidgetXmlDualShip.BATCH_B_PACKAGE_DIRS.containsAll(
            PSWidgetXmlPackageCompiler.RESIDUAL_PRODUCT_PACKAGE_DIRS));
  }

  @Test
  void productBatchC_modernOnly_noCommittedXml_installMaterializeRoundTrip() throws Exception {
    Path packagesRoot = locatePackagesRoot();
    if (packagesRoot == null) {
      System.err.println("WARN: Packages root not found; skipping product batch C ship-exit test");
      return;
    }

    Set<String> foundStems = new HashSet<>();
    int packagesWithModern = 0;
    int installXmlWritten = 0;

    for (String pkgName : PSWidgetXmlDualShip.BATCH_C_PACKAGE_DIRS) {
      Path product = packagesRoot.resolve(pkgName);
      if (!Files.isDirectory(product)) {
        System.err.println("WARN: missing package " + pkgName + "; soft-skip");
        continue;
      }

      assertTrue(
          PSWidgetXmlDualShip.hasModernWidgetSources(product),
          pkgName + " must author modern widgets/ sources (#2844 / #2885 batch C)");

      // Ship-exit (#2885): product source no longer commits install Widget XML.
      assertFalse(
          PSWidgetXmlInstallEmitter.hasCommittedWidgetXml(product),
          pkgName + " must not commit sys__UserDependency--rxconfig/Widgets/*.xml (#2885)");
      assertNoAuthoredWidgetXmlArchivePaths(product, pkgName);

      List<PSWidgetXmlCompileResult> fromModern = PSWidgetXmlDualShip.compileModernWidgets(product);
      // compilePackage falls back to modern when XML absent.
      List<PSWidgetXmlCompileResult> fromPackage =
          PSWidgetXmlPackageCompiler.compilePackage(product);
      assertEquals(
          fromModern.size(),
          fromPackage.size(),
          pkgName + " package compile must equal modern widget count");

      Map<String, PSWidgetXmlCompileResult> modernById =
          fromModern.stream()
              .collect(Collectors.toMap(r -> r.getManifest().getId(), r -> r, (a, b) -> a));
      for (String id : modernById.keySet()) {
        foundStems.add(id);
        PSComponentPackageManifestValidator.validate(modernById.get(id).getManifest());
      }

      // Staging materialize: install XML regenerated from modern for deployer wire format.
      Path staging = tempDir.resolve("batch-c-stage-" + pkgName);
      copyTree(product, staging);
      int written = PSWidgetXmlDualShip.materializeInstallWidgetXml(staging);
      assertEquals(
          fromModern.size(),
          written,
          pkgName + " materialize-install must write one XML per modern widget");
      assertTrue(PSWidgetXmlInstallEmitter.hasCommittedWidgetXml(staging));
      assertTrue(
          PSWidgetArchiveManifestInventory.containsWidgetXmlArchivePath(
              Files.readString(
                  staging.resolve(PSWidgetArchiveManifestInventory.ARCHIVE_INFO_FILE_NAME))),
          pkgName + " staging archiveInfo must list Widget XML after install emit (#3582)");
      installXmlWritten += written;

      List<PSWidgetXmlCompileResult> fromMaterialized =
          PSWidgetXmlPackageCompiler.compilePackage(staging);
      assertEquals(fromModern.size(), fromMaterialized.size(), pkgName + " install XML count");

      Map<String, PSWidgetXmlCompileResult> xmlById =
          fromMaterialized.stream()
              .collect(Collectors.toMap(r -> r.getManifest().getId(), r -> r, (a, b) -> a));
      for (Map.Entry<String, PSWidgetXmlCompileResult> e : modernById.entrySet()) {
        String id = e.getKey();
        assertTrue(xmlById.containsKey(id), pkgName + " missing install widget " + id);
        assertEquals(
            e.getValue().getManifest().getId(), xmlById.get(id).getManifest().getId());
        assertEquals(
            e.getValue().getManifest().getName(), xmlById.get(id).getManifest().getName());
        String templateKey =
            e.getValue().getManifest().getTemplates().isEmpty()
                ? null
                : e.getValue().getManifest().getTemplates().get(0).getSourceRef();
        if (templateKey != null) {
          assertEquals(
              normalizeNewlines(e.getValue().getTextArtifacts().get(templateKey)),
              normalizeNewlines(xmlById.get(id).getTextArtifacts().get(templateKey)),
              "template parity after install materialize for " + id);
        }
      }

      // Second materialize is a no-op while committed XML is present on staging.
      assertEquals(0, PSWidgetXmlDualShip.materializeInstallWidgetXml(staging));

      PSDefinitionSourceSelection sel = PSLegacyDefinitionXmlShim.selectForPackageRoot(product);
      assertEquals(PSDefinitionSourceKind.MODERN_COMPONENT_PACKAGE, sel.getKind(), pkgName);
      assertFalse(
          PSLegacyDefinitionXmlShim.wouldUseLegacyShim(product),
          pkgName + " modern-only package should not require legacy shim");

      packagesWithModern++;
    }

    assertEquals(
        PSWidgetXmlDualShip.BATCH_C_PACKAGE_DIRS.size(),
        packagesWithModern,
        "all batch C packages should be present with modern roots");
    assertEquals(
        new HashSet<>(PSWidgetXmlDualShip.BATCH_C_WIDGET_STEMS),
        foundStems,
        "batch C must cover the 19 named widget stems");
    assertEquals(
        PSWidgetXmlDualShip.BATCH_C_WIDGET_STEMS.size(),
        installXmlWritten,
        "install materialize should write 19 Widget XML files across batch C staging");
  }

  @Test
  void productPercTest_modernOnly_noCommittedXml_installMaterializeRoundTrip() throws Exception {
    Path packagesRoot = locatePackagesRoot();
    if (packagesRoot == null) {
      System.err.println("WARN: Packages root not found; skipping perc.Test ship-exit test");
      return;
    }

    String pkgName = "perc.Test";
    Path product = packagesRoot.resolve(pkgName);
    assertTrue(Files.isDirectory(product), "perc.Test package must exist");
    assertTrue(
        PSWidgetXmlDualShip.hasModernWidgetSources(product),
        "perc.Test must author modern widgets/ sources (#3736)");
    assertFalse(
        PSWidgetXmlInstallEmitter.hasCommittedWidgetXml(product),
        "perc.Test must not commit sys__UserDependency--rxconfig/Widgets/*.xml (#3736)");
    assertNoAuthoredWidgetXmlArchivePaths(product, pkgName);

    List<PSWidgetXmlCompileResult> fromModern = PSWidgetXmlDualShip.compileModernWidgets(product);
    List<PSWidgetXmlCompileResult> fromPackage =
        PSWidgetXmlPackageCompiler.compilePackage(product);
    assertEquals(1, fromModern.size());
    assertEquals(fromModern.size(), fromPackage.size());
    assertEquals("PSWidget_TestProperties", fromModern.get(0).getManifest().getId());
    PSComponentPackageManifestValidator.validate(fromModern.get(0).getManifest());

    Path staging = tempDir.resolve("perc-test-stage");
    copyTree(product, staging);
    int written = PSWidgetXmlDualShip.materializeInstallWidgetXml(staging);
    assertEquals(1, written);
    assertTrue(PSWidgetXmlInstallEmitter.hasCommittedWidgetXml(staging));
    assertTrue(
        PSWidgetArchiveManifestInventory.containsWidgetXmlArchivePath(
            Files.readString(
                staging.resolve(PSWidgetArchiveManifestInventory.ARCHIVE_INFO_FILE_NAME))),
        "perc.Test staging archiveInfo must list Widget XML after install emit (#3582)");

    List<PSWidgetXmlCompileResult> fromMaterialized =
        PSWidgetXmlPackageCompiler.compilePackage(staging);
    assertEquals(1, fromMaterialized.size());
    assertEquals(
        fromModern.get(0).getManifest().getId(), fromMaterialized.get(0).getManifest().getId());
    assertEquals(
        fromModern.get(0).getManifest().getName(),
        fromMaterialized.get(0).getManifest().getName());

    assertEquals(0, PSWidgetXmlDualShip.materializeInstallWidgetXml(staging));

    PSDefinitionSourceSelection sel = PSLegacyDefinitionXmlShim.selectForPackageRoot(product);
    assertEquals(PSDefinitionSourceKind.MODERN_COMPONENT_PACKAGE, sel.getKind(), pkgName);
    assertFalse(
        PSLegacyDefinitionXmlShim.wouldUseLegacyShim(product),
        "perc.Test modern-only package should not require legacy shim");
  }

  @Test
  void testPackageDirs_disjointFromBatchesABCAndSized() {
    assertEquals(List.of("perc.Test"), PSWidgetXmlDualShip.TEST_PACKAGE_DIRS);
    assertEquals(List.of("PSWidget_TestProperties"), PSWidgetXmlDualShip.TEST_WIDGET_STEMS);
    assertEquals(
        PSWidgetXmlDualShip.TEST_PACKAGE_DIRS,
        PSWidgetXmlPackageCompiler.DUAL_SHIP_TEST_PACKAGE_DIRS);
    assertEquals(
        PSWidgetXmlDualShip.TEST_PACKAGE_DIRS,
        PSWidgetXmlPackageCompiler.TEST_PRODUCT_PACKAGE_DIRS);
    Set<String> earlier = new HashSet<>(PSWidgetXmlDualShip.BATCH_A_PACKAGE_DIRS);
    earlier.addAll(PSWidgetXmlDualShip.BATCH_B_PACKAGE_DIRS);
    earlier.addAll(PSWidgetXmlDualShip.BATCH_C_PACKAGE_DIRS);
    assertFalse(earlier.contains("perc.Test"));
  }

  @Test
  void batchC_packageDirs_disjointFromBatchABAndTestAndSized() {
    assertFalse(PSWidgetXmlDualShip.BATCH_C_PACKAGE_DIRS.contains("perc.Test"));
    assertEquals(19, PSWidgetXmlDualShip.BATCH_C_PACKAGE_DIRS.size());
    assertEquals(19, PSWidgetXmlDualShip.BATCH_C_WIDGET_STEMS.size());
    assertEquals(
        PSWidgetXmlDualShip.BATCH_C_PACKAGE_DIRS,
        PSWidgetXmlPackageCompiler.DUAL_SHIP_BATCH_C_PACKAGE_DIRS);
    // Batches must not overlap packages.
    Set<String> earlier = new HashSet<>(PSWidgetXmlDualShip.BATCH_A_PACKAGE_DIRS);
    earlier.addAll(PSWidgetXmlDualShip.BATCH_B_PACKAGE_DIRS);
    for (String pkg : PSWidgetXmlDualShip.BATCH_C_PACKAGE_DIRS) {
      assertFalse(earlier.contains(pkg), "batch C must not re-cover batch A/B package " + pkg);
    }
    // Batch C is the remaining residual product set after A took openGraph/twitter/event/defaultLang.
    for (String pkg : PSWidgetXmlDualShip.BATCH_C_PACKAGE_DIRS) {
      assertTrue(
          PSWidgetXmlPackageCompiler.REMAINING_PRODUCT_PACKAGE_DIRS.contains(pkg),
          "batch C package " + pkg + " should be in REMAINING_PRODUCT_PACKAGE_DIRS");
    }
  }

  @Test
  void resolvePackageRelative_rejectsDotDot() {
    Path root = tempDir.resolve("root");
    try {
      PSWidgetXmlDualShip.resolvePackageRelative(root, "../escape");
      throw new AssertionError("expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
      assertTrue(expected.getMessage().contains(".."));
    }
  }

  private static void assertNoAuthoredWidgetXmlArchivePaths(Path product, String pkgName)
      throws Exception {
    for (String fileName :
        List.of(
            PSWidgetArchiveManifestInventory.ARCHIVE_INFO_FILE_NAME,
            PSWidgetArchiveManifestInventory.ARCHIVE_MANIFEST_FILE_NAME)) {
      Path descriptor = product.resolve(fileName);
      if (!Files.isRegularFile(descriptor)) {
        continue;
      }
      String xml = Files.readString(descriptor);
      assertFalse(
          PSWidgetArchiveManifestInventory.containsWidgetXmlArchivePath(xml),
          pkgName + " must not author rxconfig/Widgets/*.xml in " + fileName + " (#3582)");
    }
  }

  private static String normalizeNewlines(String s) {
    return s == null ? null : s.replace("\r\n", "\n").replace('\r', '\n');
  }

  private static void copyTree(Path source, Path target) throws Exception {
    Files.walk(source)
        .forEach(
            path -> {
              try {
                Path rel = source.relativize(path);
                Path dest = target.resolve(rel.toString());
                if (Files.isDirectory(path)) {
                  Files.createDirectories(dest);
                } else {
                  Path parent = dest.getParent();
                  if (parent != null) {
                    Files.createDirectories(parent);
                  }
                  Files.copy(path, dest, StandardCopyOption.REPLACE_EXISTING);
                }
              } catch (Exception e) {
                throw new RuntimeException(e);
              }
            });
  }

  private static Path locatePackage(String packageDirName) {
    Path packages = locatePackagesRoot();
    if (packages == null) {
      return null;
    }
    Path candidate = packages.resolve(packageDirName);
    return Files.isDirectory(candidate) ? candidate : null;
  }

  private static Path locatePackagesRoot() {
    Path candidate = Path.of("src", "main", "resources", "Packages");
    if (Files.isDirectory(candidate)) {
      return candidate.toAbsolutePath().normalize();
    }
    Path cwd = Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
    Path alt =
        cwd.resolve("src").resolve("main").resolve("resources").resolve("Packages");
    return Files.isDirectory(alt) ? alt : null;
  }
}
