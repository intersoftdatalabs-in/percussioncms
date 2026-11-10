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
 * Regression tests for the legacy intermediate bundle builder.
 *
 * shared-common.js / shared-common-minuet.js / shared-finder.js (and CSS
 * siblings) are generated into target/generated-webui/cm/ and must not be
 * checked into src/main/webapp/cm/. These tests pin:
 *
 *   1. The source root is webapp/cm (so plugins/, jslib/, services/ resolve).
 *   2. Every phase-1 intermediate source file exists under that root.
 *   3. A real build writes non-trivial intermediate bundles under
 *      target/generated-webui/cm/.
 *
 * Without (1)/(2), the builder used to silently write near-empty
 * concatenations while committed megabyte blobs still shipped in the WAR —
 * the drift that drove CodeQL false positives on the checked-in copies.
 */

import { describe, expect, it, vi } from "vitest";
import fs from "node:fs";
import path from "node:path";
import { createRequire } from "node:module";
import { fileURLToPath } from "node:url";

const require = createRequire(import.meta.url);
const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// src/test/js -> frontend root scripts/ (CommonJS builder stays .cjs under
// package `"type": "module"` so createRequire can load its exports).
const builder = require(
  path.resolve(__dirname, "../../../scripts/build-legacy-bundles.cjs"),
);

const {
  WEBUI_DIR,
  WAR_DIR,
  OUTPUT_DIR,
  BUNDLE_CONFIG_DIR,
  REQUIRED_INTERMEDIATE_BUNDLES,
  NPM_LIBRARY_MAPPINGS,
  STANDALONE_NPM_COPIES,
  resolvePath,
  buildBundlesFromConfig,
  assertRequiredIntermediates,
  syncStandaloneNpmLibraries,
} = builder;

/** Minimum expected size for a real shared-common.js (npm jquery alone is ~300KB). */
const MIN_SHARED_COMMON_BYTES = 100 * 1024;

