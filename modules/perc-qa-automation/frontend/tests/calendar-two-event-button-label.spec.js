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
 * so the button rendered with empty text content — an unnamed,
 * focusable button inside the {@code Calendar sources} group.
 *
 * <p>Hermetic surface lock: the button markup shape (element, ids,
 * classes, pressed state) is the <b>real snippet bytes</b> read from
 * disk — the harness extracts the Percussion-events button line and
 * substitutes only the preference token plus the instance ids, the
 * same substitutions Velocity performs at assembly. The substituted
 * value is HTML-escaped by a documented mirror of
 * {@code $tools.esc.html} (commons-lang
 * {@code StringEscapeUtils.escapeHtml}, ASCII-subset behavior shared
 * with commons-lang3 {@code escapeHtml4}), then the resulting markup
 * is set in-page exactly as the assembled page carries it — so a
 * value that would break the button markup (e.g. an editor {@code <}
 * or {@code &}) fails loudly here instead of shipping truncated
 * button names. Asserts:
 *
 * <ul>
 *   <li>Defaults preserve the pre-existing English output
 *       byte-for-byte.</li>
 *   <li>A missing pref (pre-existing instance) falls back to the
 *       "Other" default, so the button always carries an accessible
 *       name — never empty text content.</li>
 *   <li>A French override reaches the button's accessible name
 *       intact, and editor markup characters survive as text via
 *       HTML escaping.</li>
 *   <li>No unescaped interpolation remains on the button line; the
 *       fallback guard carries the English default.</li>
 * </ul>
 *
 * <p>Screenshots land in the gitignored {@code test-results/} tree
 * (repo convention); no curated proof PNG is needed for this text
 * swap — the assertions above are the lock.
 *
 * <p>Run:
 * {@code npm run test:surface -- --path tests/calendar-two-event-button-label.spec.js}</p>
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
const COMPONENT_PACKAGE_PATH = path.join(
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
const PROOF_DIR = path.join(__dirname, "..", "test-results");

const LABEL_TOKEN = "$tools.esc.html($perc_event_button_label)";

// Mirror of commons-lang StringEscapeUtils.escapeHtml behind
// $tools.esc.html (ASCII-subset behavior shared with
// commons-lang3 escapeHtml4): the snippet embeds the preference as
// button text content, so this is what keeps an editor <, >, or &
// from breaking the button markup.
function escapeHtml(value) {
  return value
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

// Extracts the real Percussion-events button line from the snippet
// source and performs the same substitutions Velocity performs at
// assembly: concrete instance ids for the
// ${calendarContentId}/${widgetId} references and the escaped
// preference value for the token. Returns page-ready markup.
function snippetButtonMarkup(snippet, value) {
  const line = snippet
    .split("\n")
    .find((l) => l.includes('id="perc-calendar-other-') && l.includes(LABEL_TOKEN));
  if (!line) {
    throw new Error(
      "Percussion-events button line with preference token not found in snippet",
    );
  }
  return line
    .replace("${calendarContentId}", "cal1")
    .replace("${widgetId}", "w1")
    .replace(LABEL_TOKEN, escapeHtml(value));
}

function prefDefault() {
  const pkg = JSON.parse(fs.readFileSync(COMPONENT_PACKAGE_PATH, "utf8"));
  return Object.fromEntries(
    pkg.userPreferences.map((p) => [p.name, p.defaultValue]),
  )["perc_event_button_label"];
}

test.describe("calendarTwo Percussion-events button label fallback (#7142)", () => {
  test("defaults preserve the pre-existing English output", async () => {
    expect(prefDefault()).toBe("Other");

    // No unescaped interpolation on the button line: the only
    // snippet literal allowed is the fallback-guard default.
    const snippet = fs.readFileSync(SNIPPET_PATH, "utf8");
    for (const line of snippet.split("\n")) {
      if (line.includes("perc-calendar-other-")) {
        expect(line).not.toContain(">$perc_event_button_label<");
      }
    }
    expect(snippet).toContain(
      "#if(!$perc_event_button_label)#set($perc_event_button_label = 'Other')#end",
    );

    // The guarded default renders byte-identical to the pre-slice
    // output for pre-existing instances (no stored pref).
    expect(snippetButtonMarkup(snippet, "Other")).toContain(
      ">Other</button>",
    );
  });

  test("missing pref falls back to a named button, overrides stay intact", async ({
    page,
  }) => {
    test.setTimeout(60_000);
    const snippet = fs.readFileSync(SNIPPET_PATH, "utf8");
    const frenchLabel = "Autres";
    // Editor markup characters: the exact character classes that
    // would break the button markup without esc.html.
    const trickyLabel = "Autres <à venir> & suite";

    // Pre-existing instance: no stored pref, so the guard supplies
    // "Other" — the button carries an accessible name, never empty
    // text content (the #7142 defect).
    await page.setContent(`
      <div id="perc-button-container-cal1-w1" class="perc-button-container" role="group" aria-label="Calendar sources">
      ${snippetButtonMarkup(snippet, prefDefault())}
      </div>
    `);
    await expect(
      page.locator("#perc-calendar-other-cal1-w1-button"),
    ).toHaveText("Other");

    await page.setContent(`
      <div id="perc-button-container-cal1-w1" class="perc-button-container" role="group" aria-label="Sources du calendrier">
      ${snippetButtonMarkup(snippet, frenchLabel)}
      </div>
    `);
    await expect(
      page.locator("#perc-calendar-other-cal1-w1-button"),
    ).toHaveText(frenchLabel);

    // The escaped markup characters round-trip as button text: the
    // accessible name carries the literal characters.
    await page.setContent(snippetButtonMarkup(snippet, trickyLabel));
    await expect(
      page.locator("#perc-calendar-other-cal1-w1-button"),
    ).toHaveText(trickyLabel);

    // Proof capture: French-labelled filter button as AT consumes it.
    await page.setContent(`
      <div id="perc-button-container-cal1-w1" class="perc-button-container" role="group" aria-label="Sources du calendrier">
      ${snippetButtonMarkup(snippet, frenchLabel)}
      </div>
    `);
    await page.screenshot({
      path: path.join(PROOF_DIR, "issue-7142-event-button-fr.png"),
    });
  });
});
