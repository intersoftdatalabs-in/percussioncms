#!/usr/bin/env node

/**
 * Legacy Bundle Builder
 * Concatenates JavaScript and CSS files according to the minify JSON bundle configs.
 * This replaces the functionality of the removed minify-maven-plugin.
 */

const fs = require("fs");
const path = require("path");

// Paths are relative to the WebUI directory (4 levels up from scripts/)
// scripts/ -> src/main/frontend/ -> src/main/ -> src/ -> WebUI/
const WEBUI_DIR = path.dirname(
  path.dirname(path.dirname(path.dirname(__dirname))),
);
// Legacy source tree lives under cm/ (plugins, jslib, services, css, …).
// Bundle configs list paths relative to that root (e.g. plugins/perc_utils.js).
// Served as /cm/ — do NOT use src/main/webapp alone (WAR root without those trees).
const WAR_DIR = path.join(WEBUI_DIR, "src/main/webapp", "cm");
const BUNDLE_CONFIG_DIR = path.join(WEBUI_DIR, "src/main/resources/minify");
// Generated only — never commit these. Maven war plugin overlays this dir.
const OUTPUT_DIR = path.join(WEBUI_DIR, "target/generated-webui/cm");
const NODE_MODULES_DIR = path.join(__dirname, "../node_modules");
// Delivery-tier common UI sources used by perc_common_ui.js (historically
// referenced as ../../delivery/common/js/... from WebUI/war/).
const DELIVERY_COMMON_JS_DIR = path.join(
  WEBUI_DIR,
  "..",
  "modules",
  "perc-common-ui-bundle",
  "src",
  "main",
  "js",
);

/** Intermediate bundles that must not be empty (phase-1 outputs). */
const REQUIRED_INTERMEDIATE_BUNDLES = [
  "shared-common.js",
  "shared-common-minuet.js",
  "shared-finder.js",
  "shared-common.css",
  "shared-common-minuet.css",
];

