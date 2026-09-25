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
 * Developer → Workflows: set comment-required on an existing transition (#4844).
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… TEST_DB_TYPE=h2 TEST_PRODUCT=cms \
 *     npm run test:surface -- --path tests/developer-workflow-comment-required.spec.js
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
}

function jsonHeaders() {
  return {
    ...adminBasicAuthHeaders(),
    Accept: "application/json",
    "Content-Type": "application/json",
  };
}

function edgeText(edge) {
  return `${edge.from} — ${edge.label} → ${edge.to}`;
}

test.describe("Developer workflow comment required (#4844)", () => {
  test("Admin marks one transition comment-required and the flag stays after reload", async ({
    page,
    request,
  }) => {
    test.setTimeout(180_000);
    const guards = attachConsoleGuards(page);
    const name = uniqueWorkflowName("Nightly Cmt");
    const created = await request.post(`${BASE_URL}/Rhythmyx/services/workflows`, {
      headers: jsonHeaders(),
      data: { WorkflowCreate: { name, description: "slice 38" } },
    });
    expect(created.status(), await created.text()).toBe(200);

    const graphRes = await request.get(
      `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent(name)}/graph`,
      { headers: jsonHeaders() },
    );
    expect(graphRes.status()).toBe(200);
    const before = await graphRes.json();
    const edges = before.edges || before.WorkflowGraph?.edges || [];
    expect(edges.length, JSON.stringify(before)).toBeGreaterThan(0);
    const target = edges[0];

    await loginAsAdmin(page);
    await openWorkflowsCatalog(page);
    await page
      .locator(catalogOpenByExactName("developer-wf-open", "data-wf-name", name))
      .click();
    await expect(page.locator('[data-testid="developer-wf-detail"]')).toBeVisible({
      timeout: 30_000,
    });
    await expect(page.locator('[data-testid="developer-wf-graph-kind"]')).toContainText(
      /Custom workflow/i,
    );
    const edgeRow = page
      .locator('[data-testid="developer-wf-graph-edges"] li')
      .filter({ hasText: edgeText(target) });
    await expect(edgeRow).toBeVisible();
    const box = edgeRow.locator('input[type="checkbox"]');
    await expect(box).not.toBeChecked();
    await box.check();
    await expect(page.locator('[data-testid="developer-wf-graph-notice"]')).toBeVisible({
      timeout: 30_000,
    });
    await expect(box).toBeChecked();

    await page.locator('[data-testid="developer-wf-back"]').click();
    await page
      .locator(catalogOpenByExactName("developer-wf-open", "data-wf-name", name))
      .click();
    const reloaded = page
      .locator('[data-testid="developer-wf-graph-edges"] li')
      .filter({ hasText: edgeText(target) })
      .locator('input[type="checkbox"]');
    await expect(reloaded).toBeChecked({ timeout: 30_000 });
    guards.assertClean();
  });
});
