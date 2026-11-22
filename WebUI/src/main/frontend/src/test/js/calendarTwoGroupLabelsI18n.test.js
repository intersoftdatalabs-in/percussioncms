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
 * CalendarTwo group-label string i18n (issue #7030, residual of
 * #6992; parent #6992, plan #6687, audit #5627).
 *
 * <p>Slice #6992 put the source-filter buttons in a
 * {@code role="group"} labelled "Calendar sources" next to the
 * pre-existing "Calendar layout" view-toggle group, so a non-English
 * widget exposed English accessible names for both control groups.
 * This slice sources both labels from editor-overridable widget
 * preferences ({@code perc_source_filter_group_label} /
 * {@code perc_layout_toggle_group_label}) with English defaults
 * preserving the pre-existing output:
 *
 * <ul>
 *   <li>The snippet renders the preference values (HTML-escaped so
 *       editor quotes cannot break the {@code aria-label} attribute)
 *       instead of literals.</li>
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

describe("calendarTwo group labels are preference-driven (issue #7030)", () => {
  it("toggle + filter groups render the preference values, not literals", () => {
    const snippet = fs.readFileSync(SNIPPET, "utf8");
    expect(snippet).toContain(
      'aria-label="$tools.esc.html($perc_layout_toggle_group_label)"',
    );
    expect(snippet).toContain(
      'aria-label="$tools.esc.html($perc_source_filter_group_label)"',
    );
    // No hardcoded English remains in the group markup itself (the only
    // literals allowed are the fallback-guard defaults below).
    for (const line of snippet.split("\n")) {
      if (line.includes('role="group"') && line.includes("aria-label=")) {
        expect(line).not.toContain("Calendar layout");
        expect(line).not.toContain("Calendar sources");
      }
    }
  });

  it("fallback guards preserve the English output for instances without prefs", () => {
    const snippet = fs.readFileSync(SNIPPET, "utf8");
    expect(snippet).toContain(
      "#if(!$perc_source_filter_group_label)#set($perc_source_filter_group_label = 'Calendar sources')#end",
    );
    expect(snippet).toContain(
      "#if(!$perc_layout_toggle_group_label)#set($perc_layout_toggle_group_label = 'Calendar layout')#end",
    );
  });

  it("component-package declares both preferences with English defaults", () => {
    const pkg = JSON.parse(fs.readFileSync(COMPONENT_PACKAGE, "utf8"));
    const prefs = Object.fromEntries(
      pkg.userPreferences.map((p) => [p.name, p]),
    );
    expect(prefs["perc_source_filter_group_label"].defaultValue).toBe(
      "Calendar sources",
    );
    expect(prefs["perc_layout_toggle_group_label"].defaultValue).toBe(
      "Calendar layout",
    );
    const bindings = pkg.templates
      .flatMap((t) => t.bindings || [])
      .map((b) => b.variable);
    expect(bindings).toContain("perc_source_filter_group_label");
    expect(bindings).toContain("perc_layout_toggle_group_label");
  });
});
