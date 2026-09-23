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

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { LogDetailsPanel } from "@/publishing/components/LogDetailsPanel";
import {
  logItemOpenErrorReason,
  openLogItemInEditor,
} from "@/publishing/openLogItemInEditor";

describe("openLogItemInEditor", () => {
  it("maps 403 and 404 and does not open a window", async () => {
    const open = vi.fn();
    const forbidden = await openLogItemInEditor(42, {
      probe: async () => {
        throw { status: 403, statusText: "Forbidden", body: null };
      },
      open,
    });
    expect(forbidden).toEqual({ ok: false, reason: "forbidden" });
    expect(open).not.toHaveBeenCalled();

    const missing = await openLogItemInEditor(99, {
      probe: async () => {
        throw { status: 404, statusText: "Not Found", body: null };
      },
      open,
    });
    expect(missing).toEqual({ ok: false, reason: "not_found" });
    expect(open).not.toHaveBeenCalled();
    expect(logItemOpenErrorReason({ status: 500 })).toBe("failed");

    const reserved = { closed: false, close: vi.fn() } as unknown as Window;
    const closed = await openLogItemInEditor(7, {
      reservedWindow: reserved,
      probe: async () => {
        throw { status: 403, statusText: "Forbidden", body: null };
      },
      open,
    });
    expect(closed.reason).toBe("forbidden");
    expect((reserved as unknown as { close: () => void }).close).toHaveBeenCalled();
  });

  it("opens the editor after a successful probe", async () => {
    const open = vi.fn().mockResolvedValue(true);
    const result = await openLogItemInEditor("42", {
      probe: async () => ({ contentId: "42" }),
      open,
    });
    expect(result).toEqual({ ok: true, reason: "opened" });
    expect(open).toHaveBeenCalledWith(
      { id: 42, mode: "edit" },
      { reservedWindow: null },
    );
  });

  it("refuses a row with no content id", async () => {
    const probe = vi.fn();
    const result = await openLogItemInEditor(undefined, { probe });
    expect(result.reason).toBe("missing_id");
    expect(probe).not.toHaveBeenCalled();
  });
});

describe("LogDetailsPanel open in editor", () => {
  const details = {
    SitePublishItem: [
      {
        contentid: 42,
        status: "Success",
        operation: "publish",
        fileName: "index.html",
      },
    ],
  };

  it("shows 404 in the item detail instead of a blank success", async () => {
    vi.stubGlobal("open", vi.fn(() => null));
    const openItem = vi.fn().mockResolvedValue({
      ok: false,
      reason: "not_found",
    });
    render(
      <LogDetailsPanel details={details} onClose={() => undefined} openItem={openItem} />,
    );
    fireEvent.click(screen.getByRole("button", { name: /item details/i }));
    fireEvent.click(screen.getByTestId("publish-log-open-editor"));
    await waitFor(() => {
      expect(screen.getByTestId("publish-log-item-open-error")).toHaveTextContent(
        "This item was not found",
      );
    });
    expect(openItem).toHaveBeenCalledWith(42, null);
  });
});
