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
 * Developer Pipelines Slice C — OpenAPI from resources (#4366 / #4384 / parent #1690).
 *
 * Admin opens Developer → Pipelines detail, views OpenAPI 3 generated from IR
 * resources, and downloads YAML. Documents at least one resource execute path.
 *
 * Does not require OpenAPI chrome on a non-IR first catalog row (sys_ActionPage
 * often has data sets only). Prefers sys_cmp* IR/execute apps, or
 * PIPELINE_APP_NAME. Missing chrome on every candidate is treated as a stale
 * cm/modern deploy (qa-deploy-webui Slice C SPA).
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up --then-qa-deploy-webui --then-qa-deploy-war-jars
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=…
 *     npm run test:surface -- --path tests/developer-pipelines-openapi.spec.js
 *   perc-devctl qa-down
 * </pre>
 *
 * Optional: PIPELINE_APP_NAME=<exact catalog name> (default: ranked IR apps).
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { catalogOpenByExactName } = require("./helpers/developer-catalog-selectors");
const {
  TEST_IDS,
  openApiCandidateNames,
  isPipelineOpenApiGetUrl,
  staleOpenApiChromeMessage,
} = require("./helpers/developer-pipelines-openapi-surface");

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
  const panel = page.locator(`[data-testid="${TEST_IDS.panel}"]`);
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
  await expect(page.locator(`[data-testid="${TEST_IDS.table}"]`)).toBeVisible();
}

async function catalogPipelineNames(page) {
  const opens = page.locator(`[data-testid="${TEST_IDS.open}"][data-pipe-name]`);
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
  return names;
}

test("Developer Pipelines OpenAPI view and download", async ({ page }) => {
  test.setTimeout(180_000);
  const { pageErrors, consoleErrors } = attachConsoleGuards(page);
  await loginAsAdmin(page);
  await openPipelinesCatalog(page);
  const preferred = (process.env.PIPELINE_APP_NAME || "").trim();
  const names = openApiCandidateNames(await catalogPipelineNames(page), preferred);
  const name = names[0];
  if (!name) {
    throw new Error("No pipeline catalog names after ranking");
  }

  const openApiWait = page
    .waitForResponse(
      (r) =>
        r.request().method() === "GET" && isPipelineOpenApiGetUrl(r.url(), name),
      { timeout: 45_000 },
    )
    .catch(() => null);
  const open = page.locator(
    catalogOpenByExactName(TEST_IDS.open, "data-pipe-name", name),
  );
  await expect(open, `expected catalog open for ${name}`).toBeVisible({
    timeout: 20_000,
  });
  await open.click();
  const title = page.locator('[data-testid="developer-pipe-detail-title"]');
  const catalogErr = page.locator('[data-testid="developer-pipe-error"]');
  const sectionErr = page.locator('[data-testid="developer-section-error"]');
  await expect(title.or(catalogErr).or(sectionErr)).toBeVisible({ timeout: 30_000 });
  if (await catalogErr.isVisible()) {
    throw new Error(
      `Pipelines catalog error after opening ${name}: ${(await catalogErr.innerText()).trim()}`,
    );
  }
  if (await sectionErr.isVisible()) {
    throw new Error(
      `Pipelines section error after opening ${name}: ${(await sectionErr.innerText()).trim()}`,
    );
  }
  await expect(title).toContainText(name, { timeout: 10_000 });
  await expect(page.locator('[data-testid="developer-pipe-detail-loading"]')).toHaveCount(
    0,
    { timeout: 30_000 },
  );
  const detailError = page.locator('[data-testid="developer-pipe-detail-error"]');
  if (await detailError.isVisible()) {
    throw new Error(`Pipeline detail error: ${(await detailError.innerText()).trim()}`);
  }
  const section = page.locator(`[data-testid="${TEST_IDS.openApi}"]`);
  const chromeVisible = await section
    .waitFor({ state: "visible", timeout: 20_000 })
    .then(() => true)
    .catch(() => false);
  if (!chromeVisible) {
    const ui = (
      await page.locator('[data-testid="panel-developer-pipelines"]').innerText()
    ).slice(0, 800);
    throw new Error(`${staleOpenApiChromeMessage([name])} UI=${ui}`);
  }
  const openApiResp = await openApiWait;
  if (openApiResp && openApiResp.ok()) {
    const yaml = await openApiResp.text();
    expect(yaml, "OpenAPI body should not echo raw traversal ids").not.toMatch(/\.\.\//);
    expect(yaml).toMatch(/openapi:\s*"?3\./i);
  }

  await expect(page.locator(`[data-testid="${TEST_IDS.openApiLoading}"]`)).toHaveCount(0, {
    timeout: 30_000,
  });
  const openApiError = page.locator(`[data-testid="${TEST_IDS.openApiError}"]`);
  if (await openApiError.isVisible()) {
    throw new Error(`OpenAPI section error: ${(await openApiError.innerText()).trim()}`);
  }
  const doc = page.locator(`[data-testid="${TEST_IDS.openApiDoc}"]`);
  await expect(doc).toBeVisible({ timeout: 15_000 });
  const text = (await doc.innerText()).trim();
  expect(text).toMatch(/openapi:/i);
  expect(text).toMatch(/\/pipelines\/.+\/resources\/.+\/execute/);

  const downloadButton = page.locator(`[data-testid="${TEST_IDS.openApiDownload}"]`);
  await expect(downloadButton).toBeEnabled();
  const downloadPromise = page.waitForEvent("download", { timeout: 15_000 });
  await downloadButton.click();
  const download = await downloadPromise;
  const suggested = download.suggestedFilename() || "";
  expect(suggested).toMatch(/\.ya?ml$/i);
  expect(suggested).not.toMatch(/[\\/]/);

  await expect(page.locator(`[data-testid="${TEST_IDS.openApiView}"]`)).toBeVisible();
  assertConsoleClean(pageErrors, consoleErrors);
});
