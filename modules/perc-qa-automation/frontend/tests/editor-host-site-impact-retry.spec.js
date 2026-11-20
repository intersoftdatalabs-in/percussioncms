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
 * EditorHost site-impact panel load failure with retry (#6771, parent #4532).
 *
 * <p>The site-impact panel is read-only browse surface: populated rows and the
 * explicit empty state are covered by
 * {@code page-metadata-react-editor.spec.js}. This spec proves the failure
 * path — a 500 from
 * {@code GET item/siteimpact/page/{id}} renders the named error alert with a
 * retry control, retry re-runs the load for the same item, success renders
 * rows, and a repeated failure keeps the alert. Focus follow-on (#6786):
 * retrying unmounts the error alert (and the focused Retry button) while
 * the load runs; after the retry settles, focus must land somewhere
 * meaningful — the panel heading on success, or the Retry button again
 * when the failure repeats — and the loading indicator is announced
 * through a live region, never leaving {@code document.activeElement} on
 * {@code <body>}.</p>
 *
 * <p>Tags: {@code @page-metadata-editor} {@code @editor}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-host-site-impact-retry.spec.js}</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const {
  TEST_IDS,
  editorSpaUrl,
} = require("./helpers/editor-host-save-fields");

const PAGE_ID = "42";

function fieldsBody() {
  return {
    ItemEditorFields: {
      contentId: PAGE_ID,
      contentType: "percPage",
      name: "Home",
      checkoutUser: "Admin",
      revision: 3,
      fields: [{ name: "sys_title", value: "Home" }],
    },
  };
}

const BASE_ID = "16777215-101-1";
const PLAIN_ID = "16777215-101-2";

function typeBody() {
  return {
    ContentTypeDetail: {
      name: "percPage",
      fields: [{ name: "sys_title", label: "Filename", control: "sys_EditBox" }],
      // Mirror the peer page-metadata surface: a percPage normally carries
      // template choices. An empty list makes the host fall back to a real
      // site-templates lookup that logs a Demo-site error server-side.
      allowedTemplates: [
        { name: "perc.base.page", label: "Base", guid: { stringValue: BASE_ID } },
        { name: "perc.base.plain", label: "Plain", guid: { stringValue: PLAIN_ID } },
      ],
    },
  };
}

function impactRows() {
  return {
    pages: [
      {
        id: "7",
        name: "Home",
        path: "//Sites/Demo/Home",
        site: "Demo",
        status: "Live",
        type: "Page",
      },
    ],
    templates: [{ name: "Base", site: "Demo" }],
  };
}

async function mockEditorChrome(page, { onSiteImpact } = {}) {
  await page.route("**/rest/editor/items/**/checkout", (route) =>
    route.fulfill({ status: 200, contentType: "application/json", body: "{}" }),
  );
  await page.route("**/services/itemmanagement/item/fields/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(fieldsBody()),
    }),
  );
  await page.route("**/services/contenttypes/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(typeBody()),
    }),
  );
  await page.route("**/itemmanagement/item/siteimpact/page/**", (route) => {
    if (onSiteImpact) {
      return onSiteImpact(route);
    }
    return route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ pages: [], templates: [] }),
    });
  });
  await page.route("**/pathmanagement/path/item/id/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        PathItem: { path: "//Sites/Demo/Home", name: "Home" },
      }),
    }),
  );
  await page.route("**/itemmanagement/workflow/getTransitions/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ stateName: "Draft", transitionTriggers: [] }),
    }),
  );
  await page.route("**/itemmanagement/workflow/allowedWorkflows/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ itemId: 42, currentWorkflowId: "1", choices: [] }),
    }),
  );
  await page.route("**/assembly/slot-relationships/canvas**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        SlotCanvas: { ownerId: 42, templateId: null, slots: [] },
      }),
    }),
  );
  await page.route("**/content-explorer/relationships/**/local", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ count: 0, links: [] }),
    }),
  );
  await page.route("**/content-explorer/translations/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        itemId: 42,
        locale: "en-us",
        variants: [{ contentId: 42, locale: "en-us", role: "source" }],
      }),
    }),
  );
}

function watchPage(page, errors, badResponses) {
  page.on("pageerror", (err) => errors.push(String(err)));
  page.on("console", (msg) => {
    if (msg.type() === "error") {
      // Intentional route-stubbed 500s from the siteimpact failure path
      // surface as resource-load noise; the named alert + retry behavior is
      // asserted on the page. Anything else is a real failure.
      if (/Failed to load resource:.*500/.test(msg.text())) {
        return;
      }
      errors.push(msg.text());
    }
  });
  page.on("response", (res) => {
    if (res.status() >= 500 && !/siteimpact/.test(res.url())) {
      badResponses.push(`${res.status()} ${res.url()}`);
    }
  });
}

