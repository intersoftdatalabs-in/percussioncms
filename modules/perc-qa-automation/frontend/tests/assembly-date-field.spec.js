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
 * Assembly host — save one calendar date on the assembled page.
 *
 * <p>Tags: {@code @assembly-date-field} {@code @explorer-active-assembly}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/assembly-date-field.spec.js}
 * from {@code modules/perc-qa-automation/frontend}.</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");

const OLD_DATE = "2026-10-07";
const NEW_DATE = "2026-11-02";
const TITLE = "Welcome";
const LONG_NOTE = "A long note";

function assemblySpaUrl(baseUrl, query = "") {
  const root = String(baseUrl || "").replace(/\/$/, "");
  const q = query.startsWith("?") ? query : query ? `?${query}` : "";
  const params = new URLSearchParams(q.startsWith("?") ? q.slice(1) : q);
  params.set("entry", "assembly");
  return `${root}/Rhythmyx/cm/app/spa.jsp?${params.toString()}`;
}

function fieldsBody(eventOn) {
  return {
    contentId: "42",
    contentType: "percPage",
    name: "Home",
    checkoutUser: "Admin",
    fields: [
      { name: "event_on", value: eventOn },
      { name: "displaytitle", value: TITLE },
      { name: "notes", value: LONG_NOTE },
    ],
  };
}

function previewHtml(eventOn) {
  return `<!DOCTYPE html><html><body>
    <span data-perc-field="event_on">${eventOn}</span>
    <h1 data-perc-field="displaytitle">${TITLE}</h1>
    <p data-perc-field="notes">${LONG_NOTE}</p>
  </body></html>`;
}

/**
 * @param {import("@playwright/test").Page} page
 * @param {{ putStatus?: number, readOnlyDate?: boolean, eventOn?: { current: string } }} options
 */
