#!/usr/bin/env node
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
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * bundle.mjs — Build perc_common_ui.js and perc_common_ui_slim.js using esbuild.
 *
 * These files are concatenated (in order) and then minified via esbuild's
 * transform API. The output is placed in
 * target/classes/META-INF/resources/cm/common/js/ so that the Maven JAR
 * packaging picks them up and the servlet container serves them as
 * /cm/common/js/perc_common_ui.js from the host WAR.
 */

import * as esbuild from "esbuild";
import * as fs from "fs";
import * as path from "path";
import { fileURLToPath } from "url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const ROOT = path.resolve(__dirname, "..");

// ---------------------------------------------------------------------------
// Output directory (Maven puts target/classes into the JAR)
// ---------------------------------------------------------------------------
const OUT_DIR = path.join(
  ROOT,
  "target",
  "classes",
  "META-INF",
  "resources",
  "cm",
  "common",
  "js",
);
fs.mkdirSync(OUT_DIR, { recursive: true });

// ---------------------------------------------------------------------------
// Helper: read and concatenate source files
// ---------------------------------------------------------------------------
function concat(files) {
  return files
    .map((f) => {
      const absPath = path.resolve(ROOT, f);
      if (!fs.existsSync(absPath)) {
        throw new Error(`Source file not found: ${absPath}`);
      }
      return fs.readFileSync(absPath, "utf8");
    })
    .join("\n");
}

// ---------------------------------------------------------------------------
// Helper: minify a concatenated source string via esbuild transform
// ---------------------------------------------------------------------------
async function minify(source, outFile) {
  const result = await esbuild.transform(source, {
    minify: true,
    // Target a broad set of browsers – these widgets run on public-facing pages
    target: ["es2015", "chrome58", "firefox57", "safari11"],
    loader: "js",
  });
  const dest = path.join(OUT_DIR, outFile);
  fs.writeFileSync(dest, result.code, "utf8");
  const kb = (result.code.length / 1024).toFixed(1);
  console.log(`  wrote ${outFile} (${kb} KB)`);
}

// ---------------------------------------------------------------------------
// perc_common_ui.js — full bundle
//
// File order matches the interim antrun concat that was previously in WebUI/pom.xml
// (which in turn matched the original common-ui-bundle.json recovered from git).
// PercServiceUtils.js is prepended to services as a shared utility.
// ---------------------------------------------------------------------------
const FULL_BUNDLE = [
  // Vendor libraries (not on npm — kept as committed source)
  "src/main/js/vendor/jquery.jsonp.js",
  "src/main/js/vendor/simpledateformat.js",
  "src/main/js/vendor/jquery.timeago.js",

  // Vendor libraries pulled from npm devDependencies (use unminified UMD builds
  // so esbuild controls the final minification consistently across all sources)
  "node_modules/moment/min/moment-with-locales.js",
  "node_modules/moment-jdateformatparser/moment-jdateformatparser.js",

  // Shims replacing retired libraries (jquery-bbq, jquery.jfeed)
  "src/main/js/shims/bbq-shim.js",
  "src/main/js/shims/jfeed-shim.js",

  // Perc plugins
  "src/main/js/plugins/PercResultsPaging.js",

  // Perc services — PercServiceUtils first (dependency of the other services)
  "src/main/js/services/PercServiceUtils.js",
  "src/main/js/services/PercPageListService.js",
  "src/main/js/services/PercBlogListService.js",
  "src/main/js/services/PercLikedService.js",
  "src/main/js/services/PercTagListService.js",
  "src/main/js/services/PercCategoryListService.js",
  "src/main/js/services/PercResultService.js",
  "src/main/js/services/PercArchiveListService.js",
  "src/main/js/services/PercBlogPostService.js",
  "src/main/js/services/PercCookieConsentService.js",
  "src/main/js/services/PercRssService.js",
  "src/main/js/services/PercMembershipService.js",
  "src/main/js/services/PercMostReadBlogPostsService.js",

  // Perc views
  "src/main/js/views/PercPageListView.js",
  "src/main/js/views/PercBlogListView.js",
  "src/main/js/views/PercMostReadBlogPostsView.js",
  "src/main/js/views/PercLikedView.js",
  "src/main/js/views/PercCommentsView.js",
  "src/main/js/views/PercTagListView.js",
  "src/main/js/views/PercCategoryListView.js",
  "src/main/js/views/PercArchiveListView.js",
  "src/main/js/views/PercResultView.js",
  "src/main/js/views/PercBlogPostView.js",
  "src/main/js/views/PercRssView.js",
  "src/main/js/views/PercRegistrationView.js",
  "src/main/js/views/PercLoginView.js",
  "src/main/js/views/PercSecureLoginView.js",
  "src/main/js/views/PercProtectedRegion.js",
];

// ---------------------------------------------------------------------------
// perc_common_ui_slim.js — lightweight bundle for delivery-tier pages
//
// Only the subset of vendor utilities + the common service utilities.
// ---------------------------------------------------------------------------
const SLIM_BUNDLE = [
  "src/main/js/vendor/jquery.jsonp.js",
  "src/main/js/services/PercServiceUtils.js",
];

// ---------------------------------------------------------------------------
// Main
// ---------------------------------------------------------------------------
console.log("Building perc-common-ui-bundle...");

try {
  await minify(concat(FULL_BUNDLE), "perc_common_ui.js");
  await minify(concat(SLIM_BUNDLE), "perc_common_ui_slim.js");
  console.log("Done.");
} catch (err) {
  console.error("Build failed:", err.message);
  process.exit(1);
}
