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
import type { SlotEditorRow } from "../../../main/ts/design/TemplateSlotsPanel";
import { emptyLayoutDraft, emptyStylesDraft } from "../../../main/ts/design/slotLayoutStyles";
import {
  slotRowIndexesForServerError,
  validateSlotRows,
} from "../../../main/ts/design/slotMappingValidation";

function row(partial: Partial<SlotEditorRow> = {}): SlotEditorRow {
  return {
    key: "main",
    label: "Main",
    name: "main",
    layout: emptyLayoutDraft(),
    styles: emptyStylesDraft(),
    initialLayout: emptyLayoutDraft(),
    initialStyles: emptyStylesDraft(),
    ...partial,
  };
}

describe("validateSlotRows (#6477)", () => {
  it("accepts clean rows", () => {
    expect(
      validateSlotRows([
        row({
          layout: {
            ...emptyLayoutDraft(),
            orientation: "horizontal",
            columns: "2",
            maxItems: "10",
          },
        }),
      ]),
    ).toEqual([]);
  });

  it("flags non-numeric columns and maxItems with the server slot index shape", () => {
    const issues = validateSlotRows([
      row({ layout: { ...emptyLayoutDraft(), columns: "two" } }),
      row({ layout: { ...emptyLayoutDraft(), maxItems: "-1" } }),
    ]);
    expect(issues).toHaveLength(2);
    expect(issues[0]).toEqual({
      index: 0,
      message: "slots[0].columns must be a non-negative whole number",
    });
    expect(issues[1]).toEqual({
      index: 1,
      message: "slots[1].maxItems must be a non-negative whole number",
    });
  });

  it("flags unknown orientations and unmapped rows", () => {
    const issues = validateSlotRows([
      row({ layout: { ...emptyLayoutDraft(), orientation: "diagonal" } }),
      row({ key: "  " }),
    ]);
    expect(issues).toEqual([
      {
        index: 0,
        message: "slots[0].orientation must be horizontal, vertical, or empty",
      },
      { index: 1, message: "slots[1] is not mapped to a known slot" },
    ]);
  });

  it("reports every problem on one row in field order", () => {
    const issues = validateSlotRows([
      row({
        layout: {
          ...emptyLayoutDraft(),
          orientation: "diagonal",
          columns: "two",
          maxItems: "-1",
        },
      }),
    ]);
    expect(issues).toEqual([
      {
        index: 0,
        message: "slots[0].orientation must be horizontal, vertical, or empty",
      },
      {
        index: 0,
        message: "slots[0].columns must be a non-negative whole number",
      },
      {
        index: 0,
        message: "slots[0].maxItems must be a non-negative whole number",
      },
    ]);
  });

  it("skips rows that failed to load (inline load error already shown)", () => {
    expect(
      validateSlotRows([row({ loadError: "Could not load slot details." })]),
    ).toEqual([]);
  });
});

describe("slotRowIndexesForServerError (#6477)", () => {
  const rows = [row({ key: "main", name: "main", label: "Main" }), row({ key: "sidebar", name: "sidebar", label: "Sidebar" })];

  it("maps slots[N] index references", () => {
    expect(slotRowIndexesForServerError(rows, "slots[1] not found: sidebar")).toEqual([1]);
  });

  it("maps not-found keys against row keys", () => {
    expect(slotRowIndexesForServerError(rows, "slots[7] not found: main")).toEqual([0]);
  });

  it("returns [] for unrelated messages, blanks, and empty rows", () => {
    expect(slotRowIndexesForServerError(rows, "assembler must not be blank")).toEqual([]);
    expect(slotRowIndexesForServerError(rows, null)).toEqual([]);
    expect(slotRowIndexesForServerError(rows, "slots[9] is null")).toEqual([]);
    expect(slotRowIndexesForServerError([], "slots[0] not found: main")).toEqual([]);
  });
});
