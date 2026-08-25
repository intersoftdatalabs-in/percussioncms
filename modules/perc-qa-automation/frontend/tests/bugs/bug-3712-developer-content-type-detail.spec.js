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
 * Developer → Content Types detail must not crash the Developer shell (#3712 / #2908).
 *
 * Opening content-type rows (including percArchiveList / Archive that previously
 * threw TypeError: (e || []).map is not a function) must keep Developer mounted
 * and show Content Type detail or an in-panel error — never "Unable to load
 * Content Types".
 *
 * Surface-filtered QA mode:
 * <pre>
 *   perc-devctl qa-up
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=…
 *     npm run test:surface -- --path tests/bugs/bug-3712-developer-content-type-detail.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("../helpers/auth");

const CRASH_FIXTURE_LABELS = ["Archive", "percArchiveList"];

function developerContentTypesUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "content-types",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

test.describe("Developer content type detail non-array lists (#3712)", () => {
  test("opening content type rows does not replace Developer with route error", async ({
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
    await page.goto(developerContentTypesUrl(), { waitUntil: "networkidle" });

    await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(
      page.locator('[data-testid="tab-developer-content-types"]'),
    ).toBeVisible({ timeout: 15_000 });

    const panel = page.locator('[data-testid="developer-ct-panel"]');
    const empty = page.locator('[data-testid="developer-ct-empty"]');
    const listError = page.locator('[data-testid="developer-ct-error"]');
    await expect(panel.or(empty).or(listError).first()).toBeVisible({
      timeout: 30_000,
    });

    if (await listError.isVisible()) {
      throw new Error(
        `Developer content types catalog error: ${(await listError.innerText()).trim()}`,
      );
    }
    if (await empty.isVisible()) {
      test.skip(true, "No content types in catalog — cannot exercise Content Type detail");
    }

    await expect(page.locator('[data-testid="developer-ct-table"]')).toBeVisible();

    const openButtons = page.locator(
      '[data-testid="developer-ct-table"] button[aria-label^="Open "]',
    );
    const count = await openButtons.count();
    expect(
      count,
      "content type catalog should have at least one open button",
    ).toBeGreaterThan(0);

    const preferred = [];
    for (let i = 0; i < count; i++) {
      const label = (await openButtons.nth(i).getAttribute("aria-label")) || "";
      const name = label.replace(/^Open\s+/i, "").trim();
      if (CRASH_FIXTURE_LABELS.some((fix) => name.includes(fix))) {
        preferred.push(i);
      }
    }
    const indexes =
      preferred.length > 0 ? preferred.slice(0, 5) : [0, 1, 2, 3].filter((i) => i < count);

    for (const idx of indexes) {
      await openButtons.nth(idx).click();

      const detail = page.locator('[data-testid="developer-ct-detail"]');
      const detailError = page.locator('[data-testid="developer-ct-detail-error"]');
      await expect(detail.or(detailError).first()).toBeVisible({ timeout: 20_000 });

      await expect(page.locator('[data-testid="route-error"]')).toHaveCount(0);
      await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible();
      await expect(page.locator('[data-testid="tab-developer-content-types"]')).toBeVisible();
      await expect(page.getByText("Unable to load Content Types")).toHaveCount(0);
      await expect(page.getByText("Unable to load Developer")).toHaveCount(0);

      if (await detail.isVisible()) {
        await expect(page.locator('[data-testid="developer-ct-fields-table"]')).toBeVisible();
        const gaps = page.locator('[data-testid="developer-ct-gaps"]');
        if (await gaps.isVisible()) {
          const text = (await gaps.innerText()).trim();
          expect(text.length, "designGaps should render readable text").toBeGreaterThan(0);
          expect(text).not.toMatch(/\[object Object\]/);
        }
        await page.locator('[data-testid="developer-ct-back"]').click();
      } else {
        await page.locator('[data-testid="tab-developer-templates"]').click();
        await expect(page.locator('[data-testid="tab-developer-templates"]')).toHaveAttribute(
          "aria-selected",
          "true",
        );
        await page.locator('[data-testid="tab-developer-content-types"]').click();
      }

      await expect(page.locator('[data-testid="developer-ct-table"]')).toBeVisible({
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
