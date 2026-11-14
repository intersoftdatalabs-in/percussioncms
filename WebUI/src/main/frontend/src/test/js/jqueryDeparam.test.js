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
 * Parity tests for the first-party `jquery.deparam.js` shim (issue #5663).
 *
 * The shim replaces the query-string half of the vendored jQuery BBQ v1.1
 * copy (`jquery-perc-retiredjs/jquery.bbq.js`, dead upstream since 2010),
 * which is deleted by this change. Every expectation below was verified
 * byte-identical against the real BBQ 1.1 implementation (node + jsdom
 * side-by-side harness) before the vendored file was removed — including
 * BBQ quirks (e.g. `a[]=1` after `a[b]=c` collapses under an `"undefined"`
 * key; a bare URL with no `?` parses as a single empty-valued key).
 *
 * The classic UI only calls `$.deparam.querystring()` (current page or an
 * explicit URL) and `$.param.querystring(base, params)` (navigation URL
 * building in PercNavigationManager). Fragment/hash-state, `$.bbq.*`,
 * `$.elemUrlAttr`, and `$.fn.*` helpers had zero repo-wide callers and are
 * intentionally not ported.
 */

import { readFileSync } from "fs";
import { resolve, dirname } from "path";
import { fileURLToPath } from "url";
import jquery from "jquery";
import { describe, it, expect, beforeEach, afterEach } from "vitest";

const __dirname = dirname(fileURLToPath(import.meta.url));
const SRC_PATH = resolve(
  __dirname,
  "../../../../webapp/cm/jslib/profiles/3x/jquery/plugins/jquery-perc-retiredjs/jquery.deparam.js",
);

const LOCATION_URL =
  "http://localhost/cm/app/?site=Acme&view=arch&path=%2FSites%2FAcme&debug=true&m=1&m=2&empty=&bare&plus=a+b&num=42";

let $;
let realLocation;

function loadSource() {
  let jq = jquery(globalThis.window);
  if (typeof jq !== "function") {
    jq = typeof jquery === "function" ? jquery : jquery.fn || jquery;
    if (!jq.fn && jq.prototype) jq.fn = jq.prototype;
  }
  if (!jq.fn) throw new Error("jquery has no .fn");
  $ = jq;
  globalThis.jQuery = $;
  globalThis.$ = $;

  const src = readFileSync(SRC_PATH, "utf8");
  // Run the source as a script in the global scope so its IIFE sees the
  // globalThis jQuery/window this page would provide via common_js.jsp.
  const run = new Function("window", "jQuery", "$", src);
  run(globalThis.window, $, $);
}

beforeEach(() => {
  loadSource();
  realLocation = globalThis.window.location;
  Object.defineProperty(globalThis.window, "location", {
    value: new URL(LOCATION_URL),
    configurable: true,
  });
});

afterEach(() => {
  Object.defineProperty(globalThis.window, "location", {
    value: realLocation,
    configurable: true,
  });
  delete globalThis.jQuery;
  delete globalThis.$;
});

describe("jquery.deparam.js first-party shim (issue #5663)", () => {
  it("installs the BBQ query-string surface and nothing else", () => {
    expect(typeof $.deparam).toBe("function");
    expect(typeof $.deparam.querystring).toBe("function");
    expect(typeof $.param.querystring).toBe("function");
    // Hash-state / DOM helpers are intentionally not ported.
    expect($.bbq).toBeUndefined();
    expect($.deparam.fragment).toBeUndefined();
    expect($.param.fragment).toBeUndefined();
    expect($.elemUrlAttr).toBeUndefined();
  });

  it("parses the current page query string", () => {
    expect($.deparam.querystring()).toEqual({
      site: "Acme",
      view: "arch",
      path: "/Sites/Acme",
      debug: "true",
      m: ["1", "2"],
      empty: "",
      bare: "",
      plus: "a b",
      num: "42",
    });
  });

  it("parses an explicit URL and ignores the hash", () => {
    expect($.deparam.querystring(LOCATION_URL)).toEqual(
      $.deparam.querystring(),
    );
    expect($.deparam.querystring("http://h/p#x")).toEqual({
      "http://h/p": "",
    });
    expect($.deparam.querystring("http://h/p?a=1#x=9")).toEqual({ a: "1" });
    expect($.deparam.querystring("a=1&b=2")).toEqual({ a: "1", b: "2" });
  });

  it("decodes + as space and percent escapes", () => {
    expect($.deparam.querystring("q=a+b&path=%2FSites%2FAcme")).toEqual({
      q: "a b",
      path: "/Sites/Acme",
    });
  });

  it("collects repeated keys into arrays", () => {
    expect($.deparam("m=1&m=2")).toEqual({ m: ["1", "2"] });
  });

  it("matches BBQ bracket and coercion semantics", () => {
    // BBQ quirk: `a[]` after `a[b]=c` lands under an "undefined" key.
    expect($.deparam("a[b]=c&a[]=1&a[]=2")).toEqual({
      a: { b: "c", undefined: "2" },
    });
    expect($.deparam("a=1&a=2", true)).toEqual({ a: [1, 2] });
    expect(
      $.deparam.querystring(
        "http://h/?n=42&b=true&z=null&u=undefined&s=x",
        true,
      ),
    ).toEqual({ n: 42, b: true, z: null, u: undefined, s: "x" });
  });

  it("neutralizes prototype-pollution keys", () => {
    const parsed = $.deparam("__proto__[x]=1&a=2");
    expect(parsed).toEqual({ __PROTO__: { x: "1" }, a: "2" });
    expect({}.x).toBeUndefined();
  });

  it("round-trips the current query string via $.param.querystring", () => {
    expect($.param.querystring()).toBe(
      "site=Acme&view=arch&path=%2FSites%2FAcme&debug=true&m=1&m=2&empty=&bare&plus=a+b&num=42",
    );
    expect($.param.querystring(LOCATION_URL)).toBe($.param.querystring());
  });

  it("merges params into a base URL, passed params winning by default", () => {
    // PercNavigationManager.createUrl shape.
    expect(
      $.param.querystring("/cm/app/", {
        view: "arch",
        site: "Acme",
        path: "/Sites/Acme",
        debug: "true",
      }),
    ).toBe("/cm/app/?view=arch&site=Acme&path=%2FSites%2FAcme&debug=true");
    expect(
      $.param.querystring("/cm/app/?view=old&keep=1", {
        view: "arch",
        site: "Acme",
      }),
    ).toBe("/cm/app/?view=arch&keep=1&site=Acme");
  });

  it("honors merge modes 1 (url wins) and 2 (replace)", () => {
    expect(
      $.param.querystring(
        "/cm/app/?view=old&keep=1",
        { view: "arch", site: "Acme" },
        1,
      ),
    ).toBe("/cm/app/?view=old&site=Acme&keep=1");
    expect(
      $.param.querystring("/cm/app/?view=old&keep=1", { view: "arch" }, 2),
    ).toBe("/cm/app/?view=arch");
    expect(
      $.param.querystring("/cm/app/?view=old#h", "view=arch&x=1", 2),
    ).toBe("/cm/app/?view=arch&x=1#h");
  });

  it("accepts params as a string and preserves the hash", () => {
    expect($.param.querystring("/cm/app/?view=old", "view=arch&x=1")).toBe(
      "/cm/app/?view=arch&x=1",
    );
  });

  it("serializes nested memento payloads like navigation bookmarks", () => {
    expect(
      $.param.querystring("/cm/app/", {
        view: "edit",
        memento: JSON.stringify({ a: 1, b: "x y" }),
      }),
    ).toBe(
      "/cm/app/?view=edit&memento=%7B%22a%22%3A1%2C%22b%22%3A%22x%20y%22%7D",
    );
  });
});
