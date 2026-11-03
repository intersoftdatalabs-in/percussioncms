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
 * Assembly host — a keyword field that contains a NUL is not saved.
 * An ordinary catalog choice still saves. Cancel does not write.
 * HTTP 400 is not success. The required-blank keyword gate stays unchanged.
 *
 * <p>Tags: {@code @assembly-keyword-nul} {@code @explorer-active-assembly}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/assembly-keyword-nul-field.spec.js}
 * from {@code modules/perc-qa-automation/frontend}.</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");

const OLD_CHOICE = "news";
const NEW_CHOICE = "events";
const OLD_TEXT = "Welcome";
const PREV_NOTE = "A long note";
const OLD_HTML = "<p>About the site</p>";

function assemblySpaUrl(baseUrl, query = "") {
  const root = String(baseUrl || "").replace(/\/$/, "");
  const q = query.startsWith("?") ? query : query ? `?${query}` : "";
  const params = new URLSearchParams(q.startsWith("?") ? q.slice(1) : q);
  params.set("entry", "assembly");
  return `${root}/Rhythmyx/cm/app/spa.jsp?${params.toString()}`;
}

function fieldsBody(keyword) {
  return {
    contentId: "42",
    contentType: "percPage",
    name: "Home",
    checkoutUser: "Admin",
    fields: [
      { name: "displaytitle", value: OLD_TEXT },
      { name: "notes", value: PREV_NOTE },
      { name: "description", value: OLD_HTML },
      { name: "keywords", value: keyword },
    ],
  };
}

function previewHtml(keyword) {
  return `<!DOCTYPE html><html><body>
    <h1 data-perc-field="displaytitle">${OLD_TEXT}</h1>
    <p data-perc-field="notes">${PREV_NOTE}</p>
    <div class="PsAaField" id='[3,42,7,0,0,0,0,1,0,0,0,"description",42,"Body",0]'>${OLD_HTML}</div>
    <span data-perc-field="keywords">${keyword}</span>
  </body></html>`;
}

/**
 * @param {import("@playwright/test").Page} page
 * @param {{ putStatus?: number, keyword?: { current: string } }} options
 */
