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

import { describe, expect, it } from "vitest";
import {
  isValidSiteProtocol,
  protocolsMatch,
  siteProtocolHttpFailure,
} from "@/publishing/siteProtocol";

describe("site protocol edit (#5001)", () => {
  it("accepts only http and https, ignoring case and surrounding space", () => {
    expect(isValidSiteProtocol(" https ")).toBe(true);
    expect(isValidSiteProtocol("HTTP")).toBe(true);
    expect(isValidSiteProtocol("ftp")).toBe(false);
    expect(isValidSiteProtocol("")).toBe(false);
  });

  it("treats the same protocol as unchanged and rejects a switch", () => {
    expect(protocolsMatch("HTTPS", " https ")).toBe(true);
    expect(protocolsMatch("http", "https")).toBe(false);
    expect(protocolsMatch("", "https")).toBe(false);
  });

  it("keeps 400, 403, and 409 on the failure path", () => {
    expect(siteProtocolHttpFailure(400)).toBe("bad_request");
    expect(siteProtocolHttpFailure(403)).toBe("forbidden");
    expect(siteProtocolHttpFailure(409)).toBe("conflict");
    expect(siteProtocolHttpFailure(500)).toBeNull();
  });
});
