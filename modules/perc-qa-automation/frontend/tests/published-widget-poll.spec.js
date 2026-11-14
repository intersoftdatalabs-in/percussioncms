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
 * Poll widget surface (#6244 / parent #6237, epic AC3 dynamic half): the
 * published {@code percPoll} fixture page renders the poll shell, and a
 * self-seeded poll round-trips a vote against the in-cell DTS.
 *
 * <p>Tags: {@code @published} {@code @surface-poll}.</p>
 *
 * <p>Run (needs the {@code --with-dts} cell — the full edition/publish
 * path, not the default H2 QA cell):
 * {@code PUBLISHED_SITE_URL=https://<in-cell-published-site-origin>
 * DTS_URL=https://<in-cell-dts-origin>
 * npm run test:surface -- --path tests/published-widget-poll.spec.js}
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

const entry = SURFACES.find((surface) => surface.name === "poll");

async function readJson(response) {
  try {
    return await response.json();
  } catch {
    return null;
  }
}

test.describe("published poll widget surface", () => {
  test(
    "published poll page renders the poll shell with zero noise @published",
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
        `${entry.widget}: expected poll shell ${entry.marker}`,
      ).toBeVisible({ timeout: 15_000 });

      assertQuiet(entry, noise, expect);
    },
  );

  test(
    "self-seeded poll vote persists against the in-cell DTS",
    { tag: ["@published", entry.tag] },
    async ({ request }) => {
      test.setTimeout(90_000);
      const base = dtsBaseUrl();
      test.skip(!base, SKIP_NO_DTS);
      const svc = dtsServiceUrl(base, entry);

      const pollName = `qasurface-poll-${Date.now()}`;
      const pollQuestion = `QA surface probe ${pollName}?`;
      // Widget-shaped payload (the published poll widget posts pollSubmits;
      // pollResults is the read-back shape, not the submit shape).
      const seed = await request.post(`${svc}/save`, {
        data: {
          pollName,
          pollQuestion,
          pollSubmits: { Yes: false, No: false },
          restrictBySession: false,
        },
      });
      expect(seed.ok(), `polls: seeding '${pollName}' must be accepted`).toBe(true);

      const read = await request.get(`${svc}/${encodeURIComponent(pollName)}`);
      expect(read.status(), `polls: re-read of '${pollName}' must stay 200`).toBe(200);
      const body = await readJson(read);
      expect(
        body && body.status === "SUCCESS" && body.result != null,
        `polls: seeded poll '${pollName}' must read back SUCCESS`,
      ).toBe(true);
      const before = body.result.totalVotes;
      const beforeYes = (body.result.pollResults || {})["Yes"] || 0;

      const save = await request.post(`${svc}/save`, {
        data: {
          pollName: body.result.pollName || pollName,
          pollQuestion: body.result.pollQuestion,
          pollSubmits: { Yes: true, No: false },
          restrictBySession: false,
        },
      });
      expect(save.ok(), `polls: vote on '${pollName}' must be accepted`).toBe(true);

      const reread = await request.get(`${svc}/${encodeURIComponent(pollName)}`);
      expect(reread.status(), `polls: re-read of '${pollName}' must stay 200`).toBe(200);
      const after = await readJson(reread);
      expect(
        after && after.result ? after.result.totalVotes : null,
        `polls: vote on '${pollName}' must persist (totalVotes ${before} -> ?)`,
      ).toBe(before + 1);
      expect(
        after && after.result && after.result.pollResults
          ? after.result.pollResults["Yes"]
          : null,
        `polls: vote for 'Yes' on '${pollName}' must persist (${beforeYes} -> ?)`,
      ).toBe(beforeYes + 1);
    },
  );
});
