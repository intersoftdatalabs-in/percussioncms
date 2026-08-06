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
  nextSortState,
  sortIndicator,
  sortJobs,
} from "@/publishing/statusSort";
import type { PublishingJob } from "@/publishing/types";

const jobs: PublishingJob[] = [
  { jobId: 1, siteName: "Zeta", status: "Running", completedItems: 5 },
  { jobId: 2, siteName: "Alpha", status: "Completed", completedItems: 20 },
  { jobId: 3, siteName: "Mid", status: "Failed", completedItems: 1 },
];

describe("status table sort (OPS-20)", () => {
  it("sorts by siteName ascending", () => {
    const sorted = sortJobs(jobs, { key: "siteName", direction: "asc" });
    expect(sorted.map((j) => j.siteName)).toEqual(["Alpha", "Mid", "Zeta"]);
  });

  it("sorts by siteName descending", () => {
    const sorted = sortJobs(jobs, { key: "siteName", direction: "desc" });
    expect(sorted.map((j) => j.siteName)).toEqual(["Zeta", "Mid", "Alpha"]);
  });

  it("sorts numeric completedItems", () => {
    const sorted = sortJobs(jobs, {
      key: "completedItems",
      direction: "asc",
    });
    expect(sorted.map((j) => j.completedItems)).toEqual([1, 5, 20]);
  });

  it("does not mutate input", () => {
    const copy = [...jobs];
    sortJobs(jobs, { key: "status", direction: "asc" });
    expect(jobs.map((j) => j.jobId)).toEqual(copy.map((j) => j.jobId));
  });

  it("toggles sort direction on same key", () => {
    const next = nextSortState({ key: "status", direction: "asc" }, "status");
    expect(next).toEqual({ key: "status", direction: "desc" });
  });

  it("starts ascending when switching keys", () => {
    const next = nextSortState({ key: "status", direction: "desc" }, "siteName");
    expect(next).toEqual({ key: "siteName", direction: "asc" });
  });

  it("shows sort indicator only for active column", () => {
    const state = { key: "status" as const, direction: "desc" as const };
    expect(sortIndicator(state, "status")).toBe(" ▼");
    expect(sortIndicator(state, "siteName")).toBe("");
  });
});
