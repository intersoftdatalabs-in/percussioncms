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
import { editorIncrementalUnapproveFailureMessage, unapproveEditorItemOnIncrementalQueue } from "../../../main/ts/editor/editorIncrementalUnapprove";

describe("editor incremental unapprove (#5161)", () => {
  it("does not call the server for a template, folder, or blank id", async () => {
    const unapprove = vi.fn(async () => undefined);
    await expect(
      unapproveEditorItemOnIncrementalQueue("42", "none", unapprove),
    ).resolves.toBe(false);
    await expect(
      unapproveEditorItemOnIncrementalQueue("  ", "page", unapprove),
    ).resolves.toBe(false);
    await expect(
      unapproveEditorItemOnIncrementalQueue("", "asset", unapprove),
    ).resolves.toBe(false);
    expect(unapprove).not.toHaveBeenCalled();
  });

  it("returns true only after the explorer unapprove call succeeds", async () => {
    let settled = false;
    const unapprove = vi.fn(
      () =>
        new Promise<void>((resolve) => {
          setTimeout(() => {
            settled = true;
            resolve();
          }, 0);
        }),
    );
    const pending = unapproveEditorItemOnIncrementalQueue("42", "page", unapprove);
    expect(settled).toBe(false);
    await expect(pending).resolves.toBe(true);
    expect(settled).toBe(true);
    expect(unapprove).toHaveBeenCalledWith("42");
  });

  it("unapproves an asset id the same way", async () => {
    const unapprove = vi.fn(async () => undefined);
    await expect(
      unapproveEditorItemOnIncrementalQueue("7", "asset", unapprove),
    ).resolves.toBe(true);
    expect(unapprove).toHaveBeenCalledWith("7");
  });

  it("maps HTTP 400, 403, and 409 as failures", () => {
    expect(
      editorIncrementalUnapproveFailureMessage({
        status: 400,
        statusText: "Bad Request",
        body: {},
      }),
    ).toMatch(/could not be unapproved/i);
    expect(
      editorIncrementalUnapproveFailureMessage({
        status: 403,
        statusText: "Forbidden",
        body: {},
      }),
    ).toMatch(/not allowed/i);
    expect(
      editorIncrementalUnapproveFailureMessage({
        status: 409,
        statusText: "Conflict",
        body: {},
      }),
    ).toMatch(/blocked/i);
  });

  it("stays silent when the session is already redirecting", () => {
    expect(
      editorIncrementalUnapproveFailureMessage(new SessionRedirectError()),
    ).toBe("");
  });
});
