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

package com.percussion.webservices.ui.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.cms.objectstore.PSSearch;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * #6119: the saved-search update leg can report success while writing 0 rows
 * on H2 (PUT 200, then GET 404 with the row gone — or a stale row left in
 * place). {@code ensureSearchRowCurrent} verifies the persisted row after
 * every save and JDBC-repairs it, mirroring the create-path
 * {@code ensureSearchRowPersisted} safety net.
 */
@Tag("UnitTest")
class PSUiDesignWsSaveRoutingTest {

  private static final String CREATE_SEARCHES_TABLE =
      "CREATE TABLE PSX_SEARCHES (SEARCHID INTEGER NOT NULL PRIMARY KEY,"
          + " INTERNALNAME VARCHAR(255) NOT NULL, DISPLAYNAME VARCHAR(255) NOT NULL,"
          + " PARENTCATEGORY INTEGER NOT NULL, CUSTOMURL VARCHAR(255), TYPE VARCHAR(50),"
          + " DISPLAYFORMAT INTEGER, MAXIMUMITEMS INTEGER NOT NULL,"
          + " DESCRIPTION VARCHAR(255), CASESENSITIVE INTEGER NOT NULL, VERSION INTEGER NOT NULL)";

  private Connection openSearchTables() throws Exception {
    Connection conn = DriverManager.getConnection("jdbc:h2:mem:repair" + System.nanoTime());
    try (Statement st = conn.createStatement()) {
      st.execute(CREATE_SEARCHES_TABLE);
    }
    return conn;
  }

  private PSSearch repairDomain(int id, String name, String label) throws Exception {
    PSSearch search = new PSSearch(name);
    search.setLocator(PSSearch.createKey(new String[] {String.valueOf(id)}));
    search.setDisplayName(label);
    search.setType(PSSearch.TYPE_STANDARDSEARCH);
    return search;
  }

  @Test
  void repair_missingRowIsInserted() throws Exception {
    try (Connection conn = openSearchTables()) {
      assertTrue(PSUiDesignWs.ensureSearchRowCurrent(conn, repairDomain(4242, "QaRepair", "L")));

      assertTrue(PSUiDesignWs.searchNameExists(conn, "QaRepair"));
      assertEquals("L", PSUiDesignWs.readSearchRow(conn, 4242).displayName);
    }
  }

  @Test
  void repair_staleRowIsUpdated() throws Exception {
    try (Connection conn = openSearchTables()) {
      PSUiDesignWs.insertSearchRow(
          conn,
          new PSUiDesignWs.SearchRowSpec(
              4243, "QaStale", "Old label", 1, null, PSSearch.TYPE_STANDARDSEARCH, 1, 100, null,
              0, 1));

      assertTrue(PSUiDesignWs.ensureSearchRowCurrent(conn, repairDomain(4243, "QaStale", "New label")));

      assertEquals("New label", PSUiDesignWs.readSearchRow(conn, 4243).displayName);
    }
  }

  @Test
  void repair_currentRowIsUntouched() throws Exception {
    try (Connection conn = openSearchTables()) {
      PSUiDesignWs.insertSearchRow(
          conn,
          new PSUiDesignWs.SearchRowSpec(
              4244, "QaSame", "Same label", 1, null, PSSearch.TYPE_STANDARDSEARCH, 1, 100, null,
              0, 1));

      PSSearch same = repairDomain(4244, "QaSame", "Same label");
      same.setVersion(1);

      assertFalse(PSUiDesignWs.ensureSearchRowCurrent(conn, same));
    }
  }

  @Test
  void repair_nameVisibleUnderOtherIdIsLeftAlone() throws Exception {
    try (Connection conn = openSearchTables()) {
      PSUiDesignWs.insertSearchRow(
          conn,
          new PSUiDesignWs.SearchRowSpec(
              1, "Shared", "Shared label", 1, null, PSSearch.TYPE_STANDARDSEARCH, 1, 100, null,
              0, 1));

      assertFalse(PSUiDesignWs.ensureSearchRowCurrent(conn, repairDomain(2, "Shared", "Shared label")));

      assertFalse(PSUiDesignWs.searchIdExists(conn, 2));
    }
  }
}
