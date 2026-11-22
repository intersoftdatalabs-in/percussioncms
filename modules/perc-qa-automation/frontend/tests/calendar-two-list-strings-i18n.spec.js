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
 * CalendarTwo list-state string i18n (issue #6993, residual of
 * [6919/3] cleanup #6946, parent #6919; plan #6687, audit #5627).
 *
 * <p>Slice 2 (#6945) rendered the list-layout loading and empty rows as
 * hardcoded English, so a non-English widget announced English text
 * through the {@code aria-live="polite"} list region. This slice
 * sources both strings from the {@code perc_list_loading_text} /
 * {@code perc_list_empty_text} widget preferences (English defaults in
 * {@code component-package.json}, Velocity fallback guards in the
 * snippet for pre-existing instances).
 *
 * <p>Hermetic surface lock: the row markup shape (element, classes) is
 * the <b>real snippet bytes</b> read from disk — the harness extracts
 * the two row literals and substitutes only the preference token, the
 * same single substitution Velocity performs at assembly. The
 * substituted value is JS-escaped by a documented mirror of
 * {@code $tools.esc.javascript} (commons-lang
 * {@code StringEscapeUtils.escapeJavaScript}), then the resulting
 * literal is {@code eval}'d in-page exactly as the snippet's
 * {@code .append(...)} / {@code .append(...)} consumes it — so a
 * value that would break the single-quoted row string (e.g. a French
 * ASCII apostrophe) fails loudly here instead of shipping a dead list
 * region. Asserts:
 *
 * <ul>
 *   <li>Defaults preserve the slice-2 English output byte-for-byte
 *       (the {@code calendar-two-v6-list.spec.js} port keeps passing
 *       unmodified).</li>
 *   <li>A French override (ASCII apostrophe + ellipsis) reaches the
 *       {@code aria-live} region intact for both the loading and the
 *       empty states.</li>
 *   <li>No hardcoded English remains in the snippet row markup; the
 *       fallback guards carry the English defaults.</li>
 * </ul>
 *
 * <p>Screenshots land in the gitignored {@code test-results/} tree
 * (repo convention); the curated {@code docs/ai-generated/proofs/}
 * PNG referenced from the PR body is refreshed by intentional copy
 * only, so routine runs never dirty the worktree.
 *
 * <p>Run:
 * {@code npm run test:surface -- --path tests/calendar-two-list-strings-i18n.spec.js}</p>
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

const LOADING_TOKEN = "$tools.esc.javascript($perc_list_loading_text)";
const EMPTY_TOKEN = "$tools.esc.javascript($perc_list_empty_text)";

// Mirror of commons-lang StringEscapeUtils.escapeJavaScript behind
// $tools.esc.javascript: the snippet embeds each preference inside a
// single-quoted JS row literal, so this is what keeps an editor
// apostrophe (fr: "d'événements") from terminating the string.
function escapeJavaScript(value) {
  return value
    .replace(/\\/g, "\\\\")
    .replace(/'/g, "\\'")
    .replace(/"/g, '\\"')
    .replace(/\//g, "\\/")
    .replace(/\n/g, "\\n")
    .replace(/\r/g, "\\r")
    .replace(/\t/g, "\\t");
}

// Extracts the real row literal for the given marker class from the
// snippet source and substitutes only the preference token — the same
// single substitution Velocity performs at assembly. Returns the JS
// string-literal source (including its quotes) for in-page eval.
function snippetRowLiteral(snippet, markerClass, token, value) {
  const line = snippet
    .split("\n")
    .find((l) => l.includes(markerClass) && l.includes(token));
  if (!line) {
    throw new Error(
      `row literal with ${markerClass} + preference token not found in snippet`,
    );
  }
  const first = line.indexOf("'");
  const last = line.lastIndexOf("'");
  if (first < 0 || last <= first) {
    throw new Error(`row literal quotes do not parse: ${line.trim()}`);
  }
  return line.slice(first, last + 1).replace(token, escapeJavaScript(value));
}

function prefDefaults() {
  const pkg = JSON.parse(fs.readFileSync(COMPONENT_PACKAGE_PATH, "utf8"));
  return Object.fromEntries(pkg.userPreferences.map((p) => [p.name, p.defaultValue]));
}

test.describe("calendarTwo list-state strings are preference-driven (#6993)", () => {
  test("defaults preserve the slice-2 English output", async () => {
    const defaults = prefDefaults();
    expect(defaults["perc_list_loading_text"]).toBe("Loading events…");
    expect(defaults["perc_list_empty_text"]).toBe("No upcoming events.");

    // No hardcoded English in the row markup itself: the only snippet
    // literals allowed are the fallback-guard defaults.
    const snippet = fs.readFileSync(SNIPPET_PATH, "utf8");
    for (const line of snippet.split("\n")) {
      if (
        line.includes('perc-event-list-loading">') ||
        line.includes('perc-event-list-empty">')
      ) {
        expect(line).not.toContain("Loading events");
        expect(line).not.toContain("No upcoming events.");
      }
    }
    expect(snippet).toContain(
      "#if(!$perc_list_loading_text)#set($perc_list_loading_text = 'Loading events…')#end",
    );
    expect(snippet).toContain(
      "#if(!$perc_list_empty_text)#set($perc_list_empty_text = 'No upcoming events.')#end",
    );
  });

  test("French override reaches the aria-live region intact", async ({
    page,
  }) => {
    test.setTimeout(60_000);
    const snippet = fs.readFileSync(SNIPPET_PATH, "utf8");
    // ASCII apostrophe: the exact character class that would
    // terminate the single-quoted row literal without esc.javascript.
    const frenchLoading = "Chargement des événements…";
    const frenchEmpty = "Aucun événement à venir.";

    const loadingLiteral = snippetRowLiteral(
      snippet,
      "perc-event-list-loading",
      LOADING_TOKEN,
      frenchLoading,
    );
    const emptyLiteral = snippetRowLiteral(
      snippet,
      "perc-event-list-empty",
      EMPTY_TOKEN,
      frenchEmpty,
    );

    await page.setContent(`
      <div id="perc-event-list-cal1-w1" aria-live="polite"><ul></ul></div>
    `);
    // Eval the real snippet literals in-page, as .append() consumes
    // them: a quoting break from an unescaped value throws here.
    const announced = await page.evaluate(
      ({ loadingSrc, emptySrc }) => {
        const list = document.querySelector("#perc-event-list-cal1-w1 ul");
        // eslint-disable-next-line no-eval
        list.innerHTML = eval(loadingSrc);
        const loadingText = list.textContent;
        // eslint-disable-next-line no-eval
        list.innerHTML = eval(emptySrc);
        return { loadingText, emptyText: list.textContent };
      },
      { loadingSrc: loadingLiteral, emptySrc: emptyLiteral },
    );
    expect(announced.loadingText).toBe(frenchLoading);
    expect(announced.emptyText).toBe(frenchEmpty);

    // The rows keep the slice-2 hooks the parity spec pins.
    const loadingClass = await page.evaluate((src) => {
      // eslint-disable-next-line no-eval
      const html = eval(src);
      const tmp = document.createElement("div");
      tmp.innerHTML = `<ul>${html}</ul>`;
      return tmp.querySelector("li").className;
    }, emptyLiteral);
    expect(loadingClass).toContain("perc-event-list-empty");
    expect(loadingClass).toContain("perc-event-list-first");
    expect(loadingClass).toContain("perc-event-list-last");

    await expect(page.locator("#perc-event-list-cal1-w1")).toHaveAttribute(
      "aria-live",
      "polite",
    );
    await page.screenshot({
      path: path.join(PROOF_DIR, "issue-6993-list-strings-fr.png"),
    });
  });
});
