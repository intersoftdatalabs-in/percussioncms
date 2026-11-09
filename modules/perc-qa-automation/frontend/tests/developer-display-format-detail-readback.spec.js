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
 * Developer display format detail readback (#5866 / parent #1690, slice 2).
 *
 * Proves format details read back correctly in the React shell through
 * GET /services/displayformats/{idOrName}: packaged By_Author identity
 * (title, GUID, read-only name, label, description, usage), the column table
 * (position order, display name, render type, default-sort marker, sort
 * direction), and all-communities visibility — plus a created user format
 * whose label/description/GUID read back in the shell before REST cleanup.
 * No packaged format is written or deleted.
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… npm run test:surface -- --path tests/developer-display-format-detail-readback.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const {
  catalogOpenByExactName,
} = require("./helpers/developer-catalog-selectors");

function developerDisplayFormatsUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "display-formats",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

/** REST-safe unique display-format name (no spaces, wildcards, or path characters). */
function uniqueDisplayFormatName(prefix) {
  const a = Date.now().toString(36).replace(/[^a-z0-9]/g, "").slice(-4);
  const b = Math.random().toString(36).replace(/[^a-z0-9]/g, "").slice(2, 6);
  const suffix = `${a}${b}`.slice(0, 8);
  return `${prefix}${suffix || "x"}`;
}

/**
 * Same-origin fetch so OWASP CSRF + session cookies apply.
 *
 * @param {import("@playwright/test").Page} page
 * @param {string} path
 * @param {string} method
 * @param {object} [body]
 * @returns {Promise<{status: number, text: string}>}
 */
async function inPageJson(page, path, method, body) {
  return page.evaluate(
    async ({ path: url, method: httpMethod, body: payload }) => {
      const tokenObj = window.OWASP_CSRFTOKEN;
      const metaToken = document.querySelector('meta[name="_csrf"]');
      const metaHeader = document.querySelector('meta[name="_csrf_header"]');
      const token =
        (tokenObj && tokenObj.token) || (metaToken && metaToken.content) || "";
      const headerName =
        (metaHeader && metaHeader.content) || "OWASP-CSRFTOKEN";
      const headers = {
        Accept: "application/json",
        "Content-Type": "application/json",
      };
      if (token) {
        headers[headerName] = token;
      }
      const res = await fetch(url, {
        method: httpMethod,
        credentials: "same-origin",
        headers,
        body: payload === undefined ? undefined : JSON.stringify(payload),
      });
      const text = await res.text();
      return { status: res.status, text };
    },
    { path, method, body },
  );
}

function unwrapDisplayFormat(text) {
  let parsed;
  try {
    parsed = JSON.parse(text);
  } catch {
    return {};
  }
  return parsed.DisplayFormat || parsed.displayFormat || parsed;
}

function attachConsoleGuards(page) {
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
  return { pageErrors, consoleErrors };
}

function assertConsoleClean(pageErrors, consoleErrors) {
  expect(pageErrors, `pageerror: ${pageErrors.join(" | ")}`).toEqual([]);
  const unexpectedConsole = consoleErrors.filter(
    (t) => !/Failed to load resource/i.test(t) && !/favicon/i.test(t),
  );
  expect(
    unexpectedConsole,
    `console error: ${unexpectedConsole.join(" | ")}`,
  ).toEqual([]);
}

async function openDisplayFormatsCatalog(page) {
  await page.goto(developerDisplayFormatsUrl(), { waitUntil: "networkidle" });
  await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
    timeout: 20_000,
  });
  const panel = page.locator('[data-testid="developer-df-panel"]');
  const empty = page.locator('[data-testid="developer-df-empty"]');
  const listError = page.locator('[data-testid="developer-df-error"]');
  await expect(panel.or(empty).or(listError).first()).toBeVisible({
    timeout: 30_000,
  });
  if (await listError.isVisible()) {
    throw new Error(
      `Developer display formats catalog error: ${(await listError.innerText()).trim()}`,
    );
  }
}

