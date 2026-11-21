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
 * CalendarTwo v6 print-stylesheet equivalence (issue #6945, slice 2 of
 * #6919; parent plan #6687, audit #5627; closes the #6804 slice-2b
 * remnant).
 *
 * <p>The snippet links {@code fullcalendar.print.css} with
 * {@code media="print"}, but the file shipped 2.x-era rules targeting the
 * FullCalendar 2.9.1 grid ({@code .fc-row},
 * {@code .fc-content-skeleton}, {@code .fc-time-grid},
 * {@code .fc-agenda-view}, {@code .fc-slats}, bare {@code .fc-resizer}) —
 * hooks the stock v6.1.15 payload (slice 1, #6944) never renders, so print
 * output was unstyled. Slice 2 retargets every grid rule at the v6
 * classes the shipped bundle actually renders.
 *
 * <p>This file pins the v6 equivalence: the print file keeps its slot and
 * {@code media="print"} link, carries no 2.x-only selector, covers the v6
 * daygrid/timegrid hooks the shipped bundle renders, and hides the
 * snippet interactive chrome. The v6 bundle itself ships zero CSS (the
 * slice-2a mapping assertion), so byte-identity pins do not cover this
 * Percussion-authored file — this test is its lock instead.
 */
import { describe, expect, it } from "vitest";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));

// src/test/js -> frontend root, then up to the repo root so the
// perc-packages payload + snippet resolve portably.
const FRONTEND_ROOT = path.dirname(path.dirname(path.dirname(__dirname)));
const REPO_ROOT = path.join(FRONTEND_ROOT, "..", "..", "..", "..");

const CALENDARTWO_BASE = path.join(
  "modules",
  "perc-packages",
  "src",
  "main",
  "resources",
  "Packages",
  "perc.widget.calendar",
);
const PRINT_CSS = path.join(
  REPO_ROOT,
  CALENDARTWO_BASE,
  "sys__UserDependency--web_resources",
  "widgets",
  "calendarTwo",
  "css",
  "fullcalendar.print.css",
);
const FULLCALENDAR_MIN = path.join(
  REPO_ROOT,
  CALENDARTWO_BASE,
  "sys__UserDependency--web_resources",
  "widgets",
  "calendarTwo",
  "js",
  "fullcalendar.min.js",
);
const SNIPPET = path.join(
  REPO_ROOT,
  CALENDARTWO_BASE,
  "widgets",
  "percCalendarTwo",
  "templates",
  "percCalendarTwoSnippet.vm",
);

// 2.x-only selectors: none of these hooks exists in the v6 renderer, so
// none may appear in the print file (checked as literal substrings; the
// v6 replacements deliberately avoid these spellings, e.g. .fc-event-resizer
// never collides with bare .fc-resizer).
const LEGACY_2X_SELECTORS = [
  ".fc-row",
  ".fc-content-skeleton",
  ".fc-more-cell",
  ".fc-time-grid",
  ".fc-agenda-view",
  ".fc-slats",
  ".fc-resizer",
  ".fc-view-container",
  ".fc-title",
  "fc-limited",
  ".fc-bgevent",
  ".fc-helper-skeleton",
];

// v6 hooks the print file must cover: each one is cross-checked against
// the shipped bundle bytes below.
const V6_PRINT_HOOKS = [
  ".fc-daygrid-day",
  ".fc-daygrid-day-events",
  ".fc-daygrid-more-link",
  ".fc-timegrid",
  ".fc-timegrid-axis",
  ".fc-timegrid-slots",
  ".fc-timegrid-event",
  ".fc-event-resizer",
  ".fc-popover",
  ".fc-scroller",
  ".fc-bg",
  ".fc-highlight",
];

describe("calendarTwo v6 print stylesheet equivalence (issue #6945)", () => {
  it("snippet still links the print file with media=print", () => {
    const snippet = fs.readFileSync(SNIPPET, "utf8");
    expect(snippet).toContain("fullcalendar.print.css");
    expect(snippet).toContain('media="print"');
  });

  it("print file carries no 2.x-only selector", () => {
    const raw = fs.readFileSync(PRINT_CSS, "utf8");
    // The header comment documents the 2.x -> v6 mapping, so it names
    // the legacy hooks by design — scan the rules only.
    const css = raw.replace(/\/\*[\s\S]*?\*\//g, "");
    for (const legacy of LEGACY_2X_SELECTORS) {
      expect(
        css.includes(legacy),
        `print css must not target the 2.x hook ${legacy}`,
      ).toBe(false);
    }
  });

  it("print file covers the v6 daygrid/timegrid hooks", () => {
    const css = fs.readFileSync(PRINT_CSS, "utf8");
    for (const hook of V6_PRINT_HOOKS) {
      expect(css.includes(hook), `print css must cover ${hook}`).toBe(true);
    }
    // Event flattening + page-break behavior carries over from 2.x.
    expect(css).toMatch(/page-break-inside:\s*avoid/);
    // Interactive chrome stays hidden on paper.
    expect(css).toContain(".fc button");
    expect(css).toContain(".perc-toggle-button-container");
    expect(css).toContain(".perc-button-container");
  });

  it("every v6 hook in the print file exists in the shipped bundle", () => {
    const bundle = fs.readFileSync(FULLCALENDAR_MIN, "utf8");
    for (const hook of V6_PRINT_HOOKS) {
      // Strip the leading dot: the bundle carries the class names in its
      // injected CSS + renderer strings.
      expect(
        bundle.includes(hook.slice(1)),
        `shipped bundle must render ${hook}`,
      ).toBe(true);
    }
  });
});
