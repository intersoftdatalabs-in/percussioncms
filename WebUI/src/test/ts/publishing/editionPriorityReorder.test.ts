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
import type { EditionSummary } from "@/api/publishing/designApi";
import {
  buildEditionReorderBody,
  editionsAfterSuccessfulReorder,
  sortEditionsByPriority,
} from "@/publishing/editionPriorityReorder";

function row(
  editionId: string,
  name: string,
  priority?: number,
): EditionSummary {
  return { editionId, name, siteId: "1", priority };
}

describe("buildEditionReorderBody", () => {
  it("steps the priority up by one with a priority-only body", () => {
    expect(buildEditionReorderBody(2, 1)).toEqual({ priority: 3 });
    const body = buildEditionReorderBody(4, 1);
    expect(body).not.toHaveProperty("name");
    expect(body).not.toHaveProperty("comment");
    expect(body).not.toHaveProperty("siteId");
    expect(body).not.toHaveProperty("editionId");
  });

  it("steps the priority down by one", () => {
    expect(buildEditionReorderBody(3, -1)).toEqual({ priority: 2 });
  });

  it("refuses to leave 1–5 so the caller disables the button", () => {
    expect(buildEditionReorderBody(5, 1)).toBeNull();
    expect(buildEditionReorderBody(1, -1)).toBeNull();
    expect(buildEditionReorderBody(undefined, 1)).toBeNull();
    expect(buildEditionReorderBody(null, -1)).toBeNull();
    expect(buildEditionReorderBody(2.5, 1)).toBeNull();
  });
});

describe("sortEditionsByPriority", () => {
  it("orders highest priority first with a stable name tie-break", () => {
    const rows = [row("1", "Low", 1), row("2", "High", 5), row("3", "Mid", 3)];
    expect(sortEditionsByPriority(rows).map((r) => r.name)).toEqual([
      "High",
      "Mid",
      "Low",
    ]);
  });

  it("sorts missing priorities last without mutating the input", () => {
    const rows = [row("1", "NoPri"), row("2", "PriOne", 1)];
    const sorted = sortEditionsByPriority(rows);
    expect(sorted.map((r) => r.name)).toEqual(["PriOne", "NoPri"]);
    expect(rows[0].name).toBe("NoPri");
  });
});

describe("editionsAfterSuccessfulReorder", () => {
  it("prefers refreshed rows and re-sorts them into the new order", () => {
    const prev = [row("1", "Low", 1), row("2", "High", 5)];
    const refreshed = [row("1", "Low", 2), row("2", "High", 5)];
    const next = editionsAfterSuccessfulReorder(refreshed, "1", 2, prev);
    expect(next).toEqual(sortEditionsByPriority(refreshed));
    expect(next.map((r) => r.editionId)).toEqual(["2", "1"]);
  });

  it("patches only the moved priority and re-sorts when reload failed", () => {
    const prev = [row("1", "Low", 1), row("2", "High", 5)];
    const next = editionsAfterSuccessfulReorder(null, "1", 5, prev);
    expect(next.map((r) => r.editionId)).toEqual(["2", "1"]);
    expect(next.find((r) => r.editionId === "2")?.priority).toBe(5);
  });
});
