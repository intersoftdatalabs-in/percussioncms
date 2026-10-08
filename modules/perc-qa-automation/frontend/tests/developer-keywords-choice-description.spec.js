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
 * Developer Keywords — set the description of one choice (#5350 / #1690).
 *
 * The new description shows only after the existing keyword update succeeds.
 * Label, value, sequence, and the other choices stay. The keyword label,
 * description, and sequence stay. The choice description is not the keyword
 * description. Cancel does not write. A blank description does not clear a
 * stored description. HTTP 400, 403, and 409 leave the previous description.
 * Add, remove, label change, and value change still work.
 *
 *   npm run test:surface -- --path tests/developer-keywords-choice-description.spec.js
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

function changeDescriptionButton(page, label) {
  return page.locator(
    `[data-testid="developer-kw-choice-description-edit"][data-choice-label="${label}"]`,
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
  await expect(page.locator('[data-testid="developer-kw-choice-description-edit"]')).toHaveCount(
    0,
  );
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
  expect(body.choices[0].description ?? "").toBe("");
  expect(body.choices[1].description ?? "").toBe("");
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

test.describe("Developer set the description of one keyword choice (#5350)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(180_000);
    await loginAsAdmin(page);
  });

  test("sets one choice description only after success and does not clear it when blank", async ({
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
    const label = `NdKwDs${Date.now()}`;
    await createKeyword(page, label);
    await openKeyword(page, label);
    await expect(page.locator('[data-testid="developer-kw-description"]')).toHaveValue(
      "Item priority",
    );
    await expect(page.locator('[data-testid="developer-kw-sequence"]')).toHaveValue("4");
    await expect(page.locator('[data-testid="developer-kw-choice"]')).toHaveCount(2);
    await expect(changeDescriptionButton(page, "Low")).toBeEnabled();
    await expect(choiceRow(page, "Low")).toHaveAttribute("data-choice-description", "");
    await expect(choiceRow(page, "High")).toHaveAttribute("data-choice-description", "");
    await expect(choiceRow(page, "Low")).toHaveAttribute("data-choice-value", "low");
    await expect(choiceRow(page, "High")).toHaveAttribute("data-choice-value", "high");

    await changeDescriptionButton(page, "Low").click();
    const descriptionInput = page.locator('[data-testid="developer-kw-choice-description-input"]');
    const saveDescription = page.locator('[data-testid="developer-kw-choice-description-save"]');
    await expect(descriptionInput).toHaveValue("");
    await saveDescription.click();
    await expect(descriptionInput).toHaveCount(0);
    expect(updateUrls, "an empty description must not update the keyword").toEqual([]);
    await expect(choiceRow(page, "Low")).toHaveAttribute("data-choice-description", "");

    await changeDescriptionButton(page, "Low").click();
    await descriptionInput.fill("note");
    await page.locator('[data-testid="developer-kw-choice-description-cancel"]').click();
    await expect(descriptionInput).toHaveCount(0);
    expect(updateUrls, "cancel must not update the keyword").toEqual([]);
    expect(deleteUrls, "cancel must not delete the keyword").toEqual([]);
    await expect(choiceRow(page, "Low")).toHaveAttribute("data-choice-description", "");
    await expect(page.locator('[data-testid="developer-kw-description"]')).toHaveValue(
      "Item priority",
    );

    await changeDescriptionButton(page, "Low").click();
    await descriptionInput.fill(" note ");
    await expect(choiceRow(page, "Low")).toHaveAttribute("data-choice-description", "");
    const savedDone = page.waitForResponse((response) => isKeywordUpdate(response.request()));
    await saveDescription.click();
    const saved = await savedDone;
    expect(saved.status(), await saved.text()).toBe(200);
    const savedBody = keywordBody(saved.request().postDataJSON());
    expect(savedBody.label).toBe(label);
    expect(savedBody.description).toBe("Item priority");
    expect(savedBody.sequence).toBe(4);
    expect(savedBody.choices).toEqual([
      expect.objectContaining({ label: "High", value: "high", sequence: 1 }),
      expect.objectContaining({
        label: "Low",
        value: "low",
        sequence: 2,
        description: "note",
      }),
    ]);
    expect(savedBody.choices[0].description ?? "").toBe("");
    expect(deleteUrls, "setting a description must not delete the keyword").toEqual([]);
    await expect(choiceRow(page, "Low")).toHaveAttribute("data-choice-description", "note");
    await expect(choiceRow(page, "Low")).toHaveAttribute("data-choice-label", "Low");
    await expect(choiceRow(page, "Low")).toHaveAttribute("data-choice-value", "low");
    await expect(choiceRow(page, "High")).toHaveAttribute("data-choice-description", "");
    await expect(choiceRow(page, "High")).toHaveAttribute("data-choice-value", "high");
    await expect(page.locator('[data-testid="developer-kw-choice-description-notice"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-kw-description"]')).toHaveValue(
      "Item priority",
    );
    await expect(page.locator('[data-testid="developer-kw-sequence"]')).toHaveValue("4");

    await page.reload({ waitUntil: "networkidle" });
    await openKeywords(page);
    await openKeyword(page, label);
    await expect(choiceRow(page, "Low")).toHaveAttribute("data-choice-description", "note");
    await expect(choiceRow(page, "High")).toHaveAttribute("data-choice-description", "");
    await expect(choiceRow(page, "Low")).toHaveAttribute("data-choice-value", "low");
    await expect(page.locator('[data-testid="developer-kw-description"]')).toHaveValue(
      "Item priority",
    );
    await expect(page.locator('[data-testid="developer-kw-sequence"]')).toHaveValue("4");

    const beforeBlank = updateUrls.length;
    await changeDescriptionButton(page, "Low").click();
    await page.locator('[data-testid="developer-kw-choice-description-input"]').fill("   ");
    await page.locator('[data-testid="developer-kw-choice-description-save"]').click();
    await expect(page.locator('[data-testid="developer-kw-choice-description-error"]')).toBeVisible();
    expect(updateUrls.length, "a blank description must not clear the stored description").toBe(
      beforeBlank,
    );
    await expect(choiceRow(page, "Low")).toHaveAttribute("data-choice-description", "note");
    await expect(choiceRow(page, "High")).toHaveAttribute("data-choice-description", "");
    await expect(page.locator('[data-testid="developer-kw-description"]')).toHaveValue(
      "Item priority",
    );
    await page.locator('[data-testid="developer-kw-choice-description-cancel"]').click();

    await changeDescriptionButton(page, "High").click();
    await page.locator('[data-testid="developer-kw-choice-description-input"]').fill("note");
    const highDone = page.waitForResponse((response) => isKeywordUpdate(response.request()));
    await page.locator('[data-testid="developer-kw-choice-description-save"]').click();
    const highSaved = await highDone;
    expect(highSaved.status(), await highSaved.text()).toBe(200);
    const highBody = keywordBody(highSaved.request().postDataJSON());
    expect(highBody.description).toBe("Item priority");
    expect(highBody.choices[0]).toEqual(
      expect.objectContaining({ label: "High", value: "high", sequence: 1, description: "note" }),
    );
    expect(highBody.choices[1]).toEqual(
      expect.objectContaining({ label: "Low", value: "low", sequence: 2, description: "note" }),
    );
    await expect(choiceRow(page, "High")).toHaveAttribute("data-choice-description", "note");
    await expect(choiceRow(page, "Low")).toHaveAttribute("data-choice-description", "note");
    await expect(choiceRow(page, "Low")).toHaveAttribute("data-choice-value", "low");

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
            choices: [{ label: "Low", value: "low", description: "later", sequence: 8 }],
          }),
        });
        return;
      }
      await route.continue();
    });
    for (const status of statuses) {
      const forcedDone = page.waitForResponse((response) => isKeywordUpdate(response.request()));
      const draft = page.locator('[data-testid="developer-kw-choice-description-input"]');
      if ((await draft.count()) === 0) {
        await changeDescriptionButton(page, "Low").click();
      }
      await draft.fill("later");
      await page.locator('[data-testid="developer-kw-choice-description-save"]').click();
      const forcedResponse = await forcedDone;
      expect(forcedResponse.status()).toBe(status);
      await expect(
        page.locator('[data-testid="developer-kw-choice-description-error"]'),
      ).toContainText(`forced ${status}`);
      await expect(choiceRow(page, "Low")).toHaveAttribute("data-choice-description", "note");
      await expect(choiceRow(page, "Low")).toHaveAttribute("data-choice-label", "Low");
      await expect(choiceRow(page, "Low")).toHaveAttribute("data-choice-value", "low");
      await expect(choiceRow(page, "High")).toHaveAttribute("data-choice-description", "note");
      await expect(page.locator('[data-testid="developer-kw-description"]')).toHaveValue(
        "Item priority",
      );
      await expect(page.locator('[data-testid="developer-kw-saved-choices"]')).not.toContainText(
        "later",
      );
    }
    expect(updateUrls.length).toBe(beforeForced + statuses.length);
    expect(deleteUrls).toEqual([]);
    await page.unroute("**/services/keywords/**");

    await page.locator('[data-testid="developer-kw-choice-description-cancel"]').click();
    await page.locator('[data-testid="developer-kw-choice-value-edit"][data-choice-label="Low"]').click();
    await page.locator('[data-testid="developer-kw-choice-value-input"]').fill("mid");
    const revaluedDone = page.waitForResponse((response) => isKeywordUpdate(response.request()));
    await page.locator('[data-testid="developer-kw-choice-value-save"]').click();
    const revalued = await revaluedDone;
    expect(revalued.status(), await revalued.text()).toBe(200);
    const revaluedBody = keywordBody(revalued.request().postDataJSON());
    expect(revaluedBody.choices[1]).toEqual(
      expect.objectContaining({
        label: "Low",
        value: "mid",
        sequence: 2,
        description: "note",
      }),
    );
    await expect(choiceRow(page, "Low")).toHaveAttribute("data-choice-value", "mid");
    await expect(choiceRow(page, "Low")).toHaveAttribute("data-choice-description", "note");

    await page.locator('[data-testid="developer-kw-choice-label-edit"][data-choice-label="Low"]').click();
    await page.locator('[data-testid="developer-kw-choice-label-input"]').fill("Medium");
    const relabeledDone = page.waitForResponse((response) => isKeywordUpdate(response.request()));
    await page.locator('[data-testid="developer-kw-choice-label-save"]').click();
    const relabeled = await relabeledDone;
    expect(relabeled.status(), await relabeled.text()).toBe(200);
    await expect(choiceRow(page, "Medium")).toHaveAttribute("data-choice-description", "note");
    await expect(choiceRow(page, "Medium")).toHaveAttribute("data-choice-value", "mid");
    await expect(choiceRow(page, "Low")).toHaveCount(0);

    await page.locator('[data-testid="developer-kw-add-choice-label"]').fill("Other");
    await page.locator('[data-testid="developer-kw-add-choice-value"]').fill("other");
    const addedDone = page.waitForResponse((response) => isKeywordUpdate(response.request()));
    await page.locator('[data-testid="developer-kw-add-choice-save"]').click();
    const added = await addedDone;
    expect(added.status(), await added.text()).toBe(200);
    const addedBody = keywordBody(added.request().postDataJSON());
    expect(addedBody.label).toBe(label);
    expect(addedBody.description).toBe("Item priority");
    expect(addedBody.choices.map((choice) => choice.label)).toEqual(["High", "Medium", "Other"]);
    expect(addedBody.choices[1].description).toBe("note");
    await expect(choiceRow(page, "Other")).toHaveAttribute("data-choice-value", "other");
    await expect(choiceRow(page, "Medium")).toHaveAttribute("data-choice-description", "note");

    const removedDone = page.waitForResponse((response) => isKeywordUpdate(response.request()));
    await page
      .locator('[data-testid="developer-kw-choice-remove"][data-choice-label="High"]')
      .click();
    await page.locator('[data-testid="developer-catalog-confirm-submit"]').click();
    const removed = await removedDone;
    expect(removed.status(), await removed.text()).toBe(200);
    const removedBody = keywordBody(removed.request().postDataJSON());
    expect(removedBody.choices.map((choice) => choice.label)).toEqual(["Medium", "Other"]);
    expect(removedBody.choices[0].description).toBe("note");
    await expect(choiceRow(page, "High")).toHaveCount(0);
    await expect(choiceRow(page, "Medium")).toHaveAttribute("data-choice-description", "note");
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
