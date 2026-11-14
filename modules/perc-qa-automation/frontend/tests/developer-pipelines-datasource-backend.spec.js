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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Developer Pipelines datasource backend write (#6066 / parent #1690).
 *
 * Admin opens Developer → Pipelines detail, rejects a non-allow-listed
 * datasource id, connection-tests a live allow-listed id, saves the JDBC
 * datasource backend, then removes it (second remove is 404 — the HTTP
 * backend tank is never clobbered).
 *
 * Requires `databaseExplorer.allowListedDatasources` on the QA cell (e.g.
 * `cms=repository`). Empty catalog is a setup failure, not a skip.
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… TEST_DB_TYPE=h2 \
 *     TEST_PRODUCT=cms \
 *     npm run test:surface -- --path tests/developer-pipelines-datasource-backend.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL, adminBasicAuthHeaders } = require("./helpers/auth");
const { catalogOpenByExactName } = require("./helpers/developer-catalog-selectors");
const {
  developerDatabaseExplorerRestUrl,
  unwrapDatabaseExplorerDatasources,
} = require("./helpers/developer-database-explorer-surface");

const SHADOW_ID = "shadowDs";

function developerPipelinesUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "pipelines",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
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
  return { pageErrors, consoleErrors };
}

function assertConsoleClean(pageErrors, consoleErrors) {
  expect(pageErrors, `pageerror: ${pageErrors.join(" | ")}`).toEqual([]);
  const unexpectedConsole = consoleErrors.filter(
    (t) => !/Failed to load resource/i.test(t) && !/favicon/i.test(t),
  );
  expect(
    unexpectedConsole,
    `console error: ${unexpectedConsole.join(" | ")}`,
  ).toEqual([]);
}

async function resolveDatasourceId(request) {
  const headers = {
    ...adminBasicAuthHeaders(),
    Accept: "application/json",
  };
  const res = await request.get(developerDatabaseExplorerRestUrl(BASE_URL), {
    headers,
  });
  expect(res.status(), `Database Explorer catalog status ${res.status()}`).toBeLessThan(300);
  const datasources = unwrapDatabaseExplorerDatasources(await res.json());
  expect(
    datasources.length,
    "databaseExplorer.allowListedDatasources must list at least one datasource on this H2 cell",
  ).toBeGreaterThan(0);
  return String(datasources[0].id);
}

async function openPipelinesCatalog(page) {
  await page.goto(developerPipelinesUrl(), { waitUntil: "networkidle" });
  await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
    timeout: 20_000,
  });
  const panel = page.locator('[data-testid="developer-pipe-panel"]');
  const empty = page.locator('[data-testid="developer-pipe-empty"]');
  const listError = page.locator('[data-testid="developer-pipe-error"]');
  await expect(panel.or(empty).or(listError).first()).toBeVisible({
    timeout: 30_000,
  });
  if (await listError.isVisible()) {
    throw new Error(
      `Developer pipelines catalog error: ${(await listError.innerText()).trim()}`,
    );
  }
  if (await empty.isVisible()) {
    throw new Error("Developer pipelines catalog empty on H2 (expected applications)");
  }
  await expect(page.locator('[data-testid="developer-pipe-table"]')).toBeVisible();
}

async function resolvePipelineAppName(page) {
  const preferred = (process.env.PIPELINE_APP_NAME || "").trim();
  if (preferred) {
    return preferred;
  }
  const opens = page.locator('[data-testid="developer-pipe-open"][data-pipe-name]');
  await expect(opens.first()).toBeVisible({ timeout: 20_000 });
  const count = await opens.count();
  const names = [];
  for (let i = 0; i < count; i++) {
    const name = ((await opens.nth(i).getAttribute("data-pipe-name")) || "").trim();
    if (name) names.push(name);
  }
  if (names.length === 0) {
    throw new Error("No pipeline open controls with data-pipe-name");
  }
  const sysPreferred = names.find((n) => /^sys_/i.test(n));
  return sysPreferred || names[0];
}

async function openPipelineDetail(page, name) {
  const open = page.locator(
    catalogOpenByExactName("developer-pipe-open", "data-pipe-name", name),
  );
  await expect(open, `expected catalog open for ${name}`).toBeVisible({
    timeout: 20_000,
  });
  await open.click();
  await expect(page.locator('[data-testid="developer-pipe-detail"]')).toBeVisible({
    timeout: 20_000,
  });
  await expect(page.locator('[data-testid="developer-pipe-detail-loading"]')).toHaveCount(0, {
    timeout: 30_000,
  });
  const detailError = page.locator('[data-testid="developer-pipe-detail-error"]');
  if (await detailError.isVisible()) {
    throw new Error(`Pipeline detail error: ${(await detailError.innerText()).trim()}`);
  }
  await expect(page.locator('[data-testid="developer-pipe-datasource"]')).toBeVisible({
    timeout: 15_000,
  });
}

