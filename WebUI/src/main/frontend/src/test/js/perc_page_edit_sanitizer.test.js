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
 * Tests for the `window.PercPageEditSanitizer` exposed by
 * `WebUI/src/main/webapp/cm/widgets/perc_page_edit_dialog.js`.
 *
 * Background:
 *   The CMS auto-generates a read-only "page summary" preview from the
 *   TinyMCE-authored HTML. Rendering untrusted HTML through `.html()` was
 *   flagged by CodeQL as js/xss-through-dom. The fix replaces that sink
 *   with a DOM-based sanitizer that:
 *     1. Uses DOMParser to turn the string into real DOM nodes (parsing
 *        does not execute scripts or load remote resources per the HTML
 *        spec).
 *     2. Removes a known-dangerous tag blocklist.
 *     3. Strips `on*` event-handler attributes and dangerous URL schemes
 *        (javascript:, data:, vbscript:) from URL-bearing attributes.
 *     4. Returns the sanitized body Element so callers can append its
 *        children to the live document via `Node.appendChild()`.
 *
 *   Loading pattern: the file under test is a legacy IIFE that expects
 *   a global jQuery. We install a stub jQuery, eval the file (mirror the
 *   legacy <script> runtime), then exercise the sanitizer via the
 *   global it publishes (`window.PercPageEditSanitizer`).
 */

import { readFileSync } from "fs";
import { resolve, dirname } from "path";
import { fileURLToPath } from "url";
import { beforeEach, afterEach, describe, it, expect } from "vitest";

const __dirname = dirname(fileURLToPath(import.meta.url));

// Resolve to the legacy dialog script:
//   file: WebUI/src/main/frontend/src/test/js/perc_page_edit_sanitizer.test.js
//   src:  WebUI/src/main/webapp/cm/widgets/perc_page_edit_dialog.js
// Up four levels from the file -> .../main, then descend into webapp/.
const SOURCE_PATH = resolve(
  __dirname,
  "../../../../webapp/cm/widgets/perc_page_edit_dialog.js"
);

// ---------------------------------------------------------------------------
// Bootstrap helpers
// ---------------------------------------------------------------------------

/**
 * Builds a minimal jQuery stub sufficient to load perc_page_edit_dialog.js.
 * The dialog file's IIFE only assigns `$.perc_page_edit_dialog = ...`, so
 * the stub only needs to be a regular object that accepts that property.
 */
function installJQueryStub() {
  globalThis.jQuery = {};
  globalThis.$ = globalThis.jQuery;
}

function installWindowAlias() {
  // The sanitizer IIFE uses `typeof window !== "undefined" ? window : this`.
  // In jsdom, window is already defined on globalThis, but be defensive.
  if (typeof globalThis.window === "undefined") {
    globalThis.window = globalThis;
  }
}

// ---------------------------------------------------------------------------
// Lifecycle
// ---------------------------------------------------------------------------

beforeEach(() => {
  installJQueryStub();
  installWindowAlias();
  // Clear any prior sanitizer registration so the source IIFE re-runs.
  delete globalThis.PercPageEditSanitizer;
  const code = readFileSync(SOURCE_PATH, "utf8");
  (0, eval)(code);
});

afterEach(() => {
  delete globalThis.PercPageEditSanitizer;
  delete globalThis.jQuery;
  delete globalThis.$;
});

// ---------------------------------------------------------------------------
// API surface
// ---------------------------------------------------------------------------

