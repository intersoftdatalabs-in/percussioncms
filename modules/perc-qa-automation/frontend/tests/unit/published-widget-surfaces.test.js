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
 * Unit tests for the slice-4 DTS widget surface helpers (#6244). No live
 * CMS, no DTS cell, and no seeded fixtures required:
 * {@code npm run test:unit}.
 */

"use strict";

const assert = require("node:assert");
const { describe, it } = require("node:test");

const {
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
} = require("../helpers/published-widget-surfaces");

const {
  WIDGETS,
} = require("../helpers/published-widget-matrix");

function withEnv(name, value, fn) {
  const previous = process.env[name];
  if (value === undefined) {
    delete process.env[name];
  } else {
    process.env[name] = value;
  }
  try {
    fn();
  } finally {
    if (previous === undefined) {
      delete process.env[name];
    } else {
      process.env[name] = previous;
    }
  }
}

/** Minimal Playwright-page stub: captures `on` handlers for replay. */
function stubPage() {
  const handlers = {};
  return {
    handlers,
    on(event, fn) {
      handlers[event] = handlers[event] || [];
      handlers[event].push(fn);
    },
  };
}

/** Playwright-expect stub: throws when the received value is non-empty. */
function stubExpect() {
  return (received, message) => ({
    toEqual(expected) {
      assert.deepEqual(
        received,
        expected,
        `${message} (received ${JSON.stringify(received)})`,
      );
    },
  });
}

describe("published-widget-surfaces helpers (#6244)", () => {
  it("pins exactly the five AC3 dynamic classes", () => {
    assert.equal(SURFACE_SUITE, "published-widget-surfaces");
    assert.deepEqual(
      SURFACES.map((entry) => entry.name),
      ["poll", "feed", "metadata", "form", "comments"],
    );
  });

  it("gives every surface unique names, tags, fixtures, markers, and roots", () => {
    for (const key of ["name", "tag", "fixture", "marker", "root", "context"]) {
      const values = SURFACES.map((entry) => entry[key]);
      assert.equal(
        new Set(values).size,
        values.length,
        `duplicate ${key}: ${values.join(", ")}`,
      );
    }
    for (const entry of SURFACES) {
      for (const key of ["label", "widget", "csrf", "interaction"]) {
        assert.ok(
          typeof entry[key] === "string" && entry[key].length > 0,
          `${entry.name}: ${key} must be a non-empty string`,
        );
      }
      assert.equal(entry.tag, tagForSurface(entry.name));
    }
  });

  it("reuses the US2 shell markers for the dynamic widgets", () => {
    const byName = new Map(WIDGETS.map((widget) => [widget.name, widget]));
    for (const entry of SURFACES) {
      if (entry.name === "metadata") {
        // Metadata has no dedicated widget (backs auto-lists, most-read,
        // consent); the surface borrows the most-read shell.
        assert.equal(entry.widget, "percMostReadBlogPosts");
      }
      const widget = byName.get(entry.widget);
      assert.ok(widget, `${entry.widget} must stay pinned in the US2 matrix`);
      assert.equal(
        widget.marker,
        entry.marker,
        `${entry.widget}: surface marker must match the US2 shell marker`,
      );
    }
  });

  it("leaves membership out (needs a seeded account the cell does not provision)", () => {
    const widgets = SURFACES.map((entry) => entry.widget);
    for (const member of ["percLogin", "percRegistration", "percSecureLogin"]) {
      assert.ok(
        !widgets.includes(member),
        `${member} must stay out of the surface inventory`,
      );
    }
  });

  it("reads the published-site origin from PUBLISHED_SITE_URL", () => {
    withEnv("PUBLISHED_SITE_URL", undefined, () => {
      assert.equal(publishedSiteUrl(), null);
    });
    withEnv("PUBLISHED_SITE_URL", "https://published-cell:8443/", () => {
      assert.equal(publishedSiteUrl().origin, "https://published-cell:8443");
    });
  });

  it("reads the DTS origin from DTS_URL (the cell banner variable)", () => {
    withEnv("DTS_URL", undefined, () => {
      assert.equal(dtsBaseUrl(), null);
    });
    withEnv("DTS_URL", "http://127.0.0.1:9983/", () => {
      assert.equal(dtsBaseUrl().origin, "http://127.0.0.1:9983");
    });
  });

  it("builds widget fixture page URLs under the staged widgets dir", () => {
    const site = new URL("https://published-cell:8443/");
    const poll = SURFACES.find((entry) => entry.name === "poll");
    assert.equal(
      fixturePageUrl(site, poll),
      "https://published-cell:8443/widgets/percPoll.html",
    );
  });

  it("builds service URLs through the deployment context path", () => {
    const dts = new URL("http://127.0.0.1:9983/");
    const byName = new Map(SURFACES.map((entry) => [entry.name, entry]));
    assert.equal(
      dtsContextUrl(dts, byName.get("feed")),
      "http://127.0.0.1:9983/feeds",
    );
    assert.equal(
      dtsServiceUrl(dts, byName.get("poll")),
      "http://127.0.0.1:9983/perc-polls-services/polls",
    );
    assert.equal(
      dtsServiceUrl(dts, byName.get("poll")),
      "http://127.0.0.1:9983/perc-polls-services/polls",
    );
    assert.equal(
      dtsServiceUrl(dts, byName.get("feed")),
      "http://127.0.0.1:9983/feeds/rss",
    );
    assert.equal(
      dtsServiceUrl(dts, byName.get("metadata")),
      "http://127.0.0.1:9983/perc-metadata-services/metadata",
    );
    assert.equal(
      dtsServiceUrl(dts, byName.get("form")),
      "http://127.0.0.1:9983/perc-form-processor/forms",
    );
    assert.equal(
      dtsServiceUrl(dts, byName.get("comments")),
      "http://127.0.0.1:9983/perc-comments-services/comment",
    );
  });

  it("names the missing env in both skip reasons", () => {
    assert.ok(SKIP_NO_SITE.includes("PUBLISHED_SITE_URL"));
    assert.ok(SKIP_NO_DTS.includes("DTS_URL"));
    const reason = skipNoFixture(SURFACES[0], "probe detail");
    assert.ok(reason.includes("poll"));
    assert.ok(reason.includes("probe detail"));
  });

  it("passes a quiet page and fails a noisy one", () => {
    const page = stubPage();
    const noise = watchNoise(page);
    assert.deepEqual(Object.keys(noise).sort(), [
      "consoleErrors",
      "failedRequests",
      "pageErrors",
    ]);
    assertQuiet(SURFACES[0], noise, stubExpect());

    const noisy = stubPage();
    const bad = watchNoise(noisy);
    noisy.handlers.pageerror.forEach((fn) => fn(new Error("boom")));
    assert.throws(() => assertQuiet(SURFACES[0], bad, stubExpect()));

    const failed = stubPage();
    const badRequests = watchNoise(failed);
    failed.handlers.requestfailed.forEach((fn) =>
      fn({ url: () => "https://x/widgets/percPoll.html", failure: () => ({ errorText: "reset" }) }),
    );
    assert.throws(() => assertQuiet(SURFACES[0], badRequests, stubExpect()));

    const consoleNoisy = stubPage();
    const badConsole = watchNoise(consoleNoisy);
    consoleNoisy.handlers.console.forEach((fn) =>
      fn({ type: () => "error", location: () => ({ url: "https://x/app.js" }), text: () => "bang" }),
    );
    assert.throws(() => assertQuiet(SURFACES[0], badConsole, stubExpect()));
  });

  it("ignores favicon misses (the static fixture ships no icon)", () => {
    const page = stubPage();
    const noise = watchNoise(page);
    page.handlers.requestfailed.forEach((fn) =>
      fn({ url: () => "https://x/favicon.ico", failure: () => ({ errorText: "reset" }) }),
    );
    assertQuiet(SURFACES[0], noise, stubExpect());
  });
});
