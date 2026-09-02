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
package com.percussion.install;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.intsof.percussioncms.auditlog.codes.TableFactoryErrorCodes;
import com.percussion.tablefactory.IPSJdbcTableDataHandler;
import com.percussion.tablefactory.IPSTableFactoryErrors;
import com.percussion.tablefactory.PSJdbcColumnData;
import com.percussion.tablefactory.PSJdbcColumnDef;
import com.percussion.tablefactory.PSJdbcDataTypeMap;
import com.percussion.tablefactory.PSJdbcDbmsDef;
import com.percussion.tablefactory.PSJdbcRowData;
import com.percussion.tablefactory.PSJdbcTableComponent;
import com.percussion.tablefactory.PSJdbcTableFactoryException;
import com.percussion.tablefactory.PSJdbcTableMapping;
import com.percussion.tablefactory.PSJdbcTableSchema;
import com.percussion.xml.PSXmlDocumentBuilder;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Types;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Issue #4157 (parent #2616 leftover): install JDBC table-factory helpers throw typed {@link
 * TableFactoryErrorCodes} rather than bare {@code IPSTableFactoryErrors} ints. All leftover catalog
 * codes are non-auditable (dual-write skip).
 */
@Tag("UnitTest")
class PSInstallJdbcTypedErrorCodeSliceTest {

  @Test
  void leftoverCatalogsMatchLegacyIntsAndSkipDualWrite() {
    assertEquals(
        IPSTableFactoryErrors.XML_ELEMENT_NULL,
        TableFactoryErrorCodes.XML_ELEMENT_NULL.numericCode());
    assertEquals(
        IPSTableFactoryErrors.XML_ELEMENT_WRONG_TYPE,
        TableFactoryErrorCodes.XML_ELEMENT_WRONG_TYPE.numericCode());
    assertEquals(
        IPSTableFactoryErrors.XML_ELEMENT_INVALID_ATTR,
        TableFactoryErrorCodes.XML_ELEMENT_INVALID_ATTR.numericCode());
    assertEquals(
        IPSTableFactoryErrors.COLUMN_NOT_FOUND,
        TableFactoryErrorCodes.COLUMN_NOT_FOUND.numericCode());
    assertEquals(
        IPSTableFactoryErrors.LOAD_DEFAULT_DATATYPE_MAP,
        TableFactoryErrorCodes.LOAD_DEFAULT_DATATYPE_MAP.numericCode());
    assertEquals(
        IPSTableFactoryErrors.SQL_CATALOG_DATA,
        TableFactoryErrorCodes.SQL_CATALOG_DATA.numericCode());
    assertEquals(
        IPSTableFactoryErrors.CHECK_EXISTING_DATA,
        TableFactoryErrorCodes.CHECK_EXISTING_DATA.numericCode());
    assertEquals(
        IPSTableFactoryErrors.DATA_PROCESS_ERROR,
        TableFactoryErrorCodes.DATA_PROCESS_ERROR.numericCode());

    assertFalse(TableFactoryErrorCodes.XML_ELEMENT_NULL.isAuditable());
    assertFalse(TableFactoryErrorCodes.XML_ELEMENT_WRONG_TYPE.isAuditable());
    assertFalse(TableFactoryErrorCodes.XML_ELEMENT_INVALID_ATTR.isAuditable());
    assertFalse(TableFactoryErrorCodes.COLUMN_NOT_FOUND.isAuditable());
    assertFalse(TableFactoryErrorCodes.LOAD_DEFAULT_DATATYPE_MAP.isAuditable());
    assertFalse(TableFactoryErrorCodes.SQL_CATALOG_DATA.isAuditable());
    assertFalse(TableFactoryErrorCodes.CHECK_EXISTING_DATA.isAuditable());
    assertFalse(TableFactoryErrorCodes.DATA_PROCESS_ERROR.isAuditable());
  }

  @Test
  void typedConstructionRetainsCatalogAndSkipsDualWrite() {
    leftoverNonAuditable(
        new PSJdbcTableFactoryException(
            TableFactoryErrorCodes.XML_ELEMENT_WRONG_TYPE, new Object[] {"dataHandler", "row"}),
        TableFactoryErrorCodes.XML_ELEMENT_WRONG_TYPE);
    leftoverNonAuditable(
        new PSJdbcTableFactoryException(TableFactoryErrorCodes.XML_ELEMENT_NULL, "column"),
        TableFactoryErrorCodes.XML_ELEMENT_NULL);
    leftoverNonAuditable(
        new PSJdbcTableFactoryException(
            TableFactoryErrorCodes.COLUMN_NOT_FOUND, new Object[] {"t", "c"}),
        TableFactoryErrorCodes.COLUMN_NOT_FOUND);
    leftoverNonAuditable(
        new PSJdbcTableFactoryException(
            TableFactoryErrorCodes.DATA_PROCESS_ERROR,
            new Object[] {"dest", "sql"},
            new RuntimeException("cause")),
        TableFactoryErrorCodes.DATA_PROCESS_ERROR);
    leftoverNonAuditable(
        new PSJdbcTableFactoryException(
            TableFactoryErrorCodes.SQL_CATALOG_DATA,
            new Object[] {"TRANSITIONS", "sql"},
            new RuntimeException("cause")),
        TableFactoryErrorCodes.SQL_CATALOG_DATA);
    leftoverNonAuditable(
        new PSJdbcTableFactoryException(
            TableFactoryErrorCodes.CHECK_EXISTING_DATA, new Object[] {"t", "null col"}),
        TableFactoryErrorCodes.CHECK_EXISTING_DATA);
    leftoverNonAuditable(
        new PSJdbcTableFactoryException(
            TableFactoryErrorCodes.LOAD_DEFAULT_DATATYPE_MAP, "boom", new RuntimeException("cause")),
        TableFactoryErrorCodes.LOAD_DEFAULT_DATATYPE_MAP);
  }

