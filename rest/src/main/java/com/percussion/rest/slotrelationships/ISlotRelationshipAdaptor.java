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
package com.percussion.rest.slotrelationships;

/**
 * Adaptor for Active Assembly slot relationships (add / create support /
 * arrange). Implementations live in sitemanage {@code apibridge}.
 */
public interface ISlotRelationshipAdaptor {

  /**
   * List slots on the owner template and the current AA relationships.
   *
   * @param ownerId owner content id
   * @param templateId page or snippet template; when null, only relationships
   *     are grouped by slot id
   * @return canvas, never {@code null}
   */
  SlotCanvas canvas(int ownerId, Integer templateId);

  /**
   * Add an existing item to a slot.
   *
   * @param request add request, never {@code null}
   * @return created relationship, or {@code null} when the owner is not found
   */
  SlotRelationship add(SlotAddRequest request);

  /**
   * Remove an AA relationship.
   *
   * @param relationshipId relationship id
   */
  void remove(int relationshipId);

  /**
   * Move a relationship within its slot.
   *
   * @param relationshipId relationship id
   * @param request move request, never {@code null}
   */
  void move(int relationshipId, SlotMoveRequest request);

  /**
   * Change the slot and/or snippet template of a relationship.
   *
   * @param relationshipId relationship id
   * @param request target slot and template, never {@code null}
   * @return updated relationship, or {@code null} when not found
   */
  SlotRelationship changeTemplateSlot(int relationshipId, SlotTemplateSlotRequest request);

  /**
   * Content types allowed in the slot.
   *
   * @param slotId slot id
   * @return list wrapper, never {@code null}
   */
  SlotAllowedChoiceList allowedTypes(int slotId);

  /**
   * Snippet templates allowed for the slot, optionally filtered by content type.
   *
   * @param slotId slot id
   * @param contentTypeId optional content type id
   * @return list wrapper, never {@code null}
   */
  SlotAllowedChoiceList allowedTemplates(int slotId, Integer contentTypeId);
}
