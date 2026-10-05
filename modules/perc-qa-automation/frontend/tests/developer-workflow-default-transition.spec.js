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
 * Developer → Workflows: mark one regular transition as the default (slice 71 /
 * #5198 / parent #1690).
 *
 * The graph shows the new default only after save. Cancel does not call the
 * server. HTTP 400, 403, and 409 leave the previous default in place.
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… TEST_DB_TYPE=h2 TEST_PRODUCT=cms \
 *     npm run test:surface -- --path tests/developer-workflow-default-transition.spec.js
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
    (edge) => edge && edge.aging !== true && typeof edge.defaultTransition === "boolean",
  );
}

function edgeText(edge) {
  return `${edge.from} — ${edge.label} → ${edge.to}`;
}

function edgeIdentity(edge) {
  return `${edge.from}|${edge.label}|${edge.to}`.toLowerCase();
}

function defaultUrl(name, edge) {
  const q = new URLSearchParams({
    from: edge.from,
    label: edge.label,
    to: edge.to,
  });
  return `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent(name)}/transitions/default?${q.toString()}`;
}

function graphUrl(name) {
  return `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent(name)}/graph`;
}

function edgeRow(page, edge) {
  return page
    .locator('[data-testid="developer-wf-graph-edges"] li')
    .filter({ hasText: edgeText(edge) });
}

function pickPair(edges) {
  const byFrom = new Map();
  for (const edge of edges) {
    const list = byFrom.get(edge.from) || [];
    list.push(edge);
    byFrom.set(edge.from, list);
  }
  for (const list of byFrom.values()) {
    const current = list.find((edge) => edge.defaultTransition === true);
    const next = list.find((edge) => edge.defaultTransition !== true);
    if (current && next) {
      return { current, next };
    }
  }
  return null;
}

async function ensurePair(request, headers, name) {
  let edges = regularEdges(await (await request.get(graphUrl(name), { headers })).json());
  expect(edges.length, "custom workflow has a regular transition").toBeGreaterThan(0);
  let pair = pickPair(edges);
  if (pair) {
    return { pair, edges };
  }
  const anchor = edges.find((edge) => edge.defaultTransition === true) || edges[0];
  const label = `Alt ${Date.now()}`.slice(0, 50);
  const created = await request.post(
    `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent(name)}/transitions`,
    {
      headers,
      data: { WorkflowTransitionWrite: { from: anchor.from, to: anchor.to, label } },
    },
  );
  expect(created.status(), await created.text()).toBe(200);
  edges = regularEdges(await (await request.get(graphUrl(name), { headers })).json());
  pair = pickPair(edges);
  expect(pair, "step has a default and a non-default transition").toBeTruthy();
  return { pair, edges };
}

