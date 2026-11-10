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
 * Delivered widget TipTap editor (issues #5901, #5854).
 *
 * Tags: `@editor` `@widget-editor`
 *
 * Run (no live CMS needed — static package files + an offline harness):
 * `npm run test:surface -- --path tests/widget-editor-tiptap.spec.js`
 *
 * Covers the delivered-site migration off frozen TinyMCE 6.8.6 (#5901) and
 * the #5854 retire slice: the `perc.widget.form` + `perc.widget.commentForm`
 * packages must load their rich-text editor from the in-package TipTap
 * triad (`perc-tiptap-editor.bundle.min.js` + `PercWidgetEditor.js` +
 * `TinymceConfig.js` under `rx_resources/widgets/<form|commentsForm>/js/`)
 * with zero `sys_resources/tinymce` dependency, so published forms keep
 * working after the CMS TinyMCE bundle is retired (#5734). The classic
 * `sys_tinymce` control itself stays frozen per the #5874 owner decision
 * and is asserted deprecated, not deleted, here.
 */

const { test, expect } = require("@playwright/test");
const fs = require("fs");
const path = require("path");

const REPO_ROOT = path.join(__dirname, "..", "..", "..", "..");
const FORM_PKG =
  "modules/perc-packages/src/main/resources/Packages/perc.widget.form";
const COMMENT_PKG =
  "modules/perc-packages/src/main/resources/Packages/perc.widget.commentForm";
const FORM_JS = `${FORM_PKG}/SupportFile-rx_resources/widgets/form/js`;
const COMMENT_JS = `${COMMENT_PKG}/SupportFile-rx_resources/widgets/commentsForm/js`;
const FORM_XSL = `${FORM_PKG}/SupportFile-rx_resources/stylesheets/controls/formWidgetControl.xsl`;
const COMMENT_XSL = `${COMMENT_PKG}/sys__UserDependency--rx_resources/stylesheets/controls/commentsFormWidgetControl.xsl`;
const EDITOR_SRC = "modules/perc-packages/widget-editor/src/PercWidgetEditor.js";
const SYS_TEMPLATES =
  "system/cms/content/applications/sys_resources/ApplicationFiles/stylesheets/sys_Templates.xsl";

// The shipped editor triad: vendor bundle + controller + site config.
const TRIAD = [
  "perc-tiptap-editor.bundle.min.js",
  "PercWidgetEditor.js",
  "TinymceConfig.js",
];

function readRepo(rel) {
  return fs.readFileSync(path.join(REPO_ROOT, rel), "utf8").replace(/\r\n/g, "\n");
}

test.describe("Delivered widget TipTap editor (issues #5901, #5854)", () => {
  test(
    "widget packages load the TipTap triad with no sys_resources/tinymce",
    { tag: ["@editor", "@widget-editor"] },
    async () => {
      // No live CMS needed: static shipped-artifact consistency. A stale
      // copy (the #5950 failure mode) or a reintroduced TinyMCE script
      // reference would break published sites after #5734 deletes
      // modules/perc-tinymce.
      for (const xsl of [FORM_XSL, COMMENT_XSL]) {
        const text = readRepo(xsl);
        for (const file of TRIAD) {
          expect(
            text,
            `${xsl} must declare the TipTap triad file: ${file}`,
          ).toContain(`name="${file}"`);
        }
        expect(
          text,
          `${xsl} must not reference the CMS TinyMCE tree`,
        ).not.toMatch(/sys_resources\/tinymce/);
      }

      // Controllers/views/config may keep the `tinymce.EditorManager`
      // facade shape (back-compat contract), but must never load or
      // initialize the frozen client: no bundle URLs, no init calls.
      const jsDirs = [FORM_JS, COMMENT_JS];
      const jsFiles = jsDirs.flatMap((dir) =>
        fs
          .readdirSync(path.join(REPO_ROOT, dir))
          .filter((f) => f.endsWith(".js"))
          .map((f) => `${dir}/${f}`),
      );
      expect(jsFiles.length).toBeGreaterThan(0);
      for (const file of jsFiles) {
        const text = readRepo(file);
        for (const banned of [
          "tinymce.min.js",
          "tinymce_init",
          "/sys_resources/tinymce/",
          "tinymce.init(",
        ]) {
          expect(
            text,
            `${file} must not load the frozen TinyMCE client: ${banned}`,
          ).not.toContain(banned);
        }
      }

      // Stale-copy guard (#5950): both packages ship the controller
      // byte-identical to the widget-editor source of truth.
      const src = readRepo(EDITOR_SRC);
      expect(src).toContain("window.PercWidgetEditor");
      for (const dir of jsDirs) {
        expect(
          readRepo(`${dir}/PercWidgetEditor.js`),
          `${dir}/PercWidgetEditor.js must match ${EDITOR_SRC}`,
        ).toBe(src);
      }
    },
  );

  test(
    "widget editor initializes tinymce-free with zero console errors",
    { tag: ["@editor", "@widget-editor"] },
    async ({ page }) => {
      test.setTimeout(60_000);
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      page.on("console", (msg) => {
        if (msg.type() === "error") {
          pageErrors.push(msg.text());
        }
      });

      // Prove the migrated surface never touches the CMS TinyMCE tree:
      // any such request aborts the run loudly instead of 404ing quietly.
      let tinymceRequests = 0;
      await page.route("**/sys_resources/tinymce/**", (route) => {
        tinymceRequests += 1;
        route.abort("blockedbyclient");
      });

      // Minimal delivered-site shape: the descriptive-text textarea the
      // widget views hand to `PercWidgetEditor.addEditor("elm1")`.
      await page.setContent(
        '<!doctype html><html><head><meta charset="utf-8"></head>' +
          '<body><form><textarea id="elm1"><p>Hello <b>World</b></p></textarea></form></body></html>',
      );

      // Load the shipped artifacts (config first, like a customized
      // delivered site, then the controller — the pending-settings path).
      const pkgFile = (name) => path.join(REPO_ROOT, FORM_JS, name);
      await page.addScriptTag({ path: pkgFile("perc-tiptap-editor.bundle.min.js") });
      await page.addScriptTag({ path: pkgFile("TinymceConfig.js") });
      await page.addScriptTag({ path: pkgFile("PercWidgetEditor.js") });

      const api = await page.evaluate(() => ({
        widgetEditor: typeof window.PercWidgetEditor,
        engine: window.PercWidgetEditor && window.PercWidgetEditor.engine,
        facade: typeof (window.tinymce && window.tinymce.EditorManager),
      }));
      expect(api.widgetEditor).toBe("object");
      expect(api.engine).toBe("tiptap");
      // The back-compat facade keeps customized `tinymce.*` call sites working.
      expect(api.facade).toBe("object");

      const added = await page.evaluate(() =>
        window.PercWidgetEditor.addEditor("elm1"),
      );
      expect(added).toBe(true);

      // Toolbar + editing surface render; the textarea hides but keeps syncing.
      await expect(page.locator(".pwe-toolbar")).toBeVisible();
      await expect(page.locator(".pwe-editor .ProseMirror")).toBeVisible();
      await expect(page.locator("#elm1.pwe-hidden")).toBeHidden();

      // Facade contract (PercFormController.getFieldData shape): content
      // reads through `tinymce.EditorManager.get("elm1")`, writes sync the
      // textarea and mark it dirty for save/submit flows.
      const initial = await page.evaluate(() =>
        window.tinymce.EditorManager.get("elm1").getContent(),
      );
      expect(initial).toContain("Hello");
      await page.evaluate(() =>
        window.tinymce.EditorManager.get("elm1").setContent("<p>TipTap <i>edit</i></p>"),
      );
      const synced = await page.evaluate(() => ({
        value: document.getElementById("elm1").value,
        dirty: document.getElementById("elm1").getAttribute("data-perc-dirty"),
      }));
      expect(synced.value).toContain("TipTap");
      expect(synced.dirty).toBe("true");

      // Direct API parity for new code (TinymceConfig-independent path).
      const direct = await page.evaluate(() => ({
        isActive: window.PercWidgetEditor.isActive("elm1"),
        removed: window.PercWidgetEditor.removeEditor("elm1"),
        inactive: window.PercWidgetEditor.isActive("elm1"),
      }));
      expect(direct.isActive).toBe(true);
      expect(direct.removed).toBe(true);
      expect(direct.inactive).toBe(false);

      expect(
        tinymceRequests,
        "migrated widget editor must make zero sys_resources/tinymce requests",
      ).toBe(0);
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual(
        [],
      );
    },
  );

  test(
    "classic sys_tinymce control stays frozen and is marked deprecated",
    { tag: ["@editor", "@widget-editor"] },
    async () => {
      // No live CMS needed. Per the #5874 owner decision the classic
      // control is frozen (FileDescriptors unchanged) with a deprecation
      // note until classic editing itself retires — assert both halves so
      // neither a silent re-migration nor a dropped note regresses.
      const text = readRepo(SYS_TEMPLATES);
      const meta = text.match(
        /<psxctl:ControlMeta name="sys_tinymce"[\s\S]*?<\/psxctl:AssociatedFileList>/,
      );
      expect(meta, "sys_tinymce ControlMeta must exist").not.toBeNull();
      const block = meta[0];
      expect(block).toMatch(/DEPRECATED in 8\.2/);
      expect(block).toMatch(/TinyMCE 6\.8\.6/);
      // Frozen half: the classic FileDescriptors still point at the
      // CMS-served TinyMCE tree (removed only with classic retirement).
      expect(block).toContain("/sys_resources/tinymce/js/tinymce/tinymce.min.js");
      expect(block).toContain(
        "/sys_resources/tinymce/config/default_config.json",
      );
    },
  );
});
