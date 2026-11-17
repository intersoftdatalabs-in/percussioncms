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
import type { TemplateBindingSummary } from "../../../main/ts/api/developer/types";
import type { SlotEditorRow } from "../../../main/ts/design/TemplateSlotsPanel";
import {
  emptyLayoutDraft,
  emptyStylesDraft,
} from "../../../main/ts/design/slotLayoutStyles";
import {
  firstIssuePerRow,
  mergeSlotPutRowError,
  precheckSaveInputs,
  serverSlotErrorForRows,
  withRefreshedSlotBaselines,
} from "../../../main/ts/design/templateSave";

const VELOCITY = "Java/global/percussion/assembly/velocityAssembler";

const goodBindings: TemplateBindingSummary[] = [
  { executionOrder: 1, variable: "$title", expression: "$sys.item.title" },
];

function row(key: string, patch: Partial<SlotEditorRow> = {}): SlotEditorRow {
  const layout = emptyLayoutDraft();
  const styles = emptyStylesDraft();
  return {
    key,
    label: key,
    name: key,
    layout: { ...layout },
    styles: { ...styles },
    initialLayout: { ...layout },
    initialStyles: { ...styles },
    ...patch,
  };
}

describe("templateSave precheck (#6566)", () => {
  it("rejects a blank assembler without touching row errors", () => {
    const result = precheckSaveInputs("  ", goodBindings, []);
    expect(result.ok).toBe(false);
    if (result.ok) return;
    expect(result.validationError).toBe("Assembler is required");
    expect(result.rowErrors).toBeNull();
  });

  it("rejects incomplete bindings without touching row errors", () => {
    const result = precheckSaveInputs(
      VELOCITY,
      [{ executionOrder: 1, variable: "", expression: "$sys.item.title" }],
      [],
    );
    expect(result.ok).toBe(false);
    if (result.ok) return;
    expect(result.validationError).toMatch(/variable is required/);
    expect(result.rowErrors).toBeNull();
  });

  it("maps slot issues to first-message-per-row errors", () => {
    const rows = [
      row("main", { layout: { ...emptyLayoutDraft(), columns: "two" } }),
      row("side", { layout: { ...emptyLayoutDraft(), columns: "NaN" } }),
    ];
    const result = precheckSaveInputs(VELOCITY, goodBindings, rows);
    expect(result.ok).toBe(false);
    if (result.ok) return;
    expect(result.validationError).toMatch(/2 slot rows need attention/);
    expect(result.rowErrors).toHaveLength(2);
    expect(result.rowErrors?.[0]).toMatch(/slots\[0\]\.columns/);
    expect(result.rowErrors?.[1]).toMatch(/slots\[1\]\.columns/);
  });

  it("uses the bare message for a single slot issue", () => {
    const rows = [
      row("main", { layout: { ...emptyLayoutDraft(), columns: "two" } }),
    ];
    const result = precheckSaveInputs(VELOCITY, goodBindings, rows);
    expect(result.ok).toBe(false);
    if (result.ok) return;
    expect(result.validationError).toMatch(/slots\[0\]\.columns/);
    expect(result.validationError).not.toMatch(/need attention/);
  });

  it("passes clean inputs", () => {
    expect(precheckSaveInputs(VELOCITY, goodBindings, [row("main")])).toEqual({
      ok: true,
    });
  });
});

describe("firstIssuePerRow", () => {
  it("keeps the first issue per row and nulls the rest", () => {
    const rows = [row("a"), row("b"), row("c")];
    expect(
      firstIssuePerRow(rows, [
        { index: 1, message: "first" },
        { index: 1, message: "second" },
        { index: 2, message: "third" },
      ]),
    ).toEqual([null, "first", "third"]);
  });
});

describe("mergeSlotPutRowError", () => {
  it("preserves prior row errors and stamps the failure row", () => {
    expect(mergeSlotPutRowError(["prior", null], 2, 1, "boom")).toEqual([
      "prior",
      "boom",
    ]);
  });
});

describe("withRefreshedSlotBaselines", () => {
  it("refreshes baselines only for saved rows", () => {
    const before = emptyLayoutDraft();
    const rows = [
      row("main", { layout: { ...before, rootClass: "qa-row" } }),
      row("side"),
    ];
    const next = withRefreshedSlotBaselines(rows, [{ key: "main" }]);
    expect(next[0].initialLayout).toEqual({ ...before, rootClass: "qa-row" });
    expect(next[1]).toBe(rows[1]);
  });
});

describe("serverSlotErrorForRows", () => {
  it("returns empty indexes for non-API errors", () => {
    const { indexes, serverMessage } = serverSlotErrorForRows(
      [row("main")],
      new Error("network down"),
    );
    expect(indexes).toEqual([]);
    expect(serverMessage).toBeUndefined();
  });

  it("resolves slots[N] refs to row indexes with the server message", () => {
    const { indexes, serverMessage } = serverSlotErrorForRows([row("main")], {
      status: 400,
      statusText: "Bad Request",
      body: { message: "slots[0] not found: main" },
    });
    expect(indexes).toEqual([0]);
    expect(serverMessage).toContain("slots[0] not found: main");
  });
});
