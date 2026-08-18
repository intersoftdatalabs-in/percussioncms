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
 * Developer → Workflows detail must unwrap Jackson Workflow envelopes (#3562 / #2640).
 *
 * Opening Default Workflow / Simple Workflow must show developer-wf-detail, not
 * "Workflow response missing workflowName". Workflow has no Object ACL section
 * (no GUID on the workflow DTO in this release).
 *
 * Surface-filtered QA mode:
 * <pre>
 *   perc-devctl qa-up
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… \
 *     npm run test:surface -- --path tests/bugs/bug-3562-developer-workflow-detail.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("../helpers/auth");
const { catalogOpenByExactName } = require("../helpers/developer-catalog-selectors");

const STOCK_WORKFLOWS = ["Default Workflow", "Simple Workflow"];

function developerWorkflowsUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "workflows",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

test.describe("Developer workflow detail unwrap (#3562)", () => {
  test("Default and Simple workflow detail load without Object ACL", async ({
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
    await page.goto(developerWorkflowsUrl(), { waitUntil: "networkidle" });

    await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(
      page.locator('[data-testid="tab-developer-workflows"]'),
    ).toBeVisible({ timeout: 15_000 });

    const panel = page.locator('[data-testid="developer-wf-panel"]');
    const empty = page.locator('[data-testid="developer-wf-empty"]');
    const listError = page.locator('[data-testid="developer-wf-error"]');
    await expect(panel.or(empty).or(listError).first()).toBeVisible({
      timeout: 30_000,
    });

    if (await listError.isVisible()) {
      throw new Error(
        `Developer workflows catalog error: ${(await listError.innerText()).trim()}`,
      );
    }
    if (await empty.isVisible()) {
      test.skip(true, "No workflows in catalog — cannot exercise workflow detail");
    }

    await expect(page.locator('[data-testid="developer-wf-table"]')).toBeVisible();

    const openButtons = page.locator(
      '[data-testid="developer-wf-table"] button[aria-label^="Open "]',
    );
    const count = await openButtons.count();
    expect(count, "workflow catalog should have at least one open button").toBeGreaterThan(
      0,
    );

    const names = [];
    for (let i = 0; i < count; i++) {
      const label = (await openButtons.nth(i).getAttribute("aria-label")) || "";
      names.push(label.replace(/^Open\s+/i, "").trim());
    }
    const toOpen = STOCK_WORKFLOWS.filter((n) => names.includes(n));
    const targets = toOpen.length > 0 ? toOpen : names.slice(0, 1);

    for (const name of targets) {
      const openBtn = page.locator(
        catalogOpenByExactName("developer-wf-open", "data-wf-name", name),
      );
      await expect(openBtn).toBeVisible();
      await openBtn.click();

      const detail = page.locator('[data-testid="developer-wf-detail"]');
      const detailError = page.locator('[data-testid="developer-wf-detail-error"]');
      await expect(detail.or(detailError).first()).toBeVisible({ timeout: 20_000 });

      if (await detailError.isVisible()) {
        const msg = (await detailError.innerText()).trim();
        throw new Error(
          `Workflow detail failed for "${name}" (unwrap #3562): ${msg}`,
        );
      }

      await expect(page.locator('[data-testid="developer-wf-detail-title"]')).toBeVisible();
      await expect(page.locator('[data-testid="developer-wf-detail-title"]')).toContainText(
        name,
      );
      await expect(page.getByText(/missing workflowName/i)).toHaveCount(0);
      await expect(page.locator('[data-testid="route-error"]')).toHaveCount(0);
      await expect(page.getByText("Unable to load Developer")).toHaveCount(0);

      // #2640 step 5 negative: Workflow has no Object ACL section.
      await expect(page.locator('[data-testid$="-acl-section"]')).toHaveCount(0);
      await expect(detail.getByText(/Object ACL/i)).toHaveCount(0);

      await page.locator('[data-testid="developer-wf-back"]').click();
      await expect(page.locator('[data-testid="developer-wf-table"]')).toBeVisible({
        timeout: 20_000,
      });
    }

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
