/**
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
 * Tests for the CodeMirror 5 npm bundle contract (issue #5784).
 *
 * The classic template editor (`editTemplate_js.jsp`) and the `sys_TextArea`
 * code-editor control (`sys_Templates.xsl`) load
 * `/sys_resources/codemirror/codemirror/codemirror.bundle.js` built by
 * `build-scripts/bundle-codemirror.mjs` from the `codemirror` npm package.
 * These tests pin the three halves of that contract that a plain build would
 * only catch at runtime on a live CMS:
 *
 * 1. The bundled dependency stays on the CodeMirror 5 line until issue #5701
 *    lands the CodeMirror 6 replacement (a CM6 install changes the entry
 *    imports and the global-constructor contract).
 * 2. `unwrapCodeMirror` recovers the constructor through every level of
 *    esbuild CJS/ESM interop nesting the entry can observe, so
 *    `window.CodeMirror` keeps working for the legacy consumers.
 * 3. The entry wires the dialog + search + searchcursor addons the template
 *    editor source-view find/replace needs (issue #6465), the lint addon plus
 *    the dependency-free tag-balance validator the pre-save validation
 *    markers need (issue #6466), and the marker list the build sanity-check
 *    enforces names the dialog and lint addons.
 */

import { createRequire } from "module";
import { readFileSync } from "node:fs";
import { join } from "node:path";
import { describe, it, expect } from "vitest";
import {
  unwrapCodeMirror,
  findPercTemplateHtmlIssues,
  registerPercTemplateHtmlLint,
  PERC_TEMPLATE_HTML_LINT_MODES,
  CODEMIRROR_BUNDLE_MARKERS,
  CODEMIRROR_PINNED_MAJOR,
} from "../../../build-scripts/codemirror-helpers.mjs";

const require = createRequire(import.meta.url);

describe("codemirror npm dependency line", () => {
  it("stays on the pinned CodeMirror major until #5701 lands CM6", () => {
    const pkg = require("codemirror/package.json");
    const major = Number(String(pkg.version).split(".")[0]);
    expect(major).toBe(CODEMIRROR_PINNED_MAJOR);
  });

  it("publishes the bundle markers the build sanity-check requires", () => {
    expect(CODEMIRROR_BUNDLE_MARKERS).toEqual([
      "htmlmixed",
      "searchcursor",
      "matchBrackets",
      "openDialog",
      "CodeMirror-lint-markers",
      "percValidateTemplateHtml",
    ]);
  });

  it("wires the find/replace addons into the bundle entry", () => {
    // Issue #6465: the template editor source-view find/replace (Ctrl-F /
    // replace dialog) needs the dialog addon (renders the prompt) plus the
    // search + searchcursor addons (define the commands). The bundle build
    // only checks minified markers, so pin the entry wiring itself: a
    // dropped import fails here before the browser ever loads the bundle.
    // Vitest (npm or the Maven frontend plugin) always runs with the module
    // directory as cwd; `join` keeps the resolution portable (Windows/Unix).
    const entryPath = join(
      process.cwd(),
      "build-scripts",
      "codemirror-bundle-entry.js",
    );
    const entry = readFileSync(entryPath, "utf8");
    for (const addon of [
      "codemirror/addon/dialog/dialog.js",
      "codemirror/addon/search/search.js",
      "codemirror/addon/search/searchcursor.js",
    ]) {
      expect(
        entry.includes(addon),
        `bundle entry must import ${addon} for find/replace`,
      ).toBe(true);
    }
  });

  it("wires the validation lint addon into the bundle entry", () => {
    // Issue #6466: the template editor source-view flags unbalanced markup
    // with gutter markers before save. The bundle must ship the CodeMirror 5
    // lint addon (gutter + styles) and register the dependency-free
    // tag-balance validator as the htmlmixed lint helper, exposed on the
    // global for debugging and Playwright assertions. A dropped import fails
    // here before the browser ever loads the bundle.
    const entryPath = join(
      process.cwd(),
      "build-scripts",
      "codemirror-bundle-entry.js",
    );
    const entry = readFileSync(entryPath, "utf8");
    for (const addon of [
      "codemirror/addon/lint/lint.js",
      "codemirror/addon/lint/lint.css",
    ]) {
      expect(
        entry.includes(addon),
        `bundle entry must import ${addon} for validation markers`,
      ).toBe(true);
    }
    expect(
      entry.includes("registerPercTemplateHtmlLint("),
      "bundle entry must register the template HTML lint helper (#6466)",
    ).toBe(true);
    expect(
      entry.includes("percValidateTemplateHtml"),
      "bundle entry must expose the validator on the CodeMirror global (#6466)",
    ).toBe(true);
  });
});

