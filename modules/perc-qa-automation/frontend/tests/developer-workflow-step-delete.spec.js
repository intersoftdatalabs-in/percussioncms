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
 * Developer → Workflows: delete one step that has no remaining transitions (#4769).
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… TEST_DB_TYPE=h2 TEST_PRODUCT=cms \
 *     npm run test:surface -- --path tests/developer-workflow-step-delete.spec.js
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

function unwrapGraph(body) {
  return body.WorkflowGraph || body;
}

test.describe("Developer workflow step delete (#4769)", () => {
  test("Admin deletes an unreferenced step; it stays gone after reload", async ({
    page,
    request,
  }) => {
    test.setTimeout(180_000);
    const guards = attachConsoleGuards(page);
    const name = uniqueWorkflowName("Nightly StDel");
    const stepName = "OrphanQA";
    const headers = jsonHeaders();
    const base = `${BASE_URL}/Rhythmyx/services/workflows`;

    const created = await request.post(base, {
      headers,
      data: { WorkflowCreate: { name, description: "slice 34" } },
    });
    expect(created.status(), await created.text()).toBe(200);

    const seeded = await request.get(`${base}/${encodeURIComponent(name)}/graph`, { headers });
    expect(seeded.status()).toBe(200);
    const seedGraph = unwrapGraph(await seeded.json());
    const after = (seedGraph.nodes && seedGraph.nodes[0] && seedGraph.nodes[0].name) || "Draft";
    const added = await request.post(`${base}/${encodeURIComponent(name)}/steps`, {
      headers,
      data: { WorkflowStepWrite: { name: stepName, afterStep: after, roleNames: ["Admin"] } },
    });
    expect(added.status(), await added.text()).toBe(200);

    for (let i = 0; i < 40; i += 1) {
      const graphRes = await request.get(`${base}/${encodeURIComponent(name)}/graph`, { headers });
      expect(graphRes.status()).toBe(200);
      const graph = unwrapGraph(await graphRes.json());
      const edges = graph.edges || [];
      const hit = edges.find((e) => e.from === stepName || e.to === stepName);
      if (!hit) {
        break;
      }
      const q = new URLSearchParams({ from: hit.from, label: hit.label, to: hit.to });
      const removed = await request.delete(
        `${base}/${encodeURIComponent(name)}/transitions?${q.toString()}`,
        { headers },
      );
      expect(removed.status(), await removed.text()).toBe(200);
      if (i === 39) {
        throw new Error(`could not clear transitions for ${stepName}`);
      }
    }

    const stillUsed = await request.delete(
      `${base}/${encodeURIComponent(name)}/steps/${encodeURIComponent(after)}`,
      { headers },
    );
    expect(stillUsed.status()).toBe(409);

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
    const node = page
      .locator('[data-testid^="developer-wf-graph-node-"]')
      .filter({ hasText: stepName });
    await expect(node).toBeVisible();
    await node.locator('button:has-text("Delete step")').click();
    await page.locator('[data-testid="developer-catalog-confirm-submit"]').click();
    await expect(page.locator('[data-testid="developer-wf-graph-notice"]')).toBeVisible({
      timeout: 30_000,
    });
    await expect(
      page.locator('[data-testid^="developer-wf-graph-node-"]').filter({ hasText: stepName }),
    ).toHaveCount(0);

    await page.locator('[data-testid="developer-wf-back"]').click();
    await expect(page.locator('[data-testid="developer-wf-table"]')).toBeVisible({
      timeout: 30_000,
    });
    await page
      .locator(catalogOpenByExactName("developer-wf-open", "data-wf-name", name))
      .click();
    await expect(page.locator('[data-testid="developer-wf-graph-nodes"]')).toBeVisible({
      timeout: 30_000,
    });
    await expect(
      page.locator('[data-testid^="developer-wf-graph-node-"]').filter({ hasText: stepName }),
    ).toHaveCount(0);
    guards.assertClean();
  });

  test("packaged workflow has no step delete; REST maps 403/404/400", async ({
    page,
    request,
  }) => {
    test.setTimeout(180_000);
    const guards = attachConsoleGuards(page);
    const headers = jsonHeaders();
    const base = `${BASE_URL}/Rhythmyx/services/workflows`;

    const forbidden = await request.delete(
      `${base}/${encodeURIComponent("Default Workflow")}/steps/${encodeURIComponent("Draft")}`,
      { headers },
    );
    expect(forbidden.status()).toBe(403);

    const missing = await request.delete(
      `${base}/${encodeURIComponent("No Such Workflow 4769")}/steps/${encodeURIComponent("Draft")}`,
      { headers },
    );
    expect(missing.status()).toBe(404);

    const bad = await request.delete(
      `${base}/${encodeURIComponent("Default Workflow")}/steps/${encodeURIComponent("bad*name")}`,
      { headers },
    );
    expect(bad.status()).toBe(400);

    await loginAsAdmin(page);
    await openWorkflowsCatalog(page);
    await page
      .locator(catalogOpenByExactName("developer-wf-open", "data-wf-name", "Default Workflow"))
      .click();
    await expect(page.locator('[data-testid="developer-wf-graph-kind"]')).toContainText(
      /Packaged workflow/i,
      { timeout: 30_000 },
    );
    await expect(page.locator('[data-testid^="developer-wf-graph-delete-step-"]')).toHaveCount(0);
    guards.assertClean();
  });
});
