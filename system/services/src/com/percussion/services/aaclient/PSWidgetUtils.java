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

import com.percussion.cms.objectstore.PSComponentSummary;
import com.percussion.extension.IPSJexlMethod;
import com.percussion.extension.IPSJexlParam;
import com.percussion.error.PSMissingBeanConfigurationException;
import com.percussion.services.assembly.IPSAssemblyItem;
import com.percussion.services.assembly.PSAssemblyException;
import com.percussion.services.assembly.PSAssemblyServiceLocator;
import com.percussion.services.legacy.IPSCmsObjectMgr;
import com.percussion.services.legacy.PSCmsObjectMgrLocator;
import com.percussion.system.utils.IPSHtmlParameters;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Utility class with static methods intended to be used in the Active Assembly (AA) interface.
 * This class provides JEXL methods for parsing assembly parameters, handling object IDs,
 * and managing widget node types in the AA system.
 *
 * @author Percussion Software
 */
public final class PSWidgetUtils {

    private static final Logger log = LogManager.getLogger(PSWidgetUtils.class);

    // Private constructor to prevent instantiation
    private PSWidgetUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Converts a multi-valued parameter map from an assembly item to a single-valued map.
     * Takes the first value from each parameter array.
     *
     * @param item the assembly item to extract parameters from, must not be null
     * @return map of parameter names to their first values, never null
     * @throws IllegalArgumentException if item is null
     */
    @IPSJexlMethod(description = "helper to convert map of multi valued map from the supplied assembly item to a map of single values", params = {
        @IPSJexlParam(name = "item", type = "PSAssemblyWorkItem", description = "Current assembly item to look for the assembly parameters")
    }, returns = "Map of name and single value counter parts of the assembly parameters")
    public static Map<String, String> getParams(IPSAssemblyItem item) {
        Objects.requireNonNull(item, "item must not be null");

        var params = new HashMap<String, String>();
        var oldParams = item.getParameters();

        oldParams.entrySet().stream()
            .filter(entry -> entry.getKey() instanceof String)
            .filter(entry -> entry.getValue() instanceof String[])
            .forEach(entry -> {
                var key = (String) entry.getKey();
                var values = (String[]) entry.getValue();
                if (values != null && values.length > 0) {
                    params.put(key, values[0]);
                }
            });

        params.put(IPSHtmlParameters.SYS_VARIANTID,
            String.valueOf(item.getTemplate().getGUID().getUUID()));

        return params;
    }

    /**
     * Parses assembly parameters for a page and returns a JSON string identifying the parent page.
     *
     * @param item the assembly item to process, must not be null
     * @return JSON string uniquely identifying the parent page, never null
     * @throws PSAssemblyException if assembly service operations fail
     * @throws PSMissingBeanConfigurationException if service configuration is missing
     * @throws IllegalArgumentException if item is null
     */
    @IPSJexlMethod(description = "helper to parse assembly parameters for the supplied assembly page and return a JSON string", params = {
        @IPSJexlParam(name = "item", type = "PSAssemblyWorkItem", description = "Current assembly item to look for the assembly parameters")
    }, returns = "JSON object string to uniquely identify the parent page")
    public static String parseParentObjectId(IPSAssemblyItem item)
            throws PSAssemblyException, PSMissingBeanConfigurationException {
        Objects.requireNonNull(item, "item must not be null");

        var params = getParams(item);
        var obj = new JSONObject();
        obj.putAll(parseObjectId(params, PSWidgetNodeType.WIDGET_NODE_TYPE_PAGE));
        return obj.toString();
    }

    /**
     * Parses assembly parameters for a page/snippet and slot, returning a JSON string identifying the slot.
     *
     * @param item the assembly item to process, must not be null
     * @param slotName the name of the slot, must not be null or empty
     * @return JSON string uniquely identifying the slot on the page/snippet, never null
     * @throws PSAssemblyException if assembly service operations fail
     * @throws PSMissingBeanConfigurationException if service configuration is missing
     * @throws IllegalArgumentException if parameters are invalid
     */
    @IPSJexlMethod(description = "helper to parse assembly parameters for the supplied assembly page/snippet and slot and return a JSON string", params = {
        @IPSJexlParam(name = "item", type = "PSAssemblyWorkItem", description = "Current assembly item to look for the assembly parameters"),
        @IPSJexlParam(name = "slotName", type = "String", description = "Slot name")
    }, returns = "JSON object string to uniquely identify the slot on the page/snippet")
    public static String parseSlotObjectId(IPSAssemblyItem item, String slotName)
            throws PSAssemblyException, PSMissingBeanConfigurationException {
        Objects.requireNonNull(item, "item must not be null");
        if (StringUtils.isEmpty(slotName)) {
            throw new IllegalArgumentException("slotName must not be null or empty");
        }

        var slotId = PSAssemblyServiceLocator.getAssemblyService()
            .findSlotByName(slotName).getGUID().getUUID();
        var params = getParams(item);
        params.put(IPSHtmlParameters.SYS_SLOTID, String.valueOf(slotId));

        var obj = new JSONObject();
        obj.putAll(parseObjectId(params, PSWidgetNodeType.WIDGET_NODE_TYPE_SLOT));
        return obj.toString();
    }

