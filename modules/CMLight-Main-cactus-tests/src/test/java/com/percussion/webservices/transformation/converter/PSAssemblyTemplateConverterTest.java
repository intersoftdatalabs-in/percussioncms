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
import com.percussion.services.assembly.IPSAssemblyTemplate;
import com.percussion.services.assembly.IPSTemplateSlot;
import com.percussion.services.assembly.PSAssemblyException;
import com.percussion.services.assembly.PSAssemblyServiceLocator;
import com.percussion.services.assembly.data.PSAssemblyTemplate;
import com.percussion.services.assembly.data.PSTemplateBinding;
import com.percussion.services.assembly.data.PSTemplateSlot;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.utils.guid.IPSGuid;

import com.percussion.webservices.assembly.data.PSAssemblyTemplateWs;
import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.Tag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link PSAssemblyTemplateConverter} class.
 */
@Tag("IntegrationTest")
public class PSAssemblyTemplateConverterTest extends PSConverterTestBase {

    /**
     * Tests the conversion from a server to a client object as well as a server array of objects to a client array of objects and back.
     */
    public void testConversion() throws Exception {
        PSAssemblyTemplateWs source = null;
        try {
            source = createTemplate("name", getNextId(PSTypeEnum.TEMPLATE));
            var target = (PSAssemblyTemplateWs) roundTripConversion(
                    PSAssemblyTemplateWs.class,
                    com.percussion.webservices.assembly.data.PSAssemblyTemplate.class,
                    source);
            assertEquals(source, target);

            var sourceArray = new PSAssemblyTemplateWs[]{source};
            var targetArray = (PSAssemblyTemplateWs[]) roundTripConversion(
                    PSAssemblyTemplateWs[].class,
                    com.percussion.webservices.assembly.data.PSAssemblyTemplate[].class,
                    sourceArray);
            assertEquals(sourceArray.length, targetArray.length);
            assertEquals(sourceArray[0], targetArray[0]);
        } finally {
            if (source != null) {
                var service = PSAssemblyServiceLocator.getAssemblyService();
                var template = source.getTemplate();
                for (var slot : template.getSlots()) {
                    service.deleteSlot(slot.getGUID());
                }
                service.deleteTemplate(template.getGUID());
            }
        }
    }

    /**
     * Test a list of server objects convert to client array, and vice versa.
     */
    @SuppressWarnings("unchecked")
    public void testListToArray() throws Exception {
        var srcList = new ArrayList<PSAssemblyTemplateWs>();
        try {
            srcList.add(createTemplate("testTemplate_1", getNextId(PSTypeEnum.TEMPLATE)));
            srcList.add(createTemplate("testTemplate_1", getNextId(PSTypeEnum.TEMPLATE)));

            var srcList2 = roundTripListConversion(
                    com.percussion.webservices.assembly.data.PSAssemblyTemplate[].class,
                    srcList);

            assertEquals(srcList, srcList2);
        } finally {
            for (var templateWs : srcList) {
                var service = PSAssemblyServiceLocator.getAssemblyService();
                var template = templateWs.getTemplate();
                for (var slot : template.getSlots()) {
                    service.deleteSlot(slot.getGUID());
                }
                service.deleteTemplate(template.getGUID());
            }
        }
    }

    /**
     * Creates a template object with the given name.
     */
    public static PSAssemblyTemplateWs createTemplate(String name, IPSGuid id)
            throws PSAssemblyException {
        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("name cannot be null");
        }
        if (id == null) {
            throw new IllegalArgumentException("id cannot be null");
        }
        var template = new PSAssemblyTemplate();
        template.setName(name);
        template.setGUID(id);
        template.setLabel(name + "_label");
        template.setLocationPrefix("prefix");
        template.setLocationSuffix("suffix");
        template.setAssembler(name + "_assembler");
        template.setAssemblyUrl(name + "_assemblyUrl");
        template.setStyleSheetPath(name + "_stylesheetPath");
        template.setActiveAssemblyType(IPSAssemblyTemplate.AAType.AutoIndex);
        template.setOutputFormat(IPSAssemblyTemplate.OutputFormat.Snippet);
        template.setPublishWhen(IPSAssemblyTemplate.PublishWhen.Never);
        template.setTemplateType(IPSAssemblyTemplate.TemplateType.Local);
        template.setDescription(name + "_description");
        template.setTemplate(name + "template");
        template.setMimeType(name + "mimeType");
        template.setCharset("UTF8");
        template.setGlobalTemplateUsage(IPSAssemblyTemplate.GlobalTemplateUsage.Defined);
        template.setGlobalTemplate(new PSGuid(PSTypeEnum.TEMPLATE, 5555555));

        var service = PSAssemblyServiceLocator.getAssemblyService();
        service.saveTemplate(template);

        String[] slotNames = {"name_1", "name_2", "name_3"};
        for (var slotName : slotNames) {
            var slot = PSTemplateSlotConverterTest.createSlot(slotName, getNextId(PSTypeEnum.SLOT));
            template.addSlot(slot);
        }

        template.addBinding(new PSTemplateBinding(1, "$a", "1"));
        template.addBinding(new PSTemplateBinding(2, "$b", "2"));
        template.addBinding(new PSTemplateBinding(3, "$c", "3"));

        service.saveTemplate(template);

        Map<IPSGuid, String> sites = new HashMap<>();
        sites.put(new PSGuid(PSTypeEnum.SITE, 1), "site_1");
        sites.put(new PSGuid(PSTypeEnum.SITE, 2), "site_2");
        sites.put(new PSGuid(PSTypeEnum.SITE, 3), "site_3");
        sites.put(new PSGuid(PSTypeEnum.SITE, 1000), "site_1000");
        sites.put(new PSGuid(PSTypeEnum.SITE, 1001), "site_1001");
        sites.put(new PSGuid(PSTypeEnum.SITE, 1002), "site_1002");
        sites.put(new PSGuid(PSTypeEnum.SITE, 1003), "site_1003");

        return new PSAssemblyTemplateWs(template, sites);
    }
}
