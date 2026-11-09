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
  buildEditionScheduleBody,
  editionScheduleInputDefaults,
  editionScheduleServerDates,
  editionsAfterSuccessfulSchedule,
  formatEditionScheduleLabel,
} from "@/publishing/editionSchedule";

const source: EditionSummary = {
  editionId: "12",
  name: "NightEd",
  siteId: "1",
  comment: "keep-me",
  priority: 1,
  startDate: "11/09/2026 08:00 am",
  endDate: "11/10/2026 05:00 pm",
};

const other: EditionSummary = {
  editionId: "13",
  name: "OtherEd",
  siteId: "1",
  comment: "other",
  priority: 2,
};

describe("buildEditionScheduleBody", () => {
  it("sends both dates only, including an empty clear", () => {
    expect(
      buildEditionScheduleBody({
        startDate: "11/09/2026 08:00 am",
        endDate: "11/10/2026 05:00 pm",
      }),
    ).toEqual({
      startDate: "11/09/2026 08:00 am",
      endDate: "11/10/2026 05:00 pm",
    });
    const cleared = buildEditionScheduleBody({ startDate: "", endDate: "" });
    expect(cleared).toEqual({ startDate: "", endDate: "" });
    expect(cleared).not.toHaveProperty("name");
    expect(cleared).not.toHaveProperty("priority");
    expect(cleared).not.toHaveProperty("comment");
    expect(cleared).not.toHaveProperty("siteId");
    expect(cleared).not.toHaveProperty("editionId");
  });
});

describe("editionScheduleServerDates", () => {
  it("converts datetime-local inputs to server wall time", () => {
    expect(
      editionScheduleServerDates("2026-11-09T08:00", "2026-11-10T17:00"),
    ).toEqual({
      startDate: "11/09/2026 08:00 am",
      endDate: "11/10/2026 05:00 pm",
    });
    expect(editionScheduleServerDates("", "")).toEqual({
      startDate: "",
      endDate: "",
    });
  });
});

describe("editionScheduleInputDefaults", () => {
  it("prefills datetime-local inputs from the stored schedule", () => {
    expect(editionScheduleInputDefaults(source)).toEqual({
      startLocal: "2026-11-09T08:00",
      endLocal: "2026-11-10T17:00",
    });
    expect(
      editionScheduleInputDefaults({ editionId: "12", name: "NightEd" }),
    ).toEqual({ startLocal: "", endLocal: "" });
  });
});

describe("formatEditionScheduleLabel", () => {
  it("labels both dates, one side, or nothing", () => {
    expect(
      formatEditionScheduleLabel("11/09/2026 08:00 am", "11/10/2026 05:00 pm"),
    ).toBe("11/09/2026 08:00 am → 11/10/2026 05:00 pm");
    expect(formatEditionScheduleLabel("11/09/2026 08:00 am", null)).toBe(
      "from 11/09/2026 08:00 am",
    );
    expect(formatEditionScheduleLabel(null, "11/10/2026 05:00 pm")).toBe(
      "until 11/10/2026 05:00 pm",
    );
    expect(formatEditionScheduleLabel(null, null)).toBe("");
    expect(formatEditionScheduleLabel("", "  ")).toBe("");
  });
});

describe("editionsAfterSuccessfulSchedule", () => {
  it("uses the refreshed list when the reload succeeded", () => {
    const refreshed = [
      { ...source, startDate: "11/11/2026 08:00 am" },
      other,
    ];
    expect(
      editionsAfterSuccessfulSchedule(
        refreshed,
        "12",
        { startDate: "11/11/2026 08:00 am", endDate: "" },
        [source, other],
      ),
    ).toBe(refreshed);
  });

  it("patches only the schedule when the reload failed", () => {
    expect(
      editionsAfterSuccessfulSchedule(
        null,
        "12",
        { startDate: "", endDate: "" },
        [source, other],
      ),
    ).toEqual([{ ...source, startDate: "", endDate: "" }, other]);
  });
});
