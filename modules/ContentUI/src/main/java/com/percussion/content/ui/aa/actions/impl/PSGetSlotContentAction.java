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

import com.percussion.content.ui.aa.PSAAObjectId;
import com.percussion.content.ui.aa.actions.PSAAClientActionException;
import com.percussion.content.ui.aa.actions.PSActionResponse;
import com.percussion.services.assembly.IPSAssemblyItem;
import com.percussion.services.assembly.IPSAssemblyResult;
import com.percussion.services.assembly.IPSTemplateSlot;
import com.percussion.system.utils.IPSHtmlParameters;
import com.percussion.utils.types.PSPair;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

/**
 * Retrieves the assembled html content for the specified slot. Expects an objectid for the snippet.
 *
 * <p>Required parameters:
 *
 * <ul>
 *   <li>objectid - The object ID containing item ID and slot ID
 *   <li>isaamode (optional) - Whether to use active assembly mode
 *   <li>{@link IPSHtmlParameters#SYS_ACTIVE_ASSEMBLY_MODE} (optional) - The active assembly mode
 * </ul>
 */
public class PSGetSlotContentAction extends PSAAActionBase {

  /** No-op default constructor. */
  public PSGetSlotContentAction() {
    super();
  }

  /**
   * Retrieves the assembled HTML content for the specified slot.
   *
   * @param params the action parameters containing object ID and optional assembly mode
   * @return PSActionResponse containing HTML with the slot content
   * @throws PSAAClientActionException if assembly fails
   */
  // see interface for more detail
  public PSActionResponse execute(Map<String, Object> params) throws PSAAClientActionException {
    PSAAObjectId objectId = getObjectId(params);
    String isAAMode = (String) getParameter(params, "isaamode");
    String sys_aamode = (String) getParameter(params, IPSHtmlParameters.SYS_ACTIVE_ASSEMBLY_MODE);
    String result = null;
    try {
      IPSTemplateSlot slot = PSActionUtil.loadSlot(objectId.getSlotId());
      Map<String, String[]> assemblyParams =
          PSActionUtil.getAssemblyParams(objectId, getCurrentUser());
      PSActionUtil.addAssemblyParam(
          assemblyParams, IPSHtmlParameters.SYS_PART, "slot:" + slot.getName());
      if (StringUtils.isNotBlank(isAAMode)) {
        if (isAAMode.equalsIgnoreCase("true")) {
          PSActionUtil.addAssemblyParam(
              assemblyParams, IPSHtmlParameters.SYS_COMMAND, IPSHtmlParameters.SYS_ACTIVE_ASSEMBLY);
          if (StringUtils.isNotBlank(sys_aamode)) {
            PSActionUtil.addAssemblyParam(
                assemblyParams, IPSHtmlParameters.SYS_ACTIVE_ASSEMBLY_MODE, sys_aamode);
          }
        }
      }
      PSPair<IPSAssemblyItem, IPSAssemblyResult> pair = PSActionUtil.assemble(assemblyParams);
      result = new String(pair.getSecond().getResultData(), StandardCharsets.UTF_8);

      int begin = result.indexOf("<div class=\"PsAaSlot\"");
      int end = result.lastIndexOf("</div>");

      result = result.substring(begin, end);
    } catch (Exception e) {
      throw new PSAAClientActionException(e);
    }
    return new PSActionResponse(result, PSActionResponse.RESPONSE_TYPE_HTML);
  }
}