    /**
     * Parses assembly parameters for a snippet and returns a JSON string identifying the snippet.
     *
     * @param item the assembly item to process, must not be null
     * @param slotName the name of the slot containing the snippet, must not be null or empty
     * @return JSON string uniquely identifying the snippet in a page, never null
     * @throws PSAssemblyException if assembly service operations fail
     * @throws PSMissingBeanConfigurationException if service configuration is missing
     * @throws IllegalArgumentException if parameters are invalid
     */
    @IPSJexlMethod(description = "helper to parse assembly parameters for the supplied assembly snippet and return a JSON string", params = {
        @IPSJexlParam(name = "item", type = "PSAssemblyWorkItem", description = "Current assembly item to look for the assembly parameters")
    }, returns = "JSON object string to uniquely identify the snippet in a page")
    public static String parseSnippetObjectId(IPSAssemblyItem item, String slotName)
            throws PSAssemblyException, PSMissingBeanConfigurationException {
        Objects.requireNonNull(item, "item must not be null");
        if (StringUtils.isEmpty(slotName)) {
            throw new IllegalArgumentException("slotName must not be null or empty");
        }

        var slotId = PSAssemblyServiceLocator.getAssemblyService()
            .findSlotByName(slotName).getGUID().getUUID();
        var params = getParams(item);
        params.put(IPSHtmlParameters.SYS_SLOTID, String.valueOf(slotId));

        var obj = new JSONObject();
        obj.putAll(parseObjectId(params, PSWidgetNodeType.WIDGET_NODE_TYPE_SNIPPET));
        return obj.toString();
    }

    /**
     * Parses object ID parameters based on the specified node type.
     *
     * @param params parameter map, must not be null
     * @param nodeType the type of widget node, must not be null
     * @return map containing parsed object ID parameters, never null
     * @throws PSAssemblyException if assembly service operations fail
     * @throws PSMissingBeanConfigurationException if service configuration is missing
     * @throws IllegalArgumentException if params is null
     */
    public static Map<String, String> parseObjectId(Map<String, Object> params, PSWidgetNodeType nodeType)
            throws PSAssemblyException, PSMissingBeanConfigurationException {
        Objects.requireNonNull(params, "params must not be null");
        Objects.requireNonNull(nodeType, "nodeType must not be null");

        var id = parseCommonParams(params);
        id.put(ATTR_NODETYPE, String.valueOf(nodeType.getOrdinal()));

        switch (nodeType) {
            case WIDGET_NODE_TYPE_PAGE:
                // Revision required
                var revision = parseParam(params, IPSHtmlParameters.SYS_REVISION, null, true);
                id.put(IPSHtmlParameters.SYS_REVISION, revision);
                break;
            case WIDGET_NODE_TYPE_SLOT:
                // slotid required
                var slotId = parseParam(params, IPSHtmlParameters.SYS_SLOTID, null, true);
                id.put(IPSHtmlParameters.SYS_SLOTID, slotId);
                break;
            case WIDGET_NODE_TYPE_SNIPPET:
                // relationshipid required
                var relationshipId = parseParam(params, IPSHtmlParameters.SYS_RELATIONSHIPID, null, true);
                id.put(IPSHtmlParameters.SYS_RELATIONSHIPID, relationshipId);
                // slotid required
                var snippetSlotId = parseParam(params, IPSHtmlParameters.SYS_SLOTID, null, true);
                id.put(IPSHtmlParameters.SYS_SLOTID, snippetSlotId);
                break;
            case WIDGET_NODE_TYPE_FIELD:
                // CE field name required
                var fieldName = parseParam(params, IPSHtmlParameters.SYS_FIELD_NAME, null, true);
                id.put(IPSHtmlParameters.SYS_FIELD_NAME, fieldName);
                break;
            default:
                log.warn("Unhandled node type: {}", nodeType);
                break;
        }
        return id;
    }

