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
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

const { describe, it } = require("node:test");
const assert = require("node:assert/strict");
const {
  isProductPathUrl,
  isTrackedHttpStatus,
  isFindTypesUrl,
  isFindTemplatesUrl,
  isTrackedFindMenuStatus,
  formatHits,
  isKnownExplorerTransientNetworkConsoleNoise,
} = require("../helpers/explorer-console-clean");

describe("explorer-console-clean helpers (#3458)", () => {
  it("isTrackedHttpStatus only flags 400 and 404", () => {
    assert.equal(isTrackedHttpStatus(400), true);
    assert.equal(isTrackedHttpStatus(404), true);
    assert.equal(isTrackedHttpStatus(200), false);
    assert.equal(isTrackedHttpStatus(500), false);
  });

  it("isFindTypesUrl / isFindTemplatesUrl match Explorer catalog calls (#3855)", () => {
    const base = "http://127.0.0.1:9993";
    assert.equal(
      isFindTypesUrl(`${base}/Rhythmyx/services/actions/find/types`),
      true,
    );
    assert.equal(
      isFindTemplatesUrl(`${base}/Rhythmyx/services/actions/find/templates/551?isAA=false`),
      true,
    );
    assert.equal(
      isFindTemplatesUrl(`${base}/Rhythmyx/services/actions/find/templates/551?isAA=true`),
      true,
    );
    assert.equal(isFindTypesUrl(`${base}/Rhythmyx/services/actions/find`), false);
    assert.equal(isTrackedFindMenuStatus(400), true);
    assert.equal(isTrackedFindMenuStatus(500), true);
    assert.equal(isTrackedFindMenuStatus(200), false);
    assert.equal(isTrackedFindMenuStatus(404), false);
  });

  it("isProductPathUrl keeps CMS services/rest/cm paths", () => {
    const base = "http://127.0.0.1:9993";
    assert.equal(
      isProductPathUrl(
        `${base}/Rhythmyx/services/preferences/perc_profile_gravatar_email`,
        base,
      ),
      true,
    );
    assert.equal(
      isProductPathUrl(`${base}/services/preferences/perc_profile_gravatar_email`, base),
      true,
    );
    assert.equal(
      isProductPathUrl(`${base}/Rhythmyx/services/pathmanagement/path/item/`, base),
      true,
    );
    assert.equal(
      isProductPathUrl(`${base}/Rhythmyx/cm/app/spa.jsp?entry=explorer`, base),
      true,
    );
  });

  it("isProductPathUrl ignores third-party gravatar and other origins", () => {
    const base = "http://127.0.0.1:9993";
    assert.equal(
      isProductPathUrl("https://www.gravatar.com/avatar/abc?s=64&d=404", base),
      false,
    );
    assert.equal(
      isProductPathUrl("https://cdn.example.com/cm/images/x.png", base),
      false,
    );
  });

  it("formatHits prints method status url lines", () => {
    const text = formatHits([
      {
        method: "GET",
        status: 404,
        url: "http://127.0.0.1:9993/Rhythmyx/services/preferences/x",
      },
    ]);
    assert.match(text, /GET 404 .*preferences\/x/);
  });

  it("isKnownExplorerTransientNetworkConsoleNoise covers H2 fixture noise", () => {
    assert.equal(
      isKnownExplorerTransientNetworkConsoleNoise(
        "Failed to load resource: the server responded with a status of 404",
      ),
      true,
    );
    assert.equal(
      isKnownExplorerTransientNetworkConsoleNoise("net::ERR_FAILED"),
      true,
    );
    assert.equal(
      isKnownExplorerTransientNetworkConsoleNoise(
        "Access to fetch at 'http://x' has been blocked by CORS policy",
      ),
      true,
    );
    assert.equal(
      isKnownExplorerTransientNetworkConsoleNoise("Mixed Content: the page was loaded over HTTPS"),
      true,
    );
    assert.equal(
      isKnownExplorerTransientNetworkConsoleNoise("net::ERR_CERT_AUTHORITY_INVALID"),
      true,
    );
    assert.equal(
      isKnownExplorerTransientNetworkConsoleNoise("Uncaught TypeError: boom"),
      false,
    );
  });
});
