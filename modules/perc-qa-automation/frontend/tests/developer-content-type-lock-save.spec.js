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
 * Developer Content Type detail lock / save / unlock / enable chrome
 * (#3744 / #3772 / #3781 CD-13 / parent #1690).
 *
 * Admin locks a type, saves a description, toggles enabled via dedicated PUT,
 * then unlocks. Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… \
 *     npm run test:surface -- --path tests/developer-content-type-lock-save.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { catalogRowSelector } = require("./helpers/developer-catalog-selectors");

const MARKER = " [#3744-lock-save]";

function developerContentTypesUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "content-types",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

function unwrapEnabled(payload) {
  if (payload == null || typeof payload !== "object") {
    return undefined;
  }
  const nested =
    payload.ContentTypeDetail && typeof payload.ContentTypeDetail === "object"
      ? payload.ContentTypeDetail
      : payload.contentTypeDetail && typeof payload.contentTypeDetail === "object"
        ? payload.contentTypeDetail
        : payload;
  return nested.enabled;
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
  return detail;
}

async function getContentTypeEnabled(page, idOrName) {
  const res = await page.request.get(
    `${BASE_URL}/Rhythmyx/services/contenttypes/${encodeURIComponent(idOrName)}`,
  );
  expect(res.ok(), `GET content type HTTP ${res.status()}`).toBe(true);
  return unwrapEnabled(await res.json());
}

test.describe("Developer content type lock/save chrome (#3744 / #3772 / #3781)", () => {
  test("Admin can lock, save a description, and unlock", async ({ page }) => {
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
    await openContentTypeDetail(page);

    const desc = page.locator('[data-testid="developer-ct-description"]');
    const enabledBox = page.locator('[data-testid="developer-ct-enabled"]');
    const lockBtn = page.locator('[data-testid="developer-ct-lock"]');
    const saveBtn = page.locator('[data-testid="developer-ct-save"]');
    const unlockBtn = page.locator('[data-testid="developer-ct-unlock"]');
    const status = page.locator('[data-testid="developer-ct-lock-status"]');

    await expect(page.locator('[data-testid="developer-ct-lock-toolbar"]')).toBeVisible();
    await expect(lockBtn).toBeEnabled();
    await expect(saveBtn).toBeDisabled();
    await expect(unlockBtn).toBeDisabled();
    await expect(desc).toBeDisabled();
    await expect(enabledBox).toBeDisabled();
    await expect(status).toHaveText(/Not locked/i);

    await lockBtn.click();
    await expect(status).toHaveText(/Locked by you/i, { timeout: 20_000 });
    await expect(desc).toBeEnabled();
    await expect(enabledBox).toBeEnabled();
    await expect(unlockBtn).toBeEnabled();

    const original = (await desc.inputValue()).replace(MARKER, "");
    await desc.fill(`${original}${MARKER}`);
    await expect(saveBtn).toBeEnabled();
    await saveBtn.click();
    const notice = page.locator('[data-testid="developer-ct-detail-notice"]');
    const saveError = page.locator('[data-testid="developer-ct-detail-error"]');
    await expect(notice.or(saveError).first()).toBeVisible({ timeout: 20_000 });
    if (await saveError.isVisible()) {
      throw new Error(`Save failed: ${(await saveError.innerText()).trim()}`);
    }
    await expect(notice).toContainText(/saved/i);

    await desc.fill(original);
    await saveBtn.click();
    await expect(notice.or(saveError).first()).toBeVisible({ timeout: 20_000 });
    if (await saveError.isVisible()) {
      throw new Error(`Restore save failed: ${(await saveError.innerText()).trim()}`);
    }
    await expect(notice).toContainText(/saved/i);

    await unlockBtn.click();
    await expect(status).toHaveText(/Not locked/i, { timeout: 20_000 });
    await expect(desc).toBeDisabled();
    await expect(enabledBox).toBeDisabled();
    await expect(saveBtn).toBeDisabled();
    await expect(lockBtn).toBeEnabled();

    expect(pageErrors, `pageerror: ${pageErrors.join(" | ")}`).toEqual([]);
    const unexpectedConsole = consoleErrors.filter(
      (t) => !/Failed to load resource/i.test(t) && !/favicon/i.test(t),
    );
    expect(
      unexpectedConsole,
      `console error: ${unexpectedConsole.join(" | ")}`,
    ).toEqual([]);
  });

  test("Admin lock then toggle enabled persists on GET (#3781 CD-13)", async ({
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
    await openContentTypeDetail(
      page,
      /percFileAsset|percSimpleTextAsset|percRawHtmlAsset/,
    );

    const enabledBox = page.locator('[data-testid="developer-ct-enabled"]');
    const lockBtn = page.locator('[data-testid="developer-ct-lock"]');
    const saveBtn = page.locator('[data-testid="developer-ct-save"]');
    const unlockBtn = page.locator('[data-testid="developer-ct-unlock"]');
    const status = page.locator('[data-testid="developer-ct-lock-status"]');
    const notice = page.locator('[data-testid="developer-ct-detail-notice"]');
    const saveError = page.locator('[data-testid="developer-ct-detail-error"]');

    await expect(enabledBox).toBeDisabled();
    await expect(saveBtn).toBeDisabled();

    await lockBtn.click();
    await expect(status).toHaveText(/Locked by you/i, { timeout: 20_000 });
    await expect(enabledBox).toBeEnabled();

    const originalEnabled = await enabledBox.isChecked();
    const typeName = (
      await page.locator('[data-testid="developer-ct-detail-name"]').innerText()
    ).trim();
    expect(typeName.length, "detail name for GET").toBeGreaterThan(0);

    const saveEnabled = async (label) => {
      const enabledResp = page.waitForResponse(
        (res) =>
          res.request().method() === "PUT" &&
          /\/contenttypes\/[^/?]+\/enabled(?:\?|$)/.test(res.url()),
        { timeout: 20_000 },
      );
      await expect(saveBtn).toBeEnabled();
      await saveBtn.click();
      const resp = await enabledResp;
      const reqBody = resp.request().postData() || "";
      let putEnabled;
      let putBody = "";
      try {
        const putJson = await resp.json();
        putEnabled = unwrapEnabled(putJson);
        putBody = `req=${reqBody} respEnabled=${putEnabled}`;
      } catch (e) {
        putBody = `non-json ${resp.status()} req=${reqBody} ${(e && e.message) || e}`;
      }
      await expect(notice.or(saveError).first()).toBeVisible({ timeout: 20_000 });
      if (await saveError.isVisible()) {
        throw new Error(
          `${label}: ${(await saveError.innerText()).trim()} PUT ${resp.status()} ${putBody}`,
        );
      }
      await expect(notice).toContainText(/saved/i);
      return { status: resp.status(), putBody };
    };

    try {
      await enabledBox.click();
      expect(await enabledBox.isChecked()).toBe(!originalEnabled);
      const putResult = await saveEnabled("Enable save failed");
      const getRes = await page.request.get(
        `${BASE_URL}/Rhythmyx/services/contenttypes/${encodeURIComponent(typeName)}`,
      );
      const getJson = await getRes.json();
      const afterToggle = unwrapEnabled(getJson);
      expect(putResult.status, putResult.putBody).toBe(200);
      expect(putResult.putBody).toContain("ContentTypeEnabled");
      expect(
        afterToggle,
        `GET enabled after toggle ${putResult.putBody} getEnabled=${afterToggle}`,
      ).toBe(!originalEnabled);

      await enabledBox.click();
      await saveEnabled("Enable restore failed");
      const restored = await getContentTypeEnabled(page, typeName);
      expect(restored, "GET enabled after restore").toBe(originalEnabled);
    } finally {
      if (await enabledBox.isEnabled()) {
        if ((await enabledBox.isChecked()) !== originalEnabled) {
          await enabledBox.click();
          try {
            await saveEnabled("Enable finally restore failed");
          } catch {
            // Best-effort restore so a failed assert does not leave the type disabled.
          }
        }
        if (await unlockBtn.isEnabled()) {
          await unlockBtn.click();
          await expect(status).toHaveText(/Not locked/i, { timeout: 20_000 });
        }
      }
    }

    await expect(enabledBox).toBeDisabled();

    expect(pageErrors, `pageerror: ${pageErrors.join(" | ")}`).toEqual([]);
    const unexpectedConsole = consoleErrors.filter(
      (t) => !/Failed to load resource/i.test(t) && !/favicon/i.test(t),
    );
    expect(
      unexpectedConsole,
      `console error: ${unexpectedConsole.join(" | ")}`,
    ).toEqual([]);
  });

  test("other-user lock is 409; enabled toggle stays disabled (#3781)", async ({
    page,
  }) => {
    test.setTimeout(120_000);
    await loginAsAdmin(page);
    await openContentTypeDetail(page);

    await page.route("**/services/contenttypes/**/lock", async (route) => {
      await route.fulfill({
        status: 409,
        contentType: "application/json",
        body: JSON.stringify({ message: "Locked by another user" }),
      });
    });

    const enabledBox = page.locator('[data-testid="developer-ct-enabled"]');
    const lockBtn = page.locator('[data-testid="developer-ct-lock"]');
    const saveBtn = page.locator('[data-testid="developer-ct-save"]');
    const status = page.locator('[data-testid="developer-ct-lock-status"]');

    await expect(enabledBox).toBeDisabled();
    await lockBtn.click();
    await expect(page.locator('[data-testid="developer-ct-detail-error"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(status).toHaveText(/Not locked/i);
    await expect(enabledBox).toBeDisabled();
    await expect(saveBtn).toBeDisabled();
  });
});
