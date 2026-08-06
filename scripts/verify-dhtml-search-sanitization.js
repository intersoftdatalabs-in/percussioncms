#!/usr/bin/env node
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
 * Regression check for the 7 lockstep copies of system/Docs/*\/dhtml_search.js.
 *
 * Closes 21 GitHub CodeQL alerts (js/incomplete-sanitization, 3 per
 * file x 7 files): each copy escaped a user-supplied search term with
 *   while (SearchWord.indexOf("<") > -1 || ... ) {
 *     SearchWord = SearchWord.replace("<", "&lt;")...
 *   }
 * -- a non-global .replace(string, ...) call re-run in a while loop.
 * The loop happens to converge to a fully-escaped result for every
 * input tested here (it is not a demonstrated live XSS), but it is
 * exactly the anti-pattern CodeQL's rule targets: a single edit that
 * removes the while loop (easy to do without realizing it is
 * load-bearing) silently reintroduces an incomplete-escaping bug, and
 * the loop is worst-case O(n^2) for a long search term. Each copy was
 * fixed to a single-pass global-regex replace instead.
 *
 * This script is plain Node (no test framework, no npm dependency) so
 * it runs identically on every platform without needing a paired
 * .bat/.sh wrapper (unlike this directory's shell-based verify-*.sh
 * scripts) -- appropriate here since the `system` Java module has no
 * existing JS test toolchain to hook into for 7 standalone legacy
 * static help-search files.
 *
 * For each file, extracts the real, checked-in escaping expression via
 * a narrow regex (not a broad content grep) and evals just that
 * expression as a function, then drives it with inputs covering
 * multiple/mixed special characters, matching what a real search box
 * user could type.
 *
 * Usage: node scripts/verify-dhtml-search-sanitization.js
 * Exit code 0 if every file passes every case, 1 otherwise.
 */
const fs = require("fs");
const path = require("path");

const REPO_ROOT = path.resolve(__dirname, "..");
const FILES = [
  "system/Docs/Active_Assembly_Interface/dhtml_search.js",
  "system/Docs/Active_Assembly_Tutorial/dhtml_search.js",
  "system/Docs/Percussion_Package_Manager_Help/dhtml_search.js",
  "system/Docs/Rhythmyx_Administration_Tab_Help/dhtml_search.js",
  "system/Docs/Rhythmyx_Publishing_Design_Help/dhtml_search.js",
  "system/Docs/Rhythmyx_Publishing_Runtime_Help/dhtml_search.js",
  "system/Docs/Rhythmyx_Workflow_Tab_Help/dhtml_search.js",
];

const CASES = [
  { input: "hello world", expected: "hello world" },
  { input: "<script>", expected: "&lt;script&gt;" },
  {
    input: "<script>alert(1)</script>",
    expected: "&lt;script&gt;alert(1)&lt;/script&gt;",
  },
  { input: '"quoted"', expected: "&quot;quoted&quot;" },
  { input: "<<<", expected: "&lt;&lt;&lt;" },
  { input: ">>>", expected: "&gt;&gt;&gt;" },
  { input: '"""', expected: "&quot;&quot;&quot;" },
  {
    input: '<img src=x onerror="alert(1)">',
    expected: "&lt;img src=x onerror=&quot;alert(1)&quot;&gt;",
  },
  {
    input: '>>><<<"""',
    expected: "&gt;&gt;&gt;&lt;&lt;&lt;&quot;&quot;&quot;",
  },
];

function extractEscapeFn(src, relPath) {
  const match =
    /SearchWord = SearchWord\.replace\(\/<\/g, "&lt;"\)\s*\n\s*\.replace\(\/>\/g, "&gt;"\)\s*\n\s*\.replace\(\/"\/g, "&quot;"\);/.exec(
      src,
    );
  if (!match) {
    throw new Error(
      `${relPath}: fixed escaping expression not found -- has the ` +
        `js/incomplete-sanitization fix been reverted or reformatted?`,
    );
  }
  // eslint-disable-next-line no-new-func
  return new Function("SearchWord", match[0] + "\nreturn SearchWord;");
}

let failures = 0;

for (const relPath of FILES) {
  const absPath = path.join(REPO_ROOT, relPath);
  let src;
  try {
    src = fs.readFileSync(absPath, "utf8");
  } catch (e) {
    console.error(`FAIL: could not read ${relPath}: ${e.message}`);
    failures++;
    continue;
  }

  // Guard against the old, vulnerable non-global .replace() pattern
  // reappearing (e.g. from a merge or a manual "revert this weird
  // regex" edit) even if the extraction regex above still matches
  // something else.
  if (/SearchWord\.replace\("<", "&lt;"\)/.test(src)) {
    console.error(
      `FAIL: ${relPath} still contains the old non-global ` +
        `SearchWord.replace("<", ...) pattern`,
    );
    failures++;
    continue;
  }

  let escapeFn;
  try {
    escapeFn = extractEscapeFn(src, relPath);
  } catch (e) {
    console.error(`FAIL: ${e.message}`);
    failures++;
    continue;
  }

  let fileOk = true;
  for (const { input, expected } of CASES) {
    let actual;
    try {
      actual = escapeFn(input);
    } catch (e) {
      console.error(
        `FAIL: ${relPath}: escaping threw for input ${JSON.stringify(input)}: ${
          e.message
        }`,
      );
      fileOk = false;
      failures++;
      continue;
    }
    if (actual !== expected) {
      console.error(
        `FAIL: ${relPath}: input ${JSON.stringify(input)} -> ` +
          `${JSON.stringify(actual)}, expected ${JSON.stringify(expected)}`,
      );
      fileOk = false;
      failures++;
    }
  }
  if (fileOk) {
    console.log(`OK: ${relPath} (${CASES.length} cases)`);
  }
}

if (failures > 0) {
  console.error(`verify-dhtml-search-sanitization: FAIL (${failures})`);
  process.exit(1);
}
console.log("verify-dhtml-search-sanitization: PASS");
process.exit(0);
