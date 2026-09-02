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
 * Developer Slots finder / relationship / arguments save (#4059 AS-01 / parent #1690).
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… \
 *     npm run test:surface -- --path tests/developer-slot-finder-editor.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { confirmDeveloperCatalogDelete } = require("./helpers/developer-catalog-confirm");

const VALID_FINDER =
  "Java/global/percussion/slotcontentfinder/sys_RelationshipContentFinder";
/** Catalog name is ActiveAssembly (category label is "Active Assembly"). */
const VALID_RELATIONSHIP = "ActiveAssembly";

function developerSlotsUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "slots",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

/** Unique slot name: no spaces (REST create rule). */
function uniqueSlotName() {
  const a = Date.now().toString(36).replace(/[^a-z0-9]/g, "").slice(-4);
  const b = Math.random().toString(36).replace(/[^a-z0-9]/g, "").slice(2, 6);
  const suffix = `${a}${b}`.slice(0, 8);
  return `qa4059${suffix || "slot"}`;
}

async function openSlotsCatalog(page) {
  await page.goto(developerSlotsUrl(), { waitUntil: "networkidle" });
  await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
    timeout: 20_000,
  });
  const panel = page.locator('[data-testid="developer-slot-panel"]');
  const empty = page.locator('[data-testid="developer-slot-empty"]');
  const listError = page.locator('[data-testid="developer-slot-error"]');
  await expect(panel.or(empty).or(listError).first()).toBeVisible({
    timeout: 30_000,
  });
  if (await listError.isVisible()) {
    throw new Error(
      `Developer slots catalog error: ${(await listError.innerText()).trim()}`,
    );
  }
  await expect(page.locator('[data-testid="developer-slot-new"]')).toBeVisible();
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
  expect(
    unexpectedConsole,
    `console error: ${unexpectedConsole.join(" | ")}`,
  ).toEqual([]);
}

async function createSlot(page, name, label) {
  await page.locator('[data-testid="developer-slot-new"]').click();
  await expect(page.locator('[data-testid="developer-slot-detail"]')).toBeVisible();
  await page.locator('[data-testid="developer-slot-name"]').fill(name);
  await page.locator('[data-testid="developer-slot-label"]').fill(label);
  await page.locator('[data-testid="developer-slot-save"]').click();
  const notice = page.locator('[data-testid="developer-slot-detail-notice"]');
  const saveError = page.locator('[data-testid="developer-slot-detail-error"]');
  await expect(notice).toBeVisible({ timeout: 20_000 });
  if (await saveError.isVisible()) {
    throw new Error(`Create failed: ${(await saveError.innerText()).trim()}`);
  }
}

async function lockSlot(page) {
  await page.locator('[data-testid="developer-slot-lock"]').click();
  await expect(page.locator('[data-testid="developer-slot-finder"]')).toBeEnabled({
    timeout: 20_000,
  });
}

async function deleteCurrentSlot(page, name) {
  const del = page.locator('[data-testid="developer-slot-delete"]');
  if (await del.isVisible()) {
    await del.click();
    await confirmDeveloperCatalogDelete(page);
    const panel = page.locator('[data-testid="developer-slot-panel"]');
    const err = page.locator('[data-testid="developer-slot-detail-error"]');
    await expect(panel.or(err).first()).toBeVisible({ timeout: 20_000 });
    if (await err.isVisible()) {
      const unlock = page.locator('[data-testid="developer-slot-unlock"]');
      if (await unlock.isEnabled()) {
        await unlock.click();
        await page.locator('[data-testid="developer-slot-delete"]').click();
        await confirmDeveloperCatalogDelete(page);
        await expect(page.locator('[data-testid="developer-slot-panel"]')).toBeVisible({
          timeout: 20_000,
        });
      }
    }
    await expect(page.locator(`[data-slot-name="${name}"]`)).toHaveCount(0);
  }
}

