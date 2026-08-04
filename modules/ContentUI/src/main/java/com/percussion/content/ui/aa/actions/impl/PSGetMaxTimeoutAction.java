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

import com.percussion.content.ui.aa.PSAAClientServlet;
import com.percussion.content.ui.aa.actions.PSAAClientActionException;
import com.percussion.content.ui.aa.actions.PSActionResponse;
import java.util.Map;

/**
 * Action that retrieves the server's maximum timeout setting in seconds.
 *
 * <p>This action is used for keep-alive functionality to determine how long the client should wait
 * before timing out.
 *
 * <p>Required parameters:
 *
 * <ul>
 *   <li>timeout - the timeout value from PSAAClientServlet.PARAM_TIMEOUT
 * </ul>
 */
public class PSGetMaxTimeoutAction extends PSAAActionBase {

  /** No-op default constructor. */
  public PSGetMaxTimeoutAction() {
    super();
  }

  /**
   * Executes the action to return the maximum timeout value.
   *
   * @param params a map containing: - PSAAClientServlet.PARAM_TIMEOUT: the timeout Integer value
   * @return PSActionResponse containing the timeout as a plain text string
   * @throws PSAAClientActionException if the timeout parameter is invalid
   */
  @SuppressWarnings("unused")
  public PSActionResponse execute(Map<String, Object> params) throws PSAAClientActionException {
    Integer timeout = (Integer) params.get(PSAAClientServlet.PARAM_TIMEOUT);
    return new PSActionResponse(
        String.valueOf(timeout.intValue()), PSActionResponse.RESPONSE_TYPE_PLAIN);
  }
}
