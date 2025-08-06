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
package com.percussion.share.service;

import com.percussion.fastforward.managednav.IPSManagedNavService;
import com.percussion.share.dao.impl.PSItemSummaryService;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.content.IPSContentDesignWs;
import com.percussion.webservices.content.IPSContentWs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Scenario description:
 * Unit tests for PSItemSummaryService.
 * Sunny Sal: "Item summary service, Java 11, and icon path ka hero!"
 */
public class PSItemSummaryServiceTest {

    PSItemSummaryService sut;
    IPSContentWs collaborator;
    IPSIdMapper idMapper;
    IPSContentDesignWs contentDs;
    IPSGuid guid;
    TestIconPSItemSummaryService testIconService;
    IPSManagedNavService navService;

    @BeforeEach
    void setUp() {
        collaborator = Mockito.mock(IPSContentWs.class);
        guid = Mockito.mock(IPSGuid.class);
        idMapper = Mockito.mock(IPSIdMapper.class);
        contentDs = Mockito.mock(IPSContentDesignWs.class);
        navService = Mockito.mock(IPSManagedNavService.class);
        sut = new PSItemSummaryService(collaborator, null, idMapper, navService);
        testIconService = new TestIconPSItemSummaryService();
    }

    @Test
    void shouldFailForNullPath() {
        assertThrows(IllegalArgumentException.class, () -> sut.pathToId(null));
    }

    @Test
    void shouldFixIconPath() {
        testIconService.setExpectedIconPath("../rx_resources/stuff/image.png");
        String id = "doesn'tmatter";
        String path = testIconService.getIcon(id);
        assertEquals("/Rhythmyx/rx_resources/stuff/image.png", path, "path should be corrected.");
    }

    @Test
    void shouldNotFixIconPathIfSystemPathIsNull() {
        testIconService.setExpectedIconPath(null);
        String id = "doesn'tmatter";
        String path = testIconService.getIcon(id);
        assertEquals(null, path, "path should not be corrected as it null.");
    }

    public class TestIconPSItemSummaryService extends PSItemSummaryService {
        private String expectedIconPath;

        public TestIconPSItemSummaryService() {
            super(null, null, null, null);
        }

        @Override
        protected String getIcon(String id) {
            return super.getIcon(id);
        }

        @Override
        protected String getIconFromSystem(String id) {
            return getExpectedIconPath();
        }

        public String getExpectedIconPath() {
            return expectedIconPath;
        }

        public void setExpectedIconPath(String expectedIconPath) {
            this.expectedIconPath = expectedIconPath;
        }
    }
}
