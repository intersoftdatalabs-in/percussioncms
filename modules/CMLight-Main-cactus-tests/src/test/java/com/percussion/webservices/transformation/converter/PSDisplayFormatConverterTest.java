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

import com.percussion.cms.objectstore.PSDFColumns;
import com.percussion.cms.objectstore.PSDbComponentCollection;
import com.percussion.cms.objectstore.PSDisplayColumn;
import com.percussion.cms.objectstore.PSDisplayFormat;
import com.percussion.design.objectstore.PSRelationshipConfigTest;
import com.percussion.utils.testing.IntegrationTest;
import com.percussion.webservices.transformation.PSTransformationException;
import com.percussion.xml.PSXmlDocumentBuilder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.experimental.categories.Category;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link PSDisplayFormatConverter} class.
 */
@Category(IntegrationTest.class)
public class PSDisplayFormatConverterTest extends PSConverterTestBase {

    public void testConversion() throws Exception {
        var source = getSimpleDisplayFormat("testDisplayFormat", 100);
        roundTripConversion(source);

        source = getDisplayFormat("testDisplayFormat", 100);
        roundTripConversion(source);

        var columns = source.getColumnContainer();
        var column = new PSDisplayColumn(
                PSDisplayColumn.createKey("sys_thumbnail", 2L, false));
        column.setDisplayName("Thumbnail");
        column.setRenderType("image");
        columns.add(1, column);
        roundTripConversion(source);

        source.addCommunity("1002");
        source.addCommunity("1003");
        source.addCommunity("1004");
        roundTripConversion(source);

        var dfs = getDisplayFormats();
        for (var df : dfs) {
            roundTripConversion(df);
        }
    }

    @SuppressWarnings("unchecked")
    public void testListToArray() throws Exception {
        var srcList = new ArrayList<PSDisplayFormat>();
        srcList.add(getDisplayFormat("testAction", 100));
        srcList.add(getDisplayFormat("testAction_2", 200));

        var srcList2 = roundTripListConversion(
                com.percussion.webservices.ui.data.PSDisplayFormat[].class, srcList);

        assertEquals(srcList, srcList2);

        srcList = getDisplayFormats();
        srcList2 = roundTripListConversion(
                com.percussion.webservices.ui.data.PSDisplayFormat[].class, srcList);

        assertEquals(srcList, srcList2);
    }

    private void roundTripConversion(PSDisplayFormat source)
            throws PSTransformationException {
        var target = (PSDisplayFormat) roundTripConversion(
                PSDisplayFormat.class,
                com.percussion.webservices.ui.data.PSDisplayFormat.class, source);

        Document doc = PSXmlDocumentBuilder.createXmlDocument();
        // Uncomment for debugging:
        // System.out.println(PSXmlDocumentBuilder.toString(source.toXml(doc)));
        // System.out.println(PSXmlDocumentBuilder.toString(target.toXml(doc)));

        assertEquals(source, target);
    }

    private PSDisplayFormat getDisplayFormat(String name, int id) throws Exception {
        var df = getSimpleDisplayFormat(name, id);

        df.setProperty(PSDisplayFormat.PROP_SORT_COLUMN, "Column 1");
        df.setProperty(PSDisplayFormat.PROP_SORT_DIRECTION,
                PSDisplayFormat.SORT_ASCENDING);
        df.addCommunity(PSDisplayFormat.PROP_COMMUNITY_ALL);

        return df;
    }

    private PSDisplayFormat getSimpleDisplayFormat(String name, int id)
            throws Exception {
        var df = new PSDisplayFormat();

        df.setLocator(PSDisplayFormat.createKey(new String[]{String.valueOf(id)}));
        df.setInternalName(name);
        df.setDisplayName(name + "_label");
        df.setDescription(name + "_description");

        return df;
    }

    private List<PSDisplayFormat> getDisplayFormats() throws Exception {
        Element dfElems = PSRelationshipConfigTest.loadXmlResource(
                "../../rhythmyxdesign/PSDisplayFormats.xml", this.getClass());

        Iterator<?> dfs = (new PSDbComponentCollection(dfElems)).iterator();
        var dfList = new ArrayList<PSDisplayFormat>();
        while (dfs.hasNext()) {
            dfList.add((PSDisplayFormat) dfs.next());
        }
        return dfList;
    }
}
