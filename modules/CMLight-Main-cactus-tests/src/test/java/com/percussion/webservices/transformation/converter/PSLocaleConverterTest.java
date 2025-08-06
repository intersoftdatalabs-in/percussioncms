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

import com.percussion.i18n.PSLocale;

import org.junit.jupiter.api.Tag;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link PSLocaleConverter} class.
 */
@Tag("IntegrationTest")
public class PSLocaleConverterTest extends PSConverterTestBase {

    /**
     * Tests the conversion from a server to a client object.
     */
    public void testConversion() throws Exception {
        var source = new PSLocale("de-ch", "Swiss German",
                "German language used in Switzerland", PSLocale.STATUS_ACTIVE);

        var target = (PSLocale) roundTripConversion(PSLocale.class,
                com.percussion.webservices.content.PSLocale.class, source);

        assertEquals(source, target);

        var srcList = new ArrayList<PSLocale>(2);
        srcList.add(source);
        srcList.add(new PSLocale("fr-ca", "Canadian French",
                "French language used in Canada", PSLocale.STATUS_INACTIVE));
        var tgtList = roundTripListConversion(
                com.percussion.webservices.content.PSLocale[].class, srcList);
        assertEquals(srcList, tgtList);

        target = (PSLocale) roundTripConversion(PSLocale.class,
                com.percussion.webservices.security.data.PSLocale.class, source);
        assertEquals(source, target);

        tgtList = roundTripListConversion(
                com.percussion.webservices.security.data.PSLocale[].class, srcList);
        assertEquals(srcList, tgtList);
    }

    /**
     * Test a list of server object convert to client array, and vice versa.
     */
    @SuppressWarnings("unchecked")
    public void testListToArray() throws Exception {
        var srcList = new ArrayList<PSLocale>();
        srcList.add(new PSLocale("en-us", "English US",
                "English language used in United States", PSLocale.STATUS_ACTIVE));
        srcList.add(new PSLocale("de-ch", "Swiss German",
                "German language used in Switzerland", PSLocale.STATUS_ACTIVE));

        var srcList2 = roundTripListConversion(
                com.percussion.webservices.content.PSLocale[].class, srcList);
        assertEquals(srcList, srcList2);

        srcList2 = roundTripListConversion(
                com.percussion.webservices.security.data.PSLocale[].class, srcList);
        assertEquals(srcList, srcList2);
    }
}
