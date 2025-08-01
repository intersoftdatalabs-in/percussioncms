// REFACTORED: CP-JAVA11
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

import com.percussion.cms.objectstore.PSInvalidContentTypeException;
import com.percussion.cms.objectstore.PSItemDefinition;
import com.percussion.cms.objectstore.PSItemField;
import com.percussion.cms.objectstore.server.PSItemDefManager;
import com.percussion.design.objectstore.PSContentEditorPipe;
import com.percussion.design.objectstore.PSDisplayMapping;
import com.percussion.design.objectstore.PSField;
import com.percussion.design.objectstore.PSUISet;
import com.percussion.utils.testing.IntegrationTest;
import com.percussion.webservices.transformation.impl.PSTransformerFactory;
import org.apache.commons.beanutils.ConversionException;
import org.apache.commons.beanutils.Converter;
import org.apache.commons.lang3.StringUtils;
import org.junit.experimental.categories.Category;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link PSFieldConverter} class.
 */
@Category(IntegrationTest.class)
public class PSFieldConverterTest extends PSConverterTestBase {

    /**
     * Tests the conversion from a server to a client object as well as a
     * server array of objects to a client array of objects and back.
     */
    public void testConversion() throws Exception {
        // Register the item definition used for testing
        PSItemConverterTest.getTestItemDefManager();

        // Create the source object
        var source = createItemField();
        source.setContentType(ms_contentType);

        var target = (PSItemField) roundTripConversion(
                PSItemField.class,
                com.percussion.webservices.content.PSField.class, source);

        // Verify the round-trip object is equal to the source object
        assertEquals(source, target);

        // Create the source array
        var sourceArray = new PSItemField[]{source};

        var targetArray = (PSItemField[]) roundTripConversion(
                PSItemField[].class,
                com.percussion.webservices.content.PSField[].class, sourceArray);

        // Verify the round-trip array is equal to the source array
        assertEquals(sourceArray.length, targetArray.length);
        assertEquals(sourceArray[0], targetArray[0]);
    }

    /**
     * Test a list of server object conversion to client array, and vice versa.
     */
    @SuppressWarnings("unchecked")
    public void testListToArray() throws Exception {
        var source = createItemField();
        source.setContentType(ms_contentType);

        var sourceList = new ArrayList<PSItemField>();
        sourceList.add(source);

        var targetList = roundTripListConversion(
                com.percussion.webservices.content.PSField[].class, sourceList);

        assertEquals(sourceList, targetList);
    }

    @Override
    protected Object roundTripConversion(Class serverType, Class clientType, Object source) {
        if (serverType == null) throw new IllegalArgumentException("serverType cannot be null");
        if (clientType == null) throw new IllegalArgumentException("clientType cannot be null");
        if (source == null) throw new IllegalArgumentException("source cannot be null");
        if (!source.getClass().getName().equals(serverType.getName()))
            throw new IllegalArgumentException("source must be of type serverType");

        var factory = PSTransformerFactory.getInstance();

        // Convert server to client object
        var converter = factory.getConverter(serverType);
        var clientObject = converter.convert(clientType, source);

        // Set content type
        var contentType = ms_contentType;
        if (clientObject instanceof com.percussion.webservices.content.PSField) {
            ((com.percussion.webservices.content.PSField) clientObject).setContentType(contentType);
        } else if (clientObject instanceof com.percussion.webservices.content.PSField[]) {
            for (var field : (com.percussion.webservices.content.PSField[]) clientObject) {
                field.setContentType(contentType);
            }
        }

        // Convert client to server object
        converter = factory.getConverter(clientType);
        var serverObject = converter.convert(serverType, clientObject);
        if (serverObject instanceof PSItemField) {
            ((PSItemField) serverObject).setContentType(contentType);
        } else if (serverObject instanceof PSItemField[]) {
            for (var field : (PSItemField[]) serverObject) {
                field.setContentType(contentType);
            }
        }

        return serverObject;
    }

    @Override
    protected List roundTripListConversion(Class cz, List srcList) throws Exception {
        if (!cz.isArray()) throw new IllegalArgumentException("cz must be an instance of array.");
        if (srcList == null) throw new IllegalArgumentException("srcList must not be null.");

        var factory = PSTransformerFactory.getInstance();

        // Convert from list to array
        var converter = factory.getConverter(cz);
        var array = (Object[]) converter.convert(cz, srcList);

        // Set content type
        if (array instanceof com.percussion.webservices.content.PSField[]) {
            for (var field : (com.percussion.webservices.content.PSField[]) array) {
                field.setContentType(ms_contentType);
            }
        }

        // Convert from array to list
        converter = factory.getConverter(List.class);
        return (List) converter.convert(List.class, array);
    }

    /**
     * Create an item field for testing.
     *
     * @return the new item field, never {@code null}.
     */
    private PSItemField createItemField() throws Exception {
        var fieldName = "sys_title";
        var fieldDef = getFieldDef(ms_contentType, fieldName);
        var uiDef = getUiDef(ms_contentType, fieldName);
        return new PSItemField(fieldDef, uiDef, false);
    }

    /**
     * Get the field definition for the specified name.
     *
     * @param contentType the content type name for which to get the field definition, not blank.
     * @param name        the name for the field to get the definition for, not blank.
     * @return the requested field definition, never {@code null}.
     */
    @SuppressWarnings("unchecked")
    public static PSField getFieldDef(String contentType, String name) {
        if (StringUtils.isBlank(contentType))
            throw new IllegalArgumentException("contentType cannot be null or empty");
        if (StringUtils.isBlank(name))
            throw new IllegalArgumentException("name cannot be null or empty");

        var itemDefMgr = PSItemConverterTest.getTestItemDefManager();

        try {
            var def = itemDefMgr.getItemDef(contentType, PSItemDefManager.COMMUNITY_ANY);
            return def.getFieldByName(name);
        } catch (PSInvalidContentTypeException e) {
            throw new ConversionException("Unregistered content type : " + contentType);
        }
    }

    /**
     * Get the UI definition for the specified field name.
     *
     * @param contentType the content type name for which to get the UI definition, not blank.
     * @param name        the name of the field for which to get the UI definition, not blank.
     * @return the first UI definition found for the specified field name, may be {@code null} if none was found.
     * @throws Exception for any error.
     */
    public static PSUISet getUiDef(String contentType, String name) throws Exception {
        if (StringUtils.isBlank(contentType))
            throw new IllegalArgumentException("contentType cannot be null or empty");
        if (StringUtils.isBlank(name))
            throw new IllegalArgumentException("name cannot be null or empty");

        var itemDefMgr = PSItemConverterTest.getTestItemDefManager();

        try {
            var def = itemDefMgr.getItemDef(contentType, PSItemDefManager.COMMUNITY_ANY);
            var pipe = (PSContentEditorPipe) def.getContentEditor().getPipe();
            var mapping = pipe.getMapper().getUIDefinition().getMapping(name);
            if (mapping == null)
                throw new ConversionException(
                        "Unknown field name " + name +
                                " in item definition for content type " + contentType);

            return mapping.getUISet();
        } catch (PSInvalidContentTypeException e) {
            throw new ConversionException("Unregistered content type : " + contentType);
        }
    }

    /**
     * The content type used for testing.
     */
    private static final String ms_contentType = "Press Release";
}