async function installAssemblyRoutes(page, options) {
  const keywordState = options.keyword || { current: OLD_CHOICE };
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
  await page.route("**/services/keywords**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        Keyword: [
          {
            value: "keywords",
            label: "Keywords",
            choices: [
              { value: "news", label: "News" },
              { value: "events", label: "Events" },
            ],
          },
        ],
      }),
    });
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
        const keyword = rows.find((row) => (row.name || row.Name) === "keywords");
        if (keyword) {
          keywordState.current = String(keyword.value ?? keyword.Value ?? keywordState.current);
        }
      } catch {
        // Keep the previous choice when the body cannot be read.
      }
    }
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(fieldsBody(keywordState.current)),
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
          {
            name: "keywords",
            label: "Keywords",
            control: "sys_DropDownSingle",
            required: true,
          },
        ],
      }),
    });
  });
  await page.route("**/assembler/render**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "text/html",
      body: previewHtml(keywordState.current),
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

/**
 * @param {import("@playwright/test").Locator} select
 */
async function injectKeywordNul(select) {
  await select.evaluate((el) => {
    const opt = el.ownerDocument.createElement("option");
    opt.value = "news\u0000";
    opt.textContent = "NUL";
    el.appendChild(opt);
    el.value = "news\u0000";
  });
}

test.describe("assembly host keyword NUL", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test(
    "does not save a keyword NUL and reloads the previous choice",
    { tag: ["@assembly-keyword-nul", "@explorer-active-assembly", "@explorer"] },
    async ({ page }) => {
      const { blocked, consoleErrors } = watchNoise(page);
      const keyword = { current: OLD_CHOICE };
      const puts = await installAssemblyRoutes(page, { keyword });
      await page.goto(assemblySpaUrl(BASE_URL, "contentId=42&templateId=7"));
      const frame = page.frameLocator('[data-testid="assembly-preview-frame"]');
      const select = frame.locator('[data-testid="assembly-inline-field-keywords"]');
      await expect(select).toBeVisible({ timeout: 20_000 });
      await expect(select).toHaveAttribute("data-assembly-value", "keyword");
      await expect(select).toHaveValue(OLD_CHOICE);
      await injectKeywordNul(select);
      expect(puts, "editing without Save must not write").toEqual([]);
      await page.locator('[data-testid="assembly-field-save"]').click();
      await expect(page.locator('[data-testid="assembly-field-error-keywords"]')).toContainText(
        /keyword contains a character that cannot be saved/i,
        { timeout: 10_000 },
      );
      await expect(page.locator('[data-testid="assembly-field-notice"]')).toContainText(
        /keyword contains a character that cannot be saved/i,
      );
      await expect(page.locator('[data-testid="assembly-field-notice"]')).not.toContainText(
        /fields saved/i,
      );
      await expect(page.locator('[data-testid="assembly-field-notice"]')).not.toContainText(
        /required/i,
      );
      expect(puts, "a keyword NUL must not be written").toEqual([]);

      await page.goto(assemblySpaUrl(BASE_URL, "contentId=42&templateId=7"));
      const reloaded = page.frameLocator('[data-testid="assembly-preview-frame"]');
      await expect(reloaded.locator('[data-testid="assembly-inline-field-keywords"]')).toHaveValue(
        OLD_CHOICE,
        { timeout: 15_000 },
      );
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="assembly-host"]',
      });
      assertQuiet(blocked, consoleErrors);
    },
  );

  test(
    "Cancel does not write a keyword NUL edit",
    { tag: ["@assembly-keyword-nul", "@explorer"] },
    async ({ page }) => {
      const { blocked, consoleErrors } = watchNoise(page);
      const puts = await installAssemblyRoutes(page, {});
      await page.goto(assemblySpaUrl(BASE_URL, "contentId=42&templateId=7"));
      const frame = page.frameLocator('[data-testid="assembly-preview-frame"]');
      const select = frame.locator('[data-testid="assembly-inline-field-keywords"]');
      await expect(select).toBeVisible({ timeout: 20_000 });
      await injectKeywordNul(select);
      await page.locator('[data-testid="assembly-field-cancel"]').click();
      expect(puts, "Cancel must not write the NUL edit").toEqual([]);
      await expect(page.locator('[data-testid="assembly-field-notice"]')).toHaveCount(0);
      await expect(select).toHaveValue(OLD_CHOICE);
      await page.goto(assemblySpaUrl(BASE_URL, "contentId=42&templateId=7"));
      const reloaded = page.frameLocator('[data-testid="assembly-preview-frame"]');
      await expect(reloaded.locator('[data-testid="assembly-inline-field-keywords"]')).toHaveValue(
        OLD_CHOICE,
        { timeout: 15_000 },
      );
      assertQuiet(blocked, consoleErrors);
    },
  );

  test(
    "still saves a listed catalog choice",
    { tag: ["@assembly-keyword-nul", "@explorer"] },
    async ({ page }) => {
      const { blocked, consoleErrors } = watchNoise(page);
      const keyword = { current: OLD_CHOICE };
      const puts = await installAssemblyRoutes(page, { keyword });
      await page.goto(assemblySpaUrl(BASE_URL, "contentId=42&templateId=7"));
      const frame = page.frameLocator('[data-testid="assembly-preview-frame"]');
      const select = frame.locator('[data-testid="assembly-inline-field-keywords"]');
      await expect(select).toBeVisible({ timeout: 20_000 });
      await select.selectOption(NEW_CHOICE);
      await page.locator('[data-testid="assembly-field-save"]').click();
      await expect(page.locator('[data-testid="assembly-field-notice"]')).toContainText(/saved/i, {
        timeout: 10_000,
      });
      expect(puts.length).toBeGreaterThan(0);
      const saved = JSON.parse(puts[puts.length - 1]);
      const rows = saved.fields || saved.Fields || [];
      expect(fieldValue(rows, "keywords")).toBe(NEW_CHOICE);
      expect(fieldValue(rows, "keywords")).not.toContain("\u0000");
      expect(fieldValue(rows, "displaytitle")).toBe(OLD_TEXT);
      await page.goto(assemblySpaUrl(BASE_URL, "contentId=42&templateId=7"));
      const reloaded = page.frameLocator('[data-testid="assembly-preview-frame"]');
      await expect(reloaded.locator('[data-testid="assembly-inline-field-keywords"]')).toHaveValue(
        NEW_CHOICE,
        { timeout: 15_000 },
      );
      assertQuiet(blocked, consoleErrors);
    },
  );

  test(
    "HTTP 400 on a catalog keyword does not claim success",
    { tag: ["@assembly-keyword-nul", "@explorer"] },
    async ({ page }) => {
      const { blocked, consoleErrors } = watchNoise(page);
      const puts = await installAssemblyRoutes(page, { putStatus: 400 });
      await page.goto(assemblySpaUrl(BASE_URL, "contentId=42&templateId=7"));
      const frame = page.frameLocator('[data-testid="assembly-preview-frame"]');
      const select = frame.locator('[data-testid="assembly-inline-field-keywords"]');
      await expect(select).toBeVisible({ timeout: 20_000 });
      await select.selectOption(NEW_CHOICE);
      await page.locator('[data-testid="assembly-field-save"]').click();
      await expect(page.locator('[data-testid="assembly-field-notice"]')).toContainText(
        /could not save/i,
        { timeout: 10_000 },
      );
      await expect(select).toHaveValue(OLD_CHOICE);
      expect(puts.length).toBeGreaterThan(0);
      await expect(page.locator('[data-testid="assembly-field-notice"]')).not.toContainText(
        /fields saved/i,
      );
      assertQuiet(blocked, consoleErrors);
    },
  );
});