describe("legacy intermediate bundle builder", () => {
  it("uses src/main/webapp/cm as the source root (portable Path segments)", () => {
    const normalized = WAR_DIR.split(path.sep).join("/");
    expect(normalized.endsWith("src/main/webapp/cm")).toBe(true);
    expect(fs.existsSync(path.join(WAR_DIR, "plugins", "perc_utils.js"))).toBe(
      true,
    );
  });

  it("writes intermediates only under target/generated-webui/cm", () => {
    const normalized = OUTPUT_DIR.split(path.sep).join("/");
    expect(normalized.includes("target/generated-webui/cm")).toBe(true);
  });

  it("resolves every common-bundles.json source under the cm tree", () => {
    const configPath = path.join(BUNDLE_CONFIG_DIR, "common-bundles.json");
    const config = JSON.parse(fs.readFileSync(configPath, "utf8"));
    const missing = [];
    for (const bundle of config.bundles) {
      for (const file of bundle.files) {
        if (file.includes("target/minify-common")) {
          continue;
        }
        const full = resolvePath(file);
        if (!fs.existsSync(full)) {
          missing.push(`${bundle.name}: ${file} -> ${full}`);
        }
      }
    }
    expect(missing, missing.join("\n")).toEqual([]);
  });

  it("resolves every common-minuet-bundles.json source under the cm tree", () => {
    const configPath = path.join(
      BUNDLE_CONFIG_DIR,
      "common-minuet-bundles.json",
    );
    const config = JSON.parse(fs.readFileSync(configPath, "utf8"));
    const missing = [];
    for (const bundle of config.bundles) {
      for (const file of bundle.files) {
        if (file.includes("target/minify-common")) {
          continue;
        }
        const full = resolvePath(file);
        if (!fs.existsSync(full)) {
          missing.push(`${bundle.name}: ${file} -> ${full}`);
        }
      }
    }
    expect(missing, missing.join("\n")).toEqual([]);
  });

  it("builds non-trivial shared-common* / shared-finder intermediates", () => {
    // Integration-style: exercises real resolvePath + concatenation. Writes under
    // target/generated-webui/cm (OUTPUT_DIR), which is gitignored. Requires npm
    // packages for jquery/etc. when WAR_DIR maps to npm; skips if node_modules absent.
    const jquery = path.join(
      path.dirname(path.dirname(path.dirname(__dirname))),
      "node_modules",
      "jquery",
      "dist",
      "jquery.js",
    );
    if (!fs.existsSync(jquery)) {
      // Hermetic CI without frontend node_modules still gets resolution tests above.
      return;
    }

    buildBundlesFromConfig("common-bundles.json", 1, { failOnMissing: true });
    buildBundlesFromConfig("common-minuet-bundles.json", 1, {
      failOnMissing: true,
    });
    assertRequiredIntermediates();

    for (const name of REQUIRED_INTERMEDIATE_BUNDLES) {
      const out = path.join(OUTPUT_DIR, name);
      expect(fs.existsSync(out), out).toBe(true);
      const size = fs.statSync(out).size;
      expect(size, `${name} size ${size}`).toBeGreaterThan(1024);
    }

    const sharedCommon = path.join(OUTPUT_DIR, "shared-common.js");
    expect(fs.statSync(sharedCommon).size).toBeGreaterThan(
      MIN_SHARED_COMMON_BYTES,
    );

    // Perc-written source must appear in the concatenation (not only npm libs).
    const content = fs.readFileSync(sharedCommon, "utf8");
    expect(content).toMatch(/perc_utils|htmlEntities/);
  });

  it("does not require intermediate bundles under the committed webapp tree", () => {
    // Documents the gitignore contract: packaging uses generated-webui only.
    const webappCm = path.join(WAR_DIR);
    for (const name of REQUIRED_INTERMEDIATE_BUNDLES) {
      // If a local leftover exists it is fine; git must not track it.
      // This test only asserts the packaging source of truth is OUTPUT_DIR.
      expect(path.join(OUTPUT_DIR, name)).not.toBe(path.join(webappCm, name));
    }
  });

  it("does not keep standalone npm mins under the committed webapp tree", () => {
    // Regenerable vendor mins live only under target/generated-webui (issue #1510).
    for (const { dest } of STANDALONE_NPM_COPIES) {
      const sourcePath = path.join(WAR_DIR, dest);
      expect(
        fs.existsSync(sourcePath),
        `source must not contain regenerable ${dest}: ${sourcePath}`,
      ).toBe(false);
    }
  });

  it("syncs standalone npm libraries only under target/generated-webui (issue #1510)", () => {
    // Writing into src/main/webapp/cm breaks ai-build-integrity-maven-plugin seals:
    // generate-hashes (validate) then npm-build-legacy rewrites sealed .js files.
    expect(Array.isArray(STANDALONE_NPM_COPIES)).toBe(true);
    expect(STANDALONE_NPM_COPIES.length).toBeGreaterThan(0);

    const nodeModulesRoot = path.join(
      path.dirname(path.dirname(path.dirname(__dirname))),
      "node_modules",
    );
    const jqueryMin = path.join(
      nodeModulesRoot,
      "jquery",
      "dist",
      "jquery.min.js",
    );
    if (!fs.existsSync(jqueryMin)) {
      return;
    }

    // Ensure source tree stays clean before and after sync.
    for (const { dest } of STANDALONE_NPM_COPIES) {
      expect(fs.existsSync(path.join(WAR_DIR, dest))).toBe(false);
    }

    syncStandaloneNpmLibraries();

    for (const { dest } of STANDALONE_NPM_COPIES) {
      const generated = path.join(OUTPUT_DIR, dest);
      expect(fs.existsSync(generated), generated).toBe(true);
      expect(fs.statSync(generated).size).toBeGreaterThan(1000);
      const generatedNorm = generated.split(path.sep).join("/");
      expect(generatedNorm.includes("target/generated-webui/cm")).toBe(true);
      expect(generatedNorm.includes("src/main/webapp")).toBe(false);
      // Source tree must still be free of regenerable mins after the build.
      expect(fs.existsSync(path.join(WAR_DIR, dest))).toBe(false);
    }
  });
});

/**
 * Classic-UI jQuery trio from npm (issue #5660).
 *
 * The vendored cm/jslib copies (jquery-3.6.0.js, jquery-migrate-3.3.2.js,
 * libraries/jquery-ui/*) and the unbuilt `@VERSION` root jquery-ui.js copies
 * were deleted. Classic pages now load the versionless npm-backed mins, synced
 * into target/generated-webui/cm by syncStandaloneNpmLibraries and overlaid
 * onto the WAR at package time. These tests pin:
 *
 *   1. The trio is present in STANDALONE_NPM_COPIES and marked required.
 *   2. Synced bytes are byte-identical to the node_modules dist files
 *      (npm-tracked, Dependabot-visible via frontend package.json).
 *   3. The synced trio carries the expected npm versions
 *      (jQuery 4.x + Migrate 4.x + UI 1.14.x).
 *   4. Bundle-config jslib paths for the trio resolve into node_modules
 *      (NPM_LIBRARY_MAPPINGS), never into deleted vendored files.
 *   5. A missing required npm source fails the build instead of shipping
 *      pages with a 404 where jQuery used to load.
 */