test.describe("Developer slot finder editor (#4059 / AS-01)", () => {
  test("Admin can lock a slot and save finder, relationship, and arguments", async ({
    page,
  }) => {
    test.setTimeout(180_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);
    await loginAsAdmin(page);
    await openSlotsCatalog(page);

    const name = uniqueSlotName();
    const label = `QA 4059 ${name}`;
    await createSlot(page, name, label);

    await expect(page.locator('[data-testid="developer-slot-finder"]')).toBeDisabled();
    await lockSlot(page);

    const putBodies = [];
    page.on("request", (req) => {
      if (req.method() === "PUT" && /\/services\/slots\//.test(req.url())) {
        putBodies.push(req.postData() || "");
      }
    });

    await page.locator('[data-testid="developer-slot-finder"]').fill(VALID_FINDER);
    await page.locator('[data-testid="developer-slot-relationship"]').fill(
      VALID_RELATIONSHIP,
    );
    await page.locator('[data-testid="developer-slot-arg-key"]').fill("type");
    await page.locator('[data-testid="developer-slot-arg-value"]').fill("qa4059");
    await page.locator('[data-testid="developer-slot-arg-add"]').click();
    await expect(page.locator('[data-testid="developer-slot-arg-key-0"]')).toHaveValue(
      "type",
    );

    await page.locator('[data-testid="developer-slot-save"]').click();
    const finderNotice = page.locator('[data-testid="developer-slot-detail-notice"]');
    const saveError = page.locator('[data-testid="developer-slot-detail-error"]');
    await expect(finderNotice.or(saveError).first()).toBeVisible({ timeout: 20_000 });
    if (await saveError.isVisible()) {
      throw new Error(`Finder save failed: ${(await saveError.innerText()).trim()}`);
    }
    await expect(page.locator('[data-testid="developer-slot-finder"]')).toHaveValue(
      VALID_FINDER,
    );
    await expect(page.locator('[data-testid="developer-slot-relationship"]')).toHaveValue(
      VALID_RELATIONSHIP,
    );

    await page.locator('[data-testid="developer-slot-unlock"]').click();
    await expect(page.locator('[data-testid="developer-slot-finder"]')).toBeDisabled({
      timeout: 20_000,
    });
    await page.locator('[data-testid="developer-slot-back"]').click();
    await expect(page.locator(`[data-slot-name="${name}"]`)).toBeVisible({
      timeout: 20_000,
    });
    await page.locator(`[data-slot-name="${name}"]`).click();
    await expect(page.locator('[data-testid="developer-slot-detail"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-slot-finder"]')).toHaveValue(
      VALID_FINDER,
    );
    await expect(page.locator('[data-testid="developer-slot-relationship"]')).toHaveValue(
      VALID_RELATIONSHIP,
    );
    const finderPut = putBodies[0] || "";
    expect(finderPut, "finder PUT should include finderName").toMatch(/sys_RelationshipContentFinder/);
    expect(finderPut, "finder PUT should include relationshipName").toMatch(/ActiveAssembly/);
    expect(finderPut, "finder PUT should include finder argument type=qa4059").toMatch(/qa4059/);

    const nextLabel = `${label} props`;
    await page.locator('[data-testid="developer-slot-label"]').fill(nextLabel);
    await page.locator('[data-testid="developer-slot-save"]').click();
    const propsNotice = page.locator('[data-testid="developer-slot-detail-notice"]');
    const propsError = page.locator('[data-testid="developer-slot-detail-error"]');
    await expect(propsNotice.or(propsError).first()).toBeVisible({ timeout: 20_000 });
    if (await propsError.isVisible()) {
      throw new Error(`Properties save failed: ${(await propsError.innerText()).trim()}`);
    }
    await expect(page.locator('[data-testid="developer-slot-finder"]')).toHaveValue(
      VALID_FINDER,
    );
    const propsPut = putBodies[putBodies.length - 1] || "";
    expect(propsPut, "properties-only PUT must omit finderName").not.toMatch(
      /"finderName"/,
    );

    await lockSlot(page);
    await page.locator('[data-testid="developer-slot-relationship"]').fill("");
    await page.locator('[data-testid="developer-slot-save"]').click();
    await expect(page.locator('[data-testid="developer-slot-detail-notice"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.locator('[data-testid="developer-slot-relationship"]')).toHaveValue(
      "",
    );

    await page.locator('[data-testid="developer-slot-finder"]').fill("nope");
    await page.locator('[data-testid="developer-slot-save"]').click();
    const finderErr = page.locator('[data-testid="developer-slot-detail-error"]');
    await expect(finderErr).toBeVisible({ timeout: 20_000 });
    await expect(finderErr).toContainText(
      /invalid finder|400|extension name not valid/i,
    );

    if (await page.locator('[data-testid="developer-slot-unlock"]').isEnabled()) {
      await page.locator('[data-testid="developer-slot-unlock"]').click();
    }
    await deleteCurrentSlot(page, name);

    assertConsoleClean(pageErrors, consoleErrors);
  });
});
