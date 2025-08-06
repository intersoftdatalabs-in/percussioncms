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

import com.percussion.cms.handlers.PSRelationshipCommandHandler;
import com.percussion.cms.objectstore.*;
import com.percussion.cms.objectstore.server.PSItemDefManager;
import com.percussion.design.objectstore.PSLocator;
import com.percussion.services.assembly.IPSAssemblyTemplate;
import com.percussion.services.assembly.IPSTemplateSlot;
import com.percussion.services.assembly.data.PSAssemblyTemplate;
import com.percussion.services.assembly.data.PSTemplateSlot;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.guidmgr.data.PSLegacyGuid;
import com.percussion.utils.testing.IntegrationTest;
import com.percussion.webservices.content.PSChildEntry;
import com.percussion.webservices.content.PSItem;
import com.percussion.xml.PSXmlDocumentBuilder;
import org.apache.commons.lang3.StringUtils;
import org.junit.experimental.categories.Category;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link PSItemConverter} class.
 */
@Category(IntegrationTest.class)
public class PSItemConverterTest extends PSConverterTestBase {
    /**
     * Tests the conversion from a server to a client item as well as a
     * server array of items to a client array of items and back.
     */
    public void testItemConversion() throws Exception {
        var source = createItem();
        addRelatedItems(source);

        var target = (PSCoreItem) roundTripConversion(
                PSCoreItem.class, PSItem.class, source);

        fixUnexposedFields(source, target);
        assertTrue(source.equals(target));

        var sourceArray = new PSCoreItem[]{source};
        var targetArray = (PSCoreItem[]) roundTripConversion(
                PSCoreItem[].class, PSItem[].class, sourceArray);

        assertEquals(sourceArray.length, targetArray.length);
        for (int i = 0; i < sourceArray.length; i++)
            fixUnexposedFields(sourceArray[i], targetArray[i]);
        assertTrue(sourceArray[0].equals(targetArray[0]));
    }

    /**
     * Test a list of server item conversion to client array, and vice versa.
     */
    @SuppressWarnings("unchecked")
    public void testItemListToArray() throws Exception {
        var source = createItem();
        var sourceList = new ArrayList<PSCoreItem>();
        sourceList.add(source);

        var targetList = roundTripListConversion(
                PSItem[].class, sourceList);

        for (int i = 0; i < sourceList.size(); i++)
            fixUnexposedFields(sourceList.get(i), targetList.get(i));
        assertEquals(sourceList, targetList);
    }

    /**
     * Tests the conversion from a server to a client child item as well as a
     * server array of child items to a client array and back.
     */
    @SuppressWarnings("unchecked")
    public void testChildItemConversion() throws Exception {
        var child = createItemChild();
        var srcList = new ArrayList<PSItemChildEntry>();
        var entries = child.getAllEntries();
        while (entries.hasNext()) {
            var src = (PSItemChildEntry) entries.next();
            var target = (PSItemChildEntry) roundTripConversion(
                    PSItemChildEntry.class, PSChildEntry.class, src);
            assertEquals(src, target);
            srcList.add(src);
        }

        var tgtList = roundTripListConversion(
                PSChildEntry[].class, srcList);

        assertEquals(srcList, tgtList);
    }

    /**
     * Test a list of server child item conversion to client array, and back.
     */
    @SuppressWarnings("unchecked")
    public void testChildItemListToArray() throws Exception {
        var source = createItem();
        var sourceList = new ArrayList<PSCoreItem>();
        sourceList.add(source);

        var targetList = roundTripListConversion(
                PSItem[].class, sourceList);

        for (int i = 0; i < sourceList.size(); i++)
            fixUnexposedFields(sourceList.get(i), targetList.get(i));
        assertEquals(sourceList, targetList);
    }

    /**
     * Create a server item for testing.
     */
    private PSCoreItem createItem() throws Exception {
        int contentId = 1001;
        int revision = 1;

        var item = new PSCoreItem(m_def);
        item.setContentId(contentId);
        item.setRevision(revision);
        item.setCurrentRevision(revision);
        item.setEditRevision(revision);
        item.setRequestedRevision(revision);
        item.setDataLocale(Locale.forLanguageTag("de-DE"));
        item.setSystemLocale(Locale.forLanguageTag("de-CH"));
        item.setCheckedOutByName("admin1");

        var childEntries = createItemChild().getAllEntries();
        var child = item.getChildByName("child");
        while (childEntries.hasNext())
            child.addEntry(childEntries.next());

        var folderPaths = new ArrayList<String>();
        folderPaths.add("//sites");
        folderPaths.add("//folders/test");
        item.setFolderPaths(folderPaths);

        return item;
    }

    /**
     * Create an item child with multiple child entries.
     */
    private PSItemChild createItemChild() throws Exception {
        var item = new PSCoreItem(m_def);
        var children = item.getAllChildren();
        if (!children.hasNext())
            throw new IllegalStateException("No children defined in item def");

        var itemChild = (PSItemChild) children.next();

        for (int i = 0; i < 2; i++) {
            var entry = itemChild.createAndAddChildEntry();
            entry.setAction(PSItemChildEntry.CHILD_ACTION_INSERT);
            entry.setChildRowId(i);
            entry.setGUID(new PSLegacyGuid(item.getContentTypeId(),
                    itemChild.getChildId(), i));
            var fields = entry.getAllFields();
            setFieldValues(i, fields);
        }

        return itemChild;
    }

