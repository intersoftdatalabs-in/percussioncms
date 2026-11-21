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
package com.percussion.tablefactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.percussion.xml.PSXmlDocumentBuilder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Guard for #6914: the Arrange menu family ({@code ACTIONID 111-115} scoped to {@code MODEID 4} /
 * CXMAIN) added to the installer seed by #6886 must also reach existing (upgrade) installs.
 *
 * <p>The supported upgrade path ({@code upgrade.chain} &rarr; {@code setupDB} &rarr; {@code
 * installRepository} &rarr; {@code PSTableAction} over {@code cmsTableDef.xml} + {@code
 * cmsTableData.xml}) re-processes table data through {@link PSJdbcTableFactory} on every upgrade.
 * Rows stamped {@code action="i"} (insert-if-not-exist) with {@code onTableCreateOnly="no"} are
 * inserted when missing while pre-existing rows survive via swallowed integrity-constraint
 * violations, so the seed rows backfill automatically &mdash; but only while those two attributes
 * stay exactly as shipped.
 *
 * <p>This test pins that contract against the real installer seed files (path resolved from the
 * TableFactory module directory, {@link Path#of(String, String...)} only) and replays the upgrade
 * &mdash; pre-fix seed first, then the current seed &mdash; against H2, the product embedded
 * backend, proving the five rows are backfilled and re-runs are no-ops.
 */
public class PSJdbcInsertIfNotExistUpgradeBackfillTest {

  /** Installer seed files owned by perc-distribution-tree, resolved from this module directory. */
  private static final Path DATA_DIR =
      Path.of(
          "..",
          "perc-distribution-tree",
          "src",
          "main",
          "resources",
          "distribution",
          "rxconfig",
          "Installer",
          "data");

  private static final Path TABLE_DEF = DATA_DIR.resolve("cmsTableDef.xml");

  private static final Path TABLE_DATA = DATA_DIR.resolve("cmsTableData.xml");

  private static final String TABLE = "RXMODEUICONTEXTACTION";

  /** Arrange family action ids scoped to CXMAIN by #6886. */
  private static final List<String> ARRANGE_ACTION_IDS = List.of("111", "112", "113", "114", "115");

  @Test
  void arrangeCxMainSeedRowsAreUpgradeSafe() throws Exception {
    assertTrue(Files.isRegularFile(TABLE_DATA), () -> TABLE_DATA.toAbsolutePath().toString());
    Element tableEl = findTable(parse(TABLE_DATA), TABLE);
    assertTrue(tableEl != null, TABLE + " table missing from " + TABLE_DATA);
    assertEquals(
        "no",
        tableEl.getAttribute("onCreateOnly"),
        TABLE + " must keep onCreateOnly=\"no\" so upgrades process its rows (#6914)");

    int found = 0;
    NodeList children = tableEl.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      if (!(children.item(i) instanceof Element row) || !"row".equals(row.getTagName())) {
        continue;
      }
      if (!isArrangeCxMainRow(row)) {
        continue;
      }
      found++;
      assertEquals(
          "i",
          row.getAttribute("action"),
          "Arrange CXMAIN row must stay action=\"i\" (insert-if-not-exist) "
              + "so upgrades backfill without PK aborts (#6914)");
      assertEquals(
          "no",
          row.getAttribute("onTableCreateOnly"),
          "Arrange CXMAIN row must stay onTableCreateOnly=\"no\" "
              + "so upgrades process it on existing tables (#6914)");
    }
    assertEquals(
        ARRANGE_ACTION_IDS.size(),
        found,
        "expected " + ARRANGE_ACTION_IDS.size() + " Arrange CXMAIN seed rows (#6886)");
  }

  @Test
  void upgradePassBackfillsMissingArrangeCxMainRows() throws Exception {
    assertTrue(Files.isRegularFile(TABLE_DEF), () -> TABLE_DEF.toAbsolutePath().toString());
    assertTrue(Files.isRegularFile(TABLE_DATA), () -> TABLE_DATA.toAbsolutePath().toString());

    PSJdbcDataTypeMap dataTypeMap = new PSJdbcDataTypeMap("H2", "h2", null);
    // Only the RXMODEUICONTEXTACTION table: other tables in cmsTableDef.xml reference
    // installer datahandler classes (system module) that are not on this classpath.
    // This table's own definition is self-contained; assert its shape, then reuse it verbatim.
    Element tableDefEl = findTable(parse(TABLE_DEF), TABLE);
    assertTrue(tableDefEl != null, TABLE + " table missing from " + TABLE_DEF);
    assertTableDefShape(tableDefEl);
    Document schemaDoc = newDocument();
    Element tablesRoot = schemaDoc.createElement("tables");
    schemaDoc.appendChild(tablesRoot);
    tablesRoot.appendChild(schemaDoc.importNode(tableDefEl, true));
    PSJdbcTableSchemaCollection schemaColl = new PSJdbcTableSchemaCollection(schemaDoc, dataTypeMap);
    PSJdbcTableSchema schema = schemaColl.getTableSchema(TABLE);
    assertTrue(schema != null, TABLE + " schema missing from " + TABLE_DEF);

    PSJdbcTableDataCollection dataColl = new PSJdbcTableDataCollection(parse(TABLE_DATA));
    PSJdbcTableData currentData = dataColl.getTableData(TABLE);
    assertTrue(currentData != null, TABLE + " data missing from " + TABLE_DATA);

    // Pre-fix seed: everything the current file carries except the five #6886 rows,
    // i.e. the state of a repository installed before the fix.
    List<PSJdbcRowData> preFixRows = new ArrayList<>();
    Iterator<PSJdbcRowData> rows = currentData.getRows();
    while (rows.hasNext()) {
      PSJdbcRowData row = rows.next();
      if (!isArrangeCxMainRow(row)) {
        preFixRows.add(row);
      }
    }

    Properties props = new Properties();
    props.setProperty("DB_BACKEND", "H2");
    props.setProperty("DB_DRIVER_NAME", "h2");
    props.setProperty("DB_DRIVER_CLASS_NAME", "org.h2.Driver");
    props.setProperty("DB_SERVER", "mem");
    props.setProperty("DB_NAME", "");
    props.setProperty("DB_SCHEMA", "");
    props.setProperty("UID", "sa");
    props.setProperty("PWD", "");
    PSJdbcDbmsDef dbmsDef = new PSJdbcDbmsDef(props);

    // Static reference (not Class.forName) so dependency analysis sees the test-scope use.
    org.h2.Driver.load();
    try (Connection conn =
        DriverManager.getConnection("jdbc:h2:mem:arrangeBackfill;DB_CLOSE_DELAY=-1", "sa", "")) {

      // Pass 1: pre-fix install seeds the existing table (schema is created here).
      schema.setTableData(new PSJdbcTableData(TABLE, preFixRows.iterator()));
      PSJdbcTableFactory.processTable(conn, dbmsDef, schema, null, false);
      assertArrangeRowCount(conn, 0);
      long seeded = countRows(conn);

      // Pass 2: upgrade re-processes the CURRENT seed file (what setupDB does on
      // upgrade.chain). Must insert exactly the five missing rows and touch nothing else.
      schema.setTableData(currentData);
      try {
        PSJdbcTableFactory.processTable(conn, dbmsDef, schema, null, false);
      } catch (Exception e) {
        fail("upgrade pass over existing " + TABLE + " must not abort (#6914): " + e);
      }
      assertArrangeRowCount(conn, ARRANGE_ACTION_IDS.size());
      assertEquals(
          seeded + ARRANGE_ACTION_IDS.size(),
          countRows(conn),
          "upgrade pass must add exactly the missing Arrange CXMAIN rows");

      // Pass 3: re-running the upgrade pass is a no-op (idempotent, no duplicates).
      PSJdbcTableFactory.processTable(conn, dbmsDef, schema, null, false);
      assertArrangeRowCount(conn, ARRANGE_ACTION_IDS.size());
      assertEquals(seeded + ARRANGE_ACTION_IDS.size(), countRows(conn), "re-run must be a no-op");
    }
  }

  private static boolean isArrangeCxMainRow(Element row) {
    String mode = null;
    String context = null;
    String action = null;
    NodeList cols = row.getElementsByTagName("column");
    for (int i = 0; i < cols.getLength(); i++) {
      if (!(cols.item(i) instanceof Element col)) {
        continue;
      }
      switch (col.getAttribute("name")) {
        case "MODEID" -> mode = col.getTextContent().trim();
        case "UICONTEXTID" -> context = col.getTextContent().trim();
        case "ACTIONID" -> action = col.getTextContent().trim();
        default -> {}
      }
    }
    return "4".equals(mode) && "3".equals(context) && ARRANGE_ACTION_IDS.contains(action);
  }

  private static boolean isArrangeCxMainRow(PSJdbcRowData row) {
    String mode = null;
    String context = null;
    String action = null;
    Iterator<PSJdbcColumnData> cols = row.getColumns();
    while (cols.hasNext()) {
      PSJdbcColumnData col = cols.next();
      switch (col.getName()) {
        case "MODEID" -> mode = col.getValue();
        case "UICONTEXTID" -> context = col.getValue();
        case "ACTIONID" -> action = col.getValue();
        default -> {}
      }
    }
    return "4".equals(mode) && "3".equals(context) && ARRANGE_ACTION_IDS.contains(action);
  }

  private static Element findTable(Document doc, String tableName) {
    NodeList tables = doc.getElementsByTagName("table");
    for (int i = 0; i < tables.getLength(); i++) {
      if (tables.item(i) instanceof Element el && tableName.equals(el.getAttribute("name"))) {
        return el;
      }
    }
    return null;
  }

  /**
   * The H2 replay below builds its schema from the real installer definition; pin the shape the
   * replay (and the upgrade path) relies on: exactly the three {@code INTEGER NOT NULL} key
   * columns with a composite primary key, no datahandlers.
   */
  private static void assertTableDefShape(Element tableDefEl) {
    List<String> columns = new ArrayList<>();
    NodeList rows = tableDefEl.getElementsByTagName("row");
    for (int i = 0; i < rows.getLength(); i++) {
      if (!(rows.item(i) instanceof Element row)) {
        continue;
      }
      NodeList cols = row.getElementsByTagName("column");
      for (int j = 0; j < cols.getLength(); j++) {
        if (!(cols.item(j) instanceof Element col)) {
          continue;
        }
        columns.add(col.getAttribute("name"));
        assertEquals(
            "INTEGER",
            textOf(col, "jdbctype"),
            TABLE + "." + col.getAttribute("name") + " must stay INTEGER (#6914)");
        assertEquals(
            "no",
            textOf(col, "allowsnull"),
            TABLE + "." + col.getAttribute("name") + " must stay NOT NULL (#6914)");
      }
    }
    assertEquals(
        List.of("MODEID", "UICONTEXTID", "ACTIONID"),
        columns,
        TABLE + " columns must stay exactly the composite key (#6914)");

    List<String> pk = new ArrayList<>();
    NodeList pks = tableDefEl.getElementsByTagName("primarykey");
    for (int i = 0; i < pks.getLength(); i++) {
      if (!(pks.item(i) instanceof Element pkEl)) {
        continue;
      }
      NodeList names = pkEl.getElementsByTagName("name");
      for (int j = 0; j < names.getLength(); j++) {
        pk.add(names.item(j).getTextContent().trim());
      }
    }
    assertEquals(
        List.of("MODEID", "UICONTEXTID", "ACTIONID"),
        pk,
        TABLE + " primary key must stay the composite key (#6914)");
  }

  private static String textOf(Element parent, String tag) {
    NodeList nodes = parent.getElementsByTagName(tag);
    if (nodes.getLength() == 0) {
      return "";
    }
    return nodes.item(0).getTextContent().trim();
  }

  private static long countRows(Connection conn) throws Exception {
    try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM " + TABLE);
        ResultSet rs = ps.executeQuery()) {
      rs.next();
      return rs.getLong(1);
    }
  }

  private static void assertArrangeRowCount(Connection conn, int expected) throws Exception {
    try (PreparedStatement ps =
            conn.prepareStatement(
                "SELECT COUNT(*) FROM "
                    + TABLE
                    + " WHERE MODEID = 4 AND UICONTEXTID = 3"
                    + " AND ACTIONID IN (111, 112, 113, 114, 115)");
        ResultSet rs = ps.executeQuery()) {
      rs.next();
      assertEquals(expected, rs.getInt(1), "Arrange CXMAIN backfill row count (#6914)");
    }
  }

  private static Document parse(Path path) throws Exception {
    try (var in = Files.newInputStream(path)) {
      return PSXmlDocumentBuilder.createXmlDocument(in, false);
    }
  }

  private static Document newDocument() throws Exception {
    var factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(false);
    return factory.newDocumentBuilder().newDocument();
  }
}
