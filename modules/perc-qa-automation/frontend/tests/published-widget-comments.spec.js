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
 * Comments widget surface (#6244 / parent #6237, epic AC3 dynamic half):
 * the published {@code percComments} fixture page renders the comments
 * shell, and a self-seeded comment round-trips against the in-cell DTS
 * following the site's moderation rules.
 *
 * <p>Tags: {@code @published} {@code @surface-comments}.</p>
 *
 * <p>Run (needs the {@code --with-dts} cell — the full edition/publish
 * path, not the default H2 QA cell):
 * {@code PUBLISHED_SITE_URL=https://<in-cell-published-site-origin>
 * DTS_URL=https://<in-cell-dts-origin>
 * npm run test:surface -- --path tests/published-widget-comments.spec.js}
 * from {@code modules/perc-qa-automation/frontend}. Without
 * {@code PUBLISHED_SITE_URL} the shell case skips; without
 * {@code DTS_URL} the interaction case skips.</p>
 */

const { test, expect } = require("@playwright/test");
const {
  SURFACES,
  SKIP_NO_SITE,
  SKIP_NO_DTS,
  publishedSiteUrl,
  dtsBaseUrl,
  fixturePageUrl,
  dtsServiceUrl,
  watchNoise,
  assertQuiet,
} = require("./helpers/published-widget-surfaces");

const entry = SURFACES.find((surface) => surface.name === "comments");

async function readJson(response) {
  try {
    return await response.json();
  } catch {
    return null;
  }
}

test.describe("published comments widget surface", () => {
  test(
    "published comments page renders the comments shell with zero noise @published",
    { tag: ["@published", entry.tag] },
    async ({ page, request }) => {
      test.setTimeout(90_000);
      const site = publishedSiteUrl();
      test.skip(!site, SKIP_NO_SITE);

      const pageUrl = fixturePageUrl(site, entry);
      const probe = await request.get(pageUrl);
      expect(probe.status(), `expected 200 for ${entry.widget} fixture ${pageUrl}`).toBe(200);

      const noise = watchNoise(page);
      await page.goto(pageUrl, { waitUntil: "networkidle" });
      await expect(
        page.locator(entry.marker).first(),
        `${entry.widget}: expected comments shell ${entry.marker}`,
      ).toBeVisible({ timeout: 15_000 });

      assertQuiet(entry, noise, expect);
    },
  );

  test(
    "self-seeded comment follows moderation rules on the in-cell DTS",
    { tag: ["@published", entry.tag] },
    async ({ request }) => {
      test.setTimeout(90_000);
      const base = dtsBaseUrl();
      test.skip(!base, SKIP_NO_DTS);
      const svc = dtsServiceUrl(base, entry);

      // The comment targets the published fixture page itself, so the
      // surface ties the interaction to the page the shell case renders.
      const site = publishedSiteUrl();
      const pagePath = site
        ? new URL(fixturePageUrl(site, entry)).pathname
        : "/widgets/percComments.html";
      const commentSite = `qasurface-${Date.now()}`;
      const marker = `qasurface-${Date.now()}`;

      const stateRead = await request.get(
        `${svc}/defaultModerationState/${encodeURIComponent(commentSite)}`,
      );
      const defaultState = stateRead.ok() ? (await stateRead.text()).trim() : "";

      const add = await request.post(`${svc}/addcomment`, {
        form: {
          action: "add",
          username: "qa-surface",
          site: commentSite,
          pagepath: pagePath,
          email: "qa-surface@example.com",
          text: marker,
          title: "QA surface probe",
          url: "",
        },
      });
      expect(
        add.ok(),
        `comments: posting to '${commentSite}${pagePath}' must be accepted (HTTP ${add.status()})`,
      ).toBe(true);

      const list = await request.post(`${svc}/list`, {
        data: { site: commentSite, pagepath: pagePath, maxResults: 25, startIndex: 0 },
      });
      expect(list.ok(), `comments: listing '${commentSite}${pagePath}' must stay readable`).toBe(true);
      const texts = JSON.stringify((await readJson(list)) || {});
      if (texts.includes(marker)) {
        return;
      }
      // Held for moderation is still the rule working: the site default
      // state disciplines visibility, so a non-visible comment is a pass
      // only when the site does not auto-approve.
      expect(
        defaultState.length > 0 && !/^approve/i.test(defaultState),
        `comments: probe comment on '${commentSite}${pagePath}' is neither listed nor ` +
          `covered by a moderating default state ('${defaultState}')`,
      ).toBe(true);
    },
  );
});
