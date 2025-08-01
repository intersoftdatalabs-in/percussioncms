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

// REFACTORED: CP-JAVA11
package com.percussion.services.aaclient;

import com.percussion.error.PSMissingBeanConfigurationException;
import com.percussion.services.assembly.IPSTemplateSlot;
import com.percussion.services.assembly.PSAssemblyException;
import com.percussion.services.assembly.PSAssemblyServiceLocator;
import com.percussion.services.assembly.jexl.PSAssemblerUtils;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.guidmgr.data.PSLegacyGuid;
import com.percussion.util.IPSHtmlParameters;
import com.percussion.util.PSStringTemplate;
import com.percussion.util.PSStringTemplate.PSStringTemplateException;
import com.percussion.workflow.PSWorkFlowUtils;
import com.percussion.xml.PSXmlDocumentBuilder;
import com.percussion.xml.PSXmlTreeWalker;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Active Assembly Action Bar widget handler that builds dynamic action bars
 * for different node types (pages, slots, snippets, fields) with integrated
 * workflow functionality.
 *
 * <p>This handler processes HTTP requests and generates HTML action bars based on
 * the node type and associated content, including workflow actions, edit controls,
 * and contextual operations.</p>
 *
 * @author Percussion Software
 */
public class PSActionBar implements IPSWidgetHandler {

    private static final Logger log = LogManager.getLogger(PSActionBar.class);

    // Constants for XML processing
    private static final String ELEMENT_ITEM = "Item";
    private static final String ATTRIB_CONTENTID = "contentid";

    // Template variable keys
    private static final String VAR_IMAGE_URL = "IMAGE_URL";
    private static final String VAR_TITLE = "TITLE";
    private static final String VAR_EDIT_LABEL = "EDIT_LABEL";
    private static final String VAR_EDIT_FUNCTION = "EDIT_FUNCTION";
    private static final String VAR_ACTIVATE_CAPTION = "ACTIVATE_CAPTION";
    private static final String VAR_WORKFLOW_ACTIONS = "WORKFLOW_ACTIONS";
    private static final String VAR_TEMPLATE_NAME = "TEMPLATE_NAME";

    /**
     * Handles HTTP requests for action bar generation based on node type.
     *
     * @param request the HTTP servlet request containing object ID parameter
     * @param response the HTTP servlet response for output
     * @throws IOException if request processing or response generation fails
     */
    @Override
    public void handleRequest(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        Objects.requireNonNull(request, "Request cannot be null");
        Objects.requireNonNull(response, "Response cannot be null");

        try {
            var objectId = Optional.ofNullable(request.getParameter(PSWidgetUtils.ATTR_OBJECTID))
                .filter(StringUtils::isNotEmpty)
                .orElseThrow(() -> new IOException("Missing or empty object ID parameter"));

            var jsObjId = parseObjectId(objectId);
            var nodeType = extractNodeType(jsObjId);
            var responseContent = buildActionResponse(request, jsObjId, nodeType);

            PSAaClientServlet.pushResponse(response, responseContent, "text/html", 200);

        } catch (Exception e) {
            log.error("Error handling action bar request: {}", e.getMessage(), e);
            throw new IOException("Failed to generate action bar: " + e.getMessage(), e);
        }
    }

    /**
     * Parses the JSON object ID from the request parameter.
     *
     * @param objectId the JSON string containing object identification
     * @return parsed JSON object
     * @throws IOException if JSON parsing fails
     */
    private JSONObject parseObjectId(String objectId) throws IOException {
        try {
            var parsed = JSONValue.parse(objectId);
            if (!(parsed instanceof JSONObject)) {
                throw new IOException("Object ID parameter is not a valid JSON object");
            }
            return (JSONObject) parsed;
        } catch (Exception e) {
            throw new IOException("Failed to parse object ID JSON: " + e.getMessage(), e);
        }
    }

    /**
     * Extracts and validates the node type from the JSON object.
     *
     * @param jsObjId the JSON object containing node type information
     * @return the widget node type
     * @throws IOException if node type is missing or invalid
     */
    private PSWidgetNodeType extractNodeType(JSONObject jsObjId) throws IOException {
        var nodeTypeStr = Optional.ofNullable((String) jsObjId.get(PSWidgetUtils.ATTR_NODETYPE))
            .filter(StringUtils::isNotEmpty)
            .orElseThrow(() -> new IOException("Missing node type in object ID"));

        try {
            var nodeTypeOrdinal = Short.parseShort(nodeTypeStr);
            return PSWidgetNodeType.fromOrdinal(nodeTypeOrdinal)
                .orElseThrow(() -> new IOException("Unknown node type: " + nodeTypeOrdinal));
        } catch (NumberFormatException e) {
            throw new IOException("Invalid node type format: " + nodeTypeStr, e);
        }
    }

