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
 * Published category-list tree smoke (issue #6052).
 *
 * <p>Tags: {@code @published} {@code @category-tree}</p>
 *
 * <p>Run (needs a DTS-served published site with a collapsible category-list
 * widget — full edition/publish path, not the default H2 QA cell):
 * {@code PUBLISHED_CATEGORY_URL=https://<site>/<page-with-collapsible-category-list>
 * npm run test:surface -- --path tests/published-category-tree-fancytree.spec.js}</p>
 *
 * <p>Covers the delivery-side closure of the DynaTree retirement: published
 * pages must load FancyTree from the publish-time {@code web_resources/cm}
 * copies (provisioned by {@code installDistributionFiles.xml} from the
 * npm-backed WebUI tree, Dependabot-tracked {@code jquery.fancytree}), the
 * deleted DynaTree published paths must stay gone, and the collapsible
 * category list must initialize {@code .fancytree(} with zero script 404s.
 * The URL assertions run against any published host; the widget-init half
 * skips unless {@code PUBLISHED_CATEGORY_URL} points at a page rendering a
 * collapsible category list.</p>
 */

const { test, expect } = require("@playwright/test");

const PUBLISHED_FANCYTREE = [
  "/web_resources/cm/jslib/jquery.fancytree.js",
  "/web_resources/cm/css/fancytree/skin/ui.fancytree.css",
  "/web_resources/cm/css/fancytree/skin-win8/loading.gif",
];

const REMOVED_DYNATREE_PUBLISHED = [
  "/web_resources/cm/jslib/jquery.dynatree.js",
  "/web_resources/cm/css/dynatree/skin/ui.dynatree.css",
];

function publishedOrigin() {
  const pageUrl = process.env.PUBLISHED_CATEGORY_URL || "";
  if (!pageUrl) {
    return null;
  }
  return new URL(pageUrl).origin;
}

test.describe("Published category-list tree from npm FancyTree (issue #6052)", () => {
  test(
    "published web_resources serve FancyTree, DynaTree stays gone @published",
    { tag: ["@published", "@category-tree"] },
    async ({ request }) => {
      const origin = publishedOrigin();
      test.skip(
        !origin,
        "needs PUBLISHED_CATEGORY_URL=(published site origin + page); "
          + "default H2 QA cell has no DTS publish path",
      );

      for (const asset of PUBLISHED_FANCYTREE) {
        const resp = await request.get(`${origin}${asset}`);
        expect(resp.status(), `expected 200 for ${asset}`).toBe(200);
      }
      for (const asset of REMOVED_DYNATREE_PUBLISHED) {
        const resp = await request.get(`${origin}${asset}`);
        expect(
          resp.status(),
          `expected 404 for removed dead copy ${asset}`,
        ).toBe(404);
      }
    },
  );

  test(
    "collapsible category list initializes FancyTree with zero script 404s @published",
    { tag: ["@published", "@category-tree"] },
    async ({ page }) => {
      test.setTimeout(120_000);
      const pageUrl = process.env.PUBLISHED_CATEGORY_URL || "";
      test.skip(
        !pageUrl,
        "needs PUBLISHED_CATEGORY_URL=(page rendering a collapsible category list)",
      );

      const failedRequests = [];
      page.on("requestfailed", (req) => {
        if (/fancytree|dynatree|category/i.test(req.url())) {
          failedRequests.push(`${req.url()} :: ${req.failure()?.errorText}`);
        }
      });
      page.on("response", (resp) => {
        if (resp.status() >= 400 && /fancytree|dynatree|category/i.test(resp.url())) {
          failedRequests.push(`${resp.url()} :: HTTP ${resp.status()}`);
        }
      });
      const treeErrorStacks = [];
      page.on("pageerror", (err) => {
        if (/fancytree|dynatree/i.test(String((err && err.stack) || err))) {
          treeErrorStacks.push(String(err.stack || err));
        }
      });

      await page.goto(pageUrl, { waitUntil: "networkidle" });

      const tree = await page.evaluate(() => ({
        fancytree:
          typeof window.jQuery === "function" &&
          typeof window.jQuery.fn.fancytree === "function",
        dynatreeGone:
          typeof window.jQuery !== "function" ||
          typeof window.jQuery.fn.dynatree !== "function",
        collapsiblePresent:
          window.document.querySelectorAll(".perc-category-list-collapsible").length > 0,
      }));

      expect(tree.fancytree, "published FancyTree plugin missing").toBe(true);
      expect(tree.dynatreeGone, "DynaTree plugin should be gone").toBe(true);
      expect(tree.collapsiblePresent, "no collapsible category list on page").toBe(true);

      expect(
        failedRequests,
        `failed category/tree requests:\n${failedRequests.join("\n")}`,
      ).toEqual([]);
      expect(
        treeErrorStacks,
        `tree JS errors:\n${treeErrorStacks.join("\n")}`,
      ).toEqual([]);
    },
  );
});
