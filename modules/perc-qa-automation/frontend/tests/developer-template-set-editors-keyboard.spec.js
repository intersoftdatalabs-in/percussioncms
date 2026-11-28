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
 * Developer Templates — set-label / set-description editor keyboard path
 * (#7390 / parent #1690; mirrors slot #6713 / #6733 + display-format #6630).
 *
 * The label and description inputs autofocus when the editor opens, and
 * Escape cancels the edit without writing (same as Cancel): no PUT fires
 * and the stored value is kept. When an editor closes (Escape, Cancel, or
 * a successful save) focus returns to the invoking edit button instead of
 * falling back to <body>.
 *
 *   npm run test:surface -- --path tests/developer-template-set-editors-keyboard.spec.js
 *
 * QA mode: perc-devctl qa-up → qa-health → TEST_CMS_URL → test:surface → qa-down.
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

const SOURCE = "<p>qa7390</p>";

function developerTemplatesUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "templates",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

function uniqueTemplateName() {
  const a = Date.now().toString(36).replace(/[^a-z0-9]/g, "").slice(-4);
  const b = Math.random().toString(36).replace(/[^a-z0-9]/g, "").slice(2, 6);
  return `qa7390${`${a}${b}`.slice(0, 8) || "x"}`;
}

function templateUrl(name) {
  return `${BASE_URL}/Rhythmyx/services/templates/${encodeURIComponent(name)}`;
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
      /Failed to load resource: the server responded with a status of (400|403|409)/.test(text)
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

function isTemplateWrite(request) {
  if (request.method() !== "PUT") return false;
  const path = request.url().split("?")[0];
  return /\/services\/templates\/[^/]+$/.test(path);
}

async function createTemplate(page, name) {
  const created = await page.request.post(`${BASE_URL}/Rhythmyx/services/templates`, {
    headers: {
      Accept: "application/json",
      "Content-Type": "application/json",
    },
    data: {
      TemplateDetail: {
        name,
        label: `${name} label`,
        description: "Folder list",
        templateSource: SOURCE,
      },
    },
  });
  const createdText = await created.text();
  expect(created.ok(), `create template HTTP ${created.status()} ${createdText}`).toBeTruthy();
}

async function deleteTemplate(page, name) {
  if (!name) return;
  await page.request.post(`${templateUrl(name)}/lock`);
  await page.request.put(templateUrl(name), {
    headers: {
      Accept: "application/json",
      "Content-Type": "application/json",
    },
    data: { TemplateDetail: { associatedContentTypes: [] } },
  });
  await page.request.post(`${templateUrl(name)}/unlock`);
  await page.request.delete(templateUrl(name));
}

async function openTemplates(page) {
  await page.goto(developerTemplatesUrl(), { waitUntil: "networkidle" });
  await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
    timeout: 20_000,
  });
  const panel = page.locator('[data-testid="developer-tpl-panel"]');
  const empty = page.locator('[data-testid="developer-tpl-empty"]');
  const listError = page.locator('[data-testid="developer-tpl-error"]');
  await expect(panel.or(empty).or(listError).first()).toBeVisible({
    timeout: 30_000,
  });
  if (await listError.isVisible()) {
    throw new Error(
      `Developer templates catalog error: ${(await listError.innerText()).trim()}`,
    );
  }
}

async function openTemplate(page, name) {
  const row = page.locator(`[data-tpl-name="${name}"]`).first();
  await expect(row).toBeVisible({ timeout: 20_000 });
  const openBtn = row.locator('button[aria-label^="Open "]');
  if (await openBtn.count()) {
    await openBtn.click();
  } else {
    await row.click();
  }
  await expect(page.locator('[data-testid="developer-tpl-detail"]')).toBeVisible({
    timeout: 30_000,
  });
  await expect(page.locator('[data-testid="developer-tpl-detail-name"]')).toHaveText(name);
}

async function lockTemplate(page) {
  const lockBtn = page.locator('[data-testid="developer-tpl-lock"]');
  const status = page.locator('[data-testid="developer-tpl-lock-status"]');
  const waitLock = () =>
    page.waitForResponse(
      (res) =>
        res.request().method() === "POST" && /\/templates\/[^/?]+\/lock(?:\?|$)/.test(res.url()),
      { timeout: 20_000 },
    );
  let lockWait = waitLock();
  await lockBtn.click();
  let lockHttp = await lockWait;
  if (lockHttp.status() === 409) {
    const name = (await page.locator('[data-testid="developer-tpl-detail-name"]').innerText()).trim();
    await page.request.post(`${templateUrl(name)}/unlock`);
    lockWait = waitLock();
    await lockBtn.click();
    lockHttp = await lockWait;
  }
  expect(lockHttp.status(), `lock HTTP ${lockHttp.status()}`).toBe(200);
  await expect(status).toHaveText(/Locked by you/i, { timeout: 20_000 });
}

