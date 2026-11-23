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
 * calendarTwo list-view + toggle + print + mini parity on the stock v6
 * payload (issue #6978, residual of slice 2 #6945, parent #6919; plan
 * #6687, audit #5627).
 *
 * <p>Slice 2 (PR #6977) proved this parity with a one-off hermetic proof
 * (19/19 checks, 6 screenshots, since the sibling lane owned
 * {@code modules/perc-qa-automation} for QA #6927 no spec was committed).
 * This file commits that proof next to
 * {@code calendar-two-v6-month.spec.js} so the parity stays locked.
 *
 * <p>Hermetic surface lock: inlines the <b>shipped payload bytes</b>
 * ({@code fullcalendar.min.js} + {@code lang-all.js} +
 * {@code moment.min.js}) and the <b>shipped print stylesheet bytes</b>
 * ({@code fullcalendar.print.css}) into a bare page — no CMS, no DTS, no
 * network — and asserts the slice-2 contract the rewritten
 * {@code percCalendarTwoSnippet.vm} relies on:
 *
 * <ul>
 *   <li>List-view render + toggle round-trip: real v6
 *       {@code destroy()}/re-init, {@code aria-pressed} sync,
 *       {@code Perc.range} 6/52, the loading row, the empty state, and the
 *       today-or-later + {@code maxResults} filter.</li>
 *   <li>Print-media emulation: toolbar/toggle chrome hidden, events
 *       black-on-white.</li>
 *   <li>Mini-calendar: the {@code highlightDays} hook and the
 *       {@code fullCalendarPage + "?startDate=" + clickedDate} link
 *       contract.</li>
 * </ul>
 *
 * <p>Fidelity notes (read before "fixing" the harness):
 *
 * <ul>
 *   <li>The v6 bundle, locale pack, moment, and print CSS are the real
 *       shipped bytes read from disk — byte-identity with the payload.</li>
 *   <li>The toggle/list wiring is a statement-level port of the snippet
 *       handlers ({@code percInitListView}/{@code percProcessEventList}
 *       and the two {@code show*Button} click handlers) because the
 *       snippet runs on CMS-served jQuery, which has no committed
 *       source-tree copy to inline (only build-output and
 *       {@code node_modules} copies exist). Every hook string the port
 *       uses ({@code perc-event-list-loading},
 *       {@code perc-event-list-empty}, {@code "No upcoming events."},
 *       {@code aria-live}, {@code aria-pressed}, {@code Perc.range},
 *       the date comparator, {@code destroy()}) is pinned against the
 *       snippet source by
 *       {@code calendarTwoListToggleParity.test.js} — a drift there
 *       fails Vitest loudly.</li>
 *   <li>The mini {@code highlightDays} body below is <b>not</b> a port:
 *       it is extracted from the snippet source at runtime and evaluated
 *       in-page, so the real snippet function decides the highlight.</li>
 *   <li>The datepicker widget chrome itself is CMS-served jQuery UI
 *       (unchanged by slice 2); what slice 2 owns — and what is pinned
 *       here — is the highlight hook plus the link-URL contract.</li>
 *   <li>Keyboard operation of the source-filter buttons (issue #6992)
 *       is an owned residual and is intentionally <b>not</b> asserted
 *       here (see
 *       {@code calendar-two-source-filter-keyboard.spec.js}).</li>
 *   <li>List-string i18n (issue #6993) keeps its English defaults here
 *       so this parity lock stays byte-stable; the preference override
 *       path is locked separately by
 *       {@code calendar-two-list-strings-i18n.spec.js}.</li>
 * </ul>
 *
 * <p>Run:
 * {@code npm run test:surface -- --path tests/calendar-two-v6-list.spec.js}</p>
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
const PRINT_CSS_PATH = path.join(
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
  "css",
  "fullcalendar.print.css",
);
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

// Escape the script-closing sequence for safe inlining into the harness
// page (identical JS in strings and regexes; the payload files on disk
// are untouched).
function inlinePayload(filename) {
  return fs
    .readFileSync(path.join(CALENDARTWO_JS, filename), "utf8")
    .replace(/<\/script/gi, "<\\/script");
}

function printCss() {
  return fs.readFileSync(PRINT_CSS_PATH, "utf8");
}

// Extracts the real highlightDays function source from the snippet (brace
// matching from the declaration) so the page evaluates the actual snippet
// code rather than a copy.
function extractHighlightDays(snippet) {
  const marker = "function highlightDays(date) {";
  const start = snippet.indexOf(marker);
  if (start < 0) {
    throw new Error("highlightDays not found in snippet");
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
  throw new Error("highlightDays braces do not balance in snippet");
}

// Toggle + list markup mirrors the snippet with concrete widget ids
// (calendarContentId=cal1, widgetId=w1). The toggle container id has no
// suffix in the snippet itself, reproduced verbatim.
function toggleListMarkup() {
  return `
  <div id="perc-toggle-button-container" class="perc-toggle-button-container" role="group" aria-label="Calendar layout">
  <button type="button" id="showCalendarButton-cal1-w1" class="fc-button fc-state-default fc-corner-left fc-corner-right perc-type-toggle" aria-pressed="true">Calendar View</button>
  <button type="button" id="showListButton-cal1-w1" class="fc-button fc-state-default fc-corner-left fc-corner-right perc-type-toggle" aria-pressed="false">List View</button>
  </div>
  <div class="perc-mini-calendar" id="perc-mini-calendar-stub" style="display:none"></div>
  <div class="perc-full-calendar" id="calendar-cal1-w1"></div>
  <div id="perc-button-container-cal1-w1" class="perc-button-container">
  <div id="perc-calendar-other-button" class="perc-calendar-button perc-calendar-other">Percussion Events</div>
  </div>
<div id="perc-event-list-template-cal1-w1" class="perc-event-list-template">
    <ul class="perc-list-main">
        <li class="perc-event-list-element perc-list-element">
            <div class="perc-cal-icon">
                <div class="perc-month-mmm">MMM</div>
                <div class="perc-date-dd">DD</div>
            </div>
            <div class="perc-event-info">
                <div class="perc-event-title"><h4><a class="perc-event-title-link" href="" target="_blank" rel="noopener noreferrer">Event Title</a></h4></div>
        <div class="perc-event-date">mm/dd/yyyy</div>
                <div class="perc-event-time">Event Time</div>
                </div>
        </li>
    </ul>
</div>
<div id="perc-event-list-cal1-w1" class="perc-event-list" aria-live="polite" style="display:none">
    <ul></ul>
</div>`;
}

function harnessHtml() {
  return `<!doctype html>
<html lang="en">
<head><meta charset="utf-8"><title>calendarTwo v6 list/toggle/print/mini harness</title></head>
<body>
${toggleListMarkup()}
<script>${inlinePayload("fullcalendar.min.js")}</script>
<script>${inlinePayload("lang-all.js")}</script>
<script>${inlinePayload("moment.min.js")}</script>
</body>
</html>`;
}

// Statement-level port of the snippet toggle + list wiring (see the file
// header for why the port exists and what pins its drift). jQuery-ism to
// vanilla mapping: show()/hide() -> style.display, .attr('aria-pressed')
// -> setAttribute, .empty().append() -> innerHTML, .clone() ->
// cloneNode, .on("click") -> addEventListener. The FullCalendar calls,
// the moment calls, the comparator, the class names, and the loading/
// empty strings are the snippet's own.
function portedWiringScript() {
  return `
    window.Perc = { range: 6, events: {} };
    window.__ROOT = 'cal1-w1';
    window.__maxResults = 25;
    window.__showIcon = true;
    window.__showDate = true;
    window.__showTime = true;
    window.__cal = null;
    window.__listFeed = null;

    function portedSetPressed(calPressed, listPressed) {
      document.getElementById('showCalendarButton-' + window.__ROOT)
        .setAttribute('aria-pressed', String(calPressed));
      document.getElementById('showListButton-' + window.__ROOT)
        .setAttribute('aria-pressed', String(listPressed));
    }

    function portedRenderMonth(events) {
      var el = document.getElementById('calendar-' + window.__ROOT);
      window.__cal = new FullCalendar.Calendar(el, {
        initialView: 'dayGridMonth',
        initialDate: '2026-11-01',
        headerToolbar: {
          left: 'title',
          center: 'prev,next today',
          right: 'dayGridMonth,timeGridWeek,timeGridDay',
        },
        events: events,
      });
      window.__cal.render();
    }

    // Port of percInitListView: hide the calendar, show the list region
    // with the loading row, then feed asynchronously (the snippet awaits
    // the DTS fetch; the harness awaits a macrotask so the loading row is
    // observable before the rows land). Tests that pin the loading row
    // install window.__listGate (a promise) before clicking List View and
    // resolve window.__listRelease after the loading assertion passes, so
    // the loading state is guaranteed to be observable (#7148 review).
    function portedInitListView() {
      document.getElementById('calendar-' + window.__ROOT).style.display = 'none';
      var list = document.getElementById('perc-event-list-' + window.__ROOT);
      list.style.display = '';
      list.querySelector('ul').innerHTML =
        '<li class="perc-event-list-element perc-event-list-loading">Loading events\\u2026</li>';
      var feed = window.__listFeed || [];
      var gate = window.__listGate || null;
      window.__listGate = null;
      if (gate) {
        gate.then(function () {
          portedProcessEventList(feed, window.__maxResults);
        });
      } else {
        setTimeout(function () {
          portedProcessEventList(feed, window.__maxResults);
        }, 0);
      }
    }

    // Port of percProcessEventList: same comparator, same
    // today-or-later + maxResults gate, same first/last/odd/even
    // classes, same url/icon/date/time handling, same moment formats.
    function portedProcessEventList(eventObject, maxResults) {
      var list = document.getElementById('perc-event-list-' + window.__ROOT);
      list.style.display = '';
      list.querySelector('ul').innerHTML = '';
      var sorted = (eventObject || []).slice().sort(function (x, y) {
        return new Date(x.start) - new Date(y.start);
      });
      var now = new Date();
      var template = document.querySelector(
        '#perc-event-list-template-' + window.__ROOT + ' ul li');
      var upcoming = sorted.filter(function (event) {
        return !moment(event.start).isBefore(now, 'day');
      });
      var shown = upcoming.slice(0, maxResults);
      var iterator = 0;
      shown.forEach(function (event) {
        var row = template.cloneNode(true);
        if (iterator === 0) {
          row.classList.add('perc-list-first');
        }
        if (iterator === shown.length - 1) {
          row.classList.add('perc-list-last');
        }
        row.classList.add((iterator + 1) % 2 === 0 ? 'perc-list-even' : 'perc-list-odd');
        var link = row.querySelector('a');
        if (!event.url) {
          link.removeAttribute('href');
        } else {
          link.setAttribute('href', event.url);
        }
        if (!window.__showIcon) {
          var icon = row.querySelector('.perc-cal-icon');
          if (icon) {
            icon.remove();
          }
        }
        if (!window.__showDate) {
          var dateEl = row.querySelector('.perc-event-date');
          if (dateEl) {
            dateEl.remove();
          }
        }
        if (!window.__showTime) {
          var timeEl = row.querySelector('.perc-event-time');
          if (timeEl) {
            timeEl.remove();
          }
        }
        row.querySelector('.perc-event-date').textContent =
          moment(event.start).format('MM/DD/YYYY');
        var mmm = row.querySelector('.perc-month-mmm');
        if (mmm) {
          mmm.textContent = moment(event.start).format('MMM').toUpperCase();
        }
        var dd = row.querySelector('.perc-date-dd');
        if (dd) {
          dd.textContent = moment(event.start).format('D');
        }
        var timeNode = row.querySelector('.perc-event-time');
        timeNode.textContent = moment(event.start).format('h:mm a');
        if (event.end) {
          timeNode.textContent += ' - ' + moment(event.end).format('h:mm a');
        }
        row.querySelector('.perc-event-title-link').textContent = event.title;
        list.querySelector('ul').appendChild(row);
        iterator += 1;
      });
      if (iterator === 0) {
        list.querySelector('ul').innerHTML =
          '<li class="perc-event-list-element perc-event-list-first perc-event-list-last perc-event-list-empty">No upcoming events.</li>';
      }
    }

    // Port of the showCalendarButton click handler.
    document.getElementById('showCalendarButton-' + window.__ROOT)
      .addEventListener('click', function () {
        document.getElementById('perc-event-list-' + window.__ROOT).style.display = 'none';
        window.Perc.range = 6;
        if (window.__cal) {
          window.__cal.destroy();
          window.__cal = null;
        }
        document.getElementById('calendar-' + window.__ROOT).style.display = '';
        portedRenderMonth(window.__monthFixture || []);
        document.getElementById('perc-button-container-' + window.__ROOT).style.display = '';
        portedSetPressed(true, false);
      });

    // Port of the showListButton click handler.
    document.getElementById('showListButton-' + window.__ROOT)
      .addEventListener('click', function () {
        document.getElementById('perc-button-container-' + window.__ROOT).style.display = 'none';
        if (window.__cal) {
          window.__cal.destroy();
          window.__cal = null;
        }
        window.Perc.range = 52;
        document.querySelector(
          '#perc-event-list-' + window.__ROOT + ' ul').innerHTML = '';
        portedInitListView();
        portedSetPressed(false, true);
      });
  `;
}

function monthFixture() {
  return [
    { title: "Lock Event", start: "2026-11-10" },
    { title: "Second Lock Event", start: "2026-11-18" },
  ];
}

function futureListFixture() {
  const day = 24 * 60 * 60 * 1000;
  const now = Date.now();
  const iso = (t) => new Date(t).toISOString();
  return [
    { title: "Past Event", start: iso(now - 30 * day) },
    {
      title: "Soon Event",
      start: iso(now + 2 * day),
      end: iso(now + 2 * day + 3600000),
      url: "https://example.invalid/soon",
    },
    { title: "Later Event", start: iso(now + 5 * day) },
    { title: "Last Event", start: iso(now + 9 * day) },
  ];
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

test.describe("calendarTwo v6 list/toggle/print/mini parity (issue #6978)", () => {
  test("list-view render + toggle round-trip with loading row and aria-pressed sync",
    { tag: ["@static"] },
    async ({
    page,
  }) => {
    test.setTimeout(90_000);
    const noise = [];
    listenForNoise(page, noise);

    await page.setContent(harnessHtml());
    await expect(
      page.evaluate(() => window.FullCalendar.version),
    ).resolves.toBe("6.1.15");
    await page.evaluate(portedWiringScript());
    await page.evaluate((events) => {
      window.__monthFixture = events;
      portedRenderMonth(events);
    }, monthFixture());

    const calendar = page.locator("#calendar-cal1-w1");
    const list = page.locator("#perc-event-list-cal1-w1");
    const calButton = page.locator("#showCalendarButton-cal1-w1");
    const listButton = page.locator("#showListButton-cal1-w1");

    // Month baseline (parity anchor shared with the month spec).
    await expect(calendar.locator(".fc-daygrid-day")).toHaveCount(42);
    await expect(calendar.locator(".fc-event .fc-event-title")).toHaveCount(2);
    await expect(calendar.locator("h2.fc-toolbar-title")).toContainText(
      "November 2026",
    );
    await expect(calButton).toHaveAttribute("aria-pressed", "true");
    await expect(listButton).toHaveAttribute("aria-pressed", "false");
    await expect(list).toBeHidden();

    // Toggle to list: the loading row covers the async fetch window.
    // The feed is held behind a gate released only after the loading
    // assertion passes, so the loading state is guaranteed observable
    // instead of racing the stubbed fetch (#7148 review).
    await page.evaluate((events) => {
      window.__listFeed = events;
      window.__listGate = new Promise((resolve) => {
        window.__listRelease = resolve;
      });
    }, futureListFixture());
    await listButton.click();
    await expect(
      list.locator(".perc-event-list-loading"),
    ).toContainText("Loading events");
    await page.evaluate(() => window.__listRelease());
    await expect(list.locator(".perc-event-list-element")).toHaveCount(3, {
      timeout: 10_000,
    });

    // Past event filtered, loading row dropped, newest-first sort kept.
    await expect(
      list.locator(".perc-event-list-loading"),
    ).toHaveCount(0);
    await expect(list.locator(".perc-event-list-element")).toHaveCount(3);
    await expect(list.locator(".perc-event-list-element").first()).toContainText(
      "Soon Event",
    );
    await expect(list.locator(".perc-event-list-element").first()).toHaveClass(
      /perc-list-first/,
    );
    await expect(list.locator(".perc-event-list-element").first()).toHaveClass(
      /perc-list-odd/,
    );
    await expect(list.locator(".perc-event-list-element").nth(1)).toHaveClass(
      /perc-list-even/,
    );
    await expect(list.locator(".perc-event-list-element").last()).toHaveClass(
      /perc-list-last/,
    );
    // Icon + date + time hooks render for every row.
    await expect(list.locator(".perc-month-mmm").first()).not.toBeEmpty();
    await expect(list.locator(".perc-date-dd").first()).not.toBeEmpty();
    await expect(list.locator(".perc-event-time").first()).not.toBeEmpty();
    // URL contract: keyed event keeps its href, unkeyed drops it.
    await expect(
      list.locator(".perc-event-list-element").first().locator("a"),
    ).toHaveAttribute("href", "https://example.invalid/soon");
    await expect(
      list.locator(".perc-event-list-element").nth(1).locator("a"),
    ).not.toHaveAttribute("href", /./);
    // Toggle state follows the visible layout; list fetch window is 52.
    await expect(calButton).toHaveAttribute("aria-pressed", "false");
    await expect(listButton).toHaveAttribute("aria-pressed", "true");
    await expect(
      page.evaluate(() => window.Perc.range),
    ).resolves.toBe(52);
    await expect(calendar).toBeHidden();
    await expect(list).toBeVisible();
    await expect(list).toHaveAttribute("aria-live", "polite");

    await proofScreenshot(page, "issue-6978-list.png");

    // Toggle back: destroy + re-init restores the month view.
    await calButton.click();
    await expect(calendar.locator(".fc-view")).toHaveCount(1);
    await expect(calendar.locator(".fc-event")).toHaveCount(2);
    await expect(calendar).toBeVisible();
    await expect(list).toBeHidden();
    await expect(calButton).toHaveAttribute("aria-pressed", "true");
    await expect(listButton).toHaveAttribute("aria-pressed", "false");
    await expect(page.evaluate(() => window.Perc.range)).resolves.toBe(6);

    await proofScreenshot(page, "issue-6978-toggle-back.png");

    expectQuiet(noise);
  });

  test("list-view empty state replaces the loading row",
    { tag: ["@static"] },
    async ({ page }) => {
    test.setTimeout(90_000);
    const noise = [];
    listenForNoise(page, noise);

    await page.setContent(harnessHtml());
    await page.evaluate(portedWiringScript());
    await page.evaluate((events) => {
      window.__monthFixture = events;
      portedRenderMonth(events);
    }, monthFixture());

    const list = page.locator("#perc-event-list-cal1-w1");
    await page.evaluate(() => {
      window.__listFeed = [];
      window.__listGate = new Promise((resolve) => {
        window.__listRelease = resolve;
      });
    });
    await page.locator("#showListButton-cal1-w1").click();
    await expect(
      list.locator(".perc-event-list-loading"),
    ).toContainText("Loading events");
    await page.evaluate(() => window.__listRelease());
    await expect(list.locator(".perc-event-list-empty")).toHaveCount(1, {
      timeout: 10_000,
    });

    await expect(list.locator(".perc-event-list-empty")).toContainText(
      "No upcoming events.",
    );
    await expect(list.locator(".perc-event-list-empty")).toHaveClass(
      /perc-event-list-first/,
    );
    await expect(list.locator(".perc-event-list-empty")).toHaveClass(
      /perc-event-list-last/,
    );
    await expect(list.locator(".perc-event-list-element")).toHaveCount(1);
    await expect(list).toHaveAttribute("aria-live", "polite");

    await proofScreenshot(page, "issue-6978-list-empty.png");

    expectQuiet(noise);
  });

  test("print-media emulation hides chrome and flattens events",
    { tag: ["@static"] },
    async ({
    page,
  }) => {
    test.setTimeout(90_000);
    const noise = [];
    listenForNoise(page, noise);

    await page.setContent(harnessHtml());
    await page.evaluate(portedWiringScript());
    await page.evaluate((events) => {
      window.__monthFixture = events;
      portedRenderMonth(events);
    }, monthFixture());
    // A populated list row exercises the list page-break rule.
    await page.evaluate((events) => {
      window.__listFeed = events;
      portedProcessEventList(events, 25);
    }, futureListFixture());
    await page.addStyleTag({ content: printCss() });
    await page.emulateMedia({ media: "print" });

    // Interactive chrome hides in print (toggles via their container,
    // toolbar buttons via the .fc rule, source-filter bar directly).
    await expect(page.locator("#perc-toggle-button-container")).toHaveCSS(
      "display",
      "none",
    );
    await expect(page.locator("#perc-button-container-cal1-w1")).toHaveCSS(
      "display",
      "none",
    );
    await expect(
      page.locator("#calendar-cal1-w1 .fc-prev-button"),
    ).toHaveCSS("display", "none");
    // Events flatten to black-on-white.
    const firstEvent = page.locator("#calendar-cal1-w1 .fc-event").first();
    await expect(firstEvent).toHaveCSS("color", "rgb(0, 0, 0)");
    await expect(firstEvent).toHaveCSS("background-color", "rgb(255, 255, 255)");
    // List rows avoid page breaks.
    await expect(
      page.locator("#perc-event-list-cal1-w1 .perc-event-list-element").first(),
    ).toHaveCSS("page-break-inside", "avoid");

    await proofScreenshot(page, "issue-6978-print.png");

    expectQuiet(noise);
  });

  test("mini-calendar highlight hook + fullCalendarPage link contract",
    { tag: ["@static"] },
    async ({
    page,
  }) => {
    test.setTimeout(90_000);
    const noise = [];
    listenForNoise(page, noise);
    const snippet = fs.readFileSync(SNIPPET_PATH, "utf8");

    // Both template branches (edit/preview sample data and the published
    // DTS-backed branch) carry the identical onSelect contract — mirrors
    // the calendarTwoMiniCalendarContract Vitest pins.
    const onSelectCount = (
      snippet.match(/onSelect:\s*function\s*\(\s*dateText\s*,\s*inst\s*\)/g) ??
      []
    ).length;
    expect(onSelectCount).toBe(2);
    const contractCount =
      snippet.split('fullCalendarPage + "?startDate=" + clickedDate').length -
      1;
    expect(contractCount).toBe(2);
    const normalizeCount =
      snippet.split("dateText.replace(/\\//g, ',')").length - 1;
    expect(normalizeCount).toBe(2);

    // The real snippet highlightDays decides the highlight: event days
    // get the highlight class, plain days do not.
    const highlightSource = extractHighlightDays(snippet);
    const highlight = await page.evaluate(
      ({ source, eventDates }) => {
        // eslint-disable-next-line no-eval
        const run = new Function(
          "dateWithData",
          `${source}; return { eventDay: highlightDays(new Date('2026-11-21T12:00:00')), plainDay: highlightDays(new Date('2026-11-22T12:00:00')) };`,
        );
        return run(eventDates);
      },
      { source: highlightSource, eventDates: ["2026-11-10T12:00:00", "2026-11-21T12:00:00"] },
    );
    expect(highlight.eventDay).toEqual([true, "highlight"]);
    expect(highlight.plainDay).toEqual([true, ""]);

    // The real snippet link expression routes a picked day at the full
    // calendar page (11/21/2026 -> ?startDate=11,21,2026).
    const linkTarget = await page.evaluate(() => {
      const fullCalendarPage = "/calendar";
      const dateText = "11/21/2026";
      const clickedDate = dateText.replace(/\//g, ",");
      return fullCalendarPage + "?startDate=" + clickedDate;
    });
    expect(linkTarget).toBe("/calendar?startDate=11,21,2026");

    // Demonstrate the hook end-to-end on a day grid: cells whose date
    // carries events highlight.
    await page.setContent(`<!doctype html>
<html lang="en">
<head><meta charset="utf-8"><title>mini highlight demo</title>
<style>.mini-day { display: inline-block; width: 40px; height: 40px; } .highlight { background: #ffe08a; }</style>
</head>
<body><div id="mini-demo"></div></body>
</html>`);
    await page.evaluate(
      ({ source, eventDates }) => {
        const demo = document.getElementById("mini-demo");
        for (let day = 1; day <= 30; day += 1) {
          const cell = document.createElement("div");
          cell.className = "mini-day";
          const iso = `2026-11-${String(day).padStart(2, "0")}`;
          cell.setAttribute("data-date", iso);
          cell.textContent = String(day);
          demo.appendChild(cell);
        }
        // eslint-disable-next-line no-eval
        const apply = new Function(
          "dateWithData",
          `${source}; return function (iso) { return highlightDays(new Date(iso + 'T12:00:00')); };`,
        )(eventDates);
        document.querySelectorAll("#mini-demo .mini-day").forEach((cell) => {
          const verdict = apply(cell.getAttribute("data-date"));
          if (verdict[1] === "highlight") {
            cell.classList.add("highlight");
          }
        });
      },
      { source: highlightSource, eventDates: ["2026-11-10T12:00:00", "2026-11-21T12:00:00"] },
    );
    await expect(page.locator('#mini-demo [data-date="2026-11-10"]')).toHaveClass(
      /highlight/,
    );
    await expect(page.locator('#mini-demo [data-date="2026-11-21"]')).toHaveClass(
      /highlight/,
    );
    await expect(page.locator('#mini-demo [data-date="2026-11-22"]')).not.toHaveClass(
      /highlight/,
    );

    await proofScreenshot(page, "issue-6978-mini.png");

    expectQuiet(noise);
  });
});
