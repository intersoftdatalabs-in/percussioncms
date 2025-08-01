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
package com.percussion.services.assembly.data;

import com.percussion.cms.objectstore.PSContentTypeTemplate;
import com.percussion.cms.objectstore.server.PSContentTypeVariantsMgr;
import com.percussion.error.PSExceptionUtils;
import com.percussion.services.assembly.IPSAssemblyService;
import com.percussion.services.assembly.IPSAssemblyTemplate;
import com.percussion.services.assembly.IPSTemplateBinding;
import com.percussion.services.assembly.IPSTemplateSlot;
import com.percussion.services.assembly.PSAssemblyException;
import com.percussion.services.assembly.PSAssemblyServiceLocator;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.testing.IntegrationTest;
import com.percussion.utils.timing.PSStopwatch;
import com.percussion.utils.types.PSPair;
import org.apache.commons.collections.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.*;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import static com.percussion.extension.IPSExtension.LEGACY_ASSEMBLER;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test the assembly template object for correct behavior
 * 
 * @author dougrand
 */
@Tag("IntegrationTest")
public class PSAssemblyTemplateTest {

    private static final Logger log = LogManager.getLogger(PSAssemblyTemplateTest.class);

    private static final String MYTESTVARIANT = "mytestvariant";
    private static final String MYTESTVARIANT0 = MYTESTVARIANT + "_0";
    private static int ms_count = 0;
    private static final String TEST_SLOT = "TestSlot";

