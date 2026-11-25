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
 * Cookie-consumer regression for the npm cookie swap
 * (issue #7177, parent audit #5627).
 *
 * The deleted bannerless `$.cookie` rewrite is replaced on published pages
 * by the Klaus Hartl `jquery.cookie` 1.4.1 dist (shipped to published
 * `web_resources/cm/jslib/` by `installDistributionFiles.xml`). The harness
 * loads the real `jquery-migrate` + `jquery.cookie` dists in production
 * order (jquery 4 dropped `$.isFunction`, which the dist touches; the
 * installer ships migrate alongside it), then the service source, and
 * pins the three consumer fixes:
 *
 *   1. `PercLikedService.isLiked` treats a missing key as "not liked"
 *      (the dist returns `undefined` where the rewrite returned `null`;
 *      without the guard the `toLowerCase` call throws on every un-liked
 *      page view).
 *   2. `setLiked(url, false)` deletes the marker via `$.removeCookie`
 *      (the dist would otherwise persist the literal `"null"` string for
 *      a year instead of deleting).
 *   3. Membership session writes use path-only options (the old
 *      `expires: "null"` marker was a session cookie under the rewrite
 *      but throws under the dist, which calls `toUTCString` on it).
 */

import { readFileSync } from "node:fs";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { describe, it, expect, beforeAll } from "vitest";

const __dirname = dirname(fileURLToPath(import.meta.url));
const MODULE_ROOT = resolve(__dirname, "../../../..");

function loadDist(relativePath) {
  const src = readFileSync(join(MODULE_ROOT, relativePath), "utf8");
  new Function("window", "jQuery", "$", src)(
    globalThis.window,
    globalThis.jQuery,
    globalThis.$,
  );
}

beforeAll(() => {
  // Production order on published pages: core, then migrate (restores
  // $.isFunction on jquery 4), then the cookie plugin, then the services.
  loadDist("node_modules/jquery-migrate/dist/jquery-migrate.js");
  loadDist("node_modules/jquery.cookie/jquery.cookie.js");
  loadDist("src/main/js/services/PercLikedService.js");
});

describe("PercLikedService cookie semantics on the npm dist (issue #7177)", () => {
  it("isLiked returns false (no throw) when the visitor has no like cookie", () => {
    const url = "http://example.invalid/page-7177-unliked";
    expect($.cookie(url)).toBeUndefined();
    expect(() => $.PercLikedService.isLiked(url)).not.toThrow();
    expect($.PercLikedService.isLiked(url)).toBe(false);
  });

  it("isLiked reads a persisted like case-insensitively", () => {
    const url = "http://example.invalid/page-7177-liked";
    $.PercLikedService.setLiked(url, true);
    expect($.PercLikedService.isLiked(url)).toBe(true);
    $.cookie(url, "TRUE", { expires: 365 });
    expect($.PercLikedService.isLiked(url)).toBe(true);
    $.PercLikedService.setLiked(url, false);
  });

  it("setLiked(false) deletes the marker instead of persisting 'null'", () => {
    const url = "http://example.invalid/page-7177-unlike";
    $.PercLikedService.setLiked(url, true);
    expect($.cookie(url)).toBe("true");
    $.PercLikedService.setLiked(url, false);
    expect($.cookie(url)).toBeUndefined();
    expect($.PercLikedService.isLiked(url)).toBe(false);
    // No year-long "null" residue may remain in the jar.
    expect(document.cookie).not.toContain("null");
  });

  it("setLiked(false) also clears a pre-migration verbatim marker", () => {
    // The rewrite stored the URL key verbatim; the npm dist encodes it, so
    // removeCookie alone misses legacy markers. Simulate the legacy jar
    // entry directly, then unlike: the marker must be gone afterwards.
    const url = "http://example.invalid/legacy-7177";
    document.cookie = `${url}=true`;
    expect($.PercLikedService.isLiked(url)).toBe(true);
    $.PercLikedService.setLiked(url, false);
    expect($.PercLikedService.isLiked(url)).toBe(false);
    expect(document.cookie).not.toContain("legacy-7177");
  });

  it("isLiked still honors a legacy marker for a percent-escaped URL", () => {
    // The rewrite stored the URL key verbatim; the npm dist decodes names
    // before comparing, so without the raw-jar fallback this pre-migration
    // marker would read as missing.
    window.history.pushState({}, "", "/news/a%20b-7177.html");
    try {
      const url = "http://example.invalid/news/a%20b-7177";
      document.cookie = `${url}=true`;
      expect($.cookie(url)).toBeUndefined();
      expect($.PercLikedService.isLiked(url)).toBe(true);
      $.PercLikedService.setLiked(url, false);
      expect($.PercLikedService.isLiked(url)).toBe(false);
    } finally {
      window.history.pushState({}, "", "/");
    }
  });
  it("setLiked(false) also clears a pre-migration marker from a nested page", () => {
    // Browser default cookie paths have no trailing slash (/news, not
    // /news/); the legacy expiry must target the exact path or the marker
    // survives the unlike on nested pages.
    window.history.pushState({}, "", "/news/page-7177.html");
    try {
      const url = "http://example.invalid/news/page-7177";
      document.cookie = `${url}=true`;
      expect($.PercLikedService.isLiked(url)).toBe(true);
      $.PercLikedService.setLiked(url, false);
      expect($.PercLikedService.isLiked(url)).toBe(false);
      expect(document.cookie).not.toContain("page-7177");
    } finally {
      window.history.pushState({}, "", "/");
    }
  });

  it("membership session writes use a path-only session cookie (no expires marker)", () => {
    // Exact options shape the login/registration views now pass: the old
    // expires:"null" marker threw under the npm dist (t.toUTCString is not
    // a function on a string), so it was dropped (session cookie, as the
    // rewrite effectively produced).
    expect(() =>
      $.cookie("perc_membership_session_id", "session-7177", { path: "/" }),
    ).not.toThrow();
    expect($.cookie("perc_membership_session_id")).toBe("session-7177");
    // The expires:"null" marker must be gone from every membership write.
    for (const view of ["PercLoginView.js", "PercRegistrationView.js"]) {
      const src = readFileSync(
        join(MODULE_ROOT, "src/main/js/views", view),
        "utf8",
      );
      expect(src, `${view} still passes expires:"null"`).not.toContain(
        'expires: "null"',
      );
    }
  });
});
