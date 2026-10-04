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
 * Developer workflow step role inbox (slice 66 / #5176 / parent #1690).
 *
 * Admin turns inbox on or off for one Reader or Assignee already assigned to
 * one step of a custom workflow. The table shows that flag only after the
 * server accepts it. Cancel does not call the server. Packaged workflows do
 * not offer the confirm. HTTP 400/403/409 do not claim success. Assignment
 * type and notify stay unchanged.
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… \
 *     npm run test:surface -- --path tests/developer-workflow-step-role-inbox.spec.js
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

function flagOf(row, key) {
  const value = row && row[key];
  return value === true || value === "true" || value === "y";
}

function isMutable(row) {
  const type = String((row && row.assignmentType) || "").trim().toUpperCase();
  return type === "READER" || type === "ASSIGNEE";
}

function isInboxPut(url, method) {
  return (
    method === "PUT" &&
    /\/services\/workflows\/[^/]+\/steps\/[^/]+\/role-inbox(?:\?|$)/.test(url)
  );
}

async function readInbox(page) {
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
      notify: await page
        .locator(`[data-testid="developer-wf-role-notify-${i}"]`)
        .getAttribute("data-notify"),
      inbox: await page
        .locator(`[data-testid="developer-wf-role-inbox-${i}"]`)
        .getAttribute("data-inbox"),
    });
  }
  return out;
}

