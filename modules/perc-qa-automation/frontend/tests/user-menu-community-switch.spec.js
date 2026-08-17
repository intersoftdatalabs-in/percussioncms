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
 * Top-nav session community display + switch (#3506 / parent #3505).
 *
 * Surface-filtered only — not full suite:
 *   npm run test:surface -- --path tests/user-menu-community-switch.spec.js
 *
 * QA mode: perc-devctl qa-up → TEST_CMS_URL + ADMIN_* → test:surface → qa-down.
 *
 * Lists only GET /user/user/current membership communities (not the catalog).
 * Switch uses POST /services/communities/switch/{name}.
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

function homeUrl() {
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=home&_=${Date.now()}`;
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

test.describe("Top-nav community display and switch (#3506) @community @ui", () => {
  test("shows session community and switch list from current-user membership only", async ({
    page,
  }) => {
    test.setTimeout(90_000);

    const consoleErrors = [];
    const catalogFinds = [];
    const switchPosts = [];
    page.on("pageerror", (err) => {
      consoleErrors.push(String(err && err.message ? err.message : err));
    });
    page.on("console", (msg) => {
      if (msg.type() === "error" && !isIgnoredConsoleText(msg.text())) {
        consoleErrors.push(msg.text());
      }
    });
    page.on("request", (req) => {
      if (/\/communities\/find(?:\?|$)/.test(req.url())) {
        catalogFinds.push(req.url());
      }
    });
    page.on("response", (res) => {
      if (res.request().method() === "POST" && /\/communities\/switch\//.test(res.url())) {
        switchPosts.push({ url: res.url(), status: res.status() });
      }
    });

    await loginAsAdmin(page);
    await page.goto(homeUrl(), { waitUntil: "domcontentloaded" });

    await expect(page.getByTestId("perc-spa-app")).toBeVisible({
      timeout: 30_000,
    });
    await expect(page.getByTestId("perc-spa-user-menu")).toBeVisible({
      timeout: 30_000,
    });
    await expect(page.getByTestId("perc-spa-community")).toBeVisible();
    await expect(page.getByTestId("perc-spa-community-name")).toBeVisible();

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

    const chromeName = (
      await page.getByTestId("perc-spa-community-name").textContent()
    ).trim();
    if (sessionCommunity) {
      expect(chromeName).toBe(sessionCommunity);
    } else {
      expect(chromeName.length).toBeGreaterThan(0);
    }

    if (allowed.length === 0) {
      await expect(page.getByTestId("perc-spa-community-switch")).toHaveCount(0);
      expect(catalogFinds, "must not list the community catalog").toEqual([]);
      expect(consoleErrors, `console-clean: ${consoleErrors.join(" | ")}`).toEqual(
        [],
      );
      return;
    }

    const switchBtn = page.getByTestId("perc-spa-community-switch");
    await expect(switchBtn).toBeVisible();
    await switchBtn.click();
    const list = page.getByTestId("perc-spa-community-list");
    await expect(list).toBeVisible();

    const optionNames = await list
      .locator("[data-community-name]")
      .evaluateAll((els) =>
        els.map((el) => el.getAttribute("data-community-name") || ""),
      );
    expect(optionNames.sort()).toEqual([...allowed].sort());

    const other = allowed.find((name) => name !== sessionCommunity);
    if (other) {
      await list.getByRole("option", { name: other, exact: true }).click();
      await expect
        .poll(
          async () => {
            const chrome = (
              await page.getByTestId("perc-spa-community-name").textContent()
            ).trim();
            const errCount = await page
              .getByTestId("perc-spa-community-switch-error")
              .count();
            return { chrome, errCount, posts: switchPosts.length };
          },
          { timeout: 20_000 },
        )
        .toMatchObject({ posts: 1 });
      expect(switchPosts[0].url).toContain("/communities/switch/");
      const after = (
        await page.getByTestId("perc-spa-community-name").textContent()
      ).trim();
      const err = page.getByTestId("perc-spa-community-switch-error");
      if (switchPosts[0].status >= 400) {
        await expect(err).toBeVisible();
        expect(after).toBe(chromeName);
      } else {
        expect(after).toBe(other);
        await expect(err).toHaveCount(0);
      }
      await expect(page.getByTestId("perc-spa-user-menu")).toBeVisible();
      await expect(page.getByTestId("perc-spa-logout")).toBeVisible();
    }

    expect(catalogFinds, "must not list the community catalog").toEqual([]);
    expect(consoleErrors, `console-clean: ${consoleErrors.join(" | ")}`).toEqual(
      [],
    );
  });
});
