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
 * Regression tests for remaining js/incomplete-sanitization CodeQL alerts.
 *
 * Closes GitHub CodeQL alerts:
 *   #1485  perc_site_map.js           — non-global "{" strip
 *   #1470  PercUserService.js         — non-global "%" → "*"
 *   #1468  perc_css_utils.js          — non-global '"' strip
 *   #1467  perc_common_ui.js          — non-global "\\" strip (moment unescapeFormat)
 *   #1456  legacy PercUserService.js  — lockstep of #1470
 *   #1454  legacy perc_css_utils.js   — lockstep of #1468
 *   #1135/#1134  siteimprove pages    — incomplete regex escape + non-global "\\"
 *   #1116/#1115  siteimprove app      — lockstep of #1135/#1134
 *
 * (getDashboardColumn #1110-#1113 residuals covered by percGetDashboardColumn.test.js)
 *
 * Pattern: CodeQL flags `.replace(string, ...)` / non-global regex replaces
 * that only touch the first occurrence of a metacharacter used for
 * sanitisation. Fix is always a global regex (`/g` flag) or a full
 * allow-list / full RegExp-escape.
 */

import { readFileSync } from "fs";
import { resolve, dirname } from "path";
import { fileURLToPath } from "url";
import { describe, it, expect } from "vitest";

const __dirname = dirname(fileURLToPath(import.meta.url));
const webapp = (...parts) =>
  resolve(__dirname, "../../main/webapp", ...parts);

function read(rel) {
  return readFileSync(webapp(rel), "utf8");
}

// ---------------------------------------------------------------------------
// PercUserService — directory-user wildcard: % → * for every occurrence
// ---------------------------------------------------------------------------
describe("PercUserService findDirectoryUsers wildcard (alerts #1470/#1456)", () => {
  const copies = [
    "cm/services/PercUserService.js",
    "cm/app/js/legacy/services/PercUserService.js",
  ];

  for (const rel of copies) {
    describe(rel, () => {
      const src = read(rel);

      it("uses a global regex for % → * conversion", () => {
        expect(src).toMatch(
          /usernameStartsWith\s*=\s*usernameStartsWith\.replace\s*\(\s*\/%\/g\s*,\s*["']\*["']\s*\)/
        );
      });

      it("does not use a first-only string replace for %", () => {
        expect(src).not.toMatch(
          /usernameStartsWith\.replace\s*\(\s*["']%["']\s*,/
        );
      });
    });
  }

  it("behaviourally converts every % to *", () => {
    // Mirror of the production expression.
    const convert = (s) => s.replace(/%/g, "*");
    expect(convert("a%b%c")).toBe("a*b*c");
    expect(convert("%admin%")).toBe("*admin*");
    expect(convert("plain")).toBe("plain");
  });

  it("pre-fix first-only replace leaves trailing % characters", () => {
    // Documents why the /g flag is load-bearing.
    const preFix = (s) => s.replace("%", "*");
    expect(preFix("a%b%c")).toBe("a*b%c");
    expect(preFix("a%b%c")).not.toBe("a*b*c");
  });
});

// ---------------------------------------------------------------------------
// perc_css_utils — strip every double-quote before domain CSS parse
// ---------------------------------------------------------------------------
describe("perc_css_utils parse_region_css quote strip (alerts #1468/#1454)", () => {
  const copies = [
    "cm/plugins/perc_css_utils.js",
    "cm/app/js/legacy/plugins/perc_css_utils.js",
  ];

  for (const rel of copies) {
    describe(rel, () => {
      const src = read(rel);

      it("uses a global regex to strip double-quotes", () => {
        expect(src).toMatch(
          /cssString\s*=\s*cssString\.replace\s*\(\s*\/"\/g\s*,\s*["']{2}\s*\)/
        );
      });

      it("does not use a first-only string replace for quotes", () => {
        // Pre-fix: cssString.replace('"', "") or cssString.replace('"', "")
        expect(src).not.toMatch(
          /cssString\.replace\s*\(\s*['"]\"['"]\s*,\s*["']{2}\s*\)/
        );
      });
    });
  }

  it("behaviourally strips every double-quote", () => {
    const strip = (s) => s.replace(/"/g, "");
    expect(strip('#r1{color:"red";font:"Arial"}')).toBe(
      "#r1{color:red;font:Arial}"
    );
    expect(strip('a"b"c"d')).toBe("abcd");
  });

  it("pre-fix first-only replace leaves remaining quotes", () => {
    const preFix = (s) => s.replace('"', "");
    expect(preFix('a"b"c')).toBe('ab"c');
    expect(preFix('a"b"c')).not.toBe("abc");
  });
});

// ---------------------------------------------------------------------------
// perc_site_map getJsonObj — strip every "{" when normalising unquoted JSON
// ---------------------------------------------------------------------------
describe("perc_site_map getJsonObj brace strip (alert #1485)", () => {
  const src = read("cm/widgets/perc_site_map.js");

  it("uses a global regex to strip opening braces", () => {
    // tempArrayData_arr = tempArrayData_arr[0].replace(/\{/g, "");
    expect(src).toMatch(
      /tempArrayData_arr\[0\]\.replace\s*\(\s*\/\\\{\/g\s*,\s*["']{2}\s*\)/
    );
  });

  it("does not use a first-only string replace for {", () => {
    expect(src).not.toMatch(
      /tempArrayData_arr\[0\]\.replace\s*\(\s*["']\{["']\s*,/
    );
  });

  it("behaviourally strips every opening brace", () => {
    const strip = (s) => s.replace(/\{/g, "");
    expect(strip("{{foo:1},{bar:2}")).toBe("foo:1},bar:2}");
    expect(strip("{a:{b:1}}")).toBe("a:b:1}}");
  });

  it("pre-fix first-only replace leaves remaining braces", () => {
    const preFix = (s) => s.replace("{", "");
    expect(preFix("{{a:1}")).toBe("{a:1}");
    expect(preFix("{{a:1}")).not.toBe("a:1}");
  });
});

// ---------------------------------------------------------------------------
// siteimprove_integration — backslash strip + full RegExp escape
// ---------------------------------------------------------------------------
describe("siteimprove_integration.html (alerts #1134/#1135/#1115/#1116)", () => {
  const copies = [
    "cm/app/includes/siteimprove_integration.html",
    "cm/pages/app/includes/siteimprove_integration.html",
  ];

  for (const rel of copies) {
    describe(rel, () => {
      const src = read(rel);

      it("strips every backslash from metadata with a global regex", () => {
        // dataStr = dataStr.replace(/\\/g, "");
        expect(src).toMatch(
          /dataStr\s*=\s*dataStr\.replace\s*\(\s*\/\\\\\/g\s*,\s*["']{2}\s*\)/
        );
      });

      it("does not use a first-only string replace for backslash", () => {
        // Pre-fix: dataStr.replace("\\", "")
        expect(src).not.toMatch(
          /dataStr\.replace\s*\(\s*["']\\\\["']\s*,\s*["']{2}\s*\)/
        );
      });

      it("uses a full RegExp-escape for query-param names (incl. backslash)", () => {
        // name = String(name).replace(/[.*+?^$()|[\]\\{}]/g, "\\$&");
        // Must not contain empty EL (dollar immediately followed by open-brace);
        // Jasper rejects that when this HTML is static-included into a JSP.
        expect(src).toMatch(/String\s*\(\s*name\s*\)\.replace\s*\(/);
        expect(src).not.toMatch(/\$\{}/);
        // Metacharacters (incl. backslash) remain; $ and braces as separate class members.
        expect(src).toMatch(/\[\.\*\+\?\^\$\(\)\|\[\\\]\\\\\{\}\]/);
        expect(src).toContain("$&");
      });

      it("does not use the incomplete [ / ] only escape", () => {
        // Pre-fix: name.replace(/[\[\]]/g, "\\$&") — no backslash escape
        expect(src).not.toMatch(
          /name\s*=\s*name\.replace\s*\(\s*\/\[\\\[\\\]\]\/g/
        );
      });
    });
  }

  it("behaviourally strips every backslash from metadata", () => {
    const strip = (s) => s.replace(/\\/g, "");
    expect(strip('{\\"a\\":1}')).toBe('{"a":1}');
    expect(strip("a\\b\\c")).toBe("abc");
  });

  it("pre-fix first-only backslash strip leaves remaining escapes", () => {
    const preFix = (s) => s.replace("\\", "");
    expect(preFix("a\\b\\c")).toBe("ab\\c");
    expect(preFix("a\\b\\c")).not.toBe("abc");
  });

  it("behaviourally escapes all RegExp metacharacters including backslash", () => {
    // Same class as siteimprove_integration.html (JSP-safe: no "${}" sequence)
    const escape = (s) =>
      String(s).replace(/[.*+?^$()|[\]\\{}]/g, "\\$&");
    expect(escape("id")).toBe("id");
    expect(escape("a.b")).toBe("a\\.b");
    expect(escape("x[0]")).toBe("x\\[0\\]");
    expect(escape("a\\b")).toBe("a\\\\b");
    expect(escape("a{1}")).toBe("a\\{1\\}");
    expect(escape("$x")).toBe("\\$x");
    // Incomplete pre-fix only handled [ and ]:
    const preFix = (s) => s.replace(/[\[\]]/g, "\\$&");
    expect(preFix("a.b")).toBe("a.b"); // still special in RegExp
    expect(preFix("a\\b")).toBe("a\\b"); // backslash not escaped
  });
});

// ---------------------------------------------------------------------------
// perc_common_ui.js — moment.js unescapeFormat non-global backslash strip
// ---------------------------------------------------------------------------
describe("perc_common_ui.js moment unescapeFormat (alert #1467)", () => {
  const src = read("cm/perc_common_ui.js");

  it("uses a global regex to strip backslashes inside unescapeFormat", () => {
    // .replace(/\\/g, "") in the unescapeFormat body
    const fnStart = src.indexOf("function unescapeFormat");
    expect(fnStart).toBeGreaterThan(-1);
    const fnBody = src.slice(fnStart, fnStart + 400);
    expect(fnBody).toMatch(/\.replace\s*\(\s*\/\\\\\/g\s*,\s*["']{2}\s*\)/);
  });

  it("does not use a first-only string replace for backslash in unescapeFormat", () => {
    const fnStart = src.indexOf("function unescapeFormat");
    const fnBody = src.slice(fnStart, fnStart + 400);
    expect(fnBody).not.toMatch(
      /\.replace\s*\(\s*["']\\\\["']\s*,\s*["']{2}\s*\)/
    );
  });

  it("behaviourally strips every backslash", () => {
    const strip = (s) => s.replace(/\\/g, "");
    expect(strip("\\[\\]\\\\")).toBe("[]");
    expect(strip("a\\b\\c")).toBe("abc");
  });
});
