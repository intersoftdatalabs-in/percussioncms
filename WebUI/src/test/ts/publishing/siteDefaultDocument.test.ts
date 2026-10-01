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
  defaultDocumentsMatch,
  isNonEmptyDefaultDocument,
  siteDefaultDocumentHttpFailure,
} from "@/publishing/siteDefaultDocument";

describe("siteDefaultDocument", () => {
  it("rejects an empty default document because PUT ignores blank", () => {
    expect(isNonEmptyDefaultDocument("index.html")).toBe(true);
    expect(isNonEmptyDefaultDocument("  home.htm  ")).toBe(true);
    expect(isNonEmptyDefaultDocument("")).toBe(false);
    expect(isNonEmptyDefaultDocument("   ")).toBe(false);
  });

  it("treats trim-equal values as unchanged", () => {
    expect(defaultDocumentsMatch("index.html", "  index.html  ")).toBe(true);
    expect(defaultDocumentsMatch("index.html", "home.html")).toBe(false);
  });

  it("maps 400, 403, and 409 and ignores other statuses", () => {
    expect(siteDefaultDocumentHttpFailure(400)).toBe("bad_request");
    expect(siteDefaultDocumentHttpFailure(403)).toBe("forbidden");
    expect(siteDefaultDocumentHttpFailure(409)).toBe("conflict");
    expect(siteDefaultDocumentHttpFailure(500)).toBeNull();
  });
});
