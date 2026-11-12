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
 * Workflow Administration - Site/Folder Assignment Dialog (993 US2 / P1).
 *
 * <p>Dedicated E2E for "Assign Workflows to Sites and Folders": from a saved
 * workflow's editor the {@code open-site-assign-button} opens the
 * {@code WorkflowSiteAssign} dialog, a site is selected, the assignment job
 * is started, and the job progress overlay is polled to a terminal state.</p>
 *
 * <p>The dialog's job service calls are stubbed at the network layer (the legacy
 * {@code GetAssociatedFoldersJob/start} / {@code isInProgress} endpoints) so
 * the run is deterministic and never reassigns a real folder's workflow on
 * the shared QA cell. The site catalog stays live, which proves the #6157
 * {@code SiteSummary}-envelope unwrap fix against the real payload.
 * Navigation (shell &rarr; workflow list &rarr; saved workflow editor &rarr;
 * dialog) is live, which is the reachability gap this spec closes. When no
 * saved workflow exists, the spec creates a uniquely named one first and
 * deletes it afterwards (fixture with cleanup).</p>
 *
 * <p>Tags: {@code @workflow-admin} {@code @administration}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/us2-site-folder-assignment.spec.js}</p>
 *
 * <p>Coverage map: {@code specs/993-workflow-admin-react-ui/e2e-coverage-map.md}
 * (note 1)</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

