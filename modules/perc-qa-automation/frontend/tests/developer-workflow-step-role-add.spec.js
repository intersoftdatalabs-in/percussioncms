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
 * Developer add one role to a workflow step (slice 63 / #5152 / parent #1690).
 *
 * Admin adds one existing workflow role that is not already on a step. The
 * table shows that role only after the server accepts it. Cancel does not
 * call the server. Packaged workflows do not offer confirm. HTTP 400/403/404/409
 * do not claim success. Notify and inbox are not sent.
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… \
 *     npm run test:surface -- --path tests/developer-workflow-step-role-add.spec.js
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

function jsonHeaders() {
  return {
    ...adminBasicAuthHeaders(),
    Accept: "application/json",
    "Content-Type": "application/json",
  };
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

function assignmentRows(body) {
  const wrapped =
    body.WorkflowStepRoleAssignmentList ||
    body.workflowStepRoleAssignmentList ||
    body;
  const rows = wrapped.assignments || wrapped.Assignments || [];
  return Array.isArray(rows) ? rows : [];
}

function rowIdentity(row) {
  return `${(row.stepName || "").trim()}\t${(row.roleName || "").trim()}`;
}

function graphNodeNames(body) {
  const graph = body.WorkflowGraph || body.workflowGraph || body;
  const nodes = graph.nodes || [];
  if (!Array.isArray(nodes)) {
    return [];
  }
  return nodes.map((n) => (n && (n.name || n.label)) || "").filter(Boolean);
}

function distinct(values) {
  const out = [];
  for (const value of values) {
    const name = String(value || "").trim();
    if (!name) {
      continue;
    }
    if (!out.some((existing) => existing.toLowerCase() === name.toLowerCase())) {
      out.push(name);
    }
  }
  return out;
}

/** A workflow role that is on some step but not on the returned step. */
function findAddTarget(rows) {
  const roles = distinct(rows.map((row) => row.roleName));
  const steps = distinct(rows.map((row) => row.stepName));
  for (const step of steps) {
    const onStep = new Set(
      rows
        .filter(
          (row) =>
            String(row.stepName || "").trim().toLowerCase() === step.toLowerCase(),
        )
        .map((row) => String(row.roleName || "").trim().toLowerCase()),
    );
    const missing = roles.find((role) => !onStep.has(role.toLowerCase()));
    if (missing) {
      return { stepName: step, roleName: missing };
    }
  }
  return null;
}

async function readTable(page) {
  const rows = page.locator('[data-testid^="developer-wf-role-assign-row-"]');
  const count = await rows.count();
  const out = [];
  for (let i = 0; i < count; i += 1) {
    const row = page.locator(`[data-testid="developer-wf-role-assign-row-${i}"]`);
    const cells = row.locator("td");
    out.push({
      step: (await cells.nth(0).innerText()).trim(),
      role: (await cells.nth(1).innerText()).trim(),
      type: await page
        .locator(`[data-testid="developer-wf-role-assign-type-${i}"]`)
        .getAttribute("data-assignment-type"),
    });
  }
  return out;
}

function tableHas(rows, step, role) {
  return rows.some(
    (row) =>
      row.step.toLowerCase() === step.toLowerCase() &&
      row.role.toLowerCase() === role.toLowerCase(),
  );
}

function isAddPost(url, method) {
  return (
    method === "POST" &&
    /\/services\/workflows\/[^/]+\/steps\/[^/]+\/roles(?:\?|$)/.test(url) &&
    !/role-assignment/.test(url)
  );
}

test.describe("Developer add one role to a workflow step (slice 63 / #5152)", () => {
  test("confirm adds one role; cancel does not call the server", async ({
    page,
    request,
  }) => {
    test.setTimeout(180_000);
    const guards = attachConsoleGuards(page);
    const name = uniqueWorkflowName("Nightly Add");
    const headers = jsonHeaders();
    const seeded = await request.post(`${BASE_URL}/Rhythmyx/services/workflows`, {
      headers,
      data: { WorkflowCreate: { name, description: "add role" } },
    });
    expect(seeded.status(), "seed custom workflow").toBe(200);

    try {
      const before = await request.get(
        `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent(name)}/role-assignments`,
        { headers },
      );
      expect(before.status(), "list assignment types").toBe(200);
      const beforeRows = assignmentRows(await before.json());
      const target = findAddTarget(beforeRows);
      expect(
        target,
        "seeded workflow has no workflow role that is missing from a step",
      ).toBeTruthy();
      const beforeGraph = await request.get(
        `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent(name)}/graph`,
        { headers },
      );
      expect(beforeGraph.status()).toBe(200);
      const beforeNodes = graphNodeNames(await beforeGraph.json());

      await loginAsAdmin(page);
      await openWorkflowsCatalog(page);
      await page
        .locator(catalogOpenByExactName("developer-wf-open", "data-wf-name", name))
        .click();
      await expect(page.locator('[data-testid="developer-wf-detail-title"]')).toContainText(
        name,
        { timeout: 30_000 },
      );
      await expect(page.locator('[data-testid="developer-wf-role-add"]')).toBeVisible({
        timeout: 30_000,
      });
      const shown = await readTable(page);
      expect(tableHas(shown, target.stepName, target.roleName)).toBe(false);

      let posts = 0;
      page.on("request", (req) => {
        if (isAddPost(req.url(), req.method())) {
          posts += 1;
        }
      });
      await page.locator('[data-testid="developer-wf-role-add-step"]').selectOption(target.stepName);
      await page.locator('[data-testid="developer-wf-role-add-role"]').selectOption(target.roleName);
      await page.locator('[data-testid="developer-wf-role-add-type"]').selectOption("ASSIGNEE");
      await page.locator('[data-testid="developer-wf-role-add-cancel"]').click();
      expect(posts).toBe(0);
      expect(tableHas(await readTable(page), target.stepName, target.roleName)).toBe(false);
      await expect(page.locator('[data-testid="developer-wf-role-add-type"]')).toHaveValue("READER");

      await page.locator('[data-testid="developer-wf-role-add-step"]').selectOption(target.stepName);
      await page.locator('[data-testid="developer-wf-role-add-role"]').selectOption(target.roleName);
      await page.locator('[data-testid="developer-wf-role-add-type"]').selectOption("ASSIGNEE");
      const saved = page.waitForResponse((res) =>
        isAddPost(res.url(), res.request().method()),
      );
      await page.locator('[data-testid="developer-wf-role-add-confirm"]').click();
      expect((await saved).status()).toBe(200);
      await expect(page.locator('[data-testid="developer-wf-role-add-notice"]')).toBeVisible({
        timeout: 30_000,
      });
      await expect(page.locator('[data-testid="developer-wf-role-add-error"]')).toHaveCount(0);

      const afterUi = await readTable(page);
      const added = afterUi.find(
        (row) =>
          row.step.toLowerCase() === target.stepName.toLowerCase() &&
          row.role.toLowerCase() === target.roleName.toLowerCase(),
      );
      expect(added, "table shows the role only after success").toBeTruthy();
      expect(added.type).toBe("ASSIGNEE");
      for (const prior of shown) {
        const still = afterUi.find(
          (row) => row.step === prior.step && row.role === prior.role,
        );
        expect(still, `kept ${prior.step} ${prior.role}`).toBeTruthy();
        expect(still.type).toBe(prior.type);
      }

      const after = await request.get(
        `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent(name)}/role-assignments`,
        { headers },
      );
      expect(after.status()).toBe(200);
      const afterRows = assignmentRows(await after.json());
      const changed = afterRows.find(
        (row) =>
          rowIdentity(row).toLowerCase() ===
          `${target.stepName}\t${target.roleName}`.toLowerCase(),
      );
      expect(String(changed.assignmentType).toUpperCase()).toBe("ASSIGNEE");
      for (const prior of beforeRows) {
        const still = afterRows.find(
          (row) => rowIdentity(row).toLowerCase() === rowIdentity(prior).toLowerCase(),
        );
        expect(still).toBeTruthy();
        expect(String(still.assignmentType).toUpperCase()).toBe(
          String(prior.assignmentType).toUpperCase(),
        );
      }

      const again = await request.post(
        `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent(name)}/steps/${encodeURIComponent(target.stepName)}/roles`,
        {
          headers,
          data: {
            WorkflowStepRoleAdd: {
              roleName: target.roleName,
              assignmentType: "READER",
            },
          },
        },
      );
      expect(again.status()).toBe(409);
      const unknown = await request.post(
        `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent(name)}/steps/${encodeURIComponent(target.stepName)}/roles`,
        {
          headers,
          data: {
            WorkflowStepRoleAdd: {
              roleName: "Not A Workflow Role",
              assignmentType: "READER",
            },
          },
        },
      );
      expect(unknown.status()).toBe(404);
      const invalid = await request.post(
        `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent(name)}/steps/${encodeURIComponent(target.stepName)}/roles`,
        {
          headers,
          data: {
            WorkflowStepRoleAdd: {
              roleName: target.roleName,
              assignmentType: "ADMIN",
            },
          },
        },
      );
      expect(invalid.status()).toBe(400);
      const missingStep = await request.post(
        `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent(name)}/steps/${encodeURIComponent("No Such Step")}/roles`,
        {
          headers,
          data: {
            WorkflowStepRoleAdd: {
              roleName: target.roleName,
              assignmentType: "READER",
            },
          },
        },
      );
      expect(missingStep.status()).toBe(404);

      const afterGraph = await request.get(
        `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent(name)}/graph`,
        { headers },
      );
      expect(afterGraph.status()).toBe(200);
      expect(graphNodeNames(await afterGraph.json())).toEqual(beforeNodes);
      guards.assertClean();
    } finally {
      await request.delete(
        `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent(name)}`,
        { headers },
      );
    }
  });

  test("packaged workflows and HTTP 409 do not claim the role was added", async ({
    page,
    request,
  }) => {
    test.setTimeout(180_000);
    const guards = attachConsoleGuards(page);
    const name = uniqueWorkflowName("Nightly Add2");
    const headers = jsonHeaders();
    expect(
      (
        await request.post(`${BASE_URL}/Rhythmyx/services/workflows`, {
          headers,
          data: { WorkflowCreate: { name, description: "reject add" } },
        })
      ).status(),
    ).toBe(200);

    try {
      const listed = await request.get(
        `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent(name)}/role-assignments`,
        { headers },
      );
      expect(listed.status()).toBe(200);
      const rows = assignmentRows(await listed.json());
      const target = findAddTarget(rows);
      expect(target, "seeded workflow has no role to add").toBeTruthy();

      const packaged = await request.post(
        `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent("Default Workflow")}/steps/${encodeURIComponent(target.stepName)}/roles`,
        {
          headers,
          data: {
            WorkflowStepRoleAdd: {
              roleName: target.roleName,
              assignmentType: "READER",
            },
          },
        },
      );
      expect(packaged.status()).toBe(403);

      await loginAsAdmin(page);
      await openWorkflowsCatalog(page);
      await page
        .locator(
          catalogOpenByExactName("developer-wf-open", "data-wf-name", "Default Workflow"),
        )
        .click();
      await expect(page.locator('[data-testid="developer-wf-detail-title"]')).toContainText(
        "Default Workflow",
        { timeout: 30_000 },
      );
      await expect(page.locator('[data-testid="developer-wf-role-assign"]')).toBeVisible({
        timeout: 30_000,
      });
      await expect(page.locator('[data-testid="developer-wf-role-add"]')).toHaveCount(0);
      await expect(page.locator('[data-testid="developer-wf-role-add-confirm"]')).toHaveCount(0);

      await page.locator('[data-testid="developer-wf-back"]').click();
      await page
        .locator(catalogOpenByExactName("developer-wf-open", "data-wf-name", name))
        .click();
      await expect(page.locator('[data-testid="developer-wf-role-add-confirm"]')).toBeVisible({
        timeout: 30_000,
      });
      const beforeIds = (await readTable(page)).map((row) => `${row.step}\t${row.role}`);
      expect(tableHas(await readTable(page), target.stepName, target.roleName)).toBe(false);
      await page.route(/\/steps\/[^/]+\/roles(?:\?|$)/, async (route) => {
        if (route.request().method() !== "POST") {
          await route.continue();
          return;
        }
        await route.fulfill({
          status: 409,
          contentType: "application/json",
          body: JSON.stringify({ message: "conflict" }),
        });
      });
      await page.locator('[data-testid="developer-wf-role-add-step"]').selectOption(target.stepName);
      await page.locator('[data-testid="developer-wf-role-add-role"]').selectOption(target.roleName);
      const rejected = page.waitForResponse((res) =>
        isAddPost(res.url(), res.request().method()),
      );
      await page.locator('[data-testid="developer-wf-role-add-confirm"]').click();
      expect((await rejected).status()).toBe(409);
      await expect(page.locator('[data-testid="developer-wf-role-add-error"]')).toBeVisible();
      await expect(page.locator('[data-testid="developer-wf-role-add-notice"]')).toHaveCount(0);
      expect((await readTable(page)).map((row) => `${row.step}\t${row.role}`)).toEqual(beforeIds);
      expect(tableHas(await readTable(page), target.stepName, target.roleName)).toBe(false);

      const still = await request.get(
        `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent(name)}/role-assignments`,
        { headers },
      );
      expect(still.status()).toBe(200);
      expect(
        assignmentRows(await still.json()).some(
          (row) =>
            rowIdentity(row).toLowerCase() ===
            `${target.stepName}\t${target.roleName}`.toLowerCase(),
        ),
      ).toBe(false);
      guards.assertClean();
    } finally {
      await request.delete(
        `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent(name)}`,
        { headers },
      );
    }
  });
});
