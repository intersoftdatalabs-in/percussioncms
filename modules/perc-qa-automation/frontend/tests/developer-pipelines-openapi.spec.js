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
 * Developer Pipelines Slice C — OpenAPI from resources (#4366 / parent #1690).
 *
 * Admin opens Developer → Pipelines detail, views OpenAPI 3 generated from IR
 * resources, and downloads YAML. Documents at least one resource execute path.
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=…
 *     npm run test:surface -- --path tests/developer-pipelines-openapi.spec.js
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

async function catalogPipelineNames(page) {
  const preferred = (process.env.PIPELINE_APP_NAME || "").trim();
  if (preferred) {
    return [preferred];
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
  const sys = names.filter((n) => /^sys_/i.test(n));
  const rest = names.filter((n) => !/^sys_/i.test(n));
  return sys.concat(rest);
}

test("Developer Pipelines OpenAPI view and download", async ({ page }) => {
  const { pageErrors, consoleErrors } = attachConsoleGuards(page);
  await loginAsAdmin(page);
  await openPipelinesCatalog(page);
  const names = await catalogPipelineNames(page);
  let body = "";
  let opened = false;
  for (const name of names.slice(0, 8)) {
    const openApiWait = page.waitForResponse(
      (r) =>
        /\/services\/pipelines\/[^/?#]+\/openapi(?:\?|$)/i.test(r.url()) &&
        r.request().method() === "GET",
      { timeout: 45_000 },
    );
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

    const section = page.locator('[data-testid="developer-pipe-openapi"]');
    await expect(section).toBeVisible({ timeout: 20_000 });

    const openApiResp = await openApiWait;
    const status = openApiResp.status();
    body = await openApiResp.text();
    if (status >= 400) {
      throw new Error(
        `GET pipeline OpenAPI failed: HTTP ${status} body=${body.slice(0, 400)}`,
      );
    }
    if (/\/pipelines\/.+\/resources\/.+\/execute/.test(body)) {
      opened = true;
      break;
    }
    await page.locator('[data-testid="developer-pipe-back"]').click();
    await expect(page.locator('[data-testid="developer-pipe-table"]')).toBeVisible({
      timeout: 20_000,
    });
  }
  if (!opened) {
    throw new Error(
      "No catalog pipeline returned OpenAPI with a resource execute path (tried first 8)",
    );
  }
  expect(body, "OpenAPI body should not echo raw traversal ids").not.toMatch(/\.\.\//);
  expect(body).toMatch(/openapi:\s*"?3\./i);

  await expect(page.locator('[data-testid="developer-pipe-openapi-loading"]')).toHaveCount(0, {
    timeout: 30_000,
  });
  const openApiError = page.locator('[data-testid="developer-pipe-openapi-error"]');
  if (await openApiError.isVisible()) {
    throw new Error(`OpenAPI section error: ${(await openApiError.innerText()).trim()}`);
  }
  const doc = page.locator('[data-testid="developer-pipe-openapi-doc"]');
  await expect(doc).toBeVisible({ timeout: 15_000 });
  const text = (await doc.innerText()).trim();
  expect(text).toMatch(/openapi:/i);
  expect(text).toMatch(/\/pipelines\/.+\/resources\/.+\/execute/);

  const downloadButton = page.locator('[data-testid="developer-pipe-openapi-download"]');
  await expect(downloadButton).toBeEnabled();
  const downloadPromise = page.waitForEvent("download", { timeout: 15_000 });
  await downloadButton.click();
  const download = await downloadPromise;
  const suggested = download.suggestedFilename() || "";
  expect(suggested).toMatch(/\.ya?ml$/i);
  expect(suggested).not.toMatch(/[\\/]/);

  await expect(page.locator('[data-testid="developer-pipe-openapi-view"]')).toBeVisible();
  assertConsoleClean(pageErrors, consoleErrors);
});
