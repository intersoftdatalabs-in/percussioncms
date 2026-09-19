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
 * Developer workflow create (slice 21 / #4564 / parent #1690).
 *
 * Admin creates a named workflow from Developer → Workflows (POST
 * /services/workflows with the WorkflowCreate wrap). States, transitions,
 * and roles come from the product base-workflow template; the new row opens
 * and appears on the stepped catalog. Duplicate name is 409, invalid name is
 * 400, non-Admin is 403. Full graph design stays outside this surface.
 *
 * Peer of developer-workflow-content-types.spec.js (SY-06 workflow detail).
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… \
 *     npm run test:surface -- --path tests/developer-workflow-create.spec.js
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
      expect(
        unexpectedConsole,
        `console error: ${unexpectedConsole.join(" | ")}`,
      ).toEqual([]);
    },
  };
}

async function openWorkflowsCatalog(page) {
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
}

test.describe("Developer workflow create (slice 21 / #4564)", () => {
  test("save stays disabled until the workflow name is valid", async ({ page }) => {
    test.setTimeout(120_000);
    const guards = attachConsoleGuards(page);
    await loginAsAdmin(page);
    await openWorkflowsCatalog(page);

    await page.locator('[data-testid="developer-wf-new"]').click();
    await expect(page.locator('[data-testid="developer-wf-create"]')).toBeVisible();

    const saveBtn = page.locator('[data-testid="developer-wf-create-save"]');
    await expect(saveBtn).toBeDisabled();

    await page.locator('[data-testid="developer-wf-create-name"]').fill("bad/name!");
    await expect(saveBtn).toBeDisabled();

    await page.locator('[data-testid="developer-wf-create-name"]').fill("Nightly QA Draft");
    await expect(saveBtn).toBeEnabled();
    guards.assertClean();
  });

  test("Admin creates a workflow; the new row opens and lists on catalog", async ({
    page,
  }) => {
    test.setTimeout(180_000);
    const guards = attachConsoleGuards(page);
    const name = uniqueWorkflowName("Nightly WF");
    await loginAsAdmin(page);
    await openWorkflowsCatalog(page);

    await page.locator('[data-testid="developer-wf-new"]').click();
    await page.locator('[data-testid="developer-wf-create-name"]').fill(name);
    await page
      .locator('[data-testid="developer-wf-create-description"]')
      .fill("Created by slice 21 surface spec");
    await page.locator('[data-testid="developer-wf-create-save"]').click();

    // handleCreated opens the new row (detail bound by workflow name). The
    // created notice may flash before navigation, so accept either surface.
    const detail = page.locator('[data-testid="developer-wf-detail"]');
    const notice = page.locator('[data-testid="developer-wf-create-notice"]');
    await expect(detail.or(notice).first()).toBeVisible({ timeout: 30_000 });
    await expect(detail).toBeVisible({ timeout: 30_000 });
    await expect(page.locator('[data-testid="developer-wf-detail-title"]')).toContainText(
      name,
    );

    // Back to the catalog: the created workflow lists by name.
    await page.locator('[data-testid="developer-wf-back"]').click();
    await expect(page.locator('[data-testid="developer-wf-table"]')).toBeVisible({
      timeout: 30_000,
    });
    const openBtn = page.locator(
      catalogOpenByExactName("developer-wf-open", "data-wf-name", name),
    );
    await expect(openBtn).toBeVisible({ timeout: 30_000 });
    guards.assertClean();
  });

  test("REST create contract: 409 duplicate, 400 invalid, appears on GET catalog", async ({
    request,
  }) => {
    test.setTimeout(180_000);
    const name = uniqueWorkflowName("Nightly REST WF");
    const base = `${BASE_URL}/Rhythmyx/services/workflows`;
    const headers = {
      ...adminBasicAuthHeaders(),
      Accept: "application/json",
      "Content-Type": "application/json",
    };

    const created = await request.post(base, {
      headers,
      data: { WorkflowCreate: { name, description: "REST contract check" } },
    });
    expect(created.status(), `POST ${base}`).toBe(200);
    const createdBody = await created.json();
    const createdName =
      createdBody.workflowName ||
      (createdBody.WorkflowSummary && createdBody.WorkflowSummary.workflowName);
    expect(createdName).toBe(name);

    const duplicate = await request.post(base, {
      headers,
      data: { WorkflowCreate: { name } },
    });
    expect(duplicate.status(), "duplicate name").toBe(409);

    const invalid = await request.post(base, {
      headers,
      data: { WorkflowCreate: { name: "  " } },
    });
    expect(invalid.status(), "blank name").toBe(400);

    const metadata = await request.get(
      `${BASE_URL}/Rhythmyx/services/workflowmanagement/workflows/metadata`,
      { headers: adminBasicAuthHeaders() },
    );
    expect(metadata.status(), "GET metadata catalog").toBe(200);
    const metadataText = await metadata.text();
    expect(metadataText).toContain(name);
  });
});
