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
 * Developer → Slots detail must not crash the Developer shell (#3554 / #2908).
 *
 * Opening slot rows (including FastForward names that previously threw
 * TypeError: (e || []).map is not a function) must keep Developer mounted
 * and show Slot detail or an in-panel error — never "Unable to load Developer".
 *
 * Surface-filtered QA mode:
 * <pre>
 *   perc-devctl qa-up
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… \
 *     npm run test:surface -- --path tests/bugs/bug-3554-developer-slot-detail.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("../helpers/auth");

const CRASH_FIXTURE_LABELS = [
  "All Press Releases 2007",
  "All Press Releases 2008",
  "Auto Index",
  "Calendar Events",
  "Contacts",
];

function developerSlotsUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "slots",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

test.describe("Developer slot detail non-array lists (#3554)", () => {
  test("opening slot rows does not replace Developer with route error", async ({
    page,
  }) => {
    test.setTimeout(120_000);
    const pageErrors = [];
    const consoleErrors = [];
    page.on("pageerror", (err) => {
      pageErrors.push(String(err && err.message ? err.message : err));
    });
    page.on("console", (msg) => {
      if (msg.type() === "error") {
        consoleErrors.push(msg.text());
      }
    });

    await loginAsAdmin(page);
    await page.goto(developerSlotsUrl(), { waitUntil: "networkidle" });

    await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(
      page.locator('[data-testid="tab-developer-slots"]'),
    ).toBeVisible({ timeout: 15_000 });

    const panel = page.locator('[data-testid="developer-slot-panel"]');
    const empty = page.locator('[data-testid="developer-slot-empty"]');
    const listError = page.locator('[data-testid="developer-slot-error"]');
    await expect(panel.or(empty).or(listError).first()).toBeVisible({
      timeout: 30_000,
    });

    if (await listError.isVisible()) {
      throw new Error(
        `Developer slots catalog error: ${(await listError.innerText()).trim()}`,
      );
    }
    if (await empty.isVisible()) {
      test.skip(true, "No slots in catalog — cannot exercise Slot detail");
    }

    await expect(page.locator('[data-testid="developer-slot-table"]')).toBeVisible();

    const openButtons = page.locator(
      '[data-testid="developer-slot-table"] button[aria-label^="Open "]',
    );
    const count = await openButtons.count();
    expect(count, "slot catalog should have at least one open button").toBeGreaterThan(
      0,
    );

    const preferred = [];
    for (let i = 0; i < count; i++) {
      const label = (await openButtons.nth(i).getAttribute("aria-label")) || "";
      const name = label.replace(/^Open\s+/i, "").trim();
      if (CRASH_FIXTURE_LABELS.some((fix) => name.includes(fix))) {
        preferred.push(i);
      }
    }
    const indexes = preferred.length > 0 ? preferred.slice(0, 5) : [0, 1, 2, 3].filter((i) => i < count);

    for (const idx of indexes) {
      await openButtons.nth(idx).click();

      const detail = page.locator('[data-testid="developer-slot-detail"]');
      const detailError = page.locator('[data-testid="developer-slot-detail-error"]');
      await expect(detail.or(detailError).first()).toBeVisible({ timeout: 20_000 });

      await expect(page.locator('[data-testid="route-error"]')).toHaveCount(0);
      await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible();
      await expect(page.locator('[data-testid="tab-developer-slots"]')).toBeVisible();
      await expect(page.getByText("Unable to load Developer")).toHaveCount(0);

      if (await detail.isVisible()) {
        await expect(page.locator('[data-testid="developer-slot-associations"]')).toBeVisible();
        const gaps = page.locator('[data-testid="developer-slot-gaps"]');
        if (await gaps.isVisible()) {
          const text = (await gaps.innerText()).trim();
          expect(text.length, "designGaps should render readable text").toBeGreaterThan(0);
          expect(text).not.toMatch(/\[object Object\]/);
        }
        await page.locator('[data-testid="developer-slot-back"]').click();
      } else {
        // In-panel / section boundary: remount Slots via another tab.
        await page.locator('[data-testid="tab-developer-templates"]').click();
        await expect(page.locator('[data-testid="tab-developer-templates"]')).toHaveAttribute(
          "aria-selected",
          "true",
        );
        await page.locator('[data-testid="tab-developer-slots"]').click();
      }

      await expect(page.locator('[data-testid="developer-slot-table"]')).toBeVisible({
        timeout: 20_000,
      });
    }

    const mapErrors = pageErrors.filter((m) => /\.map is not a function/i.test(m));
    expect(mapErrors, `pageerror .map: ${mapErrors.join(" | ")}`).toEqual([]);
    expect(pageErrors, `pageerror: ${pageErrors.join(" | ")}`).toEqual([]);
    const unexpectedConsole = consoleErrors.filter(
      (t) => !/Download the React DevTools/i.test(t),
    );
    expect(
      unexpectedConsole,
      `console error: ${unexpectedConsole.join(" | ")}`,
    ).toEqual([]);
  });
});
