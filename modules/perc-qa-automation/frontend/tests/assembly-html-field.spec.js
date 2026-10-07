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
 * Assembly host — save one HTML field on the assembled page.
 *
 * <p>Tags: {@code @assembly-html-field} {@code @explorer-active-assembly}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/assembly-html-field.spec.js}
 * from {@code modules/perc-qa-automation/frontend}.</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");

const OLD_HTML = "<p>About the site</p>";
const NEW_HTML = "<p>Updated body</p>";

function assemblySpaUrl(baseUrl, query = "") {
  const root = String(baseUrl || "").replace(/\/$/, "");
  const q = query.startsWith("?") ? query : query ? `?${query}` : "";
  const params = new URLSearchParams(q.startsWith("?") ? q.slice(1) : q);
  params.set("entry", "assembly");
  return `${root}/Rhythmyx/cm/app/spa.jsp?${params.toString()}`;
}

function fieldsBody(html) {
  return {
    contentId: "42",
    contentType: "percPage",
    name: "Home",
    checkoutUser: "Admin",
    fields: [
      { name: "displaytitle", value: "Welcome" },
      { name: "notes", value: "A long note" },
      { name: "description", value: html },
    ],
  };
}

function previewHtml(html) {
  return `<!DOCTYPE html><html><body>
    <h1 data-perc-field="displaytitle">Welcome</h1>
    <p data-perc-field="notes">A long note</p>
    <div class="PsAaField" id='[3,42,7,0,0,0,0,1,0,0,0,"description",42,"Body",0]'>${html}</div>
  </body></html>`;
}

/**
 * @param {import("@playwright/test").Page} page
 * @param {{ putStatus?: number, readOnlyHtml?: boolean, html?: { current: string } }} options
 */
