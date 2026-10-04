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

import { describe, expect, it, vi } from "vitest";
import { SessionRedirectError } from "../../../main/ts/api/client";
import {
  approveEditorItemToIncrementalQueue,
  editorIncrementalApproveFailureMessage,
  editorItemCanJoinIncrementalQueue,
} from "../../../main/ts/editor/editorIncrementalApprove";

describe("editor incremental approve (#5124)", () => {
  it("queues only a page or asset id", () => {
    expect(editorItemCanJoinIncrementalQueue("42", "page")).toBe(true);
    expect(editorItemCanJoinIncrementalQueue("42", "asset")).toBe(true);
    expect(editorItemCanJoinIncrementalQueue("  ", "page")).toBe(false);
    expect(editorItemCanJoinIncrementalQueue("42", "none")).toBe(false);
  });

  it("does not call the server when the item cannot be queued", async () => {
    const approve = vi.fn(async () => undefined);
    await expect(
      approveEditorItemToIncrementalQueue("42", "none", approve),
    ).resolves.toBe(false);
    await expect(
      approveEditorItemToIncrementalQueue("  ", "page", approve),
    ).resolves.toBe(false);
    expect(approve).not.toHaveBeenCalled();
  });

  it("returns true only after the explorer approve call succeeds", async () => {
    let settled = false;
    const approve = vi.fn(
      () =>
        new Promise<void>((resolve) => {
          setTimeout(() => {
            settled = true;
            resolve();
          }, 0);
        }),
    );
    const pending = approveEditorItemToIncrementalQueue("42", "asset", approve);
    expect(settled).toBe(false);
    await expect(pending).resolves.toBe(true);
    expect(settled).toBe(true);
    expect(approve).toHaveBeenCalledWith("42");
  });

  it("maps HTTP 400, 403, and 409 as failures", () => {
    expect(
      editorIncrementalApproveFailureMessage({
        status: 400,
        statusText: "Bad Request",
        body: {},
      }),
    ).toMatch(/could not be queued/i);
    expect(
      editorIncrementalApproveFailureMessage({
        status: 403,
        statusText: "Forbidden",
        body: {},
      }),
    ).toMatch(/not allowed/i);
    expect(
      editorIncrementalApproveFailureMessage({
        status: 409,
        statusText: "Conflict",
        body: {},
      }),
    ).toMatch(/blocked/i);
  });

  it("stays silent when the session is already redirecting", () => {
    expect(editorIncrementalApproveFailureMessage(new SessionRedirectError())).toBe(
      "",
    );
  });
});