// Mapping of jslib paths to npm package names for npm-managed libraries.
// Classic-UI direct <script> loads use the npm-backed files
// (jquery.min.js / jquery-migrate.min.js / jquery-ui.min.js from issue #5660;
// popper.js / bowser es5.js from issue #5667), synced into
// target/generated-webui by STANDALONE_NPM_COPIES below.
const NPM_LIBRARY_MAPPINGS = {
  "jslib/profiles/3x/jquery/jquery.min.js": "jquery/dist/jquery.min.js",
  "jslib/profiles/3x/jquery/jquery-migrate.min.js":
    "jquery-migrate/dist/jquery-migrate.min.js",
  "jslib/profiles/3x/jquery/libraries/jquery-ui/jquery-ui.min.js":
    "jquery-ui/dist/jquery-ui.min.js",
  "jslib/profiles/3x/libraries/bootstrap/js/bootstrap.bundle.js":
    "bootstrap/dist/js/bootstrap.bundle.js",
  "jslib/profiles/3x/libraries/bootstrap/css/bootstrap.css":
    "bootstrap/dist/css/bootstrap.css",
  "jslib/profiles/3x/libraries/bowser/es5.js": "bowser/es5.js",
  "jslib/profiles/3x/libraries/handlebars/handlebars-v4.7.8.js":
    "handlebars/dist/handlebars.js",
  "jslib/profiles/3x/libraries/popper/popper.js":
    "@popperjs/core/dist/umd/popper.js",
  // Classic-UI Moment.js (issue #5665): the vendored 2.29.4 copies were
  // deleted. Bundles resolve the stable jslib path from npm moment 2.31.0
  // (Dependabot-tracked). The with-locales dist preserves the locale data
  // the vendored file carried (delivery views call moment.locale()).
  // simpledateformat.js stays vendored: Tim Down's file is not on npm.
  "jslib/profiles/3x/libraries/momentjs/moment-with-locales.js":
    "moment/min/moment-with-locales.js",
  "jslib/profiles/3x/libraries/momentjs/plugins/moment-jdateformatparser/moment-jdateformatparser.js":
    "moment-jdateformatparser/moment-jdateformatparser.js",
  // Classic-UI MVC stack (issue #5664): the vendored Backbone 1.4.0 and
  // Underscore 1.13.1 copies were deleted. Bundles keep resolving the stable
  // jslib paths from the npm graph (Dependabot-tracked: backbone ^1.6.0,
  // underscore ^1.13.7). Backgrid (~0.3.x, EOL ~2017) had zero in-repo
  // consumers, so it is retired with no mapping; DataTables
  // (datatables.net, already npm-tracked) is the grid going forward.
  "jslib/profiles/3x/libraries/underscore/underscore.js":
    "underscore/underscore.js",
  "jslib/profiles/3x/libraries/backbone/backbone.js": "backbone/backbone.js",
  "jslib/profiles/3x/libraries/qunit/qunit.js": "qunit/qunit/qunit.js",
  "jslib/profiles/3x/libraries/mousetrap/mousetrap.js":
    "mousetrap/mousetrap.js",
  // Retired with issue #5667 (no Modernizr consumers remain in classic UI;
  // the old key pointed at a non-existent jslib path / node CLI entry).
  "jslib/profiles/3x/libraries/animate.css/animate.css":
    "animate.css/animate.css",
  // jQuery plugin cluster from npm (issue #5663). The vendored
  // cm/jslib copies were deleted; classic pages keep loading the stable
  // jslib URLs, served from these npm dist files via the generated-webui
  // overlay (STANDALONE_NPM_COPIES) and bundle resolution below.
  "jslib/profiles/3x/jquery/plugins/jquery-form/jquery.form.js":
    "jquery-form/dist/jquery.form.min.js",
  "jslib/profiles/3x/jquery/plugins/jquery-validation/jquery.validate.js":
    "jquery-validation/dist/jquery.validate.min.js",
  "jslib/profiles/3x/jquery/plugins/jquery-validation/additional-methods.js":
    "jquery-validation/dist/additional-methods.min.js",
  "jslib/profiles/3x/jquery/plugins/jquery-jeditable/jquery.jeditable.js":
    "jquery-jeditable/dist/jquery.jeditable.min.js",
  "jslib/profiles/3x/jquery/plugins/jquery-superfish/js/superfish.js":
    "superfish/dist/js/superfish.min.js",
  "jslib/profiles/3x/jquery/plugins/jquery-superfish/js/hoverIntent.js":
    "superfish/dist/js/hoverIntent.js",
  "jslib/profiles/3x/jquery/plugins/jquery-superfish/css/superfish.css":
    "superfish/dist/css/superfish.css",
  "jslib/profiles/3x/jquery/plugins/jquery-superfish/css/superfish-vertical.css":
    "superfish/dist/css/superfish-vertical.css",
  "jslib/profiles/3x/jquery/plugins/jquery-superfish/css/superfish-navbar.css":
    "superfish/dist/css/superfish-navbar.css",
  "jslib/profiles/3x/jquery/plugins/jquery-superfish/css/megafish.css":
    "superfish/dist/css/megafish.css",
  "jslib/profiles/3x/jquery/plugins/jquery-hoverintent/jquery.hoverIntent.js":
    "hoverintent/dist/hoverintent.min.js",
  "jslib/profiles/3x/jquery/plugins/jquery-timeago/jquery.timeago.js":
    "timeago/jquery.timeago.js",
  "jslib/profiles/3x/jquery/plugins/jquery-ui-multiselect-widget/jquery.multiselect.js":
    "jquery-ui-multiselect-widget/src/jquery.multiselect.js",
  "jslib/profiles/3x/jquery/plugins/jquery-ui-multiselect-widget/jquery.multiselect.filter.js":
    "jquery-ui-multiselect-widget/src/jquery.multiselect.filter.js",
};

// List of bundle config files to process
const BUNDLE_CONFIGS = [
  "common-bundles.json",
  "common-minuet-bundles.json",
  "static-bundles.json",
  "common-ui-bundle.json",
];

