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
 * Developer Pipelines Slice D — XSL result-page HTML apply (#4428 / parent #1690).
 *
 * Admin opens Developer → Pipelines detail, rejects a cloud stylesheet URI, saves
 * the bundled local XSL fixture, saves an HTTP tank so execute has rows, then
 * Test HTML shows PIPE-XSL-HTML (stylesheet merge, not raw JSON).
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=…
 *     npm run test:surface -- --path tests/developer-pipelines-result-page-html.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { catalogOpenByExactName } = require("./helpers/developer-catalog-selectors");

const BUNDLED_URI = "pipeline-xsl-result-fixture";
const CLOUD_URI = "https://cdn.example/result.xsl";
const HTML_RESOURCE = "xslHtmlFixture";
const HTTP_FIXTURE = "http://127.0.0.1/pipeline-http-fixture";

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
  await expect(page.locator('[data-testid="developer-pipe-result-page"]')).toBeVisible({
    timeout: 15_000,
  });
}

test.describe("Developer Pipelines result-page HTML apply (#4428 / Slice D)", () => {
  test(
    "Admin saves local XSL, rejects cloud URI, Test HTML shows transformed markup",
    { tag: ["@developer", "@pipelines", "@result-page"] },
    async ({ page }) => {
      test.setTimeout(180_000);
      const { pageErrors, consoleErrors } = attachConsoleGuards(page);

      await loginAsAdmin(page);
      await openPipelinesCatalog(page);
      const appName = await resolvePipelineAppName(page);
      await openPipelineDetail(page, appName);

      const resourceInput = page.locator('[data-testid="developer-pipe-invoke-resource"]');
      await expect(resourceInput).toBeVisible({ timeout: 10_000 });
      await resourceInput.fill(HTML_RESOURCE);

      const uriInput = page.locator('[data-testid="developer-pipe-result-page-uri"]');
      await expect(uriInput).toBeVisible();
      await uriInput.fill(CLOUD_URI);

      const cloudWait = page.waitForResponse(
        (r) =>
          /\/services\/pipelines\/[^/?#]+\/resources\/[^/?#]+\/resultPage(?:\?|$)/i.test(
            r.url(),
          ) && r.request().method() === "PUT",
        { timeout: 45_000 },
      );
      await page.locator('[data-testid="developer-pipe-result-page-save"]').click();
      const cloudResp = await cloudWait;
      expect(cloudResp.status(), "cloud stylesheet URI must be 400").toBe(400);
      await expect(page.locator('[data-testid="developer-pipe-result-page-error"]')).toBeVisible({
        timeout: 15_000,
      });
      await expect(page.locator('[data-testid="developer-pipe-result-page-error"]')).toContainText(
        /cloud|local fixture|rejected|400/i,
      );

      await uriInput.fill(BUNDLED_URI);
      const saveWait = page.waitForResponse(
        (r) =>
          /\/services\/pipelines\/[^/?#]+\/resources\/[^/?#]+\/resultPage(?:\?|$)/i.test(
            r.url(),
          ) && r.request().method() === "PUT",
        { timeout: 45_000 },
      );
      await page.locator('[data-testid="developer-pipe-result-page-save"]').click();
      const saveResp = await saveWait;
      expect(saveResp.status(), `result page save status ${saveResp.status()}`).toBe(200);
      await expect(page.locator('[data-testid="developer-pipe-result-page-notice"]')).toBeVisible({
        timeout: 15_000,
      });

      const httpUrl = page.locator('[data-testid="developer-pipe-http-url"]');
      if (await httpUrl.count()) {
        await httpUrl.fill(HTTP_FIXTURE);
        const tankWait = page.waitForResponse(
          (r) =>
            /\/services\/pipelines\/[^/?#]+\/resources\/[^/?#]+\/backendTank(?:\?|$)/i.test(
              r.url(),
            ) && r.request().method() === "PUT",
          { timeout: 45_000 },
        );
        await page.locator('[data-testid="developer-pipe-http-save"]').click();
        const tankResp = await tankWait;
        expect(tankResp.status(), `HTTP tank save status ${tankResp.status()}`).toBe(200);
      }

      const htmlWait = page.waitForResponse(
        (r) =>
          /\/services\/pipelines\/[^/?#]+\/resources\/[^/?#]+\/execute(?:\?|$)/i.test(r.url()) &&
          r.request().method() === "POST",
        { timeout: 45_000 },
      );
      await page.locator('[data-testid="developer-pipe-invoke-html"]').click();
      const htmlResp = await htmlWait;
      expect(htmlResp.status(), `HTML execute status ${htmlResp.status()}`).toBe(200);
      const htmlResult = page.locator('[data-testid="developer-pipe-invoke-html-result"]');
      await expect(htmlResult).toBeVisible({ timeout: 20_000 });
      const text = (await htmlResult.innerText()).trim();
      expect(text).toMatch(/PIPE-XSL-HTML/);
      expect(text).toMatch(/SKU-1/);
      expect(text).not.toMatch(/invented/i);

      assertConsoleClean(pageErrors, consoleErrors);
    },
  );
});