async function installAssemblyRoutes(page, options) {
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
        const description = rows.find((row) => (row.name || row.Name) === "description");
        if (description) {
          htmlState.current = String(description.value ?? description.Value ?? htmlState.current);
        }
      } catch {
        // Keep the previous HTML when the body cannot be read.
      }
    }
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(fieldsBody(htmlState.current)),
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
          {
            name: "description",
            label: "Body",
            control: "sys_tinymce",
            readOnly: options.readOnlyHtml === true,
          },
        ],
      }),
    });
  });
  await page.route("**/assembler/render**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "text/html",
      body: previewHtml(htmlState.current),
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

test.describe("assembly host HTML field", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test(
    "saves one assembled HTML field and shows it again after reload",
    { tag: ["@assembly-html-field", "@explorer-active-assembly", "@explorer"] },
    async ({ page }) => {
      const { blocked, consoleErrors } = watchNoise(page);
      const html = { current: OLD_HTML };
      const puts = await installAssemblyRoutes(page, { html });
      await page.goto(assemblySpaUrl(BASE_URL, "contentId=42&templateId=7"));
      await expect(page.locator('[data-testid="assembly-host"]')).toBeVisible({
        timeout: 20_000,
      });
      const frame = page.frameLocator('[data-testid="assembly-preview-frame"]');
      const body = frame.locator('[data-testid="assembly-inline-field-description"]');
      await expect(body).toBeVisible({ timeout: 15_000 });
      await expect(frame.locator('[data-testid="assembly-inline-field-displaytitle"]')).toBeVisible();
      await expect(frame.locator('[data-testid="assembly-inline-field-notes"]')).toBeVisible();

      await body.evaluate((el, next) => {
        el.innerHTML = next;
      }, NEW_HTML);
      expect(puts, "editing without Save must not write").toEqual([]);
      await page.locator('[data-testid="assembly-field-save"]').click();
      await expect(page.locator('[data-testid="assembly-field-notice"]')).toContainText(/saved/i, {
        timeout: 10_000,
      });
      expect(puts.length).toBeGreaterThan(0);
      expect(puts.some((bodyText) => bodyText.includes(NEW_HTML))).toBeTruthy();
      expect(puts.some((bodyText) => bodyText.includes("Welcome"))).toBeTruthy();
      expect(puts.some((bodyText) => bodyText.includes("A long note"))).toBeTruthy();

      await page.goto(assemblySpaUrl(BASE_URL, "contentId=42&templateId=7"));
      await expect(page.locator('[data-testid="assembly-host"]')).toBeVisible({
        timeout: 20_000,
      });
      const reloaded = page.frameLocator('[data-testid="assembly-preview-frame"]');
      await expect(reloaded.locator('[data-testid="assembly-inline-field-description"]')).toBeVisible({
        timeout: 15_000,
      });
      await expect
        .poll(async () =>
          reloaded.locator('[data-testid="assembly-inline-field-description"]').evaluate((el) =>
            el.innerHTML.trim(),
          ),
        )
        .toBe(NEW_HTML);

      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="assembly-host"]',
      });
      assertQuiet(blocked, consoleErrors);
    },
  );

  test(
    "does not write a read-only HTML field",
    { tag: ["@assembly-html-field", "@explorer"] },
    async ({ page }) => {
      const { blocked, consoleErrors } = watchNoise(page);
      const puts = await installAssemblyRoutes(page, { readOnlyHtml: true });
      await page.goto(assemblySpaUrl(BASE_URL, "contentId=42&templateId=7"));
      await expect(page.locator('[data-testid="assembly-field-chip-displaytitle"]')).toBeVisible({
        timeout: 20_000,
      });
      await expect(page.locator('[data-testid="assembly-field-chip-description"]')).toHaveCount(0);
      const frame = page.frameLocator('[data-testid="assembly-preview-frame"]');
      await expect(frame.locator('[data-testid="assembly-inline-field-displaytitle"]')).toBeVisible({
        timeout: 15_000,
      });
      await expect(frame.locator('[data-testid="assembly-inline-field-description"]')).toHaveCount(0);
      await frame.locator('[data-testid="assembly-inline-field-displaytitle"]').fill("Renamed title");
      await frame.locator('[data-testid="assembly-inline-field-notes"]').fill("Updated note");
      await page.locator('[data-testid="assembly-field-save"]').click();
      await expect(page.locator('[data-testid="assembly-field-notice"]')).toContainText(/saved/i, {
        timeout: 10_000,
      });
      expect(puts.some((bodyText) => bodyText.includes("Renamed title"))).toBeTruthy();
      expect(puts.some((bodyText) => bodyText.includes("Updated note"))).toBeTruthy();
      expect(puts.some((bodyText) => bodyText.includes(NEW_HTML))).toBeFalsy();
      const saved = JSON.parse(puts[puts.length - 1]);
      const rows = saved.fields || saved.Fields || [];
      const description = rows.find((row) => (row.name || row.Name) === "description");
      expect(description.value || description.Value).toBe(OLD_HTML);
      assertQuiet(blocked, consoleErrors);
    },
  );

  for (const status of [400, 403, 409]) {
    test(
      `HTTP ${status} leaves the previous HTML in place`,
      { tag: ["@assembly-html-field", "@explorer"] },
      async ({ page }) => {
        const { blocked, consoleErrors } = watchNoise(page);
        const puts = await installAssemblyRoutes(page, { putStatus: status });
        await page.goto(assemblySpaUrl(BASE_URL, "contentId=42&templateId=7"));
        const frame = page.frameLocator('[data-testid="assembly-preview-frame"]');
        const body = frame.locator('[data-testid="assembly-inline-field-description"]');
        await expect(body).toBeVisible({ timeout: 20_000 });
        await body.evaluate((el, next) => {
          el.innerHTML = next;
        }, NEW_HTML);
        await page.locator('[data-testid="assembly-field-save"]').click();
        await expect(page.locator('[data-testid="assembly-field-notice"]')).toContainText(
          /could not save/i,
          { timeout: 10_000 },
        );
        await expect
          .poll(async () => body.evaluate((el) => el.innerHTML.trim()))
          .toBe(OLD_HTML);
        expect(puts.length).toBeGreaterThan(0);
        await expect(page.locator('[data-testid="assembly-field-notice"]')).not.toContainText(
          /fields saved/i,
        );
        assertQuiet(blocked, consoleErrors);
      },
    );
  }
});
