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
 * Legacy `/cm/api` Swagger-UI 2.x viewer retirement (issue #5672).
 *
 * <p>Tags: {@code @retired-api-docs} {@code @ui}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/cm-api-retirement.spec.js}</p>
 *
 * <p>Covers the swagger-era vendored-copy removal: the legacy
 * {@code /Rhythmyx/cm/api/} page (Swagger UI 2.0.22, which cannot parse the
 * OpenAPI 3.x contract) is retired to a dependency-free redirect page
 * pointing at the {@code /openapi} webapp, and every vendored library it
 * loaded (handlebars-1.0.0, backbone, underscore, highlight, shred,
 * swagger.js, swagger-oauth, jquery copies) is gone from the war.</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

const API_BASE = "/Rhythmyx/cm/api";

// Every vendored file the legacy index.html loaded (issue #5672 scope).
// o2c.html was the swagger-oauth callback; swagger-ui.js was the 2.0.22 host.
const REMOVED_ASSETS = [
  "/lib/shred.bundle.js",
  "/lib/jquery-3.6.0.min.js",
  "/lib/jquery.slideto.min.js",
  "/lib/jquery.wiggle.min.js",
  "/lib/jquery.ba-bbq.min.js",
  "/lib/handlebars-1.0.0.js",
  "/lib/underscore-min.js",
  "/lib/backbone-min.js",
  "/lib/swagger.js",
  "/lib/highlight.7.3.pack.js",
  "/lib/swagger-oauth.js",
  "/lib/shred/content.js",
  "/swagger-ui.js",
  "/o2c.html",
  "/css/reset.css",
  "/css/screen.css",
].map((suffix) => `${API_BASE}${suffix}`);

test.describe("Legacy cm/api API-doc viewer retirement (issue #5672)", () => {
  test(
    "retirement page renders and points at /openapi/ @ui",
    { tag: ["@retired-api-docs", "@ui"] },
    async ({ page }) => {
      test.setTimeout(60_000);
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      page.on("console", (msg) => {
        if (msg.type() === "error") {
          pageErrors.push(msg.text());
        }
      });

      await loginAsAdmin(page);

      const response = await page.goto(`${BASE_URL}${API_BASE}/`, {
        waitUntil: "domcontentloaded",
      });
      expect(response && response.ok(), "retirement page not served").toBe(
        true,
      );
      // Dependency-free redirect: meta refresh + canonical + visible link.
      const refresh = await page
        .locator('meta[http-equiv="refresh"]')
        .getAttribute("content");
      expect(String(refresh || "")).toMatch(/url=\/openapi\//i);
      await expect(page.locator('link[rel="canonical"]')).toHaveAttribute(
        "href",
        "/openapi/",
      );
      await expect(page.locator('a[href="/openapi/"]')).toHaveCount(1);
      // No legacy viewer globals may leak onto the retirement page.
      const legacyGlobals = await page.evaluate(() => ({
        swaggerUi: typeof window.SwaggerUi,
        backbone: typeof window.Backbone,
        handlebars: typeof window.Handlebars,
      }));
      expect(legacyGlobals).toEqual({
        swaggerUi: "undefined",
        backbone: "undefined",
        handlebars: "undefined",
      });
      expect(pageErrors, "console/page errors on retirement page").toEqual(
        [],
      );
    },
  );

  test(
    "vendored swagger-era assets are gone (404) @ui",
    { tag: ["@retired-api-docs", "@ui"] },
    async ({ page }) => {
      test.setTimeout(60_000);
      await loginAsAdmin(page);

      for (const asset of REMOVED_ASSETS) {
        const res = await page.request.get(`${BASE_URL}${asset}`);
        expect(res.status(), `vendored asset still served: ${asset}`).toBe(
          404,
        );
      }
    },
  );
});
