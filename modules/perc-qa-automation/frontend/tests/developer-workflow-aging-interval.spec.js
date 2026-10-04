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
 * Developer → Workflows: change one absolute aging interval (#5120 / parent #1690).
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… TEST_DB_TYPE=h2 TEST_PRODUCT=cms \
 *     npm run test:surface -- --path tests/developer-workflow-aging-interval.spec.js
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

function countIntervalPuts(page) {
  let puts = 0;
  page.on("request", (req) => {
    if (req.method() === "PUT" && req.url().includes("/aging-transitions/interval")) {
      puts += 1;
    }
  });
  return () => puts;
}

async function createCustomWorkflowWithAging(request, name, minutes) {
  const headers = jsonHeaders();
  const base = `${BASE_URL}/Rhythmyx/services/workflows`;
  const created = await request.post(base, {
    headers,
    data: { WorkflowCreate: { name, description: "slice 58" } },
  });
  expect(created.status(), await created.text()).toBe(200);
  const aging = await request.post(`${base}/${encodeURIComponent(name)}/aging-transitions`, {
    headers,
    data: {
      WorkflowAgingTransitionWrite: { from: "Draft", to: "Review", intervalMinutes: minutes },
    },
  });
  expect(aging.status(), await aging.text()).toBe(200);
  return base;
}

test.describe("Developer change an aging interval (#5120)", () => {
  test("save shows the new minutes only after success; cancel and bad input do not", async ({
    page,
    request,
  }) => {
    test.setTimeout(180_000);
    const guards = attachConsoleGuards(page);
    const puts = countIntervalPuts(page);
    const name = uniqueWorkflowName("Nightly AgeIv");
    await createCustomWorkflowWithAging(request, name, 15);

    await loginAsAdmin(page);
    await openWorkflowsCatalog(page);
    await page
      .locator(catalogOpenByExactName("developer-wf-open", "data-wf-name", name))
      .click();
    const current = agingEdge(page, "15");
    await expect(current).toBeVisible({ timeout: 30_000 });
    await expect(current).toHaveAttribute("data-interval", "15");
    await expect(page.locator('[data-testid="developer-wf-aging-interval-form"]')).toHaveCount(0);

    await page.locator('[data-testid="developer-wf-aging-change-0"]').click();
    await page.locator('[data-testid="developer-wf-aging-new-minutes"]').fill("30");
    await page.locator('[data-testid="developer-wf-aging-interval-cancel"]').click();
    expect(puts()).toBe(0);
    await expect(page.locator('[data-testid="developer-wf-aging-interval-form"]')).toHaveCount(0);
    await expect(current).toHaveAttribute("data-interval", "15");
    await expect(page.locator('[data-testid="developer-wf-graph-notice"]')).toHaveCount(0);

    await page.locator('[data-testid="developer-wf-aging-change-0"]').click();
    await page.locator('[data-testid="developer-wf-aging-new-minutes"]').fill("0");
    await page.locator('[data-testid="developer-wf-aging-interval-save"]').click();
    await expect(page.locator('[data-testid="developer-wf-graph-error"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-wf-graph-notice"]')).toHaveCount(0);
    expect(puts()).toBe(0);
    await expect(current).toHaveAttribute("data-interval", "15");

    await page.locator('[data-testid="developer-wf-aging-new-minutes"]').fill("15");
    await page.locator('[data-testid="developer-wf-aging-interval-save"]').click();
    await expect(page.locator('[data-testid="developer-wf-graph-error"]')).toBeVisible();
    expect(puts()).toBe(0);
    await expect(current).toContainText("15 minutes");

    await page.locator('[data-testid="developer-wf-aging-new-minutes"]').fill("30");
    await page.locator('[data-testid="developer-wf-aging-interval-save"]').click();
    const updated = agingEdge(page, "30");
    await expect(updated).toBeVisible({ timeout: 30_000 });
    await expect(updated).toContainText("30 minutes");
    await expect(updated).toHaveAttribute("data-interval", "30");
    await expect(updated).toHaveAttribute("data-to", "Review");
    await expect(agingEdge(page, "15")).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-wf-graph-notice"]')).toContainText(
      /Aging interval saved/i,
    );
    expect(puts()).toBe(1);

    await page.locator('[data-testid="developer-wf-back"]').click();
    await expect(page.locator('[data-testid="developer-wf-table"]')).toBeVisible({
      timeout: 30_000,
    });
    await page
      .locator(catalogOpenByExactName("developer-wf-open", "data-wf-name", name))
      .click();
    const reopened = agingEdge(page, "30");
    await expect(reopened).toBeVisible({ timeout: 30_000 });
    await expect(reopened).toHaveAttribute("data-interval", "30");
    guards.assertClean();
  });

  test("HTTP 400, 403, and 409 do not claim the interval changed", async ({ page, request }) => {
    test.setTimeout(180_000);
    const guards = attachConsoleGuards(page);
    const name = uniqueWorkflowName("Nightly AgeErr");
    await createCustomWorkflowWithAging(request, name, 15);

    await page.route("**/aging-transitions/interval**", async (route) => {
      if (route.request().method() !== "PUT") {
        await route.continue();
        return;
      }
      const body = route.request().postData() || "";
      const status = body.includes('"newIntervalMinutes":409')
        ? 409
        : body.includes('"newIntervalMinutes":403')
          ? 403
          : 400;
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
    for (const minutes of ["400", "403", "409"]) {
      await page.locator('[data-testid="developer-wf-aging-change-0"]').click();
      await page.locator('[data-testid="developer-wf-aging-new-minutes"]').fill(minutes);
      await page.locator('[data-testid="developer-wf-aging-interval-save"]').click();
      await expect(page.locator('[data-testid="developer-wf-graph-error"]')).toBeVisible();
      await expect(page.locator('[data-testid="developer-wf-graph-notice"]')).toHaveCount(0);
      await expect(current).toHaveAttribute("data-interval", "15");
      await expect(agingEdge(page, minutes)).toHaveCount(0);
    }
    guards.assertClean();
  });

  test("packaged workflow is not mutated; REST maps 403/404/400/409", async ({ page, request }) => {
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

    const forbidden = await request.put(
      `${base}/${encodeURIComponent(packagedName)}/aging-transitions/interval`,
      {
        headers,
        data: {
          WorkflowAgingIntervalWrite: {
            from: "Draft",
            to: "Review",
            intervalMinutes: 15,
            newIntervalMinutes: 30,
          },
        },
      },
    );
    expect(forbidden.status()).toBe(403);
    const after = await request.get(`${base}/${encodeURIComponent(packagedName)}/graph`, {
      headers,
    });
    expect(after.status()).toBe(200);
    expect(await after.text()).toBe(beforeBody);

    const missing = await request.put(
      `${base}/${encodeURIComponent("No Such Workflow 5120")}/aging-transitions/interval`,
      {
        headers,
        data: {
          WorkflowAgingIntervalWrite: {
            from: "Draft",
            to: "Review",
            intervalMinutes: 15,
            newIntervalMinutes: 30,
          },
        },
      },
    );
    expect(missing.status()).toBe(404);

    const name = uniqueWorkflowName("Nightly AgeDup");
    await createCustomWorkflowWithAging(request, name, 15);
    const second = await request.post(`${base}/${encodeURIComponent(name)}/aging-transitions`, {
      headers,
      data: {
        WorkflowAgingTransitionWrite: { from: "Draft", to: "Review", intervalMinutes: 45 },
      },
    });
    expect(second.status(), await second.text()).toBe(200);

    const nonPositive = await request.put(
      `${base}/${encodeURIComponent(name)}/aging-transitions/interval`,
      {
        headers,
        data: {
          WorkflowAgingIntervalWrite: {
            from: "Draft",
            to: "Review",
            intervalMinutes: 15,
            newIntervalMinutes: 0,
          },
        },
      },
    );
    expect(nonPositive.status()).toBe(400);

    const unchanged = await request.put(
      `${base}/${encodeURIComponent(name)}/aging-transitions/interval`,
      {
        headers,
        data: {
          WorkflowAgingIntervalWrite: {
            from: "Draft",
            to: "Review",
            intervalMinutes: 15,
            newIntervalMinutes: 15,
          },
        },
      },
    );
    expect(unchanged.status()).toBe(400);

    const missingEdge = await request.put(
      `${base}/${encodeURIComponent(name)}/aging-transitions/interval`,
      {
        headers,
        data: {
          WorkflowAgingIntervalWrite: {
            from: "Draft",
            to: "Review",
            intervalMinutes: 99,
            newIntervalMinutes: 30,
          },
        },
      },
    );
    expect(missingEdge.status()).toBe(404);

    const dup = await request.put(
      `${base}/${encodeURIComponent(name)}/aging-transitions/interval`,
      {
        headers,
        data: {
          WorkflowAgingIntervalWrite: {
            from: "Draft",
            to: "Review",
            intervalMinutes: 15,
            newIntervalMinutes: 45,
          },
        },
      },
    );
    expect(dup.status()).toBe(409);
    const still = await request.get(`${base}/${encodeURIComponent(name)}/graph`, { headers });
    const stillBody = await still.text();
    expect(still.status(), stillBody).toBe(200);
    expect(stillBody).toMatch(/"intervalMinutes"\s*:\s*15/);
    expect(stillBody).toMatch(/"intervalMinutes"\s*:\s*45/);

    await loginAsAdmin(page);
    await openWorkflowsCatalog(page);
    await page
      .locator(catalogOpenByExactName("developer-wf-open", "data-wf-name", packagedName))
      .click();
    await expect(page.locator('[data-testid="developer-wf-graph-kind"]')).toContainText(
      /Packaged workflow/i,
      { timeout: 30_000 },
    );
    await expect(page.locator('[data-testid^="developer-wf-aging-change-"]')).toHaveCount(0);
    guards.assertClean();
  });
});
