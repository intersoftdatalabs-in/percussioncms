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
import com.percussion.webservices.content.IPSContentDesignWs;
import com.percussion.webservices.content.PSContentWsLocator;
import java.util.List;
import java.util.Map;

/**
 * Action that catalogs the locales defined in the system and returns the count.
 *
 * <p>This action takes no parameters and is useful for determining how many localization contexts
 * are available in the system.
 */
public class PSGetLocaleCountAction extends PSAAActionBase {

  /** No-op default constructor. */
  public PSGetLocaleCountAction() {
    super();
  }

  /**
   * Executes the action to retrieve the count of defined locales.
   *
   * @param params this action does not require any parameters; an empty map is acceptable
   * @return PSActionResponse containing the number of locales as a plain text string
   * @throws PSAAClientActionException if the locales cannot be cataloged
   */
  @SuppressWarnings("unused") // exception
  public PSActionResponse execute(@SuppressWarnings("unused") Map<String, Object> params)
      throws PSAAClientActionException {
    IPSContentDesignWs cd = PSContentWsLocator.getContentDesignWebservice();
    List locales = cd.findLocales(null, null);
    return new PSActionResponse(
        String.valueOf(locales.size()), PSActionResponse.RESPONSE_TYPE_PLAIN);
  }
}
