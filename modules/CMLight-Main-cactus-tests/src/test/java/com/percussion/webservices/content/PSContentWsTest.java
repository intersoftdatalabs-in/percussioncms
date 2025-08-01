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
package com.percussion.webservices.content;

import com.percussion.cms.objectstore.PSFolder;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.testing.IntegrationTest;
import com.percussion.webservices.security.IPSSecurityWs;
import com.percussion.webservices.security.PSSecurityWsLocator;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Tag;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test the content web service.
 */
@Tag("IntegrationTest")
class PSContentWsTest {

    @BeforeEach
    void setUp() throws Exception {
        var secWs = PSSecurityWsLocator.getSecurityWebservice();
        secWs.login(request, response, "admin1", "demo", null, "Enterprise_Investments_Admin", null);
    }

    @Test
    @DisplayName("Load Folders by Path and Guid")
    void testLoadFolders() throws Exception {
        var contentWs = PSContentWsLocator.getContentWebservice();
        var folders = contentWs.loadFolders(new String[]{"//Sites/EnterpriseInvestments"});
        var f1 = folders.get(0);
        var f1Guid = f1.getGuid();
        assertNotNull(f1Guid);
        folders = contentWs.loadFolders(Collections.singletonList(f1Guid));
        var f2 = folders.get(0);
        var f2Guid = f2.getGuid();
        assertNotNull(f2Guid);
        assertEquals(f1Guid, f2Guid);
        assertEquals(f1, f2);
    }

    @Test
    @DisplayName("Load Then Save Folders")
    void testLoadThenSaveFolders() throws Exception {
        var contentWs = PSContentWsLocator.getContentWebservice();
        var testFolder = contentWs.addFolder("Test", "//Sites/EnterpriseInvestments");
        var folders = contentWs.loadFolders(Collections.singletonList(testFolder.getGuid()));
        var folderIds = contentWs.saveFolders(folders);
        assertFalse(folderIds.isEmpty());
    }

    @Test
    @DisplayName("Find Descendant Folders")
    void testFindDescendantFolders() throws Exception {
        var contentWs = PSContentWsLocator.getContentWebservice();

        var id = contentWs.getIdByPath("//Sites/EnterpriseInvestments/AboutEnterpriseInvestments");
        var folders = contentWs.findDescendantFolders(id);
        assertEquals(4, folders.size());

        id = contentWs.getIdByPath("//Sites/EnterpriseInvestments/Files");
        folders = contentWs.findDescendantFolders(id);
        assertEquals(0, folders.size());
    }

    @AfterEach
    void tearDown() {
        try {
            var contentWs = PSContentWsLocator.getContentWebservice();
            var folders = contentWs.loadFolders(new String[]{"//Sites/EnterpriseInvestments/Test"});
            if (!folders.isEmpty()) {
                contentWs.deleteFolders(Collections.singletonList(folders.get(0).getGuid()), true);
            }
        } catch (Exception e) {
            // Could not delete, ignore for test cleanup
        }
    }
}
