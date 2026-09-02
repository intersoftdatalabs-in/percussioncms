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
 * Live Developer Sites Virtual Site Save helper (#4174).
 *
 * Cycle Verify timed out waiting for
 * ``[data-testid=developer-site-virtual-saved]`` when PUT
 * ``/services/sites/{name}/virtual`` failed (stale skip-image-build
 * perc-system without sitemap-xml allow-list). Waiting on the PUT
 * response surfaces HTTP 400 body instead of a 15–20s banner miss.
 */

"use strict";

const { expect } = require("@playwright/test");

/**
 * True when ``url`` is the Virtual Site properties PUT (not build/preview/publish).
 *
 * @param {string} url request URL
 * @param {string} [method=PUT]
 * @returns {boolean}
 */
function isVirtualSitePropertiesPut(url, method) {
  if (String(method || "PUT").toUpperCase() !== "PUT") {
    return false;
  }
  const raw = String(url || "");
  let pathname = raw;
  try {
    pathname = new URL(raw).pathname;
  } catch {
    pathname = raw.split("?")[0];
  }
  return /\/services\/sites\/[^/]+\/virtual\/?$/.test(pathname);
}

/**
 * Click Save, require PUT /virtual HTTP 2xx, then wait for the saved banner.
 *
 * @param {import("@playwright/test").Page} page
 * @param {{ timeout?: number }} [opts]
 * @returns {Promise<import("@playwright/test").Response>}
 */
async function saveVirtualSiteAndExpectSaved(page, opts) {
  const timeout = opts && opts.timeout != null ? opts.timeout : 20_000;
  const putPromise = page.waitForResponse(
    (resp) => isVirtualSitePropertiesPut(resp.url(), resp.request().method()),
    { timeout },
  );
  await page.locator('[data-testid="developer-site-virtual-save"]').click();
  const putResp = await putPromise;
  const body = await putResp.text();
  if (!putResp.ok()) {
    let uiErr = "";
    try {
      uiErr =
        (await page.locator('[data-testid="developer-site-virtual-error"]').textContent()) ||
        "";
    } catch {
      uiErr = "";
    }
    throw new Error(
      `PUT /services/sites/{name}/virtual HTTP ${putResp.status()}: ${body} ui=${uiErr}`,
    );
  }
  await expect(page.locator('[data-testid="developer-site-virtual-saved"]')).toBeVisible({
    timeout,
  });
  return putResp;
}

module.exports = {
  isVirtualSitePropertiesPut,
  saveVirtualSiteAndExpectSaved,
};
