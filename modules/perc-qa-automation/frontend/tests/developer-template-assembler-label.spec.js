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
 * Developer Templates normalized assembler label (#5817 / parent #2626 slice 1).
 *
 * Read-only proof: the template detail Assembler row shows the canonical
 * epic label for the assembler's registered id (Velocity power path,
 * HTML-first, Markdown, legacy XSL, page/database/binary/dispatch/resource/
 * debug) instead of the raw extension path. Unknown values fall back to the
 * raw string; blank values render an em-dash. No lock, no PUT.
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… \
 *     npm run test:surface -- --path tests/developer-template-assembler-label.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

// Mirror of WebUI/src/main/ts/developer/templateAssembler.ts (kept in-spec
// so the surface proof fails closed when the panel drifts from the catalog).
const ASSEMBLER_LABELS = {
  velocityAssembler: "Velocity (power path)",
  htmlAssembler: "HTML-first",
  markdownAssembler: "Markdown",
  legacyAssembler: "Legacy XSL",
  pageAssembler: "Page",
  pageVariantAssembler: "Page variant",
  pageDatabaseAssembler: "Page database",
  binaryAssembler: "Binary",
  dispatchAssembler: "Dispatch",
  databaseAssembler: "Database",
  resourceAssembler: "Resource",
  debugAssembler: "Debug",
};

function shortId(raw) {
  if (typeof raw !== "string") {
    return null;
  }
  const trimmed = raw.trim();
  if (!trimmed) {
    return null;
  }
  const parts = trimmed.split("/");
  const tail = String(parts[parts.length - 1] || "").trim();
  return tail || null;
}

function expectedLabel(raw) {
  const id = shortId(raw);
  if (id == null) {
    return "—";
  }
  if (Object.prototype.hasOwnProperty.call(ASSEMBLER_LABELS, id)) {
    return ASSEMBLER_LABELS[id];
  }
  return String(raw).trim();
}

function developerTemplatesUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "templates",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

function unwrapTemplateDetail(payload) {
  if (payload == null) {
    return {};
  }
  if (payload.TemplateDetail && typeof payload.TemplateDetail === "object") {
    return payload.TemplateDetail;
  }
  return payload;
}

async function openTemplateDetail(page, wantName) {
  await page.goto(developerTemplatesUrl(), { waitUntil: "networkidle" });

  await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
    timeout: 20_000,
  });

  const panel = page.locator('[data-testid="developer-tpl-panel"]');
  const empty = page.locator('[data-testid="developer-tpl-empty"]');
  const listError = page.locator('[data-testid="developer-tpl-error"]');
  await expect(panel.or(empty).or(listError).first()).toBeVisible({
    timeout: 30_000,
  });

  if (await listError.isVisible()) {
    throw new Error(
      `Developer templates catalog error: ${(await listError.innerText()).trim()}`,
    );
  }
  if (await empty.isVisible()) {
    throw new Error(
      "No templates in catalog — fail closed (H2 QA must include sample templates)",
    );
  }

  const table = page.locator('[data-testid="developer-tpl-table"]');
  await expect(table).toBeVisible({ timeout: 15_000 });
  const byNameAttr = table.locator(`[data-tpl-name="${wantName}"]`);
  const named =
    (await byNameAttr.count()) > 0
      ? byNameAttr
      : table.locator('[data-testid^="developer-tpl-row-"]').filter({
          has: page.locator("td", {
            hasText: new RegExp(
              `^${wantName.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")}$`,
            ),
          }),
        });
  if ((await named.count()) === 0) {
    throw new Error(
      `Template ${wantName} not in Developer catalog — fail closed (do not fall back to row 0)`,
    );
  }
  const targetRow = named.first();
  await expect(targetRow).toBeVisible();
  const openBtn = targetRow.locator('button[aria-label^="Open "]');
  if (await openBtn.count()) {
    await openBtn.click();
  } else {
    await targetRow.click();
  }

  const detail = page.locator('[data-testid="developer-tpl-detail"]');
  const detailError = page.locator('[data-testid="developer-tpl-detail-error"]');
  await expect(detail.or(detailError).first()).toBeVisible({ timeout: 30_000 });
  if (await detailError.isVisible()) {
    throw new Error(
      `Template detail error: ${(await detailError.innerText()).trim()}`,
    );
  }
  return detail;
}

test.describe("Developer template assembler label (#5817)", () => {
  for (const templateName of ["perc.page", "perc.pageXml"]) {
    test(`Assembler row shows the normalized label for ${templateName}`, async ({
      page,
    }) => {
      test.setTimeout(120_000);
      const pageErrors = [];
      const consoleErrors = [];
      page.on("pageerror", (err) => {
        pageErrors.push(String(err && err.message ? err.message : err));
      });
      page.on("console", (msg) => {
        if (msg.type() === "error") {
          consoleErrors.push(msg.text());
        }
      });

      await loginAsAdmin(page);
      await openTemplateDetail(page, templateName);

      const name = (
        await page.locator('[data-testid="developer-tpl-detail-name"]').innerText()
      ).trim();
      const getRes = await page.request.get(
        `${BASE_URL}/Rhythmyx/services/templates/${encodeURIComponent(name)}`,
      );
      expect(getRes.status(), `GET template ${getRes.status()}`).toBe(200);
      const rawAssembler = unwrapTemplateDetail(await getRes.json()).assembler;

      const assembler = page.locator('[data-testid="developer-tpl-detail-assembler"]');
      await expect(assembler).toBeVisible({ timeout: 20_000 });
      await expect(assembler).toHaveText(expectedLabel(rawAssembler), {
        timeout: 20_000,
      });
      const id = shortId(rawAssembler);
      if (id != null) {
        expect(await assembler.getAttribute("data-assembler-id")).toBe(id);
      }

      expect(pageErrors, `pageerror: ${pageErrors.join(" | ")}`).toEqual([]);
      expect(consoleErrors, `console error: ${consoleErrors.join(" | ")}`).toEqual([]);
    });
  }
});