test.describe("Developer workflow step role inbox (slice 66 / #5176)", () => {
  test("confirm changes one flag; cancel does not call the server", async ({
    page,
    request,
  }) => {
    test.setTimeout(180_000);
    const guards = attachConsoleGuards(page);
    const name = uniqueWorkflowName("Nightly Inb");
    const headers = jsonHeaders();
    const seeded = await request.post(`${BASE_URL}/Rhythmyx/services/workflows`, {
      headers,
      data: { WorkflowCreate: { name, description: "inbox" } },
    });
    expect(seeded.status(), "seed custom workflow").toBe(200);

    try {
      const before = await request.get(
        `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent(name)}/role-assignments`,
        { headers },
      );
      expect(before.status(), "list assignment types").toBe(200);
      const beforeRows = assignmentRows(await before.json());
      const target = beforeRows.find((row) => isMutable(row) && String(row.roleName || "").trim());
      expect(target, "seeded workflow has no Reader or Assignee").toBeTruthy();
      const nextOn = !flagOf(target, "inbox");
      const nextChoice = nextOn ? "on" : "off";
      const nextAttr = nextOn ? "true" : "false";

      const locked = beforeRows.find((row) => !isMutable(row) && String(row.roleName || "").trim());
      if (locked) {
        const rejected = await request.put(
          `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent(name)}/steps/${encodeURIComponent(locked.stepName)}/role-inbox`,
          {
            headers,
            data: {
              WorkflowStepRoleInboxWrite: {
                roleName: locked.roleName,
                inbox: !flagOf(locked, "inbox"),
              },
            },
          },
        );
        expect(rejected.status(), "Admin or None inbox is 409").toBe(409);
      }

      await loginAsAdmin(page);
      await openWorkflowsCatalog(page);
      await page
        .locator(catalogOpenByExactName("developer-wf-open", "data-wf-name", name))
        .click();
      await expect(page.locator('[data-testid="developer-wf-detail-title"]')).toContainText(
        name,
        { timeout: 30_000 },
      );
      await expect(page.locator('[data-testid="developer-wf-role-inbox"]')).toBeVisible({
        timeout: 30_000,
      });
      const shown = await readInbox(page);
      const shownTarget = shown.find(
        (row) =>
          row.step.toLowerCase() === String(target.stepName).trim().toLowerCase() &&
          row.role.toLowerCase() === String(target.roleName).trim().toLowerCase(),
      );
      expect(shownTarget, "table row for the target role").toBeTruthy();
      expect(shownTarget.inbox).toBe(flagOf(target, "inbox") ? "true" : "false");

      let puts = 0;
      page.on("request", (req) => {
        if (isInboxPut(req.url(), req.method())) {
          puts += 1;
        }
      });
      await page.locator('[data-testid="developer-wf-role-inbox-value"]').selectOption(nextChoice);
      expect((await readInbox(page)).map((row) => row.inbox)).toEqual(
        shown.map((row) => row.inbox),
      );
      await page.locator('[data-testid="developer-wf-role-inbox-cancel"]').click();
      expect(puts).toBe(0);
      await expect(page.locator('[data-testid="developer-wf-role-inbox-value"]')).toHaveValue(
        flagOf(target, "inbox") ? "on" : "off",
      );
      expect((await readInbox(page)).map((row) => row.inbox)).toEqual(
        shown.map((row) => row.inbox),
      );

      await page.locator('[data-testid="developer-wf-role-inbox-value"]').selectOption(nextChoice);
      const saved = page.waitForResponse(
        (res) => isInboxPut(res.url(), res.request().method()),
      );
      await page.locator('[data-testid="developer-wf-role-inbox-confirm"]').click();
      expect((await saved).status()).toBe(200);
      await expect(page.locator('[data-testid="developer-wf-role-inbox-notice"]')).toBeVisible({
        timeout: 30_000,
      });
      await expect(page.locator('[data-testid="developer-wf-role-inbox-error"]')).toHaveCount(0);

      const afterUi = await readInbox(page);
      expect(afterUi.map((row) => `${row.step}\t${row.role}\t${row.type}\t${row.notify}`)).toEqual(
        shown.map((row) => `${row.step}\t${row.role}\t${row.type}\t${row.notify}`),
      );
      for (const row of afterUi) {
        const same =
          row.step.toLowerCase() === String(target.stepName).trim().toLowerCase() &&
          row.role.toLowerCase() === String(target.roleName).trim().toLowerCase();
        if (same) {
          expect(row.inbox).toBe(nextAttr);
        } else {
          const prior = shown.find(
            (beforeRow) => beforeRow.step === row.step && beforeRow.role === row.role,
          );
          expect(row.inbox).toBe(prior.inbox);
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
      expect(flagOf(changed, "inbox")).toBe(nextOn);
      expect(flagOf(changed, "notify")).toBe(flagOf(target, "notify"));
      expect(String(changed.assignmentType || "").toUpperCase()).toBe(
        String(target.assignmentType || "").toUpperCase(),
      );
      for (const row of afterRows) {
        if (rowIdentity(row).toLowerCase() === rowIdentity(target).toLowerCase()) {
          continue;
        }
        const prior = beforeRows.find(
          (beforeRow) => rowIdentity(beforeRow).toLowerCase() === rowIdentity(row).toLowerCase(),
        );
        expect(flagOf(row, "inbox")).toBe(flagOf(prior, "inbox"));
        expect(flagOf(row, "notify")).toBe(flagOf(prior, "notify"));
        expect(String(row.assignmentType || "").toUpperCase()).toBe(
          String(prior.assignmentType || "").toUpperCase(),
        );
      }

      const unchanged = await request.put(
        `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent(name)}/steps/${encodeURIComponent(target.stepName)}/role-inbox`,
        {
          headers,
          data: {
            WorkflowStepRoleInboxWrite: {
              roleName: target.roleName,
              inbox: nextOn,
            },
          },
        },
      );
      expect(unchanged.status()).toBe(400);
      const blank = await request.put(
        `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent(name)}/steps/${encodeURIComponent(target.stepName)}/role-inbox`,
        {
          headers,
          data: {
            WorkflowStepRoleInboxWrite: {
              roleName: " ",
              inbox: !nextOn,
            },
          },
        },
      );
      expect(blank.status()).toBe(400);
      const missing = await request.put(
        `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent(name)}/steps/${encodeURIComponent("Missing Step")}/role-inbox`,
        {
          headers,
          data: {
            WorkflowStepRoleInboxWrite: {
              roleName: target.roleName,
              inbox: !nextOn,
            },
          },
        },
      );
      expect(missing.status()).toBe(404);
      guards.assertClean();
    } finally {
      await request.delete(
        `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent(name)}`,
        { headers },
      );
    }
  });

  test("packaged workflows do not offer inbox and reject the write", async ({
    page,
    request,
  }) => {
    test.setTimeout(180_000);
    const guards = attachConsoleGuards(page);
    const headers = jsonHeaders();
    const listed = await request.get(
      `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent("Default Workflow")}/role-assignments`,
      { headers },
    );
    expect(listed.status()).toBe(200);
    const rows = assignmentRows(await listed.json());
    const target = rows.find((row) => isMutable(row));
    expect(target, "Default Workflow has no Reader or Assignee").toBeTruthy();

    const packaged = await request.put(
      `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent("Default Workflow")}/steps/${encodeURIComponent(target.stepName)}/role-inbox`,
      {
        headers,
        data: {
          WorkflowStepRoleInboxWrite: {
            roleName: target.roleName,
            inbox: !flagOf(target, "inbox"),
          },
        },
      },
    );
    expect(packaged.status()).toBe(403);
    const still = await request.get(
      `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent("Default Workflow")}/role-assignments`,
      { headers },
    );
    const afterRows = assignmentRows(await still.json());
    const same = afterRows.find(
      (row) => rowIdentity(row).toLowerCase() === rowIdentity(target).toLowerCase(),
    );
    expect(flagOf(same, "inbox")).toBe(flagOf(target, "inbox"));
    expect(flagOf(same, "notify")).toBe(flagOf(target, "notify"));

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
    await expect(page.locator('[data-testid="developer-wf-role-inbox-confirm"]')).toHaveCount(0);
    guards.assertClean();
  });
});
