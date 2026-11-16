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
const NPM_TRACKED_MANIFESTS = [
  "modules/perc-tinymce/package.json",
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
      // .github/dependabot.yml `/modules/perc-tinymce` +
      // `/modules/perc-common-ui-bundle` entries) with a lockfile entry, so
      // `npm ls codemirror` / `npm audit` resolve the tracked version.
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
});
