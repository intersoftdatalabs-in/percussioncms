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
package com.percussion.utils.service;

import static com.percussion.utils.service.impl.PSSiteConfigUtils.*;
import static org.apache.commons.io.FileUtils.forceDelete;
import static org.apache.commons.io.FileUtils.listFiles;
import static org.apache.commons.io.FileUtils.touch;
import static org.junit.jupiter.api.Assertions.*;

import com.percussion.sitemanage.data.PSSectionNode;
import com.percussion.utils.service.impl.PSSiteConfigUtils;
import com.percussion.utils.service.impl.PSSiteConfigUtils.SecureXmlData;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Tag;

/**
 * Test cases for the {@link PSSiteConfigUtils} class.
 * Refactored for Java 11 and JUnit5.
 */
@Tag("IntegrationTest")
public class PSSiteConfigUtilsTest {

    private File secureSiteDefaultConfigFolder;
    private File nonsecureSiteDefaultConfigFolder;
    private File tchFile1;
    private File tchFile2;
    private File siteConfig1;
    private File siteConfig2;
    private File mockFile;
    private String sitename;
    private final long server1 = 100;
    private final long server2 = 200;

    @BeforeEach
    public void setUp() throws Exception {
        if (secureSiteDefaultConfigFolder == null) {
            secureSiteDefaultConfigFolder = getSourceConfigurationFolder();
        }
        if (nonsecureSiteDefaultConfigFolder == null) {
            nonsecureSiteDefaultConfigFolder = getNonSecureConfigurationFolder();
        }
        sitename = "testSite" + System.currentTimeMillis();
    }

    @AfterEach
    public void tearDown() throws Exception {
        cleanCreatedFiles();
    }

    private void cleanCreatedFiles() throws IOException {
        if (tchFile1 != null && tchFile1.exists()) {
            forceDelete(tchFile1);
        }
        if (tchFile2 != null && tchFile2.exists()) {
            forceDelete(tchFile2);
        }
        if (siteConfig1 != null && siteConfig1.exists()) {
            forceDelete(siteConfig1);
        }
        if (siteConfig2 != null && siteConfig2.exists()) {
            forceDelete(siteConfig2);
        }
        if (mockFile != null && mockFile.exists()) {
            forceDelete(mockFile);
        }
    }

    @Test
    public void testCreateSiteConfiguration() throws IOException {
        createSecureSiteConfiguration(sitename);

        siteConfig1 = new File(getSitesConfigPath(), sitename);
        assertTrue(siteConfig1.exists());

        var defaultConfigFiles = listFiles(secureSiteDefaultConfigFolder, null, true);
        var siteConfigFiles = listFiles(siteConfig1, null, true);

        assertEquals(defaultConfigFiles.size(), siteConfigFiles.size(), "Different collection sizes.");
        assertTrue(sameFilesInCollection(defaultConfigFiles, siteConfigFiles), "Different collection elements");
    }

    @Test
    public void testRemoveSiteConfiguration_nonExistingFolder() {
        var siteConfig = new File(getSitesConfigPath(), sitename);
        assertFalse(siteConfig.exists());

        try {
            removeSiteConfiguration(sitename);
            assertFalse(siteConfig.exists());
        } catch (Exception e) {
            fail("No exception should have been thrown.");
        }
    }

    @Test
    public void testRemoveSiteConfiguration() throws IOException {
        createSecureSiteConfiguration(sitename);

        var siteConfig = new File(getSitesConfigPath(), sitename);
        assertTrue(siteConfig.exists());

        removeSiteConfiguration(sitename);
        assertFalse(siteConfig.exists());
    }

    @Test
    public void testCreateTouchedFile() throws IOException {
        createTouchedFile(sitename);

        tchFile1 = getTouchedFile(sitename);
        assertTrue(tchFile1.exists(), "Tch file should exist");
    }

    @Test
    public void testRemoveSiteConfigurationAndTouchedFile_nonExistingConfigFolder() throws IOException {
        createTouchedFile(sitename);

        var siteConfig = new File(getSitesConfigPath(), sitename);
        var tchFile = getTouchedFile(sitename);

        assertFalse(siteConfig.exists());
        assertTrue(tchFile.exists());

        try {
            removeSiteConfigurationAndTouchedFile(sitename);

            assertFalse(siteConfig.exists());
            assertFalse(tchFile.exists());
        } catch (Exception e) {
            fail("No exception should have been thrown.");
        }
    }

