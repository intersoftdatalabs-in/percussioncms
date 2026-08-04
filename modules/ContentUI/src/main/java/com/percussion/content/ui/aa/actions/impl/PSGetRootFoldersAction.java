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
 * Action that retrieves the root folders accessible to the current user.
 *
 * <p>This action returns a JSON representation of the root folders in the content repository that
 * the current user has access to.
 *
 * <p>Required parameters:
 *
 * <ul>
 *   <li>None - uses the request context to determine user and permissions
 * </ul>
 */
public class PSGetRootFoldersAction extends PSAAActionBase {

  /** No-op default constructor. */
  public PSGetRootFoldersAction() {
    super();
  }

  /**
   * Executes the action to retrieve root folders for the current user context.
   *
   * @param params this action does not require explicit parameters; user context is derived from
   *     the request
   * @return PSActionResponse containing the root folders as JSON
   * @throws PSAAClientActionException if the folders cannot be retrieved
   */
  public PSActionResponse execute(Map<String, Object> params) throws PSAAClientActionException {
    try {
      return new PSActionResponse(
          PSContentBrowser.getRootFolders(getRequestContext()),
          PSActionResponse.RESPONSE_TYPE_JSON);
    } catch (Exception e) {
      throw new PSAAClientActionException(e);
    }
  }
}