/**
 * Resolve a file path, checking node_modules first for npm packages, then
 * the cm app root (and the delivery common-ui module for legacy relative paths).
 */
function resolvePath(filePath, baseDir = WAR_DIR) {
  // Handle references to intermediate builds (these will be pre-built)
  if (filePath.includes("target/minify-common")) {
    // For intermediate bundles, look for previously built bundles in outputDR
    const filename = path.basename(filePath);
    return path.join(OUTPUT_DIR, filename);
  }

  // Check if this file is npm-managed
  if (NPM_LIBRARY_MAPPINGS[filePath]) {
    const npmPath = path.join(NODE_MODULES_DIR, NPM_LIBRARY_MAPPINGS[filePath]);
    if (fs.existsSync(npmPath)) {
      return npmPath;
    }
    // Fall back to jslib/ if npm module not found (shouldn't happen if npm install worked)
    console.warn(
      `  ⚠️  npm module not found for ${filePath}, falling back to jslib/`,
    );
  }

  // Legacy relative path from the old WebUI/war layout:
  //   ../../delivery/common/js/<rest>
  // now lives in modules/perc-common-ui-bundle/src/main/js/<rest>
  const deliveryPrefix = "../../delivery/common/js/";
  if (filePath.startsWith(deliveryPrefix)) {
    return path.join(
      DELIVERY_COMMON_JS_DIR,
      filePath.slice(deliveryPrefix.length),
    );
  }

  // Otherwise, resolve from the cm app root
  return path.join(baseDir, filePath);
}

/**
 * Read a file. Returns { content, missing, error } so callers can fail hard
 * when intermediate bundles would otherwise be silently empty, and distinguish
 * "not found" from I/O errors (permissions, etc.).
 */
function readFile(filePath) {
  try {
    if (!fs.existsSync(filePath)) {
      console.warn(`  ⚠️  Missing file: ${filePath}`);
      return { content: "", missing: true, error: false };
    }
    return {
      content: fs.readFileSync(filePath, "utf8"),
      missing: false,
      error: false,
    };
  } catch (err) {
    console.error(`  ❌ Error reading ${filePath}:`, err.message);
    return { content: "", missing: false, error: true };
  }
}

/**
 * Build bundles from a single config file.
 * @param {string} configFile
 * @param {number} processingPhase
 * @param {{ failOnMissing?: boolean }} options - When true (phase-1 intermediates),
 *   missing source files abort the build instead of writing empty concatenations.
 * @returns {{ missingCount: number }}
 */
function buildBundlesFromConfig(configFile, processingPhase = 1, options = {}) {
  const { failOnMissing = false } = options;
  const configPath = path.join(BUNDLE_CONFIG_DIR, configFile);
  let missingCount = 0;
  let errorCount = 0;

  if (!fs.existsSync(configPath)) {
    console.warn(`⚠️  Config file not found: ${configPath}`);
    return { missingCount: 1, errorCount: 0 };
  }

  console.log(`\n📦 Processing ${configFile} (Phase ${processingPhase})...`);

  const config = JSON.parse(fs.readFileSync(configPath, "utf8"));

  config.bundles.forEach((bundle) => {
    // Determine the output path with appropriate suffix
    let outputPath = path.join(OUTPUT_DIR, bundle.name);

    // Add .min suffix for final page bundles (not for intermediate bundles or common-ui)
    if (bundle.name.includes("jslibMin/") || bundle.name.includes("cssMin/")) {
      // Insert .min before the file extension for page bundles
      const dir = path.dirname(outputPath);
      const file = path.basename(outputPath);
      const ext = path.extname(file);
      const nameWithoutExt = path.basename(file, ext);
      outputPath = path.join(dir, `${nameWithoutExt}.min${ext}`);
    }

    const outputDirPath = path.dirname(outputPath);

    // Create output directory if it doesn't exist
    if (!fs.existsSync(outputDirPath)) {
      fs.mkdirSync(outputDirPath, { recursive: true });
    }

    console.log(`  📄 Building ${bundle.name}...`);

    // Concatenate all files for this bundle
    const parts = [];
    for (const file of bundle.files) {
      const fullPath = resolvePath(file);
      const { content, missing, error } = readFile(fullPath);
      if (missing) {
        missingCount += 1;
      }
      if (error) {
        errorCount += 1;
      }
      parts.push(content);
    }
    const content = parts.join("\n");

    // Write the bundle
    fs.writeFileSync(outputPath, content, "utf8");
    const sizeKb = (content.length / 1024).toFixed(2);
    const outputName = path.relative(OUTPUT_DIR, outputPath);
    console.log(`    ✓ ${outputName} (${sizeKb}KB)`);
  });

  if (failOnMissing && (missingCount > 0 || errorCount > 0)) {
    const parts = [];
    if (missingCount > 0) {
      parts.push(`${missingCount} source file(s) missing`);
    }
    if (errorCount > 0) {
      parts.push(`${errorCount} source file(s) unreadable (I/O error)`);
    }
    throw new Error(
      `${configFile}: ${parts.join(
        ", ",
      )} while building intermediate bundles (source root: ${WAR_DIR})`,
    );
  }

  return { missingCount, errorCount };
}

