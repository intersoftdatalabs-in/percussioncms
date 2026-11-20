/**
 * CalendarTwo FullCalendar consumed-surface lock (issue #6687, parent audit
 * #5627).
 *
 * <p>The published calendarTwo widget serves FullCalendar 2.9.1 (EOL since
 * 2016, no upstream fixes) to site visitors. Slice 5 of #6502 rebased the
 * payload pair onto the npm {@code fullcalendar@2.9.1} dist plus the two
 * documented Percussion mods, and the parity block in
 * {@code widgetPayloadNpmParity.test.js} pins those bytes — but byte parity
 * alone does not record <em>which</em> 2.x behaviors the replacement must
 * preserve. This file locks the widget-consumed surface behaviorally, loading
 * the <b>payload</b> bytes (not {@code node_modules}) so any payload drift
 * fails loudly:
 *
 * <ul>
 *   <li>jQuery plugin entry point + 2.9.1 version ({@code $.fn.fullCalendar}).
 *   <li>Month-view calendar/list layouts driven by the {@code Perc.range}
 *       mod (6 weeks for calendar layout, 52 for list layout — set by
 *       {@code percCalendarTwoSnippet.vm} before init).
 *   <li>The {@code Perc.events} deferred the snippet awaits for list-view
 *       rendering ({@code reportEvents} resolves it per widget id).
 *   <li>{@code destroy} + re-init (the calendar/list view-toggle contract).
 *   <li>gcal companion: {@code googleCalendarId} sources route through the
 *       plugin fetcher.
 *   <li>lang-all companion: the {@code lang:} option localizes the header
 *       (the snippet passes {@code perc_calendar_display_language}).
 * </ul>
 *
 * <p>Any maintained successor (see the #6687 replacement plan) must satisfy
 * this same surface — port this file to the new API in the migration PR and
 * keep it green.
 *
 * <p>Harness notes. Published pages run the CMS jQuery plus the shipped
 * jquery-migrate, so the harness loads {@code jquery-migrate} first: the
 * 2.9.1 dist predates jQuery 4 and calls the removed {@code $.isFunction} /
 * {@code $.isArray} / {@code $.type} / {@code $.isPlainObject} utilities.
 * jsdom has no layout engine so every element reports {@code :hidden} and the
 * 2.9.1 {@code renderView} skips {@code display()} for hidden elements; the
 * harness reports elements visible to exercise the code path real browsers
 * run (the widget container is visible on published pages).
 */
import { describe, expect, it } from "vitest";
import fs from "node:fs";
import path from "node:path";
import { createRequire } from "node:module";
import { fileURLToPath } from "node:url";

const require = createRequire(import.meta.url);
const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(fileURLToPath(import.meta.url));

// src/test/js -> frontend root (peer: widgetPayloadNpmParity.test.js), then
// up to the repo root so the perc-packages payloads resolve portably.
const FRONTEND_ROOT = path.dirname(path.dirname(path.dirname(__dirname)));
const REPO_ROOT = path.join(FRONTEND_ROOT, "..", "..", "..", "..");
const NODE_MODULES = path.join(FRONTEND_ROOT, "node_modules");

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

function loadCalendarTwoRuntime() {
  // Mirror the widget script order (percCalendarTwo.xml): jQuery, moment,
  // fullcalendar.min.js, then the gcal/lang-all companions. Loading the
  // payload min build also exercises the shipped artifact, not the npm dist.
  const $ = require(path.join(NODE_MODULES, "jquery", "dist", "jquery.js"));
  require(path.join(
    NODE_MODULES,
    "jquery-migrate",
    "dist",
    "jquery-migrate.js",
  ));
  const moment = require(
    path.join(NODE_MODULES, "moment", "min", "moment.min.js"),
  );
  globalThis.jQuery = $;
  window.jQuery = $;
  globalThis.moment = moment;
  window.moment = moment;
  window.eval(
    fs.readFileSync(path.join(CALENDARTWO_JS, "fullcalendar.min.js"), "utf8"),
  );
  window.eval(fs.readFileSync(path.join(CALENDARTWO_JS, "gcal.js"), "utf8"));
  window.eval(
    fs.readFileSync(path.join(CALENDARTWO_JS, "lang-all.js"), "utf8"),
  );
  // jsdom never reports an element visible (no layout); the 2.9.1 renderView
  // skips display() for hidden elements, so report visible to mirror pages.
  $.expr.pseudos.visible = () => true;
  $.expr.pseudos.hidden = () => false;
  return { $, moment };
}

function initCalendar($, id, options) {
  // Mirror the snippet contract: the template sets Perc.range per layout
  // and creates one Deferred per widget id before calling fullCalendar().
  document.body.insertAdjacentHTML(
    "beforeend",
    `<div id="${id}"></div>`,
  );
  window.Perc.events[id] = $.Deferred();
  $(`#${id}`).fullCalendar({
    defaultView: "month",
    defaultDate: "2026-11-01",
    header: { left: "title", center: "", right: "" },
    ...options,
  });
  return $(`#${id}`);
}

