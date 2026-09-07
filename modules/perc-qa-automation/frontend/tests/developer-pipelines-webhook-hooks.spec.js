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
 * Developer Pipelines Slice C — HTTP webhook hooks persist + Test invoke (#4367 / parent #1690).
 *
 * Admin opens Developer → Pipelines detail, rejects a cloud webhook URL, saves
 * bundled loopback webhook pre/post hooks, saves HTTP tank so execute has rows,
 * then Test invoke shows real fixture status/body (hook-ok) — not a fake success.
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… \
 *     npm run test:surface -- --path tests/developer-pipelines-webhook-hooks.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { catalogOpenByExactName } = require("./helpers/developer-catalog-selectors");

const BUNDLED_HTTP = "http://127.0.0.1/pipeline-http-fixture";
const BUNDLED_WEBHOOK = "http://127.0.0.1/pipeline-webhook-fixture";
const CLOUD_URL = "https://hooks.example/catch";

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
  await expect(page.locator('[data-testid="developer-pipe-webhook"]')).toBeVisible({
    timeout: 15_000,
  });
}

async function ensureInvokeResource(page) {
  const preferred = (process.env.PIPELINE_RESOURCE_NAME || "").trim();
  const input = page.locator('[data-testid="developer-pipe-invoke-resource"]');
  await expect(input).toBeVisible({ timeout: 10_000 });
  if (preferred) {
    await input.fill(preferred);
    return preferred;
  }
  let current = ((await input.inputValue()) || "").trim();
  if (current) {
    return current;
  }
  const firstDs = page.locator('[data-testid="developer-pipe-ds-row-0"]');
  if (await firstDs.count()) {
    const text = ((await firstDs.innerText()) || "").trim().split(/\s+/)[0] || "";
    if (text) {
      await input.fill(text);
      return text;
    }
  }
  const fallback = "httpItems";
  await input.fill(fallback);
  return fallback;
}

test.describe("Developer Pipelines webhook hooks (#4367 / Slice C)", () => {
  test(
    "Admin saves loopback webhooks, rejects cloud URL, Test invoke shows fixture status/body",
    { tag: ["@developer", "@pipelines", "@webhook-hooks"] },
    async ({ page }) => {
      test.setTimeout(180_000);
      const { pageErrors, consoleErrors } = attachConsoleGuards(page);

      await loginAsAdmin(page);
      await openPipelinesCatalog(page);
      const appName = await resolvePipelineAppName(page);
      await openPipelineDetail(page, appName);

      const resource = await ensureInvokeResource(page);

      const preInput = page.locator('[data-testid="developer-pipe-webhook-pre"]');
      await expect(preInput).toBeVisible();
      await preInput.fill(CLOUD_URL);

      const cloudWait = page.waitForResponse(
        (r) =>
          /\/services\/pipelines\/[^/?#]+\/resources\/[^/?#]+\/webhookHooks(?:\?|$)/i.test(
            r.url(),
          ) && r.request().method() === "PUT",
        { timeout: 45_000 },
      );
      await page.locator('[data-testid="developer-pipe-webhook-save"]').click();
      const cloudResp = await cloudWait;
      expect(cloudResp.status(), "cloud webhook URL must be 400").toBe(400);
      await expect(page.locator('[data-testid="developer-pipe-webhook-error"]')).toBeVisible({
        timeout: 15_000,
      });
      await expect(page.locator('[data-testid="developer-pipe-webhook-error"]')).toContainText(
        /loopback|cloud|rejected|400/i,
      );

      await preInput.fill(BUNDLED_WEBHOOK);
      await page.locator('[data-testid="developer-pipe-webhook-post"]').fill(BUNDLED_WEBHOOK);
      const saveWait = page.waitForResponse(
        (r) =>
          /\/services\/pipelines\/[^/?#]+\/resources\/[^/?#]+\/webhookHooks(?:\?|$)/i.test(
            r.url(),
          ) && r.request().method() === "PUT",
        { timeout: 45_000 },
      );
      await page.locator('[data-testid="developer-pipe-webhook-save"]').click();
      const saveResp = await saveWait;
      expect(saveResp.status(), `webhook save status ${saveResp.status()}`).toBe(200);
      await expect(page.locator('[data-testid="developer-pipe-webhook-notice"]')).toBeVisible({
        timeout: 15_000,
      });

      const adapter = page.locator('[data-testid="developer-pipe-http-adapter"]');
      await expect(adapter).toBeVisible();
      await adapter.selectOption("HTTP");
      await page.locator('[data-testid="developer-pipe-http-url"]').fill(BUNDLED_HTTP);
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

      const executeWait = page.waitForResponse(
        (r) =>
          /\/services\/pipelines\/[^/?#]+\/resources\/[^/?#]+\/execute(?:\?|$)/i.test(r.url()) &&
          r.request().method() === "POST",
        { timeout: 45_000 },
      );
      await page.locator('[data-testid="developer-pipe-invoke-run"]').click();
      const executeResp = await executeWait;
      expect(executeResp.status(), `execute status ${executeResp.status()}`).toBe(200);
      const result = page.locator('[data-testid="developer-pipe-invoke-result"]');
      await expect(result).toBeVisible({ timeout: 20_000 });
      const text = (await result.innerText()).trim();
      expect(text.length).toBeGreaterThan(0);
      expect(text).toMatch(/hook-ok|pipeline-webhook/i);
      expect(text).toMatch(/preWebhookStatus|postWebhookStatus|"status":\s*200|status=200/i);
      expect(text).not.toMatch(/invented/i);

      assertConsoleClean(pageErrors, consoleErrors);
    },
  );
});