async function openFormatDetail(page, name, opts = {}) {
  const { expectColumnsTable = true } = opts;
  await page
    .locator(catalogOpenByExactName("developer-df-open", "data-df-name", name))
    .click();
  await expect(page.locator('[data-testid="developer-df-detail"]')).toBeVisible({
    timeout: 20_000,
  });
  if (!expectColumnsTable) {
    return;
  }
  await expect(page.locator('[data-testid="developer-df-columns-table"]')).toBeVisible({
    timeout: 20_000,
  });
}

/** Packaged By_Author column readback (position order on H2). */
const BY_AUTHOR_COLUMNS = [
  { source: "sys_title", displayName: "Content Title", renderType: "Text" },
  { source: "sys_checkoutstatus", displayName: "Checkout status", renderType: "Image" },
  { source: "sys_statename", displayName: "State", renderType: "Text" },
  { source: "sys_contenttypename", displayName: "Content Type", renderType: "Text" },
  { source: "sys_contentcreatedby", displayName: "Author", renderType: "Text" },
  {
    source: "sys_contentlastmodifieddate",
    displayName: "Last Modified",
    renderType: "Date",
  },
];

test.describe("Developer display format detail readback (#5866)", () => {
  test("packaged By_Author identity, usage, columns, sort, and communities read back", async ({
    page,
  }) => {
    test.setTimeout(180_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);
    await openDisplayFormatsCatalog(page);
    await openFormatDetail(page, "By_Author");

    await expect(page.locator('[data-testid="developer-df-detail-title"]')).toHaveText(
      "By Author",
    );
    await expect(page.locator('[data-testid="developer-df-detail-guid"]')).toHaveText(
      "0-31-5",
    );

    const nameInput = page.locator('[data-testid="developer-df-name"]');
    await expect(nameInput).toHaveValue("By_Author");
    await expect(nameInput).toBeDisabled();
    await expect(page.locator('[data-testid="developer-df-label"]')).toHaveValue(
      "By Author",
    );
    await expect(page.locator('[data-testid="developer-df-description"]')).toHaveValue(
      "Views & Search results by author.",
    );

    const labelText = page.locator('[data-testid="developer-df-set-label-text"]');
    await expect(labelText).toHaveText("By Author");
    await expect(labelText).toHaveAttribute("data-df-label", "By Author");
    const descText = page.locator(
      '[data-testid="developer-df-set-description-text"]',
    );
    await expect(descText).toHaveText("Views & Search results by author.");
    await expect(descText).toHaveAttribute(
      "data-df-description",
      "Views & Search results by author.",
    );

    const headerMeta = page.locator('[data-testid="developer-df-detail"] dl').first();
    await expect(headerMeta).toContainText("Views & searches");

    const rows = page.locator('[data-testid^="developer-df-column-row-"]');
    await expect(rows).toHaveCount(BY_AUTHOR_COLUMNS.length);
    for (let i = 0; i < BY_AUTHOR_COLUMNS.length; i++) {
      const row = page.locator(`[data-testid="developer-df-column-row-${i}"]`);
      await expect(row).toHaveAttribute(
        "data-df-column-source",
        BY_AUTHOR_COLUMNS[i].source,
      );
      await expect(row).toContainText(String(i));
      await expect(row).toContainText(BY_AUTHOR_COLUMNS[i].displayName);
      await expect(row).toContainText(BY_AUTHOR_COLUMNS[i].renderType);
    }

    const sortMarks = page.locator('[data-testid^="developer-df-column-sort-readonly-"]');
    await expect(sortMarks).toHaveCount(1);
    await expect(
      page.locator('[data-testid="developer-df-column-sort-readonly-0"]'),
    ).toContainText("Default");
    const sortDirs = page.locator(
      '[data-testid^="developer-df-column-sort-dir-readonly-"]',
    );
    await expect(sortDirs).toHaveCount(BY_AUTHOR_COLUMNS.length);
    for (let i = 0; i < BY_AUTHOR_COLUMNS.length; i++) {
      await expect(
        page.locator(`[data-testid="developer-df-column-sort-dir-readonly-${i}"]`),
      ).toContainText("Ascending");
    }

    await expect(
      page.locator('[data-testid="developer-df-columns-readonly"]'),
    ).toBeVisible();
    await expect(page.locator('[data-testid="developer-df-column-editor"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-df-columns-save"]')).toHaveCount(0);
    await expect(
      page.locator('[data-testid="developer-df-communities-readonly"]'),
    ).toBeVisible();
    await expect(
      page.locator('[data-testid="developer-df-communities-all-label"]'),
    ).toContainText("All communities");
    await expect(
      page.locator('[data-testid="developer-df-community-editor"]'),
    ).toHaveCount(0);

    assertConsoleClean(pageErrors, consoleErrors);
  });

  test("created user format label, description, and GUID read back in the shell", async ({
    page,
  }) => {
    test.setTimeout(180_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);
    await openDisplayFormatsCatalog(page);

    const formatName = uniqueDisplayFormatName("qa5866");
    expect(formatName.startsWith("qa5866")).toBeTruthy();
    expect(/By_Author|Default/i.test(formatName)).toBeFalsy();
    const label = `${formatName} label`;
    const description = "qa5866 user df detail readback";

    const create = await inPageJson(page, "/Rhythmyx/services/displayformats", "POST", {
      DisplayFormat: {
        name: formatName,
        internalName: formatName,
        label,
        displayName: label,
        description,
      },
    });
    expect(
      create.status,
      `POST create should be 201 (got ${create.status}): ${create.text}`,
    ).toBe(201);
    const created = unwrapDisplayFormat(create.text);
    expect(created.name || created.internalName).toBe(formatName);
    const createdGuid =
      (created.guid && created.guid.stringValue) || created.guidString || "";
    expect(createdGuid, `created guid missing: ${create.text}`).toBeTruthy();

    await openDisplayFormatsCatalog(page);
    // POSTed without columns: the columns section renders the empty state,
    // not the table — identity readback is the assertion here.
    await openFormatDetail(page, formatName, { expectColumnsTable: false });
    await expect(page.locator('[data-testid="developer-df-columns"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(
      page.locator('[data-testid="developer-df-columns-table"]'),
    ).toHaveCount(0);
    await expect(
      page.locator('[data-testid="developer-df-columns-empty"]'),
    ).toBeVisible();

    await expect(page.locator('[data-testid="developer-df-detail-title"]')).toHaveText(
      label,
    );
    await expect(page.locator('[data-testid="developer-df-detail-guid"]')).toHaveText(
      createdGuid,
    );
    await expect(page.locator('[data-testid="developer-df-name"]')).toHaveValue(
      formatName,
    );
    await expect(page.locator('[data-testid="developer-df-label"]')).toHaveValue(label);
    await expect(page.locator('[data-testid="developer-df-description"]')).toHaveValue(
      description,
    );
    await expect(
      page.locator('[data-testid="developer-df-set-label-text"]'),
    ).toHaveText(label);
    await expect(
      page.locator('[data-testid="developer-df-set-description-text"]'),
    ).toHaveText(description);
    // User format is editable: column + community editors are present.
    await expect(page.locator('[data-testid="developer-df-column-editor"]')).toBeVisible();
    await expect(
      page.locator('[data-testid="developer-df-community-editor"]'),
    ).toBeVisible();

    const del = await inPageJson(
      page,
      `/Rhythmyx/services/displayformats/${encodeURIComponent(formatName)}`,
      "DELETE",
    );
    expect(
      del.status,
      `DELETE cleanup should be 204 (got ${del.status}): ${del.text}`,
    ).toBe(204);

    await openDisplayFormatsCatalog(page);
    await expect(
      page.locator(catalogOpenByExactName("developer-df-open", "data-df-name", formatName)),
    ).toHaveCount(0);

    assertConsoleClean(pageErrors, consoleErrors);
  });
});
