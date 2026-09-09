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
 * Developer Pipelines Slice D — classic result-page import + inspect (#4429 / parent #1690).
 *
 * Admin opens Developer → Pipelines detail for a classic XML Application whose
 * GET IR includes imported result pages (extension, MIME, stylesheet URI).
 * Inspect table is read-only. Does not rewrite classic XML; does not stack
 * presentation=none (#4430).
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=…
 *     npm run test:surface -- --path tests/developer-pipelines-result-page-import.spec.js
 *   perc-devctl qa-down
 * </pre>
 *
 * Optional: PIPELINE_APP_NAME=<exact catalog name>
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

function unwrapIr(payload) {
  if (!payload || typeof payload !== "object") return payload;
  if (payload.PipelineIrDocument && typeof payload.PipelineIrDocument === "object") {
    return payload.PipelineIrDocument;
  }
  return payload;
}

function firstInspectPage(ir) {
  const doc = unwrapIr(ir);
  const resources = (doc && doc.resources) || [];
  for (const res of resources) {
    const listed = (res.resultPages || []).filter((p) => p && p.stylesheetUri);
    if (listed.length) {
      return { resource: res, page: listed[0], pages: listed };
    }
    if (res.resultPage && res.resultPage.stylesheetUri) {
      return { resource: res, page: res.resultPage, pages: [res.resultPage] };
    }
  }
  return null;
}

function rankNames(names) {
  const preferred = (process.env.PIPELINE_APP_NAME || "").trim();
  const unique = [...new Set(names.filter(Boolean))];
  const score = (n) => {
    if (preferred && n === preferred) return 0;
    if (/^sys_welcome$/i.test(n)) return 1;
    if (/^sys_Keywords$/i.test(n)) return 2;
    if (/^sys_/i.test(n)) return 3;
    return 4;
  };
  return unique.sort((a, b) => score(a) - score(b) || a.localeCompare(b));
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

async function catalogNames(page) {
  const opens = page.locator('[data-testid="developer-pipe-open"][data-pipe-name]');
  await expect(opens.first()).toBeVisible({ timeout: 20_000 });
  const count = await opens.count();
  const names = [];
  for (let i = 0; i < count; i++) {
    const name = ((await opens.nth(i).getAttribute("data-pipe-name")) || "").trim();
    if (name) names.push(name);
  }
  return names;
}

async function findImportedResultPages(page, names) {
  const ranked = rankNames(names);
  const tried = [];
  for (const name of ranked) {
    const irUrl = `${BASE_URL}/Rhythmyx/services/pipelines/${encodeURIComponent(name)}/ir`;
    const resp = await page.request.get(irUrl);
    tried.push(`${name}:${resp.status()}`);
    if (resp.status() !== 200) {
      continue;
    }
    let ir;
    try {
      ir = await resp.json();
    } catch {
      continue;
    }
    const hit = firstInspectPage(ir);
    if (hit) {
      return { name, ir, hit };
    }
  }
  throw new Error(
    `No catalog application returned imported resultPages on GET IR. Tried ${tried.join(", ")}`,
  );
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
}

test.describe("Developer Pipelines classic result-page import inspect (#4429 / Slice D)", () => {
  test(
    "GET IR imported result pages show extension, MIME, and stylesheet in Developer chrome",
    { tag: ["@developer", "@pipelines", "@result-page"] },
    async ({ page }) => {
      test.setTimeout(180_000);
      const { pageErrors, consoleErrors } = attachConsoleGuards(page);

      await loginAsAdmin(page);
      await openPipelinesCatalog(page);
      const names = await catalogNames(page);
      const found = await findImportedResultPages(page, names);
      await openPipelineDetail(page, found.name);

      await expect(page.locator('[data-testid="developer-pipe-ir"]')).toBeVisible({
        timeout: 20_000,
      });
      await expect(page.locator('[data-testid="developer-pipe-ir-loading"]')).toHaveCount(0, {
        timeout: 30_000,
      });
      const table = page.locator('[data-testid^="developer-pipe-ir-result-pages-"]');
      await expect(table.first()).toBeVisible({ timeout: 20_000 });
      const xsl = page.locator('[data-testid^="developer-pipe-ir-result-page-xsl-"]');
      await expect(xsl.first()).toBeVisible();
      const xslTexts = await xsl.allInnerTexts();
      expect(xslTexts.map((t) => t.trim())).toContain(found.hit.page.stylesheetUri);
      const row = page.locator('[data-testid^="developer-pipe-ir-result-page-"]').filter({
        has: page.locator('[data-testid^="developer-pipe-ir-result-page-xsl-"]'),
      }).first();
      await expect(row).toBeVisible();
      const rowText = (await row.innerText()).trim();
      if (found.hit.page.requestExtension) {
        expect(rowText).toContain(found.hit.page.requestExtension);
      }
      if (found.hit.page.mimeType) {
        expect(rowText).toContain(found.hit.page.mimeType);
      }
      expect(found.hit.page.stylesheetUri).not.toMatch(/\.\.|\/\/|file:\//i);

      assertConsoleClean(pageErrors, consoleErrors);
    },
  );
});