describe("classic-UI jQuery trio from npm (issue #5660)", () => {
  const FRONTEND_ROOT = path.dirname(
    path.dirname(path.dirname(__dirname)),
  );
  const NODE_MODULES = path.join(FRONTEND_ROOT, "node_modules");

  const TRIO = [
    {
      src: "jquery/dist/jquery.min.js",
      dest: "jslib/profiles/3x/jquery/jquery.min.js",
      versionRe: /jQuery v4\./,
    },
    {
      src: "jquery-migrate/dist/jquery-migrate.min.js",
      dest: "jslib/profiles/3x/jquery/jquery-migrate.min.js",
      versionRe: /jQuery Migrate v4\./,
    },
    {
      src: "jquery-ui/dist/jquery-ui.min.js",
      dest: "jslib/profiles/3x/jquery/libraries/jquery-ui/jquery-ui.min.js",
      versionRe: /jQuery UI - v1\.14\./,
    },
  ];

  it("pins the required trio in STANDALONE_NPM_COPIES", () => {
    for (const { src, dest } of TRIO) {
      const entry = STANDALONE_NPM_COPIES.find((e) => e.dest === dest);
      expect(entry, `missing standalone copy for ${dest}`).toBeDefined();
      expect(entry.src).toBe(src);
      expect(entry.required).toBe(true);
    }
  });

  it("syncs trio bytes identical to the npm dist files", () => {
    const missing = TRIO.map(({ src }) => path.join(NODE_MODULES, src)).filter(
      (p) => !fs.existsSync(p),
    );
    if (missing.length > 0) {
      // Hermetic CI without frontend node_modules: presence is pinned above.
      return;
    }
    syncStandaloneNpmLibraries();
    for (const { src, dest, versionRe } of TRIO) {
      const generated = fs.readFileSync(path.join(OUTPUT_DIR, dest));
      const npmDist = fs.readFileSync(path.join(NODE_MODULES, src));
      expect(generated.equals(npmDist), `${dest} differs from npm ${src}`).toBe(
        true,
      );
      expect(generated.toString("utf8", 0, 4096)).toMatch(versionRe);
      // The deleted vendored version must not leak back into the source tree.
      expect(fs.existsSync(path.join(WAR_DIR, dest))).toBe(false);
    }
  });

  it("resolves trio bundle paths into node_modules, not vendored files", () => {
    for (const { dest } of TRIO) {
      expect(
        NPM_LIBRARY_MAPPINGS[dest],
        `no npm mapping for ${dest}`,
      ).toBeDefined();
      const resolved = resolvePath(dest);
      const normalized = resolved.split(path.sep).join("/");
      expect(normalized).toContain("node_modules/");
      expect(fs.existsSync(resolved), resolved).toBe(true);
    }
    // Deleted vendored names must not resolve back into the source tree.
    expect(
      NPM_LIBRARY_MAPPINGS["jslib/profiles/3x/jquery/jquery-3.6.0.js"],
    ).toBeUndefined();
    expect(
      NPM_LIBRARY_MAPPINGS[
        "jslib/profiles/3x/jquery/libraries/jquery-ui/jquery-ui.js"
      ],
    ).toBeUndefined();
  });

  it("fails fast when a required npm source is missing", () => {
    const jquerySrc = path.join(NODE_MODULES, "jquery/dist/jquery.min.js");
    const realExists = fs.existsSync;
    const spy = vi.spyOn(fs, "existsSync").mockImplementation((p) => {
      if (String(p) === jquerySrc) {
        return false;
      }
      return realExists(p);
    });
    try {
      expect(() => syncStandaloneNpmLibraries()).toThrow(
        /syncStandaloneNpmLibraries: npm file not found/,
      );
    } finally {
      spy.mockRestore();
    }
  });
});

/**
/**
 * Classic-UI loader/misc infra from npm (issue #5667).
 *
 * popper.js + bowser es5.js are still loaded directly by classic pages
 * (common_js.jsp x2, contentEdit.xsl) at stable jslib URLs, but the bytes
 * are now the Dependabot-tracked npm dist files
 * (@popperjs/core 2.11.8 + bowser 2.15.0 from frontend/package.json), synced
 * into target/generated-webui/cm by syncStandaloneNpmLibraries. json2.js is
 * dropped outright (native JSON on every baseline browser) and the dead
 * vendored-only trees (mousetrap/, requirejs/, modernizr-custom.js,
 * vendored animate.css) are deleted. These tests pin:
 *
 *   1. The pair is present in STANDALONE_NPM_COPIES and marked required.
 *   2. Synced bytes are byte-identical to the node_modules dist files and
 *      carry the expected npm version markers.
 *   3. Bundle-config jslib paths for the pair resolve into node_modules.
 *   4. json2.js is gone from both bundle configs and has no npm mapping
 *      (dropped, not migrated).
 *   5. Every deleted vendored file resolves to a path that no longer exists
 *      (no silent fallback back into the source tree).
 */
