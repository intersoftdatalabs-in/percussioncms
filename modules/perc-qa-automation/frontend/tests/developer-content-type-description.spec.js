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
 * Developer Content Types — set one content type description (#5433 / #1690).
 *
 * The new description shows only after the existing content-type update
 * succeeds. The update sends the description only, so the label and the
 * enabled flag stay. A blank description clears it. Cancel does not write.
 * HTTP 400, 403, and 409 leave the previous description.
 *
 *   npm run test:surface -- --path tests/developer-content-type-description.spec.js
 *
 * QA mode: perc-devctl qa-up → qa-health → TEST_CMS_URL → test:surface → qa-down.
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

function developerContentTypesUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "content-types",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

function attachConsole(page) {
  const consoleErrors = [];
  const pageErrors = [];
  page.on("console", (msg) => {
    if (msg.type() === "error") {
      consoleErrors.push(msg.text());
    }
  });
  page.on("pageerror", (err) => {
    pageErrors.push(String(err && err.message ? err.message : err));
  });
  return { consoleErrors, pageErrors };
}

function assertClean(consoleErrors, pageErrors) {
  expect(pageErrors, `pageerror: ${pageErrors.join(" | ")}`).toEqual([]);
  const unexpected = consoleErrors.filter((text) => {
    if (
      /Failed to load resource: the server responded with a status of (400|403|409)/.test(
        text,
      )
    ) {
      return false;
    }
    if (/Failed to load resource: the server responded with a status of 404/.test(text)) {
      return false;
    }
    if (/rx_resources\/css\/(?:bootstrap|googleFonts)/.test(text)) {
      return false;
    }
    if (/favicon/i.test(text)) {
      return false;
    }
    return true;
  });
  expect(unexpected, `console error: ${unexpected.join(" | ")}`).toEqual([]);
}

function isContentTypeDetailWrite(request) {
  return (
    request.method() === "PUT" &&
    /\/services\/contenttypes\/[^/?]+\/?(?:\?|$)/.test(request.url())
  );
}

function isContentTypeList(response) {
  return (
    response.request().method() === "GET" &&
    /\/services\/contenttypes\/?(?:\?|$)/.test(response.url()) &&
    response.status() === 200
  );
}

function detailBody(payload) {
  if (!payload || typeof payload !== "object") return {};
  return payload.ContentTypeDetail || payload.contentTypeDetail || payload;
}

function descriptionText(page) {
  return page.locator('[data-testid="developer-ct-set-description-text"]');
}

async function openContentTypes(page) {
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
    throw new Error("No content types in catalog — fail closed");
  }
}

async function openPercPage(page) {
  await openContentTypes(page);
  const openBtn = page.locator('[data-testid="developer-ct-open"][data-ct-name="percPage"]');
  await expect(openBtn, "H2 catalog must include percPage").toBeVisible({ timeout: 20_000 });
  await openBtn.click();
  const detail = page.locator('[data-testid="developer-ct-detail"]');
  const detailError = page.locator('[data-testid="developer-ct-detail-error"]');
  await expect(detail.or(detailError).first()).toBeVisible({ timeout: 30_000 });
  if (await detailError.isVisible()) {
    throw new Error(`Content type detail error: ${(await detailError.innerText()).trim()}`);
  }
  // A blank stored description is an empty paragraph. Playwright treats that
  // as hidden, so wait for the section and the attached text node.
  await expect(page.locator('[data-testid="developer-ct-set-description"]')).toBeVisible({
    timeout: 30_000,
  });
  await expect(descriptionText(page)).toBeAttached();
  await expect(page.locator('[data-testid="developer-ct-lock"]')).toBeEnabled({
    timeout: 30_000,
  });
}

