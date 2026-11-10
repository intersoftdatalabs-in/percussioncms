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
 * Guard test for issue #5668 (audit #5627): the vendored client-side
 * `lib-js/` tree (jQuery 1.3.2, jquery-treeview, jquery-form,
 * jquery-splitter, jquery-dialog/simplemodal, json2.js, log4javascript)
 * was removed from the p13n-ds module. These libraries are dead upstream
 * and/or predate modern XSS hardening (pre-3.5 jQuery selector/HTML CVE
 * class), and this legacy webapp is not part of the Maven build — so the
 * disposition is removal, not an npm swap.
 *
 * <p>This test fails if anyone reintroduces a vendored pre-3.5 jQuery (or
 * any of the other removed libraries) under this module, or re-adds a JSP
 * reference to the deleted paths.
 *
 * <p>Run with:
 *   node deliverytiersuite/delivery-tier-suite/p13n-ds/src-js/p13n/__tests__/no_vendored_legacy_libs_test.js
 */

"use strict";

const fs = require("fs");
const path = require("path");

// <worktree>/deliverytiersuite/delivery-tier-suite/p13n-ds
const MODULE_DIR = path.join(__dirname, "..", "..", "..");

const REMOVED_DIR_NAMES = ["lib-js"];

const REMOVED_FILE_PATTERNS = [
  /jquery(\.min)?\.js$/i,
  /jquery\.treeview/i,
  /jquery\.cookie\.js$/i,
  /jquery\.form\.js$/i,
  /splitter\.js$/i,
  /simplemodal/i,
  /(^|[\\/])json2\.js$/i,
  /log4javascript/i,
];

const REMOVED_JSP_PATH_FRAGMENTS = [
  "jquery.min.js",
  "jquery-treeview",
  "jquery-form",
  "jquery-splitter",
  "jquery-dialog",
  "log4javascript",
];

const JSP_FILES = [
  path.join(
    MODULE_DIR,
    "src",
    "main",
    "webapp",
    "solution",
    "jsp",
    "scripts.jsp",
  ),
  path.join(
    MODULE_DIR,
    "src",
    "main",
    "webapp",
    "solution",
    "jsp",
    "header.jsp",
  ),
];

function assertRemovedTreeIsGone() {
  for (const name of REMOVED_DIR_NAMES) {
    const dir = path.join(MODULE_DIR, name);
    if (fs.existsSync(dir)) {
      throw new Error(
        `Vendored directory still present: ${dir}. ` +
          "Issue #5668 removed lib-js/ (jQuery 1.3.2 + dead-upstream plugins); " +
          "do not reintroduce vendored copies.",
      );
    }
  }
}

function walkJsFiles(dir, out) {
  if (!fs.existsSync(dir)) {
    return;
  }
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      if (entry.name === "node_modules" || entry.name === "target") {
        continue;
      }
      walkJsFiles(full, out);
    } else if (entry.isFile() && entry.name.endsWith(".js")) {
      out.push(full);
    }
  }
}

function assertNoVendoredLegacyLibs() {
  const roots = [
    path.join(MODULE_DIR, "src-js"),
    path.join(MODULE_DIR, "src", "main"),
  ];
  const files = [];
  for (const root of roots) {
    walkJsFiles(root, files);
  }
  const hits = [];
  for (const file of files) {
    const base = path.basename(file);
    if (
      REMOVED_FILE_PATTERNS.some((pattern) => pattern.test(base)) ||
      REMOVED_FILE_PATTERNS.some((pattern) => pattern.test(file))
    ) {
      // The guard tests themselves live under src-js/.../__tests__/ and
      // their filenames contain no removed patterns, so any hit here is a
      // reintroduced vendored library.
      hits.push(path.relative(MODULE_DIR, file));
      continue;
    }
    // Also catch a vendored jQuery by its version banner regardless of
    // filename (e.g. renamed copies): jQuery < 3.5 is out of bounds.
    if (base.endsWith(".min.js") || base.toLowerCase().includes("jquery")) {
      const head = fs.readFileSync(file, "utf8").slice(0, 2000);
      const versionMatch = head.match(/jQuery JavaScript Library v(\d+)\.(\d+)/);
      if (versionMatch) {
        const major = Number(versionMatch[1]);
        const minor = Number(versionMatch[2]);
        if (major < 3 || (major === 3 && minor < 5)) {
          hits.push(`${path.relative(MODULE_DIR, file)} (jQuery banner v${major}.${minor})`);
        }
      }
    }
  }
  if (hits.length > 0) {
    throw new Error(
      "Vendored legacy client libraries found under p13n-ds:\n  " +
        hits.join("\n  "),
    );
  }
}

function assertNoDanglingJspReferences() {
  const hits = [];
  for (const jsp of JSP_FILES) {
    if (!fs.existsSync(jsp)) {
      continue;
    }
    const content = fs.readFileSync(jsp, "utf8");
    // Strip JSP comments (<%-- ... --%>) first: removal notes in scripts.jsp
    // and header.jsp legitimately name the deleted libraries.
    const codeOnly = content.replace(/<%--[\s\S]*?--%>/g, "");
    for (const fragment of REMOVED_JSP_PATH_FRAGMENTS) {
      if (codeOnly.includes(fragment)) {
        hits.push(`${path.relative(MODULE_DIR, jsp)} references "${fragment}"`);
      }
    }
  }
  if (hits.length > 0) {
    throw new Error("JSP references to removed lib-js paths:\n  " + hits.join("\n  "));
  }
}

function runAllTests() {
  const tests = [
    ["Vendored lib-js/ tree is gone", assertRemovedTreeIsGone],
    ["No vendored legacy libs under p13n-ds", assertNoVendoredLegacyLibs],
    ["No JSP references to removed lib-js paths", assertNoDanglingJspReferences],
  ];

  let passed = 0;
  let failed = 0;
  for (const [name, fn] of tests) {
    try {
      fn();
      console.log(`PASS  ${name}`);
      passed++;
    } catch (e) {
      console.error(`FAIL  ${name}: ${e.message}`);
      failed++;
    }
  }
  console.log(`\nResult: ${passed} passed, ${failed} failed`);
  process.exit(failed === 0 ? 0 : 1);
}

runAllTests();
