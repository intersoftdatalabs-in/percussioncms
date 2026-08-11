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
package com.percussion.searchmanagement.service.impl;

import com.percussion.design.objectstore.PSLocator;
import com.percussion.search.IPSFieldValueModifier;
import com.percussion.search.PSSearchIndexEventQueue;
import com.percussion.services.notification.IPSNotificationListener;
import com.percussion.services.notification.IPSNotificationService;
import com.percussion.services.notification.PSNotificationEvent;
import com.percussion.services.notification.PSNotificationEvent.EventType;
import com.percussion.services.workflow.data.PSState;
import com.percussion.share.dao.IPSFolderHelper;
import com.percussion.share.dao.PSDateUtils;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.util.PSDataTypeConverter;
import com.percussion.webservices.PSWebserviceUtils;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** Modifies field values before items are indexed, to ensure search queries behave as expected. */
@Component("searchIndexFieldValueModifier")
public class PSSearchIndexFieldValueModifier
    implements IPSFieldValueModifier, IPSNotificationListener {
  private static final Logger log = LogManager.getLogger(PSSearchIndexFieldValueModifier.class);

  private final IPSFolderHelper folderHelper;
  private final IPSIdMapper idMapper;

  /**
   * Intentional publish-to-registry of {@code this} for server-initialized notification. Justified
   * {@code this-escape} suppress: listener registration is required at bean construction.
   */
  @SuppressWarnings("this-escape")
  @Autowired
  public PSSearchIndexFieldValueModifier(
      IPSFolderHelper folderHelper,
      IPSIdMapper idMapper,
      IPSNotificationService notificationService) {
    this.folderHelper = folderHelper;
    this.idMapper = idMapper;
    notificationService.addListener(EventType.CORE_SERVER_INITIALIZED, this);
  }

  @Override
  public void modifyFields(Map<String, Object> itemFragment) {
    try {
      var strContentId = (String) itemFragment.get("sys_contentid");
      var contentId = NumberUtils.toInt(strContentId);
      if (contentId <= 0) {
        throw new IllegalArgumentException("Invalid or missing content id: " + strContentId);
      }

      var wfId = NumberUtils.toInt((String) itemFragment.get("sys_workflowid"));
      var stateName = (String) itemFragment.get("sys_statename");

      if (wfId <= 0 || StringUtils.isBlank(stateName)) {
        return;
      }

      var userName = (String) itemFragment.get("sys_contentlastmodifier");
      var strDate = (String) itemFragment.get("sys_contentlastmodifieddate");

      if (StringUtils.isBlank(userName) || StringUtils.isBlank(strDate)) {
        return;
      }

      var patternUsed = new StringBuilder();
      var lastModified = PSDataTypeConverter.parseStringToDate(strDate, patternUsed);
      if (lastModified == null) {
        return;
      }

      var wf = PSWebserviceUtils.getWorkflow(wfId);
      PSState state = null;
      for (var test : wf.getStates()) {
        if (test.getName().equals(stateName)) {
          state = test;
          break;
        }
      }
      if (state == null) {
        return;
      }

      var lastModInfo =
          folderHelper.fixupLastModified(
              idMapper.getGuid(new PSLocator(contentId)),
              userName,
              lastModified,
              state.isPublishable());
      itemFragment.put("sys_contentlastmodifier", lastModInfo.getFirst());
      itemFragment.put(
          "sys_contentlastmodifieddate",
          PSDataTypeConverter.transformDateString(
              PSDateUtils.getDateFromString(lastModInfo.getSecond()),
              null,
              patternUsed.toString(),
              true));
    } catch (Exception e) {
      log.error("Failed to update last modifier fields for search indexing", e);
    }
  }

  @Override
  public void notifyEvent(PSNotificationEvent notification) {
    PSSearchIndexEventQueue.getInstance().setFieldValueModifier(this);
  }
}
