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
  canRecycleFromEditor,
  editorRecycleErrorReason,
  editorRecycleItemPath,
} from "../../../main/ts/editor/editorRecycle";

describe("editorRecycle", () => {
  it("allows recycle only in edit mode", () => {
    expect(canRecycleFromEditor("edit")).toBe(true);
    expect(canRecycleFromEditor("view")).toBe(false);
    expect(canRecycleFromEditor("promote")).toBe(false);
  });

  it("uses a non-folder item path and refuses folders", () => {
    expect(
      editorRecycleItemPath({ path: "//Sites/Demo/Home", type: "percPage" }),
    ).toEqual({ ok: true, path: "//Sites/Demo/Home" });
    expect(
      editorRecycleItemPath({ path: "//Sites/Demo/", type: "Folder" }),
    ).toEqual({ ok: false, reason: "folder" });
    expect(editorRecycleItemPath({ path: "  ", type: "percPage" })).toEqual({
      ok: false,
      reason: "not_found",
    });
    expect(editorRecycleItemPath(null)).toEqual({
      ok: false,
      reason: "not_found",
    });
  });

  it("maps 403, 404, and 409 as failures", () => {
    expect(
      editorRecycleErrorReason({ status: 403, statusText: "Forbidden", body: {} }),
    ).toBe("forbidden");
    expect(
      editorRecycleErrorReason({ status: 404, statusText: "Not Found", body: {} }),
    ).toBe("not_found");
    expect(
      editorRecycleErrorReason({ status: 409, statusText: "Conflict", body: {} }),
    ).toBe("conflict");
    expect(editorRecycleErrorReason(new Error("boom"))).toBe("failed");
  });
});