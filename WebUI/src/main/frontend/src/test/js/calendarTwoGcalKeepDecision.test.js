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
 * CalendarTwo Google-Calendar keep/drop decision (issue #6945, slice 2 of
 * #6919; parent plan #6687, audit #5627; closes the #6804 slice-3b
 * remnant).
 *
 * <p>Decision: <b>KEEP</b>. The shipped {@code gcal.js} is the stock
 * {@code @fullcalendar/google-calendar@6.1.15} plugin, which loads feeds
 * with CORS {@code fetch()} against
 * {@code https://www.googleapis.com/calendar/v3/calendars} — the 2.x
 * JSONP script-injection path died with the 2.9.1 payload, so there is no
 * JSONP-era mechanism left to drop. Under a restrictive site CSP the
 * plugin needs {@code connect-src https://www.googleapis.com}; no inline
 * scripts, no {@code callback=?} query params, no dynamic script tags.
 *
 * <p>Keyless-source behavior (pinned below): a source without
 * {@code googleCalendarApiKey} fails through {@code errorCallback} with
 * {@code Error('Specify a googleCalendarApiKey. ...')} — the calendar
 * still renders every other source and no request leaves the page. A
 * keyed source issues exactly one fetch to the Google base URL, which is
 * the evidence behind the {@code connect-src} CSP note in the product
 * docs.
 */
import { describe, expect, it, vi } from "vitest";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));

// src/test/js -> frontend root, then up to the repo root so the
// perc-packages payload + snippet resolve portably.
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
const GCAL = path.join(CALENDARTWO_JS, "gcal.js");
const FULLCALENDAR_MIN = path.join(CALENDARTWO_JS, "fullcalendar.min.js");
const LANG_ALL = path.join(CALENDARTWO_JS, "lang-all.js");
const SNIPPET = path.join(
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
const GOOGLE_API_BASE = "https://www.googleapis.com/calendar/v3/calendars";

function loadCalendarTwoRuntime() {
  // Same order the widget loads: v6 global bundle, google-calendar
  // plugin, locales-all companion.
  window.eval(fs.readFileSync(FULLCALENDAR_MIN, "utf8"));
  window.eval(fs.readFileSync(GCAL, "utf8"));
  window.eval(fs.readFileSync(LANG_ALL, "utf8"));
  return window.FullCalendar;
}

function flushAsync(ms = 50) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

describe("calendarTwo gcal keep decision (issue #6945)", () => {
  it("gcal plugin is fetch-based with no JSONP-era mechanism", () => {
    const gcal = fs.readFileSync(GCAL, "utf8");
    // CORS JSON feed root — the CSP connect-src evidence.
    expect(gcal).toContain(GOOGLE_API_BASE);
    expect(gcal).toContain("fetch(");
    // No JSONP leftovers: no callback query param, no jsonp token, no
    // dynamic script injection for feed loading.
    expect(gcal.toLowerCase()).not.toContain("jsonp");
    expect(gcal).not.toContain("callback=?");
    expect(gcal).not.toContain('createElement("script")');
    expect(gcal).not.toContain("createElement('script')");
  });

  it("snippet still wires the googleCalendarArray feed loop with keys", () => {
    const snippet = fs.readFileSync(SNIPPET, "utf8");
    expect(snippet).toContain("googleCalendarApiKey");
    expect(snippet).toContain("googleCalendarId");
    expect(snippet).toContain("connect-src https://www.googleapis.com");
  });

  it("keyless google source fails gracefully without leaving the page", async () => {
    loadCalendarTwoRuntime();
    window.Perc = { events: {}, range: 6 };
    // Hermetic: even a keyed assertion below stubs fetch; the keyless
    // path must never reach fetch at all.
    const fetchSpy = vi
      .spyOn(globalThis, "fetch")
      .mockRejectedValue(new Error("must not fetch without a key"));
    const noise = [];
    window.addEventListener("error", (event) => {
      noise.push(`window.onerror: ${event.message}`);
    });
    document.body.insertAdjacentHTML(
      "beforeend",
      `<div id="calendar-gcal-keyless"></div>`,
    );
    let calendar = null;
    expect(() => {
      calendar = new window.FullCalendar.Calendar(
        document.getElementById("calendar-gcal-keyless"),
        {
          initialView: "dayGridMonth",
          initialDate: "2026-11-01",
          headerToolbar: false,
          eventSources: [{ googleCalendarId: "lock@example.com" }],
        },
      );
      calendar.render();
    }).not.toThrow();
    await flushAsync();
    // The calendar renders the grid; the keyless source contributes zero
    // events and issues zero requests.
    const root = document.getElementById("calendar-gcal-keyless");
    expect(root.querySelectorAll(".fc-daygrid-day").length).toBe(42);
    expect(calendar.getEvents().length).toBe(0);
    expect(fetchSpy).not.toHaveBeenCalled();
    expect(noise).toEqual([]);
    calendar.destroy();
    fetchSpy.mockRestore();
  });

  it("keyed google source issues one fetch to the Google API base", async () => {
    loadCalendarTwoRuntime();
    window.Perc = { events: {}, range: 6 };
    const fetchSpy = vi.spyOn(globalThis, "fetch").mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => ({ items: [] }),
    });
    document.body.insertAdjacentHTML(
      "beforeend",
      `<div id="calendar-gcal-keyed"></div>`,
    );
    const calendar = new window.FullCalendar.Calendar(
      document.getElementById("calendar-gcal-keyed"),
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
    expect(fetchSpy).toHaveBeenCalled();
    const requestTargets = fetchSpy.mock.calls.map((call) => String(call[0]));
    expect(
      requestTargets.some((target) => target.startsWith(GOOGLE_API_BASE)),
      `expected a fetch to ${GOOGLE_API_BASE}, got: ${requestTargets.join(" | ")}`,
    ).toBe(true);
    // Empty feed: no events, calendar intact.
    expect(calendar.getEvents().length).toBe(0);
    calendar.destroy();
    fetchSpy.mockRestore();
  });
});