describe("classic-UI loader/misc infra from npm (issue #5667)", () => {
  const FRONTEND_ROOT = path.dirname(
    path.dirname(path.dirname(__dirname)),
  );
  const NODE_MODULES = path.join(FRONTEND_ROOT, "node_modules");

  const PAIR = [
    {
      src: "@popperjs/core/dist/umd/popper.js",
      dest: "jslib/profiles/3x/libraries/popper/popper.js",
      versionRe: /@popperjs\/core v2\.11\./,
    },
    {
      src: "bowser/es5.js",
      dest: "jslib/profiles/3x/libraries/bowser/es5.js",
      versionRe: /lancedikson\/bowser/,
    },
  ];

  // NOTE: popper.js + bowser es5.js are intentionally absent here — they are
  // npm-served (covered by the pair tests above) and resolve into
  // node_modules by design.
  const DELETED_VENDORED = [
    "jslib/profiles/3x/libraries/mousetrap/mousetrap.js",
    "jslib/profiles/3x/libraries/animate.css/animate.css",
    "jslib/profiles/3x/libraries/perc-retiredjs/json2.js",
    "jslib/profiles/3x/libraries/requirejs/require.js",
    "jslib/profiles/3x/libraries/requirejs/init.js",
    "jslib/modernizer/modernizr-custom.js",
  ];

  // Drop set with no npm mapping at all: nothing may resolve them.
  const UNMAPPED_DELETED = [
    "jslib/profiles/3x/libraries/perc-retiredjs/json2.js",
    "jslib/profiles/3x/libraries/requirejs/require.js",
    "jslib/profiles/3x/libraries/requirejs/init.js",
    "jslib/modernizer/modernizr-custom.js",
  ];

  it("pins the required pair in STANDALONE_NPM_COPIES", () => {
    for (const { src, dest } of PAIR) {
      const entry = STANDALONE_NPM_COPIES.find((e) => e.dest === dest);
      expect(entry, `missing standalone copy for ${dest}`).toBeDefined();
      expect(entry.src).toBe(src);
      expect(entry.required).toBe(true);
    }
  });

  it("syncs pair bytes identical to the npm dist files", () => {
    const missing = PAIR.map(({ src }) => path.join(NODE_MODULES, src)).filter(
      (p) => !fs.existsSync(p),
    );
    if (missing.length > 0) {
      // Hermetic CI without frontend node_modules: presence is pinned above.
      return;
    }
    syncStandaloneNpmLibraries();
    for (const { src, dest, versionRe } of PAIR) {
      const generated = fs.readFileSync(path.join(OUTPUT_DIR, dest));
      const npmDist = fs.readFileSync(path.join(NODE_MODULES, src));
      expect(generated.equals(npmDist), `${dest} differs from npm ${src}`).toBe(
        true,
      );
      expect(generated.toString("utf8", 0, 4096)).toMatch(versionRe);
      // The deleted vendored copy must not leak back into the source tree.
      expect(fs.existsSync(path.join(WAR_DIR, dest))).toBe(false);
    }
    // Bowser ships the npm-tracked 2.15.x line (was vendored es5 build).
    const bowserPkg = JSON.parse(
      fs.readFileSync(path.join(NODE_MODULES, "bowser", "package.json"), "utf8"),
    );
    expect(bowserPkg.version).toMatch(/^2\.15\./);
  });

  it("resolves pair bundle paths into node_modules, not vendored files", () => {
    for (const { dest } of PAIR) {
      expect(
        NPM_LIBRARY_MAPPINGS[dest],
        `no npm mapping for ${dest}`,
      ).toBeDefined();
      const resolved = resolvePath(dest);
      const normalized = resolved.split(path.sep).join("/");
      expect(normalized).toContain("node_modules/");
      expect(fs.existsSync(resolved), resolved).toBe(true);
    }
    // The retired modernizr key (nonexistent jslib path / node CLI target)
    // must stay out of the mappings.
    expect(
      NPM_LIBRARY_MAPPINGS["jslib/profiles/3x/libraries/modernizer/modernizr.custom.js"],
    ).toBeUndefined();
  });

  it("drops json2.js from bundles instead of migrating it", () => {
    for (const configFile of ["common-bundles.json", "common-minuet-bundles.json"]) {
      const configPath = path.join(BUNDLE_CONFIG_DIR, configFile);
      const config = JSON.parse(fs.readFileSync(configPath, "utf8"));
      const hits = [];
      for (const bundle of config.bundles) {
        for (const file of bundle.files) {
          if (file.includes("json2")) {
            hits.push(`${bundle.name}: ${file}`);
          }
        }
      }
      expect(hits, `${configFile} still bundles json2:\n${hits.join("\n")}`).toEqual(
        [],
      );
    }
    expect(
      NPM_LIBRARY_MAPPINGS["jslib/profiles/3x/libraries/perc-retiredjs/json2.js"],
    ).toBeUndefined();
  });

  it("deleted vendored files are gone from the source tree", () => {
    for (const dest of DELETED_VENDORED) {
      expect(
        fs.existsSync(path.join(WAR_DIR, dest)),
        `deleted vendored file still in source tree: ${dest}`,
      ).toBe(false);
    }
    // The unmapped drop set must resolve nowhere on disk (no silent
    // fallback back into the source tree).
    for (const dest of UNMAPPED_DELETED) {
      expect(
        NPM_LIBRARY_MAPPINGS[dest],
        `unexpected npm mapping for dropped ${dest}`,
      ).toBeUndefined();
      const resolved = resolvePath(dest);
      expect(
        fs.existsSync(resolved),
        `deleted vendored file still resolves: ${dest} -> ${resolved}`,
      ).toBe(false);
    }
  });
});

