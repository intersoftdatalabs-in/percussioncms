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
 * Developer → Workflows: add one repeated aging transition (#5242 / parent #1690).
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… TEST_DB_TYPE=h2 TEST_PRODUCT=cms \
 *     npm run test:surface -- --path tests/developer-workflow-repeated-aging.spec.js
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

function repeatedEdge(page) {
  return page.locator('[data-testid^="developer-wf-aging-edge-"][data-aging-type="REPEATED"]');
}

function absoluteEdge(page) {
  return page.locator('[data-testid^="developer-wf-aging-edge-"]').filter({
    hasText: "Aging 15",
  });
}

function countRepeatedPosts(page) {
  let posts = 0;
  page.on("request", (req) => {
    if (req.method() !== "POST" || !req.url().includes("/aging-transitions")) {
      return;
    }
    const body = req.postData() || "";
    if (body.includes("REPEATED")) {
      posts += 1;
    }
  });
  return () => posts;
}

test.describe("Developer repeated aging transition (#5242)", () => {
  test("save lists the repeated row only after success and keeps the absolute row", async ({
    page,
    request,
  }) => {
    test.setTimeout(180_000);
    const guards = attachConsoleGuards(page);
    const posts = countRepeatedPosts(page);
    const name = uniqueWorkflowName("Nightly Repeated");
    const headers = jsonHeaders();
    const base = `${BASE_URL}/Rhythmyx/services/workflows`;
    const created = await request.post(base, {
      headers,
      data: { WorkflowCreate: { name, description: "slice 75" } },
    });
    expect(created.status(), await created.text()).toBe(200);
    const absolute = await request.post(`${base}/${encodeURIComponent(name)}/aging-transitions`, {
      headers,
      data: {
        WorkflowAgingTransitionWrite: { from: "Draft", to: "Review", intervalMinutes: 15 },
      },
    });
    const absoluteBody = await absolute.text();
    expect(absolute.status(), absoluteBody).toBe(200);
    expect(absoluteBody).toMatch(/"agingType"\s*:\s*"ABSOLUTE"/);
    expect(absoluteBody).not.toMatch(/"agingType"\s*:\s*"REPEATED"/);

    await loginAsAdmin(page);
    await openWorkflowsCatalog(page);
    await page
      .locator(catalogOpenByExactName("developer-wf-open", "data-wf-name", name))
      .click();
    await expect(page.locator('[data-testid="developer-wf-graph-kind"]')).toContainText(
      /Custom workflow/i,
      { timeout: 30_000 },
    );
    await expect(page.locator('[data-testid="developer-wf-repeated-aging-form"]')).toBeVisible();
    await expect(absoluteEdge(page)).toBeVisible({ timeout: 30_000 });
    await expect(repeatedEdge(page)).toHaveCount(0);

    await page.locator('[data-testid="developer-wf-repeated-aging-minutes"]').fill("20");
    await page.locator('[data-testid="developer-wf-repeated-aging-cancel"]').click();
    expect(posts()).toBe(0);
    await expect(page.locator('[data-testid="developer-wf-repeated-aging-minutes"]')).toHaveValue("");
    await expect(repeatedEdge(page)).toHaveCount(0);
    await expect(absoluteEdge(page)).toBeVisible();

    await page.locator('[data-testid="developer-wf-repeated-aging-minutes"]').fill("20");
    await page.locator('[data-testid="developer-wf-repeated-aging-add"]').click();
    await expect(page.locator('[data-testid="developer-wf-graph-error"]')).toBeVisible();
    expect(posts()).toBe(0);
    await expect(repeatedEdge(page)).toHaveCount(0);

    await page.locator('[data-testid="developer-wf-repeated-aging-from"]').selectOption("Draft");
    await page.locator('[data-testid="developer-wf-repeated-aging-to"]').selectOption("Review");
    await page.locator('[data-testid="developer-wf-repeated-aging-minutes"]').fill("0");
    await page.locator('[data-testid="developer-wf-repeated-aging-add"]').click();
    await expect(page.locator('[data-testid="developer-wf-graph-error"]')).toBeVisible();
    expect(posts()).toBe(0);
    await expect(repeatedEdge(page)).toHaveCount(0);
    await expect(absoluteEdge(page)).toBeVisible();

    await page.locator('[data-testid="developer-wf-repeated-aging-minutes"]').fill("20");
    await page.locator('[data-testid="developer-wf-repeated-aging-add"]').click();
    const createdEdge = repeatedEdge(page);
    await expect(createdEdge).toBeVisible({ timeout: 30_000 });
    await expect(createdEdge).toContainText("Repeated aging 20");
    await expect(createdEdge).toContainText("repeated");
    await expect(createdEdge).toHaveAttribute("data-from", "Draft");
    await expect(createdEdge).toHaveAttribute("data-to", "Review");
    await expect(createdEdge).toHaveAttribute("data-interval", "20");
    await expect(createdEdge).toHaveAttribute("data-aging-type", "REPEATED");
    await expect(absoluteEdge(page)).toBeVisible();
    await expect(page.locator('[data-testid="developer-wf-graph-notice"]')).toContainText(
      /Repeated aging transition saved/i,
    );
    expect(posts()).toBe(1);

    await page.locator('[data-testid="developer-wf-back"]').click();
    await expect(page.locator('[data-testid="developer-wf-table"]')).toBeVisible({
      timeout: 30_000,
    });
    await page
      .locator(catalogOpenByExactName("developer-wf-open", "data-wf-name", name))
      .click();
    await expect(repeatedEdge(page)).toBeVisible({ timeout: 30_000 });
    await expect(absoluteEdge(page)).toBeVisible();
    guards.assertClean();
  });

  test("HTTP 400, 403, and 409 leave the graph without the repeated edge", async ({
    page,
    request,
  }) => {
    test.setTimeout(180_000);
    const guards = attachConsoleGuards(page);
    const name = uniqueWorkflowName("Nightly RepeatedErr");
    const headers = jsonHeaders();
    const created = await request.post(`${BASE_URL}/Rhythmyx/services/workflows`, {
      headers,
      data: { WorkflowCreate: { name, description: "slice 75 errors" } },
    });
    expect(created.status(), await created.text()).toBe(200);
    const absolute = await request.post(
      `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent(name)}/aging-transitions`,
      {
        headers,
        data: {
          WorkflowAgingTransitionWrite: { from: "Draft", to: "Review", intervalMinutes: 15 },
        },
      },
    );
    expect(absolute.status(), await absolute.text()).toBe(200);

    await page.route("**/aging-transitions**", async (route) => {
      const body = route.request().postData() || "";
      if (route.request().method() !== "POST" || !body.includes("REPEATED")) {
        await route.continue();
        return;
      }
      const status = body.includes('"intervalMinutes":409')
        ? 409
        : body.includes('"intervalMinutes":403')
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
    await expect(page.locator('[data-testid="developer-wf-repeated-aging-form"]')).toBeVisible({
      timeout: 30_000,
    });
    await expect(absoluteEdge(page)).toBeVisible();
    await page.locator('[data-testid="developer-wf-repeated-aging-from"]').selectOption("Draft");
    await page.locator('[data-testid="developer-wf-repeated-aging-to"]').selectOption("Review");
    for (const minutes of ["400", "403", "409"]) {
      await page.locator('[data-testid="developer-wf-repeated-aging-minutes"]').fill(minutes);
      await page.locator('[data-testid="developer-wf-repeated-aging-add"]').click();
      await expect(page.locator('[data-testid="developer-wf-graph-error"]')).toBeVisible();
      await expect(page.locator('[data-testid="developer-wf-graph-notice"]')).toHaveCount(0);
      await expect(repeatedEdge(page)).toHaveCount(0);
      await expect(absoluteEdge(page)).toBeVisible();
    }
    guards.assertClean();
  });

  test("packaged workflow has no repeated form; REST keeps absolute and rejects bad type", async ({
    page,
    request,
  }) => {
    test.setTimeout(180_000);
    const guards = attachConsoleGuards(page);
    const headers = jsonHeaders();
    const base = `${BASE_URL}/Rhythmyx/services/workflows`;

    const forbidden = await request.post(
      `${base}/${encodeURIComponent("Default Workflow")}/aging-transitions`,
      {
        headers,
        data: {
          WorkflowAgingTransitionWrite: {
            from: "Draft",
            to: "Review",
            intervalMinutes: 15,
            type: "REPEATED",
          },
        },
      },
    );
    expect(forbidden.status()).toBe(403);

    const missing = await request.post(
      `${base}/${encodeURIComponent("No Such Workflow 5242")}/aging-transitions`,
      {
        headers,
        data: {
          WorkflowAgingTransitionWrite: {
            from: "Draft",
            to: "Review",
            intervalMinutes: 15,
            type: "REPEATED",
          },
        },
      },
    );
    expect(missing.status()).toBe(404);

    const name = uniqueWorkflowName("Nightly RepeatedDup");
    const created = await request.post(base, {
      headers,
      data: { WorkflowCreate: { name, description: "slice 75 dup" } },
    });
    expect(created.status(), await created.text()).toBe(200);

    const systemField = await request.post(`${base}/${encodeURIComponent(name)}/aging-transitions`, {
      headers,
      data: {
        WorkflowAgingTransitionWrite: {
          from: "Draft",
          to: "Review",
          intervalMinutes: 15,
          type: "SYSTEM_FIELD",
        },
      },
    });
    expect(systemField.status()).toBe(400);

    const absolute = await request.post(`${base}/${encodeURIComponent(name)}/aging-transitions`, {
      headers,
      data: {
        WorkflowAgingTransitionWrite: { from: "Draft", to: "Review", intervalMinutes: 15 },
      },
    });
    expect(absolute.status(), await absolute.text()).toBe(200);

    const sameInterval = await request.post(`${base}/${encodeURIComponent(name)}/aging-transitions`, {
      headers,
      data: {
        WorkflowAgingTransitionWrite: {
          from: "Draft",
          to: "Review",
          intervalMinutes: 15,
          type: "REPEATED",
        },
      },
    });
    const sameBody = await sameInterval.text();
    expect(sameInterval.status(), sameBody).toBe(200);
    expect(sameBody).toMatch(/"agingType"\s*:\s*"ABSOLUTE"/);
    expect(sameBody).toMatch(/"agingType"\s*:\s*"REPEATED"/);
    expect(sameBody).toMatch(/Repeated aging 15/);

    const dup = await request.post(`${base}/${encodeURIComponent(name)}/aging-transitions`, {
      headers,
      data: {
        WorkflowAgingTransitionWrite: {
          from: "Draft",
          to: "Review",
          intervalMinutes: 15,
          type: "REPEATED",
        },
      },
    });
    expect(dup.status()).toBe(409);

    await loginAsAdmin(page);
    await openWorkflowsCatalog(page);
    await page
      .locator(catalogOpenByExactName("developer-wf-open", "data-wf-name", "Default Workflow"))
      .click();
    await expect(page.locator('[data-testid="developer-wf-graph-kind"]')).toContainText(
      /Packaged workflow/i,
      { timeout: 30_000 },
    );
    await expect(page.locator('[data-testid="developer-wf-repeated-aging-form"]')).toHaveCount(0);
    guards.assertClean();
  });
});
