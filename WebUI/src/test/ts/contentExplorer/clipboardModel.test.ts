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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { describe, expect, it } from "vitest";
import {
  buildPasteSummary,
  canPasteInto,
  EMPTY_CLIPBOARD,
  isEmpty,
  isPasteFullySuccessful,
  setClipboard,
  size,
} from "../../../main/ts/contentExplorer/clipboard/model";
import type {
  ClipboardItem,
  ClipboardPasteSummary,
} from "../../../main/ts/api/contentExplorer/types";

function item(partial: Partial<ClipboardItem> = {}): ClipboardItem {
  return {
    id: "i-1",
    path: "/Sites/Foo/Bar",
    kind: "page",
    sourceAccessLevel: "ADMIN",
    ...partial,
  };
}

describe("setClipboard", () => {
  it("returns an immutable clipboard with new items + updated timestamp", () => {
    const cb = setClipboard(EMPTY_CLIPBOARD, "copy", [item()], () => "2026-07-20T00:00:00.000Z");
    expect(cb.operation).toBe("copy");
    expect(cb.items).toHaveLength(1);
    expect(cb.updatedAt).toBe("2026-07-20T00:00:00.000Z");
    // Frozen: mutating items throws in strict mode.
    expect(Object.isFrozen(cb)).toBe(true);
    expect(Object.isFrozen(cb.items)).toBe(true);
  });

  it("copies the items array (caller mutation does not affect the clipboard)", () => {
    const arr = [item()];
    const cb = setClipboard(EMPTY_CLIPBOARD, "cut", arr);
    arr.push(item({ id: "i-2" }));
    expect(cb.items).toHaveLength(1);
  });
});

describe("EMPTY_CLIPBOARD + size / isEmpty", () => {
  it("is empty by definition", () => {
    expect(isEmpty(EMPTY_CLIPBOARD)).toBe(true);
    expect(size(EMPTY_CLIPBOARD)).toBe(0);
  });

  it("isEmpty is false after setClipboard with items", () => {
    const cb = setClipboard(EMPTY_CLIPBOARD, "copy", [item()]);
    expect(isEmpty(cb)).toBe(false);
    expect(size(cb)).toBe(1);
  });
});

describe("canPasteInto (FR-016 read-only-without-rights gate)", () => {
  it("returns false when the clipboard is empty", () => {
    expect(canPasteInto(EMPTY_CLIPBOARD, "ADMIN")).toBe(false);
  });

  it("returns false when the target folder is unknown", () => {
    const cb = setClipboard(EMPTY_CLIPBOARD, "copy", [item()]);
    expect(canPasteInto(cb, undefined)).toBe(false);
  });

  it("returns false when the target folder is VIEW or READ", () => {
    const cb = setClipboard(EMPTY_CLIPBOARD, "copy", [item()]);
    expect(canPasteInto(cb, "VIEW")).toBe(false);
    expect(canPasteInto(cb, "READ")).toBe(false);
  });

  it("returns true when the target is WRITE", () => {
    const cb = setClipboard(EMPTY_CLIPBOARD, "copy", [item()]);
    expect(canPasteInto(cb, "WRITE")).toBe(true);
  });

  it("returns true when the target is ADMIN", () => {
    const cb = setClipboard(EMPTY_CLIPBOARD, "copy", [item()]);
    expect(canPasteInto(cb, "ADMIN")).toBe(true);
  });

  it("returns false when the source permission is undefined (the item was added without a snapshot)", () => {
    const it = item({ sourceAccessLevel: undefined });
    const cb = setClipboard(EMPTY_CLIPBOARD, "copy", [it]);
    expect(canPasteInto(cb, "ADMIN")).toBe(false);
  });

  it("returns true when every source has a defined snapshot (any access level is acceptable)", () => {
    const cb = setClipboard(EMPTY_CLIPBOARD, "copy", [
      item({ id: "a", sourceAccessLevel: "VIEW" }),
      item({ id: "b", sourceAccessLevel: "READ" }),
    ]);
    expect(canPasteInto(cb, "ADMIN")).toBe(true);
  });
});

describe("buildPasteSummary (transport result aggregation)", () => {
  it("maps fulfilled promises to ok=true", () => {
    const items = [item({ id: "a" }), item({ id: "b" })];
    const summary = buildPasteSummary("copy", items, [
      { status: "fulfilled", value: undefined },
      { status: "fulfilled", value: undefined },
    ]);
    expect(summary.operation).toBe("copy");
    expect(summary.results).toHaveLength(2);
    expect(summary.results.every((r) => r.ok)).toBe(true);
  });

  it("maps rejected promises to ok=false with the Error message", () => {
    const items = [item({ id: "a" }), item({ id: "b" })];
    const summary = buildPasteSummary("cut", items, [
      { status: "fulfilled", value: undefined },
      { status: "rejected", reason: new Error("disk full") },
    ]);
    expect(summary.results[0]?.ok).toBe(true);
    expect(summary.results[1]?.ok).toBe(false);
    expect(summary.results[1]?.message).toBe("disk full");
  });

  it("renders non-Error rejections as String(reason)", () => {
    const items = [item()];
    const summary = buildPasteSummary("copy", items, [
      { status: "rejected", reason: "boom" },
    ]);
    expect(summary.results[0]?.message).toBe("boom");
  });

  it("handles a missing settled entry (programmer error) gracefully", () => {
    const items = [item(), item()];
    const summary = buildPasteSummary("copy", items, [
      { status: "fulfilled", value: undefined },
    ]);
    expect(summary.results[1]?.ok).toBe(false);
    expect(summary.results[1]?.message).toContain("missing settled");
  });
});

describe("isPasteFullySuccessful", () => {
  it("returns true when all results are ok", () => {
    const summary: ClipboardPasteSummary = {
      operation: "copy",
      results: [{ item: item(), ok: true }],
    };
    expect(isPasteFullySuccessful(summary)).toBe(true);
  });

  it("returns false when any result failed", () => {
    const summary: ClipboardPasteSummary = {
      operation: "copy",
      results: [
        { item: item({ id: "a" }), ok: true },
        { item: item({ id: "b" }), ok: false, message: "x" },
      ],
    };
    expect(isPasteFullySuccessful(summary)).toBe(false);
  });

  it("returns true on empty summary (vacuous)", () => {
    expect(
      isPasteFullySuccessful({ operation: "copy", results: [] }),
    ).toBe(true);
  });
});
