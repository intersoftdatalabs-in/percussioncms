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
 * Developer workflow step role adhoc type (slice 69 / #5196 / parent #1690).
 *
 * Admin sets disabled, enabled, or anonymous on one Reader or Assignee already
 * assigned to one step of a custom workflow. The table shows that type only
 * after the server accepts it. Cancel does not call the server. Choosing the
 * stored type does not enable confirm. Packaged workflows do not offer the
 * confirm. HTTP 400/403/409 keep the previous type. Assignment type, notify,
 * and inbox stay unchanged. A role that is not on the step is not added.
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… \
 *     npm run test:surface -- --path tests/developer-workflow-step-role-adhoc.spec.js
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

const ADHOC_TYPES = ["disabled", "enabled", "anonymous"];
// Installed workflow name is LocalContent. The server also treats the
// display label "Local Content" as packaged when that is the stored name.
const PACKAGED = ["Default Workflow", "Simple Workflow", "LocalContent"];

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

function adhocOf(row) {
  const raw = String((row && row.adhocType) || "")
    .trim()
    .toLowerCase();
  return ADHOC_TYPES.includes(raw) ? raw : "disabled";
}

function nextAdhoc(current) {
  const index = ADHOC_TYPES.indexOf(current);
  return ADHOC_TYPES[(index + 1) % ADHOC_TYPES.length];
}

function isMutable(row) {
  const type = String((row && row.assignmentType) || "").trim().toUpperCase();
  return type === "READER" || type === "ASSIGNEE";
}

function isAdhocPut(url, method) {
  return (
    method === "PUT" &&
    /\/services\/workflows\/[^/]+\/steps\/[^/]+\/role-adhoc(?:\?|$)/.test(url)
  );
}

async function readRoles(page) {
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
      adhoc: await page
        .locator(`[data-testid="developer-wf-role-adhoc-${i}"]`)
        .getAttribute("data-adhoc"),
    });
  }
  return out;
}

function fingerprint(row) {
  return `${row.step}\t${row.role}\t${row.type}\t${row.notify}\t${row.inbox}`;
}