test.describe("Developer set a content type description (#5433)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(180_000);
    await loginAsAdmin(page);
  });

  test("sets the description only after success, keeps label and enabled, and a blank description clears it", async ({
    page,
  }) => {
    const { consoleErrors, pageErrors } = attachConsole(page);
    const updateUrls = [];
    page.on("request", (request) => {
      if (isContentTypeDetailWrite(request)) {
        updateUrls.push(request.url());
      }
    });

    await openPercPage(page);
    const original = (await descriptionText(page).getAttribute("data-ct-description")) || "";
    const label = page.locator('[data-testid="developer-ct-label"]');
    const enabled = page.locator('[data-testid="developer-ct-enabled"]');
    const originalLabel = await label.inputValue();
    const originalEnabled = await enabled.isChecked();
    const nextDescription = original === "qa5433 note" ? "qa5433 note 2" : "qa5433 note";

    await expect(page.locator('[data-testid="developer-ct-set-description-edit"]')).toBeDisabled();

    await page.locator('[data-testid="developer-ct-lock"]').click();
    await expect(page.locator('[data-testid="developer-ct-lock-status"]')).toHaveText(
      /Locked by you/i,
      { timeout: 20_000 },
    );
    await expect(page.locator('[data-testid="developer-ct-set-description-edit"]')).toBeEnabled();

    const writesBefore = updateUrls.length;
    await page.locator('[data-testid="developer-ct-set-description-edit"]').click();
    const descriptionInput = page.locator('[data-testid="developer-ct-set-description-input"]');
    await expect(descriptionInput).toHaveValue(original);
    await page.locator('[data-testid="developer-ct-set-description-save"]').click();
    await expect(descriptionInput).toHaveCount(0);
    expect(updateUrls.length, "the same description must not update the type").toBe(writesBefore);
    await expect(descriptionText(page)).toHaveAttribute("data-ct-description", original);

    await page.locator('[data-testid="developer-ct-set-description-edit"]').click();
    await descriptionInput.fill("later");
    await expect(descriptionText(page)).toHaveAttribute("data-ct-description", original);
    await page.locator('[data-testid="developer-ct-set-description-cancel"]').click();
    await expect(descriptionInput).toHaveCount(0);
    expect(updateUrls.length, "cancel must not update the type").toBe(writesBefore);
    await expect(descriptionText(page)).toHaveAttribute("data-ct-description", original);
    await expect(label).toHaveValue(originalLabel);
    if (originalEnabled) {
      await expect(enabled).toBeChecked();
    } else {
      await expect(enabled).not.toBeChecked();
    }

    await page.locator('[data-testid="developer-ct-set-description-edit"]').click();
    await descriptionInput.fill(` ${nextDescription} `);
    await expect(descriptionText(page)).toHaveAttribute("data-ct-description", original);
    const savedDone = page.waitForResponse((response) =>
      isContentTypeDetailWrite(response.request()),
    );
    const listRefresh = page.waitForResponse((response) => isContentTypeList(response));
    await page.locator('[data-testid="developer-ct-set-description-save"]').click();
    const saved = await savedDone;
    await listRefresh;
    expect(saved.status(), await saved.text()).toBe(200);
    const savedBody = detailBody(saved.request().postDataJSON());
    expect(savedBody).toEqual({ description: nextDescription });
    expect(savedBody).not.toHaveProperty("label");
    expect(savedBody).not.toHaveProperty("enabled");
    await expect(descriptionText(page)).toHaveAttribute("data-ct-description", nextDescription);
    await expect(page.locator('[data-testid="developer-ct-description"]')).toHaveValue(
      nextDescription,
    );
    await expect(label).toHaveValue(originalLabel);
    if (originalEnabled) {
      await expect(enabled).toBeChecked();
    } else {
      await expect(enabled).not.toBeChecked();
    }
    await expect(page.locator('[data-testid="developer-ct-set-description-notice"]')).toBeVisible();

    await page.locator('[data-testid="developer-ct-back"]').click();
    await expect(page.locator('[data-testid="developer-ct-panel"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(
      page.locator(
        '[data-testid="developer-ct-catalog-description"][data-ct-name="percPage"]',
      ),
    ).toHaveAttribute("data-ct-description", nextDescription);
    await page.locator('[data-testid="developer-ct-open"][data-ct-name="percPage"]').click();
    await expect(descriptionText(page)).toHaveAttribute("data-ct-description", nextDescription);
    await expect(page.locator('[data-testid="developer-ct-lock-status"]')).toHaveText(
      /Not locked/i,
      { timeout: 20_000 },
    );
    await page.locator('[data-testid="developer-ct-lock"]').click();
    await expect(page.locator('[data-testid="developer-ct-lock-status"]')).toHaveText(
      /Locked by you/i,
      { timeout: 20_000 },
    );

    const beforeForced = updateUrls.length;
    let forced = 0;
    const statuses = [400, 403, 409];
    await page.route("**/services/contenttypes/**", async (route) => {
      const request = route.request();
      if (isContentTypeDetailWrite(request) && forced < statuses.length) {
        const status = statuses[forced];
        forced += 1;
        await route.fulfill({
          status,
          contentType: "application/json",
          body: JSON.stringify({
            message: `forced ${status}`,
            ContentTypeDetail: {
              name: "Renamed",
              label: "Renamed",
              description: "changed",
              enabled: !originalEnabled,
              fields: [],
            },
          }),
        });
        return;
      }
      await route.continue();
    });
    for (const status of statuses) {
      const forcedDone = page.waitForResponse((response) =>
        isContentTypeDetailWrite(response.request()),
      );
      const draft = page.locator('[data-testid="developer-ct-set-description-input"]');
      if ((await draft.count()) === 0) {
        await page.locator('[data-testid="developer-ct-set-description-edit"]').click();
      }
      await draft.fill("later");
      await page.locator('[data-testid="developer-ct-set-description-save"]').click();
      const forcedResponse = await forcedDone;
      expect(forcedResponse.status()).toBe(status);
      await expect(page.locator('[data-testid="developer-ct-set-description-error"]')).toContainText(
        `forced ${status}`,
      );
      await expect(descriptionText(page)).toHaveAttribute("data-ct-description", nextDescription);
      await expect(label).toHaveValue(originalLabel);
      if (originalEnabled) {
        await expect(enabled).toBeChecked();
      } else {
        await expect(enabled).not.toBeChecked();
      }
      await expect(page.locator('[data-testid="developer-ct-lock-status"]')).toHaveText(
        /Locked by you/i,
      );
    }
    expect(updateUrls.length).toBe(beforeForced + statuses.length);
    await page.unroute("**/services/contenttypes/**");
    await page.locator('[data-testid="developer-ct-set-description-cancel"]').click();

    await page.locator('[data-testid="developer-ct-set-description-edit"]').click();
    await page.locator('[data-testid="developer-ct-set-description-input"]').fill("   ");
    const clearedDone = page.waitForResponse((response) =>
      isContentTypeDetailWrite(response.request()),
    );
    await page.locator('[data-testid="developer-ct-set-description-save"]').click();
    const cleared = await clearedDone;
    expect(cleared.status(), await cleared.text()).toBe(200);
    expect(detailBody(cleared.request().postDataJSON())).toEqual({ description: "" });
    await expect(descriptionText(page)).toHaveAttribute("data-ct-description", "");
    await expect(label).toHaveValue(originalLabel);
    if (originalEnabled) {
      await expect(enabled).toBeChecked();
    } else {
      await expect(enabled).not.toBeChecked();
    }

    if (original !== "") {
      await page.locator('[data-testid="developer-ct-set-description-edit"]').click();
      await page.locator('[data-testid="developer-ct-set-description-input"]').fill(original);
      const restoredDone = page.waitForResponse((response) =>
        isContentTypeDetailWrite(response.request()),
      );
      await page.locator('[data-testid="developer-ct-set-description-save"]').click();
      const restored = await restoredDone;
      expect(restored.status(), await restored.text()).toBe(200);
      await expect(descriptionText(page)).toHaveAttribute("data-ct-description", original);
    }

    await page.locator('[data-testid="developer-ct-unlock"]').click();
    await expect(page.locator('[data-testid="developer-ct-lock-status"]')).toHaveText(
      /Not locked/i,
      { timeout: 20_000 },
    );

    assertClean(consoleErrors, pageErrors);
  });
});
