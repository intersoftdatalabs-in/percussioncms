/**
 * Copyright 1999-2026 Percussion Software, Inc.
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
 * Tests for bbq-shim.js
 *
 * Verifies that the URLSearchParams-based shim exposes the same behaviour that
 * jquery-bbq provided for the two API surfaces used across the Perc delivery
 * widgets:
 *
 *   $.deparam.querystring()         — parse current URL query string → object
 *   $.param.querystring()           — retrieve current URL query string
 *   $.param.querystring("", obj)    — serialize an object → query string
 */

import { readFileSync } from "fs";
import { resolve, dirname } from "path";
import { fileURLToPath } from "url";
import { beforeEach, afterEach, describe, it, expect, vi } from "vitest";

const __dirname = dirname(fileURLToPath(import.meta.url));
const ROOT = resolve(__dirname, "../../../..");

// Load and evaluate the shim once — it extends the global $ installed by setup.js
const shimCode = readFileSync(
  resolve(ROOT, "src/main/js/shims/bbq-shim.js"),
  "utf8",
);
eval(shimCode); // eslint-disable-line no-eval

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------
function stubSearch(search) {
  vi.stubGlobal("location", { search });
}

afterEach(() => {
  vi.unstubAllGlobals();
});

// ---------------------------------------------------------------------------
// $.deparam.querystring()
// ---------------------------------------------------------------------------
describe("$.deparam.querystring()", () => {
  it("is defined on $", () => {
    expect(typeof $.deparam).toBe("object");
    expect(typeof $.deparam.querystring).toBe("function");
  });

  it("returns an empty object when the query string is empty", () => {
    stubSearch("");
    expect($.deparam.querystring()).toEqual({});
  });

  it("parses a single key-value pair", () => {
    stubSearch("?page=2");
    expect($.deparam.querystring()).toEqual({ page: "2" });
  });

  it("parses multiple key-value pairs", () => {
    stubSearch("?foo=bar&baz=qux");
    expect($.deparam.querystring()).toEqual({ foo: "bar", baz: "qux" });
  });

  it("decodes percent-encoded characters", () => {
    stubSearch("?q=hello%20world");
    expect($.deparam.querystring()).toEqual({ q: "hello world" });
  });

  it("handles keys with no value", () => {
    stubSearch("?flag");
    expect($.deparam.querystring()).toEqual({ flag: "" });
  });
});

// ---------------------------------------------------------------------------
// $.param.querystring() — no arguments → return current search string
// ---------------------------------------------------------------------------
describe("$.param.querystring() — no arguments", () => {
  it("is defined on $.param", () => {
    expect(typeof $.param.querystring).toBe("function");
  });

  it("returns the current window.location.search unchanged", () => {
    stubSearch("?foo=bar");
    expect($.param.querystring()).toBe("?foo=bar");
  });

  it("returns an empty string when there is no query string", () => {
    stubSearch("");
    expect($.param.querystring()).toBe("");
  });
});

// ---------------------------------------------------------------------------
// $.param.querystring("", obj) — serialize object to query string
// ---------------------------------------------------------------------------
describe('$.param.querystring("", obj)', () => {
  it('returns "" for an empty object', () => {
    expect($.param.querystring("", {})).toBe("");
  });

  it("serializes a single key-value pair with a leading ?", () => {
    expect($.param.querystring("", { page: "2" })).toBe("?page=2");
  });

  it("serializes multiple keys and includes all of them", () => {
    const result = $.param.querystring("", { a: "1", b: "2" });
    expect(result).toMatch(/^\?/);
    // Order is not guaranteed by URLSearchParams so check keys individually
    const params = new URLSearchParams(result.slice(1));
    expect(params.get("a")).toBe("1");
    expect(params.get("b")).toBe("2");
  });

  it("percent-encodes values with spaces", () => {
    const result = $.param.querystring("", { q: "hello world" });
    expect(result).toBe("?q=hello+world");
  });
});
