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
 * Classic CodeMirror bundle regression (issues #5626, #5784, #6362).
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
 * per-file WebJar layout ({@code codemirror/lib/...}) must be gone.
 *
 * <p>Issue #6465 (template editor source-view find/replace + line numbers):
 * the {@code sys_TextArea} code-editor renders a shortcut legend
 * ({@code .perc-codeeditor-legend}) for find/replace and keeps
 * {@code lineNumbers: true}; the live test proves the served bundle renders
 * the line-number gutter, finds/replaces through the search addon, and opens
 * the find dialog.</p>
 *
 * <p>Issue #6466 (template editor source-view syntax validation markers
 * before save): the {@code sys_TextArea} code-editor enables the lint addon
 * ({@code lint: true} with the {@code CodeMirror-lint-markers} gutter kept
 * alongside line numbers) backed by the dependency-free tag-balance
 * validator the npm bundle registers as the {@code htmlmixed} lint helper.
 * Unbalanced markup renders gutter markers (red error / yellow warning)
 * before the author saves, with the legend documenting the markers.</p>
 *
 * <p>Issue #6467 (template editor source-view dark-mode + font-size prefs
 * persisted): the {@code sys_TextArea} code-editor legend grows a display
 * toolbar — a dark-mode toggle button ({@code aria-pressed}) and font-size
 * steppers with a live size readout — backed by the
 * {@code CodeMirror.percCodeEditorPrefs} namespace the npm bundle exposes
 * (dark {@code material-darker} theme CSS ships inside the bundle). Choices
 * persist in {@code localStorage} under one shared key and are re-applied on
 * dialog init; missing or corrupt storage degrades to the light default.</p>
 *
 * <p>Issue #6528 (sys_TextArea codeeditor_mode takes effect): the dead
 * {@code codeeditor_mode} param is wired into the {@code fromTextArea} mode
 * through an allow-list ({@code xml} passes through, {@code html} stays an
 * alias of the {@code htmlmixed} default, anything else falls back) so every
 * shipped mode keeps a registered lint helper and pre-save validation keeps
 * working; the legend shows the effective mode
 * ({@code data-perc-codeeditor-mode}). The live test renders unbalanced
 * markup in every effective mode with the exact sys_Templates options and
 * asserts gutter markers appear.</p>
 *
 * <p>Issue #6363 locks the migration at the manifest level (no live CMS):
 * no {@code org.webjars.npm:codemirror} in any {@code pom.xml}, no
 * {@code codemirror-unpack} build step, no {@code codemirror.version}
 * property, and {@code codemirror} declared + lockfile-tracked in both npm
 * manifests.</p></p>
 */

const { test, expect } = require("@playwright/test");
const fs = require("fs");
const path = require("path");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

const PLUGIN_BASE = "/Rhythmyx/sys_resources/codemirror/codemirror";
const BUNDLE_JS = `${PLUGIN_BASE}/codemirror.bundle.js`;
const BUNDLE_CSS = `${PLUGIN_BASE}/codemirror.bundle.css`;
// Old per-file WebJar layout (removed by #5626) under the retired
// TinyMCE-hosted base (deleted with modules/perc-tinymce in #5734;
// #5784 consumers never pointed at it). Kept as a named constant so the
// live test below can assert the path now 404s.
const LEGACY_TINYMCE_BASE =
  "/Rhythmyx/sys_resources/tinymce/plugins/codemirror/codemirror";
const LEGACY_LIB = `${LEGACY_TINYMCE_BASE}/lib/codemirror.js`;

// Classic template-editor JSPs that embed the CodeMirror loader. The
// `WebUI/war` tree is a dual-ship mirror of `WebUI/src/main/webapp` for
// these residual dialogs: every copy must load the same TinyMCE-independent
// npm bundle (#5784), never the removed per-file WebJar layout (#5626) and
// never the retired TinyMCE-hosted bundle path (deleted with
// modules/perc-tinymce in #5734).
const REPO_ROOT = path.join(__dirname, "..", "..", "..", "..");
const TEMPLATE_JSP_MIRRORS = [
  "WebUI/src/main/webapp/cm/app/includes/template/editTemplate_js.jsp",
  "WebUI/src/main/webapp/cm/pages/app/includes/template/editTemplate_js.jsp",
  "WebUI/war/app/includes/template/editTemplate_js.jsp",
];

// Classic template-metadata dialog widget (issue #5626 slice: classic
// page-edit dialogs stay in scope until deleted). The template editor's
// "Edit Meta Data" action (PercTemplateView) opens this jQuery dialog; its
// iframe hosts the server-rendered code fields and save reads them through
// the CodeMirror 5 instance the npm bundle creates
// (`textarea.nextSibling.CodeMirror`, the fromTextArea wrapper contract).
// Both dual-ship copies must carry the hardened read (enhanced-instance
// value with plain-textarea fallback and a visible save-failure message
// naming the field) and neither may reference a removed layout.
const METADATA_DIALOG_MIRRORS = [
  "WebUI/src/main/webapp/cm/widgets/perc_template_metadata_dialog.js",
  "WebUI/war/widgets/perc_template_metadata_dialog.js",
];

// Issue #6362 (slice 2/3 — "no exceptions"): classic content/page-edit
// dialogs beyond the template editor render code editing through the
// sys_TextArea control in sys_Templates.xsl, which loads the same
// TinyMCE-independent npm bundle via FileDescriptors. Pin it statically
// like the JSP mirrors so a re-point at the removed per-file layout or the
// legacy TinyMCE-hosted path fails fast without a live CMS.
const SYS_TEMPLATES_XSL =
  "system/cms/content/applications/sys_resources/ApplicationFiles/stylesheets/sys_Templates.xsl";
const SYS_TEMPLATES_BUNDLE_JS =
  "<psxctl:FileLocation>/sys_resources/codemirror/codemirror/codemirror.bundle.js</psxctl:FileLocation>";
const SYS_TEMPLATES_BUNDLE_CSS =
  "<psxctl:FileLocation>/sys_resources/codemirror/codemirror/codemirror.bundle.css</psxctl:FileLocation>";

