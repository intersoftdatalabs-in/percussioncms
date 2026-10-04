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
 * Developer → Workflows: delete one absolute aging transition (#5121 / parent #1690).
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… TEST_DB_TYPE=h2 TEST_PRODUCT=cms \
 *     npm run test:surface -- --path tests/developer-workflow-aging-delete.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const {
  loginAsAdmin,
  BASE_URL,
  adminBasicAuthHeaders,
} = require("./helpers/auth");
const { catalogOpenByExactName } = require("./helpers/developer-catalog-selectors");

function developerWorkflowsUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "workflows",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

function uniqueWorkflowName(prefix) {
  return `${prefix} ${Date.now()}`.slice(0, 50);
}

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
  return {
    assertClean() {
      expect(pageErrors, `pageerror: ${pageErrors.join(" | ")}`).toEqual([]);
      const unexpectedConsole = consoleErrors.filter(
        (t) => !/Failed to load resource/i.test(t) && !/favicon/i.test(t),
      );
      expect(unexpectedConsole, `console error: ${unexpectedConsole.join(" | ")}`).toEqual([]);
    },
  };
}

async function openWorkflowsCatalog(page) {
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
}

function jsonHeaders() {
  return {
    ...adminBasicAuthHeaders(),
    Accept: "application/json",
    "Content-Type": "application/json",
  };
}

function agingEdge(page, minutes) {
  return page.locator('[data-testid^="developer-wf-aging-edge-"]').filter({
    hasText: `Aging ${minutes}`,
  });
}

function countDeletes(page) {
  let aging = 0;
  let regular = 0;
  page.on("request", (req) => {
    if (req.method() !== "DELETE") {
      return;
    }
    const url = req.url();
    if (url.includes("/aging-transitions")) {
      aging += 1;
    } else if (url.includes("/transitions")) {
      regular += 1;
    }
  });
  return () => ({ aging, regular });
}

async function createCustomWorkflowWithAging(request, name, minutes) {
  const headers = jsonHeaders();
  const base = `${BASE_URL}/Rhythmyx/services/workflows`;
  const created = await request.post(base, {
    headers,
    data: { WorkflowCreate: { name, description: "slice 59" } },
  });
  expect(created.status(), await created.text()).toBe(200);
  const regular = await request.post(`${base}/${encodeURIComponent(name)}/transitions`, {
    headers,
    data: { WorkflowTransitionWrite: { from: "Draft", to: "Review", label: "SendQA" } },
  });
  expect(regular.status(), await regular.text()).toBe(200);
  const aging = await request.post(`${base}/${encodeURIComponent(name)}/aging-transitions`, {
    headers,
    data: {
      WorkflowAgingTransitionWrite: { from: "Draft", to: "Review", intervalMinutes: minutes },
    },
  });
  expect(aging.status(), await aging.text()).toBe(200);
  return base;
}

