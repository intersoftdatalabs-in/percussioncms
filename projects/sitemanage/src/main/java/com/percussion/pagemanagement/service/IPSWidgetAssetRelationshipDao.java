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

// REFACTORED: CP-JAVA11
package com.percussion.pagemanagement.service;

/**
 * Service used to update Page &amp; Asset relationships.
 *
 * @author YuBingChen
 */
public interface IPSWidgetAssetRelationshipDao {

  /**
   * Updates the widget name (of the given template) for all relationships where the owners are the
   * pages that use the given template.
   *
   * @param templateId The ID of the template, not blank.
   * @param widgetName The new name of the widget, may be null or empty.
   * @param widgetId The ID of the widget, not blank.
   * @return Number of relationships that have been updated.
   */
  int updateWidgetNameForRelatedPages(String templateId, String widgetName, long widgetId);
}
