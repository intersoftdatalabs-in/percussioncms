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
 * Developer → Workflows: restrict one regular transition to a single role
 * (slice 72 / #5233 / parent #1690).
 *
 * The graph shows that role, and not all roles, only after save. Cancel does
 * not call the server. HTTP 400, 403, and 409 leave allow-all in place.
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… TEST_DB_TYPE=h2 TEST_PRODUCT=cms \
 *     npm run test:surface -- --path tests/developer-workflow-transition-allowed-role.spec.js
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
}

function unwrapGraph(body) {
  return body && body.WorkflowGraph ? body.WorkflowGraph : body || {};
}

function graphEdges(body) {
  const graph = unwrapGraph(body);
  return Array.isArray(graph.edges) ? graph.edges : [];
}

function graphRoles(body) {
  const roles = unwrapGraph(body).roles;
  return asRoleList(roles);
}

/** Live JSON writes a one-element role list as a string. */
function asRoleList(raw) {
  if (typeof raw === "string") {
    const name = raw.trim();
    return name ? [name] : [];
  }
  if (!Array.isArray(raw)) {
    return [];
  }
  return raw.filter((name) => typeof name === "string" && name.trim());
}

function regularEdges(body) {
  return graphEdges(body).filter(
    (edge) => edge && edge.aging !== true && typeof edge.allowAllRoles === "boolean",
  );
}

function agingEdges(body) {
  return graphEdges(body).filter((edge) => edge && edge.aging === true);
}

function edgeText(edge) {
  return `${edge.from} — ${edge.label} → ${edge.to}`;
}

function edgeIdentity(edge) {
  return `${edge.from}|${edge.label}|${edge.to}`.toLowerCase();
}

function roleUrl(name, edge) {
  const q = new URLSearchParams({
    from: edge.from,
    label: edge.label,
    to: edge.to,
  });
  return `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent(name)}/transitions/allowed-role?${q.toString()}`;
}

function graphUrl(name) {
  return `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent(name)}/graph`;
}

function edgeRow(page, edge) {
  return page
    .locator('[data-testid="developer-wf-graph-edges"] li')
    .filter({ hasText: edgeText(edge) });
}

