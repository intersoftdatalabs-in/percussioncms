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
 * Developer workflows catalog readback (#5917 / parent #1690, slice 140).
 *
 * Proves the workflow list plus step/detail renders from the thin-read API
 * in the React shell: the catalog table reads back through
 * GET /services/workflowmanagement/workflows/metadata (name, description,
 * default flag, step count — no step, transition, or role payload), and the
 * detail panel reads back through
 * GET /services/workflowmanagement/workflows/{name} (title, default flag,
 * description, step list with state/permissions/roles/transitions).
 *
 * Test 1 is zero-write: it opens the packaged Simple Workflow row and
 * asserts list + step/detail readback plus thin-read REST probes for the
 * same row. Test 2 creates one workflow through the shell, proves
 * name/description/steps readback in the catalog and detail, then removes
 * it with REST DELETE cleanup and proves catalog omission.
 *
 * Readback only, no step mutation; out of scope: slices 139/141.
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… npm run test:surface -- --path tests/developer-workflows-readback.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const {
  catalogOpenByExactName,
  catalogRowSelector,
} = require("./helpers/developer-catalog-selectors");

/**
 * Packaged stock workflow on the H2 seed (verified against the thin-read
 * metadata payload: {"Workflow":[{workflowName, workflowDescription,
 * defaultWorkflow, ...}]} with no step payload). Steps come from the
 * detail GET; its Draft step carries Publish/Approve/Submit transitions.
 */
const PACKAGED_WORKFLOW = "Default Workflow";

function developerWorkflowsUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "workflows",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

/** REST-safe unique workflow name (letters/digits/spaces only, max 50). */
function uniqueWorkflowName(label) {
  const a = Date.now().toString(36).replace(/[^a-z0-9]/g, "").slice(-4);
  const b = Math.random().toString(36).replace(/[^a-z0-9]/g, "").slice(2, 6);
  const suffix = `${a}${b}`.slice(0, 8);
  return `QA 5917 ${label} ${suffix || "wf"}`.slice(0, 50);
}

/**
 * Same-origin fetch so OWASP CSRF + session cookies apply.
 *
 * @param {import("@playwright/test").Page} page
 * @param {string} path
 * @param {string} method
 * @param {object} [body]
 * @returns {Promise<{status: number, text: string}>}
 */
async function inPageJson(page, path, method, body) {
  return page.evaluate(
    async ({ path: url, method: httpMethod, body: payload }) => {
      const tokenObj = window.OWASP_CSRFTOKEN;
      const metaToken = document.querySelector('meta[name="_csrf"]');
      const metaHeader = document.querySelector('meta[name="_csrf_header"]');
      const token =
        (tokenObj && tokenObj.token) || (metaToken && metaToken.content) || "";
      const headerName =
        (metaHeader && metaHeader.content) || "OWASP-CSRFTOKEN";
      const headers = {
        Accept: "application/json",
        "Content-Type": "application/json",
      };
      if (token) {
        headers[headerName] = token;
      }
      const res = await fetch(url, {
        method: httpMethod,
        credentials: "same-origin",
        headers,
        body: payload === undefined ? undefined : JSON.stringify(payload),
      });
      const text = await res.text();
      return { status: res.status, text };
    },
    { path, method, body },
  );
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
  return { pageErrors, consoleErrors };
}

function assertConsoleClean(pageErrors, consoleErrors) {
  expect(pageErrors, `pageerror: ${pageErrors.join(" | ")}`).toEqual([]);
  const unexpectedConsole = consoleErrors.filter(
    (t) => !/Failed to load resource/i.test(t) && !/favicon/i.test(t),
  );
  expect(
    unexpectedConsole,
    `console error: ${unexpectedConsole.join(" | ")}`,
  ).toEqual([]);
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
  if (await empty.isVisible()) {
    throw new Error("Developer workflows catalog is empty — no packaged row to read back");
  }
  await expect(panel).toBeVisible({ timeout: 15_000 });
}

test.describe("Developer workflows catalog readback (#5917)", () => {
  test("packaged workflow list and step/detail read back from the thin-read API", async ({
    page,
  }) => {
    test.setTimeout(180_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);
    await openWorkflowsCatalog(page);

    // Thin-read catalog: packaged row renders the open button + thin cells.
    const firstRow = page.locator(catalogRowSelector("developer-wf-row", 0));
    await expect(firstRow).toBeVisible({ timeout: 15_000 });
    const packagedOpen = page.locator(
      catalogOpenByExactName("developer-wf-open", "data-wf-name", PACKAGED_WORKFLOW),
    );
    await expect(packagedOpen).toBeVisible({ timeout: 15_000 });

    // Thin-read REST probe: metadata list contains the packaged row.
    const probe = await inPageJson(
      page,
      "/Rhythmyx/services/workflowmanagement/workflows/metadata",
      "GET",
    );
    expect(
      probe.status,
      `thin-read metadata should be 200 (got ${probe.status}): ${probe.text.slice(0, 200)}`,
    ).toBe(200);
    expect(
      probe.text,
      "thin-read list contains the packaged workflow",
    ).toContain(PACKAGED_WORKFLOW);

    // Thin-read detail probe for the same row.
    const detailProbe = await inPageJson(
      page,
      `/Rhythmyx/services/workflowmanagement/workflows/${encodeURIComponent(PACKAGED_WORKFLOW)}`,
      "GET",
    );
    expect(
      detailProbe.status,
      `thin-read detail should be 200 (got ${detailProbe.status}): ${detailProbe.text.slice(0, 200)}`,
    ).toBe(200);
    expect(detailProbe.text, "thin-read detail names the workflow").toContain(
      PACKAGED_WORKFLOW,
    );

    // Detail readback in the shell: title + step list.
    await packagedOpen.click();
    await expect(page.locator('[data-testid="developer-wf-detail"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(
      page.locator('[data-testid="developer-wf-detail-title"]'),
    ).toContainText(PACKAGED_WORKFLOW, { timeout: 20_000 });
    await expect(page.locator('[data-testid="developer-wf-steps"]')).toBeVisible();
    const stepsTable = page.locator('[data-testid="developer-wf-steps-table"]');
    await expect(stepsTable).toBeVisible({ timeout: 20_000 });
    const stepRows = page.locator('[data-testid^="developer-wf-step-row-"]');
    const stepCount = await stepRows.count();
    expect(stepCount, "packaged workflow reads back at least one step").toBeGreaterThan(0);
    await expect(page.locator('[data-testid="developer-wf-step-name-0"]')).toHaveText(
      "Draft",
    );
    // Transitions column previews stepRoles[].roleTransitions (Workbench-style
    // transitionPermission): the Draft step allows Approve among others.
    await expect(
      page.locator('[data-testid="developer-wf-step-transitions-0"]'),
    ).toContainText("Approve");

    assertConsoleClean(pageErrors, consoleErrors);
  });

  test("created workflow name, description, and steps read back in the shell", async ({
    page,
  }) => {
    test.setTimeout(180_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);
    await openWorkflowsCatalog(page);

    const name = uniqueWorkflowName("readback");
    const description = "qa5917 workflows catalog readback";

    // Shell create (POST /services/workflows under the WorkflowCreate root).
    await page.locator('[data-testid="developer-wf-new"]').click();
    await expect(page.locator('[data-testid="developer-wf-create"]')).toBeVisible();
    await page.locator('[data-testid="developer-wf-create-name"]').fill(name);
    await page
      .locator('[data-testid="developer-wf-create-description"]')
      .fill(description);
    await page.locator('[data-testid="developer-wf-create-save"]').click();

    // handleCreated opens the new row: title + template steps read back.
    const detail = page.locator('[data-testid="developer-wf-detail"]');
    const notice = page.locator('[data-testid="developer-wf-create-notice"]');
    await expect(detail.or(notice).first()).toBeVisible({ timeout: 30_000 });
    await expect(detail).toBeVisible({ timeout: 30_000 });
    await expect(page.locator('[data-testid="developer-wf-detail-title"]')).toContainText(
      name,
    );
    // The thin-read detail GET carries steps but no description (server
    // behavior: workflowDescription is "" on the detail payload while the
    // metadata list carries it), so the description input stays empty here.
    // Description readback is proven on the catalog row below.
    await expect(
      page.locator('[data-testid="developer-wf-description-editor"]'),
    ).toBeVisible();
    const createdStepsTable = page.locator('[data-testid="developer-wf-steps-table"]');
    await expect(createdStepsTable).toBeVisible({ timeout: 20_000 });
    await expect(createdStepsTable).toContainText("Draft");

    // Catalog readback: row for the created workflow with its description.
    await page.locator('[data-testid="developer-wf-back"]').click();
    const openSel = catalogOpenByExactName("developer-wf-open", "data-wf-name", name);
    await expect(page.locator(openSel)).toBeVisible({ timeout: 30_000 });
    const row = page.locator(`tr:has(${openSel})`);
    await expect(row).toContainText(description, { timeout: 20_000 });

    // REST cleanup: DELETE the created workflow, then prove catalog omission.
    const del = await inPageJson(
      page,
      `/Rhythmyx/services/workflows/${encodeURIComponent(name)}`,
      "DELETE",
    );
    expect(
      [200, 204].includes(del.status),
      `DELETE cleanup should be 200/204 (got ${del.status}): ${del.text.slice(0, 200)}`,
    ).toBeTruthy();

    await openWorkflowsCatalog(page);
    await expect(page.locator(openSel)).toHaveCount(0);

    assertConsoleClean(pageErrors, consoleErrors);
  });
});
