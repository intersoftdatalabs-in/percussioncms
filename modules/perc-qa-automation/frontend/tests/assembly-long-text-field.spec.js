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
 * Assembly host — save one long-text field on the assembled page.
 *
 * <p>Tags: {@code @assembly-long-text-field} {@code @explorer-active-assembly}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/assembly-long-text-field.spec.js}
 * from {@code modules/perc-qa-automation/frontend}.</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");

const OLD_TEXT = "Welcome";
const LONG_NOTE = "A long note";
const NEW_NOTE = "Line one\nLine two";
const BODY_HTML = "<p>About the site</p>";
const PAGE_LINK = "//Sites/Example/index";

function assemblySpaUrl(baseUrl, query = "") {
  const root = String(baseUrl || "").replace(/\/$/, "");
  const q = query.startsWith("?") ? query : query ? `?${query}` : "";
  const params = new URLSearchParams(q.startsWith("?") ? q.slice(1) : q);
  params.set("entry", "assembly");
  return `${root}/Rhythmyx/cm/app/spa.jsp?${params.toString()}`;
}

function escapeHtml(value) {
  return String(value)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;");
}

function fieldsBody(notes) {
  return {
    contentId: "42",
    contentType: "percPage",
    name: "Home",
    checkoutUser: "Admin",
    fields: [
      { name: "displaytitle", value: OLD_TEXT },
      { name: "notes", value: notes },
      { name: "description", value: BODY_HTML },
      { name: "pagelink", value: PAGE_LINK },
    ],
  };
}

function previewHtml(notes) {
  return `<!DOCTYPE html><html><body>
    <h1 data-perc-field="displaytitle">${escapeHtml(OLD_TEXT)}</h1>
    <p data-perc-field="notes">${escapeHtml(notes)}</p>
    <div class="PsAaField" id='[3,42,7,0,0,0,0,1,0,0,0,"description",42,"Body",0]'>${BODY_HTML}</div>
    <a data-perc-field="pagelink" href="${PAGE_LINK}">Example</a>
  </body></html>`;
}

/**
 * @param {import("@playwright/test").Page} page
 * @param {{ putStatus?: number, readOnlyNotes?: boolean, notes?: { current: string } }} options
 */
async function installAssemblyRoutes(page, options) {
  const notesState = options.notes || { current: LONG_NOTE };
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
        const notes = rows.find((row) => (row.name || row.Name) === "notes");
        if (notes) {
          notesState.current = String(notes.value ?? notes.Value ?? notesState.current);
        }
      } catch {
        // Keep the previous text when the body cannot be read.
      }
    }
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(fieldsBody(notesState.current)),
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
          {
            name: "notes",
            label: "Notes",
            control: "sys_TextArea",
            readOnly: options.readOnlyNotes === true,
          },
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
      body: previewHtml(notesState.current),
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
 * Text of an in-place long-text node. `innerText` keeps breaks from `<br>`
 * and from `white-space: pre-wrap`.
 * @param {import("@playwright/test").Locator} locator
 */
async function longTextOf(locator) {
  return locator.evaluate((el) => {
    const raw =
      el instanceof HTMLTextAreaElement || el instanceof HTMLInputElement
        ? el.value
        : el.innerText || el.textContent || "";
    return raw.replace(/\r\n/g, "\n").replace(/\r/g, "\n").trim();
  });
}

