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
 * Metadata service surface (#6244 / parent #6237, epic AC3 dynamic half):
 * the published {@code percMostReadBlogPosts} fixture page (the metadata
 * visit-tracking consumer) renders its shell, and the visit-tracking
 * round-trip plus the index query shape hold against the in-cell DTS.
 *
 * <p>Tags: {@code @published} {@code @surface-metadata}.</p>
 *
 * <p>Run (needs the {@code --with-dts} cell — the full edition/publish
 * path, not the default H2 QA cell):
 * {@code PUBLISHED_SITE_URL=https://<in-cell-published-site-origin>
 * DTS_URL=https://<in-cell-dts-origin>
 * npm run test:surface -- --path tests/published-widget-metadata.spec.js}
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

const entry = SURFACES.find((surface) => surface.name === "metadata");

async function readJson(response) {
  try {
    return await response.json();
  } catch {
    return null;
  }
}

test.describe("published metadata widget surface", () => {
  test(
    "published most-read page renders the metadata shell with zero noise @published",
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
        `${entry.widget}: expected metadata shell ${entry.marker}`,
      ).toBeVisible({ timeout: 15_000 });

      assertQuiet(entry, noise, expect);
    },
  );

  test(
    "visit tracks and the index answers on the in-cell DTS",
    { tag: ["@published", entry.tag] },
    async ({ request }) => {
      test.setTimeout(90_000);
      const base = dtsBaseUrl();
      test.skip(!base, SKIP_NO_DTS);
      const svc = dtsServiceUrl(base, entry);

      // The tracked visit targets the published fixture page itself, so
      // the surface ties the interaction to the page the shell case
      // renders.
      const site = publishedSiteUrl();
      const pagePath = site
        ? new URL(fixturePageUrl(site, entry)).pathname
        : "/widgets/percMostReadBlogPosts.html";

      const track = await request.post(`${svc}/trackblogpost`, {
        data: { pagePath: `${pagePath}?qasurface=${Date.now()}` },
      });
      expect(
        track.ok(),
        `metadata: tracking a visit to '${pagePath}' must be accepted (HTTP ${track.status()})`,
      ).toBe(true);

      const status = await request.get(`${svc}/visits/status`);
      expect(
        status.ok(),
        "metadata: the visit-tracking scheduler status must answer",
      ).toBe(true);

      const top = await request.post(`${svc}/topblogposts`, {
        data: {},
      });
      expect(
        top.ok(),
        "metadata: topblogposts must answer against the in-cell DTS",
      ).toBe(true);
      const ranked = await readJson(top);
      expect(
        Array.isArray(ranked),
        "metadata: topblogposts must return a result list shape",
      ).toBe(true);

      // The index query half (POST /metadata/get) needs a populated
      // metadata index plus a service-side fix the cell does not have yet:
      // every non-empty query round-trips to the cell's error page today
      // (see the residual linked from #6244). The empty-index shape stays
      // covered by topblogposts above; asserting /get here would bake in
      // the broken behavior, so this case stops at the tracking pipeline.
    },
  );
});
