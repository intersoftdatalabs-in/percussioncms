#!/usr/bin/env node

/**
 * Legacy Bundle Builder (stale WebUI/scripts/ entry point).
 *
 * Canonical script used by Maven / frontend-maven-plugin:
 *   WebUI/src/main/frontend/scripts/build-legacy-bundles.cjs
 *
 * Prefer: cd src/main/frontend && npm run build:legacy
 *
 * This copy remains for accidental root package.json invocations. Standalone
 * npm mins must go to target/generated-webui only (issue #1510) — never war/
 * or src/main/webapp. Kept as .cjs so require() works under package
 * `"type": "module"`.
 */

const fs = require("fs");
const path = require("path");

// Paths are relative to the WebUI directory (parent of scripts/)
const WEBUI_DIR = path.dirname(__dirname);
const WAR_DIR = path.join(WEBUI_DIR, "war");
const BUNDLE_CONFIG_DIR = path.join(WEBUI_DIR, "src/main/resources/minify");
// Prefer generated overlay (matches canonical frontend builder). Reading still
// uses war/ when present for this stale entry point's intermediate paths.
const OUTPUT_DIR = path.join(WEBUI_DIR, "target/generated-webui/cm");
const NODE_MODULES_DIR = path.join(WEBUI_DIR, "src/main/frontend/node_modules");

// Mapping of jslib paths to npm package names for npm-managed libraries.
// Classic-UI direct <script> loads use the npm-backed files
// (issue #5660 trio + issue #5667 popper/bowser); keys mirror the canonical frontend builder.
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
  // jQuery plugin cluster from npm (issue #5663); keys mirror the canonical
  // frontend builder.
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
 * Resolve a file path, checking node_modules first for npm packages, then jslib/
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

  // Otherwise, resolve from jslib/
  return path.join(baseDir, filePath);
}

/**
 * Read a file or return empty string if file doesn't exist
 */
function readFile(filePath) {
  try {
    if (!fs.existsSync(filePath)) {
      console.warn(`  ⚠️  Missing file: ${filePath}`);
      return "";
    }
    return fs.readFileSync(filePath, "utf8");
  } catch (err) {
    console.error(`  ❌ Error reading ${filePath}:`, err.message);
    return "";
  }
}

/**
 * Build bundles from a single config file
 */
function buildBundlesFromConfig(configFile, processingPhase = 1) {
  const configPath = path.join(BUNDLE_CONFIG_DIR, configFile);

  if (!fs.existsSync(configPath)) {
    console.warn(`⚠️  Config file not found: ${configPath}`);
    return;
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
    const content = bundle.files
      .map((file) => {
        const fullPath = resolvePath(file);
        return readFile(fullPath);
      })
      .join("\n");

    // Write the bundle
    fs.writeFileSync(outputPath, content, "utf8");
    const sizeKb = (content.length / 1024).toFixed(2);
    const outputName = path.relative(OUTPUT_DIR, outputPath);
    console.log(`    ✓ ${outputName} (${sizeKb}KB)`);
  });
}

/**
 * Standalone npm mins → target/generated-webui/cm only (never war/ or src/).
 * See issue #1510 / canonical frontend builder. Classic-UI trio (issue #5660)
 * is required: the vendored cm/jslib copies are deleted, so a missing source
 * must fail instead of shipping pages with a 404 where jQuery used to load.
 * Loader/misc pair (issue #5667: popper.js + bowser es5.js) is likewise
 * required: vendored copies deleted, tags keep stable URLs served from npm.
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
  // jQuery plugin cluster from npm (issue #5663); mirrors the canonical
  // frontend builder.
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
 * Copy standalone npm library files into the generated overlay.
 * Throws when a `required` npm source is missing (fail-fast, issue #5660).
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
 * Main build process
 */
function main() {
  console.log("🔨 Building legacy JavaScript and CSS bundles...\n");
  console.log(`   WAR directory: ${WAR_DIR}`);
  console.log(`   Configs:       ${BUNDLE_CONFIG_DIR}\n`);

  try {
    // Phase 0: standalone npm mins → generated-webui only
    syncStandaloneNpmLibraries();

    // Phase 1: Build intermediate common bundles (shared-common.js, shared-finder.js, etc.)
    buildBundlesFromConfig("common-bundles.json", 1);
    buildBundlesFromConfig("common-minuet-bundles.json", 1);

    // Phase 2: Build final page-specific bundles (which reference the intermediate ones)
    buildBundlesFromConfig("static-bundles.json", 2);
    buildBundlesFromConfig("common-ui-bundle.json", 2);

    // Phase 3: Create compatibility aliases for non-.min versions (for PercProcessMonitor.jsp)
    console.log("\n🔗 Creating compatibility aliases...");
    const jslibMinDir = path.join(OUTPUT_DIR, "jslibMin");
    const cssMinDir = path.join(OUTPUT_DIR, "cssMin");

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

    console.log("\n✅ Legacy bundles built successfully!");
  } catch (err) {
    console.error("\n❌ Error building bundles:", err);
    process.exit(1);
  }
}

main();
