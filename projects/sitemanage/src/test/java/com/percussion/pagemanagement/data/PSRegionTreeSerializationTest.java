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
package com.percussion.pagemanagement.data;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.percussion.share.dao.PSSerializerUtils;

public class PSRegionTreeSerializationTest {

    @BeforeEach
    public void setup() {
        // No-op for now.
    }

    @Test
    public void testRegionTreeSerialization() throws Exception {
        var tree = new PSRegionTree();

        var region = new PSRegion();
        var child = new PSRegion();

        var code = new PSRegionCode();
        code.setTemplateCode("#blah()");

        child.setRegionId("child");

        region.getChildren().add(code);
        region.getChildren().add(child);
        var c2 = new PSRegionCode();
        c2.setTemplateCode("crap");
        region.getChildren().add(c2);

        region.setRegionId("Adam");
        var wi = new PSWidgetItem();
        wi.setName("Blah");
        wi.setId("1");

        var wi2 = new PSWidgetItem();
        wi2.setName("Foo");
        wi2.setDescription("Foo description.");
        wi2.setId("2");

        var wi3 = new PSWidgetItem();
        wi3.setId("3");

        var wr = new PSRegionWidgets();
        wr.setRegionId("Adam");
        wr.setWidgetItems(asList(wi, wi2, wi3));
        var sets = new HashSet<PSRegionWidgets>();
        sets.add(wr);
        tree.setRegionWidgetAssociations(sets);
        tree.setRootRegion(region);
        var s = PSSerializerUtils.marshal(tree);
        assertNotNull(s);
        log.debug(s);

        var unmarshal = PSSerializerUtils.unmarshal(s, PSRegionTree.class);
        assertNotNull(unmarshal);
        assertNotNull(unmarshal.getRegionWidgetAssociations());
        assertFalse(unmarshal.getRegionWidgetAssociations().isEmpty());
    }

    /**
     * The log instance to use for this class, never null.
     */
    private static final Logger log = LogManager.getLogger(PSRegionTreeSerializationTest.class);
}
