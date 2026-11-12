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
 * Published-site smoke for the future `--with-dts` QA cell (issue #6237,
 * spec `specs/2402-qa-dts-publish/`).
 *
 * <p>Tags: {@code @published} {@code @published-site}</p>
 *
 * <p>Run (needs the in-cell published site — full edition/publish path,
 * not the default H2 QA cell):
 * {@code PUBLISHED_SITE_URL=https://<in-cell-published-site-origin>
 * npm run test:surface -- --path tests/published-site-smoke.spec.js}</p>
 *
 * <p>Seed coverage per the epic proposal §4: one static published page
 * served from the cell, asserting the publish-time {@code web_resources}
 * tree (FancyTree bytes provisioned by #6052) loads with zero 404s and no
 * retired DynaTree copies resurface. Widget-class pages (poll, feed,
 * metadata, form, comments) get their own specs in slice 4 reusing this
 * skip contract.</p>
 *
 * <p>Without {@code PUBLISHED_SITE_URL} both tests skip cleanly — the
 * default H2 QA cell has no DTS publish path.</p>
 */

const { test, expect } = require("@playwright/test");

const PUBLISHED_WEB_RESOURCES = [
  "/web_resources/cm/jslib/jquery.fancytree.js",
  "/web_resources/cm/css/fancytree/skin/ui.fancytree.css",
];

const REMOVED_DYNATREE_PUBLISHED = ["/web_resources/cm/jslib/jquery.dynatree.js"];

function publishedSiteUrl() {
  const siteUrl = process.env.PUBLISHED_SITE_URL || "";
  if (!siteUrl) {
    return null;
  }
  return new URL(siteUrl);
}

test.describe("Published site smoke for the --with-dts cell (issue #6237)", () => {
  test(
    "published web_resources serve tree bytes, DynaTree stays gone @published",
    { tag: ["@published", "@published-site"] },
    async ({ request }) => {
      const site = publishedSiteUrl();
      test.skip(
        !site,
        "needs PUBLISHED_SITE_URL=(in-cell published site origin); "
          + "default H2 QA cell has no DTS publish path (#6237)",
      );

      for (const asset of PUBLISHED_WEB_RESOURCES) {
        const resp = await request.get(`${site.origin}${asset}`);
        expect(resp.status(), `expected 200 for ${asset}`).toBe(200);
      }
      for (const asset of REMOVED_DYNATREE_PUBLISHED) {
        const resp = await request.get(`${site.origin}${asset}`);
        expect(
          resp.status(),
          `expected 404 for removed dead copy ${asset}`,
        ).toBe(404);
      }
    },
  );

  test(
    "published home page loads with zero web_resources failures @published",
    { tag: ["@published", "@published-site"] },
    async ({ page }) => {
      test.setTimeout(120_000);
      const site = publishedSiteUrl();
      test.skip(
        !site,
        "needs PUBLISHED_SITE_URL=(in-cell published site origin)",
      );

      const failedRequests = [];
      page.on("requestfailed", (req) => {
        if (/web_resources/i.test(req.url())) {
          failedRequests.push(`${req.url()} :: ${req.failure()?.errorText}`);
        }
      });
      page.on("response", (resp) => {
        if (resp.status() >= 400 && /web_resources/i.test(resp.url())) {
          failedRequests.push(`${resp.url()} :: HTTP ${resp.status()}`);
        }
      });
      const pageErrors = [];
      page.on("pageerror", (err) => {
        pageErrors.push(String((err && err.stack) || err));
      });

      await page.goto(site.toString(), { waitUntil: "networkidle" });

      expect(
        failedRequests,
        `failed web_resources requests:\n${failedRequests.join("\n")}`,
      ).toEqual([]);
      expect(
        pageErrors.filter((stack) => /fancytree|dynatree/i.test(stack)),
        `tree JS errors:\n${pageErrors.join("\n")}`,
      ).toEqual([]);
    },
  );
});
