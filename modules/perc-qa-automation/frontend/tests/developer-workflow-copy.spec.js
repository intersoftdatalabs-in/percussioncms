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
 * Developer workflow copy (slice 36 / #4842 / parent #1690).
 *
 * Admin copies a catalog workflow (POST /services/workflows/{name}/copy).
 * The copy opens, keeps source steps and at least one transition, and a
 * duplicate name shows 409 without removing the source.
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… \
 *     npm run test:surface -- --path tests/developer-workflow-copy.spec.js
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

function graphEdgeCount(body) {
  const graph = body.WorkflowGraph || body.workflowGraph || body;
  const edges = graph.edges || [];
  return Array.isArray(edges) ? edges.length : 0;
}

function graphNodeNames(body) {
  const graph = body.WorkflowGraph || body.workflowGraph || body;
  const nodes = graph.nodes || [];
  if (!Array.isArray(nodes)) {
    return [];
  }
  return nodes.map((n) => (n && (n.name || n.label)) || "").filter(Boolean);
}

test.describe("Developer workflow copy (slice 36 / #4842)", () => {
  test("copy opens the new workflow and keeps steps and transitions", async ({
    page,
    request,
  }) => {
    test.setTimeout(180_000);
    const guards = attachConsoleGuards(page);
    const source = uniqueWorkflowName("Nightly Src");
    const copyName = uniqueWorkflowName("Nightly Cpy");
    const headers = jsonHeaders();
    const seeded = await request.post(`${BASE_URL}/Rhythmyx/services/workflows`, {
      headers,
      data: { WorkflowCreate: { name: source, description: "source desc" } },
    });
    expect(seeded.status(), "seed source workflow").toBe(200);

    const sourceGraph = await request.get(
      `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent(source)}/graph`,
      { headers },
    );
    expect(sourceGraph.status()).toBe(200);
    const sourceBody = await sourceGraph.json();
    expect(graphEdgeCount(sourceBody)).toBeGreaterThan(0);
    const sourceNodes = graphNodeNames(sourceBody);
    expect(sourceNodes.length).toBeGreaterThan(0);

    await loginAsAdmin(page);
    await openWorkflowsCatalog(page);
    const copyBtn = page.locator(
      catalogOpenByExactName("developer-wf-copy", "data-wf-name", source),
    );
    await expect(copyBtn).toBeVisible({ timeout: 30_000 });
    await copyBtn.click();
    await expect(page.locator('[data-testid="developer-wf-copy-panel"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-wf-copy-source"]')).toContainText(
      source,
    );
    await page.locator('[data-testid="developer-wf-copy-name"]').fill(copyName);
    await page.locator('[data-testid="developer-wf-copy-save"]').click();

    const detail = page.locator('[data-testid="developer-wf-detail"]');
    await expect(detail).toBeVisible({ timeout: 30_000 });
    await expect(page.locator('[data-testid="developer-wf-detail-title"]')).toContainText(
      copyName,
    );
    await expect(page.locator('[data-testid="developer-wf-steps"]')).toContainText(
      sourceNodes[0],
    );

    const copiedGraph = await request.get(
      `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent(copyName)}/graph`,
      { headers },
    );
    expect(copiedGraph.status()).toBe(200);
    const copiedBody = await copiedGraph.json();
    expect(graphNodeNames(copiedBody)).toEqual(sourceNodes);
    expect(graphEdgeCount(copiedBody)).toBe(graphEdgeCount(sourceBody));

    const sourceStill = await request.get(
      `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent(source)}/graph`,
      { headers },
    );
    expect(sourceStill.status()).toBe(200);
    guards.assertClean();
  });

  test("duplicate copy name shows 409 and leaves the source", async ({
    page,
    request,
  }) => {
    test.setTimeout(180_000);
    const guards = attachConsoleGuards(page);
    const source = uniqueWorkflowName("Nightly Src");
    const headers = jsonHeaders();
    const seeded = await request.post(`${BASE_URL}/Rhythmyx/services/workflows`, {
      headers,
      data: { WorkflowCreate: { name: source } },
    });
    expect(seeded.status()).toBe(200);

    await loginAsAdmin(page);
    await openWorkflowsCatalog(page);
    const copyBtn = page.locator(
      catalogOpenByExactName("developer-wf-copy", "data-wf-name", source),
    );
    await copyBtn.click();
    await page.locator('[data-testid="developer-wf-copy-name"]').fill(source);
    const duplicate = page.waitForResponse(
      (res) =>
        res.request().method() === "POST" &&
        /\/services\/workflows\/[^/]+\/copy(?:\?|$)/.test(res.url()),
    );
    await page.locator('[data-testid="developer-wf-copy-save"]').click();
    expect((await duplicate).status()).toBe(409);
    await expect(page.locator('[data-testid="developer-wf-copy-error"]')).toContainText(
      /already exists/i,
    );
    await expect(page.locator('[data-testid="developer-wf-copy-panel"]')).toBeVisible();

    const sourceStill = await request.get(
      `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent(source)}/graph`,
      { headers },
    );
    expect(sourceStill.status()).toBe(200);
    guards.assertClean();
  });
});
