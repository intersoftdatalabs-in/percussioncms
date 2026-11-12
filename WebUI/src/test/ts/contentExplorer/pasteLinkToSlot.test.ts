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
import type { SlotRelationship } from "../../../main/ts/api/contentExplorer/slotRelationshipApi";
import {
  applyPastedLinksToSlot,
  gatePasteLinkToSlot,
  pasteLinkDependentIds,
} from "../../../main/ts/contentExplorer/pasteLinkToSlot";

const AA_EDGE: PSExplorerRelationshipEdge = {
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

function created(
  overrides: Partial<SlotRelationship> = {},
): SlotRelationship {
  return {
    relationshipId: 91,
    ownerId: 42,
    dependentId: 88,
    slotId: 5,
    templateId: 4,
    sortRank: 2,
    ...overrides,
  };
}

describe("pasteLinkDependentIds", () => {
  it("keeps positive ids in order without repeats", () => {
    expect(pasteLinkDependentIds([88, 0, 89, 88, -1, Number.NaN])).toEqual([
      88, 89,
    ]);
  });

  it("is empty for nullish input", () => {
    expect(pasteLinkDependentIds(null)).toEqual([]);
    expect(pasteLinkDependentIds(undefined)).toEqual([]);
  });
});

describe("gatePasteLinkToSlot", () => {
  const base = {
    clipboard: { operation: "copy" as const, contentIds: [88, 89] },
    ownerId: 42,
    slotId: 5,
    knownSlotIds: [5, 9],
    allowedTemplateIds: [4, 8],
  };

  it("passes with the first allowed template and every clipboard id", () => {
    expect(gatePasteLinkToSlot(base)).toEqual({
      ok: true,
      ownerId: 42,
      slotId: 5,
      templateId: 4,
      dependentIds: [88, 89],
    });
  });

  it("refuses an empty clipboard", () => {
    expect(
      gatePasteLinkToSlot({
        ...base,
        clipboard: { operation: "copy", contentIds: [] },
      }),
    ).toEqual({ ok: false, reason: "empty_clipboard" });
    expect(gatePasteLinkToSlot({ ...base, clipboard: null })).toEqual({
      ok: false,
      reason: "empty_clipboard",
    });
  });

  it("refuses a cut clipboard", () => {
    expect(
      gatePasteLinkToSlot({
        ...base,
        clipboard: { operation: "cut", contentIds: [88] },
      }),
    ).toEqual({ ok: false, reason: "cut" });
  });

  it("refuses a non-slot target", () => {
    expect(gatePasteLinkToSlot({ ...base, slotId: 0 })).toEqual({
      ok: false,
      reason: "not_slot",
    });
    expect(gatePasteLinkToSlot({ ...base, slotId: 7 })).toEqual({
      ok: false,
      reason: "not_slot",
    });
    expect(gatePasteLinkToSlot({ ...base, ownerId: 0 })).toEqual({
      ok: false,
      reason: "not_slot",
    });
  });

  it("refuses a slot with no allowed template", () => {
    expect(gatePasteLinkToSlot({ ...base, allowedTemplateIds: [] })).toEqual(
      { ok: false, reason: "no_template" },
    );
  });
});

describe("applyPastedLinksToSlot", () => {
  it("appends one row per created relationship", () => {
    const next = applyPastedLinksToSlot(
      [AA_EDGE],
      [created(), created({ relationshipId: 92, dependentId: 89 })],
      "Brief",
    );
    expect(next.map((edge) => edge.relationshipId)).toEqual([71, 91, 92]);
    expect(next[1]).toMatchObject({
      configName: "ActiveAssembly",
      slotId: 5,
      dependentId: 88,
      templateName: "Brief",
    });
    expect(next[2]).toMatchObject({ slotId: 5, dependentId: 89 });
  });

  it("ignores responses without a positive relationship id", () => {
    expect(
      applyPastedLinksToSlot([AA_EDGE], [created({ relationshipId: 0 })], "Brief"),
    ).toEqual([AA_EDGE]);
  });
});
