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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.packages.shim.PSDefinitionSourceKind;
import com.percussion.packages.shim.PSDefinitionSourceNotFoundException;
import com.percussion.packages.shim.PSLegacyDefinitionXmlShim;
import com.percussion.pagemanagement.dao.impl.PSWidgetDao;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * CI-assertable modern-only selection <strong>metrics evidence harness</strong> for {@link
 * PSWidgetDao} (#3131 / #2852 / parent #2632).
 *
 * <p>Drives the modern path and asserts cumulative counters, snapshot map, and ops summary string
 * — so M2 evidence is not only INFO logs. Legacy counters/keys are retained for dashboard
 * compatibility and must read {@code 0}; retired-legacy ids fail clearly instead of counting.
 */
class PSWidgetDaoSelectionMetricsHarnessTest {

  private static final String MINIMAL_WIDGET_XML =
      """
      <?xml version="1.0" encoding="UTF-8"?>
      <Widget>
        <WidgetPrefs title="Metrics Harness Widget"
          contenttype_name="MetricsHarnessCT"
          description="Dual-run metrics harness"
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
    dao.resetSelectionMetrics();
  }

  @Test
  void selectDefinitionSource_modernCountsLegacyStaysZeroAndRetiredThrows() throws Exception {
    String modernId = "percSimpleText";
    String retiredId = "customerOnlyXml";
    writeWidgetXml(modernId);
    writeWidgetXml(retiredId);
    Path modernRoot = writeModernPackageRoot(modernId);

    dao.setModernPackageRoots(List.of(modernRoot));

    assertEquals(PSDefinitionSourceKind.MODERN_COMPONENT_PACKAGE,
        dao.selectDefinitionSource(modernId).getKind());
    // Retired legacy XML is never selected — clear error, no legacy count.
    assertThrows(
        PSDefinitionSourceNotFoundException.class,
        () -> dao.selectDefinitionSource(retiredId));
    // Second modern select accumulates
    assertEquals(PSDefinitionSourceKind.MODERN_COMPONENT_PACKAGE,
        dao.selectDefinitionSource(modernId).getKind());

    assertEquals(2L, dao.getModernSelectionCount());
    assertEquals(0L, dao.getLegacySelectionCount());
    assertEquals(2L, dao.getTotalSelectionCount());
    assertEquals(PSDefinitionSourceKind.MODERN_COMPONENT_PACKAGE, dao.getLastSelectionKind());
  }

  @Test
  void poll_accumulatesCountersPerClassifiedId() throws Exception {
    writeWidgetXml("wModern");
    writeWidgetXml("wLegacy");
    Path modernRoot = writeModernPackageRoot("wModern");
    dao.setModernPackageRoots(List.of(modernRoot));

    dao.poll();

    Map<String, PSDefinitionSourceKind> kinds = dao.getSelectionKindsById();
    assertEquals(PSDefinitionSourceKind.MODERN_COMPONENT_PACKAGE, kinds.get("wModern"));
    // Retired-legacy id stays unclassified under modern-only selection.
    assertFalse(kinds.containsKey("wLegacy"), "retired id must not be classified: " + kinds);
    assertEquals(1L, dao.getModernSelectionCount());
    assertEquals(0L, dao.getLegacySelectionCount());
    assertEquals(1L, dao.getTotalSelectionCount());

    // Direct selects accumulate independently of poll (poll may no-op when files unchanged).
    dao.selectDefinitionSource("wModern");
    assertThrows(
        PSDefinitionSourceNotFoundException.class,
        () -> dao.selectDefinitionSource("wLegacy"));
    assertEquals(2L, dao.getModernSelectionCount());
    assertEquals(0L, dao.getLegacySelectionCount());
    assertEquals(2L, dao.getTotalSelectionCount());
  }

  @Test
  void snapshotAndSummary_exposeModernLegacyTotalAndLastKind() throws Exception {
    writeWidgetXml("snapModern");
    Path modernRoot = writeModernPackageRoot("snapModern");
    dao.setModernPackageRoots(List.of(modernRoot));
    dao.selectDefinitionSource("snapModern");

    Map<String, Long> snap = dao.getSelectionMetricsSnapshot();
    assertEquals(1L, snap.get("modern"));
    assertEquals(0L, snap.get("legacyWidgetXml"));
    assertEquals(1L, snap.get("total"));

    String summary = dao.formatSelectionMetricsSummary();
    assertTrue(summary.contains("modern=1"), summary);
    assertTrue(summary.contains("legacyWidgetXml=0"), summary);
    assertTrue(summary.contains("total=1"), summary);
    assertTrue(summary.contains("lastKind=MODERN_COMPONENT_PACKAGE"), summary);
  }

  @Test
  void resetSelectionMetrics_clearsCountersAndLastKind() throws Exception {
    writeWidgetXml("resetMe");
    Path modernRoot = writeModernPackageRoot("resetMe");
    dao.setModernPackageRoots(List.of(modernRoot));
    dao.selectDefinitionSource("resetMe");
    assertEquals(1L, dao.getModernSelectionCount());
    assertEquals(PSDefinitionSourceKind.MODERN_COMPONENT_PACKAGE, dao.getLastSelectionKind());

    dao.resetSelectionMetrics();

    assertEquals(0L, dao.getModernSelectionCount());
    assertEquals(0L, dao.getLegacySelectionCount());
    assertEquals(0L, dao.getTotalSelectionCount());
    assertEquals(null, dao.getLastSelectionKind());
    assertTrue(dao.getSelectionKindsById().isEmpty());
    Map<String, Long> snap = dao.getSelectionMetricsSnapshot();
    assertEquals(0L, snap.get("total"));
  }

  @Test
  void mixedSelectAndPoll_harnessAssertsModernPreferredWhenBothPresent() throws Exception {
    String id = "bothPresent";
    writeWidgetXml(id);
    Path modernRoot = writeModernPackageRoot(id);
    dao.setModernPackageRoots(List.of(modernRoot));

    dao.selectDefinitionSource(id);
    dao.poll();

    // 1 select + 1 poll classification
    assertEquals(2L, dao.getModernSelectionCount());
    assertEquals(0L, dao.getLegacySelectionCount());
    assertEquals(
        PSDefinitionSourceKind.MODERN_COMPONENT_PACKAGE,
        dao.getSelectionKindsById().get(id));
    String summary = dao.formatSelectionMetricsSummary();
    assertTrue(summary.contains("modern=2"), summary);
    assertTrue(summary.contains("legacyWidgetXml=0"), summary);
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
}
