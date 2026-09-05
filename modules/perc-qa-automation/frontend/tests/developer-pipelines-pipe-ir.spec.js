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
 * Developer Pipelines Slice B wave 2 — read-only pipe IR / tanks (#4316 / parent #1690).
 *
 * Admin opens Developer → Pipelines detail and asserts the Pipe IR section
 * (meta + resources, optional tanks/mapper tables) loads from Admin REST.
 * Read-only; does not exercise start/stop or IR write.
 *
 * Consumes REST tip #4314 / PR #4318 and SPA tip #4315 / PR #4319.
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… \
 *     npm run test:surface -- --path tests/developer-pipelines-pipe-ir.spec.js
 *   perc-devctl qa-down
 * </pre>
 *
 * Optional: PIPELINE_APP_NAME=<exact catalog name> (default: first openable row).
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { catalogOpenByExactName } = require("./helpers/developer-catalog-selectors");

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

/**
 * Prefer PIPELINE_APP_NAME; else first catalog open control
 * (prefer sys_* content-editor apps when present — usually have classic IR).
 * @returns {Promise<string>}
 */
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

/**
 * Open catalog detail and wait for both application detail + IR GET.
 * @returns {Promise<"resources"|"empty">}
 */
async function openPipelineDetailAndWaitForIr(page, name) {
  const open = page.locator(
    catalogOpenByExactName("developer-pipe-open", "data-pipe-name", name),
  );
  await expect(open, `expected catalog open for ${name}`).toBeVisible({
    timeout: 20_000,
  });

  const irGetWait = page.waitForResponse(
    (r) =>
      /\/services\/pipelines\/[^/?#]+\/ir(?:\?|$)/i.test(r.url()) &&
      r.request().method() === "GET",
    { timeout: 45_000 },
  );

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
  await expect(page.locator('[data-testid="developer-pipe-detail-title"]')).toBeVisible();

  const section = page.locator('[data-testid="developer-pipe-ir"]');
  await expect(section).toBeVisible({ timeout: 20_000 });

  const irResp = await irGetWait;
  const status = irResp.status();
  const body = await irResp.text();
  if (status >= 400) {
    throw new Error(`GET pipeline IR failed: HTTP ${status} body=${body.slice(0, 400)}`);
  }

  await expect(page.locator('[data-testid="developer-pipe-ir-loading"]')).toHaveCount(0, {
    timeout: 30_000,
  });

  const irError = page.locator('[data-testid="developer-pipe-ir-error"]');
  if (await irError.isVisible()) {
    throw new Error(`Pipe IR section error: ${(await irError.innerText()).trim()}`);
  }

  const resources = page.locator('[data-testid="developer-pipe-ir-resources"]');
  const empty = page.locator('[data-testid="developer-pipe-ir-empty"]');
  await expect(resources.or(empty).first()).toBeVisible({ timeout: 15_000 });
  await expect(page.locator('[data-testid="developer-pipe-ir-meta"]')).toBeVisible();

  if (await resources.isVisible()) {
    return "resources";
  }
  return "empty";
}

test.describe("Developer Pipelines pipe IR read-only (#4316 / Slice B wave 2)", () => {
  test(
    "Admin sees read-only pipe IR / resource tanks summary on pipeline detail",
    { tag: ["@developer", "@pipelines", "@pipe-ir"] },
    async ({ page }) => {
      test.setTimeout(180_000);
      const { pageErrors, consoleErrors } = attachConsoleGuards(page);

      await loginAsAdmin(page);
      await openPipelinesCatalog(page);
      const appName = await resolvePipelineAppName(page);
      const irState = await openPipelineDetailAndWaitForIr(page, appName);

      const meta = page.locator('[data-testid="developer-pipe-ir-meta"]');
      await expect(meta).toContainText(/NATIVE|CLASSIC_IMPORT|—/i);

      if (irState === "resources") {
        const firstResource = page.locator('[data-testid="developer-pipe-ir-resource-0"]');
        await expect(firstResource).toBeVisible();
        // Tanks / mapper tables are optional per resource; assert when present.
        const tanks = page.locator('[data-testid="developer-pipe-ir-tanks-0"]');
        const mapper = page.locator('[data-testid="developer-pipe-ir-mapper-0"]');
        if (await tanks.count()) {
          await expect(tanks).toBeVisible();
        }
        if (await mapper.count()) {
          await expect(mapper).toBeVisible();
        }
      } else {
        // Empty IR is still a successful read-only summary for apps without resources.
        await expect(page.locator('[data-testid="developer-pipe-ir-empty"]')).toBeVisible();
      }

      // Read-only: no IR write / graph editor chrome on this surface.
      await expect(page.locator('[data-testid="developer-pipe-ir-save"]')).toHaveCount(0);
      await expect(page.locator('[data-testid="developer-pipe-ir-editor"]')).toHaveCount(0);

      assertConsoleClean(pageErrors, consoleErrors);
    },
  );
});
