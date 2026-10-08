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
 * Assembly host — save one datetime on the assembled page.
 *
 * <p>Tags: {@code @assembly-datetime-field} {@code @explorer-active-assembly}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/assembly-datetime-field.spec.js}
 * from {@code modules/perc-qa-automation/frontend}.</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");

const OLD_AT = "2026-10-07 15:30:00";
const OLD_WIDGET = "2026-10-07T15:30";
const NEW_AT = "2026-11-02 09:05:00";
const NEW_WIDGET = "2026-11-02T09:05";
const TITLE = "Welcome";
const LONG_NOTE = "A long note";

function assemblySpaUrl(baseUrl, query = "") {
  const root = String(baseUrl || "").replace(/\/$/, "");
  const q = query.startsWith("?") ? query : query ? `?${query}` : "";
  const params = new URLSearchParams(q.startsWith("?") ? q.slice(1) : q);
  params.set("entry", "assembly");
  return `${root}/Rhythmyx/cm/app/spa.jsp?${params.toString()}`;
}

function fieldsBody(eventAt) {
  return {
    contentId: "42",
    contentType: "percPage",
    name: "Home",
    checkoutUser: "Admin",
    fields: [
      { name: "event_at", value: eventAt },
      { name: "displaytitle", value: TITLE },
      { name: "notes", value: LONG_NOTE },
    ],
  };
}

function previewHtml(eventAt) {
  return `<!DOCTYPE html><html><body>
    <span data-perc-field="event_at">${eventAt}</span>
    <h1 data-perc-field="displaytitle">${TITLE}</h1>
    <p data-perc-field="notes">${LONG_NOTE}</p>
  </body></html>`;
}

/**
 * @param {import("@playwright/test").Page} page
 * @param {{ putStatus?: number, readOnly?: boolean, required?: boolean, eventAt?: { current: string } }} options
 */
async function installAssemblyRoutes(page, options) {
  const eventAt = options.eventAt || { current: OLD_AT };
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
        const row = rows.find((item) => (item.name || item.Name) === "event_at");
        if (row) {
          eventAt.current = String(row.value ?? row.Value ?? eventAt.current);
        }
      } catch {
        // Keep the previous datetime when the body cannot be read.
      }
    }
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(fieldsBody(eventAt.current)),
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
            name: "event_at",
            label: "Event at",
            control: "sys_CalendarSimple",
            dataType: "datetime",
            readOnly: options.readOnly === true,
            required: options.required === true,
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
      body: previewHtml(eventAt.current),
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

