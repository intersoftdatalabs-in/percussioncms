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

import com.percussion.cms.objectstore.PSKey;
import com.percussion.cms.objectstore.PSSearch;
import com.percussion.design.objectstore.PSRelationshipConfigTest;
import com.percussion.utils.testing.IntegrationTest;
import com.percussion.webservices.ui.data.PSSearchDef;
import com.percussion.webservices.ui.data.PSViewDef;
import com.percussion.xml.PSXmlDocumentBuilder;
import org.junit.experimental.categories.Category;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link PSSearchViewConverter} class.
 */
@Category(IntegrationTest.class)
public class PSSearchViewConverterTest extends PSConverterTestBase {

    /**
     * Tests the conversion from a server to a client object and vice versa.
     */
    public void testSearchConversion() throws Exception {
        var simpleSearch = getSimpleSearch("simpleSearch");
        roundTripConversionAssert(simpleSearch, PSSearchDef.class);

        var srcList = getPSSearches(false);
        for (var s : srcList) {
            roundTripConversionAssert(s, PSSearchDef.class);
        }
    }

    public void testViewConversion() throws Exception {
        var simpleSearch = getSimpleView("simpleView");
        roundTripConversionAssert(simpleSearch, PSViewDef.class);

        var srcList = getPSSearches(true);
        for (var s : srcList) {
            roundTripConversionAssert(s, PSViewDef.class);
        }
    }

    private void roundTripConversionAssert(PSSearch source, Class<?> clientClass)
            throws Exception {
        var target = (PSSearch) roundTripConversion(PSSearch.class, clientClass, source);

        // Uncomment for debugging:
        // var doc = PSXmlDocumentBuilder.createXmlDocument();
        // System.out.println(PSXmlDocumentBuilder.toString(source.toXml(doc)));
        // System.out.println(PSXmlDocumentBuilder.toString(target.toXml(doc)));

        assertEquals(source, target);
    }

    /**
     * Test a list of server objects convert to (client) view array, and vice versa.
     */
    @SuppressWarnings("unchecked")
    public void testViewListToArray() throws Exception {
        var srcList = new ArrayList<PSSearch>();
        srcList.add(getSimpleView("testView"));
        srcList.add(getSimpleView("testView_2"));

        var srcList2 = roundTripListConversion(PSViewDef[].class, srcList);
        assertEquals(srcList, srcList2);

        srcList = getPSSearches(true);
        srcList2 = roundTripListConversion(PSViewDef[].class, srcList);
        assertEquals(srcList, srcList2);
    }

    /**
     * Test a list of server objects convert to (client) search array, and vice versa.
     */
    @SuppressWarnings("unchecked")
    public void testSearchListToArray() throws Exception {
        var srcList = new ArrayList<PSSearch>();
        srcList.add(getSimpleSearch("testSearch"));
        srcList.add(getSimpleSearch("testSearch_2"));

        var srcList2 = roundTripListConversion(PSSearchDef[].class, srcList);
        assertEquals(srcList, srcList2);

        srcList = getPSSearches(false);
        srcList2 = roundTripListConversion(PSSearchDef[].class, srcList);
        assertEquals(srcList, srcList2);
    }

    /**
     * @return a list of search objects, loaded from predefined test data,
     * never {@code null} or empty.
     */
    private List<PSSearch> getPSSearches(boolean isView) throws Exception {
        Element searchElem = PSRelationshipConfigTest.loadXmlResource(
                "../../rhythmyxdesign/PSSearches_Views.xml", this.getClass());

        NodeList nodes = searchElem.getElementsByTagName("PSXSearch");
        int length = nodes.getLength();
        var searches = new ArrayList<PSSearch>();
        for (int i = 0; i < length; i++) {
            var s = new PSSearch((Element) nodes.item(i));
            if (isView) {
                if (s.isView())
                    searches.add(s);
            } else {
                if (!s.isView())
                    searches.add(s);
            }
        }
        return searches;
    }

    /**
     * Creates a search with the given name.
     *
     * @param name the name of the new action, assumed not {@code null}.
     * @return the created action, never {@code null}.
     */
    private PSSearch getSimpleSearch(String name) throws Exception {
        var target = new PSSearch(name);
        var key = PSSearch.createKey(new String[]{"123"});
        key.setPersisted(false);
        target.setLocator(key);
        target.setType(PSSearch.TYPE_STANDARDSEARCH);
        return target;
    }

    private PSSearch getSimpleView(String name) throws Exception {
        var target = new PSSearch(name);
        var key = PSSearch.createKey(new String[]{"123"});
        key.setPersisted(false);
        target.setLocator(key);
        target.setType(PSSearch.TYPE_VIEW);
        return target;
    }
}
