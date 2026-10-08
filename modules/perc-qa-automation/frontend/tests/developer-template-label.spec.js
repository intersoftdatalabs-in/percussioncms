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
 * Developer Templates — set one template label (#5407 / #1690).
 *
 * The new label shows only after the existing template PUT succeeds.
 * The update sends the label only, so the name, description, bindings, slots,
 * content-type associations, and source stay. A blank label does not clear
 * the template name. Cancel does not write. HTTP 400, 403, and 409 leave the
 * previous label.
 *
 *   npm run test:surface -- --path tests/developer-template-label.spec.js
 *
 * QA mode: perc-devctl qa-up → qa-health → TEST_CMS_URL → test:surface → qa-down.
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

const SOURCE = "<p>qa5407</p>";
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
  return `qa5407${`${a}${b}`.slice(0, 8) || "x"}`;
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

function labelText(page) {
  return page.locator('[data-testid="developer-tpl-set-label-text"]');
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

test.describe("Developer set a template label (#5407)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(180_000);
    await loginAsAdmin(page);
  });

  test("sets the label only after success and a blank label keeps the name", async ({ page }) => {
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
      await expect(labelText(page)).toHaveAttribute("data-tpl-label", `${name} label`);
      await expect(page.locator('[data-testid="developer-tpl-label"]')).toHaveValue(`${name} label`);
      await expect(page.locator('[data-testid="developer-tpl-description"]')).toHaveValue(
        "Folder list",
      );
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
      const writesBeforeLabel = updateUrls.length;

      await page.locator('[data-testid="developer-tpl-set-label-edit"]').click();
      const labelInput = page.locator('[data-testid="developer-tpl-set-label-input"]');
      await expect(labelInput).toHaveValue(`${name} label`);
      await page.locator('[data-testid="developer-tpl-set-label-save"]').click();
      await expect(labelInput).toHaveCount(0);
      expect(updateUrls.length, "the same label must not update the template").toBe(
        writesBeforeLabel,
      );

      await page.locator('[data-testid="developer-tpl-set-label-edit"]').click();
      await labelInput.fill("later");
      await expect(labelText(page)).toHaveAttribute("data-tpl-label", `${name} label`);
      await page.locator('[data-testid="developer-tpl-set-label-cancel"]').click();
      await expect(labelInput).toHaveCount(0);
      expect(updateUrls.length, "cancel must not update the template").toBe(writesBeforeLabel);
      await expect(labelText(page)).toHaveAttribute("data-tpl-label", `${name} label`);
      await expect(page.locator('[data-testid="developer-tpl-detail-name"]')).toHaveText(name);
      await expect(page.locator('[data-testid="developer-tpl-description"]')).toHaveValue(
        "Folder list",
      );
      await expect(page.locator('[data-testid="developer-tpl-ct-name-0"]')).toHaveText(CONTENT_TYPE);

      await page.locator('[data-testid="developer-tpl-set-label-edit"]').click();
      await labelInput.fill(" note ");
      await expect(labelText(page)).toHaveAttribute("data-tpl-label", `${name} label`);
      const savedDone = page.waitForResponse((response) => isTemplateWrite(response.request()));
      await page.locator('[data-testid="developer-tpl-set-label-save"]').click();
      const saved = await savedDone;
      expect(saved.status(), await saved.text()).toBe(200);
      expect(detailBody(saved.request().postDataJSON())).toEqual({ label: "note" });
      await expect(labelText(page)).toHaveAttribute("data-tpl-label", "note");
      await expect(page.locator('[data-testid="developer-tpl-label"]')).toHaveValue("note");
      await expect(page.locator('[data-testid="developer-tpl-detail-name"]')).toHaveText(name);
      await expect(page.locator('[data-testid="developer-tpl-description"]')).toHaveValue(
        "Folder list",
      );
      await expect(page.locator('[data-testid="developer-tpl-source-edit"]')).toHaveValue(SOURCE);
      await expect(page.locator('[data-testid="developer-tpl-binding-var-0"]')).toHaveValue(
        BINDING_VAR,
      );
      await expect(page.locator('[data-testid="developer-tpl-ct-name-0"]')).toHaveText(CONTENT_TYPE);
      expect(await page.locator('[data-testid="developer-tpl-slots"]').innerText()).toBe(slotsBefore);
      expect(
        await page.locator('[data-testid^="developer-tpl-slot-check-"]:checked').count(),
      ).toBe(checkedBefore);
      await expect(page.locator('[data-testid="developer-tpl-set-label-notice"]')).toBeVisible();

      const reloaded = await page.request.get(templateUrl(name));
      expect(reloaded.status()).toBe(200);
      const reloadedDetail = detailBody(await reloaded.json());
      expect((reloadedDetail.label || "").trim()).toBe("note");
      expect((reloadedDetail.description || "").trim()).toBe("Folder list");
      expect(reloadedDetail.templateSource).toBe(SOURCE);
      expect(reloadedDetail.name).toBe(name);

      await page.locator('[data-testid="developer-tpl-back"]').click();
      await expect(page.locator('[data-testid="developer-tpl-panel"]')).toBeVisible({
        timeout: 20_000,
      });
      await expect(
        page.locator(`[data-tpl-name="${name}"] [data-testid="developer-tpl-catalog-label"]`),
      ).toHaveAttribute("data-tpl-label", "note", { timeout: 20_000 });
      await openTemplate(page, name);
      await expect(labelText(page)).toHaveAttribute("data-tpl-label", "note");
      await expect(page.locator('[data-testid="developer-tpl-label"]')).toHaveValue("note");
      await expect(page.locator('[data-testid="developer-tpl-description"]')).toHaveValue(
        "Folder list",
      );
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
                name: "",
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
        const draft = page.locator('[data-testid="developer-tpl-set-label-input"]');
        if ((await draft.count()) === 0) {
          await page.locator('[data-testid="developer-tpl-set-label-edit"]').click();
        }
        await draft.fill("later");
        await page.locator('[data-testid="developer-tpl-set-label-save"]').click();
        const forcedResponse = await forcedDone;
        expect(forcedResponse.status()).toBe(status);
        await expect(page.locator('[data-testid="developer-tpl-set-label-error"]')).toContainText(
          `forced ${status}`,
        );
        await expect(labelText(page)).toHaveAttribute("data-tpl-label", "note");
        await expect(page.locator('[data-testid="developer-tpl-detail-name"]')).toHaveText(name);
        await expect(page.locator('[data-testid="developer-tpl-description"]')).toHaveValue(
          "Folder list",
        );
        await expect(page.locator('[data-testid="developer-tpl-ct-name-0"]')).toHaveText(CONTENT_TYPE);
      }
      expect(updateUrls.length).toBe(beforeForced + statuses.length);
      await page.unroute("**/services/templates/**");
      await page.locator('[data-testid="developer-tpl-set-label-cancel"]').click();

      await page.locator('[data-testid="developer-tpl-set-label-edit"]').click();
      await page.locator('[data-testid="developer-tpl-set-label-input"]').fill("   ");
      const clearedDone = page.waitForResponse((response) => isTemplateWrite(response.request()));
      await page.locator('[data-testid="developer-tpl-set-label-save"]').click();
      const cleared = await clearedDone;
      expect(cleared.status(), await cleared.text()).toBe(200);
      expect(detailBody(cleared.request().postDataJSON())).toEqual({ label: "" });
      await expect(page.locator('[data-testid="developer-tpl-detail-name"]')).toHaveText(name);
      await expect(page.locator('[data-testid="developer-tpl-description"]')).toHaveValue(
        "Folder list",
      );
      await expect(page.locator('[data-testid="developer-tpl-source-edit"]')).toHaveValue(SOURCE);
      await expect(page.locator('[data-testid="developer-tpl-binding-var-0"]')).toHaveValue(
        BINDING_VAR,
      );
      await expect(page.locator('[data-testid="developer-tpl-ct-name-0"]')).toHaveText(CONTENT_TYPE);
      await expect(page.locator('[data-testid="developer-tpl-label"]')).toHaveValue(name);
      await expect(labelText(page)).toHaveAttribute("data-tpl-label", name);

      const afterBlank = await page.request.get(templateUrl(name));
      expect(afterBlank.status()).toBe(200);
      const blankDetail = detailBody(await afterBlank.json());
      expect(blankDetail.name).toBe(name);
      expect((blankDetail.description || "").trim()).toBe("Folder list");
      expect(blankDetail.templateSource).toBe(SOURCE);
      const echoedLabel = (blankDetail.label || "").trim();
      expect(echoedLabel === "" || echoedLabel === name).toBeTruthy();

      assertClean(consoleErrors, pageErrors);
    } finally {
      await deleteTemplate(page, name);
    }
  });
});
