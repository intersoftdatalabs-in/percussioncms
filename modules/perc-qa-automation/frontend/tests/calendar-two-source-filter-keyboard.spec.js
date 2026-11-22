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
 * CalendarTwo source-filter keyboard operability (issue #6992, residual
 * of slice 3 #6946, parent #6919; plan #6687, audit #5627).
 *
 * <p>The widget source-filter controls were focus-less {@code <div>}
 * elements with inline {@code onClick} handlers — keyboard-only users
 * could not toggle the per-source event layers the mouse path
 * hides/shows. Slice #6992 rebuilds them as native
 * {@code <button type="button">} controls with a global
 * {@code hideCalendarSource} plus delegated click wiring and an
 * {@code aria-pressed} mirror.
 *
 * <p>Hermetic surface lock: renders the shipped snippet's real filter
 * markup shape (two filter buttons + event layers carrying the same
 * layer classes FullCalendar stamps on events) and evaluates the
 * <b>real {@code hideCalendarSource} source extracted from the
 * snippet at runtime</b> (brace matching from the declaration, the
 * same technique the list spec uses for {@code highlightDays}), so
 * the behavior under test is the snippet's own — not a copy. The
 * delegated wiring loop is ported per container (the snippet
 * registers it on {@code DOMContentLoaded}, which a
 * {@code setContent} harness cannot re-fire — the test evaluates the
 * port once per rendered widget instance, mirroring one handler run
 * per instance); its strings are pinned against the snippet by
 * {@code calendarTwoSourceFilterKeyboard.test.js}, which fails
 * Vitest loudly on drift.
 *
 * <p>Screenshots land in the gitignored {@code test-results/} tree
 * (repo convention); the curated {@code docs/ai-generated/proofs/}
 * PNGs referenced from the PR body are refreshed by intentional copy
 * only, so routine runs never dirty the worktree.
 *
 * <p>Run:
 * {@code npm run test:surface -- --path tests/calendar-two-source-filter-keyboard.spec.js}</p>
 */

const fs = require("node:fs");
const path = require("node:path");
const { test, expect } = require("@playwright/test");

const REPO_ROOT = path.join(__dirname, "..", "..", "..", "..");
const SNIPPET_PATH = path.join(
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
const PROOF_DIR = path.join(__dirname, "..", "test-results");

// Extracts the real hideCalendarSource body from the snippet source
// (brace matching from the declaration) so the page evaluates the
// actual snippet function rather than a copy. Matches on the open
// paren only so snippet signature widenings (e.g. the #7029-review
// buttonEl pass-through) do not break the extractor.
function extractHideCalendarSource(snippet) {
  const marker = "function hideCalendarSource(";
  const start = snippet.indexOf(marker);
  if (start < 0) {
    throw new Error("hideCalendarSource not found in snippet");
  }
  let depth = 0;
  for (let i = start; i < snippet.length; i += 1) {
    if (snippet[i] === "{") {
      depth += 1;
    } else if (snippet[i] === "}") {
      depth -= 1;
      if (depth === 0) {
        return snippet.slice(start, i + 1);
      }
    }
  }
  throw new Error("hideCalendarSource braces do not balance in snippet");
}

// Filter-bar markup mirrors the snippet with concrete widget ids
// (calendarContentId=cal1, widgetId=w1): two native filter buttons —
// one per google source, one for Percussion events — plus event layers
// carrying the same classes the v6 calendar stamps on events. The
// layer divs are not buttons, mirroring FullCalendar event nodes.
function filterMarkup() {
  return `
  <style>
    .perc-hide-calendar { display: none; }
    .perc-disabled-calendar-button { opacity: 0.6; }
    .perc-calendar-button:focus-visible {
      outline: 2px solid #005fcc;
      outline-offset: 2px;
    }
  </style>
  <div id="perc-button-container-cal1-w1" class="perc-button-container" role="group" aria-label="Calendar sources">
  <button type="button" id="perc-gcal-team-button" class="perc-calendar-button perc-gcal-team" data-calendar-source="perc-gcal-team" aria-pressed="true">Team Calendar</button>
  <button type="button" id="perc-calendar-other-button" class="perc-calendar-button perc-calendar-other" data-calendar-source="perc-calendar-other" aria-pressed="true">Percussion Events</button>
  </div>
  <div class="fc-event perc-gcal-team" id="layer-gcal-1">Team event one</div>
  <div class="fc-event perc-gcal-team" id="layer-gcal-2">Team event two</div>
  <div class="fc-event perc-calendar-other" id="layer-other-1">Percussion event</div>`;
}

function harnessHtml() {
  return `<!doctype html>
<html lang="en">
<head><meta charset="utf-8"><title>calendarTwo source-filter keyboard harness</title></head>
<body>
${filterMarkup()}
</body>
</html>`;
}

// Ports the snippet DOMContentLoaded wiring loop for one widget
// instance (pinned by the calendarTwoSourceFilterKeyboard Vitest):
// container-scoped delegated click handling off
// data-calendar-source with a double-bind guard, forwarding the
// clicked button for per-instance pressed sync.
function portedWiringScript(hideSource, containerId) {
  return `
    window.hideCalendarSource = (${hideSource});
    var container = document.querySelector('#${containerId}');
    var scope = container || document;
    var filters = scope.querySelectorAll('[data-calendar-source]');
    for (var i = 0; i < filters.length; i++) {
      if (filters[i].dataset && filters[i].dataset.percBound) {
        continue;
      }
      if (filters[i].dataset) {
        filters[i].dataset.percBound = '1';
      }
      filters[i].addEventListener('click', function () {
        window.hideCalendarSource(this.getAttribute('data-calendar-source'), this);
      });
    }
  `;
}

function listenForNoise(page, noise) {
  page.on("console", (message) => {
    if (message.type() === "error") {
      noise.push(`console.error: ${message.text()}`);
    }
  });
  page.on("pageerror", (error) => {
    noise.push(`pageerror: ${error.message}`);
  });
}

function expectQuiet(noise) {
  expect(noise, `expected zero page noise, got: ${noise.join(" | ")}`).toEqual(
    [],
  );
}

test.describe("calendarTwo source-filter keyboard path (issue #6992)", () => {
  test("Tab reaches both filters; Enter/Space toggle layers with aria-pressed sync", async ({
    page,
  }) => {
    test.setTimeout(90_000);
    const noise = [];
    listenForNoise(page, noise);
    const snippet = fs.readFileSync(SNIPPET_PATH, "utf8");
    const hideSource = extractHideCalendarSource(snippet);

    await page.setContent(harnessHtml());
    await page.evaluate(
      portedWiringScript(hideSource, "perc-button-container-cal1-w1"),
    );

    const teamButton = page.locator("#perc-gcal-team-button");
    const otherButton = page.locator("#perc-calendar-other-button");

    // Native-button baseline: visible, enabled, pressed (layers shown).
    await expect(teamButton).toBeVisible();
    await expect(otherButton).toBeVisible();
    await expect(teamButton).toHaveAttribute("aria-pressed", "true");
    await expect(otherButton).toHaveAttribute("aria-pressed", "true");
    await expect(page.locator("#layer-gcal-1")).toBeVisible();
    await expect(page.locator("#layer-other-1")).toBeVisible();

    // Tab order reaches the first filter; keyboard focus paints the
    // focus-visible ring (mouse clicks never do — see the mouse test).
    await page.keyboard.press("Tab");
    await expect(
      page.evaluate(() => document.activeElement.id),
    ).resolves.toBe("perc-gcal-team-button");
    await expect(
      page.evaluate(
        () =>
          getComputedStyle(
            document.getElementById("perc-gcal-team-button"),
          ).outlineWidth,
      ),
    ).resolves.toBe("2px");

    // Enter toggles the team layers off: disabled style, pressed=false,
    // layers hidden — and the control itself stays visible with focus.
    await page.keyboard.press("Enter");
    await expect(teamButton).toHaveAttribute("aria-pressed", "false");
    await expect(teamButton).toHaveClass(/perc-disabled-calendar-button/);
    await expect(page.locator("#layer-gcal-1")).toBeHidden();
    await expect(page.locator("#layer-gcal-2")).toBeHidden();
    await expect(teamButton).toBeVisible();
    await expect(
      page.evaluate(() => document.activeElement.id),
    ).resolves.toBe("perc-gcal-team-button");
    // The sibling source is untouched.
    await expect(otherButton).toHaveAttribute("aria-pressed", "true");
    await expect(page.locator("#layer-other-1")).toBeVisible();

    await page.screenshot({
      path: path.join(PROOF_DIR, "issue-6992-keyboard-filtered.png"),
    });

    // Space toggles back on (buttons fire on key-up; press covers it).
    await page.keyboard.press(" ");
    await expect(teamButton).toHaveAttribute("aria-pressed", "true");
    await expect(teamButton).not.toHaveClass(/perc-disabled-calendar-button/);
    await expect(page.locator("#layer-gcal-1")).toBeVisible();
    await expect(page.locator("#layer-gcal-2")).toBeVisible();

    // Tab advances to the second filter; Space toggles only its layer.
    await page.keyboard.press("Tab");
    await expect(
      page.evaluate(() => document.activeElement.id),
    ).resolves.toBe("perc-calendar-other-button");
    await page.keyboard.press(" ");
    await expect(otherButton).toHaveAttribute("aria-pressed", "false");
    await expect(page.locator("#layer-other-1")).toBeHidden();
    await expect(page.locator("#layer-gcal-1")).toBeVisible();
    await expect(otherButton).toBeVisible();

    await page.screenshot({
      path: path.join(PROOF_DIR, "issue-6992-keyboard-second-filter.png"),
    });

    expectQuiet(noise);
  });

  test("mouse click reaches the same state as the keyboard path", async ({
    page,
  }) => {
    test.setTimeout(90_000);
    const noise = [];
    listenForNoise(page, noise);
    const snippet = fs.readFileSync(SNIPPET_PATH, "utf8");
    const hideSource = extractHideCalendarSource(snippet);

    await page.setContent(harnessHtml());
    await page.evaluate(
      portedWiringScript(hideSource, "perc-button-container-cal1-w1"),
    );

    const teamButton = page.locator("#perc-gcal-team-button");

    // Click toggles off with the identical contract the Enter key
    // exercises above (pressed sync, disabled class, layers hidden,
    // control itself stays visible).
    await teamButton.click();
    await expect(teamButton).toHaveAttribute("aria-pressed", "false");
    await expect(teamButton).toHaveClass(/perc-disabled-calendar-button/);
    await expect(page.locator("#layer-gcal-1")).toBeHidden();
    await expect(page.locator("#layer-gcal-2")).toBeHidden();
    await expect(teamButton).toBeVisible();

    // Click toggles back on — mouse-path parity, no visual residue.
    await teamButton.click();
    await expect(teamButton).toHaveAttribute("aria-pressed", "true");
    await expect(teamButton).not.toHaveClass(/perc-disabled-calendar-button/);
    await expect(page.locator("#layer-gcal-1")).toBeVisible();
    await expect(page.locator("#layer-gcal-2")).toBeVisible();

    await page.screenshot({
      path: path.join(PROOF_DIR, "issue-6992-mouse-parity.png"),
    });

    expectQuiet(noise);
  });

  test("two widgets on one page bind once each; click syncs the clicked instance", async ({
    page,
  }) => {
    test.setTimeout(90_000);
    const noise = [];
    listenForNoise(page, noise);
    const snippet = fs.readFileSync(SNIPPET_PATH, "utf8");
    const hideSource = extractHideCalendarSource(snippet);

    // Two widget instances sharing one Google source, mirroring a real
    // page: button ids and layer classes are NOT widget-scoped (see
    // #7034), so both instances render the same button id and the same
    // layer class. Container ids ARE widget-scoped.
    await page.setContent(`<!doctype html>
<html lang="en">
<head><meta charset="utf-8"><title>two calendarTwo widgets</title>
<style>
  .perc-hide-calendar { display: none; }
  .perc-disabled-calendar-button { opacity: 0.6; }
</style>
</head>
<body>
<div id="perc-button-container-cal1-w1" class="perc-button-container" role="group" aria-label="Calendar sources">
<button type="button" id="perc-gcal-team-button" class="perc-calendar-button perc-gcal-team" data-calendar-source="perc-gcal-team" aria-pressed="true">Team Calendar</button>
</div>
<div class="fc-event perc-gcal-team" id="w1-layer-1">W1 team event</div>
<div id="perc-button-container-cal2-w2" class="perc-button-container" role="group" aria-label="Calendar sources">
<button type="button" id="perc-gcal-team-button" class="perc-calendar-button perc-gcal-team" data-calendar-source="perc-gcal-team" aria-pressed="true">Team Calendar</button>
</div>
<div class="fc-event perc-gcal-team" id="w2-layer-1">W2 team event</div>
</body>
</html>`);
    // One wiring run per rendered instance, mirroring one
    // DOMContentLoaded handler run per snippet copy on the page.
    // Instance one is wired twice on purpose: the percBound guard must
    // keep the second run from adding a duplicate listener.
    await page.evaluate(
      portedWiringScript(hideSource, "perc-button-container-cal1-w1"),
    );
    await page.evaluate(
      portedWiringScript(hideSource, "perc-button-container-cal1-w1"),
    );
    await page.evaluate(
      portedWiringScript(hideSource, "perc-button-container-cal2-w2"),
    );

    const w1Button = page.locator(
      "#perc-button-container-cal1-w1 #perc-gcal-team-button",
    );
    const w2Button = page.locator(
      "#perc-button-container-cal2-w2 #perc-gcal-team-button",
    );

    // A single click toggles exactly once. Under the old page-global
    // wiring each button carried two listeners, so one click toggled
    // twice — a net no-op with the layers left visible.
    await w2Button.click();
    await expect(page.locator("#w1-layer-1")).toBeHidden();
    await expect(page.locator("#w2-layer-1")).toBeHidden();
    // The layer sweep stays page-wide by class (known #7034
    // limitation): both instances' layers hide together.
    await expect(w2Button).toHaveAttribute("aria-pressed", "false");
    // The clicked button element carries the pressed sync — the
    // untouched instance keeps pressed=true (the old getElementById
    // lookup flipped the first match instead).
    await expect(w1Button).toHaveAttribute("aria-pressed", "true");

    // Clicking back on the same button restores the layers (single
    // toggle, not a double-bind no-op) with its own pressed sync.
    await w2Button.click();
    await expect(page.locator("#w1-layer-1")).toBeVisible();
    await expect(page.locator("#w2-layer-1")).toBeVisible();
    await expect(w2Button).toHaveAttribute("aria-pressed", "true");

    // Instance one was wired twice: one click still toggles exactly
    // once, proving the percBound guard.
    await w1Button.click();
    await expect(page.locator("#w1-layer-1")).toBeHidden();
    await expect(page.locator("#w2-layer-1")).toBeHidden();
    await expect(w1Button).toHaveAttribute("aria-pressed", "false");

    await page.screenshot({
      path: path.join(PROOF_DIR, "issue-6992-two-widgets.png"),
    });

    expectQuiet(noise);
  });
});
