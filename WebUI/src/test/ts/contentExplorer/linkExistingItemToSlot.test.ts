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
import type { PSExplorerRelationshipEdge } from "../../../main/ts/api/contentExplorer/relationship";
import {
  applyLinkedItemToSlot,
  gateLinkExistingItem,
  knownActiveAssemblySlotIds,
} from "../../../main/ts/contentExplorer/linkExistingItemToSlot";

const assembly: PSExplorerRelationshipEdge = {
  relationshipId: 71,
  configName: "ActiveAssembly",
  category: "rs_activeassembly",
  dependentId: 4,
  label: "AA first",
  slotId: 5,
  sortRank: 0,
  templateId: 4,
  templateName: "Brief",
};

const folder: PSExplorerRelationshipEdge = {
  relationshipId: 82,
  configName: "Folder",
  category: "rs_folder",
  dependentId: 3,
  label: "Folder row",
  slotId: 5,
  templateId: 4,
  templateName: "Brief",
};

const allowed = [4, 8];

describe("linkExistingItemToSlot", () => {
  it("treats only Active Assembly slots as link targets", () => {
    expect(knownActiveAssemblySlotIds([assembly, folder])).toEqual([5]);
    expect(knownActiveAssemblySlotIds([folder])).toEqual([]);
    expect(knownActiveAssemblySlotIds([{ ...assembly, slotId: 0 }])).toEqual([]);
  });

  it("refuses a selection that is not a slot, a blank target, and a missing target", () => {
    const base = {
      targetRaw: "88",
      templateId: 4,
      allowedTemplateIds: allowed,
      knownSlotIds: [5],
      ownerId: 42,
    };
    expect(gateLinkExistingItem({ ...base, selection: null }).ok).toBe(false);
    expect(
      gateLinkExistingItem({ ...base, selection: { kind: "other" } }),
    ).toEqual({ ok: false, reason: "not_slot" });
    expect(
      gateLinkExistingItem({
        ...base,
        selection: { kind: "slot", slotId: 9 },
      }),
    ).toEqual({ ok: false, reason: "not_slot" });
    expect(
      gateLinkExistingItem({
        ...base,
        selection: { kind: "slot", slotId: 5 },
        targetRaw: "  ",
      }),
    ).toEqual({ ok: false, reason: "blank" });
    expect(
      gateLinkExistingItem({
        ...base,
        selection: { kind: "slot", slotId: 5 },
        targetRaw: "not-an-item",
      }),
    ).toEqual({ ok: false, reason: "missing" });
    expect(
      gateLinkExistingItem({
        ...base,
        selection: { kind: "slot", slotId: 5 },
        templateId: 0,
      }),
    ).toEqual({ ok: false, reason: "no_template" });
    expect(
      gateLinkExistingItem({
        ...base,
        selection: { kind: "slot", slotId: 5 },
        templateId: 3,
      }),
    ).toEqual({ ok: false, reason: "not_allowed" });
  });

  it("accepts a numeric id or a content GUID for one known slot", () => {
    expect(
      gateLinkExistingItem({
        selection: { kind: "slot", slotId: 5 },
        targetRaw: "88",
        templateId: 8,
        allowedTemplateIds: allowed,
        knownSlotIds: [5],
        ownerId: 42,
      }),
    ).toEqual({
      ok: true,
      ownerId: 42,
      dependentId: 88,
      slotId: 5,
      templateId: 8,
    });
    expect(
      gateLinkExistingItem({
        selection: { kind: "slot", slotId: 5 },
        targetRaw: "1-101-708",
        templateId: 4,
        allowedTemplateIds: allowed,
        knownSlotIds: [5],
        ownerId: 42,
      }),
    ).toMatchObject({ ok: true, dependentId: 708, slotId: 5 });
  });

  it("lists the item only when the add returned that slot and dependent", () => {
    const unchanged = applyLinkedItemToSlot(
      [assembly],
      {
        relationshipId: 0,
        ownerId: 42,
        dependentId: 88,
        slotId: 5,
        templateId: 4,
        sortRank: 1,
      },
      "Brief",
    );
    expect(unchanged).toEqual([assembly]);
    const linked = applyLinkedItemToSlot(
      [assembly],
      {
        relationshipId: 91,
        ownerId: 42,
        dependentId: 88,
        slotId: 5,
        templateId: 8,
        sortRank: 1,
      },
      "Full story",
    );
    expect(linked.map((edge) => edge.relationshipId)).toEqual([71, 91]);
    expect(linked[1]).toMatchObject({
      dependentId: 88,
      slotId: 5,
      templateId: 8,
      templateName: "Full story",
      label: "88",
      category: "rs_activeassembly",
    });
    expect(linked[0]).toEqual(assembly);
  });
});
