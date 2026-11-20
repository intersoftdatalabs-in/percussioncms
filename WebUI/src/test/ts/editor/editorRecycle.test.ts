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
  editorClosedAfterRecycle,
  editorRecycleErrorReason,
  editorRecycleItemPath,
  focusRecycleError,
  formatRecycleItemNotice,
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

  it("hides the recycled item before the deferred URL update", () => {
    expect(editorClosedAfterRecycle(null, 42)).toBe(false);
    expect(editorClosedAfterRecycle(42, 42)).toBe(true);
    expect(editorClosedAfterRecycle(42, null)).toBe(true);
    expect(editorClosedAfterRecycle(42, 99)).toBe(false);
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

  it("names the open item in the recycle notice (#6773)", () => {
    expect(formatRecycleItemNotice("This item was not found.", "Home")).toBe(
      "This item was not found. (Home)",
    );
    expect(
      formatRecycleItemNotice(
        "This item cannot be recycled because it is in use or checked out.",
        "Home",
      ),
    ).toBe(
      "This item cannot be recycled because it is in use or checked out. (Home)",
    );
    expect(formatRecycleItemNotice("Could not recycle this item.", "  ")).toBe(
      "Could not recycle this item.",
    );
    expect(formatRecycleItemNotice("Could not recycle this item.", null)).toBe(
      "Could not recycle this item.",
    );
  });

  it("focusRecycleError focuses the recycle error (#6773)", () => {
    document.body.innerHTML =
      '<div data-testid="editor-recycle-error" tabindex="-1"></div>';
    const error = document.querySelector(
      '[data-testid="editor-recycle-error"]',
    ) as HTMLElement;
    expect(focusRecycleError()).toBe(true);
    expect(document.activeElement).toBe(error);
    document.body.innerHTML = "";
  });

  it("focusRecycleError returns false when the recycle error is absent (#6773)", () => {
    document.body.innerHTML = "";
    expect(focusRecycleError()).toBe(false);
  });
});