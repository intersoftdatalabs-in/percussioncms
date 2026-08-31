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
 * Developer Content Type include system/shared field picker
 * (#4036 CD-04 / parent #1690).
 *
 * Admin locks a type, includes a catalog field, and GET detail keeps origin
 * system/shared. Duplicate and unknown names surface 409 / 400-404.
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… \
 *     npm run test:surface -- --path tests/developer-content-type-include-field.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { catalogRowSelector } = require("./helpers/developer-catalog-selectors");

function developerContentTypesUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "content-types",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

function asRecord(value) {
  return value != null && typeof value === "object" && !Array.isArray(value) ? value : null;
}

function unwrapDetail(payload) {
  const root = asRecord(payload);
  if (!root) {
    return {};
  }
  return (
    asRecord(root.ContentTypeDetail) ||
    asRecord(root.contentTypeDetail) ||
    root
  );
}

function unwrapFields(payload) {
  const body = unwrapDetail(payload);
  const nested =
    asRecord(body.SystemDefDetail) ||
    asRecord(body.systemDefDetail) ||
    asRecord(body.SharedFieldGroupDetail) ||
    asRecord(body.sharedFieldGroupDetail) ||
    body;
  const raw = nested.fields || nested.ContentTypeField || nested.SystemDefFieldSummary || [];
  if (Array.isArray(raw)) {
    return raw;
  }
  return raw ? [raw] : [];
}

function unwrapList(payload, keys) {
  if (Array.isArray(payload)) {
    return payload;
  }
  const root = asRecord(payload);
  if (!root) {
    return [];
  }
  for (const key of keys) {
    if (root[key] != null) {
      return Array.isArray(root[key]) ? root[key] : [root[key]];
    }
  }
  return [];
}

function fieldNames(fields) {
  return new Set(
    (fields || [])
      .map((f) => (f && f.name ? String(f.name).trim().toLowerCase() : ""))
      .filter(Boolean),
  );
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

function expectConsoleClean(pageErrors, consoleErrors) {
  expect(pageErrors, `pageerror: ${pageErrors.join(" | ")}`).toEqual([]);
  const unexpectedConsole = consoleErrors.filter(
    (t) => !/Failed to load resource/i.test(t) && !/favicon/i.test(t),
  );
  expect(
    unexpectedConsole,
    `console error: ${unexpectedConsole.join(" | ")}`,
  ).toEqual([]);
}

async function openContentTypeDetail(page, namePattern) {
  await page.goto(developerContentTypesUrl(), { waitUntil: "networkidle" });

  await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
    timeout: 20_000,
  });

  const panel = page.locator('[data-testid="developer-ct-panel"]');
  const empty = page.locator('[data-testid="developer-ct-empty"]');
  const listError = page.locator('[data-testid="developer-ct-error"]');
  await expect(panel.or(empty).or(listError).first()).toBeVisible({
    timeout: 30_000,
  });

  if (await listError.isVisible()) {
    throw new Error(
      `Developer content types catalog error: ${(await listError.innerText()).trim()}`,
    );
  }
  if (await empty.isVisible()) {
    throw new Error(
      "No content types in catalog — fail closed (H2 QA must include sample types)",
    );
  }

  const table = page.locator('[data-testid="developer-ct-table"]');
  await expect(table).toBeVisible({ timeout: 15_000 });
  const named = table.locator('[data-testid^="developer-ct-row-"]').filter({
    hasText: namePattern || /percPage/,
  });
  const targetRow =
    (await named.count()) > 0
      ? named.first()
      : page.locator(catalogRowSelector("developer-ct-row", 0));
  await expect(targetRow).toBeVisible();
  const openBtn = targetRow.locator('button[aria-label^="Open "]');
  if (await openBtn.count()) {
    await openBtn.click();
  } else {
    await targetRow.click();
  }

  const detail = page.locator('[data-testid="developer-ct-detail"]');
  const detailError = page.locator('[data-testid="developer-ct-detail-error"]');
  await expect(detail.or(detailError).first()).toBeVisible({ timeout: 30_000 });
  if (await detailError.isVisible()) {
    throw new Error(
      `Content type detail error: ${(await detailError.innerText()).trim()}`,
    );
  }
  await expect(page.locator('[data-testid="developer-ct-include"]')).toBeVisible({
    timeout: 30_000,
  });
  return detail;
}

