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

module.exports = {
  isOffLoginPath,
  classifyLoginSurface,
  loginUiWaitSelector,
};
