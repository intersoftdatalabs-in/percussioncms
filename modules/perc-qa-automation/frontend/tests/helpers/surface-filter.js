/**
 * Build native Playwright CLI arguments for a PR / agent surface subset.
 *
 * Unattended QA mode should not run the full suite by default. Prefer a
 * focused path, title --grep, and/or tag filter for the surface under test.
 *
 * Patterns (native Playwright only — no custom runner):
 * - path(s): positional file/dir globs under frontend/ (e.g. tests/login.spec.js)
 * - grep: --grep <regex> (matches test title; also matches @tags when present)
 * - grepInvert: --grep-invert <regex>
 * - tag: convenience for --grep @name (adds leading @ if missing)
 * - list: --list (enumerate matching tests; no browser / no live CMS)
 *
 * Env aliases (used by scripts/run-surface.js): SURFACE_PATH, SURFACE_PATHS,
 * SURFACE_GREP, SURFACE_TAG, SURFACE_GREP_INVERT.
 *
 * @see docs/developer-module/workbench-rest-and-qa-modes.md (surface filter)
 * @see modules/perc-qa-automation/README.md
 */

"use strict";

/**
 * Normalize a tag token for Playwright --grep.
 * @param {string} tag raw tag (e.g. "smoke", "@smoke")
 * @returns {string} pattern suitable for --grep (e.g. "@smoke")
 */
function normalizeTag(tag) {
  const t = String(tag || "").trim();
  if (!t) {
    return "";
  }
  return t.startsWith("@") ? t : `@${t}`;
}

/**
 * Split a path list from env or comma-separated string.
 * @param {string|string[]|undefined|null} value
 * @returns {string[]}
 */
function splitPathList(value) {
  if (value == null || value === "") {
    return [];
  }
  if (Array.isArray(value)) {
    return value.map((p) => String(p).trim()).filter((p) => p.length > 0);
  }
  return String(value)
    .split(/[,;\n]/)
    .map((p) => p.trim())
    .filter((p) => p.length > 0);
}

/**
 * Read surface options from process-style env map.
 * @param {NodeJS.ProcessEnv|Record<string, string|undefined>} [env]
 * @returns {{ paths: string[], grep: string, tag: string, grepInvert: string }}
 */
function optionsFromEnv(env) {
  const e = env || {};
  const paths = [
    ...splitPathList(e.SURFACE_PATH),
    ...splitPathList(e.SURFACE_PATHS),
  ];
  return {
    paths,
    grep: String(e.SURFACE_GREP || "").trim(),
    tag: String(e.SURFACE_TAG || "").trim(),
    grepInvert: String(e.SURFACE_GREP_INVERT || "").trim(),
  };
}

/**
 * @typedef {object} SurfaceFilterOptions
 * @property {string|string[]} [path] single path or list
 * @property {string|string[]} [paths] additional paths
 * @property {string} [grep] title/tag regex for --grep
 * @property {string} [tag] convenience tag → --grep @tag
 * @property {string} [grepInvert] --grep-invert
 * @property {boolean} [list] include --list
 * @property {string[]} [extraArgs] passthrough (e.g. --project=chromium)
 */

/**
 * Build argv after `playwright test` for a surface subset.
 * Returns [] only when no filter is requested and list is false — callers
 * that require an explicit surface should reject empty filters separately.
 *
 * @param {SurfaceFilterOptions} [opts]
 * @returns {string[]} CLI args (not including `playwright` / `test`)
 */
function buildPlaywrightSurfaceArgs(opts) {
  const o = opts || {};
  const paths = [...splitPathList(o.path), ...splitPathList(o.paths)];
  const grepParts = [];
  const grep = String(o.grep || "").trim();
  const tag = normalizeTag(o.tag);
  if (grep) {
    grepParts.push(grep);
  }
  if (tag) {
    // Combine as alternation if both provided so either title match or tag works.
    if (grep && grep !== tag) {
      grepParts[0] = `(?:${grep})|${escapeForAlternation(tag)}`;
    } else if (!grep) {
      grepParts.push(tag);
    }
  }
  const grepInvert = String(o.grepInvert || "").trim();
  const list = Boolean(o.list);
  const extra = Array.isArray(o.extraArgs)
    ? o.extraArgs.map(String).filter((a) => a.length > 0)
    : [];

  /** @type {string[]} */
  const args = [];
  for (const p of paths) {
    args.push(p);
  }
  if (grepParts.length === 1) {
    args.push("--grep", grepParts[0]);
  }
  if (grepInvert) {
    args.push("--grep-invert", grepInvert);
  }
  if (list) {
    args.push("--list");
  }
  for (const a of extra) {
    args.push(a);
  }
  return args;
}

/**
 * Escape a fixed tag string for safe use inside a non-capturing alternation.
 * Tags are expected to be simple @word tokens; still escape regex meta.
 * @param {string} s
 * @returns {string}
 */
function escapeForAlternation(s) {
  return String(s).replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

/**
 * True when the options select a non-empty surface (path, grep, and/or tag).
 * `--list` alone does not count as a surface filter.
 * @param {SurfaceFilterOptions} [opts]
 * @returns {boolean}
 */
function hasSurfaceFilter(opts) {
  const o = opts || {};
  const paths = [...splitPathList(o.path), ...splitPathList(o.paths)];
  if (paths.length > 0) {
    return true;
  }
  if (String(o.grep || "").trim()) {
    return true;
  }
  if (normalizeTag(o.tag)) {
    return true;
  }
  return false;
}

/**
 * Human-readable command for docs / dry-run print.
 * @param {SurfaceFilterOptions} [opts]
 * @param {{ cwdHint?: string }} [meta]
 * @returns {string}
 */
function formatSurfaceCommand(opts, meta) {
  const args = buildPlaywrightSurfaceArgs(opts);
  const prefix = (meta && meta.cwdHint) || "npx playwright test";
  if (args.length === 0) {
    return prefix;
  }
  // Quote args that contain spaces or shell metacharacters for display only.
  // Escape backslashes first, then double-quotes (CodeQL js/incomplete-sanitization).
  const quoted = args.map((a) => {
    if (/[\s"'$&|;<>]/.test(a)) {
      const escaped = String(a).replace(/\\/g, "\\\\").replace(/"/g, '\\"');
      return `"${escaped}"`;
    }
    return a;
  });
  return `${prefix} ${quoted.join(" ")}`;
}

module.exports = {
  buildPlaywrightSurfaceArgs,
  escapeForAlternation,
  formatSurfaceCommand,
  hasSurfaceFilter,
  normalizeTag,
  optionsFromEnv,
  splitPathList,
};
