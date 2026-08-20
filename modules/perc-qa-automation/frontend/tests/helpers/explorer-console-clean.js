/**
 * Explorer product-path 404/400 collectors (#3458 / parent #2745).
 *
 * <p>Human QA failed console-clean independently of the empty Pages list.
 * This helper records same-origin 400/404 responses so Playwright can
 * assert the Explorer session is console-clean. It does <em>not</em>
 * blanket-ignore resource 404/400.</p>
 */

"use strict";

const {
  TEST_IDS,
  explorerSpaUrl,
} = require("./explorer-shell-chrome");

/**
 * True when {@code url} is a product (same-origin) request we must keep
 * console-clean. Third-party hosts (Gravatar) are out of product-path scope.
 *
 * @param {string} url
 * @param {string} baseUrl
 * @returns {boolean}
 */
function isProductPathUrl(url, baseUrl) {
  const raw = String(url || "");
  if (!raw) {
    return false;
  }
  let host = "";
  let path = raw;
  try {
    const parsed = new URL(raw, String(baseUrl || "http://127.0.0.1/"));
    host = parsed.host;
    path = parsed.pathname || "";
    if (baseUrl) {
      const origin = new URL(String(baseUrl)).host;
      if (host && origin && host !== origin) {
        return false;
      }
    }
  } catch {
    // Relative or malformed — treat as product if it looks like CMS.
  }
  if (/gravatar\.com/i.test(raw) || /chrome-extension:/i.test(raw)) {
    return false;
  }
  return (
    /\/Rhythmyx\//i.test(path) ||
    /\/services\//i.test(path) ||
    /\/rest\//i.test(path) ||
    /\/cm\//i.test(path)
  );
}

/**
 * @param {number} status
 * @returns {boolean}
 */
function isTrackedHttpStatus(status) {
  return status === 400 || status === 404;
}

/**
 * Attach response + pageerror collectors for product 400/404.
 *
 * @param {import("@playwright/test").Page} page
 * @param {string} baseUrl
 * @returns {{ hits: {status:number, method:string, url:string}[], pageErrors: string[] }}
 */
function attachProductStatusCollector(page, baseUrl) {
  const hits = [];
  const pageErrors = [];
  page.on("pageerror", (err) => {
    pageErrors.push(String(err && err.message ? err.message : err));
  });
  page.on("response", (res) => {
    const status = res.status();
    if (!isTrackedHttpStatus(status)) {
      return;
    }
    const url = res.url();
    if (!isProductPathUrl(url, baseUrl)) {
      return;
    }
    hits.push({
      status,
      method: res.request().method(),
      url,
    });
  });
  return { hits, pageErrors };
}

function formatHits(hits) {
  return (hits || [])
    .map((h) => `${h.method || "GET"} ${h.status} ${h.url}`)
    .join("\n");
}

/**
 * Transient browser/network console noise on thin H2 Explorer fixtures.
 * Does not swallow uncaught {@code pageerror} / TypeError.
 *
 * @param {string | null | undefined} text
 * @returns {boolean}
 */
function isKnownExplorerTransientNetworkConsoleNoise(text) {
  return /favicon|Download the React DevTools|ResizeObserver|third-party|Failed to load resource|net::ERR_|CORS|Access-Control-Allow-Origin|ERR_CERT|certificate|SSL handshake|mixed content|Mixed Content/i.test(
    String(text || ""),
  );
}

module.exports = {
  TEST_IDS,
  explorerSpaUrl,
  isProductPathUrl,
  isTrackedHttpStatus,
  attachProductStatusCollector,
  formatHits,
  isKnownExplorerTransientNetworkConsoleNoise,
};
