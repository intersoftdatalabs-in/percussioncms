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
 * Assembly host — a link field that contains a NUL is not saved.
 * A content id, a hyphenated content GUID, and a folder path still save.
 * The HTML NUL gate stays unchanged. HTTP 400 is not success.
 *
 * <p>Tags: {@code @assembly-link-nul} {@code @explorer-active-assembly}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/assembly-link-nul-field.spec.js}
 * from {@code modules/perc-qa-automation/frontend}.</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");

const OLD_LINK = "//Sites/Example/index";
const OLD_HTML = "<p>About the site</p>";
const OLD_TEXT = "Welcome";
const PREV_NOTE = "A long note";
const CONTENT_ID = "594";
const CONTENT_GUID = "0-101-594";
const FOLDER_PATH = "/Sites/Example/index";

function assemblySpaUrl(baseUrl, query = "") {
  const root = String(baseUrl || "").replace(/\/$/, "");
  const q = query.startsWith("?") ? query : query ? `?${query}` : "";
  const params = new URLSearchParams(q.startsWith("?") ? q.slice(1) : q);
  params.set("entry", "assembly");
  return `${root}/Rhythmyx/cm/app/spa.jsp?${params.toString()}`;
}

function fieldsBody(link, html = OLD_HTML) {
  return {
    contentId: "42",
    contentType: "percPage",
    name: "Home",
    checkoutUser: "Admin",
    fields: [
      { name: "displaytitle", value: OLD_TEXT },
      { name: "notes", value: PREV_NOTE },
      { name: "description", value: html },
      { name: "pagelink", value: link },
    ],
  };
}

function previewHtml(link, html = OLD_HTML) {
  return `<!DOCTYPE html><html><body>
    <h1 data-perc-field="displaytitle">${OLD_TEXT}</h1>
    <p data-perc-field="notes">${PREV_NOTE}</p>
    <div class="PsAaField" id='[3,42,7,0,0,0,0,1,0,0,0,"description",42,"Body",0]'>${html}</div>
    <a data-perc-field="pagelink" href="${link}">Example</a>
  </body></html>`;
}

/**
 * @param {import("@playwright/test").Page} page
 * @param {{ putStatus?: number, link?: { current: string }, html?: { current: string } }} options
 */
async function installAssemblyRoutes(page, options) {
  const linkState = options.link || { current: OLD_LINK };
  const htmlState = options.html || { current: OLD_HTML };
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
        const link = rows.find((row) => (row.name || row.Name) === "pagelink");
        if (link) {
          linkState.current = String(link.value ?? link.Value ?? linkState.current);
        }
        const description = rows.find((row) => (row.name || row.Name) === "description");
        if (description) {
          htmlState.current = String(description.value ?? description.Value ?? htmlState.current);
        }
      } catch {
        // Keep the previous link when the body cannot be read.
      }
    }
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(fieldsBody(linkState.current, htmlState.current)),
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
      body: previewHtml(linkState.current, htmlState.current),
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

function fieldType(rows, name) {
  const row = rows.find((item) => (item.name || item.Name) === name);
  return row ? String(row.dataType || row.DataType || "") : "";
}

