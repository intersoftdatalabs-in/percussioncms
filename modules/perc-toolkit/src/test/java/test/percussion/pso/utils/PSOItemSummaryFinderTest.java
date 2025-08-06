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
package test.percussion.pso.utils;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.percussion.cms.objectstore.PSComponentSummary;
import com.percussion.pso.utils.PSOItemSummaryFinder;
import com.percussion.services.legacy.IPSCmsContentSummaries;

/**
 * test.percussion.pso.utils PSOItemSummaryFinderTest.java
 *  
 * @author DavidBenua
 * // REFACTORED: CP-JAVA11
 */
@ExtendWith(MockitoExtension.class)
public class PSOItemSummaryFinderTest {
    private static final Logger log = LogManager.getLogger(PSOItemSummaryFinderTest.class);

    @Mock
    PSComponentSummary summ;
    @Mock
    IPSCmsContentSummaries sumsvc;
    @InjectMocks
    PSOItemSummaryFinder cut;

    @BeforeEach
    public void setUp() {
        PSOItemSummaryFinder.setSumsvc(sumsvc);
    }

    @Test
    public void testGetSummaryInt() {
        log.debug("Starting summary test");
        Mockito.when(sumsvc.loadComponentSummary(1)).thenReturn(summ);
        Mockito.when(summ.getCheckoutUserName()).thenReturn("fred");
        try {
            var s1 = PSOItemSummaryFinder.getSummary(1);
            assertNotNull(s1);
            assertEquals("fred", s1.getCheckoutUserName());
            log.debug("Finished summary test");
        } catch (Exception ex) {
            log.error("Unexpected Exception " + ex, ex);
            fail("Exception");
        }
    }

    @Test
    public void testGetCheckoutStatus() {
        log.debug("Starting checkout status test");
        Mockito.when(sumsvc.loadComponentSummary(1)).thenReturn(summ);
        Mockito.when(summ.getCheckoutUserName()).thenReturn("fred");
        try {
            int status = PSOItemSummaryFinder.getCheckoutStatus("1", "fred");
            assertEquals(PSOItemSummaryFinder.CHECKOUT_BY_ME, status);
            status = PSOItemSummaryFinder.getCheckoutStatus("1", "bob");
            assertEquals(PSOItemSummaryFinder.CHECKOUT_BY_OTHER, status);
            log.debug("finished checkout status test");
        } catch (Exception ex) {
            log.error("Unexpected Exception " + ex, ex);
            fail("Exception");
        }
    }

    @Test
    public void testCheckoutStatusNone() {
        log.debug("Starting checkout status none ");
        Mockito.when(sumsvc.loadComponentSummary(1)).thenReturn(summ);
        Mockito.when(summ.getCheckoutUserName()).thenReturn(null);
        try {
            int status = PSOItemSummaryFinder.getCheckoutStatus("1", "bob");
            assertEquals(PSOItemSummaryFinder.CHECKOUT_NONE, status);
        } catch (Exception ex) {
            log.error("Unexpected Exception " + ex, ex);
            fail("Exception");
        }
    }
}
