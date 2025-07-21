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

package com.percussion.services.widgetbuilder;

import com.percussion.share.dao.IPSGenericDao;

import java.util.List;

/**
 * Data Access Object interface for Widget Builder Definition operations.
 * Provides CRUD operations for managing widget builder definitions in the system.
 *
 * @author matthewernewein
 */
public interface IPSWidgetBuilderDefinitionDao {

    /**
     * Saves the widget builder definition object.
     *
     * @param definition must not be {@code null}
     * @return the saved widget builder definition
     * @throws IPSGenericDao.SaveException if there's an error saving the definition
     */
    PSWidgetBuilderDefinition save(PSWidgetBuilderDefinition definition) throws IPSGenericDao.SaveException;
    
    /**
     * Finds widget builder definition by the definition id.
     *
     * @param definitionId the unique identifier of the widget builder definition
     * @return a widget builder definition if exists, otherwise {@code null}
     */
    PSWidgetBuilderDefinition find(long definitionId);
    
    /**
     * Deletes the widget builder definition entry for the supplied id.
     *
     * @param definitionId the unique identifier, must not be negative
     */
    void delete(long definitionId);
    
    /**
     * Gets a list of all the Widget Builder Definitions in the system.
     *
     * @return a list of all widget builder definitions, never {@code null}, may be empty
     */
    List<PSWidgetBuilderDefinition> getAll();
}
