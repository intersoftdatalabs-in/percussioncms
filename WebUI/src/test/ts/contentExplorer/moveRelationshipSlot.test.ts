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
  applyRelationshipSlotMove,
  canMoveRelationshipToAnotherSlot,
  destinationSlotChoices,
  gateMoveRelationshipToSlot,
  groupActiveAssemblyBySlot,
} from "../../../main/ts/contentExplorer/moveRelationshipSlot";

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

const sibling: PSExplorerRelationshipEdge = {
  relationshipId: 72,
  configName: "ActiveAssembly",
  category: "rs_activeassembly",
  dependentId: 5,
  label: "AA last",
  slotId: 5,
  sortRank: 1,
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

const pageSlots = [
  { slotId: 5, name: "sidebar", label: "Sidebar" },
  { slotId: 9, name: "list", label: "List" },
  { slotId: 0, name: "bad", label: "Bad" },
];

describe("moveRelationshipToAnotherSlot", () => {
  it("allows one slotted Active Assembly row and refuses a folder or empty slot", () => {
    expect(canMoveRelationshipToAnotherSlot(assembly)).toBe(true);
    expect(canMoveRelationshipToAnotherSlot(folder)).toBe(false);
    expect(
      canMoveRelationshipToAnotherSlot({
        ...assembly,
        category: "rs_translation",
        configName: "Translation",
      }),
    ).toBe(false);
    expect(canMoveRelationshipToAnotherSlot({ ...assembly, slotId: 0 })).toBe(
      false,
    );
    expect(
      canMoveRelationshipToAnotherSlot({ ...assembly, relationshipId: 0 }),
    ).toBe(false);
  });

  it("offers other page slots and not the current slot", () => {
    expect(destinationSlotChoices(5, pageSlots)).toEqual([
      { slotId: 9, label: "List" },
    ]);
    expect(destinationSlotChoices(9, pageSlots)[0]?.slotId).toBe(5);
  });

  it("does not post the same slot, a folder, or a slot that is not on the page", () => {
    expect(
      gateMoveRelationshipToSlot({
        edge: null,
        destinationSlotId: 9,
        pageSlotIds: [5, 9],
      }),
    ).toEqual({ ok: false, reason: "empty" });
    expect(
      gateMoveRelationshipToSlot({
        edge: folder,
        destinationSlotId: 9,
        pageSlotIds: [5, 9],
      }),
    ).toEqual({ ok: false, reason: "folder" });
    expect(
      gateMoveRelationshipToSlot({
        edge: { ...assembly, slotId: 0 },
        destinationSlotId: 9,
        pageSlotIds: [9],
      }),
    ).toEqual({ ok: false, reason: "no_slot" });
    expect(
      gateMoveRelationshipToSlot({
        edge: { ...assembly, templateId: 0 },
        destinationSlotId: 9,
        pageSlotIds: [5, 9],
      }),
    ).toEqual({ ok: false, reason: "no_template" });
    expect(
      gateMoveRelationshipToSlot({
        edge: assembly,
        destinationSlotId: 5,
        pageSlotIds: [5, 9],
      }),
    ).toEqual({ ok: false, reason: "same" });
    expect(
      gateMoveRelationshipToSlot({
        edge: assembly,
        destinationSlotId: 11,
        pageSlotIds: [5, 9],
      }),
    ).toEqual({ ok: false, reason: "not_allowed" });
    expect(
      gateMoveRelationshipToSlot({
        edge: assembly,
        destinationSlotId: 0,
        pageSlotIds: [5, 9],
      }),
    ).toEqual({ ok: false, reason: "empty" });
  });

  it("keeps the snippet template and names the destination slot", () => {
    expect(
      gateMoveRelationshipToSlot({
        edge: assembly,
        destinationSlotId: 9,
        pageSlotIds: [5, 9],
      }),
    ).toEqual({
      ok: true,
      relationshipId: 71,
      sourceSlotId: 5,
      destinationSlotId: 9,
      templateId: 4,
    });
  });

  it("lists the row under the returned slot and leaves the sibling", () => {
    const next = applyRelationshipSlotMove( [assembly, sibling, folder], 71, {
      relationshipId: 91,
      slotId: 9,
      templateId: 4,
      sortRank: 0,
    });
    expect(next[0]).toMatchObject({
      relationshipId: 91,
      slotId: 9,
      templateId: 4,
      templateName: "Brief",
    });
    expect(next[1]).toMatchObject({ relationshipId: 72, slotId: 5 });
    expect(next[2]).toMatchObject({ relationshipId: 82, slotId: 5 });
    const grouped = groupActiveAssemblyBySlot(next);
    expect(grouped.groups.map((group) => group.slotId)).toEqual([9, 5]);
    expect(grouped.groups[0].edges.map((edge) => edge.relationshipId)).toEqual([
      91,
    ]);
    expect(grouped.groups[1].edges.map((edge) => edge.relationshipId)).toEqual([
      72,
    ]);
    expect(grouped.rest.map((edge) => edge.relationshipId)).toEqual([82]);
  });
});
