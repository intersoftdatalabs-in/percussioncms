/*
 * Copyright 1999-2025 Percussion Software, Inc.
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
// REFACTORED: CP-JAVA11
package com.percussion.sitemanage.importer;

import static org.junit.jupiter.api.Assertions.*;

import com.percussion.sitemanage.importer.IPSSiteImportLogger.PSLogEntryType;
import com.percussion.sitemanage.importer.IPSSiteImportLogger.PSLogObjectType;
import com.percussion.sitemanage.importer.data.PSImportLogEntry;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PSSiteImportLogger}.
 *
 * @author Jay Seletz, Sunny Sal (refactored)
 */
@Tag("IntegrationTest")
class PSSiteImportLoggerTest {

  @Test
  void testImportLogErrors() {
    var siteId = "1234";
    var desc = "//sites/mysite/index";
    var logger = new PSSiteImportLogger(PSLogObjectType.TEMPLATE);

    assertNull(logger.getErrors(PSLogObjectType.SITE_ERROR, siteId, desc));

    var category = "cat";
    logger.appendLogMessage(PSLogEntryType.ERROR, category, "error1");
    assertNull(logger.getErrors(PSLogObjectType.SITE_ERROR, siteId, desc));

    logger.logErrors();
    assertNotNull(logger.getErrors(PSLogObjectType.SITE_ERROR, siteId, desc));
    var log = logger.getLog();
    assertTrue(StringUtils.isNotEmpty(log));
    assertEquals(log, logger.getLog());

    assertTrue(logger.getErrors(PSLogObjectType.SITE_ERROR, siteId, desc).isEmpty());
    var errorString = "error";
    var i = 0;
    logger.appendLogMessage(PSLogEntryType.ERROR, category, errorString + i++);
    assertNotNull(logger.getErrors(PSLogObjectType.SITE_ERROR, siteId, desc));
    assertFalse(logger.getErrors(PSLogObjectType.SITE_ERROR, siteId, desc).isEmpty());
    var log2 = logger.getLog();
    assertNotEquals(log, log2);

    logger.appendLogMessage(PSLogEntryType.ERROR, category, errorString + i++);
    assertNotNull(logger.getErrors(PSLogObjectType.SITE_ERROR, siteId, desc));
    assertFalse(logger.getErrors(PSLogObjectType.SITE_ERROR, siteId, desc).isEmpty());
    assertNotEquals(log2, logger.getLog());
    assertEquals(2, logger.getErrors(PSLogObjectType.SITE_ERROR, siteId, desc).size());

    List<PSImportLogEntry> entries = logger.getErrors(PSLogObjectType.SITE_ERROR, siteId, desc);
    for (int j = 0; j < entries.size(); j++) {
      var entry = entries.get(j);
      assertEquals(siteId, entry.getObjectId());
      assertEquals(PSLogObjectType.SITE_ERROR.name(), entry.getType());
      assertEquals(category, entry.getCategory());
      assertEquals(desc, entry.getDescription());
      assertEquals(errorString + j, entry.getLogData());
    }
  }
}