describe("unwrapCodeMirror", () => {
  it("returns a bare constructor unchanged", () => {
    function CodeMirror() {}
    expect(unwrapCodeMirror(CodeMirror)).toBe(CodeMirror);
  });

  it("unwraps one level of esbuild default interop", () => {
    function CodeMirror() {}
    expect(unwrapCodeMirror({ default: CodeMirror })).toBe(CodeMirror);
  });

  it("unwraps nested default objects down to the constructor", () => {
    function CodeMirror() {}
    const nested = { default: { default: CodeMirror } };
    expect(unwrapCodeMirror(nested)).toBe(CodeMirror);
  });

  it("prefers the default export over the namespace record", () => {
    function CodeMirror() {}
    const record = { default: CodeMirror, extra: 1 };
    expect(unwrapCodeMirror(record)).toBe(CodeMirror);
  });

  it("passes through nullish records without throwing", () => {
    expect(unwrapCodeMirror(null)).toBeNull();
    expect(unwrapCodeMirror(undefined)).toBeUndefined();
  });
});

describe("findPercTemplateHtmlIssues", () => {
  it("reports nothing for balanced markup fragments", () => {
    expect(
      findPercTemplateHtmlIssues(
        '<meta charset="utf-8">\n<link rel="stylesheet" href="/a.css">\n<div class="head"><script src="/b.js"></script></div>',
      ),
    ).toEqual([]);
  });

  it("reports nothing for empty and plain-text content", () => {
    expect(findPercTemplateHtmlIssues("")).toEqual([]);
    expect(findPercTemplateHtmlIssues("Just words, no tags.")).toEqual([]);
    expect(
      findPercTemplateHtmlIssues("#if($a > $b) $x.key #end"),
    ).toEqual([]);
  });

  it("flags an unclosed tag at end of input as a warning with its position", () => {
    const found = findPercTemplateHtmlIssues("<div>\n<p>oops");
    expect(found).toHaveLength(2);
    expect(found[0]).toMatchObject({
      from: { line: 0, ch: 0 },
      severity: "warning",
    });
    expect(found[0].message).toContain("<div>");
    expect(found[1]).toMatchObject({
      from: { line: 1, ch: 0 },
      severity: "warning",
    });
    expect(found[1].message).toContain("<p>");
  });

  it("flags a mismatched closing tag as an error", () => {
    const found = findPercTemplateHtmlIssues("<div><span></div>");
    expect(found).toHaveLength(2);
    const [unclosed, mismatch] = found;
    expect(unclosed.severity).toBe("warning");
    expect(unclosed.message).toContain("<span>");
    expect(mismatch.severity).toBe("error");
    expect(mismatch.from).toEqual({ line: 0, ch: 11 });
    expect(mismatch.message).toContain("</div>");
  });

  it("flags a stray closing tag with no open tag as an error", () => {
    const found = findPercTemplateHtmlIssues("<p>hi</p></p>");
    expect(found).toHaveLength(1);
    expect(found[0].severity).toBe("error");
    expect(found[0].message).toContain("Stray closing tag");
    expect(found[0].from).toEqual({ line: 0, ch: 9 });
    expect(found[0].to).toEqual({ line: 0, ch: 13 });
  });

  it("ignores void elements, self-closing tags, comments, and doctypes", () => {
    expect(
      findPercTemplateHtmlIssues(
        '<!DOCTYPE html>\n<!-- <div> not a tag -->\n<br><hr/><img src="a.png"><input type="text">',
      ),
    ).toEqual([]);
  });

  it("ignores script and style element content, but matches their tags", () => {
    expect(
      findPercTemplateHtmlIssues(
        '<script>if (a < b && c > d) { x("</div>"); }</script>',
      ),
    ).toEqual([]);
    const found = findPercTemplateHtmlIssues("<script>var x = 1;</style>");
    expect(found).toHaveLength(1);
    expect(found[0].severity).toBe("warning");
    expect(found[0].message).toContain("Unclosed <script>");
  });

  it("does not end a tag on > inside quoted attributes or parens", () => {
    expect(
      findPercTemplateHtmlIssues('<div title="a>b" data-x=\'c>d\'>ok</div>'),
    ).toEqual([]);
    expect(
      findPercTemplateHtmlIssues("<div #if($a > $b)>ok</div>"),
    ).toEqual([]);
  });

  it("matches tags case-insensitively", () => {
    expect(
      findPercTemplateHtmlIssues("<DIV><P>hi</p></div>"),
    ).toEqual([]);
  });

  it("flags an unterminated comment instead of swallowing the rest", () => {
    const found = findPercTemplateHtmlIssues("<div>ok</div><!-- never ends");
    expect(found).toHaveLength(1);
    expect(found[0].severity).toBe("error");
    expect(found[0].message).toContain("comment");
    expect(found[0].from).toEqual({ line: 0, ch: 13 });
  });

  it("treats CRLF as single line breaks for Windows-edited templates", () => {
    const found = findPercTemplateHtmlIssues("<div>\r\n<p>oops");
    expect(found).toHaveLength(2);
    expect(found[1].from).toEqual({ line: 1, ch: 0 });
  });

  it("sorts annotations by position", () => {
    const found = findPercTemplateHtmlIssues("<u>\n<b>x</b>\n<i>y");
    expect(found).toHaveLength(2);
    expect(found[0].from).toEqual({ line: 0, ch: 0 });
    expect(found[0].message).toContain("<u>");
    expect(found[1].from).toEqual({ line: 2, ch: 0 });
    expect(found[1].message).toContain("<i>");
  });
});

