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
package com.percussion.extension;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * #1500: system database function dialect coverage for PostgreSQL (matrix smoke).
 *
 * <p>Missing driver bodies surface at runtime as {@code Database function definition not found for
 * function CHAR_TO_INT and driver postgresql}.
 */
@Tag("UnitTest")
class PSDatabaseFunctionDefsPostgresTest {

  @Test
  void charToIntAndDaysFromDateDefinedForPostgresql() throws Exception {
    Path defs = resolveFunctionDefs();
    Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(defs.toFile());

    assertTrue(
        hasDriverBody(doc, "CHAR_TO_INT", "postgresql"),
        "CHAR_TO_INT must define driver=postgresql");
    assertTrue(
        hasDriverBody(doc, "DAYSFROMDATE", "postgresql"),
        "DAYSFROMDATE must define driver=postgresql");
  }

  private static Path resolveFunctionDefs() {
    // system module cwd when running tests standalone
    Path[] candidates =
        new Path[] {
          Paths.get("config", "sys_DatabaseFunctionDefs.xml"),
          Paths.get("system", "config", "sys_DatabaseFunctionDefs.xml"),
          Paths.get("..", "config", "sys_DatabaseFunctionDefs.xml")
        };
    for (Path p : candidates) {
      if (Files.isRegularFile(p)) {
        return p.toAbsolutePath().normalize();
      }
    }
    fail("sys_DatabaseFunctionDefs.xml not found relative to " + Paths.get("").toAbsolutePath());
    return null;
  }

  private static boolean hasDriverBody(Document doc, String standardName, String driver) {
    NodeList funcs = doc.getElementsByTagName("PSXDatabaseFunction");
    for (int i = 0; i < funcs.getLength(); i++) {
      Element func = (Element) funcs.item(i);
      if (!standardName.equals(func.getAttribute("standardFunctionName"))) {
        continue;
      }
      NodeList defs = func.getElementsByTagName("PSXDatabaseFunctionDef");
      for (int j = 0; j < defs.getLength(); j++) {
        Element def = (Element) defs.item(j);
        if (driver.equals(def.getAttribute("driver"))) {
          NodeList bodies = def.getElementsByTagName("Body");
          assertNotNull(bodies);
          assertTrue(bodies.getLength() > 0, standardName + " body empty for " + driver);
          String body = bodies.item(0).getTextContent();
          assertNotNull(body);
          assertTrue(body.trim().length() > 0, standardName + " body blank for " + driver);
          return true;
        }
      }
    }
    return false;
  }
}
