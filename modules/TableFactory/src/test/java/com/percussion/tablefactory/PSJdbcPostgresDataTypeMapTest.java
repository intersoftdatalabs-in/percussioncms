/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
package com.percussion.tablefactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.Objects;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/** #1500: TableFactory ships a first-class PostgreSQL data type map (driver {@code postgresql}). */
@Tag("UnitTest")
public class PSJdbcPostgresDataTypeMapTest {

  private static final String RESOURCE = "com/percussion/tablefactory/PSJdbcDataTypeMaps.xml";

  @Test
  void postgresMapExistsWithBooleanTextBytea() throws Exception {
    Document doc;
    try (InputStream in =
        Objects.requireNonNull(
            getClass().getClassLoader().getResourceAsStream(RESOURCE), "missing " + RESOURCE)) {
      doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
    }
    NodeList maps = doc.getElementsByTagName("DataTypeMap");
    Element postgres = null;
    for (int i = 0; i < maps.getLength(); i++) {
      Element el = (Element) maps.item(i);
      if ("POSTGRES".equals(el.getAttribute("for"))
          && "postgresql".equals(el.getAttribute("driver"))) {
        postgres = el;
        break;
      }
    }
    assertNotNull(postgres, "DataTypeMap for=POSTGRES driver=postgresql required");

    assertTrue(hasNative(postgres, "BIT", "BOOLEAN"));
    assertTrue(hasNative(postgres, "BOOLEAN", "BOOLEAN"));
    assertTrue(hasNative(postgres, "CLOB", "TEXT"));
    assertTrue(hasNative(postgres, "BLOB", "BYTEA"));
    assertEquals("SMALLINT", nativeFor(postgres, "TINYINT"));
  }

  private static boolean hasNative(Element map, String jdbc, String nativeType) {
    return nativeType.equalsIgnoreCase(nativeFor(map, jdbc));
  }

  private static String nativeFor(Element map, String jdbc) {
    NodeList types = map.getElementsByTagName("DataType");
    for (int i = 0; i < types.getLength(); i++) {
      Element el = (Element) types.item(i);
      if (jdbc.equalsIgnoreCase(el.getAttribute("jdbc"))) {
        return el.getAttribute("native");
      }
    }
    return null;
  }
}