describe("PercPageEditSanitizer API surface", () => {
  it("is exposed on window after perc_page_edit_dialog.js loads", () => {
    expect(globalThis.PercPageEditSanitizer).toBeDefined();
    expect(typeof globalThis.PercPageEditSanitizer.sanitize).toBe("function");
  });

  it("is idempotent (defined-check prevents double registration)", () => {
    // Re-eval the source -- the top-level guard should short-circuit and
    // the existing global instance must not be replaced.
    const before = globalThis.PercPageEditSanitizer;
    const code = readFileSync(SOURCE_PATH, "utf8");
    (0, eval)(code);
    expect(globalThis.PercPageEditSanitizer).toBe(before);
  });

  it("publishes the dangerous-tag blocklist for documentation", () => {
    const tags = globalThis.PercPageEditSanitizer.DANGEROUS_TAGS;
    expect(tags).toContain("script");
    expect(tags).toContain("iframe");
    expect(tags).toContain("object");
    expect(tags).toContain("embed");
    expect(tags).toContain("form");
    expect(tags).toContain("style");
    expect(tags).toContain("base");
    expect(tags).toContain("link");
    expect(tags).toContain("meta");
  });
});

// ---------------------------------------------------------------------------
// sanitize() behaviour
// ---------------------------------------------------------------------------

describe("PercPageEditSanitizer.sanitize()", () => {
  it("returns null for empty / falsy input", () => {
    expect(globalThis.PercPageEditSanitizer.sanitize("")).toBeNull();
    expect(globalThis.PercPageEditSanitizer.sanitize(null)).toBeNull();
    expect(globalThis.PercPageEditSanitizer.sanitize(undefined)).toBeNull();
  });

  it("returns a detached DOM Element (parsed body) for non-empty input", () => {
    const result = globalThis.PercPageEditSanitizer.sanitize(
      "<p>Hello <strong>world</strong></p>"
    );
    expect(result).not.toBeNull();
    expect(result.nodeType).toBe(1); // ELEMENT_NODE
    expect(result.tagName.toLowerCase()).toBe("body");
  });

  it("preserves benign HTML structure and text", () => {
    const result = globalThis.PercPageEditSanitizer.sanitize(
      "<p>Hello <em>world</em></p>"
    );
    expect(result.querySelector("p")).not.toBeNull();
    expect(result.querySelector("em")).not.toBeNull();
    expect(result.textContent).toBe("Hello world");
  });
});

// ---------------------------------------------------------------------------
// Dangerous tag removal
// ---------------------------------------------------------------------------

describe("PercPageEditSanitizer removes dangerous elements", () => {
  it.each([
    "script",
    "iframe",
    "object",
    "embed",
    "form",
    "style",
    "base",
    "link",
    "meta",
  ])("strips <%s> elements entirely", (tag) => {
    const html =
      tag === "link" || tag === "meta" || tag === "base"
        ? `<${tag} href="x"><p>safe</p>`
        : `<p>safe</p><${tag}>evil</${tag}>`;
    const result = globalThis.PercPageEditSanitizer.sanitize(html);
    expect(result.querySelector(tag)).toBeNull();
  });

  it("drops the entire <script> subtree (including inline JS body)", () => {
    const result = globalThis.PercPageEditSanitizer.sanitize(
      "<p>before</p><script>alert(1)</script><p>after</p>"
    );
    expect(result.querySelector("script")).toBeNull();
    expect(result.textContent).toBe("beforeafter");
  });

  it("removes nested <script> inside allowed containers", () => {
    const result = globalThis.PercPageEditSanitizer.sanitize(
      "<div><p>text</p><script>alert('xss')</script></div>"
    );
    expect(result.querySelector("script")).toBeNull();
    expect(result.querySelector("p").textContent).toBe("text");
  });
});

// ---------------------------------------------------------------------------
// Attribute scrubbing
// ---------------------------------------------------------------------------