test.describe("Developer workflow default transition (#5198)", () => {
  test("Admin marks one transition as the default only after save", async ({
    page,
    request,
  }) => {
    test.setTimeout(180_000);
    const guards = attachConsoleGuards(page);
    const headers = jsonHeaders();
    const name = uniqueWorkflowName("Nightly Def");
    try {
      const created = await request.post(`${BASE_URL}/Rhythmyx/services/workflows`, {
        headers,
        data: { WorkflowCreate: { name, description: "slice 71" } },
      });
      expect(created.status(), await created.text()).toBe(200);

      const { pair, edges } = await ensurePair(request, headers, name);
      const target = pair.next;
      const previous = pair.current;
      const otherSteps = edges.filter(
        (edge) =>
          edge.from !== target.from &&
          edge.defaultTransition === true &&
          edgeIdentity(edge) !== edgeIdentity(previous),
      );

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
      const previousRow = edgeRow(page, previous);
      await expect(row).toBeVisible();
      await expect(previousRow).toBeVisible();
      const badge = row.locator('[data-testid^="developer-wf-graph-default-"]');
      const previousBadge = previousRow.locator('[data-testid^="developer-wf-graph-default-"]');
      await expect(badge).toHaveAttribute("data-default", "false");
      await expect(previousBadge).toHaveAttribute("data-default", "true");
      const comment = row.locator('[data-testid^="developer-wf-graph-comment-"]');
      const commentWasChecked = await comment.isChecked();
      const approvals = row.locator('[data-testid^="developer-wf-graph-approvals-"]');
      const storedApprovals = await approvals.getAttribute("data-approvals");

      let defaultPuts = 0;
      const countPuts = (req) => {
        if (req.method() === "PUT" && req.url().includes("/transitions/default")) {
          defaultPuts += 1;
        }
      };
      page.on("request", countPuts);
      await row.locator('[data-testid^="developer-wf-default-edit-"]').click();
      await page.locator('[data-testid="developer-wf-default-cancel"]').click();
      expect(defaultPuts, "cancel does not write").toBe(0);
      await expect(badge).toHaveAttribute("data-default", "false");
      await expect(previousBadge).toHaveAttribute("data-default", "true");
      await expect(page.locator('[data-testid="developer-wf-graph-notice"]')).toHaveCount(0);

      await row.locator('[data-testid^="developer-wf-default-edit-"]').click();
      await page.locator('[data-testid="developer-wf-default-save"]').click();
      await expect(page.locator('[data-testid="developer-wf-graph-notice"]')).toContainText(
        /Default transition saved/i,
        { timeout: 30_000 },
      );
      expect(defaultPuts).toBe(1);
      await expect(badge).toHaveAttribute("data-default", "true");
      await expect(previousBadge).toHaveAttribute("data-default", "false");
      expect(await comment.isChecked()).toBe(commentWasChecked);
      await expect(approvals).toHaveAttribute("data-approvals", storedApprovals);
      await expect(row).toContainText(edgeText(target));
      page.off("request", countPuts);

      await page.locator('[data-testid="developer-wf-back"]').click();
      await page
        .locator(catalogOpenByExactName("developer-wf-open", "data-wf-name", name))
        .click();
      await expect(
        edgeRow(page, target).locator('[data-testid^="developer-wf-graph-default-"]'),
      ).toHaveAttribute("data-default", "true", { timeout: 30_000 });
      await expect(
        edgeRow(page, previous).locator('[data-testid^="developer-wf-graph-default-"]'),
      ).toHaveAttribute("data-default", "false");

      const afterUi = await request.get(graphUrl(name), { headers });
      expect(afterUi.status()).toBe(200);
      const savedEdges = regularEdges(await afterUi.json());
      const saved = savedEdges.find((edge) => edgeIdentity(edge) === edgeIdentity(target));
      const cleared = savedEdges.find((edge) => edgeIdentity(edge) === edgeIdentity(previous));
      expect(saved.defaultTransition).toBe(true);
      expect(cleared.defaultTransition).toBe(false);
      expect(saved.approvalsRequired).toBe(target.approvalsRequired);
      expect(saved.commentRequired === true).toBe(target.commentRequired === true);
      expect(saved.label).toBe(target.label);
      expect(saved.to).toBe(target.to);
      const onStep = savedEdges.filter((edge) => edge.from === target.from);
      expect(onStep.filter((edge) => edge.defaultTransition === true)).toHaveLength(1);
      for (const other of otherSteps) {
        const still = savedEdges.find((edge) => edgeIdentity(edge) === edgeIdentity(other));
        expect(still.defaultTransition).toBe(true);
      }

      const clear = await request.put(defaultUrl(name, target), {
        headers,
        data: { WorkflowTransitionDefault: { defaultTransition: false } },
      });
      expect(clear.status(), "clearing the default is 409").toBe(409);
      const unchanged = await request.put(defaultUrl(name, target), {
        headers,
        data: { WorkflowTransitionDefault: { defaultTransition: true } },
      });
      expect(unchanged.status(), "already the only default is 400").toBe(400);
      const blankFrom = await request.put(
        `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent(name)}/transitions/default?from=%20&label=${encodeURIComponent(target.label)}`,
        {
          headers,
          data: { WorkflowTransitionDefault: { defaultTransition: true } },
        },
      );
      expect(blankFrom.status(), "blank from is 400").toBe(400);
      const afterFail = regularEdges(
        await (await request.get(graphUrl(name), { headers })).json(),
      );
      const kept = afterFail.find((edge) => edgeIdentity(edge) === edgeIdentity(target));
      const stillCleared = afterFail.find(
        (edge) => edgeIdentity(edge) === edgeIdentity(previous),
      );
      expect(kept.defaultTransition).toBe(true);
      expect(stillCleared.defaultTransition).toBe(false);
      expect(kept.approvalsRequired).toBe(target.approvalsRequired);

      guards.assertClean();
    } finally {
      await request.delete(
        `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent(name)}`,
        { headers },
      );
    }
  });

  test("packaged workflows keep the previous default on HTTP 403", async ({ page, request }) => {
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
      const pair = pickPair(edges);
      const target = pair ? pair.next : edges.find((edge) => edge.defaultTransition === true);
      if (!target) {
        continue;
      }
      checked += 1;
      const rejected = await request.put(defaultUrl(packagedName, target), {
        headers,
        data: { WorkflowTransitionDefault: { defaultTransition: true } },
      });
      expect(rejected.status(), `${packagedName} default is 403`).toBe(403);
      const after = regularEdges(
        await (await request.get(graphUrl(packagedName), { headers })).json(),
      );
      const same = after.find((edge) => edgeIdentity(edge) === edgeIdentity(target));
      expect(same.defaultTransition).toBe(target.defaultTransition);
      expect(same.approvalsRequired).toBe(target.approvalsRequired);
      expect(same.label).toBe(target.label);
      if (pair) {
        const previous = after.find((edge) => edgeIdentity(edge) === edgeIdentity(pair.current));
        expect(previous.defaultTransition).toBe(true);
      }
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
    await expect(page.locator('[data-testid^="developer-wf-default-edit-"]')).toHaveCount(0);
    guards.assertClean();
  });
});