test.describe("Developer workflow transition allowed role (#5233)", () => {
  test("Admin restricts one transition to one role only after save", async ({
    page,
    request,
  }) => {
    test.setTimeout(180_000);
    const guards = attachConsoleGuards(page);
    const headers = jsonHeaders();
    const name = uniqueWorkflowName("Nightly Role");
    try {
      const created = await request.post(`${BASE_URL}/Rhythmyx/services/workflows`, {
        headers,
        data: { WorkflowCreate: { name, description: "slice 72" } },
      });
      expect(created.status(), await created.text()).toBe(200);

      const before = await (await request.get(graphUrl(name), { headers })).json();
      const copied = regularEdges(before);
      expect(copied.length, "custom workflow has a regular transition").toBeGreaterThan(0);
      const roles = graphRoles(before);
      expect(roles.length, "custom workflow has a role").toBeGreaterThan(0);
      const anchor = copied[0];
      const stamp = Date.now();
      const targetLabel = `Allow ${stamp}`.slice(0, 50);
      const siblingLabel = `Open ${stamp}`.slice(0, 50);
      for (const label of [targetLabel, siblingLabel]) {
        const added = await request.post(
          `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent(name)}/transitions`,
          {
            headers,
            data: {
              WorkflowTransitionWrite: { from: anchor.from, to: anchor.to, label },
            },
          },
        );
        expect(added.status(), await added.text()).toBe(200);
      }
      const opened = await (await request.get(graphUrl(name), { headers })).json();
      const edges = regularEdges(opened);
      const target = edges.find((edge) => edge.label === targetLabel);
      const sibling = edges.find((edge) => edge.label === siblingLabel);
      expect(target, "new transition allows every role").toBeTruthy();
      expect(sibling, "sibling new transition allows every role").toBeTruthy();
      expect(target.allowAllRoles).toBe(true);
      expect(sibling.allowAllRoles).toBe(true);
      const agingBefore = agingEdges(opened).map(edgeIdentity);
      const roleName = roles.find((role) => role !== "Admin" && role !== "None") || roles[0];

      await loginAsAdmin(page);
      await openWorkflowsCatalog(page);
      await page
        .locator(catalogOpenByExactName("developer-wf-open", "data-wf-name", name))
        .click();
      await expect(page.locator('[data-testid="developer-wf-detail"]')).toBeVisible({
        timeout: 30_000,
      });
      await expect(page.locator('[data-testid="developer-wf-graph-kind"]')).toContainText(
        /Custom workflow/i,
      );

      const row = edgeRow(page, target);
      await expect(row).toBeVisible();
      const badge = row.locator('[data-testid^="developer-wf-graph-roles-"]');
      await expect(badge).toHaveAttribute("data-allow-all", "true");
      await expect(badge).toContainText(/All roles/i);
      const comment = row.locator('[data-testid^="developer-wf-graph-comment-"]');
      const commentWasChecked = await comment.isChecked();
      const approvals = row.locator('[data-testid^="developer-wf-graph-approvals-"]');
      const storedApprovals = await approvals.getAttribute("data-approvals");
      await expect(page.locator('[data-testid="developer-wf-aging-edges"] [data-testid^="developer-wf-roles-edit-"]')).toHaveCount(0);

      let rolePuts = 0;
      const countPuts = (req) => {
        if (req.method() === "PUT" && req.url().includes("/transitions/allowed-role")) {
          rolePuts += 1;
        }
      };
      page.on("request", countPuts);
      await row.locator('[data-testid^="developer-wf-roles-edit-"]').click();
      await page.locator('[data-testid="developer-wf-roles-cancel"]').click();
      expect(rolePuts, "cancel does not write").toBe(0);
      await expect(badge).toHaveAttribute("data-allow-all", "true");
      await expect(badge).toContainText(/All roles/i);
      await expect(page.locator('[data-testid="developer-wf-graph-notice"]')).toHaveCount(0);

      await row.locator('[data-testid^="developer-wf-roles-edit-"]').click();
      await page.locator('[data-testid="developer-wf-roles-value"]').selectOption(roleName);
      await page.locator('[data-testid="developer-wf-roles-save"]').click();
      await expect(page.locator('[data-testid="developer-wf-graph-notice"]')).toContainText(
        /Transition role saved/i,
        { timeout: 30_000 },
      );
      expect(rolePuts).toBe(1);
      await expect(badge).toHaveAttribute("data-allow-all", "false");
      await expect(badge).toContainText(roleName);
      await expect(badge).not.toContainText(/All roles/i);
      await expect(row.locator('[data-testid^="developer-wf-roles-edit-"]')).toHaveCount(0);
      expect(await comment.isChecked()).toBe(commentWasChecked);
      await expect(approvals).toHaveAttribute("data-approvals", storedApprovals);
      if (sibling) {
        await expect(
          edgeRow(page, sibling).locator('[data-testid^="developer-wf-graph-roles-"]'),
        ).toHaveAttribute("data-allow-all", "true");
      }
      page.off("request", countPuts);

      await page.locator('[data-testid="developer-wf-back"]').click();
      await page
        .locator(catalogOpenByExactName("developer-wf-open", "data-wf-name", name))
        .click();
      await expect(
        edgeRow(page, target).locator('[data-testid^="developer-wf-graph-roles-"]'),
      ).toHaveAttribute("data-allow-all", "false", { timeout: 30_000 });
      await expect(
        edgeRow(page, target).locator('[data-testid^="developer-wf-graph-roles-"]'),
      ).toContainText(roleName);

      const afterUi = await request.get(graphUrl(name), { headers });
      expect(afterUi.status()).toBe(200);
      const afterBody = await afterUi.json();
      const saved = regularEdges(afterBody).find(
        (edge) => edgeIdentity(edge) === edgeIdentity(target),
      );
      expect(saved.allowAllRoles).toBe(false);
      expect(asRoleList(saved.allowedRoles)).toEqual([roleName]);
      expect(saved.label).toBe(target.label);
      expect(saved.to).toBe(target.to);
      expect(saved.approvalsRequired).toBe(target.approvalsRequired);
      expect(saved.defaultTransition).toBe(target.defaultTransition);
      if (sibling) {
        const stillOpen = regularEdges(afterBody).find(
          (edge) => edgeIdentity(edge) === edgeIdentity(sibling),
        );
        expect(stillOpen.allowAllRoles).toBe(true);
        expect(stillOpen.allowedRoles).toBeFalsy();
      }
      expect(agingEdges(afterBody).map(edgeIdentity)).toEqual(agingBefore);
      for (const aging of agingEdges(afterBody)) {
        expect(aging.allowAllRoles).toBeUndefined();
        expect(aging.allowedRoles).toBeUndefined();
      }

      const again = await request.put(roleUrl(name, target), {
        headers,
        data: { WorkflowTransitionAllowedRole: { roleName } },
      });
      expect(again.status(), "already restricted is 409").toBe(409);
      const marker = sibling
        ? await request.put(roleUrl(name, sibling), {
            headers,
            data: { WorkflowTransitionAllowedRole: { roleName: "*ALL*" } },
          })
        : await request.put(roleUrl(name, target), {
            headers,
            data: { WorkflowTransitionAllowedRole: { roleName: "*ALL*" } },
          });
      expect(marker.status(), "*ALL* is 409").toBe(409);
      const blank = sibling
        ? await request.put(roleUrl(name, sibling), {
            headers,
            data: { WorkflowTransitionAllowedRole: { roleName: " " } },
          })
        : await request.put(
            `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent(name)}/transitions/allowed-role?from=%20&label=${encodeURIComponent(target.label)}`,
            {
              headers,
              data: { WorkflowTransitionAllowedRole: { roleName } },
            },
          );
      expect(blank.status(), "blank role or from is 400").toBe(400);

      const afterFail = await (await request.get(graphUrl(name), { headers })).json();
      const kept = regularEdges(afterFail).find(
        (edge) => edgeIdentity(edge) === edgeIdentity(target),
      );
      expect(kept.allowAllRoles).toBe(false);
      expect(asRoleList(kept.allowedRoles)).toEqual([roleName]);
      if (sibling) {
        const stillOpen = regularEdges(afterFail).find(
          (edge) => edgeIdentity(edge) === edgeIdentity(sibling),
        );
        expect(stillOpen.allowAllRoles).toBe(true);
      }

      guards.assertClean();
    } finally {
      await request.delete(
        `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent(name)}`,
        { headers },
      );
    }
  });

  test("packaged workflows keep allow-all on HTTP 403", async ({ page, request }) => {
    test.setTimeout(180_000);
    const guards = attachConsoleGuards(page);
    const headers = jsonHeaders();
    let checked = 0;
    for (const packagedName of PACKAGED) {
      const listed = await request.get(graphUrl(packagedName), { headers });
      if (listed.status() === 404) {
        continue;
      }
      expect(listed.status(), packagedName).toBe(200);
      const body = await listed.json();
      const target = regularEdges(body).find((edge) => edge.allowAllRoles === true);
      if (!target) {
        continue;
      }
      const roleName = graphRoles(body)[0] || "Editor";
      checked += 1;
      const rejected = await request.put(roleUrl(packagedName, target), {
        headers,
        data: { WorkflowTransitionAllowedRole: { roleName } },
      });
      expect(rejected.status(), `${packagedName} role restriction is 403`).toBe(403);
      const after = regularEdges(
        await (await request.get(graphUrl(packagedName), { headers })).json(),
      );
      const same = after.find((edge) => edgeIdentity(edge) === edgeIdentity(target));
      expect(same.allowAllRoles).toBe(true);
      expect(same.label).toBe(target.label);
    }
    expect(checked, "at least one packaged workflow has an allow-all transition").toBeGreaterThan(
      0,
    );

    await loginAsAdmin(page);
    await openWorkflowsCatalog(page);
    await page
      .locator(catalogOpenByExactName("developer-wf-open", "data-wf-name", "Default Workflow"))
      .click();
    await expect(page.locator('[data-testid="developer-wf-graph-kind"]')).toContainText(
      /Packaged workflow/i,
      { timeout: 30_000 },
    );
    await expect(page.locator('[data-testid^="developer-wf-roles-edit-"]')).toHaveCount(0);
    guards.assertClean();
  });
});
