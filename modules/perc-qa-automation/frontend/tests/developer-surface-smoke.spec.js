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
 * Developer surface smoke (#5867 / parent #1690, slice 3).
 *
 * Shell-level companion to `developer-catalog-smoke.spec.js` (which covers
 * content-types, keywords, locales, slots, shared-fields, system-def,
 * item-filters, searches, views, sites, ce-controls, and roles). This spec
 * covers the remaining catalog surfaces touched by recent post-P0 slices —
 * templates (Sites tab #5742, slot assign #5786, source viewer #5865),
 * display-formats (detail readback #5866), communities, workflows,
 * action-menus, extensions, and relationship-types — plus a shell test that
 * every Developer section tab from the `DEVELOPER_SECTIONS` allowlist
 * renders.
 *
 * Each surface asserts a non-error state (panel or empty) with the error
 * alert hidden, and every test asserts zero unexpected console errors and
 * zero page errors.
 *
 * Entry: spa.jsp?entry=developer&section=<slug>
 * Refs #1690.
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… npm run test:surface -- --path tests/developer-surface-smoke.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

/**
 * All Developer shell sections (lockstep with DEVELOPER_SECTIONS in
 * WebUI/src/main/ts/app/deepLinks/allowlists.ts and DeveloperShell).
 */
const ALL_SECTIONS = [
  "content-types",
  "templates",
  "slots",
  "keywords",
  "locales",
  "shared-fields",
  "system-def",
  "item-filters",
  "display-formats",
  "action-menus",
  "searches",
  "views",
  "extensions",
  "relationship-types",
  "workflows",
  "server-configs",
  "application-files",
  "file-explorer",
  "database-explorer",
  "problems",
  "object-sorter",
  "ce-controls",
  "sites",
  "communities",
  "roles",
  "community-visibility",
  "pipelines",
  "preferences",
];

/**
 * Catalog surfaces not covered by developer-catalog-smoke.spec.js.
 *
 * @type {{
 *   section: string,
 *   successTestIds: string[],
 *   errorTestIds: string[],
 * }[]}
 */
const SURFACES = [
  {
    section: "templates",
    successTestIds: ["developer-tpl-panel", "developer-tpl-empty"],
    errorTestIds: ["developer-tpl-error", "developer-tpl-panel-error"],
  },
  {
    section: "display-formats",
    successTestIds: ["developer-df-panel", "developer-df-empty"],
    errorTestIds: ["developer-df-error"],
  },
  {
    section: "communities",
    successTestIds: ["developer-comm-panel", "developer-comm-empty"],
    errorTestIds: ["developer-comm-error"],
  },
  {
    section: "workflows",
    successTestIds: ["developer-wf-panel", "developer-wf-empty"],
    errorTestIds: ["developer-wf-error"],
  },
  {
    section: "action-menus",
    successTestIds: ["developer-am-panel", "developer-am-empty"],
    errorTestIds: ["developer-am-error"],
  },
  {
    section: "extensions",
    successTestIds: ["developer-ex-panel", "developer-ex-empty"],
    errorTestIds: ["developer-ex-error"],
  },
  {
    section: "relationship-types",
    successTestIds: ["developer-rt-panel", "developer-rt-empty"],
    errorTestIds: ["developer-rt-error"],
  },
];

function developerUrl(section) {
  const q = new URLSearchParams({
    entry: "developer",
    section,
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

/**
 * @param {import("@playwright/test").Page} page
 * @returns {{pageErrors: string[], consoleErrors: string[]}}
 */
function attachConsoleGuards(page) {
  const pageErrors = [];
  const consoleErrors = [];
  page.on("pageerror", (err) => {
    pageErrors.push(String((err && err.message) || err));
  });
  page.on("console", (msg) => {
    if (msg.type() === "error") {
      consoleErrors.push(msg.text());
    }
  });
  return { pageErrors, consoleErrors };
}

/**
 * @param {string[]} pageErrors
 * @param {string[]} consoleErrors
 */
function assertConsoleClean(pageErrors, consoleErrors) {
  expect(pageErrors, `pageerror: ${pageErrors.join(" | ")}`).toEqual([]);
  const unexpectedConsole = consoleErrors.filter(
    (text) =>
      !/Failed to load resource: the server responded with a status of (400|403|404|409)/.test(
        text,
      ) &&
      !/rx_resources\/css\/(?:bootstrap|googleFonts)/.test(text) &&
      !/favicon/i.test(text),
  );
  expect(
    unexpectedConsole,
    `console error: ${unexpectedConsole.join(" | ")}`,
  ).toEqual([]);
}

test.describe("Developer surface smoke (#5867 / #1690)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(120_000);
    await loginAsAdmin(page);
  });

  test("shell renders every developer section tab", async ({ page }) => {
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);
    await page.goto(developerUrl("content-types"), {
      waitUntil: "networkidle",
    });

    await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(
      page.locator('[data-testid="perc-developer-shell"]'),
    ).toBeVisible({ timeout: 15_000 });

    for (const section of ALL_SECTIONS) {
      await expect(
        page.locator(`[data-testid="tab-developer-${section}"]`),
        `developer tab "${section}" must render (DEVELOPER_SECTIONS allowlist)`,
      ).toBeVisible({ timeout: 15_000 });
    }

    assertConsoleClean(pageErrors, consoleErrors);
  });

  for (const surface of SURFACES) {
    test(`${surface.section}: surface loads without API error`, async ({
      page,
    }) => {
      const { pageErrors, consoleErrors } = attachConsoleGuards(page);
      await page.goto(developerUrl(surface.section), {
        waitUntil: "networkidle",
      });

      await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
        timeout: 20_000,
      });
      await expect(
        page.locator(`[data-testid="tab-developer-${surface.section}"]`),
      ).toBeVisible({ timeout: 15_000 });

      const success = page.locator(
        surface.successTestIds
          .map((id) => `[data-testid="${id}"]`)
          .join(", "),
      );
      const error = page.locator(
        surface.errorTestIds.map((id) => `[data-testid="${id}"]`).join(", "),
      );

      // Wait until loading finishes: either success surface or error alert.
      await expect(success.or(error).first()).toBeVisible({
        timeout: 30_000,
      });

      for (const id of surface.errorTestIds) {
        await expect(
          page.locator(`[data-testid="${id}"]`),
          `Developer section "${surface.section}" must not show ${id}`,
        ).toBeHidden();
      }

      await expect(success.first()).toBeVisible();

      assertConsoleClean(pageErrors, consoleErrors);
    });
  }
});
