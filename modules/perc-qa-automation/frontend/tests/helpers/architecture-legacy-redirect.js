/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Architecture bookmark / ?view=arch console-clean helpers (#3612).
 *
 * Records HTTP 5xx with method+URL so a failed gate names the request
 * (typically GET /sitemanage/section/tree/{site}). Does not allowlist 500.
 */

"use strict";

/**
 * @param {number} status
 * @returns {boolean}
 */
function isHttp5xx(status) {
  return Number(status) >= 500 && Number(status) <= 599;
}

/**
 * True when the URL is a section-tree GET used by Architecture on load.
 *
 * @param {string} url
 * @returns {boolean}
 */
function isSectionTreeRequestUrl(url) {
  return /\/sitemanage\/section\/tree\//i.test(String(url || ""));
}

/**
 * @param {{method?:string,status?:number,url?:string}[]} hits
 * @returns {string[]}
 */
function formatHttp5xxHits(hits) {
  return (Array.isArray(hits) ? hits : []).map((h) => {
    const method = String((h && h.method) || "GET");
    const status = String((h && h.status) || "");
    const url = String((h && h.url) || "");
    return `${method} ${status} ${url}`;
  });
}

/**
 * Attach pageerror / console-error / HTTP 5xx collectors.
 * Does not allowlist 500 or "Failed to load resource".
 *
 * @param {import("@playwright/test").Page} page
 * @returns {{
 *   pageErrors: string[],
 *   consoleErrors: string[],
 *   http5xx: {method:string,status:number,url:string}[]
 * }}
 */
function attachConsoleCleanGate(page) {
  const pageErrors = [];
  const consoleErrors = [];
  const http5xx = [];
  page.on("pageerror", (err) => {
    pageErrors.push(String(err && err.message ? err.message : err));
  });
  page.on("console", (msg) => {
    if (msg.type() === "error") {
      consoleErrors.push(msg.text());
    }
  });
  page.on("response", (res) => {
    const status = res.status();
    if (!isHttp5xx(status)) {
      return;
    }
    http5xx.push({
      method: res.request().method(),
      status,
      url: res.url(),
    });
  });
  return { pageErrors, consoleErrors, http5xx };
}

module.exports = {
  isHttp5xx,
  isSectionTreeRequestUrl,
  formatHttp5xxHits,
  attachConsoleCleanGate,
};