/**
 * Classic-UI Handlebars from npm (issue #5666).
 *
 * The vendored classic-UI copies (handlebars-v4.0.12.js — affected by known
 * template-escape CVEs such as CVE-2021-20922, fixed in >= 4.7.7 — and the
 * duplicate handlebars-v4.7.8.js, under cm/jslib, cm/vendor/js/legacy, and
 * the war/ mirror) were deleted. Classic pages now load the stable versioned
 * URL `jslib/profiles/3x/libraries/handlebars/handlebars-v4.7.8.js`, synced
 * from the npm 4.7.10 dist (Dependabot-tracked in frontend package.json)
 * into target/generated-webui/cm by syncStandaloneNpmLibraries and overlaid
 * onto the WAR at package time; the minify bundle configs resolve the same
 * path through NPM_LIBRARY_MAPPINGS. These tests pin:
 *
 *   1. The entry is present in STANDALONE_NPM_COPIES and marked required.
 *   2. Synced bytes are byte-identical to the node_modules dist file.
 *   3. The shipped version is >= 4.7.7 (CVE floor) and matches package.json.
 *   4. The bundle-config jslib path resolves into node_modules, never into
 *      deleted vendored files.
 *   5. No common_js.jsp (webapp or war mirror) still references v4.0.12.
 *   6. Template-rendering parity on the exact dist classic UI ships:
 *      interpolation, #if/#unless, subexpression helpers, and HTML escaping
 *      of double-stash output (the CVE class) vs raw triple-stash.
 */
