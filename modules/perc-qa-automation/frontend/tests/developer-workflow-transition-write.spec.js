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
 * Developer → Workflows: create and update one transition between existing steps (#4706).
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… TEST_DB_TYPE=h2 TEST_PRODUCT=cms \
 *     npm run test:surface -- --path tests/developer-workflow-transition-write.spec.js
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

function edgeText(from, label, to) {
  return `${from} — ${label} → ${to}`;
}

test.describe("Developer workflow transition write (#4706)", () => {
  test("Admin adds a transition then updates its label; it stays after reload", async ({
    page,
    request,
  }) => {
    test.setTimeout(180_000);
    const guards = attachConsoleGuards(page);
    const name = uniqueWorkflowName("Nightly TrWrite");
    const created = await request.post(`${BASE_URL}/Rhythmyx/services/workflows`, {
      headers: jsonHeaders(),
      data: { WorkflowCreate: { name, description: "slice 31" } },
    });
    expect(created.status(), await created.text()).toBe(200);

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
    await expect(page.locator('[data-testid="developer-wf-transition-form"]')).toBeVisible();
    await page.locator('[data-testid="developer-wf-transition-from"]').selectOption("Draft");
    await page.locator('[data-testid="developer-wf-transition-to"]').selectOption("Review");
    await page.locator('[data-testid="developer-wf-transition-label"]').fill("Nightly Move");
    await page.locator('[data-testid="developer-wf-transition-add"]').click();
    const createdEdge = page
      .locator('[data-testid="developer-wf-graph-edges"] li')
      .filter({ hasText: edgeText("Draft", "Nightly Move", "Review") });
    await expect(createdEdge).toBeVisible({ timeout: 30_000 });
    await expect(page.locator('[data-testid="developer-wf-graph-notice"]')).toContainText(
      /Transition saved/i,
    );

    await createdEdge.locator('button:has-text("Edit transition")').click();
    await page.locator('[data-testid="developer-wf-transition-label"]').fill("Nightly Moved");
    await page.locator('[data-testid="developer-wf-transition-to"]').selectOption("Pending");
    await page.locator('[data-testid="developer-wf-transition-save"]').click();
    const updatedEdge = page
      .locator('[data-testid="developer-wf-graph-edges"] li')
      .filter({ hasText: edgeText("Draft", "Nightly Moved", "Pending") });
    await expect(updatedEdge).toBeVisible({ timeout: 30_000 });
    await expect(createdEdge).toHaveCount(0);

    await page.locator('[data-testid="developer-wf-back"]').click();
    await expect(page.locator('[data-testid="developer-wf-table"]')).toBeVisible({
      timeout: 30_000,
    });
    await page
      .locator(catalogOpenByExactName("developer-wf-open", "data-wf-name", name))
      .click();
    await expect(updatedEdge).toBeVisible({ timeout: 30_000 });
    await expect(
      page.locator('[data-testid="developer-wf-graph-edges"] li').filter({
        hasText: edgeText("Draft", "Nightly Move", "Review"),
      }),
    ).toHaveCount(0);
    guards.assertClean();
  });

  test("packaged workflow has no write form; REST maps 403/404/400/409", async ({
    page,
    request,
  }) => {
    test.setTimeout(180_000);
    const guards = attachConsoleGuards(page);
    const headers = jsonHeaders();
    const base = `${BASE_URL}/Rhythmyx/services/workflows`;

    const forbidden = await request.post(
      `${base}/${encodeURIComponent("Default Workflow")}/transitions`,
      {
        headers,
        data: { WorkflowTransitionWrite: { from: "Draft", to: "Review", label: "Send" } },
      },
    );
    expect(forbidden.status()).toBe(403);

    const missing = await request.post(
      `${base}/${encodeURIComponent("No Such Workflow 4706")}/transitions`,
      {
        headers,
        data: { WorkflowTransitionWrite: { from: "Draft", to: "Review", label: "Send" } },
      },
    );
    expect(missing.status()).toBe(404);

    const bad = await request.post(`${base}/${encodeURIComponent("Default Workflow")}/transitions`, {
      headers,
      data: { WorkflowTransitionWrite: { from: "", to: "", label: "" } },
    });
    expect(bad.status()).toBe(400);

    const name = uniqueWorkflowName("Nightly TrDup");
    const created = await request.post(base, {
      headers,
      data: { WorkflowCreate: { name, description: "slice 31 dup" } },
    });
    expect(created.status(), await created.text()).toBe(200);
    const first = await request.post(`${base}/${encodeURIComponent(name)}/transitions`, {
      headers,
      data: { WorkflowTransitionWrite: { from: "Draft", to: "Archive", label: "Nightly Dup" } },
    });
    expect(first.status(), await first.text()).toBe(200);
    const dup = await request.post(`${base}/${encodeURIComponent(name)}/transitions`, {
      headers,
      data: { WorkflowTransitionWrite: { from: "Draft", to: "Archive", label: "Nightly Dup" } },
    });
    expect(dup.status()).toBe(409);

    const badUpdate = await request.put(
      `${base}/${encodeURIComponent(name)}/transitions?from=&label=`,
      {
        headers,
        data: { WorkflowTransitionWrite: { to: "Review", label: "Send" } },
      },
    );
    expect(badUpdate.status()).toBe(400);

    await loginAsAdmin(page);
    await openWorkflowsCatalog(page);
    await page
      .locator(catalogOpenByExactName("developer-wf-open", "data-wf-name", "Default Workflow"))
      .click();
    await expect(page.locator('[data-testid="developer-wf-graph-kind"]')).toContainText(
      /Packaged workflow/i,
      { timeout: 30_000 },
    );
    await expect(page.locator('[data-testid="developer-wf-transition-form"]')).toHaveCount(0);
    await expect(page.locator('[data-testid^="developer-wf-graph-edit-"]')).toHaveCount(0);
    guards.assertClean();
  });
});