    @Test
    public void testRemoveSiteConfigurationAndTouchedFile_nonExistingTouchedFile() throws IOException {
        createSecureSiteConfiguration(sitename);

        var siteConfig = new File(getSitesConfigPath(), sitename);
        var tchFile = getTouchedFile(sitename);

        assertTrue(siteConfig.exists());
        assertFalse(tchFile.exists());

        try {
            removeSiteConfigurationAndTouchedFile(sitename);

            assertFalse(siteConfig.exists());
            assertFalse(tchFile.exists());
        } catch (Exception e) {
            fail("No exception should have been thrown.");
        }
    }

    @Test
    public void testRemoveSiteConfigurationAndTouchedFile_nonExistingTouchedAndConfig() {
        var siteConfig = new File(getSitesConfigPath(), sitename);
        var tchFile = getTouchedFile(sitename);

        assertFalse(siteConfig.exists());
        assertFalse(tchFile.exists());

        try {
            removeSiteConfigurationAndTouchedFile(sitename);

            assertFalse(siteConfig.exists());
            assertFalse(tchFile.exists());
        } catch (Exception e) {
            fail("No exception should have been thrown.");
        }
    }

    @Test
    public void testRemoveSiteConfigurationAndTouchedFile() throws IOException {
        createSecureSiteConfiguration(sitename);
        createTouchedFile(sitename);

        var siteConfig = new File(getSitesConfigPath(), sitename);
        var tchFile = getTouchedFile(sitename);

        assertTrue(siteConfig.exists());
        assertTrue(tchFile.exists());

        removeSiteConfigurationAndTouchedFile(sitename);

        assertFalse(siteConfig.exists());
        assertFalse(tchFile.exists());
    }

    @Test
    public void testRemoveTouchedFile_nonExisting() {
        var tchFile = getTouchedFile(sitename);
        assertFalse(tchFile.exists(), "Tch file should not exist");

        try {
            removeTouchedFile(sitename);
            assertFalse(tchFile.exists());
        } catch (Exception e) {
            fail("No exception should have been thrown.");
        }
    }

    @Test
    public void testRemoveTouchedFile() throws IOException {
        createTouchedFile(sitename);

        var tchFile = getTouchedFile(sitename);
        assertTrue(tchFile.exists(), "Tch file should exist");

        removeTouchedFile(sitename);
        assertFalse(tchFile.exists());
    }

    @Test
    public void testRenameSecureSiteConfiguration_equalNames() throws IOException {
        createSecureSiteConfiguration(sitename);

        siteConfig1 = new File(getSitesConfigPath(), sitename);
        assertTrue(siteConfig1.exists());

        try {
            renameOrCreateSecureSiteConfiguration(sitename, sitename);
            assertTrue(siteConfig1.exists());
        } catch (Exception e) {
            fail("No exception should have been thrown");
        }
    }

    @Test
    public void testRenameSecureSiteConfiguration_differentNamesNoTouchedFile() throws IOException {
        var newSitename = sitename + "renamed";
        createSecureSiteConfiguration(sitename);

        var oldSiteConfig = new File(getSitesConfigPath(), sitename);
        assertTrue(oldSiteConfig.exists());

        try {
            renameOrCreateSecureSiteConfiguration(sitename, newSitename);

            siteConfig1 = new File(getSitesConfigPath(), newSitename);
            assertFalse(oldSiteConfig.exists());
            assertTrue(siteConfig1.exists());
        } catch (Exception e) {
            fail("No exception should have been thrown");
        }
    }

    @Test
    public void testRenameSecureSiteConfiguration_differentNamesWithTouchedFile() throws IOException {
        var newSitename = sitename + "renamed";
        createSecureSiteConfiguration(sitename);
        createTouchedFile(sitename);

        var oldSiteConfig = new File(getSitesConfigPath(), sitename);
        var oldSiteTch = getTouchedFile(sitename);
        assertTrue(oldSiteConfig.exists());
        assertTrue(oldSiteTch.exists());

        try {
            renameOrCreateSecureSiteConfiguration(sitename, newSitename);

            siteConfig1 = new File(getSitesConfigPath(), newSitename);
            var newSiteTch = getTouchedFile(sitename);
            assertFalse(oldSiteConfig.exists());
            assertFalse(oldSiteTch.exists());
            assertFalse(newSiteTch.exists());
            assertTrue(siteConfig1.exists());
        } catch (Exception e) {
            fail("No exception should have been thrown");
        }
    }

