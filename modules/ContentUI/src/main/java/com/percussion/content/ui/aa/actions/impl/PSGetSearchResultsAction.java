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

package com.percussion.content.ui.aa.actions.impl;

import com.percussion.content.ui.aa.actions.PSAAClientActionException;
import com.percussion.content.ui.aa.actions.PSActionResponse;
import com.percussion.content.ui.search.PSSearchResult;
import java.util.Map;

/**
 * Implementation of the get search results action.
 *
 * <p>This action retrieves search results based on the current request context.
 *
 * <p>Required parameters: None - uses request context to determine search scope.
 */
public class PSGetSearchResultsAction extends PSAAActionBase {
  /** No-op default constructor. */
  public PSGetSearchResultsAction() {
    super();
  }

  /**
   * Executes the search and returns results.
   *
   * @param params the action parameters (search criteria comes from request context)
   * @return PSActionResponse containing JSON with search results
   * @throws PSAAClientActionException if search fails
   */
  public PSActionResponse execute(Map<String, Object> params) throws PSAAClientActionException {
    try {
      return new PSActionResponse(
          new PSSearchResult().getSearchResults(getRequestContext()),
          PSActionResponse.RESPONSE_TYPE_JSON);
    } catch (Exception e) {
      throw new PSAAClientActionException(e.getLocalizedMessage());
    }
  }
}