describe("calendarTwo FullCalendar consumed-surface lock (issue #6687)", () => {
  it("exposes the widget-consumed plugin entry point at 2.9.1", () => {
    const { $ } = loadCalendarTwoRuntime();
    expect(typeof $.fn.fullCalendar).toBe("function");
    expect($.fullCalendar.version).toBe("2.9.1");
  });

  it("renders the month grid and the fetched events (calendar layout)", () => {
    const { $ } = loadCalendarTwoRuntime();
    window.Perc = { events: {}, range: 6 };
    const el = initCalendar($, "calendar-surface-cal", {
      events: [
        { title: "Lock Event", start: "2026-11-10" },
        { title: "Second Lock Event", start: "2026-11-18" },
      ],
    });
    // Month grid: fixed weekMode renders whole weeks covering November 2026.
    const dayCells = document.querySelectorAll("#calendar-surface-cal td.fc-day");
    expect(dayCells.length).toBeGreaterThan(0);
    expect(dayCells.length % 7).toBe(0);
    // The static source flowed through reportEvents into the grid.
    const rendered = document.querySelectorAll(
      "#calendar-surface-cal .fc-event .fc-title",
    );
    const titles = Array.from(rendered).map((node) => node.textContent);
    expect(titles).toContain("Lock Event");
    expect(titles).toContain("Second Lock Event");
    // Header title follows the rendered month.
    expect(
      document.querySelector("#calendar-surface-cal h2").textContent,
    ).toContain("November");
    el.fullCalendar("destroy");
  });

  it("resolves the Perc.events deferred with the fetched events (list-view contract)", () => {
    const { $ } = loadCalendarTwoRuntime();
    window.Perc = { events: {}, range: 52 };
    const el = initCalendar($, "calendar-surface-list", {
      events: [{ title: "Deferred Event", start: "2026-11-10" }],
    });
    // The snippet awaits this Deferred to populate the event list.
    const deferred = window.Perc.events["calendar-surface-list"];
    expect(deferred.state()).toBe("resolved");
    el.fullCalendar("destroy");
  });

  it("expands the fetched month range from Perc.range (6 calendar vs 52 list)", () => {
    const { $ } = loadCalendarTwoRuntime();
    window.Perc = { events: {}, range: 6 };
    const calendarEl = initCalendar($, "calendar-surface-range-cal", {
      events: [],
    });
    const calendarView = calendarEl.fullCalendar("getView");
    const calendarWeeks = calendarView.end.diff(calendarView.start, "weeks");
    calendarEl.fullCalendar("destroy");

    // The snippet flips Perc.range to 52 before re-initializing for list
    // layout so events beyond the visible 6 weeks still resolve.
    window.Perc.range = 52;
    const listEl = initCalendar($, "calendar-surface-range-list", {
      events: [],
    });
    const listView = listEl.fullCalendar("getView");
    const listWeeks = listView.end.diff(listView.start, "weeks");
    listEl.fullCalendar("destroy");

    expect(calendarWeeks).toBe(6);
    expect(listWeeks).toBe(52);
  });

  it("destroy tears down and re-init restores (view-toggle contract)", () => {
    const { $ } = loadCalendarTwoRuntime();
    window.Perc = { events: {}, range: 6 };
    const el = initCalendar($, "calendar-surface-toggle", {
      events: [{ title: "Toggle Event", start: "2026-11-10" }],
    });
    expect(
      document.querySelectorAll("#calendar-surface-toggle .fc-view").length,
    ).toBe(1);
    // Mirrors the snippet toggle handlers: destroy, then re-initialize.
    el.fullCalendar("destroy");
    expect(
      document.querySelectorAll("#calendar-surface-toggle .fc-view").length,
    ).toBe(0);
    window.Perc.events["calendar-surface-toggle"] = $.Deferred();
    el.fullCalendar({
      defaultView: "month",
      defaultDate: "2026-11-01",
      header: false,
      events: [{ title: "Toggle Event", start: "2026-11-10" }],
    });
    expect(
      document.querySelectorAll("#calendar-surface-toggle .fc-view").length,
    ).toBe(1);
    expect(
      document.querySelectorAll("#calendar-surface-toggle .fc-event").length,
    ).toBe(1);
    el.fullCalendar("destroy");
  });

  it("routes googleCalendarId sources through the gcal companion", () => {
    const { $ } = loadCalendarTwoRuntime();
    window.Perc = { events: {}, range: 6 };
    // No apiKey: the gcal fetcher short-circuits to an empty source instead
    // of issuing JSONP, so this stays hermetic while proving the routing.
    const el = initCalendar($, "calendar-surface-gcal", {
      eventSources: [{ googleCalendarId: "lock@example.com" }],
    });
    const sources = el.fullCalendar("getEventSources");
    expect(sources.length).toBe(1);
    expect(el.fullCalendar("clientEvents").length).toBe(0);
    el.fullCalendar("removeEventSources");
    el.fullCalendar("destroy");
  });

  it("localizes the header through the lang-all companion", () => {
    const { $ } = loadCalendarTwoRuntime();
    window.Perc = { events: {}, range: 6 };
    // The snippet passes perc_calendar_display_language as `lang:`; the
    // lang-all payload registers both the button-text hash and the moment
    // locale the title format consumes.
    expect(typeof $.fullCalendar.lang).toBe("function");
    expect(Object.keys($.fullCalendar.langs).length).toBeGreaterThan(50);
    expect($.fullCalendar.langs.fr).toBeDefined();
    const el = initCalendar($, "calendar-surface-lang", {
      lang: "fr",
      events: [],
    });
    expect(
      document.querySelector("#calendar-surface-lang h2").textContent,
    ).toContain("novembre");
    el.fullCalendar("destroy");
  });
});