describe("classic-UI Handlebars from npm (issue #5666)", () => {
  const FRONTEND_ROOT = path.dirname(
    path.dirname(path.dirname(__dirname)),
  );
  const NODE_MODULES = path.join(FRONTEND_ROOT, "node_modules");
  const WEBAPP_CM = WAR_DIR;

  const SRC = "handlebars/dist/handlebars.js";
  const DEST = "jslib/profiles/3x/libraries/handlebars/handlebars-v4.7.8.js";
  const VULNERABLE_DEST =
    "jslib/profiles/3x/libraries/handlebars/handlebars-v4.0.12.js";

  function loadShippedHandlebars() {
    const distPath = path.join(NODE_MODULES, SRC);
    if (!fs.existsSync(distPath)) {
      return null;
    }
    return require(distPath);
  }

  function atLeastMinimumVersion(version, minimum) {
    const parts = String(version).split(".").map(Number);
    const floor = String(minimum).split(".").map(Number);
    for (let i = 0; i < floor.length; i += 1) {
      if (parts[i] !== floor[i]) {
        return parts[i] > floor[i];
      }
    }
    return true;
  }

  it("pins the required standalone copy for the versioned URL", () => {
    const entry = STANDALONE_NPM_COPIES.find((e) => e.dest === DEST);
    expect(entry, `missing standalone copy for ${DEST}`).toBeDefined();
    expect(entry.src).toBe(SRC);
    expect(entry.required).toBe(true);
  });

  it("syncs handlebars bytes identical to the npm dist file", () => {
    const distPath = path.join(NODE_MODULES, SRC);
    if (!fs.existsSync(distPath)) {
      // Hermetic CI without frontend node_modules: presence pinned above.
      return;
    }
    syncStandaloneNpmLibraries();
    const generated = fs.readFileSync(path.join(OUTPUT_DIR, DEST));
    const npmDist = fs.readFileSync(distPath);
    expect(generated.equals(npmDist), `${DEST} differs from npm ${SRC}`).toBe(
      true,
    );
    // The deleted vendored copies must not leak back into the source tree.
    expect(fs.existsSync(path.join(WEBAPP_CM, DEST))).toBe(false);
    expect(fs.existsSync(path.join(WEBAPP_CM, VULNERABLE_DEST))).toBe(false);
    expect(
      fs.existsSync(
        path.join(WEBAPP_CM, "vendor/js/legacy", VULNERABLE_DEST),
      ),
    ).toBe(false);
  });

  it("ships Handlebars >= 4.7.7 matching the npm package version", () => {
    const Handlebars = loadShippedHandlebars();
    if (Handlebars === null) {
      return;
    }
    const pkg = JSON.parse(
      fs.readFileSync(path.join(NODE_MODULES, "handlebars/package.json"), "utf8"),
    );
    expect(Handlebars.VERSION).toBe(pkg.version);
    expect(
      atLeastMinimumVersion(Handlebars.VERSION, "4.7.7"),
      `shipped Handlebars ${Handlebars.VERSION} is below the CVE floor 4.7.7`,
    ).toBe(true);
  });

  it("resolves the bundle-config handlebars path into node_modules", () => {
    expect(NPM_LIBRARY_MAPPINGS[DEST]).toBe(SRC);
    const resolved = resolvePath(DEST);
    const normalized = resolved.split(path.sep).join("/");
    expect(normalized).toContain("node_modules/");
    expect(fs.existsSync(resolved), resolved).toBe(true);
    // The deleted vulnerable name must have no npm mapping of its own.
    expect(NPM_LIBRARY_MAPPINGS[VULNERABLE_DEST]).toBeUndefined();
  });

  it("points every common_js.jsp at v4.7.8, never v4.0.12", () => {
    const jsps = [
      path.join(WEBAPP_CM, "app/includes/common_js.jsp"),
      path.join(WEBAPP_CM, "pages/app/includes/common_js.jsp"),
      path.join(WEBUI_DIR, "war/app/includes/common_js.jsp"),
    ];
    for (const jsp of jsps) {
      const content = fs.readFileSync(jsp, "utf8");
      expect(content, `${jsp} still loads vulnerable v4.0.12`).not.toContain(
        "handlebars-v4.0.12",
      );
      expect(content, `${jsp} must load npm-backed v4.7.8`).toContain(
        "handlebars/handlebars-v4.7.8.js",
      );
    }
  });

  it("renders classic alert-style templates identically (parity)", () => {
    const Handlebars = loadShippedHandlebars();
    if (Handlebars === null) {
      return;
    }
    // Mirrors templateResponseFooterAlert in minuetCommonTemplates:
    // unless-branch, nested path, and escaped interpolation.
    const footer = Handlebars.compile(
      '<div class="alert alert-{{#unless result.warning}}success{{else}}warning{{/unless}}">{{source}}:{{#unless result.warning}}{{result.status}}{{else}}{{result.warning}}{{/unless}}</div>',
    );
    expect(
      footer({ source: "Publish", result: { status: "ok" } }),
    ).toBe('<div class="alert alert-success">Publish:ok</div>');
    expect(
      footer({ source: "Publish", result: { warning: "staged" } }),
    ).toBe('<div class="alert alert-warning">Publish:staged</div>');
    // Mirrors templateFullScreenDialog: {{#if}} block plus a subexpression
    // helper in the classic (validatePropertyValue name 'warning') shape.
    Handlebars.registerHelper("validatePropertyValue", (value, expected) => value === expected);
    const dialog = Handlebars.compile(
      '<div class="{{#if (validatePropertyValue type \'warning\')}}inverse{{else}}branded{{/if}}">{{#if title}}<span>{{title}}</span>{{/if}}</div>',
    );
    expect(dialog({ type: "warning", title: "Hi" })).toBe(
      '<div class="inverse"><span>Hi</span></div>',
    );
    expect(dialog({ type: "info" })).toBe('<div class="branded"></div>');
    Handlebars.unregisterHelper("validatePropertyValue");
  });

  it("escapes double-stash output and passes triple-stash raw (CVE class)", () => {
    const Handlebars = loadShippedHandlebars();
    if (Handlebars === null) {
      return;
    }
    const evil = '<script>alert("x")</script>';
    // {{...}} must HTML-escape attacker markup (the template-escape CVE class
    // fixed in >= 4.7.7); {{{...}}} stays raw for intentional markup.
    expect(Handlebars.compile("{{source}}")({ source: evil })).toBe(
      "&lt;script&gt;alert(&quot;x&quot;)&lt;/script&gt;",
    );
    expect(Handlebars.compile("{{{source}}}")({ source: "<b>ok</b>" })).toBe(
      "<b>ok</b>",
    );
  });
});

