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
 * Developer remove one role from a workflow step (slice 64 / #5153 / parent #1690).
 *
 * Admin removes one Reader or Assignee role from one step. The table drops
 * that role only after the server accepts it. Cancel does not call the
 * server. Packaged workflows do not offer remove. HTTP 400/403/404/409 do
 * not claim success. This is not PUT role-assignment.
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… \
 *     npm run test:surface -- --path tests/developer-workflow-step-role-remove.spec.js
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

function typeOf(row) {
  return String(row.assignmentType || "").trim().toUpperCase();
}

function graphNodeNames(body) {
  const graph = body.WorkflowGraph || body.workflowGraph || body;
  const nodes = graph.nodes || [];
  if (!Array.isArray(nodes)) {
    return [];
  }
  return nodes.map((n) => (n && (n.name || n.label)) || "").filter(Boolean);
}

function findRemoveTarget(rows) {
  const mutable = rows.find((row) => typeOf(row) === "READER" || typeOf(row) === "ASSIGNEE");
  if (!mutable) {
    return null;
  }
  const stepName = String(mutable.stepName || "").trim();
  const roleName = String(mutable.roleName || "").trim();
  const alsoOn = rows.find(
    (row) =>
      String(row.roleName || "").trim().toLowerCase() === roleName.toLowerCase() &&
      String(row.stepName || "").trim().toLowerCase() !== stepName.toLowerCase(),
  );
  return {
    stepName,
    roleName,
    otherStep: alsoOn ? String(alsoOn.stepName || "").trim() : "",
  };
}

function findProtectedTarget(rows) {
  const hit = rows.find((row) => typeOf(row) === "ADMIN" || typeOf(row) === "NONE");
  if (!hit) {
    return null;
  }
  return {
    stepName: String(hit.stepName || "").trim(),
    roleName: String(hit.roleName || "").trim(),
  };
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

function isRemoveDelete(url, method) {
  return (
    method === "DELETE" &&
    /\/services\/workflows\/[^/]+\/steps\/[^/]+\/roles\/[^/?]+(?:\?|$)/.test(url) &&
    !/role-assignment/.test(url)
  );
}

function roleDeleteUrl(name, step, role) {
  return `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent(name)}/steps/${encodeURIComponent(step)}/roles/${encodeURIComponent(role)}`;
}

test.describe("Developer remove one role from a workflow step (slice 64 / #5153)", () => {
  test("confirm removes one role; cancel does not call the server", async ({
    page,
    request,
  }) => {
    test.setTimeout(180_000);
    const guards = attachConsoleGuards(page);
    const name = uniqueWorkflowName("Nightly Remove");
    const headers = jsonHeaders();
    const seeded = await request.post(`${BASE_URL}/Rhythmyx/services/workflows`, {
      headers,
      data: { WorkflowCreate: { name, description: "remove role" } },
    });
    expect(seeded.status(), "seed custom workflow").toBe(200);

    try {
      const before = await request.get(
        `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent(name)}/role-assignments`,
        { headers },
      );
      expect(before.status(), "list assignment types").toBe(200);
      const beforeRows = assignmentRows(await before.json());
      const target = findRemoveTarget(beforeRows);
      expect(target, "seeded workflow has no Reader or Assignee role").toBeTruthy();
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
      await expect(page.locator('[data-testid="developer-wf-role-remove"]')).toBeVisible({
        timeout: 30_000,
      });
      const shown = await readTable(page);
      expect(tableHas(shown, target.stepName, target.roleName)).toBe(true);

      let deletes = 0;
      let assignmentPuts = 0;
      page.on("request", (req) => {
        if (isRemoveDelete(req.url(), req.method())) {
          deletes += 1;
        }
        if (
          req.method() === "PUT" &&
          /\/role-assignment(?:\?|$)/.test(req.url())
        ) {
          assignmentPuts += 1;
        }
      });
      await page
        .locator('[data-testid="developer-wf-role-remove-step"]')
        .selectOption(target.stepName);
      await page
        .locator('[data-testid="developer-wf-role-remove-role"]')
        .selectOption(target.roleName);
      await page.locator('[data-testid="developer-wf-role-remove-request"]').click();
      await expect(page.locator('[data-testid="developer-catalog-confirm-dialog"]')).toBeVisible();
      await page.locator('[data-testid="developer-catalog-confirm-cancel"]').click();
      expect(deletes).toBe(0);
      expect(assignmentPuts).toBe(0);
      expect(tableHas(await readTable(page), target.stepName, target.roleName)).toBe(true);
      await expect(page.locator('[data-testid="developer-catalog-confirm-dialog"]')).toHaveCount(0);

      await page
        .locator('[data-testid="developer-wf-role-remove-step"]')
        .selectOption(target.stepName);
      await page
        .locator('[data-testid="developer-wf-role-remove-role"]')
        .selectOption(target.roleName);
      await page.locator('[data-testid="developer-wf-role-remove-request"]').click();
      const saved = page.waitForResponse((res) =>
        isRemoveDelete(res.url(), res.request().method()),
      );
      await page.locator('[data-testid="developer-catalog-confirm-submit"]').click();
      const savedResponse = await saved;
      expect(savedResponse.status()).toBe(200);
      expect(savedResponse.url()).not.toContain("role-assignment");
      await expect(page.locator('[data-testid="developer-wf-role-remove-notice"]')).toBeVisible({
        timeout: 30_000,
      });
      await expect(page.locator('[data-testid="developer-wf-role-remove-error"]')).toHaveCount(0);
      expect(deletes).toBe(1);
      expect(assignmentPuts).toBe(0);

      const afterUi = await readTable(page);
      expect(tableHas(afterUi, target.stepName, target.roleName)).toBe(false);
      if (target.otherStep) {
        expect(tableHas(afterUi, target.otherStep, target.roleName)).toBe(true);
      }
      for (const prior of shown) {
        if (
          prior.step.toLowerCase() === target.stepName.toLowerCase() &&
          prior.role.toLowerCase() === target.roleName.toLowerCase()
        ) {
          continue;
        }
        const still = afterUi.find((row) => row.step === prior.step && row.role === prior.role);
        expect(still, `kept ${prior.step} ${prior.role}`).toBeTruthy();
        expect(still.type).toBe(prior.type);
      }

      const after = await request.get(
        `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent(name)}/role-assignments`,
        { headers },
      );
      expect(after.status()).toBe(200);
      const afterRows = assignmentRows(await after.json());
      expect(
        afterRows.some(
          (row) =>
            rowIdentity(row).toLowerCase() ===
            `${target.stepName}\t${target.roleName}`.toLowerCase(),
        ),
      ).toBe(false);
      for (const prior of beforeRows) {
        if (
          rowIdentity(prior).toLowerCase() ===
          `${target.stepName}\t${target.roleName}`.toLowerCase()
        ) {
          continue;
        }
        const still = afterRows.find(
          (row) => rowIdentity(row).toLowerCase() === rowIdentity(prior).toLowerCase(),
        );
        expect(still, `stored ${rowIdentity(prior)}`).toBeTruthy();
        expect(typeOf(still)).toBe(typeOf(prior));
      }

      const again = await request.delete(roleDeleteUrl(name, target.stepName, target.roleName), {
        headers,
      });
      expect(again.status()).toBe(404);
      const unknown = await request.delete(
        roleDeleteUrl(name, target.stepName, "Not A Workflow Role"),
        { headers },
      );
      expect(unknown.status()).toBe(404);
      const blank = await request.delete(roleDeleteUrl(name, target.stepName, " "), { headers });
      expect(blank.status()).toBe(400);
      const missingStep = await request.delete(roleDeleteUrl(name, "No Such Step", target.roleName), {
        headers,
      });
      expect(missingStep.status()).toBe(404);
      const packaged = await request.delete(
        roleDeleteUrl("Default Workflow", target.stepName, target.roleName),
        { headers },
      );
      expect(packaged.status()).toBe(403);

      const protectedRole = findProtectedTarget(beforeRows);
      if (protectedRole) {
        const blocked = await request.delete(
          roleDeleteUrl(name, protectedRole.stepName, protectedRole.roleName),
          { headers },
        );
        expect(blocked.status()).toBe(409);
        const stillProtected = await request.get(
          `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent(name)}/role-assignments`,
          { headers },
        );
        expect(
          assignmentRows(await stillProtected.json()).some(
            (row) =>
              rowIdentity(row).toLowerCase() ===
              `${protectedRole.stepName}\t${protectedRole.roleName}`.toLowerCase(),
          ),
        ).toBe(true);
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

  test("packaged workflows and HTTP 409 do not claim the role was removed", async ({
    page,
    request,
  }) => {
    test.setTimeout(180_000);
    const guards = attachConsoleGuards(page);
    const name = uniqueWorkflowName("Nightly Remove2");
    const headers = jsonHeaders();
    expect(
      (
        await request.post(`${BASE_URL}/Rhythmyx/services/workflows`, {
          headers,
          data: { WorkflowCreate: { name, description: "reject remove" } },
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
      const target = findRemoveTarget(rows);
      expect(target, "seeded workflow has no role to remove").toBeTruthy();

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
      await expect(page.locator('[data-testid="developer-wf-role-remove"]')).toHaveCount(0);
      await expect(page.locator('[data-testid="developer-wf-role-remove-request"]')).toHaveCount(0);

      await page.locator('[data-testid="developer-wf-back"]').click();
      await page
        .locator(catalogOpenByExactName("developer-wf-open", "data-wf-name", name))
        .click();
      await expect(page.locator('[data-testid="developer-wf-role-remove-request"]')).toBeVisible({
        timeout: 30_000,
      });
      const beforeIds = (await readTable(page)).map((row) => `${row.step}\t${row.role}`);
      expect(tableHas(await readTable(page), target.stepName, target.roleName)).toBe(true);
      await page.route(/\/steps\/[^/]+\/roles\/[^/?]+(?:\?|$)/, async (route) => {
        if (route.request().method() !== "DELETE") {
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
        .locator('[data-testid="developer-wf-role-remove-step"]')
        .selectOption(target.stepName);
      await page
        .locator('[data-testid="developer-wf-role-remove-role"]')
        .selectOption(target.roleName);
      await page.locator('[data-testid="developer-wf-role-remove-request"]').click();
      const rejected = page.waitForResponse((res) =>
        isRemoveDelete(res.url(), res.request().method()),
      );
      await page.locator('[data-testid="developer-catalog-confirm-submit"]').click();
      expect((await rejected).status()).toBe(409);
      await expect(page.locator('[data-testid="developer-wf-role-remove-error"]')).toBeVisible();
      await expect(page.locator('[data-testid="developer-wf-role-remove-notice"]')).toHaveCount(0);
      expect((await readTable(page)).map((row) => `${row.step}\t${row.role}`)).toEqual(beforeIds);

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
      ).toBe(true);
      guards.assertClean();
    } finally {
      await request.delete(
        `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent(name)}`,
        { headers },
      );
    }
  });
});
