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
package com.percussion.searchmanagement.service;

import com.percussion.searchmanagement.data.PSSearchCriteria;
import com.percussion.searchmanagement.error.PSSearchServiceException;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.share.data.PSItemProperties;
import com.percussion.share.data.PSPagedItemList;
import com.percussion.share.data.PSPagedItemPropertiesList;
import com.percussion.share.service.IPSDataService;
import com.percussion.share.service.exception.PSValidationException;
import java.util.List;

/** Provides search services for {@link PSItemProperties} and related objects. */
public interface IPSSearchService {

  /**
   * Retrieves items found after performing a full text search for the given criteria.
   *
   * @param criteria search criteria, must not be {@code null}
   * @return list of search result objects, never {@code null}, may be empty
   * @throws PSSearchServiceException if any error occurs
   * @throws PSValidationException if validation fails
   * @throws PSNotFoundException if referenced data is not found
   * @throws IPSDataService.DataServiceLoadException if data cannot be loaded
   */
  PSPagedItemList search(PSSearchCriteria criteria)
      throws PSSearchServiceException,
          PSValidationException,
          PSNotFoundException,
          IPSDataService.DataServiceLoadException;

  /**
   * Creates the search result objects for the supplied content ids and returns them.
   *
   * @param criteria search criteria for result columns
   * @param contentIdList must not be {@code null}
   * @return list of search result objects, never {@code null}, may be empty
   * @throws PSSearchServiceException if any error occurs
   */
  PSPagedItemList search(PSSearchCriteria criteria, List<Integer> contentIdList)
      throws PSSearchServiceException,
          PSValidationException,
          PSNotFoundException,
          IPSDataService.DataServiceLoadException;

  /**
   * Retrieves items found after performing a full text search for the given criteria.
   *
   * @param criteria search criteria
   * @return list of search result objects, never {@code null}, may be empty. The item list will
   *     have the list of {@link PSItemProperties}
   * @throws PSSearchServiceException if any error occurs
   */
  PSPagedItemPropertiesList getExtendedSearchResults(PSSearchCriteria criteria)
      throws PSSearchServiceException;

  /**
   * Gets content IDs for fetching by status.
   *
   * @param criteria search criteria
   * @return list of content IDs, never {@code null}
   */
  List<Integer> getContentIdsForFetchingByStatus(PSSearchCriteria criteria);

  /**
   * Searches by status.
   *
   * @param criteria search criteria
   * @param contentIdList content IDs to search
   * @return paged item list
   * @throws PSSearchServiceException if any error occurs
   */
  PSPagedItemList searchByStatus(PSSearchCriteria criteria, List<Integer> contentIdList)
      throws PSSearchServiceException,
          PSValidationException,
          PSNotFoundException,
          IPSDataService.DataServiceLoadException;

  /**
   * Validates the search criteria.
   *
   * @param criteria search criteria
   * @return validated search criteria
   */
  PSSearchCriteria validateSearchCriteria(PSSearchCriteria criteria);
}
