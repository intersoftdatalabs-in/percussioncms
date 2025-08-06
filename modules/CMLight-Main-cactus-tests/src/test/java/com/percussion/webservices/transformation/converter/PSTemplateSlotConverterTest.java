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

import com.percussion.services.assembly.IPSAssemblyService;
import com.percussion.services.assembly.IPSTemplateSlot;
import com.percussion.services.assembly.PSAssemblyException;
import com.percussion.services.assembly.PSAssemblyServiceLocator;
import com.percussion.services.assembly.data.PSTemplateSlot;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.utils.guid.IPSGuid;

import com.percussion.utils.types.PSPair;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Tag;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link PSTemplateSlotConverter} class.
 */
@Tag("IntegrationTest")
public class PSTemplateSlotConverterTest extends PSConverterTestBase {

    /**
     * Tests the conversion from a server to a client object as well as a
     * server array of objects to a client array of objects and back.
     */
    public void testConversion() throws Exception {
        PSTemplateSlot source = null;
        try {
            source = createSlot("name", getNextId(PSTypeEnum.SLOT));

            var target = (PSTemplateSlot) roundTripConversion(
                    PSTemplateSlot.class,
                    com.percussion.webservices.assembly.data.PSTemplateSlot.class,
                    source);

            assertEquals(source, target);

            var sourceArray = new PSTemplateSlot[]{source};
            var targetArray = (PSTemplateSlot[]) roundTripConversion(
                    PSTemplateSlot[].class,
                    com.percussion.webservices.assembly.data.PSTemplateSlot[].class,
                    sourceArray);

            assertEquals(sourceArray.length, targetArray.length);
            assertEquals(sourceArray[0], targetArray[0]);
        } finally {
            if (source != null) {
                var service = PSAssemblyServiceLocator.getAssemblyService();
                service.deleteSlot(source.getGUID());
            }
        }
    }

    /**
     * Test a list of server object convert to client array, and vice versa.
     */
    @SuppressWarnings("unchecked")
    public void testListToArray() throws Exception {
        var srcList = new ArrayList<PSTemplateSlot>();
        try {
            srcList.add(createSlot("slot_1", getNextId(PSTypeEnum.SLOT)));
            srcList.add(createSlot("slot_2", getNextId(PSTypeEnum.SLOT)));

            var srcList2 = roundTripListConversion(
                    com.percussion.webservices.assembly.data.PSTemplateSlot[].class,
                    srcList);

            assertEquals(srcList, srcList2);
        } finally {
            for (var slot : srcList) {
                var service = PSAssemblyServiceLocator.getAssemblyService();
                service.deleteSlot(slot.getGUID());
            }
        }
    }

    /**
     * Create a test slot for the specified name.
     *
     * @param name the slot name, not {@code null} or empty.
     * @param id   the slot id, not {@code null}.
     * @return the test slot, never {@code null}.
     * @throws PSAssemblyException if we cannot save the created template.
     */
    public static PSTemplateSlot createSlot(String name, IPSGuid id)
            throws PSAssemblyException {
        if (StringUtils.isBlank(name))
            throw new IllegalArgumentException("name cannot be null");
        if (id == null)
            throw new IllegalArgumentException("id cannot be null");

        var slot = new PSTemplateSlot();
        slot.setName(name);
        slot.setGUID(id);
        slot.setLabel(name + "_label");
        slot.setDescription(name + "_description");
        slot.setFinderName(name + "_findeName");
        slot.setRelationshipName(name + "_relationshipName");
        slot.setSlottype(IPSTemplateSlot.SlotType.INLINE);
        slot.setSystemSlot(true);

        Map<String, String> finderParams = new HashMap<>();
        finderParams.put("param_1", "value_1");
        finderParams.put("param_2", "value_2");
        finderParams.put("param_3", "value_3");
        slot.setFinderArguments(finderParams);

        Collection<PSPair<IPSGuid, IPSGuid>> slotAssociations = new ArrayList<>();
        var pair1 = new PSPair<>(new PSGuid(PSTypeEnum.NODEDEF, 1000), new PSGuid(PSTypeEnum.TEMPLATE, 1001));
        slotAssociations.add(pair1);
        var pair2 = new PSPair<>(new PSGuid(PSTypeEnum.NODEDEF, 1002), new PSGuid(PSTypeEnum.TEMPLATE, 1003));
        slotAssociations.add(pair2);
        slot.setSlotAssociations(slotAssociations);

        var service = PSAssemblyServiceLocator.getAssemblyService();
        service.saveSlot(slot);

        return slot;
    }
}
