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
 * Assembly host — a required whole number cannot be saved blank.
 *
 * <p>Tags: {@code @assembly-required-number} {@code @explorer-active-assembly}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/assembly-required-number-field.spec.js}
 * from {@code modules/perc-qa-automation/frontend}.</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");

const OLD_QTY = "12";
const NEW_QTY = "27";
const TITLE = "Welcome";
const LONG_NOTE = "A long note";

function assemblySpaUrl(baseUrl, query = "") {
  const root = String(baseUrl || "").replace(/\/$/, "");
  const q = query.startsWith("?") ? query : query ? `?${query}` : "";
  const params = new URLSearchParams(q.startsWith("?") ? q.slice(1) : q);
  params.set("entry", "assembly");
  return `${root}/Rhythmyx/cm/app/spa.jsp?${params.toString()}`;
}

function fieldsBody(qty) {
  return {
    contentId: "42",
    contentType: "percPage",
    name: "Home",
    checkoutUser: "Admin",
    fields: [
      { name: "qty", value: qty },
      { name: "displaytitle", value: TITLE },
      { name: "notes", value: LONG_NOTE },
    ],
  };
}

function previewHtml(qty) {
  return `<!DOCTYPE html><html><body>
    <span data-perc-field="qty">${qty}</span>
    <h1 data-perc-field="displaytitle">${TITLE}</h1>
    <p data-perc-field="notes">${LONG_NOTE}</p>
  </body></html>`;
}

/**
 * @param {import("@playwright/test").Page} page
 * @param {{ putStatus?: number, qty?: { current: string } }} options
 */