    /**
     * Parses parameters common to all node types.
     *
     * @param params parameter map, must not be null
     * @return map containing common parameters, never null
     * @throws PSAssemblyException if assembly service operations fail
     * @throws PSMissingBeanConfigurationException if service configuration is missing
     */
    private static Map<String, String> parseCommonParams(Map<String, Object> params)
            throws PSAssemblyException, PSMissingBeanConfigurationException {
        var id = new HashMap<String, String>();

        // ContentId (required)
        var contentId = parseParam(params, IPSHtmlParameters.SYS_CONTENTID, null, true);
        id.put(IPSHtmlParameters.SYS_CONTENTID, contentId);

        // Revision (required)
        var revision = parseParam(params, IPSHtmlParameters.SYS_REVISION, null, true);
        id.put(IPSHtmlParameters.SYS_REVISION, revision);

        // Template name / VariantId
        var templateName = parseParam(params, IPSHtmlParameters.SYS_TEMPLATE, null, false);
        String variantId;
        if (StringUtils.isNotEmpty(templateName)) {
            var templateId = PSAssemblyServiceLocator.getAssemblyService()
                .findTemplateByName(templateName).getGUID().getUUID();
            variantId = String.valueOf(templateId);
        } else {
            variantId = parseParam(params, IPSHtmlParameters.SYS_VARIANTID, null, true);
        }
        id.put(IPSHtmlParameters.SYS_VARIANTID, variantId);

        // Optional parameters
        Optional.ofNullable(parseParam(params, IPSHtmlParameters.SYS_FOLDERID, null, false))
            .filter(StringUtils::isNotEmpty)
            .ifPresent(folderId -> id.put(IPSHtmlParameters.SYS_FOLDERID, folderId));

        // Parameters with default values
        id.put(IPSHtmlParameters.SYS_SITEID,
            parseParam(params, IPSHtmlParameters.SYS_SITEID, "0", false));
        id.put(IPSHtmlParameters.SYS_CONTEXT,
            parseParam(params, IPSHtmlParameters.SYS_CONTEXT, "0", false));
        id.put(IPSHtmlParameters.SYS_AUTHTYPE,
            parseParam(params, IPSHtmlParameters.SYS_AUTHTYPE, "0", false));

        return id;
    }

    /**
     * Parses a parameter with the given name from the parameter map.
     *
     * @param params parameter map, must not be null
     * @param name parameter name, must not be null or empty
     * @param defValue default value if parameter doesn't exist, may be null
     * @param isRequired true if parameter must have a non-null/non-empty value
     * @return parsed parameter value, may be null if not required
     * @throws IllegalArgumentException if required value is null or empty
     */
    private static String parseParam(Map<String, Object> params, String name,
            String defValue, boolean isRequired) {
        var val = Optional.ofNullable(params.get(name))
            .map(Object::toString)
            .filter(StringUtils::isNotEmpty)
            .orElse(defValue);

        if (isRequired && StringUtils.isEmpty(val)) {
            throw new IllegalArgumentException(name + " must not be null or empty");
        }
        return val;
    }

    /**
     * Retrieves a component summary for the specified content ID.
     *
     * @param contentId the content ID to look up, must be positive
     * @return component summary for the content item, never null
     * @throws PSMissingBeanConfigurationException if service configuration is missing
     * @throws IllegalArgumentException if contentId is not positive
     */
    public static PSComponentSummary getItemSummary(int contentId)
            throws PSMissingBeanConfigurationException {
        if (contentId <= 0) {
            throw new IllegalArgumentException("Content ID must be positive: " + contentId);
        }

        var objMgr = PSCmsObjectMgrLocator.getObjectManager();
        return objMgr.loadComponentSummary(contentId);
    }

    // Constants for attribute names

    /** Generic node type attribute name */
    public static final String ATTR_NODETYPE = "nodeType";

    /** Action attribute name */
    public static final String ATTR_ACTION = "action";

    /** Tree widget node title attribute */
    public static final String TREENODE_ATTR_TITLE = "title";

    /** Object ID attribute name */
    public static final String ATTR_OBJECTID = "objectId";

    /** Tree node folder flag attribute */
    public static final String TREENODE_ATTR_ISFOLDER = "isFolder";

    /** Tree node icon source attribute */
    public static final String TREENODE_ATTR_ICONSRC = "childIconSrc";
}
