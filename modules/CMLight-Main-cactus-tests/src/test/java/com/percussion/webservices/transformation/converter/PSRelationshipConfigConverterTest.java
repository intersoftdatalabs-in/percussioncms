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

import com.percussion.design.objectstore.PSRelationshipConfig;
import com.percussion.design.objectstore.PSRelationshipConfigSet;
import com.percussion.design.objectstore.PSRelationshipConfigTest;
import com.percussion.services.guidmgr.data.PSDesignGuid;
import com.percussion.utils.testing.IntegrationTest;
import com.percussion.webservices.system.RelationshipConfigSummary;
import com.percussion.webservices.transformation.impl.PSTransformerFactory;
import org.apache.commons.beanutils.Converter;
import org.junit.jupiter.api.Tag;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link PSRelationshipConfigConverter} class.
 */
@Tag(IntegrationTest.class)
public class PSRelationshipConfigConverterTest extends PSConverterTestBase {

    /**
     * Tests the conversion from a server to a client object and vice versa.
     */
    public void testConversion() throws Exception {
        // test with simple relationship
        var simpleConfig = getSimpleConfig("simpleConfig");
        roundTripConversionAssert(simpleConfig);

        var cset = PSRelationshipConfigTest.getConfigs();
        for (var config : cset.getConfigList()) {
            roundTripConversionAssert(config);
        }
    }

    /**
     * Round trip the supplied source.
     *
     * @param source the source to round trip, assumed not {@code null}.
     * @throws Exception for any error.
     */
    private void roundTripConversionAssert(PSRelationshipConfig source) throws Exception {
        var target = (PSRelationshipConfig) roundTripConversion(
                PSRelationshipConfig.class,
                com.percussion.webservices.system.PSRelationshipConfig.class,
                source);

        // verify the round-trip object is equal to the source object
        assertEquals(source, target);
    }

    /**
     * Test a list of server objects convert to (client) search array, and vice versa.
     *
     * @throws Exception if an error occurs.
     */
    @SuppressWarnings("unchecked")
    public void testConfigListToArray() throws Exception {
        var cset = PSRelationshipConfigTest.getConfigs();
        var srcList = cset.getConfigList();

        // test simple search objects
        var tgtList = roundTripListConversion(
                com.percussion.webservices.system.PSRelationshipConfig[].class,
                srcList);

        assertEquals(srcList, tgtList);
    }

    /**
     * Test the relationship config summary converter.
     *
     * @throws Exception for any error.
     */
    public void testRelationshipConfigSummary() throws Exception {
        var cset = PSRelationshipConfigTest.getConfigs();
        var srcList = cset.getConfigList();

        var factory = PSTransformerFactory.getInstance();

        // convert from list to array
        Converter converter = factory.getConverter(RelationshipConfigSummary[].class);
        var tgtArray = (RelationshipConfigSummary[]) converter.convert(
                RelationshipConfigSummary[].class, srcList);

        for (int i = 0; i < tgtArray.length; i++) {
            var src = srcList.get(i);
            long id = (new PSDesignGuid(tgtArray[i].getId())).longValue();
            assertEquals(id, src.getId());
            assertEquals(tgtArray[i].getName(), src.getName());
            assertEquals(tgtArray[i].getLabel(), src.getLabel());
            assertEquals(tgtArray[i].getDescription(), src.getDescription());
        }
    }

    /**
     * Creates a relationship config with the given name.
     *
     * @param name the name of the new config, assumed not {@code null}.
     * @return the created config, never {@code null}.
     */
    private PSRelationshipConfig getSimpleConfig(String name) {
        var target = new PSRelationshipConfig(
                name,
                PSRelationshipConfig.RS_TYPE_USER,
                PSRelationshipConfig.CATEGORY_COPY);
        target.setId(1000);
        return target;
    }
}
