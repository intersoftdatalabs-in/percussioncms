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
 * Classic CodeMirror bundle regression (issues #5626, #5784).
 *
 * <p>Tags: {@code @explorer-content-editor} {@code @editor}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/editor-codemirror-bundle.spec.js}</p>
 *
 * <p>Covers the WebJar-to-npm migration (#5626) and the TinyMCE-independent
 * relocation (#5784): the file the classic template/page-edit dialogs and the
 * {@code sys_TextArea} code-editor control load
 * ({@code codemirror.bundle.js} built from the {@code codemirror} npm package
 * by {@code modules/perc-common-ui-bundle}, served from
 * {@code /sys_resources/codemirror/codemirror/}) must be served, must expose
 * the same global CodeMirror 5 API the consumers use
 * ({@code fromTextArea}/{@code getValue}, {@code htmlmixed} mode, search and
 * fullscreen addons), and must edit HTML in a real browser. The old
 * per-file WebJar layout ({@code codemirror/lib/...}) must be gone.</p>
 */

const { test, expect } = require("@playwright/test");
const fs = require("fs");
const path = require("path");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

const PLUGIN_BASE = "/Rhythmyx/sys_resources/codemirror/codemirror";
const BUNDLE_JS = `${PLUGIN_BASE}/codemirror.bundle.js`;
const BUNDLE_CSS = `${PLUGIN_BASE}/codemirror.bundle.css`;
// Old per-file WebJar layout (removed by #5626) under the legacy
// TinyMCE-hosted base (kept serving the bundle until #5734 deletes
// modules/perc-tinymce; #5784 consumers no longer point at it).
const LEGACY_TINYMCE_BASE =
  "/Rhythmyx/sys_resources/tinymce/plugins/codemirror/codemirror";
const LEGACY_LIB = `${LEGACY_TINYMCE_BASE}/lib/codemirror.js`;

// Classic template-editor JSPs that embed the CodeMirror loader. The
// `WebUI/war` tree is a dual-ship mirror of `WebUI/src/main/webapp` for
// these residual dialogs: every copy must load the same TinyMCE-independent
// npm bundle (#5784), never the removed per-file WebJar layout (#5626) and
// never the legacy TinyMCE-hosted bundle path (kept only as a fallback until
// #5734 deletes modules/perc-tinymce).
const REPO_ROOT = path.join(__dirname, "..", "..", "..", "..");
const TEMPLATE_JSP_MIRRORS = [
  "WebUI/src/main/webapp/cm/app/includes/template/editTemplate_js.jsp",
  "WebUI/src/main/webapp/cm/pages/app/includes/template/editTemplate_js.jsp",
  "WebUI/war/app/includes/template/editTemplate_js.jsp",
];

function codemirrorLoaderLines(jspPath) {
  const text = fs
    .readFileSync(path.join(REPO_ROOT, jspPath), "utf8")
    .replace(/\r\n/g, "\n");
  return text.split("\n").filter((line) => /codemirror/i.test(line));
}

test.describe("Classic CodeMirror npm bundle (issues #5626, #5784)", () => {
  test(
    "bundle serves the CodeMirror 5 API and edits HTML",
    { tag: ["@explorer-content-editor", "@editor"] },
    async ({ page }) => {
      test.setTimeout(60_000);
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      page.on("console", (msg) => {
        if (msg.type() === "error") {
          pageErrors.push(msg.text());
        }
      });

      await loginAsAdmin(page);

      // Bundle assets are served (200) from the packaged
      // perc-common-ui-bundle JAR (issue #5784 relocation).
      const jsResponse = await page.request.get(`${BASE_URL}${BUNDLE_JS}`);
      expect(jsResponse.ok(), `bundle JS not served: ${BUNDLE_JS}`).toBe(true);
      const cssResponse = await page.request.get(`${BASE_URL}${BUNDLE_CSS}`);
      expect(cssResponse.ok(), `bundle CSS not served: ${BUNDLE_CSS}`).toBe(
        true,
      );

      // The old per-file WebJar layout is gone.
      const legacyResponse = await page.request.get(
        `${BASE_URL}${LEGACY_LIB}`,
      );
      expect(
        legacyResponse.status(),
        `legacy WebJar file still served: ${LEGACY_LIB}`,
      ).toBe(404);

      // Load the bundle the same way source.html / the classic dialogs do.
      await page.addScriptTag({ url: `${BASE_URL}${BUNDLE_JS}` });
      const api = await page.evaluate(() => ({
        type: typeof window.CodeMirror,
        fromTextArea: typeof window.CodeMirror.fromTextArea,
        defineInitHook: typeof window.CodeMirror.defineInitHook,
        modes: Object.keys(window.CodeMirror.modes || {}).sort(),
        openDialog:
          typeof window.CodeMirror.prototype !== "undefined"
            ? typeof window.CodeMirror.prototype.openDialog
            : "missing",
        getSearchCursor:
          typeof window.CodeMirror.prototype !== "undefined"
            ? typeof window.CodeMirror.prototype.getSearchCursor
            : "missing",
      }));
      expect(api.type).toBe("function");
      expect(api.fromTextArea).toBe("function");
      expect(api.defineInitHook).toBe("function");
      expect(api.modes).toEqual(
        expect.arrayContaining([
          "css",
          "htmlmixed",
          "javascript",
          "velocity",
          "xml",
        ]),
      );
      // dialog + search addons (source.html find/replace) extend the prototype.
      expect(api.openDialog).toBe("function");
      expect(api.getSearchCursor).toBe("function");

      // Functional check: fromTextArea with the htmlmixed mode round-trips
      // HTML exactly like the classic sys_TextArea code editor does.
      const roundTrip = await page.evaluate(() => {
        document.body.innerHTML =
          '<textarea id="cm-src"><p>Hello <b>World</b></p></textarea>';
        const editor = window.CodeMirror.fromTextArea(
          document.getElementById("cm-src"),
          {
            mode: "htmlmixed",
            lineNumbers: true,
            matchBrackets: true,
            styleActiveLine: true,
          },
        );
        const initial = editor.getValue();
        editor.setValue("<p>npm bundle</p>");
        return {
          initial,
          updated: editor.getValue(),
          lines: editor.lineCount(),
          mode: editor.getOption("mode"),
        };
      });
      expect(roundTrip.initial).toContain("Hello");
      expect(roundTrip.updated).toBe("<p>npm bundle</p>");
      expect(roundTrip.lines).toBe(1);
      expect(roundTrip.mode).toBe("htmlmixed");

      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual(
        [],
      );
    },
  );

  test(
    "classic template JSP mirrors load the same npm bundle",
    { tag: ["@explorer-content-editor", "@editor"] },
    async () => {
      // No live CMS needed: static dual-ship consistency. A stale mirror
      // pointing at the removed per-file layout would 404 its CodeMirror
      // scripts (the bundle spec above asserts those legacy paths are gone).
      const loaders = TEMPLATE_JSP_MIRRORS.map(codemirrorLoaderLines);
      for (const [index, jspPath] of TEMPLATE_JSP_MIRRORS.entries()) {
        expect(
          loaders[index].length,
          `${jspPath} must embed a CodeMirror loader block`,
        ).toBeGreaterThan(0);
        for (const line of loaders[index]) {
          expect(
            line,
            `${jspPath} must not reference the removed per-file layout: ${line}`,
          ).not.toMatch(/codemirror\/(lib|mode|addon|keymap|theme)\//);
        }
        expect(loaders[index]).toContain(
          '<script src="/sys_resources/codemirror/codemirror/codemirror.bundle.js"></script>',
        );
        expect(loaders[index]).toContain(
          '<link rel="stylesheet" href="/sys_resources/codemirror/codemirror/codemirror.bundle.css" />',
        );
        for (const line of loaders[index]) {
          expect(
            line,
            `${jspPath} must not reference the legacy TinyMCE-hosted bundle path: ${line}`,
          ).not.toMatch(/sys_resources\/tinymce\//);
        }
      }
      expect(loaders[1]).toEqual(loaders[0]);
      expect(loaders[2]).toEqual(loaders[0]);
    },
  );
});
