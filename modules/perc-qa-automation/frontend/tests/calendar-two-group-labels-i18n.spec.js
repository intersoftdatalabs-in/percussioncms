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
 * CalendarTwo group-label string i18n (issue #7030, residual of
 * #6992; parent #6992, plan #6687, audit #5627).
 *
 * <p>Slice #6992 put the source-filter buttons in a
 * {@code role="group"} labelled "Calendar sources" next to the
 * pre-existing "Calendar layout" view-toggle group, so a non-English
 * widget exposed English accessible names for both control groups.
 * This slice sources both labels from the
 * {@code perc_source_filter_group_label} /
 * {@code perc_layout_toggle_group_label} widget preferences (English
 * defaults in {@code component-package.json}, Velocity fallback
 * guards in the snippet for pre-existing instances).
 *
 * <p>Hermetic surface lock: the group markup shape (element, classes,
 * group role) is the <b>real snippet bytes</b> read from disk — the
 * harness extracts the two group lines and substitutes only the
 * preference token plus the instance ids, the same substitutions
 * Velocity performs at assembly. The substituted value is
 * HTML-escaped by a documented mirror of {@code $tools.esc.html}
 * (commons-lang {@code StringEscapeUtils.escapeHtml}, ASCII-subset
 * behavior shared with commons-lang3 {@code escapeHtml4}), then the
 * resulting markup is set in-page exactly as the assembled page
 * carries it — so a value that would break the
 * {@code aria-label="..."} attribute (e.g. an editor double-quote)
 * fails loudly here instead of shipping a truncated accessible
 * name. Asserts:
 *
 * <ul>
 *   <li>Defaults preserve the pre-existing English output
 *       byte-for-byte (the keyboard/toggle/multi-widget pins keep
 *       passing unmodified apart from their token-form updates).</li>
 *   <li>A French override reaches both groups' accessible names
 *       intact, and an editor double-quote survives the attribute
 *       via {@code &quot;} escaping.</li>
 *   <li>No hardcoded English remains in the snippet group markup;
 *       the fallback guards carry the English defaults.</li>
 * </ul>
 *
 * <p>Screenshots land in the gitignored {@code test-results/} tree
 * (repo convention); the curated {@code docs/ai-generated/proofs/}
 * PNG referenced from the PR body is refreshed by intentional copy
 * only, so routine runs never dirty the worktree.
 *
 * <p>Run:
 * {@code npm run test:surface -- --path tests/calendar-two-group-labels-i18n.spec.js}</p>
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

const SOURCE_TOKEN = "$tools.esc.html($perc_source_filter_group_label)";
const LAYOUT_TOKEN = "$tools.esc.html($perc_layout_toggle_group_label)";

// Mirror of commons-lang StringEscapeUtils.escapeHtml behind
// $tools.esc.html (ASCII-subset behavior shared with
// commons-lang3 escapeHtml4): the snippet embeds each preference
// inside a double-quoted aria-label attribute, so this is what
// keeps an editor double-quote from terminating the attribute.
function escapeHtml(value) {
  return value
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

// Extracts the real group line for the given container class from
// the snippet source and performs the same substitutions Velocity
// performs at assembly: concrete instance ids for the
// ${calendarContentId}/${widgetId} references and the escaped
// preference value for the token. Returns page-ready markup.
function snippetGroupMarkup(snippet, containerClass, token, value) {
  const line = snippet
    .split("\n")
    .find(
      (l) =>
        l.includes(containerClass) &&
        l.includes('role="group"') &&
        l.includes(token),
    );
  if (!line) {
    throw new Error(
      `group line with ${containerClass} + preference token not found in snippet`,
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

test.describe("calendarTwo group labels are preference-driven (#7030)", () => {
  test("defaults preserve the pre-existing English output", async () => {
    const defaults = prefDefaults();
    expect(defaults["perc_source_filter_group_label"]).toBe(
      "Calendar sources",
    );
    expect(defaults["perc_layout_toggle_group_label"]).toBe(
      "Calendar layout",
    );

    // No hardcoded English in the group markup itself: the only
    // snippet literals allowed are the fallback-guard defaults.
    const snippet = fs.readFileSync(SNIPPET_PATH, "utf8");
    for (const line of snippet.split("\n")) {
      if (line.includes('role="group"') && line.includes("aria-label=")) {
        expect(line).not.toContain("Calendar layout");
        expect(line).not.toContain("Calendar sources");
      }
    }
    expect(snippet).toContain(
      "#if(!$perc_source_filter_group_label)#set($perc_source_filter_group_label = 'Calendar sources')#end",
    );
    expect(snippet).toContain(
      "#if(!$perc_layout_toggle_group_label)#set($perc_layout_toggle_group_label = 'Calendar layout')#end",
    );
  });

  test("French override reaches both groups intact, quotes survive", async ({
    page,
  }) => {
    test.setTimeout(60_000);
    const snippet = fs.readFileSync(SNIPPET_PATH, "utf8");
    const frenchSource = "Sources du calendrier";
    const frenchLayout = "Affichage du calendrier";
    // Editor double-quote: the exact character class that would
    // terminate the aria-label attribute without esc.html.
    const quotedSource = 'Sources "Google" du calendrier';

    const sourceMarkup = snippetGroupMarkup(
      snippet,
      "perc-button-container",
      SOURCE_TOKEN,
      frenchSource,
    );
    const layoutMarkup = snippetGroupMarkup(
      snippet,
      "perc-toggle-button-container",
      LAYOUT_TOKEN,
      frenchLayout,
    );
    const quotedMarkup = snippetGroupMarkup(
      snippet,
      "perc-button-container",
      SOURCE_TOKEN,
      quotedSource,
    );

    // Defaults render byte-identical to the pre-slice output.
    expect(
      snippetGroupMarkup(
        snippet,
        "perc-button-container",
        SOURCE_TOKEN,
        "Calendar sources",
      ),
    ).toContain('aria-label="Calendar sources"');
    expect(
      snippetGroupMarkup(
        snippet,
        "perc-toggle-button-container",
        LAYOUT_TOKEN,
        "Calendar layout",
      ),
    ).toContain('aria-label="Calendar layout"');

    await page.setContent(`
      ${layoutMarkup}
      <button type="button">Calendar View</button>
      ${sourceMarkup}
      <button type="button">Team Calendar</button>
    `);
    await expect(page.locator("#perc-toggle-button-container")).toHaveAttribute(
      "aria-label",
      frenchLayout,
    );
    await expect(
      page.locator("#perc-button-container-cal1-w1"),
    ).toHaveAttribute("aria-label", frenchSource);

    // The escaped double-quote round-trips through the attribute:
    // the accessible name carries the literal quote characters.
    await page.setContent(quotedMarkup);
    await expect(page.locator("#perc-button-container-cal1-w1")).toHaveAttribute(
      "aria-label",
      quotedSource,
    );

    // Proof capture: French-labelled groups as AT consumes them.
    await page.setContent(`
      ${layoutMarkup}
      ${sourceMarkup}
    `);
    await page.screenshot({
      path: path.join(PROOF_DIR, "issue-7030-group-labels-fr.png"),
    });
  });
});
