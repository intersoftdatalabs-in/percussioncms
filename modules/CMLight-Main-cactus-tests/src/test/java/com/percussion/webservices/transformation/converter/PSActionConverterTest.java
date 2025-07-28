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

import com.percussion.cms.objectstore.*;
import com.percussion.design.objectstore.PSRelationshipConfigTest;
import com.percussion.utils.testing.IntegrationTest;
import com.percussion.webservices.transformation.PSTransformationException;
import com.percussion.xml.PSXmlDocumentBuilder;
import org.junit.experimental.categories.Category;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link PSActionConverter} class.
 */
@Category(IntegrationTest.class)
public class PSActionConverterTest extends PSConverterTestBase {

    /**
     * Tests the conversion from a server to a client object and vice versa.
     */
    public void testConversion() throws Exception {
        var simpleAction = getSimpleAction("simpleAction");
        roundTripConversionAssert(simpleAction);

        var source = getAction("testAction");
        roundTripConversionAssert(source);

        source.getProperties().removeProperty(PSAction.PROP_TARGET_STYLE);
        source.getProperties().removeProperty(PSAction.PROP_TARGET);
        roundTripConversionAssert(source);

        source.setURL(null);
        roundTripConversionAssert(source);

        source.setMenuType(PSAction.TYPE_MENU);
        roundTripConversionAssert(source);
    }

    /**
     * Test with modified properties.
     */
    public void testConversionWithPropChanges() throws Exception {
        var source = getAction("testAction");
        var modified = (PSAction) source.cloneFull();
        assertEquals(source, modified);

        var vctxs = modified.getVisibilityContexts();
        vctxs.clear();

        roundTripConversionAssert(modified);
    }

    /**
     * Test a list of server objects convert to client array, and vice versa.
     */
    @SuppressWarnings("unchecked")
    public void testListToArray() throws Exception {
        var srcList = new ArrayList<PSAction>();
        srcList.add(getAction("testAction"));
        srcList.add(getAction("testAction_2"));

        var srcList2 = roundTripListConversion(
                com.percussion.webservices.ui.data.PSAction[].class, srcList);

        assertEquals(srcList, srcList2);

        srcList = getActions();
        srcList2 = roundTripListConversion(
                com.percussion.webservices.ui.data.PSAction[].class, srcList);
        // assertEquals(srcList, srcList2); // Uncomment if equals is robust for all fields
    }

    private void roundTripConversionAssert(PSAction source) throws PSTransformationException {
        var target = (PSAction) roundTripConversion(PSAction.class,
                com.percussion.webservices.ui.data.PSAction.class, source);

        var doc = PSXmlDocumentBuilder.createXmlDocument();
        // System.out.println(PSXmlDocumentBuilder.toString(source.toXml(doc)));
        // System.out.println(PSXmlDocumentBuilder.toString(target.toXml(doc)));

        assertEquals(source, target);
    }

    private PSAction getSimpleAction(String name) {
        var target = new PSAction(name, name + " label");
        target.setLocator(PSAction.createKey(String.valueOf(123)));
        return target;
    }

    private PSAction getAction(String name) {
        var target = new PSAction(name, name + " label");
        int actionId = 100;
        target.setLocator(PSAction.createKey(String.valueOf(actionId)));
        target.setMenuType(PSAction.TYPE_MENUITEM);
        target.setSortRank(1);
        target.setDescription("Test Action Desc");
        target.setURL("http://localhost:9999/Rhythmyx/testAction");
        target.setClientAction(true);

        target.getParameters().add(new PSActionParameter("p1", "v1"));
        target.getParameters().add(new PSActionParameter("p2", "v2"));

        addProperty(target, PSAction.PROP_ACCEL_KEY, "Alt-K");
        addProperty(target, PSAction.PROP_MNEM_KEY, "Enum-K");
        addProperty(target, PSAction.PROP_SHORT_DESC, "tooltip");
        addProperty(target, PSAction.PROP_SMALL_ICON, "http://localhost:9999/Rhytmyx/smallicom");
        addProperty(target, PSAction.PROP_REFRESH_HINT, "selected");
        addProperty(target, PSAction.PROP_LAUNCH_NEW_WND, PSAction.YES);
        addProperty(target, PSAction.PROP_MUTLI_SELECT, PSAction.NO);
        addProperty(target, PSAction.PROP_TARGET, "test-target");
        addProperty(target, PSAction.PROP_TARGET_STYLE, "target-style");

        target.setModeUIContexts(getModeUIContexts(actionId));
        target.setVisibilityContexts(getVisibilityContexts());

        var children = target.getChildren();
        children.add(new PSMenuChild(1, "child_name_1", actionId));
        children.add(new PSMenuChild(2, "child_name_2", actionId));

        return target;
    }

    private void addProperty(PSAction action, String name, String value) {
        if (value != null && !value.trim().isEmpty()) {
            action.getProperties().add(new PSActionProperty(name, value));
        }
    }

    private PSDbComponentCollection getModeUIContexts(int parentId) {
        var actionId = String.valueOf(parentId);
        var modeCtxs = new PSDbComponentCollection(PSMenuModeContextMapping.class);
        modeCtxs.add(new PSMenuModeContextMapping("1", "2", actionId));
        modeCtxs.add(new PSMenuModeContextMapping("1", "3", actionId));
        modeCtxs.add(new PSMenuModeContextMapping("4", "5", actionId));
        return modeCtxs;
    }

    private PSActionVisibilityContexts getVisibilityContexts() {
        var vises = new PSActionVisibilityContexts();
        vises.addContext("ctx-1", "ctx-1-value");
        vises.addContext("ctx-2", "ctx-2-value");
        vises.addContext("ctx-3", new String[]{"ctx-3-value_1", "ctx-3-value_2"});
        return vises;
    }

    @SuppressWarnings("unused")
    private List<PSAction> getActions() throws Exception {
        var elems = PSRelationshipConfigTest.loadXmlResource(
                "../../rhythmyxdesign/PSActions.xml", this.getClass());
        var nodes = elems.getElementsByTagName(PSAction.XML_NODE_NAME);
        var actions = new ArrayList<PSAction>();
        for (int i = 0; i < nodes.getLength(); i++) {
            actions.add(new PSAction((Element) nodes.item(i)));
        }
        return actions;
    }
}
