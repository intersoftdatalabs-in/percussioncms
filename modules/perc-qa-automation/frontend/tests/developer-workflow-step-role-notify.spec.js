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
 * Developer workflow step role notify (slice 65 / #5154 / parent #1690).
 *
 * Admin turns notify on or off for one role already assigned to one step of a
 * custom workflow. The table shows that flag only after the server accepts it.
 * Cancel does not call the server. Packaged workflows do not offer the confirm.
 * HTTP 400/403/404 do not claim success. Assignment type is unchanged.
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… \
 *     npm run test:surface -- --path tests/developer-workflow-step-role-notify.spec.js
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

function notifyOf(row) {
  return row && (row.notify === true || row.notify === "true" || row.notify === "y");
}

function isNotifyPut(url, method) {
  return (
    method === "PUT" &&
    /\/services\/workflows\/[^/]+\/steps\/[^/]+\/role-notify(?:\?|$)/.test(url)
  );
}

async function readNotify(page) {
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
    });
  }
  return out;
}

test.describe("Developer workflow step role notify (slice 65 / #5154)", () => {
  test("confirm changes one flag; cancel does not call the server", async ({
    page,
    request,
  }) => {
    test.setTimeout(180_000);
    const guards = attachConsoleGuards(page);
    const name = uniqueWorkflowName("Nightly Nfy");
    const headers = jsonHeaders();
    const seeded = await request.post(`${BASE_URL}/Rhythmyx/services/workflows`, {
      headers,
      data: { WorkflowCreate: { name, description: "notify" } },
    });
    expect(seeded.status(), "seed custom workflow").toBe(200);

    try {
      const before = await request.get(
        `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent(name)}/role-assignments`,
        { headers },
      );
      expect(before.status(), "list assignment types").toBe(200);
      const beforeRows = assignmentRows(await before.json());
      const target = beforeRows.find(
        (row) => String(row.stepName || "").trim() && String(row.roleName || "").trim(),
      );
      expect(target, "seeded workflow has no assigned role").toBeTruthy();
      const nextOn = !notifyOf(target);
      const nextChoice = nextOn ? "on" : "off";
      const nextAttr = nextOn ? "true" : "false";

      await loginAsAdmin(page);
      await openWorkflowsCatalog(page);
      await page
        .locator(catalogOpenByExactName("developer-wf-open", "data-wf-name", name))
        .click();
      await expect(page.locator('[data-testid="developer-wf-detail-title"]')).toContainText(
        name,
        { timeout: 30_000 },
      );
      await expect(page.locator('[data-testid="developer-wf-role-notify"]')).toBeVisible({
        timeout: 30_000,
      });
      const shown = await readNotify(page);
      const shownTarget = shown.find(
        (row) =>
          row.step.toLowerCase() === String(target.stepName).trim().toLowerCase() &&
          row.role.toLowerCase() === String(target.roleName).trim().toLowerCase(),
      );
      expect(shownTarget, "table row for the target role").toBeTruthy();
      expect(shownTarget.notify).toBe(notifyOf(target) ? "true" : "false");

      let puts = 0;
      page.on("request", (req) => {
        if (isNotifyPut(req.url(), req.method())) {
          puts += 1;
        }
      });
      await page.locator('[data-testid="developer-wf-role-notify-value"]').selectOption(nextChoice);
      expect((await readNotify(page)).map((row) => row.notify)).toEqual(
        shown.map((row) => row.notify),
      );
      await page.locator('[data-testid="developer-wf-role-notify-cancel"]').click();
      expect(puts).toBe(0);
      await expect(page.locator('[data-testid="developer-wf-role-notify-value"]')).toHaveValue(
        notifyOf(target) ? "on" : "off",
      );
      expect((await readNotify(page)).map((row) => row.notify)).toEqual(
        shown.map((row) => row.notify),
      );

      await page.locator('[data-testid="developer-wf-role-notify-value"]').selectOption(nextChoice);
      expect((await readNotify(page)).find(
        (row) =>
          row.step.toLowerCase() === String(target.stepName).trim().toLowerCase() &&
          row.role.toLowerCase() === String(target.roleName).trim().toLowerCase(),
      ).notify).toBe(notifyOf(target) ? "true" : "false");
      const saved = page.waitForResponse(
        (res) => isNotifyPut(res.url(), res.request().method()),
      );
      await page.locator('[data-testid="developer-wf-role-notify-confirm"]').click();
      expect((await saved).status()).toBe(200);
      await expect(page.locator('[data-testid="developer-wf-role-notify-notice"]')).toBeVisible({
        timeout: 30_000,
      });
      await expect(page.locator('[data-testid="developer-wf-role-notify-error"]')).toHaveCount(0);

      const afterUi = await readNotify(page);
      expect(afterUi.map((row) => `${row.step}\t${row.role}\t${row.type}`)).toEqual(
        shown.map((row) => `${row.step}\t${row.role}\t${row.type}`),
      );
      for (const row of afterUi) {
        const same =
          row.step.toLowerCase() === String(target.stepName).trim().toLowerCase() &&
          row.role.toLowerCase() === String(target.roleName).trim().toLowerCase();
        if (same) {
          expect(row.notify).toBe(nextAttr);
        } else {
          const prior = shown.find(
            (beforeRow) => beforeRow.step === row.step && beforeRow.role === row.role,
          );
          expect(row.notify).toBe(prior.notify);
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
      expect(notifyOf(changed)).toBe(nextOn);
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
        expect(notifyOf(row)).toBe(notifyOf(prior));
        expect(String(row.assignmentType || "").toUpperCase()).toBe(
          String(prior.assignmentType || "").toUpperCase(),
        );
      }

      const unchanged = await request.put(
        `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent(name)}/steps/${encodeURIComponent(target.stepName)}/role-notify`,
        {
          headers,
          data: {
            WorkflowStepRoleNotifyWrite: {
              roleName: target.roleName,
              notify: nextOn,
            },
          },
        },
      );
      expect(unchanged.status()).toBe(400);
      const blank = await request.put(
        `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent(name)}/steps/${encodeURIComponent(target.stepName)}/role-notify`,
        {
          headers,
          data: {
            WorkflowStepRoleNotifyWrite: {
              roleName: " ",
              notify: !nextOn,
            },
          },
        },
      );
      expect(blank.status()).toBe(400);
      const missing = await request.put(
        `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent(name)}/steps/${encodeURIComponent("Missing Step")}/role-notify`,
        {
          headers,
          data: {
            WorkflowStepRoleNotifyWrite: {
              roleName: target.roleName,
              notify: !nextOn,
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

  test("packaged workflows do not offer notify and reject the write", async ({
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
    const target = rows.find(
      (row) => String(row.stepName || "").trim() && String(row.roleName || "").trim(),
    );
    expect(target, "Default Workflow has no assigned role").toBeTruthy();

    const packaged = await request.put(
      `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent("Default Workflow")}/steps/${encodeURIComponent(target.stepName)}/role-notify`,
      {
        headers,
        data: {
          WorkflowStepRoleNotifyWrite: {
            roleName: target.roleName,
            notify: !notifyOf(target),
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
    expect(notifyOf(same)).toBe(notifyOf(target));

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
    await expect(page.locator('[data-testid="developer-wf-role-notify-confirm"]')).toHaveCount(0);
    guards.assertClean();
  });
});
