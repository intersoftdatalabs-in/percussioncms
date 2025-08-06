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
package com.percussion.services.assembly.impl.nav;

import com.percussion.cms.PSCmsException;
import com.percussion.security.PSThreadRequestUtils;
import com.percussion.services.assembly.IPSAssemblyItem;
import com.percussion.services.assembly.IPSProxyNode;
import com.percussion.services.assembly.PSAssemblyException;
import com.percussion.services.assembly.data.PSAssemblyWorkItem;
import com.percussion.services.filter.PSFilterException;
import com.percussion.services.guidmgr.data.PSLegacyGuid;
import com.percussion.system.utils.IPSHtmlParameters;
import com.percussion.util.PSStopwatch;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.testing.IntegrationTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test managed nav support code.
 */
@Tag("IntegrationTest")
public class PSNavUnitTest {

    @Test
    public void testBasicNode() throws Exception {
        var sw = new PSStopwatch();
        PSThreadRequestUtils.initServerThreadRequest();

        var item = createWorkItem(309, 487);

        var helper = new PSNavHelper(item);
        sw.start();

        var navon = helper.findNavNode(item);
        System.err.println("Loading proxies for parent axis: " + sw);
        var self = navon;

        // Check image children
        NodeIterator images = self.getNodes("nav:image");
        System.err.println("Loading children: " + sw);
        assertTrue(images.getSize() > 0);

        // Check submenu children of parent
        NodeIterator submenu = navon.getParent().getNodes("nav:submenu");
        assertTrue(submenu.getSize() > 0);
        System.err.println("Loaded parent submenus: " + sw);

        assertNotNull(navon);

        // Test each navon in the parent axis
        int count = 0;
        while (navon != null) {
            var axis = PSNavAxisEnum.ANCESTOR;
            if (count == 0) {
                axis = PSNavAxisEnum.SELF;
            } else if (count == 1) {
                axis = PSNavAxisEnum.PARENT;
            }
            count++;
            checkNavon(navon, axis);
            navon = navon.getParent();
        }

        sw.stop();
        System.err.println("Whole test: " + sw);
    }

    @Test
    public void testGetAncestors() throws PSAssemblyException, RepositoryException, PSCmsException, PSFilterException {
        PSThreadRequestUtils.initServerThreadRequest();

        var item = createWorkItem(309, 487);

        var helper = new PSNavHelper(item);
        var navon = helper.findNavNode(item);

        var pnode = (IPSProxyNode) navon;
        List<Node> ancestors = pnode.getAncestors();
        assertEquals(1, ancestors.size());

        var rootNode = (IPSProxyNode) pnode.getRoot();

        var rootId = ((IPSProxyNode) ancestors.get(0)).getGuid();
        assertEquals(rootNode.getGuid(), rootId);

        var item487Id = pnode.getGuid();

        // test navon with 2 parents
        item = createWorkItem(316, 376);
        helper = new PSNavHelper(item);
        navon = helper.findNavNode(item);
        pnode = (IPSProxyNode) navon;
        ancestors = pnode.getAncestors();
        assertEquals(2, ancestors.size());

        var id = ((IPSProxyNode) ancestors.get(0)).getGuid();
        assertEquals(rootId, id);

        id = ((IPSProxyNode) ancestors.get(1)).getGuid();
        assertEquals(item487Id, id);
    }

    /**
     * Creates a work item with the specified item and folder in the
     * site (id=301), preview context and revision (3).
     *
     * @param folderId the ID of the parent folder of the item.
     * @param itemId   the content ID of the item.
     * @return the created work item, never null.
     */
    private IPSAssemblyItem createWorkItem(int folderId, int itemId) throws PSAssemblyException {
        var item = new PSAssemblyWorkItem();
        item.setParameterValue(IPSHtmlParameters.SYS_ITEMFILTER, "public");
        item.setParameterValue(IPSHtmlParameters.SYS_SITEID, "301");
        item.setParameterValue(IPSHtmlParameters.SYS_CONTEXT, "1");
        item.setParameterValue(IPSHtmlParameters.SYS_CONTENTID, String.valueOf(itemId));
        item.setParameterValue(IPSHtmlParameters.SYS_REVISION, "3");
        item.setParameterValue(IPSHtmlParameters.SYS_FOLDERID, String.valueOf(folderId));
        item.normalize();
        return item;
    }

    private void checkNavon(Node navon, PSNavAxisEnum axis) throws Exception {
        var axisVal = navon.getProperty("nav:axis").getString();
        var axisEnum = PSNavAxisEnum.valueOf(axisVal);
        assertEquals(axis, axisEnum);

        var image = navon.getProperty("nav:selectedImage").getNode();
        assertNotNull(image);
    }
}
