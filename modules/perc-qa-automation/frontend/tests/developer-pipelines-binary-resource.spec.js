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
 * Developer Pipelines Slice D — binary resource persist + retrieve (#4395 / parent #1690).
 *
 * Admin opens Developer → Pipelines detail, rejects a cloud binary path, saves
 * the bundled local fixture token, then Retrieve bytes shows PIPE-BIN-FIXTURE
 * (not invented content).
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… \
 *     npm run test:surface -- --path tests/developer-pipelines-binary-resource.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { catalogOpenByExactName } = require("./helpers/developer-catalog-selectors");

const BUNDLED_PATH = "pipeline-binary-fixture";
const CLOUD_PATH = "https://cdn.example/blob.bin";
const BINARY_RESOURCE = "binaryFixture";

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
  await expect(page.locator('[data-testid="developer-pipe-binary"]')).toBeVisible({
    timeout: 15_000,
  });
}

test.describe("Developer Pipelines binary resource (#4395 / Slice D)", () => {
  test(
    "Admin saves local fixture, rejects cloud path, retrieve shows fixture bytes",
    { tag: ["@developer", "@pipelines", "@binary-resource"] },
    async ({ page }) => {
      test.setTimeout(180_000);
      const { pageErrors, consoleErrors } = attachConsoleGuards(page);

      await loginAsAdmin(page);
      await openPipelinesCatalog(page);
      const appName = await resolvePipelineAppName(page);
      await openPipelineDetail(page, appName);

      const resourceInput = page.locator('[data-testid="developer-pipe-invoke-resource"]');
      await expect(resourceInput).toBeVisible({ timeout: 10_000 });
      await resourceInput.fill(BINARY_RESOURCE);

      const pathInput = page.locator('[data-testid="developer-pipe-binary-path"]');
      await expect(pathInput).toBeVisible();
      await pathInput.fill(CLOUD_PATH);

      const cloudWait = page.waitForResponse(
        (r) =>
          /\/services\/pipelines\/[^/?#]+\/resources\/[^/?#]+\/binaryResource(?:\?|$)/i.test(
            r.url(),
          ) && r.request().method() === "PUT",
        { timeout: 45_000 },
      );
      await page.locator('[data-testid="developer-pipe-binary-save"]').click();
      const cloudResp = await cloudWait;
      expect(cloudResp.status(), "cloud binary path must be 400").toBe(400);
      await expect(page.locator('[data-testid="developer-pipe-binary-error"]')).toBeVisible({
        timeout: 15_000,
      });
      await expect(page.locator('[data-testid="developer-pipe-binary-error"]')).toContainText(
        /cloud|local fixture|rejected|400/i,
      );

      await pathInput.fill(BUNDLED_PATH);
      await page.locator('[data-testid="developer-pipe-binary-type"]').fill("text/plain");
      const saveWait = page.waitForResponse(
        (r) =>
          /\/services\/pipelines\/[^/?#]+\/resources\/[^/?#]+\/binaryResource(?:\?|$)/i.test(
            r.url(),
          ) && r.request().method() === "PUT",
        { timeout: 45_000 },
      );
      await page.locator('[data-testid="developer-pipe-binary-save"]').click();
      const saveResp = await saveWait;
      expect(saveResp.status(), `binary save status ${saveResp.status()}`).toBe(200);
      await expect(page.locator('[data-testid="developer-pipe-binary-notice"]')).toBeVisible({
        timeout: 15_000,
      });

      const retrieveWait = page.waitForResponse(
        (r) =>
          /\/services\/pipelines\/[^/?#]+\/resources\/[^/?#]+\/binary(?:\?|$)/i.test(r.url()) &&
          r.request().method() === "GET",
        { timeout: 45_000 },
      );
      await page.locator('[data-testid="developer-pipe-binary-retrieve"]').click();
      const retrieveResp = await retrieveWait;
      expect(retrieveResp.status(), `binary retrieve status ${retrieveResp.status()}`).toBe(200);
      const preview = page.locator('[data-testid="developer-pipe-binary-preview"]');
      await expect(preview).toBeVisible({ timeout: 20_000 });
      const text = (await preview.innerText()).trim();
      expect(text).toMatch(/PIPE-BIN-FIXTURE/);
      expect(text).not.toMatch(/invented/i);

      assertConsoleClean(pageErrors, consoleErrors);
    },
  );
});
