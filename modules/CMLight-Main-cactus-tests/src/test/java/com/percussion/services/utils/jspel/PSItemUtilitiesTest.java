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
package com.percussion.services.utils.jspel;

import com.percussion.security.PSThreadRequestUtils;
import com.percussion.utils.testing.IntegrationTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test item utilities methods.
 */
@Tag("IntegrationTest")
public class PSItemUtilitiesTest {

    @Test
    void testSiteInfo() {
        var info = PSItemUtilities.getItemSiteInfo(376);
        assertEquals(1, info.size());

        var folders = info.values();
        assertEquals(1, folders.size());

        info = PSItemUtilities.getItemSiteInfo(455);
        assertEquals(2, info.size());

        folders = (Collection<?>) info.get(info.keySet().iterator().next());
        assertEquals(1, folders.size());
    }

    @Test
    void testFolderPathInfo() {
        PSThreadRequestUtils.initServerThreadRequest();
        var fid = PSItemUtilities.getFolderIdFromPath("//Sites/EnterpriseInvestments");
        assertNotNull(fid);

        fid = PSItemUtilities.getFolderIdFromPath("//X/Y/Z/W");
        assertNull(fid);
    }

    @Test
    void testSiteIdInfo() {
        var sid = PSItemUtilities.getSiteIdFromName("Enterprise_Investments");
        assertNotNull(sid);
        assertEquals(301, sid.intValue());

        sid = PSItemUtilities.getSiteIdFromName("Unknown Site Name");
        assertNull(sid);
    }

    @Test
    void testTitleInfo() {
        var title = PSItemUtilities.getTitle(325);
        assertEquals("Tax", title);

        title = PSItemUtilities.getTitle(1000);
        assertNull(title);
    }
}