test.describe("Developer delete an aging transition (#5121)", () => {
  test("confirm removes the aging row only after success; cancel does not call the server", async ({
    page,
    request,
  }) => {
    test.setTimeout(180_000);
    const guards = attachConsoleGuards(page);
    const deletes = countDeletes(page);
    const name = uniqueWorkflowName("Nightly AgeDel");
    await createCustomWorkflowWithAging(request, name, 15);

    await loginAsAdmin(page);
    await openWorkflowsCatalog(page);
    await page
      .locator(catalogOpenByExactName("developer-wf-open", "data-wf-name", name))
      .click();
    const current = agingEdge(page, "15");
    await expect(current).toBeVisible({ timeout: 30_000 });
    await expect(page.locator('[data-testid="developer-wf-graph-edges"]')).toContainText("SendQA");
    await expect(page.locator('[data-testid="developer-catalog-confirm-dialog"]')).toHaveCount(0);

    await page.locator('[data-testid="developer-wf-aging-delete-0"]').click();
    await expect(page.locator('[data-testid="developer-catalog-confirm-body"]')).toContainText(
      "15 minutes",
    );
    await page.locator('[data-testid="developer-catalog-confirm-cancel"]').click();
    expect(deletes()).toEqual({ aging: 0, regular: 0 });
    await expect(page.locator('[data-testid="developer-catalog-confirm-dialog"]')).toHaveCount(0);
    await expect(current).toHaveAttribute("data-interval", "15");
    await expect(page.locator('[data-testid="developer-wf-graph-notice"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-wf-graph-edges"]')).toContainText("SendQA");

    await page.locator('[data-testid="developer-wf-aging-delete-0"]').click();
    await page.locator('[data-testid="developer-catalog-confirm-submit"]').click();
    await expect(agingEdge(page, "15")).toHaveCount(0, { timeout: 30_000 });
    await expect(page.locator('[data-testid="developer-wf-graph-notice"]')).toContainText(
      /Aging transition deleted/i,
    );
    await expect(page.locator('[data-testid="developer-wf-graph-edges"]')).toContainText("SendQA");
    expect(deletes()).toEqual({ aging: 1, regular: 0 });

    await page.locator('[data-testid="developer-wf-back"]').click();
    await expect(page.locator('[data-testid="developer-wf-table"]')).toBeVisible({
      timeout: 30_000,
    });
    await page
      .locator(catalogOpenByExactName("developer-wf-open", "data-wf-name", name))
      .click();
    await expect(agingEdge(page, "15")).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-wf-graph-edges"]')).toContainText("SendQA");
    guards.assertClean();
  });

  test("HTTP 400, 403, and 409 do not claim the aging transition was deleted", async ({
    page,
    request,
  }) => {
    test.setTimeout(180_000);
    const guards = attachConsoleGuards(page);
    const name = uniqueWorkflowName("Nightly AgeDelErr");
    await createCustomWorkflowWithAging(request, name, 15);
    const statuses = [400, 403, 409];
    let calls = 0;

    await page.route("**/aging-transitions**", async (route) => {
      const req = route.request();
      if (req.method() !== "DELETE" || req.url().includes("/interval")) {
        await route.continue();
        return;
      }
      const status = statuses[calls] ?? 400;
      calls += 1;
      await route.fulfill({
        status,
        contentType: "application/json",
        body: JSON.stringify({ error: "rejected" }),
      });
    });

    await loginAsAdmin(page);
    await openWorkflowsCatalog(page);
    await page
      .locator(catalogOpenByExactName("developer-wf-open", "data-wf-name", name))
      .click();
    const current = agingEdge(page, "15");
    await expect(current).toBeVisible({ timeout: 30_000 });
    for (const status of statuses) {
      await page.locator('[data-testid="developer-wf-aging-delete-0"]').click();
      await page.locator('[data-testid="developer-catalog-confirm-submit"]').click();
      await expect(page.locator('[data-testid="developer-wf-graph-error"]')).toBeVisible();
      await expect(page.locator('[data-testid="developer-wf-graph-notice"]')).toHaveCount(0);
      await expect(current).toHaveAttribute("data-interval", "15");
      await expect(page.locator('[data-testid="developer-wf-graph-edges"]')).toContainText(
        "SendQA",
      );
      expect(status).toBeGreaterThan(0);
    }
    expect(calls).toBe(3);
    guards.assertClean();
  });

  test("packaged workflow is not mutated; REST maps 403/404/400 and leaves regular transitions", async ({
    page,
    request,
  }) => {
    test.setTimeout(180_000);
    const guards = attachConsoleGuards(page);
    const headers = jsonHeaders();
    const base = `${BASE_URL}/Rhythmyx/services/workflows`;
    const packagedName = "Default Workflow";
    const before = await request.get(`${base}/${encodeURIComponent(packagedName)}/graph`, {
      headers,
    });
    const beforeBody = await before.text();
    expect(before.status(), beforeBody).toBe(200);

    const forbidden = await request.delete(
      `${base}/${encodeURIComponent(packagedName)}/aging-transitions?from=Draft&to=Review&intervalMinutes=15`,
      { headers },
    );
    expect(forbidden.status()).toBe(403);
    const after = await request.get(`${base}/${encodeURIComponent(packagedName)}/graph`, {
      headers,
    });
    expect(after.status()).toBe(200);
    expect(await after.text()).toBe(beforeBody);

    const missing = await request.delete(
      `${base}/${encodeURIComponent("No Such Workflow 5121")}/aging-transitions?from=Draft&to=Review&intervalMinutes=15`,
      { headers },
    );
    expect(missing.status()).toBe(404);

    const name = uniqueWorkflowName("Nightly AgeDelRest");
    await createCustomWorkflowWithAging(request, name, 15);
    const nonPositive = await request.delete(
      `${base}/${encodeURIComponent(name)}/aging-transitions?from=Draft&to=Review&intervalMinutes=0`,
      { headers },
    );
    expect(nonPositive.status()).toBe(400);
    const missingEdge = await request.delete(
      `${base}/${encodeURIComponent(name)}/aging-transitions?from=Draft&to=Review&intervalMinutes=99`,
      { headers },
    );
    expect(missingEdge.status()).toBe(404);

    const removed = await request.delete(
      `${base}/${encodeURIComponent(name)}/aging-transitions?from=Draft&to=Review&intervalMinutes=15`,
      { headers },
    );
    const removedBody = await removed.text();
    expect(removed.status(), removedBody).toBe(200);
    expect(removedBody).toContain("SendQA");
    expect(removedBody).not.toMatch(/"intervalMinutes"\s*:\s*15/);

    const still = await request.get(`${base}/${encodeURIComponent(name)}/graph`, { headers });
    const stillBody = await still.text();
    expect(still.status(), stillBody).toBe(200);
    expect(stillBody).toContain("SendQA");
    expect(stillBody).not.toMatch(/"intervalMinutes"\s*:\s*15/);

    await loginAsAdmin(page);
    await openWorkflowsCatalog(page);
    await page
      .locator(catalogOpenByExactName("developer-wf-open", "data-wf-name", packagedName))
      .click();
    await expect(page.locator('[data-testid="developer-wf-graph-kind"]')).toContainText(
      /Packaged workflow/i,
      { timeout: 30_000 },
    );
    await expect(page.locator('[data-testid^="developer-wf-aging-delete-"]')).toHaveCount(0);
    guards.assertClean();
  });
});
