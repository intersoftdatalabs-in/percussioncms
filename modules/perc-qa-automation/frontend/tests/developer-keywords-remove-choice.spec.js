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
 * Developer Keywords — remove one choice (#5336 / #1690).
 *
 * The choice is gone only after the existing keyword update succeeds.
 * Label, description, sequence, and the other choices stay. Cancel does
 * not write. HTTP 400, 403, and 409 leave the previous choices. Removing
 * the last choice clears the list and does not delete the keyword. Add
 * and keyword delete still work.
 *
 *   npm run test:surface -- --path tests/developer-keywords-remove-choice.spec.js
 *
 * QA mode: perc-devctl qa-up → qa-health → TEST_CMS_URL → test:surface → qa-down.
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

function developerKeywordsUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "keywords",
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
    // H2 cell shell pages reference rx_resources CSS that is not in the image.
    // The 404 is HTML, so Chromium also reports a stylesheet MIME refusal.
    if (/Failed to load resource: the server responded with a status of 404/.test(text)) {
      return false;
    }
    if (/rx_resources\/css\/(?:bootstrap|googleFonts)/.test(text)) {
      return false;
    }
    return true;
  });
  expect(unexpected, `console error: ${unexpected.join(" | ")}`).toEqual([]);
}

function isKeywordCreate(request) {
  return (
    request.method() === "POST" &&
    /\/services\/keywords\/?(?:\?|$)/.test(request.url())
  );
}

function isKeywordUpdate(request) {
  return (
    request.method() === "PUT" && /\/services\/keywords\/[^/?]+/.test(request.url())
  );
}

function isKeywordDelete(request) {
  return (
    request.method() === "DELETE" && /\/services\/keywords\/[^/?]+/.test(request.url())
  );
}

function keywordBody(payload) {
  if (!payload || typeof payload !== "object") return {};
  return payload.Keyword || payload.keyword || payload;
}

/** The row itself is also role=button and repeats the open label. */
function keywordOpenButton(page, label) {
  return page.locator(
    `[data-testid="developer-kw-open"][aria-label="Open ${label}"]`,
  );
}

function removeChoiceButton(page, label) {
  return page.locator(
    `[data-testid="developer-kw-choice-remove"][data-choice-label="${label}"]`,
  );
}

async function openKeywords(page) {
  await page.goto(developerKeywordsUrl(), { waitUntil: "networkidle" });
  await expect(page.locator('[data-testid="tab-developer-keywords"]')).toBeVisible({
    timeout: 20_000,
  });
  const error = page.locator('[data-testid="developer-kw-error"]');
  const panel = page.locator('[data-testid="developer-kw-panel"]');
  const empty = page.locator('[data-testid="developer-kw-empty"]');
  await expect(panel.or(empty).or(error).first()).toBeVisible({ timeout: 30_000 });
  if (await error.isVisible()) {
    throw new Error(`Keywords catalog error: ${(await error.innerText()).trim()}`);
  }
  await expect(page.locator('[data-testid="developer-kw-new"]')).toBeVisible();
}

async function createKeyword(page, label) {
  await page.locator('[data-testid="developer-kw-new"]').click();
  await expect(page.locator('[data-testid="developer-kw-editor"]')).toBeVisible();
  await expect(page.locator('[data-testid="developer-kw-choice-remove"]')).toHaveCount(0);
  await page.locator('[data-testid="developer-kw-label"]').fill(label);
  await page.locator('[data-testid="developer-kw-description"]').fill("Item priority");
  await page.locator('[data-testid="developer-kw-sequence"]').fill("4");
  await page.locator('[data-testid="developer-kw-choices"]').fill("High|high|1\nLow|low|2");
  const created = page.waitForResponse((response) => isKeywordCreate(response.request()));
  await page.locator('[data-testid="developer-kw-save"]').click();
  const response = await created;
  expect(response.status(), await response.text()).toBe(200);
  const body = keywordBody(response.request().postDataJSON());
  expect(body.label).toBe(label);
  expect(body.description).toBe("Item priority");
  expect(body.sequence).toBe(4);
  expect(body.choices).toEqual([
    expect.objectContaining({ label: "High", value: "high", sequence: 1 }),
    expect.objectContaining({ label: "Low", value: "low", sequence: 2 }),
  ]);
  await expect(page.locator('[data-testid="developer-kw-panel"]')).toBeVisible({
    timeout: 30_000,
  });
  await expect(keywordOpenButton(page, label)).toBeVisible();
}