    /**
     * Builds the appropriate action response based on node type.
     *
     * @param request the HTTP request
     * @param jsObjId the parsed JSON object ID
     * @param nodeType the widget node type
     * @return the generated action bar HTML
     * @throws Exception if action building fails
     */
    private String buildActionResponse(HttpServletRequest request, JSONObject jsObjId,
            PSWidgetNodeType nodeType) throws Exception {
        switch (nodeType) {
            case WIDGET_NODE_TYPE_PAGE:
                return buildPageActions(request, jsObjId);
            case WIDGET_NODE_TYPE_SLOT:
                return buildSlotActions(request, jsObjId);
            case WIDGET_NODE_TYPE_SNIPPET:
                return buildSnippetActions(request, jsObjId);
            case WIDGET_NODE_TYPE_FIELD:
                return buildFieldActions(request, jsObjId);
            default:
                throw new IOException("Unsupported node type: " + nodeType);
        }
    }

    /**
     * Builds action bar for page nodes with workflow integration.
     *
     * @param request the HTTP request
     * @param jsObjId the JSON object containing page information
     * @return generated HTML for page actions
     * @throws PSStringTemplateException if template processing fails
     */
    private String buildPageActions(HttpServletRequest request, JSONObject jsObjId)
            throws PSStringTemplateException {
        var contentId = extractContentId(jsObjId);
        var title = new PSAssemblerUtils().getTitle(new PSLegacyGuid(contentId));

        var vars = new HashMap<String, String>();
        vars.put(VAR_IMAGE_URL, PSWidgetNodeType.WIDGET_NODE_TYPE_PAGE.getIconUrl());
        vars.put(VAR_TITLE, title);
        vars.put(VAR_EDIT_LABEL, "Edit");

        Optional.ofNullable((String) jsObjId.get("activeCaption"))
            .filter(StringUtils::isNotEmpty)
            .ifPresent(caption -> vars.put(VAR_ACTIVATE_CAPTION, caption));

        vars.putAll(buildWorkflowActions(request, jsObjId));

        var template = PSAAStubUtil.getPageActions();
        return template.expand(vars);
    }

    /**
     * Builds action bar for snippet nodes with workflow integration.
     *
     * @param request the HTTP request
     * @param jsObjId the JSON object containing snippet information
     * @return generated HTML for snippet actions
     * @throws PSStringTemplateException if template processing fails
     */
    private String buildSnippetActions(HttpServletRequest request, JSONObject jsObjId)
            throws PSStringTemplateException {
        var contentId = extractContentId(jsObjId);
        var title = new PSAssemblerUtils().getTitle(new PSLegacyGuid(contentId));

        var vars = new HashMap<String, String>();
        vars.put(VAR_IMAGE_URL, PSWidgetNodeType.WIDGET_NODE_TYPE_SNIPPET.getIconUrl());
        vars.put(VAR_TITLE, title);
        vars.put(VAR_TEMPLATE_NAME, "another template");

        Optional.ofNullable((String) jsObjId.get("activeCaption"))
            .filter(StringUtils::isNotEmpty)
            .ifPresent(caption -> vars.put(VAR_ACTIVATE_CAPTION, caption));

        vars.putAll(buildWorkflowActions(request, jsObjId));

        var template = PSAAStubUtil.getSnippetActions();
        return template.expand(vars);
    }

    /**
     * Builds action bar for slot nodes.
     *
     * @param request the HTTP request
     * @param jsObjId the JSON object containing slot information
     * @return generated HTML for slot actions
     * @throws PSStringTemplateException if template processing fails
     * @throws PSMissingBeanConfigurationException if service configuration is missing
     * @throws PSAssemblyException if assembly service operations fail
     * @throws NumberFormatException if slot ID parsing fails
     */
    private String buildSlotActions(HttpServletRequest request, JSONObject jsObjId)
            throws PSStringTemplateException, NumberFormatException,
            PSMissingBeanConfigurationException, PSAssemblyException {
        var slotIdStr = Optional.ofNullable((String) jsObjId.get(IPSHtmlParameters.SYS_SLOTID))
            .filter(StringUtils::isNotEmpty)
            .orElseThrow(() -> new IllegalArgumentException("Missing slot ID"));

        var slotId = Long.parseLong(slotIdStr);
        var slot = PSAssemblyServiceLocator.getAssemblyService()
            .loadSlot(new PSGuid(PSTypeEnum.SLOT, slotId));

        var vars = new HashMap<String, String>();
        vars.put(VAR_IMAGE_URL, PSWidgetNodeType.WIDGET_NODE_TYPE_SLOT.getIconUrl());
        vars.put(VAR_TITLE, slot.getLabel());

        Optional.ofNullable((String) jsObjId.get("activeCaption"))
            .filter(StringUtils::isNotEmpty)
            .ifPresent(caption -> vars.put(VAR_ACTIVATE_CAPTION, caption));

        var template = PSAAStubUtil.getSlotActions();
        return template.expand(vars);
    }

