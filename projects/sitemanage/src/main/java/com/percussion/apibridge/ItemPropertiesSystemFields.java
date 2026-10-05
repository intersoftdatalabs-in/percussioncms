/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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
package com.percussion.apibridge;

import com.percussion.cms.PSCmsException;
import com.percussion.cms.objectstore.PSComponentSummary;
import com.percussion.cms.objectstore.PSCoreItem;
import com.percussion.cms.objectstore.PSItemField;
import org.apache.commons.lang3.StringUtils;

/**
 * Keeps the workflow id when an item-properties save posts a blank {@code sys_workflowid}.
 *
 * <p>The content editor modify pipe writes every loaded field, and an empty {@code sys_workflowid}
 * becomes NULL. {@code WORKFLOWAPPID} is not nullable (#5246). Community id is copied the same way.
 * Other blank system fields stay as the editor loaded them. Fields that are absent are left alone.
 */
final class ItemPropertiesSystemFields {

  private static final String[] TEXT_FIELDS = {"sys_workflowid", "sys_communityid"};

  private ItemPropertiesSystemFields() {}

  static boolean needsBackfill(PSCoreItem core) {
    if (core == null) {
      return false;
    }
    for (String name : TEXT_FIELDS) {
      if (isBlankField(core, name)) {
        return true;
      }
    }
    return false;
  }

  static void preserveBlank(PSCoreItem core, PSComponentSummary summary) {
    if (core == null || summary == null) {
      return;
    }
    putTextIfBlank(
        core,
        "sys_workflowid",
        summary.getWorkflowAppId() > 0 ? Integer.toString(summary.getWorkflowAppId()) : null);
    putTextIfBlank(
        core,
        "sys_communityid",
        summary.getCommunityId() != 0 ? Integer.toString(summary.getCommunityId()) : null);
  }

  private static void putTextIfBlank(PSCoreItem core, String name, String value) {
    if (StringUtils.isBlank(value) || !isBlankField(core, name)) {
      return;
    }
    core.setTextField(name, value);
  }

  private static boolean isBlankField(PSCoreItem core, String name) {
    PSItemField field = core.getFieldByName(name);
    if (field == null || field.getValue() == null) {
      return field != null;
    }
    try {
      return StringUtils.isBlank(field.getValue().getValueAsString());
    } catch (PSCmsException e) {
      return true;
    }
  }
}
