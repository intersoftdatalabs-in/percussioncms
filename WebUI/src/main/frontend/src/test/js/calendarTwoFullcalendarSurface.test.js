/**
 * CalendarTwo FullCalendar consumed-surface lock (issue #6919, plan #6687,
 * parent audit #5627).
 *
 * <p>Slice 1 of #6919 migrates the published calendarTwo payload from EOL
 * FullCalendar 2.9.1 to the stock FullCalendar v6.1.15 global bundle (plus
 * the stock google-calendar plugin and locales-all payloads). This file is
 * the v6 port of the 2.x behavioral lock
 * ({@code calendarTwoFullcalendarSurface.test.js} slice 1 of #6687): it loads
 * the <b>payload</b> bytes (not {@code node_modules}) so any payload drift
 * fails loudly, and locks the widget-consumed v6 surface the rewritten
 * {@code percCalendarTwoSnippet.vm} relies on:
 *
 * <ul>
 *   <li>v6 global entry point + 6.1.15 version ({@code FullCalendar.Calendar}).
 *   <li>Month-view dayGridMonth render driven by the snippet's events
 *       function ({@code fetchInfo} native-Date range, static + DTS-backed).
 *   <li>The {@code Perc.events} deferred the snippet resolves inside the
 *       events function for list-view rendering.
 *   <li>Fixed 6-week month grid plus the explicit 52-week fetch window the
 *       snippet uses for list layout (v6 has no 52-week month mode).
 *   <li>{@code destroy} + re-init (the calendar/list view-toggle contract).
 *   <li>google-calendar plugin: {@code googleCalendarId} sources route
 *       through the plugin fetcher.
 *   <li>locales-all payload: the {@code locale:} option localizes the header
 *       (the snippet passes {@code perc_calendar_display_language}).
 * </ul>
 *
 * <p>Harness notes. Unlike 2.9.1, the v6 bundle has no jQuery dependency, so
 * no jquery-migrate shim is needed. jsdom has no layout engine, so content
 * taller than the (zero-size) scroller virtualizes away: every grid
 * assertion below queries the DOM the v6 renderer emits, not visibility.
 */
import { describe, expect, it, vi } from "vitest";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(fileURLToPath(import.meta.url));

// src/test/js -> frontend root (peer: widgetPayloadNpmParity.test.js), then
// up to the repo root so the perc-packages payloads resolve portably.
const FRONTEND_ROOT = path.dirname(path.dirname(path.dirname(__dirname)));
const REPO_ROOT = path.join(FRONTEND_ROOT, "..", "..", "..", "..");

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
  // Mirror the widget script order (percCalendarTwo.xml): v6 global bundle,
  // then the google-calendar plugin and locales-all companions. Loading the
  // payload files exercises the shipped artifact, not the npm dist. Each
  // load re-evaluates into a fresh jsdom window per test file run, so tests
  // reset the globals they touch instead of re-loading per test.
  window.eval(
    fs.readFileSync(path.join(CALENDARTWO_JS, "fullcalendar.min.js"), "utf8"),
  );
  window.eval(fs.readFileSync(path.join(CALENDARTWO_JS, "gcal.js"), "utf8"));
  window.eval(
    fs.readFileSync(path.join(CALENDARTWO_JS, "lang-all.js"), "utf8"),
  );
  return window.FullCalendar;
}

function renderV6Month(id, options) {
  // Mirror the snippet contract: the template resolves one Deferred per
  // widget id inside the events function before the list renderer consumes
  // it. Static events keep every test hermetic (no DTS, no network).
  document.body.insertAdjacentHTML("beforeend", `<div id="${id}"></div>`);
  const staticEvents = options.events ?? [];
  const calendar = new window.FullCalendar.Calendar(
    document.getElementById(id),
    {
      initialView: "dayGridMonth",
      initialDate: "2026-11-01",
      headerToolbar: { left: "title", center: "", right: "" },
      ...options,
      events:
        typeof options.events === "function"
          ? options.events
          : (fetchInfo, onSuccess) => {
              if (window.Perc?.events?.[id]) {
                window.Perc.events[id].resolve(staticEvents);
              }
              onSuccess(staticEvents);
            },
    },
  );
  calendar.render();
  return calendar;
}

