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
 * CalendarTwo source-filter keyboard pins (issue #6992, residual of
 * slice 3 #6946, parent #6919; plan #6687, audit #5627).
 *
 * <p>The widget source-filter controls were focus-less {@code <div>}
 * elements with inline {@code onClick} handlers — unreachable by
 * keyboard. Worse, the handler they named
 * ({@code hideCalendarSource}) was declared inside a
 * {@code DOMContentLoaded} closure, so even the mouse path resolved
 * to a {@code ReferenceError} at click time. Slice #6992 rebuilds
 * the controls as native {@code <button>} elements with a global
 * vanilla {@code hideCalendarSource} plus delegated click wiring.
 * These pins lock that contract against the snippet source:
 *
 * <ul>
 *   <li>No {@code <div>} source-filter control with an inline
 *       {@code onClick} remains.</li>
 *   <li>Filter controls are {@code type="button"} (they live inside
 *       a {@code <form>}, so the default submit type would reload
 *       the page), carry the layer class for color, expose the
 *       layer key via {@code data-calendar-source}, and start
 *       {@code aria-pressed="true"} (layers visible by default).</li>
 *   <li>{@code hideCalendarSource} is global, syncs
 *       {@code aria-pressed} with the disabled state, returns the
 *       off-state, and never hides the {@code <button>} controls
 *       themselves (they share the layer class for color but must
 *       stay visible so focus is never dropped).</li>
 *   <li>Multi-widget safe ({@code #7029} review): the wiring binds
 *       only this instance's button container (plus a
 *       {@code percBound} double-fire guard), and the clicked button
 *       is passed through so {@code aria-pressed} syncs on the
 *       activated instance — N widgets no longer bind every filter
 *       N times.</li>
 *   <li>Keyboard focus is visible ({@code :focus-visible} ring only
 *       — the mouse path stays pixel-identical) and buttons inherit
 *       the surrounding type.</li>
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

describe("calendarTwo source-filter keyboard path (issue #6992)", () => {
  it("source-filter controls are native buttons, not divs with onClick", () => {
    const snippet = fs.readFileSync(SNIPPET, "utf8");
    // The inline mouse-only path is gone entirely.
    expect(snippet).not.toContain('onClick="hideCalendarSource');
    // Both filter controls (per-source + Percussion-events) render as
    // type="button" — the surrounding <form> makes the explicit type
    // load-bearing (no accidental submit reload).
    const buttons = snippet.match(/<button[^>]*perc-calendar-button[^>]*>/g) ?? [];
    expect(buttons.length).toBe(2);
    for (const button of buttons) {
      expect(button).toContain('type="button"');
      expect(button).toContain('aria-pressed="true"');
      expect(button).toContain("data-calendar-source=");
    }
    // The filter bar is a labelled group, mirroring the view-toggle bar.
    expect(snippet).toContain('class="perc-button-container" role="group"');
    expect(snippet).toContain('aria-label="Calendar sources"');
  });

  it("hideCalendarSource is global and syncs aria-pressed without hiding the controls", () => {
    const snippet = fs.readFileSync(SNIPPET, "utf8");
    // Global declaration — callable from delegated listeners and any
    // direct call (the old closure-scoped copy was a ReferenceError).
    // The second parameter carries the clicked button so aria-pressed
    // syncs on the activated instance when several widgets share the
    // page (button ids are not widget-scoped yet — see #7034).
    expect(snippet).toContain(
      "function hideCalendarSource(calendarClass, buttonEl) {",
    );
    expect(snippet).toContain(
      "var button = buttonEl || document.getElementById(calendarClass + '-button');",
    );
    // Pressed state mirrors visibility; the caller learns the off-state.
    expect(snippet).toContain(
      "button.setAttribute('aria-pressed', nowOff ? 'false' : 'true')",
    );
    expect(snippet).toContain(
      "button.classList.toggle('perc-disabled-calendar-button')",
    );
    // The controls share the layer class for color — the layer sweep
    // must skip them so activation never drops focus.
    expect(snippet).toContain("tagName !== 'BUTTON'");
    expect(snippet).toContain(".classList.toggle('perc-hide-calendar')");
    // Delegated wiring survives re-renders of the container.
    expect(snippet).toContain("data-calendar-source]");
    expect(snippet).toContain("addEventListener('click'");
    // Multi-widget scoping (#7029 review): each rendered script binds
    // only its own container, guarded against double-binding, and
    // forwards the clicked button for per-instance pressed sync.
    expect(snippet).toContain("querySelector('#perc-button-container-");
    expect(snippet).toContain("dataset.percBound");
    expect(snippet).toContain(
      "hideCalendarSource(this.getAttribute('data-calendar-source'), this);",
    );
  });

  it("keyboard focus is visible without changing the mouse path", () => {
    const snippet = fs.readFileSync(SNIPPET, "utf8");
    // :focus-visible only — mouse clicks never paint the ring.
    expect(snippet).toContain(".perc-calendar-button:focus-visible");
    expect(snippet).toContain("outline: 2px solid #005fcc;");
    expect(snippet).toContain("outline-offset: 2px;");
    // Buttons do not inherit page type by default — pin the family so
    // the div -> button swap renders identically.
    expect(snippet).toContain("font-family: inherit;");
  });
});
