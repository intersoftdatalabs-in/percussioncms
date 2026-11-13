#!/usr/bin/env node

/*
 * Syncs the npm-tracked third-party JS shipped in the CMS installer payload.
 *
 * Source of truth: modules/perc-toolkit/package.json pins
 *   (jcrop-0.9.12, jquery-color, jquery-form). Destinations:
 *   InstallDir/rx_resources/js and
 *   InstallDir/rx_resources/addins/psoimageeditor/js, assembled verbatim into
 *   the dist by perc-distribution-tree.
 *
 * Usage:
 *   cd modules/perc-toolkit && npm install && npm run sync
 *
 * Portability: pure node (path.join, no shell-outs) — runs on Windows,
 * Linux, and macOS. See scripts/README.md.
 */

const fs = require("fs");
const path = require("path");

const MODULE_DIR = path.dirname(__dirname);
const NODE_MODULES_DIR = path.join(MODULE_DIR, "node_modules");
const RESOURCES_DIR = path.join(MODULE_DIR, "src", "main", "resources", "InstallDir");
const PAYLOAD_JS_DIR = path.join(RESOURCES_DIR, "rx_resources", "js");
const PAYLOAD_ADDIN_JS_DIR = path.join(
  RESOURCES_DIR,
  "rx_resources",
  "addins",
  "psoimageeditor",
  "js",
);

const COLOR_PROVENANCE_BANNER = `/*!
 * Percussion CMS installer payload copy — DO NOT EDIT BY HAND.
 * Source: npm package \`jquery-color@3.0.0\` (MIT), file \`dist/jquery.color.js\`.
 * Refresh with \`npm run sync\` in modules/perc-toolkit after any pin change.
 * Issue #6273 (audit #6069, parent epic #5627).
 */
`;

const FORM_PROVENANCE_BANNER = `/*!
 * Percussion CMS installer payload copy — DO NOT EDIT BY HAND.
 * Source: npm package \`jquery-form@4.5.0\` (LGPL-2.1+ OR MIT — MIT option), file \`dist/jquery.form.js\`.
 * Refresh with \`npm run sync\` in modules/perc-toolkit after any pin change.
 * Issue #6274 (audit #6069, parent epic #5627).
 */
`;

// Jcrop ships byte-identical (banner preserved from the npm tarball); the
// color and form libraries get a provenance banner prepended so the payload
// records their npm origin. The InstallerPayloadVendoredJsAuditTest pins the
// banners. The payload keeps the historic `form.js` file name so deployed
// editor pages keep working; only the bytes are now npm-sourced (#6274).
const COPIES = [
  {
    src: ["jcrop-0.9.12", "js", "jquery.Jcrop.min.js"],
    dir: PAYLOAD_JS_DIR,
    dest: "jquery.Jcrop.min.js",
    banner: null,
  },
  {
    src: ["jquery-color", "dist", "jquery.color.js"],
    dir: PAYLOAD_JS_DIR,
    dest: "jquery.color.js",
    banner: COLOR_PROVENANCE_BANNER,
  },
  {
    src: ["jquery-form", "dist", "jquery.form.js"],
    dir: PAYLOAD_ADDIN_JS_DIR,
    dest: "form.js",
    banner: FORM_PROVENANCE_BANNER,
  },
];

function fail(message) {
  console.error(`sync-installer-vendored-js: ${message}`);
  process.exit(1);
}

if (!fs.existsSync(NODE_MODULES_DIR)) {
  fail(
    `node_modules not found under ${MODULE_DIR}; run \`npm install\` in modules/perc-toolkit first.`,
  );
}

let updated = 0;
for (const copy of COPIES) {
  const src = path.join(NODE_MODULES_DIR, ...copy.src);
  const dest = path.join(copy.dir, copy.dest);
  if (!fs.existsSync(src)) {
    fail(`npm source missing: ${src}. Is the package.json pin installed?`);
  }
  const body = fs.readFileSync(src, "utf8");
  const next = (copy.banner || "") + body;
  const prev = fs.existsSync(dest) ? fs.readFileSync(dest, "utf8") : null;
  if (prev !== next) {
    fs.writeFileSync(dest, next);
    console.log(`updated ${copy.dest}`);
    updated += 1;
  } else {
    console.log(`in sync: ${copy.dest}`);
  }
}

console.log(
  updated === 0
    ? "payload JS already in sync with package.json pins."
    : `${updated} payload file(s) refreshed from npm.`,
);
