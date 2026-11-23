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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * CalendarTwo view-toggle button text i18n (issue #7118, residual of
 * #7030; parent #7030, plan #6687, audit #5627).
 *
 * <p>Slice #7030 keyed the two {@code role="group"} accessible names
 * through widget preferences, but the view-toggle buttons themselves
 * still rendered hardcoded English text content — a non-English
 * widget with localized group labels still announced English button
 * names. This slice sources both labels from the
 * {@code perc_calendar_view_button_label} /
 * {@code perc_list_view_button_label} widget preferences (English
 * defaults in {@code component-package.json}, Velocity fallback
 * guards in the snippet for pre-existing instances).
 *
 * <p>Hermetic surface lock: the button markup shape (element, ids,
 * classes, pressed state) is the <b>real snippet bytes</b> read from
 * disk — the harness extracts the two button lines and substitutes
 * only the preference token plus the instance ids, the same
 * substitutions Velocity performs at assembly. The substituted value
 * is HTML-escaped by a documented mirror of {@code $tools.esc.html}
 * (commons-lang {@code StringEscapeUtils.escapeHtml}, ASCII-subset
 * behavior shared with commons-lang3 {@code escapeHtml4}), then the
 * resulting markup is set in-page exactly as the assembled page
 * carries it — so a value that would break the button markup (e.g.
 * an editor {@code <} or {@code &}) fails loudly here instead of
 * shipping truncated button names. Asserts:
 *
 * <ul>
 *   <li>Defaults preserve the pre-existing English output
 *       byte-for-byte.</li>
 *   <li>A French override reaches both buttons' accessible names
 *       intact, and editor markup characters survive as text via
 *       HTML escaping.</li>
 *   <li>No hardcoded English remains in the snippet button markup;
 *       the fallback guards carry the English defaults.</li>
 * </ul>
 *
 * <p>Screenshots land in the gitignored {@code test-results/} tree
 * (repo convention); no curated proof PNG is needed for this text
 * swap — the assertions above are the lock.
 *
 * <p>Run:
 * {@code npm run test:surface -- --path tests/calendar-two-toggle-buttons-i18n.spec.js}</p>
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

const CALENDAR_TOKEN = "$tools.esc.html($perc_calendar_view_button_label)";
const LIST_TOKEN = "$tools.esc.html($perc_list_view_button_label)";

// Mirror of commons-lang StringEscapeUtils.escapeHtml behind
// $tools.esc.html (ASCII-subset behavior shared with
// commons-lang3 escapeHtml4): the snippet embeds each preference
// as button text content, so this is what keeps an editor <, >, or
// & from breaking the button markup.
function escapeHtml(value) {
  return value
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

// Extracts the real button line for the given button id prefix from
// the snippet source and performs the same substitutions Velocity
// performs at assembly: concrete instance ids for the
// ${calendarContentId}/${widgetId} references and the escaped
// preference value for the token. Returns page-ready markup.
function snippetButtonMarkup(snippet, idPrefix, token, value) {
  const line = snippet
    .split("\n")
    .find((l) => l.includes(`id="${idPrefix}-`) && l.includes(token));
  if (!line) {
    throw new Error(
      `button line with ${idPrefix} + preference token not found in snippet`,
    );
  }
  return line
    .replace("${calendarContentId}", "cal1")
    .replace("${widgetId}", "w1")
    .replace(token, escapeHtml(value));
}

function prefDefaults() {
  const pkg = JSON.parse(fs.readFileSync(COMPONENT_PACKAGE_PATH, "utf8"));
  return Object.fromEntries(
    pkg.userPreferences.map((p) => [p.name, p.defaultValue]),
  );
}

test.describe("calendarTwo view-toggle button text is preference-driven (#7118)", () => {
  test("defaults preserve the pre-existing English output", async () => {
    const defaults = prefDefaults();
    expect(defaults["perc_calendar_view_button_label"]).toBe("Calendar View");
    expect(defaults["perc_list_view_button_label"]).toBe("List View");

    // No hardcoded English in the button markup itself: the only
    // snippet literals allowed are the fallback-guard defaults.
    const snippet = fs.readFileSync(SNIPPET_PATH, "utf8");
    for (const line of snippet.split("\n")) {
      if (
        line.includes("showCalendarButton-") ||
        line.includes("showListButton-")
      ) {
        expect(line).not.toContain(">Calendar View<");
        expect(line).not.toContain(">List View<");
      }
    }
    expect(snippet).toContain(
      "#if(!$perc_calendar_view_button_label)#set($perc_calendar_view_button_label = 'Calendar View')#end",
    );
    expect(snippet).toContain(
      "#if(!$perc_list_view_button_label)#set($perc_list_view_button_label = 'List View')#end",
    );
  });

  test("French override reaches both buttons intact, markup survives", async ({
    page,
  }) => {
    test.setTimeout(60_000);
    const snippet = fs.readFileSync(SNIPPET_PATH, "utf8");
    const frenchCalendar = "Vue du calendrier";
    const frenchList = "Vue de la liste";
    // Editor markup characters: the exact character classes that
    // would break the button markup without esc.html.
    const trickyList = "Liste <à venir> & suite";

    const calendarMarkup = snippetButtonMarkup(
      snippet,
      "showCalendarButton",
      CALENDAR_TOKEN,
      frenchCalendar,
    );
    const listMarkup = snippetButtonMarkup(
      snippet,
      "showListButton",
      LIST_TOKEN,
      frenchList,
    );
    const trickyMarkup = snippetButtonMarkup(
      snippet,
      "showListButton",
      LIST_TOKEN,
      trickyList,
    );

    // Defaults render byte-identical to the pre-slice output.
    expect(
      snippetButtonMarkup(
        snippet,
        "showCalendarButton",
        CALENDAR_TOKEN,
        "Calendar View",
      ),
    ).toContain(">Calendar View</button>");
    expect(
      snippetButtonMarkup(snippet, "showListButton", LIST_TOKEN, "List View"),
    ).toContain(">List View</button>");

    await page.setContent(`
      <div id="perc-toggle-button-container" class="perc-toggle-button-container" role="group" aria-label="Affichage du calendrier">
      ${calendarMarkup}
      ${listMarkup}
      </div>
    `);
    await expect(page.locator("#showCalendarButton-cal1-w1")).toHaveText(
      frenchCalendar,
    );
    await expect(page.locator("#showListButton-cal1-w1")).toHaveText(
      frenchList,
    );

    // The escaped markup characters round-trip as button text: the
    // accessible name carries the literal characters.
    await page.setContent(trickyMarkup);
    await expect(page.locator("#showListButton-cal1-w1")).toHaveText(
      trickyList,
    );

    // Proof capture: French-labelled toggle buttons as AT consumes them.
    await page.setContent(`
      <div id="perc-toggle-button-container" class="perc-toggle-button-container" role="group" aria-label="Affichage du calendrier">
      ${calendarMarkup}
      ${listMarkup}
      </div>
    `);
    await page.screenshot({
      path: path.join(PROOF_DIR, "issue-7118-toggle-buttons-fr.png"),
    });
  });
});