test.describe("Workflow Administration - Site/Folder Assignment (993 US2)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(120_000);
    await loginAsAdmin(page);
  });

  function collectPageErrors(page) {
    const pageErrors = [];
    page.on("pageerror", (err) => pageErrors.push(String(err)));
    page.on("console", (msg) => {
      if (msg.type() === "error") {
        pageErrors.push(msg.text());
      }
    });
    return pageErrors;
  }

  async function gotoWorkflowList(page) {
    // Legacy view=workflow → unified Admin workflow tab (#3088)
    await page.goto(`${BASE_URL}/cm/app/index.jsp?view=workflow`);
    await expect(page.locator("[data-testid='perc-admin-shell']")).toBeVisible({
      timeout: 30_000,
    });
    // #2959: wrapper payload must not trip RouteErrorBoundary (e.map is not a function)
    await expect(page.locator("[data-testid='route-error']")).toHaveCount(0);
    await expect(page.getByText(/Unable to load Admin/i)).toHaveCount(0);
    await expect(
      page.locator("[data-testid='perc-workflow-section']"),
    ).toBeVisible({ timeout: 30_000 });
  }

  /**
   * Returns the name of a saved workflow to edit: the first listed row, or a
   * freshly created uniquely-named workflow when the list is empty.
   *
   * @returns {{name: string, created: boolean}}
   */
  async function ensureSavedWorkflow(page) {
    const rows = page.locator("[data-testid^='workflow-row-']");
    // Either at least one row or the empty-table message must render.
    await expect(async () => {
      const count = await rows.count();
      const empty = await page.getByText(/No workflows found/i).count();
      expect(count + empty).toBeGreaterThan(0);
    }).toPass({ timeout: 30_000 });

    if ((await rows.count()) > 0) {
      const rowId = await rows.first().getAttribute("data-testid");
      return { name: String(rowId).replace(/^workflow-row-/, ""), created: false };
    }

    const name = `E2E Assign Fixture ${Date.now()}`;
    await page.locator("[data-testid='create-workflow-button']").click();
    await expect(page.locator("[data-testid='perc-workflow-editor']")).toBeVisible({
      timeout: 30_000,
    });
    await page.locator("[data-testid='workflow-name-input']").fill(name);
    await page.locator("[data-testid='save-workflow-button']").click();
    await expect(
      page.locator(`[data-testid='workflow-row-${name}']`),
    ).toBeVisible({ timeout: 30_000 });
    return { name, created: true };
  }

  async function deleteWorkflow(page, name) {
    await page.goto(`${BASE_URL}/cm/app/index.jsp?view=workflow`);
    await expect(
      page.locator(`[data-testid='workflow-row-${name}']`),
    ).toBeVisible({ timeout: 30_000 });
    page.once("dialog", (dialog) => dialog.accept());
    await page.locator(`[data-testid='delete-wf-${name}']`).click();
    await expect(
      page.locator(`[data-testid='workflow-row-${name}']`),
    ).toHaveCount(0, { timeout: 30_000 });
  }

  async function openAssignDialog(page, workflowName) {
    await page.locator(`[data-testid='edit-wf-${workflowName}']`).click();
    await expect(page.locator("[data-testid='perc-workflow-editor']")).toBeVisible({
      timeout: 30_000,
    });
    // Assign entry point renders for saved workflows only.
    const assignBtn = page.locator("[data-testid='open-site-assign-button']");
    await expect(assignBtn).toBeVisible({ timeout: 30_000 });
    await assignBtn.click();
    const dialog = page.locator("[data-testid='perc-workflow-site-assign']");
    await expect(dialog).toBeVisible({ timeout: 30_000 });
    return dialog;
  }

  test(
    "saved workflow editor opens the assign dialog and the job reaches a terminal state",
    { tag: ["@workflow-admin", "@administration"] },
    async ({ page }) => {
      const pageErrors = collectPageErrors(page);

      // The dialog's job service calls are stubbed at the network layer (the
      // legacy {@code GetAssociatedFoldersJob/start} / {@code isInProgress}
      // endpoints) so the run is deterministic and never reassigns a real
      // folder's workflow on the shared QA cell. The site catalog
      // ({@code SITES_ALL}) stays LIVE: it returns the Jackson WRAP_ROOT
      // {@code { SiteSummary: [...] }} envelope, which proves the #6157
      // unwrap fix end to end. First poll tick (t+1s) reports running; later
      // ticks report finished.
      let statusCalls = 0;
      await page.route(
        "**/services/foldermanagement/GetAssociatedFoldersJob/start/**",
        (route) =>
          route.fulfill({
            status: 200,
            contentType: "application/json",
            body: "{}",
          }),
      );
      await page.route(
        "**/services/foldermanagement/workflowassignment/isInProgress**",
        (route) => {
          statusCalls += 1;
          return route.fulfill({
            status: 200,
            contentType: "application/json",
            body: JSON.stringify({ isInProgress: statusCalls < 2 }),
          });
        },
      );

      await gotoWorkflowList(page);
      const fixture = await ensureSavedWorkflow(page);
      try {
        const dialog = await openAssignDialog(page, fixture.name);

        const siteSelect = dialog.locator("[data-testid='site-select']");
        await expect(siteSelect).toBeVisible({ timeout: 30_000 });
        // Placeholder option + live seed sites (H2 ships Corporate and
        // Enterprise Investments). The pre-#6157 code crashed here because
        // the live payload is a { SiteSummary: [...] } envelope.
        await expect(siteSelect.locator("option")).not.toHaveCount(1, {
          timeout: 30_000,
        });

        await siteSelect.selectOption({ index: 1 });
        const startBtn = dialog.locator(
          "[data-testid='start-assignment-button']",
        );
        await expect(startBtn).toBeEnabled();
        await startBtn.click();

        // Progress overlay appears immediately, then polling resolves it.
        const statusMsg = dialog.locator("[data-testid='job-status-msg']");
        await expect(statusMsg).toBeVisible({ timeout: 10_000 });
        await expect(statusMsg).toContainText(/in progress/i, {
          timeout: 10_000,
        });
        await expect(statusMsg).toContainText(/assigned successfully/i, {
          timeout: 30_000,
        });
        expect(statusCalls).toBeGreaterThan(0);
      } finally {
        if (fixture.created) {
          await deleteWorkflow(page, fixture.name);
        }
      }

      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual(
        [],
      );
    },
  );

  test(
    "assign dialog cancel closes without starting a job",
    { tag: ["@workflow-admin", "@administration"] },
    async ({ page }) => {
      const pageErrors = collectPageErrors(page);

      let startCalls = 0;
      await page.route(
        "**/services/foldermanagement/GetAssociatedFoldersJob/start/**",
        (route) => {
          startCalls += 1;
          return route.fulfill({
            status: 200,
            contentType: "application/json",
            body: "{}",
          });
        },
      );

      await gotoWorkflowList(page);
      const fixture = await ensureSavedWorkflow(page);
      try {
        const dialog = await openAssignDialog(page, fixture.name);
        // Wait for the site catalog fetch to settle: the dialog re-renders
        // when loading completes, which would detach a mid-click target.
        await expect(dialog.locator("[data-testid='site-select']")).toBeVisible({
          timeout: 30_000,
        });
        await dialog
          .getByRole("button", { name: /cancel/i })
          .first()
          .click();
        await expect(dialog).toHaveCount(0, { timeout: 10_000 });
        expect(startCalls).toEqual(0);
      } finally {
        if (fixture.created) {
          await deleteWorkflow(page, fixture.name);
        }
      }

      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual(
        [],
      );
    },
  );
});
