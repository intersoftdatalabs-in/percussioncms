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
 * Developer → Workflows: add one system-field aging transition (#5243 / parent #1690).
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… TEST_DB_TYPE=h2 TEST_PRODUCT=cms \
 *     npm run test:surface -- --path tests/developer-workflow-system-field-aging.spec.js
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

function systemFieldEdge(page) {
  return page.locator('[data-testid^="developer-wf-aging-edge-"][data-aging-type="SYSTEM_FIELD"]');
}

/** New custom workflows copy the standard template, which already has system-field aging. */
function draftReviewSystemField(page, field) {
  return page.locator(
    `[data-testid^="developer-wf-aging-edge-"][data-aging-type="SYSTEM_FIELD"][data-from="Draft"][data-to="Review"][data-system-field="${field}"]`,
  );
}

function absoluteEdge(page) {
  return page.locator('[data-testid^="developer-wf-aging-edge-"][data-aging-type="ABSOLUTE"]');
}

function repeatedEdge(page) {
  return page.locator('[data-testid^="developer-wf-aging-edge-"][data-aging-type="REPEATED"]');
}

function countSystemFieldPosts(page) {
  let posts = 0;
  page.on("request", (req) => {
    if (req.method() !== "POST" || !req.url().includes("/aging-transitions")) {
      return;
    }
    const body = req.postData() || "";
    if (body.includes("SYSTEM_FIELD")) {
      posts += 1;
    }
  });
  return () => posts;
}

test.describe("Developer system-field aging transition (#5243)", () => {
  test("save lists the system-field row only after success and keeps absolute and repeated rows", async ({
    page,
    request,
  }) => {
    test.setTimeout(180_000);
    const guards = attachConsoleGuards(page);
    const posts = countSystemFieldPosts(page);
    const name = uniqueWorkflowName("Nightly SysField");
    const headers = jsonHeaders();
    const base = `${BASE_URL}/Rhythmyx/services/workflows`;
    const created = await request.post(base, {
      headers,
      data: { WorkflowCreate: { name, description: "slice 76" } },
    });
    expect(created.status(), await created.text()).toBe(200);
    const absolute = await request.post(`${base}/${encodeURIComponent(name)}/aging-transitions`, {
      headers,
      data: {
        WorkflowAgingTransitionWrite: { from: "Draft", to: "Review", intervalMinutes: 15 },
      },
    });
    expect(absolute.status(), await absolute.text()).toBe(200);
    const repeated = await request.post(`${base}/${encodeURIComponent(name)}/aging-transitions`, {
      headers,
      data: {
        WorkflowAgingTransitionWrite: {
          from: "Draft",
          to: "Review",
          intervalMinutes: 20,
          type: "REPEATED",
        },
      },
    });
    expect(repeated.status(), await repeated.text()).toBe(200);

    await loginAsAdmin(page);
    await openWorkflowsCatalog(page);
    await page
      .locator(catalogOpenByExactName("developer-wf-open", "data-wf-name", name))
      .click();
    await expect(page.locator('[data-testid="developer-wf-graph-kind"]')).toContainText(
      /Custom workflow/i,
      { timeout: 30_000 },
    );
    await expect(page.locator('[data-testid="developer-wf-system-field-aging-form"]')).toBeVisible();
    await expect(absoluteEdge(page)).toBeVisible({ timeout: 30_000 });
    await expect(repeatedEdge(page)).toBeVisible();
    const templateSystemFields = systemFieldEdge(page);
    await expect(templateSystemFields.first()).toBeVisible();
    const baselineSystemFields = await templateSystemFields.count();
    const createdEdge = draftReviewSystemField(page, "CONTENTSTARTDATE");
    await expect(createdEdge).toHaveCount(0);

    await page.locator('[data-testid="developer-wf-system-field-aging-field"]').selectOption("CONTENTSTARTDATE");
    await page.locator('[data-testid="developer-wf-system-field-aging-cancel"]').click();
    expect(posts()).toBe(0);
    await expect(page.locator('[data-testid="developer-wf-system-field-aging-field"]')).toHaveValue("");
    await expect(createdEdge).toHaveCount(0);
    await expect(systemFieldEdge(page)).toHaveCount(baselineSystemFields);
    await expect(absoluteEdge(page)).toBeVisible();
    await expect(repeatedEdge(page)).toBeVisible();

    await page.locator('[data-testid="developer-wf-system-field-aging-field"]').selectOption("CONTENTSTARTDATE");
    await page.locator('[data-testid="developer-wf-system-field-aging-add"]').click();
    await expect(page.locator('[data-testid="developer-wf-graph-error"]')).toBeVisible();
    expect(posts()).toBe(0);
    await expect(createdEdge).toHaveCount(0);
    await expect(systemFieldEdge(page)).toHaveCount(baselineSystemFields);

    await page.locator('[data-testid="developer-wf-system-field-aging-from"]').selectOption("Draft");
    await page.locator('[data-testid="developer-wf-system-field-aging-to"]').selectOption("Review");
    await page.locator('[data-testid="developer-wf-system-field-aging-field"]').selectOption("");
    await page.locator('[data-testid="developer-wf-system-field-aging-add"]').click();
    await expect(page.locator('[data-testid="developer-wf-graph-error"]')).toBeVisible();
    expect(posts()).toBe(0);
    await expect(createdEdge).toHaveCount(0);
    await expect(systemFieldEdge(page)).toHaveCount(baselineSystemFields);
    await expect(absoluteEdge(page)).toBeVisible();
    await expect(repeatedEdge(page)).toBeVisible();

    await page.locator('[data-testid="developer-wf-system-field-aging-field"]').selectOption("CONTENTSTARTDATE");
    await page.locator('[data-testid="developer-wf-system-field-aging-add"]').click();
    await expect(createdEdge).toBeVisible({ timeout: 30_000 });
    await expect(createdEdge).toContainText("System field aging CONTENTSTARTDATE");
    await expect(createdEdge).toContainText("system field");
    await expect(createdEdge).toHaveAttribute("data-from", "Draft");
    await expect(createdEdge).toHaveAttribute("data-to", "Review");
    await expect(createdEdge).toHaveAttribute("data-aging-type", "SYSTEM_FIELD");
    await expect(createdEdge).toHaveAttribute("data-system-field", "CONTENTSTARTDATE");
    await expect(absoluteEdge(page)).toBeVisible();
    await expect(repeatedEdge(page)).toBeVisible();
    await expect(page.locator('[data-testid="developer-wf-graph-notice"]')).toContainText(
      /System-field aging transition saved/i,
    );
    await expect(systemFieldEdge(page)).toHaveCount(baselineSystemFields + 1);
    expect(posts()).toBe(1);

    await page.locator('[data-testid="developer-wf-back"]').click();
    await expect(page.locator('[data-testid="developer-wf-table"]')).toBeVisible({
      timeout: 30_000,
    });
    await page
      .locator(catalogOpenByExactName("developer-wf-open", "data-wf-name", name))
      .click();
    await expect(draftReviewSystemField(page, "CONTENTSTARTDATE")).toBeVisible({ timeout: 30_000 });
    await expect(draftReviewSystemField(page, "CONTENTSTARTDATE")).toContainText(
      "System field aging CONTENTSTARTDATE",
    );
    await expect(absoluteEdge(page)).toBeVisible();
    await expect(repeatedEdge(page)).toBeVisible();
    guards.assertClean();
  });

  test("HTTP 400, 403, and 409 leave the graph without the system-field edge", async ({
    page,
    request,
  }) => {
    test.setTimeout(180_000);
    const guards = attachConsoleGuards(page);
    const name = uniqueWorkflowName("Nightly SysFieldErr");
    const headers = jsonHeaders();
    const base = `${BASE_URL}/Rhythmyx/services/workflows`;
    const created = await request.post(base, {
      headers,
      data: { WorkflowCreate: { name, description: "slice 76 errors" } },
    });
    expect(created.status(), await created.text()).toBe(200);
    const absolute = await request.post(`${base}/${encodeURIComponent(name)}/aging-transitions`, {
      headers,
      data: {
        WorkflowAgingTransitionWrite: { from: "Draft", to: "Review", intervalMinutes: 15 },
      },
    });
    expect(absolute.status(), await absolute.text()).toBe(200);
    const repeated = await request.post(`${base}/${encodeURIComponent(name)}/aging-transitions`, {
      headers,
      data: {
        WorkflowAgingTransitionWrite: {
          from: "Draft",
          to: "Review",
          intervalMinutes: 20,
          type: "REPEATED",
        },
      },
    });
    expect(repeated.status(), await repeated.text()).toBe(200);

    await page.route("**/aging-transitions**", async (route) => {
      const body = route.request().postData() || "";
      if (route.request().method() !== "POST" || !body.includes("SYSTEM_FIELD")) {
        await route.continue();
        return;
      }
      const status = body.includes("CONTENTEXPIRYDATE")
        ? 403
        : body.includes("REMINDERDATE")
          ? 409
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
    await expect(page.locator('[data-testid="developer-wf-system-field-aging-form"]')).toBeVisible({
      timeout: 30_000,
    });
    await expect(absoluteEdge(page)).toBeVisible();
    await expect(repeatedEdge(page)).toBeVisible();
    const baselineSystemFields = await systemFieldEdge(page).count();
    expect(baselineSystemFields).toBeGreaterThan(0);
    await page.locator('[data-testid="developer-wf-system-field-aging-from"]').selectOption("Draft");
    await page.locator('[data-testid="developer-wf-system-field-aging-to"]').selectOption("Review");
    for (const field of ["CONTENTSTARTDATE", "CONTENTEXPIRYDATE", "REMINDERDATE"]) {
      await page.locator('[data-testid="developer-wf-system-field-aging-field"]').selectOption(field);
      await page.locator('[data-testid="developer-wf-system-field-aging-add"]').click();
      await expect(page.locator('[data-testid="developer-wf-graph-error"]')).toBeVisible();
      await expect(page.locator('[data-testid="developer-wf-graph-notice"]')).toHaveCount(0);
      await expect(draftReviewSystemField(page, field)).toHaveCount(0);
      await expect(systemFieldEdge(page)).toHaveCount(baselineSystemFields);
      await expect(absoluteEdge(page)).toBeVisible();
      await expect(repeatedEdge(page)).toBeVisible();
    }
    guards.assertClean();
  });

  test("packaged workflow has no system-field form; REST rejects a blank field and keeps other rows", async ({
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
            type: "SYSTEM_FIELD",
            systemField: "CONTENTSTARTDATE",
          },
        },
      },
    );
    expect(forbidden.status()).toBe(403);

    const missing = await request.post(
      `${base}/${encodeURIComponent("No Such Workflow 5243")}/aging-transitions`,
      {
        headers,
        data: {
          WorkflowAgingTransitionWrite: {
            from: "Draft",
            to: "Review",
            type: "SYSTEM_FIELD",
            systemField: "CONTENTSTARTDATE",
          },
        },
      },
    );
    expect(missing.status()).toBe(404);

    const name = uniqueWorkflowName("Nightly SysFieldDup");
    const created = await request.post(base, {
      headers,
      data: { WorkflowCreate: { name, description: "slice 76 dup" } },
    });
    expect(created.status(), await created.text()).toBe(200);

    const blank = await request.post(`${base}/${encodeURIComponent(name)}/aging-transitions`, {
      headers,
      data: {
        WorkflowAgingTransitionWrite: {
          from: "Draft",
          to: "Review",
          type: "SYSTEM_FIELD",
          systemField: " ",
        },
      },
    });
    expect(blank.status()).toBe(400);

    const unknown = await request.post(`${base}/${encodeURIComponent(name)}/aging-transitions`, {
      headers,
      data: {
        WorkflowAgingTransitionWrite: {
          from: "Draft",
          to: "Review",
          type: "SYSTEM_FIELD",
          systemField: "sys_title",
        },
      },
    });
    expect(unknown.status()).toBe(400);

    const absolute = await request.post(`${base}/${encodeURIComponent(name)}/aging-transitions`, {
      headers,
      data: {
        WorkflowAgingTransitionWrite: { from: "Draft", to: "Review", intervalMinutes: 15 },
      },
    });
    expect(absolute.status(), await absolute.text()).toBe(200);
    const repeated = await request.post(`${base}/${encodeURIComponent(name)}/aging-transitions`, {
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
    expect(repeated.status(), await repeated.text()).toBe(200);

    const added = await request.post(`${base}/${encodeURIComponent(name)}/aging-transitions`, {
      headers,
      data: {
        WorkflowAgingTransitionWrite: {
          from: "Draft",
          to: "Review",
          type: "SYSTEM_FIELD",
          systemField: "CONTENTSTARTDATE",
        },
      },
    });
    const addedBody = await added.text();
    expect(added.status(), addedBody).toBe(200);
    expect(addedBody).toMatch(/"agingType"\s*:\s*"ABSOLUTE"/);
    expect(addedBody).toMatch(/"agingType"\s*:\s*"REPEATED"/);
    expect(addedBody).toMatch(/"agingType"\s*:\s*"SYSTEM_FIELD"/);
    expect(addedBody).toMatch(/"systemField"\s*:\s*"CONTENTSTARTDATE"/);
    expect(addedBody).toMatch(/System field aging CONTENTSTARTDATE/);

    const dup = await request.post(`${base}/${encodeURIComponent(name)}/aging-transitions`, {
      headers,
      data: {
        WorkflowAgingTransitionWrite: {
          from: "Draft",
          to: "Review",
          type: "SYSTEM_FIELD",
          systemField: "contentstartdate",
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
    await expect(page.locator('[data-testid="developer-wf-system-field-aging-form"]')).toHaveCount(0);
    guards.assertClean();
  });
});
