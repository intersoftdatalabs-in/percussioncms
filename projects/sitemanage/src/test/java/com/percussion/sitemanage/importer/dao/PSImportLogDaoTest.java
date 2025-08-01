// REFACTORED: CP-JAVA11
/*
 * Copyright 1999-2023 Percussion Software, Inc.
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
package com.percussion.sitemanage.importer.dao;

import com.percussion.error.PSExceptionUtils;
import com.percussion.share.dao.IPSGenericDao;
import com.percussion.share.spring.PSSpringWebApplicationContextUtils;
import com.percussion.sitemanage.importer.data.PSImportLogEntry;
import com.percussion.utils.testing.IntegrationTest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link IPSImportLogDao}.
 */
@Tag("IntegrationTest")
public class PSImportLogDaoTest {

    private static final Logger log = LogManager.getLogger(PSImportLogDaoTest.class);

    private IPSImportLogDao dao;

    @BeforeEach
    public void setUp() {
        PSSpringWebApplicationContextUtils.injectDependencies(this);
    }

    @Test
    public void testDao() {
        assertNotNull(dao);

        var testSiteId1 = "1";
        var testSiteId2 = "2";
        var testTemplateId = "1";
        var allEntries = new ArrayList<PSImportLogEntry>();

        try {
            var typeSite = "site";
            var typeTemplate = "template";
            var entries = dao.findAll(testSiteId1, typeSite);
            allEntries.addAll(entries);
            assertEquals(0, entries.size());

            var logData = "log data line 1\n log data line 2\n log data line 3";
            var now = new Date();
            var entry1 = new PSImportLogEntry(testSiteId1, typeSite, now, logData);
            dao.save(entry1);
            allEntries.add(entry1);

            entries = dao.findAll(testSiteId1, typeSite);
            assertEquals(1, entries.size());
            assertEquals(entry1.getObjectId(), entries.get(0).getObjectId());
            assertEquals(entry1.getType(), entries.get(0).getType());
            assertEquals(entry1.getLogData(), entries.get(0).getLogData());

            dao.delete(entry1);
            entries = dao.findAll(testSiteId1, typeSite);
            assertEquals(0, entries.size());
            allEntries.clear();

            var entry2 = new PSImportLogEntry(testSiteId2, typeSite, now, logData);
            dao.save(entry2);
            allEntries.add(entry2);

            var entry3 = new PSImportLogEntry(testTemplateId, typeTemplate, now, logData);
            dao.save(entry3);
            allEntries.add(entry3);

            entries = dao.findAll(testSiteId1, typeSite);
            assertEquals(0, entries.size());
            dao.save(entry1);
            allEntries.add(entry1);

            entries = dao.findAll(testSiteId1, typeSite);
            assertEquals(1, entries.size());
            assertEquals(entry1.getObjectId(), entries.get(0).getObjectId());
            assertEquals(entry1.getType(), entries.get(0).getType());
            assertEquals(entry1.getLogData(), entries.get(0).getLogData());
            assertNotEquals(entry2.getObjectId(), entries.get(0).getObjectId());

            entries = dao.findAll(testSiteId2, typeSite);
            assertEquals(1, entries.size());
            assertEquals(entry2.getObjectId(), entries.get(0).getObjectId());
            assertEquals(entry2.getType(), entries.get(0).getType());
            assertEquals(entry2.getLogData(), entries.get(0).getLogData());
            assertNotEquals(entry3.getObjectId(), entries.get(0).getObjectId());

            entries = dao.findAll(testTemplateId, typeTemplate);
            assertEquals(1, entries.size());
            assertEquals(entry3.getObjectId(), entries.get(0).getObjectId());
            assertEquals(entry3.getType(), entries.get(0).getType());
            assertEquals(entry3.getLogData(), entries.get(0).getLogData());
            assertNotEquals(entry2.getObjectId(), entries.get(0).getObjectId());

            var objectIds = new ArrayList<String>();
            objectIds.add(testSiteId1);
            objectIds.add(testSiteId2);
            var entryIds = dao.findLogIdsForObjects(objectIds, typeSite);
            assertNotNull(entryIds);
            assertEquals(objectIds.size(), entryIds.size());
            assertEquals(entry1.getLogEntryId(), entryIds.get(0).longValue());
            assertEquals(entry2.getLogEntryId(), entryIds.get(1).longValue());

            var found = dao.findLogEntryById(entry1.getLogEntryId());
            assertNotNull(found);
            assertEquals(entry1.getLogEntryId(), found.getLogEntryId());
        } catch (IPSGenericDao.SaveException e) {
            log.error(PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
        } finally {
            // clean up
            if (allEntries != null && !allEntries.isEmpty()) {
                for (var entry : allEntries) {
                    try {
                        dao.delete(entry);
                    } catch (Exception e) {
                        // noop
                    }
                }
            }
        }
    }

    public void setDao(IPSImportLogDao dao) {
        this.dao = dao;
    }
}
