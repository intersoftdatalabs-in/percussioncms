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
 * Developer workflow step role assignment type (slice 61 / #5140 / parent #1690).
 *
 * Admin sets Reader or Assignee on one role already assigned to one step of a
 * custom workflow. The table shows that type only after the server accepts it.
 * Cancel does not call the server. Packaged workflows do not offer the confirm.
 * HTTP 400/409 do not claim success. The step name and the other roles stay.
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… \
 *     npm run test:surface -- --path tests/developer-workflow-step-role-assignment.spec.js
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

function oppositeType(type) {
  return String(type).toUpperCase() === "READER" ? "ASSIGNEE" : "READER";
}

function graphNodeNames(body) {
  const graph = body.WorkflowGraph || body.workflowGraph || body;
  const nodes = graph.nodes || [];
  if (!Array.isArray(nodes)) {
    return [];
  }
  return nodes.map((n) => (n && (n.name || n.label)) || "").filter(Boolean);
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

function isAssignmentPut(url, method) {
  return (
    method === "PUT" &&
    /\/services\/workflows\/[^/]+\/steps\/[^/]+\/role-assignment(?:\?|$)/.test(url)
  );
}

test.describe("Developer workflow step role assignment (slice 61 / #5140)", () => {
  test("confirm changes one role; cancel does not call the server", async ({
    page,
    request,
  }) => {
    test.setTimeout(180_000);
    const guards = attachConsoleGuards(page);
    const name = uniqueWorkflowName("Nightly Asg");
    const headers = jsonHeaders();
    const seeded = await request.post(`${BASE_URL}/Rhythmyx/services/workflows`, {
      headers,
      data: { WorkflowCreate: { name, description: "assign type" } },
    });
    expect(seeded.status(), "seed custom workflow").toBe(200);

    try {
      const before = await request.get(
        `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent(name)}/role-assignments`,
        { headers },
      );
      expect(before.status(), "list assignment types").toBe(200);
      const beforeRows = assignmentRows(await before.json());
      const target = beforeRows.find((row) => {
        const type = String(row.assignmentType || "").toUpperCase();
        return type === "READER" || type === "ASSIGNEE";
      });
      expect(
        target,
        "seeded workflow has no Reader or Assignee role to change",
      ).toBeTruthy();
      const next = oppositeType(target.assignmentType);
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
      await expect(page.locator('[data-testid="developer-wf-role-assign-table"]')).toBeVisible({
        timeout: 30_000,
      });
      const shown = await readTable(page);
      const shownTarget = shown.find(
        (row) =>
          row.step.toLowerCase() === String(target.stepName).trim().toLowerCase() &&
          row.role.toLowerCase() === String(target.roleName).trim().toLowerCase(),
      );
      expect(shownTarget, "table row for the target role").toBeTruthy();
      expect(shownTarget.type).toBe(String(target.assignmentType).toUpperCase());

      let puts = 0;
      page.on("request", (req) => {
        if (isAssignmentPut(req.url(), req.method())) {
          puts += 1;
        }
      });
      await page.locator('[data-testid="developer-wf-role-assign-type"]').selectOption(next);
      await page.locator('[data-testid="developer-wf-role-assign-cancel"]').click();
      await expect(page.locator('[data-testid="developer-wf-role-assign-type"]')).toHaveValue(
        String(target.assignmentType).toUpperCase(),
      );
      expect(puts).toBe(0);
      expect((await readTable(page)).map((row) => row.type)).toEqual(
        shown.map((row) => row.type),
      );

      await page.locator('[data-testid="developer-wf-role-assign-type"]').selectOption(next);
      const saved = page.waitForResponse(
        (res) => isAssignmentPut(res.url(), res.request().method()),
      );
      await page.locator('[data-testid="developer-wf-role-assign-confirm"]').click();
      expect((await saved).status()).toBe(200);
      await expect(page.locator('[data-testid="developer-wf-role-assign-notice"]')).toBeVisible({
        timeout: 30_000,
      });
      await expect(page.locator('[data-testid="developer-wf-role-assign-error"]')).toHaveCount(0);

      const afterUi = await readTable(page);
      expect(afterUi.map((row) => `${row.step}\t${row.role}`)).toEqual(
        shown.map((row) => `${row.step}\t${row.role}`),
      );
      for (const row of afterUi) {
        const same =
          row.step.toLowerCase() === String(target.stepName).trim().toLowerCase() &&
          row.role.toLowerCase() === String(target.roleName).trim().toLowerCase();
        if (same) {
          expect(row.type).toBe(next);
        } else {
          const prior = shown.find(
            (beforeRow) => beforeRow.step === row.step && beforeRow.role === row.role,
          );
          expect(row.type).toBe(prior.type);
        }
      }

      const after = await request.get(
        `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent(name)}/role-assignments`,
        { headers },
      );
      expect(after.status()).toBe(200);
      const afterRows = assignmentRows(await after.json());
      expect(afterRows.map(rowIdentity).sort()).toEqual(beforeRows.map(rowIdentity).sort());
      const changed = afterRows.find(
        (row) => rowIdentity(row).toLowerCase() === rowIdentity(target).toLowerCase(),
      );
      expect(String(changed.assignmentType).toUpperCase()).toBe(next);
      for (const row of afterRows) {
        if (rowIdentity(row).toLowerCase() === rowIdentity(target).toLowerCase()) {
          continue;
        }
        const prior = beforeRows.find(
          (beforeRow) => rowIdentity(beforeRow).toLowerCase() === rowIdentity(row).toLowerCase(),
        );
        expect(String(row.assignmentType).toUpperCase()).toBe(
          String(prior.assignmentType).toUpperCase(),
        );
      }

      const unchanged = await request.put(
        `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent(name)}/steps/${encodeURIComponent(target.stepName)}/role-assignment`,
        {
          headers,
          data: {
            WorkflowStepRoleAssignmentWrite: {
              roleName: target.roleName,
              assignmentType: next,
            },
          },
        },
      );
      expect(unchanged.status()).toBe(400);
      const invalid = await request.put(
        `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent(name)}/steps/${encodeURIComponent(target.stepName)}/role-assignment`,
        {
          headers,
          data: {
            WorkflowStepRoleAssignmentWrite: {
              roleName: target.roleName,
              assignmentType: "ADMIN",
            },
          },
        },
      );
      expect(invalid.status()).toBe(400);

      const adminRow = afterRows.find(
        (row) => String(row.assignmentType || "").toUpperCase() === "ADMIN",
      );
      if (adminRow) {
        const blocked = await request.put(
          `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent(name)}/steps/${encodeURIComponent(adminRow.stepName)}/role-assignment`,
          {
            headers,
            data: {
              WorkflowStepRoleAssignmentWrite: {
                roleName: adminRow.roleName,
                assignmentType: "READER",
              },
            },
          },
        );
        expect(blocked.status()).toBe(409);
      }

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

  test("packaged workflows and HTTP 409 do not claim a new type", async ({
    page,
    request,
  }) => {
    test.setTimeout(180_000);
    const guards = attachConsoleGuards(page);
    const name = uniqueWorkflowName("Nightly Asg2");
    const headers = jsonHeaders();
    expect(
      (
        await request.post(`${BASE_URL}/Rhythmyx/services/workflows`, {
          headers,
          data: { WorkflowCreate: { name, description: "reject" } },
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
      const target = rows.find((row) => {
        const type = String(row.assignmentType || "").toUpperCase();
        return type === "READER" || type === "ASSIGNEE";
      });
      expect(target, "seeded workflow has no Reader or Assignee role").toBeTruthy();

      const packaged = await request.put(
        `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent("Default Workflow")}/steps/${encodeURIComponent(target.stepName)}/role-assignment`,
        {
          headers,
          data: {
            WorkflowStepRoleAssignmentWrite: {
              roleName: target.roleName,
              assignmentType: oppositeType(target.assignmentType),
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
      await expect(
        page.locator('[data-testid="developer-wf-role-assign-confirm"]'),
      ).toHaveCount(0);

      await page.locator('[data-testid="developer-wf-back"]').click();
      await page
        .locator(catalogOpenByExactName("developer-wf-open", "data-wf-name", name))
        .click();
      await expect(page.locator('[data-testid="developer-wf-role-assign-confirm"]')).toBeVisible({
        timeout: 30_000,
      });
      const beforeTypes = (await readTable(page)).map((row) => row.type);
      await page.route(/\/role-assignment(?:\?|$)/, async (route) => {
        if (route.request().method() !== "PUT") {
          await route.continue();
          return;
        }
        await route.fulfill({
          status: 409,
          contentType: "application/json",
          body: JSON.stringify({ message: "conflict" }),
        });
      });
      await page
        .locator('[data-testid="developer-wf-role-assign-type"]')
        .selectOption(oppositeType(target.assignmentType));
      const rejected = page.waitForResponse(
        (res) => isAssignmentPut(res.url(), res.request().method()),
      );
      await page.locator('[data-testid="developer-wf-role-assign-confirm"]').click();
      expect((await rejected).status()).toBe(409);
      await expect(page.locator('[data-testid="developer-wf-role-assign-error"]')).toBeVisible();
      await expect(page.locator('[data-testid="developer-wf-role-assign-notice"]')).toHaveCount(0);
      expect((await readTable(page)).map((row) => row.type)).toEqual(beforeTypes);

      const still = await request.get(
        `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent(name)}/role-assignments`,
        { headers },
      );
      expect(still.status()).toBe(200);
      const kept = assignmentRows(await still.json()).find(
        (row) => rowIdentity(row).toLowerCase() === rowIdentity(target).toLowerCase(),
      );
      expect(String(kept.assignmentType).toUpperCase()).toBe(
        String(target.assignmentType).toUpperCase(),
      );
      guards.assertClean();
    } finally {
      await request.delete(
        `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent(name)}`,
        { headers },
      );
    }
  });
});
