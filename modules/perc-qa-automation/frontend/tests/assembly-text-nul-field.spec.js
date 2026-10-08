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
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Assembly host — a single-line text field that contains a NUL is not saved.
 *
 * <p>Tags: {@code @assembly-text-nul} {@code @explorer-active-assembly}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/assembly-text-nul-field.spec.js}
 * from {@code modules/perc-qa-automation/frontend}.</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");

const OLD_TEXT = "Welcome";
const NEW_TEXT = "Updated welcome";
const NUL_TEXT = "bad\u0000value";
const LONG_NOTE = "A long note";
const BODY_HTML = "<p>About the site</p>";
const PAGE_LINK = "//Sites/Example/index";

function assemblySpaUrl(baseUrl, query = "") {
  const root = String(baseUrl || "").replace(/\/$/, "");
  const q = query.startsWith("?") ? query : query ? `?${query}` : "";
  const params = new URLSearchParams(q.startsWith("?") ? q.slice(1) : q);
  params.set("entry", "assembly");
  return `${root}/Rhythmyx/cm/app/spa.jsp?${params.toString()}`;
}

function fieldsBody(title) {
  return {
    contentId: "42",
    contentType: "percPage",
    name: "Home",
    checkoutUser: "Admin",
    fields: [
      { name: "displaytitle", value: title },
      { name: "notes", value: LONG_NOTE },
      { name: "description", value: BODY_HTML },
      { name: "pagelink", value: PAGE_LINK },
    ],
  };
}

function previewHtml(title) {
  return `<!DOCTYPE html><html><body>
    <h1 data-perc-field="displaytitle">${title}</h1>
    <p data-perc-field="notes">${LONG_NOTE}</p>
    <div class="PsAaField" id='[3,42,7,0,0,0,0,1,0,0,0,"description",42,"Body",0]'>${BODY_HTML}</div>
    <a data-perc-field="pagelink" href="${PAGE_LINK}">Example</a>
  </body></html>`;
}

/**
 * @param {import("@playwright/test").Page} page
 * @param {{ putStatus?: number, title?: { current: string } }} options
 */
async function installAssemblyRoutes(page, options) {
  const titleState = options.title || { current: OLD_TEXT };
  const puts = [];
  await page.route("**/services/actions/find/templates/**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        ActionMenuList: [
          {
            name: "rffPgGeneric",
            label: "Generic Page",
            url: "../assembler/render?sys_template=7",
            sortRank: 0,
            type: "MENUITEM",
          },
        ],
      }),
    });
  });
  await page.route("**/services/assembly/preview-location**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        previewUrl: "/assembler/render?sys_contentid=42&sys_template=7",
        contentId: 42,
        templateId: 7,
        revision: 1,
      }),
    });
  });
  await page.route("**/services/assembly/slot-relationships/canvas**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ ownerId: 42, templateId: 7, slots: [] }),
    });
  });
  await page.route("**/itemmanagement/workflow/checkOut/**", async (route) => {
    await route.fulfill({ status: 200, contentType: "application/json", body: "{}" });
  });
  await page.route("**/rest/editor/items/**/checkout", async (route) => {
    await route.fulfill({ status: 200, contentType: "application/json", body: "{}" });
  });
  await page.route("**/itemmanagement/item/fields/**", async (route) => {
    if (route.request().method() === "PUT") {
      puts.push(route.request().postData() || "");
      if (options.putStatus) {
        await route.fulfill({
          status: options.putStatus,
          contentType: "application/json",
          body: JSON.stringify({ message: "rejected" }),
        });
        return;
      }
      try {
        const parsed = JSON.parse(route.request().postData() || "{}");
        const rows = parsed.fields || parsed.Fields || [];
        const title = rows.find((row) => (row.name || row.Name) === "displaytitle");
        if (title) {
          titleState.current = String(title.value ?? title.Value ?? titleState.current);
        }
      } catch {
        // Keep the previous text when the body cannot be read.
      }
    }
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(fieldsBody(titleState.current)),
    });
  });
  await page.route("**/services/contenttypes/**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        name: "percPage",
        fields: [
          { name: "displaytitle", label: "Display title", control: "sys_EditBox" },
          { name: "notes", label: "Notes", control: "sys_TextArea" },
          { name: "description", label: "Body", control: "sys_tinymce" },
          { name: "pagelink", label: "Page link", control: "sys_PageLink" },
        ],
      }),
    });
  });
  await page.route("**/assembler/render**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "text/html",
      body: previewHtml(titleState.current),
    });
  });
  return puts;
}

