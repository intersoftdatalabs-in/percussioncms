/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
 * Smoke test: the Admin login flow against the live dev CMS works.
 *
 * <p>Brings up Chromium, fills the {@code /Rhythmyx/login} form with the
 * auto-discovered Admin credentials, submits, and verifies the browser
 * navigated off the login page. This is the smallest possible end-to-end
 * check that the dev CMS is up, the form is rendered, the credentials are
 * valid, and the session is established.</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

test.describe("Admin login", () => {
  test("logs in and lands on a non-login Rhythmyx page", async ({ page }) => {
    test.setTimeout(30_000);
    await loginAsAdmin(page);

    const url = page.url();
    expect(url).not.toMatch(/\/Rhythmyx\/login(\?|$)/);
    expect(url).toMatch(/\/Rhythmyx\/|\/cm\//);

    // 8.2 lands at /Rhythmyx/index.jsp (JSP welcome page); older
    // versions may land directly at /cm/app/... Either is success.
    const title = await page.title();
    expect(title.toLowerCase()).not.toContain("error");
  });

  test("BASE_URL is auto-discovered", async () => {
    expect(BASE_URL).toMatch(/^https?:\/\/[^/]+:\d+$/);
  });
});
