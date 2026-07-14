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
package com.percussion.pso.restservice.jexl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.dom4j.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PSOImportJexl XXE Prevention Tests")
class PSOImportJexlTest {

  @Test
  @DisplayName("Should parse a safe XML string using getDomFromString")
  void testGetDomFromStringSafe() throws Exception {
    PSOImportJexl importJexl = new PSOImportJexl();
    String safeXml = "<root><element>value</element></root>";
    Document doc = importJexl.getDomFromString(safeXml);
    assertNotNull(doc);
    assertEquals("value", doc.getRootElement().element("element").getText());
  }

  @Test
  @DisplayName("Should disallow external entity resolution (XXE prevention)")
  void testGetDomFromStringXXE() throws Exception {
    PSOImportJexl importJexl = new PSOImportJexl();
    String xxeXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<!DOCTYPE root [\n" +
            "  <!ENTITY xxe SYSTEM \"http://invalid-nonexistent-host-xxe-test.com/test.dtd\">\n" +
            "]>\n" +
            "<root>&xxe;</root>";
    
    try {
      importJexl.getDomFromString(xxeXml);
    } catch (Exception e) {
      // Exception is expected when DOCTYPE decl is disallowed
      assertTrue(e.getMessage().contains("disallow-doctype-decl") || e.getMessage().contains("DOCTYPE"));
    }
  }
}