async function installAssemblyRoutes(page, options) {
  const dateState = options.eventOn || { current: OLD_DATE };
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
        const eventOn = rows.find((row) => (row.name || row.Name) === "event_on");
        if (eventOn) {
          dateState.current = String(eventOn.value ?? eventOn.Value ?? dateState.current);
        }
      } catch {
        // Keep the previous date when the body cannot be read.
      }
    }
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(fieldsBody(dateState.current)),
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
            name: "event_on",
            label: "Event on",
            control: "sys_CalendarSimple",
            dataType: "date",
            readOnly: options.readOnlyDate === true,
          },
          {
            name: "event_at",
            label: "Event at",
            control: "sys_CalendarSimple",
            dataType: "datetime",
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
      body: previewHtml(dateState.current),
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

test.describe("assembly host date field", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test(
    "saves one assembled calendar date and shows it again after reload",
    { tag: ["@assembly-date-field", "@explorer-active-assembly", "@explorer"] },
    async ({ page }) => {
      const { blocked, consoleErrors } = watchNoise(page);
      const eventOn = { current: OLD_DATE };
      const puts = await installAssemblyRoutes(page, { eventOn });
      await page.goto(assemblySpaUrl(BASE_URL, "contentId=42&templateId=7"));
      await expect(page.locator('[data-testid="assembly-host"]')).toBeVisible({
        timeout: 20_000,
      });
      const frame = page.frameLocator('[data-testid="assembly-preview-frame"]');
      const dateField = frame.locator('[data-testid="assembly-inline-field-event_on"]');
      await expect(dateField).toBeVisible({ timeout: 15_000 });
      await expect(dateField).toHaveValue(OLD_DATE);
      await expect(frame.locator('[data-testid="assembly-inline-field-displaytitle"]')).toHaveText(
        TITLE,
      );
      await expect(frame.locator('[data-testid="assembly-inline-field-event_at"]')).toHaveCount(0);
      await expect(page.locator('[data-testid="assembly-field-chip-event_at"]')).toHaveCount(0);

      await dateField.fill(NEW_DATE);
      expect(puts, "editing without Save must not write").toEqual([]);
      await page.locator('[data-testid="assembly-field-save"]').click();
      await expect(page.locator('[data-testid="assembly-field-notice"]')).toContainText(/saved/i, {
        timeout: 10_000,
      });
      expect(puts.length).toBeGreaterThan(0);
      const saved = JSON.parse(puts[puts.length - 1]);
      const rows = saved.fields || saved.Fields || [];
      expect(fieldValue(rows, "event_on")).toBe(NEW_DATE);
      expect(fieldValue(rows, "displaytitle")).toBe(TITLE);
      expect(fieldValue(rows, "notes")).toBe(LONG_NOTE);
      const dateRow = rows.find((row) => (row.name || row.Name) === "event_on");
      expect(dateRow.dataType || dateRow.DataType).toBe("date");
      const titleRow = rows.find((row) => (row.name || row.Name) === "displaytitle");
      expect(titleRow.dataType || titleRow.DataType).toBeUndefined();
      await expect(dateField).toHaveValue(NEW_DATE);

      await page.goto(assemblySpaUrl(BASE_URL, "contentId=42&templateId=7"));
      await expect(page.locator('[data-testid="assembly-host"]')).toBeVisible({
        timeout: 20_000,
      });
      const reloaded = page.frameLocator('[data-testid="assembly-preview-frame"]');
      const reloadedDate = reloaded.locator('[data-testid="assembly-inline-field-event_on"]');
      await expect(reloadedDate).toBeVisible({ timeout: 15_000 });
      await expect(reloadedDate).toHaveValue(NEW_DATE);
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
    "Cancel does not write a date edit",
    { tag: ["@assembly-date-field", "@explorer"] },
    async ({ page }) => {
      const { blocked, consoleErrors } = watchNoise(page);
      const puts = await installAssemblyRoutes(page, {});
      await page.goto(assemblySpaUrl(BASE_URL, "contentId=42&templateId=7"));
      const frame = page.frameLocator('[data-testid="assembly-preview-frame"]');
      const dateField = frame.locator('[data-testid="assembly-inline-field-event_on"]');
      await expect(dateField).toBeVisible({ timeout: 20_000 });
      await dateField.fill(NEW_DATE);
      await page.locator('[data-testid="assembly-field-cancel"]').click();
      expect(puts, "Cancel must not write").toEqual([]);
      await expect(page.locator('[data-testid="assembly-field-notice"]')).toHaveCount(0);
      await expect(dateField).toHaveValue(OLD_DATE);
      await page.goto(assemblySpaUrl(BASE_URL, "contentId=42&templateId=7"));
      const reloaded = page.frameLocator('[data-testid="assembly-preview-frame"]');
      await expect(reloaded.locator('[data-testid="assembly-inline-field-event_on"]')).toHaveValue(
        OLD_DATE,
        { timeout: 15_000 },
      );
      assertQuiet(blocked, consoleErrors);
    },
  );

  test(
    "a blank date does not wipe the stored date",
    { tag: ["@assembly-date-field", "@explorer"] },
    async ({ page }) => {
      const { blocked, consoleErrors } = watchNoise(page);
      const puts = await installAssemblyRoutes(page, {});
      await page.goto(assemblySpaUrl(BASE_URL, "contentId=42&templateId=7"));
      const frame = page.frameLocator('[data-testid="assembly-preview-frame"]');
      const dateField = frame.locator('[data-testid="assembly-inline-field-event_on"]');
      await expect(dateField).toBeVisible({ timeout: 20_000 });
      await dateField.fill("");
      await page.locator('[data-testid="assembly-field-save"]').click();
      await expect(page.locator('[data-testid="assembly-field-notice"]')).toContainText(
        /calendar date/i,
        { timeout: 10_000 },
      );
      await expect(page.locator('[data-testid="assembly-field-notice"]')).not.toContainText(
        /fields saved/i,
      );
      await expect(dateField).toHaveValue(OLD_DATE);
      expect(puts).toEqual([]);
      assertQuiet(blocked, consoleErrors);
    },
  );

  test(
    "does not write a read-only date field",
    { tag: ["@assembly-date-field", "@explorer"] },
    async ({ page }) => {
      const { blocked, consoleErrors } = watchNoise(page);
      const puts = await installAssemblyRoutes(page, { readOnlyDate: true });
      await page.goto(assemblySpaUrl(BASE_URL, "contentId=42&templateId=7"));
      await expect(page.locator('[data-testid="assembly-field-chip-displaytitle"]')).toBeVisible({
        timeout: 20_000,
      });
      await expect(page.locator('[data-testid="assembly-field-chip-event_on"]')).toHaveCount(0);
      const frame = page.frameLocator('[data-testid="assembly-preview-frame"]');
      await expect(frame.locator('[data-testid="assembly-inline-field-displaytitle"]')).toBeVisible({
        timeout: 15_000,
      });
      await expect(frame.locator('[data-testid="assembly-inline-field-event_on"]')).toHaveCount(0);
      await frame.locator('[data-testid="assembly-inline-field-displaytitle"]').fill("Updated welcome");
      await page.locator('[data-testid="assembly-field-save"]').click();
      await expect(page.locator('[data-testid="assembly-field-notice"]')).toContainText(/saved/i, {
        timeout: 10_000,
      });
      const saved = JSON.parse(puts[puts.length - 1]);
      const rows = saved.fields || saved.Fields || [];
      expect(fieldValue(rows, "event_on")).toBe(OLD_DATE);
      expect(fieldValue(rows, "displaytitle")).toBe("Updated welcome");
      expect(fieldValue(rows, "notes")).toBe(LONG_NOTE);
      const dateRow = rows.find((row) => (row.name || row.Name) === "event_on");
      expect(dateRow.dataType || dateRow.DataType).toBeUndefined();
      assertQuiet(blocked, consoleErrors);
    },
  );

  for (const status of [400, 403, 409]) {
    test(
      `HTTP ${status} leaves the previous date in place`,
      { tag: ["@assembly-date-field", "@explorer"] },
      async ({ page }) => {
        const { blocked, consoleErrors } = watchNoise(page);
        const puts = await installAssemblyRoutes(page, { putStatus: status });
        await page.goto(assemblySpaUrl(BASE_URL, "contentId=42&templateId=7"));
        const frame = page.frameLocator('[data-testid="assembly-preview-frame"]');
        const dateField = frame.locator('[data-testid="assembly-inline-field-event_on"]');
        await expect(dateField).toBeVisible({ timeout: 20_000 });
        await dateField.fill(NEW_DATE);
        await page.locator('[data-testid="assembly-field-save"]').click();
        await expect(page.locator('[data-testid="assembly-field-notice"]')).toContainText(
          /could not save/i,
          { timeout: 10_000 },
        );
        await expect(dateField).toHaveValue(OLD_DATE);
        expect(puts.length).toBeGreaterThan(0);
        await expect(page.locator('[data-testid="assembly-field-notice"]')).not.toContainText(
          /fields saved/i,
        );
        assertQuiet(blocked, consoleErrors);
      },
    );
  }
});
