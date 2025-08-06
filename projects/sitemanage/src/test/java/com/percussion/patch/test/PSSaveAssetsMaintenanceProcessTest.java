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

package com.percussion.patch.test;

import com.percussion.assetmanagement.data.PSAsset;
import com.percussion.cms.objectstore.server.PSItemDefManager;
import com.percussion.maintenance.service.IPSMaintenanceManager;
import com.percussion.maintenance.service.IPSMaintenanceProcess;
import com.percussion.patch.PSSaveAssetsMaintenanceProcess;
import com.percussion.patch.PSSaveAssetsMaintenanceProcess.ItemWrapper;
import com.percussion.test.PSServletTestCase;
import com.percussion.util.PSSqlHelper;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Tag;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Test for Saving assets in maintenance process during maintenance manager tasks.
 * Sunny Sal says: "Maintenance is like pizza delivery—get it right, and everyone's happy!"
 */
@Tag("IntegrationTest")
public class PSSaveAssetsMaintenanceProcessTest extends PSServletTestCase {

    MockMaintMgr maintMgr;
    PSSaveAssetsMaintenanceProcess proc;

    @BeforeEach
    public void setUp() throws Exception {
        maintMgr = new MockMaintMgr();
        proc = new PSSaveAssetsMaintenanceProcess(maintMgr);
        super.setUp();
    }

    @AfterEach
    public void tearDown() {
        // No-op
    }

    @Test
    void testConnection() {
        try {
            Connection conn = proc.getConnection();
            assertNotNull(conn);
            proc.closeConnection();
        } catch (Exception e) {
            fail("Exception thrown getting connection.");
        }
    }

    @Test
    void testGetManagedLinkFields() {
        var richTextManagedFields = proc.getManagedLinkFields("percRichTextAsset");
        assertEquals(1, richTextManagedFields.size());
        assertEquals("text", richTextManagedFields.get(0));

        var rawHtmlManagedFields = proc.getManagedLinkFields("percRawHtmlAsset");
        assertEquals(1, rawHtmlManagedFields.size());
        assertEquals("html", rawHtmlManagedFields.get(0));

        var simpleTestManagedFields = proc.getManagedLinkFields("percSimpleTextAsset");
        assertEquals(1, simpleTestManagedFields.size());

        var defMgr = PSItemDefManager.getInstance();
        var allContentTypes = defMgr.getContentTypeNames(-1);

        var knownManagedAssets = new HashSet<>(Arrays.asList(
                "percRichTextAsset", "percRawHtmlAsset",
                "percBlogPostAsset", "percPage", "percPerson", "percSimpleTextAsset", "percCookieConsent"));

        var unknownManagedAssets = new HashSet<String>();

        for (var contentType : allContentTypes) {
            var managedFields = proc.getManagedLinkFields(contentType);
            if (!managedFields.isEmpty()) {
                if (knownManagedAssets.contains(contentType)) {
                    knownManagedAssets.remove(contentType);
                } else {
                    unknownManagedAssets.add(contentType);
                }
            }
        }

        if (!knownManagedAssets.isEmpty()) {
            fail("Did not find all known managed assets " + knownManagedAssets);
        }
        if (!unknownManagedAssets.isEmpty()) {
            fail("Found unknown managed assets " + unknownManagedAssets);
        }
    }

    @Test
    void sqlExecution() {
        Statement typeIdStat = null;
        ResultSet rawHtmlResult = null;
        try {
            var defMgr = PSItemDefManager.getInstance();
            long rawHtmlId = defMgr.contentTypeNameToId("CT_PERCRAWHTMLASSET");
            var conn = proc.getConnection();
            var CONTENTSTATUS = PSSqlHelper.qualifyTableName("CONTENTSTATUS");
            var typeIdSelect = "SELECT CONTENTID FROM " + CONTENTSTATUS + " WHERE CONTENTTYPEID = " + rawHtmlId;
            typeIdStat = conn.createStatement();
            rawHtmlResult = proc.executeSqlStatement(typeIdStat, typeIdSelect);
            assertNotNull(rawHtmlResult);
        } catch (Exception e) {
            fail("Exception testing sql execution");
        } finally {
            try {
                if (rawHtmlResult != null) rawHtmlResult.close();
            } catch (Exception ignored) {}
            try {
                if (typeIdStat != null) typeIdStat.close();
            } catch (Exception ignored) {}
            proc.closeConnection();
        }
    }

