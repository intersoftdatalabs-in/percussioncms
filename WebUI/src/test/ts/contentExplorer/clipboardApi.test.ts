/*
 * Copyright 1999-2026 Percussion Software, Inc.
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

import { afterEach, describe, expect, it, vi } from "vitest";
import { pasteClipboardItems } from "../../../main/ts/api/contentExplorer/clipboardApi";
import type { ClipboardItem } from "../../../main/ts/api/contentExplorer/types";
import { PATHS } from "../../../main/ts/api/paths";

afterEach(() => {
  vi.restoreAllMocks();
});

function page(id = "p-1"): ClipboardItem {
  return { id, path: "/Sites/Foo/" + id, kind: "page" };
}
function folder(id = "f-1"): ClipboardItem {
  return { id, path: "/Sites/" + id, kind: "folder" };
}

describe("pasteClipboardItems", () => {
  it("calls the page-copy endpoint for page items (URL composition + body)", async () => {
    const fetchMock = vi.spyOn(global, "fetch").mockResolvedValueOnce(
      new Response("", { status: 200, headers: { "Content-Type": "text/plain" } }),
    );
    const summary = await pasteClipboardItems([page()], "copy");
    expect(fetchMock).toHaveBeenCalledTimes(1);
    const [url, init] = fetchMock.mock.calls[0] ?? [];
    expect(String(url)).toBe(
      `${PATHS.PAGE_COPY}/p-1?addToRecent=false`,
    );
    expect(init?.method).toBe("POST");
    expect(summary.operation).toBe("copy");
    expect(summary.results).toHaveLength(1);
    expect(summary.results[0]?.ok).toBe(true);
  });

  it("URL-encodes the page id", async () => {
    const fetchMock = vi.spyOn(global, "fetch").mockResolvedValueOnce(
      new Response("", { status: 200 }),
    );
    await pasteClipboardItems([page("12-3")], "cut");
    const [url] = fetchMock.mock.calls[0] ?? [];
    expect(String(url)).toBe(
      `${PATHS.PAGE_COPY}/12-3?addToRecent=false`,
    );
  });

  it("uses the provided custom transport and aggregates per-item outcomes", async () => {
    const transport = vi
      .fn()
      .mockResolvedValueOnce(undefined)
      .mockRejectedValueOnce(new Error("rejected by test"));
    const summary = await pasteClipboardItems(
      [page("a"), folder("b")],
      "copy",
      transport,
    );
    expect(transport).toHaveBeenCalledTimes(2);
    expect(summary.results).toHaveLength(2);
    expect(summary.results[0]?.ok).toBe(true);
    expect(summary.results[1]?.ok).toBe(false);
    expect(summary.results[1]?.message).toBe("rejected by test");
  });

  it("uses custom transport when one paste rejects; remaining items still proceed", async () => {
    const transport = vi
      .fn()
      .mockImplementationOnce(() => Promise.reject(new Error("first fail")))
      .mockImplementationOnce(() => Promise.resolve())
      .mockImplementationOnce(() => Promise.reject("non-error"));
    const summary = await pasteClipboardItems(
      [page("a"), page("b"), folder("c")],
      "copy",
      transport,
    );
    expect(summary.results.map((r) => r.ok)).toEqual([false, true, false]);
    expect(summary.results[2]?.message).toBe("non-error");
  });

  it("passes an empty items array through without invoking the transport", async () => {
    const transport = vi.fn();
    const summary = await pasteClipboardItems([], "copy", transport);
    expect(transport).not.toHaveBeenCalled();
    expect(summary).toEqual({ operation: "copy", results: [] });
  });
});