function codemirrorLoaderLines(jspPath) {
  const text = fs
    .readFileSync(path.join(REPO_ROOT, jspPath), "utf8")
    .replace(/\r\n/g, "\n");
  return text.split("\n").filter((line) => /codemirror/i.test(line));
}

// Issue #6363 (slice 3/3): the WebJar + Ant-unpack removal and the root-POM
// freeze lift are build-manifest facts. This pins the manifests the migration
// owns so a re-introduced WebJar, unpack step, version property, or dropped
// npm dependency fails fast without a live CMS. Kept to the migration-owned
// manifests plus a repo-wide pom sweep (skipping build-output and VCS dirs).
// (The `modules/perc-tinymce` manifest retired with that module in #5734,
// so only the TinyMCE-independent bundle manifest remains.)
const NPM_TRACKED_MANIFESTS = [
  "modules/perc-common-ui-bundle/package.json",
];
const POM_SWEEP_SKIP_DIRS = new Set([
  ".git",
  "node_modules",
  "target",
  "test-results",
  "playwright-report",
]);

function collectPomPaths(dir) {
  const found = [];
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    if (entry.isDirectory()) {
      if (POM_SWEEP_SKIP_DIRS.has(entry.name)) {
        continue;
      }
      found.push(...collectPomPaths(path.join(dir, entry.name)));
    } else if (entry.isFile() && entry.name === "pom.xml") {
      found.push(path.join(dir, entry.name));
    }
  }
  return found;
}

// Issue #5746 (detach lock): extensions swept for orphaned references to
// the retired TinyMCE-hosted CodeMirror layout. Every tracked text type
// that can be served, executed, or packaged is covered — web sources,
// markup/config, and operator scripts. Prose is out of scope by design:
// migration-history notes in Markdown legitimately discuss retired paths,
// and `docs/`, `specs/`, `product-docs/` trees are not served code.
// `pom.xml` files are covered with XML comment-stripping here too (the
// manifest-lock test above owns the WebJar/unpack/version pins);
// `package-lock.json` files only carry the npm registry tarball entry.
const DETACH_SWEEP_EXTENSIONS = new Set([
  ".java",
  ".js",
  ".jsx",
  ".mjs",
  ".cjs",
  ".ts",
  ".tsx",
  ".jsp",
  ".jspx",
  ".htm",
  ".html",
  ".xhtm",
  ".css",
  ".vm",
  ".tld",
  ".tag",
  ".xml",
  ".xsl",
  ".xslt",
  ".dtd",
  ".xsd",
  ".wsdl",
  ".properties",
  ".json",
  ".yaml",
  ".yml",
  ".txt",
  ".py",
  ".sh",
  ".bat",
  ".cmd",
  ".contenttype",
  ".pdt",
  ".rpg",
  ".acldef",
  ".datasource",
  ".workflowdef",
  ".ent",
  ".tbl",
  ".frm",
  ".extension",
  ".application",
  ".sample",
]);
const DETACH_SWEEP_SKIP_DIRS = new Set([
  ".git",
  "node_modules",
  "target",
  "test-results",
  "playwright-report",
  "specs", // frozen design docs, not live sources
  "docs", // engineering/history notes, not live sources
  "product-docs", // customer help, not served code
]);
const DETACH_SWEEP_SKIP_FILES = new Set([
  // This spec documents the retired path as LEGACY_TINYMCE_BASE (a live
  // string constant) so the live test above can assert the old layout
  // 404s; the constant is the allow-list, not an orphan.
  "modules/perc-qa-automation/frontend/tests/editor-codemirror-bundle.spec.js",
]);

function collectSourcePaths(dir) {
  const found = [];
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      if (DETACH_SWEEP_SKIP_DIRS.has(entry.name)) {
        continue;
      }
      found.push(...collectSourcePaths(full));
      continue;
    }
    if (!entry.isFile()) {
      continue;
    }
    if (!DETACH_SWEEP_EXTENSIONS.has(path.extname(entry.name))) {
      continue;
    }
    if (DETACH_SWEEP_SKIP_FILES.has(path.relative(REPO_ROOT, full))) {
      continue;
    }
    found.push(full);
  }
  return found;
}

function stripXmlComments(text) {
  return text.replace(/<!--[\s\S]*?-->/g, "");
}

