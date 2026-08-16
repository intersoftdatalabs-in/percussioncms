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

import { describe, expect, it } from "vitest";
import {
  unwrapSlotAllowedChoices,
  unwrapSlotCanvas,
  unwrapSlotRelationship,
} from "../../../../main/ts/api/contentExplorer/slotRelationshipApi";

describe("slotRelationshipApi unwrap", () => {
  it("unwraps a Jackson-wrapped relationship", () => {
    const rel = unwrapSlotRelationship({
      SlotRelationship: {
        relationshipId: 9,
        ownerId: 42,
        dependentId: 7,
        slotId: 3,
        templateId: 4,
        sortRank: 1,
      },
    });
    expect(rel).toEqual({
      relationshipId: 9,
      ownerId: 42,
      dependentId: 7,
      slotId: 3,
      templateId: 4,
      sortRank: 1,
    });
  });

  it("unwraps a canvas with nested slots", () => {
    const canvas = unwrapSlotCanvas({
      ownerId: 42,
      templateId: 7,
      slots: [
        {
          slotId: 3,
          name: "sidebar",
          label: "Sidebar",
          items: [{ relationshipId: 9, ownerId: 42, dependentId: 7, slotId: 3 }],
        },
      ],
    });
    expect(canvas.slots).toHaveLength(1);
    expect(canvas.slots[0].items[0].relationshipId).toBe(9);
    expect(canvas.templateId).toBe(7);
  });

  it("unwraps allowed choices from the list wrapper", () => {
    const items = unwrapSlotAllowedChoices({
      SlotAllowedChoiceList: {
        items: [{ id: 301, name: "rffEvent", label: "Event" }],
      },
    });
    expect(items).toEqual([{ id: 301, name: "rffEvent", label: "Event" }]);
  });

  it("rejects malformed relationship ids instead of coercing them to 0", () => {
    expect(() =>
      unwrapSlotRelationship({
        relationshipId: "not-a-number",
        ownerId: 42,
        dependentId: 7,
        slotId: 3,
      }),
    ).toThrow(/relationshipId/);
    expect(() =>
      unwrapSlotRelationship({
        ownerId: 42,
        dependentId: 7,
        slotId: 3,
      }),
    ).toThrow(/relationshipId is missing/);
    expect(() =>
      unwrapSlotCanvas({
        ownerId: 42,
        slots: [{ name: "sidebar", items: [] }],
      }),
    ).toThrow(/slotId/);
  });
});
