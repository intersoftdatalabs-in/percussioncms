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
package com.percussion.pagemanagement.service;

import java.util.List;

import com.percussion.pagemanagement.data.PSResourceDefinitionGroup;
import com.percussion.pagemanagement.data.PSResourceDefinitionGroup.PSAssetResource;
import com.percussion.pagemanagement.data.PSResourceDefinitionGroup.PSResourceDefinition;
import com.percussion.share.dao.IPSGenericDao;
import com.percussion.share.service.IPSCatalogService;
import com.percussion.share.service.IPSDataService.DataServiceLoadException;
import com.percussion.share.service.IPSDataService.DataServiceNotFoundException;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.share.service.exception.PSValidationException;

/**
 * Loads and manages {@link PSResourceDefinitionGroup}s and {@link PSResourceDefinition}s.
 * This service does <em>NOT</em> convert the resources to preview/publish links.
 *
 * @author adamgent
 */
public interface IPSResourceDefinitionService extends IPSCatalogService<PSResourceDefinitionGroup, String> {

    /**
     * {@link PSResourceDefinition} have a global unique identifier
     * based on the {@link PSResourceDefinitionGroup} that contains them
     * and their own id with {@link #NAMESPACE_SEPARATOR} between them.
     */
    String NAMESPACE_SEPARATOR = ".";

    String THEME_GROUP_NAME = "theme";

    /**
     * Find all resource definition groups.
     *
     * @return List of populated objects
     */
    List<PSResourceDefinitionGroup> findAll() throws PSDataServiceException;

    /**
     * Finds all resource definitions.
     *
     * @return never {@code null}, may be empty
     * @throws PSDataServiceException if loading fails
     */
    List<PSResourceDefinition> findAllResources() throws PSDataServiceException;

    /**
     * Finds a resource definition group.
     *
     * @param id the id is the filename minus the extension
     * @return a resource definition group
     * @throws DataServiceLoadException if loading fails
     * @throws PSResourceDefinitionGroupNotFoundException if not found
     */
    PSResourceDefinitionGroup find(String id) throws DataServiceLoadException, PSResourceDefinitionGroupNotFoundException;

    /**
     * Finds a resource based on the uniqueId.
     * For unique id see {@link #createUniqueId(String, String)}.
     *
     * @param uniqueId never {@code null}
     * @return never {@code null}
     * @throws PSDataServiceException if not found
     */
    PSResourceDefinition findResource(String uniqueId) throws PSDataServiceException;

    /**
     * Finds the default resource based on the content type of the asset.
     *
     * @param contentType never {@code null}
     * @return never {@code null}
     * @throws PSDataServiceException if a default asset could not be found
     */
    PSAssetResource findDefaultAssetResourceForType(String contentType) throws PSDataServiceException;

    /**
     * Finds resources based on the content type of the asset.
     *
     * @param contentType never {@code null}
     * @return never {@code null}, may be empty
     */
    List<PSAssetResource> findAssetResourcesForType(String contentType) throws PSDataServiceException;

    /**
     * Finds resources associated to a legacy template.
     *
     * @param template never {@code null} or empty
     * @return never {@code null}, may be empty
     */
    List<PSAssetResource> findAssetResourcesForLegacyTemplate(String template) throws PSDataServiceException;

    /**
     * Creates a unique identifier to resource given its group id and its local id.
     *
     * @param groupId id of the {@link PSResourceDefinitionGroup}, never {@code null}
     * @param id      the {@link PSResourceDefinition#getId()} never {@code null}
     * @return uniqueId never {@code null}
     * @throws PSResourceDefinitionInvalidIdException if invalid
     */
    String createUniqueId(String groupId, String id) throws PSResourceDefinitionInvalidIdException;

    /**
     * Thrown for system failures like an invalid definition id.
     */
    class PSResourceDefinitionInvalidIdException extends PSDataServiceException {
        private static final long serialVersionUID = 1L;

        public PSResourceDefinitionInvalidIdException(String message) {
            super(message);
        }

        public PSResourceDefinitionInvalidIdException(String message, Throwable cause) {
            super(message, cause);
        }

        public PSResourceDefinitionInvalidIdException(Throwable cause) {
            super(cause);
        }
    }

    /**
     * The service guarantees that it will find a definition otherwise an exception is thrown.
     */
    class PSResourceDefinitionNotFoundException extends DataServiceNotFoundException {
        private static final long serialVersionUID = 1L;

        public PSResourceDefinitionNotFoundException(String message) {
            super(message);
        }

        public PSResourceDefinitionNotFoundException(String message, Throwable cause) {
            super(message, cause);
        }

        public PSResourceDefinitionNotFoundException(Throwable cause) {
            super(cause);
        }
    }

    /**
     * The service guarantees that it will find a definition otherwise an exception is thrown.
     */
    class PSResourceDefinitionGroupNotFoundException extends DataServiceNotFoundException {
        private static final long serialVersionUID = 1L;

        public PSResourceDefinitionGroupNotFoundException(String message) {
            super(message);
        }

        public PSResourceDefinitionGroupNotFoundException(String message, Throwable cause) {
            super(message, cause);
        }

        public PSResourceDefinitionGroupNotFoundException(Throwable cause) {
            super(cause);
        }
    }
}
