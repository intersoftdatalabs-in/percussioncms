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

package com.percussion.assetmanagement.data;

import com.percussion.share.data.PSDataItemSummary;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.Optional;

/**
 * Class to use internally to use attributes from orphan assets.
 *
 * @author Santiago M. Murchio
 */
@XmlRootElement
public class PSOrphanedAssetSummary extends PSDataItemSummary {

  private static final long serialVersionUID = 1L;

  /** Represents the SLOT_ID field. */
  private String slotId;

  /** Represents the WIDGET_NAME field. */
  private String widgetName;

  private int relationshipId;

  public PSOrphanedAssetSummary() {
    super();
  }

  public PSOrphanedAssetSummary(
      String assetId, String slotId, String widgetName, int relationshipId) {
    this.id = assetId;
    this.slotId = slotId;
    this.widgetName = widgetName;
    this.relationshipId = relationshipId;
  }

  public Optional<String> getSlotId() {
    return Optional.ofNullable(slotId);
  }

  public void setSlotId(String slotId) {
    this.slotId = slotId;
  }

  public Optional<String> getWidgetName() {
    return Optional.ofNullable(widgetName);
  }

  public void setWidgetName(String widgetName) {
    this.widgetName = widgetName;
  }

  public int getRelationshipId() {
    return relationshipId;
  }

  public void setRelationshipId(int id) {
    relationshipId = id;
  }
}