async function unlockIfHeld(page) {
  const unlock = page.locator('[data-testid="developer-ct-unlock"]');
  if ((await unlock.count()) && (await unlock.isEnabled())) {
    await unlock.click();
    await expect(page.locator('[data-testid="developer-ct-lock-status"]')).toHaveText(
      /Not locked/i,
      { timeout: 20_000 },
    );
  }
}

async function lockType(page) {
  const lockBtn = page.locator('[data-testid="developer-ct-lock"]');
  const status = page.locator('[data-testid="developer-ct-lock-status"]');
  const err = page.locator('[data-testid="developer-ct-detail-error"]');
  await lockBtn.click();
  await expect(status.or(err).first()).toBeVisible({ timeout: 20_000 });
  if ((await err.count()) && (await err.isVisible())) {
    throw new Error(`Lock failed: ${(await err.innerText()).trim()}`);
  }
  await expect(status).toHaveText(/Locked by you/i, { timeout: 20_000 });
}

async function getJson(page, path) {
  const res = await page.request.get(`${BASE_URL}/Rhythmyx/services/${path}`);
  expect(res.ok(), `GET ${path} HTTP ${res.status()}`).toBe(true);
  return res.json();
}

async function findUnusedIncludeTarget(page, typeName) {
  const typePayload = await getJson(page, `contenttypes/${encodeURIComponent(typeName)}`);
  const existing = fieldNames(unwrapFields(typePayload));
  const sysPayload = await getJson(page, "systemdef");
  const sysFields = unwrapFields(sysPayload);
  const unusedSys = sysFields.find((f) => {
    const n = f && f.name ? String(f.name).trim() : "";
    return n && !existing.has(n.toLowerCase());
  });
  if (unusedSys && unusedSys.name) {
    return { name: String(unusedSys.name).trim(), origin: "system" };
  }
  const groups = unwrapList(await getJson(page, "sharedfields"), [
    "SharedFieldGroupSummary",
    "sharedFieldGroupSummary",
    "SharedFieldGroup",
    "sharedFieldGroup",
  ]);
  for (const g of groups) {
    const groupName = g && g.name ? String(g.name).trim() : "";
    if (!groupName) {
      continue;
    }
    const groupPayload = await getJson(
      page,
      `sharedfields/${encodeURIComponent(groupName)}`,
    );
    const unusedShared = unwrapFields(groupPayload).find((f) => {
      const n = f && f.name ? String(f.name).trim() : "";
      return n && !existing.has(n.toLowerCase());
    });
    if (unusedShared && unusedShared.name) {
      return { name: String(unusedShared.name).trim(), origin: "shared" };
    }
  }
  return null;
}

