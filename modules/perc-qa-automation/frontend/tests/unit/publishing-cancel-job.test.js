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

"use strict";

const { describe, it } = require("node:test");
const assert = require("node:assert/strict");
const {
  publishingProductUrl,
  isStopPublishingUrl,
  isCurrentStatusUrl,
  isKnownPublishConsoleNoise,
} = require("../helpers/publishing-cancel-job");

describe("publishing-cancel-job helpers (#4615)", () => {
  it("builds the Publish SPA entry URL", () => {
    assert.equal(
      publishingProductUrl("http://127.0.0.1:9993/"),
      "http://127.0.0.1:9993/cm/app/spa.jsp?entry=publish&section=status",
    );
  });

  it("matches stopPublishing REST paths", () => {
    assert.equal(
      isStopPublishingUrl(
        "/Rhythmyx/services/publishmanagement/servers/stopPublishing/4615",
      ),
      true,
    );
    assert.equal(isStopPublishingUrl("/sitemanage/pubstatus/current"), false);
  });

  it("matches current status paths", () => {
    assert.equal(
      isCurrentStatusUrl("/Rhythmyx/services/sitemanage/pubstatus/current"),
      true,
    );
  });

  it("filters known console noise", () => {
    assert.equal(
      isKnownPublishConsoleNoise("Download the React DevTools"),
      true,
    );
    assert.equal(isKnownPublishConsoleNoise("TypeError: boom"), false);
    assert.equal(
      isKnownPublishConsoleNoise(
        "Failed to load resource: the server responded with a status of 403 (Forbidden)",
      ),
      true,
    );
  });
});
