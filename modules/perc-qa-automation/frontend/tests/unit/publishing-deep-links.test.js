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
  publishingLogsJobUrl,
  publishingStatusRequestUrl,
  isPubstatusCurrentUrl,
  isPubstatusLogsUrl,
  isPubstatusDetailsUrl,
  isKnownPublishConsoleNoise,
} = require("../helpers/publishing-deep-links");

describe("publishing-deep-links helpers (#6215)", () => {
  it("builds the Logs jobId deep-link URL", () => {
    assert.equal(
      publishingLogsJobUrl("http://127.0.0.1:9993/", 4616),
      "http://127.0.0.1:9993/cm/app/spa.jsp?entry=publish&section=logs&jobId=4616",
    );
  });

  it("builds the Status requestId deep-link URL", () => {
    assert.equal(
      publishingStatusRequestUrl("http://127.0.0.1:9993/", 42),
      "http://127.0.0.1:9993/cm/app/spa.jsp?entry=publish&section=status&requestId=42",
    );
  });

  it("matches pubstatus endpoint paths", () => {
    assert.equal(
      isPubstatusCurrentUrl("/Rhythmyx/services/sitemanage/pubstatus/current"),
      true,
    );
    assert.equal(
      isPubstatusLogsUrl("/Rhythmyx/services/sitemanage/pubstatus/logs"),
      true,
    );
    assert.equal(
      isPubstatusDetailsUrl(
        "/Rhythmyx/services/sitemanage/pubstatus/details",
      ),
      true,
    );
    assert.equal(
      isPubstatusCurrentUrl("/sitemanage/pubstatus/logs"),
      false,
    );
  });

  it("filters known console noise", () => {
    assert.equal(
      isKnownPublishConsoleNoise("Download the React DevTools"),
      true,
    );
    assert.equal(isKnownPublishConsoleNoise("TypeError: boom"), false);
  });
});
