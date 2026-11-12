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

package com.percussion.webservices.ui.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.cms.objectstore.PSSearch;
import com.percussion.cms.objectstore.PSSearchField;
import com.percussion.xml.PSXmlDocumentBuilder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * #6136: the saved-search update leg can report success while writing zero
 * child statements on H2, so field add/change/remove via
 * {@code PUT /Rhythmyx/services/searches/{id}} is silently dropped while the
 * parent row looks current. {@code ensureSearchRowCurrent} reconciles
 * {@code PSX_SEARCHFIELDS} / {@code PSX_SEARCHPROPERTIES} from the saved
 * domain; these in-memory H2 tests prove the merge (insert / update /
 * delete / convergence) in the same style as
 * {@code PSUiDesignWsSaveRoutingTest}.
 */
@Tag("UnitTest")
class PSUiDesignWsChildRepairTest {

  private static final String CREATE_SEARCHES_TABLE =
      "CREATE TABLE PSX_SEARCHES (SEARCHID INTEGER NOT NULL PRIMARY KEY,"
          + " INTERNALNAME VARCHAR(255) NOT NULL, DISPLAYNAME VARCHAR(255) NOT NULL,"
          + " PARENTCATEGORY INTEGER NOT NULL, CUSTOMURL VARCHAR(255), TYPE VARCHAR(50),"
          + " DISPLAYFORMAT INTEGER, MAXIMUMITEMS INTEGER NOT NULL,"
          + " DESCRIPTION VARCHAR(255), CASESENSITIVE INTEGER NOT NULL, VERSION INTEGER NOT NULL)";

  private static final String CREATE_FIELDS_TABLE =
      "CREATE TABLE PSX_SEARCHFIELDS (SEARCHID INTEGER NOT NULL, FIELDNAME VARCHAR(50) NOT NULL,"
          + " FIELDLABEL VARCHAR(255) NOT NULL, FIELDTYPE VARCHAR(255) NOT NULL,"
          + " OPERATOR VARCHAR(50) NOT NULL, FIELDVALUE VARCHAR(2056), SEQUENCE INTEGER,"
          + " FIELDESCRIPTION VARCHAR(255), EXTOPERATOR VARCHAR(50),"
          + " PRIMARY KEY (SEARCHID, FIELDNAME))";

  private static final String CREATE_PROPERTIES_TABLE =
      "CREATE TABLE PSX_SEARCHPROPERTIES (PROPERTYID INTEGER NOT NULL,"
          + " PROPERTYNAME VARCHAR(50) NOT NULL, PROPERTYVALUE VARCHAR(100) NOT NULL,"
          + " PROPERTYDESCRIPTION VARCHAR(255),"
          + " PRIMARY KEY (PROPERTYID, PROPERTYNAME, PROPERTYVALUE))";

  private Connection openChildTables() throws Exception {
    Connection conn = DriverManager.getConnection("jdbc:h2:mem:childrepair" + System.nanoTime());
    try (Statement st = conn.createStatement()) {
      st.execute(CREATE_SEARCHES_TABLE);
      st.execute(CREATE_FIELDS_TABLE);
      st.execute(CREATE_PROPERTIES_TABLE);
    }
    return conn;
  }

  private PSSearch childDomain(int id, String name, String label) throws Exception {
    PSSearch search = new PSSearch(name);
    search.setLocator(PSSearch.createKey(new String[] {String.valueOf(id)}));
    search.setDisplayName(label);
    search.setType(PSSearch.TYPE_STANDARDSEARCH);
    return search;
  }

  private void seedParent(Connection conn, int id, String name, String label) throws Exception {
    PSUiDesignWs.insertSearchRow(
        conn,
        new PSUiDesignWs.SearchRowSpec(
            id, name, label, 1, null, PSSearch.TYPE_STANDARDSEARCH, 1, 100, null, 0, 1));
  }

  private PSSearchField domainField(
      String name, String label, String type, String operator, String value, int position) {
    PSSearchField field = new PSSearchField(name, label, "", type, "");
    if (value == null) {
      field.setFieldValues(PSSearchField.OP_ISNULL, new java.util.ArrayList<>());
    } else {
      field.setFieldValues(operator, new java.util.ArrayList<>(List.of(value)));
    }
    field.setPosition(position);
    return field;
  }

  /**
   * Mirrors {@code PSUiDesignWs.prepareSearchForSave} for persisted searches:
   * community properties are stripped before the update leg, so the saved
   * domain never carries them.
   */
  private void stripCommunity(PSSearch search) {
    String[] values = search.getPropertyValues(PSSearch.PROP_COMMUNITY);
    if (values != null) {
      for (String community : values) {
        search.removeProperty(PSSearch.PROP_COMMUNITY, community);
      }
    }
  }

