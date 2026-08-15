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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.distribution.install;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * When {@code installSampleSites} loads FastForward {@code RxffSampleTableData},
 * {@code NEXTNUMBER} must sit at or above sample PKs so the first {@code createId}
 * is not a colliding 1001 (peer of {@link CmsTableDataObjectAclNextNumberTest} /
 * #3282).
 */
@Tag("UnitTest")
class CmsTableDataSampleContentNextNumberTest {

  private static final Path CMS_TABLE_DATA =
      Path.of("src/main/resources/distribution/rxconfig/Installer/data/cmsTableData.xml");

  static final Path SAMPLE_CONTENT =
      Path.of(
          "..",
          "..",
          "system",
          "FastForward",
          "SampleContent",
          "Config",
          "Data",
          "RxffSampleTableData.xml");

  @Test
  void contentStatusHistoryNextNumberIsPastSampleIds() throws Exception {
    assertKeysPastSample("CONTENTSTATUSHISTORY", "CONTENTSTATUSHISTORY", "CONTENTSTATUSHISTORYID");
  }

  @Test
  void relatedContentNextNumberIsPastSampleRids() throws Exception {
    assertKeysPastSample("RXRELATEDCONTENT", "PSX_OBJECTRELATIONSHIP", "RID");
  }

  @Test
  void contentNextNumberIsPastSampleContentIds() throws Exception {
    assertKeysPastSample("CONTENT", "CONTENTSTATUS", "CONTENTID");
  }

  @Test
  void objectAclNextNumberIsPastSampleSysids() throws Exception {
    assertKeysPastSample("PSX_OBJECTACL", "PSX_OBJECTACL", "SYSID");
  }

  @Test
  void propertiesNextNumberIsPastSampleSysids() throws Exception {
    assertKeysPastSample("PSX_PROPERTIES", "PSX_PROPERTIES", "SYSID");
  }

  private static void assertKeysPastSample(String nextKey, String table, String column)
      throws Exception {
    assertTrue(Files.isRegularFile(CMS_TABLE_DATA), CMS_TABLE_DATA.toAbsolutePath().toString());
    assertTrue(Files.isRegularFile(SAMPLE_CONTENT), SAMPLE_CONTENT.toAbsolutePath().toString());
    Document cms = parse(CMS_TABLE_DATA);
    Document sample = parse(SAMPLE_CONTENT);
    int nextNr = nextNumber(cms, nextKey);
    int maxSample = maxColumn(sample, table, column);
    int maxCms = maxColumn(cms, table, column);
    int max = Math.max(maxSample, maxCms);
    assertTrue(
        nextNr >= max,
        "NEXTNUMBER."
            + nextKey
            + " NEXTNR="
            + nextNr
            + " must be >= max seed "
            + table
            + "."
            + column
            + "="
            + max
            + " (sample="
            + maxSample
            + ", cms="
            + maxCms
            + ")");
    assertTrue(nextNr + 1 > max, "first allocated id for " + nextKey + " must be free");
  }

  private static Document parse(Path path) throws Exception {
    var factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(false);
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    return factory.newDocumentBuilder().parse(path.toFile());
  }

  private static int nextNumber(Document doc, String keyName) {
    for (Element row : tableRows(doc, "NEXTNUMBER")) {
      if (keyName.equals(columnValue(row, "KEYNAME"))) {
        return Integer.parseInt(columnValue(row, "NEXTNR"));
      }
    }
    throw new AssertionError("NEXTNUMBER key missing: " + keyName);
  }

  private static int maxColumn(Document doc, String tableName, String column) {
    int max = -1;
    for (Element row : tableRows(doc, tableName)) {
      String raw = columnValue(row, column);
      if (raw == null || raw.isBlank()) {
        continue;
      }
      max = Math.max(max, Integer.parseInt(raw.trim()));
    }
    return max;
  }

  private static List<Element> tableRows(Document doc, String tableName) {
    List<Element> rows = new ArrayList<>();
    NodeList tables = doc.getElementsByTagName("table");
    for (int i = 0; i < tables.getLength(); i++) {
      Node n = tables.item(i);
      if (!(n instanceof Element table)) {
        continue;
      }
      if (!tableName.equals(table.getAttribute("name"))) {
        continue;
      }
      NodeList children = table.getChildNodes();
      for (int j = 0; j < children.getLength(); j++) {
        Node c = children.item(j);
        if (c instanceof Element el && "row".equals(el.getTagName())) {
          rows.add(el);
        }
      }
    }
    return rows;
  }

  private static String columnValue(Element row, String columnName) {
    NodeList cols = row.getElementsByTagName("column");
    for (int i = 0; i < cols.getLength(); i++) {
      Element col = (Element) cols.item(i);
      if (columnName.equalsIgnoreCase(col.getAttribute("name"))) {
        return col.getTextContent();
      }
    }
    return null;
  }
}
