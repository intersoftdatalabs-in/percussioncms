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
  editionIsScheduled,
  filterDesignEditions,
  filterEditionsBySchedule,
  filterEditionsBySearch,
} from "@/publishing/editionListFilter";

const NIGHTLY = {
  editionId: "1",
  name: "Nightly Full",
  comment: "ships at midnight",
  startDate: "11/10/2026 12:00 AM",
  endDate: "",
};

const HOURLY = {
  editionId: "2",
  name: "Hourly Incremental",
  comment: "",
  startDate: "",
  endDate: "",
};

const WEEKLY = {
  editionId: "3",
  name: "Weekly Digest",
  comment: "weekend rollup",
  startDate: "",
  endDate: "11/20/2026 12:00 AM",
};

const ALL = [NIGHTLY, HOURLY, WEEKLY];

describe("editionIsScheduled", () => {
  it("is true when either schedule side is stored", () => {
    expect(editionIsScheduled(NIGHTLY)).toBe(true);
    expect(editionIsScheduled(WEEKLY)).toBe(true);
    expect(editionIsScheduled(HOURLY)).toBe(false);
    expect(editionIsScheduled({ name: "bare" })).toBe(false);
  });
});

describe("filterEditionsBySearch", () => {
  it("returns every edition when the query is blank", () => {
    expect(filterEditionsBySearch(ALL, "")).toEqual(ALL);
    expect(filterEditionsBySearch(ALL, "   ")).toEqual(ALL);
  });

  it("matches name case-insensitively", () => {
    expect(filterEditionsBySearch(ALL, "nightly").map((e) => e.editionId)).toEqual([
      "1",
    ]);
    expect(filterEditionsBySearch(ALL, "HOURLY").map((e) => e.editionId)).toEqual([
      "2",
    ]);
  });

  it("matches comment text as well as name", () => {
    expect(filterEditionsBySearch(ALL, "rollup").map((e) => e.editionId)).toEqual([
      "3",
    ]);
    expect(filterEditionsBySearch(ALL, "midnight").map((e) => e.editionId)).toEqual([
      "1",
    ]);
  });

  it("returns an empty list when nothing matches", () => {
    expect(filterEditionsBySearch(ALL, "nope")).toEqual([]);
  });
});

describe("filterEditionsBySchedule", () => {
  it("returns every edition for the all facet", () => {
    expect(filterEditionsBySchedule(ALL, "all")).toEqual(ALL);
  });

  it("keeps only editions with a stored schedule date", () => {
    expect(
      filterEditionsBySchedule(ALL, "scheduled").map((e) => e.editionId),
    ).toEqual(["1", "3"]);
  });

  it("keeps only editions with no schedule date", () => {
    expect(
      filterEditionsBySchedule(ALL, "unscheduled").map((e) => e.editionId),
    ).toEqual(["2"]);
  });
});

describe("filterDesignEditions", () => {
  it("combines the schedule facet with the search query", () => {
    expect(
      filterDesignEditions(ALL, "nightly", "scheduled").map((e) => e.editionId),
    ).toEqual(["1"]);
    expect(
      filterDesignEditions(ALL, "nightly", "unscheduled").map(
        (e) => e.editionId,
      ),
    ).toEqual([]);
  });

  it("returns the original list when both controls are at rest", () => {
    expect(filterDesignEditions(ALL, "", "all")).toEqual(ALL);
  });
});
