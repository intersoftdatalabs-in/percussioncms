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
 * Developer Problems panel design validation (#4345 / parent #1690).
 *
 * Admin opens Developer → Problems and sees at least one problem row for the
 * known invalid open-editor/session fixture. Navigate-to-source opens the
 * Content Types peer when present. Read-only — no save/write chrome.
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… TEST_DB_TYPE=h2 \
 *     TEST_PRODUCT=cms \
 *     npm run test:surface -- --path tests/developer-problems-panel.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL, adminBasicAuthHeaders } = require("./helpers/auth");
const { catalogRowsSelector } = require("./helpers/developer-catalog-selectors");
const {
  TEST_IDS,
  SAFE_ID_RE,
  INVALID_SESSION_FIXTURE,
  developerProblemsUrl,
  developerProblemsRestUrl,
  unwrapDesignProblems,
  unexpectedConsoleErrors,
} = require("./helpers/developer-problems-surface");

function attachConsoleGuards(page) {
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
  return { pageErrors, consoleErrors };
}

test.describe("Developer Problems panel (#4345 / #1690)", () => {
  test(
    "REST: GET /services/problems returns the invalid-session fixture",
    { tag: ["@developer", "@problems", "@validation"] },
    async ({ request }) => {
      test.setTimeout(30_000);
      const headers = {
        ...adminBasicAuthHeaders(),
        Accept: "application/json",
      };
      const url = developerProblemsRestUrl(BASE_URL);
      const res = await request.get(url, { headers });
      expect(
        res.status(),
        `GET ${url} must be 2xx (Problems REST #4345; was ${res.status()})`,
      ).toBeGreaterThanOrEqual(200);
      expect(res.status(), `GET ${url} must not be error`).toBeLessThan(300);

      const body = await res.json();
      const problems = unwrapDesignProblems(body);
      expect(
        problems.length,
        "session must include the known invalid open-editor fixture",
      ).toBeGreaterThan(0);
      const fixture = problems.find((p) => p.id === INVALID_SESSION_FIXTURE);
      expect(fixture, "invalid-session fixture row").toBeTruthy();
      expect(fixture.id).toMatch(SAFE_ID_RE);
      expect(String(fixture.message || "")).toMatch(/required name/i);
      const raw = JSON.stringify(body);
      expect(raw.includes("jdbc:"), "catalog must not echo JDBC URL").toBe(false);
      expect(raw.includes(".."), "catalog must not echo parent traversal").toBe(false);
    },
  );

  test(
    "REST: unsafe fixture token is 400",
    { tag: ["@developer", "@problems", "@validation"] },
    async ({ request }) => {
      test.setTimeout(30_000);
      const headers = {
        ...adminBasicAuthHeaders(),
        Accept: "application/json",
      };
      const url = `${developerProblemsRestUrl(BASE_URL)}?fixture=..%2Fetc`;
      const res = await request.get(url, { headers });
      expect(res.status(), `GET ${url} must be 400 for unsafe fixture`).toBe(400);
      const text = await res.text();
      expect(text.toLowerCase().includes("jdbc:")).toBe(false);
      expect(text.includes("..")).toBe(false);
    },
  );

  test(
    "Admin sees a problem row and can navigate to source",
    { tag: ["@developer", "@problems", "@validation"] },
    async ({ page }) => {
      test.setTimeout(120_000);
      const { pageErrors, consoleErrors } = attachConsoleGuards(page);

      await loginAsAdmin(page);
      await page.goto(developerProblemsUrl(BASE_URL), {
        waitUntil: "domcontentloaded",
      });

      await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
        timeout: 20_000,
      });
      await expect(page.locator(`[data-testid="${TEST_IDS.tab}"]`)).toBeVisible({
        timeout: 15_000,
      });

      const error = page.locator(`[data-testid="${TEST_IDS.error}"]`);
      const panel = page.locator(`[data-testid="${TEST_IDS.panel}"]`);
      const empty = page.locator(`[data-testid="${TEST_IDS.empty}"]`);

      await expect(panel.or(empty).or(error).first()).toBeVisible({
        timeout: 30_000,
      });

      if (await error.isVisible()) {
        throw new Error(`Problems catalog error: ${(await error.innerText()).trim()}`);
      }
      if (await empty.isVisible()) {
        throw new Error(
          "Problems catalog empty — expected invalid-session fixture row (#4345)",
        );
      }

      await expect(page.locator(`[data-testid="${TEST_IDS.table}"]`)).toBeVisible();
      const rows = page.locator(catalogRowsSelector(TEST_IDS.row));
      await expect(rows.first()).toBeVisible({ timeout: 15_000 });
      await expect(page.locator(`[data-testid="${TEST_IDS.message}"]`).first()).toContainText(
        /required name/i,
      );

      const navigate = page.locator(`[data-testid="${TEST_IDS.navigate}"]`).first();
      await expect(navigate).toBeVisible();
      await expect(navigate).toHaveAttribute("data-prob-navigate", "content-types");
      await navigate.click();

      await expect(page.locator(`[data-testid="${TEST_IDS.contentTypesTab}"]`)).toHaveAttribute(
        "aria-selected",
        "true",
        { timeout: 15_000 },
      );

      await expect(page.locator('[data-testid="developer-prob-save"]')).toHaveCount(0);

      expect(pageErrors, `pageerror: ${pageErrors.join(" | ")}`).toEqual([]);
      expect(
        unexpectedConsoleErrors(consoleErrors),
        `console error: ${unexpectedConsoleErrors(consoleErrors).join(" | ")}`,
      ).toEqual([]);
    },
  );
});