/**
 * Standalone npm library files copied into the generated overlay for direct
 * <script> loading (outside of bundles). Dest paths are relative to OUTPUT_DIR
 * (target/generated-webui/cm/), never src/main/webapp/cm/ — writing into the
 * source tree breaks ai-build-integrity-maven-plugin seals (issue #1510).
 * maven-war-plugin overlays target/generated-webui onto the WAR at package time.
 *
 * Classic-UI trio (issue #5660): jQuery core + Migrate + UI are served ONLY
 * from these npm dist files. The vendored cm/jslib copies were deleted, so a
 * missing `required` entry must fail the build instead of shipping pages with
 * a 404 where jQuery used to load.
 *
 * Loader/misc infra (issue #5667): popper.js + bowser es5.js likewise come
 * ONLY from npm (Dependabot-tracked in frontend/package.json); their vendored
 * cm/jslib copies were deleted and the <script> tags keep stable URLs.
 */
const STANDALONE_NPM_COPIES = [
  {
    src: "jquery/dist/jquery.min.js",
    dest: "jslib/profiles/3x/jquery/jquery.min.js",
    required: true,
  },
  {
    src: "jquery-migrate/dist/jquery-migrate.min.js",
    dest: "jslib/profiles/3x/jquery/jquery-migrate.min.js",
    required: true,
  },
  {
    src: "jquery-ui/dist/jquery-ui.min.js",
    dest: "jslib/profiles/3x/jquery/libraries/jquery-ui/jquery-ui.min.js",
    required: true,
  },
  {
    src: "@popperjs/core/dist/umd/popper.js",
    dest: "jslib/profiles/3x/libraries/popper/popper.js",
    required: true,
  },
  {
    src: "bowser/es5.js",
    dest: "jslib/profiles/3x/libraries/bowser/es5.js",
    required: true,
  },
  {
    // Classic-UI Handlebars (issue #5666): the vulnerable vendored
    // handlebars-v4.0.12.js and the duplicate v4.7.8 copies were deleted.
    // Classic pages keep loading the stable versioned URL, now served from
    // the npm 4.7.10 dist (Dependabot-tracked) via the generated overlay.
    src: "handlebars/dist/handlebars.js",
    dest: "jslib/profiles/3x/libraries/handlebars/handlebars-v4.7.8.js",
    required: true,
  },
  // jQuery plugin cluster from npm (issue #5663). Dest paths are the stable
  // classic-UI jslib URLs (common_js.jsp / common_css.jsp / bundle configs
  // keep referencing them unchanged); sources are the Dependabot-tracked npm
  // dist files. The vendored copies were deleted, so every entry is required.
  {
    src: "jquery-form/dist/jquery.form.min.js",
    dest: "jslib/profiles/3x/jquery/plugins/jquery-form/jquery.form.js",
    required: true,
  },
  {
    src: "jquery-validation/dist/jquery.validate.min.js",
    dest: "jslib/profiles/3x/jquery/plugins/jquery-validation/jquery.validate.js",
    required: true,
  },
  {
    src: "jquery-validation/dist/additional-methods.min.js",
    dest: "jslib/profiles/3x/jquery/plugins/jquery-validation/additional-methods.js",
    required: true,
  },
  {
    src: "jquery-jeditable/dist/jquery.jeditable.min.js",
    dest: "jslib/profiles/3x/jquery/plugins/jquery-jeditable/jquery.jeditable.js",
    required: true,
  },
  {
    src: "superfish/dist/js/superfish.min.js",
    dest: "jslib/profiles/3x/jquery/plugins/jquery-superfish/js/superfish.js",
    required: true,
  },
  {
    src: "superfish/dist/js/hoverIntent.js",
    dest: "jslib/profiles/3x/jquery/plugins/jquery-superfish/js/hoverIntent.js",
    required: true,
  },
  {
    src: "superfish/dist/css/superfish.css",
    dest: "jslib/profiles/3x/jquery/plugins/jquery-superfish/css/superfish.css",
    required: true,
  },
  {
    src: "superfish/dist/css/superfish-vertical.css",
    dest: "jslib/profiles/3x/jquery/plugins/jquery-superfish/css/superfish-vertical.css",
    required: true,
  },
  {
    src: "superfish/dist/css/superfish-navbar.css",
    dest: "jslib/profiles/3x/jquery/plugins/jquery-superfish/css/superfish-navbar.css",
    required: true,
  },
  {
    src: "superfish/dist/css/megafish.css",
    dest: "jslib/profiles/3x/jquery/plugins/jquery-superfish/css/megafish.css",
    required: true,
  },
  {
    src: "hoverintent/dist/hoverintent.min.js",
    dest: "jslib/profiles/3x/jquery/plugins/jquery-hoverintent/jquery.hoverIntent.js",
    required: true,
  },
  {
    src: "timeago/jquery.timeago.js",
    dest: "jslib/profiles/3x/jquery/plugins/jquery-timeago/jquery.timeago.js",
    required: true,
  },
  {
    src: "jquery-ui-multiselect-widget/src/jquery.multiselect.js",
    dest: "jslib/profiles/3x/jquery/plugins/jquery-ui-multiselect-widget/jquery.multiselect.js",
    required: true,
  },
  {
    src: "jquery-ui-multiselect-widget/src/jquery.multiselect.filter.js",
    dest: "jslib/profiles/3x/jquery/plugins/jquery-ui-multiselect-widget/jquery.multiselect.filter.js",
    required: true,
  },
];

