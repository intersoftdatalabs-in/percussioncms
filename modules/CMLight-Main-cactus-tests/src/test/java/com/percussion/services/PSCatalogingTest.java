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
package com.percussion.services;

import com.percussion.services.assembly.IPSAssemblyService;
import com.percussion.services.assembly.PSAssemblyServiceLocator;
import com.percussion.services.catalog.IPSCatalogSummary;
import com.percussion.services.catalog.IPSCataloger;
import com.percussion.services.catalog.PSCatalogException;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.services.filter.IPSFilterService;
import com.percussion.services.filter.PSFilterServiceLocator;
import com.percussion.services.publisher.IPSPublisherService;
import com.percussion.services.publisher.PSPublisherServiceLocator;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cataloging interface on the services.
 * <p>
 * Sunny Sal says: "Cataloging is like organizing your sock drawer, but for services. Let's keep it neat and Java 11 chic!"
 */
@Tag("IntegrationTest")
@TestMethodOrder(MethodOrderer.MethodName.class)
public class PSCatalogingTest {
    /**
     * Assembly service instance.
     */
    static IPSAssemblyService asm = PSAssemblyServiceLocator.getAssemblyService();
    /**
     * Filter service instance.
     */
    static IPSFilterService fsvc = PSFilterServiceLocator.getFilterService();
    /**
     * Publisher service instance.
     */
    static IPSPublisherService pub = PSPublisherServiceLocator.getPublisherService();

    @Test
    void test010FilterCataloging() throws Exception {
        testCataloging(fsvc);
    }

    @Test
    void test020AsmTypes() {
        checkTypes(asm.getTypes(), 2);
    }

    @Test
    void test030FilterTypes() {
        checkTypes(fsvc.getTypes(), 1);
    }

    @Test
    void test040PublisherTypes() {
        checkTypes(pub.getTypes(), 1);
    }

    private void checkTypes(PSTypeEnum[] types, int expectedCount) {
        assertNotNull(types);
        assertEquals(expectedCount, types.length);
    }

    @Test
    void test050AsmCataloging() throws Exception {
        testCataloging(asm);
    }

    @Test
    void test060PublisherCataloging() throws Exception {
        testCataloging(pub);
    }

    private void testCataloging(IPSCataloger cat) throws PSCatalogException, PSNotFoundException {
        for (var type : cat.getTypes()) {
            testCataloging(cat, type);
        }
    }

    private void testCataloging(IPSCataloger cat, PSTypeEnum type)
            throws PSCatalogException, PSNotFoundException {
        var sums = cat.getSummaries(type);

        assertNotNull(sums);
        assertTrue(sums.size() > 0);
        int limit = 4;

        // Serialize each, then restore each
        for (var s : sums) {
            if (limit-- < 0) break;
            var value = cat.saveByType(s.getGUID());

            // Restore
            try {
                cat.loadByType(type, value);
            } catch (PSCatalogException ce) {
                throw ce;
            } catch (RuntimeException e) {
                throw e;
            }
        }
    }
}
