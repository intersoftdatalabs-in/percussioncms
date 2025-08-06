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

import com.percussion.services.filter.IPSFilterService;
import com.percussion.services.filter.IPSItemFilter;
import com.percussion.services.filter.IPSItemFilterRuleDef;
import com.percussion.services.filter.PSFilterServiceLocator;
import com.percussion.services.filter.data.PSItemFilter;
import com.percussion.services.filter.data.PSItemFilterRuleDef;

import org.junit.jupiter.api.Tag;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link PSItemFilterConverter} class.
 */
@Tag("IntegrationTest")
public class PSItemFilterConverterTest extends PSConverterTestBase {

    /**
     * Tests the conversion from a server to a client object as well as a
     * server array of objects to a client array of objects and back.
     */
    public void testConversion() throws Exception {
        IPSItemFilter parent = null;
        IPSItemFilter source = null;

        try {
            parent = createItemFilter("parent", null, 1, 2, 3);
            source = createItemFilter("child", parent, null, 2, 3);

            var target = (PSItemFilter) roundTripConversion(
                    PSItemFilter.class,
                    com.percussion.webservices.system.PSItemFilter.class,
                    source);

            assertEquals(source, target);

            var sourceArray = new PSItemFilter[]{(PSItemFilter) source};
            var targetArray = (PSItemFilter[]) roundTripConversion(
                    PSItemFilter[].class,
                    com.percussion.webservices.system.PSItemFilter[].class,
                    sourceArray);

            assertEquals(sourceArray.length, targetArray.length);
            assertEquals(sourceArray[0], targetArray[0]);
        } finally {
            removeItemFilter(source);
            removeItemFilter(parent);
        }
    }

    /**
     * Test a list of server object conversion to client array, and vice versa.
     */
    @SuppressWarnings("unchecked")
    public void testListToArray() throws Exception {
        IPSItemFilter parent = null;
        IPSItemFilter source = null;

        try {
            parent = createItemFilter("parent", null, 1, 2, 3);
            source = createItemFilter("child", parent, null, 2, 3);

            var sourceList = new ArrayList<IPSItemFilter>();
            sourceList.add(parent);
            sourceList.add(source);

            var targetList = roundTripListConversion(
                    com.percussion.webservices.system.PSItemFilter[].class,
                    sourceList);

            assertEquals(sourceList, targetList);
        } finally {
            removeItemFilter(source);
            removeItemFilter(parent);
        }
    }

    /**
     * Create a filter for testing and save it to the repository.
     */
    private IPSItemFilter createItemFilter(String name, IPSItemFilter parent,
                                           Integer authtype, int ruleCount, int paramCount) throws Exception {
        var service = PSFilterServiceLocator.getFilterService();
        var filter = service.createFilter(name, "description");
        filter.setParentFilter(parent);
        filter.setLegacyAuthtypeId(authtype);

        for (int i = 0; i < ruleCount; i++) {
            var params = new HashMap<String, String>();
            for (int j = 0; j < paramCount; j++)
                params.put(name + "_param_" + i + "." + j,
                        "value_" + i + "." + j);

            filter.addRuleDef(createFilterRule(
                    PSItemFilterRuleDef.TEST_RULE_NAME, params));
        }

        service.saveFilter(filter);

        return filter;
    }

    /**
     * Create a new rule definition for the supplied parameters.
     */
    private IPSItemFilterRuleDef createFilterRule(String rule,
                                                  Map<String, String> params) throws Exception {
        var service = PSFilterServiceLocator.getFilterService();
        return service.createRuleDef(rule, params);
    }

    /**
     * Remove the supplied filter from the repository.
     */
    private void removeItemFilter(IPSItemFilter filter) throws Exception {
        var service = PSFilterServiceLocator.getFilterService();
        if (filter != null)
            service.deleteFilter(filter);
    }
}