/**
 * Copy standalone npm library files into target/generated-webui/cm/ so they can
 * be loaded individually via <script> tags (e.g. sys_aaPageHeader.html).
 * Does not write under src/main/webapp (source tree stays sealed).
 * Throws when a `required` npm source is missing (fail-fast for the #5660 trio).
 */
function syncStandaloneNpmLibraries() {
  console.log(
    "📋 Phase 0: Syncing standalone npm libraries to target/generated-webui/cm/...",
  );

  STANDALONE_NPM_COPIES.forEach(({ src, dest, required }) => {
    const srcPath = path.join(NODE_MODULES_DIR, src);
    const destPath = path.join(OUTPUT_DIR, dest);
    const destDir = path.dirname(destPath);

    if (!fs.existsSync(destDir)) {
      fs.mkdirSync(destDir, { recursive: true });
    }

    if (!fs.existsSync(srcPath)) {
      const message = `npm file not found: ${srcPath}`;
      if (required) {
        throw new Error(`syncStandaloneNpmLibraries: ${message}`);
      }
      console.warn(`  ⚠️  ${message}`);
      return;
    }

    fs.copyFileSync(srcPath, destPath);
    const sizeKb = (fs.statSync(destPath).size / 1024).toFixed(2);
    console.log(`  ✓ ${dest} (${sizeKb}KB from npm → generated-webui)`);
  });
}

