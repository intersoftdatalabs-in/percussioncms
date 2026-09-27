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
  baseUrlsMatch,
  isValidSiteBaseUrl,
  siteBaseUrlHttpFailure,
} from "@/publishing/siteBaseUrl";

describe("siteBaseUrl", () => {
  it("accepts absolute http(s) URLs and rejects empty or non-http values", () => {
    expect(isValidSiteBaseUrl("https://www.example.com/site")).toBe(true);
    expect(isValidSiteBaseUrl("  http://localhost:8080/  ")).toBe(true);
    expect(isValidSiteBaseUrl("")).toBe(false);
    expect(isValidSiteBaseUrl("   ")).toBe(false);
    expect(isValidSiteBaseUrl("not a url")).toBe(false);
    expect(isValidSiteBaseUrl("javascript:alert(1)")).toBe(false);
    expect(isValidSiteBaseUrl("//evil.example/x")).toBe(false);
    expect(isValidSiteBaseUrl("ftp://files.example/pub")).toBe(false);
  });

  it("treats trim-equal values as unchanged", () => {
    expect(baseUrlsMatch("https://a.example", "  https://a.example  ")).toBe(true);
    expect(baseUrlsMatch("https://a.example", "https://b.example")).toBe(false);
  });

  it("maps 400, 403, and 409 and ignores other statuses", () => {
    expect(siteBaseUrlHttpFailure(400)).toBe("bad_request");
    expect(siteBaseUrlHttpFailure(403)).toBe("forbidden");
    expect(siteBaseUrlHttpFailure(409)).toBe("conflict");
    expect(siteBaseUrlHttpFailure(500)).toBeNull();
  });
});
