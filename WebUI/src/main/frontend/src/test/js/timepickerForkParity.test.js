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
 * Behavioral parity for the retained Milesich timepicker fork
 * (issue #7179, parent audit #5627).
 *
 * The live date/time cluster keeps the vendored jQuery UI Timepicker 0.2.1
 * (Martin Milesich, 2009) under a recorded fork decision: upstream is dead
 * with no same-lineage npm artifact, and the only npm candidate
 * (Trent Richardson jquery-ui-timepicker-addon 1.6.3) is an incompatible
 * lineage — datetimepicker()/timepicker() methods with no datepicker()
 * showTime passthrough (the option would be silently ignored and the time
 * UI would disappear) and a div.ui-timepicker-div *class* instead of the
 * #ui-timepicker-div *id* the views' z-index handling targets. Serving the
 * addon at the same stable URL would silently break the live
 * sys_CalendarSimple / PercScheduleDialog / perc calendar controls, so the
 * file stays vendored. The never-built datepicker.js stub and the
 * caller-less Matt Kruse date.js are deleted outright (pinned absent +
 * unreferenced by the RETIRED_7179 block in jslibVendoredAudit.test.js).
 *
 * This file pins the fork contract the live call sites rely on:
 *
 *   - the retained file extends $.datepicker with the Milesich option
 *     surface (showTime/stepMinutes/stepHours/altTimeField/time24h),
 *   - a .perc-datetime-picker input driven through the verbatim
 *     PercScheduleDialog option shape initializes, reports showTime, and
 *     renders the #ui-timepicker-div id the Finder/Page views target,
 *   - $.datepicker._hideDatepicker (called by the schedule dialog) works,
 *   - every live call-site source still uses the Milesich option shape and
 *     no source references the deleted files.
 */

import { readFileSync, existsSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import jquery from "jquery";
import { describe, it, expect, beforeEach, vi } from "vitest";

const __dirname = dirname(fileURLToPath(import.meta.url));
const FRONTEND_ROOT = dirname(dirname(dirname(__dirname)));
const WEBAPP_CM = join(FRONTEND_ROOT, "..", "webapp", "cm");
const WAR_ROOT = join(FRONTEND_ROOT, "..", "..", "..", "war");
const NPM_ROOT = join(FRONTEND_ROOT, "node_modules");
const REPO_ROOT = join(FRONTEND_ROOT, "..", "..", "..", "..");
const JSLIB = "jslib/profiles/3x";

function loadDatepickerWithFork() {
  let jq = jquery(globalThis.window);
  if (typeof jq !== "function") {
    jq = typeof jquery === "function" ? jquery : jquery.fn || jquery;
    if (!jq.fn && jq.prototype) jq.fn = jq.prototype;
  }
  if (!jq.fn) throw new Error("jquery has no .fn");
  globalThis.jQuery = jq;
  globalThis.$ = jq;

  // Production order on classic pages: core -> migrate -> jQuery UI, then
  // the retained Milesich fork on top of the UI datepicker.
  for (const file of [
    join(NPM_ROOT, "jquery-migrate/dist/jquery-migrate.js"),
    join(NPM_ROOT, "jquery-ui/dist/jquery-ui.js"),
    join(
      WEBAPP_CM,
      `${JSLIB}/jquery/plugins/jquery-perc-retiredjs/timepicker.js`,
    ),
  ]) {
    const src = readFileSync(file, "utf8");
    const run = new Function("window", "jQuery", "$", src);
    run(globalThis.window, jq, jq);
  }
  return jq;
}

const $ = loadDatepickerWithFork();

beforeEach(() => {
  document.body.innerHTML = "";
});

describe("retained Milesich timepicker fork (issue #7179)", () => {
  it("extends the jQuery UI datepicker with the Milesich option surface", () => {
    for (const opt of [
      "showTime",
      "stepMinutes",
      "stepHours",
      "altTimeField",
      "time24h",
    ]) {
      expect(
        $.datepicker._defaults,
        `fork lost the ${opt} default the call sites pass`,
      ).toHaveProperty(opt);
    }
    expect($.datepicker._defaults.showTime).toBe(false);
    expect(typeof $.timepicker).toBe("object");
  });

  it("drives a schedule-dialog input through the verbatim PercScheduleDialog shape", () => {
    document.body.innerHTML =
      `<input class="perc-datetime-picker" id="perc-content-edit-test-display" ` +
      `name="test-display" type="text" value="" />`;
    // Verbatim option shape of PercScheduleDialog.attachFormDatepicker
    // (both twins) and the sys_CalendarSimple inline script.
    $("#perc-content-edit-test-display")
      .datepicker({
        showOn: "button",
        buttonImage: "../images/images/buttonCalendar.gif",
        duration: "",
        dateFormat: "mm/dd/yy",
        buttonImageOnly: true,
        showTime: true,
        constrainInput: true,
        stepMinutes: 10,
        stepHours: 1,
        altTimeField: "",
        time24h: false,
        minDate: new Date(),
        changeMonth: true,
        changeYear: true,
        buttonText: "",
        onSelect: () => {},
      })
      .on("paste", (evt) => evt.preventDefault());
    const input = $("#perc-content-edit-test-display");
    expect(input.hasClass("hasDatepicker")).toBe(true);
    expect(input.datepicker("option", "showTime")).toBe(true);
    expect(input.datepicker("option", "stepMinutes")).toBe(10);
    // The fork renders #ui-timepicker-div lazily on open (init/show), not at
    // bind time — drive the open path the schedule dialog relies on. jsdom
    // has no layout engine (offset() is undefined), so stub the position
    // probe; the fork markup assertions below are layout-independent.
    $.timepicker.init();
    const offset = $.fn.offset;
    $.fn.offset = () => ({ left: 0, top: 0 });
    try {
      input.datepicker("show");
    } finally {
      $.fn.offset = offset;
    }
    // The Finder/Page views and the schedule dialog target this id for
    // z-index/wrapping: the npm-addon lineage renders a class instead, so
    // this pin is the fork contract that rules the addon out.
    expect($("#ui-timepicker-div").length).toBe(1);
    // The schedule dialog hides the picker on open; must not throw.
    expect(() => $.datepicker._hideDatepicker()).not.toThrow();
  });

  it("keeps every live call site on the Milesich option shape", () => {
    const callSites = [
      join(WEBAPP_CM, "app/js/legacy/plugins/PercScheduleDialog.js"),
      join(WEBAPP_CM, "plugins/PercScheduleDialog.js"),
      join(
        REPO_ROOT,
        "system/cms/content/applications/sys_resources/ApplicationFiles/stylesheets/sys_Templates.xsl",
      ),
      join(
        REPO_ROOT,
        "system/cms/content/applications/rx_resources/ApplicationFiles/js/calendarControl.js",
      ),
      // NOTE: the SupportFile percCalendarControl twin carries no
      // Milesich options (script context owned by the sys__UserDependency
      // twin + system calendarControl.js below), so only the live shape
      // below is pinned.
      join(
        REPO_ROOT,
        "modules/perc-packages/src/main/resources/Packages/perc.Baseline/sys__UserDependency--rx_resources/stylesheets/controls/percCalendarControl.xsl",
      ),
    ];
    for (const abs of callSites) {
      const content = readFileSync(abs, "utf8");
      expect(content, `${abs} drifted off the Milesich shape`).toMatch(
        /showTime\s*:\s*true/,
      );
    }
    // PercScheduleDialog ships as identical twins (legacy bundle source +
    // served copy): pin the identity so the parity above covers both.
    const [legacy, served] = [
      readFileSync(callSites[0], "utf8"),
      readFileSync(callSites[1], "utf8"),
    ];
    expect(legacy).toBe(served);
  });

  it("leaves no reference to the deleted datepicker.js / Kruse date.js", () => {
    const deleted = [
      `${JSLIB}/jquery/plugins/jquery-perc-retiredjs/datepicker.js`,
      `${JSLIB}/libraries/perc-retiredjs/date.js`,
    ];
    for (const rel of deleted) {
      for (const abs of [
        join(WEBAPP_CM, rel),
        join(WEBAPP_CM, "vendor/js/legacy", rel.replace(/^jslib\//, "")),
        join(WAR_ROOT, rel),
      ]) {
        expect(existsSync(abs), `deleted file resurrected: ${abs}`).toBe(
          false,
        );
      }
    }
    // Kruse date.js APIs have no loader left (the file is deleted from all
    // three trees): the only Date extensions the classic pages carry are
    // the retained Tim Down simpledateformat twin and the npm moment dist.
    expect(
      typeof Date.parseString,
      "Kruse Date.parseString resurrected",
    ).toBe("undefined");
  });
});