test.describe("Developer template set-label/set-description keyboard path (#7390)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(180_000);
    await loginAsAdmin(page);
  });

  test("label input autofocuses and Escape cancels without writing", async ({ page }) => {
    const { consoleErrors, pageErrors } = attachConsole(page);
    const name = uniqueTemplateName();
    try {
      await createTemplate(page, name);
      await openTemplates(page);
      await openTemplate(page, name);

      const writes = [];
      page.on("request", (request) => {
        if (isTemplateWrite(request)) {
          writes.push(request.url());
        }
      });

      await page.locator('[data-testid="developer-tpl-set-label-edit"]').click();
      const labelInput = page.locator('[data-testid="developer-tpl-set-label-input"]');
      await expect(labelInput).toBeFocused();
      await labelInput.fill("not-saved");
      await labelInput.press("Escape");
      await expect(labelInput).toHaveCount(0);
      expect(writes, "Escape must not update the template").toEqual([]);
      await expect(page.locator('[data-testid="developer-tpl-set-label-text"]')).toHaveAttribute(
        "data-tpl-label",
        `${name} label`,
      );
      await page.screenshot({ path: `test-results/live-proof-7390-tpl-label-keyboard.png` });
    } finally {
      await deleteTemplate(page, name);
    }
    assertClean(consoleErrors, pageErrors);
  });

  test("description input autofocuses and Escape cancels without writing", async ({ page }) => {
    const { consoleErrors, pageErrors } = attachConsole(page);
    const name = uniqueTemplateName();
    try {
      await createTemplate(page, name);
      await openTemplates(page);
      await openTemplate(page, name);

      const writes = [];
      page.on("request", (request) => {
        if (isTemplateWrite(request)) {
          writes.push(request.url());
        }
      });

      await page.locator('[data-testid="developer-tpl-set-description-edit"]').click();
      const descriptionInput = page.locator('[data-testid="developer-tpl-set-description-input"]');
      await expect(descriptionInput).toBeFocused();
      await descriptionInput.fill("not-saved");
      await descriptionInput.press("Escape");
      await expect(descriptionInput).toHaveCount(0);
      expect(writes, "Escape must not update the template").toEqual([]);
      await expect(
        page.locator('[data-testid="developer-tpl-set-description-text"]'),
      ).toHaveAttribute("data-tpl-description", "Folder list");
      await page.screenshot({ path: `test-results/live-proof-7390-tpl-description-keyboard.png` });
    } finally {
      await deleteTemplate(page, name);
    }
    assertClean(consoleErrors, pageErrors);
  });
});

