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
      // Sanity floor only: small CSS assets (e.g. superfish-vertical ~1KB)
      // are legitimate standalone copies (issue #5663).
      expect(fs.statSync(generated).size).toBeGreaterThan(500);
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
 * Classic-UI jQuery plugin cluster from npm (issue #5663).
 *
 * The vendored plugin copies under cm/jslib (mirrored in cm/vendor/js/legacy
 * and WebUI/war) were deleted. Classic pages keep loading the stable jslib
 * URLs, now served from Dependabot-tracked npm dist files
 * (frontend package.json) via syncStandaloneNpmLibraries (direct <script>
 * loads) and NPM_LIBRARY_MAPPINGS (bundle concatenation). Dead copies with
 * no npm upstream and zero references (DataTables FixedHeader, standalone
 * print-this, collapser, war/api/lib ba-bbq/slideto/wiggle, validation
 * locales, jeditable input-type extras, superfish supersubs) were removed
 * outright. These tests pin:
 *
 *   1. Every migrated jslib path has an npm mapping and a required
 *      standalone copy.
 *   2. Synced bytes are byte-identical to the npm dist files and carry the
 *      expected versions (form 4.3.0, validation 1.22.x, jeditable 2.0.x,
 *      superfish 1.7.10, timeago 1.6.7, multiselect 3.0.x, hoverintent 2.2.x).
 *   3. Removed plugin dirs/files resolve nowhere (no mapping, absent from
 *      all three trees).
 */
describe("classic-UI jQuery plugin cluster from npm (issue #5663)", () => {
  const FRONTEND_ROOT = path.dirname(
    path.dirname(path.dirname(__dirname)),
  );
  const NODE_MODULES = path.join(FRONTEND_ROOT, "node_modules");
  const WEBUI_DIR = path.dirname(
    path.dirname(path.dirname(FRONTEND_ROOT)),
  );
  const WAR_MIRROR = path.join(WEBUI_DIR, "war");
  const VENDOR_MIRROR = path.join(WAR_DIR, "vendor/js/legacy");

  const CLUSTER = [
    {
      dest: "jslib/profiles/3x/jquery/plugins/jquery-form/jquery.form.js",
      src: "jquery-form/dist/jquery.form.min.js",
      versionRe: /version: 4\.3\.0/,
    },
    {
      dest: "jslib/profiles/3x/jquery/plugins/jquery-validation/jquery.validate.js",
      src: "jquery-validation/dist/jquery.validate.min.js",
      versionRe: /jQuery Validation Plugin - v1\.22\.1/,
    },
    {
      dest: "jslib/profiles/3x/jquery/plugins/jquery-validation/additional-methods.js",
      src: "jquery-validation/dist/additional-methods.min.js",
      versionRe: /jQuery Validation Plugin - v1\.22\.1/,
    },
    {
      dest: "jslib/profiles/3x/jquery/plugins/jquery-jeditable/jquery.jeditable.js",
      src: "jquery-jeditable/dist/jquery.jeditable.min.js",
      versionRe: /editableAriaShim/,
    },
    {
      dest: "jslib/profiles/3x/jquery/plugins/jquery-superfish/js/superfish.js",
      src: "superfish/dist/js/superfish.min.js",
      versionRe: /Superfish Menu Plugin - v1\.7\.10/,
    },
    {
      dest: "jslib/profiles/3x/jquery/plugins/jquery-superfish/js/hoverIntent.js",
      src: "superfish/dist/js/hoverIntent.js",
      versionRe: /hoverIntent is similar/,
    },
    {
      dest: "jslib/profiles/3x/jquery/plugins/jquery-superfish/css/superfish.css",
      src: "superfish/dist/css/superfish.css",
      versionRe: /\.sf-menu/,
    },
    {
      dest: "jslib/profiles/3x/jquery/plugins/jquery-superfish/css/superfish-vertical.css",
      src: "superfish/dist/css/superfish-vertical.css",
      versionRe: /\.sf-vertical/,
    },
    {
      dest: "jslib/profiles/3x/jquery/plugins/jquery-superfish/css/superfish-navbar.css",
      src: "superfish/dist/css/superfish-navbar.css",
      versionRe: /\.sf-navbar/,
    },
    {
      dest: "jslib/profiles/3x/jquery/plugins/jquery-superfish/css/megafish.css",
      src: "superfish/dist/css/megafish.css",
      versionRe: /\.sf-mega/,
    },
    {
      dest: "jslib/profiles/3x/jquery/plugins/jquery-hoverintent/jquery.hoverIntent.js",
      src: "hoverintent/dist/hoverintent.min.js",
      versionRe: /hoverintent/,
    },
    {
      dest: "jslib/profiles/3x/jquery/plugins/jquery-timeago/jquery.timeago.js",
      src: "timeago/jquery.timeago.js",
      versionRe: /@version 1\.6\.7/,
    },
    {
      dest: "jslib/profiles/3x/jquery/plugins/jquery-ui-multiselect-widget/jquery.multiselect.js",
      src: "jquery-ui-multiselect-widget/src/jquery.multiselect.js",
      versionRe: /MultiSelect Widget 3\.0\.0/,
    },
    {
      dest: "jslib/profiles/3x/jquery/plugins/jquery-ui-multiselect-widget/jquery.multiselect.filter.js",
      src: "jquery-ui-multiselect-widget/src/jquery.multiselect.filter.js",
      versionRe: /multiselectfilter/,
    },
  ];

  // Dead copies removed outright: no npm mapping, absent from every tree.
  const REMOVED_PREFIXES = [
    "jslib/profiles/3x/jquery/plugins/jquery-datatables-fixedheader/",
    "jslib/profiles/3x/jquery/plugins/jquery-print-this/",
    "jslib/profiles/3x/jquery/plugins/jquery-collapser/",
    "jslib/profiles/3x/jquery/plugins/jquery-validation/localization/",
  ];
  const REMOVED_WAR_API_LIBS = [
    "api/lib/jquery.ba-bbq.min.js",
    "api/lib/jquery.slideto.min.js",
    "api/lib/jquery.wiggle.min.js",
  ];

  it("pins every migrated plugin in NPM_LIBRARY_MAPPINGS + STANDALONE_NPM_COPIES", () => {
    for (const { dest, src } of CLUSTER) {
      expect(
        NPM_LIBRARY_MAPPINGS[dest],
        `no npm mapping for ${dest}`,
      ).toBe(src);
      const entry = STANDALONE_NPM_COPIES.find((e) => e.dest === dest);
      expect(entry, `missing standalone copy for ${dest}`).toBeDefined();
      expect(entry.src).toBe(src);
      expect(entry.required).toBe(true);
    }
  });

  it("syncs plugin bytes identical to the npm dist files with expected versions", () => {
    const missing = CLUSTER.map(({ src }) => path.join(NODE_MODULES, src)).filter(
      (p) => !fs.existsSync(p),
    );
    if (missing.length > 0) {
      // Hermetic CI without frontend node_modules: presence is pinned above.
      return;
    }
    syncStandaloneNpmLibraries();
    for (const { dest, src, versionRe } of CLUSTER) {
      const generated = fs.readFileSync(path.join(OUTPUT_DIR, dest));
      const npmDist = fs.readFileSync(path.join(NODE_MODULES, src));
      expect(generated.equals(npmDist), `${dest} differs from npm ${src}`).toBe(
        true,
      );
      expect(generated.toString("utf8", 0, 8192)).toMatch(versionRe);
      // The deleted vendored version must not leak back into the source tree.
      expect(fs.existsSync(path.join(WAR_DIR, dest))).toBe(false);
    }
  });

  it("resolves migrated bundle paths into node_modules, not vendored files", () => {
    for (const { dest } of CLUSTER) {
      const resolved = resolvePath(dest);
      const normalized = resolved.split(path.sep).join("/");
      expect(normalized).toContain("node_modules/");
      expect(fs.existsSync(resolved), resolved).toBe(true);
    }
  });

  it("leaves no mapping or vendored copy for the removed dead plugins", () => {
    for (const prefix of REMOVED_PREFIXES) {
      const mapped = Object.keys(NPM_LIBRARY_MAPPINGS).filter((k) =>
        k.startsWith(prefix),
      );
      expect(mapped, `unexpected npm mapping under ${prefix}`).toEqual([]);
      const standalone = STANDALONE_NPM_COPIES.filter((e) =>
        e.dest.startsWith(prefix),
      );
      expect(standalone, `unexpected standalone copy under ${prefix}`).toEqual(
        [],
      );
      for (const root of [WAR_DIR, VENDOR_MIRROR, path.join(WAR_MIRROR, "jslib")]) {
        expect(
          fs.existsSync(path.join(root, prefix)),
          `vendored copy still present: ${path.join(root, prefix)}`,
        ).toBe(false);
      }
    }
    for (const rel of REMOVED_WAR_API_LIBS) {
      expect(
        fs.existsSync(path.join(WAR_MIRROR, rel)),
        `dead war api lib still present: ${rel}`,
      ).toBe(false);
    }
  });
});
