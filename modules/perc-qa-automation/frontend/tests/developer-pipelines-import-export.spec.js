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
 * Developer Pipelines classic XML import/export (#5743 / parent #1690).
 *
 * Admin opens Developer → Pipelines, sees the create-only import section on
 * the catalog, exports one classic XML Application as PSXApplication design
 * XML (read-only, no lock steal), and re-importing that same document is
 * rejected with 409 (duplicate name, no replace, no mutation).
 *
 * Consumes REST/SPA tips from #5743.
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… \
 *     npm run test:surface -- --path tests/developer-pipelines-import-export.spec.js
 *   perc-devctl qa-down
 * </pre>
 *
 * Optional: PIPELINE_APP_NAME=<exact catalog name> (default: first safe row).
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

/** Apps that must not be touched on a live H2 cell (shell / core CMS). */
const CRITICAL_PIPELINE_APPS = new Set([
  "administration",
  "cm",
  "rhythmyx",
  "webservices",
]);

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
}

test.describe("Developer Pipelines classic XML import/export (#5743)", () => {
  test(
    "Admin sees import section, exports design XML, duplicate re-import is 409",
    { tag: ["@developer", "@pipelines", "@import-export"] },
    async ({ page }) => {
      test.setTimeout(180_000);
      const { pageErrors, consoleErrors } = attachConsoleGuards(page);

      await loginAsAdmin(page);
      await openPipelinesCatalog(page);

      // Catalog exposes the Admin create-only import surface.
      await expect(page.locator('[data-testid="developer-pipe-import"]')).toBeVisible({
        timeout: 10_000,
      });
      await expect(
        page.locator('[data-testid="developer-pipe-import-submit"]'),
      ).toBeVisible();
      // Honest empty state: no file chosen yet.
      await expect(
        page.locator('[data-testid="developer-pipe-import-error"]'),
      ).toHaveCount(0);
      await expect(
        page.locator('[data-testid="developer-pipe-import-notice"]'),
      ).toHaveCount(0);

      const appName = await resolvePipelineAppName(page);
      await openPipelineByName(page, appName);

      // Export is enabled for a visible (non-hidden) application.
      const exportBtn = page.locator('[data-testid="developer-pipe-export"]');
      await expect(exportBtn).toBeVisible({ timeout: 10_000 });
      await expect(exportBtn).toBeEnabled({ timeout: 10_000 });

      const exportWait = page.waitForResponse(
        (r) =>
          /\/services\/pipelines\/[^/?#]+\/export/i.test(r.url()) &&
          r.request().method() === "GET",
        { timeout: 45_000 },
      );
      await exportBtn.click();
      const exportResp = await exportWait;
      const exportStatus = exportResp.status();
      const exportBody = await exportResp.text();
      expect(exportStatus, `GET export status resp=${exportBody.slice(0, 200)}`).toBe(200);
      const contentType = (exportResp.headers()["content-type"] || "").toLowerCase();
      expect(contentType, `export content-type=${contentType}`).toContain("xml");
      expect(
        exportBody.trimStart().startsWith("<"),
        `export body is XML, head=${exportBody.slice(0, 120)}`,
      ).toBe(true);
      expect(exportBody, "export body carries PSXApplication design XML").toContain(
        "PSXApplication",
      );
      const disposition = (
        exportResp.headers()["content-disposition"] || ""
      ).toLowerCase();
      expect(disposition, "export has attachment Content-Disposition").toContain("attachment");
      const exportErr = page.locator('[data-testid="developer-pipe-lifecycle-error"]');
      if (await exportErr.isVisible()) {
        throw new Error(`Pipeline export error: ${(await exportErr.innerText()).trim()}`);
      }

      // Create-only import: re-importing the exported document must be 409
      // (name already exists) — no replace, no mutation. Uses the same
      // authenticated browser context as the Admin SPA session.
      const importResp = await page.request.post(
        `${BASE_URL}/Rhythmyx/services/pipelines/import`,
        {
          headers: { "Content-Type": "application/xml", Accept: "application/json" },
          data: exportBody,
        },
      );
      expect(importResp.status(), "duplicate re-import is 409").toBe(409);

      assertConsoleClean(pageErrors, consoleErrors);
    },
  );
});
