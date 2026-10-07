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
 * Developer Keywords — change the label of one choice (#5348 / #1690).
 *
 * The new label shows only after the existing keyword update succeeds.
 * Value, description, sequence, and the other choices stay. The keyword
 * label, description, and sequence stay. A blank label does not write.
 * Cancel does not write. A duplicate label does not replace another choice.
 * HTTP 400, 403, and 409 leave the previous label. Add and remove still work.
 *
 *   npm run test:surface -- --path tests/developer-keywords-change-choice-label.spec.js
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

function keywordOpenButton(page, label) {
  return page.locator(
    `[data-testid="developer-kw-open"][aria-label="Open ${label}"]`,
  );
}

function choiceRow(page, label) {
  return page.locator(`[data-testid="developer-kw-choice"][data-choice-label="${label}"]`);
}

function changeLabelButton(page, label) {
  return page.locator(
    `[data-testid="developer-kw-choice-label-edit"][data-choice-label="${label}"]`,
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
  await expect(page.locator('[data-testid="developer-kw-choice-label-edit"]')).toHaveCount(0);
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

test.describe("Developer change the label of one keyword choice (#5348)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(180_000);
    await loginAsAdmin(page);
  });

  test("changes one choice label only after success and leaves value, sequence, and the other choice", async ({
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
    const label = `NdKwLb${Date.now()}`;
    await createKeyword(page, label);
    await openKeyword(page, label);
    await expect(page.locator('[data-testid="developer-kw-description"]')).toHaveValue(
      "Item priority",
    );
    await expect(page.locator('[data-testid="developer-kw-sequence"]')).toHaveValue("4");
    await expect(page.locator('[data-testid="developer-kw-choice"]')).toHaveCount(2);
    await expect(changeLabelButton(page, "Low")).toBeEnabled();
    await expect(choiceRow(page, "Low")).toContainText("(low)");

    await changeLabelButton(page, "Low").click();
    const labelInput = page.locator('[data-testid="developer-kw-choice-label-input"]');
    const saveLabel = page.locator('[data-testid="developer-kw-choice-label-save"]');
    await expect(labelInput).toHaveValue("Low");
    await labelInput.fill("   ");
    await expect(saveLabel).toBeDisabled();
    await saveLabel.click({ force: true });
    expect(updateUrls, "blank label must not update the keyword").toEqual([]);
    await expect(choiceRow(page, "Low")).toBeVisible();
    await expect(choiceRow(page, "High")).toBeVisible();

    await labelInput.fill("HIGH");
    await saveLabel.click();
    await expect(page.locator('[data-testid="developer-kw-choice-label-error"]')).toBeVisible();
    expect(updateUrls, "duplicate label must not update the keyword").toEqual([]);
    await expect(choiceRow(page, "Low")).toBeVisible();
    await expect(choiceRow(page, "High")).toBeVisible();
    await expect(choiceRow(page, "High")).toContainText("(high)");

    await labelInput.fill("Medium");
    await page.locator('[data-testid="developer-kw-choice-label-cancel"]').click();
    await expect(labelInput).toHaveCount(0);
    expect(updateUrls, "cancel must not update the keyword").toEqual([]);
    expect(deleteUrls, "cancel must not delete the keyword").toEqual([]);
    await expect(choiceRow(page, "Low")).toBeVisible();
    await expect(choiceRow(page, "Medium")).toHaveCount(0);

    await changeLabelButton(page, "Low").click();
    await labelInput.fill("Medium");
    await expect(choiceRow(page, "Medium")).toHaveCount(0);
    const savedDone = page.waitForResponse((response) => isKeywordUpdate(response.request()));
    await saveLabel.click();
    const saved = await savedDone;
    expect(saved.status(), await saved.text()).toBe(200);
    const savedBody = keywordBody(saved.request().postDataJSON());
    expect(savedBody.label).toBe(label);
    expect(savedBody.description).toBe("Item priority");
    expect(savedBody.sequence).toBe(4);
    expect(savedBody.choices).toEqual([
      expect.objectContaining({ label: "High", value: "high", sequence: 1 }),
      expect.objectContaining({ label: "Medium", value: "low", sequence: 2 }),
    ]);
    expect(deleteUrls, "changing a label must not delete the keyword").toEqual([]);
    await expect(choiceRow(page, "Medium")).toBeVisible();
    await expect(choiceRow(page, "Medium")).toContainText("(low)");
    await expect(choiceRow(page, "Low")).toHaveCount(0);
    await expect(choiceRow(page, "High")).toContainText("(high)");
    await expect(page.locator('[data-testid="developer-kw-choice-label-notice"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-kw-label"]')).toHaveValue(label);
    await expect(page.locator('[data-testid="developer-kw-description"]')).toHaveValue(
      "Item priority",
    );
    await expect(page.locator('[data-testid="developer-kw-sequence"]')).toHaveValue("4");

    await page.reload({ waitUntil: "networkidle" });
    await openKeywords(page);
    await openKeyword(page, label);
    await expect(choiceRow(page, "Medium")).toBeVisible();
    await expect(choiceRow(page, "Medium")).toContainText("(low)");
    await expect(choiceRow(page, "Low")).toHaveCount(0);
    await expect(choiceRow(page, "High")).toBeVisible();
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
            choices: [{ label: "Later", value: "later", sequence: 8 }],
          }),
        });
        return;
      }
      await route.continue();
    });
    for (const status of statuses) {
      const forcedDone = page.waitForResponse((response) => isKeywordUpdate(response.request()));
      const draft = page.locator('[data-testid="developer-kw-choice-label-input"]');
      if ((await draft.count()) === 0) {
        await changeLabelButton(page, "Medium").click();
      }
      await draft.fill("Later");
      await page.locator('[data-testid="developer-kw-choice-label-save"]').click();
      const forcedResponse = await forcedDone;
      expect(forcedResponse.status()).toBe(status);
      await expect(page.locator('[data-testid="developer-kw-choice-label-error"]')).toContainText(
        `forced ${status}`,
      );
      await expect(choiceRow(page, "Medium")).toBeVisible();
      await expect(choiceRow(page, "Later")).toHaveCount(0);
      await expect(choiceRow(page, "High")).toBeVisible();
      await expect(page.locator('[data-testid="developer-kw-label"]')).toHaveValue(label);
      await expect(page.locator('[data-testid="developer-kw-saved-choices"]')).not.toContainText(
        "Later",
      );
    }
    expect(updateUrls.length).toBe(beforeForced + statuses.length);
    expect(deleteUrls).toEqual([]);
    await page.unroute("**/services/keywords/**");

    await page.locator('[data-testid="developer-kw-choice-label-cancel"]').click();
    await page.locator('[data-testid="developer-kw-add-choice-label"]').fill("Other");
    await page.locator('[data-testid="developer-kw-add-choice-value"]').fill("other");
    const addedDone = page.waitForResponse((response) => isKeywordUpdate(response.request()));
    await page.locator('[data-testid="developer-kw-add-choice-save"]').click();
    const added = await addedDone;
    expect(added.status(), await added.text()).toBe(200);
    const addedBody = keywordBody(added.request().postDataJSON());
    expect(addedBody.label).toBe(label);
    expect(addedBody.choices.map((choice) => choice.label)).toEqual(["High", "Medium", "Other"]);
    await expect(choiceRow(page, "Other")).toBeVisible();
    await expect(choiceRow(page, "Medium")).toBeVisible();

    const removedDone = page.waitForResponse((response) => isKeywordUpdate(response.request()));
    await page
      .locator('[data-testid="developer-kw-choice-remove"][data-choice-label="High"]')
      .click();
    await page.locator('[data-testid="developer-catalog-confirm-submit"]').click();
    const removed = await removedDone;
    expect(removed.status(), await removed.text()).toBe(200);
    const removedBody = keywordBody(removed.request().postDataJSON());
    expect(removedBody.choices.map((choice) => choice.label)).toEqual(["Medium", "Other"]);
    await expect(choiceRow(page, "High")).toHaveCount(0);
    await expect(choiceRow(page, "Medium")).toBeVisible();
    expect(deleteUrls, "removing a choice must not delete the keyword").toEqual([]);

    const deleted = page.waitForResponse((response) => isKeywordDelete(response.request()));
    await page.locator('[data-testid="developer-kw-delete"]').click();
    await page.locator('[data-testid="developer-catalog-confirm-submit"]').click();
    const deleteResponse = await deleted;
    expect(deleteResponse.status()).toBe(204);
    await expect(keywordOpenButton(page, label)).toHaveCount(0);

    assertClean(consoleErrors, pageErrors);
  });
});
