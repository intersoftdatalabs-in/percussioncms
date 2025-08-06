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

import com.percussion.cms.handlers.PSRelationshipCommandHandler;
import com.percussion.cms.objectstore.PSAaRelationship;
import com.percussion.design.objectstore.PSLocator;
import com.percussion.design.objectstore.PSRelationship;
import com.percussion.design.objectstore.PSRelationshipConfig;
import com.percussion.design.objectstore.PSRelationshipConfigSet;
import com.percussion.design.objectstore.PSRelationshipConfigTest;
import com.percussion.system.utils.IPSHtmlParameters;
import com.percussion.utils.testing.IntegrationTest;
import com.percussion.xml.PSXmlDocumentBuilder;
import org.junit.experimental.categories.Category;
import org.w3c.dom.Document;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link PSRelationshipConfigConverter} class.
 */
@Category(IntegrationTest.class)
public class PSAaRelationshipConverterTest extends PSConverterTestBase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        var cset = PSRelationshipConfigTest.getConfigs();
        PSRelationshipCommandHandler.reloadConfigs(cset);
    }

    /**
     * Tests the conversion for AA relationship from a server to a client object and vice versa.
     */
    public void testAaRelationshipConversion() throws Exception {
        var aaRel = createAaRelationship(1, 2, 3, 4, 5);
        roundTripConversionAssert(aaRel);
    }

    /**
     * Tests the conversion for relationship from a server to a client object and vice versa.
     */
    public void testRelationshipConversion() throws Exception {
        var aaRel = createAaRelationship(1, 2, 3, 4, 5);
        var doc = PSXmlDocumentBuilder.createXmlDocument();
        var rel = new PSRelationship(aaRel.toXml(doc));
        roundTripConversionAssert(rel);
    }

    private PSAaRelationship createAaRelationship(int rid, int ownerId, int dependentId, int slotId, int templateId) {
        var config = PSRelationshipCommandHandler.getRelationshipConfig(
                PSRelationshipConfig.TYPE_ACTIVE_ASSEMBLY);
        var rel = new PSRelationship(rid, new PSLocator(ownerId, 1),
                new PSLocator(dependentId, -1), config);
        rel.setProperty(IPSHtmlParameters.SYS_SLOTID, String.valueOf(slotId));
        rel.setProperty(IPSHtmlParameters.SYS_VARIANTID, String.valueOf(templateId));
        var target = new PSAaRelationship(rel);
        target.setSortRank(1);
        return target;
    }

    private void roundTripConversionAssert(PSRelationship source) throws Exception {
        PSRelationship target;
        if (source instanceof PSAaRelationship) {
            target = (PSAaRelationship) roundTripConversion(
                    PSAaRelationship.class,
                    com.percussion.webservices.content.PSAaRelationship.class,
                    source);
        } else {
            target = (PSRelationship) roundTripConversion(
                    PSRelationship.class,
                    com.percussion.webservices.system.PSRelationship.class,
                    source);
        }
        var doc = PSXmlDocumentBuilder.createXmlDocument();
        var sourceString = PSXmlDocumentBuilder.toString(source.toXml(doc));
        var targetString = PSXmlDocumentBuilder.toString(target.toXml(doc));
        // System.out.println(sourceString);
        // System.out.println(targetString);
        assertEquals(source, target);
    }

    /**
     * Test a list of server objects convert to (client) search array, and vice versa.
     */
    @SuppressWarnings("unchecked")
    public void testConfigListToArray() throws Exception {
        var srcList = new ArrayList<PSAaRelationship>();
        srcList.add(createAaRelationship(2, 3, 4, 5, 6));
        srcList.add(createAaRelationship(20, 30, 40, 50, 60));

        var tgtList = roundTripListConversion(
                com.percussion.webservices.content.PSAaRelationship[].class,
                srcList);

        assertEquals(srcList, tgtList);

        var doc = PSXmlDocumentBuilder.createXmlDocument();
        var srcList2 = new ArrayList<PSRelationship>();
        var rel = createAaRelationship(12, 13, 14, 15, 16);
        srcList2.add(new PSRelationship(rel.toXml(doc)));
        rel = createAaRelationship(120, 130, 140, 150, 160);
        srcList2.add(new PSRelationship(rel.toXml(doc)));

        var tgtList2 = roundTripListConversion(
                com.percussion.webservices.system.PSRelationship[].class,
                srcList2);
        assertEquals(srcList2, tgtList2);
    }
}
