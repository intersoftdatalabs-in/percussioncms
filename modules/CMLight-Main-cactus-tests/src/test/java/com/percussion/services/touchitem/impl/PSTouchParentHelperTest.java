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
package com.percussion.services.touchitem.impl;

import com.percussion.cms.objectstore.PSRelationshipFilter;
import com.percussion.design.objectstore.PSRelationship;
import com.percussion.server.IPSRequestContext;
import com.percussion.server.PSRequest;
import com.percussion.server.PSRequestContext;
import com.percussion.services.relationship.IPSRelationshipService;
import com.percussion.services.relationship.PSRelationshipServiceLocator;
import com.percussion.servlets.PSSecurityFilter;
import com.percussion.utils.testing.IntegrationTest;
import com.percussion.webservices.PSWebserviceUtils;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for PSTouchParentHelper.
 */
@Tag("IntegrationTest")
public class PSTouchParentHelperTest {

    private PSTouchParentHelper toucher;
    private IPSRelationshipService rservice;
    private PSRequest rxRequest;
    private IPSRequestContext requestContext;

    @BeforeEach
    public void setUp() throws Exception {
        toucher = new PSTouchParentHelper();
        rservice = PSRelationshipServiceLocator.getRelationshipService();
        // session.setAttribute("RX_LOGIN_ATTEMPTS", null); // Not needed in JUnit5
        PSSecurityFilter.authenticate(null, null, "admin1", "demo");
        rxRequest = PSWebserviceUtils.getRequest();
        requestContext = new PSRequestContext(rxRequest);
    }

    @Test
    void testTouch() throws Exception {
        var f = new PSRelationshipFilter();
        f.setOwnerId(539);
        // Hand Signing Papers.jpg (460)
        f.setDependentId(460);
        var rs = rservice.findByFilter(f);
        int total = toucher.touchItemAndParents(requestContext, rs.get(0));
        assertEquals(3, total);

        // Family Group.jpg .... Many links
        f.setDependentId(459);
        rs = rservice.findByFilter(f);
        total = toucher.touchItemAndParents(requestContext, rs.get(0));
        assertEquals(7, total, "Family Group.jpg");
    }
}