function watchNoise(page) {
  const blocked = [];
  const consoleErrors = [];
  page.on("pageerror", (err) => consoleErrors.push(String(err)));
  page.on("console", (msg) => {
    if (msg.type() === "error") {
      consoleErrors.push(msg.text());
    }
  });
  page.on("request", (req) => {
    const u = req.url();
    if (
      u.includes("sys_cxItemAssembly") ||
      u.includes("itemassembly.html") ||
      u.includes("variantlistwithslots.html") ||
      u.includes("checkoutedit.xml") ||
      u.includes("contenteditorurls.html") ||
      u.includes("sys_ceSupport") ||
      u.includes("sys_cxSupport/")
    ) {
      blocked.push(u);
    }
  });
  return { blocked, consoleErrors };
}

function assertQuiet(blocked, consoleErrors) {
  expect(blocked, `leftover AA/CE HTML must not be requested: ${blocked.join(" ")}`).toEqual([]);
  const serious = consoleErrors.filter(
    (t) =>
      !/favicon|ResizeObserver|net::ERR|404 \(Not Found\)|status of 400|status of 403|status of 409/i.test(
        t,
      ),
  );
  expect(serious, `JS console errors: ${serious.join(" | ")}`).toEqual([]);
}

function fieldValue(rows, name) {
  const row = rows.find((item) => (item.name || item.Name) === name);
  return row ? String(row.value ?? row.Value ?? "") : undefined;
}

