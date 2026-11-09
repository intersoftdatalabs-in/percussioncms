/**
 * Pure login-page surface classification for Playwright auth helpers (#3492).
 *
 * Hidden {@code perc-login-root} is the empty rxlogin.jsp mount (0×0 until
 * React paints {@code perc-login-form}). Waiting on that root burns 30s
 * even when the session already left /login or the SPA/assembly host is up.
 *
 * No live CMS. Unit: {@code tests/unit/login-surface.test.js}.
 */

"use strict";

/**
 * @typedef {object} LoginSurfaceSnapshot
 * @property {string} [url]
 * @property {string} [pathname]
 * @property {boolean} [formVisible]
 * @property {boolean} [legacyVisible]
 * @property {boolean} [spaVisible]
 * @property {boolean} [assemblyVisible]
 * @property {boolean} [rootPresent]
 * @property {boolean} [rootVisible]
 */

/**
 * @typedef {object} LoginSurfaceDecision
 * @property {"already_authenticated"|"modern_form"|"legacy_form"|"pending"} kind
 * @property {string} reason
 */

/**
 * True when the browser is no longer on the CMS login path.
 *
 * @param {string} urlOrPath href or pathname
 * @returns {boolean}
 */
function isOffLoginPath(urlOrPath) {
  const raw = String(urlOrPath || "").trim();
  if (!raw) {
    return false;
  }
  let path = raw;
  try {
    if (/^https?:\/\//i.test(raw)) {
      path = new URL(raw).pathname;
    }
  } catch {
    path = raw.split("?")[0].split("#")[0];
  }
  const p = path.replace(/\/+$/, "") || "/";
  return !p.endsWith("/Rhythmyx/login") && !p.endsWith("/login");
}

/**
 * Classify the post-goto login page.
 *
 * Hidden {@code perc-login-root} is never the login UI. Prefer a visible
 * form (modern or legacy). If the URL already left /login or the SPA /
 * assembly host is visible, treat as already authenticated.
 *
 * @param {LoginSurfaceSnapshot} snap
 * @returns {LoginSurfaceDecision}
 */
function classifyLoginSurface(snap) {
  const s = snap || {};
  if (s.formVisible) {
    return { kind: "modern_form", reason: "perc-login-form-visible" };
  }
  if (s.legacyVisible) {
    return { kind: "legacy_form", reason: "j_username-visible" };
  }
  if (s.spaVisible) {
    return { kind: "already_authenticated", reason: "perc-spa-app-visible" };
  }
  if (s.assemblyVisible) {
    return { kind: "already_authenticated", reason: "assembly-host-visible" };
  }
  const path = s.pathname || s.url || "";
  if (path && isOffLoginPath(path)) {
    return { kind: "already_authenticated", reason: "left-login-path" };
  }
  if (s.rootPresent && !s.rootVisible) {
    return { kind: "pending", reason: "hidden-perc-login-root" };
  }
  return { kind: "pending", reason: "waiting-for-login-ui" };
}

/**
 * CSS selector union used when waiting for a real login or authenticated
 * surface. Intentionally omits {@code perc-login-root}.
 *
 * @returns {string}
 */
function loginUiWaitSelector() {
  return [
    '[data-testid="perc-login-form"]',
    'input[name="j_username"]',
    '[data-testid="perc-spa-app"]',
    '[data-testid="assembly-host"]',
  ].join(", ");
}

/**
 * Default form-login attempts per {@code login()} call (#5937).
 *
 * The QA-cell login servlet intermittently answers a correct native POST
 * with HTTP 200 + a server-side auth failure (H2 BackEndTable /
 * CSRF-session flake) instead of the 302 away from /login. One retry with
 * a freshly rendered login page absorbs that flake; two retries bound the
 * worst case for a genuinely wedged cell.
 */
const DEFAULT_LOGIN_ATTEMPTS = 3;

/**
 * Attempts for the login retry loop. Override with
 * {@code PERC_QA_LOGIN_ATTEMPTS} ({@code 1} restores single-attempt
 * behavior for forensics). Non-numeric / out-of-range values fall back
 * to {@link DEFAULT_LOGIN_ATTEMPTS}.
 *
 * @param {NodeJS.ProcessEnv} [env]
 * @returns {number} attempts (>= 1)
 */
