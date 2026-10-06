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
 * Developer → Workflows: change the date column on one system-field aging transition (#5277 / parent #1690).
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… TEST_DB_TYPE=h2 TEST_PRODUCT=cms \
 *     npm run test:surface -- --path tests/developer-workflow-system-field-aging-column.spec.js
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

function repeatedEdge(page) {
  return page.locator('[data-testid^="developer-wf-aging-edge-"][data-aging-type="REPEATED"]');
}

function absoluteEdge(page) {
  return page.locator(
    '[data-testid^="developer-wf-aging-edge-"][data-aging-type="ABSOLUTE"][data-from="Draft"][data-to="Review"][data-interval="15"]',
  );
}

function systemFieldEdge(page, field) {
  return page.locator(
    `[data-testid^="developer-wf-aging-edge-"][data-aging-type="SYSTEM_FIELD"][data-from="Draft"][data-to="Review"][data-system-field="${field}"]`,
  );
}

function watchColumnPuts(page) {
  const bodies = [];
  page.on("request", (req) => {
    if (req.method() === "PUT" && req.url().includes("/aging-transitions/system-field")) {
      bodies.push(req.postData() || "");
    }
  });
  return bodies;
}

async function createCustomWorkflow(request, name) {
  const headers = jsonHeaders();
  const base = `${BASE_URL}/Rhythmyx/services/workflows`;
  const created = await request.post(base, {
    headers,
    data: { WorkflowCreate: { name, description: "slice 79" } },
  });
  expect(created.status(), await created.text()).toBe(200);
  return base;
}

async function postAging(request, base, name, body) {
  const response = await request.post(`${base}/${encodeURIComponent(name)}/aging-transitions`, {
    headers: jsonHeaders(),
    data: { WorkflowAgingTransitionWrite: body },
  });
  expect(response.status(), await response.text()).toBe(200);
}

test.describe("Developer change a system-field date column (#5277)", () => {
  test("save shows the new column only after success and leaves the other rows", async ({
    page,
    request,
  }) => {
    test.setTimeout(180_000);
    const guards = attachConsoleGuards(page);
    const puts = watchColumnPuts(page);
    const name = uniqueWorkflowName("Nightly SysCol");
    const base = await createCustomWorkflow(request, name);
    await postAging(request, base, name, { from: "Draft", to: "Review", intervalMinutes: 15 });
    await postAging(request, base, name, {
      from: "Draft",
      to: "Review",
      intervalMinutes: 15,
      type: "REPEATED",
    });
    await postAging(request, base, name, {
      from: "Draft",
      to: "Review",
      type: "SYSTEM_FIELD",
      systemField: "CONTENTSTARTDATE",
    });

    await loginAsAdmin(page);
    await openWorkflowsCatalog(page);
    await page
      .locator(catalogOpenByExactName("developer-wf-open", "data-wf-name", name))
      .click();
    const systemField = systemFieldEdge(page, "CONTENTSTARTDATE");
    const absolute = absoluteEdge(page);
    const repeated = repeatedEdge(page);
    await expect(systemField).toBeVisible({ timeout: 30_000 });
    await expect(absolute).toBeVisible();
    await expect(repeated).toBeVisible();
    await expect(systemField).toHaveAttribute("data-system-field", "CONTENTSTARTDATE");
    await expect(systemField.locator('[data-testid^="developer-wf-aging-change-"]')).toHaveCount(0);
    await expect(systemField.locator('[data-testid^="developer-wf-aging-field-change-"]')).toHaveCount(1);
    await expect(absolute.locator('[data-testid^="developer-wf-aging-change-"]')).toHaveCount(1);
    await expect(absolute.locator('[data-testid^="developer-wf-aging-field-change-"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-wf-aging-field-form"]')).toHaveCount(0);

    await systemField.locator('[data-testid^="developer-wf-aging-field-change-"]').click();
    await systemField.locator('[data-testid="developer-wf-aging-new-field"]').selectOption("REMINDERDATE");
    await systemField.locator('[data-testid="developer-wf-aging-field-cancel"]').click();
    expect(puts).toHaveLength(0);
    await expect(page.locator('[data-testid="developer-wf-aging-field-form"]')).toHaveCount(0);
    await expect(systemField).toHaveAttribute("data-system-field", "CONTENTSTARTDATE");
    await expect(absolute).toHaveAttribute("data-interval", "15");

    await systemField.locator('[data-testid^="developer-wf-aging-field-change-"]').click();
    await systemField.locator('[data-testid="developer-wf-aging-field-save"]').click();
    await expect(page.locator('[data-testid="developer-wf-graph-error"]')).toBeVisible();
    await systemField.locator('[data-testid="developer-wf-aging-new-field"]').selectOption("CONTENTSTARTDATE");
    await systemField.locator('[data-testid="developer-wf-aging-field-save"]').click();
    await expect(page.locator('[data-testid="developer-wf-graph-error"]')).toBeVisible();
    expect(puts).toHaveLength(0);
    await expect(page.locator('[data-testid="developer-wf-graph-notice"]')).toHaveCount(0);
    await expect(systemField).toContainText("CONTENTSTARTDATE");
    await expect(repeated).toHaveAttribute("data-interval", "15");

    await systemField.locator('[data-testid="developer-wf-aging-new-field"]').selectOption("CONTENTEXPIRYDATE");
    await systemField.locator('[data-testid="developer-wf-aging-field-save"]').click();
    const updated = systemFieldEdge(page, "CONTENTEXPIRYDATE");
    await expect(updated).toBeVisible({ timeout: 30_000 });
    await expect(updated).toContainText("System field aging CONTENTEXPIRYDATE");
    await expect(updated).toContainText("system field");
    await expect(updated).toHaveAttribute("data-aging-type", "SYSTEM_FIELD");
    await expect(systemFieldEdge(page, "CONTENTSTARTDATE")).toHaveCount(0);
    await expect(absolute).toHaveAttribute("data-interval", "15");
    await expect(absolute).toContainText("Aging 15");
    await expect(repeated).toHaveAttribute("data-interval", "15");
    await expect(repeated).toContainText("Repeated aging 15");
    await expect(page.locator('[data-testid="developer-wf-graph-notice"]')).toContainText(
      /System-field date column saved/i,
    );
    expect(puts).toHaveLength(1);
    expect(puts[0]).toContain("CONTENTEXPIRYDATE");
    expect(puts[0]).toContain("CONTENTSTARTDATE");
    expect(puts[0]).not.toContain("intervalMinutes");

    await page.locator('[data-testid="developer-wf-back"]').click();
    await expect(page.locator('[data-testid="developer-wf-table"]')).toBeVisible({
      timeout: 30_000,
    });
    await page
      .locator(catalogOpenByExactName("developer-wf-open", "data-wf-name", name))
      .click();
    await expect(systemFieldEdge(page, "CONTENTEXPIRYDATE")).toBeVisible({ timeout: 30_000 });
    await expect(systemFieldEdge(page, "CONTENTSTARTDATE")).toHaveCount(0);
    await expect(absoluteEdge(page)).toHaveAttribute("data-interval", "15");
    await expect(repeatedEdge(page)).toHaveAttribute("data-interval", "15");
    guards.assertClean();
  });

  test("HTTP 400, 403, and 409 leave the previous column", async ({ page, request }) => {
    test.setTimeout(180_000);
    const guards = attachConsoleGuards(page);
    const name = uniqueWorkflowName("Nightly SysErr");
    const base = await createCustomWorkflow(request, name);
    await postAging(request, base, name, { from: "Draft", to: "Review", intervalMinutes: 15 });
    await postAging(request, base, name, {
      from: "Draft",
      to: "Review",
      intervalMinutes: 15,
      type: "REPEATED",
    });
    await postAging(request, base, name, {
      from: "Draft",
      to: "Review",
      type: "SYSTEM_FIELD",
      systemField: "CONTENTSTARTDATE",
    });

    let puts = 0;
    await page.route("**/aging-transitions/system-field**", async (route) => {
      if (route.request().method() !== "PUT") {
        await route.continue();
        return;
      }
      puts += 1;
      const status = puts === 1 ? 400 : puts === 2 ? 403 : 409;
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
    const systemField = systemFieldEdge(page, "CONTENTSTARTDATE");
    const absolute = absoluteEdge(page);
    const repeated = repeatedEdge(page);
    await expect(systemField).toBeVisible({ timeout: 30_000 });
    for (const field of ["CONTENTEXPIRYDATE", "REMINDERDATE", "CONTENTEXPIRYDATE"]) {
      await systemField.locator('[data-testid^="developer-wf-aging-field-change-"]').click();
      await systemField.locator('[data-testid="developer-wf-aging-new-field"]').selectOption(field);
      await systemField.locator('[data-testid="developer-wf-aging-field-save"]').click();
      await expect(page.locator('[data-testid="developer-wf-graph-error"]')).toBeVisible();
      await expect(page.locator('[data-testid="developer-wf-graph-notice"]')).toHaveCount(0);
      await expect(systemField).toHaveAttribute("data-system-field", "CONTENTSTARTDATE");
      await expect(systemField).not.toContainText(`System field aging ${field}`);
      await expect(absolute).toHaveAttribute("data-interval", "15");
      await expect(repeated).toHaveAttribute("data-interval", "15");
    }
    guards.assertClean();
  });

  test("REST changes only the named system field and leaves absolute and repeated rows", async ({
    request,
  }) => {
    test.setTimeout(120_000);
    const headers = jsonHeaders();
    const base = `${BASE_URL}/Rhythmyx/services/workflows`;
    const name = uniqueWorkflowName("Nightly SysApi");
    await createCustomWorkflow(request, name);
    await postAging(request, base, name, { from: "Draft", to: "Review", intervalMinutes: 15 });
    await postAging(request, base, name, {
      from: "Draft",
      to: "Review",
      intervalMinutes: 15,
      type: "REPEATED",
    });
    await postAging(request, base, name, {
      from: "Draft",
      to: "Review",
      type: "SYSTEM_FIELD",
      systemField: "CONTENTSTARTDATE",
    });
    await postAging(request, base, name, {
      from: "Draft",
      to: "Review",
      type: "SYSTEM_FIELD",
      systemField: "REMINDERDATE",
    });

    const columnUrl = `${base}/${encodeURIComponent(name)}/aging-transitions/system-field`;
    const blank = await request.put(columnUrl, {
      headers,
      data: {
        WorkflowAgingSystemFieldWrite: {
          from: "Draft",
          to: "Review",
          systemField: "CONTENTSTARTDATE",
          newSystemField: " ",
        },
      },
    });
    expect(blank.status()).toBe(400);
    const same = await request.put(columnUrl, {
      headers,
      data: {
        WorkflowAgingSystemFieldWrite: {
          from: "Draft",
          to: "Review",
          systemField: "contentstartdate",
          newSystemField: "CONTENTSTARTDATE",
        },
      },
    });
    expect(same.status()).toBe(400);
    const unknown = await request.put(columnUrl, {
      headers,
      data: {
        WorkflowAgingSystemFieldWrite: {
          from: "Draft",
          to: "Review",
          systemField: "CONTENTSTARTDATE",
          newSystemField: "sys_title",
        },
      },
    });
    expect(unknown.status()).toBe(400);

    const dup = await request.put(columnUrl, {
      headers,
      data: {
        WorkflowAgingSystemFieldWrite: {
          from: "Draft",
          to: "Review",
          systemField: "CONTENTSTARTDATE",
          newSystemField: "REMINDERDATE",
        },
      },
    });
    expect(dup.status()).toBe(409);

    const changed = await request.put(columnUrl, {
      headers,
      data: {
        WorkflowAgingSystemFieldWrite: {
          from: "Draft",
          to: "Review",
          systemField: "CONTENTSTARTDATE",
          newSystemField: "CONTENTEXPIRYDATE",
        },
      },
    });
    const changedBody = await changed.text();
    expect(changed.status(), changedBody).toBe(200);
    expect(changedBody).toMatch(/System field aging CONTENTEXPIRYDATE/);
    expect(changedBody).not.toMatch(/System field aging CONTENTSTARTDATE/);
    expect(changedBody).toMatch(/System field aging REMINDERDATE/);
    expect(changedBody).toMatch(/"agingType"\s*:\s*"SYSTEM_FIELD"/);
    expect(changedBody).toMatch(/"agingType"\s*:\s*"ABSOLUTE"/);
    expect(changedBody).toMatch(/Aging 15/);
    expect(changedBody).toMatch(/Repeated aging 15/);
    expect(changedBody).toMatch(/"intervalMinutes"\s*:\s*15/);

    const packaged = await request.put(
      `${base}/${encodeURIComponent("Default Workflow")}/aging-transitions/system-field`,
      {
        headers,
        data: {
          WorkflowAgingSystemFieldWrite: {
            from: "Draft",
            to: "Review",
            systemField: "CONTENTSTARTDATE",
            newSystemField: "CONTENTEXPIRYDATE",
          },
        },
      },
    );
    expect(packaged.status()).toBe(403);
    const missingWorkflow = await request.put(
      `${base}/${encodeURIComponent("No Such Workflow 5277")}/aging-transitions/system-field`,
      {
        headers,
        data: {
          WorkflowAgingSystemFieldWrite: {
            from: "Draft",
            to: "Review",
            systemField: "CONTENTSTARTDATE",
            newSystemField: "CONTENTEXPIRYDATE",
          },
        },
      },
    );
    expect(missingWorkflow.status()).toBe(404);
  });
});
