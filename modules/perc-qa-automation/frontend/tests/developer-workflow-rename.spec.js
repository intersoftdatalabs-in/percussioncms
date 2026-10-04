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
 * Developer workflow rename (slice 60 / #5139 / parent #1690).
 *
 * Admin renames one custom workflow (POST /services/workflows/{name}/rename).
 * Catalog and detail show the new name only after success. Cancel does not
 * call the server. Packaged workflows do not offer rename. Duplicate and
 * invalid names do not claim success. Description-only PUT still rejects a
 * mismatched name.
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… \
 *     npm run test:surface -- --path tests/developer-workflow-rename.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const {
  loginAsAdmin,
  BASE_URL,
  adminBasicAuthHeaders,
} = require("./helpers/auth");
const {
  catalogOpenByExactName,
  catalogRowsSelector,
} = require("./helpers/developer-catalog-selectors");

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

function graphNodeNames(body) {
  const graph = body.WorkflowGraph || body.workflowGraph || body;
  const nodes = graph.nodes || [];
  if (!Array.isArray(nodes)) {
    return [];
  }
  return nodes.map((n) => (n && (n.name || n.label)) || "").filter(Boolean);
}

test.describe("Developer workflow rename (slice 60 / #5139)", () => {
  test("confirm renames one custom workflow; cancel does not call the server", async ({
    page,
    request,
  }) => {
    test.setTimeout(180_000);
    const guards = attachConsoleGuards(page);
    const source = uniqueWorkflowName("Nightly Ren");
    const nextName = uniqueWorkflowName("Nightly Ren2");
    const headers = jsonHeaders();
    const seeded = await request.post(`${BASE_URL}/Rhythmyx/services/workflows`, {
      headers,
      data: { WorkflowCreate: { name: source, description: "keep desc" } },
    });
    expect(seeded.status(), "seed custom workflow").toBe(200);

    const beforeGraph = await request.get(
      `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent(source)}/graph`,
      { headers },
    );
    expect(beforeGraph.status()).toBe(200);
    const beforeNodes = graphNodeNames(await beforeGraph.json());
    expect(beforeNodes.length).toBeGreaterThan(0);

    await loginAsAdmin(page);
    await openWorkflowsCatalog(page);
    const openBtn = page.locator(
      catalogOpenByExactName("developer-wf-open", "data-wf-name", source),
    );
    await expect(openBtn).toBeVisible({ timeout: 30_000 });
    await openBtn.click();
    await expect(page.locator('[data-testid="developer-wf-detail-title"]')).toContainText(
      source,
      { timeout: 30_000 },
    );
    await expect(page.locator('[data-testid="developer-wf-rename"]')).toBeVisible();

    const renameName = page.locator('[data-testid="developer-wf-rename-name"]');
    await renameName.fill(`${source} x`);
    let renameCalls = 0;
    page.on("request", (req) => {
      if (req.method() === "POST" && /\/services\/workflows\/[^/]+\/rename(?:\?|$)/.test(req.url())) {
        renameCalls += 1;
      }
    });
    await page.locator('[data-testid="developer-wf-rename-cancel"]').click();
    await expect(renameName).toHaveValue(source);
    expect(renameCalls).toBe(0);
    await expect(page.locator('[data-testid="developer-wf-detail-title"]')).toContainText(
      source,
    );
    await expect(page.locator('[data-testid="developer-wf-rename-notice"]')).toHaveCount(0);

    await renameName.fill(nextName);
    const renamed = page.waitForResponse(
      (res) =>
        res.request().method() === "POST" &&
        /\/services\/workflows\/[^/]+\/rename(?:\?|$)/.test(res.url()),
    );
    await page.locator('[data-testid="developer-wf-rename-save"]').click();
    expect((await renamed).status()).toBe(200);
    await expect(page.locator('[data-testid="developer-wf-detail-title"]')).toContainText(
      nextName,
      { timeout: 30_000 },
    );
    await expect(page.locator('[data-testid="developer-wf-rename-error"]')).toHaveCount(0);

    await page.locator('[data-testid="developer-wf-back"]').click();
    await expect(page.locator('[data-testid="developer-wf-table"]')).toBeVisible({
      timeout: 30_000,
    });
    const renamedRow = page.locator(catalogRowsSelector("developer-wf-row")).filter({
      has: page.locator(
        catalogOpenByExactName("developer-wf-open", "data-wf-name", nextName),
      ),
    });
    await expect(renamedRow).toBeVisible({ timeout: 30_000 });
    await expect(renamedRow).toContainText("keep desc");
    await expect(
      page.locator(
        catalogOpenByExactName("developer-wf-open", "data-wf-name", source),
      ),
    ).toHaveCount(0);

    const afterGraph = await request.get(
      `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent(nextName)}/graph`,
      { headers },
    );
    expect(afterGraph.status()).toBe(200);
    expect(graphNodeNames(await afterGraph.json())).toEqual(beforeNodes);
    const oldGraph = await request.get(
      `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent(source)}/graph`,
      { headers },
    );
    expect(oldGraph.status()).toBe(404);

    const meta = await request.get(
      `${BASE_URL}/Rhythmyx/services/workflowmanagement/workflows/metadata`,
      { headers },
    );
    expect(meta.status()).toBe(200);
    expect(JSON.stringify(await meta.json())).toContain("keep desc");

    await request.delete(
      `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent(nextName)}`,
      { headers },
    );
    guards.assertClean();
  });

  test("packaged workflows do not call rename; duplicate and bad PUT do not succeed", async ({
    page,
    request,
  }) => {
    test.setTimeout(180_000);
    const guards = attachConsoleGuards(page);
    const source = uniqueWorkflowName("Nightly Dup");
    const other = uniqueWorkflowName("Nightly Occ");
    const headers = jsonHeaders();
    expect(
      (
        await request.post(`${BASE_URL}/Rhythmyx/services/workflows`, {
          headers,
          data: { WorkflowCreate: { name: source, description: "src" } },
        })
      ).status(),
    ).toBe(200);
    expect(
      (
        await request.post(`${BASE_URL}/Rhythmyx/services/workflows`, {
          headers,
          data: { WorkflowCreate: { name: other, description: "other" } },
        })
      ).status(),
    ).toBe(200);

    const mismatched = await request.put(
      `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent(source)}`,
      {
        headers,
        data: { WorkflowUpdate: { name: other, description: "nope" } },
      },
    );
    expect(mismatched.status()).toBe(400);

    const invalid = await request.post(
      `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent(source)}/rename`,
      { headers, data: { WorkflowRename: { name: "Bad!" } } },
    );
    expect(invalid.status()).toBe(400);

    await loginAsAdmin(page);
    await openWorkflowsCatalog(page);
    // Simple Workflow is excluded from the stepped catalog (PSSteppedWorkflowMetadata).
    // Default Workflow is packaged and the H2 system default, so rename is not offered.
    await page
      .locator(catalogOpenByExactName("developer-wf-open", "data-wf-name", "Default Workflow"))
      .click();
    await expect(page.locator('[data-testid="developer-wf-detail-title"]')).toContainText(
      "Default Workflow",
      { timeout: 30_000 },
    );
    await expect(page.locator('[data-testid="developer-wf-rename-save"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-wf-rename-unavailable"]')).toBeVisible();

    await page.locator('[data-testid="developer-wf-back"]').click();
    await page
      .locator(catalogOpenByExactName("developer-wf-open", "data-wf-name", source))
      .click();
    await expect(page.locator('[data-testid="developer-wf-rename-name"]')).toBeVisible({
      timeout: 30_000,
    });
    await page.locator('[data-testid="developer-wf-rename-name"]').fill(other);
    const duplicate = page.waitForResponse(
      (res) =>
        res.request().method() === "POST" &&
        /\/services\/workflows\/[^/]+\/rename(?:\?|$)/.test(res.url()),
    );
    await page.locator('[data-testid="developer-wf-rename-save"]').click();
    expect((await duplicate).status()).toBe(409);
    await expect(page.locator('[data-testid="developer-wf-rename-error"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-wf-rename-notice"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-wf-detail-title"]')).toContainText(
      source,
    );

    await request.delete(
      `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent(source)}`,
      { headers },
    );
    await request.delete(
      `${BASE_URL}/Rhythmyx/services/workflows/${encodeURIComponent(other)}`,
      { headers },
    );
    guards.assertClean();
  });
});