test.describe("Developer workflow step role adhoc (slice 69 / #5196)", () => {
  test("confirm changes one type; cancel does not call the server", async ({
    page,
    request,
  }) => {
    test.setTimeout(180_000);
    const guards = attachConsoleGuards(page);
    const name = uniqueWorkflowName("Nightly Adh");
    const headers = jsonHeaders();
    const seeded = await request.post(`${BASE_URL}/Rhythmyx/services/workflows`, {
      headers,
      data: { WorkflowCreate: { name, description: "adhoc" } },
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
        (row) => isMutable(row) && String(row.roleName || "").trim(),
      );
      expect(target, "seeded workflow has no Reader or Assignee").toBeTruthy();
      const stored = adhocOf(target);
      const requested = nextAdhoc(stored);

      const locked = beforeRows.find(
        (row) => !isMutable(row) && String(row.roleName || "").trim(),
      );
      if (locked) {
        const rejected = await request.put(
          `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent(name)}/steps/${encodeURIComponent(locked.stepName)}/role-adhoc`,
          {
            headers,
            data: {
              WorkflowStepRoleAdhocWrite: {
                roleName: locked.roleName,
                adhocType: nextAdhoc(adhocOf(locked)),
              },
            },
          },
        );
        expect(rejected.status(), "Admin or None adhoc is 409").toBe(409);
        const lockedStill = assignmentRows(
          await (
            await request.get(
              `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent(name)}/role-assignments`,
              { headers },
            )
          ).json(),
        );
        const lockedRow = lockedStill.find(
          (row) => rowIdentity(row).toLowerCase() === rowIdentity(locked).toLowerCase(),
        );
        expect(adhocOf(lockedRow)).toBe(adhocOf(locked));
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
      await expect(page.locator('[data-testid="developer-wf-role-adhoc"]')).toBeVisible({
        timeout: 30_000,
      });
      const shown = await readRoles(page);
      const shownTarget = shown.find(
        (row) =>
          row.step.toLowerCase() === String(target.stepName).trim().toLowerCase() &&
          row.role.toLowerCase() === String(target.roleName).trim().toLowerCase(),
      );
      expect(shownTarget, "table row for the target role").toBeTruthy();
      expect(shownTarget.adhoc).toBe(stored);

      let puts = 0;
      page.on("request", (req) => {
        if (isAdhocPut(req.url(), req.method())) {
          puts += 1;
        }
      });
      const value = page.locator('[data-testid="developer-wf-role-adhoc-value"]');
      const confirm = page.locator('[data-testid="developer-wf-role-adhoc-confirm"]');
      await page
        .locator('[data-testid="developer-wf-role-adhoc-step"]')
        .selectOption(String(target.stepName).trim());
      await page
        .locator('[data-testid="developer-wf-role-adhoc-role"]')
        .selectOption(String(target.roleName).trim());
      await expect(value).toHaveValue(stored);
      await expect(confirm).toBeDisabled();
      await value.selectOption(requested);
      expect((await readRoles(page)).map((row) => row.adhoc)).toEqual(
        shown.map((row) => row.adhoc),
      );
      await expect(confirm).toBeEnabled();
      await value.selectOption(stored);
      await expect(confirm).toBeDisabled();
      expect(puts).toBe(0);

      await value.selectOption(requested);
      await page.locator('[data-testid="developer-wf-role-adhoc-cancel"]').click();
      expect(puts).toBe(0);
      await expect(value).toHaveValue(stored);
      await expect(confirm).toBeDisabled();
      expect((await readRoles(page)).map((row) => row.adhoc)).toEqual(
        shown.map((row) => row.adhoc),
      );

      await value.selectOption(requested);
      const saved = page.waitForResponse(
        (res) => isAdhocPut(res.url(), res.request().method()),
      );
      await confirm.click();
      expect((await saved).status()).toBe(200);
      await expect(page.locator('[data-testid="developer-wf-role-adhoc-notice"]')).toBeVisible({
        timeout: 30_000,
      });
      await expect(page.locator('[data-testid="developer-wf-role-adhoc-error"]')).toHaveCount(0);

      const afterUi = await readRoles(page);
      expect(afterUi.map(fingerprint)).toEqual(shown.map(fingerprint));
      for (const row of afterUi) {
        const same =
          row.step.toLowerCase() === String(target.stepName).trim().toLowerCase() &&
          row.role.toLowerCase() === String(target.roleName).trim().toLowerCase();
        if (same) {
          expect(row.adhoc).toBe(requested);
        } else {
          const prior = shown.find(
            (beforeRow) => beforeRow.step === row.step && beforeRow.role === row.role,
          );
          expect(row.adhoc).toBe(prior.adhoc);
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
      expect(adhocOf(changed)).toBe(requested);
      expect(flagOf(changed, "notify")).toBe(flagOf(target, "notify"));
      expect(flagOf(changed, "inbox")).toBe(flagOf(target, "inbox"));
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
        expect(adhocOf(row)).toBe(adhocOf(prior));
        expect(flagOf(row, "notify")).toBe(flagOf(prior, "notify"));
        expect(flagOf(row, "inbox")).toBe(flagOf(prior, "inbox"));
        expect(String(row.assignmentType || "").toUpperCase()).toBe(
          String(prior.assignmentType || "").toUpperCase(),
        );
      }

      const third = nextAdhoc(requested);
      const cycled = await request.put(
        `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent(name)}/steps/${encodeURIComponent(target.stepName)}/role-adhoc`,
        {
          headers,
          data: {
            WorkflowStepRoleAdhocWrite: {
              roleName: target.roleName,
              adhocType: third,
            },
          },
        },
      );
      expect(cycled.status(), "second allowed adhoc value").toBe(200);
      const restored = await request.put(
        `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent(name)}/steps/${encodeURIComponent(target.stepName)}/role-adhoc`,
        {
          headers,
          data: {
            WorkflowStepRoleAdhocWrite: {
              roleName: target.roleName,
              adhocType: stored,
            },
          },
        },
      );
      expect(restored.status(), "return to the original adhoc type").toBe(200);

      const unchanged = await request.put(
        `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent(name)}/steps/${encodeURIComponent(target.stepName)}/role-adhoc`,
        {
          headers,
          data: {
            WorkflowStepRoleAdhocWrite: {
              roleName: target.roleName,
              adhocType: stored,
            },
          },
        },
      );
      expect(unchanged.status()).toBe(400);
      const blank = await request.put(
        `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent(name)}/steps/${encodeURIComponent(target.stepName)}/role-adhoc`,
        {
          headers,
          data: {
            WorkflowStepRoleAdhocWrite: {
              roleName: " ",
              adhocType: requested,
            },
          },
        },
      );
      expect(blank.status()).toBe(400);
      const invalid = await request.put(
        `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent(name)}/steps/${encodeURIComponent(target.stepName)}/role-adhoc`,
        {
          headers,
          data: {
            WorkflowStepRoleAdhocWrite: {
              roleName: target.roleName,
              adhocType: "maybe",
            },
          },
        },
      );
      expect(invalid.status()).toBe(400);
      const missing = await request.put(
        `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent(name)}/steps/${encodeURIComponent("Missing Step")}/role-adhoc`,
        {
          headers,
          data: {
            WorkflowStepRoleAdhocWrite: {
              roleName: target.roleName,
              adhocType: requested,
            },
          },
        },
      );
      expect(missing.status()).toBe(404);
      const absentRole = "Not On This Step";
      const notOnStep = await request.put(
        `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent(name)}/steps/${encodeURIComponent(target.stepName)}/role-adhoc`,
        {
          headers,
          data: {
            WorkflowStepRoleAdhocWrite: {
              roleName: absentRole,
              adhocType: requested,
            },
          },
        },
      );
      expect(notOnStep.status()).toBe(404);
      const finalRows = assignmentRows(
        await (
          await request.get(
            `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent(name)}/role-assignments`,
            { headers },
          )
        ).json(),
      );
      expect(finalRows.map(rowIdentity).sort()).toEqual(beforeRows.map(rowIdentity).sort());
      expect(
        finalRows.some((row) => String(row.roleName || "").trim() === absentRole),
      ).toBe(false);
      const kept = finalRows.find(
        (row) => rowIdentity(row).toLowerCase() === rowIdentity(target).toLowerCase(),
      );
      expect(adhocOf(kept)).toBe(stored);
      expect(flagOf(kept, "notify")).toBe(flagOf(target, "notify"));
      expect(flagOf(kept, "inbox")).toBe(flagOf(target, "inbox"));
      expect(String(kept.assignmentType || "").toUpperCase()).toBe(
        String(target.assignmentType || "").toUpperCase(),
      );
      guards.assertClean();
    } finally {
      await request.delete(
        `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent(name)}`,
        { headers },
      );
    }
  });

  test("packaged workflows do not offer adhoc and reject the write", async ({
    page,
    request,
  }) => {
    test.setTimeout(240_000);
    const guards = attachConsoleGuards(page);
    const headers = jsonHeaders();

    for (const packagedName of PACKAGED) {
      const listed = await request.get(
        `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent(packagedName)}/role-assignments`,
        { headers },
      );
      expect(listed.status(), packagedName).toBe(200);
      const rows = assignmentRows(await listed.json());
      const target = rows.find((row) => isMutable(row));
      expect(target, `${packagedName} has no Reader or Assignee`).toBeTruthy();
      const stored = adhocOf(target);
      const rejected = await request.put(
        `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent(packagedName)}/steps/${encodeURIComponent(target.stepName)}/role-adhoc`,
        {
          headers,
          data: {
            WorkflowStepRoleAdhocWrite: {
              roleName: target.roleName,
              adhocType: nextAdhoc(stored),
            },
          },
        },
      );
      expect(rejected.status(), `${packagedName} adhoc is 403`).toBe(403);
      const still = await request.get(
        `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent(packagedName)}/role-assignments`,
        { headers },
      );
      const afterRows = assignmentRows(await still.json());
      const same = afterRows.find(
        (row) => rowIdentity(row).toLowerCase() === rowIdentity(target).toLowerCase(),
      );
      expect(adhocOf(same)).toBe(stored);
      expect(flagOf(same, "notify")).toBe(flagOf(target, "notify"));
      expect(flagOf(same, "inbox")).toBe(flagOf(target, "inbox"));
      expect(String(same.assignmentType || "").toUpperCase()).toBe(
        String(target.assignmentType || "").toUpperCase(),
      );
    }

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
    await expect(page.locator('[data-testid="developer-wf-role-adhoc-confirm"]')).toHaveCount(0);
    guards.assertClean();
  });
});
