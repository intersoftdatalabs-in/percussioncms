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
 * Developer Pipelines Admin enable/disable (#6046 / parent #1690).
 *
 * Admin opens Developer → Pipelines detail, exercises the Disable/Enable
 * round-trip, asserts the Enabled meta + lifecycle notice, and restores the
 * prior enabled (and running) state.
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… \
 *     npm run test:surface -- --path tests/developer-pipelines-enabled.spec.js
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

/** Apps that must not be enabled/disabled on a live H2 cell (shell / core CMS). */
const CRITICAL_PIPELINE_APPS = new Set([
  "administration",
  "cm",
  "rhythmyx",
  "webservices",
]);

/**
 * Prefer PIPELINE_APP_NAME; else a non-critical catalog open control
 * (prefer sys_* content-editor apps when present).
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
  const safe = names.filter((n) => !CRITICAL_PIPELINE_APPS.has(n.toLowerCase()));
  const sysPreferred = safe.find((n) => /^sys_/i.test(n));
  const chosen = sysPreferred || safe[0] || names[0];
  if (CRITICAL_PIPELINE_APPS.has(chosen.toLowerCase())) {
    throw new Error(
      `Only critical pipeline apps available (${chosen}); set PIPELINE_APP_NAME to a safe app`,
    );
  }
  return chosen;
}

async function openPipelineByName(page, name) {
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
  await expect(page.locator('[data-testid="developer-pipe-lifecycle"]')).toBeVisible({
    timeout: 10_000,
  });
  await expect(page.locator('[data-testid="developer-pipe-meta-enabled"]')).toBeVisible();
  await expect(page.locator('[data-testid="developer-pipe-meta-running"]')).toBeVisible();
}

async function readYesNo(page, testId) {
  const text = (await page.locator(`[data-testid="${testId}"]`).innerText()).trim();
  if (/^yes$/i.test(text)) return true;
  if (/^no$/i.test(text)) return false;
  throw new Error(`Unexpected ${testId} meta text: ${text}`);
}

async function clickEnabled(page, enable) {
  const testId = enable ? "developer-pipe-enable" : "developer-pipe-disable";
  const btn = page.locator(`[data-testid="${testId}"]`);
  await expect(btn).toBeEnabled({ timeout: 10_000 });
  const putWait = page.waitForResponse(
    (r) =>
      /\/services\/pipelines\/[^/?#]+\/enabled/i.test(r.url()) &&
      r.request().method() === "PUT",
    { timeout: 45_000 },
  );
  await btn.click();
  const putResp = await putWait;
  const putStatus = putResp.status();
  const putText = await putResp.text();
  const notice = page.locator('[data-testid="developer-pipe-lifecycle-notice"]');
  const err = page.locator('[data-testid="developer-pipe-lifecycle-error"]');
  await expect(notice.or(err).first()).toBeVisible({ timeout: 30_000 });
  if (await err.isVisible()) {
    throw new Error(
      `${enable ? "enable" : "disable"} failed: ${(await err.innerText()).trim()} | PUT ${putStatus} resp=${putText}`,
    );
  }
  expect(putStatus, `PUT enabled status resp=${putText}`).toBe(200);
  const expectedNotice = enable ? /Application enabled/i : /Application disabled/i;
  await expect(notice).toContainText(expectedNotice);
}

async function clickLifecycle(page, action) {
  const testId = action === "start" ? "developer-pipe-start" : "developer-pipe-stop";
  const pathSuffix = action === "start" ? "/start" : "/stop";
  const btn = page.locator(`[data-testid="${testId}"]`);
  await expect(btn).toBeEnabled({ timeout: 10_000 });
  const postWait = page.waitForResponse(
    (r) =>
      /\/services\/pipelines\/[^/?#]+\/(start|stop)/i.test(r.url()) &&
      r.url().toLowerCase().endsWith(pathSuffix) &&
      r.request().method() === "POST",
    { timeout: 45_000 },
  );
  await btn.click();
  const postResp = await postWait;
  expect(postResp.status(), `POST ${action} status`).toBe(200);
  const expectedNotice =
    action === "start" ? /Application started/i : /Application stopped/i;
  await expect(page.locator('[data-testid="developer-pipe-lifecycle-notice"]')).toContainText(
    expectedNotice,
  );
}

test.describe("Developer Pipelines enable/disable (#6046)", () => {
  test(
    "Admin can disable and re-enable a pipeline application and restore state",
    { tag: ["@developer", "@pipelines", "@lifecycle"] },
    async ({ page }) => {
      test.setTimeout(180_000);
      const { pageErrors, consoleErrors } = attachConsoleGuards(page);

      await loginAsAdmin(page);
      await openPipelinesCatalog(page);
      const appName = await resolvePipelineAppName(page);
      await openPipelineByName(page, appName);

      const initiallyEnabled = await readYesNo(page, "developer-pipe-meta-enabled");
      const initiallyRunning = await readYesNo(page, "developer-pipe-meta-running");
      const enableBtn = page.locator('[data-testid="developer-pipe-enable"]');
      const disableBtn = page.locator('[data-testid="developer-pipe-disable"]');

      try {
        if (initiallyEnabled) {
          await expect(disableBtn).toBeEnabled();
          await expect(enableBtn).toBeDisabled();
          await clickEnabled(page, false);
          await expect
            .poll(async () => readYesNo(page, "developer-pipe-meta-enabled"), {
              timeout: 20_000,
            })
            .toBe(false);
          await expect(enableBtn).toBeEnabled();
          await expect(disableBtn).toBeDisabled();

          await clickEnabled(page, true);
          await expect
            .poll(async () => readYesNo(page, "developer-pipe-meta-enabled"), {
              timeout: 20_000,
            })
            .toBe(true);
          await expect(disableBtn).toBeEnabled();
          await expect(enableBtn).toBeDisabled();
        } else {
          await expect(enableBtn).toBeEnabled();
          await expect(disableBtn).toBeDisabled();
          await clickEnabled(page, true);
          await expect
            .poll(async () => readYesNo(page, "developer-pipe-meta-enabled"), {
              timeout: 20_000,
            })
            .toBe(true);
          await expect(disableBtn).toBeEnabled();
          await expect(enableBtn).toBeDisabled();

          await clickEnabled(page, false);
          await expect
            .poll(async () => readYesNo(page, "developer-pipe-meta-enabled"), {
              timeout: 20_000,
            })
            .toBe(false);
          await expect(enableBtn).toBeEnabled();
          await expect(disableBtn).toBeDisabled();
        }

        assertConsoleClean(pageErrors, consoleErrors);
      } finally {
        // Best-effort restore so a mid-flow assertion failure does not poison H2.
        try {
          if (await page.locator('[data-testid="developer-pipe-detail"]').isVisible()) {
            const nowEnabled = await readYesNo(page, "developer-pipe-meta-enabled");
            if (nowEnabled !== initiallyEnabled) {
              await clickEnabled(page, initiallyEnabled);
              await expect
                .poll(async () => readYesNo(page, "developer-pipe-meta-enabled"), {
                  timeout: 20_000,
                })
                .toBe(initiallyEnabled);
            }
            const nowRunning = await readYesNo(page, "developer-pipe-meta-running");
            if (nowRunning !== initiallyRunning) {
              await clickLifecycle(page, initiallyRunning ? "start" : "stop");
              await expect
                .poll(async () => readYesNo(page, "developer-pipe-meta-running"), {
                  timeout: 20_000,
                })
                .toBe(initiallyRunning);
            }
          }
        } catch (restoreErr) {
          console.warn(
            `pipeline enable/disable restore failed for ${appName}: ${restoreErr}`,
          );
        }
      }
    },
  );
});
