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
 * Developer Templates — set one template description (#5409 / #1690).
 *
 * The new description shows only after the existing template PUT succeeds.
 * The update sends the description only, so the name, label, bindings, slots,
 * content-type associations, and source stay. A blank description clears it.
 * Cancel does not write. HTTP 400, 403, and 409 leave the previous description.
 *
 *   npm run test:surface -- --path tests/developer-template-description.spec.js
 *
 * QA mode: perc-devctl qa-up → qa-health → TEST_CMS_URL → test:surface → qa-down.
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

const SOURCE = "<p>qa5409</p>";
const BINDING_VAR = "$qa";
const CONTENT_TYPE = "percPage";

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
  return `qa5409${`${a}${b}`.slice(0, 8) || "x"}`;
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

function detailBody(payload) {
  if (!payload || typeof payload !== "object") return {};
  return payload.TemplateDetail || payload.templateDetail || payload;
}

function descriptionText(page) {
  return page.locator('[data-testid="developer-tpl-set-description-text"]');
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

test.describe("Developer set a template description (#5409)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(180_000);
    await loginAsAdmin(page);
  });

  test("sets the description only after success and a blank description clears it", async ({
    page,
  }) => {
    const { consoleErrors, pageErrors } = attachConsole(page);
    const name = uniqueTemplateName();
    const updateUrls = [];
    page.on("request", (request) => {
      if (isTemplateWrite(request)) {
        updateUrls.push(request.url());
      }
    });

    try {
      await createTemplate(page, name);
      await openTemplates(page);
      await openTemplate(page, name);
      await expect(descriptionText(page)).toHaveAttribute("data-tpl-description", "Folder list");
      await expect(page.locator('[data-testid="developer-tpl-description"]')).toHaveValue(
        "Folder list",
      );
      await expect(page.locator('[data-testid="developer-tpl-label"]')).toHaveValue(`${name} label`);
      await expect(page.locator('[data-testid="developer-tpl-source-edit"]')).toHaveValue(SOURCE);

      await lockTemplate(page);
      await page.locator('[data-testid="developer-tpl-binding-add"]').click();
      await page.locator('[data-testid="developer-tpl-binding-var-0"]').fill(BINDING_VAR);
      await page.locator('[data-testid="developer-tpl-binding-expr-0"]').fill("1");
      await page.locator('[data-testid="developer-tpl-ct-input"]').fill(CONTENT_TYPE);
      await page.locator('[data-testid="developer-tpl-ct-add"]').click();
      await expect(page.locator('[data-testid="developer-tpl-ct-name-0"]')).toHaveText(CONTENT_TYPE);
      const assocSaved = page.waitForResponse((response) => isTemplateWrite(response.request()));
      await page.locator('[data-testid="developer-tpl-save"]').click();
      const assocResponse = await assocSaved;
      expect(assocResponse.status(), await assocResponse.text()).toBe(200);
      await expect(page.locator('[data-testid="developer-tpl-detail-notice"]')).toBeVisible({
        timeout: 20_000,
      });
      await expect(page.locator('[data-testid="developer-tpl-ct-name-0"]')).toHaveText(CONTENT_TYPE);
      await expect(page.locator('[data-testid="developer-tpl-binding-var-0"]')).toHaveValue(
        BINDING_VAR,
      );

      const slotsBefore = await page.locator('[data-testid="developer-tpl-slots"]').innerText();
      const checkedBefore = await page
        .locator('[data-testid^="developer-tpl-slot-check-"]:checked')
        .count();
      const writesBeforeDescription = updateUrls.length;

      await page.locator('[data-testid="developer-tpl-set-description-edit"]').click();
      const descriptionInput = page.locator('[data-testid="developer-tpl-set-description-input"]');
      await expect(descriptionInput).toHaveValue("Folder list");
      await page.locator('[data-testid="developer-tpl-set-description-save"]').click();
      await expect(descriptionInput).toHaveCount(0);
      expect(updateUrls.length, "the same description must not update the template").toBe(
        writesBeforeDescription,
      );

      await page.locator('[data-testid="developer-tpl-set-description-edit"]').click();
      await descriptionInput.fill("later");
      await expect(descriptionText(page)).toHaveAttribute("data-tpl-description", "Folder list");
      await page.locator('[data-testid="developer-tpl-set-description-cancel"]').click();
      await expect(descriptionInput).toHaveCount(0);
      expect(updateUrls.length, "cancel must not update the template").toBe(writesBeforeDescription);
      await expect(descriptionText(page)).toHaveAttribute("data-tpl-description", "Folder list");
      await expect(page.locator('[data-testid="developer-tpl-label"]')).toHaveValue(`${name} label`);
      await expect(page.locator('[data-testid="developer-tpl-ct-name-0"]')).toHaveText(CONTENT_TYPE);

      await page.locator('[data-testid="developer-tpl-set-description-edit"]').click();
      await descriptionInput.fill(" note ");
      await expect(descriptionText(page)).toHaveAttribute("data-tpl-description", "Folder list");
      const savedDone = page.waitForResponse((response) => isTemplateWrite(response.request()));
      await page.locator('[data-testid="developer-tpl-set-description-save"]').click();
      const saved = await savedDone;
      expect(saved.status(), await saved.text()).toBe(200);
      expect(detailBody(saved.request().postDataJSON())).toEqual({ description: "note" });
      await expect(descriptionText(page)).toHaveAttribute("data-tpl-description", "note");
      await expect(page.locator('[data-testid="developer-tpl-description"]')).toHaveValue("note");
      await expect(page.locator('[data-testid="developer-tpl-detail-name"]')).toHaveText(name);
      await expect(page.locator('[data-testid="developer-tpl-label"]')).toHaveValue(`${name} label`);
      await expect(page.locator('[data-testid="developer-tpl-source-edit"]')).toHaveValue(SOURCE);
      await expect(page.locator('[data-testid="developer-tpl-binding-var-0"]')).toHaveValue(
        BINDING_VAR,
      );
      await expect(page.locator('[data-testid="developer-tpl-ct-name-0"]')).toHaveText(CONTENT_TYPE);
      expect(await page.locator('[data-testid="developer-tpl-slots"]').innerText()).toBe(slotsBefore);
      expect(
        await page.locator('[data-testid^="developer-tpl-slot-check-"]:checked').count(),
      ).toBe(checkedBefore);
      await expect(page.locator('[data-testid="developer-tpl-set-description-notice"]')).toBeVisible();

      const reloaded = await page.request.get(templateUrl(name));
      expect(reloaded.status()).toBe(200);
      const reloadedDetail = detailBody(await reloaded.json());
      expect((reloadedDetail.description || "").trim()).toBe("note");
      expect(reloadedDetail.label).toBe(`${name} label`);
      expect(reloadedDetail.templateSource).toBe(SOURCE);
      expect(reloadedDetail.name).toBe(name);

      await page.locator('[data-testid="developer-tpl-back"]').click();
      await expect(page.locator('[data-testid="developer-tpl-panel"]')).toBeVisible({
        timeout: 20_000,
      });
      await expect(
        page.locator(
          `[data-testid="developer-tpl-catalog-description"][data-tpl-name="${name}"]`,
        ),
      ).toHaveAttribute("data-tpl-description", "note", { timeout: 20_000 });
      await openTemplate(page, name);
      await expect(descriptionText(page)).toHaveAttribute("data-tpl-description", "note");
      await expect(page.locator('[data-testid="developer-tpl-description"]')).toHaveValue("note");
      await expect(page.locator('[data-testid="developer-tpl-ct-name-0"]')).toHaveText(CONTENT_TYPE);
      await expect(page.locator('[data-testid="developer-tpl-binding-var-0"]')).toHaveValue(
        BINDING_VAR,
      );
      await expect(page.locator('[data-testid="developer-tpl-source-edit"]')).toHaveValue(SOURCE);

      await lockTemplate(page);
      const beforeForced = updateUrls.length;
      let forced = 0;
      const statuses = [400, 403, 409];
      await page.route("**/services/templates/**", async (route) => {
        const request = route.request();
        if (isTemplateWrite(request) && forced < statuses.length) {
          const status = statuses[forced];
          forced += 1;
          await route.fulfill({
            status,
            contentType: "application/json",
            body: JSON.stringify({
              message: `forced ${status}`,
              TemplateDetail: {
                name: "Renamed",
                label: "Renamed",
                description: "changed",
                templateSource: "<p>changed</p>",
                bindings: [],
                slots: [],
                associatedContentTypes: [],
              },
            }),
          });
          return;
        }
        await route.continue();
      });
      for (const status of statuses) {
        const forcedDone = page.waitForResponse((response) => isTemplateWrite(response.request()));
        const draft = page.locator('[data-testid="developer-tpl-set-description-input"]');
        if ((await draft.count()) === 0) {
          await page.locator('[data-testid="developer-tpl-set-description-edit"]').click();
        }
        await draft.fill("later");
        await page.locator('[data-testid="developer-tpl-set-description-save"]').click();
        const forcedResponse = await forcedDone;
        expect(forcedResponse.status()).toBe(status);
        await expect(page.locator('[data-testid="developer-tpl-set-description-error"]')).toContainText(
          `forced ${status}`,
        );
        await expect(descriptionText(page)).toHaveAttribute("data-tpl-description", "note");
        await expect(page.locator('[data-testid="developer-tpl-detail-name"]')).toHaveText(name);
        await expect(page.locator('[data-testid="developer-tpl-label"]')).toHaveValue(`${name} label`);
        await expect(page.locator('[data-testid="developer-tpl-ct-name-0"]')).toHaveText(CONTENT_TYPE);
      }
      expect(updateUrls.length).toBe(beforeForced + statuses.length);
      await page.unroute("**/services/templates/**");
      await page.locator('[data-testid="developer-tpl-set-description-cancel"]').click();

      await page.locator('[data-testid="developer-tpl-set-description-edit"]').click();
      await page.locator('[data-testid="developer-tpl-set-description-input"]').fill("   ");
      const clearedDone = page.waitForResponse((response) => isTemplateWrite(response.request()));
      await page.locator('[data-testid="developer-tpl-set-description-save"]').click();
      const cleared = await clearedDone;
      expect(cleared.status(), await cleared.text()).toBe(200);
      expect(detailBody(cleared.request().postDataJSON())).toEqual({ description: "" });
      await expect(descriptionText(page)).toHaveAttribute("data-tpl-description", "");
      await expect(page.locator('[data-testid="developer-tpl-detail-name"]')).toHaveText(name);
      await expect(page.locator('[data-testid="developer-tpl-label"]')).toHaveValue(`${name} label`);
      await expect(page.locator('[data-testid="developer-tpl-source-edit"]')).toHaveValue(SOURCE);
      await expect(page.locator('[data-testid="developer-tpl-binding-var-0"]')).toHaveValue(
        BINDING_VAR,
      );
      await expect(page.locator('[data-testid="developer-tpl-ct-name-0"]')).toHaveText(CONTENT_TYPE);

      assertClean(consoleErrors, pageErrors);
    } finally {
      await deleteTemplate(page, name);
    }
  });
});
