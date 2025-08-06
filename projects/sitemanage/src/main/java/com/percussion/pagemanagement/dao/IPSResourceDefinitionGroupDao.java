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
package com.percussion.pagemanagement.dao;

import com.percussion.pagemanagement.data.PSResourceDefinitionGroup;
import com.percussion.pagemanagement.data.PSResourceDefinitionGroup.PSAssetResource;
import com.percussion.pagemanagement.data.PSResourceDefinitionGroup.PSResourceDefinition;
import com.percussion.pagemanagement.service.IPSResourceDefinitionService.PSResourceDefinitionNotFoundException;
import com.percussion.pagemanagement.service.PSResourceServiceException;
import com.percussion.share.dao.IPSGenericDao;
import com.percussion.share.service.exception.PSDataServiceException;
import java.util.List;

/**
 * DAO for retrieving resource definitions.
 */
public interface IPSResourceDefinitionGroupDao extends IPSGenericDao<PSResourceDefinitionGroup, String> {

    PSResourceDefinitionGroup find(String id) throws PSDataServiceException;

    List<PSResourceDefinition> findAllResources() throws PSResourceServiceException, PSDataServiceException;

    /**
     * Finds a resource by unique ID.
     * @param uniqueId never {@code null}.
     * @return may be {@code null}.
     */
    PSResourceDefinition findResource(String uniqueId) throws PSDataServiceException;

    /**
     * Finds the primary asset resource for the given content type.
     * @param contentType never {@code null}.
     * @return may be {@code null}.
     */
    PSAssetResource findAssetResourceForType(String contentType) throws PSDataServiceException;

    /**
     * Finds all asset resource definitions for a given content type.
     * @param contentType the content type.
     * @return never {@code null}, may be empty.
     */
    List<PSAssetResource> findAssetResourcesForType(String contentType) throws PSResourceServiceException, PSDataServiceException;

    /**
     * Finds resources associated with a legacy template.
     * @param template never {@code null}.
     * @return never {@code null}, may be empty.
     */
    List<PSAssetResource> findAssetResourcesForLegacyTemplate(String template) throws PSResourceServiceException, PSDataServiceException;
}
