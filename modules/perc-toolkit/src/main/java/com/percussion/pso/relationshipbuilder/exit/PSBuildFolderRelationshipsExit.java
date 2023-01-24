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

import com.percussion.extension.IPSResultDocumentProcessor;
import com.percussion.pso.relationshipbuilder.IPSRelationshipBuilder;
import com.percussion.pso.relationshipbuilder.PSFolderRelationshipBuilder;
import com.percussion.pso.utils.PSOExtensionParamsHelper;
import com.percussion.server.IPSRequestContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PSBuildFolderRelationshipsExit extends PSAbstractBuildRelationshipsExtension
        implements IPSResultDocumentProcessor {

    /**
     * The log instance to use for this class, never <code>null</code>.
     */




    private static final Logger log = LogManager.getLogger(PSBuildFolderRelationshipsExit.class);

    @Override
    public IPSRelationshipBuilder createRelationshipBuilder(Map<String, String> paramMap, 
            IPSRequestContext request, Mode mode) throws IllegalArgumentException {
        log.debug( mode + " Folder Relationships");
        PSOExtensionParamsHelper extParams = new PSOExtensionParamsHelper(paramMap, request, log);
        PSFolderRelationshipBuilder builder = new PSFolderRelationshipBuilder();
       // builder.setRelationshipHelperService(getRelationshipHelperService());
        String jcrQuery = extParams.getRequiredParameter("jcrQuery");
        builder.setJcrQuery(jcrQuery);
        return builder;
    }

}
