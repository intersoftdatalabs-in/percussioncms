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
 * PublishingShell Design — reorder publish editions by priority (#5885 / parent #4531).
 *
 * Move up/down steps the stored priority by one through the existing
 * priority-only edition PUT; the list is priority-ordered, so the stored
 * write persists the new order. Cancel and HTTP 400/403/409 keep the
 * previous order.
 *
 * Surface filter (H2 QA / agent path):
 *   cd modules/perc-qa-automation/frontend
 *   npm run test:surface -- --path tests/publishing/designEditionPriorityReorder.spec.js
 *
 * QA mode: perc-devctl qa-up → qa-health → TEST_CMS_URL → test:surface → qa-down.
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("../helpers/auth");

const EDITIONS_PUT =
  "**/services/sitemanage/publishingdesign/editions/**";

function editionUpdatePut(url, method) {
  if (method !== "PUT") {
    return false;
  }
  try {
    return /\/publishingdesign\/editions\/[^/]+$/.test(new URL(url).pathname);
  } catch {
    return false;
  }
}

function trackJsErrors(page) {
  const jsErrors = [];
  page.on("pageerror", (err) => jsErrors.push(String(err)));
  page.on("console", (msg) => {
    if (msg.type() !== "error") {
      return;
    }
    const text = msg.text();
    if (
      text.includes("status of 400") ||
      text.includes("status of 403") ||
      text.includes("status of 409")
    ) {
      return;
    }
    jsErrors.push(text);
  });
  return jsErrors;
}

async function openDesign(page) {
  await page.goto(
    `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=publish&section=design&_=${Date.now()}`,
  );
  await expect(page.getByTestId("publishing-shell")).toBeVisible({
    timeout: 30000,
  });
  await expect(page.getByTestId("publish-section-design")).toBeVisible();
}

async function createEdition(page, name, comment, priority) {
  await page.getByRole("tab", { name: /Editions/i }).click();
  await page.getByTestId("design-add-edition").click();
  await expect(page.getByTestId("edition-editor")).toBeVisible();
  await page.locator("#ed-name").fill(name);
  await page.locator("#ed-comment").fill(comment);
  await page.locator("#ed-priority").fill(String(priority));
  await page.getByTestId("edition-save").click();
  await expect(page.getByTestId("edition-editor")).toBeHidden({
    timeout: 20000,
  });
  await expect(page.getByRole("button", { name, exact: true })).toBeVisible({
    timeout: 20000,
  });
}

function editionRow(page, name) {
  return page.locator("li").filter({
    has: page.getByRole("button", { name, exact: true }),
  });
}

async function editionNames(page) {
  return page.locator("ul li").allTextContents();
}

async function editionIndex(page, name) {
  const names = await editionNames(page);
  return names.findIndex((text) => text.includes(name));
}

async function expectRelativeOrder(page, first, second) {
  const firstIndex = await editionIndex(page, first);
  const secondIndex = await editionIndex(page, second);
  expect(firstIndex).toBeGreaterThanOrEqual(0);
  expect(secondIndex).toBeGreaterThanOrEqual(0);
  expect(firstIndex).toBeLessThan(secondIndex);
}

test.describe("PublishingShell Design reorder publish editions", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test("moves the edition only after the update succeeds and the order persists", async ({
    page,
  }) => {
    const jsErrors = trackJsErrors(page);
    const putBodies = [];
    page.on("request", (req) => {
      if (editionUpdatePut(req.url(), req.method())) {
        putBodies.push(req.postData() || "");
      }
    });

    const stamp = Date.now();
    // Tie-break trap: after the move both editions share priority 3, so the
    // moved edition's name must sort first for the flip to be visible.
    const lower = `NightReAaa-${stamp}`;
    const upper = `NightReZzz-${stamp}`;

    await openDesign(page);
    await createEdition(page, lower, `low-${stamp}`, 2);
    await createEdition(page, upper, `high-${stamp}`, 3);

    // Priority-ordered: highest first.
    await expectRelativeOrder(page, upper, lower);

    let releasePut;
    const putGate = new Promise((resolve) => {
      releasePut = resolve;
    });
    let markSent;
    const sent = new Promise((resolve) => {
      markSent = resolve;
    });
    await page.route(EDITIONS_PUT, async (route) => {
      const req = route.request();
      let path = "";
      try {
        path = new URL(req.url()).pathname;
      } catch {
        path = "";
      }
      if (req.method() === "PUT" && /\/editions\/[^/]+$/.test(path)) {
        markSent();
        await putGate;
      }
      await route.continue();
    });

    page.once("dialog", async (dialog) => {
      expect(dialog.type()).toBe("confirm");
      expect(dialog.message()).toMatch(/Move this edition up/i);
      await dialog.accept();
    });
    const putsBefore = putBodies.length;
    await editionRow(page, lower).getByRole("button", { name: "Move edition up" }).click();
    await sent;
    // Gated write: the previous order stays until the server answers.
    await expectRelativeOrder(page, upper, lower);
    releasePut();
    await expect
      .poll(
        async () =>
          (await editionIndex(page, lower)) -
          (await editionIndex(page, upper)),
        { timeout: 20000 },
      )
      .toBeLessThan(0);
    await expectRelativeOrder(page, lower, upper);
    await expect(page.getByRole("alert")).toHaveCount(0);
    expect(putBodies.length).toBe(putsBefore + 1);
    const body = putBodies.at(-1);
    expect(body).toContain('"priority"');
    expect(body).toContain("3");
    expect(body).not.toContain('"name"');
    expect(body).not.toContain('"comment"');
    expect(body).not.toContain(lower);

    // Stored write: a fresh load keeps the new order.
    await openDesign(page);
    await page.getByRole("tab", { name: /Editions/i }).click();
    await expect(page.getByRole("button", { name: lower, exact: true })).toBeVisible({
      timeout: 20000,
    });
    await expect
      .poll(
        async () =>
          (await editionIndex(page, lower)) -
          (await editionIndex(page, upper)),
        { timeout: 20000 },
      )
      .toBeLessThan(0);
    await expectRelativeOrder(page, lower, upper);
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });

  test("cancel does not call the server", async ({ page }) => {
    const jsErrors = trackJsErrors(page);
    let puts = 0;
    await page.route(EDITIONS_PUT, async (route) => {
      if (editionUpdatePut(route.request().url(), route.request().method())) {
        puts += 1;
      }
      await route.continue();
    });

    const stamp = Date.now();
    const lower = `NightReKeepLow-${stamp}`;
    const upper = `NightReKeepHigh-${stamp}`;
    await openDesign(page);
    await createEdition(page, lower, `low-${stamp}`, 2);
    await createEdition(page, upper, `high-${stamp}`, 3);

    page.once("dialog", async (dialog) => {
      expect(dialog.type()).toBe("confirm");
      await dialog.dismiss();
    });
    await editionRow(page, lower).getByRole("button", { name: "Move edition up" }).click();
    await expectRelativeOrder(page, upper, lower);
    expect(puts).toBe(0);
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });

  test("HTTP 400, 403, and 409 keep the previous order", async ({ page }) => {
    const jsErrors = trackJsErrors(page);
    const stamp = Date.now();
    const lower = `NightReErrLow-${stamp}`;
    const upper = `NightReErrHigh-${stamp}`;
    await openDesign(page);
    await createEdition(page, lower, `low-${stamp}`, 2);
    await createEdition(page, upper, `high-${stamp}`, 3);

    const cases = [
      [400, "Edition priority must be from 1 to 5"],
      [403, "Admin or Designer role required to save a publish edition"],
      [409, "Edition is in use"],
    ];
    for (const [status, message] of cases) {
      await page.unroute(EDITIONS_PUT);
      await page.route(EDITIONS_PUT, async (route) => {
        if (editionUpdatePut(route.request().url(), route.request().method())) {
          await route.fulfill({
            status,
            contentType: "application/json",
            body: JSON.stringify({ message }),
          });
          return;
        }
        await route.continue();
      });
      page.once("dialog", (dialog) => dialog.accept());
      await editionRow(page, upper).getByRole("button", { name: "Move edition down" }).click();
      await expect(page.getByRole("alert")).toContainText(message);
      await expectRelativeOrder(page, upper, lower);
    }
    expect(jsErrors, `console/page errors: ${jsErrors.join("\n")}`).toEqual([]);
  });
});
