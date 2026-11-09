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
 * bundle-codemirror.mjs
 *
 * Bundles the npm-managed CodeMirror 5 package into a single IIFE script +
 * stylesheet for the classic template editor and the `sys_TextArea`
 * code-editor control (issue #5784).
 *
 * Relocated here from `modules/perc-tinymce` (issue #5626 origin) so the
 * bundle is served from the TinyMCE-independent path
 * `/sys_resources/codemirror/codemirror/` and survives the `perc-tinymce`
 * packaging removal (#5734). Until #5734 lands, `modules/perc-tinymce` keeps
 * building the same file set under its legacy
 * `sys_resources/tinymce/plugins/codemirror/codemirror/` path as a fallback;
 * consumers point at the new path.
 *
 * Source: node_modules/codemirror/** (via codemirror-bundle-entry.js)
 * Output: target/classes/META-INF/resources/sys_resources/codemirror/codemirror/
 *         - codemirror.bundle.js  (IIFE, exposes window.CodeMirror)
 *         - codemirror.bundle.css
 *
 * Invoked by the Maven build via frontend-maven-plugin at the prepare-package
 * phase (`npm run bundle:codemirror`), after the regular `bundle` step.
 */

import { build } from "esbuild";
import { existsSync, mkdirSync, readFileSync, statSync } from "node:fs";
import { createRequire } from "node:module";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { CODEMIRROR_BUNDLE_MARKERS } from "./codemirror-helpers.mjs";

const here = dirname(fileURLToPath(import.meta.url));
const moduleDir = dirname(here);
const outDir = join(
  moduleDir,
  "target/classes/META-INF/resources/sys_resources/codemirror/codemirror",
);

const require = createRequire(join(moduleDir, "package.json"));
const codemirrorPkg = require("codemirror/package.json");
console.log(`Bundling CodeMirror ${codemirrorPkg.version} (MIT) from npm...`);

mkdirSync(outDir, { recursive: true });

await build({
  entryPoints: [join(here, "codemirror-bundle-entry.js")],
  outfile: join(outDir, "codemirror.bundle.js"),
  bundle: true,
  minify: true,
  // Classic IIFE script. `globalName` is intentionally NOT set: the entry
  // assigns `window.CodeMirror` itself, and esbuild's globalName footer
  // would overwrite it with the entry's export namespace (`{ default: fn }`)
  // after the entry body runs.
  format: "iife",
  target: ["es2019"],
  logLevel: "warning",
});

for (const name of ["codemirror.bundle.js", "codemirror.bundle.css"]) {
  const path = join(outDir, name);
  if (!existsSync(path)) {
    console.error(`ERROR: expected bundle output missing: ${path}`);
    process.exit(1);
  }
  const size = statSync(path).size;
  if (size === 0) {
    console.error(`ERROR: bundle output is empty: ${path}`);
    process.exit(1);
  }
  console.log(`  ✓ codemirror/${name} (${size} bytes)`);
}

// Sanity check: the bundle must still expose the CodeMirror 5 API surface
// the consumers rely on (global constructor + htmlmixed mode + search).
const bundleJs = readFileSync(join(outDir, "codemirror.bundle.js"), "utf8");
for (const marker of CODEMIRROR_BUNDLE_MARKERS) {
  if (!bundleJs.includes(marker)) {
    console.error(
      `ERROR: bundle is missing expected CodeMirror 5 marker: ${marker}`,
    );
    process.exit(1);
  }
}

console.log("Done.");