/**
 * Classic-UI Moment.js from npm (issue #5665).
 *
 * The vendored Moment 2.29.4 copies (`moment-with-locales.js` under cm/jslib,
 * cm/vendor/js/legacy, and the war/ mirror) and the vendored
 * `moment-jdateformatparser.js` copies in the same trees were deleted.
 * Classic bundles (common-ui-bundle.json, common-minuet-bundles.json) keep
 * loading the stable jslib paths, now resolved from the npm graph
 * (Dependabot-tracked in frontend package.json: moment ^2.31.0,
 * moment-jdateformatparser ^1.2.1) via NPM_LIBRARY_MAPPINGS. These tests pin:
 *
 *   1. The moment + plugin jslib paths resolve into node_modules, never into
 *      deleted vendored files — and the mapping points at the with-locales
 *      dist (delivery views call moment.locale(); the old bare
 *      moment/moment.js mapping carried no locale data).
 *   2. The shipped moment version matches the npm package version.
 *   3. Date formatting/parity on the exact dist classic UI ships: the
 *      `moment(dateString).format("L LTS")` shape from PercCommonMinuetView,
 *      duration humanize, German locale output, and the Java-format
 *      translation helpers (toMomentFormatString/formatWithJDF/toJDFString).
 *   4. No vendored moment/plugin copies remain in any of the three trees,
 *      while simpledateformat.js is explicitly retained (Tim Down's file is
 *      not published on npm — same "not on npm, kept as committed source"
 *      rule as modules/perc-common-ui-bundle).
 */
