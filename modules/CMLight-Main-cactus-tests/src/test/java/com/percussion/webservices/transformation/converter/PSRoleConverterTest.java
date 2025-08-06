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
package com.percussion.webservices.transformation.converter;

import com.percussion.design.objectstore.PSRole;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;

import org.junit.jupiter.api.Tag;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link PSRoleConverter} class.
 */
@Tag("IntegrationTest")
public class PSRoleConverterTest extends PSConverterTestBase {

    /**
     * Tests the conversion from a server to a client object.
     */
    public void testConversion() throws Exception {
        // create the source object
        var source = new PSRole("name");
        source.setId(new PSGuid(PSTypeEnum.ROLE, 101).getUUID());

        var target = (PSRole) roundTripConversion(PSRole.class,
                com.percussion.webservices.security.data.PSRole.class, source);

        // verify the round-trip object is equal to the source object
        assertEquals(source, target);
    }

    /**
     * Test a list of server objects convert to client array, and vice versa.
     *
     * @throws Exception if an error occurs.
     */
    @SuppressWarnings("unchecked")
    public void testListToArray() throws Exception {
        var srcList = new ArrayList<PSRole>();
        srcList.add(new PSRole("name"));
        srcList.add(new PSRole("name_2"));

        var srcList2 = roundTripListConversion(
                com.percussion.webservices.security.data.PSRole[].class, srcList);

        assertEquals(srcList, srcList2);
    }
}
