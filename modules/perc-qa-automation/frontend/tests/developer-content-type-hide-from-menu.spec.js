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
 * Developer Content Type detail hide-from-menu chrome
 * (#6935 CD-01 meta write / parent #1690).
 *
 * Admin locks a type, toggles Hide from menu via dedicated PUT
 * .../hideFromMenu (held lock), then unlocks. Unlocked PUT is 409.
 * GET .../{idOrName} reflects the flag after a successful PUT.
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… \
 *     npm run test:surface -- --path tests/developer-content-type-hide-from-menu.spec.js
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

function unwrapDetail(payload) {
  if (payload == null || typeof payload !== "object") {
    return undefined;
  }
  const nested =
    payload.ContentTypeDetail && typeof payload.ContentTypeDetail === "object"
      ? payload.ContentTypeDetail
      : payload.contentTypeDetail && typeof payload.contentTypeDetail === "object"
        ? payload.contentTypeDetail
        : payload;
  return nested;
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
  await expect(page.locator('[data-testid="developer-ct-lock"]')).toBeEnabled({
    timeout: 30_000,
  });
  await expect(page.locator('[data-testid="developer-ct-hide-from-menu"]')).toBeDisabled({
    timeout: 15_000,
  });
  return detail;
}

async function getContentTypeHideFromMenu(page, idOrName) {
  const res = await page.request.get(
    `${BASE_URL}/Rhythmyx/services/contenttypes/${encodeURIComponent(idOrName)}`,
  );
  expect(res.ok(), `GET detail HTTP ${res.status()}`).toBe(true);
  return unwrapDetail(await res.json()).hideFromMenu === true;
}

test.describe("Developer content type hide-from-menu chrome (#6935)", () => {
  test("hide-from-menu stays disabled until lock; unlocked click does not PUT", async ({
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

    const putUrls = [];
    page.on("request", (req) => {
      if (
        req.method() === "PUT" &&
        /\/contenttypes\/[^/?]+\/hideFromMenu(?:\?|$)/.test(req.url())
      ) {
        putUrls.push(req.url());
      }
    });

    await loginAsAdmin(page);
    await openContentTypeDetail(page);

    const box = page.locator('[data-testid="developer-ct-hide-from-menu"]');
    const saveBtn = page.locator('[data-testid="developer-ct-save"]');
    const status = page.locator('[data-testid="developer-ct-lock-status"]');

    await expect(box).toBeDisabled();
    await expect(saveBtn).toBeDisabled();
    await expect(status).toHaveText(/Not locked/i);
    // Native disabled guard — do not use force:true (that bypasses the browser).
    await expect(box.click({ timeout: 2_000 })).rejects.toThrow(/not enabled/i);
    await expect(saveBtn).toBeDisabled();
    expect(putUrls, "unlocked hideFromMenu PUT").toEqual([]);

    expect(pageErrors, `pageerror: ${pageErrors.join(" | ")}`).toEqual([]);
    const unexpectedConsole = consoleErrors.filter(
      (t) => !/Failed to load resource/i.test(t) && !/favicon/i.test(t),
    );
    expect(
      unexpectedConsole,
      `console error: ${unexpectedConsole.join(" | ")}`,
    ).toEqual([]);
  });

  test("Admin lock then toggle hide-from-menu persists on GET detail (#6935)", async ({
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

    const box = page.locator('[data-testid="developer-ct-hide-from-menu"]');
    const lockBtn = page.locator('[data-testid="developer-ct-lock"]');
    const saveBtn = page.locator('[data-testid="developer-ct-save"]');
    const unlockBtn = page.locator('[data-testid="developer-ct-unlock"]');
    const status = page.locator('[data-testid="developer-ct-lock-status"]');
    const notice = page.locator('[data-testid="developer-ct-detail-notice"]');
    const saveError = page.locator('[data-testid="developer-ct-detail-error"]');

    await expect(box).toBeDisabled();
    await expect(saveBtn).toBeDisabled();

    await lockBtn.click();
    await expect(status).toHaveText(/Locked by you/i, { timeout: 20_000 });
    await expect(box).toBeEnabled();

    const original = await box.isChecked();
    const typeName = (
      await page.locator('[data-testid="developer-ct-detail-name"]').innerText()
    ).trim();
    expect(typeName.length, "detail name for GET").toBeGreaterThan(0);

    const saveHideFromMenu = async (label) => {
      const hideResp = page.waitForResponse(
        (res) =>
          res.request().method() === "PUT" &&
          /\/contenttypes\/[^/?]+\/hideFromMenu(?:\?|$)/.test(res.url()),
        { timeout: 20_000 },
      );
      await expect(saveBtn).toBeEnabled();
      await saveBtn.click();
      const resp = await hideResp;
      const reqBody = resp.request().postData() || "";
      let putFlag;
      let putBody = "";
      try {
        const putJson = unwrapDetail(await resp.json());
        putFlag = putJson && putJson.hideFromMenu === true;
        putBody = `req=${reqBody} respHideFromMenu=${putFlag}`;
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

    let restoreErr;
    try {
      await box.click();
      expect(await box.isChecked()).toBe(!original);
      const putResult = await saveHideFromMenu("Hide-from-menu save failed");
      const afterToggle = await getContentTypeHideFromMenu(page, typeName);
      expect(putResult.status, putResult.putBody).toBe(200);
      expect(putResult.putBody).toContain("ContentTypeHideFromMenu");
      expect(
        afterToggle,
        `GET detail hideFromMenu after toggle ${putResult.putBody} get=${afterToggle}`,
      ).toBe(!original);

      // Live proof at the assertion point (C5): toggled chrome on the passing run.
      await page.screenshot({
        path: "test-results/live-proof-6935-hide-from-menu.png",
      });

      await box.click();
      await saveHideFromMenu("Hide-from-menu restore failed");
      const restored = await getContentTypeHideFromMenu(page, typeName);
      expect(restored, "GET detail hideFromMenu after restore").toBe(original);
    } catch (e) {
      restoreErr = e;
    } finally {
      try {
        if (await box.isEnabled()) {
          if ((await box.isChecked()) !== original) {
            await box.click();
            await saveHideFromMenu("Hide-from-menu finally restore failed");
          }
        }
      } catch (e) {
        restoreErr = restoreErr || e;
      }
      try {
        if (await unlockBtn.isEnabled()) {
          await unlockBtn.click();
          await expect(status).toHaveText(/Not locked/i, { timeout: 20_000 });
        }
      } catch (e) {
        restoreErr = restoreErr || e;
      }
    }
    if (restoreErr) {
      throw restoreErr;
    }

    await expect(box).toBeDisabled();

    expect(pageErrors, `pageerror: ${pageErrors.join(" | ")}`).toEqual([]);
    const unexpectedConsole = consoleErrors.filter(
      (t) => !/Failed to load resource/i.test(t) && !/favicon/i.test(t),
    );
    expect(
      unexpectedConsole,
      `console error: ${unexpectedConsole.join(" | ")}`,
    ).toEqual([]);
  });

  test("unlocked or lost lock PUT 409 is surfaced; checkbox stays disabled", async ({
    page,
  }) => {
    test.setTimeout(120_000);
    await loginAsAdmin(page);
    await openContentTypeDetail(page);

    await page.route("**/services/contenttypes/**/hideFromMenu", async (route) => {
      if (route.request().method() === "PUT") {
        await route.fulfill({
          status: 409,
          contentType: "application/json",
          body: JSON.stringify({ message: "Design lock required" }),
        });
        return;
      }
      await route.continue();
    });

    const box = page.locator('[data-testid="developer-ct-hide-from-menu"]');
    const lockBtn = page.locator('[data-testid="developer-ct-lock"]');
    const saveBtn = page.locator('[data-testid="developer-ct-save"]');
    const status = page.locator('[data-testid="developer-ct-lock-status"]');

    await expect(box).toBeDisabled();
    await lockBtn.click();
    await expect(status).toHaveText(/Locked by you/i, { timeout: 20_000 });
    await expect(box).toBeEnabled();
    await box.click();
    await expect(saveBtn).toBeEnabled();
    await saveBtn.click();
    await expect(page.locator('[data-testid="developer-ct-detail-error"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(status).toHaveText(/Not locked/i);
    await expect(box).toBeDisabled();
    await expect(saveBtn).toBeDisabled();
  });
});
