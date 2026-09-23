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
import { formatSiteCopyError } from "../../../main/ts/contentExplorer/siteCopyErrors";

describe("formatSiteCopyError", () => {
  it("maps 400, 403, and 409 to operator text", () => {
    expect(formatSiteCopyError({ status: 400, statusText: "Bad Request", body: null })).toContain(
      "HTTP 400",
    );
    expect(formatSiteCopyError({ status: 403, statusText: "Forbidden", body: null })).toContain(
      "HTTP 403",
    );
    expect(formatSiteCopyError({ status: 409, statusText: "Conflict", body: null })).toContain(
      "HTTP 409",
    );
  });

  it("does not treat a transport error as success text", () => {
    expect(formatSiteCopyError(new Error("network down"))).toContain("network down");
  });
});
