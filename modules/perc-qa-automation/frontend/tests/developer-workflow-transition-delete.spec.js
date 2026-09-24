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
 * Developer → Workflows: delete one transition between existing steps (#4768).
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… TEST_DB_TYPE=h2 TEST_PRODUCT=cms \
 *     npm run test:surface -- --path tests/developer-workflow-transition-delete.spec.js
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

function edgeText(edge) {
  return `${edge.from} — ${edge.label} → ${edge.to}`;
}

test.describe("Developer workflow transition delete (#4768)", () => {
  test("Admin deletes one transition; it stays gone after reload; steps remain", async ({
    page,
    request,
  }) => {
    test.setTimeout(180_000);
    const guards = attachConsoleGuards(page);
    const name = uniqueWorkflowName("Nightly TrDel");
    const created = await request.post(`${BASE_URL}/Rhythmyx/services/workflows`, {
      headers: jsonHeaders(),
      data: { WorkflowCreate: { name, description: "slice 33" } },
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
    const victim = edges[0];
    const nodes = before.nodes || before.WorkflowGraph?.nodes || [];

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
      .filter({ hasText: edgeText(victim) });
    await expect(edgeRow).toBeVisible();
    await edgeRow.locator('button:has-text("Delete transition")').click();
    await page.locator('[data-testid="developer-catalog-confirm-submit"]').click();
    await expect(page.locator('[data-testid="developer-wf-graph-notice"]')).toBeVisible({
      timeout: 30_000,
    });
    await expect(edgeRow).toHaveCount(0);

    for (const node of nodes.slice(0, 3)) {
      if (node && node.name) {
        await expect(page.locator('[data-testid="developer-wf-graph-nodes"]')).toContainText(
          node.name,
        );
      }
    }
    await expect(page.locator('[data-testid="developer-wf-steps"]')).toBeVisible();

    await page.locator('[data-testid="developer-wf-back"]').click();
    await expect(page.locator('[data-testid="developer-wf-table"]')).toBeVisible({
      timeout: 30_000,
    });
    await page
      .locator(catalogOpenByExactName("developer-wf-open", "data-wf-name", name))
      .click();
    await expect(page.locator('[data-testid="developer-wf-graph-edges"]')).toBeVisible({
      timeout: 30_000,
    });
    await expect(
      page.locator('[data-testid="developer-wf-graph-edges"] li').filter({
        hasText: edgeText(victim),
      }),
    ).toHaveCount(0);
    guards.assertClean();
  });

  test("packaged workflow has no delete control; REST maps 403/404/400", async ({
    page,
    request,
  }) => {
    test.setTimeout(180_000);
    const guards = attachConsoleGuards(page);
    const headers = jsonHeaders();
    const base = `${BASE_URL}/Rhythmyx/services/workflows`;

    const forbidden = await request.delete(
      `${base}/${encodeURIComponent("Default Workflow")}/transitions?from=Draft&label=Submit&to=Review`,
      { headers },
    );
    expect(forbidden.status()).toBe(403);

    const missing = await request.delete(
      `${base}/${encodeURIComponent("No Such Workflow 4768")}/transitions?from=Draft&label=Submit`,
      { headers },
    );
    expect(missing.status()).toBe(404);

    const bad = await request.delete(
      `${base}/${encodeURIComponent("Default Workflow")}/transitions?from=&label=`,
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
    await expect(page.locator('[data-testid^="developer-wf-graph-delete-"]')).toHaveCount(0);
    guards.assertClean();
  });
});
