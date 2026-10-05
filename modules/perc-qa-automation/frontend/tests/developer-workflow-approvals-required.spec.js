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
 * Developer → Workflows: set how many approvals one regular transition requires
 * (slice 70 / #5197 / parent #1690).
 *
 * The graph shows the new count only after save. Cancel does not call the
 * server. HTTP 400 and 403 leave the stored count. A stored negative count
 * (each-role approval) is 409 when the H2 cell has one; otherwise that status
 * is covered by WorkflowsAdaptorTransitionApprovalsTest.
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… TEST_DB_TYPE=h2 TEST_PRODUCT=cms \
 *     npm run test:surface -- --path tests/developer-workflow-approvals-required.spec.js
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

function graphEdges(body) {
  const graph = body && body.WorkflowGraph ? body.WorkflowGraph : body || {};
  return Array.isArray(graph.edges) ? graph.edges : [];
}

function regularEdges(body) {
  return graphEdges(body).filter(
    (edge) => edge && edge.aging !== true && typeof edge.approvalsRequired === "number",
  );
}

function edgeText(edge) {
  return `${edge.from} — ${edge.label} → ${edge.to}`;
}

function edgeIdentity(edge) {
  return `${edge.from}|${edge.label}|${edge.to}`.toLowerCase();
}

function approvalsUrl(name, edge) {
  const q = new URLSearchParams({
    from: edge.from,
    label: edge.label,
    to: edge.to,
  });
  return `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent(name)}/transitions/approvals-required?${q.toString()}`;
}

function graphUrl(name) {
  return `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent(name)}/graph`;
}

function edgeRow(page, edge) {
  return page
    .locator('[data-testid="developer-wf-graph-edges"] li')
    .filter({ hasText: edgeText(edge) });
}

test.describe("Developer workflow approvals required (#5197)", () => {
  test("Admin sets a non-negative approval count only after save", async ({
    page,
    request,
  }) => {
    test.setTimeout(180_000);
    const guards = attachConsoleGuards(page);
    const headers = jsonHeaders();
    const name = uniqueWorkflowName("Nightly Appr");
    try {
      const created = await request.post(`${BASE_URL}/Rhythmyx/services/workflows`, {
        headers,
        data: { WorkflowCreate: { name, description: "slice 70" } },
      });
      expect(created.status(), await created.text()).toBe(200);

      const graphRes = await request.get(graphUrl(name), { headers });
      expect(graphRes.status()).toBe(200);
      const beforeEdges = regularEdges(await graphRes.json());
      expect(beforeEdges.length, "custom workflow has a regular transition").toBeGreaterThan(0);
      const target = beforeEdges[0];
      const stored = target.approvalsRequired;
      const next = stored + 1;
      const sibling = beforeEdges.find((edge) => edgeIdentity(edge) !== edgeIdentity(target));

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
      const count = row.locator('[data-testid^="developer-wf-graph-approvals-"]');
      await expect(count).toHaveAttribute("data-approvals", String(stored));
      const comment = row.locator('[data-testid^="developer-wf-graph-comment-"]');
      const commentWasChecked = await comment.isChecked();

      let approvalPuts = 0;
      const countPuts = (req) => {
        if (
          req.method() === "PUT" &&
          req.url().includes("/transitions/approvals-required")
        ) {
          approvalPuts += 1;
        }
      };
      page.on("request", countPuts);
      await row.locator('[data-testid^="developer-wf-approvals-edit-"]').click();
      await page.locator('[data-testid="developer-wf-approvals-value"]').fill(String(next));
      await page.locator('[data-testid="developer-wf-approvals-cancel"]').click();
      expect(approvalPuts, "cancel does not write").toBe(0);
      await expect(count).toHaveAttribute("data-approvals", String(stored));
      await expect(page.locator('[data-testid="developer-wf-graph-notice"]')).toHaveCount(0);

      await row.locator('[data-testid^="developer-wf-approvals-edit-"]').click();
      await page.locator('[data-testid="developer-wf-approvals-value"]').fill("-1");
      await page.locator('[data-testid="developer-wf-approvals-save"]').click();
      expect(approvalPuts, "a negative draft does not write").toBe(0);
      await expect(page.locator('[data-testid="developer-wf-graph-error"]')).toContainText(
        /non-negative/i,
      );
      await expect(count).toHaveAttribute("data-approvals", String(stored));

      await page.locator('[data-testid="developer-wf-approvals-value"]').fill(String(next));
      await page.locator('[data-testid="developer-wf-approvals-save"]').click();
      await expect(page.locator('[data-testid="developer-wf-graph-notice"]')).toContainText(
        /Approvals required saved/i,
        { timeout: 30_000 },
      );
      expect(approvalPuts).toBe(1);
      await expect(count).toHaveAttribute("data-approvals", String(next));
      expect(await comment.isChecked()).toBe(commentWasChecked);
      await expect(row).toContainText(edgeText(target));
      if (sibling) {
        const siblingCount = edgeRow(page, sibling).locator(
          '[data-testid^="developer-wf-graph-approvals-"]',
        );
        await expect(siblingCount).toHaveAttribute(
          "data-approvals",
          String(sibling.approvalsRequired),
        );
      }
      page.off("request", countPuts);

      await page.locator('[data-testid="developer-wf-back"]').click();
      await page
        .locator(catalogOpenByExactName("developer-wf-open", "data-wf-name", name))
        .click();
      const reloaded = edgeRow(page, target).locator(
        '[data-testid^="developer-wf-graph-approvals-"]',
      );
      await expect(reloaded).toHaveAttribute("data-approvals", String(next), {
        timeout: 30_000,
      });
      const reloadedComment = edgeRow(page, target).locator(
        '[data-testid^="developer-wf-graph-comment-"]',
      );
      if (commentWasChecked) {
        await expect(reloadedComment).toBeChecked();
      } else {
        await expect(reloadedComment).not.toBeChecked();
      }

      const afterUi = await request.get(graphUrl(name), { headers });
      expect(afterUi.status()).toBe(200);
      const savedEdges = regularEdges(await afterUi.json());
      const saved = savedEdges.find((edge) => edgeIdentity(edge) === edgeIdentity(target));
      expect(saved.approvalsRequired).toBe(next);
      expect(saved.commentRequired === true).toBe(commentWasChecked);
      expect(saved.label).toBe(target.label);
      expect(saved.to).toBe(target.to);
      if (sibling) {
        const still = savedEdges.find((edge) => edgeIdentity(edge) === edgeIdentity(sibling));
        expect(still.approvalsRequired).toBe(sibling.approvalsRequired);
      }

      const negative = await request.put(approvalsUrl(name, target), {
        headers,
        data: { WorkflowTransitionApprovals: { approvalsRequired: -1 } },
      });
      expect(negative.status(), "negative count is 400").toBe(400);
      const unchanged = await request.put(approvalsUrl(name, target), {
        headers,
        data: { WorkflowTransitionApprovals: { approvalsRequired: next } },
      });
      expect(unchanged.status(), "unchanged count is 400").toBe(400);
      const blankFrom = await request.put(
        `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent(name)}/transitions/approvals-required?from=%20&label=${encodeURIComponent(target.label)}`,
        {
          headers,
          data: { WorkflowTransitionApprovals: { approvalsRequired: next + 1 } },
        },
      );
      expect(blankFrom.status(), "blank from is 400").toBe(400);
      const after400 = regularEdges(await (await request.get(graphUrl(name), { headers })).json());
      const kept = after400.find((edge) => edgeIdentity(edge) === edgeIdentity(target));
      expect(kept.approvalsRequired).toBe(next);
      expect(kept.commentRequired === true).toBe(commentWasChecked);

      guards.assertClean();
    } finally {
      await request.delete(
        `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent(name)}`,
        { headers },
      );
    }
  });

  test("packaged workflows keep the previous approval count on HTTP 403", async ({
    page,
    request,
  }) => {
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
      const edges = regularEdges(await listed.json());
      if (edges.length === 0) {
        continue;
      }
      checked += 1;
      const target = edges[0];
      const rejected = await request.put(approvalsUrl(packagedName, target), {
        headers,
        data: { WorkflowTransitionApprovals: { approvalsRequired: target.approvalsRequired + 1 } },
      });
      expect(rejected.status(), `${packagedName} approvals is 403`).toBe(403);
      const after = regularEdges(
        await (await request.get(graphUrl(packagedName), { headers })).json(),
      );
      const same = after.find((edge) => edgeIdentity(edge) === edgeIdentity(target));
      expect(same.approvalsRequired).toBe(target.approvalsRequired);
      expect(same.commentRequired === true).toBe(target.commentRequired === true);
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
    await expect(page.locator('[data-testid^="developer-wf-approvals-edit-"]')).toHaveCount(0);
    guards.assertClean();
  });

  test("a stored each-role approval count stays on HTTP 409 when the cell has one", async ({
    request,
  }) => {
    test.setTimeout(120_000);
    const headers = jsonHeaders();
    const names = [...PACKAGED];
    const listed = await request.get(
      `${BASE_URL}/Rhythmyx/services/workflowmanagement/workflows/metadata`,
      { headers },
    );
    if (listed.ok()) {
      const body = await listed.json();
      const rows = body.workflows || body.WorkflowList || body || [];
      if (Array.isArray(rows)) {
        for (const row of rows) {
          const rowName = row.workflowName || row.name;
          if (rowName && !names.includes(rowName)) {
            names.push(rowName);
          }
        }
      }
    }
    for (const workflowName of names) {
      const graphRes = await request.get(graphUrl(workflowName), { headers });
      if (!graphRes.ok()) {
        continue;
      }
      const negative = regularEdges(await graphRes.json()).find(
        (edge) => edge.approvalsRequired < 0,
      );
      if (!negative) {
        continue;
      }
      const rejected = await request.put(approvalsUrl(workflowName, negative), {
        headers,
        data: { WorkflowTransitionApprovals: { approvalsRequired: 1 } },
      });
      expect(rejected.status(), `${workflowName} each-role is 409`).toBe(409);
      const after = regularEdges(
        await (await request.get(graphUrl(workflowName), { headers })).json(),
      );
      const same = after.find((edge) => edgeIdentity(edge) === edgeIdentity(negative));
      expect(same.approvalsRequired).toBe(negative.approvalsRequired);
      return;
    }
    test.info().annotations.push({
      type: "note",
      description:
        "H2 has no TRANSITIONAPPROVALSREQUIRED below zero. HTTP 409 is covered by WorkflowsAdaptorTransitionApprovalsTest.",
    });
  });
});
