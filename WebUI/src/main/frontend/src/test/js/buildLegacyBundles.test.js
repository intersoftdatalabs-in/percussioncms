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

import { describe, expect, it } from "vitest";
import fs from "node:fs";
import path from "node:path";
import { createRequire } from "node:module";
import { fileURLToPath } from "node:url";

const require = createRequire(import.meta.url);
const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// src/test/js -> frontend root scripts/
const builder = require(path.resolve(
  __dirname,
  "../../../scripts/build-legacy-bundles.js"
));

const {
  WAR_DIR,
  OUTPUT_DIR,
  BUNDLE_CONFIG_DIR,
  REQUIRED_INTERMEDIATE_BUNDLES,
  resolvePath,
  buildBundlesFromConfig,
  assertRequiredIntermediates,
} = builder;

/** Minimum expected size for a real shared-common.js (npm jquery alone is ~300KB). */
const MIN_SHARED_COMMON_BYTES = 100 * 1024;

describe("legacy intermediate bundle builder", () => {
  it("uses src/main/webapp/cm as the source root (portable Path segments)", () => {
    const normalized = WAR_DIR.split(path.sep).join("/");
    expect(normalized.endsWith("src/main/webapp/cm")).toBe(true);
    expect(fs.existsSync(path.join(WAR_DIR, "plugins", "perc_utils.js"))).toBe(
      true
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
      "common-minuet-bundles.json"
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
      "jquery.js"
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
      MIN_SHARED_COMMON_BYTES
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
});