describe("registerPercTemplateHtmlLint", () => {
  function fakeCodeMirror() {
    const registrations = {};
    return {
      registrations,
      Pos: (line, ch) => ({ line, ch }),
      registerHelper: (type, mode, helper) => {
        registrations[`${type}:${mode}`] = helper;
      },
    };
  }

  it("registers the htmlmixed lint helper mapping to the validator", () => {
    const fake = fakeCodeMirror();
    expect(registerPercTemplateHtmlLint(fake)).toBe(true);
    // The lint addon resolves helpers through getModeAt(Pos(0, 0)), which
    // returns the inner xml mode for markup-led content, so the validator
    // must answer under every mode name the source-view can resolve.
    expect(Object.keys(fake.registrations).sort()).toEqual(
      [...PERC_TEMPLATE_HTML_LINT_MODES].map((mode) => `lint:${mode}`).sort(),
    );
    const helper = fake.registrations["lint:xml"];
    expect(typeof helper).toBe("function");
    const annotations = helper("<div><p>x</div>", {}, null);
    expect(annotations).toHaveLength(2);
    expect(annotations[0]).toMatchObject({
      from: { line: 0, ch: 5 },
      severity: "warning",
    });
    expect(annotations[0].message).toContain("<p>");
    expect(annotations[1]).toMatchObject({
      from: { line: 0, ch: 9 },
      severity: "error",
    });
  });

  it("returns false without a helper registry", () => {
    expect(registerPercTemplateHtmlLint(null)).toBe(false);
    expect(registerPercTemplateHtmlLint({})).toBe(false);
    expect(
      registerPercTemplateHtmlLint({ registerHelper: () => {} }),
    ).toBe(false);
  });
});
