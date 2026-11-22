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
 * CalendarTwo multi-widget filter scoping (issue #7034, found during
 * review of #7029; parent residual of #6992 / #6919; plan #6687).
 *
 * <p>Two calendarTwo widgets on one page used to share page-global
 * filter ids and page-wide event-layer classes: toggling a source in
 * widget A hid that source's events in widget B, and
 * {@code getElementById} only ever resolved the first button. The
 * snippet now suffixes every filter button id and layer class with the
 * widget instance's {@code calendarContentId}-{@code widgetId}, so each
 * instance's filters affect only its own buttons and layers.
 *
 * <p>Hermetic surface lock: renders two widget instances in one page
 * (same configured Google source name in both, so the pre-scoping layer
 * classes would have collided) and evaluates the <b>real
 * {@code hideCalendarSource} source extracted from the snippet at
 * runtime</b> (brace matching from the declaration, the same technique
 * the sibling specs use), so the behavior under test is the snippet's
 * own — not a copy. The delegated wiring loop is ported per container
 * (the snippet registers it on {@code DOMContentLoaded}, which a
 * {@code setContent} harness cannot re-fire — the test evaluates the
 * port once per rendered widget instance, mirroring one handler run per
 * instance); its strings are pinned against the snippet by
 * {@code calendarTwoMultiWidgetScoping.test.js}, which fails Vitest
 * loudly on drift.
 *
 * <p>Screenshots land in the gitignored {@code test-results/} tree
 * (repo convention); the curated {@code docs/ai-generated/proofs/}
 * PNG referenced from the PR body is refreshed by intentional copy
 * only, so routine runs never dirty the worktree.
 *
 * <p>Run:
 * {@code npm run test:surface -- --path tests/calendar-two-multi-widget-scoping.spec.js}</p>
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
// paren only so snippet signature widenings do not break the
// extractor.
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

// One widget instance's filter bar plus event layers, mirroring the
// snippet with concrete ids (calendarContentId=cid, widgetId=wid).
// Both instances below configure the SAME Google source name
// ("Team Calendar" / gcal-team): pre-scoping, their layer classes
// collided page-wide; post-scoping the instance suffix keeps them
// apart. Layer divs are not buttons, mirroring FullCalendar event
// nodes.
function widgetMarkup(cid, wid, tag) {
  const scope = `perc-gcal-team-${cid}-${wid}`;
  const other = `perc-calendar-other-${cid}-${wid}`;
  return `
  <div id="perc-button-container-${cid}-${wid}" class="perc-button-container" role="group" aria-label="Calendar sources">
  <button type="button" id="perc-gcal-team-${cid}-${wid}-button" class="perc-calendar-button ${scope}" data-calendar-source="${scope}" aria-pressed="true">Team Calendar</button>
  <button type="button" id="perc-calendar-other-${cid}-${wid}-button" class="perc-calendar-button perc-calendar-other ${other}" data-calendar-source="${other}" aria-pressed="true">Percussion Events</button>
  </div>
  <div class="fc-event ${scope}" id="layer-${tag}-team-1">Team event one (${tag})</div>
  <div class="fc-event ${scope}" id="layer-${tag}-team-2">Team event two (${tag})</div>
  <div class="fc-event ${other}" id="layer-${tag}-other-1">Percussion event (${tag})</div>`;
}

function harnessHtml() {
  return `<!doctype html>
<html lang="en">
<head><meta charset="utf-8"><title>calendarTwo two-instance scoping harness</title></head>
<body>
  <style>
    .perc-hide-calendar { display: none; }
    .perc-disabled-calendar-button { opacity: 0.6; }
    .perc-calendar-button:focus-visible {
      outline: 2px solid #005fcc;
      outline-offset: 2px;
    }
  </style>
${widgetMarkup("calA", "wA", "a")}
${widgetMarkup("calB", "wB", "b")}
</body>
</html>`;
}

// Ports the snippet DOMContentLoaded wiring loop for one widget
// instance (pinned by the calendarTwoMultiWidgetScoping Vitest):
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

test.describe("calendarTwo multi-widget filter scoping (issue #7034)", () => {
  test("two instances share no ids and toggle independently", async ({
    page,
  }) => {
    test.setTimeout(90_000);
    const noise = [];
    listenForNoise(page, noise);
    const snippet = fs.readFileSync(SNIPPET_PATH, "utf8");
    const hideSource = extractHideCalendarSource(snippet);

    await page.setContent(harnessHtml());
    // One wiring run per widget instance, mirroring the per-render
    // DOMContentLoaded handler the snippet emits.
    await page.evaluate(
      portedWiringScript(hideSource, "perc-button-container-calA-wA"),
    );
    await page.evaluate(
      portedWiringScript(hideSource, "perc-button-container-calB-wB"),
    );

    // Acceptance: no duplicate DOM ids across instances.
    const duplicateIds = await page.evaluate(() => {
      const seen = new Set();
      const dupes = new Set();
      document.querySelectorAll("[id]").forEach((el) => {
        if (seen.has(el.id)) {
          dupes.add(el.id);
        }
        seen.add(el.id);
      });
      return [...dupes];
    });
    expect(duplicateIds).toEqual([]);

    const aTeam = page.locator("#perc-gcal-team-calA-wA-button");
    const aOther = page.locator("#perc-calendar-other-calA-wA-button");
    const bTeam = page.locator("#perc-gcal-team-calB-wB-button");
    const bOther = page.locator("#perc-calendar-other-calB-wB-button");

    // Baseline: everything pressed (layers shown) in both widgets.
    for (const button of [aTeam, aOther, bTeam, bOther]) {
      await expect(button).toBeVisible();
      await expect(button).toHaveAttribute("aria-pressed", "true");
    }
    await expect(page.locator("#layer-a-team-1")).toBeVisible();
    await expect(page.locator("#layer-b-team-1")).toBeVisible();

    // Toggle widget A's team source off: only A's team layers hide and
    // only A's button flips — B is untouched even though both widgets
    // configure the same source name.
    await aTeam.click();
    await expect(aTeam).toHaveAttribute("aria-pressed", "false");
    await expect(aTeam).toHaveClass(/perc-disabled-calendar-button/);
    await expect(page.locator("#layer-a-team-1")).toBeHidden();
    await expect(page.locator("#layer-a-team-2")).toBeHidden();
    await expect(aTeam).toBeVisible();
    await expect(bTeam).toHaveAttribute("aria-pressed", "true");
    await expect(page.locator("#layer-b-team-1")).toBeVisible();
    await expect(page.locator("#layer-b-team-2")).toBeVisible();
    // Sibling sources inside A stay up.
    await expect(page.locator("#layer-a-other-1")).toBeVisible();
    await expect(aOther).toHaveAttribute("aria-pressed", "true");

    // Toggle widget B's Percussion-events source off: only B's other
    // layer hides — A's other layer stays up.
    await bOther.click();
    await expect(bOther).toHaveAttribute("aria-pressed", "false");
    await expect(page.locator("#layer-b-other-1")).toBeHidden();
    await expect(page.locator("#layer-a-other-1")).toBeVisible();
    await expect(aOther).toHaveAttribute("aria-pressed", "true");

    await page.screenshot({
      path: path.join(PROOF_DIR, "issue-7034-two-instance.png"),
    });

    // Toggle back on: layers return, pressed syncs, no visual residue.
    await aTeam.click();
    await expect(aTeam).toHaveAttribute("aria-pressed", "true");
    await expect(aTeam).not.toHaveClass(/perc-disabled-calendar-button/);
    await expect(page.locator("#layer-a-team-1")).toBeVisible();
    await bOther.click();
    await expect(page.locator("#layer-b-other-1")).toBeVisible();

    expectQuiet(noise);
  });

  test("keyboard path toggles per instance without disturbing the other", async ({
    page,
  }) => {
    test.setTimeout(90_000);
    const noise = [];
    listenForNoise(page, noise);
    const snippet = fs.readFileSync(SNIPPET_PATH, "utf8");
    const hideSource = extractHideCalendarSource(snippet);

    await page.setContent(harnessHtml());
    await page.evaluate(
      portedWiringScript(hideSource, "perc-button-container-calA-wA"),
    );
    await page.evaluate(
      portedWiringScript(hideSource, "perc-button-container-calB-wB"),
    );

    const aTeam = page.locator("#perc-gcal-team-calA-wA-button");
    const bTeam = page.locator("#perc-gcal-team-calB-wB-button");

    // Tab reaches the first filter; keyboard focus paints the
    // focus-visible ring.
    await page.keyboard.press("Tab");
    await expect(
      page.evaluate(() => document.activeElement.id),
    ).resolves.toBe("perc-gcal-team-calA-wA-button");
    await expect(
      page.evaluate(
        () =>
          getComputedStyle(
            document.getElementById("perc-gcal-team-calA-wA-button"),
          ).outlineWidth,
      ),
    ).resolves.toBe("2px");

    // Tab twice more lands on widget B's team filter (order: A-team,
    // A-other, B-team, B-other).
    await page.keyboard.press("Tab");
    await expect(
      page.evaluate(() => document.activeElement.id),
    ).resolves.toBe("perc-calendar-other-calA-wA-button");
    await page.keyboard.press("Tab");
    await expect(
      page.evaluate(() => document.activeElement.id),
    ).resolves.toBe("perc-gcal-team-calB-wB-button");

    // Space toggles only B's team layers; A is untouched and the
    // control keeps focus.
    await page.keyboard.press(" ");
    await expect(bTeam).toHaveAttribute("aria-pressed", "false");
    await expect(page.locator("#layer-b-team-1")).toBeHidden();
    await expect(page.locator("#layer-a-team-1")).toBeVisible();
    await expect(aTeam).toHaveAttribute("aria-pressed", "true");
    await expect(
      page.evaluate(() => document.activeElement.id),
    ).resolves.toBe("perc-gcal-team-calB-wB-button");

    // Space again toggles B back on — screen-reader and mouse parity.
    await page.keyboard.press(" ");
    await expect(bTeam).toHaveAttribute("aria-pressed", "true");
    await expect(page.locator("#layer-b-team-1")).toBeVisible();

    await page.screenshot({
      path: path.join(PROOF_DIR, "issue-7034-keyboard-second-instance.png"),
    });

    expectQuiet(noise);
  });
});