function expectClean(errors, badResponses) {
  expect(errors, `console/page errors: ${errors.join(" | ")}`).toEqual([]);
  expect(badResponses, `bad responses: ${badResponses.join(" | ")}`).toEqual(
    [],
  );
}

test.describe("EditorHost site-impact load failure with retry (#6771)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(60_000);
    await loginAsAdmin(page);
  });

  test(
    "500 shows the named error with retry; retry loads the rows and moves focus to the heading",
    { tag: ["@page-metadata-editor", "@editor"] },
    async ({ page }) => {
      const errors = [];
      const badResponses = [];
      watchPage(page, errors, badResponses);
      let calls = 0;
      await mockEditorChrome(page, {
        onSiteImpact: async (route) => {
          calls += 1;
          if (calls === 1) {
            return route.fulfill({
              status: 500,
              contentType: "application/json",
              body: "{}",
            });
          }
          // Hold the retry load briefly so the live-region loading
          // indicator is observable before the rows render (#6786).
          await new Promise((resolve) => setTimeout(resolve, 500));
          return route.fulfill({
            status: 200,
            contentType: "application/json",
            body: JSON.stringify(impactRows()),
          });
        },
      });

      await page.goto(editorSpaUrl(BASE_URL, `contentId=${PAGE_ID}&mode=edit`));
      await expect(page.locator(`[data-testid="${TEST_IDS.host}"]`)).toBeVisible({
        timeout: 20_000,
      });
      await expect(
        page.locator('[data-testid="editor-site-impact-error"]'),
      ).toBeVisible({ timeout: 20_000 });
      await expect(
        page.locator('[data-testid="editor-site-impact-error"]'),
      ).toContainText(/could not load site impact/i);
      const retry = page.locator('[data-testid="editor-retry-site-impact"]');
      await expect(retry).toBeVisible();
      await retry.click();
      const loading = page.locator('[data-testid="editor-site-impact-loading"]');
      await expect(loading).toBeVisible({ timeout: 20_000 });
      await expect(loading).toHaveAttribute("role", "status");
      await expect(
        page.locator('[data-testid="editor-site-impact-page-row"]'),
      ).toContainText("Home", { timeout: 20_000 });
      await expect(
        page.locator('[data-testid="editor-site-impact-template-row"]'),
      ).toContainText("Base");
      await expect(
        page.locator('[data-testid="editor-site-impact-error"]'),
      ).toHaveCount(0);
      // Retry unmounts the focused button while loading (#6786): focus must
      // land on the panel heading, never on <body>.
      await expect(
        page.locator('[data-testid="editor-site-impact-heading"]'),
      ).toBeFocused();
      expect(calls).toBeGreaterThanOrEqual(2);
      await page
        .locator('[data-testid="editor-site-impact-pages-table"]')
        .scrollIntoViewIfNeeded();
      await page
        .locator('[data-testid="editor-site-impact-panel"]')
        .screenshot({
          path: "test-results/live-proof-6786-site-impact-retry-focus.png",
        });
      expectClean(errors, badResponses);
    },
  );

  test(
    "a repeated failure keeps the alert and offers retry again",
    { tag: ["@page-metadata-editor", "@editor"] },
    async ({ page }) => {
      const errors = [];
      const badResponses = [];
      watchPage(page, errors, badResponses);
      await mockEditorChrome(page, {
        onSiteImpact: (route) =>
          route.fulfill({
            status: 500,
            contentType: "application/json",
            body: "{}",
          }),
      });

      await page.goto(editorSpaUrl(BASE_URL, `contentId=${PAGE_ID}&mode=edit`));
      await expect(page.locator(`[data-testid="${TEST_IDS.host}"]`)).toBeVisible({
        timeout: 20_000,
      });
      await expect(
        page.locator('[data-testid="editor-site-impact-error"]'),
      ).toBeVisible({ timeout: 20_000 });
      await page.locator('[data-testid="editor-retry-site-impact"]').click();
      await expect(
        page.locator('[data-testid="editor-site-impact-error"]'),
      ).toBeVisible({ timeout: 20_000 });
      await expect(
        page.locator('[data-testid="editor-retry-site-impact"]'),
      ).toBeVisible();
      // The retried failure unmounts the clicked button while loading
      // (#6786): focus must return to the new Retry button, never <body>.
      await expect(
        page.locator('[data-testid="editor-retry-site-impact"]'),
      ).toBeFocused();
      await expect(
        page.locator('[data-testid="editor-site-impact-pages-table"]'),
      ).toHaveCount(0);
      expectClean(errors, badResponses);
    },
  );
});
