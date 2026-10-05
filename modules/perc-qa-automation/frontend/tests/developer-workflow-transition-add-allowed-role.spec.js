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
 * Developer → Workflows: add one more role to a transition that is already
 * restricted (slice 73 / #5234 / parent #1690).
 *
 * The graph lists both roles, and stays restricted, only after save. Cancel
 * does not call the server. HTTP 400, 403, and 409 leave the previous role
 * list in place.
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… TEST_DB_TYPE=h2 TEST_PRODUCT=cms \
 *     npm run test:surface -- --path tests/developer-workflow-transition-add-allowed-role.spec.js
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
  return asRoleList(unwrapGraph(body).roles);
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

function roleUrl(name, edge, add) {
  const q = new URLSearchParams({
    from: edge.from,
    label: edge.label,
    to: edge.to,
  });
  const action = add ? "allowed-roles" : "allowed-role";
  return `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent(name)}/transitions/${action}?${q.toString()}`;
}

function graphUrl(name) {
  return `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent(name)}/graph`;
}

function edgeRow(page, edge) {
  return page
    .locator('[data-testid="developer-wf-graph-edges"] li')
    .filter({ hasText: edgeText(edge) });
}

function pickTwoRoles(roles) {
  const preferred = roles.filter((role) => role !== "Admin" && role !== "None");
  const pool = preferred.length >= 2 ? preferred : roles;
  return pool.length >= 2 ? [pool[0], pool[1]] : [];
}

function sameRoles(actual, expected) {
  const left = asRoleList(actual).map((name) => name.toLowerCase()).sort();
  const right = expected.map((name) => name.toLowerCase()).sort();
  expect(left).toEqual(right);
}

test.describe("Developer workflow add one transition role (#5234)", () => {
  test("Admin adds a second role only after save and the transition stays restricted", async ({
    page,
    request,
  }) => {
    test.setTimeout(180_000);
    const guards = attachConsoleGuards(page);
    const headers = jsonHeaders();
    const name = uniqueWorkflowName("Nightly Add Role");
    try {
      const created = await request.post(`${BASE_URL}/Rhythmyx/services/workflows`, {
        headers,
        data: { WorkflowCreate: { name, description: "slice 73" } },
      });
      expect(created.status(), await created.text()).toBe(200);

      const before = await (await request.get(graphUrl(name), { headers })).json();
      const copied = regularEdges(before);
      expect(copied.length, "custom workflow has a regular transition").toBeGreaterThan(0);
      const [roleA, roleB] = pickTwoRoles(graphRoles(before));
      expect(roleB, "custom workflow has two roles").toBeTruthy();
      const anchor = copied[0];
      const stamp = Date.now();
      const targetLabel = `Add ${stamp}`.slice(0, 50);
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
      const target = regularEdges(opened).find((edge) => edge.label === targetLabel);
      const sibling = regularEdges(opened).find((edge) => edge.label === siblingLabel);
      expect(target.allowAllRoles).toBe(true);
      expect(sibling.allowAllRoles).toBe(true);
      const agingBefore = agingEdges(opened).map(edgeIdentity);

      const restricted = await request.put(roleUrl(name, target, false), {
        headers,
        data: { WorkflowTransitionAllowedRole: { roleName: roleA } },
      });
      expect(restricted.status(), await restricted.text()).toBe(200);
      const restrictedBody = await restricted.json();
      const restrictedEdge = regularEdges(restrictedBody).find(
        (edge) => edgeIdentity(edge) === edgeIdentity(target),
      );
      expect(restrictedEdge.allowAllRoles).toBe(false);
      sameRoles(restrictedEdge.allowedRoles, [roleA]);

      await loginAsAdmin(page);
      await openWorkflowsCatalog(page);
      await page
        .locator(catalogOpenByExactName("developer-wf-open", "data-wf-name", name))
        .click();
      await expect(page.locator('[data-testid="developer-wf-detail"]')).toBeVisible({
        timeout: 30_000,
      });

      const row = edgeRow(page, target);
      await expect(row).toBeVisible();
      const badge = row.locator('[data-testid^="developer-wf-graph-roles-"]');
      await expect(badge).toHaveAttribute("data-allow-all", "false");
      await expect(badge).toContainText(roleA);
      await expect(badge).not.toContainText(/All roles/i);
      await expect(row.locator('[data-testid^="developer-wf-roles-edit-"]')).toHaveCount(0);
      const comment = row.locator('[data-testid^="developer-wf-graph-comment-"]');
      const commentWasChecked = await comment.isChecked();
      const approvals = row.locator('[data-testid^="developer-wf-graph-approvals-"]');
      const storedApprovals = await approvals.getAttribute("data-approvals");

      let rolePosts = 0;
      const countPosts = (req) => {
        if (req.method() === "POST" && req.url().includes("/transitions/allowed-roles")) {
          rolePosts += 1;
        }
      };
      page.on("request", countPosts);
      await row.locator('[data-testid^="developer-wf-roles-add-"]').click();
      await page.locator('[data-testid="developer-wf-roles-add-cancel"]').click();
      expect(rolePosts, "cancel does not write").toBe(0);
      await expect(badge).toHaveAttribute("data-allow-all", "false");
      await expect(badge).toContainText(roleA);
      await expect(badge).not.toContainText(roleB);
      page.off("request", countPosts);

      await row.locator('[data-testid^="developer-wf-roles-add-"]').click();
      for (const status of [400, 403, 409]) {
        const failAdd = (route) => {
          if (route.request().method() !== "POST") {
            return route.continue();
          }
          return route.fulfill({
            status,
            contentType: "application/json",
            body: JSON.stringify({ message: "no" }),
          });
        };
        await page.route("**/transitions/allowed-roles**", failAdd);
        await page.locator('[data-testid="developer-wf-roles-add-save"]').click();
        await expect(page.locator('[data-testid="developer-wf-graph-error"]')).toBeVisible();
        await expect(page.locator('[data-testid="developer-wf-graph-notice"]')).toHaveCount(0);
        await expect(badge).toHaveAttribute("data-allow-all", "false");
        await expect(badge).toContainText(roleA);
        await expect(badge).not.toContainText(roleB);
        await expect(badge).not.toContainText(/All roles/i);
        await page.unroute("**/transitions/allowed-roles**", failAdd);
      }
      await page.locator('[data-testid="developer-wf-roles-add-cancel"]').click();

      await row.locator('[data-testid^="developer-wf-roles-add-"]').click();
      await page.locator('[data-testid="developer-wf-roles-add-value"]').selectOption(roleB);
      await page.locator('[data-testid="developer-wf-roles-add-save"]').click();
      await expect(page.locator('[data-testid="developer-wf-graph-notice"]')).toContainText(
        /Transition role added/i,
        { timeout: 30_000 },
      );
      await expect(badge).toHaveAttribute("data-allow-all", "false");
      await expect(badge).toContainText(roleA);
      await expect(badge).toContainText(roleB);
      await expect(badge).not.toContainText(/All roles/i);
      const spareRoles = graphRoles(opened).filter(
        (role) =>
          role.toLowerCase() !== roleA.toLowerCase() &&
          role.toLowerCase() !== roleB.toLowerCase(),
      );
      await expect(row.locator('[data-testid^="developer-wf-roles-add-"]')).toHaveCount(
        spareRoles.length === 0 ? 0 : 1,
      );
      expect(await comment.isChecked()).toBe(commentWasChecked);
      await expect(approvals).toHaveAttribute("data-approvals", storedApprovals);
      await expect(
        edgeRow(page, sibling).locator('[data-testid^="developer-wf-graph-roles-"]'),
      ).toHaveAttribute("data-allow-all", "true");

      await page.locator('[data-testid="developer-wf-back"]').click();
      await page
        .locator(catalogOpenByExactName("developer-wf-open", "data-wf-name", name))
        .click();
      const reopened = edgeRow(page, target).locator('[data-testid^="developer-wf-graph-roles-"]');
      await expect(reopened).toHaveAttribute("data-allow-all", "false", { timeout: 30_000 });
      await expect(reopened).toContainText(roleA);
      await expect(reopened).toContainText(roleB);

      const afterUi = await (await request.get(graphUrl(name), { headers })).json();
      const saved = regularEdges(afterUi).find(
        (edge) => edgeIdentity(edge) === edgeIdentity(target),
      );
      expect(saved.allowAllRoles).toBe(false);
      sameRoles(saved.allowedRoles, [roleA, roleB]);
      expect(saved.label).toBe(target.label);
      expect(saved.to).toBe(target.to);
      expect(saved.approvalsRequired).toBe(target.approvalsRequired);
      expect(saved.defaultTransition).toBe(target.defaultTransition);
      const stillOpen = regularEdges(afterUi).find(
        (edge) => edgeIdentity(edge) === edgeIdentity(sibling),
      );
      expect(stillOpen.allowAllRoles).toBe(true);
      expect(agingEdges(afterUi).map(edgeIdentity)).toEqual(agingBefore);

      const duplicate = await request.post(roleUrl(name, target, true), {
        headers,
        data: { WorkflowTransitionAllowedRole: { roleName: roleB } },
      });
      expect(duplicate.status(), "role already allowed is 409").toBe(409);
      const marker = await request.post(roleUrl(name, target, true), {
        headers,
        data: { WorkflowTransitionAllowedRole: { roleName: "*ALL*" } },
      });
      expect(marker.status(), "*ALL* is 409").toBe(409);
      const blank = await request.post(
        `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent(name)}/transitions/allowed-roles?from=%20&label=${encodeURIComponent(target.label)}`,
        {
          headers,
          data: { WorkflowTransitionAllowedRole: { roleName: roleB } },
        },
      );
      expect(blank.status(), "blank from is 400").toBe(400);
      const stillAllowAll = await request.post(roleUrl(name, sibling, true), {
        headers,
        data: { WorkflowTransitionAllowedRole: { roleName: roleB } },
      });
      expect(stillAllowAll.status(), "allow-all is not extended by add").toBe(409);
      const aging = agingEdges(afterUi)[0];
      if (aging) {
        const agingRejected = await request.post(roleUrl(name, aging, true), {
          headers,
          data: { WorkflowTransitionAllowedRole: { roleName: roleB } },
        });
        expect(agingRejected.status(), "aging is 400").toBe(400);
      }

      const afterFail = await (await request.get(graphUrl(name), { headers })).json();
      const kept = regularEdges(afterFail).find(
        (edge) => edgeIdentity(edge) === edgeIdentity(target),
      );
      expect(kept.allowAllRoles).toBe(false);
      sameRoles(kept.allowedRoles, [roleA, roleB]);
      const siblingKept = regularEdges(afterFail).find(
        (edge) => edgeIdentity(edge) === edgeIdentity(sibling),
      );
      expect(siblingKept.allowAllRoles).toBe(true);

      guards.assertClean();
    } finally {
      await request.delete(
        `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent(name)}`,
        { headers },
      );
    }
  });

  test("packaged workflows keep the previous role list on HTTP 403", async ({ page, request }) => {
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
      const target = regularEdges(body)[0];
      if (!target) {
        continue;
      }
      const roleName = graphRoles(body)[0] || "Editor";
      checked += 1;
      const beforeRoles = asRoleList(target.allowedRoles);
      const rejected = await request.post(roleUrl(packagedName, target, true), {
        headers,
        data: { WorkflowTransitionAllowedRole: { roleName } },
      });
      expect(rejected.status(), `${packagedName} add role is 403`).toBe(403);
      const after = regularEdges(
        await (await request.get(graphUrl(packagedName), { headers })).json(),
      );
      const same = after.find((edge) => edgeIdentity(edge) === edgeIdentity(target));
      expect(same.allowAllRoles).toBe(target.allowAllRoles);
      sameRoles(same.allowedRoles, beforeRoles);
      expect(same.label).toBe(target.label);
    }
    expect(checked, "at least one packaged workflow has a regular transition").toBeGreaterThan(0);

    await loginAsAdmin(page);
    await openWorkflowsCatalog(page);
    await page
      .locator(catalogOpenByExactName("developer-wf-open", "data-wf-name", "Default Workflow"))
      .click();
    await expect(page.locator('[data-testid="developer-wf-graph-kind"]')).toContainText(
      /Packaged workflow/i,
      { timeout: 30_000 },
    );
    await expect(page.locator('[data-testid^="developer-wf-roles-add-"]')).toHaveCount(0);
    guards.assertClean();
  });
});
