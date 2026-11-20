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
 * percCalendarTwo editor added-row color pickers (issue #6723).
 *
 * The Google-Calendar editor control (`percGoogleCalendarControl.xsl` +
 * shipped `percCalendarTwo.js`) broke pickers on every calendar row added
 * with the + button: the template row carried duplicate `id`s, rows were
 * added with `.clone(true)` (copying initialized picker state), and the
 * add-row re-init targeted those ids (first row only) as a no-op. The
 * #6723 fix clones without data/events, strips ids, and initializes each
 * row's color inputs independently by class.
 *
 * This spec drives the FIXED shipped bytes (SupportFile preview twin +
 * its MiniColors 2.3.6 twin, read from the repo checkout under test) on a
 * live CMS classic page that already loads jQuery: toggle the setup
 * section open, add rows, open a picker panel, pick a swatch, and save
 * through the captured pre-submit handler — asserting independent working
 * pickers, no duplicate ids, and JSON persistence.
 *
 * Run (QA mode after `perc-devctl qa-up`):
 * `npm run test:surface -- --path tests/bugs/bug-6723-calendar-minicolors.spec.js`
 * from `modules/perc-qa-automation/frontend`.
 *
 * Surface tag: `@calendar-editor-minicolors`.
 */

const path = require("node:path");
const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("../helpers/auth");

const REPO_ROOT = path.join(
  __dirname,
  "..",
  "..",
  "..",
  "..",
  "..",
);
const CALENDAR_BASE = path.join(
  "modules",
  "perc-packages",
  "src",
  "main",
  "resources",
  "Packages",
  "perc.widget.calendar",
  "SupportFile-rx_resources",
  "widgets",
  "percCalendarTwo",
);
const MINICOLORS_JS = path.join(REPO_ROOT, CALENDAR_BASE, "js", "jquery.minicolors.js");
const MINICOLORS_CSS = path.join(REPO_ROOT, CALENDAR_BASE, "css", "jquery.minicolors.css");
const EDITOR_JS = path.join(REPO_ROOT, CALENDAR_BASE, "js", "percCalendarTwo.js");

const SAVED = {
  config: [
    {
      calendarName: "Team",
      calendarId: "team@example.com",
      apiKey: "KEY-ONE",
      backgroundColor: "#3a87ad",
      textColor: "#ffffff",
    },
  ],
};

test.describe("percCalendarTwo editor added-row color pickers (issue #6723)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(120_000);
    await loginAsAdmin(page);
  });

  test(
    "added rows get independent working pickers and persist on save",
    { tag: ["@calendar-editor-minicolors", "@classic-ui"] },
    async ({ page }) => {
      const pluginErrors = [];
      page.on("pageerror", (err) => {
        if (/minicolors|percCalendarTwo|perc-google-calendar/i.test(String((err && err.stack) || err))) {
          pluginErrors.push(String((err && err.stack) || err));
        }
      });
      page.on("console", (msg) => {
        if (msg.type() === "error" && /minicolors|percCalendarTwo/i.test(msg.text())) {
          pluginErrors.push(msg.text());
        }
      });

      await page.goto(`${BASE_URL}/Rhythmyx/cm/app/dashboard.jsp`, {
        waitUntil: "domcontentloaded",
      });
      await expect
        .poll(() => page.evaluate(() => typeof window.jQuery === "function"), { timeout: 20_000 })
        .toBe(true);

      // Editor shell mirroring percGoogleCalendarControl.xsl (post-#6723:
      // class-only template inputs, no ids).
      const minicolorsBefore = await page.evaluate((savedJson) => {
        const $ = window.jQuery;
        window.jQuery.PercContentPreSubmitHandlers = {
          handlers: [],
          addHandler(handler) {
            this.handlers.push(handler);
          },
        };
        document.body.insertAdjacentHTML(
          "beforeend",
          '<input type="hidden" name="percCalendarField" ' +
            'id="perc-content-edit-percCalendarField">' +
            '<span class="perc-toggle-google-setup">toggle</span>' +
            '<div class="perc-google-calendar" id="percCalendarField">' +
            '<div class="perc-help" style="display:none;">help</div>' +
            '<div id="perc-google-calendar-setup-editor">' +
            '<span class="perc-table-add fa fa-plus-square"></span>' +
            '<table class="perc-table"><tbody>' +
            "<tr><td>Calendar Name</td><td>Calendar ID</td><td>API Key</td>" +
            "<td>Background Color</td><td>Text Color</td><td></td></tr>" +
            '<tr class="hide perc-google-calendar-row">' +
            '<td><input class="perc-google-calendar-name" type="text"></td>' +
            '<td><input class="perc-google-calendar-id" type="text"></td>' +
            '<td><input class="perc-google-calendar-api-key" type="text"></td>' +
            '<td><input class="perc-google-calendar-background-color" ' +
            'data-control="hue" data-swatches="#ff0000|#00ff00|#0000ff" value="#3a87ad" type="text" aria-label="Background color"></td>' +
            '<td><input class="perc-google-calendar-text-color" ' +
            'data-control="hue" value="#ffffff" type="text" aria-label="Text color"></td>' +
            '<td><span class="perc-table-remove">remove</span></td>' +
            "</tr>" +
            "</tbody></table></div></div>",
        );
        document.querySelector("input[name='percCalendarField']").value = savedJson;
        return typeof $.fn.minicolors;
      }, JSON.stringify(SAVED));
      expect(minicolorsBefore, "minicolors must load with the editor script, not earlier").toBe(
        "undefined",
      );

      // Fixed shipped bytes under test (repo checkout, branch under test).
      // The editor stylesheet twin ships too: the picker panel gets its
      // dimensions from CSS (the sprite png 404s harmlessly against the
      // dashboard origin — no png is served there — without affecting
      // layout or the error gate below).
      await page.addScriptTag({ path: MINICOLORS_JS });
      await page.addScriptTag({ path: EDITOR_JS });
      await page.addStyleTag({ path: MINICOLORS_CSS });
      await page.waitForFunction(
        () => typeof window.jQuery?.googleCalendarSetup === "function",
        null,
        { timeout: 20_000 },
      );
      // The editor inits on document ready: wait until the saved row is
      // restored with initialized pickers.
      await page.waitForFunction(() => {
        const rows = Array.from(
          document.querySelectorAll(".perc-google-calendar-row:not(.hide)"),
        );
        return (
          rows.length === 1 &&
          rows.every((row) =>
            Array.from(
              row.querySelectorAll(
                ".perc-google-calendar-background-color, .perc-google-calendar-text-color",
              ),
            ).every((input) => input.classList.contains("minicolors-input")),
          )
        );
      }, null, { timeout: 20_000 });

      // Real user path: toggle the setup section open, then add two rows.
      await page.locator(".perc-toggle-google-setup").click();
      await page.locator(".perc-table-add").click();
      await page.locator(".perc-table-add").click();
      await expect(page.locator(".perc-google-calendar-row:not(.hide)")).toHaveCount(3);

      const uniqueness = await page.evaluate(() => {
        const seen = new Set();
        let duplicates = 0;
        document.querySelectorAll("[id]").forEach((el) => {
          if (seen.has(el.id)) {
            duplicates += 1;
          }
          seen.add(el.id);
        });
        const rows = Array.from(
          document.querySelectorAll(".perc-google-calendar-row:not(.hide)"),
        );
        return {
          duplicates,
          initialized: rows.map((row) =>
            Array.from(
              row.querySelectorAll(
                ".perc-google-calendar-background-color, .perc-google-calendar-text-color",
              ),
            ).filter((input) => input.classList.contains("minicolors-input")).length,
          ),
        };
      });
      expect(uniqueness.duplicates, "duplicate element ids after cloning").toBe(0);
      expect(uniqueness.initialized, "every row owns two initialized pickers").toEqual([2, 2, 2]);

      // Open the added row's background picker panel and pick a swatch.
      // Each initialized input owns its panel, so scope to the background
      // input's wrapper.
      const addedRow = page.locator(".perc-google-calendar-row:not(.hide)").last();
      const bgPicker = addedRow.locator(
        ".minicolors:has(> .perc-google-calendar-background-color)",
      );
      await addedRow.locator(".perc-google-calendar-background-color").click();
      const panel = bgPicker.locator(".minicolors-panel");
      await expect(panel).toBeVisible({ timeout: 10_000 });
      await page.screenshot({ path: "test-results/live-proof-6723-calendar-minicolors.png" });
      const swatches = bgPicker.locator(".minicolors-swatches .minicolors-swatch");
      expect(await swatches.count()).toBeGreaterThan(0);
      await swatches.nth(1).click();
      const picked = await addedRow
        .locator(".perc-google-calendar-background-color")
        .inputValue();
      expect(picked).toMatch(/^#[0-9a-f]{6}$/i);
      // Independence: the first row still holds its saved color.
      await expect(
        page.locator(".perc-google-calendar-row:not(.hide)").first().locator(
          ".perc-google-calendar-background-color",
        ),
      ).toHaveValue(SAVED.config[0].backgroundColor);

      // Save through the captured pre-submit handler: the picked color
      // persists into the widget JSON.
      await addedRow.locator(".perc-google-calendar-name").fill("Added");
      const savedJson = await page.evaluate(() => {
        const handlers = window.jQuery.PercContentPreSubmitHandlers.handlers;
        if (handlers.length !== 1) {
          throw new Error(`expected 1 pre-submit handler, saw ${handlers.length}`);
        }
        if (handlers[0]() !== true) {
          throw new Error("pre-submit handler rejected the form");
        }
        return document.querySelector("input[name='percCalendarField']").value;
      });
      const saved = JSON.parse(savedJson);
      expect(saved.config).toHaveLength(3);
      expect(saved.config[2]).toMatchObject({
        calendarName: "Added",
        backgroundColor: picked,
        className: "added",
      });

      expect(pluginErrors, `picker/editor errors:\n${pluginErrors.join("\n")}`).toEqual([]);
    },
  );
});
