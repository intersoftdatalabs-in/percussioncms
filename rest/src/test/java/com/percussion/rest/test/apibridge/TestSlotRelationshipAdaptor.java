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
package com.percussion.rest.test.apibridge;

import com.percussion.rest.slotrelationships.ISlotRelationshipAdaptor;
import com.percussion.rest.slotrelationships.SlotAddRequest;
import com.percussion.rest.slotrelationships.SlotAllowedChoiceList;
import com.percussion.rest.slotrelationships.SlotCanvas;
import com.percussion.rest.slotrelationships.SlotMoveRequest;
import com.percussion.rest.slotrelationships.SlotRelationship;
import com.percussion.rest.slotrelationships.SlotTemplateSlotRequest;
import java.util.List;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/** Spring test stub for {@link ISlotRelationshipAdaptor} (MainTest shared context). */
@Component
@Lazy
public class TestSlotRelationshipAdaptor implements ISlotRelationshipAdaptor {

  @Override
  public SlotCanvas canvas(int ownerId, Integer templateId) {
    return new SlotCanvas(ownerId, templateId, List.of());
  }

  @Override
  public SlotRelationship add(SlotAddRequest request) {
    return new SlotRelationship(
        1,
        request.getOwnerId(),
        request.getDependentId(),
        request.getSlotId(),
        request.getTemplateId(),
        0);
  }

  @Override
  public void remove(int relationshipId) {
    // no-op Spring test stub
  }

  @Override
  public void move(int relationshipId, SlotMoveRequest request) {
    // no-op Spring test stub
  }

  @Override
  public SlotRelationship changeTemplateSlot(int relationshipId, SlotTemplateSlotRequest request) {
    return new SlotRelationship(relationshipId, 0, 0, request.getSlotId(), request.getTemplateId(), 0);
  }

  @Override
  public SlotAllowedChoiceList allowedTypes(int slotId) {
    return new SlotAllowedChoiceList(List.of());
  }

  @Override
  public SlotAllowedChoiceList allowedTemplates(int slotId, Integer contentTypeId) {
    return new SlotAllowedChoiceList(List.of());
  }
}
