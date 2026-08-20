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
 * Unit tests for architecture-legacy-redirect helpers (#3612) — no live CMS.
 */

"use strict";

const { describe, it } = require("node:test");
const assert = require("node:assert/strict");
const {
  isHttp5xx,
  isSectionTreeRequestUrl,
  formatHttp5xxHits,
} = require("../helpers/architecture-legacy-redirect");

describe("architecture-legacy-redirect helpers (#3612)", () => {
  it("isHttp5xx flags only 5xx (does not allowlist 500)", () => {
    assert.equal(isHttp5xx(500), true);
    assert.equal(isHttp5xx(503), true);
    assert.equal(isHttp5xx(200), false);
    assert.equal(isHttp5xx(404), false);
    assert.equal(isHttp5xx(499), false);
  });

  it("isSectionTreeRequestUrl matches sitemanage section tree GET", () => {
    assert.equal(
      isSectionTreeRequestUrl(
        "http://127.0.0.1:9993/Rhythmyx/services/sitemanage/section/tree/Corporate_Investments",
      ),
      true,
    );
    assert.equal(
      isSectionTreeRequestUrl(
        "http://127.0.0.1:9993/Rhythmyx/services/sitemanage/site/",
      ),
      false,
    );
  });

  it("formatHttp5xxHits prints method status url so the failing request is named", () => {
    const lines = formatHttp5xxHits([
      {
        method: "GET",
        status: 500,
        url: "http://127.0.0.1:9993/Rhythmyx/services/sitemanage/section/tree/Corporate_Investments",
      },
    ]);
    assert.equal(lines.length, 1);
    assert.match(lines[0], /^GET 500 /);
    assert.match(lines[0], /section\/tree\/Corporate_Investments/);
  });
});
