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
import com.percussion.services.content.data.PSContentTypeSummary;
import com.percussion.services.content.data.PSFieldDescription;
import com.percussion.services.content.data.PSContentTypeSummaryChild;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.utils.testing.IntegrationTest;
import org.junit.experimental.categories.Category;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the {@link PSContentTypeSummaryConverter} class.
 */
@Category(IntegrationTest.class)
public class PSContentTypeSummaryConverterTest extends PSConverterTestBase {

    /**
     * Tests the conversion from a server to a client object.
     */
    public void testConversion() throws Exception {
        // create the source object
        var src = createContentTypeSummary(100, "CTSummary");

        var target = (PSContentTypeSummary) roundTripConversion(
                PSContentTypeSummary.class,
                com.percussion.webservices.content.PSContentTypeSummary.class,
                src);

        // verify the round-trip object is equal to the source object
        assertEquals(src, target);
    }

    /**
     * Test a list of server objects convert to client array, and vice versa.
     */
    @SuppressWarnings("unchecked")
    public void testListToArray() throws Exception {
        var srcList = new ArrayList<PSContentTypeSummary>();
        srcList.add(createContentTypeSummary(100, "CTSummary"));
        srcList.add(createContentTypeSummary(200, "CTSummary2"));

        var srcList2 = roundTripListConversion(
                com.percussion.webservices.content.PSContentTypeSummary[].class,
                srcList);

        assertEquals(srcList, srcList2);
    }

    private PSContentTypeSummary createContentTypeSummary(int id, String name) {
        var ct = new PSContentTypeSummary();
        ct.setGuid(new PSGuid(PSTypeEnum.NODEDEF, id));
        ct.setName(name);
        ct.setDescription(name + " Desc");

        var childList = new ArrayList<PSContentTypeSummaryChild>();
        childList.add(createSummaryChild(name + "ChildSummary"));
        childList.add(createSummaryChild(name + "ChildSummary2"));

        ct.setChildren(childList);
        ct.setFields(createFieldDesc(name));

        return ct;
    }

    /**
     * Creates a child summary field, which contains 2 fields.
     *
     * @param name the name of the child summary; assumed not {@code null} or empty.
     * @return the created child summary.
     */
    private PSContentTypeSummaryChild createSummaryChild(String name) {
        var src = new PSContentTypeSummaryChild();

        var fields = createFieldDesc(name);
        src.setChildFields(fields);
        src.setName(name);

        return src;
    }

    private List<PSFieldDescription> createFieldDesc(String seed) {
        var field = new PSFieldDescription(seed + "_fld1",
                PSFieldDescription.PSFieldTypeEnum.TEXT.name());
        var field2 = new PSFieldDescription(seed + "_fld2",
                PSFieldDescription.PSFieldTypeEnum.BINARY.name());

        var fields = new ArrayList<PSFieldDescription>();
        fields.add(field);
        fields.add(field2);

        return fields;
    }
}