    @Test
    public void testRenameNonSecureSiteConfiguration_equalNames() throws IOException {
        createTouchedFile(sitename);

        var siteConfig = new File(getSitesConfigPath(), sitename);
        var oldSiteTch = getTouchedFile(sitename);
        tchFile1 = oldSiteTch;
        assertFalse(siteConfig.exists());
        assertTrue(oldSiteTch.exists());

        try {
            renameNonSecureSiteConfiguration(sitename, sitename);
            assertFalse(siteConfig.exists());
            assertTrue(oldSiteTch.exists());
        } catch (Exception e) {
            fail("No exception should have been thrown");
        }
    }

    @Test
    public void testRenameNonSecureSiteConfiguration_differentNamesNoTouchedFile() throws IOException {
        var newSitename = sitename + "renamed";

        var oldSiteTch = getTouchedFile(sitename);
        assertFalse(oldSiteTch.exists());

        try {
            renameOrCreateSecureSiteConfiguration(sitename, newSitename);

            siteConfig1 = new File(getSitesConfigPath(), newSitename);
            var newSiteTch = getTouchedFile(sitename);
            assertFalse(oldSiteTch.exists());
            assertFalse(newSiteTch.exists());
        } catch (Exception e) {
            fail("No exception should have been thrown");
        }
    }

    @Test
    public void testRenameNonSecureSiteConfiguration_differentNamesWithTouchedFile() throws IOException {
        var newSitename = sitename + "renamed";
        createTouchedFile(sitename);

        var oldSiteConfig = new File(getSitesConfigPath(), sitename);
        var oldSiteTch = getTouchedFile(sitename);
        assertFalse(oldSiteConfig.exists());
        assertTrue(oldSiteTch.exists());

        try {
            renameNonSecureSiteConfiguration(sitename, newSitename);

            var siteConfig = new File(getSitesConfigPath(), newSitename);
            var newSiteTch = getTouchedFile(sitename);
            assertFalse(oldSiteConfig.exists());
            assertFalse(oldSiteTch.exists());
            assertFalse(newSiteTch.exists());
            assertFalse(siteConfig.exists());
        } catch (Exception e) {
            fail("No exception should have been thrown");
        }
    }

    @Test
    public void testFilesModifiedAfterPublished_unsecureNoTouchedFile() {
        try {
            assertTrue(filesModifiedAfterPublished(sitename, server1),
                    "Touched File does not exist, should have returned true");
        } catch (Exception e) {
            fail("No exception should have been thrown");
        }
    }

    @Test
    public void testFilesModifiedAfterPublished_unsecureModifiedForServer1() {
        try {
            createTouchedFile(sitename);
            tchFile1 = getTouchedFile(sitename);

            // add the date for the first server
            var millis = Calendar.getInstance();
            millis.add(Calendar.DAY_OF_MONTH, -1);
            var date = new Date(millis.getTimeInMillis());
            setPublishedDateInTouchedFile(tchFile1.getPath(), server1, date);

            // modify the xml file
            mockFile = new File(getNonSecureConfigurationFolder(), sitename + ".xml");
            touch(mockFile);

            // set the date for the second server
            setPublishedDateInTouchedFile(tchFile1.getPath(), server2, new Date());

            // At this point, server 1 was published before modifications were made.
            assertTrue(filesModifiedAfterPublished(sitename, server1),
                    "Default config modified, should have returned true");
            assertFalse(filesModifiedAfterPublished(sitename, server2),
                    "Default config not modified, should have returned false");
        } catch (Exception e) {
            fail("No exception should have been thrown");
        }
    }

    @Test
    public void testFilesModifiedAfterPublished_unsecureModifiedDefaultConfig() {
        createOnlyTouchedFile();
        try {
            mockFile = new File(getNonSecureConfigurationFolder(), sitename + ".xml");
            touch(mockFile);
            assertTrue(filesModifiedAfterPublished(sitename, server1),
                    "Default config modified, should have returned true");
        } catch (Exception e) {
            fail("No exception should have been thrown");
        }
    }

    @Test
    public void testFilesModifiedAfterPublished_secureModifiedDefaultConfig() {
        createSiteConfigurationAndTouchedFile();
        try {
            mockFile = new File(getSitesConfigPath() + "/" + sitename, sitename + ".xml");
            touch(mockFile);
            assertTrue(filesModifiedAfterPublished(sitename, 0),
                    "Default config modified, should have returned true");
        } catch (Exception e) {
            fail("No exception should have been thrown");
        }
    }