/**
 * Assert intermediate shared-* bundles exist and are non-trivial so Maven
 * packaging never ships empty placeholders when sources fail to resolve.
 */
function assertRequiredIntermediates() {
  const minBytes = 1024;
  for (const name of REQUIRED_INTERMEDIATE_BUNDLES) {
    const out = path.join(OUTPUT_DIR, name);
    if (!fs.existsSync(out)) {
      throw new Error(`Required intermediate bundle missing: ${out}`);
    }
    const size = fs.statSync(out).size;
    if (size < minBytes) {
      throw new Error(
        `Required intermediate bundle too small (${size} bytes): ${out}`,
      );
    }
  }
}

/**
 * Main build process
 */
function main() {
  console.log("🔨 Building legacy JavaScript and CSS bundles...\n");
  console.log(`   Source root:   ${WAR_DIR}`);
  console.log(`   Output:        ${OUTPUT_DIR}`);
  console.log(`   Configs:       ${BUNDLE_CONFIG_DIR}\n`);

  try {
    // Phase 0: Copy standalone npm libraries into generated-webui only (not src/)
    syncStandaloneNpmLibraries();

    // Phase 1: Build intermediate common bundles (shared-common.js, shared-finder.js, etc.)
    // Fail hard on missing sources — these are the only packaging inputs for
    // shared-common* / shared-finder (no longer checked into git).
    buildBundlesFromConfig("common-bundles.json", 1, { failOnMissing: true });
    buildBundlesFromConfig("common-minuet-bundles.json", 1, {
      failOnMissing: true,
    });
    assertRequiredIntermediates();

    // Phase 2: Build final page-specific bundles (which reference the intermediate ones)
    buildBundlesFromConfig("static-bundles.json", 2);
    buildBundlesFromConfig("common-ui-bundle.json", 2);

    // Phase 3: Create compatibility aliases for non-.min versions (for PercProcessMonitor.jsp)
    console.log("\n🔗 Creating compatibility aliases...");
    const jslibMinDir = path.join(OUTPUT_DIR, "jslibMin");
    const cssMinDir = path.join(OUTPUT_DIR, "cssMin");

    if (fs.existsSync(jslibMinDir)) {
      fs.readdirSync(jslibMinDir).forEach((file) => {
        if (file.endsWith(".min.js")) {
          const minFile = file;
          const nonMinFile = file.replace(".min.js", ".js");
          const src = path.join(jslibMinDir, minFile);
          const dest = path.join(jslibMinDir, nonMinFile);
          try {
            fs.copyFileSync(src, dest);
            console.log(`  ✓ ${nonMinFile} (alias for ${minFile})`);
          } catch (err) {
            console.error(
              `  ❌ Error creating alias ${nonMinFile}: ${err.message}`,
            );
          }
        }
      });
    }

    if (fs.existsSync(cssMinDir)) {
      fs.readdirSync(cssMinDir).forEach((file) => {
        if (file.endsWith(".min.css")) {
          const minFile = file;
          const nonMinFile = file.replace(".min.css", ".css");
          const src = path.join(cssMinDir, minFile);
          const dest = path.join(cssMinDir, nonMinFile);
          try {
            fs.copyFileSync(src, dest);
            console.log(`  ✓ ${nonMinFile} (alias for ${minFile})`);
          } catch (err) {
            console.error(
              `  ❌ Error creating alias ${nonMinFile}: ${err.message}`,
            );
          }
        }
      });
    }

    console.log("\n✅ Legacy bundles built successfully!");
  } catch (err) {
    console.error("\n❌ Error building bundles:", err.message || err);
    process.exit(1);
  }
}

if (require.main === module) {
  main();
}

module.exports = {
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
  main,
};