  private String readFieldValue(Connection conn, int searchId, String fieldName) throws Exception {
    try (PreparedStatement ps =
        conn.prepareStatement("SELECT FIELDVALUE FROM PSX_SEARCHFIELDS WHERE SEARCHID = ? AND FIELDNAME = ?")) {
      ps.setInt(1, searchId);
      ps.setString(2, fieldName);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() ? rs.getString(1) : null;
      }
    }
  }

  private boolean hasPropertyRow(Connection conn, int propertyId, String name, String value)
      throws Exception {
    try (PreparedStatement ps =
        conn.prepareStatement(
            "SELECT PROPERTYVALUE FROM PSX_SEARCHPROPERTIES WHERE PROPERTYID = ? AND PROPERTYNAME = ? AND PROPERTYVALUE = ?")) {
      ps.setInt(1, propertyId);
      ps.setString(2, name);
      ps.setString(3, value);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }
    }
  }

  @Test
  void repair_missingChildrenAreInsertedAndConverge() throws Exception {
    try (Connection conn = openChildTables()) {
      seedParent(conn, 5101, "QaChildAdd", "QA child add");
      PSSearch saved = childDomain(5101, "QaChildAdd", "QA child add");
      saved.getFieldContainer().add(domainField("sys_title", "Title", "Text", "like", "alpha", 0));
      saved.getFieldContainer().add(domainField("sys_id", "Id", "Number", "equal", "7", 1));
      saved.setProperty("folderPath", "//Sites/Qa", true);

      assertTrue(PSUiDesignWs.ensureSearchRowCurrent(conn, saved));

      Map<String, PSUiDesignWs.SearchFieldSpec> fields = PSUiDesignWs.readSearchFields(conn, 5101);
      assertEquals(2, fields.size());
      assertEquals("alpha,", fields.get("sys_title").fieldValue);
      assertEquals("equal", fields.get("sys_id").operator);
      assertEquals(1, fields.get("sys_id").sequence);
      assertTrue(hasPropertyRow(conn, 5101, "folderPath", "//Sites/Qa"));

      // Second pass over identical state must be a no-op (no churn).
      assertFalse(PSUiDesignWs.ensureSearchRowCurrent(conn, saved));
    }
  }

  @Test
  void repair_changedFieldIsUpdated() throws Exception {
    try (Connection conn = openChildTables()) {
      seedParent(conn, 5102, "QaChildChange", "QA child change");
      PSSearch stale =
          childDomain(5102, "QaChildChange", "QA child change");
      stale.getFieldContainer().add(domainField("sys_title", "Title", "Text", "like", "old", 0));
      assertTrue(PSUiDesignWs.ensureSearchRowCurrent(conn, stale));

      PSSearch saved = childDomain(5102, "QaChildChange", "QA child change");
      // Container order drives SEQUENCE (PSDbComponentList.add resequences),
      // so the changed field sits at index 1.
      saved.getFieldContainer().add(domainField("sys_id", "Id", "Number", "equal", "7", 0));
      PSSearchField changed = domainField("sys_title", "Renamed", "Text", "equal", "new", 1);
      changed.setFieldDescription("desc");
      saved.getFieldContainer().add(changed);

      assertTrue(PSUiDesignWs.ensureSearchRowCurrent(conn, saved));

      Map<String, PSUiDesignWs.SearchFieldSpec> fields = PSUiDesignWs.readSearchFields(conn, 5102);
      assertEquals(2, fields.size());
      PSUiDesignWs.SearchFieldSpec row = fields.get("sys_title");
      assertEquals("Renamed", row.label);
      assertEquals("equal", row.operator);
      assertEquals("new,", row.fieldValue);
      assertEquals(1, row.sequence);
      assertEquals("desc", row.description);
      assertFalse(PSUiDesignWs.ensureSearchRowCurrent(conn, saved));
    }
  }

  @Test
  void repair_removedFieldIsDeleted() throws Exception {
    try (Connection conn = openChildTables()) {
      seedParent(conn, 5103, "QaChildRemove", "QA child remove");
      PSSearch before = childDomain(5103, "QaChildRemove", "QA child remove");
      before.getFieldContainer().add(domainField("sys_title", "Title", "Text", "like", "keep", 0));
      before.getFieldContainer().add(domainField("sys_id", "Id", "Number", "equal", "drop", 1));
      assertTrue(PSUiDesignWs.ensureSearchRowCurrent(conn, before));

      PSSearch saved = childDomain(5103, "QaChildRemove", "QA child remove");
      saved.getFieldContainer().add(domainField("sys_title", "Title", "Text", "like", "keep", 0));

      assertTrue(PSUiDesignWs.ensureSearchRowCurrent(conn, saved));

      Map<String, PSUiDesignWs.SearchFieldSpec> fields = PSUiDesignWs.readSearchFields(conn, 5103);
      assertEquals(1, fields.size());
      assertTrue(fields.containsKey("sys_title"));
      assertNull(readFieldValue(conn, 5103, "sys_id"));
      assertFalse(PSUiDesignWs.ensureSearchRowCurrent(conn, saved));
    }
  }

  @Test
  void repair_removedPropertyValueIsDeleted() throws Exception {
    try (Connection conn = openChildTables()) {
      seedParent(conn, 5104, "QaChildProp", "QA child prop");
      PSSearch before = childDomain(5104, "QaChildProp", "QA child prop");
      before.setProperty("folderPath", "//Sites/Keep", true);
      before.setProperty("folderPath", "//Sites/Drop", true);
      assertTrue(PSUiDesignWs.ensureSearchRowCurrent(conn, before));
      assertTrue(hasPropertyRow(conn, 5104, "folderPath", "//Sites/Drop"));

      PSSearch saved = childDomain(5104, "QaChildProp", "QA child prop");
      saved.setProperty("folderPath", "//Sites/Keep", true);

      assertTrue(PSUiDesignWs.ensureSearchRowCurrent(conn, saved));

      assertTrue(hasPropertyRow(conn, 5104, "folderPath", "//Sites/Keep"));
      assertFalse(hasPropertyRow(conn, 5104, "folderPath", "//Sites/Drop"));
      assertFalse(PSUiDesignWs.ensureSearchRowCurrent(conn, saved));
    }
  }

  @Test
  void repair_unmanagedPropertyNameIsLeftAlone() throws Exception {
    // prepareSearchForSave strips sys_community before the update leg, so the
    // saved domain never carries it. The repair must not delete those rows.
    try (Connection conn = openChildTables()) {
      seedParent(conn, 5105, "QaChildComm", "QA child community");
      try (Statement st = conn.createStatement()) {
        st.execute(
            "INSERT INTO PSX_SEARCHPROPERTIES (PROPERTYID, PROPERTYNAME, PROPERTYVALUE)"
                + " VALUES (5105, 'sys_community', '1001')");
      }
      PSSearch saved = childDomain(5105, "QaChildComm", "QA child community");
      // Production strips community before the update leg; the domain must
      // then preserve the pre-existing community row instead of deleting it.
      stripCommunity(saved);
      saved.getFieldContainer().add(domainField("sys_title", "Title", "Text", "like", "v", 0));

      assertTrue(PSUiDesignWs.ensureSearchRowCurrent(conn, saved));

      assertTrue(hasPropertyRow(conn, 5105, "sys_community", "1001"));
    }
  }

  @Test
  void repair_labelRegressionStillHoldsWithChildren() throws Exception {
    // #6119 behavior: stale parent label is repaired in the same pass that
    // reconciles children.
    try (Connection conn = openChildTables()) {
      seedParent(conn, 5106, "QaChildLabel", "Old label");
      PSSearch saved = childDomain(5106, "QaChildLabel", "New label");
      saved.getFieldContainer().add(domainField("sys_title", "Title", "Text", "like", "v", 0));

      assertTrue(PSUiDesignWs.ensureSearchRowCurrent(conn, saved));

      assertEquals("New label", PSUiDesignWs.readSearchRow(conn, 5106).displayName);
      assertEquals("v,", readFieldValue(conn, 5106, "sys_title"));
      assertFalse(PSUiDesignWs.ensureSearchRowCurrent(conn, saved));
    }
  }

  @Test
  void encodeSearchFieldValue_matchesFieldToXml() throws Exception {
    PSSearchField plain = domainField("sys_title", "Title", "Text", "like", "alpha", 0);
    PSSearchField escaped = domainField("sys_title", "Title", "Text", "like", "a,b", 0);
    PSSearchField empty = domainField("sys_title", "Title", "Text", "like", null, 0);

    assertEquals(fieldValueFromToXml(plain), PSUiDesignWs.encodeSearchFieldValue(plain));
    assertEquals("alpha,", PSUiDesignWs.encodeSearchFieldValue(plain));
    assertEquals(fieldValueFromToXml(escaped), PSUiDesignWs.encodeSearchFieldValue(escaped));
    assertEquals("a,,b,", PSUiDesignWs.encodeSearchFieldValue(escaped));
    assertNull(fieldValueFromToXml(empty));
    assertNull(PSUiDesignWs.encodeSearchFieldValue(empty));
  }

  private String fieldValueFromToXml(PSSearchField field) throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element root = field.toXml(doc);
    NodeList nodes = root.getElementsByTagName(PSSearchField.XML_NODE_FIELDVALUE);
    if (nodes.getLength() == 0) {
      return null;
    }
    return nodes.item(0).getTextContent();
  }
}
