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
 * Classic-UI Backbone/Underscore from npm, Backgrid retired (issue #5664).
 *
 * The vendored Backbone 1.4.0 (`backbone.js`) and Underscore 1.13.1
 * (`underscore.js` + min + map) copies under cm/jslib, cm/vendor/js/legacy,
 * and the war/ mirror were deleted, as were every vendored Backgrid (~0.3.x)
 * copy (top-level `backgridjs/` trees plus the
 * `profiles/3x/jquery/libraries/backgridjs/` twins in the same three roots).
 * The stale `war/api/lib/{backbone-min,underscore-min}.js` copies went too:
 * that Swagger-UI 2.x viewer was already retired to a redirect (#5672), so
 * the war/api mirror is dead weight, never packaged (the WAR builds from
 * src/main/webapp).
 *
 * Disposition, mirroring the issue acceptance:
 *   1. Backbone/Underscore stay npm-tracked (Dependabot-visible in frontend
 *      package.json: backbone ^1.6.0, underscore ^1.13.7) and the stable
 *      jslib paths keep resolving from the npm graph via
 *      NPM_LIBRARY_MAPPINGS (a repo-wide grep proves no JSP/JS/bundle
 *      references those URLs anymore, so nothing loads them at runtime —
 *      the mappings are resolution continuity, not live loads).
 *   2. Backgrid is retired with rationale, not replaced in place: upstream
 *      EOL ~2017, zero in-repo consumers (only the vendored files
 *      themselves referenced `backgrid`), so there is nothing to migrate.
 *      DataTables (`datatables.net`, already npm-tracked) is the grid going
 *      forward for any future grid work.
 *
 * These tests pin:
 *   1. The backbone + underscore jslib paths resolve into node_modules and
 *      the shipped dists report the npm package versions.
 *   2. Functional parity on the exact dists classic bundles would ship:
 *      underscore `map`/`template`, Backbone Model defaults/set/toJSON and
 *      Collection add/length.
 *   3. No vendored backbone/underscore/backgrid copy remains in any tree.
 *   4. No minify bundle config references the removed paths, and Backgrid
 *      has no npm mapping to resurrect it.
 */
describe("classic-UI Backbone/Underscore from npm, Backgrid retired (issue #5664)", () => {
  const FRONTEND_ROOT = path.dirname(
    path.dirname(path.dirname(__dirname)),
  );
  const NODE_MODULES = path.join(FRONTEND_ROOT, "node_modules");
  const WEBAPP_CM = WAR_DIR;

  const UNDERSCORE_DEST =
    "jslib/profiles/3x/libraries/underscore/underscore.js";
  const UNDERSCORE_SRC = "underscore/underscore.js";
  const BACKBONE_DEST = "jslib/profiles/3x/libraries/backbone/backbone.js";
  const BACKBONE_SRC = "backbone/backbone.js";

  function loadShippedStack() {
    const underscorePath = path.join(NODE_MODULES, UNDERSCORE_SRC);
    const backbonePath = path.join(NODE_MODULES, BACKBONE_SRC);
    if (!fs.existsSync(underscorePath) || !fs.existsSync(backbonePath)) {
      return null;
    }
    const underscore = require(underscorePath);
    const Backbone = require(backbonePath);
    return { underscore, Backbone };
  }

  it("resolves the backbone + underscore bundle paths into node_modules", () => {
    expect(NPM_LIBRARY_MAPPINGS[UNDERSCORE_DEST]).toBe(UNDERSCORE_SRC);
    expect(NPM_LIBRARY_MAPPINGS[BACKBONE_DEST]).toBe(BACKBONE_SRC);
    for (const dest of [UNDERSCORE_DEST, BACKBONE_DEST]) {
      const resolved = resolvePath(dest);
      const normalized = resolved.split(path.sep).join("/");
      expect(normalized).toContain("node_modules/");
      expect(fs.existsSync(resolved), resolved).toBe(true);
    }
  });

  it("ships backbone/underscore versions matching the npm packages", () => {
    const stack = loadShippedStack();
    if (stack === null) {
      return;
    }
    const underscorePkg = JSON.parse(
      fs.readFileSync(
        path.join(NODE_MODULES, "underscore/package.json"),
        "utf8",
      ),
    );
    const backbonePkg = JSON.parse(
      fs.readFileSync(
        path.join(NODE_MODULES, "backbone/package.json"),
        "utf8",
      ),
    );
    expect(stack.underscore.VERSION).toBe(underscorePkg.version);
    expect(stack.Backbone.VERSION).toBe(backbonePkg.version);
  });

  it("keeps underscore collection/template parity on the shipped dist", () => {
    const stack = loadShippedStack();
    if (stack === null) {
      return;
    }
    const { underscore: _ } = stack;
    expect(_.map([1, 2, 3], (x) => x * 2)).toEqual([2, 4, 6]);
    expect(_.template("Hello <%= name %>")({ name: "CM1" })).toBe(
      "Hello CM1",
    );
    expect(_.groupBy([1.3, 2.1, 2.4], (n) => Math.floor(n))).toEqual({
      1: [1.3],
      2: [2.1, 2.4],
    });
  });

  it("keeps Backbone Model/Collection parity on the shipped dist", () => {
    const stack = loadShippedStack();
    if (stack === null) {
      return;
    }
    const { Backbone } = stack;
    const Widget = Backbone.Model.extend({ defaults: { kind: "grid" } });
    const widget = new Widget();
    expect(widget.get("kind")).toBe("grid");
    widget.set("rows", 3);
    expect(widget.get("rows")).toBe(3);
    expect(widget.toJSON()).toEqual({ kind: "grid", rows: 3 });
    const Widgets = Backbone.Collection.extend({ model: Widget });
    const widgets = new Widgets([{ kind: "a" }, { kind: "b" }]);
    expect(widgets.length).toBe(2);
    expect(widgets.at(0).get("kind")).toBe("a");
  });

  it("keeps no vendored backbone/underscore/backgrid copies", () => {
    // Per-tree relative paths: the vendor mirror hangs directly under
    // profiles/ (no jslib/ prefix); the war mirror roots its own jslib/.
    const deletedFiles = [
      [WEBAPP_CM, BACKBONE_DEST],
      [WEBAPP_CM, UNDERSCORE_DEST],
      [
        WEBAPP_CM,
        "jslib/profiles/3x/libraries/underscore/underscore-min.js",
      ],
      [
        WEBAPP_CM,
        "jslib/profiles/3x/libraries/underscore/underscore-min.js.map",
      ],
      [
        path.join(WEBAPP_CM, "vendor/js/legacy"),
        "profiles/3x/libraries/backbone/backbone.js",
      ],
      [
        path.join(WEBAPP_CM, "vendor/js/legacy"),
        "profiles/3x/libraries/underscore/underscore.js",
      ],
      [
        path.join(WEBAPP_CM, "vendor/js/legacy"),
        "profiles/3x/libraries/underscore/underscore-min.js",
      ],
      [
        path.join(WEBAPP_CM, "vendor/js/legacy"),
        "profiles/3x/libraries/underscore/underscore-min.js.map",
      ],
      [path.join(WEBUI_DIR, "war"), BACKBONE_DEST],
      [path.join(WEBUI_DIR, "war"), UNDERSCORE_DEST],
      [
        path.join(WEBUI_DIR, "war"),
        "jslib/profiles/3x/libraries/underscore/underscore-min.js",
      ],
      [
        path.join(WEBUI_DIR, "war"),
        "jslib/profiles/3x/libraries/underscore/underscore-min.js.map",
      ],
      // Stale war/api Swagger-UI 2.x mirror (#5672 retired the viewer):
      // the vendored backbone/underscore copies go with this migration.
      [path.join(WEBUI_DIR, "war"), "api/lib/backbone-min.js"],
      [path.join(WEBUI_DIR, "war"), "api/lib/underscore-min.js"],
    ];
    for (const [root, rel] of deletedFiles) {
      const candidate = path.join(root, rel);
      expect(
        fs.existsSync(candidate),
        `vendored copy must be gone: ${candidate}`,
      ).toBe(false);
    }
    // Backgrid went as whole directories: top-level trees plus the
    // profiles/3x/jquery/libraries twins, in all three roots.
    const deletedDirs = [
      [WEBAPP_CM, "jslib/backgridjs"],
      [WEBAPP_CM, "jslib/profiles/3x/jquery/libraries/backgridjs"],
      [path.join(WEBAPP_CM, "vendor/js/legacy"), "backgridjs"],
      [
        path.join(WEBAPP_CM, "vendor/js/legacy"),
        "profiles/3x/jquery/libraries/backgridjs",
      ],
      [path.join(WEBUI_DIR, "war"), "jslib/backgridjs"],
      [
        path.join(WEBUI_DIR, "war"),
        "jslib/profiles/3x/jquery/libraries/backgridjs",
      ],
    ];
    for (const [root, rel] of deletedDirs) {
      const candidate = path.join(root, rel);
      expect(
        fs.existsSync(candidate),
        `vendored backgrid tree must be gone: ${candidate}`,
      ).toBe(false);
    }
  });

  it("references no removed path from any minify bundle config", () => {
    const configs = [
      "common-bundles.json",
      "common-minuet-bundles.json",
      "static-bundles.json",
      "common-ui-bundle.json",
    ];
    const removedRe = /backbone|underscore|backgrid/i;
    for (const name of configs) {
      const configPath = path.join(BUNDLE_CONFIG_DIR, name);
      const config = JSON.parse(fs.readFileSync(configPath, "utf8"));
      const hits = [];
      for (const bundle of config.bundles) {
        for (const file of bundle.files) {
          if (removedRe.test(file)) {
            hits.push(`${bundle.name}: ${file}`);
          }
        }
      }
      expect(hits, `${name} still references removed libs:\n${hits.join("\n")}`).toEqual(
        [],
      );
    }
    // Backgrid is retired, not npm-mapped: nothing may resurrect it.
    expect(
      Object.keys(NPM_LIBRARY_MAPPINGS).filter((k) => /backgrid/i.test(k)),
      "backgrid must have no npm mapping",
    ).toEqual([]);
  });
});
