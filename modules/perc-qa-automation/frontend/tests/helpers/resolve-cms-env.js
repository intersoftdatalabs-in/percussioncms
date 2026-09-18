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
 * Parse installer {@code var/config/generated/passwords} text (Java Properties
 * shape: {@code User=value} lines). Does not log values.
 *
 * @param {string | undefined | null} text
 * @returns {Record<string, string>}
 */
function parseGeneratedPasswordsText(text) {
  const map = {};
  if (text == null) {
    return map;
  }
  const lines = String(text).split(/\r?\n/);
  for (let i = 0; i < lines.length; i++) {
    const line = lines[i].trim();
    if (!line || line.startsWith("#")) {
      continue;
    }
    const eq = line.indexOf("=");
    if (eq <= 0) {
      continue;
    }
    const key = line.slice(0, eq).trim();
    const value = line.slice(eq + 1).trim();
    if (key && value) {
      map[key] = value;
    }
  }
  return map;
}

/**
 * Resolve a role password.
 *
 * Precedence:
 *   1. Explicit {@code ROLE_PASSWORD} env (always wins for Admin).
 *   2. QA H2 cell map (generated passwords) for Editor / Contributor when
 *      {@code hasQaModeUrlEnv} — those accounts get distinct random secrets
 *      from Admin; a stale {@code EDITOR_PASSWORD} in .env is ignored when
 *      the cell map is present (#4585).
 *   3. Host-install map.
 *   4. Missing.
 *
 * Never commit secrets. Dev mode may still discover from install passwords file.
 *
 * @param {string} roleUserName e.g. "Admin"
 * @param {NodeJS.ProcessEnv | Record<string, string | undefined>} [env]
 * @param {Record<string, string>} [installPasswords] map of user → password
 * @param {Record<string, string>} [qaCellPasswords] map from generated passwords
 * @returns {{ password: string | null, source: string }}
 */
function resolveRolePassword(
  roleUserName,
  env = process.env,
  installPasswords = {},
  qaCellPasswords = {},
) {
  const role = String(roleUserName);
  const envKey = `${role.toUpperCase()}_PASSWORD`;
  const fromEnv = trimNonEmpty(env[envKey]);
  const fromCell = trimNonEmpty(qaCellPasswords[role]);
  const qaMode = hasQaModeUrlEnv(env);
  const nonAdminQa = qaMode && role !== "Admin" && fromCell;

  if (nonAdminQa) {
    return { password: fromCell, source: "qa-cell" };
  }
  if (fromEnv) {
    return { password: fromEnv, source: envKey };
  }
  if (fromCell) {
    return { password: fromCell, source: "qa-cell" };
  }
  const fromInstall = trimNonEmpty(installPasswords[role]);
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
  parseGeneratedPasswordsText,
  hasQaModeUrlEnv,
  stripTrailingSlash,
  firstEnv,
};
