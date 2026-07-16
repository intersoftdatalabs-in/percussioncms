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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Tests for {@code $.PercServiceUtils.sanitizeUrlForHref} - URL sanitizer
 * used across the widget views (CodeQL js/xss-through-dom, alerts
 * #980-#993 for {@code modules/perc-common-ui-bundle/}).
 *
 * <p>The sanitizer allow-lists http, https, mailto, tel, and relative/
 * fragment references; any other scheme (javascript:, data:, vbscript:,
 * file:, blob:) is replaced with {@code "about:blank#blocked"} so the
 * browser cannot execute attacker-controlled code via a clicked anchor.
 */

import { readFileSync } from "fs";
import { resolve, dirname } from "path";
import { fileURLToPath } from "url";
import { describe, it, expect, beforeAll } from "vitest";

const __dirname = dirname(fileURLToPath(import.meta.url));
const ROOT = resolve(__dirname, "../../../..");

// Load PercServiceUtils once — it attaches $.PercServiceUtils to the
// global jQuery installed by setup.js.
const utilsCode = readFileSync(
  resolve(ROOT, "src/main/js/services/PercServiceUtils.js"),
  "utf8"
);
beforeAll(() => {
  eval(utilsCode); // eslint-disable-line no-eval
});

describe("$.PercServiceUtils.sanitizeUrlForHref", () => {
  it("is defined on $.PercServiceUtils", () => {
    expect(typeof $.PercServiceUtils.sanitizeUrlForHref).toBe("function");
  });

  // ----- Dangerous schemes: must be neutralized -----
  describe("neutralizes dangerous schemes", () => {
    const dangerous = [
      ["javascript:", "javascript:alert(1)"],
      ["JavaScript: case-insensitive", "JavaScript:alert(1)"],
      ["javascript: with leading whitespace", "   javascript:alert(1)"],
      ["javascript: with embedded tab", "java\tscript:alert(1)"],
      ["javascript: with embedded newline", "java\nscript:alert(1)"],
      ["data: URL", "data:text/html,<script>alert(1)</script>"],
      ["vbscript:", "vbscript:msgbox(1)"],
      ["file:", "file:///etc/passwd"],
      ["blob:", "blob:http://evil.example/payload"],
      ["chrome:", "chrome://settings"],
    ];

    for (const [label, input] of dangerous) {
      it(`${label} -> about:blank#blocked`, () => {
        expect($.PercServiceUtils.sanitizeUrlForHref(input)).toBe(
          "about:blank#blocked"
        );
      });
    }
  });

  // ----- Safe schemes: must pass through unchanged -----
  describe("allows safe schemes", () => {
    const safe = [
      ["http absolute URL", "http://example.com/page"],
      ["https absolute URL", "https://example.com/page?q=1"],
      ["mailto:", "mailto:user@example.com"],
      ["tel:", "tel:+15551234567"],
      ["relative path", "/blog/post-1"],
      ["relative path with query", "/blog/post-1?filter=foo"],
      ["fragment only", "#section-2"],
      ["query only", "?page=2"],
      ["bareword relative", "blog/post-1"],
      ["bareword with html ext", "page.html"],
      ["about:blank self", "about:blank"],
    ];

    for (const [label, input] of safe) {
      it(`${label} -> unchanged`, () => {
        expect($.PercServiceUtils.sanitizeUrlForHref(input)).toBe(input);
      });
    }
  });

  // ----- Edge cases -----
  describe("handles edge cases", () => {
    it("returns about:blank#blocked for null", () => {
      expect($.PercServiceUtils.sanitizeUrlForHref(null)).toBe(
        "about:blank#blocked"
      );
    });

    it("returns about:blank#blocked for undefined", () => {
      expect($.PercServiceUtils.sanitizeUrlForHref(undefined)).toBe(
        "about:blank#blocked"
      );
    });

    it("returns about:blank#blocked for empty string", () => {
      expect($.PercServiceUtils.sanitizeUrlForHref("")).toBe(
        "about:blank#blocked"
      );
    });

    it("returns about:blank#blocked for non-string input", () => {
      expect($.PercServiceUtils.sanitizeUrlForHref(42)).toBe(
        "about:blank#blocked"
      );
      expect($.PercServiceUtils.sanitizeUrlForHref({})).toBe(
        "about:blank#blocked"
      );
      expect($.PercServiceUtils.sanitizeUrlForHref([])).toBe(
        "about:blank#blocked"
      );
    });

    it("preserves legitimate URL fragments in href", () => {
      expect(
        $.PercServiceUtils.sanitizeUrlForHref("https://example.com/page#id")
      ).toBe("https://example.com/page#id");
    });

    it("strips leading whitespace before scheme sniffing", () => {
      // The actual href value retains leading whitespace (browsers tolerate
      // it), but the scheme detection must not be tricked by it.
      expect($.PercServiceUtils.sanitizeUrlForHref("  https://example.com")).toBe(
        "  https://example.com"
      );
      expect(
        $.PercServiceUtils.sanitizeUrlForHref("  javascript:alert(1)")
      ).toBe("about:blank#blocked");
    });
  });
});