async function installAssemblyRoutes(page, options) {
  const qtyState = options.qty || { current: OLD_QTY };
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
        const qty = rows.find((row) => (row.name || row.Name) === "qty");
        if (qty) {
          qtyState.current = String(qty.value ?? qty.Value ?? qtyState.current);
        }
      } catch {
        // Keep the previous number when the body cannot be read.
      }
    }
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(fieldsBody(qtyState.current)),
    });
  });
  await page.route("**/services/contenttypes/**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        name: "percPage",
        fields: [
          {
            name: "qty",
            label: "Quantity",
            control: "sys_Number",
            dataType: "integer",
            required: true,
          },
          { name: "displaytitle", label: "Display title", control: "sys_EditBox" },
          { name: "notes", label: "Notes", control: "sys_TextArea" },
        ],
      }),
    });
  });
  await page.route("**/assembler/render**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "text/html",
      body: previewHtml(qtyState.current),
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

test.describe("assembly host required number", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test(
    "does not save a blank required number and reloads the previous number",
    { tag: ["@assembly-required-number", "@explorer-active-assembly", "@explorer"] },
    async ({ page }) => {
      const { blocked, consoleErrors } = watchNoise(page);
      const qty = { current: OLD_QTY };
      const puts = await installAssemblyRoutes(page, { qty });
      await page.goto(assemblySpaUrl(BASE_URL, "contentId=42&templateId=7"));
      const frame = page.frameLocator('[data-testid="assembly-preview-frame"]');
      const qtyField = frame.locator('[data-testid="assembly-inline-field-qty"]');
      await expect(qtyField).toBeVisible({ timeout: 20_000 });
      await expect(qtyField).toHaveText(OLD_QTY);
      await expect(qtyField).toHaveAttribute("data-assembly-required", "true");
      await expect(page.locator('[data-testid="assembly-field-chip-qty"]')).toHaveAttribute(
        "data-required",
        "true",
      );
      await qtyField.fill("");
      expect(puts, "editing without Save must not write").toEqual([]);
      await page.locator('[data-testid="assembly-field-save"]').click();
      await expect(page.locator('[data-testid="assembly-field-error-qty"]')).toContainText(
        /required/i,
        { timeout: 10_000 },
      );
      await expect(page.locator('[data-testid="assembly-field-notice"]')).toContainText(/required/i);
      await expect(page.locator('[data-testid="assembly-field-notice"]')).not.toContainText(
        /fields saved/i,
      );
      await expect(page.locator('[data-testid="assembly-field-notice"]')).not.toContainText(
        /whole number/i,
      );
      expect(puts, "a blank required number must not be written").toEqual([]);
      await expect(qtyField).not.toHaveAttribute("aria-invalid", "true");

      await page.goto(assemblySpaUrl(BASE_URL, "contentId=42&templateId=7"));
      const reloaded = page.frameLocator('[data-testid="assembly-preview-frame"]');
      const reloadedQty = reloaded.locator('[data-testid="assembly-inline-field-qty"]');
      await expect(reloadedQty).toBeVisible({ timeout: 15_000 });
      await expect(reloadedQty).toHaveText(OLD_QTY);
      await expect(reloaded.locator('[data-testid="assembly-inline-field-displaytitle"]')).toHaveText(
        TITLE,
      );
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="assembly-host"]',
      });
      assertQuiet(blocked, consoleErrors);
    },
  );

  test(
    "does not save whitespace-only required number",
    { tag: ["@assembly-required-number", "@explorer"] },
    async ({ page }) => {
      const { blocked, consoleErrors } = watchNoise(page);
      const puts = await installAssemblyRoutes(page, {});
      await page.goto(assemblySpaUrl(BASE_URL, "contentId=42&templateId=7"));
      const frame = page.frameLocator('[data-testid="assembly-preview-frame"]');
      const qtyField = frame.locator('[data-testid="assembly-inline-field-qty"]');
      await expect(qtyField).toBeVisible({ timeout: 20_000 });
      await qtyField.fill("   ");
      await page.locator('[data-testid="assembly-field-save"]').click();
      await expect(page.locator('[data-testid="assembly-field-error-qty"]')).toContainText(
        /required/i,
        { timeout: 10_000 },
      );
      expect(puts).toEqual([]);
      await expect(page.locator('[data-testid="assembly-field-notice"]')).not.toContainText(
        /fields saved/i,
      );
      await page.goto(assemblySpaUrl(BASE_URL, "contentId=42&templateId=7"));
      const reloaded = page.frameLocator('[data-testid="assembly-preview-frame"]');
      await expect(reloaded.locator('[data-testid="assembly-inline-field-qty"]')).toHaveText(
        OLD_QTY,
        { timeout: 15_000 },
      );
      assertQuiet(blocked, consoleErrors);
    },
  );

  test(
    "Cancel does not write a blank required number edit",
    { tag: ["@assembly-required-number", "@explorer"] },
    async ({ page }) => {
      const { blocked, consoleErrors } = watchNoise(page);
      const puts = await installAssemblyRoutes(page, {});
      await page.goto(assemblySpaUrl(BASE_URL, "contentId=42&templateId=7"));
      const frame = page.frameLocator('[data-testid="assembly-preview-frame"]');
      const qtyField = frame.locator('[data-testid="assembly-inline-field-qty"]');
      await expect(qtyField).toBeVisible({ timeout: 20_000 });
      await qtyField.fill("");
      await page.locator('[data-testid="assembly-field-cancel"]').click();
      expect(puts, "Cancel must not write the blank edit").toEqual([]);
      await expect(page.locator('[data-testid="assembly-field-notice"]')).toHaveCount(0);
      await expect(qtyField).toHaveText(OLD_QTY);
      await page.goto(assemblySpaUrl(BASE_URL, "contentId=42&templateId=7"));
      const reloaded = page.frameLocator('[data-testid="assembly-preview-frame"]');
      await expect(reloaded.locator('[data-testid="assembly-inline-field-qty"]')).toHaveText(
        OLD_QTY,
        { timeout: 15_000 },
      );
      assertQuiet(blocked, consoleErrors);
    },
  );

  test(
    "still saves a non-blank required whole number",
    { tag: ["@assembly-required-number", "@explorer"] },
    async ({ page }) => {
      const { blocked, consoleErrors } = watchNoise(page);
      const qty = { current: OLD_QTY };
      const puts = await installAssemblyRoutes(page, { qty });
      await page.goto(assemblySpaUrl(BASE_URL, "contentId=42&templateId=7"));
      const frame = page.frameLocator('[data-testid="assembly-preview-frame"]');
      const qtyField = frame.locator('[data-testid="assembly-inline-field-qty"]');
      await expect(qtyField).toBeVisible({ timeout: 20_000 });
      await qtyField.fill(NEW_QTY);
      await page.locator('[data-testid="assembly-field-save"]').click();
      await expect(page.locator('[data-testid="assembly-field-notice"]')).toContainText(/saved/i, {
        timeout: 10_000,
      });
      expect(puts.length).toBeGreaterThan(0);
      const saved = JSON.parse(puts[puts.length - 1]);
      const rows = saved.fields || saved.Fields || [];
      expect(fieldValue(rows, "qty")).toBe(NEW_QTY);
      expect(fieldValue(rows, "displaytitle")).toBe(TITLE);
      expect(fieldValue(rows, "notes")).toBe(LONG_NOTE);
      const qtyRow = rows.find((row) => (row.name || row.Name) === "qty");
      expect(qtyRow.dataType || qtyRow.DataType).toBe("integer");
      await page.goto(assemblySpaUrl(BASE_URL, "contentId=42&templateId=7"));
      const reloaded = page.frameLocator('[data-testid="assembly-preview-frame"]');
      await expect(reloaded.locator('[data-testid="assembly-inline-field-qty"]')).toHaveText(
        NEW_QTY,
        { timeout: 15_000 },
      );
      assertQuiet(blocked, consoleErrors);
    },
  );

  for (const status of [400, 403, 409]) {
    test(
      `HTTP ${status} on a required number does not claim success`,
      { tag: ["@assembly-required-number", "@explorer"] },
      async ({ page }) => {
        const { blocked, consoleErrors } = watchNoise(page);
        const puts = await installAssemblyRoutes(page, { putStatus: status });
        await page.goto(assemblySpaUrl(BASE_URL, "contentId=42&templateId=7"));
        const frame = page.frameLocator('[data-testid="assembly-preview-frame"]');
        const qtyField = frame.locator('[data-testid="assembly-inline-field-qty"]');
        await expect(qtyField).toBeVisible({ timeout: 20_000 });
        await qtyField.fill(NEW_QTY);
        await page.locator('[data-testid="assembly-field-save"]').click();
        await expect(page.locator('[data-testid="assembly-field-notice"]')).toContainText(
          /could not save/i,
          { timeout: 10_000 },
        );
        await expect(qtyField).toHaveText(OLD_QTY);
        expect(puts.length).toBeGreaterThan(0);
        await expect(page.locator('[data-testid="assembly-field-notice"]')).not.toContainText(
          /fields saved/i,
        );
        assertQuiet(blocked, consoleErrors);
      },
    );
  }
});
