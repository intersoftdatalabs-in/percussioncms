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
  editorIncrementalRemoveFailureMessage,
  removeEditorItemFromIncrementalQueue,
} from "../../../main/ts/editor/editorIncrementalRemove";

describe("editor incremental remove (#5162)", () => {
  it("does not call the server for a template, folder, or blank id", async () => {
    const remove = vi.fn(async () => undefined);
    await expect(
      removeEditorItemFromIncrementalQueue("42", "none", remove),
    ).resolves.toBe(false);
    await expect(
      removeEditorItemFromIncrementalQueue("  ", "page", remove),
    ).resolves.toBe(false);
    await expect(
      removeEditorItemFromIncrementalQueue("", "asset", remove),
    ).resolves.toBe(false);
    expect(remove).not.toHaveBeenCalled();
  });

  it("returns true only after the explorer remove call succeeds", async () => {
    let settled = false;
    const remove = vi.fn(
      () =>
        new Promise<void>((resolve) => {
          setTimeout(() => {
            settled = true;
            resolve();
          }, 0);
        }),
    );
    const pending = removeEditorItemFromIncrementalQueue("42", "page", remove);
    expect(settled).toBe(false);
    await expect(pending).resolves.toBe(true);
    expect(settled).toBe(true);
    expect(remove).toHaveBeenCalledWith("42");
  });

  it("removes an asset id the same way", async () => {
    const remove = vi.fn(async () => undefined);
    await expect(
      removeEditorItemFromIncrementalQueue("7", "asset", remove),
    ).resolves.toBe(true);
    expect(remove).toHaveBeenCalledWith("7");
  });

  it("maps HTTP 400, 403, and 409 as failures", () => {
    expect(
      editorIncrementalRemoveFailureMessage({
        status: 400,
        statusText: "Bad Request",
        body: {},
      }),
    ).toMatch(/could not be removed/i);
    expect(
      editorIncrementalRemoveFailureMessage({
        status: 403,
        statusText: "Forbidden",
        body: {},
      }),
    ).toMatch(/not allowed/i);
    expect(
      editorIncrementalRemoveFailureMessage({
        status: 409,
        statusText: "Conflict",
        body: {},
      }),
    ).toMatch(/blocked/i);
  });

  it("stays silent when the session is already redirecting", () => {
    expect(
      editorIncrementalRemoveFailureMessage(new SessionRedirectError()),
    ).toBe("");
  });
});
