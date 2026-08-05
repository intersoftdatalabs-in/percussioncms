/**
 * Pure CMS URL / credential resolution for Playwright QA + dev modes.
 *
 * Extracted from auth.js so unit tests can cover precedence without a live CMS
 * or filesystem install. See #2064 / #1928 slice A.
 *
 * Precedence for base URL (highest first):
 *   1. TEST_CMS_URL (QA mode primary; also documented aliases)
 *   2. Constructed from QA_CMS_HOST_PORT / CMS_HOST_PORT (freeport harness)
 *   3. DEV_PERCUSSION_URL (explicit dev override)
 *   4. installUrl option (caller already discovered from DEV_PERCUSSION_INSTALL)
 *   5. Documented fallback (dev default port — not the only freeport pin)
 *
 * Do not hardcode host port 9993 as the sole QA URL: multi-worktree freeport
 * may allocate another port; prefer TEST_CMS_URL from `perc-devctl qa-up`.
 * Freeport contract: #2005 / #2014; docs in workbench-rest-and-qa-modes.md.
 */

"use strict";

/** Env keys checked for an explicit CMS base URL (QA-first). */
const CMS_URL_ENV_KEYS = Object.freeze([
  "TEST_CMS_URL",
  "CMS_BASE_URL",
  "QA_CMS_URL",
]);

/** Host-port env keys used when TEST_CMS_URL is unset (freeport / matrix). */
const CMS_HOST_PORT_ENV_KEYS = Object.freeze([
  "QA_CMS_HOST_PORT",
  "CMS_HOST_PORT",
]);

/** Dev-mode explicit URL. */
const DEV_URL_ENV_KEYS = Object.freeze(["DEV_PERCUSSION_URL"]);

/** Preferred single-worktree baseline when free (not the only option). */
const QA_PREFERRED_FALLBACK_URL = "http://localhost:9993";

/** Human dev-mode default when nothing else is set. */
const DEV_FALLBACK_URL = "http://localhost:9992";

/**
 * @param {string | undefined | null} value
 * @returns {string | null}
 */
function trimNonEmpty(value) {
  if (value == null) {
    return null;
  }
  const s = String(value).trim();
  return s.length > 0 ? s : null;
}

/**
 * @param {string} url
 * @returns {string}
 */
function stripTrailingSlash(url) {
  return url.replace(/\/+$/, "");
}

/**
 * @param {NodeJS.ProcessEnv | Record<string, string | undefined>} env
 * @param {readonly string[]} keys
 * @returns {string | null}
 */
function firstEnv(env, keys) {
  for (const key of keys) {
    const v = trimNonEmpty(env[key]);
    if (v) {
      return v;
    }
  }
  return null;
}

/**
 * @param {NodeJS.ProcessEnv | Record<string, string | undefined>} env
 * @returns {string | null} host port digits only, or null
 */
function firstHostPort(env) {
  const raw = firstEnv(env, CMS_HOST_PORT_ENV_KEYS);
  if (!raw) {
    return null;
  }
  // Accept plain digits (2–5 chars); reject garbage and ports outside 1–65535.
  if (!/^\d{2,5}$/.test(raw)) {
    return null;
  }
  const port = Number.parseInt(raw, 10);
  if (!Number.isFinite(port) || port < 1 || port > 65535) {
    return null;
  }
  return String(port);
}

/**
 * Resolve the CMS base URL used by Playwright auth helpers and specs.
 *
 * @param {NodeJS.ProcessEnv | Record<string, string | undefined>} [env]
 * @param {{ installUrl?: string | null, fallbackUrl?: string | null }} [options]
 * @returns {{ url: string, source: string }}
 */
function resolveCmsBaseUrl(env = process.env, options = {}) {
  const installUrl = trimNonEmpty(options.installUrl);
  const fallbackUrl = trimNonEmpty(options.fallbackUrl) || DEV_FALLBACK_URL;

  const fromTest = firstEnv(env, CMS_URL_ENV_KEYS);
  if (fromTest) {
    return { url: stripTrailingSlash(fromTest), source: "TEST_CMS_URL" };
  }

  const hostPort = firstHostPort(env);
  if (hostPort) {
    return {
      url: `http://127.0.0.1:${hostPort}`,
      source: "CMS_HOST_PORT",
    };
  }

  const fromDev = firstEnv(env, DEV_URL_ENV_KEYS);
  if (fromDev) {
    return { url: stripTrailingSlash(fromDev), source: "DEV_PERCUSSION_URL" };
  }

  if (installUrl) {
    return { url: stripTrailingSlash(installUrl), source: "install" };
  }

  return { url: stripTrailingSlash(fallbackUrl), source: "fallback" };
}

/**
 * Resolve a role password: explicit env key wins; then install map; else null.
 *
 * QA mode: set ADMIN_PASSWORD (etc.) from qa-up output / docker exec.
 * Never commit secrets. Dev mode may still discover from install passwords file.
 *
 * @param {string} roleUserName e.g. "Admin"
 * @param {NodeJS.ProcessEnv | Record<string, string | undefined>} [env]
 * @param {Record<string, string>} [installPasswords] map of user → password
 * @returns {{ password: string | null, source: string }}
 */
function resolveRolePassword(
  roleUserName,
  env = process.env,
  installPasswords = {},
) {
  const envKey = `${String(roleUserName).toUpperCase()}_PASSWORD`;
  const fromEnv = trimNonEmpty(env[envKey]);
  if (fromEnv) {
    return { password: fromEnv, source: envKey };
  }
  const fromInstall = trimNonEmpty(installPasswords[roleUserName]);
  if (fromInstall) {
    return { password: fromInstall, source: "install" };
  }
  return { password: null, source: "missing" };
}

/**
 * Whether QA-oriented env is present so callers can skip host install discovery.
 *
 * @param {NodeJS.ProcessEnv | Record<string, string | undefined>} [env]
 * @returns {boolean}
 */
function hasQaModeUrlEnv(env = process.env) {
  if (firstEnv(env, CMS_URL_ENV_KEYS)) {
    return true;
  }
  return firstHostPort(env) != null;
}

module.exports = {
  CMS_URL_ENV_KEYS,
  CMS_HOST_PORT_ENV_KEYS,
  DEV_URL_ENV_KEYS,
  QA_PREFERRED_FALLBACK_URL,
  DEV_FALLBACK_URL,
  resolveCmsBaseUrl,
  resolveRolePassword,
  hasQaModeUrlEnv,
  stripTrailingSlash,
  firstEnv,
};