  @Test
  void uniqueColumnWrongXmlElementThrowsTypedWrongType() {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element wrong = doc.createElement("row");

    leftoverNonAuditable(
        assertThrows(
            PSJdbcTableFactoryException.class, () -> new PSJdbcUniqueColumn().fromXml(wrong)),
        TableFactoryErrorCodes.XML_ELEMENT_WRONG_TYPE);
  }

  @Test
  void uniqueColumnMissingColumnElementThrowsTypedNull() {
    leftoverNonAuditable(
        assertThrows(
            PSJdbcTableFactoryException.class,
            () -> new PSJdbcUniqueColumn().fromXml(emptyHandler(PSXmlDocumentBuilder.createXmlDocument()))),
        TableFactoryErrorCodes.XML_ELEMENT_NULL);
  }

  @Test
  void uniqueColumnMissingNameAttrThrowsTypedInvalidAttr() {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element handler = emptyHandler(doc);
    Element column = doc.createElement("column");
    column.setAttribute("value", "BASE");
    handler.appendChild(column);

    leftoverNonAuditable(
        assertThrows(
            PSJdbcTableFactoryException.class, () -> new PSJdbcUniqueColumn().fromXml(handler)),
        TableFactoryErrorCodes.XML_ELEMENT_INVALID_ATTR);
  }

  @Test
  void uniqueColumnInitMissingColumnThrowsTypedNotFound() throws Exception {
    PSJdbcUniqueColumn handler = new PSJdbcUniqueColumn();
    handler.fromXml(uniqueColumnXml("targetCol", "BASE"));

    leftoverNonAuditable(
        assertThrows(
            PSJdbcTableFactoryException.class,
            () ->
                handler.init(
                    new PSJdbcDbmsDef(), null, null, schemaWithColumn("otherCol"))),
        TableFactoryErrorCodes.COLUMN_NOT_FOUND);
  }

  @Test
  void nextNumberColumnWrongXmlElementThrowsTypedWrongType() {
    leftoverNonAuditable(
        assertThrows(
            PSJdbcTableFactoryException.class,
            () ->
                new PSJdbcNextNumberColumn()
                    .fromXml(PSXmlDocumentBuilder.createXmlDocument().createElement("row"))),
        TableFactoryErrorCodes.XML_ELEMENT_WRONG_TYPE);
  }

  @Test
  void nextNumberColumnMissingColumnElementThrowsTypedNull() {
    leftoverNonAuditable(
        assertThrows(
            PSJdbcTableFactoryException.class,
            () ->
                new PSJdbcNextNumberColumn()
                    .fromXml(emptyHandler(PSXmlDocumentBuilder.createXmlDocument()))),
        TableFactoryErrorCodes.XML_ELEMENT_NULL);
  }

  @Test
  void nextNumberColumnMissingNextNumberKeyThrowsTypedInvalidAttr() {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element handler = emptyHandler(doc);
    Element column = doc.createElement("column");
    column.setAttribute("name", "id");
    handler.appendChild(column);

    leftoverNonAuditable(
        assertThrows(
            PSJdbcTableFactoryException.class,
            () -> new PSJdbcNextNumberColumn().fromXml(handler)),
        TableFactoryErrorCodes.XML_ELEMENT_INVALID_ATTR);
  }

  @Test
  void nextNumberColumnInitMissingColumnThrowsTypedNotFound() throws Exception {
    PSJdbcNextNumberColumn handler = new PSJdbcNextNumberColumn();
    handler.fromXml(nextNumberColumnXml("targetCol", "CONTENTTYPES"));

    leftoverNonAuditable(
        assertThrows(
            PSJdbcTableFactoryException.class,
            () ->
                handler.init(
                    new PSJdbcDbmsDef(), null, null, schemaWithColumn("otherCol"))),
        TableFactoryErrorCodes.COLUMN_NOT_FOUND);
  }

