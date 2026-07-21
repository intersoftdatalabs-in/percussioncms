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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.deployer.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.percussion.deployer.objectstore.PSArchiveInfo;
import com.percussion.deployer.objectstore.PSArchiveManifest;
import com.percussion.tablefactory.PSJdbcColumnData;
import com.percussion.tablefactory.PSJdbcRowData;
import com.percussion.tablefactory.PSJdbcTableData;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import org.junit.jupiter.api.Test;

class PSLogHandlerTest {

  @Test
  void testCreateArchiveLogSummaryTableDataDateFormat() throws Exception {
    PSLogHandler logHandler = new PSLogHandler();
    PSArchiveInfo archiveInfo = mock(PSArchiveInfo.class);
    PSArchiveManifest archiveManifest = mock(PSArchiveManifest.class);

    Calendar cal = new GregorianCalendar(2026, Calendar.JULY, 21, 15, 30, 45);
    Date testDate = cal.getTime();
    when(archiveInfo.getServerBuildDate()).thenReturn(testDate);

    PSJdbcTableData tableData = logHandler.createArchiveLogSummaryTableData(1, archiveInfo, archiveManifest);
    assertNotNull(tableData);

    Iterator<PSJdbcRowData> rows = tableData.getRows();
    PSJdbcRowData row = rows.next();
    Iterator<PSJdbcColumnData> cols = row.getColumns();

    PSJdbcColumnData buildDateCol = null;
    while (cols.hasNext()) {
      PSJdbcColumnData col = cols.next();
      if ("ALS_SRC_SERVER_BUILD_DATE".equalsIgnoreCase(col.getName())) {
        buildDateCol = col;
        break;
      }
    }

    assertNotNull(buildDateCol, "ALS_SRC_SERVER_BUILD_DATE column must be present");
    assertEquals("2026-07-21 15:30:45", buildDateCol.getValue());
  }
}