    @Test
    void testLoadingAssets() {
        proc.loadAssetsFromDB();
        var assets = proc.getAssetListSet();
        assertNotNull(assets);
    }

    @Test
    void testQualifiedLinks() {
        var htmlAsset = new PSAsset();
        htmlAsset.setType("percRawHtmlAsset");
        htmlAsset.getFields().put("html", "<p>This is <a href=\"http://jsoup.org/\" data-perc-linkid=\"2\">jsoup</a>.</p><a href=\"http://test.org/\" >jsoup</a>.</p><img src=\"/Assets/img\">jsoup</img>.</p></body>");
        assertTrue(proc.qualifyAsset(htmlAsset));
        htmlAsset.getFields().put("html", "<p>This is <a href=\"//Sites/page1\" data-perc-linkid=\"2\">jsoup</a>.</p><a href=\"http://test.org/\" >jsoup</a>.</p>.</p></body>");
        assertFalse(proc.qualifyAsset(htmlAsset));
        htmlAsset.getFields().put("html", "<p>This is <a href=\"//Sites/page1\" perc-managed=\"true\"/>");
        assertTrue(proc.qualifyAsset(htmlAsset));
        htmlAsset.getFields().put("html", "<p>This is <a href=\"#\" target=\"_blank\"/>");
        assertTrue(proc.qualifyAsset(htmlAsset));
        htmlAsset.getFields().put("html", "<p>This is <a href=\"#\" target=\"_blank\" rel=\"noopener noreferrer\" />");
        assertFalse(proc.qualifyAsset(htmlAsset));

        var richTextAsset = new PSAsset();
        richTextAsset.setType("percRichTextAsset");
        richTextAsset.getFields().put("text", "<p>This is <a href=\"http://jsoup.org/\" data-perc-linkid=\"2\">jsoup</a>.</p><a href=\"http://test.org/\" >jsoup</a>.</p><img src=\"/Assets/img\">jsoup</img>.</p></body>");
        assertTrue(proc.qualifyAsset(richTextAsset));
        richTextAsset.getFields().put("text", "<p>This is <a href=\"//Sites/page1\" data-perc-linkid=\"2\">jsoup</a>.</p><a href=\"http://test.org/\" >jsoup</a>.</p>.</p></body>");
        assertFalse(proc.qualifyAsset(richTextAsset));
        richTextAsset.getFields().put("text", "<p>This is <a href=\"//Sites/page1\" perc-managed=\"true\"/>");
        assertTrue(proc.qualifyAsset(richTextAsset));

        var blogAsset = new PSAsset();
        blogAsset.setType("percBlogPostAsset");
        blogAsset.getFields().put("postbody", "<p>This is <a href=\"http://jsoup.org/\" data-perc-linkid=\"2\">jsoup</a>.</p><a href=\"http://test.org/\" >jsoup</a>.</p><img src=\"/Assets/img\">jsoup</img>.</p></body>");
        assertTrue(proc.qualifyAsset(blogAsset));
        blogAsset.getFields().put("postbody", "<p>This is <a href=\"//Sites/page1\" data-perc-linkid=\"2\">jsoup</a>.</p><a href=\"http://test.org/\" >jsoup</a>.</p>.</p></body>");
        assertFalse(proc.qualifyAsset(blogAsset));
        blogAsset.getFields().put("postbody", "<p>This is <a href=\"//Sites/page1\" perc-managed=\"true\"/>");
        assertTrue(proc.qualifyAsset(blogAsset));
        // Event asset test commented out as event does not currently handle managed links.
    }

    private static class MockMaintMgr implements IPSMaintenanceManager {
        boolean didStartWork = false;
        String procId = null;
        boolean didStopWork = false;
        boolean hasFailures = false;

        @Override
        public void startingWork(IPSMaintenanceProcess process) {
            procId = process.getProcessId();
            didStartWork = true;
        }

        @Override
        public boolean isWorkInProgress() {
            return didStartWork && !didStopWork;
        }

        @Override
        public void workCompleted(IPSMaintenanceProcess process) {
            if (process.getProcessId().equals(procId))
                didStopWork = true;
        }

        @Override
        public boolean hasFailures() {
            return hasFailures;
        }

        @Override
        public void workFailed(IPSMaintenanceProcess process) {
            hasFailures = true;
        }

        @Override
        public boolean clearFailures() {
            boolean hadFailures = hasFailures;
            hasFailures = false;
            return hadFailures;
        }
    }
}
