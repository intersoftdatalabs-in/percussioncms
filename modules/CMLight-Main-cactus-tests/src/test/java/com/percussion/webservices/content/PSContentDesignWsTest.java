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
package com.percussion.webservices.content;

import com.percussion.cms.IPSConstants;
import com.percussion.cms.objectstore.server.PSItemDefManager;
import com.percussion.design.objectstore.PSContentEditor;
import com.percussion.design.objectstore.PSContentTypeHelper;
import com.percussion.design.objectstore.PSView;
import com.percussion.design.objectstore.PSViewSet;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.contentmgr.IPSNodeDefinition;
import com.percussion.services.contentmgr.data.PSContentTypeWorkflow;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.services.guidmgr.PSGuidManagerLocator;
import com.percussion.services.guidmgr.data.PSLegacyGuid;
import com.percussion.servlets.PSSecurityFilter;
import com.percussion.system.utils.IPSHtmlParameters;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.services.content.data.PSItemStatus;
import com.percussion.utils.testing.IntegrationTest;
import org.apache.commons.collections.IteratorUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.experimental.categories.Category;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for methods not exposed through web services.
 */
@Tag("IntegrationTest")
class PSContentDesignWsTest {

    // ...existing code for test context setup if needed...

    @Test
    @DisplayName("Load and Save Associated Workflows")
    void testLoadAndSaveAssociatedWorkflows() throws Exception {
        var sessionId = login("admin1", "demo");
        var gmgr = PSGuidManagerLocator.getGuidMgr();
        var ctguid = gmgr.makeGuid("311", PSTypeEnum.NODEDEF);
        var cd = PSContentWsLocator.getContentDesignWebservice();
        var ctwfs = cd.loadAssociatedWorkflows(ctguid, true, true);
        assertEquals(2, ctwfs.size());
        var wfguids = new ArrayList<IPSGuid>();
        for (var ctwf : ctwfs) {
            wfguids.add(ctwf.getWorkflowId());
        }
        cd.saveAssociatedWorkflows(ctguid, Collections.singletonList(wfguids.get(0)), true);
        var ctwfsmod = cd.loadAssociatedWorkflows(ctguid, true, false);
        assertEquals(1, ctwfsmod.size());
        cd.saveAssociatedWorkflows(ctguid, wfguids, true);
        ctwfsmod = cd.loadAssociatedWorkflows(ctguid, false, false);
        assertEquals(2, ctwfsmod.size());
    }

    @Test
    @DisplayName("Get Item Edit URL and View Fields")
    void testGetItemEditUrl() throws Exception {
        login("admin1", "demo");
        var contentTypeName = "rffGeneric";
        var node = loadNode(contentTypeName);

        // Test a view with 2 hidden fields
        var viewName = IPSConstants.SYS_HIDDEN_FIELDS_VIEW_NAME + "description,filename";
        createView(viewName, contentTypeName);

        var fields = getViewFields(viewName, contentTypeName);
        assertTrue(fields.contains("sys_title"));
        assertFalse(fields.contains("description"));
        assertFalse(fields.contains("filename"));

        // Get URL for editing a generic item id = 335
        var id = new PSLegacyGuid(335, -1);
        var cd = PSContentWsLocator.getContentDesignWebservice();
        var cw = PSContentWsLocator.getContentWebservice();
        var status = cw.prepareForEdit(id);
        var url = cd.getItemEditUrl(id, contentTypeName, viewName);
        cw.releaseFromEdit(status, false);

        assertNotNull(url);
        assertTrue(url.contains("sys_contentid"));

        // Test a view without any hidden fields
        viewName = IPSConstants.SYS_HIDDEN_FIELDS_VIEW_NAME;
        url = cd.getItemEditUrl(null, contentTypeName, viewName);
        fields = getViewFields(viewName, contentTypeName);

        var allFields = getViewFields(IPSConstants.SYS_ALL_VIEW_NAME, contentTypeName);
        assertEquals(allFields.size(), fields.size());
    }

    private IPSNodeDefinition loadNode(String contentTypeName) {
        var nodes = PSContentTypeHelper.loadNodeDefs(contentTypeName);
        assertNotNull(nodes, contentTypeName + " Content Type must exist");
        assertFalse(nodes.isEmpty(), contentTypeName + " Content Type must exist");
        return nodes.get(0);
    }

    /**
     * Creates a view if not exists for the given view name and content type.
     *
     * @param viewName the view name, assumed not blank.
     * @param ctName the content type name, assumed not blank.
     */
    private void createView(String viewName, String ctName) {
        var cd = PSContentWsLocator.getContentDesignWebservice();
        var url = cd.getItemEditUrl(null, ctName, viewName);
        assertNotNull(url);
        assertFalse(url.contains("sys_contentid"));
    }

    /**
     * Gets a list of field names for the specified view name and content type.
     *
     * @param viewName the name of the view in question, assumed not blank.
     * @param ctName the name of the content type of the view.
     * @return the list of field names, never {@code null}.
     */
    private List<String> getViewFields(String viewName, String ctName) {
        var node = loadNode(ctName);
        var ctEditor = PSItemDefManager.getInstance()
                .getContentEditorDef(node.getGUID().longValue());
        var vset = ctEditor.getViewSet();
        var view = vset.getView(viewName);
        @SuppressWarnings("unchecked")
        var fields = IteratorUtils.toList(view.getFields());
        return fields;
    }

    /**
     * Login using the supplied credentials.
     *
     * @param uid The user id, assumed not {@code null} or empty.
     * @param pwd The password, assumed not {@code null} or empty.
     * @return The session id, never {@code null} or empty.
     * @throws Exception if the login fails.
     */
    private String login(String uid, String pwd) throws Exception {
        // Hack to get by re-logging in to same session (see PSSecurityFilter)
        session.setAttribute("RX_LOGIN_ATTEMPTS", null);
        PSSecurityFilter.authenticate(request, response, uid, pwd);
        return (String) session.getAttribute(IPSHtmlParameters.SYS_SESSIONID);
    }
}
