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

import com.percussion.cas.PSModifyRelatedContent;
import com.percussion.cms.PSCmsException;
import com.percussion.cms.objectstore.PSAaRelationship;
import com.percussion.content.ui.aa.actions.PSAAClientActionException;
import com.percussion.content.ui.aa.actions.PSActionResponse;
import com.percussion.system.utils.IPSHtmlParameters;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

/**
 * Returns the sort rank of the items relationship. Expects sys_relationshipid parameter. Returns
 * the sort rank result as plain text.
 */
public class PSGetItemSortRankAction extends PSAAActionBase {

  /** No-op default constructor. */
  public PSGetItemSortRankAction() {
    super();
  }

  // see interface for more detail
  public PSActionResponse execute(Map<String, Object> params) throws PSAAClientActionException {
    String rid = (String) getParameter(params, IPSHtmlParameters.SYS_RELATIONSHIPID);
    if (StringUtils.isBlank(rid))
      throw new PSAAClientActionException("sys_relationshipid is a required parameter.");

    try {
      PSAaRelationship r =
          new PSAaRelationship(
              PSModifyRelatedContent.getRelationship(Integer.parseInt(rid), getRequestContext()));

      return new PSActionResponse(
          String.valueOf(r.getSortRank()), PSActionResponse.RESPONSE_TYPE_PLAIN);
    } catch (PSCmsException e) {
      throw new PSAAClientActionException(e);
    }
  }
}