    @Test
    public void testFilesModifiedAfterPublished_secureModifiedForServer1() {
        try {
            createTouchedFile(sitename);
            tchFile1 = getTouchedFile(sitename);

            // add the date for the first server
            var millis = Calendar.getInstance();
            millis.add(Calendar.DAY_OF_MONTH, -1);
            var date = new Date(millis.getTimeInMillis());
            setPublishedDateInTouchedFile(tchFile1.getPath(), server1, date);

            // modify the xml file
            mockFile = new File(getSitesConfigPath() + "/" + sitename, sitename + ".xml");
            touch(mockFile);

            // set the date for the second server
            setPublishedDateInTouchedFile(tchFile1.getPath(), server2, new Date());

            // At this point, server 1 was published before modifications were made.
            assertTrue(filesModifiedAfterPublished(sitename, server1),
                    "Default config modified, should have returned true");
            assertFalse(filesModifiedAfterPublished(sitename, server2),
                    "Default config not modified, should have returned false");
        } catch (Exception e) {
            fail("No exception should have been thrown");
        }
    }

    @Test
    public void testCopySecureSiteConfiguration() {
        try {
            var sitename2 = sitename + "-copy";
            createSiteConfigurationAndTouchedFile();

            mockFile = new File(getSitesConfigPath() + "/" + sitename, sitename + ".xml");
            touch(mockFile);
            copySecureSiteConfiguration(sitename, sitename2);

            siteConfig2 = new File(getSitesConfigPath(), sitename2);
            var tchFile = getTouchedFile(sitename2);

            assertTrue(siteConfig2.exists());
            assertFalse(tchFile.exists());

            var site1ConfigFiles = listFiles(siteConfig1, null, true);
            var site2ConfigFiles = listFiles(siteConfig2, null, true);

            assertEquals(site1ConfigFiles.size(), site2ConfigFiles.size(), "Different collection sizes.");
            assertTrue(sameFilesInCollection(site1ConfigFiles, site2ConfigFiles), "Different collection elements");
        } catch (Exception e) {
            fail("No exception should have been thrown");
        }
    }

    @Test
    public void testGetPublishedDateFromTouchedFile_nonExistingDate() throws IOException {
        createTouchedFile(sitename);

        tchFile1 = getTouchedFile(sitename);
        assertTrue(tchFile1.exists());

        var publishedDate = getPublishedDateFromTouchedFile(tchFile1.getPath(), server1);
        assertNull(publishedDate, "Published date should be null");
    }

    @Test
    public void testGetPublishedDateFromTouchedFile() throws IOException {
        var date = createOnlyTouchedFile();

        var publishedDate = getPublishedDateFromTouchedFile(tchFile1.getPath(), server1);
        assertEquals(date, publishedDate, "Published dates should be the same");
    }

    @Test
    public void testSetPublishedDateInTouchedFile() {
        try {
            createTouchedFile(sitename);

            tchFile1 = getTouchedFile(sitename);
            assertTrue(tchFile1.exists());

            var millis = Calendar.getInstance();
            millis.add(Calendar.DAY_OF_MONTH, -1);
            var date = new Date(millis.getTimeInMillis());
            setPublishedDateInTouchedFile(tchFile1.getPath(), server1, date);

            var publishedDate = getPublishedDateFromTouchedFile(tchFile1.getPath(), server1);
            assertEquals(date, publishedDate, "Published dates should be the same");

            millis.add(Calendar.DAY_OF_MONTH, -2);
            date = new Date(millis.getTimeInMillis());
            setPublishedDateInTouchedFile(tchFile1.getPath(), server2, date);

            publishedDate = getPublishedDateFromTouchedFile(tchFile1.getPath(), server2);
            assertEquals(date, publishedDate, "Published dates should be the same");

        } catch (FileNotFoundException | IOException e) {
            fail("No exception should have been thrown");
        }
    }

    @Test
    public void testRemoveServerEntry_nonExistingTchFile() {
        tchFile1 = getTouchedFile(sitename);

        try {
            assertFalse(tchFile1.exists());
            removeServerEntry(sitename, server1);
            assertFalse(tchFile1.exists());
            removeServerEntry(sitename, server2);
            assertFalse(tchFile1.exists());
        } catch (Exception e) {
            fail("No exception should have been thrown");
        }
    }