test.describe("Developer content type include system/shared field (#4036 CD-04)", () => {
  test("include picker is disabled until lock", async ({ page }) => {
    test.setTimeout(120_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);
    await openContentTypeDetail(page);

    await expect(page.locator('[data-testid="developer-ct-include-origin"]')).toBeDisabled();
    await expect(page.locator('[data-testid="developer-ct-include-name"]')).toBeDisabled();
    await expect(page.locator('[data-testid="developer-ct-include-submit"]')).toBeDisabled();

    expectConsoleClean(pageErrors, consoleErrors);
  });

  test("Admin can include a system or shared field and GET keeps origin", async ({
    page,
  }) => {
    test.setTimeout(180_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);
    await openContentTypeDetail(page, /percFileAsset|percSimpleTextAsset/);

    const typeName = (
      await page.locator('[data-testid="developer-ct-detail-name"]').innerText()
    ).trim();
    expect(typeName.length, "detail name for GET").toBeGreaterThan(0);

    const target = await findUnusedIncludeTarget(page, typeName);
    if (target == null) {
      test.skip(
        true,
        "H2 QA has no unused system or shared catalog field to include",
      );
    }

    try {
      await lockType(page);

      const origin = page.locator('[data-testid="developer-ct-include-origin"]');
      const nameInput = page.locator('[data-testid="developer-ct-include-name"]');
      const submit = page.locator('[data-testid="developer-ct-include-submit"]');
      await expect(origin).toBeEnabled();
      await origin.selectOption(target.origin);
      await nameInput.fill(target.name);
      await expect(submit).toBeEnabled();

      const includeResp = page.waitForResponse(
        (res) =>
          res.url().includes("/services/contenttypes/") &&
          res.url().includes("/fields/include") &&
          res.request().method() === "POST",
        { timeout: 30_000 },
      );
      await submit.click();
      const resp = await includeResp;
      expect(resp.ok(), `include HTTP ${resp.status()}`).toBe(true);

      const notice = page.locator('[data-testid="developer-ct-detail-notice"]');
      const err = page.locator('[data-testid="developer-ct-detail-error"]');
      await expect(notice.or(err).first()).toBeVisible({ timeout: 20_000 });
      if (await err.isVisible()) {
        throw new Error(`Include failed: ${(await err.innerText()).trim()}`);
      }
      await expect(notice).toContainText(/included/i);

      const originCell = page.locator(
        `[data-testid="developer-ct-field-origin-${target.name}"]`,
      );
      await expect(originCell).toHaveText(new RegExp(target.origin, "i"));

      const after = unwrapFields(
        await getJson(page, `contenttypes/${encodeURIComponent(typeName)}`),
      );
      const included = after.find(
        (f) => f && String(f.name || "").toLowerCase() === target.name.toLowerCase(),
      );
      expect(included, `GET detail lists ${target.name}`).toBeTruthy();
      expect(String(included.fieldType || "").toLowerCase()).toBe(target.origin);
    } finally {
      await unlockIfHeld(page);
    }
    expectConsoleClean(pageErrors, consoleErrors);
  });

  test("duplicate include surfaces 409 and unknown field surfaces 400/404", async ({
    page,
  }) => {
    test.setTimeout(180_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);
    await openContentTypeDetail(page, /percFileAsset|percSimpleTextAsset/);

    const typeName = (
      await page.locator('[data-testid="developer-ct-detail-name"]').innerText()
    ).trim();
    const existing = unwrapFields(
      await getJson(page, `contenttypes/${encodeURIComponent(typeName)}`),
    ).find((f) => f && f.name && /system|shared/i.test(String(f.fieldType || "")));
    expect(existing, "type must already include a system or shared field").toBeTruthy();

    try {
      await lockType(page);

      const origin = page.locator('[data-testid="developer-ct-include-origin"]');
      const nameInput = page.locator('[data-testid="developer-ct-include-name"]');
      const submit = page.locator('[data-testid="developer-ct-include-submit"]');
      const err = page.locator('[data-testid="developer-ct-detail-error"]');

      const existingOrigin =
        String(existing.fieldType).toLowerCase() === "shared" ? "shared" : "system";
      await origin.selectOption(existingOrigin);
      await nameInput.fill(String(existing.name));
      const dupResp = page.waitForResponse(
        (res) =>
          res.url().includes("/fields/include") && res.request().method() === "POST",
        { timeout: 30_000 },
      );
      await submit.click();
      expect((await dupResp).status()).toBe(409);
      await expect(err).toBeVisible({ timeout: 20_000 });
      await expect(err).toContainText(/include/i);
      await expect(page.locator('[data-testid="developer-ct-lock-status"]')).toHaveText(
        /Locked by you/i,
      );

      await origin.selectOption("system");
      await nameInput.fill("nope_field_4036");
      await expect(submit).toBeEnabled();
      const missResp = page.waitForResponse(
        (res) =>
          res.url().includes("/fields/include") && res.request().method() === "POST",
        { timeout: 30_000 },
      );
      await submit.click();
      const missStatus = (await missResp).status();
      expect([400, 404]).toContain(missStatus);
      await expect(err).toBeVisible({ timeout: 20_000 });
      await expect(err).toContainText(/include/i);
    } finally {
      await unlockIfHeld(page);
    }
    expectConsoleClean(pageErrors, consoleErrors);
  });
});
