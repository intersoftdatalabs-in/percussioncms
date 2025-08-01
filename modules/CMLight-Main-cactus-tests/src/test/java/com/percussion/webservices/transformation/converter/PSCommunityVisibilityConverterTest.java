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
package com.percussion.webservices.transformation.converter;

import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.catalog.data.PSObjectSummary;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.security.data.PSCommunityVisibility;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.testing.IntegrationTest;
import org.junit.experimental.categories.Category;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link PSCommunityVisibilityConverter} class.
 */
@Category(IntegrationTest.class)
public class PSCommunityVisibilityConverterTest extends PSConverterTestBase {

    /**
     * Tests the conversion from a server to a client object as well as a
     * server array of objects to a client array of objects and back.
     */
    public void testConversion() throws Exception {
        var source = createCommunityVisibility(getNextId(PSTypeEnum.COMMUNITY_DEF));

        var target = (PSCommunityVisibility) roundTripConversion(
                PSCommunityVisibility.class,
                com.percussion.webservices.security.data.PSCommunityVisibility.class,
                source);

        // verify the round-trip object is equal to the source object
        assertEquals(source, target);

        // create the source array
        var sourceArray = new PSCommunityVisibility[]{source};

        var targetArray = (PSCommunityVisibility[]) roundTripConversion(
                PSCommunityVisibility[].class,
                com.percussion.webservices.security.data.PSCommunityVisibility[].class,
                sourceArray);

        // verify the round-trip array is equal to the source array
        assertEquals(sourceArray.length, targetArray.length);
        assertEquals(sourceArray[0], targetArray[0]);
    }

    /**
     * Test a list of server objects convert to client array, and vice versa.
     */
    @SuppressWarnings("unchecked")
    public void testListToArray() throws Exception {
        var srcList = new ArrayList<PSCommunityVisibility>();
        srcList.add(createCommunityVisibility(getNextId(PSTypeEnum.COMMUNITY_DEF)));
        srcList.add(createCommunityVisibility(getNextId(PSTypeEnum.COMMUNITY_DEF)));

        var srcList2 = roundTripListConversion(
                com.percussion.webservices.security.data.PSCommunityVisibility[].class,
                srcList);

        assertEquals(srcList, srcList2);
    }

    /**
     * Create a test community visibility for the specified community.
     *
     * @param id the community id, not {@code null}.
     * @return the test community visibility, never {@code null}.
     */
    public static PSCommunityVisibility createCommunityVisibility(IPSGuid id) {
        if (id == null)
            throw new IllegalArgumentException("id cannot be null");

        if (id.getType() != PSTypeEnum.COMMUNITY_DEF.getOrdinal())
            throw new IllegalArgumentException("id must be of type community");

        var communityVisibility = new PSCommunityVisibility(id);
        communityVisibility.addVisibleObject(
                new PSObjectSummary(new PSGuid(PSTypeEnum.WORKFLOW, 1000),
                        "name_1", "label_1", "description_1"));
        communityVisibility.addVisibleObject(
                new PSObjectSummary(new PSGuid(PSTypeEnum.ACTION, 1001),
                        "name_2", "label_2", "description_2"));

        return communityVisibility;
    }
}
