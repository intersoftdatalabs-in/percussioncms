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
  canCopyFromEditor,
  editorCopyErrorReason,
  parseCopyLandingContentId,
} from "../../../main/ts/editor/editorCopy";

describe("editorCopy", () => {
  it("allows copy only in edit mode", () => {
    expect(canCopyFromEditor("edit")).toBe(true);
    expect(canCopyFromEditor("view")).toBe(false);
    expect(canCopyFromEditor("promote")).toBe(false);
  });

  it("parses numeric and GUID copy result ids", () => {
    expect(parseCopyLandingContentId("99")).toBe(99);
    expect(parseCopyLandingContentId("1-101-77")).toBe(77);
    expect(parseCopyLandingContentId("")).toBeNull();
  });

  it("maps 403 and 404 as forbidden / not found, not success", () => {
    expect(editorCopyErrorReason({ status: 403, statusText: "Forbidden", body: {} })).toBe(
      "forbidden",
    );
    expect(editorCopyErrorReason({ status: 404, statusText: "Not Found", body: {} })).toBe(
      "not_found",
    );
    expect(editorCopyErrorReason(new Error("boom"))).toBe("failed");
  });
});