    @Test
    public void testRemoveServerEntry_nonExistingServerEntry() {
        try {
            createTouchedFile(sitename);
            tchFile1 = getTouchedFile(sitename);

            // add the date for the first server
            var millis = Calendar.getInstance();
            millis.add(Calendar.DAY_OF_MONTH, -1);
            var date = new Date(millis.getTimeInMillis());
            setPublishedDateInTouchedFile(tchFile1.getPath(), server1, date);

            var dateServer1 = getPublishedDateFromTouchedFile(tchFile1.getPath(), server1);
            var dateServer2 = getPublishedDateFromTouchedFile(tchFile1.getPath(), server2);

            assertEquals(date, dateServer1);
            assertNull(dateServer2);

            removeServerEntry(sitename, server2);

            dateServer1 = getPublishedDateFromTouchedFile(tchFile1.getPath(), server1);
            dateServer2 = getPublishedDateFromTouchedFile(tchFile1.getPath(), server2);

            assertEquals(date, dateServer1);
            assertNull(dateServer2);

        } catch (Exception e) {
            fail("No exception should have been thrown");
        }
    }

    @Test
    public void testRemoveServerEntry() {
        try {
            createTouchedFile(sitename);
            tchFile1 = getTouchedFile(sitename);

            // add the date for the first server
            var millis = Calendar.getInstance();
            millis.add(Calendar.DAY_OF_MONTH, -1);
            var date = new Date(millis.getTimeInMillis());
            setPublishedDateInTouchedFile(tchFile1.getPath(), server1, date);

            // add the date for the second server
            millis.add(Calendar.DAY_OF_MONTH, -2);
            var date2 = new Date(millis.getTimeInMillis());
            setPublishedDateInTouchedFile(tchFile1.getPath(), server2, date2);

            var dateServer1 = getPublishedDateFromTouchedFile(tchFile1.getPath(), server1);
            var dateServer2 = getPublishedDateFromTouchedFile(tchFile1.getPath(), server2);

            assertEquals(date, dateServer1);
            assertEquals(date2, dateServer2);

            removeServerEntry(sitename, server1);

            dateServer1 = getPublishedDateFromTouchedFile(tchFile1.getPath(), server1);
            dateServer2 = getPublishedDateFromTouchedFile(tchFile1.getPath(), server2);

            assertNull(dateServer1);
            assertEquals(date2, dateServer2);

            removeServerEntry(sitename, server2);

            dateServer1 = getPublishedDateFromTouchedFile(tchFile1.getPath(), server1);
            dateServer2 = getPublishedDateFromTouchedFile(tchFile1.getPath(), server2);

            assertNull(dateServer1);
            assertNull(dateServer2);
        } catch (Exception e) {
            fail("No exception should have been thrown");
        }
    }

    @Test
    public void testSplitAllowedGroups() {
        String allowedGroups;
        String[] expected;
        String[] actual;

        allowedGroups = null;
        expected = new String[]{};
        actual = getAllowedGroups(allowedGroups);
        testEqualArrays(expected, actual);

        allowedGroups = "";
        expected = new String[]{};
        actual = getAllowedGroups(allowedGroups);
        testEqualArrays(expected, actual);

        allowedGroups = "firstGroup,secondGroup";
        expected = new String[]{"firstGroup", "secondGroup"};
        actual = getAllowedGroups(allowedGroups);
        testEqualArrays(expected, actual);

        allowedGroups = "first\\,group,secondGroup";
        expected = new String[]{"first\\,group", "secondGroup"};
        actual = getAllowedGroups(allowedGroups);
        testEqualArrays(expected, actual);

        allowedGroups = "first\\,group,second\\,Group";
        expected = new String[]{"first\\,group", "second\\,Group"};
        actual = getAllowedGroups(allowedGroups);
        testEqualArrays(expected, actual);

        allowedGroups = "first\\,,group,secondGroup";
        expected = new String[]{"first\\,", "group", "secondGroup"};
        actual = getAllowedGroups(allowedGroups);
        testEqualArrays(expected, actual);
    }

    @Test
    public void testBuildXmlData() {
        boolean useHttpsForSecureSite = true;

        var expectedXmlData = new SecureXmlData();
        var root = generateSectionTree(expectedXmlData, useHttpsForSecureSite);

        assertEquals(expectedXmlData, PSSiteConfigUtils.buildXmlDataForSite(expectedXmlData.getSitename(),
                expectedXmlData.getLoginPage(), root, useHttpsForSecureSite));
    }