    @BeforeEach
    public void setUp() {
        try {
            cleanup();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @AfterEach
    public void tearDown() {
        setUp();
    }

    public static void cleanup() throws Exception {
        var service = PSAssemblyServiceLocator.getAssemblyService();
        var templates = service.findAllTemplates();
        for (var t : templates) {
            if (t.getName().startsWith(MYTESTVARIANT) || t.getName().startsWith("____test1____")) {
                try {
                    service.deleteTemplate(t.getGUID());
                } catch (Exception e) {
                    log.error(PSExceptionUtils.getMessageForLog(e));
                    log.debug(PSExceptionUtils.getDebugMessageForLog(e));
                }
            }
        }
        while (true) {
            try {
                var slot = service.findSlotByName(TEST_SLOT);
                service.deleteSlot(slot.getGUID());
            } catch (Exception e) {
                break;
            }
        }
        ms_count = 0;
    }

    private void setupBindingData(IPSAssemblyTemplate var) {
        var bindings = new ArrayList<PSTemplateBinding>();
        bindings.add(new PSTemplateBinding(1, "x", "y * z"));
        bindings.add(new PSTemplateBinding(2, "w", "x  / 3"));
        var.setBindings(bindings);
    }

    private void setupTemplateData(IPSAssemblyTemplate var) {
        var name = MYTESTVARIANT + "_" + ms_count++;
        var.setActiveAssemblyType(IPSAssemblyTemplate.AAType.NonHtml);
        var.setAssembler(LEGACY_ASSEMBLER);
        var.setAssemblyUrl("myassemblyurl");
        var.setDescription("Test template");
        var.setLocationPrefix("prefix");
        var.setLocationSuffix("suffix");
        var.setName(name);
        var.setLabel(name);
        var.setTemplateType(IPSAssemblyTemplate.TemplateType.Shared);
        var.setOutputFormat(IPSAssemblyTemplate.OutputFormat.Page);
        var.setPublishWhen(IPSAssemblyTemplate.PublishWhen.Always);
        var.setGlobalTemplateUsage(IPSAssemblyTemplate.GlobalTemplateUsage.Defined);
        var.setStyleSheetPath("My template");
        setupSlotData(var, 2);
    }

    private void setupSlotData(IPSAssemblyTemplate var, int count) {
        var service = PSAssemblyServiceLocator.getAssemblyService();
        var slots = service.findSlotsByName("%");
        for (int i = 0; i < slots.size() && i < count; i++) {
            var.addSlot(slots.get(i));
        }
    }

    @Test
    public void testModifySlotsInTemplate() throws Exception {
        var svc = PSAssemblyServiceLocator.getAssemblyService();
        var t = svc.loadTemplate(new PSGuid(PSTypeEnum.TEMPLATE, 505), true);
        var tmpStr = t.toXML();
        var bver = new HashMap<Long, Integer>();
        for (var binding : t.getBindings()) {
            var rbinding = (PSTemplateBinding) binding;
            bver.put(rbinding.getBindingId(), rbinding.getVersion());
        }
        var slots = PSAssemblyTemplate.getSlotIdsFromTemplate(tmpStr);
        for (var guid : slots) {
            var ts = svc.loadSlot(guid);
            t.removeSlot(ts);
        }
        var newSlots = new HashSet<IPSGuid>();
        var slotNames = List.of("rffAutoCalendarEvents", "rffAutoPressReleases2004", "rffAutoPressReleases2005");
        var slotList = svc.findSlotsByNames(slotNames);
        for (var s : slotList) newSlots.add(s.getGUID());
        var newTmpStr = PSAssemblyTemplate.replaceSlotIdsFromTemplate(tmpStr, newSlots);
        var modifiedSlots = PSAssemblyTemplate.getSlotIdsFromTemplate(newTmpStr);
        assertTrue(CollectionUtils.isEqualCollection(newSlots, modifiedSlots));
        ((PSAssemblyTemplate) t).setVersion(null);
        t.fromXML(newTmpStr);
        var s1 = t.getSlots();
        for (var s : s1) {
            assertTrue(modifiedSlots.contains(s.getGUID()));
        }
        assertTrue(true);
    }

    private static final String LOREM_IPSUM = "Lorem ipsum dolor sit amet, "
            + "consectetur adipisicing elit, sed do eiusmod tempor incididunt ut "
            + "labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud "
            + "exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. "
            + "Duis aute irure dolor in reprehenderit in voluptate velit esse cillum "
            + "dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat "
            + "non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.";

    @Test
    public void testLargeTemplate() throws Exception {
        var service = PSAssemblyServiceLocator.getAssemblyService();
        var template = service.createTemplate();
        template.setName("test_big_template_source");
        var text = new StringBuilder(65000);
        while (text.length() < 65000) {
            text.append(LOREM_IPSUM).append(' ');
        }
        template.setTemplate(text.toString());
        service.saveTemplate(template);
        var restore = service.loadTemplate(template.getGUID(), true);
        assertNotNull(restore);
        assertEquals(template, restore);
        service.deleteTemplate(template.getGUID());
    }

    @Test
    public void testFinders() throws Exception {
        final var service = PSAssemblyServiceLocator.getAssemblyService();
        var loadedById = new FutureTask<IPSAssemblyTemplate>(() -> service.loadUnmodifiableTemplate("526"));
        loadedById.run();
        var template = service.findTemplateByName("rffSnLink");
        assertNotNull(template);
        assertTrue(template == loadedById.get());
        var slots = service.findSlotsByName("rffHome%");
        assertNotNull(slots);
        assertTrue(slots.size() > 1);
        var nlist = List.of("rffAutoCalendarEvents", "rffAutoPressReleases2007");
        slots = service.findSlotsByNames(nlist);
        assertNotNull(slots);
        assertEquals(2, slots.size());
        Collection<IPSAssemblyTemplate> templates;
        String templateName = "rffSn%";
        templates = service.findTemplates(templateName, null, null, null, null, null, null);
        assertNotNull(templates);
        assertTrue(templates.size() > 0);
        int count = templates.size();
        templates = service.findTemplates("RFFSN%", null, null, null, null, null, null);
        assertNotNull(templates);
        assertTrue(templates.size() > 0);
        int icount = templates.size();
        assertEquals(count, icount);
        String contentType = "rffgeneric";
        templates = service.findTemplates(templateName, contentType, null, null, null, null, null);
        assertNotNull(templates);
        assertTrue(templates.size() > 0);
        count = templates.size();
        var outputFormats = new HashSet<IPSAssemblyTemplate.OutputFormat>();
        outputFormats.add(IPSAssemblyTemplate.OutputFormat.Page);
        templates = service.findTemplates(templateName, contentType, outputFormats, null, null, null, null);
        assertNotNull(templates);
        assertEquals(0, templates.size());
        outputFormats.add(IPSAssemblyTemplate.OutputFormat.Snippet);
        templates = service.findTemplates(templateName, contentType, outputFormats, null, null, null, null);
        assertNotNull(templates);
        assertEquals(count, templates.size());
        templates = service.findTemplates(null, null, null, null, null, false, null);
        assertNotNull(templates);
        assertTrue(templates.size() > 0);
        templates = service.findTemplates(null, null, null, null, true, false, null);
        assertNotNull(templates);
        assertTrue(templates.size() > 0);
        templates = service.findTemplates(null, null, null, null, false, false, null);
        assertNotNull(templates);
        assertTrue(templates.size() > 0);
        templates = service.findAllGlobalTemplates();
        assertNotNull(templates);
        assertTrue(templates.size() > 0);
        templates = service.findAllTemplates();
        assertNotNull(templates);
        assertTrue(templates.size() > 0);
        var ids = List.of(new PSGuid(PSTypeEnum.SLOT, 103), new PSGuid(PSTypeEnum.SLOT, 104), new PSGuid(PSTypeEnum.SLOT, 105));
        slots = service.loadSlots(ids);
    }

    @Test
    public void testDuplicateName() throws Exception {
        var service = PSAssemblyServiceLocator.getAssemblyService();
        var name = "____test1____";
        var templates = service.findTemplates(name, null, null, null, false, false, null);
        for (var t : templates) {
            service.deleteTemplate(t.getGUID());
        }
        var x = service.createTemplate();
        var y = service.createTemplate();
        x.setName(name);
        x.setLabel(name);
        y.setName(name);
        y.setLabel(name);
        service.saveTemplate(x);
        service.saveTemplate(y);
        try {
            service.findTemplateByName(name);
            fail("No exception where one expected");
        } catch (PSAssemblyException e) {
            // OK
        } catch (Exception e) {
            fail("Wrong exception found");
        }
    }

    @Test
    public void testTemplateCreation() throws Exception {
        var service = PSAssemblyServiceLocator.getAssemblyService();
        var var = service.loadTemplate(new PSGuid(PSTypeEnum.TEMPLATE, 516), true);
        var watch1 = new PSStopwatch();
        var watch2 = new PSStopwatch();
        watch1.start();
        var = service.loadTemplate(new PSGuid(PSTypeEnum.TEMPLATE, 517), true);
        watch1.stop();
        watch2.start();
        var second = service.loadTemplate(new PSGuid(PSTypeEnum.TEMPLATE, 517), true);
        watch2.stop();
        System.out.println("First load " + watch1);
        System.out.println("Second load " + watch2);
        var = service.createTemplate();
        var.setActiveAssemblyType(IPSAssemblyTemplate.AAType.Normal);
        var.setAssembler("velocity");
        setupTemplateData(var);
        setupBindingData(var);
        service.saveTemplate(var);
        second = service.loadTemplate(var.getGUID(), true);
        assertEquals(var, second);
    }

    @Test
    public void testBindingModification() throws Exception {
        var service = PSAssemblyServiceLocator.getAssemblyService();
        var var = service.createTemplate();
        setupTemplateData(var);
        service.saveTemplate(var);
        var = service.loadTemplate(var.getGUID(), true);
        setupBindingData(var);
        service.saveTemplate(var);
        var = service.loadTemplate(var.getGUID(), true);
        var.setBindings(new ArrayList<>());
        setupBindingData(var);
        service.saveTemplate(var);
    }

    @Test
    public void fixme_testTemplateSerialization() throws Exception {
        var service = PSAssemblyServiceLocator.getAssemblyService();
        var var = service.findTemplateByName(MYTESTVARIANT0);
        var ser = var.toXML();
        var blank = new PSAssemblyTemplate();
        try {
            blank.fromXML(ser);
        } catch (Exception e) {
            System.out.println("Error occurred during de-serialization");
        }
        assertEquals(var, blank);
        assertEquals(var.hashCode(), blank.hashCode());
    }

    @Test
    public void fixme_testSlotSerialization() throws Exception {
        var service = PSAssemblyServiceLocator.getAssemblyService();
        var var = service.findTemplateByName(MYTESTVARIANT0);
        var s = var.getSlots().iterator().next();
        var ser = s.toXML();
        var newslot = service.createSlot();
        newslot.fromXML(ser);
        assertEquals(s, newslot);
    }

    @Test
    public void fixme_testModifySlotAssociations() throws Exception {
        var service = PSAssemblyServiceLocator.getAssemblyService();
        var cachedTemplate = service.findTemplateByName(MYTESTVARIANT0);
        var nonCacheTemplate = service.loadTemplate(cachedTemplate.getGUID(), true);
        var s = nonCacheTemplate.getSlots().iterator().next();
        var news = new ArrayList<PSPair<IPSGuid, IPSGuid>>();
        s.setSlotAssociations(news);
        service.saveTemplate(nonCacheTemplate);
    }

    @Test
    public void fixme_testDeSerializeAndLoadTemplate() throws Exception {
        var service = PSAssemblyServiceLocator.getAssemblyService();
        var cachedTemplate = service.findTemplateByName(MYTESTVARIANT0);
        var var = service.loadTemplate(cachedTemplate.getGUID(), true);
        var ser = var.toXML();
        Integer ver = ((PSAssemblyTemplate) var).getVersion();
        var blank = new PSAssemblyTemplate();
        blank.fromXML(ser);
        var bver = new HashMap<Long, Integer>();
        for (var binding : var.getBindings()) {
            var rbinding = (PSTemplateBinding) binding;
            bver.put(rbinding.getBindingId(), rbinding.getVersion());
        }
        ((PSAssemblyTemplate) var).setVersion(null);
        var.fromXML(ser);
        ((PSAssemblyTemplate) var).setVersion(null);
        ((PSAssemblyTemplate) var).setVersion(ver);
        for (var binding : var.getBindings()) {
            var rbinding = (PSTemplateBinding) binding;
            rbinding.setVersion(bver.get(rbinding.getBindingId()));
        }
        var.setAssembler("unknownAssembler");
        service.saveTemplate(var);
    }

    @Test
    public void fixme_testRemoveTemplate() throws Exception {
        var service = PSAssemblyServiceLocator.getAssemblyService();
        var var = service.findTemplateByName(MYTESTVARIANT0);
        var = service.findTemplate(var.getGUID());
        assertNotNull(var);
        service.deleteTemplate(var.getGUID());
        var = service.findTemplate(var.getGUID());
        assertNull(var);
    }

    @Test
    public void testFindByContentType() throws Exception {
        var service = PSAssemblyServiceLocator.getAssemblyService();
        var templates = service.findTemplatesByContentType(new PSGuid(PSTypeEnum.NODEDEF, 311));
        assertTrue(templates.size() > 0);
        var template = templates.get(0);
        var template2 = (PSAssemblyTemplate) template.clone();
        assertEquals(template, template2);
    }

    @Test
    public void testTemplateCheckPerformanceMetrics() throws Exception {
        var service = PSAssemblyServiceLocator.getAssemblyService();
        var fact = (SessionFactory) PSAssemblyServiceLocator.getBean("sys_sessionFactory");
        fact.getCache().evictEntityRegion(PSAssemblyTemplate.class);
        fact.getCache().evictEntityRegion(PSTemplateBinding.class);
        fact.getStatistics().setStatisticsEnabled(true);
        var watch = new PSStopwatch();
        watch.start();
        for (int i = 501; i <= 510; i++) {
            service.loadTemplate(new PSGuid(PSTypeEnum.TEMPLATE, i), false);
        }
        watch.stop();
        System.out.println("Loading 10 fresh: " + watch);
        System.out.println("2nd level cache stats: " + fact.getStatistics().getSecondLevelCacheStatistics("object"));
        watch.start();
        for (int i = 501; i <= 510; i++) {
            service.loadTemplate(new PSGuid(PSTypeEnum.TEMPLATE, i), false);
        }
        watch.stop();
        System.out.println("Loading 10 from cache: " + watch);
        System.out.println("2nd level cache stats: " + fact.getStatistics().getSecondLevelCacheStatistics("object"));
    }

    @Test
    public void testRemoveTestSlots() {
        var service = PSAssemblyServiceLocator.getAssemblyService();
        while (true) {
            try {
                var slot = service.findSlotByName(TEST_SLOT);
                service.deleteSlot(slot.getGUID());
            } catch (PSAssemblyException e) {
                break;
            }
        }
    }

    @SuppressWarnings({"deprecation", "unchecked"})
    @Test
    public void testContentTypeVariantsMgr() throws Exception {
        var set = PSContentTypeVariantsMgr.getAllContentTypeVariants(null);
        assertTrue(set.size() > 0);
        var var = set.getContentVariantById(505);
        var slots = var.getVariantSlots();
        assertNotNull(slots.getLocator());
        var iter = slots.iterator();
        while (iter.hasNext()) {
            var slot = iter.next();
            assertNotNull(slot.getLocator());
        }
    }

    @Test
    public void testFindBySlot() throws Exception {
        var service = PSAssemblyServiceLocator.getAssemblyService();
        var slot = service.loadSlot(new PSGuid(PSTypeEnum.SLOT, 510));
        var templates = service.findTemplatesBySlot(slot);
        assertTrue(templates.size() > 0);
    }

    @Test
    public void testGetTemplatesByType() throws Exception {
        var service = PSAssemblyServiceLocator.getAssemblyService();
        assertNotNull(service.findTemplateByNameAndType("rffPgEiGeneric", new PSGuid(PSTypeEnum.NODEDEF, 310)));
        try {
            service.findTemplateByNameAndType("NavImageLink", new PSGuid(PSTypeEnum.NODEDEF, 323));
        } catch (Exception e) {
            // Normal case
        }
    }

    @Test
    public void testIsVariant() {
        final var template = new PSAssemblyTemplate();
        assertTrue(template.isVariant());
        template.setAssembler("assembler");
        assertEquals("assembler", template.getAssembler());
        assertFalse(template.isVariant());
    }
}
