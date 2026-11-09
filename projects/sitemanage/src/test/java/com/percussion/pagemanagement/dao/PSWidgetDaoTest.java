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

package com.percussion.pagemanagement.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.packages.shim.PSDefinitionSourceKind;
import com.percussion.packages.shim.PSDefinitionSourceNotFoundException;
import com.percussion.packages.shim.PSDefinitionSourceSelection;
import com.percussion.packages.shim.PSLegacyDefinitionXmlShim;
import com.percussion.packages.shim.PSModernPackageRootDefaults;
import com.percussion.pagemanagement.dao.impl.PSWidgetDao;
import com.percussion.pagemanagement.data.PSWidgetDefinition;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Behavioral tests for {@link PSWidgetDao} modern-only selection (#2852 / parent #2632; dual-run
 * retired).
 *
 * <p>Policy via {@link PSLegacyDefinitionXmlShim}: modern component package required; retired
 * legacy Widgets XML is never selected — modern-absent fails with a clear {@link
 * PSDefinitionSourceNotFoundException} (no silent invent). Selection kinds are test-visible on the
 * DAO. Product defaults under {@link PSModernPackageRootDefaults#RELATIVE_MODERN_ROOTS_DIR} apply
 * when the Spring property is blank.
 */
class PSWidgetDaoTest {

  private static final String MINIMAL_WIDGET_XML =
      """
      <?xml version="1.0" encoding="UTF-8"?>
      <Widget>
        <WidgetPrefs title="Test Widget"
          contenttype_name="TestWidgetCT"
          description="Dual-run test widget"
          author="Intersoft Data Labs" />
      </Widget>
      """;

  private static final String MANIFEST_JSON_TEMPLATE =
      """
      {"schemaVersion":"1.0","id":"%s","name":"%s","version":"1.0.0"}""";

  @TempDir Path tempDir;

  private PSWidgetDao dao;
  private Path widgetsDir;

  @BeforeEach
  void setUp() throws Exception {
    widgetsDir = tempDir.resolve("rxconfig").resolve("Widgets");
    Files.createDirectories(widgetsDir);
    dao = new PSWidgetDao();
    dao.setRepositoryDirectory(widgetsDir.toString());
  }

  @Test
  void selectDefinitionSource_prefersModernWhenBothPresent() throws Exception {
    String id = "percSimpleText";
    writeWidgetXml(id);
    Path modernRoot = writeModernPackageRoot(id);

    dao.setModernPackageRoots(List.of(modernRoot));

    PSDefinitionSourceSelection selection = dao.selectDefinitionSource(id);
    assertEquals(PSDefinitionSourceKind.MODERN_COMPONENT_PACKAGE, selection.getKind());
    assertTrue(selection.isModern());
    assertFalse(selection.isLegacyXml());
    assertEquals(PSDefinitionSourceKind.MODERN_COMPONENT_PACKAGE, dao.getLastSelectionKind());
    assertTrue(selection.getPrimaryPath().isPresent());
    assertTrue(
        selection
            .getPrimaryPath()
            .orElseThrow()
            .endsWith(PSLegacyDefinitionXmlShim.MODERN_MANIFEST_FILE_NAME));
  }

  @Test
  void selectDefinitionSource_modernAbsentThrowsRetiredErrorEvenWhenXmlPresent()
      throws Exception {
    String id = "percRawHtml";
    writeWidgetXml(id);

    dao.setModernPackageRoots(List.of());

    // Modern-only (#2852): retired legacy XML is never selected.
    PSDefinitionSourceNotFoundException ex =
        assertThrows(
            PSDefinitionSourceNotFoundException.class, () -> dao.selectDefinitionSource(id));
    assertEquals(id, ex.getDefinitionId());
    String msg = ex.getMessage();
    assertTrue(msg.contains("retired"), msg);
    assertTrue(msg.contains("percRawHtml.xml"), msg);
    assertEquals(0L, dao.getLegacySelectionCount());
  }

  @Test
  void selectDefinitionSource_neitherThrowsClearError() {
    dao.setModernPackageRoots(List.of());

    PSDefinitionSourceNotFoundException ex =
        assertThrows(
            PSDefinitionSourceNotFoundException.class,
            () -> dao.selectDefinitionSource("missingWidget"));
    assertEquals("missingWidget", ex.getDefinitionId());
    String msg = ex.getMessage();
    assertTrue(msg.contains("missingWidget"));
    assertTrue(
        msg.contains(PSLegacyDefinitionXmlShim.MODERN_MANIFEST_FILE_NAME) || msg.contains("modern"));
    assertTrue(msg.contains("legacy") || msg.contains("XML"));
  }

  @Test
  void pollRecordsSelectionKinds_modernWinsOverCoLocatedXml() throws Exception {
    String id = "percRichText";
    writeWidgetXml(id);
    Path modernRoot = writeModernPackageRoot(id);
    // Dual-ship nested layout also recognized: packageRoot/widgets/<id>/manifest
    Path multiPkg = tempDir.resolve("perc.baseWidgets");
    Path nested = multiPkg.resolve("widgets").resolve(id);
    Files.createDirectories(nested);
    writeManifestJson(nested, id, "Rich");

    dao.setModernPackageRoots(List.of(modernRoot, multiPkg));
    dao.poll();

    PSWidgetDefinition def = dao.find(id);
    assertNotNull(def);
    assertEquals(id, def.getId());

    MapAssert.assertKind(
        dao.getSelectionKindsById(), id, PSDefinitionSourceKind.MODERN_COMPONENT_PACKAGE);
    assertEquals(
        PSDefinitionSourceKind.MODERN_COMPONENT_PACKAGE, dao.getLastSelectionKind());
  }

  @Test
  void pollRecordsSelectionKinds_unclassifiedWhenOnlyRetiredXml() throws Exception {
    String id = "customCustomerWidget";
    writeWidgetXml(id);

    dao.setModernPackageRoots(List.of());
    dao.poll();

    // Install wire content still loads; modern-only selection leaves the id unclassified
    // (no legacy fallback) with a clear warn in the log.
    assertNotNull(dao.find(id));
    assertFalse(dao.getSelectionKindsById().containsKey(id));
    assertEquals(0L, dao.getModernSelectionCount());
    assertEquals(0L, dao.getLegacySelectionCount());
  }

  @Test
  void setModernPackageRootsProperty_usesPathSeparator() throws Exception {
    String id = "sepWidget";
    writeWidgetXml(id);
    Path modernRoot = writeModernPackageRoot(id);

    // Single root via property (pathSeparator list form)
    dao.setModernPackageRootsProperty(modernRoot.toString());
    assertEquals(1, dao.getModernPackageRoots().size());
    assertEquals(
        modernRoot.toAbsolutePath().normalize(), dao.getModernPackageRoots().get(0));

    assertEquals(
        PSDefinitionSourceKind.MODERN_COMPONENT_PACKAGE,
        dao.selectDefinitionSource(id).getKind());
  }

  @Test
  void productDefaults_modernPresentUnderPackagesModern_selectsModernWithoutExplicitProperty()
      throws Exception {
    // Fresh DAO: blank property + rxdeploydir with Packages/Modern product layout (#3130)
    PSWidgetDao defaultingDao = new PSWidgetDao();
    defaultingDao.setRepositoryDirectory(widgetsDir.toString());

    String id = "percSimpleText";
    writeWidgetXml(id);
    Path modernPkg =
        tempDir
            .resolve(PSModernPackageRootDefaults.RELATIVE_MODERN_ROOTS_DIR)
            .resolve("perc.baseWidgets");
    Path widgetDir = modernPkg.resolve("widgets").resolve(id);
    Files.createDirectories(widgetDir);
    writeManifestJson(widgetDir, id, id);

    // Blank property first, then rx deploy dir triggers product default discovery
    defaultingDao.setModernPackageRootsProperty("");
    defaultingDao.setRxDeployDir(tempDir.toString());

    assertFalse(defaultingDao.getModernPackageRoots().isEmpty());
    assertEquals(
        PSDefinitionSourceKind.MODERN_COMPONENT_PACKAGE,
        defaultingDao.selectDefinitionSource(id).getKind());
  }

  @Test
  void productDefaults_modernAbsent_throwsRetiredError() throws Exception {
    PSWidgetDao defaultingDao = new PSWidgetDao();
    defaultingDao.setRepositoryDirectory(widgetsDir.toString());

    String id = "customerLegacyOnly";
    writeWidgetXml(id);

    // Blank property + empty install: classpath materialize may seed Packages/Modern for product
    // packages, but a customer-only id still has no modern root → retired error (#2852).
    Path emptyRx = tempDir.resolve("emptyRx");
    Files.createDirectories(emptyRx);
    defaultingDao.setModernPackageRootsProperty("");
    defaultingDao.setRxDeployDir(emptyRx.toString());

    PSDefinitionSourceNotFoundException ex =
        assertThrows(
            PSDefinitionSourceNotFoundException.class,
            () -> defaultingDao.selectDefinitionSource(id));
    assertTrue(ex.getMessage().contains("retired"), ex.getMessage());
  }

  @Test
  void productDefaults_blankPropertyWithoutRxDir_throwsUntilRxConfigured() throws Exception {
    PSWidgetDao defaultingDao = new PSWidgetDao();
    defaultingDao.setRepositoryDirectory(widgetsDir.toString());
    defaultingDao.setModernPackageRootsProperty("");
    // No setRxDeployDir yet → empty roots
    assertTrue(defaultingDao.getModernPackageRoots().isEmpty());

    String id = "preRxLegacy";
    writeWidgetXml(id);
    assertThrows(
        PSDefinitionSourceNotFoundException.class,
        () -> defaultingDao.selectDefinitionSource(id));
  }

  @Test
  void findAll_returnsLoadedWidgets() throws Exception {
    writeWidgetXml("w1");
    writeWidgetXml("w2");
    dao.poll();

    List<PSWidgetDefinition> all = dao.findAll();
    assertEquals(2, all.size());
  }

  private Path writeWidgetXml(String id) throws Exception {
    Path xml = widgetsDir.resolve(id + ".xml");
    Files.writeString(xml, MINIMAL_WIDGET_XML, StandardCharsets.UTF_8);
    return xml.toAbsolutePath().normalize();
  }

  private Path writeModernPackageRoot(String definitionId) throws Exception {
    Path modernRoot = tempDir.resolve("modern").resolve(definitionId);
    Files.createDirectories(modernRoot);
    writeManifestJson(modernRoot, definitionId, definitionId);
    return modernRoot.toAbsolutePath().normalize();
  }

  /**
   * Writes the modern component manifest for tests. Uses a text block (no backslash escapes) so
   * the content stays identical on every platform; JSON manifest text is platform-independent.
   */
  private static void writeManifestJson(Path packageDir, String id, String name)
      throws Exception {
    Files.writeString(
        packageDir.resolve(PSLegacyDefinitionXmlShim.MODERN_MANIFEST_FILE_NAME),
        MANIFEST_JSON_TEMPLATE.formatted(id, name),
        StandardCharsets.UTF_8);
  }

  /** Tiny assertion helper to keep test method bodies readable. */
  private static final class MapAssert {
    static void assertKind(
        java.util.Map<String, PSDefinitionSourceKind> map,
        String id,
        PSDefinitionSourceKind expected) {
      assertNotNull(map);
      assertTrue(map.containsKey(id), "expected selection kind for id " + id + " in " + map);
      assertEquals(expected, map.get(id));
    }
  }
}
