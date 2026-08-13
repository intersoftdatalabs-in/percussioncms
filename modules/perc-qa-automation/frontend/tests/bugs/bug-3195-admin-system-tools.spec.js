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
 * Regression: Admin → System Tools blanks Admin with RouteErrorBoundary (#3195).
 *
 * ToolsSection / Security Audit Log / Consistency Checker must render. A map
 * TypeError or Instant object child must not show "Unable to load Admin".
 *
 * Tags: @admin @system-tools @bug-3195
 *
 * Surface filter:
 *   npm run test:surface -- --path tests/bugs/bug-3195-admin-system-tools.spec.js
 *   npm run test:surface -- --tag bug-3195
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("../helpers/auth");

function adminToolsUrl() {
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=admin&tab=tools&_=${Date.now()}`;
}

function collectPageErrors(page) {
  const errors = [];
  page.on("pageerror", (err) => errors.push(String(err)));
  page.on("console", (msg) => {
    if (msg.type() !== "error") {
      return;
    }
    const text = msg.text();
    if (/Failed to load resource/i.test(text)) {
      return;
    }
    errors.push(text);
  });
  return errors;
}

function assertNoToolsCrash(errors) {
  const crash = errors.filter(
    (t) =>
      /map is not a function/i.test(t) ||
      /Route load\/render failed/i.test(t) ||
      /Unable to load Admin/i.test(t) ||
      /Objects are not valid as a React child/i.test(t),
  );
  expect(crash, crash.join("\n")).toEqual([]);
}

test.describe("Admin System Tools load (#3195)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test(
    "System Tools and Security Audit Log render without RouteErrorBoundary",
    {
      tag: ["@admin", "@system-tools", "@bug-3195"],
    },
    async ({ page }) => {
      const errors = collectPageErrors(page);
      await page.goto(adminToolsUrl(), { waitUntil: "domcontentloaded" });

      await expect(page.getByTestId("perc-spa-app")).toBeVisible({
        timeout: 30_000,
      });
      await expect(page.getByTestId("perc-admin-shell")).toBeVisible({
        timeout: 30_000,
      });

      const toolsTab = page.getByTestId("tab-tools");
      if (await toolsTab.isVisible().catch(() => false)) {
        await toolsTab.click();
      }

      await expect(page.getByTestId("route-error")).toHaveCount(0);
      await expect(page.getByText(/Unable to load Admin/i)).toHaveCount(0);

      await expect(page.getByTestId("perc-tools-section")).toBeVisible({
        timeout: 20_000,
      });
      const auditTab = page.getByTestId("tool-tab-security-audit");
      if (await auditTab.isVisible().catch(() => false)) {
        await auditTab.click();
      }
      await expect(page.getByTestId("perc-security-audit-log")).toBeVisible({
        timeout: 20_000,
      });
      await expect(page.getByTestId("audit-log-table")).toBeVisible();
      assertNoToolsCrash(errors);
    },
  );

  test(
    "Consistency Checker is reachable from System Tools",
    {
      tag: ["@admin", "@system-tools", "@bug-3195"],
    },
    async ({ page }) => {
      const errors = collectPageErrors(page);
      await page.goto(adminToolsUrl(), { waitUntil: "domcontentloaded" });
      await expect(page.getByTestId("perc-admin-shell")).toBeVisible({
        timeout: 30_000,
      });
      const toolsTab = page.getByTestId("tab-tools");
      if (await toolsTab.isVisible().catch(() => false)) {
        await toolsTab.click();
      }
      await expect(page.getByTestId("perc-tools-section")).toBeVisible({
        timeout: 20_000,
      });
      await page.getByTestId("tool-tab-consistency").click();
      await expect(page.getByTestId("perc-consistency-checker")).toBeVisible({
        timeout: 20_000,
      });
      await expect(page.getByTestId("start-check-btn")).toBeVisible();
      await expect(page.getByTestId("route-error")).toHaveCount(0);
      assertNoToolsCrash(errors);
    },
  );
});
