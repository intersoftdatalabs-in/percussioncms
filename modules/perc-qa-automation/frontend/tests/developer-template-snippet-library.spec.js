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
 * Developer Templates AS-09 snippet library insert (#4307 / parent #1690).
 *
 * Opens Developer → Templates, loads a template detail source editor, opens the
 * Velocity snippet library (GET /services/velocity/snippets), inserts a known
 * field macro at the caret, and asserts the source value + notice.
 *
 * Consumes REST/SPA tips #4305 / #4306 (#4311 / #4312). Requires
 * restVelocityResource on rest-jax-rs (CXF 404 without the ref — GH-2142 class).
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up --skip-image-build --then-qa-deploy-webui
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… \
 *     npm run test:surface -- --path tests/developer-template-snippet-library.spec.js
 *   perc-devctl qa-down
 * </pre>
 *
 * Tags: @as-09 @snippet-library @developer-templates @smoke @ui
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const {
  catalogRowSelector,
} = require("./helpers/developer-catalog-selectors");

/** Stable catalog id from VelocityAdaptor built-in Appendix C list. */
const SNIPPET_ID = "field.field";
const SNIPPET_INSERT = '#field("rx:title")';

function developerTemplatesUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "templates",
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

test.describe("Developer AS-09 template snippet library (#4307)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(120_000);
    await loginAsAdmin(page);
  });

  test("Admin inserts Velocity field.field snippet into template source @as-09 @snippet-library @developer-templates @smoke @ui", async ({
    page,
  }) => {
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await page.goto(developerTemplatesUrl(), { waitUntil: "domcontentloaded" });

    await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(
      page.locator('[data-testid="tab-developer-templates"]'),
    ).toBeVisible({ timeout: 15_000 });

    const error = page.locator('[data-testid="developer-tpl-error"]');
    const panel = page.locator('[data-testid="developer-tpl-panel"]');
    const empty = page.locator('[data-testid="developer-tpl-empty"]');

    await expect(panel.or(empty).or(error).first()).toBeVisible({
      timeout: 30_000,
    });

    if (await error.isVisible()) {
      const msg = (await error.innerText()).trim();
      throw new Error(`Templates catalog error: ${msg}`);
    }

    if (await empty.isVisible()) {
      test.skip(true, "No templates in CMS — cannot exercise AS-09 snippet insert");
      return;
    }

    const firstRow = page.locator(catalogRowSelector("developer-tpl-row", 0));
    await expect(firstRow).toBeVisible({ timeout: 15_000 });
    const openBtn = firstRow.locator("button");
    await expect(openBtn).toBeVisible({ timeout: 5_000 });
    await openBtn.click();

    await expect(
      page.locator('[data-testid="developer-tpl-detail"]'),
    ).toBeVisible({ timeout: 20_000 });
    await expect(
      page.locator('[data-testid="developer-tpl-source"]'),
    ).toBeVisible();

    // Ensure edit mode (preview hides the textarea / insert path).
    const sourceEdit = page.locator(
      '[data-testid="developer-tpl-source-edit"]',
    );
    if ((await sourceEdit.count()) === 0) {
      await page.locator('[data-testid="developer-tpl-source-mode"]').click();
      await expect(sourceEdit).toBeVisible({ timeout: 5_000 });
    }

    const before = await sourceEdit.inputValue();
    // Insert is client-side only (textarea + notice). Never Save — shared H2
    // template bodies must stay unchanged across QA reruns.
    const mutating = [];
    page.on("request", (req) => {
      const method = req.method();
      if (method === "PUT" || method === "POST" || method === "DELETE") {
        mutating.push(`${method} ${req.url()}`);
      }
    });

    const snippetsResponsePromise = page.waitForResponse(
      (r) => {
        if (r.request().method() !== "GET") return false;
        try {
          const u = new URL(r.url());
          return /\/services\/velocity\/snippets\/?$/.test(u.pathname);
        } catch {
          return false;
        }
      },
      { timeout: 30_000 },
    );

    await page.locator('[data-testid="developer-tpl-snippet-open"]').click();

    const snippetsResp = await snippetsResponsePromise;
    expect(
      snippetsResp.ok(),
      `GET /services/velocity/snippets HTTP ${snippetsResp.status()} (CXF 404 ⇒ missing restVelocityResource)`,
    ).toBeTruthy();

    await expect(
      page.locator('[data-testid="developer-tpl-snippet-dialog"]'),
    ).toBeVisible({ timeout: 15_000 });

    const loadError = page.locator('[data-testid="developer-tpl-snippet-error"]');
    if (await loadError.isVisible()) {
      throw new Error(
        `Snippet library load error: ${(await loadError.innerText()).trim()}`,
      );
    }

    await expect(
      page.locator('[data-testid="developer-tpl-snippet-loading"]'),
    ).toHaveCount(0, { timeout: 20_000 });
    await expect(
      page.locator('[data-testid="developer-tpl-snippet-table"]'),
    ).toBeVisible({ timeout: 15_000 });

    // Category filter + search narrow to the known field.field row.
    await page.locator('[data-testid="developer-tpl-snippet-cat-field"]').click();
    await page.locator('[data-testid="developer-tpl-snippet-filter"]').fill("field.field");

    const snipRow = page.locator(
      `[data-testid="developer-tpl-snippet-row-${SNIPPET_ID}"]`,
    );
    await expect(snipRow).toBeVisible({ timeout: 10_000 });
    await snipRow.click();

    await expect(
      page.locator('[data-testid="developer-tpl-snippet-preview"]'),
    ).toContainText(SNIPPET_INSERT);

    await page.locator('[data-testid="developer-tpl-snippet-insert"]').click();

    await expect(
      page.locator('[data-testid="developer-tpl-snippet-dialog"]'),
    ).toHaveCount(0, { timeout: 10_000 });

    await expect(sourceEdit).toBeVisible();
    const after = await sourceEdit.inputValue();
    expect(
      after.includes(SNIPPET_INSERT),
      `source after insert should contain ${SNIPPET_INSERT}; beforeLen=${before.length} afterLen=${after.length}`,
    ).toBeTruthy();
    expect(after.length).toBeGreaterThanOrEqual(
      before.length + SNIPPET_INSERT.length,
    );

    await expect(
      page.locator('[data-testid="developer-tpl-detail-notice"]'),
    ).toBeVisible({ timeout: 5_000 });
    await expect(
      page.locator('[data-testid="developer-tpl-detail-notice"]'),
    ).toContainText(/Snippet inserted/i);

    await expect(
      page.locator('[data-testid="developer-tpl-save"]'),
    ).toBeVisible();
    expect(
      mutating,
      `snippet insert must not PUT/POST/DELETE template bodies: ${mutating.join(" | ")}`,
    ).toEqual([]);

    assertConsoleClean(pageErrors, consoleErrors);
  });
});
