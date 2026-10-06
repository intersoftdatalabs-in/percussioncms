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
 * Developer → Workflows: delete one repeated or system-field aging transition (#5244 / parent #1690).
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… TEST_DB_TYPE=h2 TEST_PRODUCT=cms \
 *     npm run test:surface -- --path tests/developer-workflow-typed-aging-delete.spec.js
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

function agingEdge(page, type, extra) {
  let selector = `[data-testid^="developer-wf-aging-edge-"][data-aging-type="${type}"]`;
  if (extra) {
    selector += extra;
  }
  return page.locator(selector);
}

function countTypedDeletes(page) {
  let repeated = 0;
  let system = 0;
  page.on("request", (req) => {
    if (req.method() !== "DELETE" || !req.url().includes("/aging-transitions")) {
      return;
    }
    const url = req.url();
    if (url.includes("type=REPEATED")) {
      repeated += 1;
    } else if (url.includes("type=SYSTEM_FIELD")) {
      system += 1;
    }
  });
  return () => ({ repeated, system });
}

function graphEdges(body) {
  const parsed = JSON.parse(body);
  const queue = [parsed];
  const seen = new Set();
  while (queue.length > 0) {
    const current = queue.shift();
    if (!current || typeof current !== "object" || seen.has(current)) {
      continue;
    }
    seen.add(current);
    if (Array.isArray(current.edges)) {
      return current.edges;
    }
    const values = Array.isArray(current) ? current : Object.values(current);
    for (const value of values) {
      if (value && typeof value === "object") {
        queue.push(value);
      }
    }
  }
  throw new Error(`No edges in graph body: ${String(body).slice(0, 500)}`);
}

function hasDraftReview(edges, predicate) {
  return edges.some(
    (edge) => edge && edge.from === "Draft" && edge.to === "Review" && predicate(edge),
  );
}

async function postAging(request, base, name, body) {
  const created = await request.post(`${base}/${encodeURIComponent(name)}/aging-transitions`, {
    headers: jsonHeaders(),
    data: { WorkflowAgingTransitionWrite: body },
  });
  expect(created.status(), await created.text()).toBe(200);
}

async function seedTypedAging(request, name) {
  const headers = jsonHeaders();
  const base = `${BASE_URL}/Rhythmyx/services/workflows`;
  const created = await request.post(base, {
    headers,
    data: { WorkflowCreate: { name, description: "slice 77" } },
  });
  expect(created.status(), await created.text()).toBe(200);
  await postAging(request, base, name, { from: "Draft", to: "Review", intervalMinutes: 15 });
  await postAging(request, base, name, {
    from: "Draft",
    to: "Review",
    intervalMinutes: 15,
    type: "REPEATED",
  });
  await postAging(request, base, name, { from: "Draft", to: "Review", intervalMinutes: 1 });
  await postAging(request, base, name, {
    from: "Draft",
    to: "Review",
    type: "SYSTEM_FIELD",
    systemField: "CONTENTSTARTDATE",
  });
  return base;
}

test.describe("Developer delete repeated or system-field aging (#5244)", () => {
  test("confirm removes the typed row only after success and leaves the absolute row", async ({
    page,
    request,
  }) => {
    test.setTimeout(180_000);
    const guards = attachConsoleGuards(page);
    const deletes = countTypedDeletes(page);
    const name = uniqueWorkflowName("Nightly TypedAgeDel");
    await seedTypedAging(request, name);

    await loginAsAdmin(page);
    await openWorkflowsCatalog(page);
    await page
      .locator(catalogOpenByExactName("developer-wf-open", "data-wf-name", name))
      .click();
    const repeated = agingEdge(page, "REPEATED", '[data-interval="15"]');
    const absolute15 = agingEdge(page, "ABSOLUTE", '[data-interval="15"]');
    const absolute1 = agingEdge(page, "ABSOLUTE", '[data-interval="1"]');
    const system = agingEdge(
      page,
      "SYSTEM_FIELD",
      '[data-from="Draft"][data-to="Review"][data-system-field="CONTENTSTARTDATE"]',
    );
    await expect(repeated).toBeVisible({ timeout: 30_000 });
    await expect(absolute15).toBeVisible();
    await expect(absolute1).toBeVisible();
    await expect(system).toBeVisible();
    await expect(page.locator('[data-testid="developer-catalog-confirm-dialog"]')).toHaveCount(0);

    await repeated.locator('[data-testid^="developer-wf-aging-delete-"]').click();
    await expect(page.locator('[data-testid="developer-catalog-confirm-body"]')).toContainText(
      /repeated/i,
    );
    await page.locator('[data-testid="developer-catalog-confirm-cancel"]').click();
    expect(deletes()).toEqual({ repeated: 0, system: 0 });
    await expect(repeated).toBeVisible();
    await expect(absolute15).toBeVisible();
    await expect(page.locator('[data-testid="developer-wf-graph-notice"]')).toHaveCount(0);

    await repeated.locator('[data-testid^="developer-wf-aging-delete-"]').click();
    await page.locator('[data-testid="developer-catalog-confirm-submit"]').click();
    await expect(repeated).toHaveCount(0, { timeout: 30_000 });
    await expect(absolute15).toBeVisible();
    await expect(absolute1).toBeVisible();
    await expect(system).toBeVisible();
    await expect(page.locator('[data-testid="developer-wf-graph-notice"]')).toContainText(
      /Aging transition deleted/i,
    );
    expect(deletes()).toEqual({ repeated: 1, system: 0 });

    await page.locator('[data-testid="developer-wf-back"]').click();
    await expect(page.locator('[data-testid="developer-wf-table"]')).toBeVisible({
      timeout: 30_000,
    });
    await page
      .locator(catalogOpenByExactName("developer-wf-open", "data-wf-name", name))
      .click();
    await expect(agingEdge(page, "REPEATED", '[data-interval="15"]')).toHaveCount(0);
    await expect(agingEdge(page, "ABSOLUTE", '[data-interval="15"]')).toBeVisible({
      timeout: 30_000,
    });

    const systemAgain = agingEdge(
      page,
      "SYSTEM_FIELD",
      '[data-from="Draft"][data-to="Review"][data-system-field="CONTENTSTARTDATE"]',
    );
    await expect(systemAgain).toBeVisible();
    await systemAgain.locator('[data-testid^="developer-wf-aging-delete-"]').click();
    await expect(page.locator('[data-testid="developer-catalog-confirm-body"]')).toContainText(
      "CONTENTSTARTDATE",
    );
    await page.locator('[data-testid="developer-catalog-confirm-cancel"]').click();
    expect(deletes()).toEqual({ repeated: 1, system: 0 });
    await expect(systemAgain).toBeVisible();

    await systemAgain.locator('[data-testid^="developer-wf-aging-delete-"]').click();
    await page.locator('[data-testid="developer-catalog-confirm-submit"]').click();
    await expect(systemAgain).toHaveCount(0, { timeout: 30_000 });
    await expect(agingEdge(page, "ABSOLUTE", '[data-interval="15"]')).toBeVisible();
    await expect(agingEdge(page, "ABSOLUTE", '[data-interval="1"]')).toBeVisible();
    expect(deletes()).toEqual({ repeated: 1, system: 1 });
    guards.assertClean();
  });

  test("HTTP 400, 403, and 409 leave the typed row and the absolute row", async ({
    page,
    request,
  }) => {
    test.setTimeout(180_000);
    const guards = attachConsoleGuards(page);
    const name = uniqueWorkflowName("Nightly TypedAgeErr");
    await seedTypedAging(request, name);
    const statuses = [400, 403, 409];
    let calls = 0;

    await page.route("**/aging-transitions**", async (route) => {
      const req = route.request();
      const url = req.url();
      if (
        req.method() !== "DELETE" ||
        (!url.includes("type=REPEATED") && !url.includes("type=SYSTEM_FIELD"))
      ) {
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
    const repeated = agingEdge(page, "REPEATED", '[data-interval="15"]');
    const absolute15 = agingEdge(page, "ABSOLUTE", '[data-interval="15"]');
    await expect(repeated).toBeVisible({ timeout: 30_000 });
    await expect(absolute15).toBeVisible();
    for (const status of statuses) {
      await repeated.locator('[data-testid^="developer-wf-aging-delete-"]').click();
      await page.locator('[data-testid="developer-catalog-confirm-submit"]').click();
      await expect(page.locator('[data-testid="developer-wf-graph-error"]')).toBeVisible();
      await expect(page.locator('[data-testid="developer-wf-graph-notice"]')).toHaveCount(0);
      await expect(repeated).toBeVisible();
      await expect(absolute15).toBeVisible();
      expect(status).toBeGreaterThan(0);
    }
    expect(calls).toBe(3);
    guards.assertClean();
  });

  test("REST deletes one typed edge and leaves the absolute edge", async ({ page, request }) => {
    test.setTimeout(180_000);
    const guards = attachConsoleGuards(page);
    const headers = jsonHeaders();
    const name = uniqueWorkflowName("Nightly TypedAgeRest");
    const base = await seedTypedAging(request, name);
    const workflow = `${base}/${encodeURIComponent(name)}`;

    const repeatedOnly = await request.delete(
      `${workflow}/aging-transitions?from=Draft&to=Review&intervalMinutes=15&type=REPEATED`,
      { headers },
    );
    const repeatedBody = await repeatedOnly.text();
    expect(repeatedOnly.status(), repeatedBody).toBe(200);
    const afterRepeated = graphEdges(repeatedBody);
    expect(hasDraftReview(afterRepeated, (edge) => edge.agingType === "REPEATED" && edge.intervalMinutes === 15)).toBe(
      false,
    );
    expect(hasDraftReview(afterRepeated, (edge) => edge.agingType === "ABSOLUTE" && edge.intervalMinutes === 15)).toBe(
      true,
    );
    expect(hasDraftReview(afterRepeated, (edge) => edge.agingType === "ABSOLUTE" && edge.intervalMinutes === 1)).toBe(
      true,
    );
    expect(
      hasDraftReview(afterRepeated, (edge) => edge.systemField === "CONTENTSTARTDATE"),
    ).toBe(true);

    const systemOnly = await request.delete(
      `${workflow}/aging-transitions?from=Draft&to=Review&type=SYSTEM_FIELD&systemField=CONTENTSTARTDATE`,
      { headers },
    );
    const systemBody = await systemOnly.text();
    expect(systemOnly.status(), systemBody).toBe(200);
    const afterSystem = graphEdges(systemBody);
    expect(hasDraftReview(afterSystem, (edge) => edge.systemField === "CONTENTSTARTDATE")).toBe(false);
    expect(hasDraftReview(afterSystem, (edge) => edge.agingType === "ABSOLUTE" && edge.intervalMinutes === 15)).toBe(
      true,
    );
    expect(hasDraftReview(afterSystem, (edge) => edge.agingType === "ABSOLUTE" && edge.intervalMinutes === 1)).toBe(
      true,
    );

    const still = await request.get(`${workflow}/graph`, { headers });
    const stillBody = await still.text();
    expect(still.status(), stillBody).toBe(200);
    const stillEdges = graphEdges(stillBody);
    expect(hasDraftReview(stillEdges, (edge) => edge.agingType === "ABSOLUTE" && edge.intervalMinutes === 15)).toBe(
      true,
    );
    expect(hasDraftReview(stillEdges, (edge) => edge.agingType === "REPEATED")).toBe(false);
    expect(hasDraftReview(stillEdges, (edge) => edge.systemField === "CONTENTSTARTDATE")).toBe(false);

    const absoluteOnly = uniqueWorkflowName("Nightly TypedAge409");
    const absoluteBase = `${BASE_URL}/Rhythmyx/services/workflows`;
    const created = await request.post(absoluteBase, {
      headers,
      data: { WorkflowCreate: { name: absoluteOnly, description: "slice 77 409" } },
    });
    expect(created.status(), await created.text()).toBe(200);
    await postAging(request, absoluteBase, absoluteOnly, {
      from: "Draft",
      to: "Review",
      intervalMinutes: 15,
    });
    const wrongType = await request.delete(
      `${absoluteBase}/${encodeURIComponent(absoluteOnly)}/aging-transitions?from=Draft&to=Review&intervalMinutes=15&type=REPEATED`,
      { headers },
    );
    expect(wrongType.status()).toBe(409);
    const afterConflict = await request.get(
      `${absoluteBase}/${encodeURIComponent(absoluteOnly)}/graph`,
      { headers },
    );
    const conflictBody = await afterConflict.text();
    expect(afterConflict.status(), conflictBody).toBe(200);
    expect(
      hasDraftReview(graphEdges(conflictBody), (edge) => edge.agingType === "ABSOLUTE" && edge.intervalMinutes === 15),
    ).toBe(true);

    const badType = await request.delete(
      `${workflow}/aging-transitions?from=Draft&to=Review&intervalMinutes=15&type=NOPE`,
      { headers },
    );
    expect(badType.status()).toBe(400);
    const blankField = await request.delete(
      `${workflow}/aging-transitions?from=Draft&to=Review&type=SYSTEM_FIELD&systemField=`,
      { headers },
    );
    expect(blankField.status()).toBe(400);
    const unknownField = await request.delete(
      `${workflow}/aging-transitions?from=Draft&to=Review&type=SYSTEM_FIELD&systemField=NOT_A_FIELD`,
      { headers },
    );
    expect(unknownField.status()).toBe(400);
    const nonPositive = await request.delete(
      `${workflow}/aging-transitions?from=Draft&to=Review&intervalMinutes=0&type=REPEATED`,
      { headers },
    );
    expect(nonPositive.status()).toBe(400);
    const missingEdge = await request.delete(
      `${workflow}/aging-transitions?from=Draft&to=Review&intervalMinutes=99&type=REPEATED`,
      { headers },
    );
    expect(missingEdge.status()).toBe(404);
    const missingWorkflow = await request.delete(
      `${absoluteBase}/${encodeURIComponent("No Such Workflow 5244")}/aging-transitions?from=Draft&to=Review&intervalMinutes=15&type=REPEATED`,
      { headers },
    );
    expect(missingWorkflow.status()).toBe(404);

    const packaged = await request.delete(
      `${absoluteBase}/${encodeURIComponent("Default Workflow")}/aging-transitions?from=Draft&to=Review&intervalMinutes=15&type=REPEATED`,
      { headers },
    );
    expect(packaged.status()).toBe(403);

    const untouched = await request.get(`${workflow}/graph`, { headers });
    const untouchedBody = await untouched.text();
    expect(untouched.status(), untouchedBody).toBe(200);
    const untouchedEdges = graphEdges(untouchedBody);
    expect(
      hasDraftReview(untouchedEdges, (edge) => edge.agingType === "ABSOLUTE" && edge.intervalMinutes === 15),
    ).toBe(true);
    expect(
      hasDraftReview(untouchedEdges, (edge) => edge.agingType === "ABSOLUTE" && edge.intervalMinutes === 1),
    ).toBe(true);

    await loginAsAdmin(page);
    await openWorkflowsCatalog(page);
    await page
      .locator(catalogOpenByExactName("developer-wf-open", "data-wf-name", "Default Workflow"))
      .click();
    await expect(page.locator('[data-testid="developer-wf-graph-kind"]')).toContainText(
      /Packaged workflow/i,
      { timeout: 30_000 },
    );
    await expect(page.locator('[data-testid^="developer-wf-aging-delete-"]')).toHaveCount(0);
    guards.assertClean();
  });
});
