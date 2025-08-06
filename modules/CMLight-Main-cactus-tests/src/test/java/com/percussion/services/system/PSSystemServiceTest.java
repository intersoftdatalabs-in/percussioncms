// REFACTORED: CP-JAVA11
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
package com.percussion.services.system;

import com.percussion.data.PSIdGenerator;
import com.percussion.error.PSExceptionUtils;
import com.percussion.services.guidmgr.data.PSLegacyGuid;
import com.percussion.services.system.data.PSConfigurationTypes;
import com.percussion.services.system.data.PSContentStatusHistory;
import com.percussion.services.system.data.PSMimeContentAdapter;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.testing.IntegrationTest;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test case for the {@link IPSSystemService}
 */
@Tag("IntegrationTest")
public class PSSystemServiceTest {

    private static final Logger log = LogManager.getLogger(PSSystemServiceTest.class);

    List<PSContentStatusHistory> m_insertedHistories;

    @BeforeEach
    void setUp() {
        try {
            m_insertedHistories = new ArrayList<>();
        } catch (Exception e) {
            log.error(PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
        }
    }

    @AfterEach
    void tearDown() {
        var svc = PSSystemServiceLocator.getSystemService();
        try {
            for (var h : m_insertedHistories) {
                try {
                    svc.deleteContentStatusHistory(h);
                } catch (Exception e) {
                    log.error(PSExceptionUtils.getMessageForLog(e));
                    log.debug(PSExceptionUtils.getDebugMessageForLog(e));
                }
            }
        } catch (Exception e) {
            log.error(PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
        }
    }

    @Test
    void testLoadConfiguration() throws Exception {
        var svc = PSSystemServiceLocator.getSystemService();
        var data = "some test content...".getBytes();

        for (var type : PSConfigurationTypes.values()) {
            var file = svc.getConfigurationFile(type);
            assertTrue(file.exists());
            var backup = backupFile(file);
            try {
                PSMimeContentAdapter content;
                content = svc.loadConfiguration(type);
                validateContent(file, content);
                content.setContent(new ByteArrayInputStream(data));
                content.setContentLength(data.length);
                svc.saveConfiguration(content);
                validateContent(file, data);
                content = svc.loadConfiguration(type);
                validateContent(file, content);
            } finally {
                restoreBackup(backup, file);
            }
        }
    }

    @Test
    void testLoadContentStatusHistory() {
        int contentId = 471;
        IPSGuid id = new PSLegacyGuid(contentId, 1);
        var svc = PSSystemServiceLocator.getSystemService();
        var histList = svc.findContentStatusHistory(id);
        assertFalse(histList.isEmpty());

        var lastHist = svc.findLastCheckInOut(id);
        assertNotNull(lastHist);

        // find the last check in/out from "histList"
        PSContentStatusHistory lastCheckInOut = null;
        for (var hist : histList) {
            if ("CheckOut".equalsIgnoreCase(hist.getTransitionLabel())) {
                if (lastCheckInOut == null || lastCheckInOut.getId() < hist.getId()) {
                    lastCheckInOut = hist;
                }
            }
        }
        assertEquals(lastHist.getId(), lastCheckInOut.getId());
    }

    private void createLargeHistory() throws Exception {
        int itemCount = 6000;
        String[] states = {"Draft", "Review", "Pending", "Live", "Quick Edit", "Pending", "Live", "Quick Edit", "Archive"};
        for (int i = 0; i < itemCount; i++) {
            int contentId = PSIdGenerator.getNextId("CONTENT");
            int revision;
            for (int j = 0; j < states.length; j++) {
                if (j < 4)
                    revision = 1;
                else if (j < 7)
                    revision = 2;
                else
                    revision = 3;

                createNewContentStatusHistory(contentId, new Date(), states[j], j + 1, "Unit-test", revision);
            }
        }
    }

    private PSContentStatusHistory createNewContentStatusHistory(int contentId,
                                                                Date date, String stateName, int stateId, String transitionName, int revision)
            throws CloneNotSupportedException {
        var svc = PSSystemServiceLocator.getSystemService();
        IPSGuid id = new PSLegacyGuid(contentId, 1);
        var entries = svc.findContentStatusHistory(id);
        var hist = new PSContentStatusHistory();
        hist.setId(-1L);
        hist.setActor("Admin");
        hist.setContentId(contentId);
        hist.setEventTime(date);
        hist.setIsValidValue("N");
        hist.setLastModifiedDate(date);
        hist.setLastModifierName("Admin");
        hist.setRevision(revision);
        hist.setRoleName("Admin");
        hist.setSessionId("0");
        hist.setStateId(stateId);
        hist.setStateName(stateName);
        hist.setTitle("test");
        hist.setTransitionId(0);
        hist.setWorkflowId(5);
        hist.setTransitionLabel("test");

        svc.saveContentStatusHistory(hist);

        return hist;
    }

    private PSContentStatusHistory createContentStatusHistory(int contentId,
                                                             Date date, String stateName, int stateId, String transitionName)
            throws CloneNotSupportedException {
        var svc = PSSystemServiceLocator.getSystemService();
        IPSGuid id = new PSLegacyGuid(contentId, 1);
        var entries = svc.findContentStatusHistory(id);
        var cloned = entries.get(entries.size() - 1).clone();

        cloned.setId(-1L);
        cloned.setEventTime(date);
        cloned.setLastModifiedDate(date);
        cloned.setStateName(stateName);
        cloned.setStateId(stateId);
        if (transitionName != null)
            cloned.setTransitionLabel(transitionName);

        svc.saveContentStatusHistory(cloned);
        m_insertedHistories.add(cloned);

        return cloned;
    }

    @Test
    void testContentActivities() throws Exception {
        var svc = PSSystemServiceLocator.getSystemService();

        var contentIds = Collections.singletonList(1);
        int count = svc.findNumberContentActivities(contentIds, new Date(), new Date(), "Live", "Live");
        assertEquals(0, count);

        count = svc.findNewContentActivities(contentIds, new Date(), new Date(), "Live");
        assertEquals(0, count);

        count = svc.findPublishedItems(contentIds, new Date(), new Date(), "Live", "Archive");
        assertEquals(0, count);
    }

    @Test
    void testFindPublishedItemsWithDates() throws Exception {
        var svc = PSSystemServiceLocator.getSystemService();

        var contentIds = Collections.singletonList(375);

        // No content are created between 2006-3-24 00:00:00 and 2007-3-24 00:00:00
        Date beginDate = getDate(2006, 3, 24, 0, 0, 0);
        Date endDate = getDate(2007, 3, 24, 0, 0, 0);
        int count = svc.findPublishedItems(contentIds, beginDate, endDate, "Public", "Archive");
        assertEquals(0, count);

        // No content are transitioned to "Public" on 2008-3-24 00:00:00, but not include 2008-3-24 00:00:00
        endDate = getDate(2008, 3, 24, 0, 0, 0);
        count = svc.findPublishedItems(contentIds, beginDate, endDate, "Public", "Archive");
        assertEquals(0, count);

        // Items are transitioned to "Public" on 2008-3-24 00:00:00
        beginDate = getDate(2008, 3, 24, 0, 0, 0);
        count = svc.findPublishedItems(contentIds, beginDate, new Date(), "Public", "Archive");
        assertEquals(1, count);

        // Items are transitioned to "Public" before now and never transitioned to "Archive"
        count = svc.findPublishedItems(contentIds, new Date(), new Date(), "Public", "Archive");
        assertEquals(1, count);

        // After transition to public on 2008-3-24 00:00:00, then transition to "Archive" on 2008-3-24 00:00:01
        Date archiveDate = getDate(2008, 3, 24, 0, 0, 1);
        createContentStatusHistory(375, archiveDate, "Archive", 7, "Take Down");

        endDate = getDate(2008, 3, 24, 0, 0, 2);
        count = svc.findPublishedItems(contentIds, beginDate, endDate, "Public", "Archive");
        assertEquals(0, count);

        // After transition to public on 2008-3-24 00:00:00, transition to "Archive" on 2008-3-24 00:00:01
        // then transition to public again on
        endDate = getDate(2008, 4, 4, 0, 0, 1);
        count = svc.findPublishedItems(contentIds, beginDate, endDate, "Public", "Archive");
        assertEquals(1, count);
    }

    @Test
    void testFindPublishedItems() throws Exception {
        var svc = PSSystemServiceLocator.getSystemService();

        var contentIds = Collections.singletonList(375);

        // Items are transitioned to "Public" on 2008-4-6 00:00:00
        Collection<Long> items = svc.findPublishedItems(contentIds, "Public", "Archive");
        assertEquals(1, items.size());

        // After transition to public on 2008-4-6 00:00:00, then transition to "Archive" on 2008-4-6 00:00:01
        Date archiveDate = getDate(2008, 4, 6, 0, 0, 1);
        createContentStatusHistory(375, archiveDate, "Archive", 7, "Take Down");

        // Items are no longer published
        items = svc.findPublishedItems(contentIds, "Public", "Archive");
        assertEquals(0, items.size());
    }

    private Date getDate(int year, int month, int date, int hour, int minute, int second) {
        var cal = Calendar.getInstance();
        cal.clear();
        cal.set(year, month - 1, date, hour, minute, second);
        return cal.getTime();
    }

    @Test
    void testFindNewContentActivities() throws Exception {
        var svc = PSSystemServiceLocator.getSystemService();

        // Not all "Public" transitions are done right after 2008-3-24 00:00:01
        Date beginDate = getDate(2008, 3, 24, 0, 0, 1);
        var contentIds = Collections.singletonList(375);
        int count = svc.findNewContentActivities(contentIds, beginDate, new Date(), "Public");
        assertEquals(0, count);

        // The items are created before 2008-3-24 00:00:00, but NOT INCLUDE 2008-3-24 00:00:00
        beginDate = getDate(2006, 3, 24, 0, 0, 0);
        Date endDate = getDate(2008, 3, 24, 0, 0, 0);

        count = svc.findNewContentActivities(contentIds, beginDate, endDate, "Public");
        assertEquals(0, count);

        // The items are created and all transitions are done after 2006-3-24 00:00:00
        beginDate = getDate(2006, 3, 24, 0, 0, 0);
        count = svc.findNewContentActivities(contentIds, beginDate, new Date(), "Public");
        assertEquals(1, count);

        // All "Public" transitions are done right after 2008-3-24 00:00:00
        beginDate = getDate(2008, 3, 24, 0, 0, 0);
        count = svc.findNewContentActivities(contentIds, beginDate, new Date(), "Public");
        assertEquals(1, count);
    }

    @Test
    void testFindNumberContentActivities() throws Exception {
        var svc = PSSystemServiceLocator.getSystemService();

        var contentIds = Collections.singletonList(375);
        var cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -2);
        Date beginDate = cal.getTime();
        int origCount = svc.findNumberContentActivities(contentIds, beginDate, new Date(), "Public", null);

        var cal2 = Calendar.getInstance();
        cal2.add(Calendar.DATE, -1);
        Date date1Ago = cal2.getTime();

        createContentStatusHistory(375, date1Ago, "Public", 5, "Return to Public");

        int count = svc.findNumberContentActivities(contentIds, beginDate, new Date(), "Public", null);

        assertEquals(origCount + 1, count);
    }

    @Test
    void testContentStatusHistoryCRUD() throws Exception {
        var svc = PSSystemServiceLocator.getSystemService();

        IPSGuid id = new PSLegacyGuid(375, 3);
        var entries = svc.findContentStatusHistory(id);
        var cloned = entries.get(entries.size() - 1).clone();
        cloned.setId(-1L);
        assertEquals(-1L, cloned.getId());

        // add a new row
        svc.saveContentStatusHistory(cloned);
        assertNotEquals(-1L, cloned.getId());
        var entries2 = svc.findContentStatusHistory(id);
        assertEquals(entries.size() + 1, entries2.size());

        svc.deleteContentStatusHistory(cloned);
        entries2 = svc.findContentStatusHistory(id);
        assertEquals(entries.size(), entries2.size());
    }

    /**
     * Validates that the supplied file matches the loaded content.
     */
    private void validateContent(File file, PSMimeContentAdapter content)
            throws IOException {
        // copy content
        var out = new ByteArrayOutputStream();
        IOUtils.copy(content.getContent(), out);
        var data = out.toByteArray();
        assertEquals(content.getContentLength(), data.length);
        assertEquals(content.getContentLength(), file.length());
        validateContent(file, data);
    }

    /**
     * Validates the contents of the file and the supplied data are equal.
     */
    private void validateContent(File file, byte[] data) throws IOException {
        try (var in = new FileInputStream(file)) {
            assertTrue(IOUtils.contentEquals(in, new ByteArrayInputStream(data)));
        }
    }

    /**
     * Copies the source file to a backup temp file, which is set for deleteOnExit.
     */
    private File backupFile(File file) throws IOException {
        var backup = File.createTempFile("cfgTest_", ".bak");
        backup.deleteOnExit();
        FileUtils.copyFile(file, backup);

        return backup;
    }

    /**
     * Copies the backup file over the original file and deletes the backup.
     */
    private void restoreBackup(File backup, File file) throws IOException {
        FileUtils.copyFile(backup, file);
        backup.delete();
    }
}