async function openKeyword(page, label) {
  const readDone = page.waitForResponse(
    (response) =>
      response.request().method() === "GET" &&
      /\/services\/keywords\/[^/?]+/.test(response.url()) &&
      response.status() === 200,
  );
  await keywordOpenButton(page, label).click();
  await readDone;
  await expect(page.locator('[data-testid="developer-kw-editor"]')).toBeVisible();
  await expect(page.locator('[data-testid="developer-kw-label"]')).toHaveValue(label);
}

test.describe("Developer remove one choice on a keyword (#5336)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(180_000);
    await loginAsAdmin(page);
  });

  test("removes one choice only after success and keeps the keyword when the last choice is cleared", async ({
    page,
  }) => {
    const { consoleErrors, pageErrors } = attachConsole(page);
    const updateUrls = [];
    const deleteUrls = [];
    page.on("request", (request) => {
      if (isKeywordUpdate(request)) {
        updateUrls.push(request.url());
      }
      if (isKeywordDelete(request)) {
        deleteUrls.push(request.url());
      }
    });

    await openKeywords(page);
    const label = `NdKwRm${Date.now()}`;
    await createKeyword(page, label);
    await openKeyword(page, label);
    await expect(page.locator('[data-testid="developer-kw-description"]')).toHaveValue(
      "Item priority",
    );
    await expect(page.locator('[data-testid="developer-kw-sequence"]')).toHaveValue("4");
    await expect(page.locator('[data-testid="developer-kw-choice"]')).toHaveCount(2);
    await expect(removeChoiceButton(page, "Low")).toBeEnabled();

    await removeChoiceButton(page, "Low").click();
    await expect(page.locator('[data-testid="developer-catalog-confirm-body"]')).toContainText(
      "Low",
    );
    await page.locator('[data-testid="developer-catalog-confirm-cancel"]').click();
    expect(updateUrls, "cancel must not update the keyword").toEqual([]);
    expect(deleteUrls, "cancel must not delete the keyword").toEqual([]);
    await expect(page.locator('[data-testid="developer-kw-choice"]')).toHaveCount(2);
    await expect(page.locator('[data-testid="developer-kw-editor"]')).toBeVisible();

    const removedDone = page.waitForResponse((response) => isKeywordUpdate(response.request()));
    await removeChoiceButton(page, "Low").click();
    await page.locator('[data-testid="developer-catalog-confirm-submit"]').click();
    const removed = await removedDone;
    expect(removed.status(), await removed.text()).toBe(200);
    const removedBody = keywordBody(removed.request().postDataJSON());
    expect(removedBody.label).toBe(label);
    expect(removedBody.description).toBe("Item priority");
    expect(removedBody.sequence).toBe(4);
    expect(removedBody.choices).toEqual([
      expect.objectContaining({ label: "High", value: "high", sequence: 1 }),
    ]);
    expect(deleteUrls, "removing a choice must not delete the keyword").toEqual([]);
    await expect(page.locator('[data-testid="developer-kw-choice"]')).toHaveCount(1);
    await expect(
      page.locator('[data-testid="developer-kw-choice"][data-choice-label="High"]'),
    ).toBeVisible();
    await expect(
      page.locator('[data-testid="developer-kw-choice"][data-choice-label="Low"]'),
    ).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-kw-remove-choice-notice"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-kw-label"]')).toHaveValue(label);
    await expect(page.locator('[data-testid="developer-kw-description"]')).toHaveValue(
      "Item priority",
    );
    await expect(page.locator('[data-testid="developer-kw-sequence"]')).toHaveValue("4");

    await page.reload({ waitUntil: "networkidle" });
    await openKeywords(page);
    await openKeyword(page, label);
    await expect(page.locator('[data-testid="developer-kw-choice"]')).toHaveCount(1);
    await expect(
      page.locator('[data-testid="developer-kw-choice"][data-choice-label="Low"]'),
    ).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-kw-description"]')).toHaveValue(
      "Item priority",
    );
    await expect(page.locator('[data-testid="developer-kw-sequence"]')).toHaveValue("4");

    const beforeForced = updateUrls.length;
    let forced = 0;
    const statuses = [400, 403, 409];
    await page.route("**/services/keywords/**", async (route) => {
      const request = route.request();
      if (isKeywordUpdate(request) && forced < statuses.length) {
        const status = statuses[forced];
        forced += 1;
        await route.fulfill({
          status,
          contentType: "application/json",
          body: JSON.stringify({
            message: `forced ${status}`,
            label: "Renamed",
            description: "changed",
            sequence: 9,
            choices: [{ label: "Invented", value: "invented", sequence: 8 }],
          }),
        });
        return;
      }
      await route.continue();
    });
    for (const status of statuses) {
      const forcedDone = page.waitForResponse((response) => isKeywordUpdate(response.request()));
      await removeChoiceButton(page, "High").click();
      await page.locator('[data-testid="developer-catalog-confirm-submit"]').click();
      const forcedResponse = await forcedDone;
      expect(forcedResponse.status()).toBe(status);
      await expect(page.locator('[data-testid="developer-kw-remove-choice-error"]')).toContainText(
        `forced ${status}`,
      );
      await expect(page.locator('[data-testid="developer-kw-choice"]')).toHaveCount(1);
      await expect(page.locator('[data-testid="developer-kw-saved-choices"]')).not.toContainText(
        "Invented",
      );
      await expect(
        page.locator('[data-testid="developer-kw-choice"][data-choice-label="High"]'),
      ).toBeVisible();
      await expect(page.locator('[data-testid="developer-kw-label"]')).toHaveValue(label);
    }
    expect(updateUrls.length).toBe(beforeForced + statuses.length);
    expect(deleteUrls).toEqual([]);
    await page.unroute("**/services/keywords/**");

    const clearedDone = page.waitForResponse((response) => isKeywordUpdate(response.request()));
    await removeChoiceButton(page, "High").click();
    await page.locator('[data-testid="developer-catalog-confirm-submit"]').click();
    const cleared = await clearedDone;
    expect(cleared.status(), await cleared.text()).toBe(200);
    const clearedBody = keywordBody(cleared.request().postDataJSON());
    expect(clearedBody.label).toBe(label);
    expect(clearedBody.description).toBe("Item priority");
    expect(clearedBody.sequence).toBe(4);
    expect(clearedBody.choices).toEqual([]);
    expect(cleared.request().method()).toBe("PUT");
    expect(deleteUrls, "clearing the last choice must not delete the keyword").toEqual([]);
    await expect(page.locator('[data-testid="developer-kw-choices-empty"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-kw-choice"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-kw-editor"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-kw-delete"]')).toBeVisible();

    await page.reload({ waitUntil: "networkidle" });
    await openKeywords(page);
    await expect(keywordOpenButton(page, label)).toBeVisible();
    await openKeyword(page, label);
    await expect(page.locator('[data-testid="developer-kw-choices-empty"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-kw-choice"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-kw-label"]')).toHaveValue(label);
    await expect(page.locator('[data-testid="developer-kw-description"]')).toHaveValue(
      "Item priority",
    );
    await expect(page.locator('[data-testid="developer-kw-sequence"]')).toHaveValue("4");

    const addLabel = page.locator('[data-testid="developer-kw-add-choice-label"]');
    const addValue = page.locator('[data-testid="developer-kw-add-choice-value"]');
    await addLabel.fill("Low");
    await addValue.fill("low");
    const addedDone = page.waitForResponse((response) => isKeywordUpdate(response.request()));
    await page.locator('[data-testid="developer-kw-add-choice-save"]').click();
    const added = await addedDone;
    expect(added.status(), await added.text()).toBe(200);
    const addedBody = keywordBody(added.request().postDataJSON());
    expect(addedBody.label).toBe(label);
    expect(addedBody.choices).toEqual([
      expect.objectContaining({ label: "Low", value: "low" }),
    ]);
    await expect(
      page.locator('[data-testid="developer-kw-choice"][data-choice-label="Low"]'),
    ).toBeVisible();

    const deleted = page.waitForResponse((response) => isKeywordDelete(response.request()));
    await page.locator('[data-testid="developer-kw-delete"]').click();
    await page.locator('[data-testid="developer-catalog-confirm-submit"]').click();
    const deleteResponse = await deleted;
    expect(deleteResponse.status()).toBe(204);
    await expect(keywordOpenButton(page, label)).toHaveCount(0);

    assertClean(consoleErrors, pageErrors);
  });
});