test.describe("assembly host long-text field", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test(
    "saves one assembled long-text field and shows it again after reload",
    { tag: ["@assembly-long-text-field", "@explorer-active-assembly", "@explorer"] },
    async ({ page }) => {
      const { blocked, consoleErrors } = watchNoise(page);
      const notes = { current: LONG_NOTE };
      const puts = await installAssemblyRoutes(page, { notes });
      await page.goto(assemblySpaUrl(BASE_URL, "contentId=42&templateId=7"));
      await expect(page.locator('[data-testid="assembly-host"]')).toBeVisible({
        timeout: 20_000,
      });
      const frame = page.frameLocator('[data-testid="assembly-preview-frame"]');
      const notesField = frame.locator('[data-testid="assembly-inline-field-notes"]');
      await expect(notesField).toBeVisible({ timeout: 15_000 });
      await expect.poll(async () => longTextOf(notesField)).toBe(LONG_NOTE);
      await expect(frame.locator('[data-testid="assembly-inline-field-displaytitle"]')).toBeVisible();
      await expect(frame.locator('[data-testid="assembly-inline-field-description"]')).toBeVisible();
      await expect(frame.locator('[data-testid="assembly-inline-field-pagelink"]')).toBeVisible();

      await notesField.fill(NEW_NOTE);
      expect(puts, "editing without Save must not write").toEqual([]);
      await page.locator('[data-testid="assembly-field-save"]').click();
      await expect(page.locator('[data-testid="assembly-field-notice"]')).toContainText(/saved/i, {
        timeout: 10_000,
      });
      expect(puts.length).toBeGreaterThan(0);
      const saved = JSON.parse(puts[puts.length - 1]);
      const rows = saved.fields || saved.Fields || [];
      expect(fieldValue(rows, "notes")).toBe(NEW_NOTE);
      expect(fieldValue(rows, "displaytitle")).toBe(OLD_TEXT);
      expect(fieldValue(rows, "description")).toBe(BODY_HTML);
      expect(fieldValue(rows, "pagelink")).toBe(PAGE_LINK);
      const notesRow = rows.find((row) => (row.name || row.Name) === "notes");
      expect(notesRow.dataType || notesRow.DataType).toBeUndefined();
      await expect.poll(async () => longTextOf(notesField)).toBe(NEW_NOTE);

      await page.goto(assemblySpaUrl(BASE_URL, "contentId=42&templateId=7"));
      await expect(page.locator('[data-testid="assembly-host"]')).toBeVisible({
        timeout: 20_000,
      });
      const reloaded = page.frameLocator('[data-testid="assembly-preview-frame"]');
      const reloadedNotes = reloaded.locator('[data-testid="assembly-inline-field-notes"]');
      await expect(reloadedNotes).toBeVisible({ timeout: 15_000 });
      await expect.poll(async () => longTextOf(reloadedNotes)).toBe(NEW_NOTE);
      await expect(reloaded.locator('[data-testid="assembly-inline-field-displaytitle"]')).toHaveText(
        OLD_TEXT,
      );

      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="assembly-host"]',
      });
      assertQuiet(blocked, consoleErrors);
    },
  );

  test(
    "does not write a read-only long-text field",
    { tag: ["@assembly-long-text-field", "@explorer"] },
    async ({ page }) => {
      const { blocked, consoleErrors } = watchNoise(page);
      const puts = await installAssemblyRoutes(page, { readOnlyNotes: true });
      await page.goto(assemblySpaUrl(BASE_URL, "contentId=42&templateId=7"));
      await expect(page.locator('[data-testid="assembly-field-chip-displaytitle"]')).toBeVisible({
        timeout: 20_000,
      });
      await expect(page.locator('[data-testid="assembly-field-chip-notes"]')).toHaveCount(0);
      const frame = page.frameLocator('[data-testid="assembly-preview-frame"]');
      await expect(frame.locator('[data-testid="assembly-inline-field-displaytitle"]')).toBeVisible({
        timeout: 15_000,
      });
      await expect(frame.locator('[data-testid="assembly-inline-field-notes"]')).toHaveCount(0);
      await frame.locator('[data-testid="assembly-inline-field-displaytitle"]').fill("Updated welcome");
      await page.locator('[data-testid="assembly-field-save"]').click();
      await expect(page.locator('[data-testid="assembly-field-notice"]')).toContainText(/saved/i, {
        timeout: 10_000,
      });
      const saved = JSON.parse(puts[puts.length - 1]);
      const rows = saved.fields || saved.Fields || [];
      expect(fieldValue(rows, "notes")).toBe(LONG_NOTE);
      expect(fieldValue(rows, "displaytitle")).toBe("Updated welcome");
      expect(fieldValue(rows, "description")).toBe(BODY_HTML);
      expect(fieldValue(rows, "pagelink")).toBe(PAGE_LINK);
      assertQuiet(blocked, consoleErrors);
    },
  );

  for (const status of [400, 403, 409]) {
    test(
      `HTTP ${status} leaves the previous long text in place`,
      { tag: ["@assembly-long-text-field", "@explorer"] },
      async ({ page }) => {
        const { blocked, consoleErrors } = watchNoise(page);
        const puts = await installAssemblyRoutes(page, { putStatus: status });
        await page.goto(assemblySpaUrl(BASE_URL, "contentId=42&templateId=7"));
        const frame = page.frameLocator('[data-testid="assembly-preview-frame"]');
        const notesField = frame.locator('[data-testid="assembly-inline-field-notes"]');
        await expect(notesField).toBeVisible({ timeout: 20_000 });
        await notesField.fill(NEW_NOTE);
        await page.locator('[data-testid="assembly-field-save"]').click();
        await expect(page.locator('[data-testid="assembly-field-notice"]')).toContainText(
          /could not save/i,
          { timeout: 10_000 },
        );
        await expect.poll(async () => longTextOf(notesField)).toBe(LONG_NOTE);
        expect(puts.length).toBeGreaterThan(0);
        await expect(page.locator('[data-testid="assembly-field-notice"]')).not.toContainText(
          /fields saved/i,
        );
        assertQuiet(blocked, consoleErrors);
      },
    );
  }
});