    /**
     * Builds workflow actions for content items.
     *
     * @param request the HTTP request
     * @param jsObjId the JSON object containing content information
     * @return map of template variables for workflow actions
     */
    private Map<String, String> buildWorkflowActions(HttpServletRequest request, JSONObject jsObjId) {
        var contentId = extractContentId(jsObjId);
        var vars = new HashMap<String, String>();

        var isPublic = PSWorkFlowUtils.isPublic(contentId.intValue());
        if (isPublic) {
            vars.put(VAR_EDIT_LABEL, "Quick Edit");
            vars.put(VAR_EDIT_FUNCTION, "PSQuickEditContent();");
        } else {
            vars.put(VAR_EDIT_LABEL, "Edit");
            vars.put(VAR_EDIT_FUNCTION, "PSEditContent();");
        }

        // Build workflow action HTML
        var wfActionDivs = buildWorkflowActionDivs(contentId);
        vars.put(VAR_WORKFLOW_ACTIONS, wfActionDivs);

        return vars;
    }

    /**
     * Builds HTML div elements for workflow actions.
     *
     * @param contentId the content ID for workflow operations
     * @return HTML string containing workflow action divs
     */
    private String buildWorkflowActionDivs(Long contentId) {
        var doc = PSXmlDocumentBuilder.createXmlDocument();
        var rootElem = PSXmlDocumentBuilder.createRoot(doc, "workflowactions");
        rootElem.setAttribute(ATTRIB_CONTENTID, contentId.toString());

        var divBuilder = new StringBuilder();
        var div1 = "<div dojoType=\"MenuItem2\" caption=\"";
        var div2 = "onClick='PSBuildWFAction(";
        var div2c = "onClick='PSCheckinCheckout(";
        var div3 = "></div>";

        var nodeList = doc.getElementsByTagName("ActionLink");
        for (var i = 0; i < nodeList.getLength(); i++) {
            var elem = (Element) nodeList.item(i);
            var isTransition = elem.getAttribute("isTransition");
            var actionName = elem.getAttribute("name");

            var displayElement = (Element) elem.getElementsByTagName("DisplayLabel").item(0);
            var displayLabel = PSXmlTreeWalker.getElementData(displayElement);

            if ("checkout".equalsIgnoreCase(actionName)) {
                divBuilder.append(div1).append(displayLabel).append("\" ").append(div2c)
                    .append("\"").append(contentId).append("\",true)' ").append(div3);
            } else if ("checkin".equalsIgnoreCase(actionName)) {
                divBuilder.append(div1).append(displayLabel).append("\" ").append(div2c)
                    .append("\"").append(contentId).append("\",false)' ").append(div3);
            } else if ("yes".equalsIgnoreCase(isTransition)) {
                var commentRequired = elem.getAttribute("commentRequired");
                var transitionId = "";
                var workflowAction = "";

                var paramList = elem.getElementsByTagName("Param");
                for (var j = 0; j < paramList.getLength(); j++) {
                    var paramElem = (Element) paramList.item(j);
                    var paramName = paramElem.getAttribute("name");
                    var paramValue = PSXmlTreeWalker.getElementData(paramElem);

                    if ("sys_transitionid".equalsIgnoreCase(paramName)) {
                        transitionId = paramValue;
                    } else if ("sys_workflowaction".equalsIgnoreCase(paramName)) {
                        workflowAction = paramValue;
                    }
                }

                divBuilder.append(div1).append(displayLabel).append("\" ").append(div2)
                    .append("\"").append(displayLabel).append("\",\"").append(commentRequired)
                    .append("\",\"").append(transitionId).append("\",\"").append(workflowAction)
                    .append("\")' ").append(div3);
            }
        }

        return divBuilder.toString();
    }

    /**
     * Builds action bar for field nodes.
     *
     * @param request the HTTP request
     * @param jsObjId the JSON object containing field information
     * @return generated HTML for field actions
     */
    private String buildFieldActions(HttpServletRequest request, JSONObject jsObjId) {
        log.warn("Field actions not yet implemented");
        return "Field actions not implemented";
    }

    /**
     * Extracts and validates content ID from JSON object.
     *
     * @param jsObjId the JSON object containing content ID
     * @return the content ID as Long
     * @throws IllegalArgumentException if content ID is missing or invalid
     */
    private Long extractContentId(JSONObject jsObjId) {
        var contentIdStr = Optional.ofNullable((String) jsObjId.get(IPSHtmlParameters.SYS_CONTENTID))
            .filter(StringUtils::isNotEmpty)
            .orElseThrow(() -> new IllegalArgumentException("Missing content ID"));

        try {
            return Long.parseLong(contentIdStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid content ID format: " + contentIdStr, e);
        }
    }
}
