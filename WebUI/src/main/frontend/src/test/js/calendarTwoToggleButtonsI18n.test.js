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
 * CalendarTwo view-toggle button text i18n (issue #7118, residual of
 * #7030; parent #7030, plan #6687, audit #5627).
 *
 * <p>Slice #7030 keyed the two {@code role="group"} accessible names
 * through widget preferences, but the view-toggle buttons themselves
 * still rendered hardcoded English text content ("Calendar View" /
 * "List View") — a non-English widget with localized group labels
 * still announced English button names. This slice sources both
 * labels from editor-overridable widget preferences
 * ({@code perc_calendar_view_button_label} /
 * {@code perc_list_view_button_label}) with English defaults
 * preserving the pre-existing output:
 *
 * <ul>
 *   <li>The snippet renders the preference values (HTML-escaped so
 *       editor quotes cannot break the button markup) instead of
 *       literals.</li>
 *   <li>Velocity fallback guards keep the English output for
 *       pre-existing widget instances that store no preference.</li>
 *   <li>{@code component-package.json} declares both preferences with
 *       the English defaults and wires the assembler bindings.</li>
 * </ul>
 */
import { describe, expect, it } from "vitest";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));

// src/test/js -> frontend root, then up to the repo root so the widget
// package resolves portably.
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

const COMPONENT_PACKAGE = path.join(
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
  "component-package.json",
);

describe("calendarTwo view-toggle button text is preference-driven (issue #7118)", () => {
  it("toggle buttons render the preference values, not literals", () => {
    const snippet = fs.readFileSync(SNIPPET, "utf8");
    expect(snippet).toContain(
      ">$tools.esc.html($perc_calendar_view_button_label)</button>",
    );
    expect(snippet).toContain(
      ">$tools.esc.html($perc_list_view_button_label)</button>",
    );
    // No hardcoded English remains in the button markup itself (the
    // only literals allowed are the fallback-guard defaults below).
    for (const line of snippet.split("\n")) {
      if (
        line.includes("showCalendarButton-") ||
        line.includes("showListButton-")
      ) {
        expect(line).not.toContain(">Calendar View<");
        expect(line).not.toContain(">List View<");
      }
    }
  });

  it("fallback guards preserve the English output for instances without prefs", () => {
    const snippet = fs.readFileSync(SNIPPET, "utf8");
    expect(snippet).toContain(
      "#if(!$perc_calendar_view_button_label)#set($perc_calendar_view_button_label = 'Calendar View')#end",
    );
    expect(snippet).toContain(
      "#if(!$perc_list_view_button_label)#set($perc_list_view_button_label = 'List View')#end",
    );
  });

  it("component-package declares both preferences with English defaults", () => {
    const pkg = JSON.parse(fs.readFileSync(COMPONENT_PACKAGE, "utf8"));
    const prefs = Object.fromEntries(
      pkg.userPreferences.map((p) => [p.name, p]),
    );
    expect(prefs["perc_calendar_view_button_label"].defaultValue).toBe(
      "Calendar View",
    );
    expect(prefs["perc_list_view_button_label"].defaultValue).toBe(
      "List View",
    );
    const bindings = pkg.templates
      .flatMap((t) => t.bindings || [])
      .map((b) => b.variable);
    expect(bindings).toContain("perc_calendar_view_button_label");
    expect(bindings).toContain("perc_list_view_button_label");
  });
});