function resolveLoginAttempts(env) {
  const raw = env && env.PERC_QA_LOGIN_ATTEMPTS;
  if (raw === undefined || raw === null || String(raw).trim() === "") {
    return DEFAULT_LOGIN_ATTEMPTS;
  }
  const parsed = Number.parseInt(String(raw).trim(), 10);
  if (!Number.isFinite(parsed) || parsed < 1) {
    return DEFAULT_LOGIN_ATTEMPTS;
  }
  return Math.min(parsed, 10);
}

/**
 * Stagger before login retry {@code attemptIndex} (0-based): exponential
 * backoff with jitter so back-to-back runner logins do not hammer the
 * QA-cell auth path in lockstep.
 *
 * @param {number} attemptIndex 0 for the wait before the 2nd attempt
 * @returns {number} milliseconds in
 *   {@code [1500 * 2^attemptIndex, 1500 * 2^attemptIndex + 750]}, capped
 */
function loginRetryDelayMs(attemptIndex) {
  const idx =
    Number.isFinite(attemptIndex) && attemptIndex > 0
      ? Math.min(Math.floor(attemptIndex), 4)
      : 0;
  const base = Math.min(1500 * 2 ** idx, 8000);
  const jitter = Math.floor(Math.random() * 751);
  return base + jitter;
}

/**
 * Server-side auth-failure markers rendered into the login page (or the
 * login POST document) when the cell rejects the credentials/session
 * after a syntactically correct POST (#5937).
 */
const LOGIN_AUTH_FAILURE_PATTERNS = [
  /could not authenticate/i,
  /loginexception/i,
  /invalid (username|password|login|credentials)/i,
  /authentication failed/i,
  /login failed/i,
  /session (expired|invalid|timed out)/i,
];

/**
 * True when {@code text} carries a server-side auth-failure marker (as
 * opposed to a runner-side fill/navigation problem).
 *
 * @param {string} text banner / page text (may be empty)
 * @returns {boolean}
 */
function isServerAuthFailureText(text) {
  const raw = String(text || "");
  if (!raw.trim()) {
    return false;
  }
  return LOGIN_AUTH_FAILURE_PATTERNS.some((re) => re.test(raw));
}

/**
 * Final login-failure message: attempt count, last URL, last login-POST
 * status, and the server banner when present — enough to tell a QA-cell
 * auth flake (200 + server banner, correct creds) from a runner-side
 * problem without re-running under trace.
 *
 * @param {{attempts?: number, url?: string, errorBanner?: string,
 *   lastPostStatus?: number|null}} [detail]
 * @returns {string}
 */
function summarizeLoginFailure(detail) {
  const d = detail || {};
  const attempts =
    Number.isFinite(d.attempts) && d.attempts > 0
      ? d.attempts
      : DEFAULT_LOGIN_ATTEMPTS;
  const url = d.url || "(unknown url)";
  const status =
    d.lastPostStatus === null ||
    d.lastPostStatus === undefined ||
    !Number.isFinite(d.lastPostStatus)
      ? "not observed"
      : String(d.lastPostStatus);
  const banner = String(d.errorBanner || "").trim();
  const bannerPart = banner
    ? ` Server banner: "${banner.slice(0, 200)}"${isServerAuthFailureText(banner) ? " (server-side auth rejection)" : ""}.`
    : " No server error banner text captured.";
  return (
    `Login did not navigate away from login page (still at ${url})` +
    ` after ${attempts} attempt(s).` +
    ` Last login POST status: ${status}.` +
    bannerPart +
    ` A 200 POST + server banner with known-good creds points at the` +
    ` QA-cell auth path (H2 BackEndTable / CSRF session, #5937), not the` +
    ` spec. Tune with PERC_QA_LOGIN_ATTEMPTS=1 for single-attempt forensics.`
  );
}

module.exports = {
  isOffLoginPath,
  classifyLoginSurface,
  loginUiWaitSelector,
  DEFAULT_LOGIN_ATTEMPTS,
  resolveLoginAttempts,
  loginRetryDelayMs,
  isServerAuthFailureText,
  summarizeLoginFailure,
};
