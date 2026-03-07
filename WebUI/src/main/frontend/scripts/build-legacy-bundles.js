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
const WAR_DIR = path.join(WEBUI_DIR, "src/main/webapp");
const BUNDLE_CONFIG_DIR = path.join(WEBUI_DIR, "src/main/resources/minify");
const OUTPUT_DIR = path.join(WEBUI_DIR, "target/generated-webui/cm");
const NODE_MODULES_DIR = path.join(__dirname, "../node_modules");

// Mapping of jslib paths to npm package names for npm-managed libraries
const NPM_LIBRARY_MAPPINGS = {
  "jslib/profiles/3x/jquery/jquery-3.6.0.js": "jquery/dist/jquery.js",
  "jslib/profiles/3x/jquery/profiles/3x/jquery/plugins/jquery-migrate/jquery-migrate-3.3.2.js":
    "jquery-migrate/dist/jquery-migrate.js",
  "jslib/profiles/3x/jquery/libraries/jquery-ui/jquery-ui.js":
    "jquery-ui/dist/jquery-ui.js",
  "jslib/profiles/3x/libraries/bootstrap/js/bootstrap.bundle.js":
    "bootstrap/dist/js/bootstrap.bundle.js",
  "jslib/profiles/3x/libraries/bootstrap/css/bootstrap.css":
    "bootstrap/dist/css/bootstrap.css",
  "jslib/profiles/3x/libraries/bowser/es5.js": "bowser/es5.js",
  "jslib/profiles/3x/libraries/handlebars/handlebars-v4.7.8.js":
    "handlebars/dist/handlebars.js",
  "jslib/profiles/3x/libraries/popper/popper.js":
    "@popperjs/core/dist/umd/popper.js",
  "jslib/profiles/3x/libraries/momentjs/moment-with-locales.js":
    "moment/moment.js",
  "jslib/profiles/3x/libraries/underscore/underscore.js":
    "underscore/underscore.js",
  "jslib/profiles/3x/libraries/backbone/backbone.js": "backbone/backbone.js",
  "jslib/profiles/3x/libraries/qunit/qunit.js": "qunit/qunit/qunit.js",
  "jslib/profiles/3x/libraries/mousetrap/mousetrap.js":
    "mousetrap/mousetrap.js",
  "jslib/profiles/3x/libraries/modernizer/modernizr.custom.js":
    "modernizr/bin/modernizr",
  "jslib/profiles/3x/libraries/animate.css/animate.css":
    "animate.css/animate.css",
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
 * Standalone npm library files that should be copied to the WAR directory
 * for direct <script> loading (outside of bundles). The build always
 * overwrites these so the WAR stays in sync with the npm versions.
 */
const STANDALONE_NPM_COPIES = [
  {
    src: "jquery/dist/jquery.min.js",
    dest: "jslib/profiles/3x/jquery/jquery.min.js",
  },
  {
    src: "jquery-migrate/dist/jquery-migrate.min.js",
    dest: "jslib/profiles/3x/jquery/jquery-migrate.min.js",
  },
];

/**
 * Copy standalone npm library files to the WAR directory so they can be
 * loaded individually via <script> tags (e.g., from the AA page header).
 */
function syncStandaloneNpmLibraries() {
  console.log("📋 Phase 0: Syncing standalone npm libraries to war/...");

  STANDALONE_NPM_COPIES.forEach(({ src, dest }) => {
    const srcPath = path.join(NODE_MODULES_DIR, src);
    const destPath = path.join(WAR_DIR, dest);
    const destDir = path.dirname(destPath);

    if (!fs.existsSync(destDir)) {
      fs.mkdirSync(destDir, { recursive: true });
    }

    if (!fs.existsSync(srcPath)) {
      console.warn(`  ⚠️  npm file not found: ${srcPath}`);
      return;
    }

    fs.copyFileSync(srcPath, destPath);
    const sizeKb = (fs.statSync(destPath).size / 1024).toFixed(2);
    console.log(`  ✓ ${dest} (${sizeKb}KB from npm)`);
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
    // Phase 0: Copy standalone npm libraries to war/ for direct <script> loading
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
