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
 * Remember last community on login (#3507 / parent #3505 slice 3).
 *
 * Surface-filtered only — not full suite:
 *   npm run test:surface -- --path tests/remember-last-community.spec.js
 *
 * QA mode: perc-devctl qa-up → TEST_CMS_URL + ADMIN_* → test:surface → qa-down.
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

function homeUrl() {
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=home&_=${Date.now()}`;
}

function profileUrl() {
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=profile&_=${Date.now()}`;
}

function unwrapCurrentUser(body) {
  if (!body || typeof body !== "object") {
    return {};
  }
  return body.CurrentUser || body.User || body;
}

function isIgnoredConsoleText(text) {
  const raw = String(text || "");
  return (
    /gravatar\.com/i.test(raw) ||
    /Failed to load resource/i.test(raw) ||
    /net::ERR_/i.test(raw)
  );
}

/**
 * @param {import("@playwright/test").Page} page
 */
async function expectShell(page) {
  await expect(page.getByTestId("perc-spa-app")).toBeVisible({
    timeout: 30_000,
  });
  await expect(page.getByTestId("perc-spa-user-menu")).toBeVisible({
    timeout: 30_000,
  });
  await expect(page.getByTestId("perc-spa-community-name")).toBeVisible({
    timeout: 30_000,
  });
}

/**
 * @param {import("@playwright/test").Page} page
 */
async function enableRememberLast(page) {
  await page.goto(profileUrl(), { waitUntil: "domcontentloaded" });
  await expect(page.getByTestId("perc-profile-account")).toBeVisible({
    timeout: 30_000,
  });
  const box = page.getByTestId("perc-profile-account-remember-last");
  await expect(box).toBeVisible({ timeout: 30_000 });
  if (!(await box.isChecked())) {
    await box.check();
    await expect(
      page.getByTestId("perc-profile-account-success"),
    ).toBeVisible({ timeout: 30_000 });
  }
}

test.describe("Remember last community on login (#3507) @community @profile @ui", () => {
  test("next login lands on last switched community when still allowed", async ({
    page,
  }) => {
    test.setTimeout(180_000);

    const consoleErrors = [];
    page.on("pageerror", (err) => {
      consoleErrors.push(String(err && err.message ? err.message : err));
    });
    page.on("console", (msg) => {
      if (msg.type() === "error" && !isIgnoredConsoleText(msg.text())) {
        consoleErrors.push(msg.text());
      }
    });

    await loginAsAdmin(page);
    await enableRememberLast(page);

    await page.goto(homeUrl(), { waitUntil: "domcontentloaded" });
    await expectShell(page);

    const currentRes = await page.request.get(
      `${BASE_URL}/Rhythmyx/services/user/user/current`,
    );
    expect(
      currentRes.ok(),
      `GET /user/user/current HTTP ${currentRes.status()}`,
    ).toBe(true);
    const payload = unwrapCurrentUser(await currentRes.json());
    const allowed = Array.isArray(payload.communities)
      ? payload.communities.map((n) => String(n).trim()).filter(Boolean)
      : [];
    const sessionCommunity = String(payload.currentCommunity || "").trim();
    const other = allowed.find((name) => name !== sessionCommunity);

    if (!other) {
      test.info().annotations.push({
        type: "note",
        description:
          "Admin has a single membership community — persist path covered; restore switch skipped",
      });
      expect(consoleErrors, `console-clean: ${consoleErrors.join(" | ")}`).toEqual(
        [],
      );
      return;
    }

    await page.getByTestId("perc-spa-community-switch").click();
    const list = page.getByTestId("perc-spa-community-list");
    await expect(list).toBeVisible();
    await list.locator(`[data-community-name="${other}"]`).click();
    await expect(page.getByTestId("perc-spa-community-switch-error")).toHaveCount(
      0,
    );
    await expect
      .poll(
        async () =>
          (await page.getByTestId("perc-spa-community-name").textContent()).trim(),
        { timeout: 20_000 },
      )
      .toBe(other);

    await page.getByTestId("perc-spa-logout").click();
    await expect(
      page.getByTestId("perc-logout-page").or(page.getByTestId("perc-login-page")),
    ).toBeVisible({ timeout: 30_000 });

    await loginAsAdmin(page);
    await page.goto(homeUrl(), { waitUntil: "domcontentloaded" });
    await expectShell(page);
    await expect
      .poll(
        async () =>
          (await page.getByTestId("perc-spa-community-name").textContent()).trim(),
        { timeout: 30_000 },
      )
      .toBe(other);

    expect(consoleErrors, `console-clean: ${consoleErrors.join(" | ")}`).toEqual(
      [],
    );
  });
});
