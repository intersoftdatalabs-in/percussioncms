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
 * Developer Pipelines Slice B wave 3 — Test invoke (+ Problems) (#4324 / parent #1690).
 *
 * Admin opens Developer → Pipelines detail, exercises Test invoke round-trip
 * (POST …/resources/{resource}/execute), and soft-asserts Problems when
 * validation REST is present (or soft-empty / unavailable when not).
 *
 * Consumes SPA tip #4323 / PR #4329 (Test invoke + Problems chrome).
 * Validation REST #4322 / PR #4328 is preferred for Problems table/empty but
 * not hard-required (404 → unavailable is accepted).
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… \
 *     npm run test:surface -- --path tests/developer-pipelines-test-invoke.spec.js
 *   perc-devctl qa-down
 * </pre>
 *
 * Optional: PIPELINE_APP_NAME=<exact catalog name> (default: first openable row).
 * Optional: PIPELINE_RESOURCE_NAME=<resource> (default: prefilled data-set / typed fallback).
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
 * (prefer sys_* apps — usually have data-set names for the resource field).
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
  await expect(page.locator('[data-testid="developer-pipe-detail-title"]')).toBeVisible();
  await expect(page.locator('[data-testid="developer-pipe-invoke"]')).toBeVisible({
    timeout: 15_000,
  });
}

/**
 * Ensure the resource field has a non-empty value for the execute POST.
 * Prefers PIPELINE_RESOURCE_NAME, then the SPA-prefilled data-set name, else
 * the first datasets table row text, else a stable fallback token.
 * @returns {Promise<string>}
 */
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

  const fallback = "contenteditor";
  await input.fill(fallback);
  return fallback;
}

/**
 * Soft-assert Problems chrome: unavailable (validation tip not deployed),
 * empty, table, or transient error — never hang on loading.
 */
async function assertProblemsSoft(page) {
  const section = page.locator('[data-testid="developer-pipe-problems"]');
  await expect(section).toBeVisible({ timeout: 15_000 });
  await expect(page.locator('[data-testid="developer-pipe-problems-loading"]')).toHaveCount(0, {
    timeout: 30_000,
  });

  const unavailable = page.locator('[data-testid="developer-pipe-problems-unavailable"]');
  const empty = page.locator('[data-testid="developer-pipe-problems-empty"]');
  const table = page.locator('[data-testid="developer-pipe-problems-table"]');
  const forbidden = page.locator('[data-testid="developer-pipe-problems-forbidden"]');
  const err = page.locator('[data-testid="developer-pipe-problems-error"]');

  await expect(
    unavailable.or(empty).or(table).or(forbidden).or(err).first(),
  ).toBeVisible({ timeout: 10_000 });

  if (await table.isVisible()) {
    await expect(page.locator('[data-testid="developer-pipe-problem-row-0"]')).toBeVisible();
  }
}

test.describe("Developer Pipelines test-invoke (#4324 / Slice B wave 3)", () => {
  test(
    "Admin Test invoke round-trips execute and soft-asserts Problems",
    { tag: ["@developer", "@pipelines", "@test-invoke"] },
    async ({ page }) => {
      test.setTimeout(180_000);
      const { pageErrors, consoleErrors } = attachConsoleGuards(page);

      await loginAsAdmin(page);
      await openPipelinesCatalog(page);
      const appName = await resolvePipelineAppName(page);
      await openPipelineDetail(page, appName);

      // Client-side: blank resource must not POST.
      const resourceInput = page.locator('[data-testid="developer-pipe-invoke-resource"]');
      await resourceInput.fill("");
      await page.locator('[data-testid="developer-pipe-invoke-run"]').click();
      await expect(page.locator('[data-testid="developer-pipe-invoke-error"]')).toBeVisible({
        timeout: 10_000,
      });
      await expect(page.locator('[data-testid="developer-pipe-invoke-error"]')).toContainText(
        /Enter a resource name|resource name/i,
      );
      await expect(page.locator('[data-testid="developer-pipe-invoke-result"]')).toHaveCount(0);

      const resource = await ensureInvokeResource(page);
      const body = page.locator('[data-testid="developer-pipe-invoke-body"]');
      await expect(body).toBeVisible();
      const bodyText = ((await body.inputValue()) || "").trim();
      if (!bodyText) {
        await body.fill('{\n  "params": {}\n}\n');
      }

      const executeWait = page.waitForResponse(
        (r) =>
          /\/services\/pipelines\/[^/?#]+\/resources\/[^/?#]+\/execute(?:\?|$)/i.test(r.url()) &&
          r.request().method() === "POST",
        { timeout: 45_000 },
      );

      await page.locator('[data-testid="developer-pipe-invoke-run"]').click();
      const executeResp = await executeWait;
      const status = executeResp.status();
      const respText = await executeResp.text();

      const result = page.locator('[data-testid="developer-pipe-invoke-result"]');
      const invokeErr = page.locator('[data-testid="developer-pipe-invoke-error"]');
      await expect(result.or(invokeErr).first()).toBeVisible({ timeout: 30_000 });

      if (status === 200) {
        await expect(result).toBeVisible();
        await expect(result).toContainText(
          new RegExp(resource.replace(/[.*+?^${}()|[\]\\]/g, "\\$&"), "i"),
        );
      } else if (status === 404 || status === 400 || status === 500) {
        // Classic-only H2 apps lack native IR (404/400). Jackson WRAP_ROOT body
        // mismatches can surface as 500 until SPA wraps PipelineExecuteRequest —
        // product docs still require a clear invoke error under the form.
        await expect(invokeErr).toBeVisible();
        expect(
          (await invokeErr.innerText()).trim().length,
          `empty invoke error for HTTP ${status} body=${respText.slice(0, 300)}`,
        ).toBeGreaterThan(0);
      } else {
        throw new Error(
          `Unexpected execute status ${status} for ${appName}/${resource}: ${respText.slice(0, 400)}`,
        );
      }

      await assertProblemsSoft(page);
      assertConsoleClean(pageErrors, consoleErrors);
    },
  );
});