test.describe("assembly host single-line text NUL", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test(
    "does not save a single-line text value that contains a NUL and reloads the previous text",
    { tag: ["@assembly-text-nul", "@explorer-active-assembly", "@explorer"] },
    async ({ page }) => {
      const { blocked, consoleErrors } = watchNoise(page);
      const title = { current: OLD_TEXT };
      const puts = await installAssemblyRoutes(page, { title });
      await page.goto(assemblySpaUrl(BASE_URL, "contentId=42&templateId=7"));
      const frame = page.frameLocator('[data-testid="assembly-preview-frame"]');
      const titleField = frame.locator('[data-testid="assembly-inline-field-displaytitle"]');
      await expect(titleField).toBeVisible({ timeout: 20_000 });
      await expect(titleField).toHaveText(OLD_TEXT);
      await expect(titleField).toHaveAttribute("data-assembly-value", "text");
      await titleField.evaluate((el, next) => {
        el.textContent = next;
      }, NUL_TEXT);
      expect(puts, "editing without Save must not write").toEqual([]);
      await page.locator('[data-testid="assembly-field-save"]').click();
      await expect(page.locator('[data-testid="assembly-field-error-displaytitle"]')).toContainText(
        /character that cannot be saved/i,
        { timeout: 10_000 },
      );
      await expect(page.locator('[data-testid="assembly-field-notice"]')).toContainText(
        /character that cannot be saved/i,
      );
      await expect(page.locator('[data-testid="assembly-field-notice"]')).not.toContainText(
        /fields saved/i,
      );
      expect(puts, "a single-line NUL must not be written").toEqual([]);
      await expect(titleField).not.toHaveAttribute("aria-invalid", "true");

      await page.goto(assemblySpaUrl(BASE_URL, "contentId=42&templateId=7"));
      const reloaded = page.frameLocator('[data-testid="assembly-preview-frame"]');
      const reloadedTitle = reloaded.locator('[data-testid="assembly-inline-field-displaytitle"]');
      await expect(reloadedTitle).toBeVisible({ timeout: 15_000 });
      await expect(reloadedTitle).toHaveText(OLD_TEXT);
      await expect(reloaded.locator('[data-testid="assembly-inline-field-notes"]')).toHaveText(
        LONG_NOTE,
      );
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="assembly-host"]',
      });
      assertQuiet(blocked, consoleErrors);
    },
  );

  test(
    "Cancel does not write a single-line NUL edit",
    { tag: ["@assembly-text-nul", "@explorer"] },
    async ({ page }) => {
      const { blocked, consoleErrors } = watchNoise(page);
      const puts = await installAssemblyRoutes(page, {});
      await page.goto(assemblySpaUrl(BASE_URL, "contentId=42&templateId=7"));
      const frame = page.frameLocator('[data-testid="assembly-preview-frame"]');
      const titleField = frame.locator('[data-testid="assembly-inline-field-displaytitle"]');
      await expect(titleField).toBeVisible({ timeout: 20_000 });
      await titleField.evaluate((el, next) => {
        el.textContent = next;
      }, NUL_TEXT);
      await page.locator('[data-testid="assembly-field-cancel"]').click();
      expect(puts, "Cancel must not write the NUL edit").toEqual([]);
      await expect(page.locator('[data-testid="assembly-field-notice"]')).toHaveCount(0);
      await expect(titleField).toHaveText(OLD_TEXT);
      await page.goto(assemblySpaUrl(BASE_URL, "contentId=42&templateId=7"));
      const reloaded = page.frameLocator('[data-testid="assembly-preview-frame"]');
      await expect(reloaded.locator('[data-testid="assembly-inline-field-displaytitle"]')).toHaveText(
        OLD_TEXT,
        { timeout: 15_000 },
      );
      assertQuiet(blocked, consoleErrors);
    },
  );

  test(
    "still saves an ordinary single-line value",
    { tag: ["@assembly-text-nul", "@explorer"] },
    async ({ page }) => {
      const { blocked, consoleErrors } = watchNoise(page);
      const title = { current: OLD_TEXT };
      const puts = await installAssemblyRoutes(page, { title });
      await page.goto(assemblySpaUrl(BASE_URL, "contentId=42&templateId=7"));
      const frame = page.frameLocator('[data-testid="assembly-preview-frame"]');
      const titleField = frame.locator('[data-testid="assembly-inline-field-displaytitle"]');
      await expect(titleField).toBeVisible({ timeout: 20_000 });
      await titleField.fill(NEW_TEXT);
      await page.locator('[data-testid="assembly-field-save"]').click();
      await expect(page.locator('[data-testid="assembly-field-notice"]')).toContainText(/saved/i, {
        timeout: 10_000,
      });
      expect(puts.length).toBeGreaterThan(0);
      const saved = JSON.parse(puts[puts.length - 1]);
      const rows = saved.fields || saved.Fields || [];
      expect(fieldValue(rows, "displaytitle")).toBe(NEW_TEXT);
      expect(fieldValue(rows, "notes")).toBe(LONG_NOTE);
      expect(fieldValue(rows, "displaytitle")).not.toContain("\u0000");
      await page.goto(assemblySpaUrl(BASE_URL, "contentId=42&templateId=7"));
      const reloaded = page.frameLocator('[data-testid="assembly-preview-frame"]');
      await expect(reloaded.locator('[data-testid="assembly-inline-field-displaytitle"]')).toHaveText(
        NEW_TEXT,
        { timeout: 15_000 },
      );
      assertQuiet(blocked, consoleErrors);
    },
  );

  test(
    "HTTP 400 on a single-line text save does not claim success",
    { tag: ["@assembly-text-nul", "@explorer"] },
    async ({ page }) => {
      const { blocked, consoleErrors } = watchNoise(page);
      const puts = await installAssemblyRoutes(page, { putStatus: 400 });
      await page.goto(assemblySpaUrl(BASE_URL, "contentId=42&templateId=7"));
      const frame = page.frameLocator('[data-testid="assembly-preview-frame"]');
      const titleField = frame.locator('[data-testid="assembly-inline-field-displaytitle"]');
      await expect(titleField).toBeVisible({ timeout: 20_000 });
      await titleField.fill(NEW_TEXT);
      await page.locator('[data-testid="assembly-field-save"]').click();
      await expect(page.locator('[data-testid="assembly-field-notice"]')).toContainText(
        /could not save/i,
        { timeout: 10_000 },
      );
      await expect(titleField).toHaveText(OLD_TEXT);
      expect(puts.length).toBeGreaterThan(0);
      await expect(page.locator('[data-testid="assembly-field-notice"]')).not.toContainText(
        /fields saved/i,
      );
      assertQuiet(blocked, consoleErrors);
    },
  );
});
