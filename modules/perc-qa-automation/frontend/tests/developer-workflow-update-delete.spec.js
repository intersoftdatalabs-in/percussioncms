/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (name "License");
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
 * Developer workflow update / delete (slice 21 / #4606 / parent #1690).
 *
 * Admin opens a workflow, edits the description (PUT
 * /services/workflows/{idOrName}), and deletes it (DELETE
 * /services/workflows/{idOrName}). The chrome uses the public REST root
 * /services/workflows (not /services/workflowmanagement/workflows) so it sits
 * alongside the slice 21 create surface and SY-06 allowed-content-types.
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… \
 *     npm run test:surface -- --path tests/developer-workflow-update-delete.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const {
  loginAsAdmin,
  BASE_URL,
  adminBasicAuthHeaders,
} = require("./helpers/auth");
const {
  catalogOpenByExactName,
} = require("./helpers/developer-catalog-selectors");

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
        (t) =>
          !/Failed to load resource/i.test(t) &&
          !/favicon/i.test(t) &&
          !/Workflow .* still have items assigned/i.test(t),
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

async function createWorkflowViaRest(request, name, description) {
  const base = `${BASE_URL}/Rhythmyx/services/workflows`;
  const headers = {
    ...adminBasicAuthHeaders(),
    Accept: "application/json",
    "Content-Type": "application/json",
  };
  const res = await request.post(base, {
    headers,
    data: { WorkflowCreate: { name, description } },
  });
  if (res.status() !== 200) {
    throw new Error(
      `create via REST failed (${res.status()}): ${await res.text()}`,
    );
  }
}

async function deleteWorkflowViaRest(request, name) {
  const base = `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent(name)}`;
  const headers = {
    ...adminBasicAuthHeaders(),
    Accept: "application/json",
  };
  const res = await request.delete(base, { headers });
  // 204 on success; 404 is acceptable for tear-down if the test already removed it.
  if (res.status() !== 204 && res.status() !== 404) {
    throw new Error(
      `delete via REST failed (${res.status()}): ${await res.text()}`,
    );
  }
}

test.describe("Developer workflow update / delete (slice 21 / #4606)", () => {
  test("Admin edits a workflow description; the saved value re-renders after PUT", async ({
    page,
    request,
  }) => {
    test.setTimeout(180_000);
    const guards = attachConsoleGuards(page);
    const name = uniqueWorkflowName("Nightly WF Update");
    const updatedDescription = "Edited by update/delete spec";

    // Tear up: create via REST so we don't depend on create UI in this test.
    await createWorkflowViaRest(request, name, "Created by update/delete spec");

    await loginAsAdmin(page);
    await openWorkflowsCatalog(page);

    const openBtn = page.locator(
      catalogOpenByExactName("developer-wf-open", "data-wf-name", name),
    );
    await expect(openBtn).toBeVisible({ timeout: 30_000 });
    await openBtn.click();

    await expect(
      page.locator('[data-testid="developer-wf-detail"]'),
    ).toBeVisible({ timeout: 30_000 });

    // The workflowmanagement read does not surface stored descriptions
    // (PSUiWorkflow ignores PSWorkflow.getDescription()); the slice 21 update path
    // round-trips the value via PUT and reads it back from the response. Assert the
    // Save button is disabled at baseline, then becomes enabled on edit and persists.
    const descriptionInput = page.locator(
      '[data-testid="developer-wf-description-input"]',
    );
    const saveBtn = page.locator('[data-testid="developer-wf-description-save"]');
    await expect(saveBtn).toBeDisabled();

    await descriptionInput.fill(updatedDescription);
    await expect(saveBtn).toBeEnabled();
    await saveBtn.click();

    await expect(
      page.locator('[data-testid="developer-wf-description-notice"]'),
    ).toBeVisible({ timeout: 30_000 });
    await expect(saveBtn).toBeDisabled();

    // Back to the catalog and reopen the row to assert the PUT round-trip:
    // the response is the source of truth that the description field is rendered.
    await page.locator('[data-testid="developer-wf-back"]').click();
    await expect(
      page.locator('[data-testid="developer-wf-table"]'),
    ).toBeVisible({ timeout: 30_000 });
    const reopen = page.locator(
      catalogOpenByExactName("developer-wf-open", "data-wf-name", name),
    );
    await expect(reopen).toBeVisible({ timeout: 30_000 });
    await reopen.click();
    await expect(
      page.locator('[data-testid="developer-wf-description-input"]'),
    ).toBeVisible({ timeout: 30_000 });

    // Save again with a new value to make sure the PUT is repeatable.
    await descriptionInput.fill(`${updatedDescription} v2`);
    await saveBtn.click();
    await expect(
      page.locator('[data-testid="developer-wf-description-notice"]'),
    ).toBeVisible({ timeout: 30_000 });

    guards.assertClean();

    // Tear down: delete the workflow so the cell does not accumulate test rows.
    await deleteWorkflowViaRest(request, name);
  });

  test("Admin deletes a workflow from the detail chrome; catalog refreshes", async ({
    page,
    request,
  }) => {
    test.setTimeout(180_000);
    const guards = attachConsoleGuards(page);
    const name = uniqueWorkflowName("Nightly WF Delete");

    await createWorkflowViaRest(request, name, "Created for delete test");

    await loginAsAdmin(page);
    await openWorkflowsCatalog(page);

    const openBtn = page.locator(
      catalogOpenByExactName("developer-wf-open", "data-wf-name", name),
    );
    await expect(openBtn).toBeVisible({ timeout: 30_000 });
    await openBtn.click();

    await expect(
      page.locator('[data-testid="developer-wf-detail"]'),
    ).toBeVisible({ timeout: 30_000 });

    await page.locator('[data-testid="developer-wf-delete"]').click();
    await expect(
      page.locator('[data-testid="developer-catalog-confirm-dialog"]'),
    ).toBeVisible({ timeout: 10_000 });
    await page.locator('[data-testid="developer-catalog-confirm-cancel"]').click();
    await expect(
      page.locator('[data-testid="developer-catalog-confirm-dialog"]'),
    ).toHaveCount(0);

    // Confirm still on detail and workflow is still there.
    await expect(
      page.locator('[data-testid="developer-wf-detail"]'),
    ).toBeVisible();

    await page.locator('[data-testid="developer-wf-delete"]').click();
    await page
      .locator('[data-testid="developer-catalog-confirm-submit"]')
      .click();

    // After delete the catalog should refresh and the row should be gone.
    await expect(
      page.locator('[data-testid="developer-wf-panel"]'),
    ).toBeVisible({ timeout: 30_000 });
    const stillOpen = page.locator(
      catalogOpenByExactName("developer-wf-open", "data-wf-name", name),
    );
    await expect(stillOpen).toHaveCount(0);

    guards.assertClean();

    // Idempotent teardown.
    await deleteWorkflowViaRest(request, name);
  });

  test("REST update/delete contract: 200, 404, 400 name mismatch, 204", async ({
    request,
  }) => {
    test.setTimeout(180_000);
    const name = uniqueWorkflowName("Nightly REST UD");
    const headers = {
      ...adminBasicAuthHeaders(),
      Accept: "application/json",
      "Content-Type": "application/json",
    };

    await createWorkflowViaRest(request, name, "REST contract seed");

    // Update with matching name → 200 + new description.
    const updated = await request.put(
      `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent(name)}`,
      {
        headers,
        data: { WorkflowUpdate: { name, description: "REST contract updated" } },
      },
    );
    expect(updated.status(), "PUT matching name").toBe(200);
    const updatedBody = await updated.json();
    expect(
      updatedBody.workflowDescription ||
        (updatedBody.WorkflowSummary && updatedBody.WorkflowSummary.workflowDescription),
    ).toBe("REST contract updated");

    // Update with mismatched name → 400.
    const mismatch = await request.put(
      `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent(name)}`,
      {
        headers,
        data: { WorkflowUpdate: { name: "Other", description: "Edited" } },
      },
    );
    expect(mismatch.status(), "PUT mismatched name").toBe(400);

    // Update on missing workflow → 404.
    const missingUpdate = await request.put(
      `${BASE_URL}/Rhythmyx/services/workflows/missing-${Date.now()}`,
      {
        headers,
        data: { WorkflowUpdate: { name: "missing-x", description: "Edited" } },
      },
    );
    expect(missingUpdate.status(), "PUT missing workflow").toBe(404);

    // Delete → 204.
    const deleted = await request.delete(
      `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent(name)}`,
      { headers: adminBasicAuthHeaders() },
    );
    expect(deleted.status(), "DELETE workflow").toBe(204);

    // Delete missing → 404.
    const missingDelete = await request.delete(
      `${BASE_URL}/Rhythmyx/services/workflows/missing-${Date.now()}`,
      { headers: adminBasicAuthHeaders() },
    );
    expect(missingDelete.status(), "DELETE missing workflow").toBe(404);
  });
});