describe("PercPageEditSanitizer strips dangerous attributes", () => {
  it("removes on* event handlers from any element", () => {
    const result = globalThis.PercPageEditSanitizer.sanitize(
      '<a href="#" onclick="alert(1)" onmouseover="steal()">click</a>'
    );
    const a = result.querySelector("a");
    expect(a.hasAttribute("onclick")).toBe(false);
    expect(a.hasAttribute("onmouseover")).toBe(false);
    expect(a.getAttribute("href")).toBe("#");
  });

  it.each([
    "javascript:alert(1)",
    "JavaScript:alert(1)",
    "  javascript:alert(1)",
    "data:text/html,<script>alert(1)</script>",
    "vbscript:msgbox(1)",
    "VBScript:msgbox(1)",
  ])("removes javascript:/data:/vbscript: URLs", (url) => {
    const result = globalThis.PercPageEditSanitizer.sanitize(
      `<a href="${url}">x</a>`
    );
    const a = result.querySelector("a");
    expect(a.hasAttribute("href")).toBe(false);
    expect(a.textContent).toBe("x");
  });

  it("preserves safe http(s) and relative URLs", () => {
    const result = globalThis.PercPageEditSanitizer.sanitize(
      '<a href="https://example.com/foo">x</a><img src="/img.png" alt="y" />'
    );
    expect(result.querySelector("a").getAttribute("href")).toBe(
      "https://example.com/foo"
    );
    expect(result.querySelector("img").getAttribute("src")).toBe("/img.png");
  });

  it.each(["action", "formaction"])(
    "removes %s attributes that use a dangerous scheme",
    (attr) => {
      const result = globalThis.PercPageEditSanitizer.sanitize(
        `<button ${attr}="javascript:alert(1)">x</button>`
      );
      const btn = result.querySelector("button");
      expect(btn.hasAttribute(attr)).toBe(false);
    }
  );

  it("does not flag URL schemes that merely *contain* the substring", () => {
    // Legitimate URLs that mention the substrings in path/query should not be
    // stripped (only the scheme is checked, via .indexOf(...) === 0).
    const result = globalThis.PercPageEditSanitizer.sanitize(
      '<a href="https://example.com/?q=javascript:guide">x</a>'
    );
    expect(result.querySelector("a").getAttribute("href")).toBe(
      "https://example.com/?q=javascript:guide"
    );
  });
});

// ---------------------------------------------------------------------------
// Integration: appendChild sink
// ---------------------------------------------------------------------------

describe("Sanitized nodes can be safely appended via Node.appendChild", () => {
  it("appended nodes appear in the host document without innerHTML / .html()", () => {
    const host = document.createElement("div");
    const sanitized = globalThis.PercPageEditSanitizer.sanitize(
      '<p>safe<a href="https://example.com">link</a></p>'
    );
    let next = sanitized.firstChild;
    while (next) {
      const imported = document.importNode(next, true);
      sanitized.removeChild(next);
      host.appendChild(imported);
      next = sanitized.firstChild;
    }
    expect(host.querySelector("p")).not.toBeNull();
    expect(host.querySelector("a").getAttribute("href")).toBe(
      "https://example.com"
    );
  });

  it("does not inject script elements after sanitization + import", () => {
    const host = document.createElement("div");
    const sanitized = globalThis.PercPageEditSanitizer.sanitize(
      "<p>hi</p><script>window.__pwned=true;</script>"
    );
    let next = sanitized.firstChild;
    while (next) {
      const imported = document.importNode(next, true);
      sanitized.removeChild(next);
      host.appendChild(imported);
      next = sanitized.firstChild;
    }
    expect(host.querySelector("script")).toBeNull();
    expect(globalThis.__pwned).toBeUndefined();
    expect(host.textContent).toBe("hi");
  });

  it("does not execute inline event handlers when appended", () => {
    const host = document.createElement("div");
    const sanitized = globalThis.PercPageEditSanitizer.sanitize(
      '<button onclick="window.__clicked=true">x</button>'
    );
    let next = sanitized.firstChild;
    while (next) {
      const imported = document.importNode(next, true);
      sanitized.removeChild(next);
      host.appendChild(imported);
      next = sanitized.firstChild;
    }
    expect(host.querySelector("button").hasAttribute("onclick")).toBe(false);
  });
});
