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

import com.percussion.share.service.exception.PSDataServiceException;
import java.util.List;

/**
 * Service to encapsulate operations acting on both pages and templates.
 *
 * @author JaySeletz
 */
public interface IPSPageTemplateService {

  /** Name of the field on page content type to store the template ID. */
  String FIELD_NAME_TEMPLATE_ID = "templateid";

  /**
   * Changes the template of the supplied page.
   *
   * @param pageId The page ID, not <code>null</code>.
   * @param templateId The template ID, not <code>null</code>.
   * @throws PSDataServiceException If a data service error occurs.
   */
  void changeTemplate(String pageId, String templateId) throws PSDataServiceException;

  /**
   * Finds all the pages that use a certain template and returns their IDs.
   *
   * @param templateId The ID of the template, not <code>null</code>.
   * @return The list of IDs, not <code>null</code>, may be empty.
   * @throws IPSPageService.PSPageException If an error occurs.
   */
  List<Integer> findPageIdsByTemplate(String templateId) throws IPSPageService.PSPageException;
}
