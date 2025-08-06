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
package com.percussion.webservices.ui.impl;

import com.percussion.cms.objectstore.PSDisplayFormat;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.testing.IntegrationTest;
import com.percussion.webservices.security.PSSecurityWsLocator;
import com.percussion.webservices.ui.PSUiWsLocator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for UI Design Web Service.
 */
@Tag(IntegrationTest.class)
class PSUiDesignWsTest {

    @Test
    void testFindDisplayFormat() throws Exception {
        var svc = PSSecurityWsLocator.getSecurityWebservice();
        svc.login("admin1", "demo", null, null);

        var designWs = PSUiWsLocator.getUiDesignWebservice();
        var dsFmt = designWs.findDisplayFormat("Simple");
        var dsFmt2 = designWs.findDisplayFormat(dsFmt.getGUID());
        var dsFmt3 = designWs.findDisplayFormat(dsFmt.getGUID());

        assertSame(dsFmt, dsFmt2);
        assertSame(dsFmt, dsFmt3);

        var ids = new ArrayList<IPSGuid>();
        ids.add(dsFmt.getGUID());
        var dsFmt4 = designWs.loadDisplayFormats(ids, false, false, null, null).get(0);
        assertNotSame(dsFmt, dsFmt4);
    }
}
