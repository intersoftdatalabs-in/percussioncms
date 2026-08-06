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
package com.percussion.deployer.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.percussion.deployer.objectstore.PSArchiveInfo;
import com.percussion.deployer.objectstore.PSArchiveManifest;
import com.percussion.tablefactory.PSJdbcColumnData;
import com.percussion.tablefactory.PSJdbcRowData;
import com.percussion.tablefactory.PSJdbcTableData;
import com.percussion.xml.PSXmlDocumentBuilder;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

class PSLogHandlerTest {

  @Test
  void testBuildArchiveLogSummaryTableDataDateFormat() throws Exception {
    PSArchiveInfo archiveInfo = mock(PSArchiveInfo.class);
    PSArchiveManifest archiveManifest = mock(PSArchiveManifest.class);

    Calendar cal = new GregorianCalendar(2026, Calendar.JULY, 21, 15, 30, 45);
    Date testDate = cal.getTime();
    when(archiveInfo.getArchiveRef()).thenReturn("archive-ref-1");
    when(archiveInfo.getUserName()).thenReturn("alice");
    when(archiveInfo.getServerName()).thenReturn("rhythmx-source");
    when(archiveInfo.getServerVersion()).thenReturn("8.2.0");
    when(archiveInfo.getServerBuildId()).thenReturn("2026.07.21");
    when(archiveInfo.getServerBuildDate()).thenReturn(testDate);
    when(archiveManifest.toXml(any(Document.class)))
        .thenAnswer(inv -> rootElement(inv.getArgument(0)));

    PSJdbcTableData tableData =
        PSLogHandler.buildArchiveLogSummaryTableData(
            42, "rhythmx-target:9992", archiveInfo, archiveManifest);
    assertNotNull(tableData);

    Iterator<PSJdbcRowData> rows = tableData.getRows();
    PSJdbcRowData row = rows.next();
    Map<String, String> colsByName = new HashMap<>();
    Iterator<PSJdbcColumnData> cols = row.getColumns();
    while (cols.hasNext()) {
      PSJdbcColumnData col = cols.next();
      colsByName.put(col.getName(), col.getValue());
    }

    assertEquals("42", colsByName.get("ARCHIVE_LOG_ID"));
    assertNotNull(colsByName.get("INSTALL_DATE"), "INSTALL_DATE must be populated");
    assertEquals("archive-ref-1", colsByName.get("ARCHIVE_REF"));
    assertEquals("alice", colsByName.get("USER_ID"));
    assertEquals("rhythmx-source", colsByName.get("SRC_SERVER_NAME"));
    assertEquals("8.2.0", colsByName.get("SRC_SERVER_VERSION"));
    assertEquals("2026.07.21", colsByName.get("SRC_SERVER_BUILD_ID"));
    assertEquals("rhythmx-target:9992", colsByName.get("TGT_SERVER_NAME"));
    assertEquals("2026-07-21 15:30:45", colsByName.get("SRC_SERVER_BUILD_DATE"));
    assertNotNull(colsByName.get("ARCHIVE_INFO"));
    assertNotNull(colsByName.get("ARCHIVE_MANIFEST"));
  }

  @Test
  void testBuildArchiveLogSummaryTableDataWritesJdbcDateFormat() throws Exception {
    PSArchiveInfo archiveInfo = mock(PSArchiveInfo.class);
    PSArchiveManifest archiveManifest = mock(PSArchiveManifest.class);

    when(archiveInfo.getArchiveRef()).thenReturn("r");
    when(archiveInfo.getUserName()).thenReturn("u");
    when(archiveInfo.getServerName()).thenReturn("n");
    when(archiveInfo.getServerVersion()).thenReturn("v");
    when(archiveInfo.getServerBuildId()).thenReturn("b");
    when(archiveInfo.getServerBuildDate())
        .thenReturn(new GregorianCalendar(2026, Calendar.JANUARY, 2, 3, 4, 5).getTime());
    when(archiveManifest.toXml(any(Document.class)))
        .thenAnswer(inv -> rootElement(inv.getArgument(0)));

    PSJdbcTableData tableData =
        PSLogHandler.buildArchiveLogSummaryTableData(1, "h:1", archiveInfo, archiveManifest);

    assertNotNull(tableData);
    Iterator<PSJdbcRowData> rows = tableData.getRows();
    PSJdbcRowData row = rows.next();
    Iterator<PSJdbcColumnData> cols = row.getColumns();
    String buildDateValue = null;
    while (cols.hasNext()) {
      PSJdbcColumnData col = cols.next();
      if ("SRC_SERVER_BUILD_DATE".equalsIgnoreCase(col.getName())) {
        buildDateValue = col.getValue();
        break;
      }
    }
    assertNotNull(buildDateValue);
    assertEquals("2026-01-02 03:04:05", buildDateValue);
  }

  private static Element rootElement(Document doc) {
    Element el = PSXmlDocumentBuilder.createRoot(doc, "root");
    PSXmlDocumentBuilder.addElement(doc, el, "child", "value");
    return el;
  }
}