test.describe("Developer template set-label/set-description focus return (#7390)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(180_000);
    await loginAsAdmin(page);
  });

  test("label editor returns focus to the edit button after Escape, Cancel, and Save", async ({
    page,
  }) => {
    const { consoleErrors, pageErrors } = attachConsole(page);
    const name = uniqueTemplateName();
    try {
      await createTemplate(page, name);
      await openTemplates(page);
      await openTemplate(page, name);
      await lockTemplate(page);

      // Escape closes the editor and returns focus without writing.
      await page.locator('[data-testid="developer-tpl-set-label-edit"]').click();
      const labelInput = page.locator('[data-testid="developer-tpl-set-label-input"]');
      await expect(labelInput).toBeFocused();
      await labelInput.fill("not-saved");
      await labelInput.press("Escape");
      await expect(labelInput).toHaveCount(0);
      await expect(page.locator('[data-testid="developer-tpl-set-label-edit"]')).toBeFocused();
      await expect(page.locator('[data-testid="developer-tpl-set-label-text"]')).toHaveAttribute(
        "data-tpl-label",
        `${name} label`,
      );

      // Cancel closes the editor and returns focus without writing.
      await page.locator('[data-testid="developer-tpl-set-label-edit"]').click();
      await expect(page.locator('[data-testid="developer-tpl-set-label-input"]')).toBeFocused();
      await page.locator('[data-testid="developer-tpl-set-label-input"]').fill("not-saved");
      await page.locator('[data-testid="developer-tpl-set-label-cancel"]').click();
      await expect(page.locator('[data-testid="developer-tpl-set-label-input"]')).toHaveCount(0);
      await expect(page.locator('[data-testid="developer-tpl-set-label-edit"]')).toBeFocused();
      await expect(page.locator('[data-testid="developer-tpl-set-label-text"]')).toHaveAttribute(
        "data-tpl-label",
        `${name} label`,
      );

      // Save writes, then returns focus to the edit button.
      await page.locator('[data-testid="developer-tpl-set-label-edit"]').click();
      await page.locator('[data-testid="developer-tpl-set-label-input"]').fill("focus-note");
      const saved = page.waitForResponse((response) => isTemplateWrite(response.request()));
      await page.locator('[data-testid="developer-tpl-set-label-save"]').click();
      const saveResponse = await saved;
      expect(saveResponse.status(), await saveResponse.text()).toBe(200);
      await expect(page.locator('[data-testid="developer-tpl-set-label-input"]')).toHaveCount(0);
      await expect(page.locator('[data-testid="developer-tpl-set-label-edit"]')).toBeFocused();
      await expect(page.locator('[data-testid="developer-tpl-set-label-text"]')).toHaveAttribute(
        "data-tpl-label",
        "focus-note",
      );
      await expect(page.locator('[data-testid="developer-tpl-set-label-notice"]')).toHaveAttribute(
        "role",
        "status",
      );
      await page.screenshot({ path: `test-results/live-proof-7390-tpl-label-focus-return.png` });
    } finally {
      await deleteTemplate(page, name);
    }
    assertClean(consoleErrors, pageErrors);
  });

  test("description editor returns focus to the edit button after Escape, Cancel, and Save", async ({
    page,
  }) => {
    const { consoleErrors, pageErrors } = attachConsole(page);
    const name = uniqueTemplateName();
    try {
      await createTemplate(page, name);
      await openTemplates(page);
      await openTemplate(page, name);
      await lockTemplate(page);

      // Escape closes the editor and returns focus without writing.
      await page.locator('[data-testid="developer-tpl-set-description-edit"]').click();
      const descriptionInput = page.locator('[data-testid="developer-tpl-set-description-input"]');
      await expect(descriptionInput).toBeFocused();
      await descriptionInput.fill("not-saved");
      await descriptionInput.press("Escape");
      await expect(descriptionInput).toHaveCount(0);
      await expect(
        page.locator('[data-testid="developer-tpl-set-description-edit"]'),
      ).toBeFocused();
      await expect(
        page.locator('[data-testid="developer-tpl-set-description-text"]'),
      ).toHaveAttribute("data-tpl-description", "Folder list");

      // Cancel closes the editor and returns focus without writing.
      await page.locator('[data-testid="developer-tpl-set-description-edit"]').click();
      await expect(
        page.locator('[data-testid="developer-tpl-set-description-input"]'),
      ).toBeFocused();
      await page.locator('[data-testid="developer-tpl-set-description-input"]').fill("not-saved");
      await page.locator('[data-testid="developer-tpl-set-description-cancel"]').click();
      await expect(
        page.locator('[data-testid="developer-tpl-set-description-input"]'),
      ).toHaveCount(0);
      await expect(
        page.locator('[data-testid="developer-tpl-set-description-edit"]'),
      ).toBeFocused();
      await expect(
        page.locator('[data-testid="developer-tpl-set-description-text"]'),
      ).toHaveAttribute("data-tpl-description", "Folder list");

      // Save writes, then returns focus to the edit button.
      await page.locator('[data-testid="developer-tpl-set-description-edit"]').click();
      await page.locator('[data-testid="developer-tpl-set-description-input"]').fill("focus-note");
      const saved = page.waitForResponse((response) => isTemplateWrite(response.request()));
      await page.locator('[data-testid="developer-tpl-set-description-save"]').click();
      const saveResponse = await saved;
      expect(saveResponse.status(), await saveResponse.text()).toBe(200);
      await expect(
        page.locator('[data-testid="developer-tpl-set-description-input"]'),
      ).toHaveCount(0);
      await expect(
        page.locator('[data-testid="developer-tpl-set-description-edit"]'),
      ).toBeFocused();
      await expect(
        page.locator('[data-testid="developer-tpl-set-description-text"]'),
      ).toHaveAttribute("data-tpl-description", "focus-note");
      await expect(
        page.locator('[data-testid="developer-tpl-set-description-notice"]'),
      ).toHaveAttribute("role", "status");
      await page.screenshot({
        path: `test-results/live-proof-7390-tpl-description-focus-return.png`,
      });
    } finally {
      await deleteTemplate(page, name);
    }
    assertClean(consoleErrors, pageErrors);
  });
});
