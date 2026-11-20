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
 * 1. The bundled dependency stays on the CodeMirror 5 line per the issue
 *    #5701 end-of-life decision (no CodeMirror 6 migration — TipTap ships
 *    its own source view, so CM5 stays the canonical classic bundle until
 *    classic editing itself retires).
 * 2. `unwrapCodeMirror` recovers the constructor through every level of
 *    esbuild CJS/ESM interop nesting the entry can observe, so
 *    `window.CodeMirror` keeps working for the legacy consumers.
 * 3. The entry wires the dialog + search + searchcursor addons the template
 *    editor source-view find/replace needs (issue #6465), the lint addon plus
 *    the dependency-free tag-balance validator the pre-save validation
 *    markers need (issue #6466), and the marker list the build sanity-check
 *    enforces names the dialog and lint addons.
 * 4. The entry wires the persisted display preferences the source-view
 *    toolbar needs (issue #6467): the dark theme CSS plus the
 *    `percCodeEditorPrefs` read/write/apply/step/toggle namespace, with
 *    storage-failure-tolerant helpers that degrade to defaults.
 * 5. The shipped bundle pair carries the upstream MIT notice (issue #5626
 *    license hygiene): esbuild minify strips the `//` notice from the npm
 *    sources, so the build stamps it back on via `banner`, and the stamp
 *    is pinned here plus by the build sanity-check and the live QA surface.
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
  CODEMIRROR_LICENSE_NOTICE,
  CODEMIRROR_LICENSE_URL,
  CODEMIRROR_LICENSE_MARKER,
  buildCodeMirrorBundleBanners,
  PERC_CODEEDITOR_PREFS_KEY,
  PERC_CODEEDITOR_THEMES,
  PERC_CODEEDITOR_FONT_SIZE_DEFAULT,
  PERC_CODEEDITOR_FONT_SIZE_MIN,
  PERC_CODEEDITOR_FONT_SIZE_MAX,
  normalizePercCodeEditorTheme,
  clampPercCodeEditorFontSize,
  defaultPercCodeEditorPrefs,
  readPercCodeEditorPrefs,
  writePercCodeEditorPrefs,
  nextPercCodeEditorFontSize,
  togglePercCodeEditorTheme,
  applyPercCodeEditorPrefs,
} from "../../../build-scripts/codemirror-helpers.mjs";

const require = createRequire(import.meta.url);

describe("codemirror npm dependency line", () => {
  it("stays on the pinned CodeMirror major per the #5701 EOL decision (no CM6)", () => {
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
      "percCodeEditorPrefs",
      "material-darker",
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

  it("wires the persisted display preferences into the bundle entry", () => {
    // Issue #6467: the template editor source-view toolbar (dark-mode
    // toggle + font-size steppers in sys_Templates.xsl) reads
    // `CodeMirror.percCodeEditorPrefs`. The bundle must ship the dark theme
    // CSS and expose the prefs namespace, or the toolbar degrades to a dead
    // control at runtime. A dropped import fails here first.
    const entryPath = join(
      process.cwd(),
      "build-scripts",
      "codemirror-bundle-entry.js",
    );
    const entry = readFileSync(entryPath, "utf8");
    expect(
      entry.includes("codemirror/theme/material-darker.css"),
      "bundle entry must import the dark theme CSS (#6467)",
    ).toBe(true);
    expect(
      entry.includes("percCodeEditorPrefs"),
      "bundle entry must expose the prefs namespace on the CodeMirror global (#6467)",
    ).toBe(true);
    for (const helper of [
      "readPercCodeEditorPrefs(",
      "writePercCodeEditorPrefs(",
      "applyPercCodeEditorPrefs(",
      "togglePercCodeEditorTheme(",
    ]) {
      expect(
        entry.includes(helper),
        `bundle entry must delegate to ${helper} (#6467)`,
      ).toBe(true);
    }
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

describe("percCodeEditorPrefs storage key and themes", () => {
  it("uses one stable storage key for every code editor", () => {
    // The preference is the author's, not the field's: every sys_TextArea
    // code editor reads the same key.
    expect(PERC_CODEEDITOR_PREFS_KEY).toBe("perc.codeeditor.prefs");
  });

  it("ships exactly the light and dark themes", () => {
    expect(PERC_CODEEDITOR_THEMES).toEqual({
      light: "default",
      dark: "material-darker",
    });
  });

  it("keeps the font-size bounds sane", () => {
    expect(PERC_CODEEDITOR_FONT_SIZE_MIN).toBeLessThan(
      PERC_CODEEDITOR_FONT_SIZE_DEFAULT,
    );
    expect(PERC_CODEEDITOR_FONT_SIZE_DEFAULT).toBeLessThan(
      PERC_CODEEDITOR_FONT_SIZE_MAX,
    );
  });
});

describe("normalizePercCodeEditorTheme", () => {
  it("keeps the dark theme name", () => {
    expect(normalizePercCodeEditorTheme("material-darker")).toBe(
      "material-darker",
    );
  });

  it("falls back to light for anything else", () => {
    expect(normalizePercCodeEditorTheme("default")).toBe("default");
    for (const bad of ["monokai", "", null, undefined, 42, {}, []]) {
      expect(normalizePercCodeEditorTheme(bad)).toBe("default");
    }
  });
});

describe("clampPercCodeEditorFontSize", () => {
  it("keeps in-range sizes, rounding halves of a pixel", () => {
    expect(clampPercCodeEditorFontSize(13)).toBe(13);
    expect(clampPercCodeEditorFontSize("16")).toBe(16);
    expect(clampPercCodeEditorFontSize(12.4)).toBe(12);
  });

  it("clamps out-of-range sizes to the bounds", () => {
    expect(clampPercCodeEditorFontSize(1)).toBe(
      PERC_CODEEDITOR_FONT_SIZE_MIN,
    );
    expect(clampPercCodeEditorFontSize(100)).toBe(
      PERC_CODEEDITOR_FONT_SIZE_MAX,
    );
  });

  it("falls back to the default for garbage", () => {
    for (const bad of ["", "big", null, undefined, NaN, {}, []]) {
      expect(clampPercCodeEditorFontSize(bad)).toBe(
        PERC_CODEEDITOR_FONT_SIZE_DEFAULT,
      );
    }
  });
});

describe("defaultPercCodeEditorPrefs", () => {
  it("is light theme at the default size", () => {
    expect(defaultPercCodeEditorPrefs()).toEqual({
      theme: "default",
      fontSize: PERC_CODEEDITOR_FONT_SIZE_DEFAULT,
    });
  });

  it("returns a fresh object per call", () => {
    expect(defaultPercCodeEditorPrefs()).not.toBe(
      defaultPercCodeEditorPrefs(),
    );
  });
});

function memoryStorage(initial) {
  const store = { ...(initial || {}) };
  return {
    getItem: (key) =>
      Object.prototype.hasOwnProperty.call(store, key) ? store[key] : null,
    setItem: (key, value) => {
      store[key] = String(value);
    },
  };
}

describe("readPercCodeEditorPrefs", () => {
  it("reads a stored theme and size", () => {
    const storage = memoryStorage({
      [PERC_CODEEDITOR_PREFS_KEY]: JSON.stringify({
        theme: "material-darker",
        fontSize: 18,
      }),
    });
    expect(readPercCodeEditorPrefs(storage)).toEqual({
      theme: "material-darker",
      fontSize: 18,
    });
  });

  it("falls back to defaults when storage is missing or empty", () => {
    const defaults = defaultPercCodeEditorPrefs();
    expect(readPercCodeEditorPrefs(null)).toEqual(defaults);
    expect(readPercCodeEditorPrefs(undefined)).toEqual(defaults);
    expect(readPercCodeEditorPrefs({})).toEqual(defaults);
    expect(
      readPercCodeEditorPrefs(memoryStorage({})),
    ).toEqual(defaults);
    expect(
      readPercCodeEditorPrefs(
        memoryStorage({ [PERC_CODEEDITOR_PREFS_KEY]: "" }),
      ),
    ).toEqual(defaults);
  });

  it("falls back to defaults when storage throws or holds garbage", () => {
    const defaults = defaultPercCodeEditorPrefs();
    const throwing = {
      getItem: () => {
        throw new Error("denied");
      },
    };
    expect(readPercCodeEditorPrefs(throwing)).toEqual(defaults);
    for (const raw of ["{nope", "[1,2", "42", '"str"', "null"]) {
      expect(
        readPercCodeEditorPrefs(
          memoryStorage({ [PERC_CODEEDITOR_PREFS_KEY]: raw }),
        ),
      ).toEqual(defaults);
    }
  });

  it("normalizes each field independently", () => {
    // One corrupt field must not discard the other.
    expect(
      readPercCodeEditorPrefs(
        memoryStorage({
          [PERC_CODEEDITOR_PREFS_KEY]: JSON.stringify({
            theme: "material-darker",
            fontSize: "huge",
          }),
        }),
      ),
    ).toEqual({
      theme: "material-darker",
      fontSize: PERC_CODEEDITOR_FONT_SIZE_DEFAULT,
    });
    expect(
      readPercCodeEditorPrefs(
        memoryStorage({
          [PERC_CODEEDITOR_PREFS_KEY]: JSON.stringify({
            theme: "monokai",
            fontSize: 20,
          }),
        }),
      ),
    ).toEqual({ theme: "default", fontSize: 20 });
  });
});

describe("writePercCodeEditorPrefs", () => {
  it("round-trips through storage", () => {
    const storage = memoryStorage();
    expect(
      writePercCodeEditorPrefs(storage, {
        theme: "material-darker",
        fontSize: 18,
      }),
    ).toBe(true);
    expect(readPercCodeEditorPrefs(storage)).toEqual({
      theme: "material-darker",
      fontSize: 18,
    });
  });

  it("normalizes before writing so only valid values persist", () => {
    const storage = memoryStorage();
    expect(
      writePercCodeEditorPrefs(storage, { theme: "monokai", fontSize: 99 }),
    ).toBe(true);
    expect(readPercCodeEditorPrefs(storage)).toEqual({
      theme: "default",
      fontSize: PERC_CODEEDITOR_FONT_SIZE_MAX,
    });
  });

  it("reports failure instead of throwing", () => {
    expect(writePercCodeEditorPrefs(null, { theme: "default" })).toBe(false);
    expect(writePercCodeEditorPrefs({}, { theme: "default" })).toBe(false);
    const throwing = {
      setItem: () => {
        throw new Error("quota exceeded");
      },
    };
    expect(
      writePercCodeEditorPrefs(throwing, { theme: "default", fontSize: 13 }),
    ).toBe(false);
  });
});

describe("nextPercCodeEditorFontSize", () => {
  it("steps by one pixel within the bounds", () => {
    expect(nextPercCodeEditorFontSize(13, 1)).toBe(14);
    expect(nextPercCodeEditorFontSize(13, -1)).toBe(12);
  });

  it("clamps at the bounds", () => {
    expect(
      nextPercCodeEditorFontSize(PERC_CODEEDITOR_FONT_SIZE_MIN, -1),
    ).toBe(PERC_CODEEDITOR_FONT_SIZE_MIN);
    expect(
      nextPercCodeEditorFontSize(PERC_CODEEDITOR_FONT_SIZE_MAX, 1),
    ).toBe(PERC_CODEEDITOR_FONT_SIZE_MAX);
  });

  it("treats garbage current and zero delta as the base size", () => {
    expect(nextPercCodeEditorFontSize("huge", 1)).toBe(
      PERC_CODEEDITOR_FONT_SIZE_DEFAULT + 1,
    );
    expect(nextPercCodeEditorFontSize(13, 0)).toBe(13);
    expect(nextPercCodeEditorFontSize(13, NaN)).toBe(13);
  });
});

describe("togglePercCodeEditorTheme", () => {
  it("flips light to dark and back, keeping the size", () => {
    expect(
      togglePercCodeEditorTheme({ theme: "default", fontSize: 18 }),
    ).toEqual({ theme: "material-darker", fontSize: 18 });
    expect(
      togglePercCodeEditorTheme({ theme: "material-darker", fontSize: 18 }),
    ).toEqual({ theme: "default", fontSize: 18 });
  });

  it("treats garbage as light and still flips to dark", () => {
    expect(togglePercCodeEditorTheme(null)).toEqual({
      theme: "material-darker",
      fontSize: PERC_CODEEDITOR_FONT_SIZE_DEFAULT,
    });
    expect(
      togglePercCodeEditorTheme({ theme: "monokai", fontSize: 99 }),
    ).toEqual({
      theme: "material-darker",
      fontSize: PERC_CODEEDITOR_FONT_SIZE_MAX,
    });
  });
});

function fakeEditor() {
  const options = {};
  return {
    options,
    wrapperStyle: {},
    refreshed: 0,
    setOption: (name, value) => {
      options[name] = value;
    },
    getWrapperElement: function () {
      return { style: this.wrapperStyle };
    },
    refresh: function () {
      this.refreshed += 1;
    },
  };
}

describe("applyPercCodeEditorPrefs", () => {
  it("sets the theme option, wrapper size, and refreshes", () => {
    const editor = fakeEditor();
    expect(
      applyPercCodeEditorPrefs(editor, {
        theme: "material-darker",
        fontSize: 18,
      }),
    ).toBe(true);
    expect(editor.options.theme).toBe("material-darker");
    expect(editor.wrapperStyle.fontSize).toBe("18px");
    expect(editor.refreshed).toBe(1);
  });

  it("normalizes garbage prefs to light/default", () => {
    const editor = fakeEditor();
    expect(applyPercCodeEditorPrefs(editor, null)).toBe(true);
    expect(editor.options.theme).toBe("default");
    expect(editor.wrapperStyle.fontSize).toBe(
      `${PERC_CODEEDITOR_FONT_SIZE_DEFAULT}px`,
    );
  });

  it("reports failure for a missing or throwing editor", () => {
    expect(applyPercCodeEditorPrefs(null, {})).toBe(false);
    expect(applyPercCodeEditorPrefs({}, {})).toBe(false);
    expect(
      applyPercCodeEditorPrefs(
        {
          setOption: () => {
            throw new Error("dead");
          },
        },
        {},
      ),
    ).toBe(false);
  });

  it("applies the theme even without a wrapper or refresh", () => {
    const bare = { options: {}, setOption(name, value) { this.options[name] = value; } };
    expect(
      applyPercCodeEditorPrefs(bare, { theme: "default", fontSize: 13 }),
    ).toBe(true);
    expect(bare.options.theme).toBe("default");
  });
});

describe("codemirror bundle MIT notice stamp (issue #5626)", () => {
  it("repeats the upstream notice wording verbatim", () => {
    // The stamp must stay word-identical to the notice opening every
    // lib/mode/addon source in the npm package — paraphrasing it would
    // weaken the MIT attribution the LICENSE file requires.
    expect(CODEMIRROR_LICENSE_NOTICE).toBe(
      "CodeMirror, copyright (c) by Marijn Haverbeke and others",
    );
    expect(CODEMIRROR_LICENSE_URL).toBe("https://codemirror.net/5/LICENSE");
  });

  it("builds js/css banners that survive esbuild minification", () => {
    // Read the installed release so a Dependabot patch bump that changes
    // the bundle bytes does not fail the stamp — the assertion still proves
    // the banner names the bundled release.
    const installedVersion = require("codemirror/package.json").version;
    const banners = buildCodeMirrorBundleBanners(installedVersion);
    for (const banner of [banners.js, banners.css]) {
      // `/*!` opener: a legal comment by esbuild's definition, so the
      // stamp is emitted verbatim even with minify on.
      expect(banner.startsWith("/*!")).toBe(true);
      expect(banner.endsWith("*/")).toBe(true);
      expect(banner.includes(CODEMIRROR_LICENSE_MARKER)).toBe(true);
      expect(banner.includes(CODEMIRROR_LICENSE_URL)).toBe(true);
      // The stamp names the upstream release it was built from, so a
      // Dependabot bump that changes the bytes also changes the stamp.
      expect(banner.includes(`codemirror@${installedVersion}`)).toBe(true);
    }
  });

  it("wires the stamp into the esbuild build with a pinned legalComments", () => {
    // The build script is the only writer of the shipped bundle pair, so
    // pin its wiring statically: a dropped banner (or a legalComments flip
    // that silently strips notices) fails here before packaging.
    const scriptPath = join(
      process.cwd(),
      "build-scripts",
      "bundle-codemirror.mjs",
    );
    const script = readFileSync(scriptPath, "utf8");
    expect(
      script.includes("buildCodeMirrorBundleBanners"),
      "build must stamp the bundle pair via buildCodeMirrorBundleBanners",
    ).toBe(true);
    expect(
      script.includes('legalComments: "eof"'),
      "build must pin legalComments so notice retention never rides an esbuild default",
    ).toBe(true);
    expect(
      script.includes("CODEMIRROR_LICENSE_MARKER"),
      "build sanity-check must fail the build when the MIT notice is missing",
    ).toBe(true);
  });
});
