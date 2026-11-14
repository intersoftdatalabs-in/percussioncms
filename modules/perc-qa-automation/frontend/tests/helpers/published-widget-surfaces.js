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
 * Closed inventory + skip-contract helpers for the slice-4 DTS widget
 * surface specs (#6244 / parent #6237): one published page per dynamic
 * widget class (poll, RSS feed, metadata, form, comments), each spec
 * asserting the published shell plus the backing DTS round-trip.
 *
 * <p>The pinned {@link SURFACES} list is the source of truth for the five
 * AC3 dynamic classes. Each entry carries the published fixture page (one
 * of the per-widget pages staged by
 * {@code docker/scripts/qa_publish_fixture.py} under
 * {@code widgets/<Widget>.html}), the body marker the shell case asserts,
 * and the DTS service root backing the interaction case. Widgets backed by
 * the membership service (login / registration / secure login) are
 * intentionally out of scope: their interaction needs a seeded account
 * ({@code DTS_FIXTURE_USER} / {@code DTS_FIXTURE_PASSWORD}), which the
 * #6237 cell does not provision — see the US3 matrix (#6241).</p>
 *
 * <p>Two-level skip contract (mirrors the slice-1 seed
 * {@code tests/published-site-smoke.spec.js} and the spec env table): the
 * shell case skips without {@code PUBLISHED_SITE_URL}; the interaction
 * case skips without {@code DTS_URL}. With both set (the
 * {@code qa-up --with-dts} banner prints both) every case runs green
 * against the in-cell DTS — interactions self-seed with unique per-run
 * markers, so no pre-seeded DTS fixtures are required.</p>
 *
 * <p>URL paths below use {@code /} (web origins, not OS file paths).</p>
 */

"use strict";

const SURFACE_SUITE = "published-widget-surfaces";

/** Skip reason when the cell serves no published site (default H2 QA cell). */
const SKIP_NO_SITE =
  "needs PUBLISHED_SITE_URL=(in-cell published site origin); " +
  "default H2 QA cell has no DTS publish path (#6237)";

/** Skip reason when the cell serves no DTS origin (default H2 QA cell). */
const SKIP_NO_DTS =
  "needs DTS_URL=(in-cell DTS origin from the qa-up --with-dts banner); " +
  "default H2 QA cell serves no DTS services (#6237)";

/**
 * Skip reason when the interaction fixture cannot be established against
 * the live DTS origin. Each skip names the exact gap so a future fixture
 * slice can close it without touching the spec.
 *
 * @param {{ name: string, label: string }} entry surface entry
 * @param {string} detail what was probed and what is missing
 * @returns {string}
 */
function skipNoFixture(entry, detail) {
  return (
    `surface ${entry.name} (${entry.label}): ${detail}; ` +
    "seed the interaction fixture on the --with-dts cell (parent #6237) " +
    "to turn this case green"
  );
}

/**
 * One entry per AC3 dynamic widget class.
 *
 * <p>{@code fixture} is the staged fixture page name (probed at
 * {@code <origin>/widgets/<fixture>.html}); {@code marker} the body CSS
 * selector the shell case asserts. {@code context} is the DTS Tomcat
 * context path (the war name) and {@code root} the JAX-RS root — the
 * published widgets address the services through the full context path
 * (see the widget JS, e.g. the form widget posts to
 * {@code <base>/perc-form-processor/forms/form/collect/}), so the
 * interaction cases do the same; {@code csrf} the {@code HEAD} liveness
 * probe every service implements. {@code interaction} names the round-trip
 * the interaction case proves.</p>
 */
const SURFACES = Object.freeze([
  Object.freeze({
    name: "poll",
    label: "Poll",
    widget: "percPoll",
    fixture: "percPoll",
    marker: ".perc-poll-render-container",
    context: "perc-polls-services",
    root: "/polls",
    csrf: "/polls/csrf",
    tag: "@surface-poll",
    interaction:
      "vote persists: self-seeded poll saves, re-read shows the vote",
  }),
  Object.freeze({
    name: "feed",
    label: "RSS Feed",
    widget: "percRss",
    fixture: "percRss",
    marker: ".perc-feed-widget",
    context: "feeds",
    root: "/rss",
    csrf: "/rss/csrf",
    tag: "@surface-feed",
    interaction:
      "feed endpoint serves the fixture probe from the live service",
  }),
  Object.freeze({
    name: "metadata",
    label: "Metadata",
    widget: "percMostReadBlogPosts",
    fixture: "percMostReadBlogPosts",
    marker: ".perc-most-read-blog-posts-view",
    context: "perc-metadata-services",
    root: "/metadata",
    csrf: "/metadata/csrf",
    tag: "@surface-metadata",
    interaction:
      "visit tracks and index answers: trackblogpost accepted, topblogposts shape holds",
  }),
  Object.freeze({
    name: "form",
    label: "Form",
    widget: "percForm",
    fixture: "percForm",
    marker: ".perc-form",
    context: "perc-form-processor",
    root: "/forms",
    csrf: "/forms/csrf",
    tag: "@surface-form",
    interaction:
      "submit collects: self-seeded form posts to the open collect path and takes the success redirect",
  }),
  Object.freeze({
    name: "comments",
    label: "Comments",
    widget: "percComments",
    fixture: "percComments",
    marker: ".perc-comments-view",
    context: "perc-comments-services",
    root: "/comment",
    csrf: "/comment/csrf",
    tag: "@surface-comments",
    interaction:
      "comment appears per moderation rules: addcomment accepted, list follows the default state",
  }),
]);

/**
 * @param {string} name surface name
 * @returns {string} Playwright tag for the surface's cases
 */
function tagForSurface(name) {
  return `@surface-${name}`;
}

/**
 * @returns {URL|null} published site origin, or null when the cell serves
 * no published site (default H2 QA cell: shell cases skip named)
 */
function publishedSiteUrl() {
  const siteUrl = process.env.PUBLISHED_SITE_URL || "";
  if (!siteUrl) {
    return null;
  }
  return new URL(siteUrl);
}

/**
 * @returns {URL|null} in-cell DTS origin, or null when the cell serves no
 * DTS (default H2 QA cell: interaction cases skip named)
 */
function dtsBaseUrl() {
  const baseUrl = process.env.DTS_URL || "";
  if (!baseUrl) {
    return null;
  }
  return new URL(baseUrl);
}

/**
 * @param {URL} siteOrigin published site origin from {@link publishedSiteUrl}
 * @param {{ fixture: string }} entry surface entry
 * @returns {string} fixture page URL for the surface's shell case
 */
function fixturePageUrl(siteOrigin, entry) {
  return `${siteOrigin.origin}/widgets/${entry.fixture}.html`;
}

/**
 * Deployment context URL for the surface's interaction case: the DTS origin
 * plus the deployment context path (war name), without the JAX-RS root.
 * Liveness probes hang off this URL (the csrf paths are root-relative).
 *
 * @param {URL} dtsOrigin in-cell DTS origin from {@link dtsBaseUrl}
 * @param {{ context: string }} entry surface entry
 * @returns {string} context URL (no trailing slash)
 */
function dtsContextUrl(dtsOrigin, entry) {
  return `${dtsOrigin.origin}/${entry.context}`;
}

/**
 * Service base URL for the surface's interaction case: the DTS origin plus
 * the deployment context path (war name) plus the JAX-RS root, mirroring
 * how the published widgets address the services.
 *
 * @param {URL} dtsOrigin in-cell DTS origin from {@link dtsBaseUrl}
 * @param {{ context: string, root: string }} entry surface entry
 * @returns {string} service base URL (no trailing slash)
 */
function dtsServiceUrl(dtsOrigin, entry) {
  return `${dtsContextUrl(dtsOrigin, entry)}${entry.root}`;
}

/**
 * Subscribes page-noise collectors: page errors, failed requests, HTTP
 * error responses, and JS console errors. Returns the live arrays the
 * caller asserts with {@link assertQuiet}.
 *
 * @param {import("@playwright/test").Page} page Playwright page
 * @returns {{ pageErrors: string[], failedRequests: string[], consoleErrors: string[] }}
 */
function watchNoise(page) {
  const pageErrors = [];
  const failedRequests = [];
  const consoleErrors = [];
  page.on("pageerror", (err) =>
    pageErrors.push(String((err && err.stack) || err)),
  );
  page.on("requestfailed", (req) => {
    failedRequests.push(`${req.url()} :: ${req.failure()?.errorText}`);
  });
  page.on("response", (resp) => {
    if (resp.status() >= 400) {
      failedRequests.push(`${resp.url()} :: HTTP ${resp.status()}`);
    }
  });
  page.on("console", (msg) => {
    if (msg.type() === "error") {
      consoleErrors.push(`${msg.location().url} :: ${msg.text()}`);
    }
  });
  return { pageErrors, failedRequests, consoleErrors };
}

/**
 * Fails the case when the published shell left noise behind (missing
 * resources, page errors, JS console errors). Favicon misses are ignored
 * — the static fixture ships no icon.
 *
 * @param {{ name: string }} entry surface entry
 * @param {{ pageErrors: string[], failedRequests: string[], consoleErrors: string[] }} noise
 * @param {import("@playwright/test").Expect} expect Playwright expect
 */
function assertQuiet(entry, noise, expect) {
  const seriousFailures = noise.failedRequests.filter(
    (text) => !/favicon\.ico/i.test(text),
  );
  expect(
    seriousFailures,
    `${entry.name}: missing published resources:\n${seriousFailures.join("\n")}`,
  ).toEqual([]);
  expect(
    noise.pageErrors,
    `${entry.name}: page errors:\n${noise.pageErrors.join("\n")}`,
  ).toEqual([]);
  expect(
    noise.consoleErrors,
    `${entry.name}: JS console errors:\n${noise.consoleErrors.join("\n")}`,
  ).toEqual([]);
}

module.exports = {
  SURFACE_SUITE,
  SKIP_NO_SITE,
  SKIP_NO_DTS,
  skipNoFixture,
  SURFACES,
  tagForSurface,
  publishedSiteUrl,
  dtsBaseUrl,
  fixturePageUrl,
  dtsContextUrl,
  dtsServiceUrl,
  watchNoise,
  assertQuiet,
};
