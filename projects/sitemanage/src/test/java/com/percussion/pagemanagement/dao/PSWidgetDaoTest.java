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
 * Behavioral tests for {@link PSWidgetDao} dual-run modern-first selection (#3024 / parent #2630).
 *
 * <p>Policy via {@link PSLegacyDefinitionXmlShim}: modern component package preferred; legacy
 * Widgets XML fallback; neither → clear {@link PSDefinitionSourceNotFoundException} (no silent
 * invent). Selection kinds are test-visible on the DAO.
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
  void selectDefinitionSource_fallsBackToLegacyWidgetXmlWhenModernAbsent() throws Exception {
    String id = "percRawHtml";
    Path xml = writeWidgetXml(id);

    dao.setModernPackageRoots(List.of());

    PSDefinitionSourceSelection selection = dao.selectDefinitionSource(id);
    assertEquals(PSDefinitionSourceKind.LEGACY_WIDGET_XML, selection.getKind());
    assertTrue(selection.isLegacyXml());
    assertEquals(PSDefinitionSourceKind.LEGACY_WIDGET_XML, dao.getLastSelectionKind());
    assertEquals(xml.toAbsolutePath().normalize(), selection.getPrimaryPath().orElseThrow());
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
    Files.writeString(
        nested.resolve(PSLegacyDefinitionXmlShim.MODERN_MANIFEST_FILE_NAME),
        "{\"schemaVersion\":\"1.0\",\"id\":\"" + id + "\",\"name\":\"Rich\",\"version\":\"1.0.0\"}",
        StandardCharsets.UTF_8);

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
  void pollRecordsSelectionKinds_legacyWhenOnlyXml() throws Exception {
    String id = "customCustomerWidget";
    writeWidgetXml(id);

    dao.setModernPackageRoots(List.of());
    dao.poll();

    assertNotNull(dao.find(id));
    MapAssert.assertKind(
        dao.getSelectionKindsById(), id, PSDefinitionSourceKind.LEGACY_WIDGET_XML);
    assertEquals(PSDefinitionSourceKind.LEGACY_WIDGET_XML, dao.getLastSelectionKind());
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
    Files.writeString(
        modernRoot.resolve(PSLegacyDefinitionXmlShim.MODERN_MANIFEST_FILE_NAME),
        "{\"schemaVersion\":\"1.0\",\"id\":\""
            + definitionId
            + "\",\"name\":\""
            + definitionId
            + "\",\"version\":\"1.0.0\"}",
        StandardCharsets.UTF_8);
    return modernRoot.toAbsolutePath().normalize();
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
