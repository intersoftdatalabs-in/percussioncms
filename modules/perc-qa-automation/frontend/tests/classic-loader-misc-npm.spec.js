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
 * Classic-UI loader/misc infra served from npm (issue #5667).
 *
 * <p>Tags: {@code @classic-ui} {@code @ui}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/classic-loader-misc-npm.spec.js}</p>
 *
 * <p>Covers the vendored-copy removal without depending on the classic
 * dashboard application session (which cannot establish in the H2 QA cell —
 * "Welcome, null" — identically on the pristine baseline, peer #5660 spec
 * included): classic pages that include
 * {@code cm/app/includes/common_js.jsp} (dashboard, adminWorkflow, webmgt,
 * …) load popper.js + bowser es5.js from the npm-backed files
 * (@popperjs/core 2.11.x + bowser 2.15.x, Dependabot-tracked in
 * {@code WebUI/src/main/frontend/package.json}, synced into
 * target/generated-webui by build-legacy-bundles Phase 0), json2.js is gone
 * (native JSON), and the deleted dead trees (requirejs/, mousetrap/,
 * modernizr, vendored animate.css) no longer resolve.</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

const NPM_SERVED = [
  "/Rhythmyx/cm/jslib/profiles/3x/libraries/popper/popper.js",
  "/Rhythmyx/cm/jslib/profiles/3x/libraries/bowser/es5.js",
];

const REMOVED_VENDORED = [
  "/Rhythmyx/cm/jslib/profiles/3x/libraries/perc-retiredjs/json2.js",
  "/Rhythmyx/cm/jslib/profiles/3x/libraries/requirejs/require.js",
  "/Rhythmyx/cm/jslib/profiles/3x/libraries/requirejs/init.js",
  "/Rhythmyx/cm/jslib/profiles/3x/libraries/mousetrap/mousetrap.js",
  "/Rhythmyx/cm/jslib/profiles/3x/libraries/animate.css/animate.css",
  "/Rhythmyx/cm/jslib/modernizer/modernizr-custom.js",
];

test.describe("Classic-UI loader/misc infra from npm (issue #5667)", () => {
  test(
    "npm popper/bowser execute in-browser with expected globals @ui",
    { tag: ["@classic-ui", "@ui"] },
    async ({ page }) => {
      test.setTimeout(120_000);
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      page.on("console", (msg) => {
        if (msg.type() === "error") {
          pageErrors.push(msg.text());
        }
      });

      // Session-independent execution: fetch the exact served bytes over
      // HTTP, then execute them in a real browser. (The classic dashboard
      // itself cannot establish its application session in the H2 QA cell —
      // "Welcome, null" — which fails identically on the pristine baseline,
      // peer #5660 spec included; so this spec does not depend on dashboard
      // login. JSP serving itself works after login, covered below.)
      await loginAsAdmin(page);
      const popperResp = await page.goto(
        `${BASE_URL}/Rhythmyx/cm/jslib/profiles/3x/libraries/popper/popper.js`,
      );
      expect(popperResp.status(), "served popper.js must be 200").toBe(200);
      const popperText = await popperResp.text();
      const bowserResp = await page.goto(
        `${BASE_URL}/Rhythmyx/cm/jslib/profiles/3x/libraries/bowser/es5.js`,
      );
      expect(bowserResp.status(), "served bowser es5.js must be 200").toBe(200);
      const bowserText = await bowserResp.text();

      await page.goto("about:blank");
      await page.addScriptTag({ content: popperText });
      await page.addScriptTag({ content: bowserText });

      const libs = await page.evaluate(() => ({
        popperGlobal: typeof window.Popper !== "undefined",
        createPopper:
          typeof window.Popper !== "undefined" &&
          typeof window.Popper.createPopper === "function",
        bowserGlobal: typeof window.bowser !== "undefined",
        getParser:
          typeof window.bowser !== "undefined" &&
          typeof window.bowser.getParser === "function",
        // jquery.percutils.js consumes bowser.getParser at runtime.
        parsed:
          typeof window.bowser !== "undefined" &&
          typeof window.bowser.getParser === "function"
            ? window.bowser.getParser(
                window.navigator.userAgent,
              ).getBrowserName() !== undefined
            : false,
        nativeJson: typeof JSON !== "undefined" && typeof JSON.parse === "function",
      }));

      expect(libs.popperGlobal, "Popper global missing").toBe(true);
      expect(libs.createPopper, "Popper.createPopper missing").toBe(true);
      expect(libs.bowserGlobal, "bowser global missing").toBe(true);
      expect(libs.getParser, "bowser.getParser missing").toBe(true);
      expect(libs.parsed, "bowser.getParser failed on current UA").toBe(true);
      expect(libs.nativeJson, "native JSON missing").toBe(true);

      expect(
        pageErrors,
        `page errors:\n${pageErrors.join("\n")}`,
      ).toEqual([]);
    },
  );

  test(
    "npm pair resolves, deleted vendored copies are gone @ui",
    { tag: ["@classic-ui", "@ui"] },
    async ({ page }) => {
      test.setTimeout(60_000);
      await loginAsAdmin(page);

      for (const asset of NPM_SERVED) {
        const resp = await page.request.get(`${BASE_URL}${asset}`);
        expect(resp.status(), `expected 200 for ${asset}`).toBe(200);
      }
      // The served common_js include keeps the npm-backed tags and no
      // longer loads the dropped json2 polyfill.
      const includeResp = await page.goto(
        `${BASE_URL}/Rhythmyx/cm/app/includes/common_js.jsp`,
      );
      expect(includeResp.status(), "common_js.jsp must serve").toBe(200);
      const includeText = await includeResp.text();
      expect(includeText).toContain("libraries/popper/popper.js");
      expect(includeText).toContain("libraries/bowser/es5.js");
      expect(includeText).not.toContain("json2");
      for (const asset of REMOVED_VENDORED) {
        const resp = await page.request.get(`${BASE_URL}${asset}`);
        expect(
          resp.status(),
          `expected 404 for removed vendored ${asset}`,
        ).toBe(404);
      }
    },
  );
});
