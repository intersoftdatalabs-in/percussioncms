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
 * Developer template Sites tab (#5742 / parent #1690 §7.3).
 *
 * Read-only: the template editor shows associated sites for publishing usage
 * (name + GUID) with an empty state when no site uses the template. This spec
 * performs no writes (no lock, no save); site-association writes stay in
 * Workbench until a follow-on slice.
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… \
 *     npm run test:surface -- --path tests/developer-template-sites.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

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

function asNamedObjectRefList(raw) {
  if (raw == null) {
    return [];
  }
  if (Array.isArray(raw)) {
    return raw;
  }
  if (typeof raw !== "object") {
    return [];
  }
  if (Array.isArray(raw.NamedObjectRef)) {
    return raw.NamedObjectRef;
  }
  if (raw.NamedObjectRef && typeof raw.NamedObjectRef === "object") {
    return asNamedObjectRefList(raw.NamedObjectRef);
  }
  if (raw.name || raw.label || raw.guid) {
    return [raw];
  }
  return [];
}

async function openTemplateDetail(page, namePattern) {
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
  const wantName = namePattern || "perc.page";
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
  await expect(page.locator('[data-testid="developer-tpl-lock-toolbar"]')).toBeVisible({
    timeout: 30_000,
  });
  return detail;
}

test.describe("Developer template Sites tab (#5742)", () => {
  test("Sites section lists associated sites or empty state (read-only)", async ({
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
    await openTemplateDetail(page);

    const sites = page.locator('[data-testid="developer-tpl-sites"]');
    await expect(sites).toBeVisible({ timeout: 20_000 });

    const name = (await page.locator('[data-testid="developer-tpl-detail-name"]').innerText()).trim();
    const getRes = await page.request.get(
      `${BASE_URL}/Rhythmyx/services/templates/${encodeURIComponent(name)}`,
    );
    expect(getRes.status(), `GET template ${getRes.status()}`).toBe(200);
    const wireSites = asNamedObjectRefList(
      unwrapTemplateDetail(await getRes.json()).associatedSites,
    );
    expect(
      Array.isArray(wireSites),
      "associatedSites must be present on GET (empty list is ok)",
    ).toBeTruthy();
    for (const row of wireSites) {
      expect(
        String((row && (row.name || row.label)) || "").trim(),
        `site row must have a name: ${JSON.stringify(row)}`,
      ).not.toBe("");
      expect(
        row.guid && (row.guid.stringValue || row.guid.uuid != null),
        `site row must include guid: ${JSON.stringify(row)}`,
      ).toBeTruthy();
    }

    if (wireSites.length === 0) {
      await expect(
        page.locator('[data-testid="developer-tpl-sites-empty"]'),
      ).toBeVisible({ timeout: 20_000 });
    } else {
      const sitesTable = page.locator('[data-testid="developer-tpl-sites-table"]');
      await expect(sitesTable).toBeVisible({ timeout: 20_000 });
      await expect(
        page.locator('[data-testid="developer-tpl-sites-name-0"]'),
      ).toContainText(wireSites[0].name || wireSites[0].label);
    }

    expect(pageErrors, `pageerror: ${pageErrors.join(" | ")}`).toEqual([]);
    expect(consoleErrors, `console error: ${consoleErrors.join(" | ")}`).toEqual([]);
  });
});
