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
 * Developer Content Type icon strategy (CD-11 / #4047 / parent #1690).
 *
 * Admin locks a type, sets specified / fromFileField / none via PUT .../icon,
 * then GET round-trips. Unlocked chrome stays disabled; blank non-none is 400;
 * unlocked PUT is 409. No binary upload.
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… \
 *     npm run test:surface -- --path tests/developer-content-type-icon-strategy.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { catalogRowSelector } = require("./helpers/developer-catalog-selectors");

const SPECIFIED_VALUE = "rx_resources/images/ContentTypeIcons/page.gif";
const FROM_FILE_FIELD_VALUE = "item_file_attachment";

function developerContentTypesUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "content-types",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
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
  expect(unexpectedConsole, `console error: ${unexpectedConsole.join(" | ")}`).toEqual([]);
}

function unwrapIcon(payload) {
  if (payload == null || typeof payload !== "object") {
    return { source: "none", value: "" };
  }
  const nested =
    payload.ContentTypeIcon && typeof payload.ContentTypeIcon === "object"
      ? payload.ContentTypeIcon
      : payload.contentTypeIcon && typeof payload.contentTypeIcon === "object"
        ? payload.contentTypeIcon
        : payload;
  const source = String(nested.source || "none").trim() || "none";
  const value = nested.value == null ? "" : String(nested.value).trim();
  if (source.toLowerCase() === "none") {
    return { source: "none", value: "" };
  }
  return { source, value };
}

function iconUrl(typeName) {
  return `${BASE_URL}/Rhythmyx/services/contenttypes/${encodeURIComponent(typeName)}/icon`;
}

async function getIconViaRest(page, typeName) {
  const result = await page.evaluate(async (name) => {
    const res = await fetch(
      `/Rhythmyx/services/contenttypes/${encodeURIComponent(name)}/icon`,
      { credentials: "same-origin", headers: { Accept: "application/json" } },
    );
    const text = await res.text();
    return { status: res.status, text };
  }, typeName);
  expect(
    result.status,
    `GET icon ${result.status} ${String(result.text).slice(0, 400)}`,
  ).toBe(200);
  let json = {};
  try {
    json = result.text ? JSON.parse(result.text) : {};
  } catch {
    throw new Error(`GET icon non-JSON: ${String(result.text).slice(0, 400)}`);
  }
  return unwrapIcon(json);
}

async function putIconViaRest(page, typeName, source, value) {
  const body =
    source === "none"
      ? { ContentTypeIcon: { source: "none" } }
      : { ContentTypeIcon: { source, value } };
  const res = await page.request.put(iconUrl(typeName), {
    headers: {
      Accept: "application/json",
      "Content-Type": "application/json",
    },
    data: body,
  });
  return { status: res.status(), text: await res.text() };
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
  await expect(page.locator('[data-testid="developer-ct-lock-toolbar"]')).toBeVisible({
    timeout: 30_000,
  });
  await expect(page.locator('[data-testid="developer-ct-icon"]')).toBeVisible({
    timeout: 30_000,
  });
  await expect(page.locator('[data-testid="developer-ct-lock"]')).toBeEnabled({
    timeout: 30_000,
  });
  await expect(page.locator('[data-testid="developer-ct-icon-source"]')).toBeDisabled({
    timeout: 15_000,
  });
  return detail;
}

async function saveAndExpectOk(page) {
  const saveBtn = page.locator('[data-testid="developer-ct-save"]');
  await expect(saveBtn).toBeEnabled();
  await saveBtn.click();
  const notice = page.locator('[data-testid="developer-ct-detail-notice"]');
  const saveError = page.locator('[data-testid="developer-ct-detail-error"]');
  await expect(notice.or(saveError).first()).toBeVisible({ timeout: 20_000 });
  if (await saveError.isVisible()) {
    throw new Error(`Save failed: ${(await saveError.innerText()).trim()}`);
  }
  await expect(notice).toContainText(/saved/i);
}

