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
package com.percussion.services.sitemgr.data;

import com.percussion.services.guidmgr.data.PSGuid;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test for the {@link PSLocationScheme} object.
 */
@Tag("IntegrationTest")
public class PSLocationSchemeTest {

    /**
     * Test equals and hashCode.
     */
    @Test
    void testEquals() throws Exception {
        // Testing with no parameters
        var scheme1 = createScheme(0);
        var scheme2 = new PSLocationScheme();
        assertNotEquals(scheme1, scheme2);
        scheme2 = (PSLocationScheme) scheme1.clone();
        assertEquals(scheme1, scheme2);
        assertEquals(scheme1.hashCode(), scheme2.hashCode());

        // Testing with one parameter
        scheme1.setParameter("name", "type", "value");
        scheme2 = (PSLocationScheme) scheme1.clone();
        assertEquals(scheme1, scheme2);
        assertEquals(scheme1.hashCode(), scheme2.hashCode());
        assertEquals(1, scheme2.getParameterNames().size());

        scheme2.setParameter("name", "type", "value");
        assertEquals(1, scheme2.getParameterNames().size());

        // Testing with multiple parameters
        scheme1.setParameter("name2", "type2", "value2");
        scheme2 = (PSLocationScheme) scheme1.clone();
        assertEquals(scheme1, scheme2);
        assertEquals(scheme1.hashCode(), scheme2.hashCode());
        assertEquals(2, scheme2.getParameterNames().size());

        scheme2.setParameter("name3", "type3", "value3");
        assertNotEquals(scheme1, scheme2);
        assertEquals(3, scheme2.getParameterNames().size());

        scheme2.setParameter("name", "type", "value");
        assertEquals(3, scheme2.getParameterNames().size());
    }

    /**
     * Test the XML serialization.
     */
    @Test
    void testXml() throws Exception {
        // Testing with no parameters
        var scheme1 = createScheme(0);

        var str = scheme1.toXML();
        var scheme2 = new PSLocationScheme();
        assertNotEquals(scheme1, scheme2);
        scheme2.fromXML(str);
        compareSchemes(scheme1, scheme2);

        // Testing with multiple parameters
        scheme1 = createScheme(2);
        scheme2 = new PSLocationScheme();
        assertNotEquals(scheme1, scheme2);
        str = scheme1.toXML();
        scheme2.fromXML(str);
        compareSchemes(scheme1, scheme2);
    }

    /**
     * Creates a location scheme with dummy values.
     *
     * @param params The number of location scheme parameters to add to the
     *               newly created scheme.
     * @return a new {@link PSLocationScheme} object initialized with dummy values.
     */
    private PSLocationScheme createScheme(int params) {
        var scheme = new PSLocationScheme();
        scheme.setContentTypeId(311L);
        scheme.setContextId(new PSGuid("0-100-501"));
        scheme.setDescription("This is a test description");
        scheme.setGenerator("Java/com/percussion/extension/general/test");
        scheme.setGUID(new PSGuid("0-113-1"));
        scheme.setName("scheme1");
        scheme.setTemplateId(312L);
        scheme.setVersion(0);

        for (int i = 0; i < params; i++) {
            scheme.addParameter("param" + i, i, "type" + i, "value" + i);
        }

        return scheme;
    }

    /**
     * Compares two location schemes. If this method returns, then the two
     * schemes are equal. This method is used in place of
     * {@link PSLocationScheme#equals(Object)} to confirm the equality of two
     * schemes in {@link #testXml()} due to the fact that
     * {@link PSLocationScheme#toXML()} and
     * {@link PSLocationScheme#fromXML(String)} do not account for location
     * scheme parameter ids, which can't be set because they are created each
     * time a new parameter is added.
     *
     * @param scheme1 the first location scheme, assumed not null.
     * @param scheme2 the second location scheme, assumed not null.
     */
    private void compareSchemes(PSLocationScheme scheme1, PSLocationScheme scheme2) {
        assertEquals(scheme1.getContentTypeId(), scheme2.getContentTypeId());
        assertEquals(scheme1.getContextId(), scheme2.getContextId());
        assertEquals(scheme1.getDescription(), scheme2.getDescription());
        assertEquals(scheme1.getGenerator(), scheme2.getGenerator());
        assertEquals(scheme1.getGUID(), scheme2.getGUID());
        assertEquals(scheme1.getName(), scheme2.getName());
        assertEquals(scheme1.getTemplateId(), scheme2.getTemplateId());

        assertEquals(scheme1.getParameterNames().size(), scheme2.getParameterNames().size());
        var params1 = scheme1.getParameterNames();
        for (var param : params1) {
            var type = scheme1.getParameterType(param);
            var value = scheme1.getParameterValue(param);
            var sequence = scheme1.getParameterSequence(param);
            assertEquals(type, scheme2.getParameterType(param));
            assertEquals(value, scheme2.getParameterValue(param));
            assertEquals(sequence, scheme2.getParameterSequence(param));
        }
    }
}
