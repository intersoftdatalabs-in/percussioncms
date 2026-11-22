/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0. (the "License");
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
 * CalendarTwo multi-widget filter scoping (issue #7034, found during
 * review of #7029, parent residual of #6992 / #6919; plan #6687).
 *
 * <p>Two calendarTwo widgets on one page used to share page-global
 * filter ids ({@code perc-<class>-button},
 * {@code perc-calendar-other-button}) and page-wide event-layer classes
 * ({@code perc-<class>}, {@code perc-calendar-other}), so toggling a
 * source in one widget hid that source's events in every widget and
 * {@code getElementById} only ever resolved the first button. The
 * snippet now suffixes every filter button id and every event-layer
 * class with the widget instance's
 * {@code ${calendarContentId}-${widgetId}}, and the filter wiring binds
 * only its own button container. These pins lock that contract against
 * the snippet source:
 *
 * <ul>
 *   <li>No unscoped filter button id remains (no duplicate DOM ids
 *       across instances).</li>
 *   <li>Every layer class the calendar stamps on events (Google
 *       {@code className} entries and the Percussion-events layer) is a
 *       two-entry array: the unsuffixed class for legacy site/theme
 *       hooks plus the instance-suffixed class the sweep keys off, as
 *       are the color rules that paint them. Filter buttons keep the
 *       same dual class while ids stay instance-suffixed.</li>
 *   <li>{@code data-calendar-source} carries the full scoped layer key
 *       so the class sweep toggles only the activating widget's
 *       layers.</li>
 *   <li>The keyboard contract from #6992 survives scoping: native
 *       {@code type="button"} controls, global
 *       {@code hideCalendarSource} with {@code aria-pressed} sync and a
 *       {@code BUTTON} skip in the layer sweep, per-container delegated
 *       wiring with a double-bind guard, and a
 *       {@code :focus-visible} ring.</li>
 * </ul>
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

describe("calendarTwo multi-widget filter scoping (issue #7034)", () => {
  it("emits no page-global filter ids", () => {
    const snippet = fs.readFileSync(SNIPPET, "utf8");
    // The inline mouse-only path is gone entirely.
    expect(snippet).not.toContain('onClick="hideCalendarSource');
    // The pre-scoping duplicate-id shape must not render: the
    // Percussion-events button id without an instance suffix.
    expect(snippet).not.toContain('id="perc-calendar-other-button"');
    // Every filter button id carries the instance suffix, so N widgets
    // on one page never duplicate an id.
    expect(snippet).toContain(
      "id=\"perc-${googleSource.getString('className')}-${calendarContentId}-${widgetId}-button\"",
    );
    expect(snippet).toContain(
      'id="perc-calendar-other-${calendarContentId}-${widgetId}-button"',
    );
  });

  it("scopes every event-layer class and color rule to the widget instance", () => {
    const snippet = fs.readFileSync(SNIPPET, "utf8");
    // Google feed layers stamped by FullCalendar via className: the array
    // carries the scoped class for the per-instance sweep plus the
    // unsuffixed class for legacy site/theme hooks (#7034 review).
    expect(snippet).toContain(
      "className: ['perc-${googleSource.getString(\"className\")}', 'perc-${googleSource.getString(\"className\")}-${calendarContentId}-${widgetId}']",
    );
    // Percussion-events layer stamped by the DTS-backed event source.
    expect(snippet).toContain(
      "className: ['perc-calendar-other', 'perc-calendar-other-${calendarContentId}-${widgetId}'],",
    );
    // The per-source color rules paint the scoped classes (the style
    // block renders once per widget instance).
    expect(snippet).toContain(
      '.perc-$googleSource.getString("className")-${calendarContentId}-${widgetId} {',
    );
    expect(snippet).toContain(
      ".perc-calendar-other-${calendarContentId}-${widgetId} {",
    );
    // The filter key handed to the sweep is the full scoped layer key.
    expect(snippet).toContain(
      "data-calendar-source=\"perc-${googleSource.getString('className')}-${calendarContentId}-${widgetId}\"",
    );
    expect(snippet).toContain(
      'data-calendar-source="perc-calendar-other-${calendarContentId}-${widgetId}"',
    );
  });

  it("keeps legacy hooks while ids stay instance-suffixed", () => {
    const snippet = fs.readFileSync(SNIPPET, "utf8");
    // Buttons keep the unsuffixed layer class alongside the scoped one
    // so legacy CSS keeps matching; the sweep keys off the scoped
    // data-calendar-source (#7034 review).
    expect(snippet).toContain(
      "class=\"perc-calendar-button perc-$googleSource.getString('className') perc-$googleSource.getString('className')-${calendarContentId}-${widgetId}\"",
    );
    expect(snippet).toContain(
      'class="perc-calendar-button perc-calendar-other perc-calendar-other-${calendarContentId}-${widgetId}"',
    );
    // Ids stay instance-suffixed: re-emitting the unsuffixed button ids
    // would reintroduce the duplicate-DOM-id defect this slice fixes.
    expect(snippet).not.toContain('id="perc-calendar-other-button"');
  });

  it("keeps the keyboard contract while toggling per instance", () => {
    const snippet = fs.readFileSync(SNIPPET, "utf8");
    // Native buttons inside the surrounding form (explicit type is
    // load-bearing), pressed state perceivable by default.
    const buttons = snippet.match(/<button[^>]*perc-calendar-button[^>]*>/g) ?? [];
    expect(buttons.length).toBe(2);
    for (const button of buttons) {
      expect(button).toContain('type="button"');
      expect(button).toContain('aria-pressed="true"');
      expect(button).toContain("data-calendar-source=");
    }
    // The filter bar stays a labelled group. The accessible name is
    // preference-driven (#7030): the group renders
    // $perc_source_filter_group_label (HTML-escaped) with a Velocity
    // fallback guard preserving the English default.
    expect(snippet).toContain('class="perc-button-container" role="group"');
    expect(snippet).toContain(
      'aria-label="$tools.esc.html($perc_source_filter_group_label)"',
    );
    // Global toggle: caller-supplied button wins (per-instance pressed
    // sync), otherwise the scoped-id fallback resolves this widget's
    // own button; the sweep skips the BUTTON controls so focus is never
    // dropped, and returns the off-state.
    expect(snippet).toContain(
      "function hideCalendarSource(calendarClass, buttonEl) {",
    );
    expect(snippet).toContain(
      "var button = buttonEl || document.getElementById(calendarClass + '-button');",
    );
    expect(snippet).toContain(
      "button.setAttribute('aria-pressed', nowOff ? 'false' : 'true')",
    );
    expect(snippet).toContain(
      "button.classList.toggle('perc-disabled-calendar-button')",
    );
    expect(snippet).toContain("tagName !== 'BUTTON'");
    expect(snippet).toContain(".classList.toggle('perc-hide-calendar')");
    // Per-instance delegated wiring with a double-bind guard (N script
    // runs bind N containers exactly once each).
    expect(snippet).toContain("querySelector('#perc-button-container-");
    expect(snippet).toContain("dataset.percBound");
    expect(snippet).toContain(
      "hideCalendarSource(this.getAttribute('data-calendar-source'), this);",
    );
    // Keyboard focus stays visible without touching the mouse path.
    expect(snippet).toContain(".perc-calendar-button:focus-visible");
    expect(snippet).toContain("outline: 2px solid #005fcc;");
    expect(snippet).toContain("outline-offset: 2px;");
    expect(snippet).toContain("font-family: inherit;");
  });
});
