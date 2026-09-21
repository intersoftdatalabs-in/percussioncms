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
import { editorBinaryErrorReason } from "../../../main/ts/editor/editorBinary";

describe("editorBinaryErrorReason", () => {
  it("maps 403, 413, and 400", () => {
    expect(
      editorBinaryErrorReason({ status: 403, statusText: "Forbidden", body: {} }),
    ).toBe("forbidden");
    expect(
      editorBinaryErrorReason({
        status: 413,
        statusText: "Payload Too Large",
        body: {},
      }),
    ).toBe("tooLarge");
    expect(
      editorBinaryErrorReason({ status: 400, statusText: "Bad Request", body: {} }),
    ).toBe("badRequest");
  });

  it("maps other statuses to failed", () => {
    expect(
      editorBinaryErrorReason({ status: 500, statusText: "Error", body: {} }),
    ).toBe("failed");
    expect(editorBinaryErrorReason(new Error("boom"))).toBe("failed");
  });
});
