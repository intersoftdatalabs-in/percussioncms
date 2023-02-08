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
package com.percussion.pso.relationshipbuilder.exit;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.percussion.extension.PSExtensionProcessingException;
import com.percussion.extension.PSParameterMismatchException;
import com.percussion.pso.relationshipbuilder.IPSRelationshipBuilder;
import com.percussion.pso.relationshipbuilder.PSActiveAssemblyRelationshipBuilder;
import com.percussion.pso.relationshipbuilder.PSAaDependentRelationshipBuilder;
import com.percussion.pso.relationshipbuilder.PSAaOwnerRelationshipBuilder;
import com.percussion.pso.utils.PSOExtensionParamsHelper;
import com.percussion.server.IPSRequestContext;

/**
 * This class is intended to be used as a post-exit on a content editor
 * resource, to create relationships in a specific slot with the request's
 * current item as the dependent and owners determined by a field's value.
 * 
 * @author James Schultz
 * @author Adam Gent
 * @since 6.0
 */
public class PSBuildAaRelationshipsExit extends PSAbstractBuildRelationshipsExtension {
    private static final String SOURCE_ITEM_TYPE_PARAM_DEPENDENT = "DEPENDENT";
    private static final String SOURCE_ITEM_TYPE_PARAM_OWNER = "OWNER";
    private static final String SOURCE_ITEM_TYPE_PARAM = "sourceItemType";
    private static final String TEMPLATE_NAME_PARAM = "templateName";
    private static final String SLOT_NAME_PARAM = "slotName";


    /**
     * The log instance to use for this class, never <code>null</code>.
     */
    @SuppressWarnings("unused")

    private static final Logger log = LogManager.getLogger(PSBuildAaRelationshipsExit.class);
    
    /**
     * Maintains active-assembly-style relationships between the request's
     * content item and a list of content items -- missing relationships will be
     * created, existing relationships with items not in the list will be
     * deleted. The request's content item is the dependent, and the list of
     * content items become its parents. The details of the relationship are
     * provided as parameters (relationship type, slot id, and variant id).
     * 
     * @param request
     *            the current request context, not <code>null</code>..
     * @return a {@link IPSRelationshipBuilder}
     */
    @Override
    public IPSRelationshipBuilder createRelationshipBuilder(Map<String, String> paramMap, 
            IPSRequestContext request, Mode mode) {
        PSOExtensionParamsHelper extParams = new PSOExtensionParamsHelper(paramMap, request, null);
        String slotName = extParams.getRequiredParameter(SLOT_NAME_PARAM);
        String templateName = extParams.getRequiredParameter(TEMPLATE_NAME_PARAM);
        String sourceItemType = extParams.getRequiredParameter(SOURCE_ITEM_TYPE_PARAM);
        PSActiveAssemblyRelationshipBuilder builder = null;
        
        if (SOURCE_ITEM_TYPE_PARAM_OWNER.equals(sourceItemType.toUpperCase().trim())) {
            builder = new PSAaOwnerRelationshipBuilder();
        }
        else if (SOURCE_ITEM_TYPE_PARAM_DEPENDENT.equals(sourceItemType.toUpperCase().trim())) {
            builder = new PSAaDependentRelationshipBuilder();
        }
        else {
              extParams.errorOnParameter(SOURCE_ITEM_TYPE_PARAM, 
                      "sourceItemType must either be 'OWNER' or 'DEPENDENT'");
        }
        //builder.setRelationshipHelperService(getRelationshipHelperService());
        builder.setSlotName(slotName);
        builder.setTemplateName(templateName);
        return builder;
    }

}
