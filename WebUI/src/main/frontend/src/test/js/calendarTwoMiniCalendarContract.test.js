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

/**
 * CalendarTwo mini-calendar {@code fullCalendarPage} link-URL contract
 * (issue #6945, slice 2 of #6919; parent plan #6687, audit #5627; closes
 * the #6804 slice-3 remnant).
 *
 * <p>The mini branch of {@code percCalendarTwoSnippet.vm} renders a
 * jQuery-UI datepicker whose {@code onSelect} navigates to the full
 * calendar page for the picked day:
 * {@code fullCalendarPage + "?startDate=" + clickedDate}, where
 * {@code clickedDate} is the datepicker text with {@code /} replaced by
 * {@code ,}. Both template branches (edit/preview sample data and the
 * published DTS-backed branch) carry the identical contract — the
 * occurrence pins below fail loudly if one branch drifts.
 */
import { describe, expect, it } from "vitest";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));

// src/test/js -> frontend root, then up to the repo root so the snippet
// resolves portably.
const FRONTEND_ROOT = path.dirname(path.dirname(path.dirname(__dirname)));
const REPO_ROOT = path.join(FRONTEND_ROOT, "..", "..", "..", "..");

const SNIPPET = path.join(
  REPO_ROOT,
  "modules",
  "perc-packages",
  "src",
  "main",
  "resources",
  "Packages",
  "perc.widget.calendar",
  "widgets",
  "percCalendarTwo",
  "templates",
  "percCalendarTwoSnippet.vm",
);

/**
 * Mirrors the template contract line-for-line:
 * {@code clickedDate = dateText.replace(/\//g, ',')} then
 * {@code fullCalendarPage + "?startDate=" + clickedDate}.
 */
function miniCalendarLinkTarget(fullCalendarPage, dateText) {
  const clickedDate = dateText.replace(/\//g, ",");
  return fullCalendarPage + "?startDate=" + clickedDate;
}

describe("calendarTwo mini-calendar link-URL contract (issue #6945)", () => {
  it("both template branches route onSelect through the fullCalendarPage contract", () => {
    const snippet = fs.readFileSync(SNIPPET, "utf8");
    // The datepicker selection handler exists in the edit/preview branch
    // and the published branch alike.
    const onSelectCount = (
      snippet.match(/onSelect:\s*function\s*\(\s*dateText\s*,\s*inst\s*\)/g) ??
      []
    ).length;
    expect(onSelectCount).toBe(2);
    // Both branches build the identical navigation target.
    const contractCount = snippet.split(
      'fullCalendarPage + "?startDate=" + clickedDate',
    ).length - 1;
    expect(contractCount).toBe(2);
    // Both branches normalize slashes to commas before navigating.
    const normalizeCount = snippet.split("dateText.replace(/\\//g, ',')").length - 1;
    expect(normalizeCount).toBe(2);
    // Highlight hook for days with events is intact in both branches.
    const highlightCount = (
      snippet.match(/beforeShowDay:\s*highlightDays/g) ?? []
    ).length;
    expect(highlightCount).toBe(2);
  });

  it("slash date text becomes a comma-separated startDate param", () => {
    expect(miniCalendarLinkTarget("/calendar", "11/21/2026")).toBe(
      "/calendar?startDate=11,21,2026",
    );
    expect(miniCalendarLinkTarget("/sites/us2/calendar", "01/05/2027")).toBe(
      "/sites/us2/calendar?startDate=01,05,2027",
    );
  });

  it("startDate param round-trips to month, day, and year parts", () => {
    const target = miniCalendarLinkTarget("/calendar", "07/04/2026");
    const startDate = new URL(target, "https://localhost").searchParams.get(
      "startDate",
    );
    expect(startDate).toBe("07,04,2026");
    expect(startDate.split(",")).toEqual(["07", "04", "2026"]);
  });
});
