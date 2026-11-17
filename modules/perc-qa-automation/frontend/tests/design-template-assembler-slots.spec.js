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
 * Design SPA assembler picker + visual slots (#2810 / parent #2631).
 *
 * Surface-filtered only:
 *   npm run test:surface -- --path tests/design-template-assembler-slots.spec.js
 *
 * QA mode: perc-devctl qa-up  TEST_CMS_URL + ADMIN_*  test:surface  qa-down.
 *
 * Entry: spa.jsp?entry=design&section=templates
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const {
  catalogRowSelector,
} = require("./helpers/developer-catalog-selectors");

function designTemplatesUrl() {
  const q = new URLSearchParams({
    entry: "design",
    section: "templates",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

test.describe("Design template assembler + slots (#2810)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(120_000);
    await loginAsAdmin(page);
  });

  test("open template shows assembler picker and slots surface @smoke @ui", async ({
    page,
  }) => {
    await page.goto(designTemplatesUrl(), { waitUntil: "networkidle" });

    await expect(page.locator('[data-testid="perc-design-shell"]')).toBeVisible({
      timeout: 20_000,
    });

    const error = page.locator('[data-testid="design-tpl-error"]');
    const panel = page.locator('[data-testid="design-tpl-panel"]');
    const empty = page.locator('[data-testid="design-tpl-empty"]');

    await expect(panel.or(empty).or(error).first()).toBeVisible({
      timeout: 30_000,
    });

    if (await error.isVisible()) {
      const msg = (await error.innerText()).trim();
      throw new Error(`Design templates catalog error: ${msg}`);
    }

    if (await empty.isVisible()) {
      test.skip(true, "No templates in catalog - cannot exercise assembler/slots");
      return;
    }

    const firstRow = page.locator(catalogRowSelector("design-tpl-row", 0));
    await expect(firstRow).toBeVisible({ timeout: 15_000 });
    const openBtn = page.locator('[data-testid="design-tpl-open-0"]');
    await expect(openBtn).toBeVisible({ timeout: 5_000 });
    await openBtn.click();

    await expect(page.locator('[data-testid="design-tpl-editor"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(
      page.locator('[data-testid="design-tpl-assembler-select"]'),
    ).toBeVisible({ timeout: 20_000 });
    await expect(page.locator('[data-testid="design-tpl-slots"]')).toBeVisible({
      timeout: 20_000,
    });

    // Slots section may be empty for some templates; still show surface.
    const slotsEmpty = page.locator('[data-testid="design-tpl-slots-empty"]');
    const slotCard = page.locator('[data-testid="design-tpl-slot-card-0"]');
    await expect(slotsEmpty.or(slotCard).first()).toBeVisible({ timeout: 20_000 });

    if (await slotCard.isVisible()) {
      // Touch rootclass to enable save when layout/styles are present; restore after.
      const root = page.locator('[data-testid="design-tpl-slot-rootclass-0"]');
      if (await root.isVisible()) {
        const original = await root.inputValue();
        const marker = `qa2810-${Date.now()}`;
        await root.fill(marker);
        const saveBtn = page.locator('[data-testid="design-tpl-editor-save"]');
        await expect(saveBtn).toBeEnabled({ timeout: 5_000 });
        await saveBtn.click();
        await expect(
          page.locator('[data-testid="design-tpl-editor-notice"]'),
        ).toBeVisible({ timeout: 30_000 });
        await expect(
          page.locator('[data-testid="design-tpl-editor-error"]'),
        ).toHaveCount(0);
        // Restore
        await root.fill(original);
        if (await saveBtn.isEnabled()) {
          await saveBtn.click();
          await expect(
            page.locator('[data-testid="design-tpl-editor-notice"]'),
          ).toBeVisible({ timeout: 30_000 });
        }
      }
    }

    await page.locator('[data-testid="design-tpl-editor-back"]').click();
    await expect(page.locator('[data-testid="design-tpl-panel"]')).toBeVisible({
      timeout: 15_000,
    });
  });

  test("invalid slot columns blocked with inline row error (#6477) @ui", async ({
    page,
  }) => {
    const consoleErrors = [];
    page.on("pageerror", (err) => consoleErrors.push(String(err)));
    page.on("console", (msg) => {
      if (msg.type() === "error") consoleErrors.push(msg.text());
    });

    await page.goto(designTemplatesUrl(), { waitUntil: "networkidle" });

    await expect(page.locator('[data-testid="perc-design-shell"]')).toBeVisible({
      timeout: 20_000,
    });

    const error = page.locator('[data-testid="design-tpl-error"]');
    const panel = page.locator('[data-testid="design-tpl-panel"]');
    const empty = page.locator('[data-testid="design-tpl-empty"]');

    await expect(panel.or(empty).or(error).first()).toBeVisible({
      timeout: 30_000,
    });

    if (await error.isVisible()) {
      const msg = (await error.innerText()).trim();
      throw new Error(`Design templates catalog error: ${msg}`);
    }

    if (await empty.isVisible()) {
      test.skip(true, "No templates in catalog - cannot exercise slot validation");
      return;
    }

    // Fixture: ensure the first library template maps at least one slot, via
    // the same public REST contract the editor uses (lock -> PUT slots ->
    // unlock). Restored after the assertions.
    const fixture = await page.evaluate(async (baseUrl) => {
      const meta = (name) =>
        document.querySelector(`meta[name="${name}"]`)?.getAttribute("content") || "";
      const csrfToken =
        (window.OWASP_CSRFTOKEN && window.OWASP_CSRFTOKEN.token) || meta("_csrf") || "";
      const csrfHeader = meta("_csrf_header") || "OWASP-CSRFTOKEN";
      const headers = { Accept: "application/json" };
      if (csrfToken) headers[csrfHeader] = csrfToken;
      const fail = async (what, res) => {
        const text = await res.text().catch(() => "");
        throw new Error(`${what} -> HTTP ${res.status} ${text.slice(0, 200)}`);
      };
      const getJson = async (path) => {
        const res = await fetch(`${baseUrl}/Rhythmyx/services${path}`, {
          headers,
        });
        if (!res.ok) await fail(`GET ${path}`, res);
        return res.json();
      };
      const post = async (path) => {
        const res = await fetch(`${baseUrl}/Rhythmyx/services${path}`, {
          method: "POST",
          headers,
        });
        if (!res.ok) await fail(`POST ${path}`, res);
      };
      const putJson = async (path, body) => {
        const res = await fetch(`${baseUrl}/Rhythmyx/services${path}`, {
          method: "PUT",
          headers: { ...headers, "Content-Type": "application/json" },
          body: JSON.stringify(body),
        });
        if (!res.ok) await fail(`PUT ${path}`, res);
        return res.json();
      };
      const unwrapList = (payload, keys) => {
        if (Array.isArray(payload)) return payload;
        for (const k of keys) {
          const v = payload?.[k];
          if (Array.isArray(v)) return v;
          if (v && typeof v === "object") {
            for (const kk of keys) {
              if (Array.isArray(v[kk])) return v[kk];
            }
            return [v];
          }
        }
        return [];
      };
      const unwrapDetail = (payload) =>
        payload?.TemplateDetail || payload?.templateDetail || payload || {};
      const asList = (v) =>
        Array.isArray(v) ? v : v && typeof v === "object" ? [v] : [];
      const templates = unwrapList(await getJson("/templates"), [
        "TemplateSummaryList",
        "TemplateSummary",
        "templateSummaryList",
      ]);
      const templateName = templates[0]?.templateName || templates[0]?.name;
      if (!templateName) throw new Error("templates list is empty");
      const slots = unwrapList(await getJson("/slots"), ["Slot", "slot", "SlotList"]);
      const slotName = slots[0]?.name;
      if (!slotName) throw new Error("slots list is empty");
      const detail = unwrapDetail(await getJson(`/templates/${encodeURIComponent(templateName)}`));
      const original = asList(detail.slots);
      const originalNames = original
        .map((s) => s?.name)
        .filter((n) => typeof n === "string" && n.length > 0);
      if (originalNames.includes(slotName)) {
        return { templateName, added: false, original };
      }
      await post(`/templates/${encodeURIComponent(templateName)}/lock`);
      try {
        await putJson(`/templates/${encodeURIComponent(templateName)}`, {
          TemplateDetail: {
            slots: [...original, { name: slotName }],
          },
        });
      } finally {
        await post(`/templates/${encodeURIComponent(templateName)}/unlock`);
      }
      const verify = unwrapDetail(
        await getJson(`/templates/${encodeURIComponent(templateName)}`),
      );
      const mapped = asList(verify.slots)
        .map((s) => s?.name)
        .filter((n) => typeof n === "string" && n.length > 0);
      return { templateName, added: true, original, mapped };
    }, BASE_URL);

    const openTestId = await page.evaluate((templateName) => {
      const rows = Array.from(
        document.querySelectorAll('[data-testid^="design-tpl-row-"]'),
      );
      for (const row of rows) {
        const cells = Array.from(row.children);
        const nameCell = cells[1];
        if (nameCell && (nameCell.textContent || "").trim() === templateName) {
          const btn = row.querySelector('[data-testid^="design-tpl-open-"]');
          return btn ? btn.getAttribute("data-testid") : null;
        }
      }
      return null;
    }, fixture.templateName);
    if (!openTestId) {
      throw new Error(`No library row for template ${fixture.templateName}`);
    }
    if ((fixture.mapped || []).length === 0 && fixture.added) {
      throw new Error(
        `Fixture mapping missing after PUT on ${fixture.templateName}`,
      );
    }
    await page.locator(`[data-testid="${openTestId}"]`).click();

    await expect(page.locator('[data-testid="design-tpl-editor"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(
      page.locator('[data-testid="design-tpl-editor-name"]'),
    ).toContainText(fixture.templateName, { timeout: 20_000 });

    const slotCard = page.locator('[data-testid="design-tpl-slot-card-0"]');
    await expect(slotCard).toBeVisible({ timeout: 20_000 });

    const columns = page.locator('[data-testid="design-tpl-slot-columns-0"]');
    await expect(columns).toBeVisible({ timeout: 10_000 });
    const original = await columns.inputValue();
    await columns.fill("two");

    const saveBtn = page.locator('[data-testid="design-tpl-editor-save"]');
    await expect(saveBtn).toBeEnabled({ timeout: 5_000 });
    await saveBtn.click();

    // Client-side slot-mapping validation blocks the PUT and marks the row.
    await expect(
      page.locator('[data-testid="design-tpl-editor-validation"]'),
    ).toBeVisible({ timeout: 10_000 });
    await expect(
      page.locator('[data-testid="design-tpl-editor-validation"]'),
    ).toContainText(/slots\[0\]\.columns/);
    await expect(
      page.locator('[data-testid="design-tpl-slot-validation-0"]'),
    ).toBeVisible({ timeout: 10_000 });
    await expect(
      page.locator('[data-testid="design-tpl-slot-validation-0"]'),
    ).toContainText(/non-negative whole number/);
    await expect(
      page.locator('[data-testid="design-tpl-editor-notice"]'),
    ).toHaveCount(0);

    // Restore the field; the inline error clears on edit.
    await columns.fill(original);
    await expect(
      page.locator('[data-testid="design-tpl-slot-validation-0"]'),
    ).toHaveCount(0);

    // Restore the fixture mapping when this run added it.
    if (fixture.added) {
      await page.evaluate(
        async ({ baseUrl, templateName, original }) => {
          const meta = (name) =>
            document.querySelector(`meta[name="${name}"]`)?.getAttribute("content") || "";
          const csrfToken =
            (window.OWASP_CSRFTOKEN && window.OWASP_CSRFTOKEN.token) || meta("_csrf") || "";
          const csrfHeader = meta("_csrf_header") || "OWASP-CSRFTOKEN";
          const headers = {
            Accept: "application/json",
            "Content-Type": "application/json",
          };
          if (csrfToken) headers[csrfHeader] = csrfToken;
          const fail = async (what, res) => {
            const text = await res.text().catch(() => "");
            throw new Error(`${what} -> HTTP ${res.status} ${text.slice(0, 200)}`);
          };
          const key = encodeURIComponent(templateName);
          let res = await fetch(`${baseUrl}/Rhythmyx/services/templates/${key}/lock`, {
            method: "POST",
            headers,
          });
          if (!res.ok) await fail("POST lock", res);
          try {
            res = await fetch(`${baseUrl}/Rhythmyx/services/templates/${key}`, {
              method: "PUT",
              headers,
              body: JSON.stringify({
                TemplateDetail: { slots: original },
              }),
            });
            if (!res.ok) await fail("PUT restore", res);
          } finally {
            await fetch(`${baseUrl}/Rhythmyx/services/templates/${key}/unlock`, {
              method: "POST",
              headers,
            });
          }
        },
        {
          baseUrl: BASE_URL,
          templateName: fixture.templateName,
          original: fixture.original,
        },
      );
    }

    await page.locator('[data-testid="design-tpl-editor-back"]').click();
    await expect(page.locator('[data-testid="design-tpl-panel"]')).toBeVisible({
      timeout: 15_000,
    });

    expect(consoleErrors).toEqual([]);
  });
});