test.describe("assembly host datetime field", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test(
    "saves one assembled datetime and shows the same date and time after reload",
    { tag: ["@assembly-datetime-field", "@explorer-active-assembly", "@explorer"] },
    async ({ page }) => {
      const { blocked, consoleErrors } = watchNoise(page);
      const eventAt = { current: OLD_AT };
      const puts = await installAssemblyRoutes(page, { eventAt });
      await page.goto(assemblySpaUrl(BASE_URL, "contentId=42&templateId=7"));
      await expect(page.locator('[data-testid="assembly-host"]')).toBeVisible({
        timeout: 20_000,
      });
      const frame = page.frameLocator('[data-testid="assembly-preview-frame"]');
      const field = frame.locator('[data-testid="assembly-inline-field-event_at"]');
      await expect(field).toBeVisible({ timeout: 15_000 });
      await expect(field).toHaveValue(OLD_WIDGET);
      await expect(frame.locator('[data-testid="assembly-inline-field-displaytitle"]')).toHaveText(
        TITLE,
      );

      await field.fill(NEW_WIDGET);
      expect(puts, "editing without Save must not write").toEqual([]);
      await page.locator('[data-testid="assembly-field-save"]').click();
      await expect(page.locator('[data-testid="assembly-field-notice"]')).toContainText(/saved/i, {
        timeout: 10_000,
      });
      expect(puts.length).toBeGreaterThan(0);
      const saved = JSON.parse(puts[puts.length - 1]);
      const rows = saved.fields || saved.Fields || [];
      expect(fieldValue(rows, "event_at")).toBe(NEW_AT);
      expect(fieldValue(rows, "displaytitle")).toBe(TITLE);
      expect(fieldValue(rows, "notes")).toBe(LONG_NOTE);
      const datetimeRow = rows.find((row) => (row.name || row.Name) === "event_at");
      expect(datetimeRow.dataType || datetimeRow.DataType).toBe("datetime");
      const titleRow = rows.find((row) => (row.name || row.Name) === "displaytitle");
      expect(titleRow.dataType || titleRow.DataType).toBeUndefined();
      await expect(field).toHaveValue(NEW_WIDGET);

      await page.goto(assemblySpaUrl(BASE_URL, "contentId=42&templateId=7"));
      await expect(page.locator('[data-testid="assembly-host"]')).toBeVisible({
        timeout: 20_000,
      });
      const reloaded = page.frameLocator('[data-testid="assembly-preview-frame"]');
      await expect(reloaded.locator('[data-testid="assembly-inline-field-event_at"]')).toHaveValue(
        NEW_WIDGET,
        { timeout: 15_000 },
      );
      await expect(
        reloaded.locator('[data-testid="assembly-inline-field-displaytitle"]'),
      ).toHaveText(TITLE);

      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="assembly-host"]',
      });
      assertQuiet(blocked, consoleErrors);
    },
  );

  test(
    "Cancel does not write a datetime edit",
    { tag: ["@assembly-datetime-field", "@explorer"] },
    async ({ page }) => {
      const { blocked, consoleErrors } = watchNoise(page);
      const puts = await installAssemblyRoutes(page, {});
      await page.goto(assemblySpaUrl(BASE_URL, "contentId=42&templateId=7"));
      const frame = page.frameLocator('[data-testid="assembly-preview-frame"]');
      const field = frame.locator('[data-testid="assembly-inline-field-event_at"]');
      await expect(field).toBeVisible({ timeout: 20_000 });
      await field.fill(NEW_WIDGET);
      await page.locator('[data-testid="assembly-field-cancel"]').click();
      expect(puts, "Cancel must not write").toEqual([]);
      await expect(page.locator('[data-testid="assembly-field-notice"]')).toHaveCount(0);
      await expect(field).toHaveValue(OLD_WIDGET);
      await page.goto(assemblySpaUrl(BASE_URL, "contentId=42&templateId=7"));
      const reloaded = page.frameLocator('[data-testid="assembly-preview-frame"]');
      await expect(reloaded.locator('[data-testid="assembly-inline-field-event_at"]')).toHaveValue(
        OLD_WIDGET,
        { timeout: 15_000 },
      );
      assertQuiet(blocked, consoleErrors);
    },
  );

  test(
    "does not save a blank required datetime",
    { tag: ["@assembly-datetime-field", "@explorer"] },
    async ({ page }) => {
      const { blocked, consoleErrors } = watchNoise(page);
      const puts = await installAssemblyRoutes(page, { required: true });
      await page.goto(assemblySpaUrl(BASE_URL, "contentId=42&templateId=7"));
      const frame = page.frameLocator('[data-testid="assembly-preview-frame"]');
      const field = frame.locator('[data-testid="assembly-inline-field-event_at"]');
      await expect(field).toBeVisible({ timeout: 20_000 });
      await expect(field).toHaveAttribute("aria-required", "true");
      await field.fill("");
      await page.locator('[data-testid="assembly-field-save"]').click();
      await expect(page.locator('[data-testid="assembly-field-notice"]')).toContainText(
        /required/i,
        { timeout: 10_000 },
      );
      await expect(page.locator('[data-testid="assembly-field-notice"]')).not.toContainText(
        /fields saved/i,
      );
      expect(puts).toEqual([]);
      await page.goto(assemblySpaUrl(BASE_URL, "contentId=42&templateId=7"));
      const reloaded = page.frameLocator('[data-testid="assembly-preview-frame"]');
      await expect(reloaded.locator('[data-testid="assembly-inline-field-event_at"]')).toHaveValue(
        OLD_WIDGET,
        { timeout: 15_000 },
      );
      assertQuiet(blocked, consoleErrors);
    },
  );

  test(
    "an optional clear writes a blank datetime",
    { tag: ["@assembly-datetime-field", "@explorer"] },
    async ({ page }) => {
      const { blocked, consoleErrors } = watchNoise(page);
      const eventAt = { current: OLD_AT };
      const puts = await installAssemblyRoutes(page, { eventAt, required: false });
      await page.goto(assemblySpaUrl(BASE_URL, "contentId=42&templateId=7"));
      const frame = page.frameLocator('[data-testid="assembly-preview-frame"]');
      const field = frame.locator('[data-testid="assembly-inline-field-event_at"]');
      await expect(field).toBeVisible({ timeout: 20_000 });
      await field.fill("");
      await page.locator('[data-testid="assembly-field-save"]').click();
      await expect(page.locator('[data-testid="assembly-field-notice"]')).toContainText(/saved/i, {
        timeout: 10_000,
      });
      const saved = JSON.parse(puts[puts.length - 1]);
      const rows = saved.fields || saved.Fields || [];
      expect(fieldValue(rows, "event_at")).toBe("");
      expect(fieldValue(rows, "displaytitle")).toBe(TITLE);
      const datetimeRow = rows.find((row) => (row.name || row.Name) === "event_at");
      expect(datetimeRow.dataType || datetimeRow.DataType).toBe("datetime");
      await page.goto(assemblySpaUrl(BASE_URL, "contentId=42&templateId=7"));
      const reloaded = page.frameLocator('[data-testid="assembly-preview-frame"]');
      await expect(reloaded.locator('[data-testid="assembly-inline-field-event_at"]')).toHaveValue(
        "",
        { timeout: 15_000 },
      );
      assertQuiet(blocked, consoleErrors);
    },
  );

  test(
    "does not write a read-only datetime field",
    { tag: ["@assembly-datetime-field", "@explorer"] },
    async ({ page }) => {
      const { blocked, consoleErrors } = watchNoise(page);
      const puts = await installAssemblyRoutes(page, { readOnly: true });
      await page.goto(assemblySpaUrl(BASE_URL, "contentId=42&templateId=7"));
      await expect(page.locator('[data-testid="assembly-field-chip-displaytitle"]')).toBeVisible({
        timeout: 20_000,
      });
      await expect(page.locator('[data-testid="assembly-field-chip-event_at"]')).toHaveCount(0);
      const frame = page.frameLocator('[data-testid="assembly-preview-frame"]');
      await expect(frame.locator('[data-testid="assembly-inline-field-displaytitle"]')).toBeVisible({
        timeout: 15_000,
      });
      await expect(frame.locator('[data-testid="assembly-inline-field-event_at"]')).toHaveCount(0);
      await frame.locator('[data-testid="assembly-inline-field-displaytitle"]').fill("Updated welcome");
      await page.locator('[data-testid="assembly-field-save"]').click();
      await expect(page.locator('[data-testid="assembly-field-notice"]')).toContainText(/saved/i, {
        timeout: 10_000,
      });
      const saved = JSON.parse(puts[puts.length - 1]);
      const rows = saved.fields || saved.Fields || [];
      expect(fieldValue(rows, "event_at")).toBe(OLD_AT);
      expect(fieldValue(rows, "displaytitle")).toBe("Updated welcome");
      expect(fieldValue(rows, "notes")).toBe(LONG_NOTE);
      const datetimeRow = rows.find((row) => (row.name || row.Name) === "event_at");
      expect(datetimeRow.dataType || datetimeRow.DataType).toBeUndefined();
      assertQuiet(blocked, consoleErrors);
    },
  );

  test(
    "does not save a value that is not a date and time",
    { tag: ["@assembly-datetime-field", "@explorer"] },
    async ({ page }) => {
      const { blocked, consoleErrors } = watchNoise(page);
      const puts = await installAssemblyRoutes(page, {});
      await page.goto(assemblySpaUrl(BASE_URL, "contentId=42&templateId=7"));
      const frame = page.frameLocator('[data-testid="assembly-preview-frame"]');
      const field = frame.locator('[data-testid="assembly-inline-field-event_at"]');
      await expect(field).toBeVisible({ timeout: 20_000 });
      await field.evaluate((el) => {
        const input = /** @type {HTMLInputElement} */ (el);
        input.type = "text";
        input.value = "not-a-date";
      });
      await page.locator('[data-testid="assembly-field-save"]').click();
      await expect(page.locator('[data-testid="assembly-field-notice"]')).toContainText(
        /date and time/i,
        { timeout: 10_000 },
      );
      await expect(page.locator('[data-testid="assembly-field-notice"]')).not.toContainText(
        /fields saved/i,
      );
      expect(puts).toEqual([]);
      await expect(field).toHaveValue(OLD_WIDGET);
      assertQuiet(blocked, consoleErrors);
    },
  );

  for (const status of [400, 403, 409]) {
    test(
      `HTTP ${status} leaves the previous datetime in place`,
      { tag: ["@assembly-datetime-field", "@explorer"] },
      async ({ page }) => {
        const { blocked, consoleErrors } = watchNoise(page);
        const puts = await installAssemblyRoutes(page, { putStatus: status });
        await page.goto(assemblySpaUrl(BASE_URL, "contentId=42&templateId=7"));
        const frame = page.frameLocator('[data-testid="assembly-preview-frame"]');
        const field = frame.locator('[data-testid="assembly-inline-field-event_at"]');
        await expect(field).toBeVisible({ timeout: 20_000 });
        await field.fill(NEW_WIDGET);
        await page.locator('[data-testid="assembly-field-save"]').click();
        await expect(page.locator('[data-testid="assembly-field-notice"]')).toContainText(
          /could not save/i,
          { timeout: 10_000 },
        );
        await expect(field).toHaveValue(OLD_WIDGET);
        expect(puts.length).toBeGreaterThan(0);
        await expect(page.locator('[data-testid="assembly-field-notice"]')).not.toContainText(
          /fields saved/i,
        );
        assertQuiet(blocked, consoleErrors);
      },
    );
  }
});