test.describe("Developer content type icon strategy (CD-11 / #4047)", () => {
  test("unlocked icon editors stay disabled; 409 lock is not stolen", async ({ page }) => {
    test.setTimeout(120_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);
    await openContentTypeDetail(page);

    const source = page.locator('[data-testid="developer-ct-icon-source"]');
    const value = page.locator('[data-testid="developer-ct-icon-value"]');
    const saveBtn = page.locator('[data-testid="developer-ct-save"]');
    const lockBtn = page.locator('[data-testid="developer-ct-lock"]');
    const status = page.locator('[data-testid="developer-ct-lock-status"]');

    await expect(source).toBeDisabled();
    await expect(value).toBeDisabled();
    await expect(saveBtn).toBeDisabled();
    await expect(status).toHaveText(/Not locked/i);

    await page.route("**/services/contenttypes/**/lock", async (route) => {
      await route.fulfill({
        status: 409,
        contentType: "application/json",
        body: JSON.stringify({ message: "Locked by another user" }),
      });
    });

    await lockBtn.click();
    await expect(page.locator('[data-testid="developer-ct-detail-error"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(status).toHaveText(/Not locked/i);
    await expect(source).toBeDisabled();
    await expect(saveBtn).toBeDisabled();

    assertConsoleClean(pageErrors, consoleErrors);
  });

  test("unlocked PUT .../icon is 409; lock is not stolen", async ({ page }) => {
    test.setTimeout(120_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);
    await openContentTypeDetail(page);
    const typeName = (
      await page.locator('[data-testid="developer-ct-detail-name"]').innerText()
    ).trim();
    expect(typeName.length, "content type name from detail").toBeGreaterThan(0);

    const put = await putIconViaRest(page, typeName, "specified", SPECIFIED_VALUE);
    expect(put.status, `unlocked PUT ${put.status} ${put.text}`).toBe(409);

    assertConsoleClean(pageErrors, consoleErrors);
  });

  test("blank non-none save is 400 and does not persist", async ({ page }) => {
    test.setTimeout(120_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);
    await openContentTypeDetail(page);
    const typeName = (
      await page.locator('[data-testid="developer-ct-detail-name"]').innerText()
    ).trim();
    const before = await getIconViaRest(page, typeName);

    const source = page.locator('[data-testid="developer-ct-icon-source"]');
    const value = page.locator('[data-testid="developer-ct-icon-value"]');
    const lockBtn = page.locator('[data-testid="developer-ct-lock"]');
    const saveBtn = page.locator('[data-testid="developer-ct-save"]');
    const status = page.locator('[data-testid="developer-ct-lock-status"]');

    await lockBtn.click();
    await expect(status).toHaveText(/Locked by you/i, { timeout: 20_000 });
    await expect(source).toBeEnabled();

    await source.selectOption("specified");
    await value.fill("");
    await expect(saveBtn).toBeEnabled();
    await saveBtn.click();
    await expect(page.locator('[data-testid="developer-ct-detail-error"]')).toContainText(
      /value is required when source is not none/i,
      { timeout: 20_000 },
    );
    await expect(status).toHaveText(/Locked by you/i);

    const after = await getIconViaRest(page, typeName);
    expect(after).toEqual(before);

    await page.locator('[data-testid="developer-ct-unlock"]').click();
    await expect(status).toHaveText(/Not locked/i, { timeout: 20_000 });

    assertConsoleClean(pageErrors, consoleErrors);
  });

  test("Admin lock, set specified / fromFileField / none, GET round-trip, unlock", async ({
    page,
  }) => {
    test.setTimeout(180_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);
    await openContentTypeDetail(page);
    const typeName = (
      await page.locator('[data-testid="developer-ct-detail-name"]').innerText()
    ).trim();
    const original = await getIconViaRest(page, typeName);

    const source = page.locator('[data-testid="developer-ct-icon-source"]');
    const value = page.locator('[data-testid="developer-ct-icon-value"]');
    const lockBtn = page.locator('[data-testid="developer-ct-lock"]');
    const unlockBtn = page.locator('[data-testid="developer-ct-unlock"]');
    const status = page.locator('[data-testid="developer-ct-lock-status"]');

    await lockBtn.click();
    await expect(status).toHaveText(/Locked by you/i, { timeout: 20_000 });
    await expect(source).toBeEnabled();

    try {
      await source.selectOption("specified");
      await value.fill(SPECIFIED_VALUE);
      await saveAndExpectOk(page);
      expect(await getIconViaRest(page, typeName)).toEqual({
        source: "specified",
        value: SPECIFIED_VALUE,
      });

      await source.selectOption("fromFileField");
      await value.fill(FROM_FILE_FIELD_VALUE);
      await saveAndExpectOk(page);
      expect(await getIconViaRest(page, typeName)).toEqual({
        source: "fromFileField",
        value: FROM_FILE_FIELD_VALUE,
      });

      await source.selectOption("none");
      await expect(value).toBeDisabled();
      await saveAndExpectOk(page);
      expect(await getIconViaRest(page, typeName)).toEqual({
        source: "none",
        value: "",
      });

      if (original.source !== "none") {
        await source.selectOption(original.source);
        await value.fill(original.value);
        await saveAndExpectOk(page);
      } else if ((await getIconViaRest(page, typeName)).source !== "none") {
        await source.selectOption("none");
        await saveAndExpectOk(page);
      }
    } finally {
      if (await unlockBtn.isEnabled()) {
        await unlockBtn.click();
        await expect(status).toHaveText(/Not locked/i, { timeout: 20_000 });
      }
    }

    assertConsoleClean(pageErrors, consoleErrors);
  });
});
