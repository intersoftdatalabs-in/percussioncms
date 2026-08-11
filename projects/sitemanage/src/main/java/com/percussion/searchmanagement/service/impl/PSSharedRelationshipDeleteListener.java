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

// REFACTORED: CP-JAVA11
package com.percussion.searchmanagement.service.impl;

import static org.apache.commons.lang3.Validate.isTrue;
import static org.apache.commons.lang3.Validate.notNull;

import com.percussion.assetmanagement.service.impl.PSWidgetAssetRelationshipService;
import com.percussion.cms.PSRelationshipChangeEvent;
import com.percussion.design.objectstore.PSRelationship;
import com.percussion.searchmanagement.service.IPSPageIndexService;
import com.percussion.services.notification.IPSNotificationListener;
import com.percussion.services.notification.IPSNotificationService;
import com.percussion.services.notification.PSNotificationEvent;
import com.percussion.services.notification.PSNotificationEvent.EventType;
import com.percussion.share.service.exception.PSValidationException;
import com.percussion.system.utils.PSSiteManageBean;
import java.util.HashSet;
import org.springframework.beans.factory.annotation.Autowired;

/** Notified on relationship changes; re-indexes pages affected by shared asset deletion. */
@PSSiteManageBean("sharedRelationshipDeleteListener")
public class PSSharedRelationshipDeleteListener implements IPSNotificationListener {
  private final IPSPageIndexService indexService;

  /**
   * Intentional publish-to-registry of {@code this} for relationship-change notification. Justified
   * {@code this-escape} suppress: listener registration is required at bean construction.
   */
  @SuppressWarnings("this-escape")
  @Autowired
  public PSSharedRelationshipDeleteListener(
      IPSNotificationService notificationService, IPSPageIndexService indexService) {
    if (notificationService != null) {
      notificationService.addListener(EventType.RELATIONSHIP_CHANGED, this);
    }
    this.indexService = indexService;
  }

  @Override
  public void notifyEvent(PSNotificationEvent event) throws PSValidationException {
    notNull(event, "event");
    isTrue(
        EventType.RELATIONSHIP_CHANGED == event.getType(),
        "Should only be registered for relationship changes.");

    // filter out all relationship changes except delete
    var relEvent = (PSRelationshipChangeEvent) event.getTarget();
    if (relEvent.getAction() != PSRelationshipChangeEvent.ACTION_REMOVE) {
      return;
    }

    // filter out all relationships except shared
    var sharedOwnerIds = new HashSet<Integer>();
    for (var obj : relEvent.getRelationships()) {
      var rel = (PSRelationship) obj;
      if (rel.getConfig()
          .getName()
          .equals(PSWidgetAssetRelationshipService.SHARED_ASSET_WIDGET_REL_TYPE)) {
        sharedOwnerIds.add(rel.getOwner().getId());
      }
    }

    if (!sharedOwnerIds.isEmpty()) {
      indexService.index(sharedOwnerIds);
    }
  }
}