describe("classic-UI Moment.js from npm (issue #5665)", () => {
  const FRONTEND_ROOT = path.dirname(
    path.dirname(path.dirname(__dirname)),
  );
  const NODE_MODULES = path.join(FRONTEND_ROOT, "node_modules");
  const WEBAPP_CM = WAR_DIR;

  const MOMENT_DEST =
    "jslib/profiles/3x/libraries/momentjs/moment-with-locales.js";
  const MOMENT_SRC = "moment/min/moment-with-locales.js";
  const PLUGIN_DEST =
    "jslib/profiles/3x/libraries/momentjs/plugins/moment-jdateformatparser/moment-jdateformatparser.js";
  const PLUGIN_SRC = "moment-jdateformatparser/moment-jdateformatparser.js";

  function loadShippedMoment() {
    const distPath = path.join(NODE_MODULES, MOMENT_SRC);
    if (!fs.existsSync(distPath)) {
      return null;
    }
    const shipped = require(distPath);
    // Load the npm plugin the way the concatenated classic bundle does: as a
    // plain script against the global moment instance (no bundler, no AMD).
    const pluginSource = fs.readFileSync(
      path.join(NODE_MODULES, PLUGIN_SRC),
      "utf8",
    );
    const sandbox = {
      moment: shipped,
      module: { exports: {} },
      console,
    };
    require("node:vm").createContext(sandbox);
    require("node:vm").runInContext(pluginSource, sandbox);
    return shipped;
  }

  it("resolves the moment + plugin bundle paths into node_modules", () => {
    expect(NPM_LIBRARY_MAPPINGS[MOMENT_DEST]).toBe(MOMENT_SRC);
    expect(NPM_LIBRARY_MAPPINGS[PLUGIN_DEST]).toBe(PLUGIN_SRC);
    for (const dest of [MOMENT_DEST, PLUGIN_DEST]) {
      const resolved = resolvePath(dest);
      const normalized = resolved.split(path.sep).join("/");
      expect(normalized).toContain("node_modules/");
      expect(fs.existsSync(resolved), resolved).toBe(true);
    }
  });

  it("ships moment with locales matching the npm package version", () => {
    const moment = loadShippedMoment();
    if (moment === null) {
      return;
    }
    const pkg = JSON.parse(
      fs.readFileSync(
        path.join(NODE_MODULES, "moment/package.json"),
        "utf8",
      ),
    );
    expect(moment.version).toBe(pkg.version);
    // The with-locales dist (not bare moment/moment.js) must carry locale
    // data: delivery views call moment.locale(locale).
    expect(moment.locales()).toContain("de");
    expect(moment.locales().length).toBeGreaterThan(100);
    const pluginPkg = JSON.parse(
      fs.readFileSync(
        path.join(NODE_MODULES, "moment-jdateformatparser/package.json"),
        "utf8",
      ),
    );
    expect(pluginPkg.version).toBe("1.2.1");
  });

  it("formats classic L LTS dates identically (parity)", () => {
    const moment = loadShippedMoment();
    if (moment === null) {
      return;
    }
    // Mirrors the stringToDate helper in PercCommonMinuetView.
    expect(moment("2026-03-04T15:30:00").format("L LTS")).toBe(
      "03/04/2026 3:30:00 PM",
    );
    // Mirrors the duration helper in the same view.
    expect(moment.duration(90, "seconds").humanize()).toBe("2 minutes");
    // Locale data must actually switch output (de short date).
    expect(moment("2026-03-04").locale("de").format("L")).toBe("04.03.2026");
  });

  it("translates Java date formats via the npm plugin (parity)", () => {
    const moment = loadShippedMoment();
    if (moment === null) {
      return;
    }
    expect(
      moment("2026-03-04").toMomentFormatString("yyyy-MM-dd HH:mm:ss"),
    ).toBe("YYYY-MM-DD HH:mm:ss");
    expect(
      moment("2026-03-04T15:30:00").formatWithJDF("yyyy-MM-dd HH:mm:ss"),
    ).toBe("2026-03-04 15:30:00");
    expect(moment("2026-03-04").toJDFString("YYYY-MM-DD")).toBe("yyyy-MM-dd");
  });

  it("keeps no vendored moment copies while retaining simpledateformat.js", () => {
    // Per-tree relative paths: the vendor mirror hangs directly under
    // profiles/ (no jslib/ prefix); the war mirror roots its own jslib/.
    const deletedByTree = [
      [WEBAPP_CM, MOMENT_DEST],
      [WEBAPP_CM, PLUGIN_DEST],
      [
        path.join(WEBAPP_CM, "vendor/js/legacy"),
        "profiles/3x/libraries/momentjs/moment-with-locales.js",
      ],
      [
        path.join(WEBAPP_CM, "vendor/js/legacy"),
        "profiles/3x/libraries/momentjs/plugins/moment-jdateformatparser/moment-jdateformatparser.js",
      ],
      [path.join(WEBUI_DIR, "war"), MOMENT_DEST],
      [path.join(WEBUI_DIR, "war"), PLUGIN_DEST],
    ];
    for (const [root, rel] of deletedByTree) {
      const candidate = path.join(root, rel);
      expect(
        fs.existsSync(candidate),
        `vendored copy must be gone: ${candidate}`,
      ).toBe(false);
    }
    // simpledateformat.js is not on npm: retained in every tree.
    const retainedByTree = [
      [
        WEBAPP_CM,
        "jslib/profiles/3x/libraries/momentjs/plugins/moment-jdateformatparser/simpledateformat.js",
      ],
      [
        path.join(WEBAPP_CM, "vendor/js/legacy"),
        "profiles/3x/libraries/momentjs/plugins/moment-jdateformatparser/simpledateformat.js",
      ],
      [
        path.join(WEBUI_DIR, "war"),
        "jslib/profiles/3x/libraries/momentjs/plugins/moment-jdateformatparser/simpledateformat.js",
      ],
    ];
    for (const [root, rel] of retainedByTree) {
      const candidate = path.join(root, rel);
      expect(
        fs.existsSync(candidate),
        `simpledateformat.js must be retained: ${candidate}`,
      ).toBe(true);
    }
  });

  it("resolves every minify-config moment entry to an existing file", () => {
    const entries = [
      "jslib/profiles/3x/libraries/momentjs/plugins/moment-jdateformatparser/simpledateformat.js",
      MOMENT_DEST,
      PLUGIN_DEST,
    ];
    for (const entry of entries) {
      const resolved = resolvePath(entry);
      expect(fs.existsSync(resolved), `${entry} -> ${resolved}`).toBe(true);
    }
  });
});
