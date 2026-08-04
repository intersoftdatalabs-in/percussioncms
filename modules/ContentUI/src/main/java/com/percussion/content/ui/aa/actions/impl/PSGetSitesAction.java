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
import com.percussion.content.ui.browse.PSContentBrowser;
import java.util.Map;

/**
 * Action to retrieve all available sites from the content browser. Returns a JSON list of site
 * names and their properties.
 *
 * <p>Parameters: None required.
 *
 * <p>Returns: JSON response containing site information.
 */
public class PSGetSitesAction extends PSAAActionBase {

  /** No-op default constructor. */
  public PSGetSitesAction() {
    super();
  }

  /**
   * Executes the get sites action.
   *
   * @param params action parameters (unused)
   * @return PSActionResponse containing JSON list of sites
   * @throws PSAAClientActionException if an error occurs retrieving sites
   */
  public PSActionResponse execute(Map<String, Object> params) throws PSAAClientActionException {
    try {
      return new PSActionResponse(PSContentBrowser.getSites(), PSActionResponse.RESPONSE_TYPE_JSON);
    } catch (Exception e) {
      throw new PSAAClientActionException(e);
    }
  }
}
