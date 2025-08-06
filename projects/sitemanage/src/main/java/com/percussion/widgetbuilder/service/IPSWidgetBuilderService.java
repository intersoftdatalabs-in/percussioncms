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

package com.percussion.widgetbuilder.service;

import com.percussion.widgetbuilder.data.PSWidgetBuilderDefinitionData;
import com.percussion.widgetbuilder.data.PSWidgetBuilderFieldsListData;
import com.percussion.widgetbuilder.data.PSWidgetBuilderSummaryData;
import com.percussion.widgetbuilder.data.PSWidgetBuilderValidationResults;

import java.util.List;

/**
 * Service interface for Widget Builder operations.
 * <p>
 * Sunny Sal says: "Widget building is like making samosas—get the filling right, and everyone will want a bite!"
 * </p>
 */
public interface IPSWidgetBuilderService {

    /**
     * Checks if the Widget Builder service is enabled.
     * Activation and deactivation can be done in Server.properties under the key WidgetBuilderActive.
     *
     * @return true if service enabled, false otherwise
     */
    boolean isWidgetBuilderEnabled();

    /**
     * Checks if the current definition has been deployed.
     *
     * @param definitionId the widget definition ID
     * @return true if the widget is deployed, false otherwise
     */
    boolean isWidgetDefinitionDeployed(long definitionId);

    /**
     * Deletes a widget builder definition.
     *
     * @param definitionId the widget definition ID
     */
    void deleteWidgetBuilderDefinition(long definitionId);

    /**
     * Gets a list of all widget definitions on this system.
     *
     * @return a list of widget definitions
     */
    List<PSWidgetBuilderDefinitionData> loadAll();

    /**
     * Loads a widget definition given an ID.
     *
     * @param definitionId the widget definition ID
     * @return the widget definition
     */
    PSWidgetBuilderDefinitionData loadWidgetDefinition(long definitionId);

    /**
     * Saves a definition.
     *
     * @param definition the widget definition
     * @return the validation results for the save. If validation failed, errors will be present. If successful, results will have the ID of the saved definition.
     */
    PSWidgetBuilderValidationResults saveWidgetBuilderDefinition(PSWidgetBuilderDefinitionData definition);

    /**
     * Builds and deploys the widget.
     *
     * @param definitionId the widget definition ID
     */
    void deployWidget(long definitionId);

    /**
     * Gets a list of summaries for all widget definitions on this system.
     *
     * @return the list, not null, may be empty
     */
    List<PSWidgetBuilderSummaryData> loadAllSummaries();

    /**
     * Validate the supplied definition:
     * <ul>
     *   <li>Widget name for new widget definition is unique</li>
     *   <li>Required fields for widget definition and format</li>
     *   <li>All field names including child fields must be unique with character restrictions</li>
     *   <li>Version format</li>
     *   <li>No field will break widget building or packaging or installation</li>
     * </ul>
     * Does not validate widget functionality (e.g., display HTML, resources).
     *
     * @param definition the definition to validate, not null
     * @return the results of the validation, not null, may be empty
     */
    PSWidgetBuilderValidationResults validate(PSWidgetBuilderDefinitionData definition);
}
