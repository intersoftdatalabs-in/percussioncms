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
 * Guard for #3282: {@code NEXTNUMBER.PSX_OBJECTACL} (and {@code PSX_PROPERTIES}) must sit at or
 * above the highest seed {@code SYSID} so the first {@code createId} is not 1001 (Everyone on
 * CONTENTID=301).
 *
 * <p>Paths use {@link Path#of(String, String...)} only (portable).
 */
@Tag("UnitTest")
class CmsTableDataObjectAclNextNumberTest {

  private static final Path CMS_TABLE_DATA =
      Path.of("src/main/resources/distribution/rxconfig/Installer/data/cmsTableData.xml");

  @Test
  void objectAclNextNumberIsPastSeedSysids() throws Exception {
    assertTrue(Files.isRegularFile(CMS_TABLE_DATA), CMS_TABLE_DATA.toAbsolutePath().toString());
    Document doc = parse(CMS_TABLE_DATA);
    int nextNr = nextNumber(doc, "PSX_OBJECTACL");
    int maxSysid = maxColumn(doc, "PSX_OBJECTACL", "SYSID");
    assertTrue(
        nextNr >= maxSysid,
        "NEXTNUMBER.PSX_OBJECTACL NEXTNR="
            + nextNr
            + " must be >= max seed SYSID="
            + maxSysid
            + " (#3282)");
    // createId returns NEXTNR+1; keep a gap so 1001 cannot be reissued.
    assertTrue(nextNr + 1 > maxSysid, "first allocated id must be free");
  }

  @Test
  void propertiesNextNumberIsPastSeedSysids() throws Exception {
    Document doc = parse(CMS_TABLE_DATA);
    int nextNr = nextNumber(doc, "PSX_PROPERTIES");
    int maxSysid = maxColumn(doc, "PSX_PROPERTIES", "SYSID");
    assertTrue(
        nextNr >= maxSysid,
        "NEXTNUMBER.PSX_PROPERTIES NEXTNR="
            + nextNr
            + " must be >= max seed SYSID="
            + maxSysid
            + " (same collision class as #3282)");
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

  private static String columnValue(Element row, String name) {
    NodeList cols = row.getElementsByTagName("column");
    for (int i = 0; i < cols.getLength(); i++) {
      Node n = cols.item(i);
      if (n instanceof Element col && name.equals(col.getAttribute("name"))) {
        return col.getTextContent() == null ? "" : col.getTextContent().trim();
      }
    }
    return null;
  }
}
