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
 * CalendarTwo list-state string i18n (issue #6993, residual of
 * [6919/3] cleanup #6946; parent #6919, plan #6687, audit #5627).
 *
 * <p>Slice 2 (#6945) rendered the list-layout loading and empty rows as
 * hardcoded English inside {@code percCalendarTwoSnippet.vm}, so a
 * non-English widget still announced English text through the
 * {@code aria-live="polite"} list region. This slice sources both
 * strings from editor-overridable widget preferences
 * ({@code perc_list_loading_text} / {@code perc_list_empty_text}) with
 * English defaults preserving the slice-2 output:
 *
 * <ul>
 *   <li>The snippet renders the preference values (JS-escaped so editor
 *       quotes cannot break the row strings) instead of literals.</li>
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

describe("calendarTwo list-state strings are preference-driven (issue #6993)", () => {
  it("loading + empty rows render the preference values, not literals", () => {
    const snippet = fs.readFileSync(SNIPPET, "utf8");
    expect(snippet).toContain(
      "perc-event-list-loading\">$tools.esc.javascript($perc_list_loading_text)",
    );
    expect(snippet).toContain(
      "perc-event-list-empty\">$tools.esc.javascript($perc_list_empty_text)",
    );
    // No hardcoded English remains in the row markup itself (the only
    // literals allowed are the fallback-guard defaults below).
    for (const line of snippet.split("\n")) {
      if (
        line.includes("perc-event-list-loading\">") ||
        line.includes("perc-event-list-empty\">")
      ) {
        expect(line).not.toContain("Loading events");
        expect(line).not.toContain("No upcoming events.");
      }
    }
  });

  it("fallback guards preserve the English output for instances without prefs", () => {
    const snippet = fs.readFileSync(SNIPPET, "utf8");
    expect(snippet).toContain(
      "#if(!$perc_list_loading_text)#set($perc_list_loading_text = 'Loading events…')#end",
    );
    expect(snippet).toContain(
      "#if(!$perc_list_empty_text)#set($perc_list_empty_text = 'No upcoming events.')#end",
    );
  });

  it("component-package declares both preferences with English defaults", () => {
    const pkg = JSON.parse(fs.readFileSync(COMPONENT_PACKAGE, "utf8"));
    const prefs = Object.fromEntries(
      pkg.userPreferences.map((p) => [p.name, p]),
    );
    expect(prefs["perc_list_loading_text"].defaultValue).toBe(
      "Loading events…",
    );
    expect(prefs["perc_list_empty_text"].defaultValue).toBe(
      "No upcoming events.",
    );
    const bindings = pkg.templates
      .flatMap((t) => t.bindings || [])
      .map((b) => b.variable);
    expect(bindings).toContain("perc_list_loading_text");
    expect(bindings).toContain("perc_list_empty_text");
  });
});