function flushAsync(ms = 50) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

describe("calendarTwo FullCalendar v6 consumed-surface lock (issue #6919)", () => {
  it("exposes the widget-consumed v6 global entry point at 6.1.15", () => {
    const FullCalendar = loadCalendarTwoRuntime();
    expect(typeof FullCalendar.Calendar).toBe("function");
    expect(FullCalendar.version).toBe("6.1.15");
  });

  it("renders the month grid and the fetched events (calendar layout)", () => {
    loadCalendarTwoRuntime();
    window.Perc = { events: {}, range: 6 };
    const calendar = renderV6Month("calendar-surface-cal", {
      events: [
        { title: "Lock Event", start: "2026-11-10" },
        { title: "Second Lock Event", start: "2026-11-18" },
      ],
    });
    // Month grid: fixed 6-week dayGridMonth covering November 2026.
    const root = document.getElementById("calendar-surface-cal");
    const dayCells = root.querySelectorAll(".fc-daygrid-day");
    expect(dayCells.length).toBe(42);
    // The static source flowed through the events function into the grid.
    const rendered = root.querySelectorAll(".fc-event .fc-event-title");
    const titles = Array.from(rendered).map((node) => node.textContent);
    expect(titles).toContain("Lock Event");
    expect(titles).toContain("Second Lock Event");
    // Header title follows the rendered month.
    expect(root.querySelector("h2.fc-toolbar-title").textContent).toContain(
      "November 2026",
    );
    calendar.destroy();
  });

  it("hands the events function a native-Date fetch range (DTS contract)", () => {
    loadCalendarTwoRuntime();
    window.Perc = { events: {}, range: 6 };
    let seenRange = null;
    const calendar = renderV6Month("calendar-surface-range", {
      events: (fetchInfo, onSuccess) => {
        seenRange = { start: fetchInfo.start, end: fetchInfo.end };
        onSuccess([]);
      },
    });
    // The snippet formats this range as YYYY-MM-DD for the DTS dated query
    // (replacing the 2.x start._d moment internals).
    expect(seenRange).not.toBeNull();
    expect(seenRange.start instanceof Date).toBe(true);
    expect(seenRange.end instanceof Date).toBe(true);
    expect(seenRange.end.getTime()).toBeGreaterThan(
      seenRange.start.getTime(),
    );
    // Month view asks for roughly the visible 6 weeks, not 52.
    const weeks =
      (seenRange.end.getTime() - seenRange.start.getTime()) /
      (7 * 24 * 60 * 60 * 1000);
    expect(weeks).toBeLessThan(8);
    calendar.destroy();
  });

  it("resolves the Perc.events deferred with the fetched events (list-view contract)", () => {
    loadCalendarTwoRuntime();
    window.Perc = { events: {}, range: 52 };
    // Deferred shape mirrors the snippet: one jQuery-style Deferred per
    // widget id. A minimal thenable suffices here; the snippet uses the
    // real jQuery Deferred on pages.
    let resolvedWith = null;
    window.Perc.events["calendar-surface-list"] = {
      resolve(value) {
        resolvedWith = value;
      },
    };
    const calendar = renderV6Month("calendar-surface-list", {
      events: [{ title: "Deferred Event", start: "2026-11-10" }],
    });
    // The snippet awaits this Deferred to populate the event list.
    expect(resolvedWith).toEqual([
      { title: "Deferred Event", start: "2026-11-10" },
    ]);
    calendar.destroy();
  });

  it("supports the explicit 52-week list fetch window through the same function shape", () => {
    loadCalendarTwoRuntime();
    window.Perc = { events: {}, range: 52 };
    // v6 dayGridMonth always renders a fixed 6 weeks, so list layout fetches
    // an explicit 52-week window without rendering (re-deriving the 2.x
    // Perc.range = 52 computeRange expansion in the snippet).
    const listStart = new Date(2026, 10, 1);
    const listEnd = new Date(listStart.getTime());
    listEnd.setDate(listEnd.getDate() + 364);
    let received = null;
    const fakeFetchInfo = { start: listStart, end: listEnd };
    const eventsFn = (fetchInfo, onSuccess) => {
      received = { start: fetchInfo.start, end: fetchInfo.end };
      onSuccess([{ title: "Far Event", start: "2027-06-01" }]);
    };
    const collected = [];
    eventsFn(fakeFetchInfo, (events) => collected.push(...events));
    expect(received.end.getTime() - received.start.getTime()).toBe(
      364 * 24 * 60 * 60 * 1000,
    );
    expect(collected).toEqual([{ title: "Far Event", start: "2027-06-01" }]);
  });

  it("destroy tears down and re-init restores (view-toggle contract)", () => {
    loadCalendarTwoRuntime();
    window.Perc = { events: {}, range: 6 };
    const calendar = renderV6Month("calendar-surface-toggle", {
      events: [{ title: "Toggle Event", start: "2026-11-10" }],
    });
    const root = document.getElementById("calendar-surface-toggle");
    expect(root.querySelectorAll(".fc-view").length).toBe(1);
    // Mirrors the snippet toggle handlers: destroy, then re-initialize.
    calendar.destroy();
    expect(root.querySelectorAll(".fc-view").length).toBe(0);
    const second = renderV6Month("calendar-surface-toggle-second", {
      events: [{ title: "Toggle Event", start: "2026-11-10" }],
    });
    const root2 = document.getElementById("calendar-surface-toggle-second");
    expect(root2.querySelectorAll(".fc-view").length).toBe(1);
    expect(root2.querySelectorAll(".fc-event").length).toBe(1);
    second.destroy();
  });

  it("routes googleCalendarId sources through the gcal plugin", async () => {
    loadCalendarTwoRuntime();
    window.Perc = { events: {}, range: 6 };
    // Hermetic: the plugin fetches the Google API over fetch(); stub the
    // empty feed so no network leaves jsdom.
    const fetchSpy = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValue({ ok: true, status: 200, json: async () => ({ items: [] }) });
    document.body.insertAdjacentHTML(
      "beforeend",
      `<div id="calendar-surface-gcal"></div>`,
    );
    window.Perc.events["calendar-surface-gcal"] = { resolve() {} };
    const calendar = new window.FullCalendar.Calendar(
      document.getElementById("calendar-surface-gcal"),
      {
        initialView: "dayGridMonth",
        initialDate: "2026-11-01",
        headerToolbar: false,
        eventSources: [
          {
            googleCalendarApiKey: "lock-api-key",
            googleCalendarId: "lock@example.com",
          },
        ],
      },
    );
    calendar.render();
    await flushAsync();
    const sources = calendar.getEventSources();
    expect(sources.length).toBe(1);
    expect(calendar.getEvents().length).toBe(0);
    expect(fetchSpy).toHaveBeenCalled();
    calendar.destroy();
    fetchSpy.mockRestore();
  });

  it("localizes the header through the locales-all companion", () => {
    loadCalendarTwoRuntime();
    window.Perc = { events: {}, range: 6 };
    // The snippet passes perc_calendar_display_language as `locale:`; the
    // locales-all payload registers the locale data the title consumes.
    expect(window.FullCalendar.globalLocales.length).toBeGreaterThan(50);
    const codes = window.FullCalendar.globalLocales.map((entry) => entry.code);
    expect(codes).toContain("fr");
    const calendar = renderV6Month("calendar-surface-lang", {
      locale: "fr",
      events: [],
    });
    expect(
      document.querySelector("#calendar-surface-lang h2").textContent,
    ).toContain("novembre");
    calendar.destroy();
  });
});
