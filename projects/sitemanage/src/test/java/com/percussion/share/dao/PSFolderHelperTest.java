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
package com.percussion.share.dao;

import com.percussion.security.SecureStringUtils;
import com.percussion.share.dao.impl.PSFolderHelper;
import com.percussion.share.data.IPSItemSummary;
import com.percussion.share.spring.PSSpringWebApplicationContextUtils;

import com.percussion.webservices.security.IPSSecurityWs;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Tag;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Tag("IntegrationTest")
public class PSFolderHelperTest {

    // Nested test class for folder path logic
    public static class PSFolderPathTest {
        private PSFolderHelper folderHelper;

        @BeforeEach
        void setup() {
            folderHelper = new PSFolderHelper(null, null, null, null, null, null, null, null, null, null);
        }

        @Test
        void testTwoLevelParentPath() {
            assertParent("//one/two", "//one");
        }

        @Test
        void testTwoLevelParentPathWithTrailingSlash() {
            assertParent("//one/two/", "//one");
        }

        @Test
        void testOneLevelParentPath() {
            assertParent("//one", "//");
        }

        @Test
        void testNullPath() {
            assertThrows(IllegalArgumentException.class, () -> folderHelper.parentPath(null));
        }

        private void assertParent(String path, String expected) {
            var p = folderHelper.parentPath(path);
            assertEquals(expected, p, "Parent path:");
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        PSSpringWebApplicationContextUtils.injectDependencies(this);
        securityWs.login(request, response, "admin1", "demo", null, "Enterprise_Investments_Admin", null);
    }

    @Test
    void testFindItems() throws Exception {
        var items = folderHelper.findItems("//Sites");
        assertNotNull(items);
        assertFalse(items.isEmpty());
        log.debug(items);
    }

    @Test
    void testFindChildren() throws Exception {
        var paths = folderHelper.findChildren("//Sites");
        log.debug("Paths : {}", paths);
        assertNotNull(paths, "Paths should not be null");
        assertFalse(paths.isEmpty(), "Paths should not be empty");
    }

    @Test
    void testFindFolderChildren() throws Exception {
        var items = folderHelper.findItems("//Sites/EnterpriseInvestments");
        assertNotNull(items);
        assertFalse(items.isEmpty());

        var allItems = folderHelper.findItems("//Sites/EnterpriseInvestments", false);
        assertNotNull(allItems);
        assertFalse(allItems.isEmpty());
        assertEquals(items.size(), allItems.size());
        int folderCount = 0;
        for (var itemSummary : allItems) {
            if (itemSummary.isFolder())
                folderCount++;
        }

        var folderItems = folderHelper.findItems("//Sites/EnterpriseInvestments", true);
        assertNotNull(folderItems);
        assertFalse(folderItems.isEmpty());
        assertTrue(items.size() > folderItems.size());
        assertEquals(folderCount, folderItems.size());
        for (var itemSummary : folderItems) {
            assertTrue(itemSummary.isFolder());
        }
    }

    @Test
    void testUniqueNameInFolder() throws Exception {
        try {
            folderHelper.createFolder("//Sites/UniqueNameInFolderTest");
            var name = folderHelper.getUniqueNameInFolder("//Sites", "UniqueNameInFolderTest", "copy", 1, true);
            assertEquals("UniqueNameInFolderTest-copy", name);

            folderHelper.createFolder("//Sites/UniqueNameInFolderTest-copy");
            var name1 = folderHelper.getUniqueNameInFolder("//Sites", "UniqueNameInFolderTest", "copy", 1, true);
            assertEquals("UniqueNameInFolderTest-copy-2", name1);

            folderHelper.createFolder("//Sites/UniqueNameInFolderTest-copy-2");
            var name2 = folderHelper.getUniqueNameInFolder("//Sites", "UniqueNameInFolderTest", "copy", 1, true);
            assertEquals("UniqueNameInFolderTest-copy-3", name2);

            folderHelper.deleteFolder("//Sites/UniqueNameInFolderTest-copy");
            var name3 = folderHelper.getUniqueNameInFolder("//Sites", "UniqueNameInFolderTest", "copy", 1, true);
            assertEquals("UniqueNameInFolderTest-copy", name3);

            var name4 = folderHelper.getUniqueNameInFolder("//Sites", "UniqueNameInFolderTest", "copy", 1, false);
            assertEquals("UniqueNameInFolderTest-copy-1", name4);

            var name5 = folderHelper.getUniqueNameInFolder("//Sites", "UniqueNameInFolderTest", "copy", 10, false);
            assertEquals("UniqueNameInFolderTest-copy-10", name5);
        } finally {
            folderHelper.deleteFolder("//Sites/UniqueNameInFolderTest");
            folderHelper.deleteFolder("//Sites/UniqueNameInFolderTest-copy");
            folderHelper.deleteFolder("//Sites/UniqueNameInFolderTest-copy-1");
            folderHelper.deleteFolder("//Sites/UniqueNameInFolderTest-copy-2");
            folderHelper.deleteFolder("//Sites/UniqueNameInFolderTest-copy-3");
            folderHelper.deleteFolder("//Sites/UniqueNameInFolderTest-copy-10");
        }
    }

    @Test
    void testUniqueFolderName() throws Exception {
        var pathsToDelete = new ArrayList<String>();
        try {
            var folderName = "UniqueFolderNameTest";
            var name = folderHelper.getUniqueFolderName("//Sites", folderName);
            assertEquals(folderName, name);

            folderHelper.createFolder("//Sites/" + folderName);
            pathsToDelete.add("//Sites/" + folderName);
            var name2 = folderHelper.getUniqueFolderName("//Sites", folderName);
            assertEquals(folderName + "-2", name2);

            folderHelper.createFolder("//Sites/" + name2);
            var name3 = folderHelper.getUniqueFolderName("//Sites", folderName);
            assertEquals(folderName + "-3", name3);

            folderHelper.createFolder("//Sites/" + name3);
            pathsToDelete.add("//Sites/" + name3);

            folderHelper.deleteFolder("//Sites/" + name2);

            var name4 = folderHelper.getUniqueFolderName("//Sites", folderName);
            assertEquals(name2, name4);

            var name5 = folderHelper.getUniqueFolderName("//Sites",
                    folderName + SecureStringUtils.INVALID_ITEM_NAME_CHARACTERS);
            assertEquals(name2, name5);

            var name6 = folderHelper.getUniqueFolderName("//Sites",
                    "Invalid" + SecureStringUtils.INVALID_ITEM_NAME_CHARACTERS + "FolderName");
            assertEquals("InvalidFolderName", name6);
        } finally {
            for (var path : pathsToDelete) {
                folderHelper.deleteFolder(path);
            }
        }
    }

    private IPSFolderHelper folderHelper;
    private IPSSecurityWs securityWs;

    public IPSSecurityWs getSecurityWs() {
        return securityWs;
    }

    public void setSecurityWs(IPSSecurityWs securityWs) {
        this.securityWs = securityWs;
    }

    public IPSFolderHelper getFolderHelper() {
        return folderHelper;
    }

    public void setFolderHelper(IPSFolderHelper folderHelper) {
        this.folderHelper = folderHelper;
    }

    private static final Logger log = LogManager.getLogger(PSFolderHelperTest.class);
}
