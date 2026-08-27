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
 * Developer Content Type field-rule expressions chrome (CD-05–07 / #3896).
 *
 * Admin locks a type, edits validation/visibility/input/output translation
 * expression text, saves via PUT .../fields/{field}/ruleExpressions, then GET
 * reflects the expressions. Unlocked editors stay disabled; 409 lock is not
 * stolen.
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… \
 *     npm run test:surface -- --path tests/developer-content-type-field-rules.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { catalogRowSelector } = require("./helpers/developer-catalog-selectors");

const MARKER = "#3896-field-rule";

function developerContentTypesUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "content-types",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

function unwrapRuleExpressions(payload) {
  if (payload == null || typeof payload !== "object") {
    return {};
  }
  return (
    payload.ContentTypeFieldRuleExpressions ||
    payload.contentTypeFieldRuleExpressions ||
    payload
  );
}

function validationValues(env) {
  const rules = Array.isArray(env.validation) ? env.validation : env.validation ? [env.validation] : [];
  const values = [];
  for (const rule of rules) {
    const conds = Array.isArray(rule.conditionals)
      ? rule.conditionals
      : rule.conditionals
        ? [rule.conditionals]
        : [];
    for (const c of conds) {
      if (c && c.value != null) {
        values.push(String(c.value));
      }
    }
    if (rule.summary) {
      values.push(String(rule.summary));
    }
  }
  if (env.validationExpression) {
    values.push(String(env.validationExpression));
  }
  return values;
}

function attachConsoleGuards(page) {
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
  return { pageErrors, consoleErrors };
}

function assertConsoleClean(pageErrors, consoleErrors) {
  expect(pageErrors, `pageerror: ${pageErrors.join(" | ")}`).toEqual([]);
  const unexpectedConsole = consoleErrors.filter(
    (t) => !/Failed to load resource/i.test(t) && !/favicon/i.test(t),
  );
  expect(unexpectedConsole, `console error: ${unexpectedConsole.join(" | ")}`).toEqual([]);
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
  await expect(page.locator('[data-testid="developer-ct-field-rule-expressions"]')).toBeVisible({
    timeout: 30_000,
  });
  return detail;
}

async function getRuleExpressions(page, typeName, fieldName) {
  const res = await page.request.get(
    `${BASE_URL}/Rhythmyx/services/contenttypes/${encodeURIComponent(typeName)}/fields/${encodeURIComponent(fieldName)}/ruleExpressions`,
  );
  expect(res.ok(), `GET ruleExpressions HTTP ${res.status()}`).toBe(true);
  return unwrapRuleExpressions(await res.json());
}

async function releaseDesignLock(page) {
  const unlockBtn = page.locator('[data-testid="developer-ct-unlock"]');
  if (await unlockBtn.isEnabled()) {
    await unlockBtn.click();
    await expect(page.locator('[data-testid="developer-ct-lock-status"]')).toHaveText(
      /Not locked/i,
      { timeout: 20_000 },
    );
  }
}

test.describe("Developer content type field-rule expressions (CD-05-07 / #3896)", () => {
  test("unlocked expression editors stay disabled; 409 lock is not stolen", async ({
    page,
  }) => {
    test.setTimeout(120_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);
    await openContentTypeDetail(page);

    const validation = page.locator('[data-testid="developer-ct-fr-validation"]');
    const saveBtn = page.locator('[data-testid="developer-ct-save"]');
    const lockBtn = page.locator('[data-testid="developer-ct-lock"]');
    const status = page.locator('[data-testid="developer-ct-lock-status"]');

    await expect(validation).toBeVisible({ timeout: 20_000 });
    await expect(validation).toBeDisabled();
    await expect(page.locator('[data-testid="developer-ct-fr-visibility"]')).toBeDisabled();
    await expect(page.locator('[data-testid="developer-ct-fr-input"]')).toBeDisabled();
    await expect(page.locator('[data-testid="developer-ct-fr-output"]')).toBeDisabled();
    await expect(saveBtn).toBeDisabled();

    const putUrls = [];
    page.on("request", (req) => {
      if (req.method() === "PUT" && /\/ruleExpressions(?:\?|$)/.test(req.url())) {
        putUrls.push(req.url());
      }
    });
    await saveBtn.click({ force: true });
    expect(putUrls, "unlocked save must not PUT ruleExpressions").toEqual([]);

    await page.route("**/services/contenttypes/**/lock", async (route) => {
      await route.fulfill({
        status: 409,
        contentType: "application/json",
        body: JSON.stringify({ message: "Locked by another user" }),
      });
    });

    await lockBtn.click();
    await expect(page.locator('[data-testid="developer-ct-detail-error"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(status).toHaveText(/Not locked/i);
    await expect(validation).toBeDisabled();
    await expect(saveBtn).toBeDisabled();
    expect(putUrls).toEqual([]);

    assertConsoleClean(pageErrors, consoleErrors);
  });

  test("Admin lock, edit expressions, save, GET reflects", async ({ page }) => {
    test.setTimeout(180_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);
    await openContentTypeDetail(
      page,
      /percSimpleTextAsset|percFileAsset|percRawHtmlAsset|percPage/,
    );

    const typeName = (
      await page.locator('[data-testid="developer-ct-detail-name"]').innerText()
    ).trim();
    expect(typeName.length, "detail name for GET").toBeGreaterThan(0);

    const fieldSelect = page.locator('[data-testid="developer-ct-fr-field"]');
    await expect(fieldSelect).toBeVisible({ timeout: 20_000 });
    const fieldName = (await fieldSelect.inputValue()) || "sys_title";
    expect(fieldName.length, "field name for ruleExpressions").toBeGreaterThan(0);

    const validation = page.locator('[data-testid="developer-ct-fr-validation"]');
    await expect(validation).toBeVisible();
    await expect(page.locator('[data-testid="developer-ct-fr-loading"]')).toHaveCount(0, {
      timeout: 20_000,
    });

    const lockBtn = page.locator('[data-testid="developer-ct-lock"]');
    const saveBtn = page.locator('[data-testid="developer-ct-save"]');
    const unlockBtn = page.locator('[data-testid="developer-ct-unlock"]');
    const status = page.locator('[data-testid="developer-ct-lock-status"]');
    const notice = page.locator('[data-testid="developer-ct-detail-notice"]');
    const saveError = page.locator('[data-testid="developer-ct-detail-error"]');
    const frError = page.locator('[data-testid="developer-ct-fr-error"]');

    await expect(lockBtn).toBeEnabled({ timeout: 30_000 });
    const lockResponsePromise = page.waitForResponse(
      (r) => r.request().method() === "POST" && /\/contenttypes\/[^/]+\/lock(?:\?|$)/.test(r.url()),
      { timeout: 20_000 },
    );
    await lockBtn.click();
    const lockRes = await lockResponsePromise;
    if (!lockRes.ok()) {
      const body = await lockRes.text();
      throw new Error(`Lock HTTP ${lockRes.status()} ${body}`);
    }
    if (await saveError.isVisible()) {
      throw new Error(`Lock failed: ${(await saveError.innerText()).trim()}`);
    }
    await expect(status).toHaveText(/Locked by you/i, { timeout: 20_000 });
    await expect(validation).toBeEnabled();

    const originalText = await validation.inputValue();
    const markerLine = `${fieldName} <> "${MARKER}"`;
    try {
      await validation.fill(markerLine);
      await expect(saveBtn).toBeEnabled();

      const putRespPromise = page.waitForResponse(
        (res) =>
          res.request().method() === "PUT" && /\/ruleExpressions(?:\?|$)/.test(res.url()),
        { timeout: 20_000 },
      );
      await saveBtn.click();
      const putRes = await putRespPromise;
      await expect(notice.or(saveError).or(frError).first()).toBeVisible({ timeout: 20_000 });
      if (await saveError.isVisible()) {
        throw new Error(
          `Save failed: ${(await saveError.innerText()).trim()} PUT ${putRes.status()}`,
        );
      }
      if (await frError.isVisible()) {
        throw new Error(`Field-rule save failed: ${(await frError.innerText()).trim()}`);
      }
      expect(putRes.status(), `PUT ruleExpressions HTTP ${putRes.status()}`).toBe(200);
      await expect(notice).toContainText(/saved/i);
      await expect(status).toHaveText(/Locked by you/i);

      const after = await getRuleExpressions(page, typeName, fieldName);
      const afterValues = validationValues(after);
      expect(
        afterValues.some((v) => v.includes(MARKER)),
        `GET validation should include marker ${MARKER}; got ${JSON.stringify(afterValues)}`,
      ).toBe(true);

      await validation.fill(originalText);
      if (originalText !== markerLine) {
        await expect(saveBtn).toBeEnabled();
        await saveBtn.click();
        await expect(notice.or(saveError).or(frError).first()).toBeVisible({ timeout: 20_000 });
        if (await saveError.isVisible()) {
          throw new Error(`Restore save failed: ${(await saveError.innerText()).trim()}`);
        }
        if (await frError.isVisible()) {
          throw new Error(`Restore field-rule save failed: ${(await frError.innerText()).trim()}`);
        }
        await expect(notice).toContainText(/saved/i);
      }

      const restored = await getRuleExpressions(page, typeName, fieldName);
      const restoredValues = validationValues(restored);
      expect(
        restoredValues.some((v) => v.includes(MARKER)),
        `GET after restore must not keep marker; got ${JSON.stringify(restoredValues)}`,
      ).toBe(false);

      await unlockBtn.click();
      await expect(status).toHaveText(/Not locked/i, { timeout: 20_000 });
      await expect(validation).toBeDisabled();
      await expect(saveBtn).toBeDisabled();
    } finally {
      await releaseDesignLock(page);
    }

    assertConsoleClean(pageErrors, consoleErrors);
  });
});