test.describe("Developer Pipelines datasource backend (#6066 / #1690)", () => {
  test(
    "Admin rejects unknown id, tests live id, saves, then removes (404 on second remove)",
    { tag: ["@developer", "@pipelines", "@datasource-backend"] },
    async ({ page, request }) => {
      test.setTimeout(240_000);
      const { pageErrors, consoleErrors } = attachConsoleGuards(page);
      const liveId = await resolveDatasourceId(request);

      await loginAsAdmin(page);
      await openPipelinesCatalog(page);
      const appName = await resolvePipelineAppName(page);
      await openPipelineDetail(page, appName);

      const resource =
        (process.env.PIPELINE_RESOURCE_NAME || "").trim() ||
        `dsbe${String(Date.now() % 100000).padStart(5, "0")}`;
      const invokeInput = page.locator('[data-testid="developer-pipe-invoke-resource"]');
      if (await invokeInput.count()) {
        await invokeInput.fill(resource);
      }

      const idInput = page.locator('[data-testid="developer-pipe-datasource-id"]');
      await expect(idInput).toBeVisible();
      await idInput.fill(SHADOW_ID);
      const shadowWait = page.waitForResponse(
        (r) =>
          /\/services\/pipelines\/[^/?#]+\/resources\/[^/?#]+\/datasourceBackend(?:\?|$)/i.test(
            r.url(),
          ) && r.request().method() === "PUT",
        { timeout: 45_000 },
      );
      await page.locator('[data-testid="developer-pipe-datasource-save"]').click();
      const shadowResp = await shadowWait;
      expect(shadowResp.status(), "unknown datasource id must be 400").toBe(400);
      await expect(page.locator('[data-testid="developer-pipe-datasource-error"]')).toBeVisible({
        timeout: 15_000,
      });
      await expect(page.locator('[data-testid="developer-pipe-datasource-error"]')).toContainText(
        /allow-listed/i,
      );

      await idInput.fill(liveId);
      const testWait = page.waitForResponse(
        (r) =>
          /\/services\/pipelines\/[^/?#]+\/resources\/[^/?#]+\/datasourceBackend\/test(?:\?|$)/i.test(
            r.url(),
          ) && r.request().method() === "POST",
        { timeout: 45_000 },
      );
      await page.locator('[data-testid="developer-pipe-datasource-test"]').click();
      const testResp = await testWait;
      expect(testResp.status(), `connection test status ${testResp.status()}`).toBe(200);
      const testBody = await testResp.json();
      const testOutcome =
        testBody.PipelineDatasourceBackend ?? testBody.pipelineDatasourceBackend ?? testBody;
      expect(String(testOutcome.datasourceId || ""), "test names the datasource").toContain(
        liveId,
      );
      expect(testOutcome.connectionOk, "live H2 datasource test must pass").toBe(true);
      await expect(page.locator('[data-testid="developer-pipe-datasource-notice"]')).toBeVisible({
        timeout: 15_000,
      });

      const saveWait = page.waitForResponse(
        (r) =>
          /\/services\/pipelines\/[^/?#]+\/resources\/[^/?#]+\/datasourceBackend(?:\?|$)/i.test(
            r.url(),
          ) && r.request().method() === "PUT",
        { timeout: 45_000 },
      );
      await page.locator('[data-testid="developer-pipe-datasource-save"]').click();
      const saveResp = await saveWait;
      expect(saveResp.status(), `datasource save status ${saveResp.status()}`).toBe(200);
      await expect(page.locator('[data-testid="developer-pipe-datasource-notice"]')).toBeVisible({
        timeout: 15_000,
      });

      const deleteWait = page.waitForResponse(
        (r) =>
          /\/services\/pipelines\/[^/?#]+\/resources\/[^/?#]+\/datasourceBackend(?:\?|$)/i.test(
            r.url(),
          ) && r.request().method() === "DELETE",
        { timeout: 45_000 },
      );
      await page.locator('[data-testid="developer-pipe-datasource-delete"]').click();
      const deleteResp = await deleteWait;
      expect(deleteResp.status(), `datasource delete status ${deleteResp.status()}`).toBe(200);
      await expect(page.locator('[data-testid="developer-pipe-datasource-notice"]')).toBeVisible({
        timeout: 15_000,
      });

      const deleteAgainWait = page.waitForResponse(
        (r) =>
          /\/services\/pipelines\/[^/?#]+\/resources\/[^/?#]+\/datasourceBackend(?:\?|$)/i.test(
            r.url(),
          ) && r.request().method() === "DELETE",
        { timeout: 45_000 },
      );
      await page.locator('[data-testid="developer-pipe-datasource-delete"]').click();
      const deleteAgainResp = await deleteAgainWait;
      expect(deleteAgainResp.status(), "second remove must be 404").toBe(404);
      await expect(page.locator('[data-testid="developer-pipe-datasource-error"]')).toBeVisible({
        timeout: 15_000,
      });

      assertConsoleClean(pageErrors, consoleErrors);
    },
  );
});
