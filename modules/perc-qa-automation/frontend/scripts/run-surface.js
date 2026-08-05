#!/usr/bin/env node
/**
 * Run (or print) a Playwright surface subset using native CLI filters.
 *
 * Usage (from modules/perc-qa-automation/frontend):
 *   node scripts/run-surface.js --path tests/login.spec.js
 *   node scripts/run-surface.js --grep "Admin login" --list
 *   node scripts/run-surface.js --tag smoke --print-only
 *   SURFACE_PATH=tests/login.spec.js node scripts/run-surface.js
 *
 * Env: SURFACE_PATH, SURFACE_PATHS, SURFACE_GREP, SURFACE_TAG,
 *      SURFACE_GREP_INVERT
 *
 * Requires an explicit surface (path / grep / tag) unless --allow-full is set.
 * Does not start Docker CMS — set TEST_CMS_URL from perc-devctl qa-up first.
 *
 * @see tests/helpers/surface-filter.js
 */

"use strict";

const path = require("path");
const { spawnSync } = require("child_process");
const {
  buildPlaywrightSurfaceArgs,
  formatSurfaceCommand,
  hasSurfaceFilter,
  optionsFromEnv,
  splitPathList,
} = require("../tests/helpers/surface-filter");

/**
 * Minimal argv parse (no external deps). Supports:
 *   --path <p> (repeatable), --paths <csv>, --grep <re>, --tag <t>,
 *   --grep-invert <re>, --list, --print-only, --allow-full,
 *   -- <passthrough to playwright>
 * @param {string[]} argv process.argv.slice(2)
 */
function parseArgv(argv) {
  /** @type {string[]} */
  const paths = [];
  let grep = "";
  let tag = "";
  let grepInvert = "";
  let list = false;
  let printOnly = false;
  let allowFull = false;
  /** @type {string[]} */
  const extraArgs = [];
  let i = 0;
  while (i < argv.length) {
    const a = argv[i];
    if (a === "--") {
      extraArgs.push(...argv.slice(i + 1));
      break;
    }
    if (a === "--path" || a === "-p") {
      i += 1;
      if (argv[i]) {
        paths.push(argv[i]);
      }
    } else if (a === "--paths") {
      i += 1;
      paths.push(...splitPathList(argv[i] || ""));
    } else if (a === "--grep" || a === "-g") {
      i += 1;
      grep = argv[i] || "";
    } else if (a === "--tag" || a === "-t") {
      i += 1;
      tag = argv[i] || "";
    } else if (a === "--grep-invert") {
      i += 1;
      grepInvert = argv[i] || "";
    } else if (a === "--list" || a === "-l") {
      list = true;
    } else if (a === "--print-only" || a === "--dry-run") {
      printOnly = true;
    } else if (a === "--allow-full") {
      allowFull = true;
    } else if (a === "--help" || a === "-h") {
      printHelp();
      process.exit(0);
    } else if (a.startsWith("-")) {
      console.error(`Unknown option: ${a}`);
      printHelp();
      process.exit(2);
    } else {
      // bare path
      paths.push(a);
    }
    i += 1;
  }
  return {
    paths,
    grep,
    tag,
    grepInvert,
    list,
    printOnly,
    allowFull,
    extraArgs,
  };
}

function printHelp() {
  console.log(`Usage: node scripts/run-surface.js [options] [paths...]

Options:
  --path, -p <file>     Spec path (repeatable). Also bare positional paths.
  --paths <csv>         Comma-separated paths
  --grep, -g <regex>    Playwright --grep (title / @tag)
  --tag, -t <name>      Convenience → --grep @name
  --grep-invert <regex> Playwright --grep-invert
  --list, -l            List matching tests only (no live CMS)
  --print-only          Print the playwright command; do not run
  --allow-full          Allow empty filter (full suite — avoid for agents)
  -- <args>             Extra args passed to playwright test

Env (merged with CLI; CLI wins when both set for a field):
  SURFACE_PATH, SURFACE_PATHS, SURFACE_GREP, SURFACE_TAG, SURFACE_GREP_INVERT

Examples:
  node scripts/run-surface.js --path tests/login.spec.js --list
  node scripts/run-surface.js --tag smoke --print-only
  SURFACE_GREP="Content Explorer" node scripts/run-surface.js --list
`);
}

function main() {
  const cli = parseArgv(process.argv.slice(2));
  const fromEnv = optionsFromEnv(process.env);

  // CLI path/grep/tag win when provided; otherwise fall back to env.
  const paths = cli.paths.length > 0 ? cli.paths : fromEnv.paths;
  const grep = cli.grep || fromEnv.grep;
  const tag = cli.tag || fromEnv.tag;
  const grepInvert = cli.grepInvert || fromEnv.grepInvert;

  /** @type {import('../tests/helpers/surface-filter').SurfaceFilterOptions} */
  const opts = {
    paths,
    grep,
    tag,
    grepInvert,
    list: cli.list,
    extraArgs: cli.extraArgs,
  };

  if (!hasSurfaceFilter(opts) && !cli.allowFull) {
    console.error(
      "run-surface: refuse full suite without an explicit surface filter.\n" +
        "Provide --path / --grep / --tag (or SURFACE_*), or pass --allow-full.\n" +
        "Tip: --list --path tests/login.spec.js needs no live CMS.",
    );
    printHelp();
    process.exit(2);
  }

  const pwArgs = buildPlaywrightSurfaceArgs(opts);
  const cmdDisplay = formatSurfaceCommand(opts, {
    cwdHint: "npx playwright test",
  });

  if (cli.printOnly) {
    console.log(cmdDisplay);
    process.exit(0);
  }

  // Resolve playwright binary from local node_modules (cross-platform).
  const playwrightCli = path.resolve(
    __dirname,
    "..",
    "node_modules",
    "@playwright",
    "test",
    "cli.js",
  );
  const node = process.execPath;
  const spawnArgs = [playwrightCli, "test", ...pwArgs];
  console.error(`run-surface: ${node} ${spawnArgs.join(" ")}`);

  const result = spawnSync(node, spawnArgs, {
    cwd: path.resolve(__dirname, ".."),
    stdio: "inherit",
    env: process.env,
    windowsHide: true,
  });

  if (result.error) {
    console.error(
      "run-surface: failed to spawn Playwright:",
      result.error.message,
    );
    process.exit(1);
  }
  process.exit(result.status == null ? 1 : result.status);
}

main();
