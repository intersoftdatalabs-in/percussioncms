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
  buildEditorCreateRequest,
  canCreateFromEditor,
  editorCreateErrorReason,
  editorCreateRequestReady,
  parseCreateLandingContentId,
} from "../../../main/ts/editor/editorCreate";

describe("editorCreate", () => {
  it("allows create in edit and view, not promote", () => {
    expect(canCreateFromEditor("edit")).toBe(true);
    expect(canCreateFromEditor("view")).toBe(true);
    expect(canCreateFromEditor("promote")).toBe(false);
  });

  it("requires type and folder", () => {
    expect(editorCreateRequestReady("", "/Assets")).toBe(false);
    expect(editorCreateRequestReady("percImageAsset", "  ")).toBe(false);
    expect(editorCreateRequestReady("percImageAsset", "/Assets")).toBe(true);
  });

  it("builds a trimmed request", () => {
    expect(buildEditorCreateRequest(" percImageAsset ", " /Assets ", " n ")).toEqual({
      contentType: "percImageAsset",
      folderPath: "/Assets",
      name: "n",
    });
    expect(buildEditorCreateRequest("", "/Assets")).toBeNull();
  });

  it("parses numeric and GUID create result ids", () => {
    expect(parseCreateLandingContentId("88")).toBe(88);
    expect(parseCreateLandingContentId("1-101-88")).toBe(88);
    expect(parseCreateLandingContentId("")).toBeNull();
  });

  it("maps 403 / 404 / 400 as failures, not success", () => {
    expect(
      editorCreateErrorReason({ status: 403, statusText: "Forbidden", body: {} }),
    ).toBe("forbidden");
    expect(
      editorCreateErrorReason({ status: 404, statusText: "Not Found", body: {} }),
    ).toBe("not_found");
    expect(
      editorCreateErrorReason({ status: 400, statusText: "Bad Request", body: {} }),
    ).toBe("bad_request");
    expect(editorCreateErrorReason(new Error("boom"))).toBe("failed");
  });
});
