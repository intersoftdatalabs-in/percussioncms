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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Archive-manifest Widget XML dual-ship exit gate (issue #3582 / parent #2630). Product source
 * descriptors must not author {@code rxconfig/Widgets/*.xml} except waived {@code perc.Test}.
 * Install emitter re-injects those paths on staging copies.
 *
 * <p>Cross-platform: {@link Path#resolve(String)} / {@link Files} only.
 */
class PSWidgetArchiveManifestInventoryTest {

  @TempDir Path tempDir;

  @Test
  void waivedPackageSet_isExplicitlyPercTestOnly() {
    assertEquals(Set.of("perc.Test"), PSWidgetArchiveManifestInventory.WAIVED_PACKAGE_DIRS);
    assertTrue(PSWidgetArchiveManifestInventory.isWaivedPackage("perc.Test"));
    assertFalse(PSWidgetArchiveManifestInventory.isWaivedPackage("perc.baseWidgets"));
    assertFalse(PSWidgetArchiveManifestInventory.isWaivedPackage(null));
  }

  @Test
  void productPackagesTree_hasZeroNonWaivedWidgetXmlArchivePaths() throws Exception {
    Path packagesRoot = locatePackagesRoot();
    assertNotNull(
        packagesRoot,
        "Packages root must be visible when running module Surefire from perc-packages");

    PSWidgetArchiveManifestInventory.Report report =
        PSWidgetArchiveManifestInventory.scan(packagesRoot);
    assertTrue(
        report.isClean(),
        () ->
            "non-waived archive manifests must not author rxconfig/Widgets/*.xml; found: "
                + report.nonWaived());

    assertFalse(
        report.waived().isEmpty(),
        "expected waived Widget XML archive paths under perc.Test");
    for (PSWidgetArchiveManifestInventory.Finding f : report.waived()) {
      assertEquals("perc.Test", f.packageDirName());
      assertTrue(f.waived());
    }

    PSWidgetArchiveManifestInventory.assertNoNonWaivedWidgetXmlArchivePaths(packagesRoot);
  }

  @Test
  void tempTree_dummyNonWaivedArchivePath_failsGate() throws Exception {
    Path packages = tempDir.resolve("Packages");
    Path pkg = packages.resolve("perc.baseWidgets");
    Files.createDirectories(pkg);
    Files.writeString(
        pkg.resolve(PSWidgetArchiveManifestInventory.ARCHIVE_INFO_FILE_NAME),
        archiveInfoFixture("rxconfig/Widgets/percSimpleText.xml"),
        StandardCharsets.UTF_8);

    PSWidgetArchiveManifestInventory.Report report =
        PSWidgetArchiveManifestInventory.scan(packages);
    assertFalse(report.isClean());
    assertEquals(1, report.nonWaived().size());
    assertEquals("perc.baseWidgets", report.nonWaived().get(0).packageDirName());
    assertTrue(report.nonWaived().get(0).excerpt().contains("percSimpleText.xml"));

    IllegalStateException err =
        assertThrows(
            IllegalStateException.class,
            () ->
                PSWidgetArchiveManifestInventory.assertNoNonWaivedWidgetXmlArchivePaths(packages));
    assertTrue(err.getMessage().contains("#3582"));
    assertTrue(err.getMessage().contains("perc.baseWidgets"));
  }

  @Test
  void tempTree_onlyWaivedArchivePaths_isClean() throws Exception {
    Path packages = tempDir.resolve("Packages");
    Path waived = packages.resolve("perc.Test");
    Files.createDirectories(waived);
    Files.writeString(
        waived.resolve(PSWidgetArchiveManifestInventory.ARCHIVE_INFO_FILE_NAME),
        archiveInfoFixture("rxconfig/Widgets/PSWidget_TestProperties.xml"),
        StandardCharsets.UTF_8);
    Files.writeString(
        waived.resolve(PSWidgetArchiveManifestInventory.ARCHIVE_MANIFEST_FILE_NAME),
        archiveManifestFixture("PSWidget_TestProperties.xml"),
        StandardCharsets.UTF_8);

    PSWidgetArchiveManifestInventory.Report report =
        PSWidgetArchiveManifestInventory.scan(packages);
    assertTrue(report.isClean());
    assertEquals(0, report.nonWaived().size());
    assertFalse(report.waived().isEmpty());
  }

  @Test
  void strip_removesWidgetBlocks_keepsOtherUserDependencies() {
    String info = archiveInfoFixture("rxconfig/Widgets/percIframe.xml");
    assertTrue(PSWidgetArchiveManifestInventory.containsWidgetXmlArchivePath(info));
    String stripped = PSWidgetArchiveManifestInventory.stripWidgetXmlArchivePaths(info);
    assertFalse(PSWidgetArchiveManifestInventory.containsWidgetXmlArchivePath(stripped));
    assertTrue(stripped.contains("rx_resources/widgets/iframe/images/keep.png"));
    assertTrue(stripped.contains("sys_UserDependency"));

    String manifest = archiveManifestFixture("percIframe.xml");
    String strippedManifest = PSWidgetArchiveManifestInventory.stripWidgetXmlArchivePaths(manifest);
    assertFalse(PSWidgetArchiveManifestInventory.containsWidgetXmlArchivePath(strippedManifest));
    assertTrue(strippedManifest.contains("rx__resources_widgets_iframe"));
  }

  @Test
  void stripPackage_skipsWaivedAndRequiresModernRoots() throws Exception {
    Path waived = tempDir.resolve("perc.Test");
    Files.createDirectories(waived);
    Path waivedInfo = waived.resolve(PSWidgetArchiveManifestInventory.ARCHIVE_INFO_FILE_NAME);
    String original = archiveInfoFixture("rxconfig/Widgets/PSWidget_TestProperties.xml");
    Files.writeString(waivedInfo, original, StandardCharsets.UTF_8);
    assertEquals(0, PSWidgetArchiveManifestInventory.stripAuthoredWidgetXmlArchivePaths(waived));
    assertEquals(original, Files.readString(waivedInfo, StandardCharsets.UTF_8));

    Path noModern = tempDir.resolve("perc.orphan");
    Files.createDirectories(noModern);
    Path orphanInfo = noModern.resolve(PSWidgetArchiveManifestInventory.ARCHIVE_INFO_FILE_NAME);
    Files.writeString(
        orphanInfo, archiveInfoFixture("rxconfig/Widgets/percOrphan.xml"), StandardCharsets.UTF_8);
    assertEquals(0, PSWidgetArchiveManifestInventory.stripAuthoredWidgetXmlArchivePaths(noModern));
    assertTrue(
        PSWidgetArchiveManifestInventory.containsWidgetXmlArchivePath(
            Files.readString(orphanInfo, StandardCharsets.UTF_8)));
  }

  @Test
  void stripPackage_rewritesModernNonWaivedDescriptors() throws Exception {
    Path pkg = tempDir.resolve("perc.widget.iframe");
    writeMinimalModernWidget(pkg, "percIframe");
    Path info = pkg.resolve(PSWidgetArchiveManifestInventory.ARCHIVE_INFO_FILE_NAME);
    Path manifest = pkg.resolve(PSWidgetArchiveManifestInventory.ARCHIVE_MANIFEST_FILE_NAME);
    Files.writeString(
        info, archiveInfoFixture("rxconfig/Widgets/percIframe.xml"), StandardCharsets.UTF_8);
    Files.writeString(
        manifest, archiveManifestFixture("percIframe.xml"), StandardCharsets.UTF_8);

    assertEquals(2, PSWidgetArchiveManifestInventory.stripAuthoredWidgetXmlArchivePaths(pkg));
    assertFalse(
        PSWidgetArchiveManifestInventory.containsWidgetXmlArchivePath(
            Files.readString(info, StandardCharsets.UTF_8)));
    assertFalse(
        PSWidgetArchiveManifestInventory.containsWidgetXmlArchivePath(
            Files.readString(manifest, StandardCharsets.UTF_8)));
  }

  @Test
  void inject_addsMissingEntries_andIsIdempotent() throws Exception {
    Path pkg = tempDir.resolve("staged");
    Files.createDirectories(pkg);
    Path info = pkg.resolve(PSWidgetArchiveManifestInventory.ARCHIVE_INFO_FILE_NAME);
    Files.writeString(
        info,
        archiveInfoFixture("rx_resources/widgets/iframe/images/keep.png"),
        StandardCharsets.UTF_8);

    int first =
        PSWidgetArchiveManifestInventory.ensureInstallWidgetXmlArchivePaths(
            pkg, List.of("percIframe"));
    assertTrue(first >= 1);
    String infoAfter = Files.readString(info, StandardCharsets.UTF_8);
    assertTrue(infoAfter.contains("path=\"rxconfig/Widgets/percIframe.xml\""));
    assertTrue(infoAfter.contains("rx_resources/widgets/iframe/images/keep.png"));

    Path manifest = pkg.resolve(PSWidgetArchiveManifestInventory.ARCHIVE_MANIFEST_FILE_NAME);
    assertTrue(Files.isRegularFile(manifest));
    String manAfter = Files.readString(manifest, StandardCharsets.UTF_8);
    assertTrue(
        manAfter.contains(
            PSWidgetArchiveManifestInventory.encodeWidgetXmlArchiveFolder("percIframe.xml")));
    assertTrue(manAfter.contains("rxconfig/Widgets/percIframe.xml"));
    assertTrue(manAfter.contains("/percIframe.xml"));

    int second =
        PSWidgetArchiveManifestInventory.ensureInstallWidgetXmlArchivePaths(
            pkg, List.of("percIframe"));
    assertEquals(0, second);
  }

  @Test
  void encodeWidgetXmlArchiveFolder_matchesHistoricalPackageLayout() {
    assertEquals(
        "sys__UserDependency--rxconfig_Widgets_percIframe-xml",
        PSWidgetArchiveManifestInventory.encodeWidgetXmlArchiveFolder("percIframe.xml"));
    assertEquals(
        "sys__UserDependency--rxconfig_Widgets_PSWidget__TestProperties-xml",
        PSWidgetArchiveManifestInventory.encodeWidgetXmlArchiveFolder(
            "PSWidget_TestProperties.xml"));
    assertEquals(
        "sys__UserDependency--rxconfig_Widgets_simplePageAutoList-xml",
        PSWidgetArchiveManifestInventory.encodeWidgetXmlArchiveFolder("simplePageAutoList.xml"));
  }

  @Test
  void requireStem_rejectsPathTraversal() {
    assertThrows(
        IllegalArgumentException.class,
        () -> PSWidgetArchiveManifestInventory.widgetXmlInstallPath("../escape"));
    assertThrows(
        IllegalArgumentException.class,
        () -> PSWidgetArchiveManifestInventory.widgetXmlInstallPath("a/b"));
  }

  @Test
  void materializeInstall_injectsArchivePathsForModernOnly() throws Exception {
    Path pkg = tempDir.resolve("modernOnly");
    Path fixture = tempDir.resolve("percSimpleText.xml");
    try (var in =
        PSWidgetArchiveManifestInventoryTest.class.getResourceAsStream(
            "/widgetxml/percSimpleText.xml")) {
      assertNotNull(in);
      Files.write(fixture, in.readAllBytes());
    }
    PSWidgetXmlCompileResult modern = PSWidgetXmlCompiler.compile(fixture, null);
    Path modernRoot =
        pkg.resolve(PSWidgetXmlDualShip.WIDGETS_DIR_NAME).resolve("percSimpleText");
    PSWidgetXmlCompiler.writeArtifacts(modern, modernRoot);
    Files.writeString(
        pkg.resolve(PSWidgetArchiveManifestInventory.ARCHIVE_INFO_FILE_NAME),
        archiveInfoFixture("rx_resources/widgets/simpletext/keep.png"),
        StandardCharsets.UTF_8);

    int written = PSWidgetXmlInstallEmitter.materializeInstallWidgetXml(pkg);
    assertEquals(1, written);
    String info =
        Files.readString(
            pkg.resolve(PSWidgetArchiveManifestInventory.ARCHIVE_INFO_FILE_NAME),
            StandardCharsets.UTF_8);
    assertTrue(info.contains("rxconfig/Widgets/percSimpleText.xml"));
    String man =
        Files.readString(
            pkg.resolve(PSWidgetArchiveManifestInventory.ARCHIVE_MANIFEST_FILE_NAME),
            StandardCharsets.UTF_8);
    assertTrue(man.contains("rxconfig_Widgets_percSimpleText-xml"));
  }

  @Test
  void scan_rejectsNonDirectoryRoot() {
    Path missing = tempDir.resolve("does-not-exist");
    assertThrows(
        IllegalArgumentException.class, () -> PSWidgetArchiveManifestInventory.scan(missing));
  }

  private static void writeMinimalModernWidget(Path pkg, String stem) throws Exception {
    Path modern = pkg.resolve(PSWidgetXmlDualShip.WIDGETS_DIR_NAME).resolve(stem);
    Files.createDirectories(modern.resolve("templates"));
    Files.writeString(
        modern.resolve("component-package.json"),
        """
        {"schemaVersion":"1.0","id":"%s","name":"%s","version":"1.0.0",
         "catalog":{"kind":"component","title":"%s"},
         "templates":[{"name":"%sSnippet","type":"snippet","assembler":"velocityAssembler",
           "sourceRef":"templates/%sSnippet.vm","bindings":[]}],
         "contentTypes":[],"slots":[],"resources":[],"userPreferences":[],"cssPreferences":[]}
        """
            .formatted(stem, stem, stem, stem, stem),
        StandardCharsets.UTF_8);
    Files.writeString(
        modern.resolve("templates").resolve(stem + "Snippet.vm"),
        "#loadRelatedWidgetContents()",
        StandardCharsets.UTF_8);
  }

  private static String archiveInfoFixture(String path) {
    return """
        <?xml version="1.0" encoding="utf-8"?>
        <PSXArchiveInfo archiveRef="demo">
          <PSXArchiveDetail>
            <PSXExportDescriptor>
              <Packages>
                <PSXDeployableElement>
                  <PSXDependency dependencyId="sys_UserDependency" objectType="Custom"
                      supportsUserDependencies="yes">
                    <Dependencies>
                      <PSXUserDependency parentId="sys_UserDependency"
                          parentKey="Custom-sys_UserDependency" parentType="Custom"
                          path="%s">
                        <PSXDeployableObject>
                          <PSXDependency dependencyId="%s" dependencyType="User"
                              displayName="keep" objectType="sys_UserDependency">
                            <Dependencies />
                          </PSXDependency>
                          <RequiredClasses />
                        </PSXDeployableObject>
                      </PSXUserDependency>
                      <PSXUserDependency parentId="sys_UserDependency"
                          parentKey="Custom-sys_UserDependency" parentType="Custom"
                          path="rx_resources/widgets/iframe/images/keep.png">
                        <PSXDeployableObject>
                          <PSXDependency dependencyId="rx_resources/widgets/iframe/images/keep.png"
                              dependencyType="User" displayName="keep.png"
                              objectType="sys_UserDependency">
                            <Dependencies />
                          </PSXDependency>
                          <RequiredClasses />
                        </PSXDeployableObject>
                      </PSXUserDependency>
                    </Dependencies>
                  </PSXDependency>
                </PSXDeployableElement>
              </Packages>
            </PSXExportDescriptor>
          </PSXArchiveDetail>
        </PSXArchiveInfo>
        """
        .formatted(path, path);
  }

  private static String archiveManifestFixture(String widgetFileName) {
    String folder = PSWidgetArchiveManifestInventory.encodeWidgetXmlArchiveFolder(widgetFileName);
    return """
        <?xml version="1.0" encoding="utf-8"?>
        <PSXArchiveManifest>
          <PSXDepFilesIdTypes
              DependencyKey="sys__UserDependency--rx__resources_widgets_iframe_images_keep-png">
            <PSXDependencyFile fileType="SUPPORT_FILE">
              <RxFile>rx_resources/widgets/iframe/images/keep.png</RxFile>
              <ArchiveFile>sys__UserDependency--rx__resources_widgets_iframe_images_keep-png/keep.png</ArchiveFile>
            </PSXDependencyFile>
          </PSXDepFilesIdTypes>
          <PSXDepFilesIdTypes
              DependencyKey="%s">
            <PSXDependencyFile fileType="SUPPORT_FILE">
              <RxFile>rxconfig/Widgets/%s</RxFile>
              <ArchiveFile>%s/%s</ArchiveFile>
            </PSXDependencyFile>
          </PSXDepFilesIdTypes>
        </PSXArchiveManifest>
        """
        .formatted(folder, widgetFileName, folder, widgetFileName);
  }

  private static Path locatePackagesRoot() {
    Path candidate = Path.of("src", "main", "resources", "Packages");
    if (Files.isDirectory(candidate)) {
      return candidate.toAbsolutePath().normalize();
    }
    Path cwd = Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
    Path alt = cwd.resolve("src").resolve("main").resolve("resources").resolve("Packages");
    return Files.isDirectory(alt) ? alt : null;
  }
}