    private PSSectionNode generateSectionTree(SecureXmlData expected, boolean useHttpsForSecureSite) {
        var siteName = "TestSite" + System.currentTimeMillis();
        var loginPage = "/index.html";

        // Secure XML Data
        expected.setSitename(siteName);
        expected.setLoginPage(loginPage);
        expected.setUseHttpsForSecureSite(useHttpsForSecureSite);
        expected.addSecureOrMemberSection("/section1/", "");
        expected.addSecureOrMemberSection("/section2/section2-2/", "editor,admin");
        expected.addSecureOrMemberSection("/section3/section3-1/", "");

        // Section 1
        var section1childs = new ArrayList<PSSectionNode>();
        section1childs.add(createNode("//Sites/" + siteName + "/section1/section1-1", false, "", null));
        section1childs.add(createNode("//Sites/" + siteName + "/section1/section1-2", false, "", null));

        var section1 = createNode("//Sites/" + siteName + "/section1", true, "", section1childs);

        // Section 2
        var section22childs = new ArrayList<PSSectionNode>();
        section22childs.add(createNode("//Sites/" + siteName + "/section2/section2-2/section2-2-1", false, "", null));
        section22childs.add(createNode("//Sites/" + siteName + "/section2/section2-2/section2-2-2", false, "", null));
        section22childs.add(createNode("//Sites/" + siteName + "/section2/section2-2/section2-2-3", false, "", null));

        var section2childs = new ArrayList<PSSectionNode>();
        section2childs.add(createNode("//Sites/" + siteName + "/section2/section2-1", false, "", null));
        section2childs.add(createNode("//Sites/" + siteName + "/section2/section2-2", true, "editor,admin",
                section22childs));

        var section2 = createNode("//Sites/" + siteName + "/section2", false, "", section2childs);

        // Section 3
        var section3childs = new ArrayList<PSSectionNode>();
        section3childs.add(createNode("//Sites/" + siteName + "/section3/section3-1", true, "", null));
        section3childs.add(createNode("//Sites/" + siteName + "/section3/section3-2", false, "", null));

        var section3 = createNode("//Sites/" + siteName + "/section3", false, "", section3childs);

        // Root Section
        var rootChilds = new ArrayList<PSSectionNode>();
        rootChilds.add(section1);
        rootChilds.add(section2);
        rootChilds.add(section3);

        return createNode("//Sites/" + siteName, true, "", rootChilds);
    }

    private PSSectionNode createNode(String folderPath, boolean requiresLogin, String allowAccessTo,
                                     List<PSSectionNode> childs) {
        var node = new PSSectionNode();
        node.setFolderPath(folderPath);
        node.setRequiresLogin(requiresLogin);
        node.setAllowAccessTo(allowAccessTo);
        node.setChildNodes(childs);
        return node;
    }

    private void testEqualArrays(String[] expected, String[] actual) {
        assertNotNull(expected);
        assertNotNull(actual);
        assertEquals(expected.length, actual.length);
        assertArrayEquals(expected, actual);
    }

    private Date createSiteConfigurationAndTouchedFile() {
        try {
            createSecureSiteConfiguration(sitename);
            var publishedDate = createOnlyTouchedFile();

            siteConfig1 = new File(getSitesConfigPath(), sitename);
            assertTrue(siteConfig1.exists());

            return publishedDate;
        } catch (Exception e) {
            fail("No exception should have been thrown");
        }
        return null;
    }

    private Date createOnlyTouchedFile() {
        try {
            createTouchedFile(sitename);

            tchFile1 = getTouchedFile(sitename);
            assertTrue(tchFile1.exists());

            var millis = Calendar.getInstance();
            millis.add(Calendar.DAY_OF_MONTH, -1);
            millis.add(Calendar.MONTH, -1);
            setPublishedDateInTouchedFile(tchFile1.getPath(), server1, new Date(millis.getTimeInMillis()));

            return new Date(millis.getTimeInMillis());
        } catch (Exception e) {
            fail("No exception should have been thrown");
        }
        return null;
    }

    private boolean sameFilesInCollection(Collection<File> defaultConfigFiles, Collection<File> siteConfigFiles) {
        for (var defaultFile : defaultConfigFiles) {
            boolean contains = false;
            for (var siteFile : siteConfigFiles) {
                if (defaultFile.getName().equals(siteFile.getName())) {
                    contains = true;
                    break;
                }
            }
            if (!contains) {
                return false;
            }
        }
        return true;
    }
}