    /**
     * Adds test related content to the supplied item.
     */
    private void addRelatedItems(PSCoreItem item) throws Exception {
        int count = 3;
        var relatedItems = new ArrayList<PSCoreItem>();
        for (int i = 0; i < count; i++)
            relatedItems.add(createItem());

        var owner = new PSLocator(item.getContentId(),
                item.getCurrentRevision());

        IPSTemplateSlot slot = new PSTemplateSlot();
        slot.setGUID(new PSGuid(PSTypeEnum.SLOT, 103));
        slot.setName("slot");
        slot.setRelationshipName("ActiveAssembly");

        IPSAssemblyTemplate template = new PSAssemblyTemplate();
        template.setGUID(new PSGuid(PSTypeEnum.TEMPLATE, 301));
        template.setName("template");

        PSRelationshipCommandHandler.reloadConfigs();
        long rid = 1;
        var relationships = new ArrayList<PSAaRelationship>();
        for (var relatedItem : relatedItems) {
            var dependent = new PSLocator(relatedItem.getContentId(),
                    relatedItem.getCurrentRevision());
            var relationship = new PSAaRelationship(owner, dependent,
                    slot, template);
            relationship.setGUID(new PSGuid(PSTypeEnum.RELATIONSHIP, rid++));
            relationships.add(relationship);
        }

        item.setRelatedItems(createRelatedItems(relatedItems, relationships));
    }

    /**
     * Create test related items for the supplied parameters.
     */
    private Map<String, PSItemRelatedItem> createRelatedItems(
            List<PSCoreItem> items, List<PSAaRelationship> relationships)
            throws Exception {
        var relatedItems = new HashMap<String, PSItemRelatedItem>();
        for (int i = 0; i < items.size(); i++) {
            var item = items.get(i);
            var relationship = relationships.get(i);

            var relatedItem = new PSItemRelatedItem();
            relatedItem.setAction("ignore");
            relatedItem.setDependentId(item.getContentId());
            relatedItem.setRelatedItemData(item.toXml(
                    PSXmlDocumentBuilder.createXmlDocument()));
            relatedItem.setRelatedType(relationship.getConfig().getName());
            relatedItem.setRelationshipId(relationship.getId());
            relatedItem.setRelationship(relationship);

            relatedItems.put(Integer.toString((int) relationship.getId()), relatedItem);
        }
        return relatedItems;
    }

    /**
     * Create and set values for the supplied fields. Handles only text, numeric, and date for now.
     */
    private void setFieldValues(int index, Iterator fields) {
        while (fields.hasNext()) {
            var field = (PSItemField) fields.next();
            int type = field.getItemFieldMeta().getBackendDataType();
            switch (type) {
                case PSItemFieldMeta.DATATYPE_TEXT:
                    field.addValue(new PSTextValue("test" + index));
                    break;
                case PSItemFieldMeta.DATATYPE_NUMERIC:
                    field.addValue(new PSTextValue(String.valueOf(index)));
                    break;
                case PSItemFieldMeta.DATATYPE_DATE:
                    var calendar = Calendar.getInstance();
                    calendar.setTime(new Date());
                    calendar.set(Calendar.DAY_OF_YEAR, 1 + index);
                    field.addValue(new PSDateValue(calendar.getTime()));
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Get the item definition manager used for testing. This will have the test item definition registered.
     */
    public static PSItemDefManager getTestItemDefManager() {
        var itemDefMgr = PSItemDefManager.getInstance();
        itemDefMgr.registerDef(m_def, m_cmsObject);
        return itemDefMgr;
    }

    /**
     * Load the item definition from the specified file.
     */
    public static PSItemDefinition loadItemDefinition(String fileName)
            throws Exception {
        try (InputStream in = PSItemConverterTest.class.getResourceAsStream(
                StringUtils.isBlank(fileName) ? "itemDefinition.xml" : fileName)) {
            Document doc = PSXmlDocumentBuilder.createXmlDocument(in, false);
            return new PSItemDefinition(doc.getDocumentElement());
        }
    }

    /**
     * Load the icms object from the specified file.
     */
    public static PSCmsObject loadCmsObject(String fileName)
            throws Exception {
        try (InputStream in = PSItemConverterTest.class.getResourceAsStream(
                StringUtils.isBlank(fileName) ? "cmsObject.xml" : fileName)) {
            Document doc = PSXmlDocumentBuilder.createXmlDocument(in, false);
            return new PSCmsObject(doc.getDocumentElement());
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        getTestItemDefManager();
    }

    /**
     * This fixes up fields which are not exposed with the webservice item.
     */
    private void fixUnexposedFields(PSCoreItem source, PSCoreItem target) {
        target.setCurrentRevision(source.getCurrentRevision());
        target.setEditRevision(source.getEditRevision());
        target.setRequestedRevision(source.getRequestedRevision());
        var relatedItems = target.getAllRelatedItems();
        while (relatedItems.hasNext()) {
            var relatedItem = relatedItems.next();
            var data = relatedItem.getRelatedItemData();
            data.setAttribute("currentRevision", "" + source.getCurrentRevision());
            data.setAttribute("editRevision", "" + source.getEditRevision());
            data.setAttribute("requestedRevision", "" + source.getRequestedRevision());
            data.setAttribute("revisionCount", "" + source.getRevisionCount());
        }
    }

    private static PSItemDefinition m_def = null;
    private static PSCmsObject m_cmsObject = null;

    static {
        try {
            m_def = loadItemDefinition(null);
            m_cmsObject = loadCmsObject(null);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Test initialization failed because of: " +
                            e.getLocalizedMessage());
        }
    }
}