  @Test
  void tableMapperWrongXmlElementThrowsTypedWrongType() {
    leftoverNonAuditable(
        assertThrows(
            PSJdbcTableFactoryException.class,
            () ->
                new PSJdbcTableMapper()
                    .fromXml(PSXmlDocumentBuilder.createXmlDocument().createElement("row"))),
        TableFactoryErrorCodes.XML_ELEMENT_WRONG_TYPE);
  }

  @Test
  void tableMapperMissingTableMapThrowsTypedNull() {
    leftoverNonAuditable(
        assertThrows(
            PSJdbcTableFactoryException.class,
            () ->
                new PSJdbcTableMapper()
                    .fromXml(emptyHandler(PSXmlDocumentBuilder.createXmlDocument()))),
        TableFactoryErrorCodes.XML_ELEMENT_NULL);
  }

  @Test
  void tableMapperMissingDestTableThrowsTypedInvalidAttr() {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element handler = emptyHandler(doc);
    Element tableMap = doc.createElement(PSJdbcTableMapping.NODE_NAME);
    tableMap.setAttribute("srcTable", "SRC");
    handler.appendChild(tableMap);

    leftoverNonAuditable(
        assertThrows(
            PSJdbcTableFactoryException.class, () -> new PSJdbcTableMapper().fromXml(handler)),
        TableFactoryErrorCodes.XML_ELEMENT_INVALID_ATTR);
  }

  @Test
  void transitionRolesWrongXmlElementThrowsTypedWrongType() {
    leftoverNonAuditable(
        assertThrows(
            PSJdbcTableFactoryException.class,
            () ->
                new PSJdbcTransitionRoles()
                    .fromXml(PSXmlDocumentBuilder.createXmlDocument().createElement("row"))),
        TableFactoryErrorCodes.XML_ELEMENT_WRONG_TYPE);
  }

  @Test
  void transitionRolesMissingColumnThrowsTypedNotFound() throws Exception {
    leftoverNonAuditable(
        invokeRequiredColumnValue(
            rowWithColumns(new PSJdbcColumnData("OTHER", "1")), "TRANSITIONS", "TRANSITIONID"),
        TableFactoryErrorCodes.COLUMN_NOT_FOUND);
  }

  @Test
  void transitionRolesNullColumnValueThrowsTypedCheckExistingData() throws Exception {
    leftoverNonAuditable(
        invokeRequiredColumnValue(
            rowWithColumns(new PSJdbcColumnData("TRANSITIONID", null)),
            "TRANSITIONS",
            "TRANSITIONID"),
        TableFactoryErrorCodes.CHECK_EXISTING_DATA);
  }

  private static void leftoverNonAuditable(
      PSJdbcTableFactoryException ex, TableFactoryErrorCodes expected) {
    assertEquals(expected.numericCode(), ex.getErrorCode());
    assertSame(expected, ex.getTypedErrorCode());
    assertFalse(expected.isAuditable());
    assertFalse(ex.isAuditable());
  }

  private static Element emptyHandler(Document doc) {
    Element handler = doc.createElement(IPSJdbcTableDataHandler.NODE_NAME);
    handler.setAttribute(IPSJdbcTableDataHandler.CLASS_ATTR, "com.percussion.install.Test");
    return handler;
  }

  private static Element uniqueColumnXml(String columnName, String value) {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element handler = emptyHandler(doc);
    Element column = doc.createElement("column");
    column.setAttribute("name", columnName);
    column.setAttribute("value", value);
    handler.appendChild(column);
    return handler;
  }

  private static Element nextNumberColumnXml(String columnName, String key) {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element handler = emptyHandler(doc);
    Element column = doc.createElement("column");
    column.setAttribute("name", columnName);
    column.setAttribute("nextNumberKey", key);
    handler.appendChild(column);
    return handler;
  }

  private static PSJdbcTableSchema schemaWithColumn(String columnName) throws Exception {
    PSJdbcDataTypeMap map = new PSJdbcDataTypeMap("MSSQL", "sqlserver", null);
    PSJdbcColumnDef col =
        new PSJdbcColumnDef(
            map, columnName, PSJdbcTableComponent.ACTION_CREATE, Types.VARCHAR, "50", true, null);
    return new PSJdbcTableSchema("myTable", List.of(col).iterator());
  }

  private static PSJdbcRowData rowWithColumns(PSJdbcColumnData... columns) {
    return new PSJdbcRowData(List.of(columns).iterator(), PSJdbcRowData.ACTION_UPDATE);
  }

  private static PSJdbcTableFactoryException invokeRequiredColumnValue(
      PSJdbcRowData row, String tableName, String colName) throws Exception {
    Method method =
        PSJdbcTransitionRoles.class.getDeclaredMethod(
            "getRequiredColumnValue", PSJdbcRowData.class, String.class, String.class);
    method.setAccessible(true);
    try {
      method.invoke(new PSJdbcTransitionRoles(), row, tableName, colName);
    } catch (InvocationTargetException e) {
      if (e.getCause() instanceof PSJdbcTableFactoryException ex) {
        return ex;
      }
      throw e;
    }
    throw new AssertionError("expected PSJdbcTableFactoryException");
  }
}
