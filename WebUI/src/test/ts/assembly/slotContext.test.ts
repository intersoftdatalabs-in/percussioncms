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
  isSlotActionName,
  slotContextHasRelationship,
  slotContextHasSlot,
} from "../../../main/ts/assembly/slotContext";

describe("slotContext", () => {
  it("recognizes Explorer slot action names", () => {
    expect(isSlotActionName("slot_add")).toBe(true);
    expect(isSlotActionName("arrange_remove")).toBe(true);
    expect(isSlotActionName("item_activeassembly")).toBe(false);
  });

  it("does not invent a slot from folder browse", () => {
    expect(slotContextHasSlot(null)).toBe(false);
    expect(slotContextHasSlot({ ownerId: 0, slotId: 3 })).toBe(false);
    expect(slotContextHasSlot({ ownerId: 42, slotId: 3 })).toBe(true);
    expect(slotContextHasRelationship({ ownerId: 42, slotId: 3 })).toBe(false);
    expect(
      slotContextHasRelationship({ ownerId: 42, slotId: 3, relationshipId: 9 }),
    ).toBe(true);
  });
});
