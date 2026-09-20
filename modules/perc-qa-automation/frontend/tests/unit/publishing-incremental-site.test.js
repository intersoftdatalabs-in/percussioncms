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
  isIncrementalPublishUrl,
  isPubServersListUrl,
  isIncrementalPreviewUrl,
  isIncrementalConfirmMessage,
  mockPublishServer,
  mockIncrementalPublishResponse,
  isKnownPublishConsoleNoise,
} = require("../helpers/publishing-incremental-site");

describe("publishing-incremental-site helpers (#4614)", () => {
  it("builds the Publish SPA entry URL", () => {
    assert.equal(
      publishingProductUrl("http://127.0.0.1:9993/"),
      "http://127.0.0.1:9993/cm/app/spa.jsp?entry=publish",
    );
  });

  it("matches incremental publish REST paths", () => {
    assert.equal(
      isIncrementalPublishUrl(
        "/Rhythmyx/services/sitemanage/publish/incremental/publish/MySite/FTP",
      ),
      true,
    );
    assert.equal(isIncrementalPublishUrl("/sitemanage/pubstatus/logs"), false);
  });

  it("matches publish-server list URLs", () => {
    assert.equal(
      isPubServersListUrl("/Rhythmyx/services/publishmanagement/servers/42"),
      true,
    );
    assert.equal(
      isPubServersListUrl(
        "/Rhythmyx/services/publishmanagement/servers/42/4614",
      ),
      false,
    );
  });

  it("matches incremental preview URLs", () => {
    assert.equal(
      isIncrementalPreviewUrl(
        "/Rhythmyx/services/sitemanage/publish/incremental/content/MySite/FTP",
      ),
      true,
    );
    assert.equal(
      isIncrementalPreviewUrl(
        "/Rhythmyx/services/sitemanage/publish/incremental/relatedcontent/MySite/FTP",
      ),
      true,
    );
    assert.equal(
      isIncrementalPreviewUrl(
        "/Rhythmyx/services/sitemanage/publish/incremental/publish/MySite/FTP",
      ),
      false,
    );
  });

  it("recognizes incremental confirm copy", () => {
    assert.equal(
      isIncrementalConfirmMessage("Confirm Incremental Publish"),
      true,
    );
    assert.equal(isIncrementalConfirmMessage("Stop this job?"), false);
  });

  it("builds mock server and incremental job payloads", () => {
    assert.equal(mockPublishServer().serverName, "FTP-Prod");
    assert.equal(
      mockIncrementalPublishResponse().SitePublishResponse.jobid,
      4614,
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
