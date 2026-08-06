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

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import type {
  Clipboard,
  ClipboardItem,
  ClipboardPasteSummary,
} from "../../../main/ts/api/contentExplorer/types";
import { ClipboardPanel } from "../../../main/ts/contentExplorer/clipboard/ClipboardPanel";
import { EMPTY_CLIPBOARD, setClipboard } from "../../../main/ts/contentExplorer/clipboard/model";
import { renderA11yGate } from "./a11y";

function item(id: string): ClipboardItem {
  return { id, path: `/Sites/Foo/${id}`, kind: "page", sourceAccessLevel: "ADMIN" };
}
function makeCb(items: ClipboardItem[]): Clipboard {
  return setClipboard(EMPTY_CLIPBOARD, "copy", items);
}

describe("ClipboardPanel", () => {
  it("renders the clipboard panel with size 0 when empty", () => {
    render(
      <ClipboardPanel
        clipboard={EMPTY_CLIPBOARD}
        onClipboardChange={() => {}}
        items={[]}
        mode="copy"
        onModeChange={() => {}}
      />,
    );
    expect(screen.getByTestId("clipboard-panel")).toBeTruthy();
    expect(screen.getByTestId("clipboard-size").textContent).toBe("(0)");
  });

  it("Add button pushes the supplied selection into the clipboard", () => {
    const onChange = vi.fn();
    render(
      <ClipboardPanel
        clipboard={EMPTY_CLIPBOARD}
        onClipboardChange={onChange}
        items={[item("a"), item("b")]}
        mode="copy"
        onModeChange={() => {}}
      />,
    );
    fireEvent.click(screen.getByTestId("clipboard-add"));
    expect(onChange).toHaveBeenCalledTimes(1);
    const cb = onChange.mock.calls[0]?.[0] as Clipboard;
    expect(cb.operation).toBe("copy");
    expect(cb.items.map((i) => i.id)).toEqual(["a", "b"]);
  });

  it("Clear button empties the clipboard", () => {
    const onChange = vi.fn();
    const full = makeCb([item("a")]);
    render(
      <ClipboardPanel
        clipboard={full}
        onClipboardChange={onChange}
        items={[]}
        mode="copy"
        onModeChange={() => {}}
      />,
    );
    fireEvent.click(screen.getByTestId("clipboard-clear"));
    expect(onChange).toHaveBeenCalledWith(EMPTY_CLIPBOARD);
  });

  it("Mode radio buttons reflect the supplied mode and fire onModeChange", () => {
    const onModeChange = vi.fn();
    render(
      <ClipboardPanel
        clipboard={EMPTY_CLIPBOARD}
        onClipboardChange={() => {}}
        items={[]}
        mode="copy"
        onModeChange={onModeChange}
      />,
    );
    const cutRadio = screen.getByTestId("clipboard-mode-cut") as HTMLInputElement;
    fireEvent.click(cutRadio);
    expect(onModeChange).toHaveBeenCalledWith("cut");
  });

  it("Paste is disabled when target is not supplied", () => {
    render(
      <ClipboardPanel
        clipboard={makeCb([item("a")])}
        onClipboardChange={() => {}}
        items={[]}
        mode="copy"
        onModeChange={() => {}}
      />,
    );
    const paste = screen.getByTestId("clipboard-paste") as HTMLButtonElement;
    expect(paste.disabled).toBe(true);
  });

  it("Paste is disabled when the target is VIEW (FR-016 read-only-without-rights)", () => {
    render(
      <ClipboardPanel
        clipboard={makeCb([item("a")])}
        onClipboardChange={() => {}}
        items={[]}
        mode="copy"
        onModeChange={() => {}}
        target={{ path: "/Sites/Foo", accessLevel: "VIEW" }}
      />,
    );
    const paste = screen.getByTestId("clipboard-paste") as HTMLButtonElement;
    expect(paste.disabled).toBe(true);
  });

  it("Paste triggers the transport and aggregates per-item results", async () => {
    const paste = vi.fn().mockRejectedValueOnce(new Error("disk full"));
    const onClipboardChange = vi.fn();
    const onPasteSettled = vi.fn();
    render(
      <ClipboardPanel
        clipboard={makeCb([item("a"), item("b")])}
        onClipboardChange={onClipboardChange}
        items={[]}
        mode="copy"
        onModeChange={() => {}}
        target={{ path: "/Sites/Foo", accessLevel: "ADMIN" }}
        paste={paste}
        onPasteSettled={onPasteSettled}
      />,
    );
    fireEvent.click(screen.getByTestId("clipboard-paste"));
    await waitFor(() => {
      expect(onPasteSettled).toHaveBeenCalledTimes(1);
    });
    const summary: ClipboardPasteSummary = onPasteSettled.mock.calls[0]?.[0];
    expect(summary.results).toHaveLength(2);
    expect(summary.results[0]?.ok).toBe(false);
    expect(summary.results[0]?.message).toBe("disk full");
    // Partial failure: clipboard is not cleared.
    expect(onClipboardChange).not.toHaveBeenCalled();
    // Summary renders failures.
    expect(screen.getByTestId("clipboard-summary-failures")).toBeTruthy();
  });

  it("Paste clears the clipboard on full success (per-item ok)", async () => {
    const paste = vi.fn().mockResolvedValue(undefined);
    const onClipboardChange = vi.fn();
    render(
      <ClipboardPanel
        clipboard={makeCb([item("a"), item("b")])}
        onClipboardChange={onClipboardChange}
        items={[]}
        mode="copy"
        onModeChange={() => {}}
        target={{ path: "/Sites/Foo", accessLevel: "ADMIN" }}
        paste={paste}
      />,
    );
    fireEvent.click(screen.getByTestId("clipboard-paste"));
    await waitFor(() => {
      expect(onClipboardChange).toHaveBeenCalledWith(EMPTY_CLIPBOARD);
    });
    expect(screen.getByTestId("clipboard-summary")).toBeTruthy();
  });

  it("passes the zero serious/critical axe-core gate (empty state)", async () => {
    const { container } = render(
      <ClipboardPanel
        clipboard={EMPTY_CLIPBOARD}
        onClipboardChange={() => {}}
        items={[]}
        mode="copy"
        onModeChange={() => {}}
        target={null}
        paste={async () => ({ count: 0, summary: [] })}
      />,
    );
    await renderA11yGate(container);
  });

  it("passes the zero serious/critical axe-core gate (populated state)", async () => {
    const { container } = render(
      <ClipboardPanel
        clipboard={makeCb([item("a"), item("b")])}
        onClipboardChange={() => {}}
        items={[]}
        mode="copy"
        onModeChange={() => {}}
        target={{ path: "/Sites/Foo", accessLevel: "ADMIN" }}
        paste={async () => ({ count: 0, summary: [] })}
      />,
    );
    await renderA11yGate(container);
  });

  it("T092c / Edge Cases #3: marks the failure row with data-conflict when status=409", async () => {
    // Simulate the second of two concurrent moves losing the race; the
    // default paste transport would convert this ApiError into a
    // ClipboardPasteResultItem with status=409. The summary view must
    // expose that as data-conflict="true" so Playwright + axe can assert
    // it without scraping the human-readable text.
    const apiError = Object.assign(new Error("409 Conflict"), {
      status: 409,
      statusText: "Conflict",
    });
    const paste = vi.fn().mockRejectedValueOnce(apiError);
    const onPasteSettled = vi.fn();
    render(
      <ClipboardPanel
        clipboard={makeCb([item("shared")])}
        onClipboardChange={() => {}}
        items={[]}
        mode="cut"
        onModeChange={() => {}}
        target={{ path: "/Sites/Foo", accessLevel: "ADMIN" }}
        paste={paste}
        onPasteSettled={onPasteSettled}
      />,
    );
    fireEvent.click(screen.getByTestId("clipboard-paste"));
    await waitFor(() => {
      expect(onPasteSettled).toHaveBeenCalledTimes(1);
    });
    const failureRow = screen.getByTestId("clipboard-summary-failure-0");
    expect(failureRow.getAttribute("data-conflict")).toBe("true");
    expect(failureRow.textContent).toContain("409");
  });
});
