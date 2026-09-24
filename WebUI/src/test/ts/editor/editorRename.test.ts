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
  buildRenameItemPath,
  editorItemPathFromLookup,
  canRenameFromEditor,
  editorListingName,
  editorRenameErrorReason,
  renameLanded,
} from "../../../main/ts/editor/editorRename";

describe("editorRename (#4791)", () => {
  it("is edit-only", () => {
    expect(canRenameFromEditor("edit")).toBe(true);
    expect(canRenameFromEditor("view")).toBe(false);
    expect(canRenameFromEditor("promote")).toBe(false);
  });

  it("prefers sys_title over the payload name", () => {
    expect(
      editorListingName({
        contentId: "1",
        contentType: "percPage",
        name: "Payload",
        checkoutUser: "",
        fields: [{ name: "sys_title", value: " Listed " }],
      }),
    ).toBe("Listed");
  });

  it("joins the parent folder and listing name with a URL slash", () => {
    expect(buildRenameItemPath("//Assets", "Home")).toBe("//Assets/Home");
    expect(buildRenameItemPath("//Assets/", "Home")).toBe("//Assets/Home");
    expect(buildRenameItemPath("", "Home")).toBe("");
    expect(buildRenameItemPath("//Assets", "  ")).toBe("");
    expect(buildRenameItemPath("//Assets", "a/b")).toBe("");
    expect(
      editorItemPathFromLookup({
        name: "Home",
        folderPaths: "//Folders/$System$/Assets",
      }),
    ).toBe("//Folders/$System$/Assets/Home");
    expect(editorItemPathFromLookup({ path: "//Assets/Home", name: "Other" })).toBe(
      "//Assets/Home",
    );
  });

  it("treats a matching reload as landed and HTTP 400/403/404 as failures", () => {
    expect(renameLanded("Next", "Next")).toBe(true);
    expect(renameLanded("Next", "Home")).toBe(false);
    expect(renameLanded(" ", " ")).toBe(false);
    expect(editorRenameErrorReason({ status: 400, statusText: "", body: null })).toBe(
      "bad_request",
    );
    expect(editorRenameErrorReason({ status: 403, statusText: "", body: null })).toBe(
      "forbidden",
    );
    expect(editorRenameErrorReason({ status: 404, statusText: "", body: null })).toBe(
      "not_found",
    );
    expect(editorRenameErrorReason(new Error("boom"))).toBe("failed");
  });
});
