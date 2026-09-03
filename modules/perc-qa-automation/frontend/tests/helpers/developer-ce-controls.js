/**
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
 * Developer CE Controls write helpers (#4215 / UI-01).
 * Shared by Playwright surface spec and node:test unit tests.
 */

"use strict";

/**
 * REST-safe unique control name (no spaces or wildcards).
 *
 * @param {string} prefix
 * @returns {string}
 */
function uniqueControlName(prefix) {
  const a = Date.now().toString(36).replace(/[^a-z0-9]/g, "").slice(-4);
  const b = Math.random().toString(36).replace(/[^a-z0-9]/g, "").slice(2, 6);
  const suffix = `${a}${b}`.slice(0, 8);
  return `${prefix}${suffix || "x"}`;
}

/**
 * Same-origin CE control REST path (URL path uses `/`).
 *
 * @param {string} name
 * @returns {string}
 */
function ceControlPath(name) {
  return `/Rhythmyx/services/cecontrols/${encodeURIComponent(name)}`;
}

/**
 * Match a Playwright response to CE-controls POST (catalog) or named PUT/DELETE/GET.
 *
 * @param {{ request: () => { method: () => string }, url: () => string }} response
 * @param {string} method
 * @param {string} [name]
 * @returns {boolean}
 */
function isCeControlsResponse(response, method, name) {
  if (response.request().method() !== method) {
    return false;
  }
  return isCeControlsUrl(response.url(), method, name);
}

/**
 * @param {string} url
 * @param {string} method
 * @param {string} [name]
 * @returns {boolean}
 */
function isCeControlsUrl(url, method, name) {
  const marker = `/services/cecontrols`;
  if (!url.includes(marker)) {
    return false;
  }
  if (!name) {
    return method === "POST";
  }
  return url.includes(encodeURIComponent(name)) || url.includes(`/${name}`);
}

module.exports = {
  uniqueControlName,
  ceControlPath,
  isCeControlsResponse,
  isCeControlsUrl,
};