function stripCLikeComments(text) {
  return text
    .replace(/\/\*[\s\S]*?\*\//g, "")
    .replace(/(^|[^:])\/\/.*$/gm, "$1");
}

function stripHashComments(text) {
  // `#` opens a line comment in Python, shell, and YAML. A `#` preceded
  // by `:` is kept (URL scheme guard symmetry with the C-like stripper).
  return text.replace(/(^|[^:])#.*$/gm, "$1");
}

function stripPropertiesComments(text) {
  // In `.properties` files only a leading `#` or `!` opens a comment —
  // mid-line markers are literal values and must be kept.
  return text.replace(/^[#!].*$/gm, "");
}

function stripVelocityComments(text) {
  return text
    .replace(/#\*[\s\S]*?\*#/, "")
    .replace(/(^|[^:])##.*$/gm, "$1");
}

function stripBatchComments(text) {
  return text.replace(/^\s*(?:REM\b|::).*$/gim, "");
}

// Strip comments so migration-history notes (e.g. "relocated from
// modules/perc-tinymce", "legacy TinyMCE-hosted fallback path") do not trip
// the sweep — only live code, markup, and string literals count. Mirrors
// the manifest-lock test's XML-comment stripping above.
const DETACH_COMMENT_STRIPPERS = {
  ".java": stripCLikeComments,
  ".js": stripCLikeComments,
  ".jsx": stripCLikeComments,
  ".mjs": stripCLikeComments,
  ".cjs": stripCLikeComments,
  ".ts": stripCLikeComments,
  ".tsx": stripCLikeComments,
  ".css": (text) => text.replace(/\/\*[\s\S]*?\*\//g, ""),
  ".jsp": (text) => stripXmlComments(text.replace(/<%--[\s\S]*?--%>/g, "")),
  ".jspx": (text) => stripXmlComments(text.replace(/<%--[\s\S]*?--%>/g, "")),
  ".htm": stripXmlComments,
  ".html": stripXmlComments,
  ".xhtm": stripXmlComments,
  ".xml": stripXmlComments,
  ".xsl": stripXmlComments,
  ".xslt": stripXmlComments,
  ".dtd": stripXmlComments,
  ".xsd": stripXmlComments,
  ".wsdl": stripXmlComments,
  ".vm": stripVelocityComments,
  ".py": stripHashComments,
  ".sh": stripHashComments,
  ".yaml": stripHashComments,
  ".yml": stripHashComments,
  ".properties": stripPropertiesComments,
  ".bat": stripBatchComments,
  ".cmd": stripBatchComments,
};

function stripSourceComments(text, filePath) {
  const stripper = DETACH_COMMENT_STRIPPERS[path.extname(filePath)];
  return stripper ? stripper(text) : text;
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

      // C5 live proof (issues #6363, #6465): render a visible editor from the
      // served npm bundle and capture the passing surface. Issue #6465
      // proves the template editor source-view contract on the served
      // bundle: the line-number gutter renders, the search addon finds and
      // replaces, and the find dialog opens.
      await page.addStyleTag({ url: `${BASE_URL}${BUNDLE_CSS}` });
      const sourceView = await page.evaluate(() => {
        document.body.innerHTML =
          "<h1>CodeMirror npm bundle live proof (#6363, #6465)</h1>" +
          '<textarea id="cm-proof"><p>Hello <b>World</b></p></textarea>';
        const proof = window.CodeMirror.fromTextArea(
          document.getElementById("cm-proof"),
          {
            mode: "htmlmixed",
            lineNumbers: true,
            matchBrackets: true,
            styleActiveLine: true,
          },
        );
        proof.setValue("<p>Hello <b>World</b></p>\n<p>Second line</p>");
        // Line numbers: CM5 renders one .CodeMirror-linenumber marker per
        // visible line (in the gutter wrapper layer, not inside
        // .CodeMirror-gutters, which only reserves the gutter width).
        const guttersPresent =
          document.querySelectorAll(".CodeMirror-gutters").length > 0;
        const gutterMarkers = document.querySelectorAll(
          ".CodeMirror-linenumber",
        ).length;
        // Find: the search addon locates "World" on the first line.
        const cursor = proof.getSearchCursor("World");
        const found = cursor.findNext();
        const foundAt = found
          ? { line: cursor.from().line, ch: cursor.from().ch }
          : null;
        // Replace: the searchcursor machinery swaps the match in place.
        if (found) {
          cursor.replace("npm bundle");
        }
        const replaced = proof.getValue();
        // Find dialog: execCommand("find") renders the search prompt.
        proof.execCommand("find");
        const dialogOpen =
          document.querySelectorAll(".CodeMirror-dialog").length > 0;
        return {
          lines: proof.lineCount(),
          guttersPresent,
          gutterMarkers,
          found,
          foundAt,
          replaced,
          dialogOpen,
        };
      });
      expect(sourceView.lines).toBe(2);
      expect(
        sourceView.guttersPresent,
        "line-number gutter layer must render (#6465)",
      ).toBe(true);
      expect(
        sourceView.gutterMarkers,
        "line-number gutter must render one marker per line (#6465)",
      ).toBe(2);
      expect(
        sourceView.found,
        "search addon must find 'World' (#6465)",
      ).toBe(true);
      expect(sourceView.foundAt).toEqual({ line: 0, ch: 12 });
      expect(sourceView.replaced).toBe(
        "<p>Hello <b>npm bundle</b></p>\n<p>Second line</p>",
      );
      expect(
        sourceView.dialogOpen,
        "find dialog must open (#6465)",
      ).toBe(true);
      expect(await page.locator(".CodeMirror-dialog").isVisible()).toBe(true);
      await page.screenshot({
        path: "test-results/live-proof-6363-codemirror-bundle.png",
      });

      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual(
        [],
      );
    },
  );

  test(
    "template editor source-view flags unbalanced markup before save",
    { tag: ["@explorer-content-editor", "@editor"] },
    async ({ page }) => {
      // Live proof for issue #6466 on the served npm bundle: the
      // dependency-free tag-balance validator flags unbalanced markup with
      // lint gutter markers (red error / yellow warning) before save, and
      // balanced markup renders no markers.
      test.setTimeout(60_000);
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      page.on("console", (msg) => {
        if (msg.type() === "error") {
          pageErrors.push(msg.text());
        }
      });

      await loginAsAdmin(page);
      await page.addScriptTag({ url: `${BASE_URL}${BUNDLE_JS}` });
      await page.addStyleTag({ url: `${BASE_URL}${BUNDLE_CSS}` });

      // The bundle exposes the validator on the CodeMirror global:
      // unbalanced markup reports the unclosed tag plus the mismatch,
      // balanced markup reports nothing.
      const unit = await page.evaluate(() => ({
        bad: window.CodeMirror.percValidateTemplateHtml(
          "<div><p>Head code</div>",
        ),
        good: window.CodeMirror.percValidateTemplateHtml(
          '<div class="a"><br><p>ok</p></div>',
        ),
      }));
      expect(unit.bad.length).toBe(2);
      expect(unit.bad.map((finding) => finding.severity)).toEqual([
        "warning",
        "error",
      ]);
      expect(unit.bad[0].message).toContain("<p>");
      expect(unit.bad[1].message).toContain("</div>");
      expect(unit.good).toEqual([]);

      // Render an editor exactly like the sys_TextArea code-editor control
      // (same mode, gutters, and lint option as sys_Templates.xsl).
      await page.evaluate(() => {
        document.body.innerHTML =
          "<h1>Template validation live proof (#6466)</h1>" +
          '<textarea id="cm-validate"><div><p>Head code</div></textarea>';
        window.__cmValidate = window.CodeMirror.fromTextArea(
          document.getElementById("cm-validate"),
          {
            mode: "htmlmixed",
            lineNumbers: true,
            gutters: ["CodeMirror-linenumbers", "CodeMirror-lint-markers"],
            lint: true,
            matchBrackets: true,
            styleActiveLine: true,
          },
        );
      });
      // The lint addon validates the initial content synchronously, so the
      // markers are present before any edit — i.e. visible before save.
      await page.waitForSelector(
        ".CodeMirror-lint-marker-error, .CodeMirror-lint-marker-warning",
        { timeout: 10_000 },
      );
      const flagged = await page.evaluate(() => ({
        gutters: document.querySelectorAll(".CodeMirror-lint-markers").length,
        errors: document.querySelectorAll(".CodeMirror-lint-marker-error")
          .length,
        warnings: document.querySelectorAll(
          ".CodeMirror-lint-marker-warning",
        ).length,
      }));
      expect(
        flagged.gutters,
        "lint-markers gutter must render (#6466)",
      ).toBeGreaterThan(0);
      expect(
        flagged.errors + flagged.warnings,
        "unbalanced markup must render markers before save (#6466)",
      ).toBeGreaterThan(0);

      // Balanced markup clears every marker (change lint is debounced, so
      // wait for the markers to drain rather than sleeping a fixed delay).
      await page.evaluate(() =>
        window.__cmValidate.setValue("<div><p>Head code</p></div>"),
      );
      await page.waitForFunction(
        () =>
          !document.querySelector(
            ".CodeMirror-lint-marker-error, .CodeMirror-lint-marker-warning",
          ),
        null,
        { timeout: 10_000 },
      );
      await page.screenshot({
        path: "test-results/live-proof-6466-template-validation.png",
      });

      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual(
        [],
      );
    },
  );

  test(
    "template editor source-view flags unbalanced markup in every shipped mode",
    { tag: ["@explorer-content-editor", "@editor"] },
    async ({ page }) => {
      // Live proof for issue #6528 on the served npm bundle: the
      // codeeditor_mode allow-list in sys_Templates.xsl only ships modes
      // with a registered lint helper (htmlmixed + xml, with html aliased
      // to htmlmixed). Render unbalanced markup in each effective mode
      // with the exact sys_TextArea options (mode, gutters, lint) and
      // assert gutter markers appear before save for every one.
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      page.on("console", (msg) => {
        if (msg.type() === "error") {
          pageErrors.push(msg.text());
        }
      });

      await loginAsAdmin(page);
      await page.addScriptTag({ url: `${BASE_URL}${BUNDLE_JS}` });
      await page.addStyleTag({ url: `${BASE_URL}${BUNDLE_CSS}` });

      const flagged = await page.evaluate(() => {
        const modes = ["htmlmixed", "xml"];
        const result = {};
        document.body.innerHTML =
          "<h1>Template validation per shipped mode (#6528)</h1>" +
          '<textarea id="cm-mode-htmlmixed"><div><p>Head code</div></textarea>' +
          '<textarea id="cm-mode-xml"><div><p>Head code</div></textarea>';
        for (const mode of modes) {
          const editor = window.CodeMirror.fromTextArea(
            document.getElementById(`cm-mode-${mode}`),
            {
              mode,
              lineNumbers: true,
              gutters: ["CodeMirror-linenumbers", "CodeMirror-lint-markers"],
              lint: true,
              matchBrackets: true,
              styleActiveLine: true,
            },
          );
          const wrapper = editor.getWrapperElement();
          result[mode] = {
            option: editor.getOption("mode"),
            errors: wrapper.querySelectorAll(".CodeMirror-lint-marker-error")
              .length,
            warnings: wrapper.querySelectorAll(
              ".CodeMirror-lint-marker-warning",
            ).length,
          };
        }
        return result;
      });
      for (const mode of ["htmlmixed", "xml"]) {
        expect(
          flagged[mode].option,
          `editor must run in the requested ${mode} mode (#6528)`,
        ).toBe(mode);
        expect(
          flagged[mode].errors + flagged[mode].warnings,
          `unbalanced markup in ${mode} mode must render markers before save (#6528)`,
        ).toBeGreaterThan(0);
      }
      await page.screenshot({
        path: "test-results/live-proof-6528-template-modes.png",
      });

      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual(
        [],
      );
    },
  );

  test(
    "template editor source-view persists dark-mode and font-size prefs",
    { tag: ["@explorer-content-editor", "@editor"] },
    async ({ page }) => {
      // Live proof for issue #6467 on the served npm bundle: the
      // percCodeEditorPrefs namespace defaults to light/13px, applies the
      // dark theme (visibly dark background) plus a larger font, and
      // round-trips through localStorage so a reload restores the author
      // choice. The dark theme CSS ships inside the served bundle CSS.
      test.setTimeout(90_000);
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      page.on("console", (msg) => {
        if (msg.type() === "error") {
          pageErrors.push(msg.text());
        }
      });

      await loginAsAdmin(page);

      // The served bundle CSS carries the dark theme (no extra request).
      const cssResponse = await page.request.get(`${BASE_URL}${BUNDLE_CSS}`);
      expect(cssResponse.ok(), `bundle CSS not served: ${BUNDLE_CSS}`).toBe(
        true,
      );
      const cssText = await cssResponse.text();
      expect(
        cssText.includes("cm-s-material-darker"),
        "served bundle CSS must ship the dark theme (#6467)",
      ).toBe(true);

      await page.addScriptTag({ url: `${BASE_URL}${BUNDLE_JS}` });
      await page.addStyleTag({ url: `${BASE_URL}${BUNDLE_CSS}` });

      // Fresh profile: defaults are light theme at 13px.
      const defaults = await page.evaluate(() => ({
        shape: Object.keys(
          window.CodeMirror.percCodeEditorPrefs || {},
        ).sort(),
        stored: window.CodeMirror.percCodeEditorPrefs.read(),
      }));
      expect(defaults.shape).toEqual(
        expect.arrayContaining([
          "apply",
          "defaults",
          "read",
          "stepFontSize",
          "storageKey",
          "themes",
          "toggleTheme",
          "write",
        ]),
      );
      expect(defaults.stored).toEqual({ theme: "default", fontSize: 13 });

      // Apply dark + larger text exactly like sys_Templates.xsl dialog init.
      const applied = await page.evaluate(() => {
        document.body.innerHTML =
          "<h1>Display prefs live proof (#6467)</h1>" +
          '<textarea id="cm-prefs"><p>Hello</p></textarea>';
        const editor = window.CodeMirror.fromTextArea(
          document.getElementById("cm-prefs"),
          { mode: "htmlmixed", lineNumbers: true, theme: "default" },
        );
        const prefs = { theme: "material-darker", fontSize: 18 };
        window.CodeMirror.percCodeEditorPrefs.apply(editor, prefs);
        const wrapper = editor.getWrapperElement();
        const background = window.getComputedStyle(
          wrapper.querySelector(".CodeMirror-code, .CodeMirror"),
        ).backgroundColor;
        return {
          theme: editor.getOption("theme"),
          fontSize: wrapper.style.fontSize,
          background,
        };
      });
      expect(applied.theme).toBe("material-darker");
      expect(applied.fontSize).toBe("18px");
      // material-darker background is #212121: every channel stays dark.
      const channels = applied.background
        .match(/[\d.]+/g)
        .slice(0, 3)
        .map(Number);
      expect(
        channels.every((channel) => channel < 80),
        `dark theme background must read dark, got ${applied.background} (#6467)`,
      ).toBe(true);

      // Persist, reload, and prove the stored choice restores.
      await page.evaluate(() =>
        window.CodeMirror.percCodeEditorPrefs.write(window.localStorage, {
          theme: "material-darker",
          fontSize: 20,
        }),
      );
      await page.reload();
      await page.addScriptTag({ url: `${BASE_URL}${BUNDLE_JS}` });
      const restored = await page.evaluate(() => {
        const prefs = window.CodeMirror.percCodeEditorPrefs.read(
          window.localStorage,
        );
        document.body.innerHTML =
          '<textarea id="cm-restored"><p>Hello</p></textarea>';
        const editor = window.CodeMirror.fromTextArea(
          document.getElementById("cm-restored"),
          { mode: "htmlmixed", lineNumbers: true, theme: "default" },
        );
        window.CodeMirror.percCodeEditorPrefs.apply(editor, prefs);
        return {
          prefs,
          theme: editor.getOption("theme"),
          fontSize: editor.getWrapperElement().style.fontSize,
        };
      });
      expect(restored.prefs).toEqual({
        theme: "material-darker",
        fontSize: 20,
      });
      expect(restored.theme).toBe("material-darker");
      expect(restored.fontSize).toBe("20px");
      await page.screenshot({
        path: "test-results/live-proof-6467-template-prefs.png",
      });

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

  test(
    "sys_TextArea code-editor control loads the same npm bundle",
    { tag: ["@explorer-content-editor", "@editor"] },
    async () => {
      // No live CMS needed: static pin for issue #6362 (slice 2/3). The
      // sys_TextArea code-editor is the remaining classic page-edit dialog
      // surface; its CodeMirror lines must name the npm bundle
      // FileDescriptors and nothing else serving CodeMirror.
      const lines = codemirrorLoaderLines(SYS_TEMPLATES_XSL);
      expect(
        lines.length,
        `${SYS_TEMPLATES_XSL} must embed a CodeMirror loader block`,
      ).toBeGreaterThan(0);
      const trimmed = lines.map((line) => line.trim());
      expect(
        trimmed,
        `${SYS_TEMPLATES_XSL} must load the npm bundle script`,
      ).toContain(SYS_TEMPLATES_BUNDLE_JS);
      expect(
        trimmed,
        `${SYS_TEMPLATES_XSL} must load the npm bundle stylesheet`,
      ).toContain(SYS_TEMPLATES_BUNDLE_CSS);
      for (const line of lines) {
        expect(
          line,
          `${SYS_TEMPLATES_XSL} must not reference the removed per-file layout: ${line}`,
        ).not.toMatch(/codemirror\/(lib|mode|addon|keymap|theme)\//);
        expect(
          line,
          `${SYS_TEMPLATES_XSL} must not reference the legacy TinyMCE-hosted bundle path: ${line}`,
        ).not.toMatch(/sys_resources\/tinymce\//);
      }
    },
  );

  test(
    "sys_TextArea code editor shows find/replace legend and line numbers",
    { tag: ["@explorer-content-editor", "@editor"] },
    async () => {
      // No live CMS needed: static pin for issue #6465. The template
      // editor source-view (sys_TextArea with codeeditor=yes) keeps
      // lineNumbers on and renders a shortcut legend so authors can
      // discover find/replace (the bundle ships the dialog + search
      // addons; see the live test). The legend must live inside the
      // codeeditor branch so plain textareas stay chrome-free.
      const text = fs
        .readFileSync(path.join(REPO_ROOT, SYS_TEMPLATES_XSL), "utf8")
        .replace(/\r\n/g, "\n");
      const codeeditorStart = text.indexOf('$codeeditor = \'yes\'');
      expect(
        codeeditorStart,
        `${SYS_TEMPLATES_XSL} must keep the codeeditor branch`,
      ).toBeGreaterThan(-1);
      const branch = text.slice(codeeditorStart);
      const legendAt = branch.indexOf("perc-codeeditor-legend");
      expect(
        legendAt,
        `${SYS_TEMPLATES_XSL} must render the find/replace legend for code editors (#6465)`,
      ).toBeGreaterThan(-1);
      const legendBlock = branch.slice(legendAt, legendAt + 800);
      for (const hint of [
        "Ctrl-F",
        "Ctrl-G",
        "Shift-Ctrl-F",
        "Shift-Ctrl-R",
        "Cmd-F",
      ]) {
        expect(
          legendBlock,
          `${SYS_TEMPLATES_XSL} legend must name the ${hint} shortcut (#6465)`,
        ).toContain(hint);
      }
      expect(
        legendBlock,
        `${SYS_TEMPLATES_XSL} legend must not be an empty placeholder (#6465)`,
      ).not.toMatch(/perc-codeeditor-legend">\s*<\/div>/);
      expect(
        branch,
        `${SYS_TEMPLATES_XSL} code editor must keep line numbers on (#6465)`,
      ).toContain("lineNumbers: true");
    },
  );

  test(
    "sys_TextArea code editor enables validation markers before save",
    { tag: ["@explorer-content-editor", "@editor"] },
    async () => {
      // No live CMS needed: static pin for issue #6466. The template
      // editor source-view (sys_TextArea with codeeditor=yes) turns the
      // lint addon on (`lint: true`) with a gutter pair that keeps the
      // line numbers and adds the lint-markers gutter, so unbalanced
      // markup renders markers before the author saves. The legend must
      // document the markers (copy clarity: what the colors mean and that
      // hovering shows details).
      const text = fs
        .readFileSync(path.join(REPO_ROOT, SYS_TEMPLATES_XSL), "utf8")
        .replace(/\r\n/g, "\n");
      const codeeditorStart = text.indexOf('$codeeditor = \'yes\'');
      expect(
        codeeditorStart,
        `${SYS_TEMPLATES_XSL} must keep the codeeditor branch`,
      ).toBeGreaterThan(-1);
      const scriptStart = text.indexOf("fromTextArea", codeeditorStart);
      expect(
        scriptStart,
        `${SYS_TEMPLATES_XSL} code editor must create a CodeMirror instance`,
      ).toBeGreaterThan(-1);
      const scriptBlock = text.slice(scriptStart, scriptStart + 1200);
      expect(
        scriptBlock,
        `${SYS_TEMPLATES_XSL} code editor must enable lint for pre-save validation (#6466)`,
      ).toContain("lint: true");
      const guttersAt = scriptBlock.indexOf("gutters:");
      expect(
        guttersAt,
        `${SYS_TEMPLATES_XSL} code editor must declare lint gutters (#6466)`,
      ).toBeGreaterThan(-1);
      const guttersBlock = scriptBlock.slice(guttersAt, guttersAt + 200);
      expect(
        guttersBlock,
        `${SYS_TEMPLATES_XSL} code editor must keep line numbers alongside lint markers (#6466)`,
      ).toContain("CodeMirror-linenumbers");
      expect(
        guttersBlock,
        `${SYS_TEMPLATES_XSL} code editor must render the lint-markers gutter (#6466)`,
      ).toContain("CodeMirror-lint-markers");
      const legendAt = text.indexOf("perc-codeeditor-legend", codeeditorStart);
      expect(
        legendAt,
        `${SYS_TEMPLATES_XSL} must keep the code-editor legend`,
      ).toBeGreaterThan(-1);
      const legendBlock = text.slice(legendAt, legendAt + 1400);
      for (const hint of [
        "Validation",
        "unbalanced",
        "before save",
        "hover",
      ]) {
        expect(
          legendBlock,
          `${SYS_TEMPLATES_XSL} legend must document validation markers (#6466)`,
        ).toContain(hint);
      }
    },
  );

  test(
    "sys_TextArea codeeditor_mode takes effect with a lint-safe fallback",
    { tag: ["@explorer-content-editor", "@editor"] },
    async () => {
      // No live CMS needed: static pin for issue #6528. The codeeditor_mode
      // param used to be dead (read into $codeeditor_mode but never
      // referenced while fromTextArea hardcoded htmlmixed). The wiring must
      // (a) feed an allow-listed effective mode into fromTextArea, (b) pass
      // xml through while aliasing the historic "html" value (bare "html"
      // does not resolve to a CodeMirror mode and would silently lose the
      // #6466 lint helper) and falling back to htmlmixed otherwise, and
      // (c) expose the effective mode on the legend for authors and tests.
      const text = fs
        .readFileSync(path.join(REPO_ROOT, SYS_TEMPLATES_XSL), "utf8")
        .replace(/\r\n/g, "\n");
      const codeeditorStart = text.indexOf('$codeeditor = \'yes\'');
      expect(
        codeeditorStart,
        `${SYS_TEMPLATES_XSL} must keep the codeeditor branch`,
      ).toBeGreaterThan(-1);
      const branch = text.slice(codeeditorStart);
      expect(
        branch,
        `${SYS_TEMPLATES_XSL} must resolve an effective editor mode (#6528)`,
      ).toContain("codeeditor_effective_mode");
      const scriptStart = branch.indexOf("fromTextArea");
      expect(
        scriptStart,
        `${SYS_TEMPLATES_XSL} code editor must create a CodeMirror instance`,
      ).toBeGreaterThan(-1);
      const scriptBlock = branch.slice(scriptStart, scriptStart + 1200);
      expect(
        scriptBlock,
        `${SYS_TEMPLATES_XSL} must wire codeeditor_mode into the editor mode (#6528)`,
      ).toContain("mode: '<xsl:value-of select=\"$codeeditor_effective_mode\" />'");
      expect(
        scriptBlock,
        `${SYS_TEMPLATES_XSL} must not hardcode the editor mode (#6528)`,
      ).not.toContain("mode: 'htmlmixed'");
      const effectiveAt = text.indexOf(
        '<xsl:variable name="codeeditor_effective_mode">',
      );
      expect(
        effectiveAt,
        `${SYS_TEMPLATES_XSL} must resolve an effective editor mode (#6528)`,
      ).toBeGreaterThan(-1);
      const effectiveBlock = text.slice(effectiveAt, effectiveAt + 1200);
      expect(
        effectiveBlock,
        `${SYS_TEMPLATES_XSL} must pass xml through (#6528)`,
      ).toMatch(/\$codeeditor_mode\s*=\s*'xml'/);
      expect(
        effectiveBlock,
        `${SYS_TEMPLATES_XSL} must fall back to htmlmixed for unknown modes (#6528)`,
      ).toContain("<xsl:otherwise>htmlmixed</xsl:otherwise>");
      expect(
        branch,
        `${SYS_TEMPLATES_XSL} legend must expose the effective mode (#6528)`,
      ).toContain('data-perc-codeeditor-mode="{$codeeditor_effective_mode}"');
      expect(
        branch,
        `${SYS_TEMPLATES_XSL} legend must show a mode badge (#6528)`,
      ).toContain("data-perc-codeeditor-mode-badge");
    },
  );

  test(
    "sys_TextArea code editor offers persisted dark-mode and font-size prefs",
    { tag: ["@explorer-content-editor", "@editor"] },
    async () => {
      // No live CMS needed: static pin for issue #6467. The template
      // editor source-view (sys_TextArea with codeeditor=yes) renders a
      // display toolbar next to the shortcut legend: a dark-mode toggle
      // button with pressed state plus font-size steppers with a live
      // readout, all delegating to the CodeMirror.percCodeEditorPrefs
      // namespace the npm bundle exposes and persisting through
      // localStorage. The toolbar must live inside the codeeditor branch
      // so plain textareas stay chrome-free.
      const text = fs
        .readFileSync(path.join(REPO_ROOT, SYS_TEMPLATES_XSL), "utf8")
        .replace(/\r\n/g, "\n");
      const codeeditorStart = text.indexOf('$codeeditor = \'yes\'');
      expect(
        codeeditorStart,
        `${SYS_TEMPLATES_XSL} must keep the codeeditor branch`,
      ).toBeGreaterThan(-1);
      const branch = text.slice(codeeditorStart);
      for (const pin of [
        "data-perc-codeeditor-theme-toggle",
        "data-perc-codeeditor-font-decrease",
        "data-perc-codeeditor-font-increase",
        "data-perc-codeeditor-font-size",
        "aria-pressed",
        "aria-live",
        "Dark mode",
        "percCodeEditorPrefs",
        "material-darker",
        "localStorage",
        "percCodeEditorToggleTheme",
        "percCodeEditorStepFontSize",
      ]) {
        expect(
          branch,
          `${SYS_TEMPLATES_XSL} code editor must wire the display prefs toolbar (#6467): ${pin}`,
        ).toContain(pin);
      }
      const legendAt = branch.indexOf("perc-codeeditor-display-prefs");
      expect(
        legendAt,
        `${SYS_TEMPLATES_XSL} must render the display prefs toolbar for code editors (#6467)`,
      ).toBeGreaterThan(-1);
    },
  );

  test(
    "classic template metadata dialog reads CodeMirror values safely",
    { tag: ["@explorer-content-editor", "@editor"] },
    async () => {
      // No live CMS needed: static pin for the #5626 metadata-dialog
      // hardening slice. Save must read each code field through the
      // CodeMirror 5 instance the npm bundle creates (the fromTextArea
      // wrapper contract: `textarea.nextSibling.CodeMirror.getValue()`),
      // fall back to the live textarea value when a field was not enhanced,
      // and abort with a visible named-field message when a field is absent
      // (previously a silent TypeError left the dialog open with no message).
      // Both dual-ship mirrors carry the same contract; neither may
      // reference the removed per-file layout or the retired TinyMCE-hosted
      // path, and neither may keep the dead per-file-era editor vars.
      const texts = METADATA_DIALOG_MIRRORS.map((rel) =>
        fs.readFileSync(path.join(REPO_ROOT, rel), "utf8").replace(/\r\n/g, "\n"),
      );
      for (const [index, rel] of METADATA_DIALOG_MIRRORS.entries()) {
        const text = texts[index];
        expect(
          text.length,
          `${rel} must embed the metadata dialog widget`,
        ).toBeGreaterThan(0);
        for (const pin of [
          "getCodeEditorValue",
          "nextSibling.CodeMirror",
          "getValue()",
          "return field.val()",
          "Template metadata field is missing: ",
          "Could not read the template code fields",
          "Nothing was saved.",
        ]) {
          expect(
            text,
            `${rel} must harden the CodeMirror save read (#5626): ${pin}`,
          ).toContain(pin);
        }
        for (const dead of ["codeMirrorSet", "cm1"]) {
          expect(
            text,
            `${rel} must not keep dead per-file-era editor vars: ${dead}`,
          ).not.toContain(dead);
        }
        for (const line of text.split("\n")) {
          if (!/codemirror/i.test(line)) {
            continue;
          }
          expect(
            line,
            `${rel} must not reference the removed per-file layout: ${line}`,
          ).not.toMatch(/codemirror\/(lib|mode|addon|keymap|theme)\//);
          expect(
            line,
            `${rel} must not reference the retired TinyMCE-hosted bundle path: ${line}`,
          ).not.toMatch(/sys_resources\/tinymce\//);
        }
      }
      // Dual-ship consistency (header rule): the war mirror must carry the
      // same Intersoft header as the webapp source.
      for (const [index, rel] of METADATA_DIALOG_MIRRORS.entries()) {
        expect(
          texts[index],
          `${rel} must carry the Intersoft copyright header`,
        ).toContain("Copyright (c) 2026 Intersoft Data Labs, Inc.");
        expect(
          texts[index],
          `${rel} must not keep the retired Percussion header`,
        ).not.toContain("Percussion Software, Inc.");
      }
    },
  );

  test(
    "classic metadata save read works against the served npm bundle",
    { tag: ["@explorer-content-editor", "@editor"] },
    async ({ page }) => {
      // Live proof for the #5626 metadata-dialog hardening slice on the
      // served npm bundle: the exact read chain save uses
      // (`textarea.nextSibling.CodeMirror.getValue()`) returns the enhanced
      // value, an un-enhanced textarea still yields its live value (the
      // fallback), and a missing field is detectable before anything is
      // sent. Mirrors getCodeEditorValue in
      // perc_template_metadata_dialog.js (both dual-ship copies, pinned
      // statically above); the widget itself needs the full dialog harness
      // so the bundle mechanics are proved here.
      test.setTimeout(60_000);
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      page.on("console", (msg) => {
        if (msg.type() === "error") {
          pageErrors.push(msg.text());
        }
      });

      await loginAsAdmin(page);
      await page.addScriptTag({ url: `${BASE_URL}${BUNDLE_JS}` });
      await page.addStyleTag({ url: `${BASE_URL}${BUNDLE_CSS}` });

      const read = await page.evaluate(() => {
        document.body.innerHTML =
          "<h1>Metadata save read live proof (#5626)</h1>" +
          '<textarea id="additional_head_content"><p>Head</p></textarea>' +
          '<textarea id="code_insert_after_body_start"><p>Body</p></textarea>' +
          '<textarea id="plain_fallback"><p>Plain</p></textarea>';
        // Enhance two fields exactly like the server-rendered form does.
        for (const id of [
          "additional_head_content",
          "code_insert_after_body_start",
        ]) {
          window.CodeMirror.fromTextArea(document.getElementById(id), {
            mode: "htmlmixed",
            lineNumbers: true,
          });
        }
        // Same algorithm as getCodeEditorValue in the dialog widget.
        function readField(id) {
          const el = document.getElementById(id);
          if (!el) {
            throw new Error("Template metadata field is missing: " + id);
          }
          if (
            el.nextSibling &&
            el.nextSibling.CodeMirror &&
            typeof el.nextSibling.CodeMirror.getValue === "function"
          ) {
            return el.nextSibling.CodeMirror.getValue();
          }
          return el.value;
        }
        // Authors edit through the enhanced editors before save.
        const enhanced = document.getElementById("additional_head_content");
        enhanced.nextSibling.CodeMirror.setValue("<p>Head edited</p>");
        let missingMessage = null;
        try {
          readField("no_such_field");
        } catch (err) {
          missingMessage = String((err && err.message) || err);
        }
        return {
          enhanced: readField("additional_head_content"),
          second: readField("code_insert_after_body_start"),
          plain: readField("plain_fallback"),
          missingMessage,
        };
      });
      expect(
        read.enhanced,
        "enhanced field must read through the CodeMirror instance (#5626)",
      ).toBe("<p>Head edited</p>");
      expect(
        read.second,
        "second enhanced field must keep its value (#5626)",
      ).toContain("Body");
      expect(
        read.plain,
        "un-enhanced field must fall back to its live value (#5626)",
      ).toContain("Plain");
      expect(
        read.missingMessage,
        "missing field must raise a named error before anything is sent (#5626)",
      ).toContain("Template metadata field is missing: no_such_field");
      await page.screenshot({
        path: "test-results/live-proof-5626-metadata-save-read.png",
      });

      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual(
        [],
      );
    },
  );

  test(
    "CodeMirror stays npm-tracked: no WebJar, no unpack, no version freeze",
    { tag: ["@explorer-content-editor", "@editor"] },
    async () => {
      // No live CMS needed: static build-manifest lock for issue #6363
      // acceptance (no `codemirror` WebJar reference in any pom.xml, no
      // `codemirror-unpack` in the build, freeze lifted, npm-tracked).
      const pomPaths = collectPomPaths(REPO_ROOT);
      expect(
        pomPaths.length,
        "repo-wide pom sweep must find module poms",
      ).toBeGreaterThan(0);
      for (const pomPath of pomPaths) {
        const rel = path.relative(REPO_ROOT, pomPath);
        // Strip XML comments: migration history notes (e.g. "no WebJar",
        // "no codemirror-unpack step") live in comments and must not trip
        // the sweep — only live build elements count.
        const text = fs
          .readFileSync(pomPath, "utf8")
          .replace(/<!--[\s\S]*?-->/g, "");
        expect(
          /<artifactId>\s*codemirror\s*<\/artifactId>/.test(text),
          `${rel} must not declare a codemirror Maven artifact (WebJar removed)`,
        ).toBe(false);
        expect(
          text.includes("codemirror-unpack"),
          `${rel} must not contain a codemirror-unpack build step`,
        ).toBe(false);
        expect(
          /codemirror\.version/.test(text),
          `${rel} must not pin a codemirror.version property (freeze lifted)`,
        ).toBe(false);
      }
      // `codemirror` stays a standard npm dependency (Dependabot-visible via
      // the .github/dependabot.yml `/modules/perc-common-ui-bundle` entry;
      // the `/modules/perc-tinymce` entry retired with that module in #5734)
      // with a lockfile entry, so `npm ls codemirror` / `npm audit` resolve
      // the tracked version.
      for (const manifestRel of NPM_TRACKED_MANIFESTS) {
        const manifestPath = path.join(REPO_ROOT, manifestRel);
        const manifest = JSON.parse(fs.readFileSync(manifestPath, "utf8"));
        const version =
          manifest.dependencies && manifest.dependencies.codemirror;
        expect(
          typeof version === "string" && version.length > 0,
          `${manifestRel} must declare dependencies.codemirror`,
        ).toBe(true);
        const lockPath = path.join(path.dirname(manifestPath), "package-lock.json");
        const lock = JSON.parse(fs.readFileSync(lockPath, "utf8"));
        const packages = lock.packages || {};
        expect(
          Object.prototype.hasOwnProperty.call(
            packages,
            "node_modules/codemirror",
          ),
          `${path.relative(REPO_ROOT, lockPath)} must track node_modules/codemirror`,
        ).toBe(true);
      }
    },
  );

  test(
    "TinyMCE-hosted CodeMirror layout stays detached: no orphaned references",
    { tag: ["@explorer-content-editor", "@editor"] },
    async () => {
      // No live CMS needed: static detach lock for issue #5746 (gate lifted
      // by #5733 + #5734, both merged). The TinyMCE/classic surfaces that
      // pointed at the retired TinyMCE-hosted CodeMirror layout
      // (`sys_resources/tinymce/plugins/codemirror`, deleted with
      // modules/perc-tinymce in #5734) were deleted with their references;
      // every surviving `codemirror.bundle.*` reference resolves to a live
      // consumer (template editor JSP mirrors, sys_TextArea XSL, the npm
      // bundle entry itself) or to test/docs history. This sweep fails fast
      // if a future change re-points a live source at the retired layout.
      const sourcePaths = collectSourcePaths(REPO_ROOT);
      expect(
        sourcePaths.length,
        "repo-wide source sweep must find tracked sources",
      ).toBeGreaterThan(0);
      const offenders = [];
      for (const sourcePath of sourcePaths) {
        const rel = path.relative(REPO_ROOT, sourcePath);
        const text = stripSourceComments(
          fs.readFileSync(sourcePath, "utf8"),
          sourcePath,
        );
        for (const line of text.split("\n")) {
          if (/tinymce\/plugins\/codemirror/.test(line)) {
            offenders.push(`${rel}: ${line.trim()}`);
          }
        }
      }
      expect(
        offenders.slice(0, 20),
        "no live source may reference the retired TinyMCE-hosted CodeMirror path (#5746)",
      ).toEqual([]);
    },
  );
});
