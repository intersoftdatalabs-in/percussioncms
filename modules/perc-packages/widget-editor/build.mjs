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
 * Build the prebuilt widget-editor bundles (issue #5901).
 *
 * Outputs (committed into both widget packages under
 * `SupportFile-rx_resources/widgets/<form|commentsForm>/js/`):
 * - `perc-tiptap-editor.bundle.min.js` — TipTap vendor IIFE (minified).
 * - `PercWidgetEditor.js` — byte-identical copy of `src/PercWidgetEditor.js`.
 *
 * Usage: `npm run build` (installs pinned deps first via `npm ci`).
 * Requires network access to the npm registry for the first install only;
 * afterwards the committed bundles are used as-is.
 */
import { cpSync, mkdirSync, rmSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { createRequire } from "node:module";

const here = dirname(fileURLToPath(import.meta.url));
const require = createRequire(import.meta.url);
const esbuild = require("esbuild");

const clean = process.argv.includes("--clean");

const packagesRoot = join(here, "..", "src", "main", "resources", "Packages");
const targets = [
  join(packagesRoot, "perc.widget.form", "SupportFile-rx_resources", "widgets", "form", "js"),
  join(
    packagesRoot,
    "perc.widget.commentForm",
    "SupportFile-rx_resources",
    "widgets",
    "commentsForm",
    "js",
  ),
];

if (clean) {
  for (const dir of targets) {
    rmSync(join(dir, "perc-tiptap-editor.bundle.min.js"), { force: true });
  }
  console.log("cleaned committed vendor bundles (controller copies kept as source of review)");
  process.exit(0);
}

const result = await esbuild.build({
  entryPoints: [join(here, "src", "perc-tiptap-vendor.js")],
  bundle: true,
  minify: true,
  format: "iife",
  platform: "browser",
  target: "es2019",
  write: false,
  logLevel: "info",
});

const bundle = result.outputFiles[0].text;
const banner = `/*! perc-tiptap-editor.bundle.min.js — TipTap 3.33.0 vanilla (no-React) vendor bundle for perc.widget.form + perc.widget.commentForm (issue #5901). MIT sources: @tiptap/core, @tiptap/starter-kit, @tiptap/extension-underline. Rebuild: modules/perc-packages/widget-editor (npm run build). */\n`;

for (const dir of targets) {
  mkdirSync(dir, { recursive: true });
  const out = join(dir, "perc-tiptap-editor.bundle.min.js");
  const { writeFileSync } = await import("node:fs");
  writeFileSync(out, banner + bundle);
  cpSync(join(here, "src", "PercWidgetEditor.js"), join(dir, "PercWidgetEditor.js"));
  console.log("wrote", out);
}
console.log("PercWidgetEditor.js mirrored to both widget packages.");
