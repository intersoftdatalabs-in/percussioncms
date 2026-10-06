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
 * Developer → Workflows: change one repeated aging interval (#5276 / parent #1690).
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… TEST_DB_TYPE=h2 TEST_PRODUCT=cms \
 *     npm run test:surface -- --path tests/developer-workflow-repeated-aging-interval.spec.js
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
  return page.locator('[data-testid^="developer-wf-aging-edge-"][data-aging-type="ABSOLUTE"]');
}

function systemFieldEdge(page) {
  return page.locator(
    '[data-testid^="developer-wf-aging-edge-"][data-aging-type="SYSTEM_FIELD"][data-from="Draft"][data-to="Review"][data-system-field="CONTENTSTARTDATE"]',
  );
}

function watchIntervalPuts(page) {
  const bodies = [];
  page.on("request", (req) => {
    if (req.method() === "PUT" && req.url().includes("/aging-transitions/interval")) {
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
    data: { WorkflowCreate: { name, description: "slice 78" } },
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

test.describe("Developer change a repeated aging interval (#5276)", () => {
  test("save shows the new repeated minutes only after success and leaves the other rows", async ({
    page,
    request,
  }) => {
    test.setTimeout(180_000);
    const guards = attachConsoleGuards(page);
    const puts = watchIntervalPuts(page);
    const name = uniqueWorkflowName("Nightly RepIv");
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
    const repeated = repeatedEdge(page);
    const absolute = absoluteEdge(page);
    const systemField = systemFieldEdge(page);
    await expect(repeated).toBeVisible({ timeout: 30_000 });
    await expect(absolute).toBeVisible();
    await expect(systemField).toBeVisible();
    await expect(repeated).toHaveAttribute("data-interval", "15");
    await expect(absolute).toHaveAttribute("data-interval", "15");
    await expect(repeated.locator('[data-testid^="developer-wf-aging-change-"]')).toHaveCount(1);
    await expect(absolute.locator('[data-testid^="developer-wf-aging-change-"]')).toHaveCount(1);
    await expect(systemField.locator('[data-testid^="developer-wf-aging-change-"]')).toHaveCount(0);
    await expect(
      page.locator(
        '[data-aging-type="SYSTEM_FIELD"] [data-testid^="developer-wf-aging-change-"]',
      ),
    ).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-wf-aging-interval-form"]')).toHaveCount(0);

    await repeated.locator('[data-testid^="developer-wf-aging-change-"]').click();
    await repeated.locator('[data-testid="developer-wf-aging-new-minutes"]').fill("30");
    await repeated.locator('[data-testid="developer-wf-aging-interval-cancel"]').click();
    expect(puts).toHaveLength(0);
    await expect(page.locator('[data-testid="developer-wf-aging-interval-form"]')).toHaveCount(0);
    await expect(repeated).toHaveAttribute("data-interval", "15");
    await expect(absolute).toHaveAttribute("data-interval", "15");

    await repeated.locator('[data-testid^="developer-wf-aging-change-"]').click();
    await repeated.locator('[data-testid="developer-wf-aging-new-minutes"]').fill("");
    await repeated.locator('[data-testid="developer-wf-aging-interval-save"]').click();
    await expect(page.locator('[data-testid="developer-wf-graph-error"]')).toBeVisible();
    await repeated.locator('[data-testid="developer-wf-aging-new-minutes"]').fill("0");
    await repeated.locator('[data-testid="developer-wf-aging-interval-save"]').click();
    await expect(page.locator('[data-testid="developer-wf-graph-error"]')).toBeVisible();
    await repeated.locator('[data-testid="developer-wf-aging-new-minutes"]').fill("15");
    await repeated.locator('[data-testid="developer-wf-aging-interval-save"]').click();
    await expect(page.locator('[data-testid="developer-wf-graph-error"]')).toBeVisible();
    expect(puts).toHaveLength(0);
    await expect(page.locator('[data-testid="developer-wf-graph-notice"]')).toHaveCount(0);
    await expect(repeated).toContainText("15 minutes");
    await expect(absolute).toHaveAttribute("data-interval", "15");

    await repeated.locator('[data-testid="developer-wf-aging-new-minutes"]').fill("30");
    await repeated.locator('[data-testid="developer-wf-aging-interval-save"]').click();
    await expect(repeated).toHaveAttribute("data-interval", "30", { timeout: 30_000 });
    await expect(repeated).toContainText("Repeated aging 30");
    await expect(repeated).toContainText("repeated");
    await expect(repeated).toHaveAttribute("data-aging-type", "REPEATED");
    await expect(absolute).toHaveAttribute("data-interval", "15");
    await expect(absolute).toContainText("Aging 15");
    await expect(systemField).toBeVisible();
    await expect(systemField).toContainText("CONTENTSTARTDATE");
    await expect(page.locator('[data-testid="developer-wf-graph-notice"]')).toContainText(
      /Aging interval saved/i,
    );
    expect(puts).toHaveLength(1);
    expect(puts[0]).toContain("REPEATED");
    expect(puts[0]).not.toContain("SYSTEM_FIELD");

    await page.locator('[data-testid="developer-wf-back"]').click();
    await expect(page.locator('[data-testid="developer-wf-table"]')).toBeVisible({
      timeout: 30_000,
    });
    await page
      .locator(catalogOpenByExactName("developer-wf-open", "data-wf-name", name))
      .click();
    await expect(repeatedEdge(page)).toHaveAttribute("data-interval", "30", { timeout: 30_000 });
    await expect(absoluteEdge(page)).toHaveAttribute("data-interval", "15");
    await expect(systemFieldEdge(page)).toBeVisible();
    guards.assertClean();
  });

  test("HTTP 400, 403, and 409 leave the previous repeated interval", async ({ page, request }) => {
    test.setTimeout(180_000);
    const guards = attachConsoleGuards(page);
    const name = uniqueWorkflowName("Nightly RepErr");
    const base = await createCustomWorkflow(request, name);
    await postAging(request, base, name, { from: "Draft", to: "Review", intervalMinutes: 15 });
    await postAging(request, base, name, {
      from: "Draft",
      to: "Review",
      intervalMinutes: 15,
      type: "REPEATED",
    });

    await page.route("**/aging-transitions/interval**", async (route) => {
      if (route.request().method() !== "PUT") {
        await route.continue();
        return;
      }
      const body = route.request().postData() || "";
      const status = body.includes('"newIntervalMinutes":409')
        ? 409
        : body.includes('"newIntervalMinutes":403')
          ? 403
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
    const repeated = repeatedEdge(page);
    const absolute = absoluteEdge(page);
    await expect(repeated).toBeVisible({ timeout: 30_000 });
    for (const minutes of ["400", "403", "409"]) {
      await repeated.locator('[data-testid^="developer-wf-aging-change-"]').click();
      await repeated.locator('[data-testid="developer-wf-aging-new-minutes"]').fill(minutes);
      await repeated.locator('[data-testid="developer-wf-aging-interval-save"]').click();
      await expect(page.locator('[data-testid="developer-wf-graph-error"]')).toBeVisible();
      await expect(page.locator('[data-testid="developer-wf-graph-notice"]')).toHaveCount(0);
      await expect(repeated).toHaveAttribute("data-interval", "15");
      await expect(absolute).toHaveAttribute("data-interval", "15");
      await expect(repeated).not.toContainText(`${minutes} minutes`);
    }
    guards.assertClean();
  });

  test("REST identifies the repeated edge and does not change the absolute twin", async ({
    request,
  }) => {
    test.setTimeout(120_000);
    const headers = jsonHeaders();
    const base = `${BASE_URL}/Rhythmyx/services/workflows`;
    const name = uniqueWorkflowName("Nightly RepApi");
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
      intervalMinutes: 45,
      type: "REPEATED",
    });

    const intervalUrl = `${base}/${encodeURIComponent(name)}/aging-transitions/interval`;
    const nonPositive = await request.put(intervalUrl, {
      headers,
      data: {
        WorkflowAgingIntervalWrite: {
          from: "Draft",
          to: "Review",
          intervalMinutes: 15,
          newIntervalMinutes: 0,
          type: "REPEATED",
        },
      },
    });
    expect(nonPositive.status()).toBe(400);
    const unchanged = await request.put(intervalUrl, {
      headers,
      data: {
        WorkflowAgingIntervalWrite: {
          from: "Draft",
          to: "Review",
          intervalMinutes: 15,
          newIntervalMinutes: 15,
          type: "REPEATED",
        },
      },
    });
    expect(unchanged.status()).toBe(400);
    const unknown = await request.put(intervalUrl, {
      headers,
      data: {
        WorkflowAgingIntervalWrite: {
          from: "Draft",
          to: "Review",
          intervalMinutes: 15,
          newIntervalMinutes: 30,
          type: "NOPE",
        },
      },
    });
    expect(unknown.status()).toBe(400);
    const systemField = await request.put(intervalUrl, {
      headers,
      data: {
        WorkflowAgingIntervalWrite: {
          from: "Draft",
          to: "Review",
          intervalMinutes: 15,
          newIntervalMinutes: 30,
          type: "SYSTEM_FIELD",
        },
      },
    });
    expect(systemField.status()).toBe(400);

    const absoluteOnly = uniqueWorkflowName("Nightly AbsOnly");
    await createCustomWorkflow(request, absoluteOnly);
    await postAging(request, base, absoluteOnly, {
      from: "Draft",
      to: "Review",
      intervalMinutes: 15,
    });
    const missingRepeated = await request.put(
      `${base}/${encodeURIComponent(absoluteOnly)}/aging-transitions/interval`,
      {
        headers,
        data: {
          WorkflowAgingIntervalWrite: {
            from: "Draft",
            to: "Review",
            intervalMinutes: 15,
            newIntervalMinutes: 30,
            type: "REPEATED",
          },
        },
      },
    );
    expect(missingRepeated.status()).toBe(404);
    const absoluteStill = await request.get(
      `${base}/${encodeURIComponent(absoluteOnly)}/graph`,
      { headers },
    );
    const absoluteStillBody = await absoluteStill.text();
    expect(absoluteStill.status(), absoluteStillBody).toBe(200);
    expect(absoluteStillBody).toMatch(/"agingType"\s*:\s*"ABSOLUTE"/);
    expect(absoluteStillBody).toMatch(/"intervalMinutes"\s*:\s*15/);
    expect(absoluteStillBody).not.toMatch(/"agingType"\s*:\s*"REPEATED"/);

    const dup = await request.put(intervalUrl, {
      headers,
      data: {
        WorkflowAgingIntervalWrite: {
          from: "Draft",
          to: "Review",
          intervalMinutes: 15,
          newIntervalMinutes: 45,
          type: "REPEATED",
        },
      },
    });
    expect(dup.status()).toBe(409);

    const changed = await request.put(intervalUrl, {
      headers,
      data: {
        WorkflowAgingIntervalWrite: {
          from: "Draft",
          to: "Review",
          intervalMinutes: 15,
          newIntervalMinutes: 30,
          type: "REPEATED",
        },
      },
    });
    const changedBody = await changed.text();
    expect(changed.status(), changedBody).toBe(200);
    expect(changedBody).toMatch(/"agingType"\s*:\s*"REPEATED"/);
    expect(changedBody).toMatch(/Repeated aging 30/);
    expect(changedBody).toMatch(/"agingType"\s*:\s*"ABSOLUTE"/);
    expect(changedBody).toMatch(/"intervalMinutes"\s*:\s*15/);

    const absoluteMove = await request.put(intervalUrl, {
      headers,
      data: {
        WorkflowAgingIntervalWrite: {
          from: "Draft",
          to: "Review",
          intervalMinutes: 15,
          newIntervalMinutes: 20,
        },
      },
    });
    const movedBody = await absoluteMove.text();
    expect(absoluteMove.status(), movedBody).toBe(200);
    expect(movedBody).toMatch(/Aging 20/);
    expect(movedBody).toMatch(/Repeated aging 30/);
    expect(movedBody).not.toMatch(/Repeated aging 20/);

    const packaged = await request.put(
      `${base}/${encodeURIComponent("Default Workflow")}/aging-transitions/interval`,
      {
        headers,
        data: {
          WorkflowAgingIntervalWrite: {
            from: "Draft",
            to: "Review",
            intervalMinutes: 15,
            newIntervalMinutes: 30,
            type: "REPEATED",
          },
        },
      },
    );
    expect(packaged.status()).toBe(403);
    const missingWorkflow = await request.put(
      `${base}/${encodeURIComponent("No Such Workflow 5276")}/aging-transitions/interval`,
      {
        headers,
        data: {
          WorkflowAgingIntervalWrite: {
            from: "Draft",
            to: "Review",
            intervalMinutes: 15,
            newIntervalMinutes: 30,
            type: "REPEATED",
          },
        },
      },
    );
    expect(missingWorkflow.status()).toBe(404);
  });
});
