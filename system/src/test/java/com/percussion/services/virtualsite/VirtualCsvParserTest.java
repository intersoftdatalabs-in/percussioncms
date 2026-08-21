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
package com.percussion.services.virtualsite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class VirtualCsvParserTest {

  @Test
  void parsesRequiredAndOptionalColumnsCaseInsensitive() throws Exception {
    String csv =
        """
        ID,Title,Body,Path,Order
        home,Welcome,"Hello, world",index.md,10
        """;
    List<Map<String, String>> rows = VirtualCsvParser.parse(csv, "pages.csv");
    assertEquals(1, rows.size());
    Map<String, String> row = rows.get(0);
    assertEquals("home", VirtualCsvParser.cell(row, "id"));
    assertEquals("Welcome", VirtualCsvParser.cell(row, "title"));
    assertEquals("Hello, world", VirtualCsvParser.cell(row, "body"));
    assertEquals("index.md", VirtualCsvParser.cell(row, "path"));
    assertEquals("10", VirtualCsvParser.cell(row, "order"));
  }

  @Test
  void quotedBodyMayContainNewlinesAndEscapedQuotes() throws Exception {
    String csv =
        "id,title,body\n"
            + "p1,Page,\"Line 1\nSee \"\"quoted\"\".\"\n";
    List<Map<String, String>> rows = VirtualCsvParser.parse(csv, "pages.csv");
    assertEquals(1, rows.size());
    assertEquals("Line 1\nSee \"quoted\".", VirtualCsvParser.cell(rows.get(0), "body"));
  }

  @Test
  void crlfRecordsAreAccepted() throws Exception {
    String csv = "id,title,body\r\na,A,alpha\r\nb,B,beta\r\n";
    List<Map<String, String>> rows = VirtualCsvParser.parse(csv, "win.csv");
    assertEquals(2, rows.size());
    assertEquals("alpha", VirtualCsvParser.cell(rows.get(0), "body"));
    assertEquals("beta", VirtualCsvParser.cell(rows.get(1), "body"));
  }

  @Test
  void missingRequiredColumnFailsClosed() {
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () -> VirtualCsvParser.parse("id,title\nx,y\n", "bad.csv"));
    assertTrue(ex.getMessage().contains("body"), ex.getMessage());
    assertTrue(ex.getMessage().contains("bad.csv"), ex.getMessage());
  }

  @Test
  void missingIdColumnFailsClosed() {
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () -> VirtualCsvParser.parse("title,body\nT,B\n", "noid.csv"));
    assertTrue(ex.getMessage().contains("id"), ex.getMessage());
  }

  @Test
  void missingTitleColumnFailsClosed() {
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () -> VirtualCsvParser.parse("id,body\nx,B\n", "notitle.csv"));
    assertTrue(ex.getMessage().contains("title"), ex.getMessage());
  }

  @Test
  void unclosedQuoteFailsClosed() {
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () -> VirtualCsvParser.parse("id,title,body\na,b,\"c\n", "open.csv"));
    assertTrue(ex.getMessage().toLowerCase().contains("quoted"), ex.getMessage());
  }

  @Test
  void raggedRowFailsClosed() {
    VirtualSiteException ex =
        assertThrows(
            VirtualSiteException.class,
            () -> VirtualCsvParser.parse("id,title,body\na,b\n", "ragged.csv"));
    assertTrue(ex.getMessage().contains("field"), ex.getMessage());
  }

  @Test
  void emptyFileFailsClosed() {
    VirtualSiteException ex =
        assertThrows(VirtualSiteException.class, () -> VirtualCsvParser.parse("", "empty.csv"));
    assertTrue(ex.getMessage().toLowerCase().contains("header"), ex.getMessage());
  }

  @Test
  void headerOnlyYieldsNoRows() throws Exception {
    List<Map<String, String>> rows = VirtualCsvParser.parse("id,title,body\n", "header.csv");
    assertTrue(rows.isEmpty());
  }

  @Test
  void utf8BomIsStripped() throws Exception {
    String csv = "\uFEFFid,title,body\nh,Home,Hi\n";
    List<Map<String, String>> rows = VirtualCsvParser.parse(csv, "bom.csv");
    assertEquals(1, rows.size());
    assertEquals("h", VirtualCsvParser.cell(rows.get(0), "id"));
  }
}
