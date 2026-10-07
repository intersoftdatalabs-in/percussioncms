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
 * Developer Keywords — add one choice (#5335 / #1690).
 *
 * The choice appears only after the existing keyword update succeeds.
 * Label, description, sequence, and previous choices stay. A blank choice
 * is not sent. Cancel does not write. A duplicate does not add a second row.
 * HTTP 400, 403, and 409 leave the previous choices. Create, label save,
 * and delete still work.
 *
 *   npm run test:surface -- --path tests/developer-keywords-add-choice.spec.js
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
  await expect(page.locator('[data-testid="developer-kw-add-choice"]')).toHaveCount(0);
  await page.locator('[data-testid="developer-kw-label"]').fill(label);
  await page.locator('[data-testid="developer-kw-description"]').fill("Item priority");
  await page.locator('[data-testid="developer-kw-sequence"]').fill("4");
  await page.locator('[data-testid="developer-kw-choices"]').fill("High|high|1");
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
  // A blank draft stays disabled so it cannot be written.
  await expect(page.locator('[data-testid="developer-kw-add-choice-save"]')).toBeDisabled();
}

test.describe("Developer add one choice on a keyword (#5335)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(180_000);
    await loginAsAdmin(page);
  });

  test("adds one choice only after success and keeps previous choices on cancel, duplicate, 400, 403, and 409", async ({
    page,
  }) => {
    const { consoleErrors, pageErrors } = attachConsole(page);
    const updateUrls = [];
    page.on("request", (request) => {
      if (isKeywordUpdate(request)) {
        updateUrls.push(request.url());
      }
    });

    await openKeywords(page);
    const label = `NdKw${Date.now()}`;
    await createKeyword(page, label);
    await openKeyword(page, label);
    await expect(page.locator('[data-testid="developer-kw-description"]')).toHaveValue(
      "Item priority",
    );
    await expect(page.locator('[data-testid="developer-kw-sequence"]')).toHaveValue("4");
    await expect(page.locator('[data-testid="developer-kw-choice"]')).toHaveCount(1);
    await expect(
      page.locator('[data-testid="developer-kw-choice"][data-choice-label="High"]'),
    ).toBeVisible();

    const addButton = page.locator('[data-testid="developer-kw-add-choice-save"]');
    const choiceLabel = page.locator('[data-testid="developer-kw-add-choice-label"]');
    const choiceValue = page.locator('[data-testid="developer-kw-add-choice-value"]');
    await choiceLabel.fill("   ");
    await expect(addButton).toBeDisabled();
    expect(updateUrls, "blank choice must not update the keyword").toEqual([]);

    await choiceLabel.fill("HIGH");
    await choiceValue.fill("other");
    await expect(addButton).toBeEnabled();
    await addButton.click();
    await expect(page.locator('[data-testid="developer-kw-add-choice-error"]')).toBeVisible();
    expect(updateUrls, "duplicate label must not update the keyword").toEqual([]);
    await expect(page.locator('[data-testid="developer-kw-choice"]')).toHaveCount(1);

    await choiceLabel.fill("Other");
    await choiceValue.fill("HIGH");
    await addButton.click();
    await expect(page.locator('[data-testid="developer-kw-add-choice-error"]')).toBeVisible();
    expect(updateUrls, "duplicate value must not update the keyword").toEqual([]);
    await expect(page.locator('[data-testid="developer-kw-choice"]')).toHaveCount(1);

    await choiceLabel.fill("Low");
    await choiceValue.fill("low");
    await page.locator('[data-testid="developer-kw-add-choice-cancel"]').click();
    await expect(choiceLabel).toHaveValue("");
    await expect(choiceValue).toHaveValue("");
    expect(updateUrls, "cancel must not update the keyword").toEqual([]);
    await expect(page.locator('[data-testid="developer-kw-choice"]')).toHaveCount(1);
    await expect(page.locator('[data-testid="developer-kw-editor"]')).toBeVisible();

    await choiceLabel.fill("Low");
    await choiceValue.fill("low");
    const addedDone = page.waitForResponse((response) => isKeywordUpdate(response.request()));
    await addButton.click();
    const added = await addedDone;
    expect(added.status(), await added.text()).toBe(200);
    const addedBody = keywordBody(added.request().postDataJSON());
    expect(addedBody.label).toBe(label);
    expect(addedBody.description).toBe("Item priority");
    expect(addedBody.sequence).toBe(4);
    expect(addedBody.choices).toEqual([
      expect.objectContaining({ label: "High", value: "high", sequence: 1 }),
      expect.objectContaining({ label: "Low", value: "low", sequence: 2 }),
    ]);
    await expect(page.locator('[data-testid="developer-kw-choice"]')).toHaveCount(2);
    await expect(
      page.locator('[data-testid="developer-kw-choice"][data-choice-label="Low"]'),
    ).toBeVisible();
    await expect(page.locator('[data-testid="developer-kw-add-choice-notice"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-kw-label"]')).toHaveValue(label);
    await expect(page.locator('[data-testid="developer-kw-description"]')).toHaveValue(
      "Item priority",
    );
    await expect(page.locator('[data-testid="developer-kw-sequence"]')).toHaveValue("4");

    await page.reload({ waitUntil: "networkidle" });
    await openKeywords(page);
    await openKeyword(page, label);
    await expect(page.locator('[data-testid="developer-kw-choice"]')).toHaveCount(2);
    await expect(
      page.locator('[data-testid="developer-kw-choice"][data-choice-label="Low"]'),
    ).toBeVisible();
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
            choices: [
              { label: "High", value: "high", sequence: 1 },
              { label: "Invented", value: "invented", sequence: 8 },
            ],
          }),
        });
        return;
      }
      await route.continue();
    });
    for (const status of statuses) {
      await choiceLabel.fill("Medium");
      await choiceValue.fill("medium");
      const forcedDone = page.waitForResponse((response) => isKeywordUpdate(response.request()));
      await addButton.click();
      const forcedResponse = await forcedDone;
      expect(forcedResponse.status()).toBe(status);
      await expect(page.locator('[data-testid="developer-kw-add-choice-error"]')).toContainText(
        `forced ${status}`,
      );
      await expect(page.locator('[data-testid="developer-kw-choice"]')).toHaveCount(2);
      await expect(page.locator('[data-testid="developer-kw-saved-choices"]')).not.toContainText(
        "Invented",
      );
      await expect(page.locator('[data-testid="developer-kw-saved-choices"]')).not.toContainText(
        "Medium",
      );
      await expect(page.locator('[data-testid="developer-kw-label"]')).toHaveValue(label);
    }
    expect(updateUrls.length).toBe(beforeForced + statuses.length);
    await page.unroute("**/services/keywords/**");

    const updatesBeforeCancel = updateUrls.length;
    await choiceLabel.fill("Nope");
    await page.locator('[data-testid="developer-kw-cancel"]').click();
    await expect(page.locator('[data-testid="developer-kw-editor"]')).toHaveCount(0);
    expect(updateUrls.length).toBe(updatesBeforeCancel);

    await openKeyword(page, label);
    await expect(page.locator('[data-testid="developer-kw-choice"]')).toHaveCount(2);
    await expect(page.locator('[data-testid="developer-kw-saved-choices"]')).not.toContainText(
      "Nope",
    );

    const description = page.locator('[data-testid="developer-kw-description"]');
    await description.fill("Item priority kept");
    const savedDone = page.waitForResponse((response) => isKeywordUpdate(response.request()));
    await page.locator('[data-testid="developer-kw-save"]').click();
    const saved = await savedDone;
    expect(saved.status(), await saved.text()).toBe(200);
    const savedBody = keywordBody(saved.request().postDataJSON());
    expect(savedBody.label).toBe(label);
    expect(savedBody.description).toBe("Item priority kept");
    expect(savedBody.sequence).toBe(4);
    expect(savedBody.choices.map((choice) => choice.label)).toEqual(["High", "Low"]);
    await expect(page.locator('[data-testid="developer-kw-panel"]')).toBeVisible({
      timeout: 30_000,
    });

    await openKeyword(page, label);
    await expect(description).toHaveValue("Item priority kept");
    await expect(page.locator('[data-testid="developer-kw-choice"]')).toHaveCount(2);

    const deleted = page.waitForResponse((response) => isKeywordDelete(response.request()));
    await page.locator('[data-testid="developer-kw-delete"]').click();
    await page.locator('[data-testid="developer-catalog-confirm-submit"]').click();
    const deleteResponse = await deleted;
    expect(deleteResponse.status()).toBe(204);
    await expect(keywordOpenButton(page, label)).toHaveCount(0);

    assertClean(consoleErrors, pageErrors);
  });
});
