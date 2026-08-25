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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.intsof.percussioncms.auditlog.codes.TableFactoryErrorCodes;
import com.percussion.xml.PSXmlDocumentBuilder;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Issue #3741 / parent #2616 slice 3: leftover TableFactory production {@code IPSTableFactoryErrors}
 * sites throw typed {@link TableFactoryErrorCodes}. All catalog codes are non-auditable (dual-write
 * skip).
 */
class PSJdbcTableFactoryTypedErrorCodeSliceTest {

  @Test
  void typedConstructionRetainsCatalogAndSkipsDualWrite() {
    PSJdbcTableFactoryException ex =
        new PSJdbcTableFactoryException(
            TableFactoryErrorCodes.XML_ELEMENT_WRONG_TYPE, new Object[] {"column", "row"});

    assertEquals(TableFactoryErrorCodes.XML_ELEMENT_WRONG_TYPE.numericCode(), ex.getErrorCode());
    assertSame(TableFactoryErrorCodes.XML_ELEMENT_WRONG_TYPE, ex.getTypedErrorCode());
    assertFalse(TableFactoryErrorCodes.XML_ELEMENT_WRONG_TYPE.isAuditable());
    assertFalse(ex.isAuditable());
  }

  @Test
  void typedConstructionRejectsNullCode() {
    assertThrows(
        IllegalArgumentException.class, () -> new PSJdbcTableFactoryException(null, "arg"));
  }

  @Test
  void columnDataWrongXmlElementThrowsTypedWrongType() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element wrong = doc.createElement("row");

    PSJdbcTableFactoryException ex =
        assertThrows(PSJdbcTableFactoryException.class, () -> new PSJdbcColumnData(wrong));

    assertEquals(TableFactoryErrorCodes.XML_ELEMENT_WRONG_TYPE.numericCode(), ex.getErrorCode());
    assertSame(TableFactoryErrorCodes.XML_ELEMENT_WRONG_TYPE, ex.getTypedErrorCode());
    assertFalse(ex.isAuditable());
  }

  @Test
  void schemaHandlerMissingClassThrowsTypedDataHandlerNotFound() throws Exception {
    Document doc = PSXmlDocumentBuilder.createXmlDocument();
    Element handler = doc.createElement(PSJdbcTableSchemaHandler.NODE_NAME);
    handler.setAttribute("type", PSJdbcTableSchemaHandler.TYPE_STR_ON_CREATE);
    Element handlersEl = doc.createElement("dataHandlers");
    Element classEl = doc.createElement(IPSJdbcTableDataHandler.NODE_NAME);
    classEl.setAttribute(IPSJdbcTableDataHandler.CLASS_ATTR, "com.percussion.does.not.ExistHandler");
    handlersEl.appendChild(classEl);
    handler.appendChild(handlersEl);

    PSJdbcTableFactoryException ex =
        assertThrows(PSJdbcTableFactoryException.class, () -> new PSJdbcTableSchemaHandler(handler));

    assertEquals(
        TableFactoryErrorCodes.DATA_HANDLER_CLASS_NOT_FOUND.numericCode(), ex.getErrorCode());
    assertSame(TableFactoryErrorCodes.DATA_HANDLER_CLASS_NOT_FOUND, ex.getTypedErrorCode());
    assertFalse(ex.isAuditable());
  }
}
