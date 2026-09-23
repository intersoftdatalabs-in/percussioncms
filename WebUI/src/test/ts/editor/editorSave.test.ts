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
import { editorSaveErrorReason } from "../../../main/ts/editor/editorSave";

describe("editorSaveErrorReason", () => {
  it("maps HTTP 409 to stale", () => {
    expect(
      editorSaveErrorReason({ status: 409, statusText: "Conflict", body: {} }),
    ).toBe("stale");
  });

  it("maps HTTP 400 to badRequest", () => {
    expect(
      editorSaveErrorReason({ status: 400, statusText: "Bad Request", body: {} }),
    ).toBe("badRequest");
  });

  it("maps HTTP 404 and 403 for link targets", () => {
    expect(editorSaveErrorReason({ status: 404, statusText: "Not Found", body: {} })).toBe(
      "notFound",
    );
    expect(editorSaveErrorReason({ status: 403, statusText: "Forbidden", body: {} })).toBe(
      "forbidden",
    );
  });

  it("maps other statuses to failed", () => {
    expect(editorSaveErrorReason(new Error("boom"))).toBe("failed");
  });
});
