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
 * Classic-UI tree widgets from npm (issue #5662).
 *
 * <p>Tags: {@code @classic-ui} {@code @ui}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/classic-tree-widgets-npm.spec.js}</p>
 *
 * <p>Covers the vendored-copy removal: classic pages that include
 * {@code cm/app/includes/finder_js.jsp} (dashboard, webmgt, …) must load
 * FancyTree from the npm-backed jslib URLs (jquery.fancytree 2.38.x,
 * Dependabot-tracked in {@code WebUI/src/main/frontend/package.json}),
 * the deleted DynaTree copy must no longer resolve anywhere, and the
 * finder tree must still initialize (parity with the pre-migration
 * FancyTree behavior — no app code ever invoked DynaTree).</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

const NPM_SERVED = [
  "/Rhythmyx/cm/jslib/profiles/3x/jquery/plugins/jquery-fancytree/jquery.fancytree-all.min.js",
  "/Rhythmyx/cm/jslib/profiles/3x/jquery/plugins/jquery-fancytree/skin-win8/ui.fancytree.min.css",
  "/Rhythmyx/cm/jslib/profiles/3x/jquery/plugins/jquery-fancytree/skin-win8/icons.gif",
];

const REMOVED_DEAD = [
  "/Rhythmyx/cm/jslib/profiles/3x/jquery/plugins/jquery-dynatree/jquery.dynatree.js",
  "/Rhythmyx/cm/css/dynatree/skin/ui.dynatree.css",
  "/Rhythmyx/cm/css/dynatree/skin/icons.gif",
  "/Rhythmyx/cm/app/css/legacy/dynatree/skin/ui.dynatree.css",
  "/Rhythmyx/cm/vendor/js/legacy/profiles/3x/jquery/plugins/jquery-dynatree/jquery.dynatree.js",
  "/Rhythmyx/cm/vendor/js/legacy/profiles/3x/jquery/plugins/jquery-fancytree/jquery.fancytree-all.min.js",
  "/Rhythmyx/cm/vendor/css/legacy/skin-win8/ui.fancytree.css",
  "/Rhythmyx/cm/skin-win8/ui.fancytree.css",
];

test.describe("Classic-UI tree widgets from npm (issue #5662)", () => {
  test(
    "dashboard finder loads npm FancyTree with zero script 404s @ui",
    { tag: ["@classic-ui", "@ui"] },
    async ({ page }) => {
      test.setTimeout(120_000);
      const treeErrorStacks = [];
      const failedRequests = [];
      page.on("pageerror", (err) => {
        const stack = String((err && err.stack) || err);
        // Fail only on errors arising from the migrated tree files. The
        // dashboard itself emits pre-existing cell errors (never-shipped
        // cm/gadgets/container/* probes, a PercDashboard I18N.meesage typo,
        // dashboard.jsp set_label) that the already-merged #5660 dashboard
        // spec fails on identically in this cell — unrelated to #5662.
        if (/fancytree|dynatree/i.test(stack)) {
          treeErrorStacks.push(stack);
        }
      });
      page.on("console", (msg) => {
        if (
          msg.type() === "error" &&
          /fancytree|dynatree/i.test(msg.text())
        ) {
          treeErrorStacks.push(msg.text());
        }
      });
      page.on("requestfailed", (req) => {
        if (req.url().includes("/cm/jslib/")) {
          failedRequests.push(`${req.url()} :: ${req.failure()?.errorText}`);
        }
      });
      page.on("response", (resp) => {
        if (resp.status() >= 400 && resp.url().includes("/cm/jslib/")) {
          failedRequests.push(`${resp.url()} :: HTTP ${resp.status()}`);
        }
      });

      await loginAsAdmin(page);
      await page.goto(`${BASE_URL}/Rhythmyx/cm/app/dashboard.jsp`, {
        waitUntil: "networkidle",
      });

      const tree = await page.evaluate(() => ({
        fancytree:
          typeof window.jQuery === "function" &&
          typeof window.jQuery.fn.fancytree === "function",
        dynatreeGone:
          typeof window.jQuery !== "function" ||
          typeof window.jQuery.fn.dynatree !== "function",
      }));

      expect(tree.fancytree, "npm FancyTree missing on dashboard").toBe(true);
      expect(tree.dynatreeGone, "DynaTree plugin should be gone").toBe(true);

      // Parity: drive the shipped FancyTree bytes through a real
      // init → lazy-expand interaction (the finder/tree behavior the
      // classic UI depends on), without depending on app dialog flows.
      const interaction = await page.evaluate(async () => {
        const host = document.createElement("div");
        host.id = "issue-5662-probe-tree";
        document.body.appendChild(host);
        const $host = window.jQuery("#issue-5662-probe-tree");
        $host.fancytree({
          source: [
            {
              title: "Sites",
              folder: true,
              lazy: true,
              key: "sites",
            },
          ],
          lazyLoad: (event, data) => {
            data.result = [
              { title: "Foo", folder: true, lazy: true, key: "foo" },
            ];
          },
        });
        const treeApi = window.jQuery.ui.fancytree.getTree("#issue-5662-probe-tree");
        const roots = treeApi.getRootNode().getChildren() || [];
        const before = roots.length;
        await roots[0].setExpanded(true);
        // Lazy children resolve on the next tick.
        await new Promise((r) => setTimeout(r, 0));
        const after = (roots[0].getChildren() || []).length;
        const rendered = document.querySelectorAll(
          "#issue-5662-probe-tree span.fancytree-title",
        ).length;
        $host.fancytree("destroy");
        host.remove();
        return { before, after, rendered };
      });

      expect(interaction.before, "probe tree should render 1 root").toBe(1);
      expect(interaction.after, "lazy expand should load 1 child").toBe(1);
      expect(
        interaction.rendered >= 2,
        "expanded titles should render fancytree DOM",
      ).toBe(true);

      expect(
        failedRequests,
        `failed jslib requests:\n${failedRequests.join("\n")}`,
      ).toEqual([]);
      expect(
        treeErrorStacks,
        `tree JS errors:\n${treeErrorStacks.join("\n")}`,
      ).toEqual([]);
    },
  );

  test(
    "npm tree assets resolve, dead copies are gone @ui",
    { tag: ["@classic-ui", "@ui"] },
    async ({ page }) => {
      test.setTimeout(60_000);
      await loginAsAdmin(page);

      for (const asset of NPM_SERVED) {
        const resp = await page.request.get(`${BASE_URL}${asset}`);
        expect(resp.status(), `expected 200 for ${asset}`).toBe(200);
      }
      for (const asset of REMOVED_DEAD) {
        const resp = await page.request.get(`${BASE_URL}${asset}`);
        expect(
          resp.status(),
          `expected 404 for removed dead copy ${asset}`,
        ).toBe(404);
      }
    },
  );
});
