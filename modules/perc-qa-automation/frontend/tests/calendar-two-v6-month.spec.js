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
 * calendarTwo month view renders on the stock v6 payload (issue #6919,
 * slice 1; plan #6687, parent audit #5627).
 *
 * <p>Hermetic surface lock: inlines the <b>shipped payload bytes</b>
 * ({@code fullcalendar.min.js} + {@code lang-all.js}) into a bare page —
 * no CMS, no DTS, no network — and asserts the month-view contract the
 * rewritten {@code percCalendarTwoSnippet.vm} relies on: 6-week dayGrid
 * grid, static events with v6 {@code .fc-event-title} hooks, the month
 * title, destroy + re-init (view-toggle mechanics), and the {@code fr}
 * locale. Zero console errors and zero page errors required. The
 * published-page parity (month + list + toggle screenshot diffs on a
 * served fixture page) is slice 2 ([6919/2]); this spec is the runnable-
 * here browser proof for the payload swap itself.
 *
 * <p>Tags: {@code @published} {@code @published-widget}
 * {@code @widget-percCalendarTwo}</p>
 *
 * <p>Run:
 * {@code npm run test:surface -- --path tests/calendar-two-v6-month.spec.js}</p>
 */

const fs = require("node:fs");
const path = require("node:path");
const { test, expect } = require("@playwright/test");
const { proofScreenshot } = require("./helpers/proof-shot");

const REPO_ROOT = path.join(__dirname, "..", "..", "..", "..");
const CALENDARTWO_JS = path.join(
  REPO_ROOT,
  "modules",
  "perc-packages",
  "src",
  "main",
  "resources",
  "Packages",
  "perc.widget.calendar",
  "sys__UserDependency--web_resources",
  "widgets",
  "calendarTwo",
  "js",
);

function inlinePayload(filename) {
  // Escape the script-closing sequence for safe inlining into the harness
  // page (<\/script is identical JS in strings and regexes; the payload
  // files on disk are untouched).
  return fs
    .readFileSync(path.join(CALENDARTWO_JS, filename), "utf8")
    .replace(/<\/script/gi, "<\\/script");
}

function harnessHtml() {
  return `<!doctype html>
<html lang="en">
<head><meta charset="utf-8"><title>calendarTwo v6 month harness</title></head>
<body>
<div id="cal-month"></div>
<div id="cal-fr"></div>
<script>${inlinePayload("fullcalendar.min.js")}</script>
<script>${inlinePayload("lang-all.js")}</script>
</body>
</html>`;
}

test.describe("calendarTwo v6 month render (issue #6919 slice 1)", () => {
  test("month grid, events, title, destroy/re-init and fr locale render with zero console errors",
    { tag: ["@static"] },
    async ({
    page,
  }) => {
    test.setTimeout(90_000);
    const noise = [];
    page.on("console", (message) => {
      if (message.type() === "error") {
        noise.push(`console.error: ${message.text()}`);
      }
    });
    page.on("pageerror", (error) => {
      noise.push(`pageerror: ${error.message}`);
    });

    await page.setContent(harnessHtml());
    await expect(
      page.evaluate(() => typeof window.FullCalendar?.Calendar),
    ).resolves.toBe("function");
    await expect(
      page.evaluate(() => window.FullCalendar.version),
    ).resolves.toBe("6.1.15");

    // Month layout mirrors the snippet init: static events through the
    // events function, November 2026 default month.
    await page.evaluate(() => {
      const calendar = new window.FullCalendar.Calendar(
        document.getElementById("cal-month"),
        {
          initialView: "dayGridMonth",
          initialDate: "2026-11-01",
          headerToolbar: { left: "title", center: "", right: "" },
          events: [
            { title: "Lock Event", start: "2026-11-10" },
            { title: "Second Lock Event", start: "2026-11-18" },
          ],
        },
      );
      calendar.render();
      window.monthCalendar = calendar;
    });

    const month = page.locator("#cal-month");
    await expect(month.locator(".fc-daygrid-day")).toHaveCount(42);
    await expect(month.locator(".fc-event .fc-event-title")).toHaveCount(2);
    await expect(
      month.locator(".fc-event .fc-event-title").first(),
    ).toContainText("Lock Event");
    await expect(month.locator("h2.fc-toolbar-title")).toContainText(
      "November 2026",
    );

    // Locale companion: the snippet passes the widget language as locale:.
    await page.evaluate(() => {
      const fr = new window.FullCalendar.Calendar(
        document.getElementById("cal-fr"),
        {
          initialView: "dayGridMonth",
          initialDate: "2026-11-01",
          locale: "fr",
          headerToolbar: { left: "title", center: "", right: "" },
          events: [],
        },
      );
      fr.render();
      window.frCalendar = fr;
    });
    await expect(page.locator("#cal-fr h2")).toContainText("novembre");

    await proofScreenshot(page, "issue-6919-calendar-two-v6-month.png");

    // View-toggle mechanics: destroy tears down, re-init restores.
    await page.evaluate(() => {
      window.monthCalendar.destroy();
    });
    await expect(month.locator(".fc-view")).toHaveCount(0);
    await page.evaluate(() => {
      const calendar = new window.FullCalendar.Calendar(
        document.getElementById("cal-month"),
        {
          initialView: "dayGridMonth",
          initialDate: "2026-11-01",
          headerToolbar: false,
          events: [{ title: "Toggle Event", start: "2026-11-10" }],
        },
      );
      calendar.render();
    });
    await expect(month.locator(".fc-view")).toHaveCount(1);
    await expect(month.locator(".fc-event")).toHaveCount(1);

    expect(noise, `expected zero page noise, got: ${noise.join(" | ")}`).toEqual(
      [],
    );
  });
});
