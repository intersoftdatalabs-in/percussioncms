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
 * CalendarTwo Percussion-events filter button label fallback (issue
 * #7142, residual of #7118; parent #7118, grandparent #7030, plan
 * #6687, audit #5627).
 *
 * <p>Every other editor-overridable calendarTwo string preference
 * ships a Velocity fallback guard so pre-existing widget instances
 * (no stored pref) render the English default — but
 * {@code perc_event_button_label} had none, and the snippet renders
 * the Percussion-events filter button conditionally on it. For a
 * pre-existing instance with the button enabled but no stored pref,
 * Velocity resolves the pref to null and {@code null != ''} is true,
 * so the button rendered with <b>empty text content</b> — an unnamed,
 * focusable button inside the {@code Calendar sources} group. This
 * slice adds the guard (default {@code Other}) and HTML-escapes the
 * button text so editor quotes cannot break the button markup:
 *
 * <ul>
 *   <li>The snippet renders the preference value via
 *       {@code $tools.esc.html} instead of the raw reference.</li>
 *   <li>A Velocity fallback guard keeps the English output for
 *       pre-existing widget instances that store no preference.</li>
 *   <li>{@code component-package.json} already declares the
 *       preference with the English default and wires the assembler
 *       binding — this pin locks that contract.</li>
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

describe("calendarTwo Percussion-events button label fallback (issue #7142)", () => {
  it("filter button renders the escaped preference value, not the raw reference", () => {
    const snippet = fs.readFileSync(SNIPPET, "utf8");
    expect(snippet).toContain(
      ">$tools.esc.html($perc_event_button_label)</button>",
    );
    // No unescaped interpolation remains on the Percussion-events
    // button line.
    for (const line of snippet.split("\n")) {
      if (line.includes("perc-calendar-other-")) {
        expect(line).not.toContain(">$perc_event_button_label<");
      }
    }
  });

  it("fallback guard preserves the English default for instances without prefs", () => {
    const snippet = fs.readFileSync(SNIPPET, "utf8");
    expect(snippet).toContain(
      "#if(!$perc_event_button_label)#set($perc_event_button_label = 'Other')#end",
    );
  });

  it("component-package declares the preference with the English default", () => {
    const pkg = JSON.parse(fs.readFileSync(COMPONENT_PACKAGE, "utf8"));
    const prefs = Object.fromEntries(
      pkg.userPreferences.map((p) => [p.name, p]),
    );
    expect(prefs["perc_event_button_label"].defaultValue).toBe("Other");
    const bindings = pkg.templates
      .flatMap((t) => t.bindings || [])
      .map((b) => b.variable);
    expect(bindings).toContain("perc_event_button_label");
  });
});
