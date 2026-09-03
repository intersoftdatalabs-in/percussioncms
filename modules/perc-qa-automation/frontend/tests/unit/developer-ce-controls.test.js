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
 * Unit tests for Developer CE Controls write helpers (#4215).
 *
 * Run: npm run test:unit  (from frontend/)
 */

"use strict";

const { describe, it } = require("node:test");
const assert = require("node:assert/strict");
const {
  uniqueControlName,
  ceControlPath,
  isCeControlsUrl,
  isCeControlsResponse,
} = require("../helpers/developer-ce-controls");

describe("uniqueControlName", () => {
  it("prefixes a REST-safe alphanumeric suffix", () => {
    const name = uniqueControlName("qa4215");
    assert.match(name, /^qa4215[a-z0-9]+$/);
    assert.ok(name.length > "qa4215".length);
    assert.ok(!name.includes(" "));
    assert.ok(!name.includes("*"));
  });
});

describe("ceControlPath", () => {
  it("uses URL-path slashes and encodes the name", () => {
    assert.equal(
      ceControlPath("sys_EditBox"),
      "/Rhythmyx/services/cecontrols/sys_EditBox",
    );
    assert.equal(
      ceControlPath("qa 4215"),
      "/Rhythmyx/services/cecontrols/qa%204215",
    );
  });
});

describe("isCeControlsUrl", () => {
  it("matches POST catalog without a name", () => {
    assert.equal(
      isCeControlsUrl("http://127.0.0.1:9993/Rhythmyx/services/cecontrols", "POST"),
      true,
    );
    assert.equal(
      isCeControlsUrl("http://127.0.0.1:9993/Rhythmyx/services/cecontrols", "GET"),
      false,
    );
  });

  it("matches named PUT/DELETE including encoded names", () => {
    const path = "http://127.0.0.1:1/Rhythmyx/services/cecontrols/qa4215abcd";
    assert.equal(isCeControlsUrl(path, "PUT", "qa4215abcd"), true);
    assert.equal(isCeControlsUrl(path, "DELETE", "qa4215abcd"), true);
    assert.equal(isCeControlsUrl(path, "PUT", "other"), false);
    assert.equal(
      isCeControlsUrl(
        "http://127.0.0.1:1/Rhythmyx/services/cecontrols/qa%204215",
        "GET",
        "qa 4215",
      ),
      true,
    );
  });

  it("rejects unrelated REST families", () => {
    assert.equal(
      isCeControlsUrl("http://127.0.0.1:1/Rhythmyx/services/locales", "POST"),
      false,
    );
  });
});

describe("isCeControlsResponse", () => {
  function fakeResponse(method, url) {
    return {
      request: () => ({ method: () => method }),
      url: () => url,
    };
  }

  it("requires both method and cecontrols URL", () => {
    const url = "http://127.0.0.1:1/Rhythmyx/services/cecontrols/sys_EditBox";
    assert.equal(isCeControlsResponse(fakeResponse("DELETE", url), "DELETE", "sys_EditBox"), true);
    assert.equal(isCeControlsResponse(fakeResponse("PUT", url), "DELETE", "sys_EditBox"), false);
  });
});
