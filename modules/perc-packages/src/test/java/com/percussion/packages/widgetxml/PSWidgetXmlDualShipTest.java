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
 * Dual-ship modern widget authoring tests (issues #2831 batch A, #2832 batch B, #2844 batch C /
 * parent #2630).
 *
 * <p>Product packages keep install Widget XML under {@code
 * sys__UserDependency--rxconfig/Widgets} while committing modern {@code widgets/&lt;stem&gt;/}
 * roots. Selection prefers modern when both exist.
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

    // Minimal valid-shaped simple text fixture if product package unavailable.
    Path product = locatePackage("perc.baseWidgets");
    if (product != null) {
      Path src =
          product
              .resolve("sys__UserDependency--rxconfig")
              .resolve("Widgets")
              .resolve("percSimpleText.xml");
      Files.copy(src, widgetsXml.resolve("percSimpleText.xml"), StandardCopyOption.REPLACE_EXISTING);
      // Copy package props so context version matches product
      Path props = product.resolve("psx_archiveInfo.xml");
      if (Files.isRegularFile(props)) {
        Files.copy(props, packageDir.resolve("psx_archiveInfo.xml"), StandardCopyOption.REPLACE_EXISTING);
      }
    } else {
      Files.writeString(
          widgetsXml.resolve("percSimpleText.xml"),
          """
          <Widget>
            <Title>Simple Text</Title>
            <Content type="velocity"><![CDATA[#loadRelatedWidgetContents()]]></Content>
            <Code type="jexl"><![CDATA[$x = 1;]]></Code>
            <contenttype_name>percSimpleTextAsset</contenttype_name>
            <Category>content</Category>
          </Widget>
          """,
          StandardCharsets.UTF_8);
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
  void productBatchA_modernAuthoringRoots_presentAndParityWithXmlCompile() throws Exception {
    Path packagesRoot = locatePackagesRoot();
    if (packagesRoot == null) {
      System.err.println("WARN: Packages root not found; skipping product batch A dual-ship test");
      return;
    }

    Set<String> foundStems = new HashSet<>();
    int packagesWithModern = 0;

    for (String pkgName : PSWidgetXmlDualShip.BATCH_A_PACKAGE_DIRS) {
      Path product = packagesRoot.resolve(pkgName);
      if (!Files.isDirectory(product)) {
        System.err.println("WARN: missing package " + pkgName + "; soft-skip");
        continue;
      }

      assertTrue(
          PSWidgetXmlDualShip.hasModernWidgetSources(product),
          pkgName + " must author modern widgets/ sources (#2831 batch A)");

      // Dual-run: install Widget XML remains until native install path (do not mass-delete).
      Path xmlDir = PSWidgetXmlPackageCompiler.resolveWidgetsDir(product);
      assertTrue(Files.isDirectory(xmlDir), pkgName + " still dual-ships Widget XML for install");

      List<PSWidgetXmlCompileResult> fromXml = PSWidgetXmlPackageCompiler.compilePackage(product);
      List<PSWidgetXmlCompileResult> fromModern = PSWidgetXmlDualShip.compileModernWidgets(product);
      assertEquals(
          fromXml.size(),
          fromModern.size(),
          pkgName + " modern widget count must match Widget XML count");

      Map<String, PSWidgetXmlCompileResult> xmlById =
          fromXml.stream()
              .collect(Collectors.toMap(r -> r.getManifest().getId(), r -> r, (a, b) -> a));
      Map<String, PSWidgetXmlCompileResult> modernById =
          fromModern.stream()
              .collect(Collectors.toMap(r -> r.getManifest().getId(), r -> r, (a, b) -> a));

      for (Map.Entry<String, PSWidgetXmlCompileResult> e : xmlById.entrySet()) {
        String id = e.getKey();
        assertTrue(modernById.containsKey(id), pkgName + " missing modern widget " + id);
        PSComponentPackageManifest expected = e.getValue().getManifest();
        PSComponentPackageManifest actual = modernById.get(id).getManifest();
        // Reparse both so map/list equality is stable.
        PSComponentPackageManifest expectedRound =
            PSComponentPackageManifestIo.parse(PSComponentPackageManifestIo.toJson(expected));
        PSComponentPackageManifest actualRound =
            PSComponentPackageManifestIo.parse(PSComponentPackageManifestIo.toJson(actual));
        assertEquals(expectedRound, actualRound, "modern manifest parity for " + id);

        String templateKey =
            expected.getTemplates().isEmpty()
                ? null
                : expected.getTemplates().get(0).getSourceRef();
        if (templateKey != null) {
          assertEquals(
              normalizeNewlines(e.getValue().getTextArtifacts().get(templateKey)),
              normalizeNewlines(modernById.get(id).getTextArtifacts().get(templateKey)),
              "template parity for " + id);
        }
        foundStems.add(id);
      }

      // Shim: package root with modern widgets/ prefers modern over legacy XML.
      PSDefinitionSourceSelection sel = PSLegacyDefinitionXmlShim.selectForPackageRoot(product);
      assertEquals(PSDefinitionSourceKind.MODERN_COMPONENT_PACKAGE, sel.getKind(), pkgName);
      assertFalse(
          PSLegacyDefinitionXmlShim.wouldUseLegacyShim(product),
          pkgName + " dual-ship modern roots should win selection");

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
  void productBatchB_modernAuthoringRoots_presentAndParityWithXmlCompile() throws Exception {
    Path packagesRoot = locatePackagesRoot();
    if (packagesRoot == null) {
      System.err.println("WARN: Packages root not found; skipping product batch B dual-ship test");
      return;
    }

    Set<String> foundStems = new HashSet<>();
    int packagesWithModern = 0;

    for (String pkgName : PSWidgetXmlDualShip.BATCH_B_PACKAGE_DIRS) {
      Path product = packagesRoot.resolve(pkgName);
      if (!Files.isDirectory(product)) {
        System.err.println("WARN: missing package " + pkgName + "; soft-skip");
        continue;
      }

      assertTrue(
          PSWidgetXmlDualShip.hasModernWidgetSources(product),
          pkgName + " must author modern widgets/ sources (#2832 batch B)");

      // Dual-run: install Widget XML remains until native install path (do not mass-delete).
      Path xmlDir = PSWidgetXmlPackageCompiler.resolveWidgetsDir(product);
      assertTrue(Files.isDirectory(xmlDir), pkgName + " still dual-ships Widget XML for install");

      List<PSWidgetXmlCompileResult> fromXml = PSWidgetXmlPackageCompiler.compilePackage(product);
      List<PSWidgetXmlCompileResult> fromModern = PSWidgetXmlDualShip.compileModernWidgets(product);
      assertEquals(
          fromXml.size(),
          fromModern.size(),
          pkgName + " modern widget count must match Widget XML count");

      Map<String, PSWidgetXmlCompileResult> xmlById =
          fromXml.stream()
              .collect(Collectors.toMap(r -> r.getManifest().getId(), r -> r, (a, b) -> a));
      Map<String, PSWidgetXmlCompileResult> modernById =
          fromModern.stream()
              .collect(Collectors.toMap(r -> r.getManifest().getId(), r -> r, (a, b) -> a));

      for (Map.Entry<String, PSWidgetXmlCompileResult> e : xmlById.entrySet()) {
        String id = e.getKey();
        assertTrue(modernById.containsKey(id), pkgName + " missing modern widget " + id);
        PSComponentPackageManifest expected = e.getValue().getManifest();
        PSComponentPackageManifest actual = modernById.get(id).getManifest();
        PSComponentPackageManifest expectedRound =
            PSComponentPackageManifestIo.parse(PSComponentPackageManifestIo.toJson(expected));
        PSComponentPackageManifest actualRound =
            PSComponentPackageManifestIo.parse(PSComponentPackageManifestIo.toJson(actual));
        assertEquals(expectedRound, actualRound, "modern manifest parity for " + id);

        String templateKey =
            expected.getTemplates().isEmpty()
                ? null
                : expected.getTemplates().get(0).getSourceRef();
        if (templateKey != null) {
          assertEquals(
              normalizeNewlines(e.getValue().getTextArtifacts().get(templateKey)),
              normalizeNewlines(modernById.get(id).getTextArtifacts().get(templateKey)),
              "template parity for " + id);
        }
        foundStems.add(id);
      }

      PSDefinitionSourceSelection sel = PSLegacyDefinitionXmlShim.selectForPackageRoot(product);
      assertEquals(PSDefinitionSourceKind.MODERN_COMPONENT_PACKAGE, sel.getKind(), pkgName);
      assertFalse(
          PSLegacyDefinitionXmlShim.wouldUseLegacyShim(product),
          pkgName + " dual-ship modern roots should win selection");

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
  void productBatchC_modernAuthoringRoots_presentAndParityWithXmlCompile() throws Exception {
    Path packagesRoot = locatePackagesRoot();
    if (packagesRoot == null) {
      System.err.println("WARN: Packages root not found; skipping product batch C dual-ship test");
      return;
    }

    Set<String> foundStems = new HashSet<>();
    int packagesWithModern = 0;

    for (String pkgName : PSWidgetXmlDualShip.BATCH_C_PACKAGE_DIRS) {
      Path product = packagesRoot.resolve(pkgName);
      if (!Files.isDirectory(product)) {
        System.err.println("WARN: missing package " + pkgName + "; soft-skip");
        continue;
      }

      assertTrue(
          PSWidgetXmlDualShip.hasModernWidgetSources(product),
          pkgName + " must author modern widgets/ sources (#2844 batch C)");

      // Dual-run: install Widget XML remains until native install path (do not mass-delete).
      Path xmlDir = PSWidgetXmlPackageCompiler.resolveWidgetsDir(product);
      assertTrue(Files.isDirectory(xmlDir), pkgName + " still dual-ships Widget XML for install");

      List<PSWidgetXmlCompileResult> fromXml = PSWidgetXmlPackageCompiler.compilePackage(product);
      List<PSWidgetXmlCompileResult> fromModern = PSWidgetXmlDualShip.compileModernWidgets(product);
      assertEquals(
          fromXml.size(),
          fromModern.size(),
          pkgName + " modern widget count must match Widget XML count");

      Map<String, PSWidgetXmlCompileResult> xmlById =
          fromXml.stream()
              .collect(Collectors.toMap(r -> r.getManifest().getId(), r -> r, (a, b) -> a));
      Map<String, PSWidgetXmlCompileResult> modernById =
          fromModern.stream()
              .collect(Collectors.toMap(r -> r.getManifest().getId(), r -> r, (a, b) -> a));

      for (Map.Entry<String, PSWidgetXmlCompileResult> e : xmlById.entrySet()) {
        String id = e.getKey();
        assertTrue(modernById.containsKey(id), pkgName + " missing modern widget " + id);
        PSComponentPackageManifest expected = e.getValue().getManifest();
        PSComponentPackageManifest actual = modernById.get(id).getManifest();
        PSComponentPackageManifest expectedRound =
            PSComponentPackageManifestIo.parse(PSComponentPackageManifestIo.toJson(expected));
        PSComponentPackageManifest actualRound =
            PSComponentPackageManifestIo.parse(PSComponentPackageManifestIo.toJson(actual));
        assertEquals(expectedRound, actualRound, "modern manifest parity for " + id);

        String templateKey =
            expected.getTemplates().isEmpty()
                ? null
                : expected.getTemplates().get(0).getSourceRef();
        if (templateKey != null) {
          assertEquals(
              normalizeNewlines(e.getValue().getTextArtifacts().get(templateKey)),
              normalizeNewlines(modernById.get(id).getTextArtifacts().get(templateKey)),
              "template parity for " + id);
        }
        foundStems.add(id);
      }

      PSDefinitionSourceSelection sel = PSLegacyDefinitionXmlShim.selectForPackageRoot(product);
      assertEquals(PSDefinitionSourceKind.MODERN_COMPONENT_PACKAGE, sel.getKind(), pkgName);
      assertFalse(
          PSLegacyDefinitionXmlShim.wouldUseLegacyShim(product),
          pkgName + " dual-ship modern roots should win selection");

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

  private static String normalizeNewlines(String s) {
    return s == null ? null : s.replace("\r\n", "\n").replace('\r', '\n');
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
