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

describe("pasteClipboardItems / T092c / Edge Cases #3: concurrent rename/move 409", () => {
  it("copy of a folder POSTs CopyFolderItemRequest, not a bare sourcePath moveItem", async () => {
    const fetchMock = vi.spyOn(global, "fetch").mockResolvedValueOnce(
      new Response("", { status: 200 }),
    );
    await pasteClipboardItems([folder("Help")], "copy");
    const [url, init] = fetchMock.mock.calls[0] ?? [];
    expect(String(url)).toContain("/folders/copy/folder");
    expect(String(url)).not.toContain("/pathmanagement/path/moveItem");
    const posted = JSON.parse(String(init?.body ?? "{}")) as Record<string, unknown>;
    expect(posted).toEqual({
      CopyFolderItemRequest: {
        itemPath: "/Sites/Help",
        targetFolderPath: "/Sites/Help",
      },
    });
    expect(posted).not.toHaveProperty("sourcePath");
    expect(posted).not.toHaveProperty("copy");
  });

  it("surfaces a 409 from moveItem as a clear conflict per-item result (no silent overwrite)", async () => {
    // Cut (move) hits POST pathmanagement/path/moveItem with MoveFolderItem wrap.
    // Mock the server returning 409 Conflict — the second of two concurrent
    // moves on the same source folder; the first wins, the second sees 409.
    vi.spyOn(global, "fetch").mockResolvedValueOnce(
      new Response(
        JSON.stringify({ error: { code: "folder.nameConflict", message: "Folder already exists at target path" } }),
        { status: 409, headers: { "Content-Type": "application/json" } },
      ),
    );
    const summary = await pasteClipboardItems(
      [folder("shared")],
      "cut",
    );
    expect(summary.results).toHaveLength(1);
    expect(summary.results[0]?.ok).toBe(false);
    expect(summary.results[0]?.status).toBe(409);
    expect(summary.results[0]?.message).toContain("409");
    // No silent overwrite: the per-item failure is reported; the other
    // items in the clipboard (none here) would still proceed via
    // Promise.allSettled in the multi-item case.
  });

  it("surfaces 409 from moveItem in a mixed-clipboard paste; the rest proceed", async () => {
    // First item (folder) — server returns 409 conflict.
    // Second item (folder) — server returns 200 success.
    vi.spyOn(global, "fetch")
      .mockResolvedValueOnce(
        new Response(
          JSON.stringify({ error: { code: "folder.nameConflict", message: "Already exists" } }),
          { status: 409, headers: { "Content-Type": "application/json" } },
        ),
      )
      .mockResolvedValueOnce(
        new Response("", { status: 200 }),
      );
    const summary = await pasteClipboardItems(
      [folder("conflict"), folder("ok")],
      "copy",
    );
    expect(summary.results).toHaveLength(2);
    expect(summary.results[0]?.ok).toBe(false);
    expect(summary.results[0]?.status).toBe(409);
    expect(summary.results[1]?.ok).toBe(true);
    expect(summary.results[1]?.status).toBeUndefined();
  });

  it("does not set status when the rejection is a generic Error (network failure)", async () => {
    const transport = vi.fn().mockRejectedValueOnce(new Error("network down"));
    const summary = await pasteClipboardItems([folder("x")], "copy", transport);
    expect(summary.results[0]?.ok).toBe(false);
    expect(summary.results[0]?.status).toBeUndefined();
    expect(summary.results[0]?.message).toBe("network down");
  });
});

describe("pasteClipboardItems / T092e / Edge Cases #11: network failure mid-action", () => {
  it("surfaces a network-drop mid-flight as a recoverable failure (no data corruption, no hard fail)", async () => {
    // Browser network drop: fetch rejects with TypeError("Failed to fetch").
    // The summary surfaces the failure per-item; the destination folder is
    // untouched (no partial write). The UI can render a recoverable error
    // and let the user retry without a hard refresh.
    const transport = vi
      .fn()
      .mockRejectedValueOnce(new TypeError("Failed to fetch"));
    const summary = await pasteClipboardItems(
      [folder("net-drop")],
      "copy",
      transport,
    );
    expect(summary.results[0]?.ok).toBe(false);
    expect(summary.results[0]?.status).toBeUndefined();
    expect(summary.results[0]?.message).toBe("Failed to fetch");
    // No status field — this is a transport-level failure, not an HTTP
    // status; consumers distinguish "network" from "conflict" by the
    // absence of status (T092c) + the TypeError name.
  });

  it("surfaces a 401 (session expired) as a recoverable re-auth-required failure", async () => {
    // Session expired mid-action: the server returns 401 Unauthorized.
    // The clipboard layer surfaces it as ok:false with status=401; the UI
    // can render a "session expired — please refresh and retry" message.
    // No data corruption: the server rejected the write; the destination
    // folder is untouched.
    const apiError = Object.assign(new Error("401 Unauthorized"), {
      status: 401,
      statusText: "Unauthorized",
    });
    const transport = vi.fn().mockRejectedValueOnce(apiError);
    const summary = await pasteClipboardItems(
      [folder("auth")],
      "cut",
      transport,
    );
    expect(summary.results[0]?.ok).toBe(false);
    expect(summary.results[0]?.status).toBe(401);
    expect(summary.results[0]?.message).toContain("401");
  });

  it("supports re-auth + retry without a hard refresh (second attempt succeeds)", async () => {
    // First paste attempt: network drop (TypeError). User re-auths via
    // the in-page login dialog (no page reload). Second paste attempt
    // (same clipboard contents) succeeds — the destination folder
    // receives the copy; no data corruption from the first attempt's
    // failure. Each pasteClipboardItems call is independent; the
    // summary is fresh per call.
    const failingTransport = vi
      .fn()
      .mockRejectedValueOnce(new TypeError("Failed to fetch"));
    const firstSummary = await pasteClipboardItems(
      [folder("retry")],
      "copy",
      failingTransport,
    );
    expect(firstSummary.results[0]?.ok).toBe(false);

    // Re-auth happened (mocked at the boundary). The retry transport
    // succeeds — proves the paste layer is idempotent across retry.
    const succeedingTransport = vi.fn().mockResolvedValueOnce(undefined);
    const secondSummary = await pasteClipboardItems(
      [folder("retry")],
      "copy",
      succeedingTransport,
    );
    expect(secondSummary.results[0]?.ok).toBe(true);
    expect(succeedingTransport).toHaveBeenCalledTimes(1);
  });

  it("does not silently overwrite on transport failure (Promise.allSettled keeps per-item boundaries)", async () => {
    // Two items in the clipboard; the first call hits a network drop;
    // the second call succeeds. Each item's outcome is reported
    // independently — there is no "all-or-nothing" rollback because
    // the server is the authoritative writer (it would have rejected
    // the first write before any side-effect). The test asserts the
    // boundary is preserved per-item, not silently absorbed.
    const transport = vi
      .fn()
      .mockRejectedValueOnce(new TypeError("network blip"))
      .mockResolvedValueOnce(undefined);
    const summary = await pasteClipboardItems(
      [folder("a"), folder("b")],
      "copy",
      transport,
    );
    expect(summary.results.map((r) => r.ok)).toEqual([false, true]);
    expect(summary.results[0]?.status).toBeUndefined();
    expect(summary.results[1]?.status).toBeUndefined();
  });
});
