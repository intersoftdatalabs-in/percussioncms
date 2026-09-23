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
  canMoveFromEditor,
  cmsFoldersEqual,
  editorMoveErrorReason,
  parentFolderOfItemPath,
} from "../../../main/ts/editor/editorMove";

describe("editorMove (#4774)", () => {
  it("allows move only in edit mode", () => {
    expect(canMoveFromEditor("edit")).toBe(true);
    expect(canMoveFromEditor("view")).toBe(false);
    expect(canMoveFromEditor("promote")).toBe(false);
  });

  it("strips the item name to the parent folder", () => {
    expect(parentFolderOfItemPath("//Sites/Demo/Home")).toBe("//Sites/Demo");
    expect(parentFolderOfItemPath("/Assets/bin/photo")).toBe("/Assets/bin");
    expect(parentFolderOfItemPath("/Assets/photo")).toBe("/Assets");
    expect(parentFolderOfItemPath("")).toBeNull();
  });

  it("treats finder and repository folder forms as the same folder", () => {
    expect(cmsFoldersEqual("//Sites/Demo", "/Sites/Demo/")).toBe(true);
    expect(cmsFoldersEqual("//Sites/Demo", "//Sites/Other")).toBe(false);
  });

  it("maps 403, 404, and 409", () => {
    expect(editorMoveErrorReason({ status: 403 })).toBe("forbidden");
    expect(editorMoveErrorReason({ status: 404 })).toBe("not_found");
    expect(editorMoveErrorReason({ status: 409 })).toBe("conflict");
    expect(editorMoveErrorReason({ status: 500 })).toBe("failed");
  });
});