test.describe("assembly host link NUL", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test(
    "does not save a link that contains a NUL and reloads the previous link",
    { tag: ["@assembly-link-nul", "@explorer-active-assembly", "@explorer"] },
    async ({ page }) => {
      const { blocked, consoleErrors } = watchNoise(page);
      const link = { current: OLD_LINK };
      const puts = await installAssemblyRoutes(page, { link });
      await page.goto(assemblySpaUrl(BASE_URL, "contentId=42&templateId=7"));
      const frame = page.frameLocator('[data-testid="assembly-preview-frame"]');
      const linkInput = frame.locator('[data-testid="assembly-inline-field-pagelink"]');
      await expect(linkInput).toBeVisible({ timeout: 20_000 });
      await expect(linkInput).toHaveAttribute("data-assembly-value", "link");
      await expect(linkInput).toHaveValue(OLD_LINK);
      await linkInput.evaluate((el) => {
        el.value = "594\u0000";
      });
      expect(puts, "editing without Save must not write").toEqual([]);
      await page.locator('[data-testid="assembly-field-save"]').click();
      await expect(page.locator('[data-testid="assembly-field-error-pagelink"]')).toContainText(
        /link contains a character that cannot be saved/i,
        { timeout: 10_000 },
      );
      await expect(page.locator('[data-testid="assembly-field-notice"]')).toContainText(
        /link contains a character that cannot be saved/i,
      );
      await expect(page.locator('[data-testid="assembly-field-notice"]')).not.toContainText(
        /HTML contains a character/i,
      );
      await expect(page.locator('[data-testid="assembly-field-notice"]')).not.toContainText(
        /fields saved/i,
      );
      await expect(page.locator('[data-testid="assembly-field-error-description"]')).toHaveCount(0);
      await expect(linkInput).toHaveAttribute("aria-invalid", "true");
      expect(puts, "a link NUL must not be written").toEqual([]);

      await page.goto(assemblySpaUrl(BASE_URL, "contentId=42&templateId=7"));
      const reloaded = page.frameLocator('[data-testid="assembly-preview-frame"]');
      const reloadedLink = reloaded.locator('[data-testid="assembly-inline-field-pagelink"]');
      await expect(reloadedLink).toBeVisible({ timeout: 15_000 });
      await expect(reloadedLink).toHaveValue(OLD_LINK);
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="assembly-host"]',
      });
      assertQuiet(blocked, consoleErrors);
    },
  );

  test(
    "Cancel does not write a link NUL edit",
    { tag: ["@assembly-link-nul", "@explorer"] },
    async ({ page }) => {
      const { blocked, consoleErrors } = watchNoise(page);
      const puts = await installAssemblyRoutes(page, {});
      await page.goto(assemblySpaUrl(BASE_URL, "contentId=42&templateId=7"));
      const frame = page.frameLocator('[data-testid="assembly-preview-frame"]');
      const linkInput = frame.locator('[data-testid="assembly-inline-field-pagelink"]');
      await expect(linkInput).toBeVisible({ timeout: 20_000 });
      await linkInput.evaluate((el) => {
        el.value = `${el.value}\u0000`;
      });
      await page.locator('[data-testid="assembly-field-cancel"]').click();
      expect(puts, "Cancel must not write the NUL edit").toEqual([]);
      await expect(page.locator('[data-testid="assembly-field-notice"]')).toHaveCount(0);
      await expect(linkInput).toHaveValue(OLD_LINK);
      await page.goto(assemblySpaUrl(BASE_URL, "contentId=42&templateId=7"));
      const reloaded = page.frameLocator('[data-testid="assembly-preview-frame"]');
      await expect(reloaded.locator('[data-testid="assembly-inline-field-pagelink"]')).toHaveValue(
        OLD_LINK,
        { timeout: 15_000 },
      );
      assertQuiet(blocked, consoleErrors);
    },
  );

  test(
    "still saves a content id, a GUID, and a folder path",
    { tag: ["@assembly-link-nul", "@explorer"] },
    async ({ page }) => {
      const { blocked, consoleErrors } = watchNoise(page);
      const link = { current: OLD_LINK };
      const puts = await installAssemblyRoutes(page, { link });
      await page.goto(assemblySpaUrl(BASE_URL, "contentId=42&templateId=7"));

      for (const next of [CONTENT_ID, CONTENT_GUID, FOLDER_PATH]) {
        const frame = page.frameLocator('[data-testid="assembly-preview-frame"]');
        const linkInput = frame.locator('[data-testid="assembly-inline-field-pagelink"]');
        await expect(linkInput).toBeVisible({ timeout: 20_000 });
        const before = puts.length;
        await linkInput.fill(next);
        await page.locator('[data-testid="assembly-field-save"]').click();
        await expect(page.locator('[data-testid="assembly-field-notice"]')).toContainText(/saved/i, {
          timeout: 10_000,
        });
        expect(puts.length).toBeGreaterThan(before);
        const saved = JSON.parse(puts[puts.length - 1]);
        const rows = saved.fields || saved.Fields || [];
        expect(fieldValue(rows, "pagelink")).toBe(next);
        expect(fieldType(rows, "pagelink")).toBe("link");
        expect(fieldValue(rows, "pagelink")).not.toContain("\u0000");
        expect(fieldValue(rows, "description")).toBe(OLD_HTML);
        await page.goto(assemblySpaUrl(BASE_URL, "contentId=42&templateId=7"));
        const reloaded = page.frameLocator('[data-testid="assembly-preview-frame"]');
        await expect(
          reloaded.locator('[data-testid="assembly-inline-field-pagelink"]'),
        ).toHaveValue(next, { timeout: 15_000 });
      }
      assertQuiet(blocked, consoleErrors);
    },
  );

  test(
    "an HTML NUL still uses the HTML message when the link has no NUL",
    { tag: ["@assembly-link-nul", "@explorer"] },
    async ({ page }) => {
      const { blocked, consoleErrors } = watchNoise(page);
      const puts = await installAssemblyRoutes(page, {});
      await page.goto(assemblySpaUrl(BASE_URL, "contentId=42&templateId=7"));
      const frame = page.frameLocator('[data-testid="assembly-preview-frame"]');
      const body = frame.locator('[data-testid="assembly-inline-field-description"]');
      const linkInput = frame.locator('[data-testid="assembly-inline-field-pagelink"]');
      await expect(body).toBeVisible({ timeout: 20_000 });
      await expect(linkInput).toHaveValue(OLD_LINK);
      await body.evaluate((el) => {
        el.appendChild(el.ownerDocument.createTextNode("bad\u0000"));
      });
      await page.locator('[data-testid="assembly-field-save"]').click();
      await expect(page.locator('[data-testid="assembly-field-error-description"]')).toContainText(
        /HTML contains a character that cannot be saved/i,
        { timeout: 10_000 },
      );
      await expect(page.locator('[data-testid="assembly-field-error-pagelink"]')).toHaveCount(0);
      await expect(page.locator('[data-testid="assembly-field-notice"]')).toContainText(
        /HTML contains a character that cannot be saved/i,
      );
      await expect(page.locator('[data-testid="assembly-field-notice"]')).not.toContainText(
        /link contains a character/i,
      );
      expect(puts, "an HTML NUL must not be written").toEqual([]);
      assertQuiet(blocked, consoleErrors);
    },
  );

  test(
    "HTTP 400 on a link save does not claim success",
    { tag: ["@assembly-link-nul", "@explorer"] },
    async ({ page }) => {
      const { blocked, consoleErrors } = watchNoise(page);
      const puts = await installAssemblyRoutes(page, { putStatus: 400 });
      await page.goto(assemblySpaUrl(BASE_URL, "contentId=42&templateId=7"));
      const frame = page.frameLocator('[data-testid="assembly-preview-frame"]');
      const linkInput = frame.locator('[data-testid="assembly-inline-field-pagelink"]');
      await expect(linkInput).toBeVisible({ timeout: 20_000 });
      await linkInput.fill(CONTENT_ID);
      await page.locator('[data-testid="assembly-field-save"]').click();
      await expect(page.locator('[data-testid="assembly-field-notice"]')).toContainText(
        /could not save/i,
        { timeout: 10_000 },
      );
      await expect(linkInput).toHaveValue(OLD_LINK);
      expect(puts.length).toBeGreaterThan(0);
      await expect(page.locator('[data-testid="assembly-field-notice"]')).not.toContainText(
        /fields saved/i,
      );
      assertQuiet(blocked, consoleErrors);
    },
  );
});
