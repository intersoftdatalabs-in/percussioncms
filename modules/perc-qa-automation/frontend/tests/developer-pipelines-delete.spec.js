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
 * Developer Pipelines application delete (#6463 / parent #1690, APP-08).
 *
 * Admin opens Developer → Pipelines, imports a uniquely-named copy of an
 * existing design XML (create-only import), opens the copy, cancels a delete
 * confirm (row stays), then confirms: DELETE is 204, the catalog row drops
 * without a refetch, and the application is gone (GET detail is 404).
 *
 * Consumes REST/SPA tips from #6463.
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… \
 *     npm run test:surface -- --path tests/developer-pipelines-delete.spec.js
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
  // Deterministic round-trip: the packaged DTD application exports design XML
  // that re-imports cleanly (200). sys_* content apps may reference classic
  // files (blank.dtd) that fail create-only import with 500.
  const exact = names.find((n) => n === "DTD");
  if (exact) {
    return exact;
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

test.describe("Developer Pipelines application delete (#6463)", () => {
  test(
    "Admin imports a copy, cancels delete (row stays), confirms delete (row drops, GET 404)",
    { tag: ["@developer", "@pipelines", "@delete"] },
    async ({ page }) => {
      test.setTimeout(240_000);
      const { pageErrors, consoleErrors } = attachConsoleGuards(page);

      await loginAsAdmin(page);
      await openPipelinesCatalog(page);

      const sourceName = await resolvePipelineAppName(page);

      // Copy the source design XML through the same authenticated context the
      // Admin SPA session uses (peer of the import/export spec).
      const exportResp = await page.request.get(
        `${BASE_URL}/Rhythmyx/services/pipelines/${encodeURIComponent(sourceName)}/export`,
        { headers: { Accept: "application/xml" } },
      );
      expect(exportResp.status(), "source export is 200").toBe(200);
      const exportBody = await exportResp.text();
      expect(exportBody, "export body carries PSXApplication design XML").toContain(
        "PSXApplication",
      );

      // Create-only import of a uniquely-named copy via the catalog UI.
      const copyName = `night-qa-del-${Date.now()}`;
      await expect(page.locator('[data-testid="developer-pipe-import"]')).toBeVisible({
        timeout: 10_000,
      });
      await page.locator('[data-testid="developer-pipe-import-file"]').setInputFiles({
        name: `${copyName}.xml`,
        mimeType: "application/xml",
        buffer: Buffer.from(exportBody, "utf8"),
      });
      await page.locator('[data-testid="developer-pipe-import-name"]').fill(copyName);
      const importWait = page.waitForResponse(
        (r) =>
          /\/services\/pipelines\/import/i.test(r.url()) &&
          r.request().method() === "POST",
        { timeout: 60_000 },
      );
      await page.locator('[data-testid="developer-pipe-import-submit"]').click();
      const importResp = await importWait;
      if (importResp.status() !== 200) {
        // A failed create-only import can leave a partial application behind
        // (classic validation gap); remove it so the cell stays clean, then
        // fail with the real status for diagnosis.
        await page.request.delete(
          `${BASE_URL}/Rhythmyx/services/pipelines/${encodeURIComponent(copyName)}`,
        );
        expect(importResp.status(), "import copy is 200").toBe(200);
      }
      await expect(page.locator('[data-testid="developer-pipe-import-notice"]')).toBeVisible({
        timeout: 30_000,
      });

      await openPipelineByName(page, copyName);

      // Fail-closed while running: stop the copy first when it is active.
      const stopBtn = page.locator('[data-testid="developer-pipe-stop"]');
      if (await stopBtn.isEnabled()) {
        const stopWait = page.waitForResponse(
          (r) =>
            new RegExp(`/services/pipelines/${copyName}/stop`, "i").test(r.url()) &&
            r.request().method() === "POST",
          { timeout: 60_000 },
        );
        await stopBtn.click();
        const stopped = await stopWait;
        expect(stopped.status(), "stop copy is 200").toBe(200);
        await expect(stopBtn).toBeDisabled({ timeout: 30_000 });
      }

      // Cancel writes nothing: no DELETE fires, the detail stays mounted.
      const deleteBtn = page.locator('[data-testid="developer-pipe-delete"]');
      await expect(deleteBtn).toBeEnabled({ timeout: 10_000 });
      await deleteBtn.click();
      const dialog = page.locator('[data-testid="developer-catalog-confirm-dialog"]');
      await expect(dialog).toBeVisible({ timeout: 10_000 });
      await page.locator('[data-testid="developer-catalog-confirm-cancel"]').click();
      await expect(dialog).toHaveCount(0, { timeout: 10_000 });
      await expect(page.locator('[data-testid="developer-pipe-detail"]')).toBeVisible();

      // Confirm deletes: DELETE is 204, back at the catalog, row dropped.
      await deleteBtn.click();
      await expect(dialog).toBeVisible({ timeout: 10_000 });
      const deleteWait = page.waitForResponse(
        (r) =>
          new RegExp(
            `/services/pipelines/${copyName.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")}$`,
            "i",
          ).test(r.url()) && r.request().method() === "DELETE",
        { timeout: 60_000 },
      );
      await page.locator('[data-testid="developer-catalog-confirm-submit"]').click();
      const deleteResp = await deleteWait;
      expect(deleteResp.status(), `delete copy status`).toBe(204);
      await expect(page.locator('[data-testid="developer-pipe-panel"]')).toBeVisible({
        timeout: 30_000,
      });
      await expect(
        page.locator(catalogOpenByExactName("developer-pipe-open", "data-pipe-name", copyName)),
        `deleted row for ${copyName} is gone from the catalog`,
      ).toHaveCount(0, { timeout: 30_000 });

      // The application is gone server-side.
      const detailResp = await page.request.get(
        `${BASE_URL}/Rhythmyx/services/pipelines/${encodeURIComponent(copyName)}`,
        { headers: { Accept: "application/json" } },
      );
      expect(detailResp.status(), "deleted application GET is 404").toBe(404);

      assertConsoleClean(pageErrors, consoleErrors);
    },
  );
});
