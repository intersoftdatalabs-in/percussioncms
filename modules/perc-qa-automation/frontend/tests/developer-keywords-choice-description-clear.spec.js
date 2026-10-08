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
 * Developer Keywords — clear the description of one choice (#5364 / #1690).
 *
 * Confirm sends an empty description on the existing keyword update. The
 * description is empty only after that save succeeds. Label, value, sequence,
 * and the other choices stay. The keyword stays. Cancel does not write.
 * Change description still refuses a blank description. HTTP 400, 403, and
 * 409 leave the previous description.
 *
 *   npm run test:surface -- --path tests/developer-keywords-choice-description-clear.spec.js
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

function clearDescriptionButton(page, label) {
  return page.locator(
    `[data-testid="developer-kw-choice-description-clear"][data-choice-label="${label}"]`,
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
  await expect(clearDescriptionButton(page, "Low")).toHaveCount(0);
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

async function setChoiceDescription(page, choiceLabel, description) {
  await changeDescriptionButton(page, choiceLabel).click();
  await page.locator('[data-testid="developer-kw-choice-description-input"]').fill(description);
  const savedDone = page.waitForResponse((response) => isKeywordUpdate(response.request()));
  await page.locator('[data-testid="developer-kw-choice-description-save"]').click();
  const saved = await savedDone;
  expect(saved.status(), await saved.text()).toBe(200);
  return keywordBody(saved.request().postDataJSON());
}

test.describe("Developer clear the description of one keyword choice (#5364)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(180_000);
    await loginAsAdmin(page);
  });

  test("clears one choice description only after confirm and still refuses a blank set", async ({
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
    const label = `NdKwDc${Date.now()}`;
    await createKeyword(page, label);
    await openKeyword(page, label);
    await expect(page.locator('[data-testid="developer-kw-description"]')).toHaveValue(
      "Item priority",
    );
    await expect(page.locator('[data-testid="developer-kw-sequence"]')).toHaveValue("4");
    await expect(clearDescriptionButton(page, "Low")).toBeEnabled();
    await expect(choiceRow(page, "Low")).toHaveAttribute("data-choice-description", "");
    await expect(choiceRow(page, "High")).toHaveAttribute("data-choice-description", "");

    const beforeEmptyClear = updateUrls.length;
    await clearDescriptionButton(page, "Low").click();
    await expect(page.locator('[data-testid="developer-catalog-confirm-body"]')).toContainText(
      "Low",
    );
    await page.locator('[data-testid="developer-catalog-confirm-submit"]').click();
    await expect(page.locator('[data-testid="developer-catalog-confirm-dialog"]')).toHaveCount(0);
    expect(updateUrls.length, "an empty description must not be cleared again").toBe(
      beforeEmptyClear,
    );

    const setBody = await setChoiceDescription(page, "Low", " note ");
    expect(setBody.description).toBe("Item priority");
    expect(setBody.sequence).toBe(4);
    expect(setBody.choices[1]).toEqual(
      expect.objectContaining({
        label: "Low",
        value: "low",
        sequence: 2,
        description: "note",
      }),
    );
    expect(setBody.choices[0].description ?? "").toBe("");
    await expect(choiceRow(page, "Low")).toHaveAttribute("data-choice-description", "note");
    await expect(choiceRow(page, "High")).toHaveAttribute("data-choice-description", "");

    const beforeBlank = updateUrls.length;
    await changeDescriptionButton(page, "Low").click();
    await page.locator('[data-testid="developer-kw-choice-description-input"]').fill("   ");
    await page.locator('[data-testid="developer-kw-choice-description-save"]').click();
    await expect(page.locator('[data-testid="developer-kw-choice-description-error"]')).toBeVisible();
    expect(updateUrls.length, "a blank description must not clear the stored description").toBe(
      beforeBlank,
    );
    await expect(choiceRow(page, "Low")).toHaveAttribute("data-choice-description", "note");
    await page.locator('[data-testid="developer-kw-choice-description-cancel"]').click();

    const beforeCancel = updateUrls.length;
    await clearDescriptionButton(page, "Low").click();
    await expect(page.locator('[data-testid="developer-catalog-confirm-body"]')).toContainText(
      "Low",
    );
    await page.locator('[data-testid="developer-catalog-confirm-cancel"]').click();
    await expect(page.locator('[data-testid="developer-catalog-confirm-dialog"]')).toHaveCount(0);
    expect(updateUrls.length, "cancel must not update the keyword").toBe(beforeCancel);
    expect(deleteUrls, "cancel must not delete the keyword").toEqual([]);
    await expect(choiceRow(page, "Low")).toHaveAttribute("data-choice-description", "note");
    await expect(choiceRow(page, "Low")).toHaveAttribute("data-choice-value", "low");
    await expect(choiceRow(page, "Low")).toHaveAttribute("data-choice-sequence", "2");
    await expect(page.locator('[data-testid="developer-kw-description"]')).toHaveValue(
      "Item priority",
    );

    await clearDescriptionButton(page, "Low").click();
    await expect(choiceRow(page, "Low")).toHaveAttribute("data-choice-description", "note");
    const clearedDone = page.waitForResponse((response) => isKeywordUpdate(response.request()));
    await page.locator('[data-testid="developer-catalog-confirm-submit"]').click();
    const cleared = await clearedDone;
    expect(cleared.status(), await cleared.text()).toBe(200);
    const clearedBody = keywordBody(cleared.request().postDataJSON());
    expect(clearedBody.label).toBe(label);
    expect(clearedBody.description).toBe("Item priority");
    expect(clearedBody.sequence).toBe(4);
    expect(clearedBody.choices[0]).toEqual(
      expect.objectContaining({ label: "High", value: "high", sequence: 1 }),
    );
    expect(clearedBody.choices[0].description ?? "").toBe("");
    expect(clearedBody.choices[1]).toEqual(
      expect.objectContaining({
        label: "Low",
        value: "low",
        sequence: 2,
        description: "",
      }),
    );
    expect(deleteUrls, "clearing a description must not delete the keyword").toEqual([]);
    await expect(choiceRow(page, "Low")).toHaveAttribute("data-choice-description", "");
    await expect(choiceRow(page, "Low")).toHaveAttribute("data-choice-label", "Low");
    await expect(choiceRow(page, "Low")).toHaveAttribute("data-choice-value", "low");
    await expect(choiceRow(page, "Low")).toHaveAttribute("data-choice-sequence", "2");
    await expect(choiceRow(page, "High")).toHaveAttribute("data-choice-description", "");
    await expect(choiceRow(page, "High")).toHaveAttribute("data-choice-value", "high");
    await expect(
      page.locator('[data-testid="developer-kw-choice-description-clear-notice"]'),
    ).toBeVisible();
    await expect(page.locator('[data-testid="developer-kw-description"]')).toHaveValue(
      "Item priority",
    );
    await expect(page.locator('[data-testid="developer-kw-sequence"]')).toHaveValue("4");

    await page.reload({ waitUntil: "networkidle" });
    await openKeywords(page);
    await openKeyword(page, label);
    await expect(choiceRow(page, "Low")).toHaveAttribute("data-choice-description", "");
    await expect(choiceRow(page, "High")).toHaveAttribute("data-choice-description", "");
    await expect(choiceRow(page, "Low")).toHaveAttribute("data-choice-value", "low");
    await expect(page.locator('[data-testid="developer-kw-description"]')).toHaveValue(
      "Item priority",
    );

    await setChoiceDescription(page, "Low", "note");
    await setChoiceDescription(page, "High", "top");
    await expect(choiceRow(page, "Low")).toHaveAttribute("data-choice-description", "note");
    await expect(choiceRow(page, "High")).toHaveAttribute("data-choice-description", "top");

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
            choices: [{ label: "Low", value: "low", description: "", sequence: 8 }],
          }),
        });
        return;
      }
      await route.continue();
    });
    for (const status of statuses) {
      const forcedDone = page.waitForResponse((response) => isKeywordUpdate(response.request()));
      await clearDescriptionButton(page, "Low").click();
      await page.locator('[data-testid="developer-catalog-confirm-submit"]').click();
      const forcedResponse = await forcedDone;
      expect(forcedResponse.status()).toBe(status);
      await expect(
        page.locator('[data-testid="developer-kw-choice-description-clear-error"]'),
      ).toContainText(`forced ${status}`);
      await expect(choiceRow(page, "Low")).toHaveAttribute("data-choice-description", "note");
      await expect(choiceRow(page, "Low")).toHaveAttribute("data-choice-label", "Low");
      await expect(choiceRow(page, "Low")).toHaveAttribute("data-choice-value", "low");
      await expect(choiceRow(page, "High")).toHaveAttribute("data-choice-description", "top");
      await expect(page.locator('[data-testid="developer-kw-description"]')).toHaveValue(
        "Item priority",
      );
    }
    expect(updateUrls.length).toBe(beforeForced + statuses.length);
    expect(deleteUrls).toEqual([]);
    await page.unroute("**/services/keywords/**");

    const keptDone = page.waitForResponse((response) => isKeywordUpdate(response.request()));
    await clearDescriptionButton(page, "Low").click();
    await page.locator('[data-testid="developer-catalog-confirm-submit"]').click();
    const kept = await keptDone;
    expect(kept.status(), await kept.text()).toBe(200);
    const keptBody = keywordBody(kept.request().postDataJSON());
    expect(keptBody.description).toBe("Item priority");
    expect(keptBody.choices[0]).toEqual(
      expect.objectContaining({
        label: "High",
        value: "high",
        sequence: 1,
        description: "top",
      }),
    );
    expect(keptBody.choices[1]).toEqual(
      expect.objectContaining({
        label: "Low",
        value: "low",
        sequence: 2,
        description: "",
      }),
    );
    await expect(choiceRow(page, "Low")).toHaveAttribute("data-choice-description", "");
    await expect(choiceRow(page, "High")).toHaveAttribute("data-choice-description", "top");

    const deleted = page.waitForResponse((response) => isKeywordDelete(response.request()));
    await page.locator('[data-testid="developer-kw-delete"]').click();
    await page.locator('[data-testid="developer-catalog-confirm-submit"]').click();
    const deleteResponse = await deleted;
    expect(deleteResponse.status()).toBe(204);
    await expect(keywordOpenButton(page, label)).toHaveCount(0);

    assertClean(consoleErrors, pageErrors);
  });
});
