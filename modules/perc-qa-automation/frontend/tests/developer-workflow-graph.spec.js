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
 * Developer → Workflows detail: read-only state/transition graph (#4707 / #1690 slice 32).
 *
 * Opening a stock workflow must show developer-wf-graph with a packaged badge
 * and at least one state node (or the empty-graph copy). Transition writes stay
 * out of this surface.
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… \
 *     npm run test:surface -- --path tests/developer-workflow-graph.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { catalogOpenByExactName } = require("./helpers/developer-catalog-selectors");

const STOCK_WORKFLOWS = ["Default Workflow", "Simple Workflow"];

function developerWorkflowsUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "workflows",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

test.describe("Developer workflow graph browse (#4707)", () => {
  test("workflow detail shows a read-only graph and packaged badge", async ({ page }) => {
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
    await expect(page.locator('[data-testid="tab-developer-workflows"]')).toBeVisible({
      timeout: 15_000,
    });

    const panel = page.locator('[data-testid="developer-wf-panel"]');
    const empty = page.locator('[data-testid="developer-wf-empty"]');
    const listError = page.locator('[data-testid="developer-wf-error"]');
    await expect(panel.or(empty).or(listError).first()).toBeVisible({ timeout: 30_000 });

    if (await listError.isVisible()) {
      throw new Error(
        `Developer workflows catalog error: ${(await listError.innerText()).trim()}`,
      );
    }
    if (await empty.isVisible()) {
      test.skip(true, "No workflows in catalog — cannot exercise graph browse");
    }

    const openButtons = page.locator(
      '[data-testid="developer-wf-table"] button[aria-label^="Open "]',
    );
    const count = await openButtons.count();
    expect(count, "workflow catalog should have at least one open button").toBeGreaterThan(0);

    const names = [];
    for (let i = 0; i < count; i++) {
      const label = (await openButtons.nth(i).getAttribute("aria-label")) || "";
      names.push(label.replace(/^Open\s+/i, "").trim());
    }
    const toOpen = STOCK_WORKFLOWS.filter((n) => names.includes(n));
    const target = toOpen.length > 0 ? toOpen[0] : names[0];

    const openBtn = page.locator(
      catalogOpenByExactName("developer-wf-open", "data-wf-name", target),
    );
    await expect(openBtn).toBeVisible();
    await openBtn.click();

    const detail = page.locator('[data-testid="developer-wf-detail"]');
    const detailError = page.locator('[data-testid="developer-wf-detail-error"]');
    await expect(detail.or(detailError).first()).toBeVisible({ timeout: 20_000 });
    if (await detailError.isVisible()) {
      throw new Error(
        `Workflow detail failed for "${target}": ${(await detailError.innerText()).trim()}`,
      );
    }

    const graph = page.locator('[data-testid="developer-wf-graph"]');
    await expect(graph).toBeVisible();
    const graphError = page.locator('[data-testid="developer-wf-graph-error"]');
    await expect(page.locator('[data-testid="developer-wf-graph-kind"]').or(graphError)).toBeVisible({
      timeout: 20_000,
    });
    if (await graphError.isVisible()) {
      throw new Error(`Workflow graph failed: ${(await graphError.innerText()).trim()}`);
    }

    const kind = (await page.locator('[data-testid="developer-wf-graph-kind"]').innerText()).trim();
    if (STOCK_WORKFLOWS.includes(target)) {
      expect(kind).toMatch(/Packaged workflow/i);
    } else {
      expect(kind.length).toBeGreaterThan(0);
    }

    const node = page.locator('[data-testid="developer-wf-graph-node-0"]');
    const emptyGraph = page.locator('[data-testid="developer-wf-graph-empty"]');
    await expect(node.or(emptyGraph).first()).toBeVisible();
    if (await node.isVisible()) {
      const name = (await node.innerText()).trim();
      expect(name.length, "first graph node should not be blank").toBeGreaterThan(0);
    }

    expect(pageErrors, `pageerror: ${pageErrors.join(" | ")}`).toEqual([]);
    const unexpectedConsole = consoleErrors.filter(
      (t) => !/Download the React DevTools/i.test(t),
    );
    expect(unexpectedConsole, `console error: ${unexpectedConsole.join(" | ")}`).toEqual([]);
  });
});
