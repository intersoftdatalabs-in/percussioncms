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
import {
  openRelatedItemInEditor,
  relatedItemOpenErrorReason,
} from "../../../main/ts/contentExplorer/openRelatedItemInEditor";

describe("openRelatedItemInEditor (#5218)", () => {
  it("opens a related item after the fields probe succeeds", async () => {
    const probe = vi.fn(async () => ({ fields: [] }));
    const open = vi.fn(async () => true);
    const result = await openRelatedItemInEditor(
      { contentId: 9, folder: false },
      { probe, open },
    );
    expect(result).toEqual({ ok: true, reason: "opened" });
    expect(probe).toHaveBeenCalledWith("9");
    expect(open).toHaveBeenCalledWith(
      { id: 9, mode: "edit" },
      { reservedWindow: null },
    );
  });

  it("does not open a folder or a row with no content id", async () => {
    const probe = vi.fn(async () => ({ fields: [] }));
    const open = vi.fn(async () => true);
    const folder = await openRelatedItemInEditor(
      { contentId: 12, folder: true },
      { probe, open },
    );
    const missing = await openRelatedItemInEditor(
      { contentId: 0, folder: false },
      { probe, open },
    );
    const blank = await openRelatedItemInEditor(
      { contentId: null, folder: false },
      { probe, open },
    );
    expect(folder).toEqual({ ok: false, reason: "folder" });
    expect(missing).toEqual({ ok: false, reason: "no_content" });
    expect(blank).toEqual({ ok: false, reason: "no_content" });
    expect(probe).not.toHaveBeenCalled();
    expect(open).not.toHaveBeenCalled();
  });

  it("does not open on HTTP 403 or 404", async () => {
    const open = vi.fn(async () => true);
    const forbidden = await openRelatedItemInEditor(
      { contentId: 4, folder: false },
      {
        probe: async () => {
          throw { status: 403, statusText: "Forbidden", body: "" };
        },
        open,
      },
    );
    const missing = await openRelatedItemInEditor(
      { contentId: 5, folder: false },
      {
        probe: async () => {
          throw { status: 404, statusText: "Not Found", body: "" };
        },
        open,
      },
    );
    expect(forbidden).toEqual({ ok: false, reason: "forbidden" });
    expect(missing).toEqual({ ok: false, reason: "not_found" });
    expect(open).not.toHaveBeenCalled();
    expect(relatedItemOpenErrorReason({ status: 500 })).toBe("failed");
  });

  it("closes a reserved window when the probe fails", async () => {
    const close = vi.fn();
    const reserved = { closed: false, close } as unknown as Window;
    const result = await openRelatedItemInEditor(
      { contentId: 8, folder: false },
      {
        reservedWindow: reserved,
        probe: async () => {
          throw { status: 404, statusText: "Not Found", body: "" };
        },
        open: async () => true,
      },
    );
    expect(result.reason).toBe("not_found");
    expect(close).toHaveBeenCalled();
  });
});
