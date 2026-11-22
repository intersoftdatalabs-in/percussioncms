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
 * CalendarTwo list-view + view-toggle parity pins (issue #6945, slice 2
 * of #6919; parent plan #6687, audit #5627).
 *
 * <p>Slice 1 (#6944) ported the month path to stock v6 and left the toggle
 * handlers + list renderer functionally ported but unpolished: layout
 * switches animated through {@code fadeIn()} (non-deterministic for
 * screenshot-diff captures), the toggle state lived only in pixels, and
 * the list region rendered nothing while the DTS fetch was in flight and
 * nothing at all on an empty result. Slice 2 hardens the contract in
 * {@code percCalendarTwoSnippet.vm}:
 *
 * <ul>
 *   <li>Deterministic switching: every layout change uses
 *       {@code show()}/{@code hide()} — no {@code fadeIn()} in snippet
 *       code (comment mentions are exempt).</li>
 *   <li>Toggle affordances: the Calendar/List buttons carry
 *       {@code aria-pressed} mirroring the visible layout, grouped under
 *       {@code role="group"}; the 2.x theme classes stay so the v6 toggle
 *       compat rule keeps their shape.</li>
 *   <li>List states: a {@code perc-event-list-loading} row covers the DTS
 *       fetch, and a {@code perc-event-list-empty} row covers an empty
 *       result; the list region is {@code aria-live="polite"}.</li>
 *   <li>Unchanged mechanics: {@code destroy()}+re-init toggle contract,
 *       the 52-week list window (364 days), and the date comparator.</li>
 * </ul>
 */
import { describe, expect, it } from "vitest";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));

// src/test/js -> frontend root, then up to the repo root so the snippet
// resolves portably.
const FRONTEND_ROOT = path.dirname(path.dirname(path.dirname(__dirname)));
const REPO_ROOT = path.join(FRONTEND_ROOT, "..", "..", "..", "..");

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

describe("calendarTwo list-view + view-toggle parity (issue #6945)", () => {
  it("toggle buttons keep the v6 compat classes and carry aria-pressed", () => {
    const snippet = fs.readFileSync(SNIPPET, "utf8");
    for (const id of ["showCalendarButton", "showListButton"]) {
      expect(snippet).toContain(`id="${id}-`);
    }
    // 2.x theme classes stay: the snippet toggle-compat rule retargets
    // them since v6 never styles these classes.
    expect(snippet).toContain("perc-type-toggle");
    expect(snippet).toContain("fc-state-default");
    // Affordance: pressed state mirrors the visible layout (initial +
    // both handlers).
    expect(snippet).toContain('role="group"');
    // The toggle group accessible name is preference-driven (#7030,
    // residual of #6992): the group renders
    // $perc_layout_toggle_group_label (HTML-escaped) with a Velocity
    // fallback guard preserving the English default for pre-existing
    // widget instances.
    expect(snippet).toContain(
      'aria-label="$tools.esc.html($perc_layout_toggle_group_label)"',
    );
    expect(snippet).toContain(
      "#if(!$perc_layout_toggle_group_label)#set($perc_layout_toggle_group_label = 'Calendar layout')#end",
    );
    expect(snippet).toContain("aria-pressed=\"true\"");
    expect(snippet).toContain("aria-pressed=\"false\"");
    expect(snippet).toContain(".attr('aria-pressed', 'true')");
    expect(snippet).toContain(".attr('aria-pressed', 'false')");
  });

  it("layout switching is deterministic (no fadeIn in snippet code)", () => {
    const snippet = fs.readFileSync(SNIPPET, "utf8");
    const fadeLines = snippet
      .split("\n")
      .filter((line) => line.includes("fadeIn("));
    // Every remaining mention must be a comment explaining the removal —
    // no fadeIn() call may drive layout switching (vacuous when the
    // snippet stops mentioning fadeIn at all).
    for (const line of fadeLines) {
      const trimmed = line.trim();
      expect(
        trimmed.startsWith("##") || trimmed.startsWith("//"),
        `non-comment fadeIn: ${line}`,
      ).toBe(true);
    }
  });

  it("toggle handlers destroy + re-init the v6 instance", () => {
    const snippet = fs.readFileSync(SNIPPET, "utf8");
    expect(snippet).toContain("percCalendarInstance.destroy()");
    expect(snippet).toContain("initializeCalendar()");
    expect(snippet).toContain("percInitListView()");
  });

  it("list region has loading + empty states and announces updates", () => {
    const snippet = fs.readFileSync(SNIPPET, "utf8");
    expect(snippet).toContain("perc-event-list-loading");
    expect(snippet).toContain("perc-event-list-empty");
    expect(snippet).toContain("No upcoming events.");
    expect(snippet).toContain('aria-live="polite"');
  });

  it("list mechanics stay pinned (52-week window, date comparator)", () => {
    const snippet = fs.readFileSync(SNIPPET, "utf8");
    expect(snippet).toContain("Perc.range = 52;");
    expect(snippet).toContain("Perc.range = 6;");
    expect(snippet).toContain("setDate(listEnd.getDate() + 364)");
    expect(snippet).toContain("return new Date(x.start) - new Date(y.start);");
  });
});
