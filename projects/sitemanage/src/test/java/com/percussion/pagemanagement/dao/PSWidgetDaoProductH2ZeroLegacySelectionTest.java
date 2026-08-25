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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.packages.shim.PSDefinitionSourceKind;
import com.percussion.packages.shim.PSProductPackageRootSelectionEvidence;
import com.percussion.pagemanagement.dao.impl.PSWidgetDao;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Runtime H2-shape M2 evidence: {@link PSWidgetDao} with blank {@code
 * widgetDao.modernPackageRoots} + product classpath materialize must select {@link
 * PSDefinitionSourceKind#MODERN_COMPONENT_PACKAGE} for every non-waived product widget (#3583 /
 * #3738 / parent #2630). Unexpected {@code LEGACY_WIDGET_XML} fails the harness. {@code perc.Test}
 * / {@code PSWidget_TestProperties} may remain legacy while the Widget G4 waive list is {@code
 * perc.Test} only; after that waiver is dropped (#3736) it must be modern-first. Customer-only XML
 * still selects legacy — the shim stays (#2852). Not M2 PASS overall (M3 still FAIL).
 */
class PSWidgetDaoProductH2ZeroLegacySelectionTest {

  private static final String MINIMAL_WIDGET_XML =
      """
      <?xml version="1.0" encoding="UTF-8"?>
      <Widget>
        <WidgetPrefs title="H2 M2 Evidence Widget"
          contenttype_name="H2EvidenceCT"
          description="Product/H2 zero-legacy-selection evidence"
          author="Intersoft Data Labs" />
      </Widget>
      """;

  @TempDir Path tempDir;

  @Test
  void h2DefaultRoots_productWidgetsSelectModern_customerOnlyStaysLegacy() throws Exception {
    Path widgetsDir = tempDir.resolve("rxconfig").resolve("Widgets");
    Files.createDirectories(widgetsDir);

    PSWidgetDao dao = new PSWidgetDao();
    dao.setRepositoryDirectory(widgetsDir.toString());
    dao.setModernPackageRootsProperty("");
    dao.setRxDeployDir(tempDir.toString());

    List<Path> roots = dao.getModernPackageRoots();
    assertFalse(
        roots.isEmpty(),
        "H2 blank property + rxdeploydir must resolve product modern roots via classpath materialize");

    PSProductPackageRootSelectionEvidence.assertNoUnexpectedLegacyOnDiscoveredModernRoots(roots);
    PSProductPackageRootSelectionEvidence.assertWidgetWaiverPolicy();

    List<String> productIds =
        PSProductPackageRootSelectionEvidence.listModernWidgetDefinitionIds(roots);
    assertTrue(
        productIds.containsAll(PSProductPackageRootSelectionEvidence.KNOWN_PRODUCT_WIDGET_STEMS),
        () -> "H2 modern roots missing known product widget stems: " + productIds);

    boolean percTestOnModernRoots =
        productIds.contains(PSProductPackageRootSelectionEvidence.PERC_TEST_WIDGET_STEM);
    if (PSProductPackageRootSelectionEvidence.isPercTestWidgetWaiverDropped()) {
      assertTrue(
          percTestOnModernRoots,
          "perc.Test modern stem must be on H2 classpath after #3736");
    }

    for (String id : productIds) {
      writeWidgetXml(widgetsDir, id);
    }
    writeWidgetXml(widgetsDir, "customerOnlyXml");
    if (!percTestOnModernRoots) {
      writeWidgetXml(widgetsDir, PSProductPackageRootSelectionEvidence.PERC_TEST_WIDGET_STEM);
    }

    dao.resetSelectionMetrics();

    List<String> unexpectedLegacy = new ArrayList<>();
    for (String id : productIds) {
      PSDefinitionSourceKind kind = dao.selectDefinitionSource(id).getKind();
      if (kind != PSDefinitionSourceKind.MODERN_COMPONENT_PACKAGE) {
        unexpectedLegacy.add(id + "=" + kind);
      }
    }
    assertTrue(
        unexpectedLegacy.isEmpty(),
        "unexpected LEGACY_* on non-waived product widgets: " + unexpectedLegacy);

    PSProductPackageRootSelectionEvidence.assertNoUnexpectedLegacyDefinitions(
        roots,
        widgetsDir,
        productIds,
        PSProductPackageRootSelectionEvidence.currentWaivedDefinitionIds());

    assertEquals(
        PSDefinitionSourceKind.LEGACY_WIDGET_XML,
        dao.selectDefinitionSource("customerOnlyXml").getKind(),
        "customer-only XML must still select LEGACY_WIDGET_XML (shim kept)");

    long expectedLegacy = 1L;
    if (!percTestOnModernRoots) {
      assertEquals(
          PSDefinitionSourceKind.LEGACY_WIDGET_XML,
          dao.selectDefinitionSource(PSProductPackageRootSelectionEvidence.PERC_TEST_WIDGET_STEM)
              .getKind(),
          "waived perc.Test widget may remain LEGACY_WIDGET_XML until #3736");
      expectedLegacy = 2L;
    }

    assertEquals(productIds.size(), dao.getModernSelectionCount());
    assertEquals(expectedLegacy, dao.getLegacySelectionCount(), dao.formatSelectionMetricsSummary());
    assertEquals(0L, unexpectedLegacy.size());

    String summary = dao.formatSelectionMetricsSummary();
    assertTrue(summary.contains("legacyWidgetXml=" + expectedLegacy), summary);
    assertTrue(summary.contains("modern=" + productIds.size()), summary);
  }

  private static void writeWidgetXml(Path widgetsDir, String id) throws Exception {
    Files.writeString(
        widgetsDir.resolve(id + ".xml"), MINIMAL_WIDGET_XML, StandardCharsets.UTF_8);
  }
}
