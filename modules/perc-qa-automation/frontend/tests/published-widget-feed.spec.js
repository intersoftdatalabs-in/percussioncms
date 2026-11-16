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
 * RSS feed widget surface (#6244 / parent #6237, epic AC3 dynamic half):
 * the published {@code percRss} fixture page renders the feed shell, the
 * feeds service answers liveness, and the feed endpoint follows its
 * documented contract for the fixture probe.
 *
 * <p>Feed entries render from the metadata index behind a seeded feed
 * descriptor ({@code PUT /rss/descriptors} as {@code deliverymanager}).
 * The shell case renders the published fixture page and the liveness case
 * gates the service; the entries case below self-seeds two index rows plus
 * a uniquely-named descriptor and asserts the generated feed carries them.
 * Seeding follows the metadata-surface precedent (same indexer contract,
 * same {@code deliverymanager} credential contract) and skips by name when
 * the cell rejects the seed (auth-gated or CONFIDENTIAL-redirected seed
 * path) — cell provisioning of the seed path is tracked separately
 * (parent #6237 residuals #6343/#6375). The unseeded-name probe stays as
 * the documented error-page contract for names that were never seeded.</p>
 *
 * <p>Tags: {@code @published} {@code @surface-feed}.</p>
 *
 * <p>Run (needs the {@code --with-dts} cell — the full edition/publish
 * path, not the default H2 QA cell):
 * {@code PUBLISHED_SITE_URL=https://<in-cell-published-site-origin>
 * DTS_URL=https://<in-cell-dts-origin>
 * npm run test:surface -- --path tests/published-widget-feed.spec.js}
 * from {@code modules/perc-qa-automation/frontend}. Without
 * {@code PUBLISHED_SITE_URL} the shell case skips; without
 * {@code DTS_URL} the service cases skip.</p>
 */

const { test, expect } = require("@playwright/test");
const {
  SURFACES,
  SKIP_NO_SITE,
  SKIP_NO_DTS,
  publishedSiteUrl,
  dtsBaseUrl,
  fixturePageUrl,
  dtsContextUrl,
  dtsServiceUrl,
  watchNoise,
  assertQuiet,
} = require("./helpers/published-widget-surfaces");

const entry = SURFACES.find((surface) => surface.name === "feed");

/**
 * BASIC credentials for the role-protected DTS seed endpoints (the feeds
 * {@code PUT /rss/descriptors} descriptor surface plus the metadata
 * context-level {@code /indexer/entry/...} extractor used for the index
 * rows, both {@code deliverymanager} role). Defaults are the shipped DTS
 * development defaults (see
 * `delivery-tier-distribution/src/main/resources/etc/dts-realm.properties`
 * and the installed `conf/tomcat-users.xml`); override with env when the
 * cell provisions its own. Both seed paths are marked {@code CONFIDENTIAL}
 * in the shipped `web.xml` files, so plain-HTTP cells answer the seed with
 * a redirect to the (usually unpublished) HTTPS port and the entries case
 * below skips by name — mirroring the metadata-surface seed contract (the
 * residual linked from #6343, cell provisioning in #6375).
 */
function deliveryAuthHeader() {
  const user = process.env.DTS_DELIVERY_USER || "ps_manager";
  const password =
    process.env.DTS_DELIVERY_PASSWORD || "f2c57870308dc87f432e5912d4de6f8e322721ba";
  return {
    Authorization: `Basic ${Buffer.from(`${user}:${password}`).toString("base64")}`,
  };
}

test.describe("published RSS feed widget surface", () => {
  test(
    "published feed page renders the feed shell with zero noise @published",
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
        `${entry.widget}: expected feed shell ${entry.marker}`,
      ).toBeVisible({ timeout: 15_000 });

      assertQuiet(entry, noise, expect);
    },
  );

  test(
    "feeds service answers liveness on the in-cell DTS",
    { tag: ["@published", entry.tag] },
    async ({ request }) => {
      test.setTimeout(90_000);
      const base = dtsBaseUrl();
      test.skip(!base, SKIP_NO_DTS);

      const live = await request.head(`${dtsContextUrl(base, entry)}${entry.csrf}`);
      expect(
        live.ok(),
        `feeds: HEAD ${entry.context}${entry.csrf} must answer 2xx`,
      ).toBe(true);
    },
  );

  test(
    "feed endpoint reports the documented unseeded outcome for the fixture probe",
    { tag: ["@published", entry.tag] },
    async ({ request }) => {
      test.setTimeout(90_000);
      const base = dtsBaseUrl();
      test.skip(!base, SKIP_NO_DTS);

      // Unseeded fixture path: the service answers 404 (no descriptor),
      // which the cell's error-page contract surfaces as a redirect at the
      // service's error page. Assert the contract, not the bare code.
      const feedUrl =
        `${dtsServiceUrl(base, entry)}/qasurface/qasurface-feed-${Date.now()}/localhost`;
      const probe = await request.get(feedUrl, {
        headers: { Accept: "text/xml" },
        maxRedirects: 0,
      });
      expect(
        probe.status(),
        "feeds: unseeded fixture path must redirect at the documented error page",
      ).toBe(302);
      expect(
        String(probe.headers().location || ""),
        "feeds: unseeded fixture redirect must stay at the feeds error page",
      ).toMatch(/\/feeds\/error\.html$/);
    },
  );

  test(
    "feed endpoint serves seeded entries from the live index",
    { tag: ["@published", entry.tag] },
    async ({ request }) => {
      test.setTimeout(90_000);
      const base = dtsBaseUrl();
      test.skip(!base, SKIP_NO_DTS);
      const svc = dtsServiceUrl(base, entry);
      // The descriptor's generateFeed call targets the metadata query
      // endpoint on the same Tomcat host (server-side loopback); the index
      // rows are seeded through the metadata extractor at the context root.
      const metadataSvc = `${base.origin}/perc-metadata-services/metadata`;
      const indexerSvc = `${base.origin}/perc-metadata-services/indexer`;

      const stamp = Date.now();
      const site = "qacell";
      const feedName = `qasurface-feed-${stamp}`;
      const feedTitle = `QA surface feed ${stamp}`;
      const entryTitles = [
        `QA surface feed item ${stamp}-1`,
        `QA surface feed item ${stamp}-2`,
      ];
      const seedPaths = [1, 2].map((n) => `/qacell/surface-feed-${stamp}-${n}.html`);

      // Seeded rows (cleaned below) so the disposable cell stays clean; the
      // uniquely-named descriptor itself has no REST delete path (PUT with
      // an empty list is ignored server-side) and stays behind like the
      // self-seeded poll fixtures — harmless in a disposable cell.
      const seeded = [];
      try {
        for (let i = 0; i < seedPaths.length; i += 1) {
          const seed = await request.post(`${indexerSvc}/entry${seedPaths[i]}`, {
            maxRedirects: 0,
            headers: deliveryAuthHeader(),
            data: {
              name: `surface-feed-${stamp}-${i + 1}.html`,
              folder: "/qacell/",
              pagepath: seedPaths[i],
              linktext: entryTitles[i],
              type: "page",
              site,
              properties: [
                { name: "dcterms:title", stringvalue: entryTitles[i] },
                {
                  name: "dcterms:abstract",
                  stringvalue: `QA surface feed probe ${stamp} item ${i + 1}`,
                },
              ],
            },
          });
          // Skip only for the known cell limitations (auth-gated or
          // redirected seed); any other failure (5xx, 404, …) means the
          // seed path itself is broken and must fail the test.
          test.skip(
            !seed.ok() && [401, 403, 302].includes(seed.status()),
            `surface feed (index seeding): the cell rejected the seeded entry ` +
              `'${seedPaths[i]}' (HTTP ${seed.status()}); the shipped extractor is ` +
              `CONFIDENTIAL + deliverymanager-gated, so plain-HTTP cells without ` +
              `provisioned DTS deliverymanager credentials (DTS_DELIVERY_USER / ` +
              `DTS_DELIVERY_PASSWORD) and a reachable DTS HTTPS port cannot seed ` +
              `(parent #6237, residuals from #6343 / #6375)`,
          );
          expect(
            seed.ok(),
            `feeds: seeding the index entry '${seedPaths[i]}' must be accepted (HTTP ${seed.status()})`,
          ).toBe(true);
          seeded.push(seedPaths[i]);
        }

        // PUT the fixture descriptor as deliverymanager. Connection info
        // must be present or getFeed answers 503; the values themselves are
        // unused by generateFeed (it builds the metadata URI server-side),
        // so fixture placeholders are sufficient.
        const put = await request.put(`${svc}/descriptors`, {
          maxRedirects: 0,
          headers: deliveryAuthHeader(),
          data: {
            descriptors: [
              {
                name: feedName,
                site,
                description: `QA surface feed probe ${stamp}`,
                link: "http://localhost/qacell",
                title: feedTitle,
                query: JSON.stringify({
                  criteria: [`site = '${site}'`],
                  maxResults: 25,
                  startIndex: 0,
                }),
                type: "RSS",
              },
            ],
            serviceUrl: `${metadataSvc}/get`,
            serviceUser: "qasurface",
            servicePass: "qasurface",
            servicePassEncrypted: false,
            site,
          },
        });
        test.skip(
          !put.ok() && [401, 403, 302].includes(put.status()),
          `surface feed (descriptor seeding): the cell rejected the fixture ` +
            `descriptor '${feedName}' (HTTP ${put.status()}); the shipped ` +
            `/rss/descriptors` +
            ` path is CONFIDENTIAL + deliverymanager-gated, so plain-HTTP cells ` +
            `without provisioned DTS deliverymanager credentials ` +
            `(DTS_DELIVERY_USER / DTS_DELIVERY_PASSWORD) and a reachable DTS ` +
            `HTTPS port cannot seed (parent #6237, residuals from #6343 / #6375)`,
        );
        expect(
          put.ok(),
          `feeds: seeding the fixture descriptor '${feedName}' must be accepted (HTTP ${put.status()})`,
        ).toBe(true);

        const feed = await request.get(`${svc}/${site}/${feedName}/localhost`, {
          headers: { Accept: "text/xml" },
          maxRedirects: 0,
        });
        expect(
          feed.ok(),
          `feeds: the seeded feed '${feedName}' must answer 200 (HTTP ${feed.status()})`,
        ).toBe(true);
        const xml = await feed.text();
        expect(
          xml,
          "feeds: the generated feed must carry the seeded descriptor title",
        ).toContain(feedTitle);
        for (const itemTitle of entryTitles) {
          expect(
            xml,
            `feeds: the generated feed must carry the seeded entry '${itemTitle}'`,
          ).toContain(itemTitle);
        }
      } finally {
        // Best-effort removal of the seeded index rows; never masks the
        // original failure (no assertions here).
        for (const seedPath of seeded) {
          const leftover = await request
            .delete(`${indexerSvc}/entry${seedPath}`, {
              maxRedirects: 0,
              headers: deliveryAuthHeader(),
            })
            .catch((cleanupError) => {
              console.warn(
                `feeds: best-effort cleanup of '${seedPath}' threw: ` +
                  `${cleanupError?.message ?? cleanupError}`,
              );
              return null;
            });
          if (leftover && !leftover.ok()) {
            console.warn(
              `feeds: best-effort cleanup of '${seedPath}' answered HTTP ` +
                `${leftover.status()}; the seeded row may remain in the cell`,
            );
          }
        }
      }
    },
  );
});
