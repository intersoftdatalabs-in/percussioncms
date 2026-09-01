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
 * Developer Display Format allowed-community write (#4098 UI-05 / parent #1690).
 *
 * Admin restricts a uniquely named user format via SPA PUT allowedCommunities;
 * GET detail matches. Empty / All communities is one persist state. Packaged
 * formats stay read-only.
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=…
 *     npm run test:surface -- --path tests/developer-display-format-communities.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { catalogOpenByExactName } = require("./helpers/developer-catalog-selectors");

function developerDisplayFormatsUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "display-formats",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
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

function uniqueDisplayFormatName(prefix) {
  const a = Date.now().toString(36).replace(/[^a-z0-9]/g, "").slice(-4);
  const b = Math.random().toString(36).replace(/[^a-z0-9]/g, "").slice(2, 6);
  const suffix = `${a}${b}`.slice(0, 8);
  return `${prefix}${suffix || "x"}`;
}

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
  return parsed.DisplayFormat || parsed;
}

function communityRowKey(row) {
  if (!row || typeof row !== "object") {
    return typeof row === "string" ? row : "";
  }
  if (row.guid) return String(row.guid);
  if (row.name) return String(row.name);
  if (row.key && row.key.stringValue) return String(row.key.stringValue);
  if (row.stringValue) return String(row.stringValue);
  return "";
}

function allowedCommunityKeys(detail) {
  const raw = detail && detail.allowedCommunities;
  if (raw == null || raw === "") {
    return [];
  }
  if (Array.isArray(raw)) {
    return raw.map(communityRowKey).filter(Boolean);
  }
  if (typeof raw === "object") {
    if (raw.guid || raw.name) {
      const key = communityRowKey(raw);
      return key ? [key] : [];
    }
    const jaxb = raw.DisplayFormatCommunity || raw.displayFormatCommunity;
    if (jaxb != null) {
      return allowedCommunityKeys({ allowedCommunities: jaxb });
    }
    return Object.keys(raw).filter(
      (k) => k && k !== "entry" && k !== "Entry" && k !== "guid" && k !== "name",
    );
  }
  return [];
}

async function saveCommunitiesAndAssertOk(page) {
  await page.locator('[data-testid="developer-df-communities-save"]').click();
  const saveNotice = page.locator('[data-testid="developer-df-editor-notice"]');
  const saveError = page.locator('[data-testid="developer-df-detail-error"]');
  await expect(saveNotice.or(saveError).first()).toBeVisible({ timeout: 20_000 });
  if (await saveError.isVisible()) {
    throw new Error(`Save communities failed: ${(await saveError.innerText()).trim()}`);
  }
}

test.describe("Developer display format communities (#4098 / UI-05)", () => {
  test("packaged By_Author format is read-only for communities", async ({ page }) => {
    test.setTimeout(120_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);
    await openDisplayFormatsCatalog(page);

    await page
      .locator(catalogOpenByExactName("developer-df-open", "data-df-name", "By_Author"))
      .click();
    await expect(page.locator('[data-testid="developer-df-detail"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.locator('[data-testid="developer-df-communities-readonly"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.locator('[data-testid="developer-df-community-editor"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-df-communities-save"]')).toHaveCount(0);

    assertConsoleClean(pageErrors, consoleErrors);
  });

  test("Admin sets allowed communities on a user DF; empty/all is one persist state", async ({
    page,
  }) => {
    test.setTimeout(180_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);
    await openDisplayFormatsCatalog(page);

    const formatName = uniqueDisplayFormatName("qa4098");
    expect(formatName.startsWith("qa4098")).toBeTruthy();
    expect(/By_Author|Default/i.test(formatName)).toBeFalsy();

    const create = await inPageJson(page, "/Rhythmyx/services/displayformats", "POST", {
      DisplayFormat: {
        name: formatName,
        internalName: formatName,
        label: `${formatName} label`,
        displayName: `${formatName} label`,
        description: "qa4098 allowed communities",
      },
    });
    expect(
      create.status,
      `POST create should be 201 (got ${create.status}): ${create.text}`,
    ).toBe(201);

    await openDisplayFormatsCatalog(page);
    const createdOpen = page.locator(
      catalogOpenByExactName("developer-df-open", "data-df-name", formatName),
    );
    await expect(createdOpen).toHaveCount(1, { timeout: 20_000 });

    const restrictPut = await inPageJson(
      page,
      `/Rhythmyx/services/displayformats/${encodeURIComponent(formatName)}`,
      "PUT",
      {
        DisplayFormat: {
          name: formatName,
          label: `${formatName} label`,
          displayName: `${formatName} label`,
          allowedCommunities: [{ guid: "Default", name: "Default" }],
        },
      },
    );
    expect(
      restrictPut.status,
      `PUT allowedCommunities should be 200 (got ${restrictPut.status}): ${restrictPut.text}`,
    ).toBe(200);
    expect(
      restrictPut.text,
      `PUT response should echo restricted communities: ${restrictPut.text}`,
    ).toMatch(/Default/i);

    const afterRestrict = await inPageJson(
      page,
      `/Rhythmyx/services/displayformats/${encodeURIComponent(formatName)}`,
      "GET",
    );
    expect(
      afterRestrict.status,
      `GET after restrict (got ${afterRestrict.status}): ${afterRestrict.text}`,
    ).toBe(200);
    const restricted = unwrapDisplayFormat(afterRestrict.text);
    expect(restricted.name || restricted.internalName).toBe(formatName);
    const restrictedKeys = allowedCommunityKeys(restricted);
    expect(
      restrictedKeys.length,
      `restricted GET should list communities, got ${afterRestrict.text}`,
    ).toBeGreaterThan(0);
    expect(restrictedKeys.join(" ")).toMatch(/Default|0-\d+-\d+/i);

    await createdOpen.click();
    await expect(page.locator('[data-testid="developer-df-detail"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.locator('[data-testid="developer-df-community-editor"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-df-communities-readonly"]')).toHaveCount(0);

    const communityBox = page.locator('[data-testid="developer-df-community-Default"]');
    await expect(communityBox).toBeVisible({ timeout: 20_000 });
    const allBox = page.locator('[data-testid="developer-df-communities-all"]');
    await expect(communityBox).toBeChecked({ timeout: 10_000 });
    await expect(allBox).not.toBeChecked();
    await allBox.check();
    await expect(allBox).toBeChecked();
    await expect(page.locator('[data-testid="developer-df-communities-save"]')).toBeEnabled();
    await saveCommunitiesAndAssertOk(page);

    const afterAll = await inPageJson(
      page,
      `/Rhythmyx/services/displayformats/${encodeURIComponent(formatName)}`,
      "GET",
    );
    expect(afterAll.status, `GET after all-communities (got ${afterAll.status}): ${afterAll.text}`).toBe(
      200,
    );
    const cleared = unwrapDisplayFormat(afterAll.text);
    expect(allowedCommunityKeys(cleared)).toEqual([]);

    assertConsoleClean(pageErrors, consoleErrors);
  });
});
